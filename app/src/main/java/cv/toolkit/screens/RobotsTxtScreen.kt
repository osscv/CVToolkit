package cv.toolkit.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

data class RobotsTxtResult(
    val url: String,
    val statusCode: Int,
    val rawContent: String,
    val userAgents: List<UserAgentBlock>,
    val sitemaps: List<String>,
    val crawlDelay: Int?,
    val responseTime: Long
)

data class UserAgentBlock(
    val userAgent: String,
    val allow: List<String>,
    val disallow: List<String>
)

data class RobotsTxtDirective(
    val type: DirectiveType,
    val value: String
)

enum class DirectiveType {
    USER_AGENT, ALLOW, DISALLOW, SITEMAP, CRAWL_DELAY, HOST, COMMENT, UNKNOWN
}

data class BotUserAgent(
    val name: String,
    val userAgent: String
)

data class PathTestResult(
    val path: String,
    val botName: String,
    val userAgent: String,
    // Rules validation
    val isAllowedByRules: Boolean,
    val matchedRule: String?,
    val ruleType: RuleType,
    // HTTP reachability
    val httpStatusCode: Int?,
    val responseTime: Long?,
    val isReachable: Boolean,
    val httpError: String?,
    // Response comparison
    val responseBodyHash: String?,
    val contentLength: Long?,
    val contentType: String?
)

enum class RuleType {
    ALLOW, DISALLOW, NO_RULE
}

data class CloakingDetectionResult(
    val path: String,
    val botName: String,
    val hasDifferentStatusCode: Boolean,
    val hasDifferentContent: Boolean,
    val botStatusCode: Int?,
    val regularStatusCode: Int?,
    val suspiciousDifferences: List<String>
)

data class BotTestingResult(
    val domain: String,
    val testedPaths: List<String>,
    val testedBots: List<BotUserAgent>,
    val pathTestResults: List<PathTestResult>,
    val cloakingResults: List<CloakingDetectionResult>,
    val sitemapTestResults: List<SitemapTestResult>,
    val totalTests: Int
)

data class SitemapTestResult(
    val sitemapUrl: String,
    val botName: String,
    val userAgent: String,
    val httpStatusCode: Int?,
    val responseTime: Long?,
    val isReachable: Boolean,
    val isValidXml: Boolean,
    val urlCount: Int?,
    val httpError: String?
)

private fun getDefaultBots(): List<BotUserAgent> = listOf(
    BotUserAgent("Googlebot", "Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)"),
    BotUserAgent("Bingbot", "Mozilla/5.0 (compatible; bingbot/2.0; +http://www.bing.com/bingbot.htm)"),
    BotUserAgent("curl", "curl/8.4.0"),
    BotUserAgent("Postman", "PostmanRuntime/7.36.0")
)

private fun extractBotName(userAgent: String): String {
    return when {
        userAgent.contains("Googlebot", ignoreCase = true) -> "Googlebot"
        userAgent.contains("bingbot", ignoreCase = true) -> "Bingbot"
        userAgent.contains("curl", ignoreCase = true) -> "curl"
        userAgent.contains("Postman", ignoreCase = true) -> "Postman"
        else -> "*"
    }
}

private fun pathMatchesRule(path: String, rule: String): Boolean {
    if (rule.isEmpty()) return false
    if (rule == "/") return true

    // Convert robots.txt wildcards to regex
    val regexPattern = rule
        .replace(".", "\\.")
        .replace("*", ".*")
        .replace("$", "\\$")

    return path.matches(Regex("^$regexPattern"))
}

private fun validatePathAgainstRules(
    path: String,
    userAgent: String,
    robotsResult: RobotsTxtResult
): PathTestResult {
    // Find matching user-agent block (specific bot or *)
    val botName = extractBotName(userAgent)
    val matchingBlock = robotsResult.userAgents.find {
        it.userAgent.equals(botName, ignoreCase = true)
    } ?: robotsResult.userAgents.find { it.userAgent == "*" }

    if (matchingBlock == null) {
        return PathTestResult(
            path, botName, userAgent,
            isAllowedByRules = true, matchedRule = null, ruleType = RuleType.NO_RULE,
            httpStatusCode = null, responseTime = null, isReachable = false,
            httpError = null, responseBodyHash = null, contentLength = null, contentType = null
        )
    }

    // Check Allow rules first (more specific = longer paths first)
    val allowRules = matchingBlock.allow.sortedByDescending { it.length }
    for (rule in allowRules) {
        if (pathMatchesRule(path, rule)) {
            return PathTestResult(
                path, botName, userAgent,
                isAllowedByRules = true, matchedRule = rule, ruleType = RuleType.ALLOW,
                httpStatusCode = null, responseTime = null, isReachable = false,
                httpError = null, responseBodyHash = null, contentLength = null, contentType = null
            )
        }
    }

    // Check Disallow rules
    val disallowRules = matchingBlock.disallow.sortedByDescending { it.length }
    for (rule in disallowRules) {
        if (pathMatchesRule(path, rule)) {
            return PathTestResult(
                path, botName, userAgent,
                isAllowedByRules = false, matchedRule = rule, ruleType = RuleType.DISALLOW,
                httpStatusCode = null, responseTime = null, isReachable = false,
                httpError = null, responseBodyHash = null, contentLength = null, contentType = null
            )
        }
    }

    // No matching rule = allowed by default
    return PathTestResult(
        path, botName, userAgent,
        isAllowedByRules = true, matchedRule = null, ruleType = RuleType.NO_RULE,
        httpStatusCode = null, responseTime = null, isReachable = false,
        httpError = null, responseBodyHash = null, contentLength = null, contentType = null
    )
}

private fun hashString(input: String): String {
    val bytes = java.security.MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
    return bytes.joinToString("") { "%02x".format(it) }
}

private suspend fun testHttpReachability(
    url: String,
    userAgent: String,
    timeout: Int = 10000
): PathTestResult {
    val startTime = System.currentTimeMillis()
    val botName = extractBotName(userAgent)

    return withContext(Dispatchers.IO) {
        try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = timeout
            connection.readTimeout = timeout
            connection.setRequestProperty("User-Agent", userAgent)
            connection.instanceFollowRedirects = true

            connection.connect()
            val responseTime = System.currentTimeMillis() - startTime
            val statusCode = connection.responseCode

            // Read response body for hashing
            val responseBody = try {
                connection.inputStream.bufferedReader().use { it.readText() }
            } catch (e: Exception) {
                ""
            }

            val contentLength = connection.contentLengthLong
            val contentType = connection.contentType

            connection.disconnect()

            PathTestResult(
                path = url,
                botName = botName,
                userAgent = userAgent,
                isAllowedByRules = false, // Will be merged later
                matchedRule = null,
                ruleType = RuleType.NO_RULE,
                httpStatusCode = statusCode,
                responseTime = responseTime,
                isReachable = statusCode in 200..299,
                httpError = null,
                responseBodyHash = hashString(responseBody),
                contentLength = contentLength,
                contentType = contentType
            )
        } catch (e: Exception) {
            PathTestResult(
                path = url,
                botName = botName,
                userAgent = userAgent,
                isAllowedByRules = false,
                matchedRule = null,
                ruleType = RuleType.NO_RULE,
                httpStatusCode = null,
                responseTime = System.currentTimeMillis() - startTime,
                isReachable = false,
                httpError = e.message ?: "Unknown error",
                responseBodyHash = null,
                contentLength = null,
                contentType = null
            )
        }
    }
}

private fun detectCloaking(
    botResult: PathTestResult,
    regularResult: PathTestResult
): CloakingDetectionResult {
    val differences = mutableListOf<String>()

    val statusDiff = botResult.httpStatusCode != regularResult.httpStatusCode
    if (statusDiff) {
        differences.add("Status: ${botResult.httpStatusCode} vs ${regularResult.httpStatusCode}")
    }

    val contentDiff = botResult.responseBodyHash != regularResult.responseBodyHash
    if (contentDiff && botResult.responseBodyHash != null && regularResult.responseBodyHash != null) {
        differences.add("Content differs (different hash)")
    }

    return CloakingDetectionResult(
        path = botResult.path,
        botName = botResult.botName,
        hasDifferentStatusCode = statusDiff,
        hasDifferentContent = contentDiff,
        botStatusCode = botResult.httpStatusCode,
        regularStatusCode = regularResult.httpStatusCode,
        suspiciousDifferences = differences
    )
}

private suspend fun testSitemapWithBot(
    sitemapUrl: String,
    userAgent: String,
    timeout: Int = 15000
): SitemapTestResult {
    val startTime = System.currentTimeMillis()
    val botName = extractBotName(userAgent)

    return withContext(Dispatchers.IO) {
        try {
            val connection = URL(sitemapUrl).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = timeout
            connection.readTimeout = timeout
            connection.setRequestProperty("User-Agent", userAgent)
            connection.setRequestProperty("Accept", "application/xml, text/xml, */*")
            connection.instanceFollowRedirects = true

            connection.connect()
            val responseTime = System.currentTimeMillis() - startTime
            val statusCode = connection.responseCode

            // Read response body to check if it's valid XML
            val responseBody = try {
                connection.inputStream.bufferedReader().use { it.readText() }
            } catch (e: Exception) {
                ""
            }

            connection.disconnect()

            // Check if it's valid XML sitemap
            val isValidXml = responseBody.contains("<urlset") ||
                            responseBody.contains("<sitemapindex") ||
                            responseBody.contains("<?xml")

            // Count URLs in sitemap
            val urlCount = if (isValidXml) {
                "<loc>".toRegex().findAll(responseBody).count()
            } else null

            SitemapTestResult(
                sitemapUrl = sitemapUrl,
                botName = botName,
                userAgent = userAgent,
                httpStatusCode = statusCode,
                responseTime = responseTime,
                isReachable = statusCode in 200..299,
                isValidXml = isValidXml,
                urlCount = urlCount,
                httpError = null
            )
        } catch (e: Exception) {
            SitemapTestResult(
                sitemapUrl = sitemapUrl,
                botName = botName,
                userAgent = userAgent,
                httpStatusCode = null,
                responseTime = System.currentTimeMillis() - startTime,
                isReachable = false,
                isValidXml = false,
                urlCount = null,
                httpError = e.message ?: "Unknown error"
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RobotsTxtScreen(navController: NavController) {
    var urlInput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<RobotsTxtResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showRawContent by remember { mutableStateOf(false) }
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    // Bot testing state variables
    var showBotTesting by remember { mutableStateOf(false) }
    var selectedBots by remember { mutableStateOf(getDefaultBots()) }
    var testPaths by remember { mutableStateOf(listOf("/", "/api", "/admin")) }
    var newPathInput by remember { mutableStateOf("") }
    var botTestingResult by remember { mutableStateOf<BotTestingResult?>(null) }
    var isTesting by remember { mutableStateOf(false) }
    var testProgress by remember { mutableFloatStateOf(0f) }
    var enableCloakingDetection by remember { mutableStateOf(true) }
    var enableSitemapTesting by remember { mutableStateOf(true) }

    fun fetchRobotsTxt() {
        if (urlInput.isBlank()) return

        var domain = urlInput.trim()
        // Clean up the URL to get just the domain
        domain = domain.removePrefix("http://").removePrefix("https://")
        domain = domain.split("/").firstOrNull() ?: domain

        val robotsUrl = "https://$domain/robots.txt"

        isLoading = true
        errorMessage = null
        result = null

        scope.launch {
            try {
                val fetchResult = withContext(Dispatchers.IO) {
                    fetchAndParseRobotsTxt(robotsUrl)
                }
                result = fetchResult
            } catch (e: Exception) {
                errorMessage = "Failed to fetch robots.txt: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    fun startBotTesting() {
        if (result == null || selectedBots.isEmpty() || testPaths.isEmpty()) return

        isTesting = true
        testProgress = 0f
        botTestingResult = null

        scope.launch {
            try {
                val domain = result!!.url.removePrefix("https://").removePrefix("http://").split("/").first()
                val baseUrl = "https://$domain"

                val allResults = mutableListOf<PathTestResult>()
                val cloakingResults = mutableListOf<CloakingDetectionResult>()
                val sitemapResults = mutableListOf<SitemapTestResult>()

                // Calculate total tests including sitemap tests
                val sitemapsToTest = if (enableSitemapTesting) result!!.sitemaps else emptyList()
                val pathTests = selectedBots.size * testPaths.size * (if (enableCloakingDetection) 2 else 1)
                val sitemapTests = if (enableSitemapTesting) selectedBots.size * sitemapsToTest.size else 0
                val totalTests = pathTests + sitemapTests
                var completedTests = 0

                // Test each path with each bot
                for (path in testPaths) {
                    val fullUrl = baseUrl + path

                    for (bot in selectedBots) {
                        // 1. Validate against robots.txt rules
                        val rulesResult = validatePathAgainstRules(path, bot.userAgent, result!!)

                        // 2. Test HTTP reachability with bot UA
                        val botHttpResult = testHttpReachability(fullUrl, bot.userAgent)

                        // Merge results
                        val mergedBotResult = botHttpResult.copy(
                            isAllowedByRules = rulesResult.isAllowedByRules,
                            matchedRule = rulesResult.matchedRule,
                            ruleType = rulesResult.ruleType
                        )
                        allResults.add(mergedBotResult)

                        completedTests++
                        testProgress = completedTests.toFloat() / totalTests

                        // 3. Cloaking detection (if enabled)
                        if (enableCloakingDetection) {
                            val regularUA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"
                            val regularHttpResult = testHttpReachability(fullUrl, regularUA)

                            completedTests++
                            testProgress = completedTests.toFloat() / totalTests

                            val cloakingResult = detectCloaking(mergedBotResult, regularHttpResult)
                            if (cloakingResult.hasDifferentStatusCode || cloakingResult.hasDifferentContent) {
                                cloakingResults.add(cloakingResult)
                            }
                        }
                    }
                }

                // Test sitemaps with each bot (if enabled)
                if (enableSitemapTesting && sitemapsToTest.isNotEmpty()) {
                    for (sitemapUrl in sitemapsToTest) {
                        for (bot in selectedBots) {
                            val sitemapResult = testSitemapWithBot(sitemapUrl, bot.userAgent)
                            sitemapResults.add(sitemapResult)

                            completedTests++
                            testProgress = completedTests.toFloat() / totalTests
                        }
                    }
                }

                botTestingResult = BotTestingResult(
                    domain = domain,
                    testedPaths = testPaths,
                    testedBots = selectedBots,
                    pathTestResults = allResults,
                    cloakingResults = cloakingResults,
                    sitemapTestResults = sitemapResults,
                    totalTests = totalTests
                )
            } catch (e: Exception) {
                errorMessage = "Bot testing failed: ${e.message}"
            } finally {
                isTesting = false
                testProgress = 0f
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Robots.txt Analyzer") },
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Input field
            item {
                OutlinedTextField(
                    value = urlInput,
                    onValueChange = { urlInput = it },
                    label = { Text("Domain") },
                    placeholder = { Text("example.com") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Filled.Language, null) },
                    trailingIcon = {
                        if (urlInput.isNotEmpty()) {
                            IconButton(onClick = { urlInput = "" }) {
                                Icon(Icons.Filled.Clear, "Clear")
                            }
                        }
                    }
                )
            }

            // Action buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            scope.launch {
                                clipboard.getClipEntry()?.clipData?.getItemAt(0)?.text?.let {
                                    urlInput = it.toString()
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
                        onClick = { fetchRobotsTxt() },
                        modifier = Modifier.weight(1f),
                        enabled = urlInput.isNotBlank() && !isLoading
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Icon(Icons.AutoMirrored.Filled.Send, null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(4.dp))
                        Text("Fetch")
                    }
                }
            }

            // Error message
            errorMessage?.let {
                item {
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
            }

            // Results
            result?.let { robotsResult ->
                // Status card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = when {
                                robotsResult.statusCode == 200 -> MaterialTheme.colorScheme.primaryContainer
                                robotsResult.statusCode == 404 -> MaterialTheme.colorScheme.tertiaryContainer
                                else -> MaterialTheme.colorScheme.errorContainer
                            }
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "HTTP ${robotsResult.statusCode}",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    if (robotsResult.statusCode == 200) "robots.txt found" else "Not found",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    "${robotsResult.responseTime}ms",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    "Response Time",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                }

                // Toggle raw/parsed view
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = !showRawContent,
                            onClick = { showRawContent = false },
                            label = { Text("Parsed") },
                            leadingIcon = if (!showRawContent) {
                                { Icon(Icons.Filled.Check, null, modifier = Modifier.size(18.dp)) }
                            } else null
                        )
                        FilterChip(
                            selected = showRawContent,
                            onClick = { showRawContent = true },
                            label = { Text("Raw") },
                            leadingIcon = if (showRawContent) {
                                { Icon(Icons.Filled.Check, null, modifier = Modifier.size(18.dp)) }
                            } else null
                        )
                        Spacer(Modifier.weight(1f))
                        IconButton(
                            onClick = {
                                scope.launch {
                                    clipboard.setClipEntry(
                                        androidx.compose.ui.platform.ClipEntry(
                                            android.content.ClipData.newPlainText("robots.txt", robotsResult.rawContent)
                                        )
                                    )
                                }
                            }
                        ) {
                            Icon(Icons.Filled.ContentCopy, "Copy", modifier = Modifier.size(20.dp))
                        }
                    }
                }

                // Content - Summary
                item {
                    SummaryCard(robotsResult)
                }

                // Raw content view
                if (showRawContent) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Text(
                                robotsResult.rawContent.ifEmpty { "No content" },
                                style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier
                                    .padding(12.dp)
                                    .horizontalScroll(rememberScrollState())
                            )
                        }
                    }
                } else {
                    // Parsed view - Sitemaps
                    if (robotsResult.sitemaps.isNotEmpty()) {
                        item {
                            Text(
                                "Sitemaps (${robotsResult.sitemaps.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        items(robotsResult.sitemaps) { sitemap ->
                            SitemapItem(sitemap) {
                                scope.launch {
                                    clipboard.setClipEntry(
                                        androidx.compose.ui.platform.ClipEntry(
                                            android.content.ClipData.newPlainText("sitemap", sitemap)
                                        )
                                    )
                                }
                            }
                        }
                    }

                    // User Agent Blocks
                    if (robotsResult.userAgents.isNotEmpty()) {
                        item {
                            Text(
                                "User-Agent Rules (${robotsResult.userAgents.size})",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        items(robotsResult.userAgents) { block ->
                            UserAgentCard(block)
                        }
                    }
                }
            }

            // Bot Testing Section
            if (result != null) {
                item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Filled.SmartToy, null, modifier = Modifier.size(24.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Bot Testing",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            IconButton(onClick = { showBotTesting = !showBotTesting }) {
                                Icon(
                                    if (showBotTesting) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                    null
                                )
                            }
                        }

                        if (showBotTesting) {
                            Spacer(Modifier.height(12.dp))

                            // Bot selection
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        "Select Bots to Test",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(12.dp))

                                    getDefaultBots().forEach { bot ->
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Checkbox(
                                                checked = selectedBots.contains(bot),
                                                onCheckedChange = { checked ->
                                                    selectedBots = if (checked) selectedBots + bot else selectedBots - bot
                                                }
                                            )
                                            Spacer(Modifier.width(8.dp))
                                            Column {
                                                Text(bot.name, style = MaterialTheme.typography.bodyMedium)
                                                Text(
                                                    bot.userAgent,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    maxLines = 1,
                                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            // Path input
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        "Paths to Test (${testPaths.size})",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(8.dp))

                                    // Selected paths
                                    if (testPaths.isNotEmpty()) {
                                        testPaths.forEach { path ->
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    path,
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    fontFamily = FontFamily.Monospace,
                                                    modifier = Modifier.weight(1f),
                                                    maxLines = 1,
                                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                                )
                                                IconButton(
                                                    onClick = { testPaths = testPaths - path },
                                                    modifier = Modifier.size(24.dp)
                                                ) {
                                                    Icon(Icons.Filled.Close, "Remove", modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        }
                                        Spacer(Modifier.height(8.dp))
                                    }

                                    // Custom path input
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedTextField(
                                            value = newPathInput,
                                            onValueChange = { newPathInput = it },
                                            label = { Text("Custom Path") },
                                            placeholder = { Text("/custom/path") },
                                            modifier = Modifier.weight(1f),
                                            singleLine = true,
                                            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        IconButton(
                                            onClick = {
                                                if (newPathInput.isNotBlank()) {
                                                    val pathToAdd = if (newPathInput.startsWith("/")) newPathInput else "/$newPathInput"
                                                    if (!testPaths.contains(pathToAdd)) {
                                                        testPaths = testPaths + pathToAdd
                                                    }
                                                    newPathInput = ""
                                                }
                                            },
                                            enabled = newPathInput.isNotBlank()
                                        ) {
                                            Icon(Icons.Filled.Add, "Add Path")
                                        }
                                    }

                                    Spacer(Modifier.height(12.dp))

                                    // Quick Add - Common paths
                                    Text(
                                        "Common Paths:",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Spacer(Modifier.height(4.dp))
                                    androidx.compose.foundation.layout.FlowRow(
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        listOf("/", "/api", "/admin", "/wp-admin", "/sitemap.xml", "/login", "/search").forEach { commonPath ->
                                            if (!testPaths.contains(commonPath)) {
                                                FilterChip(
                                                    selected = false,
                                                    onClick = { testPaths = testPaths + commonPath },
                                                    label = { Text(commonPath, style = MaterialTheme.typography.labelSmall) }
                                                )
                                            }
                                        }
                                    }

                                    // Paths from robots.txt
                                    val robotsPaths = result?.userAgents?.flatMap { it.allow + it.disallow }?.distinct()?.filter { it.isNotBlank() } ?: emptyList()
                                    if (robotsPaths.isNotEmpty()) {
                                        Spacer(Modifier.height(12.dp))
                                        Text(
                                            "From Robots.txt (${robotsPaths.size}):",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        androidx.compose.foundation.layout.FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            robotsPaths.take(10).forEach { robotsPath ->
                                                if (!testPaths.contains(robotsPath)) {
                                                    FilterChip(
                                                        selected = false,
                                                        onClick = { testPaths = testPaths + robotsPath },
                                                        label = {
                                                            Text(
                                                                if (robotsPath.length > 20) robotsPath.take(20) + "..." else robotsPath,
                                                                style = MaterialTheme.typography.labelSmall
                                                            )
                                                        }
                                                    )
                                                }
                                            }
                                            if (robotsPaths.size > 10) {
                                                FilterChip(
                                                    selected = false,
                                                    onClick = {
                                                        testPaths = (testPaths + robotsPaths).distinct()
                                                    },
                                                    label = { Text("Add All (${robotsPaths.size})", style = MaterialTheme.typography.labelSmall) }
                                                )
                                            }
                                        }
                                    }

                                    // Paths from sitemaps
                                    val sitemapPaths = result?.sitemaps?.mapNotNull { sitemap ->
                                        try {
                                            java.net.URL(sitemap).path.takeIf { it.isNotBlank() }
                                        } catch (e: Exception) { null }
                                    }?.distinct() ?: emptyList()
                                    if (sitemapPaths.isNotEmpty()) {
                                        Spacer(Modifier.height(12.dp))
                                        Text(
                                            "From Sitemaps (${sitemapPaths.size}):",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        androidx.compose.foundation.layout.FlowRow(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            sitemapPaths.forEach { sitemapPath ->
                                                if (!testPaths.contains(sitemapPath)) {
                                                    FilterChip(
                                                        selected = false,
                                                        onClick = { testPaths = testPaths + sitemapPath },
                                                        label = {
                                                            Text(
                                                                if (sitemapPath.length > 20) sitemapPath.take(20) + "..." else sitemapPath,
                                                                style = MaterialTheme.typography.labelSmall
                                                            )
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    // Clear all button
                                    if (testPaths.isNotEmpty()) {
                                        Spacer(Modifier.height(8.dp))
                                        TextButton(
                                            onClick = { testPaths = emptyList() },
                                            modifier = Modifier.align(Alignment.End)
                                        ) {
                                            Icon(Icons.Filled.DeleteSweep, null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text("Clear All")
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            // Options
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        "Test Options",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(Modifier.height(8.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = enableCloakingDetection,
                                            onCheckedChange = { enableCloakingDetection = it }
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                "Cloaking detection",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Text(
                                                "Compare bot vs regular user agent responses",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }

                                    val hasSitemaps = result?.sitemaps?.isNotEmpty() == true
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Checkbox(
                                            checked = enableSitemapTesting && hasSitemaps,
                                            onCheckedChange = { enableSitemapTesting = it },
                                            enabled = hasSitemaps
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                "Sitemap testing",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = if (hasSitemaps) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                if (hasSitemaps) "Test ${result?.sitemaps?.size} sitemap(s) with each bot"
                                                else "No sitemaps found in robots.txt",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }

                            Spacer(Modifier.height(12.dp))

                            // Start test button
                            Button(
                                onClick = { startBotTesting() },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !isTesting && selectedBots.isNotEmpty() && testPaths.isNotEmpty()
                            ) {
                                if (isTesting) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("Testing... ${(testProgress * 100).toInt()}%")
                                } else {
                                    Icon(Icons.Filled.PlayArrow, null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Start Bot Testing")
                                }
                            }

                            // Progress indicator
                            if (isTesting) {
                                Spacer(Modifier.height(8.dp))
                                LinearProgressIndicator(
                                    progress = { testProgress },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }

                            // Results
                            botTestingResult?.let { testResult ->
                                Spacer(Modifier.height(16.dp))
                                BotTestResultsCard(testResult)
                            }
                        }
                    }
                }
                }
            }

            // Empty state
            if (result == null && errorMessage == null && !isLoading) {
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
                                Icons.Filled.SmartToy,
                                null,
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "Enter a domain",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Fetch and analyze robots.txt directives",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Loading state
            if (isLoading) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(16.dp))
                            Text("Fetching robots.txt...", style = MaterialTheme.typography.bodyMedium)
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

@Composable
private fun SummaryCard(result: RobotsTxtResult) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                "Summary",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SummaryItem("User-Agents", result.userAgents.size.toString())
                SummaryItem("Sitemaps", result.sitemaps.size.toString())
                SummaryItem("Crawl-Delay", result.crawlDelay?.toString() ?: "N/A")
            }
        }
    }
}

@Composable
private fun SummaryItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SitemapItem(sitemap: String, onCopy: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Filled.Map,
                null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.tertiary
            )
            Spacer(Modifier.width(8.dp))
            Text(
                sitemap,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState())
            )
            IconButton(onClick = onCopy, modifier = Modifier.size(24.dp)) {
                Icon(Icons.Filled.ContentCopy, "Copy", modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun UserAgentCard(block: UserAgentBlock) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.SmartToy,
                    null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    block.userAgent,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    fontFamily = FontFamily.Monospace
                )
            }

            if (block.allow.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Allow (${block.allow.size})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary,
                    fontWeight = FontWeight.Medium
                )
                block.allow.forEach { path ->
                    Text(
                        "  $path",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            if (block.disallow.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Disallow (${block.disallow.size})",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Medium
                )
                block.disallow.forEach { path ->
                    Text(
                        "  $path",
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun BotTestResultsCard(
    result: BotTestingResult
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val hasSitemapResults = result.sitemapTestResults.isNotEmpty()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "Test Results",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SummaryItem("Tests", result.totalTests.toString())
                SummaryItem("Paths", result.testedPaths.size.toString())
                SummaryItem("Bots", result.testedBots.size.toString())
                if (hasSitemapResults) {
                    SummaryItem("Sitemaps", (result.sitemapTestResults.size / result.testedBots.size).toString())
                }
            }

            Spacer(Modifier.height(16.dp))

            PrimaryScrollableTabRow(
                selectedTabIndex = selectedTab,
                edgePadding = 0.dp
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Rules") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Reachability") }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Cloaking") }
                )
                if (hasSitemapResults) {
                    Tab(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        text = { Text("Sitemaps") }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            when (selectedTab) {
                0 -> RulesValidationView(result.pathTestResults)
                1 -> ReachabilityView(result.pathTestResults)
                2 -> CloakingDetectionView(result.cloakingResults)
                3 -> SitemapTestResultsView(result.sitemapTestResults)
            }
        }
    }
}

@Composable
private fun RulesValidationView(results: List<PathTestResult>) {
    LazyColumn(
        modifier = Modifier.heightIn(max = 400.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(results) { result ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        result.isAllowedByRules -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                        else -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                    }
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            result.path,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            if (result.isAllowedByRules) Icons.Filled.CheckCircle else Icons.Filled.Block,
                            null,
                            tint = if (result.isAllowedByRules)
                                MaterialTheme.colorScheme.tertiary
                            else
                                MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Bot: ${result.botName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    result.matchedRule?.let {
                        Text(
                            "Rule: ${result.ruleType.name} $it",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReachabilityView(results: List<PathTestResult>) {
    LazyColumn(
        modifier = Modifier.heightIn(max = 400.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(results.filter { it.httpStatusCode != null }) { result ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        result.isReachable -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                        result.httpStatusCode in 300..399 -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                        else -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                    }
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                result.path,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                "Bot: ${result.botName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "HTTP ${result.httpStatusCode}",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                            result.responseTime?.let {
                                Text(
                                    "${it}ms",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }
                    result.httpError?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Error: $it",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CloakingDetectionView(results: List<CloakingDetectionResult>) {
    if (results.isEmpty()) {
        Text(
            "No cloaking detected",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    LazyColumn(
        modifier = Modifier.heightIn(max = 400.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(results.filter {
            it.hasDifferentStatusCode || it.hasDifferentContent
        }) { result ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.Warning,
                            null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Cloaking Detected",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Path: ${result.path}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        "Bot: ${result.botName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Differences:",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium
                    )
                    result.suspiciousDifferences.forEach { diff ->
                        Text(
                            "• $diff",
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(start = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SitemapTestResultsView(results: List<SitemapTestResult>) {
    if (results.isEmpty()) {
        Text(
            "No sitemap tests performed",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    LazyColumn(
        modifier = Modifier.heightIn(max = 400.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(results) { result ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        result.isReachable && result.isValidXml -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                        result.isReachable && !result.isValidXml -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                        else -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                    }
                )
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                result.sitemapUrl.substringAfterLast("/"),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            Text(
                                "Bot: ${result.botName}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (result.isReachable) {
                                    Icon(
                                        Icons.Filled.CheckCircle,
                                        null,
                                        tint = MaterialTheme.colorScheme.tertiary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                } else {
                                    Icon(
                                        Icons.Filled.Error,
                                        null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "HTTP ${result.httpStatusCode ?: "N/A"}",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            result.responseTime?.let {
                                Text(
                                    "${it}ms",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(8.dp))

                    // Sitemap details
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                if (result.isValidXml) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                                null,
                                tint = if (result.isValidXml) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(Modifier.width(4.dp))
                            Text(
                                if (result.isValidXml) "Valid XML" else "Invalid XML",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                        result.urlCount?.let { count ->
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.Link,
                                    null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "$count URLs",
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        }
                    }

                    result.httpError?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Error: $it",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

private fun fetchAndParseRobotsTxt(urlString: String): RobotsTxtResult {
    val startTime = System.currentTimeMillis()
    val url = URL(urlString)
    val connection = url.openConnection() as HttpURLConnection

    connection.requestMethod = "GET"
    connection.connectTimeout = 15000
    connection.readTimeout = 15000
    connection.setRequestProperty("User-Agent", "CVToolkit/1.0")

    try {
        connection.connect()
        val responseTime = System.currentTimeMillis() - startTime
        val statusCode = connection.responseCode

        val rawContent = if (statusCode == 200) {
            connection.inputStream.bufferedReader().use { it.readText() }
        } else {
            ""
        }

        // Parse the robots.txt content
        val userAgents = mutableListOf<UserAgentBlock>()
        val sitemaps = mutableListOf<String>()
        var crawlDelay: Int? = null

        var currentUserAgent: String? = null
        var currentAllow = mutableListOf<String>()
        var currentDisallow = mutableListOf<String>()

        rawContent.lines().forEach { line ->
            val trimmedLine = line.trim()
            if (trimmedLine.isEmpty() || trimmedLine.startsWith("#")) return@forEach

            val colonIndex = trimmedLine.indexOf(':')
            if (colonIndex == -1) return@forEach

            val directive = trimmedLine.substring(0, colonIndex).trim().lowercase()
            val value = trimmedLine.substring(colonIndex + 1).trim()

            when (directive) {
                "user-agent" -> {
                    // Save previous block if exists
                    if (currentUserAgent != null) {
                        userAgents.add(UserAgentBlock(currentUserAgent!!, currentAllow.toList(), currentDisallow.toList()))
                    }
                    currentUserAgent = value
                    currentAllow = mutableListOf()
                    currentDisallow = mutableListOf()
                }
                "allow" -> {
                    if (currentUserAgent != null && value.isNotEmpty()) {
                        currentAllow.add(value)
                    }
                }
                "disallow" -> {
                    if (currentUserAgent != null && value.isNotEmpty()) {
                        currentDisallow.add(value)
                    }
                }
                "sitemap" -> {
                    if (value.isNotEmpty()) {
                        sitemaps.add(value)
                    }
                }
                "crawl-delay" -> {
                    crawlDelay = value.toIntOrNull()
                }
            }
        }

        // Save last block
        if (currentUserAgent != null) {
            userAgents.add(UserAgentBlock(currentUserAgent!!, currentAllow.toList(), currentDisallow.toList()))
        }

        return RobotsTxtResult(
            url = urlString,
            statusCode = statusCode,
            rawContent = rawContent,
            userAgents = userAgents,
            sitemaps = sitemaps,
            crawlDelay = crawlDelay,
            responseTime = responseTime
        )
    } finally {
        connection.disconnect()
    }
}
