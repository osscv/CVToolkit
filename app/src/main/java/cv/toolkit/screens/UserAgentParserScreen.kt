package cv.toolkit.screens

import android.webkit.WebSettings
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

data class ParsedUserAgent(
    val browser: String?,
    val browserVersion: String?,
    val engine: String?,
    val engineVersion: String?,
    val os: String?,
    val osVersion: String?,
    val deviceType: String,
    val device: String?,
    val isMobile: Boolean,
    val isBot: Boolean
)

data class SampleUserAgent(val name: String, val ua: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserAgentParserScreen(navController: NavController) {
    val context = LocalContext.current
    var userAgentInput by remember { mutableStateOf("") }
    var parsedResult by remember { mutableStateOf<ParsedUserAgent?>(null) }
    var showSamples by remember { mutableStateOf(false) }
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    // Get device's default user agent
    val defaultUserAgent = remember {
        try {
            WebSettings.getDefaultUserAgent(context)
        } catch (e: Exception) {
            "Mozilla/5.0 (Linux; Android) AppleWebKit/537.36"
        }
    }

    val sampleUserAgents = listOf(
        SampleUserAgent("Chrome (Windows)", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"),
        SampleUserAgent("Chrome (Android)", "Mozilla/5.0 (Linux; Android 14; Pixel 8) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"),
        SampleUserAgent("Safari (macOS)", "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_2) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2 Safari/605.1.15"),
        SampleUserAgent("Safari (iOS)", "Mozilla/5.0 (iPhone; CPU iPhone OS 17_2 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.2 Mobile/15E148 Safari/604.1"),
        SampleUserAgent("Firefox (Windows)", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:121.0) Gecko/20100101 Firefox/121.0"),
        SampleUserAgent("Edge (Windows)", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 Edg/120.0.0.0"),
        SampleUserAgent("Samsung Browser", "Mozilla/5.0 (Linux; Android 14; SM-S918B) AppleWebKit/537.36 (KHTML, like Gecko) SamsungBrowser/23.0 Chrome/115.0.0.0 Mobile Safari/537.36"),
        SampleUserAgent("Opera (Windows)", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36 OPR/106.0.0.0"),
        SampleUserAgent("Googlebot", "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)"),
        SampleUserAgent("Bingbot", "Mozilla/5.0 (compatible; bingbot/2.0; +http://www.bing.com/bingbot.htm)"),
        SampleUserAgent("curl", "curl/8.4.0"),
        SampleUserAgent("Postman", "PostmanRuntime/7.36.0")
    )

    fun parseUserAgent(ua: String): ParsedUserAgent {
        val uaLower = ua.lowercase()

        // Detect if it's a bot
        val isBot = listOf("bot", "crawler", "spider", "slurp", "mediapartners", "curl", "wget", "postman")
            .any { uaLower.contains(it) }

        // Detect browser
        val (browser, browserVersion) = when {
            ua.contains("Edg/") -> "Microsoft Edge" to extractVersion(ua, "Edg/")
            ua.contains("OPR/") || ua.contains("Opera") -> "Opera" to (extractVersion(ua, "OPR/") ?: extractVersion(ua, "Opera/"))
            ua.contains("SamsungBrowser") -> "Samsung Browser" to extractVersion(ua, "SamsungBrowser/")
            ua.contains("UCBrowser") -> "UC Browser" to extractVersion(ua, "UCBrowser/")
            ua.contains("Brave") -> "Brave" to extractVersion(ua, "Brave/")
            ua.contains("Vivaldi") -> "Vivaldi" to extractVersion(ua, "Vivaldi/")
            ua.contains("Firefox") -> "Firefox" to extractVersion(ua, "Firefox/")
            ua.contains("FxiOS") -> "Firefox (iOS)" to extractVersion(ua, "FxiOS/")
            ua.contains("CriOS") -> "Chrome (iOS)" to extractVersion(ua, "CriOS/")
            ua.contains("Chrome") && !ua.contains("Chromium") -> "Chrome" to extractVersion(ua, "Chrome/")
            ua.contains("Chromium") -> "Chromium" to extractVersion(ua, "Chromium/")
            ua.contains("Safari") && ua.contains("Version") -> "Safari" to extractVersion(ua, "Version/")
            ua.contains("MSIE") -> "Internet Explorer" to extractVersion(ua, "MSIE ")
            ua.contains("Trident") -> "Internet Explorer 11" to "11.0"
            ua.contains("curl") -> "curl" to extractVersion(ua, "curl/")
            ua.contains("Postman") -> "Postman" to extractVersion(ua, "PostmanRuntime/")
            ua.contains("Googlebot") -> "Googlebot" to extractVersion(ua, "Googlebot/")
            ua.contains("bingbot") -> "Bingbot" to extractVersion(ua, "bingbot/")
            else -> null to null
        }

        // Detect rendering engine
        val (engine, engineVersion) = when {
            ua.contains("AppleWebKit") -> "WebKit" to extractVersion(ua, "AppleWebKit/")
            ua.contains("Gecko/") && ua.contains("Firefox") -> "Gecko" to extractVersion(ua, "rv:")
            ua.contains("Trident") -> "Trident" to extractVersion(ua, "Trident/")
            ua.contains("Presto") -> "Presto" to extractVersion(ua, "Presto/")
            else -> null to null
        }

        // Detect OS
        val (os, osVersion) = when {
            ua.contains("Windows NT 10.0") -> "Windows" to "10/11"
            ua.contains("Windows NT 6.3") -> "Windows" to "8.1"
            ua.contains("Windows NT 6.2") -> "Windows" to "8"
            ua.contains("Windows NT 6.1") -> "Windows" to "7"
            ua.contains("Windows NT 6.0") -> "Windows" to "Vista"
            ua.contains("Windows NT 5.1") -> "Windows" to "XP"
            ua.contains("Mac OS X") -> "macOS" to extractMacVersion(ua)
            ua.contains("iPhone OS") || ua.contains("CPU OS") -> "iOS" to extractIOSVersion(ua)
            ua.contains("Android") -> "Android" to extractVersion(ua, "Android ")
            ua.contains("Linux") -> "Linux" to null
            ua.contains("CrOS") -> "Chrome OS" to null
            ua.contains("Ubuntu") -> "Ubuntu" to null
            ua.contains("Fedora") -> "Fedora" to null
            else -> null to null
        }

        // Detect device type
        val isMobile = uaLower.contains("mobile") || uaLower.contains("android") ||
                uaLower.contains("iphone") || uaLower.contains("ipod") ||
                uaLower.contains("windows phone") || uaLower.contains("blackberry")

        val isTablet = uaLower.contains("tablet") || uaLower.contains("ipad") ||
                (uaLower.contains("android") && !uaLower.contains("mobile"))

        val deviceType = when {
            isBot -> "Bot/Crawler"
            isTablet -> "Tablet"
            isMobile -> "Mobile"
            else -> "Desktop"
        }

        // Detect specific device
        val device = when {
            ua.contains("iPhone") -> "iPhone"
            ua.contains("iPad") -> "iPad"
            ua.contains("Pixel") -> extractDeviceName(ua, "Pixel")
            ua.contains("SM-") -> extractDeviceName(ua, "SM-")
            ua.contains("Nexus") -> extractDeviceName(ua, "Nexus")
            ua.contains("SAMSUNG") -> "Samsung"
            ua.contains("Huawei") || ua.contains("HUAWEI") -> "Huawei"
            ua.contains("Xiaomi") -> "Xiaomi"
            ua.contains("OnePlus") -> "OnePlus"
            ua.contains("Macintosh") -> "Mac"
            else -> null
        }

        return ParsedUserAgent(
            browser = browser,
            browserVersion = browserVersion,
            engine = engine,
            engineVersion = engineVersion,
            os = os,
            osVersion = osVersion,
            deviceType = deviceType,
            device = device,
            isMobile = isMobile || isTablet,
            isBot = isBot
        )
    }

    fun parse() {
        if (userAgentInput.isNotBlank()) {
            parsedResult = parseUserAgent(userAgentInput.trim())
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.user_agent_parser_title)) },
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
            // Input field
            OutlinedTextField(
                value = userAgentInput,
                onValueChange = { userAgentInput = it },
                label = { Text("User Agent String") },
                placeholder = { Text("Paste user agent string here...") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5,
                leadingIcon = { Icon(Icons.Filled.Web, null) },
                trailingIcon = {
                    if (userAgentInput.isNotEmpty()) {
                        IconButton(onClick = { userAgentInput = ""; parsedResult = null }) {
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
                                userAgentInput = it.toString()
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.ContentPaste, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Paste")
                }
                OutlinedButton(
                    onClick = {
                        userAgentInput = defaultUserAgent
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Filled.PhoneAndroid, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("This Device")
                }
                Button(
                    onClick = { parse() },
                    modifier = Modifier.weight(1f),
                    enabled = userAgentInput.isNotBlank()
                ) {
                    Icon(Icons.Filled.Search, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Parse")
                }
            }

            // Sample user agents toggle
            TextButton(
                onClick = { showSamples = !showSamples },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    if (showSamples) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(4.dp))
                Text(if (showSamples) "Hide Sample User Agents" else "Show Sample User Agents")
            }

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Sample user agents
                if (showSamples) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    "Sample User Agents",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(8.dp))
                                sampleUserAgents.forEach { sample ->
                                    TextButton(
                                        onClick = {
                                            userAgentInput = sample.ua
                                            showSamples = false
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            sample.name,
                                            modifier = Modifier.fillMaxWidth(),
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Parsed results
                parsedResult?.let { result ->
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    when {
                                        result.isBot -> Icons.Filled.SmartToy
                                        result.deviceType == "Mobile" -> Icons.Filled.PhoneAndroid
                                        result.deviceType == "Tablet" -> Icons.Filled.Tablet
                                        else -> Icons.Filled.Computer
                                    },
                                    null,
                                    modifier = Modifier.size(40.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(Modifier.width(16.dp))
                                Column {
                                    Text(
                                        result.deviceType,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    result.device?.let {
                                        Text(
                                            it,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Browser info
                    item {
                        ResultCard(
                            title = "Browser",
                            icon = Icons.Filled.Language,
                            items = listOfNotNull(
                                result.browser?.let { "Name" to it },
                                result.browserVersion?.let { "Version" to it }
                            )
                        )
                    }

                    // Engine info
                    if (result.engine != null) {
                        item {
                            ResultCard(
                                title = "Rendering Engine",
                                icon = Icons.Filled.Settings,
                                items = listOfNotNull(
                                    result.engine.let { "Engine" to it },
                                    result.engineVersion?.let { "Version" to it }
                                )
                            )
                        }
                    }

                    // OS info
                    item {
                        ResultCard(
                            title = "Operating System",
                            icon = when (result.os) {
                                "Windows" -> Icons.Filled.DesktopWindows
                                "macOS" -> Icons.Filled.LaptopMac
                                "iOS" -> Icons.Filled.PhoneIphone
                                "Android" -> Icons.Filled.Android
                                "Linux" -> Icons.Filled.Terminal
                                else -> Icons.Filled.Devices
                            },
                            items = listOfNotNull(
                                result.os?.let { "Name" to it },
                                result.osVersion?.let { "Version" to it }
                            )
                        )
                    }

                    // Additional info
                    item {
                        ResultCard(
                            title = "Additional Info",
                            icon = Icons.Filled.Info,
                            items = listOf(
                                "Mobile Device" to if (result.isMobile) "Yes" else "No",
                                "Bot/Crawler" to if (result.isBot) "Yes" else "No"
                            )
                        )
                    }

                    // Copy result button
                    item {
                        OutlinedButton(
                            onClick = {
                                val text = buildString {
                                    appendLine("User Agent Analysis")
                                    appendLine("==================")
                                    appendLine("Device Type: ${result.deviceType}")
                                    result.device?.let { appendLine("Device: $it") }
                                    result.browser?.let { appendLine("Browser: $it ${result.browserVersion ?: ""}") }
                                    result.engine?.let { appendLine("Engine: $it ${result.engineVersion ?: ""}") }
                                    result.os?.let { appendLine("OS: $it ${result.osVersion ?: ""}") }
                                    appendLine("Mobile: ${if (result.isMobile) "Yes" else "No"}")
                                    appendLine("Bot: ${if (result.isBot) "Yes" else "No"}")
                                }
                                scope.launch {
                                    clipboard.setClipEntry(
                                        androidx.compose.ui.platform.ClipEntry(
                                            android.content.ClipData.newPlainText("ua_result", text)
                                        )
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.ContentCopy, null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Copy Analysis")
                        }
                    }
                }

                // Empty state
                if (parsedResult == null && !showSamples) {
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
                                Icon(
                                    Icons.Filled.Web,
                                    null,
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.height(16.dp))
                                Text(
                                    "Parse User Agent",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    "Paste a user agent string to identify\nbrowser, OS, device type, and more",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
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
private fun ResultCard(
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
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        value,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

private fun extractVersion(ua: String, prefix: String): String? {
    val index = ua.indexOf(prefix)
    if (index == -1) return null
    val start = index + prefix.length
    val end = ua.indexOfAny(charArrayOf(' ', ';', ')'), start).takeIf { it != -1 } ?: ua.length
    return ua.substring(start, end).takeIf { it.isNotEmpty() }
}

private fun extractMacVersion(ua: String): String? {
    val regex = Regex("""Mac OS X (\d+[._]\d+(?:[._]\d+)?)""")
    return regex.find(ua)?.groupValues?.get(1)?.replace('_', '.')
}

private fun extractIOSVersion(ua: String): String? {
    val regex = Regex("""(?:iPhone OS|CPU OS) (\d+[._]\d+(?:[._]\d+)?)""")
    return regex.find(ua)?.groupValues?.get(1)?.replace('_', '.')
}

private fun extractDeviceName(ua: String, prefix: String): String? {
    val index = ua.indexOf(prefix)
    if (index == -1) return null
    val start = index
    val end = ua.indexOfAny(charArrayOf(';', ')'), start).takeIf { it != -1 } ?: ua.length
    return ua.substring(start, end).trim().takeIf { it.isNotEmpty() }
}
