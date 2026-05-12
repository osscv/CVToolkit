package cv.toolkit.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.GnssStatus
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Looper
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import cv.toolkit.R
import cv.toolkit.ads.BannerAd
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private data class LocationDetail(
    val label: String,
    val value: String,
    val icon: ImageVector
)

private data class SatelliteInfo(
    val constellation: String,
    val svid: Int,
    val cn0: Float,
    val elevation: Float,
    val azimuth: Float,
    val usedInFix: Boolean
)

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GpsLocationScreen(navController: NavController) {
    val context = LocalContext.current
    val locationManager = remember { context.getSystemService(Context.LOCATION_SERVICE) as LocationManager }

    var hasPermission by remember { mutableStateOf(false) }
    var location by remember { mutableStateOf<Location?>(null) }
    var address by remember { mutableStateOf("") }
    var satellites by remember { mutableStateOf(listOf<SatelliteInfo>()) }
    var totalSatellites by remember { mutableStateOf(0) }
    var usedSatellites by remember { mutableStateOf(0) }
    var isTracking by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasPermission = results.values.all { it }
    }

    LaunchedEffect(Unit) {
        hasPermission = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ).all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }

        if (!hasPermission) {
            permissionLauncher.launch(arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ))
        }
    }

    // GNSS Status callback for satellite info
    DisposableEffect(hasPermission, isTracking) {
        if (!hasPermission || !isTracking) return@DisposableEffect onDispose { }

        val gnssCallback = object : GnssStatus.Callback() {
            override fun onSatelliteStatusChanged(status: GnssStatus) {
                val satList = mutableListOf<SatelliteInfo>()
                var used = 0
                for (i in 0 until status.satelliteCount) {
                    val constellation = when (status.getConstellationType(i)) {
                        GnssStatus.CONSTELLATION_GPS -> "GPS"
                        GnssStatus.CONSTELLATION_GLONASS -> "GLONASS"
                        GnssStatus.CONSTELLATION_BEIDOU -> "BeiDou"
                        GnssStatus.CONSTELLATION_GALILEO -> "Galileo"
                        GnssStatus.CONSTELLATION_QZSS -> "QZSS"
                        GnssStatus.CONSTELLATION_SBAS -> "SBAS"
                        GnssStatus.CONSTELLATION_IRNSS -> "IRNSS"
                        else -> "Unknown"
                    }
                    val usedInFix = status.usedInFix(i)
                    if (usedInFix) used++
                    satList.add(SatelliteInfo(
                        constellation = constellation,
                        svid = status.getSvid(i),
                        cn0 = status.getCn0DbHz(i),
                        elevation = status.getElevationDegrees(i),
                        azimuth = status.getAzimuthDegrees(i),
                        usedInFix = usedInFix
                    ))
                }
                satellites = satList.sortedByDescending { it.cn0 }
                totalSatellites = status.satelliteCount
                usedSatellites = used
            }
        }

        try {
            locationManager.registerGnssStatusCallback(gnssCallback, android.os.Handler(Looper.getMainLooper()))
        } catch (_: SecurityException) { }

        onDispose {
            try { locationManager.unregisterGnssStatusCallback(gnssCallback) } catch (_: Exception) { }
        }
    }

    // Location updates
    DisposableEffect(hasPermission, isTracking) {
        if (!hasPermission || !isTracking) return@DisposableEffect onDispose { }

        val listener = android.location.LocationListener { loc ->
            location = loc
            // Reverse geocode
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    Geocoder(context, Locale.getDefault()).getFromLocation(loc.latitude, loc.longitude, 1) { addresses ->
                        address = addresses.firstOrNull()?.let { addr ->
                            (0..addr.maxAddressLineIndex).joinToString(", ") { addr.getAddressLine(it) }
                        } ?: ""
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val addresses = Geocoder(context, Locale.getDefault()).getFromLocation(loc.latitude, loc.longitude, 1)
                    address = addresses?.firstOrNull()?.let { addr ->
                        (0..addr.maxAddressLineIndex).joinToString(", ") { addr.getAddressLine(it) }
                    } ?: ""
                }
            } catch (_: Exception) {
                address = ""
            }
        }

        try {
            locationManager.requestLocationUpdates(
                LocationManager.GPS_PROVIDER,
                1000L,
                0f,
                listener,
                Looper.getMainLooper()
            )
        } catch (_: SecurityException) { }

        onDispose {
            locationManager.removeUpdates(listener)
        }
    }

    // Provider status
    val gpsEnabled = try { locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) } catch (_: Exception) { false }
    val networkEnabled = try { locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) } catch (_: Exception) { false }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.gps_location_title)) },
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
                // Start/Stop button
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Button(
                                onClick = {
                                    if (!hasPermission) {
                                        permissionLauncher.launch(arrayOf(
                                            Manifest.permission.ACCESS_FINE_LOCATION,
                                            Manifest.permission.ACCESS_COARSE_LOCATION
                                        ))
                                    } else {
                                        isTracking = !isTracking
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (isTracking) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("Stop Tracking")
                                } else {
                                    Icon(Icons.Filled.MyLocation, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Start GPS Tracking")
                                }
                            }

                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                ProviderChip("GPS", gpsEnabled)
                                ProviderChip("Network", networkEnabled)
                            }
                        }
                    }
                }

                // Coordinates card
                if (location != null) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Filled.LocationOn,
                                    null,
                                    modifier = Modifier.size(36.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "%.6f, %.6f".format(location!!.latitude, location!!.longitude),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                if (address.isNotEmpty()) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        address,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }

                    // Location details
                    item {
                        Text(
                            "Location Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                        )
                    }

                    val details = buildLocationDetails(location!!)
                    items(details) { detail ->
                        LocationDetailCard(detail)
                    }
                }

                // Satellites section
                if (satellites.isNotEmpty()) {
                    item {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Satellites ($usedSatellites used / $totalSatellites visible)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    // Constellation summary
                    item {
                        val constellationCounts = satellites.groupBy { it.constellation }
                            .map { (name, sats) ->
                                "$name: ${sats.count { it.usedInFix }}/${sats.size}"
                            }
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                constellationCounts.forEach { summary ->
                                    Text(summary, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }

                    items(satellites.take(20)) { sat ->
                        SatelliteCard(sat)
                    }
                }

                if (!isTracking && location == null) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 48.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Filled.SatelliteAlt,
                                    null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                                Spacer(Modifier.height(12.dp))
                                Text(
                                    "Start tracking to view GPS data",
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }
            BannerAd(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ProviderChip(name: String, enabled: Boolean) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = if (enabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                if (enabled) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                null,
                modifier = Modifier.size(14.dp),
                tint = if (enabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
            )
            Spacer(Modifier.width(4.dp))
            Text(
                name,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = if (enabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Composable
private fun LocationDetailCard(detail: LocationDetail) {
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
            Icon(detail.icon, null, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(detail.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(2.dp))
                Text(detail.value, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            }
        }
    }
}

@Composable
private fun SatelliteCard(sat: SatelliteInfo) {
    val signalColor = when {
        sat.cn0 >= 30 -> MaterialTheme.colorScheme.primary
        sat.cn0 >= 20 -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.error
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${sat.constellation} #${sat.svid}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium
                    )
                    if (sat.usedInFix) {
                        Spacer(Modifier.width(6.dp))
                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text(
                                "IN FIX",
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    "El: %.0f\u00B0  Az: %.0f\u00B0".format(sat.elevation, sat.azimuth),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                "%.1f dB".format(sat.cn0),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = signalColor
            )
        }
    }
}

private fun buildLocationDetails(loc: Location): List<LocationDetail> {
    val details = mutableListOf<LocationDetail>()
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    details.add(LocationDetail("Latitude", "%.6f\u00B0".format(loc.latitude), Icons.Filled.NorthEast))
    details.add(LocationDetail("Longitude", "%.6f\u00B0".format(loc.longitude), Icons.Filled.SouthEast))

    if (loc.hasAltitude()) {
        details.add(LocationDetail("Altitude", "%.1f m".format(loc.altitude), Icons.Filled.Terrain))
    }

    details.add(LocationDetail("Accuracy", "%.1f m".format(loc.accuracy), Icons.Filled.GpsFixed))

    if (loc.hasSpeed()) {
        val speedKmh = loc.speed * 3.6
        details.add(LocationDetail("Speed", "%.1f km/h (%.1f m/s)".format(speedKmh, loc.speed), Icons.Filled.Speed))
    }

    if (loc.hasBearing()) {
        val direction = when {
            loc.bearing < 22.5 || loc.bearing >= 337.5 -> "N"
            loc.bearing < 67.5 -> "NE"
            loc.bearing < 112.5 -> "E"
            loc.bearing < 157.5 -> "SE"
            loc.bearing < 202.5 -> "S"
            loc.bearing < 247.5 -> "SW"
            loc.bearing < 292.5 -> "W"
            else -> "NW"
        }
        details.add(LocationDetail("Bearing", "%.1f\u00B0 ($direction)".format(loc.bearing), Icons.Filled.Explore))
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && loc.hasVerticalAccuracy()) {
        details.add(LocationDetail("Vertical Accuracy", "%.1f m".format(loc.verticalAccuracyMeters), Icons.Filled.Height))
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && loc.hasSpeedAccuracy()) {
        details.add(LocationDetail("Speed Accuracy", "%.1f m/s".format(loc.speedAccuracyMetersPerSecond), Icons.Filled.Tune))
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && loc.hasBearingAccuracy()) {
        details.add(LocationDetail("Bearing Accuracy", "%.1f\u00B0".format(loc.bearingAccuracyDegrees), Icons.Filled.Tune))
    }

    details.add(LocationDetail("Provider", loc.provider ?: "N/A", Icons.Filled.SettingsInputAntenna))
    details.add(LocationDetail("Time", dateFormat.format(Date(loc.time)), Icons.Filled.Schedule))

    return details
}
