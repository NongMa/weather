package com.nongma.weather

import android.Manifest
import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.rounded.Search
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Air
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.AcUnit
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeviceThermostat
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Thunderstorm
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nongma.weather.data.CurrentWeather
import com.nongma.weather.data.DailyWeather
import com.nongma.weather.data.WeatherResponse
import com.nongma.weather.location.LocationProvider
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import java.text.SimpleDateFormat
import java.util.Locale
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

internal val LocalWeatherHaze = staticCompositionLocalOf { HazeState() }

@Composable
fun WeatherScreen() {
    val context = LocalContext.current
    // WeatherViewModel needs the Application context to persist the user's city list;
    // the default Compose factory cannot supply it, so build an explicit factory.
    val viewModel: WeatherViewModel = viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return WeatherViewModel(
                    application = context.applicationContext as Application
                ) as T
            }
        }
    )
    val uiState by viewModel.uiState.collectAsState()
    val selectedCity by viewModel.selectedCity.collectAsState()
    val isFollowingLocation by viewModel.isFollowingLocation.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val userCities by viewModel.userCities.collectAsState()
    var showAddCityDialog by remember { mutableStateOf(false) }
    var cityToRemove by remember { mutableStateOf<City?>(null) }
    val dark = isSystemInDarkTheme()
    val hazeState = remember { HazeState() }

    // iOS 27 style: Conditions / Precipitation / Wind switcher over the forecast.
    var forecastViewIndex by rememberSaveable { mutableStateOf(0) }
    val forecastView = ForecastView.entries[
        forecastViewIndex.coerceIn(0, ForecastView.entries.size - 1)
    ]

    var hasLocationPermission by remember { mutableStateOf(context.hasLocationPermission()) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
    }

    fun requestLocationPermission() {
        permissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    LaunchedEffect(Unit) {
        if (!hasLocationPermission) {
            requestLocationPermission()
        }
    }

    // One-shot location: fetch the current position when entering the app (or when the
    // permission first becomes available / the user taps the location chip) — never as a
    // continuous background stream.
    val locationProvider = remember { LocationProvider(context) }

    fun fetchCurrentLocation() {
        locationProvider.fetchOnce { fix ->
            viewModel.onLocationFix(fix.latitude, fix.longitude, fix.placeName)
        }
    }

    LaunchedEffect(hasLocationPermission, isFollowingLocation) {
        if (hasLocationPermission && isFollowingLocation) {
            fetchCurrentLocation()
        }
    }

    // Re-check the permission when the app returns to the foreground; clean up on exit.
    val activity = context as? ComponentActivity
    DisposableEffect(activity) {
        val lifecycle = activity?.lifecycle
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasLocationPermission = context.hasLocationPermission()
            }
        }
        lifecycle?.addObserver(observer)
        onDispose {
            lifecycle?.removeObserver(observer)
            locationProvider.destroy()
        }
    }

    val weather = (uiState as? WeatherUiState.Success)?.weather
    val current = weather?.current
    val hours = remember(weather) {
        nextHours(hourly = weather?.hourly, currentTime = current?.time ?: "")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .hazeSource(state = hazeState)
    ) {
        CompositionLocalProvider(LocalWeatherHaze provides hazeState) {
            SkyBackground(
                code = current?.weatherCode ?: 0,
                isDay = current?.isDay ?: 1,
                dark = dark
            )

            PullToRefreshBox(
                isRefreshing = isRefreshing,
                onRefresh = viewModel::refresh,
                modifier = Modifier.fillMaxSize()
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 8.dp,
                        bottom = 28.dp
                    ),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    item {
                        if (uiState is WeatherUiState.Loading) {
                            WeatherHeaderSkeleton()
                        } else {
                            WeatherHeader(
                                current = current,
                                daily = weather?.daily
                            )
                        }
                    }
                    item {
                        CityPills(
                            cities = userCities,
                            selectedCity = selectedCity,
                            isFollowingLocation = isFollowingLocation,
                            onCitySelected = viewModel::selectCity,
                            onLocationSelected = {
                                if (hasLocationPermission) {
                                    viewModel.resumeLocationFollowing()
                                } else {
                                    requestLocationPermission()
                                }
                            },
                            onAddCity = { showAddCityDialog = true },
                            onRemoveCity = { cityToRemove = it }
                        )
                    }
                    if (!hasLocationPermission) {
                        item {
                            PermissionBanner(
                                modifier = Modifier.fillMaxWidth(),
                                onRequest = { requestLocationPermission() }
                            )
                        }
                    }
                    when (val state = uiState) {
                        is WeatherUiState.Loading -> Unit

                        is WeatherUiState.Error -> item {
                            ErrorState(message = state.message, onRetry = viewModel::refresh)
                        }

                        is WeatherUiState.Success -> {
                            item {
                                val highlights = remember(state.weather) {
                                    buildHighlights(
                                        current = state.weather.current,
                                        daily = state.weather.daily,
                                        hours = hours
                                    )
                                }
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    SectionLabel(text = "HIGHLIGHTS")
                                    HighlightsCard(items = highlights)
                                }
                            }
                            if (hours.isNotEmpty() || state.weather.daily != null) {
                                item {
                                    ForecastCard(
                                        hours = hours,
                                        daily = state.weather.daily,
                                        view = forecastView,
                                        onViewChange = { forecastViewIndex = it.ordinal }
                                    )
                                }
                            }
                            item { DetailGrid(weather = state.weather) }
                        }
                    }
                    item { Footer() }
                }
            }

            if (showAddCityDialog) {
                CityManagerScreen(
                    cities = userCities,
                    selectedCity = selectedCity,
                    isFollowingLocation = isFollowingLocation,
                    onDismiss = { showAddCityDialog = false },
                    onAdd = { city -> viewModel.addCity(city) },
                    onRemove = { city -> viewModel.removeCity(city) },
                    onSelect = { city -> viewModel.selectCity(city) },
                    onLocation = {
                        if (hasLocationPermission) {
                            viewModel.resumeLocationFollowing()
                        } else {
                            requestLocationPermission()
                        }
                        showAddCityDialog = false
                    },
                    search = { query -> viewModel.searchCities(query) }
                )
            }
        }
    }

    cityToRemove?.let { city ->
        AlertDialog(
            onDismissRequest = { cityToRemove = null },
            title = { Text(text = "移除城市") },
            text = { Text(text = "确定移除「${city.name}」吗？") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.removeCity(city)
                    cityToRemove = null
                }) {
                    Text(text = "移除")
                }
            },
            dismissButton = {
                TextButton(onClick = { cityToRemove = null }) {
                    Text(text = "取消")
                }
            }
        )
    }
}

private fun Context.hasLocationPermission(): Boolean =
    ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) ==
        PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

// region Sky background (condition + day/night + system dark mode)

private data class SkyPalette(
    val top: Color,
    val bottom: Color,
    val glowA: Color,
    val glowB: Color,
)

/** Background sky colours derived from the WMO weather code, like the iOS Weather app. */
private fun skyPalette(code: Int, isDay: Int): SkyPalette {
    val day = isDay == 1
    return when {
        code == 0 || code == 1 ->
            if (day) SkyPalette(Color(0xFF5AA8F8), Color(0xFF1F62D2), Color(0xFFA8E4FF), Color(0xFFFFD9A0))
            else SkyPalette(Color(0xFF10275A), Color(0xFF04091C), Color(0xFF2B4E96), Color(0xFF4A4282))

        code == 2 ->
            if (day) SkyPalette(Color(0xFF7EB0F2), Color(0xFF3772C7), Color(0xFFBFE2FF), Color(0xFFFFE0B2))
            else SkyPalette(Color(0xFF14294F), Color(0xFF050B1D), Color(0xFF2A4680), Color(0xFF453E78))

        code == 3 ->
            if (day) SkyPalette(Color(0xFF93A7BC), Color(0xFF5D7187), Color(0xFFC9D6E6), Color(0xFFE8D9C7))
            else SkyPalette(Color(0xFF2B3542), Color(0xFF0C1118), Color(0xFF3E4C5E), Color(0xFF40405A))

        code in 45..48 ->
            if (day) SkyPalette(Color(0xFF9FA8B4), Color(0xFF6B7484), Color(0xFFD2D8E0), Color(0xFFE4DDD2))
            else SkyPalette(Color(0xFF31363E), Color(0xFF101317), Color(0xFF454B55), Color(0xFF3C3C48))

        code in 51..67 || code in 80..82 ->
            if (day) SkyPalette(Color(0xFF7C8FA6), Color(0xFF46576E), Color(0xFFAFC4D8), Color(0xFFC7C2B4))
            else SkyPalette(Color(0xFF232D3A), Color(0xFF080C12), Color(0xFF36465C), Color(0xFF424158))

        code in 71..77 || code == 85 || code == 86 ->
            if (day) SkyPalette(Color(0xFFAFC6DE), Color(0xFF7089A6), Color(0xFFE3F1FF), Color(0xFFF3F7FC))
            else SkyPalette(Color(0xFF26313F), Color(0xFF0A0F16), Color(0xFF3A4E68), Color(0xFF414B60))

        code in 95..99 ->
            if (day) SkyPalette(Color(0xFF5B6478), Color(0xFF272C3B), Color(0xFF818CA6), Color(0xFFA98A6B))
            else SkyPalette(Color(0xFF1E212D), Color(0xFF06070C), Color(0xFF343850), Color(0xFF4A4468))

        else ->
            if (day) SkyPalette(Color(0xFF5AA8F8), Color(0xFF1F62D2), Color(0xFFA8E4FF), Color(0xFFFFD9A0))
            else SkyPalette(Color(0xFF10275A), Color(0xFF04091C), Color(0xFF2B4E96), Color(0xFF4A4282))
    }
}

@Composable
private fun SkyBackground(code: Int, isDay: Int, dark: Boolean) {
    val palette = remember(code, isDay) { skyPalette(code, isDay) }
    // System dark mode deepens the sky so the scene stays readable and moody.
    val top = if (dark) lerp(palette.top, Color.Black, 0.50f) else palette.top
    val bottom = if (dark) lerp(palette.bottom, Color.Black, 0.58f) else palette.bottom
    val glowAlpha = if (dark) 0.40f else 0.75f

    val transition = rememberInfiniteTransition(label = "sky")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 12_000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "phase"
    )

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(top, bottom)))
    ) {
        val density = LocalDensity.current
        val widthPx = with(density) { maxWidth.toPx() }
        val heightPx = with(density) { maxHeight.toPx() }
        val maxRadius = maxOf(widthPx, heightPx)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(palette.glowA.copy(alpha = glowAlpha), Color.Transparent),
                        center = Offset(widthPx * (0.18f + 0.64f * phase), heightPx * 0.16f),
                        radius = maxRadius * 0.78f
                    )
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(
                        colors = listOf(palette.glowB.copy(alpha = glowAlpha * 0.8f), Color.Transparent),
                        center = Offset(widthPx * (0.84f - 0.56f * phase), heightPx * 0.84f),
                        radius = maxRadius * 0.72f
                    )
                )
        )

        // Animated weather layers driven by the current condition.
        WeatherLayers(code = code, isDay = isDay, dark = dark)
    }
}

// region Animated weather layers (rain / snow / fog / clouds / stars / lightning)

/** Deterministic pseudo-random value in [0, 1) for particle layout. */
private fun rand(seed: Int, salt: Int): Float {
    val v = sin(seed * 127.1 + salt * 311.7) * 43758.5453
    return (v - floor(v)).toFloat()
}

/** Chooses the animated layers that match the current WMO weather code. */
@Composable
private fun WeatherLayers(code: Int, isDay: Int, dark: Boolean) {
    when {
        code in 95..99 -> {
            LightningFlash()
            CloudLayer(dark = dark, dense = true)
            RainLayer(intense = true, dark = dark)
        }

        code in 51..67 || code in 80..82 -> {
            CloudLayer(dark = dark, dense = code >= 63)
            RainLayer(intense = code in 63..67 || code == 82, dark = dark)
        }

        code in 71..77 || code == 85 || code == 86 -> {
            CloudLayer(dark = dark, dense = false)
            SnowLayer(dark = dark)
        }

        code in 45..48 -> FogLayer(dark = dark)

        code == 2 || code == 3 -> CloudLayer(dark = dark, dense = code == 3)

        (code == 0 || code == 1) && isDay == 0 -> StarLayer()

        else -> Unit
    }
}

@Composable
private fun RainLayer(intense: Boolean, dark: Boolean, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "rain")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = if (intense) 2800 else 4000, easing = LinearEasing)
        ),
        label = "progress"
    )
    // Soft, sparse, cool-tinted drizzle instead of bright white streaks.
    val color = if (dark) Color(0xFFC9DCF5).copy(alpha = 0.18f)
    else Color(0xFFE8F2FF).copy(alpha = 0.26f)
    val count = if (intense) 50 else 32

    Canvas(modifier = modifier.fillMaxSize()) {
        val length = size.height * (if (intense) 0.035f else 0.024f)
        val drift = length * 0.22f
        val strokeWidth = (if (intense) 1.6f else 1.2f).dp.toPx()
        repeat(count) { i ->
            val x = rand(i, 1) * (size.width + 60f) - 30f
            val speed = 0.60f + rand(i, 2) * 0.45f
            val y = ((rand(i, 3) + progress * speed) % 1f) * (size.height + length) - length
            drawLine(
                color = color,
                start = Offset(x, y),
                end = Offset(x - drift, y + length),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
        }
    }
}

@Composable
private fun SnowLayer(dark: Boolean, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "snow")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9_000, easing = LinearEasing)
        ),
        label = "progress"
    )
    val color = if (dark) Color.White.copy(alpha = 0.75f) else Color.White

    Canvas(modifier = modifier.fillMaxSize()) {
        repeat(60) { i ->
            val radius = (2.2f + rand(i, 7) * 3.4f).dp.toPx()
            val sway = sin(
                (progress * 6.2832f * (0.5f + rand(i, 4)) + rand(i, 5) * 6.2832f).toDouble()
            ).toFloat() * (8f + rand(i, 6) * 22f).dp.toPx()
            val x = rand(i, 1) * size.width + sway
            val speed = 0.35f + rand(i, 2) * 0.5f
            val y = ((rand(i, 3) + progress * speed) % 1f) * (size.height + radius * 2f) - radius
            val alpha = 0.45f + rand(i, 8) * 0.55f
            drawCircle(
                color = color.copy(alpha = alpha),
                radius = radius,
                center = Offset(x, y)
            )
        }
    }
}

@Composable
private fun CloudLayer(dark: Boolean, dense: Boolean, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "clouds")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 45_000, easing = LinearEasing)
        ),
        label = "progress"
    )
    val baseAlpha = (if (dark) 0.16f else 0.42f) * (if (dense) 1.35f else 1f)

    Canvas(modifier = modifier.fillMaxSize()) {
        val count = if (dense) 7 else 4
        repeat(count) { i ->
            val width = size.width * (0.55f + rand(i, 1) * 0.55f)
            val height = size.height * (0.10f + rand(i, 2) * 0.09f)
            val speed = 0.35f + rand(i, 3) * 0.5f
            val cx = ((rand(i, 4) + progress * speed) % 1f) * (size.width + width) - width / 2f
            val cy = size.height * (0.06f + rand(i, 5) * 0.55f)
            val alpha = (baseAlpha * (0.7f + rand(i, 6) * 0.5f)).coerceIn(0f, 1f)
            val brush = Brush.radialGradient(
                colors = listOf(Color.White.copy(alpha = alpha), Color.White.copy(alpha = 0f)),
                center = Offset(cx, cy),
                radius = width / 2f
            )
            drawOval(
                brush = brush,
                topLeft = Offset(cx - width / 2f, cy - height / 2f),
                size = Size(width, height)
            )
        }
    }
}

@Composable
private fun FogLayer(dark: Boolean, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "fog")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 30_000, easing = LinearEasing)
        ),
        label = "progress"
    )
    val fogColor = if (dark) Color(0xFFC6D0DE).copy(alpha = 0.22f) else Color.White.copy(alpha = 0.50f)

    Canvas(modifier = modifier.fillMaxSize()) {
        repeat(5) { i ->
            val bandHeight = size.height * (0.07f + rand(i, 1) * 0.06f)
            val bandWidth = size.width * 1.5f
            val y = size.height * (0.05f + i * 0.19f + rand(i, 2) * 0.05f)
            val x = ((rand(i, 3) + progress * (0.35f + i * 0.12f)) % 1f) *
                (size.width + bandWidth) - bandWidth / 2f
            val brush = Brush.horizontalGradient(
                colors = listOf(Color.Transparent, fogColor, Color.Transparent),
                startX = x,
                endX = x + bandWidth
            )
            drawRect(
                brush = brush,
                topLeft = Offset(x, y),
                size = Size(bandWidth, bandHeight)
            )
        }
    }
}

@Composable
private fun StarLayer(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "stars")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 6_000, easing = LinearEasing)
        ),
        label = "progress"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        repeat(70) { i ->
            val x = rand(i, 1) * size.width
            val y = rand(i, 2) * size.height * 0.72f
            val phaseOffset = rand(i, 3) * 6.2832f
            val speed = 0.5f + rand(i, 4)
            val twinkle = 0.5f + 0.5f *
                sin((progress * 6.2832f * speed + phaseOffset).toDouble()).toFloat()
            val radius = (1f + rand(i, 5) * 1.4f).dp.toPx()
            drawCircle(
                color = Color.White.copy(alpha = (0.20f + rand(i, 6) * 0.55f) * twinkle),
                radius = radius,
                center = Offset(x, y)
            )
        }
    }
}

/** A quick double lightning flicker every few seconds during thunderstorms. */
@Composable
private fun LightningFlash(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "lightning")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 5_200, easing = LinearEasing)
        ),
        label = "progress"
    )

    fun spike(center: Float, width: Float): Float {
        val distance = abs(progress - center)
        return if (distance >= width) 0f else 1f - distance / width
    }

    val alpha = max(spike(0.60f, 0.020f), spike(0.66f, 0.014f) * 0.65f) * 0.45f
    if (alpha > 0.005f) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(Color.White.copy(alpha = alpha))
        )
    }
}

// endregion

// endregion

// region Glass materials with light / dark support

internal data class GlassPalette(
    val fillTop: Color,
    val fillBottom: Color,
    val borderTop: Color,
    val borderBottom: Color,
    val sheenTop: Color,
    val text: Color,
    val title: Color,
    val secondary: Color,
    val separator: Color,
    val track: Color,
    val pillBg: Color,
    val pillBorder: Color,
    val pillText: Color,
    val pillSelectedBg: Color,
    val pillSelectedText: Color,
)

@Composable
internal fun glassPalette(): GlassPalette =
    if (isSystemInDarkTheme()) {
        GlassPalette(
            fillTop = Color(0xFF12161F).copy(alpha = 0.42f),
            fillBottom = Color(0xFF080B12).copy(alpha = 0.56f),
            borderTop = Color.White.copy(alpha = 0.20f),
            borderBottom = Color.White.copy(alpha = 0.05f),
            sheenTop = Color.White.copy(alpha = 0.06f),
            text = Color.White.copy(alpha = 0.96f),
            title = Color.White.copy(alpha = 0.62f),
            secondary = Color.White.copy(alpha = 0.70f),
            separator = Color.White.copy(alpha = 0.10f),
            track = Color.White.copy(alpha = 0.16f),
            pillBg = Color.Black.copy(alpha = 0.28f),
            pillBorder = Color.White.copy(alpha = 0.18f),
            pillText = Color.White.copy(alpha = 0.92f),
            pillSelectedBg = Color.White.copy(alpha = 0.92f),
            pillSelectedText = Color(0xFF0D1118),
        )
    } else {
        GlassPalette(
            fillTop = Color.White.copy(alpha = 0.46f),
            fillBottom = Color(0xFFF2F5FA).copy(alpha = 0.30f),
            borderTop = Color.White.copy(alpha = 0.90f),
            borderBottom = Color.White.copy(alpha = 0.35f),
            sheenTop = Color.White.copy(alpha = 0.30f),
            text = Color(0xFF11141A),
            title = Color(0xFF5C6478),
            secondary = Color(0xFF4A5265),
            separator = Color(0x14000000),
            track = Color(0x1A000000),
            pillBg = Color.White.copy(alpha = 0.45f),
            pillBorder = Color.White.copy(alpha = 0.70f),
            pillText = Color(0xFF141821),
            pillSelectedBg = Color(0xFF141821),
            pillSelectedText = Color.White,
        )
    }

/** Frosted-glass card surface shared by every panel on the screen. */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
private fun GlassCard(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(26.dp),
    content: @Composable BoxScope.() -> Unit,
) {
    val hazeState = LocalWeatherHaze.current
    val palette = glassPalette()
    Box(
        modifier = modifier
            .clip(shape)
            .hazeEffect(state = hazeState, style = HazeMaterials.thin())
            .background(Brush.verticalGradient(listOf(palette.fillTop, palette.fillBottom)))
            .border(
                width = 1.dp,
                brush = Brush.verticalGradient(listOf(palette.borderTop, palette.borderBottom)),
                shape = shape
            ),
        content = {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(
                            0f to palette.sheenTop,
                            0.45f to Color.Transparent
                        )
                    )
            )
            content()
        }
    )
}

// endregion

// region Header, pills, banner

@Composable
private fun WeatherHeader(
    current: CurrentWeather?,
    daily: DailyWeather?,
) {
    val shadow = Shadow(color = Color.Black.copy(alpha = 0.28f), offset = Offset(0f, 2f), blurRadius = 14f)

    val temperature = current?.temperature?.roundToInt()
    val description = current?.let { weatherDescription(it.weatherCode) }.orEmpty()
    val todayHigh = daily?.temperatureMax?.getOrNull(0)?.roundToInt()
    val todayLow = daily?.temperatureMin?.getOrNull(0)?.roundToInt()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 28.dp, bottom = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (temperature != null) {
            // Soft halo makes the current temperature the clear focal point.
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(300.dp, 190.dp)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    Color.White.copy(alpha = 0.18f),
                                    Color.White.copy(alpha = 0.05f),
                                    Color.Transparent
                                )
                            )
                        )
                )
                Text(
                    text = "${temperature}°",
                    style = TextStyle(
                        color = Color.White,
                        fontSize = 112.sp,
                        fontWeight = FontWeight.Normal,
                        textAlign = TextAlign.Center,
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.32f),
                            offset = Offset(0f, 3f),
                            blurRadius = 22f
                        )
                    )
                )
            }
        }
        if (description.isNotBlank()) {
            Text(
                text = description,
                style = TextStyle(
                    color = Color.White.copy(alpha = 0.94f),
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center,
                    shadow = shadow
                ),
                modifier = Modifier.padding(top = 2.dp)
            )
        }
        if (todayHigh != null && todayLow != null) {
            Text(
                text = "最高 ${todayHigh}°　最低 ${todayLow}°",
                style = TextStyle(
                    color = Color.White.copy(alpha = 0.80f),
                    fontSize = 16.sp,
                    textAlign = TextAlign.Center,
                    shadow = shadow
                ),
                modifier = Modifier.padding(top = 5.dp)
            )
        }
    }
}

/** Breathing glass placeholders shown while the forecast is loading. */
@Composable
private fun WeatherHeaderSkeleton() {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 950),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 48.dp, bottom = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .width(190.dp)
                .height(92.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(Color.White.copy(alpha = alpha * 0.55f))
        )
        Box(
            modifier = Modifier
                .width(110.dp)
                .height(20.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.White.copy(alpha = alpha * 0.45f))
        )
        Box(
            modifier = Modifier
                .width(160.dp)
                .height(14.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(Color.White.copy(alpha = alpha * 0.38f))
        )
    }
}

@Composable
private fun CityPills(
    cities: List<City>,
    selectedCity: City,
    isFollowingLocation: Boolean,
    onCitySelected: (City) -> Unit,
    onLocationSelected: () -> Unit,
    onAddCity: () -> Unit,
    onRemoveCity: (City) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(cities.size) {
        if (cities.isNotEmpty()) {
            // Reveal the chip that was just added.
            listState.animateScrollToItem(cities.size)
        }
    }
    LazyRow(
        modifier = modifier.fillMaxWidth(),
        state = listState,
        contentPadding = PaddingValues(vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        item {
            AppPill(
                label = if (isFollowingLocation) "我的位置" else "定位",
                icon = if (isFollowingLocation) Icons.Rounded.MyLocation else Icons.Rounded.LocationOn,
                selected = isFollowingLocation,
                onClick = onLocationSelected
            )
        }
        items(cities.size) { index ->
            val city = cities[index]
            AppPill(
                label = city.name,
                selected = !isFollowingLocation && city == selectedCity,
                onClick = { onCitySelected(city) },
                onLongClick = { onRemoveCity(city) }
            )
        }
        item {
            AppPill(
                label = "添加城市",
                icon = Icons.Rounded.Add,
                selected = false,
                onClick = onAddCity
            )
        }
    }
}

@Composable
private fun AppPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
) {
    val palette = glassPalette()
    val shape = RoundedCornerShape(50)
    Box(
        modifier = modifier
            .clip(shape)
            .background(if (selected) palette.pillSelectedBg else palette.pillBg)
            .border(
                width = 1.dp,
                color = if (selected) palette.pillSelectedBg else palette.pillBorder,
                shape = shape
            )
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 15.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = if (selected) palette.pillSelectedText else palette.pillText
                )
                Spacer(modifier = Modifier.width(6.dp))
            }
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) palette.pillSelectedText else palette.pillText,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                maxLines = 1
            )
        }
    }
}

/** iOS 27 style liquid-glass search sheet for adding a city. */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
private fun AddCityDialog(
    existingCities: List<City>,
    onDismiss: () -> Unit,
    onAdd: (City) -> Unit,
    search: suspend (String) -> List<City>,
) {
    val palette = glassPalette()
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<City>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    // Ignore the pointer that opened the sheet so it cannot immediately dismiss it.
    var canDismiss by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(350)
        canDismiss = true
    }

    LaunchedEffect(query) {
        if (query.isBlank()) {
            results = emptyList()
            errorMessage = null
            return@LaunchedEffect
        }
        delay(300)
        searching = true
        errorMessage = null
        try {
            results = search(query.trim())
        } catch (_: Exception) {
            results = emptyList()
            errorMessage = "搜索失败，请检查网络连接"
        } finally {
            searching = false
        }
    }

    // In-tree overlay: haze samples the home screen in the same window, so the sheet can
    // stay translucent and legible — the iOS 27 liquid-glass approach.
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(enabled = canDismiss, onClick = onDismiss)
            .background(Color.Black.copy(alpha = 0.22f)),
        contentAlignment = Alignment.Center
    ) {
        val sheetShape = RoundedCornerShape(32.dp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 40.dp)
                .clip(sheetShape)
                .hazeEffect(
                    state = LocalWeatherHaze.current,
                    style = HazeMaterials.regular()
                )
                .background(
                    Brush.verticalGradient(
                        if (isSystemInDarkTheme()) {
                            listOf(
                                Color(0xFF1A202C).copy(alpha = 0.82f),
                                Color(0xFF0E131C).copy(alpha = 0.76f)
                            )
                        } else {
                            listOf(
                                Color.White.copy(alpha = 0.78f),
                                Color(0xFFEEF3FA).copy(alpha = 0.68f)
                            )
                        }
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(listOf(palette.borderTop, palette.borderBottom)),
                    shape = sheetShape
                )
        ) {
            // Specular sheen across the top edge.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(
                        Brush.verticalGradient(0f to palette.sheenTop, 0.4f to Color.Transparent)
                    )
            )
            Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(22.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(palette.pillSelectedBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Add,
                                contentDescription = null,
                                modifier = Modifier.size(21.dp),
                                tint = palette.pillSelectedText
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "添加城市",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = palette.text
                            )
                            Text(
                                text = "搜索城市、县区或乡镇",
                                style = MaterialTheme.typography.labelMedium,
                                color = palette.secondary
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // Translucent search field with a search glyph.
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(palette.pillBg)
                            .border(1.dp, palette.pillBorder, RoundedCornerShape(18.dp))
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Search,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = palette.secondary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Box(modifier = Modifier.weight(1f)) {
                            if (query.isEmpty()) {
                                Text(
                                    text = "输入城市名称",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = palette.title
                                )
                            }
                            BasicTextField(
                                value = query,
                                onValueChange = { query = it },
                                singleLine = true,
                                textStyle = TextStyle(color = palette.text, fontSize = 16.sp),
                                cursorBrush = SolidColor(palette.text),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 120.dp, max = 320.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(palette.pillBg)
                    ) {
                        when {
                            searching -> {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = Color(0xFF4C8DFF),
                                        trackColor = palette.track
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "搜索中…",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = palette.secondary
                                    )
                                }
                            }

                            errorMessage != null -> {
                                Text(
                                    text = errorMessage ?: "",
                                    modifier = Modifier.padding(16.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color(0xFFFF6B6B)
                                )
                            }

                            query.isBlank() -> {
                                Text(
                                    text = "输入城市名称后自动搜索，点击结果即可添加",
                                    modifier = Modifier.padding(16.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = palette.secondary
                                )
                            }

                            results.isEmpty() -> {
                                Text(
                                    text = "未找到匹配城市",
                                    modifier = Modifier.padding(16.dp),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = palette.secondary
                                )
                            }

                            else -> {
                                LazyColumn(modifier = Modifier.fillMaxSize()) {
                                    items(results) { city ->
                                        val added = existingCities.any {
                                            it.name == city.name &&
                                                abs(it.latitude - city.latitude) < 0.01 &&
                                                abs(it.longitude - city.longitude) < 0.01
                                        }
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable(enabled = !added) { onAdd(city) }
                                                .padding(vertical = 12.dp, horizontal = 14.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = city.name,
                                                    style = MaterialTheme.typography.bodyLarge,
                                                    fontWeight = FontWeight.Medium,
                                                    color = if (added) palette.secondary else palette.text
                                                )
                                                if (city.region.isNotBlank()) {
                                                    Text(
                                                        text = city.region,
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = palette.secondary,
                                                        maxLines = 1
                                                    )
                                                }
                                            }
                                            Text(
                                                text = if (added) "已添加" else "添加",
                                                style = MaterialTheme.typography.labelMedium,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (added) palette.secondary else Color(0xFF4C8DFF)
                                            )
                                        }
                                        HorizontalDivider(color = palette.separator)
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(palette.pillSelectedBg)
                                .clickable(onClick = onDismiss)
                                .padding(horizontal = 24.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = "完成",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = palette.pillSelectedText
                            )
                        }
                    }
                }
            }
        }
}

@Composable
private fun PermissionBanner(
    onRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = glassPalette()
    GlassCard(modifier = modifier, shape = RoundedCornerShape(22.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "定位权限未开启，无法自动获取当前位置天气",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = palette.text
            )
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(palette.pillSelectedBg)
                    .clickable(onClick = onRequest)
                    .padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "去开启",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.pillSelectedText
                )
            }
        }
    }
}

// endregion

// region Highlights (iOS 27 summary card)

private data class HighlightItem(val icon: ImageVector, val text: String)

/**
 * Builds the short list of "things that would actually change your plans" shown in the
 * iOS 27 style Highlights card: rain arriving, a big temperature swing, strong wind or
 * high UV over the next day.
 */
private fun buildHighlights(
    current: CurrentWeather?,
    daily: DailyWeather?,
    hours: List<HourInfo>,
): List<HighlightItem> {
    val items = mutableListOf<HighlightItem>()

    val rainy = hours.firstOrNull { (it.precipProbability ?: 0) >= 40 }
    val maxProbability = hours.mapNotNull { it.precipProbability }.maxOrNull()
    if (rainy != null && maxProbability != null) {
        items += HighlightItem(
            icon = Icons.Rounded.WaterDrop,
            text = "降水临近：${rainy.label}起降雨概率升至 ${maxProbability}%"
        )
    }

    val todayMax = daily?.temperatureMax?.getOrNull(0)
    val todayMin = daily?.temperatureMin?.getOrNull(0)
    if (todayMax != null && todayMin != null && todayMax - todayMin >= 8.0) {
        items += HighlightItem(
            icon = Icons.Rounded.DeviceThermostat,
            text = "今日温差 ${(todayMax - todayMin).roundToInt()}°，" +
                "最高 ${todayMax.roundToInt()}° / 最低 ${todayMin.roundToInt()}°"
        )
    }

    val gust = daily?.windGustsMax?.getOrNull(0)
    if (gust != null && gust >= 30.0) {
        items += HighlightItem(
            icon = Icons.Rounded.Air,
            text = "风力增强：今日阵风可达 ${gust.roundToInt()} km/h"
        )
    }

    val uv = daily?.uvIndexMax?.getOrNull(0)
    if (uv != null && uv >= 6.0) {
        items += HighlightItem(
            icon = Icons.Rounded.WbSunny,
            text = "紫外线指数 ${formatNumber(uv)}，${uvLevel(uv)}"
        )
    }

    if (items.isEmpty()) {
        val feels = current?.apparentTemperature
        val temp = current?.temperature
        val verdict = when {
            feels == null || temp == null -> "未来 24 小时无明显天气变化"
            feels < temp - 1.0 -> "风寒明显，体感比实际低 ${abs((temp - feels).roundToInt())}°"
            feels > temp + 1.0 -> "体感偏热，比实际高 ${abs((feels - temp).roundToInt())}°"
            else -> "未来 24 小时天气平稳，无需特别准备"
        }
        items += HighlightItem(icon = Icons.Rounded.CheckCircle, text = verdict)
    }

    return items.take(3)
}

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 6.dp),
        style = TextStyle(
            color = Color.White.copy(alpha = 0.92f),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 2.sp,
            shadow = Shadow(
                color = Color.Black.copy(alpha = 0.38f),
                offset = Offset(0f, 1f),
                blurRadius = 5f
            )
        )
    )
}

@Composable
private fun HighlightsCard(items: List<HighlightItem>, modifier: Modifier = Modifier) {
    val palette = glassPalette()
    GlassCard(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items.forEach { item ->
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        imageVector = item.icon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = palette.text
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = item.text,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.text
                    )
                }
            }
        }
    }
}

// endregion

// region Hourly / daily / detail cards

private data class HourInfo(
    val label: String,
    val weatherCode: Int,
    val temperature: Int,
    val isDay: Int,
    val precipProbability: Int?,
    val windSpeed: Double?,
)

private fun nextHours(
    hourly: com.nongma.weather.data.HourlyWeather?,
    currentTime: String,
    count: Int = 24,
): List<HourInfo> {
    if (hourly == null || hourly.time.isEmpty()) return emptyList()
    val key = currentTime.take(13)
    val start = hourly.time.indexOfFirst { it.take(13) == key }.coerceAtLeast(0)
    return (0 until count).mapNotNull { offset ->
        val index = start + offset
        val time = hourly.time.getOrNull(index) ?: return@mapNotNull null
        HourInfo(
            label = if (offset == 0) "现在" else "${time.substring(11, 13).toInt()}时",
            weatherCode = hourly.weatherCode.getOrElse(index) { 0 },
            temperature = hourly.temperature.getOrNull(index)?.roundToInt() ?: 0,
            isDay = hourly.isDay.getOrElse(index) { 1 },
            precipProbability = hourly.precipitationProbability.getOrNull(index),
            windSpeed = hourly.windSpeed.getOrNull(index)
        )
    }
}

/** iOS 27 style forecast tabs: Conditions, Precipitation and Wind. */
enum class ForecastView { Conditions, Precipitation, Wind }

private fun viewTitle(view: ForecastView): String = when (view) {
    ForecastView.Conditions -> "实况预报"
    ForecastView.Precipitation -> "降水预报"
    ForecastView.Wind -> "风力预报"
}

private fun viewSubtitle(view: ForecastView): String = when (view) {
    ForecastView.Conditions -> "温度 · 天气状况"
    ForecastView.Precipitation -> "强度 · 降水概率"
    ForecastView.Wind -> "风速 · 阵风"
}

private fun viewIcon(view: ForecastView): ImageVector = when (view) {
    ForecastView.Conditions -> Icons.Rounded.WbSunny
    ForecastView.Precipitation -> Icons.Rounded.WaterDrop
    ForecastView.Wind -> Icons.Rounded.Air
}

/**
 * One iOS 27 style forecast card: header + view switcher on the top right, the hourly
 * strip and the daily list below, all switching together between Conditions,
 * Precipitation and Wind.
 */
@Composable
private fun ForecastCard(
    hours: List<HourInfo>,
    daily: DailyWeather?,
    view: ForecastView,
    onViewChange: (ForecastView) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = glassPalette()
    GlassCard(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 12.dp, top = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = viewTitle(view),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.text
                    )
                    Text(
                        text = viewSubtitle(view),
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.secondary
                    )
                }
                ForecastViewSwitcher(view = view, onViewChange = onViewChange)
            }

            when (view) {
                ForecastView.Conditions -> ConditionsHourly(hours = hours, palette = palette)
                ForecastView.Precipitation -> PrecipHourly(hours = hours, palette = palette)
                ForecastView.Wind -> WindHourly(hours = hours, palette = palette)
            }

            HorizontalDivider(color = palette.separator)
            ForecastDaily(daily = daily, view = view)
        }
    }
}

@Composable
private fun ForecastViewSwitcher(
    view: ForecastView,
    onViewChange: (ForecastView) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = glassPalette()
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(palette.track)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        ForecastView.entries.forEach { entry ->
            val selected = entry == view
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (selected) palette.pillSelectedBg else Color.Transparent)
                    .clickable { onViewChange(entry) },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = viewIcon(entry),
                    contentDescription = null,
                    modifier = Modifier.size(19.dp),
                    tint = if (selected) palette.pillSelectedText else palette.secondary
                )
            }
        }
    }
}

@Composable
private fun ConditionsHourly(hours: List<HourInfo>, palette: GlassPalette) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        hours.forEach { hour ->
            Column(
                modifier = Modifier.width(62.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = hour.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.secondary,
                    maxLines = 1
                )
                Icon(
                    imageVector = weatherIconVector(hour.weatherCode, hour.isDay),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = palette.secondary
                )
                Text(
                    text = "${hour.temperature}°",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.text,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun PrecipHourly(hours: List<HourInfo>, palette: GlassPalette) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 8.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        hours.forEach { hour ->
            val probability = (hour.precipProbability ?: 0).coerceIn(0, 100)
            Column(
                modifier = Modifier.width(62.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = hour.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = palette.secondary,
                    maxLines = 1
                )
                Box(
                    modifier = Modifier
                        .width(26.dp)
                        .height(56.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(palette.track),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height((4 + 52 * probability / 100f).dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(
                                Brush.verticalGradient(
                                    listOf(Color(0xFF8FD3FF), Color(0xFF2E6BFF))
                                )
                            )
                    )
                }
                Text(
                    text = "${probability}%",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.text,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
private fun WindHourly(hours: List<HourInfo>, palette: GlassPalette) {
    if (hours.isEmpty()) return
    val columnWidth = 62.dp
    val speeds = hours.mapNotNull { it.windSpeed?.toFloat() }
    val minSpeed = ((speeds.minOrNull() ?: 0f) - 2f).coerceAtLeast(0f)
    val maxSpeed = ((speeds.maxOrNull() ?: 10f) + 2f).coerceAtLeast(minSpeed + 5f)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 14.dp)
    ) {
        Row(modifier = Modifier.padding(horizontal = 8.dp)) {
            hours.forEach { hour ->
                Column(
                    modifier = Modifier.width(62.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = hour.label,
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.secondary,
                        maxLines = 1
                    )
                    Text(
                        text = hour.windSpeed?.roundToInt()?.toString() ?: "--",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.text,
                        maxLines = 1
                    )
                    Text(
                        text = "km/h",
                        style = MaterialTheme.typography.labelSmall,
                        color = palette.title,
                        maxLines = 1
                    )
                }
            }
        }

        // iOS 27 style: a line chart connecting the hourly wind speeds.
        Canvas(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .width(columnWidth * hours.size)
                .height(48.dp)
        ) {
            if (hours.size >= 2) {
                val step = columnWidth.toPx()
                val range = (maxSpeed - minSpeed).takeIf { it > 0f } ?: 1f
                val path = Path()
                hours.forEachIndexed { index, hour ->
                    val speed = (hour.windSpeed ?: 0.0).toFloat()
                    val fraction = ((speed - minSpeed) / range).coerceIn(0f, 1f)
                    val x = step * (index + 0.5f)
                    val y = size.height * (0.88f - 0.72f * fraction)
                    if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                }
                drawPath(
                    path = path,
                    color = Color(0xFF34C759),
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )
                val nowSpeed = (hours[0].windSpeed ?: 0.0).toFloat()
                val nowFraction = ((nowSpeed - minSpeed) / range).coerceIn(0f, 1f)
                drawCircle(
                    color = Color(0xFFFFB36B),
                    radius = 5.dp.toPx(),
                    center = Offset(step * 0.5f, size.height * (0.88f - 0.72f * nowFraction))
                )
            }
        }
    }
}

/** The daily list below the hourly strip; its rows follow the selected forecast view. */
@Composable
private fun ForecastDaily(daily: DailyWeather?, view: ForecastView) {
    val palette = glassPalette()
    val days = daily?.time.orEmpty()
    if (days.isEmpty()) {
        Text(
            text = "暂无数据",
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.bodyMedium,
            color = palette.secondary
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        when (view) {
            ForecastView.Conditions -> {
                val weekMin = days.indices.minOfOrNull {
                    daily?.temperatureMin?.getOrNull(it) ?: 0.0
                } ?: 0.0
                val weekMax = days.indices.maxOfOrNull {
                    daily?.temperatureMax?.getOrNull(it) ?: 30.0
                } ?: 30.0
                days.forEachIndexed { index, date ->
                    DailyRow(
                        dayLabel = dayLabel(date, index),
                        weatherCode = daily?.weatherCode?.getOrNull(index) ?: 0,
                        minTemp = daily?.temperatureMin?.getOrNull(index),
                        maxTemp = daily?.temperatureMax?.getOrNull(index),
                        weekMin = weekMin,
                        weekMax = weekMax
                    )
                    if (index < days.lastIndex) {
                        HorizontalDivider(color = palette.separator)
                    }
                }
            }

            ForecastView.Precipitation -> {
                days.forEachIndexed { index, date ->
                    PrecipDailyRow(
                        dayLabel = dayLabel(date, index),
                        probability = daily?.precipitationProbabilityMax?.getOrNull(index),
                        amount = daily?.precipitationSum?.getOrNull(index)
                    )
                    if (index < days.lastIndex) {
                        HorizontalDivider(color = palette.separator)
                    }
                }
            }

            ForecastView.Wind -> {
                val weekMin = days.indices.minOfOrNull {
                    daily?.windSpeedMin?.getOrNull(it) ?: 0.0
                } ?: 0.0
                val weekMax = days.indices.maxOfOrNull {
                    daily?.windSpeedMax?.getOrNull(it) ?: 10.0
                } ?: 10.0
                days.forEachIndexed { index, date ->
                    WindDailyRow(
                        dayLabel = dayLabel(date, index),
                        minWind = daily?.windSpeedMin?.getOrNull(index),
                        maxWind = daily?.windSpeedMax?.getOrNull(index),
                        weekMin = weekMin,
                        weekMax = weekMax
                    )
                    if (index < days.lastIndex) {
                        HorizontalDivider(color = palette.separator)
                    }
                }
            }
        }
    }
}

@Composable
private fun PrecipDailyRow(
    dayLabel: String,
    probability: Int?,
    amount: Double?,
) {
    val palette = glassPalette()
    val prob = (probability ?: 0).coerceIn(0, 100)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = dayLabel,
            modifier = Modifier.width(46.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = palette.text,
            maxLines = 1
        )
        Spacer(modifier = Modifier.weight(1f))
        RangeBar(
            startFraction = 0f,
            endFraction = prob / 100f,
            modifier = Modifier.padding(horizontal = 10.dp),
            colors = listOf(Color(0xFF7FC8FF), Color(0xFF2E6BFF))
        )
        Text(
            text = if (amount != null && amount > 0.0) formatMm(amount) else "--",
            style = MaterialTheme.typography.titleMedium,
            color = palette.secondary,
            textAlign = TextAlign.End,
            modifier = Modifier.width(70.dp),
            maxLines = 1
        )
        Text(
            text = "${prob}%",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = palette.text,
            textAlign = TextAlign.End,
            modifier = Modifier.width(44.dp),
            maxLines = 1
        )
    }
}

@Composable
private fun WindDailyRow(
    dayLabel: String,
    minWind: Double?,
    maxWind: Double?,
    weekMin: Double,
    weekMax: Double,
) {
    val palette = glassPalette()
    val range = (weekMax - weekMin).takeIf { it > 0.0 } ?: 1.0
    val start = (((minWind ?: weekMin) - weekMin) / range).coerceIn(0.0, 1.0).toFloat()
    val end = (((maxWind ?: weekMax) - weekMin) / range)
        .coerceIn(start.toDouble(), 1.0)
        .toFloat()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = dayLabel,
            modifier = Modifier.width(46.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = palette.text,
            maxLines = 1
        )
        Spacer(modifier = Modifier.weight(1f))
        RangeBar(
            startFraction = start,
            endFraction = end,
            modifier = Modifier.padding(horizontal = 10.dp),
            colors = listOf(Color(0xFF66E0FF), Color(0xFF34C759))
        )
        Text(
            text = "${minWind?.roundToInt() ?: 0} - ${maxWind?.roundToInt() ?: 0}",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = palette.text,
            textAlign = TextAlign.End,
            modifier = Modifier.width(74.dp),
            maxLines = 1
        )
        Text(
            text = "km/h",
            style = MaterialTheme.typography.labelSmall,
            color = palette.secondary,
            modifier = Modifier.width(44.dp),
            maxLines = 1
        )
    }
}

@Composable
private fun DailyRow(
    dayLabel: String,
    weatherCode: Int,
    minTemp: Double?,
    maxTemp: Double?,
    weekMin: Double,
    weekMax: Double,
) {
    val palette = glassPalette()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = dayLabel,
            modifier = Modifier.width(52.dp),
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = palette.text,
            maxLines = 1
        )
        Box(
            modifier = Modifier.width(30.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = weatherIconVector(weatherCode, 1),
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = palette.secondary
            )
        }
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = "${minTemp?.roundToInt() ?: "--"}°",
            style = MaterialTheme.typography.titleMedium,
            color = palette.secondary,
            textAlign = TextAlign.End,
            modifier = Modifier.width(40.dp),
            maxLines = 1
        )
        RangeBar(
            startFraction = tempFractionStart(minTemp, weekMin, weekMax),
            endFraction = tempFractionEnd(minTemp, maxTemp, weekMin, weekMax),
            modifier = Modifier.padding(horizontal = 10.dp)
        )
        Text(
            text = "${maxTemp?.roundToInt() ?: "--"}°",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = palette.text,
            textAlign = TextAlign.End,
            modifier = Modifier.width(40.dp),
            maxLines = 1
        )
    }
}

/** A small iOS style range track: a rounded window inside a translucent bar. */
@Composable
private fun RangeBar(
    startFraction: Float,
    endFraction: Float,
    modifier: Modifier = Modifier,
    colors: List<Color> = listOf(Color(0xFF4FA8F5), Color(0xFFFFC24B)),
) {
    val palette = glassPalette()
    val fullWidth = 56f
    val start = startFraction.coerceIn(0f, 1f)
    val end = endFraction.coerceIn(start, 1f)

    Box(
        modifier = modifier
            .width(fullWidth.dp)
            .height(5.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(palette.track),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .padding(start = (fullWidth * start).dp)
                .width(max(6f, fullWidth * (end - start)).dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(3.dp))
                .background(Brush.horizontalGradient(colors))
        )
    }
}

private fun tempFractionStart(minTemp: Double?, weekMin: Double, weekMax: Double): Float {
    val range = (weekMax - weekMin).takeIf { it > 0.0 } ?: 1.0
    return (((minTemp ?: weekMin) - weekMin) / range).coerceIn(0.0, 1.0).toFloat()
}

private fun tempFractionEnd(
    minTemp: Double?,
    maxTemp: Double?,
    weekMin: Double,
    weekMax: Double,
): Float {
    val start = tempFractionStart(minTemp, weekMin, weekMax)
    val range = (weekMax - weekMin).takeIf { it > 0.0 } ?: 1.0
    return (((maxTemp ?: weekMax) - weekMin) / range)
        .coerceIn(start.toDouble(), 1.0)
        .toFloat()
}

@Composable
private fun DetailGrid(weather: WeatherResponse) {
    val current = weather.current
    val daily = weather.daily
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Each pair shares the height of the taller card, like the iOS Weather grid.
        DetailRow {
            UvCard(daily = daily, modifier = Modifier.weight(1f).fillMaxHeight())
            SunriseCard(daily = daily, modifier = Modifier.weight(1f).fillMaxHeight())
        }
        DetailRow {
            WindCard(current = current, modifier = Modifier.weight(1f).fillMaxHeight())
            FeelsLikeCard(current = current, modifier = Modifier.weight(1f).fillMaxHeight())
        }
        DetailRow {
            PrecipitationCard(current = current, modifier = Modifier.weight(1f).fillMaxHeight())
            HumidityCard(current = current, modifier = Modifier.weight(1f).fillMaxHeight())
        }
        DetailRow {
            PressureCard(current = current, modifier = Modifier.weight(1f).fillMaxHeight())
            CloudCard(current = current, modifier = Modifier.weight(1f).fillMaxHeight())
        }
    }
}

@Composable
private fun DetailRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        content = content
    )
}

@Composable
private fun DetailCard(
    title: String,
    value: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    extra: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val palette = glassPalette()
    GlassCard(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp)) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Medium,
                color = palette.title,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontSize = 28.sp,
                fontWeight = FontWeight.SemiBold,
                color = palette.text,
                maxLines = 1
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = palette.secondary,
                maxLines = 2
            )
            if (extra != null) {
                Spacer(modifier = Modifier.height(10.dp))
                extra()
            }
        }
    }
}

@Composable
private fun UvCard(daily: DailyWeather?, modifier: Modifier = Modifier) {
    val uv = daily?.uvIndexMax?.getOrNull(0)
    DetailCard(
        title = "紫外线指数",
        value = uv?.let { formatNumber(it) } ?: "--",
        subtitle = uv?.let { uvLevel(it) } ?: "暂无数据",
        modifier = modifier,
        extra = { UvScaleBar(value = (uv ?: 0.0).toFloat()) }
    )
}

@Composable
private fun UvScaleBar(value: Float, modifier: Modifier = Modifier) {
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(
                Brush.horizontalGradient(
                    listOf(
                        Color(0xFF34C759),
                        Color(0xFFFFD60A),
                        Color(0xFFFF9500),
                        Color(0xFFFF3B30)
                    )
                )
            )
    ) {
        val fraction = (value / 11f).coerceIn(0f, 1f)
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = (maxWidth - 8.dp) * fraction)
                .size(8.dp)
                .clip(CircleShape)
                .background(Color.White)
                .border(1.dp, Color.Black.copy(alpha = 0.18f), CircleShape)
        )
    }
}

@Composable
private fun SunriseCard(daily: DailyWeather?, modifier: Modifier = Modifier) {
    val sunrise = timeOf(daily?.sunrise?.getOrNull(0))
    val sunset = timeOf(daily?.sunset?.getOrNull(0))
    DetailCard(
        title = "日出日落",
        value = sunrise ?: "--",
        subtitle = "日出时间",
        modifier = modifier,
        extra = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.WbSunny,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color(0xFFFFB36B)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${sunset ?: "--"} 日落",
                    style = MaterialTheme.typography.bodySmall,
                    color = glassPalette().secondary
                )
            }
        }
    )
}

@Composable
private fun WindCard(current: CurrentWeather?, modifier: Modifier = Modifier) {
    val palette = glassPalette()
    val direction = current?.windDirection ?: 0
    DetailCard(
        title = "风",
        value = current?.windSpeed?.let { "${it.roundToInt()} km/h" } ?: "--",
        subtitle = "${windDirectionName(direction)} · ${direction}°",
        modifier = modifier,
        extra = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "阵风 ${current?.windSpeed?.let { (it * 1.4).roundToInt() } ?: 0} km/h",
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.secondary
                )
                WindCompass(direction = direction)
            }
        }
    )
}

@Composable
private fun WindCompass(direction: Int, modifier: Modifier = Modifier) {
    val palette = glassPalette()
    Box(
        modifier = modifier
            .size(44.dp)
            .clip(CircleShape)
            .border(1.dp, palette.separator, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "N",
            style = MaterialTheme.typography.labelSmall,
            color = palette.title,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 3.dp)
        )
        Icon(
            imageVector = Icons.Rounded.Navigation,
            contentDescription = null,
            modifier = Modifier
                .size(22.dp)
                .rotate(((direction + 180) % 360).toFloat()),
            tint = palette.text
        )
    }
}

@Composable
private fun FeelsLikeCard(current: CurrentWeather?, modifier: Modifier = Modifier) {
    val feels = current?.apparentTemperature
    val actual = current?.temperature
    val subtitle = if (feels == null || actual == null) {
        "暂无数据"
    } else {
        val diff = (feels - actual).roundToInt()
        when {
            abs(diff) < 1 -> "与实际气温相近"
            diff > 0 -> "比实际气温高 ${abs(diff)}°"
            else -> "比实际气温低 ${abs(diff)}°"
        }
    }
    DetailCard(
        title = "体感温度",
        value = feels?.let { "${it.roundToInt()}°" } ?: "--",
        subtitle = subtitle,
        modifier = modifier
    )
}

@Composable
private fun PrecipitationCard(current: CurrentWeather?, modifier: Modifier = Modifier) {
    val mm = current?.precipitation
    DetailCard(
        title = "降水量",
        value = mm?.let { formatMm(it) } ?: "--",
        subtitle = if (mm != null && mm > 0.0) "当前小时降水" else "当前无降水",
        modifier = modifier
    )
}

@Composable
private fun HumidityCard(current: CurrentWeather?, modifier: Modifier = Modifier) {
    val humidity = current?.relativeHumidity
    DetailCard(
        title = "湿度",
        value = humidity?.let { "${it}%" } ?: "--",
        subtitle = humidity?.let {
            when {
                it < 30 -> "空气偏干燥"
                it <= 60 -> "体感舒适"
                else -> "空气偏潮湿"
            }
        } ?: "暂无数据",
        modifier = modifier
    )
}

@Composable
private fun PressureCard(current: CurrentWeather?, modifier: Modifier = Modifier) {
    val pressure = current?.pressureMsl
    DetailCard(
        title = "气压",
        value = pressure?.let { "${it.roundToInt()}" } ?: "--",
        subtitle = if (pressure != null) "hPa · 海平面气压" else "暂无数据",
        modifier = modifier
    )
}

@Composable
private fun CloudCard(current: CurrentWeather?, modifier: Modifier = Modifier) {
    val cloud = current?.cloudCover
    DetailCard(
        title = "云量",
        value = cloud?.let { "${it}%" } ?: "--",
        subtitle = cloud?.let {
            when {
                it > 90 -> "阴云密布"
                it > 50 -> "多云"
                it > 10 -> "少云"
                else -> "晴朗少云"
            }
        } ?: "暂无数据",
        modifier = modifier
    )
}

// endregion

// region States & footer

@Composable
private fun LoadingState() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(
                color = Color.White,
                trackColor = Color.White.copy(alpha = 0.25f)
            )
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "正在加载天气…",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.88f)
            )
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    val palette = glassPalette()
    GlassCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 24.dp),
        shape = RoundedCornerShape(26.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = message,
                style = MaterialTheme.typography.bodyLarge,
                color = palette.text,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(18.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(palette.pillSelectedBg)
                    .clickable(onClick = onRetry)
                    .padding(horizontal = 30.dp, vertical = 11.dp)
            ) {
                Text(
                    text = "重试",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.pillSelectedText
                )
            }
        }
    }
}

@Composable
private fun Footer() {
    Text(
        text = "天气数据由 Open-Meteo 提供",
        modifier = Modifier.padding(top = 6.dp),
        style = MaterialTheme.typography.labelSmall,
        color = Color.White.copy(alpha = 0.55f)
    )
}

// endregion

// region Helpers

private fun timeOf(iso: String?): String? =
    iso?.takeIf { it.length >= 16 }?.substring(11, 16)

private fun dayLabel(isoDate: String, index: Int): String = when (index) {
    0 -> "今天"
    1 -> "明天"
    else -> try {
        val parsed = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).parse(isoDate) ?: return isoDate
        SimpleDateFormat("EEE", Locale.CHINA).format(parsed)
    } catch (_: Exception) {
        isoDate
    }
}

private fun formatNumber(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString()
    else String.format(Locale.US, "%.1f", value)

private fun formatMm(value: Double): String {
    val tenths = (value * 10).roundToInt() / 10.0
    return if (tenths == tenths.toLong().toDouble()) {
        "${tenths.toLong()} mm"
    } else {
        "$tenths mm"
    }
}

private fun uvLevel(uv: Double): String = when {
    uv < 3.0 -> "低 · 无需特别防护"
    uv < 6.0 -> "中等 · 建议涂抹防晒"
    uv < 8.0 -> "高 · 避免正午暴晒"
    uv < 11.0 -> "很高 · 注意防晒措施"
    else -> "极高 · 尽量避免外出"
}

private fun windDirectionName(degrees: Int): String {
    val names = listOf("北风", "东北风", "东风", "东南风", "南风", "西南风", "西风", "西北风")
    val normalized = ((degrees % 360) + 360) % 360
    return names[((normalized + 22) / 45) % 8]
}

private fun weatherIconVector(code: Int, isDay: Int): ImageVector = when (code) {
    0 -> if (isDay == 1) Icons.Rounded.WbSunny else Icons.Rounded.DarkMode
    1, 2 -> if (isDay == 1) Icons.Rounded.WbSunny else Icons.Rounded.Cloud
    3, 45, 48 -> Icons.Rounded.Cloud
    51, 53, 55, 56, 57, 61, 63, 65, 66, 67, 80, 81, 82 -> Icons.Rounded.WaterDrop
    71, 73, 75, 77, 85, 86 -> Icons.Rounded.AcUnit
    95, 96, 99 -> Icons.Rounded.Thunderstorm
    else -> Icons.Rounded.Cloud
}

private fun weatherDescription(code: Int): String {
    return when (code) {
        0 -> "晴朗"
        1 -> "大部晴朗"
        2 -> "多云"
        3 -> "阴天"
        45, 48 -> "雾"
        51, 53, 55 -> "毛毛雨"
        56, 57 -> "冻毛毛雨"
        61 -> "小雨"
        63 -> "中雨"
        65 -> "大雨"
        66, 67 -> "冻雨"
        71 -> "小雪"
        73 -> "中雪"
        75 -> "大雪"
        77 -> "雪粒"
        80, 81, 82 -> "阵雨"
        85, 86 -> "阵雪"
        95 -> "雷暴"
        96, 99 -> "雷暴伴冰雹"
        else -> "未知"
    }
}

// endregion
