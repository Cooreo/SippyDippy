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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cooreo.sippy.BuildConfig
import com.cooreo.sippy.R
import com.cooreo.sippy.data.AppGraph
import com.cooreo.sippy.domain.water.WaterMath
import com.cooreo.sippy.model.ThemeMode
import com.cooreo.sippy.ui.components.Celebration
import com.cooreo.sippy.ui.glass.GlassChip
import com.cooreo.sippy.ui.glass.GlassTier
import com.cooreo.sippy.ui.glass.LocalGlassTier
import com.cooreo.sippy.ui.glass.SippyBackdropHost
import com.cooreo.sippy.ui.screens.DeveloperScreen
import com.cooreo.sippy.ui.screens.FriendsScreen
import com.cooreo.sippy.ui.screens.OnboardingScreen
import com.cooreo.sippy.ui.screens.SettingsScreen
import com.cooreo.sippy.ui.screens.TodayScreen
import com.cooreo.sippy.ui.sheets.AddDrinkSheet
import com.cooreo.sippy.ui.sheets.UpdateSheet
import com.cooreo.sippy.ui.state.SippyController
import com.cooreo.sippy.ui.theme.Accent
import com.cooreo.sippy.ui.theme.LocalSippyColors
import com.cooreo.sippy.ui.theme.SippyTheme
import com.cooreo.sippy.ui.theme.accentFor
import com.cooreo.sippy.util.Dates

private enum class Tab { TODAY, FRIENDS, SETTINGS }

/**
 * Root of the app: theme, glass tier, the one backdrop, the three tabs, the
 * sheets, the celebration and the splash. Everything below here is a pure
 * function of the stored state plus these callbacks.
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
                when {
                    !ready -> Splash()
                    // First run (or a kill mid-flow): onboarding owns the screen.
                    !state.settings.onboardingComplete -> OnboardingScreen(
                        state = state,
                        controller = controller,
                        animate = !reduce,
                    )
                    else -> AppScaffold(state, controller, animate = !reduce)
                }
            }
        }
    }
}

@Composable
private fun AppScaffold(
    state: com.cooreo.sippy.model.StoredState,
    controller: SippyController,
    animate: Boolean,
) {
    var tab by remember { mutableStateOf(Tab.TODAY) }
    var showDeveloper by remember { mutableStateOf(false) }
    var showAddDrink by remember { mutableStateOf(false) }
    var celebrationAt by remember { mutableStateOf(0L) }
    var info by remember { mutableStateOf<String?>(null) }

    val today = Dates.todayEpochDay()
    val todayEntries = remember(state.log, today) { state.log.filter { it.dayEpoch == today } }
    val hydrated = WaterMath.hydratedMl(todayEntries)
    val goal = state.settings.goalMl

    LaunchedEffect(Unit) {
        controller.events.collect { event ->
            when (event) {
                is SippyController.Event.Celebration -> celebrationAt = System.currentTimeMillis()
                is SippyController.Event.Milestone ->
                    info = "New milestone: ${event.days} days. Your friends grew."
                is SippyController.Event.Info -> info = event.text
            }
        }
    }
    // One update check a day, in the background; never blocking the first frame.
    LaunchedEffect(Unit) { controller.checkForUpdate(manual = false) }

    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f)) {
            Crossfade(targetState = tab, animationSpec = tween(if (animate) 220 else 0), label = "tab") { current ->
                when (current) {
                    Tab.TODAY -> TodayScreen(
                        state = state,
                        controller = controller,
                        todayEntries = todayEntries,
                        hydratedMl = hydrated,
                        goalMl = goal,
                        animate = animate,
                        ringOverride = controller.ringOverride,
                        onAddDrink = { showAddDrink = true },
                        onOpenFriends = { tab = Tab.FRIENDS },
                    )
                    Tab.FRIENDS -> FriendsScreen(
                        state = state,
                        controller = controller,
                        hydratedMl = hydrated,
                        goalMl = goal,
                        animate = animate,
                    )
                    Tab.SETTINGS -> SettingsScreen(
                        state = state,
                        controller = controller,
                        onOpenDeveloper = { showDeveloper = true },
                    )
                }
            }
            Celebration(
                trigger = celebrationAt,
                animate = animate,
                modifier = Modifier.fillMaxSize(),
            )
        }
        SippyTabBar(current = tab, onSelect = { tab = it })
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

    controller.updateOffer?.let { manifest ->
        UpdateSheet(
            manifest = manifest,
            progress = controller.updateManager.progress,
            installing = controller.installing,
            onInstall = { controller.downloadAndInstall(manifest) { message -> info = message } },
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

    info?.let { text ->
        LaunchedEffect(text) {
            kotlinx.coroutines.delay(3200)
            info = null
        }
        InfoToast(text = text, onDismiss = { info = null })
    }
}

@Composable
private fun SippyTabBar(current: Tab, onSelect: (Tab) -> Unit) {
    val c = LocalSippyColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TabEntries.forEach { (entryTab, label) ->
            Box(Modifier.weight(1f)) {
                GlassChip(
                    label = label,
                    selected = current == entryTab,
                    onClick = { onSelect(entryTab) },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

private val TabEntries = listOf(Tab.TODAY to "Today", Tab.FRIENDS to "Friends", Tab.SETTINGS to "Settings")

@Composable
private fun InfoToast(text: String, onDismiss: () -> Unit) {
    val c = LocalSippyColors.current
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        Box(
            Modifier
                .padding(bottom = 96.dp, start = 24.dp, end = 24.dp)
                .fillMaxWidth(),
        ) {
            com.cooreo.sippy.ui.glass.GlassCard(Modifier.fillMaxWidth()) {
                Text(text = text, color = c.text1, fontSize = 14.sp)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Tap to dismiss",
                    color = c.text3,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onDismiss),
                )
            }
        }
    }
}

/** Splash: static, no animation; replaced the moment storage is ready (<1.5 s). */
@Composable
private fun Splash() {
    val c = LocalSippyColors.current
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
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
