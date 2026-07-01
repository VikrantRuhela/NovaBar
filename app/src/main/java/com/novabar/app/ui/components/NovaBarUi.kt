package com.novabar.app.ui.components

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import android.util.Log
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.draw.alpha
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import com.novabar.app.ui.icons.NovaIcons
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.lifecycle.viewmodel.compose.viewModel
import com.novabar.app.domain.*
import com.novabar.app.presentation.OverlayViewModel
import com.novabar.app.presentation.ViewModelFactory
import com.novabar.app.ui.theme.*
import com.novabar.app.utils.TorchManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlin.math.roundToInt
import kotlin.math.sin
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.translate
import android.os.Build
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb

fun drawableToBitmap(drawable: android.graphics.drawable.Drawable?): Bitmap? {
    if (drawable == null) return null
    if (drawable is android.graphics.drawable.BitmapDrawable) {
        if (drawable.bitmap != null) {
            return drawable.bitmap
        }
    }
    val bitmap = try {
        val w = if (drawable.intrinsicWidth <= 0) 100 else drawable.intrinsicWidth
        val h = if (drawable.intrinsicHeight <= 0) 100 else drawable.intrinsicHeight
        Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    } catch (e: Exception) {
        Bitmap.createBitmap(100, 100, Bitmap.Config.ARGB_8888)
    }
    val canvas = android.graphics.Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    return bitmap
}

@Composable
fun DrawableImage(
    drawable: Drawable,
    modifier: Modifier = Modifier,
    tintColor: Color? = null
) {
    Canvas(modifier = modifier) {
        val w = size.width.toInt()
        val h = size.height.toInt()
        drawable.setBounds(0, 0, w, h)
        
        if (tintColor != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                drawable.colorFilter = android.graphics.BlendModeColorFilter(
                    tintColor.toArgb(),
                    android.graphics.BlendMode.SRC_IN
                )
            } else {
                @Suppress("DEPRECATION")
                drawable.setColorFilter(tintColor.toArgb(), android.graphics.PorterDuff.Mode.SRC_IN)
            }
        }
        
        drawIntoCanvas { canvas ->
            drawable.draw(canvas.nativeCanvas)
        }
    }
}



fun formatSystemTime(timeFormat: String, showSeconds: Boolean): String {
    val cal = java.util.Calendar.getInstance()
    val is24 = if (timeFormat == "System Default") {
        false
    } else {
        timeFormat == "24 Hour"
    }
    
    val pattern = java.lang.StringBuilder()
    if (is24) {
        pattern.append("HH:mm")
        if (showSeconds) pattern.append(":ss")
    } else {
        pattern.append("h:mm")
        if (showSeconds) pattern.append(":ss")
        pattern.append(" a")
    }
    
    val sdf = java.text.SimpleDateFormat(pattern.toString(), java.util.Locale.getDefault())
    return sdf.format(cal.time)
}

fun formatSystemDate(): String {
    val cal = java.util.Calendar.getInstance()
    val sdf = java.text.SimpleDateFormat("EEE d", java.util.Locale.getDefault())
    return sdf.format(cal.time)
}

fun getSystemBatteryLevel(context: android.content.Context): Int {
    try {
        val bm = context.getSystemService(android.content.Context.BATTERY_SERVICE) as android.os.BatteryManager
        return bm.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY)
    } catch (e: Exception) {
        return 100
    }
}

@Composable
fun AlwaysOnView(
    config: String,
    timeFormat: String,
    showSeconds: Boolean,
    color: Color,
    textSizeOffset: Float,
    splitSegment: SplitSegment? = null
) {
    val context = LocalContext.current
    var timeText by remember { mutableStateOf("") }
    var batteryText by remember { mutableStateOf("") }
    var dateText by remember { mutableStateOf("") }
    
    LaunchedEffect(timeFormat, showSeconds, config) {
        while (true) {
            timeText = formatSystemTime(timeFormat, showSeconds)
            dateText = formatSystemDate()
            batteryText = "${getSystemBatteryLevel(context)}%"
            delay(if (showSeconds) 500L else 5000L)
        }
    }

    if (splitSegment != null) {
        if (splitSegment == SplitSegment.LEFT) {
            Row(
                modifier = Modifier.fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = timeText,
                    color = color,
                    fontSize = (13f + textSizeOffset).sp,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxHeight(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = batteryText,
                    color = color,
                    fontSize = (13f + textSizeOffset).sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        return
    }

    val displayString = remember(config, timeText, dateText, batteryText) {
        val parts = mutableListOf<String>()
        parts.add(timeText)
        if (config.contains("Date")) parts.add(dateText)
        if (config.contains("Battery")) parts.add(batteryText)
        parts.joinToString("  •  ")
    }

    Row(
        modifier = Modifier
            .fillMaxHeight()
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = displayString,
            color = color,
            fontSize = (13f + textSizeOffset).sp,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
fun NovaBarUi() {
    val context = LocalContext.current
    val viewModel: OverlayViewModel = viewModel(
        factory = ViewModelFactory(context)
    )

    val activeStateKey by remember {
        viewModel.activeState.map { state ->
            when (state) {
                is OverlayState.Idle -> "Idle"
                is OverlayState.Charging -> "Charging"
                is OverlayState.Notification -> "Notification"
                is OverlayState.Timer -> "Timer"
                is OverlayState.Stopwatch -> "Stopwatch"
                is OverlayState.Navigation -> "Navigation"
                is OverlayState.Media -> "Media"
                is OverlayState.PhoneCall -> "PhoneCall"
                is OverlayState.Torch -> "Torch"
                is OverlayState.Hotspot -> "Hotspot"
                is OverlayState.VoiceRecorder -> "VoiceRecorder"
                is OverlayState.NovaGuy -> "NovaGuy"
            }
        }.distinctUntilChanged()
    }.collectAsState(initial = "Idle")
    val settings by viewModel.settingsFlow.collectAsState()
    val isDarkBgFromLuminance by viewModel.isDarkBackground.collectAsState()
    val baseBackgroundColor = when (settings.pillAppearanceStyle) {
        "Dark" -> Color(0xFF1C1C1E)
        "Light" -> Color(0xFFFFFFFF)
        "Monet" -> MaterialTheme.colorScheme.surfaceVariant
        "Custom" -> Color(settings.pillCustomColor)
        else -> { // Auto
            val isDarkBgAuto = if (settings.colorAdaptationEnabled) isDarkBgFromLuminance else androidx.compose.foundation.isSystemInDarkTheme()
            if (isDarkBgAuto) Color(0xFF1C1C1E) else Color(0xFFFFFFFF)
        }
    }
    val isDarkBg = (0.2126f * baseBackgroundColor.red + 0.7152f * baseBackgroundColor.green + 0.0722f * baseBackgroundColor.blue) <= 0.5f
    val isExpanded by OverlayStateManager.isExpanded.collectAsState()
    val activeList by remember {
        OverlayStateManager.activeActivities.map { list ->
            list.map { it::class.java.simpleName }
        }.distinctUntilChanged()
    }.collectAsState(initial = emptyList())
    val activeActivities by OverlayStateManager.activeActivities.collectAsState()
    val expandedActivityKey by OverlayStateManager.expandedActivityKey.collectAsState()

    LaunchedEffect(isExpanded) {
        if (isExpanded && activeStateKey == "Media") {
            val elapsed = System.currentTimeMillis() - DiagnosticsManager.expandClickTime
            Log.d("NovaBar", "MEDIA_EXPAND_ANIMATION_STARTED: elapsed=${elapsed}ms")
        }
    }

    var userInteractionTick by remember { mutableStateOf(0L) }

    // Collapse Sequencing state for Charging view
    var resolvedExpandedCharging by remember { mutableStateOf(isExpanded) }
    val contentAlpha = remember { androidx.compose.animation.core.Animatable(if (isExpanded) 1f else 0f) }
    val expansionFraction by animateFloatAsState(
        targetValue = if (isExpanded) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 850f),
        label = "expansionFraction"
    )

    LaunchedEffect(isExpanded, activeStateKey) {
        if (activeStateKey == "Charging") {
            if (isExpanded) {
                resolvedExpandedCharging = true
                contentAlpha.animateTo(1f, animationSpec = tween(250))
            } else {
                contentAlpha.animateTo(0f, animationSpec = tween(250))
                resolvedExpandedCharging = false
            }
        } else {
            resolvedExpandedCharging = isExpanded
            contentAlpha.snapTo(if (isExpanded) 1f else 0f)
        }
    }

    val targetState = when (activeStateKey) {
        "Media" -> {
            when (settings.defaultPresentationMode) {
                "Minimized" -> if (isExpanded) NowBarState.EXPANDED else NowBarState.MINIMIZED
                "Expanded" -> NowBarState.EXPANDED
                else -> if (isExpanded) NowBarState.EXPANDED else NowBarState.COMPACT
            }
        }
        "Charging" -> {
            when (settings.defaultPresentationMode) {
                "Minimized" -> if (resolvedExpandedCharging) NowBarState.EXPANDED else NowBarState.MINIMIZED
                "Expanded" -> NowBarState.EXPANDED
                else -> if (resolvedExpandedCharging) NowBarState.EXPANDED else NowBarState.COMPACT
            }
        }
        else -> {
            when (settings.defaultPresentationMode) {
                "Minimized" -> if (isExpanded) NowBarState.EXPANDED else NowBarState.MINIMIZED
                "Expanded" -> NowBarState.EXPANDED
                else -> if (isExpanded) NowBarState.EXPANDED else NowBarState.COMPACT
            }
        }
    }

    var lastTargetState by remember { mutableStateOf<NowBarState?>(null) }
    LaunchedEffect(targetState) {
        val prev = lastTargetState
        lastTargetState = targetState
        
        if (prev != null) {
            val isGrowing = when {
                targetState == NowBarState.EXPANDED -> true
                targetState == NowBarState.COMPACT && prev == NowBarState.MINIMIZED -> true
                else -> false
            }
            if (isGrowing) {
                val modeStr = when (targetState) {
                    NowBarState.MINIMIZED -> "Minimized"
                    NowBarState.COMPACT -> "Compact"
                    NowBarState.EXPANDED -> "Expanded"
                }
                OverlayStateManager.windowMode.value = modeStr
            }
        } else {
            val modeStr = when (targetState) {
                NowBarState.MINIMIZED -> "Minimized"
                NowBarState.COMPACT -> "Compact"
                NowBarState.EXPANDED -> "Expanded"
            }
            OverlayStateManager.windowMode.value = modeStr
        }

        if (prev != null && prev != targetState) {
            Log.d("NovaBar", "CURRENT_STATE: $prev, TARGET_STATE: $targetState")
        } else if (prev == null) {
            Log.d("NovaBar", "CURRENT_STATE: null, TARGET_STATE: $targetState")
        }
    }

    val textSizeOffset = settings.pillTextSize

    // Palette Color Extraction glass tint (Asynchronous)
    var extractedColor by remember { mutableStateOf<Color?>(null) }
    val albumArtState by remember {
        viewModel.activeState.map { state ->
            (state as? OverlayState.Media)?.data?.albumArt
        }.distinctUntilChanged()
    }.collectAsState(initial = null)

    LaunchedEffect(albumArtState) {
        val art = albumArtState
        if (art != null) {
            val color = withContext(Dispatchers.Default) {
                try {
                    val palette = androidx.palette.graphics.Palette.from(art).generate()
                    val colorVal = palette.getMutedColor(palette.getDominantColor(0))
                    if (colorVal != 0) Color(colorVal) else null
                } catch (e: Exception) {
                    null
                }
            }
            extractedColor = color
        } else {
            extractedColor = null
        }
    }

    LaunchedEffect(isExpanded, activeStateKey, userInteractionTick, settings.autoCollapseTimeout, activeActivities.size, expandedActivityKey) {
        if (isExpanded && activeStateKey != "Idle") {
            val isMultiDashboardOverview = activeActivities.size > 1 && expandedActivityKey == null
            if (!isMultiDashboardOverview) {
                delay(settings.autoCollapseTimeout * 1000L)
                OverlayStateManager.collapse()
            }
        }
    }

    // Dynamic color styling
    val foregroundColor by animateColorAsState(
        targetValue = if (isDarkBg) Color.White else Color.Black,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy),
        label = "ForegroundColor"
    )

    val targetColor = baseBackgroundColor.copy(alpha = settings.opacity)
    
    // Apply glass tint composite color
    val extColor = extractedColor
    val backgroundColor by animateColorAsState(
        targetValue = if (extColor != null) {
            val tintOpacity = 0.12f
            Color(
                red = (targetColor.red * (1f - tintOpacity) + extColor.red * tintOpacity),
                green = (targetColor.green * (1f - tintOpacity) + extColor.green * tintOpacity),
                blue = (targetColor.blue * (1f - tintOpacity) + extColor.blue * tintOpacity),
                alpha = targetColor.alpha
            )
        } else {
            targetColor
        },
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy),
        label = "BackgroundColor"
    )

    val borderColor by animateColorAsState(
        targetValue = if (isDarkBg) GlassBorderDark else GlassBorderLight,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy),
        label = "BorderColor"
    )

    val systemBarVisible by OverlayStateManager.systemBarVisible.collectAsState()
    val isLocked by OverlayStateManager.isScreenLocked.collectAsState()
    val isStatusBarSyncVisible = if (settings.followStatusBarVisibility && !isLocked) systemBarVisible else true
    val showOverlay = (activeStateKey != "Idle" || settings.alwaysOnBar) && isStatusBarSyncVisible



    // Dimensions
    val borderThickness = settings.barBorderThickness.dp

    val baseTargetWidth = when (targetState) {
        NowBarState.MINIMIZED -> (115 * settings.barWidthScale).dp
        NowBarState.COMPACT -> (185 * settings.barWidthScale).dp
        NowBarState.EXPANDED -> 290.dp
    }

    val cameraCutoutModeEnabled = settings.cameraCutoutMode
    val hasCenteredPunchHole by com.novabar.app.utils.CutoutManager.hasCenteredPunchHole.collectAsState()
    
    val isSplitLayout = cameraCutoutModeEnabled && (targetState == NowBarState.MINIMIZED || targetState == NowBarState.COMPACT) && expansionFraction == 0f
    
    val density = LocalContext.current.resources.displayMetrics.density
    val cutoutWidthPx = com.novabar.app.utils.CutoutManager.cutoutWidth.collectAsState().value
    val cutoutWidthDp = (cutoutWidthPx / density).dp
    val safetyPadding = 12.dp
    val baseGap = if (hasCenteredPunchHole && cutoutWidthPx > 0) {
        cutoutWidthDp + safetyPadding
    } else {
        36.dp
    }

    val targetGap = baseGap * settings.cameraCutoutGapScale

    // Gap Animation:
    val gapWidth by animateDpAsState(
        targetValue = targetGap,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 850f),
        label = "cameraGap"
    )

    val animatedLeftSegmentWidth by animateDpAsState(
        targetValue = if (isSplitLayout) (settings.leftSegmentWidthDp * settings.barWidthScale).dp else settings.leftSegmentWidthDp.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 850f),
        label = "leftSegmentWidth"
    )

    val animatedRightSegmentWidth by animateDpAsState(
        targetValue = if (isSplitLayout) (settings.rightSegmentWidthDp * settings.barWidthScale).dp else settings.rightSegmentWidthDp.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 850f),
        label = "rightSegmentWidth"
    )

    val targetWidth = if (isSplitLayout) {
        (settings.leftSegmentWidthDp * settings.barWidthScale).dp + targetGap + (settings.rightSegmentWidthDp * settings.barWidthScale).dp
    } else {
        baseTargetWidth
    }

    val targetHeight = when (targetState) {
        NowBarState.MINIMIZED -> (38 + settings.barHeightPadding).dp.coerceAtLeast(24.dp)
        NowBarState.COMPACT -> (44 + settings.barHeightPadding).dp.coerceAtLeast(30.dp)
        NowBarState.EXPANDED -> {
            val activeCount = activeActivities.size
            if (activeCount > 1) {
                if (expandedActivityKey != null) {
                    205.dp
                } else {
                    val totalCompactHeight = activeActivities.sumOf { state ->
                        val h: Int = when (state) {
                            is OverlayState.Navigation -> 56
                            is OverlayState.Media -> 52
                            else -> 48
                        }
                        h
                    }
                    val spacing = 8
                    val dashHeight = totalCompactHeight + (activeCount - 1) * spacing
                    maxOf(205, dashHeight).dp
                }
            } else {
                205.dp
            }
        }
    }

    val animatedWidth by animateDpAsState(
        targetValue = targetWidth,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 850f),
        label = "animatedWidth",
        finishedListener = { width ->
            if (targetState == NowBarState.EXPANDED && width == 290.dp) {
                val elapsed = System.currentTimeMillis() - DiagnosticsManager.expandClickTime
                Log.d("NovaBar", "MEDIA_EXPAND_COMPLETE: elapsed=${elapsed}ms")
            }
            
            val modeStr = when (targetState) {
                NowBarState.MINIMIZED -> "Minimized"
                NowBarState.COMPACT -> "Compact"
                NowBarState.EXPANDED -> "Expanded"
            }
            if (OverlayStateManager.windowMode.value != modeStr) {
                OverlayStateManager.windowMode.value = modeStr
            }
        }
    )

    val animatedHeight by animateDpAsState(
        targetValue = targetHeight,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 850f),
        label = "animatedHeight"
    )

    // Shape Interpolation: animate corner radius continuously between expanded and compact
    val compactCornerRadius = settings.cornerRadius.dp
    val expandedCornerRadius = 28.dp
    val targetCornerRadius = when (targetState) {
        NowBarState.EXPANDED -> expandedCornerRadius
        else -> compactCornerRadius
    }
    val animatedCornerRadius by animateDpAsState(
        targetValue = targetCornerRadius,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 850f),
        label = "animatedCornerRadius"
    )

    val batteryPercent by remember {
        viewModel.activeState.map { state ->
            (state as? OverlayState.Charging)?.data?.batteryPercentage ?: 0
        }.distinctUntilChanged()
    }.collectAsState(initial = 0)

    val animatedPercent by animateFloatAsState(
        targetValue = batteryPercent.toFloat(),
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "batteryPercent"
    )

    // Wave Continuity: shared phase to prevent jumps during layout switches
    val chargingTransition = rememberInfiniteTransition(label = "chargingWaveGlobal")
    val chargingWavePhase by chargingTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "chargingWavePhase"
    )

    // Expose cameraCutoutModeEnabled in DiagnosticsManager
    LaunchedEffect(settings.cameraCutoutMode) {
        DiagnosticsManager.cameraCutoutModeEnabled.value = settings.cameraCutoutMode
    }

    LaunchedEffect(settings.cameraCutoutGapScale, gapWidth) {
        DiagnosticsManager.cutoutGapScale.value = settings.cameraCutoutGapScale
        DiagnosticsManager.finalGapWidth.value = (gapWidth.value * density).roundToInt()
    }

    val showDebug by DiagnosticsManager.showDebugMarkers.collectAsState()
    val winX by DiagnosticsManager.windowX.collectAsState()
    val winY by DiagnosticsManager.windowY.collectAsState()
    val winWidth by DiagnosticsManager.windowWidth.collectAsState()
    val winHeight by DiagnosticsManager.windowHeight.collectAsState()
    val centerX = winX + winWidth / 2
    val centerY = winY + winHeight / 2

    val rootAlignment = when (settings.barGravity) {
        "Left" -> Alignment.TopStart
        "Right" -> Alignment.TopEnd
        else -> Alignment.TopCenter
    }

    val springSpecFloat = spring<Float>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 850f
    )
    val springSpecIntOffset = spring<IntOffset>(
        dampingRatio = Spring.DampingRatioNoBouncy,
        stiffness = 850f
    )
    val translationOffsetPx = (8 * density).roundToInt()

    AnimatedVisibility(
        visible = showOverlay,
        enter = fadeIn(animationSpec = springSpecFloat) + slideInVertically(
            initialOffsetY = { -translationOffsetPx },
            animationSpec = springSpecIntOffset
        ),
        exit = fadeOut(animationSpec = springSpecFloat) + slideOutVertically(
            targetOffsetY = { -translationOffsetPx },
            animationSpec = springSpecIntOffset
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = rootAlignment
        ) {

            val activeStateMap = remember {
                mutableStateMapOf<String, OverlayState>().apply {
                    val initial = viewModel.activeState.value
                    val key = when (initial) {
                        is OverlayState.Idle -> "Idle"
                        is OverlayState.Charging -> "Charging"
                        is OverlayState.Notification -> "Notification"
                        is OverlayState.Timer -> "Timer"
                        is OverlayState.Stopwatch -> "Stopwatch"
                        is OverlayState.Navigation -> "Navigation"
                        is OverlayState.Media -> "Media"
                        is OverlayState.PhoneCall -> "PhoneCall"
                        is OverlayState.Torch -> "Torch"
                        is OverlayState.Hotspot -> "Hotspot"
                        is OverlayState.VoiceRecorder -> "VoiceRecorder"
                        is OverlayState.NovaGuy -> "NovaGuy"
                    }
                    put(key, initial)
                }
            }
            LaunchedEffect(Unit) {
                viewModel.activeState.collect { state ->
                    val key = when (state) {
                        is OverlayState.Idle -> "Idle"
                        is OverlayState.Charging -> "Charging"
                        is OverlayState.Notification -> "Notification"
                        is OverlayState.Timer -> "Timer"
                        is OverlayState.Stopwatch -> "Stopwatch"
                        is OverlayState.Navigation -> "Navigation"
                        is OverlayState.Media -> "Media"
                        is OverlayState.PhoneCall -> "PhoneCall"
                        is OverlayState.Torch -> "Torch"
                        is OverlayState.Hotspot -> "Hotspot"
                        is OverlayState.VoiceRecorder -> "VoiceRecorder"
                        is OverlayState.NovaGuy -> "NovaGuy"
                    }
                    activeStateMap[key] = state
                }
            }

            @Composable
            fun RenderSegmentContent(segment: SplitSegment?, key: String) {
                val state = activeStateMap[key]
                if (state != null) {
                    val stateTargetState = when (key) {
                        "Media" -> {
                            when (settings.defaultPresentationMode) {
                                "Minimized" -> if (isExpanded) NowBarState.EXPANDED else NowBarState.MINIMIZED
                                "Expanded" -> NowBarState.EXPANDED
                                else -> if (isExpanded) NowBarState.EXPANDED else NowBarState.COMPACT
                            }
                        }
                        "Charging" -> {
                            when (settings.defaultPresentationMode) {
                                "Minimized" -> if (resolvedExpandedCharging) NowBarState.EXPANDED else NowBarState.MINIMIZED
                                "Expanded" -> NowBarState.EXPANDED
                                else -> if (resolvedExpandedCharging) NowBarState.EXPANDED else NowBarState.COMPACT
                            }
                        }
                        else -> {
                            when (settings.defaultPresentationMode) {
                                "Minimized" -> if (isExpanded) NowBarState.EXPANDED else NowBarState.MINIMIZED
                                "Expanded" -> NowBarState.EXPANDED
                                else -> if (isExpanded) NowBarState.EXPANDED else NowBarState.COMPACT
                            }
                        }
                    }
                    val stateTargetWidth = when (stateTargetState) {
                        NowBarState.MINIMIZED -> (115 * settings.barWidthScale).dp
                        NowBarState.COMPACT -> (185 * settings.barWidthScale).dp
                        NowBarState.EXPANDED -> 290.dp
                    }
                    val stateTargetHeight = when (stateTargetState) {
                        NowBarState.MINIMIZED -> (38 + settings.barHeightPadding).dp.coerceAtLeast(24.dp)
                        NowBarState.COMPACT -> (44 + settings.barHeightPadding).dp.coerceAtLeast(30.dp)
                        NowBarState.EXPANDED -> 205.dp
                    }

                    Box(
                        modifier = if (segment != null) Modifier.fillMaxSize() else Modifier.requiredSize(stateTargetWidth, stateTargetHeight),
                        contentAlignment = Alignment.Center
                    ) {
                        when (state) {
                            is OverlayState.PhoneCall -> PhoneCallView(state.data, stateTargetState, foregroundColor, textSizeOffset, segment) {
                                userInteractionTick = System.currentTimeMillis()
                            }
                            is OverlayState.Charging -> ChargingPill(
                                state = state.data,
                                currentState = stateTargetState,
                                color = foregroundColor,
                                textSizeOffset = textSizeOffset,
                                contentAlpha = contentAlpha.value,
                                splitSegment = segment
                            )
                            is OverlayState.Notification -> NotificationView(
                                state.data, stateTargetState, viewModel, foregroundColor, textSizeOffset, splitSegment = segment
                            )
                            is OverlayState.Timer -> TimerView(state.data, stateTargetState, foregroundColor, settings.showSeconds, textSizeOffset, splitSegment = segment) {
                                userInteractionTick = System.currentTimeMillis()
                            }
                            is OverlayState.Stopwatch -> StopwatchView(state.data, stateTargetState, foregroundColor, settings.showSeconds, textSizeOffset, splitSegment = segment) {
                                userInteractionTick = System.currentTimeMillis()
                            }
                            is OverlayState.Navigation -> NavigationView(state.data, stateTargetState, foregroundColor, settings.timeFormat, textSizeOffset, splitSegment = segment)
                            is OverlayState.Media -> {
                                MediaView(
                                    state = state.data,
                                    currentState = stateTargetState,
                                    color = foregroundColor,
                                    albumArtCornerRadius = settings.albumArtCornerRadius,
                                    visualizerStyle = settings.visualizerStyle,
                                    visualizerSensitivity = settings.visualizerSensitivity,
                                    progressVisibility = settings.progressVisibility,
                                    splitSegment = segment,
                                    textSizeOffset = textSizeOffset,
                                    onSeekTo = { posMs ->
                                        com.novabar.app.services.NovaNotificationListener.seekTo(posMs)
                                    },
                                    onInteraction = {
                                        userInteractionTick = System.currentTimeMillis()
                                    }
                                )
                            }
                            is OverlayState.Torch -> {
                                TorchView(
                                    state = state.data,
                                    currentState = stateTargetState,
                                    color = foregroundColor,
                                    textSizeOffset = textSizeOffset,
                                    splitSegment = segment,
                                    onInteraction = {
                                        userInteractionTick = System.currentTimeMillis()
                                    }
                                )
                            }
                            is OverlayState.Hotspot -> {
                                HotspotView(
                                    state = state.data,
                                    currentState = stateTargetState,
                                    color = foregroundColor,
                                    textSizeOffset = textSizeOffset,
                                    splitSegment = segment,
                                    onTurnOffClick = {
                                        viewModel.disableHotspot(context)
                                    }
                                )
                            }
                            is OverlayState.VoiceRecorder -> {
                                VoiceRecorderView(
                                    state = state.data,
                                    currentState = stateTargetState,
                                    color = foregroundColor,
                                    textSizeOffset = textSizeOffset,
                                    splitSegment = segment
                                ) {
                                    userInteractionTick = System.currentTimeMillis()
                                }
                            }
                            is OverlayState.Idle -> {
                                if (settings.alwaysOnBar) {
                                    AlwaysOnView(settings.alwaysOnConfig, settings.timeFormat, settings.showSeconds, foregroundColor, textSizeOffset, segment)
                                } else {
                                    Row(modifier = Modifier.padding(horizontal = 14.dp)) {
                                        Text("Ready", color = foregroundColor, fontSize = (12f + textSizeOffset).sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                            is OverlayState.NovaGuy -> {
                                if (stateTargetState == NowBarState.EXPANDED) {
                                    NovaGuyExpandedView(foregroundColor)
                                } else {
                                    val currentWidth = if (segment == SplitSegment.LEFT) animatedLeftSegmentWidth else animatedRightSegmentWidth
                                    if (segment == SplitSegment.LEFT) {
                                        NovaGuyCompactView(foregroundColor, currentWidth)
                                    } else {
                                        RightSegmentContextView(
                                            isLocked = isLocked,
                                            timeFormat = settings.timeFormat,
                                            showSeconds = settings.showSeconds,
                                            color = foregroundColor,
                                            textSizeOffset = textSizeOffset
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            val gesturesModifier = if (isExpanded && activeActivities.size > 1) Modifier else Modifier.pointerInput(activeList, activeStateKey) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        var dragX = 0f
                        var isSwipe = false
                        var isLongPress = false
                        val pointerId = down.id
                        val touchSlop = viewConfiguration.touchSlop
                        
                        val longPressResult = withTimeoutOrNull(500L) {
                            var finished = false
                            while (!finished) {
                                val event = awaitPointerEvent()
                                val dragChange = event.changes.firstOrNull { it.id == pointerId }
                                if (dragChange != null) {
                                    if (!dragChange.pressed) {
                                        finished = true
                                    } else {
                                        val diffX = dragChange.position.x - dragChange.previousPosition.x
                                        dragX += diffX
                                        if (kotlin.math.abs(dragX) > touchSlop) {
                                            isSwipe = true
                                            dragChange.consume()
                                            finished = true
                                        }
                                    }
                                } else {
                                    finished = true
                                }
                            }
                        }
                        
                        if (longPressResult == null && !isSwipe) {
                            isLongPress = true
                            if (activeStateKey == "Torch") {
                                Log.d("NovaBar", "TORCH_LONG_PRESS_DETECTED")
                                com.novabar.app.utils.TorchManager.setTorchEnabled(false)
                            }
                            do {
                                val event = awaitPointerEvent()
                                event.changes.forEach { it.consume() }
                            } while (event.changes.any { it.pressed })
                        } else if (isSwipe) {
                            do {
                                val event = awaitPointerEvent()
                                val dragChange = event.changes.firstOrNull { it.id == pointerId }
                                if (dragChange != null) {
                                    if (dragChange.pressed) {
                                        val diffX = dragChange.position.x - dragChange.previousPosition.x
                                        dragX += diffX
                                        dragChange.consume()
                                    }
                                }
                            } while (event.changes.any { it.pressed })
                            
                            if (dragX > 60f) {
                                Log.d("NovaBar-SwipeDebug", "1. Swipe Right gesture detected. dragX=$dragX")
                                OverlayStateManager.swipeRight()
                                userInteractionTick = System.currentTimeMillis()
                            } else if (dragX < -60f) {
                                Log.d("NovaBar-SwipeDebug", "1. Swipe Left gesture detected. dragX=$dragX")
                                OverlayStateManager.swipeLeft()
                                userInteractionTick = System.currentTimeMillis()
                            }
                        } else {
                            // Tap detected!
                            // Since the finger was already released within 500ms to exit longPressResult,
                            // we can execute the tap/expand action immediately.
                            if (activeStateKey != "Idle") {
                                val currentStateObj = viewModel.activeState.value
                                var canExpand = true
                                if (currentStateObj is OverlayState.Hotspot && !currentStateObj.data.isDisableSupported) {
                                    canExpand = false
                                }
                                if (currentStateObj is OverlayState.NovaGuy && !settings.enableNovaGuyMessages) {
                                    canExpand = false
                                }
                                if (canExpand) {
                                    if (activeStateKey == "Media") {
                                        DiagnosticsManager.expandClickTime = System.currentTimeMillis()
                                        Log.d("NovaBar", "MEDIA_EXPAND_CLICK")
                                    }
                                    OverlayStateManager.expand()
                                    userInteractionTick = System.currentTimeMillis()
                                }
                            }
                        }
                    }
                }
            }

            val isMultiDashboard = activeActivities.size > 1 && (isExpanded || expansionFraction > 0.01f)
            val containerAlpha = 1f - expansionFraction
            val currentBorderColor = if (isMultiDashboard) {
                borderColor.copy(alpha = containerAlpha)
            } else if (isSplitLayout) {
                borderColor
            } else {
                Color.Transparent
            }

            val currentBorderThickness = if (isMultiDashboard) {
                borderThickness * containerAlpha
            } else if (isSplitLayout) {
                borderThickness
            } else {
                0.dp
            }

            val currentBackgroundColor = if (isMultiDashboard) {
                backgroundColor.copy(alpha = backgroundColor.alpha * containerAlpha)
            } else if (isSplitLayout) {
                backgroundColor
            } else {
                Color.Transparent
            }
            val currentCornerRadius = animatedCornerRadius

            Box(
                modifier = Modifier
                    .width(animatedWidth)
                    .height(animatedHeight)
                    .onGloballyPositioned { coordinates ->
                        val rect = coordinates.boundsInWindow()
                        OverlayStateManager.pillBounds.value = android.graphics.Rect(
                            rect.left.roundToInt(),
                            rect.top.roundToInt(),
                            rect.right.roundToInt(),
                            rect.bottom.roundToInt()
                        )
                    }
                    .border(currentBorderThickness, currentBorderColor, RoundedCornerShape(currentCornerRadius))
                    .clip(RoundedCornerShape(currentCornerRadius))
                    .background(currentBackgroundColor)
                    .then(gesturesModifier),
                contentAlignment = Alignment.Center
            ) {
                if (isMultiDashboard) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        activeActivities.forEachIndexed { index, activity ->
                            val activityKey = when (activity) {
                                is OverlayState.Navigation -> "Navigation"
                                is OverlayState.Media -> "Media"
                                is OverlayState.Timer -> "Timer"
                                is OverlayState.Stopwatch -> "Stopwatch"
                                is OverlayState.Torch -> "Torch"
                                is OverlayState.Hotspot -> "Hotspot"
                                is OverlayState.Charging -> "Charging"
                                is OverlayState.PhoneCall -> "PhoneCall"
                                is OverlayState.Notification -> "Notification"
                                is OverlayState.VoiceRecorder -> "VoiceRecorder"
                                is OverlayState.Idle -> "Idle"
                                is OverlayState.NovaGuy -> "NovaGuy"
                            }

                            key(activityKey) {
                                val isThisCardExpanded = expandedActivityKey == activityKey

                                val targetCardY = if (expandedActivityKey == null) {
                                    val prevCompactHeightSum = activeActivities.take(index).sumOf { state ->
                                        val h: Int = when (state) {
                                            is OverlayState.Navigation -> 56
                                            is OverlayState.Media -> 52
                                            is OverlayState.VoiceRecorder -> 52
                                            else -> 48
                                        }
                                        h
                                    }
                                    val spacing = 8
                                    (prevCompactHeightSum + index * spacing).dp
                                } else {
                                    if (isThisCardExpanded) {
                                        0.dp
                                    } else {
                                        val expandedIndex = activeActivities.indexOfFirst { act ->
                                            val actKey = when (act) {
                                                is OverlayState.Navigation -> "Navigation"
                                                is OverlayState.Media -> "Media"
                                                is OverlayState.Timer -> "Timer"
                                                is OverlayState.Stopwatch -> "Stopwatch"
                                                is OverlayState.Torch -> "Torch"
                                                is OverlayState.Hotspot -> "Hotspot"
                                                is OverlayState.Charging -> "Charging"
                                                is OverlayState.PhoneCall -> "PhoneCall"
                                                is OverlayState.Notification -> "Notification"
                                                is OverlayState.VoiceRecorder -> "VoiceRecorder"
                                                is OverlayState.Idle -> "Idle"
                                                is OverlayState.NovaGuy -> "NovaGuy"
                                            }
                                            actKey == expandedActivityKey
                                        }
                                        if (expandedIndex > index) {
                                            0.dp
                                        } else {
                                            205.dp
                                        }
                                    }
                                }

                                val targetCardHeight = if (expandedActivityKey == null) {
                                    when (activity) {
                                        is OverlayState.Navigation -> 56.dp
                                        is OverlayState.Media -> 52.dp
                                        is OverlayState.VoiceRecorder -> 52.dp
                                        else -> 48.dp
                                    }
                                } else {
                                    if (isThisCardExpanded) 205.dp else 0.dp
                                }

                                val animatedCardY by animateDpAsState(
                                    targetValue = targetCardY,
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 850f),
                                    label = "animatedCardY_${activityKey}"
                                )
                                val animatedCardHeight by animateDpAsState(
                                    targetValue = targetCardHeight,
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 850f),
                                    label = "animatedCardHeight_${activityKey}"
                                )

                                if (animatedCardHeight > 4.dp) {
                                    val cardCornerRadius = if (isThisCardExpanded) expandedCornerRadius else compactCornerRadius
                                    val animatedCardCornerRadius by animateDpAsState(
                                        targetValue = cardCornerRadius,
                                        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 850f),
                                        label = "animatedCardCornerRadius_${activityKey}"
                                    )

                                    val cardBackgroundColor = baseBackgroundColor.copy(alpha = settings.opacity)

                                    Box(
                                        modifier = Modifier
                                            .offset(y = animatedCardY)
                                            .requiredSize(width = 290.dp, height = animatedCardHeight)
                                            .graphicsLayer { alpha = expansionFraction }
                                            .shadow(elevation = 2.dp, shape = RoundedCornerShape(animatedCardCornerRadius))
                                            .clip(RoundedCornerShape(animatedCardCornerRadius))
                                            .background(cardBackgroundColor)
                                            .clickable(enabled = expandedActivityKey == null && activityKey != "NovaGuy") {
                                                OverlayStateManager.expandedActivityKey.value = activityKey
                                                userInteractionTick = System.currentTimeMillis()
                                            }
                                    ) {
                                        val presentationMode = if (isThisCardExpanded) NowBarState.EXPANDED else NowBarState.COMPACT
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            when (activity) {
                                                is OverlayState.PhoneCall -> PhoneCallView(activity.data, presentationMode, foregroundColor, textSizeOffset, null) {
                                                    userInteractionTick = System.currentTimeMillis()
                                                }
                                                is OverlayState.Charging -> ChargingPill(
                                                    state = activity.data,
                                                    currentState = presentationMode,
                                                    color = foregroundColor,
                                                    textSizeOffset = textSizeOffset,
                                                    contentAlpha = contentAlpha.value,
                                                    splitSegment = null
                                                )
                                                is OverlayState.Notification -> NotificationView(
                                                    activity.data, presentationMode, viewModel, foregroundColor, textSizeOffset, splitSegment = null
                                                )
                                                is OverlayState.Timer -> TimerView(
                                                    state = activity.data,
                                                    currentState = presentationMode,
                                                    color = foregroundColor,
                                                    showSeconds = settings.showSeconds,
                                                    textSizeOffset = textSizeOffset,
                                                    splitSegment = null,
                                                    isDashboardCard = true
                                                ) {
                                                    userInteractionTick = System.currentTimeMillis()
                                                }
                                                is OverlayState.Stopwatch -> StopwatchView(
                                                    state = activity.data,
                                                    currentState = presentationMode,
                                                    color = foregroundColor,
                                                    showSeconds = settings.showSeconds,
                                                    textSizeOffset = textSizeOffset,
                                                    splitSegment = null,
                                                    isDashboardCard = true
                                                ) {
                                                    userInteractionTick = System.currentTimeMillis()
                                                }
                                                is OverlayState.Navigation -> NavigationView(
                                                    state = activity.data,
                                                    currentState = presentationMode,
                                                    color = foregroundColor,
                                                    timeFormat = settings.timeFormat,
                                                    textSizeOffset = textSizeOffset,
                                                    splitSegment = null,
                                                    isDashboardCard = true
                                                )
                                                is OverlayState.Media -> {
                                                    MediaView(
                                                        state = activity.data,
                                                        currentState = presentationMode,
                                                        color = foregroundColor,
                                                        albumArtCornerRadius = settings.albumArtCornerRadius,
                                                        visualizerStyle = settings.visualizerStyle,
                                                        visualizerSensitivity = settings.visualizerSensitivity,
                                                        progressVisibility = settings.progressVisibility,
                                                        splitSegment = null,
                                                        textSizeOffset = textSizeOffset,
                                                        onSeekTo = { posMs ->
                                                            com.novabar.app.services.NovaNotificationListener.seekTo(posMs)
                                                        },
                                                        onInteraction = {
                                                            userInteractionTick = System.currentTimeMillis()
                                                        }
                                                    )
                                                }
                                                is OverlayState.Torch -> {
                                                    TorchView(
                                                        state = activity.data,
                                                        currentState = presentationMode,
                                                        color = foregroundColor,
                                                        textSizeOffset = textSizeOffset,
                                                        splitSegment = null,
                                                        onInteraction = {
                                                            userInteractionTick = System.currentTimeMillis()
                                                        }
                                                    )
                                                }
                                                is OverlayState.Hotspot -> {
                                                    HotspotView(
                                                        state = activity.data,
                                                        currentState = presentationMode,
                                                        color = foregroundColor,
                                                        textSizeOffset = textSizeOffset,
                                                        splitSegment = null,
                                                        onTurnOffClick = {
                                                            viewModel.disableHotspot(context)
                                                        }
                                                    )
                                                }
                                                 is OverlayState.VoiceRecorder -> {
                                                     VoiceRecorderView(
                                                         state = activity.data,
                                                         currentState = presentationMode,
                                                         color = foregroundColor,
                                                         textSizeOffset = textSizeOffset,
                                                         splitSegment = null,
                                                         isDashboardCard = true
                                                     ) {
                                                         userInteractionTick = System.currentTimeMillis()
                                                     }
                                                 }
                                                 is OverlayState.Idle -> {
                                                    if (settings.alwaysOnBar) {
                                                        AlwaysOnView(settings.alwaysOnConfig, settings.timeFormat, settings.showSeconds, foregroundColor, textSizeOffset, null)
                                                    } else {
                                                        Row(modifier = Modifier.padding(horizontal = 14.dp)) {
                                                            Text("Ready", color = foregroundColor, fontSize = (12f + textSizeOffset).sp, fontWeight = FontWeight.Bold)
                                                        }
                                                    }
                                                }
                                                is OverlayState.NovaGuy -> {
                                                     val selectedMessage by OverlayStateManager.selectedNovaGuyMessage.collectAsState()
                                                     val context = androidx.compose.ui.platform.LocalContext.current
                                                     val messageText = remember(selectedMessage) {
                                                         selectedMessage?.let { context.getString(it.textResId) } ?: run {
                                                             val msg = com.novabar.app.domain.NovaGuyMessageProvider.selectRandomMessage(
                                                                 com.novabar.app.domain.DeviceContext(
                                                                     hourOfDay = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY),
                                                                     isCharging = false,
                                                                     batteryPercentage = 100,
                                                                     isMusicPlaying = false
                                                                 ),
                                                                 settings.contextAwareMessagesEnabled
                                                             )
                                                             context.getString(msg.textResId)
                                                         }
                                                     }

                                                     val eyeOffset = remember { androidx.compose.animation.core.Animatable(0f) }
                                                     LaunchedEffect(Unit) {
                                                         while (true) {
                                                             delay(kotlin.random.Random.nextLong(5000, 10000))
                                                             val glanceDirection = listOf(-6f, 6f).random()
                                                             eyeOffset.animateTo(
                                                                 targetValue = glanceDirection,
                                                                 animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
                                                             )
                                                             delay(kotlin.random.Random.nextLong(1000, 2000))
                                                             eyeOffset.animateTo(
                                                                 targetValue = 0f,
                                                                 animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
                                                             )
                                                         }
                                                     }

                                                     Row(
                                                         modifier = Modifier
                                                             .fillMaxSize()
                                                             .padding(horizontal = 16.dp),
                                                         verticalAlignment = Alignment.CenterVertically
                                                     ) {
                                                         Box(
                                                             modifier = Modifier
                                                                 .width(44.dp)
                                                                 .fillMaxHeight(),
                                                             contentAlignment = Alignment.Center
                                                         ) {
                                                             Canvas(modifier = Modifier.size(width = 40.dp, height = 20.dp)) {
                                                                  val density = this.density
                                                                  val offsetPx = eyeOffset.value * density
                                                                  
                                                                  val centerY = size.height / 2f
                                                                  val centerX = size.width / 2f
                                                                  
                                                                  val eyeSpacingPx = 14.dp.toPx()
                                                                  val eyeWidthPx = 3.dp.toPx()
                                                                  val eyeHeightPx = 5.dp.toPx()
                                                                  val eyeCornerRadiusPx = 1.5f.dp.toPx()
                                                                  
                                                                  val leftEyeX = centerX - eyeSpacingPx / 2f + offsetPx
                                                                  val rightEyeX = centerX + eyeSpacingPx / 2f + offsetPx
                                                                  
                                                                  val gapPx = 1.5f.dp.toPx()
                                                                  val connectorThicknessPx = 1.5f.dp.toPx()
                                                                  
                                                                  val leftInnerEdgeX = leftEyeX + (eyeWidthPx / 2f)
                                                                  val rightInnerEdgeX = rightEyeX - (eyeWidthPx / 2f)
                                                                  
                                                                  val connectorStartX = leftInnerEdgeX + gapPx + (connectorThicknessPx / 2f)
                                                                  val connectorEndX = rightInnerEdgeX - gapPx - (connectorThicknessPx / 2f)
                                                                  
                                                                  // Draw floating connecting line with rounded ends and small symmetrical gaps
                                                                  drawLine(
                                                                      color = foregroundColor.copy(alpha = 0.5f),
                                                                      start = Offset(connectorStartX, centerY),
                                                                      end = Offset(connectorEndX, centerY),
                                                                      strokeWidth = connectorThicknessPx,
                                                                      cap = androidx.compose.ui.graphics.StrokeCap.Round
                                                                  )
                                                                  
                                                                  // Draw left capsule eye
                                                                  drawRoundRect(
                                                                      color = foregroundColor,
                                                                      topLeft = Offset(leftEyeX - eyeWidthPx / 2f, centerY - eyeHeightPx / 2f),
                                                                      size = androidx.compose.ui.geometry.Size(eyeWidthPx, eyeHeightPx),
                                                                      cornerRadius = androidx.compose.ui.geometry.CornerRadius(eyeCornerRadiusPx, eyeCornerRadiusPx)
                                                                  )
                                                                  
                                                                  // Draw right capsule eye
                                                                  drawRoundRect(
                                                                      color = foregroundColor,
                                                                      topLeft = Offset(rightEyeX - eyeWidthPx / 2f, centerY - eyeHeightPx / 2f),
                                                                      size = androidx.compose.ui.geometry.Size(eyeWidthPx, eyeHeightPx),
                                                                      cornerRadius = androidx.compose.ui.geometry.CornerRadius(eyeCornerRadiusPx, eyeCornerRadiusPx)
                                                                  )
                                                              }
                                                         }

                                                         Spacer(modifier = Modifier.width(16.dp))

                                                         Text(
                                                             text = messageText,
                                                             color = foregroundColor,
                                                             fontSize = (12f + textSizeOffset).sp,
                                                             fontWeight = FontWeight.Medium,
                                                             textAlign = androidx.compose.ui.text.style.TextAlign.Start,
                                                             modifier = Modifier.weight(1f)
                                                         )
                                                     }
                                                 }
                                            }
                                        }

                                        if (isThisCardExpanded) {
                                            Box(
                                                modifier = Modifier
                                                    .size(36.dp)
                                                    .align(Alignment.TopEnd)
                                                    .clickable {
                                                        OverlayStateManager.expandedActivityKey.value = null
                                                        userInteractionTick = System.currentTimeMillis()
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Canvas(modifier = Modifier.size(12.dp)) {
                                                    val strokeW = 2.dp.toPx()
                                                    drawLine(
                                                        color = foregroundColor.copy(alpha = 0.7f),
                                                        start = Offset(0f, size.height / 2f),
                                                        end = Offset(size.width, size.height / 2f),
                                                        strokeWidth = strokeW
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else {
                    if (isSplitLayout) {
                        val lastTransitionTypeSplit by OverlayStateManager.lastTransitionType.collectAsState()
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .width(animatedLeftSegmentWidth)
                                    .fillMaxHeight(),
                                contentAlignment = Alignment.Center
                            ) {
                                AnimatedContent(
                                    targetState = activeStateKey,
                                    transitionSpec = {
                                        val duration = 350
                                        val transType = OverlayStateManager.lastTransitionType.value
                                        val keyFrom = initialState
                                        val keyTo = targetState
                                        Log.d("NovaBar-SwipeDebug", "Split Left transitionSpec evaluated. targetState: '$keyFrom' -> '$keyTo'. transType: '$transType'")
                                        if (transType == OverlayStateManager.TransitionType.SWIPE_LEFT) {
                                            androidx.compose.animation.slideInHorizontally(animationSpec = tween(duration, easing = androidx.compose.animation.core.LinearOutSlowInEasing)) { width -> width } + fadeIn(animationSpec = tween(duration)) togetherWith
                                            androidx.compose.animation.slideOutHorizontally(animationSpec = tween(duration, easing = androidx.compose.animation.core.FastOutLinearInEasing)) { width -> -width } + fadeOut(animationSpec = tween(duration))
                                        } else if (transType == OverlayStateManager.TransitionType.SWIPE_RIGHT) {
                                            androidx.compose.animation.slideInHorizontally(animationSpec = tween(duration, easing = androidx.compose.animation.core.LinearOutSlowInEasing)) { width -> -width } + fadeIn(animationSpec = tween(duration)) togetherWith
                                            androidx.compose.animation.slideOutHorizontally(animationSpec = tween(duration, easing = androidx.compose.animation.core.FastOutLinearInEasing)) { width -> width } + fadeOut(animationSpec = tween(duration))
                                        } else {
                                            slideInVertically(animationSpec = tween(duration, easing = androidx.compose.animation.core.LinearOutSlowInEasing)) { height -> (height * 0.7f).roundToInt() } + fadeIn(animationSpec = tween(duration)) togetherWith
                                            slideOutVertically(animationSpec = tween(duration, easing = androidx.compose.animation.core.FastOutLinearInEasing)) { height -> (-height * 0.7f).roundToInt() } + fadeOut(animationSpec = tween(duration))
                                        }
                                    },
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) { key ->
                                    RenderSegmentContent(SplitSegment.LEFT, key)
                                }
                            }
                            Spacer(modifier = Modifier.width(gapWidth))
                            Box(
                                modifier = Modifier
                                    .width(animatedRightSegmentWidth)
                                    .fillMaxHeight(),
                                contentAlignment = Alignment.Center
                            ) {
                                AnimatedContent(
                                    targetState = activeStateKey,
                                    transitionSpec = {
                                        val duration = 350
                                        val transType = OverlayStateManager.lastTransitionType.value
                                        val keyFrom = initialState
                                        val keyTo = targetState
                                        Log.d("NovaBar-SwipeDebug", "Split Right transitionSpec evaluated. targetState: '$keyFrom' -> '$keyTo'. transType: '$transType'")
                                        if (transType == OverlayStateManager.TransitionType.SWIPE_LEFT) {
                                            androidx.compose.animation.slideInHorizontally(animationSpec = tween(duration, easing = androidx.compose.animation.core.LinearOutSlowInEasing)) { width -> width } + fadeIn(animationSpec = tween(duration)) togetherWith
                                            androidx.compose.animation.slideOutHorizontally(animationSpec = tween(duration, easing = androidx.compose.animation.core.FastOutLinearInEasing)) { width -> -width } + fadeOut(animationSpec = tween(duration))
                                        } else if (transType == OverlayStateManager.TransitionType.SWIPE_RIGHT) {
                                            androidx.compose.animation.slideInHorizontally(animationSpec = tween(duration, easing = androidx.compose.animation.core.LinearOutSlowInEasing)) { width -> -width } + fadeIn(animationSpec = tween(duration)) togetherWith
                                            androidx.compose.animation.slideOutHorizontally(animationSpec = tween(duration, easing = androidx.compose.animation.core.FastOutLinearInEasing)) { width -> width } + fadeOut(animationSpec = tween(duration))
                                        } else {
                                            slideInVertically(animationSpec = tween(duration, easing = androidx.compose.animation.core.LinearOutSlowInEasing)) { height -> (height * 0.7f).roundToInt() } + fadeIn(animationSpec = tween(duration)) togetherWith
                                            slideOutVertically(animationSpec = tween(duration, easing = androidx.compose.animation.core.FastOutLinearInEasing)) { height -> (-height * 0.7f).roundToInt() } + fadeOut(animationSpec = tween(duration))
                                        }
                                    },
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) { key ->
                                    RenderSegmentContent(SplitSegment.RIGHT, key)
                                }
                            }
                        }
                    } else {
                        val lastTransitionType by OverlayStateManager.lastTransitionType.collectAsState()
                        var previousActiveStateKey by remember { mutableStateOf(activeStateKey) }
                        LaunchedEffect(activeStateKey) {
                            Log.d("NovaBar-SwipeDebug", "4. activeStateKey before swipe/change: '$previousActiveStateKey', 5. activeStateKey after swipe/change: '$activeStateKey'")
                            previousActiveStateKey = activeStateKey
                            delay(50L)
                            val rawTransType = OverlayStateManager.lastTransitionType.value
                            OverlayStateManager.resetTransitionType()
                            Log.d("NovaBar-SwipeDebug", "Reset transition type: beforeReset='$rawTransType', afterReset='${OverlayStateManager.lastTransitionType.value}'")
                        }

                        AnimatedContent(
                            targetState = activeStateKey,
                            transitionSpec = {
                                val duration = 350
                                val transType = OverlayStateManager.lastTransitionType.value
                                val keyFrom = initialState
                                val keyTo = targetState
                                Log.d("NovaBar-SwipeDebug", "6. transitionSpec evaluated. targetState: '$keyFrom' -> '$keyTo'. transType collected state: '$lastTransitionType', raw flow state: '$transType'")
                                
                                when (transType) {
                                    OverlayStateManager.TransitionType.SWIPE_LEFT -> {
                                        Log.d("NovaBar-SwipeDebug", "7/8. SWIPE_LEFT transition selected and NOT skipped.")
                                        (slideInHorizontally(animationSpec = tween(duration, easing = androidx.compose.animation.core.LinearOutSlowInEasing)) { width -> width } + 
                                         fadeIn(animationSpec = tween(duration))) togetherWith
                                        (slideOutHorizontally(animationSpec = tween(duration, easing = androidx.compose.animation.core.FastOutLinearInEasing)) { width -> -width } + 
                                         fadeOut(animationSpec = tween(duration)))
                                    }
                                    OverlayStateManager.TransitionType.SWIPE_RIGHT -> {
                                        Log.d("NovaBar-SwipeDebug", "7/8. SWIPE_RIGHT transition selected and NOT skipped.")
                                        (slideInHorizontally(animationSpec = tween(duration, easing = androidx.compose.animation.core.LinearOutSlowInEasing)) { width -> -width } + 
                                         fadeIn(animationSpec = tween(duration))) togetherWith
                                        (slideOutHorizontally(animationSpec = tween(duration, easing = androidx.compose.animation.core.FastOutLinearInEasing)) { width -> width } + 
                                         fadeOut(animationSpec = tween(duration)))
                                    }
                                    else -> {
                                        Log.d("NovaBar-SwipeDebug", "7/8. AUTOMATIC (vertical cascade) transition selected and NOT skipped.")
                                        (slideInVertically(animationSpec = tween(duration, easing = androidx.compose.animation.core.LinearOutSlowInEasing)) { height -> (height * 0.7f).roundToInt() } + 
                                         fadeIn(animationSpec = tween(duration))) togetherWith
                                        (slideOutVertically(animationSpec = tween(duration, easing = androidx.compose.animation.core.FastOutLinearInEasing)) { height -> (-height * 0.7f).roundToInt() } + 
                                         fadeOut(animationSpec = tween(duration)))
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.TopCenter
                        ) { key ->
                            val state = activeStateMap[key]
                            if (state != null) {
                                val stateTargetState = when (state) {
                                    is OverlayState.Media -> {
                                        when (settings.defaultPresentationMode) {
                                            "Minimized" -> if (isExpanded) NowBarState.EXPANDED else NowBarState.MINIMIZED
                                            "Expanded" -> NowBarState.EXPANDED
                                            else -> if (isExpanded) NowBarState.EXPANDED else NowBarState.COMPACT
                                        }
                                    }
                                    is OverlayState.Charging -> {
                                        when (settings.defaultPresentationMode) {
                                            "Minimized" -> if (resolvedExpandedCharging) NowBarState.EXPANDED else NowBarState.MINIMIZED
                                            "Expanded" -> NowBarState.EXPANDED
                                            else -> if (resolvedExpandedCharging) NowBarState.EXPANDED else NowBarState.COMPACT
                                        }
                                    }
                                    else -> {
                                        when (settings.defaultPresentationMode) {
                                            "Minimized" -> if (isExpanded) NowBarState.EXPANDED else NowBarState.MINIMIZED
                                            "Expanded" -> NowBarState.EXPANDED
                                            else -> if (isExpanded) NowBarState.EXPANDED else NowBarState.COMPACT
                                        }
                                    }
                                }

                                val stateTargetWidth = when (stateTargetState) {
                                    NowBarState.MINIMIZED -> (115 * settings.barWidthScale).dp
                                    NowBarState.COMPACT -> (185 * settings.barWidthScale).dp
                                    NowBarState.EXPANDED -> 290.dp
                                }
                                val stateTargetHeight = when (stateTargetState) {
                                    NowBarState.MINIMIZED -> (38 + settings.barHeightPadding).dp.coerceAtLeast(24.dp)
                                    NowBarState.COMPACT -> (44 + settings.barHeightPadding).dp.coerceAtLeast(30.dp)
                                    NowBarState.EXPANDED -> 205.dp
                                }

                                val stateWidth by animateDpAsState(
                                    targetValue = stateTargetWidth,
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 850f),
                                    label = "stateWidth"
                                )
                                val stateHeight by animateDpAsState(
                                    targetValue = stateTargetHeight,
                                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = 850f),
                                    label = "stateHeight"
                                )

                                Box(
                                    modifier = Modifier
                                        .requiredSize(stateWidth, stateHeight)
                                        .onGloballyPositioned { coordinates ->
                                            if (state == activeStateMap[activeStateKey]) {
                                                val rect = coordinates.boundsInWindow()
                                                OverlayStateManager.pillBounds.value = android.graphics.Rect(
                                                    rect.left.roundToInt(),
                                                    rect.top.roundToInt(),
                                                    rect.right.roundToInt(),
                                                    rect.bottom.roundToInt()
                                                )
                                            }
                                        }
                                        .border(borderThickness, borderColor, RoundedCornerShape(animatedCornerRadius))
                                        .clip(RoundedCornerShape(animatedCornerRadius))
                                        .background(backgroundColor)
                                        .drawBehind {
                                            if (state is OverlayState.Charging) {
                                                val fillFraction = (animatedPercent / 100f).coerceIn(0f, 1f)
                                                if (fillFraction > 0f) {
                                                    val fillThemeColor = Color(0xFF34C759)
                                                    val drawWidth = size.width
                                                    val drawHeight = size.height
                                                    val fillWidth = drawWidth * fillFraction

                                                    val steps = 30
                                                    val amplitude1 = 3.dp.toPx()
                                                    val frequency1 = (2f * Math.PI.toFloat()) / drawHeight
                                                    val fillPath1 = Path()
                                                    fillPath1.moveTo(0f, 0f)
                                                    val xAtZero1 = fillWidth + kotlin.math.sin(0.0 - chargingWavePhase.toDouble()).toFloat() * amplitude1
                                                    fillPath1.lineTo(xAtZero1.coerceIn(0f, drawWidth), 0f)
                                                    for (i in 0..steps) {
                                                        val y = (i.toFloat() / steps) * drawHeight
                                                        val x = fillWidth + kotlin.math.sin(y * frequency1 - chargingWavePhase.toDouble()).toFloat() * amplitude1
                                                        fillPath1.lineTo(x.coerceIn(0f, drawWidth), y)
                                                    }
                                                    fillPath1.lineTo(0f, drawHeight)
                                                    fillPath1.close()
                                                    drawPath(path = fillPath1, color = fillThemeColor.copy(alpha = 0.18f))

                                                    val amplitude2 = 2.dp.toPx()
                                                    val frequency2 = (2f * Math.PI.toFloat()) / (drawHeight * 0.8f)
                                                    val phaseOffset2 = 2f
                                                    val fillPath2 = Path()
                                                    fillPath2.moveTo(0f, 0f)
                                                    val xAtZero2 = fillWidth + kotlin.math.sin(0.0 - (chargingWavePhase + phaseOffset2).toDouble()).toFloat() * amplitude2
                                                    fillPath2.lineTo(xAtZero2.coerceIn(0f, drawWidth), 0f)
                                                    for (i in 0..steps) {
                                                        val y = (i.toFloat() / steps) * drawHeight
                                                        val x = fillWidth + kotlin.math.sin(y * frequency2 - (chargingWavePhase + phaseOffset2).toDouble()).toFloat() * amplitude2
                                                        fillPath2.lineTo(x.coerceIn(0f, drawWidth), y)
                                                    }
                                                    fillPath2.lineTo(0f, drawHeight)
                                                    fillPath2.close()
                                                    drawPath(path = fillPath2, color = fillThemeColor.copy(alpha = 0.10f))

                                                    val amplitude3 = 1.dp.toPx()
                                                    val frequency3 = (2f * Math.PI.toFloat()) / (drawHeight * 1.2f)
                                                    val phaseOffset3 = 4f
                                                    val fillPath3 = Path()
                                                    fillPath3.moveTo(0f, 0f)
                                                    val xAtZero3 = fillWidth + kotlin.math.sin(0.0 - (chargingWavePhase + phaseOffset3).toDouble()).toFloat() * amplitude3
                                                    fillPath3.lineTo(xAtZero3.coerceIn(0f, drawWidth), 0f)
                                                    for (i in 0..steps) {
                                                        val y = (i.toFloat() / steps) * drawHeight
                                                        val x = fillWidth + kotlin.math.sin(y * frequency3 - (chargingWavePhase + phaseOffset3).toDouble()).toFloat() * amplitude3
                                                        fillPath3.lineTo(x.coerceIn(0f, drawWidth), y)
                                                    }
                                                    fillPath3.lineTo(0f, drawHeight)
                                                    fillPath3.close()
                                                    drawPath(path = fillPath3, color = fillThemeColor.copy(alpha = 0.05f))
                                                }
                                            }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    when (state) {
                                        is OverlayState.PhoneCall -> PhoneCallView(state.data, stateTargetState, foregroundColor, textSizeOffset, null) {
                                            userInteractionTick = System.currentTimeMillis()
                                        }
                                        is OverlayState.Charging -> ChargingPill(
                                            state = state.data,
                                            currentState = stateTargetState,
                                            color = foregroundColor,
                                            textSizeOffset = textSizeOffset,
                                            contentAlpha = contentAlpha.value,
                                            splitSegment = null
                                        )
                                        is OverlayState.Notification -> NotificationView(
                                            state.data, stateTargetState, viewModel, foregroundColor, textSizeOffset, splitSegment = null
                                        )
                                        is OverlayState.Timer -> TimerView(state.data, stateTargetState, foregroundColor, settings.showSeconds, textSizeOffset, splitSegment = null) {
                                            userInteractionTick = System.currentTimeMillis()
                                        }
                                        is OverlayState.Stopwatch -> StopwatchView(state.data, stateTargetState, foregroundColor, settings.showSeconds, textSizeOffset, splitSegment = null) {
                                            userInteractionTick = System.currentTimeMillis()
                                        }
                                        is OverlayState.Navigation -> NavigationView(state.data, stateTargetState, foregroundColor, settings.timeFormat, textSizeOffset, splitSegment = null)
                                        is OverlayState.Media -> {
                                            MediaView(
                                                state = state.data,
                                                currentState = stateTargetState,
                                                color = foregroundColor,
                                                albumArtCornerRadius = settings.albumArtCornerRadius,
                                                visualizerStyle = settings.visualizerStyle,
                                                visualizerSensitivity = settings.visualizerSensitivity,
                                                progressVisibility = settings.progressVisibility,
                                                splitSegment = null,
                                                textSizeOffset = textSizeOffset,
                                                onSeekTo = { posMs ->
                                                    com.novabar.app.services.NovaNotificationListener.seekTo(posMs)
                                                },
                                                onInteraction = {
                                                    userInteractionTick = System.currentTimeMillis()
                                                }
                                            )
                                        }
                                        is OverlayState.Torch -> {
                                            TorchView(
                                                state = state.data,
                                                currentState = stateTargetState,
                                                color = foregroundColor,
                                                textSizeOffset = textSizeOffset,
                                                splitSegment = null,
                                                onInteraction = {
                                                    userInteractionTick = System.currentTimeMillis()
                                                }
                                            )
                                        }
                                        is OverlayState.Hotspot -> {
                                            HotspotView(
                                                state = state.data,
                                                currentState = stateTargetState,
                                                color = foregroundColor,
                                                textSizeOffset = textSizeOffset,
                                                splitSegment = null,
                                                onTurnOffClick = {
                                                    viewModel.disableHotspot(context)
                                                }
                                            )
                                        }
                                        is OverlayState.VoiceRecorder -> {
                                            VoiceRecorderView(
                                                state = state.data,
                                                currentState = stateTargetState,
                                                color = foregroundColor,
                                                textSizeOffset = textSizeOffset,
                                                splitSegment = null
                                            ) {
                                                userInteractionTick = System.currentTimeMillis()
                                            }
                                        }
                                        is OverlayState.Idle -> {
                                            if (settings.alwaysOnBar) {
                                                AlwaysOnView(settings.alwaysOnConfig, settings.timeFormat, settings.showSeconds, foregroundColor, textSizeOffset, null)
                                            } else {
                                                Row(modifier = Modifier.padding(horizontal = 14.dp)) {
                                                    Text("Ready", color = foregroundColor, fontSize = (12f + textSizeOffset).sp, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                        is OverlayState.NovaGuy -> {
                                            if (stateTargetState == NowBarState.EXPANDED) {
                                                NovaGuyExpandedView(foregroundColor)
                                            } else {
                                                NovaGuyCompactView(foregroundColor, stateWidth)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (showDebug) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    drawLine(Color.Red, Offset(cx, 0f), Offset(cx, size.height), strokeWidth = 1.dp.toPx())
                    drawLine(Color.Red, Offset(0f, cy), Offset(size.width, cy), strokeWidth = 1.dp.toPx())
                    drawCircle(Color.Red, radius = 4.dp.toPx(), center = Offset(cx, cy))
                }
            }

        if (showDebug) {
            val density = LocalContext.current.resources.displayMetrics.density
            val animatedHeightPx = (animatedHeight.value * density).roundToInt()
            val animatedCenterY = winY + animatedHeightPx / 2
            Column(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(8.dp)
                    .background(Color.Black.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                    .padding(8.dp)
            ) {
                Text("Top Y: $winY (STABLE)", color = Color.Green, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text("Height: ${animatedHeight.value.roundToInt()} dp", color = Color.Green, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text("Animated Center Y: $animatedCenterY", color = Color.Green, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text("Window Y: $winY", color = Color.Green, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}
}

// --- PHONE CALL VIEW ---
@Composable
fun PhoneCallView(
    state: PhoneCallState,
    currentState: NowBarState,
    color: Color,
    textSizeOffset: Float,
    splitSegment: SplitSegment? = null,
    onInteraction: () -> Unit
) {
    val sizeOffset = textSizeOffset
    val context = LocalContext.current
    var isEndingCall by remember { mutableStateOf(false) }

    if (splitSegment != null) {
        if (splitSegment == SplitSegment.LEFT) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = state.callerName.ifEmpty { state.phoneNumber },
                    color = color,
                    fontSize = (11f + sizeOffset).sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (state.isIncoming) "Incoming" else "Active",
                    color = color.copy(alpha = 0.6f),
                    fontSize = (10f + sizeOffset).sp,
                    maxLines = 1
                )
            }
        }
        return
    }

    when (currentState) {
        NowBarState.MINIMIZED -> {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
            ) {
                Canvas(modifier = Modifier.size(12.dp)) {
                    drawCircle(Color(0xFF4CAF50))
                }
                Text(
                    text = state.callerName.ifEmpty { state.phoneNumber },
                    color = color,
                    fontSize = (11f + sizeOffset).sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        NowBarState.COMPACT -> {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Canvas(modifier = Modifier.size(14.dp)) {
                    drawCircle(Color(0xFF4CAF50))
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.callerName.ifEmpty { "Call" },
                        color = color,
                        fontSize = (12f + sizeOffset).sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (state.isIncoming) "Incoming call" else "In call...",
                        color = color.copy(alpha = 0.6f),
                        fontSize = (10f + sizeOffset).sp,
                        maxLines = 1
                    )
                }
            }
        }
        NowBarState.EXPANDED -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(color.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = (state.callerName.firstOrNull() ?: 'C').toString(),
                            color = color,
                            fontSize = (18f + sizeOffset).sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = state.callerName.ifEmpty { "Call" },
                            color = color,
                            fontSize = (14f + sizeOffset).sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        Text(
                            text = state.phoneNumber,
                            color = color.copy(alpha = 0.6f),
                            fontSize = (11f + sizeOffset).sp,
                            maxLines = 1
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    if (state.isIncoming) {
                        Button(
                            onClick = { onInteraction() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                            modifier = Modifier.weight(1f).padding(horizontal = 6.dp)
                        ) {
                            Text("Answer", color = Color.White, fontSize = (11f + sizeOffset).sp)
                        }
                        Button(
                            onClick = {
                                isEndingCall = true
                                onInteraction()
                                OverlayStateManager.endCall(context)
                            },
                            enabled = !isEndingCall,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF44336),
                                disabledContainerColor = Color(0xFFD32F2F)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 6.dp)
                                .graphicsLayer {
                                    if (isEndingCall) {
                                        scaleX = 0.95f
                                        scaleY = 0.95f
                                    }
                                }
                        ) {
                            if (isEndingCall) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text("Decline", color = Color.White, fontSize = (11f + sizeOffset).sp)
                            }
                        }
                    } else {
                        Button(
                            onClick = {
                                isEndingCall = true
                                onInteraction()
                                OverlayStateManager.endCall(context)
                            },
                            enabled = !isEndingCall,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF44336),
                                disabledContainerColor = Color(0xFFD32F2F)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 6.dp)
                                .graphicsLayer {
                                    if (isEndingCall) {
                                        scaleX = 0.95f
                                        scaleY = 0.95f
                                    }
                                }
                        ) {
                            if (isEndingCall) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                    Text("Ending...", color = Color.White, fontSize = (11f + sizeOffset).sp)
                                }
                            } else {
                                Text("End Call", color = Color.White, fontSize = (11f + sizeOffset).sp)
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- BOLT ICON ---
@Composable
fun BoltIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            moveTo(w * 0.6f, 0.05f * h)
            lineTo(w * 0.25f, 0.55f * h)
            lineTo(w * 0.5f, 0.55f * h)
            lineTo(w * 0.4f, 0.95f * h)
            lineTo(w * 0.75f, 0.45f * h)
            lineTo(w * 0.5f, 0.45f * h)
            close()
        }
        drawPath(path, color)
    }
}

// --- CHARGING PILL ---
@Composable
fun ChargingPill(
    state: ChargingState,
    currentState: NowBarState,
    color: Color,
    textSizeOffset: Float,
    contentAlpha: Float = 1f,
    splitSegment: SplitSegment? = null
) {
    val sizeOffset = textSizeOffset

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {

        if (splitSegment != null) {
            if (splitSegment == SplitSegment.LEFT) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    BoltIcon(color = color, modifier = Modifier.size(if (currentState == NowBarState.MINIMIZED) 12.dp else 14.dp))
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "${state.batteryPercentage}%",
                        color = color,
                        fontSize = (11f + sizeOffset).sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        } else {
            when (currentState) {
                NowBarState.MINIMIZED -> {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
                    ) {
                        BoltIcon(color = color, modifier = Modifier.size(12.dp))
                        Text(
                            text = "${state.batteryPercentage}%",
                            color = color,
                            fontSize = (11f + sizeOffset).sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                NowBarState.COMPACT -> {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BoltIcon(color = color, modifier = Modifier.size(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Charging ${state.batteryPercentage}%",
                                color = color,
                                fontSize = (12f + sizeOffset).sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                            if (state.speed.isNotEmpty()) {
                                Text(
                                    text = state.speed,
                                    color = color.copy(alpha = 0.6f),
                                    fontSize = (10f + sizeOffset).sp,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }
                NowBarState.EXPANDED -> {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp)
                            .graphicsLayer { alpha = contentAlpha },
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            BoltIcon(color = color, modifier = Modifier.size(24.dp))
                            Column {
                                Text(
                                    text = "Charging ${state.batteryPercentage}%",
                                    color = color,
                                    fontSize = (15f + sizeOffset).sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = state.speed,
                                    color = color.copy(alpha = 0.6f),
                                    fontSize = (11f + sizeOffset).sp
                                )
                            }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("Battery Temp: ${state.temperature}°C", color = color.copy(alpha = 0.8f), fontSize = (11f + sizeOffset).sp)
                            Text("Status: Charging normally", color = color.copy(alpha = 0.5f), fontSize = (9f + sizeOffset).sp)
                        }
                    }
                }
            }
        }
    }
}

// --- NOTIFICATION BANNER ---
@Composable
fun NotificationView(
    state: NotificationState,
    currentState: NowBarState,
    viewModel: OverlayViewModel,
    color: Color,
    textSizeOffset: Float,
    contentAlpha: Float = 1f,
    controlsAlpha: Float = 1f,
    controlsOffsetY: androidx.compose.ui.unit.Dp = 0.dp,
    splitSegment: SplitSegment? = null
) {
    val sizeOffset = textSizeOffset
    var offsetX by remember { mutableStateOf(0f) }

    if (splitSegment != null) {
        if (splitSegment == SplitSegment.LEFT) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(if (currentState == NowBarState.MINIMIZED) 16.dp else 20.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(if (currentState == NowBarState.MINIMIZED) 8.dp else 10.dp)) {
                        drawCircle(color, radius = size.minDimension / 2)
                    }
                }
            }
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = state.title.ifEmpty { state.appName },
                    color = color,
                    fontSize = (if (currentState == NowBarState.MINIMIZED) 11f else 12f + sizeOffset).sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        return
    }

    val swipeModifier = Modifier
        .offset { IntOffset(offsetX.roundToInt(), 0) }
        .pointerInput(Unit) {
            detectHorizontalDragGestures(
                onDragEnd = {
                    if (kotlin.math.abs(offsetX) > 150f) {
                        viewModel.dismissNotification()
                    }
                    offsetX = 0f
                },
                onDragCancel = { offsetX = 0f },
                onHorizontalDrag = { _, dragAmount ->
                    offsetX += dragAmount
                }
            )
        }

    when (currentState) {
        NowBarState.MINIMIZED -> {
            Row(
                modifier = Modifier
                    .then(swipeModifier)
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(8.dp)) {
                        drawCircle(color, radius = size.minDimension / 2)
                    }
                }
                Text(
                    text = state.appName,
                    color = color,
                    fontSize = (11f + sizeOffset).sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        NowBarState.COMPACT -> {
            Row(
                modifier = Modifier
                    .then(swipeModifier)
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(color.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(10.dp)) {
                        drawCircle(color, radius = size.minDimension / 2)
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.appName,
                        color = color,
                        fontSize = (12f + sizeOffset).sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Text(
                        text = "New alert received",
                        color = color.copy(alpha = 0.6f),
                        fontSize = (10f + sizeOffset).sp,
                        maxLines = 1
                    )
                }
            }
        }
        NowBarState.EXPANDED -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer { alpha = contentAlpha },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(color.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(14.dp)) {
                            drawCircle(color, radius = size.minDimension / 2)
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = state.title.ifEmpty { state.appName },
                            color = color,
                            fontSize = (14f + sizeOffset).sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = state.summary,
                            color = color.copy(alpha = 0.7f),
                            fontSize = (11.5f + sizeOffset).sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = controlsAlpha
                            translationY = controlsOffsetY.toPx()
                        },
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = { viewModel.dismissNotification() }) {
                        Text("Dismiss", color = color.copy(alpha = 0.6f), fontSize = (11f + sizeOffset).sp)
                    }
                }
            }
        }
    }
}

// --- TIMER VIEW ---
@Composable
fun TimerDisplayText(
    remainingMs: Long,
    showSeconds: Boolean,
    color: Color,
    fontSize: TextUnit,
    fontWeight: FontWeight,
    modifier: Modifier = Modifier
) {
    Text(
        text = formatDuration(remainingMs, showSeconds),
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        modifier = modifier,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

// --- TIMER VIEW ---
@Composable
fun TimerView(
    state: TimerState,
    currentState: NowBarState,
    color: Color,
    showSeconds: Boolean,
    textSizeOffset: Float,
    contentAlpha: Float = 1f,
    controlsAlpha: Float = 1f,
    controlsOffsetY: androidx.compose.ui.unit.Dp = 0.dp,
    splitSegment: SplitSegment? = null,
    isDashboardCard: Boolean = false,
    onInteraction: () -> Unit
) {
    val sizeOffset = textSizeOffset

    if (splitSegment != null) {
        if (splitSegment == SplitSegment.LEFT) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(id = NovaIcons.Timer),
                    contentDescription = "Timer",
                    tint = color,
                    modifier = Modifier.size(if (currentState == NowBarState.MINIMIZED) 14.dp else 16.dp)
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                TimerDisplayText(
                    remainingMs = state.remainingMs,
                    showSeconds = showSeconds,
                    color = color,
                    fontSize = (11f + sizeOffset).sp,
                    fontWeight = if (currentState == NowBarState.MINIMIZED) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
        return
    }

    when (currentState) {
        NowBarState.MINIMIZED -> {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
            ) {
                Icon(
                    painter = painterResource(id = NovaIcons.Timer),
                    contentDescription = "Timer",
                    tint = color,
                    modifier = Modifier.size(14.dp)
                )
                TimerDisplayText(
                    remainingMs = state.remainingMs,
                    showSeconds = showSeconds,
                    color = color,
                    fontSize = (11f + sizeOffset).sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        NowBarState.COMPACT -> {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    painter = painterResource(id = NovaIcons.Timer),
                    contentDescription = "Timer",
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (state.label.isNotEmpty()) state.label else "Timer",
                        color = color,
                        fontSize = (12f + sizeOffset).sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    TimerDisplayText(
                        remainingMs = state.remainingMs,
                        showSeconds = showSeconds,
                        color = color.copy(alpha = 0.8f),
                        fontSize = (11f + sizeOffset).sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (isDashboardCard) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Cancel (Reset) button
                        MiniCircleButton(
                            iconRes = NovaIcons.Close,
                            onClick = {
                                onInteraction()
                                com.novabar.app.services.NovaNotificationListener.resetTimer()
                            },
                            color = color,
                            enabled = state.hasReset
                        )
                        
                        // Pause / Resume button
                        val canPauseOrResume = if (state.isRunning) state.hasPause else state.hasResume
                        MiniCircleButton(
                            iconRes = if (state.isRunning) NovaIcons.Pause else NovaIcons.Play,
                            onClick = {
                                onInteraction()
                                if (state.isRunning) {
                                    com.novabar.app.services.NovaNotificationListener.pauseTimer()
                                } else {
                                    com.novabar.app.services.NovaNotificationListener.resumeTimer()
                                }
                            },
                            color = color,
                            enabled = canPauseOrResume,
                            isPrimary = true
                        )
                    }
                }
            }
        }
        NowBarState.EXPANDED -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer { alpha = contentAlpha },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        painter = painterResource(id = NovaIcons.Timer),
                        contentDescription = "Timer",
                        tint = color,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "Timer",
                            color = color,
                            fontSize = (15f + sizeOffset).sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (state.isRunning) "Running" else "Paused",
                            color = color.copy(alpha = 0.5f),
                            fontSize = (10f + sizeOffset).sp
                        )
                    }
                }
                
                TimerDisplayText(
                    remainingMs = state.remainingMs,
                    showSeconds = showSeconds,
                    color = color,
                    fontSize = (26f + sizeOffset).sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .graphicsLayer { alpha = contentAlpha }
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = controlsAlpha
                            translationY = controlsOffsetY.toPx()
                        },
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val resetInteractionSource = remember { MutableInteractionSource() }
                    val isResetPressed by resetInteractionSource.collectIsPressedAsState()
                    val resetScale by animateFloatAsState(if (isResetPressed) 0.92f else 1f, label = "ResetScale")

                    Button(
                        onClick = {
                            onInteraction()
                            com.novabar.app.services.NovaNotificationListener.resetTimer()
                        },
                        enabled = state.hasReset,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = color.copy(alpha = 0.1f),
                            disabledContainerColor = color.copy(alpha = 0.03f)
                        ),
                        interactionSource = resetInteractionSource,
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = resetScale
                                scaleY = resetScale
                            }
                            .alpha(if (state.hasReset) 1f else 0.5f)
                    ) {
                        Text("Reset", color = color, fontSize = (11f + sizeOffset).sp)
                    }

                    val playInteractionSource = remember { MutableInteractionSource() }
                    val isPlayPressed by playInteractionSource.collectIsPressedAsState()
                    val playScale by animateFloatAsState(if (isPlayPressed) 0.92f else 1f, label = "PlayScale")
                    val canPauseOrResume = if (state.isRunning) state.hasPause else state.hasResume

                    Button(
                        onClick = {
                            onInteraction()
                            if (state.isRunning) {
                                com.novabar.app.services.NovaNotificationListener.pauseTimer()
                            } else {
                                com.novabar.app.services.NovaNotificationListener.resumeTimer()
                            }
                        },
                        enabled = canPauseOrResume,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = color,
                            disabledContainerColor = color.copy(alpha = 0.3f)
                        ),
                        interactionSource = playInteractionSource,
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = playScale
                                scaleY = playScale
                            }
                            .alpha(if (canPauseOrResume) 1f else 0.5f)
                    ) {
                        Text(
                            text = if (state.isRunning) "Pause" else "Resume",
                            color = if (color == Color.White) Color.Black else Color.White,
                            fontSize = (11f + sizeOffset).sp
                        )
                    }
                }
            }
        }
    }
}

fun formatDuration(ms: Long, showSeconds: Boolean): String {
    val displayMs = if (!showSeconds) ms + 999L else ms
    val hours = displayMs / 3600000
    val minutes = (displayMs % 3600000) / 60000
    val seconds = (displayMs % 60000) / 1000
    val centiseconds = (displayMs % 1000) / 10
    
    return if (showSeconds) {
        if (hours > 0) {
            String.format("%02d:%02d:%02d.%02d", hours, minutes, seconds, centiseconds)
        } else {
            String.format("%02d:%02d.%02d", minutes, seconds, centiseconds)
        }
    } else {
        if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }
}

// --- STOPWATCH VIEW ---
@Composable
fun StopwatchDisplayText(
    elapsedMs: Long,
    showSeconds: Boolean,
    color: Color,
    fontSize: TextUnit,
    fontWeight: FontWeight,
    modifier: Modifier = Modifier
) {
    Text(
        text = formatStopwatch(elapsedMs, showSeconds),
        color = color,
        fontSize = fontSize,
        fontWeight = fontWeight,
        modifier = modifier,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

// --- AUDIO VISUALIZER ---
@Composable
fun AudioVisualizer(
    isAnimating: Boolean,
    modifier: Modifier = Modifier,
    barCount: Int = 12,
    barColor: Color = Color.White,
    maxBarHeight: androidx.compose.ui.unit.Dp = 32.dp,
    barWidth: androidx.compose.ui.unit.Dp = 3.dp,
    barSpacing: androidx.compose.ui.unit.Dp = 2.dp,
    amplitudes: List<Float>? = null
) {
    val transition = rememberInfiniteTransition(label = "audio_visualizer")
    val barScale = (0 until barCount).map { index ->
        if (isAnimating) {
            val realAmp = amplitudes?.getOrNull(index)
            if (realAmp != null && realAmp > 0f) {
                realAmp.coerceIn(0.1f, 1f)
            } else {
                val duration = 600 + (index * 83) % 400
                val delay = (index * 113) % 250
                val anim = transition.animateFloat(
                    initialValue = 0.1f,
                    targetValue = 0.35f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(duration, delayMillis = delay, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "bar_$index"
                )
                anim.value
            }
        } else {
            0.1f
        }
    }

    Row(
        modifier = modifier.height(maxBarHeight),
        horizontalArrangement = Arrangement.spacedBy(barSpacing),
        verticalAlignment = Alignment.CenterVertically
    ) {
        barScale.forEach { scale ->
            Box(
                modifier = Modifier
                    .width(barWidth)
                    .height(maxBarHeight * scale)
                    .background(barColor, RoundedCornerShape(1.5.dp))
            )
        }
    }
}

// --- BLINKING MIC ICON ---
@Composable
fun BlinkingMicIcon(
    isRecording: Boolean,
    color: Color,
    size: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "mic_blink")
    val alpha by if (isRecording) {
        transition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1.0f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "alpha"
        )
    } else {
        remember { mutableStateOf(0.7f) }
    }

    Icon(
        painter = painterResource(id = NovaIcons.VoiceRecorder),
        contentDescription = "Voice Recorder Icon",
        tint = color.copy(alpha = alpha),
        modifier = modifier.size(size)
    )
}

// --- VOICE RECORDER VIEW ---
@Composable
fun VoiceRecorderView(
    state: VoiceRecorderState,
    currentState: NowBarState,
    color: Color,
    textSizeOffset: Float,
    contentAlpha: Float = 1f,
    controlsAlpha: Float = 1f,
    controlsOffsetY: androidx.compose.ui.unit.Dp = 0.dp,
    splitSegment: SplitSegment? = null,
    isDashboardCard: Boolean = false,
    onInteraction: () -> Unit
) {
    val buttonsEnabled = state.hasPause || state.hasResume || state.hasStop
    android.util.Log.d("VoiceRecorder", "[VoiceRecorder] Stage 6: Compose Recomposition | elapsedTime=${state.durationMs} | isRecording=${state.isRecording} | buttonsEnabled=$buttonsEnabled | pauseIntent != null=${state.pauseIntent != null} | resumeIntent != null=${state.resumeIntent != null} | stopIntent != null=${state.stopIntent != null} | currentState=$currentState | isDashboardCard=$isDashboardCard")

    val sizeOffset = textSizeOffset

    // 1. SplitSegment segment (for split compact view)
    if (splitSegment != null) {
        if (splitSegment == SplitSegment.LEFT) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                BlinkingMicIcon(
                    isRecording = state.isRecording,
                    color = color,
                    size = if (currentState == NowBarState.MINIMIZED) 14.dp else 16.dp
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                val totalSecs = state.durationMs / 1000
                val mins = (totalSecs % 3600) / 60
                val secs = totalSecs % 60
                val formattedTime = String.format("%02d:%02d", mins, secs)
                Text(
                    text = formattedTime,
                    color = color,
                    fontSize = (11f + sizeOffset).sp,
                    fontWeight = if (currentState == NowBarState.MINIMIZED) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
        return
    }

    // Helper to format duration: "mm:ss" or "hh:mm:ss"
    fun formatVoiceRecorderDuration(ms: Long): String {
        val totalSecs = ms / 1000
        val hrs = totalSecs / 3600
        val mins = (totalSecs % 3600) / 60
        val secs = totalSecs % 60
        return if (hrs > 0) {
            String.format("%02d:%02d:%02d", hrs, mins, secs)
        } else {
            String.format("%02d:%02d", mins, secs)
        }
    }

    when (currentState) {
        NowBarState.MINIMIZED -> {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
            ) {
                BlinkingMicIcon(isRecording = state.isRecording, color = color, size = 14.dp)
                Text(
                    text = formatVoiceRecorderDuration(state.durationMs),
                    color = color,
                    fontSize = (11f + sizeOffset).sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        NowBarState.COMPACT -> {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                BlinkingMicIcon(isRecording = state.isRecording, color = color, size = 16.dp)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.appName.ifEmpty { "Voice Recorder" },
                        color = color,
                        fontSize = (12f + sizeOffset).sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (state.isRecording) "Recording" else "Paused",
                        color = color.copy(alpha = 0.8f),
                        fontSize = (11f + sizeOffset).sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (isDashboardCard) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Discard / Stop button
                        MiniCircleButton(
                            iconRes = NovaIcons.Delete,
                            onClick = {
                                onInteraction()
                                com.novabar.app.services.NovaNotificationListener.stopVoiceRecorder()
                            },
                            color = color,
                            enabled = state.hasStop
                        )
                        
                        // Pause / Resume button
                        val canPauseOrResume = if (state.isRecording) state.hasPause else state.hasResume
                        MiniCircleButton(
                            iconRes = if (state.isRecording) NovaIcons.Pause else NovaIcons.Play,
                            onClick = {
                                onInteraction()
                                if (state.isRecording) {
                                    com.novabar.app.services.NovaNotificationListener.pauseVoiceRecorder()
                                } else {
                                    com.novabar.app.services.NovaNotificationListener.resumeVoiceRecorder()
                                }
                            },
                            color = color,
                            enabled = canPauseOrResume,
                            isPrimary = true
                        )
                    }
                } else {
                    // Show compact duration in the pill when not in dashboard card
                    Text(
                        text = formatVoiceRecorderDuration(state.durationMs),
                        color = color,
                        fontSize = (14f + sizeOffset).sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        NowBarState.EXPANDED -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header (AppName + Recording State)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer { alpha = contentAlpha },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (state.appIcon != null) {
                        DrawableImage(
                            drawable = state.appIcon,
                            tintColor = null,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Icon(
                            painter = painterResource(id = NovaIcons.VoiceRecorder),
                            contentDescription = "Voice Recorder",
                            tint = color,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = state.appName.ifEmpty { "Voice Recorder" },
                            color = color,
                            fontSize = (15f + sizeOffset).sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (state.isRecording) "Recording" else "Paused",
                            color = color.copy(alpha = 0.6f),
                            fontSize = (11f + sizeOffset).sp
                        )
                    }
                }

                // Middle: Visualizer + Large Duration
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .graphicsLayer { alpha = contentAlpha },
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = formatVoiceRecorderDuration(state.durationMs),
                        color = color,
                        fontSize = (36f + sizeOffset).sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    AudioVisualizer(
                        isAnimating = state.isRecording,
                        barCount = 18,
                        barColor = color,
                        maxBarHeight = 40.dp,
                        barWidth = 2.5.dp,
                        barSpacing = 3.dp,
                        amplitudes = state.amplitudes
                    )
                }

                // Bottom: Action Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = controlsAlpha
                            translationY = controlsOffsetY.value
                        },
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Discard / Stop Button (Trash Icon)
                    CircleButton(
                        iconRes = NovaIcons.Delete,
                        onClick = {
                            onInteraction()
                            com.novabar.app.services.NovaNotificationListener.stopVoiceRecorder()
                        },
                        color = color,
                        enabled = state.hasStop,
                        size = 46.dp,
                        iconSize = 20.dp
                    )

                    // Play/Pause Button (Primary Action)
                    val canPauseOrResume = if (state.isRecording) state.hasPause else state.hasResume
                    CircleButton(
                        iconRes = if (state.isRecording) NovaIcons.Pause else NovaIcons.Play,
                        onClick = {
                            onInteraction()
                            if (state.isRecording) {
                                com.novabar.app.services.NovaNotificationListener.pauseVoiceRecorder()
                            } else {
                                com.novabar.app.services.NovaNotificationListener.resumeVoiceRecorder()
                            }
                        },
                        color = color,
                        enabled = canPauseOrResume,
                        isPrimary = true,
                        size = 56.dp,
                        iconSize = 24.dp
                    )
                }
            }
        }
    }
}

// --- STOPWATCH VIEW ---
@Composable
fun StopwatchView(
    state: StopwatchState,
    currentState: NowBarState,
    color: Color,
    showSeconds: Boolean,
    textSizeOffset: Float,
    contentAlpha: Float = 1f,
    controlsAlpha: Float = 1f,
    controlsOffsetY: androidx.compose.ui.unit.Dp = 0.dp,
    splitSegment: SplitSegment? = null,
    isDashboardCard: Boolean = false,
    onInteraction: () -> Unit
) {
    val sizeOffset = textSizeOffset

    if (splitSegment != null) {
        if (splitSegment == SplitSegment.LEFT) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(id = NovaIcons.Stopwatch),
                    contentDescription = "Stopwatch",
                    tint = color,
                    modifier = Modifier.size(if (currentState == NowBarState.MINIMIZED) 14.dp else 16.dp)
                )
            }
        } else {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                StopwatchDisplayText(
                    elapsedMs = state.elapsedMs,
                    showSeconds = showSeconds,
                    color = color,
                    fontSize = (11f + sizeOffset).sp,
                    fontWeight = if (currentState == NowBarState.MINIMIZED) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
        return
    }

    when (currentState) {
        NowBarState.MINIMIZED -> {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
            ) {
                Icon(
                    painter = painterResource(id = NovaIcons.Stopwatch),
                    contentDescription = "Stopwatch",
                    tint = color,
                    modifier = Modifier.size(14.dp)
                )
                StopwatchDisplayText(
                    elapsedMs = state.elapsedMs,
                    showSeconds = showSeconds,
                    color = color,
                    fontSize = (11f + sizeOffset).sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        NowBarState.COMPACT -> {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    painter = painterResource(id = NovaIcons.Stopwatch),
                    contentDescription = "Stopwatch",
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Stopwatch",
                        color = color,
                        fontSize = (12f + sizeOffset).sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    StopwatchDisplayText(
                        elapsedMs = state.elapsedMs,
                        showSeconds = showSeconds,
                        color = color.copy(alpha = 0.8f),
                        fontSize = (11f + sizeOffset).sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                if (isDashboardCard) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Reset button
                        MiniCircleButton(
                            iconRes = NovaIcons.Reset,
                            onClick = {
                                onInteraction()
                                com.novabar.app.services.NovaNotificationListener.resetStopwatch()
                            },
                            color = color,
                            enabled = true // Stopwatch reset is always enabled
                        )
                        
                        // Pause / Resume button
                        val canPauseOrResume = if (state.isRunning) state.hasPause else state.hasResume
                        MiniCircleButton(
                            iconRes = if (state.isRunning) NovaIcons.Pause else NovaIcons.Play,
                            onClick = {
                                onInteraction()
                                if (state.isRunning) {
                                    com.novabar.app.services.NovaNotificationListener.pauseStopwatch()
                                } else {
                                    com.novabar.app.services.NovaNotificationListener.resumeStopwatch()
                                }
                            },
                            color = color,
                            enabled = canPauseOrResume,
                            isPrimary = true
                        )
                    }
                }
            }
        }
        NowBarState.EXPANDED -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer { alpha = contentAlpha },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        painter = painterResource(id = NovaIcons.Stopwatch),
                        contentDescription = "Stopwatch",
                        tint = color,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = "Stopwatch",
                            color = color,
                            fontSize = (15f + sizeOffset).sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (state.isRunning) "Running" else "Paused",
                            color = color.copy(alpha = 0.5f),
                            fontSize = (10f + sizeOffset).sp
                        )
                    }
                }
                
                StopwatchDisplayText(
                    elapsedMs = state.elapsedMs,
                    showSeconds = showSeconds,
                    color = color,
                    fontSize = (26f + sizeOffset).sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .graphicsLayer { alpha = contentAlpha }
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer {
                            alpha = controlsAlpha
                            translationY = controlsOffsetY.toPx()
                        },
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val leftButtonInteractionSource = remember { MutableInteractionSource() }
                    val isLeftButtonPressed by leftButtonInteractionSource.collectIsPressedAsState()
                    val leftButtonScale by animateFloatAsState(if (isLeftButtonPressed) 0.92f else 1f, label = "LeftButtonScale")

                    if (state.isRunning) {
                        Button(
                            onClick = {
                                onInteraction()
                                com.novabar.app.services.NovaNotificationListener.lapStopwatch()
                            },
                            enabled = state.hasLap,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = color.copy(alpha = 0.1f),
                                disabledContainerColor = color.copy(alpha = 0.03f)
                            ),
                            interactionSource = leftButtonInteractionSource,
                            modifier = Modifier
                                .graphicsLayer {
                                    scaleX = leftButtonScale
                                    scaleY = leftButtonScale
                                }
                                .alpha(if (state.hasLap) 1f else 0.5f)
                        ) {
                            Text("Lap", color = color, fontSize = (11f + sizeOffset).sp)
                        }
                    } else {
                        Button(
                            onClick = {
                                onInteraction()
                                com.novabar.app.services.NovaNotificationListener.resetStopwatch()
                            },
                            enabled = state.hasReset,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = color.copy(alpha = 0.1f),
                                disabledContainerColor = color.copy(alpha = 0.03f)
                            ),
                            interactionSource = leftButtonInteractionSource,
                            modifier = Modifier
                                .graphicsLayer {
                                    scaleX = leftButtonScale
                                    scaleY = leftButtonScale
                                }
                                .alpha(if (state.hasReset) 1f else 0.5f)
                        ) {
                            Text("Reset", color = color, fontSize = (11f + sizeOffset).sp)
                        }
                    }

                    val playInteractionSource = remember { MutableInteractionSource() }
                    val isPlayPressed by playInteractionSource.collectIsPressedAsState()
                    val playScale by animateFloatAsState(if (isPlayPressed) 0.92f else 1f, label = "PlayScale")
                    val canPauseOrResume = if (state.isRunning) state.hasPause else state.hasResume

                    Button(
                        onClick = {
                            onInteraction()
                            if (state.isRunning) {
                                com.novabar.app.services.NovaNotificationListener.pauseStopwatch()
                            } else {
                                com.novabar.app.services.NovaNotificationListener.resumeStopwatch()
                            }
                        },
                        enabled = canPauseOrResume,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = color,
                            disabledContainerColor = color.copy(alpha = 0.3f)
                        ),
                        interactionSource = playInteractionSource,
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = playScale
                                scaleY = playScale
                            }
                            .alpha(if (canPauseOrResume) 1f else 0.5f)
                    ) {
                        Text(
                            text = if (state.isRunning) "Pause" else "Resume",
                            color = if (color == Color.White) Color.Black else Color.White,
                            fontSize = (11f + sizeOffset).sp
                        )
                    }
                }
            }
        }
    }
}

fun formatStopwatch(ms: Long, showSeconds: Boolean): String {
    val hours = ms / 3600000
    val minutes = (ms % 3600000) / 60000
    val seconds = (ms % 60000) / 1000
    val centiseconds = (ms % 1000) / 10
    
    return if (showSeconds) {
        if (hours > 0) {
            String.format("%02d:%02d:%02d.%02d", hours, minutes, seconds, centiseconds)
        } else {
            String.format("%02d:%02d.%02d", minutes, seconds, centiseconds)
        }
    } else {
        if (hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }
}

private fun cleanDistanceText(input: String): String {
    val clean = input.trim()
    val distanceRegex = Regex("""(?i)(?:\bin\s+)?(\d+(?:\.\d+)?\s*(?:ft|mi|m|km|feet|miles|meters|yd|yds|yard|yards))""")
    val match = distanceRegex.find(clean)
    if (match != null) {
        return match.groupValues[1].trim()
    }
    // If it only contains numbers, spaces, and dots/commas (and optional units), keep it
    if (clean.matches(Regex("""(?i)^[0-9\s.,]+(ft|mi|m|km|feet|miles|meters|yd|yds|yard|yards)?$"""))) {
        return clean
    }
    return ""
}

@Composable
fun CircleButton(
    iconRes: Int,
    onClick: () -> Unit,
    color: Color,
    enabled: Boolean = true,
    isPrimary: Boolean = false,
    size: androidx.compose.ui.unit.Dp = 50.dp,
    iconSize: androidx.compose.ui.unit.Dp = 22.dp
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.9f else 1f, label = "CircleButtonScale")
    
    Box(
        modifier = Modifier
            .size(size)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(
                if (isPrimary) {
                    if (enabled) color else color.copy(alpha = 0.3f)
                } else {
                    if (enabled) color.copy(alpha = 0.1f) else color.copy(alpha = 0.03f)
                }
            )
            .clickable(enabled = enabled, interactionSource = interactionSource, indication = null) {
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = if (isPrimary) {
                if (color == Color.White) Color.Black else Color.White
            } else {
                if (enabled) color else color.copy(alpha = 0.3f)
            },
            modifier = Modifier.size(iconSize)
        )
    }
}

@Composable
fun MiniCircleButton(
    iconRes: Int,
    onClick: () -> Unit,
    color: Color,
    enabled: Boolean = true,
    isPrimary: Boolean = false
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(if (isPressed) 0.88f else 1f, label = "MiniCircleScale")
    
    Box(
        modifier = Modifier
            .size(32.dp)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(CircleShape)
            .background(
                if (isPrimary) {
                    if (enabled) color else color.copy(alpha = 0.3f)
                } else {
                    if (enabled) color.copy(alpha = 0.1f) else color.copy(alpha = 0.03f)
                }
            )
            .clickable(enabled = enabled, interactionSource = interactionSource, indication = null) {
                onClick()
            },
        contentAlignment = Alignment.Center
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = null,
            tint = if (isPrimary) {
                if (color == Color.White) Color.Black else Color.White
            } else {
                color
            },
            modifier = Modifier.size(16.dp)
        )
    }
}

// --- NAVIGATION VIEW ---
@Composable
fun NavigationView(
    state: NavigationState,
    currentState: NowBarState,
    color: Color,
    timeFormat: String,
    textSizeOffset: Float,
    contentAlpha: Float = 1f,
    splitSegment: SplitSegment? = null,
    isDashboardCard: Boolean = false
) {
    val sizeOffset = textSizeOffset
    val etaFormatted = formatEta(state.eta, timeFormat)

    if (splitSegment != null) {
        if (splitSegment == SplitSegment.LEFT) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (state.maneuverIcon != null) {
                    DrawableImage(
                        drawable = state.maneuverIcon,
                        tintColor = Color(0xFF2196F3),
                        modifier = Modifier.size(if (currentState == NowBarState.MINIMIZED) 14.dp else 16.dp)
                    )
                } else {
                    ManeuverIcon(
                        type = state.maneuverType,
                        color = Color(0xFF2196F3),
                        modifier = Modifier.size(if (currentState == NowBarState.MINIMIZED) 14.dp else 16.dp)
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                val cleanedDistance = cleanDistanceText(state.distanceRemaining)
                if (cleanedDistance.isNotEmpty()) {
                    Text(
                        text = cleanedDistance,
                        color = color,
                        fontSize = (if (currentState == NowBarState.MINIMIZED) 11f else 11f + sizeOffset).sp,
                        fontWeight = if (currentState == NowBarState.MINIMIZED) FontWeight.Bold else FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        return
    }

    when (currentState) {
        NowBarState.MINIMIZED -> {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
            ) {
                if (state.maneuverIcon != null) {
                    DrawableImage(
                        drawable = state.maneuverIcon,
                        tintColor = Color(0xFF2196F3),
                        modifier = Modifier.size(14.dp)
                    )
                } else {
                    ManeuverIcon(
                        type = state.maneuverType,
                        color = Color(0xFF2196F3),
                        modifier = Modifier.size(14.dp)
                    )
                }
                val cleanedDistance = cleanDistanceText(state.distanceRemaining)
                if (cleanedDistance.isNotEmpty()) {
                    Text(
                        text = cleanedDistance,
                        color = color,
                        fontSize = (11f + sizeOffset).sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        NowBarState.COMPACT -> {
            if (isDashboardCard) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Left side: Maneuver Icon & Distance to next maneuver
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (state.maneuverIcon != null) {
                            DrawableImage(
                                drawable = state.maneuverIcon,
                                tintColor = Color(0xFF2196F3),
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            ManeuverIcon(
                                type = state.maneuverType,
                                color = Color(0xFF2196F3),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        val cleanedDistance = cleanDistanceText(state.distanceRemaining)
                        if (cleanedDistance.isNotEmpty()) {
                            Text(
                                text = cleanedDistance,
                                color = color,
                                fontSize = (14f + sizeOffset).sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Right side: ETA (arrival time)
                    if (etaFormatted.isNotEmpty()) {
                        Text(
                            text = "ETA: $etaFormatted",
                            color = color.copy(alpha = 0.7f),
                            fontSize = (13f + sizeOffset).sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (state.maneuverIcon != null) {
                        DrawableImage(
                            drawable = state.maneuverIcon,
                            tintColor = Color(0xFF2196F3),
                            modifier = Modifier.size(20.dp)
                        )
                    } else {
                        ManeuverIcon(
                            type = state.maneuverType,
                            color = Color(0xFF2196F3),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    val cleanedDistance = cleanDistanceText(state.distanceRemaining)
                    if (cleanedDistance.isNotEmpty()) {
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = cleanedDistance,
                            color = color,
                            fontSize = (13f + sizeOffset).sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
        NowBarState.EXPANDED -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .graphicsLayer { alpha = contentAlpha },
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Primary: Maneuver Icon & Distance to next maneuver
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (state.maneuverIcon != null) {
                            DrawableImage(
                                drawable = state.maneuverIcon,
                                tintColor = Color(0xFF2196F3),
                                modifier = Modifier.size(36.dp)
                            )
                        } else {
                            ManeuverIcon(
                                type = state.maneuverType,
                                color = Color(0xFF2196F3),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                    }
                    Column(
                        verticalArrangement = Arrangement.Center
                    ) {
                        val contextTitle = when {
                            state.roadName.isNotEmpty() -> {
                                val road = state.roadName.trim()
                                val prefixes = listOf("towards", "toward", "continue", "stay", "keep", "merge")
                                val hasPrefix = prefixes.any { road.startsWith(it, ignoreCase = true) }
                                if (hasPrefix) road else "Towards $road"
                            }
                            state.maneuverInstruction.isNotEmpty() && state.maneuverInstruction.contains("towards", ignoreCase = true) -> {
                                state.maneuverInstruction.trim()
                            }
                            state.destination.isNotEmpty() -> {
                                val dest = state.destination.trim()
                                if (dest.startsWith("towards", ignoreCase = true)) dest else "Towards $dest"
                            }
                            state.maneuverInstruction.isNotEmpty() -> {
                                val instr = state.maneuverInstruction.trim()
                                if (instr.startsWith("towards", ignoreCase = true)) instr else "Towards $instr"
                            }
                            else -> "Navigating"
                        }
                        Text(
                            text = state.distanceRemaining.ifEmpty { contextTitle },
                            color = color,
                            fontSize = (22f + sizeOffset).sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        val subtitleText = if (state.distanceRemaining.isEmpty()) {
                            if (state.destination.isNotEmpty() && !contextTitle.contains(state.destination, ignoreCase = true)) {
                                "to: ${state.destination}"
                            } else {
                                ""
                            }
                        } else {
                            state.roadName
                        }
                        if (subtitleText.isNotEmpty()) {
                            Text(
                                text = subtitleText,
                                color = color.copy(alpha = 0.7f),
                                fontSize = (13f + sizeOffset).sp,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Divider line
                Spacer(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.dp)
                        .background(color.copy(alpha = 0.15f))
                )

                // Secondary: ETA & Remaining Trip Distance
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "ETA",
                            color = color.copy(alpha = 0.5f),
                            fontSize = (10f + sizeOffset).sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = etaFormatted.ifEmpty { "--:--" },
                            color = color,
                            fontSize = (16f + sizeOffset).sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "DISTANCE",
                            color = color.copy(alpha = 0.5f),
                            fontSize = (10f + sizeOffset).sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = state.remainingDistance.ifEmpty { "-- mi" },
                            color = color,
                            fontSize = (16f + sizeOffset).sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Supporting: Destination Name
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(color.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = if (state.destination.isNotEmpty()) "to: ${state.destination}" else "Google Maps Active Route",
                        color = color.copy(alpha = 0.8f),
                        fontSize = (11f + sizeOffset).sp,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

fun formatEta(etaStr: String, timeFormat: String): String {
    if (etaStr.isEmpty()) return ""
    try {
        val clean = etaStr.trim()
        val is24 = timeFormat == "24 Hour"
        if (clean.contains("AM", ignoreCase = true) || clean.contains("PM", ignoreCase = true)) {
            val sdfInput = java.text.SimpleDateFormat("h:mm a", java.util.Locale.US)
            val date = sdfInput.parse(clean.uppercase())
            if (date != null) {
                val sdfOutput = if (is24) {
                    java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
                } else {
                    java.text.SimpleDateFormat("h:mm a", java.util.Locale.getDefault())
                }
                return sdfOutput.format(date)
            }
        }
    } catch (e: Exception) {
        // Fallback
    }
    return etaStr
}

enum class NowBarState {
    MINIMIZED,
    COMPACT,
    EXPANDED
}

enum class SplitSegment {
    LEFT,
    RIGHT
}

@Composable
fun AudioVisualizer(
    style: String,
    sensitivity: Float,
    isPlaying: Boolean,
    color: Color,
    modifier: Modifier = Modifier
) {
    var animationPhase by remember { mutableStateOf(0f) }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            val startTime = System.currentTimeMillis()
            val startPhase = animationPhase
            while (true) {
                androidx.compose.runtime.withFrameMillis { frameTime ->
                    val delta = System.currentTimeMillis() - startTime
                    animationPhase = startPhase + (delta / 1000f) * 2f * Math.PI.toFloat() * sensitivity
                }
            }
        }
    }

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val centerY = height / 2f

        when (style) {
            "Waveform" -> {
                val path1 = Path()
                val path2 = Path()
                path1.moveTo(0f, centerY)
                path2.moveTo(0f, centerY)

                val points = 30
                val segmentWidth = width / points

                for (i in 0..points) {
                    val x = i * segmentWidth
                    val progress = i.toFloat() / points
                    val envelope = Math.sin(progress * Math.PI).toFloat()

                    val angle1 = progress * 2f * Math.PI.toFloat() * 1.5f - animationPhase
                    val y1 = centerY + Math.sin(angle1.toDouble()).toFloat() * (height / 2.2f) * envelope

                    val angle2 = progress * 2f * Math.PI.toFloat() * 2f + animationPhase + 1.0f
                    val y2 = centerY + Math.sin(angle2.toDouble()).toFloat() * (height / 3.5f) * envelope

                    if (i == 0) {
                        path1.moveTo(x, y1)
                        path2.moveTo(x, y2)
                    } else {
                        path1.lineTo(x, y1)
                        path2.lineTo(x, y2)
                    }
                }
                
                drawPath(
                    path = path2,
                    color = color.copy(alpha = 0.4f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 1.5.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                )
                drawPath(
                    path = path1,
                    color = color,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                        width = 2.dp.toPx(),
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                )
            }
            "Pulse" -> {
                val barCount = 6
                val spacing = 3.dp.toPx()
                val barWidth = (width - (barCount - 1) * spacing) / barCount
                
                for (i in 0 until barCount) {
                    val phaseOffset = i * 0.6f
                    val amplitude = 0.2f + 0.8f * Math.abs(Math.sin((animationPhase + phaseOffset).toDouble())).toFloat()
                    val barHeight = (height * amplitude).coerceIn(minOf(4.dp.toPx(), height), height)
                    
                    val x = i * (barWidth + spacing)
                    val y = (height - barHeight) / 2f
                    
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f, barWidth / 2f)
                    )
                }
            }
            "Minimal" -> {
                val barCount = 3
                val spacing = 3.dp.toPx()
                val barWidth = (width - (barCount - 1) * spacing) / barCount
                
                for (i in 0 until barCount) {
                    val phaseOffset = i * 1.0f
                    val amplitude = 0.15f + 0.85f * Math.abs(Math.sin((animationPhase * 1.3f + phaseOffset).toDouble())).toFloat()
                    val barHeight = (height * amplitude).coerceIn(minOf(3.dp.toPx(), height), height)
                    
                    val x = i * (barWidth + spacing)
                    val y = (height - barHeight) / 2f
                    
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(x, y),
                        size = Size(barWidth, barHeight),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(barWidth / 2f, barWidth / 2f)
                    )
                }
            }
        }
    }
}

@Composable
fun FallbackArtIcon(color: Color, sizeDp: Int) {
    Box(
        modifier = Modifier
            .size(sizeDp.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.1f)),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size((sizeDp / 2).dp)) {
            drawCircle(color, radius = size.minDimension * 0.18f, center = Offset(size.width * 0.35f, size.height * 0.65f))
            drawLine(color, Offset(size.width * 0.53f, size.height * 0.65f), Offset(size.width * 0.53f, size.height * 0.25f), strokeWidth = 1.5.dp.toPx())
            drawLine(color, Offset(size.width * 0.53f, size.height * 0.25f), Offset(size.width * 0.8f, size.height * 0.35f), strokeWidth = 1.5.dp.toPx())
        }
    }
}

fun formatTime(ms: Long): String {
    val totalSeconds = (ms / 1000).coerceAtLeast(0L)
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return String.format("%d:%02d", minutes, seconds)
}

fun queryCurrentMediaStateDirectly(context: android.content.Context): MediaState? {
    val controller = com.novabar.app.services.NovaNotificationListener.getActiveMediaController() ?: return null
    val playbackState = try { controller.playbackState } catch (e: Exception) { null }
    val isPlaying = playbackState?.state == android.media.session.PlaybackState.STATE_PLAYING
    val metadata = try { controller.metadata } catch (e: Exception) { null }

    val title = try { metadata?.getString(android.media.MediaMetadata.METADATA_KEY_TITLE) } catch (e: Exception) { null } ?: ""
    val artist = try { metadata?.getString(android.media.MediaMetadata.METADATA_KEY_ARTIST) } catch (e: Exception) { null } ?: ""
    val duration = try { metadata?.getLong(android.media.MediaMetadata.METADATA_KEY_DURATION) } catch (e: Exception) { null } ?: 0L
    val position = try { playbackState?.position } catch (e: Exception) { null } ?: 0L

    val albumArt = try {
        metadata?.getBitmap(android.media.MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: metadata?.getBitmap(android.media.MediaMetadata.METADATA_KEY_DISPLAY_ICON)
            ?: metadata?.getBitmap(android.media.MediaMetadata.METADATA_KEY_ART)
    } catch (e: Exception) {
        null
    }

    var appIcon: android.graphics.drawable.Drawable? = null
    var appName = ""
    try {
        val pm = context.packageManager
        val appInfo = pm.getApplicationInfo(controller.packageName, 0)
        appIcon = pm.getApplicationIcon(appInfo)
        appName = pm.getApplicationLabel(appInfo).toString()
    } catch (e: Exception) {
        appName = controller.packageName
    }

    val progress = if (duration > 0) position.toFloat() / duration else 0f
    val extras = try { controller.extras } catch (e: Exception) { null }
    val shuffleMode = extras?.getInt("android.support.v4.media.session.extra.SHUFFLE_MODE") ?: 0
    val isShuffle = shuffleMode == 1 || shuffleMode == 2
    
    val lastUpdate = playbackState?.lastPositionUpdateTime ?: android.os.SystemClock.elapsedRealtime()

    return MediaState(
        isPlaying = isPlaying,
        title = title,
        artist = artist,
        progress = progress.coerceIn(0f, 1f),
        duration = duration,
        position = position,
        albumArt = albumArt,
        appIcon = appIcon,
        appName = appName,
        isShuffleEnabled = isShuffle,
        lastUpdateTime = lastUpdate
    )
}

// --- MEDIA CONTROLS PILL & CARD ---
@Composable
fun MediaView(
    state: MediaState,
    currentState: NowBarState,
    color: Color,
    albumArtCornerRadius: Int,
    visualizerStyle: String,
    visualizerSensitivity: Float,
    progressVisibility: Boolean,
    onSeekTo: (Long) -> Unit,
    onInteraction: () -> Unit,
    contentAlpha: Float = 1f,
    controlsAlpha: Float = 1f,
    controlsOffsetY: androidx.compose.ui.unit.Dp = 0.dp,
    textSizeOffset: Float = 0f,
    splitSegment: SplitSegment? = null
) {
    val initialPosition = remember(state.position, state.isPlaying, state.lastUpdateTime) {
        if (state.isPlaying && state.lastUpdateTime > 0) {
            val elapsedSinceUpdate = android.os.SystemClock.elapsedRealtime() - state.lastUpdateTime
            (state.position + elapsedSinceUpdate).coerceAtMost(state.duration)
        } else {
            state.position
        }
    }

    val playbackPositionFlow = remember(state.position, state.isPlaying, state.lastUpdateTime) {
        kotlinx.coroutines.flow.MutableStateFlow(initialPosition)
    }

    LaunchedEffect(state.position, state.isPlaying, state.lastUpdateTime) {
        if (state.isPlaying && state.lastUpdateTime > 0) {
            val basePosition = state.position
            val baseTime = state.lastUpdateTime
            while (true) {
                val elapsed = android.os.SystemClock.elapsedRealtime() - baseTime
                playbackPositionFlow.value = (basePosition + elapsed).coerceAtMost(state.duration)
                delay(250L)
            }
        } else {
            playbackPositionFlow.value = state.position
        }
    }

    if (splitSegment != null) {
        if (splitSegment == SplitSegment.LEFT) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                MediaAlbumArtSection(
                    albumArt = state.albumArt,
                    color = color,
                    albumArtCornerRadius = albumArtCornerRadius,
                    sizeDp = if (currentState == NowBarState.MINIMIZED) 20 else 26
                )
            }
        } else {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                MediaVisualizerSection(
                    style = visualizerStyle,
                    sensitivity = visualizerSensitivity,
                    isPlaying = state.isPlaying,
                    color = color,
                    widthDp = if (currentState == NowBarState.MINIMIZED) 24 else 22,
                    heightDp = if (currentState == NowBarState.MINIMIZED) 12 else 10
                )
            }
        }
        return
    }

    when (currentState) {
        NowBarState.MINIMIZED -> {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
            ) {
                MediaAlbumArtSection(
                    albumArt = state.albumArt,
                    color = color,
                    albumArtCornerRadius = albumArtCornerRadius,
                    sizeDp = 20
                )
                
                MediaVisualizerSection(
                    style = visualizerStyle,
                    sensitivity = visualizerSensitivity,
                    isPlaying = state.isPlaying,
                    color = color,
                    widthDp = 24,
                    heightDp = 12
                )
            }
        }
        NowBarState.COMPACT -> {
            val currentPositionMs by playbackPositionFlow.collectAsState()
            val continuousProgress = if (state.duration > 0) {
                currentPositionMs.toFloat() / state.duration
            } else {
                0f
            }

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                MediaAlbumArtSection(
                    albumArt = state.albumArt,
                    color = color,
                    albumArtCornerRadius = albumArtCornerRadius,
                    sizeDp = 26
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.title.ifEmpty { "Not Playing" },
                        color = color,
                        fontSize = (12f + textSizeOffset).sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (progressVisibility && state.duration > 0) {
                        Spacer(modifier = Modifier.height(2.dp))
                        LinearProgressIndicator(
                            progress = continuousProgress,
                            color = color,
                            trackColor = color.copy(alpha = 0.2f),
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .height(2.dp)
                                .clip(CircleShape)
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Previous button
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .clickable {
                                onInteraction()
                                com.novabar.app.services.NovaNotificationListener.skipToPrevious()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(10.dp)) {
                            val path = Path().apply {
                                moveTo(size.width * 0.8f, size.height * 0.2f)
                                lineTo(size.width * 0.35f, size.height * 0.5f)
                                lineTo(size.width * 0.8f, size.height * 0.8f)
                                close()
                            }
                            drawPath(path, color)
                            drawRect(color, Offset(size.width * 0.15f, size.height * 0.2f), Size(size.width * 0.15f, size.height * 0.6f))
                        }
                    }
                    
                    // Play / Pause button
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(color.copy(alpha = 0.15f))
                            .clickable {
                                onInteraction()
                                if (state.isPlaying) {
                                    com.novabar.app.services.NovaNotificationListener.pause()
                                } else {
                                    com.novabar.app.services.NovaNotificationListener.play()
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (state.isPlaying) {
                            Canvas(modifier = Modifier.size(10.dp)) {
                                drawRect(color, Offset(size.width * 0.25f, size.height * 0.2f), Size(size.width * 0.15f, size.height * 0.6f))
                                drawRect(color, Offset(size.width * 0.6f, size.height * 0.2f), Size(size.width * 0.15f, size.height * 0.6f))
                            }
                        } else {
                            Canvas(modifier = Modifier.size(10.dp)) {
                                val path = Path().apply {
                                    moveTo(size.width * 0.3f, size.height * 0.2f)
                                    lineTo(size.width * 0.8f, size.height * 0.5f)
                                    lineTo(size.width * 0.3f, size.height * 0.8f)
                                    close()
                                }
                                drawPath(path, color)
                            }
                        }
                    }
                    
                    // Next button
                    Box(
                        modifier = Modifier
                            .size(24.dp)
                            .clip(CircleShape)
                            .clickable {
                                onInteraction()
                                com.novabar.app.services.NovaNotificationListener.skipToNext()
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Canvas(modifier = Modifier.size(10.dp)) {
                            val path = Path().apply {
                                moveTo(size.width * 0.2f, size.height * 0.2f)
                                lineTo(size.width * 0.65f, size.height * 0.5f)
                                lineTo(size.width * 0.2f, size.height * 0.8f)
                                close()
                            }
                            drawPath(path, color)
                            drawRect(color, Offset(size.width * 0.7f, size.height * 0.2f), Size(size.width * 0.15f, size.height * 0.6f))
                        }
                    }
                }
            }
        }
        NowBarState.EXPANDED -> {
            val context = LocalContext.current
            LaunchedEffect(Unit) {
                val directState = withContext(Dispatchers.IO) {
                    queryCurrentMediaStateDirectly(context)
                }
                if (directState != null) {
                    OverlayStateManager.mediaState.value = directState
                }
            }

            LaunchedEffect(state.title, state.artist) {
                if (state.title.isNotEmpty() || state.artist.isNotEmpty()) {
                    val elapsed = System.currentTimeMillis() - DiagnosticsManager.expandClickTime
                    Log.d("NovaBar", "MEDIA_EXPAND_CONTENT_READY: elapsed=${elapsed}ms")
                }
            }

            LaunchedEffect(state.albumArt) {
                if (state.albumArt != null) {
                    val elapsed = System.currentTimeMillis() - DiagnosticsManager.expandClickTime
                    Log.d("NovaBar", "MEDIA_ALBUM_ART_READY: elapsed=${elapsed}ms")
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .graphicsLayer { alpha = contentAlpha },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    MediaAlbumArtSection(
                        albumArt = state.albumArt,
                        color = color,
                        albumArtCornerRadius = albumArtCornerRadius,
                        sizeDp = 40
                    )

                    MediaMetadataSection(
                        title = state.title,
                        artist = state.artist,
                        color = color,
                        modifier = Modifier.weight(1f)
                    )

                    MediaVisualizerSection(
                        style = visualizerStyle,
                        sensitivity = visualizerSensitivity,
                        isPlaying = state.isPlaying,
                        color = color.copy(alpha = 0.8f),
                        widthDp = 36,
                        heightDp = 14,
                        paddingHorizontalDp = 4
                    )

                    MediaAppIconSection(
                        appIcon = state.appIcon
                    )
                }

                PlaybackSeekBar(
                    playbackPositionStateFlow = playbackPositionFlow,
                    duration = state.duration,
                    color = color,
                    onSeekTo = onSeekTo,
                    onInteraction = onInteraction,
                    controlsAlpha = controlsAlpha,
                    controlsOffsetY = controlsOffsetY,
                    songId = state.title + "_" + state.artist,
                    isPlaying = state.isPlaying,
                    textSizeOffset = textSizeOffset
                )

                MediaControlsSection(
                    isPlaying = state.isPlaying,
                    color = color,
                    onInteraction = onInteraction,
                    controlsAlpha = controlsAlpha,
                    controlsOffsetY = controlsOffsetY
                )
            }
        }
    }
}

@Composable
fun MediaAlbumArtSection(
    albumArt: Bitmap?,
    color: Color,
    albumArtCornerRadius: Int,
    sizeDp: Int
) {
    val hasArt = albumArt != null
    val artAlpha by animateFloatAsState(
        targetValue = if (hasArt) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "albumArtAlpha"
    )
    
    Box(
        modifier = Modifier.size(sizeDp.dp),
        contentAlignment = Alignment.Center
    ) {
        if (!hasArt || artAlpha < 1f) {
            FallbackArtIcon(color = color, sizeDp = sizeDp)
        }
        
        if (hasArt) {
            Image(
                bitmap = albumArt!!.asImageBitmap(),
                contentDescription = "Album Art",
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = artAlpha }
                    .clip(RoundedCornerShape(albumArtCornerRadius.dp))
            )
        }
    }
}

@Composable
fun MediaMetadataSection(
    title: String,
    artist: String,
    color: Color,
    textSizeOffset: Float = 0f,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            color = color,
            fontSize = (13f + textSizeOffset).sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = artist.ifEmpty { "Unknown Artist" },
            color = color.copy(alpha = 0.7f),
            fontSize = (11f + textSizeOffset).sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun MediaVisualizerSection(
    style: String,
    sensitivity: Float,
    isPlaying: Boolean,
    color: Color,
    widthDp: Int,
    heightDp: Int,
    paddingHorizontalDp: Int = 0
) {
    AudioVisualizer(
        style = style,
        sensitivity = sensitivity,
        isPlaying = isPlaying,
        color = color,
        modifier = Modifier
            .width(widthDp.dp)
            .height(heightDp.dp)
            .padding(horizontal = paddingHorizontalDp.dp)
    )
}

@Composable
fun MediaAppIconSection(
    appIcon: Drawable?
) {
    val appIconBitmap = remember(appIcon) {
        drawableToBitmap(appIcon)?.asImageBitmap()
    }
    if (appIconBitmap != null) {
        Image(
            bitmap = appIconBitmap,
            contentDescription = "App Icon",
            modifier = Modifier
                .size(18.dp)
                .clip(CircleShape)
        )
    }
}

@Composable
fun MediaControlsSection(
    isPlaying: Boolean,
    color: Color,
    onInteraction: () -> Unit,
    controlsAlpha: Float,
    controlsOffsetY: androidx.compose.ui.unit.Dp
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
            .graphicsLayer {
                alpha = controlsAlpha
                translationY = controlsOffsetY.toPx()
            },
        horizontalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val prevInteraction = remember { MutableInteractionSource() }
        val isPrevPressed by prevInteraction.collectIsPressedAsState()
        val prevScale by animateFloatAsState(if (isPrevPressed) 0.90f else 1f, label = "PrevScale")

        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.1f))
                .graphicsLayer {
                    scaleX = prevScale
                    scaleY = prevScale
                }
                .clickable(
                    interactionSource = prevInteraction,
                    indication = androidx.compose.foundation.LocalIndication.current,
                    onClick = {
                        onInteraction()
                        com.novabar.app.services.NovaNotificationListener.skipToPrevious()
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(16.dp)) {
                val path = Path().apply {
                    moveTo(size.width * 0.75f, size.height * 0.2f)
                    lineTo(size.width * 0.3f, size.height * 0.5f)
                    lineTo(size.width * 0.75f, size.height * 0.8f)
                    close()
                }
                drawPath(path, color)
                drawRect(color, Offset(size.width * 0.15f, size.height * 0.2f), Size(size.width * 0.12f, size.height * 0.6f))
            }
        }

        val playPauseInteraction = remember { MutableInteractionSource() }
        val isPlayPausePressed by playPauseInteraction.collectIsPressedAsState()
        val playPauseScale by animateFloatAsState(if (isPlayPausePressed) 0.90f else 1f, label = "PlayPauseScale")

        val playPauseBgColor = color
        val playPauseIconColor = if (color.luminance() > 0.5f) Color(0xFF1C1C1E) else Color.White
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(playPauseBgColor)
                .graphicsLayer {
                    scaleX = playPauseScale
                    scaleY = playPauseScale
                }
                .clickable(
                    interactionSource = playPauseInteraction,
                    indication = androidx.compose.foundation.LocalIndication.current,
                    onClick = {
                        onInteraction()
                        if (isPlaying) {
                            com.novabar.app.services.NovaNotificationListener.pause()
                        } else {
                            com.novabar.app.services.NovaNotificationListener.play()
                        }
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(24.dp)) {
                if (isPlaying) {
                    val w = size.width * 0.14f
                    val gap = size.width * 0.18f
                    drawRect(playPauseIconColor, Offset(size.width * 0.26f, size.height * 0.2f), Size(w, size.height * 0.6f))
                    drawRect(playPauseIconColor, Offset(size.width * 0.26f + w + gap, size.height * 0.2f), Size(w, size.height * 0.6f))
                } else {
                    val path = Path().apply {
                        moveTo(size.width * 0.32f, size.height * 0.18f)
                        lineTo(size.width * 0.82f, size.height * 0.5f)
                        lineTo(size.width * 0.32f, size.height * 0.82f)
                        close()
                    }
                    drawPath(path, playPauseIconColor)
                }
            }
        }

        val nextInteraction = remember { MutableInteractionSource() }
        val isNextPressed by nextInteraction.collectIsPressedAsState()
        val nextScale by animateFloatAsState(if (isNextPressed) 0.90f else 1f, label = "NextScale")

        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.1f))
                .graphicsLayer {
                    scaleX = nextScale
                    scaleY = nextScale
                }
                .clickable(
                    interactionSource = nextInteraction,
                    indication = androidx.compose.foundation.LocalIndication.current,
                    onClick = {
                        onInteraction()
                        com.novabar.app.services.NovaNotificationListener.skipToNext()
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.size(16.dp)) {
                val path = Path().apply {
                    moveTo(size.width * 0.25f, size.height * 0.2f)
                    lineTo(size.width * 0.7f, size.height * 0.5f)
                    lineTo(size.width * 0.25f, size.height * 0.8f)
                    close()
                }
                drawPath(path, color)
                drawRect(color, Offset(size.width * 0.73f, size.height * 0.2f), Size(size.width * 0.12f, size.height * 0.6f))
            }
        }
    }
}

@Composable
fun WavyProgressSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: (Float) -> Unit,
    color: Color,
    songId: String,
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    var width by remember { mutableStateOf(1f) }
    val density = androidx.compose.ui.platform.LocalDensity.current
    
    val amplitude = remember(density) { with(density) { 3.dp.toPx() } }
    val wavelength = remember(density) { with(density) { 22.dp.toPx() } }
    val strokeWidth = remember(density) { with(density) { 3.dp.toPx() } }
    val thumbRadius = remember(density) { with(density) { 5.dp.toPx() } }

    val fullPath = remember(width, songId) {
        Path().apply {
            if (width > 1f) {
                moveTo(0f, 0f)
                var x = 0f
                val limit = width + wavelength
                while (x <= limit) {
                    val y = amplitude * kotlin.math.sin(2.0 * Math.PI * x / wavelength).toFloat()
                    lineTo(x, y)
                    x += 2f
                }
            }
        }
    }

    val progressX = width * value
    
    var phase by remember { mutableStateOf(0f) }

    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            val speed = 2.0 * Math.PI / 4.0 // 1 complete cycle every 4 seconds
            var lastTime = android.os.SystemClock.elapsedRealtime()
            while (true) {
                withFrameNanos {
                    val now = android.os.SystemClock.elapsedRealtime()
                    val delta = (now - lastTime) / 1000f
                    lastTime = now
                    phase = (phase + speed * delta).toFloat() % (2f * Math.PI.toFloat())
                }
            }
        }
    }

    val dx = (phase / (2f * Math.PI.toFloat())) * wavelength

    val latestOnValueChange by rememberUpdatedState(onValueChange)
    val latestOnValueChangeFinished by rememberUpdatedState(onValueChangeFinished)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(24.dp)
            .onGloballyPositioned { coordinates ->
                width = coordinates.size.width.toFloat()
            }
            .pointerInput(width) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val startX = down.position.x
                        var currentValue = (startX / width).coerceIn(0f, 1f)
                        latestOnValueChange(currentValue)
                        
                        var isDragging = false
                        val pointerId = down.id
                        
                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull { it.id == pointerId }
                            if (change == null || !change.pressed) {
                                latestOnValueChangeFinished(currentValue)
                                break
                            }
                            
                            val currentX = change.position.x
                            if (!isDragging && kotlin.math.abs(currentX - startX) > viewConfiguration.touchSlop) {
                                isDragging = true
                            }
                            
                            if (isDragging) {
                                currentValue = (currentX / width).coerceIn(0f, 1f)
                                latestOnValueChange(currentValue)
                                change.consume()
                            }
                        }
                    }
                }
            }
    ) {
        Canvas(
            modifier = Modifier.fillMaxSize()
        ) {
            val cy = size.height / 2f
            
            // Draw remaining portion (flat horizontal line)
            drawLine(
                color = color.copy(alpha = 0.24f),
                start = Offset(progressX, cy),
                end = Offset(size.width, cy),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round
            )
            
            // Draw played portion (sine wave)
            clipRect(
                left = 0f,
                top = 0f,
                right = progressX,
                bottom = size.height
            ) {
                translate(left = -dx, top = cy) {
                    drawPath(
                        path = fullPath,
                        color = color,
                        style = androidx.compose.ui.graphics.drawscope.Stroke(
                            width = strokeWidth,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }
            }
            
            // Draw start round cap for the wave if progress > 0
            if (progressX > 0f) {
                val yStart = cy + amplitude * kotlin.math.sin(2.0 * Math.PI * dx / wavelength).toFloat()
                drawCircle(
                    color = color,
                    radius = strokeWidth / 2f,
                    center = Offset(0f, yStart)
                )
            }
            
            // Draw progress thumb
            val yProgress = cy + amplitude * kotlin.math.sin(2.0 * Math.PI * (progressX + dx) / wavelength).toFloat()
            drawCircle(
                color = color,
                radius = thumbRadius,
                center = Offset(progressX, yProgress)
            )
        }
    }
}

@Composable
fun PlaybackSeekBar(
    playbackPositionStateFlow: kotlinx.coroutines.flow.StateFlow<Long>,
    duration: Long,
    color: Color,
    onSeekTo: (Long) -> Unit,
    onInteraction: () -> Unit,
    controlsAlpha: Float = 1f,
    controlsOffsetY: androidx.compose.ui.unit.Dp = 0.dp,
    songId: String = "",
    isPlaying: Boolean = false,
    textSizeOffset: Float = 0f
) {
    val currentPosition by playbackPositionStateFlow.collectAsState()
    var dragPosition by remember { mutableStateOf<Float?>(null) }
    
    val displayPositionMs = if (dragPosition != null) {
        (dragPosition!! * duration).toLong()
    } else {
        currentPosition
    }

    val sliderProgress = if (duration > 0) {
        (displayPositionMs.toFloat() / duration).coerceIn(0f, 1f)
    } else {
        0f
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp)
            .graphicsLayer {
                alpha = controlsAlpha
                translationY = controlsOffsetY.toPx()
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = formatTime(displayPositionMs),
            color = color.copy(alpha = 0.6f),
            fontSize = (10f + textSizeOffset).sp
        )
        WavyProgressSlider(
            value = sliderProgress,
            onValueChange = {
                onInteraction()
                dragPosition = it
            },
            onValueChangeFinished = { finalValue ->
                val seekMs = (finalValue * duration).toLong()
                onSeekTo(seekMs)
                dragPosition = null
            },
            color = color,
            songId = songId,
            isPlaying = isPlaying,
            modifier = Modifier
                .weight(1f)
                .height(24.dp)
                .padding(horizontal = 8.dp)
        )
        Text(
            text = formatTime(duration),
            color = color.copy(alpha = 0.6f),
            fontSize = (10f + textSizeOffset).sp
        )
    }
}

@Composable
fun TorchIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        
        // Flashlight head: trapezoid in top half
        val headPath = Path().apply {
            moveTo(w * 0.35f, h * 0.4f)
            lineTo(w * 0.25f, h * 0.15f)
            lineTo(w * 0.75f, h * 0.15f)
            lineTo(w * 0.65f, h * 0.4f)
            close()
        }
        drawPath(headPath, color)
        
        // Flashlight handle: rectangle in bottom half
        val handlePath = Path().apply {
            moveTo(w * 0.4f, h * 0.4f)
            lineTo(w * 0.6f, h * 0.4f)
            lineTo(w * 0.6f, h * 0.85f)
            lineTo(w * 0.4f, h * 0.85f)
            close()
        }
        drawPath(handlePath, color)
        
        // Light bulb details: small dot on handle
        drawCircle(
            color = if (color == Color.White) Color.Black else Color.White,
            radius = w * 0.04f,
            center = Offset(w * 0.5f, h * 0.55f)
        )
    }
}

@Composable
fun TorchView(
    state: TorchState,
    currentState: NowBarState,
    color: Color,
    textSizeOffset: Float,
    splitSegment: SplitSegment? = null,
    onInteraction: () -> Unit
) {
    val sizeOffset = textSizeOffset
    val isStrengthSupported = state.isStrengthSupported
    val pct = state.brightnessPercentage

    LaunchedEffect(state) {
        Log.d("TorchView", "Received TorchState in UI: isActive=${state.isActive}, brightness=${pct}%, isStrengthSupported=$isStrengthSupported")
    }

    if (splitSegment != null) {
        if (splitSegment == SplitSegment.LEFT) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                val iconSize = if (currentState == NowBarState.MINIMIZED) 12.dp else 14.dp
                TorchIcon(color = color, modifier = Modifier.size(iconSize))
            }
        } else {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                val label = if (isStrengthSupported) "Torch • $pct%" else "Torch"
                Text(
                    text = label,
                    color = color,
                    fontSize = (if (currentState == NowBarState.MINIMIZED) 11f else 12f + sizeOffset).sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        return
    }

    when (currentState) {
        NowBarState.MINIMIZED -> {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
            ) {
                TorchIcon(color = color, modifier = Modifier.size(12.dp))
                val label = if (isStrengthSupported) "Torch • $pct%" else "Torch"
                Text(
                    text = label,
                    color = color,
                    fontSize = (11f + sizeOffset).sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        NowBarState.COMPACT -> {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TorchIcon(color = color, modifier = Modifier.size(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Torch",
                        color = color,
                        fontSize = (12f + sizeOffset).sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                    if (isStrengthSupported) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "Brightness: $pct%",
                            color = color.copy(alpha = 0.6f),
                            fontSize = (10f + sizeOffset).sp,
                            maxLines = 1
                        )
                    }
                }
            }
        }
        NowBarState.EXPANDED -> {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TorchIcon(color = color, modifier = Modifier.size(24.dp))
                    Column {
                        Text(
                            text = "Torch",
                            color = color,
                            fontSize = (15f + sizeOffset).sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = if (isStrengthSupported) "Adjustable Brightness" else "Active",
                            color = color.copy(alpha = 0.5f),
                            fontSize = (10f + sizeOffset).sp
                        )
                    }
                }

                if (isStrengthSupported) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            text = "$pct%",
                            color = color,
                            fontSize = (20f + sizeOffset).sp,
                            fontWeight = FontWeight.Bold
                        )
                        Slider(
                            value = pct.toFloat() / 100f,
                            onValueChange = {
                                onInteraction()
                                val newPct = (it * 100).toInt().coerceIn(1, 100)
                                TorchManager.setTorchBrightnessPercentage(newPct)
                            },
                            valueRange = 0.01f..1f,
                            colors = SliderDefaults.colors(
                                thumbColor = color,
                                activeTrackColor = color,
                                inactiveTrackColor = color.copy(alpha = 0.2f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(18.dp)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "Flashlight Active",
                            color = color.copy(alpha = 0.8f),
                            fontSize = (14f + sizeOffset).sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Button(
                    onClick = {
                        onInteraction()
                        TorchManager.setTorchEnabled(false)
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFF44336)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 6.dp)
                ) {
                    Text("Turn Off", color = Color.White, fontSize = (12f + sizeOffset).sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun HotspotIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val cy = h * 0.5f
        
        // Center dot
        drawCircle(
            color = color,
            radius = w * 0.12f,
            center = Offset(cx, cy)
        )
        
        // Inner ring
        drawCircle(
            color = color,
            radius = w * 0.28f,
            center = Offset(cx, cy),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.08f)
        )
        
        // Outer ring
        drawCircle(
            color = color,
            radius = w * 0.44f,
            center = Offset(cx, cy),
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = w * 0.08f)
        )
    }
}

@Composable
fun HotspotView(
    state: HotspotState,
    currentState: NowBarState,
    color: Color,
    textSizeOffset: Float,
    splitSegment: SplitSegment? = null,
    onTurnOffClick: () -> Unit
) {
    val sizeOffset = textSizeOffset

    if (splitSegment != null) {
        if (splitSegment == SplitSegment.LEFT) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                val iconSize = if (currentState == NowBarState.MINIMIZED) 12.dp else 14.dp
                HotspotIcon(color = color, modifier = Modifier.size(iconSize))
            }
        } else {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Hotspot",
                    color = color,
                    fontSize = (if (currentState == NowBarState.MINIMIZED) 11f else 12f + sizeOffset).sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        return
    }

    when (currentState) {
        NowBarState.MINIMIZED -> {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
            ) {
                HotspotIcon(color = color, modifier = Modifier.size(12.dp))
                Text(
                    text = "Hotspot",
                    color = color,
                    fontSize = (11f + sizeOffset).sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        NowBarState.COMPACT -> {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                HotspotIcon(color = color, modifier = Modifier.size(14.dp))
                Text(
                    text = "Hotspot",
                    color = color,
                    fontSize = (12f + sizeOffset).sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
            }
        }
        NowBarState.EXPANDED -> {
            if (state.isDisableSupported) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        HotspotIcon(color = color, modifier = Modifier.size(24.dp))
                        Column {
                            Text(
                                text = "Hotspot Active",
                                color = color,
                                fontSize = (15f + sizeOffset).sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    
                    Button(
                        onClick = onTurnOffClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = color.copy(alpha = 0.15f),
                            contentColor = color
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                    ) {
                        Text(
                            text = "Turn Off",
                            fontSize = (13f + sizeOffset).sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ManeuverIcon(type: com.novabar.app.domain.ManeuverType, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val cx = w * 0.5f
        val cy = h * 0.5f
        val strokeWidth = w * 0.12f
        
        when (type) {
            com.novabar.app.domain.ManeuverType.STRAIGHT -> {
                drawLine(
                    color = color,
                    start = Offset(cx, h * 0.85f),
                    end = Offset(cx, h * 0.15f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = color,
                    start = Offset(cx - w * 0.22f, h * 0.37f),
                    end = Offset(cx, h * 0.15f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = color,
                    start = Offset(cx + w * 0.22f, h * 0.37f),
                    end = Offset(cx, h * 0.15f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }
            com.novabar.app.domain.ManeuverType.LEFT -> {
                val path = Path().apply {
                    moveTo(cx, h * 0.85f)
                    lineTo(cx, h * 0.5f)
                    quadraticBezierTo(cx, h * 0.35f, cx - w * 0.15f, h * 0.35f)
                    lineTo(w * 0.15f, h * 0.35f)
                }
                drawPath(path, color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawLine(
                    color = color,
                    start = Offset(w * 0.35f, h * 0.15f),
                    end = Offset(w * 0.15f, h * 0.35f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = color,
                    start = Offset(w * 0.35f, h * 0.55f),
                    end = Offset(w * 0.15f, h * 0.35f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }
            com.novabar.app.domain.ManeuverType.RIGHT -> {
                val path = Path().apply {
                    moveTo(cx, h * 0.85f)
                    lineTo(cx, h * 0.5f)
                    quadraticBezierTo(cx, h * 0.35f, cx + w * 0.15f, h * 0.35f)
                    lineTo(w * 0.85f, h * 0.35f)
                }
                drawPath(path, color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawLine(
                    color = color,
                    start = Offset(w * 0.65f, h * 0.15f),
                    end = Offset(w * 0.85f, h * 0.35f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = color,
                    start = Offset(w * 0.65f, h * 0.55f),
                    end = Offset(w * 0.85f, h * 0.35f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }
            com.novabar.app.domain.ManeuverType.SLIGHT_LEFT -> {
                val path = Path().apply {
                    moveTo(cx + w * 0.15f, h * 0.85f)
                    lineTo(cx - w * 0.15f, h * 0.35f)
                }
                drawPath(path, color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth, cap = StrokeCap.Round))
                drawLine(
                    color = color,
                    start = Offset(cx - w * 0.35f, h * 0.55f),
                    end = Offset(cx - w * 0.15f, h * 0.35f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = color,
                    start = Offset(cx - w * 0.05f, h * 0.15f),
                    end = Offset(cx - w * 0.15f, h * 0.35f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }
            com.novabar.app.domain.ManeuverType.SLIGHT_RIGHT -> {
                val path = Path().apply {
                    moveTo(cx - w * 0.15f, h * 0.85f)
                    lineTo(cx + w * 0.15f, h * 0.35f)
                }
                drawPath(path, color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth, cap = StrokeCap.Round))
                drawLine(
                    color = color,
                    start = Offset(cx + w * 0.35f, h * 0.55f),
                    end = Offset(cx + w * 0.15f, h * 0.35f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = color,
                    start = Offset(cx + w * 0.05f, h * 0.15f),
                    end = Offset(cx + w * 0.15f, h * 0.35f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }
            com.novabar.app.domain.ManeuverType.SHARP_LEFT -> {
                val path = Path().apply {
                    moveTo(cx + w * 0.2f, h * 0.85f)
                    lineTo(cx + w * 0.2f, h * 0.25f)
                    lineTo(w * 0.15f, h * 0.25f)
                }
                drawPath(path, color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Miter))
                drawLine(
                    color = color,
                    start = Offset(w * 0.35f, h * 0.05f),
                    end = Offset(w * 0.15f, h * 0.25f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = color,
                    start = Offset(w * 0.35f, h * 0.45f),
                    end = Offset(w * 0.15f, h * 0.25f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }
            com.novabar.app.domain.ManeuverType.SHARP_RIGHT -> {
                val path = Path().apply {
                    moveTo(cx - w * 0.2f, h * 0.85f)
                    lineTo(cx - w * 0.2f, h * 0.25f)
                    lineTo(w * 0.85f, h * 0.25f)
                }
                drawPath(path, color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Miter))
                drawLine(
                    color = color,
                    start = Offset(w * 0.65f, h * 0.05f),
                    end = Offset(w * 0.85f, h * 0.25f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = color,
                    start = Offset(w * 0.65f, h * 0.45f),
                    end = Offset(w * 0.85f, h * 0.25f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }
            com.novabar.app.domain.ManeuverType.KEEP_LEFT -> {
                val pathRight = Path().apply {
                    moveTo(cx, h * 0.85f)
                    quadraticBezierTo(cx, h * 0.55f, cx + w * 0.25f, h * 0.25f)
                }
                drawPath(pathRight, color.copy(alpha = 0.3f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth, cap = StrokeCap.Round))
                val pathLeft = Path().apply {
                    moveTo(cx, h * 0.85f)
                    quadraticBezierTo(cx, h * 0.55f, cx - w * 0.25f, h * 0.25f)
                }
                drawPath(pathLeft, color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth, cap = StrokeCap.Round))
                drawLine(
                    color = color,
                    start = Offset(cx - w * 0.35f, h * 0.45f),
                    end = Offset(cx - w * 0.25f, h * 0.25f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = color,
                    start = Offset(cx - w * 0.1f, h * 0.2f),
                    end = Offset(cx - w * 0.25f, h * 0.25f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }
            com.novabar.app.domain.ManeuverType.KEEP_RIGHT -> {
                val pathLeft = Path().apply {
                    moveTo(cx, h * 0.85f)
                    quadraticBezierTo(cx, h * 0.55f, cx - w * 0.25f, h * 0.25f)
                }
                drawPath(pathLeft, color.copy(alpha = 0.3f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth, cap = StrokeCap.Round))
                val pathRight = Path().apply {
                    moveTo(cx, h * 0.85f)
                    quadraticBezierTo(cx, h * 0.55f, cx + w * 0.25f, h * 0.25f)
                }
                drawPath(pathRight, color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth, cap = StrokeCap.Round))
                drawLine(
                    color = color,
                    start = Offset(cx + w * 0.35f, h * 0.45f),
                    end = Offset(cx + w * 0.25f, h * 0.25f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = color,
                    start = Offset(cx + w * 0.1f, h * 0.2f),
                    end = Offset(cx + w * 0.25f, h * 0.25f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }
            com.novabar.app.domain.ManeuverType.MERGE -> {
                drawLine(
                    color = color,
                    start = Offset(cx - w * 0.1f, h * 0.85f),
                    end = Offset(cx - w * 0.1f, h * 0.15f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                val pathMerge = Path().apply {
                    moveTo(cx + w * 0.25f, h * 0.75f)
                    quadraticBezierTo(cx + w * 0.15f, h * 0.5f, cx - w * 0.1f, h * 0.45f)
                }
                drawPath(pathMerge, color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth, cap = StrokeCap.Round))
                drawLine(
                    color = color,
                    start = Offset(cx - w * 0.3f, h * 0.35f),
                    end = Offset(cx - w * 0.1f, h * 0.15f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = color,
                    start = Offset(cx + w * 0.1f, h * 0.35f),
                    end = Offset(cx - w * 0.1f, h * 0.15f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }
            com.novabar.app.domain.ManeuverType.EXIT -> {
                drawLine(
                    color = color.copy(alpha = 0.3f),
                    start = Offset(cx - w * 0.15f, h * 0.85f),
                    end = Offset(cx - w * 0.15f, h * 0.15f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                val pathExit = Path().apply {
                    moveTo(cx - w * 0.15f, h * 0.65f)
                    quadraticBezierTo(cx - w * 0.05f, h * 0.5f, cx + w * 0.25f, h * 0.3f)
                }
                drawPath(pathExit, color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth, cap = StrokeCap.Round))
                drawLine(
                    color = color,
                    start = Offset(cx + w * 0.3f, h * 0.5f),
                    end = Offset(cx + w * 0.25f, h * 0.3f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = color,
                    start = Offset(cx + w * 0.1f, h * 0.2f),
                    end = Offset(cx + w * 0.25f, h * 0.3f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }
            com.novabar.app.domain.ManeuverType.UTURN -> {
                val path = Path().apply {
                    moveTo(cx + w * 0.15f, h * 0.85f)
                    lineTo(cx + w * 0.15f, h * 0.35f)
                    quadraticBezierTo(cx + w * 0.15f, h * 0.15f, cx, h * 0.15f)
                    quadraticBezierTo(cx - w * 0.15f, h * 0.15f, cx - w * 0.15f, h * 0.35f)
                    lineTo(cx - w * 0.15f, h * 0.75f)
                }
                drawPath(path, color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawLine(
                    color = color,
                    start = Offset(cx - w * 0.3f, h * 0.55f),
                    end = Offset(cx - w * 0.15f, h * 0.75f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = color,
                    start = Offset(cx, h * 0.55f),
                    end = Offset(cx - w * 0.15f, h * 0.75f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }
            com.novabar.app.domain.ManeuverType.ROUNDABOUT_ENTER -> {
                drawCircle(
                    color = color,
                    radius = w * 0.3f,
                    center = Offset(cx, cy),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
                )
                drawLine(
                    color = color,
                    start = Offset(cx - w * 0.1f, cy - w * 0.42f),
                    end = Offset(cx + w * 0.12f, cy - w * 0.3f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = color,
                    start = Offset(cx - w * 0.05f, cy - w * 0.12f),
                    end = Offset(cx + w * 0.12f, cy - w * 0.3f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = color,
                    start = Offset(cx, h * 0.9f),
                    end = Offset(cx, cy + w * 0.3f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }
            com.novabar.app.domain.ManeuverType.ROUNDABOUT_EXIT -> {
                drawCircle(
                    color = color.copy(alpha = 0.3f),
                    radius = w * 0.28f,
                    center = Offset(cx - w * 0.05f, cy + h * 0.05f),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
                )
                val path = Path().apply {
                    moveTo(cx - w * 0.05f, h * 0.85f)
                    lineTo(cx - w * 0.05f, cy + h * 0.05f)
                    quadraticBezierTo(cx - w * 0.05f, cy - h * 0.2f, cx + w * 0.25f, cy - h * 0.25f)
                }
                drawPath(path, color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawLine(
                    color = color,
                    start = Offset(cx + w * 0.1f, cy - h * 0.42f),
                    end = Offset(cx + w * 0.25f, cy - h * 0.25f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = color,
                    start = Offset(cx + 0.12f * w, cy - h * 0.05f),
                    end = Offset(cx + w * 0.25f, cy - h * 0.25f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }
            com.novabar.app.domain.ManeuverType.DESTINATION,
            com.novabar.app.domain.ManeuverType.ARRIVAL -> {
                val path = Path().apply {
                    moveTo(cx, cy + h * 0.35f)
                    cubicTo(cx - w * 0.25f, cy, cx - w * 0.25f, cy - h * 0.25f, cx, cy - h * 0.35f)
                    cubicTo(cx + w * 0.25f, cy - h * 0.25f, cx + w * 0.25f, cy, cx, cy + h * 0.35f)
                    close()
                }
                drawPath(path, color)
                drawCircle(
                    color = Color.Black.copy(alpha = 0.6f),
                    radius = w * 0.1f,
                    center = Offset(cx, cy - h * 0.08f)
                )
            }
            com.novabar.app.domain.ManeuverType.FORK_LEFT -> {
                drawLine(color = color, start = Offset(cx, h * 0.85f), end = Offset(cx, h * 0.55f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
                val pathLeft = Path().apply {
                    moveTo(cx, h * 0.55f)
                    quadraticBezierTo(cx, h * 0.35f, cx - w * 0.2f, h * 0.25f)
                }
                drawPath(pathLeft, color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth, cap = StrokeCap.Round))
                val pathRight = Path().apply {
                    moveTo(cx, h * 0.55f)
                    quadraticBezierTo(cx, h * 0.35f, cx + w * 0.2f, h * 0.25f)
                }
                drawPath(pathRight, color.copy(alpha = 0.3f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth, cap = StrokeCap.Round))
                drawLine(color = color, start = Offset(cx - w * 0.3f, h * 0.35f), end = Offset(cx - w * 0.2f, h * 0.25f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
                drawLine(color = color, start = Offset(cx - w * 0.1f, h * 0.15f), end = Offset(cx - w * 0.2f, h * 0.25f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
            }
            com.novabar.app.domain.ManeuverType.FORK_RIGHT -> {
                drawLine(color = color, start = Offset(cx, h * 0.85f), end = Offset(cx, h * 0.55f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
                val pathRight = Path().apply {
                    moveTo(cx, h * 0.55f)
                    quadraticBezierTo(cx, h * 0.35f, cx + w * 0.2f, h * 0.25f)
                }
                drawPath(pathRight, color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth, cap = StrokeCap.Round))
                val pathLeft = Path().apply {
                    moveTo(cx, h * 0.55f)
                    quadraticBezierTo(cx, h * 0.35f, cx - w * 0.2f, h * 0.25f)
                }
                drawPath(pathLeft, color.copy(alpha = 0.3f), style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth, cap = StrokeCap.Round))
                drawLine(color = color, start = Offset(cx + w * 0.3f, h * 0.35f), end = Offset(cx + w * 0.2f, h * 0.25f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
                drawLine(color = color, start = Offset(cx + w * 0.1f, h * 0.15f), end = Offset(cx + w * 0.2f, h * 0.25f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
            }
            com.novabar.app.domain.ManeuverType.RAMP_LEFT -> {
                drawLine(color = color.copy(alpha = 0.3f), start = Offset(cx + w * 0.1f, h * 0.85f), end = Offset(cx + w * 0.1f, h * 0.15f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
                val path = Path().apply {
                    moveTo(cx + w * 0.1f, h * 0.6f)
                    quadraticBezierTo(cx - w * 0.15f, h * 0.5f, cx - w * 0.25f, h * 0.25f)
                }
                drawPath(path, color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth, cap = StrokeCap.Round))
                drawLine(color = color, start = Offset(cx - w * 0.35f, h * 0.38f), end = Offset(cx - w * 0.25f, h * 0.25f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
                drawLine(color = color, start = Offset(cx - w * 0.15f, h * 0.17f), end = Offset(cx - w * 0.25f, h * 0.25f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
            }
            com.novabar.app.domain.ManeuverType.RAMP_RIGHT -> {
                drawLine(color = color.copy(alpha = 0.3f), start = Offset(cx - w * 0.1f, h * 0.85f), end = Offset(cx - w * 0.1f, h * 0.15f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
                val path = Path().apply {
                    moveTo(cx - w * 0.1f, h * 0.6f)
                    quadraticBezierTo(cx + w * 0.15f, h * 0.5f, cx + w * 0.25f, h * 0.25f)
                }
                drawPath(path, color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth, cap = StrokeCap.Round))
                drawLine(color = color, start = Offset(cx + w * 0.35f, h * 0.38f), end = Offset(cx + w * 0.25f, h * 0.25f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
                drawLine(color = color, start = Offset(cx + w * 0.15f, h * 0.17f), end = Offset(cx + w * 0.25f, h * 0.25f), strokeWidth = strokeWidth, cap = StrokeCap.Round)
            }
            com.novabar.app.domain.ManeuverType.UNKNOWN -> {
                val path = Path().apply {
                    moveTo(cx - w * 0.15f, h * 0.85f)
                    lineTo(cx - w * 0.15f, h * 0.45f)
                    quadraticBezierTo(cx - w * 0.15f, h * 0.3f, cx, h * 0.3f)
                    lineTo(w * 0.8f, h * 0.3f)
                }
                drawPath(path, color, style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round))
                drawLine(
                    color = color,
                    start = Offset(w * 0.6f, h * 0.1f),
                    end = Offset(w * 0.8f, h * 0.3f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
                drawLine(
                    color = color,
                    start = Offset(w * 0.6f, h * 0.5f),
                    end = Offset(w * 0.8f, h * 0.3f),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round
                )
            }
        }
    }
}

// --- NOVAGUY VIEWS ---

@Composable
fun NovaGuyCompactView(color: Color, pillWidth: androidx.compose.ui.unit.Dp = 185.dp) {
    val eyeOffset by OverlayStateManager.novaGuyEyeOffset.collectAsState()
    var isBlinking by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            delay((2500..6000).random().toLong())
            isBlinking = true
            delay(100L)
            isBlinking = false
        }
    }

    val scaleFactor = (pillWidth.value / 185f).coerceIn(0.5f, 2.0f)

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(width = 44.dp * scaleFactor, height = 20.dp)) {
            val density = this.density
            val offsetPx = eyeOffset * density * scaleFactor * 2.2f
            
            val centerY = size.height / 2f
            val centerX = size.width / 2f
            
            val eyeSpacingPx = 14.dp.toPx()
            val eyeWidthPx = 4.dp.toPx()
            val eyeHeightPx = 7.dp.toPx()
            val eyeCornerRadiusPx = 2.dp.toPx()
            
            val leftEyeX = centerX - eyeSpacingPx / 2f + offsetPx
            val rightEyeX = centerX + eyeSpacingPx / 2f + offsetPx
            
            val gapPx = 2.dp.toPx()
            val connectorThicknessPx = 2.dp.toPx()
            
            val leftInnerEdgeX = leftEyeX + (eyeWidthPx / 2f)
            val rightInnerEdgeX = rightEyeX - (eyeWidthPx / 2f)
            
            val connectorStartX = leftInnerEdgeX + gapPx + (connectorThicknessPx / 2f)
            val connectorEndX = rightInnerEdgeX - gapPx - (connectorThicknessPx / 2f)
            
            // Draw floating connecting line with rounded ends and small symmetrical gaps
            drawLine(
                color = color.copy(alpha = 0.5f),
                start = Offset(connectorStartX, centerY),
                end = Offset(connectorEndX, centerY),
                strokeWidth = connectorThicknessPx,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            
            if (isBlinking) {
                val strokeW = 2.dp.toPx()
                drawLine(
                    color = color,
                    start = Offset(leftEyeX - eyeWidthPx / 2f, centerY),
                    end = Offset(leftEyeX + eyeWidthPx / 2f, centerY),
                    strokeWidth = strokeW,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
                drawLine(
                    color = color,
                    start = Offset(rightEyeX - eyeWidthPx / 2f, centerY),
                    end = Offset(rightEyeX + eyeWidthPx / 2f, centerY),
                    strokeWidth = strokeW,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            } else {
                // Draw left capsule eye
                drawRoundRect(
                    color = color,
                    topLeft = Offset(leftEyeX - eyeWidthPx / 2f, centerY - eyeHeightPx / 2f),
                    size = androidx.compose.ui.geometry.Size(eyeWidthPx, eyeHeightPx),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(eyeCornerRadiusPx, eyeCornerRadiusPx)
                )
                
                // Draw right capsule eye
                drawRoundRect(
                    color = color,
                    topLeft = Offset(rightEyeX - eyeWidthPx / 2f, centerY - eyeHeightPx / 2f),
                    size = androidx.compose.ui.geometry.Size(eyeWidthPx, eyeHeightPx),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(eyeCornerRadiusPx, eyeCornerRadiusPx)
                )
            }
        }
    }
}

@Composable
fun NovaGuyExpandedView(color: Color) {
    val selectedMessage by OverlayStateManager.selectedNovaGuyMessage.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val messageText = selectedMessage?.let { context.getString(it.textResId) } ?: "Hey! 👋"
    var isBlinking by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            delay((2500..6000).random().toLong())
            isBlinking = true
            delay(100L)
            isBlinking = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Centered large NovaGuy face
        Canvas(modifier = Modifier.size(width = 80.dp, height = 40.dp)) {
            val centerY = size.height / 2f
            val centerX = size.width / 2f
            
            val eyeSpacingPx = 28.dp.toPx()
            val eyeWidthPx = 8.dp.toPx()
            val eyeHeightPx = 14.dp.toPx()
            val eyeCornerRadiusPx = 4.dp.toPx()
            
            val leftEyeX = centerX - eyeSpacingPx / 2f
            val rightEyeX = centerX + eyeSpacingPx / 2f
            
            val gapPx = 4.dp.toPx()
            val connectorThicknessPx = 4.dp.toPx()
            
            val leftInnerEdgeX = leftEyeX + (eyeWidthPx / 2f)
            val rightInnerEdgeX = rightEyeX - (eyeWidthPx / 2f)
            
            val connectorStartX = leftInnerEdgeX + gapPx + (connectorThicknessPx / 2f)
            val connectorEndX = rightInnerEdgeX - gapPx - (connectorThicknessPx / 2f)
            
            // Draw floating connecting line with rounded ends and small symmetrical gaps
            drawLine(
                color = color.copy(alpha = 0.5f),
                start = Offset(connectorStartX, centerY),
                end = Offset(connectorEndX, centerY),
                strokeWidth = connectorThicknessPx,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            
            if (isBlinking) {
                val strokeW = 4.dp.toPx()
                drawLine(
                    color = color,
                    start = Offset(leftEyeX - eyeWidthPx / 2f, centerY),
                    end = Offset(leftEyeX + eyeWidthPx / 2f, centerY),
                    strokeWidth = strokeW,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
                drawLine(
                    color = color,
                    start = Offset(rightEyeX - eyeWidthPx / 2f, centerY),
                    end = Offset(rightEyeX + eyeWidthPx / 2f, centerY),
                    strokeWidth = strokeW,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round
                )
            } else {
                // Draw left capsule eye
                drawRoundRect(
                    color = color,
                    topLeft = Offset(leftEyeX - eyeWidthPx / 2f, centerY - eyeHeightPx / 2f),
                    size = androidx.compose.ui.geometry.Size(eyeWidthPx, eyeHeightPx),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(eyeCornerRadiusPx, eyeCornerRadiusPx)
                )
                
                // Draw right capsule eye
                drawRoundRect(
                    color = color,
                    topLeft = Offset(rightEyeX - eyeWidthPx / 2f, centerY - eyeHeightPx / 2f),
                    size = androidx.compose.ui.geometry.Size(eyeWidthPx, eyeHeightPx),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(eyeCornerRadiusPx, eyeCornerRadiusPx)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        // Greeting text
        Text(
            text = messageText,
            color = color,
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(4.dp))

        // Optional small subtitle (future use)
        Text(
            text = "",
            color = color.copy(alpha = 0.6f),
            fontSize = 12.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
fun LockIcon(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val centerX = center.x
        val centerY = center.y
        
        // Base: rounded rectangle at the bottom half
        val baseWidth = w * 0.7f
        val baseHeight = h * 0.45f
        val baseLeft = (w - baseWidth) / 2f
        val baseTop = h * 0.48f
        val baseCorner = 2.dp.toPx()
        
        drawRoundRect(
            color = color,
            topLeft = Offset(baseLeft, baseTop),
            size = androidx.compose.ui.geometry.Size(baseWidth, baseHeight),
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(baseCorner, baseCorner)
        )
        
        // Shackle: arched line at the top half
        val shackleRadius = w * 0.22f
        val shackleStroke = 1.8f.dp.toPx()
        val path = Path().apply {
            moveTo(centerX - shackleRadius, baseTop)
            lineTo(centerX - shackleRadius, centerY - h * 0.12f)
            arcTo(
                rect = androidx.compose.ui.geometry.Rect(
                    left = centerX - shackleRadius,
                    top = centerY - h * 0.12f - shackleRadius,
                    right = centerX + shackleRadius,
                    bottom = centerY - h * 0.12f + shackleRadius
                ),
                startAngleDegrees = 180f,
                sweepAngleDegrees = 180f,
                forceMoveTo = false
            )
            lineTo(centerX + shackleRadius, baseTop)
        }
        drawPath(
            path = path,
            color = color,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = shackleStroke,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
        )
    }
}

@Composable
fun RightSegmentContextView(
    isLocked: Boolean,
    timeFormat: String,
    showSeconds: Boolean,
    color: Color,
    textSizeOffset: Float
) {
    var timeText by remember { mutableStateOf("") }
    
    LaunchedEffect(timeFormat, showSeconds) {
        while (true) {
            timeText = formatSystemTime(timeFormat, showSeconds)
            delay(if (showSeconds) 500L else 5000L)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        androidx.compose.animation.Crossfade(
            targetState = isLocked,
            label = "LockTimeTransition"
        ) { locked ->
            if (locked) {
                LockIcon(
                    color = color,
                    modifier = Modifier.size(16.dp)
                )
            } else {
                Text(
                    text = timeText,
                    color = color,
                    fontSize = (13f + textSizeOffset).sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}



