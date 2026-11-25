package dev.roxy.kryptonplayer.onboarding

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddAPhoto
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import dev.roxy.kryptonplayer.R
import dev.roxy.kryptonplayer.core.ui.preview.DayNightPreview
import dev.roxy.kryptonplayer.core.ui.theme.JioHotstarFontFamily
import dev.roxy.kryptonplayer.core.ui.theme.KryptonPlayerTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
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
            .height(104.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF212121))
            .border(1.dp, Color(0xFF4A4A4A), RoundedCornerShape(16.dp)),
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp, vertical = 22.dp),
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
 * User Profile Card in Onboarding — allows the user to pick their profile photo and enter their display name,
 * saving directly to `krypton_profile_prefs` so it appears on the Krypton Pass ID Card, Home top bar, and Krypton Hub.
 */
@Composable
internal fun OnboardingUserProfileCard() {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val profilePrefs = remember { context.getSharedPreferences("krypton_profile_prefs", Context.MODE_PRIVATE) }

    var username by remember {
        mutableStateOf(profilePrefs.getString("username", "") ?: "")
    }
    var profilePicUri by remember {
        mutableStateOf(profilePrefs.getString("profile_pic_uri", null))
    }

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                val savedPath = withContext(Dispatchers.IO) {
                    runCatching {
                        val destFile = File(context.filesDir, "user_profile_avatar_${System.currentTimeMillis()}.jpg")
                        context.contentResolver.openInputStream(uri)?.use { input ->
                            FileOutputStream(destFile).use { output ->
                                input.copyTo(output)
                            }
                        }
                        // Clean up old avatar files
                        context.filesDir.listFiles()?.forEach { file ->
                            if (file.name.startsWith("user_profile_avatar_") && file.absolutePath != destFile.absolutePath) {
                                runCatching { file.delete() }
                            }
                        }
                        destFile.absolutePath
                    }.getOrNull() ?: uri.toString()
                }
                profilePicUri = savedPath
                profilePrefs.edit().putString("profile_pic_uri", savedPath).apply()
            }
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("onboarding_profile_card"),
        shape = RoundedCornerShape(18.dp),
        color = KryptonOnbColors.Card,
        border = BorderStroke(1.dp, KryptonOnbColors.CardBorder),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Circular Profile Photo Picker
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(KryptonOnbColors.CardElevated)
                    .border(1.2.dp, Color.White.copy(alpha = 0.28f), CircleShape)
                    .clickable {
                        photoPickerLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    }
                    .testTag("onboarding_profile_avatar_picker"),
                contentAlignment = Alignment.Center,
            ) {
                if (!profilePicUri.isNullOrBlank()) {
                    val model = if (profilePicUri!!.contains("://")) profilePicUri else "file://$profilePicUri"
                    AsyncImage(
                        model = model,
                        contentDescription = "Profile Photo",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.AddAPhoto,
                            contentDescription = "Choose Profile Photo",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp),
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Photo",
                            fontFamily = JioHotstarFontFamily,
                            fontSize = 9.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = KryptonOnbColors.Subtitle,
                        )
                    }
                }
            }

            // Display Name Input Field
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    text = "Your Cinema Profile",
                    fontFamily = JioHotstarFontFamily,
                    color = KryptonOnbColors.Title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                OutlinedTextField(
                    value = username,
                    onValueChange = { newName ->
                        username = newName
                        profilePrefs.edit().putString("username", newName.trim().ifBlank { "Roxy" }).apply()
                    },
                    placeholder = {
                        Text(
                            text = "Enter your name",
                            fontFamily = JioHotstarFontFamily,
                            fontSize = 13.5.sp,
                            color = KryptonOnbColors.Muted,
                        )
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Rounded.Person,
                            contentDescription = null,
                            tint = KryptonOnbColors.Subtitle,
                            modifier = Modifier.size(18.dp),
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = KryptonOnbColors.CardElevated,
                        unfocusedContainerColor = KryptonOnbColors.CardElevated.copy(alpha = 0.65f),
                        focusedBorderColor = Color.White.copy(alpha = 0.45f),
                        unfocusedBorderColor = KryptonOnbColors.CardBorder,
                        focusedTextColor = KryptonOnbColors.Title,
                        unfocusedTextColor = KryptonOnbColors.Title,
                        cursorColor = Color.White,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("onboarding_profile_name_input"),
                )
            }
        }
    }
}

/**
 * Stage 2 — Tailor Your Cinema:
 * Pairs user profile customization (name + photo) and the custom Canvas cinema waveform banner
 * with live toggles wired directly to PlayerPreferences.
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
        OnboardingUserProfileCard()

        Spacer(Modifier.height(14.dp))

        CinemaEngineBanner()

        Spacer(Modifier.height(14.dp))

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
