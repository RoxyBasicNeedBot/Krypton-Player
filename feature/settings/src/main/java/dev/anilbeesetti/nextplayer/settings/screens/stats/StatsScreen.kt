package dev.anilbeesetti.nextplayer.settings.screens.stats

import android.text.format.DateUtils
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ListItem
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import dev.anilbeesetti.nextplayer.core.ui.theme.PremiumMeshBackground
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.anilbeesetti.nextplayer.core.database.dao.MediumDao
import dev.anilbeesetti.nextplayer.core.database.dao.MediumStateDao
import dev.anilbeesetti.nextplayer.core.database.entities.MediumEntity
import dev.anilbeesetti.nextplayer.core.database.entities.MediumStateEntity
import dev.anilbeesetti.nextplayer.core.ui.components.NextTopAppBar
import dev.anilbeesetti.nextplayer.core.ui.designsystem.NextIcons
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject

enum class StatsFilter {
    ALL_TIME,
    LAST_7_DAYS,
    LAST_30_DAYS,
    LAST_YEAR
}

data class FolderWatchStat(
    val folderPath: String,
    val folderName: String,
    val watchTimeMs: Long,
    val videoCount: Int
)

data class VideoWatchStat(
    val video: MediumEntity,
    val state: MediumStateEntity,
    val watchTimeMs: Long,
    val percentWatched: Float
)

data class StatsSummary(
    val totalWatchTimeMs: Long,
    val watchedVideosCount: Int,
    val totalVideosInLibrary: Int,
    val uniqueFoldersCount: Int,
    val totalLibrarySize: Long
)

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val mediumDao: MediumDao,
    private val mediumStateDao: MediumStateDao,
) : ViewModel() {

    val selectedFilter = MutableStateFlow(StatsFilter.ALL_TIME)

    private val allMediaFlow = mediumDao.getAll()
    private val allMediaStateFlow = mediumStateDao.getAll()

    val statsState = combine(
        allMediaFlow,
        allMediaStateFlow,
        selectedFilter
    ) { mediaList, stateList, filter ->
        val stateMap = stateList.associateBy { it.uriString }
        val now = System.currentTimeMillis()
        val filterTimeLimit = when (filter) {
            StatsFilter.ALL_TIME -> 0L
            StatsFilter.LAST_7_DAYS -> now - TimeUnit.DAYS.toMillis(7)
            StatsFilter.LAST_30_DAYS -> now - TimeUnit.DAYS.toMillis(30)
            StatsFilter.LAST_YEAR -> now - TimeUnit.DAYS.toMillis(365)
        }

        // Filter watched items by date
        val filteredStates = stateList.filter { state ->
            val playedTime = state.lastPlayedTime ?: 0L
            playedTime >= filterTimeLimit
        }
        val filteredStateMap = filteredStates.associateBy { it.uriString }

        // Summary Calculations
        val totalWatchTime = filteredStates.sumOf { it.playbackPosition }
        val watchedVideosCount = filteredStates.size
        val totalVideosInLibrary = mediaList.size
        val uniqueFoldersCount = mediaList.map { it.parentPath }.distinct().size
        val totalSize = mediaList.sumOf { it.size }

        val summary = StatsSummary(
            totalWatchTimeMs = totalWatchTime,
            watchedVideosCount = watchedVideosCount,
            totalVideosInLibrary = totalVideosInLibrary,
            uniqueFoldersCount = uniqueFoldersCount,
            totalLibrarySize = totalSize
        )

        // Top Played Videos
        val topVideos = mediaList.mapNotNull { video ->
            val state = filteredStateMap[video.uriString] ?: return@mapNotNull null
            val watchTime = state.playbackPosition
            val percent = if (video.duration > 0) watchTime.toFloat() / video.duration else 0f
            VideoWatchStat(video, state, watchTime, percent.coerceIn(0f, 1f))
        }.sortedByDescending { it.watchTimeMs }

        // Folder Distribution
        val folderStats = mediaList.groupBy { it.parentPath }.map { (path, videos) ->
            val folderName = path.substringAfterLast('/')
            val watchTime = videos.sumOf { video ->
                filteredStateMap[video.uriString]?.playbackPosition ?: 0L
            }
            FolderWatchStat(path, folderName, watchTime, videos.size)
        }.filter { it.watchTimeMs > 0 }.sortedByDescending { it.watchTimeMs }

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
            val day = cal.get(Calendar.DAY_OF_WEEK) - 1 // Calendar.SUNDAY is 1, convert to 0-indexed
            dailySlots[day] += 1
        }
        val dailyTrend = dailySlots.mapIndexed { day, count -> day to count }

        // Favorite Folder
        val favoriteFolder = folderStats.firstOrNull()
        // Favorite Video
        val favoriteVideo = topVideos.firstOrNull()

        StatsUiState(
            summary = summary,
            topVideos = topVideos,
            folderStats = folderStats,
            hourlyTrend = hourlyTrend,
            dailyTrend = dailyTrend,
            favoriteFolder = favoriteFolder,
            favoriteVideo = favoriteVideo
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = StatsUiState()
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
    val favoriteVideo: VideoWatchStat? = null
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
                    }
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
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Filter Row
                item {
                    FilterRow(
                        selectedFilter = filter,
                        onFilterSelected = viewModel::setFilter
                    )
                }

                // Cards Summary Grid
                item {
                    StatsSummarySection(summary = uiState.summary)
                }

                // Highlights Section
                item {
                    HighlightsSection(
                        favoriteFolder = uiState.favoriteFolder,
                        favoriteVideo = uiState.favoriteVideo
                    )
                }

                // Folder Donut Chart
                item {
                    if (uiState.folderStats.isNotEmpty()) {
                        FolderDistributionChart(folderStats = uiState.folderStats)
                    }
                }

                // Daily Patterns Graph
                item {
                    if (uiState.dailyTrend.any { it.second > 0 }) {
                        DailyWatchChart(dailyTrend = uiState.dailyTrend)
                    }
                }

                // Hourly Patterns Graph
                item {
                    if (uiState.hourlyTrend.any { it.second > 0 }) {
                        HourlyWatchChart(hourlyTrend = uiState.hourlyTrend)
                    }
                }

                // Ranked Videos List Header
                item {
                    if (uiState.topVideos.isNotEmpty()) {
                        Text(
                            text = "Top Played Videos",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }

                // Ranked Videos
                val maxWatchTime = uiState.topVideos.firstOrNull()?.watchTimeMs ?: 1L
                itemsIndexed(uiState.topVideos.take(10)) { index, stat ->
                    val playFraction = stat.watchTimeMs.toFloat() / maxWatchTime
                    val progressColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)

                    ListItem(
                        headlineContent = {
                            Text(
                                text = "${index + 1}. ${stat.video.name}",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        supportingContent = {
                            Text(
                                text = "Watched ${formatMillisToTime(stat.watchTimeMs)} • ${stat.video.parentPath.substringAfterLast('/')}"
                            )
                        },
                        leadingContent = {
                            Icon(
                                imageVector = Icons.Default.Movie,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.size(24.dp)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                            .drawBehind {
                                drawRect(
                                    color = progressColor,
                                    size = Size(size.width * playFraction, size.height),
                                )
                            }
                    )
                }
            }
        }
    }
}

@Composable
private fun FilterRow(
    selectedFilter: StatsFilter,
    onFilterSelected: (StatsFilter) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatsFilter.entries.forEach { f ->
            val label = when (f) {
                StatsFilter.ALL_TIME -> "All Time"
                StatsFilter.LAST_7_DAYS -> "Last 7 Days"
                StatsFilter.LAST_30_DAYS -> "Last 30 Days"
                StatsFilter.LAST_YEAR -> "Last Year"
            }
            val isSelected = f == selectedFilter
            val container = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh
            val content = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface

            Surface(
                shape = RoundedCornerShape(999.dp),
                color = container,
                contentColor = content,
                shadowElevation = if (isSelected) 4.dp else 0.dp,
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .clickable { onFilterSelected(f) }
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun StatsSummarySection(summary: StatsSummary) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        ElevatedCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.elevatedCardColors()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "Total Watch Time",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.secondary,
                )
                Text(
                    text = formatMillisToTime(summary.totalWatchTimeMs),
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            StatCard(
                label = "Videos Watched",
                value = summary.watchedVideosCount.toString(),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "Unique Folders",
                value = summary.uniqueFoldersCount.toString(),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label = "Library Size",
                value = formatBytes(summary.totalLibrarySize),
                modifier = Modifier.weight(1.2f)
            )
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.elevatedCardColors()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HighlightsSection(
    favoriteFolder: FolderWatchStat?,
    favoriteVideo: VideoWatchStat?
) {
    if (favoriteFolder == null && favoriteVideo == null) return

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (favoriteFolder != null) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.size(54.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Folder,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "FAVORITE WATCHING FOLDER",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = favoriteFolder.folderName,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Watched ${formatMillisToTime(favoriteFolder.watchTimeMs)} across ${favoriteFolder.videoCount} videos",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        if (favoriteVideo != null) {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.tertiaryContainer,
                        modifier = Modifier.size(54.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Movie,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                    Column {
                        Text(
                            text = "MOST WATCHED VIDEO",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                        Text(
                            text = favoriteVideo.video.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Watched ${formatMillisToTime(favoriteVideo.watchTimeMs)} • ${(favoriteVideo.percentWatched * 100).toInt()}% completed",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderDistributionChart(folderStats: List<FolderWatchStat>) {
    val totalWatchTime = remember(folderStats) { folderStats.sumOf { it.watchTimeMs } }
    if (totalWatchTime == 0L) return

    val segmentColors = listOf(
        Color(0xFF6750A4), Color(0xFF4A8FA8), Color(0xFF4CAF50),
        Color(0xFFFF9800), Color(0xFFE91E63),
    )

    val segmentData = remember(folderStats, totalWatchTime) {
        var startAngle = -90f
        folderStats.take(5).mapNotNull { stat ->
            val sweep = (stat.watchTimeMs.toFloat() / totalWatchTime) * 360f
            if (sweep < 1f) return@mapNotNull null
            val entry = Triple(stat, startAngle, sweep)
            startAngle += sweep
            entry
        }
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Folder Distributions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .drawWithCache {
                            val strokeWidth = size.width * 0.16f
                            val inset = strokeWidth / 2f
                            val arcRect = Rect(
                                left = inset, top = inset,
                                right = size.width - inset, bottom = size.height - inset,
                            )
                            onDrawBehind {
                                segmentData.forEachIndexed { i, (_, startAngle, sweep) ->
                                    val gapDeg = if (segmentData.size > 1) 2.5f else 0f
                                    drawArc(
                                        color = segmentColors[i % segmentColors.size],
                                        startAngle = startAngle + gapDeg / 2f,
                                        sweepAngle = sweep - gapDeg,
                                        useCenter = false,
                                        topLeft = arcRect.topLeft,
                                        size = Size(arcRect.width, arcRect.height),
                                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                                    )
                                }
                            }
                        }
                )

                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    segmentData.forEachIndexed { i, (stat, _, sweep) ->
                        val percentage = (sweep / 360f * 100).toInt()
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(10.dp)
                                    .clip(CircleShape)
                                    .background(segmentColors[i % segmentColors.size])
                            )
                            Text(
                                text = stat.folderName,
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "$percentage%",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
    val days = listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat")
    val maxCount = remember(dailyTrend) { dailyTrend.maxOfOrNull { it.second } ?: 1 }
    val primaryColor = MaterialTheme.colorScheme.primary
    val barBgColor = MaterialTheme.colorScheme.secondaryContainer

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Watching Days Trend",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                dailyTrend.forEach { (day, count) ->
                    val fraction = count.toFloat() / maxCount
                    val animatedFraction by animateFloatAsState(
                        targetValue = fraction,
                        animationSpec = tween(450),
                        label = "day_bar_$day"
                    )

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .width(22.dp)
                                .height(80.dp),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            // Track
                            Box(
                                modifier = Modifier
                                    .width(22.dp)
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.5f))
                            )
                            // Filled Bar
                            Box(
                                modifier = Modifier
                                    .width(22.dp)
                                    .height((80 * animatedFraction).dp.coerceAtLeast(3.dp))
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (count == maxCount && count > 0) primaryColor else barBgColor)
                            )
                        }
                        Text(
                            text = days[day],
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HourlyWatchChart(hourlyTrend: List<Pair<Int, Int>>) {
    val maxCount = remember(hourlyTrend) { hourlyTrend.maxOfOrNull { it.second } ?: 1 }
    val primaryColor = MaterialTheme.colorScheme.primary
    val areaColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = "Hourly Watch Patterns",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(3.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                hourlyTrend.forEach { (hour, count) ->
                    val fraction = count.toFloat() / maxCount
                    val animatedFraction by animateFloatAsState(
                        targetValue = fraction,
                        animationSpec = tween(400),
                        label = "hour_bar_$hour"
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(60.dp),
                        contentAlignment = Alignment.BottomCenter
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height((60 * animatedFraction).dp.coerceAtLeast(2.dp))
                                .clip(RoundedCornerShape(topStart = 2.dp, topEnd = 2.dp))
                                .background(if (count == maxCount && count > 0) primaryColor else areaColor)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = "12 AM", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = "6 AM", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = "12 PM", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = "6 PM", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = "11 PM", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// Helpers
private fun formatMillisToTime(ms: Long): String {
    val totalSecs = ms / 1000
    val hours = totalSecs / 3600
    val minutes = (totalSecs % 3600) / 60
    return if (hours > 0) {
        "${hours}h ${minutes}m"
    } else {
        "${minutes}m"
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = listOf("B", "KB", "MB", "GB", "TB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    return String.format("%.1f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
}
