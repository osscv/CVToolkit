package cv.toolkit.screens

import android.content.ClipData
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import cv.toolkit.ads.BannerAd
import kotlinx.coroutines.launch
import kotlin.math.*

// ── Data types ──────────────────────────────────────────────────────────────

private data class HistoryEntry(val expression: String, val result: String)

private sealed class Token {
    data class Number(val value: Double) : Token()
    data class Op(val symbol: String) : Token()
    data class Func(val name: String) : Token()
    data object LeftParen : Token()
    data object RightParen : Token()
}

private enum class AngleMode { DEG, RAD }

// ── Screen composable ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScientificCalculatorScreen(navController: NavController) {
    var expression by remember { mutableStateOf("") }
    var displayResult by remember { mutableStateOf("0") }
    var history by remember { mutableStateOf(listOf<HistoryEntry>()) }
    var angleMode by remember { mutableStateOf(AngleMode.DEG) }
    var showHistory by remember { mutableStateOf(false) }
    var memory by remember { mutableDoubleStateOf(0.0) }
    var hasMemory by remember { mutableStateOf(false) }
    var lastResult by remember { mutableDoubleStateOf(0.0) }

    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()

    fun evaluateCurrent() {
        if (expression.isBlank()) return
        try {
            val result = evaluateExpression(expression, angleMode)
            val formatted = formatResult(result)
            displayResult = formatted
            lastResult = result
            history = listOf(HistoryEntry(expression, formatted)) + history
        } catch (_: Exception) {
            displayResult = "Error"
        }
    }

    fun appendToExpression(text: String) {
        expression += text
        // Live preview
        try {
            val result = evaluateExpression(expression, angleMode)
            displayResult = formatResult(result)
        } catch (_: Exception) {
            // Keep previous result on display while typing
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scientific Calculator") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showHistory = !showHistory }) {
                        Icon(Icons.Filled.History, "History")
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
            if (showHistory) {
                // ── History panel ────────────────────────────────────────
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("History", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            if (history.isNotEmpty()) {
                                TextButton(onClick = { history = emptyList() }) {
                                    Text("Clear")
                                }
                            }
                        }
                        if (history.isEmpty()) {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("No history yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        } else {
                            LazyColumn(
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(history) { entry ->
                                    Surface(
                                        modifier = Modifier.fillMaxWidth(),
                                        shape = RoundedCornerShape(8.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        onClick = {
                                            expression = entry.expression
                                            displayResult = entry.result
                                            showHistory = false
                                        }
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(
                                                entry.expression,
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                "= ${entry.result}",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                // ── Display area ─────────────────────────────────────────
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        // Memory / Angle mode indicators
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                if (hasMemory) {
                                    Text(
                                        "M",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                angleMode.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.tertiary,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Spacer(Modifier.height(4.dp))

                        // Expression
                        Text(
                            text = expression.ifEmpty { " " },
                            style = MaterialTheme.typography.bodyLarge.copy(fontSize = 20.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.End,
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(Modifier.height(4.dp))

                        // Result
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            IconButton(
                                onClick = {
                                    scope.launch {
                                        clipboard.setClipEntry(
                                            ClipEntry(ClipData.newPlainText("result", displayResult))
                                        )
                                    }
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    Icons.Filled.ContentCopy,
                                    contentDescription = "Copy result",
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = displayResult,
                                style = MaterialTheme.typography.headlineLarge.copy(
                                    fontSize = 36.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                                textAlign = TextAlign.End
                            )
                        }
                    }
                }

                Spacer(Modifier.height(4.dp))

                // ── Button grid ──────────────────────────────────────────
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 6.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Row 1: Memory operations + angle toggle
                    CalcButtonRow {
                        CalcButton("MC", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, Modifier.weight(1f)) {
                            memory = 0.0; hasMemory = false
                        }
                        CalcButton("MR", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, Modifier.weight(1f)) {
                            if (hasMemory) appendToExpression(formatResult(memory))
                        }
                        CalcButton("M+", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, Modifier.weight(1f)) {
                            memory += lastResult; hasMemory = true
                        }
                        CalcButton("M-", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, Modifier.weight(1f)) {
                            memory -= lastResult; hasMemory = true
                        }
                        CalcButton(
                            if (angleMode == AngleMode.DEG) "DEG" else "RAD",
                            MaterialTheme.colorScheme.tertiaryContainer,
                            MaterialTheme.colorScheme.onTertiaryContainer,
                            Modifier.weight(1f)
                        ) {
                            angleMode = if (angleMode == AngleMode.DEG) AngleMode.RAD else AngleMode.DEG
                        }
                    }

                    // Row 2: Scientific functions row 1
                    CalcButtonRow {
                        CalcButton("sin", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, Modifier.weight(1f)) { appendToExpression("sin(") }
                        CalcButton("cos", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, Modifier.weight(1f)) { appendToExpression("cos(") }
                        CalcButton("tan", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, Modifier.weight(1f)) { appendToExpression("tan(") }
                        CalcButton("ln", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, Modifier.weight(1f)) { appendToExpression("ln(") }
                        CalcButton("log", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, Modifier.weight(1f)) { appendToExpression("log(") }
                    }

                    // Row 3: Scientific functions row 2
                    CalcButtonRow {
                        CalcButton("\u221A", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, Modifier.weight(1f)) { appendToExpression("sqrt(") }
                        CalcButton("x\u02B8", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, Modifier.weight(1f)) { appendToExpression("^") }
                        CalcButton("n!", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, Modifier.weight(1f)) { appendToExpression("!") }
                        CalcButton("\u03C0", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, Modifier.weight(1f)) { appendToExpression("\u03C0") }
                        CalcButton("e", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, Modifier.weight(1f)) { appendToExpression("e") }
                    }

                    // Row 4: Parens, percent, negate, backspace
                    CalcButtonRow {
                        CalcButton("(", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, Modifier.weight(1f)) { appendToExpression("(") }
                        CalcButton(")", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, Modifier.weight(1f)) { appendToExpression(")") }
                        CalcButton("%", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, Modifier.weight(1f)) { appendToExpression("%") }
                        CalcButton("+/-", MaterialTheme.colorScheme.secondaryContainer, MaterialTheme.colorScheme.onSecondaryContainer, Modifier.weight(1f)) {
                            expression = if (expression.startsWith("-")) expression.drop(1)
                            else if (expression.startsWith("(-")) expression.drop(2).let { if (it.startsWith(")")) it.drop(1) else it }
                            else "(-$expression"
                            try {
                                val r = evaluateExpression(expression, angleMode)
                                displayResult = formatResult(r)
                            } catch (_: Exception) { }
                        }
                        CalcButton("\u232B", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer, Modifier.weight(1f)) {
                            if (expression.isNotEmpty()) {
                                // If the last chars are a function prefix like "sin(" remove all of it
                                val funcPrefixes = listOf("sin(", "cos(", "tan(", "ln(", "log(", "sqrt(")
                                val removed = funcPrefixes.firstOrNull { expression.endsWith(it) }
                                expression = if (removed != null) expression.dropLast(removed.length)
                                else expression.dropLast(1)
                                try {
                                    val r = evaluateExpression(expression, angleMode)
                                    displayResult = formatResult(r)
                                } catch (_: Exception) { }
                            }
                        }
                    }

                    // Row 5: C, CE, 7, 8, 9, /
                    CalcButtonRow {
                        CalcButton("C", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer, Modifier.weight(1f)) {
                            expression = ""; displayResult = "0"
                        }
                        CalcButton("CE", MaterialTheme.colorScheme.errorContainer, MaterialTheme.colorScheme.onErrorContainer, Modifier.weight(1f)) {
                            // Remove last number segment
                            val trimmed = expression.trimEnd { it.isDigit() || it == '.' }
                            expression = trimmed
                            try {
                                val r = evaluateExpression(expression, angleMode)
                                displayResult = formatResult(r)
                            } catch (_: Exception) { }
                        }
                        CalcButton("7", MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.onSurface, Modifier.weight(1f)) { appendToExpression("7") }
                        CalcButton("8", MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.onSurface, Modifier.weight(1f)) { appendToExpression("8") }
                        CalcButton("9", MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.onSurface, Modifier.weight(1f)) { appendToExpression("9") }
                        CalcButton("\u00F7", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, Modifier.weight(1f)) { appendToExpression("/") }
                    }

                    // Row 6: 4, 5, 6, *
                    CalcButtonRow {
                        Spacer(Modifier.weight(1f))
                        Spacer(Modifier.weight(1f))
                        CalcButton("4", MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.onSurface, Modifier.weight(1f)) { appendToExpression("4") }
                        CalcButton("5", MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.onSurface, Modifier.weight(1f)) { appendToExpression("5") }
                        CalcButton("6", MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.onSurface, Modifier.weight(1f)) { appendToExpression("6") }
                        CalcButton("\u00D7", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, Modifier.weight(1f)) { appendToExpression("*") }
                    }

                    // Row 7: 1, 2, 3, -
                    CalcButtonRow {
                        Spacer(Modifier.weight(1f))
                        Spacer(Modifier.weight(1f))
                        CalcButton("1", MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.onSurface, Modifier.weight(1f)) { appendToExpression("1") }
                        CalcButton("2", MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.onSurface, Modifier.weight(1f)) { appendToExpression("2") }
                        CalcButton("3", MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.onSurface, Modifier.weight(1f)) { appendToExpression("3") }
                        CalcButton("\u2212", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, Modifier.weight(1f)) { appendToExpression("-") }
                    }

                    // Row 8: 0, ., =, +
                    CalcButtonRow {
                        Spacer(Modifier.weight(1f))
                        Spacer(Modifier.weight(1f))
                        CalcButton("0", MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.onSurface, Modifier.weight(1f)) { appendToExpression("0") }
                        CalcButton(".", MaterialTheme.colorScheme.surface, MaterialTheme.colorScheme.onSurface, Modifier.weight(1f)) { appendToExpression(".") }
                        CalcButton("=", MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.onPrimary, Modifier.weight(1f)) { evaluateCurrent() }
                        CalcButton("+", MaterialTheme.colorScheme.primaryContainer, MaterialTheme.colorScheme.onPrimaryContainer, Modifier.weight(1f)) { appendToExpression("+") }
                    }
                }
            }

            BannerAd(modifier = Modifier.fillMaxWidth())
        }
    }
}

// ── UI helper composables ────────────────────────────────────────────────────

@Composable
private fun CalcButtonRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        content = content
    )
}

@Composable
private fun CalcButton(
    text: String,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(48.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        contentPadding = PaddingValues(2.dp)
    ) {
        Text(
            text = text,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1
        )
    }
}

// ── Expression evaluation engine ─────────────────────────────────────────────

private fun formatResult(value: Double): String {
    if (value.isNaN()) return "Error"
    if (value.isInfinite()) return if (value > 0) "\u221E" else "-\u221E"
    // If it's a whole number, show without decimal
    return if (value == value.toLong().toDouble() && value in Long.MIN_VALUE.toDouble()..Long.MAX_VALUE.toDouble()) {
        value.toLong().toString()
    } else {
        // Up to 10 significant decimal places, remove trailing zeros
        "%.10f".format(value).trimEnd('0').trimEnd('.')
    }
}

private fun evaluateExpression(expr: String, angleMode: AngleMode): Double {
    val tokens = tokenize(expr)
    val postfix = shuntingYard(tokens)
    return evaluatePostfix(postfix, angleMode)
}

private fun tokenize(expr: String): List<Token> {
    val tokens = mutableListOf<Token>()
    var i = 0
    val s = expr.trim()

    while (i < s.length) {
        val c = s[i]

        when {
            c.isWhitespace() -> i++

            c.isDigit() || (c == '.' && i + 1 < s.length && s[i + 1].isDigit()) -> {
                val start = i
                while (i < s.length && (s[i].isDigit() || s[i] == '.')) i++
                tokens.add(Token.Number(s.substring(start, i).toDouble()))
            }

            c == '\u03C0' -> { // pi symbol
                tokens.add(Token.Number(PI))
                i++
            }

            c == 'e' && (i + 1 >= s.length || !s[i + 1].isLetter()) -> {
                // Standalone 'e' = Euler's number, but not if it's part of a function name
                tokens.add(Token.Number(E))
                i++
            }

            c == '(' -> { tokens.add(Token.LeftParen); i++ }
            c == ')' -> { tokens.add(Token.RightParen); i++ }

            c == '+' || c == '*' || c == '/' || c == '^' -> {
                tokens.add(Token.Op(c.toString()))
                i++
            }

            c == '-' -> {
                // Determine if unary minus
                val prev = tokens.lastOrNull()
                if (prev == null || prev is Token.Op || prev is Token.LeftParen) {
                    // Unary minus: read the number or insert a negate marker
                    // We'll handle it by inserting 0 - ...
                    tokens.add(Token.Number(0.0))
                    tokens.add(Token.Op("-"))
                } else {
                    tokens.add(Token.Op("-"))
                }
                i++
            }

            c == '%' -> {
                // Percentage: divide previous by 100
                tokens.add(Token.Op("*"))
                tokens.add(Token.Number(0.01))
                i++
            }

            c == '!' -> {
                tokens.add(Token.Op("!"))
                i++
            }

            c.isLetter() -> {
                val start = i
                while (i < s.length && s[i].isLetter()) i++
                val name = s.substring(start, i)
                when (name) {
                    "sin", "cos", "tan", "ln", "log", "sqrt" -> tokens.add(Token.Func(name))
                    "pi" -> tokens.add(Token.Number(PI))
                    "e" -> tokens.add(Token.Number(E))
                    else -> throw IllegalArgumentException("Unknown function: $name")
                }
            }

            else -> throw IllegalArgumentException("Unexpected character: $c")
        }
    }
    return tokens
}

private fun precedence(op: String): Int = when (op) {
    "+", "-" -> 2
    "*", "/" -> 3
    "^" -> 4
    "!" -> 5
    else -> 0
}

private fun isRightAssociative(op: String): Boolean = op == "^"

private fun shuntingYard(tokens: List<Token>): List<Token> {
    val output = mutableListOf<Token>()
    val stack = ArrayDeque<Token>()

    for (token in tokens) {
        when (token) {
            is Token.Number -> output.add(token)

            is Token.Func -> stack.addLast(token)

            is Token.Op -> {
                if (token.symbol == "!") {
                    // Factorial is a postfix unary operator - apply immediately to top of output
                    output.add(token)
                } else {
                    while (stack.isNotEmpty()) {
                        val top = stack.last()
                        if (top is Token.Op && top.symbol != "!" &&
                            (precedence(top.symbol) > precedence(token.symbol) ||
                                    (precedence(top.symbol) == precedence(token.symbol) && !isRightAssociative(token.symbol)))
                        ) {
                            output.add(stack.removeLast())
                        } else break
                    }
                    stack.addLast(token)
                }
            }

            is Token.LeftParen -> stack.addLast(token)

            is Token.RightParen -> {
                while (stack.isNotEmpty() && stack.last() !is Token.LeftParen) {
                    output.add(stack.removeLast())
                }
                if (stack.isNotEmpty() && stack.last() is Token.LeftParen) {
                    stack.removeLast() // Remove '('
                }
                // If top of stack is a function, pop it to output
                if (stack.isNotEmpty() && stack.last() is Token.Func) {
                    output.add(stack.removeLast())
                }
            }
        }
    }

    while (stack.isNotEmpty()) {
        val top = stack.removeLast()
        if (top is Token.LeftParen || top is Token.RightParen) {
            throw IllegalArgumentException("Mismatched parentheses")
        }
        output.add(top)
    }

    return output
}

private fun evaluatePostfix(tokens: List<Token>, angleMode: AngleMode): Double {
    val stack = ArrayDeque<Double>()

    fun toAngle(v: Double): Double = if (angleMode == AngleMode.DEG) Math.toRadians(v) else v

    for (token in tokens) {
        when (token) {
            is Token.Number -> stack.addLast(token.value)

            is Token.Op -> {
                when (token.symbol) {
                    "!" -> {
                        val a = stack.removeLast()
                        stack.addLast(factorial(a))
                    }
                    else -> {
                        val b = stack.removeLast()
                        val a = stack.removeLast()
                        val result = when (token.symbol) {
                            "+" -> a + b
                            "-" -> a - b
                            "*" -> a * b
                            "/" -> {
                                if (b == 0.0) Double.NaN
                                else a / b
                            }
                            "^" -> a.pow(b)
                            else -> throw IllegalArgumentException("Unknown operator: ${token.symbol}")
                        }
                        stack.addLast(result)
                    }
                }
            }

            is Token.Func -> {
                val a = stack.removeLast()
                val result = when (token.name) {
                    "sin" -> sin(toAngle(a))
                    "cos" -> cos(toAngle(a))
                    "tan" -> tan(toAngle(a))
                    "ln" -> ln(a)
                    "log" -> log10(a)
                    "sqrt" -> sqrt(a)
                    else -> throw IllegalArgumentException("Unknown function: ${token.name}")
                }
                stack.addLast(result)
            }

            else -> throw IllegalArgumentException("Unexpected token in postfix")
        }
    }

    return if (stack.size == 1) stack.last()
    else throw IllegalArgumentException("Invalid expression")
}

private fun factorial(n: Double): Double {
    if (n < 0) return Double.NaN
    if (n == 0.0 || n == 1.0) return 1.0
    // For non-integer, use gamma function approximation: n! = Gamma(n+1)
    if (n != n.toLong().toDouble()) {
        // Stirling approximation for non-integers
        return sqrt(2 * PI / n) * (n / E).pow(n)
    }
    val intN = n.toLong()
    if (intN > 170) return Double.POSITIVE_INFINITY // Overflow
    var result = 1.0
    for (i in 2..intN) {
        result *= i
    }
    return result
}
