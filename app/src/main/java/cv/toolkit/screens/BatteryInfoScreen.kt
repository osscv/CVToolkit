package cv.toolkit.screens

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
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
import androidx.navigation.NavController
import cv.toolkit.R
import cv.toolkit.ads.BannerAd
import kotlinx.coroutines.delay
import java.io.File

private data class BatteryDetail(
    val label: String,
    val value: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BatteryInfoScreen(navController: NavController) {
    val context = LocalContext.current
    var details by remember { mutableStateOf(getBatteryDetails(context)) }

    // Auto-refresh every 3 seconds
    LaunchedEffect(Unit) {
        while (true) {
            delay(3000)
            details = getBatteryDetails(context)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.battery_info_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { details = getBatteryDetails(context) }) {
                        Icon(Icons.Filled.Refresh, contentDescription = "Refresh")
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
                // Battery level header card
                item {
                    val levelDetail = details.find { it.label == "Level" }
                    val statusDetail = details.find { it.label == "Status" }
                    val healthDetail = details.find { it.label == "Health" }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Filled.BatteryFull,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                levelDetail?.value ?: "N/A",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "${statusDetail?.value ?: "Unknown"} • ${healthDetail?.value ?: "Unknown"}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                item { Spacer(Modifier.height(8.dp)) }

                items(details.filter { it.label != "Level" && it.label != "Status" && it.label != "Health" }) { detail ->
                    BatteryDetailCard(detail)
                }
            }
            BannerAd(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun BatteryDetailCard(detail: BatteryDetail) {
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

private fun getBatteryDetails(context: Context): List<BatteryDetail> {
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
        BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE -> "Failure"
        else -> "Unknown"
    }

    val plugged = when (batteryIntent?.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)) {
        BatteryManager.BATTERY_PLUGGED_AC -> "AC Charger"
        BatteryManager.BATTERY_PLUGGED_USB -> "USB"
        BatteryManager.BATTERY_PLUGGED_WIRELESS -> "Wireless"
        else -> "Unplugged"
    }

    val temp = (batteryIntent?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10.0
    val voltage = batteryIntent?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
    val technology = batteryIntent?.getStringExtra(BatteryManager.EXTRA_TECHNOLOGY) ?: "Unknown"

    val currentNow = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)
    val currentMa = currentNow / 1000
    val currentAvg = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE) / 1000

    val powerNow = if (currentMa != 0 && voltage != 0) {
        "%.2f W".format(kotlin.math.abs(currentMa * voltage / 1000000.0))
    } else "N/A"

    val capacity = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER) / 1000
    val energyCounter = if (Build.VERSION.SDK_INT >= 21) {
        val energy = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER)
        if (energy > 0) "%.2f mWh".format(energy / 1000000.0) else "N/A"
    } else "N/A"

    val chargeTimeRemaining = if (Build.VERSION.SDK_INT >= 28) {
        val time = bm.computeChargeTimeRemaining()
        if (time > 0) {
            val hours = time / 3600000
            val mins = (time % 3600000) / 60000
            if (hours > 0) "${hours}h ${mins}m" else "${mins}m"
        } else "N/A"
    } else "N/A"

    val cycleCount = try {
        File("/sys/class/power_supply/battery/cycle_count").readText().trim()
    } catch (_: Exception) { "N/A" }

    val chargeType = try {
        File("/sys/class/power_supply/battery/charge_type").readText().trim()
    } catch (_: Exception) { "N/A" }

    return listOf(
        BatteryDetail("Level", "$percent%", Icons.Filled.BatteryFull),
        BatteryDetail("Status", status, Icons.Filled.Info),
        BatteryDetail("Health", health, Icons.Filled.HealthAndSafety),
        BatteryDetail("Temperature", "%.1f\u00B0C / %.1f\u00B0F".format(temp, temp * 9 / 5 + 32), Icons.Filled.Thermostat),
        BatteryDetail("Voltage", "$voltage mV", Icons.Filled.ElectricBolt),
        BatteryDetail("Current", "${kotlin.math.abs(currentMa)} mA", Icons.Filled.ElectricMeter),
        BatteryDetail("Average Current", "${kotlin.math.abs(currentAvg)} mA", @Suppress("DEPRECATION") Icons.Filled.ShowChart),
        BatteryDetail("Power", powerNow, Icons.Filled.Power),
        BatteryDetail("Power Source", plugged, Icons.Filled.SettingsInputHdmi),
        BatteryDetail("Technology", technology, Icons.Filled.Science),
        BatteryDetail("Capacity", if (capacity > 0) "$capacity mAh" else "N/A", Icons.Filled.Battery5Bar),
        BatteryDetail("Energy", energyCounter, Icons.Filled.EnergySavingsLeaf),
        BatteryDetail("Charge Cycles", cycleCount, Icons.Filled.Loop),
        BatteryDetail("Charge Type", chargeType, Icons.Filled.ChargingStation),
        BatteryDetail("Time to Full", chargeTimeRemaining, Icons.Filled.Schedule)
    )
}
