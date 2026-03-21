package cv.toolkit.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cv.toolkit.ads.BannerAd
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

enum class ConversionMode {
    IMAGE_TO_BASE64,
    BASE64_TO_IMAGE
}

enum class Base64OutputFormat {
    PLAIN,
    DATA_URI
}

data class ImageInfo(
    val name: String,
    val size: Long,
    val mimeType: String?,
    val width: Int,
    val height: Int,
    val bitmap: Bitmap?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageBase64Screen(navController: NavController) {
    var conversionMode by remember { mutableStateOf(ConversionMode.IMAGE_TO_BASE64) }
    var outputFormat by remember { mutableStateOf(Base64OutputFormat.DATA_URI) }
    var selectedImage by remember { mutableStateOf<ImageInfo?>(null) }
    var base64Output by remember { mutableStateOf("") }
    var base64Input by remember { mutableStateOf("") }
    var decodedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isProcessing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val clipboardManager = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager }
    val scope = rememberCoroutineScope()

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            errorMessage = null
            base64Output = ""
            isProcessing = true

            scope.launch {
                try {
                    val result = withContext(Dispatchers.IO) {
                        processImageToBase64(context.contentResolver, it, outputFormat)
                    }
                    selectedImage = result.first
                    base64Output = result.second
                } catch (e: Exception) {
                    errorMessage = "Failed to process image: ${e.message}"
                } finally {
                    isProcessing = false
                }
            }
        }
    }

    fun decodeBase64ToImage() {
        if (base64Input.isBlank()) return

        errorMessage = null
        decodedBitmap = null
        isProcessing = true

        scope.launch {
            try {
                val bitmap = withContext(Dispatchers.IO) {
                    decodeBase64ToBitmap(base64Input)
                }
                decodedBitmap = bitmap
            } catch (e: Exception) {
                errorMessage = "Failed to decode Base64: ${e.message}"
            } finally {
                isProcessing = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Image Base64") },
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
            // Mode selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = conversionMode == ConversionMode.IMAGE_TO_BASE64,
                    onClick = {
                        conversionMode = ConversionMode.IMAGE_TO_BASE64
                        errorMessage = null
                    },
                    label = { Text("Image → Base64") },
                    leadingIcon = if (conversionMode == ConversionMode.IMAGE_TO_BASE64) {
                        { Icon(Icons.Filled.Check, null, modifier = Modifier.size(18.dp)) }
                    } else null,
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = conversionMode == ConversionMode.BASE64_TO_IMAGE,
                    onClick = {
                        conversionMode = ConversionMode.BASE64_TO_IMAGE
                        errorMessage = null
                    },
                    label = { Text("Base64 → Image") },
                    leadingIcon = if (conversionMode == ConversionMode.BASE64_TO_IMAGE) {
                        { Icon(Icons.Filled.Check, null, modifier = Modifier.size(18.dp)) }
                    } else null,
                    modifier = Modifier.weight(1f)
                )
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

            when (conversionMode) {
                ConversionMode.IMAGE_TO_BASE64 -> {
                    ImageToBase64Content(
                        selectedImage = selectedImage,
                        base64Output = base64Output,
                        outputFormat = outputFormat,
                        isProcessing = isProcessing,
                        onBase64OutputFormatChange = { newFormat ->
                            outputFormat = newFormat
                            // Re-encode if image is already selected
                            selectedImage?.let { img ->
                                img.bitmap?.let { bitmap ->
                                    scope.launch {
                                        isProcessing = true
                                        try {
                                            val newBase64 = withContext(Dispatchers.IO) {
                                                encodeBitmapToBase64(bitmap, img.mimeType, newFormat)
                                            }
                                            base64Output = newBase64
                                        } finally {
                                            isProcessing = false
                                        }
                                    }
                                }
                            }
                        },
                        onSelectImage = { imagePicker.launch(arrayOf("image/*")) },
                        onCopy = {
                            clipboardManager.setPrimaryClip(
                                ClipData.newPlainText("base64", base64Output)
                            )
                        },
                        onClear = {
                            selectedImage = null
                            base64Output = ""
                        }
                    )
                }
                ConversionMode.BASE64_TO_IMAGE -> {
                    Base64ToImageContent(
                        base64Input = base64Input,
                        decodedBitmap = decodedBitmap,
                        isProcessing = isProcessing,
                        onBase64InputChange = { base64Input = it },
                        onDecode = { decodeBase64ToImage() },
                        onPaste = {
                            try {
                                val clip = clipboardManager.primaryClip
                                if (clip != null && clip.itemCount > 0) {
                                    val text = clip.getItemAt(0).text?.toString() ?: ""
                                    base64Input = text
                                }
                            } catch (e: Exception) {
                                errorMessage = "Failed to paste: ${e.message}"
                            }
                        },
                        onClear = {
                            base64Input = ""
                            decodedBitmap = null
                        }
                    )
                }
            }

            BannerAd(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ImageToBase64Content(
    selectedImage: ImageInfo?,
    base64Output: String,
    outputFormat: Base64OutputFormat,
    isProcessing: Boolean,
    onBase64OutputFormatChange: (Base64OutputFormat) -> Unit,
    onSelectImage: () -> Unit,
    onCopy: () -> Unit,
    onClear: () -> Unit
) {
    // Output format selector
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Output:", style = MaterialTheme.typography.labelMedium)
        FilterChip(
            selected = outputFormat == Base64OutputFormat.DATA_URI,
            onClick = { onBase64OutputFormatChange(Base64OutputFormat.DATA_URI) },
            label = { Text("Data URI") }
        )
        FilterChip(
            selected = outputFormat == Base64OutputFormat.PLAIN,
            onClick = { onBase64OutputFormatChange(Base64OutputFormat.PLAIN) },
            label = { Text("Plain") }
        )
    }

    // Select image button
    Button(
        onClick = onSelectImage,
        modifier = Modifier.fillMaxWidth(),
        enabled = !isProcessing
    ) {
        Icon(Icons.Filled.Image, null, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text("Select Image")
    }

    // Processing indicator
    if (isProcessing) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
                Spacer(Modifier.height(12.dp))
                Text("Processing image...", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }

    // Image preview and info
    selectedImage?.let { image ->
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Image preview
                image.bitmap?.let { bitmap ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(MaterialTheme.shapes.medium)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Image(
                            bitmap = bitmap.asImageBitmap(),
                            contentDescription = "Selected image",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                }

                // Image info
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            image.name,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Text(
                            image.mimeType ?: "Unknown type",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            "${image.width} × ${image.height}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            formatFileSizeBase64(image.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }

    // Base64 output
    if (base64Output.isNotEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Base64 Output",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row {
                        IconButton(onClick = onCopy) {
                            Icon(Icons.Filled.ContentCopy, "Copy", modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = onClear) {
                            Icon(Icons.Filled.Clear, "Clear", modifier = Modifier.size(20.dp))
                        }
                    }
                }

                Text(
                    "${base64Output.length} characters",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(8.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Text(
                        base64Output.take(500) + if (base64Output.length > 500) "..." else "",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                            .horizontalScroll(rememberScrollState())
                    )
                }
            }
        }
    }

    // Empty state
    if (selectedImage == null && !isProcessing) {
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
                Icon(
                    Icons.Filled.Image,
                    null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "Select an Image",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Convert any image to Base64 string",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun Base64ToImageContent(
    base64Input: String,
    decodedBitmap: Bitmap?,
    isProcessing: Boolean,
    onBase64InputChange: (String) -> Unit,
    onDecode: () -> Unit,
    onPaste: () -> Unit,
    onClear: () -> Unit
) {
    // Show summary card for large inputs (pasted via button)
    if (base64Input.length > 5000) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Base64 Data Loaded",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        "${base64Input.length} characters",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(Modifier.height(8.dp))
                Text(
                    base64Input.take(100) + "...",
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 2,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Press Clear to enter new data manually",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        // Normal editable input field
        OutlinedTextField(
            value = base64Input,
            onValueChange = onBase64InputChange,
            label = { Text("Base64 String") },
            placeholder = { Text("Type or paste Base64 here...") },
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp),
            textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace),
            supportingText = if (base64Input.isNotEmpty()) {
                { Text("${base64Input.length} characters") }
            } else null
        )
    }

    // Action buttons
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(
            onClick = onPaste,
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Filled.ContentPaste, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("Paste")
        }
        OutlinedButton(
            onClick = onClear,
            modifier = Modifier.weight(1f),
            enabled = base64Input.isNotEmpty()
        ) {
            Icon(Icons.Filled.Clear, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(4.dp))
            Text("Clear")
        }
        Button(
            onClick = onDecode,
            modifier = Modifier.weight(1f),
            enabled = base64Input.isNotBlank() && !isProcessing
        ) {
            if (isProcessing) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Icon(Icons.Filled.Transform, null, modifier = Modifier.size(18.dp))
            }
            Spacer(Modifier.width(4.dp))
            Text("Decode")
        }
    }

    // Decoded image preview
    decodedBitmap?.let { bitmap ->
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "Decoded Image",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(Modifier.height(12.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(MaterialTheme.shapes.medium)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Decoded image",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${bitmap.width}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "Width",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${bitmap.height}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "Height",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "${bitmap.config}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            "Config",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }

    // Empty state
    if (decodedBitmap == null && base64Input.isEmpty() && !isProcessing) {
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
                Icon(
                    Icons.Filled.Transform,
                    null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "Paste Base64 String",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Decode Base64 string back to image\nSupports data URI and plain Base64",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    // Info card
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
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
                "Supports both plain Base64 and data URI format (data:image/...;base64,...)",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

private fun processImageToBase64(
    contentResolver: ContentResolver,
    uri: Uri,
    outputFormat: Base64OutputFormat
): Pair<ImageInfo, String> {
    var name = "Unknown"
    var size = 0L

    contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)

            if (nameIndex >= 0) {
                name = cursor.getString(nameIndex) ?: "Unknown"
            }
            if (sizeIndex >= 0) {
                size = cursor.getLong(sizeIndex)
            }
        }
    }

    val mimeType = contentResolver.getType(uri)

    // Read image bytes and decode bitmap
    val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: ByteArray(0)
    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)

    val imageInfo = ImageInfo(
        name = name,
        size = size,
        mimeType = mimeType,
        width = bitmap?.width ?: 0,
        height = bitmap?.height ?: 0,
        bitmap = bitmap
    )

    val base64String = Base64.encodeToString(bytes, Base64.NO_WRAP)
    val output = when (outputFormat) {
        Base64OutputFormat.PLAIN -> base64String
        Base64OutputFormat.DATA_URI -> "data:${mimeType ?: "image/png"};base64,$base64String"
    }

    return Pair(imageInfo, output)
}

private fun encodeBitmapToBase64(bitmap: Bitmap, mimeType: String?, outputFormat: Base64OutputFormat): String {
    val outputStream = ByteArrayOutputStream()
    val format = when {
        mimeType?.contains("png") == true -> Bitmap.CompressFormat.PNG
        mimeType?.contains("webp") == true -> Bitmap.CompressFormat.WEBP
        else -> Bitmap.CompressFormat.JPEG
    }
    bitmap.compress(format, 100, outputStream)
    val bytes = outputStream.toByteArray()
    val base64String = Base64.encodeToString(bytes, Base64.NO_WRAP)

    return when (outputFormat) {
        Base64OutputFormat.PLAIN -> base64String
        Base64OutputFormat.DATA_URI -> "data:${mimeType ?: "image/png"};base64,$base64String"
    }
}

private fun decodeBase64ToBitmap(input: String): Bitmap {
    // Remove data URI prefix if present
    val base64String = if (input.contains(",")) {
        input.substringAfter(",")
    } else {
        input
    }.trim()

    val bytes = Base64.decode(base64String, Base64.DEFAULT)
    return BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        ?: throw IllegalArgumentException("Invalid image data")
}

private fun formatFileSizeBase64(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
        else -> String.format("%.1f MB", bytes / (1024.0 * 1024))
    }
}
