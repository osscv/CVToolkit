package cv.toolkit.screens

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
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cv.toolkit.ads.BannerAd
import kotlinx.coroutines.launch
import java.nio.ByteBuffer
import java.security.MessageDigest
import java.util.*

enum class UuidVersion(val label: String, val description: String) {
    V1("UUID v1", "Time-based (MAC address)"),
    V4("UUID v4", "Random (most common)"),
    V5("UUID v5", "Name-based (SHA-1)")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UuidGeneratorScreen(navController: NavController) {
    var selectedVersion by remember { mutableStateOf(UuidVersion.V4) }
    var generatedUuids by remember { mutableStateOf(listOf<String>()) }
    var uuidCount by remember { mutableFloatStateOf(1f) }
    var uppercase by remember { mutableStateOf(false) }
    var noDashes by remember { mutableStateOf(false) }
    var namespace by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    fun generateUuids() {
        val count = uuidCount.toInt()
        generatedUuids = (1..count).map {
            var uuid = when (selectedVersion) {
                UuidVersion.V1 -> generateUuidV1()
                UuidVersion.V4 -> UUID.randomUUID().toString()
                UuidVersion.V5 -> {
                    val ns = if (namespace.isBlank()) UUID.randomUUID() else try {
                        UUID.fromString(namespace)
                    } catch (_: Exception) {
                        UUID.nameUUIDFromBytes(namespace.toByteArray())
                    }
                    val n = name.ifBlank { System.currentTimeMillis().toString() }
                    generateUuidV5(ns, n)
                }
            }
            if (noDashes) uuid = uuid.replace("-", "")
            if (uppercase) uuid = uuid.uppercase() else uuid.lowercase()
            uuid
        }
    }

    // Generate initial UUIDs
    LaunchedEffect(Unit) {
        generateUuids()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("UUID Generator") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
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
            // Version selector
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                UuidVersion.entries.forEachIndexed { index, version ->
                    SegmentedButton(
                        selected = selectedVersion == version,
                        onClick = { selectedVersion = version },
                        shape = SegmentedButtonDefaults.itemShape(index, UuidVersion.entries.size)
                    ) {
                        Text(version.label, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            // Version description
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Filled.Info,
                        null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        selectedVersion.description,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            // UUID v5 specific inputs
            if (selectedVersion == UuidVersion.V5) {
                OutlinedTextField(
                    value = namespace,
                    onValueChange = { namespace = it },
                    label = { Text("Namespace (UUID or string)") },
                    placeholder = { Text("Optional - leave blank for random") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    placeholder = { Text("Enter name to hash") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }

            // Options
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
                        Text("Count", style = MaterialTheme.typography.labelLarge)
                        Text(
                            "${uuidCount.toInt()}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = uuidCount,
                        onValueChange = { uuidCount = it },
                        valueRange = 1f..20f,
                        steps = 18,
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = uppercase, onCheckedChange = { uppercase = it })
                            Text("UPPERCASE", style = MaterialTheme.typography.bodySmall)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = noDashes, onCheckedChange = { noDashes = it })
                            Text("No Dashes", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }

            // Generate button
            Button(
                onClick = { generateUuids() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Generate UUID${if (uuidCount > 1) "s" else ""}")
            }

            // Generated UUIDs
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
                            "Generated UUID${if (generatedUuids.size > 1) "s" else ""}",
                            style = MaterialTheme.typography.labelMedium
                        )
                        if (generatedUuids.size == 1) {
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        clipboard.setClipEntry(
                                            androidx.compose.ui.platform.ClipEntry(
                                                android.content.ClipData.newPlainText("uuid", generatedUuids[0])
                                            )
                                        )
                                    }
                                }
                            ) {
                                Icon(Icons.Filled.ContentCopy, "Copy", modifier = Modifier.size(20.dp))
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        clipboard.setClipEntry(
                                            androidx.compose.ui.platform.ClipEntry(
                                                android.content.ClipData.newPlainText("uuids", generatedUuids.joinToString("\n"))
                                            )
                                        )
                                    }
                                }
                            ) {
                                Icon(Icons.Filled.ContentCopy, "Copy All", modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(generatedUuids) { uuid ->
                            UuidItem(
                                uuid = uuid,
                                showCopyButton = generatedUuids.size > 1,
                                onCopy = {
                                    scope.launch {
                                        clipboard.setClipEntry(
                                            androidx.compose.ui.platform.ClipEntry(
                                                android.content.ClipData.newPlainText("uuid", uuid)
                                            )
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }

            BannerAd(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun UuidItem(
    uuid: String,
    showCopyButton: Boolean,
    onCopy: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                uuid,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            if (showCopyButton) {
                IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.ContentCopy, "Copy", modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

private fun generateUuidV1(): String {
    val timestamp = System.currentTimeMillis()
    val gregorianOffset = 122192928000000000L
    val uuidTime = (timestamp * 10000) + gregorianOffset

    val timeLow = (uuidTime and 0xFFFFFFFFL).toInt()
    val timeMid = ((uuidTime shr 32) and 0xFFFFL).toInt()
    val timeHiAndVersion = (((uuidTime shr 48) and 0x0FFFL) or 0x1000L).toInt()

    val random = java.security.SecureRandom()
    val clockSeq = random.nextInt(0x3FFF) or 0x8000
    val node = ByteArray(6)
    random.nextBytes(node)
    node[0] = (node[0].toInt() or 0x01).toByte()

    return String.format(
        "%08x-%04x-%04x-%04x-%02x%02x%02x%02x%02x%02x",
        timeLow,
        timeMid,
        timeHiAndVersion,
        clockSeq,
        node[0].toInt() and 0xFF,
        node[1].toInt() and 0xFF,
        node[2].toInt() and 0xFF,
        node[3].toInt() and 0xFF,
        node[4].toInt() and 0xFF,
        node[5].toInt() and 0xFF
    )
}

private fun generateUuidV5(namespace: UUID, name: String): String {
    val md = MessageDigest.getInstance("SHA-1")

    val nsBytes = ByteBuffer.allocate(16)
        .putLong(namespace.mostSignificantBits)
        .putLong(namespace.leastSignificantBits)
        .array()

    md.update(nsBytes)
    md.update(name.toByteArray(Charsets.UTF_8))

    val hash = md.digest()

    hash[6] = ((hash[6].toInt() and 0x0F) or 0x50).toByte()
    hash[8] = ((hash[8].toInt() and 0x3F) or 0x80).toByte()

    val msb = ByteBuffer.wrap(hash, 0, 8).long
    val lsb = ByteBuffer.wrap(hash, 8, 8).long

    return UUID(msb, lsb).toString()
}
