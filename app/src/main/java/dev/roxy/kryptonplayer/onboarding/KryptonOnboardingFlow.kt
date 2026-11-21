package dev.roxy.kryptonplayer.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import dev.roxy.kryptonplayer.core.ui.preview.DayNightPreview
import dev.roxy.kryptonplayer.core.ui.theme.KryptonPlayerTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class OnboardingStep {
    Welcome,
    CinemaSetup,
    Permissions,
    PassFinale,
}

/**
 * Coordinates Krypton Player's 4-stage onboarding sequence:
 * Welcome Hero -> Tailor Your Cinema (Live Preferences) -> Permissions Hub -> Brushed-Silver Pass Finale.
 */
@Composable
fun KryptonOnboardingFlow(
    gesturesEnabled: Boolean,
    lyricsBlurEnabled: Boolean,
    autoPipEnabled: Boolean,
    onGesturesChanged: (Boolean) -> Unit,
    onLyricsBlurChanged: (Boolean) -> Unit,
    onAutoPipChanged: (Boolean) -> Unit,
    onStorageGranted: () -> Unit,
    onComplete: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val history = remember { mutableStateListOf(OnboardingStep.Welcome) }
    var isNavigatingBack by remember { mutableStateOf(false) }
    var isWelcomeHiding by remember { mutableStateOf(false) }

    val currentStep = history.lastOrNull() ?: OnboardingStep.Welcome

    fun goForward(next: OnboardingStep) {
        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        isNavigatingBack = false
        history.add(next)
    }

    fun goBack() {
        if (history.size > 1 && currentStep != OnboardingStep.PassFinale) {
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            isNavigatingBack = true
            isWelcomeHiding = false
            history.removeAt(history.lastIndex)
        }
    }

    BackHandler(enabled = history.size > 1 && currentStep != OnboardingStep.PassFinale) {
        goBack()
    }

    AnimatedContent(
        targetState = currentStep,
        modifier = Modifier.fillMaxSize(),
        transitionSpec = {
            if (isNavigatingBack) {
                (slideInHorizontally(tween(320, easing = FastOutSlowInEasing)) { -it / 4 } +
                    scaleIn(tween(320, easing = FastOutSlowInEasing), initialScale = 0.94f) +
                    fadeIn(tween(240)))
                    .togetherWith(
                        slideOutHorizontally(tween(300, easing = FastOutSlowInEasing)) { it } +
                            fadeOut(tween(220)),
                    )
            } else {
                (slideInHorizontally(tween(340, easing = FastOutSlowInEasing)) { it / 2 } +
                    fadeIn(tween(260)))
                    .togetherWith(
                        slideOutHorizontally(tween(300, easing = FastOutSlowInEasing)) { -it / 4 } +
                            scaleOut(tween(300, easing = FastOutSlowInEasing), targetScale = 0.94f) +
                            fadeOut(tween(220)),
                    )
            }
        },
        label = "kryptonOnboardingStepTransition",
    ) { step ->
        when (step) {
            OnboardingStep.Welcome -> WelcomeHeroStep(
                isHiding = isWelcomeHiding,
                onNext = {
                    if (!isWelcomeHiding) {
                        isWelcomeHiding = true
                        scope.launch {
                            delay(220)
                            goForward(OnboardingStep.CinemaSetup)
                            isWelcomeHiding = false
                        }
                    }
                },
            )

            OnboardingStep.CinemaSetup -> CinemaSetupStep(
                gesturesEnabled = gesturesEnabled,
                lyricsBlurEnabled = lyricsBlurEnabled,
                autoPipEnabled = autoPipEnabled,
                onGesturesChanged = { enabled ->
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onGesturesChanged(enabled)
                },
                onLyricsBlurChanged = { enabled ->
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onLyricsBlurChanged(enabled)
                },
                onAutoPipChanged = { enabled ->
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    onAutoPipChanged(enabled)
                },
                onBack = ::goBack,
                onNext = { goForward(OnboardingStep.Permissions) },
            )

            OnboardingStep.Permissions -> PermissionsSetupStep(
                onStorageGranted = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onStorageGranted()
                },
                onBack = ::goBack,
                onNext = { goForward(OnboardingStep.PassFinale) },
            )

            OnboardingStep.PassFinale -> KryptonPassFinaleStep(
                onFinished = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onComplete()
                },
            )
        }
    }
}

@DayNightPreview
@Composable
private fun KryptonOnboardingFlowPreview() {
    KryptonPlayerTheme {
        KryptonOnboardingFlow(
            gesturesEnabled = true,
            lyricsBlurEnabled = true,
            autoPipEnabled = true,
            onGesturesChanged = {},
            onLyricsBlurChanged = {},
            onAutoPipChanged = {},
            onStorageGranted = {},
            onComplete = {},
        )
    }
}
