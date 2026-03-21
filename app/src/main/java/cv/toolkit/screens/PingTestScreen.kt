package cv.toolkit.screens

import android.app.Activity
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cv.toolkit.R
import cv.toolkit.ads.AdMobManager
import cv.toolkit.ads.BannerAd
import kotlinx.coroutines.*
import java.net.InetAddress

data class PingResult(val seq: Int, val time: Long, val success: Boolean)

// Ping count modes similar to CMD ping command
enum class PingCountMode(val label: String, val count: Int) {
    FOUR("4 times", 4),           // Default Windows ping
    TEN("10 times", 10),
    TWENTY("20 times", 20),
    UNLIMITED("Unlimited (-t)", -1),  // Like ping -t
    CUSTOM("Custom", 0)
}

// Packet size options (in bytes, excluding ICMP header)
enum class PacketSizeMode(val label: String, val size: Int) {
    DEFAULT("32 bytes", 32),      // Windows default
    SMALL("56 bytes", 56),        // Linux default
    MEDIUM("128 bytes", 128),
    LARGE("512 bytes", 512),
    JUMBO("1024 bytes", 1024),
    MAX("1472 bytes", 1472),      // Max without fragmentation (MTU 1500 - IP header 20 - ICMP header 8)
    CUSTOM("Custom", 0)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PingTestScreen(navController: NavController) {
    val context = LocalContext.current
    val activity = context as? Activity
    var target by remember { mutableStateOf("") }
    var isPinging by remember { mutableStateOf(false) }
    val results = remember { mutableStateListOf<PingResult>() }
    var resolvedIp by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    // Ping count options
    var selectedMode by remember { mutableStateOf(PingCountMode.FOUR) }
    var customCount by remember { mutableStateOf("") }
    var showCustomDialog by remember { mutableStateOf(false) }
    var pingJob by remember { mutableStateOf<Job?>(null) }

    // Packet size options
    var selectedPacketSize by remember { mutableStateOf(PacketSizeMode.DEFAULT) }
    var customPacketSize by remember { mutableStateOf("") }
    var showPacketSizeDialog by remember { mutableStateOf(false) }

    val popularTargets = listOf(
        "google.com", "facebook.com", "youtube.com", "bing.com",
        "amazon.com", "apple.com", "huawei.com"
    )

    fun stopPing() {
        isPinging = false
        pingJob?.cancel()
        pingJob = null
        // Track usage for interstitial ad (every 5 pings)
        activity?.let { AdMobManager.trackPingUsage(it) }
    }

    fun startPing() {
        if (target.isBlank()) return
        isPinging = true
        results.clear()
        resolvedIp = ""

        // Determine ping count based on selected mode
        val pingCount = when (selectedMode) {
            PingCountMode.CUSTOM -> customCount.toIntOrNull() ?: 4
            PingCountMode.UNLIMITED -> Int.MAX_VALUE  // Effectively unlimited
            else -> selectedMode.count
        }

        // Determine packet size
        val packetSize = when (selectedPacketSize) {
            PacketSizeMode.CUSTOM -> customPacketSize.toIntOrNull() ?: 32
            else -> selectedPacketSize.size
        }

        pingJob = scope.launch(Dispatchers.IO) {
            resolvedIp = try {
                InetAddress.getByName(target).hostAddress ?: ""
            } catch (_: Exception) { "" }

            var seq = 0
            while (seq < pingCount && isPinging) {
                val result = try {
                    // Use native ping command with packet size
                    val start = System.currentTimeMillis()
                    val process = Runtime.getRuntime().exec(
                        arrayOf("ping", "-c", "1", "-W", "3", "-s", "$packetSize", target)
                    )
                    val output = process.inputStream.bufferedReader().readText()
                    process.waitFor()
                    val elapsed = System.currentTimeMillis() - start

                    // Extract RTT from ping output
                    val rttMatch = Regex("time[=<]\\s*([\\d.]+)").find(output)
                    val rttTime = rttMatch?.groupValues?.getOrNull(1)?.toDoubleOrNull()?.toLong() ?: elapsed

                    val success = process.exitValue() == 0 && output.contains("time=")
                    PingResult(seq + 1, if (success) rttTime else -1, success)
                } catch (_: Exception) {
                    // Fallback to Java-based ping (doesn't support packet size)
                    val start = System.currentTimeMillis()
                    val reachable = try {
                        InetAddress.getByName(target).isReachable(3000)
                    } catch (_: Exception) { false }
                    val time = System.currentTimeMillis() - start
                    PingResult(seq + 1, if (reachable) time else -1, reachable)
                }
                results.add(result)
                seq++
                if (seq < pingCount && isPinging) {
                    delay(1000)
                }
            }
            isPinging = false
            // Track usage for interstitial ad (every 5 pings)
            activity?.let { AdMobManager.trackPingUsage(it) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ping_test_title)) },
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
                enabled = !isPinging,
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
                        enabled = !isPinging
                    )
                }
            }

            // Ping count mode selector
            Text("Ping Count", style = MaterialTheme.typography.labelMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(PingCountMode.entries.toList()) { mode ->
                    FilterChip(
                        selected = selectedMode == mode,
                        onClick = {
                            if (mode == PingCountMode.CUSTOM) {
                                showCustomDialog = true
                            }
                            selectedMode = mode
                        },
                        label = {
                            Text(
                                if (mode == PingCountMode.CUSTOM && customCount.isNotEmpty())
                                    "$customCount times"
                                else
                                    mode.label
                            )
                        },
                        enabled = !isPinging
                    )
                }
            }

            // Packet size selector
            Text("Packet Size", style = MaterialTheme.typography.labelMedium)
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(PacketSizeMode.entries.toList()) { sizeMode ->
                    FilterChip(
                        selected = selectedPacketSize == sizeMode,
                        onClick = {
                            if (sizeMode == PacketSizeMode.CUSTOM) {
                                showPacketSizeDialog = true
                            }
                            selectedPacketSize = sizeMode
                        },
                        label = {
                            Text(
                                if (sizeMode == PacketSizeMode.CUSTOM && customPacketSize.isNotEmpty())
                                    "$customPacketSize bytes"
                                else
                                    sizeMode.label
                            )
                        },
                        enabled = !isPinging
                    )
                }
            }

            Button(
                onClick = { if (isPinging) stopPing() else startPing() },
                modifier = Modifier.fillMaxWidth(),
                enabled = target.isNotBlank() || isPinging
            ) {
                Icon(if (isPinging) Icons.Filled.Stop else Icons.Filled.NetworkPing, null)
                Spacer(Modifier.width(8.dp))
                Text(if (isPinging) "Stop" else "Start Ping")
            }

            if (resolvedIp.isNotEmpty()) {
                Text("Resolved IP: $resolvedIp", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }

            if (results.isNotEmpty()) {
                val successful = results.count { it.success }
                val avgTime = results.filter { it.success }.map { it.time }.average().takeIf { !it.isNaN() }?.toLong() ?: 0
                val minTime = results.filter { it.success }.minOfOrNull { it.time } ?: 0
                val maxTime = results.filter { it.success }.maxOfOrNull { it.time } ?: 0

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatItem("Sent", "${results.size}")
                        StatItem("Success", "$successful")
                        StatItem("Loss", "${((results.size - successful) * 100) / results.size}%")
                        StatItem("Avg", "${avgTime}ms")
                        StatItem("Min", "${minTime}ms")
                        StatItem("Max", "${maxTime}ms")
                    }
                }
            }

            Text("Results", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                items(results) { result ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                if (result.success) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                                null,
                                tint = if (result.success) Color(0xFF4CAF50) else Color(0xFFF44336)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text("Seq ${result.seq}", modifier = Modifier.weight(1f))
                            Text(
                                if (result.success) "${result.time}ms" else "Timeout",
                                fontWeight = FontWeight.Medium,
                                color = if (result.success) MaterialTheme.colorScheme.onSurface else Color(0xFFF44336)
                            )
                        }
                    }
                }
            }
            BannerAd(modifier = Modifier.fillMaxWidth())
        }
    }

    // Custom ping count dialog
    if (showCustomDialog) {
        var tempCount by remember { mutableStateOf(customCount.ifEmpty { "10" }) }
        AlertDialog(
            onDismissRequest = {
                showCustomDialog = false
                if (customCount.isEmpty()) {
                    selectedMode = PingCountMode.FOUR
                }
            },
            title = { Text("Custom Ping Count") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter the number of ping requests (1-1000):")
                    OutlinedTextField(
                        value = tempCount,
                        onValueChange = { value ->
                            if (value.isEmpty() || value.all { it.isDigit() }) {
                                tempCount = value
                            }
                        },
                        label = { Text("Count") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val count = tempCount.toIntOrNull()
                        if (count != null && count in 1..1000) {
                            customCount = tempCount
                            showCustomDialog = false
                        }
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showCustomDialog = false
                        if (customCount.isEmpty()) {
                            selectedMode = PingCountMode.FOUR
                        }
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Custom packet size dialog
    if (showPacketSizeDialog) {
        var tempSize by remember { mutableStateOf(customPacketSize.ifEmpty { "64" }) }
        AlertDialog(
            onDismissRequest = {
                showPacketSizeDialog = false
                if (customPacketSize.isEmpty()) {
                    selectedPacketSize = PacketSizeMode.DEFAULT
                }
            },
            title = { Text("Custom Packet Size") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter packet size in bytes (1-65500):")
                    Text(
                        "Note: Sizes > 1472 bytes may cause fragmentation",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = tempSize,
                        onValueChange = { value ->
                            if (value.isEmpty() || value.all { it.isDigit() }) {
                                tempSize = value
                            }
                        },
                        label = { Text("Size (bytes)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val size = tempSize.toIntOrNull()
                        if (size != null && size in 1..65500) {
                            customPacketSize = tempSize
                            showPacketSizeDialog = false
                        }
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPacketSizeDialog = false
                        if (customPacketSize.isEmpty()) {
                            selectedPacketSize = PacketSizeMode.DEFAULT
                        }
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
