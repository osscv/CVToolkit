package cv.toolkit.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import cv.toolkit.ads.BannerAd
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

// --- Data models ---

private data class ForecastLocation(
    @SerializedName("location_id") val locationId: String?,
    @SerializedName("location_name") val locationName: String?
)

private data class ForecastItem(
    @SerializedName("location") val location: ForecastLocation?,
    @SerializedName("date") val date: String?,
    @SerializedName("morning_forecast") val morningForecast: String?,
    @SerializedName("afternoon_forecast") val afternoonForecast: String?,
    @SerializedName("night_forecast") val nightForecast: String?,
    @SerializedName("summary_forecast") val summaryForecast: String?,
    @SerializedName("summary_when") val summaryWhen: String?,
    @SerializedName("min_temp") val minTemp: Int?,
    @SerializedName("max_temp") val maxTemp: Int?
)

private data class WarningIssue(
    @SerializedName("issued") val issued: String?,
    @SerializedName("title_bm") val titleBm: String?,
    @SerializedName("title_en") val titleEn: String?
)

private data class WarningItem(
    @SerializedName("warning_issue") val warningIssue: WarningIssue?,
    @SerializedName("valid_from") val validFrom: String?,
    @SerializedName("valid_to") val validTo: String?,
    @SerializedName("heading_en") val headingEn: String?,
    @SerializedName("text_en") val textEn: String?,
    @SerializedName("instruction_en") val instructionEn: String?,
    @SerializedName("heading_bm") val headingBm: String?,
    @SerializedName("text_bm") val textBm: String?,
    @SerializedName("instruction_bm") val instructionBm: String?
)

private data class EarthquakeItem(
    @SerializedName("utcdatetime") val utcDatetime: String?,
    @SerializedName("localdatetime") val localDatetime: String?,
    @SerializedName("lat") val lat: Double?,
    @SerializedName("lon") val lon: Double?,
    @SerializedName("depth") val depth: Double?,
    @SerializedName("location") val location: String?,
    @SerializedName("location_original") val locationOriginal: String?,
    @SerializedName("magdefault") val magnitude: Double?,
    @SerializedName("magtypedefault") val magnitudeType: String?,
    @SerializedName("status") val status: String?
)

private const val BASE_URL = "https://api.data.gov.my"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherScreen(navController: NavController) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Forecast", "Warnings", "Earthquakes")

    // Forecast state
    var forecasts by remember { mutableStateOf<List<ForecastItem>>(emptyList()) }
    var forecastLoading by remember { mutableStateOf(false) }
    var forecastError by remember { mutableStateOf<String?>(null) }
    var forecastLoaded by remember { mutableStateOf(false) }

    // Warning state
    var warnings by remember { mutableStateOf<List<WarningItem>>(emptyList()) }
    var warningLoading by remember { mutableStateOf(false) }
    var warningError by remember { mutableStateOf<String?>(null) }
    var warningLoaded by remember { mutableStateOf(false) }

    // Earthquake state
    var earthquakes by remember { mutableStateOf<List<EarthquakeItem>>(emptyList()) }
    var earthquakeLoading by remember { mutableStateOf(false) }
    var earthquakeError by remember { mutableStateOf<String?>(null) }
    var earthquakeLoaded by remember { mutableStateOf(false) }

    // Search state for forecast
    var searchQuery by remember { mutableStateOf("") }

    val client = remember {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    val gson = remember { Gson() }
    val scope = rememberCoroutineScope()

    fun loadForecasts() {
        if (forecastLoading) return
        scope.launch {
            forecastLoading = true
            forecastError = null
            try {
                val body = withContext(Dispatchers.IO) {
                    val request = Request.Builder()
                        .url("$BASE_URL/weather/forecast?limit=500")
                        .build()
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) throw Exception("HTTP ${response.code}")
                        response.body.string()
                    }
                }
                val type = object : TypeToken<List<ForecastItem>>() {}.type
                forecasts = gson.fromJson(body, type)
                forecastLoaded = true
            } catch (e: Exception) {
                forecastError = e.message ?: "Failed to load forecasts"
            }
            forecastLoading = false
        }
    }

    fun loadWarnings() {
        if (warningLoading) return
        scope.launch {
            warningLoading = true
            warningError = null
            try {
                val body = withContext(Dispatchers.IO) {
                    val request = Request.Builder()
                        .url("$BASE_URL/weather/warning")
                        .build()
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) throw Exception("HTTP ${response.code}")
                        response.body.string()
                    }
                }
                val type = object : TypeToken<List<WarningItem>>() {}.type
                warnings = gson.fromJson(body, type)
                warningLoaded = true
            } catch (e: Exception) {
                warningError = e.message ?: "Failed to load warnings"
            }
            warningLoading = false
        }
    }

    fun loadEarthquakes() {
        if (earthquakeLoading) return
        scope.launch {
            earthquakeLoading = true
            earthquakeError = null
            try {
                val body = withContext(Dispatchers.IO) {
                    val request = Request.Builder()
                        .url("$BASE_URL/weather/warning/earthquake?limit=50")
                        .build()
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) throw Exception("HTTP ${response.code}")
                        response.body.string()
                    }
                }
                val type = object : TypeToken<List<EarthquakeItem>>() {}.type
                earthquakes = gson.fromJson(body, type)
                earthquakeLoaded = true
            } catch (e: Exception) {
                earthquakeError = e.message ?: "Failed to load earthquakes"
            }
            earthquakeLoading = false
        }
    }

    // Load data for the selected tab
    LaunchedEffect(selectedTab) {
        when (selectedTab) {
            0 -> if (!forecastLoaded && !forecastLoading) loadForecasts()
            1 -> if (!warningLoaded && !warningLoading) loadWarnings()
            2 -> if (!earthquakeLoaded && !earthquakeLoading) loadEarthquakes()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MY Weather") },
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
        ) {
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> ForecastTab(
                        forecasts = forecasts,
                        isLoading = forecastLoading,
                        error = forecastError,
                        searchQuery = searchQuery,
                        onSearchChange = { searchQuery = it },
                        onRetry = { forecastLoaded = false; loadForecasts() }
                    )
                    1 -> WarningTab(
                        warnings = warnings,
                        isLoading = warningLoading,
                        error = warningError,
                        onRetry = { warningLoaded = false; loadWarnings() }
                    )
                    2 -> EarthquakeTab(
                        earthquakes = earthquakes,
                        isLoading = earthquakeLoading,
                        error = earthquakeError,
                        onRetry = { earthquakeLoaded = false; loadEarthquakes() }
                    )
                }
            }

            BannerAd(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ForecastTab(
    forecasts: List<ForecastItem>,
    isLoading: Boolean,
    error: String?,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onRetry: () -> Unit
) {
    val filtered = remember(forecasts, searchQuery) {
        if (searchQuery.isBlank()) forecasts
        else forecasts.filter {
            it.location?.locationName?.contains(searchQuery, ignoreCase = true) == true
        }
    }

    // Group by location
    val grouped = remember(filtered) {
        filtered.groupBy { it.location?.locationName ?: "Unknown" }
            .toSortedMap()
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            placeholder = { Text("Search location...") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Filled.Clear, "Clear")
                    }
                }
            },
            singleLine = true,
            shape = MaterialTheme.shapes.extraLarge
        )

        when {
            isLoading -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            }
            error != null -> ErrorCard(error, onRetry)
            grouped.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No forecasts found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    grouped.forEach { (locationName, items) ->
                        item(key = "header_$locationName") {
                            Text(
                                locationName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                            )
                        }
                        items(items, key = { "${it.location?.locationId}_${it.date}" }) { forecast ->
                            ForecastCard(forecast)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ForecastCard(item: ForecastItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    item.date ?: "-",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                if (item.minTemp != null && item.maxTemp != null) {
                    Text(
                        "${item.minTemp}°C - ${item.maxTemp}°C",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            if (item.summaryForecast != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.WbSunny, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "${item.summaryForecast}${if (item.summaryWhen != null) " (${item.summaryWhen})" else ""}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Spacer(Modifier.height(4.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item.morningForecast?.let { forecast ->
                    ForecastChip("AM", forecast, Modifier.weight(1f))
                }
                item.afternoonForecast?.let { forecast ->
                    ForecastChip("PM", forecast, Modifier.weight(1f))
                }
                item.nightForecast?.let { forecast ->
                    ForecastChip("Night", forecast, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun ForecastChip(period: String, forecast: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = MaterialTheme.shapes.small
    ) {
        Column(modifier = Modifier.padding(6.dp)) {
            Text(period, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold)
            Text(
                forecast,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun WarningTab(
    warnings: List<WarningItem>,
    isLoading: Boolean,
    error: String?,
    onRetry: () -> Unit
) {
    when {
        isLoading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        error != null -> ErrorCard(error, onRetry)
        warnings.isEmpty() -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Filled.CheckCircle, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.height(8.dp))
                    Text("No active warnings", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
        else -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(warnings, key = { "${it.warningIssue?.issued}_${it.headingEn}" }) { warning ->
                    WarningCard(warning)
                }
            }
        }
    }
}

@Composable
private fun WarningCard(item: WarningItem) {
    val isNoAdvisory = item.headingEn?.contains("No Advisory", ignoreCase = true) == true

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isNoAdvisory)
                MaterialTheme.colorScheme.surface
            else
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    if (isNoAdvisory) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                    null,
                    Modifier.size(20.dp),
                    tint = if (isNoAdvisory) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    item.warningIssue?.titleEn ?: item.headingEn ?: "Warning",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(Modifier.height(8.dp))

            item.textEn?.let { text ->
                Text(text, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(4.dp))
            }

            item.instructionEn?.let { instruction ->
                Text(
                    instruction,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
                Spacer(Modifier.height(4.dp))
            }

            if (item.validFrom != null || item.validTo != null) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    item.validFrom?.let {
                        Text("From: ${it.replace("T", " ")}", style = MaterialTheme.typography.labelSmall)
                    }
                    item.validTo?.let {
                        Text("To: ${it.replace("T", " ")}", style = MaterialTheme.typography.labelSmall)
                    }
                }
            }

            item.warningIssue?.issued?.let { issued ->
                Text(
                    "Issued: ${issued.replace("T", " ")}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EarthquakeTab(
    earthquakes: List<EarthquakeItem>,
    isLoading: Boolean,
    error: String?,
    onRetry: () -> Unit
) {
    when {
        isLoading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        error != null -> ErrorCard(error, onRetry)
        earthquakes.isEmpty() -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No earthquake data", style = MaterialTheme.typography.titleMedium)
            }
        }
        else -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(earthquakes, key = { "${it.utcDatetime}_${it.lat}_${it.lon}" }) { quake ->
                    EarthquakeCard(quake)
                }
            }
        }
    }
}

@Composable
private fun EarthquakeCard(item: EarthquakeItem) {
    val magnitudeColor = when {
        (item.magnitude ?: 0.0) >= 7.0 -> MaterialTheme.colorScheme.error
        (item.magnitude ?: 0.0) >= 5.0 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            // Magnitude badge
            Surface(
                color = magnitudeColor.copy(alpha = 0.15f),
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.size(56.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            String.format("%.1f", item.magnitude ?: 0.0),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = magnitudeColor
                        )
                        Text(
                            item.magnitudeType ?: "",
                            style = MaterialTheme.typography.labelSmall,
                            color = magnitudeColor
                        )
                    }
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    item.locationOriginal ?: item.location ?: "Unknown",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(4.dp))

                item.localDatetime?.let { dt ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Schedule, null, Modifier.size(14.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(Modifier.width(4.dp))
                        Text(
                            dt.replace("T", " "),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (item.depth != null) {
                        Text(
                            "Depth: ${String.format("%.1f", item.depth)} km",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (item.lat != null && item.lon != null) {
                        Text(
                            "${String.format("%.2f", item.lat)}°, ${String.format("%.2f", item.lon)}°",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ErrorCard(error: String, onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Filled.Warning, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
                Text(error, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                Spacer(Modifier.height(12.dp))
                FilledTonalButton(onClick = onRetry) {
                    Text("Retry")
                }
            }
        }
    }
}
