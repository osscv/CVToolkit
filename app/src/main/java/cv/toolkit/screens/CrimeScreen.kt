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

private data class CrimeRecord(
    val date: String?,
    val type: String?,
    val state: String?,
    val crimes: Int?,
    val category: String?,
    val district: String?
)

private data class CategoryOption(val key: String, val label: String)

private val CATEGORIES = listOf(
    CategoryOption("all", "All Categories"),
    CategoryOption("assault", "Assault"),
    CategoryOption("robbery", "Robbery"),
    CategoryOption("theft", "Theft"),
    CategoryOption("vehicle_theft", "Vehicle Theft"),
    CategoryOption("burglary", "Burglary"),
    CategoryOption("rape", "Rape"),
    CategoryOption("murder", "Murder")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CrimeScreen(navController: NavController) {
    var records by remember { mutableStateOf<List<CrimeRecord>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedCategory by remember { mutableStateOf(CATEGORIES[0]) }
    var categoryExpanded by remember { mutableStateOf(false) }

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
                val url = "https://api.data.gov.my/data-catalogue?id=crime_district&limit=200&filter=All@district,all@type"
                val body = withContext(Dispatchers.IO) {
                    val request = Request.Builder().url(url).build()
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) throw Exception("HTTP ${response.code}")
                        response.body.string()
                    }
                }
                val type = object : TypeToken<List<CrimeRecord>>() {}.type
                records = gson.fromJson(body, type)
                if (records.isEmpty()) {
                    errorMessage = "No crime data available"
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

    // Filter records by selected category
    val filteredRecords = remember(records, selectedCategory) {
        if (selectedCategory.key == "all") {
            records
        } else {
            records.filter { it.category.equals(selectedCategory.key, ignoreCase = true) }
        }
    }

    // Group by year and sum crimes
    val yearlyData = remember(filteredRecords) {
        filteredRecords
            .filter { it.date != null && it.crimes != null }
            .groupBy { it.date!!.substring(0, 4) }
            .map { (year, recs) -> year to recs.sumOf { it.crimes ?: 0 } }
            .sortedBy { it.first }
    }

    // Chart points (oldest first)
    val chartPoints = remember(yearlyData) {
        yearlyData.map { (year, total) -> ChartPoint(year, total.toFloat()) }
    }

    // Summary stats
    val totalCrimes = remember(yearlyData) { yearlyData.sumOf { it.second } }
    val avgPerYear = remember(yearlyData) {
        if (yearlyData.isNotEmpty()) totalCrimes / yearlyData.size else 0
    }
    val trend = remember(yearlyData) {
        if (yearlyData.size >= 2) {
            val firstHalf = yearlyData.take(yearlyData.size / 2).sumOf { it.second }
            val secondHalf = yearlyData.drop(yearlyData.size / 2).sumOf { it.second }
            if (secondHalf > firstHalf) "Increasing" else "Decreasing"
        } else "N/A"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MY Crime Statistics") },
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
            // Category selector
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Category", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(6.dp))
                    ExposedDropdownMenuBox(
                        expanded = categoryExpanded,
                        onExpandedChange = { categoryExpanded = it }
                    ) {
                        OutlinedTextField(
                            value = selectedCategory.label,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                            singleLine = true
                        )
                        ExposedDropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false }
                        ) {
                            CATEGORIES.forEach { cat ->
                                DropdownMenuItem(
                                    text = { Text(cat.label) },
                                    onClick = {
                                        selectedCategory = cat
                                        categoryExpanded = false
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
            if (filteredRecords.isNotEmpty() && errorMessage == null && !isLoading) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Bar chart
                    if (chartPoints.isNotEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        "Crimes by Year",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    SimpleBarChart(
                                        data = chartPoints,
                                        modifier = Modifier.fillMaxWidth(),
                                        barColor = MaterialTheme.colorScheme.error,
                                        yAxisLabel = "Total Crimes"
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
                                    label = "Total",
                                    value = numberFormat.format(totalCrimes),
                                    modifier = Modifier.weight(1f)
                                )
                                StatCard(
                                    label = "Avg / Year",
                                    value = numberFormat.format(avgPerYear),
                                    modifier = Modifier.weight(1f)
                                )
                                StatCard(
                                    label = "Trend",
                                    value = trend,
                                    modifier = Modifier.weight(1f),
                                    valueColor = if (trend == "Increasing") Color(0xFFD32F2F) else Color(0xFF388E3C)
                                )
                            }
                        }
                    }

                    // Year-by-year cards
                    items(yearlyData.reversed()) { (year, count) ->
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
                                    year,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    numberFormat.format(count),
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
