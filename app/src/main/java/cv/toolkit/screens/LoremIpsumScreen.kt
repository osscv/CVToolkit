package cv.toolkit.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cv.toolkit.R
import cv.toolkit.ads.BannerAd
import kotlinx.coroutines.launch

private val CLASSIC_OPENING =
    "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua."

private val LOREM_WORDS = listOf(
    // Classic Lorem Ipsum paragraph words
    "lorem", "ipsum", "dolor", "sit", "amet", "consectetur", "adipiscing", "elit",
    "sed", "do", "eiusmod", "tempor", "incididunt", "ut", "labore", "et", "dolore",
    "magna", "aliqua", "enim", "ad", "minim", "veniam", "quis", "nostrud",
    "exercitation", "ullamco", "laboris", "nisi", "aliquip", "ex", "ea", "commodo",
    "consequat", "duis", "aute", "irure", "in", "reprehenderit", "voluptate",
    "velit", "esse", "cillum", "fugiat", "nulla", "pariatur", "excepteur", "sint",
    "occaecat", "cupidatat", "non", "proident", "sunt", "culpa", "qui", "officia",
    "deserunt", "mollit", "anim", "id", "est", "laborum",
    // Additional Latin-esque words for variety
    "ac", "accumsan", "aliquet", "ante", "arcu", "at", "auctor", "augue",
    "bibendum", "blandit", "condimentum", "congue", "consequat", "convallis",
    "cras", "curabitur", "cursus", "dapibus", "diam", "dictum", "dignissim",
    "donec", "efficitur", "egestas", "eleifend", "elementum", "euismod",
    "facilisi", "facilisis", "fames", "faucibus", "felis", "fermentum",
    "feugiat", "finibus", "fringilla", "fusce", "gravida", "habitant",
    "habitasse", "hac", "hendrerit", "iaculis", "imperdiet", "integer",
    "interdum", "justo", "lacinia", "lacus", "laoreet", "lectus", "leo",
    "libero", "ligula", "lobortis", "luctus", "maecenas", "massa",
    "mattis", "mauris", "maximus", "metus", "mi", "molestie", "morbi",
    "nam", "nec", "neque", "nibh", "nisl", "nullam", "nunc", "odio",
    "orci", "ornare", "pellentesque", "pharetra", "placerat", "platea",
    "porta", "porttitor", "posuere", "potenti", "praesent", "pretium",
    "primis", "proin", "pulvinar", "purus", "quam", "quisque", "rhoncus",
    "risus", "rutrum", "sagittis", "sapien", "scelerisque", "semper",
    "senectus", "sodales", "sollicitudin", "suscipit", "suspendisse",
    "tellus", "tempus", "tincidunt", "tortor", "tristique", "turpis",
    "ultrices", "ultricies", "urna", "varius", "vehicula", "vel",
    "vestibulum", "vitae", "vivamus", "viverra", "volutpat", "vulputate"
)

private enum class GenerationMode(val label: String) {
    PARAGRAPHS("Paragraphs"),
    SENTENCES("Sentences"),
    WORDS("Words")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoremIpsumScreen(navController: NavController) {
    val context = LocalContext.current
    var selectedMode by remember { mutableStateOf(GenerationMode.PARAGRAPHS) }
    var countText by remember { mutableStateOf("3") }
    var startWithLorem by remember { mutableStateOf(true) }
    var generatedText by remember { mutableStateOf("") }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    fun generate() {
        val count = countText.toIntOrNull()?.coerceIn(1, 999) ?: return
        generatedText = generateLoremIpsum(selectedMode, count, startWithLorem)
    }

    // Generate on first composition
    LaunchedEffect(Unit) {
        generatedText = generateLoremIpsum(selectedMode, 3, startWithLorem)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.lorem_ipsum_title)) },
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Mode selector
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                GenerationMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = selectedMode == mode,
                        onClick = { selectedMode = mode },
                        shape = SegmentedButtonDefaults.itemShape(index, GenerationMode.entries.size)
                    ) {
                        Text(mode.label, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // Count input and options
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = countText,
                        onValueChange = { newValue ->
                            if (newValue.isEmpty() || newValue.all { it.isDigit() }) {
                                countText = newValue
                            }
                        },
                        label = { Text("Count") },
                        placeholder = { Text("Number of ${selectedMode.label.lowercase()}") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = startWithLorem,
                            onCheckedChange = { startWithLorem = it }
                        )
                        Text(
                            "Start with \"Lorem ipsum dolor sit amet...\"",
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Generate button
            Button(
                onClick = { generate() },
                modifier = Modifier.fillMaxWidth(),
                enabled = countText.isNotEmpty() && (countText.toIntOrNull() ?: 0) > 0
            ) {
                Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Generate")
            }

            // Output area
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Text("Generated Text", style = MaterialTheme.typography.labelMedium)
                        IconButton(
                            onClick = {
                                if (generatedText.isNotEmpty()) {
                                    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboardManager.setPrimaryClip(
                                        ClipData.newPlainText("lorem_ipsum", generatedText)
                                    )
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Copied to clipboard")
                                    }
                                }
                            },
                            enabled = generatedText.isNotEmpty()
                        ) {
                            Icon(Icons.Filled.ContentCopy, "Copy", modifier = Modifier.size(20.dp))
                        }
                    }
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))

                    SelectionContainer(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        Text(
                            generatedText.ifEmpty { "Generated text will appear here..." },
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (generatedText.isEmpty())
                                MaterialTheme.colorScheme.onSurfaceVariant
                            else
                                MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            BannerAd(modifier = Modifier.fillMaxWidth())
        }
    }
}

private fun generateLoremIpsum(
    mode: GenerationMode,
    count: Int,
    startWithLorem: Boolean
): String {
    return when (mode) {
        GenerationMode.PARAGRAPHS -> generateParagraphs(count, startWithLorem)
        GenerationMode.SENTENCES -> generateSentences(count, startWithLorem)
        GenerationMode.WORDS -> generateWords(count, startWithLorem)
    }
}

private fun generateParagraphs(count: Int, startWithLorem: Boolean): String {
    val paragraphs = mutableListOf<String>()
    for (i in 0 until count) {
        val useClassicOpening = startWithLorem && i == 0
        val sentenceCount = (4..8).random()
        val sentences = mutableListOf<String>()

        for (j in 0 until sentenceCount) {
            if (useClassicOpening && j == 0) {
                sentences.add(CLASSIC_OPENING)
            } else {
                sentences.add(generateRandomSentence())
            }
        }
        paragraphs.add(sentences.joinToString(" "))
    }
    return paragraphs.joinToString("\n\n")
}

private fun generateSentences(count: Int, startWithLorem: Boolean): String {
    val sentences = mutableListOf<String>()
    for (i in 0 until count) {
        if (startWithLorem && i == 0) {
            sentences.add(CLASSIC_OPENING)
        } else {
            sentences.add(generateRandomSentence())
        }
    }
    return sentences.joinToString(" ")
}

private fun generateWords(count: Int, startWithLorem: Boolean): String {
    if (startWithLorem) {
        val classicWords = CLASSIC_OPENING.replace(".", "").split(" ")
        return if (count <= classicWords.size) {
            classicWords.take(count).joinToString(" ")
        } else {
            val remaining = count - classicWords.size
            val extraWords = (1..remaining).map { LOREM_WORDS.random() }
            (classicWords + extraWords).joinToString(" ")
        }
    }
    return (1..count).map { LOREM_WORDS.random() }.joinToString(" ")
}

private fun generateRandomSentence(): String {
    val wordCount = (5..15).random()
    val words = (1..wordCount).map { LOREM_WORDS.random() }.toMutableList()
    // Capitalize the first word
    words[0] = words[0].replaceFirstChar { it.uppercase() }
    return words.joinToString(" ") + "."
}
