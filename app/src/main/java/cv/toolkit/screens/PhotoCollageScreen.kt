package cv.toolkit.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
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

private data class CollageLayout(
    val name: String,
    val minImages: Int,
    val maxImages: Int,
    val regions: (Int, Int, Int) -> List<RectF> // (width, height, count) -> rects
)

private val collageLayouts = listOf(
    CollageLayout("2 Side", 2, 2, { w, h, _ ->
        val hw = w / 2f
        listOf(RectF(0f, 0f, hw, h.toFloat()), RectF(hw, 0f, w.toFloat(), h.toFloat()))
    }),
    CollageLayout("2 Stack", 2, 2, { w, h, _ ->
        val hh = h / 2f
        listOf(RectF(0f, 0f, w.toFloat(), hh), RectF(0f, hh, w.toFloat(), h.toFloat()))
    }),
    CollageLayout("3 Left", 3, 3, { w, h, _ ->
        val hw = w / 2f; val hh = h / 2f
        listOf(
            RectF(0f, 0f, hw, h.toFloat()),
            RectF(hw, 0f, w.toFloat(), hh),
            RectF(hw, hh, w.toFloat(), h.toFloat())
        )
    }),
    CollageLayout("3 Top", 3, 3, { w, h, _ ->
        val hh = h / 2f; val hw = w / 2f
        listOf(
            RectF(0f, 0f, w.toFloat(), hh),
            RectF(0f, hh, hw, h.toFloat()),
            RectF(hw, hh, w.toFloat(), h.toFloat())
        )
    }),
    CollageLayout("4 Grid", 4, 4, { w, h, _ ->
        val hw = w / 2f; val hh = h / 2f
        listOf(
            RectF(0f, 0f, hw, hh), RectF(hw, 0f, w.toFloat(), hh),
            RectF(0f, hh, hw, h.toFloat()), RectF(hw, hh, w.toFloat(), h.toFloat())
        )
    }),
    CollageLayout("3 Row", 3, 3, { w, h, _ ->
        val tw = w / 3f
        listOf(
            RectF(0f, 0f, tw, h.toFloat()),
            RectF(tw, 0f, 2 * tw, h.toFloat()),
            RectF(2 * tw, 0f, w.toFloat(), h.toFloat())
        )
    }),
    CollageLayout("6 Grid", 6, 6, { w, h, _ ->
        val tw = w / 3f; val hh = h / 2f
        listOf(
            RectF(0f, 0f, tw, hh), RectF(tw, 0f, 2 * tw, hh), RectF(2 * tw, 0f, w.toFloat(), hh),
            RectF(0f, hh, tw, h.toFloat()), RectF(tw, hh, 2 * tw, h.toFloat()), RectF(2 * tw, hh, w.toFloat(), h.toFloat())
        )
    })
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhotoCollageScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedImages by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var selectedLayout by remember { mutableStateOf(collageLayouts[0]) }
    var padding by remember { mutableIntStateOf(8) }
    var bgColor by remember { mutableStateOf(Color.White) }
    var outputSize by remember { mutableIntStateOf(1080) }
    var collageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var collageBytes by remember { mutableStateOf<ByteArray?>(null) }
    var isProcessing by remember { mutableStateOf(false) }

    val bgColors = listOf(
        Color.White, Color.Black, Color(0xFFF5F5F5), Color(0xFF212121),
        Color(0xFFE91E63), Color(0xFF2196F3), Color(0xFF4CAF50), Color(0xFFFF9800)
    )

    val imagesPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                isProcessing = true
                try {
                    val loaded = withContext(Dispatchers.IO) {
                        uris.take(9).mapNotNull { uri ->
                            try {
                                val stream = context.contentResolver.openInputStream(uri)
                                stream?.use { BitmapFactory.decodeStream(it) }
                            } catch (_: Exception) { null }
                        }
                    }
                    selectedImages = loaded
                    collageBitmap = null
                    collageBytes = null
                    // Auto-select best fitting layout
                    val best = collageLayouts.firstOrNull { it.maxImages == loaded.size }
                        ?: collageLayouts.filter { loaded.size >= it.minImages && loaded.size >= it.maxImages }
                            .maxByOrNull { it.maxImages }
                        ?: collageLayouts[0]
                    selectedLayout = best
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Failed to load images: ${e.message}")
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
            val bytes = collageBytes ?: return@let
            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(destUri)?.use { it.write(bytes) }
                    }
                    snackbarHostState.showSnackbar("Collage saved successfully")
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Failed to save: ${e.message}")
                }
            }
        }
    }

    fun createCollage() {
        if (selectedImages.size < selectedLayout.minImages) {
            scope.launch { snackbarHostState.showSnackbar("Need at least ${selectedLayout.minImages} images for this layout") }
            return
        }
        scope.launch {
            isProcessing = true
            try {
                val result = withContext(Dispatchers.IO) {
                    val w = outputSize
                    val h = outputSize
                    val bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                    val canvas = Canvas(bmp)
                    canvas.drawColor(bgColor.toArgb())

                    val p = padding.toFloat()
                    val usedImages = selectedImages.take(selectedLayout.maxImages)
                    val regions = selectedLayout.regions(w, h, usedImages.size)

                    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
                    for ((index, region) in regions.withIndex()) {
                        if (index >= usedImages.size) break
                        val img = usedImages[index]
                        val dst = RectF(region.left + p, region.top + p, region.right - p, region.bottom - p)
                        if (dst.width() <= 0 || dst.height() <= 0) continue

                        // Center-crop the source image into the destination rect
                        val dstAspect = dst.width() / dst.height()
                        val srcAspect = img.width.toFloat() / img.height.toFloat()
                        val srcRect = if (srcAspect > dstAspect) {
                            val cropW = (img.height * dstAspect).toInt()
                            val offset = (img.width - cropW) / 2
                            android.graphics.Rect(offset, 0, offset + cropW, img.height)
                        } else {
                            val cropH = (img.width / dstAspect).toInt()
                            val offset = (img.height - cropH) / 2
                            android.graphics.Rect(0, offset, img.width, offset + cropH)
                        }
                        canvas.drawBitmap(img, srcRect, dst, paint)
                    }

                    val baos = ByteArrayOutputStream()
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, baos)
                    Pair(bmp, baos.toByteArray())
                }
                collageBitmap = result.first
                collageBytes = result.second
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Failed to create collage: ${e.message}")
            } finally {
                isProcessing = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.photo_collage_title)) },
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
                Button(onClick = { imagesPicker.launch("image/*") }, modifier = Modifier.fillMaxWidth(), enabled = !isProcessing) {
                    Icon(Icons.Filled.PhotoLibrary, null, Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select Photos (2-6)")
                }

                if (selectedImages.isEmpty() && !isProcessing) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Filled.GridView, null, Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Create photo collages", style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Select multiple photos and arrange them into layouts",
                                style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                        }
                    }
                }

                if (selectedImages.isNotEmpty()) {
                    // Thumbnail preview
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Selected (${selectedImages.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                itemsIndexed(selectedImages) { index, bitmap ->
                                    Card(modifier = Modifier.size(64.dp)) {
                                        Image(bitmap = bitmap.asImageBitmap(), contentDescription = "Photo ${index + 1}",
                                            modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                                    }
                                }
                            }
                        }
                    }

                    // Layout picker
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Layout", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                items(collageLayouts.filter { selectedImages.size >= it.minImages }) { layout ->
                                    FilterChip(
                                        selected = selectedLayout == layout,
                                        onClick = {
                                            selectedLayout = layout
                                            collageBitmap = null
                                            collageBytes = null
                                        },
                                        label = { Text(layout.name, style = MaterialTheme.typography.labelSmall) }
                                    )
                                }
                            }
                        }
                    }

                    // Settings
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Settings", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(12.dp))

                            Text("Padding: ${padding}px", style = MaterialTheme.typography.bodySmall)
                            Slider(
                                value = padding.toFloat(),
                                onValueChange = { padding = it.toInt() },
                                valueRange = 0f..32f,
                                steps = 7
                            )

                            Text("Output size: ${outputSize}px", style = MaterialTheme.typography.bodySmall)
                            Slider(
                                value = outputSize.toFloat(),
                                onValueChange = { outputSize = it.toInt() },
                                valueRange = 540f..2160f,
                                steps = 5
                            )

                            Text("Background", style = MaterialTheme.typography.bodySmall)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                bgColors.forEach { color ->
                                    Surface(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clickable {
                                                bgColor = color
                                                collageBitmap = null
                                                collageBytes = null
                                            },
                                        shape = MaterialTheme.shapes.small,
                                        color = color,
                                        border = if (bgColor == color)
                                            ButtonDefaults.outlinedButtonBorder(true)
                                        else null
                                    ) {}
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
                        onClick = { createCollage() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isProcessing && selectedImages.size >= selectedLayout.minImages
                    ) {
                        Icon(Icons.Filled.GridView, null, Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create Collage")
                    }

                    // Result
                    collageBitmap?.let { result ->
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Result", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(8.dp))
                                Image(
                                    bitmap = result.asImageBitmap(),
                                    contentDescription = "Collage",
                                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                                    contentScale = ContentScale.Fit
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "${result.width} x ${result.height} \u2022 ${collageFormatSize(collageBytes?.size?.toLong() ?: 0)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = { saveLauncher.launch("collage.png") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isProcessing
                        ) {
                            Icon(Icons.Filled.Save, null, Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save Collage")
                        }
                    }
                }
            }
            BannerAd(modifier = Modifier.fillMaxWidth())
        }
    }
}

private fun collageFormatSize(bytes: Long): String = when {
    bytes >= 1_048_576 -> String.format("%.2f MB", bytes / 1_048_576.0)
    bytes >= 1024 -> String.format("%.1f KB", bytes / 1024.0)
    else -> "$bytes B"
}
