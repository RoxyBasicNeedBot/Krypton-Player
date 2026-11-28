package dev.roxy.kryptonplayer.settings.screens.stats

import android.net.Uri
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.roxy.kryptonplayer.core.database.dao.MediumDao
import dev.roxy.kryptonplayer.core.database.dao.MediumStateDao
import dev.roxy.kryptonplayer.core.database.entities.MediumEntity
import dev.roxy.kryptonplayer.core.database.entities.MediumStateEntity
import dev.roxy.kryptonplayer.core.ui.components.NextTopAppBar
import dev.roxy.kryptonplayer.core.ui.designsystem.NextIcons
import dev.roxy.kryptonplayer.core.ui.preview.DayNightPreview
import dev.roxy.kryptonplayer.core.ui.theme.KryptonPlayerTheme
import dev.roxy.kryptonplayer.core.ui.theme.PremiumMeshBackground
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.log10
import kotlin.math.pow
import kotlin.math.roundToInt

private const val CLOUD_STREAMS_FOLDER_PATH = "__cloud_streams__"
private const val CLOUD_STREAMS_FOLDER_NAME = "Cloud & Network Streams"
private const val OTHER_FOLDERS_PATH = "__other_folders__"

enum class StatsFilter {
    ALL_TIME,
    LAST_7_DAYS,
    LAST_30_DAYS,
    LAST_YEAR,
}

data class FolderWatchStat(
    val folderPath: String,
    val folderName: String,
    val watchTimeMs: Long,
    val videoCount: Int,
)

data class VideoWatchStat(
    val video: MediumEntity,
    val state: MediumStateEntity,
    val watchTimeMs: Long,
    val percentWatched: Float,
)

data class StatsSummary(
    val totalWatchTimeMs: Long,
    val watchedVideosCount: Int,
    val totalVideosInLibrary: Int,
    val uniqueFoldersCount: Int,
    val totalLibrarySize: Long,
)

private data class FolderArcSegment(
    val stat: FolderWatchStat,
    val startAngle: Float,
    val sweepAngle: Float,
    val fraction: Float,
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    mediumDao: MediumDao,
    mediumStateDao: MediumStateDao,
) : ViewModel() {

    val selectedFilter = MutableStateFlow(StatsFilter.ALL_TIME)

    private val allMediaFlow = mediumDao.getAll()
    private val allMediaStateFlow = mediumStateDao.getAll()

    val statsState = combine(
        allMediaFlow,
        allMediaStateFlow,
        selectedFilter,
    ) { mediaList, stateList, filter ->
        val mediaMap = mediaList.associateBy { it.uriString }
        val now = System.currentTimeMillis()
        val filterTimeLimit = when (filter) {
            StatsFilter.ALL_TIME -> 0L
            StatsFilter.LAST_7_DAYS -> now - TimeUnit.DAYS.toMillis(7)
            StatsFilter.LAST_30_DAYS -> now - TimeUnit.DAYS.toMillis(30)
            StatsFilter.LAST_YEAR -> now - TimeUnit.DAYS.toMillis(365)
        }

        val filteredStates = stateList.filter { state ->
            val effectiveWatchMs = state.effectiveWatchDurationMs()
            when (filter) {
                StatsFilter.ALL_TIME -> state.lastPlayedTime != null || effectiveWatchMs > 0L
                StatsFilter.LAST_7_DAYS,
                StatsFilter.LAST_30_DAYS,
                StatsFilter.LAST_YEAR,
                -> (state.lastPlayedTime ?: 0L) >= filterTimeLimit
            }
        }
        val filteredStateMap = filteredStates.associateBy { it.uriString }

        // Summary calculations using cumulative effective watch duration
        val totalWatchTime = filteredStates.sumOf { it.effectiveWatchDurationMs() }
        val watchedVideosCount = filteredStates.size
        val totalVideosInLibrary = mediaList.size
        val uniqueFoldersCount = mediaList.map { it.parentPath }.distinct().size
        val totalSize = mediaList.sumOf { it.size }

        val summary = StatsSummary(
            totalWatchTimeMs = totalWatchTime,
            watchedVideosCount = watchedVideosCount,
            totalVideosInLibrary = totalVideosInLibrary,
            uniqueFoldersCount = uniqueFoldersCount,
            totalLibrarySize = totalSize,
        )

        // Top Played Videos (includes indexed library videos + cloud/network streams)
        val topVideos = filteredStates.mapNotNull { state ->
            val watchTime = state.effectiveWatchDurationMs()
            if (watchTime <= 0L) return@mapNotNull null
            val video = mediaMap[state.uriString] ?: createStreamFallbackMedium(state)
            val percent = if (video.duration > 0L) {
                (watchTime.toFloat() / video.duration).coerceIn(0f, 1f)
            } else {
                0f
            }
            VideoWatchStat(video, state, watchTime, percent)
        }.sortedByDescending { it.watchTimeMs }

        // Folder Distribution (library folders + Cloud & Network Streams bucket)
        val indexedFolderStats = mediaList.groupBy { it.parentPath }.mapNotNull { (path, videos) ->
            val watchTime = videos.sumOf { video ->
                filteredStateMap[video.uriString]?.effectiveWatchDurationMs() ?: 0L
            }
            if (watchTime <= 0L) return@mapNotNull null
            val watchedInFolder = videos.count { video ->
                val state = filteredStateMap[video.uriString]
                state != null && state.effectiveWatchDurationMs() > 0L
            }
            FolderWatchStat(
                folderPath = path,
                folderName = folderDisplayName(path),
                watchTimeMs = watchTime,
                videoCount = watchedInFolder.coerceAtLeast(1),
            )
        }
        val unindexedWatchedStates = filteredStates.filter { state ->
            state.uriString !in mediaMap && state.effectiveWatchDurationMs() > 0L
        }
        val unindexedWatchTime = unindexedWatchedStates.sumOf { it.effectiveWatchDurationMs() }
        val folderStats = buildList {
            addAll(indexedFolderStats)
            if (unindexedWatchTime > 0L) {
                add(
                    FolderWatchStat(
                        folderPath = CLOUD_STREAMS_FOLDER_PATH,
                        folderName = CLOUD_STREAMS_FOLDER_NAME,
                        watchTimeMs = unindexedWatchTime,
                        videoCount = unindexedWatchedStates.size,
                    ),
                )
            }
        }.sortedByDescending { it.watchTimeMs }

        // Hourly Watch Pattern (0-23 hours)
        val hourlySlots = IntArray(24)
        filteredStates.forEach { state ->
            val time = state.lastPlayedTime ?: return@forEach
            val cal = Calendar.getInstance().apply { timeInMillis = time }
            val hour = cal.get(Calendar.HOUR_OF_DAY)
            hourlySlots[hour] += 1
        }
        val hourlyTrend = hourlySlots.mapIndexed { hour, count -> hour to count }

        // Daily Watch Pattern (0=Sun, 1=Mon, ..., 6=Sat)
        val dailySlots = IntArray(7)
        filteredStates.forEach { state ->
            val time = state.lastPlayedTime ?: return@forEach
            val cal = Calendar.getInstance().apply { timeInMillis = time }
            val day = cal.get(Calendar.DAY_OF_WEEK) - 1
            dailySlots[day] += 1
        }
        val dailyTrend = dailySlots.mapIndexed { day, count -> day to count }

        StatsUiState(
            summary = summary,
            topVideos = topVideos,
            folderStats = folderStats,
            hourlyTrend = hourlyTrend,
            dailyTrend = dailyTrend,
            favoriteFolder = folderStats.firstOrNull(),
            favoriteVideo = topVideos.firstOrNull(),
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StatsUiState(),
    )

    fun setFilter(filter: StatsFilter) {
        selectedFilter.value = filter
    }
}

data class StatsUiState(
    val summary: StatsSummary = StatsSummary(0, 0, 0, 0, 0),
    val topVideos: List<VideoWatchStat> = emptyList(),
    val folderStats: List<FolderWatchStat> = emptyList(),
    val hourlyTrend: List<Pair<Int, Int>> = emptyList(),
    val dailyTrend: List<Pair<Int, Int>> = emptyList(),
    val favoriteFolder: FolderWatchStat? = null,
    val favoriteVideo: VideoWatchStat? = null,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    onNavigateUp: () -> Unit,
    viewModel: StatsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.statsState.collectAsState()
    val filter by viewModel.selectedFilter.collectAsState()
    val lazyListState = rememberLazyListState()
    val hasWatchActivity = uiState.summary.watchedVideosCount > 0 ||
        uiState.topVideos.isNotEmpty() ||
        uiState.folderStats.isNotEmpty()

    PremiumMeshBackground {
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                NextTopAppBar(
                    title = "Library Statistics",
                    navigationIcon = {
                        FilledTonalIconButton(onClick = onNavigateUp) {
                            Icon(
                                imageVector = NextIcons.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
                    },
                )
            },
        ) { innerPadding ->
            LazyColumn(
                state = lazyListState,
                contentPadding = PaddingValues(bottom = 32.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item {
                    FilterRow(
                        selectedFilter = filter,
                        onFilterSelected = viewModel::setFilter,
                    )
                }

                item {
                    StatsSummarySection(summary = uiState.summary)
                }

                if (!hasWatchActivity) {
                    item {
                        EmptyStatsStateCard(filter = filter)
                    }
                }

                if (uiState.favoriteFolder != null || uiState.favoriteVideo != null) {
                    item {
                        HighlightsSection(
                            favoriteFolder = uiState.favoriteFolder,
                            favoriteVideo = uiState.favoriteVideo,
                        )
                    }
                }

                if (uiState.folderStats.isNotEmpty()) {
                    item {
                        FolderDistributionChart(folderStats = uiState.folderStats)
                    }
                }

                if (uiState.dailyTrend.any { it.second > 0 }) {
                    item {
                        DailyWatchChart(dailyTrend = uiState.dailyTrend)
                    }
                }

                if (uiState.hourlyTrend.any { it.second > 0 }) {
                    item {
                        HourlyWatchChart(hourlyTrend = uiState.hourlyTrend)
                    }
                }

                if (uiState.topVideos.isNotEmpty()) {
                    item {
                        Text(
                            text = "TOP PLAYED VIDEOS",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.9.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 6.dp, start = 4.dp),
                        )
                    }

                    val maxWatchTime = (uiState.topVideos.firstOrNull()?.watchTimeMs ?: 0L).coerceAtLeast(1L)
                    itemsIndexed(uiState.topVideos.take(10)) { index, stat ->
                        val playFraction = (stat.watchTimeMs.toFloat() / maxWatchTime).coerceIn(0f, 1f)
                        TopPlayedVideoRow(
                            rank = index + 1,
                            stat = stat,
                            playFraction = playFraction,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatsMochiCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 22.dp,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerLow,
    borderColor: Color = MaterialTheme.colorScheme.outlineVariant,
    content: @Composable () -> Unit,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(cornerRadius),
        color = containerColor,
        border = BorderStroke(0.9.dp, borderColor),
        content = content,
    )
}

@Composable
private fun TopPlayedVideoRow(
    rank: Int,
    stat: VideoWatchStat,
    playFraction: Float,
) {
    val colorScheme = MaterialTheme.colorScheme
    val progressColor = colorScheme.primary.copy(alpha = 0.10f)
    val folderName = folderDisplayName(stat.video.parentPath)

    StatsMochiCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 18.dp,
    ) {
        ListItem(
            colors = ListItemDefaults.colors(containerColor = Color.Transparent),
            headlineContent = {
                Text(
                    text = "$rank. ${stat.video.name}",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            supportingContent = {
                Text(
                    text = "Watched ${formatMillisToTime(stat.watchTimeMs)} • $folderName",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            leadingContent = {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colorScheme.surfaceContainer,
                    border = BorderStroke(0.8.dp, colorScheme.outlineVariant),
                    modifier = Modifier.size(40.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Movie,
                            contentDescription = null,
                            tint = colorScheme.primary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawRect(
                        color = progressColor,
                        size = Size(size.width * playFraction, size.height),
                    )
                },
        )
    }
}

@Composable
private fun EmptyStatsStateCard(filter: StatsFilter) {
    val colorScheme = MaterialTheme.colorScheme
    val title = if (filter == StatsFilter.ALL_TIME) {
        "No Watch History Yet"
    } else {
        "No Activity in ${filterLabel(filter)}"
    }
    val subtitle = if (filter == StatsFilter.ALL_TIME) {
        "Play a local video or stream to start tracking real watch time, folder breakdowns, and viewing habits."
    } else {
        "No playback activity matched this time range. Try selecting All Time or play a video."
    }

    StatsMochiCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 22.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Surface(
                shape = CircleShape,
                color = colorScheme.surfaceContainer,
                border = BorderStroke(0.9.dp, colorScheme.outlineVariant),
                modifier = Modifier.size(56.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Default.BarChart,
                        contentDescription = null,
                        tint = colorScheme.primary,
                        modifier = Modifier.size(26.dp),
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun FilterRow(
    selectedFilter: StatsFilter,
    onFilterSelected: (StatsFilter) -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        StatsFilter.entries.forEach { f ->
            val label = filterLabel(f)
            val isSelected = f == selectedFilter
            val container by animateColorAsState(
                targetValue = if (isSelected) colorScheme.primary else colorScheme.surfaceContainerLow,
                animationSpec = tween(180),
                label = "filter_bg_$f",
            )
            val content by animateColorAsState(
                targetValue = if (isSelected) colorScheme.onPrimary else colorScheme.onSurfaceVariant,
                animationSpec = tween(180),
                label = "filter_fg_$f",
            )
            val borderColor = if (isSelected) Color.Transparent else colorScheme.outlineVariant

            Surface(
                shape = RoundedCornerShape(999.dp),
                color = container,
                contentColor = content,
                border = BorderStroke(0.9.dp, borderColor),
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .clickable { onFilterSelected(f) },
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
                )
            }
        }
    }
}

@Composable
private fun StatsSummarySection(summary: StatsSummary) {
    val colorScheme = MaterialTheme.colorScheme
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        StatsMochiCard(
            modifier = Modifier.fillMaxWidth(),
            cornerRadius = 22.dp,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.04f),
                                Color.Transparent,
                            ),
                        ),
                    )
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    Text(
                        text = "TOTAL WATCH TIME",
                        style = MaterialTheme.typography.labelSmall,
                        letterSpacing = 0.9.sp,
                        color = colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = formatMillisToTime(summary.totalWatchTimeMs),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.primary,
                    )
                    Text(
                        text = "${summary.watchedVideosCount} watched • ${summary.totalVideosInLibrary} in library",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant,
                    )
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = colorScheme.surfaceContainer,
                    border = BorderStroke(0.9.dp, colorScheme.outlineVariant),
                    modifier = Modifier.size(48.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = colorScheme.primary,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StatCard(
                label = "Watched",
                value = summary.watchedVideosCount.toString(),
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label = "Folders",
                value = summary.uniqueFoldersCount.toString(),
                modifier = Modifier.weight(1f),
            )
            StatCard(
                label = "Library Size",
                value = formatBytes(summary.totalLibrarySize),
                modifier = Modifier.weight(1.25f),
            )
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    StatsMochiCard(
        modifier = modifier,
        cornerRadius = 22.dp,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 15.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun HighlightsSection(
    favoriteFolder: FolderWatchStat?,
    favoriteVideo: VideoWatchStat?,
) {
    if (favoriteFolder == null && favoriteVideo == null) return
    val colorScheme = MaterialTheme.colorScheme

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (favoriteFolder != null) {
            val videoWord = if (favoriteFolder.videoCount == 1) "video" else "videos"
            StatsMochiCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 22.dp,
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = colorScheme.surfaceContainer,
                        border = BorderStroke(0.9.dp, colorScheme.outlineVariant),
                        modifier = Modifier.size(50.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = colorScheme.primary,
                                modifier = Modifier.size(25.dp),
                            )
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "FAVORITE WATCHING FOLDER",
                            style = MaterialTheme.typography.labelSmall,
                            letterSpacing = 0.7.sp,
                            color = colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = favoriteFolder.folderName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "Watched ${formatMillisToTime(favoriteFolder.watchTimeMs)} across ${favoriteFolder.videoCount} $videoWord",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        if (favoriteVideo != null) {
            val completionSuffix = if (favoriteVideo.video.duration > 0L) {
                " • ${(favoriteVideo.percentWatched * 100).toInt()}% completed"
            } else {
                ""
            }
            StatsMochiCard(
                modifier = Modifier.fillMaxWidth(),
                cornerRadius = 22.dp,
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = colorScheme.surfaceContainer,
                        border = BorderStroke(0.9.dp, colorScheme.outlineVariant),
                        modifier = Modifier.size(50.dp),
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Movie,
                                contentDescription = null,
                                tint = colorScheme.tertiary,
                                modifier = Modifier.size(25.dp),
                            )
                        }
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "MOST WATCHED VIDEO",
                            style = MaterialTheme.typography.labelSmall,
                            letterSpacing = 0.7.sp,
                            color = colorScheme.tertiary,
                        )
                        Text(
                            text = favoriteVideo.video.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "Watched ${formatMillisToTime(favoriteVideo.watchTimeMs)}$completionSuffix",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderDistributionChart(folderStats: List<FolderWatchStat>) {
    val displayFolders = remember(folderStats) {
        val topFolders = folderStats.take(4)
        val remaining = folderStats.drop(4)
        val otherWatchMs = remaining.sumOf { it.watchTimeMs }
        if (remaining.isNotEmpty() && otherWatchMs > 0L) {
            topFolders + FolderWatchStat(
                folderPath = OTHER_FOLDERS_PATH,
                folderName = "Other",
                watchTimeMs = otherWatchMs,
                videoCount = remaining.sumOf { it.videoCount },
            )
        } else {
            topFolders
        }
    }
    val totalWatchTime = remember(displayFolders) { displayFolders.sumOf { it.watchTimeMs } }
    if (totalWatchTime <= 0L) return

    val colorScheme = MaterialTheme.colorScheme
    val segmentColors = listOf(
        Color(0xFF9B87F5),
        Color(0xFF4CC9F0),
        Color(0xFF4ADE80),
        Color(0xFFFFB703),
        Color(0xFFF472B6),
    )

    val segmentData = remember(displayFolders, totalWatchTime) {
        var startAngle = -90f
        displayFolders.map { stat ->
            val fraction = (stat.watchTimeMs.toFloat() / totalWatchTime).coerceIn(0f, 1f)
            val sweep = fraction * 360f
            val entry = FolderArcSegment(
                stat = stat,
                startAngle = startAngle,
                sweepAngle = sweep,
                fraction = fraction,
            )
            startAngle += sweep
            entry
        }
    }

    StatsMochiCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 22.dp,
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Folder Distributions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 14.dp),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(120.dp)
                        .drawWithCache {
                            val strokeWidth = size.width * 0.16f
                            val inset = strokeWidth / 2f
                            val arcRect = Rect(
                                left = inset,
                                top = inset,
                                right = size.width - inset,
                                bottom = size.height - inset,
                            )
                            onDrawBehind {
                                segmentData.forEachIndexed { i, segment ->
                                    val gapDeg = if (segmentData.size > 1) {
                                        minOf(2.5f, segment.sweepAngle * 0.4f)
                                    } else {
                                        0f
                                    }
                                    val drawSweep = (segment.sweepAngle - gapDeg).coerceAtLeast(0.4f)
                                    drawArc(
                                        color = segmentColors[i % segmentColors.size],
                                        startAngle = segment.startAngle + gapDeg / 2f,
                                        sweepAngle = drawSweep,
                                        useCenter = false,
                                        topLeft = arcRect.topLeft,
                                        size = Size(arcRect.width, arcRect.height),
                                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                                    )
                                }
                            }
                        },
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = formatMillisToTime(totalWatchTime),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface,
                            maxLines = 1,
                        )
                        Text(
                            text = "${folderStats.size} folders",
                            style = MaterialTheme.typography.labelSmall,
                            color = colorScheme.onSurfaceVariant,
                            maxLines = 1,
                        )
                    }
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f),
                ) {
                    segmentData.forEachIndexed { i, segment ->
                        val pct = (segment.fraction * 100f).roundToInt()
                        val pctText = if (pct == 0 && segment.stat.watchTimeMs > 0L) "<1%" else "$pct%"
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(segmentColors[i % segmentColors.size]),
                            )
                            Text(
                                text = segment.stat.folderName,
                                style = MaterialTheme.typography.labelMedium,
                                color = colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = "${formatMillisToTime(segment.stat.watchTimeMs)} • $pctText",
                                style = MaterialTheme.typography.labelSmall,
                                color = colorScheme.onSurfaceVariant,
                                maxLines = 1,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DailyWatchChart(dailyTrend: List<Pair<Int, Int>>) {
    val colorScheme = MaterialTheme.colorScheme
    val days = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    val maxCount = remember(dailyTrend) { (dailyTrend.maxOfOrNull { it.second } ?: 0).coerceAtLeast(1) }
    val primaryColor = colorScheme.primary
    val activeBarColor = colorScheme.primary.copy(alpha = 0.45f)
    val emptyBarColor = colorScheme.surfaceContainerHighest.copy(alpha = 0.6f)

    StatsMochiCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 22.dp,
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Watching Days Trend",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 14.dp),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom,
            ) {
                dailyTrend.forEach { (day, count) ->
                    val fraction = (count.toFloat() / maxCount).coerceIn(0f, 1f)
                    val animatedFraction by animateFloatAsState(
                        targetValue = fraction,
                        animationSpec = tween(450),
                        label = "day_bar_$day",
                    )
                    val barColor = when {
                        count <= 0 -> emptyBarColor
                        count == maxCount -> primaryColor
                        else -> activeBarColor
                    }

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Box(
                            modifier = Modifier
                                .width(22.dp)
                                .height(80.dp),
                            contentAlignment = Alignment.BottomCenter,
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(22.dp)
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(colorScheme.surfaceContainer),
                            )
                            Box(
                                modifier = Modifier
                                    .width(22.dp)
                                    .height((80 * animatedFraction).dp.coerceAtLeast(3.dp))
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(barColor),
                            )
                        }
                        Text(
                            text = days.getOrElse(day) { "" },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (count == maxCount && count > 0) primaryColor else colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HourlyWatchChart(hourlyTrend: List<Pair<Int, Int>>) {
    val colorScheme = MaterialTheme.colorScheme
    val maxCount = remember(hourlyTrend) { (hourlyTrend.maxOfOrNull { it.second } ?: 0).coerceAtLeast(1) }
    val primaryColor = colorScheme.primary
    val activeBarColor = colorScheme.primary.copy(alpha = 0.42f)
    val emptyBarColor = colorScheme.surfaceContainerHighest.copy(alpha = 0.45f)

    StatsMochiCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 22.dp,
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Hourly Watch Patterns",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = colorScheme.onSurface,
                modifier = Modifier.padding(bottom = 14.dp),
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                hourlyTrend.forEach { (hour, count) ->
                    val fraction = (count.toFloat() / maxCount).coerceIn(0f, 1f)
                    val animatedFraction by animateFloatAsState(
                        targetValue = fraction,
                        animationSpec = tween(400),
                        label = "hour_bar_$hour",
                    )
                    val barColor = when {
                        count <= 0 -> emptyBarColor
                        count == maxCount -> primaryColor
                        else -> activeBarColor
                    }

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(60.dp),
                        contentAlignment = Alignment.BottomCenter,
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height((60 * animatedFraction).dp.coerceAtLeast(2.dp))
                                .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp))
                                .background(barColor),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(text = "12 AM", style = MaterialTheme.typography.labelSmall, color = colorScheme.onSurfaceVariant)
                Text(text = "6 AM", style = MaterialTheme.typography.labelSmall, color = colorScheme.onSurfaceVariant)
                Text(text = "12 PM", style = MaterialTheme.typography.labelSmall, color = colorScheme.onSurfaceVariant)
                Text(text = "6 PM", style = MaterialTheme.typography.labelSmall, color = colorScheme.onSurfaceVariant)
                Text(text = "11 PM", style = MaterialTheme.typography.labelSmall, color = colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun createStreamFallbackMedium(state: MediumStateEntity): MediumEntity {
    return MediumEntity(
        uriString = state.uriString,
        path = state.uriString,
        name = resolveStreamFallbackTitle(state.uriString),
        parentPath = CLOUD_STREAMS_FOLDER_PATH,
        modified = state.lastPlayedTime ?: 0L,
        size = 0L,
        width = 0,
        height = 0,
        duration = 0L,
        mediaStoreId = 0L,
    )
}

private fun resolveStreamFallbackTitle(uriString: String): String {
    val parsed = runCatching { Uri.parse(uriString) }.getOrNull()
    val queryTitle = parsed?.let { uri ->
        listOf("name", "title", "filename")
            .firstNotNullOfOrNull { key ->
                runCatching { uri.getQueryParameter(key) }.getOrNull()?.takeIf { it.isNotBlank() }
            }
    }?.let { raw -> runCatching { Uri.decode(raw) }.getOrDefault(raw).trim() }
    if (!queryTitle.isNullOrBlank()) return queryTitle

    val lastSegment = parsed?.lastPathSegment
        ?.let { raw -> runCatching { Uri.decode(raw) }.getOrDefault(raw).trim() }
        ?.takeIf { it.isNotBlank() && !it.equals("stream", ignoreCase = true) }
    if (!lastSegment.isNullOrBlank()) return lastSegment

    return "Streamed Video"
}

private fun folderDisplayName(parentPath: String): String = when {
    parentPath == CLOUD_STREAMS_FOLDER_PATH -> CLOUD_STREAMS_FOLDER_NAME
    else -> parentPath.substringAfterLast('/').ifBlank { parentPath }
}

private fun filterLabel(filter: StatsFilter): String = when (filter) {
    StatsFilter.ALL_TIME -> "All Time"
    StatsFilter.LAST_7_DAYS -> "Last 7 Days"
    StatsFilter.LAST_30_DAYS -> "Last 30 Days"
    StatsFilter.LAST_YEAR -> "Last Year"
}

private fun formatMillisToTime(ms: Long): String {
    if (ms <= 0L) return "0m"
    if (ms < 60_000L) {
        val totalSecs = (ms / 1000L).coerceAtLeast(1L)
        return "${totalSecs}s"
    }
    val totalMinutes = ms / 60_000L
    val hours = totalMinutes / 60L
    val minutes = totalMinutes % 60L
    return if (hours > 0L) {
        "${hours}h ${minutes}m"
    } else {
        "${minutes}m"
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val units = listOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt().coerceIn(0, units.lastIndex)
    return String.format("%.1f %s", bytes / 1024.0.pow(digitGroups.toDouble()), units[digitGroups])
}

@DayNightPreview
@Composable
private fun StatCardPreview() {
    KryptonPlayerTheme {
        StatCard(
            label = "Total Playback",
            value = "12h 45m",
        )
    }
}

@DayNightPreview
@Composable
private fun StatsSummarySectionPreview() {
    KryptonPlayerTheme {
        StatsSummarySection(
            summary = StatsSummary(
                totalWatchTimeMs = 3600000L * 5,
                watchedVideosCount = 14,
                totalVideosInLibrary = 42,
                uniqueFoldersCount = 3,
                totalLibrarySize = 1024L * 1024 * 1024 * 5,
            ),
        )
    }
}
