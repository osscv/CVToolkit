package cv.toolkit.screens

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.navigation.NavController
import cv.toolkit.R
import cv.toolkit.ads.BannerAd
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.zIndex
import kotlin.math.roundToInt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

private data class ImageItem(
    val id: Long,
    val uri: Uri,
    val name: String,
    val thumbnail: Bitmap?,
    val rotation: Int = 0,
    val cropRect: RectF? = null
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
    var nextImageId by remember { mutableLongStateOf(0L) }
    var draggedItemId by remember { mutableStateOf<Long?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val itemHeightPx = with(density) { 84.dp.toPx() }
    var pageSize by remember { mutableStateOf(PageSize.A4) }
    var customWidthPt by remember { mutableStateOf("595") }
    var customHeightPt by remember { mutableStateOf("842") }
    var orientation by remember { mutableStateOf(Orientation.PORTRAIT) }
    var marginPreset by remember { mutableStateOf(MarginPreset.MEDIUM) }
    var fitMode by remember { mutableStateOf(FitMode.FIT) }
    var quality by remember { mutableIntStateOf(85) }
    var isConverting by remember { mutableStateOf(false) }
    var conversionProgress by remember { mutableFloatStateOf(0f) }
    var previewBitmaps by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var pageSizeMenuExpanded by remember { mutableStateOf(false) }
    var showPreview by remember { mutableStateOf(false) }
    var cropDialogImageId by remember { mutableStateOf<Long?>(null) }

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
                        if (thumbnail != null) ImageItem(0L, uri, name, thumbnail) else null
                    }
                }
                val itemsWithIds = newItems.mapIndexed { i, item ->
                    item.copy(id = nextImageId + i)
                }
                nextImageId += newItems.size
                images = images + itemsWithIds
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

    // Regenerate previews when settings change
    LaunchedEffect(images, pageSize, customWidthPt, customHeightPt, orientation, marginPreset, fitMode) {
        if (images.isNotEmpty()) {
            previewBitmaps = withContext(Dispatchers.IO) {
                images.mapNotNull { imageItem ->
                    generatePreview(context, imageItem, pageSize, customWidthPt, customHeightPt, orientation, marginPreset, fitMode)
                }
            }
        } else {
            previewBitmaps = emptyList()
        }
    }

    if (showPreview) {
        BackHandler {
            showPreview = false
        }
    }

    // Crop dialog
    cropDialogImageId?.let { imageId ->
        val imageItem = images.find { it.id == imageId }
        if (imageItem != null) {
            CropDialog(
                context = context,
                imageItem = imageItem,
                onConfirm = { cropRect ->
                    images = images.map {
                        if (it.id == imageId) it.copy(cropRect = cropRect) else it
                    }
                    cropDialogImageId = null
                },
                onReset = {
                    images = images.map {
                        if (it.id == imageId) it.copy(cropRect = null) else it
                    }
                    cropDialogImageId = null
                },
                onDismiss = { cropDialogImageId = null }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(if (showPreview) "Preview" else stringResource(R.string.image_to_pdf_title))
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (showPreview) {
                            showPreview = false
                        } else {
                            navController.popBackStack()
                        }
                    }) {
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
        if (showPreview) {
            // Full-screen preview
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp)
            ) {
                if (previewBitmaps.isNotEmpty()) {
                    val pagerState = rememberPagerState(pageCount = { previewBitmaps.size })
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "${previewBitmaps.size} pages",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            "Page ${pagerState.currentPage + 1} of ${previewBitmaps.size}",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) { page ->
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(horizontal = 4.dp)
                                .background(
                                    MaterialTheme.colorScheme.surfaceVariant,
                                    shape = MaterialTheme.shapes.medium
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = previewBitmaps[page].asImageBitmap(),
                                contentDescription = "Page ${page + 1}",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Progress bar in preview
                    if (isConverting) {
                        LinearProgressIndicator(
                            progress = { conversionProgress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                        )
                    }

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
                                    showPreview = false
                                    savePdfLauncher.launch("images.pdf")
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("Conversion failed: ${e.message}")
                                } finally {
                                    isConverting = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isConverting
                    ) {
                        if (isConverting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(Icons.Filled.Save, null, modifier = Modifier.size(20.dp))
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(if (isConverting) "Converting..." else "Export to PDF")
                    }
                    Spacer(Modifier.height(8.dp))
                    BannerAd(modifier = Modifier.fillMaxWidth())
                }
            }
        } else {
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

                itemsIndexed(images, key = { _, item -> item.id }) { index, item ->
                    val isDragged = item.id == draggedItemId
                    Box(
                        modifier = Modifier
                            .zIndex(if (isDragged) 1f else 0f)
                            .offset { IntOffset(0, if (isDragged) dragOffset.roundToInt() else 0) }
                            .pointerInput(item.id) {
                                detectDragGesturesAfterLongPress(
                                    onDragStart = {
                                        draggedItemId = item.id
                                        dragOffset = 0f
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        dragOffset += dragAmount.y
                                        val currentIndex = images.indexOfFirst { it.id == draggedItemId }
                                        if (currentIndex >= 0) {
                                            if (dragOffset > itemHeightPx / 2 && currentIndex < images.size - 1) {
                                                val mutable = images.toMutableList()
                                                val moved = mutable.removeAt(currentIndex)
                                                mutable.add(currentIndex + 1, moved)
                                                images = mutable
                                                dragOffset -= itemHeightPx
                                            } else if (dragOffset < -itemHeightPx / 2 && currentIndex > 0) {
                                                val mutable = images.toMutableList()
                                                val moved = mutable.removeAt(currentIndex)
                                                mutable.add(currentIndex - 1, moved)
                                                images = mutable
                                                dragOffset += itemHeightPx
                                            }
                                        }
                                    },
                                    onDragEnd = { draggedItemId = null; dragOffset = 0f },
                                    onDragCancel = { draggedItemId = null; dragOffset = 0f }
                                )
                            }
                    ) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = if (isDragged) 8.dp else 0.dp
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Drag handle
                                Icon(
                                    Icons.Filled.DragHandle,
                                    "Long press to reorder",
                                    modifier = Modifier.size(24.dp),
                                    tint = if (isDragged) MaterialTheme.colorScheme.primary
                                           else MaterialTheme.colorScheme.onSurfaceVariant
                                )

                                Spacer(Modifier.width(8.dp))

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
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .graphicsLayer { rotationZ = item.rotation.toFloat() },
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
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            "Page ${index + 1}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        if (item.rotation != 0) {
                                            Text(
                                                " \u00B7 ${item.rotation}\u00B0",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        if (item.cropRect != null) {
                                            Text(
                                                " \u00B7 Cropped",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }

                                // Rotate
                                IconButton(
                                    onClick = {
                                        images = images.map {
                                            if (it.id == item.id) it.copy(rotation = (it.rotation + 90) % 360) else it
                                        }
                                    },
                                    enabled = !isConverting,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.RotateRight,
                                        "Rotate",
                                        modifier = Modifier.size(20.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Crop
                                IconButton(
                                    onClick = { cropDialogImageId = item.id },
                                    enabled = !isConverting,
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        Icons.Filled.Crop,
                                        "Crop",
                                        modifier = Modifier.size(20.dp),
                                        tint = if (item.cropRect != null) MaterialTheme.colorScheme.primary
                                               else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
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
                                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
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

                // Page previews
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            if (previewBitmaps.isNotEmpty()) {
                                val pagerState = rememberPagerState(pageCount = { previewBitmaps.size })
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Page Preview",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    Text(
                                        "Page ${pagerState.currentPage + 1} of ${previewBitmaps.size}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                HorizontalPager(
                                    state = pagerState,
                                    modifier = Modifier.fillMaxWidth()
                                ) { page ->
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(280.dp)
                                            .clip(MaterialTheme.shapes.medium)
                                            .background(MaterialTheme.colorScheme.surfaceVariant),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Image(
                                            bitmap = previewBitmaps[page].asImageBitmap(),
                                            contentDescription = "Page ${page + 1} preview",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Fit
                                        )
                                    }
                                }
                                // Page indicator dots
                                if (previewBitmaps.size > 1) {
                                    Spacer(Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        repeat(previewBitmaps.size) { index ->
                                            Box(
                                                modifier = Modifier
                                                    .padding(horizontal = 3.dp)
                                                    .size(if (index == pagerState.currentPage) 8.dp else 6.dp)
                                                    .background(
                                                        color = if (index == pagerState.currentPage)
                                                            MaterialTheme.colorScheme.primary
                                                        else
                                                            MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                                                        shape = MaterialTheme.shapes.small
                                                    )
                                            )
                                        }
                                    }
                                }
                            } else {
                                Text(
                                    "Page Preview",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.height(8.dp))
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(280.dp)
                                        .clip(MaterialTheme.shapes.medium)
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                    }
                }

                // Preview & Convert button
                item {
                    Button(
                        onClick = { showPreview = true },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = images.isNotEmpty() && !isConverting
                    ) {
                        Icon(Icons.Filled.PictureAsPdf, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Preview & Convert to PDF")
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
        } // end else
    }
}

@Composable
private fun CropDialog(
    context: Context,
    imageItem: ImageItem,
    onConfirm: (RectF) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    val bitmap = remember {
        val bmp = decodeFullBitmap(context, imageItem.uri, 1024) ?: return@remember null
        if (imageItem.rotation != 0) {
            val matrix = Matrix().apply { postRotate(imageItem.rotation.toFloat()) }
            val rotated = Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, matrix, true)
            if (rotated !== bmp) bmp.recycle()
            rotated
        } else bmp
    }

    if (bitmap == null) {
        onDismiss()
        return
    }

    var cropLeft by remember { mutableFloatStateOf(imageItem.cropRect?.left ?: 0f) }
    var cropTop by remember { mutableFloatStateOf(imageItem.cropRect?.top ?: 0f) }
    var cropRight by remember { mutableFloatStateOf(imageItem.cropRect?.right ?: 1f) }
    var cropBottom by remember { mutableFloatStateOf(imageItem.cropRect?.bottom ?: 1f) }
    var activeHandle by remember { mutableIntStateOf(-1) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = MaterialTheme.shapes.large
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Crop Image",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Drag corners to adjust crop area",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))

                var imageDisplayRect by remember { mutableStateOf(android.graphics.Rect(0, 0, 0, 0)) }
                val imageBitmap = remember(bitmap) { bitmap.asImageBitmap() }
                val primaryColor = MaterialTheme.colorScheme.primary
                val overlayColor = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.5f)
                val handleOuterColor = androidx.compose.ui.graphics.Color.White
                val gridColor = androidx.compose.ui.graphics.Color.White.copy(alpha = 0.3f)

                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(MaterialTheme.shapes.medium)
                        .background(androidx.compose.ui.graphics.Color(0xFF1A1A1A))
                ) {
                    androidx.compose.foundation.Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .pointerInput(Unit) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        val imgL = imageDisplayRect.left.toFloat()
                                        val imgT = imageDisplayRect.top.toFloat()
                                        val imgW = imageDisplayRect.width().toFloat()
                                        val imgH = imageDisplayRect.height().toFloat()

                                        val cL = imgL + cropLeft * imgW
                                        val cT = imgT + cropTop * imgH
                                        val cR = imgL + cropRight * imgW
                                        val cB = imgT + cropBottom * imgH

                                        val corners = listOf(
                                            Offset(cL, cT),
                                            Offset(cR, cT),
                                            Offset(cL, cB),
                                            Offset(cR, cB)
                                        )

                                        val closest = corners.withIndex().minByOrNull {
                                            (offset - it.value).getDistance()
                                        }

                                        activeHandle = if (closest != null && (offset - closest.value).getDistance() < 80f) {
                                            closest.index
                                        } else if (offset.x in cL..cR && offset.y in cT..cB) {
                                            4 // move entire box
                                        } else {
                                            -1
                                        }
                                    },
                                    onDrag = { change, dragAmount ->
                                        change.consume()
                                        val imgW = imageDisplayRect.width().toFloat()
                                        val imgH = imageDisplayRect.height().toFloat()
                                        if (imgW <= 0 || imgH <= 0) return@detectDragGestures

                                        val dx = dragAmount.x / imgW
                                        val dy = dragAmount.y / imgH

                                        when (activeHandle) {
                                            0 -> {
                                                cropLeft = (cropLeft + dx).coerceIn(0f, cropRight - 0.05f)
                                                cropTop = (cropTop + dy).coerceIn(0f, cropBottom - 0.05f)
                                            }
                                            1 -> {
                                                cropRight = (cropRight + dx).coerceIn(cropLeft + 0.05f, 1f)
                                                cropTop = (cropTop + dy).coerceIn(0f, cropBottom - 0.05f)
                                            }
                                            2 -> {
                                                cropLeft = (cropLeft + dx).coerceIn(0f, cropRight - 0.05f)
                                                cropBottom = (cropBottom + dy).coerceIn(cropTop + 0.05f, 1f)
                                            }
                                            3 -> {
                                                cropRight = (cropRight + dx).coerceIn(cropLeft + 0.05f, 1f)
                                                cropBottom = (cropBottom + dy).coerceIn(cropTop + 0.05f, 1f)
                                            }
                                            4 -> {
                                                val w = cropRight - cropLeft
                                                val h = cropBottom - cropTop
                                                val newLeft = (cropLeft + dx).coerceIn(0f, 1f - w)
                                                val newTop = (cropTop + dy).coerceIn(0f, 1f - h)
                                                cropLeft = newLeft
                                                cropTop = newTop
                                                cropRight = newLeft + w
                                                cropBottom = newTop + h
                                            }
                                        }
                                    },
                                    onDragEnd = { activeHandle = -1 },
                                    onDragCancel = { activeHandle = -1 }
                                )
                            }
                    ) {
                        // Calculate image display rect (fit within canvas)
                        val scaleX = size.width / bitmap.width
                        val scaleY = size.height / bitmap.height
                        val scale = minOf(scaleX, scaleY)
                        val imgW = bitmap.width * scale
                        val imgH = bitmap.height * scale
                        val imgL = (size.width - imgW) / 2f
                        val imgT = (size.height - imgH) / 2f

                        imageDisplayRect = android.graphics.Rect(
                            imgL.toInt(), imgT.toInt(),
                            (imgL + imgW).toInt(), (imgT + imgH).toInt()
                        )

                        // Draw image
                        drawImage(
                            image = imageBitmap,
                            dstOffset = androidx.compose.ui.unit.IntOffset(imgL.toInt(), imgT.toInt()),
                            dstSize = IntSize(imgW.toInt(), imgH.toInt())
                        )

                        // Crop box pixel coordinates
                        val cL = imgL + cropLeft * imgW
                        val cT = imgT + cropTop * imgH
                        val cR = imgL + cropRight * imgW
                        val cB = imgT + cropBottom * imgH

                        // Dark overlay outside crop
                        drawRect(overlayColor, Offset(imgL, imgT), Size(imgW, cT - imgT))
                        drawRect(overlayColor, Offset(imgL, cB), Size(imgW, imgT + imgH - cB))
                        drawRect(overlayColor, Offset(imgL, cT), Size(cL - imgL, cB - cT))
                        drawRect(overlayColor, Offset(cR, cT), Size(imgL + imgW - cR, cB - cT))

                        // Crop border
                        drawRect(
                            color = handleOuterColor,
                            topLeft = Offset(cL, cT),
                            size = Size(cR - cL, cB - cT),
                            style = Stroke(width = 2f)
                        )

                        // Rule-of-thirds grid
                        val thirdW = (cR - cL) / 3f
                        val thirdH = (cB - cT) / 3f
                        for (i in 1..2) {
                            drawLine(gridColor, Offset(cL + thirdW * i, cT), Offset(cL + thirdW * i, cB), strokeWidth = 1f)
                            drawLine(gridColor, Offset(cL, cT + thirdH * i), Offset(cR, cT + thirdH * i), strokeWidth = 1f)
                        }

                        // Corner handles
                        val handleRadius = 8f
                        listOf(
                            Offset(cL, cT), Offset(cR, cT),
                            Offset(cL, cB), Offset(cR, cB)
                        ).forEach { corner ->
                            drawCircle(handleOuterColor, handleRadius, corner)
                            drawCircle(primaryColor, handleRadius - 2f, corner)
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onReset,
                        modifier = Modifier.weight(1f)
                    ) { Text("Reset") }
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) { Text("Cancel") }
                    Button(
                        onClick = {
                            if (cropLeft == 0f && cropTop == 0f && cropRight == 1f && cropBottom == 1f) {
                                onReset()
                            } else {
                                onConfirm(RectF(cropLeft, cropTop, cropRight, cropBottom))
                            }
                        },
                        modifier = Modifier.weight(1f)
                    ) { Text("Apply") }
                }
            }
        }
    }
}

private fun applyTransforms(bitmap: Bitmap, rotation: Int, cropRect: RectF?): Bitmap {
    var result = bitmap

    // Apply rotation
    if (rotation != 0) {
        val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
        val rotated = Bitmap.createBitmap(result, 0, 0, result.width, result.height, matrix, true)
        if (rotated !== result) result.recycle()
        result = rotated
    }

    // Apply crop
    if (cropRect != null) {
        val x = (cropRect.left * result.width).toInt().coerceIn(0, result.width - 1)
        val y = (cropRect.top * result.height).toInt().coerceIn(0, result.height - 1)
        val w = ((cropRect.right - cropRect.left) * result.width).toInt()
            .coerceAtLeast(1).coerceAtMost(result.width - x)
        val h = ((cropRect.bottom - cropRect.top) * result.height).toInt()
            .coerceAtLeast(1).coerceAtMost(result.height - y)
        val cropped = Bitmap.createBitmap(result, x, y, w, h)
        if (cropped !== result) result.recycle()
        result = cropped
    }

    return result
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

    val rawBitmap = decodeFullBitmap(context, imageItem.uri, 1024) ?: return null
    val bitmap = applyTransforms(rawBitmap, imageItem.rotation, imageItem.cropRect)

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
            val rawBitmap = decodeFullBitmap(context, imageItem.uri, 4096)
            val bitmap = rawBitmap?.let { applyTransforms(it, imageItem.rotation, imageItem.cropRect) }
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
