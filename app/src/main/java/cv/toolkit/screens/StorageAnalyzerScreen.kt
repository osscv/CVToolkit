package cv.toolkit.screens

import android.app.ActivityManager
import android.app.usage.StorageStatsManager
import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.storage.StorageManager
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cv.toolkit.R
import cv.toolkit.ads.BannerAd
import java.io.File

private data class StorageCategory(
    val name: String,
    val size: Long,
    val icon: ImageVector,
    val color: Color
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StorageAnalyzerScreen(navController: NavController) {
    val context = LocalContext.current
    val storageData = remember { getStorageData(context) }
    val ramData = remember { getRamData(context) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.storage_analyzer_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
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
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Internal Storage Ring Chart
                item {
                    StorageRingCard(
                        title = "Internal Storage",
                        total = storageData.totalInternal,
                        used = storageData.usedInternal,
                        categories = storageData.categories
                    )
                }

                // Category breakdown
                item {
                    Text(
                        "Storage Breakdown",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                items(storageData.categories) { cat ->
                    StorageCategoryCard(cat, storageData.totalInternal)
                }

                // External storage if available
                if (storageData.hasExternalStorage) {
                    item {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "External Storage",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Total", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(formatSize(storageData.totalExternal), fontWeight = FontWeight.Medium)
                                }
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Used", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(formatSize(storageData.usedExternal), fontWeight = FontWeight.Medium)
                                }
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Free", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(formatSize(storageData.totalExternal - storageData.usedExternal), fontWeight = FontWeight.Medium)
                                }
                                Spacer(Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { if (storageData.totalExternal > 0) storageData.usedExternal.toFloat() / storageData.totalExternal else 0f },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(8.dp),
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }
                    }
                }

                // RAM section
                item {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Memory (RAM)",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Total RAM", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(formatSize(ramData.total), fontWeight = FontWeight.Medium)
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Used RAM", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(formatSize(ramData.used), fontWeight = FontWeight.Medium)
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Available RAM", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(formatSize(ramData.available), fontWeight = FontWeight.Medium)
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Low Memory Threshold", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(formatSize(ramData.threshold), fontWeight = FontWeight.Medium)
                            }
                            Spacer(Modifier.height(12.dp))
                            LinearProgressIndicator(
                                progress = { if (ramData.total > 0) ramData.used.toFloat() / ramData.total else 0f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp),
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "${(ramData.used * 100 / ramData.total)}% used",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }
            }
            BannerAd(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun StorageRingCard(
    title: String,
    total: Long,
    used: Long,
    categories: List<StorageCategory>
) {
    val usedPercent = if (total > 0) (used * 100 / total).toInt() else 0

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(160.dp)
            ) {
                val trackColor = MaterialTheme.colorScheme.surfaceVariant
                Canvas(modifier = Modifier.size(160.dp)) {
                    val strokeWidth = 24f
                    val radius = (size.minDimension - strokeWidth) / 2
                    val topLeft = Offset(
                        (size.width - radius * 2) / 2,
                        (size.height - radius * 2) / 2
                    )
                    val arcSize = Size(radius * 2, radius * 2)

                    // Background track
                    drawArc(
                        color = trackColor,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth)
                    )

                    // Category arcs
                    var startAngle = -90f
                    categories.forEach { cat ->
                        val sweep = if (total > 0) (cat.size.toFloat() / total) * 360f else 0f
                        if (sweep > 0.5f) {
                            drawArc(
                                color = cat.color,
                                startAngle = startAngle,
                                sweepAngle = sweep,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = strokeWidth)
                            )
                        }
                        startAngle += sweep
                    }
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        "$usedPercent%",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "used",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Total", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatSize(total), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Used", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatSize(used), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Free", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatSize(total - used), style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun StorageCategoryCard(cat: StorageCategory, total: Long) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(40.dp),
                shape = MaterialTheme.shapes.small,
                color = cat.color.copy(alpha = 0.15f)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Icon(cat.icon, null, tint = cat.color, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(cat.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { if (total > 0) cat.size.toFloat() / total else 0f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = cat.color,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                formatSize(cat.size),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

private data class StorageData(
    val totalInternal: Long,
    val usedInternal: Long,
    val hasExternalStorage: Boolean,
    val totalExternal: Long,
    val usedExternal: Long,
    val categories: List<StorageCategory>
)

private data class RamData(
    val total: Long,
    val available: Long,
    val used: Long,
    val threshold: Long
)

private fun getStorageData(context: Context): StorageData {
    val internalStat = StatFs(Environment.getDataDirectory().path)
    val totalInternal = internalStat.totalBytes
    val freeInternal = internalStat.availableBytes
    val usedInternal = totalInternal - freeInternal

    // Check external storage
    val externalDirs = context.getExternalFilesDirs(null)
    var hasExternal = false
    var totalExternal = 0L
    var usedExternal = 0L
    if (externalDirs.size > 1 && externalDirs[1] != null) {
        try {
            val extStat = StatFs(externalDirs[1].path)
            totalExternal = extStat.totalBytes
            usedExternal = totalExternal - extStat.availableBytes
            hasExternal = true
        } catch (_: Exception) { }
    }

    // Estimate storage categories by scanning known directories
    val categories = mutableListOf<StorageCategory>()

    val appSize = estimateDirectorySize(context.dataDir)
    categories.add(StorageCategory("Apps & Data", appSize, Icons.Filled.Apps, Color(0xFF4285F4)))

    val dcim = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM)
    val pictures = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES)
    val photoSize = estimateDirectorySize(dcim) + estimateDirectorySize(pictures)
    categories.add(StorageCategory("Photos", photoSize, Icons.Filled.Photo, Color(0xFF34A853)))

    val movies = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
    val videoSize = estimateDirectorySize(movies)
    categories.add(StorageCategory("Videos", videoSize, Icons.Filled.Videocam, Color(0xFFEA4335)))

    val music = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC)
    val audioSize = estimateDirectorySize(music)
    categories.add(StorageCategory("Audio", audioSize, Icons.Filled.MusicNote, Color(0xFFFBBC05)))

    val downloads = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
    val downloadSize = estimateDirectorySize(downloads)
    categories.add(StorageCategory("Downloads", downloadSize, Icons.Filled.Download, Color(0xFF9C27B0)))

    val documents = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS)
    val docSize = estimateDirectorySize(documents)
    categories.add(StorageCategory("Documents", docSize, Icons.Filled.Description, Color(0xFFFF9800)))

    val categorizedSize = appSize + photoSize + videoSize + audioSize + downloadSize + docSize
    val systemAndOther = usedInternal - categorizedSize
    if (systemAndOther > 0) {
        categories.add(StorageCategory("System & Other", systemAndOther, Icons.Filled.Android, Color(0xFF607D8B)))
    }

    return StorageData(
        totalInternal = totalInternal,
        usedInternal = usedInternal,
        hasExternalStorage = hasExternal,
        totalExternal = totalExternal,
        usedExternal = usedExternal,
        categories = categories.sortedByDescending { it.size }
    )
}

private fun getRamData(context: Context): RamData {
    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val memInfo = ActivityManager.MemoryInfo()
    am.getMemoryInfo(memInfo)
    return RamData(
        total = memInfo.totalMem,
        available = memInfo.availMem,
        used = memInfo.totalMem - memInfo.availMem,
        threshold = memInfo.threshold
    )
}

private fun estimateDirectorySize(dir: File?): Long {
    if (dir == null || !dir.exists()) return 0
    return try {
        var size = 0L
        val files = dir.listFiles() ?: return 0
        for (file in files) {
            size += if (file.isDirectory) estimateDirectorySize(file) else file.length()
            if (size > 100L * 1024 * 1024 * 1024) break // Safety limit
        }
        size
    } catch (_: Exception) { 0 }
}

private fun formatSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var size = bytes.toDouble()
    var unitIndex = 0
    while (size >= 1024 && unitIndex < units.size - 1) {
        size /= 1024
        unitIndex++
    }
    return "%.1f %s".format(size, units[unitIndex])
}
