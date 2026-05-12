package cv.toolkit.screens

import androidx.compose.foundation.clickable
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
import java.util.concurrent.TimeUnit

// --- Data models for SG Data Catalogue API ---

private data class SgDataset(
    val datasetId: String,
    val name: String,
    val description: String?,
    val format: String?,
    val managedByAgencyName: String?,
    val coverageStart: String?,
    val coverageEnd: String?,
    val lastUpdatedAt: String?
)

private data class SgDatasetsResponse(
    val code: Int,
    val data: SgDatasetsData?
)

private data class SgDatasetsData(
    val datasets: List<SgDataset>,
    val pages: Int,
    val totalRowCount: Int
)


private const val SG_API_KEY = "v2:3628e11212324251b93eccb69f46844aa70b7877ab736bf17f2f75deb7c19c0b:bmc3TaMdW9V-4pdcz5quf7qP-4VCHR2B"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SgDataCatalogueScreen(navController: NavController) {
    var searchQuery by remember { mutableStateOf("") }

    // Browse state
    var datasets by remember { mutableStateOf<List<SgDataset>>(emptyList()) }
    var browseLoading by remember { mutableStateOf(false) }
    var browseError by remember { mutableStateOf<String?>(null) }
    var browsePage by remember { mutableIntStateOf(0) }
    var totalPages by remember { mutableIntStateOf(0) }
    var totalDatasets by remember { mutableIntStateOf(0) }

    val client = remember {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }
    val gson = remember { Gson() }
    val scope = rememberCoroutineScope()

    fun loadDatasets(page: Int) {
        scope.launch {
            browseLoading = true
            browseError = null
            try {
                val body = withContext(Dispatchers.IO) {
                    val request = Request.Builder()
                        .url("https://api-production.data.gov.sg/v2/public/api/datasets?page=$page")
                        .addHeader("x-api-key", SG_API_KEY)
                        .build()
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) throw Exception("HTTP ${response.code}")
                        response.body.string()
                    }
                }
                val parsed = gson.fromJson(body, SgDatasetsResponse::class.java)
                if (parsed.code == 0 && parsed.data != null) {
                    datasets = parsed.data.datasets
                    totalPages = parsed.data.pages
                    totalDatasets = parsed.data.totalRowCount
                    browsePage = page
                } else {
                    browseError = "API returned code ${parsed.code}"
                }
            } catch (e: Exception) {
                browseError = e.message ?: "Failed to load datasets"
            }
            browseLoading = false
        }
    }

    fun openDataset(dataset: SgDataset) {
        navController.navigate(
            cv.toolkit.navigation.Screen.SgDatasetViewer.createRoute(dataset.datasetId, dataset.name)
        )
    }

    // Load first page on launch
    LaunchedEffect(Unit) {
        loadDatasets(0)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SG Data Catalogue") },
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
            SgBrowseView(
                datasets = datasets,
                searchQuery = searchQuery,
                onSearchChange = { searchQuery = it },
                isLoading = browseLoading,
                errorMessage = browseError,
                currentPage = browsePage,
                totalPages = totalPages,
                totalDatasets = totalDatasets,
                onPageChange = { loadDatasets(it) },
                onDatasetClick = { openDataset(it) },
                onRetry = { loadDatasets(browsePage) },
                modifier = Modifier.weight(1f)
            )
            BannerAd(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun SgBrowseView(
    datasets: List<SgDataset>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    isLoading: Boolean,
    errorMessage: String?,
    currentPage: Int,
    totalPages: Int,
    totalDatasets: Int,
    onPageChange: (Int) -> Unit,
    onDatasetClick: (SgDataset) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    val filteredDatasets = remember(datasets, searchQuery) {
        if (searchQuery.isBlank()) datasets
        else {
            val q = searchQuery.lowercase()
            datasets.filter { ds ->
                ds.name.lowercase().contains(q) ||
                        (ds.description?.lowercase()?.contains(q) == true) ||
                        (ds.managedByAgencyName?.lowercase()?.contains(q) == true) ||
                        ds.datasetId.lowercase().contains(q)
            }
        }
    }

    Column(modifier = modifier) {
        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            placeholder = { Text("Search datasets on this page...") },
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Filled.Clear, "Clear")
                    }
                }
            },
            singleLine = true,
            shape = MaterialTheme.shapes.extraLarge
        )

        // Page info bar
        if (totalDatasets > 0) {
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
                        "$totalDatasets datasets  •  Page ${currentPage + 1} of $totalPages",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

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
                            Text(errorMessage, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                            Spacer(Modifier.height(12.dp))
                            FilledTonalButton(onClick = onRetry) { Text("Retry") }
                        }
                    }
                }
            }
            filteredDatasets.isEmpty() && searchQuery.isNotBlank() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No datasets match your search", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(filteredDatasets, key = { it.datasetId }) { dataset ->
                        SgDatasetCard(dataset = dataset, onClick = { onDatasetClick(dataset) })
                    }

                    // Loading indicator at bottom
                    if (isLoading && datasets.isNotEmpty()) {
                        item(key = "loading") {
                            Box(Modifier.fillMaxWidth().padding(12.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator(modifier = Modifier.size(24.dp))
                            }
                        }
                    }
                }

                // Pagination controls
                if (totalPages > 1 && searchQuery.isBlank()) {
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
                                onClick = { onPageChange(currentPage - 1) },
                                enabled = currentPage > 0 && !isLoading
                            ) {
                                Text("Previous")
                            }
                            Text(
                                "${currentPage + 1} / $totalPages",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.Medium
                            )
                            FilledTonalButton(
                                onClick = { onPageChange(currentPage + 1) },
                                enabled = currentPage < totalPages - 1 && !isLoading
                            ) {
                                Text("Next")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SgDatasetCard(
    dataset: SgDataset,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    dataset.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.weight(1f)
                )
                if (!dataset.format.isNullOrBlank()) {
                    Spacer(Modifier.width(8.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = MaterialTheme.shapes.small
                    ) {
                        Text(
                            dataset.format,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            if (!dataset.managedByAgencyName.isNullOrBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    dataset.managedByAgencyName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (!dataset.description.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    dataset.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (!dataset.lastUpdatedAt.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    "Updated: ${dataset.lastUpdatedAt.take(10)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}

