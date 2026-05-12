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

private data class HouseholdIncomeRecord(
    val date: String?,
    val state: String?,
    @SerializedName("income_mean") val incomeMean: Double?,
    @SerializedName("income_median") val incomeMedian: Double?
) {
    val year: String get() = date?.substring(0, 4) ?: ""
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HouseholdIncomeScreen(navController: NavController) {
    var allRecords by remember { mutableStateOf<List<HouseholdIncomeRecord>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var selectedYear by remember { mutableStateOf("") }
    var sortByIncome by remember { mutableStateOf(true) }

    val client = remember {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }
    val gson = remember { Gson() }
    val scope = rememberCoroutineScope()
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.US) }

    fun formatRM(value: Double?): String {
        if (value == null) return "-"
        return "RM ${numberFormat.format(value.toLong())}"
    }

    fun loadData() {
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val url = "https://api.data.gov.my/data-catalogue?id=hh_income_state&limit=500"
                val body = withContext(Dispatchers.IO) {
                    val request = Request.Builder().url(url).build()
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) throw Exception("HTTP ${response.code}")
                        response.body.string()
                    }
                }
                val type = object : TypeToken<List<HouseholdIncomeRecord>>() {}.type
                val records: List<HouseholdIncomeRecord> = gson.fromJson(body, type)
                allRecords = records
                if (records.isEmpty()) {
                    errorMessage = "No data available"
                } else {
                    val years = records.map { it.year }.filter { it.isNotEmpty() }.distinct().sortedDescending()
                    if (selectedYear.isEmpty() && years.isNotEmpty()) {
                        selectedYear = years.first()
                    }
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

    val years = remember(allRecords) {
        allRecords.map { it.year }.filter { it.isNotEmpty() }.distinct().sortedDescending()
    }

    val yearRecords = remember(allRecords, selectedYear) {
        allRecords.filter { it.year == selectedYear }
    }

    val nationalRecord = remember(yearRecords) {
        yearRecords.find { it.state == "Malaysia" }
    }

    val stateRecords = remember(yearRecords, sortByIncome) {
        val states = yearRecords.filter { it.state != "Malaysia" }
        if (sortByIncome) {
            states.sortedByDescending { it.incomeMean ?: 0.0 }
        } else {
            states.sortedBy { it.state ?: "" }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MY Household Income") },
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
        bottomBar = {
            BannerAd(modifier = Modifier.fillMaxWidth())
        },
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) { paddingValues ->
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(16.dp))
                        Text("Loading household income data...")
                    }
                }
            }
            errorMessage != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            errorMessage ?: "Unknown error",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.error
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { loadData() }) {
                            Text("Retry")
                        }
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Year selector
                    item {
                        YearSelector(
                            years = years,
                            selectedYear = selectedYear,
                            onYearSelected = { selectedYear = it }
                        )
                    }

                    // National summary card
                    if (nationalRecord != null) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.primaryContainer
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(16.dp)
                                ) {
                                    Text(
                                        "Malaysia National ($selectedYear)",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Spacer(Modifier.height(12.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceEvenly
                                    ) {
                                        StatCard(
                                            label = "Mean Income",
                                            value = formatRM(nationalRecord.incomeMean),
                                            modifier = Modifier.weight(1f)
                                        )
                                        StatCard(
                                            label = "Median Income",
                                            value = formatRM(nationalRecord.incomeMedian),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Bar chart
                    if (stateRecords.isNotEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        "Mean Income by State",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    val chartData = stateRecords
                                        .sortedByDescending { it.incomeMean ?: 0.0 }
                                        .map { record ->
                                            ChartPoint(
                                                label = record.state?.take(10) ?: "-",
                                                value = (record.incomeMean ?: 0.0).toFloat()
                                            )
                                        }
                                    SimpleBarChart(
                                        data = chartData,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(280.dp),
                                        barColor = MaterialTheme.colorScheme.primary,
                                        maxBars = 16,
                                        yAxisLabel = "RM"
                                    )
                                }
                            }
                        }

                        // Sort toggle
                        item {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "${stateRecords.size} States",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                TextButton(onClick = { sortByIncome = !sortByIncome }) {
                                    Icon(
                                        Icons.Filled.SortByAlpha,
                                        contentDescription = "Sort",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text(if (sortByIncome) "By Income" else "By Name")
                                }
                            }
                        }

                        // State cards
                        items(stateRecords, key = { it.state ?: "" }) { record ->
                            StateIncomeCard(record, ::formatRM)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun YearSelector(
    years: List<String>,
    selectedYear: String,
    onYearSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = selectedYear,
            onValueChange = {},
            readOnly = true,
            label = { Text("Year") },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            years.forEach { year ->
                DropdownMenuItem(
                    text = { Text(year) },
                    onClick = {
                        onYearSelected(year)
                        expanded = false
                    },
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun StateIncomeCard(
    record: HouseholdIncomeRecord,
    formatRM: (Double?) -> String
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                record.state ?: "-",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Mean Income",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        formatRM(record.incomeMean),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        "Median Income",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        formatRM(record.incomeMedian),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
