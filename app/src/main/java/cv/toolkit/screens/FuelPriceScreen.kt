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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

// --- Data model ---

private data class FuelPriceRecord(
    @SerializedName("date") val date: String?,
    @SerializedName("ron95") val ron95: Double?,
    @SerializedName("ron97") val ron97: Double?,
    @SerializedName("diesel") val diesel: Double?,
    @SerializedName("series_type") val seriesType: String?,
    @SerializedName("ron95_budi95") val ron95Budi95: Double?,
    @SerializedName("diesel_eastmsia") val dieselEastMsia: Double?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FuelPriceScreen(navController: NavController) {
    val scope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var levelRecords by remember { mutableStateOf<List<FuelPriceRecord>>(emptyList()) }
    var changeRecords by remember { mutableStateOf<List<FuelPriceRecord>>(emptyList()) }

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
                val records = withContext(Dispatchers.IO) {
                    val request = Request.Builder()
                        .url("https://api.data.gov.my/data-catalogue?id=fuelprice&sort=-date&limit=100")
                        .build()
                    val response = client.newCall(request).execute()
                    val body = response.body.string()
                    val type = object : TypeToken<List<FuelPriceRecord>>() {}.type
                    gson.fromJson<List<FuelPriceRecord>>(body, type)
                }
                levelRecords = records.filter { it.seriesType == "level" }
                changeRecords = records.filter { it.seriesType == "change_weekly" }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to fetch fuel prices"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) { fetchData() }

    // Colors for chart
    val ron95Color = MaterialTheme.colorScheme.primary
    val ron97Color = MaterialTheme.colorScheme.tertiary
    val dieselColor = MaterialTheme.colorScheme.error

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Malaysia Fuel Prices") },
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
                val latest = levelRecords.firstOrNull()
                val latestChange = changeRecords.firstOrNull()

                // Chart data - chronological (oldest first)
                val chartRecords = levelRecords.sortedBy { it.date }
                val chartLines = listOf(
                    ChartLine(
                        label = "RON95",
                        points = chartRecords.mapNotNull { r ->
                            r.date?.let { d ->
                                r.ron95?.let { v ->
                                    ChartPoint(
                                        label = d.takeLast(5), // MM-DD
                                        value = v.toFloat()
                                    )
                                }
                            }
                        },
                        color = ron95Color
                    ),
                    ChartLine(
                        label = "RON97",
                        points = chartRecords.mapNotNull { r ->
                            r.date?.let { d ->
                                r.ron97?.let { v ->
                                    ChartPoint(
                                        label = d.takeLast(5),
                                        value = v.toFloat()
                                    )
                                }
                            }
                        },
                        color = ron97Color
                    ),
                    ChartLine(
                        label = "Diesel",
                        points = chartRecords.mapNotNull { r ->
                            r.date?.let { d ->
                                r.diesel?.let { v ->
                                    ChartPoint(
                                        label = d.takeLast(5),
                                        value = v.toFloat()
                                    )
                                }
                            }
                        },
                        color = dieselColor
                    )
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Current prices card
                    item {
                        if (latest != null) {
                            CurrentPricesCard(
                                record = latest,
                                change = latestChange,
                                ron95Color = ron95Color,
                                ron97Color = ron97Color,
                                dieselColor = dieselColor
                            )
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
                                    "Price Trends",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(12.dp))
                                SimpleLineChart(
                                    lines = chartLines,
                                    modifier = Modifier.fillMaxWidth(),
                                    yAxisLabel = "RM/litre"
                                )
                            }
                        }
                    }

                    // History header
                    item {
                        Text(
                            "Price History",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // History list (already sorted date desc from API)
                    items(levelRecords) { record ->
                        FuelPriceHistoryCard(record = record)
                    }
                }
            }
        }
    }
}

@Composable
private fun CurrentPricesCard(
    record: FuelPriceRecord,
    change: FuelPriceRecord?,
    ron95Color: Color,
    ron97Color: Color,
    dieselColor: Color
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
                "Current Fuel Prices",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Text(
                record.date ?: "",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                FuelPriceItem(
                    label = "RON 95",
                    price = record.ron95,
                    weeklyChange = change?.ron95,
                    color = ron95Color
                )
                FuelPriceItem(
                    label = "RON 97",
                    price = record.ron97,
                    weeklyChange = change?.ron97,
                    color = ron97Color
                )
                FuelPriceItem(
                    label = "Diesel",
                    price = record.diesel,
                    weeklyChange = change?.diesel,
                    color = dieselColor
                )
            }

            // Extra prices if available
            if (record.ron95Budi95 != null || record.dieselEastMsia != null) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    record.ron95Budi95?.let {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "BUDI (RON 95)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            Text(
                                "RM %.2f".format(it),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                    record.dieselEastMsia?.let {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Diesel (East Msia)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                            Text(
                                "RM %.2f".format(it),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FuelPriceItem(
    label: String,
    price: Double?,
    weeklyChange: Double?,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
        )
        Spacer(Modifier.height(4.dp))
        Text(
            if (price != null) "RM %.2f".format(price) else "—",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Spacer(Modifier.height(2.dp))
        if (weeklyChange != null) {
            val arrow = when {
                weeklyChange > 0 -> "\u25B2"  // ▲
                weeklyChange < 0 -> "\u25BC"  // ▼
                else -> "\u2014"               // —
            }
            val changeColor = when {
                weeklyChange > 0 -> Color(0xFF4CAF50) // green
                weeklyChange < 0 -> Color(0xFFF44336) // red
                else -> MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
            }
            Text(
                "$arrow %.2f".format(weeklyChange),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = changeColor
            )
        }
    }
}

@Composable
private fun FuelPriceHistoryCard(record: FuelPriceRecord) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                record.date ?: "Unknown date",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                HistoryPriceColumn("RON 95", record.ron95)
                HistoryPriceColumn("RON 97", record.ron97)
                HistoryPriceColumn("Diesel", record.diesel)
            }
            if (record.ron95Budi95 != null || record.dieselEastMsia != null) {
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    HistoryPriceColumn("BUDI 95", record.ron95Budi95)
                    HistoryPriceColumn("Diesel (East)", record.dieselEastMsia)
                    Spacer(Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun RowScope.HistoryPriceColumn(label: String, price: Double?) {
    Column(
        modifier = Modifier.weight(1f),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            if (price != null) "RM %.2f".format(price) else "—",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
