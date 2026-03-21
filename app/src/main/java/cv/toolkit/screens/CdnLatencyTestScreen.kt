package cv.toolkit.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.snapshots.SnapshotStateMap
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import cv.toolkit.ads.BannerAd
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL
import kotlin.system.measureTimeMillis

// Provider Logo URLs
object ProviderLogos {
    private const val BASE_URL = "https://www.dkly.net/uploads/CVToolkit/logoOFcdnCloud"
    
    fun getLogoUrl(provider: CdnProvider): String {
        return when (provider) {
            CdnProvider.AZURE -> "$BASE_URL/NET_BIG.D-52893f5e.png"
            CdnProvider.AWS -> "$BASE_URL/Amazon_Web_Services_Logo.svg.png"
            CdnProvider.ALIBABA -> "$BASE_URL/AlibabaCloudLogo.svg.png"
            CdnProvider.VULTR -> "$BASE_URL/VULTR.jpg"
            CdnProvider.LINODE -> "$BASE_URL/Linode_updated_logo.png"
            CdnProvider.LIGHTSAIL -> "$BASE_URL/AWS-Lightsail.png"
            CdnProvider.DIGITALOCEAN -> "$BASE_URL/DigitalOcean_logo.png"
            CdnProvider.ORACLE -> "$BASE_URL/Oracle_logo.svg.webp"
            CdnProvider.HUAWEI -> "$BASE_URL/huawei-logo.png"
            CdnProvider.TENCENT -> "$BASE_URL/Tencent_Logo.svg.png"
            CdnProvider.GCP -> "$BASE_URL/Google_Cloud_logo.svg.png"
            CdnProvider.OVH -> "$BASE_URL/Logo_OVH.svg.png"
            CdnProvider.HETZNER -> "$BASE_URL/Logo_Hetzner.svg.png"
            CdnProvider.FASTLY -> "$BASE_URL/Fastly_logo.svg.png"
        }
    }
    
    fun getCdnLogoUrl(provider: String): String {
        return when (provider) {
            "Cloudflare" -> "$BASE_URL/Cloudflare_Logo.svg.png"
            "CloudFront" -> "$BASE_URL/logo-amazon-cloudfront.png"
            "Alibaba Cloud" -> "$BASE_URL/AlibabaCloudLogo.svg.png"
            else -> "$BASE_URL/NET_BIG.D-52893f5e.png"
        }
    }
}

// Helper function to get friendly location name from code
fun getCdnLocationName(code: String): String {
    val locationMap = mapOf(
        "KUL" to "Kuala Lumpur, Malaysia",
        "SIN" to "Singapore",
        "HKG" to "Hong Kong",
        "NRT" to "Tokyo, Japan",
        "ICN" to "Seoul, South Korea",
        "BOM" to "Mumbai, India",
        "SYD" to "Sydney, Australia",
        "MEL" to "Melbourne, Australia",
        "AKL" to "Auckland, New Zealand",
        "DXB" to "Dubai, UAE",
        "BAH" to "Manama, Bahrain",
        "JNB" to "Johannesburg, South Africa",
        "LHR" to "London, UK",
        "CDG" to "Paris, France",
        "FRA" to "Frankfurt, Germany",
        "AMS" to "Amsterdam, Netherlands",
        "MAD" to "Madrid, Spain",
        "MXP" to "Milan, Italy",
        "ARN" to "Stockholm, Sweden",
        "ZRH" to "Zurich, Switzerland",
        "VIE" to "Vienna, Austria",
        "WAW" to "Warsaw, Poland",
        "BKK" to "Bangkok, Thailand",
        "MNL" to "Manila, Philippines",
        "CGK" to "Jakarta, Indonesia",
        "IAD" to "Virginia, USA",
        "ORD" to "Chicago, USA",
        "DFW" to "Dallas, USA",
        "LAX" to "Los Angeles, USA",
        "SFO" to "San Francisco, USA",
        "SEA" to "Seattle, USA",
        "YVR" to "Vancouver, Canada",
        "YYZ" to "Toronto, Canada",
        "GRU" to "São Paulo, Brazil",
        "SCL" to "Santiago, Chile",
        "MEX" to "Mexico City, Mexico",
        "TLV" to "Tel Aviv, Israel",
        "CAI" to "Cairo, Egypt",
        "DOH" to "Doha, Qatar"
    )
    return locationMap[code] ?: code
}

// Data models
data class CdnRegion(
    val id: String,
    val name: String,
    val location: String,
    val geography: String,
    val url: String,
    val icon: String,
    val provider: CdnProvider,
    val availabilityZones: Int = 0,
    var latency: Long? = null,
    var status: TestStatus = TestStatus.PENDING
)

enum class CdnProvider {
    AZURE, AWS, ALIBABA, VULTR, LINODE, LIGHTSAIL, DIGITALOCEAN, ORACLE, HUAWEI, TENCENT, GCP, OVH, HETZNER, FASTLY
}

enum class TestStatus {
    PENDING, TESTING, SUCCESS, FAILED, TIMEOUT
}

data class UserCdnInfo(
    val provider: String,
    val location: String,
    val code: String,
    val isLoading: Boolean = false,
    val error: String? = null
)

// Data class for ping history and statistics
data class PingHistory(
    val regionId: String,
    val latencies: List<Long> = emptyList(),
    val timestamps: List<Long> = emptyList(),
    val failedCount: Int = 0,
    val totalCount: Int = 0
) {
    val minLatency: Long? get() = latencies.minOrNull()
    val maxLatency: Long? get() = latencies.maxOrNull()
    val avgLatency: Long? get() = if (latencies.isNotEmpty()) latencies.average().toLong() else null
    val packetLoss: Float get() = if (totalCount > 0) (failedCount.toFloat() / totalCount) * 100f else 0f
    val lastLatency: Long? get() = latencies.lastOrNull()
    val jitter: Long get() {
        if (latencies.size < 2) return 0L
        val diffs = latencies.zipWithNext { a, b -> kotlin.math.abs(b - a) }
        return if (diffs.isNotEmpty()) diffs.average().toLong() else 0L
    }

    fun addLatency(latency: Long, timestamp: Long, maxDataPoints: Int): PingHistory {
        val newLatencies = (latencies + latency).takeLast(maxDataPoints)
        val newTimestamps = (timestamps + timestamp).takeLast(maxDataPoints)
        return copy(
            latencies = newLatencies,
            timestamps = newTimestamps,
            totalCount = totalCount + 1
        )
    }

    fun addFailure(): PingHistory {
        return copy(
            failedCount = failedCount + 1,
            totalCount = totalCount + 1
        )
    }
}

// Continuous ping interval options
enum class CdnPingInterval(val label: String, val ms: Long) {
    FAST("1s", 1000),
    NORMAL("2s", 2000),
    SLOW("5s", 5000)
}

// Extended colors for all regions in graph (supports many regions)
val cdnGraphColors = listOf(
    Color(0xFFFFD700), // Gold/Yellow - Africa
    Color(0xFF4CAF50), // Green
    Color(0xFFFF9800), // Orange
    Color(0xFF8BC34A), // Light Green
    Color(0xFFFF5722), // Deep Orange
    Color(0xFF2196F3), // Blue
    Color(0xFFE91E63), // Pink
    Color(0xFF9C27B0), // Purple
    Color(0xFF00BCD4), // Cyan
    Color(0xFF009688), // Teal
    Color(0xFF3F51B5), // Indigo
    Color(0xFFCDDC39), // Lime
    Color(0xFF795548), // Brown
    Color(0xFF607D8B), // Blue Grey
    Color(0xFFF44336), // Red
    Color(0xFF673AB7), // Deep Purple
    Color(0xFF03A9F4), // Light Blue
    Color(0xFFFF4081), // Pink Accent
    Color(0xFF00E676), // Green Accent
    Color(0xFFFFAB00), // Amber Accent
    Color(0xFF536DFE), // Indigo Accent
    Color(0xFF7C4DFF), // Deep Purple Accent
    Color(0xFF18FFFF), // Cyan Accent
    Color(0xFF64FFDA), // Teal Accent
    Color(0xFFB388FF), // Purple Accent
    Color(0xFF82B1FF), // Blue Accent
    Color(0xFFFFFF00), // Yellow
    Color(0xFFFF6E40), // Deep Orange Accent
    Color(0xFFEEFF41), // Lime Accent
    Color(0xFF69F0AE), // Green Accent 2
    Color(0xFFE040FB), // Purple Accent 2
    Color(0xFF40C4FF), // Light Blue Accent
    Color(0xFFFFD740), // Amber Accent 2
    Color(0xFFFF5252), // Red Accent
    Color(0xFF448AFF), // Blue Accent 2
)

// Azure regions data
object AzureRegions {
    val regions = listOf(
        CdnRegion("meic18632", "Israel Central", "Israel", "Israel", "https://speedtestmeic18632.z39.web.core.windows.net", "il", CdnProvider.AZURE, 1),
        CdnRegion("we07148", "West Europe", "Netherlands", "Europe", "https://speedtestwe07148.z6.web.core.windows.net", "nl", CdnProvider.AZURE, 1),
        CdnRegion("sea18632", "Southeast Asia", "Singapore", "Asia Pacific", "https://speedtestsea18632.z23.web.core.windows.net", "sg", CdnProvider.AZURE, 1),
        CdnRegion("ea09073", "East Asia", "Hong Kong SAR", "Asia Pacific", "https://stea09073.z7.web.core.windows.net", "hk", CdnProvider.AZURE, 1),
        CdnRegion("nsus18632", "North Central US", "Illinois", "United States", "https://speedtestnsus18632.z14.web.core.windows.net", "us", CdnProvider.AZURE, 0),
        CdnRegion("ne18632", "North Europe", "Ireland", "Europe", "https://speedtestne18632.z16.web.core.windows.net", "ie", CdnProvider.AZURE, 1),
        CdnRegion("scus18632", "South Central US", "Texas", "United States", "https://speedtestscus18632.z21.web.core.windows.net", "us", CdnProvider.AZURE, 1),
        CdnRegion("wus09377", "West US", "California", "United States", "https://stwus09377.z22.web.core.windows.net", "us", CdnProvider.AZURE, 0),
        CdnRegion("eus09377", "East US", "Virginia", "United States", "https://steus09377.z13.web.core.windows.net", "us", CdnProvider.AZURE, 1),
        CdnRegion("jpe09377", "Japan East", "Tokyo, Saitama", "Japan", "https://stjpe09377.z11.web.core.windows.net", "jp", CdnProvider.AZURE, 1),
        CdnRegion("jpw18632", "Japan West", "Osaka", "Japan", "https://speedtestjpw18632.z31.web.core.windows.net", "jp", CdnProvider.AZURE, 1),
        CdnRegion("cus09377", "Central US", "Iowa", "United States", "https://stcus09377.z19.web.core.windows.net", "us", CdnProvider.AZURE, 1),
        CdnRegion("eus208516", "East US 2", "Virginia", "United States", "https://steus208516.z20.web.core.windows.net", "us", CdnProvider.AZURE, 1),
        CdnRegion("ozse07105", "Australia Southeast", "Victoria", "Australia", "https://speedtestozse07105.z26.web.core.windows.net", "au", CdnProvider.AZURE, 0),
        CdnRegion("oze18632", "Australia East", "New South Wales", "Australia", "https://speedtestoze18632.z8.web.core.windows.net", "au", CdnProvider.AZURE, 1),
        CdnRegion("ukw08516", "UK West", "Cardiff", "United Kingdom", "https://stukw08516.z35.web.core.windows.net", "gb", CdnProvider.AZURE, 0),
        CdnRegion("uks18632", "UK South", "London", "United Kingdom", "https://speedtestuks18632.z33.web.core.windows.net", "gb", CdnProvider.AZURE, 1),
        CdnRegion("cac18632", "Canada Central", "Toronto", "Canada", "https://speedtestcac18632.z9.web.core.windows.net", "ca", CdnProvider.AZURE, 1),
        CdnRegion("cae18632", "Canada East", "Quebec", "Canada", "https://speedtestcae18632.z27.web.core.windows.net", "ca", CdnProvider.AZURE, 0),
        CdnRegion("westus218632", "West US 2", "Washington", "United States", "https://speedtestwestus218632.z5.web.core.windows.net", "us", CdnProvider.AZURE, 1),
        CdnRegion("westindia18632", "West India", "Mumbai", "India", "https://speedtestwestindia18632.z10.web.core.windows.net", "in", CdnProvider.AZURE, 0),
        CdnRegion("ei08516", "South India", "Chennai", "India", "https://stei08516.z30.web.core.windows.net", "in", CdnProvider.AZURE, 0),
        CdnRegion("ci08318", "Central India", "Pune", "India", "https://stci08318.z29.web.core.windows.net", "in", CdnProvider.AZURE, 1),
        CdnRegion("kc09377", "Korea Central", "Seoul", "Korea", "https://stkc09377.z12.web.core.windows.net", "kr", CdnProvider.AZURE, 1),
        CdnRegion("koreasout18632", "Korea South", "Busan", "Korea", "https://speedtestkoreasout18632.z32.web.core.windows.net", "kr", CdnProvider.AZURE, 0),
        CdnRegion("westcentr18632", "West Central US", "Wyoming", "United States", "https://speedtestwestcentr18632.z4.web.core.windows.net", "us", CdnProvider.AZURE, 0),
        CdnRegion("frc18632", "France Central", "Paris", "France", "https://speedtestfrc18632.z28.web.core.windows.net", "fr", CdnProvider.AZURE, 1),
        CdnRegion("san18632", "South Africa North", "Johannesburg", "South Africa", "https://speedtestsan18632.z1.web.core.windows.net", "za", CdnProvider.AZURE, 1),
        CdnRegion("uaen18632", "UAE North", "Dubai", "UAE", "https://speedtestuaen18632.z1.web.core.windows.net", "ae", CdnProvider.AZURE, 1),
        CdnRegion("den18632", "Germany West Central", "Frankfurt", "Germany", "https://speedtestden18632.z1.web.core.windows.net", "de", CdnProvider.AZURE, 1),
        CdnRegion("ene18632", "Norway East", "Norway", "Norway", "https://speedtestene18632.z1.web.core.windows.net", "no", CdnProvider.AZURE, 1),
        CdnRegion("nea18632", "Brazil South", "Sao Paulo State", "Brazil", "https://speedtestnea18632.z15.web.core.windows.net", "br", CdnProvider.AZURE, 1),
        CdnRegion("esc218632", "Sweden Central", "Gävle", "Sweden", "https://speedtestesc218632.z1.web.core.windows.net", "se", CdnProvider.AZURE, 1),
        CdnRegion("wus318632", "West US 3", "Phoenix", "United States", "https://speedtestwus318632.z1.web.core.windows.net", "us", CdnProvider.AZURE, 1),
        CdnRegion("aqc18632", "Qatar Central", "Doha", "Qatar", "https://speedtestaqc18632.z1.web.core.windows.net", "qa", CdnProvider.AZURE, 1),
        CdnRegion("plc18632", "Poland Central", "Warsaw", "Poland", "https://speedtestplc18632.z36.web.core.windows.net", "pl", CdnProvider.AZURE, 1),
        CdnRegion("ein18632", "Italy North", "Milan", "Italy", "https://speedtestein18632.z38.web.core.windows.net", "it", CdnProvider.AZURE, 1),
        CdnRegion("mmc18632", "Mexico Central", "Querétaro State", "Mexico", "https://speedtestmmc18632.z41.web.core.windows.net", "mx", CdnProvider.AZURE, 1),
        CdnRegion("apac18632", "Australia Central", "Canberra", "Australia", "https://speedtestapac18632.z24.web.core.windows.net", "au", CdnProvider.AZURE, 0),
        CdnRegion("jioindiawest47365", "Jio India West", "Jamnagar", "India", "https://jioindiawest47365.z1.web.core.windows.net", "in", CdnProvider.AZURE, 0),
        CdnRegion("esn18632", "Switzerland North", "Zurich", "Switzerland", "https://speedtestesn18632.z1.web.core.windows.net", "ch", CdnProvider.AZURE, 1),
        CdnRegion("nzn18632", "New Zealand North", "Auckland", "New Zealand", "https://speedtestnzn18632.z44.web.core.windows.net", "nz", CdnProvider.AZURE, 1),
        CdnRegion("amw18632", "Malaysia West", "Kuala Lumpur", "Malaysia", "https://speedtestamw18632.z48.web.core.windows.net", "my", CdnProvider.AZURE, 1),
        CdnRegion("aic18632", "Indonesia Central", "Jakarta", "Indonesia", "https://speedtestaic18632.z45.web.core.windows.net", "id", CdnProvider.AZURE, 1),
        CdnRegion("chile53201", "Chile Central", "Santiago", "Chile", "https://speedtestchile53201.z47.web.core.windows.net", "cl", CdnProvider.AZURE, 1),
        CdnRegion("spain53288", "Spain Central", "Madrid", "Spain", "https://speedtestspain53288.z43.web.core.windows.net", "es", CdnProvider.AZURE, 1),
        CdnRegion("at53726", "Austria East", "Vienna", "Austria", "https://speedtestat53726.z49.web.core.windows.net", "at", CdnProvider.AZURE, 1)
    )
}

// Alibaba Cloud regions data
object AlibabaRegions {
    val regions = listOf(
        // China Regions
        CdnRegion("cn-beijing", "China (Beijing)", "Beijing", "China", "https://alibaba.oss-cn-beijing.aliyuncs.com", "cn", CdnProvider.ALIBABA, 3),
        CdnRegion("cn-guangzhou", "China (Guangzhou)", "Guangzhou", "China", "https://alibaba.oss-cn-guangzhou.aliyuncs.com", "cn", CdnProvider.ALIBABA, 3),
        CdnRegion("cn-hangzhou", "China (Hangzhou)", "Hangzhou", "China", "https://alibaba.oss-cn-hangzhou.aliyuncs.com", "cn", CdnProvider.ALIBABA, 3),
        CdnRegion("cn-heyuan", "China (Heyuan)", "Heyuan", "China", "https://alibaba.oss-cn-heyuan.aliyuncs.com", "cn", CdnProvider.ALIBABA, 3),
        CdnRegion("cn-huhehaote", "China (Hohhot)", "Hohhot", "China", "https://alibaba.oss-cn-huhehaote.aliyuncs.com", "cn", CdnProvider.ALIBABA, 3),
        CdnRegion("cn-qingdao", "China (Qingdao)", "Qingdao", "China", "https://alibaba.oss-cn-qingdao.aliyuncs.com", "cn", CdnProvider.ALIBABA, 3),
        CdnRegion("cn-shanghai", "China (Shanghai)", "Shanghai", "China", "https://alibaba.oss-cn-shanghai.aliyuncs.com", "cn", CdnProvider.ALIBABA, 3),
        CdnRegion("cn-shenzhen", "China (Shenzhen)", "Shenzhen", "China", "https://alibaba.oss-cn-shenzhen.aliyuncs.com", "cn", CdnProvider.ALIBABA, 3),
        CdnRegion("cn-wulanchabu", "China (Ulanqab)", "Ulanqab", "China", "https://alibaba.oss-cn-wulanchabu.aliyuncs.com", "cn", CdnProvider.ALIBABA, 3),
        CdnRegion("cn-zhangjiakou", "China (Zhangjiakou)", "Zhangjiakou", "China", "https://alibaba.oss-cn-zhangjiakou.aliyuncs.com", "cn", CdnProvider.ALIBABA, 3),
        CdnRegion("cn-chengdu", "China (Chengdu)", "Chengdu", "China", "https://alibaba.oss-cn-chengdu.aliyuncs.com", "cn", CdnProvider.ALIBABA, 3),
        CdnRegion("cn-hongkong", "China (Hong Kong)", "Hong Kong", "Hong Kong", "https://alibaba.oss-cn-hongkong.aliyuncs.com", "hk", CdnProvider.ALIBABA, 3),
        // Asia Pacific Regions
        CdnRegion("ap-northeast-1", "Asia Pacific (Tokyo)", "Tokyo", "Japan", "https://alibaba.oss-ap-northeast-1.aliyuncs.com", "jp", CdnProvider.ALIBABA, 3),
        CdnRegion("ap-northeast-2", "Asia Pacific (Seoul)", "Seoul", "South Korea", "https://alibaba.oss-ap-northeast-2.aliyuncs.com", "kr", CdnProvider.ALIBABA, 3),
        CdnRegion("ap-southeast-1", "Asia Pacific (Singapore)", "Singapore", "Singapore", "https://alibaba.oss-ap-southeast-1.aliyuncs.com", "sg", CdnProvider.ALIBABA, 3),
        CdnRegion("ap-southeast-3", "Asia Pacific (Kuala Lumpur)", "Kuala Lumpur", "Malaysia", "https://alibaba.oss-ap-southeast-3.aliyuncs.com", "my", CdnProvider.ALIBABA, 3),
        CdnRegion("ap-southeast-5", "Asia Pacific (Jakarta)", "Jakarta", "Indonesia", "https://alibaba.oss-ap-southeast-5.aliyuncs.com", "id", CdnProvider.ALIBABA, 3),
        CdnRegion("ap-southeast-6", "Asia Pacific (Manila)", "Manila", "Philippines", "https://alibaba.oss-ap-southeast-6.aliyuncs.com", "ph", CdnProvider.ALIBABA, 3),
        CdnRegion("ap-southeast-7", "Asia Pacific (Bangkok)", "Bangkok", "Thailand", "https://alibaba.oss-ap-southeast-7.aliyuncs.com", "th", CdnProvider.ALIBABA, 3),
        // Europe Regions
        CdnRegion("eu-central-1", "Europe (Frankfurt)", "Frankfurt", "Germany", "https://alibaba.oss-eu-central-1.aliyuncs.com", "de", CdnProvider.ALIBABA, 3),
        CdnRegion("eu-west-1", "Europe (London)", "London", "United Kingdom", "https://alibaba.oss-eu-west-1.aliyuncs.com", "gb", CdnProvider.ALIBABA, 3),
        // Middle East Region
        CdnRegion("me-east-1", "Middle East (Dubai)", "Dubai", "UAE", "https://alibaba.oss-me-east-1.aliyuncs.com", "ae", CdnProvider.ALIBABA, 3),
        // US Regions
        CdnRegion("us-east-1", "US (Virginia)", "Virginia", "United States", "https://alibaba.oss-us-east-1.aliyuncs.com", "us", CdnProvider.ALIBABA, 3),
        CdnRegion("us-west-1", "US (Silicon Valley)", "Silicon Valley", "United States", "https://alibaba.oss-us-west-1.aliyuncs.com", "us", CdnProvider.ALIBABA, 3)
    )
}

// Fastly CDN regions data
object FastlyRegions {
    val regions = listOf(
        // Anycast
        CdnRegion("any", "Anycast (Global)", "Global", "Global", "https://any.pops.fastly-analytics.com", "un", CdnProvider.FASTLY, 0),
        // Asia Pacific - Thailand
        CdnRegion("bkk", "Bangkok, Thailand", "Bangkok", "Thailand", "https://bkk.pops.fastly-analytics.com", "th", CdnProvider.FASTLY, 0),
        // Asia Pacific - India
        CdnRegion("ccu", "Kolkata, India", "Kolkata", "India", "https://ccu.pops.fastly-analytics.com", "in", CdnProvider.FASTLY, 0),
        CdnRegion("del", "Delhi, India", "Delhi", "India", "https://del.pops.fastly-analytics.com", "in", CdnProvider.FASTLY, 0),
        CdnRegion("hyd", "Hyderabad, India", "Hyderabad", "India", "https://hyd.pops.fastly-analytics.com", "in", CdnProvider.FASTLY, 0),
        CdnRegion("maa", "Chennai, India", "Chennai", "India", "https://maa.pops.fastly-analytics.com", "in", CdnProvider.FASTLY, 0),
        CdnRegion("vanm", "Mumbai, India", "Mumbai", "India", "https://vanm.pops.fastly-analytics.com", "in", CdnProvider.FASTLY, 0),
        // Asia Pacific - Middle East
        CdnRegion("dxb", "Dubai, UAE", "Dubai", "UAE", "https://dxb.pops.fastly-analytics.com", "ae", CdnProvider.FASTLY, 0),
        CdnRegion("fjr", "Fujairah, UAE", "Fujairah", "UAE", "https://fjr.pops.fastly-analytics.com", "ae", CdnProvider.FASTLY, 0),
        // Asia Pacific - East Asia
        CdnRegion("hkg", "Hong Kong", "Hong Kong", "Hong Kong", "https://hkg.pops.fastly-analytics.com", "hk", CdnProvider.FASTLY, 0),
        CdnRegion("icn", "Seoul, South Korea", "Seoul", "South Korea", "https://icn.pops.fastly-analytics.com", "kr", CdnProvider.FASTLY, 0),
        CdnRegion("itm", "Osaka, Japan", "Osaka", "Japan", "https://itm.pops.fastly-analytics.com", "jp", CdnProvider.FASTLY, 0),
        CdnRegion("rjtf", "Tokyo (Chofu), Japan", "Tokyo", "Japan", "https://rjtf.pops.fastly-analytics.com", "jp", CdnProvider.FASTLY, 0),
        CdnRegion("rjtt", "Tokyo (Narita), Japan", "Tokyo", "Japan", "https://rjtt.pops.fastly-analytics.com", "jp", CdnProvider.FASTLY, 0),
        CdnRegion("tyo", "Tokyo, Japan", "Tokyo", "Japan", "https://tyo.pops.fastly-analytics.com", "jp", CdnProvider.FASTLY, 0),
        // Asia Pacific - Southeast Asia
        CdnRegion("kul", "Kuala Lumpur, Malaysia", "Kuala Lumpur", "Malaysia", "https://kul.pops.fastly-analytics.com", "my", CdnProvider.FASTLY, 0),
        CdnRegion("mnl", "Manila, Philippines", "Manila", "Philippines", "https://mnl.pops.fastly-analytics.com", "ph", CdnProvider.FASTLY, 0),
        CdnRegion("qpg", "Singapore (Paya Lebar)", "Singapore", "Singapore", "https://qpg.pops.fastly-analytics.com", "sg", CdnProvider.FASTLY, 0),
        CdnRegion("wsat", "Singapore", "Singapore", "Singapore", "https://wsat.pops.fastly-analytics.com", "sg", CdnProvider.FASTLY, 0),
        CdnRegion("wsss", "Singapore (Changi)", "Singapore", "Singapore", "https://wsss.pops.fastly-analytics.com", "sg", CdnProvider.FASTLY, 0)
    )
}

// Hetzner Cloud regions data
object HetznerRegions {
    val regions = listOf(
        // Asia Pacific
        CdnRegion("sin", "Singapore", "Singapore", "Singapore", "https://sin-speed.hetzner.com", "sg", CdnProvider.HETZNER, 0),
        // Europe - Germany
        CdnRegion("nbg1", "Nuremberg, Germany", "Nuremberg", "Germany", "https://nbg1-speed.hetzner.com", "de", CdnProvider.HETZNER, 0),
        CdnRegion("fsn1", "Falkenstein, Germany", "Falkenstein", "Germany", "https://fsn1-speed.hetzner.com", "de", CdnProvider.HETZNER, 0),
        // Europe - Finland
        CdnRegion("hel1", "Helsinki, Finland", "Helsinki", "Finland", "https://hel1-speed.hetzner.com", "fi", CdnProvider.HETZNER, 0),
        // United States
        CdnRegion("ash", "Ashburn, VA", "Ashburn", "United States", "https://ash-speed.hetzner.com", "us", CdnProvider.HETZNER, 0),
        CdnRegion("hil", "Hillsboro, OR", "Hillsboro", "United States", "https://hil-speed.hetzner.com", "us", CdnProvider.HETZNER, 0)
    )
}

// OVH Cloud regions data
object OvhRegions {
    val regions = listOf(
        // France
        CdnRegion("sbg", "Strasbourg, France", "Strasbourg", "France", "https://sbg.proof.ovh.net", "fr", CdnProvider.OVH, 0),
        CdnRegion("gra", "Gravelines, France", "Gravelines", "France", "https://gra.proof.ovh.net", "fr", CdnProvider.OVH, 0),
        CdnRegion("rbx", "Roubaix, France", "Roubaix", "France", "https://rbx.proof.ovh.net", "fr", CdnProvider.OVH, 0),
        // Germany
        CdnRegion("de1", "Frankfurt, Germany", "Frankfurt", "Germany", "https://de1.lg.ovh.net", "de", CdnProvider.OVH, 0),
        // United Kingdom
        CdnRegion("uk1", "London, UK", "London", "United Kingdom", "https://uk1.lg.ovh.net", "gb", CdnProvider.OVH, 0),
        CdnRegion("eri", "Erith, UK", "Erith", "United Kingdom", "https://eri.proof.ovh.net", "gb", CdnProvider.OVH, 0),
        // Poland
        CdnRegion("waw", "Warsaw, Poland", "Warsaw", "Poland", "https://waw.lg.ovh.net", "pl", CdnProvider.OVH, 0),
        // United States
        CdnRegion("hil", "Hillsboro, OR", "Hillsboro", "United States", "https://hil.proof.ovh.net", "us", CdnProvider.OVH, 0),
        CdnRegion("vin", "Vint Hill, VA", "Virginia", "United States", "https://vin.proof.ovh.net", "us", CdnProvider.OVH, 0),
        CdnRegion("us1", "US East", "East Coast", "United States", "https://us1.lg.ovh.net", "us", CdnProvider.OVH, 0),
        CdnRegion("us2", "US West", "West Coast", "United States", "https://us2.lg.ovh.net", "us", CdnProvider.OVH, 0),
        // Canada
        CdnRegion("bhs", "Beauharnois, Canada", "Montreal", "Canada", "https://bhs.proof.ovh.net", "ca", CdnProvider.OVH, 0),
        // Asia Pacific
        CdnRegion("sgp", "Singapore", "Singapore", "Singapore", "https://sgp.proof.ovh.net", "sg", CdnProvider.OVH, 0),
        CdnRegion("sin1", "Singapore", "Singapore", "Singapore", "https://sin1.lg.ovh.net", "sg", CdnProvider.OVH, 0),
        CdnRegion("syd", "Sydney, Australia", "Sydney", "Australia", "https://syd.proof.ovh.net", "au", CdnProvider.OVH, 0),
        CdnRegion("syd1", "Sydney, Australia", "Sydney", "Australia", "https://syd1.lg.ovh.net", "au", CdnProvider.OVH, 0),
        CdnRegion("bom", "Mumbai, India", "Mumbai", "India", "https://bom.proof.ovh.net", "in", CdnProvider.OVH, 0)
    )
}

// Google Cloud Platform regions data
object GoogleCloudRegions {
    val regions = listOf(
        // Global
        CdnRegion("global", "Global HTTPS LB", "Global", "Global", "https://global.gcping.com", "un", CdnProvider.GCP, 0),
        // Africa
        CdnRegion("africa-south1", "Johannesburg", "Johannesburg", "South Africa", "https://africa-south1-5tkroniexa-bq.a.run.app", "za", CdnProvider.GCP, 0),
        // Asia - East
        CdnRegion("asia-east1", "Taiwan", "Taiwan", "Taiwan", "https://asia-east1-5tkroniexa-de.a.run.app", "tw", CdnProvider.GCP, 0),
        CdnRegion("asia-east2", "Hong Kong", "Hong Kong", "Hong Kong", "https://asia-east2-5tkroniexa-df.a.run.app", "hk", CdnProvider.GCP, 0),
        // Asia - Northeast
        CdnRegion("asia-northeast1", "Tokyo", "Tokyo", "Japan", "https://asia-northeast1-5tkroniexa-an.a.run.app", "jp", CdnProvider.GCP, 0),
        CdnRegion("asia-northeast2", "Osaka", "Osaka", "Japan", "https://asia-northeast2-5tkroniexa-dt.a.run.app", "jp", CdnProvider.GCP, 0),
        CdnRegion("asia-northeast3", "Seoul", "Seoul", "South Korea", "https://asia-northeast3-5tkroniexa-du.a.run.app", "kr", CdnProvider.GCP, 0),
        // Asia - South
        CdnRegion("asia-south1", "Mumbai", "Mumbai", "India", "https://asia-south1-5tkroniexa-el.a.run.app", "in", CdnProvider.GCP, 0),
        CdnRegion("asia-south2", "Delhi", "Delhi", "India", "https://asia-south2-5tkroniexa-em.a.run.app", "in", CdnProvider.GCP, 0),
        // Asia - Southeast
        CdnRegion("asia-southeast1", "Singapore", "Singapore", "Singapore", "https://asia-southeast1-5tkroniexa-as.a.run.app", "sg", CdnProvider.GCP, 0),
        CdnRegion("asia-southeast2", "Jakarta", "Jakarta", "Indonesia", "https://asia-southeast2-5tkroniexa-et.a.run.app", "id", CdnProvider.GCP, 0),
        // Australia
        CdnRegion("australia-southeast1", "Sydney", "Sydney", "Australia", "https://australia-southeast1-5tkroniexa-ts.a.run.app", "au", CdnProvider.GCP, 0),
        CdnRegion("australia-southeast2", "Melbourne", "Melbourne", "Australia", "https://australia-southeast2-5tkroniexa-km.a.run.app", "au", CdnProvider.GCP, 0),
        // Europe - Central
        CdnRegion("europe-central2", "Warsaw", "Warsaw", "Poland", "https://europe-central2-5tkroniexa-lm.a.run.app", "pl", CdnProvider.GCP, 0),
        // Europe - North
        CdnRegion("europe-north1", "Finland", "Finland", "Finland", "https://europe-north1-5tkroniexa-lz.a.run.app", "fi", CdnProvider.GCP, 0),
        CdnRegion("europe-north2", "Stockholm", "Stockholm", "Sweden", "https://europe-north2-5tkroniexa-ma.a.run.app", "se", CdnProvider.GCP, 0),
        // Europe - Southwest
        CdnRegion("europe-southwest1", "Madrid", "Madrid", "Spain", "https://europe-southwest1-5tkroniexa-no.a.run.app", "es", CdnProvider.GCP, 0),
        // Europe - West
        CdnRegion("europe-west1", "Belgium", "Belgium", "Belgium", "https://europe-west1-5tkroniexa-ew.a.run.app", "be", CdnProvider.GCP, 0),
        CdnRegion("europe-west2", "London", "London", "United Kingdom", "https://europe-west2-5tkroniexa-nw.a.run.app", "gb", CdnProvider.GCP, 0),
        CdnRegion("europe-west3", "Frankfurt", "Frankfurt", "Germany", "https://europe-west3-5tkroniexa-ey.a.run.app", "de", CdnProvider.GCP, 0),
        CdnRegion("europe-west4", "Netherlands", "Netherlands", "Netherlands", "https://europe-west4-5tkroniexa-ez.a.run.app", "nl", CdnProvider.GCP, 0),
        CdnRegion("europe-west6", "Zurich", "Zurich", "Switzerland", "https://europe-west6-5tkroniexa-oa.a.run.app", "ch", CdnProvider.GCP, 0),
        CdnRegion("europe-west8", "Milan", "Milan", "Italy", "https://europe-west8-5tkroniexa-oc.a.run.app", "it", CdnProvider.GCP, 0),
        CdnRegion("europe-west9", "Paris", "Paris", "France", "https://europe-west9-5tkroniexa-od.a.run.app", "fr", CdnProvider.GCP, 0),
        CdnRegion("europe-west10", "Berlin", "Berlin", "Germany", "https://europe-west10-5tkroniexa-oe.a.run.app", "de", CdnProvider.GCP, 0),
        CdnRegion("europe-west12", "Turin", "Turin", "Italy", "https://europe-west12-5tkroniexa-og.a.run.app", "it", CdnProvider.GCP, 0),
        // Middle East
        CdnRegion("me-central1", "Doha", "Doha", "Qatar", "https://me-central1-5tkroniexa-ww.a.run.app", "qa", CdnProvider.GCP, 0),
        CdnRegion("me-central2", "Dammam", "Dammam", "Saudi Arabia", "https://me-central2-5tkroniexa-wx.a.run.app", "sa", CdnProvider.GCP, 0),
        CdnRegion("me-west1", "Tel Aviv", "Tel Aviv", "Israel", "https://me-west1-5tkroniexa-zf.a.run.app", "il", CdnProvider.GCP, 0),
        // North America
        CdnRegion("northamerica-northeast1", "Montréal", "Montreal", "Canada", "https://northamerica-northeast1-5tkroniexa-nn.a.run.app", "ca", CdnProvider.GCP, 0),
        CdnRegion("northamerica-northeast2", "Toronto", "Toronto", "Canada", "https://northamerica-northeast2-5tkroniexa-pd.a.run.app", "ca", CdnProvider.GCP, 0),
        CdnRegion("northamerica-south1", "México", "Mexico City", "Mexico", "https://northamerica-south1-5tkroniexa-pv.a.run.app", "mx", CdnProvider.GCP, 0),
        // South America
        CdnRegion("southamerica-east1", "São Paulo", "Sao Paulo", "Brazil", "https://southamerica-east1-5tkroniexa-rj.a.run.app", "br", CdnProvider.GCP, 0),
        CdnRegion("southamerica-west1", "Santiago", "Santiago", "Chile", "https://southamerica-west1-5tkroniexa-tl.a.run.app", "cl", CdnProvider.GCP, 0),
        // US - Central
        CdnRegion("us-central1", "Iowa", "Iowa", "United States", "https://us-central1-5tkroniexa-uc.a.run.app", "us", CdnProvider.GCP, 0),
        // US - East
        CdnRegion("us-east1", "South Carolina", "South Carolina", "United States", "https://us-east1-5tkroniexa-ue.a.run.app", "us", CdnProvider.GCP, 0),
        CdnRegion("us-east4", "North Virginia", "North Virginia", "United States", "https://us-east4-5tkroniexa-uk.a.run.app", "us", CdnProvider.GCP, 0),
        CdnRegion("us-east5", "Columbus", "Columbus", "United States", "https://us-east5-5tkroniexa-ul.a.run.app", "us", CdnProvider.GCP, 0),
        // US - South
        CdnRegion("us-south1", "Dallas", "Dallas", "United States", "https://us-south1-5tkroniexa-vp.a.run.app", "us", CdnProvider.GCP, 0),
        // US - West
        CdnRegion("us-west1", "Oregon", "Oregon", "United States", "https://us-west1-5tkroniexa-uw.a.run.app", "us", CdnProvider.GCP, 0),
        CdnRegion("us-west2", "Los Angeles", "Los Angeles", "United States", "https://us-west2-5tkroniexa-wl.a.run.app", "us", CdnProvider.GCP, 0),
        CdnRegion("us-west3", "Salt Lake City", "Salt Lake City", "United States", "https://us-west3-5tkroniexa-wm.a.run.app", "us", CdnProvider.GCP, 0),
        CdnRegion("us-west4", "Las Vegas", "Las Vegas", "United States", "https://us-west4-5tkroniexa-wn.a.run.app", "us", CdnProvider.GCP, 0)
    )
}

// Tencent Cloud regions data
object TencentRegions {
    val regions = listOf(
        // China - Mainland
        CdnRegion("ap-beijing", "Beijing", "Beijing", "China", "https://cos.ap-beijing.myqcloud.com", "cn", CdnProvider.TENCENT, 0),
        CdnRegion("ap-chengdu", "Chengdu", "Chengdu", "China", "https://cos.ap-chengdu.myqcloud.com", "cn", CdnProvider.TENCENT, 0),
        CdnRegion("ap-chongqing", "Chongqing", "Chongqing", "China", "https://cos.ap-chongqing.myqcloud.com", "cn", CdnProvider.TENCENT, 0),
        CdnRegion("ap-guangzhou", "Guangzhou", "Guangzhou", "China", "https://cos.ap-guangzhou.myqcloud.com", "cn", CdnProvider.TENCENT, 0),
        CdnRegion("ap-nanjing", "Nanjing", "Nanjing", "China", "https://cos.ap-nanjing.myqcloud.com", "cn", CdnProvider.TENCENT, 0),
        CdnRegion("ap-shanghai", "Shanghai", "Shanghai", "China", "https://cos.ap-shanghai.myqcloud.com", "cn", CdnProvider.TENCENT, 0),
        // China - Finance Zones
        CdnRegion("ap-beijing-fsi", "Beijing Finance", "Beijing", "China", "https://cos.ap-beijing-fsi.myqcloud.com", "cn", CdnProvider.TENCENT, 0),
        CdnRegion("ap-shanghai-fsi", "Shanghai Finance", "Shanghai", "China", "https://cos.ap-shanghai-fsi.myqcloud.com", "cn", CdnProvider.TENCENT, 0),
        CdnRegion("ap-shenzhen-fsi", "Shenzhen Finance", "Shenzhen", "China", "https://cos.ap-shenzhen-fsi.myqcloud.com", "cn", CdnProvider.TENCENT, 0),
        // Asia Pacific
        CdnRegion("ap-hongkong", "Hong Kong, China", "Hong Kong", "Hong Kong", "https://cos.ap-hongkong.myqcloud.com", "hk", CdnProvider.TENCENT, 0),
        CdnRegion("ap-bangkok", "Bangkok, Thailand", "Bangkok", "Thailand", "https://cos.ap-bangkok.myqcloud.com", "th", CdnProvider.TENCENT, 0),
        CdnRegion("ap-jakarta", "Jakarta, Indonesia", "Jakarta", "Indonesia", "https://cos.ap-jakarta.myqcloud.com", "id", CdnProvider.TENCENT, 0),
        CdnRegion("ap-seoul", "Seoul, Korea", "Seoul", "South Korea", "https://cos.ap-seoul.myqcloud.com", "kr", CdnProvider.TENCENT, 0),
        CdnRegion("ap-singapore", "Singapore", "Singapore", "Singapore", "https://cos.ap-singapore.myqcloud.com", "sg", CdnProvider.TENCENT, 0),
        CdnRegion("ap-tokyo", "Tokyo, Japan", "Tokyo", "Japan", "https://cos.ap-tokyo.myqcloud.com", "jp", CdnProvider.TENCENT, 0),
        // Europe
        CdnRegion("eu-frankfurt", "Frankfurt, Germany", "Frankfurt", "Germany", "https://cos.eu-frankfurt.myqcloud.com", "de", CdnProvider.TENCENT, 0),
        // North America
        CdnRegion("na-ashburn", "Ashburn, VA", "Ashburn", "United States", "https://cos.na-ashburn.myqcloud.com", "us", CdnProvider.TENCENT, 0),
        CdnRegion("na-siliconvalley", "Silicon Valley, CA", "Silicon Valley", "United States", "https://cos.na-siliconvalley.myqcloud.com", "us", CdnProvider.TENCENT, 0),
        // South America
        CdnRegion("sa-saopaulo", "Sao Paulo, Brazil", "Sao Paulo", "Brazil", "https://cos.sa-saopaulo.myqcloud.com", "br", CdnProvider.TENCENT, 0)
    )
}

// Huawei Cloud regions data
object HuaweiRegions {
    val regions = listOf(
        // China - North
        CdnRegion("cn-north-1", "Beijing 1", "Beijing", "China", "https://obs.cn-north-1.myhuaweicloud.com", "cn", CdnProvider.HUAWEI, 0),
        CdnRegion("cn-north-4", "Beijing 4", "Beijing", "China", "https://obs.cn-north-4.myhuaweicloud.com", "cn", CdnProvider.HUAWEI, 0),
        CdnRegion("cn-north-9", "Wulanchabu", "Wulanchabu", "China", "https://obs.cn-north-9.myhuaweicloud.com", "cn", CdnProvider.HUAWEI, 0),
        // China - South
        CdnRegion("cn-south-1", "Guangzhou", "Guangzhou", "China", "https://obs.cn-south-1.myhuaweicloud.com", "cn", CdnProvider.HUAWEI, 0),
        // China - Southwest
        CdnRegion("cn-southwest-2", "Guiyang 1", "Guiyang", "China", "https://obs.cn-southwest-2.myhuaweicloud.com", "cn", CdnProvider.HUAWEI, 0),
        // China - East
        CdnRegion("cn-east-2", "Shanghai 2", "Shanghai", "China", "https://obs.cn-east-2.myhuaweicloud.com", "cn", CdnProvider.HUAWEI, 0),
        CdnRegion("cn-east-3", "Shanghai 1", "Shanghai", "China", "https://obs.cn-east-3.myhuaweicloud.com", "cn", CdnProvider.HUAWEI, 0),
        // Asia Pacific
        CdnRegion("ap-southeast-1", "Hong Kong, China", "Hong Kong", "Hong Kong", "https://obs.ap-southeast-1.myhuaweicloud.com", "hk", CdnProvider.HUAWEI, 0),
        CdnRegion("ap-southeast-2", "Bangkok, Thailand", "Bangkok", "Thailand", "https://obs.ap-southeast-2.myhuaweicloud.com", "th", CdnProvider.HUAWEI, 0),
        CdnRegion("ap-southeast-3", "Singapore", "Singapore", "Singapore", "https://obs.ap-southeast-3.myhuaweicloud.com", "sg", CdnProvider.HUAWEI, 0),
        // Americas - Mexico
        CdnRegion("na-mexico-1", "Mexico City 1", "Mexico City", "Mexico", "https://obs.na-mexico-1.myhuaweicloud.com", "mx", CdnProvider.HUAWEI, 0),
        CdnRegion("la-north-2", "Mexico City 2", "Mexico City", "Mexico", "https://obs.la-north-2.myhuaweicloud.com", "mx", CdnProvider.HUAWEI, 0),
        // South America
        CdnRegion("sa-brazil-1", "Sao Paulo, Brazil", "Sao Paulo", "Brazil", "https://obs.sa-brazil-1.myhuaweicloud.com", "br", CdnProvider.HUAWEI, 0),
        CdnRegion("la-south-2", "Santiago, Chile", "Santiago", "Chile", "https://obs.la-south-2.myhuaweicloud.com", "cl", CdnProvider.HUAWEI, 0),
        // Africa
        CdnRegion("af-south-1", "Johannesburg, South Africa", "Johannesburg", "South Africa", "https://obs.af-south-1.myhuaweicloud.com", "za", CdnProvider.HUAWEI, 0),
        // Russia
        CdnRegion("ru-moscow-1", "Moscow, Russia", "Moscow", "Russia", "https://obs.ru-moscow-1.hc.sbercloud.ru", "ru", CdnProvider.HUAWEI, 0)
    )
}

// Oracle Cloud regions data
object OracleRegions {
    val regions = listOf(
        // Asia Pacific - Korea
        CdnRegion("ap-chuncheon-1", "Chuncheon, Korea", "Chuncheon", "South Korea", "https://objectstorage.ap-chuncheon-1.oraclecloud.com", "kr", CdnProvider.ORACLE, 0),
        CdnRegion("ap-seoul-1", "Seoul, Korea", "Seoul", "South Korea", "https://objectstorage.ap-seoul-1.oraclecloud.com", "kr", CdnProvider.ORACLE, 0),
        // Asia Pacific - India
        CdnRegion("ap-hyderabad-1", "Hyderabad, India", "Hyderabad", "India", "https://objectstorage.ap-hyderabad-1.oraclecloud.com", "in", CdnProvider.ORACLE, 0),
        CdnRegion("ap-mumbai-1", "Mumbai, India", "Mumbai", "India", "https://objectstorage.ap-mumbai-1.oraclecloud.com", "in", CdnProvider.ORACLE, 0),
        // Asia Pacific - Japan
        CdnRegion("ap-osaka-1", "Osaka, Japan", "Osaka", "Japan", "https://objectstorage.ap-osaka-1.oraclecloud.com", "jp", CdnProvider.ORACLE, 0),
        CdnRegion("ap-tokyo-1", "Tokyo, Japan", "Tokyo", "Japan", "https://objectstorage.ap-tokyo-1.oraclecloud.com", "jp", CdnProvider.ORACLE, 0),
        // Asia Pacific - Australia
        CdnRegion("ap-sydney-1", "Sydney, Australia", "Sydney", "Australia", "https://objectstorage.ap-sydney-1.oraclecloud.com", "au", CdnProvider.ORACLE, 0),
        CdnRegion("ap-melbourne-1", "Melbourne, Australia", "Melbourne", "Australia", "https://objectstorage.ap-melbourne-1.oraclecloud.com", "au", CdnProvider.ORACLE, 0),
        // US Regions
        CdnRegion("us-ashburn-1", "Ashburn, VA", "Ashburn", "United States", "https://objectstorage.us-ashburn-1.oraclecloud.com", "us", CdnProvider.ORACLE, 0),
        CdnRegion("us-phoenix-1", "Phoenix, AZ", "Phoenix", "United States", "https://objectstorage.us-phoenix-1.oraclecloud.com", "us", CdnProvider.ORACLE, 0),
        CdnRegion("us-sanjose-1", "San Jose, CA", "San Jose", "United States", "https://objectstorage.us-sanjose-1.oraclecloud.com", "us", CdnProvider.ORACLE, 0),
        // Canada
        CdnRegion("ca-montreal-1", "Montréal, Canada", "Montreal", "Canada", "https://objectstorage.ca-montreal-1.oraclecloud.com", "ca", CdnProvider.ORACLE, 0),
        CdnRegion("ca-toronto-1", "Toronto, Canada", "Toronto", "Canada", "https://objectstorage.ca-toronto-1.oraclecloud.com", "ca", CdnProvider.ORACLE, 0),
        // Europe - UK
        CdnRegion("uk-cardiff-1", "Cardiff, UK", "Cardiff", "United Kingdom", "https://objectstorage.uk-cardiff-1.oraclecloud.com", "gb", CdnProvider.ORACLE, 0),
        CdnRegion("uk-london-1", "London, UK", "London", "United Kingdom", "https://objectstorage.uk-london-1.oraclecloud.com", "gb", CdnProvider.ORACLE, 0),
        // Europe - Other
        CdnRegion("eu-amsterdam-1", "Amsterdam, Netherlands", "Amsterdam", "Netherlands", "https://objectstorage.eu-amsterdam-1.oraclecloud.com", "nl", CdnProvider.ORACLE, 0),
        CdnRegion("eu-frankfurt-1", "Frankfurt, Germany", "Frankfurt", "Germany", "https://objectstorage.eu-frankfurt-1.oraclecloud.com", "de", CdnProvider.ORACLE, 0),
        CdnRegion("eu-zurich-1", "Zurich, Switzerland", "Zurich", "Switzerland", "https://objectstorage.eu-zurich-1.oraclecloud.com", "ch", CdnProvider.ORACLE, 0),
        // Middle East
        CdnRegion("me-dubai-1", "Dubai, UAE", "Dubai", "UAE", "https://objectstorage.me-dubai-1.oraclecloud.com", "ae", CdnProvider.ORACLE, 0),
        CdnRegion("me-jeddah-1", "Jeddah, Saudi Arabia", "Jeddah", "Saudi Arabia", "https://objectstorage.me-jeddah-1.oraclecloud.com", "sa", CdnProvider.ORACLE, 0),
        // South America
        CdnRegion("sa-santiago-1", "Santiago, Chile", "Santiago", "Chile", "https://objectstorage.sa-santiago-1.oraclecloud.com", "cl", CdnProvider.ORACLE, 0),
        CdnRegion("sa-saopaulo-1", "Sao Paulo, Brazil", "Sao Paulo", "Brazil", "https://objectstorage.sa-saopaulo-1.oraclecloud.com", "br", CdnProvider.ORACLE, 0)
    )
}

// DigitalOcean regions data
object DigitalOceanRegions {
    val regions = listOf(
        // US Regions - New York
        CdnRegion("nyc1", "New York 1", "New York", "United States", "https://speedtest-nyc1.digitalocean.com", "us", CdnProvider.DIGITALOCEAN, 0),
        CdnRegion("nyc2", "New York 2", "New York", "United States", "https://speedtest-nyc2.digitalocean.com", "us", CdnProvider.DIGITALOCEAN, 0),
        CdnRegion("nyc3", "New York 3", "New York", "United States", "https://speedtest-nyc3.digitalocean.com", "us", CdnProvider.DIGITALOCEAN, 0),
        // US Regions - San Francisco
        CdnRegion("sfo1", "San Francisco 1", "San Francisco", "United States", "https://speedtest-sfo1.digitalocean.com", "us", CdnProvider.DIGITALOCEAN, 0),
        CdnRegion("sfo2", "San Francisco 2", "San Francisco", "United States", "https://speedtest-sfo2.digitalocean.com", "us", CdnProvider.DIGITALOCEAN, 0),
        CdnRegion("sfo3", "San Francisco 3", "San Francisco", "United States", "https://speedtest-sfo3.digitalocean.com", "us", CdnProvider.DIGITALOCEAN, 0),
        // Canada
        CdnRegion("tor1", "Toronto 1", "Toronto", "Canada", "https://speedtest-tor1.digitalocean.com", "ca", CdnProvider.DIGITALOCEAN, 0),
        // Europe - Amsterdam
        CdnRegion("ams2", "Amsterdam 2", "Amsterdam", "Netherlands", "https://speedtest-ams2.digitalocean.com", "nl", CdnProvider.DIGITALOCEAN, 0),
        CdnRegion("ams3", "Amsterdam 3", "Amsterdam", "Netherlands", "https://speedtest-ams3.digitalocean.com", "nl", CdnProvider.DIGITALOCEAN, 0),
        // Europe - Other
        CdnRegion("lon1", "London 1", "London", "United Kingdom", "https://speedtest-lon1.digitalocean.com", "gb", CdnProvider.DIGITALOCEAN, 0),
        CdnRegion("fra1", "Frankfurt 1", "Frankfurt", "Germany", "https://speedtest-fra1.digitalocean.com", "de", CdnProvider.DIGITALOCEAN, 0),
        // Asia Pacific
        CdnRegion("sgp1", "Singapore 1", "Singapore", "Singapore", "https://speedtest-sgp1.digitalocean.com", "sg", CdnProvider.DIGITALOCEAN, 0),
        CdnRegion("blr1", "Bangalore 1", "Bangalore", "India", "https://speedtest-blr1.digitalocean.com", "in", CdnProvider.DIGITALOCEAN, 0)
    )
}

// AWS Lightsail regions data
object LightsailRegions {
    val regions = listOf(
        // US Regions
        CdnRegion("us-east-1", "US East (Virginia)", "N. Virginia", "United States", "https://lightsail.us-east-1.amazonaws.com/ping", "us", CdnProvider.LIGHTSAIL, 0),
        CdnRegion("us-east-2", "US East (Ohio)", "Ohio", "United States", "https://lightsail.us-east-2.amazonaws.com/ping", "us", CdnProvider.LIGHTSAIL, 0),
        CdnRegion("us-west-2", "US West (Oregon)", "Oregon", "United States", "https://lightsail.us-west-2.amazonaws.com/ping", "us", CdnProvider.LIGHTSAIL, 0),
        // Canada
        CdnRegion("ca-central-1", "Canada (Central)", "Montreal", "Canada", "https://lightsail.ca-central-1.amazonaws.com/ping", "ca", CdnProvider.LIGHTSAIL, 0),
        // Europe
        CdnRegion("eu-west-1", "Europe (Ireland)", "Dublin", "Ireland", "https://lightsail.eu-west-1.amazonaws.com/ping", "ie", CdnProvider.LIGHTSAIL, 0),
        CdnRegion("eu-west-2", "Europe (London)", "London", "United Kingdom", "https://lightsail.eu-west-2.amazonaws.com/ping", "gb", CdnProvider.LIGHTSAIL, 0),
        CdnRegion("eu-west-3", "Europe (Paris)", "Paris", "France", "https://lightsail.eu-west-3.amazonaws.com/ping", "fr", CdnProvider.LIGHTSAIL, 0),
        CdnRegion("eu-central-1", "Europe (Frankfurt)", "Frankfurt", "Germany", "https://lightsail.eu-central-1.amazonaws.com/ping", "de", CdnProvider.LIGHTSAIL, 0),
        CdnRegion("eu-north-1", "Europe (Stockholm)", "Stockholm", "Sweden", "https://lightsail.eu-north-1.amazonaws.com/ping", "se", CdnProvider.LIGHTSAIL, 0),
        // Asia Pacific
        CdnRegion("ap-south-1", "Asia Pacific (Mumbai)", "Mumbai", "India", "https://lightsail.ap-south-1.amazonaws.com/ping", "in", CdnProvider.LIGHTSAIL, 0),
        CdnRegion("ap-northeast-1", "Asia Pacific (Tokyo)", "Tokyo", "Japan", "https://lightsail.ap-northeast-1.amazonaws.com/ping", "jp", CdnProvider.LIGHTSAIL, 0),
        CdnRegion("ap-northeast-2", "Asia Pacific (Seoul)", "Seoul", "South Korea", "https://lightsail.ap-northeast-2.amazonaws.com/ping", "kr", CdnProvider.LIGHTSAIL, 0),
        CdnRegion("ap-southeast-1", "Asia Pacific (Singapore)", "Singapore", "Singapore", "https://lightsail.ap-southeast-1.amazonaws.com/ping", "sg", CdnProvider.LIGHTSAIL, 0),
        CdnRegion("ap-southeast-2", "Asia Pacific (Sydney)", "Sydney", "Australia", "https://lightsail.ap-southeast-2.amazonaws.com/ping", "au", CdnProvider.LIGHTSAIL, 0)
    )
}

// Linode regions data
object LinodeRegions {
    val regions = listOf(
        // US Regions
        CdnRegion("newark", "US East", "Newark, NJ", "United States", "https://speedtest.newark.linode.com/empty.php", "us", CdnProvider.LINODE, 0),
        CdnRegion("atlanta", "US Southeast", "Atlanta, GA", "United States", "https://speedtest.atlanta.linode.com/empty.php", "us", CdnProvider.LINODE, 0),
        CdnRegion("dallas", "US Central", "Dallas, TX", "United States", "https://speedtest.dallas.linode.com/empty.php", "us", CdnProvider.LINODE, 0),
        CdnRegion("fremont", "US West", "Fremont, CA", "United States", "https://speedtest.fremont.linode.com/empty.php", "us", CdnProvider.LINODE, 0),
        // Canada
        CdnRegion("toronto1", "CA Central", "Toronto", "Canada", "https://speedtest.toronto1.linode.com/empty.php", "ca", CdnProvider.LINODE, 0),
        // Europe
        CdnRegion("frankfurt", "EU Central", "Frankfurt", "Germany", "https://speedtest.frankfurt.linode.com/empty.php", "de", CdnProvider.LINODE, 0),
        CdnRegion("london", "EU West", "London", "United Kingdom", "https://speedtest.london.linode.com/empty.php", "gb", CdnProvider.LINODE, 0),
        // Asia Pacific
        CdnRegion("singapore", "AP South", "Singapore", "Singapore", "https://speedtest.singapore.linode.com/empty.php", "sg", CdnProvider.LINODE, 0),
        CdnRegion("syd1", "AP Southeast", "Sydney", "Australia", "https://speedtest.syd1.linode.com/empty.php", "au", CdnProvider.LINODE, 0),
        CdnRegion("tokyo2", "AP Northeast", "Tokyo", "Japan", "https://speedtest.tokyo2.linode.com/empty.php", "jp", CdnProvider.LINODE, 0),
        CdnRegion("mumbai1", "AP West", "Mumbai", "India", "https://speedtest.mumbai1.linode.com/empty.php", "in", CdnProvider.LINODE, 0)
    )
}

// Vultr regions data
object VultrRegions {
    val regions = listOf(
        // US Regions
        CdnRegion("wa-us", "Seattle, WA", "Seattle", "United States", "https://wa-us-ping.vultr.com/robots.txt", "us", CdnProvider.VULTR, 0),
        CdnRegion("lax-ca-us", "Los Angeles, CA", "Los Angeles", "United States", "https://lax-ca-us-ping.vultr.com/robots.txt", "us", CdnProvider.VULTR, 0),
        CdnRegion("sjo-ca-us", "Silicon Valley, CA", "Silicon Valley", "United States", "https://sjo-ca-us-ping.vultr.com/robots.txt", "us", CdnProvider.VULTR, 0),
        CdnRegion("fl-us", "Miami, FL", "Miami", "United States", "https://fl-us-ping.vultr.com/robots.txt", "us", CdnProvider.VULTR, 0),
        CdnRegion("ga-us", "Atlanta, GA", "Atlanta", "United States", "https://ga-us-ping.vultr.com/robots.txt", "us", CdnProvider.VULTR, 0),
        CdnRegion("hon-hi-us", "Honolulu, HI", "Honolulu", "United States", "https://hon-hi-us-ping.vultr.com/robots.txt", "us", CdnProvider.VULTR, 0),
        CdnRegion("il-us", "Chicago, IL", "Chicago", "United States", "https://il-us-ping.vultr.com/robots.txt", "us", CdnProvider.VULTR, 0),
        CdnRegion("nj-us", "New Jersey, NJ", "New Jersey", "United States", "https://nj-us-ping.vultr.com/robots.txt", "us", CdnProvider.VULTR, 0),
        CdnRegion("tx-us", "Dallas, TX", "Dallas", "United States", "https://tx-us-ping.vultr.com/robots.txt", "us", CdnProvider.VULTR, 0),
        // Americas
        CdnRegion("tor-ca", "Toronto, Canada", "Toronto", "Canada", "https://tor-ca-ping.vultr.com/robots.txt", "ca", CdnProvider.VULTR, 0),
        CdnRegion("mex-mx", "Mexico City, Mexico", "Mexico City", "Mexico", "https://mex-mx-ping.vultr.com/robots.txt", "mx", CdnProvider.VULTR, 0),
        CdnRegion("sao-br", "Sao Paulo, Brazil", "Sao Paulo", "Brazil", "https://sao-br-ping.vultr.com/robots.txt", "br", CdnProvider.VULTR, 0),
        // Europe
        CdnRegion("fra-de", "Frankfurt, Germany", "Frankfurt", "Germany", "https://fra-de-ping.vultr.com/robots.txt", "de", CdnProvider.VULTR, 0),
        CdnRegion("mad-es", "Madrid, Spain", "Madrid", "Spain", "https://mad-es-ping.vultr.com/robots.txt", "es", CdnProvider.VULTR, 0),
        CdnRegion("par-fr", "Paris, France", "Paris", "France", "https://par-fr-ping.vultr.com/robots.txt", "fr", CdnProvider.VULTR, 0),
        CdnRegion("lon-gb", "London, UK", "London", "United Kingdom", "https://lon-gb-ping.vultr.com/robots.txt", "gb", CdnProvider.VULTR, 0),
        CdnRegion("ams-nl", "Amsterdam, Netherlands", "Amsterdam", "Netherlands", "https://ams-nl-ping.vultr.com/robots.txt", "nl", CdnProvider.VULTR, 0),
        CdnRegion("waw-pl", "Warsaw, Poland", "Warsaw", "Poland", "https://waw-pl-ping.vultr.com/robots.txt", "pl", CdnProvider.VULTR, 0),
        CdnRegion("sto-se", "Stockholm, Sweden", "Stockholm", "Sweden", "https://sto-se-ping.vultr.com/robots.txt", "se", CdnProvider.VULTR, 0),
        // Asia Pacific
        CdnRegion("hnd-jp", "Tokyo, Japan", "Tokyo", "Japan", "https://hnd-jp-ping.vultr.com/robots.txt", "jp", CdnProvider.VULTR, 0),
        CdnRegion("sel-kor", "Seoul, Korea", "Seoul", "South Korea", "https://sel-kor-ping.vultr.com/robots.txt", "kr", CdnProvider.VULTR, 0),
        CdnRegion("sgp", "Singapore", "Singapore", "Singapore", "https://sgp-ping.vultr.com/robots.txt", "sg", CdnProvider.VULTR, 0),
        CdnRegion("syd-au", "Sydney, Australia", "Sydney", "Australia", "https://syd-au-ping.vultr.com/robots.txt", "au", CdnProvider.VULTR, 0),
        CdnRegion("mel-au", "Melbourne, Australia", "Melbourne", "Australia", "https://mel-au-ping.vultr.com/robots.txt", "au", CdnProvider.VULTR, 0)
    )
}

// AWS regions data
object AwsRegions {
    val regions = listOf(
        CdnRegion("af-south-1", "Africa (Cape Town)", "Cape Town", "South Africa", "https://ec2.af-south-1.amazonaws.com", "za", CdnProvider.AWS, 3),
        CdnRegion("ap-east-1", "Asia Pacific (Hong Kong)", "Hong Kong", "Hong Kong", "https://ec2.ap-east-1.amazonaws.com", "hk", CdnProvider.AWS, 3),
        CdnRegion("ap-east-2", "Asia Pacific (Taipei)", "Taipei", "Taiwan", "https://ec2.ap-east-2.amazonaws.com", "tw", CdnProvider.AWS, 3),
        CdnRegion("ap-northeast-1", "Asia Pacific (Tokyo)", "Tokyo", "Japan", "https://ec2.ap-northeast-1.amazonaws.com", "jp", CdnProvider.AWS, 3),
        CdnRegion("ap-northeast-2", "Asia Pacific (Seoul)", "Seoul", "South Korea", "https://ec2.ap-northeast-2.amazonaws.com", "kr", CdnProvider.AWS, 4),
        CdnRegion("ap-northeast-3", "Asia Pacific (Osaka)", "Osaka", "Japan", "https://ec2.ap-northeast-3.amazonaws.com", "jp", CdnProvider.AWS, 3),
        CdnRegion("ap-south-1", "Asia Pacific (Mumbai)", "Mumbai", "India", "https://ec2.ap-south-1.amazonaws.com", "in", CdnProvider.AWS, 3),
        CdnRegion("ap-south-2", "Asia Pacific (Hyderabad)", "Hyderabad", "India", "https://ec2.ap-south-2.amazonaws.com", "in", CdnProvider.AWS, 3),
        CdnRegion("ap-southeast-1", "Asia Pacific (Singapore)", "Singapore", "Singapore", "https://ec2.ap-southeast-1.amazonaws.com", "sg", CdnProvider.AWS, 3),
        CdnRegion("ap-southeast-2", "Asia Pacific (Sydney)", "Sydney", "Australia", "https://ec2.ap-southeast-2.amazonaws.com", "au", CdnProvider.AWS, 3),
        CdnRegion("ap-southeast-3", "Asia Pacific (Jakarta)", "Jakarta", "Indonesia", "https://ec2.ap-southeast-3.amazonaws.com", "id", CdnProvider.AWS, 3),
        CdnRegion("ap-southeast-4", "Asia Pacific (Melbourne)", "Melbourne", "Australia", "https://ec2.ap-southeast-4.amazonaws.com", "au", CdnProvider.AWS, 3),
        CdnRegion("ap-southeast-5", "Asia Pacific (Malaysia)", "Malaysia", "Malaysia", "https://ec2.ap-southeast-5.amazonaws.com", "my", CdnProvider.AWS, 3),
        CdnRegion("ap-southeast-6", "Asia Pacific (New Zealand)", "New Zealand", "New Zealand", "https://ec2.ap-southeast-6.amazonaws.com", "nz", CdnProvider.AWS, 3),
        CdnRegion("ap-southeast-7", "Asia Pacific (Thailand)", "Thailand", "Thailand", "https://ec2.ap-southeast-7.amazonaws.com", "th", CdnProvider.AWS, 3),
        CdnRegion("ca-central-1", "Canada (Central)", "Central", "Canada", "https://ec2.ca-central-1.amazonaws.com", "ca", CdnProvider.AWS, 3),
        CdnRegion("ca-west-1", "Canada West (Calgary)", "Calgary", "Canada", "https://ec2.ca-west-1.amazonaws.com", "ca", CdnProvider.AWS, 3),
        CdnRegion("eu-central-1", "Europe (Frankfurt)", "Frankfurt", "Germany", "https://ec2.eu-central-1.amazonaws.com", "de", CdnProvider.AWS, 3),
        CdnRegion("eu-central-2", "Europe (Zurich)", "Zurich", "Switzerland", "https://ec2.eu-central-2.amazonaws.com", "ch", CdnProvider.AWS, 3),
        CdnRegion("eu-north-1", "Europe (Stockholm)", "Stockholm", "Sweden", "https://ec2.eu-north-1.amazonaws.com", "se", CdnProvider.AWS, 3),
        CdnRegion("eu-south-1", "Europe (Milan)", "Milan", "Italy", "https://ec2.eu-south-1.amazonaws.com", "it", CdnProvider.AWS, 3),
        CdnRegion("eu-south-2", "Europe (Spain)", "Spain", "Spain", "https://ec2.eu-south-2.amazonaws.com", "es", CdnProvider.AWS, 3),
        CdnRegion("eu-west-1", "Europe (Ireland)", "Ireland", "Ireland", "https://ec2.eu-west-1.amazonaws.com", "ie", CdnProvider.AWS, 3),
        CdnRegion("eu-west-2", "Europe (London)", "London", "United Kingdom", "https://ec2.eu-west-2.amazonaws.com", "gb", CdnProvider.AWS, 3),
        CdnRegion("eu-west-3", "Europe (Paris)", "Paris", "France", "https://ec2.eu-west-3.amazonaws.com", "fr", CdnProvider.AWS, 3),
        CdnRegion("il-central-1", "Israel (Tel Aviv)", "Tel Aviv", "Israel", "https://ec2.il-central-1.amazonaws.com", "il", CdnProvider.AWS, 3),
        CdnRegion("me-central-1", "Middle East (UAE)", "UAE", "United Arab Emirates", "https://ec2.me-central-1.amazonaws.com", "ae", CdnProvider.AWS, 3),
        CdnRegion("me-south-1", "Middle East (Bahrain)", "Bahrain", "Bahrain", "https://ec2.me-south-1.amazonaws.com", "bh", CdnProvider.AWS, 3),
        CdnRegion("mx-central-1", "Mexico (Central)", "Central", "Mexico", "https://ec2.mx-central-1.amazonaws.com", "mx", CdnProvider.AWS, 3),
        CdnRegion("sa-east-1", "South America (Sao Paulo)", "Sao Paulo", "Brazil", "https://ec2.sa-east-1.amazonaws.com", "br", CdnProvider.AWS, 3),
        CdnRegion("us-east-1", "US East (N. Virginia)", "N. Virginia", "United States", "https://ec2.us-east-1.amazonaws.com", "us", CdnProvider.AWS, 6),
        CdnRegion("us-east-2", "US East (Ohio)", "Ohio", "United States", "https://ec2.us-east-2.amazonaws.com", "us", CdnProvider.AWS, 3),
        CdnRegion("us-west-1", "US West (N. California)", "N. California", "United States", "https://ec2.us-west-1.amazonaws.com", "us", CdnProvider.AWS, 2),
        CdnRegion("us-west-2", "US West (Oregon)", "Oregon", "United States", "https://ec2.us-west-2.amazonaws.com", "us", CdnProvider.AWS, 4)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CdnLatencyTestScreen(navController: NavController) {
    var selectedProvider by remember { mutableStateOf(CdnProvider.AZURE) }
    val regions = remember { mutableStateListOf<CdnRegion>().apply { addAll(AzureRegions.regions) } }
    var isTesting by remember { mutableStateOf(false) }
    var testProgress by remember { mutableStateOf(0) }
    var sortBy by remember { mutableStateOf("latency") }
    var filterQuery by remember { mutableStateOf("") }
    var showSortMenu by remember { mutableStateOf(false) }
    var cloudflareCdn by remember { mutableStateOf<UserCdnInfo?>(null) }
    var cloudfrontCdn by remember { mutableStateOf<UserCdnInfo?>(null) }
    var alibabaCdn by remember { mutableStateOf<UserCdnInfo?>(null) }

    // Continuous ping state
    var isContinuousPinging by remember { mutableStateOf(false) }
    var continuousPingJob by remember { mutableStateOf<Job?>(null) }
    var selectedPingInterval by remember { mutableStateOf(CdnPingInterval.NORMAL) }
    val monitoredRegions = remember { mutableStateListOf<String>() } // Region IDs
    val pingHistories = remember { mutableStateMapOf<String, PingHistory>() }
    var showIntervalMenu by remember { mutableStateOf(false) }
    val maxDataPoints = 30 // Show last 30 data points in graph
    var pingUpdateTrigger by remember { mutableIntStateOf(0) } // Force recomposition trigger

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // Detect Cloudflare CDN location
    suspend fun detectCloudflare(): UserCdnInfo = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://1.1.1.1/cdn-cgi/trace")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000

            val response = BufferedReader(InputStreamReader(connection.inputStream)).use { it.readText() }
            connection.disconnect()

            // Parse the response to extract colo and loc
            val coloLine = response.lines().find { it.startsWith("colo=") }
            val locLine = response.lines().find { it.startsWith("loc=") }
            val colo = coloLine?.substringAfter("colo=")?.trim() ?: "Unknown"
            val loc = locLine?.substringAfter("loc=")?.trim() ?: ""

            // Display code as "colo (country)" if loc is available
            val displayCode = if (loc.isNotEmpty()) "$colo ($loc)" else colo

            UserCdnInfo(
                provider = "Cloudflare",
                location = getCdnLocationName(colo),
                code = displayCode,
                isLoading = false,
                error = null
            )
        } catch (e: Exception) {
            UserCdnInfo(
                provider = "Cloudflare",
                location = "Unknown",
                code = "N/A",
                isLoading = false,
                error = e.message
            )
        }
    }
    
    // Detect CloudFront CDN location
    suspend fun detectCloudFront(): UserCdnInfo = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://000057.awsstudygroup.com/images/cf/7.3/0005.png?t=${System.currentTimeMillis()}")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "HEAD"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.connect()
            
            val pop = connection.getHeaderField("x-amz-cf-pop") ?: "Unknown"
            connection.disconnect()
            
            // Extract location code from POP (e.g., "KUL50-P2" -> "KUL")
            val locationCode = if (pop.length >= 3) pop.substring(0, 3) else pop
            
            UserCdnInfo(
                provider = "CloudFront",
                location = getCdnLocationName(locationCode),
                code = pop,
                isLoading = false,
                error = null
            )
        } catch (e: Exception) {
            UserCdnInfo(
                provider = "CloudFront",
                location = "Unknown",
                code = "N/A",
                isLoading = false,
                error = e.message
            )
        }
    }
    
    // Detect Alibaba Cloud CDN location
    suspend fun detectAlibaba(): UserCdnInfo = withContext(Dispatchers.IO) {
        try {
            val url = URL("https://alibaba.oss-cn-zhangjiakou.aliyuncs.com")
            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "HEAD"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.connect()
            
            // Try to extract region from response headers or URL redirection
            val server = connection.getHeaderField("Server") ?: ""
            val xOssRequestId = connection.getHeaderField("x-oss-request-id") ?: ""
            connection.disconnect()
            
            // Extract region from x-oss-request-id if available
            // Format: 65B1234567890ABCDEF01234
            val region = if (xOssRequestId.isNotEmpty()) {
                // Default to cn-zhangjiakou if we can't determine
                "cn-zhangjiakou"
            } else {
                "cn-zhangjiakou"
            }
            
            val alibabaLocationMap = mapOf(
                "bjs" to "Beijing",
                "can" to "Guangzhou",
                "hgh" to "Hangzhou",
                "heyuan" to "Heyuan",
                "het" to "Hohhot",
                "tao" to "Qingdao",
                "sha" to "Shanghai",
                "szx" to "Shenzhen",
                "ucb" to "Ulanqab",
                "zqz" to "Zhangjiakou",
                "ctu" to "Chengdu",
                "hkg" to "Hong Kong",
                "nrt" to "Tokyo",
                "icn" to "Seoul",
                "sin" to "Singapore",
                "kul" to "Kuala Lumpur",
                "cgk" to "Jakarta",
                "mnl" to "Manila",
                "bkk" to "Bangkok",
                "fra" to "Frankfurt",
                "lhr" to "London",
                "dxb" to "Dubai",
                "virginia" to "Virginia",
                "silicon-valley" to "Silicon Valley"
            )
            
            UserCdnInfo(
                provider = "Alibaba Cloud",
                location = alibabaLocationMap[region] ?: "Zhangjiakou, China",
                code = region,
                isLoading = false,
                error = null
            )
        } catch (e: Exception) {
            UserCdnInfo(
                provider = "Alibaba Cloud",
                location = "Unknown",
                code = "N/A",
                isLoading = false,
                error = e.message
            )
        }
    }
    
    // Detect user's connected CDNs on component mount
    LaunchedEffect(Unit) {
        cloudflareCdn = UserCdnInfo("Cloudflare", "Detecting...", "", isLoading = true)
        cloudfrontCdn = UserCdnInfo("CloudFront", "Detecting...", "", isLoading = true)
        alibabaCdn = UserCdnInfo("Alibaba Cloud", "Detecting...", "", isLoading = true)
        
        launch {
            cloudflareCdn = detectCloudflare()
        }
        launch {
            cloudfrontCdn = detectCloudFront()
        }
        launch {
            alibabaCdn = detectAlibaba()
        }
    }

    // Switch provider
    fun switchProvider(provider: CdnProvider) {
        selectedProvider = provider
        regions.clear()
        regions.addAll(when (provider) {
            CdnProvider.AZURE -> AzureRegions.regions
            CdnProvider.AWS -> AwsRegions.regions
            CdnProvider.ALIBABA -> AlibabaRegions.regions
            CdnProvider.VULTR -> VultrRegions.regions
            CdnProvider.LINODE -> LinodeRegions.regions
            CdnProvider.LIGHTSAIL -> LightsailRegions.regions
            CdnProvider.DIGITALOCEAN -> DigitalOceanRegions.regions
            CdnProvider.ORACLE -> OracleRegions.regions
            CdnProvider.HUAWEI -> HuaweiRegions.regions
            CdnProvider.TENCENT -> TencentRegions.regions
            CdnProvider.GCP -> GoogleCloudRegions.regions
            CdnProvider.OVH -> OvhRegions.regions
            CdnProvider.HETZNER -> HetznerRegions.regions
            CdnProvider.FASTLY -> FastlyRegions.regions
        })
    }

    // Test latency for a single region (measures TCP connection time only)
    suspend fun testRegionLatency(region: CdnRegion): CdnRegion = withContext(Dispatchers.IO) {
        var socket: Socket? = null
        try {
            val url = URL(region.url)
            val host = url.host
            val port = if (url.port != -1) url.port else if (url.protocol == "https") 443 else 80

            // Resolve DNS first (outside timing)
            val address = InetAddress.getByName(host)
            val socketAddress = InetSocketAddress(address, port)

            // Measure only TCP connection time (multiple attempts for accuracy)
            val measurements = mutableListOf<Long>()
            repeat(3) {
                socket = Socket()
                socket?.soTimeout = 5000
                val time = measureTimeMillis {
                    socket?.connect(socketAddress, 5000)
                }
                measurements.add(time)
                socket?.close()
            }

            // Use the minimum latency (most accurate, removes outliers)
            val latency = measurements.minOrNull() ?: measurements.average().toLong()

            region.copy(latency = latency, status = TestStatus.SUCCESS)
        } catch (e: Exception) {
            socket?.close()
            region.copy(latency = null, status = TestStatus.FAILED)
        }
    }

    // Test all regions in batches
    fun testAllRegions() {
        if (isTesting) return

        isTesting = true
        testProgress = 0
        scope.launch {
            val totalRegions = regions.size

            // Test in batches of 5 for better reliability
            val batchSize = 5
            var testedCount = 0

            for (i in regions.indices step batchSize) {
                val endIdx = minOf(i + batchSize, regions.size)

                // Set batch to testing
                for (idx in i until endIdx) {
                    regions[idx] = regions[idx].copy(status = TestStatus.TESTING)
                }

                // Test batch in parallel
                val testedBatch = (i until endIdx).map { idx ->
                    async(Dispatchers.IO) {
                        idx to testRegionLatency(regions[idx])
                    }
                }.awaitAll()

                // Update results
                testedBatch.forEach { (idx, result) ->
                    regions[idx] = result
                }

                testedCount += (endIdx - i)
                testProgress = (testedCount * 100) / totalRegions
            }

            isTesting = false
            testProgress = 0
        }
    }

    // Toggle region for continuous monitoring
    fun toggleMonitoredRegion(regionId: String) {
        if (monitoredRegions.contains(regionId)) {
            monitoredRegions.remove(regionId)
            pingHistories.remove(regionId)
        } else {
            // No limit - allow all regions
            monitoredRegions.add(regionId)
            pingHistories[regionId] = PingHistory(regionId)
        }
    }

    // Add all regions for monitoring
    fun addAllRegionsToMonitor() {
        regions.forEach { region ->
            if (!monitoredRegions.contains(region.id)) {
                monitoredRegions.add(region.id)
                pingHistories[region.id] = PingHistory(region.id)
            }
        }
    }

    // Remove all regions from monitoring
    fun removeAllRegionsFromMonitor() {
        monitoredRegions.clear()
        pingHistories.clear()
    }

    // Start continuous pinging
    fun startContinuousPing() {
        if (monitoredRegions.isEmpty()) return
        isContinuousPinging = true

        continuousPingJob = scope.launch(Dispatchers.IO) {
            while (isActive && isContinuousPinging) {
                // Ping all monitored regions in parallel
                val currentMonitoredRegions = monitoredRegions.toList() // Snapshot
                val pingJobs = currentMonitoredRegions.map { regionId ->
                    async {
                        val region = regions.find { it.id == regionId } ?: return@async null
                        val result = testRegionLatency(region)
                        regionId to result
                    }
                }

                // Collect results and update on main thread
                val results = pingJobs.mapNotNull { it.await() }

                withContext(Dispatchers.Main) {
                    results.forEach { (regionId, result) ->
                        val history = pingHistories[regionId] ?: PingHistory(regionId)
                        val latencyValue = result.latency

                        val updatedHistory = if (result.status == TestStatus.SUCCESS && latencyValue != null) {
                            history.addLatency(latencyValue, System.currentTimeMillis(), maxDataPoints)
                        } else {
                            history.addFailure()
                        }

                        pingHistories[regionId] = updatedHistory
                    }
                    pingUpdateTrigger++ // Force UI update
                }

                delay(selectedPingInterval.ms)
            }
        }
    }

    // Stop continuous pinging
    fun stopContinuousPing() {
        isContinuousPinging = false
        continuousPingJob?.cancel()
        continuousPingJob = null
    }

    // Clear ping histories
    fun clearPingHistories() {
        val regionIds = pingHistories.keys.toList()
        regionIds.forEach { regionId ->
            pingHistories[regionId] = PingHistory(regionId)
        }
    }

    // Cleanup on dispose
    DisposableEffect(Unit) {
        onDispose {
            continuousPingJob?.cancel()
        }
    }

    // Sort regions - use derivedStateOf to properly observe mutableStateListOf changes
    val sortedRegions by remember {
        derivedStateOf {
            val filtered = regions.filter {
                it.name.contains(filterQuery, ignoreCase = true) ||
                it.location.contains(filterQuery, ignoreCase = true) ||
                it.geography.contains(filterQuery, ignoreCase = true)
            }

            when (sortBy) {
                "latency" -> filtered.sortedBy { it.latency ?: Long.MAX_VALUE }
                "name" -> filtered.sortedBy { it.name }
                "location" -> filtered.sortedBy { it.location }
                else -> filtered
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cloud & CDN Latency") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showSortMenu = true }) {
                        @Suppress("DEPRECATION")
                        Icon(Icons.Filled.Sort, "Sort")
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Sort by Latency") },
                            onClick = {
                                sortBy = "latency"
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Sort by Name") },
                            onClick = {
                                sortBy = "name"
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Sort by Location") },
                            onClick = {
                                sortBy = "location"
                                showSortMenu = false
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Your Connected CDN Section
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = "Your CDN Edge Location",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(IntrinsicSize.Max),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Cloudflare Card
                        cloudflareCdn?.let { cdn ->
                            UserCdnCard(cdn, modifier = Modifier.weight(1f).fillMaxHeight())
                        }

                        // CloudFront Card
                        cloudfrontCdn?.let { cdn ->
                            UserCdnCard(cdn, modifier = Modifier.weight(1f).fillMaxHeight())
                        }

                        // Alibaba Card
                        alibabaCdn?.let { cdn ->
                            UserCdnCard(cdn, modifier = Modifier.weight(1f).fillMaxHeight())
                        }
                    }
                }
            }
            
            item {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }

            // Provider selector - Row 1
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 16.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProviderChip(
                        provider = CdnProvider.AZURE,
                        label = "Azure (${AzureRegions.regions.size})",
                        isSelected = selectedProvider == CdnProvider.AZURE,
                        onClick = { switchProvider(CdnProvider.AZURE) },
                        modifier = Modifier.weight(1f)
                    )
                    ProviderChip(
                        provider = CdnProvider.AWS,
                        label = "AWS (${AwsRegions.regions.size})",
                        isSelected = selectedProvider == CdnProvider.AWS,
                        onClick = { switchProvider(CdnProvider.AWS) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Provider selector - Row 2
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProviderChip(
                        provider = CdnProvider.GCP,
                        label = "Google Cloud (${GoogleCloudRegions.regions.size})",
                        isSelected = selectedProvider == CdnProvider.GCP,
                        onClick = { switchProvider(CdnProvider.GCP) },
                        modifier = Modifier.weight(1f)
                    )
                    ProviderChip(
                        provider = CdnProvider.ALIBABA,
                        label = "Alibaba (${AlibabaRegions.regions.size})",
                        isSelected = selectedProvider == CdnProvider.ALIBABA,
                        onClick = { switchProvider(CdnProvider.ALIBABA) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Provider selector - Row 3
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProviderChip(
                        provider = CdnProvider.ORACLE,
                        label = "Oracle (${OracleRegions.regions.size})",
                        isSelected = selectedProvider == CdnProvider.ORACLE,
                        onClick = { switchProvider(CdnProvider.ORACLE) },
                        modifier = Modifier.weight(1f)
                    )
                    ProviderChip(
                        provider = CdnProvider.DIGITALOCEAN,
                        label = "DigitalOcean (${DigitalOceanRegions.regions.size})",
                        isSelected = selectedProvider == CdnProvider.DIGITALOCEAN,
                        onClick = { switchProvider(CdnProvider.DIGITALOCEAN) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Provider selector - Row 4
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProviderChip(
                        provider = CdnProvider.VULTR,
                        label = "Vultr (${VultrRegions.regions.size})",
                        isSelected = selectedProvider == CdnProvider.VULTR,
                        onClick = { switchProvider(CdnProvider.VULTR) },
                        modifier = Modifier.weight(1f)
                    )
                    ProviderChip(
                        provider = CdnProvider.LINODE,
                        label = "Linode (${LinodeRegions.regions.size})",
                        isSelected = selectedProvider == CdnProvider.LINODE,
                        onClick = { switchProvider(CdnProvider.LINODE) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Provider selector - Row 5
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProviderChip(
                        provider = CdnProvider.LIGHTSAIL,
                        label = "Lightsail (${LightsailRegions.regions.size})",
                        isSelected = selectedProvider == CdnProvider.LIGHTSAIL,
                        onClick = { switchProvider(CdnProvider.LIGHTSAIL) },
                        modifier = Modifier.weight(1f)
                    )
                    ProviderChip(
                        provider = CdnProvider.TENCENT,
                        label = "Tencent (${TencentRegions.regions.size})",
                        isSelected = selectedProvider == CdnProvider.TENCENT,
                        onClick = { switchProvider(CdnProvider.TENCENT) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Provider selector - Row 6
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProviderChip(
                        provider = CdnProvider.HUAWEI,
                        label = "Huawei (${HuaweiRegions.regions.size})",
                        isSelected = selectedProvider == CdnProvider.HUAWEI,
                        onClick = { switchProvider(CdnProvider.HUAWEI) },
                        modifier = Modifier.weight(1f)
                    )
                    ProviderChip(
                        provider = CdnProvider.OVH,
                        label = "OVH (${OvhRegions.regions.size})",
                        isSelected = selectedProvider == CdnProvider.OVH,
                        onClick = { switchProvider(CdnProvider.OVH) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Provider selector - Row 7
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProviderChip(
                        provider = CdnProvider.HETZNER,
                        label = "Hetzner (${HetznerRegions.regions.size})",
                        isSelected = selectedProvider == CdnProvider.HETZNER,
                        onClick = { switchProvider(CdnProvider.HETZNER) },
                        modifier = Modifier.weight(1f)
                    )
                    ProviderChip(
                        provider = CdnProvider.FASTLY,
                        label = "Fastly (${FastlyRegions.regions.size})",
                        isSelected = selectedProvider == CdnProvider.FASTLY,
                        onClick = { switchProvider(CdnProvider.FASTLY) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Search field
            item {
                OutlinedTextField(
                value = filterQuery,
                onValueChange = { filterQuery = it },
                label = { Text("Search regions") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                trailingIcon = {
                    if (filterQuery.isNotEmpty()) {
                        IconButton(onClick = { filterQuery = "" }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
                singleLine = true
                )
            }

            // Test button
            item {
                Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 8.dp)
            ) {
                Button(
                    onClick = { testAllRegions() },
                    enabled = !isTesting,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        if (isTesting) Icons.Filled.HourglassEmpty else Icons.Filled.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isTesting) "Testing... $testProgress%" else "Test All ${regions.size} Regions",
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Progress bar
                if (isTesting) {
                    LinearProgressIndicator(
                        progress = { testProgress / 100f },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp)
                    )
                }
                }
            }
            
            // Info banner
            if (!isTesting && regions.none { it.status == TestStatus.SUCCESS }) {
                item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Filled.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Tap 'Test All Regions' to measure latency to all ${regions.size} locations",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
                }
            }

            // Statistics
            if (regions.any { it.status == TestStatus.SUCCESS }) {
                item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Test Results",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            val successfulTests = regions.filter { it.status == TestStatus.SUCCESS }
                            val failedTests = regions.filter { it.status == TestStatus.FAILED }
                            val avgLatency = if (successfulTests.isNotEmpty()) {
                                successfulTests.mapNotNull { it.latency }.average()
                            } else 0.0
                            val minLatency = successfulTests.mapNotNull { it.latency }.minOrNull()
                            val maxLatency = successfulTests.mapNotNull { it.latency }.maxOrNull()

                            CdnStatItem("Success", "${successfulTests.size}")
                            CdnStatItem("Failed", "${failedTests.size}")
                            CdnStatItem("Best", "${minLatency ?: 0}ms")
                            CdnStatItem("Avg", "${avgLatency.toInt()}ms")
                            CdnStatItem("Worst", "${maxLatency ?: 0}ms")
                        }
                    }
                }
                }
            }

            // Continuous Ping Section
            item {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Continuous Monitoring",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Interval selector
                            Box {
                                TextButton(
                                    onClick = { showIntervalMenu = true },
                                    enabled = !isContinuousPinging
                                ) {
                                    Icon(Icons.Filled.Timer, null, modifier = Modifier.size(16.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(selectedPingInterval.label, fontSize = 12.sp)
                                }
                                DropdownMenu(
                                    expanded = showIntervalMenu,
                                    onDismissRequest = { showIntervalMenu = false }
                                ) {
                                    CdnPingInterval.entries.forEach { interval ->
                                        DropdownMenuItem(
                                            text = { Text(interval.label) },
                                            onClick = {
                                                selectedPingInterval = interval
                                                showIntervalMenu = false
                                            },
                                            leadingIcon = {
                                                if (selectedPingInterval == interval) {
                                                    Icon(Icons.Filled.Check, null, modifier = Modifier.size(16.dp))
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                            // Clear button
                            IconButton(
                                onClick = { clearPingHistories() },
                                enabled = !isContinuousPinging && monitoredRegions.isNotEmpty()
                            ) {
                                Icon(Icons.Filled.ClearAll, "Clear", modifier = Modifier.size(20.dp))
                            }
                        }
                    }

                    // Select All / Deselect All buttons
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { addAllRegionsToMonitor() },
                            enabled = !isContinuousPinging && monitoredRegions.size < regions.size,
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Filled.SelectAll, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Select All", fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = { removeAllRegionsFromMonitor() },
                            enabled = !isContinuousPinging && monitoredRegions.isNotEmpty(),
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Icon(Icons.Filled.Deselect, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Deselect All", fontSize = 12.sp)
                        }
                    }

                    Text(
                        text = "Tap regions below to add/remove from monitoring (${monitoredRegions.size}/${regions.size} selected)",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    // Start/Stop button
                    Button(
                        onClick = { if (isContinuousPinging) stopContinuousPing() else startContinuousPing() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = monitoredRegions.isNotEmpty() && !isTesting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isContinuousPinging) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
                        )
                    ) {
                        Icon(
                            if (isContinuousPinging) Icons.Filled.Stop else Icons.Filled.Timeline,
                            null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (isContinuousPinging) "Stop Monitoring" else "Start Continuous Ping (${monitoredRegions.size} regions)",
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Real-time Graph with Legend - observe pingUpdateTrigger for updates
            val hasData = pingUpdateTrigger >= 0 && pingHistories.values.any { it.latencies.isNotEmpty() }
            if (monitoredRegions.isNotEmpty() && hasData) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(top = 8.dp)
                    ) {
                        // Title
                        Text(
                            text = "${selectedProvider.name} Latency Test Results",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )

                        // Graph Card - use pingUpdateTrigger to force recomposition
                        val graphKey = pingUpdateTrigger
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(280.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                                key(graphKey) {
                                    CdnPingGraph(
                                        monitoredRegions = monitoredRegions.toList(),
                                        pingHistories = pingHistories.toMap(),
                                        regions = regions.toList(),
                                        maxDataPoints = maxDataPoints
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        // Legend - Flow layout with region names and colors
                        CdnGraphLegend(
                            monitoredRegions = monitoredRegions,
                            regions = regions
                        )

                        Spacer(Modifier.height(16.dp))

                        // Per-region real-time statistics
                        Text(
                            text = "Real-time Statistics",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                }

                // Per-region statistics cards - use pingUpdateTrigger in key to force updates
                val regionsWithData = monitoredRegions.filter { pingHistories[it]?.latencies?.isNotEmpty() == true }
                val statsKey = pingUpdateTrigger
                items(
                    count = regionsWithData.size,
                    key = { index ->
                        val regionId = regionsWithData[index]
                        "$regionId-$statsKey"
                    }
                ) { index ->
                    val regionId = regionsWithData[index]
                    val region = regions.find { it.id == regionId }
                    val history = pingHistories[regionId]
                    val colorIndex = monitoredRegions.indexOf(regionId)
                    val color = cdnGraphColors[colorIndex % cdnGraphColors.size]
                    if (region != null && history != null) {
                        CdnRegionStatsCard(
                            region = region,
                            history = history,
                            color = color,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
            }

            // Region count header with provider info
            item {
                Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (sortedRegions.isEmpty()) "No regions found" else "${sortedRegions.size} Regions",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = selectedProvider.name,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
                
                // Provider logo
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(ProviderLogos.getLogoUrl(selectedProvider))
                        .crossfade(true)
                        .allowHardware(false)
                        .build(),
                    contentDescription = selectedProvider.name,
                    modifier = Modifier
                        .height(20.dp)
                        .widthIn(max = 60.dp),
                    contentScale = ContentScale.Fit
                )
                }
            }

            // Regions list
            items(sortedRegions) { region ->
                RegionCard(
                    region = region,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    isMonitored = monitoredRegions.contains(region.id),
                    monitorColor = if (monitoredRegions.contains(region.id)) {
                        cdnGraphColors[monitoredRegions.indexOf(region.id) % cdnGraphColors.size]
                    } else null,
                    onMonitorClick = { toggleMonitoredRegion(region.id) },
                    canAddToMonitor = !isContinuousPinging // No limit, just check if not pinging
                )
            }
            
            // Empty state
            if (sortedRegions.isEmpty() && filterQuery.isNotEmpty()) {
                item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                                .padding(vertical = 32.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Filled.SearchOff,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "No regions found",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Try a different search term",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

            // Banner ad
            item {
                BannerAd()
            }
        }
    }
}

@Composable
fun ProviderChip(
    provider: CdnProvider,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    FilterChip(
        selected = isSelected,
        onClick = onClick,
        label = {
            Text(
                text = label,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(start = 6.dp, end = 2.dp)
            )
        },
        leadingIcon = {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(ProviderLogos.getLogoUrl(provider))
                    .crossfade(true)
                    .allowHardware(false)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .height(18.dp)
                    .widthIn(max = 36.dp),
                contentScale = ContentScale.Fit
            )
        },
        modifier = modifier.height(36.dp)
    )
}

@Composable
fun UserCdnCard(cdnInfo: UserCdnInfo, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = if (cdnInfo.error == null) 
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
            else 
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Provider logo and name
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(ProviderLogos.getCdnLogoUrl(cdnInfo.provider))
                    .crossfade(true)
                    .allowHardware(false)
                    .build(),
                contentDescription = cdnInfo.provider,
                modifier = Modifier
                    .height(24.dp)
                    .widthIn(max = 100.dp)
                    .padding(bottom = 6.dp),
                contentScale = ContentScale.Fit
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (cdnInfo.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp
                )
            } else if (cdnInfo.error != null) {
                Icon(
                    Icons.Filled.Error,
                    contentDescription = "Error",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Failed",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                Text(
                    text = cdnInfo.location,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (cdnInfo.code.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                    ) {
                        Text(
                            text = cdnInfo.code,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CdnStatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun RegionCard(
    region: CdnRegion,
    modifier: Modifier = Modifier,
    isMonitored: Boolean = false,
    monitorColor: Color? = null,
    onMonitorClick: (() -> Unit)? = null,
    canAddToMonitor: Boolean = true
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onMonitorClick != null && (canAddToMonitor || isMonitored)) {
                    Modifier.clickable { onMonitorClick() }
                } else Modifier
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isMonitored -> monitorColor?.copy(alpha = 0.15f) ?: MaterialTheme.colorScheme.surfaceVariant
                region.status == TestStatus.SUCCESS -> MaterialTheme.colorScheme.surfaceVariant
                region.status == TestStatus.FAILED -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                region.status == TestStatus.TESTING -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isMonitored && monitorColor != null) {
            BorderStroke(2.dp, monitorColor)
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                // Monitor indicator or Flag icon
                Box {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data("https://cdn.statically.io/gh/hampusborgos/country-flags/main/svg/${region.icon}.svg")
                            .crossfade(true)
                            .allowHardware(false)
                            .build(),
                        contentDescription = "Flag",
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp)),
                        contentScale = ContentScale.Crop
                    )
                    if (isMonitored && monitorColor != null) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .offset(x = 4.dp, y = (-4).dp)
                                .size(12.dp)
                                .background(monitorColor, CircleShape)
                                .border(1.dp, Color.White, CircleShape)
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = region.name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (region.availabilityZones > 0) {
                            Surface(
                                shape = RoundedCornerShape(4.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Text(
                                    text = "${region.availabilityZones} AZ",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                    Text(
                        text = "${region.location} • ${region.geography}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(ProviderLogos.getLogoUrl(region.provider))
                                .crossfade(true)
                                .allowHardware(false)
                                .build(),
                            contentDescription = region.provider.name,
                            modifier = Modifier
                                .height(12.dp)
                                .widthIn(max = 45.dp),
                            contentScale = ContentScale.Fit
                        )
                        Surface(
                            shape = RoundedCornerShape(3.dp),
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                            border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                        ) {
                            Text(
                                text = region.id,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            // Latency display
            when (region.status) {
                TestStatus.TESTING -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                }
                TestStatus.SUCCESS -> {
                    Text(
                        text = "${region.latency}ms",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = when {
                            (region.latency ?: 0) < 50 -> Color(0xFF4CAF50)
                            (region.latency ?: 0) < 100 -> Color(0xFFFFC107)
                            (region.latency ?: 0) < 200 -> Color(0xFFFF9800)
                            else -> Color(0xFFF44336)
                        }
                    )
                }
                TestStatus.FAILED -> {
                    Column(horizontalAlignment = Alignment.End) {
                        Icon(
                            Icons.Filled.Error,
                            contentDescription = "Failed",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Failed",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                else -> {
                    Column(horizontalAlignment = Alignment.End) {
                        Icon(
                            Icons.Filled.RemoveCircleOutline,
                            contentDescription = "Not tested",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Pending",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CdnPingGraph(
    monitoredRegions: List<String>,
    pingHistories: Map<String, PingHistory>,
    regions: List<CdnRegion>,
    maxDataPoints: Int
) {
    val textColor = MaterialTheme.colorScheme.onSurface
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val textColorInt = android.graphics.Color.argb(
        (textColor.alpha * 255).toInt(),
        (textColor.red * 255).toInt(),
        (textColor.green * 255).toInt(),
        (textColor.blue * 255).toInt()
    )

    // Get time range from all histories
    val allTimestamps = pingHistories.values.flatMap { it.timestamps }
    val minTime = allTimestamps.minOrNull() ?: System.currentTimeMillis()
    val maxTime = allTimestamps.maxOrNull() ?: System.currentTimeMillis()
    val timeRange = kotlin.math.max(maxTime - minTime, 1000L) // At least 1 second range

    Canvas(modifier = Modifier.fillMaxSize()) {
        val width = size.width
        val height = size.height
        val leftPadding = 50f
        val rightPadding = 15f
        val topPadding = 15f
        val bottomPadding = 40f
        val graphWidth = width - leftPadding - rightPadding
        val graphHeight = height - topPadding - bottomPadding

        // Find max latency across all histories for scaling
        val allLatencies = pingHistories.values.flatMap { it.latencies }
        val maxLatency = if (allLatencies.isEmpty()) 100L else kotlin.math.max(allLatencies.maxOrNull() ?: 100L, 50L)
        val yScale = graphHeight / maxLatency.toFloat()

        // Draw horizontal grid lines and Y-axis labels
        val gridLines = 5
        for (i in 0..gridLines) {
            val y = topPadding + (graphHeight * i / gridLines)
            drawLine(
                color = gridColor.copy(alpha = 0.5f),
                start = Offset(leftPadding, y),
                end = Offset(width - rightPadding, y),
                strokeWidth = 1f
            )
            // Draw Y-axis labels
            val labelValue = maxLatency - (maxLatency * i / gridLines)
            drawContext.canvas.nativeCanvas.drawText(
                "$labelValue",
                5f,
                y + 5f,
                android.graphics.Paint().apply {
                    color = textColorInt
                    textSize = 24f
                }
            )
        }

        // Draw vertical grid lines and X-axis time labels
        val timeGridLines = 5
        val timeFormat = java.text.SimpleDateFormat("mm:ss", java.util.Locale.getDefault())
        for (i in 0..timeGridLines) {
            val x = leftPadding + (graphWidth * i / timeGridLines)
            drawLine(
                color = gridColor.copy(alpha = 0.3f),
                start = Offset(x, topPadding),
                end = Offset(x, height - bottomPadding),
                strokeWidth = 1f
            )
            // Draw time labels
            val timeValue = minTime + (timeRange * i / timeGridLines)
            drawContext.canvas.nativeCanvas.drawText(
                timeFormat.format(java.util.Date(timeValue)),
                x - 18f,
                height - 10f,
                android.graphics.Paint().apply {
                    color = textColorInt
                    textSize = 22f
                }
            )
        }

        // Draw each region's line
        monitoredRegions.forEachIndexed { index, regionId ->
            val history = pingHistories[regionId] ?: return@forEachIndexed
            if (history.latencies.isEmpty()) return@forEachIndexed

            val lineColor = cdnGraphColors[index % cdnGraphColors.size]
            val path = Path()
            var firstPoint = true

            history.latencies.forEachIndexed { idx, latency ->
                val timestamp = history.timestamps.getOrNull(idx) ?: return@forEachIndexed
                val xRatio = (timestamp - minTime).toFloat() / timeRange.toFloat()
                val x = leftPadding + xRatio * graphWidth
                val y = topPadding + graphHeight - (latency * yScale)

                if (firstPoint) {
                    path.moveTo(x, y)
                    firstPoint = false
                } else {
                    path.lineTo(x, y)
                }
            }

            // Draw the path (line)
            if (!firstPoint) { // Only draw if we have at least one point
                drawPath(
                    path = path,
                    color = lineColor,
                    style = Stroke(width = 2.5f)
                )
            }
        }

        // Draw axes
        drawLine(
            color = textColor,
            start = Offset(leftPadding, topPadding),
            end = Offset(leftPadding, height - bottomPadding),
            strokeWidth = 1.5f
        )
        drawLine(
            color = textColor,
            start = Offset(leftPadding, height - bottomPadding),
            end = Offset(width - rightPadding, height - bottomPadding),
            strokeWidth = 1.5f
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CdnGraphLegend(
    monitoredRegions: List<String>,
    regions: List<CdnRegion>
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        monitoredRegions.forEachIndexed { index, regionId ->
            val region = regions.find { it.id == regionId }
            val color = cdnGraphColors[index % cdnGraphColors.size]
            if (region != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(vertical = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(color, CircleShape)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = region.name,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
fun CdnRegionStatsCard(
    region: CdnRegion,
    history: PingHistory,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(color, CircleShape)
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        region.name,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        region.location,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // Current latency badge
                Surface(
                    shape = CircleShape,
                    color = if (history.lastLatency != null) {
                        when {
                            history.lastLatency!! < 50 -> Color(0xFF4CAF50)
                            history.lastLatency!! < 100 -> Color(0xFFFFC107)
                            history.lastLatency!! < 200 -> Color(0xFFFF9800)
                            else -> Color(0xFFF44336)
                        }
                    } else Color.Gray
                ) {
                    Text(
                        if (history.lastLatency != null) "${history.lastLatency}ms" else "---",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                CdnMiniStat("Sent", "${history.totalCount}")
                CdnMiniStat("Loss", "${String.format("%.1f", history.packetLoss)}%")
                CdnMiniStat("Avg", "${history.avgLatency ?: 0}ms")
                CdnMiniStat("Min", "${history.minLatency ?: 0}ms")
                CdnMiniStat("Max", "${history.maxLatency ?: 0}ms")
                CdnMiniStat("Jitter", "${history.jitter}ms")
            }
        }
    }
}

@Composable
private fun CdnMiniStat(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 9.sp
        )
    }
}
