package cv.toolkit.screens

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import android.webkit.MimeTypeMap
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.InsertDriveFile
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cv.toolkit.ads.BannerAd
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.*

data class FileInfo(
    val name: String,
    val size: Long,
    val mimeType: String?,
    val lastModified: Long?,
    val uri: Uri
)

data class FileHashes(
    val md5: String,
    val sha1: String,
    val sha256: String,
    val sha512: String,
    val crc32: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileHashScreen(navController: NavController) {
    var selectedFile by remember { mutableStateOf<FileInfo?>(null) }
    var fileHashes by remember { mutableStateOf<FileHashes?>(null) }
    var isCalculating by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var progress by remember { mutableFloatStateOf(0f) }
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    val filePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            errorMessage = null
            fileHashes = null
            progress = 0f

            // Get file info
            val fileInfo = getFileInfo(context.contentResolver, it)
            selectedFile = fileInfo

            // Calculate hashes
            isCalculating = true
            scope.launch {
                try {
                    val hashes = withContext(Dispatchers.IO) {
                        calculateFileHashes(context.contentResolver, it) { p ->
                            progress = p
                        }
                    }
                    fileHashes = hashes
                } catch (e: Exception) {
                    errorMessage = "Failed to calculate hashes: ${e.message}"
                } finally {
                    isCalculating = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("File Hash & Info") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Select file button
            Button(
                onClick = { filePicker.launch(arrayOf("*/*")) },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isCalculating
            ) {
                Icon(Icons.Filled.FolderOpen, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("Select File")
            }

            // Progress indicator
            if (isCalculating) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Calculating hashes...", style = MaterialTheme.typography.bodyMedium)
                        Spacer(Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "${(progress * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Error message
            errorMessage?.let {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Error,
                            null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // File info
            selectedFile?.let { file ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                getFileIcon(file.mimeType),
                                null,
                                modifier = Modifier.size(32.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    file.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    file.mimeType ?: "Unknown type",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }

                // File metadata
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "File Metadata",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(12.dp))

                        MetadataRow("File Name", file.name)
                        MetadataRow("Size", formatFileSize(file.size))
                        MetadataRow("Size (bytes)", file.size.toString())
                        MetadataRow("MIME Type", file.mimeType ?: "Unknown")
                        MetadataRow("Extension", getFileExtension(file.name))
                        file.lastModified?.let {
                            MetadataRow("Last Modified", formatDate(it))
                        }
                    }
                }
            }

            // Hash values
            fileHashes?.let { hashes ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Hash Values",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(12.dp))

                        HashRow("MD5", hashes.md5, clipboard, scope)
                        HashRow("SHA-1", hashes.sha1, clipboard, scope)
                        HashRow("SHA-256", hashes.sha256, clipboard, scope)
                        HashRow("SHA-512", hashes.sha512, clipboard, scope)
                        HashRow("CRC32", hashes.crc32, clipboard, scope)
                    }
                }

                // Copy all button
                OutlinedButton(
                    onClick = {
                        val allHashes = """
                            |File: ${selectedFile?.name}
                            |Size: ${selectedFile?.size} bytes
                            |
                            |MD5: ${hashes.md5}
                            |SHA-1: ${hashes.sha1}
                            |SHA-256: ${hashes.sha256}
                            |SHA-512: ${hashes.sha512}
                            |CRC32: ${hashes.crc32}
                        """.trimMargin()
                        scope.launch {
                            clipboard.setClipEntry(
                                androidx.compose.ui.platform.ClipEntry(
                                    android.content.ClipData.newPlainText("hashes", allHashes)
                                )
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Filled.ContentCopy, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Copy All Hashes")
                }
            }

            // Empty state
            if (selectedFile == null && !isCalculating) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.InsertDriveFile,
                            null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Select a File",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Calculate MD5, SHA-1, SHA-256, SHA-512 hashes\nand view file metadata",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            // Info card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Info,
                        null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Hash values are unique fingerprints used to verify file integrity and detect modifications.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            BannerAd(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun MetadataRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun HashRow(
    algorithm: String,
    hash: String,
    clipboard: androidx.compose.ui.platform.Clipboard,
    scope: kotlinx.coroutines.CoroutineScope
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    algorithm,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(
                    onClick = {
                        scope.launch {
                            clipboard.setClipEntry(
                                androidx.compose.ui.platform.ClipEntry(
                                    android.content.ClipData.newPlainText(algorithm, hash)
                                )
                            )
                        }
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Filled.ContentCopy, "Copy", modifier = Modifier.size(18.dp))
                }
            }
            Text(
                hash,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

private fun getFileInfo(contentResolver: ContentResolver, uri: Uri): FileInfo {
    var name = "Unknown"
    var size = 0L
    var lastModified: Long? = null

    contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)

            if (nameIndex >= 0) {
                name = cursor.getString(nameIndex) ?: "Unknown"
            }
            if (sizeIndex >= 0) {
                size = cursor.getLong(sizeIndex)
            }
        }
    }

    val mimeType = contentResolver.getType(uri)

    return FileInfo(
        name = name,
        size = size,
        mimeType = mimeType,
        lastModified = lastModified,
        uri = uri
    )
}

private fun calculateFileHashes(
    contentResolver: ContentResolver,
    uri: Uri,
    onProgress: (Float) -> Unit
): FileHashes {
    val md5Digest = MessageDigest.getInstance("MD5")
    val sha1Digest = MessageDigest.getInstance("SHA-1")
    val sha256Digest = MessageDigest.getInstance("SHA-256")
    val sha512Digest = MessageDigest.getInstance("SHA-512")
    val crc32 = java.util.zip.CRC32()

    var totalBytesRead = 0L
    var fileSize = 0L

    // Get file size first
    contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (sizeIndex >= 0) {
                fileSize = cursor.getLong(sizeIndex)
            }
        }
    }

    contentResolver.openInputStream(uri)?.use { inputStream ->
        val buffer = ByteArray(8192)
        var bytesRead: Int

        while (inputStream.read(buffer).also { bytesRead = it } != -1) {
            md5Digest.update(buffer, 0, bytesRead)
            sha1Digest.update(buffer, 0, bytesRead)
            sha256Digest.update(buffer, 0, bytesRead)
            sha512Digest.update(buffer, 0, bytesRead)
            crc32.update(buffer, 0, bytesRead)

            totalBytesRead += bytesRead
            if (fileSize > 0) {
                onProgress(totalBytesRead.toFloat() / fileSize)
            }
        }
    }

    return FileHashes(
        md5 = md5Digest.digest().toHexString(),
        sha1 = sha1Digest.digest().toHexString(),
        sha256 = sha256Digest.digest().toHexString(),
        sha512 = sha512Digest.digest().toHexString(),
        crc32 = String.format("%08X", crc32.value)
    )
}

private fun ByteArray.toHexString(): String {
    return joinToString("") { "%02x".format(it) }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format("%.2f KB", bytes / 1024.0)
        bytes < 1024 * 1024 * 1024 -> String.format("%.2f MB", bytes / (1024.0 * 1024))
        else -> String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024))
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun getFileExtension(filename: String): String {
    val lastDot = filename.lastIndexOf('.')
    return if (lastDot > 0) filename.substring(lastDot + 1).uppercase() else "Unknown"
}

private fun getFileIcon(mimeType: String?): androidx.compose.ui.graphics.vector.ImageVector {
    return when {
        mimeType == null -> Icons.AutoMirrored.Filled.InsertDriveFile
        mimeType.startsWith("image/") -> Icons.Filled.Image
        mimeType.startsWith("video/") -> Icons.Filled.VideoFile
        mimeType.startsWith("audio/") -> Icons.Filled.AudioFile
        mimeType.startsWith("text/") -> Icons.Filled.Description
        mimeType.contains("pdf") -> Icons.Filled.PictureAsPdf
        mimeType.contains("zip") || mimeType.contains("rar") || mimeType.contains("tar") -> Icons.Filled.FolderZip
        mimeType.contains("application") -> Icons.Filled.Apps
        else -> Icons.AutoMirrored.Filled.InsertDriveFile
    }
}
