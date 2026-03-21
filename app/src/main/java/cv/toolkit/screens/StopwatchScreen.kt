package cv.toolkit.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import cv.toolkit.ads.BannerAd
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class LapTime(
    val lapNumber: Int,
    val lapTime: Long,
    val totalTime: Long
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StopwatchScreen(navController: NavController) {
    var elapsedTime by remember { mutableLongStateOf(0L) }
    var isRunning by remember { mutableStateOf(false) }
    var laps by remember { mutableStateOf(listOf<LapTime>()) }
    var lastLapTime by remember { mutableLongStateOf(0L) }
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    // Timer effect
    LaunchedEffect(isRunning) {
        if (isRunning) {
            val startTime = System.currentTimeMillis() - elapsedTime
            while (isRunning) {
                elapsedTime = System.currentTimeMillis() - startTime
                delay(10)
            }
        }
    }

    fun startStop() {
        isRunning = !isRunning
    }

    fun reset() {
        isRunning = false
        elapsedTime = 0L
        laps = emptyList()
        lastLapTime = 0L
    }

    fun lap() {
        if (isRunning) {
            val lapTime = elapsedTime - lastLapTime
            laps = laps + LapTime(
                lapNumber = laps.size + 1,
                lapTime = lapTime,
                totalTime = elapsedTime
            )
            lastLapTime = elapsedTime
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Stopwatch") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    if (laps.isNotEmpty()) {
                        IconButton(
                            onClick = {
                                val lapsText = laps.joinToString("\n") { lap ->
                                    "Lap ${lap.lapNumber}: ${formatTime(lap.lapTime)} (Total: ${formatTime(lap.totalTime)})"
                                }
                                scope.launch {
                                    clipboard.setClipEntry(
                                        androidx.compose.ui.platform.ClipEntry(
                                            android.content.ClipData.newPlainText("laps", lapsText)
                                        )
                                    )
                                }
                            }
                        ) {
                            Icon(Icons.Filled.ContentCopy, "Copy Laps")
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
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Timer display
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        formatTime(elapsedTime),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 56.sp
                        ),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    if (laps.isNotEmpty()) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Current Lap: ${formatTime(elapsedTime - lastLapTime)}",
                            style = MaterialTheme.typography.titleMedium,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Control buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                // Reset button
                FilledTonalButton(
                    onClick = { reset() },
                    enabled = elapsedTime > 0 && !isRunning,
                    modifier = Modifier.size(80.dp),
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Icon(Icons.Filled.Refresh, "Reset", modifier = Modifier.size(32.dp))
                }

                // Start/Stop button
                Button(
                    onClick = { startStop() },
                    modifier = Modifier.size(100.dp),
                    shape = MaterialTheme.shapes.extraLarge,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRunning)
                            MaterialTheme.colorScheme.error
                        else
                            MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        if (isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        if (isRunning) "Pause" else "Start",
                        modifier = Modifier.size(48.dp)
                    )
                }

                // Lap button
                FilledTonalButton(
                    onClick = { lap() },
                    enabled = isRunning,
                    modifier = Modifier.size(80.dp),
                    shape = MaterialTheme.shapes.extraLarge
                ) {
                    Icon(Icons.Filled.Flag, "Lap", modifier = Modifier.size(32.dp))
                }
            }

            // Laps list
            if (laps.isNotEmpty()) {
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
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Laps (${laps.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            IconButton(
                                onClick = { laps = emptyList(); lastLapTime = 0L }
                            ) {
                                Icon(Icons.Filled.DeleteOutline, "Clear Laps", modifier = Modifier.size(20.dp))
                            }
                        }
                        HorizontalDivider()
                        Spacer(Modifier.height(8.dp))

                        // Find best and worst laps
                        val bestLap = laps.minByOrNull { it.lapTime }
                        val worstLap = laps.maxByOrNull { it.lapTime }

                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            reverseLayout = true
                        ) {
                            itemsIndexed(laps) { _, lap ->
                                LapItem(
                                    lap = lap,
                                    isBest = lap == bestLap && laps.size > 1,
                                    isWorst = lap == worstLap && laps.size > 1
                                )
                            }
                        }
                    }
                }
            } else {
                // Empty state
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Filled.Timer,
                            null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "No laps recorded",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Press the flag button while running to record laps",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            BannerAd(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun LapItem(
    lap: LapTime,
    isBest: Boolean,
    isWorst: Boolean
) {
    val backgroundColor = when {
        isBest -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        isWorst -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = backgroundColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Lap ${lap.lapNumber}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                if (isBest) {
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primary,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            "BEST",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
                if (isWorst) {
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            "SLOWEST",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onError
                        )
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    formatTime(lap.lapTime),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
                Text(
                    formatTime(lap.totalTime),
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun formatTime(millis: Long): String {
    val totalSeconds = millis / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    val hundredths = (millis % 1000) / 10

    return String.format("%02d:%02d.%02d", minutes, seconds, hundredths)
}
