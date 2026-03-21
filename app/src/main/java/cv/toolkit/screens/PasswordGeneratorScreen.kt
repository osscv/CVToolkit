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
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cv.toolkit.ads.BannerAd
import kotlinx.coroutines.launch
import java.security.SecureRandom

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordGeneratorScreen(navController: NavController) {
    var passwordLength by remember { mutableFloatStateOf(16f) }
    var includeUppercase by remember { mutableStateOf(true) }
    var includeLowercase by remember { mutableStateOf(true) }
    var includeNumbers by remember { mutableStateOf(true) }
    var includeSymbols by remember { mutableStateOf(true) }
    var excludeAmbiguous by remember { mutableStateOf(false) }
    var generatedPasswords by remember { mutableStateOf(listOf<String>()) }
    var passwordCount by remember { mutableFloatStateOf(1f) }
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    fun generatePasswords() {
        val uppercase = if (excludeAmbiguous) "ABCDEFGHJKLMNPQRSTUVWXYZ" else "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        val lowercase = if (excludeAmbiguous) "abcdefghjkmnpqrstuvwxyz" else "abcdefghijklmnopqrstuvwxyz"
        val numbers = if (excludeAmbiguous) "23456789" else "0123456789"
        val symbols = "!@#\$%^&*()_+-=[]{}|;:,.<>?"

        val charPool = buildString {
            if (includeUppercase) append(uppercase)
            if (includeLowercase) append(lowercase)
            if (includeNumbers) append(numbers)
            if (includeSymbols) append(symbols)
        }

        if (charPool.isEmpty()) {
            generatedPasswords = listOf("Select at least one character type")
            return
        }

        val random = SecureRandom()
        val length = passwordLength.toInt()
        val count = passwordCount.toInt()

        generatedPasswords = (1..count).map {
            buildString {
                repeat(length) {
                    append(charPool[random.nextInt(charPool.length)])
                }
            }
        }
    }

    // Generate initial password
    LaunchedEffect(Unit) {
        generatePasswords()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Password Generator") },
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
            // Password length slider
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
                        Text("Password Length", style = MaterialTheme.typography.labelLarge)
                        Text(
                            "${passwordLength.toInt()} characters",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = passwordLength,
                        onValueChange = { passwordLength = it },
                        valueRange = 4f..64f,
                        steps = 59,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Number of Passwords", style = MaterialTheme.typography.labelLarge)
                        Text(
                            "${passwordCount.toInt()}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Slider(
                        value = passwordCount,
                        onValueChange = { passwordCount = it },
                        valueRange = 1f..10f,
                        steps = 8,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            // Character options
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Character Types", style = MaterialTheme.typography.labelLarge)
                    Spacer(Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = includeUppercase, onCheckedChange = { includeUppercase = it })
                                Text("A-Z", style = MaterialTheme.typography.bodySmall)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = includeLowercase, onCheckedChange = { includeLowercase = it })
                                Text("a-z", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = includeNumbers, onCheckedChange = { includeNumbers = it })
                                Text("0-9", style = MaterialTheme.typography.bodySmall)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = includeSymbols, onCheckedChange = { includeSymbols = it })
                                Text("!@#\$%", style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }

                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = excludeAmbiguous, onCheckedChange = { excludeAmbiguous = it })
                        Column {
                            Text("Exclude Ambiguous", style = MaterialTheme.typography.bodySmall)
                            Text(
                                "Removes 0, O, l, 1, I",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Generate button
            Button(
                onClick = { generatePasswords() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text("Generate Password${if (passwordCount > 1) "s" else ""}")
            }

            // Password strength indicator
            if (generatedPasswords.isNotEmpty() && generatedPasswords[0] != "Select at least one character type") {
                val strength = calculatePasswordStrength(
                    passwordLength.toInt(),
                    includeUppercase,
                    includeLowercase,
                    includeNumbers,
                    includeSymbols
                )
                PasswordStrengthIndicator(strength)
            }

            // Generated passwords
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Generated Password${if (passwordCount > 1) "s" else ""}", style = MaterialTheme.typography.labelMedium)
                        if (generatedPasswords.size == 1) {
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        clipboard.setClipEntry(
                                            androidx.compose.ui.platform.ClipEntry(
                                                android.content.ClipData.newPlainText("password", generatedPasswords[0])
                                            )
                                        )
                                    }
                                }
                            ) {
                                Icon(Icons.Filled.ContentCopy, "Copy", modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))

                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(generatedPasswords) { password ->
                            PasswordItem(
                                password = password,
                                showCopyButton = generatedPasswords.size > 1,
                                onCopy = {
                                    scope.launch {
                                        clipboard.setClipEntry(
                                            androidx.compose.ui.platform.ClipEntry(
                                                android.content.ClipData.newPlainText("password", password)
                                            )
                                        )
                                    }
                                }
                            )
                        }
                    }
                }
            }

            BannerAd(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun PasswordItem(
    password: String,
    showCopyButton: Boolean,
    onCopy: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                password,
                fontFamily = FontFamily.Monospace,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            if (showCopyButton) {
                IconButton(onClick = onCopy, modifier = Modifier.size(32.dp)) {
                    Icon(Icons.Filled.ContentCopy, "Copy", modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

private fun calculatePasswordStrength(
    length: Int,
    hasUpper: Boolean,
    hasLower: Boolean,
    hasNumbers: Boolean,
    hasSymbols: Boolean
): PasswordStrength {
    var poolSize = 0
    if (hasUpper) poolSize += 26
    if (hasLower) poolSize += 26
    if (hasNumbers) poolSize += 10
    if (hasSymbols) poolSize += 30

    if (poolSize == 0) return PasswordStrength.WEAK

    val entropy = length * kotlin.math.log2(poolSize.toDouble())

    return when {
        entropy < 28 -> PasswordStrength.WEAK
        entropy < 36 -> PasswordStrength.FAIR
        entropy < 60 -> PasswordStrength.GOOD
        entropy < 128 -> PasswordStrength.STRONG
        else -> PasswordStrength.VERY_STRONG
    }
}

enum class PasswordStrength(val label: String, val progress: Float) {
    WEAK("Weak", 0.2f),
    FAIR("Fair", 0.4f),
    GOOD("Good", 0.6f),
    STRONG("Strong", 0.8f),
    VERY_STRONG("Very Strong", 1f)
}

@Composable
private fun PasswordStrengthIndicator(strength: PasswordStrength) {
    val color = when (strength) {
        PasswordStrength.WEAK -> MaterialTheme.colorScheme.error
        PasswordStrength.FAIR -> MaterialTheme.colorScheme.tertiary
        PasswordStrength.GOOD -> MaterialTheme.colorScheme.primary
        PasswordStrength.STRONG -> MaterialTheme.colorScheme.secondary
        PasswordStrength.VERY_STRONG -> MaterialTheme.colorScheme.primary
    }

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
                Text("Password Strength", style = MaterialTheme.typography.labelMedium)
                Text(
                    strength.label,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }
            Spacer(Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { strength.progress },
                modifier = Modifier.fillMaxWidth(),
                color = color,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}
