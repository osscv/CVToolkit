package cv.toolkit.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.util.Base64
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import cv.toolkit.R
import cv.toolkit.ads.BannerAd
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

// ─── Data models ──────────────────────────────────────────────────────────────

private data class SavedApiRequest(
    val name: String,
    val method: String,
    val url: String,
    val headers: List<List<String>>,
    val params: List<List<String>> = emptyList(),
    val body: String = "",
    val bodyType: String = "json",
    val authType: String = "none",
    val authToken: String = "",
    val authUser: String = "",
    val authPass: String = "",
    val authKeyName: String = "",
    val authKeyValue: String = "",
    val authKeyIn: String = "header",
    val timeout: Int = 30,
    val followRedirects: Boolean = true,
    val verifySSL: Boolean = true
)

private data class ApiResponse(
    val statusCode: Int,
    val statusMessage: String,
    val responseTimeMs: Long,
    val headers: List<Pair<String, String>>,
    val body: String,
    val bodySize: Long,
    val protocol: String
)

private const val PREFS_NAME = "api_tester_collection"
private const val PREFS_KEY_REQUESTS = "saved_requests"

private val httpMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "HEAD", "OPTIONS", "COPY", "LINK", "PURGE")

private fun methodColor(method: String): Color = when (method) {
    "GET" -> Color(0xFF4CAF50)
    "POST" -> Color(0xFFFF9800)
    "PUT" -> Color(0xFF2196F3)
    "PATCH" -> Color(0xFF9C27B0)
    "DELETE" -> Color(0xFFF44336)
    "HEAD" -> Color(0xFF607D8B)
    "OPTIONS" -> Color(0xFF009688)
    else -> Color(0xFF795548)
}

// ─── Persistence ──────────────────────────────────────────────────────────────

private fun loadSavedRequests(context: Context): List<SavedApiRequest> {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val json = prefs.getString(PREFS_KEY_REQUESTS, null) ?: return emptyList()
    return try {
        val type = object : TypeToken<List<SavedApiRequest>>() {}.type
        Gson().fromJson(json, type)
    } catch (_: Exception) { emptyList() }
}

private fun saveSavedRequests(context: Context, requests: List<SavedApiRequest>) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit().putString(PREFS_KEY_REQUESTS, Gson().toJson(requests)).apply()
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

private fun prettyPrintJson(raw: String): String {
    return try {
        val el = JsonParser.parseString(raw)
        GsonBuilder().setPrettyPrinting().create().toJson(el)
    } catch (_: Exception) { raw }
}

private fun buildCurlCommand(
    method: String, url: String,
    headers: List<Pair<String, String>>,
    body: String, bodyType: String
): String {
    val sb = StringBuilder("curl -X $method")
    headers.filter { it.first.isNotBlank() }.forEach { (k, v) ->
        sb.append(" \\\n  -H '${k}: ${v}'")
    }
    if (method in listOf("POST", "PUT", "PATCH") && body.isNotBlank()) {
        val escaped = body.replace("'", "'\\''")
        if (bodyType == "form") {
            sb.append(" \\\n  --data-urlencode '$escaped'")
        } else {
            sb.append(" \\\n  -d '$escaped'")
        }
    }
    sb.append(" \\\n  '$url'")
    return sb.toString()
}

private fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
    else -> "%.2f MB".format(bytes / (1024.0 * 1024.0))
}

private val defaultHeaderPresets = listOf(
    "Content-Type" to "application/json",
    "Content-Type" to "application/x-www-form-urlencoded",
    "Content-Type" to "text/xml",
    "Content-Type" to "multipart/form-data",
    "Accept" to "application/json",
    "Accept" to "*/*",
    "Authorization" to "Bearer ",
    "Cache-Control" to "no-cache",
    "User-Agent" to "CVToolkit/1.0",
    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36",
    "X-Requested-With" to "XMLHttpRequest",
    "Origin" to "https://",
    "Referer" to "https://",
    "X-Forwarded-For" to "",
    "X-Custom-Header" to "",
)

private const val PREFS_KEY_CUSTOM_PRESETS = "custom_header_presets"

private fun loadCustomPresets(context: Context): List<Pair<String, String>> {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val json = prefs.getString(PREFS_KEY_CUSTOM_PRESETS, null) ?: return emptyList()
    return try {
        val type = object : TypeToken<List<List<String>>>() {}.type
        val raw: List<List<String>> = Gson().fromJson(json, type)
        raw.map { Pair(it.getOrElse(0) { "" }, it.getOrElse(1) { "" }) }
    } catch (_: Exception) { emptyList() }
}

private fun saveCustomPresets(context: Context, presets: List<Pair<String, String>>) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    val raw = presets.map { listOf(it.first, it.second) }
    prefs.edit().putString(PREFS_KEY_CUSTOM_PRESETS, Gson().toJson(raw)).apply()
}

// ─── Main Screen ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApiTesterScreen(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val snackbarHostState = remember { SnackbarHostState() }

    // ── Request state
    var selectedMethod by remember { mutableStateOf("GET") }
    var showMethodDropdown by remember { mutableStateOf(false) }
    var urlInput by remember { mutableStateOf("") }
    var requestBody by remember { mutableStateOf("") }
    var bodyType by remember { mutableStateOf("json") } // json, form, raw, xml, none
    var headers by remember { mutableStateOf(listOf(Pair("", ""))) }
    var queryParams by remember { mutableStateOf(listOf(Pair("", ""))) }

    // ── Auth state
    var authType by remember { mutableStateOf("none") } // none, bearer, basic, apikey
    var authToken by remember { mutableStateOf("") }
    var authUser by remember { mutableStateOf("") }
    var authPass by remember { mutableStateOf("") }
    var authKeyName by remember { mutableStateOf("") }
    var authKeyValue by remember { mutableStateOf("") }
    var authKeyIn by remember { mutableStateOf("header") } // header, query

    // ── Settings state
    var timeout by remember { mutableIntStateOf(30) }
    var followRedirects by remember { mutableStateOf(true) }
    var verifySSL by remember { mutableStateOf(true) }

    // ── Tab state: 0=Params, 1=Headers, 2=Auth, 3=Body, 4=Response
    var selectedTab by remember { mutableIntStateOf(1) }

    // ── Response state
    var isLoading by remember { mutableStateOf(false) }
    var apiResponse by remember { mutableStateOf<ApiResponse?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // ── Dialog state
    var showSaveDialog by remember { mutableStateOf(false) }
    var saveRequestName by remember { mutableStateOf("") }
    var showLoadDialog by remember { mutableStateOf(false) }
    var savedRequests by remember { mutableStateOf(loadSavedRequests(context)) }
    var showCurlDialog by remember { mutableStateOf(false) }
    var showPresetsMenu by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showSavePresetDialog by remember { mutableStateOf(false) }
    var newPresetKey by remember { mutableStateOf("") }
    var newPresetValue by remember { mutableStateOf("") }
    var customPresets by remember { mutableStateOf(loadCustomPresets(context)) }

    // ── Build final URL with query params
    fun buildFinalUrl(): String {
        var url = urlInput.trim()
        if (url.isBlank()) return ""
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "https://$url"
        }
        val validParams = queryParams.filter { it.first.isNotBlank() }
        if (validParams.isNotEmpty()) {
            val separator = if (url.contains("?")) "&" else "?"
            val paramStr = validParams.joinToString("&") { (k, v) ->
                "${java.net.URLEncoder.encode(k, "UTF-8")}=${java.net.URLEncoder.encode(v, "UTF-8")}"
            }
            url += separator + paramStr
        }
        // API Key in query param
        if (authType == "apikey" && authKeyIn == "query" && authKeyName.isNotBlank()) {
            val sep = if (url.contains("?")) "&" else "?"
            url += "${sep}${java.net.URLEncoder.encode(authKeyName, "UTF-8")}=${java.net.URLEncoder.encode(authKeyValue, "UTF-8")}"
        }
        return url
    }

    // ── Build all headers including auth
    fun buildAllHeaders(): List<Pair<String, String>> {
        val result = headers.filter { it.first.isNotBlank() && it.second.isNotBlank() }.toMutableList()
        // Auth headers
        when (authType) {
            "bearer" -> if (authToken.isNotBlank()) result.add("Authorization" to "Bearer $authToken")
            "basic" -> {
                val encoded = Base64.encodeToString("$authUser:$authPass".toByteArray(), Base64.NO_WRAP)
                result.add("Authorization" to "Basic $encoded")
            }
            "apikey" -> if (authKeyIn == "header" && authKeyName.isNotBlank()) {
                result.add(authKeyName to authKeyValue)
            }
        }
        // Default User-Agent if not set
        if (result.none { it.first.equals("User-Agent", ignoreCase = true) }) {
            result.add("User-Agent" to "CVToolkit/1.0")
        }
        // Content-Type based on body type if not set
        if (selectedMethod in listOf("POST", "PUT", "PATCH") && result.none { it.first.equals("Content-Type", ignoreCase = true) }) {
            when (bodyType) {
                "json" -> result.add("Content-Type" to "application/json")
                "form" -> result.add("Content-Type" to "application/x-www-form-urlencoded")
                "xml" -> result.add("Content-Type" to "text/xml")
                "graphql" -> result.add("Content-Type" to "application/json")
                "raw" -> result.add("Content-Type" to "text/plain")
            }
        }
        return result
    }

    @Suppress("CustomX509TrustManager", "TrustAllX509TrustManager")
    fun sendRequest() {
        val finalUrl = buildFinalUrl()
        if (finalUrl.isBlank()) return

        isLoading = true
        errorMessage = null
        apiResponse = null

        val allHeaders = buildAllHeaders()
        val method = selectedMethod
        val body = requestBody
        val currentBodyType = bodyType
        val currentTimeout = timeout.toLong()
        val currentFollowRedirects = followRedirects
        val currentVerifySSL = verifySSL

        scope.launch {
            try {
                val response = withContext(Dispatchers.IO) {
                    val clientBuilder = OkHttpClient.Builder()
                        .connectTimeout(currentTimeout, TimeUnit.SECONDS)
                        .readTimeout(currentTimeout, TimeUnit.SECONDS)
                        .writeTimeout(currentTimeout, TimeUnit.SECONDS)
                        .followRedirects(currentFollowRedirects)
                        .followSslRedirects(currentFollowRedirects)

                    if (!currentVerifySSL) {
                        val trustAll = object : X509TrustManager {
                            override fun checkClientTrusted(chain: Array<X509Certificate>?, authType: String?) {}
                            override fun checkServerTrusted(chain: Array<X509Certificate>?, authType: String?) {}
                            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
                        }
                        val sslContext = SSLContext.getInstance("TLS")
                        sslContext.init(null, arrayOf<TrustManager>(trustAll), SecureRandom())
                        clientBuilder.sslSocketFactory(sslContext.socketFactory, trustAll)
                        clientBuilder.hostnameVerifier { _, _ -> true }
                    }

                    val client = clientBuilder.build()
                    val requestBuilder = Request.Builder().url(finalUrl)

                    allHeaders.forEach { (key, value) -> requestBuilder.addHeader(key, value) }

                    val requestBodyObj = when {
                        method in listOf("POST", "PUT", "PATCH") && currentBodyType == "form" && body.isNotBlank() -> {
                            val formBuilder = FormBody.Builder()
                            body.lines().forEach { line ->
                                val parts = line.split("=", limit = 2)
                                if (parts.size == 2) formBuilder.add(parts[0].trim(), parts[1].trim())
                            }
                            formBuilder.build()
                        }
                        method in listOf("POST", "PUT", "PATCH") && body.isNotBlank() -> {
                            val ct = allHeaders.firstOrNull { it.first.equals("Content-Type", ignoreCase = true) }?.second
                                ?: "application/json"
                            body.toRequestBody(ct.toMediaTypeOrNull())
                        }
                        method in listOf("POST", "PUT", "PATCH") -> "".toRequestBody(null)
                        method == "DELETE" && body.isNotBlank() -> {
                            val ct = allHeaders.firstOrNull { it.first.equals("Content-Type", ignoreCase = true) }?.second
                                ?: "application/json"
                            body.toRequestBody(ct.toMediaTypeOrNull())
                        }
                        else -> null
                    }

                    requestBuilder.method(method, requestBodyObj)

                    val startTime = System.currentTimeMillis()
                    val response = client.newCall(requestBuilder.build()).execute()
                    val elapsed = System.currentTimeMillis() - startTime

                    val responseHeaders = mutableListOf<Pair<String, String>>()
                    response.headers.forEach { (name, value) -> responseHeaders.add(name to value) }

                    val responseBodyBytes = response.body?.bytes()
                    val responseBody = responseBodyBytes?.let { String(it) } ?: ""
                    val bodySize = responseBodyBytes?.size?.toLong() ?: 0L

                    ApiResponse(
                        statusCode = response.code,
                        statusMessage = response.message.ifEmpty { "OK" },
                        responseTimeMs = elapsed,
                        headers = responseHeaders,
                        body = responseBody,
                        bodySize = bodySize,
                        protocol = response.protocol.toString()
                    )
                }
                apiResponse = response
                selectedTab = 4
            } catch (e: Exception) {
                errorMessage = "${e.javaClass.simpleName}: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.api_tester_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    // Settings
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(Icons.Filled.Settings, "Settings")
                    }
                    // cURL
                    IconButton(onClick = { showCurlDialog = true }) {
                        Icon(Icons.Filled.Terminal, "cURL")
                    }
                    // Load
                    IconButton(onClick = {
                        savedRequests = loadSavedRequests(context)
                        showLoadDialog = true
                    }) {
                        Icon(Icons.Filled.FolderOpen, "Load")
                    }
                    // Save
                    IconButton(onClick = {
                        saveRequestName = ""
                        showSaveDialog = true
                    }) {
                        Icon(Icons.Filled.Save, "Save")
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
                .padding(horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Spacer(Modifier.height(4.dp))

            // ── Method + URL row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ExposedDropdownMenuBox(
                    expanded = showMethodDropdown,
                    onExpandedChange = { showMethodDropdown = it },
                    modifier = Modifier.width(128.dp)
                ) {
                    OutlinedTextField(
                        value = selectedMethod,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showMethodDropdown) },
                        modifier = Modifier.menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, enabled = true),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            fontWeight = FontWeight.Bold,
                            color = methodColor(selectedMethod)
                        )
                    )
                    ExposedDropdownMenu(
                        expanded = showMethodDropdown,
                        onDismissRequest = { showMethodDropdown = false }
                    ) {
                        httpMethods.forEach { method ->
                            DropdownMenuItem(
                                text = {
                                    Text(method, fontWeight = FontWeight.Bold, color = methodColor(method))
                                },
                                onClick = { selectedMethod = method; showMethodDropdown = false }
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    placeholder = { Text("https://api.example.com/v1/users") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                    trailingIcon = {
                        if (urlInput.isNotEmpty()) {
                            IconButton(onClick = { urlInput = "" }) {
                                Icon(Icons.Filled.Clear, "Clear", Modifier.size(18.dp))
                            }
                        }
                    }
                )
            }

            // ── Send button
            Button(
                onClick = { sendRequest() },
                modifier = Modifier.fillMaxWidth(),
                enabled = urlInput.isNotBlank() && !isLoading,
                colors = ButtonDefaults.buttonColors(containerColor = methodColor(selectedMethod))
            ) {
                if (isLoading) {
                    CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                } else {
                    Icon(Icons.AutoMirrored.Filled.Send, null, Modifier.size(18.dp))
                }
                Spacer(Modifier.width(8.dp))
                Text("Send Request", fontWeight = FontWeight.Bold)
            }

            // ── Error
            errorMessage?.let {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Error, null, tint = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(it, color = MaterialTheme.colorScheme.onErrorContainer, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            // ── Tabs: Params | Headers | Auth | Body | Response
            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth(),
                edgePadding = 0.dp,
                divider = {}
            ) {
                val paramCount = queryParams.count { it.first.isNotBlank() }
                val headerCount = headers.count { it.first.isNotBlank() }
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 },
                    text = { Text("Params" + if (paramCount > 0) " ($paramCount)" else "") })
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 },
                    text = { Text("Headers" + if (headerCount > 0) " ($headerCount)" else "") })
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 },
                    text = { Text("Auth" + if (authType != "none") " \u2022" else "") })
                Tab(selected = selectedTab == 3, onClick = { selectedTab = 3 },
                    text = { Text("Body") })
                Tab(selected = selectedTab == 4, onClick = { selectedTab = 4 },
                    text = {
                        if (apiResponse != null) {
                            val r = apiResponse!!
                            Text("${r.statusCode} \u2022 ${r.responseTimeMs}ms")
                        } else Text("Response")
                    })
            }

            HorizontalDivider()

            // ── Tab content
            when (selectedTab) {
                // ── PARAMS TAB
                0 -> KeyValueEditor(
                    items = queryParams,
                    onItemsChange = { queryParams = it },
                    keyPlaceholder = "Parameter",
                    valuePlaceholder = "Value",
                    addLabel = "Add Parameter",
                    modifier = Modifier.weight(1f)
                )

                // ── HEADERS TAB
                1 -> Column(Modifier.weight(1f)) {
                    // Presets row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Quick Add:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Box {
                            AssistChip(
                                onClick = { showPresetsMenu = true },
                                label = { Text("Presets") },
                                leadingIcon = { Icon(Icons.Filled.Add, null, Modifier.size(16.dp)) }
                            )
                            DropdownMenu(expanded = showPresetsMenu, onDismissRequest = { showPresetsMenu = false }) {
                                // Custom presets first
                                if (customPresets.isNotEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("MY PRESETS", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary) },
                                        onClick = {},
                                        enabled = false
                                    )
                                    customPresets.forEachIndexed { index, (k, v) ->
                                        DropdownMenuItem(
                                            text = {
                                                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                                    Column(Modifier.weight(1f)) {
                                                        Text(k, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                                        Text(v.ifEmpty { "(empty)" }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                                    }
                                                    IconButton(
                                                        onClick = {
                                                            customPresets = customPresets.toMutableList().also { it.removeAt(index) }
                                                            saveCustomPresets(context, customPresets)
                                                        },
                                                        modifier = Modifier.size(24.dp)
                                                    ) {
                                                        Icon(Icons.Filled.Close, "Remove", Modifier.size(14.dp), tint = MaterialTheme.colorScheme.error)
                                                    }
                                                }
                                            },
                                            onClick = {
                                                headers = headers + Pair(k, v)
                                                showPresetsMenu = false
                                            }
                                        )
                                    }
                                    HorizontalDivider(Modifier.padding(vertical = 4.dp))
                                }
                                // Default presets
                                DropdownMenuItem(
                                    text = { Text("BUILT-IN", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                                    onClick = {},
                                    enabled = false
                                )
                                defaultHeaderPresets.forEach { (k, v) ->
                                    DropdownMenuItem(
                                        text = {
                                            Column {
                                                Text(k, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
                                                if (v.isNotBlank()) Text(v, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                            }
                                        },
                                        onClick = {
                                            headers = headers + Pair(k, v)
                                            showPresetsMenu = false
                                        }
                                    )
                                }
                                HorizontalDivider(Modifier.padding(vertical = 4.dp))
                                // Save custom preset option
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Icon(Icons.Filled.BookmarkAdd, null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
                                            Text("Save Custom Preset...", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                                        }
                                    },
                                    onClick = {
                                        showPresetsMenu = false
                                        newPresetKey = ""
                                        newPresetValue = ""
                                        showSavePresetDialog = true
                                    }
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                    KeyValueEditor(
                        items = headers,
                        onItemsChange = { headers = it },
                        keyPlaceholder = "Header Name",
                        valuePlaceholder = "Header Value",
                        addLabel = "Add Header",
                        modifier = Modifier.weight(1f)
                    )
                }

                // ── AUTH TAB
                2 -> Card(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Authorization", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)

                        SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                            listOf("none" to "None", "bearer" to "Bearer", "basic" to "Basic", "apikey" to "API Key").forEachIndexed { index, (tag, label) ->
                                SegmentedButton(
                                    selected = authType == tag,
                                    onClick = { authType = tag },
                                    shape = SegmentedButtonDefaults.itemShape(index, 4)
                                ) { Text(label, style = MaterialTheme.typography.labelSmall) }
                            }
                        }

                        when (authType) {
                            "bearer" -> {
                                OutlinedTextField(
                                    value = authToken,
                                    onValueChange = { authToken = it },
                                    label = { Text("Token") },
                                    placeholder = { Text("Enter Bearer token") },
                                    modifier = Modifier.fillMaxWidth(),
                                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                                    minLines = 3
                                )
                                Text(
                                    "Will be sent as: Authorization: Bearer <token>",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            "basic" -> {
                                OutlinedTextField(
                                    value = authUser,
                                    onValueChange = { authUser = it },
                                    label = { Text("Username") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = authPass,
                                    onValueChange = { authPass = it },
                                    label = { Text("Password") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                Text(
                                    "Will be sent as: Authorization: Basic <base64(user:pass)>",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            "apikey" -> {
                                OutlinedTextField(
                                    value = authKeyName,
                                    onValueChange = { authKeyName = it },
                                    label = { Text("Key Name") },
                                    placeholder = { Text("e.g. X-API-Key") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                OutlinedTextField(
                                    value = authKeyValue,
                                    onValueChange = { authKeyValue = it },
                                    label = { Text("Key Value") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text("Send in:", style = MaterialTheme.typography.labelMedium)
                                    Spacer(Modifier.width(12.dp))
                                    FilterChip(selected = authKeyIn == "header", onClick = { authKeyIn = "header" }, label = { Text("Header") })
                                    Spacer(Modifier.width(8.dp))
                                    FilterChip(selected = authKeyIn == "query", onClick = { authKeyIn = "query" }, label = { Text("Query Param") })
                                }
                            }
                            else -> {
                                Text("No authorization", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                // ── BODY / PAYLOAD TAB
                3 -> Card(
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Request Payload", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        // Body type selector
                        Row(
                            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            listOf(
                                "none" to "None", "json" to "JSON", "form" to "Form Data",
                                "graphql" to "GraphQL", "raw" to "Raw", "xml" to "XML"
                            ).forEach { (tag, label) ->
                                FilterChip(
                                    selected = bodyType == tag,
                                    onClick = { bodyType = tag },
                                    label = { Text(label, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))

                        when (bodyType) {
                            "none" -> {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(Icons.Filled.Block, null, Modifier.size(36.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
                                        Spacer(Modifier.height(8.dp))
                                        Text("No payload for this request", color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                            "form" -> {
                                // Form data as key-value pairs editor
                                Text("Form fields (key=value per line, or use editor below):", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.height(4.dp))
                                OutlinedTextField(
                                    value = requestBody,
                                    onValueChange = { requestBody = it },
                                    placeholder = { Text("username=admin\npassword=secret\nemail=user@example.com\nremember=true", style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)) },
                                    modifier = Modifier.fillMaxSize(),
                                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, lineHeight = 20.sp),
                                    label = { Text("Form Fields (key=value)") }
                                )
                            }
                            "graphql" -> {
                                OutlinedTextField(
                                    value = requestBody,
                                    onValueChange = { requestBody = it },
                                    placeholder = {
                                        Text(
                                            "{\n  \"query\": \"query { users { id name email } }\",\n  \"variables\": {}\n}",
                                            style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                                        )
                                    },
                                    modifier = Modifier.fillMaxSize(),
                                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, lineHeight = 20.sp),
                                    label = { Text("GraphQL Query (JSON)") }
                                )
                            }
                            else -> {
                                val (placeholder, label) = when (bodyType) {
                                    "json" -> "{\n  \"key\": \"value\",\n  \"name\": \"example\",\n  \"items\": [1, 2, 3]\n}" to "JSON Payload"
                                    "xml" -> "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<root>\n  <item id=\"1\">value</item>\n</root>" to "XML Payload"
                                    else -> "Enter request body / payload..." to "Raw Payload"
                                }
                                OutlinedTextField(
                                    value = requestBody,
                                    onValueChange = { requestBody = it },
                                    placeholder = { Text(placeholder, style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)) },
                                    modifier = Modifier.fillMaxSize(),
                                    textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, lineHeight = 20.sp),
                                    label = { Text(label) }
                                )
                            }
                        }
                    }
                }

                // ── RESPONSE TAB
                4 -> {
                    if (apiResponse != null) {
                        val response = apiResponse!!
                        // Status bar
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = when {
                                    response.statusCode in 200..299 -> Color(0xFF4CAF50).copy(alpha = 0.15f)
                                    response.statusCode in 300..399 -> Color(0xFFFF9800).copy(alpha = 0.15f)
                                    else -> Color(0xFFF44336).copy(alpha = 0.15f)
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = MaterialTheme.shapes.small,
                                        color = when {
                                            response.statusCode in 200..299 -> Color(0xFF4CAF50)
                                            response.statusCode in 300..399 -> Color(0xFFFF9800)
                                            else -> Color(0xFFF44336)
                                        }
                                    ) {
                                        Text(
                                            "${response.statusCode}",
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                    }
                                    Column {
                                        Text(response.statusMessage, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                        Text(response.protocol.uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("${response.responseTimeMs}ms", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                    Text(formatBytes(response.bodySize), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }

                        // Response sub-tabs
                        var responseSubTab by remember { mutableIntStateOf(0) }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(selected = responseSubTab == 0, onClick = { responseSubTab = 0 }, label = { Text("Body") })
                            FilterChip(selected = responseSubTab == 1, onClick = { responseSubTab = 1 }, label = { Text("Headers (${response.headers.size})") })
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            when (responseSubTab) {
                                0 -> {
                                    Column {
                                        // Action bar
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
                                            horizontalArrangement = Arrangement.End,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            TextButton(onClick = {
                                                clipboard.setPrimaryClip(ClipData.newPlainText("response", response.body))
                                                scope.launch { snackbarHostState.showSnackbar("Response copied") }
                                            }) {
                                                Icon(Icons.Filled.ContentCopy, null, Modifier.size(16.dp))
                                                Spacer(Modifier.width(4.dp))
                                                Text("Copy", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                        SelectionContainer {
                                            Text(
                                                text = prettyPrintJson(response.body).ifEmpty { "(empty response)" },
                                                style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, lineHeight = 18.sp),
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                                                    .verticalScroll(rememberScrollState())
                                                    .horizontalScroll(rememberScrollState())
                                            )
                                        }
                                    }
                                }
                                1 -> {
                                    LazyColumn(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        items(response.headers) { (key, value) ->
                                            Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp)) {
                                                Text(
                                                    key,
                                                    style = MaterialTheme.typography.labelMedium,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    modifier = Modifier.width(130.dp)
                                                )
                                                Text(
                                                    value,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontFamily = FontFamily.Monospace,
                                                    modifier = Modifier.weight(1f),
                                                    maxLines = 3,
                                                    overflow = TextOverflow.Ellipsis
                                                )
                                            }
                                            HorizontalDivider(Modifier.padding(horizontal = 8.dp))
                                        }
                                    }
                                }
                            }
                        }
                    } else if (isLoading) {
                        Card(modifier = Modifier.fillMaxWidth().weight(1f), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                CircularProgressIndicator()
                                Spacer(Modifier.height(16.dp))
                                Text("Sending request...", style = MaterialTheme.typography.bodyMedium)
                            }
                        }
                    } else {
                        Card(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        ) {
                            Column(Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                                Icon(Icons.Filled.Api, null, Modifier.size(48.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                                Spacer(Modifier.height(16.dp))
                                Text("No response yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                                Spacer(Modifier.height(4.dp))
                                Text("Enter a URL and hit Send", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            BannerAd(modifier = Modifier.fillMaxWidth())
        }
    }

    // ── Save Dialog
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Save Request") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Save the current request configuration to your collection.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(
                        value = saveRequestName,
                        onValueChange = { saveRequestName = it },
                        label = { Text("Request Name") },
                        placeholder = { Text("e.g. Get Users, Create Post") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    if (urlInput.isNotBlank()) {
                        Text(
                            "$selectedMethod $urlInput",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = methodColor(selectedMethod),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (saveRequestName.isNotBlank()) {
                            val request = SavedApiRequest(
                                name = saveRequestName.trim(),
                                method = selectedMethod,
                                url = urlInput,
                                headers = headers.filter { it.first.isNotBlank() }.map { listOf(it.first, it.second) },
                                params = queryParams.filter { it.first.isNotBlank() }.map { listOf(it.first, it.second) },
                                body = requestBody,
                                bodyType = bodyType,
                                authType = authType,
                                authToken = authToken,
                                authUser = authUser,
                                authPass = authPass,
                                authKeyName = authKeyName,
                                authKeyValue = authKeyValue,
                                authKeyIn = authKeyIn,
                                timeout = timeout,
                                followRedirects = followRedirects,
                                verifySSL = verifySSL
                            )
                            val current = loadSavedRequests(context).toMutableList()
                            current.add(request)
                            saveSavedRequests(context, current)
                            savedRequests = current
                            showSaveDialog = false
                            scope.launch { snackbarHostState.showSnackbar("Request saved") }
                        }
                    },
                    enabled = saveRequestName.isNotBlank()
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showSaveDialog = false }) { Text("Cancel") } }
        )
    }

    // ── Load Dialog
    if (showLoadDialog) {
        AlertDialog(
            onDismissRequest = { showLoadDialog = false },
            title = { Text("Saved Requests (${savedRequests.size})") },
            text = {
                if (savedRequests.isEmpty()) {
                    Text("No saved requests yet.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.heightIn(max = 400.dp)) {
                        items(savedRequests) { request ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(request.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                            Surface(shape = MaterialTheme.shapes.extraSmall, color = methodColor(request.method).copy(alpha = 0.2f)) {
                                                Text(request.method, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = methodColor(request.method))
                                            }
                                            Text(request.url, style = MaterialTheme.typography.bodySmall, fontFamily = FontFamily.Monospace, maxLines = 1, overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                    Row {
                                        IconButton(onClick = {
                                            selectedMethod = request.method
                                            urlInput = request.url
                                            headers = if (request.headers.isNotEmpty()) request.headers.map { Pair(it.getOrElse(0) { "" }, it.getOrElse(1) { "" }) } else listOf(Pair("", ""))
                                            queryParams = if (request.params.isNotEmpty()) request.params.map { Pair(it.getOrElse(0) { "" }, it.getOrElse(1) { "" }) } else listOf(Pair("", ""))
                                            requestBody = request.body
                                            bodyType = request.bodyType
                                            authType = request.authType
                                            authToken = request.authToken
                                            authUser = request.authUser
                                            authPass = request.authPass
                                            authKeyName = request.authKeyName
                                            authKeyValue = request.authKeyValue
                                            authKeyIn = request.authKeyIn
                                            timeout = request.timeout
                                            followRedirects = request.followRedirects
                                            verifySSL = request.verifySSL
                                            selectedTab = 1
                                            showLoadDialog = false
                                        }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Filled.FileOpen, "Load", Modifier.size(18.dp))
                                        }
                                        IconButton(onClick = {
                                            val updated = savedRequests.toMutableList().also { it.remove(request) }
                                            saveSavedRequests(context, updated)
                                            savedRequests = updated
                                        }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Filled.Delete, "Delete", Modifier.size(18.dp), tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showLoadDialog = false }) { Text("Close") } }
        )
    }

    // ── cURL Dialog
    if (showCurlDialog) {
        val curlCmd = buildCurlCommand(selectedMethod, buildFinalUrl(), buildAllHeaders(), requestBody, bodyType)
        AlertDialog(
            onDismissRequest = { showCurlDialog = false },
            title = { Text("cURL Command") },
            text = {
                SelectionContainer {
                    Text(
                        curlCmd,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace, lineHeight = 18.sp),
                        modifier = Modifier.verticalScroll(rememberScrollState())
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    clipboard.setPrimaryClip(ClipData.newPlainText("curl", curlCmd))
                    scope.launch { snackbarHostState.showSnackbar("cURL copied") }
                    showCurlDialog = false
                }) { Text("Copy") }
            },
            dismissButton = { TextButton(onClick = { showCurlDialog = false }) { Text("Close") } }
        )
    }

    // ── Save Custom Preset Dialog
    if (showSavePresetDialog) {
        AlertDialog(
            onDismissRequest = { showSavePresetDialog = false },
            title = { Text("Save Custom Header Preset") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        "Save a custom header as a preset for quick access.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = newPresetKey,
                        onValueChange = { newPresetKey = it },
                        label = { Text("Header Name") },
                        placeholder = { Text("e.g. X-Api-Key, Authorization") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                    )
                    OutlinedTextField(
                        value = newPresetValue,
                        onValueChange = { newPresetValue = it },
                        label = { Text("Default Value (optional)") },
                        placeholder = { Text("e.g. Bearer token123...") },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        minLines = 2
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newPresetKey.isNotBlank()) {
                            customPresets = customPresets + Pair(newPresetKey.trim(), newPresetValue.trim())
                            saveCustomPresets(context, customPresets)
                            showSavePresetDialog = false
                            scope.launch { snackbarHostState.showSnackbar("Preset saved") }
                        }
                    },
                    enabled = newPresetKey.isNotBlank()
                ) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { showSavePresetDialog = false }) { Text("Cancel") } }
        )
    }

    // ── Settings Dialog
    if (showSettingsDialog) {
        AlertDialog(
            onDismissRequest = { showSettingsDialog = false },
            title = { Text("Request Settings") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Timeout
                    OutlinedTextField(
                        value = timeout.toString(),
                        onValueChange = { timeout = it.filter { c -> c.isDigit() }.toIntOrNull() ?: 30 },
                        label = { Text("Timeout (seconds)") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )
                    // Follow Redirects
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column {
                            Text("Follow Redirects", style = MaterialTheme.typography.bodyMedium)
                            Text("Automatically follow 3xx redirects", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = followRedirects, onCheckedChange = { followRedirects = it })
                    }
                    // SSL Verification
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Verify SSL", style = MaterialTheme.typography.bodyMedium)
                            Text("Validate server certificates", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(checked = verifySSL, onCheckedChange = { verifySSL = it })
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showSettingsDialog = false }) { Text("Done") } }
        )
    }
}

// ── Reusable Key-Value Editor ─────────────────────────────────────────────────

@Composable
private fun KeyValueEditor(
    items: List<Pair<String, String>>,
    onItemsChange: (List<Pair<String, String>>) -> Unit,
    keyPlaceholder: String,
    valuePlaceholder: String,
    addLabel: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(items.indices.toList()) { index ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = items[index].first,
                            onValueChange = { newKey ->
                                onItemsChange(items.toMutableList().also { it[index] = Pair(newKey, it[index].second) })
                            },
                            placeholder = { Text(keyPlaceholder, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                        )
                        OutlinedTextField(
                            value = items[index].second,
                            onValueChange = { newValue ->
                                onItemsChange(items.toMutableList().also { it[index] = Pair(it[index].first, newValue) })
                            },
                            placeholder = { Text(valuePlaceholder, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.weight(1f),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                        )
                        IconButton(
                            onClick = {
                                val newList = items.toMutableList().also { it.removeAt(index) }
                                onItemsChange(newList.ifEmpty { listOf(Pair("", "")) })
                            },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(Icons.Filled.Close, "Remove", Modifier.size(16.dp))
                        }
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            OutlinedButton(
                onClick = { onItemsChange(items + Pair("", "")) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Add, null, Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text(addLabel)
            }
        }
    }
}
