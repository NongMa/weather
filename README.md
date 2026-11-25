# 天气预报

一款 Android 天气应用，采用 iOS 27 风格的液态玻璃（Liquid Glass）界面，数据来自 Open-Meteo。

## 功能特性

- **液态玻璃 UI**：玻璃卡片、模糊背景、渐变描边、镜面高光；深浅主题自动适配
- **条件化动态天空**：晴/多云/阴/雾/雨/雪/雷暴对应不同渐变、光晕与粒子动画（雨丝、雪花、雾带、云朵、星空、闪电）
- **iOS 27 预报版式**：居中大温度、HIGHLIGHTS 摘要卡、小时预报 + 10 日预报
- **三种预报视图**：实况（温度/天气）、降水（概率柱 + 降水量）、风力（风速折线 + 区间条），小时与 10 日列表同步切换
- **城市管理**：默认当前位置；自行搜索添加城市/县区/乡镇；全屏管理页可切换、删除；标签栏左右滚动；城市列表持久化
- **实时定位**：进入应用时获取当前位置一次（不持续后台定位），Geocoder 后台反查地名
- **全面屏**：edge-to-edge、状态栏/导航栏透明、刘海/挖孔区域绘制
- **自适应图标**：玻璃天气球设计，支持主题图标（Android 13+）与旧设备位图

## 技术栈

- Kotlin + Jetpack Compose + Material 3
- Kotlin Coroutines / Flow
- [Haze](https://github.com/chrisbanes/haze) 实现玻璃模糊
- Retrofit + OkHttp + kotlinx.serialization
- Google Play Services Location（FusedLocationProviderClient）
- Gradle 9.6 / AGP 9.5.2 / compileSdk 37 / minSdk 24

## 数据来源

- 天气：[Open-Meteo Forecast API](https://open-meteo.com/en/docs)（`current` 为 15 分钟实时分析值）
- 城市搜索：Open-Meteo Geocoding API + 内置中国行政区划精简索引（名称/坐标，来源 DataV GeoAtlas）
- 地点名：`Geocoder` 反向地理编码（失败时回退为「当前位置」）

## 构建与安装

```powershell
# Debug
.\gradlew.bat :app:assembleDebug
adb install -r app\build\outputs\apk\debug\app-debug.apk

# Release（使用 debug keystore 签名，可直接覆盖 debug 安装）
.\gradlew.bat :app:assembleRelease
adb install -r app\build\outputs\apk\release\app-release.apk
```

> Release 构建复用了 AGP 的 debug 签名配置，便于本地覆盖安装；R8 全量压缩已开启，**不要**给 `optimization` 设置 `packageScope`，否则 Kotlin stdlib 会被部分改写导致启动崩溃。

## CI / Release

[.github/workflows/release.yml](.github/workflows/release.yml)：

- push 到 `main`：构建并归档 APK artifact
- push `v*` tag：构建并自动创建 GitHub Release，附加 `app-release.apk`

最新发布：[v1.0.1](https://github.com/NongMa/weather/releases/tag/v1.0.1)

## 目录结构

```text
app/src/main/java/com/nongma/weather/
├── MainActivity.kt            # 入口，edge-to-edge
├── WeatherScreen.kt           # 主界面（玻璃 UI、动态天空、预报卡片）
├── WeatherViewModel.kt        # 城市管理与定位状态
├── CityManagerScreen.kt       # 全屏城市管理（搜索/添加/删除）
├── data/                      # API、模型、仓库、中国地点索引
├── location/LocationProvider.kt
└── ui/theme/                  # 颜色、主题、字体
```
