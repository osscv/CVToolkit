package cv.toolkit.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cv.toolkit.ads.BannerAd
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

data class TimeZoneInfo(
    val id: String,
    val displayName: String,
    val city: String,
    val country: String
)

private val defaultTimeZones = listOf(
    TimeZoneInfo("America/New_York", "Eastern Time", "New York", "USA"),
    TimeZoneInfo("America/Los_Angeles", "Pacific Time", "Los Angeles", "USA"),
    TimeZoneInfo("America/Chicago", "Central Time", "Chicago", "USA"),
    TimeZoneInfo("Europe/London", "GMT/BST", "London", "UK"),
    TimeZoneInfo("Europe/Paris", "Central European", "Paris", "France"),
    TimeZoneInfo("Europe/Berlin", "Central European", "Berlin", "Germany"),
    TimeZoneInfo("Asia/Tokyo", "Japan Standard", "Tokyo", "Japan"),
    TimeZoneInfo("Asia/Shanghai", "China Standard", "Shanghai", "China"),
    TimeZoneInfo("Asia/Hong_Kong", "Hong Kong", "Hong Kong", "China"),
    TimeZoneInfo("Asia/Singapore", "Singapore", "Singapore", "Singapore"),
    TimeZoneInfo("Asia/Kuala_Lumpur", "Malaysia", "Kuala Lumpur", "Malaysia"),
    TimeZoneInfo("Asia/Seoul", "Korea Standard", "Seoul", "South Korea"),
    TimeZoneInfo("Asia/Dubai", "Gulf Standard", "Dubai", "UAE"),
    TimeZoneInfo("Asia/Kolkata", "India Standard", "Mumbai", "India"),
    TimeZoneInfo("Asia/Jakarta", "Western Indonesia", "Jakarta", "Indonesia"),
    TimeZoneInfo("Australia/Sydney", "Australian Eastern", "Sydney", "Australia"),
    TimeZoneInfo("Pacific/Auckland", "New Zealand", "Auckland", "New Zealand"),
    TimeZoneInfo("Europe/Moscow", "Moscow", "Moscow", "Russia"),
    TimeZoneInfo("America/Sao_Paulo", "Brasilia", "Sao Paulo", "Brazil"),
    TimeZoneInfo("Africa/Johannesburg", "South Africa", "Johannesburg", "South Africa")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorldTimeScreen(navController: NavController) {
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var searchQuery by remember { mutableStateOf("") }
    var use24Hour by remember { mutableStateOf(true) }

    // Update time every second
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            delay(1000)
        }
    }

    val filteredTimeZones = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            defaultTimeZones
        } else {
            defaultTimeZones.filter {
                it.city.contains(searchQuery, ignoreCase = true) ||
                it.country.contains(searchQuery, ignoreCase = true) ||
                it.displayName.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    val localTimeZone = remember { TimeZone.getDefault() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("World Time") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { use24Hour = !use24Hour }) {
                        Icon(
                            if (use24Hour) Icons.Filled.Schedule else Icons.Filled.AccessTime,
                            contentDescription = "Toggle time format"
                        )
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
            // Local time card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Filled.LocationOn,
                            null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Your Local Time",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        formatTime(currentTime, localTimeZone, use24Hour),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        formatDate(currentTime, localTimeZone),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "${localTimeZone.displayName} (${localTimeZone.id})",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                    )
                }
            }

            // Search field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                label = { Text("Search city or country") },
                placeholder = { Text("e.g., Tokyo, USA, London...") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                leadingIcon = { Icon(Icons.Filled.Search, null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Clear, "Clear")
                        }
                    }
                }
            )

            // Time format indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "${filteredTimeZones.size} time zones",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    if (use24Hour) "24-hour format" else "12-hour format",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Time zones list
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredTimeZones, key = { it.id }) { tzInfo ->
                    TimeZoneCard(
                        tzInfo = tzInfo,
                        currentTime = currentTime,
                        localTimeZone = localTimeZone,
                        use24Hour = use24Hour
                    )
                }
            }

            BannerAd(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun TimeZoneCard(
    tzInfo: TimeZoneInfo,
    currentTime: Long,
    localTimeZone: TimeZone,
    use24Hour: Boolean
) {
    val timeZone = remember(tzInfo.id) { TimeZone.getTimeZone(tzInfo.id) }
    val offsetDiff = remember(currentTime, tzInfo.id) {
        val localOffset = localTimeZone.getOffset(currentTime)
        val targetOffset = timeZone.getOffset(currentTime)
        val diffHours = (targetOffset - localOffset) / (1000 * 60 * 60).toFloat()
        when {
            diffHours == 0f -> "Same time"
            diffHours > 0 -> "+${formatOffset(diffHours)}"
            else -> formatOffset(diffHours)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    tzInfo.city,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    tzInfo.country,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    tzInfo.displayName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    formatTime(currentTime, timeZone, use24Hour),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    formatDateShort(currentTime, timeZone),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Surface(
                    color = when {
                        offsetDiff == "Same time" -> MaterialTheme.colorScheme.secondaryContainer
                        offsetDiff.startsWith("+") -> MaterialTheme.colorScheme.tertiaryContainer
                        else -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                    },
                    shape = MaterialTheme.shapes.small
                ) {
                    Text(
                        offsetDiff,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = when {
                            offsetDiff == "Same time" -> MaterialTheme.colorScheme.onSecondaryContainer
                            offsetDiff.startsWith("+") -> MaterialTheme.colorScheme.onTertiaryContainer
                            else -> MaterialTheme.colorScheme.onErrorContainer
                        }
                    )
                }
            }
        }
    }
}

private fun formatTime(timeMillis: Long, timeZone: TimeZone, use24Hour: Boolean): String {
    val pattern = if (use24Hour) "HH:mm:ss" else "hh:mm:ss a"
    val sdf = SimpleDateFormat(pattern, Locale.getDefault())
    sdf.timeZone = timeZone
    return sdf.format(Date(timeMillis))
}

private fun formatDate(timeMillis: Long, timeZone: TimeZone): String {
    val sdf = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault())
    sdf.timeZone = timeZone
    return sdf.format(Date(timeMillis))
}

private fun formatDateShort(timeMillis: Long, timeZone: TimeZone): String {
    val sdf = SimpleDateFormat("MMM d", Locale.getDefault())
    sdf.timeZone = timeZone
    return sdf.format(Date(timeMillis))
}

private fun formatOffset(hours: Float): String {
    val absHours = kotlin.math.abs(hours)
    val wholeHours = absHours.toInt()
    val minutes = ((absHours - wholeHours) * 60).toInt()
    val sign = if (hours < 0) "-" else ""
    return if (minutes == 0) {
        "${sign}${wholeHours}h"
    } else {
        "${sign}${wholeHours}h ${minutes}m"
    }
}
