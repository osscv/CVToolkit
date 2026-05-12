package cv.toolkit.screens

import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import cv.toolkit.ads.BannerAd
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

// --- Data models for Ireland CKAN API ---

private data class IeCkanResponse(
    val success: Boolean,
    val result: IeCkanResult?
)

private data class IeCkanResult(
    val count: Int,
    val results: List<IeDataset>
)

private data class IeDataset(
    val title: String,
    val notes: String?,
    val organization: IeOrganization?,
    @SerializedName("metadata_modified") val metadataModified: String?,
    @SerializedName("num_resources") val numResources: Int,
    val resources: List<IeResource>?,
    val theme: String?,
    val tags: List<IeTag>?
)

private data class IeOrganization(val title: String?)
private data class IeResource(
    val format: String?,
    val url: String?,
    val name: String?,
    @SerializedName("datastore_active") val datastoreActive: Boolean
)
private data class IeTag(val name: String)

private val THEME_FILTERS = listOf("Government", "Environment", "Health", "Society", "Transport")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IeDataCatalogueScreen(navController: NavController) {
    var searchQuery by remember { mutableStateOf("") }
    var debouncedQuery by remember { mutableStateOf("") }
    var selectedTheme by remember { mutableStateOf<String?>(null) }

    var datasets by remember { mutableStateOf<List<IeDataset>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var currentPage by remember { mutableIntStateOf(0) }
    var totalCount by remember { mutableIntStateOf(0) }

    val rowsPerPage = 10
    val totalPages = if (totalCount > 0) ((totalCount + rowsPerPage - 1) / rowsPerPage) else 0

    val client = remember {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }
    val gson = remember { Gson() }
    val scope = rememberCoroutineScope()
    val uriHandler = LocalUriHandler.current

    // Debounce search query
    LaunchedEffect(searchQuery) {
        delay(300)
        debouncedQuery = searchQuery
    }

    fun loadDatasets(page: Int, query: String, theme: String?) {
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val body = withContext(Dispatchers.IO) {
                    val offset = page * rowsPerPage
                    var q = query.ifBlank { "*:*" }
                    if (theme != null) {
                        q = if (query.isBlank()) "theme:$theme" else "$q theme:$theme"
                    }
                    val encoded = URLEncoder.encode(q, "UTF-8")
                    val url = "https://data.gov.ie/api/3/action/package_search?rows=$rowsPerPage&start=$offset&q=$encoded"
                    val request = Request.Builder().url(url).build()
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) throw Exception("HTTP ${response.code}")
                        response.body.string()
                    }
                }
                val parsed = gson.fromJson(body, IeCkanResponse::class.java)
                if (parsed.success && parsed.result != null) {
                    datasets = parsed.result.results
                    totalCount = parsed.result.count
                    currentPage = page
                } else {
                    errorMessage = "API returned unsuccessful response"
                }
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to load datasets"
            }
            isLoading = false
        }
    }

    // Reload when debounced query or theme changes
    LaunchedEffect(debouncedQuery, selectedTheme) {
        loadDatasets(0, debouncedQuery, selectedTheme)
    }

    fun onDatasetClick(dataset: IeDataset) {
        // Prefer datastore-active resource
        val resource = dataset.resources?.firstOrNull { it.datastoreActive } ?: dataset.resources?.firstOrNull() ?: return
        val resourceId = resource.url ?: return
        val encodedName = URLEncoder.encode(dataset.title, "UTF-8")
        val encodedId = URLEncoder.encode(resourceId, "UTF-8")
        navController.navigate(
            cv.toolkit.navigation.Screen.IeDatasetViewer.createRoute(encodedId, encodedName)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("IE Data Catalogue") },
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
            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                placeholder = { Text("Search Irish open data...") },
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Clear, "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.extraLarge
            )

            // Theme filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                THEME_FILTERS.forEach { theme ->
                    FilterChip(
                        selected = selectedTheme == theme,
                        onClick = { selectedTheme = if (selectedTheme == theme) null else theme },
                        label = { Text(theme, style = MaterialTheme.typography.labelMedium) }
                    )
                }
            }

            // Info bar
            if (totalCount > 0) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Filled.Dataset, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "$totalCount datasets  \u2022  Page ${currentPage + 1} of $totalPages",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Content
            when {
                isLoading && datasets.isEmpty() -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(8.dp))
                            Text("Loading datasets...", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
                errorMessage != null && datasets.isEmpty() -> {
                    Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.Warning, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.error)
                                Spacer(Modifier.height(8.dp))
                                Text(errorMessage!!, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                                Spacer(Modifier.height(12.dp))
                                FilledTonalButton(onClick = { loadDatasets(currentPage, debouncedQuery, selectedTheme) }) { Text("Retry") }
                            }
                        }
                    }
                }
                datasets.isEmpty() && !isLoading -> {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No datasets found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(datasets, key = { it.title + it.metadataModified }) { dataset ->
                            IeDatasetCard(dataset = dataset, onClick = { onDatasetClick(dataset) })
                        }
                        if (isLoading && datasets.isNotEmpty()) {
                            item(key = "loading") {
                                Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                }
                            }
                        }
                    }

                    // Pagination
                    if (totalPages > 1) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shadowElevation = 4.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                FilledTonalButton(
                                    onClick = { loadDatasets(currentPage - 1, debouncedQuery, selectedTheme) },
                                    enabled = currentPage > 0 && !isLoading
                                ) { Text("Previous") }
                                Text(
                                    "${currentPage + 1} / $totalPages",
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Medium
                                )
                                FilledTonalButton(
                                    onClick = { loadDatasets(currentPage + 1, debouncedQuery, selectedTheme) },
                                    enabled = currentPage < totalPages - 1 && !isLoading
                                ) { Text("Next") }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IeDatasetCard(dataset: IeDataset, onClick: () -> Unit) {
    val uriHandler = LocalUriHandler.current
    val resource = dataset.resources?.firstOrNull()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Title + format badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    dataset.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                val format = resource?.format
                if (!format.isNullOrBlank()) {
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            format.uppercase(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            // Organization
            val orgName = dataset.organization?.title
            if (!orgName.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(orgName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
            }

            // Description
            if (!dataset.notes.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    dataset.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Resource link for non-datastore resources
            if (resource != null && !resource.datastoreActive && !resource.url.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    resource.url,
                    style = MaterialTheme.typography.labelSmall.copy(textDecoration = TextDecoration.Underline),
                    color = MaterialTheme.colorScheme.tertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.clickable { uriHandler.openUri(resource.url) }
                )
            }

            // Tags
            val tags = dataset.tags
            if (!tags.isNullOrEmpty()) {
                Spacer(Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    tags.take(4).forEach { tag ->
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = MaterialTheme.shapes.extraSmall
                        ) {
                            Text(
                                tag.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 1.dp)
                            )
                        }
                    }
                }
            }

            // Last updated
            if (!dataset.metadataModified.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Updated: ${dataset.metadataModified.take(10)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}
