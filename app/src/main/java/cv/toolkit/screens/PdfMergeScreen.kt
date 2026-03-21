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
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cv.toolkit.R
import cv.toolkit.ads.BannerAd
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class PdfFileInfo(
    val uri: Uri,
    val name: String,
    val pageCount: Int,
    val size: Long,
    val thumbnail: Bitmap?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfMergeScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var pdfFiles by remember { mutableStateOf<List<PdfFileInfo>>(emptyList()) }
    var isMerging by remember { mutableStateOf(false) }
    var mergeProgress by remember { mutableFloatStateOf(0f) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var pendingOutputUri by remember { mutableStateOf<Uri?>(null) }

    val pdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                try {
                    val fileInfo = withContext(Dispatchers.IO) {
                        getPdfFileInfo(context, it)
                    }
                    if (fileInfo != null) {
                        pdfFiles = pdfFiles + fileInfo
                        errorMessage = null
                    } else {
                        errorMessage = "Failed to read PDF file."
                    }
                } catch (e: Exception) {
                    errorMessage = "Error adding PDF: ${e.message}"
                }
            }
        }
    }

    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        uri?.let { outputUri ->
            isMerging = true
            mergeProgress = 0f
            errorMessage = null
            successMessage = null
            scope.launch {
                try {
                    val success = withContext(Dispatchers.IO) {
                        mergePdfs(context, pdfFiles, outputUri) { progress ->
                            mergeProgress = progress
                        }
                    }
                    if (success) {
                        val totalPages = pdfFiles.sumOf { it.pageCount }
                        successMessage = "Merge complete! $totalPages pages merged successfully."
                    } else {
                        errorMessage = "Failed to merge PDFs."
                    }
                } catch (e: Exception) {
                    errorMessage = "Merge error: ${e.message}"
                } finally {
                    isMerging = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.pdf_merge_title)) },
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
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Add PDF button
                item {
                    Button(
                        onClick = { pdfPicker.launch("application/pdf") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isMerging
                    ) {
                        Icon(Icons.Filled.Add, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Add PDF File")
                    }
                }

                // Info note about merge quality
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
                                "Merged PDF preserves visual layout. Text may not be searchable.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                // Summary card
                if (pdfFiles.isNotEmpty()) {
                    item {
                        val totalPages = pdfFiles.sumOf { it.pageCount }
                        val totalSize = pdfFiles.sumOf { it.size }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Merge Summary",
                                    style = MaterialTheme.typography.titleMedium,
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
                                            "${pdfFiles.size}",
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            "Files",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            "$totalPages",
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            "Total Pages",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            formatPdfFileSize(totalSize),
                                            style = MaterialTheme.typography.headlineSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Text(
                                            "Est. Size",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Progress indicator
                if (isMerging) {
                    item {
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
                                    "Merging PDFs...",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Spacer(Modifier.height(12.dp))
                                LinearProgressIndicator(
                                    progress = { mergeProgress },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "${(mergeProgress * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Error message
                if (errorMessage != null) {
                    item {
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
                }

                // Success message
                if (successMessage != null) {
                    item {
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
                }

                // PDF file list
                itemsIndexed(pdfFiles, key = { index, item -> "${item.uri}_$index" }) { index, pdfFile ->
                    PdfFileCard(
                        pdfFile = pdfFile,
                        index = index,
                        totalCount = pdfFiles.size,
                        enabled = !isMerging,
                        onMoveUp = {
                            if (index > 0) {
                                val mutable = pdfFiles.toMutableList()
                                val item = mutable.removeAt(index)
                                mutable.add(index - 1, item)
                                pdfFiles = mutable
                            }
                        },
                        onMoveDown = {
                            if (index < pdfFiles.size - 1) {
                                val mutable = pdfFiles.toMutableList()
                                val item = mutable.removeAt(index)
                                mutable.add(index + 1, item)
                                pdfFiles = mutable
                            }
                        },
                        onRemove = {
                            pdfFiles = pdfFiles.toMutableList().also { it.removeAt(index) }
                            successMessage = null
                        }
                    )
                }

                // Empty state
                if (pdfFiles.isEmpty() && !isMerging) {
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
                                    "No PDFs Added",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Add two or more PDF files to merge them\ninto a single document.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                // Merge button
                if (pdfFiles.size >= 2) {
                    item {
                        Button(
                            onClick = {
                                successMessage = null
                                errorMessage = null
                                saveLauncher.launch("merged.pdf")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isMerging,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Icon(Icons.Filled.MergeType, null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Merge ${pdfFiles.size} PDFs")
                        }
                    }
                }
            }

            BannerAd(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun PdfFileCard(
    pdfFile: PdfFileInfo,
    index: Int,
    totalCount: Int,
    enabled: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .size(60.dp)
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant,
                        shape = MaterialTheme.shapes.small
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (pdfFile.thumbnail != null) {
                    Image(
                        bitmap = pdfFile.thumbnail.asImageBitmap(),
                        contentDescription = "PDF thumbnail",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        Icons.Filled.PictureAsPdf,
                        null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            // File info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    pdfFile.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "${pdfFile.pageCount} page${if (pdfFile.pageCount != 1) "s" else ""} \u2022 ${formatPdfFileSize(pdfFile.size)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "#${index + 1} of $totalCount",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Reorder buttons
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconButton(
                    onClick = onMoveUp,
                    enabled = enabled && index > 0,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Filled.KeyboardArrowUp,
                        "Move up",
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = onMoveDown,
                    enabled = enabled && index < totalCount - 1,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Filled.KeyboardArrowDown,
                        "Move down",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Remove button
            IconButton(
                onClick = onRemove,
                enabled = enabled,
                modifier = Modifier.size(32.dp)
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

private fun getPdfFileInfo(context: Context, uri: Uri): PdfFileInfo? {
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

        // Render first page thumbnail
        val thumbnail = if (pageCount > 0) {
            val page = renderer.openPage(0)
            val thumbWidth = 120
            val thumbHeight = (thumbWidth * page.height.toFloat() / page.width.toFloat()).toInt()
            val bitmap = Bitmap.createBitmap(thumbWidth, thumbHeight, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(android.graphics.Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()
            bitmap
        } else {
            null
        }

        renderer.close()
        pfd.close()

        PdfFileInfo(
            uri = uri,
            name = name,
            pageCount = pageCount,
            size = size,
            thumbnail = thumbnail
        )
    } catch (e: Exception) {
        null
    }
}

private fun mergePdfs(
    context: Context,
    files: List<PdfFileInfo>,
    outputUri: Uri,
    onProgress: (Float) -> Unit
): Boolean {
    if (files.size < 2) return false

    val totalPages = files.sumOf { it.pageCount }
    if (totalPages == 0) return false

    var pagesProcessed = 0
    val outputDocument = PdfDocument()

    try {
        var globalPageIndex = 0

        for (fileInfo in files) {
            val pfd = context.contentResolver.openFileDescriptor(fileInfo.uri, "r") ?: continue
            val renderer = PdfRenderer(pfd)

            for (pageIndex in 0 until renderer.pageCount) {
                val sourcePage = renderer.openPage(pageIndex)

                // Use source page dimensions (in points, 1/72 inch)
                val pageWidth = sourcePage.width
                val pageHeight = sourcePage.height

                // Render source page to bitmap at native resolution
                val bitmap = Bitmap.createBitmap(pageWidth, pageHeight, Bitmap.Config.ARGB_8888)
                bitmap.eraseColor(android.graphics.Color.WHITE)
                sourcePage.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                sourcePage.close()

                // Create new page in output document
                val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, globalPageIndex).create()
                val outputPage = outputDocument.startPage(pageInfo)

                // Draw the bitmap onto the new page
                val canvas = outputPage.canvas
                canvas.drawBitmap(bitmap, 0f, 0f, null)

                outputDocument.finishPage(outputPage)
                bitmap.recycle()

                globalPageIndex++
                pagesProcessed++
                onProgress(pagesProcessed.toFloat() / totalPages)
            }

            renderer.close()
            pfd.close()
        }

        // Write to output URI
        context.contentResolver.openOutputStream(outputUri)?.use { outputStream ->
            outputDocument.writeTo(outputStream)
        } ?: return false

        return true
    } catch (e: Exception) {
        return false
    } finally {
        outputDocument.close()
    }
}

private fun formatPdfFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
        bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024))
        else -> String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024))
    }
}
