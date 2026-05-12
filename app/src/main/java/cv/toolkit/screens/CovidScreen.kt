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

private data class CovidRecord(
    val date: String?,
    val state: String?,
    val cases_new: Int?,
    val cases_active: Int?,
    val cases_import: Int?,
    val cases_cluster: Int?,
    val cases_recovered: Int?
)

private val STATES = listOf(
    "Malaysia",
    "Selangor",
    "Johor",
    "Sabah",
    "Sarawak",
    "Perak",
    "Kedah",
    "Kelantan",
    "Pulau Pinang",
    "Pahang",
    "Terengganu",
    "Negeri Sembilan",
    "Melaka",
    "W.P. Kuala Lumpur",
    "W.P. Putrajaya",
    "W.P. Labuan",
    "Perlis"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CovidScreen(navController: NavController) {
    var selectedState by remember { mutableStateOf(STATES[0]) }
    var records by remember { mutableStateOf<List<CovidRecord>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    val client = remember {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    val gson = remember { Gson() }
    val scope = rememberCoroutineScope()
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.US) }

    fun loadData(state: String) {
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val url = buildString {
                    append("https://api.data.gov.my/data-catalogue?id=covid_cases")
                    append("&sort=-date")
                    append("&limit=200")
                    append("&filter=${state}@state")
                }
                val body = withContext(Dispatchers.IO) {
                    val request = Request.Builder().url(url).build()
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) throw Exception("HTTP ${response.code}")
                        response.body.string()
                    }
                }
                val type = object : TypeToken<List<CovidRecord>>() {}.type
                records = gson.fromJson(body, type)
                if (records.isEmpty()) {
                    errorMessage = "No data available for $state"
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to fetch data"
            }
            isLoading = false
        }
    }

    LaunchedEffect(selectedState) {
        loadData(selectedState)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MY COVID-19") },
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
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // State selector
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("State / Region", style = MaterialTheme.typography.labelLarge)
                            Spacer(Modifier.height(6.dp))
                            ExposedDropdownMenuBox(
                                expanded = dropdownExpanded,
                                onExpandedChange = { dropdownExpanded = it }
                            ) {
                                OutlinedTextField(
                                    value = selectedState,
                                    onValueChange = {},
                                    readOnly = true,
                                    trailingIcon = {
                                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded)
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
                                    singleLine = true
                                )
                                ExposedDropdownMenu(
                                    expanded = dropdownExpanded,
                                    onDismissRequest = { dropdownExpanded = false }
                                ) {
                                    STATES.forEach { state ->
                                        DropdownMenuItem(
                                            text = { Text(state) },
                                            onClick = {
                                                selectedState = state
                                                dropdownExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Loading / Error
                if (isLoading) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                } else if (errorMessage != null) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Text(
                                errorMessage ?: "",
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                } else if (records.isNotEmpty()) {
                    // Summary cards
                    item {
                        val latest = records.first()
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    "Latest: ${latest.date ?: "N/A"}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    StatCard(
                                        label = "New Cases",
                                        value = numberFormat.format(latest.cases_new ?: 0),
                                        modifier = Modifier.weight(1f),
                                        valueColor = Color(0xFFE53935)
                                    )
                                    StatCard(
                                        label = "Active",
                                        value = numberFormat.format(latest.cases_active ?: 0),
                                        modifier = Modifier.weight(1f),
                                        valueColor = Color(0xFFFF9800)
                                    )
                                    StatCard(
                                        label = "Recovered",
                                        value = numberFormat.format(latest.cases_recovered ?: 0),
                                        modifier = Modifier.weight(1f),
                                        valueColor = Color(0xFF43A047)
                                    )
                                }
                            }
                        }
                    }

                    // Line chart - daily new cases trend (chronological)
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    "Daily New Cases Trend",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(8.dp))

                                val chronological = records.reversed()
                                val chartPoints = chronological.map { record ->
                                    ChartPoint(
                                        label = record.date?.substring(0, 7) ?: "",
                                        value = (record.cases_new ?: 0).toFloat()
                                    )
                                }
                                val chartLine = ChartLine(
                                    label = "New Cases",
                                    points = chartPoints,
                                    color = Color(0xFFE53935)
                                )
                                SimpleLineChart(
                                    lines = listOf(chartLine),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(top = 4.dp),
                                    yAxisLabel = "Cases"
                                )
                            }
                        }
                    }

                    // History header
                    item {
                        Text(
                            "History (${records.size} records)",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }

                    // Scrollable history cards
                    items(records) { record ->
                        val newCases = record.cases_new ?: 0
                        val cardColor = when {
                            newCases >= 10000 -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f)
                            newCases >= 5000 -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
                            newCases >= 1000 -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
                            else -> MaterialTheme.colorScheme.surface
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = cardColor)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        record.date ?: "N/A",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    if (record.cases_import != null && record.cases_import > 0) {
                                        Text(
                                            "Imported: ${numberFormat.format(record.cases_import)}",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        "+${numberFormat.format(newCases)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFE53935)
                                    )
                                    Text(
                                        "Active: ${numberFormat.format(record.cases_active ?: 0)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFFFF9800)
                                    )
                                    Text(
                                        "Recovered: ${numberFormat.format(record.cases_recovered ?: 0)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color(0xFF43A047)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            BannerAd(modifier = Modifier.fillMaxWidth())
        }
    }
}
