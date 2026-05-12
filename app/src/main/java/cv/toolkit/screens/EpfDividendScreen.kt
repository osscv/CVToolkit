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

private data class EpfDividendRecord(
    @SerializedName("date") val date: String?,
    @SerializedName("shariah") val shariah: Double?,
    @SerializedName("conventional") val conventional: Double?
) {
    val year: String get() = date?.substring(0, 4) ?: ""
}

private const val API_URL =
    "https://api.data.gov.my/data-catalogue?id=epf_dividend&sort=-date&limit=100"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EpfDividendScreen(navController: NavController) {
    var records by remember { mutableStateOf<List<EpfDividendRecord>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    val client = remember {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }
    val gson = remember { Gson() }
    val scope = rememberCoroutineScope()

    fun loadData() {
        if (isLoading) return
        scope.launch {
            isLoading = true
            error = null
            try {
                val body = withContext(Dispatchers.IO) {
                    val request = Request.Builder().url(API_URL).build()
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) throw Exception("HTTP ${response.code}")
                        response.body.string()
                    }
                }
                val type = object : TypeToken<List<EpfDividendRecord>>() {}.type
                records = gson.fromJson(body, type)
            } catch (e: Exception) {
                error = e.message ?: "Failed to load data"
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { loadData() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MY EPF Dividend") },
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
        bottomBar = { BannerAd(modifier = Modifier.fillMaxWidth()) },
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) { paddingValues ->
        when {
            isLoading && records.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            error != null && records.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(error!!, color = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { loadData() }) { Text("Retry") }
                    }
                }
            }
            else -> {
                EpfDividendContent(
                    records = records,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
        }
    }
}

@Composable
private fun EpfDividendContent(records: List<EpfDividendRecord>, modifier: Modifier = Modifier) {
    // Records are already sorted descending by date from API
    val latest = records.firstOrNull()
    val conventionalRates = records.mapNotNull { it.conventional }
    val shariahRates = records.mapNotNull { it.shariah }
    val avgConventional = if (conventionalRates.isNotEmpty()) conventionalRates.average() else 0.0
    val avgShariah = if (shariahRates.isNotEmpty()) shariahRates.average() else 0.0
    val highestRate = (conventionalRates + shariahRates).maxOrNull() ?: 0.0

    // Chart data (oldest first)
    val chronological = records.reversed()
    val conventionalLine = ChartLine(
        label = "Conventional",
        points = chronological.mapNotNull { record ->
            record.conventional?.let { ChartPoint(record.year, it.toFloat()) }
        },
        color = Color(0xFF1565C0)
    )
    val shariahLine = ChartLine(
        label = "Shariah",
        points = chronological.mapNotNull { record ->
            record.shariah?.let { ChartPoint(record.year, it.toFloat()) }
        },
        color = Color(0xFF2E7D32)
    )

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Latest rates card
        if (latest != null) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "Latest Rates (${latest.year})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "Conventional",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                                Text(
                                    latest.conventional?.let { String.format("%.2f%%", it) } ?: "-",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "Shariah",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                                Text(
                                    latest.shariah?.let { String.format("%.2f%%", it) } ?: "-",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }
        }

        // Summary stats
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatCard(
                    label = "Avg Conventional",
                    value = String.format("%.2f%%", avgConventional),
                    modifier = Modifier.weight(1f),
                    subtitle = "All time"
                )
                StatCard(
                    label = "Avg Shariah",
                    value = String.format("%.2f%%", avgShariah),
                    modifier = Modifier.weight(1f),
                    subtitle = "All time"
                )
                StatCard(
                    label = "Highest Ever",
                    value = String.format("%.2f%%", highestRate),
                    modifier = Modifier.weight(1f),
                    subtitle = "Any type"
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
                        "Dividend Rates Over Time",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    SimpleLineChart(
                        lines = listOf(conventionalLine, shariahLine),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        yAxisLabel = "%"
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ChartLegendDot(Color(0xFF1565C0), "Conventional")
                        Spacer(Modifier.width(16.dp))
                        ChartLegendDot(Color(0xFF2E7D32), "Shariah")
                    }
                }
            }
        }

        // History header
        item {
            Text(
                "History",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        // History cards
        items(records) { record ->
            EpfYearCard(record)
        }
    }
}

@Composable
private fun EpfYearCard(yd: EpfDividendRecord) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                yd.year,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                yd.conventional?.let { rate ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Conventional",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            String.format("%.2f%%", rate),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = rateColor(rate)
                        )
                    }
                }
                yd.shariah?.let { rate ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Shariah",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            String.format("%.2f%%", rate),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = rateColor(rate)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChartLegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            modifier = Modifier.size(10.dp),
            shape = MaterialTheme.shapes.small,
            color = color
        ) {}
        Spacer(Modifier.width(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall)
    }
}

private fun rateColor(rate: Double): Color = when {
    rate >= 5.0 -> Color(0xFF2E7D32)
    rate >= 4.0 -> Color(0xFFF9A825)
    else -> Color(0xFFC62828)
}
