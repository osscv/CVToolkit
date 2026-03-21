package cv.toolkit.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ManageSearch
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
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.Socket
import java.net.InetAddress

data class WhoisResult(
    val domain: String,
    val rawData: String,
    val registrar: String?,
    val creationDate: String?,
    val expirationDate: String?,
    val updatedDate: String?,
    val nameServers: List<String>,
    val status: List<String>,
    val registrantOrg: String?,
    val registrantCountry: String?
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WhoisLookupScreen(navController: NavController) {
    var domainInput by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var whoisResult by remember { mutableStateOf<WhoisResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showRawData by remember { mutableStateOf(false) }
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    fun performWhoisLookup() {
        if (domainInput.isBlank()) return

        val domain = domainInput.trim()
            .removePrefix("http://")
            .removePrefix("https://")
            .removePrefix("www.")
            .split("/").first()
            .lowercase()

        isLoading = true
        errorMessage = null
        whoisResult = null

        scope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    queryWhois(domain)
                }
                whoisResult = result
            } catch (e: Exception) {
                errorMessage = "Failed to lookup domain: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Whois Lookup") },
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
                value = domainInput,
                onValueChange = { domainInput = it },
                label = { Text("Domain Name") },
                placeholder = { Text("example.com") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Language, null) },
                trailingIcon = {
                    if (domainInput.isNotEmpty()) {
                        IconButton(onClick = { domainInput = "" }) {
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
                                domainInput = it.toString()
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
                    onClick = { performWhoisLookup() },
                    modifier = Modifier.weight(1f),
                    enabled = domainInput.isNotBlank() && !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    } else {
                        Icon(Icons.Filled.Search, null, modifier = Modifier.size(18.dp))
                    }
                    Spacer(Modifier.width(4.dp))
                    Text("Lookup")
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

            // Results
            whoisResult?.let { result ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Domain info card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Filled.Domain,
                                    null,
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    result.domain,
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }

                    // Registration details
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Registration Details",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Spacer(Modifier.height(12.dp))

                            result.registrar?.let {
                                WhoisInfoRow("Registrar", it)
                            }
                            result.creationDate?.let {
                                WhoisInfoRow("Created", it)
                            }
                            result.expirationDate?.let {
                                WhoisInfoRow("Expires", it)
                            }
                            result.updatedDate?.let {
                                WhoisInfoRow("Updated", it)
                            }
                            result.registrantOrg?.let {
                                WhoisInfoRow("Organization", it)
                            }
                            result.registrantCountry?.let {
                                WhoisInfoRow("Country", it)
                            }
                        }
                    }

                    // Name servers
                    if (result.nameServers.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Name Servers",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.height(8.dp))
                                result.nameServers.forEach { ns ->
                                    Row(
                                        modifier = Modifier.padding(vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Filled.Dns,
                                            null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            ns,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Status
                    if (result.status.isNotEmpty()) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Domain Status",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Spacer(Modifier.height(8.dp))
                                result.status.forEach { status ->
                                    val statusName = status.split(" ").first()
                                    Surface(
                                        color = MaterialTheme.colorScheme.secondaryContainer,
                                        shape = MaterialTheme.shapes.small,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    ) {
                                        Text(
                                            statusName,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Raw data toggle
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
                                    "Raw WHOIS Data",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Row {
                                    IconButton(
                                        onClick = {
                                            scope.launch {
                                                clipboard.setClipEntry(
                                                    androidx.compose.ui.platform.ClipEntry(
                                                        android.content.ClipData.newPlainText("whois", result.rawData)
                                                    )
                                                )
                                            }
                                        }
                                    ) {
                                        Icon(Icons.Filled.ContentCopy, "Copy", modifier = Modifier.size(20.dp))
                                    }
                                    IconButton(onClick = { showRawData = !showRawData }) {
                                        Icon(
                                            if (showRawData) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                                            if (showRawData) "Collapse" else "Expand",
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                            if (showRawData) {
                                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                Text(
                                    result.rawData,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                }
            }

            // Empty state
            if (whoisResult == null && errorMessage == null && !isLoading) {
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
                            Icons.AutoMirrored.Filled.ManageSearch,
                            null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Enter a Domain Name",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Get registration info, name servers, and more",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Loading state
            if (isLoading) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(16.dp))
                        Text("Looking up domain...", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            BannerAd(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun WhoisInfoRow(label: String, value: String) {
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
            fontWeight = FontWeight.Medium
        )
    }
}

private fun queryWhois(domain: String): WhoisResult {
    val tld = domain.substringAfterLast(".")
    var whoisServer = getWhoisServer(tld)

    val rawData = StringBuilder()

    // Try primary server first, fallback to IANA referral if it fails
    try {
        rawData.append(queryWhoisServer(whoisServer, domain))
    } catch (e: Exception) {
        // If primary server fails, try to get referral from IANA
        try {
            val ianaResponse = queryWhoisServer("whois.iana.org", tld)
            val referralServer = extractReferralServer(ianaResponse)
            if (referralServer != null && referralServer != whoisServer) {
                whoisServer = referralServer
                rawData.append(queryWhoisServer(whoisServer, domain))
            } else {
                throw e // Re-throw original exception if no referral found
            }
        } catch (e2: Exception) {
            throw e // Throw original exception
        }
    }

    // Check if we need to follow a referral in the response (for verisign, etc.)
    val data = rawData.toString()
    val referral = extractReferralServer(data)
    if (referral != null && referral != whoisServer && !data.contains("Registrar:")) {
        try {
            val detailedData = queryWhoisServer(referral, domain)
            rawData.clear()
            rawData.append(detailedData)
        } catch (e: Exception) {
            // Keep original data if referral fails
        }
    }

    val finalData = rawData.toString()

    // Parse common fields
    val registrar = extractField(finalData, listOf("Registrar:", "registrar:", "Registrar Name:", "Sponsoring Registrar:"))
    val creationDate = extractField(finalData, listOf("Creation Date:", "created:", "Created:", "Registration Time:", "Registered on:", "Created On:"))
    val expirationDate = extractField(finalData, listOf("Registry Expiry Date:", "Expiration Date:", "expires:", "Expiry Date:", "Expiration Time:", "Expires On:"))
    val updatedDate = extractField(finalData, listOf("Updated Date:", "Last Updated:", "last-update:", "Last Modified:", "Updated On:"))
    val registrantOrg = extractField(finalData, listOf("Registrant Organization:", "Registrant:", "org:", "Registrant Name:"))
    val registrantCountry = extractField(finalData, listOf("Registrant Country:", "Registrant State/Province:", "country:"))

    val nameServers = mutableListOf<String>()
    finalData.lines().forEach { line ->
        if (line.lowercase().contains("name server:") || line.lowercase().startsWith("nserver:")) {
            val ns = line.substringAfter(":").trim()
            if (ns.isNotEmpty() && !nameServers.contains(ns.lowercase())) {
                nameServers.add(ns.lowercase())
            }
        }
    }

    val status = mutableListOf<String>()
    finalData.lines().forEach { line ->
        if (line.lowercase().contains("domain status:") || line.lowercase().startsWith("status:")) {
            val s = line.substringAfter(":").trim()
            if (s.isNotEmpty()) {
                status.add(s)
            }
        }
    }

    return WhoisResult(
        domain = domain,
        rawData = finalData,
        registrar = registrar,
        creationDate = creationDate,
        expirationDate = expirationDate,
        updatedDate = updatedDate,
        nameServers = nameServers.take(10),
        status = status.take(10),
        registrantOrg = registrantOrg,
        registrantCountry = registrantCountry
    )
}

private fun queryWhoisServer(server: String, query: String): String {
    val rawData = StringBuilder()

    Socket(server, 43).use { socket ->
        socket.soTimeout = 15000

        OutputStreamWriter(socket.getOutputStream()).use { writer ->
            writer.write("$query\r\n")
            writer.flush()
        }

        BufferedReader(InputStreamReader(socket.getInputStream())).use { reader ->
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                rawData.appendLine(line)
            }
        }
    }

    return rawData.toString()
}

private fun extractReferralServer(data: String): String? {
    // Look for referral/whois server in response
    val patterns = listOf(
        "whois:" to { line: String -> line.substringAfter(":").trim() },
        "Whois Server:" to { line: String -> line.substringAfter(":").trim() },
        "Registrar WHOIS Server:" to { line: String -> line.substringAfter(":").trim() },
        "refer:" to { line: String -> line.substringAfter(":").trim() }
    )

    for ((pattern, extractor) in patterns) {
        data.lines().forEach { line ->
            if (line.lowercase().startsWith(pattern.lowercase()) ||
                line.lowercase().contains(pattern.lowercase())) {
                val server = extractor(line)
                if (server.isNotEmpty() && server.contains(".") && !server.contains(" ")) {
                    return server.lowercase()
                }
            }
        }
    }
    return null
}

private fun extractField(data: String, keys: List<String>): String? {
    for (key in keys) {
        data.lines().forEach { line ->
            if (line.lowercase().startsWith(key.lowercase())) {
                val value = line.substringAfter(":").trim()
                if (value.isNotEmpty()) return value
            }
        }
    }
    return null
}

private fun getWhoisServer(tld: String): String {
    return when (tld.lowercase()) {
        // Generic TLDs
        "com", "net" -> "whois.verisign-grs.com"
        "org" -> "whois.pir.org"
        "info" -> "whois.afilias.net"
        "biz" -> "whois.biz"

        // Country code TLDs
        "io" -> "whois.nic.io"
        "co" -> "whois.nic.co"  // Will fallback to IANA if fails
        "me" -> "whois.nic.me"
        "tv" -> "tvwhois.verisign-grs.com"
        "cc" -> "ccwhois.verisign-grs.com"
        "us" -> "whois.nic.us"
        "uk", "co.uk" -> "whois.nic.uk"
        "de" -> "whois.denic.de"
        "fr" -> "whois.nic.fr"
        "nl" -> "whois.domain-registry.nl"
        "eu" -> "whois.eu"
        "ru" -> "whois.tcinet.ru"
        "cn" -> "whois.cnnic.cn"
        "jp" -> "whois.jprs.jp"
        "kr" -> "whois.kr"
        "au", "com.au" -> "whois.auda.org.au"
        "ca" -> "whois.cira.ca"
        "br", "com.br" -> "whois.registro.br"
        "in" -> "whois.registry.in"
        "mx" -> "whois.mx"
        "it" -> "whois.nic.it"
        "es" -> "whois.nic.es"
        "pl" -> "whois.dns.pl"
        "se" -> "whois.iis.se"
        "no" -> "whois.norid.no"
        "fi" -> "whois.fi"
        "dk" -> "whois.dk-hostmaster.dk"
        "at" -> "whois.nic.at"
        "ch" -> "whois.nic.ch"
        "be" -> "whois.dns.be"
        "nz" -> "whois.srs.net.nz"
        "sg" -> "whois.sgnic.sg"
        "hk" -> "whois.hkirc.hk"
        "tw" -> "whois.twnic.net.tw"
        "th" -> "whois.thnic.co.th"
        "my" -> "whois.mynic.my"
        "id" -> "whois.id"
        "ph" -> "whois.dot.ph"
        "vn" -> "whois.vnnic.vn"
        "za" -> "whois.registry.net.za"
        "ae" -> "whois.aeda.net.ae"
        "sa" -> "whois.nic.net.sa"
        "il" -> "whois.isoc.org.il"
        "tr" -> "whois.trabis.gov.tr"
        "ua" -> "whois.ua"
        "cz" -> "whois.nic.cz"
        "hu" -> "whois.nic.hu"
        "ro" -> "whois.rotld.ro"
        "sk" -> "whois.sk-nic.sk"
        "bg" -> "whois.register.bg"
        "gr" -> "whois.ics.forth.gr"
        "pt" -> "whois.dns.pt"
        "ie" -> "whois.iedr.ie"
        "cl" -> "whois.nic.cl"
        "ar" -> "whois.nic.ar"
        "pe" -> "kero.yachay.pe"
        "ve" -> "whois.nic.ve"
        "ec" -> "whois.nic.ec"

        // New gTLDs
        "app" -> "whois.nic.google"
        "dev" -> "whois.nic.google"
        "page" -> "whois.nic.google"
        "xyz" -> "whois.nic.xyz"
        "online" -> "whois.nic.online"
        "site" -> "whois.nic.site"
        "tech" -> "whois.nic.tech"
        "store" -> "whois.nic.store"
        "cloud" -> "whois.nic.cloud"
        "club" -> "whois.nic.club"
        "live" -> "whois.nic.live"
        "shop" -> "whois.nic.shop"
        "blog" -> "whois.nic.blog"
        "pro" -> "whois.nic.pro"
        "top" -> "whois.nic.top"
        "vip" -> "whois.nic.vip"
        "work" -> "whois.nic.work"
        "life" -> "whois.nic.life"
        "world" -> "whois.nic.world"
        "today" -> "whois.nic.today"
        "news" -> "whois.nic.news"
        "media" -> "whois.nic.media"
        "agency" -> "whois.nic.agency"
        "company" -> "whois.nic.company"
        "network" -> "whois.nic.network"
        "digital" -> "whois.nic.digital"
        "solutions" -> "whois.nic.solutions"
        "services" -> "whois.nic.services"
        "systems" -> "whois.nic.systems"
        "email" -> "whois.nic.email"
        "support" -> "whois.nic.support"
        "design" -> "whois.nic.design"
        "studio" -> "whois.nic.studio"
        "photography" -> "whois.nic.photography"
        "marketing" -> "whois.nic.marketing"
        "consulting" -> "whois.nic.consulting"
        "education" -> "whois.nic.education"
        "academy" -> "whois.nic.academy"
        "training" -> "whois.nic.training"
        "center" -> "whois.nic.center"
        "zone" -> "whois.nic.zone"
        "space" -> "whois.nic.space"
        "earth" -> "whois.nic.earth"
        "global" -> "whois.nic.global"
        "international" -> "whois.nic.international"

        // Default to IANA (will provide referral)
        else -> "whois.iana.org"
    }
}
