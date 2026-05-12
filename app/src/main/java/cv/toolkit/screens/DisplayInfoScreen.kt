package cv.toolkit.screens

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.util.DisplayMetrics
import android.view.WindowManager
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cv.toolkit.R
import cv.toolkit.ads.BannerAd
import kotlin.math.sqrt

private data class DisplayDetail(
    val label: String,
    val value: String,
    val icon: ImageVector
)

private data class DisplaySection(
    val title: String,
    val details: List<DisplayDetail>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DisplayInfoScreen(navController: NavController) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val sections = remember(configuration) { getDisplaySections(context) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.display_info_title)) },
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
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                sections.forEach { section ->
                    item {
                        Text(
                            section.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }
                    items(section.details) { detail ->
                        DisplayDetailCard(detail)
                    }
                }
            }
            BannerAd(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun DisplayDetailCard(detail: DisplayDetail) {
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
            Icon(
                detail.icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    detail.label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    detail.value,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

private fun getDisplaySections(context: Context): List<DisplaySection> {
    val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    val config = context.resources.configuration

    // Get screen dimensions
    val widthPx: Int
    val heightPx: Int
    val densityDpi: Int
    val xdpi: Float
    val ydpi: Float

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val windowMetrics = wm.currentWindowMetrics
        val bounds = windowMetrics.bounds
        widthPx = bounds.width()
        heightPx = bounds.height()
        val dm = context.resources.displayMetrics
        densityDpi = dm.densityDpi
        xdpi = dm.xdpi
        ydpi = dm.ydpi
    } else {
        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        wm.defaultDisplay.getRealMetrics(metrics)
        widthPx = metrics.widthPixels
        heightPx = metrics.heightPixels
        densityDpi = metrics.densityDpi
        xdpi = metrics.xdpi
        ydpi = metrics.ydpi
    }

    val density = context.resources.displayMetrics.density
    @Suppress("DEPRECATION")
    val scaledDensity = context.resources.displayMetrics.scaledDensity
    val widthDp = (widthPx / density).toInt()
    val heightDp = (heightPx / density).toInt()

    // Physical size in inches
    val widthInches = widthPx / xdpi.toDouble()
    val heightInches = heightPx / ydpi.toDouble()
    val diagonalInches = sqrt(widthInches * widthInches + heightInches * heightInches)

    // Refresh rate
    val refreshRate = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.display.refreshRate.toInt()
        } else {
            @Suppress("DEPRECATION")
            wm.defaultDisplay.refreshRate.toInt()
        }
    } catch (_: Exception) { 60 }

    // Supported refresh rates
    val supportedRefreshRates = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.display.supportedModes.map { it.refreshRate.toInt() }.distinct().sorted().joinToString(", ") { "${it} Hz" }
        } else {
            @Suppress("DEPRECATION")
            wm.defaultDisplay.supportedModes.map { it.refreshRate.toInt() }.distinct().sorted().joinToString(", ") { "${it} Hz" }
        }
    } catch (_: Exception) { "N/A" }

    // HDR support
    val hdrCapabilities = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
        try {
            val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.display
            } else {
                @Suppress("DEPRECATION")
                wm.defaultDisplay
            }
            val hdr = display?.hdrCapabilities
            val types = mutableListOf<String>()
            @Suppress("DEPRECATION")
            hdr?.supportedHdrTypes?.forEach { type ->
                when (type) {
                    android.view.Display.HdrCapabilities.HDR_TYPE_HDR10 -> types.add("HDR10")
                    android.view.Display.HdrCapabilities.HDR_TYPE_HLG -> types.add("HLG")
                    android.view.Display.HdrCapabilities.HDR_TYPE_DOLBY_VISION -> types.add("Dolby Vision")
                    android.view.Display.HdrCapabilities.HDR_TYPE_HDR10_PLUS -> types.add("HDR10+")
                }
            }
            if (types.isEmpty()) "Not Supported" else types.joinToString(", ")
        } catch (_: Exception) { "N/A" }
    } else "N/A (API < 24)"

    // Wide color gamut
    val wideColorGamut = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        try {
            val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) context.display
            else @Suppress("DEPRECATION") wm.defaultDisplay
            if (display?.isWideColorGamut == true) "Supported" else "Not Supported"
        } catch (_: Exception) { "N/A" }
    } else "N/A"

    val densityBucket = when {
        densityDpi <= 120 -> "ldpi"
        densityDpi <= 160 -> "mdpi"
        densityDpi <= 240 -> "hdpi"
        densityDpi <= 320 -> "xhdpi"
        densityDpi <= 480 -> "xxhdpi"
        else -> "xxxhdpi"
    }

    val orientation = when (config.orientation) {
        Configuration.ORIENTATION_LANDSCAPE -> "Landscape"
        Configuration.ORIENTATION_PORTRAIT -> "Portrait"
        else -> "Undefined"
    }

    val screenLayout = when (config.screenLayout and Configuration.SCREENLAYOUT_SIZE_MASK) {
        Configuration.SCREENLAYOUT_SIZE_SMALL -> "Small"
        Configuration.SCREENLAYOUT_SIZE_NORMAL -> "Normal"
        Configuration.SCREENLAYOUT_SIZE_LARGE -> "Large"
        Configuration.SCREENLAYOUT_SIZE_XLARGE -> "X-Large"
        else -> "Undefined"
    }

    val nightMode = when (config.uiMode and Configuration.UI_MODE_NIGHT_MASK) {
        Configuration.UI_MODE_NIGHT_YES -> "Dark Mode"
        Configuration.UI_MODE_NIGHT_NO -> "Light Mode"
        else -> "Undefined"
    }

    val fontScale = config.fontScale

    val aspectRatio = run {
        val gcd = gcd(widthPx, heightPx)
        "${heightPx / gcd}:${widthPx / gcd}"
    }

    return listOf(
        DisplaySection("Resolution & Size", listOf(
            DisplayDetail("Resolution", "${heightPx} \u00D7 ${widthPx} px", Icons.Filled.Fullscreen),
            DisplayDetail("DP Resolution", "${heightDp} \u00D7 ${widthDp} dp", Icons.Filled.AspectRatio),
            DisplayDetail("Aspect Ratio", aspectRatio, Icons.Filled.CropLandscape),
            DisplayDetail("Screen Size", "%.1f\"".format(diagonalInches), Icons.Filled.Straighten),
            DisplayDetail("Screen Layout", screenLayout, Icons.Filled.Smartphone)
        )),
        DisplaySection("Density", listOf(
            DisplayDetail("Density (DPI)", "$densityDpi dpi ($densityBucket)", Icons.Filled.GridOn),
            DisplayDetail("Density Scale", "%.2fx".format(density), Icons.Filled.ZoomIn),
            DisplayDetail("X DPI", "%.1f".format(xdpi), Icons.Filled.SwapHoriz),
            DisplayDetail("Y DPI", "%.1f".format(ydpi), Icons.Filled.SwapVert),
            DisplayDetail("Font Scale", "%.2fx".format(fontScale), Icons.Filled.TextFields),
            DisplayDetail("Scaled Density", "%.2f".format(scaledDensity), Icons.Filled.FormatSize)
        )),
        DisplaySection("Refresh Rate & HDR", listOf(
            DisplayDetail("Current Refresh Rate", "$refreshRate Hz", Icons.Filled.Speed),
            DisplayDetail("Supported Refresh Rates", supportedRefreshRates, Icons.Filled.Tune),
            DisplayDetail("HDR Support", hdrCapabilities, Icons.Filled.HdrOn),
            DisplayDetail("Wide Color Gamut", wideColorGamut, Icons.Filled.Palette)
        )),
        DisplaySection("Configuration", listOf(
            DisplayDetail("Orientation", orientation, Icons.Filled.ScreenRotation),
            DisplayDetail("Theme", nightMode, Icons.Filled.DarkMode)
        ))
    )
}

private fun gcd(a: Int, b: Int): Int = if (b == 0) a else gcd(b, a % b)
