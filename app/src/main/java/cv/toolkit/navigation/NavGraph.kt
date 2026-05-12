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
    object PasswordStrengthChecker : Screen("password_strength_checker")
    object DnsBenchmark : Screen("dns_benchmark")
    object JsonFormatter : Screen("json_formatter")
    object ExifViewer : Screen("exif_viewer")
    object ExifRemover : Screen("exif_remover")
    object PdfToImage : Screen("pdf_to_image")
    object PdfSplit : Screen("pdf_split")
    object PdfPassword : Screen("pdf_password")
    object ImageResizer : Screen("image_resizer")
    object ImageFormatConverter : Screen("image_format_converter")
    object ImageCropper : Screen("image_cropper")
    object ColorPicker : Screen("color_picker")
    object FaviconGenerator : Screen("favicon_generator")
    object BatteryInfo : Screen("battery_info")
    object DisplayInfo : Screen("display_info")
    object StorageAnalyzer : Screen("storage_analyzer")
    object BluetoothScanner : Screen("bluetooth_scanner")
    object GpsLocation : Screen("gps_location")
    object GifMaker : Screen("gif_maker")
    object PhotoCollage : Screen("photo_collage")
    object ImageWatermark : Screen("image_watermark")
    object NfcReader : Screen("nfc_reader")
    object PomodoroTimer : Screen("pomodoro_timer")
    object RandomGenerator : Screen("random_generator")
    object NotePad : Screen("note_pad")
    object TextCaseConverter : Screen("text_case_converter")
    object ImageBackgroundRemover : Screen("image_background_remover")
    object ImageFilters : Screen("image_filters")
    object MemeGenerator : Screen("meme_generator")
    object ScreenshotStitcher : Screen("screenshot_stitcher")
    object ImageRotateFlip : Screen("image_rotate_flip")
    object CurrencyConverter : Screen("currency_converter")
    object DateCalculator : Screen("date_calculator")
    object AspectRatioCalculator : Screen("aspect_ratio_calculator")
    object NumberBaseConverter : Screen("number_base_converter")
    object ScientificCalculator : Screen("scientific_calculator")
    object Weather : Screen("weather")
    object BirthdayExplorer : Screen("birthday_explorer")
    object Immigration : Screen("immigration")
    object DataCatalogue : Screen("data_catalogue")
    object FuelPrice : Screen("fuel_price")
    object Population : Screen("population")
    object ExchangeRate : Screen("exchange_rate")
    object CpiInflation : Screen("cpi_inflation")
    object Covid : Screen("covid")
    object Crime : Screen("crime")
    object TransportRidership : Screen("transport_ridership")
    object BirthsDeaths : Screen("births_deaths")
    object Marriage : Screen("marriage")
    object HouseholdIncome : Screen("household_income")
    object Gdp : Screen("gdp")
    object LabourMarket : Screen("labour_market")
    object BloodDonation : Screen("blood_donation")
    object InterestRate : Screen("interest_rate")
    object Electricity : Screen("electricity")
    object EpfDividend : Screen("epf_dividend")
    object VehicleRegistration : Screen("vehicle_registration")
    object TransitRealtime : Screen("transit_realtime")
    object SgWeather : Screen("sg_weather")
    object SgAirQuality : Screen("sg_air_quality")
    object SgTaxi : Screen("sg_taxi")
    object SgEnvironment : Screen("sg_environment")
    object SgFloodAlert : Screen("sg_flood_alert")
    object SgDataCatalogue : Screen("sg_data_catalogue")
    object SgCarpark : Screen("sg_carpark")
    object SgTraffic : Screen("sg_traffic")
    object SgWbgt : Screen("sg_wbgt")
    object SgLightning : Screen("sg_lightning")
    object SgDesignPatent : Screen("sg_design_patent")
    object SgDatasetViewer : Screen("sg_dataset_viewer/{datasetId}/{datasetName}") {
        fun createRoute(datasetId: String, datasetName: String): String {
            val encoded = java.net.URLEncoder.encode(datasetName, "UTF-8")
            return "sg_dataset_viewer/$datasetId/$encoded"
        }
    }
    object IeDataCatalogue : Screen("ie_data_catalogue")
    object IeDatasetViewer : Screen("ie_dataset_viewer/{resourceId}/{datasetName}") {
        fun createRoute(resourceId: String, datasetName: String): String {
            val encoded = java.net.URLEncoder.encode(datasetName, "UTF-8")
            return "ie_dataset_viewer/$resourceId/$encoded"
        }
    }
    object SwkDataCatalogue : Screen("swk_data_catalogue")
    object SwkDatasetViewer : Screen("swk_dataset_viewer/{resourceId}/{datasetName}") {
        fun createRoute(resourceId: String, datasetName: String): String {
            val encoded = java.net.URLEncoder.encode(datasetName, "UTF-8")
            return "swk_dataset_viewer/$resourceId/$encoded"
        }
    }
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

        composable(Screen.PasswordStrengthChecker.route) {
            PasswordStrengthCheckerScreen(navController = navController)
        }

        composable(Screen.DnsBenchmark.route) {
            DnsBenchmarkScreen(navController = navController)
        }

        composable(Screen.JsonFormatter.route) {
            JsonFormatterScreen(navController = navController)
        }

        composable(Screen.ExifViewer.route) {
            ExifViewerScreen(navController = navController)
        }

        composable(Screen.ExifRemover.route) {
            ExifRemoverScreen(navController = navController)
        }

        composable(Screen.PdfToImage.route) {
            PdfToImageScreen(navController = navController)
        }

        composable(Screen.PdfSplit.route) {
            PdfSplitScreen(navController = navController)
        }

        composable(Screen.PdfPassword.route) {
            PdfPasswordScreen(navController = navController)
        }

        composable(Screen.ImageResizer.route) {
            ImageResizerScreen(navController = navController)
        }

        composable(Screen.ImageFormatConverter.route) {
            ImageFormatConverterScreen(navController = navController)
        }

        composable(Screen.ImageCropper.route) {
            ImageCropperScreen(navController = navController)
        }

        composable(Screen.ColorPicker.route) {
            ColorPickerScreen(navController = navController)
        }

        composable(Screen.FaviconGenerator.route) {
            FaviconGeneratorScreen(navController = navController)
        }

        composable(Screen.BatteryInfo.route) {
            BatteryInfoScreen(navController = navController)
        }

        composable(Screen.DisplayInfo.route) {
            DisplayInfoScreen(navController = navController)
        }

        composable(Screen.StorageAnalyzer.route) {
            StorageAnalyzerScreen(navController = navController)
        }

        composable(Screen.BluetoothScanner.route) {
            BluetoothScannerScreen(navController = navController)
        }

        composable(Screen.GpsLocation.route) {
            GpsLocationScreen(navController = navController)
        }

        composable(Screen.GifMaker.route) {
            GifMakerScreen(navController = navController)
        }

        composable(Screen.PhotoCollage.route) {
            PhotoCollageScreen(navController = navController)
        }

        composable(Screen.ImageWatermark.route) {
            ImageWatermarkScreen(navController = navController)
        }

        composable(Screen.NfcReader.route) {
            NfcReaderScreen(navController = navController)
        }

        composable(Screen.PomodoroTimer.route) {
            PomodoroTimerScreen(navController = navController)
        }

        composable(Screen.RandomGenerator.route) {
            RandomGeneratorScreen(navController = navController)
        }

        composable(Screen.NotePad.route) {
            NotePadScreen(navController = navController)
        }

        composable(Screen.TextCaseConverter.route) {
            TextCaseConverterScreen(navController = navController)
        }

        composable(Screen.ImageBackgroundRemover.route) {
            ImageBackgroundRemoverScreen(navController = navController)
        }

        composable(Screen.ImageFilters.route) {
            ImageFiltersScreen(navController = navController)
        }

        composable(Screen.MemeGenerator.route) {
            MemeGeneratorScreen(navController = navController)
        }

        composable(Screen.ScreenshotStitcher.route) {
            ScreenshotStitcherScreen(navController = navController)
        }

        composable(Screen.ImageRotateFlip.route) {
            ImageRotateFlipScreen(navController = navController)
        }

        composable(Screen.CurrencyConverter.route) {
            CurrencyConverterScreen(navController = navController)
        }

        composable(Screen.DateCalculator.route) {
            DateCalculatorScreen(navController = navController)
        }

        composable(Screen.AspectRatioCalculator.route) {
            AspectRatioCalculatorScreen(navController = navController)
        }

        composable(Screen.NumberBaseConverter.route) {
            NumberBaseConverterScreen(navController = navController)
        }

        composable(Screen.ScientificCalculator.route) {
            ScientificCalculatorScreen(navController = navController)
        }

        composable(Screen.Weather.route) {
            WeatherScreen(navController = navController)
        }

        composable(Screen.BirthdayExplorer.route) {
            BirthdayExplorerScreen(navController = navController)
        }

        composable(Screen.Immigration.route) {
            ImmigrationScreen(navController = navController)
        }

        composable(Screen.DataCatalogue.route) {
            DataCatalogueScreen(navController = navController)
        }

        composable(Screen.FuelPrice.route) {
            FuelPriceScreen(navController = navController)
        }

        composable(Screen.Population.route) {
            PopulationScreen(navController = navController)
        }

        composable(Screen.ExchangeRate.route) {
            ExchangeRateScreen(navController = navController)
        }

        composable(Screen.CpiInflation.route) {
            CpiInflationScreen(navController = navController)
        }

        composable(Screen.Covid.route) {
            CovidScreen(navController = navController)
        }

        composable(Screen.Crime.route) {
            CrimeScreen(navController = navController)
        }

        composable(Screen.TransportRidership.route) {
            TransportRidershipScreen(navController = navController)
        }

        composable(Screen.BirthsDeaths.route) {
            BirthsDeathsScreen(navController = navController)
        }

        composable(Screen.Marriage.route) {
            MarriageScreen(navController = navController)
        }

        composable(Screen.HouseholdIncome.route) {
            HouseholdIncomeScreen(navController = navController)
        }

        composable(Screen.Gdp.route) {
            GdpScreen(navController = navController)
        }

        composable(Screen.LabourMarket.route) {
            LabourMarketScreen(navController = navController)
        }

        composable(Screen.BloodDonation.route) {
            BloodDonationScreen(navController = navController)
        }

        composable(Screen.InterestRate.route) {
            InterestRateScreen(navController = navController)
        }

        composable(Screen.Electricity.route) {
            ElectricityScreen(navController = navController)
        }

        composable(Screen.EpfDividend.route) {
            EpfDividendScreen(navController = navController)
        }

        composable(Screen.VehicleRegistration.route) {
            VehicleRegistrationScreen(navController = navController)
        }

        composable(Screen.TransitRealtime.route) {
            TransitRealtimeScreen(navController = navController)
        }

        composable(Screen.SgWeather.route) {
            SgWeatherScreen(navController = navController)
        }

        composable(Screen.SgAirQuality.route) {
            SgAirQualityScreen(navController = navController)
        }

        composable(Screen.SgTaxi.route) {
            SgTaxiScreen(navController = navController)
        }

        composable(Screen.SgEnvironment.route) {
            SgEnvironmentScreen(navController = navController)
        }

        composable(Screen.SgFloodAlert.route) {
            SgFloodAlertScreen(navController = navController)
        }

        composable(Screen.SgDataCatalogue.route) {
            SgDataCatalogueScreen(navController = navController)
        }

        composable(Screen.SgCarpark.route) {
            SgCarparkScreen(navController = navController)
        }

        composable(Screen.SgTraffic.route) {
            SgTrafficScreen(navController = navController)
        }

        composable(Screen.SgWbgt.route) {
            SgWbgtScreen(navController = navController)
        }

        composable(Screen.SgLightning.route) {
            SgLightningScreen(navController = navController)
        }

        composable(Screen.SgDesignPatent.route) {
            SgDesignPatentScreen(navController = navController)
        }

        composable(Screen.IeDataCatalogue.route) {
            IeDataCatalogueScreen(navController = navController)
        }

        composable(Screen.SwkDataCatalogue.route) {
            SwkDataCatalogueScreen(navController = navController)
        }

        composable(
            Screen.SwkDatasetViewer.route,
            arguments = listOf(
                androidx.navigation.navArgument("resourceId") { type = androidx.navigation.NavType.StringType },
                androidx.navigation.navArgument("datasetName") { type = androidx.navigation.NavType.StringType }
            )
        ) { backStackEntry ->
            val resourceId = backStackEntry.arguments?.getString("resourceId") ?: ""
            val datasetName = java.net.URLDecoder.decode(
                backStackEntry.arguments?.getString("datasetName") ?: "Dataset", "UTF-8"
            )
            SwkDatasetViewerScreen(
                navController = navController,
                resourceId = resourceId,
                datasetName = datasetName
            )
        }

        composable(
            Screen.IeDatasetViewer.route,
            arguments = listOf(
                androidx.navigation.navArgument("resourceId") { type = androidx.navigation.NavType.StringType },
                androidx.navigation.navArgument("datasetName") { type = androidx.navigation.NavType.StringType }
            )
        ) { backStackEntry ->
            val resourceId = backStackEntry.arguments?.getString("resourceId") ?: ""
            val datasetName = java.net.URLDecoder.decode(
                backStackEntry.arguments?.getString("datasetName") ?: "Dataset", "UTF-8"
            )
            IeDatasetViewerScreen(
                navController = navController,
                resourceId = resourceId,
                datasetName = datasetName
            )
        }

        composable(
            Screen.SgDatasetViewer.route,
            arguments = listOf(
                androidx.navigation.navArgument("datasetId") { type = androidx.navigation.NavType.StringType },
                androidx.navigation.navArgument("datasetName") { type = androidx.navigation.NavType.StringType }
            )
        ) { backStackEntry ->
            val datasetId = backStackEntry.arguments?.getString("datasetId") ?: ""
            val datasetName = java.net.URLDecoder.decode(
                backStackEntry.arguments?.getString("datasetName") ?: "Dataset", "UTF-8"
            )
            SgDatasetViewerScreen(
                navController = navController,
                datasetId = datasetId,
                datasetName = datasetName
            )
        }
    }
}
