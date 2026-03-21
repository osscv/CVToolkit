package cv.toolkit.screens

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import cv.toolkit.R
import cv.toolkit.ads.BannerAd

@Suppress("DEPRECATION")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WifiAnalyzerScreen(navController: NavController) {
    val context = LocalContext.current
    val wifiManager = remember {
        context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
    }

    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var scanResults by remember { mutableStateOf<List<ScanResult>>(emptyList()) }
    var isScanning by remember { mutableStateOf(false) }
    var connectedSsid by remember { mutableStateOf<String?>(null) }
    var connectedBssid by remember { mutableStateOf<String?>(null) }
    var connectedRssi by remember { mutableStateOf<Int?>(null) }
    var connectedFrequency by remember { mutableStateOf<Int?>(null) }
    var connectedLinkSpeed by remember { mutableStateOf<Int?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPermission = isGranted
        if (isGranted) {
            isScanning = true
            wifiManager.startScan()
        }
    }

    // Update connected network info
    fun updateConnectedInfo() {
        val connectionInfo = wifiManager.connectionInfo
        if (connectionInfo != null && connectionInfo.networkId != -1) {
            connectedSsid = connectionInfo.ssid?.removeSurrounding("\"")
                ?.takeIf { it != "<unknown ssid>" }
            connectedBssid = connectionInfo.bssid
            connectedRssi = connectionInfo.rssi
            connectedFrequency = connectionInfo.frequency
            connectedLinkSpeed = connectionInfo.linkSpeed
        } else {
            connectedSsid = null
            connectedBssid = null
            connectedRssi = null
            connectedFrequency = null
            connectedLinkSpeed = null
        }
    }

    // Register scan results receiver
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (hasPermission) {
                    try {
                        scanResults = wifiManager.scanResults
                            .sortedByDescending { it.level }
                    } catch (_: SecurityException) {
                        // Permission might have been revoked
                    }
                }
                isScanning = false
            }
        }
        context.registerReceiver(
            receiver,
            IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)
        )
        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    // Initial scan
    LaunchedEffect(hasPermission) {
        if (hasPermission) {
            updateConnectedInfo()
            isScanning = true
            wifiManager.startScan()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.wifi_analyzer_title)) },
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
            if (!hasPermission) {
                // Permission denied state
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Filled.LocationOff,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "Location Permission Required",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "WiFi scanning requires location permission to discover nearby networks. Please grant the permission to use this feature.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = {
                                permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                            }
                        ) {
                            Icon(Icons.Filled.LocationOn, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Grant Permission")
                        }
                    }
                }

                Spacer(Modifier.weight(1f))
                BannerAd(modifier = Modifier.fillMaxWidth())
            } else {
                // Scan button
                Button(
                    onClick = {
                        isScanning = true
                        updateConnectedInfo()
                        wifiManager.startScan()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isScanning
                ) {
                    if (isScanning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Scanning...")
                    } else {
                        Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Scan")
                    }
                }

                // Connected network info
                if (connectedSsid != null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.Wifi,
                                    null,
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Connected Network",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            ConnectedInfoRow("SSID", connectedSsid ?: "Unknown")
                            connectedBssid?.let { ConnectedInfoRow("BSSID", it) }
                            connectedRssi?.let {
                                ConnectedInfoRow("Signal", "$it dBm (${signalLevelLabel(getSignalLevel(it))})")
                            }
                            connectedFrequency?.let {
                                ConnectedInfoRow("Frequency", "${it} MHz (${getFrequencyBand(it)})")
                                ConnectedInfoRow("Channel", "${getChannelFromFrequency(it)}")
                            }
                            connectedLinkSpeed?.let {
                                ConnectedInfoRow("Link Speed", "$it Mbps")
                            }
                        }
                    }
                }

                // Network count
                if (scanResults.isNotEmpty()) {
                    Text(
                        "${scanResults.size} network${if (scanResults.size != 1) "s" else ""} found",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Scan results list
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (scanResults.isEmpty() && !isScanning) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        Icons.Filled.WifiFind,
                                        null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                    Spacer(Modifier.height(16.dp))
                                    Text(
                                        "No networks found",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        "Tap Scan to search for nearby WiFi networks",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }

                    items(scanResults, key = { it.BSSID }) { result ->
                        WifiNetworkCard(result, connectedBssid)
                    }

                    item {
                        BannerAd(modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

@Suppress("DEPRECATION")
@Composable
private fun WifiNetworkCard(scanResult: ScanResult, connectedBssid: String?) {
    val isConnected = scanResult.BSSID == connectedBssid
    val signalLevel = getSignalLevel(scanResult.level)
    val signalFraction = getSignalFraction(scanResult.level)
    val securityType = getSecurityType(scanResult.capabilities)
    val channel = getChannelFromFrequency(scanResult.frequency)
    val band = getFrequencyBand(scanResult.frequency)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isConnected)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // SSID and connected badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        when {
                            signalLevel >= 3 -> Icons.Filled.Wifi
                            signalLevel >= 2 -> Icons.Filled.Wifi
                            signalLevel >= 1 -> Icons.Filled.Wifi
                            else -> Icons.Filled.Wifi
                        },
                        null,
                        modifier = Modifier.size(20.dp),
                        tint = when {
                            signalLevel >= 3 -> MaterialTheme.colorScheme.primary
                            signalLevel >= 2 -> MaterialTheme.colorScheme.tertiary
                            else -> MaterialTheme.colorScheme.error
                        }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = scanResult.SSID.ifEmpty { "(Hidden Network)" },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (isConnected) {
                    AssistChip(
                        onClick = {},
                        label = {
                            Text(
                                "Connected",
                                style = MaterialTheme.typography.labelSmall
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Filled.Check,
                                null,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            // BSSID
            Text(
                text = "BSSID: ${scanResult.BSSID}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(8.dp))

            // Signal strength bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Signal:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(52.dp)
                )
                LinearProgressIndicator(
                    progress = { signalFraction },
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp),
                    color = when {
                        signalLevel >= 3 -> MaterialTheme.colorScheme.primary
                        signalLevel >= 2 -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.error
                    },
                    trackColor = MaterialTheme.colorScheme.surfaceVariant,
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "${scanResult.level} dBm",
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.width(64.dp)
                )
            }

            Spacer(Modifier.height(4.dp))

            // Signal level text
            Text(
                text = "Level: ${signalLevelLabel(signalLevel)} ($signalLevel/4)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(8.dp))

            // Details row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                DetailChip(label = "Ch $channel")
                DetailChip(label = band)
                DetailChip(label = "${scanResult.frequency} MHz")
                DetailChip(label = securityType)
            }
        }
    }
}

@Composable
private fun DetailChip(label: String) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ConnectedInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
        Text(
            value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
}

private fun getChannelFromFrequency(frequency: Int): Int {
    return when (frequency) {
        in 2412..2484 -> {
            if (frequency == 2484) 14
            else (frequency - 2412) / 5 + 1
        }
        in 5170..5825 -> (frequency - 5000) / 5
        in 5955..7115 -> (frequency - 5955) / 5 + 1  // 6 GHz (WiFi 6E)
        else -> 0
    }
}

private fun getFrequencyBand(frequency: Int): String {
    return when {
        frequency in 2400..2500 -> "2.4 GHz"
        frequency in 5000..5900 -> "5 GHz"
        frequency in 5925..7125 -> "6 GHz"
        else -> "Unknown"
    }
}

private fun getSecurityType(capabilities: String): String {
    return when {
        capabilities.contains("WPA3") -> "WPA3"
        capabilities.contains("WPA2") -> "WPA2"
        capabilities.contains("WPA") -> "WPA"
        capabilities.contains("WEP") -> "WEP"
        else -> "Open"
    }
}

private fun getSignalLevel(rssi: Int): Int {
    return when {
        rssi >= -50 -> 4
        rssi >= -60 -> 3
        rssi >= -70 -> 2
        rssi >= -80 -> 1
        else -> 0
    }
}

private fun getSignalFraction(rssi: Int): Float {
    // Map dBm from -100..-30 range to 0..1
    val clamped = rssi.coerceIn(-100, -30)
    return (clamped + 100) / 70f
}

private fun signalLevelLabel(level: Int): String {
    return when (level) {
        4 -> "Excellent"
        3 -> "Good"
        2 -> "Fair"
        1 -> "Weak"
        else -> "Very Weak"
    }
}
