package cv.toolkit.screens

import android.app.Activity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import cv.toolkit.R
import cv.toolkit.ads.AdMobManager
import cv.toolkit.ads.BannerAd
import kotlinx.coroutines.*
import java.net.InetAddress
import kotlin.math.max
import kotlin.math.min

// Data class for individual ping result
data class ContinuousPingResult(
    val timestamp: Long,
    val latency: Long, // -1 for timeout
    val success: Boolean
)

// Data class for a host being monitored
data class MonitoredHost(
    val id: String,
    val host: String,
    val displayName: String,
    val color: Color,
    val results: MutableList<ContinuousPingResult> = mutableListOf(),
    var resolvedIp: String = "",
    var isResolving: Boolean = false
)

// Predefined colors for hosts
val hostColors = listOf(
    Color(0xFF4CAF50), // Green
    Color(0xFF2196F3), // Blue
    Color(0xFFFF9800), // Orange
    Color(0xFFE91E63), // Pink
    Color(0xFF9C27B0), // Purple
    Color(0xFF00BCD4), // Cyan
    Color(0xFFFFEB3B), // Yellow
    Color(0xFF795548), // Brown
)

// Ping interval options
enum class PingInterval(val label: String, val ms: Long) {
    FAST("500ms", 500),
    NORMAL("1s", 1000),
    SLOW("2s", 2000),
    VERY_SLOW("5s", 5000)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContinuousPingScreen(navController: NavController) {
    val context = LocalContext.current
    val activity = context as? Activity
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    // State
    var isPinging by remember { mutableStateOf(false) }
    var newHostInput by remember { mutableStateOf("") }
    var selectedInterval by remember { mutableStateOf(PingInterval.NORMAL) }
    val hosts = remember { mutableStateListOf<MonitoredHost>() }
    var pingJob by remember { mutableStateOf<Job?>(null) }
    var showIntervalMenu by remember { mutableStateOf(false) }
    var maxDataPoints by remember { mutableIntStateOf(60) } // Show last 60 data points

    // Popular targets for quick add
    val popularTargets = listOf(
        "google.com" to "Google",
        "cloudflare.com" to "Cloudflare",
        "amazon.com" to "Amazon",
        "microsoft.com" to "Microsoft",
        "1.1.1.1" to "CF DNS",
        "8.8.8.8" to "Google DNS"
    )

    fun addHost(host: String, displayName: String? = null) {
        if (host.isBlank()) return
        if (hosts.any { it.host.equals(host, ignoreCase = true) }) return
        if (hosts.size >= 8) return // Max 8 hosts

        val colorIndex = hosts.size % hostColors.size
        val newHost = MonitoredHost(
            id = System.currentTimeMillis().toString(),
            host = host.trim(),
            displayName = displayName ?: host.trim(),
            color = hostColors[colorIndex]
        )
        hosts.add(newHost)
        newHostInput = ""

        // Resolve IP in background
        scope.launch(Dispatchers.IO) {
            val index = hosts.indexOfFirst { it.id == newHost.id }
            if (index >= 0) {
                hosts[index] = hosts[index].copy(isResolving = true)
                try {
                    val ip = InetAddress.getByName(host).hostAddress ?: ""
                    val updatedIndex = hosts.indexOfFirst { it.id == newHost.id }
                    if (updatedIndex >= 0) {
                        hosts[updatedIndex] = hosts[updatedIndex].copy(resolvedIp = ip, isResolving = false)
                    }
                } catch (_: Exception) {
                    val updatedIndex = hosts.indexOfFirst { it.id == newHost.id }
                    if (updatedIndex >= 0) {
                        hosts[updatedIndex] = hosts[updatedIndex].copy(isResolving = false)
                    }
                }
            }
        }
    }

    fun removeHost(hostId: String) {
        hosts.removeAll { it.id == hostId }
    }

    fun startPinging() {
        if (hosts.isEmpty()) return
        isPinging = true

        pingJob = scope.launch(Dispatchers.IO) {
            while (isActive && isPinging) {
                // Ping all hosts in parallel
                val pingJobs = hosts.mapIndexed { index, host ->
                    async {
                        val result = try {
                            val start = System.currentTimeMillis()
                            // Use /system/bin/ping for broader Android compatibility
                            val process = Runtime.getRuntime().exec(
                                arrayOf("/system/bin/ping", "-c", "1", "-W", "2", host.host)
                            )
                            val output = process.inputStream.bufferedReader().readText()
                            val errorOutput = process.errorStream.bufferedReader().readText()
                            val exitCode = process.waitFor()
                            val elapsed = System.currentTimeMillis() - start

                            // Parse RTT from output (handles both "time=X" and "time<X" formats)
                            val rttMatch = Regex("time[=<]\\s*([\\d.]+)").find(output)
                            val rttTime = rttMatch?.groupValues?.getOrNull(1)?.toDoubleOrNull()?.toLong() ?: elapsed

                            val success = exitCode == 0 && (output.contains("time=") || output.contains("time<"))
                            ContinuousPingResult(
                                timestamp = System.currentTimeMillis(),
                                latency = if (success) rttTime else -1,
                                success = success
                            )
                        } catch (_: Exception) {
                            // Fallback: TCP connect to common ports (80, 443) as ping alternative
                            val start = System.currentTimeMillis()
                            val reachable = try {
                                val addr = InetAddress.getByName(host.host)
                                val socket = java.net.Socket()
                                socket.connect(java.net.InetSocketAddress(addr, 443), 2000)
                                socket.close()
                                true
                            } catch (_: Exception) {
                                try {
                                    val addr = InetAddress.getByName(host.host)
                                    val socket = java.net.Socket()
                                    socket.connect(java.net.InetSocketAddress(addr, 80), 2000)
                                    socket.close()
                                    true
                                } catch (_: Exception) { false }
                            }
                            val time = System.currentTimeMillis() - start
                            ContinuousPingResult(
                                timestamp = System.currentTimeMillis(),
                                latency = if (reachable) time else -1,
                                success = reachable
                            )
                        }
                        index to result
                    }
                }

                // Collect results
                pingJobs.forEach { deferred ->
                    val (index, result) = deferred.await()
                    if (index < hosts.size) {
                        val host = hosts[index]
                        host.results.add(result)
                        // Keep only last maxDataPoints
                        while (host.results.size > maxDataPoints) {
                            host.results.removeAt(0)
                        }
                        // Trigger recomposition by updating the list
                        hosts[index] = host.copy()
                    }
                }

                delay(selectedInterval.ms)
            }
        }
    }

    fun stopPinging() {
        isPinging = false
        pingJob?.cancel()
        pingJob = null
        activity?.let { AdMobManager.trackPingUsage(it) }
    }

    fun clearResults() {
        hosts.forEachIndexed { index, host ->
            host.results.clear()
            hosts[index] = host.copy()
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            pingJob?.cancel()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.continuous_ping_title)) },
                navigationIcon = {
                    IconButton(onClick = {
                        stopPinging()
                        navController.popBackStack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    // Interval selector
                    Box {
                        TextButton(onClick = { showIntervalMenu = true }) {
                            Icon(Icons.Filled.Timer, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(selectedInterval.label)
                        }
                        DropdownMenu(
                            expanded = showIntervalMenu,
                            onDismissRequest = { showIntervalMenu = false }
                        ) {
                            PingInterval.entries.forEach { interval ->
                                DropdownMenuItem(
                                    text = { Text(interval.label) },
                                    onClick = {
                                        selectedInterval = interval
                                        showIntervalMenu = false
                                    },
                                    leadingIcon = {
                                        if (selectedInterval == interval) {
                                            Icon(Icons.Filled.Check, null)
                                        }
                                    }
                                )
                            }
                        }
                    }
                    // Clear button
                    IconButton(onClick = { clearResults() }, enabled = !isPinging) {
                        Icon(Icons.Filled.ClearAll, "Clear Results")
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
            // Add host input
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newHostInput,
                    onValueChange = { newHostInput = it },
                    label = { Text("Add Host/IP") },
                    modifier = Modifier.weight(1f),
                    enabled = !isPinging && hosts.size < 8,
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Filled.Add, null) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            addHost(newHostInput)
                            focusManager.clearFocus()
                        }
                    )
                )
                FilledIconButton(
                    onClick = { addHost(newHostInput) },
                    enabled = newHostInput.isNotBlank() && !isPinging && hosts.size < 8
                ) {
                    Icon(Icons.Filled.Add, "Add")
                }
            }

            // Quick add popular targets
            if (hosts.size < 8 && !isPinging) {
                Text("Quick Add", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(popularTargets.filter { (host, _) ->
                        hosts.none { it.host.equals(host, ignoreCase = true) }
                    }) { (host, name) ->
                        FilterChip(
                            selected = false,
                            onClick = { addHost(host, name) },
                            label = { Text(name) }
                        )
                    }
                }
            }

            // Host chips with colors
            if (hosts.isNotEmpty()) {
                Text("Monitoring (${hosts.size}/8)", style = MaterialTheme.typography.labelMedium)
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(hosts, key = { it.id }) { host ->
                        InputChip(
                            selected = true,
                            onClick = { },
                            label = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .background(host.color, CircleShape)
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(host.displayName)
                                }
                            },
                            trailingIcon = {
                                if (!isPinging) {
                                    Icon(
                                        Icons.Filled.Close,
                                        "Remove",
                                        modifier = Modifier
                                            .size(18.dp)
                                            .clickable { removeHost(host.id) }
                                    )
                                }
                            }
                        )
                    }
                }
            }

            // Start/Stop button
            Button(
                onClick = { if (isPinging) stopPinging() else startPinging() },
                modifier = Modifier.fillMaxWidth(),
                enabled = hosts.isNotEmpty()
            ) {
                Icon(if (isPinging) Icons.Filled.Stop else Icons.Filled.PlayArrow, null)
                Spacer(Modifier.width(8.dp))
                Text(if (isPinging) "Stop Monitoring" else "Start Monitoring")
            }

            // Real-time graph
            if (hosts.isNotEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                        PingGraph(
                            hosts = hosts,
                            maxDataPoints = maxDataPoints
                        )
                    }
                }
            }

            // Statistics for each host
            Text("Statistics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(hosts, key = { it.id }) { host ->
                    HostStatisticsCard(host = host)
                }
            }

            BannerAd(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun PingGraph(
    hosts: List<MonitoredHost>,
    maxDataPoints: Int
) {
    val textColor = MaterialTheme.colorScheme.onSurface
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val padding = 40f
        val graphWidth = width - padding * 2
        val graphHeight = height - padding * 2

        // Find max latency across all hosts for scaling
        val allLatencies = hosts.flatMap { host ->
            host.results.filter { it.success }.map { it.latency }
        }
        val maxLatency = if (allLatencies.isEmpty()) 100L else max(allLatencies.maxOrNull() ?: 100L, 50L)
        val yScale = graphHeight / maxLatency.toFloat()

        // Draw grid lines
        val gridLines = 5
        for (i in 0..gridLines) {
            val y = padding + (graphHeight * i / gridLines)
            drawLine(
                color = gridColor,
                start = Offset(padding, y),
                end = Offset(width - padding, y),
                strokeWidth = 1f
            )
            // Draw Y-axis labels
            val labelValue = maxLatency - (maxLatency * i / gridLines)
            drawContext.canvas.nativeCanvas.drawText(
                "${labelValue}ms",
                5f,
                y + 4f,
                android.graphics.Paint().apply {
                    color = textColor.hashCode()
                    textSize = 24f
                }
            )
        }

        // Draw each host's line
        hosts.forEach { host ->
            if (host.results.isEmpty()) return@forEach

            val path = Path()
            var firstPoint = true
            val xStep = if (host.results.size > 1) graphWidth / (maxDataPoints - 1) else 0f

            host.results.forEachIndexed { index, result ->
                val x = padding + (host.results.size - 1 - (host.results.size - 1 - index)) * xStep
                val adjustedX = padding + index * xStep

                if (result.success) {
                    val y = padding + graphHeight - (result.latency * yScale)
                    if (firstPoint) {
                        path.moveTo(adjustedX, y)
                        firstPoint = false
                    } else {
                        path.lineTo(adjustedX, y)
                    }
                    // Draw point
                    drawCircle(
                        color = host.color,
                        radius = 4f,
                        center = Offset(adjustedX, y)
                    )
                } else {
                    // Draw timeout indicator at top
                    val y = padding + 10f
                    drawCircle(
                        color = Color.Red.copy(alpha = 0.5f),
                        radius = 4f,
                        center = Offset(adjustedX, y)
                    )
                    firstPoint = true // Break the line
                }
            }

            // Draw the path
            drawPath(
                path = path,
                color = host.color,
                style = Stroke(width = 2f)
            )
        }

        // Draw axes
        drawLine(
            color = textColor,
            start = Offset(padding, padding),
            end = Offset(padding, height - padding),
            strokeWidth = 2f
        )
        drawLine(
            color = textColor,
            start = Offset(padding, height - padding),
            end = Offset(width - padding, height - padding),
            strokeWidth = 2f
        )
    }
}

@Composable
fun HostStatisticsCard(host: MonitoredHost) {
    val results = host.results
    val successfulResults = results.filter { it.success }
    val successRate = if (results.isEmpty()) 0f else (successfulResults.size.toFloat() / results.size * 100)
    val avgLatency = if (successfulResults.isEmpty()) 0L else successfulResults.map { it.latency }.average().toLong()
    val minLatency = successfulResults.minOfOrNull { it.latency } ?: 0L
    val maxLatency = successfulResults.maxOfOrNull { it.latency } ?: 0L
    val lastLatency = successfulResults.lastOrNull()?.latency ?: 0L
    val jitter = if (successfulResults.size >= 2) {
        val latencies = successfulResults.map { it.latency }
        val diffs = latencies.zipWithNext { a, b -> kotlin.math.abs(b - a) }
        if (diffs.isNotEmpty()) diffs.average().toLong() else 0L
    } else 0L

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(host.color, CircleShape)
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        host.displayName,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    if (host.resolvedIp.isNotEmpty()) {
                        Text(
                            host.resolvedIp,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (host.isResolving) {
                        Text(
                            "Resolving...",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                // Current status indicator
                val isOnline = successfulResults.lastOrNull()?.success == true
                Surface(
                    shape = CircleShape,
                    color = if (results.isEmpty()) Color.Gray
                           else if (isOnline) Color(0xFF4CAF50)
                           else Color(0xFFF44336)
                ) {
                    Text(
                        if (results.isEmpty()) "---" else if (isOnline) "${lastLatency}ms" else "TIMEOUT",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatColumn("Sent", "${results.size}")
                StatColumn("Success", "${String.format("%.1f", successRate)}%")
                StatColumn("Avg", "${avgLatency}ms")
                StatColumn("Min", "${minLatency}ms")
                StatColumn("Max", "${maxLatency}ms")
                StatColumn("Jitter", "${jitter}ms")
            }
        }
    }
}

@Composable
private fun StatColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
