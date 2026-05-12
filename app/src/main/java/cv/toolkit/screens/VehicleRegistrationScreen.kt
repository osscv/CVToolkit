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
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

data class VehicleRegistrationRecord(
    @SerializedName("date") val date: String?,
    @SerializedName("fuel") val fuel: String?,
    @SerializedName("type") val type: String?,
    @SerializedName("registrations") val registrations: Long?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleRegistrationScreen(navController: NavController) {
    val scope = rememberCoroutineScope()
    var records by remember { mutableStateOf<List<VehicleRegistrationRecord>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.US) }

    fun fetchData() {
        isLoading = true
        errorMessage = null
        scope.launch {
            try {
                val data = withContext(Dispatchers.IO) {
                    val client = OkHttpClient.Builder()
                        .connectTimeout(15, TimeUnit.SECONDS)
                        .readTimeout(15, TimeUnit.SECONDS)
                        .writeTimeout(15, TimeUnit.SECONDS)
                        .build()

                    val request = Request.Builder()
                        .url("https://api.data.gov.my/data-catalogue?id=registrations_type_fuel&sort=-date&limit=500")
                        .build()

                    val response = client.newCall(request).execute()
                    val body = response.body.string()
                    val type = object : TypeToken<List<VehicleRegistrationRecord>>() {}.type
                    Gson().fromJson<List<VehicleRegistrationRecord>>(body, type)
                }
                records = data.filter { it.fuel == "all_fuels" && it.type == "all_types" }.sortedBy { it.date }
                isLoading = false
            } catch (e: Exception) {
                errorMessage = e.message ?: "Unknown error occurred"
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) { fetchData() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MY Vehicle Registrations") },
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
                        Text(
                            text = errorMessage ?: "Error",
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(16.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { fetchData() }) {
                            Text("Retry")
                        }
                    }
                }
                records.isEmpty() -> {
                    Text(
                        text = "No data available",
                        modifier = Modifier.align(Alignment.Center),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
                else -> {
                    VehicleRegistrationContent(records, numberFormat)
                }
            }
        }
    }
}

@Composable
private fun VehicleRegistrationContent(
    records: List<VehicleRegistrationRecord>,
    numberFormat: NumberFormat
) {
    val latestRecord = records.last()
    val last12 = records.takeLast(12)
    val average12 = if (last12.isNotEmpty()) last12.map { it.registrations ?: 0L }.average().toLong() else 0L
    val peakRecord = records.maxByOrNull { it.registrations ?: 0L }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Summary cards
        item {
            Text(
                text = "National Overview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    label = "Latest Month",
                    value = numberFormat.format(latestRecord.registrations ?: 0L),
                    modifier = Modifier.weight(1f),
                    subtitle = latestRecord.date ?: ""
                )
                StatCard(
                    label = "12-Month Avg",
                    value = numberFormat.format(average12),
                    modifier = Modifier.weight(1f),
                    subtitle = "Monthly average"
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            if (peakRecord != null) {
                StatCard(
                    label = "Peak Month",
                    value = numberFormat.format(peakRecord.registrations ?: 0L),
                    modifier = Modifier.fillMaxWidth(),
                    subtitle = peakRecord.date ?: ""
                )
            }
        }

        // Line chart - monthly trend
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Monthly Trend",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            val chartPoints = records.map { record ->
                val label = (record.date ?: "").let { d ->
                    if (d.length >= 7) {
                        val parts = d.split("-")
                        "${parts[0].takeLast(2)}-${parts[1]}"
                    } else d
                }
                ChartPoint(label = label, value = (record.registrations ?: 0L).toFloat())
            }
            val line = ChartLine(
                label = "Registrations",
                points = chartPoints,
                color = MaterialTheme.colorScheme.primary
            )
            SimpleLineChart(
                lines = listOf(line),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                yAxisLabel = "Registrations"
            )
        }

        // Bar chart - last 12 months
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Last 12 Months",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            val barData = last12.map { record ->
                val label = (record.date ?: "").let { d ->
                    if (d.length >= 7) {
                        val parts = d.split("-")
                        "${parts[0].takeLast(2)}-${parts[1]}"
                    } else d
                }
                ChartPoint(label, (record.registrations ?: 0L).toFloat())
            }
            SimpleBarChart(
                data = barData,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                barColor = MaterialTheme.colorScheme.primary,
                maxBars = 12,
                yAxisLabel = "Registrations"
            )
        }

        // History list header
        item {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Monthly History",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        // History cards (newest first)
        items(records.reversed()) { record ->
            VehicleRegistrationHistoryCard(record, numberFormat)
        }
    }
}

@Composable
private fun VehicleRegistrationHistoryCard(
    record: VehicleRegistrationRecord,
    numberFormat: NumberFormat
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = record.date ?: "",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "${numberFormat.format(record.registrations ?: 0L)} registrations",
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
