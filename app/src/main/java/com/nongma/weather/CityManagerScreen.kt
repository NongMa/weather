package com.nongma.weather

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.MyLocation
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import kotlinx.coroutines.delay
import kotlin.math.abs

/**
 * Full-screen city manager: search to add places, and review/delete places already added.
 * Rendered inside the same haze tree as the home screen so it keeps the liquid-glass look.
 */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun CityManagerScreen(
    cities: List<City>,
    selectedCity: City,
    isFollowingLocation: Boolean,
    onDismiss: () -> Unit,
    onAdd: (City) -> Unit,
    onRemove: (City) -> Unit,
    onSelect: (City) -> Unit,
    onLocation: () -> Unit,
    search: suspend (String) -> List<City>,
) {
    BackHandler(onBack = onDismiss)

    // Match the system bar icons to this screen's light/dark surface, and restore on exit.
    val view = LocalView.current
    val dark = isSystemInDarkTheme()
    DisposableEffect(view, dark) {
        val window = (view.context as? Activity)?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, window.decorView) }
        val previousStatus = controller?.isAppearanceLightStatusBars
        val previousNav = controller?.isAppearanceLightNavigationBars
        controller?.isAppearanceLightStatusBars = !dark
        controller?.isAppearanceLightNavigationBars = !dark
        onDispose {
            if (previousStatus != null) controller?.isAppearanceLightStatusBars = previousStatus
            if (previousNav != null) controller?.isAppearanceLightNavigationBars = previousNav
        }
    }

    val palette = glassPalette()
    val statusBarHeight = with(LocalDensity.current) {
        WindowInsets.statusBars.getTop(this).toDp()
    }
    // The list is more opaque so text stays crisp over the home-screen content.
    val listBackground = if (isSystemInDarkTheme()) {
        Color(0xFF151B26).copy(alpha = 0.96f)
    } else {
        Color.White.copy(alpha = 0.95f)
    }
    var query by remember { mutableStateOf("") }
    var results by remember { mutableStateOf<List<City>>(emptyList()) }
    var searching by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

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

    Box(modifier = Modifier.fillMaxSize()) {
        // A dim scrim behind the glass makes the blurred home screen read like iOS's
        // material layers, while the system bar icons stay legible above it.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.32f))
        )
        // Status-bar scrim: keeps the light system icons legible over the glass sheet.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(statusBarHeight)
                .align(Alignment.TopCenter)
                .background(Color.Black.copy(alpha = 0.42f))
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .hazeEffect(
                    state = LocalWeatherHaze.current,
                    style = HazeMaterials.regular()
                )
                .background(
                    Brush.verticalGradient(
                    if (isSystemInDarkTheme()) {
                        listOf(
                            Color(0xFF10151F).copy(alpha = 0.96f),
                            Color(0xFF060A11).copy(alpha = 0.98f)
                        )
                    } else {
                        listOf(
                            Color.White.copy(alpha = 0.95f),
                            Color(0xFFE9F0FA).copy(alpha = 0.97f)
                        )
                    }
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(palette.pillBg)
                        .border(1.dp, palette.pillBorder, CircleShape)
                        .clickable(onClick = onDismiss),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "返回",
                        modifier = Modifier.size(20.dp),
                        tint = palette.text
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "城市管理",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.text
                    )
                    Text(
                        text = "搜索添加，或移除已添加的城市",
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.secondary
                    )
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(palette.pillSelectedBg)
                        .clickable(onClick = onDismiss)
                        .padding(horizontal = 18.dp, vertical = 9.dp)
                ) {
                    Text(
                        text = "完成",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.SemiBold,
                        color = palette.pillSelectedText
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Search field
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
                            text = "输入城市、县区或乡镇名称",
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

            Spacer(modifier = Modifier.height(18.dp))

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(24.dp))
                    .background(listBackground)
                    .border(1.dp, palette.pillBorder, RoundedCornerShape(24.dp)),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                if (query.isBlank()) {
                    item {
                        Text(
                            text = "已添加城市 · ${cities.size}",
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.secondary
                        )
                    }
                    if (cities.isEmpty()) {
                        item {
                            Text(
                                text = "还没有添加城市，在上方搜索即可添加",
                                modifier = Modifier.padding(horizontal = 18.dp, vertical = 24.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.secondary
                            )
                        }
                    }
                    items(cities, key = { it.name + it.latitude + it.longitude }) { city ->
                        CityRow(
                            name = city.name,
                            region = city.region,
                            selected = !isFollowingLocation && city == selectedCity,
                            icon = Icons.Rounded.LocationOn,
                            onClick = { onSelect(city) },
                            onDelete = { onRemove(city) },
                            palette = palette
                        )
                        HorizontalDivider(color = palette.separator)
                    }
                } else {
                    item {
                        Text(
                            text = "搜索结果",
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = palette.secondary
                        )
                    }
                    when {
                        searching -> item {
                            Row(
                                modifier = Modifier.padding(18.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = Color(0xFF4C8DFF),
                                    trackColor = palette.track
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("搜索中…", style = MaterialTheme.typography.bodyMedium, color = palette.secondary)
                            }
                        }

                        errorMessage != null -> item {
                            Text(
                                text = errorMessage ?: "",
                                modifier = Modifier.padding(18.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFFFF6B6B)
                            )
                        }

                        results.isEmpty() -> item {
                            Text(
                                text = "未找到匹配城市",
                                modifier = Modifier.padding(18.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = palette.secondary
                            )
                        }

                        else -> items(results, key = { it.name + it.latitude + it.longitude }) { city ->
                            val added = cities.any {
                                it.name == city.name &&
                                    abs(it.latitude - city.latitude) < 0.01 &&
                                    abs(it.longitude - city.longitude) < 0.01
                            }
                            CityRow(
                                name = city.name,
                                region = city.region,
                                selected = false,
                                icon = if (added) Icons.Rounded.Check else Icons.Rounded.Add,
                                onClick = { if (!added) onAdd(city) },
                                palette = palette,
                                actionText = if (added) "已添加" else "添加"
                            )
                            HorizontalDivider(color = palette.separator)
                        }
                    }
                }
            }
        }
    }
    }
}

@Composable
private fun CityRow(
    name: String,
    region: String,
    selected: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
    palette: GlassPalette,
    onDelete: (() -> Unit)? = null,
    actionText: String? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(if (selected) palette.pillSelectedBg else palette.track),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = if (selected) palette.pillSelectedText else palette.secondary
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                color = if (selected) palette.text else palette.text
            )
            if (region.isNotBlank()) {
                Text(
                    text = region,
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.secondary,
                    maxLines = 1
                )
            }
        }
        if (actionText != null) {
            Text(
                text = actionText,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF4C8DFF)
            )
        }
        if (onDelete != null) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(palette.track)
                    .clickable(onClick = onDelete),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = "删除 $name",
                    modifier = Modifier.size(18.dp),
                    tint = Color(0xFFFF6B6B)
                )
            }
        }
    }
}
