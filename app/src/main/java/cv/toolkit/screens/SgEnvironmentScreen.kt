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
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

private data class StationReading(val name: String, val value: Double)

private const val API_KEY = "v2:3628e11212324251b93eccb69f46844aa70b7877ab736bf17f2f75deb7c19c0b:bmc3TaMdW9V-4pdcz5quf7qP-4VCHR2B"

private fun parseReadings(json: String, gson: Gson): List<StationReading> {
    val type = object : TypeToken<Map<String, Any>>() {}.type
    val root: Map<String, Any> = gson.fromJson(json, type)
    val data = root["data"] as? Map<*, *> ?: return emptyList()
    val stations = data["stations"] as? List<*> ?: return emptyList()
    val readings = data["readings"] as? List<*> ?: return emptyList()

    val stationMap = mutableMapOf<String, String>()
    for (s in stations) {
        val sm = s as? Map<*, *> ?: continue
        val id = sm["id"]?.toString() ?: continue
        val name = sm["name"]?.toString() ?: continue
        stationMap[id] = name
    }

    val result = mutableListOf<StationReading>()
    for (r in readings) {
        val rm = r as? Map<*, *> ?: continue
        val stationId = rm["stationId"]?.toString() ?: continue
        val value = (rm["value"] as? Number)?.toDouble() ?: continue
        val name = stationMap[stationId] ?: stationId
        result.add(StationReading(name, value))
    }
    return result
}

private fun shortName(name: String): String = name.split(" ").firstOrNull() ?: name

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SgEnvironmentScreen(navController: NavController) {
    var temperature by remember { mutableStateOf<List<StationReading>>(emptyList()) }
    var rainfall by remember { mutableStateOf<List<StationReading>>(emptyList()) }
    var humidity by remember { mutableStateOf<List<StationReading>>(emptyList()) }
    var windSpeed by remember { mutableStateOf<List<StationReading>>(emptyList()) }
    var windDirection by remember { mutableStateOf<List<StationReading>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val client = remember {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }
    val gson = remember { Gson() }
    val scope = rememberCoroutineScope()

    fun fetchApi(url: String): String {
        val request = Request.Builder()
            .url(url)
            .addHeader("x-api-key", API_KEY)
            .build()
        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw Exception("HTTP ${response.code}")
            response.body.string()
        }
    }

    fun loadData() {
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val base = "https://api-open.data.gov.sg/v2/real-time/api"
                val (tempJson, rainJson, humJson, wsJson, wdJson) = withContext(Dispatchers.IO) {
                    val dTemp = async { fetchApi("$base/air-temperature") }
                    val dRain = async { fetchApi("$base/rainfall") }
                    val dHum = async { fetchApi("$base/relative-humidity") }
                    val dWs = async { fetchApi("$base/wind-speed") }
                    val dWd = async { fetchApi("$base/wind-direction") }
                    listOf(dTemp.await(), dRain.await(), dHum.await(), dWs.await(), dWd.await())
                }
                temperature = parseReadings(tempJson, gson).sortedByDescending { it.value }
                rainfall = parseReadings(rainJson, gson).sortedByDescending { it.value }
                humidity = parseReadings(humJson, gson).sortedByDescending { it.value }
                windSpeed = parseReadings(wsJson, gson).sortedByDescending { it.value }
                windDirection = parseReadings(wdJson, gson)
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to fetch data"
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { loadData() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SG Environment") },
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
        bottomBar = { BannerAd() },
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                errorMessage != null -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(errorMessage ?: "", color = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.height(8.dp))
                            Button(onClick = { loadData() }) { Text("Retry") }
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Summary card
                        item {
                            val avgTemp = if (temperature.isNotEmpty()) temperature.map { it.value }.average() else 0.0
                            val totalRain = rainfall.sumOf { it.value }
                            val avgHumidity = if (humidity.isNotEmpty()) humidity.map { it.value }.average() else 0.0
                            val avgWind = if (windSpeed.isNotEmpty()) windSpeed.map { it.value }.average() else 0.0

                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text("Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(8.dp))
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        SummaryItem("Temp", "%.1f\u00B0C".format(avgTemp), Modifier.weight(1f))
                                        SummaryItem("Rain", "%.1f mm".format(totalRain), Modifier.weight(1f))
                                        SummaryItem("Humidity", "%.0f%%".format(avgHumidity), Modifier.weight(1f))
                                        SummaryItem("Wind", "%.1f kn".format(avgWind), Modifier.weight(1f))
                                    }
                                }
                            }
                        }

                        // Temperature section
                        item {
                            Spacer(Modifier.height(8.dp))
                            Text("Air Temperature", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        item {
                            val chartData = temperature.take(15).map {
                                ChartPoint(shortName(it.name), it.value.toFloat())
                            }
                            if (chartData.isNotEmpty()) {
                                SimpleBarChart(
                                    data = chartData,
                                    modifier = Modifier.fillMaxWidth().height(200.dp),
                                    barColor = Color(0xFFFF6F00),
                                    maxBars = 15,
                                    yAxisLabel = "\u00B0C"
                                )
                            }
                        }
                        items(temperature) { reading ->
                            StationCard(reading.name, "%.1f\u00B0C".format(reading.value))
                        }

                        // Rainfall section
                        item {
                            Spacer(Modifier.height(8.dp))
                            Text("Rainfall", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        item {
                            val chartData = rainfall.take(15).map {
                                ChartPoint(shortName(it.name), it.value.toFloat())
                            }
                            if (chartData.isNotEmpty()) {
                                SimpleBarChart(
                                    data = chartData,
                                    modifier = Modifier.fillMaxWidth().height(200.dp),
                                    barColor = Color(0xFF1976D2),
                                    maxBars = 15,
                                    yAxisLabel = "mm"
                                )
                            }
                        }
                        items(rainfall) { reading ->
                            val color = when {
                                reading.value > 20 -> Color(0xFFD32F2F)
                                reading.value > 5 -> Color(0xFFF57C00)
                                reading.value > 0 -> Color(0xFF1976D2)
                                else -> Color(0xFF388E3C)
                            }
                            StationCard(reading.name, "%.1f mm".format(reading.value), valueColor = color)
                        }

                        // Humidity section
                        item {
                            Spacer(Modifier.height(8.dp))
                            Text("Relative Humidity", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        item {
                            val chartData = humidity.take(15).map {
                                ChartPoint(shortName(it.name), it.value.toFloat())
                            }
                            if (chartData.isNotEmpty()) {
                                SimpleBarChart(
                                    data = chartData,
                                    modifier = Modifier.fillMaxWidth().height(200.dp),
                                    barColor = Color(0xFF0288D1),
                                    maxBars = 15,
                                    yAxisLabel = "%"
                                )
                            }
                        }
                        items(humidity) { reading ->
                            StationCard(reading.name, "%.0f%%".format(reading.value))
                        }

                        // Wind section
                        item {
                            Spacer(Modifier.height(8.dp))
                            Text("Wind", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        item {
                            val chartData = windSpeed.take(15).map {
                                ChartPoint(shortName(it.name), it.value.toFloat())
                            }
                            if (chartData.isNotEmpty()) {
                                SimpleBarChart(
                                    data = chartData,
                                    modifier = Modifier.fillMaxWidth().height(200.dp),
                                    barColor = Color(0xFF546E7A),
                                    maxBars = 15,
                                    yAxisLabel = "knots"
                                )
                            }
                        }
                        items(windSpeed) { reading ->
                            val dir = windDirection.find { it.name == reading.name }
                            val dirStr = if (dir != null) " | ${degreeToCompass(dir.value)} (%.0f\u00B0)".format(dir.value) else ""
                            StationCard(reading.name, "%.1f kn%s".format(reading.value, dirStr))
                        }

                        item { Spacer(Modifier.height(8.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryItem(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun StationCard(
    name: String,
    value: String,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                name,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            Text(
                value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
        }
    }
}

private fun degreeToCompass(degree: Double): String {
    val dirs = listOf("N", "NNE", "NE", "ENE", "E", "ESE", "SE", "SSE",
        "S", "SSW", "SW", "WSW", "W", "WNW", "NW", "NNW")
    val index = ((degree % 360 + 360) % 360 / 22.5 + 0.5).toInt() % 16
    return dirs[index]
}
