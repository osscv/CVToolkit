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

private data class MarriageRecord(
    val date: String?,
    val abs: Long?,
    val sex: String?,
    val rate: Double?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarriageScreen(navController: NavController) {
    var allRecords by remember { mutableStateOf<List<MarriageRecord>>(emptyList()) }
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
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.US) }

    val bothRecords = remember(allRecords) {
        allRecords
            .filter { it.sex == "female" && it.abs != null }
            .sortedByDescending { it.date ?: "" }
    }

    val latestRecord = remember(bothRecords) { bothRecords.firstOrNull() }

    val averageMarriages = remember(bothRecords) {
        if (bothRecords.isEmpty()) 0L
        else bothRecords.mapNotNull { it.abs }.average().toLong()
    }

    val peakRecord = remember(bothRecords) {
        bothRecords.maxByOrNull { it.abs ?: 0L }
    }

    val chartPoints = remember(bothRecords) {
        bothRecords
            .sortedBy { it.date ?: "" }
            .map { record ->
                ChartPoint(
                    label = record.date?.substring(0, 4) ?: "",
                    value = (record.abs ?: 0L).toFloat()
                )
            }
    }

    fun fetchData() {
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val url = "https://api.data.gov.my/data-catalogue?id=marriages&sort=-date&limit=100"
                val body = withContext(Dispatchers.IO) {
                    val request = Request.Builder().url(url).build()
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) throw Exception("HTTP ${response.code}")
                        response.body.string()
                    }
                }
                val type = object : TypeToken<List<MarriageRecord>>() {}.type
                val records: List<MarriageRecord> = gson.fromJson(body, type)
                allRecords = records
                if (records.isEmpty()) {
                    errorMessage = "No data available"
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to fetch data"
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { fetchData() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MY Marriage Statistics") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = { BannerAd() }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (errorMessage != null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            errorMessage!!,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodyLarge,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { fetchData() }) {
                            Text("Retry")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Summary card
                    if (latestRecord != null) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        "Summary",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        StatCard(
                                            label = "Latest (${latestRecord.date?.substring(0, 4)})",
                                            value = numberFormat.format(latestRecord.abs ?: 0L),
                                            modifier = Modifier.weight(1f),
                                            subtitle = "Marriages"
                                        )
                                        StatCard(
                                            label = "Average/Year",
                                            value = numberFormat.format(averageMarriages),
                                            modifier = Modifier.weight(1f),
                                            subtitle = "Marriages"
                                        )
                                    }
                                    if (peakRecord != null) {
                                        StatCard(
                                            label = "Peak Year (${peakRecord.date?.substring(0, 4)})",
                                            value = numberFormat.format(peakRecord.abs ?: 0L),
                                            modifier = Modifier.fillMaxWidth(),
                                            subtitle = "Highest recorded marriages"
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Line chart
                    if (chartPoints.size >= 2) {
                        item {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        "Marriages Trend Over Time",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    SimpleLineChart(
                                        lines = listOf(
                                            ChartLine(
                                                label = "Marriages",
                                                points = chartPoints,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(200.dp),
                                        yAxisLabel = "Marriages"
                                    )
                                }
                            }
                        }
                    }

                    // History list header
                    if (bothRecords.isNotEmpty()) {
                        item {
                            Text(
                                "Yearly History (${bothRecords.size})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        items(bothRecords, key = { it.date ?: "" }) { record ->
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            record.date?.substring(0, 4) ?: "Unknown",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium
                                        )
                                        if (record.rate != null) {
                                            Text(
                                                "Rate: ${String.format("%.1f", record.rate)} per 1,000",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Text(
                                        numberFormat.format(record.abs ?: 0L),
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
