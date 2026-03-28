package cv.toolkit.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cv.toolkit.ads.BannerAd
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

private data class CropRatio(val name: String, val ratioW: Float, val ratioH: Float)

private val cropRatios = listOf(
    CropRatio("Free", 0f, 0f),
    CropRatio("1:1", 1f, 1f),
    CropRatio("4:3", 4f, 3f),
    CropRatio("3:4", 3f, 4f),
    CropRatio("16:9", 16f, 9f),
    CropRatio("9:16", 9f, 16f),
    CropRatio("3:2", 3f, 2f),
    CropRatio("2:3", 2f, 3f),
    CropRatio("5:4", 5f, 4f),
    CropRatio("4:5", 4f, 5f)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageCropperScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var originalName by remember { mutableStateOf("") }
    var croppedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var croppedBytes by remember { mutableStateOf<ByteArray?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    var selectedRatio by remember { mutableStateOf(cropRatios[0]) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    // Crop rect as fractions of the image (0.0 to 1.0)
    var cropLeft by remember { mutableFloatStateOf(0.1f) }
    var cropTop by remember { mutableFloatStateOf(0.1f) }
    var cropRight by remember { mutableFloatStateOf(0.9f) }
    var cropBottom by remember { mutableFloatStateOf(0.9f) }

    fun resetCropRect() {
        if (selectedRatio.ratioW == 0f) {
            cropLeft = 0.1f; cropTop = 0.1f; cropRight = 0.9f; cropBottom = 0.9f
        } else {
            val bitmap = originalBitmap ?: return
            val imgAspect = bitmap.width.toFloat() / bitmap.height.toFloat()
            val cropAspect = selectedRatio.ratioW / selectedRatio.ratioH
            if (cropAspect > imgAspect) {
                val cropW = 0.8f
                val cropH = (cropW * bitmap.width / cropAspect) / bitmap.height
                val hCenter = 0.5f
                cropLeft = 0.1f; cropRight = 0.9f
                cropTop = (hCenter - cropH / 2).coerceAtLeast(0.02f)
                cropBottom = (hCenter + cropH / 2).coerceAtMost(0.98f)
            } else {
                val cropH = 0.8f
                val cropW = (cropH * bitmap.height * cropAspect) / bitmap.width
                val wCenter = 0.5f
                cropTop = 0.1f; cropBottom = 0.9f
                cropLeft = (wCenter - cropW / 2).coerceAtLeast(0.02f)
                cropRight = (wCenter + cropW / 2).coerceAtMost(0.98f)
            }
        }
    }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                isProcessing = true
                try {
                    val result = withContext(Dispatchers.IO) { loadForCrop(context, it) }
                    originalBitmap = result.first
                    originalName = result.second
                    croppedBitmap = null
                    croppedBytes = null
                    resetCropRect()
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
            val bytes = croppedBytes
            if (bytes != null) {
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            context.contentResolver.openOutputStream(destUri)?.use { it.write(bytes) }
                        }
                        snackbarHostState.showSnackbar("Cropped image saved")
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar("Failed to save: ${e.message}")
                    }
                }
            }
        }
    }

    fun cropImage() {
        val bitmap = originalBitmap ?: return
        scope.launch {
            isProcessing = true
            try {
                val result = withContext(Dispatchers.IO) {
                    val x = (cropLeft * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
                    val y = (cropTop * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
                    val w = ((cropRight - cropLeft) * bitmap.width).toInt().coerceIn(1, bitmap.width - x)
                    val h = ((cropBottom - cropTop) * bitmap.height).toInt().coerceIn(1, bitmap.height - y)
                    val cropped = Bitmap.createBitmap(bitmap, x, y, w, h)
                    val baos = ByteArrayOutputStream()
                    cropped.compress(Bitmap.CompressFormat.PNG, 100, baos)
                    Pair(cropped, baos.toByteArray())
                }
                croppedBitmap = result.first
                croppedBytes = result.second
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Crop failed: ${e.message}")
            } finally {
                isProcessing = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Image Cropper") },
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
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(onClick = { imagePicker.launch("image/*") }, modifier = Modifier.fillMaxWidth(), enabled = !isProcessing) {
                    Icon(Icons.Filled.Image, null, Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select Image")
                }

                if (originalBitmap == null && !isProcessing) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Filled.Crop, null, Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Crop images with precision", style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Drag the crop area or use aspect ratio presets",
                                style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                        }
                    }
                }

                originalBitmap?.let { bitmap ->
                    // Aspect ratio presets
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Aspect Ratio", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(cropRatios) { ratio ->
                                    FilterChip(
                                        selected = selectedRatio == ratio,
                                        onClick = {
                                            selectedRatio = ratio
                                            resetCropRect()
                                            croppedBitmap = null
                                            croppedBytes = null
                                        },
                                        label = { Text(ratio.name, style = MaterialTheme.typography.labelSmall) },
                                        enabled = !isProcessing
                                    )
                                }
                            }
                        }
                    }

                    // Crop area with overlay
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(bitmap.width.toFloat() / bitmap.height.toFloat())
                                .onGloballyPositioned { containerSize = it.size }
                                .pointerInput(selectedRatio) {
                                    detectDragGestures { change, dragAmount ->
                                        change.consume()
                                        val dx = dragAmount.x / containerSize.width
                                        val dy = dragAmount.y / containerSize.height
                                        val newLeft = (cropLeft + dx).coerceIn(0f, cropRight - 0.05f)
                                        val newTop = (cropTop + dy).coerceIn(0f, cropBottom - 0.05f)
                                        val newRight = (cropRight + dx).coerceIn(cropLeft + 0.05f, 1f)
                                        val newBottom = (cropBottom + dy).coerceIn(cropTop + 0.05f, 1f)
                                        // Move entire rect
                                        val w = cropRight - cropLeft
                                        val h = cropBottom - cropTop
                                        if (newLeft >= 0f && newRight <= 1f) {
                                            cropLeft = newLeft; cropRight = newLeft + w
                                        }
                                        if (newTop >= 0f && newBottom <= 1f) {
                                            cropTop = newTop; cropBottom = newTop + h
                                        }
                                    }
                                }
                        ) {
                            Image(bitmap = bitmap.asImageBitmap(), contentDescription = null,
                                modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                            // Dark overlay outside crop area
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val cw = size.width; val ch = size.height
                                val left = cropLeft * cw; val top = cropTop * ch
                                val right = cropRight * cw; val bottom = cropBottom * ch
                                // Dim areas outside crop
                                drawRect(Color.Black.copy(alpha = 0.5f), Offset.Zero, Size(cw, top))
                                drawRect(Color.Black.copy(alpha = 0.5f), Offset(0f, bottom), Size(cw, ch - bottom))
                                drawRect(Color.Black.copy(alpha = 0.5f), Offset(0f, top), Size(left, bottom - top))
                                drawRect(Color.Black.copy(alpha = 0.5f), Offset(right, top), Size(cw - right, bottom - top))
                                // Crop border
                                drawRect(Color.White, Offset(left, top), Size(right - left, bottom - top), style = Stroke(3f))
                                // Grid lines (rule of thirds)
                                val thirdW = (right - left) / 3f
                                val thirdH = (bottom - top) / 3f
                                drawLine(Color.White.copy(alpha = 0.4f), Offset(left + thirdW, top), Offset(left + thirdW, bottom), strokeWidth = 1f)
                                drawLine(Color.White.copy(alpha = 0.4f), Offset(left + 2 * thirdW, top), Offset(left + 2 * thirdW, bottom), strokeWidth = 1f)
                                drawLine(Color.White.copy(alpha = 0.4f), Offset(left, top + thirdH), Offset(right, top + thirdH), strokeWidth = 1f)
                                drawLine(Color.White.copy(alpha = 0.4f), Offset(left, top + 2 * thirdH), Offset(right, top + 2 * thirdH), strokeWidth = 1f)
                                // Corner handles
                                val handleSize = 16f
                                listOf(Offset(left, top), Offset(right - handleSize, top),
                                    Offset(left, bottom - handleSize), Offset(right - handleSize, bottom - handleSize)
                                ).forEach { drawRect(Color.White, it, Size(handleSize, handleSize)) }
                            }
                        }
                    }

                    // Crop dimensions
                    val cropW = ((cropRight - cropLeft) * bitmap.width).toInt()
                    val cropH = ((cropBottom - cropTop) * bitmap.height).toInt()
                    Text("Crop: ${cropW} x ${cropH} px", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center)

                    // Result preview
                    croppedBitmap?.let { cropped ->
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Result", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(8.dp))
                                Image(bitmap = cropped.asImageBitmap(), contentDescription = "Cropped",
                                    modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp), contentScale = ContentScale.Fit)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("${cropped.width} x ${cropped.height} \u2022 ${formatCropSize(croppedBytes?.size?.toLong() ?: 0)}",
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    if (isProcessing) {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                    }

                    Button(onClick = { cropImage() }, modifier = Modifier.fillMaxWidth(), enabled = !isProcessing) {
                        Icon(Icons.Filled.Crop, null, Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Crop Image")
                    }

                    if (croppedBytes != null) {
                        OutlinedButton(
                            onClick = {
                                val baseName = originalName.substringBeforeLast(".")
                                saveLauncher.launch("${baseName}_cropped.png")
                            },
                            modifier = Modifier.fillMaxWidth(), enabled = !isProcessing
                        ) {
                            Icon(Icons.Filled.Save, null, Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save Cropped Image")
                        }
                    }
                }
            }
            BannerAd(modifier = Modifier.fillMaxWidth())
        }
    }
}

private fun loadForCrop(context: Context, uri: Uri): Pair<Bitmap, String> {
    var name = "image"
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val ni = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (ni >= 0) name = cursor.getString(ni) ?: "image"
        }
    }
    val inputStream = context.contentResolver.openInputStream(uri) ?: throw Exception("Cannot open file")
    val bitmap = inputStream.use { BitmapFactory.decodeStream(it) } ?: throw Exception("Cannot decode image")
    return Pair(bitmap, name)
}

private fun formatCropSize(bytes: Long): String {
    return when {
        bytes >= 1_048_576 -> String.format("%.2f MB", bytes / 1_048_576.0)
        bytes >= 1024 -> String.format("%.1f KB", bytes / 1024.0)
        else -> "$bytes B"
    }
}
