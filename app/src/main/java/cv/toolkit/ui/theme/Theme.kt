package cv.toolkit.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ─── Dark scheme — terminal at night ────────────────────────────────────────
private val DarkScheme = darkColorScheme(
    primary           = SignalCyan70,
    onPrimary         = SignalCyan10,
    primaryContainer  = SignalCyan30,
    onPrimaryContainer = SignalCyan90,

    secondary           = TerminalGreen60,
    onSecondary         = TerminalGreen10,
    secondaryContainer  = TerminalGreen20,
    onSecondaryContainer = TerminalGreen90,

    tertiary           = Amber60,
    onTertiary         = Amber20,
    tertiaryContainer  = Amber20,
    onTertiaryContainer = Amber90,

    error           = Alarm80,
    onError         = Alarm20,
    errorContainer  = Alarm20,
    onErrorContainer = Alarm90,

    background        = Ink05,
    onBackground      = Ink90,
    surface           = Ink10,
    onSurface         = Ink90,
    surfaceVariant    = Ink20,
    onSurfaceVariant  = Ink70,
    surfaceTint       = SignalCyan70,
    inverseSurface    = Ink90,
    inverseOnSurface  = Ink15,
    inversePrimary    = SignalCyan40,

    surfaceContainerLowest = Ink05,
    surfaceContainerLow    = Ink10,
    surfaceContainer       = Ink15,
    surfaceContainerHigh   = Ink20,
    surfaceContainerHighest = Ink25,
    surfaceBright          = Ink30,
    surfaceDim             = Ink05,

    outline         = Ink50,
    outlineVariant  = Ink25,
    scrim           = Color.Black,
)

// ─── Light scheme — paper terminal ─────────────────────────────────────────
private val LightScheme = lightColorScheme(
    primary           = SignalCyan40,
    onPrimary         = Color.White,
    primaryContainer  = SignalCyan95,
    onPrimaryContainer = SignalCyan10,

    secondary           = TerminalGreen40,
    onSecondary         = Color.White,
    secondaryContainer  = TerminalGreen90,
    onSecondaryContainer = TerminalGreen10,

    tertiary           = Amber40,
    onTertiary         = Color.White,
    tertiaryContainer  = Amber90,
    onTertiaryContainer = Amber20,

    error           = Alarm40,
    onError         = Color.White,
    errorContainer  = Alarm90,
    onErrorContainer = Alarm10,

    background        = Paper99,
    onBackground      = Ink10,
    surface           = Paper100,
    onSurface         = Ink10,
    surfaceVariant    = Paper95,
    onSurfaceVariant  = Ink40,
    surfaceTint       = SignalCyan40,
    inverseSurface    = Ink20,
    inverseOnSurface  = Paper95,
    inversePrimary    = SignalCyan70,

    surfaceContainerLowest = Paper100,
    surfaceContainerLow    = Paper99,
    surfaceContainer       = Paper97,
    surfaceContainerHigh   = Paper95,
    surfaceContainerHighest = Paper90,
    surfaceBright          = Paper100,
    surfaceDim             = Paper95,

    outline         = Paper70,
    outlineVariant  = Paper90,
    scrim           = Color.Black,
)

@Composable
fun CVToolkitTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Custom brand identity by default. Opt-in to Material You via this flag.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = androidx.compose.ui.platform.LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkScheme
        else      -> LightScheme
    }

    // Sync system bar icon contrast with the theme background.
    // Bar transparency is already handled by `enableEdgeToEdge()` in MainActivity.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            val controller = WindowCompat.getInsetsController(window, view)
            val lightBg = colorScheme.background.luminance() > 0.5f
            controller.isAppearanceLightStatusBars = lightBg
            controller.isAppearanceLightNavigationBars = lightBg
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        shapes      = AppShapes,
        content     = content,
    )
}
