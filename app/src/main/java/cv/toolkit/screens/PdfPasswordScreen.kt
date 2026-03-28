package cv.toolkit.screens

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cv.toolkit.ads.BannerAd
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.encryption.AccessPermission
import com.tom_roush.pdfbox.pdmodel.encryption.StandardProtectionPolicy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

private enum class PasswordMode(val label: String) {
    PROTECT("Add Password"),
    REMOVE("Remove Password")
}

private data class PdfPasswordInfo(
    val uri: Uri,
    val name: String,
    val pageCount: Int,
    val size: Long,
    val isEncrypted: Boolean,
    val thumbnail: Bitmap?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfPasswordScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Init PDFBox
    LaunchedEffect(Unit) {
        PDFBoxResourceLoader.init(context)
    }

    var pdfInfo by remember { mutableStateOf<PdfPasswordInfo?>(null) }
    var mode by remember { mutableStateOf(PasswordMode.PROTECT) }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var existingPassword by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var successMessage by remember { mutableStateOf<String?>(null) }
    var pendingBytes by remember { mutableStateOf<ByteArray?>(null) }

    // Permissions for protect mode
    var allowPrinting by remember { mutableStateOf(true) }
    var allowCopying by remember { mutableStateOf(false) }
    var allowModifying by remember { mutableStateOf(false) }

    val pdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                isProcessing = true
                errorMessage = null
                successMessage = null
                pendingBytes = null
                try {
                    val info = withContext(Dispatchers.IO) {
                        getPdfPasswordInfo(context, it)
                    }
                    pdfInfo = info
                    mode = if (info.isEncrypted) PasswordMode.REMOVE else PasswordMode.PROTECT
                    password = ""
                    confirmPassword = ""
                    existingPassword = ""
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Failed to open PDF: ${e.message}")
                } finally {
                    isProcessing = false
                }
            }
        }
    }

    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        uri?.let { destUri ->
            val bytes = pendingBytes
            if (bytes != null) {
                scope.launch {
                    try {
                        withContext(Dispatchers.IO) {
                            context.contentResolver.openOutputStream(destUri)?.use { os ->
                                os.write(bytes)
                            }
                        }
                        snackbarHostState.showSnackbar("PDF saved successfully")
                    } catch (e: Exception) {
                        snackbarHostState.showSnackbar("Failed to save: ${e.message}")
                    }
                }
            }
        }
    }

    fun processProtect() {
        val info = pdfInfo ?: return
        if (password.isEmpty()) {
            errorMessage = "Please enter a password"
            return
        }
        if (password != confirmPassword) {
            errorMessage = "Passwords do not match"
            return
        }

        scope.launch {
            isProcessing = true
            errorMessage = null
            successMessage = null
            try {
                val result = withContext(Dispatchers.IO) {
                    addPasswordToPdf(
                        context, info.uri, password,
                        allowPrinting, allowCopying, allowModifying
                    )
                }
                pendingBytes = result
                successMessage = "Password protection added"
                val pdfName = info.name.removeSuffix(".pdf")
                saveLauncher.launch("${pdfName}_protected.pdf")
            } catch (e: Exception) {
                errorMessage = "Failed to protect PDF: ${e.message}"
            } finally {
                isProcessing = false
            }
        }
    }

    fun processRemove() {
        val info = pdfInfo ?: return
        if (existingPassword.isEmpty()) {
            errorMessage = "Please enter the current password"
            return
        }

        scope.launch {
            isProcessing = true
            errorMessage = null
            successMessage = null
            try {
                val result = withContext(Dispatchers.IO) {
                    removePasswordFromPdf(context, info.uri, existingPassword)
                }
                pendingBytes = result
                successMessage = "Password removed successfully"
                val pdfName = info.name.removeSuffix(".pdf")
                saveLauncher.launch("${pdfName}_unlocked.pdf")
            } catch (e: Exception) {
                errorMessage = when {
                    e.message?.contains("password", ignoreCase = true) == true -> "Incorrect password"
                    else -> "Failed to remove password: ${e.message}"
                }
            } finally {
                isProcessing = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("PDF Password") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Select PDF button
                Button(
                    onClick = { pdfPicker.launch("application/pdf") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isProcessing
                ) {
                    Icon(Icons.Filled.UploadFile, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Select PDF File")
                }

                // Empty state
                if (pdfInfo == null && !isProcessing) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                        )
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Filled.Lock,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "PDF Password Protection",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                "Add or remove password protection from PDF files",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }

                pdfInfo?.let { info ->
                    // File info card
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.PictureAsPdf,
                                contentDescription = null,
                                modifier = Modifier.size(40.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    info.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    "${info.pageCount} page${if (info.pageCount != 1) "s" else ""} \u2022 ${formatPdfPwdSize(info.size)}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            // Encryption status badge
                            Surface(
                                color = if (info.isEncrypted)
                                    MaterialTheme.colorScheme.primaryContainer
                                else
                                    MaterialTheme.colorScheme.surfaceVariant,
                                shape = MaterialTheme.shapes.small
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        if (info.isEncrypted) Icons.Filled.Lock else Icons.Filled.LockOpen,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        if (info.isEncrypted) "Protected" else "Unprotected",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    }

                    // Mode selector
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Action",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                PasswordMode.entries.forEach { m ->
                                    FilterChip(
                                        selected = mode == m,
                                        onClick = {
                                            mode = m
                                            errorMessage = null
                                            successMessage = null
                                        },
                                        label = { Text(m.label) },
                                        leadingIcon = {
                                            Icon(
                                                if (m == PasswordMode.PROTECT) Icons.Filled.Lock else Icons.Filled.LockOpen,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        },
                                        modifier = Modifier.weight(1f),
                                        enabled = !isProcessing
                                    )
                                }
                            }
                        }
                    }

                    // Password input
                    if (mode == PasswordMode.PROTECT) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Set Password",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedTextField(
                                    value = password,
                                    onValueChange = { password = it },
                                    label = { Text("Password") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                    trailingIcon = {
                                        IconButton(onClick = { showPassword = !showPassword }) {
                                            Icon(
                                                if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                                contentDescription = "Toggle password visibility"
                                            )
                                        }
                                    },
                                    enabled = !isProcessing
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedTextField(
                                    value = confirmPassword,
                                    onValueChange = { confirmPassword = it },
                                    label = { Text("Confirm Password") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                    isError = confirmPassword.isNotEmpty() && password != confirmPassword,
                                    supportingText = {
                                        if (confirmPassword.isNotEmpty() && password != confirmPassword) {
                                            Text("Passwords do not match")
                                        }
                                    },
                                    enabled = !isProcessing
                                )
                            }
                        }

                        // Permissions
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Permissions",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Allow Printing", style = MaterialTheme.typography.bodyMedium)
                                    Switch(
                                        checked = allowPrinting,
                                        onCheckedChange = { allowPrinting = it },
                                        enabled = !isProcessing
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Allow Copying", style = MaterialTheme.typography.bodyMedium)
                                    Switch(
                                        checked = allowCopying,
                                        onCheckedChange = { allowCopying = it },
                                        enabled = !isProcessing
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Allow Modifying", style = MaterialTheme.typography.bodyMedium)
                                    Switch(
                                        checked = allowModifying,
                                        onCheckedChange = { allowModifying = it },
                                        enabled = !isProcessing
                                    )
                                }
                            }
                        }
                    }

                    if (mode == PasswordMode.REMOVE) {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Enter Current Password",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                OutlinedTextField(
                                    value = existingPassword,
                                    onValueChange = { existingPassword = it },
                                    label = { Text("Current Password") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                                    trailingIcon = {
                                        IconButton(onClick = { showPassword = !showPassword }) {
                                            Icon(
                                                if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                                contentDescription = "Toggle password visibility"
                                            )
                                        }
                                    },
                                    enabled = !isProcessing
                                )
                            }
                        }
                    }

                    // Error message
                    if (errorMessage != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.Error,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    errorMessage!!,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    // Success message
                    if (successMessage != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    successMessage!!,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }

                    // Processing indicator
                    if (isProcessing) {
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    // Action button
                    Button(
                        onClick = {
                            if (mode == PasswordMode.PROTECT) processProtect() else processRemove()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isProcessing
                    ) {
                        Icon(
                            if (mode == PasswordMode.PROTECT) Icons.Filled.Lock else Icons.Filled.LockOpen,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (mode == PasswordMode.PROTECT) "Protect PDF" else "Remove Password")
                    }
                }
            }
            BannerAd(modifier = Modifier.fillMaxWidth())
        }
    }
}

private fun getPdfPasswordInfo(context: Context, uri: Uri): PdfPasswordInfo {
    var name = "Unknown.pdf"
    var size = 0L
    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        if (cursor.moveToFirst()) {
            val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (nameIndex >= 0) name = cursor.getString(nameIndex) ?: "Unknown.pdf"
            if (sizeIndex >= 0) size = cursor.getLong(sizeIndex)
        }
    }

    // Check encryption with PDFBox
    var isEncrypted = false
    var pageCount = 0
    try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val doc = PDDocument.load(inputStream)
            isEncrypted = doc.isEncrypted
            pageCount = doc.numberOfPages
            doc.close()
        }
    } catch (_: Exception) {
        // If we can't open it, try without password - it might be encrypted
        isEncrypted = true
    }

    // Get thumbnail via PdfRenderer (works for unencrypted PDFs)
    var thumbnail: Bitmap? = null
    if (!isEncrypted) {
        try {
            val pfd = context.contentResolver.openFileDescriptor(uri, "r")
            if (pfd != null) {
                val renderer = PdfRenderer(pfd)
                if (renderer.pageCount > 0) {
                    val page = renderer.openPage(0)
                    val thumbWidth = 120
                    val thumbHeight = (thumbWidth * page.height.toFloat() / page.width.toFloat()).toInt()
                    thumbnail = Bitmap.createBitmap(thumbWidth, thumbHeight, Bitmap.Config.ARGB_8888)
                    thumbnail.eraseColor(android.graphics.Color.WHITE)
                    page.render(thumbnail, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()
                    if (pageCount == 0) pageCount = renderer.pageCount
                }
                renderer.close()
                pfd.close()
            }
        } catch (_: Exception) { }
    }

    return PdfPasswordInfo(uri, name, pageCount, size, isEncrypted, thumbnail)
}

private fun addPasswordToPdf(
    context: Context,
    uri: Uri,
    password: String,
    allowPrinting: Boolean,
    allowCopying: Boolean,
    allowModifying: Boolean
): ByteArray {
    val inputStream = context.contentResolver.openInputStream(uri)
        ?: throw Exception("Cannot open file")

    val doc = inputStream.use { PDDocument.load(it) }

    try {
        val ap = AccessPermission()
        ap.setCanPrint(allowPrinting)
        ap.setCanExtractContent(allowCopying)
        ap.setCanModify(allowModifying)
        ap.setCanModifyAnnotations(allowModifying)
        ap.setCanFillInForm(allowModifying)
        ap.setCanAssembleDocument(allowModifying)
        ap.setCanPrintFaithful(allowPrinting)

        val policy = StandardProtectionPolicy(password, password, ap)
        policy.encryptionKeyLength = 128

        doc.protect(policy)

        val baos = ByteArrayOutputStream()
        doc.save(baos)
        return baos.toByteArray()
    } finally {
        doc.close()
    }
}

private fun removePasswordFromPdf(
    context: Context,
    uri: Uri,
    password: String
): ByteArray {
    val inputStream = context.contentResolver.openInputStream(uri)
        ?: throw Exception("Cannot open file")

    val doc = inputStream.use { PDDocument.load(it, password) }

    try {
        doc.isAllSecurityToBeRemoved = true

        val baos = ByteArrayOutputStream()
        doc.save(baos)
        return baos.toByteArray()
    } finally {
        doc.close()
    }
}

private fun formatPdfPwdSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
        bytes < 1024L * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024))
        else -> String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024))
    }
}
