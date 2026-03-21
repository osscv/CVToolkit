package cv.toolkit.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

data class UpdateInfo(
    val version: String = "",
    @SerializedName("version_code") val versionCode: Int = 0,
    val title: String = "",
    val description: String = "",
    val changelog: List<String> = emptyList(),
    @SerializedName("download_url") val downloadUrl: String = "",
    val mandatory: Boolean = false,
    @SerializedName("release_date") val releaseDate: String = "",
    @SerializedName("file_size") val fileSize: String = ""
)

object UpdateChecker {

    private const val UPDATE_URL =
        "https://raw.githubusercontent.com/osscv/CVToolkit/refs/heads/main/app/release/update.json"

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    suspend fun checkForUpdate(currentVersionCode: Int): UpdateInfo? = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder().url(UPDATE_URL).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null

                val body = response.body.string()
                val updateInfo = Gson().fromJson(body, UpdateInfo::class.java)

                if (updateInfo.versionCode > currentVersionCode) updateInfo else null
            }
        } catch (_: Exception) {
            null
        }
    }

    fun openDownloadUrl(context: Context, url: String) {
        if (url.isBlank()) return
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    }
}
