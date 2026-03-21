package cv.toolkit.screens

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
import cv.toolkit.R
import cv.toolkit.ads.BannerAd
import cv.toolkit.data.LocaleHelper

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
                item {
                    Text(
                        text = stringResource(R.string.language_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 12.dp)
                    )
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
private fun LanguageItem(
    language: LanguageOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        shape = MaterialTheme.shapes.medium,
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
        else MaterialTheme.colorScheme.surface,
        tonalElevation = if (isSelected) 2.dp else 0.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = language.flag,
                style = MaterialTheme.typography.headlineSmall
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = language.nativeName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurface
                )
                if (language.englishName.isNotEmpty()) {
                    Text(
                        text = language.englishName,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (isSelected) {
                Icon(
                    Icons.Filled.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
