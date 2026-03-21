package cv.toolkit.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cv.toolkit.ads.BannerAd
import kotlinx.coroutines.launch

data class TextStats(
    val characters: Int,
    val charactersNoSpaces: Int,
    val words: Int,
    val sentences: Int,
    val paragraphs: Int,
    val lines: Int,
    val uniqueWords: Int,
    val avgWordLength: Double,
    val readingTime: Int,
    val speakingTime: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextCounterScreen(navController: NavController) {
    var inputText by remember { mutableStateOf("") }
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    val stats = remember(inputText) {
        calculateTextStats(inputText)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Text Counter") },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Input field
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                label = { Text("Enter your text") },
                placeholder = { Text("Type or paste text here...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                minLines = 8,
                trailingIcon = {
                    if (inputText.isNotEmpty()) {
                        IconButton(onClick = { inputText = "" }) {
                            Icon(Icons.Filled.Clear, "Clear")
                        }
                    }
                }
            )

            // Action buttons
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
                    Icon(Icons.Filled.ContentPaste, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Paste")
                }
                Button(
                    onClick = { inputText = "" },
                    modifier = Modifier.weight(1f),
                    enabled = inputText.isNotEmpty()
                ) {
                    Icon(Icons.Filled.DeleteOutline, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Clear")
                }
            }

            // Main stats
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatColumn("Characters", stats.characters.toString())
                    StatColumn("Words", stats.words.toString())
                    StatColumn("Sentences", stats.sentences.toString())
                    StatColumn("Paragraphs", stats.paragraphs.toString())
                }
            }

            // Detailed stats
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Detailed Statistics",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(12.dp))

                    StatRow("Characters (no spaces)", stats.charactersNoSpaces.toString())
                    StatRow("Lines", stats.lines.toString())
                    StatRow("Unique Words", stats.uniqueWords.toString())
                    StatRow("Avg. Word Length", String.format("%.1f", stats.avgWordLength))
                }
            }

            // Reading/Speaking time
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Estimated Time",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        TimeCard(
                            icon = Icons.AutoMirrored.Filled.MenuBook,
                            label = "Reading",
                            time = formatTime(stats.readingTime)
                        )
                        TimeCard(
                            icon = Icons.Filled.RecordVoiceOver,
                            label = "Speaking",
                            time = formatTime(stats.speakingTime)
                        )
                    }
                }
            }

            BannerAd(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun StatColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
        )
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun TimeCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    time: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            icon,
            null,
            modifier = Modifier.size(24.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(4.dp))
        Text(
            time,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun calculateTextStats(text: String): TextStats {
    if (text.isEmpty()) {
        return TextStats(0, 0, 0, 0, 0, 0, 0, 0.0, 0, 0)
    }

    val characters = text.length
    val charactersNoSpaces = text.replace(Regex("\\s"), "").length

    val words = text.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    val wordCount = words.size

    val sentences = text.split(Regex("[.!?]+")).filter { it.trim().isNotEmpty() }.size

    val paragraphs = text.split(Regex("\n\\s*\n")).filter { it.trim().isNotEmpty() }.size

    val lines = if (text.isEmpty()) 0 else text.lines().size

    val uniqueWords = words.map { it.lowercase().replace(Regex("[^a-zA-Z0-9]"), "") }
        .filter { it.isNotEmpty() }
        .toSet()
        .size

    val avgWordLength = if (words.isNotEmpty()) {
        words.sumOf { it.replace(Regex("[^a-zA-Z0-9]"), "").length } / words.size.toDouble()
    } else 0.0

    // Average reading speed: 200-250 words per minute
    val readingTime = (wordCount / 200.0 * 60).toInt()

    // Average speaking speed: 125-150 words per minute
    val speakingTime = (wordCount / 130.0 * 60).toInt()

    return TextStats(
        characters = characters,
        charactersNoSpaces = charactersNoSpaces,
        words = wordCount,
        sentences = sentences,
        paragraphs = paragraphs,
        lines = lines,
        uniqueWords = uniqueWords,
        avgWordLength = avgWordLength,
        readingTime = readingTime,
        speakingTime = speakingTime
    )
}

private fun formatTime(seconds: Int): String {
    return when {
        seconds < 60 -> "${seconds}s"
        seconds < 3600 -> "${seconds / 60}m ${seconds % 60}s"
        else -> "${seconds / 3600}h ${(seconds % 3600) / 60}m"
    }
}
