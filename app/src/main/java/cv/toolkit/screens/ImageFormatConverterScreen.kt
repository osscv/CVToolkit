package cv.toolkit.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cv.toolkit.ads.BannerAd
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

private enum class OutputImageFormat(
    val label: String,
    val extension: String,
    val mimeType: String,
    val minSdk: Int = 0
) {
    JPEG("JPEG", "jpg", "image/jpeg"),
    PNG("PNG", "png", "image/png"),
    WEBP_LOSSY("WebP (Lossy)", "webp", "image/webp"),
    WEBP_LOSSLESS("WebP (Lossless)", "webp", "image/webp", 30),
    BMP("BMP", "bmp", "image/bmp")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageFormatConverterScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var originalName by remember { mutableStateOf("") }
    var originalSize by remember { mutableLongStateOf(0L) }
    var originalFormat by remember { mutableStateOf("") }
    var selectedFormat by remember { mutableStateOf(OutputImageFormat.PNG) }
    var quality by remember { mutableIntStateOf(90) }
    var convertedBytes by remember { mutableStateOf<ByteArray?>(null) }
    var convertedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    val availableFormats = OutputImageFormat.entries.filter { it.minSdk <= Build.VERSION.SDK_INT }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                isProcessing = true
                try {
                    val result = withContext(Dispatchers.IO) { loadForConversion(context, it) }
                    originalBitmap = result.bitmap
                    originalName = result.name
                    originalSize = result.size
                    originalFormat = result.format
                    convertedBytes = null
                    convertedBitmap = null
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Failed to load: ${e.message}")
                } finally {
                    isProcessing = false
                }
            }
        }
    }

    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(selectedFormat.mimeType)
    ) { uri ->
        uri?.let { destUri ->
            val bytes = convertedBytes
            if (bytes != null) {
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            context.contentResolver.openOutputStream(destUri)?.use { it.write(bytes) }
                        }
                        snackbarHostState.showSnackbar("Image saved as ${selectedFormat.label}")
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar("Failed to save: ${e.message}")
                    }
                }
            }
        }
    }

    fun convert() {
        val bitmap = originalBitmap ?: return
        scope.launch {
            isProcessing = true
            try {
                val result = withContext(Dispatchers.IO) {
                    convertImage(bitmap, selectedFormat, quality)
                }
                convertedBytes = result.first
                convertedBitmap = result.second
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Conversion failed: ${e.message}")
            } finally {
                isProcessing = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Image Converter") },
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
            modifier = Modifier.fillMaxSize().padding(paddingValues)
        ) {
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(onClick = { imagePicker.launch("image/*") }, modifier = Modifier.fillMaxWidth(), enabled = !isProcessing) {
                    Icon(Icons.Filled.Image, null, Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select Image")
                }

                if (originalBitmap == null && !isProcessing) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Filled.SwapHoriz, null, Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Convert images between formats", style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Supports JPEG, PNG, WebP, and BMP",
                                style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                        }
                    }
                }

                originalBitmap?.let { bitmap ->
                    // Source info
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Image(bitmap = bitmap.asImageBitmap(), contentDescription = null,
                                modifier = Modifier.size(60.dp), contentScale = ContentScale.Fit)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(originalName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text("${bitmap.width} x ${bitmap.height} \u2022 ${formatConvSize(originalSize)}",
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Format: $originalFormat", style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    // Output format selector
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Output Format", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(12.dp))
                            availableFormats.forEach { format ->
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = selectedFormat == format,
                                        onClick = {
                                            selectedFormat = format
                                            convertedBytes = null
                                            convertedBitmap = null
                                        },
                                        enabled = !isProcessing
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Column {
                                        Text(format.label, style = MaterialTheme.typography.bodyMedium)
                                        Text(".${format.extension}", style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }

                    // Quality slider (for lossy formats)
                    if (selectedFormat == OutputImageFormat.JPEG || selectedFormat == OutputImageFormat.WEBP_LOSSY) {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Quality: $quality%", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Slider(
                                    value = quality.toFloat(),
                                    onValueChange = { quality = it.toInt(); convertedBytes = null; convertedBitmap = null },
                                    valueRange = 10f..100f,
                                    steps = 17,
                                    enabled = !isProcessing,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    // Result
                    if (convertedBytes != null) {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Conversion Result", style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(formatConvSize(originalSize), style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                        Text(originalFormat, style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                                    }
                                    Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(24.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f))
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(formatConvSize(convertedBytes!!.size.toLong()), style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                        Text(selectedFormat.label, style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                                    }
                                }
                            }
                        }
                    }

                    if (isProcessing) {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    }

                    Button(onClick = { convert() }, modifier = Modifier.fillMaxWidth(), enabled = !isProcessing) {
                        Icon(Icons.Filled.SwapHoriz, null, Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Convert to ${selectedFormat.label}")
                    }

                    if (convertedBytes != null) {
                        OutlinedButton(
                            onClick = {
                                val baseName = originalName.substringBeforeLast(".")
                                saveLauncher.launch("$baseName.${selectedFormat.extension}")
                            },
                            modifier = Modifier.fillMaxWidth(), enabled = !isProcessing
                        ) {
                            Icon(Icons.Filled.Save, null, Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save Converted Image")
                        }
                    }
                }
            }
            BannerAd(modifier = Modifier.fillMaxWidth())
        }
    }
}

private data class ConversionInput(val bitmap: Bitmap, val name: String, val size: Long, val format: String)

private fun loadForConversion(context: Context, uri: Uri): ConversionInput {
    var name = "image"
    var size = 0L
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val ni = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val si = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (ni >= 0) name = cursor.getString(ni) ?: "image"
            if (si >= 0) size = cursor.getLong(si)
        }
    }
    val mimeType = context.contentResolver.getType(uri) ?: "unknown"
    val format = when {
        mimeType.contains("jpeg") || mimeType.contains("jpg") -> "JPEG"
        mimeType.contains("png") -> "PNG"
        mimeType.contains("webp") -> "WebP"
        mimeType.contains("bmp") -> "BMP"
        mimeType.contains("heif") || mimeType.contains("heic") -> "HEIF"
        mimeType.contains("gif") -> "GIF"
        else -> name.substringAfterLast(".", "Unknown").uppercase()
    }
    val inputStream = context.contentResolver.openInputStream(uri) ?: throw Exception("Cannot open file")
    val bitmap = inputStream.use { BitmapFactory.decodeStream(it) } ?: throw Exception("Cannot decode image")
    return ConversionInput(bitmap, name, size, format)
}

@Suppress("DEPRECATION")
private fun convertImage(bitmap: Bitmap, format: OutputImageFormat, quality: Int): Pair<ByteArray, Bitmap?> {
    val baos = ByteArrayOutputStream()
    when (format) {
        OutputImageFormat.JPEG -> bitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos)
        OutputImageFormat.PNG -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
        OutputImageFormat.WEBP_LOSSY -> {
            if (Build.VERSION.SDK_INT >= 30) {
                bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, quality, baos)
            } else {
                bitmap.compress(Bitmap.CompressFormat.WEBP, quality, baos)
            }
        }
        OutputImageFormat.WEBP_LOSSLESS -> {
            if (Build.VERSION.SDK_INT >= 30) {
                bitmap.compress(Bitmap.CompressFormat.WEBP_LOSSLESS, 100, baos)
            }
        }
        OutputImageFormat.BMP -> {
            // BMP: write raw bitmap data as BMP file
            writeBmp(bitmap, baos)
        }
    }
    val bytes = baos.toByteArray()
    val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    return Pair(bytes, decoded)
}

private fun writeBmp(bitmap: Bitmap, baos: ByteArrayOutputStream) {
    val width = bitmap.width
    val height = bitmap.height
    val rowSize = ((24 * width + 31) / 32) * 4
    val imageSize = rowSize * height
    val fileSize = 54 + imageSize

    // BMP File Header (14 bytes)
    baos.write(byteArrayOf(0x42, 0x4D)) // "BM"
    baos.write(intToLittleEndian(fileSize))
    baos.write(intToLittleEndian(0)) // Reserved
    baos.write(intToLittleEndian(54)) // Pixel data offset

    // DIB Header (40 bytes)
    baos.write(intToLittleEndian(40)) // Header size
    baos.write(intToLittleEndian(width))
    baos.write(intToLittleEndian(height))
    baos.write(shortToLittleEndian(1)) // Planes
    baos.write(shortToLittleEndian(24)) // Bits per pixel
    baos.write(intToLittleEndian(0)) // Compression
    baos.write(intToLittleEndian(imageSize))
    baos.write(intToLittleEndian(2835)) // X ppm
    baos.write(intToLittleEndian(2835)) // Y ppm
    baos.write(intToLittleEndian(0)) // Colors used
    baos.write(intToLittleEndian(0)) // Important colors

    // Pixel data (bottom-up)
    val padding = rowSize - (width * 3)
    val padBytes = ByteArray(padding)
    for (y in height - 1 downTo 0) {
        for (x in 0 until width) {
            val pixel = bitmap.getPixel(x, y)
            baos.write((pixel and 0xFF)) // Blue
            baos.write((pixel shr 8) and 0xFF) // Green
            baos.write((pixel shr 16) and 0xFF) // Red
        }
        if (padding > 0) baos.write(padBytes)
    }
}

private fun intToLittleEndian(value: Int): ByteArray {
    return byteArrayOf(
        (value and 0xFF).toByte(),
        ((value shr 8) and 0xFF).toByte(),
        ((value shr 16) and 0xFF).toByte(),
        ((value shr 24) and 0xFF).toByte()
    )
}

private fun shortToLittleEndian(value: Int): ByteArray {
    return byteArrayOf(
        (value and 0xFF).toByte(),
        ((value shr 8) and 0xFF).toByte()
    )
}

private fun formatConvSize(bytes: Long): String {
    return when {
        bytes >= 1_048_576 -> String.format("%.2f MB", bytes / 1_048_576.0)
        bytes >= 1024 -> String.format("%.1f KB", bytes / 1024.0)
        else -> "$bytes B"
    }
}
