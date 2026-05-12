package cv.toolkit.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import cv.toolkit.ads.BannerAd
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.text.NumberFormat
import java.util.Locale
import java.util.concurrent.TimeUnit

// --- Dataset catalogue definition ---

private data class DatasetInfo(val id: String, val name: String)
private data class SubCategory(val name: String, val datasets: List<DatasetInfo>)
private data class Category(val name: String, val icon: String, val subcategories: List<SubCategory>)

private val CATALOGUE = listOf(
    Category("Demography", "D", listOf(
        SubCategory("Population", listOf(
            DatasetInfo("population_malaysia", "Population: Malaysia"),
            DatasetInfo("population_state", "Population: States"),
            DatasetInfo("population_district", "Population: Districts"),
            DatasetInfo("population_parlimen", "Population: Parliaments"),
            DatasetInfo("population_dun", "Population: DUNs")
        )),
        SubCategory("Births", listOf(
            DatasetInfo("births", "Daily Live Births"),
            DatasetInfo("births_annual", "Annual Live Births"),
            DatasetInfo("births_annual_state", "Annual Live Births by State"),
            DatasetInfo("births_annual_sex_ethnic", "Annual Live Births by Sex & Ethnicity"),
            DatasetInfo("births_annual_sex_ethnic_state", "Annual Live Births by State, Sex & Ethnicity"),
            DatasetInfo("births_district_sex", "Annual Live Births by District & Sex"),
            DatasetInfo("stillbirths", "Annual Stillbirths"),
            DatasetInfo("stillbirths_state", "Annual Stillbirths by State")
        )),
        SubCategory("Deaths", listOf(
            DatasetInfo("deaths", "Annual Deaths"),
            DatasetInfo("deaths_state", "Annual Deaths by State"),
            DatasetInfo("deaths_sex_ethnic", "Annual Deaths by Sex & Ethnicity"),
            DatasetInfo("deaths_sex_ethnic_state", "Annual Deaths by State, Sex & Ethnicity"),
            DatasetInfo("deaths_district_sex", "Annual Deaths by District & Sex"),
            DatasetInfo("deaths_early_childhood", "Annual Early Childhood Deaths"),
            DatasetInfo("deaths_early_childhood_sex", "Annual Early Childhood Deaths by Sex"),
            DatasetInfo("deaths_early_childhood_state", "Annual Early Childhood Deaths by State"),
            DatasetInfo("deaths_early_childhood_state_sex", "Annual Early Childhood Deaths by State & Sex"),
            DatasetInfo("deaths_maternal", "Annual Maternal Deaths"),
            DatasetInfo("deaths_maternal_state", "Annual Maternal Deaths by State")
        )),
        SubCategory("Marriage", listOf(
            DatasetInfo("marriages", "Annual Marriages"),
            DatasetInfo("marriages_age", "Annual Marriages by Age"),
            DatasetInfo("marriages_state", "Annual Marriages by State"),
            DatasetInfo("marriages_state_age", "Annual Marriages by State & Age")
        )),
        SubCategory("Fertility", listOf(
            DatasetInfo("fertility", "TFR and ASFR"),
            DatasetInfo("fertility_state", "TFR and ASFR by State")
        )),
        SubCategory("Migration", listOf(
            DatasetInfo("arrivals", "Monthly Arrivals by Nationality & Sex"),
            DatasetInfo("arrivals_soe", "Monthly Arrivals by State of Entry"),
            DatasetInfo("passports", "Monthly Passport Issuances")
        ))
    )),
    Category("Households", "H", listOf(
        SubCategory("Profile", listOf(
            DatasetInfo("hh_profile", "Households and Living Quarters"),
            DatasetInfo("hh_profile_state", "Households by State")
        )),
        SubCategory("Income", listOf(
            DatasetInfo("hh_income", "Household Income"),
            DatasetInfo("hies_malaysia_percentile", "Household Income by Percentile"),
            DatasetInfo("hh_income_state", "Household Income by State"),
            DatasetInfo("hies_state_percentile", "Household Income by State & Percentile"),
            DatasetInfo("hh_income_district", "Household Income by District"),
            DatasetInfo("hh_income_parlimen", "Household Income by Parliament"),
            DatasetInfo("hh_income_dun", "Household Income by DUN")
        )),
        SubCategory("Poverty", listOf(
            DatasetInfo("hh_poverty", "Poverty"),
            DatasetInfo("hh_poverty_state", "Poverty by State"),
            DatasetInfo("hh_poverty_district", "Poverty by District"),
            DatasetInfo("hh_poverty_parlimen", "Poverty by Parliament"),
            DatasetInfo("hh_poverty_dun", "Poverty by DUN")
        )),
        SubCategory("Inequality", listOf(
            DatasetInfo("hh_inequality", "Income Inequality"),
            DatasetInfo("hh_inequality_state", "Income Inequality by State"),
            DatasetInfo("hh_inequality_district", "Income Inequality by District"),
            DatasetInfo("hh_inequality_parlimen", "Income Inequality by Parliament"),
            DatasetInfo("hh_inequality_dun", "Income Inequality by DUN")
        )),
        SubCategory("Expenditure", listOf(
            DatasetInfo("hies_state", "Income & Expenditure: States"),
            DatasetInfo("hies_district", "Income & Expenditure: Districts"),
            DatasetInfo("hh_expenditure_parlimen", "Expenditure by Parliament"),
            DatasetInfo("hh_expenditure_dun", "Expenditure by DUN")
        )),
        SubCategory("Basic Amenities", listOf(
            DatasetInfo("hh_access_amenities", "Access to Basic Amenities")
        ))
    )),
    Category("National Accounts", "N", listOf(
        SubCategory("GDP (Annual)", listOf(
            DatasetInfo("gdp_gni_annual_nominal", "Annual Nominal GDP & GNI"),
            DatasetInfo("gdp_gni_annual_real", "Annual Real GDP & GNI"),
            DatasetInfo("gdp_annual_nominal_supply", "Nominal GDP by Sector"),
            DatasetInfo("gdp_annual_nominal_supply_granular", "Nominal GDP by Subsector"),
            DatasetInfo("gdp_annual_nominal_demand", "Nominal GDP by Expenditure"),
            DatasetInfo("gdp_annual_nominal_demand_granular", "Nominal GDP by Expenditure Sub"),
            DatasetInfo("gdp_annual_nominal_income", "Nominal GDP by Income"),
            DatasetInfo("gdp_annual_real_supply", "Real GDP by Sector"),
            DatasetInfo("gdp_annual_real_supply_granular", "Real GDP by Subsector"),
            DatasetInfo("gdp_annual_real_demand", "Real GDP by Expenditure"),
            DatasetInfo("gdp_annual_real_demand_granular", "Real GDP by Expenditure Sub")
        )),
        SubCategory("GDP (Quarterly)", listOf(
            DatasetInfo("gdp_qtr_nominal", "Quarterly Nominal GDP"),
            DatasetInfo("gdp_qtr_nominal_supply", "Quarterly Nominal by Sector"),
            DatasetInfo("gdp_qtr_nominal_supply_granular", "Quarterly Nominal by Subsector"),
            DatasetInfo("gdp_qtr_nominal_demand", "Quarterly Nominal by Expenditure"),
            DatasetInfo("gdp_qtr_nominal_demand_granular", "Quarterly Nominal by Exp. Sub"),
            DatasetInfo("gdp_qtr_real", "Quarterly Real GDP"),
            DatasetInfo("gdp_qtr_real_supply", "Quarterly Real by Sector"),
            DatasetInfo("gdp_qtr_real_supply_granular", "Quarterly Real by Subsector"),
            DatasetInfo("gdp_qtr_real_demand", "Quarterly Real by Expenditure"),
            DatasetInfo("gdp_qtr_real_demand_granular", "Quarterly Real by Exp. Sub"),
            DatasetInfo("gdp_qtr_real_sa", "Quarterly Real GDP (SA)"),
            DatasetInfo("gdp_qtr_real_sa_supply", "Quarterly Real SA by Sector"),
            DatasetInfo("gdp_qtr_real_sa_demand", "Quarterly Real SA by Expenditure")
        )),
        SubCategory("GDP (Regional)", listOf(
            DatasetInfo("gdp_state_real_supply", "Real GDP by State & Sector"),
            DatasetInfo("gdp_district_real_supply", "Real GDP by District & Sector"),
            DatasetInfo("gdp_lookup", "Lookup Table: GDP")
        )),
        SubCategory("Balance of Payments", listOf(
            DatasetInfo("bop_balance", "Balance of Key BOP Components"),
            DatasetInfo("fdi_flows", "Foreign Direct Investment Flows")
        ))
    )),
    Category("Education", "E", listOf(
        SubCategory("Infrastructure", listOf(
            DatasetInfo("schools_district", "Schools by District"),
            DatasetInfo("teachers_district", "Teachers by District"),
            DatasetInfo("lecturers_uni", "Lecturers in Public Universities")
        )),
        SubCategory("Outcomes", listOf(
            DatasetInfo("enrolment_school_district", "School Enrolment by District"),
            DatasetInfo("completion_school_state", "Completion Rates by State")
        ))
    )),
    Category("Healthcare", "HC", listOf(
        SubCategory("General Health", listOf(
            DatasetInfo("nutrition_children_sex", "Child Nutrition by Sex"),
            DatasetInfo("nutrition_children_strata", "Child Nutrition by Strata")
        )),
        SubCategory("Infrastructure", listOf(
            DatasetInfo("sanitation_access", "Sanitary Latrine Access"),
            DatasetInfo("hospital_beds", "Hospital Beds by State"),
            DatasetInfo("healthcare_staff", "Healthcare Staff by State")
        )),
        SubCategory("Programs", listOf(
            DatasetInfo("blood_donations", "Daily Blood Donations"),
            DatasetInfo("blood_donations_state", "Blood Donations by State"),
            DatasetInfo("organ_pledges", "Daily Organ Donation Pledges"),
            DatasetInfo("organ_pledges_state", "Organ Pledges by State"),
            DatasetInfo("pekab40_screenings", "PeKaB40 Screenings"),
            DatasetInfo("pekab40_screenings_state", "PeKaB40 Screenings by State"),
            DatasetInfo("infant_immunisation", "Infant Immunisation")
        )),
        SubCategory("Infectious Diseases", listOf(
            DatasetInfo("covid_cases", "COVID-19 Cases by State"),
            DatasetInfo("covid_cases_vaxstatus", "COVID-19 by Vax Status"),
            DatasetInfo("covid_cases_age", "COVID-19 by Age Group"),
            DatasetInfo("vaxreg_covid", "COVID-19 Vaccine Registrations"),
            DatasetInfo("vaxreg_covid_demog", "COVID-19 Vax by Demographics"),
            DatasetInfo("std_state", "STDs by State")
        )),
        SubCategory("Regulation", listOf(
            DatasetInfo("pharmaceutical_manufacturers", "Pharmaceutical Manufacturers"),
            DatasetInfo("pharmaceutical_importers", "Pharmaceutical Importers"),
            DatasetInfo("pharmaceutical_wholesalers", "Pharmaceutical Wholesalers"),
            DatasetInfo("cosmetics_manufacturers", "Cosmetics Manufacturers"),
            DatasetInfo("pharmaceutical_products", "Pharmaceutical Products"),
            DatasetInfo("pharmaceutical_products_cancelled", "Cancelled Pharma Products"),
            DatasetInfo("cosmetic_notifications", "Cosmetic Notifications"),
            DatasetInfo("cosmetic_notifications_cancelled", "Cancelled Cosmetic Notifs")
        )),
        SubCategory("Accounts", listOf(
            DatasetInfo("mnha", "Health Expenditure (TEH & CHE)"),
            DatasetInfo("mnha_moh", "MOH Health Expenditure")
        ))
    )),
    Category("Transportation", "T", listOf(
        SubCategory("Ridership", listOf(
            DatasetInfo("ridership_ktmb_daily", "Daily KTMB Ridership"),
            DatasetInfo("ridership_ktmb_monthly", "Monthly KTMB Ridership"),
            DatasetInfo("ridership_headline", "Daily Public Transport Ridership"),
            DatasetInfo("ridership_od_komuter", "OD Ridership: Komuter"),
            DatasetInfo("ridership_od_komuter_utara", "OD Ridership: Komuter Utara"),
            DatasetInfo("ridership_od_intercity", "OD Ridership: Intercity"),
            DatasetInfo("ridership_od_ets", "OD Ridership: ETS"),
            DatasetInfo("ridership_od_shuttle_tebrau", "OD Ridership: Shuttle Tebrau"),
            DatasetInfo("ridership_od_rapidrail_daily", "OD Ridership: Rapid Rail"),
            DatasetInfo("ridership_od_brt_daily", "OD Ridership: BRT Sunway")
        )),
        SubCategory("Vehicles", listOf(
            DatasetInfo("registration_transactions_all", "Vehicle Registrations"),
            DatasetInfo("registration_transactions_car", "Car Registrations"),
            DatasetInfo("registration_transactions_motorcycle", "Motorcycle Registrations"),
            DatasetInfo("registrations_type_fuel", "Registrations by Type & Fuel")
        ))
    )),
    Category("Public Safety", "PS", listOf(
        SubCategory("Crime", listOf(
            DatasetInfo("crime_district", "Crimes by District & Type")
        )),
        SubCategory("Justice", listOf(
            DatasetInfo("legal_advisory_services", "Legal Advisory Services"),
            DatasetInfo("legal_advisory_category", "Legal Advisory by Category"),
            DatasetInfo("legal_advisory_subcategory", "Legal Advisory by Subcategory"),
            DatasetInfo("legal_advisory_branch", "Legal Advisory by Branch"),
            DatasetInfo("prisoners_state", "Prisoners by State"),
            DatasetInfo("prisoners_prison", "Prisoners by Prison Centre")
        )),
        SubCategory("Drug Addiction", listOf(
            DatasetInfo("drug_arrests_ethnicity", "Drug Arrests by Ethnicity"),
            DatasetInfo("drug_arrests_age", "Drug Arrests by Age"),
            DatasetInfo("drug_addicts_age", "Drug Addicts by Age"),
            DatasetInfo("drug_addicts_drugtype", "Drug Addicts by Drug Type"),
            DatasetInfo("drug_addicts_education", "Drug Addicts by Education"),
            DatasetInfo("drug_addicts_occupation", "Drug Addicts by Occupation")
        ))
    )),
    Category("Welfare", "W", listOf(
        SubCategory("Retirement", listOf(
            DatasetInfo("epf_dividend", "Annual EPF Dividend")
        ))
    )),
    Category("Environment", "EN", listOf(
        SubCategory("Land Use", listOf(
            DatasetInfo("forest_reserve", "Permanent Forest Reserves"),
            DatasetInfo("forest_reserve_state", "Forest Reserves by State")
        )),
        SubCategory("Energy", listOf(
            DatasetInfo("electricity_consumption", "Monthly Electricity Consumption"),
            DatasetInfo("electricity_supply", "Electricity Supply"),
            DatasetInfo("electricity_access", "Electricity Access")
        )),
        SubCategory("Water", listOf(
            DatasetInfo("water_consumption", "Water Consumption by Sector"),
            DatasetInfo("water_production", "Water Production by State"),
            DatasetInfo("water_access", "Treated Water Access")
        )),
        SubCategory("Pollution", listOf(
            DatasetInfo("air_pollution", "Monthly Air Pollution"),
            DatasetInfo("ghg_emissions", "Greenhouse Gas Emissions"),
            DatasetInfo("water_pollution_basin", "River Basin Pollution")
        ))
    )),
    Category("Communications", "C", listOf(
        SubCategory("Telecom", listOf(
            DatasetInfo("cellular_subscribers", "Cellular Subscribers")
        ))
    )),
    Category("Labour Markets", "L", listOf(
        SubCategory("Labour Force (Monthly)", listOf(
            DatasetInfo("lfs_month", "Monthly Labour Force Stats"),
            DatasetInfo("lfs_month_sa", "Monthly Labour Force (SA)"),
            DatasetInfo("lfs_month_status", "Employment by Status"),
            DatasetInfo("lfs_month_duration", "Unemployment by Duration"),
            DatasetInfo("lfs_month_youth", "Youth Unemployment")
        )),
        SubCategory("Labour Force (Quarterly)", listOf(
            DatasetInfo("lfs_qtr", "Quarterly Labour Force Stats"),
            DatasetInfo("lfs_qtr_sru_age", "Skills Underemployment by Age"),
            DatasetInfo("lfs_qtr_sru_sex", "Skills Underemployment by Sex"),
            DatasetInfo("lfs_qtr_tru_age", "Time Underemployment by Age"),
            DatasetInfo("lfs_qtr_tru_sex", "Time Underemployment by Sex"),
            DatasetInfo("lfs_qtr_state", "Quarterly by State")
        )),
        SubCategory("Labour Force (Annual)", listOf(
            DatasetInfo("lfs_year", "Annual Labour Force Stats"),
            DatasetInfo("lfs_year_sex", "Annual by Sex"),
            DatasetInfo("lfs_state_sex", "Annual by State & Sex"),
            DatasetInfo("lfs_district", "Annual by District"),
            DatasetInfo("lfs_parlimen", "Annual by Parliament"),
            DatasetInfo("lfs_dun", "Annual by DUN"),
            DatasetInfo("employment_sector", "Employment by Sector")
        )),
        SubCategory("Productivity", listOf(
            DatasetInfo("productivity_annual", "Annual Productivity"),
            DatasetInfo("productivity_annual_priority", "Priority Subsectors"),
            DatasetInfo("productivity_qtr", "Quarterly Productivity"),
            DatasetInfo("productivity_lookup", "Lookup: Productivity")
        ))
    )),
    Category("Financial Markets", "F", listOf(
        SubCategory("Money Supply", listOf(
            DatasetInfo("monetary_aggregates", "Monetary Aggregates: M1, M2, M3"),
            DatasetInfo("currency_in_circulation", "Monthly Currency in Circulation"),
            DatasetInfo("currency_in_circulation_annual", "Annual Currency in Circulation")
        )),
        SubCategory("Banking", listOf(
            DatasetInfo("interestrates", "Monthly Interest Rates"),
            DatasetInfo("interestrates_annual", "Annual Interest Rates")
        )),
        SubCategory("Payments", listOf(
            DatasetInfo("trnsc_daily_fpx", "Daily FPX Transactions"),
            DatasetInfo("trnsc_daily_san", "Daily ATM (SAN) Transactions"),
            DatasetInfo("trnsc_daily_jompay", "Daily JomPAY Transactions"),
            DatasetInfo("trnsc_daily_directdebit", "Daily DirectDebit"),
            DatasetInfo("payment_instruments", "Payment Instruments"),
            DatasetInfo("payment_channels", "Payment Channels"),
            DatasetInfo("payment_systems", "Payment Systems")
        )),
        SubCategory("Foreign Exchange", listOf(
            DatasetInfo("exchangerates_daily_0900", "Daily Exchange Rates (0900)"),
            DatasetInfo("exchangerates_daily_1130", "Daily Exchange Rates (1130)"),
            DatasetInfo("exchangerates_daily_1200", "Daily Exchange Rates (1200)"),
            DatasetInfo("exchangerates_daily_1700", "Daily Exchange Rates (1700)"),
            DatasetInfo("exchangerates", "Monthly Exchange Rates")
        ))
    )),
    Category("Prices", "P", listOf(
        SubCategory("Consumer Prices", listOf(
            DatasetInfo("cpi_annual", "Annual CPI by Division"),
            DatasetInfo("cpi_annual_inflation", "Annual CPI Inflation"),
            DatasetInfo("cpi_headline", "Monthly CPI by Division"),
            DatasetInfo("cpi_headline_inflation", "Monthly CPI Inflation"),
            DatasetInfo("cpi_core", "Monthly Core CPI"),
            DatasetInfo("cpi_core_inflation", "Monthly Core CPI Inflation"),
            DatasetInfo("cpi_state", "Monthly CPI by State"),
            DatasetInfo("cpi_state_inflation", "Monthly CPI Inflation by State"),
            DatasetInfo("cpi_3d", "Monthly CPI by Group (3-digit)"),
            DatasetInfo("cpi_4d", "Monthly CPI by Class (4-digit)"),
            DatasetInfo("cpi_5d", "Monthly CPI by Subclass (5-digit)"),
            DatasetInfo("cpi_strata", "Monthly CPI by Strata"),
            DatasetInfo("cpi_lowincome", "CPI for Low-Income Households"),
            DatasetInfo("pricecatcher", "PriceCatcher Records")
        )),
        SubCategory("Producer Prices", listOf(
            DatasetInfo("ppi", "Producer Price Index"),
            DatasetInfo("ppi_1d", "PPI by Section (1-digit)"),
            DatasetInfo("ppi_2d", "PPI by Division (2-digit)"),
            DatasetInfo("ppi_3d", "PPI by Group (3-digit)"),
            DatasetInfo("ppi_sitc", "PPI by SITC"),
            DatasetInfo("ppi_sop", "PPI by Stage of Processing"),
            DatasetInfo("sppi", "Services PPI"),
            DatasetInfo("sppi_1d", "SPPI by Section"),
            DatasetInfo("sppi_2d", "SPPI by Division"),
            DatasetInfo("sppi_3d", "SPPI by Group")
        )),
        SubCategory("Commodity Prices", listOf(
            DatasetInfo("fuelprice", "Petroleum & Diesel Prices")
        ))
    )),
    Category("Public Admin", "PA", listOf(
        SubCategory("Finance", listOf(
            DatasetInfo("federal_finance_qtr", "Quarterly Federal Finance"),
            DatasetInfo("federal_finance_qtr_revenue", "Quarterly Revenue"),
            DatasetInfo("federal_finance_qtr_oe", "Quarterly Operating Expenditure"),
            DatasetInfo("federal_finance_qtr_de", "Quarterly Dev Expenditure"),
            DatasetInfo("federal_finance_year", "Annual Federal Finance"),
            DatasetInfo("federal_finance_year_revenue", "Annual Revenue"),
            DatasetInfo("federal_finance_year_oe", "Annual Operating Expenditure"),
            DatasetInfo("federal_finance_year_de", "Annual Dev Expenditure"),
            DatasetInfo("state_finance_expenditure", "State Expenditure"),
            DatasetInfo("state_finance_revenue", "State Revenue"),
            DatasetInfo("federal_budget_moe", "MOE Budget Allocation"),
            DatasetInfo("federal_budget_moh", "MOH Budget Allocation")
        )),
        SubCategory("Public Service", listOf(
            DatasetInfo("parliament_sex", "Female Representation: Parliament"),
            DatasetInfo("local_authority_sex", "Female Representation: Local Auth")
        )),
        SubCategory("Digital Services", listOf(
            DatasetInfo("government_apps", "Government Mobile Apps"),
            DatasetInfo("government_apps_reviews", "Gov App Reviews"),
            DatasetInfo("government_apps_active", "Active Gov Apps"),
            DatasetInfo("government_apps_downloads", "Gov App Downloads")
        )),
        SubCategory("Digital Infrastructure", listOf(
            DatasetInfo("domains", "Registered .MY Domains"),
            DatasetInfo("domains_ipv6", ".MY Domains with IPv6"),
            DatasetInfo("domains_dnssec", ".MY Domains with DNSSEC"),
            DatasetInfo("domains_idn", "Internationalised .MY Domains")
        ))
    )),
    Category("Economic Sectors", "ES", listOf(
        SubCategory("Services", listOf(
            DatasetInfo("iowrt", "Wholesale & Retail Trade"),
            DatasetInfo("iowrt_2d", "W&R Trade by Division"),
            DatasetInfo("iowrt_3d", "W&R Trade by Group")
        )),
        SubCategory("Manufacturing", listOf(
            DatasetInfo("ipi", "Industrial Production Index"),
            DatasetInfo("ipi_1d", "IPI by Section"),
            DatasetInfo("ipi_2d", "IPI by Division"),
            DatasetInfo("ipi_3d", "IPI by Group"),
            DatasetInfo("ipi_5d", "IPI by Item"),
            DatasetInfo("ipi_export", "IPI Export-Oriented"),
            DatasetInfo("ipi_domestic", "IPI Domestic-Oriented")
        )),
        SubCategory("Agriculture", listOf(
            DatasetInfo("crops_state", "Crops by State"),
            DatasetInfo("crops_district_area", "Crop Area by District"),
            DatasetInfo("crops_district_production", "Crop Production by District"),
            DatasetInfo("timber_production", "Timber Products"),
            DatasetInfo("fish_landings", "Marine Fish Landings")
        )),
        SubCategory("Mining", listOf(
            DatasetInfo("mineral_extraction", "Mineral Extraction")
        )),
        SubCategory("External Trade", listOf(
            DatasetInfo("trade_sitc_1d", "Monthly Trade by SITC")
        ))
    )),
    Category("Indicators", "I", listOf(
        SubCategory("SDG", listOf(
            DatasetInfo("sdg_03-3-1", "SDG: HIV Incidence"),
            DatasetInfo("sdg_04-6-1", "SDG: Literacy & Numeracy"),
            DatasetInfo("sdg_10-c-1", "SDG: Remittance Costs"),
            DatasetInfo("sdg_16-1-1", "SDG: Intentional Homicide"),
            DatasetInfo("sdg_16-2-2", "SDG: Human Trafficking")
        )),
        SubCategory("Economic", listOf(
            DatasetInfo("economic_indicators", "Economic Indicators")
        ))
    )),
    Category("Reference", "R", listOf(
        SubCategory("International Codes", listOf(
            DatasetInfo("currency_codes", "Currency Codes (ISO 4217)")
        )),
        SubCategory("National Codes", listOf(
            DatasetInfo("msic", "MSIC"),
            DatasetInfo("mcoicop", "MCOICOP"),
            DatasetInfo("sitc", "SITC"),
            DatasetInfo("sitc_sop", "SITC: Stage of Processing")
        )),
        SubCategory("Lookups", listOf(
            DatasetInfo("lookup_federal_finance", "Lookup: Federal Finance"),
            DatasetInfo("lookup_money_banking", "Lookup: Money & Banking")
        ))
    )),
    Category("Metadata", "M", listOf(
        SubCategory("Site Metrics", listOf(
            DatasetInfo("usage_metrics", "Daily Usage Metrics"),
            DatasetInfo("usage_metrics_openapi", "Daily OpenAPI Hits"),
            DatasetInfo("usage_metrics_openapi_cumul", "Cumulative API Hits"),
            DatasetInfo("metrics_content", "Number of Datasets"),
            DatasetInfo("metrics_dataset_cumul", "Cumulative Views & Downloads")
        )),
        SubCategory("Site Content", listOf(
            DatasetInfo("datasets", "List of Datasets"),
            DatasetInfo("arc_dosm", "DOSM Release Calendar")
        ))
    ))
)

// Flat list for search
private val ALL_DATASETS: List<Pair<String, DatasetInfo>> by lazy {
    CATALOGUE.flatMap { cat ->
        cat.subcategories.flatMap { sub ->
            sub.datasets.map { "${cat.name} > ${sub.name}" to it }
        }
    }
}

// --- Screen states ---

private enum class ViewState { CATEGORIES, DATASETS, DATA }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DataCatalogueScreen(navController: NavController) {
    var viewState by remember { mutableStateOf(ViewState.CATEGORIES) }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var selectedDataset by remember { mutableStateOf<DatasetInfo?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    // Data viewer state
    var records by remember { mutableStateOf<List<Map<String, Any?>>>(emptyList()) }
    var columns by remember { mutableStateOf<List<String>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var currentPage by remember { mutableIntStateOf(0) }
    var hasMore by remember { mutableStateOf(true) }
    val pageSize = 20

    val client = remember {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    val gson = remember { Gson() }
    val scope = rememberCoroutineScope()

    fun loadData(dataset: DatasetInfo, page: Int, append: Boolean = false) {
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                val offset = page * pageSize
                val url = "https://api.data.gov.my/data-catalogue?id=${dataset.id}&limit=$pageSize"
                val body = withContext(Dispatchers.IO) {
                    val request = Request.Builder().url(url).build()
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) throw Exception("HTTP ${response.code}")
                        response.body.string()
                    }
                }
                val type = object : TypeToken<List<Map<String, Any?>>>() {}.type
                val newRecords: List<Map<String, Any?>> = gson.fromJson(body, type)

                if (newRecords.isNotEmpty()) {
                    columns = newRecords.first().keys.toList()
                }

                if (append) {
                    records = records + newRecords
                } else {
                    records = newRecords
                }
                hasMore = newRecords.size >= pageSize
                currentPage = page
            } catch (e: Exception) {
                errorMessage = e.message ?: "Failed to load data"
            }
            isLoading = false
        }
    }

    fun openDataset(dataset: DatasetInfo) {
        selectedDataset = dataset
        viewState = ViewState.DATA
        records = emptyList()
        columns = emptyList()
        currentPage = 0
        hasMore = true
        loadData(dataset, 0)
    }

    fun goBack() {
        when (viewState) {
            ViewState.DATA -> {
                viewState = if (selectedCategory != null) ViewState.DATASETS else ViewState.CATEGORIES
                records = emptyList()
                errorMessage = null
            }
            ViewState.DATASETS -> {
                viewState = ViewState.CATEGORIES
                selectedCategory = null
            }
            ViewState.CATEGORIES -> navController.popBackStack()
        }
    }

    val title = when (viewState) {
        ViewState.CATEGORIES -> "MY Data Catalogue"
        ViewState.DATASETS -> selectedCategory?.name ?: "Datasets"
        ViewState.DATA -> selectedDataset?.name ?: "Data"
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { goBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (viewState) {
                ViewState.CATEGORIES -> CatalogueBrowser(
                    searchQuery = searchQuery,
                    onSearchChange = { searchQuery = it },
                    onCategoryClick = {
                        selectedCategory = it
                        viewState = ViewState.DATASETS
                    },
                    onDatasetClick = { openDataset(it) },
                    modifier = Modifier.weight(1f)
                )
                ViewState.DATASETS -> DatasetList(
                    category = selectedCategory!!,
                    onDatasetClick = { openDataset(it) },
                    modifier = Modifier.weight(1f)
                )
                ViewState.DATA -> DataViewer(
                    dataset = selectedDataset!!,
                    records = records,
                    columns = columns,
                    isLoading = isLoading,
                    errorMessage = errorMessage,
                    hasMore = hasMore,
                    onRetry = { loadData(selectedDataset!!, 0) },
                    modifier = Modifier.weight(1f)
                )
            }
            BannerAd(modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun CatalogueBrowser(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onCategoryClick: (Category) -> Unit,
    onDatasetClick: (DatasetInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    val isSearching = searchQuery.isNotBlank()
    val searchResults = remember(searchQuery) {
        if (!isSearching) emptyList()
        else {
            val q = searchQuery.lowercase()
            ALL_DATASETS.filter { (path, ds) ->
                ds.name.lowercase().contains(q) || ds.id.lowercase().contains(q) || path.lowercase().contains(q)
            }
        }
    }

    Column(modifier = modifier) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            placeholder = { Text("Search ~300 datasets...") },
            leadingIcon = { Icon(Icons.Filled.Search, null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Filled.Clear, "Clear")
                    }
                }
            },
            singleLine = true,
            shape = MaterialTheme.shapes.extraLarge
        )

        if (isSearching) {
            // Search results
            if (searchResults.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No datasets found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(searchResults, key = { it.second.id }) { (path, dataset) ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onDatasetClick(dataset) },
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(dataset.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text(path, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(
                                    dataset.id,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontFamily = FontFamily.Monospace,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        } else {
            // Category grid
            val totalDatasets = ALL_DATASETS.size
            Text(
                "$totalDatasets datasets across ${CATALOGUE.size} categories",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            LazyColumn(
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(CATALOGUE, key = { it.name }) { category ->
                    val datasetCount = category.subcategories.sumOf { it.datasets.size }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCategoryClick(category) },
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    category.name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "${category.subcategories.size} subcategories, $datasetCount datasets",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                Icons.Filled.ChevronRight,
                                null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DatasetList(
    category: Category,
    onDatasetClick: (DatasetInfo) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        category.subcategories.forEach { sub ->
            item(key = "header_${sub.name}") {
                Text(
                    sub.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                )
            }
            items(sub.datasets, key = { it.id }) { dataset ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDatasetClick(dataset) },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(dataset.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            Text(
                                dataset.id,
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Icon(Icons.Filled.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun DataViewer(
    dataset: DatasetInfo,
    records: List<Map<String, Any?>>,
    columns: List<String>,
    isLoading: Boolean,
    errorMessage: String?,
    hasMore: Boolean,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    var sortColumn by remember { mutableStateOf<String?>(null) }
    var sortAscending by remember { mutableStateOf(true) }
    var showChart by remember { mutableStateOf(true) }
    var searchQuery by remember { mutableStateOf("") }
    var filterColumn by remember { mutableStateOf<String?>(null) }
    var filterValue by remember { mutableStateOf<String?>(null) }

    val nf = remember { NumberFormat.getNumberInstance(Locale.US) }

    // Auto-detect date and numeric columns
    val dateColumn = remember(columns) {
        columns.firstOrNull { col ->
            val l = col.lowercase()
            l == "date" || l.contains("year") || l.contains("month") || l == "period" || l == "quarter"
        }
    }
    val numericColumns = remember(columns, records) {
        if (records.isEmpty()) emptyList()
        else columns.filter { col ->
            col != dateColumn && records.any { r ->
                val v = r[col]; v is Double || v is Number || (v is String && v.toDoubleOrNull() != null)
            }
        }
    }
    val textColumns = remember(columns, numericColumns, dateColumn) {
        columns.filter { it !in numericColumns && it != dateColumn }
    }

    // Filter options for text columns (max 20 unique values)
    val filterOptions = remember(records, textColumns) {
        textColumns.associateWith { col ->
            records.mapNotNull { it[col]?.toString() }.distinct().sorted().take(20)
        }.filter { it.value.size in 2..20 }
    }

    // Apply search + filter
    val filteredRecords = remember(records, searchQuery, filterColumn, filterValue) {
        var result = records
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.lowercase()
            result = result.filter { r -> r.values.any { it?.toString()?.lowercase()?.contains(q) == true } }
        }
        if (filterColumn != null && filterValue != null) {
            result = result.filter { it[filterColumn]?.toString() == filterValue }
        }
        result
    }

    // Sort
    val sortedRecords = remember(filteredRecords, sortColumn, sortAscending) {
        if (sortColumn == null) filteredRecords
        else {
            val col = sortColumn!!
            val sorted = filteredRecords.sortedWith(compareBy<Map<String, Any?>> {
                val v = it[col]
                when (v) {
                    is Double -> v
                    is Number -> v.toDouble()
                    is String -> v.toDoubleOrNull() ?: v.lowercase().hashCode().toDouble()
                    else -> 0.0
                }
            })
            if (sortAscending) sorted else sorted.reversed()
        }
    }

    // Chart data
    val chartLines = remember(filteredRecords, dateColumn, numericColumns) {
        if (dateColumn == null || numericColumns.isEmpty() || filteredRecords.size < 2) emptyList()
        else {
            val chrono = filteredRecords.sortedBy { it[dateColumn]?.toString() ?: "" }
            val colors = listOf(Color(0xFF6750A4), Color(0xFF7D5260), Color(0xFF006D40), Color(0xFFB3261E), Color(0xFF625B71))
            numericColumns.take(3).mapIndexed { i, col ->
                ChartLine(col, chrono.mapNotNull { r ->
                    val lbl = r[dateColumn]?.toString()?.let { if (it.length >= 7) it.substring(0, 7) else it } ?: return@mapNotNull null
                    val v = (r[col] as? Number)?.toFloat() ?: (r[col] as? String)?.toFloatOrNull() ?: return@mapNotNull null
                    ChartPoint(lbl, v)
                }, colors[i % colors.size])
            }.filter { it.points.size >= 2 }
        }
    }

    // Bar chart fallback
    val barData = remember(filteredRecords, numericColumns, dateColumn, columns) {
        if (numericColumns.isEmpty() || filteredRecords.size < 2) emptyList()
        else {
            val col = numericColumns.first()
            val labelCol = dateColumn ?: columns.firstOrNull { it != col } ?: return@remember emptyList()
            filteredRecords.sortedBy { it[labelCol]?.toString() ?: "" }.takeLast(20).mapNotNull { r ->
                val lbl = r[labelCol]?.toString()?.let { if (it.length > 10) it.takeLast(10) else it } ?: return@mapNotNull null
                val v = (r[col] as? Number)?.toFloat() ?: (r[col] as? String)?.toFloatOrNull() ?: return@mapNotNull null
                ChartPoint(lbl, v)
            }
        }
    }

    Column(modifier = modifier) {
        // Info bar
        Surface(color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f), modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.Dataset, null, Modifier.size(16.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(6.dp))
                Text(
                    "${records.size} records  •  ${columns.size} columns  •  ${sortedRecords.size} shown",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.weight(1f)
                )
                if (chartLines.isNotEmpty() || barData.size >= 2) {
                    IconButton(onClick = { showChart = !showChart }, modifier = Modifier.size(32.dp)) {
                        Icon(if (showChart) Icons.Filled.TableChart else Icons.Filled.Timeline, "Toggle chart", Modifier.size(18.dp))
                    }
                }
            }
        }

        // Search bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp).height(48.dp),
            placeholder = { Text("Search records...") },
            leadingIcon = { Icon(Icons.Filled.Search, null, Modifier.size(18.dp)) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) { Icon(Icons.Filled.Clear, "Clear", Modifier.size(16.dp)) }
                }
            },
            singleLine = true,
            shape = MaterialTheme.shapes.large
        )

        // Filter + sort chips
        if (columns.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 2.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Filter chips
                filterOptions.entries.take(3).forEach { (col, values) ->
                    var expanded by remember { mutableStateOf(false) }
                    Box {
                        FilterChip(
                            selected = filterColumn == col,
                            onClick = { if (filterColumn == col) { filterColumn = null; filterValue = null } else expanded = true },
                            label = {
                                Text(
                                    if (filterColumn == col && filterValue != null) "${col.take(8)}: ${filterValue!!.take(10)}"
                                    else col.take(12),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            },
                            leadingIcon = if (filterColumn == col) {{ Icon(Icons.Filled.FilterAlt, null, Modifier.size(14.dp)) }} else null,
                            modifier = Modifier.height(28.dp)
                        )
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            values.forEach { v ->
                                DropdownMenuItem(
                                    text = { Text(v, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                    onClick = { filterColumn = col; filterValue = v; expanded = false }
                                )
                            }
                        }
                    }
                }
                Spacer(Modifier.width(4.dp))
                // Sort chips
                columns.take(5).forEach { col ->
                    FilterChip(
                        selected = sortColumn == col,
                        onClick = { if (sortColumn == col) sortAscending = !sortAscending else { sortColumn = col; sortAscending = true } },
                        label = { Text(col.take(10), style = MaterialTheme.typography.labelSmall) },
                        leadingIcon = if (sortColumn == col) {{ Icon(if (sortAscending) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward, null, Modifier.size(14.dp)) }} else null,
                        modifier = Modifier.height(28.dp)
                    )
                }
            }
        }

        when {
            isLoading && records.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator()
                        Spacer(Modifier.height(8.dp))
                        Text("Loading ${dataset.name}...", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            errorMessage != null && records.isEmpty() -> {
                Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Filled.Warning, null, Modifier.size(32.dp), tint = MaterialTheme.colorScheme.error)
                            Spacer(Modifier.height(8.dp))
                            Text(errorMessage, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                            Spacer(Modifier.height(12.dp))
                            FilledTonalButton(onClick = onRetry) { Text("Retry") }
                        }
                    }
                }
            }
            records.isEmpty() -> {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No data available", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Line chart
                    if (showChart && chartLines.isNotEmpty()) {
                        item(key = "chart") {
                            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                                Column(Modifier.padding(12.dp)) {
                                    Text("Trend", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(8.dp))
                                    SimpleLineChart(lines = chartLines, modifier = Modifier.fillMaxWidth())
                                    Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        chartLines.forEach { line ->
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Surface(color = line.color, modifier = Modifier.size(8.dp), shape = MaterialTheme.shapes.extraSmall) {}
                                                Spacer(Modifier.width(4.dp))
                                                Text(line.label, style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Bar chart fallback
                    if (showChart && chartLines.isEmpty() && barData.size >= 2) {
                        item(key = "bar_chart") {
                            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                                Column(Modifier.padding(12.dp)) {
                                    Text(numericColumns.firstOrNull() ?: "Values", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(8.dp))
                                    SimpleBarChart(data = barData, modifier = Modifier.fillMaxWidth(), barColor = MaterialTheme.colorScheme.primary, maxBars = 20)
                                }
                            }
                        }
                    }

                    // Summary stats
                    if (numericColumns.isNotEmpty() && filteredRecords.size > 1) {
                        item(key = "stats") {
                            Card(Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))) {
                                Column(Modifier.padding(12.dp)) {
                                    Text("Summary", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                    Spacer(Modifier.height(8.dp))
                                    numericColumns.take(4).forEach { col ->
                                        val values = filteredRecords.mapNotNull { r ->
                                            val v = r[col]; when (v) { is Number -> v.toDouble(); is String -> v.toDoubleOrNull(); else -> null }
                                        }
                                        if (values.isNotEmpty()) {
                                            Text(col, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                                            Row(Modifier.fillMaxWidth().padding(bottom = 8.dp), horizontalArrangement = Arrangement.SpaceEvenly) {
                                                StatCard(label = "Latest", value = formatNum(values.first(), nf))
                                                StatCard(label = "Average", value = formatNum(values.average(), nf))
                                                StatCard(label = "Min", value = formatNum(values.min(), nf))
                                                StatCard(label = "Max", value = formatNum(values.max(), nf))
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Records header with clear
                    item(key = "header") {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Records (${sortedRecords.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            if (sortColumn != null || filterColumn != null || searchQuery.isNotBlank()) {
                                TextButton(onClick = { sortColumn = null; filterColumn = null; filterValue = null; searchQuery = "" }, modifier = Modifier.height(28.dp)) {
                                    Text("Clear All", style = MaterialTheme.typography.labelSmall)
                                }
                            }
                        }
                    }

                    // Records
                    itemsIndexed(sortedRecords, key = { index, _ -> "rec_$index" }) { index, record ->
                        SmartRecordCard(index + 1, record, columns, dateColumn, numericColumns, nf)
                    }
                }
            }
        }
    }
}

private fun formatNum(value: Double, nf: NumberFormat): String {
    return if (value == value.toLong().toDouble() && kotlin.math.abs(value) < 1e15) {
        nf.format(value.toLong())
    } else if (kotlin.math.abs(value) >= 1.0) {
        String.format("%.2f", value)
    } else {
        String.format("%.4f", value)
    }
}

@Composable
private fun SmartRecordCard(
    index: Int,
    record: Map<String, Any?>,
    columns: List<String>,
    dateColumn: String?,
    numericColumns: List<String>,
    nf: NumberFormat
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Header row with date and index
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (dateColumn != null) {
                    Text(
                        record[dateColumn]?.toString() ?: "-",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    "#$index",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                )
            }

            Spacer(Modifier.height(4.dp))

            // Numeric values in a more visual layout
            if (numericColumns.isNotEmpty()) {
                val numCols = numericColumns.filter { record[it] != null }
                if (numCols.isNotEmpty()) {
                    numCols.chunked(2).forEach { row ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            row.forEach { col ->
                                val value = record[col]
                                val display = when (value) {
                                    is Double -> formatNum(value, nf)
                                    is Number -> nf.format(value.toLong())
                                    else -> value?.toString() ?: "-"
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(col, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(display, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace)
                                }
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }

            // Non-numeric, non-date fields
            val otherCols = columns.filter { it != dateColumn && it !in numericColumns }
            if (otherCols.isNotEmpty()) {
                if (numericColumns.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                }
                otherCols.forEach { col ->
                    val value = record[col]?.toString() ?: "-"
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(col, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(0.4f))
                        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, modifier = Modifier.weight(0.6f), maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}
