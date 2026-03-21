package cv.toolkit.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnixTimestampScreen(navController: NavController) {
    var currentTimestamp by remember { mutableLongStateOf(System.currentTimeMillis() / 1000) }
    var timestampInput by remember { mutableStateOf("") }
    var dateInput by remember { mutableStateOf("") }
    var convertedDate by remember { mutableStateOf<String?>(null) }
    var convertedTimestamp by remember { mutableStateOf<Long?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var useMilliseconds by remember { mutableStateOf(false) }
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    // Update current timestamp every second
    LaunchedEffect(Unit) {
        while (true) {
            currentTimestamp = if (useMilliseconds) System.currentTimeMillis() else System.currentTimeMillis() / 1000
            delay(1000)
        }
    }

    // Update current timestamp when unit changes
    LaunchedEffect(useMilliseconds) {
        currentTimestamp = if (useMilliseconds) System.currentTimeMillis() else System.currentTimeMillis() / 1000
    }

    fun convertTimestampToDate() {
        errorMessage = null
        convertedDate = null

        if (timestampInput.isBlank()) return

        try {
            val timestamp = timestampInput.trim().toLong()
            val millis = if (useMilliseconds) timestamp else timestamp * 1000

            if (millis < 0 || millis > 253402300799000L) {
                errorMessage = "Timestamp out of valid range"
                return
            }

            val date = Date(millis)
            val formats = listOf(
                "yyyy-MM-dd HH:mm:ss z" to TimeZone.getDefault(),
                "yyyy-MM-dd HH:mm:ss 'UTC'" to TimeZone.getTimeZone("UTC"),
                "EEE, dd MMM yyyy HH:mm:ss z" to TimeZone.getDefault(),
                "ISO 8601" to null
            )

            val result = StringBuilder()
            formats.forEach { (pattern, tz) ->
                if (pattern == "ISO 8601") {
                    val iso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX", Locale.getDefault())
                    iso.timeZone = TimeZone.getDefault()
                    result.appendLine("ISO 8601: ${iso.format(date)}")
                } else {
                    val sdf = SimpleDateFormat(pattern, Locale.getDefault())
                    tz?.let { sdf.timeZone = it }
                    result.appendLine("${if (tz?.id == "UTC") "UTC" else "Local"}: ${sdf.format(date)}")
                }
            }

            convertedDate = result.toString().trim()
        } catch (e: NumberFormatException) {
            errorMessage = "Invalid timestamp format"
        } catch (e: Exception) {
            errorMessage = "Conversion failed: ${e.message}"
        }
    }

    fun convertDateToTimestamp() {
        errorMessage = null
        convertedTimestamp = null

        if (dateInput.isBlank()) return

        val formats = listOf(
            "yyyy-MM-dd HH:mm:ss",
            "yyyy-MM-dd HH:mm",
            "yyyy-MM-dd",
            "MM/dd/yyyy HH:mm:ss",
            "MM/dd/yyyy",
            "dd-MM-yyyy HH:mm:ss",
            "dd-MM-yyyy",
            "yyyy/MM/dd HH:mm:ss",
            "yyyy/MM/dd"
        )

        for (format in formats) {
            try {
                val sdf = SimpleDateFormat(format, Locale.getDefault())
                sdf.isLenient = false
                val date = sdf.parse(dateInput.trim())
                if (date != null) {
                    convertedTimestamp = if (useMilliseconds) date.time else date.time / 1000
                    return
                }
            } catch (_: Exception) {
                continue
            }
        }

        errorMessage = "Could not parse date. Try formats like:\nyyyy-MM-dd HH:mm:ss\nMM/dd/yyyy\nyyyy-MM-dd"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Unix Timestamp") },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Current timestamp card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Current Unix Timestamp",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            currentTimestamp.toString(),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(Modifier.width(8.dp))
                        IconButton(
                            onClick = {
                                scope.launch {
                                    clipboard.setClipEntry(
                                        androidx.compose.ui.platform.ClipEntry(
                                            android.content.ClipData.newPlainText("timestamp", currentTimestamp.toString())
                                        )
                                    )
                                }
                            }
                        ) {
                            Icon(
                                Icons.Filled.ContentCopy,
                                "Copy",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    Text(
                        if (useMilliseconds) "milliseconds" else "seconds",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                }
            }

            // Unit toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text("Seconds", style = MaterialTheme.typography.bodySmall)
                Switch(
                    checked = useMilliseconds,
                    onCheckedChange = { useMilliseconds = it },
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
                Text("Milliseconds", style = MaterialTheme.typography.bodySmall)
            }

            HorizontalDivider()

            // Timestamp to Date conversion
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Timestamp → Date",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = timestampInput,
                        onValueChange = { timestampInput = it },
                        label = { Text("Unix Timestamp") },
                        placeholder = { Text(if (useMilliseconds) "1702300800000" else "1702300800") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        trailingIcon = {
                            if (timestampInput.isNotEmpty()) {
                                IconButton(onClick = { timestampInput = ""; convertedDate = null }) {
                                    Icon(Icons.Filled.Clear, "Clear")
                                }
                            }
                        }
                    )

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                timestampInput = currentTimestamp.toString()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.AccessTime, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Now")
                        }
                        Button(
                            onClick = { convertTimestampToDate() },
                            modifier = Modifier.weight(1f),
                            enabled = timestampInput.isNotBlank()
                        ) {
                            Icon(Icons.Filled.Transform, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Convert")
                        }
                    }

                    convertedDate?.let { result ->
                        Spacer(Modifier.height(12.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Result",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                clipboard.setClipEntry(
                                                    androidx.compose.ui.platform.ClipEntry(
                                                        android.content.ClipData.newPlainText("date", result)
                                                    )
                                                )
                                            }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(Icons.Filled.ContentCopy, "Copy", modifier = Modifier.size(18.dp))
                                    }
                                }
                                Text(
                                    result,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }

            // Date to Timestamp conversion
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Date → Timestamp",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(12.dp))

                    OutlinedTextField(
                        value = dateInput,
                        onValueChange = { dateInput = it },
                        label = { Text("Date & Time") },
                        placeholder = { Text("2024-12-11 15:30:00") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        trailingIcon = {
                            if (dateInput.isNotEmpty()) {
                                IconButton(onClick = { dateInput = ""; convertedTimestamp = null }) {
                                    Icon(Icons.Filled.Clear, "Clear")
                                }
                            }
                        }
                    )

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                                dateInput = sdf.format(Date())
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Filled.Today, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Now")
                        }
                        Button(
                            onClick = { convertDateToTimestamp() },
                            modifier = Modifier.weight(1f),
                            enabled = dateInput.isNotBlank()
                        ) {
                            Icon(Icons.Filled.Transform, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Convert")
                        }
                    }

                    convertedTimestamp?.let { result ->
                        Spacer(Modifier.height(12.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        "Unix Timestamp",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                    Text(
                                        result.toString(),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            clipboard.setClipEntry(
                                                androidx.compose.ui.platform.ClipEntry(
                                                    android.content.ClipData.newPlainText("timestamp", result.toString())
                                                )
                                            )
                                        }
                                    }
                                ) {
                                    Icon(Icons.Filled.ContentCopy, "Copy")
                                }
                            }
                        }
                    }
                }
            }

            // Error message
            errorMessage?.let {
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

            // Reference card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        "Common Timestamps",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(8.dp))
                    TimestampReference("Y2K (2000-01-01)", 946684800)
                    TimestampReference("Unix Epoch (1970-01-01)", 0)
                    TimestampReference("Y2K38 Problem", 2147483647)
                }
            }

            BannerAd(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun TimestampReference(label: String, timestamp: Long) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            timestamp.toString(),
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace
        )
    }
}
