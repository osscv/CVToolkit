package cv.toolkit.screens

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
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cv.toolkit.ads.BannerAd
import kotlinx.coroutines.launch

enum class UnitCategory(val label: String) {
    LENGTH("Length"),
    WEIGHT("Weight"),
    TEMPERATURE("Temperature"),
    DATA("Data Size"),
    TIME("Time"),
    AREA("Area"),
    VOLUME("Volume")
}

data class UnitDef(val name: String, val symbol: String, val toBase: (Double) -> Double, val fromBase: (Double) -> Double)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnitConverterScreen(navController: NavController) {
    var selectedCategory by remember { mutableStateOf(UnitCategory.LENGTH) }
    var inputValue by remember { mutableStateOf("1") }
    var fromUnit by remember { mutableStateOf(0) }
    var toUnit by remember { mutableStateOf(1) }
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    val units = remember(selectedCategory) {
        getUnitsForCategory(selectedCategory)
    }

    // Reset units when category changes
    LaunchedEffect(selectedCategory) {
        fromUnit = 0
        toUnit = if (units.size > 1) 1 else 0
    }

    val result = remember(inputValue, fromUnit, toUnit, selectedCategory) {
        try {
            val input = inputValue.toDoubleOrNull() ?: return@remember ""
            if (fromUnit >= units.size || toUnit >= units.size) return@remember ""
            val baseValue = units[fromUnit].toBase(input)
            val converted = units[toUnit].fromBase(baseValue)
            formatResult(converted)
        } catch (_: Exception) {
            "Error"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Unit Converter") },
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
            // Category selector
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Category", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        UnitCategory.entries.take(4).forEach { category ->
                            FilterChip(
                                selected = selectedCategory == category,
                                onClick = { selectedCategory = category },
                                label = { Text(category.label, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        UnitCategory.entries.drop(4).forEach { category ->
                            FilterChip(
                                selected = selectedCategory == category,
                                onClick = { selectedCategory = category },
                                label = { Text(category.label, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
            }

            // Input
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("From", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(8.dp))

                    OutlinedTextField(
                        value = inputValue,
                        onValueChange = { inputValue = it.filter { c -> c.isDigit() || c == '.' || c == '-' } },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace),
                        trailingIcon = {
                            if (inputValue.isNotEmpty()) {
                                IconButton(onClick = { inputValue = "" }) {
                                    Icon(Icons.Filled.Clear, "Clear")
                                }
                            }
                        }
                    )

                    Spacer(Modifier.height(8.dp))

                    // From unit dropdown
                    var fromExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = fromExpanded,
                        onExpandedChange = { fromExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = if (fromUnit < units.size) "${units[fromUnit].name} (${units[fromUnit].symbol})" else "",
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fromExpanded) }
                        )
                        ExposedDropdownMenu(
                            expanded = fromExpanded,
                            onDismissRequest = { fromExpanded = false }
                        ) {
                            units.forEachIndexed { index, unit ->
                                DropdownMenuItem(
                                    text = { Text("${unit.name} (${unit.symbol})") },
                                    onClick = {
                                        fromUnit = index
                                        fromExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Swap button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                IconButton(
                    onClick = {
                        val temp = fromUnit
                        fromUnit = toUnit
                        toUnit = temp
                    }
                ) {
                    Icon(Icons.Filled.SwapVert, "Swap", modifier = Modifier.size(32.dp))
                }
            }

            // Output
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("To", style = MaterialTheme.typography.labelLarge)
                        IconButton(
                            onClick = {
                                scope.launch {
                                    clipboard.setClipEntry(
                                        androidx.compose.ui.platform.ClipEntry(
                                            android.content.ClipData.newPlainText("result", result)
                                        )
                                    )
                                }
                            }
                        ) {
                            Icon(Icons.Filled.ContentCopy, "Copy")
                        }
                    }

                    Text(
                        result,
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    Spacer(Modifier.height(8.dp))

                    // To unit dropdown
                    var toExpanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = toExpanded,
                        onExpandedChange = { toExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = if (toUnit < units.size) "${units[toUnit].name} (${units[toUnit].symbol})" else "",
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = toExpanded) }
                        )
                        ExposedDropdownMenu(
                            expanded = toExpanded,
                            onDismissRequest = { toExpanded = false }
                        ) {
                            units.forEachIndexed { index, unit ->
                                DropdownMenuItem(
                                    text = { Text("${unit.name} (${unit.symbol})") },
                                    onClick = {
                                        toUnit = index
                                        toExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Formula display
            if (inputValue.isNotEmpty() && result.isNotEmpty() && result != "Error") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Text(
                        "$inputValue ${if (fromUnit < units.size) units[fromUnit].symbol else ""} = $result ${if (toUnit < units.size) units[toUnit].symbol else ""}",
                        modifier = Modifier.padding(12.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            BannerAd(modifier = Modifier.fillMaxWidth())
        }
    }
}

private fun formatResult(value: Double): String {
    return when {
        value == 0.0 -> "0"
        kotlin.math.abs(value) >= 1e9 || kotlin.math.abs(value) < 1e-6 -> String.format("%.6e", value)
        value == value.toLong().toDouble() -> value.toLong().toString()
        else -> String.format("%.6f", value).trimEnd('0').trimEnd('.')
    }
}

private fun getUnitsForCategory(category: UnitCategory): List<UnitDef> {
    return when (category) {
        UnitCategory.LENGTH -> listOf(
            UnitDef("Meter", "m", { it }, { it }),
            UnitDef("Kilometer", "km", { it * 1000 }, { it / 1000 }),
            UnitDef("Centimeter", "cm", { it / 100 }, { it * 100 }),
            UnitDef("Millimeter", "mm", { it / 1000 }, { it * 1000 }),
            UnitDef("Mile", "mi", { it * 1609.344 }, { it / 1609.344 }),
            UnitDef("Yard", "yd", { it * 0.9144 }, { it / 0.9144 }),
            UnitDef("Foot", "ft", { it * 0.3048 }, { it / 0.3048 }),
            UnitDef("Inch", "in", { it * 0.0254 }, { it / 0.0254 }),
            UnitDef("Nautical Mile", "nmi", { it * 1852 }, { it / 1852 })
        )
        UnitCategory.WEIGHT -> listOf(
            UnitDef("Kilogram", "kg", { it }, { it }),
            UnitDef("Gram", "g", { it / 1000 }, { it * 1000 }),
            UnitDef("Milligram", "mg", { it / 1000000 }, { it * 1000000 }),
            UnitDef("Metric Ton", "t", { it * 1000 }, { it / 1000 }),
            UnitDef("Pound", "lb", { it * 0.453592 }, { it / 0.453592 }),
            UnitDef("Ounce", "oz", { it * 0.0283495 }, { it / 0.0283495 }),
            UnitDef("Stone", "st", { it * 6.35029 }, { it / 6.35029 })
        )
        UnitCategory.TEMPERATURE -> listOf(
            UnitDef("Celsius", "°C", { it }, { it }),
            UnitDef("Fahrenheit", "°F", { (it - 32) * 5 / 9 }, { it * 9 / 5 + 32 }),
            UnitDef("Kelvin", "K", { it - 273.15 }, { it + 273.15 })
        )
        UnitCategory.DATA -> listOf(
            UnitDef("Byte", "B", { it }, { it }),
            UnitDef("Kilobyte", "KB", { it * 1024 }, { it / 1024 }),
            UnitDef("Megabyte", "MB", { it * 1024 * 1024 }, { it / (1024 * 1024) }),
            UnitDef("Gigabyte", "GB", { it * 1024 * 1024 * 1024 }, { it / (1024 * 1024 * 1024) }),
            UnitDef("Terabyte", "TB", { it * 1024L * 1024 * 1024 * 1024 }, { it / (1024L * 1024 * 1024 * 1024) }),
            UnitDef("Bit", "bit", { it / 8 }, { it * 8 }),
            UnitDef("Kilobit", "Kb", { it * 128 }, { it / 128 }),
            UnitDef("Megabit", "Mb", { it * 131072 }, { it / 131072 })
        )
        UnitCategory.TIME -> listOf(
            UnitDef("Second", "s", { it }, { it }),
            UnitDef("Millisecond", "ms", { it / 1000 }, { it * 1000 }),
            UnitDef("Minute", "min", { it * 60 }, { it / 60 }),
            UnitDef("Hour", "h", { it * 3600 }, { it / 3600 }),
            UnitDef("Day", "d", { it * 86400 }, { it / 86400 }),
            UnitDef("Week", "wk", { it * 604800 }, { it / 604800 }),
            UnitDef("Month (30d)", "mo", { it * 2592000 }, { it / 2592000 }),
            UnitDef("Year (365d)", "yr", { it * 31536000 }, { it / 31536000 })
        )
        UnitCategory.AREA -> listOf(
            UnitDef("Square Meter", "m²", { it }, { it }),
            UnitDef("Square Kilometer", "km²", { it * 1000000 }, { it / 1000000 }),
            UnitDef("Hectare", "ha", { it * 10000 }, { it / 10000 }),
            UnitDef("Acre", "ac", { it * 4046.86 }, { it / 4046.86 }),
            UnitDef("Square Foot", "ft²", { it * 0.092903 }, { it / 0.092903 }),
            UnitDef("Square Inch", "in²", { it * 0.00064516 }, { it / 0.00064516 })
        )
        UnitCategory.VOLUME -> listOf(
            UnitDef("Liter", "L", { it }, { it }),
            UnitDef("Milliliter", "mL", { it / 1000 }, { it * 1000 }),
            UnitDef("Cubic Meter", "m³", { it * 1000 }, { it / 1000 }),
            UnitDef("Gallon (US)", "gal", { it * 3.78541 }, { it / 3.78541 }),
            UnitDef("Quart (US)", "qt", { it * 0.946353 }, { it / 0.946353 }),
            UnitDef("Pint (US)", "pt", { it * 0.473176 }, { it / 0.473176 }),
            UnitDef("Cup (US)", "cup", { it * 0.236588 }, { it / 0.236588 }),
            UnitDef("Fluid Ounce", "fl oz", { it * 0.0295735 }, { it / 0.0295735 })
        )
    }
}
