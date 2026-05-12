package cv.toolkit.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cv.toolkit.R
import cv.toolkit.ads.BannerAd
import cv.toolkit.data.GifEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GifMakerScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var selectedMode by remember { mutableIntStateOf(0) }
    var selectedImages by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var videoFrames by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var frameDelay by remember { mutableIntStateOf(200) }
    var gifWidth by remember { mutableIntStateOf(480) }
    var isProcessing by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var gifBytes by remember { mutableStateOf<ByteArray?>(null) }

    val imagesPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        if (uris.isNotEmpty()) {
            scope.launch {
                isProcessing = true
                try {
                    val loaded = withContext(Dispatchers.IO) {
                        uris.mapNotNull { uri ->
                            try {
                                val stream = context.contentResolver.openInputStream(uri)
                                stream?.use { BitmapFactory.decodeStream(it) }
                            } catch (_: Exception) { null }
                        }
                    }
                    selectedImages = loaded
                    gifBytes = null
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Failed to load images: ${e.message}")
                } finally {
                    isProcessing = false
                }
            }
        }
    }

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                isProcessing = true
                try {
                    val frames = withContext(Dispatchers.IO) {
                        extractVideoFrames(context, it, maxFrames = 30, targetWidth = gifWidth)
                    }
                    videoFrames = frames
                    gifBytes = null
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Failed to extract frames: ${e.message}")
                } finally {
                    isProcessing = false
                }
            }
        }
    }

    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/gif")
    ) { uri ->
        uri?.let { destUri ->
            val bytes = gifBytes ?: return@let
            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(destUri)?.use { it.write(bytes) }
                    }
                    snackbarHostState.showSnackbar("GIF saved successfully")
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Failed to save: ${e.message}")
                }
            }
        }
    }

    fun createGif() {
        val frames = if (selectedMode == 0) selectedImages else videoFrames
        if (frames.size < 2) {
            scope.launch { snackbarHostState.showSnackbar("Need at least 2 frames") }
            return
        }
        scope.launch {
            isProcessing = true
            progress = 0f
            try {
                val result = withContext(Dispatchers.IO) {
                    val encoder = GifEncoder()
                    val first = frames[0]
                    val targetH = (gifWidth.toFloat() / first.width * first.height).toInt()
                    encoder.setSize(gifWidth, targetH)
                    for ((index, frame) in frames.withIndex()) {
                        encoder.addFrame(frame, frameDelay)
                        withContext(Dispatchers.Main) {
                            progress = (index + 1).toFloat() / frames.size
                        }
                    }
                    encoder.encode()
                }
                gifBytes = result
                snackbarHostState.showSnackbar("GIF created! ${gifFormatSize(result.size.toLong())}")
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Failed to create GIF: ${e.message}")
            } finally {
                isProcessing = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.gif_maker_title)) },
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
                // Mode selector
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = selectedMode == 0,
                        onClick = { selectedMode = 0; gifBytes = null },
                        shape = SegmentedButtonDefaults.itemShape(0, 2)
                    ) { Text("Images to GIF") }
                    SegmentedButton(
                        selected = selectedMode == 1,
                        onClick = { selectedMode = 1; gifBytes = null },
                        shape = SegmentedButtonDefaults.itemShape(1, 2)
                    ) { Text("Video to GIF") }
                }

                // Source selection
                if (selectedMode == 0) {
                    Button(onClick = { imagesPicker.launch("image/*") }, modifier = Modifier.fillMaxWidth(), enabled = !isProcessing) {
                        Icon(Icons.Filled.PhotoLibrary, null, Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Select Images")
                    }
                } else {
                    Button(onClick = { videoPicker.launch("video/*") }, modifier = Modifier.fillMaxWidth(), enabled = !isProcessing) {
                        Icon(Icons.Filled.VideoLibrary, null, Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Select Video")
                    }
                }

                // Empty state
                val hasFrames = if (selectedMode == 0) selectedImages.isNotEmpty() else videoFrames.isNotEmpty()
                if (!hasFrames && !isProcessing) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Filled.Gif, null, Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Create animated GIFs", style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                if (selectedMode == 0) "Select multiple images to combine into a GIF"
                                else "Select a video to convert into a GIF",
                                style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                // Frame preview
                if (hasFrames) {
                    val frames = if (selectedMode == 0) selectedImages else videoFrames
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Frames (${frames.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                itemsIndexed(frames) { index, bitmap ->
                                    Card(modifier = Modifier.size(80.dp)) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Image(
                                                bitmap = bitmap.asImageBitmap(),
                                                contentDescription = "Frame ${index + 1}",
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                            Surface(
                                                modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp),
                                                shape = MaterialTheme.shapes.extraSmall,
                                                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.8f)
                                            ) {
                                                Text(
                                                    "${index + 1}",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Settings
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Settings", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(12.dp))

                            Text("Frame delay: ${frameDelay}ms (${1000 / frameDelay} FPS)", style = MaterialTheme.typography.bodySmall)
                            Slider(
                                value = frameDelay.toFloat(),
                                onValueChange = { frameDelay = it.toInt() },
                                valueRange = 50f..1000f,
                                steps = 18
                            )

                            Text("Width: ${gifWidth}px", style = MaterialTheme.typography.bodySmall)
                            Slider(
                                value = gifWidth.toFloat(),
                                onValueChange = { gifWidth = it.toInt() },
                                valueRange = 160f..800f,
                                steps = 7
                            )
                        }
                    }

                    if (isProcessing) {
                        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                    }

                    Button(
                        onClick = { createGif() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isProcessing && frames.size >= 2
                    ) {
                        Icon(Icons.Filled.Gif, null, Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Create GIF")
                    }

                    gifBytes?.let { bytes ->
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("GIF Created", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text(
                                    "Size: ${gifFormatSize(bytes.size.toLong())} \u2022 ${frames.size} frames",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = { saveLauncher.launch("animation.gif") },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !isProcessing
                        ) {
                            Icon(Icons.Filled.Save, null, Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save GIF")
                        }
                    }
                }
            }
            BannerAd(modifier = Modifier.fillMaxWidth())
        }
    }
}

private fun extractVideoFrames(context: Context, uri: Uri, maxFrames: Int, targetWidth: Int): List<Bitmap> {
    val retriever = MediaMetadataRetriever()
    try {
        retriever.setDataSource(context, uri)
        val durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
            ?: return emptyList()
        val frameCount = minOf(maxFrames, (durationMs / 100).toInt().coerceAtLeast(2))
        val intervalUs = durationMs * 1000 / frameCount

        return (0 until frameCount).mapNotNull { i ->
            retriever.getFrameAtTime(i * intervalUs, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)?.let { frame ->
                val scale = targetWidth.toFloat() / frame.width
                Bitmap.createScaledBitmap(frame, targetWidth, (frame.height * scale).toInt(), true)
            }
        }
    } finally {
        retriever.release()
    }
}

private fun gifFormatSize(bytes: Long): String = when {
    bytes >= 1_048_576 -> String.format("%.2f MB", bytes / 1_048_576.0)
    bytes >= 1024 -> String.format("%.1f KB", bytes / 1024.0)
    else -> "$bytes B"
}
