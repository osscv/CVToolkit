package cv.toolkit.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cv.toolkit.ads.BannerAd
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

private data class FaviconSize(val size: Int, val label: String, val description: String)

private val faviconSizes = listOf(
    FaviconSize(16, "16x16", "Browser tab favicon"),
    FaviconSize(32, "32x32", "Standard favicon"),
    FaviconSize(48, "48x48", "Windows site icon"),
    FaviconSize(64, "64x64", "Windows site icon (HiDPI)"),
    FaviconSize(96, "96x96", "Google TV"),
    FaviconSize(120, "120x120", "iPhone Retina"),
    FaviconSize(128, "128x128", "Chrome Web Store"),
    FaviconSize(144, "144x144", "Windows 8 tile"),
    FaviconSize(152, "152x152", "iPad Retina"),
    FaviconSize(167, "167x167", "iPad Pro"),
    FaviconSize(180, "180x180", "Apple Touch Icon"),
    FaviconSize(192, "192x192", "Android Chrome"),
    FaviconSize(256, "256x256", "Opera Speed Dial"),
    FaviconSize(384, "384x384", "Android Chrome (HiDPI)"),
    FaviconSize(512, "512x512", "PWA / Android Chrome")
)

private data class GeneratedFavicon(
    val size: Int,
    val label: String,
    val description: String,
    val bitmap: Bitmap,
    val bytes: ByteArray,
    val sizeBytes: Int
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FaviconGeneratorScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var sourceBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var sourceName by remember { mutableStateOf("") }
    var selectedSizes by remember { mutableStateOf(setOf(16, 32, 48, 128, 180, 192, 512)) }
    var generatedFavicons by remember { mutableStateOf<List<GeneratedFavicon>>(emptyList()) }
    var isProcessing by remember { mutableStateOf(false) }
    var processProgress by remember { mutableFloatStateOf(0f) }
    var pendingSaveIndex by remember { mutableIntStateOf(-1) }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                isProcessing = true
                try {
                    val result = withContext(Dispatchers.IO) { loadForFavicon(context, it) }
                    sourceBitmap = result.first
                    sourceName = result.second
                    generatedFavicons = emptyList()
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Failed to load: ${e.message}")
                } finally {
                    isProcessing = false
                }
            }
        }
    }

    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("image/png")
    ) { uri ->
        uri?.let { destUri ->
            val idx = pendingSaveIndex
            if (idx in generatedFavicons.indices) {
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            context.contentResolver.openOutputStream(destUri)?.use { it.write(generatedFavicons[idx].bytes) }
                        }
                        snackbarHostState.showSnackbar("Favicon saved")
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar("Failed to save: ${e.message}")
                    }
                }
            }
        }
    }

    fun generate() {
        val bitmap = sourceBitmap ?: return
        val sizes = faviconSizes.filter { it.size in selectedSizes }
        if (sizes.isEmpty()) return

        scope.launch {
            isProcessing = true
            processProgress = 0f
            try {
                val favicons = withContext(Dispatchers.IO) {
                    sizes.mapIndexed { index, fs ->
                        val scaled = Bitmap.createScaledBitmap(bitmap, fs.size, fs.size, true)
                        val baos = ByteArrayOutputStream()
                        scaled.compress(Bitmap.CompressFormat.PNG, 100, baos)
                        val bytes = baos.toByteArray()
                        processProgress = (index + 1).toFloat() / sizes.size
                        GeneratedFavicon(fs.size, fs.label, fs.description, scaled, bytes, bytes.size)
                    }
                }
                generatedFavicons = favicons
                snackbarHostState.showSnackbar("${favicons.size} favicon(s) generated")
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Generation failed: ${e.message}")
            } finally {
                isProcessing = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Favicon Generator") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Button(onClick = { imagePicker.launch("image/*") }, modifier = Modifier.fillMaxWidth(), enabled = !isProcessing) {
                        Icon(Icons.Filled.Image, null, Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Select Source Image")
                    }
                }

                if (sourceBitmap == null && !isProcessing) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.Web, null, Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Generate favicons in multiple sizes", style = MaterialTheme.typography.bodyLarge,
                                    textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("For websites, PWAs, app icons, and social media",
                                    style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                            }
                        }
                    }
                }

                sourceBitmap?.let { bitmap ->
                    // Source preview
                    item {
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Image(bitmap = bitmap.asImageBitmap(), contentDescription = null,
                                    modifier = Modifier.size(60.dp), contentScale = ContentScale.Fit)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(sourceName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                    Text("${bitmap.width} x ${bitmap.height}", style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }

                    // Size selection
                    if (generatedFavicons.isEmpty()) {
                        item {
                            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Select Sizes", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                        Row {
                                            TextButton(onClick = { selectedSizes = faviconSizes.map { it.size }.toSet() }) { Text("All") }
                                            TextButton(onClick = { selectedSizes = emptySet() }) { Text("None") }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    faviconSizes.forEach { fs ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = fs.size in selectedSizes,
                                                onCheckedChange = {
                                                    selectedSizes = if (it) selectedSizes + fs.size else selectedSizes - fs.size
                                                },
                                                enabled = !isProcessing
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(fs.label, style = MaterialTheme.typography.bodyMedium)
                                                Text(fs.description, style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Progress
                        if (isProcessing) {
                            item {
                                Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                    LinearProgressIndicator(progress = { processProgress }, modifier = Modifier.fillMaxWidth())
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("${(processProgress * 100).toInt()}%", style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        item {
                            Button(
                                onClick = { generate() },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isProcessing && selectedSizes.isNotEmpty()
                            ) {
                                Icon(Icons.Filled.AutoAwesome, null, Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Generate ${selectedSizes.size} Favicon${if (selectedSizes.size != 1) "s" else ""}")
                            }
                        }
                    }
                }

                // Generated favicons
                if (generatedFavicons.isNotEmpty()) {
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Generated Favicons", style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            TextButton(onClick = { generatedFavicons = emptyList() }) { Text("Regenerate") }
                        }
                    }

                    itemsIndexed(generatedFavicons) { index, favicon ->
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .background(
                                            MaterialTheme.colorScheme.surfaceVariant,
                                            shape = MaterialTheme.shapes.small
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val displaySize = (favicon.size.coerceAtMost(56)).dp
                                    Image(bitmap = favicon.bitmap.asImageBitmap(), contentDescription = favicon.label,
                                        modifier = Modifier.size(displaySize), contentScale = ContentScale.Fit)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(favicon.label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                    Text(favicon.description, style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(formatFavSize(favicon.sizeBytes.toLong()), style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                                OutlinedButton(onClick = {
                                    pendingSaveIndex = index
                                    val baseName = sourceName.substringBeforeLast(".")
                                    saveLauncher.launch("${baseName}_${favicon.size}x${favicon.size}.png")
                                }) {
                                    Icon(Icons.Filled.Save, null, Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Save", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }
                }
            }
            BannerAd(modifier = Modifier.fillMaxWidth())
        }
    }
}

private fun loadForFavicon(context: Context, uri: Uri): Pair<Bitmap, String> {
    var name = "icon"
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val ni = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (ni >= 0) name = cursor.getString(ni) ?: "icon"
        }
    }
    val inputStream = context.contentResolver.openInputStream(uri) ?: throw Exception("Cannot open file")
    val bitmap = inputStream.use { BitmapFactory.decodeStream(it) } ?: throw Exception("Cannot decode image")
    return Pair(bitmap, name)
}

private fun formatFavSize(bytes: Long): String {
    return when {
        bytes >= 1_048_576 -> String.format("%.2f MB", bytes / 1_048_576.0)
        bytes >= 1024 -> String.format("%.1f KB", bytes / 1024.0)
        else -> "$bytes B"
    }
}
