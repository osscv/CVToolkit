package cv.toolkit.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cv.toolkit.ads.BannerAd
import kotlin.math.pow

data class SubnetResult(
    val ipAddress: String,
    val cidr: Int,
    val subnetMask: String,
    val wildcardMask: String,
    val networkAddress: String,
    val broadcastAddress: String,
    val firstUsableHost: String,
    val lastUsableHost: String,
    val totalHosts: Long,
    val usableHosts: Long,
    val ipClass: String,
    val ipType: String,
    val binarySubnetMask: String,
    val binaryNetworkAddress: String
)

// Common CIDR presets
val cidrPresets = listOf(
    8 to "Class A (/8)",
    16 to "Class B (/16)",
    24 to "Class C (/24)",
    25 to "/25 - 128 hosts",
    26 to "/26 - 64 hosts",
    27 to "/27 - 32 hosts",
    28 to "/28 - 16 hosts",
    29 to "/29 - 8 hosts",
    30 to "/30 - 4 hosts",
    31 to "/31 - P2P Link",
    32 to "/32 - Single Host"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubnetCalculatorScreen(navController: NavController) {
    var ipAddress by remember { mutableStateOf("192.168.1.0") }
    var cidr by remember { mutableStateOf("24") }
    var result by remember { mutableStateOf<SubnetResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun calculateSubnet() {
        errorMessage = null
        result = null

        val cidrValue = cidr.toIntOrNull()
        if (cidrValue == null || cidrValue < 0 || cidrValue > 32) {
            errorMessage = "CIDR must be between 0 and 32"
            return
        }

        val ipParts = ipAddress.trim().split(".")
        if (ipParts.size != 4) {
            errorMessage = "Invalid IP address format"
            return
        }

        val octets = try {
            ipParts.map { it.toInt() }
        } catch (_: Exception) {
            errorMessage = "Invalid IP address"
            return
        }

        if (octets.any { it < 0 || it > 255 }) {
            errorMessage = "Each octet must be between 0 and 255"
            return
        }

        // Calculate subnet mask
        val maskBits = 0xFFFFFFFF.toUInt() shl (32 - cidrValue)
        val subnetMask = listOf(
            ((maskBits shr 24) and 0xFFu).toInt(),
            ((maskBits shr 16) and 0xFFu).toInt(),
            ((maskBits shr 8) and 0xFFu).toInt(),
            (maskBits and 0xFFu).toInt()
        ).joinToString(".")

        // Calculate wildcard mask
        val wildcardBits = maskBits.inv()
        val wildcardMask = listOf(
            ((wildcardBits shr 24) and 0xFFu).toInt(),
            ((wildcardBits shr 16) and 0xFFu).toInt(),
            ((wildcardBits shr 8) and 0xFFu).toInt(),
            (wildcardBits and 0xFFu).toInt()
        ).joinToString(".")

        // Convert IP to 32-bit integer
        val ipInt = (octets[0].toUInt() shl 24) or
                (octets[1].toUInt() shl 16) or
                (octets[2].toUInt() shl 8) or
                octets[3].toUInt()

        // Calculate network address
        val networkInt = ipInt and maskBits
        val networkAddress = listOf(
            ((networkInt shr 24) and 0xFFu).toInt(),
            ((networkInt shr 16) and 0xFFu).toInt(),
            ((networkInt shr 8) and 0xFFu).toInt(),
            (networkInt and 0xFFu).toInt()
        ).joinToString(".")

        // Calculate broadcast address
        val broadcastInt = networkInt or wildcardBits
        val broadcastAddress = listOf(
            ((broadcastInt shr 24) and 0xFFu).toInt(),
            ((broadcastInt shr 16) and 0xFFu).toInt(),
            ((broadcastInt shr 8) and 0xFFu).toInt(),
            (broadcastInt and 0xFFu).toInt()
        ).joinToString(".")

        // Calculate first and last usable hosts
        val totalHosts = 2.0.pow(32 - cidrValue).toLong()
        val usableHosts = if (cidrValue >= 31) {
            if (cidrValue == 31) 2L else 1L
        } else {
            totalHosts - 2
        }

        val firstUsableInt = if (cidrValue >= 31) networkInt else networkInt + 1u
        val firstUsableHost = listOf(
            ((firstUsableInt shr 24) and 0xFFu).toInt(),
            ((firstUsableInt shr 16) and 0xFFu).toInt(),
            ((firstUsableInt shr 8) and 0xFFu).toInt(),
            (firstUsableInt and 0xFFu).toInt()
        ).joinToString(".")

        val lastUsableInt = if (cidrValue >= 31) broadcastInt else broadcastInt - 1u
        val lastUsableHost = listOf(
            ((lastUsableInt shr 24) and 0xFFu).toInt(),
            ((lastUsableInt shr 16) and 0xFFu).toInt(),
            ((lastUsableInt shr 8) and 0xFFu).toInt(),
            (lastUsableInt and 0xFFu).toInt()
        ).joinToString(".")

        // Determine IP class
        val ipClass = when {
            octets[0] in 1..126 -> "Class A"
            octets[0] in 128..191 -> "Class B"
            octets[0] in 192..223 -> "Class C"
            octets[0] in 224..239 -> "Class D (Multicast)"
            octets[0] in 240..255 -> "Class E (Reserved)"
            else -> "Unknown"
        }

        // Determine IP type (Private/Public)
        val ipType = when {
            octets[0] == 10 -> "Private (10.0.0.0/8)"
            octets[0] == 172 && octets[1] in 16..31 -> "Private (172.16.0.0/12)"
            octets[0] == 192 && octets[1] == 168 -> "Private (192.168.0.0/16)"
            octets[0] == 127 -> "Loopback"
            octets[0] == 169 && octets[1] == 254 -> "Link-Local (APIPA)"
            octets[0] in 224..239 -> "Multicast"
            octets[0] >= 240 -> "Reserved"
            else -> "Public"
        }

        // Binary representations
        val binarySubnetMask = listOf(
            ((maskBits shr 24) and 0xFFu).toInt(),
            ((maskBits shr 16) and 0xFFu).toInt(),
            ((maskBits shr 8) and 0xFFu).toInt(),
            (maskBits and 0xFFu).toInt()
        ).joinToString(".") { it.toString(2).padStart(8, '0') }

        val binaryNetworkAddress = listOf(
            ((networkInt shr 24) and 0xFFu).toInt(),
            ((networkInt shr 16) and 0xFFu).toInt(),
            ((networkInt shr 8) and 0xFFu).toInt(),
            (networkInt and 0xFFu).toInt()
        ).joinToString(".") { it.toString(2).padStart(8, '0') }

        result = SubnetResult(
            ipAddress = ipAddress.trim(),
            cidr = cidrValue,
            subnetMask = subnetMask,
            wildcardMask = wildcardMask,
            networkAddress = networkAddress,
            broadcastAddress = broadcastAddress,
            firstUsableHost = firstUsableHost,
            lastUsableHost = lastUsableHost,
            totalHosts = totalHosts,
            usableHosts = usableHosts,
            ipClass = ipClass,
            ipType = ipType,
            binarySubnetMask = binarySubnetMask,
            binaryNetworkAddress = binaryNetworkAddress
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Subnet Calculator") },
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
            // IP Address input
            OutlinedTextField(
                value = ipAddress,
                onValueChange = { ipAddress = it },
                label = { Text("IP Address") },
                placeholder = { Text("192.168.1.0") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Language, null) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
            )

            // CIDR input
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = cidr,
                    onValueChange = { if (it.isEmpty() || it.all { c -> c.isDigit() }) cidr = it },
                    label = { Text("CIDR") },
                    modifier = Modifier.width(100.dp),
                    singleLine = true,
                    leadingIcon = { Text("/", fontWeight = FontWeight.Bold) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
                Text("or select:", style = MaterialTheme.typography.bodySmall)
            }

            // CIDR presets
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(cidrPresets) { (value, label) ->
                    FilterChip(
                        selected = cidr == value.toString(),
                        onClick = { cidr = value.toString() },
                        label = { Text("/$value") }
                    )
                }
            }

            // Calculate button
            Button(
                onClick = { calculateSubnet() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Calculate, null)
                Spacer(Modifier.width(8.dp))
                Text("Calculate")
            }

            // Error message
            errorMessage?.let {
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            // Results
            result?.let { r ->
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    item {
                        Text("Results", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    // Network Info Card
                    item {
                        ResultCard(title = "Network Information") {
                            ResultRow("IP Address", "${r.ipAddress}/${r.cidr}")
                            ResultRow("Network Address", r.networkAddress)
                            ResultRow("Broadcast Address", r.broadcastAddress)
                            ResultRow("Subnet Mask", r.subnetMask)
                            ResultRow("Wildcard Mask", r.wildcardMask)
                        }
                    }

                    // Host Range Card
                    item {
                        ResultCard(title = "Host Range") {
                            ResultRow("First Usable Host", r.firstUsableHost)
                            ResultRow("Last Usable Host", r.lastUsableHost)
                            ResultRow("Total Addresses", formatNumber(r.totalHosts))
                            ResultRow("Usable Hosts", formatNumber(r.usableHosts))
                        }
                    }

                    // IP Classification Card
                    item {
                        ResultCard(title = "IP Classification") {
                            ResultRow("IP Class", r.ipClass)
                            ResultRow("IP Type", r.ipType)
                        }
                    }

                    // Binary Representation Card
                    item {
                        ResultCard(title = "Binary Representation") {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Subnet Mask:", style = MaterialTheme.typography.labelMedium)
                                Text(
                                    r.binarySubnetMask,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace
                                )
                                Spacer(Modifier.height(4.dp))
                                Text("Network Address:", style = MaterialTheme.typography.labelMedium)
                                Text(
                                    r.binaryNetworkAddress,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }

                    // CIDR Quick Reference
                    item {
                        ResultCard(title = "CIDR Quick Reference") {
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text("/24 = 256 addresses (254 usable)", style = MaterialTheme.typography.bodySmall)
                                Text("/25 = 128 addresses (126 usable)", style = MaterialTheme.typography.bodySmall)
                                Text("/26 = 64 addresses (62 usable)", style = MaterialTheme.typography.bodySmall)
                                Text("/27 = 32 addresses (30 usable)", style = MaterialTheme.typography.bodySmall)
                                Text("/28 = 16 addresses (14 usable)", style = MaterialTheme.typography.bodySmall)
                                Text("/29 = 8 addresses (6 usable)", style = MaterialTheme.typography.bodySmall)
                                Text("/30 = 4 addresses (2 usable)", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            }

            BannerAd(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ResultCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleSmall)
            content()
        }
    }
}

@Composable
private fun ResultRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace)
    }
}

private fun formatNumber(number: Long): String {
    return when {
        number >= 1_000_000_000 -> String.format("%.2fB", number / 1_000_000_000.0)
        number >= 1_000_000 -> String.format("%.2fM", number / 1_000_000.0)
        number >= 1_000 -> String.format("%.2fK", number / 1_000.0)
        else -> number.toString()
    }
}
