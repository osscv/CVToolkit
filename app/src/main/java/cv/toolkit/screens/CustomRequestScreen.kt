package cv.toolkit.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cv.toolkit.ads.BannerAd
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

enum class HttpMethod(val displayName: String) {
    GET("GET"),
    POST("POST"),
    PUT("PUT"),
    PATCH("PATCH"),
    DELETE("DELETE"),
    HEAD("HEAD"),
    OPTIONS("OPTIONS"),
    COPY("COPY"),
    LINK("LINK")
}

// Common HTTP header keys for suggestions
val commonHeaderKeys = listOf(
    "Content-Type",
    "Authorization",
    "Accept",
    "Accept-Language",
    "Accept-Encoding",
    "Cache-Control",
    "Cookie",
    "User-Agent",
    "Referer",
    "Origin",
    "X-API-Key",
    "X-Auth-Token",
    "X-Requested-With",
    "If-None-Match",
    "If-Modified-Since"
)

// Common Content-Type values
val commonContentTypes = listOf(
    "application/json",
    "application/x-www-form-urlencoded",
    "multipart/form-data",
    "text/plain",
    "text/html",
    "text/xml",
    "application/xml"
)

data class HeaderEntry(
    val key: String,
    val value: String
)

data class CustomRequestResult(
    val url: String,
    val method: String,
    val statusCode: Int,
    val statusMessage: String,
    val responseHeaders: Map<String, List<String>>,
    val responseBody: String,
    val responseTime: Long,
    val requestHeaders: List<HeaderEntry>,
    val requestBody: String?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CustomRequestScreen(navController: NavController) {
    var urlInput by remember { mutableStateOf("") }
    var selectedMethod by remember { mutableStateOf(HttpMethod.GET) }
    var isLoading by remember { mutableStateOf(false) }
    var requestResult by remember { mutableStateOf<CustomRequestResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var followRedirects by remember { mutableStateOf(true) }
    var requestBody by remember { mutableStateOf("") }
    var showMethodDropdown by remember { mutableStateOf(false) }
    var curlInput by remember { mutableStateOf("") }
    var showCurlDialog by remember { mutableStateOf(false) }

    // Headers management
    var headers by remember { mutableStateOf(listOf(HeaderEntry("", ""))) }
    var newHeaderKey by remember { mutableStateOf("") }
    var newHeaderValue by remember { mutableStateOf("") }
    var showHeaderKeyDropdown by remember { mutableStateOf(false) }
    var showHeaderValueDropdown by remember { mutableStateOf(false) }
    var showPasteHeadersDialog by remember { mutableStateOf(false) }
    var pasteHeadersInput by remember { mutableStateOf("") }

    // Tab state
    var selectedTab by remember { mutableIntStateOf(0) }

    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    fun parseAndApplyCurl(curl: String) {
        try {
            // Basic CURL parser
            var url = ""
            var method = HttpMethod.GET
            val parsedHeaders = mutableListOf<HeaderEntry>()
            var body = ""

            val parts = curl.trim().split(Regex("\\s+(?=(?:[^\"]*\"[^\"]*\")*[^\"]*\$)"))
            var i = 0
            while (i < parts.size) {
                val part = parts[i].trim('"', '\'')
                when {
                    part == "curl" -> { /* skip */ }
                    part == "-X" || part == "--request" -> {
                        if (i + 1 < parts.size) {
                            val methodStr = parts[i + 1].trim('"', '\'').uppercase()
                            method = HttpMethod.entries.find { it.name == methodStr } ?: HttpMethod.GET
                            i++
                        }
                    }
                    part == "-H" || part == "--header" -> {
                        if (i + 1 < parts.size) {
                            val header = parts[i + 1].trim('"', '\'')
                            val colonIndex = header.indexOf(':')
                            if (colonIndex > 0) {
                                val key = header.substring(0, colonIndex).trim()
                                val value = header.substring(colonIndex + 1).trim()
                                parsedHeaders.add(HeaderEntry(key, value))
                            }
                            i++
                        }
                    }
                    part == "-d" || part == "--data" || part == "--data-raw" -> {
                        if (i + 1 < parts.size) {
                            body = parts[i + 1].trim('"', '\'')
                            if (method == HttpMethod.GET) method = HttpMethod.POST
                            i++
                        }
                    }
                    part == "-L" || part == "--location" -> {
                        followRedirects = true
                    }
                    part.startsWith("http://") || part.startsWith("https://") -> {
                        url = part
                    }
                    !part.startsWith("-") && url.isEmpty() && part != "curl" -> {
                        url = part
                    }
                }
                i++
            }

            if (url.isNotEmpty()) {
                urlInput = url
                selectedMethod = method
                requestBody = body
                if (parsedHeaders.isNotEmpty()) {
                    headers = parsedHeaders
                }
                showCurlDialog = false
            } else {
                errorMessage = "Could not parse URL from CURL command"
            }
        } catch (e: Exception) {
            errorMessage = "Failed to parse CURL: ${e.message}"
        }
    }

    fun generateCurl(): String {
        val sb = StringBuilder("curl")

        if (selectedMethod != HttpMethod.GET) {
            sb.append(" -X ${selectedMethod.name}")
        }

        if (followRedirects) {
            sb.append(" -L")
        }

        headers.filter { it.key.isNotBlank() && it.value.isNotBlank() }.forEach { header ->
            sb.append(" -H \"${header.key}: ${header.value}\"")
        }

        if (requestBody.isNotBlank() && selectedMethod in listOf(HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH)) {
            sb.append(" -d '${requestBody.replace("'", "\\'")}'")
        }

        var url = urlInput.trim()
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://$url"
        }
        sb.append(" \"$url\"")

        return sb.toString()
    }

    fun sendRequest() {
        if (urlInput.isBlank()) return

        var url = urlInput.trim()
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://$url"
        }

        isLoading = true
        errorMessage = null
        requestResult = null

        val validHeaders = headers.filter { it.key.isNotBlank() && it.value.isNotBlank() }

        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    executeRequest(
                        url = url,
                        method = selectedMethod,
                        headers = validHeaders,
                        body = if (selectedMethod in listOf(HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH)) requestBody else null,
                        followRedirects = followRedirects
                    )
                }
                requestResult = result
            } catch (e: Exception) {
                errorMessage = "Request failed: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Custom Request") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showCurlDialog = true }) {
                        Icon(Icons.Filled.Terminal, "Import CURL")
                    }
                    IconButton(
                        onClick = {
                            val curl = generateCurl()
                            scope.launch {
                                clipboard.setClipEntry(
                                    androidx.compose.ui.platform.ClipEntry(
                                        android.content.ClipData.newPlainText("curl", curl)
                                    )
                                )
                            }
                        }
                    ) {
                        Icon(Icons.Filled.ContentCopy, "Copy as CURL")
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Method and URL row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Method dropdown
                ExposedDropdownMenuBox(
                    expanded = showMethodDropdown,
                    onExpandedChange = { showMethodDropdown = it },
                    modifier = Modifier.width(120.dp)
                ) {
                    OutlinedTextField(
                        value = selectedMethod.displayName,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showMethodDropdown) },
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                    )
                    ExposedDropdownMenu(
                        expanded = showMethodDropdown,
                        onDismissRequest = { showMethodDropdown = false }
                    ) {
                        HttpMethod.entries.forEach { method ->
                            DropdownMenuItem(
                                text = { Text(method.displayName) },
                                onClick = {
                                    selectedMethod = method
                                    showMethodDropdown = false
                                }
                            )
                        }
                    }
                }

                // URL input
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    label = { Text("URL") },
                    placeholder = { Text("https://api.example.com") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    trailingIcon = {
                        if (urlInput.isNotEmpty()) {
                            IconButton(onClick = { urlInput = "" }) {
                                Icon(Icons.Filled.Clear, "Clear")
                            }
                        }
                    }
                )
            }

            // Options row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = followRedirects, onCheckedChange = { followRedirects = it })
                    Text("Follow Redirects", style = MaterialTheme.typography.bodySmall)
                }
            }

            // Tabs for Headers and Body
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                PrimaryTabRow(
                    selectedTabIndex = selectedTab,
                    modifier = Modifier.weight(1f)
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        text = { Text("Headers (${headers.count { it.key.isNotBlank() }})") }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        text = { Text("Body") },
                        enabled = selectedMethod in listOf(HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH)
                    )
                }
                if (selectedTab == 0) {
                    IconButton(onClick = { showPasteHeadersDialog = true }) {
                        Icon(Icons.Filled.ContentPaste, "Paste Headers")
                    }
                }
            }

            // Tab content
            when (selectedTab) {
                0 -> {
                    // Headers section
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            // Add header row with dropdowns
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Header key with dropdown
                                ExposedDropdownMenuBox(
                                    expanded = showHeaderKeyDropdown,
                                    onExpandedChange = { showHeaderKeyDropdown = it },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    OutlinedTextField(
                                        value = newHeaderKey,
                                        onValueChange = {
                                            newHeaderKey = it
                                            showHeaderKeyDropdown = true
                                        },
                                        placeholder = { Text("Key") },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodySmall,
                                        trailingIcon = {
                                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = showHeaderKeyDropdown)
                                        }
                                    )
                                    val filteredKeys = commonHeaderKeys.filter {
                                        it.contains(newHeaderKey, ignoreCase = true)
                                    }
                                    if (filteredKeys.isNotEmpty()) {
                                        ExposedDropdownMenu(
                                            expanded = showHeaderKeyDropdown,
                                            onDismissRequest = { showHeaderKeyDropdown = false }
                                        ) {
                                            filteredKeys.forEach { key ->
                                                DropdownMenuItem(
                                                    text = { Text(key, style = MaterialTheme.typography.bodySmall) },
                                                    onClick = {
                                                        newHeaderKey = key
                                                        showHeaderKeyDropdown = false
                                                        // Show value suggestions for Content-Type
                                                        if (key == "Content-Type") {
                                                            showHeaderValueDropdown = true
                                                        }
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }

                                // Header value with dropdown (for Content-Type)
                                ExposedDropdownMenuBox(
                                    expanded = showHeaderValueDropdown && newHeaderKey.equals("Content-Type", ignoreCase = true),
                                    onExpandedChange = {
                                        if (newHeaderKey.equals("Content-Type", ignoreCase = true)) {
                                            showHeaderValueDropdown = it
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    OutlinedTextField(
                                        value = newHeaderValue,
                                        onValueChange = {
                                            newHeaderValue = it
                                            if (newHeaderKey.equals("Content-Type", ignoreCase = true)) {
                                                showHeaderValueDropdown = true
                                            }
                                        },
                                        placeholder = { Text("Value") },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryEditable),
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodySmall,
                                        trailingIcon = {
                                            if (newHeaderKey.equals("Content-Type", ignoreCase = true)) {
                                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = showHeaderValueDropdown)
                                            }
                                        }
                                    )
                                    if (newHeaderKey.equals("Content-Type", ignoreCase = true)) {
                                        val filteredValues = commonContentTypes.filter {
                                            it.contains(newHeaderValue, ignoreCase = true)
                                        }
                                        if (filteredValues.isNotEmpty()) {
                                            ExposedDropdownMenu(
                                                expanded = showHeaderValueDropdown,
                                                onDismissRequest = { showHeaderValueDropdown = false }
                                            ) {
                                                filteredValues.forEach { value ->
                                                    DropdownMenuItem(
                                                        text = { Text(value, style = MaterialTheme.typography.bodySmall) },
                                                        onClick = {
                                                            newHeaderValue = value
                                                            showHeaderValueDropdown = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        if (newHeaderKey.isNotBlank()) {
                                            headers = headers + HeaderEntry(newHeaderKey, newHeaderValue)
                                            newHeaderKey = ""
                                            newHeaderValue = ""
                                        }
                                    }
                                ) {
                                    Icon(Icons.Filled.Add, "Add Header")
                                }
                            }

                            Spacer(Modifier.height(8.dp))

                            // Headers list
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                items(headers.filter { it.key.isNotBlank() }) { header ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            "${header.key}: ${header.value}",
                                            style = MaterialTheme.typography.bodySmall,
                                            fontFamily = FontFamily.Monospace,
                                            modifier = Modifier.weight(1f)
                                        )
                                        IconButton(
                                            onClick = { headers = headers.filter { it != header } },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            Icon(Icons.Filled.Close, "Remove", modifier = Modifier.size(16.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    // Body section
                    OutlinedTextField(
                        value = requestBody,
                        onValueChange = { requestBody = it },
                        label = { Text("Request Body") },
                        placeholder = { Text("{\"key\": \"value\"}") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                    )
                }
            }

            // Send button
            Button(
                onClick = { sendRequest() },
                modifier = Modifier.fillMaxWidth(),
                enabled = urlInput.isNotBlank() && !isLoading
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Icon(Icons.AutoMirrored.Filled.Send, null, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text("Send Request")
            }

            // Error message
            errorMessage?.let {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Error,
                            null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Results
            requestResult?.let { result ->
                // Status card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            result.statusCode in 200..299 -> MaterialTheme.colorScheme.primaryContainer
                            result.statusCode in 300..399 -> MaterialTheme.colorScheme.tertiaryContainer
                            else -> MaterialTheme.colorScheme.errorContainer
                        }
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "${result.method} ${result.statusCode}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                result.statusMessage,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "${result.responseTime}ms",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                "Response Time",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }

                // Response tabs
                var responseTab by remember { mutableIntStateOf(0) }
                PrimaryTabRow(selectedTabIndex = responseTab) {
                    Tab(
                        selected = responseTab == 0,
                        onClick = { responseTab = 0 },
                        text = { Text("Body") }
                    )
                    Tab(
                        selected = responseTab == 1,
                        onClick = { responseTab = 1 },
                        text = { Text("Headers (${result.responseHeaders.size})") }
                    )
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    when (responseTab) {
                        0 -> {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Response Body",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                clipboard.setClipEntry(
                                                    androidx.compose.ui.platform.ClipEntry(
                                                        android.content.ClipData.newPlainText("response", result.responseBody)
                                                    )
                                                )
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Filled.ContentCopy, "Copy", modifier = Modifier.size(20.dp))
                                    }
                                }
                                HorizontalDivider()
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    result.responseBody.ifEmpty { "(empty response)" },
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .verticalScroll(rememberScrollState())
                                        .horizontalScroll(rememberScrollState())
                                )
                            }
                        }
                        1 -> {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Response Headers",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    IconButton(
                                        onClick = {
                                            val headersText = result.responseHeaders.entries.joinToString("\n") { (key, values) ->
                                                "$key: ${values.joinToString(", ")}"
                                            }
                                            scope.launch {
                                                clipboard.setClipEntry(
                                                    androidx.compose.ui.platform.ClipEntry(
                                                        android.content.ClipData.newPlainText("headers", headersText)
                                                    )
                                                )
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Filled.ContentCopy, "Copy All", modifier = Modifier.size(20.dp))
                                    }
                                }
                                HorizontalDivider()
                                Spacer(Modifier.height(8.dp))
                                LazyColumn(
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    items(result.responseHeaders.entries.toList()) { (key, values) ->
                                        ResponseHeaderItem(
                                            headerName = key,
                                            headerValues = values
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Empty state
            if (requestResult == null && errorMessage == null && !isLoading) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Filled.Api,
                            null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Custom HTTP Request",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Send GET, POST, PUT, PATCH, DELETE, HEAD, OPTIONS, COPY, or LINK requests with custom headers and body",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }

            // Loading state
            if (isLoading) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(16.dp))
                        Text("Sending request...", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            BannerAd(modifier = Modifier.fillMaxWidth())
        }
    }

    // CURL Import Dialog
    if (showCurlDialog) {
        AlertDialog(
            onDismissRequest = { showCurlDialog = false },
            title = { Text("Import CURL Command") },
            text = {
                Column {
                    Text(
                        "Paste a CURL command to import URL, method, headers, and body",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = curlInput,
                        onValueChange = { curlInput = it },
                        label = { Text("CURL Command") },
                        placeholder = { Text("curl -X POST https://...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { parseAndApplyCurl(curlInput) },
                    enabled = curlInput.isNotBlank()
                ) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCurlDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Paste Headers Dialog
    if (showPasteHeadersDialog) {
        AlertDialog(
            onDismissRequest = { showPasteHeadersDialog = false },
            title = { Text("Paste Headers") },
            text = {
                Column {
                    Text(
                        "Paste headers in format:\nKey: Value\n\nOne header per line",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = pasteHeadersInput,
                        onValueChange = { pasteHeadersInput = it },
                        label = { Text("Headers") },
                        placeholder = { Text("Content-Type: application/json\nAuthorization: Bearer token") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        // Parse headers line by line
                        val parsedHeaders = pasteHeadersInput
                            .lines()
                            .filter { it.contains(":") }
                            .map { line ->
                                val colonIndex = line.indexOf(':')
                                val key = line.substring(0, colonIndex).trim()
                                val value = line.substring(colonIndex + 1).trim()
                                HeaderEntry(key, value)
                            }
                            .filter { it.key.isNotBlank() }

                        if (parsedHeaders.isNotEmpty()) {
                            headers = headers + parsedHeaders
                            pasteHeadersInput = ""
                            showPasteHeadersDialog = false
                        }
                    },
                    enabled = pasteHeadersInput.isNotBlank()
                ) {
                    Text("Add Headers")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPasteHeadersDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun ResponseHeaderItem(
    headerName: String,
    headerValues: List<String>
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                headerName,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            headerValues.forEach { value ->
                Text(
                    value,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .horizontalScroll(rememberScrollState())
                )
            }
        }
    }
}

private fun executeRequest(
    url: String,
    method: HttpMethod,
    headers: List<HeaderEntry>,
    body: String?,
    followRedirects: Boolean
): CustomRequestResult {
    val startTime = System.currentTimeMillis()
    val urlObj = URL(url)
    val connection = urlObj.openConnection() as HttpURLConnection

    connection.requestMethod = method.name
    connection.instanceFollowRedirects = followRedirects
    connection.connectTimeout = 30000
    connection.readTimeout = 30000
    connection.setRequestProperty("User-Agent", "CVToolkit/1.0")

    // Set custom headers
    headers.forEach { header ->
        connection.setRequestProperty(header.key, header.value)
    }

    // Set body for POST, PUT, PATCH
    if (body != null && body.isNotBlank() && method in listOf(HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH)) {
        connection.doOutput = true
        if (headers.none { it.key.equals("Content-Type", ignoreCase = true) }) {
            connection.setRequestProperty("Content-Type", "application/json")
        }
        OutputStreamWriter(connection.outputStream).use { writer ->
            writer.write(body)
            writer.flush()
        }
    }

    try {
        connection.connect()
        val responseTime = System.currentTimeMillis() - startTime

        val statusCode = connection.responseCode
        val statusMessage = connection.responseMessage ?: "Unknown"

        val responseHeaders = mutableMapOf<String, List<String>>()
        connection.headerFields.forEach { (key, values) ->
            if (key != null) {
                responseHeaders[key] = values
            }
        }

        // Read response body
        val responseBody = try {
            val inputStream = if (statusCode >= 400) {
                connection.errorStream
            } else {
                connection.inputStream
            }
            inputStream?.let {
                BufferedReader(InputStreamReader(it)).use { reader ->
                    reader.readText()
                }
            } ?: ""
        } catch (e: Exception) {
            ""
        }

        return CustomRequestResult(
            url = url,
            method = method.name,
            statusCode = statusCode,
            statusMessage = statusMessage,
            responseHeaders = responseHeaders,
            responseBody = responseBody,
            responseTime = responseTime,
            requestHeaders = headers,
            requestBody = body
        )
    } finally {
        connection.disconnect()
    }
}
