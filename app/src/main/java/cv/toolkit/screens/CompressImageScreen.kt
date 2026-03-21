package cv.toolkit.screens

import android.content.Context
import android.graphics.*
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import java.io.ByteArrayOutputStream
import kotlin.math.roundToInt

// ── Data models ──────────────────────────────────────────────────────────────

private enum class ImageOutputFormat(val label: String, val extension: String, val mimeType: String) {
    JPEG("JPEG", "jpg", "image/jpeg"),
    PNG("PNG", "png", "image/png"),
    WEBP("WebP", "webp", "image/webp")
}

private enum class ResizeMode(val label: String) {
    ORIGINAL("Keep Original"),
    PERCENTAGE("Percentage"),
    CUSTOM("Custom Dimensions"),
    MAX_DIMENSION("Max Dimension")
}

private enum class CompareMode(val label: String) {
    SIDE_BY_SIDE("Side by Side"),
    TOGGLE("Toggle")
}

private data class CompressImageInfo(
    val uri: Uri,
    val name: String,
    val width: Int,
    val height: Int,
    val format: String,
    val sizeBytes: Long,
    val bitmap: Bitmap
)

private data class CompressedResult(
    val bytes: ByteArray,
    val bitmap: Bitmap,
    val width: Int,
    val height: Int,
    val sizeBytes: Int
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CompressedResult) return false
        return bytes.contentEquals(other.bytes) && width == other.width && height == other.height && sizeBytes == other.sizeBytes
    }

    override fun hashCode(): Int {
        var result = bytes.contentHashCode()
        result = 31 * result + width
        result = 31 * result + height
        result = 31 * result + sizeBytes
        return result
    }
}

// ── Main screen ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CompressImageScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Image state
    var originalImage by remember { mutableStateOf<CompressImageInfo?>(null) }
    var compressedResult by remember { mutableStateOf<CompressedResult?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    // Batch mode
    var batchMode by remember { mutableStateOf(false) }
    var batchImages by remember { mutableStateOf(listOf<CompressImageInfo>()) }
    var batchResults by remember { mutableStateOf(listOf<CompressedResult>()) }
    var isBatchProcessing by remember { mutableStateOf(false) }
    var batchProgress by remember { mutableFloatStateOf(0f) }

    // Compression settings
    var outputFormat by remember { mutableStateOf(ImageOutputFormat.JPEG) }
    var quality by remember { mutableIntStateOf(80) }
    var resizeMode by remember { mutableStateOf(ResizeMode.ORIGINAL) }
    var resizePercentage by remember { mutableIntStateOf(50) }
    var customWidth by remember { mutableStateOf("") }
    var customHeight by remember { mutableStateOf("") }
    var lockAspectRatio by remember { mutableStateOf(true) }
    var maxDimension by remember { mutableStateOf("1920") }

    // Compare
    var compareMode by remember { mutableStateOf(CompareMode.SIDE_BY_SIDE) }
    var showOriginalInToggle by remember { mutableStateOf(true) }

    // Pending save for batch
    var pendingBatchBytes by remember { mutableStateOf<List<Pair<String, ByteArray>>?>(null) }

    // Single image picker
    val singleImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                isProcessing = true
                try {
                    val info = withContext(Dispatchers.IO) {
                        loadImageInfo(context, it)
                    }
                    originalImage = info
                    compressedResult = null
                    // Auto-set custom dimensions from original
                    customWidth = info.width.toString()
                    customHeight = info.height.toString()
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Failed to load image: ${e.message}")
                } finally {
                    isProcessing = false
                }
            }
        }
    }

    // Batch image picker
    val batchImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                isBatchProcessing = true
                batchProgress = 0f
                try {
                    val infos = withContext(Dispatchers.IO) {
                        uris.mapIndexedNotNull { index, uri ->
                            try {
                                val info = loadImageInfo(context, uri)
                                batchProgress = (index + 1).toFloat() / uris.size
                                info
                            } catch (_: Exception) {
                                null
                            }
                        }
                    }
                    batchImages = infos
                    batchResults = emptyList()
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Failed to load images: ${e.message}")
                } finally {
                    isBatchProcessing = false
                }
            }
        }
    }

    // Save single compressed image
    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument(outputFormat.mimeType)
    ) { uri ->
        uri?.let { destUri ->
            val result = compressedResult
            if (result != null) {
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            context.contentResolver.openOutputStream(destUri)?.use { os ->
                                os.write(result.bytes)
                            }
                        }
                        snackbarHostState.showSnackbar("Image saved successfully")
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar("Failed to save: ${e.message}")
                    }
                }
            }
        }
    }

    // Compress whenever settings change & we have an image
    LaunchedEffect(originalImage, outputFormat, quality, resizeMode, resizePercentage, customWidth, customHeight, lockAspectRatio, maxDimension) {
        val img = originalImage ?: return@LaunchedEffect
        isProcessing = true
        try {
            val result = withContext(Dispatchers.IO) {
                compressBitmap(
                    img.bitmap, outputFormat, quality, resizeMode,
                    resizePercentage, customWidth.toIntOrNull(), customHeight.toIntOrNull(),
                    lockAspectRatio, maxDimension.toIntOrNull() ?: 1920
                )
            }
            compressedResult = result
        } catch (_: Exception) {
            compressedResult = null
        } finally {
            isProcessing = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.compress_image_title)) },
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // ── Batch mode toggle ────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Batch Mode", style = MaterialTheme.typography.labelLarge)
                Switch(
                    checked = batchMode,
                    onCheckedChange = {
                        batchMode = it
                        if (!it) {
                            batchImages = emptyList()
                            batchResults = emptyList()
                        }
                    }
                )
            }

            // ── Pick image button ────────────────────────────────────────
            Button(
                onClick = {
                    if (batchMode) {
                        batchImagePicker.launch("image/*")
                    } else {
                        singleImagePicker.launch("image/*")
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isProcessing && !isBatchProcessing
            ) {
                Icon(Icons.Filled.Image, null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (batchMode) "Select Images" else "Select Image")
            }

            // ── Single mode content ──────────────────────────────────────
            if (!batchMode) {
                // Original image info
                originalImage?.let { img ->
                    OriginalImageInfoCard(img)
                }

                // Settings
                if (originalImage != null) {
                    FormatSelector(outputFormat) { outputFormat = it }

                    if (outputFormat != ImageOutputFormat.PNG) {
                        QualitySlider(quality) { quality = it }
                    }

                    ResizeOptions(
                        resizeMode = resizeMode,
                        onResizeModeChange = { resizeMode = it },
                        percentage = resizePercentage,
                        onPercentageChange = { resizePercentage = it },
                        customWidth = customWidth,
                        customHeight = customHeight,
                        onCustomWidthChange = { newW ->
                            customWidth = newW
                            if (lockAspectRatio && originalImage != null) {
                                val w = newW.toIntOrNull()
                                if (w != null && originalImage!!.width > 0) {
                                    val ratio = originalImage!!.height.toFloat() / originalImage!!.width.toFloat()
                                    customHeight = (w * ratio).roundToInt().toString()
                                }
                            }
                        },
                        onCustomHeightChange = { newH ->
                            customHeight = newH
                            if (lockAspectRatio && originalImage != null) {
                                val h = newH.toIntOrNull()
                                if (h != null && originalImage!!.height > 0) {
                                    val ratio = originalImage!!.width.toFloat() / originalImage!!.height.toFloat()
                                    customWidth = (h * ratio).roundToInt().toString()
                                }
                            }
                        },
                        lockAspectRatio = lockAspectRatio,
                        onLockAspectRatioChange = { lockAspectRatio = it },
                        maxDimension = maxDimension,
                        onMaxDimensionChange = { maxDimension = it }
                    )

                    // Processing indicator
                    if (isProcessing) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }

                    // Compressed result info
                    compressedResult?.let { result ->
                        CompressedResultCard(originalImage!!, result)

                        // Compare view
                        CompareView(
                            original = originalImage!!,
                            compressed = result,
                            compareMode = compareMode,
                            onCompareModeChange = { compareMode = it },
                            showOriginal = showOriginalInToggle,
                            onToggle = { showOriginalInToggle = !showOriginalInToggle }
                        )

                        // Save button
                        Button(
                            onClick = {
                                val baseName = originalImage!!.name.substringBeforeLast(".")
                                saveLauncher.launch("${baseName}_compressed.${outputFormat.extension}")
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Save, null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Save Compressed Image")
                        }
                    }
                }

                // Empty state
                if (originalImage == null && !isProcessing) {
                    EmptyState()
                }
            }

            // ── Batch mode content ───────────────────────────────────────
            if (batchMode) {
                if (isBatchProcessing) {
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
                            CircularProgressIndicator()
                            Spacer(Modifier.height(8.dp))
                            Text("Loading images... ${(batchProgress * 100).roundToInt()}%")
                            LinearProgressIndicator(
                                progress = { batchProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            )
                        }
                    }
                }

                if (batchImages.isNotEmpty()) {
                    Text(
                        "${batchImages.size} images selected",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )

                    // Thumbnail strip
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(batchImages) { img ->
                            Card(
                                modifier = Modifier.size(80.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Image(
                                    bitmap = img.bitmap.asImageBitmap(),
                                    contentDescription = img.name,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }

                    // Settings for batch
                    FormatSelector(outputFormat) { outputFormat = it }

                    if (outputFormat != ImageOutputFormat.PNG) {
                        QualitySlider(quality) { quality = it }
                    }

                    ResizeOptions(
                        resizeMode = resizeMode,
                        onResizeModeChange = { resizeMode = it },
                        percentage = resizePercentage,
                        onPercentageChange = { resizePercentage = it },
                        customWidth = customWidth,
                        customHeight = customHeight,
                        onCustomWidthChange = { customWidth = it },
                        onCustomHeightChange = { customHeight = it },
                        lockAspectRatio = lockAspectRatio,
                        onLockAspectRatioChange = { lockAspectRatio = it },
                        maxDimension = maxDimension,
                        onMaxDimensionChange = { maxDimension = it }
                    )

                    // Compress all button
                    Button(
                        onClick = {
                            scope.launch {
                                isBatchProcessing = true
                                batchProgress = 0f
                                try {
                                    val results = withContext(Dispatchers.IO) {
                                        batchImages.mapIndexed { index, img ->
                                            val result = compressBitmap(
                                                img.bitmap, outputFormat, quality, resizeMode,
                                                resizePercentage, customWidth.toIntOrNull(), customHeight.toIntOrNull(),
                                                lockAspectRatio, maxDimension.toIntOrNull() ?: 1920
                                            )
                                            batchProgress = (index + 1).toFloat() / batchImages.size
                                            result
                                        }
                                    }
                                    batchResults = results

                                    val totalOriginal = batchImages.sumOf { it.sizeBytes }
                                    val totalCompressed = results.sumOf { it.sizeBytes.toLong() }
                                    val ratio = if (totalOriginal > 0) ((1.0 - totalCompressed.toDouble() / totalOriginal) * 100) else 0.0
                                    snackbarHostState.showSnackbar(
                                        "Compressed ${results.size} images (${String.format("%.1f", ratio)}% reduction)"
                                    )
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("Batch compression failed: ${e.message}")
                                } finally {
                                    isBatchProcessing = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isBatchProcessing && batchImages.isNotEmpty()
                    ) {
                        Icon(Icons.Filled.Compress, null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Compress All")
                    }

                    if (isBatchProcessing && batchProgress > 0f) {
                        LinearProgressIndicator(
                            progress = { batchProgress },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Text(
                            "Processing... ${(batchProgress * 100).roundToInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center
                        )
                    }

                    // Batch results summary
                    if (batchResults.isNotEmpty() && batchResults.size == batchImages.size) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Batch Results",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(Modifier.height(8.dp))

                                batchImages.forEachIndexed { index, img ->
                                    val result = batchResults[index]
                                    val reduction = if (img.sizeBytes > 0)
                                        ((1.0 - result.sizeBytes.toDouble() / img.sizeBytes) * 100) else 0.0
                                    val isSmaller = result.sizeBytes < img.sizeBytes

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            img.name,
                                            style = MaterialTheme.typography.bodySmall,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            "${formatFileSize(img.sizeBytes)} -> ${formatFileSize(result.sizeBytes.toLong())}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(
                                            "${if (isSmaller) "-" else "+"}${String.format("%.1f", kotlin.math.abs(reduction))}%",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isSmaller) Color(0xFF2E7D32) else Color(0xFFC62828)
                                        )
                                    }
                                }

                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                                val totalOriginal = batchImages.sumOf { it.sizeBytes }
                                val totalCompressed = batchResults.sumOf { it.sizeBytes.toLong() }
                                val totalReduction = if (totalOriginal > 0)
                                    ((1.0 - totalCompressed.toDouble() / totalOriginal) * 100) else 0.0

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        "Total",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        "${formatFileSize(totalOriginal)} -> ${formatFileSize(totalCompressed)}  (${String.format("%.1f", totalReduction)}%)",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = if (totalCompressed < totalOriginal) Color(0xFF2E7D32) else Color(0xFFC62828)
                                    )
                                }
                            }
                        }

                        // Save all button (saves one by one via SAF)
                        var currentBatchSaveIndex by remember { mutableIntStateOf(-1) }
                        var pendingNextSave by remember { mutableIntStateOf(-1) }

                        val batchSaveLauncher = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.CreateDocument(outputFormat.mimeType)
                        ) { uri ->
                            uri?.let { destUri ->
                                val idx = currentBatchSaveIndex
                                if (idx in batchResults.indices) {
                                    scope.launch {
                                        try {
                                            withContext(Dispatchers.IO) {
                                                context.contentResolver.openOutputStream(destUri)?.use { os ->
                                                    os.write(batchResults[idx].bytes)
                                                }
                                            }
                                            snackbarHostState.showSnackbar("Saved image ${idx + 1} of ${batchResults.size}")
                                            // Trigger next save via state change
                                            val nextIdx = idx + 1
                                            if (nextIdx < batchResults.size) {
                                                currentBatchSaveIndex = nextIdx
                                                pendingNextSave = nextIdx
                                            }
                                        } catch (e: Exception) {
                                            snackbarHostState.showSnackbar("Failed to save: ${e.message}")
                                        }
                                    }
                                }
                            }
                        }

                        // Launch next save when pendingNextSave changes
                        LaunchedEffect(pendingNextSave) {
                            if (pendingNextSave >= 0 && pendingNextSave < batchResults.size) {
                                val name = batchImages[pendingNextSave].name.substringBeforeLast(".")
                                batchSaveLauncher.launch("${name}_compressed.${outputFormat.extension}")
                                pendingNextSave = -1
                            }
                        }

                        Button(
                            onClick = {
                                if (batchResults.isNotEmpty()) {
                                    currentBatchSaveIndex = 0
                                    val name = batchImages[0].name.substringBeforeLast(".")
                                    batchSaveLauncher.launch("${name}_compressed.${outputFormat.extension}")
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Save, null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Save All (one by one)")
                        }
                    }
                }

                // Batch empty state
                if (batchImages.isEmpty() && !isBatchProcessing) {
                    EmptyState(isBatch = true)
                }
            }

            BannerAd(modifier = Modifier.fillMaxWidth())
        }
    }
}

// ── Sub-composables ──────────────────────────────────────────────────────────

@Composable
private fun OriginalImageInfoCard(img: CompressImageInfo) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    bitmap = img.bitmap.asImageBitmap(),
                    contentDescription = "Original image",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        img.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        img.format,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "${img.width} x ${img.height}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        formatFileSize(img.sizeBytes),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }
        }
    }
}

@Composable
private fun FormatSelector(selected: ImageOutputFormat, onSelect: (ImageOutputFormat) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Output Format",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ImageOutputFormat.entries.forEach { format ->
                    FilterChip(
                        selected = selected == format,
                        onClick = { onSelect(format) },
                        label = { Text(format.label) },
                        leadingIcon = if (selected == format) {
                            { Icon(Icons.Filled.Check, null, modifier = Modifier.size(18.dp)) }
                        } else null,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun QualitySlider(quality: Int, onQualityChange: (Int) -> Unit) {
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
                    "Quality",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    "$quality%",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Slider(
                value = quality.toFloat(),
                onValueChange = { onQualityChange(it.roundToInt()) },
                valueRange = 1f..100f,
                steps = 0,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("1 (smallest)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("100 (best)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun ResizeOptions(
    resizeMode: ResizeMode,
    onResizeModeChange: (ResizeMode) -> Unit,
    percentage: Int,
    onPercentageChange: (Int) -> Unit,
    customWidth: String,
    customHeight: String,
    onCustomWidthChange: (String) -> Unit,
    onCustomHeightChange: (String) -> Unit,
    lockAspectRatio: Boolean,
    onLockAspectRatioChange: (Boolean) -> Unit,
    maxDimension: String,
    onMaxDimensionChange: (String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Resize",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(8.dp))

            // Resize mode selector (2x2 grid)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = resizeMode == ResizeMode.ORIGINAL,
                        onClick = { onResizeModeChange(ResizeMode.ORIGINAL) },
                        label = { Text(ResizeMode.ORIGINAL.label, maxLines = 1) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = resizeMode == ResizeMode.PERCENTAGE,
                        onClick = { onResizeModeChange(ResizeMode.PERCENTAGE) },
                        label = { Text(ResizeMode.PERCENTAGE.label, maxLines = 1) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = resizeMode == ResizeMode.CUSTOM,
                        onClick = { onResizeModeChange(ResizeMode.CUSTOM) },
                        label = { Text(ResizeMode.CUSTOM.label, maxLines = 1) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = resizeMode == ResizeMode.MAX_DIMENSION,
                        onClick = { onResizeModeChange(ResizeMode.MAX_DIMENSION) },
                        label = { Text(ResizeMode.MAX_DIMENSION.label, maxLines = 1) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            when (resizeMode) {
                ResizeMode.ORIGINAL -> {
                    Text(
                        "Image will keep its original dimensions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                ResizeMode.PERCENTAGE -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(25, 50, 75).forEach { pct ->
                            FilterChip(
                                selected = percentage == pct,
                                onClick = { onPercentageChange(pct) },
                                label = { Text("$pct%") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                ResizeMode.CUSTOM -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = customWidth,
                            onValueChange = onCustomWidthChange,
                            label = { Text("Width") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Text("x", style = MaterialTheme.typography.titleMedium)
                        OutlinedTextField(
                            value = customHeight,
                            onValueChange = onCustomHeightChange,
                            label = { Text("Height") },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = lockAspectRatio,
                            onCheckedChange = onLockAspectRatioChange
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Lock aspect ratio", style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            if (lockAspectRatio) Icons.Filled.Lock else Icons.Filled.LockOpen,
                            null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                ResizeMode.MAX_DIMENSION -> {
                    OutlinedTextField(
                        value = maxDimension,
                        onValueChange = onMaxDimensionChange,
                        label = { Text("Max pixels (longest side)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        supportingText = { Text("e.g., 1920 limits the longest side to 1920px") }
                    )
                }
            }
        }
    }
}

@Composable
private fun CompressedResultCard(original: CompressImageInfo, result: CompressedResult) {
    val reduction = if (original.sizeBytes > 0)
        ((1.0 - result.sizeBytes.toDouble() / original.sizeBytes) * 100) else 0.0
    val isSmaller = result.sizeBytes < original.sizeBytes

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Compression Result",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        formatFileSize(original.sizeBytes),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Original",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    null,
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.CenterVertically),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        formatFileSize(result.sizeBytes.toLong()),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isSmaller) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                    Text(
                        "Compressed",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Percentage indicator
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSmaller)
                        Color(0xFF2E7D32).copy(alpha = 0.1f)
                    else
                        Color(0xFFC62828).copy(alpha = 0.1f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (isSmaller) Icons.AutoMirrored.Filled.TrendingDown else Icons.AutoMirrored.Filled.TrendingUp,
                        null,
                        modifier = Modifier.size(20.dp),
                        tint = if (isSmaller) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${if (isSmaller) "-" else "+"}${String.format("%.1f", kotlin.math.abs(reduction))}% ${if (isSmaller) "smaller" else "larger"}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (isSmaller) Color(0xFF2E7D32) else Color(0xFFC62828)
                    )
                }
            }

            // Dimensions info
            if (result.width != original.width || result.height != original.height) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Resized: ${original.width}x${original.height} -> ${result.width}x${result.height}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun CompareView(
    original: CompressImageInfo,
    compressed: CompressedResult,
    compareMode: CompareMode,
    onCompareModeChange: (CompareMode) -> Unit,
    showOriginal: Boolean,
    onToggle: () -> Unit
) {
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
                    "Compare",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    FilterChip(
                        selected = compareMode == CompareMode.SIDE_BY_SIDE,
                        onClick = { onCompareModeChange(CompareMode.SIDE_BY_SIDE) },
                        label = { Text("Side by Side", style = MaterialTheme.typography.labelSmall) }
                    )
                    FilterChip(
                        selected = compareMode == CompareMode.TOGGLE,
                        onClick = { onCompareModeChange(CompareMode.TOGGLE) },
                        label = { Text("Toggle", style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }
            Spacer(Modifier.height(12.dp))

            when (compareMode) {
                CompareMode.SIDE_BY_SIDE -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Original",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .clip(MaterialTheme.shapes.small)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = original.bitmap.asImageBitmap(),
                                    contentDescription = "Original",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "Compressed",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.height(4.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(120.dp)
                                    .clip(MaterialTheme.shapes.small)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                Image(
                                    bitmap = compressed.bitmap.asImageBitmap(),
                                    contentDescription = "Compressed",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Fit
                                )
                            }
                        }
                    }
                }

                CompareMode.TOGGLE -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            if (showOriginal) "Original" else "Compressed",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .clip(MaterialTheme.shapes.medium)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Image(
                                bitmap = if (showOriginal) original.bitmap.asImageBitmap()
                                else compressed.bitmap.asImageBitmap(),
                                contentDescription = if (showOriginal) "Original" else "Compressed",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Fit
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(onClick = onToggle) {
                            Icon(Icons.Filled.SwapHoriz, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Toggle View")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyState(isBatch: Boolean = false) {
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
                Icons.Filled.Compress,
                null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(16.dp))
            Text(
                if (isBatch) "Select Images to Compress" else "Select an Image to Compress",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )
            Spacer(Modifier.height(8.dp))
            Text(
                if (isBatch) "Pick multiple images and compress them all with the same settings"
                else "Choose an image to compress with various format and size options",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

// ── Utility functions ────────────────────────────────────────────────────────

private fun loadImageInfo(context: Context, uri: Uri): CompressImageInfo {
    var name = "Unknown"
    var size = 0L

    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (nameIndex >= 0) name = cursor.getString(nameIndex) ?: "Unknown"
            if (sizeIndex >= 0) size = cursor.getLong(sizeIndex)
        }
    }

    val mimeType = context.contentResolver.getType(uri) ?: "image/unknown"
    val format = when {
        mimeType.contains("png") -> "PNG"
        mimeType.contains("webp") -> "WebP"
        mimeType.contains("gif") -> "GIF"
        mimeType.contains("bmp") -> "BMP"
        else -> "JPEG"
    }

    val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: ByteArray(0)
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        ?: throw IllegalArgumentException("Could not decode image")

    return CompressImageInfo(
        uri = uri,
        name = name,
        width = bitmap.width,
        height = bitmap.height,
        format = format,
        sizeBytes = size,
        bitmap = bitmap
    )
}

private fun compressBitmap(
    original: Bitmap,
    format: ImageOutputFormat,
    quality: Int,
    resizeMode: ResizeMode,
    percentage: Int,
    customWidth: Int?,
    customHeight: Int?,
    lockAspectRatio: Boolean,
    maxDim: Int
): CompressedResult {
    // Calculate target dimensions
    var targetW = original.width
    var targetH = original.height

    when (resizeMode) {
        ResizeMode.ORIGINAL -> { /* keep original */ }

        ResizeMode.PERCENTAGE -> {
            targetW = (original.width * percentage / 100f).roundToInt().coerceAtLeast(1)
            targetH = (original.height * percentage / 100f).roundToInt().coerceAtLeast(1)
        }

        ResizeMode.CUSTOM -> {
            targetW = (customWidth ?: original.width).coerceAtLeast(1)
            targetH = (customHeight ?: original.height).coerceAtLeast(1)
        }

        ResizeMode.MAX_DIMENSION -> {
            val longest = maxOf(original.width, original.height)
            if (longest > maxDim) {
                val scale = maxDim.toFloat() / longest
                targetW = (original.width * scale).roundToInt().coerceAtLeast(1)
                targetH = (original.height * scale).roundToInt().coerceAtLeast(1)
            }
        }
    }

    // Resize if needed
    val resized = if (targetW != original.width || targetH != original.height) {
        Bitmap.createScaledBitmap(original, targetW, targetH, true)
    } else {
        original
    }

    // Compress
    val compressFormat = when (format) {
        ImageOutputFormat.JPEG -> Bitmap.CompressFormat.JPEG
        ImageOutputFormat.PNG -> Bitmap.CompressFormat.PNG
        ImageOutputFormat.WEBP -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                Bitmap.CompressFormat.WEBP_LOSSY
            } else {
                @Suppress("DEPRECATION")
                Bitmap.CompressFormat.WEBP
            }
        }
    }

    val outputStream = ByteArrayOutputStream()
    resized.compress(compressFormat, quality, outputStream)
    val compressedBytes = outputStream.toByteArray()

    // Decode compressed bytes back for preview
    val previewBitmap = BitmapFactory.decodeByteArray(compressedBytes, 0, compressedBytes.size)
        ?: resized

    return CompressedResult(
        bytes = compressedBytes,
        bitmap = previewBitmap,
        width = targetW,
        height = targetH,
        sizeBytes = compressedBytes.size
    )
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
        else -> String.format("%.1f MB", bytes / (1024.0 * 1024))
    }
}
