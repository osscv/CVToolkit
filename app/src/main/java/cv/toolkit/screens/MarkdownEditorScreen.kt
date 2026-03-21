package cv.toolkit.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import cv.toolkit.R
import cv.toolkit.ads.BannerAd

private enum class EditorViewMode { Split, EditorOnly, PreviewOnly }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkdownEditorScreen(navController: NavController) {
    val context = LocalContext.current

    var textFieldValue by remember { mutableStateOf(TextFieldValue("")) }
    var currentFileName by remember { mutableStateOf<String?>(null) }
    var currentFileUri by remember { mutableStateOf<Uri?>(null) }
    var savedContent by remember { mutableStateOf("") }
    var viewMode by remember { mutableStateOf(EditorViewMode.Split) }
    var showFileMenu by remember { mutableStateOf(false) }
    var showExportMenu by remember { mutableStateOf(false) }

    val hasUnsavedChanges by remember {
        derivedStateOf { textFieldValue.text != savedContent }
    }

    // ---------- file launchers ----------

    val openFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        try {
            val content = context.contentResolver.openInputStream(uri)
                ?.bufferedReader()?.use { it.readText() } ?: return@rememberLauncherForActivityResult
            textFieldValue = TextFieldValue(content)
            savedContent = content
            currentFileUri = uri
            currentFileName = getFileName(context, uri)
        } catch (_: Exception) { }
    }

    val saveAsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/markdown")
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        try {
            context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use {
                it.write(textFieldValue.text)
            }
            savedContent = textFieldValue.text
            currentFileUri = uri
            currentFileName = getFileName(context, uri)
        } catch (_: Exception) { }
    }

    val exportHtmlLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/html")
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        try {
            val html = markdownToHtml(textFieldValue.text)
            context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use {
                it.write(html)
            }
        } catch (_: Exception) { }
    }

    // ---------- helper: save to current uri ----------

    fun saveToCurrentUri() {
        val uri = currentFileUri ?: return
        try {
            context.contentResolver.openOutputStream(uri, "wt")?.bufferedWriter()?.use {
                it.write(textFieldValue.text)
            }
            savedContent = textFieldValue.text
        } catch (_: Exception) { }
    }

    // ---------- helper: insert markdown syntax ----------

    fun insertMarkdown(prefix: String, suffix: String = "") {
        val selection = textFieldValue.selection
        val text = textFieldValue.text
        val selStart = selection.min
        val selEnd = selection.max
        val selectedText = text.substring(selStart, selEnd)
        val newText = text.substring(0, selStart) + prefix + selectedText + suffix + text.substring(selEnd)
        val newCursor = selStart + prefix.length + selectedText.length + suffix.length
        textFieldValue = TextFieldValue(
            text = newText,
            selection = TextRange(newCursor)
        )
    }

    fun insertMarkdownWrap(before: String, after: String) {
        val selection = textFieldValue.selection
        val text = textFieldValue.text
        val selStart = selection.min
        val selEnd = selection.max
        val selectedText = text.substring(selStart, selEnd)
        val newText = text.substring(0, selStart) + before + selectedText + after + text.substring(selEnd)
        val newCursorStart = selStart + before.length
        val newCursorEnd = newCursorStart + selectedText.length
        textFieldValue = TextFieldValue(
            text = newText,
            selection = TextRange(newCursorStart, newCursorEnd)
        )
    }

    // ---------- stats ----------

    val wordCount by remember {
        derivedStateOf {
            textFieldValue.text.trim().let { t ->
                if (t.isEmpty()) 0 else t.split(Regex("\\s+")).size
            }
        }
    }
    val lineCount by remember {
        derivedStateOf { if (textFieldValue.text.isEmpty()) 0 else textFieldValue.text.lines().size }
    }
    val charCount by remember {
        derivedStateOf { textFieldValue.text.length }
    }

    // ---------- UI ----------

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(R.string.markdown_editor_title))
                        if (currentFileName != null || hasUnsavedChanges) {
                            Text(
                                text = (currentFileName ?: "Untitled") +
                                        if (hasUnsavedChanges) " *" else "",
                                style = MaterialTheme.typography.bodySmall,
                                color = if (hasUnsavedChanges)
                                    MaterialTheme.colorScheme.error
                                else
                                    MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    // File menu
                    Box {
                        IconButton(onClick = { showFileMenu = true }) {
                            Icon(Icons.Filled.FolderOpen, "File")
                        }
                        DropdownMenu(
                            expanded = showFileMenu,
                            onDismissRequest = { showFileMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("New") },
                                onClick = {
                                    showFileMenu = false
                                    textFieldValue = TextFieldValue("")
                                    savedContent = ""
                                    currentFileUri = null
                                    currentFileName = null
                                },
                                leadingIcon = { Icon(Icons.Filled.NoteAdd, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Open") },
                                onClick = {
                                    showFileMenu = false
                                    openFileLauncher.launch("text/*")
                                },
                                leadingIcon = { Icon(Icons.Filled.FileOpen, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Save") },
                                onClick = {
                                    showFileMenu = false
                                    if (currentFileUri != null) {
                                        saveToCurrentUri()
                                    } else {
                                        saveAsLauncher.launch("document.md")
                                    }
                                },
                                leadingIcon = { Icon(Icons.Filled.Save, null) },
                                enabled = hasUnsavedChanges
                            )
                            DropdownMenuItem(
                                text = { Text("Save As") },
                                onClick = {
                                    showFileMenu = false
                                    saveAsLauncher.launch(currentFileName ?: "document.md")
                                },
                                leadingIcon = { Icon(Icons.Filled.SaveAs, null) }
                            )
                        }
                    }

                    // Export menu
                    Box {
                        IconButton(onClick = { showExportMenu = true }) {
                            Icon(Icons.Filled.IosShare, "Export")
                        }
                        DropdownMenu(
                            expanded = showExportMenu,
                            onDismissRequest = { showExportMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Export as HTML") },
                                onClick = {
                                    showExportMenu = false
                                    val name = (currentFileName?.removeSuffix(".md") ?: "document") + ".html"
                                    exportHtmlLauncher.launch(name)
                                },
                                leadingIcon = { Icon(Icons.Filled.Code, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Copy as HTML") },
                                onClick = {
                                    showExportMenu = false
                                    val html = markdownToHtml(textFieldValue.text)
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(ClipData.newPlainText("html", html))
                                },
                                leadingIcon = { Icon(Icons.Filled.ContentCopy, null) }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = {
            Column {
                // Status bar
                Surface(
                    tonalElevation = 2.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Words: $wordCount",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Lines: $lineCount",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Chars: $charCount",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                BannerAd(modifier = Modifier.fillMaxWidth())
            }
        },
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // ---------- Toolbar ----------
            EditorMarkdownToolbar(
                onBold = { insertMarkdownWrap("**", "**") },
                onItalic = { insertMarkdownWrap("*", "*") },
                onStrikethrough = { insertMarkdownWrap("~~", "~~") },
                onH1 = { insertMarkdown("# ") },
                onH2 = { insertMarkdown("## ") },
                onH3 = { insertMarkdown("### ") },
                onBulletList = { insertMarkdown("- ") },
                onNumberedList = { insertMarkdown("1. ") },
                onCodeBlock = { insertMarkdownWrap("```\n", "\n```") },
                onInlineCode = { insertMarkdownWrap("`", "`") },
                onLink = { insertMarkdown("[", "](url)") },
                onImage = { insertMarkdown("![alt](", ")") },
                onHorizontalRule = { insertMarkdown("\n---\n") },
                onQuote = { insertMarkdown("> ") },
                onTable = {
                    insertMarkdown(
                        "| Header 1 | Header 2 | Header 3 |\n" +
                                "| -------- | -------- | -------- |\n" +
                                "| Cell 1   | Cell 2   | Cell 3   |\n" +
                                "| Cell 4   | Cell 5   | Cell 6   |\n"
                    )
                }
            )

            // ---------- View mode toggle ----------
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SingleChoiceSegmentedButtonRow {
                    SegmentedButton(
                        selected = viewMode == EditorViewMode.EditorOnly,
                        onClick = { viewMode = EditorViewMode.EditorOnly },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                        icon = {}
                    ) {
                        Icon(Icons.Filled.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Editor")
                    }
                    SegmentedButton(
                        selected = viewMode == EditorViewMode.Split,
                        onClick = { viewMode = EditorViewMode.Split },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                        icon = {}
                    ) {
                        Icon(Icons.Filled.Splitscreen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Split")
                    }
                    SegmentedButton(
                        selected = viewMode == EditorViewMode.PreviewOnly,
                        onClick = { viewMode = EditorViewMode.PreviewOnly },
                        shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                        icon = {}
                    ) {
                        Icon(Icons.Filled.Visibility, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Preview")
                    }
                }
            }

            // ---------- Editor / Preview ----------
            when (viewMode) {
                EditorViewMode.Split -> {
                    EditorTextField(
                        value = textFieldValue,
                        onValueChange = { textFieldValue = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                    EditorPreviewPane(
                        markdown = textFieldValue.text,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }
                EditorViewMode.EditorOnly -> {
                    EditorTextField(
                        value = textFieldValue,
                        onValueChange = { textFieldValue = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }
                EditorViewMode.PreviewOnly -> {
                    EditorPreviewPane(
                        markdown = textFieldValue.text,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Markdown toolbar
// ---------------------------------------------------------------------------

@Composable
private fun EditorMarkdownToolbar(
    onBold: () -> Unit,
    onItalic: () -> Unit,
    onStrikethrough: () -> Unit,
    onH1: () -> Unit,
    onH2: () -> Unit,
    onH3: () -> Unit,
    onBulletList: () -> Unit,
    onNumberedList: () -> Unit,
    onCodeBlock: () -> Unit,
    onInlineCode: () -> Unit,
    onLink: () -> Unit,
    onImage: () -> Unit,
    onHorizontalRule: () -> Unit,
    onQuote: () -> Unit,
    onTable: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        ToolbarChip("B", onClick = onBold, bold = true)
        ToolbarChip("I", onClick = onItalic, italic = true)
        ToolbarChip("S", onClick = onStrikethrough, strikethrough = true)
        ToolbarDivider()
        ToolbarChip("H1", onClick = onH1)
        ToolbarChip("H2", onClick = onH2)
        ToolbarChip("H3", onClick = onH3)
        ToolbarDivider()
        ToolbarIconButton(Icons.Filled.FormatListBulleted, "Bullet list", onBulletList)
        ToolbarIconButton(Icons.Filled.FormatListNumbered, "Numbered list", onNumberedList)
        ToolbarDivider()
        ToolbarChip("< >", onClick = onCodeBlock)
        ToolbarChip("`", onClick = onInlineCode)
        ToolbarDivider()
        ToolbarIconButton(Icons.Filled.Link, "Link", onLink)
        ToolbarIconButton(Icons.Filled.Image, "Image", onImage)
        ToolbarIconButton(Icons.Filled.HorizontalRule, "Horizontal rule", onHorizontalRule)
        ToolbarIconButton(Icons.Filled.FormatQuote, "Quote", onQuote)
        ToolbarIconButton(Icons.Filled.TableChart, "Table", onTable)
    }
}

@Composable
private fun ToolbarChip(
    label: String,
    onClick: () -> Unit,
    bold: Boolean = false,
    italic: Boolean = false,
    strikethrough: Boolean = false
) {
    AssistChip(
        onClick = onClick,
        label = {
            Text(
                text = label,
                fontWeight = if (bold) FontWeight.Bold else null,
                fontStyle = if (italic) FontStyle.Italic else null,
                textDecoration = if (strikethrough) TextDecoration.LineThrough else null,
                style = MaterialTheme.typography.labelSmall
            )
        },
        modifier = Modifier.height(32.dp)
    )
}

@Composable
private fun ToolbarIconButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier.size(32.dp)
    ) {
        Icon(icon, contentDescription = contentDescription, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun ToolbarDivider() {
    Spacer(modifier = Modifier.width(2.dp))
    VerticalDivider(modifier = Modifier.height(24.dp))
    Spacer(modifier = Modifier.width(2.dp))
}

// ---------------------------------------------------------------------------
// Editor text field
// ---------------------------------------------------------------------------

@Composable
private fun EditorTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier,
        placeholder = { Text("Start writing Markdown...") },
        textStyle = LocalTextStyle.current.copy(
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp
        )
    )
}

// ---------------------------------------------------------------------------
// Preview pane
// ---------------------------------------------------------------------------

@Composable
private fun EditorPreviewPane(
    markdown: String,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val annotatedString = remember(markdown, colorScheme) {
        editorParseMarkdown(
            markdown = markdown,
            linkColor = colorScheme.primary,
            codeBackground = colorScheme.surfaceVariant,
            blockquoteColor = colorScheme.primary.copy(alpha = 0.6f),
            onSurface = colorScheme.onSurface,
            onSurfaceVariant = colorScheme.onSurfaceVariant,
            ruleColor = colorScheme.outlineVariant
        )
    }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        SelectionContainer {
            Text(
                text = annotatedString,
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                style = MaterialTheme.typography.bodyMedium.copy(lineHeight = 22.sp)
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Markdown parser -> AnnotatedString
// ---------------------------------------------------------------------------

private fun editorParseMarkdown(
    markdown: String,
    linkColor: Color,
    codeBackground: Color,
    blockquoteColor: Color,
    onSurface: Color,
    onSurfaceVariant: Color,
    ruleColor: Color
): AnnotatedString = buildAnnotatedString {
    val lines = markdown.lines()
    var i = 0
    var firstBlock = true

    while (i < lines.size) {
        val line = lines[i]

        // --- fenced code block ---
        if (line.trimStart().startsWith("```")) {
            if (!firstBlock) append("\n")
            firstBlock = false
            val codeLines = mutableListOf<String>()
            i++ // skip opening fence
            while (i < lines.size && !lines[i].trimStart().startsWith("```")) {
                codeLines.add(lines[i])
                i++
            }
            if (i < lines.size) i++ // skip closing fence
            withStyle(
                SpanStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    background = codeBackground
                )
            ) {
                append(codeLines.joinToString("\n"))
            }
            continue
        }

        // --- horizontal rule ---
        if (line.trim().matches(Regex("^-{3,}$")) ||
            line.trim().matches(Regex("^\\*{3,}$")) ||
            line.trim().matches(Regex("^_{3,}$"))
        ) {
            if (!firstBlock) append("\n")
            firstBlock = false
            withStyle(SpanStyle(color = ruleColor, fontSize = 8.sp)) {
                append("\u2500".repeat(40))
            }
            i++
            continue
        }

        // --- headings ---
        val headingMatch = Regex("^(#{1,3})\\s+(.*)").matchEntire(line)
        if (headingMatch != null) {
            if (!firstBlock) append("\n")
            firstBlock = false
            val level = headingMatch.groupValues[1].length
            val headingText = headingMatch.groupValues[2]
            val fontSize = when (level) {
                1 -> 26.sp
                2 -> 22.sp
                else -> 18.sp
            }
            withStyle(
                SpanStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = fontSize,
                    color = onSurface
                )
            ) {
                append(headingText)
            }
            i++
            continue
        }

        // --- blockquote ---
        if (line.trimStart().startsWith("> ") || line.trimStart() == ">") {
            if (!firstBlock) append("\n")
            firstBlock = false
            val quoteLines = mutableListOf<String>()
            var j = i
            while (j < lines.size && (lines[j].trimStart().startsWith("> ") || lines[j].trimStart() == ">")) {
                val content = lines[j].trimStart().removePrefix("> ").removePrefix(">")
                quoteLines.add(content)
                j++
            }
            withStyle(
                SpanStyle(
                    color = blockquoteColor,
                    fontStyle = FontStyle.Italic
                )
            ) {
                append("\u2502 ")
                append(quoteLines.joinToString("\n\u2502 "))
            }
            i = j
            continue
        }

        // --- unordered list ---
        if (line.trimStart().matches(Regex("^[-*+]\\s+.*"))) {
            if (!firstBlock) append("\n")
            firstBlock = false
            var j = i
            while (j < lines.size && lines[j].trimStart().matches(Regex("^[-*+]\\s+.*"))) {
                if (j > i) append("\n")
                val itemText = lines[j].trimStart().replace(Regex("^[-*+]\\s+"), "")
                append("  \u2022 ")
                editorAppendInlineMarkdown(itemText, linkColor, codeBackground, onSurface)
                j++
            }
            i = j
            continue
        }

        // --- ordered list ---
        if (line.trimStart().matches(Regex("^\\d+\\.\\s+.*"))) {
            if (!firstBlock) append("\n")
            firstBlock = false
            var j = i
            var number = 1
            while (j < lines.size && lines[j].trimStart().matches(Regex("^\\d+\\.\\s+.*"))) {
                if (j > i) append("\n")
                val itemText = lines[j].trimStart().replace(Regex("^\\d+\\.\\s+"), "")
                append("  $number. ")
                editorAppendInlineMarkdown(itemText, linkColor, codeBackground, onSurface)
                number++
                j++
            }
            i = j
            continue
        }

        // --- blank line ---
        if (line.isBlank()) {
            if (!firstBlock) append("\n")
            i++
            continue
        }

        // --- regular paragraph ---
        if (!firstBlock) append("\n")
        firstBlock = false
        editorAppendInlineMarkdown(line, linkColor, codeBackground, onSurface)
        i++
    }
}

private fun AnnotatedString.Builder.editorAppendInlineMarkdown(
    text: String,
    linkColor: Color,
    codeBackground: Color,
    onSurface: Color
) {
    val inlinePattern = Regex(
        """\*\*(.+?)\*\*""" +
                """|(?<!\*)\*(?!\*)(.+?)(?<!\*)\*(?!\*)""" +
                """|`([^`]+)`""" +
                """|\[([^\]]+)]\(([^)]+)\)"""
    )

    var lastIndex = 0
    val matches = inlinePattern.findAll(text)

    for (match in matches) {
        if (match.range.first > lastIndex) {
            append(text.substring(lastIndex, match.range.first))
        }
        when {
            match.groupValues[1].isNotEmpty() -> {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(match.groupValues[1])
                }
            }
            match.groupValues[2].isNotEmpty() -> {
                withStyle(SpanStyle(fontStyle = FontStyle.Italic)) {
                    append(match.groupValues[2])
                }
            }
            match.groupValues[3].isNotEmpty() -> {
                withStyle(
                    SpanStyle(
                        fontFamily = FontFamily.Monospace,
                        background = codeBackground,
                        fontSize = 13.sp
                    )
                ) {
                    append(" ${match.groupValues[3]} ")
                }
            }
            match.groupValues[4].isNotEmpty() -> {
                pushStringAnnotation(tag = "URL", annotation = match.groupValues[5])
                withStyle(
                    SpanStyle(
                        color = linkColor,
                        textDecoration = TextDecoration.Underline
                    )
                ) {
                    append(match.groupValues[4])
                }
                pop()
            }
        }
        lastIndex = match.range.last + 1
    }

    if (lastIndex < text.length) {
        append(text.substring(lastIndex))
    }
}

// ---------------------------------------------------------------------------
// Markdown -> HTML export
// ---------------------------------------------------------------------------

private fun markdownToHtml(markdown: String): String {
    val sb = StringBuilder()
    sb.appendLine("<!DOCTYPE html>")
    sb.appendLine("<html lang=\"en\">")
    sb.appendLine("<head>")
    sb.appendLine("<meta charset=\"UTF-8\">")
    sb.appendLine("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">")
    sb.appendLine("<title>Markdown Export</title>")
    sb.appendLine("<style>")
    sb.appendLine("body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; max-width: 800px; margin: 0 auto; padding: 20px; line-height: 1.6; color: #333; }")
    sb.appendLine("h1, h2, h3 { margin-top: 1.2em; margin-bottom: 0.6em; }")
    sb.appendLine("h1 { font-size: 2em; border-bottom: 2px solid #eee; padding-bottom: 0.3em; }")
    sb.appendLine("h2 { font-size: 1.5em; border-bottom: 1px solid #eee; padding-bottom: 0.3em; }")
    sb.appendLine("h3 { font-size: 1.25em; }")
    sb.appendLine("code { background: #f4f4f4; padding: 2px 6px; border-radius: 4px; font-size: 0.9em; }")
    sb.appendLine("pre { background: #f4f4f4; padding: 16px; border-radius: 8px; overflow-x: auto; }")
    sb.appendLine("pre code { background: none; padding: 0; }")
    sb.appendLine("blockquote { border-left: 4px solid #ddd; margin: 0; padding: 0.5em 1em; color: #666; }")
    sb.appendLine("hr { border: none; border-top: 2px solid #eee; margin: 1.5em 0; }")
    sb.appendLine("a { color: #0366d6; text-decoration: none; }")
    sb.appendLine("a:hover { text-decoration: underline; }")
    sb.appendLine("ul, ol { padding-left: 2em; }")
    sb.appendLine("table { border-collapse: collapse; width: 100%; margin: 1em 0; }")
    sb.appendLine("th, td { border: 1px solid #ddd; padding: 8px 12px; text-align: left; }")
    sb.appendLine("th { background: #f4f4f4; font-weight: bold; }")
    sb.appendLine("img { max-width: 100%; }")
    sb.appendLine("</style>")
    sb.appendLine("</head>")
    sb.appendLine("<body>")

    val lines = markdown.lines()
    var i = 0

    while (i < lines.size) {
        val line = lines[i]

        // fenced code block
        if (line.trimStart().startsWith("```")) {
            val codeLines = mutableListOf<String>()
            i++
            while (i < lines.size && !lines[i].trimStart().startsWith("```")) {
                codeLines.add(escapeHtml(lines[i]))
                i++
            }
            if (i < lines.size) i++
            sb.appendLine("<pre><code>${codeLines.joinToString("\n")}</code></pre>")
            continue
        }

        // horizontal rule
        if (line.trim().matches(Regex("^-{3,}$")) ||
            line.trim().matches(Regex("^\\*{3,}$")) ||
            line.trim().matches(Regex("^_{3,}$"))
        ) {
            sb.appendLine("<hr>")
            i++
            continue
        }

        // headings
        val headingMatch = Regex("^(#{1,3})\\s+(.*)").matchEntire(line)
        if (headingMatch != null) {
            val level = headingMatch.groupValues[1].length
            val text = inlineMarkdownToHtml(headingMatch.groupValues[2])
            sb.appendLine("<h$level>$text</h$level>")
            i++
            continue
        }

        // blockquote
        if (line.trimStart().startsWith("> ") || line.trimStart() == ">") {
            val quoteLines = mutableListOf<String>()
            var j = i
            while (j < lines.size && (lines[j].trimStart().startsWith("> ") || lines[j].trimStart() == ">")) {
                val content = lines[j].trimStart().removePrefix("> ").removePrefix(">")
                quoteLines.add(inlineMarkdownToHtml(content))
                j++
            }
            sb.appendLine("<blockquote><p>${quoteLines.joinToString("<br>")}</p></blockquote>")
            i = j
            continue
        }

        // unordered list
        if (line.trimStart().matches(Regex("^[-*+]\\s+.*"))) {
            sb.appendLine("<ul>")
            var j = i
            while (j < lines.size && lines[j].trimStart().matches(Regex("^[-*+]\\s+.*"))) {
                val itemText = lines[j].trimStart().replace(Regex("^[-*+]\\s+"), "")
                sb.appendLine("<li>${inlineMarkdownToHtml(itemText)}</li>")
                j++
            }
            sb.appendLine("</ul>")
            i = j
            continue
        }

        // ordered list
        if (line.trimStart().matches(Regex("^\\d+\\.\\s+.*"))) {
            sb.appendLine("<ol>")
            var j = i
            while (j < lines.size && lines[j].trimStart().matches(Regex("^\\d+\\.\\s+.*"))) {
                val itemText = lines[j].trimStart().replace(Regex("^\\d+\\.\\s+"), "")
                sb.appendLine("<li>${inlineMarkdownToHtml(itemText)}</li>")
                j++
            }
            sb.appendLine("</ol>")
            i = j
            continue
        }

        // blank line
        if (line.isBlank()) {
            i++
            continue
        }

        // paragraph
        val paraLines = mutableListOf<String>()
        var j = i
        while (j < lines.size && lines[j].isNotBlank() &&
            !lines[j].trimStart().startsWith("#") &&
            !lines[j].trimStart().startsWith("```") &&
            !lines[j].trimStart().startsWith("> ") &&
            !lines[j].trimStart().matches(Regex("^[-*+]\\s+.*")) &&
            !lines[j].trimStart().matches(Regex("^\\d+\\.\\s+.*")) &&
            !lines[j].trim().matches(Regex("^-{3,}$")) &&
            !lines[j].trim().matches(Regex("^\\*{3,}$")) &&
            !lines[j].trim().matches(Regex("^_{3,}$"))
        ) {
            paraLines.add(inlineMarkdownToHtml(lines[j]))
            j++
        }
        if (paraLines.isNotEmpty()) {
            sb.appendLine("<p>${paraLines.joinToString(" ")}</p>")
        }
        i = j
    }

    sb.appendLine("</body>")
    sb.appendLine("</html>")
    return sb.toString()
}

private fun escapeHtml(text: String): String =
    text.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")

private fun inlineMarkdownToHtml(text: String): String {
    var result = escapeHtml(text)

    // images: ![alt](url)
    result = Regex("""!\[([^\]]*?)]\(([^)]+)\)""").replace(result) { m ->
        "<img src=\"${m.groupValues[2]}\" alt=\"${m.groupValues[1]}\">"
    }

    // links: [text](url)
    result = Regex("""\[([^\]]+?)]\(([^)]+)\)""").replace(result) { m ->
        "<a href=\"${m.groupValues[2]}\">${m.groupValues[1]}</a>"
    }

    // bold
    result = Regex("""\*\*(.+?)\*\*""").replace(result) { m ->
        "<strong>${m.groupValues[1]}</strong>"
    }

    // strikethrough
    result = Regex("""~~(.+?)~~""").replace(result) { m ->
        "<del>${m.groupValues[1]}</del>"
    }

    // italic
    result = Regex("""(?<!\*)\*(?!\*)(.+?)(?<!\*)\*(?!\*)""").replace(result) { m ->
        "<em>${m.groupValues[1]}</em>"
    }

    // inline code
    result = Regex("""`([^`]+)`""").replace(result) { m ->
        "<code>${m.groupValues[1]}</code>"
    }

    return result
}

// ---------------------------------------------------------------------------
// Utility
// ---------------------------------------------------------------------------

private fun getFileName(context: Context, uri: Uri): String? {
    if (uri.scheme == "content") {
        context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (nameIndex >= 0) return cursor.getString(nameIndex)
            }
        }
    }
    return uri.lastPathSegment
}
