# CV Toolkit

**🌐 [English README](README.md)**

<div align="center">
  <img src="app/src/main/res/drawable/logo.png" alt="CV Toolkit Logo" width="200"/>

  **一款全面的 Android 网络诊断与实用工具集**

  [![Android](https://img.shields.io/badge/平台-Android-green.svg)](https://www.android.com/)
  [![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg)](https://android-arsenal.com/api?level=24)
  [![Kotlin](https://img.shields.io/badge/Kotlin-2.3-blue.svg)](https://kotlinlang.org)
  [![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-最新版-blue.svg)](https://developer.android.com/jetpack/compose)

  ---

  **⚠️ 重要提示：** 如果您使用此仓库，**必须**保留所有作者署名信息。
  **请勿删除**作者信息。详见[许可证与署名](#-许可证与署名)。

  **版权所有 © 2024 [Khoo Lay Yang](https://www.dkly.net) - 保留所有权利**
</div>

---

## 📱 关于

**CV Toolkit** 是一款专业级多功能 Android 应用，专为网络诊断、云基础设施监控、系统信息查看和开发者实用工具而设计。采用现代 Android 技术（Kotlin 2.3 + Jetpack Compose）构建，在一个精美设计的应用中提供 **63 个强大工具，覆盖 66 个屏幕**。

**适用人群：**
- 👨‍💻 **网络管理员** - 全面的网络诊断与监控工具
- 🌐 **云工程师** - 测试 14 个云服务商 311+ 个区域的连接性，支持实时图表
- 🔐 **安全专家** - 端口扫描、SSL 检查和安全分析工具
- 💻 **软件开发者** - 编码、哈希、JWT 解码和开发实用工具
- 🎓 **学生与教育者** - 通过实操工具学习网络概念
- 🏠 **家庭用户** - 排查网络问题并监控连接状况

**为什么选择 CV Toolkit？**
- ✅ 所有处理均在您的设备上 **100% 本地完成**
- ✅ 您的数据 **绝不离开手机** - 完全保障隐私
- ✅ 无需注册或登录账号
- ✅ 大多数工具支持离线使用（网络工具需要网络连接）
- ✅ 专业级精确度和性能
- ✅ 精美的现代 Material Design 3 界面
- ✅ 定期更新，持续添加新功能

---

## 🌟 功能亮点

### 🚀 **高级 CDN 与云延迟测试**
CV Toolkit 的核心功能 - 专业级网络监控工具：
- **实时折线图** 支持多区域可视化
- **持续监控** 可配置间隔（1秒/2秒/5秒）
- **全面统计**：最小值、最大值、平均值、抖动和丢包率
- **311+ 个测试区域** 覆盖 14 个全球云服务商
- **同时监控 35+ 个区域** 并用颜色编码追踪
- **边缘节点检测** 支持 Cloudflare、CloudFront 和阿里云
- **后台服务** 支持不间断监控

### 📱 **63 个专业工具**
涵盖网络诊断、开发工具、PDF 管理、代码编辑和系统分析，一应俱全。

### 🔒 **隐私优先设计**
- 零数据收集
- 不上传服务器
- 100% 本地处理
- 公开透明

---

## ✨ 功能详情

### 🌐 网络工具（16 个）

- **速度测试** - 全面的网络速度测试，支持下载/上传速度、延迟、抖动测量和历史记录。前台服务支持不间断测试
- **Ping 测试** - ICMP Ping 测试，支持包数量配置、超时设置和详细统计（最小/最大/平均/丢包率）
- **持续 Ping** - 多主机实时 Ping 监控，支持实时折线图、颜色编码，用于持续连接分析
- **路由追踪** - 可视化网络路径追踪，显示所有跳转点的延迟测量和地理信息
- **端口扫描** - 扫描任意主机的开放端口，支持服务检测和并行扫描
- **网络扫描（设备发现）** - 发现网络中的所有连接设备，支持 MAC 地址查找和制造商识别
- **IP 查询** - 获取任意 IP 地址的详细地理位置、ISP、安全标志（VPN/代理/TOR 检测）和运营商信息，支持本地历史记录
- **DNS 查询** - 全面的 DNS 记录查询（A、AAAA、CNAME、MX、TXT、NS、SOA、PTR），支持自定义 DNS 服务器
- **DNS 基准测试** - 比较 10 个热门 DNS 提供商（Google、Cloudflare、Quad9、OpenDNS、AdGuard 等）的响应速度，支持图表可视化、按速度/名称/可靠性排序、可配置查询次数
- **子网计算器** - 高级 IPv4/IPv6 子网计算，支持 CIDR 表示法、网络/广播地址、可用主机范围
- **SSL/TLS 检查** - 详细的 SSL 证书检查，包括有效期、颁发者信息、证书链、加密算法和过期提醒
- **Whois 查询** - 获取域名注册信息
- **HTTP 响应头查看器** - 查看 HTTP 响应头
- **自定义请求构建器** - 构建并发送自定义 HTTP/CURL 请求，支持自定义头部和请求体
- **WiFi 分析器** - 分析 WiFi 信号强度、频道和网络详情
- **CDN 与云延迟测试** - **支持实时图表的实时监控** - 测试 14 个主要服务商 311+ 个区域的延迟：
  - AWS（亚马逊云服务）
  - GCP（谷歌云平台）
  - Azure（微软 Azure）
  - Oracle Cloud（甲骨文云）
  - Alibaba Cloud（阿里云）
  - DigitalOcean
  - Linode (Akamai)
  - Vultr
  - Hetzner
  - OVH
  - Tencent Cloud（腾讯云）
  - Huawei Cloud（华为云）
  - Lightsail
  - Fastly

### 🛠️ 实用工具（42 个工具，分为 9 个子类别）

**🔄 编码与解码（7 个工具）：**
- **Base64 编解码** - 编码/解码 Base64 字符串
- **URL 编解码** - 编码/解码 URL 字符串
- **进制转换** - 文本与二进制/十六进制/八进制互转
- **十六进制编码** - 文本与十六进制互转
- **ASCII 转换** - 文本与 ASCII 值互转
- **JWT 解码** - 解码并检查 JWT 令牌
- **图片 Base64** - 图片与 Base64 编码互转

**🔐 安全与密码（6 个工具）：**
- **哈希生成器** - 生成 MD5、SHA-1、SHA-256、SHA-384、SHA-512 哈希值
- **凯撒密码** - 使用凯撒密码、ROT13、ROT47 加密/解密
- **摩尔斯电码** - 文本与摩尔斯电码互转
- **密码生成器** - 生成安全的强密码，支持自定义选项
- **密码强度检查** - 分析密码熵值、字符组成、破解时间估算，检测常见模式和序列，提供详细安全检查和可视化强度指示
- **文件哈希计算器** - 计算文件的 MD5、SHA、CRC32 哈希值及元数据

**📱 二维码与条形码（3 个工具）：**
- **二维码生成器** - 从文本、URL、联系人等生成二维码
- **条形码生成器** - 生成多种条形码格式（EAN、UPC、Code 128 等）
- **二维码/条形码扫描** - 使用摄像头扫描和解码二维码与条形码

**🌐 Web 与 API（4 个工具）：**
- **API 测试器** - 保存、组织和执行 API 请求，支持自定义头部和请求体
- **User Agent 解析器** - 解析和分析浏览器 User-Agent 字符串
- **Robots.txt 分析器** - 获取并解析任意域名的 robots.txt 文件
- **站点地图查看器** - 解析和浏览 XML 站点地图

**📄 PDF 与文档（5 个工具）：**
- **PDF 查看器** - 查看和浏览 PDF 文档
- **PDF 合并** - 将多个 PDF 文件合并为一个
- **图片转 PDF** - 将图片转换为 PDF 文档
- **PDF 压缩** - 减小 PDF 文件大小
- **幻灯片转 PDF** - 将演示文稿（PPT）转换为 PDF

**✏️ 编辑器（4 个工具）：**
- **文本编辑器** - 在设备上创建和编辑文本文件
- **Markdown 编辑器** - 编写 Markdown 并实时预览
- **Markdown 预览** - 渲染和预览 Markdown 内容
- **SVG 查看器与编辑器** - 查看和编辑 SVG 矢量图形

**🎨 媒体与颜色（3 个工具）：**
- **图片压缩** - 在保持质量的同时减小图片文件大小
- **颜色转换器** - HEX、RGB、HSL 和 CMYK 颜色格式之间的转换
- **调色板生成器** - 生成和探索调色板

**🔢 转换器与计算器（5 个工具）：**
- **IP 计算器** - 不同 IP 格式之间的转换
- **单位转换器** - 转换长度、重量、温度和数据大小
- **世界时钟** - 查看全球时区的当前时间
- **Unix 时间戳转换器** - Unix 时间戳与可读日期之间的转换
- **UUID 生成器** - 生成 UUID v1、v4 和 v5

**📝 文本与杂项（5 个工具）：**
- **文本计数** - 统计单词、字符、行数和段落数
- **文本对比** - 比较两段文本并高亮差异
- **Lorem Ipsum 生成器** - 生成设计和开发用的占位符文本
- **打字测试** - 练习和测量打字速度与准确率
- **秒表** - 计时器，支持分圈功能

### 📱 设备工具（5 个）

- **设备信息** - 完整的系统信息（CPU、内存、存储、操作系统等）
- **DRM 与编解码信息** - 查看 DRM 系统、视频/音频编解码器和支持的格式
- **摄像头信息** - 详细的摄像头规格和功能参数
- **安全审计** - 全面的设备安全检查和漏洞评估
- **传感器仪表盘** - 所有设备传感器的实时读数（加速度计、陀螺仪、距离传感器等）

---

## 🏗️ 技术栈

- **编程语言：** Kotlin 2.3（100%）
- **UI 框架：** Jetpack Compose BOM 2026.03（现代声明式 UI + Material Design 3）
- **架构：** MVVM + Repository 模式，清晰的关注点分离
- **网络层：**
  - Retrofit 3.0（类型安全的 REST API 调用）
  - OkHttp 5.3（HTTP 客户端，支持拦截器和日志）
  - Gson 2.13（JSON 解析与序列化）
  - 协程（异步编程）
- **相机与机器学习：**
  - CameraX 1.5（现代相机 API，支持预览和分析）
  - ML Kit Barcode Scanning 17.3（设备端二维码/条形码识别）
  - ZXing Core 3.5（二维码和条形码生成）
- **数据可视化：**
  - Canvas API（自定义折线图和图表）
  - 基于 Compose 重组的实时渲染
- **后台处理：**
  - 前台服务（速度测试和持续监控）
  - 协程（并行处理和异步操作）
- **数据持久化：**
  - SQLite（IP 查询历史存储）
  - SharedPreferences（语言设置、广告使用追踪）
- **导航：** Jetpack Navigation Compose 2.9，基于密封类的类型安全路由（66 个目的地）
- **最低 SDK：** Android 7.0（API 24）- 广泛设备兼容
- **目标 SDK：** Android 16（API 36）- 最新 Android 特性
- **编译 SDK：** 36（Android 16）
- **图片加载：** Coil 2.7（高效图片加载，支持 SVG 和缓存）
- **广告：** Google AdMob（非侵入式横幅广告，基于使用量的插页式和激励广告）

## 🎨 UI/UX 特性

- **Material Design 3** - 遵循 Google 最新设计规范的现代精美界面
- **动态主题** - 自适应系统主题（亮色/暗色模式）
- **响应式布局** - 适配所有屏幕尺寸和方向
- **流畅动画** - 精心打磨的过渡和交互效果
- **颜色编码指示** - 延迟质量和状态的视觉反馈
- **实时更新** - 无需刷新页面的实时数据可视化
- **服务商标识** - 使用官方品牌 Logo 进行视觉识别
- **直观导航** - 逻辑清晰、易于使用的界面
- **复制与分享** - 一键操作结果和数据
- **搜索与筛选** - 快速在 311+ 个区域中查找所需内容

---

## 🌍 多语言支持

CV Toolkit 支持 18 种语言：

| 语言 | 代码 | 语言 | 代码 |
|------|------|------|------|
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

## 🚀 快速开始

### 环境要求

- **Android Studio：** Ladybug (2024.2) 或更高版本
- **Android SDK：** API 24（Android 7.0）或更高
- **Kotlin：** 2.3+
- **Gradle：** 8.0+
- **JDK：** Java 11 或更高

### 安装步骤

1. **克隆仓库：**

```bash
git clone https://github.com/osscv/CV-Toolkit.git
cd CVToolkit
```

2. **在 Android Studio 中打开项目**
   - 选择"打开现有项目"
   - 导航到克隆的目录

3. **同步 Gradle 依赖**
   - Android Studio 会提示同步
   - 或手动：文件 → 同步项目与 Gradle 文件

4. **配置 AdMob（可选）**
   - AdMob ID 位于 `AndroidManifest.xml`
   - 如果发布自己的版本，请替换为您的 ID

5. **构建并运行**
   - 连接 Android 设备或启动模拟器
   - 点击运行（▶）或按 Shift+F10

### 构建命令

**调试版本：**
```bash
./gradlew assembleDebug
```

**发布版本：**
```bash
./gradlew assembleRelease
```

**安装到设备：**
```bash
./gradlew installDebug
```

**运行测试：**
```bash
./gradlew test
```

**APK 位置：**
- 调试版：`app/build/outputs/apk/debug/app-debug.apk`
- 发布版：`app/build/outputs/apk/release/app-release.apk`

---

## 🔒 隐私与安全

**您的隐私至关重要：**
- ✅ 所有数据处理均在 **设备本地** 完成
- ✅ **不上传数据** 到我们的服务器或任何外部服务器
- ✅ 网络扫描、设备信息和查询结果仅保存在您的手机上
- ✅ IP 查询历史仅存储在本地
- ✅ 不收集、不存储、不传输个人数据
- ✅ 开源且透明

**使用的权限：**
- `INTERNET` - 网络诊断、IP 查询、DNS 查询所需
- `ACCESS_NETWORK_STATE` - 检测当前网络配置
- `CAMERA` - 二维码/条形码扫描和摄像头信息（可选功能）
- `FOREGROUND_SERVICE` / `FOREGROUND_SERVICE_DATA_SYNC` - 后台速度测试和持续监控
- `POST_NOTIFICATIONS` - 速度测试进度通知
- `ACCESS_WIFI_STATE` / `CHANGE_WIFI_STATE` - WiFi 分析功能
- `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` - 基于位置的网络功能
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` - 持续速度测试

**广告说明：**
- 本应用通过 Google AdMob 展示广告
- Google 可能会收集数据用于个性化广告
- 详情请参阅 [Google 隐私政策](https://policies.google.com/privacy)

---

## ⚠️ 负责任使用

本应用专为 **合法的网络诊断、故障排查和教育目的** 而设计。

**请注意：**
- 仅在您 **拥有** 或获得 **明确许可** 的网络和设备上使用网络扫描工具
- 未经授权的网络扫描或端口扫描在您的管辖范围内可能是 **违法的**
- 本应用按"原样"提供，不附带任何保证
- 开发者对误用或因使用本应用而产生的任何法律问题不承担责任

---

## 🤝 贡献

欢迎贡献！请随时提交 Pull Request。

### 贡献指南

1. Fork 项目
2. 创建功能分支（`git checkout -b feature/AmazingFeature`）
3. 提交更改（`git commit -m 'Add some AmazingFeature'`）
4. 推送到分支（`git push origin feature/AmazingFeature`）
5. 创建 Pull Request

### ⚠️ 贡献者重要说明

**参与贡献即表示您同意：**
- 保留所有现有的作者署名和版权声明
- 不删除或修改原作者（Khoo Lay Yang）的署名信息
- 保持应用中"关于作者"部分完整
- 保留文档中的所有版权和署名信息

所有贡献将经过审核以确保符合上述要求。

---

## 📝 许可证与署名

版权所有 © 2024 Khoo Lay Yang。保留所有权利。

本项目为专有软件。未经作者明确许可，禁止未经授权的复制、分发、修改或使用。

### ⚠️ 署名要求

**重要：** 如果您使用、Fork 或修改此仓库，**必须**：

1. **保留所有作者署名** - 不得删除或修改：
   - 版权声明 "Copyright © 2024 Khoo Lay Yang"
   - 作者信息（姓名：Khoo Lay Yang，网站：www.dkly.net）
   - README 中的 "Made with ❤️ by Khoo Lay Yang"
   - 应用中"关于作者"部分的署名信息

2. **在以下内容中保留原始作者署名**：
   - 文档文件（README、LICENSE 等）
   - 源代码头部或注释
   - 应用 UI / 关于界面
   - 衍生作品或 Fork

3. **在以下情况下给予适当署名**：
   - 分享或分发本应用时
   - 创建衍生作品时
   - 使用代码片段或大量代码时
   - 发布修改版本时

**删除或修改作者署名严格禁止，构成侵犯版权。**

如需商业使用、许可咨询或权限申请，请联系：www.dkly.net

---

## 👨‍💻 作者

**Khoo Lay Yang**

- 网站：[www.dkly.net](https://www.dkly.net)

---

## 📞 技术支持

如有以下需求，欢迎在 GitHub 上提交 Issue：
- 🐛 Bug 报告
- 💡 功能建议
- ❓ 功能咨询
- 🔧 技术问题

---

## 🙏 致谢

- [Jetpack Compose](https://developer.android.com/jetpack/compose) - 现代 Android UI 工具包
- [Retrofit](https://square.github.io/retrofit/) - 类型安全的 HTTP 客户端
- [ZXing](https://github.com/zxing/zxing) - 二维码生成
- [ML Kit](https://developers.google.com/ml-kit) - 条形码扫描
- [CameraX](https://developer.android.com/training/camerax) - 相机 API
- [Coil](https://coil-kt.github.io/coil/) - 图片加载，支持 SVG
- [OkHttp](https://square.github.io/okhttp/) - HTTP 客户端
- [dkly DATAHUB](https://data.dkly.net) - IP 查询服务
- 所有云服务商提供的全球基础设施

---

## 📊 统计数据

- **63 个专业工具** 分为 3 大类别（9 个子类别），覆盖 **66 个屏幕**
- **311+ 个全球测试区域** 覆盖 14 个云/CDN 服务商
- **实时监控** 支持实时图表和持续 Ping
- **高级分析** - 最小/最大/平均/抖动/丢包率追踪
- **100% 本地处理** - 数据留在您的设备上
- **现代 UI** 采用 Material Design 3 和 Jetpack Compose
- **零服务器依赖** 核心功能完全本地化
- **同时监控 35+ 个区域**
- **专业级** 网络诊断工具
- **隐私优先** - 无数据收集，无追踪
- **18 种语言** 全球本地化支持

---

<div align="center">

**由 Khoo Lay Yang 用 ❤️ 制作**

⭐ 如果觉得有用，请给这个仓库点个 Star！

</div>
