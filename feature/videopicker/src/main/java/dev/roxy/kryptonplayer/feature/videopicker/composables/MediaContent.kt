package dev.roxy.kryptonplayer.feature.videopicker.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.LoadingIndicatorDefaults
import androidx.compose.material3.toShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.roxy.kryptonplayer.core.ui.R
import dev.roxy.kryptonplayer.core.ui.designsystem.NextIcons
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

const val CIRCULAR_PROGRESS_INDICATOR_TEST_TAG = "circularProgressIndicator"

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CenterCircularProgressBar(modifier: Modifier = Modifier) {
    var containerShape by remember {
        mutableStateOf(LoadingIndicatorDefaults.IndeterminateIndicatorPolygons.first())
    }

    LaunchedEffect(Unit) {
        while (isActive) {
            containerShape = LoadingIndicatorDefaults.IndeterminateIndicatorPolygons.random()
            delay(500)
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Crossfade(
            targetState = containerShape,
            modifier = Modifier.wrapContentSize(),
            label = "CenterCircularProgressBarMorph",
        ) { shape ->
            ContainedLoadingIndicator(
                modifier = Modifier.size(56.dp),
                polygons = LoadingIndicatorDefaults.IndeterminateIndicatorPolygons,
                containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                indicatorColor = MaterialTheme.colorScheme.primary,
                containerShape = shape.toShape(),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun NoVideosFound(contentPadding: PaddingValues) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(contentPadding)
            .padding(
                horizontal = 24.dp,
                vertical = 40.dp,
            ),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(24.dp),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = NextIcons.Video,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(40.dp),
            )
        }
        Spacer(modifier = Modifier.size(16.dp))
        Text(
            text = stringResource(id = R.string.no_videos_found),
            style = MaterialTheme.typography.titleLargeEmphasized,
        )
    }
}
