package com.cooreo.sippy.ui

import android.content.Context
import android.provider.Settings as AndroidSettings
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cooreo.sippy.BuildConfig
import com.cooreo.sippy.R
import com.cooreo.sippy.data.AppGraph
import com.cooreo.sippy.domain.drinks.DrinkTable
import com.cooreo.sippy.domain.water.WaterMath
import com.cooreo.sippy.model.Friend
import com.cooreo.sippy.model.LogEntry
import com.cooreo.sippy.model.StoredState
import com.cooreo.sippy.model.ThemeMode
import com.cooreo.sippy.ui.components.Celebration
import com.cooreo.sippy.ui.components.FloatingNav
import com.cooreo.sippy.ui.components.NavBarHeight
import com.cooreo.sippy.ui.components.NavTarget
import com.cooreo.sippy.ui.glass.GlassTier
import com.cooreo.sippy.ui.glass.LocalGlassTier
import com.cooreo.sippy.ui.glass.SippyBackdropHost
import com.cooreo.sippy.ui.screens.DeveloperScreen
import com.cooreo.sippy.ui.screens.DrinksScreen
import com.cooreo.sippy.ui.screens.FriendsScreen
import com.cooreo.sippy.ui.screens.HomeScreen
import com.cooreo.sippy.ui.screens.OnboardingScreen
import com.cooreo.sippy.ui.screens.SettingsScreen
import com.cooreo.sippy.ui.screens.moodFor
import com.cooreo.sippy.ui.screens.moodLine
import com.cooreo.sippy.ui.sheets.AddDrinkSheet
import com.cooreo.sippy.ui.sheets.CustomVolumeSheet
import com.cooreo.sippy.ui.sheets.EditEntrySheet
import com.cooreo.sippy.ui.sheets.UpdateSheet
import com.cooreo.sippy.ui.state.SippyController
import com.cooreo.sippy.ui.theme.LocalSippyColors
import com.cooreo.sippy.ui.theme.SippyTheme
import com.cooreo.sippy.ui.theme.accentFor
import com.cooreo.sippy.util.Dates
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
fun SippyRoot(graph: AppGraph) {
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
        val reduce = state.settings.reduceEffects || animationsDisabled(context)
        val tier = remember(reduce) { GlassTier.forSdk(reduceEffects = reduce) }
        CompositionLocalProvider(LocalGlassTier provides tier) {
            SippyBackdropHost {
                // The backdrop stays full-bleed; the content is padded by the
                // stable status-bar inset so the first row clears the clock even
                // while the bars are hidden (A1).
                Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.statusBarsIgnoringVisibility)) {
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
                                animate = !reduce,
                            )
                        }
                        else -> AppScaffold(state, controller, animate = !reduce)
                    }
                }
            }
        }
    }
}

@Composable
private fun AppScaffold(
    state: StoredState,
    controller: SippyController,
    animate: Boolean,
) {
    var tab by remember { mutableStateOf(Tab.HOME) }
    var showDeveloper by remember { mutableStateOf(false) }
    var showAddDrink by remember { mutableStateOf(false) }
    var showCustomVolume by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<LogEntry?>(null) }
    var celebrationAt by remember { mutableStateOf(0L) }
    var toast by remember { mutableStateOf<ToastMessage?>(null) }
    var remindersPrompt by remember { mutableIntStateOf(0) }

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
    val navSpace = NavBarHeight + navInset + 12.dp
    // Settings ends on a full-height card, so it gets the pill + inset + a
    // margin of its own (A1) instead of the shared clearance.
    val settingsBottom = navSpace + 24.dp

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
        Crossfade(
            targetState = tab,
            animationSpec = tween(if (animate) 220 else 0),
            label = "tab",
        ) { current ->
            when (current) {
                Tab.HOME -> HomeScreen(
                    state = state,
                    todayEntries = todayEntries,
                    hydratedMl = hydrated,
                    goalMl = goal,
                    animate = animate,
                    ringOverride = controller.ringOverride,
                    bottomPadding = navSpace,
                    onQuickAdd = { ml -> controller.addDrink(water, ml) },
                    onCustomAdd = { showCustomVolume = true },
                    onOpenFriends = { tab = Tab.FRIENDS },
                    onOpenReminders = {
                        tab = Tab.SETTINGS
                        remindersPrompt += 1
                    },
                    onEditEntry = { editing = it },
                    onFriendTap = { friend -> toast = moodToast(friend, state, goalMet, todayEntries) },
                )
                Tab.DRINKS -> DrinksScreen(
                    bottomPadding = navSpace,
                    onLog = { drink -> controller.addDrink(drink, drink.defaultVolumeMl) },
                )
                Tab.FRIENDS -> FriendsScreen(
                    state = state,
                    controller = controller,
                    hydratedMl = hydrated,
                    goalMl = goal,
                    animate = animate,
                    bottomPadding = navSpace,
                )
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
        )

        FloatingNav(
            current = tab.navTarget,
            onSelect = { tab = it.toTab() },
            onLog = { showAddDrink = true },
            modifier = Modifier.align(Alignment.BottomCenter),
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

    if (showAddDrink) {
        AddDrinkSheet(
            onDismiss = { showAddDrink = false },
            onPick = { drink, volume ->
                controller.addDrink(drink, volume)
                showAddDrink = false
            },
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
            onSave = { ml ->
                controller.updateEntryVolume(entry.id, ml)
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

    if (showDeveloper) {
        DeveloperScreen(
            state = state,
            controller = controller,
            onClose = { showDeveloper = false },
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
