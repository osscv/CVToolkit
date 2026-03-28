package cv.toolkit.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Matrix
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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

private enum class ImageFormat(val label: String, val extension: String, val mimeType: String) {
    PNG("PNG (Lossless)", "png", "image/png"),
    JPEG("JPEG (Smaller)", "jpg", "image/jpeg")
}

private enum class ExportDpi(val label: String, val dpi: Int) {
    DPI_72("72 DPI", 72),
    DPI_150("150 DPI", 150),
    DPI_200("200 DPI", 200),
    DPI_300("300 DPI (Print)", 300)
}

private data class PdfToImageInfo(
    val uri: Uri,
    val name: String,
    val pageCount: Int,
    val size: Long
)

private data class RenderedPage(
    val pageIndex: Int,
    val bitmap: Bitmap,
    val bytes: ByteArray,
    val width: Int,
    val height: Int,
    val sizeBytes: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfToImageScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var pdfInfo by remember { mutableStateOf<PdfToImageInfo?>(null) }
    var selectedFormat by remember { mutableStateOf(ImageFormat.PNG) }
    var selectedDpi by remember { mutableStateOf(ExportDpi.DPI_200) }
    var jpegQuality by remember { mutableIntStateOf(90) }
    var renderedPages by remember { mutableStateOf<List<RenderedPage>>(emptyList()) }
    var isProcessing by remember { mutableStateOf(false) }
    var processProgress by remember { mutableFloatStateOf(0f) }
    var pendingSaveIndex by remember { mutableIntStateOf(-1) }

    val pdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                isProcessing = true
                try {
                    val info = withContext(Dispatchers.IO) {
                        getPdfToImageInfo(context, it)
                    }
                    pdfInfo = info
                    renderedPages = emptyList()
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Failed to open PDF: ${e.message}")
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
            val idx = pendingSaveIndex
            if (idx in renderedPages.indices) {
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            context.contentResolver.openOutputStream(destUri)?.use { os ->
                                os.write(renderedPages[idx].bytes)
                            }
                        }
                        snackbarHostState.showSnackbar("Page ${idx + 1} saved")
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar("Failed to save: ${e.message}")
                    }
                }
            }
        }
    }

    fun convertPages() {
        val info = pdfInfo ?: return
        scope.launch {
            isProcessing = true
            processProgress = 0f
            try {
                val pages = withContext(Dispatchers.IO) {
                    renderPdfPages(context, info.uri, selectedFormat, selectedDpi.dpi, jpegQuality) { progress ->
                        processProgress = progress
                    }
                }
                renderedPages = pages
                snackbarHostState.showSnackbar("${pages.size} page(s) converted")
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
                title = { Text("PDF to Image") },
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
                // Select PDF button
                item {
                    Button(
                        onClick = { pdfPicker.launch("application/pdf") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isProcessing
                    ) {
                        Icon(Icons.Filled.UploadFile, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Select PDF File")
                    }
                }

                // Empty state
                if (pdfInfo == null && !isProcessing) {
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
                                    Icons.Filled.BurstMode,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "Convert PDF Pages to Images",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Extract each page as a high-quality PNG or JPEG image",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // PDF info + settings
                pdfInfo?.let { info ->
                    // File info card
                    item {
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
                                    Icons.Filled.PictureAsPdf,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        info.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        "${info.pageCount} page${if (info.pageCount != 1) "s" else ""} \u2022 ${formatPdfToImageSize(info.size)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Format selector
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Output Format",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ImageFormat.entries.forEach { format ->
                                        FilterChip(
                                            selected = selectedFormat == format,
                                            onClick = { selectedFormat = format },
                                            label = { Text(format.label, style = MaterialTheme.typography.labelSmall) },
                                            modifier = Modifier.weight(1f),
                                            enabled = !isProcessing
                                        )
                                    }
                                }
                                if (selectedFormat == ImageFormat.JPEG) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        "JPEG Quality: $jpegQuality%",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Slider(
                                        value = jpegQuality.toFloat(),
                                        onValueChange = { jpegQuality = it.toInt() },
                                        valueRange = 50f..100f,
                                        steps = 9,
                                        enabled = !isProcessing,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }

                    // DPI selector
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Resolution",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    ExportDpi.entries.forEach { dpi ->
                                        FilterChip(
                                            selected = selectedDpi == dpi,
                                            onClick = { selectedDpi = dpi },
                                            label = { Text(dpi.label, style = MaterialTheme.typography.labelSmall) },
                                            modifier = Modifier.weight(1f),
                                            enabled = !isProcessing
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Progress
                    if (isProcessing) {
                        item {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Converting pages...", style = MaterialTheme.typography.bodyMedium)
                                Spacer(modifier = Modifier.height(12.dp))
                                LinearProgressIndicator(
                                    progress = { processProgress },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "${(processProgress * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Convert button
                    if (renderedPages.isEmpty()) {
                        item {
                            Button(
                                onClick = { convertPages() },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isProcessing
                            ) {
                                Icon(Icons.Filled.Transform, contentDescription = null, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Convert ${info.pageCount} Page${if (info.pageCount != 1) "s" else ""}")
                            }
                        }
                    }
                }

                // Rendered pages
                if (renderedPages.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${renderedPages.size} Page(s) Converted",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            TextButton(onClick = {
                                renderedPages = emptyList()
                            }) {
                                Text("Reconvert")
                            }
                        }
                    }

                    itemsIndexed(renderedPages) { index, page ->
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                // Page preview
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 200.dp)
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant,
                                            shape = MaterialTheme.shapes.small
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        bitmap = page.bitmap.asImageBitmap(),
                                        contentDescription = "Page ${index + 1}",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            "Page ${index + 1}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            "${page.width} x ${page.height} \u2022 ${formatPdfToImageSize(page.sizeBytes.toLong())}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    OutlinedButton(onClick = {
                                        pendingSaveIndex = index
                                        val pdfName = pdfInfo?.name?.removeSuffix(".pdf") ?: "page"
                                        saveLauncher.launch("${pdfName}_page${index + 1}.${selectedFormat.extension}")
                                    }) {
                                        Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Save")
                                    }
                                }
                            }
                        }
                    }
                }
            }
            BannerAd(modifier = Modifier.fillMaxWidth())
        }
    }
}

private fun getPdfToImageInfo(context: Context, uri: Uri): PdfToImageInfo {
    var name = "Unknown.pdf"
    var size = 0L
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (nameIndex >= 0) name = cursor.getString(nameIndex) ?: "Unknown.pdf"
            if (sizeIndex >= 0) size = cursor.getLong(sizeIndex)
        }
    }

    val pfd = context.contentResolver.openFileDescriptor(uri, "r")
        ?: throw Exception("Cannot open file")
    val renderer = PdfRenderer(pfd)
    val pageCount = renderer.pageCount
    renderer.close()
    pfd.close()

    return PdfToImageInfo(uri, name, pageCount, size)
}

private fun renderPdfPages(
    context: Context,
    uri: Uri,
    format: ImageFormat,
    dpi: Int,
    jpegQuality: Int,
    onProgress: (Float) -> Unit
): List<RenderedPage> {
    val pfd = context.contentResolver.openFileDescriptor(uri, "r")
        ?: throw Exception("Cannot open file")
    val renderer = PdfRenderer(pfd)
    val pages = mutableListOf<RenderedPage>()

    try {
        val scale = dpi / 72f
        for (i in 0 until renderer.pageCount) {
            val page = renderer.openPage(i)
            val bitmapWidth = (page.width * scale).toInt().coerceAtLeast(1)
            val bitmapHeight = (page.height * scale).toInt().coerceAtLeast(1)

            val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(android.graphics.Color.WHITE)

            val transform = Matrix()
            transform.setScale(scale, scale)
            page.render(bitmap, null, transform, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
            page.close()

            // Encode to bytes
            val baos = ByteArrayOutputStream()
            when (format) {
                ImageFormat.PNG -> bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos)
                ImageFormat.JPEG -> bitmap.compress(Bitmap.CompressFormat.JPEG, jpegQuality, baos)
            }
            val bytes = baos.toByteArray()

            pages.add(
                RenderedPage(
                    pageIndex = i,
                    bitmap = bitmap,
                    bytes = bytes,
                    width = bitmapWidth,
                    height = bitmapHeight,
                    sizeBytes = bytes.size
                )
            )
            onProgress((i + 1).toFloat() / renderer.pageCount)
        }
    } finally {
        renderer.close()
        pfd.close()
    }

    return pages
}

private fun formatPdfToImageSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
        bytes < 1024L * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024))
        else -> String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024))
    }
}
