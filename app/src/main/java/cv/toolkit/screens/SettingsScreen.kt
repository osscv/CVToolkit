package cv.toolkit.screens

import android.app.Activity
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.sp
import cv.toolkit.R
import cv.toolkit.ads.BannerAd
import cv.toolkit.data.LocaleHelper
import cv.toolkit.data.ThemeHelper
import cv.toolkit.ui.components.SectionHeader
import cv.toolkit.ui.theme.MonoText

data class LanguageOption(
    val tag: String,
    val nativeName: String,
    val englishName: String,
    val flag: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: NavController) {
    val context = LocalContext.current
    var selectedLanguage by remember { mutableStateOf(LocaleHelper.getSavedLanguage(context)) }
    var selectedTheme by remember { mutableStateOf(ThemeHelper.getSavedTheme(context)) }

    val languages = listOf(
        LanguageOption("system", stringResource(R.string.system_default), "", "\uD83C\uDF10"),
        LanguageOption("en", "English", "", "\uD83C\uDDFA\uD83C\uDDF8"),
        LanguageOption("zh-CN", "\u7B80\u4F53\u4E2D\u6587", "Chinese (Simplified)", "\uD83C\uDDE8\uD83C\uDDF3"),
        LanguageOption("zh-TW", "\u7E41\u9AD4\u4E2D\u6587", "Chinese (Traditional)", "\uD83C\uDDF9\uD83C\uDDFC"),
        LanguageOption("ms", "Bahasa Melayu", "Malay", "\uD83C\uDDF2\uD83C\uDDFE"),
        LanguageOption("fr", "Fran\u00E7ais", "French", "\uD83C\uDDEB\uD83C\uDDF7"),
        LanguageOption("th", "\u0E20\u0E32\u0E29\u0E32\u0E44\u0E17\u0E22", "Thai", "\uD83C\uDDF9\uD83C\uDDED"),
        LanguageOption("hi", "\u0939\u093F\u0928\u094D\u0926\u0940", "Hindi", "\uD83C\uDDEE\uD83C\uDDF3"),
        LanguageOption("ko", "\uD55C\uAD6D\uC5B4", "Korean", "\uD83C\uDDF0\uD83C\uDDF7"),
        LanguageOption("ja", "\u65E5\u672C\u8A9E", "Japanese", "\uD83C\uDDEF\uD83C\uDDF5"),
        LanguageOption("es", "Espa\u00F1ol", "Spanish", "\uD83C\uDDEA\uD83C\uDDF8"),
        LanguageOption("pt-BR", "Portugu\u00EAs", "Portuguese", "\uD83C\uDDE7\uD83C\uDDF7"),
        LanguageOption("de", "Deutsch", "German", "\uD83C\uDDE9\uD83C\uDDEA"),
        LanguageOption("id", "Bahasa Indonesia", "Indonesian", "\uD83C\uDDEE\uD83C\uDDE9"),
        LanguageOption("vi", "Ti\u1EBFng Vi\u1EC7t", "Vietnamese", "\uD83C\uDDFB\uD83C\uDDF3"),
        LanguageOption("ar", "\u0627\u0644\u0639\u0631\u0628\u064A\u0629", "Arabic", "\uD83C\uDDF8\uD83C\uDDE6"),
        LanguageOption("ru", "\u0420\u0443\u0441\u0441\u043A\u0438\u0439", "Russian", "\uD83C\uDDF7\uD83C\uDDFA"),
        LanguageOption("it", "Italiano", "Italian", "\uD83C\uDDEE\uD83C\uDDF9"),
        LanguageOption("tr", "T\u00FCrk\u00E7e", "Turkish", "\uD83C\uDDF9\uD83C\uDDF7")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                // --- Theme Section ---
                item {
                    Spacer(Modifier.height(8.dp))
                    SectionHeader(label = "Appearance")
                }

                item {
                    ThemeOption(
                        icon = Icons.Filled.BrightnessAuto,
                        title = "System Default",
                        subtitle = "Follow device theme",
                        isSelected = selectedTheme == ThemeHelper.THEME_SYSTEM,
                        onClick = {
                            selectedTheme = ThemeHelper.THEME_SYSTEM
                            ThemeHelper.saveTheme(context, ThemeHelper.THEME_SYSTEM)
                            (context as? Activity)?.recreate()
                        }
                    )
                }

                item {
                    ThemeOption(
                        icon = Icons.Filled.LightMode,
                        title = "Light",
                        subtitle = "Always use light theme",
                        isSelected = selectedTheme == ThemeHelper.THEME_LIGHT,
                        onClick = {
                            selectedTheme = ThemeHelper.THEME_LIGHT
                            ThemeHelper.saveTheme(context, ThemeHelper.THEME_LIGHT)
                            (context as? Activity)?.recreate()
                        }
                    )
                }

                item {
                    ThemeOption(
                        icon = Icons.Filled.DarkMode,
                        title = "Dark",
                        subtitle = "Always use dark theme",
                        isSelected = selectedTheme == ThemeHelper.THEME_DARK,
                        onClick = {
                            selectedTheme = ThemeHelper.THEME_DARK
                            ThemeHelper.saveTheme(context, ThemeHelper.THEME_DARK)
                            (context as? Activity)?.recreate()
                        }
                    )
                }

                // --- Language Section ---
                item {
                    Spacer(Modifier.height(20.dp))
                    SectionHeader(label = stringResource(R.string.language_title))
                }

                items(languages) { language ->
                    LanguageItem(
                        language = language,
                        isSelected = selectedLanguage == language.tag,
                        onClick = {
                            if (selectedLanguage != language.tag) {
                                selectedLanguage = language.tag
                                LocaleHelper.changeLanguage(context, language.tag)
                            }
                        }
                    )
                }
            }
            BannerAd(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun ThemeOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.outlineVariant
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
            else MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, borderColor),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                modifier = Modifier.size(36.dp),
                shape = RoundedCornerShape(10.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                    else MaterialTheme.colorScheme.surfaceContainerHigh,
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = if (isSelected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (isSelected) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun LanguageItem(
    language: LanguageOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) MaterialTheme.colorScheme.primary
        else MaterialTheme.colorScheme.outlineVariant
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
            else MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, borderColor),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = language.flag,
                style = MaterialTheme.typography.headlineSmall
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = language.nativeName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (language.englishName.isNotEmpty()) {
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = language.englishName,
                        style = MonoText.Label.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (isSelected) {
                Icon(
                    Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}
