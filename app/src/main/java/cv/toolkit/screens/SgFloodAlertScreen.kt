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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import cv.toolkit.ads.BannerAd
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

private data class FloodReading(
    val areaDesc: String?,
    val circle: String?,
    val description: String?,
    val headline: String?,
    val instruction: String?,
    val severity: String?,
    val urgency: String?,
    val event: String?
)

private data class FloodRecord(
    val datetime: String?,
    val msgType: String?,
    val identifier: String?,
    val readings: List<FloodReading>?
)

private fun severityColor(severity: String?): Color = when (severity?.lowercase()) {
    "extreme" -> Color(0xFFD32F2F)
    "severe" -> Color(0xFFE65100)
    "moderate" -> Color(0xFFFF9800)
    "minor" -> Color(0xFFFDD835)
    else -> Color.Gray
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SgFloodAlertScreen(navController: NavController) {
    var readings by remember { mutableStateOf<List<FloodReading>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var lastUpdated by remember { mutableStateOf("") }

    val client = remember {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }
    val gson = remember { Gson() }
    val scope = rememberCoroutineScope()

    fun fetchAlerts() {
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val body = withContext(Dispatchers.IO) {
                    val request = Request.Builder()
                        .url("https://api-open.data.gov.sg/v2/real-time/api/weather/flood-alerts")
                        .addHeader(
                            "x-api-key",
                            "v2:3628e11212324251b93eccb69f46844aa70b7877ab736bf17f2f75deb7c19c0b:bmc3TaMdW9V-4pdcz5quf7qP-4VCHR2B"
                        )
                        .build()
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) throw Exception("HTTP ${response.code}")
                        response.body.string()
                    }
                }
                val mapType = object : TypeToken<Map<String, Any>>() {}.type
                val root: Map<String, Any> = gson.fromJson(body, mapType)
                val data = root["data"] as? Map<*, *>
                val recordsRaw = data?.get("records") as? List<*> ?: emptyList<Any>()

                val allReadings = mutableListOf<FloodReading>()
                for (rec in recordsRaw) {
                    val recJson = gson.toJson(rec)
                    val record = gson.fromJson(recJson, FloodRecord::class.java)
                    record.readings?.let { allReadings.addAll(it) }
                }

                readings = allReadings
                val now = java.text.SimpleDateFormat(
                    "dd MMM yyyy, HH:mm:ss", java.util.Locale.getDefault()
                ).format(java.util.Date())
                lastUpdated = now
            } catch (e: Exception) {
                errorMessage = e.message ?: "Unknown error"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        fetchAlerts()
        while (true) {
            delay(60_000L)
            fetchAlerts()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SG Flood Alerts") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { fetchAlerts() }, enabled = !isLoading) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        bottomBar = { BannerAd() }
    ) { padding ->
        when {
            isLoading && readings.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Loading flood alerts...")
                    }
                }
            }

            errorMessage != null && readings.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Error: $errorMessage", color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { fetchAlerts() }) {
                            Text("Retry")
                        }
                    }
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Status card
                    item {
                        if (readings.isEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF4CAF50).copy(alpha = 0.15f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(20.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = Color(0xFF4CAF50),
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(
                                            "No Active Flood Alerts",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2E7D32)
                                        )
                                        Text(
                                            "All areas are clear",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFF388E3C)
                                        )
                                    }
                                }
                            }
                        } else {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFD32F2F).copy(alpha = 0.15f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(20.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        Icons.Default.Warning,
                                        contentDescription = null,
                                        tint = Color(0xFFD32F2F),
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Column {
                                        Text(
                                            "${readings.size} Active Flood Alert${if (readings.size > 1) "s" else ""}",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFFC62828)
                                        )
                                        Text(
                                            "Tap each alert for details",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color(0xFFD32F2F)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Alert cards
                    items(readings) { reading ->
                        val color = severityColor(reading.severity)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = color.copy(alpha = 0.1f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = color,
                                        shape = MaterialTheme.shapes.small
                                    ) {
                                        Text(
                                            text = reading.severity?.uppercase() ?: "UNKNOWN",
                                            color = Color.White,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    if (reading.urgency != null) {
                                        Surface(
                                            color = MaterialTheme.colorScheme.surfaceVariant,
                                            shape = MaterialTheme.shapes.small
                                        ) {
                                            Text(
                                                text = reading.urgency,
                                                style = MaterialTheme.typography.labelSmall,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = reading.headline ?: "Flood Alert",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                if (!reading.areaDesc.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(verticalAlignment = Alignment.Top) {
                                        Icon(
                                            Icons.Default.LocationOn,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = color
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = reading.areaDesc,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                }
                                if (!reading.description.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = reading.description,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                if (!reading.instruction.isNullOrBlank()) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Surface(
                                        color = MaterialTheme.colorScheme.surfaceVariant,
                                        shape = MaterialTheme.shapes.small
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(8.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Icon(
                                                Icons.Default.Info,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = reading.instruction,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Last updated
                    if (lastUpdated.isNotEmpty()) {
                        item {
                            Text(
                                text = "Last updated: $lastUpdated",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp, bottom = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
