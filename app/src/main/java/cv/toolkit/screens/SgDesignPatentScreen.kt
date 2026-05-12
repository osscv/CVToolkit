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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.gson.Gson
import cv.toolkit.ads.BannerAd
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit

private const val API_KEY = "v2:3628e11212324251b93eccb69f46844aa70b7877ab736bf17f2f75deb7c19c0b:bmc3TaMdW9V-4pdcz5quf7qP-4VCHR2B"
private const val DESIGNS_URL = "https://api.data.gov.sg/v1/technology/ipos/designs"
private const val PATENTS_URL = "https://api.data.gov.sg/v1/technology/ipos/patents"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SgDesignPatentScreen(navController: NavController) {
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var showDatePicker by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) }

    var designCount by remember { mutableIntStateOf(0) }
    var designItems by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var patentCount by remember { mutableIntStateOf(0) }
    var patentItems by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
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

    fun fetchData(date: LocalDate) {
        scope.launch {
            isLoading = true
            errorMessage = null
            hasSearched = true
            try {
                val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                val url = if (selectedTab == 0) "$DESIGNS_URL?lodgement_date=$dateStr"
                else "$PATENTS_URL?lodgement_date=$dateStr"

                val body = withContext(Dispatchers.IO) {
                    val request = Request.Builder()
                        .url(url)
                        .addHeader("x-api-key", API_KEY)
                        .build()
                    client.newCall(request).execute().use { r ->
                        if (!r.isSuccessful) throw Exception("HTTP ${r.code}")
                        r.body.string()
                    }
                }

                @Suppress("UNCHECKED_CAST")
                val json = gson.fromJson(body, Map::class.java) as Map<String, Any>
                val count = (json["count"] as? Double)?.toInt() ?: 0
                @Suppress("UNCHECKED_CAST")
                val items = (json["items"] as? List<Map<String, Any>>) ?: emptyList()

                if (selectedTab == 0) {
                    designCount = count
                    designItems = items
                } else {
                    patentCount = count
                    patentItems = items
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to fetch data"
            }
            isLoading = false
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate
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
                        fetchData(date)
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
                title = { Text("SG IP Applications") },
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
        bottomBar = { BannerAd(modifier = Modifier.fillMaxWidth()) },
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab row
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = {
                        selectedTab = 0
                        if (hasSearched) fetchData(selectedDate)
                    },
                    text = { Text("Designs") },
                    icon = { Icon(Icons.Filled.Brush, null) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = {
                        selectedTab = 1
                        if (hasSearched) fetchData(selectedDate)
                    },
                    text = { Text("Patents") },
                    icon = { Icon(Icons.Filled.Science, null) }
                )
            }

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
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Info, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Browse Singapore IPOS design and patent applications by lodgement date.",
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
                        Text("Lodgement Date", style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(8.dp))
                        OutlinedButton(
                            onClick = { showDatePicker = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.CalendarMonth, null)
                            Spacer(Modifier.width(8.dp))
                            Text(selectedDate.format(dateFormatter))
                        }
                        Spacer(Modifier.height(8.dp))
                        Button(
                            onClick = { fetchData(selectedDate) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Search, null)
                            Spacer(Modifier.width(8.dp))
                            Text("Search")
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

                // Results
                if (hasSearched && !isLoading && errorMessage == null) {
                    val count = if (selectedTab == 0) designCount else patentCount
                    val items = if (selectedTab == 0) designItems else patentItems
                    val label = if (selectedTab == 0) "design" else "patent"

                    // Count card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "$count",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                "$label application${if (count != 1) "s" else ""} found",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }

                    // Application cards
                    items.forEach { item ->
                        ApplicationCard(item)
                    }

                    if (items.isEmpty() && count == 0) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "No ${label}s found for this date.",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }

                // Initial state
                if (!hasSearched) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Filled.Search,
                                null,
                                Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Pick a date and search",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ApplicationCard(item: Map<String, Any>) {
    @Suppress("UNCHECKED_CAST")
    val summary = item["summary"] as? Map<String, Any> ?: return
    val appNum = summary["applicationNum"] as? String ?: "N/A"
    val filingDate = summary["filingDate"] as? String ?: "N/A"
    val status = summary["status"] as? String ?: "Unknown"
    val classSubClass = summary["classSubClass"] as? String
    val approvedDate = summary["approvedDate"] as? String
    val expiryDate = summary["expiryDate"] as? String
    val novelty = item["statementOfNovelty"] as? String

    val statusColor = when {
        status.equals("Registered", ignoreCase = true) -> Color(0xFF2E7D32)
        status.equals("Pending", ignoreCase = true) -> Color(0xFFF9A825)
        status.equals("Lapsed", ignoreCase = true) -> Color(0xFFC62828)
        else -> Color.Gray
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    appNum,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = statusColor.copy(alpha = 0.15f)
                ) {
                    Text(
                        status,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))

            DetailRow("Filing Date", filingDate)
            classSubClass?.let { DetailRow("Class", it) }
            approvedDate?.let { if (it.isNotBlank()) DetailRow("Approved", it) }
            expiryDate?.let { if (it.isNotBlank()) DetailRow("Expiry", it) }

            novelty?.let {
                if (it.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Statement of Novelty",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
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
