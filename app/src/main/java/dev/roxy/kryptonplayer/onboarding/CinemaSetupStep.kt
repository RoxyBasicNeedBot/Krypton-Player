package dev.roxy.kryptonplayer.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.roxy.kryptonplayer.R
import dev.roxy.kryptonplayer.core.ui.preview.DayNightPreview
import dev.roxy.kryptonplayer.core.ui.theme.KryptonPlayerTheme
import dev.roxy.kryptonplayer.core.ui.theme.JioHotstarFontFamily
import kotlin.math.abs
import kotlin.math.sin

/**
 * Custom Canvas-drawn cinema engine graphic banner: crisp audio/video timeline bars
 * with crossing bezier playback curves inside a hairline-bordered dark frame.
 */
@Composable
internal fun CinemaEngineBanner(
    modifier: Modifier = Modifier,
) {
    val phase = 0.85f
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(126.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF212121))
            .border(1.dp, Color(0xFF4A4A4A), RoundedCornerShape(16.dp)),
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 30.dp),
        ) {
            val w = size.width
            val h = size.height
            val mid = h / 2f
            val bars = 60
            val gap = w / bars

            for (i in 0 until bars) {
                val x = i * gap + gap / 2f
                val t = i / (bars - 1f)
                val outgoing = (1f - t) * (1f - t)
                val incoming = t * t
                val wave = 0.52f + 0.48f * abs(sin(i * 1.6f + phase * (if (i % 2 == 0) 1f else -1f)))
                val amp = (maxOf(outgoing, incoming) * 0.88f + 0.10f) * wave * mid
                drawLine(
                    color = Color.White.copy(alpha = 0.45f + 0.48f * maxOf(outgoing, incoming)),
                    start = Offset(x, mid - amp),
                    end = Offset(x, mid + amp),
                    strokeWidth = gap * 0.40f,
                    cap = StrokeCap.Round,
                )
            }

            val curveStroke = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round)
            val curveA = Path().apply {
                moveTo(0f, 0f)
                cubicTo(w * 0.44f, 0f, w * 0.56f, h, w, h)
            }
            val curveB = Path().apply {
                moveTo(0f, h)
                cubicTo(w * 0.44f, h, w * 0.56f, 0f, w, 0f)
            }
            drawPath(curveA, Color(0xFF9A9A9A), style = curveStroke)
            drawPath(curveB, Color(0xFFD0D0D0), style = curveStroke)
        }

        Text(
            text = "HDR · 60FPS",
            fontFamily = JioHotstarFontFamily,
            color = KryptonOnbColors.Title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = 10.dp, end = 18.dp),
        )

        Text(
            text = "KRYPTON ENGINE",
            fontFamily = JioHotstarFontFamily,
            color = KryptonOnbColors.Muted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(bottom = 10.dp, start = 18.dp),
        )
    }
}

/**
 * Stage 2 — Tailor Your Cinema:
 * Pairs the custom Canvas cinema waveform banner with live toggles wired directly to PlayerPreferences.
 */
@Composable
internal fun CinemaSetupStep(
    gesturesEnabled: Boolean,
    lyricsBlurEnabled: Boolean,
    autoPipEnabled: Boolean,
    onGesturesChanged: (Boolean) -> Unit,
    onLyricsBlurChanged: (Boolean) -> Unit,
    onAutoPipChanged: (Boolean) -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
) {
    KryptonOnboardingPage(
        badge = {
            KryptonOnbBadge(
                icon = Icons.Rounded.Tune,
                contentDescription = stringResource(R.string.onb_setup_title),
            )
        },
        title = stringResource(R.string.onb_setup_title),
        subtitle = stringResource(R.string.onb_setup_subtitle),
        stepIndex = 0,
        totalSteps = 3,
        footer = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                KryptonOnbSecondaryButton(
                    text = stringResource(R.string.onb_back),
                    onClick = onBack,
                    testTag = "onboarding_setup_back_button",
                    modifier = Modifier.weight(0.42f),
                )
                KryptonOnbPrimaryButton(
                    text = stringResource(R.string.onb_next),
                    onClick = onNext,
                    testTag = "onboarding_setup_next_button",
                    modifier = Modifier.weight(0.58f),
                )
            }
        },
    ) {
        CinemaEngineBanner()

        Spacer(Modifier.height(20.dp))

        KryptonOnbSwitchCard(
            icon = Icons.Rounded.TouchApp,
            title = stringResource(R.string.onb_pref_gestures_title),
            description = stringResource(R.string.onb_pref_gestures_desc),
            checked = gesturesEnabled,
            onCheckedChange = onGesturesChanged,
            testTag = "onboarding_toggle_gestures",
        )

        Spacer(Modifier.height(12.dp))

        KryptonOnbSwitchCard(
            icon = Icons.Rounded.Lyrics,
            title = stringResource(R.string.onb_pref_lyrics_title),
            description = stringResource(R.string.onb_pref_lyrics_desc),
            checked = lyricsBlurEnabled,
            onCheckedChange = onLyricsBlurChanged,
            testTag = "onboarding_toggle_lyrics",
        )

        Spacer(Modifier.height(12.dp))

        KryptonOnbSwitchCard(
            icon = Icons.Rounded.PictureInPictureAlt,
            title = stringResource(R.string.onb_pref_pip_title),
            description = stringResource(R.string.onb_pref_pip_desc),
            checked = autoPipEnabled,
            onCheckedChange = onAutoPipChanged,
            testTag = "onboarding_toggle_pip",
        )
    }
}

@DayNightPreview
@Composable
private fun CinemaSetupStepPreview() {
    KryptonPlayerTheme {
        CinemaSetupStep(
            gesturesEnabled = true,
            lyricsBlurEnabled = true,
            autoPipEnabled = true,
            onGesturesChanged = {},
            onLyricsBlurChanged = {},
            onAutoPipChanged = {},
            onBack = {},
            onNext = {},
        )
    }
}
