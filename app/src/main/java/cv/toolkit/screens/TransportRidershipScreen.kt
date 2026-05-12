package cv.toolkit.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

private data class RidershipRecord(
    val date: String?,
    val bus_rkl: Long?,
    val rail_lrt_kj: Long?,
    val rail_lrt_ampang: Long?,
    val rail_mrt_kajang: Long?,
    val rail_mrt_pjy: Long?,
    val rail_monorail: Long?,
    val rail_komuter: Long?,
    val rail_ets: Long?
)

private data class ServiceOption(val key: String, val label: String, val color: Color)

private val SERVICES = listOf(
    ServiceOption("rail_lrt_kj", "LRT Kelana Jaya", Color(0xFFE53935)),
    ServiceOption("rail_lrt_ampang", "LRT Ampang", Color(0xFF1E88E5)),
    ServiceOption("rail_mrt_kajang", "MRT Kajang", Color(0xFF43A047)),
    ServiceOption("rail_mrt_pjy", "MRT Putrajaya", Color(0xFFFFB300)),
    ServiceOption("rail_monorail", "Monorail", Color(0xFF8E24AA)),
    ServiceOption("rail_komuter", "KTM Komuter", Color(0xFF00897B)),
    ServiceOption("rail_ets", "ETS", Color(0xFFFF6D00))
)

private fun getRidership(record: RidershipRecord, key: String): Long? {
    return when (key) {
        "rail_lrt_kj" -> record.rail_lrt_kj
        "rail_lrt_ampang" -> record.rail_lrt_ampang
        "rail_mrt_kajang" -> record.rail_mrt_kajang
        "rail_mrt_pjy" -> record.rail_mrt_pjy
        "rail_monorail" -> record.rail_monorail
        "rail_komuter" -> record.rail_komuter
        "rail_ets" -> record.rail_ets
        else -> null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransportRidershipScreen(navController: NavController) {
    var records by remember { mutableStateOf<List<RidershipRecord>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedService by remember { mutableStateOf(SERVICES[0]) }
    var serviceExpanded by remember { mutableStateOf(false) }

    val client = remember {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    val gson = remember { Gson() }
    val scope = rememberCoroutineScope()
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.US) }

    fun loadData() {
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val url = "https://api.data.gov.my/data-catalogue?id=ridership_headline&sort=-date&limit=200"
                val body = withContext(Dispatchers.IO) {
                    val request = Request.Builder().url(url).build()
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) throw Exception("HTTP ${response.code}")
                        response.body.string()
                    }
                }
                val type = object : TypeToken<List<RidershipRecord>>() {}.type
                records = gson.fromJson(body, type)
                if (records.isEmpty()) {
                    errorMessage = "No ridership data available"
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to fetch data"
            }
            isLoading = false
        }
    }

    LaunchedEffect(Unit) {
        loadData()
    }

    // Filter records that have data for selected service, sorted oldest first for chart
    val serviceRecords = remember(records, selectedService) {
        records
            .filter { getRidership(it, selectedService.key) != null }
            .sortedBy { it.date }
    }

    // Chart points (oldest first)
    val chartPoints = remember(serviceRecords) {
        serviceRecords.map { rec ->
            val label = rec.date?.substring(0, 10) ?: ""
            ChartPoint(label, (getRidership(rec, selectedService.key) ?: 0).toFloat())
        }
    }

    // Stats
    val latestRidership = remember(serviceRecords) {
        serviceRecords.lastOrNull()?.let { getRidership(it, selectedService.key) }
    }
    val averageRidership = remember(serviceRecords) {
        val values = serviceRecords.mapNotNull { getRidership(it, selectedService.key) }
        if (values.isNotEmpty()) values.sum() / values.size else 0L
    }
    val peakRidership = remember(serviceRecords) {
        serviceRecords.mapNotNull { getRidership(it, selectedService.key) }.maxOrNull() ?: 0L
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MY Transport Ridership") },
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
            // Service selector
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Service", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(6.dp))
                    ExposedDropdownMenuBox(
                        expanded = serviceExpanded,
                        onExpandedChange = { serviceExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedService.label,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = serviceExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                            singleLine = true
                        )
                        ExposedDropdownMenu(
                            expanded = serviceExpanded,
                            onDismissRequest = { serviceExpanded = false }
                        ) {
                            SERVICES.forEach { svc ->
                                DropdownMenuItem(
                                    text = { Text(svc.label) },
                                    onClick = {
                                        selectedService = svc
                                        serviceExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Loading
            AnimatedVisibility(visible = isLoading) {
                Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(modifier = Modifier.size(32.dp))
                }
            }

            // Error
            errorMessage?.let { error ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Warning, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onErrorContainer)
                        Spacer(Modifier.width(8.dp))
                        Text(error, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // Content
            if (serviceRecords.isNotEmpty() && errorMessage == null && !isLoading) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Line chart
                    if (chartPoints.isNotEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        "Daily Ridership - ${selectedService.label}",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    SimpleLineChart(
                                        lines = listOf(
                                            ChartLine(
                                                label = selectedService.label,
                                                points = chartPoints,
                                                color = selectedService.color
                                            )
                                        ),
                                        modifier = Modifier.fillMaxWidth(),
                                        yAxisLabel = "Ridership"
                                    )
                                }
                            }
                        }
                    }

                    // Summary stats
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                StatCard(
                                    label = "Latest",
                                    value = numberFormat.format(latestRidership ?: 0),
                                    modifier = Modifier.weight(1f)
                                )
                                StatCard(
                                    label = "Average",
                                    value = numberFormat.format(averageRidership),
                                    modifier = Modifier.weight(1f)
                                )
                                StatCard(
                                    label = "Peak",
                                    value = numberFormat.format(peakRidership),
                                    modifier = Modifier.weight(1f),
                                    valueColor = Color(0xFF1B5E20)
                                )
                            }
                        }
                    }

                    // Scrollable history (newest first)
                    items(serviceRecords.reversed()) { record ->
                        val ridership = getRidership(record, selectedService.key) ?: 0
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    record.date?.substring(0, 10) ?: "-",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    numberFormat.format(ridership),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            } else if (!isLoading && errorMessage == null) {
                Spacer(Modifier.weight(1f))
            }

            BannerAd(modifier = Modifier.fillMaxWidth())
        }
    }
}
