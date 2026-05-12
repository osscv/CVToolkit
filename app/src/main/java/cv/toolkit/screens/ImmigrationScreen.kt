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

private data class ArrivalRecord(
    val date: String?,
    val country: String?,
    val arrivals: Long?,
    val arrivals_male: Long?,
    val arrivals_female: Long?
)

private data class CountryInfo(val code: String, val name: String)

private val POPULAR_COUNTRIES = listOf(
    CountryInfo("ALL", "All Countries"),
    CountryInfo("SGP", "Singapore"),
    CountryInfo("IDN", "Indonesia"),
    CountryInfo("CHN", "China"),
    CountryInfo("THA", "Thailand"),
    CountryInfo("IND", "India"),
    CountryInfo("JPN", "Japan"),
    CountryInfo("KOR", "South Korea"),
    CountryInfo("GBR", "United Kingdom"),
    CountryInfo("USA", "United States"),
    CountryInfo("AUS", "Australia"),
    CountryInfo("BRN", "Brunei"),
    CountryInfo("PHL", "Philippines"),
    CountryInfo("VNM", "Vietnam"),
    CountryInfo("BGD", "Bangladesh"),
    CountryInfo("MMR", "Myanmar"),
    CountryInfo("SAU", "Saudi Arabia"),
    CountryInfo("DEU", "Germany"),
    CountryInfo("FRA", "France"),
    CountryInfo("TWN", "Taiwan")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImmigrationScreen(navController: NavController) {
    var selectedCountry by remember { mutableStateOf(POPULAR_COUNTRIES[0]) }
    var records by remember { mutableStateOf<List<ArrivalRecord>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val client = remember {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    val gson = remember { Gson() }
    val scope = rememberCoroutineScope()
    val numberFormat = remember { NumberFormat.getNumberInstance(Locale.US) }

    fun loadData(country: CountryInfo) {
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val url = buildString {
                    append("https://api.data.gov.my/data-catalogue?id=arrivals")
                    append("&filter=${country.code}@country")
                    append("&sort=-date")
                    append("&limit=60")
                }
                val body = withContext(Dispatchers.IO) {
                    val request = Request.Builder().url(url).build()
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) throw Exception("HTTP ${response.code}")
                        response.body.string()
                    }
                }
                val type = object : TypeToken<List<ArrivalRecord>>() {}.type
                records = gson.fromJson(body, type)
                if (records.isEmpty()) {
                    errorMessage = "No data available for ${country.name}"
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to fetch data"
            }
            isLoading = false
        }
    }

    // Load on first launch and country change
    LaunchedEffect(selectedCountry) {
        loadData(selectedCountry)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MY Immigration") },
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
            // Country selector
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Nationality", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(6.dp))
                    CountryDropdown(
                        selected = selectedCountry,
                        onSelect = { selectedCountry = it }
                    )
                }
            }

            // Summary card
            if (records.isNotEmpty() && errorMessage == null) {
                val totalArrivals = records.sumOf { it.arrivals ?: 0 }
                val totalMale = records.sumOf { it.arrivals_male ?: 0 }
                val totalFemale = records.sumOf { it.arrivals_female ?: 0 }
                val latestDate = records.firstOrNull()?.date ?: "-"
                val earliestDate = records.lastOrNull()?.date ?: "-"

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            numberFormat.format(totalArrivals),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "total arrivals shown",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.Male, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text(
                                    numberFormat.format(totalMale),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text("Male", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.Female, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text(
                                    numberFormat.format(totalFemale),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text("Female", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                            }
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "$earliestDate to $latestDate",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                        )
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

            // Monthly records list
            if (records.isNotEmpty() && errorMessage == null && !isLoading) {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(records, key = { "${it.date}_${it.country}" }) { record ->
                        ArrivalCard(record, numberFormat)
                    }
                }
            } else if (!isLoading && errorMessage == null) {
                Spacer(Modifier.weight(1f))
            }

            BannerAd(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ArrivalCard(record: ArrivalRecord, numberFormat: NumberFormat) {
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
            Column {
                Text(
                    record.date?.substring(0, 7) ?: "-",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                if (record.arrivals_male != null && record.arrivals_female != null) {
                    Text(
                        "M: ${numberFormat.format(record.arrivals_male)}  F: ${numberFormat.format(record.arrivals_female)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                numberFormat.format(record.arrivals ?: 0),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CountryDropdown(
    selected: CountryInfo,
    onSelect: (CountryInfo) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val filtered = remember(searchQuery) {
        if (searchQuery.isBlank()) POPULAR_COUNTRIES
        else {
            val query = searchQuery.lowercase()
            POPULAR_COUNTRIES.filter {
                it.code.lowercase().contains(query) || it.name.lowercase().contains(query)
            }
        }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = "${selected.code} - ${selected.name}",
            onValueChange = {},
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false; searchQuery = "" }
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                placeholder = { Text("Search country...") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, null, Modifier.size(18.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Clear, "Clear", Modifier.size(18.dp))
                        }
                    }
                },
                textStyle = MaterialTheme.typography.bodySmall
            )

            filtered.forEach { country ->
                DropdownMenuItem(
                    text = { Text("${country.code} - ${country.name}") },
                    onClick = {
                        onSelect(country)
                        expanded = false
                        searchQuery = ""
                    },
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            if (filtered.isEmpty()) {
                DropdownMenuItem(
                    text = { Text("No countries found", color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall) },
                    onClick = {},
                    enabled = false
                )
            }
        }
    }
}
