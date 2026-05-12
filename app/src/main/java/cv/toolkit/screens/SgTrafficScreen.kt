package cv.toolkit.screens

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
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

private const val TAG = "SgTraffic"
private const val API_URL = "https://api.data.gov.sg/v1/transport/traffic-images"
private const val API_KEY = "v2:3628e11212324251b93eccb69f46844aa70b7877ab736bf17f2f75deb7c19c0b:bmc3TaMdW9V-4pdcz5quf7qP-4VCHR2B"

private val httpClient = OkHttpClient.Builder()
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(15, TimeUnit.SECONDS)
    .writeTimeout(15, TimeUnit.SECONDS)
    .build()

private val gson = Gson()

private data class TrafficCamera(
    val cameraId: String,
    val imageUrl: String,
    val latitude: Double,
    val longitude: Double,
    val timestamp: String
)

@Suppress("UNCHECKED_CAST")
private fun parseTrafficCameras(json: String): List<TrafficCamera> {
    val type = object : TypeToken<Map<String, Any?>>() {}.type
    val root: Map<String, Any?> = gson.fromJson(json, type)
    val items = root["items"] as? List<Map<String, Any?>> ?: return emptyList()
    val firstItem = items.firstOrNull() ?: return emptyList()
    val cameras = firstItem["cameras"] as? List<Map<String, Any?>> ?: return emptyList()

    return cameras.mapNotNull { cam ->
        try {
            val location = cam["location"] as? Map<String, Any?>
            TrafficCamera(
                cameraId = cam["camera_id"]?.toString() ?: return@mapNotNull null,
                imageUrl = cam["image"]?.toString() ?: return@mapNotNull null,
                latitude = (location?.get("latitude") as? Number)?.toDouble() ?: 0.0,
                longitude = (location?.get("longitude") as? Number)?.toDouble() ?: 0.0,
                timestamp = cam["timestamp"]?.toString() ?: ""
            )
        } catch (e: Exception) {
            null
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SgTrafficScreen(navController: NavController) {
    val scope = rememberCoroutineScope()

    var cameras by remember { mutableStateOf<List<TrafficCamera>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var expandedCamera by remember { mutableStateOf<TrafficCamera?>(null) }

    suspend fun fetchCameras() {
        isLoading = true
        errorMessage = null
        try {
            val result = withContext(Dispatchers.IO) {
                val request = Request.Builder()
                    .url(API_URL)
                    .addHeader("x-api-key", API_KEY)
                    .build()
                val response = httpClient.newCall(request).execute()
                response.use { resp ->
                    if (!resp.isSuccessful) throw Exception("HTTP ${resp.code}")
                    parseTrafficCameras(resp.body.string())
                }
            }
            cameras = result
        } catch (e: Exception) {
            Log.e(TAG, "Fetch failed", e)
            errorMessage = e.message ?: "Unknown error"
        } finally {
            isLoading = false
        }
    }

    // Auto-refresh every 60s
    LaunchedEffect(Unit) {
        while (true) {
            fetchCameras()
            delay(60_000)
        }
    }

    // Expanded image dialog
    if (expandedCamera != null) {
        val cam = expandedCamera!!
        Dialog(
            onDismissRequest = { expandedCamera = null },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                shape = MaterialTheme.shapes.large
            ) {
                Column {
                    // Header
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Camera ${cam.cameraId}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                formatTimestamp(cam.timestamp),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { expandedCamera = null }) {
                            Icon(Icons.Default.Close, contentDescription = "Close")
                        }
                    }

                    // Full-width image
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(cam.imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Camera ${cam.cameraId}",
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(16f / 9f),
                        contentScale = ContentScale.Fit
                    )

                    // Location info
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "%.4f, %.4f".format(cam.latitude, cam.longitude),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("SG Traffic Cameras") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { scope.launch { fetchCameras() } },
                        enabled = !isLoading
                    ) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        bottomBar = { BannerAd() }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when {
                isLoading && cameras.isEmpty() -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                errorMessage != null && cameras.isEmpty() -> {
                    Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(errorMessage ?: "", textAlign = TextAlign.Center)
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = { scope.launch { fetchCameras() } }) {
                            Text("Retry")
                        }
                    }
                }
                else -> {
                    CameraGrid(
                        cameras = cameras,
                        isLoading = isLoading,
                        onCameraClick = { expandedCamera = it }
                    )
                }
            }
        }
    }
}

@Composable
private fun CameraGrid(
    cameras: List<TrafficCamera>,
    isLoading: Boolean,
    onCameraClick: (TrafficCamera) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Info bar
        item(span = { GridItemSpan(2) }) {
            Surface(
                tonalElevation = 2.dp,
                shape = MaterialTheme.shapes.small,
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
                            Icons.Default.CameraAlt,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            "${cameras.size} cameras active",
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                    if (isLoading) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(12.dp),
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                "Refreshing...",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        // Camera grid items
        items(cameras, key = { it.cameraId }) { camera ->
            CameraCell(camera = camera, onClick = { onCameraClick(camera) })
        }
    }
}

@Composable
private fun CameraCell(
    camera: TrafficCamera,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium
    ) {
        Column {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(camera.imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Camera ${camera.cameraId}",
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(MaterialTheme.shapes.medium),
                contentScale = ContentScale.Crop
            )
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    "Cam ${camera.cameraId}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    formatTimestamp(camera.timestamp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun formatTimestamp(timestamp: String): String {
    return try {
        // Input: "2026-04-05T11:36:04+08:00" -> "11:36:04"
        val timePart = timestamp.substringAfter("T").substringBefore("+").substringBefore("-")
        timePart.ifEmpty { timestamp }
    } catch (_: Exception) {
        timestamp
    }
}
