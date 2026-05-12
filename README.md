# CV Toolkit

**🌐 [中文版 README](README_CN.md)**

<div align="center">
  <img src="app/src/main/res/drawable/logo.png" alt="CV Toolkit Logo" width="200"/>

  **A Comprehensive Network Diagnostics & Utility Toolkit for Android**
  
  [![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://www.android.com/)
  [![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg)](https://android-arsenal.com/api?level=24)
  [![Kotlin](https://img.shields.io/badge/Kotlin-2.3.20-blue.svg)](https://kotlinlang.org)
  [![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Latest-blue.svg)](https://developer.android.com/jetpack/compose)

  [![Download APK](https://img.shields.io/badge/Download-APK%20v10.0-brightgreen?style=for-the-badge&logo=android)](https://github.com/osscv/CVToolkit/raw/refs/heads/main/app/release/files/CVToolkit-V10-release.apk)

  ---

  **⚠️ IMPORTANT:** If you use this repository, you **MUST** keep all author attribution intact.
  **DO NOT REMOVE** the author credits. See [License & Attribution](#-license--attribution) for details.

  **Copyright © 2024-2026 [Khoo Lay Yang](https://www.dkly.net) - All Rights Reserved**
</div>

---

## 📑 Table of Contents

- [About](#-about)
- [Feature Highlights](#-feature-highlights)
- [Key Features](#-key-features)
  - [Network Tools](#-network-tools-17-tools)
  - [Utility Tools](#️-utility-tools-81-tools-across-14-categories)
  - [Device Tools](#-device-tools-11-tools)
  - [MY OpenData](#-my-opendata-23-tools)
  - [SG OpenData](#-sg-opendata-11-tools)
  - [IE OpenData](#-ie-opendata-1-tool)
- [Technical Stack](#️-technical-stack)
- [Design System](#-design-system)
- [UI/UX Features](#-uiux-features)
- [Multi-language Support](#-multi-language-support)
- [What's New](#-whats-new---latest-updates)
- [Getting Started](#-getting-started)
- [Features Breakdown](#-features-breakdown)
- [Use Cases](#-use-cases--examples)
- [Privacy & Security](#-privacy--security)
- [Responsible Use](#️-responsible-use)
- [Contributing](#-contributing)
- [License & Attribution](#-license--attribution)
- [Author](#-author)
- [Support](#-support)
- [Acknowledgments](#-acknowledgments)
- [Statistics](#-statistics)

---

## 📱 About

**CV Toolkit** is a professional-grade, all-in-one Android application designed for network diagnostics, cloud infrastructure monitoring, system information, and a comprehensive suite of developer utilities. Built with modern Android technologies (Kotlin 2.3.20 + Jetpack Compose), it delivers **133 powerful tools across 139 screens** in one beautifully designed app.

**Perfect for:**
- 👨‍💻 **Network Administrators** - Comprehensive network diagnostics and monitoring
- 🌐 **Cloud Engineers** - Test connectivity to 311+ regions across 14 cloud providers with real-time graphs
- 🔐 **Security Professionals** - Port scanning, SSL checking, and security analysis tools
- 💻 **Software Developers** - Encoding, hashing, JWT decoding, and development utilities
- 🎓 **Students & Educators** - Learn networking concepts with hands-on tools
- 🏠 **Home Users** - Troubleshoot network issues and monitor your connection

**Why CV Toolkit?**
- ✅ All processing is done **100% locally** on your device
- ✅ Your data **never leaves your phone** - complete privacy guaranteed
- ✅ No registration or account required
- ✅ Works offline for most tools (network tools require connectivity)
- ✅ Professional-grade accuracy and performance
- ✅ Beautiful, modern Material Design 3 interface
- ✅ Regular updates with new features and improvements

---

## 🌟 Feature Highlights

### 🚀 **Advanced CDN & Cloud Latency Testing**
The crown jewel of CV Toolkit - a professional-grade network monitoring tool featuring:
- **Real-time line graphs** with multi-region visualization
- **Continuous monitoring** with configurable intervals (1s/2s/5s)
- **Comprehensive statistics**: Min, Max, Avg, Jitter, and Packet Loss tracking
- **311+ test regions** across 14 global cloud providers
- **Monitor 35+ regions simultaneously** with color-coded tracking
- **Edge location detection** for Cloudflare, CloudFront, and Alibaba Cloud
- **Background service** support for uninterrupted monitoring

### 📱 **133 Professional Tools**
Everything you need for network diagnostics, development, PDF management, image processing, code editing, Malaysia & Singapore open data, and system analysis in one app.

### 🔒 **Privacy-First Design**
- Zero data collection
- No server uploads
- 100% local processing
- Transparent and well-documented

---

## ✨ Key Features

### 🌐 Network Tools (17 Tools)

- **Speed Test** - Comprehensive internet speed testing with download/upload speeds, ping latency, jitter measurement, and historical tracking. Foreground service support for uninterrupted testing. **Custom targets are now persisted across sessions** — add your own with the **+** button and delete them anytime from the dropdown
- **Ping Test** - ICMP ping testing with packet count configuration, timeout settings, and detailed statistics (min/max/avg/packet loss)
- **Continuous Ping** - Multi-host real-time ping monitoring with live line graphs, color-coded per host, for ongoing connectivity analysis
- **Traceroute** - Visual network path tracing showing all hops with latency measurements and geographic information
- **Port Scanner** - Scan for open ports on any host with service detection and parallel scanning
- **Network Scanner (Device Discovery)** - Discover all devices connected to your network with MAC address lookup and manufacturer identification
- **IP Lookup** - Get detailed geolocation, ISP, security flags (VPN/proxy/TOR detection), and carrier information for any IP address with local history
- **DNS Lookup** - Comprehensive DNS record querying (A, AAAA, CNAME, MX, TXT, NS, SOA, PTR) with custom DNS server support and response time tracking
- **Subnet Calculator** - Advanced IPv4/IPv6 subnet calculations with CIDR notation, network/broadcast addresses, usable host range, and subnet mask conversion
- **SSL/TLS Checker** - Detailed SSL certificate inspection including validity period, issuer information, subject details, certificate chain, encryption algorithms, and expiration warnings
- **Whois Lookup** - Get domain registration information
- **HTTP Headers Viewer** - Inspect HTTP response headers
- **Custom Request Builder** - Build and send custom HTTP/CURL requests with headers and body
- **WiFi Analyzer** - Analyze WiFi signal strength, channels, and network details
- **DNS Benchmark** - Compare response times across 10 popular DNS providers (Google, Cloudflare, Quad9, OpenDNS, AdGuard, etc.) with bar chart visualization, sorting by speed/name/reliability, configurable query count, and detailed min/max/avg statistics
- **CDN & Cloud Latency Test** - **Real-time monitoring with live graphs** - Test latency to 311+ regions across 14 major providers with continuous ping, multi-region tracking, detailed statistics (min/max/avg/jitter/packet loss), automatic edge detection, and color-coded line charts:
  - AWS (Amazon Web Services)
  - GCP (Google Cloud Platform)
  - Azure (Microsoft Azure)
  - Oracle Cloud
  - Alibaba Cloud
  - DigitalOcean
  - Linode (Akamai)
  - Vultr
  - Hetzner
  - OVH
  - Tencent Cloud
  - Huawei Cloud
  - Lightsail
  - Fastly
- **Network Stress Test** - Quick-access hub for network performance diagnostics including speed tests, DNS lookups, and port scanning

### 🛠️ Utility Tools (81 Tools across 14 Categories)

**🔄 Encoders & Decoders (8 tools):**
- **JSON Formatter/Validator** - Format, minify, and validate JSON with syntax highlighting, statistics (key/value counts, depth, types), configurable indentation, and one-tap copy
- **Base64 Encoder/Decoder** - Encode and decode Base64 strings
- **URL Encoder/Decoder** - Encode and decode URL strings
- **Binary Converter** - Convert text to/from Binary, Hex, and Octal
- **Hex Encoder/Decoder** - Convert text to/from hexadecimal
- **ASCII Converter** - Convert text to ASCII values and vice versa
- **JWT Decoder** - Decode and inspect JWT tokens
- **Image Base64** - Convert images to/from Base64 encoding

**🔐 Security & Crypto (6 tools):**
- **Hash Generator** - Generate MD5, SHA-1, SHA-256, SHA-384, SHA-512 hashes
- **Caesar Cipher** - Encrypt/decrypt with Caesar cipher, ROT13, ROT47
- **Morse Code Converter** - Convert text to Morse code and back
- **Password Generator** - Generate strong, secure passwords with customizable options
- **Password Strength Checker** - Analyze password entropy, character composition, crack time estimation, detect common patterns/sequences, and provide detailed security checks with visual strength meter
- **File Hash Calculator** - Calculate MD5, SHA, CRC32 hashes for files with metadata

**📱 QR & Barcode (3 tools):**
- **QR Code Generator** - Generate QR codes from text, URLs, contacts, etc.
- **Barcode Generator** - Generate various barcode formats (EAN, UPC, Code 128, etc.)
- **QR/Barcode Scanner** - Scan and decode QR codes and barcodes with camera

**🌐 Web & API (4 tools):**
- **API Tester** - Save, organize, and execute API requests with custom headers and body
- **User Agent Parser** - Parse and analyze browser User-Agent strings
- **Robots.txt Analyzer** - Fetch and parse robots.txt files from any domain
- **Sitemap Viewer** - Parse and browse XML sitemaps

**📄 PDF & Documents (8 tools):**
- **PDF Viewer** - View and navigate PDF documents
- **PDF Merge** - Combine multiple PDF files into one with multi-select import, drag-to-reorder, and full page preview before saving
- **PDF Split** - Split PDF into individual pages or custom page ranges with visual page thumbnails, three split modes (Each Page, Page Range, Select Pages), and page selection/deselection
- **PDF to Image** - Extract PDF pages as high-quality PNG or JPEG images with configurable DPI (72/150/200/300), JPEG quality slider, per-page preview with dimensions and file size, and individual page saving
- **Image to PDF** - Convert images to PDF with multi-select import, drag-to-reorder, per-image rotation (90°/180°/270°), visual crop editor with draggable handles, page size/orientation/margin/fit settings, and full page preview before export
- **Compress PDF** - Reduce PDF file size
- **PDF Password** - Add or remove password protection from PDFs with 128-bit encryption, configurable permissions (printing, copying, modifying), auto-detection of encrypted files, and password verification
- **Slides to PDF** - Convert presentation slides (PPT) to PDF

**✏️ Editors (4 tools):**
- **Text Editor** - Create and edit text files on device
- **Markdown Editor** - Write markdown with live preview
- **Markdown Preview** - Render and preview markdown content
- **SVG Viewer & Editor** - View and edit SVG vector graphics

**🎨 Media & Colors (18 tools):**
- **Compress Image** - Reduce image file size while maintaining quality
- **Image Resizer** - Resize images to exact dimensions with social media presets (Instagram, Facebook, Twitter/X, YouTube, LinkedIn, TikTok, Pinterest, WhatsApp) and common sizes (HD, Full HD, 2K, 4K, A4), with aspect ratio locking
- **Image Cropper** - Crop images with 10 aspect ratio presets (Free, 1:1, 4:3, 3:4, 16:9, 9:16, 3:2, 2:3, 5:4, 4:5), visual crop overlay with rule-of-thirds grid, draggable crop area, and corner handles
- **Image Format Converter** - Convert between JPEG, PNG, WebP (Lossy/Lossless), and BMP with quality control for lossy formats and before/after size comparison
- **EXIF Viewer** - View detailed image EXIF metadata organized by category: Camera (make, model, lens), Image Details (dimensions, resolution, color space, orientation), Shooting Settings (exposure, aperture, ISO, focal length, flash, metering), Date & Time, GPS Location (lat/long, altitude), and Copyright info. Copy all data to clipboard
- **EXIF Remover** - Strip EXIF metadata from images for privacy with batch processing support, EXIF tag count display, and clean JPEG output at 95% quality
- **Color Picker** - Pick colors from any image by tapping, with HEX, RGB, RGBA, and HSL values, per-value copy buttons, color history palette, and copy-all functionality
- **Color Converter** - Convert between HEX, RGB, HSL, and CMYK color formats
- **Color Palette Generator** - Generate and explore color palettes
- **Favicon Generator** - Generate favicons from images in 15 standard sizes (16x16 to 512x512) for browser tabs, Apple Touch Icons, Android Chrome, PWA, Windows tiles, and more, with select all/none, individual previews, and per-size saving
- **GIF Maker** - Create animated GIFs from multiple images or convert video to GIF with configurable frame delay (FPS), output width, and frame preview
- **Photo Collage** - Arrange multiple photos (2-6) into 7 layout templates (side-by-side, stacked, grid, L-shaped) with adjustable padding, background color, and output size
- **Image Watermark** - Add text or image watermarks with customizable opacity, position (9-point grid), rotation, color, size, and tiled pattern mode
- **Image Background Remover** - Remove image backgrounds by color selection with adjustable threshold and edge sensitivity, side-by-side before/after preview
- **Image Filters** - Apply visual filters and effects to images
- **Image Rotate/Flip** - Rotate and flip images with quick 90°/180°/270° rotation and horizontal/vertical flip options
- **Meme Generator** - Create memes by adding customizable text overlays to images
- **Screenshot Stitcher** - Combine multiple screenshots into a single long vertical image for sharing

**🔢 Converters & Calculators (10 tools):**
- **IP Calculator** - Convert between different IP formats
- **Unit Converter** - Convert length, weight, temperature, and data sizes
- **World Time** - View current time across global time zones
- **Unix Timestamp Converter** - Convert between Unix timestamps and readable dates
- **UUID Generator** - Generate UUID v1, v4, and v5
- **Number Base Converter** - Convert numbers between decimal, binary, hexadecimal, octal, and custom bases with real-time conversion
- **Currency Converter** - Live currency exchange rates for 30+ currencies with conversion calculations and caching
- **Aspect Ratio Calculator** - Calculate aspect ratios, scale dimensions while maintaining ratios, and preview common presets (16:9, 4:3, etc.)
- **Date Calculator** - Calculate days between dates, add/subtract time periods, and determine age with detailed year/month/day breakdowns
- **Scientific Calculator** - Full-featured calculator with trigonometric, logarithmic, and advanced math functions, history tracking, and angle mode selection

**📝 Text & Misc (9 tools):**
- **Text Counter** - Count words, characters, lines, and paragraphs
- **Text Diff** - Compare two texts and highlight differences
- **Lorem Ipsum Generator** - Generate placeholder text for design and development
- **Typing Test** - Practice and measure typing speed (WPM) and accuracy with 2,300+ unique content pieces across 25+ topics. Features 3 modes (Words/Sentences/Paragraphs), 3 difficulty levels, configurable duration (30s/60s/120s/Full Text), real-time stats, and repeat/new test options. Every session generates a unique random combination
- **Stopwatch** - Timer with lap functionality
- **Text Case Converter** - Convert text between uppercase, lowercase, title case, sentence case, and other formats
- **Pomodoro Timer** - Time-boxed productivity timer with work/break intervals for focused sessions
- **Note Pad** - Simple and quick note-taking utility
- **Random Generator** - Multi-tab random generator for numbers, strings, colors, UUIDs, and dice rolls with customizable parameters

### 📱 Device Tools (11 Tools)

- **Device Information** - Complete system information (CPU, RAM, storage, OS, etc.)
- **DRM & Codec Info** - View DRM systems, video/audio codecs, and supported formats
- **Camera Information** - Detailed camera specifications and capabilities
- **Security Audit** - Comprehensive device security check and vulnerability assessment
- **Sensor Dashboard** - Real-time readings from all device sensors (accelerometer, gyroscope, proximity, etc.)
- **NFC Reader** - Read and display NFC tag contents including tag type (MIFARE, ISO 14443, FeliCa, etc.), tag ID, NDEF records with parsed URI/text/MIME payloads, supported technologies, writable status, and max size. Copy all data to clipboard
- **Battery Info** - Real-time battery status including level, health, temperature, voltage, and charging information with auto-refresh
- **Display Info** - Detailed display specifications including resolution, density, refresh rate, supported refresh rates, and HDR capabilities
- **Storage Analyzer** - Visual breakdown of internal storage usage by category (apps, media, cache, etc.) with ring charts and RAM analysis
- **Bluetooth Scanner** - Discover and list nearby Bluetooth devices with signal strength (RSSI), device type, bond status, and BLE/Classic support
- **GPS Location** - Track current GPS location with coordinates, address, altitude, speed, and connected satellite constellation information

### 🇲🇾 MY OpenData (23 Tools)

Real-time government statistics and data from Malaysia's official open data portal (DOSM & data.gov.my):

- **Weather** - Weather forecasts, severe weather warnings, and earthquake information for Malaysia using official government API
- **Birthday Explorer** - Explore the number of births on any selected date in Malaysia with historical data spanning 1920–2022
- **Data Catalogue** - Browse Malaysia's comprehensive open data catalogue organized by categories (demography, households, economy, etc.)
- **Immigration** - Track international arrivals to Malaysia by country with monthly trends and demographic breakdowns
- **Fuel Price** - Historical fuel prices (RON95, RON97, diesel) in Malaysia with weekly change data and price trend charts
- **Population** - Population statistics by Malaysian states with bar charts, sorting by name or population, and year selection
- **Exchange Rate** - BNM historical exchange rates for MYR against 15+ currencies with trend visualization
- **CPI & Inflation** - Consumer Price Index trends and year-over-year inflation rates for Malaysia
- **COVID-19** - COVID-19 case statistics by Malaysian state with new cases, active cases, recoveries, and imported case tracking
- **Crime Statistics** - Crime data by category (assault, robbery, theft, etc.) with yearly trends and summary statistics
- **Transport Ridership** - Public transportation ridership data for KL/Selangor including LRT, MRT, Monorail, KTM, and ETS services
- **Births & Deaths** - Annual births vs deaths demographic trends with line charts and natural growth statistics
- **Marriage Statistics** - Annual marriage trends with counts, rates per 1,000, peak year analysis, and trend visualization
- **Household Income** - Household income comparison by state with mean/median income, expenditure, bar charts, and year selector
- **GDP & GNI** - Real GDP and GNI growth trends with per-capita analysis, year-over-year growth percentages, and multi-line charts
- **Labour Market** - Monthly employment and unemployment statistics with color-coded unemployment rate trends and labour force breakdown
- **Blood Donations** - Daily blood donation tracking with 7-day/30-day averages, monthly aggregation bar charts, and color-coded donation levels
- **Interest Rates** - BNM interest rate trends including OPR, base rate, fixed deposit, and savings rates with multi-line comparison charts
- **Electricity Consumption** - Monthly electricity consumption by sector (residential, commercial, industrial) with trend charts and sector breakdown
- **EPF Dividend** - Historical EPF dividend rates for conventional and shariah accounts with trend visualization and rate comparison
- **Vehicle Registrations** - Monthly car registration trends with 12-month averages, peak month analysis, and bar charts
- **Transit Tracker** - Real-time public transport vehicle tracking on an interactive OpenStreetMap with GTFS Realtime data from 15 Malaysian transit agencies (KTMB, Prasarana, myBAS), live vehicle positions with route/speed/bearing, user geolocation with nearest-first sorting, vehicle route tracing with polyline trails, and auto-refresh every 30 seconds
- **Sarawak Data** - Browse and search 200+ Sarawak state government datasets with dedicated viewer pages featuring auto-charts, stats, search, filter, and multi-format parsing (XLSX, CSV, HTML, JSON)

### 🇸🇬 SG OpenData (11 Tools)

Real-time government data and 5,000+ datasets from Singapore's data.gov.sg:

**Real-time Weather & Environment:**
- **SG Weather** - 24-hour forecast, 2-hour area forecast, and 4-day outlook with regional breakdowns by area (North/South/East/West/Central)
- **SG Air Quality** - PSI, PM2.5, and UV Index readings by region with color-coded health indicators and hourly bar charts
- **SG Environment** - Real-time air temperature, rainfall, humidity, wind speed, and wind direction readings from 50+ weather stations with bar charts
- **SG Heat Stress (WBGT)** - Wet Bulb Globe Temperature readings from 15 monitoring stations with heat stress level indicators (Low/Moderate/High/Extreme)
- **SG Lightning** - Real-time lightning strike observations on an interactive OpenStreetMap with Cloud-to-Cloud and Cloud-to-Ground classification
- **SG Flood Alerts** - Active flood warnings with severity levels, affected areas, and recommended actions from PUB

**Transport:**
- **SG Taxi Availability** - Real-time map of 3,000+ available taxis across Singapore on OpenStreetMap with regional distribution summary
- **SG Carpark** - Real-time HDB carpark lot availability with search, sort by availability/occupancy, and color-coded occupancy bars
- **SG Traffic Cameras** - Live traffic camera images in a 2-column grid with tap-to-expand full-screen view from 80+ cameras

**Intellectual Property:**
- **SG IP Applications** - Design and patent application lookup by lodgement date with status tracking and detailed application info

**Reference:**
- **SG Data Catalogue** - Browse and search 5,000+ government datasets, each opening as its own dedicated page with auto-generated charts, summary statistics, search, filter dropdowns, column sorting, and multi-format parsing (XLSX, CSV, HTML, JSON)

### 🇮🇪 IE OpenData (1 Tool)

Ireland's open data portal with 21,900+ government datasets:

- **IE Data Catalogue** - Browse and search 21,900+ Irish government datasets from data.gov.ie with theme filters (Government, Environment, Health, Society, Transport), each opening as a dedicated page with auto-generated charts, summary statistics, search, filter, sort, and multi-format parsing (XLSX, CSV, HTML tables, JSON) with WebView fallback for web-based datasets

---

## 🏗️ Technical Stack

- **Language:** Kotlin 2.3.20 (100%)
- **UI Framework:** Jetpack Compose BOM 2026.03.00 (Modern declarative UI with Material Design 3)
- **Architecture:** MVVM with Repository pattern for clean separation of concerns
- **Build System:** AGP 9.1.0, Gradle 8.0+, JDK 11+
- **Networking:**
  - Retrofit 3.0.0 (Type-safe REST API calls)
  - OkHttp 5.3.2 (HTTP client with interceptor support and logging)
  - Gson 2.13.2 (JSON parsing and serialization)
  - Coroutines (Asynchronous programming)
- **Camera & ML:**
  - CameraX 1.5.3 (Modern camera API with preview and analysis)
  - ML Kit Barcode Scanning 17.3.0 (On-device QR/Barcode recognition)
  - ZXing Core 3.5.4 (QR code and barcode generation)
- **Data Visualization:**
  - Canvas API (Custom line graphs and charts)
  - Real-time rendering with Compose recomposition
- **Background Processing:**
  - Foreground Services (Speed tests and continuous monitoring)
  - Coroutines (Parallel processing and async operations)
- **Data Persistence:**
  - SQLite (IP lookup history storage)
  - SharedPreferences (Language settings, ad usage tracking)
- **Navigation:** Jetpack Navigation Compose 2.9.7 with type-safe sealed class routes (139 destinations)
- **Minimum SDK:** Android 7.0 (API 24) - Wide device compatibility
- **Target SDK:** Android 16 (API 36) - Latest Android features
- **Compilation SDK:** 36 (Android 16)
- **Transit & Open Data:**
  - GTFS Realtime Bindings (Protocol Buffers parsing for real-time transit feeds)
  - Protobuf Java 4.x (Binary protocol buffer deserialization)
  - WebView + Leaflet.js (Interactive OpenStreetMap display with markers and polylines)
  - Built-in FileParser (XLSX via ZIP+XML, CSV/TSV, HTML table extraction — zero external dependencies)
  - CKAN API integration (Ireland, Sarawak open data portals)
  - WebViewAssetLoader (local asset serving for map tiles)
- **Image Loading:** Coil 2.7.0 (Efficient image loading with SVG support and caching)
- **Monetization:** Google AdMob (Non-intrusive banner ads with usage-based interstitial and rewarded ads)

## 🎨 Design System

CV Toolkit ships a custom diagnostic-tool design language built on top of Material Design 3. The full system lives under `app/src/main/java/cv/toolkit/ui/`:

```
ui/
├── theme/
│   ├── Color.kt       ─ Brand palette + semantic latency / chart tokens
│   ├── Type.kt        ─ Full M3 type scale + MonoText (Display/Title/Body/Label)
│   ├── Shape.kt       ─ Tighter corner radii (6 / 10 / 14 / 18 / 24 dp)
│   └── Theme.kt       ─ Light + dark schemes, edge-to-edge system bars
├── components/
│   └── Components.kt  ─ Reusable primitives (see below)
└── UpdateDialog.kt
```

### Palette

| Role | Light | Dark | Use |
|------|-------|------|-----|
| **Primary**   | `SignalCyan40` | `SignalCyan70` | Action accents, links, selected tabs |
| **Secondary** | `TerminalGreen40` | `TerminalGreen60` | Positive status, completion |
| **Tertiary**  | `Amber40` | `Amber60` | Warnings, callouts |
| **Surface**   | `Paper100/99/97` | `Ink05/10/15/20/25` | Containers, cards, sheets |
| **Outline**   | `Paper70/90` | `Ink25/50` | Borders, dividers |

Dynamic color (Material You) is **opt-in** so the brand identity stays consistent across devices and screenshots.

### Semantic Tokens

Instead of inline hex literals, screens pull from semantic tokens declared in `Color.kt`:

| Token | Use |
|-------|-----|
| `LatencyExcellent` / `Good` / `Fair` / `Poor` / `Unknown` | Ping, CDN, DNS, SSL status badges |
| `ChartPalette` (8 hues) | Multi-series line / bar charts |

A `latencyColor(ms)` helper in `ui/components/Components.kt` returns the right token for a latency reading.

### Typography

`MonoText` styles (`Display` / `Title` / `Body` / `Label`) deliver tabular numerics for stats, latency, IPs, hashes, hex dumps, byte counters and version strings. UI chrome uses `FontFamily.SansSerif`.

### Reusable Components

Common primitives in `ui/components/Components.kt`:

- `SectionHeader(label, count)` — "▸ NETWORK TOOLS [16]" terminal-style header
- `FlagSectionHeader(label, flagPainter, count)` — country-grouped section header
- `MonoCountChip(count)` — small monospace count pill
- `StatusBadge(text, tone)` — dot + mono pill for stats and statuses
- `StatTile(label, value, icon, tone)` — outlined stat card with mono value
- `ToolScaffold(title, onBack, actions, content)` — consistent tool-screen wrapper
- `MonoTag(text)` — tiny caps mono label (e.g. `RTT`, `TTL`, `AS#`)

## 🎨 UI/UX Features

- **Custom Diagnostic-Tool Design Language** - Distinctive technical aesthetic built on Material Design 3: SignalCyan primary, TerminalGreen secondary, Amber tertiary with deep "ink" dark surfaces and warm "paper" light surfaces
- **Monospaced Numerics** - IP addresses, hashes, latency readings, byte counters and version strings render in monospace for clean column alignment
- **Outlined Card Grid** - Dense, scannable tool grid with tinted icon squares, mono section counts and "▸ SECTION" terminal-style headers
- **Tab Pills with Mono Counts** - Compact scrollable category switcher with per-tab tool counts
- **Adaptive Light & Dark Themes** - Cohesive light and dark palettes; system-bar icon contrast follows background luminance automatically
- **Edge-to-Edge Layout** - Transparent system bars across all screens
- **Semantic Latency Tokens** - Centralized `latencyColor()` helper (Excellent/Good/Fair/Poor) drives status badges across CDN, Ping, DNS, and SSL tools
- **Reusable Design Components** - Shared `SectionHeader`, `StatusBadge`, `StatTile`, `ToolScaffold`, `MonoTag` primitives keep all 139 screens visually consistent
- **Responsive Layout** - Optimized for all screen sizes and orientations
- **Smooth Animations** - Polished transitions and interactions
- **Real-time Updates** - Live data visualization without page refreshes
- **Provider Logos** - Visual identification with official brand logos
- **Intuitive Navigation** - Easy-to-use interface with logical organization
- **Copy & Share** - One-tap actions for results and data
- **Search & Filter** - Quickly find what you need across 311+ regions

## 🌍 Multi-language Support

CV Toolkit supports 18 languages:

| Language | Code | Language | Code |
|----------|------|----------|------|
| 🇬🇧 English | en | 🇨🇳 简体中文 | zh-CN |
| 🇹🇼 繁體中文 | zh-TW | 🇯🇵 日本語 | ja |
| 🇰🇷 한국어 | ko | 🇩🇪 Deutsch | de |
| 🇪🇸 Español | es | 🇫🇷 Français | fr |
| 🇮🇹 Italiano | it | 🇧🇷 Português | pt-BR |
| 🇷🇺 Русский | ru | 🇹🇷 Türkçe | tr |
| 🇮🇳 हिन्दी | hi | 🇹🇭 ไทย | th |
| 🇻🇳 Tiếng Việt | vi | 🇮🇩 Indonesia | id |
| 🇲🇾 Melayu | ms | 🇸🇦 العربية | ar |

---

## 🆕 What's New - Latest Updates

### V10.0 - Diagnostic-Tool Design Refresh & Speed Test Persistence 🎨🔧

A focused release: zero new tools, every screen redesigned. The app moves from the stock Material You purple template to a custom, distinctive brand identity inspired by terminal / network-diagnostic tools, plus a long-standing Speed Test bug is fixed.

**New design language across all 139 screens:**

| Area | Before | After |
|------|--------|-------|
| **Palette** | Stock Material You purple | SignalCyan primary · TerminalGreen secondary · Amber tertiary · Ink/Paper neutrals |
| **Dynamic color** | Default on (Material You) | **Opt-in** — consistent brand on every device |
| **Typography** | Single overridden style | Full M3 scale + `MonoText` (Display / Title / Body / Label) for numerics |
| **Shapes** | M3 defaults | Tighter radii (6 / 10 / 14 / 18 / 24 dp) |
| **System bars** | Solid surface colors | Edge-to-edge with auto icon contrast |
| **Home cards** | Square elevated 2-up grid | Dense outlined cards with tinted icon squares |
| **Section headers** | Plain bold text | "▸ SECTION" terminal headers + mono count chips |
| **Tabs** | M3 scrollable tab row | Scrollable pill bar with icons + mono counts |

**Component library** — new `ui/components/` package with `SectionHeader`, `FlagSectionHeader`, `MonoCountChip`, `StatusBadge`, `StatTile`, `ToolScaffold`, `MonoTag`, plus a `latencyColor()` helper that returns semantic latency tokens (`LatencyExcellent` / `Good` / `Fair` / `Poor` / `Unknown`) — usable across every tool screen instead of inline hex literals.

**Redesigned screens** — Home (brand top bar with mono tagline · outlined card grid · pill tabs · terminal-style section headers · refined Privacy/About dialogs and empty-results state) · Settings (section headers · outlined row cards with tinted icon containers · mono English-name subtitle for languages) · Update dialog (mono version + filesize, primary-tinted version label).

**Bug fixes:**
- **Speed Test — custom targets now persist** — Targets added via the **+** button are saved to `SharedPreferences` (via new `SpeedTestTargetsManager`, Gson-encoded) and survive both navigation away from the screen and full app restart. Built-in targets stay protected; custom targets gain a trash-icon delete button in the dropdown that falls back to the first default if the currently-selected target is removed.

**Internal changes:**
- New `ui/theme/Shape.kt`, `ui/components/Components.kt`, `data/SpeedTestTargetsManager.kt`
- Removed deprecated `Window.statusBarColor` / `Window.navigationBarColor` calls in favor of `enableEdgeToEdge()` + `WindowInsetsControllerCompat.isAppearanceLightStatusBars`

### V9.0 - Malaysia Data, Device Deep Dive & Productivity Boost 🇲🇾📊🔧

**New Malaysia Open Data Tools (22 tools):**
- **Weather** - Real-time weather forecasts, severe weather warnings, and earthquake information from Malaysia's official API
- **Birthday Explorer** - Discover the number of births on any date in Malaysia with data spanning 1920–2022
- **Data Catalogue** - Browse Malaysia's open data catalogue organized by demography, economy, households, and more
- **Immigration** - International arrivals to Malaysia by country with monthly trends and breakdowns
- **Fuel Price** - Historical RON95, RON97, and diesel prices with weekly change data and trend charts
- **Population** - State-level population statistics with bar charts, sorting, and year-by-year comparison
- **Exchange Rate** - BNM exchange rates for MYR against 15+ currencies with trend visualization
- **CPI & Inflation** - Consumer Price Index and inflation rate trends
- **COVID-19** - Case statistics by state with new/active/recovered/imported case tracking
- **Crime Statistics** - Crime data by category with yearly trends and national summary
- **Transport Ridership** - Public transit ridership for LRT, MRT, Monorail, KTM, and ETS services
- **Births & Deaths** - Annual demographic trends with births vs deaths line charts and natural growth analysis
- **Marriage Statistics** - Marriage trends with annual counts, rates, and peak year analysis
- **Household Income** - State-level income comparison with mean/median income, expenditure, and interactive sorting
- **GDP & GNI** - Economic growth with real GDP/GNI trends, per-capita analysis, and YoY growth %
- **Labour Market** - Monthly employment stats with color-coded unemployment rate and labour force breakdown
- **Blood Donations** - Daily donation tracking with rolling averages and monthly aggregation charts
- **Interest Rates** - OPR, base rate, FD, and savings rate trends with multi-line comparison
- **Electricity Consumption** - Sector-level consumption (residential, commercial, industrial) with trend charts
- **EPF Dividend** - Historical conventional and shariah dividend rates with trend visualization
- **Vehicle Registrations** - Monthly car registration trends with averages and peak analysis
- **Transit Tracker** - Real-time public transport tracking on an interactive OpenStreetMap with GTFS Realtime protobuf parsing, 15 transit agencies, vehicle route tracing with polyline trails, user geolocation, and 30-second auto-refresh

**New Device Tools (5 tools):**
- **Battery Info** - Real-time battery level, health, temperature, voltage, and charging status with auto-refresh
- **Display Info** - Screen resolution, density, refresh rates, and HDR capability detection
- **Storage Analyzer** - Visual storage breakdown by category with ring charts and RAM analysis
- **Bluetooth Scanner** - Discover nearby Bluetooth devices with RSSI, device type, and BLE/Classic support
- **GPS Location** - Live GPS tracking with coordinates, address, altitude, speed, and satellite info

**New Media & Image Tools (5 tools):**
- **Image Background Remover** - Remove backgrounds by color selection with adjustable threshold and side-by-side preview
- **Image Filters** - Apply visual filters and effects to images
- **Image Rotate/Flip** - Quick rotation (90°/180°/270°) and horizontal/vertical flip
- **Meme Generator** - Create memes with customizable text overlays on images
- **Screenshot Stitcher** - Combine multiple screenshots into a single long vertical image

**New Converters & Calculators (5 tools):**
- **Number Base Converter** - Convert between decimal, binary, hex, octal, and custom bases
- **Currency Converter** - Live exchange rates for 30+ currencies with conversion
- **Aspect Ratio Calculator** - Calculate and scale dimensions with common presets
- **Date Calculator** - Days between dates, add/subtract periods, and age calculation
- **Scientific Calculator** - Trigonometric, logarithmic, and advanced math functions with history

**New Productivity & Text Tools (4 tools):**
- **Random Generator** - Generate random numbers, strings, colors, UUIDs, and dice rolls
- **Text Case Converter** - Convert between uppercase, lowercase, title case, and more
- **Pomodoro Timer** - Time-boxed work/break intervals for focused productivity
- **Note Pad** - Quick and simple note-taking utility

**Network (1 tool):**
- **Network Stress Test** - Quick-access hub for network performance diagnostics

**New Singapore OpenData Tools (11 tools):**
- **SG Weather** - 24-hour, 2-hour area, and 4-day weather forecasts from NEA
- **SG Air Quality** - PSI, PM2.5, UV Index by region with color-coded indicators
- **SG Environment** - Real-time temperature, rainfall, humidity, wind from 50+ stations with bar charts
- **SG Heat Stress (WBGT)** - Wet Bulb Globe Temperature from 15 stations with heat stress levels
- **SG Lightning** - Real-time lightning strikes on an interactive OpenStreetMap
- **SG Flood Alerts** - Active flood warnings from PUB with severity and area info
- **SG Taxi Availability** - Live taxi positions on OpenStreetMap with regional summary
- **SG Carpark** - Real-time HDB carpark availability with search, sort, and occupancy bars
- **SG Traffic Cameras** - Live traffic camera images in grid with tap-to-expand
- **SG IP Applications** - Design and patent lookup by date from IPOS
- **SG Data Catalogue** - Browse 5,000+ datasets, each as a dedicated page with auto-charts, stats, search, filter, and sort

**New Ireland OpenData (1 tool):**
- **IE Data Catalogue** - Browse 21,900+ Irish government datasets with theme filters, dedicated viewer pages with auto-charts, and multi-format file parsing (XLSX, CSV, HTML, JSON) with WebView fallback

**New Sarawak OpenData (1 tool):**
- **Sarawak Data** - Browse 200+ Sarawak state government datasets with dedicated viewer pages and multi-format file parsing

**Universal Data File Parser:**
- Built-in parser handles XLSX (Excel), CSV, TSV, HTML tables, and JSON — no external dependencies
- XLSX parsing via Android's built-in ZIP + XML libraries
- HTML table extraction with tag stripping and entity decoding
- WebView fallback for web-based datasets that don't contain tabular data
- All parsed data displayed with auto-generated charts, summary statistics, search, filter, and sort

### V8.0 - Creative Media Tools & NFC 🎨🎞️📡

**New Media & Image Tools (3 tools):**
- **GIF Maker** - Create animated GIFs from selected images or convert video clips to GIF. Features configurable frame delay/FPS, adjustable output width, frame preview thumbnails, and video frame extraction (up to 30 frames)
- **Photo Collage** - Arrange 2-6 photos into beautiful layouts with 7 templates (2 Side, 2 Stack, 3 Left, 3 Top, 4 Grid, 3 Row, 6 Grid). Customizable padding, output size (540-2160px), and 8 background color options with center-crop fitting
- **Image Watermark** - Add text or image watermarks with full customization: text size/color/opacity/rotation, image scale/opacity, 9-point position grid, and tiled pattern mode for repeating watermarks across the entire image

**New Device Tool (1 tool):**
- **NFC Reader** - Read and display NFC tag contents with automatic tag type detection (MIFARE Classic/Ultralight, ISO 14443, FeliCa, ISO 15693), NDEF record parsing (URI, text, MIME), tag ID, technology list, writable status, max size, and copy-to-clipboard support

### V7.0 - Image & PDF Powerhouse 🖼️📄

**New Image & Media Tools (7 tools):**
- **Image Resizer** - Resize images to exact dimensions with 12 social media presets and 10 common size presets, aspect ratio locking
- **Image Cropper** - Crop images with 10 aspect ratio presets, visual overlay with rule-of-thirds grid and corner handles
- **Image Format Converter** - Convert between JPEG, PNG, WebP (Lossy/Lossless), and BMP with quality slider
- **EXIF Viewer** - View all EXIF metadata (camera, shooting settings, GPS, dates, copyright) organized by category
- **EXIF Remover** - Batch strip EXIF data from images for privacy protection
- **Color Picker** - Tap on any image to extract colors with HEX, RGB, RGBA, HSL values and color history
- **Favicon Generator** - Generate favicons in 15 standard sizes (16x16 to 512x512) for websites and apps

**New PDF Tools (3 tools):**
- **PDF Split** - Split PDFs by page range or select individual pages with visual thumbnails
- **PDF to Image** - Extract pages as PNG/JPEG at configurable DPI (72-300)
- **PDF Password** - Add 128-bit encryption or remove password protection with configurable permissions

### V6.0 - PDF & Document Tools Enhanced 📄

**PDF Merge Improvements:**
- Multi-select PDF import - select multiple PDFs at once instead of adding one by one
- Drag-to-reorder - long press and drag to rearrange PDF order
- Full page preview - swipe through all pages before saving the merged PDF

**Image to PDF Improvements:**
- **Image Rotation** - rotate any image 90°/180°/270° before conversion with one tap
- **Visual Crop Editor** - crop images with draggable corner handles, dark overlay preview, and rule-of-thirds grid
- Drag-to-reorder - long press and drag to rearrange image order
- Full page preview - swipe through all pages with settings applied before exporting
- Preview shows all pages (not just the first) with page counter and swipe navigation

### Real-time CDN Monitoring Revolution 🚀
The CDN & Cloud Latency Test has been completely redesigned with professional-grade features:

**🎯 Real-time Line Graphs**
- Multi-color line charts showing latency trends over time
- Support for monitoring 35+ regions simultaneously
- Auto-scaling Y-axis with grid lines
- Time-series X-axis with mm:ss timestamps
- Smooth animations and responsive updates

**📊 Continuous Monitoring**
- Configurable ping intervals (1s, 2s, or 5s)
- Background service support for uninterrupted testing
- Add/remove regions on-the-fly without stopping monitoring
- Batch monitoring for all regions in a provider

**📈 Advanced Statistics**
- **Per-region statistics cards** with color-coded latency badges:
  - 🟢 Green: < 50ms (Excellent)
  - 🟡 Yellow: 50-100ms (Good)
  - 🟠 Orange: 100-200ms (Fair)
  - 🔴 Red: ≥ 200ms (Poor)
- **Comprehensive metrics**: Sent count, Packet loss %, Min/Max/Avg latency, Jitter
- **Historical tracking**: Last 30 data points per region
- **Graph legend**: Color-coded region identification

**🌐 Edge Detection**
- Automatically detects your CDN edge location
- Shows Cloudflare, CloudFront, and Alibaba Cloud connections
- Visual provider logos for easy identification

**✨ UI Improvements**
- Provider logos for all 14 cloud providers
- Sort by latency, name, or location
- Search and filter across 311+ regions
- Export results for documentation
- Responsive Material Design 3 interface

---

## 📸 Screenshots

> _Screenshots are pending the V10 design refresh — drop the new captures into `docs/screenshots/` and link them here:_
>
> ```markdown
> <div align="center">
>   <img src="docs/screenshots/home.png" width="240" />
>   <img src="docs/screenshots/cdn-latency.png" width="240" />
>   <img src="docs/screenshots/settings.png" width="240" />
> </div>
> ```

---

## 🚀 Getting Started

### Prerequisites

- **Android Studio:** Meerkat (2024.3) or later recommended
- **Android SDK:** API 24 (Android 7.0) or higher
- **AGP:** 9.1.0+
- **Kotlin:** 2.3.20+
- **Gradle:** 8.0+
- **JDK:** Java 11 or higher

### Installation

1. **Clone the repository:**

```bash
git clone https://github.com/osscv/CV-Toolkit.git
cd CVToolkit
```

2. **Open the project in Android Studio**
   - Select "Open an Existing Project"
   - Navigate to the cloned directory
   - Android Studio will automatically detect the project

3. **Sync Gradle dependencies**
   - Android Studio will prompt to sync
   - Or manually: File → Sync Project with Gradle Files

4. **Configure AdMob (Optional)**
   - The app includes AdMob integration
   - AdMob ID is in `AndroidManifest.xml`
   - Replace with your own ID if publishing your own version

5. **Build and run**
   - Connect your Android device or start an emulator
   - Click Run (▶) or press Shift+F10

### Build Commands

**Debug Build:**
```bash
./gradlew assembleDebug
```

**Release Build:**
```bash
./gradlew assembleRelease
```

**Install on Device:**
```bash
./gradlew installDebug
```

**Run Tests:**
```bash
./gradlew test
```

**APK Location:**
- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/app-release.apk`

---

## 📦 Features Breakdown

### IP Lookup with History
- Lookup your current IP or any custom IP address
- Get detailed information: location, ISP, ASN, timezone
- History tracking with timestamps stored locally
- Uses dkly DATAHUB API

### CDN & Cloud Latency Testing - **Advanced Real-time Monitoring**

**Comprehensive Provider Coverage:**
- **311+ Test Regions** across **14 Major Providers**:
  - Azure (46 regions)
  - AWS (30+ regions)  
  - Google Cloud Platform (40+ regions)
  - Alibaba Cloud (23 regions)
  - Oracle Cloud (20 regions)
  - DigitalOcean (15 regions)
  - Vultr (32 regions)
  - Linode (12 regions)
  - Lightsail (11 regions)
  - Hetzner (6 regions)
  - OVH (17 regions)
  - Fastly (18 regions)
  - Tencent Cloud (15 regions)
  - Huawei Cloud (12 regions)

**Edge Location Detection:**
- Automatically detects your nearest CDN edge location
- Shows Cloudflare, CloudFront, and Alibaba Cloud edge servers
- Displays real-time connection information

**Real-time Monitoring & Graphing:**
- **Live Line Graph** - Visualize latency trends over time with multi-color line charts
- **Continuous Ping Mode** - Monitor multiple regions simultaneously
- **Configurable Intervals** - Choose between 1s, 2s, or 5s ping intervals
- **Multi-region Tracking** - Monitor up to 35+ regions at once with color-coded lines
- **Auto-scaling Graph** - Dynamic Y-axis scaling based on latency values
- **Time-series Display** - X-axis shows timestamps with mm:ss format

**Detailed Statistics:**
- **Current Latency** - Real-time latency with color indicators (Green < 50ms, Yellow < 100ms, Orange < 200ms, Red ≥ 200ms)
- **Min/Max/Avg** - Comprehensive latency statistics
- **Jitter** - Network stability measurement (variance between consecutive pings)
- **Packet Loss** - Percentage of failed ping attempts
- **Sent Count** - Total number of ping attempts
- **Historical Data** - Stores last 30 data points per region

**Advanced Features:**
- **Provider Logos** - Visual provider identification with official logos
- **Sort Options** - Sort by latency, name, or location
- **Search & Filter** - Quickly find specific regions
- **Export Results** - Share and save test results
- **Add/Remove Monitoring** - Dynamically add or remove regions from monitoring
- **Background Service** - Continue testing even when app is minimized
- **Statistics Cards** - Individual stat cards for each monitored region with color-coded badges

### Network Scanner (Device Discovery)
- **Fast Network Scanning** - Discover all devices on your local network
- **MAC Address Lookup** - Identify device manufacturers automatically
- **Parallel Scanning** - Multi-threaded scanning for faster results
- **Device Details** - IP address, MAC address, hostname, and manufacturer
- **Export Functionality** - Save device list for documentation
- **Real-time Updates** - See devices as they're discovered

### Port Scanner
- **Comprehensive Port Scanning** - Scan any port range (1-65535)
- **Common Ports Preset** - Quick scan of well-known service ports
- **Service Detection** - Automatic identification of services (HTTP, FTP, SSH, etc.)
- **Parallel Scanning** - Multi-threaded for faster results
- **Timeout Configuration** - Adjustable connection timeouts
- **Export Results** - Save scan reports for security audits

### QR/Barcode Scanner
- **ML Kit Powered** - Google's machine learning for accurate scanning
- **Multiple Format Support** - QR Code, EAN-8, EAN-13, UPC-A, UPC-E, Code 39, Code 93, Code 128, ITF, Codabar, Aztec, Data Matrix, PDF417
- **Real-time Camera Preview** - Instant scanning with visual feedback
- **Automatic Detection** - No need to press buttons, auto-detects codes
- **Smart Actions** - Automatic URL opening, contact adding, Wi-Fi connecting
- **Scan History** - Keep track of all scanned codes with timestamps
- **Copy to Clipboard** - One-tap copy of scanned content
- **Flashlight Support** - Scan in low-light conditions

---

## 💡 Use Cases & Examples

### For Network Administrators
1. **Network Troubleshooting**
   - Use Ping Test to verify host connectivity
   - Run Traceroute to identify routing issues
   - Scan network with Device Discovery to find unauthorized devices
   - Check DNS resolution with DNS Lookup

2. **Infrastructure Monitoring**
   - Monitor cloud regions with CDN Latency Test's continuous mode
   - Track jitter and packet loss for quality assessment
   - Export results for reports and documentation

3. **Security Audits**
   - Scan ports to identify exposed services
   - Check SSL certificates for expiration and validity
   - Verify DNS records and configurations

### For Cloud Engineers
1. **Multi-region Latency Testing**
   - Test all AWS/Azure/GCP regions simultaneously
   - Compare provider performance from your location
   - Identify optimal regions for deployment

2. **Performance Monitoring**
   - Set up continuous monitoring for critical regions
   - Track latency trends over time with graphs
   - Analyze jitter and stability metrics

### For Developers
1. **API Development**
   - Test HTTP headers and responses
   - Build custom HTTP requests with headers
   - Decode JWT tokens for debugging
   - Generate test data (UUIDs, passwords, QR codes)

2. **Data Processing**
   - Encode/decode Base64 strings
   - Calculate hash values (MD5, SHA)
   - Convert between formats (hex, binary, ASCII)
   - Parse and analyze user agent strings

### For Students & Learning
- Visualize how network routing works (Traceroute)
- Understand DNS resolution process
- Learn about subnet calculations and IP addressing
- Explore different encoding formats
- See real-time network latency across the globe

---

## 🔒 Privacy & Security

**Your Privacy Matters:**
- ✅ All data processing is performed **locally on your device**
- ✅ **No data is uploaded** to our servers or any external servers
- ✅ Network scans, device information, and lookup results stay on your phone only
- ✅ IP lookup history is stored locally only
- ✅ No personal data collection, storage, or transmission
- ✅ Transparent and well-documented

**Permissions Used:**
- `INTERNET` - Required for network diagnostics, IP lookups, DNS queries
- `ACCESS_NETWORK_STATE` - Detect current network configuration
- `CAMERA` - QR/Barcode scanning and camera info (optional feature)
- `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_DATA_SYNC` - Background speed tests and continuous monitoring
- `POST_NOTIFICATIONS` - Speed test progress notifications
- `ACCESS_WIFI_STATE` / `CHANGE_WIFI_STATE` - WiFi analysis features
- `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` - Location-based network features
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` - Sustained speed testing
- `NFC` - NFC tag reading (optional feature)

**Advertising:**
- This app displays ads via Google AdMob
- Google may collect data for personalized advertising
- Refer to [Google's Privacy Policy](https://policies.google.com/privacy) for details

---

## ⚠️ Responsible Use

This app is designed for **legitimate network diagnostics, troubleshooting, and educational purposes only**.

**Please Note:**
- Only use network scanning tools on networks and devices you **own** or have **explicit permission** to test
- Unauthorized network scanning or port scanning may be **illegal** in your jurisdiction
- The app is provided "as is" without warranty
- The developer is not responsible for misuse or any legal issues arising from use of this app

---

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

### Contribution Guidelines

1. Fork the project
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

### ⚠️ Important Note for Contributors

**By contributing to this project, you agree to:**
- Maintain all existing author attributions and copyright notices
- Not remove or modify credits to the original author (Khoo Lay Yang)
- Keep the "About Author" section intact in the application
- Preserve all copyright and attribution information in documentation

All contributions will be reviewed to ensure compliance with these requirements.

---

## 📝 License & Attribution

Copyright © 2024-2026 Khoo Lay Yang. All Rights Reserved.

This project is proprietary software. Unauthorized copying, distribution, modification, or use of this software is prohibited without explicit permission from the author.

### ⚠️ Attribution Requirements

**IMPORTANT:** If you use, fork, or modify this repository, you **MUST**:

1. **Keep all author attribution intact** - Do not remove or modify:
   - The copyright notice "Copyright © 2024-2026 Khoo Lay Yang"
   - Author information (Name: Khoo Lay Yang, Website: www.dkly.net)
   - "Made with ❤️ by Khoo Lay Yang" footer in the README
   - Author credits in the app's "About Author" section

2. **Maintain the original author credits** in any:
   - Documentation files (README, LICENSE, etc.)
   - Source code headers or comments
   - Application UI/About screens
   - Derivative works or forks

3. **Give appropriate credit** when:
   - Sharing or distributing the app
   - Creating derivative works
   - Using code snippets or substantial portions
   - Publishing modified versions

**Removal or modification of author attribution is strictly prohibited and constitutes a violation of the copyright.**

For commercial use, licensing inquiries, or permission requests, please contact: www.dkly.net

---

## 👨‍💻 Author

**Khoo Lay Yang**

- Website: [www.dkly.net](https://www.dkly.net)

---

## 📞 Support

Feel free to open an issue on GitHub for:
- 🐛 Bug reports
- 💡 Feature requests or suggestions
- ❓ Questions about functionality
- 🔧 Technical issues

---

## 🙏 Acknowledgments

- [Jetpack Compose](https://developer.android.com/jetpack/compose) - Modern Android UI toolkit
- [Retrofit](https://square.github.io/retrofit/) - Type-safe HTTP client
- [ZXing](https://github.com/zxing/zxing) - QR code generation
- [ML Kit](https://developers.google.com/ml-kit) - Barcode scanning
- [CameraX](https://developer.android.com/training/camerax) - Camera API
- [Coil](https://coil-kt.github.io/coil/) - Image loading with SVG support
- [OkHttp](https://square.github.io/okhttp/) - HTTP client
- [AndroidX ExifInterface](https://developer.android.com/jetpack/androidx/releases/exifinterface) - EXIF metadata reading
- [PDFBox Android](https://github.com/TomRoush/PdfBox-Android) - PDF encryption and password protection
- [dkly DATAHUB](https://data.dkly.net) - IP lookup services
- All cloud providers for their global infrastructure

---

## 📊 Statistics

- **133 Professional Tools** organized into 5 tabs (All / Network / Utility / Device / OpenData with 4 countries) across **139 screens**
- **311+ Global Test Regions** across 14 cloud/CDN providers
- **Real-time Monitoring** with live graphs and continuous ping
- **Advanced Analytics** — Min / Max / Avg / Jitter / Packet Loss tracking
- **100% Local Processing** — Your data stays on your device
- **Custom Diagnostic-Tool UI** built on Material Design 3 + Jetpack Compose (V10 refresh)
- **Zero Server Dependencies** for core functionality
- **Multi-region Support** — Monitor 35+ regions simultaneously
- **Professional Grade** — Network diagnostics used by IT professionals
- **Privacy First** — No data collection, no tracking
- **18 Languages** — Global localization support

---

<div align="center">
  
**Made with ❤️ by Khoo Lay Yang**

⭐ Star this repo if you find it useful!

</div>

