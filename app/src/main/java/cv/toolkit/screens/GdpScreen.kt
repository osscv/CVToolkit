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

private data class GdpRecord(
    val date: String? = null,
    val series: String? = null,
    val gdp: Double? = null,
    val gni: Double? = null,
    @SerializedName("gdp_capita") val gdpCapita: Double? = null,
    @SerializedName("gni_capita") val gniCapita: Double? = null
)

private data class YearData(
    val year: String,
    val gdp: Double?,
    val gni: Double?,
    val gdpCapita: Double?,
    val gniCapita: Double?,
    val gdpGrowth: Double? = null
)

private const val API_URL =
    "https://api.data.gov.my/data-catalogue?id=gdp_gni_annual_real&sort=-date&limit=100"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GdpScreen(navController: NavController) {
    var records by remember { mutableStateOf<List<GdpRecord>>(emptyList()) }
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
                val type = object : TypeToken<List<GdpRecord>>() {}.type
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
                title = { Text("MY GDP & GNI") },
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
                GdpContent(
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
private fun GdpContent(records: List<GdpRecord>, modifier: Modifier = Modifier) {
    // Filter to absolute values only, sorted descending by date
    val absRecords = records
        .filter { it.series == "abs" && it.date != null }
        .sortedByDescending { it.date }

    // Build year data list (descending)
    val yearDataList = absRecords.mapIndexed { index, rec ->
        val year = rec.date!!.substring(0, 4)
        val prevGdp = absRecords.getOrNull(index + 1)?.gdp
        val growth = if (rec.gdp != null && prevGdp != null && prevGdp != 0.0) {
            ((rec.gdp - prevGdp) / prevGdp) * 100.0
        } else null

        YearData(
            year = year,
            gdp = rec.gdp,
            gni = rec.gni,
            gdpCapita = rec.gdpCapita,
            gniCapita = rec.gniCapita,
            gdpGrowth = growth
        )
    }

    val latest = yearDataList.firstOrNull()
    val nf = remember { NumberFormat.getNumberInstance(Locale.US) }

    // Chart data (oldest first)
    val chronological = yearDataList.reversed()
    val gdpLine = ChartLine(
        label = "GDP",
        points = chronological.mapNotNull { yd ->
            yd.gdp?.let { ChartPoint(yd.year, it.toFloat()) }
        },
        color = Color(0xFF1565C0)
    )
    val gniLine = ChartLine(
        label = "GNI",
        points = chronological.mapNotNull { yd ->
            yd.gni?.let { ChartPoint(yd.year, it.toFloat()) }
        },
        color = Color(0xFF2E7D32)
    )
    val capitaBarData = chronological.mapNotNull { yd ->
        yd.gdpCapita?.let { ChartPoint(yd.year, it.toFloat()) }
    }

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
                    label = "GDP",
                    value = latest?.gdp?.let { "RM ${formatBillion(it, nf)}" } ?: "-",
                    subtitle = latest?.year,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "GNI",
                    value = latest?.gni?.let { "RM ${formatBillion(it, nf)}" } ?: "-",
                    subtitle = latest?.year,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "GDP/Capita",
                    value = latest?.gdpCapita?.let { "RM ${nf.format(it.toLong())}" } ?: "-",
                    subtitle = latest?.year,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // GDP vs GNI line chart
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "GDP vs GNI (Real, Annual)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    SimpleLineChart(
                        lines = listOf(gdpLine, gniLine),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp),
                        yAxisLabel = "RM mil"
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LegendDot(Color(0xFF1565C0), "GDP")
                        Spacer(Modifier.width(16.dp))
                        LegendDot(Color(0xFF2E7D32), "GNI")
                    }
                }
            }
        }

        // GDP per capita bar chart
        if (capitaBarData.isNotEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "GDP per Capita Trend",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        SimpleBarChart(
                            data = capitaBarData,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp),
                            barColor = Color(0xFF1565C0),
                            maxBars = 20,
                            yAxisLabel = "RM"
                        )
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
        items(yearDataList) { yd ->
            GdpYearCard(yd, nf)
        }
    }
}

@Composable
private fun GdpYearCard(yd: YearData, nf: NumberFormat) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    yd.year,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                yd.gdpGrowth?.let { growth ->
                    val growthColor = if (growth >= 0) Color(0xFF2E7D32) else Color(0xFFC62828)
                    Text(
                        String.format("%+.1f%%", growth),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = growthColor
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "GDP",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        yd.gdp?.let { "RM ${formatBillion(it, nf)}" } ?: "-",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "GNI",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        yd.gni?.let { "RM ${formatBillion(it, nf)}" } ?: "-",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            yd.gdpCapita?.let { capita ->
                Spacer(Modifier.height(2.dp))
                Text(
                    "GDP/Capita: RM ${nf.format(capita.toLong())}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
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

/**
 * Format a value in millions as "X.X billion" or "X,XXX million".
 * The API returns values in RM millions.
 */
private fun formatBillion(valueMil: Double, nf: NumberFormat): String {
    return if (valueMil >= 1000.0) {
        String.format("%.1f billion", valueMil / 1000.0)
    } else {
        "${nf.format(valueMil.toLong())} million"
    }
}
