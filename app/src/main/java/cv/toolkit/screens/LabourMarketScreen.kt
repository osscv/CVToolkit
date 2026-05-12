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

private data class LfsRecord(
    @SerializedName("date") val date: String,
    @SerializedName("lf") val lf: Double,
    @SerializedName("lf_employed") val lfEmployed: Double,
    @SerializedName("lf_unemployed") val lfUnemployed: Double,
    @SerializedName("u_rate") val uRate: Double,
    @SerializedName("p_rate") val pRate: Double
)

private const val API_URL =
    "https://api.data.gov.my/data-catalogue?id=lfs_month&sort=-date&limit=120"

private fun formatThousands(value: Double, nf: NumberFormat): String {
    return if (value >= 1000.0) {
        String.format("%.2f million", value / 1000.0)
    } else {
        "${nf.format(value.toLong())}k"
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LabourMarketScreen(navController: NavController) {
    var records by remember { mutableStateOf<List<LfsRecord>>(emptyList()) }
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

    fun loadData() {
        if (isLoading) return
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val body = withContext(Dispatchers.IO) {
                    val request = Request.Builder().url(API_URL).build()
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) throw Exception("HTTP ${response.code}")
                        response.body.string()
                    }
                }
                val type = object : TypeToken<List<LfsRecord>>() {}.type
                records = gson.fromJson(body, type)
                if (records.isEmpty()) {
                    errorMessage = "No data available"
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to fetch data"
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) { loadData() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MY Labour Market") },
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
            errorMessage != null && records.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            errorMessage ?: "Error",
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { loadData() }) { Text("Retry") }
                    }
                }
            }
            else -> {
                LabourMarketContent(
                    records = records,
                    numberFormat = numberFormat,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                )
            }
        }
    }
}

@Composable
private fun LabourMarketContent(
    records: List<LfsRecord>,
    numberFormat: NumberFormat,
    modifier: Modifier = Modifier
) {
    val latest = records.first()
    val chronological = remember(records) { records.sortedBy { it.date } }

    val uRateColor = when {
        latest.uRate < 4.0 -> Color(0xFF2E7D32)
        latest.uRate < 5.0 -> Color(0xFFF9A825)
        else -> Color(0xFFC62828)
    }

    // Chart data - unemployment rate
    val uRatePoints = remember(chronological) {
        chronological.map { item ->
            ChartPoint(
                label = item.date.substring(2, 7), // YY-MM
                value = item.uRate.toFloat()
            )
        }
    }
    val uRateLine = ChartLine(
        label = "Unemployment %",
        points = uRatePoints,
        color = Color(0xFFC62828)
    )

    // Chart data - labour force composition
    val employedPoints = remember(chronological) {
        chronological.map { item ->
            ChartPoint(
                label = item.date.substring(2, 7),
                value = item.lfEmployed.toFloat()
            )
        }
    }
    val unemployedPoints = remember(chronological) {
        chronological.map { item ->
            ChartPoint(
                label = item.date.substring(2, 7),
                value = item.lfUnemployed.toFloat()
            )
        }
    }
    val lfLines = listOf(
        ChartLine(
            label = "Employed",
            points = employedPoints,
            color = Color(0xFF2E7D32)
        ),
        ChartLine(
            label = "Unemployed",
            points = unemployedPoints,
            color = Color(0xFFC62828)
        )
    )

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Summary cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    label = "Unemployment Rate",
                    value = String.format("%.1f%%", latest.uRate),
                    subtitle = latest.date,
                    valueColor = uRateColor,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Labour Force",
                    value = formatThousands(latest.lf, numberFormat),
                    subtitle = latest.date,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    label = "Employed",
                    value = formatThousands(latest.lfEmployed, numberFormat),
                    subtitle = "Participation: ${String.format("%.1f%%", latest.pRate)}",
                    valueColor = Color(0xFF2E7D32),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Unemployed",
                    value = formatThousands(latest.lfUnemployed, numberFormat),
                    subtitle = latest.date,
                    valueColor = Color(0xFFC62828),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Unemployment rate chart
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Unemployment Rate Trend",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    SimpleLineChart(
                        lines = listOf(uRateLine),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        yAxisLabel = "%"
                    )
                }
            }
        }

        // Labour force chart
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Labour Force (thousands)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    SimpleLineChart(
                        lines = lfLines,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        yAxisLabel = "'000"
                    )
                }
            }
        }

        // History header
        item {
            Text(
                "Monthly History",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        // Monthly records
        items(records, key = { it.date }) { record ->
            LfsMonthCard(record, numberFormat)
        }
    }
}

@Composable
private fun LfsMonthCard(record: LfsRecord, numberFormat: NumberFormat) {
    val uRateColor = when {
        record.uRate < 4.0 -> Color(0xFF2E7D32)
        record.uRate < 5.0 -> Color(0xFFF9A825)
        else -> Color(0xFFC62828)
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
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    record.date,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    "Employed: ${formatThousands(record.lfEmployed, numberFormat)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "Unemployed: ${formatThousands(record.lfUnemployed, numberFormat)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    String.format("%.1f%%", record.uRate),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = uRateColor
                )
                Text(
                    "unemployment",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
