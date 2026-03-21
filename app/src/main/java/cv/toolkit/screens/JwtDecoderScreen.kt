package cv.toolkit.screens

import android.util.Base64
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
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cv.toolkit.ads.BannerAd
import kotlinx.coroutines.launch
import org.json.JSONObject

data class JwtParts(
    val header: String,
    val payload: String,
    val signature: String,
    val headerJson: String?,
    val payloadJson: String?,
    val isValid: Boolean,
    val error: String?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JwtDecoderScreen(navController: NavController) {
    var inputText by remember { mutableStateOf("") }
    var jwtParts by remember { mutableStateOf<JwtParts?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    fun decodeJwt() {
        errorMessage = null
        jwtParts = null

        if (inputText.isBlank()) return

        try {
            val token = inputText.trim()
            val parts = token.split(".")

            if (parts.size != 3) {
                errorMessage = "Invalid JWT format. Expected 3 parts separated by dots."
                return
            }

            val (headerB64, payloadB64, signature) = parts

            val headerJson = try {
                val decoded = Base64.decode(headerB64, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
                val json = String(decoded, Charsets.UTF_8)
                JSONObject(json).toString(2)
            } catch (e: Exception) {
                null
            }

            val payloadJson = try {
                val decoded = Base64.decode(payloadB64, Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP)
                val json = String(decoded, Charsets.UTF_8)
                JSONObject(json).toString(2)
            } catch (e: Exception) {
                null
            }

            jwtParts = JwtParts(
                header = headerB64,
                payload = payloadB64,
                signature = signature,
                headerJson = headerJson,
                payloadJson = payloadJson,
                isValid = headerJson != null && payloadJson != null,
                error = if (headerJson == null || payloadJson == null) "Could not decode JWT parts" else null
            )

        } catch (e: Exception) {
            errorMessage = "Failed to decode JWT: ${e.message}"
        }
    }

    LaunchedEffect(inputText) {
        decodeJwt()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("JWT Decoder") },
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
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Input field
            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                label = { Text("JWT Token") },
                placeholder = { Text("Paste your JWT token here...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 100.dp, max = 150.dp),
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

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            clipboard.getClipEntry()?.clipData?.getItemAt(0)?.text?.let {
                                inputText = it.toString()
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.ContentPaste, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Paste")
                }
                Button(
                    onClick = { inputText = "" },
                    modifier = Modifier.weight(1f),
                    enabled = inputText.isNotEmpty()
                ) {
                    Icon(Icons.Filled.DeleteOutline, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Clear")
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

            // Decoded output
            jwtParts?.let { parts ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Header section
                    JwtSection(
                        title = "Header",
                        rawValue = parts.header,
                        decodedJson = parts.headerJson,
                        color = MaterialTheme.colorScheme.primary,
                        clipboard = clipboard,
                        scope = scope
                    )

                    // Payload section
                    JwtSection(
                        title = "Payload",
                        rawValue = parts.payload,
                        decodedJson = parts.payloadJson,
                        color = MaterialTheme.colorScheme.secondary,
                        clipboard = clipboard,
                        scope = scope
                    )

                    // Signature section
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.tertiary,
                                        shape = MaterialTheme.shapes.small
                                    ) {
                                        Text(
                                            "Signature",
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onTertiary
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            clipboard.setClipEntry(
                                                androidx.compose.ui.platform.ClipEntry(
                                                    android.content.ClipData.newPlainText("signature", parts.signature)
                                                )
                                            )
                                        }
                                    }
                                ) {
                                    Icon(Icons.Filled.ContentCopy, "Copy", modifier = Modifier.size(20.dp))
                                }
                            }
                            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                            Text(
                                parts.signature,
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.horizontalScroll(rememberScrollState())
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Note: Signature verification requires the secret key",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // JWT Info card
                    parts.payloadJson?.let { payload ->
                        JwtInfoCard(payload)
                    }
                }
            }

            // Empty state
            if (jwtParts == null && errorMessage == null && inputText.isEmpty()) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            Icons.Filled.Token,
                            null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Paste a JWT Token",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "The token will be decoded automatically",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            BannerAd(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun JwtSection(
    title: String,
    rawValue: String,
    decodedJson: String?,
    color: androidx.compose.ui.graphics.Color,
    clipboard: androidx.compose.ui.platform.Clipboard,
    scope: kotlinx.coroutines.CoroutineScope
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = color,
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        title,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.surface
                    )
                }
                IconButton(
                    onClick = {
                        scope.launch {
                            clipboard.setClipEntry(
                                androidx.compose.ui.platform.ClipEntry(
                                    android.content.ClipData.newPlainText(title, decodedJson ?: rawValue)
                                )
                            )
                        }
                    }
                ) {
                    Icon(Icons.Filled.ContentCopy, "Copy", modifier = Modifier.size(20.dp))
                }
            }
            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
            if (decodedJson != null) {
                Text(
                    decodedJson,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall
                )
            } else {
                Text(
                    "Could not decode $title",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun JwtInfoCard(payloadJson: String) {
    val claims = remember(payloadJson) {
        parseJwtClaims(payloadJson)
    }

    if (claims.isNotEmpty()) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Filled.Info,
                        null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Token Information",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(Modifier.height(8.dp))
                claims.forEach { (label, value) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            label,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            value,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = if (value.contains("EXPIRED")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

private fun parseJwtClaims(payloadJson: String): List<Pair<String, String>> {
    return try {
        val json = JSONObject(payloadJson)
        val claims = mutableListOf<Pair<String, String>>()

        if (json.has("iss")) claims.add("Issuer" to json.getString("iss"))
        if (json.has("sub")) claims.add("Subject" to json.getString("sub"))
        if (json.has("aud")) claims.add("Audience" to json.optString("aud", json.optJSONArray("aud")?.toString() ?: ""))
        if (json.has("exp")) {
            val exp = json.getLong("exp")
            val expDate = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                .format(java.util.Date(exp * 1000))
            val isExpired = System.currentTimeMillis() > exp * 1000
            claims.add("Expires" to "$expDate${if (isExpired) " (EXPIRED)" else ""}")
        }
        if (json.has("iat")) {
            val iat = json.getLong("iat")
            val iatDate = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                .format(java.util.Date(iat * 1000))
            claims.add("Issued At" to iatDate)
        }
        if (json.has("nbf")) {
            val nbf = json.getLong("nbf")
            val nbfDate = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault())
                .format(java.util.Date(nbf * 1000))
            claims.add("Not Before" to nbfDate)
        }
        if (json.has("jti")) claims.add("JWT ID" to json.getString("jti"))

        claims
    } catch (_: Exception) {
        emptyList()
    }
}
