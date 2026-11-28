package com.cooreo.sippy.ui

import android.content.Context
import android.os.Build
import android.provider.Settings as AndroidSettings
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cooreo.sippy.BuildConfig
import com.cooreo.sippy.R
import com.cooreo.sippy.data.AppGraph
import com.cooreo.sippy.domain.drinks.DrinkCategory
import com.cooreo.sippy.domain.drinks.DrinkTable
import com.cooreo.sippy.domain.friends.MoodRules
import com.cooreo.sippy.domain.intro.Vignette
import com.cooreo.sippy.domain.points.PointsRules
import com.cooreo.sippy.domain.water.WaterMath
import com.cooreo.sippy.model.Friend
import com.cooreo.sippy.model.LogEntry
import com.cooreo.sippy.model.StoredState
import com.cooreo.sippy.model.ThemeMode
import com.cooreo.sippy.ui.components.Celebration
import com.cooreo.sippy.ui.components.FloatingNav
import com.cooreo.sippy.ui.components.NavBarHeight
import com.cooreo.sippy.ui.components.NavTarget
import com.cooreo.sippy.ui.components.OrbAnchor
import com.cooreo.sippy.ui.glass.BackdropTints
import com.cooreo.sippy.ui.glass.GlassCapabilities
import com.cooreo.sippy.ui.glass.GlassDiagnostics
import com.cooreo.sippy.ui.glass.LocalBackdropTint
import com.cooreo.sippy.ui.glass.LocalGlassBoost
import com.cooreo.sippy.ui.glass.LocalGlassTier
import com.cooreo.sippy.ui.glass.SippyBackdropHost
import com.cooreo.sippy.ui.intro.StartupIntro
import com.cooreo.sippy.ui.screens.DeveloperScreen
import com.cooreo.sippy.ui.screens.DrinksScreen
import com.cooreo.sippy.ui.screens.FriendsScreen
import com.cooreo.sippy.ui.screens.HistoryScreen
import com.cooreo.sippy.ui.screens.HomeScreen
import com.cooreo.sippy.ui.screens.OnboardingScreen
import com.cooreo.sippy.ui.screens.SettingsScreen
import com.cooreo.sippy.ui.screens.ShopScreen
import com.cooreo.sippy.ui.screens.moodFor
import com.cooreo.sippy.ui.screens.moodLine
import com.cooreo.sippy.ui.sheets.AddDrinkSheet
import com.cooreo.sippy.ui.sheets.CustomVolumeSheet
import com.cooreo.sippy.ui.sheets.DressUpSheet
import com.cooreo.sippy.ui.sheets.EditEntrySheet
import com.cooreo.sippy.ui.sheets.LogDrinkFlow
import com.cooreo.sippy.ui.sheets.LogFly
import com.cooreo.sippy.ui.sheets.LogFlyData
import com.cooreo.sippy.ui.sheets.UpdateSheet
import com.cooreo.sippy.ui.state.SippyController
import com.cooreo.sippy.ui.system.LocalMotionAllowed
import com.cooreo.sippy.ui.system.rememberHaptics
import com.cooreo.sippy.ui.theme.DrinkTints
import com.cooreo.sippy.ui.theme.LocalSippyColors
import com.cooreo.sippy.ui.theme.SippyTheme
import com.cooreo.sippy.ui.theme.accentFor
import com.cooreo.sippy.util.Dates
import kotlin.random.Random
import kotlinx.coroutines.delay

private enum class Tab { HOME, DRINKS, FRIENDS, SETTINGS }

private val Tab.navTarget: NavTarget
    get() = when (this) {
        Tab.HOME -> NavTarget.HOME
        Tab.DRINKS -> NavTarget.DRINKS
        Tab.FRIENDS -> NavTarget.FRIENDS
        Tab.SETTINGS -> NavTarget.SETTINGS
    }

private fun NavTarget.toTab(): Tab = when (this) {
    NavTarget.HOME -> Tab.HOME
    NavTarget.DRINKS -> Tab.DRINKS
    NavTarget.FRIENDS -> Tab.FRIENDS
    NavTarget.SETTINGS -> Tab.SETTINGS
}

/** A short message above the bottom bar, optionally with one action (Undo). */
private data class ToastMessage(
    val text: String,
    val actionLabel: String? = null,
    val onAction: (() -> Unit)? = null,
)

/**
 * Root of the app: theme, glass tier, the one backdrop, onboarding, the four
 * tabs behind the floating nav, the sheets, the celebration and the splash.
 * Everything below here is a pure function of the stored state plus these
 * callbacks.
 */
@Composable
fun SippyRoot(
    graph: AppGraph,
    /** False on a warm start: the intro only plays on a cold one (B8). */
    showIntro: Boolean = true,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val controller = remember(graph) {
        SippyController(context, graph.repository, graph.updateManager, scope)
    }
    val state by graph.repository.state.collectAsStateWithLifecycle()
    val ready by graph.repository.ready.collectAsStateWithLifecycle()

    val dark = when (state.settings.theme) {
        ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val accent = accentFor(state.settings.accent)

    SippyTheme(dark = dark, accent = accent) {
        // A2: glass and motion are separate decisions. Only the app's own
        // "Reduce effects" switch may turn the glass off; the system
        // "remove animations" setting only makes animations instant.
        val reduceEffects = state.settings.reduceEffects
        val systemMotionOff = animationsDisabled(context)
        val animate = !(reduceEffects || systemMotionOff)
        val apiLevel = Build.VERSION.SDK_INT
        val lowRam = remember(context) {
            val manager = context.getSystemService(android.app.ActivityManager::class.java)
            manager?.isLowRamDevice ?: false
        }
        val diagnostics = remember(apiLevel, reduceEffects, systemMotionOff, lowRam) {
            GlassDiagnostics(
                apiLevel = apiLevel,
                reduceEffects = reduceEffects,
                systemAnimationsOff = systemMotionOff,
                lowRamDevice = lowRam,
                renderEffectSupported = GlassCapabilities.renderEffectSupported(apiLevel),
                runtimeShaderSupported = GlassCapabilities.runtimeShaderSupported(apiLevel),
                backdropInitialised = true,
            )
        }
        CompositionLocalProvider(
            LocalGlassTier provides diagnostics.tier,
            LocalGlassBoost provides if (controller.glassBoost) 2.4f else 1f,
        ) {
            SippyBackdropHost {
                // A2: the bars are visible and transparent over the backdrop. A
                // soft scrim sits behind the status icons so they stay legible on
                // a bright caustic, and the content frame starts below the real
                // status-bar inset (which also covers the display cutout).
                Box(Modifier.fillMaxSize()) {
                    StatusBarScrim()
                    Box(Modifier.fillMaxSize().statusBarsPadding()) {
                    when {
                        !ready -> Splash()
                        // First run (or a kill mid-flow): onboarding owns the screen.
                        !state.settings.onboardingComplete -> Box(
                            Modifier
                                .fillMaxSize()
                                .navigationBarsPadding(),
                        ) {
                            OnboardingScreen(
                                state = state,
                                controller = controller,
                                animate = animate,
                            )
                        }
                        else -> CompositionLocalProvider(LocalMotionAllowed provides animate) {
                            AppScaffold(
                                state = state,
                                controller = controller,
                                animate = animate,
                                diagnostics = diagnostics,
                                showIntro = showIntro,
                            )
                        }
                    }
                    }
                }
            }
        }
    }
}

/**
 * A subtle dark gradient behind the status-bar icons (round 8, A2). Drawn over
 * the backdrop, under the content, and it takes no input.
 */
@Composable
private fun StatusBarScrim() {
    val top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Box(
        Modifier
            .fillMaxWidth()
            .height(top + 20.dp)
            .background(
                Brush.verticalGradient(
                    listOf(Color.Black.copy(alpha = 0.34f), Color.Transparent),
                ),
            ),
    )
}

@Composable
private fun AppScaffold(
    state: StoredState,
    controller: SippyController,
    animate: Boolean,
    diagnostics: GlassDiagnostics,
    showIntro: Boolean = false,
) {
    var tab by remember { mutableStateOf(Tab.HOME) }
    val haptics = rememberHaptics(enabled = state.settings.hapticsEnabled)
    // B2: each tab tints the backdrop; the host crossfades on the change.
    val backdropTint = LocalBackdropTint.current
    LaunchedEffect(tab) {
        backdropTint.color = when (tab) {
            Tab.HOME -> BackdropTints.home
            Tab.DRINKS -> BackdropTints.drinks
            Tab.FRIENDS -> BackdropTints.friends
            Tab.SETTINGS -> BackdropTints.settings
        }
    }
    var showDeveloper by remember { mutableStateOf(false) }
    // B7: the Log flow grows out of the + button and flies its droplet into the orb.
    var logFlowOpen by remember { mutableStateOf(false) }
    var logButtonRect by remember { mutableStateOf(Rect.Zero) }
    var logFly by remember { mutableStateOf<LogFlyData?>(null) }
    // B8: the intro plays once per cold start, over Home, and its vignette is
    // derived (not stored in composition state) so a late settings read still
    // picks a different vignette from last time.
    var introVisible by remember { mutableStateOf(showIntro) }
    var introToken by remember { mutableIntStateOf(0) }
    var forcedVignette by remember { mutableStateOf<Vignette?>(null) }
    val introVignette = remember(forcedVignette, state.settings.lastIntroVignette, introToken) {
        forcedVignette ?: Vignette.pick(
            seed = Random.nextInt(10_000),
            lastIndex = state.settings.lastIntroVignette,
        )
    }
    var showCustomVolume by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<LogEntry?>(null) }
    var celebrationAt by remember { mutableStateOf(0L) }
    var toast by remember { mutableStateOf<ToastMessage?>(null) }
    var remindersPrompt by remember { mutableIntStateOf(0) }
    var showHistory by remember { mutableStateOf(false) }
    var showShop by remember { mutableStateOf(false) }
    // A4: the friend whose dress-up sheet is open, if any.
    var dressing by remember { mutableStateOf<Friend?>(null) }

    val today = Dates.todayEpochDay()
    val todayEntries = remember(state.log, today) {
        state.log.filter { it.dayEpoch == today }.sortedByDescending { it.atEpochMs }
    }
    val hydrated = WaterMath.hydratedMl(todayEntries)
    val goal = state.settings.goalMl
    val goalMet = WaterMath.goalMet(hydrated, goal)
    // Quick adds are water; the Log sheet is there for everything else.
    val water = remember { DrinkTable.byId("water") }

    // The floating pill plus the gesture inset, so nothing hides behind it.
    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    // Round 9, 1.4: one clearance for every screen — the pill, the gesture inset
    // and 24dp of air. Settings used to add another 24 on top of a 16dp base, so
    // the same screen scrolled to two different distances depending on the tab.
    val navSpace = NavBarHeight + navInset + 24.dp
    val settingsBottom = navSpace

    LaunchedEffect(Unit) {
        controller.events.collect { event ->
            when (event) {
                is SippyController.Event.Celebration -> celebrationAt = System.currentTimeMillis()
                is SippyController.Event.Milestone -> toast = ToastMessage(
                    text = "New milestone: ${event.days} days. Your friends grew.",
                )
                is SippyController.Event.Info -> toast = ToastMessage(text = event.text)
                is SippyController.Event.Logged -> {
                    val entryId = event.entryId
                    toast = ToastMessage(
                        text = "${event.drinkName} · ${event.volumeMl} ml logged",
                        actionLabel = "Undo",
                        onAction = { controller.deleteEntry(entryId) },
                    )
                }
            }
        }
    }
    LaunchedEffect(toast) {
        if (toast != null) {
            delay(4200)
            toast = null
        }
    }
    // One update check a day, in the background; never blocking the first frame.
    LaunchedEffect(Unit) { controller.checkForUpdate(manual = false) }

    Box(Modifier.fillMaxSize()) {
        // Shared-axis tab change: both screens move along one horizontal axis,
        // the incoming one springing in from the side it came from. Instant
        // (no transition at all) when motion is off.
        AnimatedContent(
            targetState = tab,
            transitionSpec = {
                if (!animate) {
                    EnterTransition.None togetherWith ExitTransition.None
                } else {
                    val forward = targetState.ordinal >= initialState.ordinal
                    val springSpec = spring<IntOffset>(dampingRatio = 0.86f, stiffness = 320f)
                    val enter = slideInHorizontally(animationSpec = springSpec) { full ->
                        if (forward) full / 6 else -full / 6
                    } + fadeIn(tween(180))
                    val exit = slideOutHorizontally(animationSpec = springSpec) { full ->
                        if (forward) -full / 5 else full / 5
                    } + fadeOut(tween(140))
                    enter togetherWith exit
                }
            },
            label = "tabs",
        ) { current ->
            when (current) {
                Tab.HOME -> if (showHistory) {
                    HistoryScreen(
                        state = state,
                        bottomPadding = navSpace,
                        onBack = { showHistory = false },
                        onEditEntry = { editing = it },
                        onDeleteEntry = { entry ->
                            controller.deleteEntry(entry.id)
                            toast = ToastMessage(
                                text = "Entry deleted",
                                actionLabel = "Undo",
                                onAction = { controller.restoreEntry(entry) },
                            )
                        },
                    )
                } else HomeScreen(
                    state = state,
                    todayEntries = todayEntries,
                    hydratedMl = hydrated,
                    goalMl = goal,
                    animate = animate,
                    ringOverride = controller.ringOverride,
                    bottomPadding = navSpace,
                    onQuickAdd = { ml -> controller.addDrink(water, ml) },
                    onOpenFriends = { tab = Tab.FRIENDS },
                    // A5: the points chip opens the Shop, which lives under Friends.
                    onOpenShop = {
                        tab = Tab.FRIENDS
                        showShop = true
                    },
                    onEditEntry = { editing = it },
                    onFriendTap = { friend -> toast = moodToast(friend, state, goalMet, todayEntries) },
                    onSeeAll = { showHistory = true },
                )
                Tab.DRINKS -> DrinksScreen(
                    bottomPadding = navSpace,
                    onLog = { drink -> controller.addDrink(drink, drink.defaultVolumeMl) },
                )
                Tab.FRIENDS -> if (showShop) {
                    ShopScreen(
                        state = state,
                        onBuy = { id -> controller.buyItem(id) },
                        onEquip = { friendId, itemId -> controller.equipItem(friendId, itemId) },
                        onBack = { showShop = false },
                        bottomPadding = navSpace,
                        animate = animate,
                    )
                } else {
                    FriendsScreen(
                        state = state,
                        controller = controller,
                        hydratedMl = hydrated,
                        goalMl = goal,
                        animate = animate,
                        onTogglePin = { id -> controller.togglePin(id) },
                        onOpenShop = { showShop = true },
                        onDressUp = { friend -> dressing = friend },
                        bottomPadding = navSpace,
                    )
                }
                Tab.SETTINGS -> SettingsScreen(
                    state = state,
                    controller = controller,
                    onOpenDeveloper = { showDeveloper = true },
                    bottomPadding = settingsBottom,
                    remindersPrompt = remindersPrompt,
                )
            }
        }

        Celebration(
            trigger = celebrationAt,
            animate = animate,
            modifier = Modifier.fillMaxSize(),
            onFinished = { celebrationAt = 0L },
        )

        FloatingNav(
            current = tab.navTarget,
            onSelect = { target ->
                // B10: a tick on every real tab change, never on a re-tap.
                if (target.toTab() != tab) haptics.tick()
                tab = target.toTab()
                showHistory = false
                showShop = false
            },
            onLog = { logFlowOpen = true },
            modifier = Modifier.align(Alignment.BottomCenter),
            animate = animate,
            onLogBounds = { logButtonRect = it },
        )

        toast?.let { message ->
            ToastBar(
                message = message,
                onDismiss = { toast = null },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(
                        start = 20.dp,
                        end = 20.dp,
                        bottom = navSpace,
                    ),
            )
        }
    }

    if (logFlowOpen) {
        LogDrinkFlow(
            origin = logButtonRect,
            animate = animate,
            onDismiss = { logFlowOpen = false },
            onLog = { drink, volume ->
                logFlowOpen = false
                val counted = WaterMath.hydrate(volume, drink.hydrationFactor)
                val gained = PointsRules.credit(
                    points = state.points,
                    carryMl = state.pointsCarryMl,
                    countedMl = counted,
                ).gained
                logFly = LogFlyData(
                    id = System.currentTimeMillis(),
                    from = logButtonRect,
                    to = OrbAnchor.bounds,
                    volumeMl = volume,
                    points = gained,
                    tint = DrinkTints.of(DrinkCategory.of(drink.id)),
                )
                haptics.thud()
                controller.addDrink(drink, volume)
            },
            onCustom = {
                logFlowOpen = false
                showCustomVolume = true
            },
        )
    }

    logFly?.let { flight ->
        LogFly(
            data = flight,
            animate = animate,
            onFinished = { logFly = null },
            modifier = Modifier.fillMaxSize(),
        )
    }

    if (showCustomVolume) {
        CustomVolumeSheet(
            onDismiss = { showCustomVolume = false },
            onPick = { ml ->
                controller.addDrink(water, ml)
                showCustomVolume = false
            },
        )
    }

    editing?.let { entry ->
        EditEntrySheet(
            entry = entry,
            goalMl = goal,
            onSave = { ml, atEpochMs ->
                controller.updateEntry(entry.id, ml, atEpochMs)
                editing = null
            },
            onDelete = {
                controller.deleteEntry(entry.id)
                editing = null
            },
            onDismiss = { editing = null },
        )
    }

    controller.updateOffer?.let { manifest ->
        UpdateSheet(
            manifest = manifest,
            progress = controller.updateManager.progress,
            installing = controller.installing,
            onInstall = { controller.downloadAndInstall(manifest) { message -> toast = ToastMessage(message) } },
            onDismiss = { controller.dismissUpdate() },
        )
    }

    // B8: drawn over everything, including the bar and the sheets. Fading it out
    // is what reveals Home, so the hand-off can never flash black.
    if (introVisible) {
        key(introToken) {
            StartupIntro(
                state = state,
                vignette = introVignette,
                animate = animate,
                onFinished = {
                    introVisible = false
                    controller.rememberIntroVignette(introVignette.ordinal)
                },
            )
        }
    }

    dressing?.let { friend ->
        // Keep the sheet alive across renames and equips: resolve the latest copy.
        val live = state.friends.firstOrNull { it.id == friend.id }
        if (live != null) {
            DressUpSheet(
                friend = live,
                mood = controller.moodOverride ?: MoodRules.forDay(
                    goalMetToday = goalMet,
                    hasEntriesToday = todayEntries.isNotEmpty(),
                    currentStreakDays = state.streak.currentDays,
                    yesterdayMissedAfterStreak = false,
                ),
                owned = state.ownedItems,
                hats = state.equipped,
                outfits = state.equippedOutfits,
                onToggle = { itemId -> controller.equipItem(live.id, itemId) },
                onDismiss = { dressing = null },
            )
        }
    }

    if (showDeveloper) {
        DeveloperScreen(
            state = state,
            controller = controller,
            onClose = { showDeveloper = false },
            diagnostics = diagnostics,
            onReplayIntro = {
                forcedVignette = null
                introToken += 1
                introVisible = true
            },
            onPickVignette = { vignette ->
                forcedVignette = vignette
                introToken += 1
                introVisible = true
            },
        )
    }
}

private fun moodToast(
    friend: Friend,
    state: StoredState,
    goalMetToday: Boolean,
    todayEntries: List<LogEntry>,
): ToastMessage {
    val mood = moodFor(
        friend = friend,
        state = state,
        goalMetToday = goalMetToday,
        hasEntriesToday = todayEntries.isNotEmpty(),
    )
    return ToastMessage(text = moodLine(friend.name, mood))
}

@Composable
private fun ToastBar(
    message: ToastMessage,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalSippyColors.current
    com.cooreo.sippy.ui.glass.GlassCard(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = message.text,
                color = c.text1,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f),
            )
            val action = message.onAction
            if (action != null && message.actionLabel != null) {
                Spacer(Modifier.size(8.dp))
                Text(
                    text = message.actionLabel,
                    color = c.accentBright,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable(role = Role.Button) {
                            action()
                            onDismiss()
                        }
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                )
            }
        }
        if (message.onAction == null) {
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Tap to dismiss",
                color = c.text3,
                fontSize = 12.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onDismiss)
                    .padding(vertical = 4.dp),
            )
        }
    }
}

/** Splash: static, no animation; replaced the moment storage is ready (<1.5 s). */
@Composable
private fun Splash() {
    val c = LocalSippyColors.current
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(R.drawable.dippy_waving),
                contentDescription = null,
                modifier = Modifier.size(120.dp),
            )
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Sippy",
                color = c.text1,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(text = BuildConfig.VERSION_NAME, color = c.text3, fontSize = 13.sp)
        }
    }
}

/** True when the system "remove animations" scale is 0 (respect it). */
private fun animationsDisabled(context: Context): Boolean = try {
    AndroidSettings.Global.getFloat(
        context.contentResolver,
        AndroidSettings.Global.ANIMATOR_DURATION_SCALE,
        1f,
    ) == 0f
} catch (t: Throwable) {
    false
}
