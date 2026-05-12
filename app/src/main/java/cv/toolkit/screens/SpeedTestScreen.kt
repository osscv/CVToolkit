package cv.toolkit.screens

import android.Manifest
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cv.toolkit.R
import cv.toolkit.data.SpeedTestTargetsManager
import cv.toolkit.service.SpeedTestService
import kotlinx.coroutines.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.atomic.AtomicLong

data class TestTarget(val name: String, val url: String, val userAgent: String = "")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpeedTestScreen(navController: NavController) {
    val context = LocalContext.current
    var hasNotificationPermission by remember { mutableStateOf(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == android.content.pm.PackageManager.PERMISSION_GRANTED
        } else true
    )}

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> hasNotificationPermission = granted }

    val defaultUA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36"
    val defaultTargets = listOf(
        TestTarget("Hetzner NBG1 10GB", "https://nbg1-speed.hetzner.com/10GB.bin", defaultUA),
        TestTarget("Hetzner FSN1 10GB", "https://fsn1-speed.hetzner.com/10GB.bin", defaultUA),
        TestTarget("Hetzner HEL1 10GB", "https://hel1-speed.hetzner.com/10GB.bin", defaultUA),
        TestTarget("Hetzner ASH 10GB", "https://ash-speed.hetzner.com/10GB.bin", defaultUA),
        TestTarget("Hetzner HIL1 10GB", "https://hil1-speed.hetzner.com/10GB.bin", defaultUA),
        TestTarget("Hetzner SIN 10GB", "https://sin-speed.hetzner.com/10GB.bin", defaultUA),
        TestTarget("Hetzner ASH 1GB", "https://ash-speed.hetzner.com/1GB.bin", defaultUA),
        TestTarget("Mirror SG 10GB", "https://mirror.sg.gs/10gb.bin", defaultUA),
        TestTarget("NForce 10GB", "https://mirror.nforce.com/pub/speedtests/10000mb.bin", defaultUA),
        TestTarget("NForce 1GB", "https://mirror.nforce.com/pub/speedtests/1000mb.bin", defaultUA),
        TestTarget("NForce 500MB", "https://mirror.nforce.com/pub/speedtests/500mb.bin", defaultUA),
        TestTarget("Virtua 10GB", "https://ping.virtua.cloud/10GB.bin", defaultUA),
        TestTarget("Virtua 100MB", "https://ping.virtua.cloud/100MB.bin", defaultUA),
        TestTarget("TransIP 1GB", "http://speed.transip.nl/1gb.bin", defaultUA),
        TestTarget("OneAsiaHost 100MB", "http://speedtest.oneasiahost.com/100mb.bin", defaultUA)
    )

    // Persisted custom targets (built-in defaults are not stored — they're
    // appended at read time). Survives navigation away and app restart.
    var customTargets by remember {
        mutableStateOf(SpeedTestTargetsManager.load(context))
    }
    val targets = defaultTargets + customTargets
    var selectedTarget by remember { mutableStateOf(targets[0]) }
    var expanded by remember { mutableStateOf(false) }
    var showAddDialog by remember { mutableStateOf(false) }
    var threadCount by remember { mutableFloatStateOf(6f) }
    var backgroundRun by remember { mutableStateOf(false) }
    var autoStart by remember { mutableStateOf(false) }

    // Check if service is already running when screen loads
    var isRunning by remember { mutableStateOf(SpeedTestService.isRunning) }
    var isServiceMode by remember { mutableStateOf(SpeedTestService.isRunning) }

    var totalData by remember { mutableStateOf("0.00 MB") }
    var avgSpeed by remember { mutableStateOf("0.00 MB/s") }
    var bandwidth by remember { mutableStateOf("0.00 Mbps") }
    var latency by remember { mutableStateOf("-- ms") }

    val scope = rememberCoroutineScope()
    val totalBytes = remember { AtomicLong(0L) }
    val startTime = remember { mutableLongStateOf(0L) }

    // Sync with service state when returning to screen
    LaunchedEffect(Unit) {
        if (SpeedTestService.isRunning) {
            isRunning = true
            isServiceMode = true
            backgroundRun = true
            // Update UI with service stats
            while (SpeedTestService.isRunning) {
                val bytes = SpeedTestService.totalBytes.get()
                val elapsed = (System.currentTimeMillis() - SpeedTestService.serviceStartTime) / 1000.0
                val mb = bytes / (1024.0 * 1024.0)
                val speed = if (elapsed > 0) mb / elapsed else 0.0
                val mbps = speed * 8

                totalData = "%.2f MB".format(mb)
                avgSpeed = "%.2f MB/s".format(speed)
                bandwidth = "%.2f Mbps".format(mbps)

                delay(100)
            }
            // Service stopped externally (e.g., from notification)
            isRunning = false
            isServiceMode = false
        }
    }

    // Local speed test (non-service mode)
    LaunchedEffect(isRunning, isServiceMode) {
        // Skip local processing if running in service mode
        if (isRunning && !isServiceMode) {
            totalBytes.set(0L)
            startTime.longValue = System.currentTimeMillis()

            val jobs = (1..threadCount.toInt()).map {
                scope.launch(Dispatchers.IO) {
                    while (isActive && isRunning && !isServiceMode) {
                        try {
                            val conn = URL(selectedTarget.url).openConnection() as HttpURLConnection
                            conn.connectTimeout = 10000
                            conn.readTimeout = 10000
                            if (selectedTarget.userAgent.isNotBlank()) {
                                conn.setRequestProperty("User-Agent", selectedTarget.userAgent)
                            }
                            conn.inputStream.use { input ->
                                val buffer = ByteArray(8192)
                                var bytesRead: Int
                                while (input.read(buffer).also { bytesRead = it } != -1 && isRunning && !isServiceMode) {
                                    totalBytes.addAndGet(bytesRead.toLong())
                                }
                            }
                            conn.disconnect()
                        } catch (_: Exception) { }
                    }
                }
            }

            val pingJob = scope.launch(Dispatchers.IO) {
                while (isActive && isRunning && !isServiceMode) {
                    try {
                        val pingStart = System.currentTimeMillis()
                        val conn = URL(selectedTarget.url).openConnection() as HttpURLConnection
                        conn.connectTimeout = 5000
                        conn.readTimeout = 5000
                        conn.requestMethod = "HEAD"
                        conn.connect()
                        val pingTime = System.currentTimeMillis() - pingStart
                        conn.disconnect()
                        latency = "$pingTime ms"
                    } catch (_: Exception) {
                        latency = "-- ms"
                    }
                    delay(1000)
                }
            }

            while (isRunning && !isServiceMode) {
                val bytes = totalBytes.get()
                val elapsed = (System.currentTimeMillis() - startTime.longValue) / 1000.0
                val mb = bytes / (1024.0 * 1024.0)
                val speed = if (elapsed > 0) mb / elapsed else 0.0
                val mbps = speed * 8

                totalData = "%.2f MB".format(mb)
                avgSpeed = "%.2f MB/s".format(speed)
                bandwidth = "%.2f Mbps".format(mbps)

                delay(100)
            }

            pingJob.cancel()
            jobs.forEach { it.cancel() }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.speed_test_title),
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { if (!isRunning) expanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    OutlinedTextField(
                        value = selectedTarget.name,
                        onValueChange = {},
                        readOnly = true,
                        enabled = !isRunning,
                        label = { Text("Test Target") },
                        leadingIcon = { Icon(Icons.Filled.Link, null) },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                    )
                    ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        targets.forEach { target ->
                            val isCustom = target in customTargets
                            DropdownMenuItem(
                                text = { Text(target.name) },
                                onClick = { selectedTarget = target; expanded = false },
                                trailingIcon = if (isCustom) {
                                    {
                                        IconButton(onClick = {
                                            val newCustom = customTargets - target
                                            customTargets = newCustom
                                            SpeedTestTargetsManager.save(context, newCustom)
                                            if (selectedTarget == target) {
                                                selectedTarget = defaultTargets[0]
                                            }
                                            if (newCustom.isEmpty()) expanded = false
                                        }) {
                                            Icon(
                                                Icons.Filled.Delete,
                                                contentDescription = "Delete target",
                                                tint = MaterialTheme.colorScheme.error,
                                            )
                                        }
                                    }
                                } else null
                            )
                        }
                    }
                }
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = { showAddDialog = true }, enabled = !isRunning) {
                    Icon(Icons.Filled.Add, "Add Target")
                }
            }

            Column {
                Text("Thread Count: ${threadCount.toInt()}", style = MaterialTheme.typography.bodyMedium)
                Slider(
                    value = threadCount,
                    onValueChange = { threadCount = it },
                    valueRange = 1f..32f,
                    steps = 30,
                    enabled = !isRunning
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(
                        checked = backgroundRun,
                        onCheckedChange = { enabled ->
                            if (enabled) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                                val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
                                if (!pm.isIgnoringBatteryOptimizations(context.packageName)) {
                                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                        data = Uri.parse("package:${context.packageName}")
                                    }
                                    context.startActivity(intent)
                                }
                            }
                            backgroundRun = enabled
                        }
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Background Run")
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Switch(checked = autoStart, onCheckedChange = { autoStart = it })
                    Spacer(Modifier.width(8.dp))
                    Text("Auto Start")
                }
            }

            Spacer(Modifier.height(8.dp))

            SpeedStatCard(Icons.Filled.Cloud, "Total Data", totalData)
            SpeedStatCard(Icons.Filled.Speed, "Avg Speed", avgSpeed)
            SpeedStatCard(Icons.Filled.Bolt, "Bandwidth", bandwidth)
            SpeedStatCard(Icons.Filled.NetworkPing, "Latency", latency)

            Spacer(Modifier.weight(1f))

            Button(
                onClick = {
                    if (!isRunning) {
                        // Reset stats when starting new test
                        totalData = "0.00 MB"
                        avgSpeed = "0.00 MB/s"
                        bandwidth = "0.00 Mbps"
                        latency = "-- ms"

                        if (backgroundRun) {
                            isServiceMode = true
                            val intent = Intent(context, SpeedTestService::class.java).apply {
                                action = SpeedTestService.ACTION_START
                                putExtra(SpeedTestService.EXTRA_URL, selectedTarget.url)
                                putExtra(SpeedTestService.EXTRA_USER_AGENT, selectedTarget.userAgent)
                                putExtra(SpeedTestService.EXTRA_THREAD_COUNT, threadCount.toInt())
                            }
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                context.startForegroundService(intent)
                            } else {
                                context.startService(intent)
                            }
                        } else {
                            isServiceMode = false
                        }
                        isRunning = true
                    } else {
                        // Stop the test
                        if (isServiceMode || SpeedTestService.isRunning) {
                            context.stopService(Intent(context, SpeedTestService::class.java))
                        }
                        isRunning = false
                        isServiceMode = false
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Icon(
                    if (isRunning) Icons.Filled.Stop else Icons.Filled.PlayArrow,
                    null,
                    Modifier.size(32.dp)
                )
            }
        }
    }

    if (showAddDialog) {
        AddTargetDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { newTarget ->
                val newCustom = customTargets + newTarget
                customTargets = newCustom
                SpeedTestTargetsManager.save(context, newCustom)
                selectedTarget = newTarget
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun AddTargetDialog(onDismiss: () -> Unit, onAdd: (TestTarget) -> Unit) {
    var name by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var userAgent by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Custom Target") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = url, onValueChange = { url = it }, label = { Text("URL") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(value = userAgent, onValueChange = { userAgent = it }, label = { Text("User-Agent (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            TextButton(onClick = { if (name.isNotBlank() && url.isNotBlank()) onAdd(TestTarget(name, url, userAgent)) }, enabled = name.isNotBlank() && url.isNotBlank()) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun SpeedStatCard(icon: ImageVector, label: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(modifier = Modifier.size(48.dp), shape = MaterialTheme.shapes.medium, color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(icon, null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}
