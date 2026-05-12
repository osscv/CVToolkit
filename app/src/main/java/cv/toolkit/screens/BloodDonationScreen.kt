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
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

// --- Data model ---

private data class BloodDonationRecord(
    @SerializedName("date") val date: String?,
    @SerializedName("donations") val donations: Long?,
    @SerializedName("blood_type") val bloodType: String?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BloodDonationScreen(navController: NavController) {
    val scope = rememberCoroutineScope()
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.US) }

    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var records by remember { mutableStateOf<List<BloodDonationRecord>>(emptyList()) }

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
                records = withContext(Dispatchers.IO) {
                    val request = Request.Builder()
                        .url("https://api.data.gov.my/data-catalogue?id=blood_donations&sort=-date&limit=365")
                        .build()
                    val response = client.newCall(request).execute()
                    val body = response.body.string()
                    val type = object : TypeToken<List<BloodDonationRecord>>() {}.type
                    val all: List<BloodDonationRecord> = gson.fromJson(body, type)
                    all.filter { it.bloodType == "all" }
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to fetch data"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) { fetchData() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MY Blood Donations") },
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
                val validRecords = remember(records) {
                    records.filter { it.date != null && it.donations != null }
                        .sortedByDescending { it.date }
                }

                val latestRecord = validRecords.firstOrNull()
                val avg7 = remember(validRecords) {
                    validRecords.take(7).mapNotNull { it.donations }.let { list ->
                        if (list.isNotEmpty()) list.average().toLong() else 0L
                    }
                }
                val avg30 = remember(validRecords) {
                    validRecords.take(30).mapNotNull { it.donations }.let { list ->
                        if (list.isNotEmpty()) list.average().toLong() else 0L
                    }
                }
                val totalDonations = remember(validRecords) {
                    validRecords.sumOf { it.donations ?: 0L }
                }

                // Line chart: last 90 days, oldest first
                val chartLines = remember(validRecords) {
                    val last90 = validRecords.take(90).reversed()
                    listOf(
                        ChartLine(
                            label = "Daily Donations",
                            points = last90.map { r ->
                                val dateStr = r.date ?: ""
                                val label = if (dateStr.length >= 10)
                                    dateStr.substring(5) // MM-DD
                                else dateStr
                                ChartPoint(label = label, value = (r.donations ?: 0L).toFloat())
                            },
                            color = Color(0xFFE53935)
                        )
                    )
                }

                // Monthly aggregation bar chart
                val monthlyBarData = remember(validRecords) {
                    validRecords
                        .groupBy { (it.date ?: "").take(7) } // YYYY-MM
                        .filter { it.key.length == 7 }
                        .map { (month, recs) ->
                            ChartPoint(
                                label = month,
                                value = recs.sumOf { it.donations ?: 0L }.toFloat()
                            )
                        }
                        .sortedBy { it.label }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Summary card
                    item {
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
                                    "Donation Summary",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    StatCard(
                                        label = "Latest",
                                        value = latestRecord?.donations?.let { numberFormat.format(it) } ?: "\u2014",
                                        modifier = Modifier.weight(1f),
                                        subtitle = latestRecord?.date ?: "",
                                        valueColor = Color(0xFFE53935)
                                    )
                                    StatCard(
                                        label = "7-Day Avg",
                                        value = numberFormat.format(avg7),
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    StatCard(
                                        label = "30-Day Avg",
                                        value = numberFormat.format(avg30),
                                        modifier = Modifier.weight(1f)
                                    )
                                    StatCard(
                                        label = "Total (365d)",
                                        value = numberFormat.format(totalDonations),
                                        modifier = Modifier.weight(1f),
                                        valueColor = Color(0xFF1B5E20)
                                    )
                                }
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
                                    "Daily Donation Trend (Last 90 Days)",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(12.dp))
                                SimpleLineChart(
                                    lines = chartLines,
                                    modifier = Modifier.fillMaxWidth(),
                                    yAxisLabel = "Donations"
                                )
                            }
                        }
                    }

                    // Monthly bar chart
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Monthly Total Donations",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(12.dp))
                                SimpleBarChart(
                                    data = monthlyBarData,
                                    modifier = Modifier.fillMaxWidth(),
                                    barColor = Color(0xFFE53935),
                                    maxBars = 12,
                                    yAxisLabel = "Donations"
                                )
                            }
                        }
                    }

                    // Recent records header
                    item {
                        Text(
                            "Recent Records",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Recent records list
                    items(validRecords) { record ->
                        DonationRecordCard(
                            record = record,
                            numberFormat = numberFormat
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DonationRecordCard(
    record: BloodDonationRecord,
    numberFormat: NumberFormat
) {
    val donations = record.donations ?: 0L
    val donationColor = when {
        donations > 1000 -> Color(0xFF4CAF50)
        donations > 500 -> Color(0xFFFFC107)
        else -> Color(0xFFF44336)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                record.date ?: "\u2014",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
            Text(
                numberFormat.format(donations),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = donationColor
            )
        }
    }
}
