package cv.toolkit.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import cv.toolkit.R
import cv.toolkit.ads.BannerAd
import kotlinx.coroutines.launch
import org.json.*

private enum class JsonAction { FORMAT, MINIFY, VALIDATE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JsonFormatterScreen(navController: NavController) {
    var inputText by remember { mutableStateOf("") }
    var outputText by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var indentSize by remember { mutableIntStateOf(2) }
    var stats by remember { mutableStateOf<JsonStats?>(null) }
    var showOutput by remember { mutableStateOf(false) }
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    fun process(action: JsonAction) {
        errorMessage = null
        stats = null
        showOutput = false

        if (inputText.isBlank()) {
            outputText = ""
            errorMessage = "Input is empty"
            return
        }

        val trimmed = inputText.trim()
        try {
            when (action) {
                JsonAction.FORMAT -> {
                    val parsed = parseJson(trimmed)
                    val indent = indentSize
                    outputText = when (parsed) {
                        is JSONObject -> parsed.toString(indent)
                        is JSONArray -> parsed.toString(indent)
                        else -> trimmed
                    }
                    stats = analyzeJson(parsed)
                    showOutput = true
                }
                JsonAction.MINIFY -> {
                    val parsed = parseJson(trimmed)
                    outputText = parsed.toString()
                    stats = analyzeJson(parsed)
                    showOutput = true
                }
                JsonAction.VALIDATE -> {
                    val parsed = parseJson(trimmed)
                    stats = analyzeJson(parsed)
                    outputText = when (parsed) {
                        is JSONObject -> parsed.toString(indentSize)
                        is JSONArray -> parsed.toString(indentSize)
                        else -> trimmed
                    }
                    showOutput = true
                }
            }
        } catch (e: JSONException) {
            val msg = e.message ?: "Invalid JSON"
            errorMessage = msg
            outputText = ""
            showOutput = false
        }
    }

    fun loadSample() {
        inputText = """{"name":"CV Toolkit","version":"1.0","features":["network","utility","device"],"settings":{"theme":"dark","language":"en","notifications":true},"stats":{"tools":64,"screens":66,"languages":18}}"""
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.json_formatter_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
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
            // Input
            OutlinedTextField(
                value = inputText,
                onValueChange = {
                    inputText = it
                    errorMessage = null
                },
                label = { Text("JSON Input") },
                placeholder = { Text("{\"key\": \"value\"}") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 150.dp, max = 250.dp),
                textStyle = LocalTextStyle.current.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp
                ),
                leadingIcon = { Icon(Icons.Filled.DataObject, null) },
                trailingIcon = {
                    if (inputText.isNotEmpty()) {
                        IconButton(onClick = { inputText = ""; outputText = ""; errorMessage = null; stats = null; showOutput = false }) {
                            Icon(Icons.Filled.Clear, "Clear")
                        }
                    }
                }
            )

            // Action buttons row 1
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            clipboard.getClipEntry()?.clipData?.getItemAt(0)?.text?.let {
                                inputText = it.toString()
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.ContentPaste, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Paste", maxLines = 1)
                }
                OutlinedButton(
                    onClick = { loadSample() },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Code, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Sample", maxLines = 1)
                }
            }

            // Indent size selector
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Indent:", style = MaterialTheme.typography.bodyMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf(2, 3, 4).forEach { size ->
                            FilterChip(
                                selected = indentSize == size,
                                onClick = { indentSize = size },
                                label = { Text("${size}sp") }
                            )
                        }
                    }
                }
            }

            // Main action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { process(JsonAction.FORMAT) },
                    modifier = Modifier.weight(1f),
                    enabled = inputText.isNotBlank()
                ) {
                    Icon(Icons.AutoMirrored.Filled.FormatIndentIncrease, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Format")
                }
                Button(
                    onClick = { process(JsonAction.MINIFY) },
                    modifier = Modifier.weight(1f),
                    enabled = inputText.isNotBlank()
                ) {
                    Icon(Icons.Filled.Compress, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Minify")
                }
                OutlinedButton(
                    onClick = { process(JsonAction.VALIDATE) },
                    modifier = Modifier.weight(1f),
                    enabled = inputText.isNotBlank()
                ) {
                    Icon(Icons.Filled.CheckCircle, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Validate")
                }
            }

            // Error message
            errorMessage?.let { msg ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Error, null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            msg,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }

            // Valid badge
            if (showOutput && errorMessage == null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2E7D32).copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.CheckCircle, null, tint = Color(0xFF2E7D32), modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Valid JSON", color = Color(0xFF2E7D32), fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // Stats
            stats?.let { s ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Statistics", style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            StatChip("Type", s.rootType)
                            StatChip("Keys", s.totalKeys.toString())
                            StatChip("Values", s.totalValues.toString())
                            StatChip("Depth", s.maxDepth.toString())
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            StatChip("Strings", s.stringCount.toString())
                            StatChip("Numbers", s.numberCount.toString())
                            StatChip("Booleans", s.booleanCount.toString())
                            StatChip("Nulls", s.nullCount.toString())
                        }
                        if (s.arrayCount > 0 || s.objectCount > 0) {
                            Spacer(Modifier.height(8.dp))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                StatChip("Objects", s.objectCount.toString())
                                StatChip("Arrays", s.arrayCount.toString())
                            }
                        }
                    }
                }
            }

            // Output
            if (showOutput && outputText.isNotEmpty()) {
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
                            Text("Output", style = MaterialTheme.typography.labelLarge)
                            Row {
                                // Copy
                                IconButton(onClick = {
                                    scope.launch {
                                        clipboard.setClipEntry(
                                            ClipEntry(android.content.ClipData.newPlainText("json", outputText))
                                        )
                                    }
                                }) {
                                    Icon(Icons.Filled.ContentCopy, "Copy", modifier = Modifier.size(20.dp))
                                }
                                // Use as input
                                IconButton(onClick = {
                                    inputText = outputText
                                    outputText = ""
                                    showOutput = false
                                    stats = null
                                }) {
                                    Icon(Icons.AutoMirrored.Filled.Input, "Use as input", modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                        HorizontalDivider()
                        Spacer(Modifier.height(8.dp))

                        val highlighted = highlightJson(outputText)
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 350.dp)
                                .verticalScroll(rememberScrollState())
                                .horizontalScroll(rememberScrollState())
                        ) {
                            Text(
                                text = highlighted,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                        }

                        Spacer(Modifier.height(8.dp))
                        Text(
                            "${outputText.length} characters, ${outputText.lines().size} lines",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            BannerAd(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun StatChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// ── JSON Parsing ──────────────────────────────────────────────────────────────

private fun parseJson(input: String): Any {
    val trimmed = input.trim()
    return when {
        trimmed.startsWith("{") -> JSONObject(trimmed)
        trimmed.startsWith("[") -> JSONArray(trimmed)
        else -> throw JSONException("Input must start with '{' or '['")
    }
}

// ── JSON Statistics ───────────────────────────────────────────────────────────

private data class JsonStats(
    val rootType: String,
    val totalKeys: Int,
    val totalValues: Int,
    val maxDepth: Int,
    val stringCount: Int,
    val numberCount: Int,
    val booleanCount: Int,
    val nullCount: Int,
    val objectCount: Int,
    val arrayCount: Int
)

private fun analyzeJson(json: Any): JsonStats {
    var keys = 0; var values = 0; var strings = 0; var numbers = 0
    var booleans = 0; var nulls = 0; var objects = 0; var arrays = 0

    fun walk(node: Any?, depth: Int): Int {
        var maxD = depth
        when (node) {
            is JSONObject -> {
                objects++
                val iter = node.keys()
                while (iter.hasNext()) {
                    val key = iter.next()
                    keys++
                    val value = node.opt(key)
                    values++
                    when (value) {
                        is JSONObject, is JSONArray -> maxD = maxOf(maxD, walk(value, depth + 1))
                        is String -> strings++
                        is Number -> numbers++
                        is Boolean -> booleans++
                        JSONObject.NULL -> nulls++
                    }
                }
            }
            is JSONArray -> {
                arrays++
                for (i in 0 until node.length()) {
                    val value = node.opt(i)
                    values++
                    when (value) {
                        is JSONObject, is JSONArray -> maxD = maxOf(maxD, walk(value, depth + 1))
                        is String -> strings++
                        is Number -> numbers++
                        is Boolean -> booleans++
                        JSONObject.NULL -> nulls++
                    }
                }
            }
        }
        return maxD
    }

    val maxDepth = walk(json, 1)
    val rootType = if (json is JSONObject) "Object" else "Array"

    return JsonStats(rootType, keys, values, maxDepth, strings, numbers, booleans, nulls, objects, arrays)
}

// ── Syntax Highlighting ───────────────────────────────────────────────────────

@Composable
private fun highlightJson(json: String) = buildAnnotatedString {
    val keyColor = Color(0xFF6A1B9A)      // purple for keys
    val stringColor = Color(0xFF2E7D32)    // green for string values
    val numberColor = Color(0xFF0277BD)    // blue for numbers
    val boolColor = Color(0xFFE65100)      // orange for booleans
    val nullColor = Color(0xFF757575)      // gray for null
    val braceColor = Color(0xFF424242)     // dark gray for braces/brackets
    val punctColor = Color(0xFF9E9E9E)     // light gray for commas/colons

    var i = 0
    var inString = false
    var isKey = false
    var afterColon = false

    while (i < json.length) {
        val c = json[i]
        when {
            c == '"' && (i == 0 || json[i - 1] != '\\') -> {
                if (!inString) {
                    // Starting a string - determine if key or value
                    inString = true
                    val end = findStringEnd(json, i)
                    val str = json.substring(i, end + 1)

                    // Heuristic: if after { or , and before :, it's a key
                    isKey = !afterColon && isLikelyKey(json, i)

                    val color = if (isKey) keyColor else stringColor
                    withStyle(SpanStyle(color = color, fontWeight = if (isKey) FontWeight.Medium else FontWeight.Normal)) {
                        append(str)
                    }
                    i = end + 1
                    inString = false
                    afterColon = false
                    continue
                }
            }
            c == ':' -> {
                withStyle(SpanStyle(color = punctColor)) { append(c.toString()) }
                afterColon = true
                i++; continue
            }
            c == ',' -> {
                withStyle(SpanStyle(color = punctColor)) { append(c.toString()) }
                afterColon = false
                i++; continue
            }
            c == '{' || c == '}' || c == '[' || c == ']' -> {
                withStyle(SpanStyle(color = braceColor, fontWeight = FontWeight.Bold)) { append(c.toString()) }
                if (c == '{') afterColon = false
                i++; continue
            }
            c.isDigit() || (c == '-' && i + 1 < json.length && json[i + 1].isDigit()) -> {
                val numEnd = findNumberEnd(json, i)
                val num = json.substring(i, numEnd)
                withStyle(SpanStyle(color = numberColor)) { append(num) }
                afterColon = false
                i = numEnd; continue
            }
            json.startsWith("true", i) -> {
                withStyle(SpanStyle(color = boolColor, fontWeight = FontWeight.Bold)) { append("true") }
                afterColon = false; i += 4; continue
            }
            json.startsWith("false", i) -> {
                withStyle(SpanStyle(color = boolColor, fontWeight = FontWeight.Bold)) { append("false") }
                afterColon = false; i += 5; continue
            }
            json.startsWith("null", i) -> {
                withStyle(SpanStyle(color = nullColor, fontWeight = FontWeight.Bold)) { append("null") }
                afterColon = false; i += 4; continue
            }
            else -> {
                append(c.toString())
            }
        }
        i++
    }
}

private fun findStringEnd(json: String, start: Int): Int {
    var i = start + 1
    while (i < json.length) {
        if (json[i] == '"' && json[i - 1] != '\\') return i
        i++
    }
    return json.length - 1
}

private fun findNumberEnd(json: String, start: Int): Int {
    var i = start
    if (i < json.length && json[i] == '-') i++
    while (i < json.length && (json[i].isDigit() || json[i] == '.' || json[i] == 'e' || json[i] == 'E' || json[i] == '+' || json[i] == '-')) {
        if ((json[i] == '+' || json[i] == '-') && i > start && json[i - 1] != 'e' && json[i - 1] != 'E') break
        i++
    }
    return i
}

private fun isLikelyKey(json: String, quotePos: Int): Boolean {
    // Look backward past whitespace/newlines for { or ,
    var j = quotePos - 1
    while (j >= 0 && (json[j] == ' ' || json[j] == '\n' || json[j] == '\r' || json[j] == '\t')) j--
    if (j < 0) return true
    return json[j] == '{' || json[j] == ','
}
