package cv.toolkit.screens

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
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

private data class SensorData(
    val name: String,
    val type: Int,
    val icon: ImageVector,
    val values: FloatArray?,
    val available: Boolean,
    val unit: String,
    val labels: List<String>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SensorData) return false
        return name == other.name && type == other.type && available == other.available &&
                unit == other.unit && labels == other.labels &&
                (values contentEquals other.values)
    }

    override fun hashCode(): Int {
        var result = name.hashCode()
        result = 31 * result + type
        result = 31 * result + (values?.contentHashCode() ?: 0)
        result = 31 * result + available.hashCode()
        result = 31 * result + unit.hashCode()
        result = 31 * result + labels.hashCode()
        return result
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SensorDashboardScreen(navController: NavController) {
    val context = LocalContext.current
    val sensorManager = remember {
        context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }

    val sensorConfigs = remember {
        listOf(
            SensorData("Accelerometer", Sensor.TYPE_ACCELEROMETER, Icons.Filled.Speed, null, false, "m/s\u00B2", listOf("X", "Y", "Z")),
            SensorData("Gyroscope", Sensor.TYPE_GYROSCOPE, Icons.Filled.RotateRight, null, false, "rad/s", listOf("X", "Y", "Z")),
            SensorData("Magnetometer", Sensor.TYPE_MAGNETIC_FIELD, Icons.Filled.Explore, null, false, "\u00B5T", listOf("X", "Y", "Z")),
            SensorData("Light", Sensor.TYPE_LIGHT, Icons.Filled.LightMode, null, false, "lux", listOf("Illuminance")),
            SensorData("Proximity", Sensor.TYPE_PROXIMITY, Icons.Filled.NearMe, null, false, "cm", listOf("Distance")),
            SensorData("Pressure", Sensor.TYPE_PRESSURE, Icons.Filled.Compress, null, false, "hPa", listOf("Pressure")),
            SensorData("Gravity", Sensor.TYPE_GRAVITY, Icons.Filled.FitnessCenter, null, false, "m/s\u00B2", listOf("X", "Y", "Z")),
            SensorData("Rotation Vector", Sensor.TYPE_ROTATION_VECTOR, Icons.Filled.ThreeDRotation, null, false, "", listOf("X", "Y", "Z"))
        )
    }

    val sensorStates = remember {
        sensorConfigs.map { config ->
            val sensor = sensorManager.getDefaultSensor(config.type)
            mutableStateOf(config.copy(available = sensor != null))
        }
    }

    DisposableEffect(sensorManager) {
        val listeners = mutableListOf<Pair<SensorEventListener, Sensor>>()

        sensorConfigs.forEachIndexed { index, config ->
            val sensor = sensorManager.getDefaultSensor(config.type)
            if (sensor != null) {
                val listener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent) {
                        sensorStates[index].value = sensorStates[index].value.copy(
                            values = event.values.copyOf(),
                            available = true
                        )
                    }

                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
                }
                sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_UI)
                listeners.add(listener to sensor)
            }
        }

        onDispose {
            listeners.forEach { (listener, _) ->
                sensorManager.unregisterListener(listener)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.sensor_dashboard_title)) },
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
                items(sensorStates.size) { index ->
                    val sensorData = sensorStates[index].value
                    SensorCard(sensorData = sensorData, sensorManager = sensorManager)
                }
            }
            BannerAd(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun SensorCard(sensorData: SensorData, sensorManager: SensorManager) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    sensorData.icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = if (sensorData.available) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
                Spacer(Modifier.width(12.dp))
                Text(
                    sensorData.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = if (sensorData.available) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Spacer(Modifier.weight(1f))
                if (sensorData.available) {
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            "LIVE",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            if (!sensorData.available) {
                Text(
                    "Not Available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            } else if (sensorData.values == null) {
                Text(
                    "Waiting for data...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                when (sensorData.type) {
                    Sensor.TYPE_LIGHT -> LightSensorContent(sensorData)
                    Sensor.TYPE_PROXIMITY -> ProximitySensorContent(sensorData, sensorManager)
                    else -> DefaultSensorContent(sensorData)
                }
            }
        }
    }
}

@Composable
private fun DefaultSensorContent(sensorData: SensorData) {
    val values = sensorData.values ?: return
    sensorData.labels.forEachIndexed { index, label ->
        if (index < values.size) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "${"%.2f".format(values[index])} ${sensorData.unit}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun LightSensorContent(sensorData: SensorData) {
    val values = sensorData.values ?: return
    val lux = values[0]
    val description = when {
        lux < 10f -> "Dark"
        lux < 50f -> "Dim"
        lux < 500f -> "Indoor"
        lux < 10000f -> "Outdoor"
        else -> "Direct Sunlight"
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            "Illuminance",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "${"%.2f".format(lux)} ${sensorData.unit}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
    Spacer(Modifier.height(4.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            "Level",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            description,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun ProximitySensorContent(sensorData: SensorData, sensorManager: SensorManager) {
    val values = sensorData.values ?: return
    val distance = values[0]
    val proximitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)
    val maxRange = proximitySensor?.maximumRange ?: 5f
    val isNear = distance < maxRange

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            "Distance",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            "${"%.2f".format(distance)} ${sensorData.unit}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
    Spacer(Modifier.height(4.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            "Status",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            if (isNear) "Near" else "Far",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = if (isNear) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
        )
    }
}
