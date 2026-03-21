package cv.toolkit.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
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
import java.net.HttpURLConnection
import java.net.URL

data class HttpHeaderResult(
    val url: String,
    val statusCode: Int,
    val statusMessage: String,
    val headers: Map<String, List<String>>,
    val responseTime: Long
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HttpHeadersScreen(navController: NavController) {
    var urlInput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var headerResult by remember { mutableStateOf<HttpHeaderResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var followRedirects by remember { mutableStateOf(true) }
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    fun fetchHeaders() {
        if (urlInput.isBlank()) return

        var url = urlInput.trim()
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://$url"
        }

        isLoading = true
        errorMessage = null
        headerResult = null

        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    fetchHttpHeaders(url, followRedirects)
                }
                headerResult = result
            } catch (e: Exception) {
                errorMessage = "Failed to fetch headers: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("HTTP Headers") },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Input field
            OutlinedTextField(
                value = urlInput,
                onValueChange = { urlInput = it },
                label = { Text("URL") },
                placeholder = { Text("https://example.com") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Link, null) },
                trailingIcon = {
                    if (urlInput.isNotEmpty()) {
                        IconButton(onClick = { urlInput = "" }) {
                            Icon(Icons.Filled.Clear, "Clear")
                        }
                    }
                }
            )

            // Options
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(checked = followRedirects, onCheckedChange = { followRedirects = it })
                Text("Follow Redirects", style = MaterialTheme.typography.bodySmall)
            }

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            clipboard.getClipEntry()?.clipData?.getItemAt(0)?.text?.let {
                                urlInput = it.toString()
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.ContentPaste, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Paste")
                }
                Button(
                    onClick = { fetchHeaders() },
                    modifier = Modifier.weight(1f),
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
                    Spacer(Modifier.width(4.dp))
                    Text("Fetch")
                }
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
            headerResult?.let { result ->
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
                                "HTTP ${result.statusCode}",
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

                // Headers list
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Response Headers (${result.headers.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            IconButton(
                                onClick = {
                                    val headersText = result.headers.entries.joinToString("\n") { (key, values) ->
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
                            items(result.headers.entries.toList()) { (key, values) ->
                                HeaderItem(
                                    headerName = key,
                                    headerValues = values,
                                    onCopy = {
                                        scope.launch {
                                            clipboard.setClipEntry(
                                                androidx.compose.ui.platform.ClipEntry(
                                                    android.content.ClipData.newPlainText(key, values.joinToString(", "))
                                                )
                                            )
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Empty state
            if (headerResult == null && errorMessage == null && !isLoading) {
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
                            Icons.Filled.Http,
                            null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Enter a URL",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "View HTTP response headers from any website",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
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
                        Text("Fetching headers...", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            BannerAd(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun HeaderItem(
    headerName: String,
    headerValues: List<String>,
    onCopy: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    headerName,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
                IconButton(onClick = onCopy, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Filled.ContentCopy, "Copy", modifier = Modifier.size(16.dp))
                }
            }
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

private fun fetchHttpHeaders(urlString: String, followRedirects: Boolean): HttpHeaderResult {
    val startTime = System.currentTimeMillis()
    val url = URL(urlString)
    val connection = url.openConnection() as HttpURLConnection

    connection.requestMethod = "HEAD"
    connection.instanceFollowRedirects = followRedirects
    connection.connectTimeout = 15000
    connection.readTimeout = 15000
    connection.setRequestProperty("User-Agent", "CVToolkit/1.0")

    try {
        connection.connect()
        val responseTime = System.currentTimeMillis() - startTime

        val statusCode = connection.responseCode
        val statusMessage = connection.responseMessage ?: "Unknown"

        val headers = mutableMapOf<String, List<String>>()
        connection.headerFields.forEach { (key, values) ->
            if (key != null) {
                headers[key] = values
            }
        }

        return HttpHeaderResult(
            url = urlString,
            statusCode = statusCode,
            statusMessage = statusMessage,
            headers = headers,
            responseTime = responseTime
        )
    } finally {
        connection.disconnect()
    }
}
