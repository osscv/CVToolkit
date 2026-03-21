package cv.toolkit.screens

import android.app.Activity
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
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cv.toolkit.R
import cv.toolkit.ads.AdMobManager
import cv.toolkit.ads.BannerAd
import kotlinx.coroutines.*
import org.json.JSONObject
import java.net.InetAddress
import java.net.URL

data class DnsRecord(val type: String, val value: String)

enum class DnsLookupMode {
    FORWARD, REVERSE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DnsLookupScreen(navController: NavController) {
    val context = LocalContext.current
    val activity = context as? Activity
    var input by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val records = remember { mutableStateListOf<DnsRecord>() }
    var error by remember { mutableStateOf<String?>(null) }
    var lookupMode by remember { mutableStateOf(DnsLookupMode.FORWARD) }
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    fun queryDns(host: String, type: String): List<String> {
        return try {
            val url = URL("https://dns.google/resolve?name=$host&type=$type")
            val response = url.readText()
            val json = JSONObject(response)
            val answers = json.optJSONArray("Answer") ?: return emptyList()
            (0 until answers.length()).mapNotNull { answers.getJSONObject(it).optString("data") }
        } catch (_: Exception) { emptyList() }
    }

    fun reverseLookup(ip: String): String? {
        return try {
            val addr = InetAddress.getByName(ip)
            val hostname = addr.canonicalHostName
            if (hostname != ip) hostname else null
        } catch (_: Exception) { null }
    }

    fun queryPtrRecord(ip: String): List<String> {
        return try {
            // Convert IP to reverse DNS format
            val parts = ip.split(".")
            if (parts.size != 4) return emptyList()
            val ptrName = "${parts[3]}.${parts[2]}.${parts[1]}.${parts[0]}.in-addr.arpa"
            queryDns(ptrName, "PTR")
        } catch (_: Exception) { emptyList() }
    }

    fun lookup() {
        if (input.isBlank()) return
        isLoading = true
        error = null
        records.clear()

        scope.launch(Dispatchers.IO) {
            try {
                val results = mutableListOf<DnsRecord>()
                val query = input.trim()
                    .removePrefix("http://")
                    .removePrefix("https://")
                    .split("/").first()

                when (lookupMode) {
                    DnsLookupMode.FORWARD -> {
                        // Forward DNS lookup (domain -> IP)
                        val types = listOf("A", "AAAA", "MX", "TXT", "NS", "CNAME", "SOA")
                        types.forEach { type ->
                            queryDns(query, type).forEach { value ->
                                results.add(DnsRecord(type, value))
                            }
                        }
                    }
                    DnsLookupMode.REVERSE -> {
                        // Reverse DNS lookup (IP -> domain)
                        // Try PTR record first
                        val ptrRecords = queryPtrRecord(query)
                        ptrRecords.forEach { ptr ->
                            results.add(DnsRecord("PTR", ptr.trimEnd('.')))
                        }

                        // Also try Java's reverse lookup
                        val hostname = reverseLookup(query)
                        if (hostname != null && !ptrRecords.any { it.trimEnd('.') == hostname }) {
                            results.add(DnsRecord("HOST", hostname))
                        }

                        // Get additional info about the IP
                        try {
                            val addr = InetAddress.getByName(query)
                            results.add(DnsRecord("IP", addr.hostAddress ?: query))
                            if (addr.isLoopbackAddress) results.add(DnsRecord("INFO", "Loopback Address"))
                            if (addr.isSiteLocalAddress) results.add(DnsRecord("INFO", "Private/Local Address"))
                            if (addr.isMulticastAddress) results.add(DnsRecord("INFO", "Multicast Address"))
                            if (addr.isLinkLocalAddress) results.add(DnsRecord("INFO", "Link-Local Address"))
                        } catch (_: Exception) {}
                    }
                }

                records.clear()
                records.addAll(results)
                if (results.isEmpty()) error = "No records found"
                activity?.let { AdMobManager.trackDnsUsage(it) }
            } catch (e: Exception) {
                error = e.message ?: "Lookup failed"
            }
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.dns_lookup_title)) },
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
            // Mode selector
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                SegmentedButton(
                    selected = lookupMode == DnsLookupMode.FORWARD,
                    onClick = {
                        lookupMode = DnsLookupMode.FORWARD
                        records.clear()
                        error = null
                    },
                    shape = SegmentedButtonDefaults.itemShape(0, 2),
                    icon = { Icon(Icons.Filled.Language, null, modifier = Modifier.size(18.dp)) }
                ) {
                    Text("Domain → IP")
                }
                SegmentedButton(
                    selected = lookupMode == DnsLookupMode.REVERSE,
                    onClick = {
                        lookupMode = DnsLookupMode.REVERSE
                        records.clear()
                        error = null
                    },
                    shape = SegmentedButtonDefaults.itemShape(1, 2),
                    icon = { Icon(Icons.Filled.SwapHoriz, null, modifier = Modifier.size(18.dp)) }
                ) {
                    Text("IP → Domain")
                }
            }

            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                label = { Text(if (lookupMode == DnsLookupMode.FORWARD) "Domain" else "IP Address") },
                placeholder = { Text(if (lookupMode == DnsLookupMode.FORWARD) "example.com" else "8.8.8.8") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                singleLine = true,
                leadingIcon = {
                    Icon(
                        if (lookupMode == DnsLookupMode.FORWARD) Icons.Filled.Language else Icons.Filled.Router,
                        null
                    )
                },
                trailingIcon = {
                    if (input.isNotEmpty()) {
                        IconButton(onClick = { input = "" }) {
                            Icon(Icons.Filled.Clear, "Clear")
                        }
                    }
                }
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            clipboard.getClipEntry()?.clipData?.getItemAt(0)?.text?.let {
                                input = it.toString()
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.ContentPaste, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Paste")
                }
                Button(
                    onClick = { lookup() },
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading && input.isNotBlank()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(Icons.Filled.Search, null, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(4.dp))
                    Text("Lookup")
                }
            }

            error?.let {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Error,
                            null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            if (records.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Records: ${records.size}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = {
                            val text = records.joinToString("\n") { "${it.type}: ${it.value}" }
                            scope.launch {
                                clipboard.setClipEntry(
                                    androidx.compose.ui.platform.ClipEntry(
                                        android.content.ClipData.newPlainText("dns", text)
                                    )
                                )
                            }
                        }
                    ) {
                        Icon(Icons.Filled.ContentCopy, "Copy All", modifier = Modifier.size(20.dp))
                    }
                }
            }

            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                items(records) { record ->
                    DnsRecordCard(record, clipboard, scope)
                }
            }
            BannerAd(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun DnsRecordCard(
    record: DnsRecord,
    clipboard: androidx.compose.ui.platform.Clipboard,
    scope: CoroutineScope
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = MaterialTheme.shapes.small,
                color = when (record.type) {
                    "A", "AAAA" -> MaterialTheme.colorScheme.primaryContainer
                    "PTR", "HOST" -> MaterialTheme.colorScheme.secondaryContainer
                    "MX" -> MaterialTheme.colorScheme.tertiaryContainer
                    "INFO" -> MaterialTheme.colorScheme.surfaceVariant
                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                }
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        record.type,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Text(
                record.value,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                fontFamily = if (record.type in listOf("A", "AAAA", "IP")) FontFamily.Monospace else FontFamily.Default
            )
            IconButton(
                onClick = {
                    scope.launch {
                        clipboard.setClipEntry(
                            androidx.compose.ui.platform.ClipEntry(
                                android.content.ClipData.newPlainText(record.type, record.value)
                            )
                        )
                    }
                },
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Filled.ContentCopy, "Copy", modifier = Modifier.size(18.dp))
            }
        }
    }
}
