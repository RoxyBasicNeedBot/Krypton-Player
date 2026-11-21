package dev.roxy.kryptonplayer.core.media.services

import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import dagger.hilt.android.qualifiers.ApplicationContext
import dev.roxy.kryptonplayer.core.common.extensions.deleteMedia
import dev.roxy.kryptonplayer.core.common.extensions.getPath
import dev.roxy.kryptonplayer.core.common.extensions.updateMedia
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

@Singleton
class LocalMediaService @Inject constructor(
    @ApplicationContext private val context: Context,
) : MediaService {

    private lateinit var activity: Activity
    private val contentResolver = context.contentResolver
    private var resultOkCallback: () -> Unit = {}
    private var resultCancelledCallback: () -> Unit = {}
    private var mediaRequestLauncher: ActivityResultLauncher<IntentSenderRequest>? = null

    override fun initialize(activity: ComponentActivity) {
        this.activity = activity
        mediaRequestLauncher = activity.registerForActivityResult(
            ActivityResultContracts.StartIntentSenderForResult(),
        ) { result ->
            when (result.resultCode) {
                Activity.RESULT_OK -> resultOkCallback()
                Activity.RESULT_CANCELED -> resultCancelledCallback()
            }
        }
    }

    override suspend fun deleteMedia(uris: List<Uri>): Boolean = withContext(Dispatchers.IO) {
        return@withContext if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            deleteMediaR(uris)
        } else {
            deleteMediaBelowR(uris)
        }
    }

    override suspend fun renameMedia(uri: Uri, to: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            renameMediaR(uri, to)
        } else {
            renameMediaBelowR(uri, to)
        }
    }

    override suspend fun shareMedia(uris: List<Uri>) {
        val shareIntent = Intent().apply {
            type = "video/*"
            action = Intent.ACTION_SEND_MULTIPLE
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(uris))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val intent = Intent.createChooser(
            shareIntent,
            null,
        ).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        activity.startActivity(intent)
    }

    private suspend fun requestRecoverablePermission(intentSender: IntentSender): Boolean =
        suspendCancellableCoroutine { continuation ->
            val launcher = mediaRequestLauncher
            if (launcher == null) {
                continuation.resume(false)
                return@suspendCancellableCoroutine
            }
            resultOkCallback = { continuation.resume(true) }
            resultCancelledCallback = { continuation.resume(false) }
            try {
                launcher.launch(IntentSenderRequest.Builder(intentSender).build())
            } catch (e: Exception) {
                continuation.resume(false)
            }
        }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun launchWriteRequest(
        uris: List<Uri>,
        onResultCanceled: () -> Unit = {},
        onResultOk: () -> Unit = {},
    ) {
        if (uris.isEmpty()) {
            onResultOk()
            return
        }
        val mediaStoreUris = uris.filter { it.authority == MediaStore.AUTHORITY }
        if (mediaStoreUris.isEmpty()) {
            onResultOk()
            return
        }
        val launcher = mediaRequestLauncher
        if (launcher == null) {
            onResultCanceled()
            return
        }
        resultOkCallback = onResultOk
        resultCancelledCallback = onResultCanceled
        try {
            val intent = MediaStore.createWriteRequest(contentResolver, mediaStoreUris)
            launcher.launch(IntentSenderRequest.Builder(intent).build())
        } catch (e: Exception) {
            onResultCanceled()
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun launchDeleteRequest(
        uris: List<Uri>,
        onResultCanceled: () -> Unit = {},
        onResultOk: () -> Unit = {},
    ) {
        if (uris.isEmpty()) {
            onResultOk()
            return
        }
        val mediaStoreUris = uris.filter { it.authority == MediaStore.AUTHORITY }
        if (mediaStoreUris.isEmpty()) {
            onResultOk()
            return
        }
        val launcher = mediaRequestLauncher
        if (launcher == null) {
            onResultCanceled()
            return
        }
        resultOkCallback = onResultOk
        resultCancelledCallback = onResultCanceled
        try {
            val intent = MediaStore.createDeleteRequest(contentResolver, mediaStoreUris)
            launcher.launch(IntentSenderRequest.Builder(intent).build())
        } catch (e: Exception) {
            onResultCanceled()
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private suspend fun deleteMediaR(uris: List<Uri>): Boolean {
        if (uris.isEmpty()) return true
        val nonMediaStoreUris = uris.filter { it.authority != MediaStore.AUTHORITY }
        nonMediaStoreUris.forEach { uri ->
            try {
                if (contentResolver.delete(uri, null, null) <= 0) {
                    context.getPath(uri)?.let { File(it).delete() }
                }
            } catch (_: Exception) {}
        }
        val mediaStoreUris = uris.filter { it.authority == MediaStore.AUTHORITY }
        if (mediaStoreUris.isEmpty()) return true
        return suspendCancellableCoroutine { continuation ->
            launchDeleteRequest(
                uris = mediaStoreUris,
                onResultOk = { continuation.resume(true) },
                onResultCanceled = { continuation.resume(false) },
            )
        }
    }

    private suspend fun deleteMediaBelowR(uris: List<Uri>): Boolean {
        if (uris.isEmpty()) return true
        for (uri in uris) {
            val success = deleteSingleMediaBelowR(uri)
            if (!success) return false
        }
        return true
    }

    private suspend fun deleteSingleMediaBelowR(uri: Uri): Boolean {
        return try {
            val count = contentResolver.delete(uri, null, null)
            if (count > 0) {
                true
            } else {
                context.getPath(uri)?.let { File(it).delete() } ?: false
            }
        } catch (e: SecurityException) {
            if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q && e is RecoverableSecurityException) {
                val granted = requestRecoverablePermission(e.userAction.actionIntent.intentSender)
                if (granted) {
                    try {
                        contentResolver.delete(uri, null, null) > 0
                    } catch (e2: Exception) {
                        false
                    }
                } else {
                    false
                }
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private suspend fun renameMediaR(uri: Uri, to: String): Boolean {
        if (uri.authority != MediaStore.AUTHORITY) {
            return runCatching {
                val oldFile = context.getPath(uri)?.let { File(it) } ?: return@runCatching false
                val newFile = File(oldFile.parentFile, to)
                oldFile.renameTo(newFile)
            }.getOrDefault(false)
        }
        return suspendCancellableCoroutine { continuation ->
            val scope = CoroutineScope(Dispatchers.Default)
            launchWriteRequest(
                uris = listOf(uri),
                onResultOk = {
                    scope.launch {
                        val result = contentResolver.updateMedia(
                            uri = uri,
                            contentValues = ContentValues().apply {
                                put(MediaStore.MediaColumns.DISPLAY_NAME, to)
                            },
                        )
                        continuation.resume(result)
                    }
                },
                onResultCanceled = { continuation.resume(false) },
            )
            continuation.invokeOnCancellation { scope.cancel() }
        }
    }

    private suspend fun renameMediaBelowR(uri: Uri, to: String): Boolean {
        if (Build.VERSION.SDK_INT == Build.VERSION_CODES.Q) {
            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, to)
                put(MediaStore.MediaColumns.TITLE, to)
            }
            return try {
                val updated = contentResolver.update(uri, contentValues, null, null) > 0
                if (!updated) {
                    val oldFile = context.getPath(uri)?.let { File(it) }
                    if (oldFile != null && oldFile.exists()) {
                        val newFile = File(oldFile.parentFile, to)
                        oldFile.renameTo(newFile)
                    } else false
                } else true
            } catch (e: SecurityException) {
                if (e is RecoverableSecurityException) {
                    val granted = requestRecoverablePermission(e.userAction.actionIntent.intentSender)
                    if (granted) {
                        try {
                            contentResolver.update(uri, contentValues, null, null) > 0
                        } catch (e2: Exception) {
                            false
                        }
                    } else false
                } else false
            } catch (e: Exception) {
                false
            }
        } else {
            return runCatching {
                val oldFile = context.getPath(uri)?.let { File(it) } ?: throw Error()
                val newFile = File(oldFile.parentFile, to)
                oldFile.renameTo(newFile).also { success ->
                    if (success) {
                        contentResolver.updateMedia(
                            uri = uri,
                            contentValues = ContentValues().apply {
                                put(MediaStore.Files.FileColumns.DISPLAY_NAME, to)
                                put(MediaStore.Files.FileColumns.TITLE, to)
                                put(MediaStore.Files.FileColumns.DATA, newFile.path)
                            },
                        )
                    }
                }
            }.getOrNull() ?: false
        }
    }
}
