package cv.toolkit.screens

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cv.toolkit.ads.BannerAd
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

data class ColorValues(
    val hex: String,
    val rgb: Triple<Int, Int, Int>,
    val hsl: Triple<Float, Float, Float>,
    val hsv: Triple<Float, Float, Float>,
    val cmyk: Quadruple<Float, Float, Float, Float>
)

data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorConverterScreen(navController: NavController) {
    var hexInput by remember { mutableStateOf("") }
    var redInput by remember { mutableStateOf("128") }
    var greenInput by remember { mutableStateOf("128") }
    var blueInput by remember { mutableStateOf("128") }
    var colorValues by remember { mutableStateOf<ColorValues?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var currentColor by remember { mutableStateOf(Color(128, 128, 128)) }
    var selectedImageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showPalette by remember { mutableStateOf(false) }
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            scope.launch {
                try {
                    val bitmap = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(it)?.use { stream ->
                            BitmapFactory.decodeStream(stream)
                        }
                    }
                    selectedImageBitmap = bitmap
                } catch (e: Exception) {
                    errorMessage = "Failed to load image: ${e.message}"
                }
            }
        }
    }

    fun updateFromRgb() {
        errorMessage = null
        try {
            val r = redInput.toIntOrNull()?.coerceIn(0, 255) ?: 0
            val g = greenInput.toIntOrNull()?.coerceIn(0, 255) ?: 0
            val b = blueInput.toIntOrNull()?.coerceIn(0, 255) ?: 0

            currentColor = Color(r, g, b)
            colorValues = calculateColorValues(r, g, b)
            hexInput = colorValues?.hex?.removePrefix("#") ?: ""
        } catch (e: Exception) {
            errorMessage = "Invalid RGB values"
        }
    }

    fun updateFromColor(color: Color) {
        redInput = (color.red * 255).toInt().toString()
        greenInput = (color.green * 255).toInt().toString()
        blueInput = (color.blue * 255).toInt().toString()
        updateFromRgb()
    }

    fun pickColorFromImage(bitmap: Bitmap, x: Float, y: Float) {
        try {
            val pixelX = (x * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
            val pixelY = (y * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
            val pixel = bitmap.getPixel(pixelX, pixelY)
            
            val r = android.graphics.Color.red(pixel)
            val g = android.graphics.Color.green(pixel)
            val b = android.graphics.Color.blue(pixel)
            
            redInput = r.toString()
            greenInput = g.toString()
            blueInput = b.toString()
            updateFromRgb()
        } catch (e: Exception) {
            errorMessage = "Failed to pick color: ${e.message}"
        }
    }

    fun updateFromHex() {
        errorMessage = null
        try {
            var hex = hexInput.trim().removePrefix("#")
            if (hex.length == 3) {
                hex = hex.map { "$it$it" }.joinToString("")
            }
            if (hex.length != 6) {
                if (hex.isNotEmpty()) errorMessage = "Invalid HEX format"
                return
            }

            val r = hex.substring(0, 2).toInt(16)
            val g = hex.substring(2, 4).toInt(16)
            val b = hex.substring(4, 6).toInt(16)

            redInput = r.toString()
            greenInput = g.toString()
            blueInput = b.toString()
            currentColor = Color(r, g, b)
            colorValues = calculateColorValues(r, g, b)
        } catch (e: Exception) {
            errorMessage = "Invalid HEX format"
        }
    }

    // Initialize
    LaunchedEffect(Unit) {
        updateFromRgb()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Color Converter") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Color preview
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Color Preview",
                            style = MaterialTheme.typography.labelLarge
                        )
                        Row {
                            IconButton(onClick = { showPalette = !showPalette }) {
                                Icon(
                                    if (showPalette) Icons.Filled.Palette else Icons.Filled.Palette,
                                    "Color Palette",
                                    tint = if (showPalette) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { imagePicker.launch("image/*") }) {
                                Icon(Icons.Filled.Image, "Pick from Image")
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(currentColor)
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline,
                                RoundedCornerShape(12.dp)
                            )
                    )
                }
            }

            // Color palette
            if (showPalette) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Color Palette",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(12.dp))
                        
                        // Popular colors
                        ColorPaletteSection("Popular", getPopularColors(), ::updateFromColor)
                        Spacer(Modifier.height(8.dp))
                        ColorPaletteSection("Reds", getRedColors(), ::updateFromColor)
                        Spacer(Modifier.height(8.dp))
                        ColorPaletteSection("Greens", getGreenColors(), ::updateFromColor)
                        Spacer(Modifier.height(8.dp))
                        ColorPaletteSection("Blues", getBlueColors(), ::updateFromColor)
                        Spacer(Modifier.height(8.dp))
                        ColorPaletteSection("Grays", getGrayColors(), ::updateFromColor)
                    }
                }
            }

            // Image color picker
            selectedImageBitmap?.let { bitmap ->
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
                                "Pick from Image",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            IconButton(onClick = { selectedImageBitmap = null }) {
                                Icon(Icons.Filled.Close, "Remove Image")
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Tap on the image to pick a color",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        
                        androidx.compose.foundation.Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Selected Image",
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 300.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .pointerInput(bitmap) {
                                    awaitPointerEventScope {
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            event.changes.forEach { change ->
                                                if (change.pressed) {
                                                    val position = change.position
                                                    val x = position.x / size.width
                                                    val y = position.y / size.height
                                                    pickColorFromImage(bitmap, x, y)
                                                    change.consume()
                                                }
                                            }
                                        }
                                    }
                                },
                            contentScale = ContentScale.Fit
                        )
                    }
                }
            }

            // HEX input
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "HEX",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("#", style = MaterialTheme.typography.titleLarge, fontFamily = FontFamily.Monospace)
                        OutlinedTextField(
                            value = hexInput,
                            onValueChange = {
                                hexInput = it.filter { c -> c.isLetterOrDigit() }.take(6).uppercase()
                            },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            placeholder = { Text("RRGGBB") },
                            textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace)
                        )
                        Button(onClick = { updateFromHex() }) {
                            Text("Apply")
                        }
                    }
                }
            }

            // RGB input
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "RGB",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))

                    ColorSlider(
                        label = "R",
                        value = redInput.toFloatOrNull() ?: 0f,
                        onValueChange = {
                            redInput = it.roundToInt().toString()
                            updateFromRgb()
                        },
                        color = Color.Red
                    )
                    ColorSlider(
                        label = "G",
                        value = greenInput.toFloatOrNull() ?: 0f,
                        onValueChange = {
                            greenInput = it.roundToInt().toString()
                            updateFromRgb()
                        },
                        color = Color.Green
                    )
                    ColorSlider(
                        label = "B",
                        value = blueInput.toFloatOrNull() ?: 0f,
                        onValueChange = {
                            blueInput = it.roundToInt().toString()
                            updateFromRgb()
                        },
                        color = Color.Blue
                    )
                }
            }

            // Error message
            errorMessage?.let {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
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
                            it,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Converted values
            colorValues?.let { values ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Color Values",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(12.dp))

                        ColorValueRow(
                            label = "HEX",
                            value = values.hex,
                            onCopy = {
                                scope.launch {
                                    clipboard.setClipEntry(
                                        androidx.compose.ui.platform.ClipEntry(
                                            android.content.ClipData.newPlainText("hex", values.hex)
                                        )
                                    )
                                }
                            }
                        )

                        ColorValueRow(
                            label = "RGB",
                            value = "rgb(${values.rgb.first}, ${values.rgb.second}, ${values.rgb.third})",
                            onCopy = {
                                scope.launch {
                                    clipboard.setClipEntry(
                                        androidx.compose.ui.platform.ClipEntry(
                                            android.content.ClipData.newPlainText("rgb", "rgb(${values.rgb.first}, ${values.rgb.second}, ${values.rgb.third})")
                                        )
                                    )
                                }
                            }
                        )

                        ColorValueRow(
                            label = "HSL",
                            value = "hsl(${values.hsl.first.roundToInt()}°, ${(values.hsl.second * 100).roundToInt()}%, ${(values.hsl.third * 100).roundToInt()}%)",
                            onCopy = {
                                scope.launch {
                                    clipboard.setClipEntry(
                                        androidx.compose.ui.platform.ClipEntry(
                                            android.content.ClipData.newPlainText("hsl", "hsl(${values.hsl.first.roundToInt()}, ${(values.hsl.second * 100).roundToInt()}%, ${(values.hsl.third * 100).roundToInt()}%)")
                                        )
                                    )
                                }
                            }
                        )

                        ColorValueRow(
                            label = "HSV/HSB",
                            value = "hsv(${values.hsv.first.roundToInt()}°, ${(values.hsv.second * 100).roundToInt()}%, ${(values.hsv.third * 100).roundToInt()}%)",
                            onCopy = {
                                scope.launch {
                                    clipboard.setClipEntry(
                                        androidx.compose.ui.platform.ClipEntry(
                                            android.content.ClipData.newPlainText("hsv", "hsv(${values.hsv.first.roundToInt()}, ${(values.hsv.second * 100).roundToInt()}%, ${(values.hsv.third * 100).roundToInt()}%)")
                                        )
                                    )
                                }
                            }
                        )

                        ColorValueRow(
                            label = "CMYK",
                            value = "cmyk(${(values.cmyk.first * 100).roundToInt()}%, ${(values.cmyk.second * 100).roundToInt()}%, ${(values.cmyk.third * 100).roundToInt()}%, ${(values.cmyk.fourth * 100).roundToInt()}%)",
                            onCopy = {
                                scope.launch {
                                    clipboard.setClipEntry(
                                        androidx.compose.ui.platform.ClipEntry(
                                            android.content.ClipData.newPlainText("cmyk", "cmyk(${(values.cmyk.first * 100).roundToInt()}%, ${(values.cmyk.second * 100).roundToInt()}%, ${(values.cmyk.third * 100).roundToInt()}%, ${(values.cmyk.fourth * 100).roundToInt()}%)")
                                        )
                                    )
                                }
                            }
                        )
                    }
                }
            }

            BannerAd(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ColorSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = color,
            modifier = Modifier.width(24.dp)
        )
        Slider(
            value = value.coerceIn(0f, 255f),
            onValueChange = onValueChange,
            valueRange = 0f..255f,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color
            )
        )
        Text(
            value.roundToInt().toString().padStart(3),
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.width(36.dp)
        )
    }
}

@Composable
private fun ColorValueRow(
    label: String,
    value: String,
    onCopy: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = FontFamily.Monospace
            )
        }
        IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Filled.ContentCopy, "Copy", modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun ColorPaletteSection(
    title: String,
    colors: List<Color>,
    onColorSelected: (Color) -> Unit
) {
    Column {
        Text(
            title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            colors.forEach { color ->
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(color)
                        .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                        .clickable { onColorSelected(color) }
                )
            }
        }
    }
}

private fun getPopularColors() = listOf(
    Color(0xFFFF0000), // Red
    Color(0xFF00FF00), // Green
    Color(0xFF0000FF), // Blue
    Color(0xFFFFFF00), // Yellow
    Color(0xFFFF00FF), // Magenta
    Color(0xFF00FFFF), // Cyan
    Color(0xFFFF8800), // Orange
    Color(0xFF8800FF), // Purple
    Color(0xFF000000), // Black
    Color(0xFFFFFFFF), // White
)

private fun getRedColors() = listOf(
    Color(0xFFFFEBEE), Color(0xFFFFCDD2), Color(0xFFEF9A9A), Color(0xFFE57373),
    Color(0xFFEF5350), Color(0xFFF44336), Color(0xFFE53935), Color(0xFFD32F2F),
    Color(0xFFC62828), Color(0xFFB71C1C)
)

private fun getGreenColors() = listOf(
    Color(0xFFE8F5E9), Color(0xFFC8E6C9), Color(0xFFA5D6A7), Color(0xFF81C784),
    Color(0xFF66BB6A), Color(0xFF4CAF50), Color(0xFF43A047), Color(0xFF388E3C),
    Color(0xFF2E7D32), Color(0xFF1B5E20)
)

private fun getBlueColors() = listOf(
    Color(0xFFE3F2FD), Color(0xFFBBDEFB), Color(0xFF90CAF9), Color(0xFF64B5F6),
    Color(0xFF42A5F5), Color(0xFF2196F3), Color(0xFF1E88E5), Color(0xFF1976D2),
    Color(0xFF1565C0), Color(0xFF0D47A1)
)

private fun getGrayColors() = listOf(
    Color(0xFFFFFFFF), Color(0xFFFAFAFA), Color(0xFFF5F5F5), Color(0xFFEEEEEE),
    Color(0xFFE0E0E0), Color(0xFFBDBDBD), Color(0xFF9E9E9E), Color(0xFF757575),
    Color(0xFF616161), Color(0xFF424242), Color(0xFF212121), Color(0xFF000000)
)

private fun calculateColorValues(r: Int, g: Int, b: Int): ColorValues {
    val hex = "#${r.toString(16).padStart(2, '0')}${g.toString(16).padStart(2, '0')}${b.toString(16).padStart(2, '0')}".uppercase()

    // RGB to HSL
    val rf = r / 255f
    val gf = g / 255f
    val bf = b / 255f

    val max = maxOf(rf, gf, bf)
    val min = minOf(rf, gf, bf)
    val delta = max - min

    val l = (max + min) / 2f

    val s = if (delta == 0f) 0f else delta / (1 - kotlin.math.abs(2 * l - 1))

    val h = when {
        delta == 0f -> 0f
        max == rf -> 60f * (((gf - bf) / delta) % 6)
        max == gf -> 60f * (((bf - rf) / delta) + 2)
        else -> 60f * (((rf - gf) / delta) + 4)
    }.let { if (it < 0) it + 360 else it }

    // RGB to HSV
    val v = max
    val sv = if (max == 0f) 0f else delta / max

    // RGB to CMYK
    val k = 1 - max
    val c = if (k == 1f) 0f else (1 - rf - k) / (1 - k)
    val m = if (k == 1f) 0f else (1 - gf - k) / (1 - k)
    val y = if (k == 1f) 0f else (1 - bf - k) / (1 - k)

    return ColorValues(
        hex = hex,
        rgb = Triple(r, g, b),
        hsl = Triple(h, s, l),
        hsv = Triple(h, sv, v),
        cmyk = Quadruple(c, m, y, k)
    )
}
