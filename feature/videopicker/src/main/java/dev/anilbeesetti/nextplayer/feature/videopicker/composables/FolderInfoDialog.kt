package dev.anilbeesetti.nextplayer.feature.videopicker.composables

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.anilbeesetti.nextplayer.core.common.Utils
import dev.anilbeesetti.nextplayer.core.model.Folder
import dev.anilbeesetti.nextplayer.core.ui.R
import dev.anilbeesetti.nextplayer.core.ui.components.NextDialog
import dev.anilbeesetti.nextplayer.core.ui.designsystem.NextIcons

@Composable
fun FolderInfoDialog(
    folder: Folder,
    onDismiss: () -> Unit,
) {
    NextDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = NextIcons.Folder,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = folder.name.ifEmpty { "Folder Details" },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        content = {
            HorizontalDivider()
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 12.dp),
            ) {
                FolderInfoRow(
                    title = "Location",
                    value = folder.path,
                )
                FolderInfoRow(
                    title = "Videos",
                    value = if (folder.folderList.isNotEmpty()) {
                        "${folder.mediaList.size} in folder (${folder.allMediaList.size} total with subfolders)"
                    } else {
                        "${folder.mediaList.size} videos"
                    },
                )
                if (folder.folderList.isNotEmpty()) {
                    FolderInfoRow(
                        title = "Subfolders",
                        value = "${folder.folderList.size} folders",
                    )
                }
                FolderInfoRow(
                    title = "Total Size",
                    value = Utils.formatFileSize(folder.mediaSize),
                )
                if (folder.mediaDuration > 0L) {
                    FolderInfoRow(
                        title = "Total Duration",
                        value = Utils.formatDurationMillis(folder.mediaDuration),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.okay))
            }
        },
    )
}

@Composable
private fun FolderInfoRow(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
