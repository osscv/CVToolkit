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
import java.util.concurrent.TimeUnit

private data class CurrencyOption(val code: String, val name: String)

private val CURRENCY_OPTIONS = listOf(
    CurrencyOption("usd", "US Dollar (USD)"),
    CurrencyOption("sgd", "Singapore Dollar (SGD)"),
    CurrencyOption("gbp", "British Pound (GBP)"),
    CurrencyOption("eur", "Euro (EUR)"),
    CurrencyOption("jpy", "Japanese Yen (JPY)"),
    CurrencyOption("aud", "Australian Dollar (AUD)"),
    CurrencyOption("cny", "Chinese Yuan (CNY)"),
    CurrencyOption("thb", "Thai Baht (THB)"),
    CurrencyOption("idr", "Indonesian Rupiah (IDR)"),
    CurrencyOption("krw", "South Korean Won (KRW)"),
    CurrencyOption("hkd", "Hong Kong Dollar (HKD)"),
    CurrencyOption("twd", "Taiwan Dollar (TWD)"),
    CurrencyOption("inr", "Indian Rupee (INR)"),
    CurrencyOption("sar", "Saudi Riyal (SAR)"),
    CurrencyOption("chf", "Swiss Franc (CHF)")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExchangeRateScreen(navController: NavController) {
    val scope = rememberCoroutineScope()
    var records by remember { mutableStateOf<List<Map<String, Any?>>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedCurrency by remember { mutableStateOf(CURRENCY_OPTIONS.first()) }
    var dropdownExpanded by remember { mutableStateOf(false) }

    // Fetch data
    LaunchedEffect(Unit) {
        scope.launch {
            try {
                val data = withContext(Dispatchers.IO) {
                    val client = OkHttpClient.Builder()
                        .connectTimeout(15, TimeUnit.SECONDS)
                        .readTimeout(15, TimeUnit.SECONDS)
                        .build()
                    val request = Request.Builder()
                        .url("https://api.data.gov.my/data-catalogue?id=exchangerates&sort=-date&limit=120&filter=avg@indicator")
                        .build()
                    val response = client.newCall(request).execute()
                    val body = response.body.string()
                    val type = object : TypeToken<List<Map<String, Any?>>>() {}.type
                    Gson().fromJson<List<Map<String, Any?>>>(body, type)
                }
                records = data
                isLoading = false
            } catch (e: Exception) {
                error = e.message ?: "Failed to fetch exchange rates"
                isLoading = false
            }
        }
    }

    // Extract rates for selected currency (chronological - oldest first)
    val ratesForCurrency = remember(records, selectedCurrency) {
        records.reversed().mapNotNull { record ->
            val date = record["date"] as? String ?: return@mapNotNull null
            val value = record[selectedCurrency.code]
            val rate = when (value) {
                is Double -> value
                is Number -> value.toDouble()
                else -> null
            }
            if (rate != null) Triple(date, rate, record) else null
        }
    }

    val latestRate = remember(ratesForCurrency) {
        ratesForCurrency.lastOrNull()
    }

    val previousRate = remember(ratesForCurrency) {
        if (ratesForCurrency.size >= 2) ratesForCurrency[ratesForCurrency.size - 2] else null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("BNM Exchange Rates") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        bottomBar = { BannerAd() }
    ) { padding ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (error != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Error", style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(8.dp))
                    Text(error!!, color = MaterialTheme.colorScheme.error)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Currency selector
                item {
                    Text(
                        "Currency",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(4.dp))
                    ExposedDropdownMenuBox(
                        expanded = dropdownExpanded,
                        onExpandedChange = { dropdownExpanded = !dropdownExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedCurrency.name,
                            onValueChange = {},
                            readOnly = true,
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = dropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                                .fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = dropdownExpanded,
                            onDismissRequest = { dropdownExpanded = false }
                        ) {
                            CURRENCY_OPTIONS.forEach { currency ->
                                DropdownMenuItem(
                                    text = { Text(currency.name) },
                                    onClick = {
                                        selectedCurrency = currency
                                        dropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Current rate card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "1 ${selectedCurrency.code.uppercase()} =",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(Modifier.height(4.dp))
                            if (latestRate != null) {
                                Text(
                                    "MYR ${String.format("%.4f", latestRate.second)}",
                                    style = MaterialTheme.typography.headlineLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(Modifier.height(4.dp))

                                // Change indicator
                                if (previousRate != null) {
                                    val change = latestRate.second - previousRate.second
                                    val changePercent = (change / previousRate.second) * 100
                                    val changeColor = when {
                                        change > 0 -> Color(0xFF4CAF50)
                                        change < 0 -> Color(0xFFF44336)
                                        else -> MaterialTheme.colorScheme.onPrimaryContainer
                                    }
                                    val arrow = when {
                                        change > 0 -> "\u25B2"
                                        change < 0 -> "\u25BC"
                                        else -> ""
                                    }
                                    Text(
                                        "$arrow ${String.format("%.4f", change)} (${String.format("%.2f", changePercent)}%)",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = changeColor
                                    )
                                }

                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Date: ${latestRate.first}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                )
                            } else {
                                Text(
                                    "No data available",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }

                // Line chart
                if (ratesForCurrency.size >= 2) {
                    item {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "MYR / ${selectedCurrency.code.uppercase()} Trend",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(8.dp))
                                val chartPoints = ratesForCurrency.map { (date, rate, _) ->
                                    // Show abbreviated date as label (e.g. "Jan 97")
                                    val label = formatChartDate(date)
                                    ChartPoint(label, rate.toFloat())
                                }
                                val line = ChartLine(
                                    label = selectedCurrency.code.uppercase(),
                                    points = chartPoints,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                SimpleLineChart(
                                    lines = listOf(line),
                                    modifier = Modifier.fillMaxWidth(),
                                    yAxisLabel = "MYR per 1 ${selectedCurrency.code.uppercase()}"
                                )
                            }
                        }
                    }
                }

                // History header
                if (ratesForCurrency.isNotEmpty()) {
                    item {
                        Text(
                            "Monthly History",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // History list (newest first for the list)
                    items(ratesForCurrency.reversed()) { (date, rate, _) ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    date,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    "MYR ${String.format("%.4f", rate)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatChartDate(date: String): String {
    // date format: "1997-01-01" -> "Jan 97"
    return try {
        val parts = date.split("-")
        val year = parts[0].takeLast(2)
        val month = when (parts[1]) {
            "01" -> "Jan"
            "02" -> "Feb"
            "03" -> "Mar"
            "04" -> "Apr"
            "05" -> "May"
            "06" -> "Jun"
            "07" -> "Jul"
            "08" -> "Aug"
            "09" -> "Sep"
            "10" -> "Oct"
            "11" -> "Nov"
            "12" -> "Dec"
            else -> parts[1]
        }
        "$month $year"
    } catch (e: Exception) {
        date
    }
}
