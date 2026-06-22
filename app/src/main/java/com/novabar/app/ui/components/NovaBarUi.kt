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
import androidx.compose.ui.unit.TextUnit
import androidx.lifecycle.viewmodel.compose.viewModel
import com.novabar.app.domain.*
import com.novabar.app.presentation.OverlayViewModel
import com.novabar.app.presentation.ViewModelFactory
import com.novabar.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import kotlin.math.roundToInt
import kotlin.math.sin
import androidx.compose.foundation.gestures.awaitFirstDown

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
fun getTextSizeOffset(size: String): Float {
    return when (size) {
        "Small" -> -2f
        "Large" -> 2f
        "Extra Large" -> 4f
        else -> 0f
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
    textSizeOffset: Float
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
            }
        }.distinctUntilChanged()
    }.collectAsState(initial = "Idle")
    val settings by viewModel.settingsFlow.collectAsState()
    val isDarkBg by viewModel.isDarkBackground.collectAsState()
    val isExpanded by OverlayStateManager.isExpanded.collectAsState()
    val activeList by OverlayStateManager.activeActivities.collectAsState()

    LaunchedEffect(isExpanded) {
        if (isExpanded && activeStateKey == "Media") {
            val elapsed = System.currentTimeMillis() - DiagnosticsManager.expandClickTime
            Log.d("NovaBar", "MEDIA_EXPAND_ANIMATION_STARTED: elapsed=${elapsed}ms")
        }
    }

    var userInteractionTick by remember { mutableStateOf(0L) }

    val targetState = when (activeStateKey) {
        "Media" -> {
            when (settings.defaultPresentationMode) {
                "Minimized" -> if (isExpanded) NowBarState.EXPANDED else NowBarState.MINIMIZED
                "Expanded" -> NowBarState.EXPANDED
                else -> if (isExpanded) NowBarState.EXPANDED else NowBarState.COMPACT
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

    val textSizeOffset = getTextSizeOffset(settings.textSize)

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

    LaunchedEffect(isExpanded, activeStateKey, userInteractionTick, settings.autoCollapseTimeout) {
        if (isExpanded && activeStateKey != "Idle") {
            delay(settings.autoCollapseTimeout * 1000L)
            OverlayStateManager.collapse()
        }
    }

    // Dynamic color styling
    val foregroundColor by animateColorAsState(
        targetValue = if (isDarkBg) Color.White else Color.Black,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy),
        label = "ForegroundColor"
    )

    val baseBackgroundColor = if (isDarkBg) Color(0xFF1C1C1E) else Color(0xFFFFFFFF)
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

    val showOverlay = activeStateKey != "Idle" || settings.alwaysOnBar

    // Dimensions
    val borderThickness = settings.barBorderThickness.dp
    val targetWidth = when (targetState) {
        NowBarState.MINIMIZED -> (115 * settings.barWidthScale).dp
        NowBarState.COMPACT -> (185 * settings.barWidthScale).dp
        NowBarState.EXPANDED -> 290.dp
    }

    val targetHeight = when (targetState) {
        NowBarState.MINIMIZED -> (38 + settings.barHeightPadding).dp.coerceAtLeast(24.dp)
        NowBarState.COMPACT -> (44 + settings.barHeightPadding).dp.coerceAtLeast(30.dp)
        NowBarState.EXPANDED -> 205.dp
    }

    val animatedWidth by animateDpAsState(
        targetValue = targetWidth,
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
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
        animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "animatedHeight"
    )

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

    AnimatedVisibility(
        visible = showOverlay,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = rootAlignment
        ) {
            Box(
                modifier = Modifier
                    .width(animatedWidth)
                    .height(animatedHeight)
                    .onGloballyPositioned { coordinates ->
                        val rect = coordinates.boundsInWindow()
                        val androidRect = android.graphics.Rect(
                            rect.left.roundToInt(),
                            rect.top.roundToInt(),
                            rect.right.roundToInt(),
                            rect.bottom.roundToInt()
                        )
                        OverlayStateManager.updatePillBounds(androidRect)
                    }
                    .shadow(elevation = 12.dp, shape = RoundedCornerShape(settings.cornerRadius.dp))
                    .border(borderThickness, borderColor, RoundedCornerShape(settings.cornerRadius.dp))
                    .clip(RoundedCornerShape(settings.cornerRadius.dp))
                    .background(backgroundColor)
                    .pointerInput(activeList) {
                        awaitPointerEventScope {
                            while (true) {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                var dragX = 0f
                                var isSwipe = false
                                val pointerId = down.id
                                val touchSlop = viewConfiguration.touchSlop
                                
                                do {
                                    val event = awaitPointerEvent()
                                    val dragChange = event.changes.firstOrNull { it.id == pointerId }
                                    if (dragChange != null) {
                                        if (dragChange.pressed) {
                                            val diffX = dragChange.position.x - dragChange.previousPosition.x
                                            dragX += diffX
                                            if (kotlin.math.abs(dragX) > touchSlop) {
                                                isSwipe = true
                                            }
                                            if (isSwipe) {
                                                dragChange.consume()
                                            }
                                        }
                                    }
                                } while (event.changes.any { it.pressed })
                                
                                if (isSwipe) {
                                    if (dragX > 60f) {
                                        OverlayStateManager.swipeRight()
                                        userInteractionTick = System.currentTimeMillis()
                                    } else if (dragX < -60f) {
                                        OverlayStateManager.swipeLeft()
                                        userInteractionTick = System.currentTimeMillis()
                                    }
                                } else {
                                    // Tap!
                                    if (activeStateKey != "Idle") {
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
                    },
                contentAlignment = Alignment.Center
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
                        }
                        activeStateMap[key] = state
                    }
                }

                Crossfade(
                    targetState = activeStateKey,
                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
                    label = "ActiveStateCrossfade"
                ) { key ->
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
                            modifier = Modifier.requiredSize(stateTargetWidth, stateTargetHeight),
                            contentAlignment = Alignment.Center
                        ) {
                            when (state) {
                                is OverlayState.PhoneCall -> PhoneCallView(state.data, stateTargetState, foregroundColor, textSizeOffset) {
                                    userInteractionTick = System.currentTimeMillis()
                                }
                                is OverlayState.Charging -> ChargingPill(state.data, stateTargetState, foregroundColor, textSizeOffset)
                                is OverlayState.Notification -> NotificationView(state.data, stateTargetState, viewModel, foregroundColor, textSizeOffset)
                                is OverlayState.Timer -> TimerView(state.data, stateTargetState, foregroundColor, settings.showSeconds, textSizeOffset) {
                                    userInteractionTick = System.currentTimeMillis()
                                }
                                is OverlayState.Stopwatch -> StopwatchView(state.data, stateTargetState, foregroundColor, settings.showSeconds, textSizeOffset) {
                                    userInteractionTick = System.currentTimeMillis()
                                }
                                is OverlayState.Navigation -> NavigationView(state.data, stateTargetState, foregroundColor, settings.timeFormat, textSizeOffset)
                                is OverlayState.Media -> {
                                    MediaView(
                                        state = state.data,
                                        currentState = stateTargetState,
                                        color = foregroundColor,
                                        albumArtCornerRadius = settings.albumArtCornerRadius,
                                        visualizerStyle = settings.visualizerStyle,
                                        visualizerSensitivity = settings.visualizerSensitivity,
                                        progressVisibility = settings.progressVisibility,
                                        onSeekTo = { posMs ->
                                            com.novabar.app.services.NovaNotificationListener.seekTo(posMs)
                                        },
                                        onInteraction = {
                                            userInteractionTick = System.currentTimeMillis()
                                        }
                                    )
                                }
                                is OverlayState.Idle -> {
                                    if (settings.alwaysOnBar) {
                                        AlwaysOnView(settings.alwaysOnConfig, settings.timeFormat, settings.showSeconds, foregroundColor, textSizeOffset)
                                    } else {
                                        Row(modifier = Modifier.padding(horizontal = 14.dp)) {
                                            Text("Ready", color = foregroundColor, fontSize = (12f + textSizeOffset).sp, fontWeight = FontWeight.Bold)
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
    onInteraction: () -> Unit
) {
    val sizeOffset = textSizeOffset
    val context = LocalContext.current
    var isEndingCall by remember { mutableStateOf(false) }
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
    contentAlpha: Float = 1f
) {
    val sizeOffset = textSizeOffset
    val batteryPercent = state.batteryPercentage.coerceIn(0, 100)
    
    // Smooth transition when battery updates
    val animatedPercent by animateFloatAsState(
        targetValue = batteryPercent.toFloat(),
        animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing),
        label = "batteryPercent"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "chargingWave")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
                val width = size.width
                val height = size.height

                // Calculate fill width based on percentage
                val fillFraction = animatedPercent / 100f
                val fillWidth = width * fillFraction

                // Premium green charging color
                val fillThemeColor = Color(0xFF34C759)

                if (fillWidth > 0f) {
                    val steps = 30

                    // Layer 1: alpha = 0.18, amplitude = 3dp
                    val amplitude1 = 3.dp.toPx()
                    val frequency1 = (2f * Math.PI.toFloat()) / height
                    val fillPath1 = Path()
                    fillPath1.moveTo(0f, 0f)
                    val xAtZero1 = fillWidth + kotlin.math.sin(0.0 - wavePhase.toDouble()).toFloat() * amplitude1
                    fillPath1.lineTo(xAtZero1.coerceIn(0f, width), 0f)
                    for (i in 0..steps) {
                        val y = (i.toFloat() / steps) * height
                        val x = fillWidth + kotlin.math.sin(y * frequency1 - wavePhase.toDouble()).toFloat() * amplitude1
                        fillPath1.lineTo(x.coerceIn(0f, width), y)
                    }
                    fillPath1.lineTo(0f, height)
                    fillPath1.close()
                    drawPath(path = fillPath1, color = fillThemeColor.copy(alpha = 0.18f))

                    // Layer 2: alpha = 0.10, amplitude = 2dp (with phase offset)
                    val amplitude2 = 2.dp.toPx()
                    val frequency2 = (2f * Math.PI.toFloat()) / (height * 0.8f)
                    val phaseOffset2 = 2f
                    val fillPath2 = Path()
                    fillPath2.moveTo(0f, 0f)
                    val xAtZero2 = fillWidth + kotlin.math.sin(0.0 - (wavePhase + phaseOffset2).toDouble()).toFloat() * amplitude2
                    fillPath2.lineTo(xAtZero2.coerceIn(0f, width), 0f)
                    for (i in 0..steps) {
                        val y = (i.toFloat() / steps) * height
                        val x = fillWidth + kotlin.math.sin(y * frequency2 - (wavePhase + phaseOffset2).toDouble()).toFloat() * amplitude2
                        fillPath2.lineTo(x.coerceIn(0f, width), y)
                    }
                    fillPath2.lineTo(0f, height)
                    fillPath2.close()
                    drawPath(path = fillPath2, color = fillThemeColor.copy(alpha = 0.10f))

                    // Layer 3: alpha = 0.05, amplitude = 1dp (with phase/depth offset)
                    val amplitude3 = 1.dp.toPx()
                    val frequency3 = (2f * Math.PI.toFloat()) / (height * 1.2f)
                    val phaseOffset3 = 4f
                    val fillPath3 = Path()
                    fillPath3.moveTo(0f, 0f)
                    val xAtZero3 = fillWidth + kotlin.math.sin(0.0 - (wavePhase + phaseOffset3).toDouble()).toFloat() * amplitude3
                    fillPath3.lineTo(xAtZero3.coerceIn(0f, width), 0f)
                    for (i in 0..steps) {
                        val y = (i.toFloat() / steps) * height
                        val x = fillWidth + kotlin.math.sin(y * frequency3 - (wavePhase + phaseOffset3).toDouble()).toFloat() * amplitude3
                        fillPath3.lineTo(x.coerceIn(0f, width), y)
                    }
                    fillPath3.lineTo(0f, height)
                    fillPath3.close()
                    drawPath(path = fillPath3, color = fillThemeColor.copy(alpha = 0.05f))
                }
            },
        contentAlignment = Alignment.Center
    ) {
        when (currentState) {
            NowBarState.MINIMIZED -> {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
                ) {
                    BoltIcon(color = Color(0xFFFFEB3B), modifier = Modifier.size(12.dp))
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
                    BoltIcon(color = Color(0xFFFFEB3B), modifier = Modifier.size(14.dp))
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
                        BoltIcon(color = Color(0xFFFFEB3B), modifier = Modifier.size(24.dp))
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
    controlsOffsetY: androidx.compose.ui.unit.Dp = 0.dp
) {
    val sizeOffset = textSizeOffset
    var offsetX by remember { mutableStateOf(0f) }

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
    durationMs: Long,
    initialRemainingMs: Long,
    isRunning: Boolean,
    showSeconds: Boolean,
    color: Color,
    fontSize: TextUnit,
    fontWeight: FontWeight,
    modifier: Modifier = Modifier
) {
    var remainingMs by remember(initialRemainingMs, isRunning) { mutableStateOf(initialRemainingMs) }

    LaunchedEffect(isRunning, initialRemainingMs) {
        remainingMs = initialRemainingMs
        if (isRunning) {
            val startTime = System.currentTimeMillis()
            val startVal = remainingMs
            while (remainingMs > 0) {
                delay(10L)
                val elapsed = System.currentTimeMillis() - startTime
                remainingMs = (startVal - elapsed).coerceAtLeast(0L)
            }
        }
    }

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
    onInteraction: () -> Unit
) {
    val sizeOffset = textSizeOffset

    when (currentState) {
        NowBarState.MINIMIZED -> {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
            ) {
                Text("⏱", color = color, fontSize = (11f + sizeOffset).sp)
                TimerDisplayText(
                    durationMs = state.durationMs,
                    initialRemainingMs = state.remainingMs,
                    isRunning = state.isRunning,
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
                Text("⏱", color = color, fontSize = (13f + sizeOffset).sp)
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
                        durationMs = state.durationMs,
                        initialRemainingMs = state.remainingMs,
                        isRunning = state.isRunning,
                        showSeconds = showSeconds,
                        color = color.copy(alpha = 0.8f),
                        fontSize = (11f + sizeOffset).sp,
                        fontWeight = FontWeight.Medium
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
                    Text("⏱", color = color, fontSize = (22f + sizeOffset).sp)
                    Column {
                        Text(
                            text = if (state.label.isNotEmpty()) state.label else "Timer",
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
                    durationMs = state.durationMs,
                    initialRemainingMs = state.remainingMs,
                    isRunning = state.isRunning,
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

// --- STOPWATCH VIEW ---
@Composable
fun StopwatchDisplayText(
    initialElapsedMs: Long,
    isRunning: Boolean,
    showSeconds: Boolean,
    color: Color,
    fontSize: TextUnit,
    fontWeight: FontWeight,
    modifier: Modifier = Modifier
) {
    var elapsedMs by remember(initialElapsedMs, isRunning) { mutableStateOf(initialElapsedMs) }

    LaunchedEffect(isRunning, initialElapsedMs) {
        elapsedMs = initialElapsedMs
        if (isRunning) {
            val startTime = System.currentTimeMillis() - elapsedMs
            while (true) {
                delay(33L)
                elapsedMs = System.currentTimeMillis() - startTime
            }
        }
    }

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
    onInteraction: () -> Unit
) {
    val sizeOffset = textSizeOffset

    when (currentState) {
        NowBarState.MINIMIZED -> {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
            ) {
                Text("⏲", color = color, fontSize = (11f + sizeOffset).sp)
                StopwatchDisplayText(
                    initialElapsedMs = state.elapsedMs,
                    isRunning = state.isRunning,
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
                Text("⏲", color = color, fontSize = (13f + sizeOffset).sp)
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
                        initialElapsedMs = state.elapsedMs,
                        isRunning = state.isRunning,
                        showSeconds = showSeconds,
                        color = color.copy(alpha = 0.8f),
                        fontSize = (11f + sizeOffset).sp,
                        fontWeight = FontWeight.Medium
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
                    Text("⏲", color = color, fontSize = (22f + sizeOffset).sp)
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
                    initialElapsedMs = state.elapsedMs,
                    isRunning = state.isRunning,
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
                    val lapInteractionSource = remember { MutableInteractionSource() }
                    val isLapPressed by lapInteractionSource.collectIsPressedAsState()
                    val lapScale by animateFloatAsState(if (isLapPressed) 0.92f else 1f, label = "LapScale")

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
                        interactionSource = lapInteractionSource,
                        modifier = Modifier
                            .graphicsLayer {
                                scaleX = lapScale
                                scaleY = lapScale
                            }
                            .alpha(if (state.hasLap) 1f else 0.5f)
                    ) {
                        Text("Lap", color = color, fontSize = (11f + sizeOffset).sp)
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
    
    return if (hours > 0) {
        String.format("%02d:%02d:%02d.%02d", hours, minutes, seconds, centiseconds)
    } else {
        String.format("%02d:%02d.%02d", minutes, seconds, centiseconds)
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
    contentAlpha: Float = 1f
) {
    val sizeOffset = textSizeOffset
    val etaFormatted = formatEta(state.eta, timeFormat)

    when (currentState) {
        NowBarState.MINIMIZED -> {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
            ) {
                Text("↱", color = Color(0xFF2196F3), fontSize = (12f + sizeOffset).sp, fontWeight = FontWeight.Bold)
                Text(
                    text = state.distanceRemaining.ifEmpty { "Nav" },
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
                Text("↱", color = Color(0xFF2196F3), fontSize = (14f + sizeOffset).sp, fontWeight = FontWeight.Bold)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.maneuverInstruction.ifEmpty { "Navigating..." },
                        color = color,
                        fontSize = (12f + sizeOffset).sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (state.distanceRemaining.isNotEmpty()) {
                        Text(
                            text = state.distanceRemaining,
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
                    Text("↱", color = Color(0xFF2196F3), fontSize = (22f + sizeOffset).sp, fontWeight = FontWeight.Bold)
                    Column {
                        Text(
                            text = state.maneuverInstruction.ifEmpty { "Navigation" },
                            color = color,
                            fontSize = (15f + sizeOffset).sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Google Maps",
                            color = color.copy(alpha = 0.5f),
                            fontSize = (10f + sizeOffset).sp
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Distance remaining: ${state.distanceRemaining}", color = color.copy(alpha = 0.8f), fontSize = (12f + sizeOffset).sp)
                    if (etaFormatted.isNotEmpty()) {
                        Text("Estimated arrival (ETA): $etaFormatted", color = color.copy(alpha = 0.6f), fontSize = (11f + sizeOffset).sp)
                    }
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
                    val barHeight = (height * amplitude).coerceIn(4.dp.toPx(), height)
                    
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
                    val barHeight = (height * amplitude).coerceIn(3.dp.toPx(), height)
                    
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
    controlsOffsetY: androidx.compose.ui.unit.Dp = 0.dp
) {
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
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (progressVisibility && state.duration > 0) {
                        Spacer(modifier = Modifier.height(2.dp))
                        LinearProgressIndicator(
                            progress = state.progress,
                            color = color,
                            trackColor = color.copy(alpha = 0.2f),
                            modifier = Modifier
                                .fillMaxWidth(0.9f)
                                .height(2.dp)
                                .clip(CircleShape)
                        )
                    }
                }

                MediaVisualizerSection(
                    style = visualizerStyle,
                    sensitivity = visualizerSensitivity,
                    isPlaying = state.isPlaying,
                    color = color,
                    widthDp = 22,
                    heightDp = 10
                )
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
                    controlsOffsetY = controlsOffsetY
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
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = title,
            color = color,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = artist.ifEmpty { "Unknown Artist" },
            color = color.copy(alpha = 0.7f),
            fontSize = 11.sp,
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
fun PlaybackSeekBar(
    playbackPositionStateFlow: kotlinx.coroutines.flow.StateFlow<Long>,
    duration: Long,
    color: Color,
    onSeekTo: (Long) -> Unit,
    onInteraction: () -> Unit,
    controlsAlpha: Float = 1f,
    controlsOffsetY: androidx.compose.ui.unit.Dp = 0.dp
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
            fontSize = 10.sp
        )
        Slider(
            value = sliderProgress,
            onValueChange = {
                onInteraction()
                dragPosition = it
            },
            onValueChangeFinished = {
                val seekMs = (sliderProgress * duration).toLong()
                onSeekTo(seekMs)
                dragPosition = null
            },
            colors = SliderDefaults.colors(
                thumbColor = color,
                activeTrackColor = color,
                inactiveTrackColor = color.copy(alpha = 0.2f)
            ),
            modifier = Modifier
                .weight(1f)
                .height(18.dp)
        )
        Text(
            text = formatTime(duration),
            color = color.copy(alpha = 0.6f),
            fontSize = 10.sp
        )
    }
}
