package cv.toolkit.screens

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cv.toolkit.R
import cv.toolkit.ads.BannerAd
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

private data class CompressPdfInfo(
    val uri: Uri,
    val name: String,
    val pageCount: Int,
    val size: Long,
    val firstPageBitmap: Bitmap?
)

private enum class QualityPreset(val label: String, val quality: Int) {
    LOW("Low (50%)", 50),
    MEDIUM("Medium (70%)", 70),
    HIGH("High (85%)", 85),
    CUSTOM("Custom", -1)
}

private enum class DpiOption(val label: String, val dpi: Int) {
    DPI_72("72 DPI", 72),
    DPI_100("100 DPI", 100),
    DPI_150("150 DPI", 150),
    DPI_200("200 DPI (Original)", 200)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompressPdfScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var pdfInfo by remember { mutableStateOf<CompressPdfInfo?>(null) }
    var selectedPreset by remember { mutableStateOf(QualityPreset.MEDIUM) }
    var customQuality by remember { mutableFloatStateOf(70f) }
    var selectedDpi by remember { mutableStateOf(DpiOption.DPI_150) }
    var grayscale by remember { mutableStateOf(false) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isCompressing by remember { mutableStateOf(false) }
    var compressProgress by remember { mutableFloatStateOf(0f) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var compressedSize by remember { mutableLongStateOf(0L) }

    val effectiveQuality = if (selectedPreset == QualityPreset.CUSTOM) {
        customQuality.toInt()
    } else {
        selectedPreset.quality
    }

    // Estimate output size based on settings
    val estimatedSize = remember(pdfInfo, effectiveQuality, selectedDpi, grayscale) {
        pdfInfo?.let { info ->
            val qualityFactor = effectiveQuality / 100.0
            val dpiFactor = (selectedDpi.dpi / 200.0) * (selectedDpi.dpi / 200.0)
            val grayscaleFactor = if (grayscale) 0.6 else 1.0
            (info.size * qualityFactor * dpiFactor * grayscaleFactor).toLong().coerceAtLeast(1024)
        }
    }

    // Generate preview when settings change
    LaunchedEffect(pdfInfo, effectiveQuality, selectedDpi, grayscale) {
        pdfInfo?.let { info ->
            previewBitmap = withContext(Dispatchers.IO) {
                generatePreview(context, info.uri, effectiveQuality, selectedDpi.dpi, grayscale)
            }
        }
    }

    val pdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                try {
                    val info = withContext(Dispatchers.IO) {
                        getCompressPdfInfo(context, it)
                    }
                    if (info != null) {
                        pdfInfo = info
                        errorMessage = null
                        successMessage = null
                        compressedSize = 0L
                    } else {
                        errorMessage = "Failed to read PDF file."
                    }
                } catch (e: Exception) {
                    errorMessage = "Error opening PDF: ${e.message}"
                }
            }
        }
    }

    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        uri?.let { outputUri ->
            val info = pdfInfo ?: return@let
            isCompressing = true
            compressProgress = 0f
            errorMessage = null
            successMessage = null
            scope.launch {
                try {
                    val result = withContext(Dispatchers.IO) {
                        compressPdf(
                            context = context,
                            sourceUri = info.uri,
                            outputUri = outputUri,
                            quality = effectiveQuality,
                            dpi = selectedDpi.dpi,
                            grayscale = grayscale,
                            onProgress = { progress -> compressProgress = progress }
                        )
                    }
                    if (result > 0) {
                        compressedSize = result
                        val reduction = if (info.size > 0) {
                            ((1.0 - result.toDouble() / info.size) * 100).toInt()
                        } else {
                            0
                        }
                        successMessage = if (reduction > 0) {
                            "Compression complete! Reduced by $reduction% (${formatCompressSize(result)})"
                        } else {
                            "Compression complete. Output: ${formatCompressSize(result)}"
                        }
                    } else {
                        errorMessage = "Failed to compress PDF."
                    }
                } catch (e: Exception) {
                    errorMessage = "Compression error: ${e.message}"
                } finally {
                    isCompressing = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.compress_pdf_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
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
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Pick PDF button
                Button(
                    onClick = { pdfPicker.launch("application/pdf") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isCompressing
                ) {
                    Icon(Icons.Filled.UploadFile, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Select PDF File")
                }

                // Empty state
                if (pdfInfo == null && !isCompressing) {
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
                                Icons.Filled.PictureAsPdf,
                                null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "No PDF Selected",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Select a PDF file to compress it\nby reducing quality and resolution.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // PDF info card
                pdfInfo?.let { info ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
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
                                null,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    info.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    "${info.pageCount} page${if (info.pageCount != 1) "s" else ""} \u2022 ${formatCompressSize(info.size)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Quality preset selector
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Quality Preset",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                QualityPreset.entries.forEach { preset ->
                                    FilterChip(
                                        selected = selectedPreset == preset,
                                        onClick = {
                                            selectedPreset = preset
                                            if (preset != QualityPreset.CUSTOM) {
                                                customQuality = preset.quality.toFloat()
                                            }
                                        },
                                        label = { Text(preset.label, style = MaterialTheme.typography.labelSmall) },
                                        modifier = Modifier.weight(1f),
                                        enabled = !isCompressing
                                    )
                                }
                            }

                            // Custom quality slider
                            if (selectedPreset == QualityPreset.CUSTOM) {
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    "Quality: ${customQuality.toInt()}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Slider(
                                    value = customQuality,
                                    onValueChange = { customQuality = it },
                                    valueRange = 10f..100f,
                                    steps = 17,
                                    enabled = !isCompressing,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                    // Resolution (DPI) selector
                    Card(
                        modifier = Modifier.fillMaxWidth(),
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
                            Spacer(Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                DpiOption.entries.forEach { dpiOption ->
                                    FilterChip(
                                        selected = selectedDpi == dpiOption,
                                        onClick = { selectedDpi = dpiOption },
                                        label = { Text(dpiOption.label, style = MaterialTheme.typography.labelSmall) },
                                        modifier = Modifier.weight(1f),
                                        enabled = !isCompressing
                                    )
                                }
                            }
                        }
                    }

                    // Grayscale toggle
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Grayscale",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "Convert to black & white to reduce size further",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = grayscale,
                                onCheckedChange = { grayscale = it },
                                enabled = !isCompressing
                            )
                        }
                    }

                    // Estimated output size
                    estimatedSize?.let { estSize ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Estimated Output",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            formatCompressSize(info.size),
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            "Original",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                        )
                                    }
                                    Icon(
                                        Icons.AutoMirrored.Filled.ArrowForward,
                                        null,
                                        modifier = Modifier
                                            .size(24.dp)
                                            .align(Alignment.CenterVertically),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
                                    )
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            "~${formatCompressSize(estSize)}",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            "Estimated",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Preview first page
                    previewBitmap?.let { bitmap ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Preview (First Page)",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.height(8.dp))
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = "Preview of first page at selected quality",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 300.dp),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                    }

                    // Info note
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
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
                                "Pages are re-rendered as images at the selected quality. Text in the output will not be searchable.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }

                    // Progress indicator
                    if (isCompressing) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "Compressing PDF...",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(Modifier.height(12.dp))
                                LinearProgressIndicator(
                                    progress = { compressProgress },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "${(compressProgress * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Error message
                    if (errorMessage != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
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
                                    errorMessage!!,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    // Success message with compression ratio
                    if (successMessage != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.CheckCircle,
                                    null,
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    successMessage!!,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    // Compress button
                    Button(
                        onClick = {
                            successMessage = null
                            errorMessage = null
                            val suggestedName = info.name.removeSuffix(".pdf") + "_compressed.pdf"
                            saveLauncher.launch(suggestedName)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isCompressing,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Filled.Compress, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Compress PDF")
                    }
                }
            }

            BannerAd(modifier = Modifier.fillMaxWidth())
        }
    }
}

private fun getCompressPdfInfo(context: Context, uri: Uri): CompressPdfInfo? {
    return try {
        var name = "Unknown.pdf"
        var size = 0L

        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (nameIndex >= 0) {
                    name = cursor.getString(nameIndex) ?: "Unknown.pdf"
                }
                if (sizeIndex >= 0) {
                    size = cursor.getLong(sizeIndex)
                }
            }
        }

        val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return null
        val renderer = PdfRenderer(pfd)
        val pageCount = renderer.pageCount

        val thumbnail = if (pageCount > 0) {
            val page = renderer.openPage(0)
            val thumbWidth = 200
            val thumbHeight = (thumbWidth * page.height.toFloat() / page.width.toFloat()).toInt()
            val bitmap = Bitmap.createBitmap(thumbWidth, thumbHeight, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            bitmap
        } else {
            null
        }

        renderer.close()
        pfd.close()

        CompressPdfInfo(
            uri = uri,
            name = name,
            pageCount = pageCount,
            size = size,
            firstPageBitmap = thumbnail
        )
    } catch (e: Exception) {
        null
    }
}

private fun generatePreview(
    context: Context,
    uri: Uri,
    quality: Int,
    dpi: Int,
    grayscale: Boolean
): Bitmap? {
    return try {
        val pfd = context.contentResolver.openFileDescriptor(uri, "r") ?: return null
        val renderer = PdfRenderer(pfd)
        if (renderer.pageCount == 0) {
            renderer.close()
            pfd.close()
            return null
        }

        val page = renderer.openPage(0)
        val scale = dpi / 72f
        val bitmapWidth = (page.width * scale).toInt().coerceAtLeast(1)
        val bitmapHeight = (page.height * scale).toInt().coerceAtLeast(1)

        val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.WHITE)

        val transform = Matrix()
        transform.setScale(scale, scale)
        page.render(bitmap, null, transform, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        page.close()
        renderer.close()
        pfd.close()

        // Apply grayscale if needed
        val processedBitmap = if (grayscale) {
            toGrayscale(bitmap)
        } else {
            bitmap
        }

        // Compress to JPEG and decode back to simulate quality loss
        val baos = ByteArrayOutputStream()
        processedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos)
        if (processedBitmap != bitmap) {
            processedBitmap.recycle()
        }
        bitmap.recycle()

        val bytes = baos.toByteArray()
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    } catch (e: Exception) {
        null
    }
}

private fun compressPdf(
    context: Context,
    sourceUri: Uri,
    outputUri: Uri,
    quality: Int,
    dpi: Int,
    grayscale: Boolean,
    onProgress: (Float) -> Unit
): Long {
    val pfd = context.contentResolver.openFileDescriptor(sourceUri, "r") ?: return -1L
    val renderer = PdfRenderer(pfd)
    val pageCount = renderer.pageCount
    if (pageCount == 0) {
        renderer.close()
        pfd.close()
        return -1L
    }

    val outputDocument = PdfDocument()
    val scale = dpi / 72f

    try {
        for (pageIndex in 0 until pageCount) {
            val sourcePage = renderer.openPage(pageIndex)

            val pageWidth = sourcePage.width
            val pageHeight = sourcePage.height

            val bitmapWidth = (pageWidth * scale).toInt().coerceAtLeast(1)
            val bitmapHeight = (pageHeight * scale).toInt().coerceAtLeast(1)

            // Render source page to bitmap at selected DPI
            val bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.WHITE)

            val transform = Matrix()
            transform.setScale(scale, scale)
            sourcePage.render(bitmap, null, transform, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
            sourcePage.close()

            // Apply grayscale if needed
            val processedBitmap = if (grayscale) {
                toGrayscale(bitmap)
            } else {
                bitmap
            }

            // Compress to JPEG at selected quality, then decode back
            val baos = ByteArrayOutputStream()
            processedBitmap.compress(Bitmap.CompressFormat.JPEG, quality, baos)
            if (processedBitmap != bitmap) {
                processedBitmap.recycle()
            }
            bitmap.recycle()

            val jpegBytes = baos.toByteArray()
            val compressedBitmap = BitmapFactory.decodeStream(ByteArrayInputStream(jpegBytes))

            // Create output page at original point dimensions
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex).create()
            val outputPage = outputDocument.startPage(pageInfo)
            val canvas = outputPage.canvas

            // Draw compressed bitmap scaled to fill the page
            val destRect = RectF(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat())
            canvas.drawBitmap(
                compressedBitmap,
                Rect(0, 0, compressedBitmap.width, compressedBitmap.height),
                destRect,
                Paint(Paint.FILTER_BITMAP_FLAG)
            )

            outputDocument.finishPage(outputPage)
            compressedBitmap.recycle()

            onProgress((pageIndex + 1).toFloat() / pageCount)
        }

        renderer.close()
        pfd.close()

        // Write output
        val outputStream = context.contentResolver.openOutputStream(outputUri)
            ?: return -1L

        outputStream.use { stream ->
            outputDocument.writeTo(stream)
        }

        // Get actual output size
        var outputSize = 0L
        context.contentResolver.query(outputUri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex >= 0) {
                    outputSize = cursor.getLong(sizeIndex)
                }
            }
        }

        // Fallback: read output to measure size
        if (outputSize == 0L) {
            context.contentResolver.openInputStream(outputUri)?.use { input ->
                outputSize = input.available().toLong()
            }
        }

        return outputSize
    } catch (e: Exception) {
        return -1L
    } finally {
        outputDocument.close()
    }
}

private fun toGrayscale(source: Bitmap): Bitmap {
    val grayscaleBitmap = Bitmap.createBitmap(source.width, source.height, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(grayscaleBitmap)
    val paint = Paint()
    val colorMatrix = ColorMatrix()
    colorMatrix.setSaturation(0f)
    paint.colorFilter = ColorMatrixColorFilter(colorMatrix)
    canvas.drawBitmap(source, 0f, 0f, paint)
    return grayscaleBitmap
}

private fun formatCompressSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
        bytes < 1024L * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024))
        else -> String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024))
    }
}
