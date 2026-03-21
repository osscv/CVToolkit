package cv.toolkit.screens

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.NavigateBefore
import androidx.compose.material.icons.automirrored.filled.NavigateNext
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cv.toolkit.R
import cv.toolkit.ads.BannerAd
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private data class PdfInfo(
    val fileName: String,
    val pageCount: Int,
    val fileSize: Long,
    val uri: Uri
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfViewerScreen(navController: NavController) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var pdfRenderer by remember { mutableStateOf<PdfRenderer?>(null) }
    var pdfInfo by remember { mutableStateOf<PdfInfo?>(null) }
    var currentPage by remember { mutableIntStateOf(0) }
    var pageCount by remember { mutableIntStateOf(0) }
    var currentPageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var showJumpToPageDialog by remember { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }

    // Zoom and pan state
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Page cache
    val pageCache = remember { mutableStateMapOf<Int, Bitmap>() }
    val thumbnailCache = remember { mutableStateMapOf<Int, Bitmap>() }

    // Screen width for render resolution
    val screenWidthPx = with(density) {
        (LocalContext.current.resources.displayMetrics.widthPixels)
    }

    // Render a page at full resolution
    suspend fun renderPage(renderer: PdfRenderer, pageIndex: Int): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                val page = renderer.openPage(pageIndex)
                val renderWidth = (screenWidthPx * 2).coerceAtLeast(1080)
                val aspectRatio = page.height.toFloat() / page.width.toFloat()
                val renderHeight = (renderWidth * aspectRatio).toInt()
                val bitmap = Bitmap.createBitmap(renderWidth, renderHeight, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(android.graphics.Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                bitmap
            } catch (e: Exception) {
                null
            }
        }
    }

    // Render a thumbnail
    suspend fun renderThumbnail(renderer: PdfRenderer, pageIndex: Int): Bitmap? {
        return withContext(Dispatchers.IO) {
            try {
                val page = renderer.openPage(pageIndex)
                val thumbWidth = 150
                val aspectRatio = page.height.toFloat() / page.width.toFloat()
                val thumbHeight = (thumbWidth * aspectRatio).toInt()
                val bitmap = Bitmap.createBitmap(thumbWidth, thumbHeight, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(android.graphics.Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()
                bitmap
            } catch (e: Exception) {
                null
            }
        }
    }

    // Navigate to a page
    fun navigateToPage(pageIndex: Int) {
        if (pageIndex in 0 until pageCount) {
            currentPage = pageIndex
            scale = 1f
            offset = Offset.Zero
        }
    }

    // Load current page bitmap
    LaunchedEffect(currentPage, pdfRenderer) {
        val renderer = pdfRenderer ?: return@LaunchedEffect
        isLoading = true
        val cached = pageCache[currentPage]
        if (cached != null) {
            currentPageBitmap = cached
            isLoading = false
        } else {
            val bitmap = renderPage(renderer, currentPage)
            if (bitmap != null) {
                pageCache[currentPage] = bitmap
                currentPageBitmap = bitmap
            }
            isLoading = false
        }
    }

    // Load thumbnails
    LaunchedEffect(pdfRenderer, pageCount) {
        val renderer = pdfRenderer ?: return@LaunchedEffect
        for (i in 0 until pageCount) {
            if (thumbnailCache[i] == null) {
                val thumb = renderThumbnail(renderer, i)
                if (thumb != null) {
                    thumbnailCache[i] = thumb
                }
            }
        }
    }

    // Clean up PdfRenderer when leaving the screen
    DisposableEffect(Unit) {
        onDispose {
            pageCache.values.forEach { it.recycle() }
            thumbnailCache.values.forEach { it.recycle() }
            pageCache.clear()
            thumbnailCache.clear()
            try {
                pdfRenderer?.close()
            } catch (_: Exception) {
            }
        }
    }

    // File picker
    val pdfPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            // Clear previous state
            pageCache.values.forEach { bmp -> bmp.recycle() }
            thumbnailCache.values.forEach { bmp -> bmp.recycle() }
            pageCache.clear()
            thumbnailCache.clear()
            currentPageBitmap = null
            currentPage = 0
            scale = 1f
            offset = Offset.Zero

            try {
                pdfRenderer?.close()
            } catch (_: Exception) {
            }

            try {
                val fileDescriptor = context.contentResolver.openFileDescriptor(it, "r")
                if (fileDescriptor != null) {
                    val renderer = PdfRenderer(fileDescriptor)
                    pdfRenderer = renderer
                    pageCount = renderer.pageCount

                    // Get file info
                    var fileName = "Unknown"
                    var fileSize = 0L
                    context.contentResolver.query(it, null, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                            if (nameIndex >= 0) fileName = cursor.getString(nameIndex) ?: "Unknown"
                            if (sizeIndex >= 0) fileSize = cursor.getLong(sizeIndex)
                        }
                    }

                    pdfInfo = PdfInfo(
                        fileName = fileName,
                        pageCount = renderer.pageCount,
                        fileSize = fileSize,
                        uri = it
                    )
                }
            } catch (e: Exception) {
                pdfRenderer = null
                pdfInfo = null
                pageCount = 0
            }
        }
    }

    // Jump to page dialog
    if (showJumpToPageDialog) {
        var jumpPageText by remember { mutableStateOf("") }
        var jumpError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showJumpToPageDialog = false },
            title = { Text("Jump to Page") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Enter page number (1 - $pageCount)",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    OutlinedTextField(
                        value = jumpPageText,
                        onValueChange = {
                            jumpPageText = it
                            jumpError = null
                        },
                        label = { Text("Page number") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        isError = jumpError != null,
                        supportingText = jumpError?.let { { Text(it) } },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val pageNum = jumpPageText.toIntOrNull()
                    if (pageNum != null && pageNum in 1..pageCount) {
                        navigateToPage(pageNum - 1)
                        showJumpToPageDialog = false
                    } else {
                        jumpError = "Enter a number between 1 and $pageCount"
                    }
                }) {
                    Text("Go")
                }
            },
            dismissButton = {
                TextButton(onClick = { showJumpToPageDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // PDF info dialog
    if (showInfoDialog && pdfInfo != null) {
        AlertDialog(
            onDismissRequest = { showInfoDialog = false },
            title = { Text("PDF Information") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PdfInfoRow("Filename", pdfInfo!!.fileName)
                    PdfInfoRow("Pages", pdfInfo!!.pageCount.toString())
                    PdfInfoRow("File Size", formatPdfFileSize(pdfInfo!!.fileSize))
                }
            },
            confirmButton = {
                TextButton(onClick = { showInfoDialog = false }) {
                    Text("Close")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.pdf_viewer_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (pdfInfo != null) {
                        // Share button
                        IconButton(onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/pdf"
                                putExtra(Intent.EXTRA_STREAM, pdfInfo!!.uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(
                                Intent.createChooser(shareIntent, "Share PDF")
                            )
                        }) {
                            Icon(Icons.Filled.Share, "Share PDF")
                        }

                        // Info button
                        IconButton(onClick = { showInfoDialog = true }) {
                            Icon(Icons.Filled.Info, "PDF Info")
                        }
                    }

                    // Open PDF button in toolbar
                    IconButton(onClick = { pdfPickerLauncher.launch("application/pdf") }) {
                        Icon(Icons.Filled.FileOpen, "Open PDF")
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
            if (pdfRenderer == null) {
                // Empty state - no PDF loaded
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
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
                                "Open a PDF File",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "View and navigate PDF documents\nwith zoom and page thumbnails",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(24.dp))
                            Button(
                                onClick = { pdfPickerLauncher.launch("application/pdf") }
                            ) {
                                Icon(
                                    Icons.Filled.FolderOpen,
                                    null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Open PDF")
                            }
                        }
                    }
                }
            } else {
                // PDF content area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clipToBounds()
                        .background(Color(0xFF424242))
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(0.5f, 5f)
                                offset = Offset(
                                    x = offset.x + pan.x,
                                    y = offset.y + pan.y
                                )
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = Color.White
                        )
                    } else {
                        currentPageBitmap?.let { bitmap ->
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = "PDF Page ${currentPage + 1}",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer(
                                        scaleX = scale,
                                        scaleY = scale,
                                        translationX = offset.x,
                                        translationY = offset.y
                                    ),
                                contentScale = ContentScale.Fit
                            )
                        }
                    }

                    // Reset zoom button
                    if (scale != 1f || offset != Offset.Zero) {
                        FilledTonalIconButton(
                            onClick = {
                                scale = 1f
                                offset = Offset.Zero
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .size(36.dp)
                        ) {
                            Icon(
                                Icons.Filled.CenterFocusWeak,
                                contentDescription = "Reset zoom",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Page navigation controls
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { navigateToPage(currentPage - 1) },
                            enabled = currentPage > 0
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.NavigateBefore,
                                "Previous page"
                            )
                        }

                        TextButton(onClick = { showJumpToPageDialog = true }) {
                            Text(
                                "Page ${currentPage + 1} of $pageCount",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        IconButton(
                            onClick = { navigateToPage(currentPage + 1) },
                            enabled = currentPage < pageCount - 1
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.NavigateNext,
                                "Next page"
                            )
                        }
                    }
                }

                // Thumbnail strip
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 1.dp
                ) {
                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp)
                    ) {
                        itemsIndexed(
                            items = (0 until pageCount).toList(),
                            key = { _, index -> index }
                        ) { _, pageIndex ->
                            val isSelected = pageIndex == currentPage
                            Card(
                                onClick = { navigateToPage(pageIndex) },
                                modifier = Modifier
                                    .width(60.dp)
                                    .height(80.dp),
                                border = if (isSelected) BorderStroke(
                                    2.dp,
                                    MaterialTheme.colorScheme.primary
                                ) else null,
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val thumbnail = thumbnailCache[pageIndex]
                                    if (thumbnail != null) {
                                        Image(
                                            bitmap = thumbnail.asImageBitmap(),
                                            contentDescription = "Page ${pageIndex + 1}",
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Fit
                                        )
                                    } else {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            strokeWidth = 2.dp
                                        )
                                    }

                                    // Page number overlay
                                    Surface(
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .fillMaxWidth(),
                                        color = Color.Black.copy(alpha = 0.6f)
                                    ) {
                                        Text(
                                            text = "${pageIndex + 1}",
                                            modifier = Modifier.padding(2.dp),
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelSmall,
                                            textAlign = TextAlign.Center,
                                            maxLines = 1,
                                            overflow = TextOverflow.Clip
                                        )
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

@Composable
private fun PdfInfoRow(label: String, value: String) {
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
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.widthIn(max = 200.dp)
        )
    }
}

private fun formatPdfFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format("%.2f KB", bytes / 1024.0)
        bytes < 1024 * 1024 * 1024 -> String.format("%.2f MB", bytes / (1024.0 * 1024))
        else -> String.format("%.2f GB", bytes / (1024.0 * 1024 * 1024))
    }
}
