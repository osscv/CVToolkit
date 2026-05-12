package cv.toolkit.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cv.toolkit.ui.theme.LatencyExcellent
import cv.toolkit.ui.theme.LatencyFair
import cv.toolkit.ui.theme.LatencyGood
import cv.toolkit.ui.theme.LatencyPoor
import cv.toolkit.ui.theme.LatencyUnknown
import cv.toolkit.ui.theme.MonoText

// ─── Semantic helpers ──────────────────────────────────────────────────────

/** Returns a color for a ping latency (ms). Use this instead of inline hex. */
fun latencyColor(ms: Long?): Color = when {
    ms == null || ms < 0 -> LatencyUnknown
    ms < 50              -> LatencyExcellent
    ms < 100             -> LatencyGood
    ms < 200             -> LatencyFair
    else                 -> LatencyPoor
}

fun latencyColor(ms: Int?): Color = latencyColor(ms?.toLong())
fun latencyColor(ms: Double?): Color = latencyColor(ms?.toLong())

// ─── Section header ────────────────────────────────────────────────────────

/**
 * Terminal-style section header — small monospace prefix + label + optional count.
 *
 *   ▸  ENCODERS & DECODERS                                          [ 8 ]
 */
@Composable
fun SectionHeader(
    label: String,
    count: Int? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, top = 4.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = "▸",
            style = MonoText.Label.copy(
                color = MaterialTheme.colorScheme.primary,
                fontSize = 12.sp,
            ),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 1.sp,
            ),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (count != null) {
            MonoCountChip(count)
        }
    }
}

/** Section header with a leading flag/icon (used for country-grouped sections). */
@Composable
fun FlagSectionHeader(
    label: String,
    flagPainter: androidx.compose.ui.graphics.painter.Painter,
    count: Int? = null,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 4.dp, top = 8.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(20.dp)
                .clip(CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            androidx.compose.foundation.Image(
                painter = flagPainter,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
            )
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp,
            ),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        if (count != null) {
            MonoCountChip(count)
        }
    }
}

@Composable
fun MonoCountChip(count: Int) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier.padding(start = 8.dp),
    ) {
        Text(
            text = count.toString(),
            style = MonoText.Label.copy(
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            ),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

// ─── Status badge ──────────────────────────────────────────────────────────

/**
 * Mono pill for stats: "23 ms", "EXCELLENT", "200 OK", etc.
 * Pass `tone` to color the dot + tint.
 */
@Composable
fun StatusBadge(
    text: String,
    tone: Color,
    modifier: Modifier = Modifier,
    showDot: Boolean = true,
) {
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = tone.copy(alpha = 0.14f),
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
        ) {
            if (showDot) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(tone),
                )
                Spacer(Modifier.width(6.dp))
            }
            Text(
                text = text,
                style = MonoText.Label.copy(color = tone, fontSize = 11.sp),
            )
        }
    }
}

// ─── Stat tile (icon + label + mono value) ──────────────────────────────────

@Composable
fun StatTile(
    label: String,
    value: String,
    icon: ImageVector? = null,
    tone: Color = MaterialTheme.colorScheme.primary,
    modifier: Modifier = Modifier,
) {
    OutlinedCard(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = tone,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                }
                Text(
                    text = label.uppercase(),
                    style = MaterialTheme.typography.labelMedium.copy(
                        letterSpacing = 0.8.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = value,
                style = MonoText.Title.copy(color = MaterialTheme.colorScheme.onSurface),
            )
        }
    }
}

// ─── Tool scaffold ──────────────────────────────────────────────────────────

/**
 * Consistent tool-screen wrapper: small TopAppBar styled for the technical
 * theme, with back arrow and optional trailing actions. Tool screens can
 * opt into this for unified chrome, or keep their own Scaffold.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolScaffold(
    title: String,
    onBack: () -> Unit,
    actions: @Composable () -> Unit = {},
    scrollBehavior: TopAppBarScrollBehavior? = null,
    content: @Composable (androidx.compose.foundation.layout.PaddingValues) -> Unit,
) {
    Scaffold(
        modifier = if (scrollBehavior != null) {
            Modifier.nestedScroll(scrollBehavior.nestedScrollConnection)
        } else Modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.2.sp,
                        ),
                        maxLines = 1,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                        )
                    }
                },
                actions = { actions() },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
        content = content,
    )
}

// ─── Mono tag (very small caps label, e.g. "RTT", "TTL", "AS#") ─────────────

@Composable
fun MonoTag(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Text(
        text = text.uppercase(),
        style = MonoText.Label.copy(
            color = color,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            fontSize = 10.sp,
            letterSpacing = 0.8.sp,
        ),
        modifier = modifier,
    )
}
