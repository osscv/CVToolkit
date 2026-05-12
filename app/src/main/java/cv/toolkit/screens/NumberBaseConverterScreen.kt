package cv.toolkit.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cv.toolkit.ads.BannerAd
import kotlinx.coroutines.launch
import java.math.BigInteger

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NumberBaseConverterScreen(navController: NavController) {
    var inputText by remember { mutableStateOf("") }
    var fromBase by remember { mutableIntStateOf(10) }
    var customOutputBase by remember { mutableIntStateOf(12) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var parsedValue by remember { mutableStateOf<BigInteger?>(null) }
    var fromBaseDropdownExpanded by remember { mutableStateOf(false) }
    var customBaseDropdownExpanded by remember { mutableStateOf(false) }
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    // Auto-convert when input or fromBase changes
    LaunchedEffect(inputText, fromBase) {
        errorMessage = null
        parsedValue = null
        if (inputText.isBlank()) return@LaunchedEffect
        try {
            val cleaned = inputText.trim().lowercase()
            parsedValue = BigInteger(cleaned, fromBase)
        } catch (e: NumberFormatException) {
            errorMessage = "Invalid input for base $fromBase"
        } catch (e: Exception) {
            errorMessage = e.message ?: "Conversion error"
        }
    }

    fun formatBinary(value: BigInteger): String {
        val isNegative = value < BigInteger.ZERO
        val absStr = value.abs().toString(2)
        val grouped = absStr.reversed().chunked(4).joinToString(" ") { it.reversed() }.reversed()
        return if (isNegative) "-$grouped" else grouped
    }

    fun formatDecimal(value: BigInteger): String {
        val isNegative = value < BigInteger.ZERO
        val absStr = value.abs().toString(10)
        val formatted = absStr.reversed().chunked(3).joinToString(",") { it.reversed() }.reversed()
        return if (isNegative) "-$formatted" else formatted
    }

    fun formatHex(value: BigInteger): String {
        val hex = value.toString(16).uppercase()
        return if (value < BigInteger.ZERO) "-0x${hex.removePrefix("-")}" else "0x$hex"
    }

    fun copyToClipboard(text: String) {
        scope.launch {
            clipboard.setClipEntry(
                androidx.compose.ui.platform.ClipEntry(
                    android.content.ClipData.newPlainText("number", text)
                )
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Number Base Converter") },
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
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Input field
                OutlinedTextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    label = { Text("Number Input") },
                    placeholder = { Text("Enter a number...") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                    trailingIcon = {
                        if (inputText.isNotEmpty()) {
                            IconButton(onClick = { inputText = "" }) {
                                Icon(Icons.Filled.Clear, "Clear")
                            }
                        }
                    }
                )

                // From Base label + dropdown
                Text("From Base", style = MaterialTheme.typography.labelMedium)

                // Quick base buttons
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val quickBases = listOf(2 to "Bin", 8 to "Oct", 10 to "Dec", 16 to "Hex")
                    quickBases.forEachIndexed { index, (base, label) ->
                        SegmentedButton(
                            selected = fromBase == base,
                            onClick = { fromBase = base },
                            shape = SegmentedButtonDefaults.itemShape(index, quickBases.size)
                        ) {
                            Text(label, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                // Full base dropdown
                ExposedDropdownMenuBox(
                    expanded = fromBaseDropdownExpanded,
                    onExpandedChange = { fromBaseDropdownExpanded = it }
                ) {
                    OutlinedTextField(
                        value = "Base $fromBase",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("From Base (2-36)") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = fromBaseDropdownExpanded) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                        textStyle = MaterialTheme.typography.bodyMedium
                    )
                    ExposedDropdownMenu(
                        expanded = fromBaseDropdownExpanded,
                        onDismissRequest = { fromBaseDropdownExpanded = false }
                    ) {
                        (2..36).forEach { base ->
                            DropdownMenuItem(
                                text = { Text("Base $base") },
                                onClick = {
                                    fromBase = base
                                    fromBaseDropdownExpanded = false
                                },
                                leadingIcon = {
                                    if (fromBase == base) {
                                        Icon(Icons.Filled.Check, null, modifier = Modifier.size(18.dp))
                                    }
                                }
                            )
                        }
                    }
                }

                // Error message
                errorMessage?.let {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Text(
                            it,
                            modifier = Modifier.padding(12.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }

                // Results card
                parsedValue?.let { value ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                "Conversions",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            HorizontalDivider()

                            // Binary
                            val binaryFormatted = formatBinary(value)
                            ResultRow(
                                label = "Binary (2)",
                                value = binaryFormatted,
                                onCopy = { copyToClipboard(binaryFormatted) }
                            )

                            // Octal
                            val octalStr = value.toString(8)
                            ResultRow(
                                label = "Octal (8)",
                                value = octalStr,
                                onCopy = { copyToClipboard(octalStr) }
                            )

                            // Decimal
                            val decimalFormatted = formatDecimal(value)
                            ResultRow(
                                label = "Decimal (10)",
                                value = decimalFormatted,
                                onCopy = { copyToClipboard(decimalFormatted) }
                            )

                            // Hexadecimal
                            val hexFormatted = formatHex(value)
                            ResultRow(
                                label = "Hex (16)",
                                value = hexFormatted,
                                onCopy = { copyToClipboard(hexFormatted) }
                            )

                            // Base 36
                            val base36Str = value.toString(36).uppercase()
                            ResultRow(
                                label = "Base 36",
                                value = base36Str,
                                onCopy = { copyToClipboard(base36Str) }
                            )

                            HorizontalDivider()

                            // Custom output base selector
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    "Custom Base",
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.width(90.dp)
                                )
                                ExposedDropdownMenuBox(
                                    expanded = customBaseDropdownExpanded,
                                    onExpandedChange = { customBaseDropdownExpanded = it },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    OutlinedTextField(
                                        value = "Base $customOutputBase",
                                        onValueChange = {},
                                        readOnly = true,
                                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = customBaseDropdownExpanded) },
                                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                                        textStyle = MaterialTheme.typography.bodySmall
                                    )
                                    ExposedDropdownMenu(
                                        expanded = customBaseDropdownExpanded,
                                        onDismissRequest = { customBaseDropdownExpanded = false }
                                    ) {
                                        (2..36).forEach { base ->
                                            DropdownMenuItem(
                                                text = { Text("Base $base") },
                                                onClick = {
                                                    customOutputBase = base
                                                    customBaseDropdownExpanded = false
                                                },
                                                leadingIcon = {
                                                    if (customOutputBase == base) {
                                                        Icon(Icons.Filled.Check, null, modifier = Modifier.size(18.dp))
                                                    }
                                                }
                                            )
                                        }
                                    }
                                }
                            }

                            val customStr = value.toString(customOutputBase).uppercase()
                            ResultRow(
                                label = "Base $customOutputBase",
                                value = customStr,
                                onCopy = { copyToClipboard(customStr) }
                            )
                        }
                    }
                }
            }

            BannerAd(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ResultRow(
    label: String,
    value: String,
    onCopy: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(80.dp)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Filled.ContentCopy, "Copy", modifier = Modifier.size(18.dp))
        }
    }
}
