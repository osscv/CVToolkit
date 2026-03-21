package cv.toolkit.screens

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import java.net.URLEncoder
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Subject
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cv.toolkit.ads.BannerAd
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel
import android.content.ContentValues
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File
import java.io.FileOutputStream

enum class QrCodeType(val displayName: String, val icon: @Composable () -> Unit) {
    TEXT("Text", { Icon(Icons.Filled.TextFields, null, modifier = Modifier.size(18.dp)) }),
    URL("URL/Link", { Icon(Icons.Filled.Link, null, modifier = Modifier.size(18.dp)) }),
    WIFI("WiFi", { Icon(Icons.Filled.Wifi, null, modifier = Modifier.size(18.dp)) }),
    CONTACT("Contact (vCard)", { Icon(Icons.Filled.Person, null, modifier = Modifier.size(18.dp)) }),
    EMAIL("Email", { Icon(Icons.Filled.Email, null, modifier = Modifier.size(18.dp)) }),
    PHONE("Phone", { Icon(Icons.Filled.Phone, null, modifier = Modifier.size(18.dp)) }),
    SMS("SMS", { Icon(Icons.Filled.Sms, null, modifier = Modifier.size(18.dp)) }),
    LOCATION("Location", { Icon(Icons.Filled.LocationOn, null, modifier = Modifier.size(18.dp)) })
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QrGeneratorScreen(navController: NavController) {
    var selectedType by remember { mutableStateOf(QrCodeType.TEXT) }
    var inputText by remember { mutableStateOf("") }
    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var qrSize by remember { mutableFloatStateOf(512f) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var saveMessage by remember { mutableStateOf<String?>(null) }
    val clipboard = LocalClipboard.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // WiFi fields
    var wifiSsid by remember { mutableStateOf("") }
    var wifiPassword by remember { mutableStateOf("") }
    var wifiEncryption by remember { mutableStateOf("WPA") }
    var wifiHidden by remember { mutableStateOf(false) }
    var showWifiPassword by remember { mutableStateOf(false) }

    // Contact fields
    var contactName by remember { mutableStateOf("") }
    var contactPhone by remember { mutableStateOf("") }
    var contactEmail by remember { mutableStateOf("") }
    var contactOrg by remember { mutableStateOf("") }

    // Email fields
    var emailAddress by remember { mutableStateOf("") }
    var emailSubject by remember { mutableStateOf("") }
    var emailBody by remember { mutableStateOf("") }

    // Phone field
    var phoneNumber by remember { mutableStateOf("") }

    // SMS fields
    var smsNumber by remember { mutableStateOf("") }
    var smsMessage by remember { mutableStateOf("") }

    // Location fields
    var latitude by remember { mutableStateOf("") }
    var longitude by remember { mutableStateOf("") }

    // Generate content based on type
    fun getQrContent(): String {
        return when (selectedType) {
            QrCodeType.TEXT -> inputText
            QrCodeType.URL -> {
                val url = inputText.trim()
                if (url.isNotEmpty() && !url.startsWith("http://") && !url.startsWith("https://")) {
                    "https://$url"
                } else url
            }
            QrCodeType.WIFI -> {
                if (wifiSsid.isBlank()) ""
                else "WIFI:T:$wifiEncryption;S:${wifiSsid.escapeWifi()};P:${wifiPassword.escapeWifi()};H:$wifiHidden;;"
            }
            QrCodeType.CONTACT -> {
                if (contactName.isBlank()) ""
                else buildString {
                    append("BEGIN:VCARD\n")
                    append("VERSION:3.0\n")
                    append("N:$contactName\n")
                    append("FN:$contactName\n")
                    if (contactPhone.isNotBlank()) append("TEL:$contactPhone\n")
                    if (contactEmail.isNotBlank()) append("EMAIL:$contactEmail\n")
                    if (contactOrg.isNotBlank()) append("ORG:$contactOrg\n")
                    append("END:VCARD")
                }
            }
            QrCodeType.EMAIL -> {
                if (emailAddress.isBlank()) ""
                else buildString {
                    append("mailto:$emailAddress")
                    val params = mutableListOf<String>()
                    if (emailSubject.isNotBlank()) params.add("subject=${emailSubject.encodeUrl()}")
                    if (emailBody.isNotBlank()) params.add("body=${emailBody.encodeUrl()}")
                    if (params.isNotEmpty()) append("?${params.joinToString("&")}")
                }
            }
            QrCodeType.PHONE -> if (phoneNumber.isBlank()) "" else "tel:$phoneNumber"
            QrCodeType.SMS -> {
                if (smsNumber.isBlank()) ""
                else if (smsMessage.isBlank()) "sms:$smsNumber"
                else "sms:$smsNumber?body=${smsMessage.encodeUrl()}"
            }
            QrCodeType.LOCATION -> {
                val lat = latitude.toDoubleOrNull()
                val lng = longitude.toDoubleOrNull()
                if (lat != null && lng != null) "geo:$lat,$lng" else ""
            }
        }
    }

    fun generateQrCode() {
        val content = getQrContent()
        if (content.isBlank()) {
            qrBitmap = null
            return
        }

        errorMessage = null
        scope.launch {
            try {
                val bitmap = withContext(Dispatchers.Default) {
                    generateQrBitmap(content, qrSize.toInt())
                }
                qrBitmap = bitmap
            } catch (e: Exception) {
                errorMessage = "Failed to generate QR code: ${e.message}"
                qrBitmap = null
            }
        }
    }

    fun saveQrCode() {
        val bitmap = qrBitmap ?: return
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    val filename = "QR_${System.currentTimeMillis()}.png"

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
                saveMessage = "QR code saved to Pictures/CVToolkit"
            } catch (e: Exception) {
                saveMessage = "Failed to save: ${e.message}"
            }
        }
    }

    // Auto-generate when input changes
    LaunchedEffect(
        selectedType, inputText, qrSize,
        wifiSsid, wifiPassword, wifiEncryption, wifiHidden,
        contactName, contactPhone, contactEmail, contactOrg,
        emailAddress, emailSubject, emailBody,
        phoneNumber, smsNumber, smsMessage,
        latitude, longitude
    ) {
        generateQrCode()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("QR Generator") },
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
            // QR Type selector
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "QR Code Type",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QrCodeType.entries.forEach { type ->
                            FilterChip(
                                selected = selectedType == type,
                                onClick = { selectedType = type },
                                label = { Text(type.displayName) },
                                leadingIcon = { type.icon() }
                            )
                        }
                    }
                }
            }

            // Type-specific input fields
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (selectedType) {
                        QrCodeType.TEXT -> {
                            OutlinedTextField(
                                value = inputText,
                                onValueChange = { inputText = it },
                                label = { Text("Text") },
                                placeholder = { Text("Enter any text...") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 3,
                                maxLines = 5,
                                trailingIcon = {
                                    if (inputText.isNotEmpty()) {
                                        IconButton(onClick = { inputText = "" }) {
                                            Icon(Icons.Filled.Clear, "Clear")
                                        }
                                    }
                                }
                            )
                        }
                        QrCodeType.URL -> {
                            OutlinedTextField(
                                value = inputText,
                                onValueChange = { inputText = it },
                                label = { Text("URL") },
                                placeholder = { Text("example.com or https://...") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Filled.Link, null) },
                                trailingIcon = {
                                    if (inputText.isNotEmpty()) {
                                        IconButton(onClick = { inputText = "" }) {
                                            Icon(Icons.Filled.Clear, "Clear")
                                        }
                                    }
                                }
                            )
                        }
                        QrCodeType.WIFI -> {
                            OutlinedTextField(
                                value = wifiSsid,
                                onValueChange = { wifiSsid = it },
                                label = { Text("Network Name (SSID)") },
                                placeholder = { Text("WiFi network name") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Filled.Wifi, null) }
                            )
                            OutlinedTextField(
                                value = wifiPassword,
                                onValueChange = { wifiPassword = it },
                                label = { Text("Password") },
                                placeholder = { Text("WiFi password") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                visualTransformation = if (showWifiPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                leadingIcon = { Icon(Icons.Filled.Lock, null) },
                                trailingIcon = {
                                    IconButton(onClick = { showWifiPassword = !showWifiPassword }) {
                                        Icon(
                                            if (showWifiPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                            if (showWifiPassword) "Hide password" else "Show password"
                                        )
                                    }
                                }
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf("WPA", "WEP", "nopass").forEach { enc ->
                                    FilterChip(
                                        selected = wifiEncryption == enc,
                                        onClick = { wifiEncryption = enc },
                                        label = { Text(if (enc == "nopass") "Open" else enc) }
                                    )
                                }
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = wifiHidden,
                                    onCheckedChange = { wifiHidden = it }
                                )
                                Text("Hidden network")
                            }
                        }
                        QrCodeType.CONTACT -> {
                            OutlinedTextField(
                                value = contactName,
                                onValueChange = { contactName = it },
                                label = { Text("Name *") },
                                placeholder = { Text("Full name") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Filled.Person, null) }
                            )
                            OutlinedTextField(
                                value = contactPhone,
                                onValueChange = { contactPhone = it },
                                label = { Text("Phone") },
                                placeholder = { Text("+1234567890") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Filled.Phone, null) }
                            )
                            OutlinedTextField(
                                value = contactEmail,
                                onValueChange = { contactEmail = it },
                                label = { Text("Email") },
                                placeholder = { Text("email@example.com") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Filled.Email, null) }
                            )
                            OutlinedTextField(
                                value = contactOrg,
                                onValueChange = { contactOrg = it },
                                label = { Text("Organization") },
                                placeholder = { Text("Company name") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Filled.Business, null) }
                            )
                        }
                        QrCodeType.EMAIL -> {
                            OutlinedTextField(
                                value = emailAddress,
                                onValueChange = { emailAddress = it },
                                label = { Text("Email Address *") },
                                placeholder = { Text("recipient@example.com") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Filled.Email, null) }
                            )
                            OutlinedTextField(
                                value = emailSubject,
                                onValueChange = { emailSubject = it },
                                label = { Text("Subject") },
                                placeholder = { Text("Email subject") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                leadingIcon = { Icon(Icons.AutoMirrored.Filled.Subject, null) }
                            )
                            OutlinedTextField(
                                value = emailBody,
                                onValueChange = { emailBody = it },
                                label = { Text("Message") },
                                placeholder = { Text("Email body...") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2,
                                maxLines = 4
                            )
                        }
                        QrCodeType.PHONE -> {
                            OutlinedTextField(
                                value = phoneNumber,
                                onValueChange = { phoneNumber = it },
                                label = { Text("Phone Number") },
                                placeholder = { Text("+1234567890") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Filled.Phone, null) }
                            )
                        }
                        QrCodeType.SMS -> {
                            OutlinedTextField(
                                value = smsNumber,
                                onValueChange = { smsNumber = it },
                                label = { Text("Phone Number *") },
                                placeholder = { Text("+1234567890") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Filled.Phone, null) }
                            )
                            OutlinedTextField(
                                value = smsMessage,
                                onValueChange = { smsMessage = it },
                                label = { Text("Message") },
                                placeholder = { Text("Pre-filled message...") },
                                modifier = Modifier.fillMaxWidth(),
                                minLines = 2,
                                maxLines = 4,
                                leadingIcon = { Icon(Icons.Filled.Sms, null) }
                            )
                        }
                        QrCodeType.LOCATION -> {
                            OutlinedTextField(
                                value = latitude,
                                onValueChange = { latitude = it },
                                label = { Text("Latitude *") },
                                placeholder = { Text("e.g., 37.7749") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Filled.MyLocation, null) }
                            )
                            OutlinedTextField(
                                value = longitude,
                                onValueChange = { longitude = it },
                                label = { Text("Longitude *") },
                                placeholder = { Text("e.g., -122.4194") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                leadingIcon = { Icon(Icons.Filled.LocationOn, null) }
                            )
                        }
                    }
                }
            }

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { saveQrCode() },
                    modifier = Modifier.weight(1f),
                    enabled = qrBitmap != null
                ) {
                    Icon(Icons.Filled.Save, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Save QR Code")
                }
            }

            // Size slider
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("QR Code Size", style = MaterialTheme.typography.labelLarge)
                        Text(
                            "${qrSize.toInt()}px",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = qrSize,
                        onValueChange = { qrSize = it },
                        valueRange = 256f..1024f,
                        steps = 6,
                        modifier = Modifier.fillMaxWidth()
                    )
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

            // QR Code display
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
                    if (qrBitmap != null) {
                        Box(
                            modifier = Modifier
                                .size(280.dp)
                                .background(androidx.compose.ui.graphics.Color.White)
                                .padding(16.dp)
                        ) {
                            Image(
                                bitmap = qrBitmap!!.asImageBitmap(),
                                contentDescription = "QR Code",
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "${selectedType.displayName} QR Code",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(280.dp)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    Icons.Filled.QrCode2,
                                    null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Fill in the fields above",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Info card
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
                    Text(
                        when (selectedType) {
                            QrCodeType.TEXT -> "Plain text QR codes can store any text content."
                            QrCodeType.URL -> "URL QR codes open websites when scanned."
                            QrCodeType.WIFI -> "WiFi QR codes allow instant network connection when scanned."
                            QrCodeType.CONTACT -> "vCard QR codes add contacts directly to phone."
                            QrCodeType.EMAIL -> "Email QR codes open email composer with pre-filled fields."
                            QrCodeType.PHONE -> "Phone QR codes initiate a call when scanned."
                            QrCodeType.SMS -> "SMS QR codes open messaging app with pre-filled content."
                            QrCodeType.LOCATION -> "Location QR codes open maps at the specified coordinates."
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            BannerAd(modifier = Modifier.fillMaxWidth())
        }
    }
}

private fun generateQrBitmap(content: String, size: Int): Bitmap {
    val hints = mapOf<EncodeHintType, Any>(
        EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.H,
        EncodeHintType.MARGIN to 1,
        EncodeHintType.CHARACTER_SET to "UTF-8"
    )

    val writer = QRCodeWriter()
    val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, size, size, hints)

    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    for (x in 0 until size) {
        for (y in 0 until size) {
            bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
        }
    }

    return bitmap
}

// Helper extension functions for QR content formatting
private fun String.escapeWifi(): String {
    return this.replace("\\", "\\\\")
        .replace(";", "\\;")
        .replace(",", "\\,")
        .replace(":", "\\:")
        .replace("\"", "\\\"")
}

private fun String.encodeUrl(): String {
    return URLEncoder.encode(this, "UTF-8")
}
