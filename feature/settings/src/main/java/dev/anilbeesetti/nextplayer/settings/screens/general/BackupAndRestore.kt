package dev.anilbeesetti.nextplayer.settings.screens.general

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import dev.anilbeesetti.nextplayer.core.ui.designsystem.NextIcons
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dev.anilbeesetti.nextplayer.core.ui.components.NextTopAppBar
import dev.anilbeesetti.nextplayer.core.ui.theme.PremiumMeshBackground
import androidx.compose.material3.TopAppBarDefaults
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject

@HiltViewModel
class BackupRestoreViewModel @Inject constructor() : ViewModel() {

    private val uiStateInternal = MutableStateFlow<BackupRestoreState>(BackupRestoreState.Idle)
    val uiState = uiStateInternal.asStateFlow()

    fun performBackup(
        context: Context,
        uri: Uri,
        backupDb: Boolean,
        backupPrefs: Boolean,
        backupHistory: Boolean
    ) {
        uiStateInternal.value = BackupRestoreState.Loading("Creating backup zip...")
        viewModelScope.launch(Dispatchers.IO) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                    ZipOutputStream(outputStream).use { zipOut ->
                        // 1. Backup Database
                        if (backupDb) {
                            val dbFile = context.getDatabasePath("media.db")
                            if (dbFile.exists()) {
                                addToZip(dbFile, "media.db", zipOut)
                            }
                            val dbWal = context.getDatabasePath("media.db-wal")
                            if (dbWal.exists()) {
                                addToZip(dbWal, "media.db-wal", zipOut)
                            }
                            val dbShm = context.getDatabasePath("media.db-shm")
                            if (dbShm.exists()) {
                                addToZip(dbShm, "media.db-shm", zipOut)
                            }
                        }

                        // 2. Backup Preferences
                        if (backupPrefs) {
                            val datastoreDir = File(context.filesDir, "datastore")
                            if (datastoreDir.exists()) {
                                datastoreDir.listFiles()?.forEach { file ->
                                    if (file.isFile && (file.name == "app_preferences.json" || file.name == "player_preferences.json")) {
                                        addToZip(file, "datastore/${file.name}", zipOut)
                                    }
                                }
                            }
                        }

                        // 3. Backup History/Search
                        if (backupHistory) {
                            val datastoreDir = File(context.filesDir, "datastore")
                            if (datastoreDir.exists()) {
                                val searchHistoryFile = File(datastoreDir, "search_history.json")
                                if (searchHistoryFile.exists()) {
                                    addToZip(searchHistoryFile, "datastore/search_history.json", zipOut)
                                }
                            }
                        }
                    }
                }
                withContext(Dispatchers.Main) {
                    uiStateInternal.value = BackupRestoreState.Success("Backup created successfully!")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    uiStateInternal.value = BackupRestoreState.Error("Backup failed: ${e.localizedMessage}")
                }
            }
        }
    }

    fun performRestore(
        context: Context,
        uri: Uri,
        restoreDb: Boolean,
        restorePrefs: Boolean,
        restoreHistory: Boolean
    ) {
        uiStateInternal.value = BackupRestoreState.Loading("Restoring data...")
        viewModelScope.launch(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    ZipInputStream(inputStream).use { zipIn ->
                        var entry = zipIn.getNextEntry()
                        while (entry != null) {
                            val name = entry.name
                            if (name == "media.db" || name == "media.db-wal" || name == "media.db-shm") {
                                if (restoreDb) {
                                    val targetFile = context.getDatabasePath(name)
                                    targetFile.parentFile?.mkdirs()
                                    FileOutputStream(targetFile).use { fos ->
                                        zipIn.copyTo(fos)
                                    }
                                }
                            } else if (name.startsWith("datastore/")) {
                                val fileName = name.substringAfter("datastore/")
                                if (restorePrefs && (fileName == "app_preferences.json" || fileName == "player_preferences.json")) {
                                    val targetFile = File(File(context.filesDir, "datastore"), fileName)
                                    targetFile.parentFile?.mkdirs()
                                    FileOutputStream(targetFile).use { fos ->
                                        zipIn.copyTo(fos)
                                    }
                                } else if (restoreHistory && fileName == "search_history.json") {
                                    val targetFile = File(File(context.filesDir, "datastore"), fileName)
                                    targetFile.parentFile?.mkdirs()
                                    FileOutputStream(targetFile).use { fos ->
                                        zipIn.copyTo(fos)
                                    }
                                }
                            }
                            zipIn.closeEntry()
                            entry = zipIn.getNextEntry()
                        }
                    }
                }
                withContext(Dispatchers.Main) {
                    uiStateInternal.value = BackupRestoreState.Success("Restore complete! Please restart the app to apply changes.")
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    uiStateInternal.value = BackupRestoreState.Error("Restore failed: ${e.localizedMessage}")
                }
            }
        }
    }

    private fun addToZip(file: File, zipPath: String, zipOut: ZipOutputStream) {
        FileInputStream(file).use { fis ->
            val entry = ZipEntry(zipPath)
            zipOut.putNextEntry(entry)
            fis.copyTo(zipOut)
            zipOut.closeEntry()
        }
    }

    fun resetState() {
        uiStateInternal.value = BackupRestoreState.Idle
    }
}

sealed interface BackupRestoreState {
    data object Idle : BackupRestoreState
    data class Loading(val message: String) : BackupRestoreState
    data class Success(val message: String) : BackupRestoreState
    data class Error(val error: String) : BackupRestoreState
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BackupAndRestoreScreen(
    onNavigateUp: () -> Unit,
    viewModel: BackupRestoreViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()

    var backupDb by remember { mutableStateOf(true) }
    var backupPrefs by remember { mutableStateOf(true) }
    var backupHistory by remember { mutableStateOf(true) }

    val createBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        if (uri != null) {
            viewModel.performBackup(context, uri, backupDb, backupPrefs, backupHistory)
        }
    }

    val restoreBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            viewModel.performRestore(context, uri, backupDb, backupPrefs, backupHistory)
        }
    }

    PremiumMeshBackground {
        Scaffold(
            topBar = {
                NextTopAppBar(
                    title = "Backup & Restore",
                    navigationIcon = {
                        FilledTonalIconButton(onClick = onNavigateUp) {
                            Icon(
                                imageVector = NextIcons.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                    ),
                )
            },
            containerColor = Color.Transparent,
        ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Backup,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(60.dp)
                    )
                    Column {
                        Text(
                            text = "Selective Cloud Backup",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Keep your preferences, media library database, and watch history safe by exporting a backup file.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Options Selection
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Selective Backup Categories",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { backupDb = !backupDb }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = backupDb, onCheckedChange = { backupDb = it })
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Library Database", style = MaterialTheme.typography.bodyLarge)
                            Text("All scanned videos, folders metadata, and layouts", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { backupPrefs = !backupPrefs }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = backupPrefs, onCheckedChange = { backupPrefs = it })
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("App Preferences", style = MaterialTheme.typography.bodyLarge)
                            Text("Theming settings, custom seeds, and slider configurations", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { backupHistory = !backupHistory }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = backupHistory, onCheckedChange = { backupHistory = it })
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Watch History & Search", style = MaterialTheme.typography.bodyLarge)
                            Text("Video playback states, durations watched, and search keywords", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                        val fileName = "NextPlayer_Backup_${sdf.format(Date())}.npbackup"
                        createBackupLauncher.launch(fileName)
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Backup, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Export")
                }

                Button(
                    onClick = {
                        restoreBackupLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*"))
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Restore, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Import")
                }
            }

            // Status Loading/Success Dialogs
            AnimatedVisibility(visible = uiState != BackupRestoreState.Idle) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        when (val state = uiState) {
                            is BackupRestoreState.Loading -> {
                                LoadingIndicator(modifier = Modifier.size(56.dp), color = MaterialTheme.colorScheme.primary)
                                Text(state.message, style = MaterialTheme.typography.bodyMedium)
                            }
                            is BackupRestoreState.Success -> {
                                Text("Success", style = MaterialTheme.typography.titleMedium, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
                                Text(state.message, style = MaterialTheme.typography.bodyMedium)
                                TextButton(onClick = { viewModel.resetState() }) {
                                    Text("Dismiss")
                                }
                            }
                            is BackupRestoreState.Error -> {
                                Text("Error", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
                                Text(state.error, style = MaterialTheme.typography.bodyMedium)
                                TextButton(onClick = { viewModel.resetState() }) {
                                    Text("Dismiss")
                                }
                            }
                            BackupRestoreState.Idle -> {}
                        }
                    }
                }
            }
        }
    }
    }
}
