package cv.toolkit.screens

import android.annotation.SuppressLint
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone
import java.util.concurrent.TimeUnit

// --- Data model ---

private data class LightningStrike(
    val latitude: Double,
    val longitude: Double,
    val type: String,
    val text: String,
    val datetime: String
)

// --- OkHttp client ---

private val httpClient = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(15, TimeUnit.SECONDS)
    .writeTimeout(15, TimeUnit.SECONDS)
    .build()

private const val TAG = "SgLightning"

// --- Main screen ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SgLightningScreen(navController: NavController) {
    val scope = rememberCoroutineScope()

    // State
    var strikes by remember { mutableStateOf<List<LightningStrike>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var webView by remember { mutableStateOf<WebView?>(null) }

    // Update map when strikes change
    LaunchedEffect(strikes, webView) {
        val wv = webView ?: return@LaunchedEffect
        val jsArray = strikes.joinToString(",", "[", "]") { s ->
            val escapedText = s.text.replace("'", "\\'")
            "{lat:${s.latitude},lng:${s.longitude},type:'${s.type}',text:'$escapedText'}"
        }
        wv.evaluateJavascript("updateStrikes($jsArray)", null)
    }

    // Fetch lightning data
    suspend fun fetchLightning() {
        isLoading = true
        errorMessage = null
        try {
            val result = withContext(Dispatchers.IO) {
                val request = Request.Builder()
                    .url("https://api-open.data.gov.sg/v2/real-time/api/weather?api=lightning")
                    .addHeader(
                        "x-api-key",
                        "v2:3628e11212324251b93eccb69f46844aa70b7877ab736bf17f2f75deb7c19c0b:bmc3TaMdW9V-4pdcz5quf7qP-4VCHR2B"
                    )
                    .build()
                val response = httpClient.newCall(request).execute()
                response.use { resp ->
                    if (!resp.isSuccessful) throw Exception("HTTP ${resp.code}")
                    val json = resp.body.string()
                    val gson = Gson()
                    val mapType = object : TypeToken<Map<String, Any>>() {}.type
                    val root: Map<String, Any> = gson.fromJson(json, mapType)
                    val data = root["data"] as? Map<*, *> ?: throw Exception("No data")
                    val records = data["records"] as? List<*> ?: throw Exception("No records")
                    val allStrikes = mutableListOf<LightningStrike>()
                    for (record in records) {
                        val rec = record as? Map<*, *> ?: continue
                        val item = rec["item"] as? Map<*, *> ?: continue
                        val readings = item["readings"] as? List<*> ?: continue
                        for (reading in readings) {
                            val r = reading as? Map<*, *> ?: continue
                            val location = r["location"] as? Map<*, *> ?: continue
                            val lat = (location["latitude"] as? String)?.toDoubleOrNull()
                                ?: (location["latitude"] as? Number)?.toDouble() ?: continue
                            val lng = (location["longitude"] as? String)?.toDoubleOrNull()
                                ?: (location["longitude"] as? Number)?.toDouble() ?: continue
                            val type = r["type"] as? String ?: ""
                            val text = r["text"] as? String ?: ""
                            val dt = r["datetime"] as? String ?: ""
                            allStrikes.add(LightningStrike(lat, lng, type, text, dt))
                        }
                    }
                    allStrikes
                }
            }
            strikes = result
        } catch (e: Exception) {
            Log.e(TAG, "Fetch error", e)
            errorMessage = e.message ?: "Unknown error"
        } finally {
            isLoading = false
        }
    }

    // Auto-refresh every 60s
    LaunchedEffect(Unit) {
        while (true) {
            fetchLightning()
            delay(60_000)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SG Lightning") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { scope.launch { fetchLightning() } },
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
            // Status card
            StatusCard(
                strikeCount = strikes.size,
                isLoading = isLoading,
                errorMessage = errorMessage
            )

            // OpenStreetMap via WebView
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1.2f)
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
                                    Log.d(TAG, "Page loaded: $url")
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
                            Log.d(TAG, "Loading lightning map via WebViewAssetLoader")
                            loadUrl("https://appassets.androidplatform.net/assets/sg_lightning_map.html")
                            webView = this
                        }
                    }
                )

                if (isLoading && strikes.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            // Strike list
            if (strikes.isEmpty() && !isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage ?: "No lightning strikes detected",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(strikes) { strike ->
                        StrikeCard(strike = strike)
                    }
                }
            }
        }
    }
}

// --- Status card ---

@Composable
private fun StatusCard(
    strikeCount: Int,
    isLoading: Boolean,
    errorMessage: String?
) {
    val containerColor: Color
    val contentColor: Color
    val icon: @Composable () -> Unit
    val statusText: String

    if (errorMessage != null) {
        containerColor = MaterialTheme.colorScheme.errorContainer
        contentColor = MaterialTheme.colorScheme.onErrorContainer
        icon = { Icon(Icons.Filled.Warning, contentDescription = null, tint = contentColor) }
        statusText = "Error: $errorMessage"
    } else if (strikeCount == 0) {
        containerColor = Color(0xFF4CAF50).copy(alpha = 0.15f)
        contentColor = Color(0xFF2E7D32)
        icon = { Icon(Icons.Filled.CheckCircle, contentDescription = null, tint = contentColor) }
        statusText = if (isLoading) "Loading..." else "No Lightning Detected"
    } else {
        containerColor = Color(0xFFFFA726).copy(alpha = 0.2f)
        contentColor = Color(0xFFE65100)
        icon = { Icon(Icons.Filled.Warning, contentDescription = null, tint = contentColor) }
        statusText = "$strikeCount Lightning Strike${if (strikeCount != 1) "s" else ""} Detected"
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            icon()
            Text(
                text = statusText,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
            if (isLoading) {
                Spacer(modifier = Modifier.weight(1f))
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
            }
        }
    }
}

// --- Strike card ---

@Composable
private fun StrikeCard(strike: LightningStrike) {
    val typeColor = if (strike.type == "G") Color(0xFFE53935) else Color(0xFFFDD835)
    val typeLabel = if (strike.type == "G") "Cloud to Ground" else "Cloud to Cloud"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = typeColor.copy(alpha = 0.2f),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text(
                            text = strike.type,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (strike.type == "G") Color(0xFFC62828) else Color(0xFFF9A825),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                    Text(
                        text = typeLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.LocationOn,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "${String.format("%.4f", strike.latitude)}, ${String.format("%.4f", strike.longitude)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (strike.datetime.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formatDateTime(strike.datetime),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

// --- Date formatter ---

private fun formatDateTime(isoString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
        inputFormat.timeZone = TimeZone.getTimeZone("Asia/Singapore")
        val date = inputFormat.parse(isoString.take(19)) ?: return isoString
        val outputFormat = SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.US)
        outputFormat.timeZone = TimeZone.getTimeZone("Asia/Singapore")
        outputFormat.format(date)
    } catch (_: Exception) {
        isoString
    }
}
