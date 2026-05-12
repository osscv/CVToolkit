package cv.toolkit.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import cv.toolkit.screens.TestTarget

/**
 * Persists user-added Speed Test targets across app launches.
 * Only custom targets are stored — the built-in defaults live in
 * SpeedTestScreen.defaultTargets and are merged at read time.
 */
object SpeedTestTargetsManager {
    private const val PREF_NAME = "speed_test_prefs"
    private const val KEY_TARGETS = "custom_targets"

    private val gson = Gson()
    private val listType = object : TypeToken<List<TestTarget>>() {}.type

    fun load(context: Context): List<TestTarget> {
        val raw = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .getString(KEY_TARGETS, null) ?: return emptyList()
        return try {
            gson.fromJson<List<TestTarget>>(raw, listType) ?: emptyList()
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun save(context: Context, targets: List<TestTarget>) {
        val json = gson.toJson(targets, listType)
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            .edit().putString(KEY_TARGETS, json).apply()
    }
}
