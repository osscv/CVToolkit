package cv.toolkit.screens

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.ui.graphics.Color
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
import kotlinx.coroutines.delay

private data class BtDevice(
    val name: String,
    val address: String,
    val rssi: Int?,
    val type: String,
    val bondState: String,
    val isBle: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothScannerScreen(navController: NavController) {
    val context = LocalContext.current
    val bluetoothManager = remember { context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager }
    val bluetoothAdapter = remember { bluetoothManager?.adapter }

    var devices by remember { mutableStateOf(listOf<BtDevice>()) }
    var isScanning by remember { mutableStateOf(false) }
    var hasPermission by remember { mutableStateOf(false) }
    var statusMessage by remember { mutableStateOf("") }

    val requiredPermissions = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasPermission = results.values.all { it }
        if (!hasPermission) {
            statusMessage = "Bluetooth permissions are required"
        }
    }

    // Check permissions on launch
    LaunchedEffect(Unit) {
        hasPermission = requiredPermissions.all {
            ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
        }
        if (!hasPermission) {
            permissionLauncher.launch(requiredPermissions)
        }
    }

    // Classic Bluetooth discovery receiver
    DisposableEffect(Unit) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context?, intent: Intent?) {
                if (intent?.action == BluetoothDevice.ACTION_FOUND) {
                    try {
                        val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                        } else {
                            @Suppress("DEPRECATION")
                            intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                        }
                        val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE).toInt()
                        device?.let { addDevice(it, rssi, false, devices) { devices = it } }
                    } catch (_: SecurityException) { }
                }
            }
        }
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        context.registerReceiver(receiver, filter)
        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    // BLE scan callback
    val bleScanCallback = remember {
        object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                try {
                    addDevice(result.device, result.rssi, true, devices) { devices = it }
                } catch (_: SecurityException) { }
            }
        }
    }

    // Stop scan after timeout
    LaunchedEffect(isScanning) {
        if (isScanning) {
            delay(15000)
            stopScanning(bluetoothAdapter, bleScanCallback)
            isScanning = false
            statusMessage = "Scan complete. Found ${devices.size} devices."
        }
    }

    fun startScan() {
        if (bluetoothAdapter == null) {
            statusMessage = "Bluetooth not available"
            return
        }
        if (!bluetoothAdapter.isEnabled) {
            statusMessage = "Bluetooth is disabled. Please enable it."
            return
        }
        if (!hasPermission) {
            permissionLauncher.launch(requiredPermissions)
            return
        }

        devices = emptyList()
        isScanning = true
        statusMessage = "Scanning..."

        try {
            // Add bonded devices first
            bluetoothAdapter.bondedDevices?.forEach { device ->
                addDevice(device, null, false, devices) { devices = it }
            }

            // Start classic discovery
            @Suppress("MissingPermission")
            bluetoothAdapter.startDiscovery()

            // Start BLE scan
            bluetoothAdapter.bluetoothLeScanner?.startScan(bleScanCallback)
        } catch (_: SecurityException) {
            statusMessage = "Permission denied"
            isScanning = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.bluetooth_scanner_title)) },
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
        ) {
            // Scan button and status
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (bluetoothAdapter == null) {
                        Icon(Icons.Filled.BluetoothDisabled, null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(8.dp))
                        Text("Bluetooth not available on this device", style = MaterialTheme.typography.bodyMedium)
                    } else {
                        Button(
                            onClick = {
                                if (isScanning) {
                                    stopScanning(bluetoothAdapter, bleScanCallback)
                                    isScanning = false
                                    statusMessage = "Scan stopped. Found ${devices.size} devices."
                                } else {
                                    startScan()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (isScanning) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Spacer(Modifier.width(8.dp))
                                Text("Stop Scanning")
                            } else {
                                Icon(@Suppress("DEPRECATION") Icons.Filled.BluetoothSearching, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Start Scan")
                            }
                        }

                        if (statusMessage.isNotEmpty()) {
                            Spacer(Modifier.height(8.dp))
                            Text(
                                statusMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Device count
            if (devices.isNotEmpty()) {
                Text(
                    "Found ${devices.size} device${if (devices.size > 1) "s" else ""}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }

            // Device list
            LazyColumn(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(devices.sortedByDescending { it.rssi ?: -200 }) { device ->
                    BluetoothDeviceCard(device)
                }
            }
            BannerAd(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun BluetoothDeviceCard(device: BtDevice) {
    val signalStrength = device.rssi?.let {
        when {
            it >= -50 -> "Excellent"
            it >= -70 -> "Good"
            it >= -85 -> "Fair"
            else -> "Weak"
        }
    }

    val signalColor = device.rssi?.let {
        when {
            it >= -50 -> MaterialTheme.colorScheme.primary
            it >= -70 -> Color(0xFF34A853)
            it >= -85 -> Color(0xFFFBBC05)
            else -> MaterialTheme.colorScheme.error
        }
    } ?: MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (device.isBle) @Suppress("DEPRECATION") Icons.Filled.BluetoothSearching else Icons.Filled.Bluetooth,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    device.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    device.address,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = if (device.isBle) MaterialTheme.colorScheme.tertiaryContainer else MaterialTheme.colorScheme.secondaryContainer
                    ) {
                        Text(
                            if (device.isBle) "BLE" else device.type,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (device.bondState != "None") {
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                device.bondState,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            if (device.rssi != null) {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "${device.rssi} dBm",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = signalColor
                    )
                    Text(
                        signalStrength ?: "",
                        style = MaterialTheme.typography.bodySmall,
                        color = signalColor
                    )
                }
            }
        }
    }
}

@Suppress("MissingPermission")
private fun stopScanning(adapter: BluetoothAdapter?, bleScanCallback: ScanCallback) {
    try {
        adapter?.cancelDiscovery()
        adapter?.bluetoothLeScanner?.stopScan(bleScanCallback)
    } catch (_: SecurityException) { }
}

@Suppress("MissingPermission")
private fun addDevice(
    device: BluetoothDevice,
    rssi: Int?,
    isBle: Boolean,
    currentDevices: List<BtDevice>,
    update: (List<BtDevice>) -> Unit
) {
    try {
        val name = device.name ?: "Unknown Device"
        val address = device.address ?: return

        val type = try {
            when (device.type) {
                BluetoothDevice.DEVICE_TYPE_CLASSIC -> "Classic"
                BluetoothDevice.DEVICE_TYPE_LE -> "LE"
                BluetoothDevice.DEVICE_TYPE_DUAL -> "Dual"
                else -> "Unknown"
            }
        } catch (_: SecurityException) { "Unknown" }

        val bondState = try {
            when (device.bondState) {
                BluetoothDevice.BOND_BONDED -> "Paired"
                BluetoothDevice.BOND_BONDING -> "Pairing"
                else -> "None"
            }
        } catch (_: SecurityException) { "None" }

        val existing = currentDevices.indexOfFirst { it.address == address }
        if (existing >= 0) {
            // Update RSSI if stronger
            val existingDevice = currentDevices[existing]
            if (rssi != null && (existingDevice.rssi == null || rssi > existingDevice.rssi)) {
                val updated = currentDevices.toMutableList()
                updated[existing] = existingDevice.copy(rssi = rssi)
                update(updated)
            }
        } else {
            update(currentDevices + BtDevice(name, address, rssi, type, bondState, isBle))
        }
    } catch (_: SecurityException) { }
}

