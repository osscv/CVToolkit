package cv.toolkit.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import coil.decode.SvgDecoder
import coil.request.ImageRequest
import coil.size.Size
import cv.toolkit.R
import cv.toolkit.ads.BannerAd
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class ViewMode { SPLIT, CODE, PREVIEW }

private val defaultSvgTemplate = """
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 200 200" width="200" height="200">
  <rect width="200" height="200" fill="#f0f0f0" />
  <circle cx="100" cy="100" r="80" fill="#4CAF50" opacity="0.8" />
  <text x="100" y="108" text-anchor="middle" font-size="24" fill="white">SVG</text>
</svg>
""".trimIndent()

private data class SvgTemplate(val name: String, val code: String)

private val svgTemplates = listOf(
    SvgTemplate(
        "Blank Canvas",
        """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 200 200" width="200" height="200">
</svg>"""
    ),
    SvgTemplate(
        "Simple Icon",
        """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100" width="100" height="100">
  <circle cx="50" cy="50" r="45" fill="#2196F3" />
  <path d="M35 50 L45 60 L65 40" stroke="white" stroke-width="6" fill="none" stroke-linecap="round" stroke-linejoin="round" />
</svg>"""
    ),
    SvgTemplate(
        "Chart Template",
        """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 200 150" width="200" height="150">
  <rect width="200" height="150" fill="#fafafa" rx="8" />
  <line x1="30" y1="10" x2="30" y2="120" stroke="#ccc" stroke-width="1" />
  <line x1="30" y1="120" x2="190" y2="120" stroke="#ccc" stroke-width="1" />
  <rect x="45" y="60" width="20" height="60" fill="#4CAF50" rx="2" />
  <rect x="80" y="40" width="20" height="80" fill="#2196F3" rx="2" />
  <rect x="115" y="80" width="20" height="40" fill="#FF9800" rx="2" />
  <rect x="150" y="30" width="20" height="90" fill="#9C27B0" rx="2" />
</svg>"""
    ),
    SvgTemplate(
        "Badge / Shield",
        """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 200 80" width="200" height="80">
  <rect width="200" height="80" rx="10" fill="#555" />
  <rect x="80" width="120" height="80" rx="10" fill="#4CAF50" />
  <rect x="80" width="20" height="80" fill="#4CAF50" />
  <text x="45" y="46" text-anchor="middle" font-size="16" fill="white" font-family="sans-serif">build</text>
  <text x="140" y="46" text-anchor="middle" font-size="16" fill="white" font-family="sans-serif">passing</text>
</svg>"""
    ),
    SvgTemplate(
        "Loading Spinner",
        """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100" width="100" height="100">
  <circle cx="50" cy="50" r="40" stroke="#e0e0e0" stroke-width="8" fill="none" />
  <circle cx="50" cy="50" r="40" stroke="#2196F3" stroke-width="8" fill="none" stroke-dasharray="80 170" stroke-linecap="round">
    <animateTransform attributeName="transform" type="rotate" from="0 50 50" to="360 50 50" dur="1s" repeatCount="indefinite" />
  </circle>
</svg>"""
    )
)

private data class SvgElement(val name: String, val snippet: String)

private val svgElements = listOf(
    SvgElement("Rect", """<rect x="10" y="10" width="100" height="50" fill="#4CAF50" />"""),
    SvgElement("Circle", """<circle cx="50" cy="50" r="40" fill="#2196F3" />"""),
    SvgElement("Ellipse", """<ellipse cx="50" cy="50" rx="40" ry="25" fill="#FF9800" />"""),
    SvgElement("Line", """<line x1="0" y1="0" x2="100" y2="100" stroke="#000" stroke-width="2" />"""),
    SvgElement("Text", """<text x="10" y="50" font-size="20" fill="#000">Hello</text>"""),
    SvgElement("Path", """<path d="M10 10 L90 10 L50 90 Z" fill="#9C27B0" />"""),
    SvgElement("Polygon", """<polygon points="50,5 20,99 95,39 5,39 80,99" fill="#F44336" />""")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SvgViewerScreen(navController: NavController) {
    var svgCode by remember { mutableStateOf(defaultSvgTemplate) }
    var debouncedSvgCode by remember { mutableStateOf(defaultSvgTemplate) }
    var viewMode by remember { mutableStateOf(ViewMode.SPLIT) }
    var showTemplateDialog by remember { mutableStateOf(false) }
    var showAttributeEditor by remember { mutableStateOf(false) }
    var svgRenderError by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val clipboardManager = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Debounce SVG rendering
    LaunchedEffect(svgCode) {
        delay(400L)
        debouncedSvgCode = svgCode
        svgRenderError = false
    }

    // File picker - load SVG
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openInputStream(it)?.use { stream ->
                    val content = stream.bufferedReader().readText()
                    svgCode = content
                }
            } catch (e: Exception) {
                scope.launch {
                    snackbarHostState.showSnackbar("Failed to load file: ${e.message}")
                }
            }
        }
    }

    // File saver - export SVG
    val fileSaverLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/svg+xml")
    ) { uri ->
        uri?.let {
            try {
                context.contentResolver.openOutputStream(it)?.use { stream ->
                    stream.write(svgCode.toByteArray(Charsets.UTF_8))
                }
                scope.launch {
                    snackbarHostState.showSnackbar("SVG saved successfully")
                }
            } catch (e: Exception) {
                scope.launch {
                    snackbarHostState.showSnackbar("Failed to save: ${e.message}")
                }
            }
        }
    }

    // Template dialog
    if (showTemplateDialog) {
        AlertDialog(
            onDismissRequest = { showTemplateDialog = false },
            title = { Text("SVG Templates") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    svgTemplates.forEach { template ->
                        OutlinedCard(
                            onClick = {
                                svgCode = template.code
                                showTemplateDialog = false
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = template.name,
                                modifier = Modifier.padding(16.dp),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTemplateDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Attribute editor dialog
    if (showAttributeEditor) {
        SvgAttributeEditorDialog(
            svgCode = svgCode,
            onSvgCodeChange = { svgCode = it },
            onDismiss = { showAttributeEditor = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.svg_viewer_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    // Load file
                    IconButton(onClick = { filePickerLauncher.launch("image/svg+xml") }) {
                        Icon(Icons.Filled.FileOpen, "Load SVG")
                    }
                    // Save/Export
                    IconButton(onClick = { fileSaverLauncher.launch("image.svg") }) {
                        Icon(Icons.Filled.Save, "Save SVG")
                    }
                    // More options
                    var showMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { showMenu = true }) {
                        Icon(Icons.Filled.MoreVert, "More")
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Copy SVG Code") },
                            onClick = {
                                clipboardManager.setPrimaryClip(
                                    ClipData.newPlainText("SVG", svgCode)
                                )
                                scope.launch {
                                    snackbarHostState.showSnackbar("SVG code copied to clipboard")
                                }
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Filled.ContentCopy, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Format / Pretty Print") },
                            onClick = {
                                svgCode = formatSvgCode(svgCode)
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Filled.FormatAlignLeft, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Templates") },
                            onClick = {
                                showTemplateDialog = true
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Filled.Dashboard, null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Edit Attributes") },
                            onClick = {
                                showAttributeEditor = true
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Filled.Tune, null) }
                        )
                        HorizontalDivider()
                        DropdownMenuItem(
                            text = { Text("SVG Info") },
                            onClick = {
                                val info = getSvgInfo(svgCode)
                                scope.launch {
                                    snackbarHostState.showSnackbar(info)
                                }
                                showMenu = false
                            },
                            leadingIcon = { Icon(Icons.Filled.Info, null) }
                        )
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
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // View mode toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SingleChoiceSegmentedButtonRow {
                    SegmentedButton(
                        selected = viewMode == ViewMode.CODE,
                        onClick = { viewMode = ViewMode.CODE },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                        icon = {}
                    ) {
                        Icon(
                            Icons.Filled.Code,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Code")
                    }
                    SegmentedButton(
                        selected = viewMode == ViewMode.SPLIT,
                        onClick = { viewMode = ViewMode.SPLIT },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                        icon = {}
                    ) {
                        Icon(
                            Icons.Filled.Splitscreen,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Split")
                    }
                    SegmentedButton(
                        selected = viewMode == ViewMode.PREVIEW,
                        onClick = { viewMode = ViewMode.PREVIEW },
                        shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                        icon = {}
                    ) {
                        Icon(
                            Icons.Filled.Visibility,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Preview")
                    }
                }
            }

            // Quick-insert toolbar
            if (viewMode != ViewMode.PREVIEW) {
                QuickInsertToolbar(
                    onInsert = { element ->
                        svgCode = insertSvgElement(svgCode, element)
                    }
                )
            }

            // Editor and Preview
            when (viewMode) {
                ViewMode.SPLIT -> {
                    SvgCodeEditor(
                        code = svgCode,
                        onCodeChange = { svgCode = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                    SvgPreviewPane(
                        svgCode = debouncedSvgCode,
                        onError = { svgRenderError = true },
                        hasError = svgRenderError,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }

                ViewMode.CODE -> {
                    SvgCodeEditor(
                        code = svgCode,
                        onCodeChange = { svgCode = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }

                ViewMode.PREVIEW -> {
                    SvgPreviewPane(
                        svgCode = debouncedSvgCode,
                        onError = { svgRenderError = true },
                        hasError = svgRenderError,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }
            }

            BannerAd(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun QuickInsertToolbar(
    onInsert: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        svgElements.forEach { element ->
            SuggestionChip(
                onClick = { onInsert(element.snippet) },
                label = { Text(element.name, fontSize = 12.sp) }
            )
        }
    }
}

@Composable
private fun SvgCodeEditor(
    code: String,
    onCodeChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = code,
        onValueChange = onCodeChange,
        modifier = modifier,
        placeholder = { Text("Paste or type SVG code here...") },
        textStyle = LocalTextStyle.current.copy(
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
    )
}

@Composable
private fun SvgPreviewPane(
    svgCode: String,
    onError: () -> Unit,
    hasError: Boolean,
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            if (svgCode.isBlank()) {
                Text(
                    text = "Enter SVG code to preview",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                // Checkerboard-like background for transparency
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFE8E8E8))
                        .clipToBounds()
                        .pointerInput(Unit) {
                            detectTransformGestures { _, pan, zoom, _ ->
                                scale = (scale * zoom).coerceIn(0.25f, 5f)
                                offset = Offset(
                                    x = offset.x + pan.x,
                                    y = offset.y + pan.y
                                )
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val svgBytes = svgCode.toByteArray(Charsets.UTF_8)
                    val painter = rememberAsyncImagePainter(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(svgBytes)
                            .decoderFactory(SvgDecoder.Factory())
                            .size(Size.ORIGINAL)
                            .build(),
                        onError = { onError() }
                    )

                    Image(
                        painter = painter,
                        contentDescription = "SVG Preview",
                        modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer(
                                scaleX = scale,
                                scaleY = scale,
                                translationX = offset.x,
                                translationY = offset.y
                            ),
                        contentScale = ContentScale.Fit
                    )

                    if (hasError) {
                        Surface(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(8.dp),
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = "Invalid SVG - check your code",
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    // Reset zoom button
                    if (scale != 1f || offset != Offset.Zero) {
                        FilledTonalIconButton(
                            onClick = {
                                scale = 1f
                                offset = Offset.Zero
                            },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(4.dp)
                                .size(32.dp)
                        ) {
                            Icon(
                                Icons.Filled.CenterFocusWeak,
                                contentDescription = "Reset zoom",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SvgAttributeEditorDialog(
    svgCode: String,
    onSvgCodeChange: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val viewBoxRegex = Regex("""viewBox\s*=\s*"([^"]*?)"""")
    val widthRegex = Regex("""<svg[^>]*?\swidth\s*=\s*"([^"]*?)"""")
    val heightRegex = Regex("""<svg[^>]*?\sheight\s*=\s*"([^"]*?)"""")

    val currentViewBox = viewBoxRegex.find(svgCode)?.groupValues?.get(1) ?: "0 0 200 200"
    val viewBoxParts = currentViewBox.split(Regex("\\s+"))
    var vbX by remember { mutableStateOf(viewBoxParts.getOrElse(0) { "0" }) }
    var vbY by remember { mutableStateOf(viewBoxParts.getOrElse(1) { "0" }) }
    var vbW by remember { mutableStateOf(viewBoxParts.getOrElse(2) { "200" }) }
    var vbH by remember { mutableStateOf(viewBoxParts.getOrElse(3) { "200" }) }

    var svgWidth by remember { mutableStateOf(widthRegex.find(svgCode)?.groupValues?.get(1) ?: "200") }
    var svgHeight by remember { mutableStateOf(heightRegex.find(svgCode)?.groupValues?.get(1) ?: "200") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("SVG Attributes") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "viewBox",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = vbX,
                        onValueChange = { vbX = it },
                        label = { Text("X") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = vbY,
                        onValueChange = { vbY = it },
                        label = { Text("Y") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = vbW,
                        onValueChange = { vbW = it },
                        label = { Text("Width") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = vbH,
                        onValueChange = { vbH = it },
                        label = { Text("Height") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                HorizontalDivider()

                Text(
                    "SVG Dimensions",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = svgWidth,
                        onValueChange = { svgWidth = it },
                        label = { Text("Width") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = svgHeight,
                        onValueChange = { svgHeight = it },
                        label = { Text("Height") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                var updated = svgCode
                val newViewBox = "$vbX $vbY $vbW $vbH"

                // Update or add viewBox
                updated = if (viewBoxRegex.containsMatchIn(updated)) {
                    updated.replace(viewBoxRegex, "viewBox=\"$newViewBox\"")
                } else {
                    updated.replaceFirst("<svg", "<svg viewBox=\"$newViewBox\"")
                }

                // Update or add width
                updated = if (widthRegex.containsMatchIn(updated)) {
                    widthRegex.replace(updated) { match ->
                        match.value.replaceFirst(match.groupValues[1], svgWidth)
                    }
                } else {
                    updated.replaceFirst("<svg", "<svg width=\"$svgWidth\"")
                }

                // Update or add height
                updated = if (heightRegex.containsMatchIn(updated)) {
                    heightRegex.replace(updated) { match ->
                        match.value.replaceFirst(match.groupValues[1], svgHeight)
                    }
                } else {
                    updated.replaceFirst("<svg", "<svg height=\"$svgHeight\"")
                }

                onSvgCodeChange(updated)
                onDismiss()
            }) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

/**
 * Inserts an SVG element snippet before the closing </svg> tag.
 */
private fun insertSvgElement(svgCode: String, element: String): String {
    val closingTagIndex = svgCode.lastIndexOf("</svg>")
    return if (closingTagIndex >= 0) {
        val before = svgCode.substring(0, closingTagIndex)
        val after = svgCode.substring(closingTagIndex)
        val indent = "  "
        "${before.trimEnd()}\n$indent$element\n$after"
    } else {
        // No closing tag found; just append
        "$svgCode\n$element"
    }
}

/**
 * Simple SVG formatter: adds newlines after tags and indents nested elements.
 */
private fun formatSvgCode(svgCode: String): String {
    // First, collapse to a single line (remove existing formatting)
    val collapsed = svgCode
        .replace(Regex("\\s*\\n\\s*"), " ")
        .replace(Regex(">\\s+<"), ">\n<")
        .replace(Regex("/>\\s*"), "/>\n")

    val lines = collapsed.split("\n").filter { it.isNotBlank() }
    val result = StringBuilder()
    var indent = 0

    for (line in lines) {
        val trimmed = line.trim()
        if (trimmed.isEmpty()) continue

        // Decrease indent for closing tags
        if (trimmed.startsWith("</")) {
            indent = (indent - 1).coerceAtLeast(0)
        }

        result.append("  ".repeat(indent))
        result.appendLine(trimmed)

        // Increase indent for opening tags (not self-closing, not closing)
        if (trimmed.startsWith("<") &&
            !trimmed.startsWith("</") &&
            !trimmed.endsWith("/>") &&
            !trimmed.contains("</")
        ) {
            indent++
        }
    }

    return result.toString().trimEnd()
}

/**
 * Returns a short info string about the SVG: element count and estimated size.
 */
private fun getSvgInfo(svgCode: String): String {
    val elementCount = Regex("<[a-zA-Z][^/]*?>|<[a-zA-Z][^>]*?/>").findAll(svgCode).count()
    val sizeBytes = svgCode.toByteArray(Charsets.UTF_8).size
    val sizeStr = if (sizeBytes >= 1024) {
        "%.1f KB".format(sizeBytes / 1024.0)
    } else {
        "$sizeBytes bytes"
    }
    return "Elements: $elementCount | Size: $sizeStr"
}
