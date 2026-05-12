package cv.toolkit.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import cv.toolkit.ads.BannerAd
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale
import java.util.concurrent.TimeUnit

private data class BirthRecord(
    val date: String?,
    val state: String?,
    val births: Int?
)

private data class AnnualBirthStat(
    val date: String?,
    val abs: Long?,
    val rate: Double?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BirthdayExplorerScreen(navController: NavController) {
    var selectedDate by remember { mutableStateOf<LocalDate?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    var birthData by remember { mutableStateOf<BirthRecord?>(null) }
    var yearStat by remember { mutableStateOf<AnnualBirthStat?>(null) }
    var sameDayAcrossYears by remember { mutableStateOf<List<BirthRecord>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var hasSearched by remember { mutableStateOf(false) }

    val client = remember {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }
    val gson = remember { Gson() }
    val scope = rememberCoroutineScope()
    val dateFormatter = remember { DateTimeFormatter.ofPattern("d MMMM yyyy", Locale.ENGLISH) }
    val monthFormatter = remember { DateTimeFormatter.ofPattern("MMMM", Locale.ENGLISH) }
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.US) }

    fun searchBirthday(date: LocalDate) {
        scope.launch {
            isLoading = true
            errorMessage = null
            birthData = null
            yearStat = null
            sameDayAcrossYears = emptyList()
            hasSearched = true
            try {
                val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                val yearStr = "${date.year}-01-01"
                val monthDay = date.format(DateTimeFormatter.ofPattern("-MM-dd"))

                val (dailyBody, annualBody, sameDayBody) = withContext(Dispatchers.IO) {
                    val dailyDeferred = async {
                        val request = Request.Builder()
                            .url("https://api.data.gov.my/data-catalogue?id=births&filter=$dateStr@date,Malaysia@state&limit=1")
                            .build()
                        client.newCall(request).execute().use { r ->
                            if (!r.isSuccessful) throw Exception("HTTP ${r.code}")
                            r.body.string()
                        }
                    }
                    val annualDeferred = async {
                        val request = Request.Builder()
                            .url("https://api.data.gov.my/data-catalogue?id=births_annual&filter=$yearStr@date&limit=1")
                            .build()
                        client.newCall(request).execute().use { r ->
                            if (!r.isSuccessful) ""
                            else r.body.string()
                        }
                    }
                    // Fetch same month-day across recent years for comparison
                    val sameDayDeferred = async {
                        val years = (2013..2022).map { "$it$monthDay" }
                        val results = mutableListOf<BirthRecord>()
                        for (y in years) {
                            try {
                                val request = Request.Builder()
                                    .url("https://api.data.gov.my/data-catalogue?id=births&filter=$y@date,Malaysia@state&limit=1")
                                    .build()
                                val resp = client.newCall(request).execute().use { r ->
                                    if (r.isSuccessful) r.body.string() else "[]"
                                }
                                val type = object : TypeToken<List<BirthRecord>>() {}.type
                                val recs: List<BirthRecord> = gson.fromJson(resp, type)
                                results.addAll(recs)
                            } catch (_: Exception) { }
                        }
                        results
                    }
                    Triple(dailyDeferred.await(), annualDeferred.await(), sameDayDeferred.await())
                }

                val type = object : TypeToken<List<BirthRecord>>() {}.type
                val records: List<BirthRecord> = gson.fromJson(dailyBody, type)
                birthData = records.firstOrNull()

                if (annualBody.isNotEmpty()) {
                    val annualType = object : TypeToken<List<AnnualBirthStat>>() {}.type
                    val annualRecords: List<AnnualBirthStat> = gson.fromJson(annualBody, annualType)
                    yearStat = annualRecords.firstOrNull()
                }

                sameDayAcrossYears = sameDayBody.sortedBy { it.date }

                if (birthData == null) {
                    errorMessage = "No birth data available for this date. Data covers 1920-2022."
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to fetch data"
            }
            isLoading = false
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = (selectedDate ?: LocalDate.of(2000, 1, 1))
                .atStartOfDay(java.time.ZoneOffset.UTC)
                .toInstant()
                .toEpochMilli()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        val date = java.time.Instant.ofEpochMilli(millis)
                            .atZone(java.time.ZoneOffset.UTC)
                            .toLocalDate()
                        selectedDate = date
                        searchBirthday(date)
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Birthday Explorer") },
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
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Info card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Info, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Discover how many Malaysians share your birthday! Data from JPN (1920-2022).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }

                // Date picker card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Select your birthday", style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { showDatePicker = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.CalendarMonth, null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (selectedDate != null) selectedDate!!.format(dateFormatter)
                                else "Choose a date"
                            )
                        }
                    }
                }

                // Loading
                AnimatedVisibility(visible = isLoading) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    }
                }

                // Error
                errorMessage?.let { error ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Warning, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onErrorContainer)
                            Spacer(Modifier.width(8.dp))
                            Text(error, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }

                // Result
                if (birthData != null && errorMessage == null && selectedDate != null) {
                    val record = birthData!!
                    val date = selectedDate!!

                    // Birth count card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Filled.Cake, null, Modifier.size(40.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                numberFormat.format(record.births ?: 0),
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                "babies were born on this date",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }

                    // Age card
                    val today = LocalDate.now()
                    if (date.isBefore(today) || date.isEqual(today)) {
                        val years = ChronoUnit.YEARS.between(date, today)
                        val afterYears = date.plusYears(years)
                        val months = ChronoUnit.MONTHS.between(afterYears, today)
                        val afterMonths = afterYears.plusMonths(months)
                        val days = ChronoUnit.DAYS.between(afterMonths, today)

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("Your Age", style = MaterialTheme.typography.labelLarge)
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    AgeUnit("$years", "Years")
                                    AgeUnit("$months", "Months")
                                    AgeUnit("$days", "Days")
                                }
                                Spacer(Modifier.height(8.dp))
                                val totalDays = ChronoUnit.DAYS.between(date, today)
                                Text(
                                    "That's ${numberFormat.format(totalDays)} days!",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    // Year summary card
                    if (yearStat != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    "Year ${date.year} Summary",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            numberFormat.format(yearStat!!.abs ?: 0),
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                        Text(
                                            "Total Births",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                        )
                                    }
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            "%.1f".format(yearStat!!.rate ?: 0.0),
                                            style = MaterialTheme.typography.titleLarge,
                                            fontWeight = FontWeight.Bold,
                                            fontFamily = FontFamily.Monospace,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                        Text(
                                            "per 1,000 population",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                                Spacer(Modifier.height(8.dp))
                                val dailyAvg = (yearStat!!.abs ?: 0) / 365
                                Text(
                                    "Daily average: ~${numberFormat.format(dailyAvg)} births/day",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }

                    // Same day across years chart
                    if (sameDayAcrossYears.size >= 2) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Births on ${date.format(monthFormatter)} ${date.dayOfMonth} Across Years",
                                    style = MaterialTheme.typography.labelLarge
                                )
                                Spacer(Modifier.height(8.dp))
                                val barData = sameDayAcrossYears.mapNotNull { r ->
                                    r.date?.let { d ->
                                        ChartPoint(d.substring(0, 4), (r.births ?: 0).toFloat())
                                    }
                                }
                                SimpleBarChart(
                                    data = barData,
                                    modifier = Modifier.fillMaxWidth(),
                                    barColor = MaterialTheme.colorScheme.primary,
                                    maxBars = 10,
                                    yAxisLabel = "Births"
                                )
                            }
                        }
                    }

                    // Date details
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Date Details", style = MaterialTheme.typography.labelLarge)
                            Spacer(Modifier.height(8.dp))
                            DetailRow("Date", date.format(dateFormatter))
                            DetailRow("Day of Week", date.dayOfWeek.name.lowercase()
                                .replaceFirstChar { it.uppercase() })
                            DetailRow("Day of Year", "${date.dayOfYear} / ${if (date.isLeapYear) 366 else 365}")
                            DetailRow("Week of Year", "${(date.dayOfYear + 6) / 7}")
                        }
                    }
                }

                if (!hasSearched) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Filled.Cake,
                                null,
                                Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Pick a date to explore",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }

            BannerAd(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun AgeUnit(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.primary
        )
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
    }
}
