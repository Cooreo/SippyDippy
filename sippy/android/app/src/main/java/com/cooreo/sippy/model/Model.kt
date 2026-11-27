package com.cooreo.sippy.model

import kotlinx.serialization.Serializable

/**
 * The whole v1 state is a handful of JSON documents in DataStore.
 *
 * Contract shared by every document:
 *  - every property has a default, so a document written by an older build
 *    still loads;
 *  - unknown properties are ignored at decode time (StorageCodec);
 *  - [schemaVersion] identifies the writer; a different value is never
 *    "fixed" silently — the raw file is backed up and a clean state starts.
 */
@Serializable
data class StoredState(
    val schemaVersion: Int = 1,
    val settings: Settings = Settings(),
    val friends: List<Friend> = listOf(Friend.dippy()),
    val log: List<LogEntry> = emptyList(),
    val streak: StreakState = StreakState(),
    val update: UpdateState = UpdateState(),
    /** Spendable points (B4). Never negative. */
    val points: Int = 0,
    /** Counted millilitres not yet worth a full point — the partial carry-over. */
    val pointsCarryMl: Int = 0,
    /** Newest last, capped by PointsRules.LEDGER_LIMIT. */
    val ledger: List<PointsEntry> = emptyList(),
    /** Item ids the player owns. Keyed by item, so a friend leaving keeps them. */
    val ownedItems: Set<String> = emptySet(),
    /** friendId -> equipped item id. At most one cosmetic per friend. */
    val equipped: Map<String, String> = emptyMap(),
)

@Serializable
enum class ThemeMode { SYSTEM, LIGHT, DARK }

@Serializable
enum class AccentId { BLUE, TEAL, VIOLET, CORAL }

@Serializable
data class Settings(
    val theme: ThemeMode = ThemeMode.SYSTEM,
    val accent: AccentId = AccentId.BLUE,
    val goalMl: Int = 2000,
    val remindersEnabled: Boolean = false,
    /** "HH:mm", 24h, at most [MAX_REMINDERS] entries. */
    val reminderTimes: List<String> = listOf("09:00", "13:00", "17:00", "21:00"),
    val reduceEffects: Boolean = false,
    val developerUnlocked: Boolean = false,
    /** Optional; when blank the Home greeting says "Good morning" alone. */
    val displayName: String = "",
    /** Onboarding gate. False on a fresh install, so the app opens on page 1. */
    val onboardingComplete: Boolean = false,
    /** Last onboarding page shown, so a mid-onboarding kill reopens there. */
    val onboardingPage: Int = 0,
) {
    companion object {
        const val MAX_REMINDERS = 4
        const val MIN_GOAL_ML = 750
        const val MAX_GOAL_ML = 3000
        const val DEFAULT_GOAL_ML = 2000
    }
}

@Serializable
enum class FriendKind {
    DIPPY,
    PLANT,
    CAT,
    KOI,
    OTTER,
    TURTLE,
    AXOLOTL;

    /** Short label for the Add-a-friend chips. */
    val short: String
        get() = when (this) {
            DIPPY -> "Dippy"
            PLANT -> "Plant"
            CAT -> "Cat"
            KOI -> "Koi"
            OTTER -> "Otter"
            TURTLE -> "Turtle"
            AXOLOTL -> "Axolotl"
        }
}

@Serializable
data class Friend(
    val id: String,
    val kind: FriendKind,
    val name: String,
    /** Streak milestone the friend has grown to (0, 3, 7, 14, 30, 100). */
    val grownAtStreak: Int = 0,
    /** Sits in one of the two slots beside the water orb (B2). Max two. */
    val pinned: Boolean = false,
    val addedAtEpochDay: Long = 0L,
) {
    companion object {
        const val MAX_FRIENDS = 5
        fun dippy(epochDay: Long = 0L) =
            Friend(id = "dippy", kind = FriendKind.DIPPY, name = "Dippy", addedAtEpochDay = epochDay)
    }
}

@Serializable
data class LogEntry(
    val id: String,
    /** Local date as epoch-day, so day grouping never depends on time zones at read time. */
    val dayEpoch: Long,
    /** Wall clock for ordering and for "last entry" undo. */
    val atEpochMs: Long,
    val drinkId: String,
    val volumeMl: Int,
    /** Volume x hydration factor, frozen at log time. */
    val hydratedMl: Int,
    val sugary: Boolean,
    /** Points this entry earned when it was logged (B4); reversed on removal. */
    val pointsEarned: Int = 0,
)

/** One line of the points ledger. [delta] is negative for purchases. */
@Serializable
data class PointsEntry(
    val id: String,
    val atEpochMs: Long,
    val delta: Int,
    val reason: String,
    val balanceAfter: Int,
)

@Serializable
data class StreakState(
    val currentDays: Int = 0,
    val bestDays: Int = 0,
    val lastGoalEpochDay: Long = 0L,
)

@Serializable
data class UpdateState(
    val lastCheckEpochMs: Long = 0L,
    val snoozedUntilEpochMs: Long = 0L,
    /** Last 20 updater log lines, newest last (developer screen). */
    val log: List<String> = emptyList(),
)
