package cv.toolkit.screens

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cv.toolkit.R
import cv.toolkit.ads.AdMobManager
import cv.toolkit.ads.BannerAd
import kotlinx.coroutines.*
import java.net.InetSocketAddress
import java.net.Socket

data class PortResult(val port: Int, val isOpen: Boolean, val service: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortScanScreen(navController: NavController) {
    val context = LocalContext.current
    val activity = context as? Activity
    var scanType by remember { mutableIntStateOf(0) } // 0=Local, 1=Single IP, 2=IP Range
    var targetIp by remember { mutableStateOf("") }
    var startIp by remember { mutableStateOf("") }
    var endIp by remember { mutableStateOf("") }
    var startPort by remember { mutableStateOf("1") }
    var endPort by remember { mutableStateOf("1024") }
    var portPreset by remember { mutableIntStateOf(0) }
    var isScanning by remember { mutableStateOf(false) }

    val portPresets = listOf(
        "Custom" to (0 to 0),
        "Well Known (1-1024)" to (1 to 1024),
        "Common (1-10000)" to (1 to 10000),
        "Web (80,443,8080,8443)" to null,
        "Database (3306,5432,27017,6379)" to null,
        "Remote (22,23,3389,5900)" to null,
        "Mail (25,110,143,465,587,993,995)" to null
    )

    val servicePorts = mapOf(
        3 to listOf(80, 443, 8080, 8443),
        4 to listOf(3306, 5432, 27017, 6379, 1433, 1521),
        5 to listOf(22, 23, 3389, 5900, 5901),
        6 to listOf(25, 110, 143, 465, 587, 993, 995)
    )
    var progress by remember { mutableFloatStateOf(0f) }
    val results = remember { mutableStateListOf<PortResult>() }

    val scope = rememberCoroutineScope()

    fun getLocalIp(): String {
        return try {
            java.net.NetworkInterface.getNetworkInterfaces().toList()
                .flatMap { it.inetAddresses.toList() }
                .firstOrNull { !it.isLoopbackAddress && it is java.net.Inet4Address }
                ?.hostAddress ?: "127.0.0.1"
        } catch (_: Exception) { "127.0.0.1" }
    }

    fun getServiceName(port: Int): String = when (port) {
        21 -> "FTP"; 22 -> "SSH"; 23 -> "Telnet"; 25 -> "SMTP"; 53 -> "DNS"
        80 -> "HTTP"; 110 -> "POP3"; 143 -> "IMAP"; 443 -> "HTTPS"; 445 -> "SMB"
        465 -> "SMTPS"; 587 -> "SMTP"; 993 -> "IMAPS"; 995 -> "POP3S"
        1433 -> "MSSQL"; 1521 -> "Oracle"; 3306 -> "MySQL"; 3389 -> "RDP"
        5432 -> "PostgreSQL"; 5900 -> "VNC"; 5901 -> "VNC"; 6379 -> "Redis"
        8080 -> "HTTP-Alt"; 8443 -> "HTTPS-Alt"; 27017 -> "MongoDB"; else -> ""
    }

    fun scanPort(ip: String, port: Int, timeout: Int = 200): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(ip, port), timeout)
                true
            }
        } catch (_: Exception) { false }
    }

    fun startScan() {
        val ips = when (scanType) {
            0 -> listOf(getLocalIp())
            1 -> listOf(targetIp)
            else -> {
                val start = startIp.split(".").lastOrNull()?.toIntOrNull() ?: 1
                val end = endIp.split(".").lastOrNull()?.toIntOrNull() ?: 254
                val prefix = startIp.substringBeforeLast(".")
                (start..end).map { "$prefix.$it" }
            }
        }

        val portsToScan = when {
            portPreset >= 3 -> servicePorts[portPreset] ?: emptyList()
            else -> {
                val portStart = startPort.toIntOrNull() ?: 1
                val portEnd = endPort.toIntOrNull() ?: 1024
                (portStart..portEnd).toList()
            }
        }

        isScanning = true
        results.clear()
        progress = 0f

        scope.launch(Dispatchers.IO) {
            val totalScans = ips.size * portsToScan.size
            var completed = 0
            val openPorts = mutableListOf<PortResult>()

            ips.forEach { ip ->
                portsToScan.chunked(50).forEach { chunk ->
                    chunk.map { port ->
                        async {
                            val isOpen = scanPort(ip, port)
                            if (isOpen) {
                                synchronized(openPorts) {
                                    openPorts.add(PortResult(port, true, getServiceName(port)))
                                }
                            }
                            completed++
                            progress = completed.toFloat() / totalScans
                        }
                    }.awaitAll()
                    results.clear()
                    results.addAll(openPorts.toList().sortedBy { it.port })
                }
            }
            isScanning = false
            // Track usage for interstitial ad (every 4 scans)
            activity?.let { AdMobManager.trackPortScanUsage(it) }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.port_scan_title)) },
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
            // Scan Type Tabs
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(selected = scanType == 0, onClick = { scanType = 0 }, shape = SegmentedButtonDefaults.itemShape(0, 3)) { Text("Local") }
                SegmentedButton(selected = scanType == 1, onClick = { scanType = 1 }, shape = SegmentedButtonDefaults.itemShape(1, 3)) { Text("Single IP") }
                SegmentedButton(selected = scanType == 2, onClick = { scanType = 2 }, shape = SegmentedButtonDefaults.itemShape(2, 3)) { Text("IP Range") }
            }

            // IP Input
            when (scanType) {
                1 -> OutlinedTextField(
                    value = targetIp,
                    onValueChange = { targetIp = it },
                    label = { Text("Target IP") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isScanning,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                2 -> Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startIp,
                        onValueChange = { startIp = it },
                        label = { Text("Start IP") },
                        modifier = Modifier.weight(1f),
                        enabled = !isScanning,
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = endIp,
                        onValueChange = { endIp = it },
                        label = { Text("End IP") },
                        modifier = Modifier.weight(1f),
                        enabled = !isScanning,
                        singleLine = true
                    )
                }
            }

            // Port Preset
            var presetExpanded by remember { mutableStateOf(false) }
            ExposedDropdownMenuBox(expanded = presetExpanded, onExpandedChange = { if (!isScanning) presetExpanded = it }) {
                OutlinedTextField(
                    value = portPresets[portPreset].first,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Port Preset") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(presetExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true),
                    enabled = !isScanning
                )
                ExposedDropdownMenu(expanded = presetExpanded, onDismissRequest = { presetExpanded = false }) {
                    portPresets.forEachIndexed { index, (name, range) ->
                        DropdownMenuItem(
                            text = { Text(name) },
                            onClick = {
                                portPreset = index
                                if (range != null && index < 3) {
                                    startPort = range.first.toString()
                                    endPort = range.second.toString()
                                }
                                presetExpanded = false
                            }
                        )
                    }
                }
            }

            // Port Range (only for custom/range presets)
            if (portPreset < 3) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = startPort,
                        onValueChange = { startPort = it; portPreset = 0 },
                        label = { Text("Start Port") },
                        modifier = Modifier.weight(1f),
                        enabled = !isScanning,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    OutlinedTextField(
                        value = endPort,
                        onValueChange = { endPort = it; portPreset = 0 },
                        label = { Text("End Port") },
                        modifier = Modifier.weight(1f),
                        enabled = !isScanning,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                }
            }

            // Progress
            if (isScanning) {
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                Text("Scanning... ${(progress * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
            }

            // Scan Button
            Button(
                onClick = { if (isScanning) { isScanning = false } else startScan() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(if (isScanning) Icons.Filled.Stop else Icons.Filled.Search, null)
                Spacer(Modifier.width(8.dp))
                Text(if (isScanning) "Stop" else "Start Scan")
            }

            // Results
            Text("Open Ports: ${results.size}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                items(results) { result ->
                    PortResultCard(result)
                }
            }
            BannerAd(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun PortResultCard(result: PortResult) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Port ${result.port}", fontWeight = FontWeight.Bold)
                if (result.service.isNotEmpty()) {
                    Text(result.service, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text("OPEN", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
    }
}
