package cv.toolkit.screens

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Redo
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import cv.toolkit.R
import cv.toolkit.ads.BannerAd
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

private enum class TextSizeOption(val label: String, val size: Int) {
    Small("S", 12),
    Medium("M", 14),
    Large("L", 18)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextEditorScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // ----- Editor state -----
    var textFieldValue by remember { mutableStateOf(TextFieldValue("")) }
    var hasUnsavedChanges by remember { mutableStateOf(false) }
    var currentUri by remember { mutableStateOf<Uri?>(null) }
    var currentFileName by remember { mutableStateOf<String?>(null) }
    var currentFileSize by remember { mutableStateOf<Long?>(null) }
    var currentEncoding by remember { mutableStateOf("UTF-8") }
    var wordWrap by remember { mutableStateOf(true) }
    var textSizeOption by remember { mutableStateOf(TextSizeOption.Medium) }

    // ----- Undo / Redo -----
    val undoStack = remember { mutableStateListOf<String>() }
    val redoStack = remember { mutableStateListOf<String>() }
    var lastRecordedText by remember { mutableStateOf("") }
    var lastChangeTime by remember { mutableLongStateOf(0L) }

    // Debounced undo snapshot
    LaunchedEffect(textFieldValue.text) {
        val current = textFieldValue.text
        if (current != lastRecordedText) {
            delay(500)
            if (textFieldValue.text == current && current != lastRecordedText) {
                if (undoStack.size >= 50) undoStack.removeAt(0)
                undoStack.add(lastRecordedText)
                redoStack.clear()
                lastRecordedText = current
            }
        }
    }

    fun undo() {
        if (undoStack.isNotEmpty()) {
            val previous = undoStack.removeAt(undoStack.lastIndex)
            redoStack.add(textFieldValue.text)
            lastRecordedText = previous
            textFieldValue = TextFieldValue(previous, TextRange(previous.length))
        }
    }

    fun redo() {
        if (redoStack.isNotEmpty()) {
            val next = redoStack.removeAt(redoStack.lastIndex)
            undoStack.add(textFieldValue.text)
            lastRecordedText = next
            textFieldValue = TextFieldValue(next, TextRange(next.length))
        }
    }

    // ----- Find & Replace -----
    var showFindReplace by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var replaceQuery by remember { mutableStateOf("") }
    var caseSensitive by remember { mutableStateOf(false) }
    var currentMatchIndex by remember { mutableIntStateOf(0) }
    val matchPositions = remember { mutableStateListOf<IntRange>() }

    fun updateMatches() {
        matchPositions.clear()
        currentMatchIndex = 0
        if (searchQuery.isEmpty()) return
        val text = textFieldValue.text
        val options = if (caseSensitive) emptySet() else setOf(RegexOption.IGNORE_CASE)
        try {
            val regex = Regex(Regex.escape(searchQuery), options)
            regex.findAll(text).forEach { matchPositions.add(it.range) }
        } catch (_: Exception) {
            // ignore bad patterns
        }
    }

    fun navigateMatch(forward: Boolean) {
        if (matchPositions.isEmpty()) return
        currentMatchIndex = if (forward) {
            (currentMatchIndex + 1) % matchPositions.size
        } else {
            (currentMatchIndex - 1 + matchPositions.size) % matchPositions.size
        }
        // Move cursor to the match
        val range = matchPositions[currentMatchIndex]
        textFieldValue = textFieldValue.copy(selection = TextRange(range.first, range.last + 1))
    }

    fun replaceCurrent() {
        if (matchPositions.isEmpty()) return
        val range = matchPositions[currentMatchIndex]
        val text = textFieldValue.text
        val newText = text.substring(0, range.first) + replaceQuery + text.substring(range.last + 1)
        textFieldValue = TextFieldValue(newText, TextRange(range.first + replaceQuery.length))
        hasUnsavedChanges = true
        updateMatches()
    }

    fun replaceAll() {
        if (matchPositions.isEmpty()) return
        val options = if (caseSensitive) emptySet() else setOf(RegexOption.IGNORE_CASE)
        val regex = Regex(Regex.escape(searchQuery), options)
        val newText = regex.replace(textFieldValue.text, replaceQuery)
        textFieldValue = TextFieldValue(newText, TextRange(newText.length))
        hasUnsavedChanges = true
        updateMatches()
    }

    // Update matches when search query or text changes
    LaunchedEffect(searchQuery, caseSensitive, textFieldValue.text) {
        updateMatches()
    }

    // ----- Go to line dialog -----
    var showGoToLineDialog by remember { mutableStateOf(false) }
    var goToLineInput by remember { mutableStateOf("") }

    // ----- File I/O -----
    fun readFileContent(uri: Uri): String {
        return context.contentResolver.openInputStream(uri)?.use { stream ->
            stream.bufferedReader(Charsets.UTF_8).readText()
        } ?: ""
    }

    fun writeFileContent(uri: Uri, content: String) {
        context.contentResolver.openOutputStream(uri, "wt")?.use { stream ->
            stream.bufferedWriter(Charsets.UTF_8).use { it.write(content) }
        }
    }

    fun getFileName(uri: Uri): String {
        var name = "Untitled.txt"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (cursor.moveToFirst() && nameIndex >= 0) {
                name = cursor.getString(nameIndex)
            }
        }
        return name
    }

    fun getFileSize(uri: Uri): Long? {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (cursor.moveToFirst() && sizeIndex >= 0) {
                return cursor.getLong(sizeIndex)
            }
        }
        return null
    }

    // Open file launcher
    val openFileLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                try {
                    val content = readFileContent(it)
                    textFieldValue = TextFieldValue(content, TextRange(0))
                    lastRecordedText = content
                    undoStack.clear()
                    redoStack.clear()
                    currentUri = it
                    currentFileName = getFileName(it)
                    currentFileSize = getFileSize(it)
                    hasUnsavedChanges = false
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Failed to open file: ${e.message}")
                }
            }
        }
    }

    // Save As launcher
    val saveAsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/plain")
    ) { uri ->
        uri?.let {
            scope.launch {
                try {
                    writeFileContent(it, textFieldValue.text)
                    currentUri = it
                    currentFileName = getFileName(it)
                    currentFileSize = textFieldValue.text.toByteArray(Charsets.UTF_8).size.toLong()
                    hasUnsavedChanges = false
                    snackbarHostState.showSnackbar("File saved")
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Failed to save: ${e.message}")
                }
            }
        }
    }

    fun saveFile() {
        val uri = currentUri
        if (uri != null) {
            scope.launch {
                try {
                    writeFileContent(uri, textFieldValue.text)
                    currentFileSize = textFieldValue.text.toByteArray(Charsets.UTF_8).size.toLong()
                    hasUnsavedChanges = false
                    snackbarHostState.showSnackbar("File saved")
                } catch (e: Exception) {
                    // If writing to the existing URI fails, fall back to Save As
                    saveAsLauncher.launch(currentFileName ?: "untitled.txt")
                }
            }
        } else {
            saveAsLauncher.launch("untitled.txt")
        }
    }

    fun newFile() {
        textFieldValue = TextFieldValue("", TextRange(0))
        lastRecordedText = ""
        undoStack.clear()
        redoStack.clear()
        currentUri = null
        currentFileName = null
        currentFileSize = null
        hasUnsavedChanges = false
    }

    fun insertTimestamp() {
        val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        val text = textFieldValue.text
        val cursor = textFieldValue.selection.start
        val newText = text.substring(0, cursor) + timestamp + text.substring(cursor)
        textFieldValue = TextFieldValue(newText, TextRange(cursor + timestamp.length))
        hasUnsavedChanges = true
    }

    fun goToLine(lineNumber: Int) {
        val lines = textFieldValue.text.split("\n")
        if (lineNumber in 1..lines.size) {
            var offset = 0
            for (i in 0 until lineNumber - 1) {
                offset += lines[i].length + 1 // +1 for the newline
            }
            textFieldValue = textFieldValue.copy(selection = TextRange(offset))
        }
    }

    // ----- Computed stats -----
    val text = textFieldValue.text
    val lineCount = if (text.isEmpty()) 0 else text.split("\n").size
    val wordCount = if (text.isBlank()) 0 else text.trim().split(Regex("\\s+")).size
    val charCount = text.length

    // ----- Overflow menu -----
    var showOverflowMenu by remember { mutableStateOf(false) }

    // ----- Scroll state for editor -----
    val verticalScrollState = rememberScrollState()
    val horizontalScrollState = rememberScrollState()
    val lineNumberScrollState = rememberScrollState()

    // Sync line number scroll with editor scroll
    LaunchedEffect(verticalScrollState.value) {
        lineNumberScrollState.scrollTo(verticalScrollState.value)
    }

    // ----- Title -----
    val baseTitle = stringResource(R.string.text_editor_title)
    val displayTitle = if (hasUnsavedChanges) "$baseTitle *" else baseTitle

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(displayTitle, maxLines = 1)
                        if (currentFileName != null) {
                            Text(
                                text = currentFileName ?: "",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
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
                    // Undo
                    IconButton(onClick = { undo() }, enabled = undoStack.isNotEmpty()) {
                        Icon(Icons.AutoMirrored.Filled.Undo, "Undo")
                    }
                    // Redo
                    IconButton(onClick = { redo() }, enabled = redoStack.isNotEmpty()) {
                        Icon(Icons.AutoMirrored.Filled.Redo, "Redo")
                    }
                    // Find
                    IconButton(onClick = { showFindReplace = !showFindReplace }) {
                        Icon(Icons.Filled.Search, "Find & Replace")
                    }
                    // Overflow menu
                    Box {
                        IconButton(onClick = { showOverflowMenu = true }) {
                            Icon(Icons.Filled.MoreVert, "More Options")
                        }
                        DropdownMenu(
                            expanded = showOverflowMenu,
                            onDismissRequest = { showOverflowMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("New") },
                                onClick = { showOverflowMenu = false; newFile() },
                                leadingIcon = { Icon(Icons.Filled.NoteAdd, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Open") },
                                onClick = {
                                    showOverflowMenu = false
                                    openFileLauncher.launch("text/*")
                                },
                                leadingIcon = { Icon(Icons.Filled.FolderOpen, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Save") },
                                onClick = { showOverflowMenu = false; saveFile() },
                                leadingIcon = { Icon(Icons.Filled.Save, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Save As") },
                                onClick = {
                                    showOverflowMenu = false
                                    saveAsLauncher.launch(currentFileName ?: "untitled.txt")
                                },
                                leadingIcon = { Icon(Icons.Filled.SaveAs, null) }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Insert Timestamp") },
                                onClick = { showOverflowMenu = false; insertTimestamp() },
                                leadingIcon = { Icon(Icons.Filled.AccessTime, null) }
                            )
                            DropdownMenuItem(
                                text = { Text("Go to Line") },
                                onClick = {
                                    showOverflowMenu = false
                                    goToLineInput = ""
                                    showGoToLineDialog = true
                                },
                                leadingIcon = { Icon(Icons.Filled.LinearScale, null) }
                            )
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = {
                                    Text(if (wordWrap) "Disable Word Wrap" else "Enable Word Wrap")
                                },
                                onClick = {
                                    showOverflowMenu = false
                                    wordWrap = !wordWrap
                                },
                                leadingIcon = { Icon(Icons.Filled.WrapText, null) }
                            )
                            // Text size submenu
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text("Text Size: ")
                                        TextSizeOption.entries.forEach { option ->
                                            FilterChip(
                                                selected = textSizeOption == option,
                                                onClick = {
                                                    textSizeOption = option
                                                    showOverflowMenu = false
                                                },
                                                label = {
                                                    Text(
                                                        option.label,
                                                        fontSize = 12.sp
                                                    )
                                                },
                                                modifier = Modifier.height(28.dp)
                                            )
                                        }
                                    }
                                },
                                onClick = {},
                                leadingIcon = { Icon(Icons.Filled.FormatSize, null) }
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
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ----- Find & Replace bar -----
            if (showFindReplace) {
                FindReplaceBar(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    replaceQuery = replaceQuery,
                    onReplaceQueryChange = { replaceQuery = it },
                    caseSensitive = caseSensitive,
                    onCaseSensitiveToggle = { caseSensitive = it },
                    matchCount = matchPositions.size,
                    currentMatchIndex = if (matchPositions.isEmpty()) 0 else currentMatchIndex + 1,
                    onNext = { navigateMatch(true) },
                    onPrevious = { navigateMatch(false) },
                    onReplace = { replaceCurrent() },
                    onReplaceAll = { replaceAll() },
                    onClose = { showFindReplace = false }
                )
            }

            // ----- File info bar -----
            if (currentFileName != null) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    tonalElevation = 1.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currentEncoding,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        currentFileSize?.let { size ->
                            Text(
                                text = formatFileSize(size),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // ----- Editor area -----
            val editorTextStyle = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = textSizeOption.size.sp,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = (textSizeOption.size * 1.5).sp
            )
            val lineNumberStyle = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = textSizeOption.size.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                lineHeight = (textSizeOption.size * 1.5).sp
            )

            val lines = if (text.isEmpty()) listOf("") else text.split("\n")
            val lineNumberWidth = (lines.size.toString().length * textSizeOption.size * 0.6 + 16).dp

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    // Line numbers
                    Box(
                        modifier = Modifier
                            .width(lineNumberWidth)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .verticalScroll(lineNumberScrollState)
                            .padding(end = 4.dp, top = 8.dp),
                        contentAlignment = Alignment.TopEnd
                    ) {
                        Column(horizontalAlignment = Alignment.End) {
                            lines.forEachIndexed { index, _ ->
                                Text(
                                    text = "${index + 1}",
                                    style = lineNumberStyle,
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            }
                        }
                    }

                    // Divider
                    Box(
                        modifier = Modifier
                            .width(1.dp)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )

                    // Editor
                    val editorModifier = if (wordWrap) {
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(verticalScrollState)
                            .padding(start = 8.dp, top = 8.dp, end = 8.dp)
                    } else {
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(verticalScrollState)
                            .horizontalScroll(horizontalScrollState)
                            .padding(start = 8.dp, top = 8.dp, end = 8.dp)
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        // Build annotated string with find highlights
                        val highlightColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)
                        val currentHighlightColor =
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.6f)

                        BasicTextField(
                            value = textFieldValue,
                            onValueChange = { newValue ->
                                // Auto-indent: if user pressed enter, carry over indentation
                                val oldText = textFieldValue.text
                                val newText = newValue.text
                                if (newText.length == oldText.length + 1 &&
                                    newValue.selection.start > 0 &&
                                    newText[newValue.selection.start - 1] == '\n'
                                ) {
                                    // Find the previous line's indentation
                                    val cursorPos = newValue.selection.start - 1
                                    val lineStart =
                                        oldText.lastIndexOf('\n', cursorPos - 1) + 1
                                    val line = oldText.substring(
                                        lineStart,
                                        minOf(cursorPos, oldText.length)
                                    )
                                    val indent = line.takeWhile { it == ' ' || it == '\t' }
                                    if (indent.isNotEmpty()) {
                                        val autoIndentedText =
                                            newText.substring(0, newValue.selection.start) +
                                                    indent +
                                                    newText.substring(newValue.selection.start)
                                        val newCursor = newValue.selection.start + indent.length
                                        textFieldValue =
                                            TextFieldValue(autoIndentedText, TextRange(newCursor))
                                        hasUnsavedChanges = true
                                        return@BasicTextField
                                    }
                                }
                                textFieldValue = newValue
                                hasUnsavedChanges = true
                            },
                            textStyle = editorTextStyle,
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            modifier = editorModifier,
                            decorationBox = { innerTextField ->
                                // Overlay highlights for find matches
                                if (matchPositions.isNotEmpty() && searchQuery.isNotEmpty()) {
                                    val annotated = buildAnnotatedString {
                                        append(textFieldValue.text)
                                        matchPositions.forEachIndexed { index, range ->
                                            val color = if (index == currentMatchIndex) {
                                                currentHighlightColor
                                            } else {
                                                highlightColor
                                            }
                                            addStyle(
                                                SpanStyle(background = color),
                                                range.first,
                                                range.last + 1
                                            )
                                        }
                                    }
                                    // We still use innerTextField for the editable field
                                }
                                innerTextField()
                            }
                        )
                    }
                }
            }

            // ----- Status bar -----
            Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 2.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Ln $lineCount  |  Words $wordCount  |  Chars $charCount",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (hasUnsavedChanges) {
                        Text(
                            text = "Modified",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            BannerAd(modifier = Modifier.fillMaxWidth())
        }
    }

    // ----- Go to Line dialog -----
    if (showGoToLineDialog) {
        AlertDialog(
            onDismissRequest = { showGoToLineDialog = false },
            title = { Text("Go to Line") },
            text = {
                OutlinedTextField(
                    value = goToLineInput,
                    onValueChange = { goToLineInput = it.filter { c -> c.isDigit() } },
                    label = { Text("Line number (1-$lineCount)") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        goToLineInput.toIntOrNull()?.let { goToLine(it) }
                        showGoToLineDialog = false
                    }
                ) {
                    Text("Go")
                }
            },
            dismissButton = {
                TextButton(onClick = { showGoToLineDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ============================
// Find & Replace bar composable
// ============================
@Composable
private fun FindReplaceBar(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    replaceQuery: String,
    onReplaceQueryChange: (String) -> Unit,
    caseSensitive: Boolean,
    onCaseSensitiveToggle: (Boolean) -> Unit,
    matchCount: Int,
    currentMatchIndex: Int,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onReplace: () -> Unit,
    onReplaceAll: () -> Unit,
    onClose: () -> Unit
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Search row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Find", fontSize = 13.sp) },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 13.sp),
                    trailingIcon = {
                        if (matchCount > 0) {
                            Text(
                                "$currentMatchIndex/$matchCount",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                        }
                    }
                )
                IconButton(onClick = onPrevious, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Filled.KeyboardArrowUp, "Previous",
                        modifier = Modifier.size(18.dp)
                    )
                }
                IconButton(onClick = onNext, modifier = Modifier.size(32.dp)) {
                    Icon(
                        Icons.Filled.KeyboardArrowDown, "Next",
                        modifier = Modifier.size(18.dp)
                    )
                }
                FilterChip(
                    selected = caseSensitive,
                    onClick = { onCaseSensitiveToggle(!caseSensitive) },
                    label = { Text("Aa", fontSize = 11.sp) },
                    modifier = Modifier.height(28.dp)
                )
                IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.Close, "Close", modifier = Modifier.size(18.dp))
                }
            }

            // Replace row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                OutlinedTextField(
                    value = replaceQuery,
                    onValueChange = onReplaceQueryChange,
                    modifier = Modifier.weight(1f),
                    placeholder = { Text("Replace", fontSize = 13.sp) },
                    singleLine = true,
                    textStyle = TextStyle(fontSize = 13.sp)
                )
                TextButton(
                    onClick = onReplace,
                    enabled = matchCount > 0,
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text("Replace", fontSize = 11.sp)
                }
                TextButton(
                    onClick = onReplaceAll,
                    enabled = matchCount > 0,
                    contentPadding = PaddingValues(horizontal = 8.dp)
                ) {
                    Text("All", fontSize = 11.sp)
                }
            }
        }
    }
}

// ============================
// Utility
// ============================
private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
        else -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
    }
}
