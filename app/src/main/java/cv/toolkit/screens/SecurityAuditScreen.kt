@file:Suppress("DEPRECATION")

package cv.toolkit.screens

import android.app.KeyguardManager
import android.app.admin.DevicePolicyManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import cv.toolkit.R
import cv.toolkit.ads.BannerAd
import java.io.File

data class SecurityCheck(
    val name: String,
    val description: String,
    val isPassed: Boolean,
    val detail: String
)

fun performSecurityAudit(context: Context): List<SecurityCheck> {
    val checks = mutableListOf<SecurityCheck>()

    // 1. Root Detection
    val suPaths = listOf(
        "/system/bin/su",
        "/system/xbin/su",
        "/sbin/su",
        "/data/local/bin/su",
        "/data/local/xbin/su"
    )
    val suFound = suPaths.any { File(it).exists() }
    val magiskFound = try {
        val pm = context.packageManager
        pm.getPackageInfo("com.topjohnwu.magisk", 0)
        true
    } catch (_: PackageManager.NameNotFoundException) {
        false
    }
    val testKeys = Build.TAGS?.contains("test-keys") == true
    val isRooted = suFound || magiskFound || testKeys
    checks.add(
        SecurityCheck(
            name = "Root Detection",
            description = "Checks if the device is rooted",
            isPassed = !isRooted,
            detail = when {
                suFound && magiskFound -> "su binary and Magisk detected"
                suFound -> "su binary found in system paths"
                magiskFound -> "Magisk package detected"
                testKeys -> "Build signed with test-keys"
                else -> "No root indicators found"
            }
        )
    )

    // 2. Developer Options
    val devOptionsEnabled = try {
        Settings.Secure.getInt(
            context.contentResolver,
            Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
            0
        ) == 1
    } catch (_: Exception) {
        false
    }
    checks.add(
        SecurityCheck(
            name = "Developer Options",
            description = "Checks if developer options are enabled",
            isPassed = !devOptionsEnabled,
            detail = if (devOptionsEnabled) "Developer options are enabled" else "Developer options are disabled"
        )
    )

    // 3. USB Debugging
    val usbDebugging = try {
        Settings.Global.getInt(
            context.contentResolver,
            Settings.Global.ADB_ENABLED,
            0
        ) == 1
    } catch (_: Exception) {
        false
    }
    checks.add(
        SecurityCheck(
            name = "USB Debugging",
            description = "Checks if ADB debugging is enabled",
            isPassed = !usbDebugging,
            detail = if (usbDebugging) "USB debugging is enabled" else "USB debugging is disabled"
        )
    )

    // 4. Screen Lock
    val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
    val screenLockSet = keyguardManager?.isDeviceSecure == true
    checks.add(
        SecurityCheck(
            name = "Screen Lock",
            description = "Checks if a secure screen lock is set",
            isPassed = screenLockSet,
            detail = if (screenLockSet) "Secure screen lock is set" else "No secure screen lock configured"
        )
    )

    // 5. Encryption
    val isEncrypted = try {
        val dpm = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
        val encryptionStatus = dpm?.storageEncryptionStatus
        encryptionStatus == DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE ||
                encryptionStatus == DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_PER_USER ||
                encryptionStatus == DevicePolicyManager.ENCRYPTION_STATUS_ACTIVE_DEFAULT_KEY
    } catch (_: Exception) {
        false
    }
    checks.add(
        SecurityCheck(
            name = "Device Encryption",
            description = "Checks if storage encryption is active",
            isPassed = isEncrypted,
            detail = if (isEncrypted) "Device storage is encrypted" else "Device storage may not be encrypted"
        )
    )

    // 6. Mock Location
    val mockLocationEnabled = try {
        Settings.Secure.getString(
            context.contentResolver,
            "mock_location"
        )?.equals("1") == true
    } catch (_: Exception) {
        false
    }
    val mockLocationApps = try {
        val pm = context.packageManager
        val installedApps = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        installedApps.any { appInfo ->
            try {
                val appInfoFull = pm.getApplicationInfo(appInfo.packageName, PackageManager.GET_META_DATA)
                appInfoFull.metaData?.containsKey("com.google.android.gms.location.sample.mock_location") == true
            } catch (_: Exception) {
                false
            }
        }
    } catch (_: Exception) {
        false
    }
    val hasMockLocation = mockLocationEnabled || mockLocationApps
    checks.add(
        SecurityCheck(
            name = "Mock Location",
            description = "Checks for mock location providers",
            isPassed = !hasMockLocation,
            detail = when {
                mockLocationEnabled && mockLocationApps -> "Mock location enabled with apps detected"
                mockLocationEnabled -> "Mock location setting is enabled"
                mockLocationApps -> "Mock location apps detected"
                else -> "No mock location detected"
            }
        )
    )

    // 7. Unknown Sources
    val unknownSourcesEnabled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        try {
            context.packageManager.canRequestPackageInstalls()
        } catch (_: Exception) {
            false
        }
    } else {
        try {
            Settings.Secure.getInt(
                context.contentResolver,
                Settings.Secure.INSTALL_NON_MARKET_APPS,
                0
            ) == 1
        } catch (_: Exception) {
            false
        }
    }
    checks.add(
        SecurityCheck(
            name = "Unknown Sources",
            description = "Checks if installation from unknown sources is allowed",
            isPassed = !unknownSourcesEnabled,
            detail = if (unknownSourcesEnabled) "Installation from unknown sources is allowed"
            else "Installation from unknown sources is blocked"
        )
    )

    // 8. Verify Apps
    val verifyAppsEnabled = try {
        Settings.Global.getInt(
            context.contentResolver,
            "package_verifier_enable",
            1
        ) == 1
    } catch (_: Exception) {
        true
    }
    checks.add(
        SecurityCheck(
            name = "Verify Apps",
            description = "Checks if app verification is enabled",
            isPassed = verifyAppsEnabled,
            detail = if (verifyAppsEnabled) "App verification is enabled" else "App verification is disabled"
        )
    )

    return checks
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SecurityAuditScreen(navController: NavController) {
    val context = LocalContext.current
    val checks = remember { performSecurityAudit(context) }
    val passedCount = checks.count { it.isPassed }
    val totalCount = checks.size
    val scorePercent = if (totalCount > 0) (passedCount * 100) / totalCount else 0

    val scoreColor = when {
        scorePercent > 80 -> Color(0xFF4CAF50)
        scorePercent >= 50 -> Color(0xFFFFC107)
        else -> Color(0xFFF44336)
    }

    val scoreLabel = when {
        scorePercent > 80 -> "Good"
        scorePercent >= 50 -> "Moderate"
        else -> "Poor"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.security_audit_title)) },
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
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Score header
                item {
                    SecurityScoreHeader(
                        scorePercent = scorePercent,
                        scoreColor = scoreColor,
                        scoreLabel = scoreLabel,
                        passedCount = passedCount,
                        totalCount = totalCount
                    )
                }

                item { Spacer(Modifier.height(4.dp)) }

                // Check items
                items(checks) { check ->
                    SecurityCheckCard(check)
                }

                item { Spacer(Modifier.height(8.dp)) }
            }
            BannerAd(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun SecurityScoreHeader(
    scorePercent: Int,
    scoreColor: Color,
    scoreLabel: String,
    passedCount: Int,
    totalCount: Int
) {
    val animatedProgress by animateFloatAsState(
        targetValue = scorePercent / 100f,
        animationSpec = tween(durationMillis = 1000),
        label = "scoreAnimation"
    )

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
            Text(
                text = "Device Security Score",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(20.dp))

            // Circular progress indicator
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(160.dp)
            ) {
                val trackColor = MaterialTheme.colorScheme.surfaceVariant

                Canvas(modifier = Modifier.size(160.dp)) {
                    val strokeWidth = 12.dp.toPx()
                    val diameter = size.minDimension
                    val radius = (diameter - strokeWidth) / 2
                    val topLeft = Offset(
                        (size.width - 2 * radius) / 2,
                        (size.height - 2 * radius) / 2
                    )
                    val arcSize = Size(radius * 2, radius * 2)

                    // Background track
                    drawArc(
                        color = trackColor,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )

                    // Progress arc
                    drawArc(
                        color = scoreColor,
                        startAngle = -90f,
                        sweepAngle = 360f * animatedProgress,
                        useCenter = false,
                        topLeft = topLeft,
                        size = arcSize,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "$scorePercent%",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = scoreColor
                    )
                    Text(
                        text = scoreLabel,
                        style = MaterialTheme.typography.bodyMedium,
                        color = scoreColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = "$passedCount of $totalCount checks passed",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun SecurityCheckCard(check: SecurityCheck) {
    val statusColor = if (check.isPassed) Color(0xFF4CAF50) else Color(0xFFF44336)
    val statusIcon = if (check.isPassed) Icons.Filled.CheckCircle else Icons.Filled.Warning
    val statusText = if (check.isPassed) "Secure" else "At Risk"

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = statusIcon,
                contentDescription = statusText,
                tint = statusColor,
                modifier = Modifier.size(32.dp)
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = check.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Surface(
                        shape = MaterialTheme.shapes.small,
                        color = statusColor.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = statusText,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = statusColor,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp
                        )
                    }
                }

                Spacer(Modifier.height(2.dp))

                Text(
                    text = check.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.height(4.dp))

                Text(
                    text = check.detail,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}
