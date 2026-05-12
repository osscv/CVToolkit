package cv.toolkit.screens

import android.annotation.SuppressLint
import android.util.Log
import android.webkit.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavController
import androidx.webkit.WebViewAssetLoader
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import cv.toolkit.ads.BannerAd
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

// --- Data models ---

private data class TaxiResponse(
    val type: String,
    val features: List<TaxiFeature>
)

private data class TaxiFeature(
    val type: String,
    val geometry: TaxiGeometry,
    val properties: TaxiProperties
)

private data class TaxiGeometry(
    val type: String,
    val coordinates: List<List<Double>>
)

private data class TaxiProperties(
    val timestamp: String,
    val taxi_count: Int,
    val api_info: ApiInfo?
)

private data class ApiInfo(val status: String?)

private data class RegionInfo(
    val name: String,
    val count: Int,
    val color: Color
)

// --- Region classification ---

private fun classifyRegion(lat: Double, lng: Double): String {
    return when {
        lat > 1.38 -> "North"
        lat < 1.30 -> "South"
        lat in 1.30..1.38 && lng in 103.78..103.87 -> "Central"
        lng > 103.87 -> "East"
        lng < 103.78 -> "West"
        else -> "Central"
    }
}

// --- OkHttp client ---

private val httpClient = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(15, TimeUnit.SECONDS)
    .writeTimeout(15, TimeUnit.SECONDS)
    .build()

private const val TAG = "SgTaxi"
private const val API_URL = "https://api.data.gov.sg/v1/transport/taxi-availability"

// --- Main screen ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SgTaxiScreen(navController: NavController) {
    val scope = rememberCoroutineScope()

    // State
    var taxiCount by remember { mutableIntStateOf(0) }
    var taxiCoords by remember { mutableStateOf<List<Pair<Double, Double>>>(emptyList()) }
    var regionCounts by remember { mutableStateOf<List<RegionInfo>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var lastFetchTime by remember { mutableLongStateOf(0L) }
    var secondsSinceUpdate by remember { mutableIntStateOf(0) }
    var isRefreshing by remember { mutableStateOf(false) }

    // WebView reference
    var webView by remember { mutableStateOf<WebView?>(null) }

    // Update map when taxi data changes
    LaunchedEffect(taxiCoords, webView) {
        val wv = webView ?: return@LaunchedEffect
        if (taxiCoords.isEmpty()) return@LaunchedEffect
        val jsArray = taxiCoords.joinToString(",", "[", "]") { (lat, lng) ->
            "[$lat,$lng]"
        }
        wv.evaluateJavascript("updateTaxis($jsArray)", null)
    }

    // Fetch taxi data
    suspend fun fetchTaxis() {
        isLoading = true
        isRefreshing = true
        errorMessage = null
        try {
            val result = withContext(Dispatchers.IO) {
                val request = Request.Builder().url(API_URL).build()
                val response = httpClient.newCall(request).execute()
                response.use { resp ->
                    if (!resp.isSuccessful) throw Exception("HTTP ${resp.code}")
                    val body = resp.body.string()
                    val taxiResponse = Gson().fromJson(body, TaxiResponse::class.java)
                    taxiResponse
                }
            }

            if (result.features.isNotEmpty()) {
                val feature = result.features[0]
                val coords = feature.geometry.coordinates.map { coord ->
                    // API returns [longitude, latitude], we need (lat, lng)
                    val lng = coord[0]
                    val lat = coord[1]
                    Pair(lat, lng)
                }
                taxiCoords = coords
                taxiCount = feature.properties.taxi_count

                // Classify into regions
                val regionMap = mutableMapOf<String, Int>()
                for ((lat, lng) in coords) {
                    val region = classifyRegion(lat, lng)
                    regionMap[region] = (regionMap[region] ?: 0) + 1
                }

                val regionColors = mapOf(
                    "North" to Color(0xFF43A047),
                    "South" to Color(0xFFE53935),
                    "East" to Color(0xFF1E88E5),
                    "West" to Color(0xFFFB8C00),
                    "Central" to Color(0xFF8E24AA)
                )

                regionCounts = listOf("Central", "North", "South", "East", "West")
                    .filter { regionMap.containsKey(it) }
                    .map { name ->
                        RegionInfo(
                            name = name,
                            count = regionMap[name] ?: 0,
                            color = regionColors[name] ?: Color.Gray
                        )
                    }
            }

            lastFetchTime = System.currentTimeMillis() / 1000
            secondsSinceUpdate = 0
        } catch (e: Exception) {
            Log.e(TAG, "Fetch failed", e)
            errorMessage = e.message ?: "Unknown error"
        } finally {
            isLoading = false
            isRefreshing = false
        }
    }

    // Auto-refresh every 30s
    LaunchedEffect(Unit) {
        while (true) {
            fetchTaxis()
            delay(30_000)
        }
    }

    // Tick counter
    LaunchedEffect(lastFetchTime) {
        if (lastFetchTime > 0) {
            while (true) {
                delay(1_000)
                secondsSinceUpdate = ((System.currentTimeMillis() / 1000) - lastFetchTime).toInt()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SG Taxi Availability") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { scope.launch { fetchTaxis() } },
                        enabled = !isLoading
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        bottomBar = { BannerAd() }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // OpenStreetMap via WebView
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        val assetLoader = WebViewAssetLoader.Builder()
                            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(ctx))
                            .build()
                        WebView(ctx).apply {
                            @SuppressLint("SetJavaScriptEnabled")
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true
                            settings.allowContentAccess = true
                            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            webViewClient = object : WebViewClient() {
                                override fun shouldInterceptRequest(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): WebResourceResponse? {
                                    return request?.let { assetLoader.shouldInterceptRequest(it.url) }
                                        ?: super.shouldInterceptRequest(view, request)
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    Log.d(TAG, "Map page loaded: $url")
                                }
                            }
                            webChromeClient = object : WebChromeClient() {
                                override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                                    Log.d(
                                        TAG,
                                        "JS: ${consoleMessage?.message()} [${consoleMessage?.sourceId()}:${consoleMessage?.lineNumber()}]"
                                    )
                                    return true
                                }
                            }
                            loadUrl("https://appassets.androidplatform.net/assets/sg_taxi_map.html")
                            webView = this
                        }
                    }
                )

                // Loading overlay
                if (isLoading && taxiCoords.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            // Stats bar
            StatsBar(
                taxiCount = taxiCount,
                secondsSinceUpdate = secondsSinceUpdate,
                isRefreshing = isRefreshing,
                errorMessage = errorMessage
            )

            // Region summary cards
            if (regionCounts.isNotEmpty()) {
                RegionSummary(regionCounts = regionCounts)
            }
        }
    }
}

// --- Stats bar ---

@Composable
private fun StatsBar(
    taxiCount: Int,
    secondsSinceUpdate: Int,
    isRefreshing: Boolean,
    errorMessage: String?
) {
    Surface(
        tonalElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.LocalTaxi,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "$taxiCount taxis available",
                    style = MaterialTheme.typography.labelLarge
                )
            }

            if (errorMessage != null) {
                Text(
                    text = "Error",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error
                )
            } else if (isRefreshing) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(12.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Refreshing...",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Text(
                    text = "Updated ${secondsSinceUpdate}s ago",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// --- Region summary ---

@Composable
private fun RegionSummary(regionCounts: List<RegionInfo>) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = "Taxis by Region",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 6.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            regionCounts.forEach { region ->
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = region.color.copy(alpha = 0.15f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = region.name,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = region.color
                        )
                        Text(
                            text = "${region.count}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = region.color
                        )
                    }
                }
            }
        }
    }
}
