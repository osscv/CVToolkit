package cv.toolkit.screens

import android.content.ClipData
import android.content.ClipboardManager
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
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cv.toolkit.ads.BannerAd
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class PickedColor(
    val red: Int,
    val green: Int,
    val blue: Int,
    val alpha: Int = 255
) {
    val hex: String get() = String.format("#%02X%02X%02X", red, green, blue)
    val hexAlpha: String get() = String.format("#%02X%02X%02X%02X", alpha, red, green, blue)
    val rgb: String get() = "rgb($red, $green, $blue)"
    val rgba: String get() = "rgba($red, $green, $blue, ${String.format("%.2f", alpha / 255.0)})"
    val hsl: String get() {
        val r = red / 255f; val g = green / 255f; val b = blue / 255f
        val max = maxOf(r, g, b); val min = minOf(r, g, b)
        val l = (max + min) / 2f
        if (max == min) return "hsl(0, 0%, ${(l * 100).toInt()}%)"
        val d = max - min
        val s = if (l > 0.5f) d / (2f - max - min) else d / (max + min)
        val h = when (max) {
            r -> ((g - b) / d + (if (g < b) 6 else 0)) * 60
            g -> ((b - r) / d + 2) * 60
            else -> ((r - g) / d + 4) * 60
        }
        return "hsl(${h.toInt()}, ${(s * 100).toInt()}%, ${(l * 100).toInt()}%)"
    }
    val composeColor: Color get() = Color(red, green, blue, alpha)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorPickerScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager

    var bitmap by remember { mutableStateOf<Bitmap?>(null) }
    var imageName by remember { mutableStateOf("") }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }
    var currentColor by remember { mutableStateOf<PickedColor?>(null) }
    var pickedColors by remember { mutableStateOf<List<PickedColor>>(emptyList()) }
    var isProcessing by remember { mutableStateOf(false) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                isProcessing = true
                try {
                    val result = withContext(Dispatchers.IO) { loadForPicker(context, it) }
                    bitmap = result.first
                    imageName = result.second
                    currentColor = null
                    pickedColors = emptyList()
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Failed to load: ${e.message}")
                } finally {
                    isProcessing = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Color Picker") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (pickedColors.isNotEmpty()) {
                        IconButton(onClick = {
                            val text = pickedColors.joinToString("\n") { "${it.hex} | ${it.rgb} | ${it.hsl}" }
                            clipboardManager.setPrimaryClip(ClipData.newPlainText("colors", text))
                            scope.launch { snackbarHostState.showSnackbar("All colors copied") }
                        }) {
                            Icon(Icons.Filled.ContentCopy, contentDescription = "Copy all")
                        }
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
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Button(onClick = { imagePicker.launch("image/*") }, modifier = Modifier.fillMaxWidth(), enabled = !isProcessing) {
                        Icon(Icons.Filled.Image, null, Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Select Image")
                    }
                }

                if (bitmap == null && !isProcessing) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.Colorize, null, Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Pick colors from any image", style = MaterialTheme.typography.bodyLarge,
                                    textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("Tap on the image to pick a color. Get HEX, RGB, and HSL values.",
                                    style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                            }
                        }
                    }
                }

                bitmap?.let { bmp ->
                    // Image with tap
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(bmp.width.toFloat() / bmp.height.toFloat())
                                    .onGloballyPositioned { containerSize = it.size }
                                    .pointerInput(bmp) {
                                        detectTapGestures { offset ->
                                            if (containerSize.width > 0 && containerSize.height > 0) {
                                                val x = (offset.x / containerSize.width * bmp.width).toInt()
                                                    .coerceIn(0, bmp.width - 1)
                                                val y = (offset.y / containerSize.height * bmp.height).toInt()
                                                    .coerceIn(0, bmp.height - 1)
                                                val pixel = bmp.getPixel(x, y)
                                                val color = PickedColor(
                                                    red = (pixel shr 16) and 0xFF,
                                                    green = (pixel shr 8) and 0xFF,
                                                    blue = pixel and 0xFF,
                                                    alpha = (pixel shr 24) and 0xFF
                                                )
                                                currentColor = color
                                                if (!pickedColors.any { it.hex == color.hex }) {
                                                    pickedColors = pickedColors + color
                                                }
                                            }
                                        }
                                    }
                            ) {
                                Image(bitmap = bmp.asImageBitmap(), contentDescription = null,
                                    modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
                            }
                        }
                    }

                    item {
                        Text("Tap on the image to pick a color", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.fillMaxWidth(),
                            textAlign = TextAlign.Center)
                    }

                    // Current color display
                    currentColor?.let { color ->
                        item {
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(CircleShape)
                                                .background(color.composeColor)
                                                .border(2.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text("Selected Color", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                            Text(color.hex, style = MaterialTheme.typography.headlineSmall,
                                                fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    HorizontalDivider()
                                    Spacer(modifier = Modifier.height(12.dp))
                                    ColorValueRow("HEX", color.hex, clipboardManager, scope, snackbarHostState)
                                    ColorValueRow("RGB", color.rgb, clipboardManager, scope, snackbarHostState)
                                    ColorValueRow("RGBA", color.rgba, clipboardManager, scope, snackbarHostState)
                                    ColorValueRow("HSL", color.hsl, clipboardManager, scope, snackbarHostState)
                                }
                            }
                        }
                    }

                    // Picked colors history
                    if (pickedColors.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Picked Colors (${pickedColors.size})", style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                                TextButton(onClick = { pickedColors = emptyList(); currentColor = null }) {
                                    Text("Clear All")
                                }
                            }
                        }

                        items(pickedColors.reversed()) { color ->
                            Card(
                                onClick = { currentColor = color },
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(color.composeColor)
                                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(color.hex, style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace)
                                        Text(color.rgb, style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    IconButton(onClick = {
                                        clipboardManager.setPrimaryClip(ClipData.newPlainText("color", color.hex))
                                        scope.launch { snackbarHostState.showSnackbar("${color.hex} copied") }
                                    }) {
                                        Icon(Icons.Filled.ContentCopy, null, Modifier.size(18.dp))
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
private fun ColorValueRow(
    label: String,
    value: String,
    clipboardManager: ClipboardManager,
    scope: kotlinx.coroutines.CoroutineScope,
    snackbarHostState: SnackbarHostState
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(48.dp))
        Text(value, style = MaterialTheme.typography.bodyMedium, fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f))
        IconButton(onClick = {
            clipboardManager.setPrimaryClip(ClipData.newPlainText(label, value))
            scope.launch { snackbarHostState.showSnackbar("$label copied") }
        }, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Filled.ContentCopy, null, Modifier.size(16.dp))
        }
    }
}

private fun loadForPicker(context: Context, uri: Uri): Pair<Bitmap, String> {
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
