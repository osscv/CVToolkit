package cv.toolkit.screens

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Size
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import cv.toolkit.ads.BannerAd
import java.util.concurrent.Executors

data class ScanResult(
    val rawValue: String,
    val format: String,
    val type: String,
    val displayValue: String,
    val timestamp: Long = System.currentTimeMillis()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrScannerScreen(navController: NavController) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    
    var hasCameraPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        )
    }
    var scanResult by remember { mutableStateOf<ScanResult?>(null) }
    var isScanning by remember { mutableStateOf(true) }
    var flashEnabled by remember { mutableStateOf(false) }
    var scanHistory by remember { mutableStateOf<List<ScanResult>>(emptyList()) }
    var showHistory by remember { mutableStateOf(false) }
    var copyMessage by remember { mutableStateOf<String?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasCameraPermission = granted
        if (!granted) {
            // Permission denied
        }
    }

    // Check and request permission
    DisposableEffect(Unit) {
        val currentPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        
        if (currentPermission != hasCameraPermission) {
            hasCameraPermission = currentPermission
        }
        
        if (!hasCameraPermission) {
            permissionLauncher.launch(Manifest.permission.CAMERA)
        }
        
        onDispose { }
    }

    // Clear copy message after delay
    LaunchedEffect(copyMessage) {
        if (copyMessage != null) {
            kotlinx.coroutines.delay(2000)
            copyMessage = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("QR/Barcode Scanner") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showHistory = !showHistory }) {
                        Icon(
                            if (showHistory) Icons.Filled.CameraAlt else Icons.Filled.History,
                            if (showHistory) "Camera" else "History"
                        )
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
            if (!hasCameraPermission) {
                // Permission denied state
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Filled.CameraAlt,
                            null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Camera Permission Required",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Please grant camera permission to scan QR codes and barcodes.",
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                            Text("Grant Permission")
                        }
                    }
                }
            } else if (showHistory) {
                // History view
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "Scan History",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            if (scanHistory.isNotEmpty()) {
                                TextButton(onClick = { scanHistory = emptyList() }) {
                                    Text("Clear All")
                                }
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        if (scanHistory.isEmpty()) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .weight(1f),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Filled.History,
                                        null,
                                        modifier = Modifier.size(48.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        "No scan history yet",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(rememberScrollState()),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                scanHistory.forEach { result ->
                                    HistoryItem(
                                        result = result,
                                        onCopy = {
                                            copyToClipboard(context, result.rawValue)
                                            copyMessage = "Copied to clipboard"
                                        },
                                        onOpen = {
                                            openContent(context, result)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Camera preview
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CameraPreview(
                            isScanning = isScanning,
                            flashEnabled = flashEnabled,
                            onBarcodeDetected = { barcode ->
                                if (isScanning) {
                                    val result = ScanResult(
                                        rawValue = barcode.rawValue ?: "",
                                        format = getBarcodeFormatName(barcode.format),
                                        type = getBarcodeTypeName(barcode.valueType),
                                        displayValue = barcode.displayValue ?: barcode.rawValue ?: ""
                                    )
                                    scanResult = result
                                    scanHistory = listOf(result) + scanHistory.take(49)
                                    isScanning = false
                                }
                            }
                        )

                        // Scan overlay
                        Box(
                            modifier = Modifier
                                .size(250.dp)
                                .border(
                                    width = 3.dp,
                                    color = if (isScanning) MaterialTheme.colorScheme.primary else Color.Green,
                                    shape = RoundedCornerShape(16.dp)
                                )
                        )

                        // Flash toggle
                        IconButton(
                            onClick = { flashEnabled = !flashEnabled },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .background(
                                    MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                                    RoundedCornerShape(50)
                                )
                        ) {
                            Icon(
                                if (flashEnabled) Icons.Filled.FlashOn else Icons.Filled.FlashOff,
                                "Toggle flash",
                                tint = if (flashEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        }

                        // Scanning indicator
                        if (isScanning) {
                            Text(
                                "Point camera at a QR code or barcode",
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .padding(16.dp)
                                    .background(
                                        MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                                        RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }

                // Scan result
                scanResult?.let { result ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Filled.CheckCircle,
                                        null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Scan Successful",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                TextButton(onClick = {
                                    scanResult = null
                                    isScanning = true
                                }) {
                                    Text("Scan Again")
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                AssistChip(
                                    onClick = {},
                                    label = { Text(result.format) },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Filled.QrCode2,
                                            null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                )
                                AssistChip(
                                    onClick = {},
                                    label = { Text(result.type) },
                                    leadingIcon = {
                                        Icon(
                                            getTypeIcon(result.type),
                                            null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                )
                            }
                        }
                    }

                    // Content card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Content",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(Modifier.height(8.dp))
                            SelectionContainer {
                                Text(
                                    result.displayValue,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Spacer(Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        copyToClipboard(context, result.rawValue)
                                        copyMessage = "Copied to clipboard"
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(
                                        Icons.Filled.ContentCopy,
                                        null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Text("Copy")
                                }
                                if (canOpenContent(result)) {
                                    Button(
                                        onClick = { openContent(context, result) },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            Icons.AutoMirrored.Filled.OpenInNew,
                                            null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(4.dp))
                                        Text(getOpenButtonText(result))
                                    }
                                }
                            }
                        }
                    }
                }

                // Copy message
                copyMessage?.let {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(it, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }

                // Info card
                if (scanResult == null) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.Info,
                                null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "Supports QR codes, barcodes (EAN, UPC, Code 128, Code 39), Data Matrix, PDF417, and Aztec codes.",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))
            BannerAd(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun HistoryItem(
    result: ScanResult,
    onCopy: () -> Unit,
    onOpen: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        result.format,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        "•",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        result.type,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row {
                    IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Filled.ContentCopy,
                            "Copy",
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    if (canOpenContent(result)) {
                        IconButton(onClick = onOpen, modifier = Modifier.size(32.dp)) {
                            Icon(
                                Icons.AutoMirrored.Filled.OpenInNew,
                                "Open",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
            Text(
                result.displayValue,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}

@Composable
@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
private fun CameraPreview(
    isScanning: Boolean,
    flashEnabled: Boolean,
    onBarcodeDetected: (Barcode) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val cameraProviderFuture = remember { ProcessCameraProvider.getInstance(context) }
    var camera by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }

    // Update flash
    LaunchedEffect(flashEnabled, camera) {
        camera?.cameraControl?.enableTorch(flashEnabled)
    }

    DisposableEffect(lifecycleOwner) {
        val executor = Executors.newSingleThreadExecutor()
        
        onDispose {
            try {
                cameraProviderFuture.get()?.unbindAll()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            executor.shutdown()
        }
    }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            }

            val executor = Executors.newSingleThreadExecutor()
            val barcodeScanner = BarcodeScanning.getClient()

            cameraProviderFuture.addListener({
                try {
                    val cameraProvider = cameraProviderFuture.get()

                    val preview = Preview.Builder()
                        .build()
                        .also {
                            it.surfaceProvider = previewView.surfaceProvider
                        }

                    val resolutionSelector = ResolutionSelector.Builder()
                        .setResolutionStrategy(
                            ResolutionStrategy(
                                android.util.Size(1280, 720),
                                ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER
                            )
                        )
                        .build()
                    
                    val imageAnalysis = ImageAnalysis.Builder()
                        .setResolutionSelector(resolutionSelector)
                        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { analysis ->
                            analysis.setAnalyzer(executor) { imageProxy ->
                                val mediaImage = imageProxy.image
                                if (mediaImage != null && isScanning) {
                                    val image = InputImage.fromMediaImage(
                                        mediaImage,
                                        imageProxy.imageInfo.rotationDegrees
                                    )
                                    barcodeScanner.process(image)
                                        .addOnSuccessListener { barcodes ->
                                            barcodes.firstOrNull()?.let { barcode ->
                                                if (barcode.rawValue != null) {
                                                    onBarcodeDetected(barcode)
                                                }
                                            }
                                        }
                                        .addOnFailureListener { e ->
                                            e.printStackTrace()
                                        }
                                        .addOnCompleteListener {
                                            imageProxy.close()
                                        }
                                } else {
                                    imageProxy.close()
                                }
                            }
                        }

                    val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                    try {
                        cameraProvider.unbindAll()
                        camera = cameraProvider.bindToLifecycle(
                            lifecycleOwner,
                            cameraSelector,
                            preview,
                            imageAnalysis
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }, ContextCompat.getMainExecutor(ctx))

            previewView
        },
        update = { previewView ->
            // Update if needed
        },
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(12.dp))
    )
}

@Composable
private fun SelectionContainer(content: @Composable () -> Unit) {
    androidx.compose.foundation.text.selection.SelectionContainer {
        content()
    }
}

private fun getBarcodeFormatName(format: Int): String {
    return when (format) {
        Barcode.FORMAT_QR_CODE -> "QR Code"
        Barcode.FORMAT_AZTEC -> "Aztec"
        Barcode.FORMAT_CODABAR -> "Codabar"
        Barcode.FORMAT_CODE_39 -> "Code 39"
        Barcode.FORMAT_CODE_93 -> "Code 93"
        Barcode.FORMAT_CODE_128 -> "Code 128"
        Barcode.FORMAT_DATA_MATRIX -> "Data Matrix"
        Barcode.FORMAT_EAN_8 -> "EAN-8"
        Barcode.FORMAT_EAN_13 -> "EAN-13"
        Barcode.FORMAT_ITF -> "ITF"
        Barcode.FORMAT_PDF417 -> "PDF417"
        Barcode.FORMAT_UPC_A -> "UPC-A"
        Barcode.FORMAT_UPC_E -> "UPC-E"
        else -> "Unknown"
    }
}

private fun getBarcodeTypeName(type: Int): String {
    return when (type) {
        Barcode.TYPE_URL -> "URL"
        Barcode.TYPE_WIFI -> "WiFi"
        Barcode.TYPE_EMAIL -> "Email"
        Barcode.TYPE_PHONE -> "Phone"
        Barcode.TYPE_SMS -> "SMS"
        Barcode.TYPE_GEO -> "Location"
        Barcode.TYPE_CONTACT_INFO -> "Contact"
        Barcode.TYPE_CALENDAR_EVENT -> "Calendar"
        Barcode.TYPE_PRODUCT -> "Product"
        Barcode.TYPE_ISBN -> "ISBN"
        Barcode.TYPE_TEXT -> "Text"
        Barcode.TYPE_DRIVER_LICENSE -> "Driver License"
        else -> "Text"
    }
}

private fun getTypeIcon(type: String) = when (type) {
    "URL" -> Icons.Filled.Link
    "WiFi" -> Icons.Filled.Wifi
    "Email" -> Icons.Filled.Email
    "Phone" -> Icons.Filled.Phone
    "SMS" -> Icons.Filled.Sms
    "Location" -> Icons.Filled.LocationOn
    "Contact" -> Icons.Filled.Person
    "Calendar" -> Icons.Filled.CalendarMonth
    "Product" -> Icons.Filled.ShoppingCart
    "ISBN" -> Icons.Filled.Book
    else -> Icons.Filled.TextFields
}

private fun canOpenContent(result: ScanResult): Boolean {
    return result.type in listOf("URL", "Phone", "Email", "SMS", "Location")
}

private fun getOpenButtonText(result: ScanResult): String {
    return when (result.type) {
        "URL" -> "Open"
        "Phone" -> "Call"
        "Email" -> "Email"
        "SMS" -> "Message"
        "Location" -> "Map"
        else -> "Open"
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText("Scanned content", text)
    clipboard.setPrimaryClip(clip)
}

private fun openContent(context: Context, result: ScanResult) {
    try {
        val intent = when (result.type) {
            "URL" -> {
                val url = if (result.rawValue.startsWith("http://") || result.rawValue.startsWith("https://")) {
                    result.rawValue
                } else {
                    "https://${result.rawValue}"
                }
                Intent(Intent.ACTION_VIEW, Uri.parse(url))
            }
            "Phone" -> {
                val phone = result.rawValue.removePrefix("tel:")
                Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
            }
            "Email" -> {
                if (result.rawValue.startsWith("mailto:")) {
                    Intent(Intent.ACTION_VIEW, Uri.parse(result.rawValue))
                } else {
                    Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${result.rawValue}"))
                }
            }
            "SMS" -> {
                if (result.rawValue.startsWith("sms:") || result.rawValue.startsWith("smsto:")) {
                    Intent(Intent.ACTION_VIEW, Uri.parse(result.rawValue))
                } else {
                    Intent(Intent.ACTION_VIEW, Uri.parse("sms:${result.rawValue}"))
                }
            }
            "Location" -> {
                Intent(Intent.ACTION_VIEW, Uri.parse(result.rawValue))
            }
            else -> null
        }
        intent?.let { context.startActivity(it) }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
