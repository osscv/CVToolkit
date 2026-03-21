package cv.toolkit.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import java.net.NetworkInterface

data class NetworkDevice(
    val ip: String,
    val hostname: String,
    val macAddress: String,
    val vendor: String,
    val model: String,
    val version: String,
    val isOnline: Boolean,
    val responseTime: Long,
    val deviceType: DeviceType
)

enum class DeviceType(val displayName: String) {
    ROUTER("Router"),
    SWITCH("Switch"),
    ACCESS_POINT("Access Point"),
    PHONE("Phone"),
    TABLET("Tablet"),
    LAPTOP("Laptop"),
    DESKTOP("Desktop"),
    SMART_TV("Smart TV"),
    GAME_CONSOLE("Game Console"),
    PRINTER("Printer"),
    CAMERA("Camera"),
    IOT("IoT Device"),
    NAS("NAS Storage"),
    MEDIA_PLAYER("Media Player"),
    SMART_SPEAKER("Smart Speaker"),
    WEARABLE("Wearable"),
    DEVICE("Device"),
    UNKNOWN("Unknown")
}

enum class ScanMethod { ICMP, ARP, UDP, ALL }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkScanScreen(navController: NavController) {
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    var isScanning by remember { mutableStateOf(false) }
    var progress by remember { mutableFloatStateOf(0f) }
    val devices = remember { mutableStateListOf<NetworkDevice>() }
    var localIp by remember { mutableStateOf("") }
    var gatewayIp by remember { mutableStateOf("") }
    var netmask by remember { mutableStateOf("") }
    var dns by remember { mutableStateOf("") }
    var scanMethod by remember { mutableStateOf(ScanMethod.ALL) }
    var hasAutoScanned by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val effectiveNetmask = remember(localIp, netmask) { chooseScanNetmask(localIp, netmask) }

    LaunchedEffect(Unit) {
        localIp = getLocalIpAddress()
        gatewayIp = getGatewayIp(localIp)
        netmask = getNetmask()
        dns = getDnsServers()
    }

    fun startScan() {
        if (localIp.isEmpty()) return
        isScanning = true
        devices.clear()
        progress = 0f

        scope.launch(Dispatchers.IO) {
            val foundDevices = java.util.concurrent.ConcurrentHashMap<String, NetworkDevice>()

            // Calculate IP range based on netmask
            val ipRange = calculateIpRange(localIp, effectiveNetmask)
            val networkCidr = "${toIpString(ipRange.second.first)}/${ipRange.second.third}"

            // Try native tools first (fastest)
            progress = 0.05f

            // Method 1: Try nmap if available (most comprehensive)
            val nmapResults = tryNmapScan(networkCidr)
            if (nmapResults.isNotEmpty()) {
                nmapResults.forEach { (ip, mac, hostname) ->
                    val vendor = getVendorFromMac(mac)
                    foundDevices[ip] = NetworkDevice(ip, hostname, mac, vendor, "", "", true, 0, DeviceType.DEVICE)
                }
                progress = 0.6f
            } else {
                // Method 2: Try arp-scan if available
                val arpScanResults = tryArpScan(networkCidr)
                if (arpScanResults.isNotEmpty()) {
                    arpScanResults.forEach { (ip, mac) ->
                        val vendor = getVendorFromMac(mac)
                        foundDevices[ip] = NetworkDevice(ip, "", mac, vendor, "", "", true, 0, DeviceType.DEVICE)
                    }
                    progress = 0.5f
                } else {
                    // Method 3: Use ping sweep with fping or native ping
                    val pingResults = tryPingSweep(ipRange.first)
                    pingResults.forEach { ip ->
                        val mac = getMacFromArp(ip)
                        val vendor = getVendorFromMac(mac)
                        foundDevices[ip] = NetworkDevice(ip, "", mac, vendor, "", "", true, 0, DeviceType.DEVICE)
                    }
                    progress = 0.4f
                }

                // Always check ARP table
                val arpEntries = getArpTable()
                arpEntries.filter { isIpInRange(it.first, ipRange.second) }.forEach { (ip, mac) ->
                    if (mac != "00:00:00:00:00:00" && !foundDevices.containsKey(ip)) {
                        val vendor = getVendorFromMac(mac)
                        foundDevices[ip] = NetworkDevice(ip, "", mac, vendor, "", "", true, 0, DeviceType.DEVICE)
                    }
                }
            }

            devices.clear()
            devices.addAll(sortDevicesByIp(foundDevices.values.toList()))
            progress = progress.coerceAtLeast(0.2f)

            // Always run Java-based batch scan for remaining IPs to avoid missing devices when native tools fail
            val batchSize = 192
            val scannedCount = java.util.concurrent.atomic.AtomicInteger(0)
            val allIps = ipRange.first
            val totalIps = allIps.size.coerceAtLeast(1)
            val initialProgress = progress.coerceAtLeast(0.1f)

            // Mass ARP trigger across full range
            allIps.chunked(batchSize).map { chunk ->
                async { chunk.forEach { ip -> triggerArpEntry(ip) } }
            }.awaitAll()
            delay(300)

            // Read ARP table
            getArpTable().filter { isIpInRange(it.first, ipRange.second) }.forEach { (ip, mac) ->
                if (mac != "00:00:00:00:00:00" && !foundDevices.containsKey(ip)) {
                    val vendor = getVendorFromMac(mac)
                    foundDevices[ip] = NetworkDevice(ip, "", mac, vendor, "", "", true, 0, DeviceType.DEVICE)
                }
            }

            // Fast batch ping any remaining IPs (progress up to 65%)
            val remainingIps = allIps.filter { !foundDevices.containsKey(it) }
            remainingIps.chunked(batchSize).forEach { chunk ->
                if (!isScanning) return@launch
                chunk.map { ip ->
                    async {
                        val result = fastPing(ip)
                        if (result.first) {
                            val mac = getMacFromArp(ip)
                            val vendor = getVendorFromMac(mac)
                            foundDevices.putIfAbsent(ip, NetworkDevice(ip, "", mac, vendor, "", "", true, result.second, DeviceType.DEVICE))
                        }
                        progress = initialProgress + (scannedCount.incrementAndGet().toFloat() / totalIps * 0.5f)
                    }
                }.awaitAll()
                devices.clear()
                devices.addAll(sortDevicesByIp(foundDevices.values.toList()))
            }

            // Ensure gateway is present even if not caught by scans
            if (gatewayIp.isNotEmpty() && !foundDevices.containsKey(gatewayIp)) {
                val gwPing = fastPing(gatewayIp)
                val gwMac = getMacFromArp(gatewayIp)
                val gwVendor = getVendorFromMac(gwMac)
                if (gwPing.first || gwMac.isNotEmpty()) {
                    val gwInfo = getDeviceInfoFast(gatewayIp, gwMac, gwVendor, true)
                    foundDevices[gatewayIp] = NetworkDevice(
                        gatewayIp,
                        gwInfo.hostname.ifEmpty { "Gateway" },
                        gwMac,
                        if (gwInfo.vendor.isNotEmpty()) gwInfo.vendor else gwVendor,
                        gwInfo.model,
                        gwInfo.version,
                        true,
                        gwPing.second,
                        if (gwInfo.type != DeviceType.UNKNOWN) gwInfo.type else DeviceType.ROUTER
                    )
                    devices.clear()
                devices.addAll(sortDevicesByIp(foundDevices.values.toList()))
                }
            }

            // Enrich device info (parallel)
            progress = 0.7f
            val devicesToEnrich = foundDevices.values.toList()
            val enrichedCount = java.util.concurrent.atomic.AtomicInteger(0)

            devicesToEnrich.chunked(32).forEach { chunk ->
                if (!isScanning) return@launch
                chunk.map { device ->
                    async {
                        val deviceInfo = getDeviceInfoFast(device.ip, device.macAddress, device.vendor, device.ip == gatewayIp)
                        foundDevices[device.ip] = device.copy(
                            hostname = if (deviceInfo.hostname.isNotEmpty()) deviceInfo.hostname else device.hostname,
                            vendor = if (deviceInfo.vendor.isNotEmpty()) deviceInfo.vendor else device.vendor,
                            model = deviceInfo.model,
                            version = deviceInfo.version,
                            deviceType = deviceInfo.type
                        )
                        progress = 0.7f + (enrichedCount.incrementAndGet().toFloat() / devicesToEnrich.size * 0.25f)
                    }
                }.awaitAll()
                devices.clear()
                devices.addAll(sortDevicesByIp(foundDevices.values.toList()))
            }

            // Final ARP check
            progress = 0.95f
            delay(200)
            getArpTable().filter { isIpInRange(it.first, ipRange.second) && !foundDevices.containsKey(it.first) }.forEach { (ip, mac) ->
                if (mac != "00:00:00:00:00:00") {
                    val vendor = getVendorFromMac(mac)
                    val deviceInfo = getDeviceInfoFast(ip, mac, vendor, ip == gatewayIp)
                    foundDevices[ip] = NetworkDevice(ip, deviceInfo.hostname, mac,
                        if (deviceInfo.vendor.isNotEmpty()) deviceInfo.vendor else vendor,
                        deviceInfo.model, deviceInfo.version, true, 0, deviceInfo.type)
                }
            }

            devices.clear()
                devices.addAll(sortDevicesByIp(foundDevices.values.toList()))
            progress = 1f
            isScanning = false
            // Track usage for interstitial ad (every 2 scans)
            activity?.let { AdMobManager.trackDeviceDiscoveryUsage(it) }
        }
    }

    LaunchedEffect(localIp) {
        if (localIp.isNotEmpty() && !hasAutoScanned && !isScanning) {
            hasAutoScanned = true
            startScan()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.network_scan_title)) },
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
            // Network Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Network Info", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    NetworkInfoRow("Your IP", localIp)
                    NetworkInfoRow("Gateway", gatewayIp)
                    NetworkInfoRow("Netmask", effectiveNetmask)
                    NetworkInfoRow("Scan Range", getScanRangeDisplay(localIp, effectiveNetmask))
                    NetworkInfoRow("DNS", dns)
                }
            }

            // Scan Method Selection
            Text("Scan Method", style = MaterialTheme.typography.labelMedium)
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ScanMethod.entries.forEachIndexed { index, method ->
                    SegmentedButton(
                        selected = scanMethod == method,
                        onClick = { scanMethod = method },
                        shape = SegmentedButtonDefaults.itemShape(index, ScanMethod.entries.size),
                        enabled = !isScanning
                    ) { Text(method.name) }
                }
            }

            // Progress
            if (isScanning) {
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth())
                Text("Scanning... ${(progress * 100).toInt()}%", style = MaterialTheme.typography.bodySmall)
            }

            // Scan Button
            Button(
                onClick = { if (isScanning) isScanning = false else startScan() },
                modifier = Modifier.fillMaxWidth(),
                enabled = localIp.isNotEmpty()
            ) {
                Icon(if (isScanning) Icons.Filled.Stop else Icons.Filled.Radar, null)
                Spacer(Modifier.width(8.dp))
                Text(if (isScanning) "Stop" else "Scan Network")
            }

            // Results Summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Devices Found: ${devices.size}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row {
                    Icon(Icons.Filled.Circle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(12.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("${devices.count { it.isOnline }} Online", style = MaterialTheme.typography.bodySmall)
                }
            }

            // Device List
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                items(devices) { device ->
                    DeviceCard(device, device.ip == localIp, device.ip == gatewayIp)
                }
            }
            BannerAd(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun DeviceCard(device: NetworkDevice, isLocal: Boolean, isGateway: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                when (device.deviceType) {
                    DeviceType.ROUTER -> Icons.Filled.Router
                    DeviceType.SWITCH -> Icons.Filled.Hub
                    DeviceType.ACCESS_POINT -> Icons.Filled.Wifi
                    DeviceType.PHONE -> Icons.Filled.Smartphone
                    DeviceType.TABLET -> Icons.Filled.Tablet
                    DeviceType.LAPTOP -> Icons.Filled.Laptop
                    DeviceType.DESKTOP -> Icons.Filled.Computer
                    DeviceType.SMART_TV -> Icons.Filled.Tv
                    DeviceType.GAME_CONSOLE -> Icons.Filled.SportsEsports
                    DeviceType.PRINTER -> Icons.Filled.Print
                    DeviceType.CAMERA -> Icons.Filled.CameraAlt
                    DeviceType.IOT -> Icons.Filled.Sensors
                    DeviceType.NAS -> Icons.Filled.Storage
                    DeviceType.MEDIA_PLAYER -> Icons.Filled.PlayCircle
                    DeviceType.SMART_SPEAKER -> Icons.Filled.Speaker
                    DeviceType.WEARABLE -> Icons.Filled.Watch
                    else -> Icons.Filled.Devices
                },
                null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        device.hostname.ifEmpty { device.vendor.ifEmpty { "Unknown Device" } },
                        fontWeight = FontWeight.Bold
                    )
                    if (isLocal) {
                        Spacer(Modifier.width(8.dp))
                        SuggestionChip(onClick = {}, label = { Text("You", style = MaterialTheme.typography.labelSmall) })
                    }
                    if (isGateway) {
                        Spacer(Modifier.width(8.dp))
                        SuggestionChip(onClick = {}, label = { Text("Gateway", style = MaterialTheme.typography.labelSmall) })
                    }
                }
                // Show device type
                Text(device.deviceType.displayName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                // Show vendor if different from hostname
                if (device.vendor.isNotEmpty() && device.vendor != device.hostname) {
                    Text(device.vendor, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                }
                // Show model and version
                if (device.model.isNotEmpty() || device.version.isNotEmpty()) {
                    Text(
                        listOf(device.model, device.version).filter { it.isNotEmpty() }.joinToString(" - "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(device.ip, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (device.macAddress.isNotEmpty()) {
                    Text(device.macAddress, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (device.responseTime > 0) {
                    Text("${device.responseTime}ms", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Icon(Icons.Filled.Circle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(12.dp))
        }
    }
}

private fun getLocalIpAddress(): String {
    return try {
        NetworkInterface.getNetworkInterfaces().toList()
            .flatMap { it.inetAddresses.toList() }
            .firstOrNull { !it.isLoopbackAddress && it is java.net.Inet4Address }
            ?.hostAddress ?: ""
    } catch (_: Exception) { "" }
}

private fun getGatewayIp(localIp: String): String {
    return if (localIp.isNotEmpty()) "${localIp.substringBeforeLast(".")}.1" else ""
}

private fun getNetmask(): String {
    return try {
        NetworkInterface.getNetworkInterfaces().toList()
            .flatMap { it.interfaceAddresses }
            .firstOrNull { it.address is java.net.Inet4Address && !it.address.isLoopbackAddress }
            ?.let { prefixToNetmask(it.networkPrefixLength.toInt()) } ?: ""
    } catch (_: Exception) { "" }
}

// Pick a wider scan mask for private networks when DHCP mask is too narrow (common on segmented Wi-Fi)
private fun chooseScanNetmask(localIp: String, netmask: String): String {
    val prefix = netmaskToPrefix(netmask) ?: 24
    val isPrivate10 = localIp.startsWith("10.")
    val isPrivate172 = localIp.startsWith("172.")
    // Expand to /16 for 10.x.x.x or 172.x.x.x when DHCP hands out /24, so we sweep adjacent subnets
    return when {
        (isPrivate10 || isPrivate172) && prefix > 16 -> "255.255.0.0"
        netmask.isNotEmpty() -> netmask
        else -> "255.255.255.0"
    }
}

private fun prefixToNetmask(prefix: Int): String {
    val mask = -1 shl (32 - prefix)
    return "${(mask shr 24) and 0xFF}.${(mask shr 16) and 0xFF}.${(mask shr 8) and 0xFF}.${mask and 0xFF}"
}

private fun netmaskToPrefix(mask: String): Int? {
    val parts = mask.split(".")
    if (parts.size != 4) return null
    val maskLong = parts.fold(0L) { acc, part ->
        val value = part.toIntOrNull() ?: return null
        (acc shl 8) or value.toLong()
    }
    return maskLong.countOneBits()
}

private fun getDnsServers(): String {
    return try {
        val process = Runtime.getRuntime().exec("getprop net.dns1")
        val reader = java.io.BufferedReader(java.io.InputStreamReader(process.inputStream))
        val dns1 = reader.readLine()?.trim() ?: ""
        reader.close()

        val process2 = Runtime.getRuntime().exec("getprop net.dns2")
        val reader2 = java.io.BufferedReader(java.io.InputStreamReader(process2.inputStream))
        val dns2 = reader2.readLine()?.trim() ?: ""
        reader2.close()

        listOf(dns1, dns2).filter { it.isNotEmpty() }.joinToString(", ").ifEmpty { "8.8.8.8" }
    } catch (_: Exception) { "" }
}

@Composable
private fun NetworkInfoRow(label: String, value: String) {
    Row {
        Text("$label: ", color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value.ifEmpty { "Detecting..." }, fontWeight = FontWeight.Medium)
    }
}

// Get display string for scan range
private fun getScanRangeDisplay(localIp: String, netmask: String): String {
    if (localIp.isEmpty()) return ""

    val ipParts = localIp.split(".").map { it.toIntOrNull() ?: 0 }
    val maskParts = if (netmask.isNotEmpty() && netmask.contains(".")) {
        netmask.split(".").map { it.toIntOrNull() ?: 255 }
    } else {
        listOf(255, 255, 255, 0)
    }

    val maskLong = (maskParts[0].toLong() shl 24) or (maskParts[1].toLong() shl 16) or
                   (maskParts[2].toLong() shl 8) or maskParts[3].toLong()
    val prefixLen = maskLong.countOneBits()

    // Calculate network address
    val networkParts = ipParts.zip(maskParts).map { (ip, mask) -> ip and mask }

    // Calculate host count
    val hostBits = 32 - prefixLen
    val hostCount = if (hostBits > 16) 65534 else ((1 shl hostBits) - 2).coerceAtLeast(0)

    return "${networkParts.joinToString(".")}/$prefixLen ($hostCount hosts)"
}

// Calculate IP range based on netmask - returns (list of IPs to scan, network info for filtering)
private fun calculateIpRange(localIp: String, netmask: String): Pair<List<String>, Triple<Long, Long, Int>> {
    val ipParts = localIp.split(".").map { it.toIntOrNull() ?: 0 }
    val maskParts = if (netmask.isNotEmpty() && netmask.contains(".")) {
        netmask.split(".").map { it.toIntOrNull() ?: 255 }
    } else {
        listOf(255, 255, 255, 0) // Default to /24
    }

    val ipLong = (ipParts[0].toLong() shl 24) or (ipParts[1].toLong() shl 16) or
                 (ipParts[2].toLong() shl 8) or ipParts[3].toLong()
    val maskLong = (maskParts[0].toLong() shl 24) or (maskParts[1].toLong() shl 16) or
                   (maskParts[2].toLong() shl 8) or maskParts[3].toLong()

    val networkAddr = ipLong and maskLong
    val broadcastAddr = networkAddr or (maskLong.inv() and 0xFFFFFFFFL)

    // Calculate prefix length for display
    val prefixLen = maskLong.countOneBits()

    // Generate list of IPs to scan (excluding network and broadcast addresses)
    // Limit to max 65534 IPs (/16 network) to prevent memory issues
    val startIp = networkAddr + 1
    val endIp = minOf(broadcastAddr - 1, networkAddr + 65534)

    val ips = mutableListOf<String>()
    var current = startIp
    while (current <= endIp) {
        val a = ((current shr 24) and 0xFF).toInt()
        val b = ((current shr 16) and 0xFF).toInt()
        val c = ((current shr 8) and 0xFF).toInt()
        val d = (current and 0xFF).toInt()
        ips.add("$a.$b.$c.$d")
        current++
    }

    return Pair(ips, Triple(networkAddr, broadcastAddr, prefixLen))
}

private fun toIpString(ipLong: Long): String {
    val a = ((ipLong shr 24) and 0xFF).toInt()
    val b = ((ipLong shr 16) and 0xFF).toInt()
    val c = ((ipLong shr 8) and 0xFF).toInt()
    val d = (ipLong and 0xFF).toInt()
    return "$a.$b.$c.$d"
}

// Check if IP is in the network range
private fun isIpInRange(ip: String, networkInfo: Triple<Long, Long, Int>): Boolean {
    val parts = ip.split(".").map { it.toIntOrNull() ?: 0 }
    if (parts.size != 4) return false

    val ipLong = (parts[0].toLong() shl 24) or (parts[1].toLong() shl 16) or
                 (parts[2].toLong() shl 8) or parts[3].toLong()

    return ipLong > networkInfo.first && ipLong < networkInfo.second
}

// Sort devices by IP address numerically
private fun sortDevicesByIp(devices: List<NetworkDevice>): List<NetworkDevice> {
    return devices.sortedBy { device ->
        val parts = device.ip.split(".").map { it.toIntOrNull() ?: 0 }
        (parts[0].toLong() shl 24) or (parts[1].toLong() shl 16) or
        (parts[2].toLong() shl 8) or parts[3].toLong()
    }
}

// Try nmap scan (if available on rooted device or Termux)
private fun tryNmapScan(networkCidr: String): List<Triple<String, String, String>> {
    val results = mutableListOf<Triple<String, String, String>>()
    try {
        // Try nmap ping scan with MAC detection
        val process = Runtime.getRuntime().exec(arrayOf("nmap", "-sn", "-PR", "--host-timeout", "2s", networkCidr))
        val reader = java.io.BufferedReader(java.io.InputStreamReader(process.inputStream))
        var currentIp = ""
        var currentMac = ""
        var currentHostname = ""

        reader.forEachLine { line ->
            when {
                line.contains("Nmap scan report for") -> {
                    // Save previous device
                    if (currentIp.isNotEmpty()) {
                        results.add(Triple(currentIp, currentMac, currentHostname))
                    }
                    // Parse new device
                    val ipMatch = Regex("(\\d+\\.\\d+\\.\\d+\\.\\d+)").find(line)
                    currentIp = ipMatch?.value ?: ""
                    currentHostname = line.substringAfter("for ").substringBefore(" (").takeIf { !it.contains(".") } ?: ""
                    currentMac = ""
                }
                line.contains("MAC Address:") -> {
                    currentMac = line.substringAfter("MAC Address: ").substringBefore(" ").uppercase()
                }
            }
        }
        // Add last device
        if (currentIp.isNotEmpty()) {
            results.add(Triple(currentIp, currentMac, currentHostname))
        }
        reader.close()
        process.waitFor()
    } catch (_: Exception) {}
    return results
}

// Try arp-scan (if available)
private fun tryArpScan(networkCidr: String): List<Pair<String, String>> {
    val results = mutableListOf<Pair<String, String>>()
    try {
        val process = Runtime.getRuntime().exec(arrayOf("arp-scan", "--localnet", "-q"))
        val reader = java.io.BufferedReader(java.io.InputStreamReader(process.inputStream))

        reader.forEachLine { line ->
            val parts = line.split("\\s+".toRegex())
            if (parts.size >= 2) {
                val ip = parts[0]
                val mac = parts[1].uppercase()
                if (ip.matches(Regex("\\d+\\.\\d+\\.\\d+\\.\\d+")) && mac.matches(Regex("[0-9A-F:]{17}"))) {
                    results.add(Pair(ip, mac))
                }
            }
        }
        reader.close()
        process.waitFor()
    } catch (_: Exception) {}
    return results
}

// Try ping sweep using fping or native ping
private fun tryPingSweep(ips: List<String>): List<String> {
    val results = mutableListOf<String>()

    // Try fping first (fastest)
    try {
        val ipList = ips.joinToString(" ")
        val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", "fping -a -q -i 1 -r 0 $ipList 2>/dev/null"))
        val reader = java.io.BufferedReader(java.io.InputStreamReader(process.inputStream))

        reader.forEachLine { line ->
            if (line.matches(Regex("\\d+\\.\\d+\\.\\d+\\.\\d+"))) {
                results.add(line.trim())
            }
        }
        reader.close()
        process.waitFor()

        if (results.isNotEmpty()) return results
    } catch (_: Exception) {}

    // Fallback: Use native ping with parallel execution
    try {
        // Ping multiple IPs in parallel using shell
        val subnet = ips.firstOrNull()?.substringBeforeLast(".") ?: return results
        val process = Runtime.getRuntime().exec(arrayOf("sh", "-c",
            "for i in \$(seq 1 254); do ping -c 1 -W 1 $subnet.\$i >/dev/null 2>&1 && echo $subnet.\$i & done; wait"))
        val reader = java.io.BufferedReader(java.io.InputStreamReader(process.inputStream))

        reader.forEachLine { line ->
            if (line.matches(Regex("\\d+\\.\\d+\\.\\d+\\.\\d+"))) {
                results.add(line.trim())
            }
        }
        reader.close()
        process.waitFor()
    } catch (_: Exception) {}

    return results
}

// Get full ARP table
private fun getArpTable(): List<Pair<String, String>> {
    return try {
        val process = Runtime.getRuntime().exec("cat /proc/net/arp")
        val reader = java.io.BufferedReader(java.io.InputStreamReader(process.inputStream))
        val lines = reader.readLines()
        reader.close()
        lines.drop(1).mapNotNull { line ->
            val parts = line.split("\\s+".toRegex())
            if (parts.size >= 4 && parts[2] != "0x0") {
                Pair(parts[0], parts[3].uppercase())
            } else null
        }
    } catch (_: Exception) { emptyList() }
}

// Trigger ARP entry without waiting - sends packets to populate ARP cache
private fun triggerArpEntry(ip: String) {
    try {
        // Method 1: UDP packet to common ports including Ubiquiti (10001)
        java.net.DatagramSocket().use { socket ->
            socket.soTimeout = 50
            val data = ByteArray(1) { 0 }
            val ports = listOf(7, 53, 67, 137, 5353, 10001, 1900)
            ports.forEach { port ->
                try {
                    socket.send(java.net.DatagramPacket(data, data.size, InetAddress.getByName(ip), port))
                } catch (_: Exception) {}
            }
        }
    } catch (_: Exception) {}

    // Method 2: Send Ubiquiti Discovery packet
    try {
        java.net.DatagramSocket().use { socket ->
            socket.soTimeout = 50
            // UBNT Discovery Protocol v1 packet
            val ubntDiscovery = byteArrayOf(0x01, 0x00, 0x00, 0x00)
            socket.send(java.net.DatagramPacket(ubntDiscovery, ubntDiscovery.size, InetAddress.getByName(ip), 10001))
        }
    } catch (_: Exception) {}

    try {
        // Method 3: TCP SYN to common ports (non-blocking)
        val socket = java.net.Socket()
        socket.soTimeout = 30
        try {
            socket.connect(java.net.InetSocketAddress(ip, 80), 30)
        } catch (_: Exception) {}
        socket.close()
    } catch (_: Exception) {}
}

// Thorough scan using all methods with longer timeouts
private fun thoroughScan(ip: String): Pair<Boolean, Long> {
    val start = System.currentTimeMillis()

    // Method 1: ICMP
    try {
        if (InetAddress.getByName(ip).isReachable(200)) {
            return Pair(true, System.currentTimeMillis() - start)
        }
    } catch (_: Exception) {}

    // Method 2: TCP connect to multiple ports
    val tcpPorts = listOf(80, 443, 22, 23, 21, 8080, 8443, 445, 139, 53, 8291, 8728, 161, 179)
    for (port in tcpPorts) {
        try {
            java.net.Socket().use { socket ->
                socket.connect(java.net.InetSocketAddress(ip, port), 80)
                return Pair(true, System.currentTimeMillis() - start)
            }
        } catch (_: Exception) {}
    }

    // Method 3: UDP to common ports including Ubiquiti discovery (10001)
    val udpPorts = listOf(53, 67, 68, 123, 137, 138, 161, 5353, 1900, 10001)
    try {
        java.net.DatagramSocket().use { socket ->
            socket.soTimeout = 100
            val data = ByteArray(1) { 0 }
            udpPorts.forEach { port ->
                try {
                    socket.send(java.net.DatagramPacket(data, data.size, InetAddress.getByName(ip), port))
                } catch (_: Exception) {}
            }
        }
    } catch (_: Exception) {}

    // Method 4: Check ARP table
    val mac = getMacFromArp(ip)
    if (mac.isNotEmpty() && mac != "00:00:00:00:00:00") {
        return Pair(true, System.currentTimeMillis() - start)
    }

    return Pair(false, 0)
}

// Enhanced ARP ping scan
private fun arpPingScan(ip: String, timeout: Int = 400): Pair<Boolean, Long> {
    val start = System.currentTimeMillis()

    // Send multiple packets to trigger ARP
    triggerArpEntry(ip)

    // Wait a bit for ARP to populate
    Thread.sleep(50)

    // Check ARP table
    val mac = getMacFromArp(ip)
    val time = System.currentTimeMillis() - start

    return if (mac.isNotEmpty() && mac != "00:00:00:00:00:00") {
        Pair(true, time)
    } else {
        // Try TCP connect as fallback
        val tcpPorts = listOf(80, 443, 22, 8080)
        for (port in tcpPorts) {
            try {
                java.net.Socket().use { socket ->
                    socket.connect(java.net.InetSocketAddress(ip, port), timeout / 4)
                    return Pair(true, System.currentTimeMillis() - start)
                }
            } catch (_: Exception) {}
        }
        Pair(false, 0)
    }
}

// Fast ping - minimal timeout for batch scanning
private fun fastPing(ip: String): Pair<Boolean, Long> {
    val start = System.currentTimeMillis()

    // Try ICMP first (fastest)
    try {
        if (InetAddress.getByName(ip).isReachable(100)) {
            return Pair(true, System.currentTimeMillis() - start)
        }
    } catch (_: Exception) {}

    // Quick TCP check on common ports
    val quickPorts = listOf(80, 443, 22)
    for (port in quickPorts) {
        try {
            java.net.Socket().use { socket ->
                socket.connect(java.net.InetSocketAddress(ip, port), 50)
                return Pair(true, System.currentTimeMillis() - start)
            }
        } catch (_: Exception) {}
    }

    // Check ARP table
    val mac = getMacFromArp(ip)
    if (mac.isNotEmpty() && mac != "00:00:00:00:00:00") {
        return Pair(true, System.currentTimeMillis() - start)
    }

    return Pair(false, 0)
}

// ICMP Ping - Uses Java's isReachable (ICMP echo request)
private fun icmpPing(ip: String, timeout: Int = 200): Pair<Boolean, Long> {
    return try {
        val start = System.currentTimeMillis()
        val reachable = InetAddress.getByName(ip).isReachable(timeout)
        val time = System.currentTimeMillis() - start
        Pair(reachable, time)
    } catch (_: Exception) { Pair(false, 0) }
}

// ARP Ping - Triggers ARP request by attempting connection, then checks ARP table
private fun arpPing(ip: String, timeout: Int = 200): Pair<Boolean, Long> {
    return try {
        val start = System.currentTimeMillis()
        // Trigger ARP by attempting a TCP connection
        try {
            java.net.Socket().use { socket ->
                socket.connect(java.net.InetSocketAddress(ip, 80), timeout)
            }
        } catch (_: Exception) {
            // Connection may fail but ARP entry might be created
        }
        // Also try UDP to trigger ARP
        try {
            java.net.DatagramSocket().use { socket ->
                socket.soTimeout = timeout
                val data = ByteArray(0)
                socket.send(java.net.DatagramPacket(data, 0, InetAddress.getByName(ip), 7))
            }
        } catch (_: Exception) {}

        val time = System.currentTimeMillis() - start
        // Check if IP appears in ARP table
        val mac = getMacFromArp(ip)
        Pair(mac.isNotEmpty() && mac != "00:00:00:00:00:00", time)
    } catch (_: Exception) { Pair(false, 0) }
}

// UDP Ping - Sends UDP packets to common ports
private fun udpPing(ip: String, timeout: Int = 200): Pair<Boolean, Long> {
    val udpPorts = listOf(7, 53, 67, 68, 123, 137, 138, 161, 5353) // Echo, DNS, DHCP, NTP, NetBIOS, SNMP, mDNS
    return try {
        val start = System.currentTimeMillis()
        var reachable = false

        for (port in udpPorts) {
            try {
                java.net.DatagramSocket().use { socket ->
                    socket.soTimeout = timeout / udpPorts.size
                    val address = InetAddress.getByName(ip)
                    val data = ByteArray(1) { 0 }
                    socket.send(java.net.DatagramPacket(data, data.size, address, port))

                    // Try to receive response (some services respond)
                    try {
                        val buffer = ByteArray(1024)
                        socket.receive(java.net.DatagramPacket(buffer, buffer.size))
                        reachable = true
                    } catch (_: Exception) {
                        // No response, but ARP might have been triggered
                    }
                }
            } catch (_: Exception) {}

            if (reachable) break
        }

        val time = System.currentTimeMillis() - start
        // Check ARP table as fallback
        if (!reachable) {
            val mac = getMacFromArp(ip)
            reachable = mac.isNotEmpty() && mac != "00:00:00:00:00:00"
        }
        Pair(reachable, time)
    } catch (_: Exception) { Pair(false, 0) }
}

// Combined Ping - Uses all methods for maximum detection
private fun combinedPing(ip: String, timeout: Int = 300): Pair<Boolean, Long> {
    val start = System.currentTimeMillis()

    // Try ICMP first (fastest)
    val icmpResult = icmpPing(ip, timeout / 3)
    if (icmpResult.first) {
        return Pair(true, System.currentTimeMillis() - start)
    }

    // Try TCP connect to common ports
    val tcpPorts = listOf(80, 443, 22, 445, 139, 8080, 21, 23)
    for (port in tcpPorts) {
        try {
            java.net.Socket().use { socket ->
                socket.connect(java.net.InetSocketAddress(ip, port), timeout / 8)
                return Pair(true, System.currentTimeMillis() - start)
            }
        } catch (_: Exception) {}
    }

    // Try UDP
    val udpResult = udpPing(ip, timeout / 3)
    if (udpResult.first) {
        return Pair(true, System.currentTimeMillis() - start)
    }

    // Final check: ARP table
    val mac = getMacFromArp(ip)
    val time = System.currentTimeMillis() - start
    return Pair(mac.isNotEmpty() && mac != "00:00:00:00:00:00", time)
}

private fun getMacFromArp(ip: String): String {
    return try {
        val process = Runtime.getRuntime().exec("cat /proc/net/arp")
        val reader = java.io.BufferedReader(java.io.InputStreamReader(process.inputStream))
        reader.readLines().find { it.contains(ip) }?.split("\\s+".toRegex())?.getOrNull(3)?.uppercase() ?: ""
    } catch (_: Exception) { "" }
}

private fun getVendorFromMac(mac: String): String {
    if (mac.length < 8) return ""
    val prefix = mac.take(8).replace(":", "").uppercase()
    return macVendors[prefix] ?: ""
}

// Data class for device identification results
private data class DeviceIdentification(
    val hostname: String,
    val vendor: String,
    val model: String,
    val version: String,
    val type: DeviceType
)

// Ubiquiti Discovery Protocol (UDP port 10001)
private fun getUbiquitiDeviceInfo(ip: String): DeviceIdentification? {
    return try {
        val socket = java.net.DatagramSocket()
        socket.soTimeout = 800

        // UBNT Discovery Protocol v1 request
        val discoveryPacket = byteArrayOf(0x01, 0x00, 0x00, 0x00)
        socket.send(java.net.DatagramPacket(discoveryPacket, discoveryPacket.size, InetAddress.getByName(ip), 10001))

        val buffer = ByteArray(2048)
        val response = java.net.DatagramPacket(buffer, buffer.size)
        socket.receive(response)
        socket.close()

        // Parse UBNT discovery response
        parseUbiquitiResponse(buffer, response.length)
    } catch (_: Exception) { null }
}

// Parse Ubiquiti Discovery Protocol response
private fun parseUbiquitiResponse(data: ByteArray, length: Int): DeviceIdentification? {
    if (length < 4) return null

    var hostname = ""
    var model = ""
    var version = ""
    var type = DeviceType.DEVICE

    try {
        var offset = 4 // Skip header
        while (offset < length - 3) {
            val fieldType = data[offset].toInt() and 0xFF
            val fieldLen = ((data[offset + 1].toInt() and 0xFF) shl 8) or (data[offset + 2].toInt() and 0xFF)
            offset += 3

            if (offset + fieldLen > length) break

            val fieldData = data.copyOfRange(offset, offset + fieldLen)
            val fieldStr = String(fieldData).trim().replace("\u0000", "")

            when (fieldType) {
                0x01 -> {} // MAC address (skip, we already have it)
                0x02 -> {} // MAC + IP info
                0x03 -> version = fieldStr // Firmware version
                0x0B -> hostname = fieldStr // Hostname
                0x0C -> model = fieldStr // Model (short)
                0x14 -> if (model.isEmpty()) model = fieldStr // Model (full)
                0x15 -> {} // ESSID
                0x16 -> {} // WLAN mode
            }
            offset += fieldLen
        }

        // Determine device type from model
        type = when {
            model.contains("UAP", true) || model.contains("U6", true) || model.contains("U7", true) -> DeviceType.ACCESS_POINT
            model.contains("USW", true) || model.contains("US-", true) -> DeviceType.SWITCH
            model.contains("USG", true) || model.contains("UDM", true) || model.contains("UXG", true) -> DeviceType.ROUTER
            model.contains("NanoStation", true) || model.contains("LiteBeam", true) || model.contains("PowerBeam", true) -> DeviceType.ACCESS_POINT
            else -> DeviceType.DEVICE
        }

        // Map model codes to friendly names
        model = mapUbiquitiModel(model)

        if (hostname.isNotEmpty() || model.isNotEmpty()) {
            return DeviceIdentification(hostname, "Ubiquiti", model, version, type)
        }
    } catch (_: Exception) {}

    return null
}

// Map Ubiquiti model codes to friendly names
private fun mapUbiquitiModel(model: String): String {
    return when {
        model.contains("U7-Pro", true) -> "U7-Pro (WiFi 7 AP)"
        model.contains("U6-Pro", true) -> "U6-Pro (WiFi 6 AP)"
        model.contains("U6-LR", true) -> "U6-LR (Long Range AP)"
        model.contains("U6-Lite", true) -> "U6-Lite (WiFi 6 AP)"
        model.contains("U6-Mesh", true) -> "U6-Mesh (Mesh AP)"
        model.contains("UAP-AC-Pro", true) -> "UAP-AC-Pro"
        model.contains("UAP-AC-LR", true) -> "UAP-AC-LR"
        model.contains("UAP-AC-Lite", true) -> "UAP-AC-Lite"
        model.contains("UAP-AC-HD", true) -> "UAP-AC-HD"
        model.contains("UAP-nanoHD", true) -> "UAP-nanoHD"
        model.contains("USW-24", true) -> "USW-24 (24-Port Switch)"
        model.contains("USW-16", true) -> "USW-16 (16-Port Switch)"
        model.contains("USW-8", true) -> "USW-8 (8-Port Switch)"
        model.contains("US-8-60W", true) -> "US-8-60W (8-Port PoE Switch)"
        model.contains("US-8-150W", true) -> "US-8-150W (8-Port PoE Switch)"
        model.contains("US-16-150W", true) -> "US-16-150W (16-Port PoE Switch)"
        model.contains("US-24", true) -> "US-24 (24-Port Switch)"
        model.contains("US-48", true) -> "US-48 (48-Port Switch)"
        model.contains("USG", true) -> "USG (Security Gateway)"
        model.contains("UDM-Pro", true) -> "UDM-Pro (Dream Machine Pro)"
        model.contains("UDM-SE", true) -> "UDM-SE (Dream Machine SE)"
        model.contains("UDM", true) -> "UDM (Dream Machine)"
        model.contains("UXG-Pro", true) -> "UXG-Pro (Next-Gen Gateway)"
        model.contains("NanoStation", true) -> "NanoStation"
        model.contains("LiteBeam", true) -> "LiteBeam"
        model.contains("PowerBeam", true) -> "PowerBeam"
        model.contains("airMAX", true) -> "airMAX"
        model.contains("UA-Hub", true) || model.contains("UA Hub", true) -> "UA Hub (Access Hub)"
        model.isNotEmpty() -> model
        else -> "UniFi Device"
    }
}

// Main function to get device info using multiple methods
// Fast device info - skips slow operations for batch scanning
private fun getDeviceInfoFast(ip: String, mac: String, macVendor: String, isGateway: Boolean): DeviceIdentification {
    var hostname = ""
    var vendor = macVendor
    var model = ""
    var version = ""

    // 1. Try Ubiquiti Discovery (fast, UDP-based)
    val ubntInfo = getUbiquitiDeviceInfo(ip)
    if (ubntInfo != null) {
        return ubntInfo
    }

    // 2. Quick DNS lookup
    hostname = try {
        InetAddress.getByName(ip).canonicalHostName.takeIf { it != ip } ?: ""
    } catch (_: Exception) { "" }

    // 3. Identify device type from hostname and vendor (no port scanning)
    val type = identifyDeviceTypeFast(hostname, vendor, isGateway)

    // 4. Infer model from vendor
    if (model.isEmpty() && vendor.isNotEmpty()) {
        model = inferModelFromVendor(vendor, type)
    }

    return DeviceIdentification(hostname, vendor, model, version, type)
}

// Fast device type identification (no port scanning)
private fun identifyDeviceTypeFast(hostname: String, vendor: String, isGateway: Boolean): DeviceType {
    if (isGateway) return DeviceType.ROUTER

    val hostLower = hostname.lowercase()
    val vendorLower = vendor.lowercase()

    // Hostname patterns
    return when {
        hostLower.contains("iphone") || hostLower.contains("android") || hostLower.contains("galaxy") ||
        hostLower.contains("pixel") || hostLower.contains("phone") -> DeviceType.PHONE

        hostLower.contains("ipad") || hostLower.contains("tablet") -> DeviceType.TABLET

        hostLower.contains("macbook") || hostLower.contains("laptop") -> DeviceType.LAPTOP

        hostLower.startsWith("desktop-") || hostLower.contains("imac") ||
        hostLower.contains("-pc") -> DeviceType.DESKTOP

        hostLower.contains("tv") || hostLower.contains("roku") || hostLower.contains("firetv") ||
        hostLower.contains("chromecast") -> DeviceType.SMART_TV

        hostLower.contains("playstation") || hostLower.contains("xbox") ||
        hostLower.contains("nintendo") -> DeviceType.GAME_CONSOLE

        hostLower.contains("printer") -> DeviceType.PRINTER

        hostLower.contains("camera") || hostLower.contains("ipcam") -> DeviceType.CAMERA

        hostLower.contains("nas") || hostLower.contains("diskstation") -> DeviceType.NAS

        hostLower.contains("echo") || hostLower.contains("homepod") -> DeviceType.SMART_SPEAKER

        // Vendor-based identification
        vendorLower in listOf("samsung", "xiaomi", "huawei", "oppo", "vivo", "oneplus") -> DeviceType.PHONE
        vendorLower in listOf("tp-link", "netgear", "asus", "d-link", "cisco", "mikrotik", "ubiquiti") -> DeviceType.ROUTER
        vendorLower in listOf("hp", "canon", "epson", "brother") -> DeviceType.PRINTER
        vendorLower in listOf("hikvision", "dahua", "reolink") -> DeviceType.CAMERA
        vendorLower in listOf("synology", "qnap") -> DeviceType.NAS
        vendorLower in listOf("espressif", "tuya") -> DeviceType.IOT
        vendorLower.contains("apple") -> when {
            hostLower.contains("iphone") -> DeviceType.PHONE
            hostLower.contains("ipad") -> DeviceType.TABLET
            hostLower.contains("macbook") -> DeviceType.LAPTOP
            hostLower.contains("imac") -> DeviceType.DESKTOP
            else -> DeviceType.DEVICE
        }
        vendorLower in listOf("intel", "dell", "lenovo", "hp", "acer") -> DeviceType.DESKTOP
        vendorLower.contains("raspberry") -> DeviceType.IOT

        else -> DeviceType.DEVICE
    }
}

// Full device info with all detection methods
private fun getDeviceInfo(ip: String, mac: String, macVendor: String, isGateway: Boolean): DeviceIdentification {
    var hostname = ""
    var vendor = macVendor
    var model = ""
    var version = ""
    var type = if (isGateway) DeviceType.ROUTER else DeviceType.DEVICE

    // 1. Try Ubiquiti Discovery first (most reliable for UniFi devices)
    val ubntInfo = getUbiquitiDeviceInfo(ip)
    if (ubntInfo != null) {
        hostname = ubntInfo.hostname
        vendor = "Ubiquiti"
        model = ubntInfo.model
        version = ubntInfo.version
        type = ubntInfo.type
        return DeviceIdentification(hostname, vendor, model, version, type)
    }

    // 2. Try DNS reverse lookup for hostname
    hostname = try {
        InetAddress.getByName(ip).canonicalHostName.takeIf { it != ip } ?: ""
    } catch (_: Exception) { "" }

    // 3. Try NetBIOS name lookup
    if (hostname.isEmpty()) {
        hostname = getNetBiosName(ip)
    }

    // 4. Try HTTP banner grabbing for routers/devices with web interface
    val httpInfo = getHttpBanner(ip)
    if (httpInfo.isNotEmpty()) {
        val parsed = parseHttpBanner(httpInfo, macVendor)
        if (parsed.model.isNotEmpty()) model = parsed.model
        if (parsed.version.isNotEmpty()) version = parsed.version
        if (parsed.vendor.isNotEmpty()) vendor = parsed.vendor
        if (parsed.hostname.isNotEmpty() && hostname.isEmpty()) hostname = parsed.hostname
    }

    // 5. Try mDNS/Bonjour discovery
    val mdnsInfo = getMdnsInfo(ip)
    if (mdnsInfo.isNotEmpty()) {
        if (hostname.isEmpty()) hostname = mdnsInfo
    }

    // 6. Identify device type from multiple sources
    type = identifyDeviceType(hostname, vendor, mac, ip, isGateway)

    // 7. Infer model from MAC vendor if not found
    if (model.isEmpty() && vendor.isNotEmpty()) {
        model = inferModelFromVendor(vendor, type)
    }

    return DeviceIdentification(hostname, vendor, model, version, type)
}

// Comprehensive device type identification
private fun identifyDeviceType(hostname: String, vendor: String, mac: String, ip: String, isGateway: Boolean): DeviceType {
    val hostLower = hostname.lowercase()
    val vendorLower = vendor.lowercase()

    // 1. Check if it's a gateway/router
    if (isGateway) return DeviceType.ROUTER

    // 2. Identify by hostname patterns
    when {
        // Phones
        hostLower.contains("iphone") || hostLower.contains("android") ||
        hostLower.contains("galaxy") || hostLower.contains("pixel") ||
        hostLower.contains("oneplus") || hostLower.contains("huawei-") ||
        hostLower.contains("xiaomi") || hostLower.contains("redmi") ||
        hostLower.contains("oppo") || hostLower.contains("vivo") ||
        hostLower.contains("phone") -> return DeviceType.PHONE

        // Tablets
        hostLower.contains("ipad") || hostLower.contains("tablet") ||
        hostLower.contains("galaxy-tab") || hostLower.contains("surface") -> return DeviceType.TABLET

        // Laptops/Desktops
        hostLower.contains("macbook") || hostLower.contains("laptop") -> return DeviceType.LAPTOP
        hostLower.contains("imac") || hostLower.contains("desktop") ||
        hostLower.contains("pc-") || hostLower.startsWith("desktop-") ||
        hostLower.matches(Regex(".*-pc$")) -> return DeviceType.DESKTOP

        // Smart TVs
        hostLower.contains("tv") || hostLower.contains("bravia") ||
        hostLower.contains("roku") || hostLower.contains("firetv") ||
        hostLower.contains("chromecast") || hostLower.contains("appletv") ||
        hostLower.contains("smarttv") || hostLower.contains("android-tv") -> return DeviceType.SMART_TV

        // Game Consoles
        hostLower.contains("playstation") || hostLower.contains("ps4") || hostLower.contains("ps5") ||
        hostLower.contains("xbox") || hostLower.contains("nintendo") ||
        hostLower.contains("switch") -> return DeviceType.GAME_CONSOLE

        // Printers
        hostLower.contains("printer") || hostLower.contains("epson") ||
        hostLower.contains("canon") || hostLower.contains("hp-") ||
        hostLower.contains("brother") || hostLower.contains("xerox") -> return DeviceType.PRINTER

        // Cameras
        hostLower.contains("camera") || hostLower.contains("ipcam") ||
        hostLower.contains("cam-") || hostLower.contains("hikvision") ||
        hostLower.contains("dahua") || hostLower.contains("reolink") -> return DeviceType.CAMERA

        // NAS
        hostLower.contains("nas") || hostLower.contains("synology") ||
        hostLower.contains("qnap") || hostLower.contains("diskstation") -> return DeviceType.NAS

        // Smart Speakers
        hostLower.contains("echo") || hostLower.contains("alexa") ||
        hostLower.contains("google-home") || hostLower.contains("homepod") ||
        hostLower.contains("nest-") -> return DeviceType.SMART_SPEAKER

        // Media Players
        hostLower.contains("sonos") || hostLower.contains("plex") ||
        hostLower.contains("kodi") || hostLower.contains("shield") -> return DeviceType.MEDIA_PLAYER

        // Wearables
        hostLower.contains("watch") || hostLower.contains("band") ||
        hostLower.contains("fitbit") || hostLower.contains("garmin") -> return DeviceType.WEARABLE
    }

    // 3. Identify by vendor
    when {
        // Phone manufacturers
        vendorLower in listOf("apple") && hostLower.contains("iphone") -> return DeviceType.PHONE
        vendorLower in listOf("samsung", "xiaomi", "huawei", "oppo", "vivo", "oneplus", "realme", "motorola", "nokia", "lg electronics", "zte", "meizu") -> {
            // Could be phone or tablet, default to phone
            return DeviceType.PHONE
        }

        // Network equipment vendors
        vendorLower in listOf("cisco", "juniper", "arista", "fortinet", "palo alto", "sonicwall") -> return DeviceType.ROUTER
        vendorLower in listOf("tp-link", "netgear", "asus", "d-link", "linksys", "mikrotik", "ubiquiti") -> {
            // Check ports to distinguish router vs AP
            val portType = identifyDeviceFromPorts(ip)
            return if (portType != DeviceType.UNKNOWN) portType else DeviceType.ROUTER
        }

        // Printer vendors
        vendorLower in listOf("hewlett packard", "hp", "canon", "epson", "brother", "xerox", "lexmark", "ricoh", "kyocera") -> return DeviceType.PRINTER

        // Smart TV vendors
        vendorLower in listOf("lg electronics", "sony", "tcl", "hisense", "vizio", "roku") -> return DeviceType.SMART_TV

        // Game console vendors
        vendorLower.contains("sony") && hostLower.contains("playstation") -> return DeviceType.GAME_CONSOLE
        vendorLower.contains("microsoft") && hostLower.contains("xbox") -> return DeviceType.GAME_CONSOLE
        vendorLower.contains("nintendo") -> return DeviceType.GAME_CONSOLE

        // IoT vendors
        vendorLower in listOf("espressif", "tuya", "shenzhen", "sonoff", "tasmota", "wemo", "philips hue", "lifx", "yeelight", "smartthings", "nest", "ring", "arlo", "wyze") -> return DeviceType.IOT

        // Camera vendors
        vendorLower in listOf("hikvision", "dahua", "axis", "reolink", "amcrest", "foscam", "lorex") -> return DeviceType.CAMERA

        // NAS vendors
        vendorLower in listOf("synology", "qnap", "western digital", "seagate", "buffalo", "asustor", "terramaster") -> return DeviceType.NAS

        // Smart speaker vendors
        vendorLower.contains("amazon") && (hostLower.contains("echo") || hostLower.contains("alexa")) -> return DeviceType.SMART_SPEAKER
        vendorLower.contains("google") && hostLower.contains("home") -> return DeviceType.SMART_SPEAKER
        vendorLower.contains("sonos") -> return DeviceType.MEDIA_PLAYER

        // Apple devices
        vendorLower.contains("apple") -> {
            return when {
                hostLower.contains("iphone") -> DeviceType.PHONE
                hostLower.contains("ipad") -> DeviceType.TABLET
                hostLower.contains("macbook") -> DeviceType.LAPTOP
                hostLower.contains("imac") || hostLower.contains("mac-") -> DeviceType.DESKTOP
                hostLower.contains("appletv") || hostLower.contains("apple-tv") -> DeviceType.SMART_TV
                hostLower.contains("homepod") -> DeviceType.SMART_SPEAKER
                hostLower.contains("watch") -> DeviceType.WEARABLE
                else -> DeviceType.DEVICE
            }
        }

        // Intel/PC manufacturers - likely laptop or desktop
        vendorLower in listOf("intel", "dell", "lenovo", "hp", "acer", "asus", "msi", "gigabyte") -> {
            return if (hostLower.contains("laptop") || hostLower.contains("notebook")) DeviceType.LAPTOP else DeviceType.DESKTOP
        }

        // Raspberry Pi
        vendorLower.contains("raspberry") -> return DeviceType.IOT
    }

    // 4. Check open ports for service-based identification
    val portType = identifyDeviceFromPorts(ip)
    if (portType != DeviceType.UNKNOWN) return portType

    return DeviceType.DEVICE
}

// NetBIOS name lookup
private fun getNetBiosName(ip: String): String {
    return try {
        val socket = java.net.DatagramSocket()
        socket.soTimeout = 500

        // NetBIOS Name Query packet
        val query = byteArrayOf(
            0x80.toByte(), 0x94.toByte(), 0x00, 0x00, 0x00, 0x01, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x20, 0x43, 0x4B, 0x41,
            0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41,
            0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41,
            0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41, 0x41,
            0x41, 0x41, 0x41, 0x41, 0x41, 0x00, 0x00, 0x21,
            0x00, 0x01
        )

        val packet = java.net.DatagramPacket(query, query.size, InetAddress.getByName(ip), 137)
        socket.send(packet)

        val response = ByteArray(1024)
        val responsePacket = java.net.DatagramPacket(response, response.size)
        socket.receive(responsePacket)
        socket.close()

        // Parse NetBIOS response - name starts at offset 57
        if (responsePacket.length > 57) {
            val nameBytes = response.copyOfRange(57, 72)
            String(nameBytes).trim().replace("\u0000", "").trim()
        } else ""
    } catch (_: Exception) { "" }
}

// HTTP banner grabbing
private fun getHttpBanner(ip: String): String {
    val ports = listOf(80, 8080, 443, 8443, 8000, 8888)
    for (port in ports) {
        try {
            val socket = java.net.Socket()
            socket.connect(java.net.InetSocketAddress(ip, port), 500)
            socket.soTimeout = 1000

            val output = socket.getOutputStream()
            val input = socket.getInputStream()

            // Send HTTP request
            val request = "GET / HTTP/1.1\r\nHost: $ip\r\nUser-Agent: CVToolkit/1.0\r\nConnection: close\r\n\r\n"
            output.write(request.toByteArray())
            output.flush()

            // Read response headers
            val reader = java.io.BufferedReader(java.io.InputStreamReader(input))
            val response = StringBuilder()
            var line: String?
            var lineCount = 0
            while (reader.readLine().also { line = it } != null && lineCount < 30) {
                response.append(line).append("\n")
                lineCount++
                if (line.isNullOrEmpty()) break
            }

            socket.close()
            if (response.isNotEmpty()) return response.toString()
        } catch (_: Exception) {}
    }
    return ""
}

// Parse HTTP banner for device info
private fun parseHttpBanner(banner: String, macVendor: String): DeviceIdentification {
    var hostname = ""
    var vendor = ""
    var model = ""
    var version = ""

    val bannerLower = banner.lowercase()

    // Extract Server header
    val serverMatch = Regex("server:\\s*([^\\r\\n]+)", RegexOption.IGNORE_CASE).find(banner)
    val serverHeader = serverMatch?.groupValues?.get(1) ?: ""

    // Extract WWW-Authenticate for realm (often contains device name)
    val realmMatch = Regex("realm=\"([^\"]+)\"", RegexOption.IGNORE_CASE).find(banner)
    val realm = realmMatch?.groupValues?.get(1) ?: ""

    // Identify router brands and models
    when {
        bannerLower.contains("tp-link") || bannerLower.contains("tplink") -> {
            vendor = "TP-Link"
            model = extractModel(banner, listOf("TL-", "Archer", "Deco", "RE", "WR", "WA"))
        }
        bannerLower.contains("asus") -> {
            vendor = "ASUS"
            model = extractModel(banner, listOf("RT-", "ROG", "ZenWiFi", "TUF"))
        }
        bannerLower.contains("netgear") -> {
            vendor = "Netgear"
            model = extractModel(banner, listOf("R", "RAX", "RBK", "WAX", "XR"))
        }
        bannerLower.contains("linksys") -> {
            vendor = "Linksys"
            model = extractModel(banner, listOf("EA", "WRT", "MR", "MX"))
        }
        bannerLower.contains("d-link") || bannerLower.contains("dlink") -> {
            vendor = "D-Link"
            model = extractModel(banner, listOf("DIR-", "DAP-", "DWR-", "DSL-"))
        }
        bannerLower.contains("cisco") -> {
            vendor = "Cisco"
            model = extractModel(banner, listOf("RV", "WAP", "SG", "SF"))
        }
        bannerLower.contains("huawei") -> {
            vendor = "Huawei"
            model = extractModel(banner, listOf("HG", "WS", "AX", "WiFi"))
        }
        bannerLower.contains("mikrotik") -> {
            vendor = "MikroTik"
            model = extractModel(banner, listOf("RB", "hAP", "hEX", "CCR", "CRS"))
        }
        bannerLower.contains("ubiquiti") || bannerLower.contains("unifi") -> {
            vendor = "Ubiquiti"
            model = extractModel(banner, listOf("UAP", "USG", "USW", "UDM", "U6"))
        }
        bannerLower.contains("synology") -> {
            vendor = "Synology"
            model = extractModel(banner, listOf("DS", "RS", "RT"))
        }
        bannerLower.contains("qnap") -> {
            vendor = "QNAP"
            model = extractModel(banner, listOf("TS-", "TVS-", "TBS-"))
        }
        bannerLower.contains("xiaomi") || bannerLower.contains("miwifi") -> {
            vendor = "Xiaomi"
            model = extractModel(banner, listOf("AX", "AC", "R", "Mi Router"))
        }
        realm.isNotEmpty() -> {
            hostname = realm
        }
    }

    // Extract version from Server header or body
    val versionMatch = Regex("(\\d+\\.\\d+\\.?\\d*\\.?\\d*)").find(serverHeader)
    version = versionMatch?.value ?: ""

    // Use MAC vendor if HTTP didn't identify
    if (vendor.isEmpty()) vendor = macVendor

    return DeviceIdentification(hostname, vendor, model, version, DeviceType.UNKNOWN)
}

// Extract model number from banner
private fun extractModel(banner: String, prefixes: List<String>): String {
    for (prefix in prefixes) {
        val regex = Regex("($prefix[A-Z0-9-]+)", RegexOption.IGNORE_CASE)
        val match = regex.find(banner)
        if (match != null) return match.value.uppercase()
    }
    return ""
}

// Identify device type from open ports
private fun identifyDeviceFromPorts(ip: String): DeviceType {
    val openPorts = mutableListOf<Int>()
    // Extended port list for better device identification
    val portsToCheck = listOf(
        22,    // SSH
        23,    // Telnet
        53,    // DNS
        80,    // HTTP
        443,   // HTTPS
        161,   // SNMP
        515,   // Printer (LPD)
        631,   // Printer (IPP/CUPS)
        5000,  // Synology DSM / UPnP
        5001,  // Synology DSM SSL
        8080,  // HTTP Alt
        8443,  // HTTPS Alt
        8291,  // MikroTik Winbox
        8728,  // MikroTik API
        9100,  // Printer (RAW)
        32400, // Plex
        8096,  // Jellyfin
        62078, // iPhone sync
        5353,  // mDNS
        548,   // AFP (Mac file sharing)
        445,   // SMB
        3689,  // iTunes/DAAP
        7000,  // AirPlay
        554,   // RTSP (cameras)
        1883,  // MQTT (IoT)
        8883   // MQTT SSL (IoT)
    )

    for (port in portsToCheck) {
        try {
            java.net.Socket().use { socket ->
                socket.connect(java.net.InetSocketAddress(ip, port), 50)
                openPorts.add(port)
            }
        } catch (_: Exception) {}
    }

    return when {
        // Printers - specific printer ports
        openPorts.contains(9100) || openPorts.contains(515) || openPorts.contains(631) -> DeviceType.PRINTER

        // NAS - Synology/QNAP specific ports
        openPorts.contains(5000) || openPorts.contains(5001) -> DeviceType.NAS

        // Media Players - Plex/Jellyfin
        openPorts.contains(32400) || openPorts.contains(8096) -> DeviceType.MEDIA_PLAYER

        // iPhone/iOS device
        openPorts.contains(62078) -> DeviceType.PHONE

        // IP Cameras - RTSP
        openPorts.contains(554) && !openPorts.contains(53) -> DeviceType.CAMERA

        // IoT devices - MQTT
        openPorts.contains(1883) || openPorts.contains(8883) -> DeviceType.IOT

        // Apple devices with AirPlay
        openPorts.contains(7000) && openPorts.contains(3689) -> DeviceType.SMART_TV

        // MikroTik specific ports
        openPorts.contains(8291) || openPorts.contains(8728) -> DeviceType.ROUTER

        // DNS server typically on routers
        openPorts.contains(53) && (openPorts.contains(80) || openPorts.contains(443)) -> DeviceType.ROUTER

        // SNMP + Web interface = network device (switch)
        openPorts.contains(161) && openPorts.contains(80) -> DeviceType.SWITCH

        // SMB/AFP file sharing - likely desktop/laptop
        openPorts.contains(445) || openPorts.contains(548) -> DeviceType.DESKTOP

        // SSH only - likely Linux server/desktop
        openPorts.contains(22) && openPorts.size == 1 -> DeviceType.DESKTOP

        else -> DeviceType.UNKNOWN
    }
}

// mDNS/Bonjour discovery
private fun getMdnsInfo(ip: String): String {
    return try {
        val socket = java.net.DatagramSocket()
        socket.soTimeout = 500

        // Simple mDNS query for _services._dns-sd._udp.local
        val query = byteArrayOf(
            0x00, 0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00,
            0x00, 0x00, 0x00, 0x00, 0x09, 0x5F, 0x73, 0x65,
            0x72, 0x76, 0x69, 0x63, 0x65, 0x73, 0x07, 0x5F,
            0x64, 0x6E, 0x73, 0x2D, 0x73, 0x64, 0x04, 0x5F,
            0x75, 0x64, 0x70, 0x05, 0x6C, 0x6F, 0x63, 0x61,
            0x6C, 0x00, 0x00, 0x0C, 0x00, 0x01
        )

        val packet = java.net.DatagramPacket(query, query.size, InetAddress.getByName(ip), 5353)
        socket.send(packet)

        val response = ByteArray(1024)
        val responsePacket = java.net.DatagramPacket(response, response.size)
        socket.receive(responsePacket)
        socket.close()

        // Parse mDNS response for device name
        if (responsePacket.length > 12) {
            String(response, 0, responsePacket.length)
                .replace("[^\\x20-\\x7E]".toRegex(), " ")
                .split(" ")
                .filter { it.length > 3 && !it.contains("local") && !it.contains("_") }
                .firstOrNull() ?: ""
        } else ""
    } catch (_: Exception) { "" }
}

// Infer model from vendor for common devices
private fun inferModelFromVendor(vendor: String, type: DeviceType): String {
    return when {
        type == DeviceType.ROUTER -> when (vendor) {
            "TP-Link" -> "Wireless Router"
            "ASUS" -> "Wireless Router"
            "Netgear" -> "Wireless Router"
            "D-Link" -> "Wireless Router"
            "Cisco" -> "Network Router"
            "Huawei" -> "Wireless Router"
            "MikroTik" -> "RouterBoard"
            "Ubiquiti" -> "UniFi Gateway"
            else -> "Router"
        }
        type == DeviceType.SWITCH -> "Network Switch"
        type == DeviceType.ACCESS_POINT -> "Access Point"
        else -> ""
    }
}

private val macVendors = mapOf(
    "00155D" to "Microsoft",
    "000C29" to "VMware",
    "001C42" to "Parallels",
    "0050568" to "VMware",
    "080027" to "VirtualBox",
    "00163E" to "Xensource",
    "001A11" to "Google",
    "3C5AB4" to "Google",
    "94EB2C" to "Google",
    "F4F5D8" to "Google",
    "A47733" to "Google",
    "DC56E7" to "Apple",
    "F0D4F7" to "Apple",
    "A860B6" to "Apple",
    "3C0754" to "Apple",
    "F8FF0B" to "Apple",
    "A4B197" to "Apple",
    "98D6BB" to "Apple",
    "F0B479" to "Apple",
    "A4D1D2" to "Apple",
    "002312" to "Apple",
    "B8E856" to "Apple",
    "60F81D" to "Apple",
    "BC6778" to "Apple",
    "88E9FE" to "Apple",
    "B8C111" to "Apple",
    "E0B9BA" to "Apple",
    "F0DCE2" to "Apple",
    "18AF8F" to "Apple",
    "B0BE76" to "Apple",
    "00E04C" to "Realtek",
    "52540" to "Realtek",
    "001E4F" to "Dell",
    "D4BE" to "Dell",
    "F8BC12" to "Dell",
    "001DD8" to "Microsoft",
    "0003FF" to "Microsoft",
    "7CED8D" to "Microsoft",
    "28C63F" to "Intel",
    "3C970E" to "Intel",
    "A4C494" to "Intel",
    "48A472" to "Intel",
    "8C8CAA" to "Intel",
    "B4969" to "Intel",
    "E8B1FC" to "Intel",
    "001B21" to "Intel",
    "001E67" to "Intel",
    "0024D6" to "Intel",
    "18DB" to "Intel",
    "5CE0C5" to "Intel",
    "606720" to "Intel",
    "6C8814" to "Intel",
    "88B111" to "Intel",
    "B4B52F" to "Intel",
    "C8D3FF" to "Intel",
    "E4A7A0" to "Intel",
    "F8F21E" to "Intel",
    "001E65" to "Intel",
    "001F3B" to "Intel",
    "002314" to "Intel",
    "00248C" to "Intel",
    "0026C6" to "Intel",
    "0026C7" to "Intel",
    "002710" to "Intel",
    "5C5181" to "Huawei",
    "00E0FC" to "Huawei",
    "48435A" to "Huawei",
    "707BE8" to "Huawei",
    "7C1E52" to "Huawei",
    "84A9C4" to "Huawei",
    "88CEFA" to "Huawei",
    "C8D15E" to "Huawei",
    "E0247F" to "Huawei",
    "F4C714" to "Huawei",
    "F83DFF" to "Huawei",
    "FC48EF" to "Huawei",
    "001E10" to "Samsung",
    "002339" to "Samsung",
    "0024E9" to "Samsung",
    "0026E2" to "Samsung",
    "1C62B8" to "Samsung",
    "5056BF" to "Samsung",
    "5C0A5B" to "Samsung",
    "6C2F2C" to "Samsung",
    "78D6F0" to "Samsung",
    "84119E" to "Samsung",
    "8C71F8" to "Samsung",
    "9463D1" to "Samsung",
    "9C65B0" to "Samsung",
    "A0B4A5" to "Samsung",
    "B47443" to "Samsung",
    "C44619" to "Samsung",
    "D0176A" to "Samsung",
    "E4E0C5" to "Samsung",
    "F025B7" to "Samsung",
    "F8042E" to "Samsung",
    "001A2B" to "Xiaomi",
    "0C1DAF" to "Xiaomi",
    "10D07A" to "Xiaomi",
    "14F65A" to "Xiaomi",
    "286C07" to "Xiaomi",
    "34CE00" to "Xiaomi",
    "3C9157" to "Xiaomi",
    "50EC50" to "Xiaomi",
    "58448E" to "Xiaomi",
    "640980" to "Xiaomi",
    "7451BA" to "Xiaomi",
    "7C1DD9" to "Xiaomi",
    "842E27" to "Xiaomi",
    "8CBEBE" to "Xiaomi",
    "9C99A0" to "Xiaomi",
    "A086C6" to "Xiaomi",
    "B0E235" to "Xiaomi",
    "C40BCB" to "Xiaomi",
    "D4970B" to "Xiaomi",
    "E8FA23" to "Xiaomi",
    "F48B32" to "Xiaomi",
    "F8A45F" to "Xiaomi",
    "001DD9" to "TP-Link",
    "14CC20" to "TP-Link",
    "1C3BF3" to "TP-Link",
    "30B5C2" to "TP-Link",
    "503EAA" to "TP-Link",
    "54E6FC" to "TP-Link",
    "6466B3" to "TP-Link",
    "788CB5" to "TP-Link",
    "9C216A" to "TP-Link",
    "B0BE76" to "TP-Link",
    "C025E9" to "TP-Link",
    "D46E0E" to "TP-Link",
    "E894F6" to "TP-Link",
    "F4F26D" to "TP-Link",
    "001E58" to "D-Link",
    "0015E9" to "D-Link",
    "00179A" to "D-Link",
    "001CF0" to "D-Link",
    "0022B0" to "D-Link",
    "00265A" to "D-Link",
    "1CAFF7" to "D-Link",
    "1CBDB9" to "D-Link",
    "28107B" to "D-Link",
    "340804" to "D-Link",
    "78542E" to "D-Link",
    "9094E4" to "D-Link",
    "ACF1DF" to "D-Link",
    "B8A386" to "D-Link",
    "C8BE19" to "D-Link",
    "CCB255" to "D-Link",
    "F07D68" to "D-Link",
    "FC7516" to "D-Link",
    "001802" to "Netgear",
    "00184D" to "Netgear",
    "001E2A" to "Netgear",
    "001F33" to "Netgear",
    "00223F" to "Netgear",
    "00224D" to "Netgear",
    "002636" to "Netgear",
    "00265B" to "Netgear",
    "008EF2" to "Netgear",
    "204E7F" to "Netgear",
    "2CB05D" to "Netgear",
    "4494FC" to "Netgear",
    "6CB0CE" to "Netgear",
    "84C9B2" to "Netgear",
    "9CD36D" to "Netgear",
    "A00460" to "Netgear",
    "A42B8C" to "Netgear",
    "B03956" to "Netgear",
    "C03F0E" to "Netgear",
    "C43DC7" to "Netgear",
    "CC40D0" to "Netgear",
    "E0469A" to "Netgear",
    "E091F5" to "Netgear",
    "E4F4C6" to "Netgear",
    "001AA0" to "ASUS",
    "001FC6" to "ASUS",
    "002354" to "ASUS",
    "00248D" to "ASUS",
    "049226" to "ASUS",
    "08606E" to "ASUS",
    "107B44" to "ASUS",
    "14DDA9" to "ASUS",
    "1C872C" to "ASUS",
    "2C4D54" to "ASUS",
    "2CFDA1" to "ASUS",
    "305A3A" to "ASUS",
    "3085A9" to "ASUS",
    "382C4A" to "ASUS",
    "485B39" to "ASUS",
    "50465D" to "ASUS",
    "54A050" to "ASUS",
    "6045CB" to "ASUS",
    "708BCD" to "ASUS",
    "74D02B" to "ASUS",
    "AC220B" to "ASUS",
    "BCEE7B" to "ASUS",
    "C86000" to "ASUS",
    "D850E6" to "ASUS",
    "E03F49" to "ASUS",
    "F46D04" to "ASUS",
    "F832E4" to "ASUS",
    "001217" to "Cisco",
    "0012D9" to "Cisco",
    "001320" to "Cisco",
    "001795" to "Cisco",
    "001A2F" to "Cisco",
    "001A6C" to "Cisco",
    "001B0C" to "Cisco",
    "001B2A" to "Cisco",
    "001B53" to "Cisco",
    "001B54" to "Cisco",
    "001B67" to "Cisco",
    "001BD4" to "Cisco",
    "001BD5" to "Cisco",
    "001BD7" to "Cisco",
    "001C0E" to "Cisco",
    "001C0F" to "Cisco",
    "001C10" to "Cisco",
    "001C57" to "Cisco",
    "001C58" to "Cisco",
    "001D45" to "Cisco",
    "001D46" to "Cisco",
    "001D70" to "Cisco",
    "001D71" to "Cisco",
    "001DE5" to "Cisco",
    "001DE6" to "Cisco",
    "001E13" to "Cisco",
    "001E14" to "Cisco",
    "001E49" to "Cisco",
    "001E4A" to "Cisco",
    "001E79" to "Cisco",
    "001E7A" to "Cisco",
    "001EB6" to "Cisco",
    "001EB7" to "Cisco",
    "001EBD" to "Cisco",
    "001EBE" to "Cisco",
    "001EF6" to "Cisco",
    "001EF7" to "Cisco",
    "001F26" to "Cisco",
    "001F27" to "Cisco",
    "001F6C" to "Cisco",
    "001F6D" to "Cisco",
    "001F9D" to "Cisco",
    "001F9E" to "Cisco",
    "001FC9" to "Cisco",
    "001FCA" to "Cisco",
    "002155" to "Cisco",
    "002156" to "Cisco",
    "0021A0" to "Cisco",
    "0021A1" to "Cisco",
    "0021BE" to "Cisco",
    "0021BF" to "Cisco",
    "0021D7" to "Cisco",
    "0021D8" to "Cisco",
    "002216" to "Cisco",
    "00223A" to "Cisco",
    "00226B" to "Cisco",
    "002290" to "Cisco",
    "0022BD" to "Cisco",
    "0022CE" to "Cisco",
    "002351" to "Cisco",
    "00235D" to "Cisco",
    "002398" to "Cisco",
    "0023AB" to "Cisco",
    "0023AC" to "Cisco",
    "0023BE" to "Cisco",
    "0023EA" to "Cisco",
    "0023EB" to "Cisco",
    "002433" to "Cisco",
    "002434" to "Cisco",
    "00244A" to "Cisco",
    "002450" to "Cisco",
    "002451" to "Cisco",
    "00248A" to "Cisco",
    "0024C3" to "Cisco",
    "0024C4" to "Cisco",
    "0024F7" to "Cisco",
    "0024F9" to "Cisco",
    "002511" to "Cisco",
    "002512" to "Cisco",
    "002545" to "Cisco",
    "002546" to "Cisco",
    "00259A" to "Cisco",
    "00259B" to "Cisco",
    "0025B4" to "Cisco",
    "0025B5" to "Cisco",
    "002643" to "Cisco",
    "002644" to "Cisco",
    "00268B" to "Cisco",
    "00270D" to "Cisco",
    "002710" to "Cisco",
    "00271A" to "Cisco",
    "00271B" to "Cisco",
    "00272F" to "Cisco",
    "002730" to "Cisco",
    "0050E2" to "Cisco",
    "0050F0" to "Cisco",
    "005080" to "Cisco",
    "00503E" to "Cisco",
    "005054" to "Cisco",
    "0050A2" to "Cisco",
    "0050BD" to "Cisco",
    "0060B9" to "Cisco",
    "006009" to "Cisco",
    "006047" to "Cisco",
    "006070" to "Cisco",
    "006083" to "Cisco",
    "0090A6" to "Cisco",
    "0090BF" to "Cisco",
    "0090F2" to "Cisco",
    "00D006" to "Cisco",
    "00D058" to "Cisco",
    "00D079" to "Cisco",
    "00D0BA" to "Cisco",
    "00D0BB" to "Cisco",
    "00D0BC" to "Cisco",
    "00D0C0" to "Cisco",
    "00D0D3" to "Cisco",
    "00D0E4" to "Cisco",
    "00D0FF" to "Cisco",
    "00E014" to "Cisco",
    "00E016" to "Cisco",
    "00E01E" to "Cisco",
    "00E034" to "Cisco",
    "00E04F" to "Cisco",
    "00E08F" to "Cisco",
    "00E0A3" to "Cisco",
    "00E0B0" to "Cisco",
    "00E0F7" to "Cisco",
    "00E0F9" to "Cisco",
    "00E0FE" to "Cisco",
    "0C8525" to "Cisco",
    "0C8DDB" to "Cisco",
    "0C8DDB" to "Cisco",
    "0CD996" to "Cisco",
    "0CDFA4" to "Cisco",
    "0CE0E4" to "Cisco",
    "100000" to "Private",
    "2C3033" to "Netgear",
    "B827EB" to "Raspberry Pi",
    "DCA632" to "Raspberry Pi",
    "E45F01" to "Raspberry Pi"
)
