package cv.toolkit.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.util.Log
import android.webkit.ConsoleMessage
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewAssetLoader
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.google.transit.realtime.GtfsRealtime
import cv.toolkit.ads.BannerAd
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import kotlin.math.*

// --- Data model ---

private data class TransitVehicle(
    val vehicleId: String,
    val label: String,
    val routeId: String,
    val tripId: String,
    val latitude: Double,
    val longitude: Double,
    val speed: Float?,
    val bearing: Float?,
    val timestamp: Long,
    val distanceKm: Double? = null
)

// --- Agency definitions ---

private data class Agency(val displayName: String, val urlPath: String)

private val agencies = listOf(
    Agency("KTMB", "ktmb"),
    Agency("Prasarana (Bus KL)", "prasarana?category=rapid-bus-kl"),
    Agency("Prasarana (MRT Feeder)", "prasarana?category=rapid-bus-mrtfeeder"),
    Agency("Prasarana (Bus Kuantan)", "prasarana?category=rapid-bus-kuantan"),
    Agency("Prasarana (Bus Penang)", "prasarana?category=rapid-bus-penang"),
    Agency("myBAS Johor", "mybas-johor"),
    Agency("myBAS Melaka", "mybas-melaka"),
    Agency("myBAS Kuching", "mybas-kuching"),
    Agency("myBAS Ipoh", "mybas-ipoh"),
    Agency("myBAS Seremban A", "mybas-seremban-a"),
    Agency("myBAS Seremban B", "mybas-seremban-b"),
    Agency("myBAS Kuala Terengganu", "mybas-kuala-terengganu"),
    Agency("myBAS Kota Bharu", "mybas-kota-bharu"),
    Agency("myBAS Alor Setar", "mybas-alor-setar"),
    Agency("myBAS Kangar", "mybas-kangar")
)

// --- Route color palette (CSS colors) ---

private val routeColorsCss = listOf(
    "#E53935", "#1E88E5", "#43A047", "#FB8C00", "#8E24AA",
    "#00ACC1", "#D81B60", "#FDD835", "#F4511E", "#3949AB"
)

// --- Haversine distance ---

private fun haversineDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Double {
    val R = 6371.0
    val dLat = Math.toRadians(lat2 - lat1)
    val dLng = Math.toRadians(lng2 - lng1)
    val a = sin(dLat / 2).pow(2) +
            cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLng / 2).pow(2)
    return R * 2 * asin(sqrt(a))
}

// --- OkHttp client ---

private val httpClient = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(15, TimeUnit.SECONDS)
    .writeTimeout(15, TimeUnit.SECONDS)
    .build()

// --- Main screen ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransitRealtimeScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // State
    var selectedAgency by remember { mutableStateOf(agencies[0]) }
    var dropdownExpanded by remember { mutableStateOf(false) }
    var vehicles by remember { mutableStateOf<List<TransitVehicle>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var lastFetchTime by remember { mutableLongStateOf(0L) }
    var secondsSinceUpdate by remember { mutableIntStateOf(0) }
    var isRefreshing by remember { mutableStateOf(false) }

    // User location
    var userLat by remember { mutableDoubleStateOf(0.0) }
    var userLng by remember { mutableDoubleStateOf(0.0) }
    var hasUserLocation by remember { mutableStateOf(false) }
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                    PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasLocationPermission = granted
    }

    // WebView reference
    var webView by remember { mutableStateOf<WebView?>(null) }

    // Position history: vehicleId -> list of (lat, lng, timestamp)
    val positionHistory = remember { mutableStateMapOf<String, MutableList<Triple<Double, Double, Long>>>() }
    // Set of vehicle IDs currently being traced
    var tracedVehicles by remember { mutableStateOf(setOf<String>()) }

    // Route color mapping
    val routeColorMap = remember(vehicles) {
        val distinctRoutes = vehicles.map { it.routeId }.distinct()
        distinctRoutes.mapIndexed { index, route ->
            route to routeColorsCss[index % routeColorsCss.size]
        }.toMap()
    }

    // Fetch user location via LocationManager (free, no Google Play billing)
    @SuppressLint("MissingPermission")
    fun fetchUserLocation() {
        if (!hasLocationPermission) return
        try {
            val locationManager = context.getSystemService(android.content.Context.LOCATION_SERVICE) as LocationManager
            val location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            if (location != null) {
                userLat = location.latitude
                userLng = location.longitude
                hasUserLocation = true
                webView?.evaluateJavascript("setUserLocation(${location.latitude}, ${location.longitude})", null)
            }
        } catch (_: Exception) { }
    }

    // Request permission on first launch
    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    LaunchedEffect(hasLocationPermission) {
        if (hasLocationPermission) fetchUserLocation()
    }

    // Update map markers when vehicles change
    LaunchedEffect(vehicles, webView) {
        val wv = webView ?: return@LaunchedEffect
        if (vehicles.isEmpty()) return@LaunchedEffect
        val jsArray = vehicles.joinToString(",", "[", "]") { v ->
            val color = routeColorMap[v.routeId] ?: "#E53935"
            val escapedRoute = v.routeId.replace("'", "\\'")
            val escapedLabel = v.label.replace("'", "\\'")
            val escapedId = v.vehicleId.replace("'", "\\'")
            "{lat:${v.latitude},lng:${v.longitude},route:'$escapedRoute',label:'$escapedLabel',id:'$escapedId',speed:${v.speed ?: -1},color:'$color'}"
        }
        wv.evaluateJavascript("updateMarkers($jsArray)", null)
    }

    // Fetch vehicles function
    suspend fun fetchVehicles(agency: Agency) {
        isLoading = true
        isRefreshing = true
        errorMessage = null
        try {
            val result = withContext(Dispatchers.IO) {
                val url = "https://api.data.gov.my/gtfs-realtime/vehicle-position/${agency.urlPath}"
                val request = Request.Builder().url(url).build()
                val response = httpClient.newCall(request).execute()
                response.use { resp ->
                    if (!resp.isSuccessful) throw Exception("HTTP ${resp.code}")
                    val bytes = resp.body.bytes()
                    val feedMessage = GtfsRealtime.FeedMessage.parseFrom(bytes)
                    feedMessage.entityList.mapNotNull { entity ->
                        if (entity.hasVehicle()) {
                            val v = entity.vehicle
                            TransitVehicle(
                                vehicleId = if (v.hasVehicle()) v.vehicle.id else "",
                                label = if (v.hasVehicle()) v.vehicle.label else "",
                                routeId = if (v.hasTrip()) v.trip.routeId else "",
                                tripId = if (v.hasTrip()) v.trip.tripId else "",
                                latitude = v.position.latitude.toDouble(),
                                longitude = v.position.longitude.toDouble(),
                                speed = if (v.position.hasSpeed()) v.position.speed else null,
                                bearing = if (v.position.hasBearing()) v.position.bearing else null,
                                timestamp = v.timestamp
                            )
                        } else null
                    }
                }
            }

            // Record position history for all vehicles
            val now = System.currentTimeMillis() / 1000
            for (v in result) {
                val key = v.vehicleId.ifEmpty { v.tripId }
                if (key.isEmpty()) continue
                val history = positionHistory.getOrPut(key) { mutableListOf() }
                val lastPos = history.lastOrNull()
                // Only add if position changed or first entry
                if (lastPos == null || lastPos.first != v.latitude || lastPos.second != v.longitude) {
                    history.add(Triple(v.latitude, v.longitude, now))
                    // Keep max 100 points per vehicle
                    if (history.size > 100) history.removeAt(0)
                }
            }

            // Update traced vehicle polylines on the map
            for (vid in tracedVehicles) {
                val history = positionHistory[vid]
                if (history != null && history.size >= 2) {
                    val color = routeColorMap[result.find { (it.vehicleId.ifEmpty { it.tripId }) == vid }?.routeId] ?: "#E53935"
                    val pointsJs = history.joinToString(",") { "[${it.first},${it.second}]" }
                    webView?.evaluateJavascript("traceVehicle('$vid',[$pointsJs],'$color')", null)
                }
            }

            vehicles = if (hasUserLocation) {
                result.map { v ->
                    v.copy(distanceKm = haversineDistance(userLat, userLng, v.latitude, v.longitude))
                }.sortedBy { it.distanceKm }
            } else {
                result
            }

            lastFetchTime = now
            secondsSinceUpdate = 0
        } catch (e: Exception) {
            errorMessage = e.message ?: "Unknown error"
        } finally {
            isLoading = false
            isRefreshing = false
        }
    }

    // Auto-refresh every 30s
    LaunchedEffect(selectedAgency) {
        while (true) {
            fetchVehicles(selectedAgency)
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
                title = { Text("MY Transit Tracker") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { scope.launch { fetchVehicles(selectedAgency) } },
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
            // Agency selector
            AgencySelector(
                selectedAgency = selectedAgency,
                expanded = dropdownExpanded,
                onExpandedChange = { dropdownExpanded = it },
                onAgencySelected = { agency ->
                    selectedAgency = agency
                    dropdownExpanded = false
                    tracedVehicles = emptySet()
                    positionHistory.clear()
                    webView?.evaluateJavascript("clearAllTraces()", null)
                }
            )

            // OpenStreetMap via WebView + Leaflet
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
                        WebView.setWebContentsDebuggingEnabled(true)
                        WebView(ctx).apply {
                            @SuppressLint("SetJavaScriptEnabled")
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.loadWithOverviewMode = true
                            settings.useWideViewPort = true
                            settings.allowContentAccess = true
                            settings.mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                            webViewClient = object : WebViewClient() {
                                override fun shouldInterceptRequest(view: WebView?, request: WebResourceRequest?): WebResourceResponse? {
                                    return request?.let { assetLoader.shouldInterceptRequest(it.url) }
                                        ?: super.shouldInterceptRequest(view, request)
                                }
                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    Log.d("TransitMap", "Page loaded: $url")
                                }
                            }
                            webChromeClient = object : WebChromeClient() {
                                override fun onConsoleMessage(consoleMessage: ConsoleMessage?): Boolean {
                                    Log.d("TransitMap", "JS: ${consoleMessage?.message()} [${consoleMessage?.sourceId()}:${consoleMessage?.lineNumber()}]")
                                    return true
                                }
                            }
                            Log.d("TransitMap", "Loading map via WebViewAssetLoader")
                            loadUrl("https://appassets.androidplatform.net/assets/transit_map.html")
                            webView = this
                        }
                    }
                )

                // Loading overlay
                if (isLoading && vehicles.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }

            // Info bar
            InfoBar(
                vehicleCount = vehicles.size,
                secondsSinceUpdate = secondsSinceUpdate,
                isRefreshing = isRefreshing,
                errorMessage = errorMessage
            )

            // Vehicle list
            if (vehicles.isEmpty() && !isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.DirectionsBus,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = errorMessage ?: "No vehicles found",
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
                    items(vehicles, key = { it.vehicleId + it.tripId }) { vehicle ->
                        val vid = vehicle.vehicleId.ifEmpty { vehicle.tripId }
                        val isTracing = vid in tracedVehicles
                        val historyCount = positionHistory[vid]?.size ?: 0
                        VehicleCard(
                            vehicle = vehicle,
                            hasUserLocation = hasUserLocation,
                            isTracing = isTracing,
                            historyCount = historyCount,
                            onLocateClick = {
                                webView?.evaluateJavascript(
                                    "panTo(${vehicle.latitude}, ${vehicle.longitude})", null
                                )
                            },
                            onTraceToggle = {
                                if (isTracing) {
                                    tracedVehicles = tracedVehicles - vid
                                    webView?.evaluateJavascript("clearTrace('$vid')", null)
                                } else {
                                    tracedVehicles = tracedVehicles + vid
                                    val history = positionHistory[vid]
                                    if (history != null && history.size >= 2) {
                                        val color = routeColorMap[vehicle.routeId] ?: "#E53935"
                                        val pointsJs = history.joinToString(",") { "[${it.first},${it.second}]" }
                                        webView?.evaluateJavascript("traceVehicle('$vid',[$pointsJs],'$color')", null)
                                    }
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

// --- Agency dropdown selector ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AgencySelector(
    selectedAgency: Agency,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    onAgencySelected: (Agency) -> Unit
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = onExpandedChange,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        OutlinedTextField(
            value = selectedAgency.displayName,
            onValueChange = {},
            readOnly = true,
            label = { Text("Transit Agency") },
            leadingIcon = { Icon(Icons.Filled.DirectionsBus, contentDescription = null) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth()
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onExpandedChange(false) }
        ) {
            agencies.forEach { agency ->
                DropdownMenuItem(
                    text = { Text(agency.displayName) },
                    onClick = { onAgencySelected(agency) },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}

// --- Info bar ---

@Composable
private fun InfoBar(
    vehicleCount: Int,
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
                    Icons.Filled.DirectionsBus,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "$vehicleCount vehicles active",
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
                val nextRefreshIn = maxOf(0, 30 - secondsSinceUpdate)
                Text(
                    text = "Updated ${secondsSinceUpdate}s ago · next in ${nextRefreshIn}s",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// --- Vehicle card ---

@Composable
private fun VehicleCard(
    vehicle: TransitVehicle,
    hasUserLocation: Boolean,
    isTracing: Boolean,
    historyCount: Int,
    onLocateClick: () -> Unit,
    onTraceToggle: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isTracing)
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            else
                MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        Icons.Filled.Route,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = vehicle.routeId.ifEmpty { "No Route" },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.widthIn(max = 120.dp)
                    )
                }
                // Trace toggle button
                IconButton(
                    onClick = onTraceToggle,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        if (isTracing) Icons.Filled.Timeline else Icons.Filled.Timeline,
                        contentDescription = if (isTracing) "Stop tracing" else "Trace route",
                        modifier = Modifier.size(18.dp),
                        tint = if (isTracing) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onLocateClick,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.Filled.MyLocation,
                        contentDescription = "Locate on map",
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Tracing indicator
            if (isTracing) {
                Text(
                    text = "Tracing · $historyCount points recorded",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            // Vehicle ID / label
            if (vehicle.label.isNotEmpty() || vehicle.vehicleId.isNotEmpty()) {
                Text(
                    text = buildString {
                        if (vehicle.label.isNotEmpty()) append(vehicle.label)
                        if (vehicle.vehicleId.isNotEmpty() && vehicle.vehicleId != vehicle.label) {
                            if (isNotEmpty()) append(" (${vehicle.vehicleId})")
                            else append(vehicle.vehicleId)
                        }
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Details row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    DetailChip(
                        icon = Icons.Filled.LocationOn,
                        text = "${String.format("%.5f", vehicle.latitude)}, ${String.format("%.5f", vehicle.longitude)}"
                    )
                }
                Column {
                    DetailChip(
                        icon = Icons.Filled.Speed,
                        text = if (vehicle.speed != null) {
                            "${String.format("%.1f", vehicle.speed * 3.6)} km/h"
                        } else "N/A"
                    )
                }
            }

            // Bearing + distance row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    DetailChip(
                        icon = Icons.Filled.Explore,
                        text = if (vehicle.bearing != null) {
                            "${String.format("%.0f", vehicle.bearing)}\u00B0"
                        } else "N/A"
                    )
                }
                if (hasUserLocation && vehicle.distanceKm != null) {
                    Column {
                        DetailChip(
                            icon = Icons.Filled.NearMe,
                            text = if (vehicle.distanceKm < 1.0) {
                                "${String.format("%.0f", vehicle.distanceKm * 1000)} m away"
                            } else {
                                "${String.format("%.1f", vehicle.distanceKm)} km away"
                            }
                        )
                    }
                }
            }

            // Timestamp
            if (vehicle.timestamp > 0) {
                val timeAgo = (System.currentTimeMillis() / 1000) - vehicle.timestamp
                val timeText = when {
                    timeAgo < 60 -> "${timeAgo}s ago"
                    timeAgo < 3600 -> "${timeAgo / 60}m ago"
                    else -> "${timeAgo / 3600}h ago"
                }
                Text(
                    text = "Reported $timeText",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }
    }
}

// --- Detail chip ---

@Composable
private fun DetailChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
