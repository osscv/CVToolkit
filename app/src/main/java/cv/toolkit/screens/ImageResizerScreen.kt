package cv.toolkit.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cv.toolkit.ads.BannerAd
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

private data class SizePreset(val name: String, val width: Int, val height: Int)

private val socialMediaPresets = listOf(
    SizePreset("Instagram Post", 1080, 1080),
    SizePreset("Instagram Story", 1080, 1920),
    SizePreset("Facebook Cover", 820, 312),
    SizePreset("Facebook Post", 1200, 630),
    SizePreset("Twitter/X Header", 1500, 500),
    SizePreset("Twitter/X Post", 1200, 675),
    SizePreset("YouTube Thumbnail", 1280, 720),
    SizePreset("LinkedIn Cover", 1584, 396),
    SizePreset("LinkedIn Post", 1200, 627),
    SizePreset("WhatsApp DP", 500, 500),
    SizePreset("TikTok Video", 1080, 1920),
    SizePreset("Pinterest Pin", 1000, 1500)
)

private val commonPresets = listOf(
    SizePreset("HD", 1280, 720),
    SizePreset("Full HD", 1920, 1080),
    SizePreset("2K", 2560, 1440),
    SizePreset("4K", 3840, 2160),
    SizePreset("Square 512", 512, 512),
    SizePreset("Square 1024", 1024, 1024),
    SizePreset("Icon 256", 256, 256),
    SizePreset("Passport", 600, 600),
    SizePreset("A4 @150dpi", 1240, 1754),
    SizePreset("A4 @300dpi", 2480, 3508)
)

private enum class ResizePresetTab(val label: String) {
    CUSTOM("Custom"),
    SOCIAL("Social Media"),
    COMMON("Common Sizes")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageResizerScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var originalBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var originalName by remember { mutableStateOf("") }
    var originalSize by remember { mutableLongStateOf(0L) }
    var resizedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var resizedBytes by remember { mutableStateOf<ByteArray?>(null) }

    var targetWidth by remember { mutableStateOf("") }
    var targetHeight by remember { mutableStateOf("") }
    var lockAspectRatio by remember { mutableStateOf(true) }
    var selectedTab by remember { mutableStateOf(ResizePresetTab.CUSTOM) }
    var isProcessing by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                isProcessing = true
                try {
                    val result = withContext(Dispatchers.IO) {
                        loadImageForResize(context, it)
                    }
                    originalBitmap = result.first
                    originalName = result.second
                    originalSize = result.third
                    targetWidth = result.first.width.toString()
                    targetHeight = result.first.height.toString()
                    resizedBitmap = null
                    resizedBytes = null
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Failed to load image: ${e.message}")
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
            val bytes = resizedBytes
            if (bytes != null) {
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            context.contentResolver.openOutputStream(destUri)?.use { it.write(bytes) }
                        }
                        snackbarHostState.showSnackbar("Image saved")
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar("Failed to save: ${e.message}")
                    }
                }
            }
        }
    }

    fun applyPreset(preset: SizePreset) {
        targetWidth = preset.width.toString()
        targetHeight = preset.height.toString()
        lockAspectRatio = false
    }

    fun resizeImage() {
        val bitmap = originalBitmap ?: return
        val w = targetWidth.toIntOrNull() ?: return
        val h = targetHeight.toIntOrNull() ?: return
        if (w <= 0 || h <= 0) return

        scope.launch {
            isProcessing = true
            try {
                val result = withContext(Dispatchers.IO) {
                    val scaled = Bitmap.createScaledBitmap(bitmap, w, h, true)
                    val baos = ByteArrayOutputStream()
                    scaled.compress(Bitmap.CompressFormat.PNG, 100, baos)
                    Pair(scaled, baos.toByteArray())
                }
                resizedBitmap = result.first
                resizedBytes = result.second
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Resize failed: ${e.message}")
            } finally {
                isProcessing = false
            }
        }
    }

    fun onWidthChanged(newWidth: String) {
        targetWidth = newWidth
        if (lockAspectRatio && originalBitmap != null) {
            val w = newWidth.toIntOrNull()
            if (w != null && w > 0) {
                val ratio = originalBitmap!!.height.toFloat() / originalBitmap!!.width.toFloat()
                targetHeight = (w * ratio).toInt().toString()
            }
        }
    }

    fun onHeightChanged(newHeight: String) {
        targetHeight = newHeight
        if (lockAspectRatio && originalBitmap != null) {
            val h = newHeight.toIntOrNull()
            if (h != null && h > 0) {
                val ratio = originalBitmap!!.width.toFloat() / originalBitmap!!.height.toFloat()
                targetWidth = (h * ratio).toInt().toString()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Image Resizer") },
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
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { imagePicker.launch("image/*") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isProcessing
                ) {
                    Icon(Icons.Filled.Image, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select Image")
                }

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
                            Icon(Icons.Filled.PhotoSizeSelectLarge, null, Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("Resize images to exact dimensions", style = MaterialTheme.typography.bodyLarge,
                                textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Includes social media presets and common sizes",
                                style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                        }
                    }
                }

                originalBitmap?.let { bitmap ->
                    // Original info
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Image(bitmap = bitmap.asImageBitmap(), contentDescription = null,
                                modifier = Modifier.size(60.dp), contentScale = ContentScale.Fit)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(originalName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text("${bitmap.width} x ${bitmap.height} \u2022 ${formatResizerSize(originalSize)}",
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    // Preset tabs
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Size", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                ResizePresetTab.entries.forEach { tab ->
                                    FilterChip(
                                        selected = selectedTab == tab,
                                        onClick = { selectedTab = tab },
                                        label = { Text(tab.label, style = MaterialTheme.typography.labelSmall) },
                                        modifier = Modifier.weight(1f),
                                        enabled = !isProcessing
                                    )
                                }
                            }

                            when (selectedTab) {
                                ResizePresetTab.CUSTOM -> {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Row(modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalAlignment = Alignment.CenterVertically) {
                                        OutlinedTextField(
                                            value = targetWidth,
                                            onValueChange = { onWidthChanged(it.filter { c -> c.isDigit() }) },
                                            label = { Text("Width") },
                                            suffix = { Text("px") },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true, enabled = !isProcessing
                                        )
                                        IconButton(onClick = { lockAspectRatio = !lockAspectRatio }) {
                                            Icon(
                                                if (lockAspectRatio) Icons.Filled.Lock else Icons.Filled.LockOpen,
                                                contentDescription = "Lock aspect ratio",
                                                tint = if (lockAspectRatio) MaterialTheme.colorScheme.primary
                                                else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        OutlinedTextField(
                                            value = targetHeight,
                                            onValueChange = { onHeightChanged(it.filter { c -> c.isDigit() }) },
                                            label = { Text("Height") },
                                            suffix = { Text("px") },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true, enabled = !isProcessing
                                        )
                                    }
                                }
                                ResizePresetTab.SOCIAL -> {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(socialMediaPresets) { preset ->
                                            val isSelected = targetWidth == preset.width.toString() && targetHeight == preset.height.toString()
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = { applyPreset(preset) },
                                                label = {
                                                    Column {
                                                        Text(preset.name, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
                                                        Text("${preset.width}x${preset.height}", style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                },
                                                enabled = !isProcessing
                                            )
                                        }
                                    }
                                }
                                ResizePresetTab.COMMON -> {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        items(commonPresets) { preset ->
                                            val isSelected = targetWidth == preset.width.toString() && targetHeight == preset.height.toString()
                                            FilterChip(
                                                selected = isSelected,
                                                onClick = { applyPreset(preset) },
                                                label = {
                                                    Column {
                                                        Text(preset.name, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Medium)
                                                        Text("${preset.width}x${preset.height}", style = MaterialTheme.typography.labelSmall,
                                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                    }
                                                },
                                                enabled = !isProcessing
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Preview
                    resizedBitmap?.let { resized ->
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text("Result", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Spacer(modifier = Modifier.height(8.dp))
                                Image(bitmap = resized.asImageBitmap(), contentDescription = "Resized",
                                    modifier = Modifier.fillMaxWidth().heightIn(max = 250.dp), contentScale = ContentScale.Fit)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("${resized.width} x ${resized.height} \u2022 ${formatResizerSize(resizedBytes?.size?.toLong() ?: 0)}",
                                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }

                    if (isProcessing) {
                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                    }

                    // Action buttons
                    Button(onClick = { resizeImage() }, modifier = Modifier.fillMaxWidth(), enabled = !isProcessing) {
                        Icon(Icons.Filled.Transform, null, Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Resize Image")
                    }

                    if (resizedBytes != null) {
                        OutlinedButton(
                            onClick = {
                                val baseName = originalName.substringBeforeLast(".")
                                saveLauncher.launch("${baseName}_resized.png")
                            },
                            modifier = Modifier.fillMaxWidth(), enabled = !isProcessing
                        ) {
                            Icon(Icons.Filled.Save, null, Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Save Resized Image")
                        }
                    }
                }
            }
            BannerAd(modifier = Modifier.fillMaxWidth())
        }
    }
}

private fun loadImageForResize(context: Context, uri: Uri): Triple<Bitmap, String, Long> {
    var name = "image"
    var size = 0L
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val ni = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val si = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (ni >= 0) name = cursor.getString(ni) ?: "image"
            if (si >= 0) size = cursor.getLong(si)
        }
    }
    val inputStream = context.contentResolver.openInputStream(uri) ?: throw Exception("Cannot open file")
    val bitmap = inputStream.use { BitmapFactory.decodeStream(it) } ?: throw Exception("Cannot decode image")
    return Triple(bitmap, name, size)
}

private fun formatResizerSize(bytes: Long): String {
    return when {
        bytes >= 1_048_576 -> String.format("%.2f MB", bytes / 1_048_576.0)
        bytes >= 1024 -> String.format("%.1f KB", bytes / 1024.0)
        else -> "$bytes B"
    }
}
