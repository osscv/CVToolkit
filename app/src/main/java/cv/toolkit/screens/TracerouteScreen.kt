package cv.toolkit.screens

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.AltRoute
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cv.toolkit.R
import cv.toolkit.ads.AdMobManager
import cv.toolkit.ads.BannerAd
import kotlinx.coroutines.*
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.URL

data class ResolvedIp(val ip: String, val type: String) // type: "IPv4" or "IPv6"
data class TracerouteHop(
    val hop: Int,
    val ip: String,
    val hostname: String = "",
    val rtt1: String = "",
    val rtt2: String = "",
    val rtt3: String = "",
    val status: HopStatus
)
enum class HopStatus { SUCCESS, TIMEOUT, ERROR }

// Data class for single ping result in traceroute
private data class TracePingResult(val hopIp: String, val rttMs: Long, val isTimeout: Boolean)

// Format RTT for display
private fun formatRtt(result: TracePingResult): String {
    return when {
        result.isTimeout -> "*"
        result.rttMs < 1 -> "<1 ms"
        else -> "${result.rttMs} ms"
    }
}

// Single ping with TTL - returns IP of responding host and RTT
private fun singlePingWithTtl(host: String, ttl: Int): TracePingResult {
    return try {
        val start = System.currentTimeMillis()
        val process = Runtime.getRuntime().exec(arrayOf("ping", "-c", "1", "-t", "$ttl", "-W", "2", host))

        val stdoutReader = BufferedReader(InputStreamReader(process.inputStream))
        val stderrReader = BufferedReader(InputStreamReader(process.errorStream))
        val stdout = stdoutReader.readText()
        val stderr = stderrReader.readText()
        stdoutReader.close()
        stderrReader.close()
        process.waitFor()
        val elapsed = System.currentTimeMillis() - start

        val combinedOutput = stdout + stderr

        // Try to extract IP from various ping output formats
        val ipPatterns = listOf(
            Regex("from ([\\d.]+)"),
            Regex("From ([\\d.]+)"),
            Regex("([\\d.]+): icmp"),
            Regex("From ([\\d.]+) .*[Tt]ime to live exceeded"),
            Regex("From ([\\d.]+) .*TTL")
        )

        var ip = ""
        for (pattern in ipPatterns) {
            val match = pattern.find(combinedOutput)
            if (match != null) {
                ip = match.groupValues.getOrNull(1) ?: ""
                if (ip.isNotEmpty()) break
            }
        }

        // Extract actual RTT from ping output if available
        val rttMatch = Regex("time[=<]\\s*([\\d.]+)").find(combinedOutput)
        val rttTime = rttMatch?.groupValues?.getOrNull(1)?.toDoubleOrNull()?.toLong() ?: elapsed

        if (ip.isNotEmpty()) {
            TracePingResult(ip, rttTime, false)
        } else {
            TracePingResult("", 0, true)
        }
    } catch (_: Exception) {
        TracePingResult("", 0, true)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TracerouteScreen(navController: NavController) {
    val context = LocalContext.current
    val activity = context as? Activity
    var target by remember { mutableStateOf("") }
    var isTracing by remember { mutableStateOf(false) }
    val hops = remember { mutableStateListOf<TracerouteHop>() }
    var isResolving by remember { mutableStateOf(false) }
    var resolvedIps by remember { mutableStateOf(listOf<ResolvedIp>()) }
    var showIpSelectionDialog by remember { mutableStateOf(false) }
    var currentTracingTarget by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    val popularTargets = listOf("google.com", "baidu.com", "apple.com", "facebook.com", "youtube.com")

    // Check if input is an IP address (IPv4 or IPv6)
    fun isIpAddress(input: String): Boolean {
        val ipv4Regex = Regex("^\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}$")
        val ipv6Regex = Regex("^([0-9a-fA-F]{0,4}:){2,7}[0-9a-fA-F]{0,4}$")
        return ipv4Regex.matches(input) || ipv6Regex.matches(input) || input.contains("::")
    }

    // Resolve domain to IP addresses using Google DNS-over-HTTPS
    fun resolveDomain(domain: String, onResult: (List<ResolvedIp>) -> Unit) {
        scope.launch(Dispatchers.IO) {
            val results = mutableListOf<ResolvedIp>()
            try {
                // Query A records (IPv4)
                val urlA = URL("https://dns.google/resolve?name=$domain&type=A")
                val responseA = urlA.readText()
                val jsonA = JSONObject(responseA)
                val answersA = jsonA.optJSONArray("Answer")
                if (answersA != null) {
                    for (i in 0 until answersA.length()) {
                        val data = answersA.getJSONObject(i).optString("data")
                        if (data.isNotEmpty() && data.matches(Regex("\\d+\\.\\d+\\.\\d+\\.\\d+"))) {
                            results.add(ResolvedIp(data, "IPv4"))
                        }
                    }
                }
            } catch (_: Exception) {}

            try {
                // Query AAAA records (IPv6)
                val urlAAAA = URL("https://dns.google/resolve?name=$domain&type=AAAA")
                val responseAAAA = urlAAAA.readText()
                val jsonAAAA = JSONObject(responseAAAA)
                val answersAAAA = jsonAAAA.optJSONArray("Answer")
                if (answersAAAA != null) {
                    for (i in 0 until answersAAAA.length()) {
                        val data = answersAAAA.getJSONObject(i).optString("data")
                        if (data.isNotEmpty() && data.contains(":")) {
                            results.add(ResolvedIp(data, "IPv6"))
                        }
                    }
                }
            } catch (_: Exception) {}

            withContext(Dispatchers.Main) {
                onResult(results.distinctBy { it.ip })
            }
        }
    }

    fun startTraceroute(traceTarget: String) {
        if (traceTarget.isBlank()) return
        currentTracingTarget = traceTarget
        isTracing = true
        hops.clear()

        scope.launch(Dispatchers.IO) {
            val targetIp = try {
                java.net.InetAddress.getByName(traceTarget).hostAddress ?: traceTarget
            } catch (_: Exception) { traceTarget }

            // Perform ping-based traceroute with 3 RTT measurements per hop
            for (ttl in 1..30) {
                if (!isTracing) break

                // Perform 3 pings for this TTL
                val results = mutableListOf<TracePingResult>()
                for (i in 1..3) {
                    if (!isTracing) break
                    results.add(singlePingWithTtl(traceTarget, ttl))
                }

                // Get the IP from successful pings (should be same for all 3)
                val hopIp = results.firstOrNull { it.hopIp.isNotEmpty() }?.hopIp ?: ""

                // Resolve hostname for this hop
                val hostname = if (hopIp.isNotEmpty()) {
                    try {
                        val addr = java.net.InetAddress.getByName(hopIp)
                        val resolved = addr.canonicalHostName
                        if (resolved != hopIp) resolved else ""
                    } catch (_: Exception) { "" }
                } else ""

                // Format RTT times
                val rtt1 = results.getOrNull(0)?.let { formatRtt(it) } ?: "*"
                val rtt2 = results.getOrNull(1)?.let { formatRtt(it) } ?: "*"
                val rtt3 = results.getOrNull(2)?.let { formatRtt(it) } ?: "*"

                val status = when {
                    results.any { it.hopIp.isNotEmpty() } -> HopStatus.SUCCESS
                    results.all { it.isTimeout } -> HopStatus.TIMEOUT
                    else -> HopStatus.ERROR
                }

                val hop = TracerouteHop(
                    hop = ttl,
                    ip = hopIp,
                    hostname = hostname,
                    rtt1 = rtt1,
                    rtt2 = rtt2,
                    rtt3 = rtt3,
                    status = status
                )
                hops.add(hop)

                // Check if we reached the destination
                if (hopIp.isNotEmpty() && (hopIp == targetIp || hopIp == traceTarget)) {
                    break
                }
            }

            isTracing = false
            // Track usage for interstitial ad (every 5 traceroutes)
            activity?.let { AdMobManager.trackTracerouteUsage(it) }
        }
    }

    // Handle start button click - resolve domain if needed
    fun onStartClick() {
        if (target.isBlank()) return
        val trimmedTarget = target.trim()

        if (isIpAddress(trimmedTarget)) {
            // Direct IP address - start traceroute immediately
            startTraceroute(trimmedTarget)
        } else {
            // Domain name - resolve IPs first
            isResolving = true
            resolveDomain(trimmedTarget) { ips ->
                isResolving = false
                resolvedIps = ips
                when {
                    ips.isEmpty() -> {
                        // No IPs resolved, try with domain directly
                        startTraceroute(trimmedTarget)
                    }
                    ips.size == 1 -> {
                        // Only one IP, start traceroute directly
                        startTraceroute(ips.first().ip)
                    }
                    else -> {
                        // Multiple IPs, show selection dialog
                        showIpSelectionDialog = true
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.traceroute_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
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
            OutlinedTextField(
                value = target,
                onValueChange = { target = it },
                label = { Text("IP or Domain") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isTracing && !isResolving,
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Language, null) }
            )

            Text("Popular Targets", style = MaterialTheme.typography.labelMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(popularTargets) { t ->
                    FilterChip(
                        selected = target == t,
                        onClick = { target = t },
                        label = { Text(t) },
                        enabled = !isTracing && !isResolving
                    )
                }
            }

            Button(
                onClick = { if (isTracing) isTracing = false else onStartClick() },
                modifier = Modifier.fillMaxWidth(),
                enabled = (target.isNotBlank() || isTracing) && !isResolving
            ) {
                if (isResolving) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Resolving...")
                } else {
                    Icon(if (isTracing) Icons.Filled.Stop else Icons.AutoMirrored.Filled.AltRoute, null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (isTracing) "Stop" else "Start Traceroute")
                }
            }

            if (isTracing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                if (currentTracingTarget.isNotEmpty() && currentTracingTarget != target.trim()) {
                    Text(
                        "Tracing: $currentTracingTarget",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Text("Hops: ${hops.size}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                items(hops) { hop ->
                    HopCard(hop)
                }
            }
            BannerAd(modifier = Modifier.fillMaxWidth())
        }
    }

    // IP Selection Dialog
    if (showIpSelectionDialog) {
        AlertDialog(
            onDismissRequest = { showIpSelectionDialog = false },
            title = { Text("Select IP Address") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Domain \"${target.trim()}\" resolved to multiple IP addresses. Select which one to trace:",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(8.dp))
                    resolvedIps.forEach { resolvedIp ->
                        Card(
                            onClick = {
                                showIpSelectionDialog = false
                                startTraceroute(resolvedIp.ip)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (resolvedIp.type == "IPv4") Icons.Filled.Dns else Icons.Filled.Language,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(resolvedIp.ip, fontWeight = FontWeight.Medium)
                                    Text(resolvedIp.type, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                    // Trace All option
                    if (resolvedIps.size > 1) {
                        Spacer(Modifier.height(4.dp))
                        OutlinedCard(
                            onClick = {
                                showIpSelectionDialog = false
                                // Start traceroute for all IPs sequentially
                                val allIps = resolvedIps.map { it.ip }
                                scope.launch {
                                    allIps.forEachIndexed { index, ip ->
                                        if (index > 0) {
                                            // Wait for previous trace to complete
                                            while (isTracing) {
                                                delay(100)
                                            }
                                        }
                                        startTraceroute(ip)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.SelectAll,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.secondary
                                )
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text("Trace All", fontWeight = FontWeight.Medium)
                                    Text("Trace all ${resolvedIps.size} IPs sequentially", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showIpSelectionDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun HopCard(hop: TracerouteHop) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Hop number
            Surface(
                shape = MaterialTheme.shapes.small,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(32.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text("${hop.hop}", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                }
            }
            Spacer(Modifier.width(8.dp))

            // RTT times (3 columns)
            Row(
                modifier = Modifier.width(140.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Text(
                    hop.rtt1.ifEmpty { "*" },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (hop.rtt1 == "*") MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.width(45.dp)
                )
                Text(
                    hop.rtt2.ifEmpty { "*" },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (hop.rtt2 == "*") MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.width(45.dp)
                )
                Text(
                    hop.rtt3.ifEmpty { "*" },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (hop.rtt3 == "*") MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.width(45.dp)
                )
            }

            Spacer(Modifier.width(8.dp))

            // Hostname and IP
            Column(modifier = Modifier.weight(1f)) {
                if (hop.hostname.isNotEmpty()) {
                    Text(
                        hop.hostname,
                        fontWeight = FontWeight.Medium,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                    Text(
                        "[${hop.ip}]",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else if (hop.ip.isNotEmpty()) {
                    Text(
                        hop.ip,
                        fontWeight = FontWeight.Medium,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                } else {
                    Text(
                        "Request timed out",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Status icon
            Icon(
                when (hop.status) {
                    HopStatus.SUCCESS -> Icons.Filled.CheckCircle
                    HopStatus.TIMEOUT -> Icons.Filled.HourglassEmpty
                    HopStatus.ERROR -> Icons.Filled.Error
                },
                null,
                tint = when (hop.status) {
                    HopStatus.SUCCESS -> Color(0xFF4CAF50)
                    HopStatus.TIMEOUT -> Color(0xFFFF9800)
                    HopStatus.ERROR -> Color(0xFFF44336)
                },
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

