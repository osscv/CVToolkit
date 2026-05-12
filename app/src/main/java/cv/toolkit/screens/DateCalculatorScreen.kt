package cv.toolkit.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import cv.toolkit.ads.BannerAd
import java.text.SimpleDateFormat
import java.util.*

// ─── Helpers ─────────────────────────────────────────────────────────────────

private fun formatDate(cal: Calendar): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(cal.time)
}

private fun calendarOf(year: Int, month: Int, day: Int): Calendar {
    return Calendar.getInstance().apply {
        set(Calendar.YEAR, year)
        set(Calendar.MONTH, month)
        set(Calendar.DAY_OF_MONTH, day)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
}

private fun calendarFromMillis(millis: Long): Calendar {
    return Calendar.getInstance().apply {
        timeInMillis = millis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }
}

private fun daysBetween(start: Calendar, end: Calendar): Long {
    val startMs = start.clone() as Calendar
    val endMs = end.clone() as Calendar
    startMs.set(Calendar.HOUR_OF_DAY, 0); startMs.set(Calendar.MINUTE, 0)
    startMs.set(Calendar.SECOND, 0); startMs.set(Calendar.MILLISECOND, 0)
    endMs.set(Calendar.HOUR_OF_DAY, 0); endMs.set(Calendar.MINUTE, 0)
    endMs.set(Calendar.SECOND, 0); endMs.set(Calendar.MILLISECOND, 0)
    val diffMs = endMs.timeInMillis - startMs.timeInMillis
    return diffMs / (24 * 60 * 60 * 1000)
}

private fun calculateYearsMonthsDays(start: Calendar, end: Calendar): Triple<Int, Int, Int> {
    // Ensure start <= end
    val (s, e) = if (start.before(end)) Pair(start.clone() as Calendar, end.clone() as Calendar)
    else Pair(end.clone() as Calendar, start.clone() as Calendar)

    var years = e.get(Calendar.YEAR) - s.get(Calendar.YEAR)
    var months = e.get(Calendar.MONTH) - s.get(Calendar.MONTH)
    var days = e.get(Calendar.DAY_OF_MONTH) - s.get(Calendar.DAY_OF_MONTH)

    if (days < 0) {
        months--
        val temp = s.clone() as Calendar
        temp.add(Calendar.YEAR, years)
        temp.add(Calendar.MONTH, months + 1) // go one month ahead
        // days in previous month
        val prevMonth = temp.clone() as Calendar
        prevMonth.add(Calendar.MONTH, -1)
        val daysInPrevMonth = prevMonth.getActualMaximum(Calendar.DAY_OF_MONTH)
        days += daysInPrevMonth
    }
    if (months < 0) {
        years--
        months += 12
    }
    return Triple(years, months, days)
}

private fun getDayOfWeekName(cal: Calendar): String {
    val sdf = SimpleDateFormat("EEEE", Locale.getDefault())
    return sdf.format(cal.time)
}

// ─── Screen ──────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateCalculatorScreen(navController: NavController) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("Days Between", "Add/Subtract", "Age Calculator")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Date Calculator") },
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
                edgePadding = 16.dp,
                divider = {}
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title) }
                    )
                }
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                when (selectedTab) {
                    0 -> DaysBetweenTab()
                    1 -> AddSubtractTab()
                    2 -> AgeCalculatorTab()
                }
            }

            BannerAd(modifier = Modifier.fillMaxWidth())
        }
    }
}

// ─── Tab 1: Days Between Dates ───────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DaysBetweenTab() {
    val today = Calendar.getInstance()
    var startDate by remember {
        mutableStateOf(calendarOf(today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH)))
    }
    var endDate by remember {
        mutableStateOf(calendarOf(today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH)))
    }
    var includeEndDate by remember { mutableStateOf(false) }
    var showStartPicker by remember { mutableStateOf(false) }
    var showEndPicker by remember { mutableStateOf(false) }

    // Date picker dialogs
    if (showStartPicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = startDate.timeInMillis
        )
        DatePickerDialog(
            onDismissRequest = { showStartPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        startDate = calendarFromMillis(millis)
                    }
                    showStartPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showStartPicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = state)
        }
    }

    if (showEndPicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = endDate.timeInMillis
        )
        DatePickerDialog(
            onDismissRequest = { showEndPicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        endDate = calendarFromMillis(millis)
                    }
                    showEndPicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showEndPicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = state)
        }
    }

    // Start date
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Start Date", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { showStartPicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(formatDate(startDate))
            }
        }
    }

    // End date
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("End Date", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { showEndPicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(formatDate(endDate))
            }
        }
    }

    // Include end date toggle
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Include end date", style = MaterialTheme.typography.bodyMedium)
            Switch(checked = includeEndDate, onCheckedChange = { includeEndDate = it })
        }
    }

    // Results
    val rawDays = daysBetween(startDate, endDate)
    val totalDays = kotlin.math.abs(rawDays) + if (includeEndDate) 1 else 0
    val (years, months, days) = calculateYearsMonthsDays(startDate, endDate)

    val weeksWhole = totalDays / 7
    val weeksDaysRemainder = totalDays % 7

    // Months + days breakdown
    val adjustedMonths = if (includeEndDate && days == 0 && (years > 0 || months > 0)) {
        // edge case: exact month boundary with include end date
        months
    } else months
    val adjustedDays = if (includeEndDate) days + 1 else days
    val carryMonth = if (adjustedDays >= 30) 1 else 0
    val finalMonths = adjustedMonths + carryMonth + (years * 12)
    val finalDaysInMonthBreakdown = if (carryMonth > 0) adjustedDays - 30 else adjustedDays

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Results", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            HorizontalDivider()

            ResultRow("Total Days", "$totalDays days")
            ResultRow("Weeks + Days", "$weeksWhole weeks, $weeksDaysRemainder days")

            val yearsAdj = if (includeEndDate) {
                calculateYearsMonthsDays(
                    startDate,
                    (endDate.clone() as Calendar).apply { add(Calendar.DAY_OF_MONTH, 1) }
                )
            } else {
                Triple(years, months, days)
            }
            ResultRow(
                "Years + Months + Days",
                "${yearsAdj.first} years, ${yearsAdj.second} months, ${yearsAdj.third} days"
            )

            if (rawDays < 0) {
                Text(
                    "Note: Start date is after end date",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

// ─── Tab 2: Add / Subtract Days ──────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddSubtractTab() {
    val today = Calendar.getInstance()
    var startDate by remember {
        mutableStateOf(calendarOf(today.get(Calendar.YEAR), today.get(Calendar.MONTH), today.get(Calendar.DAY_OF_MONTH)))
    }
    var showDatePicker by remember { mutableStateOf(false) }
    var isAdd by remember { mutableStateOf(true) }
    var daysInput by remember { mutableStateOf("0") }
    var weeksInput by remember { mutableStateOf("0") }
    var monthsInput by remember { mutableStateOf("0") }
    var yearsInput by remember { mutableStateOf("0") }

    if (showDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = startDate.timeInMillis
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        startDate = calendarFromMillis(millis)
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = state)
        }
    }

    // Start date
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Start Date", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(formatDate(startDate))
            }
        }
    }

    // Add / Subtract toggle
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Operation", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = isAdd,
                    onClick = { isAdd = true },
                    label = { Text("Add") },
                    leadingIcon = if (isAdd) {
                        { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    } else null,
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = !isAdd,
                    onClick = { isAdd = false },
                    label = { Text("Subtract") },
                    leadingIcon = if (!isAdd) {
                        { Icon(Icons.Default.Remove, contentDescription = null, modifier = Modifier.size(18.dp)) }
                    } else null,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }

    // Duration inputs
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Duration", style = MaterialTheme.typography.labelLarge)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = yearsInput,
                    onValueChange = { yearsInput = it.filter { c -> c.isDigit() } },
                    label = { Text("Years") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = monthsInput,
                    onValueChange = { monthsInput = it.filter { c -> c.isDigit() } },
                    label = { Text("Months") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = weeksInput,
                    onValueChange = { weeksInput = it.filter { c -> c.isDigit() } },
                    label = { Text("Weeks") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = daysInput,
                    onValueChange = { daysInput = it.filter { c -> c.isDigit() } },
                    label = { Text("Days") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
            }
        }
    }

    // Result
    val resultDate = remember(startDate, isAdd, daysInput, weeksInput, monthsInput, yearsInput) {
        try {
            val d = daysInput.toIntOrNull() ?: 0
            val w = weeksInput.toIntOrNull() ?: 0
            val m = monthsInput.toIntOrNull() ?: 0
            val y = yearsInput.toIntOrNull() ?: 0
            val sign = if (isAdd) 1 else -1

            (startDate.clone() as Calendar).apply {
                add(Calendar.YEAR, y * sign)
                add(Calendar.MONTH, m * sign)
                add(Calendar.DAY_OF_MONTH, (w * 7 + d) * sign)
            }
        } catch (_: Exception) {
            null
        }
    }

    resultDate?.let { result ->
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Result", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                HorizontalDivider()

                Text(
                    formatDate(result),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )

                Text(
                    getDayOfWeekName(result),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )

                val totalDiff = kotlin.math.abs(daysBetween(startDate, result))
                Text(
                    "$totalDiff days ${if (isAdd) "after" else "before"} start date",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

// ─── Tab 3: Age Calculator ───────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AgeCalculatorTab() {
    val today = Calendar.getInstance()
    var birthDate by remember {
        mutableStateOf(calendarOf(2000, Calendar.JANUARY, 1))
    }
    var showDatePicker by remember { mutableStateOf(false) }

    if (showDatePicker) {
        val state = rememberDatePickerState(
            initialSelectedDateMillis = birthDate.timeInMillis
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    state.selectedDateMillis?.let { millis ->
                        birthDate = calendarFromMillis(millis)
                    }
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancel") }
            }
        ) {
            DatePicker(state = state)
        }
    }

    // Birth date
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Date of Birth", style = MaterialTheme.typography.labelLarge)
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { showDatePicker = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Cake, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(formatDate(birthDate))
            }
        }
    }

    // Validation
    if (!birthDate.before(today)) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
        ) {
            Text(
                "Please select a birth date in the past",
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        return
    }

    // Age calculation
    val (years, months, days) = calculateYearsMonthsDays(birthDate, today)
    val totalDaysLived = daysBetween(birthDate, today)
    val dayOfWeekBorn = getDayOfWeekName(birthDate)

    // Next birthday
    val nextBirthday = (birthDate.clone() as Calendar).apply {
        set(Calendar.YEAR, today.get(Calendar.YEAR))
        if (!after(today)) {
            add(Calendar.YEAR, 1)
        }
    }
    val daysUntilBirthday = daysBetween(today, nextBirthday)

    // Exact age
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Your Age", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            HorizontalDivider()

            Text(
                "$years years, $months months, $days days",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }

    // Details
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Details", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            HorizontalDivider()

            ResultRow("Total Days Lived", "$totalDaysLived days")
            ResultRow("Total Weeks Lived", "${totalDaysLived / 7} weeks")
            ResultRow("Total Months", "${years * 12 + months} months")
            ResultRow("Born on", dayOfWeekBorn)
        }
    }

    // Next birthday
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Next Birthday", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            HorizontalDivider()

            Text(
                formatDate(nextBirthday),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )

            val turningAge = nextBirthday.get(Calendar.YEAR) - birthDate.get(Calendar.YEAR)
            Text(
                "Turning $turningAge in $daysUntilBirthday days",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
            )

            Text(
                getDayOfWeekName(nextBirthday),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
            )
        }
    }
}

// ─── Shared Components ───────────────────────────────────────────────────────

@Composable
private fun ResultRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold
        )
    }
}
