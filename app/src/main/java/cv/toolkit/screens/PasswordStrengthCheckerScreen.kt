package cv.toolkit.screens

import androidx.compose.animation.animateContentSize
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cv.toolkit.R
import cv.toolkit.ads.BannerAd
import kotlin.math.log2
import kotlin.math.pow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PasswordStrengthCheckerScreen(navController: NavController) {
    var password by remember { mutableStateOf("") }
    var showPassword by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.password_strength_checker_title)) },
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
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Password input
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Enter password to check") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                leadingIcon = { Icon(Icons.Filled.Lock, null) },
                trailingIcon = {
                    Row {
                        IconButton(onClick = { showPassword = !showPassword }) {
                            Icon(
                                if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                if (showPassword) "Hide" else "Show"
                            )
                        }
                        if (password.isNotEmpty()) {
                            IconButton(onClick = { password = "" }) {
                                Icon(Icons.Filled.Clear, "Clear")
                            }
                        }
                    }
                }
            )

            if (password.isNotEmpty()) {
                val analysis = analyzePassword(password)

                // Strength meter
                StrengthMeterCard(analysis)

                // Entropy & crack time
                EntropyCard(analysis)

                // Character composition
                CompositionCard(analysis)

                // Checks & feedback
                FeedbackCard(analysis)
            } else {
                // Empty state
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
                        Icon(
                            Icons.Filled.Shield,
                            null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            "Enter a password above to analyze its strength",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            Spacer(Modifier.weight(1f))
            BannerAd(modifier = Modifier.fillMaxWidth())
        }
    }
}

// --- Data ---

private enum class StrengthLevel(val label: String, val score: Int) {
    VERY_WEAK("Very Weak", 0),
    WEAK("Weak", 1),
    FAIR("Fair", 2),
    GOOD("Good", 3),
    STRONG("Strong", 4),
    VERY_STRONG("Very Strong", 5)
}

private data class PasswordAnalysis(
    val password: String,
    val strength: StrengthLevel,
    val entropy: Double,
    val crackTime: String,
    val poolSize: Int,
    val length: Int,
    val hasUppercase: Boolean,
    val hasLowercase: Boolean,
    val hasDigits: Boolean,
    val hasSymbols: Boolean,
    val hasSpaces: Boolean,
    val uppercaseCount: Int,
    val lowercaseCount: Int,
    val digitCount: Int,
    val symbolCount: Int,
    val spaceCount: Int,
    val uniqueChars: Int,
    val checks: List<PasswordCheck>
)

private data class PasswordCheck(
    val label: String,
    val passed: Boolean,
    val detail: String
)

// --- Analysis logic ---

private fun analyzePassword(password: String): PasswordAnalysis {
    val length = password.length
    val hasUpper = password.any { it.isUpperCase() }
    val hasLower = password.any { it.isLowerCase() }
    val hasDigit = password.any { it.isDigit() }
    val hasSymbol = password.any { !it.isLetterOrDigit() && !it.isWhitespace() }
    val hasSpace = password.any { it.isWhitespace() }

    val upperCount = password.count { it.isUpperCase() }
    val lowerCount = password.count { it.isLowerCase() }
    val digitCount = password.count { it.isDigit() }
    val symbolCount = password.count { !it.isLetterOrDigit() && !it.isWhitespace() }
    val spaceCount = password.count { it.isWhitespace() }
    val uniqueChars = password.toSet().size

    // Pool size calculation
    var pool = 0
    if (hasUpper) pool += 26
    if (hasLower) pool += 26
    if (hasDigit) pool += 10
    if (hasSymbol) pool += 33
    if (hasSpace) pool += 1
    if (pool == 0) pool = 1

    val entropy = if (length > 0) length * log2(pool.toDouble()) else 0.0

    // Crack time estimation (10 billion guesses/sec)
    val guessesPerSecond = 1e10
    val combinations = if (entropy > 0) 2.0.pow(entropy) else 1.0
    val seconds = combinations / guessesPerSecond
    val crackTime = formatCrackTime(seconds)

    // Checks
    val checks = buildList {
        add(PasswordCheck("Length >= 8", length >= 8, "$length characters"))
        add(PasswordCheck("Length >= 12", length >= 12, if (length >= 12) "Good length" else "Consider 12+ characters"))
        add(PasswordCheck("Uppercase letters", hasUpper, if (hasUpper) "$upperCount found" else "None found"))
        add(PasswordCheck("Lowercase letters", hasLower, if (hasLower) "$lowerCount found" else "None found"))
        add(PasswordCheck("Numbers", hasDigit, if (hasDigit) "$digitCount found" else "None found"))
        add(PasswordCheck("Special characters", hasSymbol, if (hasSymbol) "$symbolCount found" else "None found"))
        add(PasswordCheck("No repeated chars (>3)", !hasRepeatedChars(password), if (hasRepeatedChars(password)) "Sequential repeats detected" else "No excessive repeats"))
        add(PasswordCheck("No common sequences", !hasCommonSequences(password), if (hasCommonSequences(password)) "Common sequence found" else "No common sequences"))
        add(PasswordCheck("Not a common password", !isCommonPassword(password), if (isCommonPassword(password)) "This is a commonly used password" else "Not in common list"))
        add(PasswordCheck("Unique characters >= 50%", uniqueChars.toFloat() / length >= 0.5f, "$uniqueChars unique out of $length"))
    }

    // Strength scoring
    var score = 0
    if (length >= 8) score++
    if (length >= 12) score++
    if (hasUpper && hasLower) score++
    if (hasDigit) score++
    if (hasSymbol) score++
    if (entropy >= 60) score++
    if (uniqueChars.toFloat() / length >= 0.5f) score++

    // Penalties
    if (isCommonPassword(password)) score = 0
    if (hasCommonSequences(password)) score = (score - 1).coerceAtLeast(0)
    if (hasRepeatedChars(password)) score = (score - 1).coerceAtLeast(0)
    if (length < 6) score = score.coerceAtMost(1)

    val strength = when {
        score <= 1 -> StrengthLevel.VERY_WEAK
        score == 2 -> StrengthLevel.WEAK
        score == 3 -> StrengthLevel.FAIR
        score == 4 -> StrengthLevel.GOOD
        score == 5 -> StrengthLevel.STRONG
        else -> StrengthLevel.VERY_STRONG
    }

    return PasswordAnalysis(
        password = password,
        strength = strength,
        entropy = entropy,
        crackTime = crackTime,
        poolSize = pool,
        length = length,
        hasUppercase = hasUpper,
        hasLowercase = hasLower,
        hasDigits = hasDigit,
        hasSymbols = hasSymbol,
        hasSpaces = hasSpace,
        uppercaseCount = upperCount,
        lowercaseCount = lowerCount,
        digitCount = digitCount,
        symbolCount = symbolCount,
        spaceCount = spaceCount,
        uniqueChars = uniqueChars,
        checks = checks
    )
}

private fun hasRepeatedChars(password: String): Boolean {
    if (password.length < 4) return false
    for (i in 0..password.length - 4) {
        if (password[i] == password[i + 1] && password[i + 1] == password[i + 2] && password[i + 2] == password[i + 3]) return true
    }
    return false
}

private fun hasCommonSequences(password: String): Boolean {
    val sequences = listOf(
        "1234", "2345", "3456", "4567", "5678", "6789", "0123",
        "abcd", "bcde", "cdef", "defg", "efgh", "fghi",
        "qwer", "wert", "erty", "asdf", "sdfg", "zxcv",
        "4321", "3210", "9876", "8765", "7654", "6543",
        "dcba", "fedc", "edcb"
    )
    val lower = password.lowercase()
    return sequences.any { lower.contains(it) }
}

private fun isCommonPassword(password: String): Boolean {
    val common = setOf(
        "password", "123456", "12345678", "qwerty", "abc123", "monkey", "1234567",
        "letmein", "trustno1", "dragon", "baseball", "iloveyou", "master", "sunshine",
        "ashley", "michael", "shadow", "123123", "654321", "superman", "qazwsx",
        "password1", "password123", "admin", "welcome", "hello", "charlie", "donald",
        "football", "access", "thunder", "god", "love", "money", "test", "pass",
        "1234", "12345", "123456789", "1234567890", "000000", "111111",
        "abcdef", "abcabc", "aaaaaa", "passw0rd", "p@ssword", "p@ssw0rd"
    )
    return password.lowercase() in common
}

private fun formatCrackTime(seconds: Double): String {
    return when {
        seconds < 0.001 -> "Instantly"
        seconds < 1 -> "Less than a second"
        seconds < 60 -> "${seconds.toLong()} seconds"
        seconds < 3600 -> "${(seconds / 60).toLong()} minutes"
        seconds < 86400 -> "${(seconds / 3600).toLong()} hours"
        seconds < 86400 * 30 -> "${(seconds / 86400).toLong()} days"
        seconds < 86400 * 365 -> "${(seconds / (86400 * 30)).toLong()} months"
        seconds < 86400 * 365 * 1000 -> "${(seconds / (86400 * 365)).toLong()} years"
        seconds < 86400 * 365 * 1e6 -> "${(seconds / (86400 * 365 * 1000)).toLong()}k years"
        seconds < 86400 * 365 * 1e9 -> "${(seconds / (86400 * 365 * 1e6)).toLong()}M years"
        seconds < 86400 * 365 * 1e12 -> "${(seconds / (86400 * 365 * 1e9)).toLong()}B years"
        else -> "Centuries+"
    }
}

// --- UI Components ---

@Composable
private fun StrengthMeterCard(analysis: PasswordAnalysis) {
    val strength = analysis.strength
    val progress = (strength.score + 1) / 6f
    val color = strengthColor(strength)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .animateContentSize()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        when (strength) {
                            StrengthLevel.VERY_WEAK, StrengthLevel.WEAK -> Icons.Filled.ErrorOutline
                            StrengthLevel.FAIR -> Icons.Filled.Warning
                            StrengthLevel.GOOD -> Icons.Filled.CheckCircle
                            StrengthLevel.STRONG, StrengthLevel.VERY_STRONG -> Icons.Filled.VerifiedUser
                        },
                        null,
                        tint = color,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Strength", style = MaterialTheme.typography.titleMedium)
                }
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = color.copy(alpha = 0.15f)
                ) {
                    Text(
                        strength.label,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = color
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = color,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }
    }
}

@Composable
private fun EntropyCard(analysis: PasswordAnalysis) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Security Metrics", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricChip(
                    icon = Icons.Filled.Shuffle,
                    label = "Entropy",
                    value = "${"%.1f".format(analysis.entropy)} bits",
                    modifier = Modifier.weight(1f)
                )
                MetricChip(
                    icon = Icons.Filled.Timer,
                    label = "Crack Time",
                    value = analysis.crackTime,
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MetricChip(
                    icon = Icons.Filled.Dataset,
                    label = "Pool Size",
                    value = "${analysis.poolSize} chars",
                    modifier = Modifier.weight(1f)
                )
                MetricChip(
                    icon = Icons.Filled.Fingerprint,
                    label = "Unique Chars",
                    value = "${analysis.uniqueChars} / ${analysis.length}",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "* Crack time assumes 10 billion guesses/sec (offline attack)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
        }
    }
}

@Composable
private fun MetricChip(icon: ImageVector, label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(icon, null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(4.dp))
                Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace)
        }
    }
}

@Composable
private fun CompositionCard(analysis: PasswordAnalysis) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Character Composition", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))

            val total = analysis.length.toFloat().coerceAtLeast(1f)
            CompositionRow("Uppercase (A-Z)", analysis.uppercaseCount, total, MaterialTheme.colorScheme.primary)
            CompositionRow("Lowercase (a-z)", analysis.lowercaseCount, total, MaterialTheme.colorScheme.secondary)
            CompositionRow("Digits (0-9)", analysis.digitCount, total, MaterialTheme.colorScheme.tertiary)
            CompositionRow("Symbols (!@#)", analysis.symbolCount, total, MaterialTheme.colorScheme.error)
            if (analysis.spaceCount > 0) {
                CompositionRow("Spaces", analysis.spaceCount, total, MaterialTheme.colorScheme.outline)
            }
        }
    }
}

@Composable
private fun CompositionRow(label: String, count: Int, total: Float, color: Color) {
    Column(modifier = Modifier.padding(vertical = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodySmall)
            Text(
                "$count (${"%.0f".format(count / total * 100)}%)",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.Monospace
            )
        }
        Spacer(Modifier.height(4.dp))
        LinearProgressIndicator(
            progress = { count / total },
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp),
            color = color,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

@Composable
private fun FeedbackCard(analysis: PasswordAnalysis) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Security Checks", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            analysis.checks.forEach { check ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (check.passed) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                        null,
                        modifier = Modifier.size(20.dp),
                        tint = if (check.passed) Color(0xFF4CAF50) else MaterialTheme.colorScheme.error
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(check.label, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            check.detail,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun strengthColor(strength: StrengthLevel): Color {
    return when (strength) {
        StrengthLevel.VERY_WEAK -> MaterialTheme.colorScheme.error
        StrengthLevel.WEAK -> Color(0xFFFF5722)
        StrengthLevel.FAIR -> Color(0xFFFF9800)
        StrengthLevel.GOOD -> Color(0xFF8BC34A)
        StrengthLevel.STRONG -> Color(0xFF4CAF50)
        StrengthLevel.VERY_STRONG -> Color(0xFF2E7D32)
    }
}
