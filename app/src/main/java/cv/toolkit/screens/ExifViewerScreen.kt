package cv.toolkit.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.exifinterface.media.ExifInterface
import androidx.navigation.NavController
import cv.toolkit.ads.BannerAd
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class ExifCategory(
    val name: String,
    val icon: @Composable () -> Unit,
    val entries: List<Pair<String, String>>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExifViewerScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    var fileName by remember { mutableStateOf<String?>(null) }
    var fileSize by remember { mutableStateOf<Long?>(null) }
    var categories by remember { mutableStateOf<List<ExifCategory>>(emptyList()) }
    var isProcessing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                isProcessing = true
                errorMessage = null
                try {
                    val result = withContext(Dispatchers.IO) {
                        extractExifData(context, it)
                    }
                    fileName = result.first
                    fileSize = result.second
                    categories = result.third
                } catch (e: Exception) {
                    errorMessage = "Failed to read EXIF: ${e.message}"
                    categories = emptyList()
                } finally {
                    isProcessing = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("EXIF Viewer") },
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
                // Pick image button
                item {
                    Button(
                        onClick = { imagePicker.launch("image/*") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isProcessing
                    ) {
                        Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Select Image")
                    }
                }

                // Loading
                if (isProcessing) {
                    item {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }
                }

                // Error
                if (errorMessage != null) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                errorMessage!!,
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }

                // Empty state
                if (!isProcessing && errorMessage == null && categories.isEmpty()) {
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
                                    Icons.Filled.ImageSearch,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "Select an image to view its EXIF metadata",
                                    style = MaterialTheme.typography.bodyLarge,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Supports JPEG, PNG, WebP, HEIF and other image formats",
                                    style = MaterialTheme.typography.bodySmall,
                                    textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }

                // File info
                if (fileName != null && categories.isNotEmpty()) {
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.InsertDriveFile,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        fileName ?: "Unknown",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    fileSize?.let {
                                        Text(
                                            formatFileSize(it),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                IconButton(onClick = {
                                    val allText = categories.joinToString("\n\n") { cat ->
                                        "── ${cat.name} ──\n" + cat.entries.joinToString("\n") { "${it.first}: ${it.second}" }
                                    }
                                    clipboardManager.setPrimaryClip(ClipData.newPlainText("exif", allText))
                                    scope.launch { snackbarHostState.showSnackbar("EXIF data copied") }
                                }) {
                                    Icon(Icons.Filled.ContentCopy, contentDescription = "Copy all")
                                }
                            }
                        }
                    }
                }

                // EXIF categories
                categories.forEach { category ->
                    item {
                        Text(
                            category.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                    items(category.entries) { (key, value) ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    key,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.weight(0.4f)
                                )
                                Text(
                                    value,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(0.6f),
                                    textAlign = TextAlign.End
                                )
                            }
                        }
                    }
                }
            }
            BannerAd(modifier = Modifier.fillMaxWidth())
        }
    }
}

private fun extractExifData(
    context: android.content.Context,
    uri: Uri
): Triple<String?, Long?, List<ExifCategory>> {
    // Get file name and size
    var name: String? = null
    var size: Long? = null
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (nameIndex >= 0) name = cursor.getString(nameIndex)
            if (sizeIndex >= 0) size = cursor.getLong(sizeIndex)
        }
    }

    val inputStream = context.contentResolver.openInputStream(uri)
        ?: throw Exception("Cannot open file")

    val exif = inputStream.use { ExifInterface(it) }

    val categories = mutableListOf<ExifCategory>()

    // Camera Information
    val cameraEntries = mutableListOf<Pair<String, String>>()
    exif.getAttribute(ExifInterface.TAG_MAKE)?.let { cameraEntries.add("Make" to it) }
    exif.getAttribute(ExifInterface.TAG_MODEL)?.let { cameraEntries.add("Model" to it) }
    exif.getAttribute(ExifInterface.TAG_LENS_MAKE)?.let { cameraEntries.add("Lens Make" to it) }
    exif.getAttribute(ExifInterface.TAG_LENS_MODEL)?.let { cameraEntries.add("Lens Model" to it) }
    exif.getAttribute(ExifInterface.TAG_SOFTWARE)?.let { cameraEntries.add("Software" to it) }
    if (cameraEntries.isNotEmpty()) {
        categories.add(ExifCategory("Camera", { Icon(Icons.Filled.CameraAlt, null) }, cameraEntries))
    }

    // Image Details
    val imageEntries = mutableListOf<Pair<String, String>>()
    val width = exif.getAttributeInt(ExifInterface.TAG_IMAGE_WIDTH, 0)
    val height = exif.getAttributeInt(ExifInterface.TAG_IMAGE_LENGTH, 0)
    if (width > 0 && height > 0) imageEntries.add("Dimensions" to "${width} x ${height}")
    exif.getAttribute(ExifInterface.TAG_X_RESOLUTION)?.let { imageEntries.add("X Resolution" to "$it dpi") }
    exif.getAttribute(ExifInterface.TAG_Y_RESOLUTION)?.let { imageEntries.add("Y Resolution" to "$it dpi") }
    exif.getAttribute(ExifInterface.TAG_COLOR_SPACE)?.let {
        val colorSpace = when (it) {
            "1" -> "sRGB"
            "65535" -> "Uncalibrated"
            else -> it
        }
        imageEntries.add("Color Space" to colorSpace)
    }
    exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, -1).let {
        if (it > 0) {
            val orientation = when (it) {
                ExifInterface.ORIENTATION_NORMAL -> "Normal"
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> "Flipped Horizontal"
                ExifInterface.ORIENTATION_ROTATE_180 -> "Rotated 180°"
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> "Flipped Vertical"
                ExifInterface.ORIENTATION_TRANSPOSE -> "Transposed"
                ExifInterface.ORIENTATION_ROTATE_90 -> "Rotated 90°"
                ExifInterface.ORIENTATION_TRANSVERSE -> "Transverse"
                ExifInterface.ORIENTATION_ROTATE_270 -> "Rotated 270°"
                else -> "Unknown ($it)"
            }
            imageEntries.add("Orientation" to orientation)
        }
    }
    if (imageEntries.isNotEmpty()) {
        categories.add(ExifCategory("Image Details", { Icon(Icons.Filled.Photo, null) }, imageEntries))
    }

    // Shooting Settings
    val shootingEntries = mutableListOf<Pair<String, String>>()
    exif.getAttribute(ExifInterface.TAG_EXPOSURE_TIME)?.let {
        val time = it.toDoubleOrNull()
        val display = if (time != null && time < 1) "1/${(1.0 / time).toInt()}s" else "${it}s"
        shootingEntries.add("Exposure Time" to display)
    }
    exif.getAttribute(ExifInterface.TAG_F_NUMBER)?.let { shootingEntries.add("Aperture" to "f/$it") }
    exif.getAttribute(ExifInterface.TAG_PHOTOGRAPHIC_SENSITIVITY)?.let { shootingEntries.add("ISO" to it) }
    exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH)?.let {
        val parts = it.split("/")
        val focal = if (parts.size == 2) {
            val num = parts[0].toDoubleOrNull() ?: 0.0
            val den = parts[1].toDoubleOrNull() ?: 1.0
            if (den > 0) "${(num / den).toInt()}mm" else it
        } else "${it}mm"
        shootingEntries.add("Focal Length" to focal)
    }
    exif.getAttribute(ExifInterface.TAG_FOCAL_LENGTH_IN_35MM_FILM)?.let {
        shootingEntries.add("35mm Equivalent" to "${it}mm")
    }
    exif.getAttribute(ExifInterface.TAG_FLASH)?.let {
        val flash = when (it.toIntOrNull()) {
            0 -> "No Flash"
            1 -> "Flash Fired"
            5 -> "Flash Fired, Strobe Return Not Detected"
            7 -> "Flash Fired, Strobe Return Detected"
            16 -> "Flash Did Not Fire (Compulsory)"
            24 -> "Flash Did Not Fire (Auto)"
            25 -> "Flash Fired (Auto)"
            else -> it
        }
        shootingEntries.add("Flash" to flash)
    }
    exif.getAttribute(ExifInterface.TAG_WHITE_BALANCE)?.let {
        shootingEntries.add("White Balance" to if (it == "0") "Auto" else "Manual")
    }
    exif.getAttribute(ExifInterface.TAG_METERING_MODE)?.let {
        val mode = when (it.toIntOrNull()) {
            0 -> "Unknown"
            1 -> "Average"
            2 -> "Center-Weighted"
            3 -> "Spot"
            4 -> "Multi-Spot"
            5 -> "Pattern"
            6 -> "Partial"
            else -> it
        }
        shootingEntries.add("Metering Mode" to mode)
    }
    exif.getAttribute(ExifInterface.TAG_EXPOSURE_PROGRAM)?.let {
        val program = when (it.toIntOrNull()) {
            0 -> "Not Defined"
            1 -> "Manual"
            2 -> "Normal Program"
            3 -> "Aperture Priority"
            4 -> "Shutter Priority"
            5 -> "Creative"
            6 -> "Action"
            7 -> "Portrait"
            8 -> "Landscape"
            else -> it
        }
        shootingEntries.add("Exposure Program" to program)
    }
    if (shootingEntries.isNotEmpty()) {
        categories.add(ExifCategory("Shooting Settings", { Icon(Icons.Filled.Tune, null) }, shootingEntries))
    }

    // Date & Time
    val dateEntries = mutableListOf<Pair<String, String>>()
    exif.getAttribute(ExifInterface.TAG_DATETIME)?.let { dateEntries.add("Date/Time" to it) }
    exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)?.let { dateEntries.add("Original Date" to it) }
    exif.getAttribute(ExifInterface.TAG_DATETIME_DIGITIZED)?.let { dateEntries.add("Digitized Date" to it) }
    if (dateEntries.isNotEmpty()) {
        categories.add(ExifCategory("Date & Time", { Icon(Icons.Filled.CalendarMonth, null) }, dateEntries))
    }

    // GPS Location
    val gpsEntries = mutableListOf<Pair<String, String>>()
    val latLong = exif.latLong
    if (latLong != null) {
        gpsEntries.add("Latitude" to String.format("%.6f°", latLong[0]))
        gpsEntries.add("Longitude" to String.format("%.6f°", latLong[1]))
    }
    exif.getAttribute(ExifInterface.TAG_GPS_ALTITUDE)?.let {
        val parts = it.split("/")
        val alt = if (parts.size == 2) {
            val num = parts[0].toDoubleOrNull() ?: 0.0
            val den = parts[1].toDoubleOrNull() ?: 1.0
            if (den > 0) String.format("%.1fm", num / den) else it
        } else "${it}m"
        gpsEntries.add("Altitude" to alt)
    }
    exif.getAttribute(ExifInterface.TAG_GPS_DATESTAMP)?.let { gpsEntries.add("GPS Date" to it) }
    exif.getAttribute(ExifInterface.TAG_GPS_TIMESTAMP)?.let { gpsEntries.add("GPS Time" to it) }
    if (gpsEntries.isNotEmpty()) {
        categories.add(ExifCategory("GPS Location", { Icon(Icons.Filled.LocationOn, null) }, gpsEntries))
    }

    // Copyright & Description
    val copyrightEntries = mutableListOf<Pair<String, String>>()
    exif.getAttribute(ExifInterface.TAG_ARTIST)?.let { copyrightEntries.add("Artist" to it) }
    exif.getAttribute(ExifInterface.TAG_COPYRIGHT)?.let { copyrightEntries.add("Copyright" to it) }
    exif.getAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION)?.let { copyrightEntries.add("Description" to it) }
    exif.getAttribute(ExifInterface.TAG_USER_COMMENT)?.let { copyrightEntries.add("User Comment" to it) }
    if (copyrightEntries.isNotEmpty()) {
        categories.add(ExifCategory("Copyright & Info", { Icon(Icons.Filled.Copyright, null) }, copyrightEntries))
    }

    // If no EXIF data found at all
    if (categories.isEmpty()) {
        categories.add(
            ExifCategory(
                "No EXIF Data",
                { Icon(Icons.Filled.Info, null) },
                listOf("Status" to "No EXIF metadata found in this image")
            )
        )
    }

    return Triple(name, size, categories)
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes >= 1_048_576 -> String.format("%.2f MB", bytes / 1_048_576.0)
        bytes >= 1024 -> String.format("%.1f KB", bytes / 1024.0)
        else -> "$bytes B"
    }
}
