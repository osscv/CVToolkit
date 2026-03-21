package cv.toolkit.screens

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
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.*
import androidx.compose.ui.text.font.*
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import cv.toolkit.R
import cv.toolkit.ads.BannerAd
import kotlinx.coroutines.launch

private enum class ViewMode { Split, EditorOnly, PreviewOnly }

private val defaultSampleMarkdown = """
# Markdown Preview

## Features

This editor supports **bold**, *italic*, and `inline code` formatting.

### Links and Lists

Visit [Compose Docs](https://developer.android.com/jetpack/compose) for more info.

- First bullet item
- Second bullet item
- Third bullet item

1. Numbered item one
2. Numbered item two
3. Numbered item three

> This is a blockquote.
> It can span multiple lines.

### Code Block

```
fun main() {
    println("Hello, Markdown!")
}
```

---

That's a horizontal rule above. Enjoy writing Markdown!
""".trimIndent()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkdownPreviewScreen(navController: NavController) {
    var markdownText by remember { mutableStateOf(defaultSampleMarkdown) }
    var viewMode by remember { mutableStateOf(ViewMode.Split) }
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.markdown_preview_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    // Copy markdown button
                    IconButton(
                        onClick = {
                            scope.launch {
                                clipboard.setClipEntry(
                                    androidx.compose.ui.platform.ClipEntry(
                                        android.content.ClipData.newPlainText(
                                            "markdown",
                                            markdownText
                                        )
                                    )
                                )
                            }
                        },
                        enabled = markdownText.isNotEmpty()
                    ) {
                        Icon(Icons.Filled.ContentCopy, "Copy Markdown")
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
                        selected = viewMode == ViewMode.EditorOnly,
                        onClick = { viewMode = ViewMode.EditorOnly },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                        icon = {}
                    ) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text("Editor")
                    }
                    SegmentedButton(
                        selected = viewMode == ViewMode.Split,
                        onClick = { viewMode = ViewMode.Split },
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
                        selected = viewMode == ViewMode.PreviewOnly,
                        onClick = { viewMode = ViewMode.PreviewOnly },
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

            // Editor and Preview
            when (viewMode) {
                ViewMode.Split -> {
                    // Editor - top half
                    MarkdownEditor(
                        text = markdownText,
                        onTextChange = { markdownText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                    // Preview - bottom half
                    MarkdownPreviewPane(
                        markdown = markdownText,
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }

                ViewMode.EditorOnly -> {
                    MarkdownEditor(
                        text = markdownText,
                        onTextChange = { markdownText = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    )
                }

                ViewMode.PreviewOnly -> {
                    MarkdownPreviewPane(
                        markdown = markdownText,
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
private fun MarkdownEditor(
    text: String,
    onTextChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = text,
        onValueChange = onTextChange,
        modifier = modifier,
        placeholder = { Text("Enter Markdown here...") },
        textStyle = LocalTextStyle.current.copy(
            fontFamily = FontFamily.Monospace,
            fontSize = 14.sp
        )
    )
}

@Composable
private fun MarkdownPreviewPane(
    markdown: String,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val annotatedString = remember(markdown, colorScheme) {
        parseMarkdown(
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
                style = MaterialTheme.typography.bodyMedium.copy(
                    lineHeight = 22.sp
                )
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Markdown parser: converts markdown text into a Compose AnnotatedString
// ---------------------------------------------------------------------------

private fun parseMarkdown(
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
        if (line.trim().matches(Regex("^-{3,}$")) || line.trim().matches(Regex("^\\*{3,}$")) || line.trim().matches(Regex("^_{3,}$"))) {
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
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            ) {
                append("\u2502 ") // vertical bar as left border
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
                appendInlineMarkdown(itemText, linkColor, codeBackground, onSurface)
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
                appendInlineMarkdown(itemText, linkColor, codeBackground, onSurface)
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

        // --- regular paragraph line ---
        if (!firstBlock) append("\n")
        firstBlock = false
        appendInlineMarkdown(line, linkColor, codeBackground, onSurface)
        i++
    }
}

/**
 * Parses inline markdown formatting within a single line of text and appends
 * the styled result to the [AnnotatedString.Builder].
 *
 * Supports: **bold**, *italic*, `code`, and [link](url).
 */
private fun AnnotatedString.Builder.appendInlineMarkdown(
    text: String,
    linkColor: Color,
    codeBackground: Color,
    onSurface: Color
) {
    // Regex that matches inline markdown tokens in order of precedence.
    // Groups: 1=bold, 2=italic, 3=inline code, 4=link text, 5=link url
    val inlinePattern = Regex(
        """\*\*(.+?)\*\*""" +          // group 1: bold
        """|(?<!\*)\*(?!\*)(.+?)(?<!\*)\*(?!\*)""" + // group 2: italic
        """|`([^`]+)`""" +              // group 3: inline code
        """|\[([^\]]+)]\(([^)]+)\)"""   // group 4,5: link text, url
    )

    var lastIndex = 0
    val matches = inlinePattern.findAll(text)

    for (match in matches) {
        // Append plain text before this match
        if (match.range.first > lastIndex) {
            append(text.substring(lastIndex, match.range.first))
        }

        when {
            // **bold**
            match.groupValues[1].isNotEmpty() -> {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(match.groupValues[1])
                }
            }
            // *italic*
            match.groupValues[2].isNotEmpty() -> {
                withStyle(SpanStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)) {
                    append(match.groupValues[2])
                }
            }
            // `inline code`
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
            // [link](url)
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

    // Append any remaining plain text
    if (lastIndex < text.length) {
        append(text.substring(lastIndex))
    }
}
