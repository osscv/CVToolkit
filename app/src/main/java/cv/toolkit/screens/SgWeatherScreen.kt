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
import androidx.compose.ui.text.style.TextAlign
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

private val client = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(15, TimeUnit.SECONDS)
    .writeTimeout(15, TimeUnit.SECONDS)
    .build()

private val gson = Gson()

private fun fetchJson(endpoint: String): Map<String, Any?> {
    val request = Request.Builder()
        .url("$BASE_URL/$endpoint")
        .addHeader("x-api-key", API_KEY)
        .build()
    val response = client.newCall(request).execute()
    val body = response.body.string()
    val type = object : TypeToken<Map<String, Any?>>() {}.type
    return gson.fromJson(body, type)
}

@Suppress("UNCHECKED_CAST")
private fun Map<String, Any?>.nested(vararg keys: String): Any? {
    var current: Any? = this
    for (key in keys) {
        current = (current as? Map<String, Any?>)?.get(key) ?: return null
    }
    return current
}

private fun Any?.asString(): String = this?.toString() ?: "N/A"
private fun Any?.asInt(): Int = (this as? Number)?.toInt() ?: 0

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SgWeatherScreen(navController: NavController) {
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var twentyFourHr by remember { mutableStateOf<Map<String, Any?>?>(null) }
    var fourDay by remember { mutableStateOf<Map<String, Any?>?>(null) }
    var twoHr by remember { mutableStateOf<Map<String, Any?>?>(null) }

    val scope = rememberCoroutineScope()

    fun loadData() {
        isLoading = true
        errorMessage = null
        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    val d1 = async { fetchJson("twenty-four-hr-forecast") }
                    val d2 = async { fetchJson("four-day-outlook") }
                    val d3 = async { fetchJson("two-hr-forecast") }
                    Triple(d1.await(), d2.await(), d3.await())
                }
                twentyFourHr = result.first
                fourDay = result.second
                twoHr = result.third
                isLoading = false
            } catch (e: Exception) {
                errorMessage = e.message ?: "Unknown error"
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) { loadData() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SG Weather") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                errorMessage != null -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(errorMessage ?: "", textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { loadData() }) { Text("Retry") }
                    }
                }
                else -> {
                    WeatherContent(twentyFourHr, twoHr, fourDay)
                }
            }
        }
    }
}

@Suppress("UNCHECKED_CAST")
@Composable
private fun WeatherContent(
    twentyFourHr: Map<String, Any?>?,
    twoHr: Map<String, Any?>?,
    fourDay: Map<String, Any?>?
) {
    val record = (twentyFourHr?.nested("data", "records") as? List<Map<String, Any?>>)?.firstOrNull()
    val general = record?.get("general") as? Map<String, Any?>
    val periods = record?.get("periods") as? List<Map<String, Any?>>

    val twoHrRecords = (twoHr?.nested("data", "records") as? List<Map<String, Any?>>)?.firstOrNull()
    val areaForecasts = twoHrRecords?.get("forecasts") as? List<Map<String, Any?>>

    val fourDayRecords = (fourDay?.nested("data", "records") as? List<Map<String, Any?>>)?.firstOrNull()
    val fourDayForecasts = fourDayRecords?.get("forecasts") as? List<Map<String, Any?>>

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. Current overview card
        item {
            CurrentOverviewCard(general)
        }

        // 2. Two-hour forecast by area
        item {
            TwoHourForecastSection(areaForecasts)
        }

        // 3. Today's periods
        if (!periods.isNullOrEmpty()) {
            item {
                Text(
                    "Today's Forecast by Period",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            items(periods) { period ->
                PeriodCard(period)
            }
        }

        // 4. Four-day outlook
        if (!fourDayForecasts.isNullOrEmpty()) {
            item {
                Text(
                    "4-Day Outlook",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }
            items(fourDayForecasts) { forecast ->
                FourDayCard(forecast)
            }
        }
    }
}

@Suppress("UNCHECKED_CAST")
@Composable
private fun CurrentOverviewCard(general: Map<String, Any?>?) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Current Overview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            val tempLow = general?.nested("temperature", "low").asInt()
            val tempHigh = general?.nested("temperature", "high").asInt()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Thermostat, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Temperature: ${tempLow}\u00B0C - ${tempHigh}\u00B0C", style = MaterialTheme.typography.bodyLarge)
            }
            Spacer(modifier = Modifier.height(8.dp))

            val humLow = general?.nested("relativeHumidity", "low").asInt()
            val humHigh = general?.nested("relativeHumidity", "high").asInt()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.WaterDrop, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Humidity: ${humLow}% - ${humHigh}%", style = MaterialTheme.typography.bodyLarge)
            }
            Spacer(modifier = Modifier.height(8.dp))

            val forecast = general?.nested("forecast", "text").asString()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Cloud, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Forecast: $forecast", style = MaterialTheme.typography.bodyLarge)
            }
            Spacer(modifier = Modifier.height(8.dp))

            val windLow = general?.nested("wind", "speed", "low").asInt()
            val windHigh = general?.nested("wind", "speed", "high").asInt()
            val windDir = general?.nested("wind", "direction").asString()
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Air, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Wind: ${windLow}-${windHigh} km/h ($windDir)", style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}

@Suppress("UNCHECKED_CAST")
@Composable
private fun TwoHourForecastSection(areaForecasts: List<Map<String, Any?>>?) {
    Column {
        Text(
            "2-Hour Forecast by Area",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        if (areaForecasts.isNullOrEmpty()) {
            Text("No area forecast data available.", style = MaterialTheme.typography.bodyMedium)
        } else {
            @Composable
            fun weatherChipColor(forecast: String): Color {
                return when {
                    "thundery" in forecast.lowercase() -> Color(0xFFFF8F00)
                    "showers" in forecast.lowercase() || "rain" in forecast.lowercase() -> Color(0xFF42A5F5)
                    "cloudy" in forecast.lowercase() -> Color(0xFF90A4AE)
                    "fair" in forecast.lowercase() || "sunny" in forecast.lowercase() -> Color(0xFFFDD835)
                    "hazy" in forecast.lowercase() -> Color(0xFFBCAAA4)
                    else -> Color(0xFF78909C)
                }
            }

            val columns = 2
            val rows = areaForecasts.chunked(columns)
            for (row in rows) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    for (item in row) {
                        val area = item["area"].asString()
                        val forecast = item["forecast"].asString()
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .padding(vertical = 4.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    area,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                SuggestionChip(
                                    onClick = {},
                                    label = {
                                        Text(
                                            forecast,
                                            style = MaterialTheme.typography.labelSmall,
                                            maxLines = 1
                                        )
                                    },
                                    colors = SuggestionChipDefaults.suggestionChipColors(
                                        containerColor = weatherChipColor(forecast).copy(alpha = 0.25f)
                                    )
                                )
                            }
                        }
                    }
                    if (row.size < columns) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Suppress("UNCHECKED_CAST")
@Composable
private fun PeriodCard(period: Map<String, Any?>) {
    val timePeriod = (period["timePeriod"] as? Map<String, Any?>)?.get("text").asString()
    val regions = period["regions"] as? Map<String, Any?> ?: emptyMap()

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                timePeriod,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            for ((region, data) in regions) {
                val forecast = (data as? Map<String, Any?>)?.get("text").asString()
                Row(modifier = Modifier.padding(vertical = 2.dp)) {
                    Text(
                        "${region.replaceFirstChar { it.uppercase() }}:",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.width(72.dp)
                    )
                    Text(forecast, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Suppress("UNCHECKED_CAST")
@Composable
private fun FourDayCard(forecast: Map<String, Any?>) {
    val day = forecast["day"].asString()
    val text = forecast.nested("forecast", "text").asString()
    val tempLow = forecast.nested("temperature", "low").asInt()
    val tempHigh = forecast.nested("temperature", "high").asInt()
    val windLow = forecast.nested("wind", "speed", "low").asInt()
    val windHigh = forecast.nested("wind", "speed", "high").asInt()
    val windDir = forecast.nested("wind", "direction").asString()

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(day, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text, style = MaterialTheme.typography.bodyLarge)
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Thermostat, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${tempLow}\u00B0C - ${tempHigh}\u00B0C", style = MaterialTheme.typography.bodyMedium)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Air, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("${windLow}-${windHigh} km/h $windDir", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}
