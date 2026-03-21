package cv.toolkit.screens

import android.content.Context
import android.graphics.ImageFormat
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CameraMetadata
import android.os.Build
import android.util.Size
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
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cv.toolkit.R
import cv.toolkit.ads.BannerAd
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

data class CameraInfo(
    val id: String,
    val facing: String,
    val megapixels: Float?,
    val maxResolution: Size?,
    val apertures: List<Float>,
    val focalLengths: List<Float>,
    val opticalStabilization: Boolean,
    val videoStabilization: Boolean,
    val flashSupported: Boolean,
    val autoFocusModes: List<String>,
    val hardwareLevel: String,
    val supportedFormats: List<String>,
    val maxZoom: Float?,
    val sensorSize: String?,
    val isoRange: String?,
    val exposureRange: String?,
    val capabilities: List<String>,
    val physicalSize: String?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CameraInfoScreen(navController: NavController) {
    val context = LocalContext.current
    var cameras by remember { mutableStateOf<List<CameraInfo>>(emptyList()) }
    var selectedCameraIndex by remember { mutableIntStateOf(0) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        try {
            cameras = getCameraInfoList(context)
        } catch (e: Exception) {
            errorMessage = "Failed to get camera info: ${e.message}"
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.camera_info_title)) },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Error message
            errorMessage?.let {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Error,
                            null,
                            tint = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            it,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
            }

            // Camera count summary
            if (cameras.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.CameraAlt,
                            null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text(
                                "${cameras.size} Camera${if (cameras.size > 1) "s" else ""} Detected",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                cameras.groupBy { it.facing }.map { "${it.value.size} ${it.key}" }.joinToString(", "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                            )
                        }
                    }
                }

                // Camera selector
                if (cameras.size > 1) {
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        cameras.forEachIndexed { index, camera ->
                            SegmentedButton(
                                selected = selectedCameraIndex == index,
                                onClick = { selectedCameraIndex = index },
                                shape = SegmentedButtonDefaults.itemShape(index, cameras.size),
                                icon = {
                                    Icon(
                                        when (camera.facing) {
                                            "Front" -> Icons.Filled.CameraFront
                                            "Back" -> Icons.Filled.CameraRear
                                            else -> Icons.Filled.Camera
                                        },
                                        null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            ) {
                                Text(
                                    "${camera.facing} ${camera.megapixels?.let { "${it.roundToInt()}MP" } ?: ""}",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }
            }

            // Camera details
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                if (cameras.isNotEmpty() && selectedCameraIndex < cameras.size) {
                    val camera = cameras[selectedCameraIndex]

                    // Basic info
                    item {
                        CameraDetailCard(
                            title = "Basic Info",
                            icon = Icons.Filled.Info,
                            items = listOfNotNull(
                                "Camera ID" to camera.id,
                                "Facing" to camera.facing,
                                camera.megapixels?.let { "Megapixels" to "${String.format("%.1f", it)} MP" },
                                camera.maxResolution?.let { "Max Resolution" to "${it.width} × ${it.height}" },
                                "Hardware Level" to camera.hardwareLevel
                            )
                        )
                    }

                    // Lens info
                    item {
                        CameraDetailCard(
                            title = "Lens",
                            icon = Icons.Filled.Camera,
                            items = listOfNotNull(
                                if (camera.apertures.isNotEmpty()) "Aperture" to camera.apertures.joinToString(", ") { "f/${it}" } else null,
                                if (camera.focalLengths.isNotEmpty()) "Focal Length" to camera.focalLengths.joinToString(", ") { "${it}mm" } else null,
                                camera.maxZoom?.let { "Max Zoom" to "${it}x" },
                                "Optical Stabilization" to if (camera.opticalStabilization) "Yes" else "No",
                                "Video Stabilization" to if (camera.videoStabilization) "Yes" else "No",
                                "Flash" to if (camera.flashSupported) "Yes" else "No"
                            )
                        )
                    }

                    // Sensor info
                    item {
                        CameraDetailCard(
                            title = "Sensor",
                            icon = Icons.Filled.Memory,
                            items = listOfNotNull(
                                camera.sensorSize?.let { "Sensor Size" to it },
                                camera.physicalSize?.let { "Physical Size" to it },
                                camera.isoRange?.let { "ISO Range" to it },
                                camera.exposureRange?.let { "Exposure Range" to it }
                            )
                        )
                    }

                    // Auto focus modes
                    if (camera.autoFocusModes.isNotEmpty()) {
                        item {
                            CameraDetailCard(
                                title = "Auto Focus Modes",
                                icon = Icons.Filled.CenterFocusStrong,
                                items = camera.autoFocusModes.mapIndexed { index, mode -> "${index + 1}" to mode }
                            )
                        }
                    }

                    // Capabilities
                    if (camera.capabilities.isNotEmpty()) {
                        item {
                            CameraDetailCard(
                                title = "Capabilities",
                                icon = Icons.Filled.CheckCircle,
                                items = camera.capabilities.mapIndexed { index, cap -> "${index + 1}" to cap }
                            )
                        }
                    }

                    // Supported formats
                    if (camera.supportedFormats.isNotEmpty()) {
                        item {
                            CameraDetailCard(
                                title = "Supported Formats",
                                icon = Icons.Filled.Image,
                                items = camera.supportedFormats.mapIndexed { index, format -> "${index + 1}" to format }
                            )
                        }
                    }

                    // Copy button
                    item {
                        OutlinedButton(
                            onClick = {
                                val text = buildCameraInfoText(camera)
                                scope.launch {
                                    clipboard.setClipEntry(
                                        androidx.compose.ui.platform.ClipEntry(
                                            android.content.ClipData.newPlainText("camera_info", text)
                                        )
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.ContentCopy, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Copy Camera Info")
                        }
                    }
                }

                // Empty state
                if (cameras.isEmpty() && errorMessage == null) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator()
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    "Loading camera info...",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }

                item {
                    BannerAd(modifier = Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun CameraDetailCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    items: List<Pair<String, String>>
) {
    if (items.isEmpty()) return

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    icon,
                    null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(12.dp))
            items.forEach { (label, value) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(0.4f)
                    )
                    Text(
                        value,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.weight(0.6f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.End
                    )
                }
            }
        }
    }
}

private fun getCameraInfoList(context: Context): List<CameraInfo> {
    val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
    val cameraList = mutableListOf<CameraInfo>()

    for (cameraId in cameraManager.cameraIdList) {
        try {
            val characteristics = cameraManager.getCameraCharacteristics(cameraId)
            cameraList.add(parseCameraCharacteristics(cameraId, characteristics))
        } catch (e: Exception) {
            // Skip cameras that can't be accessed
        }
    }

    return cameraList
}

private fun parseCameraCharacteristics(cameraId: String, chars: CameraCharacteristics): CameraInfo {
    // Facing
    val facing = when (chars.get(CameraCharacteristics.LENS_FACING)) {
        CameraCharacteristics.LENS_FACING_FRONT -> "Front"
        CameraCharacteristics.LENS_FACING_BACK -> "Back"
        CameraCharacteristics.LENS_FACING_EXTERNAL -> "External"
        else -> "Unknown"
    }

    // Resolution and megapixels
    val streamConfigMap = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
    val jpegSizes = streamConfigMap?.getOutputSizes(ImageFormat.JPEG)
    val maxResolution = jpegSizes?.maxByOrNull { it.width * it.height }
    val megapixels = maxResolution?.let { (it.width * it.height) / 1_000_000f }

    // Apertures
    val apertures = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES)?.toList() ?: emptyList()

    // Focal lengths
    val focalLengths = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.toList() ?: emptyList()

    // Stabilization
    val oisModes = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION) ?: intArrayOf()
    val opticalStabilization = oisModes.contains(CameraMetadata.LENS_OPTICAL_STABILIZATION_MODE_ON)

    val videoStabModes = chars.get(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES) ?: intArrayOf()
    val videoStabilization = videoStabModes.contains(CameraMetadata.CONTROL_VIDEO_STABILIZATION_MODE_ON)

    // Flash
    val flashSupported = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false

    // Auto focus modes
    val afModes = chars.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES) ?: intArrayOf()
    val autoFocusModes = afModes.toList().mapNotNull { mode ->
        when (mode) {
            CameraMetadata.CONTROL_AF_MODE_OFF -> "Off"
            CameraMetadata.CONTROL_AF_MODE_AUTO -> "Auto"
            CameraMetadata.CONTROL_AF_MODE_MACRO -> "Macro"
            CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_VIDEO -> "Continuous Video"
            CameraMetadata.CONTROL_AF_MODE_CONTINUOUS_PICTURE -> "Continuous Picture"
            CameraMetadata.CONTROL_AF_MODE_EDOF -> "Extended DOF"
            else -> null
        }
    }

    // Hardware level
    val hardwareLevel = when (chars.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL)) {
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY -> "Legacy"
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED -> "Limited"
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_FULL -> "Full"
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_3 -> "Level 3"
        CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL_EXTERNAL -> "External"
        else -> "Unknown"
    }

    // Supported formats
    val supportedFormats = streamConfigMap?.outputFormats?.toList()?.mapNotNull { format ->
        when (format) {
            ImageFormat.JPEG -> "JPEG"
            ImageFormat.RAW_SENSOR -> "RAW"
            ImageFormat.RAW10 -> "RAW10"
            ImageFormat.RAW12 -> "RAW12"
            ImageFormat.YUV_420_888 -> "YUV 420"
            ImageFormat.NV21 -> "NV21"
            ImageFormat.HEIC -> "HEIC"
            ImageFormat.DEPTH16 -> "Depth16"
            ImageFormat.DEPTH_POINT_CLOUD -> "Depth Point Cloud"
            else -> null
        }
    } ?: emptyList()

    // Max zoom
    val maxZoom = chars.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM)

    // Sensor size
    val sensorSize = chars.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE)?.let {
        "${String.format("%.2f", it.width)} × ${String.format("%.2f", it.height)} mm"
    }

    // Pixel array size
    val pixelArraySize = chars.get(CameraCharacteristics.SENSOR_INFO_PIXEL_ARRAY_SIZE)?.let {
        "${it.width} × ${it.height} px"
    }

    // ISO range
    val isoRange = chars.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)?.let {
        "${it.lower} - ${it.upper}"
    }

    // Exposure range
    val exposureRange = chars.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)?.let {
        val minMs = it.lower / 1_000_000.0
        val maxMs = it.upper / 1_000_000.0
        "${String.format("%.3f", minMs)}ms - ${String.format("%.0f", maxMs)}ms"
    }

    // Capabilities
    val capabilities = chars.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES)?.toList()?.mapNotNull { cap ->
        when (cap) {
            CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_BACKWARD_COMPATIBLE -> "Backward Compatible"
            CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR -> "Manual Sensor"
            CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_POST_PROCESSING -> "Manual Post Processing"
            CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_RAW -> "RAW"
            CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_READ_SENSOR_SETTINGS -> "Read Sensor Settings"
            CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_BURST_CAPTURE -> "Burst Capture"
            CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_DEPTH_OUTPUT -> "Depth Output"
            CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_PRIVATE_REPROCESSING -> "Private Reprocessing"
            CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_YUV_REPROCESSING -> "YUV Reprocessing"
            CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_CONSTRAINED_HIGH_SPEED_VIDEO -> "High Speed Video"
            CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_MOTION_TRACKING -> "Motion Tracking"
            CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_LOGICAL_MULTI_CAMERA -> "Multi-Camera"
            CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_MONOCHROME -> "Monochrome"
            CameraMetadata.REQUEST_AVAILABLE_CAPABILITIES_SECURE_IMAGE_DATA -> "Secure Image"
            else -> null
        }
    } ?: emptyList()

    return CameraInfo(
        id = cameraId,
        facing = facing,
        megapixels = megapixels,
        maxResolution = maxResolution,
        apertures = apertures,
        focalLengths = focalLengths,
        opticalStabilization = opticalStabilization,
        videoStabilization = videoStabilization,
        flashSupported = flashSupported,
        autoFocusModes = autoFocusModes,
        hardwareLevel = hardwareLevel,
        supportedFormats = supportedFormats,
        maxZoom = maxZoom,
        sensorSize = sensorSize,
        isoRange = isoRange,
        exposureRange = exposureRange,
        capabilities = capabilities,
        physicalSize = pixelArraySize
    )
}

private fun buildCameraInfoText(camera: CameraInfo): String {
    return buildString {
        appendLine("Camera Info - ${camera.facing} Camera (ID: ${camera.id})")
        appendLine("=" .repeat(40))
        appendLine()
        appendLine("Basic Info:")
        camera.megapixels?.let { appendLine("  Megapixels: ${String.format("%.1f", it)} MP") }
        camera.maxResolution?.let { appendLine("  Max Resolution: ${it.width} × ${it.height}") }
        appendLine("  Hardware Level: ${camera.hardwareLevel}")
        appendLine()
        appendLine("Lens:")
        if (camera.apertures.isNotEmpty()) appendLine("  Aperture: ${camera.apertures.joinToString(", ") { "f/$it" }}")
        if (camera.focalLengths.isNotEmpty()) appendLine("  Focal Length: ${camera.focalLengths.joinToString(", ") { "${it}mm" }}")
        camera.maxZoom?.let { appendLine("  Max Zoom: ${it}x") }
        appendLine("  Optical Stabilization: ${if (camera.opticalStabilization) "Yes" else "No"}")
        appendLine("  Video Stabilization: ${if (camera.videoStabilization) "Yes" else "No"}")
        appendLine("  Flash: ${if (camera.flashSupported) "Yes" else "No"}")
        appendLine()
        appendLine("Sensor:")
        camera.sensorSize?.let { appendLine("  Sensor Size: $it") }
        camera.physicalSize?.let { appendLine("  Pixel Array: $it") }
        camera.isoRange?.let { appendLine("  ISO Range: $it") }
        camera.exposureRange?.let { appendLine("  Exposure Range: $it") }
        appendLine()
        if (camera.autoFocusModes.isNotEmpty()) {
            appendLine("Auto Focus Modes: ${camera.autoFocusModes.joinToString(", ")}")
        }
        if (camera.capabilities.isNotEmpty()) {
            appendLine("Capabilities: ${camera.capabilities.joinToString(", ")}")
        }
        if (camera.supportedFormats.isNotEmpty()) {
            appendLine("Supported Formats: ${camera.supportedFormats.joinToString(", ")}")
        }
    }
}
