package cv.toolkit.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cv.toolkit.ads.BannerAd
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import javax.xml.parsers.DocumentBuilderFactory

data class SitemapResult(
    val url: String,
    val statusCode: Int,
    val type: SitemapType,
    val urls: List<SitemapUrl>,
    val sitemapIndexUrls: List<String>,
    val totalUrls: Int,
    val responseTime: Long,
    val rawContent: String
)

data class SitemapUrl(
    val loc: String,
    val lastmod: String?,
    val changefreq: String?,
    val priority: String?
)

enum class SitemapType {
    SITEMAP, SITEMAP_INDEX, UNKNOWN
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SitemapViewerScreen(navController: NavController) {
    var urlInput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<SitemapResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showRawContent by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    fun fetchSitemap() {
        if (urlInput.isBlank()) return

        var sitemapUrl = urlInput.trim()
        // If user enters just a domain, try common sitemap locations
        if (!sitemapUrl.contains("/sitemap") && !sitemapUrl.endsWith(".xml")) {
            sitemapUrl = sitemapUrl.removePrefix("http://").removePrefix("https://")
            sitemapUrl = sitemapUrl.split("/").firstOrNull() ?: sitemapUrl
            sitemapUrl = "https://$sitemapUrl/sitemap.xml"
        } else if (!sitemapUrl.startsWith("http://") && !sitemapUrl.startsWith("https://")) {
            sitemapUrl = "https://$sitemapUrl"
        }

        isLoading = true
        errorMessage = null
        result = null

        scope.launch {
            try {
                val fetchResult = withContext(Dispatchers.IO) {
                    fetchAndParseSitemap(sitemapUrl)
                }
                result = fetchResult
            } catch (e: Exception) {
                errorMessage = "Failed to fetch sitemap: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    val filteredUrls = result?.urls?.filter {
        searchQuery.isEmpty() || it.loc.contains(searchQuery, ignoreCase = true)
    } ?: emptyList()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sitemap Viewer") },
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
                label = { Text("Sitemap URL or Domain") },
                placeholder = { Text("example.com or example.com/sitemap.xml") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Map, null) },
                trailingIcon = {
                    if (urlInput.isNotEmpty()) {
                        IconButton(onClick = { urlInput = "" }) {
                            Icon(Icons.Filled.Clear, "Clear")
                        }
                    }
                }
            )

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
                    onClick = { fetchSitemap() },
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
            result?.let { sitemapResult ->
                // Status card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            sitemapResult.statusCode == 200 -> MaterialTheme.colorScheme.primaryContainer
                            sitemapResult.statusCode == 404 -> MaterialTheme.colorScheme.tertiaryContainer
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
                                when (sitemapResult.type) {
                                    SitemapType.SITEMAP -> "Sitemap"
                                    SitemapType.SITEMAP_INDEX -> "Sitemap Index"
                                    SitemapType.UNKNOWN -> "Unknown Format"
                                },
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "HTTP ${sitemapResult.statusCode}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "${sitemapResult.totalUrls}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                if (sitemapResult.type == SitemapType.SITEMAP_INDEX) "Sitemaps" else "URLs",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }

                // Toggle and search
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = !showRawContent,
                        onClick = { showRawContent = false },
                        label = { Text("Parsed") },
                        leadingIcon = if (!showRawContent) {
                            { Icon(Icons.Filled.Check, null, modifier = Modifier.size(18.dp)) }
                        } else null
                    )
                    FilterChip(
                        selected = showRawContent,
                        onClick = { showRawContent = true },
                        label = { Text("Raw") },
                        leadingIcon = if (showRawContent) {
                            { Icon(Icons.Filled.Check, null, modifier = Modifier.size(18.dp)) }
                        } else null
                    )
                    Spacer(Modifier.weight(1f))
                    IconButton(
                        onClick = {
                            scope.launch {
                                val content = if (showRawContent) {
                                    sitemapResult.rawContent
                                } else {
                                    sitemapResult.urls.joinToString("\n") { it.loc }
                                }
                                clipboard.setClipEntry(
                                    androidx.compose.ui.platform.ClipEntry(
                                        android.content.ClipData.newPlainText("sitemap", content)
                                    )
                                )
                            }
                        }
                    ) {
                        Icon(Icons.Filled.ContentCopy, "Copy", modifier = Modifier.size(20.dp))
                    }
                }

                // Search (only for parsed view)
                if (!showRawContent && sitemapResult.urls.isNotEmpty()) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        label = { Text("Filter URLs") },
                        placeholder = { Text("Search...") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Filled.Search, null) },
                        trailingIcon = {
                            if (searchQuery.isNotEmpty()) {
                                IconButton(onClick = { searchQuery = "" }) {
                                    Icon(Icons.Filled.Clear, "Clear")
                                }
                            }
                        }
                    )
                    if (searchQuery.isNotEmpty()) {
                        Text(
                            "Showing ${filteredUrls.size} of ${sitemapResult.urls.size} URLs",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Content
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    if (showRawContent) {
                        // Raw content view
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                        ) {
                            item {
                                Text(
                                    sitemapResult.rawContent.ifEmpty { "No content" },
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace,
                                    modifier = Modifier.horizontalScroll(rememberScrollState())
                                )
                            }
                        }
                    } else {
                        // Parsed view
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Sitemap Index URLs
                            if (sitemapResult.sitemapIndexUrls.isNotEmpty()) {
                                item {
                                    Text(
                                        "Child Sitemaps",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                items(sitemapResult.sitemapIndexUrls) { sitemapUrl ->
                                    SitemapIndexItem(
                                        url = sitemapUrl,
                                        onCopy = {
                                            scope.launch {
                                                clipboard.setClipEntry(
                                                    androidx.compose.ui.platform.ClipEntry(
                                                        android.content.ClipData.newPlainText("url", sitemapUrl)
                                                    )
                                                )
                                            }
                                        },
                                        onOpen = {
                                            urlInput = sitemapUrl
                                            fetchSitemap()
                                        }
                                    )
                                }
                            }

                            // Regular URLs
                            if (filteredUrls.isNotEmpty()) {
                                item {
                                    Text(
                                        "URLs",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                items(filteredUrls) { sitemapUrl ->
                                    SitemapUrlItem(
                                        sitemapUrl = sitemapUrl,
                                        onCopy = {
                                            scope.launch {
                                                clipboard.setClipEntry(
                                                    androidx.compose.ui.platform.ClipEntry(
                                                        android.content.ClipData.newPlainText("url", sitemapUrl.loc)
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
            }

            // Empty state
            if (result == null && errorMessage == null && !isLoading) {
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
                            Icons.Filled.Map,
                            null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Enter a sitemap URL",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Parse and display XML sitemaps",
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
                        Text("Fetching sitemap...", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            BannerAd(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun SitemapIndexItem(
    url: String,
    onCopy: () -> Unit,
    onOpen: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Folder,
                null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.tertiary
            )
            Spacer(Modifier.width(8.dp))
            Text(
                url,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.weight(1f),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
                Icon(Icons.Filled.ContentCopy, "Copy", modifier = Modifier.size(16.dp))
            }
            IconButton(onClick = onOpen, modifier = Modifier.size(32.dp)) {
                Icon(Icons.AutoMirrored.Filled.OpenInNew, "Open", modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun SitemapUrlItem(
    sitemapUrl: SitemapUrl,
    onCopy: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Link,
                    null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    sitemapUrl.loc,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                IconButton(onClick = onCopy, modifier = Modifier.size(24.dp)) {
                    Icon(Icons.Filled.ContentCopy, "Copy", modifier = Modifier.size(14.dp))
                }
            }

            // Metadata row
            if (sitemapUrl.lastmod != null || sitemapUrl.changefreq != null || sitemapUrl.priority != null) {
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    sitemapUrl.lastmod?.let {
                        MetadataChip(Icons.Filled.Schedule, it)
                    }
                    sitemapUrl.changefreq?.let {
                        MetadataChip(Icons.Filled.Refresh, it)
                    }
                    sitemapUrl.priority?.let {
                        MetadataChip(Icons.Filled.PriorityHigh, it)
                    }
                }
            }
        }
    }
}

@Composable
private fun MetadataChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            null,
            modifier = Modifier.size(12.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun fetchAndParseSitemap(urlString: String): SitemapResult {
    val startTime = System.currentTimeMillis()
    val url = URL(urlString)
    val connection = url.openConnection() as HttpURLConnection

    connection.requestMethod = "GET"
    connection.connectTimeout = 15000
    connection.readTimeout = 30000
    connection.setRequestProperty("User-Agent", "CVToolkit/1.0")
    connection.setRequestProperty("Accept", "application/xml, text/xml, */*")

    try {
        connection.connect()
        val responseTime = System.currentTimeMillis() - startTime
        val statusCode = connection.responseCode

        if (statusCode != 200) {
            return SitemapResult(
                url = urlString,
                statusCode = statusCode,
                type = SitemapType.UNKNOWN,
                urls = emptyList(),
                sitemapIndexUrls = emptyList(),
                totalUrls = 0,
                responseTime = responseTime,
                rawContent = ""
            )
        }

        val rawContent = connection.inputStream.bufferedReader().use { it.readText() }

        // Parse XML
        val urls = mutableListOf<SitemapUrl>()
        val sitemapIndexUrls = mutableListOf<String>()
        var type = SitemapType.UNKNOWN

        try {
            val factory = DocumentBuilderFactory.newInstance()
            factory.isNamespaceAware = false
            val builder = factory.newDocumentBuilder()
            val document = builder.parse(rawContent.byteInputStream())
            document.documentElement.normalize()

            val rootElement = document.documentElement.tagName.lowercase()

            when {
                rootElement.contains("sitemapindex") -> {
                    type = SitemapType.SITEMAP_INDEX
                    val sitemapNodes = document.getElementsByTagName("sitemap")
                    for (i in 0 until sitemapNodes.length) {
                        val node = sitemapNodes.item(i)
                        val locNodes = (node as org.w3c.dom.Element).getElementsByTagName("loc")
                        if (locNodes.length > 0) {
                            sitemapIndexUrls.add(locNodes.item(0).textContent.trim())
                        }
                    }
                }
                rootElement.contains("urlset") -> {
                    type = SitemapType.SITEMAP
                    val urlNodes = document.getElementsByTagName("url")
                    for (i in 0 until urlNodes.length) {
                        val node = urlNodes.item(i) as org.w3c.dom.Element
                        val loc = node.getElementsByTagName("loc").let {
                            if (it.length > 0) it.item(0).textContent.trim() else null
                        }
                        val lastmod = node.getElementsByTagName("lastmod").let {
                            if (it.length > 0) it.item(0).textContent.trim() else null
                        }
                        val changefreq = node.getElementsByTagName("changefreq").let {
                            if (it.length > 0) it.item(0).textContent.trim() else null
                        }
                        val priority = node.getElementsByTagName("priority").let {
                            if (it.length > 0) it.item(0).textContent.trim() else null
                        }

                        if (loc != null) {
                            urls.add(SitemapUrl(loc, lastmod, changefreq, priority))
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // XML parsing failed, return with unknown type
        }

        val totalUrls = if (type == SitemapType.SITEMAP_INDEX) sitemapIndexUrls.size else urls.size

        return SitemapResult(
            url = urlString,
            statusCode = statusCode,
            type = type,
            urls = urls,
            sitemapIndexUrls = sitemapIndexUrls,
            totalUrls = totalUrls,
            responseTime = responseTime,
            rawContent = rawContent
        )
    } finally {
        connection.disconnect()
    }
}
