package cv.toolkit.data

import android.app.Activity
import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LocaleHelper {
    private const val PREF_NAME = "locale_prefs"
    private const val KEY_LANGUAGE = "selected_language"

    fun getSavedLanguage(context: Context): String {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        return prefs.getString(KEY_LANGUAGE, "system") ?: "system"
    }

    fun saveLanguage(context: Context, languageTag: String) {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_LANGUAGE, languageTag).apply()
    }

    fun applyLocale(context: Context) {
        val language = getSavedLanguage(context)
        if (language == "system") return
        val locale = createLocale(language)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        context.resources.updateConfiguration(config, context.resources.displayMetrics)
    }

    fun wrapContext(context: Context): Context {
        val language = getSavedLanguage(context)
        if (language == "system") return context
        val locale = createLocale(language)
        Locale.setDefault(locale)
        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        return context.createConfigurationContext(config)
    }

    fun changeLanguage(context: Context, languageTag: String) {
        saveLanguage(context, languageTag)
        if (context is Activity) {
            context.recreate()
        }
    }

    private fun createLocale(tag: String): Locale {
        return when {
            tag.contains("-") -> {
                val parts = tag.split("-")
                Locale(parts[0], parts[1])
            }
            else -> Locale(tag)
        }
    }
}
