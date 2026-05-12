package cv.toolkit.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import cv.toolkit.ads.BannerAd
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

private data class ExchangeRateResponse(
    @SerializedName("result") val result: String?,
    @SerializedName("base_code") val baseCode: String?,
    @SerializedName("time_last_update_utc") val timeLastUpdateUtc: String?,
    @SerializedName("rates") val rates: Map<String, Double>?
)

private data class CachedRates(
    val base: String,
    val rates: Map<String, Double>,
    val lastUpdated: String,
    val fetchedAt: Long
)

private data class CurrencyInfo(val code: String, val name: String, val flag: String)

private val CURRENCIES = listOf(
    CurrencyInfo("USD", "US Dollar", "\uD83C\uDDFA\uD83C\uDDF8"),
    CurrencyInfo("EUR", "Euro", "\uD83C\uDDEA\uD83C\uDDFA"),
    CurrencyInfo("GBP", "British Pound", "\uD83C\uDDEC\uD83C\uDDE7"),
    CurrencyInfo("JPY", "Japanese Yen", "\uD83C\uDDEF\uD83C\uDDF5"),
    CurrencyInfo("CNY", "Chinese Yuan", "\uD83C\uDDE8\uD83C\uDDF3"),
    CurrencyInfo("KRW", "South Korean Won", "\uD83C\uDDF0\uD83C\uDDF7"),
    CurrencyInfo("AUD", "Australian Dollar", "\uD83C\uDDE6\uD83C\uDDFA"),
    CurrencyInfo("CAD", "Canadian Dollar", "\uD83C\uDDE8\uD83C\uDDE6"),
    CurrencyInfo("CHF", "Swiss Franc", "\uD83C\uDDE8\uD83C\uDDED"),
    CurrencyInfo("HKD", "Hong Kong Dollar", "\uD83C\uDDED\uD83C\uDDF0"),
    CurrencyInfo("SGD", "Singapore Dollar", "\uD83C\uDDF8\uD83C\uDDEC"),
    CurrencyInfo("MYR", "Malaysian Ringgit", "\uD83C\uDDF2\uD83C\uDDFE"),
    CurrencyInfo("THB", "Thai Baht", "\uD83C\uDDF9\uD83C\uDDED"),
    CurrencyInfo("INR", "Indian Rupee", "\uD83C\uDDEE\uD83C\uDDF3"),
    CurrencyInfo("IDR", "Indonesian Rupiah", "\uD83C\uDDEE\uD83C\uDDE9"),
    CurrencyInfo("PHP", "Philippine Peso", "\uD83C\uDDF5\uD83C\uDDED"),
    CurrencyInfo("VND", "Vietnamese Dong", "\uD83C\uDDFB\uD83C\uDDF3"),
    CurrencyInfo("TWD", "Taiwan Dollar", "\uD83C\uDDF9\uD83C\uDDFC"),
    CurrencyInfo("BRL", "Brazilian Real", "\uD83C\uDDE7\uD83C\uDDF7"),
    CurrencyInfo("MXN", "Mexican Peso", "\uD83C\uDDF2\uD83C\uDDFD"),
    CurrencyInfo("RUB", "Russian Ruble", "\uD83C\uDDF7\uD83C\uDDFA"),
    CurrencyInfo("TRY", "Turkish Lira", "\uD83C\uDDF9\uD83C\uDDF7"),
    CurrencyInfo("ZAR", "South African Rand", "\uD83C\uDDFF\uD83C\uDDE6"),
    CurrencyInfo("SEK", "Swedish Krona", "\uD83C\uDDF8\uD83C\uDDEA"),
    CurrencyInfo("NOK", "Norwegian Krone", "\uD83C\uDDF3\uD83C\uDDF4"),
    CurrencyInfo("DKK", "Danish Krone", "\uD83C\uDDE9\uD83C\uDDF0"),
    CurrencyInfo("NZD", "New Zealand Dollar", "\uD83C\uDDF3\uD83C\uDDFF"),
    CurrencyInfo("SAR", "Saudi Riyal", "\uD83C\uDDF8\uD83C\uDDE6"),
    CurrencyInfo("AED", "UAE Dirham", "\uD83C\uDDE6\uD83C\uDDEA"),
    CurrencyInfo("KWD", "Kuwaiti Dinar", "\uD83C\uDDF0\uD83C\uDDFC")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyConverterScreen(navController: NavController) {
    var amount by remember { mutableStateOf("1") }
    var fromCurrency by remember { mutableStateOf(CURRENCIES[0]) }
    var toCurrency by remember { mutableStateOf(CURRENCIES[1]) }
    var convertedResult by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var lastUpdated by remember { mutableStateOf<String?>(null) }

    val ratesCache = remember { mutableMapOf<String, CachedRates>() }
    val client = remember {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }
    val gson = remember { Gson() }
    val scope = rememberCoroutineScope()

    fun convert() {
        val inputAmount = amount.toDoubleOrNull()
        if (inputAmount == null || inputAmount < 0) {
            convertedResult = null
            errorMessage = if (amount.isNotEmpty()) "Invalid amount" else null
            return
        }

        val base = fromCurrency.code
        val target = toCurrency.code

        // Check if same currency
        if (base == target) {
            convertedResult = formatCurrencyResult(inputAmount)
            errorMessage = null
            return
        }

        // Check cache (valid for 30 minutes)
        val cached = ratesCache[base]
        if (cached != null && (System.currentTimeMillis() - cached.fetchedAt) < 30 * 60 * 1000) {
            val rate = cached.rates[target]
            if (rate != null) {
                convertedResult = formatCurrencyResult(inputAmount * rate)
                lastUpdated = cached.lastUpdated
                errorMessage = null
                return
            }
        }

        // Fetch from API
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val response = withContext(Dispatchers.IO) {
                    val request = Request.Builder()
                        .url("https://open.er-api.com/v6/latest/$base")
                        .build()
                    client.newCall(request).execute()
                }

                val body = response.body.string()
                if (!response.isSuccessful) {
                    errorMessage = "Failed to fetch rates (HTTP ${response.code})"
                    isLoading = false
                    return@launch
                }

                val parsed = gson.fromJson(body, ExchangeRateResponse::class.java)
                if (parsed.result != "success" || parsed.rates == null) {
                    errorMessage = "API returned an error"
                    isLoading = false
                    return@launch
                }

                // Cache the rates
                val updatedTime = parsed.timeLastUpdateUtc ?: "Unknown"
                ratesCache[base] = CachedRates(
                    base = base,
                    rates = parsed.rates,
                    lastUpdated = updatedTime,
                    fetchedAt = System.currentTimeMillis()
                )

                val rate = parsed.rates[target]
                if (rate != null) {
                    convertedResult = formatCurrencyResult(inputAmount * rate)
                    lastUpdated = updatedTime
                } else {
                    errorMessage = "Rate for $target not found"
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Network error"
            }
            isLoading = false
        }
    }

    // Auto-convert when inputs change
    LaunchedEffect(amount, fromCurrency, toCurrency) {
        convert()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Currency Converter") },
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
                // Amount input
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Amount", style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = amount,
                            onValueChange = { newValue ->
                                amount = newValue.filter { c -> c.isDigit() || c == '.' }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            textStyle = LocalTextStyle.current.copy(
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Medium
                            ),
                            placeholder = { Text("Enter amount") },
                            trailingIcon = {
                                if (amount.isNotEmpty()) {
                                    IconButton(onClick = { amount = "" }) {
                                        Icon(Icons.Filled.Clear, "Clear")
                                    }
                                }
                            }
                        )
                    }
                }

                // From currency
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("From", style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(8.dp))
                        CurrencyDropdown(
                            selected = fromCurrency,
                            onSelect = { fromCurrency = it }
                        )
                    }
                }

                // Swap button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    FilledIconButton(
                        onClick = {
                            val temp = fromCurrency
                            fromCurrency = toCurrency
                            toCurrency = temp
                        }
                    ) {
                        Icon(Icons.Filled.SwapVert, "Swap currencies")
                    }
                }

                // To currency
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("To", style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(8.dp))
                        CurrencyDropdown(
                            selected = toCurrency,
                            onSelect = { toCurrency = it }
                        )
                    }
                }

                // Loading indicator
                AnimatedVisibility(visible = isLoading) {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(32.dp))
                    }
                }

                // Error message
                errorMessage?.let { error ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onErrorContainer,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                error,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                // Result card
                if (convertedResult != null && errorMessage == null) {
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
                                "Result",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(Modifier.height(8.dp))

                            val displayAmount = amount.toDoubleOrNull()?.let { formatCurrencyResult(it) } ?: amount
                            Text(
                                "$displayAmount ${fromCurrency.code}",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                "=",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                            )
                            Text(
                                "$convertedResult ${toCurrency.code}",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )

                            Spacer(Modifier.height(4.dp))
                            Text(
                                "${toCurrency.flag} ${toCurrency.name}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                // Last updated
                lastUpdated?.let { timestamp ->
                    Text(
                        "Rates last updated: $timestamp",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            BannerAd(modifier = Modifier.fillMaxWidth())
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CurrencyDropdown(
    selected: CurrencyInfo,
    onSelect: (CurrencyInfo) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredCurrencies = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            CURRENCIES
        } else {
            val query = searchQuery.lowercase()
            CURRENCIES.filter {
                it.code.lowercase().contains(query) || it.name.lowercase().contains(query)
            }
        }
    }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it }
    ) {
        OutlinedTextField(
            value = "${selected.flag} ${selected.code} - ${selected.name}",
            onValueChange = {},
            readOnly = true,
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable),
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) }
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = {
                expanded = false
                searchQuery = ""
            }
        ) {
            // Search field inside dropdown
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                placeholder = { Text("Search currency...") },
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Clear, "Clear", modifier = Modifier.size(18.dp))
                        }
                    }
                },
                textStyle = MaterialTheme.typography.bodySmall
            )

            filteredCurrencies.forEach { currency ->
                DropdownMenuItem(
                    text = {
                        Text("${currency.flag} ${currency.code} - ${currency.name}")
                    },
                    onClick = {
                        onSelect(currency)
                        expanded = false
                        searchQuery = ""
                    },
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            if (filteredCurrencies.isEmpty()) {
                DropdownMenuItem(
                    text = {
                        Text(
                            "No currencies found",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    },
                    onClick = {},
                    enabled = false
                )
            }
        }
    }
}

private fun formatCurrencyResult(value: Double): String {
    return when {
        value == 0.0 -> "0"
        kotlin.math.abs(value) >= 1e12 -> String.format("%.2e", value)
        value == value.toLong().toDouble() && kotlin.math.abs(value) < 1e15 -> {
            String.format("%,.0f", value)
        }
        kotlin.math.abs(value) >= 1.0 -> String.format("%,.2f", value)
        kotlin.math.abs(value) >= 0.01 -> String.format("%.4f", value)
        else -> String.format("%.6f", value)
    }
}
