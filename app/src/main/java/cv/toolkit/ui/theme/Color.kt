package cv.toolkit.ui.theme

import androidx.compose.ui.graphics.Color

// ─── Brand / Primary (Signal Cyan) ──────────────────────────────────────────
val SignalCyan10  = Color(0xFF001F24)
val SignalCyan20  = Color(0xFF00363D)
val SignalCyan30  = Color(0xFF004F58)
val SignalCyan40  = Color(0xFF006874)
val SignalCyan50  = Color(0xFF008391)
val SignalCyan60  = Color(0xFF00A2B3)
val SignalCyan70  = Color(0xFF22C1D6)
val SignalCyan80  = Color(0xFF54D7EB)
val SignalCyan90  = Color(0xFF97F0FF)
val SignalCyan95  = Color(0xFFC9F7FF)

// ─── Secondary (Terminal Green) ─────────────────────────────────────────────
val TerminalGreen10 = Color(0xFF062A12)
val TerminalGreen20 = Color(0xFF0E4421)
val TerminalGreen30 = Color(0xFF155E2F)
val TerminalGreen40 = Color(0xFF1F7A3F)
val TerminalGreen60 = Color(0xFF34C759)
val TerminalGreen80 = Color(0xFF7BE49A)
val TerminalGreen90 = Color(0xFFB7F2C6)

// ─── Tertiary (Amber accent) ────────────────────────────────────────────────
val Amber20 = Color(0xFF4A2D00)
val Amber40 = Color(0xFFB67100)
val Amber60 = Color(0xFFFFB020)
val Amber80 = Color(0xFFFFD58A)
val Amber90 = Color(0xFFFFE7B8)

// ─── Neutrals / Surface (Ink) ───────────────────────────────────────────────
// Dark surface system — near-black with a hint of blue, like a terminal at night
val Ink0   = Color(0xFF000000)
val Ink05  = Color(0xFF06080B)
val Ink10  = Color(0xFF0B0E12)
val Ink15  = Color(0xFF11151B)
val Ink20  = Color(0xFF161B22)
val Ink25  = Color(0xFF1C2229)
val Ink30  = Color(0xFF222932)
val Ink40  = Color(0xFF2D3540)
val Ink50  = Color(0xFF3A4452)
val Ink60  = Color(0xFF5A6473)
val Ink70  = Color(0xFF8A95A4)
val Ink80  = Color(0xFFB6BEC9)
val Ink90  = Color(0xFFD7DCE3)
val Ink95  = Color(0xFFE9ECF0)
val Ink99  = Color(0xFFF7F8FA)

// Light surface system — paper with subtle warmth, not pure white
val Paper100 = Color(0xFFFFFFFF)
val Paper99  = Color(0xFFFAFBFC)
val Paper97  = Color(0xFFF3F5F7)
val Paper95  = Color(0xFFEDF0F3)
val Paper90  = Color(0xFFE0E4E9)
val Paper80  = Color(0xFFC3C9D2)
val Paper70  = Color(0xFFA6ADB8)

// ─── Errors (Alarm Red) ─────────────────────────────────────────────────────
val Alarm10 = Color(0xFF410002)
val Alarm20 = Color(0xFF690005)
val Alarm40 = Color(0xFFBA1A1A)
val Alarm80 = Color(0xFFFFB4AB)
val Alarm90 = Color(0xFFFFDAD6)

// ─── Semantic latency / health tokens ───────────────────────────────────────
// Use these everywhere instead of ad-hoc hex literals.
// Latency: < 50ms green · < 100ms cyan · < 200ms amber · >= 200ms red.
val LatencyExcellent = Color(0xFF22C55E) // < 50ms  · Excellent
val LatencyGood      = Color(0xFF06B6D4) // < 100ms · Good
val LatencyFair      = Color(0xFFF59E0B) // < 200ms · Fair
val LatencyPoor      = Color(0xFFEF4444) // ≥ 200ms · Poor
val LatencyUnknown   = Color(0xFF6B7280) // n/a

// Chart series palette — 8 distinct hues that read well on both light/dark
val ChartHue1 = Color(0xFF22D3EE) // cyan
val ChartHue2 = Color(0xFF22C55E) // green
val ChartHue3 = Color(0xFFF59E0B) // amber
val ChartHue4 = Color(0xFFEF4444) // red
val ChartHue5 = Color(0xFFA78BFA) // violet
val ChartHue6 = Color(0xFFFB7185) // rose
val ChartHue7 = Color(0xFF60A5FA) // blue
val ChartHue8 = Color(0xFFFACC15) // yellow

val ChartPalette = listOf(
    ChartHue1, ChartHue2, ChartHue3, ChartHue4,
    ChartHue5, ChartHue6, ChartHue7, ChartHue8
)
