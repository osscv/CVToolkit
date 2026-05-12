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
import com.google.gson.reflect.TypeToken
import cv.toolkit.ads.BannerAd
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

private data class InterestRateRecord(
    val bank: String? = null,
    val date: String? = null,
    val rate: String? = null,
    val value: Double? = null
)

private data class MonthlyRates(
    val date: String,
    val opr: Double?,
    val baseRate: Double?,
    val fd12m: Double?,
    val savings: Double?
)

private val KEY_RATE_TYPES = listOf("opr", "br", "fdr_12mo", "savings")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InterestRateScreen(navController: NavController) {
    val scope = rememberCoroutineScope()
    var records by remember { mutableStateOf<List<InterestRateRecord>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    fun fetchData() {
        isLoading = true
        error = null
        scope.launch {
            try {
                val data = withContext(Dispatchers.IO) {
                    val client = OkHttpClient.Builder()
                        .connectTimeout(15, TimeUnit.SECONDS)
                        .readTimeout(15, TimeUnit.SECONDS)
                        .build()
                    val request = Request.Builder()
                        .url("https://api.data.gov.my/data-catalogue?id=interestrates&sort=-date&limit=120")
                        .build()
                    val response = client.newCall(request).execute()
                    val body = response.body.string()
                    val type = object : TypeToken<List<InterestRateRecord>>() {}.type
                    Gson().fromJson<List<InterestRateRecord>>(body, type)
                }
                records = data
                isLoading = false
            } catch (e: Exception) {
                error = e.message ?: "Failed to fetch interest rates"
                isLoading = false
            }
        }
    }

    LaunchedEffect(Unit) { fetchData() }

    // Filter for commercial bank and key variables
    val commercialRecords = remember(records) {
        records.filter { it.bank == "commercial" && it.rate in KEY_RATE_TYPES }
    }

    // Group by date into MonthlyRates (chronological - oldest first)
    val monthlyRates = remember(commercialRecords) {
        commercialRecords
            .groupBy { it.date ?: "" }
            .map { (date, recs) ->
                MonthlyRates(
                    date = date,
                    opr = recs.find { it.rate == "opr" }?.value,
                    baseRate = recs.find { it.rate == "br" }?.value,
                    fd12m = recs.find { it.rate == "fdr_12mo" }?.value,
                    savings = recs.find { it.rate == "savings" }?.value
                )
            }
            .sortedBy { it.date }
    }

    val latestRates = remember(monthlyRates) { monthlyRates.lastOrNull() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MY Interest Rates") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = { BannerAd() }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (error != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Error", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(error!!, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { fetchData() }) {
                        Text("Retry")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Current rates card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Current Rates",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            if (latestRates != null) {
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    latestRates.date,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                                Spacer(Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    StatCard(
                                        label = "OPR",
                                        value = latestRates.opr?.let { "${String.format("%.2f", it)}%" } ?: "-",
                                        modifier = Modifier.weight(1f)
                                    )
                                    StatCard(
                                        label = "Base Rate",
                                        value = latestRates.baseRate?.let { "${String.format("%.2f", it)}%" } ?: "-",
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    StatCard(
                                        label = "FD 12M",
                                        value = latestRates.fd12m?.let { "${String.format("%.2f", it)}%" } ?: "-",
                                        modifier = Modifier.weight(1f)
                                    )
                                    StatCard(
                                        label = "Savings",
                                        value = latestRates.savings?.let { "${String.format("%.2f", it)}%" } ?: "-",
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            } else {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "No data available",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }

                // OPR trend chart
                val oprPoints = monthlyRates.mapNotNull { m ->
                    m.opr?.let { ChartPoint(formatRateDate(m.date), it.toFloat()) }
                }
                if (oprPoints.size >= 2) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "OPR Trend",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(8.dp))
                                SimpleLineChart(
                                    lines = listOf(
                                        ChartLine(
                                            label = "OPR",
                                            points = oprPoints,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    ),
                                    modifier = Modifier.fillMaxWidth(),
                                    yAxisLabel = "Rate (%)"
                                )
                            }
                        }
                    }
                }

                // Multi-line comparison chart
                val baseRatePoints = monthlyRates.mapNotNull { m ->
                    m.baseRate?.let { ChartPoint(formatRateDate(m.date), it.toFloat()) }
                }
                val fd12mPoints = monthlyRates.mapNotNull { m ->
                    m.fd12m?.let { ChartPoint(formatRateDate(m.date), it.toFloat()) }
                }
                if (oprPoints.size >= 2 && baseRatePoints.size >= 2) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Rate Comparison",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(8.dp))
                                val lines = mutableListOf(
                                    ChartLine("OPR", oprPoints, Color(0xFF1976D2)),
                                    ChartLine("Base Rate", baseRatePoints, Color(0xFF388E3C))
                                )
                                if (fd12mPoints.size >= 2) {
                                    lines.add(ChartLine("FD 12M", fd12mPoints, Color(0xFFF57C00)))
                                }
                                SimpleLineChart(
                                    lines = lines,
                                    modifier = Modifier.fillMaxWidth(),
                                    yAxisLabel = "Rate (%)"
                                )
                            }
                        }
                    }
                }

                // History header
                if (monthlyRates.isNotEmpty()) {
                    item {
                        Text(
                            "Monthly History",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // History list (newest first)
                    items(monthlyRates.reversed()) { monthly ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                Text(
                                    monthly.date,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    RateItem("OPR", monthly.opr)
                                    RateItem("Base", monthly.baseRate)
                                    RateItem("FD 12M", monthly.fd12m)
                                    RateItem("Savings", monthly.savings)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RateItem(label: String, rate: Double?) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            rate?.let { "${String.format("%.2f", it)}%" } ?: "-",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium
        )
    }
}

private fun formatRateDate(date: String): String {
    return try {
        val parts = date.split("-")
        "${parts[0].takeLast(2)}-${parts[1]}"
    } catch (e: Exception) {
        date
    }
}
