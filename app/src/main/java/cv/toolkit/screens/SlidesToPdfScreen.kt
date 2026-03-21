package cv.toolkit.screens

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cv.toolkit.R
import cv.toolkit.ads.BannerAd
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

private data class SlideItem(
    val uri: Uri,
    val name: String,
    val thumbnail: Bitmap?
)

private enum class SlidePageSize(val label: String, val widthPt: Int, val heightPt: Int) {
    WIDESCREEN_16_9("16:9 Widescreen", 1280, 720),
    STANDARD_4_3("4:3 Standard", 1024, 768),
    A4_LANDSCAPE("A4 Landscape", 842, 595)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SlidesToPdfScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var slides by remember { mutableStateOf(listOf<SlideItem>()) }
    var pageSize by remember { mutableStateOf(SlidePageSize.WIDESCREEN_16_9) }
    var addSlideNumbers by remember { mutableStateOf(false) }
    var isConverting by remember { mutableStateOf(false) }
    var conversionProgress by remember { mutableFloatStateOf(0f) }
    var pageSizeMenuExpanded by remember { mutableStateOf(false) }

    var pendingPdfBytes by remember { mutableStateOf<ByteArray?>(null) }
    var pendingPageCount by remember { mutableIntStateOf(0) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                val newItems = withContext(Dispatchers.IO) {
                    uris.mapNotNull { uri ->
                        val name = getSlideDisplayName(context, uri)
                        val thumbnail = decodeSlideThumb(context, uri, 256)
                        if (thumbnail != null) SlideItem(uri, name, thumbnail) else null
                    }
                }
                slides = slides + newItems
            }
        }
    }

    val savePdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        uri?.let { destUri ->
            val bytes = pendingPdfBytes
            if (bytes != null) {
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            context.contentResolver.openOutputStream(destUri)?.use { os ->
                                os.write(bytes)
                            }
                        }
                        snackbarHostState.showSnackbar("PDF saved successfully ($pendingPageCount pages)")
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar("Failed to save PDF: ${e.message}")
                    } finally {
                        pendingPdfBytes = null
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.slides_to_pdf_title)) },
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Scrollable content area
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Spacer(Modifier.height(4.dp))

                // Info note at top
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
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
                            "Export your PowerPoint/Google Slides as images, then convert them to PDF here.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                // Add slides button
                Button(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isConverting
                ) {
                    Icon(Icons.Filled.AddPhotoAlternate, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Add Slide Images")
                }

                // Empty state
                if (slides.isEmpty() && !isConverting) {
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
                                Icons.Filled.Slideshow,
                                null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "No Slides Added",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Select slide images exported from PowerPoint or Google Slides.\nEach image becomes one page in the PDF.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                // Slide list header
                if (slides.isNotEmpty()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${slides.size} slide(s)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        TextButton(
                            onClick = { slides = emptyList() },
                            enabled = !isConverting
                        ) {
                            Text("Clear All")
                        }
                    }

                    // Slide thumbnail grid
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        itemsIndexed(slides, key = { index, item -> "${item.uri}_$index" }) { index, item ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(
                                    modifier = Modifier.padding(8.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    // Slide number label
                                    Text(
                                        "Slide ${index + 1}",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.height(4.dp))

                                    // Thumbnail
                                    item.thumbnail?.let { bmp ->
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .aspectRatio(16f / 9f)
                                                .border(
                                                    1.dp,
                                                    MaterialTheme.colorScheme.outlineVariant,
                                                    MaterialTheme.shapes.small
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Image(
                                                bitmap = bmp.asImageBitmap(),
                                                contentDescription = "Slide ${index + 1}",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Fit
                                            )
                                        }
                                    }

                                    Spacer(Modifier.height(4.dp))

                                    // Action buttons row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Move up
                                        IconButton(
                                            onClick = {
                                                if (index > 0) {
                                                    val mutable = slides.toMutableList()
                                                    val temp = mutable[index]
                                                    mutable[index] = mutable[index - 1]
                                                    mutable[index - 1] = temp
                                                    slides = mutable
                                                }
                                            },
                                            enabled = index > 0 && !isConverting,
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Filled.KeyboardArrowUp,
                                                "Move up",
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        // Move down
                                        IconButton(
                                            onClick = {
                                                if (index < slides.size - 1) {
                                                    val mutable = slides.toMutableList()
                                                    val temp = mutable[index]
                                                    mutable[index] = mutable[index + 1]
                                                    mutable[index + 1] = temp
                                                    slides = mutable
                                                }
                                            },
                                            enabled = index < slides.size - 1 && !isConverting,
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Filled.KeyboardArrowDown,
                                                "Move down",
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        // Remove
                                        IconButton(
                                            onClick = {
                                                slides = slides.toMutableList().also { it.removeAt(index) }
                                            },
                                            enabled = !isConverting,
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                Icons.Filled.Close,
                                                "Remove",
                                                modifier = Modifier.size(18.dp),
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Settings section
                    Text(
                        "Settings",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    // Page size dropdown
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Page Size",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(8.dp))
                            ExposedDropdownMenuBox(
                                expanded = pageSizeMenuExpanded,
                                onExpandedChange = { pageSizeMenuExpanded = it }
                            ) {
                                OutlinedTextField(
                                    value = pageSize.label,
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = pageSizeMenuExpanded)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                                )
                                ExposedDropdownMenu(
                                    expanded = pageSizeMenuExpanded,
                                    onDismissRequest = { pageSizeMenuExpanded = false }
                                ) {
                                    SlidePageSize.entries.forEach { size ->
                                        DropdownMenuItem(
                                            text = {
                                                Text("${size.label} (${size.widthPt}x${size.heightPt})")
                                            },
                                            onClick = {
                                                pageSize = size
                                                pageSizeMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Slide numbers toggle
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Add Slide Numbers",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "Overlay page number at the bottom of each slide",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = addSlideNumbers,
                                onCheckedChange = { addSlideNumbers = it }
                            )
                        }
                    }

                    // Progress bar
                    if (isConverting) {
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
                                Text("Converting...", style = MaterialTheme.typography.bodyMedium)
                                Spacer(Modifier.height(12.dp))
                                LinearProgressIndicator(
                                    progress = { conversionProgress },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "${(conversionProgress * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Convert button
                    Button(
                        onClick = {
                            isConverting = true
                            conversionProgress = 0f
                            scope.launch {
                                try {
                                    val result = withContext(Dispatchers.IO) {
                                        convertSlidesToPdf(
                                            context = context,
                                            slides = slides,
                                            pageSize = pageSize,
                                            addSlideNumbers = addSlideNumbers,
                                            onProgress = { progress ->
                                                conversionProgress = progress
                                            }
                                        )
                                    }
                                    pendingPdfBytes = result.first
                                    pendingPageCount = result.second
                                    savePdfLauncher.launch("slides.pdf")
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("Conversion failed: ${e.message}")
                                } finally {
                                    isConverting = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = slides.isNotEmpty() && !isConverting
                    ) {
                        if (isConverting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(Icons.Filled.PictureAsPdf, null, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(if (isConverting) "Converting..." else "Convert to PDF")
                    }
                }

                Spacer(Modifier.height(8.dp))
            }

            // Banner ad at bottom
            BannerAd(modifier = Modifier.fillMaxWidth())
        }
    }
}

private fun getSlideDisplayName(context: Context, uri: Uri): String {
    var name = "Unknown"
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (nameIndex >= 0) {
                name = cursor.getString(nameIndex) ?: "Unknown"
            }
        }
    }
    return name
}

private fun decodeSlideThumb(context: Context, uri: Uri, maxSize: Int): Bitmap? {
    return try {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream, null, options)
        }

        val width = options.outWidth
        val height = options.outHeight
        var sampleSize = 1
        while (width / sampleSize > maxSize || height / sampleSize > maxSize) {
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
        }
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            BitmapFactory.decodeStream(inputStream, null, decodeOptions)
        }
    } catch (_: Exception) {
        null
    }
}

private fun decodeSlideFullBitmap(context: Context, uri: Uri, maxDimension: Int): Bitmap? {
    return try {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        }

        val width = options.outWidth
        val height = options.outHeight
        var sampleSize = 1
        while (width / sampleSize > maxDimension || height / sampleSize > maxDimension) {
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        context.contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, decodeOptions)
        }
    } catch (_: Exception) {
        null
    }
}

private fun convertSlidesToPdf(
    context: Context,
    slides: List<SlideItem>,
    pageSize: SlidePageSize,
    addSlideNumbers: Boolean,
    onProgress: (Float) -> Unit
): Pair<ByteArray, Int> {
    val pageWidth = pageSize.widthPt
    val pageHeight = pageSize.heightPt

    val document = PdfDocument()
    var pageCount = 0

    try {
        slides.forEachIndexed { index, slideItem ->
            val bitmap = decodeSlideFullBitmap(context, slideItem.uri, 4096)
            if (bitmap != null) {
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create()
                val page = document.startPage(pageInfo)
                val canvas = page.canvas

                // White background
                canvas.drawColor(Color.WHITE)

                // Calculate scaling to fit image centered on page while maintaining aspect ratio
                val scaleX = pageWidth.toFloat() / bitmap.width
                val scaleY = pageHeight.toFloat() / bitmap.height
                val scale = minOf(scaleX, scaleY)

                val scaledWidth = (bitmap.width * scale).toInt()
                val scaledHeight = (bitmap.height * scale).toInt()
                val left = (pageWidth - scaledWidth) / 2f
                val top = (pageHeight - scaledHeight) / 2f

                val destRect = RectF(left, top, left + scaledWidth, top + scaledHeight)
                val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                canvas.drawBitmap(bitmap, null, destRect, paint)

                // Draw slide number if enabled
                if (addSlideNumbers) {
                    val numberPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = Color.DKGRAY
                        textSize = pageHeight * 0.03f
                        textAlign = Paint.Align.CENTER
                        typeface = Typeface.DEFAULT
                    }
                    val slideNumberText = "${index + 1}"
                    val xPos = pageWidth / 2f
                    val yPos = pageHeight - (pageHeight * 0.02f)
                    canvas.drawText(slideNumberText, xPos, yPos, numberPaint)
                }

                document.finishPage(page)
                bitmap.recycle()
                pageCount++
            }
            onProgress((index + 1).toFloat() / slides.size)
        }

        val outputStream = ByteArrayOutputStream()
        document.writeTo(outputStream)
        return Pair(outputStream.toByteArray(), pageCount)
    } finally {
        document.close()
    }
}
