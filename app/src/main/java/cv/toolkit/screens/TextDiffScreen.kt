package cv.toolkit.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cv.toolkit.ads.BannerAd
import kotlinx.coroutines.launch

data class DiffResult(
    val additions: Int,
    val deletions: Int,
    val unchanged: Int,
    val diffLines: List<DiffLine>
)

data class DiffLine(
    val type: DiffType,
    val lineNumber1: Int?,
    val lineNumber2: Int?,
    val content: String
)

enum class DiffType { ADDED, REMOVED, UNCHANGED }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TextDiffScreen(navController: NavController) {
    var text1 by remember { mutableStateOf("") }
    var text2 by remember { mutableStateOf("") }
    var diffResult by remember { mutableStateOf<DiffResult?>(null) }
    var ignoreCase by remember { mutableStateOf(false) }
    var ignoreWhitespace by remember { mutableStateOf(false) }
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    fun compareTexts() {
        if (text1.isEmpty() && text2.isEmpty()) {
            diffResult = null
            return
        }

        val lines1 = text1.lines()
        val lines2 = text2.lines()

        val processedLines1 = lines1.map { line ->
            var processed = line
            if (ignoreCase) processed = processed.lowercase()
            if (ignoreWhitespace) processed = processed.trim().replace(Regex("\\s+"), " ")
            processed
        }

        val processedLines2 = lines2.map { line ->
            var processed = line
            if (ignoreCase) processed = processed.lowercase()
            if (ignoreWhitespace) processed = processed.trim().replace(Regex("\\s+"), " ")
            processed
        }

        val diffLines = mutableListOf<DiffLine>()
        var additions = 0
        var deletions = 0
        var unchanged = 0

        val lcs = longestCommonSubsequence(processedLines1, processedLines2)

        var i = 0
        var j = 0
        var lcsIndex = 0

        while (i < lines1.size || j < lines2.size) {
            when {
                i < lines1.size && (lcsIndex >= lcs.size || processedLines1[i] != lcs[lcsIndex]) -> {
                    diffLines.add(DiffLine(DiffType.REMOVED, i + 1, null, lines1[i]))
                    deletions++
                    i++
                }
                j < lines2.size && (lcsIndex >= lcs.size || processedLines2[j] != lcs[lcsIndex]) -> {
                    diffLines.add(DiffLine(DiffType.ADDED, null, j + 1, lines2[j]))
                    additions++
                    j++
                }
                else -> {
                    if (i < lines1.size && j < lines2.size) {
                        diffLines.add(DiffLine(DiffType.UNCHANGED, i + 1, j + 1, lines1[i]))
                        unchanged++
                        i++
                        j++
                        lcsIndex++
                    }
                }
            }
        }

        diffResult = DiffResult(additions, deletions, unchanged, diffLines)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Text Diff") },
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
            // Input fields row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Text 1
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Original", style = MaterialTheme.typography.labelMedium)
                        IconButton(
                            onClick = {
                                scope.launch {
                                    clipboard.getClipEntry()?.clipData?.getItemAt(0)?.text?.let {
                                        text1 = it.toString()
                                    }
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Filled.ContentPaste, "Paste", modifier = Modifier.size(18.dp))
                        }
                    }
                    OutlinedTextField(
                        value = text1,
                        onValueChange = { text1 = it },
                        modifier = Modifier.fillMaxSize(),
                        placeholder = { Text("Enter original text...") },
                        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace)
                    )
                }

                // Text 2
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Modified", style = MaterialTheme.typography.labelMedium)
                        IconButton(
                            onClick = {
                                scope.launch {
                                    clipboard.getClipEntry()?.clipData?.getItemAt(0)?.text?.let {
                                        text2 = it.toString()
                                    }
                                }
                            },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(Icons.Filled.ContentPaste, "Paste", modifier = Modifier.size(18.dp))
                        }
                    }
                    OutlinedTextField(
                        value = text2,
                        onValueChange = { text2 = it },
                        modifier = Modifier.fillMaxSize(),
                        placeholder = { Text("Enter modified text...") },
                        textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace)
                    )
                }
            }

            // Options
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = ignoreCase, onCheckedChange = { ignoreCase = it })
                    Text("Ignore Case", style = MaterialTheme.typography.bodySmall)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = ignoreWhitespace, onCheckedChange = { ignoreWhitespace = it })
                    Text("Ignore Whitespace", style = MaterialTheme.typography.bodySmall)
                }
            }

            // Compare button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        text1 = ""
                        text2 = ""
                        diffResult = null
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Clear, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Clear")
                }
                Button(
                    onClick = { compareTexts() },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.Compare, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Compare")
                }
            }

            // Results
            diffResult?.let { result ->
                // Stats
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem("+${result.additions}", "Added", Color(0xFF4CAF50))
                        StatItem("-${result.deletions}", "Removed", Color(0xFFF44336))
                        StatItem("${result.unchanged}", "Unchanged", MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                // Diff view
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp)
                            .verticalScroll(rememberScrollState())
                            .horizontalScroll(rememberScrollState())
                    ) {
                        result.diffLines.forEach { line ->
                            DiffLineView(line)
                        }
                    }
                }
            }

            // Empty state
            if (diffResult == null) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Filled.Compare,
                            null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Compare Two Texts",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Enter text in both fields and click Compare",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            BannerAd(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun StatItem(value: String, label: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun DiffLineView(line: DiffLine) {
    val backgroundColor = when (line.type) {
        DiffType.ADDED -> Color(0xFF4CAF50).copy(alpha = 0.2f)
        DiffType.REMOVED -> Color(0xFFF44336).copy(alpha = 0.2f)
        DiffType.UNCHANGED -> Color.Transparent
    }

    val prefix = when (line.type) {
        DiffType.ADDED -> "+"
        DiffType.REMOVED -> "-"
        DiffType.UNCHANGED -> " "
    }

    val textColor = when (line.type) {
        DiffType.ADDED -> Color(0xFF2E7D32)
        DiffType.REMOVED -> Color(0xFFC62828)
        DiffType.UNCHANGED -> MaterialTheme.colorScheme.onSurface
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .padding(horizontal = 4.dp, vertical = 2.dp)
    ) {
        Text(
            text = buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = textColor)) {
                    append(prefix)
                }
                append(" ")
                withStyle(SpanStyle(color = textColor)) {
                    append(line.content)
                }
            },
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

private fun longestCommonSubsequence(list1: List<String>, list2: List<String>): List<String> {
    val m = list1.size
    val n = list2.size
    val dp = Array(m + 1) { IntArray(n + 1) }

    for (i in 1..m) {
        for (j in 1..n) {
            if (list1[i - 1] == list2[j - 1]) {
                dp[i][j] = dp[i - 1][j - 1] + 1
            } else {
                dp[i][j] = maxOf(dp[i - 1][j], dp[i][j - 1])
            }
        }
    }

    val result = mutableListOf<String>()
    var i = m
    var j = n
    while (i > 0 && j > 0) {
        when {
            list1[i - 1] == list2[j - 1] -> {
                result.add(0, list1[i - 1])
                i--
                j--
            }
            dp[i - 1][j] > dp[i][j - 1] -> i--
            else -> j--
        }
    }

    return result
}
