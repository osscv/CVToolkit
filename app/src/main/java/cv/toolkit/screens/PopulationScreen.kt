package cv.toolkit.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import cv.toolkit.ads.BannerAd
import okhttp3.OkHttpClient
import okhttp3.Request
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

private data class PopulationRecord(
    val age: String?,
    val sex: String?,
    val date: String?,
    val state: String?,
    val ethnicity: String?,
    val population: Double?
)

private val STATE_ABBREVIATIONS = mapOf(
    "Johor" to "JHR",
    "Kedah" to "KDH",
    "Kelantan" to "KTN",
    "Melaka" to "MLK",
    "Negeri Sembilan" to "N.S.",
    "Pahang" to "PHG",
    "Perak" to "PRK",
    "Perlis" to "PLS",
    "Pulau Pinang" to "P.P.",
    "Sabah" to "SBH",
    "Sarawak" to "SWK",
    "Selangor" to "Sel",
    "Terengganu" to "TRG",
    "W.P. Kuala Lumpur" to "W.P. KL",
    "W.P. Labuan" to "Labuan",
    "W.P. Putrajaya" to "Pjaya",
    "Malaysia" to "MY"
)

private enum class SortMode { NAME, POPULATION }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PopulationScreen(navController: NavController) {
    var allRecords by remember { mutableStateOf<List<PopulationRecord>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedYear by remember { mutableStateOf<String?>(null) }
    var yearDropdownExpanded by remember { mutableStateOf(false) }
    var sortMode by remember { mutableStateOf(SortMode.POPULATION) }

    val client = remember {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    val gson = remember { Gson() }
    val scope = rememberCoroutineScope()
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.US) }

    val years = remember(allRecords) {
        allRecords.mapNotNull { it.date?.take(4) }.distinct().sortedDescending()
    }

    val filteredRecords = remember(allRecords, selectedYear) {
        if (selectedYear == null) emptyList()
        else allRecords.filter {
            it.date?.startsWith(selectedYear!!) == true && it.state != "Malaysia"
        }
    }

    val totalPopulation = remember(allRecords, selectedYear) {
        if (selectedYear == null) null
        else {
            val malaysiaRecord = allRecords.find {
                it.date?.startsWith(selectedYear!!) == true && it.state == "Malaysia"
            }
            malaysiaRecord?.population?.let { it * 1000 }
                ?: filteredRecords.sumOf { (it.population ?: 0.0) * 1000 }
        }
    }

    val sortedRecords = remember(filteredRecords, sortMode) {
        when (sortMode) {
            SortMode.NAME -> filteredRecords.sortedBy { it.state ?: "" }
            SortMode.POPULATION -> filteredRecords.sortedByDescending { it.population ?: 0.0 }
        }
    }

    val chartData = remember(filteredRecords) {
        filteredRecords
            .sortedByDescending { it.population ?: 0.0 }
            .map { record ->
                val abbr = STATE_ABBREVIATIONS[record.state] ?: (record.state?.take(5) ?: "?")
                ChartPoint(abbr, ((record.population ?: 0.0) * 1000).toFloat())
            }
    }

    LaunchedEffect(Unit) {
        isLoading = true
        errorMessage = null
        try {
            val url = "https://api.data.gov.my/data-catalogue?id=population_state&limit=500" +
                    "&filter=overall_age@age,overall_sex@sex,overall_ethnicity@ethnicity"
            val body = withContext(Dispatchers.IO) {
                val request = Request.Builder().url(url).build()
                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) throw Exception("HTTP ${response.code}")
                    response.body.string()
                }
            }
            val type = object : TypeToken<List<PopulationRecord>>() {}.type
            val records: List<PopulationRecord> = gson.fromJson(body, type)
            allRecords = records
            if (records.isEmpty()) {
                errorMessage = "No data available"
            }
        } catch (e: Exception) {
            errorMessage = e.message ?: "Failed to fetch data"
        }
        isLoading = false
    }

    LaunchedEffect(years) {
        if (selectedYear == null && years.isNotEmpty()) {
            selectedYear = years.first()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MY Population") },
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
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = {
                            scope.launch {
                                isLoading = true
                                errorMessage = null
                                try {
                                    val url = "https://api.data.gov.my/data-catalogue?id=population_state&limit=500" +
                                            "&filter=overall_age@age,overall_sex@sex,overall_ethnicity@ethnicity"
                                    val body = withContext(Dispatchers.IO) {
                                        val request = Request.Builder().url(url).build()
                                        client.newCall(request).execute().use { response ->
                                            if (!response.isSuccessful) throw Exception("HTTP ${response.code}")
                                            response.body.string()
                                        }
                                    }
                                    val type = object : TypeToken<List<PopulationRecord>>() {}.type
                                    allRecords = gson.fromJson(body, type)
                                } catch (e: Exception) {
                                    errorMessage = e.message ?: "Failed to fetch data"
                                }
                                isLoading = false
                            }
                        }) {
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
                    // Year selector
                    item {
                        ExposedDropdownMenuBox(
                            expanded = yearDropdownExpanded,
                            onExpandedChange = { yearDropdownExpanded = it }
                        ) {
                            OutlinedTextField(
                                value = selectedYear ?: "",
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Year") },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = yearDropdownExpanded)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                                colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors()
                            )
                            ExposedDropdownMenu(
                                expanded = yearDropdownExpanded,
                                onDismissRequest = { yearDropdownExpanded = false }
                            ) {
                                years.forEach { year ->
                                    DropdownMenuItem(
                                        text = { Text(year) },
                                        onClick = {
                                            selectedYear = year
                                            yearDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    // Total population card
                    if (totalPopulation != null) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                StatCard(
                                    label = "Total Population ($selectedYear)",
                                    value = numberFormat.format(totalPopulation.toLong()),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(8.dp),
                                    subtitle = "Source: DOSM"
                                )
                            }
                        }
                    }

                    // Bar chart
                    if (chartData.isNotEmpty()) {
                        item {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        "Population by State",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    SimpleBarChart(
                                        data = chartData,
                                        modifier = Modifier.fillMaxWidth(),
                                        barColor = MaterialTheme.colorScheme.primary,
                                        maxBars = 16,
                                        yAxisLabel = "Population"
                                    )
                                }
                            }
                        }
                    }

                    // Sort toggle header
                    if (sortedRecords.isNotEmpty()) {
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "States (${sortedRecords.size})",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                IconButton(onClick = {
                                    sortMode = when (sortMode) {
                                        SortMode.NAME -> SortMode.POPULATION
                                        SortMode.POPULATION -> SortMode.NAME
                                    }
                                }) {
                                    Icon(
                                        Icons.Default.SortByAlpha,
                                        contentDescription = "Sort by ${
                                            when (sortMode) {
                                                SortMode.NAME -> "population"
                                                SortMode.POPULATION -> "name"
                                            }
                                        }",
                                        tint = if (sortMode == SortMode.NAME)
                                            MaterialTheme.colorScheme.primary
                                        else
                                            MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        // State cards
                        items(sortedRecords, key = { it.state ?: "" }) { record ->
                            val pop = (record.population ?: 0.0) * 1000
                            val percentage = if (totalPopulation != null && totalPopulation > 0) {
                                pop / totalPopulation * 100
                            } else 0.0

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
                                            record.state ?: "Unknown",
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            String.format("%.1f%%", percentage),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        numberFormat.format(pop.toLong()),
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
