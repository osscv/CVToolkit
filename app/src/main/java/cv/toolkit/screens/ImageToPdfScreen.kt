package cv.toolkit.screens

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
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
import java.io.IOException

private data class ImageItem(
    val uri: Uri,
    val name: String,
    val thumbnail: Bitmap?
)

private enum class PageSize(val label: String, val widthPt: Int, val heightPt: Int) {
    A4("A4", 595, 842),
    LETTER("Letter", 612, 792),
    A3("A3", 842, 1191),
    CUSTOM("Custom", 595, 842)
}

private enum class Orientation(val label: String) {
    PORTRAIT("Portrait"),
    LANDSCAPE("Landscape")
}

private enum class MarginPreset(val label: String, val valuePt: Int) {
    NONE("None", 0),
    SMALL("Small", 18),
    MEDIUM("Medium", 36),
    LARGE("Large", 72)
}

private enum class FitMode(val label: String) {
    FIT("Fit"),
    FILL("Fill"),
    ORIGINAL("Original")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageToPdfScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var images by remember { mutableStateOf(listOf<ImageItem>()) }
    var pageSize by remember { mutableStateOf(PageSize.A4) }
    var customWidthPt by remember { mutableStateOf("595") }
    var customHeightPt by remember { mutableStateOf("842") }
    var orientation by remember { mutableStateOf(Orientation.PORTRAIT) }
    var marginPreset by remember { mutableStateOf(MarginPreset.MEDIUM) }
    var fitMode by remember { mutableStateOf(FitMode.FIT) }
    var quality by remember { mutableIntStateOf(85) }
    var isConverting by remember { mutableStateOf(false) }
    var conversionProgress by remember { mutableFloatStateOf(0f) }
    var previewBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var pageSizeMenuExpanded by remember { mutableStateOf(false) }

    // Pending PDF bytes to be saved once the user picks a destination
    var pendingPdfBytes by remember { mutableStateOf<ByteArray?>(null) }
    var pendingPageCount by remember { mutableIntStateOf(0) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                val newItems = withContext(Dispatchers.IO) {
                    uris.mapNotNull { uri ->
                        val name = getDisplayName(context, uri)
                        val thumbnail = decodeThumbnail(context, uri, 256)
                        if (thumbnail != null) ImageItem(uri, name, thumbnail) else null
                    }
                }
                images = images + newItems
                // Generate preview of first page
                if (images.isNotEmpty()) {
                    previewBitmap = withContext(Dispatchers.IO) {
                        generatePreview(context, images.first(), pageSize, customWidthPt, customHeightPt, orientation, marginPreset, fitMode)
                    }
                }
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
                    } catch (e: IOException) {
                        snackbarHostState.showSnackbar("Failed to save PDF: ${e.message}")
                    } finally {
                        pendingPdfBytes = null
                    }
                }
            }
        }
    }

    // Regenerate preview when settings change
    LaunchedEffect(images, pageSize, customWidthPt, customHeightPt, orientation, marginPreset, fitMode) {
        if (images.isNotEmpty()) {
            previewBitmap = withContext(Dispatchers.IO) {
                generatePreview(context, images.first(), pageSize, customWidthPt, customHeightPt, orientation, marginPreset, fitMode)
            }
        } else {
            previewBitmap = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.image_to_pdf_title)) },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Add images button
            item {
                Button(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isConverting
                ) {
                    Icon(Icons.Filled.AddPhotoAlternate, null, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Add Images")
                }
            }

            // Image list
            if (images.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${images.size} image(s)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        TextButton(
                            onClick = { images = emptyList() },
                            enabled = !isConverting
                        ) {
                            Text("Clear All")
                        }
                    }
                }

                itemsIndexed(images, key = { index, item -> "${item.uri}_$index" }) { index, item ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Thumbnail
                            item.thumbnail?.let { bmp ->
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .clip(MaterialTheme.shapes.small)
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = item.name,
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }

                            Spacer(Modifier.width(12.dp))

                            // File name and index
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    item.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "Page ${index + 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }

                            // Move up
                            IconButton(
                                onClick = {
                                    if (index > 0) {
                                        val mutable = images.toMutableList()
                                        val temp = mutable[index]
                                        mutable[index] = mutable[index - 1]
                                        mutable[index - 1] = temp
                                        images = mutable
                                    }
                                },
                                enabled = index > 0 && !isConverting,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Filled.KeyboardArrowUp, "Move up", modifier = Modifier.size(20.dp))
                            }

                            // Move down
                            IconButton(
                                onClick = {
                                    if (index < images.size - 1) {
                                        val mutable = images.toMutableList()
                                        val temp = mutable[index]
                                        mutable[index] = mutable[index + 1]
                                        mutable[index + 1] = temp
                                        images = mutable
                                    }
                                },
                                enabled = index < images.size - 1 && !isConverting,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(Icons.Filled.KeyboardArrowDown, "Move down", modifier = Modifier.size(20.dp))
                            }

                            // Remove
                            IconButton(
                                onClick = {
                                    images = images.toMutableList().also { it.removeAt(index) }
                                },
                                enabled = !isConverting,
                                modifier = Modifier.size(36.dp)
                            ) {
                                Icon(
                                    Icons.Filled.Close,
                                    "Remove",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }
            }

            // Empty state
            if (images.isEmpty() && !isConverting) {
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
                                Icons.Filled.PictureAsPdf,
                                null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "Add Images",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Select images to combine into a PDF document.\nEach image becomes one page.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Settings section
            if (images.isNotEmpty()) {
                item {
                    Text(
                        "Settings",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                // Page size
                item {
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
                                        .menuAnchor(),
                                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                                )
                                ExposedDropdownMenu(
                                    expanded = pageSizeMenuExpanded,
                                    onDismissRequest = { pageSizeMenuExpanded = false }
                                ) {
                                    PageSize.entries.forEach { size ->
                                        DropdownMenuItem(
                                            text = {
                                                Text(
                                                    if (size == PageSize.CUSTOM) "Custom"
                                                    else "${size.label} (${size.widthPt}x${size.heightPt} pt)"
                                                )
                                            },
                                            onClick = {
                                                pageSize = size
                                                pageSizeMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            if (pageSize == PageSize.CUSTOM) {
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    OutlinedTextField(
                                        value = customWidthPt,
                                        onValueChange = { customWidthPt = it.filter { c -> c.isDigit() } },
                                        label = { Text("Width (pt)") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                    OutlinedTextField(
                                        value = customHeightPt,
                                        onValueChange = { customHeightPt = it.filter { c -> c.isDigit() } },
                                        label = { Text("Height (pt)") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true
                                    )
                                }
                            }
                        }
                    }
                }

                // Orientation
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Orientation",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Orientation.entries.forEach { orient ->
                                    FilterChip(
                                        selected = orientation == orient,
                                        onClick = { orientation = orient },
                                        label = { Text(orient.label) },
                                        leadingIcon = if (orientation == orient) {
                                            {
                                                Icon(
                                                    if (orient == Orientation.PORTRAIT) Icons.Filled.StayCurrentPortrait
                                                    else Icons.Filled.StayCurrentLandscape,
                                                    null,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        } else null,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }

                // Margins
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Margins",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                MarginPreset.entries.forEach { margin ->
                                    FilterChip(
                                        selected = marginPreset == margin,
                                        onClick = { marginPreset = margin },
                                        label = { Text(margin.label) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }

                // Fit mode
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Image Fit Mode",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FitMode.entries.forEach { mode ->
                                    FilterChip(
                                        selected = fitMode == mode,
                                        onClick = { fitMode = mode },
                                        label = { Text(mode.label) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                            Spacer(Modifier.height(4.dp))
                            Text(
                                when (fitMode) {
                                    FitMode.FIT -> "Maintain aspect ratio, fit within page"
                                    FitMode.FILL -> "Stretch to fill the entire page"
                                    FitMode.ORIGINAL -> "Center at actual pixel size"
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Quality slider
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "JPEG Quality",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    "$quality%",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Slider(
                                value = quality.toFloat(),
                                onValueChange = { quality = it.toInt() },
                                valueRange = 10f..100f,
                                steps = 8,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                "Lower quality = smaller file size",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Preview of first page
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "First Page Preview",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(8.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp)
                                    .clip(MaterialTheme.shapes.medium)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                previewBitmap?.let { bmp ->
                                    Image(
                                        bitmap = bmp.asImageBitmap(),
                                        contentDescription = "First page preview",
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Fit
                                    )
                                } ?: CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }

                // Progress bar
                if (isConverting) {
                    item {
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
                }

                // Convert button
                item {
                    Button(
                        onClick = {
                            isConverting = true
                            conversionProgress = 0f
                            scope.launch {
                                try {
                                    val result = withContext(Dispatchers.IO) {
                                        convertImagesToPdf(
                                            context = context,
                                            images = images,
                                            pageSize = pageSize,
                                            customWidthPt = customWidthPt,
                                            customHeightPt = customHeightPt,
                                            orientation = orientation,
                                            marginPreset = marginPreset,
                                            fitMode = fitMode,
                                            quality = quality,
                                            onProgress = { progress ->
                                                conversionProgress = progress
                                            }
                                        )
                                    }
                                    pendingPdfBytes = result.first
                                    pendingPageCount = result.second
                                    savePdfLauncher.launch("images.pdf")
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("Conversion failed: ${e.message}")
                                } finally {
                                    isConverting = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = images.isNotEmpty() && !isConverting
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
            }

            // Info card
            item {
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
                            "Each image is placed on its own page. Reorder images to change page order in the PDF.",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Banner ad
            item {
                BannerAd(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

private fun getDisplayName(context: Context, uri: Uri): String {
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

private fun decodeThumbnail(context: Context, uri: Uri, maxSize: Int): Bitmap? {
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

private fun resolvePageDimensions(
    pageSize: PageSize,
    customWidthPt: String,
    customHeightPt: String,
    orientation: Orientation
): Pair<Int, Int> {
    val baseWidth: Int
    val baseHeight: Int
    if (pageSize == PageSize.CUSTOM) {
        baseWidth = customWidthPt.toIntOrNull() ?: 595
        baseHeight = customHeightPt.toIntOrNull() ?: 842
    } else {
        baseWidth = pageSize.widthPt
        baseHeight = pageSize.heightPt
    }
    return if (orientation == Orientation.LANDSCAPE) {
        maxOf(baseWidth, baseHeight) to minOf(baseWidth, baseHeight)
    } else {
        minOf(baseWidth, baseHeight) to maxOf(baseWidth, baseHeight)
    }
}

private fun decodeFullBitmap(context: Context, uri: Uri, maxDimension: Int): Bitmap? {
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

private fun drawBitmapOnPage(
    canvas: Canvas,
    bitmap: Bitmap,
    pageWidth: Int,
    pageHeight: Int,
    margin: Int,
    fitMode: FitMode,
    quality: Int
) {
    val contentWidth = pageWidth - 2 * margin
    val contentHeight = pageHeight - 2 * margin

    if (contentWidth <= 0 || contentHeight <= 0) return

    // Compress bitmap to JPEG at specified quality and re-decode to simulate quality loss
    val compressedBitmap = if (quality < 100) {
        val stream = java.io.ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, stream)
        val bytes = stream.toByteArray()
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: bitmap
    } else {
        bitmap
    }

    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)

    when (fitMode) {
        FitMode.FIT -> {
            val scaleX = contentWidth.toFloat() / compressedBitmap.width
            val scaleY = contentHeight.toFloat() / compressedBitmap.height
            val scale = minOf(scaleX, scaleY)

            val scaledWidth = (compressedBitmap.width * scale).toInt()
            val scaledHeight = (compressedBitmap.height * scale).toInt()

            val left = margin + (contentWidth - scaledWidth) / 2f
            val top = margin + (contentHeight - scaledHeight) / 2f

            val destRect = RectF(left, top, left + scaledWidth, top + scaledHeight)
            canvas.drawBitmap(compressedBitmap, null, destRect, paint)
        }

        FitMode.FILL -> {
            val destRect = RectF(
                margin.toFloat(),
                margin.toFloat(),
                (pageWidth - margin).toFloat(),
                (pageHeight - margin).toFloat()
            )
            canvas.drawBitmap(compressedBitmap, null, destRect, paint)
        }

        FitMode.ORIGINAL -> {
            val left = (pageWidth - compressedBitmap.width) / 2f
            val top = (pageHeight - compressedBitmap.height) / 2f
            canvas.drawBitmap(compressedBitmap, left, top, paint)
        }
    }

    if (compressedBitmap !== bitmap) {
        compressedBitmap.recycle()
    }
}

private fun generatePreview(
    context: Context,
    imageItem: ImageItem,
    pageSize: PageSize,
    customWidthPt: String,
    customHeightPt: String,
    orientation: Orientation,
    marginPreset: MarginPreset,
    fitMode: FitMode
): Bitmap? {
    val (pageWidth, pageHeight) = resolvePageDimensions(pageSize, customWidthPt, customHeightPt, orientation)
    val margin = marginPreset.valuePt

    val bitmap = decodeFullBitmap(context, imageItem.uri, 1024) ?: return null

    // Create a preview bitmap scaled down for display
    val previewScale = 2f // render at 2x points for decent preview resolution
    val previewWidth = (pageWidth * previewScale).toInt()
    val previewHeight = (pageHeight * previewScale).toInt()

    val previewBitmap = Bitmap.createBitmap(previewWidth, previewHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(previewBitmap)
    canvas.drawColor(Color.WHITE)

    drawBitmapOnPage(
        canvas = canvas,
        bitmap = bitmap,
        pageWidth = previewWidth,
        pageHeight = previewHeight,
        margin = (margin * previewScale).toInt(),
        fitMode = fitMode,
        quality = 100 // No quality loss for preview
    )

    bitmap.recycle()
    return previewBitmap
}

private fun convertImagesToPdf(
    context: Context,
    images: List<ImageItem>,
    pageSize: PageSize,
    customWidthPt: String,
    customHeightPt: String,
    orientation: Orientation,
    marginPreset: MarginPreset,
    fitMode: FitMode,
    quality: Int,
    onProgress: (Float) -> Unit
): Pair<ByteArray, Int> {
    val (pageWidth, pageHeight) = resolvePageDimensions(pageSize, customWidthPt, customHeightPt, orientation)
    val margin = marginPreset.valuePt

    val document = PdfDocument()
    var pageCount = 0

    try {
        images.forEachIndexed { index, imageItem ->
            val bitmap = decodeFullBitmap(context, imageItem.uri, 4096)
            if (bitmap != null) {
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create()
                val page = document.startPage(pageInfo)

                val canvas = page.canvas
                canvas.drawColor(Color.WHITE)

                drawBitmapOnPage(
                    canvas = canvas,
                    bitmap = bitmap,
                    pageWidth = pageWidth,
                    pageHeight = pageHeight,
                    margin = margin,
                    fitMode = fitMode,
                    quality = quality
                )

                document.finishPage(page)
                bitmap.recycle()
                pageCount++
            }
            onProgress((index + 1).toFloat() / images.size)
        }

        val outputStream = java.io.ByteArrayOutputStream()
        document.writeTo(outputStream)
        return Pair(outputStream.toByteArray(), pageCount)
    } finally {
        document.close()
    }
}
