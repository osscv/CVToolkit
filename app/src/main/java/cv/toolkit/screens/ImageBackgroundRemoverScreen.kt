package cv.toolkit.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cv.toolkit.R
import cv.toolkit.ads.BannerAd
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import kotlin.math.sqrt

// ── Data models ─────────────────────────────────────────────────────────────

private data class LoadedImage(
    val bitmap: Bitmap,
    val name: String
)

private enum class PreviewMode(val label: String) {
    ORIGINAL("Original"),
    PROCESSED("Processed"),
    SIDE_BY_SIDE("Side by Side")
}

// ── Main screen ─────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageBackgroundRemoverScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Image state
    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var originalName by remember { mutableStateOf("image") }
    var resultBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var resultBytes by remember { mutableStateOf<ByteArray?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var processingProgress by remember { mutableFloatStateOf(0f) }

    // Color selection
    var selectedColor by remember { mutableStateOf<Color?>(null) }
    var isColorPickMode by remember { mutableStateOf(false) }

    // Settings
    var colorThreshold by remember { mutableFloatStateOf(30f) }
    var edgeSensitivity by remember { mutableFloatStateOf(5f) }

    // Preview
    var previewMode by remember { mutableStateOf(PreviewMode.PROCESSED) }
    var imageViewSize by remember { mutableStateOf(IntSize.Zero) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                isProcessing = true
                try {
                    val result = withContext(Dispatchers.IO) { loadImage(context, it) }
                    originalBitmap = result.bitmap
                    originalName = result.name
                    resultBitmap = null
                    resultBytes = null
                    selectedColor = null
                    isColorPickMode = true
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Failed to load: ${e.message}")
                } finally {
                    isProcessing = false
                }
            }
        }
    }

    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/png")
    ) { uri ->
        uri?.let { destUri ->
            val bytes = resultBytes ?: return@let
            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(destUri)?.use { it.write(bytes) }
                    }
                    snackbarHostState.showSnackbar("Image saved as PNG with transparency")
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Failed to save: ${e.message}")
                }
            }
        }
    }

    fun removeBackground() {
        val source = originalBitmap ?: return
        val bgColor = selectedColor ?: return
        scope.launch {
            isProcessing = true
            processingProgress = 0f
            try {
                val result = withContext(Dispatchers.IO) {
                    removeBackgroundFromBitmap(
                        source = source,
                        targetColor = bgColor,
                        threshold = colorThreshold,
                        edgeFeather = edgeSensitivity,
                        onProgress = { processingProgress = it }
                    )
                }
                resultBitmap = result.first
                resultBytes = result.second
                previewMode = PreviewMode.PROCESSED
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Processing failed: ${e.message}")
            } finally {
                isProcessing = false
                processingProgress = 0f
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.image_bg_remover_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
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
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Pick image button
                Button(
                    onClick = { imagePicker.launch("image/*") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isProcessing
                ) {
                    Icon(Icons.Filled.Image, null, Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select Image")
                }

                // Empty state
                if (originalBitmap == null && !isProcessing) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Filled.ContentCut, null, Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Remove image backgrounds",
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Tap on the image to select the background color, then adjust threshold and edge sensitivity for best results",
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                // Processing indicator
                if (isProcessing) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (processingProgress > 0f) {
                                LinearProgressIndicator(
                                    progress = { processingProgress },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "${(processingProgress * 100).toInt()}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            } else {
                                CircularProgressIndicator()
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Processing...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                originalBitmap?.let { source ->

                    // Color pick mode banner
                    if (isColorPickMode) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    Icons.Filled.TouchApp, null,
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Text(
                                    "Tap on the image to select the background color to remove",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Preview mode toggle (only when result exists)
                    if (resultBitmap != null) {
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                            PreviewMode.entries.forEachIndexed { index, mode ->
                                SegmentedButton(
                                    selected = previewMode == mode,
                                    onClick = { previewMode = mode },
                                    shape = SegmentedButtonDefaults.itemShape(index, PreviewMode.entries.size)
                                ) { Text(mode.label) }
                            }
                        }
                    }

                    // Image preview with tap-to-select
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        when {
                            previewMode == PreviewMode.SIDE_BY_SIDE && resultBitmap != null -> {
                                Row(modifier = Modifier.fillMaxWidth().padding(4.dp)) {
                                    // Checkerboard behind processed to show transparency
                                    Box(modifier = Modifier.weight(1f)) {
                                        Image(
                                            bitmap = source.asImageBitmap(),
                                            contentDescription = "Original",
                                            modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp),
                                            contentScale = ContentScale.Fit
                                        )
                                        Text(
                                            "Original",
                                            modifier = Modifier
                                                .align(Alignment.TopStart)
                                                .background(
                                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                                    MaterialTheme.shapes.extraSmall
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Box(modifier = Modifier.weight(1f)) {
                                        // Checkerboard background to indicate transparency
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .heightIn(max = 200.dp)
                                                .background(Color(0xFFEEEEEE))
                                        )
                                        Image(
                                            bitmap = resultBitmap!!.asImageBitmap(),
                                            contentDescription = "Processed",
                                            modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp),
                                            contentScale = ContentScale.Fit
                                        )
                                        Text(
                                            "Processed",
                                            modifier = Modifier
                                                .align(Alignment.TopStart)
                                                .background(
                                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                                    MaterialTheme.shapes.extraSmall
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            }

                            previewMode == PreviewMode.PROCESSED && resultBitmap != null -> {
                                Box {
                                    // Light background to show transparency
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 250.dp)
                                            .background(Color(0xFFEEEEEE))
                                    )
                                    Image(
                                        bitmap = resultBitmap!!.asImageBitmap(),
                                        contentDescription = "Processed",
                                        modifier = Modifier.fillMaxWidth().heightIn(max = 250.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            }

                            else -> {
                                // Original image with tap-to-select
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .onSizeChanged { imageViewSize = it }
                                        .pointerInput(source, isColorPickMode) {
                                            if (!isColorPickMode) return@pointerInput
                                            detectTapGestures { offset ->
                                                val pickedColor = getColorFromTap(
                                                    bitmap = source,
                                                    tapOffset = offset,
                                                    viewSize = imageViewSize
                                                )
                                                if (pickedColor != null) {
                                                    selectedColor = pickedColor
                                                    isColorPickMode = false
                                                }
                                            }
                                        }
                                ) {
                                    Image(
                                        bitmap = source.asImageBitmap(),
                                        contentDescription = "Source image",
                                        modifier = Modifier.fillMaxWidth().heightIn(max = 250.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                }
                            }
                        }
                    }

                    // Selected color indicator
                    selectedColor?.let { color ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Selected Background Color",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    val argb = color.toArgb()
                                    val r = (argb shr 16) and 0xFF
                                    val g = (argb shr 8) and 0xFF
                                    val b = argb and 0xFF
                                    Text(
                                        "RGB($r, $g, $b)",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                TextButton(onClick = { isColorPickMode = true }) {
                                    Text("Reselect")
                                }
                            }
                        }
                    }

                    // Settings card
                    Card(
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Settings",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            // Color threshold
                            Text(
                                "Color Threshold: ${colorThreshold.toInt()}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                "Higher values remove more similar colors",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                            Slider(
                                value = colorThreshold,
                                onValueChange = { colorThreshold = it },
                                valueRange = 0f..100f,
                                steps = 99
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            // Edge sensitivity
                            Text(
                                "Edge Feathering: ${edgeSensitivity.toInt()}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                "Smooths the edges between foreground and transparent areas",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                            Slider(
                                value = edgeSensitivity,
                                onValueChange = { edgeSensitivity = it },
                                valueRange = 0f..20f,
                                steps = 19
                            )
                        }
                    }

                    // Action buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { removeBackground() },
                            modifier = Modifier.weight(1f),
                            enabled = !isProcessing && selectedColor != null
                        ) {
                            Icon(Icons.Filled.AutoFixHigh, null, Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Remove Background")
                        }
                    }

                    // Save button
                    if (resultBitmap != null && resultBytes != null) {
                        Button(
                            onClick = {
                                val baseName = originalName.substringBeforeLast(".")
                                saveLauncher.launch("${baseName}_no_bg.png")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isProcessing
                        ) {
                            Icon(Icons.Filled.Save, null, Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save as PNG")
                        }
                    }
                }
            }
            BannerAd(modifier = Modifier.fillMaxWidth())
        }
    }
}

// ── Helper: load image ──────────────────────────────────────────────────────

private fun loadImage(context: Context, uri: Uri): LoadedImage {
    var name = "image"
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0) name = cursor.getString(idx) ?: "image"
        }
    }
    val bitmap = context.contentResolver.openInputStream(uri)?.use { stream ->
        BitmapFactory.decodeStream(stream)
    } ?: throw IllegalStateException("Cannot decode image")
    return LoadedImage(bitmap, name)
}

// ── Helper: get color from tap position ─────────────────────────────────────

private fun getColorFromTap(
    bitmap: Bitmap,
    tapOffset: Offset,
    viewSize: IntSize
): Color? {
    if (viewSize.width <= 0 || viewSize.height <= 0) return null

    // Calculate the actual image display area (ContentScale.Fit)
    val imageAspect = bitmap.width.toFloat() / bitmap.height.toFloat()
    val viewAspect = viewSize.width.toFloat() / viewSize.height.toFloat()

    val displayWidth: Float
    val displayHeight: Float
    val offsetX: Float
    val offsetY: Float

    if (imageAspect > viewAspect) {
        // Image is wider - letterboxed top/bottom
        displayWidth = viewSize.width.toFloat()
        displayHeight = viewSize.width.toFloat() / imageAspect
        offsetX = 0f
        offsetY = (viewSize.height - displayHeight) / 2f
    } else {
        // Image is taller - pillarboxed left/right
        displayHeight = viewSize.height.toFloat()
        displayWidth = viewSize.height.toFloat() * imageAspect
        offsetX = (viewSize.width - displayWidth) / 2f
        offsetY = 0f
    }

    // Map tap to bitmap coordinates
    val relX = (tapOffset.x - offsetX) / displayWidth
    val relY = (tapOffset.y - offsetY) / displayHeight

    if (relX < 0f || relX > 1f || relY < 0f || relY > 1f) return null

    val px = (relX * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
    val py = (relY * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)

    val pixel = bitmap.getPixel(px, py)
    return Color(pixel)
}

// ── Background removal algorithm ────────────────────────────────────────────

private fun removeBackgroundFromBitmap(
    source: Bitmap,
    targetColor: Color,
    threshold: Float,
    edgeFeather: Float,
    onProgress: (Float) -> Unit
): Pair<Bitmap, ByteArray> {
    val width = source.width
    val height = source.height
    val result = source.copy(Bitmap.Config.ARGB_8888, true)

    val targetArgb = targetColor.toArgb()
    val tR = (targetArgb shr 16) and 0xFF
    val tG = (targetArgb shr 8) and 0xFF
    val tB = targetArgb and 0xFF

    // Read all pixels at once for performance
    val pixels = IntArray(width * height)
    source.getPixels(pixels, 0, width, 0, 0, width, height)
    val outputPixels = pixels.copyOf()

    val totalRows = height
    for (y in 0 until height) {
        for (x in 0 until width) {
            val idx = y * width + x
            val pixel = pixels[idx]
            val pR = (pixel shr 16) and 0xFF
            val pG = (pixel shr 8) and 0xFF
            val pB = pixel and 0xFF

            // Euclidean distance in RGB space
            val dr = (pR - tR).toFloat()
            val dg = (pG - tG).toFloat()
            val db = (pB - tB).toFloat()
            val distance = sqrt(dr * dr + dg * dg + db * db)

            if (distance < threshold) {
                // Within threshold: make fully transparent
                outputPixels[idx] = android.graphics.Color.TRANSPARENT
            } else if (edgeFeather > 0f && distance < threshold + edgeFeather * 10f) {
                // Edge feathering zone: partial transparency for smooth edges
                val featherRange = edgeFeather * 10f
                val alpha = ((distance - threshold) / featherRange).coerceIn(0f, 1f)
                val a = (alpha * 255).toInt()
                outputPixels[idx] = (a shl 24) or (pR shl 16) or (pG shl 8) or pB
            }
            // else: keep original pixel
        }
        // Update progress periodically
        if (y % 50 == 0 || y == totalRows - 1) {
            onProgress((y + 1).toFloat() / totalRows)
        }
    }

    result.setPixels(outputPixels, 0, width, 0, 0, width, height)

    // Encode to PNG to preserve transparency
    val baos = ByteArrayOutputStream()
    result.compress(Bitmap.CompressFormat.PNG, 100, baos)
    return Pair(result, baos.toByteArray())
}
