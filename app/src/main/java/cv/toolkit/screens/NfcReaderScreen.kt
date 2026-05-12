package cv.toolkit.screens

import android.app.Activity
import android.content.Context
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.*
import android.os.Build
import android.os.Bundle
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cv.toolkit.R
import cv.toolkit.ads.BannerAd
import java.nio.charset.Charset
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class NfcTagInfo(
    val id: String,
    val techList: List<String>,
    val ndefRecords: List<NdefRecordInfo>,
    val tagType: String,
    val maxSize: Int?,
    val isWritable: Boolean?,
    val timestamp: String
)

private data class NdefRecordInfo(
    val type: String,
    val payload: String,
    val tnf: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NfcReaderScreen(navController: NavController) {
    val context = LocalContext.current
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val nfcAdapter = remember { NfcAdapter.getDefaultAdapter(context) }
    var nfcSupported by remember { mutableStateOf(nfcAdapter != null) }
    var nfcEnabled by remember { mutableStateOf(nfcAdapter?.isEnabled == true) }
    var isListening by remember { mutableStateOf(false) }
    var scannedTags by remember { mutableStateOf<List<NfcTagInfo>>(emptyList()) }

    // Enable reader mode when composable is active
    DisposableEffect(isListening) {
        if (isListening && nfcAdapter != null) {
            val activity = context as? Activity
            if (activity != null) {
                val callback = NfcAdapter.ReaderCallback { tag ->
                    val tagInfo = parseTag(tag)
                    scannedTags = listOf(tagInfo) + scannedTags
                }
                val flags = NfcAdapter.FLAG_READER_NFC_A or
                        NfcAdapter.FLAG_READER_NFC_B or
                        NfcAdapter.FLAG_READER_NFC_F or
                        NfcAdapter.FLAG_READER_NFC_V or
                        NfcAdapter.FLAG_READER_NFC_BARCODE
                val extras = Bundle().apply {
                    putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250)
                }
                nfcAdapter.enableReaderMode(activity, callback, flags, extras)
            }
        }
        onDispose {
            if (nfcAdapter != null) {
                val activity = context as? Activity
                if (activity != null) {
                    try {
                        nfcAdapter.disableReaderMode(activity)
                    } catch (_: Exception) {}
                }
            }
        }
    }

    // Check NFC state periodically
    LaunchedEffect(Unit) {
        while (true) {
            nfcEnabled = nfcAdapter?.isEnabled == true
            kotlinx.coroutines.delay(2000)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nfc_reader_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    if (scannedTags.isNotEmpty()) {
                        IconButton(onClick = { scannedTags = emptyList() }) {
                            Icon(Icons.Filled.DeleteOutline, contentDescription = "Clear")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (!nfcSupported) {
                // NFC not available
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Filled.NearbyError, null, Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("NFC Not Available", style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("This device does not have NFC hardware",
                                style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            } else if (!nfcEnabled) {
                // NFC disabled
                Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier.padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f))
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Filled.NearbyError, null, Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f))
                            Spacer(modifier = Modifier.height(16.dp))
                            Text("NFC is Disabled", style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Please enable NFC in your device settings to scan tags",
                                style = MaterialTheme.typography.bodyMedium, textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedButton(onClick = {
                                context.startActivity(android.content.Intent(android.provider.Settings.ACTION_NFC_SETTINGS))
                            }) {
                                Icon(Icons.Filled.Settings, null, Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Open NFC Settings")
                            }
                        }
                    }
                }
            } else {
                // NFC available and enabled
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Scan toggle
                    item {
                        Button(
                            onClick = { isListening = !isListening },
                            modifier = Modifier.fillMaxWidth(),
                            colors = if (isListening) ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            ) else ButtonDefaults.buttonColors()
                        ) {
                            Icon(
                                if (isListening) Icons.Filled.Stop else Icons.Filled.Nfc,
                                null, Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(if (isListening) "Stop Scanning" else "Start Scanning")
                        }
                    }

                    // Scanning indicator
                    if (isListening && scannedTags.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(48.dp),
                                        strokeWidth = 3.dp
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("Waiting for NFC tag...", style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.SemiBold)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Hold your device near an NFC tag to read it",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center)
                                }
                            }
                        }
                    }

                    // Empty state (not scanning)
                    if (!isListening && scannedTags.isEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(Icons.Filled.Nfc, null, Modifier.size(64.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text("Read NFC tags", style = MaterialTheme.typography.bodyLarge,
                                        textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Tap Start Scanning, then hold your device near an NFC tag",
                                        style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                                }
                            }
                        }
                    }

                    // Scanned tags
                    items(scannedTags) { tag ->
                        NfcTagCard(tag = tag, onCopy = { text ->
                            clipboardManager.setText(AnnotatedString(text))
                        })
                    }
                }
            }
            BannerAd(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun NfcTagCard(tag: NfcTagInfo, onCopy: (String) -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Nfc, null, Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(tag.tagType, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton(onClick = { onCopy(buildCopyText(tag)) }, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.ContentCopy, "Copy", Modifier.size(18.dp))
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            NfcDetailRow("Tag ID", tag.id)
            NfcDetailRow("Scanned", tag.timestamp)

            if (tag.techList.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text("Technologies", style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(4.dp))
                tag.techList.forEach { tech ->
                    Text(
                        "\u2022 $tech",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            tag.maxSize?.let { size ->
                NfcDetailRow("Max Size", "$size bytes")
            }
            tag.isWritable?.let { writable ->
                NfcDetailRow("Writable", if (writable) "Yes" else "No")
            }

            if (tag.ndefRecords.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text("NDEF Records (${tag.ndefRecords.size})", style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.height(4.dp))
                tag.ndefRecords.forEachIndexed { index, record ->
                    if (index > 0) {
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    }
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            NfcDetailRow("TNF", record.tnf)
                            NfcDetailRow("Type", record.type)
                            Text("Payload", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                            Text(
                                record.payload,
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NfcDetailRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.width(80.dp))
        Text(value, style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            modifier = Modifier.weight(1f))
    }
}

private fun parseTag(tag: Tag): NfcTagInfo {
    val id = tag.id?.joinToString(":") { String.format("%02X", it) } ?: "Unknown"
    val techList = tag.techList.map { it.substringAfterLast(".") }
    val tagType = detectTagType(tag)

    var maxSize: Int? = null
    var isWritable: Boolean? = null
    val ndefRecords = mutableListOf<NdefRecordInfo>()

    // Try to read NDEF
    val ndef = Ndef.get(tag)
    if (ndef != null) {
        try {
            ndef.connect()
            maxSize = ndef.maxSize
            isWritable = ndef.isWritable
            ndef.cachedNdefMessage?.let { msg ->
                ndefRecords.addAll(parseNdefMessage(msg))
            }
            ndef.close()
        } catch (_: Exception) {
            try { ndef.close() } catch (_: Exception) {}
        }
    }

    // Try NdefFormatable if no NDEF
    if (ndefRecords.isEmpty()) {
        val ndefFormatable = NdefFormatable.get(tag)
        if (ndefFormatable != null && tagType == "Unknown") {
            // Tag is formatable but empty
        }
    }

    val timestamp = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())

    return NfcTagInfo(
        id = id,
        techList = techList,
        ndefRecords = ndefRecords,
        tagType = tagType,
        maxSize = maxSize,
        isWritable = isWritable,
        timestamp = timestamp
    )
}

private fun detectTagType(tag: Tag): String {
    val techs = tag.techList.map { it.substringAfterLast(".") }
    return when {
        techs.contains("MifareClassic") -> "MIFARE Classic"
        techs.contains("MifareUltralight") -> {
            val mu = MifareUltralight.get(tag)
            when (mu?.type) {
                MifareUltralight.TYPE_ULTRALIGHT -> "MIFARE Ultralight"
                MifareUltralight.TYPE_ULTRALIGHT_C -> "MIFARE Ultralight C"
                else -> "MIFARE Ultralight"
            }
        }
        techs.contains("IsoDep") && techs.contains("NfcA") -> "ISO 14443-4 Type A (NFC-A)"
        techs.contains("IsoDep") && techs.contains("NfcB") -> "ISO 14443-4 Type B (NFC-B)"
        techs.contains("IsoDep") -> "ISO 14443-4 (IsoDep)"
        techs.contains("NfcA") -> "NFC-A (ISO 14443-3A)"
        techs.contains("NfcB") -> "NFC-B (ISO 14443-3B)"
        techs.contains("NfcF") -> "NFC-F (FeliCa)"
        techs.contains("NfcV") -> "NFC-V (ISO 15693)"
        techs.contains("NfcBarcode") -> "NFC Barcode"
        techs.contains("Ndef") -> "NDEF Tag"
        techs.contains("NdefFormatable") -> "NDEF Formatable"
        else -> "Unknown NFC Tag"
    }
}

private fun parseNdefMessage(message: NdefMessage): List<NdefRecordInfo> {
    return message.records.map { record ->
        val tnf = when (record.tnf) {
            NdefRecord.TNF_EMPTY -> "Empty"
            NdefRecord.TNF_WELL_KNOWN -> "Well-Known"
            NdefRecord.TNF_MIME_MEDIA -> "MIME Media"
            NdefRecord.TNF_ABSOLUTE_URI -> "Absolute URI"
            NdefRecord.TNF_EXTERNAL_TYPE -> "External Type"
            NdefRecord.TNF_UNKNOWN -> "Unknown"
            NdefRecord.TNF_UNCHANGED -> "Unchanged"
            else -> "Reserved (${record.tnf})"
        }
        val type = String(record.type, Charsets.US_ASCII)
        val payload = parseNdefPayload(record)

        NdefRecordInfo(type = type, payload = payload, tnf = tnf)
    }
}

private fun parseNdefPayload(record: NdefRecord): String {
    val payload = record.payload
    if (payload.isEmpty()) return "(empty)"

    return when {
        // Well-Known URI
        record.tnf == NdefRecord.TNF_WELL_KNOWN && record.type.contentEquals(NdefRecord.RTD_URI) -> {
            val prefixByte = payload[0].toInt() and 0xFF
            val uriPrefixes = arrayOf(
                "", "http://www.", "https://www.", "http://", "https://",
                "tel:", "mailto:", "ftp://anonymous:anonymous@", "ftp://ftp.",
                "ftps://", "sftp://", "smb://", "nfs://", "ftp://", "dav://",
                "news:", "telnet://", "imap:", "rtsp://", "urn:", "pop:",
                "sip:", "sips:", "tftp:", "btspp://", "btl2cap://", "btgoep://",
                "tcpobex://", "irdaobex://", "file://", "urn:epc:id:", "urn:epc:tag:",
                "urn:epc:pat:", "urn:epc:raw:", "urn:epc:", "urn:nfc:"
            )
            val prefix = if (prefixByte < uriPrefixes.size) uriPrefixes[prefixByte] else ""
            val rest = String(payload, 1, payload.size - 1, Charsets.UTF_8)
            prefix + rest
        }
        // Well-Known Text
        record.tnf == NdefRecord.TNF_WELL_KNOWN && record.type.contentEquals(NdefRecord.RTD_TEXT) -> {
            val statusByte = payload[0].toInt() and 0xFF
            val langLen = statusByte and 0x3F
            val isUtf16 = (statusByte and 0x80) != 0
            val charset = if (isUtf16) Charsets.UTF_16 else Charsets.UTF_8
            val lang = String(payload, 1, langLen, Charsets.US_ASCII)
            val text = String(payload, 1 + langLen, payload.size - 1 - langLen, charset)
            "[$lang] $text"
        }
        // MIME media
        record.tnf == NdefRecord.TNF_MIME_MEDIA -> {
            // Try UTF-8 first
            val text = String(payload, Charsets.UTF_8)
            if (text.all { it.isLetterOrDigit() || it.isWhitespace() || it in "{}[]()\"':;,.!?@#\$%^&*-_+=/<>~`\\|" }) {
                text
            } else {
                payload.joinToString(" ") { String.format("%02X", it) }
            }
        }
        // Absolute URI
        record.tnf == NdefRecord.TNF_ABSOLUTE_URI -> {
            String(record.type, Charsets.UTF_8)
        }
        // External type
        record.tnf == NdefRecord.TNF_EXTERNAL_TYPE -> {
            val text = String(payload, Charsets.UTF_8)
            if (text.all { it.code in 0x20..0x7E }) text
            else payload.joinToString(" ") { String.format("%02X", it) }
        }
        // Fallback: try text, otherwise hex
        else -> {
            val text = String(payload, Charsets.UTF_8)
            if (text.all { it.code in 0x20..0x7E }) text
            else payload.joinToString(" ") { String.format("%02X", it) }
        }
    }
}

private fun buildCopyText(tag: NfcTagInfo): String {
    val sb = StringBuilder()
    sb.appendLine("NFC Tag: ${tag.tagType}")
    sb.appendLine("ID: ${tag.id}")
    sb.appendLine("Technologies: ${tag.techList.joinToString(", ")}")
    tag.maxSize?.let { sb.appendLine("Max Size: $it bytes") }
    tag.isWritable?.let { sb.appendLine("Writable: ${if (it) "Yes" else "No"}") }
    if (tag.ndefRecords.isNotEmpty()) {
        sb.appendLine("NDEF Records:")
        tag.ndefRecords.forEachIndexed { i, r ->
            sb.appendLine("  Record ${i + 1}: [${r.tnf}] ${r.type}")
            sb.appendLine("  Payload: ${r.payload}")
        }
    }
    return sb.toString()
}
