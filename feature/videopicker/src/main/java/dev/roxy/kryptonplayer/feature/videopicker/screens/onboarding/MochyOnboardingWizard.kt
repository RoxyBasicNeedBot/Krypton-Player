package dev.roxy.kryptonplayer.feature.videopicker.screens.onboarding

import android.content.Context
import android.net.Uri
import android.os.Build
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.scaleIn
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.withInfiniteAnimationFrameNanos
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import dev.roxy.kryptonplayer.core.ui.R
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/**
 * Mochy Onboarding Design Tokens (1:1 Parity with Mochy OnboardingDesign.kt).
 */
internal object Onb {
    val Page = Color(0xFF1C1C1C)
    val Card = Color(0xFF2C2C2C)
    val CardPressed = Color(0xFF353535)
    val Field = Color(0xFF2C2C2C)
    val Title = Color.White
    val Subtitle = Color(0xFFBDBDBD)
    val Muted = Color(0xFF8C8C8C)
    val Faint = Color(0xFF6A6A6A)
    val ButtonPrimary = Color(0xFF5E5E5E)
    val ButtonSecondary = Color(0xFF353535)
    val Badge = Color(0xFFD9D9D9)

    val MaxContentWidth = 560.dp
    val TopClearance = 36.dp
}

internal val OnbTitleTracking = (-0.04).em
internal val OnbSubtitleTracking = (-0.035).em
internal const val IdCardAspect = 1.89f
internal const val CardMomentMillis = 3000L

private enum class Step {
    Welcome, Profile, Hello, BlurWarning, Auth, Done
}

private val AutoSteps = setOf(Step.Hello, Step.Done)

internal data class CardInfo(
    val username: String,
    val photo: Any?,
    val cardNumber: String,
    val joinedText: String,
)

/**
 * Mochy 1:1 Setup Wizard (Exact SetupWizardScreen.kt parity):
 * - Step 1: Welcome page with morphing green/black radial gradients, spinning vinyl turntable, bold headline
 * - Step 2: Profile page with username field, silver holographic 3D ID card, interactive photo picker
 * - Step 3: Hello page with celebratory falling silver confetti burst (90 particles) around 3D card
 * - Step 4: BlurWarning page with hardware refresh rate probe and phone internals schematic
 * - Step 5: Device Auth Gate (if present)
 * - Step 6: Done page with checkmark badge and finish transition
 */
@Composable
fun MochyOnboardingWizard(
    onFinish: () -> Unit,
    modifier: Modifier = Modifier,
    authStepContent: (@Composable (onComplete: () -> Unit, onSkip: () -> Unit) -> Unit)? = null,
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("krypton_profile_prefs", Context.MODE_PRIVATE) }

    val history = remember { mutableStateListOf(Step.Welcome) }
    var forward by remember { mutableStateOf(true) }
    val step = history.last()

    fun go(next: Step, replaceCurrent: Boolean = false) {
        forward = true
        if (replaceCurrent && history.size > 1) history.removeAt(history.lastIndex)
        history.add(next)
    }

    fun back() {
        if (history.size <= 1 || step in AutoSteps) return
        forward = false
        history.removeAt(history.lastIndex)
    }

    BackHandler(enabled = history.size > 1 && step !in AutoSteps) {
        back()
    }

    // Dynamic Username - fully synchronized with SharedPreferences
    var username by remember {
        mutableStateOf(prefs.getString("username", null)?.takeIf { it.isNotBlank() } ?: "Roxy")
    }

    // Working Profile Photo - persisted via ProfileStore
    var profilePicUri by remember {
        mutableStateOf(prefs.getString("profile_pic_uri", null))
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { pickedUri: Uri? ->
        if (pickedUri != null) {
            val savedPath = ProfileStore.savePhoto(context, pickedUri)
            if (savedPath != null) {
                profilePicUri = savedPath
            }
        }
    }

    fun openPhotoPicker() {
        photoPickerLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }

    val cardNumber = remember {
        val existing = prefs.getString("card_number", null)
        if (existing != null) existing else {
            val gen = (10_000_000_000L + Random.nextLong(89_999_999_999L)).toString()
            prefs.edit().putString("card_number", gen).apply()
            gen
        }
    }
    val joinedText = remember {
        val existing = prefs.getString("joined_text", null)
        if (existing != null) existing else {
            val dateFormat = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
            val gen = "joined in " + dateFormat.format(Date())
            prefs.edit().putString("joined_text", gen).apply()
            gen
        }
    }

    // Hardware blur probe
    var disableBlur by remember { mutableStateOf(prefs.getBoolean("disable_blur", false)) }
    val hardwareSpecs = remember {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as? WindowManager
        val display = wm?.defaultDisplay
        val refreshRate = display?.refreshRate?.roundToInt() ?: 60
        "Display: ${refreshRate}Hz · Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})"
    }

    fun handleComplete() {
        prefs.edit()
            .putString("username", username.trim().ifEmpty { "Roxy" })
            .putString("card_number", cardNumber)
            .putString("joined_text", joinedText)
            .putBoolean("disable_blur", disableBlur)
            .apply()
        onFinish()
    }

    val card = remember(username, profilePicUri, cardNumber, joinedText) {
        CardInfo(
            username = username,
            photo = profilePicUri,
            cardNumber = cardNumber,
            joinedText = joinedText,
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(if (step == Step.Welcome) Color.Black else Onb.Page),
    ) {
        AnimatedContent(
            targetState = step,
            transitionSpec = {
                val auto = targetState in AutoSteps || initialState in AutoSteps
                when {
                    auto -> (fadeIn(tween(420)) + scaleIn(tween(520), initialScale = 0.96f)) togetherWith fadeOut(tween(260))
                    forward -> (slideInHorizontally(tween(380)) { it / 3 } + fadeIn(tween(320))) togetherWith
                        (slideOutHorizontally(tween(380)) { -it / 4 } + fadeOut(tween(220)))
                    else -> (slideInHorizontally(tween(380)) { -it / 3 } + fadeIn(tween(320))) togetherWith
                        (slideOutHorizontally(tween(380)) { it / 4 } + fadeOut(tween(220)))
                }
            },
            label = "onboardingStep",
            modifier = Modifier.fillMaxSize(),
        ) { current ->
            when (current) {
                Step.Welcome -> Box(Modifier.fillMaxSize().background(Color.Black)) {
                    WelcomeStep(
                        onGetStarted = {
                            go(Step.Profile)
                        }
                    )
                }

                Step.Profile -> ProfilePage(
                    username = username,
                    onUsernameChange = {
                        username = it
                        prefs.edit().putString("username", it.trim()).apply()
                    },
                    photo = profilePicUri,
                    onPhotoClick = ::openPhotoPicker,
                    cardNumber = cardNumber,
                    joinedText = joinedText,
                    onNext = {
                        prefs.edit().putString("username", username.trim().ifEmpty { "Roxy" }).apply()
                        go(Step.Hello)
                    },
                )

                Step.Hello -> HelloCardStep(
                    card = card,
                    onFinished = {
                        go(Step.BlurWarning, replaceCurrent = true)
                    },
                )

                Step.BlurWarning -> BlurWarningPage(
                    disableBlur = disableBlur,
                    onDisableBlurChange = {
                        disableBlur = it
                        prefs.edit().putBoolean("disable_blur", it).apply()
                    },
                    detected = hardwareSpecs,
                    onContinue = {
                        if (authStepContent != null) {
                            go(Step.Auth)
                        } else {
                            go(Step.Done)
                        }
                    },
                )

                Step.Auth -> {
                    if (authStepContent != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Onb.Page)
                                .statusBarsPadding()
                                .navigationBarsPadding()
                        ) {
                            authStepContent(
                                { go(Step.Done) },
                                { go(Step.Done) },
                            )
                        }
                    } else {
                        go(Step.Done)
                    }
                }

                Step.Done -> DoneStep(
                    card = card,
                    onFinished = ::handleComplete,
                )
            }
        }
    }
}

/**
 * Mochy 1:1 Welcome Screen (WelcomeStep.kt).
 */
@Composable
private fun WelcomeStep(
    onGetStarted: () -> Unit,
) {
    val infiniteBgTransition = rememberInfiniteTransition(label = "bg_morph")
    val morphProgress by infiniteBgTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bg_morph_progress"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clipToBounds()
    ) {
        // Atmospheric Ambient Background
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center1 = Offset(size.width * 0.2f + (size.width * 0.6f * morphProgress), size.height * 0.3f)
            val center2 = Offset(size.width * 0.8f - (size.width * 0.4f * morphProgress), size.height * 0.7f + (size.height * 0.2f * morphProgress))

            drawRect(Color.Black)

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF0A1F13).copy(alpha = 0.55f), Color.Transparent),
                    center = center1,
                    radius = size.width * 0.8f
                ),
                radius = size.width * 0.8f,
                center = center1
            )

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF07170E).copy(alpha = 0.45f), Color.Transparent),
                    center = center2,
                    radius = size.width * 0.85f
                ),
                radius = size.width * 0.85f,
                center = center2
            )
        }

        // Turntable Vinyl Disc Animation
        TurntableDiscAnimation(modifier = Modifier.fillMaxSize())

        // Bottom Scrim Gradient
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .fillMaxHeight(0.52f)
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.4f to Color(0xFF121212).copy(alpha = 0.85f),
                        0.8f to Color(0xFF121212),
                        1f to Color(0xFF121212)
                    )
                )
        )

        // Bold Typography & Next Button
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(start = 28.dp, end = 28.dp, bottom = 24.dp)
        ) {
            Text(
                text = "Welcome to\nmochy!",
                fontSize = 42.sp,
                lineHeight = 44.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                letterSpacing = (-1).sp
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "A way to listen and own your\nmusic, differently.",
                fontSize = 16.sp,
                color = Color(0xFFB3B3B3),
                lineHeight = 22.sp,
                letterSpacing = (-0.5).sp
            )
            Spacer(modifier = Modifier.height(28.dp))

            OnbButton(
                text = "Next",
                onClick = onGetStarted,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Vinyl Disc Turntable Animation (from Mochy WelcomeStep.kt DiscAnimation).
 */
@Composable
private fun TurntableDiscAnimation(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "InfiniteDisc")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "DiscRotation"
    )

    BoxWithConstraints(modifier = modifier) {
        val canvasWidth = constraints.maxWidth.toFloat()
        val canvasHeight = constraints.maxHeight.toFloat()

        Canvas(modifier = Modifier.fillMaxSize()) {
            val discCenter = Offset(canvasWidth * 0.95f, canvasHeight * 0.32f)
            val discRadius = canvasWidth * 0.72f

            // 1. Vinyl Grooves Outer Body
            drawCircle(
                color = Color(0xFF111114),
                radius = discRadius,
                center = discCenter
            )

            // 2. Grooves Lines (Micro-rings)
            rotate(rotation, pivot = discCenter) {
                for (r in (discRadius * 0.38f).toInt()..(discRadius * 0.96f).toInt() step 12) {
                    drawCircle(
                        color = Color.White.copy(alpha = if (r % 24 == 0) 0.05f else 0.02f),
                        radius = r.toFloat(),
                        center = discCenter,
                        style = Stroke(width = 1.5f)
                    )
                }

                // Vinyl sheen highlight reflection
                drawCircle(
                    brush = Brush.sweepGradient(
                        0.0f to Color.Transparent,
                        0.25f to Color.White.copy(alpha = 0.07f),
                        0.5f to Color.Transparent,
                        0.75f to Color.White.copy(alpha = 0.07f),
                        1.0f to Color.Transparent,
                        center = discCenter
                    ),
                    radius = discRadius,
                    center = discCenter
                )

                // 3. Center Label (Emerald/Silver album circle)
                drawCircle(
                    brush = Brush.linearGradient(
                        colors = listOf(Color(0xFF2E7D32), Color(0xFF1B5E20)),
                        start = Offset(discCenter.x - discRadius * 0.35f, discCenter.y - discRadius * 0.35f),
                        end = Offset(discCenter.x + discRadius * 0.35f, discCenter.y + discRadius * 0.35f)
                    ),
                    radius = discRadius * 0.32f,
                    center = discCenter
                )

                // 4. Center Spindle hole
                drawCircle(
                    color = Color.Black,
                    radius = discRadius * 0.06f,
                    center = discCenter
                )
            }

            // 5. Tonearm Needle
            val armBase = Offset(canvasWidth * 0.22f, canvasHeight * 0.12f)
            val armHead = Offset(discCenter.x - discRadius * 0.42f, discCenter.y - discRadius * 0.15f)

            drawLine(
                color = Color(0xFF9E9E9E),
                start = armBase,
                end = armHead,
                strokeWidth = 5f
            )
            drawCircle(
                color = Color(0xFFD4D4D4),
                radius = 12f,
                center = armBase
            )
            drawCircle(
                color = Color(0xFFE0E0E0),
                radius = 8f,
                center = armHead
            )
        }
    }
}

/**
 * Mochy 1:1 Profile Page (ProfilePage in Mochy).
 */
@Composable
private fun ProfilePage(
    username: String,
    onUsernameChange: (String) -> Unit,
    photo: Any?,
    onPhotoClick: () -> Unit,
    cardNumber: String,
    joinedText: String,
    onNext: () -> Unit,
) {
    OnboardingPage(
        badge = { PageBadge(icon = painterResource(id = R.drawable.person)) },
        title = "Let's create your profile!",
        footer = {
            Text(
                text = "Mochy will never store your data online!",
                color = Onb.Faint,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 4.dp),
            )
            OnbButton(
                text = "Next",
                onClick = onNext,
                enabled = username.isNotBlank(),
            )
        },
    ) {
        Text("what's your username?", color = Onb.Muted, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(10.dp))
        OnbTextField(
            value = username,
            onValueChange = { onUsernameChange(it.take(32)) },
            placeholder = "username"
        )
        Spacer(Modifier.height(24.dp))
        Text("Set a profile picture here!", color = Onb.Muted, fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))

        ProfileIdCard(
            username = username,
            photo = photo,
            onPhotoClick = onPhotoClick,
            cardNumber = cardNumber,
            joinedText = joinedText,
            silver = true,
        )
    }
}

/**
 * Mochy 1:1 Celebratory Hello Card Page (HelloCardStep in Mochy).
 * - Silver Confetti burst falling around 3D card
 * - Expanding light ripple rings
 * - Giant typography: "Hello!"
 * - Automatically advances after 3 seconds
 */
@Composable
private fun HelloCardStep(
    card: CardInfo,
    onFinished: () -> Unit,
) {
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val currentOnFinished by rememberUpdatedState(onFinished)

    var time by remember { mutableFloatStateOf(0f) }
    val tiltX = remember { Animatable(0f) }
    val tiltY = remember { Animatable(0f) }
    val confetti = remember { Confetti.burst(count = 90, seed = card.cardNumber.hashCode()) }

    LaunchedEffect(Unit) {
        var start = 0L
        while (time * 1000f < CardMomentMillis) {
            withFrameNanos { now ->
                if (start == 0L) start = now
                time = (now - start) / 1_000_000_000f
            }
        }
        currentOnFinished()
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.radialGradient(listOf(Color(0xFF2A2A2A), Color(0xFF0E0E0E)), radius = 1400f))
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragEnd = {
                        scope.launch { tiltX.animateTo(0f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessLow)) }
                        scope.launch { tiltY.animateTo(0f, spring(dampingRatio = 0.45f, stiffness = Spring.StiffnessLow)) }
                    },
                ) { change, drag ->
                    change.consume()
                    scope.launch { tiltY.snapTo((tiltY.value + drag.x * 0.12f).coerceIn(-35f, 35f)) }
                    scope.launch { tiltX.snapTo((tiltX.value - drag.y * 0.12f).coerceIn(-35f, 35f)) }
                }
            },
    ) {
        val cardWidth = min(maxWidth.value * 0.86f, 420f).dp
        val cardCenterFromTop = 0.42f

        // Ripple ring
        Canvas(modifier = Modifier.fillMaxSize()) {
            val c = Offset(size.width / 2f, size.height * cardCenterFromTop)
            val far = hypot(size.width, size.height)
            val p = easeOut((time / 0.9f).coerceIn(0f, 1f))
            if (p < 1f) drawCircle(Color.White.copy(alpha = 0.10f * (1f - p)), radius = far * p, center = c)
            val ring = easeOut(((time - 0.12f) / 1.1f).coerceIn(0f, 1f))
            if (ring in 0.001f..0.999f) {
                drawCircle(
                    Color.White.copy(alpha = 0.35f * (1f - ring)),
                    radius = far * 0.75f * ring,
                    center = c,
                    style = Stroke(width = 3.dp.toPx() * (1f - ring) + 1f),
                )
            }
        }

        // Falling confetti burst
        Canvas(modifier = Modifier.fillMaxSize()) {
            confetti.draw(this, origin = Offset(size.width / 2f, size.height * cardCenterFromTop), time = time - 0.15f)
        }

        // Floating 3D card
        ProfileIdCard(
            username = card.username,
            photo = card.photo,
            cardNumber = card.cardNumber,
            joinedText = card.joinedText,
            silver = true,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = (maxHeight * cardCenterFromTop - (cardWidth / IdCardAspect) / 2).coerceAtLeast(0.dp))
                .width(cardWidth)
                .graphicsLayer {
                    val enter = easeOutBack((time / 0.75f).coerceIn(0f, 1f))
                    val s = 0.6f + 0.4f * enter
                    scaleX = s
                    scaleY = s
                    alpha = (time / 0.25f).coerceIn(0f, 1f)
                    rotationZ = -8f * enter
                    rotationY = (1f - enter) * -55f + tiltY.value + cos(time * 1.4f) * 7f
                    rotationX = tiltX.value + sin(time * 1.1f) * 4f
                    cameraDistance = 14f * density.density
                },
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .padding(start = 28.dp, end = 28.dp, bottom = 40.dp)
                .graphicsLayer {
                    val p = easeOut(((time - 0.35f) / 0.6f).coerceIn(0f, 1f))
                    alpha = p
                    translationY = (1f - p) * 40.dp.toPx()
                },
        ) {
            Text("Hello!", color = Color.White, fontSize = 64.sp, lineHeight = 66.sp, fontWeight = FontWeight.Bold, letterSpacing = (-2).sp)
            Text("welcome to mochy!", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

/**
 * Mochy 1:1 Blur Warning Page (BlurWarningPage in Mochy).
 */
@Composable
private fun BlurWarningPage(
    disableBlur: Boolean,
    onDisableBlurChange: (Boolean) -> Unit,
    detected: String?,
    onContinue: () -> Unit,
) {
    OnboardingPage(
        badge = { PageBadge(icon = painterResource(id = R.drawable.warning)) },
        title = "Warning",
        subtitle = "Your phone may be too weak to run blurs. Do you want to disable blur? You can always change this in Settings.",
        footer = {
            OnbButton(
                text = if (disableBlur) "continue" else "no, continue",
                onClick = onContinue,
            )
        },
    ) {
        OnbToggleCard(
            title = "Disable Blur",
            description = detected?.let { "Measured: $it" } ?: "",
            checked = disableBlur,
            onCheckedChange = onDisableBlurChange,
            leading = { Box(Modifier.size(22.dp).clip(CircleShape).background(Onb.Badge)) },
        )
        Spacer(Modifier.height(20.dp))
        Image(
            painter = painterResource(id = R.drawable.onboarding_phone_internals),
            contentDescription = null,
            contentScale = ContentScale.FillWidth,
            alignment = Alignment.TopCenter,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp),
        )
    }
}

/**
 * Mochy 1:1 Done Step (DoneStep in Mochy).
 */
@Composable
private fun DoneStep(
    card: CardInfo,
    onFinished: () -> Unit,
) {
    var time by remember { mutableFloatStateOf(0f) }
    val currentOnFinished by rememberUpdatedState(onFinished)
    LaunchedEffect(Unit) {
        var start = 0L
        while (time * 1000f < CardMomentMillis) {
            withFrameNanos { now ->
                if (start == 0L) start = now
                time = (now - start) / 1_000_000_000f
            }
        }
        currentOnFinished()
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(Onb.Page)) {
        val cardWidth = min(maxWidth.value * 0.86f, 420f).dp

        Canvas(modifier = Modifier.fillMaxSize()) {
            val ring = easeOut(((time - 0.1f) / 1.0f).coerceIn(0f, 1f))
            if (ring in 0.001f..0.999f) {
                drawCircle(
                    Color.White.copy(alpha = 0.22f * (1f - ring)),
                    radius = size.width * 0.9f * ring,
                    center = Offset(size.width / 2f, size.height / 2f),
                    style = Stroke(width = 2.dp.toPx()),
                )
            }
        }

        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .graphicsLayer {
                        val s = easeOutBack((time / 0.5f).coerceIn(0f, 1f))
                        scaleX = s
                        scaleY = s
                    }
                    .clip(CircleShape)
                    .background(Onb.Badge),
                contentAlignment = Alignment.Center,
            ) {
                Icon(painterResource(id = R.drawable.check), contentDescription = null, tint = Color(0xFF1C1C1C), modifier = Modifier.size(34.dp))
            }
            Spacer(Modifier.height(18.dp))
            Text(
                "You're good to go",
                color = Onb.Subtitle,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.graphicsLayer { alpha = ((time - 0.2f) / 0.4f).coerceIn(0f, 1f) },
            )
            Spacer(Modifier.height(36.dp))
            ProfileIdCard(
                username = card.username,
                photo = card.photo,
                cardNumber = card.cardNumber,
                joinedText = card.joinedText,
                silver = true,
                modifier = Modifier
                    .width(cardWidth)
                    .graphicsLayer {
                        val p = easeOutBack(((time - 0.15f) / 0.8f).coerceIn(0f, 1f))
                        translationY = (1f - p) * 120f
                        alpha = ((time - 0.15f) / 0.3f).coerceIn(0f, 1f)
                        rotationZ = -6f + (1f - p) * 10f
                        rotationY = sin(time * 1.2f) * 6f
                    },
            )
        }
    }
}

/**
 * Mochy 1:1 OnboardingPage Container (OnboardingDesign.kt).
 */
@Composable
internal fun OnboardingPage(
    badge: @Composable () -> Unit,
    title: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier,
    scrollable: Boolean = true,
    contentTop: Dp = Onb.TopClearance + 18.dp,
    footer: @Composable ColumnScope.() -> Unit = {},
    content: @Composable ColumnScope.() -> Unit = {},
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize().background(Onb.Page)) {
        val side = if (maxWidth < 340.dp) 20.dp else 28.dp
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = Onb.MaxContentWidth)
                .fillMaxSize(),
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                    .padding(horizontal = side)
                    .padding(top = contentTop, bottom = 16.dp),
            ) {
                badge()
                Spacer(Modifier.height(22.dp))
                Text(
                    text = title,
                    color = Onb.Title,
                    fontSize = 34.sp,
                    lineHeight = 38.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = OnbTitleTracking,
                )
                if (subtitle != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = subtitle,
                        color = Onb.Subtitle,
                        fontSize = 18.sp,
                        lineHeight = 23.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = OnbSubtitleTracking,
                    )
                }
                Spacer(Modifier.height(22.dp))
                content()
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = side)
                    .padding(bottom = 18.dp, top = 6.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                content = footer,
            )
        }
    }
}

/**
 * Mochy 1:1 PageBadge (OnboardingDesign.kt).
 */
@Composable
internal fun PageBadge(icon: Painter, size: Dp = 40.dp) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Onb.Badge),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Color(0xFF1C1C1C), modifier = Modifier.size(size * 0.52f))
    }
}

internal enum class OnbButtonStyle { Primary, Secondary }

/**
 * Mochy 1:1 Pill Button (OnboardingDesign.kt).
 */
@Composable
internal fun OnbButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: OnbButtonStyle = OnbButtonStyle.Primary,
    enabled: Boolean = true,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 50.dp)
            .graphicsLayer {
                val s = if (pressed && enabled) 0.97f else 1f
                scaleX = s
                scaleY = s
                alpha = if (enabled) 1f else 0.45f
            }
            .clip(CircleShape)
            .background(if (style == OnbButtonStyle.Primary) Onb.ButtonPrimary else Onb.ButtonSecondary)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = if (style == OnbButtonStyle.Primary) Color.White else Onb.Muted,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * Mochy 1:1 OnbTextField (OnboardingDesign.kt).
 */
@Composable
internal fun OnbTextField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold, letterSpacing = (-0.03).em),
        cursorBrush = SolidColor(Color.White),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
        modifier = modifier,
        decorationBox = { inner ->
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(CircleShape)
                    .background(Onb.Field)
                    .padding(horizontal = 20.dp, vertical = 14.dp),
            ) {
                if (value.isEmpty()) Text(placeholder, color = Onb.Faint, fontSize = 16.sp)
                inner()
            }
        },
    )
}

/**
 * Mochy 1:1 OnbCard (OnboardingDesign.kt).
 */
@Composable
internal fun OnbCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: RoundedCornerShape = RoundedCornerShape(24.dp),
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    Box(
        modifier = modifier
            .graphicsLayer {
                val s = if (pressed) 0.985f else 1f
                scaleX = s
                scaleY = s
            }
            .clip(shape)
            .background(if (pressed) Onb.CardPressed else Onb.Card)
            .then(
                if (onClick != null) Modifier.clickable(interactionSource = interaction, indication = null, onClick = onClick)
                else Modifier
            ),
    ) {
        content()
    }
}

/**
 * Mochy 1:1 OnbToggleCard (OnboardingDesign.kt).
 */
@Composable
internal fun OnbToggleCard(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    leading: (@Composable () -> Unit)? = null,
    header: (@Composable () -> Unit)? = null,
) {
    OnbCard(modifier = modifier.fillMaxWidth(), onClick = { onCheckedChange(!checked) }) {
        Column(modifier = Modifier.padding(18.dp)) {
            if (header != null) {
                header()
                Spacer(Modifier.height(14.dp))
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (leading != null) {
                    leading()
                    Spacer(Modifier.width(14.dp))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    if (description.isNotEmpty()) {
                        Spacer(Modifier.height(3.dp))
                        Text(description, color = Onb.Muted, fontSize = 11.sp, lineHeight = 14.sp)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Switch(
                    checked = checked,
                    onCheckedChange = onCheckedChange,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Onb.ButtonPrimary,
                        uncheckedThumbColor = Color(0xFF888888),
                        uncheckedTrackColor = Color(0xFF383838)
                    )
                )
            }
        }
    }
}

/**
 * Mochy Profile ID Card (1:1 Parity with Mochy ProfileIdCard.kt).
 */
@Composable
internal fun ProfileIdCard(
    username: String,
    cardNumber: String,
    joinedText: String,
    modifier: Modifier = Modifier,
    silver: Boolean = true,
    photo: Any? = null,
    onPhotoClick: (() -> Unit)? = null,
    onClick: (() -> Unit)? = null,
) {
    val name = username.trim().ifEmpty { "Roxy" }
    val shape = RoundedCornerShape(22.dp)
    val bodyText = if (silver) Color(0xFF111114) else Color.White
    val mutedText = if (silver) Color(0xFF3A3A40) else Color(0xFFB5B5B5)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .aspectRatio(IdCardAspect)
            .clip(shape)
            .silverSurface()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
    ) {
        val unit = maxWidth / 332f

        Row(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(start = unit * 20f, top = unit * 18f, end = unit * 12f, bottom = unit * 16f),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Row: User Number Monospace
                Text(
                    text = "PROFILE USR#$cardNumber",
                    color = mutedText,
                    fontFamily = FontFamily.Monospace,
                    fontSize = (unit.value * 11f).sp,
                    maxLines = 1
                )

                // Middle: Avatar + Name Details
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(unit * 84f)
                            .clip(CircleShape)
                            .background(if (silver) Color(0xFF5A5A60) else Color(0xFF4F4F4F))
                            .then(if (onPhotoClick != null) Modifier.clickable(onClick = onPhotoClick) else Modifier),
                        contentAlignment = Alignment.Center
                    ) {
                        if (photo != null) {
                            val imageModel = if (photo is String && !photo.contains("://")) "file://$photo" else photo
                            AsyncImage(
                                model = imageModel,
                                contentDescription = "Profile avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                painter = painterResource(id = R.drawable.add),
                                contentDescription = "Add profile photo",
                                tint = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.size(unit * 32f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(unit * 14f))

                    Column {
                        Text(
                            text = "MEMBER OF MOCHY",
                            color = mutedText,
                            fontSize = (unit.value * 9.5f).sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                        Spacer(modifier = Modifier.height(unit * 2f))
                        Text(
                            text = name,
                            color = bodyText,
                            fontSize = (unit.value * 23f).sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(unit * 2f))
                        Text(
                            text = joinedText,
                            color = mutedText,
                            fontSize = (unit.value * 10f).sp
                        )
                    }
                }

                // Bottom Row: Hologram micro text & barcode lines
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "SECURITY PASS · VERIFIED AUDIO",
                        color = mutedText.copy(alpha = 0.7f),
                        fontFamily = FontFamily.Monospace,
                        fontSize = (unit.value * 8.5f).sp
                    )

                    // Barcode
                    Canvas(
                        modifier = Modifier
                            .width(unit * 72f)
                            .height(unit * 18f)
                    ) {
                        val barWidth = 1.8f
                        var x = 0f
                        val random = Random(cardNumber.hashCode())
                        while (x < size.width) {
                            val isThick = random.nextBoolean()
                            val w = if (isThick) barWidth * 2.2f else barWidth
                            drawRect(
                                color = mutedText,
                                topLeft = Offset(x, 0f),
                                size = Size(w, size.height)
                            )
                            x += w + (if (random.nextBoolean()) 2.5f else 4f)
                        }
                    }
                }
            }

            // Right Edge: Vertical Marquee Strip
            Box(
                modifier = Modifier
                    .width(unit * 52f)
                    .fillMaxHeight()
                    .background(Color.White.copy(alpha = if (silver) 0.18f else 0.08f))
            ) {
                VerticalCardMarquee(
                    text = "MOCHY AUDIO • PASS • 2026 • HIGH FIDELITY • ",
                    textColor = mutedText,
                    fontSize = (unit.value * 9.5f).sp
                )
            }
        }
    }
}

/**
 * Infinite looping vertical marquee text on the right strip of ProfileIdCard.
 */
@Composable
private fun VerticalCardMarquee(
    text: String,
    textColor: Color,
    fontSize: androidx.compose.ui.unit.TextUnit,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "marquee")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -300f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "marquee_scroll"
    )

    val textMeasurer = rememberTextMeasurer()

    Canvas(modifier = Modifier.fillMaxSize()) {
        val measured = textMeasurer.measure(
            text = text.repeat(8),
            style = TextStyle(
                color = textColor,
                fontSize = fontSize,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                letterSpacing = 2.sp
            )
        )

        rotate(90f, pivot = Offset(size.width / 2f, size.height / 2f)) {
            translate(left = -size.height / 2f, top = offsetY % measured.size.height) {
                drawText(measured)
            }
        }
    }
}

/**
 * Brushed Silver Surface with Holographic sheen.
 */
private fun Modifier.silverSurface(): Modifier = this.drawWithCache {
    val baseSilver = Brush.linearGradient(
        colors = listOf(
            Color(0xFFE4E5EB),
            Color(0xFFD0D2DA),
            Color(0xFFBCC0CA),
            Color(0xFFDCDCDE),
            Color(0xFFECEEF2)
        ),
        start = Offset(0f, 0f),
        end = Offset(size.width, size.height)
    )

    val holographicSheen = Brush.linearGradient(
        0.0f to Color.Transparent,
        0.30f to Color(0x33FFB3BA),
        0.50f to Color(0x4DBAE1FF),
        0.70f to Color(0x33BFFCC6),
        1.0f to Color.Transparent,
        start = Offset(0f, size.height),
        end = Offset(size.width, 0f)
    )

    onDrawBehind {
        drawRect(baseSilver)
        for (y in 0..size.height.toInt() step 4) {
            drawLine(
                color = Color.Black.copy(alpha = if (y % 8 == 0) 0.025f else 0.012f),
                start = Offset(0f, y.toFloat()),
                end = Offset(size.width, y.toFloat()),
                strokeWidth = 1f
            )
        }
        drawRect(holographicSheen, blendMode = BlendMode.Screen)
    }
}

private fun easeOut(t: Float): Float = 1f - (1f - t) * (1f - t) * (1f - t)

private fun easeOutBack(t: Float): Float {
    val c1 = 1.4f
    val c3 = c1 + 1f
    val x = t - 1f
    return 1f + c3 * x * x * x + c1 * x * x
}

/**
 * Silver Confetti burst falling around 3D holographic card (1:1 with Mochy CardMoments.kt).
 */
private class Confetti private constructor(private val pieces: List<Piece>) {
    private class Piece(
        val vx: Float,
        val vy: Float,
        val spin: Float,
        val flutter: Float,
        val w: Float,
        val h: Float,
        val round: Boolean,
        val color: Color,
    )

    fun draw(scope: androidx.compose.ui.graphics.drawscope.DrawScope, origin: Offset, time: Float) = with(scope) {
        if (time <= 0f) return@with
        val unit = size.minDimension / 400f
        val gravity = 900f * unit
        pieces.forEach { p ->
            val x = origin.x + p.vx * unit * time + sin(time * p.flutter) * 10f * unit
            val y = origin.y + p.vy * unit * time + 0.5f * gravity * time * time
            val fade = (1f - ((time - 1.9f) / 0.9f)).coerceIn(0f, 1f)
            if (fade <= 0f || y > size.height + 20f) return@forEach
            val w = p.w * unit
            val h = p.h * unit * (0.35f + 0.65f * kotlin.math.abs(cos(time * p.flutter)))
            rotate(degrees = p.spin * time, pivot = Offset(x, y)) {
                if (p.round) {
                    drawCircle(p.color.copy(alpha = p.color.alpha * fade), radius = w / 2f, center = Offset(x, y))
                } else {
                    drawRect(p.color.copy(alpha = p.color.alpha * fade), topLeft = Offset(x - w / 2f, y - h / 2f), size = Size(w, h))
                }
            }
        }
    }

    companion object {
        private val palette = listOf(
            Color.White,
            Color(0xFFE9EAEE),
            Color(0xFFC4C6CC),
            Color(0xFF9EA1A8),
            Color(0xFFF5F5F7),
            Color(0xFF7D8088),
        )

        fun burst(count: Int, seed: Int): Confetti {
            val random = Random(seed)
            val pieces = List(count) {
                val angle = (-PI / 2 + (random.nextFloat() - 0.5f) * PI * 1.25f).toFloat()
                val speed = 260f + random.nextFloat() * 420f
                Piece(
                    vx = cos(angle) * speed,
                    vy = sin(angle) * speed,
                    spin = (random.nextFloat() - 0.5f) * 720f,
                    flutter = 4f + random.nextFloat() * 8f,
                    w = 5f + random.nextFloat() * 6f,
                    h = 3f + random.nextFloat() * 4f,
                    round = random.nextFloat() < 0.25f,
                    color = palette[random.nextInt(palette.size)],
                )
            }
            return Confetti(pieces)
        }
    }
}
