package com.cooreo.sippy.ui.state

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.cooreo.sippy.data.SippyRepository
import com.cooreo.sippy.domain.drinks.Drink
import com.cooreo.sippy.domain.drinks.DrinkTable
import com.cooreo.sippy.domain.friends.FriendNameRules
import com.cooreo.sippy.domain.friends.Mood
import com.cooreo.sippy.domain.friends.PinRules
import com.cooreo.sippy.domain.points.PointsRules
import com.cooreo.sippy.domain.shop.ItemTable
import com.cooreo.sippy.domain.shop.ShopRules
import com.cooreo.sippy.domain.onboarding.Onboarding
import com.cooreo.sippy.domain.streak.StreakCalculator
import com.cooreo.sippy.domain.updates.UpdateDecision
import com.cooreo.sippy.domain.updates.UpdateManifest
import com.cooreo.sippy.domain.updates.UpdatePolicy
import com.cooreo.sippy.domain.water.VolumeRules
import com.cooreo.sippy.domain.water.WaterMath
import com.cooreo.sippy.model.AccentId
import com.cooreo.sippy.model.Friend
import com.cooreo.sippy.model.FriendKind
import com.cooreo.sippy.model.LogEntry
import com.cooreo.sippy.model.PointsEntry
import com.cooreo.sippy.model.Settings
import com.cooreo.sippy.model.StoredState
import com.cooreo.sippy.model.StreakState
import com.cooreo.sippy.model.ThemeMode
import com.cooreo.sippy.reminders.ReminderScheduler
import com.cooreo.sippy.update.UpdateManager
import com.cooreo.sippy.util.Dates
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * The one place UI actions turn into state changes. No Composable touches the
 * repository directly; nothing here does I/O on the main thread.
 */
class SippyController(
    private val context: Context,
    private val repository: SippyRepository,
    val updateManager: UpdateManager,
    private val scope: CoroutineScope,
) {
    sealed interface Event {
        data object Celebration : Event
        data class Milestone(val days: Int) : Event
        data class Info(val text: String) : Event

        /** A drink was logged; the UI offers undo for exactly this entry. */
        data class Logged(val entryId: String, val drinkName: String, val volumeMl: Int) : Event
    }

    private val _events = MutableSharedFlow<Event>(extraBufferCapacity = 8)
    val events = _events.asSharedFlow()

    /** Developer-screen overrides (never persisted). */
    var ringOverride by mutableStateOf<Float?>(null)
        private set
    var moodOverride by mutableStateOf<Mood?>(null)
        private set

    /** A newer build is available and the sheet should be shown. */
    var updateOffer by mutableStateOf<UpdateManifest?>(null)
        private set
    var updateMessage by mutableStateOf<String?>(null)
        private set
    var installing by mutableStateOf(false)
        private set

    // ---------------------------------------------------------------- water

    fun addDrink(drink: Drink, volumeMl: Int) {
        if (volumeMl <= 0) return
        scope.launch {
            val today = Dates.todayEpochDay()
            val before = repository.state.value
            val goalBefore = before.settings.goalMl
            val hydratedBefore = WaterMath.hydratedMl(before.log.filter { it.dayEpoch == today })
            val entry = LogEntry(
                id = UUID.randomUUID().toString(),
                dayEpoch = today,
                atEpochMs = System.currentTimeMillis(),
                drinkId = drink.id,
                volumeMl = volumeMl,
                hydratedMl = WaterMath.hydrate(volumeMl, drink.hydrationFactor),
                sugary = drink.sugary,
            )
            repository.edit { state ->
                val credit = PointsRules.credit(state.points, state.pointsCarryMl, entry.hydratedMl)
                val logged = entry.copy(pointsEarned = credit.gained)
                val ledger = if (credit.gained > 0) {
                    PointsRules.append(
                        state.ledger,
                        PointsEntry(
                            id = "${logged.id}-points",
                            atEpochMs = logged.atEpochMs,
                            delta = credit.gained,
                            reason = "Logged ${logged.volumeMl} ml",
                            balanceAfter = credit.points,
                        ),
                    )
                } else {
                    state.ledger
                }
                val credited = state.copy(
                    points = credit.points,
                    pointsCarryMl = credit.carryMl,
                    ledger = ledger,
                )
                // A3: nothing is dropped. The log keeps every entry the player
                // ever logged; the list is lazy, so size is a storage concern, not
                // a UI one.
                val log = state.log + logged
                withStreak(credited, log, today)
            }
            val after = repository.state.value
            val hydratedAfter = WaterMath.hydratedMl(after.log.filter { it.dayEpoch == today })
            _events.tryEmit(Event.Logged(entry.id, drink.name, volumeMl))
            if (!WaterMath.goalMet(hydratedBefore, goalBefore) &&
                WaterMath.goalMet(hydratedAfter, after.settings.goalMl)
            ) {
                _events.tryEmit(Event.Celebration)
                StreakCalculator.milestoneReached(after.streak.currentDays)?.let {
                    _events.tryEmit(Event.Milestone(it))
                }
            }
        }
    }

    fun undoLast() {
        scope.launch {
            val today = Dates.todayEpochDay()
            repository.edit { state ->
                val last = state.log.maxByOrNull { it.atEpochMs } ?: return@edit state
                withStreak(releasePoints(state, listOf(last), "Undid a log"), state.log - last, today)
            }
        }
    }

    /**
     * Takes back the points an entry earned. Clamped at zero, and the ledger
     * records the real delta, so the balance and the history never disagree.
     */
    private fun releasePoints(state: StoredState, removed: List<LogEntry>, reason: String): StoredState {
        val earned = removed.sumOf { it.pointsEarned }
        if (earned <= 0) return state
        val after = PointsRules.debit(state.points, earned)
        if (after == state.points) return state
        return state.copy(
            points = after,
            ledger = PointsRules.append(
                state.ledger,
                PointsEntry(
                    id = UUID.randomUUID().toString(),
                    atEpochMs = System.currentTimeMillis(),
                    delta = after - state.points,
                    reason = reason,
                    balanceAfter = after,
                ),
            ),
        )
    }

    /** Re-quantifies one entry; hydration is recomputed from the drink table. */
    fun updateEntryVolume(id: String, volumeMl: Int) = updateEntry(id, volumeMl, null)

    /**
     * Edits volume and/or the time of an entry (A3). Moving an entry across local
     * midnight moves its day with it, so dayEpoch is recomputed, never copied.
     */
    fun updateEntry(id: String, volumeMl: Int, atEpochMs: Long?) {
        if (!VolumeRules.isValid(volumeMl)) return
        scope.launch {
            val today = Dates.todayEpochDay()
            repository.edit { state ->
                val target = state.log.firstOrNull { it.id == id } ?: return@edit state
                val drink = DrinkTable.byId(target.drinkId)
                val whenMs = atEpochMs ?: target.atEpochMs
                val edited = target.copy(
                    volumeMl = volumeMl,
                    hydratedMl = WaterMath.hydrate(volumeMl, drink.hydrationFactor),
                    atEpochMs = whenMs,
                    dayEpoch = Dates.dayOf(whenMs),
                )
                withStreak(state, state.log.map { if (it.id == id) edited else it }, today)
            }
        }
    }

    /** Puts a deleted entry back — the Undo half of swipe-to-delete (A3). */
    fun restoreEntry(entry: LogEntry) {
        scope.launch {
            val today = Dates.todayEpochDay()
            repository.edit { state ->
                if (state.log.any { it.id == entry.id }) return@edit state
                val log = (state.log + entry).sortedBy { it.atEpochMs }
                val credited = if (entry.pointsEarned > 0) {
                    state.copy(
                        points = state.points + entry.pointsEarned,
                        ledger = PointsRules.append(
                            state.ledger,
                            PointsEntry(
                                id = "${entry.id}-undo",
                                atEpochMs = System.currentTimeMillis(),
                                delta = entry.pointsEarned,
                                reason = "Undid a delete",
                                balanceAfter = state.points + entry.pointsEarned,
                            ),
                        ),
                    )
                } else {
                    state
                }
                withStreak(credited, log, today)
            }
        }
    }

    fun deleteEntry(id: String) {
        scope.launch {
            val today = Dates.todayEpochDay()
            repository.edit { state ->
                val target = state.log.firstOrNull { it.id == id } ?: return@edit state
                withStreak(
                    releasePoints(state, listOf(target), "Deleted an entry"),
                    state.log.filterNot { it.id == id },
                    today,
                )
            }
        }
    }

    // ------------------------------------------------------------- settings

    fun setGoal(goalMl: Int) {
        scope.launch {
            val today = Dates.todayEpochDay()
            repository.edit { state ->
                val goal = WaterMath.clampGoal(
                    goalMl,
                    Settings.MIN_GOAL_ML,
                    Settings.MAX_GOAL_ML,
                )
                withStreak(state.copy(settings = state.settings.copy(goalMl = goal)), state.log, today)
            }
        }
    }

    fun setTheme(mode: ThemeMode) = update { it.copy(theme = mode) }

    fun setAccent(accent: AccentId) = update { it.copy(accent = accent) }

    fun setReduceEffects(reduce: Boolean) = update { it.copy(reduceEffects = reduce) }

    fun setDeveloperUnlocked(unlocked: Boolean) = update { it.copy(developerUnlocked = unlocked) }

    fun setRemindersEnabled(enabled: Boolean) = update { it.copy(remindersEnabled = enabled) }

    fun setReminderTime(slot: Int, hhmm: String) {
        scope.launch {
            val today = Dates.todayEpochDay()
            repository.edit { state ->
                val times = Dates.sanitizeTimes(
                    state.settings.reminderTimes.toMutableList().also { list ->
                        while (list.size <= slot) list.add("12:00")
                        list[slot] = hhmm
                    },
                )
                withStreak(
                    state.copy(settings = state.settings.copy(reminderTimes = times)),
                    state.log,
                    today,
                )
            }
            ReminderScheduler.sync(context, repository.state.value.settings)
        }
    }

    // ----------------------------------------------------------- onboarding

    /** Remembers the visible page so a kill mid-onboarding reopens there. */
    fun setOnboardingPage(page: Int) {
        val clamped = Onboarding.clampPage(page)
        scope.launch {
            repository.edit { state ->
                if (state.settings.onboardingPage == clamped) {
                    state
                } else {
                    state.copy(settings = state.settings.copy(onboardingPage = clamped))
                }
            }
        }
    }

    fun completeOnboarding() {
        scope.launch {
            repository.edit { state ->
                state.copy(
                    settings = state.settings.copy(
                        onboardingComplete = true,
                        onboardingPage = Onboarding.PAGE_WELCOME,
                    ),
                )
            }
        }
    }

    /** Onboarding page 3: exactly one friend, chosen kind, validated name. */
    fun chooseFirstFriend(kind: FriendKind, rawName: String) {
        val name = FriendNameRules.sanitize(rawName)
            .ifBlank { FriendNameRules.defaultNameFor(kind) }
        scope.launch {
            repository.edit { state ->
                state.copy(
                    friends = listOf(
                        Friend(
                            id = kind.name.lowercase(),
                            kind = kind,
                            name = name,
                            grownAtStreak = StreakCalculator.growthFor(state.streak.currentDays),
                            addedAtEpochDay = Dates.todayEpochDay(),
                        ),
                    ),
                )
            }
        }
    }

    /** Optional name used by the Home greeting; blank means "Good morning". */
    fun setDisplayName(raw: String) = update { it.copy(displayName = raw.trim().take(24)) }

    // -------------------------------------------------------------- friends

    /**
     * Pin or unpin a friend for one of the two slots beside the orb (B2).
     * A third pin is refused with a message instead of silently doing nothing.
     */
    fun togglePin(friendId: String) {
        val current = repository.state.value
        val target = current.friends.firstOrNull { it.id == friendId } ?: return
        if (!target.pinned && !PinRules.canPin(current.friends, friendId)) {
            _events.tryEmit(Event.Info("Two friends can sit by the orb — unpin one first."))
            return
        }
        scope.launch {
            repository.edit { state -> state.copy(friends = PinRules.toggle(state.friends, friendId)) }
        }
    }

    /**
     * Buys a cosmetic if the balance covers it. Refusals are reported instead of
     * silently doing nothing.
     */
    fun buyItem(itemId: String) {
        val current = repository.state.value
        when (val outcome = ShopRules.purchase(current.points, current.ownedItems, itemId)) {
            is ShopRules.Purchase.Bought -> Unit
            ShopRules.Purchase.TooExpensive -> {
                val price = ItemTable.byId(itemId)?.price ?: 0
                _events.tryEmit(Event.Info("That costs $price points — you have ${current.points}."))
                return
            }
            ShopRules.Purchase.AlreadyOwned -> {
                _events.tryEmit(Event.Info("You already own that."))
                return
            }
            ShopRules.Purchase.UnknownItem -> return
        }
        scope.launch {
            repository.edit { state ->
                when (val r = ShopRules.purchase(state.points, state.ownedItems, itemId)) {
                    is ShopRules.Purchase.Bought -> state.copy(
                        points = r.balance,
                        ownedItems = r.owned,
                        ledger = PointsRules.append(
                            state.ledger,
                            PointsEntry(
                                id = UUID.randomUUID().toString(),
                                atEpochMs = System.currentTimeMillis(),
                                delta = -r.item.price,
                                reason = "Bought ${r.item.name}",
                                balanceAfter = r.balance,
                            ),
                        ),
                    )
                    else -> state
                }
            }
        }
    }

    /** Wears one cosmetic on a friend; [itemId] null takes it off. */
    fun equipItem(friendId: String, itemId: String?) {
        scope.launch {
            repository.edit { state ->
                val friend = state.friends.firstOrNull { it.id == friendId } ?: return@edit state
                state.copy(
                    equipped = ShopRules.equip(
                        equipped = state.equipped,
                        friendId = friend.id,
                        kind = friend.kind,
                        owned = state.ownedItems,
                        itemId = itemId,
                    ),
                )
            }
        }
    }

    fun addFriend(kind: FriendKind) {
        scope.launch {
            repository.edit { state ->
                if (state.friends.size >= Friend.MAX_FRIENDS) return@edit state
                if (state.friends.any { it.kind == kind }) return@edit state
                val name = FriendNameRules.defaultNameFor(kind)
                state.copy(
                    friends = state.friends + Friend(
                        id = UUID.randomUUID().toString(),
                        kind = kind,
                        name = name,
                        grownAtStreak = StreakCalculator.growthFor(state.streak.currentDays),
                        addedAtEpochDay = Dates.todayEpochDay(),
                    ),
                )
            }
        }
    }

    fun renameFriend(id: String, name: String) {
        if (!FriendNameRules.isValid(name)) return
        val clean = FriendNameRules.sanitize(name)
        scope.launch {
            repository.edit { state ->
                state.copy(friends = state.friends.map { if (it.id == id) it.copy(name = clean) else it })
            }
        }
    }

    // -------------------------------------------------------------- updater

    fun checkForUpdate(manual: Boolean) {
        scope.launch {
            when (val decision = updateManager.checkForUpdate(manual)) {
                is UpdateDecision.Offer -> {
                    updateOffer = decision.manifest
                    updateMessage = null
                }
                UpdateDecision.NoUpdate ->
                    if (manual) updateMessage = "You are on the newest build."
                UpdateDecision.Skipped ->
                    if (manual) updateMessage = "Already checked recently — nothing new."
            }
        }
    }

    fun dismissUpdate() {
        updateOffer = null
        scope.launch {
            repository.edit { state ->
                state.copy(
                    update = state.update.copy(
                        snoozedUntilEpochMs = System.currentTimeMillis() + UpdatePolicy.SNOOZE_MS,
                    ),
                )
            }
        }
    }

    fun clearUpdateMessage() {
        updateMessage = null
    }

    /**
     * Download → verify → hand to the system installer. [onError] receives a
     * user-facing sentence; null means the installer was opened.
     */
    fun downloadAndInstall(manifest: UpdateManifest, onError: (String) -> Unit) {
        if (installing) return
        installing = true
        scope.launch {
            try {
                val file = updateManager.download(manifest).getOrElse {
                    onError("Could not download the update. Check your connection and try again.")
                    return@launch
                }
                when (val verdict = updateManager.verify(file, manifest)) {
                    UpdateManager.Verify.Ok -> {
                        if (!updateManager.install(file)) {
                            onError("Could not open the Android installer.")
                        }
                    }
                    UpdateManager.Verify.SigningMismatch -> onError(SIGNING_MISMATCH_MESSAGE)
                    is UpdateManager.Verify.Failed ->
                        onError("The download did not verify (${verdict.reason}). Nothing was installed.")
                }
            } finally {
                installing = false
            }
        }
    }

    // ------------------------------------------------------------ developer

    fun triggerCelebration() {
        _events.tryEmit(Event.Celebration)
    }

    /** Developer preview of the ring (0..1), null returns to live data. */
    fun previewRing(fraction: Float?) {
        ringOverride = fraction
    }

    /** Developer mood override; null returns to the derived mood. */
    fun previewMood(mood: Mood?) {
        moodOverride = mood
    }

    fun resetAllData() {
        scope.launch {
            repository.resetToDefault()
            ringOverride = null
            moodOverride = null
            ReminderScheduler.sync(context, repository.state.value.settings)
            _events.tryEmit(Event.Info("All Sippy data was reset."))
        }
    }

    fun backupFiles(): List<String> = repository.backupFiles()

    // ------------------------------------------------------------- internals

    private fun update(transform: (Settings) -> Settings) {
        scope.launch {
            repository.edit { state -> state.copy(settings = transform(state.settings)) }
            ReminderScheduler.sync(context, repository.state.value.settings)
        }
    }

    /** Recomputes streak + friend growth after any change to the log or goal. */
    private fun withStreak(state: StoredState, log: List<LogEntry>, today: Long): StoredState {
        val goal = state.settings.goalMl
        val current = StreakCalculator.currentStreak(log, goal, today)
        val best = maxOf(state.streak.bestDays, StreakCalculator.bestStreak(log, goal))
        val lastGoalDay = StreakCalculator.metGoalDays(log, goal).maxOrNull() ?: 0L
        val growth = StreakCalculator.growthFor(current)
        return state.copy(
            log = log,
            streak = StreakState(currentDays = current, bestDays = best, lastGoalEpochDay = lastGoalDay),
            friends = state.friends.map { friend ->
                if (growth > friend.grownAtStreak) friend.copy(grownAtStreak = growth) else friend
            },
        )
    }

    companion object {
        /** Bounded log: a few hundred sips, oldest dropped first. */

        const val SIGNING_MISMATCH_MESSAGE =
            "This update is signed with a different key than the copy on your phone, so Android " +
                "will not install it over the top. Sippy has to be uninstalled once and installed " +
                "again — that removes local data. Nothing was changed."
    }
}
