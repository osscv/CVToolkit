package cv.toolkit.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.exifinterface.media.ExifInterface
import androidx.navigation.NavController
import cv.toolkit.ads.BannerAd
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

private data class ImageFileInfo(
    val uri: Uri,
    val name: String,
    val sizeBytes: Long,
    val exifTagCount: Int
)

private data class CleanedImage(
    val originalName: String,
    val originalSize: Long,
    val cleanedBytes: ByteArray,
    val cleanedSize: Long,
    val tagsRemoved: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExifRemoverScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedImages by remember { mutableStateOf<List<ImageFileInfo>>(emptyList()) }
    var cleanedImages by remember { mutableStateOf<List<CleanedImage>>(emptyList()) }
    var isProcessing by remember { mutableStateOf(false) }
    var processProgress by remember { mutableFloatStateOf(0f) }

    // For saving individual cleaned images
    var pendingSaveIndex by remember { mutableIntStateOf(-1) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                isProcessing = true
                processProgress = 0f
                try {
                    val infos = withContext(Dispatchers.IO) {
                        uris.mapIndexedNotNull { index, uri ->
                            try {
                                val info = getImageFileInfo(context, uri)
                                processProgress = (index + 1).toFloat() / uris.size * 0.5f
                                info
                            } catch (_: Exception) {
                                null
                            }
                        }
                    }
                    selectedImages = infos
                    cleanedImages = emptyList()
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Failed to load images: ${e.message}")
                } finally {
                    isProcessing = false
                }
            }
        }
    }

    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/jpeg")
    ) { uri ->
        uri?.let { destUri ->
            val idx = pendingSaveIndex
            if (idx in cleanedImages.indices) {
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            context.contentResolver.openOutputStream(destUri)?.use { os ->
                                os.write(cleanedImages[idx].cleanedBytes)
                            }
                        }
                        snackbarHostState.showSnackbar("Image saved successfully")
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar("Failed to save: ${e.message}")
                    }
                }
            }
        }
    }

    fun removeExif() {
        scope.launch {
            isProcessing = true
            processProgress = 0f
            try {
                val results = withContext(Dispatchers.IO) {
                    selectedImages.mapIndexed { index, info ->
                        val cleaned = stripExifFromImage(context, info)
                        processProgress = (index + 1).toFloat() / selectedImages.size
                        cleaned
                    }
                }
                cleanedImages = results
                snackbarHostState.showSnackbar("EXIF data removed from ${results.size} image(s)")
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Error: ${e.message}")
            } finally {
                isProcessing = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("EXIF Remover") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Pick images button
                item {
                    Button(
                        onClick = { imagePicker.launch("image/*") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isProcessing
                    ) {
                        Icon(Icons.Filled.PhotoLibrary, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Select Images")
                    }
                }

                // Empty state
                if (!isProcessing && selectedImages.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Filled.CleaningServices,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "Remove EXIF metadata from images",
                                    style = MaterialTheme.typography.bodyLarge,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Strip location, camera info, and other private metadata. Select one or more images to get started.",
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }

                // Processing indicator
                if (isProcessing) {
                    item {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            LinearProgressIndicator(
                                progress = { processProgress },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "${(processProgress * 100).toInt()}%",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Selected images list
                if (selectedImages.isNotEmpty() && cleanedImages.isEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${selectedImages.size} image(s) selected",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(onClick = {
                                selectedImages = emptyList()
                                cleanedImages = emptyList()
                            }) {
                                Text("Clear")
                            }
                        }
                    }

                    itemsIndexed(selectedImages) { _, info ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.Image,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        info.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        "${formatExifFileSize(info.sizeBytes)} • ${info.exifTagCount} EXIF tags",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Remove EXIF button
                    item {
                        Button(
                            onClick = { removeExif() },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isProcessing,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Filled.DeleteSweep, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Remove EXIF Data")
                        }
                    }
                }

                // Results
                if (cleanedImages.isNotEmpty()) {
                    item {
                        Text(
                            "Cleaned Images",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    itemsIndexed(cleanedImages) { index, cleaned ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Filled.CheckCircle,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            cleaned.originalName,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            "${cleaned.tagsRemoved} tags removed • ${formatExifFileSize(cleaned.originalSize)} → ${formatExifFileSize(cleaned.cleanedSize)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = {
                                        pendingSaveIndex = index
                                        val baseName = cleaned.originalName.substringBeforeLast(".")
                                        saveLauncher.launch("${baseName}_clean.jpg")
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Save Clean Image")
                                }
                            }
                        }
                    }

                    // Process more button
                    item {
                        OutlinedButton(
                            onClick = {
                                selectedImages = emptyList()
                                cleanedImages = emptyList()
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Process More Images")
                        }
                    }
                }
            }
            BannerAd(modifier = Modifier.fillMaxWidth())
        }
    }
}

private fun getImageFileInfo(context: android.content.Context, uri: Uri): ImageFileInfo {
    var name = "Unknown"
    var size = 0L
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (nameIndex >= 0) name = cursor.getString(nameIndex)
            if (sizeIndex >= 0) size = cursor.getLong(sizeIndex)
        }
    }

    var tagCount = 0
    try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val exif = ExifInterface(inputStream)
            val tagsToCheck = listOf(
                ExifInterface.TAG_MAKE, ExifInterface.TAG_MODEL, ExifInterface.TAG_SOFTWARE,
                ExifInterface.TAG_DATETIME, ExifInterface.TAG_DATETIME_ORIGINAL,
                ExifInterface.TAG_GPS_LATITUDE, ExifInterface.TAG_GPS_LONGITUDE,
                ExifInterface.TAG_EXPOSURE_TIME, ExifInterface.TAG_F_NUMBER,
                ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY, ExifInterface.TAG_FOCAL_LENGTH,
                ExifInterface.TAG_FLASH, ExifInterface.TAG_WHITE_BALANCE,
                ExifInterface.TAG_ORIENTATION, ExifInterface.TAG_IMAGE_WIDTH,
                ExifInterface.TAG_IMAGE_LENGTH, ExifInterface.TAG_ARTIST,
                ExifInterface.TAG_COPYRIGHT, ExifInterface.TAG_IMAGE_DESCRIPTION,
                ExifInterface.TAG_USER_COMMENT, ExifInterface.TAG_LENS_MAKE,
                ExifInterface.TAG_LENS_MODEL, ExifInterface.TAG_GPS_ALTITUDE,
                ExifInterface.TAG_METERING_MODE, ExifInterface.TAG_EXPOSURE_PROGRAM,
                ExifInterface.TAG_COLOR_SPACE, ExifInterface.TAG_X_RESOLUTION,
                ExifInterface.TAG_Y_RESOLUTION
            )
            tagCount = tagsToCheck.count { exif.getAttribute(it) != null }
        }
    } catch (_: Exception) { }

    return ImageFileInfo(uri, name, size, tagCount)
}

private fun stripExifFromImage(context: android.content.Context, info: ImageFileInfo): CleanedImage {
    val inputStream = context.contentResolver.openInputStream(info.uri)
        ?: throw Exception("Cannot open file")

    // Decode bitmap (this strips EXIF) and re-encode
    val bitmap = inputStream.use { BitmapFactory.decodeStream(it) }
        ?: throw Exception("Cannot decode image")

    val outputStream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 95, outputStream)
    val cleanedBytes = outputStream.toByteArray()

    return CleanedImage(
        originalName = info.name,
        originalSize = info.sizeBytes,
        cleanedBytes = cleanedBytes,
        cleanedSize = cleanedBytes.size.toLong(),
        tagsRemoved = info.exifTagCount
    )
}

private fun formatExifFileSize(bytes: Long): String {
    return when {
        bytes >= 1_048_576 -> String.format("%.2f MB", bytes / 1_048_576.0)
        bytes >= 1024 -> String.format("%.1f KB", bytes / 1024.0)
        else -> "$bytes B"
    }
}
