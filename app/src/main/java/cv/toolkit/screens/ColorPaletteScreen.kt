package cv.toolkit.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cv.toolkit.R
import cv.toolkit.ads.BannerAd
import kotlin.math.*
import kotlin.random.Random

private data class HSL(val h: Float, val s: Float, val l: Float)

private data class PaletteColor(val hex: String, val name: String)

private enum class PaletteType(val label: String) {
    COMPLEMENTARY("Complementary"),
    ANALOGOUS("Analogous"),
    TRIADIC("Triadic"),
    SPLIT_COMPLEMENTARY("Split-Comp"),
    MONOCHROMATIC("Monochromatic")
}

private fun hexToRgb(hex: String): Triple<Int, Int, Int> {
    val clean = hex.trimStart('#')
    val expanded = if (clean.length == 3) {
        clean.map { "$it$it" }.joinToString("")
    } else {
        clean
    }
    val r = expanded.substring(0, 2).toInt(16)
    val g = expanded.substring(2, 4).toInt(16)
    val b = expanded.substring(4, 6).toInt(16)
    return Triple(r, g, b)
}

private fun hexToHsl(hex: String): HSL {
    val (r, g, b) = hexToRgb(hex)
    val rf = r / 255f
    val gf = g / 255f
    val bf = b / 255f

    val max = maxOf(rf, gf, bf)
    val min = minOf(rf, gf, bf)
    val delta = max - min

    val l = (max + min) / 2f

    val s = if (delta == 0f) 0f else delta / (1f - abs(2f * l - 1f))

    val h = when {
        delta == 0f -> 0f
        max == rf -> 60f * (((gf - bf) / delta) % 6f)
        max == gf -> 60f * (((bf - rf) / delta) + 2f)
        else -> 60f * (((rf - gf) / delta) + 4f)
    }.let { if (it < 0f) it + 360f else it }

    return HSL(h, s.coerceIn(0f, 1f), l.coerceIn(0f, 1f))
}

private fun hslToRgb(hsl: HSL): Triple<Int, Int, Int> {
    val h = hsl.h % 360f
    val s = hsl.s.coerceIn(0f, 1f)
    val l = hsl.l.coerceIn(0f, 1f)

    val c = (1f - abs(2f * l - 1f)) * s
    val x = c * (1f - abs((h / 60f) % 2f - 1f))
    val m = l - c / 2f

    val (r1, g1, b1) = when {
        h < 60f -> Triple(c, x, 0f)
        h < 120f -> Triple(x, c, 0f)
        h < 180f -> Triple(0f, c, x)
        h < 240f -> Triple(0f, x, c)
        h < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }

    val r = ((r1 + m) * 255f).roundToInt().coerceIn(0, 255)
    val g = ((g1 + m) * 255f).roundToInt().coerceIn(0, 255)
    val b = ((b1 + m) * 255f).roundToInt().coerceIn(0, 255)
    return Triple(r, g, b)
}

private fun hslToHex(hsl: HSL): String {
    val (r, g, b) = hslToRgb(hsl)
    return "#${r.toString(16).padStart(2, '0')}${g.toString(16).padStart(2, '0')}${b.toString(16).padStart(2, '0')}".uppercase()
}

private fun hslToColor(hsl: HSL): Color {
    val (r, g, b) = hslToRgb(hsl)
    return Color(r, g, b)
}

private fun hexToColor(hex: String): Color {
    val (r, g, b) = hexToRgb(hex)
    return Color(r, g, b)
}

private fun generatePalette(baseHex: String, type: PaletteType): List<PaletteColor> {
    val hsl = hexToHsl(baseHex)

    return when (type) {
        PaletteType.COMPLEMENTARY -> {
            val comp = HSL((hsl.h + 180f) % 360f, hsl.s, hsl.l)
            listOf(
                PaletteColor(hslToHex(hsl), "Base"),
                PaletteColor(hslToHex(comp), "Complementary")
            )
        }

        PaletteType.ANALOGOUS -> {
            val a1 = HSL((hsl.h + 330f) % 360f, hsl.s, hsl.l)
            val a2 = HSL((hsl.h + 30f) % 360f, hsl.s, hsl.l)
            listOf(
                PaletteColor(hslToHex(a1), "Analogous -30\u00B0"),
                PaletteColor(hslToHex(hsl), "Base"),
                PaletteColor(hslToHex(a2), "Analogous +30\u00B0")
            )
        }

        PaletteType.TRIADIC -> {
            val t1 = HSL((hsl.h + 120f) % 360f, hsl.s, hsl.l)
            val t2 = HSL((hsl.h + 240f) % 360f, hsl.s, hsl.l)
            listOf(
                PaletteColor(hslToHex(hsl), "Base"),
                PaletteColor(hslToHex(t1), "Triadic +120\u00B0"),
                PaletteColor(hslToHex(t2), "Triadic +240\u00B0")
            )
        }

        PaletteType.SPLIT_COMPLEMENTARY -> {
            val sc1 = HSL((hsl.h + 150f) % 360f, hsl.s, hsl.l)
            val sc2 = HSL((hsl.h + 210f) % 360f, hsl.s, hsl.l)
            listOf(
                PaletteColor(hslToHex(hsl), "Base"),
                PaletteColor(hslToHex(sc1), "Split +150\u00B0"),
                PaletteColor(hslToHex(sc2), "Split +210\u00B0")
            )
        }

        PaletteType.MONOCHROMATIC -> {
            listOf(
                PaletteColor(hslToHex(HSL(hsl.h, 0.70f, 0.30f)), "Dark"),
                PaletteColor(hslToHex(HSL(hsl.h, 0.50f, 0.50f)), "Medium"),
                PaletteColor(hslToHex(hsl), "Base"),
                PaletteColor(hslToHex(HSL(hsl.h, 0.30f, 0.70f)), "Light"),
                PaletteColor(hslToHex(HSL(hsl.h, 0.30f, 0.90f)), "Lightest")
            )
        }
    }
}

private fun randomHexColor(): String {
    val r = Random.nextInt(256)
    val g = Random.nextInt(256)
    val b = Random.nextInt(256)
    return "#${r.toString(16).padStart(2, '0')}${g.toString(16).padStart(2, '0')}${b.toString(16).padStart(2, '0')}".uppercase()
}

private fun isValidHex(hex: String): Boolean {
    val clean = hex.trimStart('#')
    return (clean.length == 3 || clean.length == 6) && clean.all { it.isLetterOrDigit() && it in "0123456789ABCDEFabcdef" }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ColorPaletteScreen(navController: NavController) {
    var hexInput by remember { mutableStateOf("FF5722") }
    var baseHex by remember { mutableStateOf("#FF5722") }
    var selectedType by remember { mutableStateOf(PaletteType.COMPLEMENTARY) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current

    val palette = remember(baseHex, selectedType) {
        generatePalette(baseHex, selectedType)
    }

    fun applyHex() {
        val clean = hexInput.trimStart('#').uppercase()
        if (isValidHex(clean)) {
            val expanded = if (clean.length == 3) {
                clean.map { "$it$it" }.joinToString("")
            } else {
                clean
            }
            baseHex = "#$expanded"
            hexInput = expanded
            errorMessage = null
        } else {
            errorMessage = "Invalid HEX color format"
        }
    }

    fun randomize() {
        val newHex = randomHexColor()
        baseHex = newHex
        hexInput = newHex.removePrefix("#")
        errorMessage = null
    }

    fun copyToClipboard(text: String) {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("color", text))
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.color_palette_title)) },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Base color input
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Base Color",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(12.dp))

                        // Color preview
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    try {
                                        hexToColor(baseHex)
                                    } catch (_: Exception) {
                                        Color.Gray
                                    }
                                )
                                .border(
                                    1.dp,
                                    MaterialTheme.colorScheme.outline,
                                    RoundedCornerShape(12.dp)
                                )
                        )

                        Spacer(Modifier.height(12.dp))

                        // HEX input row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "#",
                                style = MaterialTheme.typography.titleLarge,
                                fontFamily = FontFamily.Monospace
                            )
                            OutlinedTextField(
                                value = hexInput,
                                onValueChange = {
                                    hexInput = it.filter { c -> c in "0123456789ABCDEFabcdef" }.take(6).uppercase()
                                },
                                modifier = Modifier.weight(1f),
                                singleLine = true,
                                placeholder = { Text("FF5722") },
                                textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace)
                            )
                            Button(onClick = { applyHex() }) {
                                Text("Apply")
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        // Random button
                        OutlinedButton(
                            onClick = { randomize() },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.Filled.Shuffle,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Random Color")
                        }
                    }
                }
            }

            // Error message
            errorMessage?.let { msg ->
                item {
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
                                msg,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            // Palette type selector
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Palette Type",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(12.dp))

                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            PaletteType.entries.forEach { type ->
                                FilterChip(
                                    selected = selectedType == type,
                                    onClick = { selectedType = type },
                                    label = {
                                        Text(
                                            type.label,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    },
                                    leadingIcon = if (selectedType == type) {
                                        {
                                            Icon(
                                                Icons.Filled.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(FilterChipDefaults.IconSize)
                                            )
                                        }
                                    } else null
                                )
                            }
                        }
                    }
                }
            }

            // Generated palette header
            item {
                Text(
                    "Generated Palette",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            // Palette color swatches
            items(palette) { paletteColor ->
                PaletteColorCard(
                    paletteColor = paletteColor,
                    onCopyHex = { copyToClipboard(paletteColor.hex) }
                )
            }

            // Banner ad
            item {
                Spacer(Modifier.height(4.dp))
                BannerAd(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun PaletteColorCard(
    paletteColor: PaletteColor,
    onCopyHex: () -> Unit
) {
    val rgb = try {
        hexToRgb(paletteColor.hex)
    } catch (_: Exception) {
        Triple(0, 0, 0)
    }

    val color = try {
        hexToColor(paletteColor.hex)
    } catch (_: Exception) {
        Color.Gray
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Color preview box
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(color)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outline,
                        RoundedCornerShape(12.dp)
                    )
            )

            // Color info
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    paletteColor.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    paletteColor.hex,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    "RGB(${rgb.first}, ${rgb.second}, ${rgb.third})",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Copy button
            IconButton(onClick = onCopyHex) {
                Icon(
                    Icons.Filled.ContentCopy,
                    contentDescription = "Copy HEX",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
