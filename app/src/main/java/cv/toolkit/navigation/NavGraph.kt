package cv.toolkit.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import cv.toolkit.screens.*

sealed class Screen(val route: String) {
    object Main : Screen("main")
    object IpLookup : Screen("ip_lookup")
    object NetworkStressTest : Screen("network_stress_test")
    object DrmInfo : Screen("drm_info")
    object DeviceInfo : Screen("device_info")
    object SpeedTest : Screen("speed_test")
    object NetworkScan : Screen("network_scan")
    object DnsLookup : Screen("dns_lookup")
    object PingTest : Screen("ping_test")
    object PortScan : Screen("port_scan")
    object Traceroute : Screen("traceroute")
    object SubnetCalculator : Screen("subnet_calculator")
    object SSLChecker : Screen("ssl_checker")
    object IPCalculator : Screen("ip_calculator")
    object Base64Tool : Screen("base64_tool")
    object UrlEncoder : Screen("url_encoder")
    object BinaryConverter : Screen("binary_converter")
    object HashGenerator : Screen("hash_generator")
    object CaesarCipher : Screen("caesar_cipher")
    object MorseCode : Screen("morse_code")
    object HexEncoder : Screen("hex_encoder")
    object AsciiConverter : Screen("ascii_converter")
    object JwtDecoder : Screen("jwt_decoder")
    object PasswordGenerator : Screen("password_generator")
    object WorldTime : Screen("world_time")
    object WhoisLookup : Screen("whois_lookup")
    object HttpHeaders : Screen("http_headers")
    object UuidGenerator : Screen("uuid_generator")
    object UnixTimestamp : Screen("unix_timestamp")
    object ColorConverter : Screen("color_converter")
    object TextDiff : Screen("text_diff")
    object UnitConverter : Screen("unit_converter")
    object QrGenerator : Screen("qr_generator")
    object BarcodeGenerator : Screen("barcode_generator")
    object TextCounter : Screen("text_counter")
    object Stopwatch : Screen("stopwatch")
    object FileHash : Screen("file_hash")
    object UserAgentParser : Screen("user_agent_parser")
    object CameraInfo : Screen("camera_info")
    object CustomRequest : Screen("custom_request")
    object QrScanner : Screen("qr_scanner")
    object CdnLatencyTest : Screen("cdn_latency_test")
    object ContinuousPing : Screen("continuous_ping")
    object RobotsTxt : Screen("robots_txt")
    object SitemapViewer : Screen("sitemap_viewer")
    object ImageBase64 : Screen("image_base64")
    object Settings : Screen("settings")
    object WifiAnalyzer : Screen("wifi_analyzer")
    object SecurityAudit : Screen("security_audit")
    object LoremIpsum : Screen("lorem_ipsum")
    object ApiTester : Screen("api_tester")
    object MarkdownPreview : Screen("markdown_preview")
    object ColorPalette : Screen("color_palette")
    object SensorDashboard : Screen("sensor_dashboard")
    object TypingTest : Screen("typing_test")
    object SvgViewer : Screen("svg_viewer")
    object PdfViewer : Screen("pdf_viewer")
    object PdfMerge : Screen("pdf_merge")
    object ImageToPdf : Screen("image_to_pdf")
    object CompressPdf : Screen("compress_pdf")
    object CompressImage : Screen("compress_image")
    object TextEditor : Screen("text_editor")
    object MarkdownEditor : Screen("markdown_editor")
    object SlidesToPdf : Screen("slides_to_pdf")
}

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(
        navController = navController,
        startDestination = Screen.Main.route
    ) {
        composable(Screen.Main.route) {
            MainScreen(navController = navController)
        }

        composable(Screen.IpLookup.route) {
            IpLookupScreen(navController = navController)
        }

        composable(Screen.NetworkStressTest.route) {
            NetworkStressTestScreen(navController = navController)
        }

        composable(Screen.DrmInfo.route) {
            DrmInfoScreen(navController = navController)
        }

        composable(Screen.DeviceInfo.route) {
            DeviceInfoScreen(navController = navController)
        }

        composable(Screen.SpeedTest.route) {
            SpeedTestScreen(navController = navController)
        }

        composable(Screen.NetworkScan.route) {
            NetworkScanScreen(navController = navController)
        }

        composable(Screen.DnsLookup.route) {
            DnsLookupScreen(navController = navController)
        }

        composable(Screen.PingTest.route) {
            PingTestScreen(navController = navController)
        }

        composable(Screen.PortScan.route) {
            PortScanScreen(navController = navController)
        }

        composable(Screen.Traceroute.route) {
            TracerouteScreen(navController = navController)
        }

        composable(Screen.SubnetCalculator.route) {
            SubnetCalculatorScreen(navController = navController)
        }

        composable(Screen.SSLChecker.route) {
            SSLCheckerScreen(navController = navController)
        }

        composable(Screen.IPCalculator.route) {
            IPCalculatorScreen(navController = navController)
        }

        composable(Screen.Base64Tool.route) {
            Base64Screen(navController = navController)
        }

        composable(Screen.UrlEncoder.route) {
            UrlEncoderScreen(navController = navController)
        }

        composable(Screen.BinaryConverter.route) {
            BinaryConverterScreen(navController = navController)
        }

        composable(Screen.HashGenerator.route) {
            HashGeneratorScreen(navController = navController)
        }

        composable(Screen.CaesarCipher.route) {
            CaesarCipherScreen(navController = navController)
        }

        composable(Screen.MorseCode.route) {
            MorseCodeScreen(navController = navController)
        }

        composable(Screen.HexEncoder.route) {
            HexEncoderScreen(navController = navController)
        }

        composable(Screen.AsciiConverter.route) {
            AsciiConverterScreen(navController = navController)
        }

        composable(Screen.JwtDecoder.route) {
            JwtDecoderScreen(navController = navController)
        }

        composable(Screen.PasswordGenerator.route) {
            PasswordGeneratorScreen(navController = navController)
        }

        composable(Screen.WorldTime.route) {
            WorldTimeScreen(navController = navController)
        }

        composable(Screen.WhoisLookup.route) {
            WhoisLookupScreen(navController = navController)
        }

        composable(Screen.HttpHeaders.route) {
            HttpHeadersScreen(navController = navController)
        }

        composable(Screen.UuidGenerator.route) {
            UuidGeneratorScreen(navController = navController)
        }

        composable(Screen.UnixTimestamp.route) {
            UnixTimestampScreen(navController = navController)
        }

        composable(Screen.ColorConverter.route) {
            ColorConverterScreen(navController = navController)
        }

        composable(Screen.TextDiff.route) {
            TextDiffScreen(navController = navController)
        }

        composable(Screen.UnitConverter.route) {
            UnitConverterScreen(navController = navController)
        }

        composable(Screen.QrGenerator.route) {
            QrGeneratorScreen(navController = navController)
        }

        composable(Screen.BarcodeGenerator.route) {
            BarcodeGeneratorScreen(navController = navController)
        }

        composable(Screen.TextCounter.route) {
            TextCounterScreen(navController = navController)
        }

        composable(Screen.Stopwatch.route) {
            StopwatchScreen(navController = navController)
        }

        composable(Screen.FileHash.route) {
            FileHashScreen(navController = navController)
        }

        composable(Screen.UserAgentParser.route) {
            UserAgentParserScreen(navController = navController)
        }

        composable(Screen.CameraInfo.route) {
            CameraInfoScreen(navController = navController)
        }

        composable(Screen.CustomRequest.route) {
            CustomRequestScreen(navController = navController)
        }

        composable(Screen.QrScanner.route) {
            QrScannerScreen(navController = navController)
        }

        composable(Screen.CdnLatencyTest.route) {
            CdnLatencyTestScreen(navController = navController)
        }

        composable(Screen.ContinuousPing.route) {
            ContinuousPingScreen(navController = navController)
        }

        composable(Screen.RobotsTxt.route) {
            RobotsTxtScreen(navController = navController)
        }

        composable(Screen.SitemapViewer.route) {
            SitemapViewerScreen(navController = navController)
        }

        composable(Screen.ImageBase64.route) {
            ImageBase64Screen(navController = navController)
        }

        composable(Screen.Settings.route) {
            SettingsScreen(navController = navController)
        }

        composable(Screen.WifiAnalyzer.route) {
            WifiAnalyzerScreen(navController = navController)
        }

        composable(Screen.SecurityAudit.route) {
            SecurityAuditScreen(navController = navController)
        }

        composable(Screen.LoremIpsum.route) {
            LoremIpsumScreen(navController = navController)
        }

        composable(Screen.ApiTester.route) {
            ApiTesterScreen(navController = navController)
        }

        composable(Screen.MarkdownPreview.route) {
            MarkdownPreviewScreen(navController = navController)
        }

        composable(Screen.ColorPalette.route) {
            ColorPaletteScreen(navController = navController)
        }

        composable(Screen.SensorDashboard.route) {
            SensorDashboardScreen(navController = navController)
        }

        composable(Screen.TypingTest.route) {
            TypingTestScreen(navController = navController)
        }

        composable(Screen.SvgViewer.route) {
            SvgViewerScreen(navController = navController)
        }

        composable(Screen.PdfViewer.route) {
            PdfViewerScreen(navController = navController)
        }

        composable(Screen.PdfMerge.route) {
            PdfMergeScreen(navController = navController)
        }

        composable(Screen.ImageToPdf.route) {
            ImageToPdfScreen(navController = navController)
        }

        composable(Screen.CompressPdf.route) {
            CompressPdfScreen(navController = navController)
        }

        composable(Screen.CompressImage.route) {
            CompressImageScreen(navController = navController)
        }

        composable(Screen.TextEditor.route) {
            TextEditorScreen(navController = navController)
        }

        composable(Screen.MarkdownEditor.route) {
            MarkdownEditorScreen(navController = navController)
        }

        composable(Screen.SlidesToPdf.route) {
            SlidesToPdfScreen(navController = navController)
        }
    }
}
