package com.yonte.core.designsystem

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.dp

/** Decorative papyrus-inspired mark. Pair it with the visible app name. */
@Composable
fun YonteMark(
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
) {
    Canvas(modifier = modifier.size(32.dp)) {
        val unit = minOf(size.width, size.height) / 32f
        withTransform({
            translate((size.width - 32f * unit) / 2f, (size.height - 32f * unit) / 2f)
            scale(unit, unit, pivot = Offset.Zero)
        }) {
            // Three restrained blades form the upper Y, above its central stem.
            drawPath(
                path = Path().apply {
                    moveTo(16f, 3f)
                    lineTo(19f, 11f)
                    lineTo(16f, 17f)
                    lineTo(13f, 11f)
                    close()
                    moveTo(4f, 7f)
                    lineTo(11f, 10f)
                    lineTo(14f, 18f)
                    lineTo(8f, 15f)
                    close()
                    moveTo(28f, 7f)
                    lineTo(21f, 10f)
                    lineTo(18f, 18f)
                    lineTo(24f, 15f)
                    close()
                },
                color = tint,
            )
            drawLine(tint, Offset(16f, 19f), Offset(16f, 26f), strokeWidth = 2f)
            drawLine(tint, Offset(11f, 28f), Offset(21f, 28f), strokeWidth = 2f)
        }
    }
}
