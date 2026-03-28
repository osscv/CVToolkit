package cv.toolkit.screens

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.net.wifi.WifiManager
import android.provider.Settings
import android.telephony.TelephonyManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.util.DisplayMetrics
import android.util.SizeF
import android.view.WindowManager
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cv.toolkit.R
import cv.toolkit.ads.AdMobManager
import cv.toolkit.ads.BannerAd
import java.io.File

data class InfoItem(val label: String, val value: String)
data class InfoSection(val title: String, val icon: ImageVector, val items: List<InfoItem>)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeviceInfoScreen(navController: NavController) {
    val context = LocalContext.current
    val activity = context as? android.app.Activity
    val sections = remember { getDeviceInfoSections(context) }

    // Track usage for interstitial ad (every 2 views)
    LaunchedEffect(Unit) {
        activity?.let { AdMobManager.trackDeviceInfoUsage(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.device_info_title)) },
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
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                sections.forEach { section ->
                    item(span = { GridItemSpan(2) }) {
                        SectionHeader(section.title, section.icon)
                    }
                    items(section.items) { item ->
                        InfoCard(item)
                    }
                }
            }
            BannerAd(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun SectionHeader(title: String, icon: ImageVector) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(8.dp))
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun InfoCard(item: InfoItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(item.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(item.value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
        }
    }
}

private fun getDeviceInfoSections(context: Context): List<InfoSection> {
    return listOf(
        getDeviceSection(context),
        getSocSection(),
        getMemorySection(context),
        getScreenSection(context),
        getBatterySection(context),
        getConnectivitySection(context),
        getSystemSection(context)
    )
}

private fun getDeviceSection(context: Context): InfoSection {
    val tm = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    val androidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)

    val deviceType = when {
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_LEANBACK) -> "Android TV"
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE) -> "Android Auto"
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_WATCH) -> "Wear OS"
        context.resources.configuration.smallestScreenWidthDp >= 600 -> "Tablet"
        else -> "Phone"
    }

    val hasEsim = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        try { tm.isMultiSimSupported != TelephonyManager.MULTISIM_NOT_SUPPORTED_BY_HARDWARE } catch (_: Exception) { false }
    } else false

    val networkType = try {
        when (tm.dataNetworkType) {
            TelephonyManager.NETWORK_TYPE_LTE -> "4G LTE"
            TelephonyManager.NETWORK_TYPE_NR -> "5G NR"
            TelephonyManager.NETWORK_TYPE_HSPAP, TelephonyManager.NETWORK_TYPE_HSPA -> "3G HSPA"
            TelephonyManager.NETWORK_TYPE_EDGE, TelephonyManager.NETWORK_TYPE_GPRS -> "2G"
            else -> "Unknown"
        }
    } catch (_: Exception) { "N/A" }

    val operator = try { tm.networkOperatorName.ifEmpty { "N/A" } } catch (_: Exception) { "N/A" }
    val simOperator = try { tm.simOperatorName.ifEmpty { "N/A" } } catch (_: Exception) { "N/A" }

    return InfoSection("Device", Icons.Filled.Phone, listOf(
        InfoItem("Device Name", "${Build.MANUFACTURER} ${Build.MODEL}"),
        InfoItem("Model", Build.MODEL),
        InfoItem("Manufacturer", Build.MANUFACTURER),
        InfoItem("Device", Build.DEVICE),
        InfoItem("Board", Build.BOARD),
        InfoItem("Hardware", Build.HARDWARE),
        InfoItem("Brand", Build.BRAND),
        InfoItem("Android ID", androidId ?: "N/A"),
        InfoItem("Build Fingerprint", Build.FINGERPRINT.takeLast(40)),
        InfoItem("Device Type", deviceType),
        InfoItem("eSIM Support", if (hasEsim) "Yes" else "No"),
        InfoItem("Network Type", networkType),
        InfoItem("Network Operator", operator),
        InfoItem("SIM Operator", simOperator)
    ))
}

private fun getSocSection(): InfoSection {
    val cpuInfo = getCpuInfo()

    val governor = try {
        File("/sys/devices/system/cpu/cpu0/cpufreq/scaling_governor").readText().trim()
    } catch (_: Exception) { "N/A" }

    val cpuFreq = try {
        val max = File("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq").readText().trim().toLong() / 1000
        val min = File("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_min_freq").readText().trim().toLong() / 1000
        "$min - $max MHz"
    } catch (_: Exception) { "N/A" }

    val glInfo = try {
        javax.microedition.khronos.egl.EGLContext.getEGL().let { egl ->
            val egl10 = egl as javax.microedition.khronos.egl.EGL10
            val display = egl10.eglGetDisplay(javax.microedition.khronos.egl.EGL10.EGL_DEFAULT_DISPLAY)
            if (display == javax.microedition.khronos.egl.EGL10.EGL_NO_DISPLAY) {
                return@let Triple("N/A", "N/A", "N/A")
            }
            if (!egl10.eglInitialize(display, IntArray(2))) {
                return@let Triple("N/A", "N/A", "N/A")
            }
            val configs = arrayOfNulls<javax.microedition.khronos.egl.EGLConfig>(1)
            val numConfigs = IntArray(1)
            if (!egl10.eglChooseConfig(display, intArrayOf(
                javax.microedition.khronos.egl.EGL10.EGL_RENDERABLE_TYPE, 4,
                javax.microedition.khronos.egl.EGL10.EGL_NONE
            ), configs, 1, numConfigs) || numConfigs[0] == 0 || configs[0] == null) {
                egl10.eglTerminate(display)
                return@let Triple("N/A", "N/A", "N/A")
            }
            val ctx = egl10.eglCreateContext(display, configs[0], javax.microedition.khronos.egl.EGL10.EGL_NO_CONTEXT, intArrayOf(0x3098, 2, javax.microedition.khronos.egl.EGL10.EGL_NONE))
            if (ctx == javax.microedition.khronos.egl.EGL10.EGL_NO_CONTEXT) {
                egl10.eglTerminate(display)
                return@let Triple("N/A", "N/A", "N/A")
            }
            val surf = egl10.eglCreatePbufferSurface(display, configs[0], intArrayOf(javax.microedition.khronos.egl.EGL10.EGL_WIDTH, 1, javax.microedition.khronos.egl.EGL10.EGL_HEIGHT, 1, javax.microedition.khronos.egl.EGL10.EGL_NONE))
            if (surf == javax.microedition.khronos.egl.EGL10.EGL_NO_SURFACE) {
                egl10.eglDestroyContext(display, ctx)
                egl10.eglTerminate(display)
                return@let Triple("N/A", "N/A", "N/A")
            }
            egl10.eglMakeCurrent(display, surf, surf, ctx)
            val renderer = android.opengl.GLES20.glGetString(android.opengl.GLES20.GL_RENDERER) ?: "Unknown"
            val vendor = android.opengl.GLES20.glGetString(android.opengl.GLES20.GL_VENDOR) ?: "Unknown"
            val version = android.opengl.GLES20.glGetString(android.opengl.GLES20.GL_VERSION) ?: "Unknown"
            egl10.eglMakeCurrent(display, javax.microedition.khronos.egl.EGL10.EGL_NO_SURFACE, javax.microedition.khronos.egl.EGL10.EGL_NO_SURFACE, javax.microedition.khronos.egl.EGL10.EGL_NO_CONTEXT)
            egl10.eglDestroySurface(display, surf)
            egl10.eglDestroyContext(display, ctx)
            egl10.eglTerminate(display)
            Triple(renderer, vendor, version)
        }
    } catch (_: Exception) {
        Triple("N/A", "N/A", "N/A")
    }

    return InfoSection("CPU", Icons.Filled.Memory, listOf(
        InfoItem("Processor", cpuInfo["Processor"] ?: cpuInfo["model name"] ?: Build.HARDWARE),
        InfoItem("CPU Architecture", System.getProperty("os.arch") ?: "Unknown"),
        InfoItem("Supported ABIs", Build.SUPPORTED_ABIS.joinToString(", ")),
        InfoItem("CPU Hardware", cpuInfo["Hardware"] ?: Build.HARDWARE),
        InfoItem("CPU Type", Build.SUPPORTED_ABIS.firstOrNull() ?: "Unknown"),
        InfoItem("CPU Governor", governor),
        InfoItem("Cores", Runtime.getRuntime().availableProcessors().toString()),
        InfoItem("CPU Frequency", cpuFreq),
        InfoItem("GPU Renderer", glInfo.first),
        InfoItem("GPU Vendor", glInfo.second),
        InfoItem("GPU Version", glInfo.third)
    ))
}

private fun getMemorySection(context: Context): InfoSection {
    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    val memInfo = ActivityManager.MemoryInfo()
    activityManager.getMemoryInfo(memInfo)

    val totalRam = memInfo.totalMem / (1024 * 1024)
    val availRam = memInfo.availMem / (1024 * 1024)
    val usedRam = totalRam - availRam

    val stat = StatFs(Environment.getDataDirectory().path)
    val totalStorage = stat.totalBytes / (1024 * 1024 * 1024)
    val freeStorage = stat.availableBytes / (1024 * 1024 * 1024)
    val usedStorage = totalStorage - freeStorage

    return InfoSection("Memory", Icons.Filled.Storage, listOf(
        InfoItem("Total RAM", "${totalRam} MB"),
        InfoItem("Used RAM", "${usedRam} MB"),
        InfoItem("Available RAM", "${availRam} MB"),
        InfoItem("RAM Usage", "${(usedRam * 100 / totalRam)}%"),
        InfoItem("Total Storage", "${totalStorage} GB"),
        InfoItem("Used Storage", "${usedStorage} GB"),
        InfoItem("Free Storage", "${freeStorage} GB"),
        InfoItem("Storage Usage", "${(usedStorage * 100 / totalStorage)}%")
    ))
}

private data class ScreenInfo(val width: Int, val height: Int, val density: Int, val xdpi: Float, val ydpi: Float)

private fun getScreenSection(context: Context): InfoSection {
    val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    val screenInfo = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val windowMetrics = wm.currentWindowMetrics
            val bounds = windowMetrics.bounds
            val displayMetrics = context.resources.displayMetrics
            ScreenInfo(bounds.width(), bounds.height(), displayMetrics.densityDpi, displayMetrics.xdpi, displayMetrics.ydpi)
        } else {
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            wm.defaultDisplay.getRealMetrics(metrics)
            ScreenInfo(metrics.widthPixels, metrics.heightPixels, metrics.densityDpi, metrics.xdpi, metrics.ydpi)
        }
    } catch (_: Exception) {
        val displayMetrics = context.resources.displayMetrics
        ScreenInfo(displayMetrics.widthPixels, displayMetrics.heightPixels, displayMetrics.densityDpi, displayMetrics.xdpi, displayMetrics.ydpi)
    }

    val refreshRate = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.display.refreshRate.toInt()
        } else {
            @Suppress("DEPRECATION")
            wm.defaultDisplay.refreshRate.toInt()
        }
    } catch (_: Exception) { 60 }

    val densityBucket = when {
        screenInfo.density <= 120 -> "ldpi"
        screenInfo.density <= 160 -> "mdpi"
        screenInfo.density <= 240 -> "hdpi"
        screenInfo.density <= 320 -> "xhdpi"
        screenInfo.density <= 480 -> "xxhdpi"
        else -> "xxxhdpi"
    }

    return InfoSection("Screen", Icons.Filled.Smartphone, listOf(
        InfoItem("Resolution", "${screenInfo.height} × ${screenInfo.width}"),
        InfoItem("Density", "${screenInfo.density} dpi ($densityBucket)"),
        InfoItem("X DPI", "%.1f".format(screenInfo.xdpi)),
        InfoItem("Y DPI", "%.1f".format(screenInfo.ydpi)),
        InfoItem("Refresh Rate", "$refreshRate Hz")
    ))
}

private fun getBatterySection(context: Context): InfoSection {
    val batteryIntent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
    val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager

    val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
    val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
    val percent = if (level >= 0 && scale > 0) (level * 100 / scale) else -1

    val status = when (batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)) {
        BatteryManager.BATTERY_STATUS_CHARGING -> "Charging"
        BatteryManager.BATTERY_STATUS_DISCHARGING -> "Discharging"
        BatteryManager.BATTERY_STATUS_FULL -> "Full"
        BatteryManager.BATTERY_STATUS_NOT_CHARGING -> "Not Charging"
        else -> "Unknown"
    }

    val health = when (batteryIntent?.getIntExtra(BatteryManager.EXTRA_HEALTH, -1)) {
        BatteryManager.BATTERY_HEALTH_GOOD -> "Good"
        BatteryManager.BATTERY_HEALTH_OVERHEAT -> "Overheat"
        BatteryManager.BATTERY_HEALTH_DEAD -> "Dead"
        BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> "Over Voltage"
        BatteryManager.BATTERY_HEALTH_COLD -> "Cold"
        else -> "Unknown"
    }

    val plugged = when (batteryIntent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)) {
        BatteryManager.BATTERY_PLUGGED_AC -> "AC"
        BatteryManager.BATTERY_PLUGGED_USB -> "USB"
        BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
        else -> "Unplugged"
    }

    val temp = (batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10.0
    val voltage = batteryIntent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0

    val currentNow = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) / 1000
    val voltageNow = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
    val powerNow = if (currentNow != 0 && voltage != 0) "%.2f W".format(kotlin.math.abs(currentNow * voltage / 1000000.0)) else "N/A"

    val capacity = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER) / 1000
    val chargeTimeRemaining = if (Build.VERSION.SDK_INT >= 28) {
        val time = bm.computeChargeTimeRemaining()
        if (time > 0) "${time / 60000} min" else "N/A"
    } else "N/A"

    val cycleCount = try { File("/sys/class/power_supply/battery/cycle_count").readText().trim() } catch (_: Exception) { "N/A" }

    return InfoSection("Battery", Icons.Filled.BatteryFull, listOf(
        InfoItem("Health", health),
        InfoItem("Level", "$percent%"),
        InfoItem("Status", status),
        InfoItem("Power Source", plugged),
        InfoItem("Temperature", "%.1f°C".format(temp)),
        InfoItem("Current (mA)", "${kotlin.math.abs(currentNow)} mA"),
        InfoItem("Power (W)", powerNow),
        InfoItem("Voltage (mV)", "$voltage mV"),
        InfoItem("Time to Charge", chargeTimeRemaining),
        InfoItem("Charge Cycles", cycleCount),
        InfoItem("Capacity", if (capacity > 0) "$capacity mAh" else "N/A")
    ))
}

private fun getConnectivitySection(context: Context): InfoSection {
    val pm = context.packageManager
    val btAdapter = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            (context.getSystemService(Context.BLUETOOTH_SERVICE) as? android.bluetooth.BluetoothManager)?.adapter
        } else {
            @Suppress("DEPRECATION")
            android.bluetooth.BluetoothAdapter.getDefaultAdapter()
        }
    } catch (_: Exception) { null }

    val btSupported = pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH)
    val btLeSupported = pm.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)

    val btMultiAdv = try { btAdapter?.isMultipleAdvertisementSupported ?: false } catch (_: Exception) { false }
    val btOffloadFilter = try { btAdapter?.isOffloadedFilteringSupported ?: false } catch (_: Exception) { false }
    val btOffloadScan = try { btAdapter?.isOffloadedScanBatchingSupported ?: false } catch (_: Exception) { false }
    val btLe2mPhy = if (Build.VERSION.SDK_INT >= 26) try { btAdapter?.isLe2MPhySupported ?: false } catch (_: Exception) { false } else false
    val btLeCodedPhy = if (Build.VERSION.SDK_INT >= 26) try { btAdapter?.isLeCodedPhySupported ?: false } catch (_: Exception) { false } else false
    val btLeExtAdv = if (Build.VERSION.SDK_INT >= 26) try { btAdapter?.isLeExtendedAdvertisingSupported ?: false } catch (_: Exception) { false } else false
    val btLePeriodicAdv = if (Build.VERSION.SDK_INT >= 26) try { btAdapter?.isLePeriodicAdvertisingSupported ?: false } catch (_: Exception) { false } else false
    val btLeAudio = if (Build.VERSION.SDK_INT >= 33) try { pm.hasSystemFeature("android.hardware.bluetooth.le_audio") } catch (_: Exception) { false } else false

    val nfcSupported = pm.hasSystemFeature(PackageManager.FEATURE_NFC)
    val secureNfc = if (Build.VERSION.SDK_INT >= 29) {
        try { (context.getSystemService(Context.NFC_SERVICE) as? android.nfc.NfcAdapter)?.isSecureNfcSupported ?: false } catch (_: Exception) { false }
    } else false

    val uwbSupported = if (Build.VERSION.SDK_INT >= 31) pm.hasSystemFeature(PackageManager.FEATURE_UWB) else false
    val usbHost = pm.hasSystemFeature(PackageManager.FEATURE_USB_HOST)
    val usbAccessory = pm.hasSystemFeature(PackageManager.FEATURE_USB_ACCESSORY)
    val adbEnabled = Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED, 0) == 1

    fun yn(b: Boolean) = if (b) "Yes" else "No"

    return InfoSection("Connectivity", Icons.Filled.Wifi, listOf(
        InfoItem("Bluetooth", yn(btSupported)),
        InfoItem("Bluetooth LE", yn(btLeSupported)),
        InfoItem("Multiple Advertisement", yn(btMultiAdv)),
        InfoItem("Offload Filtering", yn(btOffloadFilter)),
        InfoItem("Offload Scan Batching", yn(btOffloadScan)),
        InfoItem("LE 2M PHY", yn(btLe2mPhy)),
        InfoItem("LE Coded PHY", yn(btLeCodedPhy)),
        InfoItem("LE Extended Advertising", yn(btLeExtAdv)),
        InfoItem("LE Periodic Advertising", yn(btLePeriodicAdv)),
        InfoItem("LE Audio", yn(btLeAudio)),
        InfoItem("NFC", yn(nfcSupported)),
        InfoItem("Secure NFC", yn(secureNfc)),
        InfoItem("Ultra Wideband", yn(uwbSupported)),
        InfoItem("USB Host", yn(usbHost)),
        InfoItem("USB Accessory", yn(usbAccessory)),
        InfoItem("USB Debugging", if (adbEnabled) "On" else "Off")
    ))
}

private fun getSystemSection(context: Context): InfoSection {
    val uptimeMs = android.os.SystemClock.elapsedRealtime()
    val hours = (uptimeMs / 3600000).toInt()
    val mins = ((uptimeMs % 3600000) / 60000).toInt()

    val gmsVersion = try {
        context.packageManager.getPackageInfo("com.google.android.gms", 0).versionName ?: "N/A"
    } catch (_: Exception) { "Not installed" }

    val openglVersion = (context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager)
        .deviceConfigurationInfo.glEsVersion

    val treble = if (Build.VERSION.SDK_INT >= 28) {
        getSystemPropertyBoolean("ro.treble.enabled", false)
    } else false

    val seamless = if (Build.VERSION.SDK_INT >= 28) {
        getSystemPropertyBoolean("ro.build.ab_update", false)
    } else false

    val dynamicPartitions = if (Build.VERSION.SDK_INT >= 29) {
        getSystemPropertyBoolean("ro.boot.dynamic_partitions", false)
    } else false

    val vulkanVersion = if (Build.VERSION.SDK_INT >= 24) {
        try {
            val pm = context.packageManager
            if (pm.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_VERSION)) {
                val info = pm.systemAvailableFeatures.find { it.name == PackageManager.FEATURE_VULKAN_HARDWARE_VERSION }
                if (info != null && info.version > 0) {
                    val major = (info.version shr 22) and 0x3FF
                    val minor = (info.version shr 12) and 0x3FF
                    val patch = info.version and 0xFFF
                    "$major.$minor.$patch"
                } else "Supported"
            } else "Not supported"
        } catch (_: Exception) { "Unknown" }
    } else "N/A"

    return InfoSection("System", Icons.Filled.Settings, listOf(
        InfoItem("Codename", if (Build.VERSION.SDK_INT >= 23) Build.VERSION.BASE_OS.ifEmpty { Build.VERSION.CODENAME } else Build.VERSION.CODENAME),
        InfoItem("API Level", Build.VERSION.SDK_INT.toString()),
        InfoItem("Android Version", Build.VERSION.RELEASE),
        InfoItem("MIUI Version", getSystemProperty("ro.miui.ui.version.name") ?: "N/A"),
        InfoItem("Security Patch", if (Build.VERSION.SDK_INT >= 23) Build.VERSION.SECURITY_PATCH else "N/A"),
        InfoItem("Bootloader", Build.BOOTLOADER),
        InfoItem("Build Number", Build.DISPLAY),
        InfoItem("Baseband", safeGetRadioVersion()),
        InfoItem("Java VM", System.getProperty("java.vm.name") ?: "Unknown"),
        InfoItem("Kernel", System.getProperty("os.version") ?: "Unknown"),
        InfoItem("Language", java.util.Locale.getDefault().displayLanguage),
        InfoItem("Timezone", java.util.TimeZone.getDefault().id),
        InfoItem("OpenGL ES", openglVersion),
        InfoItem("Google Play Services", gmsVersion),
        InfoItem("System Uptime", "${hours}h ${mins}m"),
        InfoItem("Vulkan", vulkanVersion),
        InfoItem("Treble", if (treble) "Supported" else "Not supported"),
        InfoItem("Seamless Updates", if (seamless) "Supported" else "Not supported"),
        InfoItem("Dynamic Partitions", if (dynamicPartitions) "Supported" else "Not supported")
    ))
}

private fun getSystemProperty(key: String): String? {
    return try {
        val result = Class.forName("android.os.SystemProperties")
            .getMethod("get", String::class.java)
            .invoke(null, key) as? String
        result?.takeIf { it.isNotEmpty() }
    } catch (_: Exception) { null }
    catch (_: Error) { null }
}

private fun getSystemPropertyBoolean(key: String, default: Boolean = false): Boolean {
    return try {
        Class.forName("android.os.SystemProperties")
            .getMethod("getBoolean", String::class.java, Boolean::class.javaPrimitiveType)
            .invoke(null, key, default) as? Boolean ?: default
    } catch (_: Exception) { default }
    catch (_: Error) { default }
}

private fun safeGetRadioVersion(): String {
    return try {
        Build.getRadioVersion()?.takeIf { it.isNotEmpty() } ?: "Unknown"
    } catch (_: Exception) { "Unknown" }
    catch (_: Error) { "Unknown" }
}

private fun getCpuInfo(): Map<String, String> {
    return try {
        File("/proc/cpuinfo").readLines()
            .filter { it.contains(":") }
            .associate {
                val parts = it.split(":", limit = 2)
                parts[0].trim() to parts.getOrElse(1) { "" }.trim()
            }
    } catch (_: Exception) { emptyMap() }
}
