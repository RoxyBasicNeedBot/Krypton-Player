package dev.anilbeesetti.nextplayer.feature.player.ui

import android.content.ClipData
import android.content.Context
import android.os.BatteryManager
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.AutoMode
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headset
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.HourglassBottom
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.PictureInPicture
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SlowMotionVideo
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.Title
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Waves
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import dev.anilbeesetti.nextplayer.core.model.ApplicationPreferences
import dev.anilbeesetti.nextplayer.core.model.ControlButtonsPosition
import dev.anilbeesetti.nextplayer.core.model.DoubleTapGesture
import dev.anilbeesetti.nextplayer.core.model.PlayerPreferences
import dev.anilbeesetti.nextplayer.core.model.Resume
import dev.anilbeesetti.nextplayer.core.model.ScreenOrientation
import dev.anilbeesetti.nextplayer.core.model.VideoContentScale
import dev.anilbeesetti.nextplayer.core.ui.R
import dev.anilbeesetti.nextplayer.core.ui.components.LottieLyricsIcon
import dev.anilbeesetti.nextplayer.core.ui.components.NextSwitch
import dev.anilbeesetti.nextplayer.feature.player.extensions.repeatingClickable
import dev.anilbeesetti.nextplayer.feature.player.extensions.roundToTwoDecimals
import dev.anilbeesetti.nextplayer.feature.player.state.rememberPlaybackParametersState
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

private const val SPEED_STEP = 0.05f

@OptIn(UnstableApi::class)
@Composable
fun BoxScope.QuickToolsSelectorView(
    modifier: Modifier = Modifier,
    show: Boolean,
    player: Player? = null,
    playerPreferences: PlayerPreferences,
    appPreferences: ApplicationPreferences,
    onUpdatePlayerPreferences: ((PlayerPreferences) -> PlayerPreferences) -> Unit = {},
    onUpdateApplicationPreferences: ((ApplicationPreferences) -> ApplicationPreferences) -> Unit = {},
    currentVideoScale: VideoContentScale = VideoContentScale.BEST_FIT,
    onVideoContentScaleChanged: (VideoContentScale) -> Unit = {},
    onLockControlsClick: () -> Unit = {},
    onDismiss: () -> Unit,
    onRingtoneMakerClick: () -> Unit,
    onClipMakerClick: () -> Unit,
    onLyricsClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    onXRayClick: () -> Unit,
    onSoundtrackClick: () -> Unit,
    onPlaybackSpeedClick: () -> Unit,
    onPlaylistClick: () -> Unit,
    isProHudVisible: Boolean,
    onProHudToggle: () -> Unit,
    onUsbCastClick: () -> Unit,
    onQualityClick: (() -> Unit)? = null
) {
    var activeTab by remember { mutableIntStateOf(0) }

    // Dialog visibility states
    var showDoubleTapDialog by remember { mutableStateOf(false) }
    var showSeekDurationDialog by remember { mutableStateOf(false) }
    var showOrientationDialog by remember { mutableStateOf(false) }
    var showScalingDialog by remember { mutableStateOf(false) }
    var showButtonsPositionDialog by remember { mutableStateOf(false) }
    var showSeekBarStyleDialog by remember { mutableStateOf(false) }
    var showResumeDialog by remember { mutableStateOf(false) }

    // Local appearance toggles
    var showRemainingTime by remember { mutableStateOf(false) }
    var showBatteryClockOverlay by remember { mutableStateOf(true) }
    var showSeekButtons by remember { mutableStateOf(true) }
    var showNextPrevButtons by remember { mutableStateOf(true) }
    var keepAwakeAlways by remember { mutableStateOf(true) }

    OverlayView(
        modifier = modifier,
        show = show,
        title = "Player Settings & Options"
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // 6-Tab Signature Bar (matching nosved player)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = Color.White.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(14.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val tabs = listOf(
                    Icons.Rounded.TouchApp to "Tap Controls",
                    Icons.Rounded.ScreenRotation to "Layout",
                    Icons.Rounded.Tune to "Appearance",
                    Icons.Rounded.AutoMode to "Automation",
                    Icons.Rounded.Info to "Media Info",
                    Icons.Rounded.Speed to "Speed & Tools"
                )

                tabs.forEachIndexed { index, (icon, label) ->
                    val isSelected = activeTab == index
                    val bgSelectedColor = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent
                    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else Color.White.copy(alpha = 0.65f)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(bgSelectedColor)
                            .clickable { activeTab = index },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = label,
                            tint = contentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Tab Subtitle Badge
            val tabTitle = when (activeTab) {
                0 -> "Press to Tap Controls"
                1 -> "Layout & Orientation"
                2 -> "Player Appearance"
                3 -> "Automation Behaviour"
                4 -> "Media Information"
                else -> "Speed & Quick Tools"
            }
            Text(
                text = tabTitle,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
            )

            // Tab Body Content
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                when (activeTab) {
                    0 -> {
                        // Press to Tap Controls Tab
                        TapControlsTab(
                            playerPreferences = playerPreferences,
                            onUpdatePlayerPreferences = onUpdatePlayerPreferences,
                            onShowDoubleTapDialog = { showDoubleTapDialog = true },
                            onShowSeekDurationDialog = { showSeekDurationDialog = true }
                        )
                    }
                    1 -> {
                        // Layout & Orientation Tab
                        LayoutOrientationTab(
                            playerPreferences = playerPreferences,
                            currentVideoScale = currentVideoScale,
                            onShowOrientationDialog = { showOrientationDialog = true },
                            onShowScalingDialog = { showScalingDialog = true },
                            onShowButtonsPositionDialog = { showButtonsPositionDialog = true },
                            onLockControls = {
                                onDismiss()
                                onLockControlsClick()
                            }
                        )
                    }
                    2 -> {
                        // Player Appearance Tab
                        PlayerAppearanceTab(
                            appPreferences = appPreferences,
                            showRemainingTime = showRemainingTime,
                            onToggleRemainingTime = { showRemainingTime = it },
                            showBatteryClockOverlay = showBatteryClockOverlay,
                            onToggleBatteryClockOverlay = { showBatteryClockOverlay = it },
                            showSeekButtons = showSeekButtons,
                            onToggleSeekButtons = { showSeekButtons = it },
                            showNextPrevButtons = showNextPrevButtons,
                            onToggleNextPrevButtons = { showNextPrevButtons = it },
                            isProHudVisible = isProHudVisible,
                            onProHudToggle = onProHudToggle,
                            onShowSeekBarStyleDialog = { showSeekBarStyleDialog = true }
                        )
                    }
                    3 -> {
                        // Automation Behaviour Tab
                        AutomationBehaviourTab(
                            playerPreferences = playerPreferences,
                            onUpdatePlayerPreferences = onUpdatePlayerPreferences,
                            keepAwakeAlways = keepAwakeAlways,
                            onToggleKeepAwake = { keepAwakeAlways = it },
                            onShowResumeDialog = { showResumeDialog = true }
                        )
                    }
                    4 -> {
                        // Media Information Tab
                        MediaInformationTab(player = player)
                    }
                    5 -> {
                        // Speed & Quick Tools Tab
                        SpeedAndToolsTab(
                            player = player,
                            onDismiss = onDismiss,
                            onPlaybackSpeedClick = onPlaybackSpeedClick,
                            onLyricsClick = onLyricsClick,
                            onXRayClick = onXRayClick,
                            onSoundtrackClick = onSoundtrackClick,
                            onClipMakerClick = onClipMakerClick,
                            onRingtoneMakerClick = onRingtoneMakerClick,
                            onBookmarkClick = onBookmarkClick,
                            onPlaylistClick = onPlaylistClick,
                            onUsbCastClick = onUsbCastClick,
                            onQualityClick = onQualityClick
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Dialog: Double Tap Action
    if (showDoubleTapDialog) {
        val actions = remember {
            listOf(
                DoubleTapGesture.BOTH,
                DoubleTapGesture.PLAY_PAUSE,
                DoubleTapGesture.FAST_FORWARD_AND_REWIND,
                DoubleTapGesture.NONE
            )
        }
        PremiumSelectionDialog(
            title = "Double Tap Action",
            onDismiss = { showDoubleTapDialog = false },
            onReset = {
                onUpdatePlayerPreferences { it.copy(doubleTapGesture = DoubleTapGesture.BOTH) }
                showDoubleTapDialog = false
            },
            options = actions,
            selectedOption = playerPreferences.doubleTapGesture,
            onOptionSelected = {
                onUpdatePlayerPreferences { pref -> pref.copy(doubleTapGesture = it) }
                showDoubleTapDialog = false
            },
            optionLabel = {
                when (it) {
                    DoubleTapGesture.BOTH -> "Seek Left/Right"
                    DoubleTapGesture.PLAY_PAUSE -> "Play / Pause"
                    DoubleTapGesture.FAST_FORWARD_AND_REWIND -> "Fast Forward & Rewind Only"
                    DoubleTapGesture.NONE -> "Disable Double Tap"
                }
            },
            optionDescription = {
                when (it) {
                    DoubleTapGesture.BOTH -> "Double tap left half to rewind, right half to jump forward"
                    DoubleTapGesture.PLAY_PAUSE -> "Double tap anywhere on the video to play or pause"
                    DoubleTapGesture.FAST_FORWARD_AND_REWIND -> "Double tap to trigger fast seek navigation"
                    DoubleTapGesture.NONE -> "No gesture action executed on double tap"
                }
            }
        )
    }

    // Dialog: Seek Duration
    if (showSeekDurationDialog) {
        val durations = remember { listOf(5, 10, 15, 30, 60) }
        PremiumSelectionDialog(
            title = "Double Tap Seek Duration",
            onDismiss = { showSeekDurationDialog = false },
            onReset = {
                onUpdatePlayerPreferences { it.copy(seekIncrement = 10) }
                showSeekDurationDialog = false
            },
            options = durations,
            selectedOption = playerPreferences.seekIncrement,
            onOptionSelected = {
                onUpdatePlayerPreferences { pref -> pref.copy(seekIncrement = it) }
                showSeekDurationDialog = false
            },
            optionLabel = { "$it seconds" },
            optionDescription = { "Jump forward or backward by $it seconds per double-tap trigger" }
        )
    }

    // Dialog: Screen Orientation
    if (showOrientationDialog) {
        val orientations = remember {
            listOf(
                ScreenOrientation.AUTOMATIC,
                ScreenOrientation.LANDSCAPE,
                ScreenOrientation.PORTRAIT,
                ScreenOrientation.VIDEO_ORIENTATION
            )
        }
        PremiumSelectionDialog(
            title = "Screen Orientation",
            onDismiss = { showOrientationDialog = false },
            onReset = {
                onUpdatePlayerPreferences { it.copy(playerScreenOrientation = ScreenOrientation.AUTOMATIC) }
                showOrientationDialog = false
            },
            options = orientations,
            selectedOption = playerPreferences.playerScreenOrientation,
            onOptionSelected = {
                onUpdatePlayerPreferences { pref -> pref.copy(playerScreenOrientation = it) }
                showOrientationDialog = false
            },
            optionLabel = {
                when (it) {
                    ScreenOrientation.AUTOMATIC -> "Sensor Auto-Rotate"
                    ScreenOrientation.LANDSCAPE -> "Force Landscape Mode"
                    ScreenOrientation.PORTRAIT -> "Force Portrait Mode"
                    ScreenOrientation.VIDEO_ORIENTATION -> "Follow Video Aspect"
                    else -> it.name
                }
            },
            optionDescription = {
                when (it) {
                    ScreenOrientation.AUTOMATIC -> "Rotate screen dynamically based on device accelerometer sensor"
                    ScreenOrientation.LANDSCAPE -> "Lock screen in widescreen landscape viewing"
                    ScreenOrientation.PORTRAIT -> "Lock screen in vertical upright portrait viewing"
                    ScreenOrientation.VIDEO_ORIENTATION -> "Automatically switch orientation to match video aspect ratio"
                    else -> null
                }
            }
        )
    }

    // Dialog: Fullscreen Scale Mode
    if (showScalingDialog) {
        val scales = remember {
            listOf(
                VideoContentScale.BEST_FIT,
                VideoContentScale.STRETCH,
                VideoContentScale.CROP,
                VideoContentScale.HUNDRED_PERCENT
            )
        }
        PremiumSelectionDialog(
            title = "Fullscreen Scaling Mode",
            onDismiss = { showScalingDialog = false },
            onReset = {
                onVideoContentScaleChanged(VideoContentScale.BEST_FIT)
                onUpdatePlayerPreferences { it.copy(playerVideoZoom = VideoContentScale.BEST_FIT) }
                showScalingDialog = false
            },
            options = scales,
            selectedOption = currentVideoScale,
            onOptionSelected = {
                onVideoContentScaleChanged(it)
                onUpdatePlayerPreferences { pref -> pref.copy(playerVideoZoom = it) }
                showScalingDialog = false
            },
            optionLabel = {
                when (it) {
                    VideoContentScale.BEST_FIT -> "Fit Screen (Letterbox)"
                    VideoContentScale.STRETCH -> "Stretch to Fill"
                    VideoContentScale.CROP -> "Crop and Zoom"
                    VideoContentScale.HUNDRED_PERCENT -> "100% Original Resolution"
                }
            },
            optionDescription = {
                when (it) {
                    VideoContentScale.BEST_FIT -> "Preserve original aspect ratio with subtle black letterbox bars"
                    VideoContentScale.STRETCH -> "Stretch image geometry to fill entire display edge-to-edge"
                    VideoContentScale.CROP -> "Zoom to fill complete screen without bars, cropping top/bottom"
                    VideoContentScale.HUNDRED_PERCENT -> "Render strictly 1:1 at video's native source pixel dimensions"
                }
            }
        )
    }

    // Dialog: Playback Control Position
    if (showButtonsPositionDialog) {
        val positions = remember {
            listOf(
                ControlButtonsPosition.LEFT,
                ControlButtonsPosition.RIGHT
            )
        }
        PremiumSelectionDialog(
            title = "Playback Controls Alignment",
            onDismiss = { showButtonsPositionDialog = false },
            onReset = {
                onUpdatePlayerPreferences { it.copy(controlButtonsPosition = ControlButtonsPosition.LEFT) }
                showButtonsPositionDialog = false
            },
            options = positions,
            selectedOption = playerPreferences.controlButtonsPosition,
            onOptionSelected = {
                onUpdatePlayerPreferences { pref -> pref.copy(controlButtonsPosition = it) }
                showButtonsPositionDialog = false
            },
            optionLabel = {
                when (it) {
                    ControlButtonsPosition.LEFT -> "Left Aligned"
                    ControlButtonsPosition.RIGHT -> "Right Aligned"
                }
            },
            optionDescription = {
                when (it) {
                    ControlButtonsPosition.LEFT -> "Position bottom playback action buttons toward the left side"
                    ControlButtonsPosition.RIGHT -> "Position bottom playback action buttons toward the right side"
                }
            }
        )
    }

    // Dialog: Seekbar Style
    if (showSeekBarStyleDialog) {
        val styles = remember { listOf("STANDARD", "WAVY", "THICK", "SLIM") }
        PremiumSelectionDialog(
            title = "Seekbar Style",
            onDismiss = { showSeekBarStyleDialog = false },
            onReset = {
                onUpdateApplicationPreferences { it.copy(playerSliderStyle = "STANDARD") }
                showSeekBarStyleDialog = false
            },
            options = styles,
            selectedOption = appPreferences.playerSliderStyle,
            onOptionSelected = {
                onUpdateApplicationPreferences { pref -> pref.copy(playerSliderStyle = it) }
                showSeekBarStyleDialog = false
            },
            optionLabel = {
                when (it) {
                    "WAVY" -> "Wavy (Organic Curve)"
                    "THICK" -> "Thick (Bold Track)"
                    "SLIM" -> "Slim (Minimalist Line)"
                    else -> "Standard"
                }
            },
            optionDescription = {
                when (it) {
                    "WAVY" -> "Animated fluid wave curve during active playback"
                    "THICK" -> "Substantial, easily draggable touch track"
                    "SLIM" -> "Discreet thin timeline for distraction-free watching"
                    else -> "Clean balanced default progress bar"
                }
            }
        )
    }

    // Dialog: Resume Playback
    if (showResumeDialog) {
        val options = remember { listOf(Resume.YES, Resume.NO) }
        PremiumSelectionDialog(
            title = "Resume Playback",
            onDismiss = { showResumeDialog = false },
            onReset = {
                onUpdatePlayerPreferences { it.copy(resume = Resume.YES) }
                showResumeDialog = false
            },
            options = options,
            selectedOption = playerPreferences.resume,
            onOptionSelected = {
                onUpdatePlayerPreferences { pref -> pref.copy(resume = it) }
                showResumeDialog = false
            },
            optionLabel = {
                when (it) {
                    Resume.YES -> "Always Resume Playback"
                    Resume.NO -> "Start From Beginning"
                }
            },
            optionDescription = {
                when (it) {
                    Resume.YES -> "Remember timestamp and continue video automatically"
                    Resume.NO -> "Always start playing video from 0:00"
                }
            }
        )
    }
}

// ---------------- TAB 0: PRESS TO TAP CONTROLS ----------------

@Composable
private fun TapControlsTab(
    playerPreferences: PlayerPreferences,
    onUpdatePlayerPreferences: ((PlayerPreferences) -> PlayerPreferences) -> Unit,
    onShowDoubleTapDialog: () -> Unit,
    onShowSeekDurationDialog: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SheetCard {
            SheetRow(
                icon = Icons.Rounded.TouchApp,
                title = "Double Tap Action",
                subtitle = when (playerPreferences.doubleTapGesture) {
                    DoubleTapGesture.BOTH -> "Seek Left/Right"
                    DoubleTapGesture.PLAY_PAUSE -> "Play / Pause"
                    DoubleTapGesture.FAST_FORWARD_AND_REWIND -> "Fast Forward & Rewind Only"
                    DoubleTapGesture.NONE -> "No Action"
                },
                onClick = onShowDoubleTapDialog
            )

            SheetDivider()

            SheetRow(
                icon = Icons.Rounded.Schedule,
                title = "Double Tap Seek Duration",
                subtitle = "${playerPreferences.seekIncrement} seconds",
                onClick = onShowSeekDurationDialog
            )

            SheetDivider()

            SheetToggleRow(
                icon = Icons.Rounded.Speed,
                title = "Press & Hold Acceleration",
                subtitle = "Hold screen to temporarily accelerate playback rate",
                checked = playerPreferences.useLongPressControls,
                onCheckedChange = { onUpdatePlayerPreferences { pref -> pref.copy(useLongPressControls = it) } }
            )

            if (playerPreferences.useLongPressControls) {
                SheetDivider()
                SpeedInlineSlider(
                    label = "Hold Acceleration Rate",
                    value = playerPreferences.longPressControlsSpeed,
                    valueRange = 1.5f..4.0f,
                    step = SPEED_STEP,
                    defaultValue = 2.0f,
                    onValueChange = { onUpdatePlayerPreferences { pref -> pref.copy(longPressControlsSpeed = it) } }
                )
            }
        }

        SheetSectionLabel("Swipe & Gesture Controls")
        SheetCard {
            SheetToggleRow(
                icon = Icons.AutoMirrored.Rounded.VolumeUp,
                title = "Swipe Volume Gesture",
                subtitle = "Swipe vertically on the right half of the screen to adjust volume",
                checked = playerPreferences.enableVolumeSwipeGesture,
                onCheckedChange = { onUpdatePlayerPreferences { pref -> pref.copy(enableVolumeSwipeGesture = it) } }
            )

            SheetDivider()

            SheetToggleRow(
                icon = Icons.Rounded.LightMode,
                title = "Swipe Brightness Gesture",
                subtitle = "Swipe vertically on the left half of the screen to adjust brightness",
                checked = playerPreferences.enableBrightnessSwipeGesture,
                onCheckedChange = { onUpdatePlayerPreferences { pref -> pref.copy(enableBrightnessSwipeGesture = it) } }
            )

            SheetDivider()

            SheetToggleRow(
                icon = Icons.Rounded.FastForward,
                title = "Swipe to Seek",
                subtitle = "Drag finger horizontally across screen to scrub playback position",
                checked = playerPreferences.useSeekControls,
                onCheckedChange = { onUpdatePlayerPreferences { pref -> pref.copy(useSeekControls = it) } }
            )

            SheetDivider()

            SheetToggleRow(
                icon = Icons.Rounded.ZoomIn,
                title = "Pinch to Zoom",
                subtitle = "Pinch with two fingers to zoom, crop, and pan video frame",
                checked = playerPreferences.useZoomControls,
                onCheckedChange = { onUpdatePlayerPreferences { pref -> pref.copy(useZoomControls = it) } }
            )
        }
    }
}

// ---------------- TAB 1: LAYOUT & ORIENTATION ----------------

@Composable
private fun LayoutOrientationTab(
    playerPreferences: PlayerPreferences,
    currentVideoScale: VideoContentScale,
    onShowOrientationDialog: () -> Unit,
    onShowScalingDialog: () -> Unit,
    onShowButtonsPositionDialog: () -> Unit,
    onLockControls: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SheetCard {
            SheetRow(
                icon = Icons.Rounded.ScreenRotation,
                title = "Screen Orientation",
                subtitle = when (playerPreferences.playerScreenOrientation) {
                    ScreenOrientation.AUTOMATIC -> "Sensor Auto-Rotate"
                    ScreenOrientation.LANDSCAPE -> "Force Landscape"
                    ScreenOrientation.PORTRAIT -> "Force Portrait"
                    ScreenOrientation.VIDEO_ORIENTATION -> "Follow Video Aspect"
                    else -> playerPreferences.playerScreenOrientation.name
                },
                onClick = onShowOrientationDialog
            )

            SheetDivider()

            SheetRow(
                icon = Icons.Rounded.AspectRatio,
                title = "Fullscreen Scale Mode",
                subtitle = when (currentVideoScale) {
                    VideoContentScale.BEST_FIT -> "Fit Screen (Letterbox)"
                    VideoContentScale.STRETCH -> "Stretch to Fill"
                    VideoContentScale.CROP -> "Crop and Zoom"
                    VideoContentScale.HUNDRED_PERCENT -> "100% Original"
                },
                onClick = onShowScalingDialog
            )

            SheetDivider()

            SheetRow(
                icon = Icons.Rounded.Tune,
                title = "Playback Controls Alignment",
                subtitle = if (playerPreferences.controlButtonsPosition == ControlButtonsPosition.LEFT) "Left Aligned" else "Right Aligned",
                onClick = onShowButtonsPositionDialog
            )

            SheetDivider()

            SheetRow(
                icon = Icons.Rounded.Lock,
                title = "Lock Screen Controls",
                subtitle = "Lock gestures and touch buttons to prevent accidental touches",
                onClick = onLockControls
            )
        }
    }
}

// ---------------- TAB 2: PLAYER APPEARANCE ----------------

@Composable
private fun PlayerAppearanceTab(
    appPreferences: ApplicationPreferences,
    showRemainingTime: Boolean,
    onToggleRemainingTime: (Boolean) -> Unit,
    showBatteryClockOverlay: Boolean,
    onToggleBatteryClockOverlay: (Boolean) -> Unit,
    showSeekButtons: Boolean,
    onToggleSeekButtons: (Boolean) -> Unit,
    showNextPrevButtons: Boolean,
    onToggleNextPrevButtons: (Boolean) -> Unit,
    isProHudVisible: Boolean,
    onProHudToggle: () -> Unit,
    onShowSeekBarStyleDialog: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SheetCard {
            SheetRow(
                icon = Icons.Rounded.Waves,
                title = "Seekbar Style",
                subtitle = when (appPreferences.playerSliderStyle) {
                    "WAVY" -> "Wavy (Organic Curve)"
                    "THICK" -> "Thick (Bold Track)"
                    "SLIM" -> "Slim (Minimalist Line)"
                    else -> "Standard"
                },
                onClick = onShowSeekBarStyleDialog
            )

            SheetDivider()

            SheetToggleRow(
                icon = Icons.Rounded.HourglassBottom,
                title = "Show Remaining Time",
                subtitle = "Display remaining countdown time (-MM:SS) instead of total duration",
                checked = showRemainingTime,
                onCheckedChange = onToggleRemainingTime
            )

            SheetDivider()

            SheetToggleRow(
                icon = Icons.Rounded.BatteryChargingFull,
                title = "Battery & Clock Overlay",
                subtitle = "Show current battery level and system clock in player top bar",
                checked = showBatteryClockOverlay,
                onCheckedChange = onToggleBatteryClockOverlay
            )

            SheetDivider()

            SheetToggleRow(
                icon = Icons.Rounded.FastForward,
                title = "Show Seek Buttons",
                subtitle = "Show +/-10s rewind and fast forward buttons in player controls",
                checked = showSeekButtons,
                onCheckedChange = onToggleSeekButtons
            )

            SheetDivider()

            SheetToggleRow(
                icon = Icons.Rounded.SkipNext,
                title = "Show Skip Prev / Next Buttons",
                subtitle = "Show previous/next video and chapter buttons in bottom bar",
                checked = showNextPrevButtons,
                onCheckedChange = onToggleNextPrevButtons
            )

            SheetDivider()

            SheetToggleRow(
                icon = Icons.Rounded.Analytics,
                title = "Stats for Nerds HUD",
                subtitle = if (isProHudVisible) "Active • Real-time Codec, FPS, Bitrate & Buffer" else "Real-time technical stream telemetry and stats",
                checked = isProHudVisible,
                onCheckedChange = { onProHudToggle() }
            )
        }
    }
}

// ---------------- TAB 3: AUTOMATION BEHAVIOUR ----------------

@Composable
private fun AutomationBehaviourTab(
    playerPreferences: PlayerPreferences,
    onUpdatePlayerPreferences: ((PlayerPreferences) -> PlayerPreferences) -> Unit,
    keepAwakeAlways: Boolean,
    onToggleKeepAwake: (Boolean) -> Unit,
    onShowResumeDialog: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        SheetCard {
            SheetToggleRow(
                icon = Icons.Rounded.SkipNext,
                title = "Auto-play Next Video",
                subtitle = "Automatically load and play the next video in folder or queue",
                checked = playerPreferences.autoplay,
                onCheckedChange = { onUpdatePlayerPreferences { pref -> pref.copy(autoplay = it) } }
            )

            SheetDivider()

            SheetToggleRow(
                icon = Icons.Rounded.PictureInPicture,
                title = "Picture-in-Picture (PiP)",
                subtitle = "Automatically transition to floating PiP window when exiting app",
                checked = playerPreferences.autoPip,
                onCheckedChange = { onUpdatePlayerPreferences { pref -> pref.copy(autoPip = it) } }
            )

            SheetDivider()

            SheetToggleRow(
                icon = Icons.Rounded.Headset,
                title = "Background Audio Play",
                subtitle = "Continue playing audio stream in background when screen is off",
                checked = playerPreferences.autoBackgroundPlay,
                onCheckedChange = { onUpdatePlayerPreferences { pref -> pref.copy(autoBackgroundPlay = it) } }
            )

            SheetDivider()

            SheetToggleRow(
                icon = Icons.AutoMirrored.Rounded.VolumeOff,
                title = "Pause on Headset Disconnect",
                subtitle = "Pause playback automatically when headphones or Bluetooth disconnect",
                checked = playerPreferences.pauseOnHeadsetDisconnect,
                onCheckedChange = { onUpdatePlayerPreferences { pref -> pref.copy(pauseOnHeadsetDisconnect = it) } }
            )

            SheetDivider()

            SheetToggleRow(
                icon = Icons.Rounded.LightMode,
                title = "Remember Brightness Level",
                subtitle = "Save and restore custom brightness across playback sessions",
                checked = playerPreferences.rememberPlayerBrightness,
                onCheckedChange = { onUpdatePlayerPreferences { pref -> pref.copy(rememberPlayerBrightness = it) } }
            )

            SheetDivider()

            SheetToggleRow(
                icon = Icons.Rounded.WbSunny,
                title = "Keep Screen Awake Always",
                subtitle = "Prevent screen from turning off while video is playing or paused",
                checked = keepAwakeAlways,
                onCheckedChange = onToggleKeepAwake
            )

            SheetDivider()

            SheetRow(
                icon = Icons.Rounded.History,
                title = "Resume Playback",
                subtitle = if (playerPreferences.resume == Resume.YES) "Always Resume From Last Position" else "Start From Beginning",
                onClick = onShowResumeDialog
            )
        }
    }
}

// ---------------- TAB 4: MEDIA INFORMATION ----------------

@OptIn(UnstableApi::class)
@Composable
private fun MediaInformationTab(player: Player?) {
    val clipboardManager = LocalClipboardManager.current
    val hapticFeedback = LocalHapticFeedback.current

    val title = player?.currentMediaItem?.mediaMetadata?.title?.toString()
        ?: player?.currentMediaItem?.localConfiguration?.uri?.lastPathSegment
        ?: "Video"
    val uri = player?.currentMediaItem?.localConfiguration?.uri?.toString() ?: ""

    val exoPlayer = player as? ExoPlayer
    val videoFormat = exoPlayer?.videoFormat
    val audioFormat = exoPlayer?.audioFormat
    val videoSize = player?.videoSize

    val width = videoFormat?.width ?: videoSize?.width ?: 0
    val height = videoFormat?.height ?: videoSize?.height ?: 0
    val fps = videoFormat?.frameRate ?: 0f
    val videoCodec = videoFormat?.sampleMimeType ?: videoFormat?.codecs ?: "Standard Video Stream"
    val videoBitrate = videoFormat?.bitrate?.takeIf { it > 0 }?.let { "${it / 1000} kbps" }

    val audioCodec = audioFormat?.sampleMimeType ?: audioFormat?.codecs ?: "Standard Audio"
    val channelCount = audioFormat?.channelCount ?: 0
    val channelLabel = when (channelCount) {
        1 -> "Mono (1 ch)"
        2 -> "Stereo (2 ch)"
        6 -> "5.1 Surround (6 ch)"
        8 -> "7.1 Surround (8 ch)"
        else -> if (channelCount > 0) "$channelCount ch" else "Stereo"
    }
    val sampleRate = audioFormat?.sampleRate?.takeIf { it > 0 }?.let { "$it Hz" }
    val audioBitrate = audioFormat?.bitrate?.takeIf { it > 0 }?.let { "${it / 1000} kbps" }

    val durationMs = player?.duration?.coerceAtLeast(0L) ?: 0L
    val seconds = (durationMs / 1000) % 60
    val minutes = (durationMs / (1000 * 60)) % 60
    val hours = durationMs / (1000 * 60 * 60)
    val durationStr = if (hours > 0) String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
                      else String.format(Locale.US, "%02d:%02d", minutes, seconds)

    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Hero Media Overview Card
        ElevatedCard(
            colors = CardDefaults.elevatedCardColors(
                containerColor = Color.White.copy(alpha = 0.08f)
            ),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (width > 0 && height > 0) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text("${width}x$height", style = MaterialTheme.typography.labelSmall) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                labelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                    if (fps > 0f) {
                        SuggestionChip(
                            onClick = {},
                            label = { Text("${fps.roundToInt()} fps", style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                    SuggestionChip(
                        onClick = {},
                        label = { Text(durationStr, style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
        }

        // General Technical Specifications Card
        SheetSectionLabel("General Information")
        SheetCard {
            InfoPropertyRow(
                icon = Icons.Rounded.Title,
                label = "Title",
                value = title
            )
            SheetDivider()
            InfoPropertyRow(
                icon = Icons.Rounded.Schedule,
                label = "Duration",
                value = durationStr
            )
            if (uri.isNotBlank()) {
                SheetDivider()
                InfoPropertyRow(
                    icon = Icons.Rounded.Storage,
                    label = "Source URI",
                    value = uri
                )
            }
        }

        // Video Specifications Card
        SheetSectionLabel("Video Stream")
        SheetCard {
            if (width > 0 && height > 0) {
                InfoPropertyRow(
                    icon = Icons.Rounded.AspectRatio,
                    label = "Resolution",
                    value = "${width}x${height}"
                )
                SheetDivider()
            }
            if (fps > 0f) {
                InfoPropertyRow(
                    icon = Icons.Rounded.SlowMotionVideo,
                    label = "Frame Rate",
                    value = "${fps.roundToInt()} fps"
                )
                SheetDivider()
            }
            InfoPropertyRow(
                icon = Icons.Rounded.Movie,
                label = "Video Codec",
                value = videoCodec
            )
            if (videoBitrate != null) {
                SheetDivider()
                InfoPropertyRow(
                    icon = Icons.Rounded.Speed,
                    label = "Bitrate",
                    value = videoBitrate
                )
            }
        }

        // Audio Specifications Card
        SheetSectionLabel("Audio Stream")
        SheetCard {
            InfoPropertyRow(
                icon = Icons.Rounded.GraphicEq,
                label = "Audio Codec",
                value = audioCodec
            )
            SheetDivider()
            InfoPropertyRow(
                icon = Icons.AutoMirrored.Rounded.VolumeUp,
                label = "Channels",
                value = channelLabel
            )
            if (sampleRate != null) {
                SheetDivider()
                InfoPropertyRow(
                    icon = Icons.Rounded.Speed,
                    label = "Sample Rate",
                    value = sampleRate
                )
            }
            if (audioBitrate != null) {
                SheetDivider()
                InfoPropertyRow(
                    icon = Icons.Rounded.Speed,
                    label = "Audio Bitrate",
                    value = audioBitrate
                )
            }
        }

        // Copy All Button
        Button(
            onClick = {
                hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                val fullSpecs = buildString {
                    appendLine("=== MEDIA INFORMATION ===")
                    appendLine("Title: $title")
                    appendLine("Duration: $durationStr")
                    if (width > 0 && height > 0) appendLine("Resolution: ${width}x${height} @ ${fps.roundToInt()} fps")
                    appendLine("Video Codec: $videoCodec")
                    if (videoBitrate != null) appendLine("Video Bitrate: $videoBitrate")
                    appendLine("Audio Codec: $audioCodec")
                    appendLine("Channels: $channelLabel")
                    if (sampleRate != null) appendLine("Sample Rate: $sampleRate")
                    if (audioBitrate != null) appendLine("Audio Bitrate: $audioBitrate")
                    if (uri.isNotBlank()) appendLine("Source: $uri")
                }
                clipboardManager.setText(AnnotatedString(fullSpecs))
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            )
        ) {
            Icon(
                imageVector = Icons.Rounded.ContentCopy,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text("Copy All Specifications", fontWeight = FontWeight.SemiBold)
        }
    }
}

// ---------------- TAB 5: SPEED & QUICK TOOLS ----------------

@OptIn(UnstableApi::class)
@Composable
private fun SpeedAndToolsTab(
    player: Player?,
    onDismiss: () -> Unit,
    onPlaybackSpeedClick: () -> Unit,
    onLyricsClick: () -> Unit,
    onXRayClick: () -> Unit,
    onSoundtrackClick: () -> Unit,
    onClipMakerClick: () -> Unit,
    onRingtoneMakerClick: () -> Unit,
    onBookmarkClick: () -> Unit,
    onPlaylistClick: () -> Unit,
    onUsbCastClick: () -> Unit,
    onQualityClick: (() -> Unit)?
) {
    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
        // Playback Speed Controls
        if (player != null) {
            SpeedSheetContent(player = player)
        } else {
            QuickToolTile(
                title = "Playback Speed",
                subtitle = "Adjust playback speed rate (0.25x - 4.0x)",
                iconRes = R.drawable.ic_speed,
                accentColor = Color(0xFFAB47BC),
                onClick = {
                    onDismiss()
                    onPlaybackSpeedClick()
                }
            )
        }

        SheetSectionLabel("Creative & Power Tools")
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            QuickToolTile(
                title = "Live Synced Lyrics",
                subtitle = "Real-time karaoke lyrics synchronized to playback",
                accentColor = Color(0xFF00E5FF),
                customIcon = {
                    LottieLyricsIcon(
                        size = 24.dp,
                        isPlaying = true
                    )
                },
                onClick = {
                    onDismiss()
                    onLyricsClick()
                }
            )

            QuickToolTile(
                title = "Amazon X-Ray",
                subtitle = "Actors, characters, director, trivia & film info",
                iconRes = R.drawable.ic_xray_custom,
                accentColor = Color(0xFFFF9100),
                onClick = {
                    onDismiss()
                    onXRayClick()
                }
            )

            QuickToolTile(
                title = "Soundtracks & Shazam",
                subtitle = "Identify background songs with 30s preview audio",
                iconRes = R.drawable.ic_soundtrack_custom,
                accentColor = Color(0xFFE040FB),
                onClick = {
                    onDismiss()
                    onSoundtrackClick()
                }
            )

            QuickToolTile(
                title = "10s Clip Maker",
                subtitle = "Trim, export and share short video clips",
                iconRes = R.drawable.ic_scissors,
                accentColor = Color(0xFFFF4081),
                onClick = {
                    onDismiss()
                    onClipMakerClick()
                }
            )

            QuickToolTile(
                title = "Ringtone Maker",
                subtitle = "Trim audio section and export as MP3 ringtone",
                iconRes = R.drawable.ic_ringtone,
                accentColor = Color(0xFF00E676),
                onClick = {
                    onDismiss()
                    onRingtoneMakerClick()
                }
            )

            QuickToolTile(
                title = "Bookmarks & Notes",
                subtitle = "Timestamped annotations to jump across video scenes",
                iconRes = R.drawable.ic_feather_bookmark,
                accentColor = Color(0xFF448AFF),
                onClick = {
                    onDismiss()
                    onBookmarkClick()
                }
            )

            QuickToolTile(
                title = "Playlist Queue",
                subtitle = "Manage, view, and reorder playlist queue",
                iconRes = R.drawable.ic_playlist,
                accentColor = Color(0xFF26A69A),
                onClick = {
                    onDismiss()
                    onPlaylistClick()
                }
            )

            QuickToolTile(
                title = "USB & Wireless Cast",
                subtitle = "Cast to TV, PC, VLC via adb & local network",
                iconRes = R.drawable.ic_usb_cast_custom,
                accentColor = Color(0xFF3D5AFE),
                onClick = {
                    onDismiss()
                    onUsbCastClick()
                }
            )

            if (onQualityClick != null) {
                QuickToolTile(
                    title = "Video Quality",
                    subtitle = "Switch stream resolution and source quality",
                    iconRes = R.drawable.ic_settings_custom,
                    accentColor = Color(0xFFFFB74D),
                    onClick = {
                        onDismiss()
                        onQualityClick()
                    }
                )
            }
        }
    }
}

// ---------------- SPEED SUBCOMPONENT ----------------

@OptIn(UnstableApi::class)
@Composable
private fun SpeedSheetContent(player: Player) {
    val hapticFeedback = LocalHapticFeedback.current
    val playbackParametersState = rememberPlaybackParametersState(player)
    val currentSpeed = playbackParametersState.speed

    SheetCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Speed readout
            Text(
                text = String.format(Locale.US, "%.2fx", currentSpeed),
                fontSize = 42.sp,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            // Slider with buttons
            val minValue = 0.25f
            val maxValue = 4.0f
            val stepSize = 0.05f

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilledTonalIconButton(
                    onClick = {
                        val newSpeed = (currentSpeed - stepSize).coerceAtLeast(minValue)
                        playbackParametersState.setPlaybackSpeed(newSpeed)
                    },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Remove,
                        contentDescription = "Decrease",
                        modifier = Modifier.size(18.dp)
                    )
                }

                Slider(
                    value = currentSpeed,
                    valueRange = minValue..maxValue,
                    onValueChange = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        playbackParametersState.setPlaybackSpeed(it)
                    },
                    modifier = Modifier.weight(1f)
                )

                FilledTonalIconButton(
                    onClick = {
                        val newSpeed = (currentSpeed + stepSize).coerceAtMost(maxValue)
                        playbackParametersState.setPlaybackSpeed(newSpeed)
                    },
                    modifier = Modifier.size(38.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Increase",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Presets
            val presets = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f)
            presets.chunked(3).forEach { rowItems ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowItems.forEach { speed ->
                        val isSelected = abs(currentSpeed - speed) < 0.01f
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(38.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                                    else Color.White.copy(alpha = 0.06f)
                                )
                                .border(
                                    width = 1.dp,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.12f),
                                    shape = RoundedCornerShape(10.dp)
                                )
                                .clickable {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    playbackParametersState.setPlaybackSpeed(speed)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = String.format(Locale.US, "%.2fx", speed),
                                color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            // Skip silence toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { playbackParametersState.setIsSkipSilenceEnabled(!playbackParametersState.skipSilenceEnabled) }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(R.string.skip_silence),
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White
                )
                NextSwitch(
                    checked = playbackParametersState.skipSilenceEnabled,
                    onCheckedChange = { playbackParametersState.setIsSkipSilenceEnabled(it) }
                )
            }
        }
    }
}

// ---------------- REUSABLE UI ELEMENTS (NOSVED LOOK & FEEL) ----------------

@Composable
private fun SheetSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp)
    )
}

@Composable
private fun SheetCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.06f),
            contentColor = Color.White
        ),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.09f))
    ) {
        Column(content = content)
    }
}

@Composable
private fun SheetDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 52.dp),
        color = Color.White.copy(alpha = 0.10f)
    )
}

@Composable
private fun SheetRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.65f)
            )
        }

        Icon(
            imageVector = Icons.Rounded.ChevronRight,
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = Color.White.copy(alpha = 0.45f)
        )
    }
}

@Composable
private fun SheetToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.65f)
            )
        }

        NextSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange
        )
    }
}

@Composable
private fun SpeedInlineSlider(
    label: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    step: Float = SPEED_STEP,
    defaultValue: Float,
    onValueChange: (Float) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f)
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = String.format(Locale.US, "%.2fx", value),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Surface(
                    onClick = { onValueChange(defaultValue) },
                    shape = CircleShape,
                    color = Color.White.copy(alpha = 0.12f),
                    modifier = Modifier.size(22.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Reset",
                            modifier = Modifier.size(12.dp),
                            tint = Color.White
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(4.dp))

        StepSlider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            step = step,
            defaultValue = defaultValue
        )
    }
}

@Composable
private fun StepSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    step: Float = 0.05f,
    defaultValue: Float = valueRange.start
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        LongPressStepButton(
            icon = Icons.Rounded.Remove,
            contentDescription = "Decrease",
            onStep = {
                val next = (value - step).coerceIn(valueRange.start, valueRange.endInclusive).roundToTwoDecimals()
                val snapped = ((next / step).roundToInt() * step).roundToTwoDecimals()
                onValueChange(snapped.coerceIn(valueRange.start, valueRange.endInclusive))
            }
        )

        Slider(
            value = value.coerceIn(valueRange.start, valueRange.endInclusive),
            onValueChange = { raw ->
                val snapped = ((raw / step).roundToInt() * step).roundToTwoDecimals()
                    .coerceIn(valueRange.start, valueRange.endInclusive)
                onValueChange(snapped)
            },
            valueRange = valueRange,
            modifier = Modifier.weight(1f)
        )

        LongPressStepButton(
            icon = Icons.Rounded.Add,
            contentDescription = "Increase",
            onStep = {
                val next = (value + step).coerceIn(valueRange.start, valueRange.endInclusive).roundToTwoDecimals()
                val snapped = ((next / step).roundToInt() * step).roundToTwoDecimals()
                onValueChange(snapped.coerceIn(valueRange.start, valueRange.endInclusive))
            }
        )
    }
}

@Composable
private fun LongPressStepButton(
    icon: ImageVector,
    contentDescription: String,
    onStep: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Box(
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(
                if (isPressed) MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
                else Color.White.copy(alpha = 0.10f)
            )
            .repeatingClickable(
                initialDelayMillis = 400,
                delayMillis = 80,
                interactionSource = interactionSource,
                onClick = onStep
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            modifier = Modifier.size(16.dp),
            tint = if (isPressed) MaterialTheme.colorScheme.primary else Color.White
        )
    }
}

@Composable
private fun <T> PremiumSelectionDialog(
    title: String,
    onDismiss: () -> Unit,
    onReset: (() -> Unit)? = null,
    options: List<T>,
    selectedOption: T,
    onOptionSelected: (T) -> Unit,
    optionLabel: @Composable (T) -> String,
    optionDescription: (@Composable (T) -> String?)? = null
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .wrapContentHeight(),
            shape = RoundedCornerShape(24.dp),
            color = Color(0xFF1A1D27),
            tonalElevation = 8.dp,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                // Title and Reset Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (onReset != null) {
                        TextButton(
                            onClick = onReset,
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = "Reset",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text("Reset", style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }

                Spacer(Modifier.height(6.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.12f))

                // Options List
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 380.dp)
                        .padding(horizontal = 12.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(options) { option ->
                        val isSelected = option == selectedOption
                        val cardBgColor = if (isSelected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        } else {
                            Color.Transparent
                        }
                        val cardBorderColor = if (isSelected) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        } else {
                            Color.Transparent
                        }

                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            shape = RoundedCornerShape(14.dp),
                            color = cardBgColor,
                            border = BorderStroke(1.dp, cardBorderColor),
                            onClick = { onOptionSelected(option) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = null,
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = MaterialTheme.colorScheme.primary,
                                        unselectedColor = Color.White.copy(alpha = 0.5f)
                                    )
                                )
                                Spacer(Modifier.width(14.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = optionLabel(option),
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.White
                                    )
                                    val desc = optionDescription?.invoke(option)
                                    if (desc != null) {
                                        Spacer(Modifier.height(2.dp))
                                        Text(
                                            text = desc,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.65f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.12f))
                Spacer(Modifier.height(6.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = Color.White.copy(alpha = 0.8f))
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoPropertyRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    val clipboard = LocalClipboardManager.current
    val haptic = LocalHapticFeedback.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .clickable {
                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                clipboard.setText(AnnotatedString(value))
            }
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.65f),
            modifier = Modifier.width(110.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            modifier = Modifier.weight(1f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.width(6.dp))
        Icon(
            imageVector = Icons.Rounded.ContentCopy,
            contentDescription = "Copy",
            tint = Color.White.copy(alpha = 0.4f),
            modifier = Modifier.size(14.dp)
        )
    }
}

@Composable
private fun QuickToolTile(
    title: String,
    subtitle: String,
    iconRes: Int = 0,
    accentColor: Color,
    customIcon: (@Composable () -> Unit)? = null,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = Color.White.copy(alpha = 0.06f),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(14.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                if (customIcon != null) {
                    customIcon()
                } else if (iconRes != 0) {
                    Icon(
                        painter = painterResource(id = iconRes),
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.65f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
