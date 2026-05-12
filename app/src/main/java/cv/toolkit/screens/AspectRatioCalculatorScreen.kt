package cv.toolkit.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import cv.toolkit.ads.BannerAd

data class AspectRatioPreset(val label: String, val w: Int, val h: Int)

private val ratioPresets = listOf(
    AspectRatioPreset("1:1", 1, 1),
    AspectRatioPreset("4:3", 4, 3),
    AspectRatioPreset("3:2", 3, 2),
    AspectRatioPreset("16:9", 16, 9),
    AspectRatioPreset("16:10", 16, 10),
    AspectRatioPreset("21:9", 21, 9),
    AspectRatioPreset("9:16", 9, 16),
    AspectRatioPreset("3:4", 3, 4),
    AspectRatioPreset("2:3", 2, 3)
)

private data class CommonResolution(val name: String, val width: Int, val height: Int)

private val commonResolutions = listOf(
    CommonResolution("HD", 1280, 720),
    CommonResolution("Full HD", 1920, 1080),
    CommonResolution("2K", 2560, 1440),
    CommonResolution("4K", 3840, 2160),
    CommonResolution("Instagram", 1080, 1080),
    CommonResolution("Story", 1080, 1920)
)

private fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)

private fun simplifyRatio(w: Int, h: Int): Pair<Int, Int> {
    if (w <= 0 || h <= 0) return Pair(0, 0)
    val divisor = gcd(w, h)
    return Pair(w / divisor, h / divisor)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AspectRatioCalculatorScreen(navController: NavController) {
    // Calculate Ratio section state
    var calcWidth by remember { mutableStateOf("") }
    var calcHeight by remember { mutableStateOf("") }

    // Scale Dimensions section state
    var scaleKnownWidth by remember { mutableStateOf("") }
    var scaleKnownHeight by remember { mutableStateOf("") }
    var ratioW by remember { mutableStateOf("16") }
    var ratioH by remember { mutableStateOf("9") }
    var lockRatio by remember { mutableStateOf(true) }
    var selectedPresetIndex by remember { mutableIntStateOf(3) } // default 16:9

    // Preview ratio state
    var previewW by remember { mutableIntStateOf(16) }
    var previewH by remember { mutableIntStateOf(9) }

    // Derived: simplified ratio from Calculate section
    val simplifiedRatio = remember(calcWidth, calcHeight) {
        val w = calcWidth.toIntOrNull() ?: 0
        val h = calcHeight.toIntOrNull() ?: 0
        if (w > 0 && h > 0) simplifyRatio(w, h) else null
    }

    // Derived: scaled dimension result
    val scaledResult = remember(scaleKnownWidth, scaleKnownHeight, ratioW, ratioH) {
        val rw = ratioW.toDoubleOrNull() ?: 0.0
        val rh = ratioH.toDoubleOrNull() ?: 0.0
        if (rw <= 0 || rh <= 0) return@remember null

        val knownW = scaleKnownWidth.toDoubleOrNull()
        val knownH = scaleKnownHeight.toDoubleOrNull()

        when {
            knownW != null && knownW > 0 && (knownH == null || scaleKnownHeight.isEmpty()) -> {
                val computedH = (knownW * rh / rw)
                Pair("width", computedH)
            }
            knownH != null && knownH > 0 && (knownW == null || scaleKnownWidth.isEmpty()) -> {
                val computedW = (knownH * rw / rh)
                Pair("height", computedW)
            }
            else -> null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Aspect Ratio Calculator") },
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
            // --- Calculate Ratio Section ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Calculate Ratio",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Enter width and height to find the simplified ratio",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = calcWidth,
                            onValueChange = { calcWidth = it.filter { c -> c.isDigit() } },
                            label = { Text("Width") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = calcHeight,
                            onValueChange = { calcHeight = it.filter { c -> c.isDigit() } },
                            label = { Text("Height") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (simplifiedRatio != null) {
                        Spacer(Modifier.height(12.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "${calcWidth}×${calcHeight}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    "  →  ",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "${simplifiedRatio.first}:${simplifiedRatio.second}",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }

                        // Update preview when calculating
                        LaunchedEffect(simplifiedRatio) {
                            previewW = simplifiedRatio.first
                            previewH = simplifiedRatio.second
                        }
                    }
                }
            }

            // --- Ratio Presets ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Common Ratios",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))

                    @OptIn(ExperimentalLayoutApi::class)
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ratioPresets.forEachIndexed { index, preset ->
                            FilterChip(
                                selected = selectedPresetIndex == index,
                                onClick = {
                                    selectedPresetIndex = index
                                    ratioW = preset.w.toString()
                                    ratioH = preset.h.toString()
                                    previewW = preset.w
                                    previewH = preset.h
                                },
                                label = { Text(preset.label) }
                            )
                        }
                    }
                }
            }

            // --- Visual Preview ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Ratio Preview",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${previewW}:${previewH}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(12.dp))

                    val primary = MaterialTheme.colorScheme.primary
                    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
                    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant

                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    ) {
                        val canvasW = size.width
                        val canvasH = size.height
                        val padding = 16.dp.toPx()
                        val availableW = canvasW - padding * 2
                        val availableH = canvasH - padding * 2

                        val ratioVal = previewW.toFloat() / previewH.toFloat()
                        val rectW: Float
                        val rectH: Float

                        if (ratioVal > availableW / availableH) {
                            rectW = availableW
                            rectH = availableW / ratioVal
                        } else {
                            rectH = availableH
                            rectW = availableH * ratioVal
                        }

                        val offsetX = (canvasW - rectW) / 2
                        val offsetY = (canvasH - rectH) / 2

                        // Fill
                        drawRect(
                            color = primaryContainer,
                            topLeft = Offset(offsetX, offsetY),
                            size = Size(rectW, rectH)
                        )
                        // Border
                        drawRect(
                            color = primary,
                            topLeft = Offset(offsetX, offsetY),
                            size = Size(rectW, rectH),
                            style = Stroke(width = 2.dp.toPx())
                        )

                        // Diagonal line
                        drawLine(
                            color = onSurfaceVariant.copy(alpha = 0.3f),
                            start = Offset(offsetX, offsetY),
                            end = Offset(offsetX + rectW, offsetY + rectH),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
                }
            }

            // --- Scale Dimensions Section ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Scale Dimensions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Enter a known width or height to calculate the other",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))

                    // Ratio inputs
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = ratioW,
                            onValueChange = {
                                ratioW = it.filter { c -> c.isDigit() || c == '.' }
                                selectedPresetIndex = -1
                                val rw = ratioW.toIntOrNull() ?: 0
                                val rh = ratioH.toIntOrNull() ?: 0
                                if (rw > 0 && rh > 0) {
                                    previewW = rw
                                    previewH = rh
                                }
                            },
                            label = { Text("Ratio W") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            ":",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedTextField(
                            value = ratioH,
                            onValueChange = {
                                ratioH = it.filter { c -> c.isDigit() || c == '.' }
                                selectedPresetIndex = -1
                                val rw = ratioW.toIntOrNull() ?: 0
                                val rh = ratioH.toIntOrNull() ?: 0
                                if (rw > 0 && rh > 0) {
                                    previewW = rw
                                    previewH = rh
                                }
                            },
                            label = { Text("Ratio H") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (lockRatio) Icons.Default.Lock else Icons.Default.LockOpen,
                            contentDescription = "Lock ratio",
                            tint = if (lockRatio) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Lock ratio",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.weight(1f))
                        Switch(
                            checked = lockRatio,
                            onCheckedChange = { lockRatio = it }
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedTextField(
                            value = scaleKnownWidth,
                            onValueChange = {
                                scaleKnownWidth = it.filter { c -> c.isDigit() || c == '.' }
                                if (lockRatio && it.isNotEmpty()) {
                                    scaleKnownHeight = ""
                                }
                            },
                            label = { Text("Known Width") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = scaleKnownHeight,
                            onValueChange = {
                                scaleKnownHeight = it.filter { c -> c.isDigit() || c == '.' }
                                if (lockRatio && it.isNotEmpty()) {
                                    scaleKnownWidth = ""
                                }
                            },
                            label = { Text("Known Height") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (scaledResult != null) {
                        Spacer(Modifier.height(12.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                val resultValue = String.format("%.1f", scaledResult.second)
                                val cleanResult = if (resultValue.endsWith(".0"))
                                    resultValue.dropLast(2) else resultValue

                                if (scaledResult.first == "width") {
                                    Text(
                                        "Calculated Height",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        "${scaleKnownWidth} × $cleanResult",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                } else {
                                    Text(
                                        "Calculated Width",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                    Text(
                                        "$cleanResult × ${scaleKnownHeight}",
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // --- Common Resolutions Reference ---
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Common Resolutions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))

                    commonResolutions.forEach { res ->
                        val ratio = simplifyRatio(res.width, res.height)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                res.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "${res.width}×${res.height}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                "${ratio.first}:${ratio.second}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(0.6f),
                                textAlign = TextAlign.End
                            )
                        }
                        if (res != commonResolutions.last()) {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }

            BannerAd(modifier = Modifier.fillMaxWidth())
        }
    }
}
