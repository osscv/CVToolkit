package cv.toolkit.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageWatermarkScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var originalName by remember { mutableStateOf("image") }
    var watermarkMode by remember { mutableIntStateOf(0) } // 0=Text, 1=Image
    var watermarkText by remember { mutableStateOf("Watermark") }
    var textSize by remember { mutableFloatStateOf(48f) }
    var textOpacity by remember { mutableFloatStateOf(0.5f) }
    var textColor by remember { mutableStateOf(Color.White) }
    var positionIndex by remember { mutableIntStateOf(4) } // center
    var tiled by remember { mutableStateOf(false) }
    var rotation by remember { mutableFloatStateOf(-30f) }

    var watermarkBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var imageScale by remember { mutableFloatStateOf(0.3f) }
    var imageOpacity by remember { mutableFloatStateOf(0.5f) }

    var resultBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var resultBytes by remember { mutableStateOf<ByteArray?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    val positions = listOf("Top Left", "Top Center", "Top Right", "Center Left", "Center", "Center Right", "Bottom Left", "Bottom Center", "Bottom Right")
    val textColors = listOf(
        Color.White to "White", Color.Black to "Black", Color.Red to "Red",
        Color(0xFF2196F3) to "Blue", Color.Yellow to "Yellow", Color.Gray to "Gray"
    )

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                isProcessing = true
                try {
                    val result = withContext(Dispatchers.IO) { loadWatermarkImage(context, it) }
                    originalBitmap = result.first
                    originalName = result.second
                    resultBitmap = null; resultBytes = null
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Failed to load: ${e.message}")
                } finally {
                    isProcessing = false
                }
            }
        }
    }

    val watermarkImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                try {
                    val bmp = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(it)?.use { s -> BitmapFactory.decodeStream(s) }
                    }
                    watermarkBitmap = bmp
                    resultBitmap = null; resultBytes = null
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Failed to load watermark: ${e.message}")
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
                    snackbarHostState.showSnackbar("Watermarked image saved")
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Failed to save: ${e.message}")
                }
            }
        }
    }

    fun applyWatermark() {
        val source = originalBitmap ?: return
        scope.launch {
            isProcessing = true
            try {
                val result = withContext(Dispatchers.IO) {
                    val bmp = source.copy(Bitmap.Config.ARGB_8888, true)
                    val canvas = Canvas(bmp)

                    if (watermarkMode == 0) {
                        // Text watermark
                        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = textColor.copy(alpha = textOpacity).toArgb()
                            this.textSize = textSize * (bmp.width / 1080f).coerceAtLeast(0.5f)
                            typeface = Typeface.DEFAULT_BOLD
                        }

                        if (tiled) {
                            canvas.save()
                            canvas.rotate(rotation, bmp.width / 2f, bmp.height / 2f)
                            val textW = paint.measureText(watermarkText)
                            val textH = paint.textSize * 1.5f
                            val startX = -bmp.width * 0.5f
                            val startY = -bmp.height * 0.5f
                            var y = startY
                            while (y < bmp.height * 1.5f) {
                                var x = startX
                                while (x < bmp.width * 1.5f) {
                                    canvas.drawText(watermarkText, x, y, paint)
                                    x += textW + paint.textSize
                                }
                                y += textH * 2
                            }
                            canvas.restore()
                        } else {
                            val textW = paint.measureText(watermarkText)
                            val textH = paint.textSize
                            val margin = bmp.width * 0.05f
                            val (x, y) = getPosition(positionIndex, bmp.width.toFloat(), bmp.height.toFloat(), textW, textH, margin)
                            canvas.save()
                            canvas.rotate(rotation, x + textW / 2, y - textH / 2)
                            canvas.drawText(watermarkText, x, y, paint)
                            canvas.restore()
                        }
                    } else {
                        // Image watermark
                        val wmBmp = watermarkBitmap ?: return@withContext null
                        val scaledW = (bmp.width * imageScale).toInt().coerceAtLeast(1)
                        val scaledH = (scaledW.toFloat() / wmBmp.width * wmBmp.height).toInt().coerceAtLeast(1)
                        val scaled = Bitmap.createScaledBitmap(wmBmp, scaledW, scaledH, true)
                        val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG).apply {
                            alpha = (imageOpacity * 255).toInt()
                        }

                        if (tiled) {
                            var y = 0f
                            while (y < bmp.height) {
                                var x = 0f
                                while (x < bmp.width) {
                                    canvas.drawBitmap(scaled, x, y, paint)
                                    x += scaledW + scaledW * 0.5f
                                }
                                y += scaledH + scaledH * 0.5f
                            }
                        } else {
                            val margin = bmp.width * 0.05f
                            val (x, y) = getPosition(positionIndex, bmp.width.toFloat(), bmp.height.toFloat(), scaledW.toFloat(), scaledH.toFloat(), margin)
                            canvas.drawBitmap(scaled, x, y - scaledH, paint)
                        }
                    }

                    val baos = ByteArrayOutputStream()
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, baos)
                    Pair(bmp, baos.toByteArray())
                }
                if (result != null) {
                    resultBitmap = result.first
                    resultBytes = result.second
                } else {
                    snackbarHostState.showSnackbar("Please select a watermark image first")
                }
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Failed: ${e.message}")
            } finally {
                isProcessing = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.image_watermark_title)) },
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
                            Icon(@Suppress("DEPRECATION") Icons.Filled.BrandingWatermark, null, Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Add watermarks to images", style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Add text or image watermarks with customizable opacity and position",
                                style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                        }
                    }
                }

                originalBitmap?.let { source ->
                    // Preview
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                        Image(
                            bitmap = source.asImageBitmap(),
                            contentDescription = "Source",
                            modifier = Modifier.fillMaxWidth().heightIn(max = 200.dp),
                            contentScale = ContentScale.Fit
                        )
                    }

                    // Mode selector
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = watermarkMode == 0,
                            onClick = { watermarkMode = 0; resultBitmap = null; resultBytes = null },
                            shape = SegmentedButtonDefaults.itemShape(0, 2)
                        ) { Text("Text") }
                        SegmentedButton(
                            selected = watermarkMode == 1,
                            onClick = { watermarkMode = 1; resultBitmap = null; resultBytes = null },
                            shape = SegmentedButtonDefaults.itemShape(1, 2)
                        ) { Text("Image") }
                    }

                    if (watermarkMode == 0) {
                        // Text watermark settings
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Text Watermark", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedTextField(
                                    value = watermarkText,
                                    onValueChange = { watermarkText = it; resultBitmap = null; resultBytes = null },
                                    label = { Text("Watermark text") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )

                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Size: ${textSize.toInt()}px", style = MaterialTheme.typography.bodySmall)
                                Slider(
                                    value = textSize,
                                    onValueChange = { textSize = it },
                                    valueRange = 16f..128f,
                                    steps = 13
                                )

                                Text("Opacity: ${(textOpacity * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                                Slider(value = textOpacity, onValueChange = { textOpacity = it }, valueRange = 0.1f..1f)

                                Text("Color", style = MaterialTheme.typography.bodySmall)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    textColors.forEach { (color, name) ->
                                        FilterChip(
                                            selected = textColor == color,
                                            onClick = { textColor = color; resultBitmap = null; resultBytes = null },
                                            label = { Text(name, style = MaterialTheme.typography.labelSmall) }
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Rotation: ${rotation.toInt()}\u00B0", style = MaterialTheme.typography.bodySmall)
                                Slider(value = rotation, onValueChange = { rotation = it }, valueRange = -180f..180f)
                            }
                        }
                    } else {
                        // Image watermark settings
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text("Image Watermark", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(12.dp))

                                OutlinedButton(
                                    onClick = { watermarkImagePicker.launch("image/*") },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(Icons.Filled.AddPhotoAlternate, null, Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(if (watermarkBitmap != null) "Change Watermark Image" else "Select Watermark Image")
                                }

                                watermarkBitmap?.let { wm ->
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Image(
                                        bitmap = wm.asImageBitmap(),
                                        contentDescription = "Watermark",
                                        modifier = Modifier.size(64.dp),
                                        contentScale = ContentScale.Fit
                                    )
                                }

                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Scale: ${(imageScale * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                                Slider(value = imageScale, onValueChange = { imageScale = it }, valueRange = 0.05f..0.8f)

                                Text("Opacity: ${(imageOpacity * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
                                Slider(value = imageOpacity, onValueChange = { imageOpacity = it }, valueRange = 0.1f..1f)
                            }
                        }
                    }

                    // Position and tiling
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("Tiled pattern", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                                Switch(checked = tiled, onCheckedChange = { tiled = it; resultBitmap = null; resultBytes = null })
                            }

                            if (!tiled) {
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Position", style = MaterialTheme.typography.bodySmall)
                                Spacer(modifier = Modifier.height(4.dp))
                                // 3x3 grid of position buttons
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    for (row in 0..2) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            for (col in 0..2) {
                                                val idx = row * 3 + col
                                                FilterChip(
                                                    selected = positionIndex == idx,
                                                    onClick = { positionIndex = idx; resultBitmap = null; resultBytes = null },
                                                    label = { Text(positions[idx], style = MaterialTheme.typography.labelSmall) },
                                                    modifier = Modifier.weight(1f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (isProcessing) {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }

                    Button(
                        onClick = { applyWatermark() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isProcessing && (watermarkMode == 0 || watermarkBitmap != null)
                    ) {
                        Icon(@Suppress("DEPRECATION") Icons.Filled.BrandingWatermark, null, Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Apply Watermark")
                    }

                    // Result
                    resultBitmap?.let { result ->
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Result", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(8.dp))
                                Image(
                                    bitmap = result.asImageBitmap(),
                                    contentDescription = "Watermarked",
                                    modifier = Modifier.fillMaxWidth().heightIn(max = 300.dp),
                                    contentScale = ContentScale.Fit
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "${result.width} x ${result.height} \u2022 ${wmFormatSize(resultBytes?.size?.toLong() ?: 0)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = {
                                val baseName = originalName.substringBeforeLast(".")
                                saveLauncher.launch("${baseName}_watermarked.png")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isProcessing
                        ) {
                            Icon(Icons.Filled.Save, null, Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save Watermarked Image")
                        }
                    }
                }
            }
            BannerAd(modifier = Modifier.fillMaxWidth())
        }
    }
}

private fun getPosition(index: Int, canvasW: Float, canvasH: Float, objW: Float, objH: Float, margin: Float): Pair<Float, Float> {
    val row = index / 3
    val col = index % 3
    val x = when (col) {
        0 -> margin
        1 -> (canvasW - objW) / 2
        else -> canvasW - objW - margin
    }
    val y = when (row) {
        0 -> objH + margin
        1 -> (canvasH + objH) / 2
        else -> canvasH - margin
    }
    return Pair(x, y)
}

private fun loadWatermarkImage(context: Context, uri: Uri): Pair<Bitmap, String> {
    var name = "image"
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val ni = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (ni >= 0) name = cursor.getString(ni) ?: "image"
        }
    }
    val stream = context.contentResolver.openInputStream(uri) ?: throw Exception("Cannot open file")
    val bitmap = stream.use { BitmapFactory.decodeStream(it) } ?: throw Exception("Cannot decode image")
    return Pair(bitmap, name)
}

private fun wmFormatSize(bytes: Long): String = when {
    bytes >= 1_048_576 -> String.format("%.2f MB", bytes / 1_048_576.0)
    bytes >= 1024 -> String.format("%.1f KB", bytes / 1024.0)
    else -> "$bytes B"
}
