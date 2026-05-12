package cv.toolkit.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.AltRoute
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.ManageSearch
import androidx.compose.material.icons.automirrored.filled.MergeType
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import cv.toolkit.R
import cv.toolkit.ads.BannerAd
import cv.toolkit.navigation.Screen
import cv.toolkit.ui.components.FlagSectionHeader
import cv.toolkit.ui.components.MonoCountChip
import cv.toolkit.ui.components.SectionHeader
import cv.toolkit.ui.theme.MonoText

data class ToolItem(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val route: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(navController: NavController) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    var showPrivacyDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedTab by remember { mutableIntStateOf(0) }
    var selectedCountry by remember { mutableIntStateOf(0) } // 0 = Malaysia, 1 = Singapore, 2 = Ireland
    val tabTitles = listOf("All", "Network", "Utility", "Device", "OpenData")
    val tabIcons = listOf(
        Icons.Filled.GridView,
        Icons.Filled.NetworkCheck,
        Icons.Filled.Build,
        Icons.Filled.PhoneAndroid,
        Icons.Filled.Public,
    )

    val networkTools = listOf(
        ToolItem(stringResource(R.string.ping_test_title), stringResource(R.string.ping_test_desc), Icons.Filled.NetworkPing, Screen.PingTest.route),
        ToolItem(stringResource(R.string.continuous_ping_title), stringResource(R.string.continuous_ping_desc), Icons.Filled.Timeline, Screen.ContinuousPing.route),
        ToolItem(stringResource(R.string.traceroute_title), stringResource(R.string.traceroute_desc), Icons.AutoMirrored.Filled.AltRoute, Screen.Traceroute.route),
        ToolItem(stringResource(R.string.port_scan_title), stringResource(R.string.port_scan_desc), Icons.Filled.Router, Screen.PortScan.route),
        ToolItem(stringResource(R.string.network_scan_title), stringResource(R.string.network_scan_desc), Icons.Filled.Radar, Screen.NetworkScan.route),
        ToolItem(stringResource(R.string.ip_lookup_title), stringResource(R.string.ip_lookup_desc), Icons.Filled.LocationOn, Screen.IpLookup.route),
        ToolItem(stringResource(R.string.dns_lookup_title), stringResource(R.string.dns_lookup_desc), Icons.Filled.Dns, Screen.DnsLookup.route),
        ToolItem(stringResource(R.string.dns_benchmark_title), stringResource(R.string.dns_benchmark_desc), Icons.Filled.Leaderboard, Screen.DnsBenchmark.route),
        ToolItem(stringResource(R.string.subnet_calc_title), stringResource(R.string.subnet_calc_desc), Icons.Filled.Calculate, Screen.SubnetCalculator.route),
        ToolItem(stringResource(R.string.ssl_checker_title), stringResource(R.string.ssl_checker_desc), Icons.Filled.Security, Screen.SSLChecker.route),
        ToolItem(stringResource(R.string.whois_lookup_title), stringResource(R.string.whois_lookup_desc), Icons.AutoMirrored.Filled.ManageSearch, Screen.WhoisLookup.route),
        ToolItem(stringResource(R.string.http_headers_title), stringResource(R.string.http_headers_desc), Icons.Filled.Http, Screen.HttpHeaders.route),
        ToolItem(stringResource(R.string.custom_request_title), stringResource(R.string.custom_request_desc), Icons.Filled.Api, Screen.CustomRequest.route),
        ToolItem(stringResource(R.string.speed_test_title), stringResource(R.string.speed_test_desc), Icons.Filled.Speed, Screen.SpeedTest.route),
        ToolItem(stringResource(R.string.cdn_latency_title), stringResource(R.string.cdn_latency_desc), Icons.Filled.Cloud, Screen.CdnLatencyTest.route),
        ToolItem(stringResource(R.string.wifi_analyzer_title), stringResource(R.string.wifi_analyzer_desc), Icons.Filled.Wifi, Screen.WifiAnalyzer.route)
    )

    val encoderTools = listOf(
        ToolItem(stringResource(R.string.json_formatter_title), stringResource(R.string.json_formatter_desc), Icons.Filled.DataObject, Screen.JsonFormatter.route),
        ToolItem(stringResource(R.string.base64_title), stringResource(R.string.base64_desc), Icons.Filled.Code, Screen.Base64Tool.route),
        ToolItem(stringResource(R.string.url_encoder_title), stringResource(R.string.url_encoder_desc), Icons.Filled.Link, Screen.UrlEncoder.route),
        ToolItem(stringResource(R.string.binary_converter_title), stringResource(R.string.binary_converter_desc), Icons.Filled.DataObject, Screen.BinaryConverter.route),
        ToolItem(stringResource(R.string.hex_encoder_title), stringResource(R.string.hex_encoder_desc), Icons.Filled.Memory, Screen.HexEncoder.route),
        ToolItem(stringResource(R.string.ascii_converter_title), stringResource(R.string.ascii_converter_desc), Icons.Filled.TextFields, Screen.AsciiConverter.route),
        ToolItem(stringResource(R.string.jwt_decoder_title), stringResource(R.string.jwt_decoder_desc), Icons.Filled.Token, Screen.JwtDecoder.route),
        ToolItem(stringResource(R.string.image_base64_title), stringResource(R.string.image_base64_desc), Icons.Filled.Image, Screen.ImageBase64.route)
    )

    val securityTools = listOf(
        ToolItem(stringResource(R.string.hash_generator_title), stringResource(R.string.hash_generator_desc), Icons.Filled.Tag, Screen.HashGenerator.route),
        ToolItem(stringResource(R.string.caesar_cipher_title), stringResource(R.string.caesar_cipher_desc), Icons.Filled.Key, Screen.CaesarCipher.route),
        ToolItem(stringResource(R.string.morse_code_title), stringResource(R.string.morse_code_desc), Icons.Filled.GraphicEq, Screen.MorseCode.route),
        ToolItem(stringResource(R.string.password_generator_title), stringResource(R.string.password_generator_desc), Icons.Filled.Password, Screen.PasswordGenerator.route),
        ToolItem(stringResource(R.string.password_strength_checker_title), stringResource(R.string.password_strength_checker_desc), Icons.Filled.Shield, Screen.PasswordStrengthChecker.route),
        ToolItem(stringResource(R.string.file_hash_title), stringResource(R.string.file_hash_desc), Icons.Filled.Fingerprint, Screen.FileHash.route)
    )

    val qrTools = listOf(
        ToolItem(stringResource(R.string.qr_generator_title), stringResource(R.string.qr_generator_desc), Icons.Filled.QrCode2, Screen.QrGenerator.route),
        ToolItem(stringResource(R.string.barcode_generator_title), stringResource(R.string.barcode_generator_desc), Icons.Filled.QrCode, Screen.BarcodeGenerator.route),
        ToolItem(stringResource(R.string.qr_scanner_title), stringResource(R.string.qr_scanner_desc), Icons.Filled.QrCodeScanner, Screen.QrScanner.route)
    )

    val webTools = listOf(
        ToolItem(stringResource(R.string.api_tester_title), stringResource(R.string.api_tester_desc), Icons.Filled.Storage, Screen.ApiTester.route),
        ToolItem(stringResource(R.string.user_agent_parser_title), stringResource(R.string.user_agent_parser_desc), Icons.Filled.Web, Screen.UserAgentParser.route),
        ToolItem(stringResource(R.string.robots_txt_title), stringResource(R.string.robots_txt_desc), Icons.Filled.SmartToy, Screen.RobotsTxt.route),
        ToolItem(stringResource(R.string.sitemap_viewer_title), stringResource(R.string.sitemap_viewer_desc), Icons.Filled.Map, Screen.SitemapViewer.route)
    )

    val pdfTools = listOf(
        ToolItem(stringResource(R.string.pdf_viewer_title), stringResource(R.string.pdf_viewer_desc), Icons.Filled.PictureAsPdf, Screen.PdfViewer.route),
        ToolItem(stringResource(R.string.pdf_merge_title), stringResource(R.string.pdf_merge_desc), Icons.AutoMirrored.Filled.MergeType, Screen.PdfMerge.route),
        ToolItem(stringResource(R.string.pdf_split_title), stringResource(R.string.pdf_split_desc), Icons.Filled.ContentCut, Screen.PdfSplit.route),
        ToolItem(stringResource(R.string.pdf_to_image_title), stringResource(R.string.pdf_to_image_desc), Icons.Filled.BurstMode, Screen.PdfToImage.route),
        ToolItem(stringResource(R.string.image_to_pdf_title), stringResource(R.string.image_to_pdf_desc), Icons.Filled.PhotoLibrary, Screen.ImageToPdf.route),
        ToolItem(stringResource(R.string.compress_pdf_title), stringResource(R.string.compress_pdf_desc), Icons.Filled.Compress, Screen.CompressPdf.route),
        ToolItem(stringResource(R.string.pdf_password_title), stringResource(R.string.pdf_password_desc), Icons.Filled.Lock, Screen.PdfPassword.route),
        ToolItem(stringResource(R.string.slides_to_pdf_title), stringResource(R.string.slides_to_pdf_desc), Icons.Filled.Slideshow, Screen.SlidesToPdf.route)
    )

    val editorTools = listOf(
        ToolItem(stringResource(R.string.text_editor_title), stringResource(R.string.text_editor_desc), Icons.Filled.EditNote, Screen.TextEditor.route),
        ToolItem(stringResource(R.string.markdown_editor_title), stringResource(R.string.markdown_editor_desc), Icons.Filled.Description, Screen.MarkdownEditor.route),
        ToolItem(stringResource(R.string.markdown_preview_title), stringResource(R.string.markdown_preview_desc), Icons.AutoMirrored.Filled.Article, Screen.MarkdownPreview.route),
        ToolItem(stringResource(R.string.svg_viewer_title), stringResource(R.string.svg_viewer_desc), Icons.Filled.Draw, Screen.SvgViewer.route)
    )

    val mediaTools = listOf(
        ToolItem(stringResource(R.string.compress_image_title), stringResource(R.string.compress_image_desc), Icons.Filled.PhotoSizeSelectLarge, Screen.CompressImage.route),
        ToolItem(stringResource(R.string.image_resizer_title), stringResource(R.string.image_resizer_desc), Icons.Filled.AspectRatio, Screen.ImageResizer.route),
        ToolItem(stringResource(R.string.image_cropper_title), stringResource(R.string.image_cropper_desc), Icons.Filled.Crop, Screen.ImageCropper.route),
        ToolItem(stringResource(R.string.image_format_converter_title), stringResource(R.string.image_format_converter_desc), Icons.Filled.SwapHoriz, Screen.ImageFormatConverter.route),
        ToolItem(stringResource(R.string.exif_viewer_title), stringResource(R.string.exif_viewer_desc), Icons.Filled.ImageSearch, Screen.ExifViewer.route),
        ToolItem(stringResource(R.string.exif_remover_title), stringResource(R.string.exif_remover_desc), Icons.Filled.CleaningServices, Screen.ExifRemover.route),
        ToolItem(stringResource(R.string.color_picker_title), stringResource(R.string.color_picker_desc), Icons.Filled.Colorize, Screen.ColorPicker.route),
        ToolItem(stringResource(R.string.color_converter_title), stringResource(R.string.color_converter_desc), Icons.Filled.Palette, Screen.ColorConverter.route),
        ToolItem(stringResource(R.string.color_palette_title), stringResource(R.string.color_palette_desc), Icons.Filled.ColorLens, Screen.ColorPalette.route),
        ToolItem(stringResource(R.string.favicon_generator_title), stringResource(R.string.favicon_generator_desc), Icons.Filled.Web, Screen.FaviconGenerator.route),
        ToolItem(stringResource(R.string.gif_maker_title), stringResource(R.string.gif_maker_desc), Icons.Filled.Gif, Screen.GifMaker.route),
        ToolItem(stringResource(R.string.photo_collage_title), stringResource(R.string.photo_collage_desc), Icons.Filled.GridView, Screen.PhotoCollage.route),
        ToolItem(stringResource(R.string.image_watermark_title), stringResource(R.string.image_watermark_desc), @Suppress("DEPRECATION") Icons.Filled.BrandingWatermark, Screen.ImageWatermark.route),
        ToolItem(stringResource(R.string.image_bg_remover_title), stringResource(R.string.image_bg_remover_desc), Icons.Filled.ContentCut, Screen.ImageBackgroundRemover.route),
        ToolItem(stringResource(R.string.image_filters_title), stringResource(R.string.image_filters_desc), Icons.Filled.FilterVintage, Screen.ImageFilters.route),
        ToolItem(stringResource(R.string.meme_generator_title), stringResource(R.string.meme_generator_desc), Icons.Filled.EmojiEmotions, Screen.MemeGenerator.route),
        ToolItem(stringResource(R.string.screenshot_stitcher_title), stringResource(R.string.screenshot_stitcher_desc), Icons.Filled.ViewDay, Screen.ScreenshotStitcher.route),
        ToolItem(stringResource(R.string.image_rotate_flip_title), stringResource(R.string.image_rotate_flip_desc), @Suppress("DEPRECATION") Icons.Filled.RotateRight, Screen.ImageRotateFlip.route)
    )

    val converterTools = listOf(
        ToolItem(stringResource(R.string.ip_calc_title), stringResource(R.string.ip_calc_desc), Icons.Filled.Transform, Screen.IPCalculator.route),
        ToolItem(stringResource(R.string.unit_converter_title), stringResource(R.string.unit_converter_desc), Icons.Filled.Straighten, Screen.UnitConverter.route),
        ToolItem(stringResource(R.string.world_time_title), stringResource(R.string.world_time_desc), Icons.Filled.Public, Screen.WorldTime.route),
        ToolItem(stringResource(R.string.unix_timestamp_title), stringResource(R.string.unix_timestamp_desc), Icons.Filled.Schedule, Screen.UnixTimestamp.route),
        ToolItem(stringResource(R.string.uuid_generator_title), stringResource(R.string.uuid_generator_desc), Icons.Filled.Fingerprint, Screen.UuidGenerator.route),
        ToolItem(stringResource(R.string.currency_converter_title), stringResource(R.string.currency_converter_desc), Icons.Filled.CurrencyExchange, Screen.CurrencyConverter.route),
        ToolItem(stringResource(R.string.date_calculator_title), stringResource(R.string.date_calculator_desc), Icons.Filled.DateRange, Screen.DateCalculator.route),
        ToolItem(stringResource(R.string.aspect_ratio_calc_title), stringResource(R.string.aspect_ratio_calc_desc), Icons.Filled.AspectRatio, Screen.AspectRatioCalculator.route),
        ToolItem(stringResource(R.string.number_base_converter_title), stringResource(R.string.number_base_converter_desc), Icons.Filled.Tag, Screen.NumberBaseConverter.route),
        ToolItem(stringResource(R.string.scientific_calc_title), stringResource(R.string.scientific_calc_desc), Icons.Filled.Calculate, Screen.ScientificCalculator.route)
    )

    val textTools = listOf(
        ToolItem(stringResource(R.string.text_counter_title), stringResource(R.string.text_counter_desc), Icons.Filled.Numbers, Screen.TextCounter.route),
        ToolItem(stringResource(R.string.text_diff_title), stringResource(R.string.text_diff_desc), Icons.Filled.Compare, Screen.TextDiff.route),
        ToolItem(stringResource(R.string.lorem_ipsum_title), stringResource(R.string.lorem_ipsum_desc), Icons.AutoMirrored.Filled.Notes, Screen.LoremIpsum.route),
        ToolItem(stringResource(R.string.typing_test_title), stringResource(R.string.typing_test_desc), Icons.Filled.Keyboard, Screen.TypingTest.route),
        ToolItem(stringResource(R.string.stopwatch_title), stringResource(R.string.stopwatch_desc), Icons.Filled.Timer, Screen.Stopwatch.route),
        ToolItem(stringResource(R.string.pomodoro_timer_title), stringResource(R.string.pomodoro_timer_desc), Icons.Filled.AvTimer, Screen.PomodoroTimer.route),
        ToolItem(stringResource(R.string.random_generator_title), stringResource(R.string.random_generator_desc), Icons.Filled.Casino, Screen.RandomGenerator.route),
        ToolItem(stringResource(R.string.note_pad_title), stringResource(R.string.note_pad_desc), Icons.Filled.NoteAlt, Screen.NotePad.route),
        ToolItem(stringResource(R.string.text_case_converter_title), stringResource(R.string.text_case_converter_desc), Icons.Filled.TextFormat, Screen.TextCaseConverter.route)
    )

    data class ToolCategory(val label: String, val tools: List<ToolItem>)

    val myDemographyTools = listOf(
        ToolItem(stringResource(R.string.population_title), stringResource(R.string.population_desc), Icons.Filled.People, Screen.Population.route),
        ToolItem(stringResource(R.string.births_deaths_title), stringResource(R.string.births_deaths_desc), Icons.Filled.ChildCare, Screen.BirthsDeaths.route),
        ToolItem(stringResource(R.string.birthday_explorer_title), stringResource(R.string.birthday_explorer_desc), Icons.Filled.Cake, Screen.BirthdayExplorer.route),
        ToolItem(stringResource(R.string.marriage_title), stringResource(R.string.marriage_desc), Icons.Filled.Favorite, Screen.Marriage.route),
        ToolItem(stringResource(R.string.immigration_title), stringResource(R.string.immigration_desc), Icons.Filled.Flight, Screen.Immigration.route),
    )
    val myEconomyTools = listOf(
        ToolItem(stringResource(R.string.gdp_title), stringResource(R.string.gdp_desc), Icons.Filled.StackedLineChart, Screen.Gdp.route),
        ToolItem(stringResource(R.string.fuel_price_title), stringResource(R.string.fuel_price_desc), Icons.Filled.LocalGasStation, Screen.FuelPrice.route),
        ToolItem(stringResource(R.string.cpi_inflation_title), stringResource(R.string.cpi_inflation_desc), Icons.Filled.StackedLineChart, Screen.CpiInflation.route),
        ToolItem(stringResource(R.string.exchange_rate_title), stringResource(R.string.exchange_rate_desc), Icons.Filled.CurrencyExchange, Screen.ExchangeRate.route),
        ToolItem(stringResource(R.string.household_income_title), stringResource(R.string.household_income_desc), Icons.Filled.AccountBalance, Screen.HouseholdIncome.route),
        ToolItem(stringResource(R.string.labour_market_title), stringResource(R.string.labour_market_desc), Icons.Filled.Work, Screen.LabourMarket.route),
        ToolItem(stringResource(R.string.epf_dividend_title), stringResource(R.string.epf_dividend_desc), Icons.Filled.Savings, Screen.EpfDividend.route),
    )
    val myFinanceTools = listOf(
        ToolItem(stringResource(R.string.interest_rate_title), stringResource(R.string.interest_rate_desc), Icons.Filled.Percent, Screen.InterestRate.route),
    )
    val myTransportTools = listOf(
        ToolItem(stringResource(R.string.transit_realtime_title), stringResource(R.string.transit_realtime_desc), Icons.Filled.DirectionsBus, Screen.TransitRealtime.route),
        ToolItem(stringResource(R.string.transport_ridership_title), stringResource(R.string.transport_ridership_desc), Icons.Filled.Train, Screen.TransportRidership.route),
        ToolItem(stringResource(R.string.vehicle_registration_title), stringResource(R.string.vehicle_registration_desc), Icons.Filled.DirectionsCar, Screen.VehicleRegistration.route),
    )
    val myHealthcareTools = listOf(
        ToolItem(stringResource(R.string.covid_title), stringResource(R.string.covid_desc), Icons.Filled.Coronavirus, Screen.Covid.route),
        ToolItem(stringResource(R.string.blood_donation_title), stringResource(R.string.blood_donation_desc), Icons.Filled.Bloodtype, Screen.BloodDonation.route),
    )
    val myEnvironmentTools = listOf(
        ToolItem(stringResource(R.string.electricity_title), stringResource(R.string.electricity_desc), Icons.Filled.ElectricBolt, Screen.Electricity.route),
        ToolItem(stringResource(R.string.weather_title), stringResource(R.string.weather_desc), Icons.Filled.Cloud, Screen.Weather.route),
    )
    val myPublicSafetyTools = listOf(
        ToolItem(stringResource(R.string.crime_title), stringResource(R.string.crime_desc), Icons.Filled.Policy, Screen.Crime.route),
    )
    val myReferenceTools = listOf(
        ToolItem(stringResource(R.string.swk_data_catalogue_title), stringResource(R.string.swk_data_catalogue_desc), Icons.Filled.Dataset, Screen.SwkDataCatalogue.route),
        ToolItem(stringResource(R.string.data_catalogue_title), stringResource(R.string.data_catalogue_desc), Icons.Filled.Dataset, Screen.DataCatalogue.route),
    )

    val malaysiaCategories = listOf(
        ToolCategory("Demography", myDemographyTools),
        ToolCategory("Economy & Finance", myEconomyTools),
        ToolCategory("Financial Markets", myFinanceTools),
        ToolCategory("Transportation", myTransportTools),
        ToolCategory("Healthcare", myHealthcareTools),
        ToolCategory("Environment & Energy", myEnvironmentTools),
        ToolCategory("Public Safety", myPublicSafetyTools),
        ToolCategory("Reference", myReferenceTools),
    )
    val malaysiaDataTools = malaysiaCategories.flatMap { it.tools }

    val sgRealtimeWeatherTools = listOf(
        ToolItem(stringResource(R.string.sg_weather_title), stringResource(R.string.sg_weather_desc), Icons.Filled.Cloud, Screen.SgWeather.route),
        ToolItem(stringResource(R.string.sg_air_quality_title), stringResource(R.string.sg_air_quality_desc), Icons.Filled.Air, Screen.SgAirQuality.route),
        ToolItem(stringResource(R.string.sg_environment_title), stringResource(R.string.sg_environment_desc), Icons.Filled.Thermostat, Screen.SgEnvironment.route),
        ToolItem(stringResource(R.string.sg_wbgt_title), stringResource(R.string.sg_wbgt_desc), Icons.Filled.Whatshot, Screen.SgWbgt.route),
        ToolItem(stringResource(R.string.sg_lightning_title), stringResource(R.string.sg_lightning_desc), Icons.Filled.FlashOn, Screen.SgLightning.route),
        ToolItem(stringResource(R.string.sg_flood_alert_title), stringResource(R.string.sg_flood_alert_desc), Icons.Filled.Flood, Screen.SgFloodAlert.route),
    )
    val sgTransportTools = listOf(
        ToolItem(stringResource(R.string.sg_taxi_title), stringResource(R.string.sg_taxi_desc), Icons.Filled.LocalTaxi, Screen.SgTaxi.route),
        ToolItem(stringResource(R.string.sg_carpark_title), stringResource(R.string.sg_carpark_desc), Icons.Filled.LocalParking, Screen.SgCarpark.route),
        ToolItem(stringResource(R.string.sg_traffic_title), stringResource(R.string.sg_traffic_desc), Icons.Filled.Videocam, Screen.SgTraffic.route),
    )
    val sgIpTools = listOf(
        ToolItem(stringResource(R.string.sg_design_patent_title), stringResource(R.string.sg_design_patent_desc), Icons.Filled.Lightbulb, Screen.SgDesignPatent.route),
    )
    val sgReferenceTools = listOf(
        ToolItem(stringResource(R.string.sg_data_catalogue_title), stringResource(R.string.sg_data_catalogue_desc), Icons.Filled.Dataset, Screen.SgDataCatalogue.route),
    )

    val singaporeCategories = listOf(
        ToolCategory("Real-time Weather & Environment", sgRealtimeWeatherTools),
        ToolCategory("Transport", sgTransportTools),
        ToolCategory("Intellectual Property", sgIpTools),
        ToolCategory("Reference", sgReferenceTools),
    )
    val singaporeDataTools = singaporeCategories.flatMap { it.tools }

    val ieReferenceTools = listOf(
        ToolItem(stringResource(R.string.ie_data_catalogue_title), stringResource(R.string.ie_data_catalogue_desc), Icons.Filled.Dataset, Screen.IeDataCatalogue.route),
    )
    val irelandCategories = listOf(
        ToolCategory("Reference", ieReferenceTools),
    )
    val irelandDataTools = irelandCategories.flatMap { it.tools }

    val utilityCategories = listOf(
        ToolCategory("Encoders & Decoders", encoderTools),
        ToolCategory("Security & Crypto", securityTools),
        ToolCategory("QR & Barcode", qrTools),
        ToolCategory("Web & API", webTools),
        ToolCategory("PDF & Documents", pdfTools),
        ToolCategory("Editors", editorTools),
        ToolCategory("Media & Colors", mediaTools),
        ToolCategory("Converters & Calculators", converterTools),
        ToolCategory("Text & Productivity", textTools)
    )

    val utilityTools = utilityCategories.flatMap { it.tools }

    val deviceTools = listOf(
        ToolItem(stringResource(R.string.drm_info_title), stringResource(R.string.drm_info_desc), Icons.Filled.Lock, Screen.DrmInfo.route),
        ToolItem(stringResource(R.string.device_info_title), stringResource(R.string.device_info_desc), Icons.Filled.Phone, Screen.DeviceInfo.route),
        ToolItem(stringResource(R.string.camera_info_title), stringResource(R.string.camera_info_desc), Icons.Filled.CameraAlt, Screen.CameraInfo.route),
        ToolItem(stringResource(R.string.security_audit_title), stringResource(R.string.security_audit_desc), Icons.Filled.VerifiedUser, Screen.SecurityAudit.route),
        ToolItem(stringResource(R.string.sensor_dashboard_title), stringResource(R.string.sensor_dashboard_desc), Icons.Filled.Sensors, Screen.SensorDashboard.route),
        ToolItem(stringResource(R.string.battery_info_title), stringResource(R.string.battery_info_desc), Icons.Filled.BatteryFull, Screen.BatteryInfo.route),
        ToolItem(stringResource(R.string.display_info_title), stringResource(R.string.display_info_desc), Icons.Filled.Screenshot, Screen.DisplayInfo.route),
        ToolItem(stringResource(R.string.storage_analyzer_title), stringResource(R.string.storage_analyzer_desc), Icons.Filled.Storage, Screen.StorageAnalyzer.route),
        ToolItem(stringResource(R.string.bluetooth_scanner_title), stringResource(R.string.bluetooth_scanner_desc), Icons.Filled.Bluetooth, Screen.BluetoothScanner.route),
        ToolItem(stringResource(R.string.gps_location_title), stringResource(R.string.gps_location_desc), Icons.Filled.MyLocation, Screen.GpsLocation.route),
        ToolItem(stringResource(R.string.nfc_reader_title), stringResource(R.string.nfc_reader_desc), Icons.Filled.Nfc, Screen.NfcReader.route)
    )

    val isSearching = searchQuery.isNotBlank()
    fun filterTools(tools: List<ToolItem>) = if (isSearching) {
        tools.filter { it.title.contains(searchQuery, ignoreCase = true) || it.description.contains(searchQuery, ignoreCase = true) }
    } else tools
    val filteredNetworkTools = filterTools(networkTools)
    val filteredDeviceTools = filterTools(deviceTools)
    val filteredMalaysiaCategories = malaysiaCategories.map {
        ToolCategory(it.label, filterTools(it.tools))
    }.filter { it.tools.isNotEmpty() }
    val filteredMalaysiaTools = filteredMalaysiaCategories.flatMap { it.tools }
    val filteredSingaporeCategories = singaporeCategories.map {
        ToolCategory(it.label, filterTools(it.tools))
    }.filter { it.tools.isNotEmpty() }
    val filteredSingaporeTools = filteredSingaporeCategories.flatMap { it.tools }
    val filteredIrelandCategories = irelandCategories.map {
        ToolCategory(it.label, filterTools(it.tools))
    }.filter { it.tools.isNotEmpty() }
    val filteredIrelandTools = filteredIrelandCategories.flatMap { it.tools }
    val filteredUtilityCategories = utilityCategories.map {
        ToolCategory(it.label, filterTools(it.tools))
    }.filter { it.tools.isNotEmpty() }
    val filteredUtilityCount = filteredUtilityCategories.sumOf { it.tools.size }

    val showNetwork = selectedTab == 0 || selectedTab == 1
    val showUtility = selectedTab == 0 || selectedTab == 2
    val showDevice = selectedTab == 0 || selectedTab == 3
    val showOpenData = selectedTab == 0 || selectedTab == 4
    val showMalaysia = showOpenData && (selectedTab == 0 || selectedCountry == 0)
    val showSingapore = showOpenData && (selectedTab == 0 || selectedCountry == 1)
    val showIreland = showOpenData && (selectedTab == 0 || selectedCountry == 2)

    val visibleNetwork = if (showNetwork) filteredNetworkTools else emptyList()
    val visibleUtilityCategories = if (showUtility) filteredUtilityCategories else emptyList()
    val visibleDevice = if (showDevice) filteredDeviceTools else emptyList()
    val visibleMalaysiaCategories = if (showMalaysia) filteredMalaysiaCategories else emptyList()
    val visibleMalaysia = if (showMalaysia) filteredMalaysiaTools else emptyList()
    val visibleSingaporeCategories = if (showSingapore) filteredSingaporeCategories else emptyList()
    val visibleSingapore = if (showSingapore) filteredSingaporeTools else emptyList()
    val visibleIrelandCategories = if (showIreland) filteredIrelandCategories else emptyList()
    val visibleIreland = if (showIreland) filteredIrelandTools else emptyList()
    val noResults = visibleNetwork.isEmpty() && visibleUtilityCategories.isEmpty() && visibleDevice.isEmpty() && visibleMalaysia.isEmpty() && visibleSingapore.isEmpty() && visibleIreland.isEmpty()

    val totalCount = filteredNetworkTools.size + filteredUtilityCount + filteredDeviceTools.size +
            filteredMalaysiaTools.size + filteredSingaporeTools.size + filteredIrelandTools.size

    Scaffold(
        topBar = {
            BrandTopBar(
                onMenuClick = { showMenu = true },
                showMenu = showMenu,
                onMenuDismiss = { showMenu = false },
                onSettingsClick = {
                    showMenu = false
                    navController.navigate(Screen.Settings.route)
                },
                onPrivacyClick = {
                    showMenu = false
                    showPrivacyDialog = true
                },
                onAboutClick = {
                    showMenu = false
                    showAboutDialog = true
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            // Search field — outlined, mono placeholder for the technical feel
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                placeholder = {
                    Text(
                        "Search 133 tools…",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                leadingIcon = {
                    Icon(
                        Icons.Filled.Search,
                        contentDescription = "Search",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = MaterialTheme.shapes.medium,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                )
            )

            // Tab pills — denser and more "category-switcher" than M3 default
            TabPills(
                tabTitles = tabTitles,
                tabIcons = tabIcons,
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                counts = listOf(
                    totalCount,
                    filteredNetworkTools.size,
                    filteredUtilityCount,
                    filteredDeviceTools.size,
                    filteredMalaysiaTools.size + filteredSingaporeTools.size + filteredIrelandTools.size,
                ),
            )

            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.weight(1f)
            ) {
                if (noResults) {
                    item(span = { GridItemSpan(2) }) {
                        EmptyResultsState(query = searchQuery)
                    }
                }
                if (visibleNetwork.isNotEmpty()) {
                    if (selectedTab == 0) {
                        item(span = { GridItemSpan(2) }) {
                            SectionHeader(label = "Network Tools", count = visibleNetwork.size)
                        }
                    }
                    items(visibleNetwork) { tool ->
                        ToolCard(tool = tool, onClick = { navController.navigate(tool.route) })
                    }
                }
                if (visibleUtilityCategories.isNotEmpty()) {
                    if (selectedTab == 0) {
                        item(span = { GridItemSpan(2) }) {
                            SectionHeader(
                                label = "Utility Tools",
                                count = visibleUtilityCategories.sumOf { it.tools.size },
                            )
                        }
                    }
                    visibleUtilityCategories.forEach { category ->
                        item(span = { GridItemSpan(2) }) {
                            SubSectionHeader(category.label, category.tools.size)
                        }
                        items(category.tools) { tool ->
                            ToolCard(tool = tool, onClick = { navController.navigate(tool.route) })
                        }
                    }
                }
                if (visibleDevice.isNotEmpty()) {
                    if (selectedTab == 0) {
                        item(span = { GridItemSpan(2) }) {
                            SectionHeader(label = "Device Tools", count = visibleDevice.size)
                        }
                    }
                    items(visibleDevice) { tool ->
                        ToolCard(tool = tool, onClick = { navController.navigate(tool.route) })
                    }
                }
                if (visibleMalaysia.isNotEmpty() || visibleSingapore.isNotEmpty() || visibleIreland.isNotEmpty()) {
                    if (selectedTab == 0) {
                        item(span = { GridItemSpan(2) }) {
                            SectionHeader(
                                label = "OpenData",
                                count = visibleMalaysia.size + visibleSingapore.size + visibleIreland.size,
                            )
                        }
                    }

                    if (selectedTab == 4) {
                        item(span = { GridItemSpan(2) }) {
                            @OptIn(ExperimentalMaterial3Api::class)
                            SingleChoiceSegmentedButtonRow(
                                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
                            ) {
                                SegmentedButton(
                                    selected = selectedCountry == 0,
                                    onClick = { selectedCountry = 0 },
                                    shape = SegmentedButtonDefaults.itemShape(index = 0, count = 3),
                                    icon = {}
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Image(painter = painterResource(R.drawable.flag_my), contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("MY", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                                SegmentedButton(
                                    selected = selectedCountry == 1,
                                    onClick = { selectedCountry = 1 },
                                    shape = SegmentedButtonDefaults.itemShape(index = 1, count = 3),
                                    icon = {}
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Image(painter = painterResource(R.drawable.flag_sg), contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("SG", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                                SegmentedButton(
                                    selected = selectedCountry == 2,
                                    onClick = { selectedCountry = 2 },
                                    shape = SegmentedButtonDefaults.itemShape(index = 2, count = 3),
                                    icon = {}
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Image(painter = painterResource(R.drawable.flag_ie), contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("IE", style = MaterialTheme.typography.labelMedium)
                                    }
                                }
                            }
                        }
                    }

                    if (visibleMalaysiaCategories.isNotEmpty()) {
                        if (selectedTab == 0) {
                            item(span = { GridItemSpan(2) }) {
                                FlagSectionHeader(
                                    label = "Malaysia",
                                    flagPainter = painterResource(R.drawable.flag_my),
                                    count = visibleMalaysia.size,
                                )
                            }
                        }
                        visibleMalaysiaCategories.forEach { category ->
                            item(span = { GridItemSpan(2) }) {
                                SubSectionHeader(category.label, category.tools.size)
                            }
                            items(category.tools) { tool ->
                                ToolCard(tool = tool, onClick = { navController.navigate(tool.route) })
                            }
                        }
                    }

                    if (visibleSingaporeCategories.isNotEmpty()) {
                        if (selectedTab == 0) {
                            item(span = { GridItemSpan(2) }) {
                                FlagSectionHeader(
                                    label = "Singapore",
                                    flagPainter = painterResource(R.drawable.flag_sg),
                                    count = visibleSingapore.size,
                                )
                            }
                        }
                        visibleSingaporeCategories.forEach { category ->
                            item(span = { GridItemSpan(2) }) {
                                SubSectionHeader(category.label, category.tools.size)
                            }
                            items(category.tools) { tool ->
                                ToolCard(tool = tool, onClick = { navController.navigate(tool.route) })
                            }
                        }
                    }

                    if (visibleIrelandCategories.isNotEmpty()) {
                        if (selectedTab == 0) {
                            item(span = { GridItemSpan(2) }) {
                                FlagSectionHeader(
                                    label = "Ireland",
                                    flagPainter = painterResource(R.drawable.flag_ie),
                                    count = visibleIreland.size,
                                )
                            }
                        }
                        visibleIrelandCategories.forEach { category ->
                            item(span = { GridItemSpan(2) }) {
                                SubSectionHeader(category.label, category.tools.size)
                            }
                            items(category.tools) { tool ->
                                ToolCard(tool = tool, onClick = { navController.navigate(tool.route) })
                            }
                        }
                    }
                }
            }
            BannerAd(modifier = Modifier.fillMaxWidth())
        }
    }

    if (showPrivacyDialog) {
        PrivacyDialog(onDismiss = { showPrivacyDialog = false })
    }

    if (showAboutDialog) {
        AboutDialog(
            onDismiss = { showAboutDialog = false },
            onWebsite = {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.dkly.net")))
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BrandTopBar(
    onMenuClick: () -> Unit,
    showMenu: Boolean,
    onMenuDismiss: () -> Unit,
    onSettingsClick: () -> Unit,
    onPrivacyClick: () -> Unit,
    onAboutClick: () -> Unit,
) {
    TopAppBar(
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(id = R.drawable.logo),
                    contentDescription = "CV Toolkit",
                    modifier = Modifier.height(40.dp),
                    contentScale = ContentScale.Fit
                )
                Spacer(Modifier.width(10.dp))
                Text(
                    text = "// network · device · data",
                    style = MonoText.Label.copy(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 10.sp,
                    ),
                )
            }
        },
        actions = {
            Box {
                IconButton(onClick = onMenuClick) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "Menu")
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = onMenuDismiss,
                ) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.settings_title)) },
                        onClick = onSettingsClick,
                        leadingIcon = { Icon(Icons.Filled.Settings, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Privacy & Terms") },
                        onClick = onPrivacyClick,
                        leadingIcon = { Icon(Icons.Filled.Policy, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("About Author") },
                        onClick = onAboutClick,
                        leadingIcon = { Icon(Icons.Filled.Person, contentDescription = null) }
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    )
}

@Composable
private fun TabPills(
    tabTitles: List<String>,
    tabIcons: List<ImageVector>,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    counts: List<Int>,
) {
    val scrollState = rememberScrollState()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(scrollState)
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        tabTitles.forEachIndexed { index, title ->
            val selected = index == selectedTab
            val containerColor = if (selected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceContainerLow
            val contentColor = if (selected) MaterialTheme.colorScheme.onPrimary
                else MaterialTheme.colorScheme.onSurfaceVariant
            val border = if (selected) null
                else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)

            Surface(
                onClick = { onTabSelected(index) },
                shape = RoundedCornerShape(10.dp),
                color = containerColor,
                contentColor = contentColor,
                border = border,
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                ) {
                    Icon(
                        tabIcons[index],
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                            letterSpacing = 0.3.sp,
                        ),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = counts[index].toString(),
                        style = MonoText.Label.copy(
                            color = contentColor.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                        ),
                    )
                }
            }
        }
    }
}

@Composable
private fun SubSectionHeader(label: String, count: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 4.dp, start = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(4.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.secondary),
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.3.sp,
            ),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f),
        )
        MonoCountChip(count)
    }
}

@Composable
private fun EmptyResultsState(query: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Surface(
                modifier = Modifier.size(72.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Filled.SearchOff,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                "No tools found",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "for \"$query\"",
                style = MonoText.Body.copy(
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                ),
            )
        }
    }
}

@Composable
fun ToolCard(
    tool: ToolItem,
    onClick: () -> Unit
) {
    OutlinedCard(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(132.dp),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = CardDefaults.outlinedCardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            Surface(
                modifier = Modifier.size(36.dp),
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        imageVector = tool.icon,
                        contentDescription = tool.title,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Column {
                Text(
                    text = tool.title,
                    style = MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.1.sp,
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = tool.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    lineHeight = 14.sp,
                )
            }
        }
    }
}

@Composable
private fun PrivacyDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Filled.Policy,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.width(10.dp))
                Text("Privacy & Terms", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Privacy Policy",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    """CV Toolkit respects your privacy and is committed to protecting your personal data.

Data Collection:
- We DO NOT upload your network information to our servers or anywhere else.
- All network scans, device information, and lookup results stay on your phone only.
- All data processing is performed locally on your device.
- We do not collect, store, transmit, or share your personal data.

Permissions:
- Internet: Required for IP lookups, DNS queries, and network connectivity tests.
- Network State: Used to detect your current network configuration.
- Foreground Service: Used for background speed tests.

Advertising:
- This app displays advertisements provided by Google AdMob.
- Google may collect and use data for personalized advertising. Please refer to Google's Privacy Policy for more information.

Local Storage:
- All data including IP lookup history is stored locally on your device only.
- No data is uploaded to our servers or any external servers.
- You can clear this data by clearing the app's data in your device settings.""",
                    style = MaterialTheme.typography.bodySmall
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    "Terms of Use",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    """By using CV Toolkit, you agree to the following terms:

1. Intended Use: This app is designed for legitimate network diagnostics, troubleshooting, and educational purposes only.

2. Responsible Use: You agree to use this app only on networks and devices you own or have explicit permission to test. Unauthorized network scanning may be illegal in your jurisdiction.

3. No Warranty: This app is provided "as is" without warranty of any kind. The developer is not responsible for any damages or legal issues arising from the use of this app.

4. Accuracy: While we strive for accuracy, network information and test results may vary and should not be relied upon for critical decisions.

5. Third-Party Services: Some features rely on third-party APIs (IP lookup, DNS resolution). Their availability and accuracy are not guaranteed.

6. Updates: We may update these terms at any time. Continued use of the app constitutes acceptance of any changes.

Last updated: December 2024""",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun AboutDialog(
    onDismiss: () -> Unit,
    onWebsite: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                "About Author",
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                Surface(
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Filled.Person,
                            contentDescription = null,
                            modifier = Modifier.size(40.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Khoo Lay Yang",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "www.dkly.net",
                    style = MonoText.Body,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { onWebsite() }
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Copyright All Rights Reserved",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}
