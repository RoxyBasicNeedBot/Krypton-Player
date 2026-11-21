package dev.roxy.kryptonplayer.settings.composables

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Animation types inspired by the animateicons library.
 *
 * Each type maps to a Compose [Animatable]-driven motion:
 * - [SPIN] — gear-like rotation (settings-icon.tsx)
 * - [BOUNCE] — elastic scale pulse (play-icon.tsx)
 * - [SWIPE] — horizontal translate (sliders-horizontal-icon.tsx)
 * - [PULSE] — opacity + scale heartbeat (moon-icon.tsx)
 * - [SHAKE] — rapid subtle rotation ±4° (bell-icon.tsx)
 * - [SLIDE_UP] — translate Y + alpha (rocket-icon.tsx)
 * - [GROW] — scale from small with overshoot (shield-check-icon.tsx)
 * - [FLIP] — 3D rotationX via graphicsLayer
 */
enum class SettingsIconAnimation {
    SPIN,
    BOUNCE,
    SWIPE,
    PULSE,
    SHAKE,
    SLIDE_UP,
    GROW,
    FLIP,
}

/**
 * A composable that wraps a Material [ImageVector] icon with one of
 * several micro-animations.  The animation fires:
 *   1. On first composition (entrance).
 *   2. Whenever [triggerKey] changes to a new value (press replay).
 *
 * Inspired by https://github.com/Avijit07x/animateicons — translated
 * from React/Framer Motion into native Jetpack Compose Animatable APIs.
 */
@Composable
fun AnimatedSettingsIcon(
    icon: ImageVector,
    animationType: SettingsIconAnimation,
    tint: Color,
    modifier: Modifier = Modifier,
    triggerKey: Any = Unit,
) {
    when (animationType) {
        SettingsIconAnimation.SPIN -> SpinIcon(icon, tint, triggerKey, modifier)
        SettingsIconAnimation.BOUNCE -> BounceIcon(icon, tint, triggerKey, modifier)
        SettingsIconAnimation.SWIPE -> SwipeIcon(icon, tint, triggerKey, modifier)
        SettingsIconAnimation.PULSE -> PulseIcon(icon, tint, triggerKey, modifier)
        SettingsIconAnimation.SHAKE -> ShakeIcon(icon, tint, triggerKey, modifier)
        SettingsIconAnimation.SLIDE_UP -> SlideUpIcon(icon, tint, triggerKey, modifier)
        SettingsIconAnimation.GROW -> GrowIcon(icon, tint, triggerKey, modifier)
        SettingsIconAnimation.FLIP -> FlipIcon(icon, tint, triggerKey, modifier)
    }
}

// ── SPIN — rotate 0→360° with slight scale pulse ────────────────────
@Composable
private fun SpinIcon(
    icon: ImageVector,
    tint: Color,
    triggerKey: Any,
    modifier: Modifier,
) {
    val rotation = remember { Animatable(0f) }
    val scale = remember { Animatable(1f) }

    LaunchedEffect(triggerKey) {
        rotation.snapTo(0f)
        scale.snapTo(1f)
        rotation.animateTo(
            360f,
            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        )
    }
    LaunchedEffect(triggerKey) {
        scale.animateTo(
            1.12f,
            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        )
        scale.animateTo(
            1f,
            animationSpec = spring(stiffness = Spring.StiffnessMedium),
        )
    }

    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = tint,
        modifier = modifier
            .size(26.dp)
            .graphicsLayer {
                rotationZ = rotation.value
                scaleX = scale.value
                scaleY = scale.value
            },
    )
}

// ── BOUNCE — elastic scale [1→1.15→0.95→1] ──────────────────────────
@Composable
private fun BounceIcon(
    icon: ImageVector,
    tint: Color,
    triggerKey: Any,
    modifier: Modifier,
) {
    val scale = remember { Animatable(1f) }

    LaunchedEffect(triggerKey) {
        scale.snapTo(0.85f)
        scale.animateTo(
            1.15f,
            animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        )
        scale.animateTo(
            0.95f,
            animationSpec = tween(durationMillis = 150),
        )
        scale.animateTo(
            1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium,
            ),
        )
    }

    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = tint,
        modifier = modifier
            .size(26.dp)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            },
    )
}

// ── SWIPE — horizontal translate [0→3dp→-3dp→0] ─────────────────────
@Composable
private fun SwipeIcon(
    icon: ImageVector,
    tint: Color,
    triggerKey: Any,
    modifier: Modifier,
) {
    val translationX = remember { Animatable(0f) }

    LaunchedEffect(triggerKey) {
        translationX.snapTo(0f)
        translationX.animateTo(
            8f,
            animationSpec = tween(durationMillis = 150, easing = FastOutSlowInEasing),
        )
        translationX.animateTo(
            -8f,
            animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing),
        )
        translationX.animateTo(
            0f,
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        )
    }

    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = tint,
        modifier = modifier
            .size(26.dp)
            .graphicsLayer {
                this.translationX = translationX.value
            },
    )
}

// ── PULSE — scale + opacity heartbeat ────────────────────────────────
@Composable
private fun PulseIcon(
    icon: ImageVector,
    tint: Color,
    triggerKey: Any,
    modifier: Modifier,
) {
    val scale = remember { Animatable(1f) }
    val alpha = remember { Animatable(1f) }

    LaunchedEffect(triggerKey) {
        scale.snapTo(1f)
        alpha.snapTo(1f)
        scale.animateTo(
            1.1f,
            animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        )
        scale.animateTo(
            1f,
            animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        )
    }
    LaunchedEffect(triggerKey) {
        alpha.animateTo(
            0.6f,
            animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        )
        alpha.animateTo(
            1f,
            animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing),
        )
    }

    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = tint,
        modifier = modifier
            .size(26.dp)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                this.alpha = alpha.value
            },
    )
}

// ── SHAKE — rapid rotation ±4° ───────────────────────────────────────
@Composable
private fun ShakeIcon(
    icon: ImageVector,
    tint: Color,
    triggerKey: Any,
    modifier: Modifier,
) {
    val rotation = remember { Animatable(0f) }

    LaunchedEffect(triggerKey) {
        rotation.snapTo(0f)
        rotation.animateTo(-4f, animationSpec = tween(60))
        rotation.animateTo(4f, animationSpec = tween(80))
        rotation.animateTo(-3f, animationSpec = tween(70))
        rotation.animateTo(3f, animationSpec = tween(70))
        rotation.animateTo(-1.5f, animationSpec = tween(60))
        rotation.animateTo(0f, animationSpec = spring(stiffness = Spring.StiffnessHigh))
    }

    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = tint,
        modifier = modifier
            .size(26.dp)
            .graphicsLayer {
                rotationZ = rotation.value
            },
    )
}

// ── SLIDE_UP — translateY + alpha ────────────────────────────────────
@Composable
private fun SlideUpIcon(
    icon: ImageVector,
    tint: Color,
    triggerKey: Any,
    modifier: Modifier,
) {
    val translationY = remember { Animatable(12f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(triggerKey) {
        translationY.snapTo(12f)
        alpha.snapTo(0f)
        translationY.animateTo(
            0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium,
            ),
        )
    }
    LaunchedEffect(triggerKey) {
        alpha.animateTo(
            1f,
            animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        )
    }

    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = tint,
        modifier = modifier
            .size(26.dp)
            .graphicsLayer {
                this.translationY = translationY.value
                this.alpha = alpha.value
            },
    )
}

// ── GROW — scale from 0.6 with overshoot ─────────────────────────────
@Composable
private fun GrowIcon(
    icon: ImageVector,
    tint: Color,
    triggerKey: Any,
    modifier: Modifier,
) {
    val scale = remember { Animatable(0.5f) }

    LaunchedEffect(triggerKey) {
        scale.snapTo(0.5f)
        scale.animateTo(
            1.12f,
            animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        )
        scale.animateTo(
            1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMedium,
            ),
        )
    }

    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = tint,
        modifier = modifier
            .size(26.dp)
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            },
    )
}

// ── FLIP — 3D rotationX via graphicsLayer ────────────────────────────
@Composable
private fun FlipIcon(
    icon: ImageVector,
    tint: Color,
    triggerKey: Any,
    modifier: Modifier,
) {
    val rotationX = remember { Animatable(0f) }

    LaunchedEffect(triggerKey) {
        rotationX.snapTo(0f)
        rotationX.animateTo(
            360f,
            animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        )
    }

    Icon(
        imageVector = icon,
        contentDescription = null,
        tint = tint,
        modifier = modifier
            .size(26.dp)
            .graphicsLayer {
                this.rotationX = rotationX.value
                cameraDistance = 12f * density
            },
    )
}
