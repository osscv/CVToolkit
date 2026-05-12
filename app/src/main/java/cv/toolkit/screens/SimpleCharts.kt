package cv.toolkit.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.Locale

data class ChartPoint(val label: String, val value: Float)

data class ChartLine(val label: String, val points: List<ChartPoint>, val color: Color)

@Composable
fun SimpleLineChart(
    lines: List<ChartLine>,
    modifier: Modifier = Modifier,
    showLabels: Boolean = true,
    maxXLabels: Int = 6,
    yAxisLabel: String? = null
) {
    if (lines.isEmpty() || lines.all { it.points.isEmpty() }) return

    val allValues = lines.flatMap { it.points.map { p -> p.value } }
    val minVal = allValues.minOrNull() ?: 0f
    val maxVal = allValues.maxOrNull() ?: 1f
    val range = if (maxVal == minVal) 1f else maxVal - minVal
    val paddedMin = minVal - range * 0.05f
    val paddedMax = maxVal + range * 0.05f
    val paddedRange = paddedMax - paddedMin

    val maxPoints = lines.maxOf { it.points.size }
    val labelStep = if (maxPoints > maxXLabels) maxPoints / maxXLabels else 1
    val allLabels = lines.maxByOrNull { it.points.size }?.points?.map { it.label } ?: emptyList()
    val nf = remember { NumberFormat.getNumberInstance(Locale.US) }

    Column(modifier = modifier) {
        // Legend
        if (lines.size > 1) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                lines.forEach { line ->
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Canvas(Modifier.size(10.dp)) {
                            drawCircle(color = line.color, radius = 5.dp.toPx())
                        }
                        Spacer(Modifier.width(4.dp))
                        Text(line.label, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }

        // Y-axis label
        yAxisLabel?.let {
            Text(
                it,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }

        // Chart
        val lineColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
        val textColor = MaterialTheme.colorScheme.onSurfaceVariant

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            val leftPadding = 50.dp.toPx()
            val rightPadding = 12.dp.toPx()
            val topPadding = 8.dp.toPx()
            val bottomPadding = if (showLabels) 24.dp.toPx() else 8.dp.toPx()
            val chartWidth = size.width - leftPadding - rightPadding
            val chartHeight = size.height - topPadding - bottomPadding

            // Grid lines
            val gridLines = 4
            for (i in 0..gridLines) {
                val y = topPadding + chartHeight * i / gridLines
                drawLine(lineColor, Offset(leftPadding, y), Offset(size.width - rightPadding, y), strokeWidth = 1f)

                val value = paddedMax - (paddedRange * i / gridLines)
                val formatted = if (kotlin.math.abs(value) >= 1000) {
                    nf.format(value.toLong())
                } else {
                    String.format("%.1f", value)
                }
                drawContext.canvas.nativeCanvas.drawText(
                    formatted,
                    leftPadding - 6.dp.toPx(),
                    y + 4.dp.toPx(),
                    android.graphics.Paint().apply {
                        textSize = 9.sp.toPx()
                        color = textColor.hashCode()
                        textAlign = android.graphics.Paint.Align.RIGHT
                    }
                )
            }

            // Draw lines
            lines.forEach { line ->
                if (line.points.size < 2) return@forEach
                val path = Path()
                line.points.forEachIndexed { i, point ->
                    val x = leftPadding + (chartWidth * i / (line.points.size - 1).coerceAtLeast(1))
                    val y = topPadding + chartHeight * (1f - (point.value - paddedMin) / paddedRange)
                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(path, line.color, style = Stroke(width = 2.5.dp.toPx()))
            }

            // X-axis labels
            if (showLabels && allLabels.isNotEmpty()) {
                allLabels.forEachIndexed { i, label ->
                    if (i % labelStep == 0 || i == allLabels.size - 1) {
                        val x = leftPadding + (chartWidth * i / (allLabels.size - 1).coerceAtLeast(1))
                        drawContext.canvas.nativeCanvas.drawText(
                            label,
                            x,
                            size.height - 2.dp.toPx(),
                            android.graphics.Paint().apply {
                                textSize = 8.sp.toPx()
                                color = textColor.hashCode()
                                textAlign = android.graphics.Paint.Align.CENTER
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SimpleBarChart(
    data: List<ChartPoint>,
    modifier: Modifier = Modifier,
    barColor: Color = MaterialTheme.colorScheme.primary,
    maxBars: Int = 15,
    yAxisLabel: String? = null
) {
    if (data.isEmpty()) return
    val items = if (data.size > maxBars) data.take(maxBars) else data

    val maxVal = items.maxOf { it.value }.coerceAtLeast(1f)
    val nf = remember { NumberFormat.getNumberInstance(Locale.US) }
    val textColor = MaterialTheme.colorScheme.onSurfaceVariant

    Column(modifier = modifier) {
        yAxisLabel?.let {
            Text(
                it,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 2.dp)
            )
        }

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            val leftPadding = 50.dp.toPx()
            val rightPadding = 12.dp.toPx()
            val topPadding = 8.dp.toPx()
            val bottomPadding = 28.dp.toPx()
            val chartWidth = size.width - leftPadding - rightPadding
            val chartHeight = size.height - topPadding - bottomPadding

            // Grid
            val gridLines = 4
            val gridColor = textColor.copy(alpha = 0.15f)
            for (i in 0..gridLines) {
                val y = topPadding + chartHeight * i / gridLines
                drawLine(gridColor, Offset(leftPadding, y), Offset(size.width - rightPadding, y), strokeWidth = 1f)
                val value = maxVal * (gridLines - i) / gridLines
                val formatted = if (value >= 1000) nf.format(value.toLong()) else String.format("%.1f", value)
                drawContext.canvas.nativeCanvas.drawText(
                    formatted,
                    leftPadding - 6.dp.toPx(),
                    y + 4.dp.toPx(),
                    android.graphics.Paint().apply {
                        textSize = 9.sp.toPx()
                        color = textColor.hashCode()
                        textAlign = android.graphics.Paint.Align.RIGHT
                    }
                )
            }

            // Bars
            val barWidth = (chartWidth / items.size) * 0.7f
            val gap = (chartWidth / items.size) * 0.3f

            items.forEachIndexed { i, item ->
                val barHeight = (item.value / maxVal) * chartHeight
                val x = leftPadding + i * (barWidth + gap) + gap / 2
                val y = topPadding + chartHeight - barHeight

                drawRect(
                    color = barColor,
                    topLeft = Offset(x, y),
                    size = Size(barWidth, barHeight)
                )

                // Label
                val labelText = if (item.label.length > 6) item.label.take(6) + ".." else item.label
                drawContext.canvas.nativeCanvas.drawText(
                    labelText,
                    x + barWidth / 2,
                    size.height - 4.dp.toPx(),
                    android.graphics.Paint().apply {
                        textSize = 7.sp.toPx()
                        color = textColor.hashCode()
                        textAlign = android.graphics.Paint.Align.CENTER
                    }
                )
            }
        }
    }
}

@Composable
fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    valueColor: Color = MaterialTheme.colorScheme.onSurface
) {
    Column(
        modifier = modifier.padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(2.dp))
        Text(
            value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = valueColor,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        subtitle?.let {
            Text(
                it,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}
