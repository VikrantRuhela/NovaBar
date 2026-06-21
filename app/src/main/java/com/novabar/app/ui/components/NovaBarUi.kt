package com.novabar.app.ui.components

import android.graphics.Bitmap
import android.graphics.drawable.Drawable
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.novabar.app.domain.*
import com.novabar.app.presentation.OverlayViewModel
import com.novabar.app.presentation.ViewModelFactory
import com.novabar.app.ui.theme.*
import kotlinx.coroutines.delay
import kotlin.math.roundToInt
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

    val activeState by viewModel.activeState.collectAsState()
    val settings by viewModel.settingsFlow.collectAsState()
    val isDarkBg by viewModel.isDarkBackground.collectAsState()
    val isExpanded by OverlayStateManager.isExpanded.collectAsState()
    val activeList by OverlayStateManager.activeActivities.collectAsState()

    var userInteractionTick by remember { mutableStateOf(0L) }

    val targetState = when (val s = activeState) {
        is OverlayState.Media -> {
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

    val textSizeOffset = getTextSizeOffset(settings.textSize)

    // Palette Color Extraction glass tint
    val extractedColor = remember(activeState) {
        val mediaState = (activeState as? OverlayState.Media)?.data
        val art = mediaState?.albumArt
        if (art != null) {
            try {
                val palette = androidx.palette.graphics.Palette.from(art).generate()
                val colorVal = palette.getMutedColor(palette.getDominantColor(0))
                if (colorVal != 0) Color(colorVal) else null
            } catch (e: Exception) {
                null
            }
        } else {
            null
        }
    }

    LaunchedEffect(isExpanded, activeState, userInteractionTick, settings.autoCollapseTimeout) {
        if (isExpanded && activeState !is OverlayState.Idle) {
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
    val backgroundColor by animateColorAsState(
        targetValue = if (extractedColor != null) {
            val tintOpacity = 0.12f
            Color(
                red = (targetColor.red * (1f - tintOpacity) + extractedColor.red * tintOpacity),
                green = (targetColor.green * (1f - tintOpacity) + extractedColor.green * tintOpacity),
                blue = (targetColor.blue * (1f - tintOpacity) + extractedColor.blue * tintOpacity),
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

    val showOverlay = activeState !is OverlayState.Idle || settings.alwaysOnBar

    // Dimensions
    val borderThickness = settings.barBorderThickness.dp
    val minWidth = (120 * settings.barWidthScale).dp

    val widthModifier = when (targetState) {
        NowBarState.MINIMIZED -> Modifier.width((115 * settings.barWidthScale).dp)
        NowBarState.COMPACT -> Modifier.width((185 * settings.barWidthScale).dp)
        NowBarState.EXPANDED -> Modifier.width(290.dp)
    }

    val heightModifier = when (targetState) {
        NowBarState.MINIMIZED -> Modifier.height((38 + settings.barHeightPadding).dp.coerceAtLeast(24.dp))
        NowBarState.COMPACT -> Modifier.height((44 + settings.barHeightPadding).dp.coerceAtLeast(30.dp))
        NowBarState.EXPANDED -> Modifier.height(205.dp)
    }

    AnimatedVisibility(
        visible = showOverlay,
        enter = fadeIn() + expandVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy)),
        exit = fadeOut() + shrinkVertically(animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy))
    ) {
        Box(
            modifier = Modifier
                .then(widthModifier)
                .then(heightModifier)
                .animateContentSize(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioNoBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    )
                )
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
                                if (activeState !is OverlayState.Idle) {
                                    OverlayStateManager.expand()
                                    userInteractionTick = System.currentTimeMillis()
                                }
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Crossfade(
                targetState = activeState,
                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow),
                label = "ActiveStateCrossfade"
            ) { state ->
                when (state) {
                    is OverlayState.PhoneCall -> PhoneCallView(state.data, targetState, foregroundColor, textSizeOffset) {
                        userInteractionTick = System.currentTimeMillis()
                    }
                    is OverlayState.Charging -> ChargingPill(state.data, targetState, foregroundColor, textSizeOffset)
                    is OverlayState.Notification -> NotificationView(state.data, targetState, viewModel, foregroundColor, textSizeOffset)
                    is OverlayState.Timer -> TimerView(state.data, targetState, foregroundColor, settings.showSeconds, textSizeOffset) {
                        userInteractionTick = System.currentTimeMillis()
                    }
                    is OverlayState.Stopwatch -> StopwatchView(state.data, targetState, foregroundColor, settings.showSeconds, textSizeOffset) {
                        userInteractionTick = System.currentTimeMillis()
                    }
                    is OverlayState.Navigation -> NavigationView(state.data, targetState, foregroundColor, settings.timeFormat, textSizeOffset)
                    is OverlayState.Media -> {
                        MediaView(
                            state = state.data,
                            currentState = targetState,
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
                            onClick = { onInteraction() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336)),
                            modifier = Modifier.weight(1f).padding(horizontal = 6.dp)
                        ) {
                            Text("Decline", color = Color.White, fontSize = (11f + sizeOffset).sp)
                        }
                    } else {
                        Button(
                            onClick = { onInteraction() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336)),
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp)
                        ) {
                            Text("End Call", color = Color.White, fontSize = (11f + sizeOffset).sp)
                        }
                    }
                }
            }
        }
    }
}

// --- CHARGING PILL ---
@Composable
fun ChargingPill(
    state: ChargingState,
    currentState: NowBarState,
    color: Color,
    textSizeOffset: Float
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
                Text("⚡", color = Color(0xFFFFEB3B), fontSize = (12f + sizeOffset).sp)
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
                Text("⚡", color = Color(0xFFFFEB3B), fontSize = (14f + sizeOffset).sp)
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
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("⚡", color = Color(0xFFFFEB3B), fontSize = (22f + sizeOffset).sp)
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

// --- NOTIFICATION BANNER ---
@Composable
fun NotificationView(
    state: NotificationState,
    currentState: NowBarState,
    viewModel: OverlayViewModel,
    color: Color,
    textSizeOffset: Float
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
                    modifier = Modifier.fillMaxWidth(),
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
                    modifier = Modifier.fillMaxWidth(),
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
fun TimerView(
    state: TimerState,
    currentState: NowBarState,
    color: Color,
    showSeconds: Boolean,
    textSizeOffset: Float,
    onInteraction: () -> Unit
) {
    val sizeOffset = textSizeOffset
    var remainingMs by remember(state.remainingMs) { mutableStateOf(state.remainingMs) }

    LaunchedEffect(state.isRunning, state.remainingMs) {
        remainingMs = state.remainingMs
        if (state.isRunning) {
            val startTime = System.currentTimeMillis()
            val startVal = remainingMs
            while (remainingMs > 0) {
                delay(200L)
                val elapsed = System.currentTimeMillis() - startTime
                remainingMs = (startVal - elapsed).coerceAtLeast(0L)
            }
        }
    }

    val displayString = formatDuration(remainingMs, showSeconds)

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
                Text(
                    text = displayString,
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
                    Text(
                        text = displayString,
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
                    modifier = Modifier.fillMaxWidth(),
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
                
                Text(
                    text = displayString,
                    color = color,
                    fontSize = (26f + sizeOffset).sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = { onInteraction() },
                        colors = ButtonDefaults.buttonColors(containerColor = color.copy(alpha = 0.1f))
                    ) {
                        Text("Reset", color = color, fontSize = (11f + sizeOffset).sp)
                    }
                    Button(
                        onClick = { onInteraction() },
                        colors = ButtonDefaults.buttonColors(containerColor = color)
                    ) {
                        Text(if (state.isRunning) "Pause" else "Resume", color = if (color == Color.White) Color.Black else Color.White, fontSize = (11f + sizeOffset).sp)
                    }
                }
            }
        }
    }
}

fun formatDuration(ms: Long, showSeconds: Boolean): String {
    val totalSecs = ms / 1000
    val hours = totalSecs / 3600
    val minutes = (totalSecs % 3600) / 60
    val seconds = totalSecs % 60
    
    return if (hours > 0) {
        if (showSeconds) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", hours, minutes)
        }
    } else {
        if (showSeconds) {
            String.format("%02d:%02d", minutes, seconds)
        } else {
            String.format("%d min", minutes)
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
    onInteraction: () -> Unit
) {
    val sizeOffset = textSizeOffset
    var elapsedMs by remember(state.elapsedMs) { mutableStateOf(state.elapsedMs) }

    LaunchedEffect(state.isRunning, state.elapsedMs) {
        elapsedMs = state.elapsedMs
        if (state.isRunning) {
            val startTime = System.currentTimeMillis() - elapsedMs
            while (true) {
                delay(100L)
                elapsedMs = System.currentTimeMillis() - startTime
            }
        }
    }

    val displayString = formatStopwatch(elapsedMs, showSeconds)

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
                Text(
                    text = displayString,
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
                    Text(
                        text = displayString,
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
                    modifier = Modifier.fillMaxWidth(),
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
                
                Text(
                    text = displayString,
                    color = color,
                    fontSize = (26f + sizeOffset).sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = { onInteraction() },
                        colors = ButtonDefaults.buttonColors(containerColor = color.copy(alpha = 0.1f))
                    ) {
                        Text("Lap", color = color, fontSize = (11f + sizeOffset).sp)
                    }
                    Button(
                        onClick = { onInteraction() },
                        colors = ButtonDefaults.buttonColors(containerColor = color)
                    ) {
                        Text(if (state.isRunning) "Pause" else "Resume", color = if (color == Color.White) Color.Black else Color.White, fontSize = (11f + sizeOffset).sp)
                    }
                }
            }
        }
    }
}

fun formatStopwatch(ms: Long, showSeconds: Boolean): String {
    val totalSecs = ms / 1000
    val minutes = totalSecs / 60
    val seconds = totalSecs % 60
    val tenths = (ms / 100) % 10
    
    return if (showSeconds) {
        String.format("%02d:%02d.%d", minutes, seconds, tenths)
    } else {
        String.format("%02d min", minutes)
    }
}

// --- NAVIGATION VIEW ---
@Composable
fun NavigationView(
    state: NavigationState,
    currentState: NowBarState,
    color: Color,
    timeFormat: String,
    textSizeOffset: Float
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
                    .padding(14.dp),
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
                val delta = System.currentTimeMillis() - startTime
                animationPhase = startPhase + (delta / 1000f) * 2f * Math.PI.toFloat() * sensitivity
                delay(16L) // ~60 FPS
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
    onInteraction: () -> Unit
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
                if (state.albumArt != null) {
                    Image(
                        bitmap = state.albumArt.asImageBitmap(),
                        contentDescription = "Album Art",
                        modifier = Modifier
                            .size(20.dp)
                            .clip(RoundedCornerShape(albumArtCornerRadius.dp))
                    )
                } else {
                    FallbackArtIcon(color = color, sizeDp = 20)
                }
                
                AudioVisualizer(
                    style = visualizerStyle,
                    sensitivity = visualizerSensitivity,
                    isPlaying = state.isPlaying,
                    color = color,
                    modifier = Modifier
                        .width(24.dp)
                        .height(12.dp)
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
                if (state.albumArt != null) {
                    Image(
                        bitmap = state.albumArt.asImageBitmap(),
                        contentDescription = "Album Art",
                        modifier = Modifier
                            .size(26.dp) // Compact: 24dp - 28dp
                            .clip(RoundedCornerShape(albumArtCornerRadius.dp))
                    )
                } else {
                    FallbackArtIcon(color = color, sizeDp = 26)
                }

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

                AudioVisualizer(
                    style = visualizerStyle,
                    sensitivity = visualizerSensitivity,
                    isPlaying = state.isPlaying,
                    color = color,
                    modifier = Modifier
                        .width(22.dp)
                        .height(10.dp)
                )
            }
        }
        NowBarState.EXPANDED -> {
            var dragPosition by remember { mutableStateOf<Float?>(null) }
            var currentProgressMs by remember { mutableStateOf(state.position) }

            LaunchedEffect(state.position, state.isPlaying) {
                currentProgressMs = state.position
                if (state.isPlaying) {
                    val startTime = System.currentTimeMillis()
                    val startPosition = state.position
                    while (true) {
                        val elapsed = System.currentTimeMillis() - startTime
                        currentProgressMs = (startPosition + elapsed).coerceAtMost(state.duration)
                        delay(250L)
                    }
                }
            }

            val displayPositionMs = if (dragPosition != null) {
                (dragPosition!! * state.duration).toLong()
            } else {
                currentProgressMs
            }

            val sliderProgress = if (state.duration > 0) {
                (displayPositionMs.toFloat() / state.duration).coerceIn(0f, 1f)
            } else {
                0f
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (state.albumArt != null) {
                        Image(
                            bitmap = state.albumArt.asImageBitmap(),
                            contentDescription = "Album Art",
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(albumArtCornerRadius.dp))
                        )
                    } else {
                        FallbackArtIcon(color = color, sizeDp = 40)
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = state.title,
                            color = color,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = state.artist.ifEmpty { "Unknown Artist" },
                            color = color.copy(alpha = 0.7f),
                            fontSize = 11.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    val appIconBitmap = remember(state.appIcon) {
                        drawableToBitmap(state.appIcon)?.asImageBitmap()
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

                Column(modifier = Modifier.fillMaxWidth()) {
                    Slider(
                        value = sliderProgress,
                        onValueChange = {
                            onInteraction()
                            dragPosition = it
                        },
                        onValueChangeFinished = {
                            val seekMs = (sliderProgress * state.duration).toLong()
                            onSeekTo(seekMs)
                            dragPosition = null
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = color,
                            activeTrackColor = color,
                            inactiveTrackColor = color.copy(alpha = 0.2f)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(18.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(formatTime(displayPositionMs), color = color.copy(alpha = 0.6f), fontSize = 10.sp)
                        Text(formatTime(state.duration), color = color.copy(alpha = 0.6f), fontSize = 10.sp)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            onInteraction()
                            com.novabar.app.services.NovaNotificationListener.toggleShuffle()
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Canvas(modifier = Modifier.size(16.dp)) {
                            val w = size.width
                            val h = size.height
                            val stroke = 2.dp.toPx()
                            
                            val path1 = Path().apply {
                                moveTo(0f, h * 0.25f)
                                lineTo(w * 0.3f, h * 0.25f)
                                lineTo(w * 0.7f, h * 0.75f)
                                lineTo(w, h * 0.75f)
                            }
                            val path2 = Path().apply {
                                moveTo(0f, h * 0.75f)
                                lineTo(w * 0.3f, h * 0.75f)
                                lineTo(w * 0.7f, h * 0.25f)
                                lineTo(w, h * 0.25f)
                            }
                            
                            drawPath(
                                path1, 
                                color = if (state.isShuffleEnabled) color else color.copy(alpha = 0.5f),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(stroke)
                            )
                            drawPath(
                                path2, 
                                color = if (state.isShuffleEnabled) color else color.copy(alpha = 0.5f),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(stroke)
                            )
                            
                            val arrowTop = Path().apply {
                                moveTo(w * 0.82f, h * 0.1f)
                                lineTo(w, h * 0.25f)
                                lineTo(w * 0.82f, h * 0.4f)
                            }
                            drawPath(
                                arrowTop,
                                color = if (state.isShuffleEnabled) color else color.copy(alpha = 0.5f),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(stroke)
                            )
                            
                            val arrowBottom = Path().apply {
                                moveTo(w * 0.82f, h * 0.6f)
                                lineTo(w, h * 0.75f)
                                lineTo(w * 0.82f, h * 0.9f)
                            }
                            drawPath(
                                arrowBottom,
                                color = if (state.isShuffleEnabled) color else color.copy(alpha = 0.5f),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(stroke)
                            )
                        }
                    }

                    IconButton(
                        onClick = {
                            onInteraction()
                            com.novabar.app.services.NovaNotificationListener.skipToPrevious()
                        },
                        modifier = Modifier.size(28.dp)
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

                    IconButton(
                        onClick = {
                            onInteraction()
                            if (state.isPlaying) {
                                com.novabar.app.services.NovaNotificationListener.pause()
                            } else {
                                com.novabar.app.services.NovaNotificationListener.play()
                            }
                        },
                        modifier = Modifier.size(34.dp)
                    ) {
                        Canvas(modifier = Modifier.size(24.dp)) {
                            if (state.isPlaying) {
                                val w = size.width * 0.14f
                                val gap = size.width * 0.18f
                                drawRect(color, Offset(size.width * 0.26f, size.height * 0.2f), Size(w, size.height * 0.6f))
                                drawRect(color, Offset(size.width * 0.26f + w + gap, size.height * 0.2f), Size(w, size.height * 0.6f))
                            } else {
                                val path = Path().apply {
                                    moveTo(size.width * 0.32f, size.height * 0.18f)
                                    lineTo(size.width * 0.82f, size.height * 0.5f)
                                    lineTo(size.width * 0.32f, size.height * 0.82f)
                                    close()
                                }
                                drawPath(path, color)
                            }
                        }
                    }

                    IconButton(
                        onClick = {
                            onInteraction()
                            com.novabar.app.services.NovaNotificationListener.skipToNext()
                        },
                        modifier = Modifier.size(28.dp)
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

                    val context = LocalContext.current
                    IconButton(
                        onClick = {
                            onInteraction()
                            com.novabar.app.services.NovaNotificationListener.showOutputSwitcher(context)
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Canvas(modifier = Modifier.size(16.dp)) {
                            val w = size.width
                            val h = size.height
                            val path = Path().apply {
                                moveTo(w * 0.35f, h * 0.35f)
                                lineTo(w * 0.15f, h * 0.35f)
                                lineTo(w * 0.15f, h * 0.65f)
                                lineTo(w * 0.35f, h * 0.65f)
                                lineTo(w * 0.65f, h * 0.85f)
                                lineTo(w * 0.65f, h * 0.15f)
                                close()
                            }
                            drawPath(path, color.copy(alpha = 0.7f))
                            drawArc(
                                color = color.copy(alpha = 0.7f),
                                startAngle = -45f,
                                sweepAngle = 90f,
                                useCenter = false,
                                topLeft = Offset(w * 0.3f, h * 0.15f),
                                size = Size(w * 0.6f, h * 0.7f),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx())
                            )
                        }
                    }
                }
            }
        }
    }
}
