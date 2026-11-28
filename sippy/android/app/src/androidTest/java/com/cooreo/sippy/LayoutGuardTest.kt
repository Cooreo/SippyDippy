package com.cooreo.sippy

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollToIndexAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToIndex
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.cooreo.sippy.data.SippyRepository
import com.cooreo.sippy.model.StoredState
import com.cooreo.sippy.ui.components.FloatingNav
import com.cooreo.sippy.ui.components.NavTarget
import com.cooreo.sippy.ui.components.WaterOrb
import com.cooreo.sippy.ui.glass.GlassTier
import com.cooreo.sippy.ui.glass.LocalGlassTier
import com.cooreo.sippy.ui.screens.LayoutCheckSampleSet
import com.cooreo.sippy.ui.screens.SettingsScreen
import com.cooreo.sippy.ui.state.SippyController
import com.cooreo.sippy.ui.system.LocalMotionAllowed
import com.cooreo.sippy.ui.theme.SippyTheme
import com.cooreo.sippy.update.UpdateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Round 9, 1.3: instrumented layout guard at font scales 1.0, 1.3 and 1.5 on a
 * narrow 360dp phone width. Proves single-line chips, buttons, nav tabs, grouped
 * settings and the orb caption never wrap mid-word, ellipsise or overflow.
 */
@RunWith(AndroidJUnit4::class)
class LayoutGuardTest {

    @get:Rule
    val rule = createComposeRule()

    private val scales = listOf(1.0f, 1.3f, 1.5f)

    @Test
    fun chipsAndButtonsStaySingleLineAndInside360dpAcrossFontScales() {
        var fontScale by mutableFloatStateOf(1.0f)
        rule.setContent {
            val base = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(base.density, fontScale),
                LocalGlassTier provides GlassTier.FROST,
                LocalMotionAllowed provides false,
            ) {
                SippyTheme(dark = true) {
                    Box(
                        modifier = Modifier
                            .requiredWidth(360.dp)
                            .padding(horizontal = 20.dp)
                            .testTag("guard-root"),
                    ) {
                        LayoutCheckSampleSet()
                    }
                }
            }
        }

        val labels = listOf(
            "Shop",
            "See all",
            "Back",
            "−250",
            "+250",
            "−15 min",
            "Buy",
            "Equip",
            "Needs Koi",
            "7",
            "days",
            "120",
            "pts",
            "Dress up",
            "Save",
        )
        for (scale in scales) {
            rule.runOnUiThread { fontScale = scale }
            rule.waitForIdle()
            labels.forEach { label ->
                assertSingleLineInsideRoot(label, scale, "guard-root")
            }
        }
    }

    @Test
    fun floatingNavTabLabelsStaySingleLineAcrossFontScales() {
        var fontScale by mutableFloatStateOf(1.0f)
        rule.setContent {
            val base = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(base.density, fontScale),
                LocalGlassTier provides GlassTier.FROST,
                LocalMotionAllowed provides false,
            ) {
                SippyTheme(dark = true) {
                    Box(
                        modifier = Modifier
                            .requiredWidth(360.dp)
                            .testTag("nav-root"),
                    ) {
                        FloatingNav(
                            current = NavTarget.HOME,
                            onSelect = {},
                            onLog = {},
                            animate = false,
                        )
                    }
                }
            }
        }

        for (scale in scales) {
            rule.runOnUiThread { fontScale = scale }
            rule.waitForIdle()
            NavTarget.entries.forEach { target ->
                assertSingleLineInsideRoot(target.label, scale, "nav-root")
            }
        }
    }

    @Test
    fun groupedSettingsRenderCleanlyAt360dpAcrossFontScales() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val repo = InMemorySippyRepository()
        val controller = SippyController(
            context = context,
            repository = repo,
            updateManager = UpdateManager(context, repo),
            scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
        )
        var fontScale by mutableFloatStateOf(1.0f)

        rule.setContent {
            val base = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(base.density, fontScale),
                LocalGlassTier provides GlassTier.FROST,
                LocalMotionAllowed provides false,
            ) {
                SippyTheme(dark = true) {
                    Box(
                        modifier = Modifier
                            .requiredWidth(360.dp)
                            .testTag("settings-root"),
                    ) {
                        SettingsScreen(
                            state = repo.state.value,
                            controller = controller,
                            onOpenDeveloper = {},
                        )
                    }
                }
            }
        }

        for (scale in scales) {
            rule.runOnUiThread { fontScale = scale }
            rule.waitForIdle()
            rule.onNode(hasScrollToIndexAction()).performScrollToIndex(1)
            rule.waitForIdle()
            assertSingleLineInsideRoot("−250", scale, "settings-root", scrollIntoView = true)
            assertSingleLineInsideRoot("+250", scale, "settings-root", scrollIntoView = true)
            rule.onNode(hasScrollToIndexAction()).performScrollToIndex(2)
            rule.waitForIdle()
            listOf("System", "Light", "Dark", "Full", "Short", "Off").forEach { chip ->
                assertSingleLineInsideRoot(chip, scale, "settings-root", scrollIntoView = true)
            }
            rule.onNode(hasScrollToIndexAction()).performScrollToIndex(3)
            rule.waitForIdle()
            assertSingleLineInsideRoot("Save", scale, "settings-root", scrollIntoView = true)
            rule.onNode(hasScrollToIndexAction()).performScrollToIndex(4)
            rule.waitForIdle()
            assertSingleLineInsideRoot("Check for updates", scale, "settings-root", scrollIntoView = true)
        }
    }

    @Test
    fun waterOrbOverGoalCaptionFitsAcrossFontScales() {
        var fontScale by mutableFloatStateOf(1.0f)
        rule.setContent {
            val base = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(base.density, fontScale),
                LocalGlassTier provides GlassTier.FROST,
                LocalMotionAllowed provides false,
            ) {
                SippyTheme(dark = true) {
                    Box(Modifier.testTag("orb-root")) {
                        WaterOrb(
                            hydratedMl = 2400,
                            goalMl = 2000,
                            fraction = 1.2f,
                            animate = false,
                            diameter = 230.dp,
                        )
                    }
                }
            }
        }

        for (scale in scales) {
            rule.runOnUiThread { fontScale = scale }
            rule.waitForIdle()
            listOf("2400", "of 2000 ml", "Goal reached", "+400 ml over").forEach { caption ->
                assertSingleLineInsideRoot(caption, scale, "orb-root")
            }
        }
    }

    private fun assertSingleLineInsideRoot(
        text: String,
        scale: Float,
        rootTag: String,
        scrollIntoView: Boolean = false,
    ) {
        val rootBounds = rule.onNodeWithTag(rootTag).fetchSemanticsNode().boundsInRoot
        val nodeInteraction = rule.onNodeWithText(text, useUnmergedTree = true)
        if (scrollIntoView) {
            nodeInteraction.performScrollTo()
            rule.waitForIdle()
        }
        nodeInteraction.assertIsDisplayed()
        val node = nodeInteraction.fetchSemanticsNode()
        val layouts = mutableListOf<TextLayoutResult>()
        val action = node.config.getOrNull(SemanticsActions.GetTextLayoutResult)?.action
        assertTrue("Missing text layout result for '$text' at scale $scale", action?.invoke(layouts) == true)
        val layout = layouts.first()
        assertEquals("'$text' wrapped onto multiple lines at scale $scale", 1, layout.lineCount)
        assertFalse(
            "'$text' exceeded maxLines at scale $scale",
            layout.multiParagraph.didExceedMaxLines,
        )
        assertFalse(
            "'$text' overflowed vertically at scale $scale (size=${layout.size}, lineB=${layout.getLineBottom(0)})",
            layout.didOverflowHeight,
        )
        val lineWidth = layout.getLineRight(0) - layout.getLineLeft(0)
        assertTrue(
            "'$text' clipped horizontally at scale $scale (lineW=$lineWidth > sizeW=${layout.size.width})",
            lineWidth <= layout.size.width + 1f,
        )
        val bounds = node.boundsInRoot
        assertTrue(
            "'$text' overflowed left of $rootTag at scale $scale (${bounds.left} < ${rootBounds.left})",
            bounds.left >= rootBounds.left - 1f,
        )
        assertTrue(
            "'$text' overflowed right of $rootTag at scale $scale (${bounds.right} > ${rootBounds.right})",
            bounds.right <= rootBounds.right + 1f,
        )
    }

    private class InMemorySippyRepository : SippyRepository {
        private val _state = MutableStateFlow(StoredState())
        override val state: StateFlow<StoredState> = _state.asStateFlow()
        override val ready: StateFlow<Boolean> = MutableStateFlow(true)
        override suspend fun edit(transform: (StoredState) -> StoredState) {
            _state.value = transform(_state.value)
        }
        override suspend fun resetToDefault() {
            _state.value = StoredState()
        }
        override suspend fun logUpdate(line: String) = Unit
        override fun backupFiles(): List<String> = emptyList()
    }
}
