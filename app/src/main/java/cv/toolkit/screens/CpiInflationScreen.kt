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

// --- Data models ---

private data class CpiItem(
    @SerializedName("date") val date: String,
    @SerializedName("index") val index: Float,
    @SerializedName("division") val division: String
)

private data class InflationItem(
    @SerializedName("date") val date: String,
    @SerializedName("inflation_yoy") val inflationYoy: Float,
    @SerializedName("division") val division: String
)

private const val BASE_URL = "https://api.data.gov.my/data-catalogue"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CpiInflationScreen(navController: NavController) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("CPI Index", "Inflation Rate")

    // CPI state
    var cpiData by remember { mutableStateOf<List<CpiItem>>(emptyList()) }
    var cpiLoading by remember { mutableStateOf(false) }
    var cpiError by remember { mutableStateOf<String?>(null) }

    // Inflation state
    var inflationData by remember { mutableStateOf<List<InflationItem>>(emptyList()) }
    var inflationLoading by remember { mutableStateOf(false) }
    var inflationError by remember { mutableStateOf<String?>(null) }

    val client = remember {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    val gson = remember { Gson() }
    val scope = rememberCoroutineScope()

    fun loadCpi() {
        if (cpiLoading) return
        scope.launch {
            cpiLoading = true
            cpiError = null
            try {
                val body = withContext(Dispatchers.IO) {
                    val request = Request.Builder()
                        .url("$BASE_URL?id=cpi_headline&sort=-date&limit=60&filter=overall@division")
                        .build()
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) throw Exception("HTTP ${response.code}")
                        response.body.string()
                    }
                }
                val type = object : TypeToken<List<CpiItem>>() {}.type
                cpiData = gson.fromJson(body, type)
            } catch (e: Exception) {
                cpiError = e.message ?: "Failed to load CPI data"
            }
            cpiLoading = false
        }
    }

    fun loadInflation() {
        if (inflationLoading) return
        scope.launch {
            inflationLoading = true
            inflationError = null
            try {
                val body = withContext(Dispatchers.IO) {
                    val request = Request.Builder()
                        .url("$BASE_URL?id=cpi_headline_inflation&sort=-date&limit=60&filter=overall@division")
                        .build()
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) throw Exception("HTTP ${response.code}")
                        response.body.string()
                    }
                }
                val type = object : TypeToken<List<InflationItem>>() {}.type
                inflationData = gson.fromJson(body, type)
            } catch (e: Exception) {
                inflationError = e.message ?: "Failed to load inflation data"
            }
            inflationLoading = false
        }
    }

    // Load both datasets on first composition
    LaunchedEffect(Unit) {
        loadCpi()
        loadInflation()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MY CPI & Inflation") },
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
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Summary cards
            val isLoading = cpiLoading || inflationLoading
            if (isLoading && cpiData.isEmpty() && inflationData.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                val latestCpi = cpiData.firstOrNull()
                val latestInflation = inflationData.firstOrNull()
                val inflationColor = when {
                    latestInflation == null -> MaterialTheme.colorScheme.onSurface
                    latestInflation.inflationYoy < 3f -> Color(0xFF2E7D32) // green
                    latestInflation.inflationYoy <= 5f -> Color(0xFFF9A825) // yellow/amber
                    else -> Color(0xFFC62828) // red
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatCard(
                        label = "Latest CPI",
                        value = latestCpi?.let { String.format("%.1f", it.index) } ?: "—",
                        subtitle = latestCpi?.date,
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "YoY Inflation",
                        value = latestInflation?.let { String.format("%.1f%%", it.inflationYoy) } ?: "—",
                        subtitle = latestInflation?.date,
                        valueColor = inflationColor,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Tabs
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            // Tab content
            Column(modifier = Modifier.weight(1f)) {
                when (selectedTab) {
                    0 -> CpiTab(
                        data = cpiData,
                        isLoading = cpiLoading,
                        error = cpiError,
                        onRetry = { loadCpi() }
                    )
                    1 -> InflationTab(
                        data = inflationData,
                        isLoading = inflationLoading,
                        error = inflationError,
                        onRetry = { loadInflation() }
                    )
                }
            }

            BannerAd(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun CpiTab(
    data: List<CpiItem>,
    isLoading: Boolean,
    error: String?,
    onRetry: () -> Unit
) {
    when {
        isLoading && data.isEmpty() -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        error != null && data.isEmpty() -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(error, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = onRetry) { Text("Retry") }
                }
            }
        }
        else -> {
            val chronological = data.sortedBy { it.date }
            val chartPoints = chronological.map { item ->
                ChartPoint(
                    label = item.date.substring(0, 7), // yyyy-MM
                    value = item.index
                )
            }
            val chartLine = ChartLine(
                label = "CPI Index",
                points = chartPoints,
                color = MaterialTheme.colorScheme.primary
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "CPI Index Trend",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(8.dp))
                            SimpleLineChart(
                                lines = listOf(chartLine),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                yAxisLabel = "Index"
                            )
                        }
                    }
                }

                item {
                    Text(
                        "History",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                items(data) { item ->
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
                                item.date,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                String.format("%.1f", item.index),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InflationTab(
    data: List<InflationItem>,
    isLoading: Boolean,
    error: String?,
    onRetry: () -> Unit
) {
    when {
        isLoading && data.isEmpty() -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        error != null && data.isEmpty() -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(error, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = onRetry) { Text("Retry") }
                }
            }
        }
        else -> {
            val chronological = data.sortedBy { it.date }
            val inflationPoints = chronological.map { item ->
                ChartPoint(
                    label = item.date.substring(0, 7),
                    value = item.inflationYoy
                )
            }
            val zeroLinePoints = chronological.map { item ->
                ChartPoint(
                    label = item.date.substring(0, 7),
                    value = 0f
                )
            }
            val chartLines = listOf(
                ChartLine(
                    label = "Inflation YoY %",
                    points = inflationPoints,
                    color = MaterialTheme.colorScheme.primary
                ),
                ChartLine(
                    label = "0% Reference",
                    points = zeroLinePoints,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                )
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Inflation Rate Trend",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(8.dp))
                            SimpleLineChart(
                                lines = chartLines,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(200.dp),
                                yAxisLabel = "%"
                            )
                        }
                    }
                }

                item {
                    Text(
                        "History",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                items(data) { item ->
                    val valueColor = when {
                        item.inflationYoy < 3f -> Color(0xFF2E7D32)
                        item.inflationYoy <= 5f -> Color(0xFFF9A825)
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
                            Text(
                                item.date,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                String.format("%.1f%%", item.inflationYoy),
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.Bold,
                                color = valueColor
                            )
                        }
                    }
                }
            }
        }
    }
}
