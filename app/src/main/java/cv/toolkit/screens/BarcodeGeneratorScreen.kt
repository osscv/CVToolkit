package cv.toolkit.screens

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cv.toolkit.ads.BannerAd
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.zxing.BarcodeFormat
import com.google.zxing.MultiFormatWriter
import com.google.zxing.common.BitMatrix
import android.content.ContentValues
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream

enum class BarcodeType(
    val displayName: String,
    val format: BarcodeFormat,
    val description: String,
    val example: String,
    val validation: (String) -> String?
) {
    EAN_13(
        "EAN-13",
        BarcodeFormat.EAN_13,
        "13-digit product barcode (European/International)",
        "5901234123457",
        { text ->
            when {
                text.length != 13 -> "Must be exactly 13 digits"
                !text.all { it.isDigit() } -> "Must contain only digits"
                else -> null
            }
        }
    ),
    EAN_8(
        "EAN-8",
        BarcodeFormat.EAN_8,
        "8-digit short product barcode",
        "12345670",
        { text ->
            when {
                text.length != 8 -> "Must be exactly 8 digits"
                !text.all { it.isDigit() } -> "Must contain only digits"
                else -> null
            }
        }
    ),
    UPC_A(
        "UPC-A",
        BarcodeFormat.UPC_A,
        "12-digit product barcode (North America)",
        "123456789012",
        { text ->
            when {
                text.length != 12 -> "Must be exactly 12 digits"
                !text.all { it.isDigit() } -> "Must contain only digits"
                else -> null
            }
        }
    ),
    UPC_E(
        "UPC-E",
        BarcodeFormat.UPC_E,
        "8-digit compressed UPC barcode",
        "01234565",
        { text ->
            when {
                text.length != 8 -> "Must be exactly 8 digits"
                !text.all { it.isDigit() } -> "Must contain only digits"
                else -> null
            }
        }
    ),
    CODE_128(
        "Code 128",
        BarcodeFormat.CODE_128,
        "High-density barcode for alphanumeric data",
        "ABC-123456",
        { text ->
            when {
                text.isEmpty() -> "Cannot be empty"
                text.length > 80 -> "Maximum 80 characters"
                else -> null
            }
        }
    ),
    CODE_39(
        "Code 39",
        BarcodeFormat.CODE_39,
        "Alphanumeric barcode (uppercase letters, digits, some symbols)",
        "CODE39",
        { text ->
            val allowed = Regex("^[A-Z0-9 \\-.$/+%]*$")
            when {
                text.isEmpty() -> "Cannot be empty"
                !allowed.matches(text) -> "Only uppercase letters, digits, and -./+%\$ space allowed"
                else -> null
            }
        }
    ),
    CODE_93(
        "Code 93",
        BarcodeFormat.CODE_93,
        "Compact barcode for alphanumeric data",
        "CODE93",
        { text ->
            when {
                text.isEmpty() -> "Cannot be empty"
                text.length > 80 -> "Maximum 80 characters"
                else -> null
            }
        }
    ),
    CODABAR(
        "Codabar",
        BarcodeFormat.CODABAR,
        "Barcode for numbers, used in libraries & blood banks",
        "A123456B",
        { text ->
            val allowed = Regex("^[A-D][0-9\\-\$:/.+]+[A-D]$")
            when {
                text.isEmpty() -> "Cannot be empty"
                !allowed.matches(text) -> "Must start/end with A-D, contain digits and -\$:/.+"
                else -> null
            }
        }
    ),
    ITF(
        "ITF (Interleaved 2 of 5)",
        BarcodeFormat.ITF,
        "Numeric-only barcode for logistics",
        "1234567890",
        { text ->
            when {
                text.isEmpty() -> "Cannot be empty"
                text.length % 2 != 0 -> "Must have even number of digits"
                !text.all { it.isDigit() } -> "Must contain only digits"
                else -> null
            }
        }
    ),
    PDF_417(
        "PDF417",
        BarcodeFormat.PDF_417,
        "2D stacked barcode for large data capacity",
        "PDF417 Barcode",
        { text ->
            when {
                text.isEmpty() -> "Cannot be empty"
                text.length > 2700 -> "Maximum 2700 characters"
                else -> null
            }
        }
    ),
    DATA_MATRIX(
        "Data Matrix",
        BarcodeFormat.DATA_MATRIX,
        "2D matrix barcode for small items",
        "DataMatrix123",
        { text ->
            when {
                text.isEmpty() -> "Cannot be empty"
                text.length > 2335 -> "Maximum 2335 characters"
                else -> null
            }
        }
    ),
    AZTEC(
        "Aztec",
        BarcodeFormat.AZTEC,
        "2D barcode with high data capacity",
        "Aztec Code",
        { text ->
            when {
                text.isEmpty() -> "Cannot be empty"
                text.length > 3000 -> "Maximum 3000 characters"
                else -> null
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BarcodeGeneratorScreen(navController: NavController) {
    var selectedType by remember { mutableStateOf(BarcodeType.CODE_128) }
    var inputText by remember { mutableStateOf("") }
    var barcodeBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var barcodeWidth by remember { mutableFloatStateOf(800f) }
    var barcodeHeight by remember { mutableFloatStateOf(300f) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var saveMessage by remember { mutableStateOf<String?>(null) }
    var validationError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    fun generateBarcode() {
        val text = inputText.trim()
        if (text.isEmpty()) {
            barcodeBitmap = null
            validationError = null
            return
        }

        // Validate input
        val validation = selectedType.validation(text)
        if (validation != null) {
            validationError = validation
            barcodeBitmap = null
            return
        }

        validationError = null
        errorMessage = null
        scope.launch {
            try {
                val bitmap = withContext(Dispatchers.Default) {
                    generateBarcodeBitmap(text, selectedType.format, barcodeWidth.toInt(), barcodeHeight.toInt())
                }
                barcodeBitmap = bitmap
            } catch (e: Exception) {
                errorMessage = "Failed to generate barcode: ${e.message}"
                barcodeBitmap = null
            }
        }
    }

    fun saveBarcode() {
        val bitmap = barcodeBitmap ?: return
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val filename = "Barcode_${selectedType.displayName.replace(" ", "_")}_${System.currentTimeMillis()}.png"

                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        val contentValues = ContentValues().apply {
                            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
                            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES + "/CVToolkit")
                        }
                        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                        uri?.let {
                            context.contentResolver.openOutputStream(it)?.use { stream ->
                                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                            }
                        }
                    } else {
                        @Suppress("DEPRECATION")
                        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "CVToolkit")
                        if (!dir.exists()) dir.mkdirs()
                        val file = File(dir, filename)
                        FileOutputStream(file).use { stream ->
                            bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
                        }
                    }
                }
                saveMessage = "Barcode saved to Pictures/CVToolkit"
            } catch (e: Exception) {
                saveMessage = "Failed to save: ${e.message}"
            }
        }
    }

    // Auto-generate when input changes
    LaunchedEffect(selectedType, inputText, barcodeWidth, barcodeHeight) {
        generateBarcode()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Barcode Generator") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Barcode Type selector
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "Barcode Type",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    // 1D Barcodes
                    Text(
                        "1D Barcodes",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            BarcodeType.EAN_13, BarcodeType.EAN_8,
                            BarcodeType.UPC_A, BarcodeType.UPC_E,
                            BarcodeType.CODE_128, BarcodeType.CODE_39,
                            BarcodeType.CODE_93, BarcodeType.CODABAR,
                            BarcodeType.ITF
                        ).forEach { type ->
                            FilterChip(
                                selected = selectedType == type,
                                onClick = { 
                                    selectedType = type
                                    inputText = type.example
                                },
                                label = { Text(type.displayName, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    
                    // 2D Barcodes
                    Text(
                        "2D Barcodes",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            BarcodeType.PDF_417,
                            BarcodeType.DATA_MATRIX,
                            BarcodeType.AZTEC
                        ).forEach { type ->
                            FilterChip(
                                selected = selectedType == type,
                                onClick = { 
                                    selectedType = type
                                    inputText = type.example
                                },
                                label = { Text(type.displayName, style = MaterialTheme.typography.labelSmall) }
                            )
                        }
                    }
                }
            }

            // Type info
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
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
                    Column {
                        Text(
                            selectedType.description,
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            "Example: ${selectedType.example}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }

            // Input field
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        label = { Text("Barcode Content") },
                        placeholder = { Text(selectedType.example) },
                        modifier = Modifier.fillMaxWidth(),
                        isError = validationError != null,
                        supportingText = {
                            validationError?.let {
                                Text(it, color = MaterialTheme.colorScheme.error)
                            }
                        },
                        trailingIcon = {
                            Row {
                                if (inputText.isNotEmpty()) {
                                    IconButton(onClick = { inputText = "" }) {
                                        Icon(Icons.Filled.Clear, "Clear")
                                    }
                                }
                                IconButton(onClick = { inputText = selectedType.example }) {
                                    Icon(Icons.Filled.AutoAwesome, "Use Example")
                                }
                            }
                        }
                    )
                }
            }

            // Action button
            Button(
                onClick = { saveBarcode() },
                modifier = Modifier.fillMaxWidth(),
                enabled = barcodeBitmap != null
            ) {
                Icon(Icons.Filled.Save, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(4.dp))
                Text("Save Barcode")
            }

            // Size sliders (only for 1D barcodes)
            if (selectedType.format !in listOf(BarcodeFormat.PDF_417, BarcodeFormat.DATA_MATRIX, BarcodeFormat.AZTEC)) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Width", style = MaterialTheme.typography.labelLarge)
                            Text(
                                "${barcodeWidth.toInt()}px",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = barcodeWidth,
                            onValueChange = { barcodeWidth = it },
                            valueRange = 400f..1200f,
                            steps = 7,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Height", style = MaterialTheme.typography.labelLarge)
                            Text(
                                "${barcodeHeight.toInt()}px",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = barcodeHeight,
                            onValueChange = { barcodeHeight = it },
                            valueRange = 100f..500f,
                            steps = 7,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            } else {
                // Size for 2D barcodes
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Size", style = MaterialTheme.typography.labelLarge)
                            Text(
                                "${barcodeWidth.toInt()}px",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        Slider(
                            value = barcodeWidth,
                            onValueChange = { 
                                barcodeWidth = it
                                barcodeHeight = it
                            },
                            valueRange = 200f..800f,
                            steps = 11,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }

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

            // Save message
            saveMessage?.let {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = if (it.startsWith("Failed"))
                            MaterialTheme.colorScheme.errorContainer
                        else
                            MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (it.startsWith("Failed")) Icons.Filled.Error else Icons.Filled.CheckCircle,
                            null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(it, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // Barcode display
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (barcodeBitmap != null) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(androidx.compose.ui.graphics.Color.White)
                                .padding(16.dp)
                        ) {
                            Image(
                                bitmap = barcodeBitmap!!.asImageBitmap(),
                                contentDescription = "Generated Barcode",
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "${selectedType.displayName} Barcode",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(200.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Filled.QrCode,
                                    null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    if (validationError != null) "Fix validation error" else "Enter content above",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
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

private fun generateBarcodeBitmap(content: String, format: BarcodeFormat, width: Int, height: Int): Bitmap {
    val writer = MultiFormatWriter()
    val bitMatrix: BitMatrix = writer.encode(content, format, width, height)
    
    val w = bitMatrix.width
    val h = bitMatrix.height
    val pixels = IntArray(w * h)
    
    for (y in 0 until h) {
        val offset = y * w
        for (x in 0 until w) {
            pixels[offset + x] = if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE
        }
    }
    
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
    return bitmap
}

