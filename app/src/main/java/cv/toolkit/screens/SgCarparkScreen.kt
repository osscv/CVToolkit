package cv.toolkit.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import cv.toolkit.ads.BannerAd
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

private const val TAG = "SgCarpark"
private const val API_URL = "https://api.data.gov.sg/v1/transport/carpark-availability"
private const val API_KEY = "v2:3628e11212324251b93eccb69f46844aa70b7877ab736bf17f2f75deb7c19c0b:bmc3TaMdW9V-4pdcz5quf7qP-4VCHR2B"

private val httpClient = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(15, TimeUnit.SECONDS)
    .writeTimeout(15, TimeUnit.SECONDS)
    .build()

private data class CarparkItem(
    val carparkNumber: String,
    val lotType: String,
    val totalLots: Int,
    val lotsAvailable: Int,
    val updateDatetime: String
) {
    val occupancyPercent: Float
        get() = if (totalLots > 0) ((totalLots - lotsAvailable).toFloat() / totalLots) * 100f else 0f
}

private fun lotTypeLabel(type: String): String = when (type) {
    "C" -> "Car"
    "Y" -> "Motorcycle"
    "H" -> "Heavy Vehicle"
    else -> type
}

private fun occupancyColor(percent: Float): Color = when {
    percent < 50f -> Color(0xFF4CAF50)
    percent < 80f -> Color(0xFFFFC107)
    else -> Color(0xFFF44336)
}

private fun parseCarparks(json: String): List<CarparkItem> {
    val type = object : TypeToken<Map<String, Any>>() {}.type
    val root: Map<String, Any> = Gson().fromJson(json, type)
    val items = (root["items"] as? List<*>) ?: return emptyList()
    val firstItem = (items.firstOrNull() as? Map<*, *>) ?: return emptyList()
    val carparkData = (firstItem["carpark_data"] as? List<*>) ?: return emptyList()

    return carparkData.flatMap { entry ->
        val cp = entry as? Map<*, *> ?: return@flatMap emptyList()
        val carparkNumber = cp["carpark_number"] as? String ?: ""
        val updateDatetime = cp["update_datetime"] as? String ?: ""
        val infoList = (cp["carpark_info"] as? List<*>) ?: return@flatMap emptyList()

        infoList.mapNotNull { info ->
            val i = info as? Map<*, *> ?: return@mapNotNull null
            val totalStr = i["total_lots"] as? String ?: "0"
            val availStr = i["lots_available"] as? String ?: "0"
            val lotType = i["lot_type"] as? String ?: "C"
            CarparkItem(
                carparkNumber = carparkNumber,
                lotType = lotType,
                totalLots = totalStr.toIntOrNull() ?: 0,
                lotsAvailable = availStr.toIntOrNull() ?: 0,
                updateDatetime = updateDatetime
            )
        }
    }
}

// Sort modes
private enum class CarparkSortMode { AVAILABILITY, OCCUPANCY }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SgCarparkScreen(navController: NavController) {
    val scope = rememberCoroutineScope()

    var carparks by remember { mutableStateOf<List<CarparkItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var sortMode by remember { mutableStateOf(CarparkSortMode.AVAILABILITY) }

    suspend fun fetchData() {
        isLoading = true
        errorMessage = null
        try {
            val result = withContext(Dispatchers.IO) {
                val request = Request.Builder()
                    .url(API_URL)
                    .header("x-api-key", API_KEY)
                    .build()
                val response = httpClient.newCall(request).execute()
                response.use { resp ->
                    if (!resp.isSuccessful) throw Exception("HTTP ${resp.code}")
                    parseCarparks(resp.body.string())
                }
            }
            carparks = result
        } catch (e: Exception) {
            Log.e(TAG, "Fetch failed", e)
            errorMessage = e.message ?: "Unknown error"
        } finally {
            isLoading = false
        }
    }

    // Auto-refresh every 60s
    LaunchedEffect(Unit) {
        while (true) {
            fetchData()
            delay(60_000)
        }
    }

    // Filtered + sorted list
    val displayList by remember(carparks, searchQuery, sortMode) {
        derivedStateOf {
            val filtered = if (searchQuery.isBlank()) carparks
            else carparks.filter { it.carparkNumber.contains(searchQuery.trim(), ignoreCase = true) }

            when (sortMode) {
                CarparkSortMode.AVAILABILITY -> filtered.sortedByDescending { it.lotsAvailable }
                CarparkSortMode.OCCUPANCY -> filtered.sortedByDescending { it.occupancyPercent }
            }
        }
    }

    // Summary stats
    val totalCarparks = carparks.size
    val totalLots = carparks.sumOf { it.totalLots }
    val totalAvailable = carparks.sumOf { it.lotsAvailable }
    val overallOccupancy = if (totalLots > 0)
        ((totalLots - totalAvailable).toFloat() / totalLots * 100f) else 0f

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SG Carpark Availability") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { scope.launch { fetchData() } },
                        enabled = !isLoading
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
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
            if (isLoading && carparks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            } else if (errorMessage != null && carparks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Filled.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(errorMessage ?: "Error", color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(onClick = { scope.launch { fetchData() } }) {
                            Text("Retry")
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Summary card
                    item {
                        SummaryCard(totalCarparks, totalLots, totalAvailable, overallOccupancy)
                    }

                    // Search bar
                    item {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            modifier = Modifier.fillMaxWidth(),
                            placeholder = { Text("Search carpark number...") },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(Icons.Filled.Clear, contentDescription = "Clear")
                                    }
                                }
                            },
                            singleLine = true
                        )
                    }

                    // Sort toggle
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = sortMode == CarparkSortMode.AVAILABILITY,
                                onClick = { sortMode = CarparkSortMode.AVAILABILITY },
                                label = { Text("Most Available") },
                                leadingIcon = if (sortMode == CarparkSortMode.AVAILABILITY) {
                                    { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null
                            )
                            FilterChip(
                                selected = sortMode == CarparkSortMode.OCCUPANCY,
                                onClick = { sortMode = CarparkSortMode.OCCUPANCY },
                                label = { Text("Fullest First") },
                                leadingIcon = if (sortMode == CarparkSortMode.OCCUPANCY) {
                                    { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                } else null
                            )
                            Spacer(modifier = Modifier.weight(1f))
                            Text(
                                text = "${displayList.size} carparks",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.align(Alignment.CenterVertically)
                            )
                        }
                    }

                    // Carpark cards
                    items(displayList, key = { "${it.carparkNumber}_${it.lotType}" }) { cp ->
                        CarparkCard(cp)
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(
    totalCarparks: Int,
    totalLots: Int,
    totalAvailable: Int,
    occupancyPercent: Float
) {
    val color = occupancyColor(occupancyPercent)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Overview",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatColumn("Carparks", "$totalCarparks")
                StatColumn("Total Lots", "$totalLots")
                StatColumn("Available", "$totalAvailable")
                StatColumn("Occupancy", "${"%.1f".format(occupancyPercent)}%")
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (occupancyPercent / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = color,
                trackColor = color.copy(alpha = 0.2f)
            )
        }
    }
}

@Composable
private fun StatColumn(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(text = label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun CarparkCard(cp: CarparkItem) {
    val occ = cp.occupancyPercent
    val color = occupancyColor(occ)

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = cp.carparkNumber,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = lotTypeLabel(cp.lotType),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${cp.lotsAvailable} / ${cp.totalLots}",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                    Text(
                        text = "${"%.1f".format(occ)}% occupied",
                        style = MaterialTheme.typography.labelSmall,
                        color = color
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { (occ / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = color,
                trackColor = color.copy(alpha = 0.2f)
            )
        }
    }
}
