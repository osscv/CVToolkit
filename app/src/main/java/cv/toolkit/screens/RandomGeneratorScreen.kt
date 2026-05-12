package cv.toolkit.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cv.toolkit.ads.BannerAd
import kotlinx.coroutines.launch
import java.security.SecureRandom
import java.util.*

private enum class GeneratorTab(val label: String) {
    Numbers("Numbers"),
    Strings("Strings"),
    Colors("Colors"),
    UUID("UUID"),
    Dice("Dice")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RandomGeneratorScreen(navController: NavController) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabs = GeneratorTab.entries

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Random Generator") },
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
        ) {
            PrimaryScrollableTabRow(
                selectedTabIndex = selectedTab,
                modifier = Modifier.fillMaxWidth(),
                edgePadding = 0.dp,
                divider = {}
            ) {
                tabs.forEachIndexed { index, tab ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(tab.label) }
                    )
                }
            }

            when (tabs[selectedTab]) {
                GeneratorTab.Numbers -> NumbersGenerator()
                GeneratorTab.Strings -> StringsGenerator()
                GeneratorTab.Colors -> ColorsGenerator()
                GeneratorTab.UUID -> UuidGenerator()
                GeneratorTab.Dice -> DiceGenerator()
            }
        }
    }
}

// ─── Numbers Generator ──────────────────────────────────────────────────────────

@Composable
private fun NumbersGenerator() {
    var minValue by remember { mutableStateOf("0") }
    var maxValue by remember { mutableStateOf("100") }
    var count by remember { mutableStateOf("1") }
    var allowDecimals by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf(listOf<String>()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val random = remember { SecureRandom() }

    fun generate() {
        errorMessage = null
        val min = minValue.toDoubleOrNull()
        val max = maxValue.toDoubleOrNull()
        val cnt = count.toIntOrNull()?.coerceIn(1, 100)

        if (min == null || max == null || cnt == null) {
            errorMessage = "Please enter valid numbers"
            return
        }
        if (min >= max) {
            errorMessage = "Min must be less than Max"
            return
        }

        results = (1..cnt).map {
            if (allowDecimals) {
                val value = min + random.nextDouble() * (max - min)
                "%.4f".format(value)
            } else {
                val minInt = min.toLong()
                val maxInt = max.toLong()
                val value = minInt + (random.nextDouble() * (maxInt - minInt + 1)).toLong()
                value.coerceIn(minInt, maxInt).toString()
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = minValue,
                        onValueChange = { minValue = it },
                        label = { Text("Min") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = maxValue,
                        onValueChange = { maxValue = it },
                        label = { Text("Max") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                OutlinedTextField(
                    value = count,
                    onValueChange = { count = it },
                    label = { Text("Count (1-100)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Allow Decimals", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = allowDecimals, onCheckedChange = { allowDecimals = it })
                }
            }
        }

        if (errorMessage != null) {
            Text(errorMessage!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Button(
            onClick = { generate() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Generate Numbers")
        }

        ResultsList(
            results = results,
            label = "number",
            modifier = Modifier.weight(1f)
        )

        BannerAd(modifier = Modifier.fillMaxWidth())
    }
}

// ─── Strings Generator ──────────────────────────────────────────────────────────

@Composable
private fun StringsGenerator() {
    var length by remember { mutableStateOf("16") }
    var count by remember { mutableStateOf("1") }
    var includeUppercase by remember { mutableStateOf(true) }
    var includeLowercase by remember { mutableStateOf(true) }
    var includeDigits by remember { mutableStateOf(true) }
    var includeSpecial by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf(listOf<String>()) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val random = remember { SecureRandom() }

    fun generate() {
        errorMessage = null
        val len = length.toIntOrNull()?.coerceIn(1, 256)
        val cnt = count.toIntOrNull()?.coerceIn(1, 100)

        if (len == null || cnt == null) {
            errorMessage = "Please enter valid numbers"
            return
        }

        val charPool = buildString {
            if (includeUppercase) append("ABCDEFGHIJKLMNOPQRSTUVWXYZ")
            if (includeLowercase) append("abcdefghijklmnopqrstuvwxyz")
            if (includeDigits) append("0123456789")
            if (includeSpecial) append("!@#\$%^&*()_+-=[]{}|;:,.<>?")
        }

        if (charPool.isEmpty()) {
            errorMessage = "Select at least one character set"
            return
        }

        results = (1..cnt).map {
            buildString {
                repeat(len) {
                    append(charPool[random.nextInt(charPool.length)])
                }
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedTextField(
                        value = length,
                        onValueChange = { length = it },
                        label = { Text("Length") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = count,
                        onValueChange = { count = it },
                        label = { Text("Count (1-100)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }

                Text("Character Sets", style = MaterialTheme.typography.labelLarge)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
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
                            Checkbox(checked = includeDigits, onCheckedChange = { includeDigits = it })
                            Text("0-9", style = MaterialTheme.typography.bodySmall)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(checked = includeSpecial, onCheckedChange = { includeSpecial = it })
                            Text("!@#\$%", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        if (errorMessage != null) {
            Text(errorMessage!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Button(
            onClick = { generate() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Generate Strings")
        }

        ResultsList(
            results = results,
            label = "string",
            modifier = Modifier.weight(1f)
        )

        BannerAd(modifier = Modifier.fillMaxWidth())
    }
}

// ─── Colors Generator ───────────────────────────────────────────────────────────

@Composable
private fun ColorsGenerator() {
    var count by remember { mutableStateOf("5") }
    var results by remember { mutableStateOf(listOf<String>()) }
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val random = remember { SecureRandom() }

    fun generate() {
        val cnt = (count.toIntOrNull() ?: 5).coerceIn(1, 50)
        results = (1..cnt).map {
            String.format("#%06X", random.nextInt(0xFFFFFF + 1))
        }
    }

    LaunchedEffect(Unit) { generate() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = count,
                    onValueChange = { count = it },
                    label = { Text("Number of Colors (1-50)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Button(
            onClick = { generate() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Generate Colors")
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            if (results.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Tap Generate to create random colors", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(results) { hex ->
                        val color = try {
                            Color(android.graphics.Color.parseColor(hex))
                        } catch (_: Exception) {
                            Color.Gray
                        }

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(color)
                                        .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                )
                                Text(
                                    hex,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.weight(1f)
                                )
                                IconButton(
                                    onClick = {
                                        scope.launch {
                                            clipboard.setClipEntry(
                                                androidx.compose.ui.platform.ClipEntry(
                                                    android.content.ClipData.newPlainText("color", hex)
                                                )
                                            )
                                        }
                                    }
                                ) {
                                    Icon(Icons.Filled.ContentCopy, "Copy", modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        }

        BannerAd(modifier = Modifier.fillMaxWidth())
    }
}

// ─── UUID Generator ─────────────────────────────────────────────────────────────

@Composable
private fun UuidGenerator() {
    var count by remember { mutableStateOf("1") }
    var results by remember { mutableStateOf(listOf<String>()) }
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    fun generate() {
        val cnt = (count.toIntOrNull() ?: 1).coerceIn(1, 100)
        results = (1..cnt).map { UUID.randomUUID().toString() }
    }

    LaunchedEffect(Unit) { generate() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                OutlinedTextField(
                    value = count,
                    onValueChange = { count = it },
                    label = { Text("Count (1-100)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Button(
            onClick = { generate() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.Refresh, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Generate UUIDs")
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            if (results.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Tap Generate to create UUIDs", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(results) { uuid ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                uuid,
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        clipboard.setClipEntry(
                                            androidx.compose.ui.platform.ClipEntry(
                                                android.content.ClipData.newPlainText("uuid", uuid)
                                            )
                                        )
                                    }
                                }
                            ) {
                                Icon(Icons.Filled.ContentCopy, "Copy", modifier = Modifier.size(20.dp))
                            }
                        }
                        if (results.last() != uuid) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }

        BannerAd(modifier = Modifier.fillMaxWidth())
    }
}

// ─── Dice Generator ─────────────────────────────────────────────────────────────

private data class DieType(val label: String, val sides: Int)

private val diceTypes = listOf(
    DieType("d4", 4),
    DieType("d6", 6),
    DieType("d8", 8),
    DieType("d10", 10),
    DieType("d12", 12),
    DieType("d20", 20)
)

@Composable
private fun DiceGenerator() {
    var selectedDie by remember { mutableIntStateOf(1) } // default d6
    var numberOfDice by remember { mutableStateOf("1") }
    var results by remember { mutableStateOf(listOf<Int>()) }
    var total by remember { mutableIntStateOf(0) }
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    val random = remember { SecureRandom() }

    fun roll() {
        val die = diceTypes[selectedDie]
        val cnt = (numberOfDice.toIntOrNull() ?: 1).coerceIn(1, 100)
        results = (1..cnt).map { random.nextInt(die.sides) + 1 }
        total = results.sum()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Die Type", style = MaterialTheme.typography.labelLarge)

                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    diceTypes.forEachIndexed { index, die ->
                        SegmentedButton(
                            selected = selectedDie == index,
                            onClick = { selectedDie = index },
                            shape = SegmentedButtonDefaults.itemShape(index, diceTypes.size)
                        ) {
                            Text(die.label, style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                OutlinedTextField(
                    value = numberOfDice,
                    onValueChange = { numberOfDice = it },
                    label = { Text("Number of Dice (1-100)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Button(
            onClick = { roll() },
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Filled.Casino, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Roll ${diceTypes[selectedDie].label}")
        }

        if (results.isNotEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Total", style = MaterialTheme.typography.labelMedium)
                        Text(
                            "$total",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    IconButton(
                        onClick = {
                            scope.launch {
                                val text = results.joinToString(", ") + " (Total: $total)"
                                clipboard.setClipEntry(
                                    androidx.compose.ui.platform.ClipEntry(
                                        android.content.ClipData.newPlainText("dice", text)
                                    )
                                )
                            }
                        }
                    ) {
                        Icon(Icons.Filled.ContentCopy, "Copy All", modifier = Modifier.size(20.dp))
                    }
                }
            }
        }

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            if (results.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Tap Roll to throw some dice", style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(results.size) { index ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "Die ${index + 1}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                "${results[index]}",
                                style = MaterialTheme.typography.bodyLarge,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        if (index < results.size - 1) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }

        BannerAd(modifier = Modifier.fillMaxWidth())
    }
}

// ─── Shared Results List ────────────────────────────────────────────────────────

@Composable
private fun ResultsList(
    results: List<String>,
    label: String,
    modifier: Modifier = Modifier
) {
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        if (results.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Tap Generate to create results", style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${results.size} Result${if (results.size > 1) "s" else ""}",
                        style = MaterialTheme.typography.labelMedium
                    )
                    if (results.size > 1) {
                        IconButton(
                            onClick = {
                                scope.launch {
                                    clipboard.setClipEntry(
                                        androidx.compose.ui.platform.ClipEntry(
                                            android.content.ClipData.newPlainText(label, results.joinToString("\n"))
                                        )
                                    )
                                }
                            }
                        ) {
                            Icon(Icons.Filled.CopyAll, "Copy All", modifier = Modifier.size(20.dp))
                        }
                    }
                }
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(results) { result ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                result,
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = FontFamily.Monospace,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        clipboard.setClipEntry(
                                            androidx.compose.ui.platform.ClipEntry(
                                                android.content.ClipData.newPlainText(label, result)
                                            )
                                        )
                                    }
                                }
                            ) {
                                Icon(Icons.Filled.ContentCopy, "Copy", modifier = Modifier.size(20.dp))
                            }
                        }
                        if (results.last() != result) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                        }
                    }
                }
            }
        }
    }
}
