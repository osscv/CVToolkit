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

private data class ElectricityRecord(
    @SerializedName("date") val date: String,
    @SerializedName("sector") val sector: String,
    @SerializedName("consumption") val consumption: Double
)

private const val API_URL =
    "https://api.data.gov.my/data-catalogue?id=electricity_consumption&sort=-date&limit=200"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ElectricityScreen(navController: NavController) {
    var records by remember { mutableStateOf<List<ElectricityRecord>>(emptyList()) }
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
                val type = object : TypeToken<List<ElectricityRecord>>() {}.type
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
                title = { Text("MY Electricity Consumption") },
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
                ElectricityContent(
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
private fun ElectricityContent(records: List<ElectricityRecord>, modifier: Modifier = Modifier) {
    val nf = remember { NumberFormat.getNumberInstance(Locale.US).apply { maximumFractionDigits = 1 } }

    // Group by variable
    val totalRecords = records.filter { it.sector == "total" }
    val residentialRecords = records.filter { it.sector == "residential" }
    val commercialRecords = records.filter { it.sector == "commercial" }
    val industrialRecords = records.filter { it.sector == "industrial" }
    val miningRecords = records.filter { it.sector == "mining" }
    val lightingRecords = records.filter { it.sector == "public_lighting" }

    // Latest month data (records sorted descending by date from API)
    val latestDate = totalRecords.firstOrNull()?.date ?: "-"
    val latestTotal = totalRecords.firstOrNull()?.consumption
    val latestResidential = residentialRecords.firstOrNull()?.consumption
    val latestCommercial = commercialRecords.firstOrNull()?.consumption
    val latestIndustrial = industrialRecords.firstOrNull()?.consumption

    // Chart: total consumption trend (oldest first)
    val totalLine = ChartLine(
        label = "Total",
        points = totalRecords.sortedBy { it.date }.map { ChartPoint(it.date, it.consumption.toFloat()) },
        color = Color(0xFF1565C0)
    )

    // Multi-line chart: residential vs commercial vs industrial
    val residentialLine = ChartLine(
        label = "Residential",
        points = residentialRecords.sortedBy { it.date }.map { ChartPoint(it.date, it.consumption.toFloat()) },
        color = Color(0xFF2E7D32)
    )
    val commercialLine = ChartLine(
        label = "Commercial",
        points = commercialRecords.sortedBy { it.date }.map { ChartPoint(it.date, it.consumption.toFloat()) },
        color = Color(0xFFE65100)
    )
    val industrialLine = ChartLine(
        label = "Industrial",
        points = industrialRecords.sortedBy { it.date }.map { ChartPoint(it.date, it.consumption.toFloat()) },
        color = Color(0xFF6A1B9A)
    )

    // Bar chart: latest month breakdown by sector
    val latestSectorData = buildList {
        latestResidential?.let { add(ChartPoint("Residential", it.toFloat())) }
        latestCommercial?.let { add(ChartPoint("Commercial", it.toFloat())) }
        latestIndustrial?.let { add(ChartPoint("Industrial", it.toFloat())) }
        miningRecords.firstOrNull()?.let { add(ChartPoint("Mining", it.consumption.toFloat())) }
        lightingRecords.firstOrNull()?.let { add(ChartPoint("Lighting", it.consumption.toFloat())) }
    }

    // History: monthly totals (descending)
    val monthlyTotals = totalRecords.sortedByDescending { it.date }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Summary cards
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatCard(
                    label = "Total",
                    value = latestTotal?.let { "${nf.format(it)} GWh" } ?: "-",
                    subtitle = latestDate,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Residential",
                    value = latestResidential?.let { "${nf.format(it)} GWh" } ?: "-",
                    subtitle = latestDate,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatCard(
                    label = "Commercial",
                    value = latestCommercial?.let { "${nf.format(it)} GWh" } ?: "-",
                    subtitle = latestDate,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Industrial",
                    value = latestIndustrial?.let { "${nf.format(it)} GWh" } ?: "-",
                    subtitle = latestDate,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Total consumption trend line chart
        if (totalLine.points.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Total Consumption Trend",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        SimpleLineChart(
                            lines = listOf(totalLine),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            yAxisLabel = "GWh"
                        )
                    }
                }
            }
        }

        // Multi-line chart: Residential vs Commercial vs Industrial
        if (residentialLine.points.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Residential vs Commercial vs Industrial",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        SimpleLineChart(
                            lines = listOf(residentialLine, commercialLine, industrialLine),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            yAxisLabel = "GWh"
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ElectricityLegendDot(Color(0xFF2E7D32), "Residential")
                            Spacer(Modifier.width(12.dp))
                            ElectricityLegendDot(Color(0xFFE65100), "Commercial")
                            Spacer(Modifier.width(12.dp))
                            ElectricityLegendDot(Color(0xFF6A1B9A), "Industrial")
                        }
                    }
                }
            }
        }

        // Bar chart: latest month breakdown
        if (latestSectorData.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Breakdown by Sector ($latestDate)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        SimpleBarChart(
                            data = latestSectorData,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            barColor = Color(0xFF1565C0),
                            maxBars = 10,
                            yAxisLabel = "GWh"
                        )
                    }
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

        // History cards
        items(monthlyTotals) { record ->
            ElectricityMonthCard(record, nf)
        }
    }
}

@Composable
private fun ElectricityMonthCard(record: ElectricityRecord, nf: NumberFormat) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                record.date,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                "${nf.format(record.consumption)} GWh",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.End
            )
        }
    }
}

@Composable
private fun ElectricityLegendDot(color: Color, label: String) {
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
