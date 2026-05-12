package cv.toolkit.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import cv.toolkit.ads.BannerAd
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

// --- Data models ---

private data class AnnualBirthRecord(
    @SerializedName("date") val date: String?,
    @SerializedName("abs") val abs: Long?,
    @SerializedName("rate") val rate: Double?
)

private data class DeathRecord(
    @SerializedName("date") val date: String?,
    @SerializedName("abs") val abs: Long?,
    @SerializedName("rate") val rate: Double?
)

private data class YearlyData(
    val year: String,
    val births: Long?,
    val birthRate: Double?,
    val deaths: Long?,
    val deathRate: Double?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BirthsDeathsScreen(navController: NavController) {
    val scope = rememberCoroutineScope()
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.US) }

    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var yearlyData by remember { mutableStateOf<List<YearlyData>>(emptyList()) }
    var sortAscending by remember { mutableStateOf(false) }

    val client = remember {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }
    val gson = remember { Gson() }

    fun fetchData() {
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val (births, deaths) = withContext(Dispatchers.IO) {
                    val birthsDeferred = async {
                        val request = Request.Builder()
                            .url("https://api.data.gov.my/data-catalogue?id=births_annual&limit=100")
                            .build()
                        val response = client.newCall(request).execute()
                        val body = response.body.string()
                        val type = object : TypeToken<List<AnnualBirthRecord>>() {}.type
                        gson.fromJson<List<AnnualBirthRecord>>(body, type)
                    }
                    val deathsDeferred = async {
                        val request = Request.Builder()
                            .url("https://api.data.gov.my/data-catalogue?id=deaths&limit=100")
                            .build()
                        val response = client.newCall(request).execute()
                        val body = response.body.string()
                        val type = object : TypeToken<List<DeathRecord>>() {}.type
                        gson.fromJson<List<DeathRecord>>(body, type)
                    }
                    Pair(birthsDeferred.await(), deathsDeferred.await())
                }

                val deathsByYear = deaths.associateBy { it.date }
                yearlyData = births.mapNotNull { b ->
                    b.date?.let { date ->
                        val year = date.take(4)
                        val d = deathsByYear[date]
                        YearlyData(
                            year = year,
                            births = b.abs,
                            birthRate = b.rate,
                            deaths = d?.abs,
                            deathRate = d?.rate
                        )
                    }
                }.sortedByDescending { it.year }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to fetch data"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) { fetchData() }

    val displayData = remember(yearlyData, sortAscending) {
        if (sortAscending) yearlyData.sortedBy { it.year }
        else yearlyData.sortedByDescending { it.year }
    }

    val birthsColor = Color(0xFF4CAF50)
    val deathsColor = Color(0xFFF44336)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MY Births & Deaths") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = {
            BannerAd(modifier = Modifier.fillMaxWidth())
        }
    ) { padding ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            errorMessage ?: "Unknown error",
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { fetchData() }) {
                            Text("Retry")
                        }
                    }
                }
            }

            else -> {
                val latest = yearlyData.firstOrNull()
                val chronological = yearlyData.sortedBy { it.year }

                // Chart lines
                val chartLines = listOf(
                    ChartLine(
                        label = "Births",
                        points = chronological.mapNotNull { d ->
                            d.births?.let { ChartPoint(label = d.year, value = it.toFloat()) }
                        },
                        color = birthsColor
                    ),
                    ChartLine(
                        label = "Deaths",
                        points = chronological.mapNotNull { d ->
                            d.deaths?.let { ChartPoint(label = d.year, value = it.toFloat()) }
                        },
                        color = deathsColor
                    )
                )

                // Bar chart data
                val barData = chronological.mapNotNull { d ->
                    d.births?.let { ChartPoint(label = d.year, value = it.toFloat()) }
                }

                // Summary stats
                val totalBirths = yearlyData.sumOf { it.births ?: 0L }
                val totalDeaths = yearlyData.sumOf { it.deaths ?: 0L }
                val naturalGrowth = totalBirths - totalDeaths

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Summary card — latest year
                    item {
                        if (latest != null) {
                            LatestYearCard(
                                data = latest,
                                numberFormat = numberFormat,
                                birthsColor = birthsColor,
                                deathsColor = deathsColor
                            )
                        }
                    }

                    // Stat cards row
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                StatCard(
                                    label = "Total Births",
                                    value = numberFormat.format(totalBirths),
                                    modifier = Modifier.weight(1f),
                                    valueColor = birthsColor
                                )
                                StatCard(
                                    label = "Total Deaths",
                                    value = numberFormat.format(totalDeaths),
                                    modifier = Modifier.weight(1f),
                                    valueColor = deathsColor
                                )
                                StatCard(
                                    label = "Natural Growth",
                                    value = numberFormat.format(naturalGrowth),
                                    modifier = Modifier.weight(1f),
                                    valueColor = if (naturalGrowth >= 0) birthsColor else deathsColor
                                )
                            }
                        }
                    }

                    // Line chart
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Births vs Deaths Over Time",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(12.dp))
                                SimpleLineChart(
                                    lines = chartLines,
                                    modifier = Modifier.fillMaxWidth(),
                                    yAxisLabel = "People"
                                )
                            }
                        }
                    }

                    // Bar chart
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Annual Births",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(12.dp))
                                SimpleBarChart(
                                    data = barData,
                                    modifier = Modifier.fillMaxWidth(),
                                    barColor = birthsColor,
                                    maxBars = 20,
                                    yAxisLabel = "Births"
                                )
                            }
                        }
                    }

                    // History header with sort toggle
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Yearly Records",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(onClick = { sortAscending = !sortAscending }) {
                                Text(
                                    if (sortAscending) "Year \u2191" else "Year \u2193",
                                    style = MaterialTheme.typography.labelLarge
                                )
                            }
                        }
                    }

                    // History list
                    items(displayData) { data ->
                        BirthsDeathsHistoryCard(
                            data = data,
                            numberFormat = numberFormat,
                            birthsColor = birthsColor,
                            deathsColor = deathsColor
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LatestYearCard(
    data: YearlyData,
    numberFormat: NumberFormat,
    birthsColor: Color,
    deathsColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "Latest Data \u2014 ${data.year}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Births",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        data.births?.let { numberFormat.format(it) } ?: "\u2014",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = birthsColor
                    )
                    data.birthRate?.let { rate ->
                        Text(
                            "Rate: $rate/1k",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                        )
                    }
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "Deaths",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        data.deaths?.let { numberFormat.format(it) } ?: "\u2014",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = deathsColor
                    )
                    data.deathRate?.let { rate ->
                        Text(
                            "Rate: $rate/1k",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                        )
                    }
                }
            }
            if (data.births != null && data.deaths != null) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                )
                Spacer(Modifier.height(12.dp))
                val growth = data.births - data.deaths
                Text(
                    "Natural Growth: ${numberFormat.format(growth)}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = if (growth >= 0) birthsColor else deathsColor
                )
            }
        }
    }
}

@Composable
private fun BirthsDeathsHistoryCard(
    data: YearlyData,
    numberFormat: NumberFormat,
    birthsColor: Color,
    deathsColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                data.year,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Births",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        data.births?.let { numberFormat.format(it) } ?: "\u2014",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = birthsColor
                    )
                    data.birthRate?.let { rate ->
                        Text(
                            "$rate/1k",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Deaths",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        data.deaths?.let { numberFormat.format(it) } ?: "\u2014",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = deathsColor
                    )
                    data.deathRate?.let { rate ->
                        Text(
                            "$rate/1k",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Growth",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    val growth = if (data.births != null && data.deaths != null)
                        data.births - data.deaths else null
                    Text(
                        growth?.let { numberFormat.format(it) } ?: "\u2014",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = when {
                            growth == null -> MaterialTheme.colorScheme.onSurface
                            growth >= 0 -> birthsColor
                            else -> deathsColor
                        }
                    )
                }
            }
        }
    }
}
