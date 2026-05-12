package cv.toolkit.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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

private const val API_KEY = "v2:3628e11212324251b93eccb69f46844aa70b7877ab736bf17f2f75deb7c19c0b:bmc3TaMdW9V-4pdcz5quf7qP-4VCHR2B"
private const val BASE_URL = "https://api-open.data.gov.sg/v2/real-time/api"
private val REGIONS = listOf("west", "east", "central", "south", "north")

private fun uvColor(value: Int): Color = when {
    value <= 2 -> Color(0xFF4CAF50)
    value <= 5 -> Color(0xFFFFC107)
    value <= 7 -> Color(0xFFFF9800)
    value <= 10 -> Color(0xFFF44336)
    else -> Color(0xFF9C27B0)
}

private fun uvLabel(value: Int): String = when {
    value <= 2 -> "Low"
    value <= 5 -> "Moderate"
    value <= 7 -> "High"
    value <= 10 -> "Very High"
    else -> "Extreme"
}

private fun pm25Color(value: Int): Color = when {
    value <= 55 -> Color(0xFF4CAF50)
    value <= 150 -> Color(0xFFFFC107)
    value <= 250 -> Color(0xFFF44336)
    else -> Color(0xFF9C27B0)
}

private fun psiColor(value: Int): Color = when {
    value <= 50 -> Color(0xFF4CAF50)
    value <= 100 -> Color(0xFFFFC107)
    value <= 200 -> Color(0xFFFF9800)
    value <= 300 -> Color(0xFFF44336)
    else -> Color(0xFF9C27B0)
}

private fun psiLabel(value: Int): String = when {
    value <= 50 -> "Good"
    value <= 100 -> "Moderate"
    value <= 200 -> "Unhealthy"
    value <= 300 -> "Very Unhealthy"
    else -> "Hazardous"
}

@Suppress("UNCHECKED_CAST")
private fun extractRegionReadings(json: String, readingKey: String): Map<String, Int> {
    val gson = Gson()
    val type = object : TypeToken<Map<String, Any>>() {}.type
    val root: Map<String, Any> = gson.fromJson(json, type)
    val data = root["data"] as? Map<String, Any> ?: return emptyMap()
    val items = data["items"] as? List<Map<String, Any>> ?: return emptyMap()
    if (items.isEmpty()) return emptyMap()
    val readings = items[0]["readings"] as? Map<String, Any> ?: return emptyMap()
    val values = readings[readingKey] as? Map<String, Any> ?: return emptyMap()
    return values.mapValues { (it.value as? Number)?.toInt() ?: 0 }
}

@Suppress("UNCHECKED_CAST")
private fun extractAllPsiReadings(json: String): Map<String, Map<String, Int>> {
    val gson = Gson()
    val type = object : TypeToken<Map<String, Any>>() {}.type
    val root: Map<String, Any> = gson.fromJson(json, type)
    val data = root["data"] as? Map<String, Any> ?: return emptyMap()
    val items = data["items"] as? List<Map<String, Any>> ?: return emptyMap()
    if (items.isEmpty()) return emptyMap()
    val readings = items[0]["readings"] as? Map<String, Any> ?: return emptyMap()
    val result = mutableMapOf<String, Map<String, Int>>()
    for ((key, value) in readings) {
        val regionMap = value as? Map<String, Any> ?: continue
        result[key] = regionMap.mapValues { (it.value as? Number)?.toInt() ?: 0 }
    }
    return result
}

@Suppress("UNCHECKED_CAST")
private fun extractTimestamp(json: String): String {
    val gson = Gson()
    val type = object : TypeToken<Map<String, Any>>() {}.type
    val root: Map<String, Any> = gson.fromJson(json, type)
    val data = root["data"] as? Map<String, Any> ?: return ""
    val items = data["items"] as? List<Map<String, Any>> ?: return ""
    if (items.isEmpty()) return ""
    return (items[0]["timestamp"] as? String) ?: ""
}

@Suppress("UNCHECKED_CAST")
private fun extractUvData(json: String): List<Pair<String, Int>> {
    val gson = Gson()
    val type = object : TypeToken<Map<String, Any>>() {}.type
    val root: Map<String, Any> = gson.fromJson(json, type)
    val data = root["data"] as? Map<String, Any> ?: return emptyList()
    val records = data["records"] as? List<Map<String, Any>> ?: return emptyList()
    if (records.isEmpty()) return emptyList()
    val indexList = records[0]["index"] as? List<Map<String, Any>> ?: return emptyList()
    return indexList.map { entry ->
        val hour = (entry["hour"] as? String) ?: ""
        val value = (entry["value"] as? Number)?.toInt() ?: 0
        val shortHour = if (hour.length >= 16) hour.substring(11, 16) else hour
        shortHour to value
    }
}

private fun buildRequest(endpoint: String): Request =
    Request.Builder()
        .url("$BASE_URL/$endpoint")
        .addHeader("x-api-key", API_KEY)
        .build()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SgAirQualityScreen(navController: NavController) {
    var pm25Data by remember { mutableStateOf<Map<String, Int>>(emptyMap()) }
    var psiReadings by remember { mutableStateOf<Map<String, Map<String, Int>>>(emptyMap()) }
    var uvHourly by remember { mutableStateOf<List<Pair<String, Int>>>(emptyList()) }
    var timestamp by remember { mutableStateOf("") }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val client = remember {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }
    val scope = rememberCoroutineScope()

    fun loadData() {
        if (loading) return
        scope.launch {
            loading = true
            error = null
            try {
                val (pm25Json, psiJson, uvJson) = withContext(Dispatchers.IO) {
                    val pm25Def = async {
                        client.newCall(buildRequest("pm25")).execute().use { r ->
                            if (!r.isSuccessful) throw Exception("PM2.5: HTTP ${r.code}")
                            r.body.string()
                        }
                    }
                    val psiDef = async {
                        client.newCall(buildRequest("psi")).execute().use { r ->
                            if (!r.isSuccessful) throw Exception("PSI: HTTP ${r.code}")
                            r.body.string()
                        }
                    }
                    val uvDef = async {
                        client.newCall(buildRequest("uv")).execute().use { r ->
                            if (!r.isSuccessful) throw Exception("UV: HTTP ${r.code}")
                            r.body.string()
                        }
                    }
                    Triple(pm25Def.await(), psiDef.await(), uvDef.await())
                }
                pm25Data = extractRegionReadings(pm25Json, "pm25_one_hourly")
                psiReadings = extractAllPsiReadings(psiJson)
                uvHourly = extractUvData(uvJson)
                timestamp = extractTimestamp(pm25Json)
            } catch (e: Exception) {
                error = e.message ?: "Failed to load data"
            }
            loading = false
        }
    }

    LaunchedEffect(Unit) { loadData() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SG Air Quality") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { loadData() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        bottomBar = { BannerAd() }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                loading && pm25Data.isEmpty() -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                error != null && pm25Data.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center).padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(error ?: "Error", color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { loadData() }) { Text("Retry") }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // UV Index Card
                        item {
                            UvIndexCard(uvHourly)
                        }
                        // PM2.5 Card
                        item {
                            RegionCard(
                                title = "PM2.5 (1-hour)",
                                data = pm25Data,
                                colorFn = ::pm25Color,
                                icon = Icons.Default.Air
                            )
                        }
                        // PSI Card
                        item {
                            val psi24 = psiReadings["psi_twenty_four_hourly"] ?: emptyMap()
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Assessment, contentDescription = null, modifier = Modifier.size(20.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("PSI (24-hour)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                    }
                                    Spacer(Modifier.height(12.dp))
                                    REGIONS.forEach { region ->
                                        val value = psi24[region] ?: 0
                                        RegionRow(region, value, psiColor(value), psiLabel(value))
                                    }
                                    if (psi24.isNotEmpty()) {
                                        Spacer(Modifier.height(8.dp))
                                        SimpleBarChart(
                                            data = REGIONS.map { ChartPoint(it.replaceFirstChar { c -> c.uppercase() }, (psi24[it] ?: 0).toFloat()) },
                                            modifier = Modifier.fillMaxWidth(),
                                            barColor = MaterialTheme.colorScheme.tertiary,
                                            yAxisLabel = "PSI"
                                        )
                                    }
                                }
                            }
                        }
                        // Detailed pollutant cards
                        item {
                            DetailedPollutantCards(psiReadings)
                        }
                        // Timestamp
                        if (timestamp.isNotEmpty()) {
                            item {
                                Text(
                                    "Last updated: ${formatTimestamp(timestamp)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.fillMaxWidth().padding(top = 4.dp)
                                )
                            }
                        }
                    }
                    if (loading) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter))
                    }
                }
            }
        }
    }
}

@Composable
private fun UvIndexCard(uvHourly: List<Pair<String, Int>>) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.WbSunny, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text("UV Index", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            if (uvHourly.isNotEmpty()) {
                val latest = uvHourly.last()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${latest.second}",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = uvColor(latest.second)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            uvLabel(latest.second),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = uvColor(latest.second)
                        )
                        Text(
                            "as of ${latest.first}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text("Hourly UV", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                SimpleBarChart(
                    data = uvHourly.map { ChartPoint(it.first, it.second.toFloat()) },
                    modifier = Modifier.fillMaxWidth(),
                    barColor = Color(0xFFFF9800),
                    yAxisLabel = "UV Index"
                )
            } else {
                Text("No UV data available", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun RegionCard(
    title: String,
    data: Map<String, Int>,
    colorFn: (Int) -> Color,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(12.dp))
            REGIONS.forEach { region ->
                val value = data[region] ?: 0
                RegionRow(region, value, colorFn(value))
            }
            if (data.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                SimpleBarChart(
                    data = REGIONS.map { ChartPoint(it.replaceFirstChar { c -> c.uppercase() }, (data[it] ?: 0).toFloat()) },
                    modifier = Modifier.fillMaxWidth(),
                    barColor = MaterialTheme.colorScheme.primary,
                    yAxisLabel = "PM2.5 (ug/m3)"
                )
            }
        }
    }
}

@Composable
private fun RegionRow(region: String, value: Int, color: Color, label: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            region.replaceFirstChar { it.uppercase() },
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "$value",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = color
            )
            if (label != null) {
                Spacer(Modifier.width(8.dp))
                Text(
                    label,
                    style = MaterialTheme.typography.labelSmall,
                    color = color
                )
            }
        }
    }
}

@Composable
private fun DetailedPollutantCards(psiReadings: Map<String, Map<String, Int>>) {
    val pollutants = listOf(
        Triple("SO2 (24-hour)", "so2_twenty_four_hourly", Icons.Default.Cloud),
        Triple("CO (8-hour max)", "co_eight_hour_max", Icons.Default.Factory),
        Triple("NO2 (1-hour max)", "no2_one_hour_max", Icons.Default.Warning),
        Triple("O3 (8-hour max)", "o3_eight_hour_max", Icons.Default.FilterDrama),
        Triple("PM10 (24-hour)", "pm10_twenty_four_hourly", Icons.Default.Grain)
    )
    pollutants.forEach { (title, key, icon) ->
        val data = psiReadings[key]
        if (data != null && data.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.height(8.dp))
                    REGIONS.forEach { region ->
                        val value = data[region] ?: 0
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                region.replaceFirstChar { it.uppercase() },
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                "$value",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun formatTimestamp(ts: String): String {
    return try {
        // "2026-04-05T11:00:00+08:00" -> "2026-04-05 11:00"
        ts.replace("T", " ").take(16)
    } catch (_: Exception) {
        ts
    }
}
