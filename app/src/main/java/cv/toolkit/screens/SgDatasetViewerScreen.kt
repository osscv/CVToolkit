package cv.toolkit.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

private data class DsField(val type: String?, val id: String)
private data class DsResult(val fields: List<DsField>?, val records: List<Map<String, Any?>>?, val total: Int?)
private data class DsResponse(val success: Boolean, val result: DsResult?)

private fun formatValue(value: Any?, nf: NumberFormat): String = when (value) {
    null -> "—"
    is Double -> if (value == value.toLong().toDouble() && kotlin.math.abs(value) < 1e15) nf.format(value.toLong())
                 else if (kotlin.math.abs(value) >= 1.0) String.format("%.2f", value)
                 else String.format("%.4f", value)
    is Number -> nf.format(value)
    else -> value.toString()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SgDatasetViewerScreen(
    navController: NavController,
    datasetId: String,
    datasetName: String
) {
    val scope = rememberCoroutineScope()
    val pageSize = 50

    var records by remember { mutableStateOf<List<Map<String, Any?>>>(emptyList()) }
    var fields by remember { mutableStateOf<List<DsField>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var offset by remember { mutableIntStateOf(0) }
    var total by remember { mutableIntStateOf(0) }
    var showChart by remember { mutableStateOf(true) }
    var sortColumn by remember { mutableStateOf<String?>(null) }
    var sortAscending by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var filterColumn by remember { mutableStateOf<String?>(null) }
    var filterValue by remember { mutableStateOf<String?>(null) }

    val client = remember {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }
    val gson = remember { Gson() }
    val nf = remember { NumberFormat.getNumberInstance(Locale.US) }

    fun loadData(newOffset: Int, append: Boolean = false) {
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val body = withContext(Dispatchers.IO) {
                    val url = "https://data.gov.sg/api/action/datastore_search?resource_id=$datasetId&limit=$pageSize&offset=$newOffset"
                    val request = Request.Builder().url(url).build()
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) throw Exception("HTTP ${response.code}")
                        response.body.string()
                    }
                }
                val parsed = gson.fromJson(body, DsResponse::class.java)
                if (parsed.success && parsed.result != null) {
                    val newFields = parsed.result.fields ?: emptyList()
                    val newRecords = parsed.result.records ?: emptyList()
                    if (newFields.isNotEmpty()) fields = newFields
                    records = if (append) records + newRecords else newRecords
                    total = parsed.result.total ?: 0
                    offset = newOffset + newRecords.size
                } else {
                    errorMessage = "Failed to load dataset"
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Unknown error"
            }
            isLoading = false
        }
    }

    LaunchedEffect(datasetId) { loadData(0) }

    val columns = remember(fields) { fields.map { it.id }.filter { it != "_id" } }
    val hasMore = offset < total

    // Detect column types
    val dateColumn = remember(columns) {
        columns.firstOrNull { col ->
            val l = col.lowercase()
            l.contains("date") || l.contains("year") || l.contains("month") || l == "period" || l == "quarter" || l.contains("time")
        }
    }
    val numericColumns = remember(columns, records) {
        if (records.isEmpty()) emptyList()
        else columns.filter { col ->
            records.any { r -> val v = r[col]; v is Number || (v is String && v.toDoubleOrNull() != null) }
        }.filter { it != dateColumn }
    }
    val textColumns = remember(columns, numericColumns) {
        columns.filter { it !in numericColumns && it != dateColumn }
    }

    // Distinct values for filter dropdown (text columns only, max 20 unique values)
    val filterOptions = remember(records, textColumns) {
        textColumns.associateWith { col ->
            records.mapNotNull { it[col]?.toString() }.distinct().sorted().take(20)
        }.filter { it.value.size in 2..20 }
    }

    // Apply search + filter
    val filteredRecords = remember(records, searchQuery, filterColumn, filterValue) {
        var result = records
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.lowercase()
            result = result.filter { r -> r.values.any { it?.toString()?.lowercase()?.contains(q) == true } }
        }
        if (filterColumn != null && filterValue != null) {
            result = result.filter { it[filterColumn]?.toString() == filterValue }
        }
        result
    }

    // Sort
    val sortedRecords = remember(filteredRecords, sortColumn, sortAscending) {
        if (sortColumn == null) filteredRecords
        else {
            val col = sortColumn!!
            val sorted = filteredRecords.sortedWith(compareBy {
                val v = it[col]
                when (v) {
                    is Number -> v.toDouble()
                    is String -> v.toDoubleOrNull() ?: v.lowercase().hashCode().toDouble()
                    else -> 0.0
                }
            })
            if (sortAscending) sorted else sorted.reversed()
        }
    }

    // Summary stats with min/max/latest
    data class ColStat(val col: String, val avg: Double, val min: Double, val max: Double, val latest: Double, val count: Int)
    val colStats = remember(filteredRecords, numericColumns) {
        numericColumns.take(4).mapNotNull { col ->
            val values = filteredRecords.mapNotNull { r ->
                val v = r[col]; when (v) { is Number -> v.toDouble(); is String -> v.toDoubleOrNull(); else -> null }
            }
            if (values.isNotEmpty()) ColStat(col, values.average(), values.min(), values.max(), values.last(), values.size)
            else null
        }
    }

    // Line chart
    val chartLines = remember(filteredRecords, dateColumn, numericColumns) {
        if (dateColumn == null || numericColumns.isEmpty() || filteredRecords.size < 2) emptyList()
        else {
            val chrono = filteredRecords.sortedBy { it[dateColumn]?.toString() ?: "" }
            val colors = listOf(Color(0xFF6750A4), Color(0xFF7D5260), Color(0xFF006D40), Color(0xFFB3261E), Color(0xFF625B71))
            numericColumns.take(3).mapIndexed { i, col ->
                ChartLine(col, chrono.mapNotNull { r ->
                    val lbl = r[dateColumn]?.toString()?.let { if (it.length >= 7) it.substring(0, 7) else it } ?: return@mapNotNull null
                    val v = r[col]?.let { when (it) { is Number -> it.toFloat(); is String -> it.toFloatOrNull(); else -> null } } ?: return@mapNotNull null
                    ChartPoint(lbl, v)
                }, colors[i % colors.size])
            }.filter { it.points.size >= 2 }
        }
    }

    // Bar chart
    val barData = remember(filteredRecords, numericColumns, dateColumn, columns) {
        if (numericColumns.isEmpty() || filteredRecords.size < 2) emptyList()
        else {
            val col = numericColumns.first()
            val labelCol = dateColumn ?: columns.firstOrNull { it != col } ?: return@remember emptyList()
            filteredRecords.sortedBy { it[labelCol]?.toString() ?: "" }.takeLast(20).mapNotNull { r ->
                val lbl = r[labelCol]?.toString()?.let { if (it.length > 10) it.takeLast(10) else it } ?: return@mapNotNull null
                val v = r[col]?.let { when (it) { is Number -> it.toFloat(); is String -> it.toFloatOrNull(); else -> null } } ?: return@mapNotNull null
                ChartPoint(lbl, v)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(datasetName, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = { IconButton(onClick = { navController.popBackStack() }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } },
                actions = {
                    if (chartLines.isNotEmpty() || barData.isNotEmpty()) {
                        IconButton(onClick = { showChart = !showChart }) {
                            Icon(if (showChart) Icons.Filled.TableChart else Icons.Filled.Timeline, "Toggle chart")
                        }
                    }
                }
            )
        },
        bottomBar = { BannerAd() }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            // Info bar
            Surface(color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Filled.Dataset, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "${if (total > 0) nf.format(total) else records.size} records  •  ${columns.size} columns  •  ${sortedRecords.size} shown",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp).height(48.dp),
                placeholder = { Text("Search records...") },
                leadingIcon = { Icon(Icons.Filled.Search, null, Modifier.size(18.dp)) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Clear, "Clear", Modifier.size(16.dp))
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.large
            )

            // Filter chips row
            if (filterOptions.isNotEmpty() || columns.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Filter dropdown chips
                    filterOptions.entries.take(3).forEach { (col, values) ->
                        var expanded by remember { mutableStateOf(false) }
                        Box {
                            FilterChip(
                                selected = filterColumn == col,
                                onClick = { if (filterColumn == col) { filterColumn = null; filterValue = null } else expanded = true },
                                label = {
                                    Text(
                                        if (filterColumn == col && filterValue != null) "${ col.replace("_"," ").take(8)}: ${filterValue!!.take(10)}"
                                        else col.replace("_", " ").take(12),
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                },
                                leadingIcon = if (filterColumn == col) {{ Icon(Icons.Filled.FilterAlt, null, Modifier.size(14.dp)) }} else null,
                                modifier = Modifier.height(28.dp)
                            )
                            DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                                values.forEach { v ->
                                    DropdownMenuItem(
                                        text = { Text(v, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                        onClick = { filterColumn = col; filterValue = v; expanded = false }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(Modifier.width(8.dp))

                    // Sort chips
                    columns.take(5).forEach { col ->
                        FilterChip(
                            selected = sortColumn == col,
                            onClick = { if (sortColumn == col) sortAscending = !sortAscending else { sortColumn = col; sortAscending = true } },
                            label = { Text(col.replace("_", " ").take(10), style = MaterialTheme.typography.labelSmall) },
                            leadingIcon = if (sortColumn == col) {{
                                Icon(if (sortAscending) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward, null, Modifier.size(14.dp))
                            }} else null,
                            modifier = Modifier.height(28.dp)
                        )
                    }
                }
            }

            // Content
            when {
                isLoading && records.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(8.dp))
                            Text("Loading data...", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                errorMessage != null && records.isEmpty() -> {
                    Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.Warning, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.error)
                                Spacer(Modifier.height(8.dp))
                                Text(errorMessage ?: "", color = MaterialTheme.colorScheme.onErrorContainer)
                                Spacer(Modifier.height(12.dp))
                                FilledTonalButton(onClick = { loadData(0) }) { Text("Retry") }
                            }
                        }
                    }
                }
                records.isEmpty() && !isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No data available", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Summary stats
                        if (colStats.isNotEmpty()) {
                            item(key = "stats") {
                                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))) {
                                    Column(Modifier.padding(12.dp)) {
                                        Text("Summary", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        Spacer(Modifier.height(8.dp))
                                        colStats.forEach { stat ->
                                            Text(stat.col.replace("_", " "), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                                            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                                                StatCard(label = "Latest", value = formatValue(stat.latest, nf))
                                                StatCard(label = "Average", value = formatValue(stat.avg, nf))
                                                StatCard(label = "Min", value = formatValue(stat.min, nf))
                                                StatCard(label = "Max", value = formatValue(stat.max, nf))
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Line chart
                        if (showChart && chartLines.isNotEmpty()) {
                            item(key = "line_chart") {
                                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                                    Column(Modifier.padding(12.dp)) {
                                        Text("Trend", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        Spacer(Modifier.height(8.dp))
                                        SimpleLineChart(lines = chartLines, modifier = Modifier.fillMaxWidth())
                                        Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                            chartLines.forEach { line ->
                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Surface(color = line.color, modifier = Modifier.size(8.dp), shape = MaterialTheme.shapes.extraSmall) {}
                                                    Spacer(Modifier.width(4.dp))
                                                    Text(line.label.replace("_", " "), style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Bar chart
                        if (showChart && chartLines.isEmpty() && barData.size >= 2) {
                            item(key = "bar_chart") {
                                Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                                    Column(Modifier.padding(12.dp)) {
                                        Text(numericColumns.firstOrNull()?.replace("_", " ") ?: "Values", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                        Spacer(Modifier.height(8.dp))
                                        SimpleBarChart(data = barData, modifier = Modifier.fillMaxWidth(), barColor = MaterialTheme.colorScheme.primary, maxBars = 20)
                                    }
                                }
                            }
                        }

                        // Records header
                        item(key = "header") {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                Text("Records (${sortedRecords.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                if (sortColumn != null || filterColumn != null || searchQuery.isNotBlank()) {
                                    TextButton(onClick = { sortColumn = null; filterColumn = null; filterValue = null; searchQuery = "" }, modifier = Modifier.height(28.dp)) {
                                        Text("Clear All", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }

                        // Record cards
                        items(sortedRecords, key = { it.hashCode() }) { record ->
                            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                                Column(Modifier.padding(10.dp)) {
                                    // Highlight the first/main column as title
                                    val mainCol = dateColumn ?: columns.firstOrNull()
                                    if (mainCol != null) {
                                        val mainVal = record[mainCol]
                                        if (mainVal != null) {
                                            Text(
                                                formatValue(mainVal, nf),
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(Modifier.height(4.dp))
                                        }
                                    }
                                    // Other columns
                                    columns.filter { it != mainCol }.forEach { col ->
                                        val value = record[col]
                                        if (value != null) {
                                            Row(Modifier.fillMaxWidth().padding(vertical = 1.dp)) {
                                                Text(
                                                    col.replace("_", " "),
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.weight(0.4f)
                                                )
                                                val fmtVal = formatValue(value, nf)
                                                val isNum = value is Number || (value is String && value.toDoubleOrNull() != null)
                                                Text(
                                                    fmtVal,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontWeight = if (isNum) FontWeight.SemiBold else FontWeight.Normal,
                                                    fontFamily = if (isNum) FontFamily.Monospace else FontFamily.Default,
                                                    color = if (isNum) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.weight(0.6f),
                                                    maxLines = 2,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Load more
                        if (hasMore) {
                            item(key = "load_more") {
                                Box(Modifier.fillMaxWidth().padding(8.dp), contentAlignment = Alignment.Center) {
                                    if (isLoading) {
                                        CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                    } else {
                                        FilledTonalButton(onClick = { loadData(offset, append = true) }) {
                                            Icon(Icons.Filled.ExpandMore, null, Modifier.size(18.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Load More (${nf.format(total - offset)} remaining)")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
