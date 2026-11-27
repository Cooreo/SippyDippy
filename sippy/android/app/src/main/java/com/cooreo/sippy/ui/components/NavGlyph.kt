package com.cooreo.sippy.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** The five bottom-bar glyphs. Drawn, so no icon font or image assets. */
enum class NavGlyphKind { HOME, DRINKS, LOG, FRIENDS, SETTINGS }

/**
 * A single stroke icon. The caller owns size, tint and the accessible label (the
 * canvas itself is hidden from TalkBack with [clearAndSetSemantics]), so a tap
 * target's content description is stated once, on the button.
 */
@Composable
fun NavGlyph(
    kind: NavGlyphKind,
    tint: Color,
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    strokeWidth: Dp = 1.9.dp,
) {
    Canvas(
        modifier = modifier
            .size(size)
            .clearAndSetSemantics { },
    ) {
        val w = this.size.width
        val h = this.size.height
        val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)

        fun path(block: Path.() -> Unit) = Path().apply(block)

        when (kind) {
            NavGlyphKind.HOME -> {
                drawPath(
                    path {
                        moveTo(w * 0.12f, h * 0.50f)
                        lineTo(w * 0.50f, h * 0.16f)
                        lineTo(w * 0.88f, h * 0.50f)
                    },
                    tint,
                    style = stroke,
                )
                drawPath(
                    path {
                        moveTo(w * 0.22f, h * 0.46f)
                        lineTo(w * 0.22f, h * 0.84f)
                        lineTo(w * 0.78f, h * 0.84f)
                        lineTo(w * 0.78f, h * 0.46f)
                    },
                    tint,
                    style = stroke,
                )
                drawPath(
                    path {
                        moveTo(w * 0.43f, h * 0.84f)
                        lineTo(w * 0.43f, h * 0.64f)
                        lineTo(w * 0.57f, h * 0.64f)
                        lineTo(w * 0.57f, h * 0.84f)
                    },
                    tint,
                    style = stroke,
                )
            }

            NavGlyphKind.DRINKS -> {
                drawPath(
                    path {
                        moveTo(w * 0.20f, h * 0.18f)
                        lineTo(w * 0.80f, h * 0.18f)
                        lineTo(w * 0.66f, h * 0.86f)
                        lineTo(w * 0.34f, h * 0.86f)
                        close()
                    },
                    tint,
                    style = stroke,
                )
                drawPath(
                    path {
                        moveTo(w * 0.26f, h * 0.56f)
                        lineTo(w * 0.74f, h * 0.56f)
                    },
                    tint,
                    style = stroke,
                )
            }

            NavGlyphKind.LOG -> {
                drawPath(
                    path {
                        moveTo(w * 0.50f, h * 0.22f)
                        lineTo(w * 0.50f, h * 0.78f)
                    },
                    tint,
                    style = stroke,
                )
                drawPath(
                    path {
                        moveTo(w * 0.22f, h * 0.50f)
                        lineTo(w * 0.78f, h * 0.50f)
                    },
                    tint,
                    style = stroke,
                )
            }

            NavGlyphKind.FRIENDS -> {
                drawCircle(tint, radius = w * 0.15f, center = Offset(w * 0.36f, h * 0.36f), style = stroke)
                drawCircle(tint, radius = w * 0.12f, center = Offset(w * 0.72f, h * 0.44f), style = stroke)
                drawPath(
                    path {
                        moveTo(w * 0.14f, h * 0.82f)
                        cubicTo(w * 0.16f, h * 0.60f, w * 0.56f, h * 0.60f, w * 0.58f, h * 0.82f)
                    },
                    tint,
                    style = stroke,
                )
                drawPath(
                    path {
                        moveTo(w * 0.60f, h * 0.80f)
                        cubicTo(w * 0.62f, h * 0.64f, w * 0.90f, h * 0.64f, w * 0.92f, h * 0.80f)
                    },
                    tint,
                    style = stroke,
                )
            }

            NavGlyphKind.SETTINGS -> {
                drawCircle(tint, radius = w * 0.26f, center = Offset(w / 2f, h / 2f), style = stroke)
                repeat(8) { index ->
                    val angle = (index * (PI / 4f)).toFloat()
                    val inner = w * 0.34f
                    val outer = w * 0.46f
                    drawPath(
                        path {
                            moveTo(
                                w / 2f + cos(angle) * inner,
                                h / 2f + sin(angle) * inner,
                            )
                            lineTo(
                                w / 2f + cos(angle) * outer,
                                h / 2f + sin(angle) * outer,
                            )
                        },
                        tint,
                        style = stroke,
                    )
                }
            }
        }
    }
}
