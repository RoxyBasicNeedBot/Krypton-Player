package dev.roxy.kryptonplayer.feature.videopicker.composables.highlights

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

/**
 * Handcrafted, crystal-clear line SVG icons matching JioHotstar's aesthetic.
 */
object HighlightIcons {

    val SparkleStar: ImageVector by lazy {
        ImageVector.Builder(
            name = "SparkleStar",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.White),
                fillAlpha = 1f,
                stroke = null,
                pathFillType = PathFillType.NonZero
            ) {
                moveTo(12f, 2f)
                curveTo(12f, 7.52f, 16.48f, 12f, 22f, 12f)
                curveTo(16.48f, 12f, 12f, 16.48f, 12f, 22f)
                curveTo(12f, 16.48f, 7.52f, 12f, 2f, 12f)
                curveTo(7.52f, 12f, 12f, 7.52f, 12f, 2f)
                close()
            }
        }.build()
    }

    val Flame: ImageVector by lazy {
        ImageVector.Builder(
            name = "Flame",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(12f, 22f)
                curveTo(16.97f, 22f, 20f, 18.5f, 20f, 14f)
                curveTo(20f, 9.5f, 16f, 6.5f, 14.5f, 2f)
                curveTo(14.5f, 4.5f, 12.5f, 6.5f, 11f, 8f)
                curveTo(9.5f, 9.5f, 8f, 11.5f, 8f, 14f)
                curveTo(8f, 18.5f, 11.03f, 22f, 12f, 22f)
                close()
            }
        }.build()
    }

    val Clapperboard: ImageVector by lazy {
        ImageVector.Builder(
            name = "Clapperboard",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(4f, 8f)
                lineTo(20f, 8f)
                lineTo(20f, 19f)
                curveTo(20f, 20.1f, 19.1f, 21f, 18f, 21f)
                lineTo(6f, 21f)
                curveTo(4.9f, 21f, 4f, 20.1f, 4f, 19f)
                close()
                moveTo(4f, 8f)
                lineTo(4f, 5f)
                curveTo(4f, 3.9f, 4.9f, 3f, 6f, 3f)
                lineTo(18f, 3f)
                curveTo(19.1f, 3f, 20f, 3.9f, 20f, 5f)
                lineTo(20f, 8f)
                close()
                moveTo(8.5f, 3f)
                lineTo(10.5f, 8f)
                moveTo(13.5f, 3f)
                lineTo(15.5f, 8f)
            }
        }.build()
    }

    val Newspaper: ImageVector by lazy {
        ImageVector.Builder(
            name = "Newspaper",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(19f, 20f)
                horizontalLineTo(5f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 3f, 18f)
                verticalLineTo(6f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 5f, 4f)
                horizontalLineTo(19f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 21f, 6f)
                verticalLineTo(18f)
                arcTo(2f, 2f, 0f, isMoreThanHalf = false, isPositiveArc = true, 19f, 20f)
                close()
                moveTo(7f, 8f)
                horizontalLineTo(17f)
                moveTo(7f, 12f)
                horizontalLineTo(17f)
                moveTo(7f, 16f)
                horizontalLineTo(13f)
            }
        }.build()
    }

    val PlayRound: ImageVector by lazy {
        ImageVector.Builder(
            name = "PlayRound",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = SolidColor(Color.White),
                stroke = null
            ) {
                moveTo(9f, 6.5f)
                verticalLineTo(17.5f)
                lineTo(17.5f, 12f)
                close()
            }
        }.build()
    }

    val ShareCurve: ImageVector by lazy {
        ImageVector.Builder(
            name = "ShareCurve",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 1.8f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(15f, 8f)
                lineTo(19f, 4f)
                moveTo(19f, 4f)
                horizontalLineTo(14f)
                moveTo(19f, 4f)
                verticalLineTo(9f)
                moveTo(10f, 14f)
                lineTo(4f, 20f)
                moveTo(4f, 15f)
                verticalLineTo(20f)
                horizontalLineTo(9f)
            }
        }.build()
    }

    val CloseCircle: ImageVector by lazy {
        ImageVector.Builder(
            name = "CloseCircle",
            defaultWidth = 24.dp,
            defaultHeight = 24.dp,
            viewportWidth = 24f,
            viewportHeight = 24f
        ).apply {
            path(
                fill = null,
                stroke = SolidColor(Color.White),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            ) {
                moveTo(18f, 6f)
                lineTo(6f, 18f)
                moveTo(6f, 6f)
                lineTo(18f, 18f)
            }
        }.build()
    }
}
