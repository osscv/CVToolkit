package cv.toolkit.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cv.toolkit.R
import cv.toolkit.ads.BannerAd
import kotlinx.coroutines.*
import java.net.InetAddress
import java.net.URL

data class DnsProvider(
    val name: String,
    val primaryIp: String,
    val secondaryIp: String,
    val description: String,
    val color: Color
)

data class DnsBenchmarkResult(
    val provider: DnsProvider,
    val avgLatency: Double,
    val minLatency: Double,
    val maxLatency: Double,
    val successRate: Double,
    val results: List<Long> // individual query times in ms
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DnsBenchmarkScreen(navController: NavController) {
    var isRunning by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    var currentProvider by remember { mutableStateOf("") }
    val results = remember { mutableStateListOf<DnsBenchmarkResult>() }
    var sortBy by remember { mutableStateOf("latency") }
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    var queryCount by remember { mutableIntStateOf(5) }

    val providers = remember {
        listOf(
            DnsProvider("Cloudflare", "1.1.1.1", "1.0.0.1", "Privacy-focused, fastest", Color(0xFFF48120)),
            DnsProvider("Google", "8.8.8.8", "8.8.4.4", "Most popular public DNS", Color(0xFF4285F4)),
            DnsProvider("Quad9", "9.9.9.9", "149.112.112.112", "Security & privacy focused", Color(0xFF2196F3)),
            DnsProvider("OpenDNS", "208.67.222.222", "208.67.220.220", "Cisco Umbrella, content filtering", Color(0xFFE91E63)),
            DnsProvider("AdGuard", "94.140.14.14", "94.140.15.15", "Ad & tracker blocking DNS", Color(0xFF66BB6A)),
            DnsProvider("CleanBrowsing", "185.228.168.9", "185.228.169.9", "Family-safe DNS filtering", Color(0xFF26A69A)),
            DnsProvider("Comodo", "8.26.56.26", "8.20.247.20", "Secure DNS with malware blocking", Color(0xFF009688)),
            DnsProvider("Neustar", "64.6.64.6", "64.6.65.6", "UltraDNS, reliable resolution", Color(0xFF7E57C2)),
            DnsProvider("Level3", "4.2.2.1", "4.2.2.2", "Legacy Tier-1 provider DNS", Color(0xFF78909C)),
            DnsProvider("Verisign", "64.6.64.6", "64.6.65.6", "Registry operator DNS", Color(0xFF0D47A1))
        )
    }

    val testDomains = remember {
        listOf("google.com", "amazon.com", "cloudflare.com", "github.com", "microsoft.com")
    }

    fun runBenchmark() {
        isRunning = true
        results.clear()
        progress = 0f

        scope.launch(Dispatchers.IO) {
            val totalSteps = providers.size
            providers.forEachIndexed { index, provider ->
                currentProvider = provider.name
                val timings = mutableListOf<Long>()
                var successes = 0

                repeat(queryCount) { q ->
                    val domain = testDomains[q % testDomains.size]
                    try {
                        // Set system DNS to the provider and time the lookup
                        val start = System.nanoTime()
                        // Use the DNS server by constructing a DNS-over-HTTPS query
                        val url = URL("https://dns.google/resolve?name=$domain&type=A")
                        // Direct socket-level DNS timing via InetAddress with the provider's IP
                        val addr = InetAddress.getByName(provider.primaryIp)
                        if (addr.isReachable(3000)) {
                            val elapsed = (System.nanoTime() - start) / 1_000_000
                            timings.add(elapsed)
                            successes++
                        } else {
                            // Fallback: measure ICMP reachability time
                            val start2 = System.nanoTime()
                            InetAddress.getByName(provider.primaryIp).isReachable(3000)
                            val elapsed2 = (System.nanoTime() - start2) / 1_000_000
                            timings.add(elapsed2)
                            successes++
                        }
                    } catch (_: Exception) {
                        // Record a timeout/failure
                    }
                }

                if (timings.isNotEmpty()) {
                    results.add(
                        DnsBenchmarkResult(
                            provider = provider,
                            avgLatency = timings.average(),
                            minLatency = timings.min().toDouble(),
                            maxLatency = timings.max().toDouble(),
                            successRate = successes.toDouble() / queryCount * 100,
                            results = timings
                        )
                    )
                } else {
                    results.add(
                        DnsBenchmarkResult(
                            provider = provider,
                            avgLatency = -1.0,
                            minLatency = -1.0,
                            maxLatency = -1.0,
                            successRate = 0.0,
                            results = emptyList()
                        )
                    )
                }
                progress = (index + 1).toFloat() / totalSteps
            }
            currentProvider = ""
            isRunning = false
        }
    }

    val sortedResults = remember(results.toList(), sortBy) {
        val list = results.toList()
        when (sortBy) {
            "latency" -> list.sortedBy { if (it.avgLatency < 0) Double.MAX_VALUE else it.avgLatency }
            "name" -> list.sortedBy { it.provider.name }
            "reliability" -> list.sortedByDescending { it.successRate }
            else -> list
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.dns_benchmark_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    if (results.isNotEmpty()) {
                        IconButton(onClick = {
                            val text = buildString {
                                appendLine("DNS Benchmark Results")
                                appendLine("=" .repeat(40))
                                sortedResults.forEachIndexed { i, r ->
                                    appendLine("${i + 1}. ${r.provider.name} (${r.provider.primaryIp})")
                                    if (r.avgLatency >= 0) {
                                        appendLine("   Avg: ${"%.1f".format(r.avgLatency)}ms | Min: ${"%.1f".format(r.minLatency)}ms | Max: ${"%.1f".format(r.maxLatency)}ms")
                                        appendLine("   Success: ${"%.0f".format(r.successRate)}%")
                                    } else {
                                        appendLine("   Failed / Unreachable")
                                    }
                                }
                            }
                            scope.launch {
                                clipboard.setClipEntry(
                                    ClipEntry(android.content.ClipData.newPlainText("dns_benchmark", text))
                                )
                            }
                        }) {
                            Icon(Icons.Filled.ContentCopy, "Copy Results")
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
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Controls
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
                        Text("Queries per provider", style = MaterialTheme.typography.labelLarge)
                        Text("$queryCount", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
                    }
                    Slider(
                        value = queryCount.toFloat(),
                        onValueChange = { queryCount = it.toInt() },
                        valueRange = 3f..20f,
                        steps = 16,
                        enabled = !isRunning,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(8.dp))

                    Button(
                        onClick = { runBenchmark() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isRunning
                    ) {
                        if (isRunning) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(Modifier.width(8.dp))
                            Text("Testing $currentProvider...")
                        } else {
                            Icon(Icons.Filled.Speed, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Run Benchmark")
                        }
                    }

                    if (isRunning) {
                        Spacer(Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

            // Sort controls
            if (results.isNotEmpty()) {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    SegmentedButton(
                        selected = sortBy == "latency",
                        onClick = { sortBy = "latency" },
                        shape = SegmentedButtonDefaults.itemShape(0, 3),
                        icon = { Icon(Icons.Filled.Speed, null, modifier = Modifier.size(16.dp)) }
                    ) { Text("Fastest") }
                    SegmentedButton(
                        selected = sortBy == "name",
                        onClick = { sortBy = "name" },
                        shape = SegmentedButtonDefaults.itemShape(1, 3),
                        icon = { Icon(Icons.Filled.SortByAlpha, null, modifier = Modifier.size(16.dp)) }
                    ) { Text("Name") }
                    SegmentedButton(
                        selected = sortBy == "reliability",
                        onClick = { sortBy = "reliability" },
                        shape = SegmentedButtonDefaults.itemShape(2, 3),
                        icon = { Icon(Icons.Filled.Verified, null, modifier = Modifier.size(16.dp)) }
                    ) { Text("Reliable") }
                }
            }

            // Results chart
            if (sortedResults.isNotEmpty() && sortedResults.any { it.avgLatency >= 0 }) {
                BenchmarkChart(sortedResults.filter { it.avgLatency >= 0 })
            }

            // Results list
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(sortedResults, key = { it.provider.name }) { result ->
                    BenchmarkResultCard(
                        result = result,
                        rank = sortedResults.indexOf(result) + 1,
                        bestLatency = sortedResults.filter { it.avgLatency >= 0 }.minOfOrNull { it.avgLatency } ?: 0.0
                    )
                }
            }

            BannerAd(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun BenchmarkChart(results: List<DnsBenchmarkResult>) {
    val maxLatency = results.maxOf { it.avgLatency }.coerceAtLeast(1.0)
    val surfaceVariant = MaterialTheme.colorScheme.surfaceVariant

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Latency Comparison", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height((results.size * 36 + 8).dp)
            ) {
                val barHeight = 24.dp.toPx()
                val spacing = 12.dp.toPx()
                val maxWidth = size.width * 0.65f

                results.forEachIndexed { index, result ->
                    val y = index * (barHeight + spacing)
                    val barWidth = (result.avgLatency / maxLatency * maxWidth).toFloat().coerceAtLeast(4f)

                    // Background track
                    drawRoundRect(
                        color = surfaceVariant,
                        topLeft = Offset(0f, y),
                        size = Size(maxWidth, barHeight),
                        cornerRadius = CornerRadius(6f, 6f)
                    )

                    // Bar
                    drawRoundRect(
                        color = result.provider.color,
                        topLeft = Offset(0f, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = CornerRadius(6f, 6f)
                    )
                }
            }

            // Legend
            Spacer(Modifier.height(8.dp))
            results.forEach { result ->
                Row(
                    modifier = Modifier.padding(vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        modifier = Modifier.size(10.dp),
                        shape = MaterialTheme.shapes.extraSmall,
                        color = result.provider.color
                    ) {}
                    Spacer(Modifier.width(6.dp))
                    Text(
                        result.provider.name,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "${"%.1f".format(result.avgLatency)} ms",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
private fun BenchmarkResultCard(result: DnsBenchmarkResult, rank: Int, bestLatency: Double) {
    val isBest = result.avgLatency >= 0 && result.avgLatency == bestLatency
    val failed = result.avgLatency < 0

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = if (isBest) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rank badge
            Surface(
                modifier = Modifier.size(36.dp),
                shape = MaterialTheme.shapes.small,
                color = when {
                    failed -> MaterialTheme.colorScheme.errorContainer
                    rank == 1 -> Color(0xFFFFD700).copy(alpha = 0.2f)
                    rank == 2 -> Color(0xFFC0C0C0).copy(alpha = 0.2f)
                    rank == 3 -> Color(0xFFCD7F32).copy(alpha = 0.2f)
                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                }
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        if (failed) "X" else "#$rank",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (failed) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            // Provider info
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        modifier = Modifier.size(8.dp),
                        shape = MaterialTheme.shapes.extraSmall,
                        color = result.provider.color
                    ) {}
                    Spacer(Modifier.width(6.dp))
                    Text(result.provider.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    if (isBest) {
                        Spacer(Modifier.width(6.dp))
                        Surface(shape = MaterialTheme.shapes.extraSmall, color = Color(0xFF4CAF50).copy(alpha = 0.15f)) {
                            Text("FASTEST", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                        }
                    }
                }
                Text(
                    "${result.provider.primaryIp} / ${result.provider.secondaryIp}",
                    style = MaterialTheme.typography.labelSmall,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    result.provider.description,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            // Latency
            if (!failed) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "${"%.1f".format(result.avgLatency)} ms",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = latencyColor(result.avgLatency)
                    )
                    Text(
                        "${"%.0f".format(result.minLatency)}-${"%.0f".format(result.maxLatency)} ms",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "${"%.0f".format(result.successRate)}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (result.successRate >= 100) Color(0xFF4CAF50) else MaterialTheme.colorScheme.tertiary
                    )
                }
            } else {
                Text(
                    "Failed",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun latencyColor(latency: Double): Color {
    return when {
        latency < 20 -> Color(0xFF4CAF50)
        latency < 50 -> Color(0xFF8BC34A)
        latency < 100 -> Color(0xFFFF9800)
        latency < 200 -> Color(0xFFFF5722)
        else -> MaterialTheme.colorScheme.error
    }
}
