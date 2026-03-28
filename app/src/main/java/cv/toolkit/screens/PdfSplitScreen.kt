package cv.toolkit.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfDocument
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cv.toolkit.ads.BannerAd
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private enum class SplitMode(val label: String) {
    INDIVIDUAL("Each Page"),
    RANGE("Page Range"),
    SELECT("Select Pages")
}

private data class SplitPdfInfo(
    val uri: Uri,
    val name: String,
    val pageCount: Int,
    val size: Long,
    val thumbnails: List<Bitmap?>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfSplitScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var pdfInfo by remember { mutableStateOf<SplitPdfInfo?>(null) }
    var splitMode by remember { mutableStateOf(SplitMode.RANGE) }
    var rangeStart by remember { mutableStateOf("1") }
    var rangeEnd by remember { mutableStateOf("") }
    var selectedPages by remember { mutableStateOf(setOf<Int>()) }
    var isProcessing by remember { mutableStateOf(false) }
    var processProgress by remember { mutableFloatStateOf(0f) }
    var pendingSplitBytes by remember { mutableStateOf<ByteArray?>(null) }
    var splitPageCount by remember { mutableIntStateOf(0) }

    val pdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                isProcessing = true
                try {
                    val info = withContext(Dispatchers.IO) {
                        getSplitPdfInfo(context, it)
                    }
                    pdfInfo = info
                    rangeEnd = info.pageCount.toString()
                    selectedPages = (0 until info.pageCount).toSet()
                    pendingSplitBytes = null
                    splitPageCount = 0
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Failed to open PDF: ${e.message}")
                } finally {
                    isProcessing = false
                }
            }
        }
    }

    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        uri?.let { destUri ->
            val bytes = pendingSplitBytes
            if (bytes != null) {
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            context.contentResolver.openOutputStream(destUri)?.use { os ->
                                os.write(bytes)
                            }
                        }
                        snackbarHostState.showSnackbar("Split PDF saved ($splitPageCount pages)")
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar("Failed to save: ${e.message}")
                    }
                }
            }
        }
    }

    fun getPageIndices(): List<Int> {
        val info = pdfInfo ?: return emptyList()
        return when (splitMode) {
            SplitMode.INDIVIDUAL -> (0 until info.pageCount).toList()
            SplitMode.RANGE -> {
                val start = (rangeStart.toIntOrNull() ?: 1).coerceIn(1, info.pageCount) - 1
                val end = (rangeEnd.toIntOrNull() ?: info.pageCount).coerceIn(1, info.pageCount) - 1
                (start..end).toList()
            }
            SplitMode.SELECT -> selectedPages.sorted()
        }
    }

    fun splitPdf() {
        val info = pdfInfo ?: return
        val pageIndices = getPageIndices()
        if (pageIndices.isEmpty()) {
            scope.launch { snackbarHostState.showSnackbar("No pages selected") }
            return
        }

        scope.launch {
            isProcessing = true
            processProgress = 0f
            try {
                val result = withContext(Dispatchers.IO) {
                    splitPdfPages(context, info.uri, pageIndices) { progress ->
                        processProgress = progress
                    }
                }
                pendingSplitBytes = result
                splitPageCount = pageIndices.size
                val pdfName = info.name.removeSuffix(".pdf")
                val suffix = when (splitMode) {
                    SplitMode.RANGE -> "_p${rangeStart}-${rangeEnd}"
                    SplitMode.SELECT -> "_${pageIndices.size}pages"
                    SplitMode.INDIVIDUAL -> "_all"
                }
                saveLauncher.launch("${pdfName}${suffix}.pdf")
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Split failed: ${e.message}")
            } finally {
                isProcessing = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PDF Split") },
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
                                    Icons.Filled.ContentCut,
                                    contentDescription = null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    "Split PDF Pages",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Extract specific pages or ranges from a PDF file",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                pdfInfo?.let { info ->
                    // File info
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
                                        "${info.pageCount} page${if (info.pageCount != 1) "s" else ""} \u2022 ${formatSplitFileSize(info.size)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Page thumbnails
                    if (info.thumbnails.isNotEmpty()) {
                        item {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                itemsIndexed(info.thumbnails) { index, thumbnail ->
                                    val isSelected = when (splitMode) {
                                        SplitMode.INDIVIDUAL -> true
                                        SplitMode.RANGE -> {
                                            val start = (rangeStart.toIntOrNull() ?: 1) - 1
                                            val end = (rangeEnd.toIntOrNull() ?: info.pageCount) - 1
                                            index in start..end
                                        }
                                        SplitMode.SELECT -> index in selectedPages
                                    }

                                    Card(
                                        modifier = Modifier
                                            .width(80.dp)
                                            .then(
                                                if (splitMode == SplitMode.SELECT) {
                                                    Modifier.toggleable(
                                                        value = isSelected,
                                                        role = Role.Checkbox,
                                                        onValueChange = {
                                                            selectedPages = if (isSelected) {
                                                                selectedPages - index
                                                            } else {
                                                                selectedPages + index
                                                            }
                                                        }
                                                    )
                                                } else Modifier
                                            ),
                                        colors = CardDefaults.cardColors(
                                            containerColor = if (isSelected)
                                                MaterialTheme.colorScheme.primaryContainer
                                            else
                                                MaterialTheme.colorScheme.surfaceVariant
                                        ),
                                        elevation = CardDefaults.cardElevation(
                                            defaultElevation = if (isSelected) 4.dp else 0.dp
                                        )
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(4.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .aspectRatio(0.7f)
                                                    .background(
                                                        MaterialTheme.colorScheme.surface,
                                                        shape = MaterialTheme.shapes.small
                                                    ),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                if (thumbnail != null) {
                                                    Image(
                                                        bitmap = thumbnail.asImageBitmap(),
                                                        contentDescription = "Page ${index + 1}",
                                                        modifier = Modifier.fillMaxSize(),
                                                        contentScale = ContentScale.Fit
                                                    )
                                                } else {
                                                    Icon(
                                                        Icons.Filled.Description,
                                                        contentDescription = null,
                                                        modifier = Modifier.size(24.dp),
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                            Text(
                                                "${index + 1}",
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Split mode selector
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Split Mode",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    SplitMode.entries.forEach { mode ->
                                        FilterChip(
                                            selected = splitMode == mode,
                                            onClick = { splitMode = mode },
                                            label = { Text(mode.label, style = MaterialTheme.typography.labelSmall) },
                                            modifier = Modifier.weight(1f),
                                            enabled = !isProcessing
                                        )
                                    }
                                }

                                // Range inputs
                                if (splitMode == SplitMode.RANGE) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = rangeStart,
                                            onValueChange = { rangeStart = it.filter { c -> c.isDigit() } },
                                            label = { Text("From") },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true,
                                            enabled = !isProcessing
                                        )
                                        Text("to", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        OutlinedTextField(
                                            value = rangeEnd,
                                            onValueChange = { rangeEnd = it.filter { c -> c.isDigit() } },
                                            label = { Text("To") },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true,
                                            enabled = !isProcessing
                                        )
                                    }
                                    Text(
                                        "Pages 1 to ${info.pageCount}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(top = 4.dp)
                                    )
                                }

                                // Select pages info
                                if (splitMode == SplitMode.SELECT) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "${selectedPages.size} of ${info.pageCount} pages selected",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Row {
                                            TextButton(onClick = {
                                                selectedPages = (0 until info.pageCount).toSet()
                                            }) { Text("All") }
                                            TextButton(onClick = {
                                                selectedPages = emptySet()
                                            }) { Text("None") }
                                        }
                                    }
                                }

                                if (splitMode == SplitMode.INDIVIDUAL) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "All ${info.pageCount} pages will be included in the output PDF",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // Info note
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
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Output preserves visual layout. Text may not be searchable.",
                                    style = MaterialTheme.typography.bodySmall
                                )
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
                                Text("Splitting PDF...", style = MaterialTheme.typography.bodyMedium)
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

                    // Split button
                    item {
                        val pageCount = getPageIndices().size
                        Button(
                            onClick = { splitPdf() },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isProcessing && pageCount > 0
                        ) {
                            Icon(Icons.Filled.ContentCut, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Split ($pageCount page${if (pageCount != 1) "s" else ""})")
                        }
                    }
                }
            }
            BannerAd(modifier = Modifier.fillMaxWidth())
        }
    }
}

private fun getSplitPdfInfo(context: Context, uri: Uri): SplitPdfInfo {
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

    val thumbnails = mutableListOf<Bitmap?>()
    for (i in 0 until pageCount) {
        try {
            val page = renderer.openPage(i)
            val thumbWidth = 100
            val thumbHeight = (thumbWidth * page.height.toFloat() / page.width.toFloat()).toInt()
            val bitmap = Bitmap.createBitmap(thumbWidth, thumbHeight, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(android.graphics.Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            thumbnails.add(bitmap)
        } catch (_: Exception) {
            thumbnails.add(null)
        }
    }

    renderer.close()
    pfd.close()

    return SplitPdfInfo(uri, name, pageCount, size, thumbnails)
}

private fun splitPdfPages(
    context: Context,
    sourceUri: Uri,
    pageIndices: List<Int>,
    onProgress: (Float) -> Unit
): ByteArray {
    val pfd = context.contentResolver.openFileDescriptor(sourceUri, "r")
        ?: throw Exception("Cannot open file")
    val renderer = PdfRenderer(pfd)
    val outputDocument = PdfDocument()

    try {
        pageIndices.forEachIndexed { idx, pageIndex ->
            if (pageIndex < renderer.pageCount) {
                val sourcePage = renderer.openPage(pageIndex)
                val pageWidth = sourcePage.width
                val pageHeight = sourcePage.height

                val bitmap = Bitmap.createBitmap(pageWidth, pageHeight, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(android.graphics.Color.WHITE)
                sourcePage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                sourcePage.close()

                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, idx).create()
                val outputPage = outputDocument.startPage(pageInfo)
                outputPage.canvas.drawBitmap(bitmap, 0f, 0f, null)
                outputDocument.finishPage(outputPage)
                bitmap.recycle()
            }
            onProgress((idx + 1).toFloat() / pageIndices.size)
        }

        val baos = java.io.ByteArrayOutputStream()
        outputDocument.writeTo(baos)
        return baos.toByteArray()
    } finally {
        outputDocument.close()
        renderer.close()
        pfd.close()
    }
}

private fun formatSplitFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
        bytes < 1024L * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024))
        else -> String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024))
    }
}
