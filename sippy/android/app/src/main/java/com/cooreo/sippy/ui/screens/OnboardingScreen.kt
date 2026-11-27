package com.cooreo.sippy.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.cooreo.sippy.domain.friends.FriendNameRules
import com.cooreo.sippy.domain.friends.Mood
import com.cooreo.sippy.domain.onboarding.Onboarding
import com.cooreo.sippy.model.FriendKind
import com.cooreo.sippy.model.Settings
import com.cooreo.sippy.model.StoredState
import com.cooreo.sippy.ui.components.FriendImage
import com.cooreo.sippy.ui.glass.GlassChip
import com.cooreo.sippy.ui.glass.GlassSurface
import com.cooreo.sippy.ui.state.SippyController
import com.cooreo.sippy.ui.theme.Dimens
import com.cooreo.sippy.ui.theme.LocalSippyColors
import com.cooreo.sippy.ui.theme.SippyType
import kotlinx.coroutines.launch

/**
 * Three-page onboarding over the site's backdrop: welcome, goal, first friend.
 * Skippable at any point (defaults are a 2000 ml goal and Dippy). The visible
 * page is persisted on every change, so a kill mid-flow reopens on that page.
 */
@Composable
fun OnboardingScreen(
    state: StoredState,
    controller: SippyController,
    animate: Boolean,
) {
    val c = LocalSippyColors.current
    val scope = rememberCoroutineScope()
    val startPage = Onboarding.startPage(
        storedPage = state.settings.onboardingPage,
        complete = state.settings.onboardingComplete,
    ) ?: Onboarding.PAGE_WELCOME
    val pagerState = rememberPagerState(initialPage = startPage) { Onboarding.PAGE_COUNT }

    var goalMl by remember { mutableIntStateOf(Onboarding.goalFor(state.settings.goalMl)) }
    val storedFriend = state.friends.firstOrNull()
    var kind by remember { mutableStateOf(storedFriend?.kind ?: FriendKind.DIPPY) }
    var name by remember {
        mutableStateOf(storedFriend?.name ?: FriendNameRules.defaultNameFor(FriendKind.DIPPY))
    }
    var nameEdited by remember { mutableStateOf(false) }
    var showNameError by remember { mutableStateOf(false) }

    LaunchedEffect(pagerState.currentPage) { controller.setOnboardingPage(pagerState.currentPage) }

    fun goTo(page: Int) {
        val target = Onboarding.clampPage(page)
        scope.launch {
            if (animate) pagerState.animateScrollToPage(target) else pagerState.scrollToPage(target)
        }
    }

    fun next() {
        if (Onboarding.isLastPage(pagerState.currentPage)) {
            val error = FriendNameRules.error(name)
            showNameError = error != null
            if (error != null) return
            controller.setGoal(goalMl)
            controller.chooseFirstFriend(kind, name)
            controller.completeOnboarding()
        } else {
            if (pagerState.currentPage == Onboarding.PAGE_GOAL) controller.setGoal(goalMl)
            goTo(Onboarding.nextPage(pagerState.currentPage))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = Dimens.screenPadding),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            SkipButton(onSkip = { controller.completeOnboarding() })
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.weight(1f),
            userScrollEnabled = true,
            key = { it },
        ) { page ->
            when (page) {
                Onboarding.PAGE_WELCOME -> WelcomePage(animate = animate)
                Onboarding.PAGE_GOAL -> GoalPage(
                    goalMl = goalMl,
                    onGoal = { goalMl = it },
                    onCommit = { controller.setGoal(it) },
                )
                else -> FriendPage(
                    kind = kind,
                    name = name,
                    showError = showNameError,
                    animate = animate,
                    onKind = { picked ->
                        kind = picked
                        if (!nameEdited) name = FriendNameRules.defaultNameFor(picked)
                        showNameError = false
                    },
                    onName = {
                        name = it
                        nameEdited = true
                        showNameError = false
                    },
                )
            }
        }

        PagerDots(
            count = Onboarding.PAGE_COUNT,
            current = pagerState.currentPage,
            onSelect = { goTo(it) },
        )
        Spacer(Modifier.height(10.dp))
        PrimaryCta(
            label = if (Onboarding.isLastPage(pagerState.currentPage)) {
                "Start sipping"
            } else {
                "Next"
            },
            onClick = { next() },
        )
        Spacer(Modifier.height(18.dp))
        Text(
            text = "Skipping keeps a ${Settings.DEFAULT_GOAL_ML} ml goal and Dippy.",
            style = SippyType.caption,
            color = c.text3,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(6.dp))
    }
}

@Composable
private fun SkipButton(onSkip: () -> Unit) {
    val c = LocalSippyColors.current
    Box(
        modifier = Modifier
            .heightIn(min = Dimens.touch)
            .widthIn(min = Dimens.touch)
            .clip(RoundedCornerShape(Dimens.pill))
            .clickable(role = Role.Button, onClick = onSkip)
            .semantics { contentDescription = "Skip onboarding" },
        contentAlignment = Alignment.Center,
    ) {
        GlassSurface(shape = RoundedCornerShape(Dimens.pill), shadow = false) {
            Box(Modifier.padding(horizontal = 18.dp, vertical = 12.dp)) {
                Text("Skip", style = SippyType.emphasis, color = c.text1)
            }
        }
    }
}

@Composable
private fun PrimaryCta(label: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(Dimens.pill)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clip(shape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        GlassSurface(shape = shape, tint = LocalSippyColors.current.accent.copy(alpha = 0.92f)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 17.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = label,
                    style = SippyType.emphasis,
                    fontWeight = FontWeight.Bold,
                    color = androidx.compose.ui.graphics.Color.White,
                )
            }
        }
    }
}

@Composable
private fun WelcomePage(animate: Boolean) {
    val c = LocalSippyColors.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        FriendImage(
            kind = FriendKind.DIPPY,
            mood = Mood.HAPPY,
            growthStreak = 0,
            size = 190,
            animate = animate,
            modifier = Modifier.size(190.dp),
        )
        Spacer(Modifier.height(22.dp))
        Text(
            text = "Every sip counts.",
            style = SippyType.display,
            color = c.text1,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Water, tea, coffee — every drink counted by what your body actually gets.",
            style = SippyType.body,
            color = c.text2,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 6.dp),
        )
    }
}

@Composable
private fun GoalPage(goalMl: Int, onGoal: (Int) -> Unit, onCommit: (Int) -> Unit) {
    val c = LocalSippyColors.current
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Set your goal.", style = SippyType.display, color = c.text1, textAlign = TextAlign.Center)
        Spacer(Modifier.height(14.dp))
        Text(
            text = "$goalMl ml",
            style = SippyType.value,
            color = c.accentBright,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "Your daily minimum. Change it any time.",
            style = SippyType.body,
            color = c.text2,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        Slider(
            value = goalMl.toFloat(),
            onValueChange = { onGoal(Onboarding.snapGoal(it)) },
            onValueChangeFinished = { onCommit(goalMl) },
            valueRange = Settings.MIN_GOAL_ML.toFloat()..Settings.MAX_GOAL_ML.toFloat(),
            steps = Onboarding.GOAL_SLIDER_STEPS,
            colors = SliderDefaults.colors(
                thumbColor = c.accentBright,
                activeTrackColor = c.accent,
                inactiveTrackColor = c.glassRimEnd,
                activeTickColor = c.glassTint,
                inactiveTickColor = c.glassTint,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = "Daily goal, $goalMl millilitres, from " +
                        "${Settings.MIN_GOAL_ML} to ${Settings.MAX_GOAL_ML}"
                },
        )
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Onboarding.GOAL_PRESETS.forEach { preset ->
                GlassChip(
                    label = "$preset ml",
                    selected = goalMl == preset,
                    onClick = {
                        onGoal(preset)
                        onCommit(preset)
                    },
                )
            }
        }
    }
}

@Composable
private fun FriendPage(
    kind: FriendKind,
    name: String,
    showError: Boolean,
    animate: Boolean,
    onKind: (FriendKind) -> Unit,
    onName: (String) -> Unit,
) {
    val c = LocalSippyColors.current
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text("Friends who notice.", style = SippyType.display, color = c.text1, textAlign = TextAlign.Center)
        Spacer(Modifier.height(10.dp))
        Text(
            text = "Pick one to start. You can add more later.",
            style = SippyType.body,
            color = c.text2,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            FriendKind.entries.forEach { option ->
                FriendOption(
                    kind = option,
                    label = FriendNameRules.defaultNameFor(option),
                    selected = kind == option,
                    animate = animate,
                    onClick = { onKind(option) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        OutlinedTextField(
            value = name,
            onValueChange = onName,
            singleLine = true,
            isError = showError,
            label = { Text("Name", style = SippyType.caption) },
            supportingText = if (showError) {
                { Text(FriendNameRules.error(name) ?: "", style = SippyType.caption) }
            } else {
                null
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = c.text1,
                unfocusedTextColor = c.text1,
                errorTextColor = c.text1,
                focusedBorderColor = c.accentBright,
                unfocusedBorderColor = c.glassRimStart,
                errorBorderColor = c.danger,
                focusedLabelColor = c.accentBright,
                unfocusedLabelColor = c.text2,
                errorLabelColor = c.danger,
                cursorColor = c.accentBright,
                focusedSupportingTextColor = c.danger,
                unfocusedSupportingTextColor = c.text3,
                errorSupportingTextColor = c.danger,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Friend name, 1 to 20 characters" },
        )
    }
}

@Composable
private fun FriendOption(
    kind: FriendKind,
    label: String,
    selected: Boolean,
    animate: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalSippyColors.current
    val shape = RoundedCornerShape(Dimens.inner)
    Box(
        modifier = modifier
            .heightIn(min = 132.dp)
            .clip(shape)
            .then(
                if (selected) Modifier.border(2.dp, c.accentBright, shape) else Modifier,
            )
            .clickable(role = Role.Button, onClick = onClick)
            .semantics {
                this.selected = selected
                contentDescription = if (selected) "$label, selected" else label
            },
        contentAlignment = Alignment.Center,
    ) {
        GlassSurface(
            shape = shape,
            tint = if (selected) c.accent.copy(alpha = 0.22f) else null,
            shadow = false,
        ) {
            Column(
                modifier = Modifier.padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                FriendImage(
                    kind = kind,
                    mood = Mood.HAPPY,
                    growthStreak = 0,
                    size = 72,
                    animate = animate,
                    modifier = Modifier.size(72.dp),
                )
                Spacer(Modifier.height(6.dp))
                Text(label, style = SippyType.caption, color = c.text2)
            }
        }
    }
}

@Composable
private fun PagerDots(count: Int, current: Int, onSelect: (Int) -> Unit) {
    val c = LocalSippyColors.current
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) { index ->
            val isCurrent = index == current
            Box(
                modifier = Modifier
                    .size(Dimens.touch)
                    .clip(CircleShape)
                    .clickable(role = Role.Tab, onClick = { onSelect(index) })
                    .semantics {
                        selected = isCurrent
                        contentDescription = "Page ${index + 1} of $count"
                    },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(if (isCurrent) 10.dp else 7.dp)
                        .clip(CircleShape)
                        .background(if (isCurrent) c.accentBright else c.text3.copy(alpha = 0.55f)),
                )
            }
        }
    }
}
