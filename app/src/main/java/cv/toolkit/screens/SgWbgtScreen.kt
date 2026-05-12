package cv.toolkit.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
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

private data class WbgtReading(
    val stationName: String,
    val stationId: String,
    val townCenter: String,
    val wbgt: Double,
    val heatStress: String
)

private fun heatStressColor(level: String): Color = when (level) {
    "Low" -> Color(0xFF388E3C)
    "Moderate" -> Color(0xFFF9A825)
    "High" -> Color(0xFFFF6F00)
    "Extreme" -> Color(0xFFD32F2F)
    else -> Color.Gray
}

private fun overallHeatStress(avg: Double): String = when {
    avg >= 33 -> "Extreme"
    avg >= 31 -> "High"
    avg >= 28 -> "Moderate"
    else -> "Low"
}

private fun parseWbgtReadings(json: String, gson: Gson): List<WbgtReading> {
    val type = object : TypeToken<Map<String, Any>>() {}.type
    val root: Map<String, Any> = gson.fromJson(json, type)
    val data = root["data"] as? Map<*, *> ?: return emptyList()
    val records = data["records"] as? List<*> ?: return emptyList()

    val result = mutableListOf<WbgtReading>()
    for (rec in records) {
        val recMap = rec as? Map<*, *> ?: continue
        val item = recMap["item"] as? Map<*, *> ?: continue
        val readings = item["readings"] as? List<*> ?: continue
        for (r in readings) {
            val rm = r as? Map<*, *> ?: continue
            val station = rm["station"] as? Map<*, *> ?: continue
            val name = station["name"]?.toString() ?: continue
            val id = station["id"]?.toString() ?: ""
            val town = station["townCenter"]?.toString() ?: ""
            val wbgt = rm["wbgt"]?.toString()?.toDoubleOrNull() ?: continue
            val stress = rm["heatStress"]?.toString() ?: "Low"
            result.add(WbgtReading(name, id, town, wbgt, stress))
        }
    }
    return result
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SgWbgtScreen(navController: NavController) {
    var readings by remember { mutableStateOf<List<WbgtReading>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val client = remember {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }
    val gson = remember { Gson() }
    val scope = rememberCoroutineScope()

    fun fetchData() {
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val body = withContext(Dispatchers.IO) {
                    val request = Request.Builder()
                        .url("https://api-open.data.gov.sg/v2/real-time/api/weather?api=wbgt")
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
                readings = parseWbgtReadings(body, gson).sortedByDescending { it.wbgt }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Unknown error"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) {
        fetchData()
        while (true) {
            delay(60_000L)
            fetchData()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SG Heat Stress (WBGT)") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { fetchData() }, enabled = !isLoading) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        bottomBar = { BannerAd() },
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) { padding ->
        when {
            isLoading && readings.isEmpty() -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            errorMessage != null && readings.isEmpty() -> {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(errorMessage ?: "", color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { fetchData() }) { Text("Retry") }
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Summary card
                    item {
                        val avgWbgt = if (readings.isNotEmpty()) readings.map { it.wbgt }.average() else 0.0
                        val overall = overallHeatStress(avgWbgt)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Column(Modifier.padding(16.dp)) {
                                Text("Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(8.dp))
                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("%.1f\u00B0C".format(avgWbgt), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                        Text("Avg WBGT", style = MaterialTheme.typography.bodySmall)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(overall, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = heatStressColor(overall))
                                        Text("Heat Stress", style = MaterialTheme.typography.bodySmall)
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("${readings.size}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                        Text("Stations", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }

                    // Bar chart
                    item {
                        val chartData = readings.take(15).map {
                            ChartPoint(it.townCenter.ifEmpty { it.stationName.split(" ").first() }, it.wbgt.toFloat())
                        }
                        if (chartData.isNotEmpty()) {
                            Spacer(Modifier.height(4.dp))
                            Text("WBGT by Station", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            SimpleBarChart(
                                data = chartData,
                                modifier = Modifier.fillMaxWidth().height(200.dp),
                                barColor = Color(0xFFFF6F00),
                                maxBars = 15,
                                yAxisLabel = "\u00B0C"
                            )
                        }
                    }

                    // Station cards
                    item {
                        Spacer(Modifier.height(4.dp))
                        Text("Stations", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    items(readings) { reading ->
                        val color = heatStressColor(reading.heatStress)
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(Modifier.weight(1f)) {
                                    Text(reading.stationName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                    if (reading.townCenter.isNotEmpty()) {
                                        Text(reading.townCenter, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("%.1f\u00B0C".format(reading.wbgt), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                    Surface(shape = MaterialTheme.shapes.small, color = color.copy(alpha = 0.15f)) {
                                        Text(
                                            reading.heatStress,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = color
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item { Spacer(Modifier.height(8.dp)) }
                }
            }
        }
    }
}
