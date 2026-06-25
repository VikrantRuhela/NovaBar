package com.novabar.app

import android.accessibilityservice.AccessibilityServiceInfo
import android.util.Log
import com.novabar.app.data.OverlayEngine
import com.novabar.app.domain.DiagnosticsManager
import androidx.compose.ui.text.style.TextOverflow
import android.content.*
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.novabar.app.data.NovaSettings
import com.novabar.app.presentation.SettingsViewModel
import com.novabar.app.presentation.ViewModelFactory
import com.novabar.app.services.NovaAccessibilityService
import com.novabar.app.services.NovaNotificationListener
import com.novabar.app.services.OverlayService
import com.novabar.app.ui.theme.NovaBarTheme
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.novabar.app.utils.DeveloperLogger

class SettingsActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.novabar.app.utils.CutoutManager.detectCutout(this)
        window.decorView.setOnApplyWindowInsetsListener { _, insets ->
            com.novabar.app.utils.CutoutManager.detectCutout(this, insets)
            insets
        }
        setContent {
            NovaBarTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(
                            title = { Text("Nova Bar Settings", fontWeight = FontWeight.Bold) },
                            colors = TopAppBarDefaults.topAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        )
                    }
                ) { paddingValues ->
                    SettingsScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues)
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val viewModel: SettingsViewModel = viewModel(
        factory = ViewModelFactory(context)
    )
    val settings by viewModel.settingsFlow.collectAsState()


    val hasOverlayPermission = remember { mutableStateOf(false) }
    val hasNotificationPermission = remember { mutableStateOf(false) }
    val hasAccessibilityPermission = remember { mutableStateOf(false) }
    val hasPhonePermission = remember { mutableStateOf(false) }
    var permissionsExpanded by remember { mutableStateOf(false) }
    var showDiagnosticsDialog by remember { mutableStateOf(false) }

    val requestPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasPhonePermission.value = isGranted
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasOverlayPermission.value = Settings.canDrawOverlays(context)
                hasNotificationPermission.value = isNotificationServiceEnabled(context)
                hasAccessibilityPermission.value = isAccessibilityServiceEnabled(context)
                hasPhonePermission.value = androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.ANSWER_PHONE_CALLS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                
                com.novabar.app.utils.CutoutManager.detectCutout(context)

                // If overlay is enabled and permissions are granted, start/update service
                if (settings.isEnabled && hasOverlayPermission.value) {
                    val serviceIntent = Intent(context, OverlayService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent)
                    } else {
                        context.startService(serviceIntent)
                    }
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- 1. PERMISSIONS SYSTEM ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val grantedCount = (if (hasAccessibilityPermission.value) 1 else 0) +
                                   (if (hasNotificationPermission.value) 1 else 0) +
                                   (if (hasOverlayPermission.value) 1 else 0) +
                                   (if (hasPhonePermission.value) 1 else 0)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { permissionsExpanded = !permissionsExpanded },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Permissions ($grantedCount/4)",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if (hasAccessibilityPermission.value) Color(0xFF4CAF50) else Color(0xFFF44336)))
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if (hasNotificationPermission.value) Color(0xFF4CAF50) else Color(0xFFF44336)))
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if (hasOverlayPermission.value) Color(0xFF4CAF50) else Color(0xFFF44336)))
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(if (hasPhonePermission.value) Color(0xFF4CAF50) else Color(0xFFF44336)))
                    }
                    Text(
                        text = if (permissionsExpanded) "▲" else "▼",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (permissionsExpanded) {
                    Divider()
                    
                    PermissionCard(
                        title = "Display Overlay Permission",
                        description = "Required to show the floating capsule bar over other apps.",
                        isGranted = hasOverlayPermission.value,
                        onRequest = {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        }
                    )

                    PermissionCard(
                        title = "Notification & Media Listener Access",
                        description = "Allows reading playing media sessions and incoming clock timers or navigation.",
                        isGranted = hasNotificationPermission.value,
                        onRequest = {
                            val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                            context.startActivity(intent)
                        }
                    )

                    PermissionCard(
                        title = "Accessibility Color Adapter Service",
                        description = "Enables content-aware status bar luminance calculations under the pill.",
                        isGranted = hasAccessibilityPermission.value,
                        onRequest = {
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            context.startActivity(intent)
                        }
                    )

                    PermissionCard(
                        title = "Phone Call Control Permission",
                        description = "Required to answer/end system phone calls using TelecomManager directly.",
                        isGranted = hasPhonePermission.value,
                        onRequest = {
                            requestPermissionLauncher.launch(android.Manifest.permission.ANSWER_PHONE_CALLS)
                        }
                    )
                }
            }
        }

        // --- 2. MAIN SWITCH ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(settings.isEnabled) {
                        coroutineScope {
                            awaitPointerEventScope {
                                while (true) {
                                    val down = awaitFirstDown()
                                    var isLongPress = false
                                    val longPressJob = this@coroutineScope.launch {
                                        delay(900)
                                        isLongPress = true
                                        showDiagnosticsDialog = true
                                    }
                                    val up = waitForUpOrCancellation()
                                    longPressJob.cancel()
                                    if (up != null && !isLongPress) {
                                        val checked = !settings.isEnabled
                                        viewModel.setEnabled(checked)
                                        val intent = Intent(context, OverlayService::class.java)
                                        if (checked) {
                                            if (hasOverlayPermission.value) {
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                                    context.startForegroundService(intent)
                                                } else {
                                                    context.startService(intent)
                                                }
                                            } else {
                                                Toast.makeText(context, "Please grant Overlay Permission first", Toast.LENGTH_SHORT).show()
                                            }
                                        } else {
                                            context.stopService(intent)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Enable Nova Bar", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("Enable/disable floating Nova Bar overlay", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                }
                Switch(
                    checked = settings.isEnabled,
                    onCheckedChange = null
                )
            }
        }

        Divider()

        // --- 3. CUSTOMIZATION SLIDERS ---
        Text("Visual Customizations", fontSize = 18.sp, fontWeight = FontWeight.Bold)

        ToggleSetting(
            title = "Camera Cutout Layout",
            description = "Split Nova Bar around centered punch-hole cameras.",
            checked = settings.cameraCutoutMode,
            onCheckedChange = { viewModel.setCameraCutoutMode(it) }
        )

        ToggleSetting(
            title = "Follow Status Bar Visibility",
            description = "Hide Nova Bar when the status bar is hidden (e.g. fullscreen apps).",
            checked = settings.followStatusBarVisibility,
            onCheckedChange = { viewModel.setFollowStatusBarVisibility(it) }
        )
        if (settings.cameraCutoutMode) {
            Spacer(modifier = Modifier.height(8.dp))
            SliderSetting(
                title = "Camera Cutout Size: ${String.format(java.util.Locale.US, "%.1fx", settings.cameraCutoutGapScale)}",
                value = settings.cameraCutoutGapScale,
                valueRange = 0.7f..2.0f,
                onValueChange = { viewModel.setCameraCutoutGapScale(it) }
            )
            Spacer(modifier = Modifier.height(8.dp))
            SliderSetting(
                title = "Left Segment Width: ${settings.leftSegmentWidthDp}dp",
                value = settings.leftSegmentWidthDp.toFloat(),
                valueRange = 60f..240f,
                onValueChange = { viewModel.setLeftSegmentWidthDp(it.toInt()) }
            )
            Spacer(modifier = Modifier.height(8.dp))
            SliderSetting(
                title = "Right Segment Width: ${settings.rightSegmentWidthDp}dp",
                value = settings.rightSegmentWidthDp.toFloat(),
                valueRange = 60f..240f,
                onValueChange = { viewModel.setRightSegmentWidthDp(it.toInt()) }
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    viewModel.setLeftSegmentWidthDp(120)
                    viewModel.setRightSegmentWidthDp(120)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Reset Segment Widths")
            }
        }

        SliderSetting(
            title = "Vertical Position Offset Y: ${settings.offsetY}dp",
            value = settings.offsetY.toFloat(),
            valueRange = -50f..400f,
            onValueChange = { viewModel.setOffsetY(it.toInt()) }
        )

        SliderSetting(
            title = "Horizontal Position Offset X: ${settings.offsetX}dp",
            value = settings.offsetX.toFloat(),
            valueRange = -300f..300f,
            onValueChange = { viewModel.setOffsetX(it.toInt()) }
        )

        Text("Pill Horizontal Alignment", fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Left", "Center", "Right").forEach { gravity ->
                val selected = settings.barGravity == gravity
                Button(
                    onClick = { viewModel.setBarGravity(gravity) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(gravity)
                }
            }
        }

        SliderSetting(
            title = "Width Scale multiplier: ${String.format("%.2fx", settings.barWidthScale)}",
            value = settings.barWidthScale.coerceIn(if (settings.cameraCutoutMode) 0.01f..1.5f else 0.5f..1.5f),
            valueRange = if (settings.cameraCutoutMode) 0.01f..1.5f else 0.5f..1.5f,
            onValueChange = { viewModel.setBarWidthScale(it) }
        )

        SliderSetting(
            title = "Height Extra padding: ${settings.barHeightPadding}dp",
            value = settings.barHeightPadding.toFloat(),
            valueRange = -36f..50f,
            onValueChange = { viewModel.setBarHeightPadding(it.toInt()) }
        )

        SliderSetting(
            title = "Border thickness: ${settings.barBorderThickness}dp",
            value = settings.barBorderThickness.toFloat(),
            valueRange = 0f..5f,
            onValueChange = { viewModel.setBarBorderThickness(it.toInt()) }
        )

        SliderSetting(
            title = "Corner Radius: ${settings.cornerRadius}dp",
            value = settings.cornerRadius.toFloat(),
            valueRange = 12f..36f,
            onValueChange = { viewModel.setCornerRadius(it.toInt()) }
        )

        SliderSetting(
            title = "Transparency: ${(settings.opacity * 100).toInt()}%",
            value = settings.opacity,
            valueRange = 0.2f..1.0f,
            onValueChange = { viewModel.setOpacity(it) }
        )

        SliderSetting(
            title = "Animation Speed: ${String.format("%.1fx", settings.animationSpeedMultiplier)}",
            value = settings.animationSpeedMultiplier,
            valueRange = 0.5f..2.0f,
            onValueChange = { viewModel.setAnimationSpeed(it) }
        )

        Divider()

        // --- Now Bar State & Visualizer Settings ---
        Text("Nova Bar State & Visualizer Settings", fontSize = 18.sp, fontWeight = FontWeight.Bold)

        Text("Default Presentation Mode", fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Minimized", "Compact").forEach { mode ->
                val selected = settings.defaultPresentationMode == mode
                Button(
                    onClick = { viewModel.setDefaultPresentationMode(mode) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(mode)
                }
            }
        }

        Text("Visualizer Style", fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Waveform", "Pulse", "Minimal").forEach { style ->
                val selected = settings.visualizerStyle == style
                Button(
                    onClick = { viewModel.setVisualizerStyle(style) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(style)
                }
            }
        }

        SliderSetting(
            title = "Visualizer Sensitivity: ${String.format("%.1fx", settings.visualizerSensitivity)}",
            value = settings.visualizerSensitivity,
            valueRange = 0.5f..3.0f,
            onValueChange = { viewModel.setVisualizerSensitivity(it) }
        )

        SliderSetting(
            title = "Album Art Corner Radius: ${settings.albumArtCornerRadius}dp",
            value = settings.albumArtCornerRadius.toFloat(),
            valueRange = 0f..24f,
            onValueChange = { viewModel.setAlbumArtCornerRadius(it.toInt()) }
        )

        SliderSetting(
            title = "Auto-Collapse Timeout: ${settings.autoCollapseTimeout}s",
            value = settings.autoCollapseTimeout.toFloat(),
            valueRange = 3f..20f,
            onValueChange = { viewModel.setAutoCollapseTimeout(it.toInt()) }
        )

        ToggleSetting(
            title = "Show Playback Progress Line",
            description = "Show linear progress bar below title in Compact mode.",
            checked = settings.progressVisibility,
            onCheckedChange = { viewModel.setProgressVisibility(it) }
        )

        Divider()

        // --- 4. FEATURE TOGGLES ---
        Text("Overlay Modules", fontSize = 18.sp, fontWeight = FontWeight.Bold)

        ToggleSetting(
            title = "Media Playback Controls",
            description = "Track album artwork, details, and show expanded remote buttons.",
            checked = settings.mediaControlsEnabled,
            onCheckedChange = { viewModel.setMediaEnabled(it) }
        )

        ToggleSetting(
            title = "Timer Pill",
            description = "Monitor ongoing clocks and active timers.",
            checked = settings.timerEnabled,
            onCheckedChange = { viewModel.setTimerEnabled(it) }
        )

        ToggleSetting(
            title = "Stopwatch Pill",
            description = "Track elapsed stopwatch values.",
            checked = settings.stopwatchEnabled,
            onCheckedChange = { viewModel.setStopwatchEnabled(it) }
        )

        ToggleSetting(
            title = "Maps Navigation Maneuvers",
            description = "Expose direction guide from Google Maps/Waze.",
            checked = settings.navigationEnabled,
            onCheckedChange = { viewModel.setNavigationEnabled(it) }
        )

        ToggleSetting(
            title = "Charging Alert Card",
            description = "Display stats when battery is plugged into power.",
            checked = settings.chargingEnabled,
            onCheckedChange = { viewModel.setChargingEnabled(it) }
        )

        ToggleSetting(
            title = "Heads-up Alerts & Notifications",
            description = "Show incoming notification alerts in a rounded banner.",
            checked = settings.notificationsEnabled,
            onCheckedChange = { viewModel.setNotificationsEnabled(it) }
        )

        ToggleSetting(
            title = "Dynamic Luminance Color Adaptation",
            description = "Automatically switches text/icon styles based on brightness behind overlay.",
            checked = settings.colorAdaptationEnabled,
            onCheckedChange = { viewModel.setColorAdaptationEnabled(it) }
        )

        ToggleSetting(
            title = "Show Clock Pill when Idle",
            description = "Display a minimal clock/status capsule when no other modes are active.",
            checked = settings.showWhenIdle,
            onCheckedChange = { viewModel.setShowWhenIdle(it) }
        )

        Divider()

        // --- 5. SYSTEM SETTINGS ---
        Text("System Settings", fontSize = 18.sp, fontWeight = FontWeight.Bold)

        Text("Text Size", fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf("Small", "Default", "Large", "Extra Large").forEach { size ->
                val selected = settings.textSize == size
                Button(
                    onClick = { viewModel.setTextSize(size) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(size, fontSize = 10.sp, maxLines = 1)
                }
            }
        }

        Text("Overlay Position", fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("Below Status Bar", "Overlay Status Bar").forEach { pos ->
                val selected = settings.overlayPosition == pos
                Button(
                    onClick = { viewModel.setOverlayPosition(pos) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(pos, fontSize = 11.sp, maxLines = 1)
                }
            }
        }

        Text("Overlay Engine", fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(OverlayEngine.APPLICATION, OverlayEngine.ACCESSIBILITY).forEach { engine ->
                val selected = settings.overlayEngine == engine
                val label = when (engine) {
                    OverlayEngine.APPLICATION -> "Application Overlay"
                    OverlayEngine.ACCESSIBILITY -> "Accessibility Overlay"
                }
                Button(
                    onClick = { viewModel.setOverlayEngine(engine) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(label, fontSize = 11.sp, maxLines = 1)
                }
            }
        }

        Text("Time Format", fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("System Default", "12 Hour", "24 Hour").forEach { fmt ->
                val selected = settings.timeFormat == fmt
                Button(
                    onClick = { viewModel.setTimeFormat(fmt) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(fmt, fontSize = 10.sp, maxLines = 1)
                }
            }
        }

        ToggleSetting(
            title = "Show Seconds",
            description = "Display seconds in time readouts across the bar and Always On view.",
            checked = settings.showSeconds,
            onCheckedChange = { viewModel.setShowSeconds(it) }
        )

        ToggleSetting(
            title = "Show on Lockscreen",
            description = "Display the floating capsule bar overlay on the lockscreen.",
            checked = settings.showOnLockscreen,
            onCheckedChange = { viewModel.setShowOnLockscreen(it) }
        )

        ToggleSetting(
            title = "Always On Bar",
            description = "Show a minimal clock/battery capsule when no activities are active.",
            checked = settings.alwaysOnBar,
            onCheckedChange = { viewModel.setAlwaysOnBar(it) }
        )

        if (settings.alwaysOnBar) {
            Text("Always On Layout", fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("Time", "Time • Battery", "Time • Date • Battery").forEach { cfg ->
                    val selected = settings.alwaysOnConfig == cfg
                    Button(
                        onClick = { viewModel.setAlwaysOnConfig(cfg) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                        ),
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text(cfg, fontSize = 9.sp, maxLines = 1)
                    }
                }
            }
        }

        Divider()

        // --- 6. BACKUP & CONFIGURATION ---
        Text("Backup & Configuration", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Button(
                onClick = {
                    val data = exportSettings(settings)
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Nova Bar Settings", data))
                    Toast.makeText(context, "Settings copied to clipboard!", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Export Backup")
            }

            Button(
                onClick = {
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val item = clipboard.primaryClip?.getItemAt(0)
                    val text = item?.text?.toString() ?: ""
                    if (text.startsWith("NovaBarSettingsV1:") || text.startsWith("NovaBarSettingsV2:") || text.startsWith("NovaBarSettingsV3:") || text.startsWith("NovaBarSettingsV4:") || text.startsWith("NovaBarSettingsV5:")) {
                        val imported = importSettings(text)
                        if (imported != null) {
                            viewModel.importSettings(imported)
                            Toast.makeText(context, "Settings loaded successfully!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Corrupted settings content", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "No valid settings copied in clipboard", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.weight(1f)
            ) {
                Text("Import Backup")
            }
        }

    }

    if (showDiagnosticsDialog) {
        AlertDialog(
            onDismissRequest = { showDiagnosticsDialog = false },
            title = { Text("Developer Diagnostics", fontWeight = FontWeight.Bold) },
            text = {
                Box(modifier = Modifier.fillMaxHeight(0.85f)) {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        DiagnosticsDashboard(
                            context = context,
                            hasNotificationPermission = hasNotificationPermission.value,
                            hasAccessibilityPermission = hasAccessibilityPermission.value,
                            hasOverlayPermission = hasOverlayPermission.value
                        )
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showDiagnosticsDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
fun PermissionCard(
    title: String,
    description: String,
    isGranted: Boolean,
    onRequest: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted) Color(0x1F4CAF50) else Color(0x1FF44336)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                Text(
                    text = if (isGranted) "Granted" else "Required",
                    color = if (isGranted) Color(0xFF4CAF50) else Color(0xFFF44336),
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
            Text(description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            if (!isGranted) {
                Button(
                    onClick = onRequest,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Grant Permission", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun PermissionIndicator(label: String, isGranted: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(if (isGranted) Color(0xFF4CAF50) else Color(0xFFF44336))
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )
    }
}

@Composable
fun SliderSetting(
    title: String,
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(title, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun ToggleSetting(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

private fun isNotificationServiceEnabled(context: Context): Boolean {
    val cn = ComponentName(context, NovaNotificationListener::class.java)
    val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
    return flat != null && flat.contains(cn.flattenToString())
}

private fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    val enabledServices = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_GENERIC)
    for (service in enabledServices) {
        val componentId = service.resolveInfo.serviceInfo.packageName + "/" + service.resolveInfo.serviceInfo.name
        if (componentId.contains(context.packageName) && service.resolveInfo.serviceInfo.name.contains("NovaAccessibilityService")) {
            return true
        }
    }
    return false
}

// Custom String exporter/importer representation
private fun exportSettings(s: NovaSettings): String {
    val pkgs = s.allowedNotificationPackages.joinToString("|")
    return "NovaBarSettingsV6:${s.isEnabled},${s.positionY},${s.cornerRadius},${s.opacity},${s.blurRadius},${s.animationSpeedMultiplier},${s.mediaControlsEnabled},${s.timerEnabled},${s.stopwatchEnabled},${s.navigationEnabled},${s.chargingEnabled},${s.notificationsEnabled},${s.colorAdaptationEnabled},${pkgs},${s.barWidthScale},${s.barHeightPadding},${s.barBorderThickness},${s.barGravity},${s.offsetX},${s.offsetY},${s.showWhenIdle},${s.defaultPresentationMode},${s.visualizerStyle},${s.visualizerSensitivity},${s.albumArtCornerRadius},${s.progressVisibility},${s.autoCollapseTimeout},${s.textSize},${s.overlayPosition},${s.alwaysOnBar},${s.alwaysOnConfig},${s.timeFormat},${s.showSeconds},${s.showOnLockscreen},${s.cameraCutoutMode},${s.cameraCutoutGapScale},${s.leftSegmentWidthDp},${s.rightSegmentWidthDp}"
}

private fun importSettings(str: String): NovaSettings? {
    return try {
        if (str.startsWith("NovaBarSettingsV6:")) {
            val parts = str.substringAfter("NovaBarSettingsV6:").split(",")
            val pkgs = parts[13].split("|").filter { it.isNotEmpty() }.toSet()
            NovaSettings(
                isEnabled = parts[0].toBoolean(),
                positionY = parts[1].toInt(),
                cornerRadius = parts[2].toInt(),
                opacity = parts[3].toFloat(),
                blurRadius = parts[4].toInt(),
                animationSpeedMultiplier = parts[5].toFloat(),
                mediaControlsEnabled = parts[6].toBoolean(),
                timerEnabled = parts[7].toBoolean(),
                stopwatchEnabled = parts[8].toBoolean(),
                navigationEnabled = parts[9].toBoolean(),
                chargingEnabled = parts[10].toBoolean(),
                notificationsEnabled = parts[11].toBoolean(),
                colorAdaptationEnabled = parts[12].toBoolean(),
                allowedNotificationPackages = pkgs,
                barWidthScale = parts[14].toFloat(),
                barHeightPadding = parts[15].toInt(),
                barBorderThickness = parts[16].toInt(),
                barGravity = parts[17],
                offsetX = parts[18].toInt(),
                offsetY = parts[19].toInt(),
                showWhenIdle = parts[20].toBoolean(),
                defaultPresentationMode = parts[21],
                visualizerStyle = parts[22],
                visualizerSensitivity = parts[23].toFloat(),
                albumArtCornerRadius = parts[24].toInt(),
                progressVisibility = parts[25].toBoolean(),
                autoCollapseTimeout = parts[26].toInt(),
                textSize = parts[27],
                overlayPosition = parts[28],
                alwaysOnBar = parts[29].toBoolean(),
                alwaysOnConfig = parts[30],
                timeFormat = parts[31],
                showSeconds = parts[32].toBoolean(),
                showOnLockscreen = parts[33].toBoolean(),
                cameraCutoutMode = if (parts.size > 34) parts[34].toBoolean() else false,
                cameraCutoutGapScale = if (parts.size > 35) parts[35].toFloat() else 1.0f,
                leftSegmentWidthDp = if (parts.size > 36) parts[36].toInt() else 120,
                rightSegmentWidthDp = if (parts.size > 37) parts[37].toInt() else 120
            )
        } else if (str.startsWith("NovaBarSettingsV5:")) {
            val parts = str.substringAfter("NovaBarSettingsV5:").split(",")
            val pkgs = parts[13].split("|").filter { it.isNotEmpty() }.toSet()
            NovaSettings(
                isEnabled = parts[0].toBoolean(),
                positionY = parts[1].toInt(),
                cornerRadius = parts[2].toInt(),
                opacity = parts[3].toFloat(),
                blurRadius = parts[4].toInt(),
                animationSpeedMultiplier = parts[5].toFloat(),
                mediaControlsEnabled = parts[6].toBoolean(),
                timerEnabled = parts[7].toBoolean(),
                stopwatchEnabled = parts[8].toBoolean(),
                navigationEnabled = parts[9].toBoolean(),
                chargingEnabled = parts[10].toBoolean(),
                notificationsEnabled = parts[11].toBoolean(),
                colorAdaptationEnabled = parts[12].toBoolean(),
                allowedNotificationPackages = pkgs,
                barWidthScale = parts[14].toFloat(),
                barHeightPadding = parts[15].toInt(),
                barBorderThickness = parts[16].toInt(),
                barGravity = parts[17],
                offsetX = parts[18].toInt(),
                offsetY = parts[19].toInt(),
                showWhenIdle = parts[20].toBoolean(),
                defaultPresentationMode = parts[21],
                visualizerStyle = parts[22],
                visualizerSensitivity = parts[23].toFloat(),
                albumArtCornerRadius = parts[24].toInt(),
                progressVisibility = parts[25].toBoolean(),
                autoCollapseTimeout = parts[26].toInt(),
                textSize = parts[27],
                overlayPosition = parts[28],
                alwaysOnBar = parts[29].toBoolean(),
                alwaysOnConfig = parts[30],
                timeFormat = parts[31],
                showSeconds = parts[32].toBoolean(),
                showOnLockscreen = parts[33].toBoolean(),
                cameraCutoutMode = if (parts.size > 34) parts[34].toBoolean() else false,
                cameraCutoutGapScale = if (parts.size > 35) parts[35].toFloat() else 1.0f,
                leftSegmentWidthDp = 120,
                rightSegmentWidthDp = 120
            )
        } else if (str.startsWith("NovaBarSettingsV4:")) {
            val parts = str.substringAfter("NovaBarSettingsV4:").split(",")
            val pkgs = parts[13].split("|").filter { it.isNotEmpty() }.toSet()
            NovaSettings(
                isEnabled = parts[0].toBoolean(),
                positionY = parts[1].toInt(),
                cornerRadius = parts[2].toInt(),
                opacity = parts[3].toFloat(),
                blurRadius = parts[4].toInt(),
                animationSpeedMultiplier = parts[5].toFloat(),
                mediaControlsEnabled = parts[6].toBoolean(),
                timerEnabled = parts[7].toBoolean(),
                stopwatchEnabled = parts[8].toBoolean(),
                navigationEnabled = parts[9].toBoolean(),
                chargingEnabled = parts[10].toBoolean(),
                notificationsEnabled = parts[11].toBoolean(),
                colorAdaptationEnabled = parts[12].toBoolean(),
                allowedNotificationPackages = pkgs,
                barWidthScale = parts[14].toFloat(),
                barHeightPadding = parts[15].toInt(),
                barBorderThickness = parts[16].toInt(),
                barGravity = parts[17],
                offsetX = parts[18].toInt(),
                offsetY = parts[19].toInt(),
                showWhenIdle = parts[20].toBoolean(),
                defaultPresentationMode = parts[21],
                visualizerStyle = parts[22],
                visualizerSensitivity = parts[23].toFloat(),
                albumArtCornerRadius = parts[24].toInt(),
                progressVisibility = parts[25].toBoolean(),
                autoCollapseTimeout = parts[26].toInt(),
                textSize = parts[27],
                overlayPosition = parts[28],
                alwaysOnBar = parts[29].toBoolean(),
                alwaysOnConfig = parts[30],
                timeFormat = parts[31],
                showSeconds = parts[32].toBoolean(),
                showOnLockscreen = if (parts.size > 33) parts[33].toBoolean() else true,
                cameraCutoutMode = false
            )
        } else if (str.startsWith("NovaBarSettingsV3:")) {
            val parts = str.substringAfter("NovaBarSettingsV3:").split(",")
            val pkgs = parts[13].split("|").filter { it.isNotEmpty() }.toSet()
            NovaSettings(
                isEnabled = parts[0].toBoolean(),
                positionY = parts[1].toInt(),
                cornerRadius = parts[2].toInt(),
                opacity = parts[3].toFloat(),
                blurRadius = parts[4].toInt(),
                animationSpeedMultiplier = parts[5].toFloat(),
                mediaControlsEnabled = parts[6].toBoolean(),
                timerEnabled = parts[7].toBoolean(),
                stopwatchEnabled = parts[8].toBoolean(),
                navigationEnabled = parts[9].toBoolean(),
                chargingEnabled = parts[10].toBoolean(),
                notificationsEnabled = parts[11].toBoolean(),
                colorAdaptationEnabled = parts[12].toBoolean(),
                allowedNotificationPackages = pkgs,
                barWidthScale = parts[14].toFloat(),
                barHeightPadding = parts[15].toInt(),
                barBorderThickness = parts[16].toInt(),
                barGravity = parts[17],
                offsetX = parts[18].toInt(),
                offsetY = parts[19].toInt(),
                showWhenIdle = parts[20].toBoolean(),
                defaultPresentationMode = parts[21],
                visualizerStyle = parts[22],
                visualizerSensitivity = parts[23].toFloat(),
                albumArtCornerRadius = parts[24].toInt(),
                progressVisibility = parts[25].toBoolean(),
                autoCollapseTimeout = parts[26].toInt()
            )
        } else if (str.startsWith("NovaBarSettingsV2:")) {
            val parts = str.substringAfter("NovaBarSettingsV2:").split(",")
            val pkgs = parts[13].split("|").filter { it.isNotEmpty() }.toSet()
            NovaSettings(
                isEnabled = parts[0].toBoolean(),
                positionY = parts[1].toInt(),
                cornerRadius = parts[2].toInt(),
                opacity = parts[3].toFloat(),
                blurRadius = parts[4].toInt(),
                animationSpeedMultiplier = parts[5].toFloat(),
                mediaControlsEnabled = parts[6].toBoolean(),
                timerEnabled = parts[7].toBoolean(),
                stopwatchEnabled = parts[8].toBoolean(),
                navigationEnabled = parts[9].toBoolean(),
                chargingEnabled = parts[10].toBoolean(),
                notificationsEnabled = parts[11].toBoolean(),
                colorAdaptationEnabled = parts[12].toBoolean(),
                allowedNotificationPackages = pkgs,
                barWidthScale = parts[14].toFloat(),
                barHeightPadding = parts[15].toInt(),
                barBorderThickness = parts[16].toInt(),
                barGravity = parts[17],
                offsetX = parts[18].toInt(),
                offsetY = parts[19].toInt(),
                showWhenIdle = parts[20].toBoolean()
            )
        } else {
            val parts = str.substringAfter("NovaBarSettingsV1:").split(",")
            val pkgs = parts[13].split("|").filter { it.isNotEmpty() }.toSet()
            NovaSettings(
                isEnabled = parts[0].toBoolean(),
                positionY = parts[1].toInt(),
                cornerRadius = parts[2].toInt(),
                opacity = parts[3].toFloat(),
                blurRadius = parts[4].toInt(),
                animationSpeedMultiplier = parts[5].toFloat(),
                mediaControlsEnabled = parts[6].toBoolean(),
                timerEnabled = parts[7].toBoolean(),
                stopwatchEnabled = parts[8].toBoolean(),
                navigationEnabled = parts[9].toBoolean(),
                chargingEnabled = parts[10].toBoolean(),
                notificationsEnabled = parts[11].toBoolean(),
                colorAdaptationEnabled = parts[12].toBoolean(),
                allowedNotificationPackages = pkgs
            )
        }
    } catch (e: Exception) {
        null
    }
}

@Composable
fun DiagnosticsDashboard(
    context: Context,
    hasNotificationPermission: Boolean,
    hasAccessibilityPermission: Boolean,
    hasOverlayPermission: Boolean
) {
    var showDashboard by remember { mutableStateOf(false) }

    LaunchedEffect(showDashboard) {
        com.novabar.app.domain.DiagnosticsManager.showDebugMarkers.value = showDashboard
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Developer Diagnostics Dashboard", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("Real-time debugging parameters", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                }
                Switch(
                    checked = showDashboard,
                    onCheckedChange = { showDashboard = it }
                )
            }

            if (showDashboard) {
                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // Collect StateFlows from DiagnosticsManager
                val overlayEngine by DiagnosticsManager.overlayEngine.collectAsState()
                val windowType by DiagnosticsManager.windowType.collectAsState()
                val touchableState by DiagnosticsManager.touchableState.collectAsState()
                val currentActivity by DiagnosticsManager.currentActivity.collectAsState()
                val currentPresentationState by DiagnosticsManager.currentPresentationState.collectAsState()
                val blurEnabled by DiagnosticsManager.blurEnabled.collectAsState()
                val blurBackend by DiagnosticsManager.blurBackend.collectAsState()
                val windowWidth by DiagnosticsManager.windowWidth.collectAsState()
                val windowHeight by DiagnosticsManager.windowHeight.collectAsState()
                val touchEventsReceived by DiagnosticsManager.touchEventsReceived.collectAsState()

                val mediaSessionActive by DiagnosticsManager.mediaSessionActive.collectAsState()
                val mediaAppName by DiagnosticsManager.mediaAppName.collectAsState()
                val mediaPackageName by DiagnosticsManager.mediaPackageName.collectAsState()
                val mediaPlaybackState by DiagnosticsManager.mediaPlaybackState.collectAsState()
                val mediaTrackTitle by DiagnosticsManager.mediaTrackTitle.collectAsState()
                val mediaArtist by DiagnosticsManager.mediaArtist.collectAsState()
                val mediaProgress by DiagnosticsManager.mediaProgress.collectAsState()
                val mediaControlsAvailable by DiagnosticsManager.mediaControlsAvailable.collectAsState()
                val lastMediaUpdateTimestamp by DiagnosticsManager.lastMediaUpdateTimestamp.collectAsState()

                val notificationListenerConnected by DiagnosticsManager.notificationListenerConnected.collectAsState()
                val lastNotificationApp by DiagnosticsManager.lastNotificationApp.collectAsState()
                val lastNotificationPackage by DiagnosticsManager.lastNotificationPackage.collectAsState()
                val notificationCount by DiagnosticsManager.notificationCount.collectAsState()

                val accessibilityOverlayActive by DiagnosticsManager.accessibilityOverlayActive.collectAsState()
                val lastAccessibilityEvent by DiagnosticsManager.lastAccessibilityEvent.collectAsState()
                val accessibilityForegroundPackage by DiagnosticsManager.accessibilityForegroundPackage.collectAsState()

                val overlayVisible by DiagnosticsManager.overlayVisible.collectAsState()
                val overlayAttached by DiagnosticsManager.overlayAttached.collectAsState()
                val currentOverlayBounds by DiagnosticsManager.currentOverlayBounds.collectAsState()
                val currentPriorityActivity by DiagnosticsManager.currentPriorityActivity.collectAsState()

                val cutoutHasCutout by DiagnosticsManager.hasDisplayCutout.collectAsState()
                val cutoutWidthDiag by DiagnosticsManager.cutoutWidth.collectAsState()
                val cutoutCenterXDiag by DiagnosticsManager.cutoutCenterX.collectAsState()
                val cutoutModeEnabled by DiagnosticsManager.cameraCutoutModeEnabled.collectAsState()
                val cutoutGapScale by DiagnosticsManager.cutoutGapScale.collectAsState()
                val finalGapWidth by DiagnosticsManager.finalGapWidth.collectAsState()

                // Display Sections
                DiagnosticsSection(title = "General Settings") {
                    DiagnosticsRow("Overlay Engine", overlayEngine)
                    DiagnosticsRow("Window Type", windowType)
                    DiagnosticsRow("Touchable State", touchableState)
                    DiagnosticsRow("Current Activity", currentActivity)
                    DiagnosticsRow("Current State", currentPresentationState)
                    DiagnosticsRow("Blur Enabled", blurEnabled.toString())
                    DiagnosticsRow("Blur Backend", blurBackend)
                    DiagnosticsRow("Dimensions", "${windowWidth}x${windowHeight} px")
                    DiagnosticsRow("Touch Count", touchEventsReceived.toString())
                }

                DiagnosticsSection(title = "Display Cutout Status") {
                    DiagnosticsRow("Has Cutout", cutoutHasCutout.toString())
                    DiagnosticsRow("Cutout Width", "$cutoutWidthDiag px")
                    DiagnosticsRow("Cutout Center X", "$cutoutCenterXDiag px")
                    DiagnosticsRow("Cutout Mode Enabled", cutoutModeEnabled.toString())
                    DiagnosticsRow("Cutout Gap Scale", "${String.format(java.util.Locale.US, "%.2f", cutoutGapScale)}x")
                    DiagnosticsRow("Final Gap Width", "$finalGapWidth px")
                }

                DiagnosticsSection(title = "Media Playback Status") {
                    DiagnosticsRow("Active Session", mediaSessionActive.toString())
                    DiagnosticsRow("App Name", mediaAppName)
                    DiagnosticsRow("Package", mediaPackageName)
                    DiagnosticsRow("Playback State", mediaPlaybackState)
                    DiagnosticsRow("Track Title", mediaTrackTitle)
                    DiagnosticsRow("Artist", mediaArtist)
                    DiagnosticsRow("Progress", mediaProgress)
                    DiagnosticsRow("Controls", mediaControlsAvailable)
                    DiagnosticsRow("Last Updated", lastMediaUpdateTimestamp)
                }

                DiagnosticsSection(title = "Notifications & Listener") {
                    DiagnosticsRow("Listener Connected", notificationListenerConnected.toString())
                    DiagnosticsRow("Last App Name", lastNotificationApp)
                    DiagnosticsRow("Last Package", lastNotificationPackage)
                    DiagnosticsRow("Listener Count", notificationCount.toString())
                    DiagnosticsRow("Permission", if (hasNotificationPermission) "GRANTED" else "DENIED")
                }

                DiagnosticsSection(title = "Accessibility Engine") {
                    DiagnosticsRow("Service Running", if (hasAccessibilityPermission) "ENABLED" else "DISABLED")
                    DiagnosticsRow("Overlay Active", accessibilityOverlayActive.toString())
                    DiagnosticsRow("Last Event Type", lastAccessibilityEvent)
                    DiagnosticsRow("Foreground App", accessibilityForegroundPackage)
                }

                DiagnosticsSection(title = "Overlay Layout Tree") {
                    DiagnosticsRow("Overlay Shown", overlayVisible.toString())
                    DiagnosticsRow("View Attached", overlayAttached.toString())
                    DiagnosticsRow("Overlay Bounds", currentOverlayBounds)
                    DiagnosticsRow("Priority Activity", currentPriorityActivity)
                }

                val isLoggingEnabledState by DeveloperLogger.isLoggingEnabled.collectAsState()
                
                DiagnosticsSection(title = "Developer Diagnostic Logs") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Record Debug Logs", fontSize = 11.sp)
                        Switch(
                            checked = isLoggingEnabledState,
                            onCheckedChange = { checked ->
                                DeveloperLogger.setLoggingEnabled(context, checked)
                            }
                        )
                    }
                    
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val logContent = DeveloperLogger.readLog(context)
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Nova Bar Debug Log", logContent))
                                Toast.makeText(context, "Debug logs copied to clipboard!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text("Copy Log", fontSize = 10.sp, maxLines = 1)
                        }

                        Button(
                            onClick = {
                                try {
                                    val logFile = DeveloperLogger.getLogFile(context)
                                    if (!logFile.exists() || logFile.length() == 0L) {
                                        Toast.makeText(context, "Log file is empty or doesn't exist yet", Toast.LENGTH_SHORT).show()
                                    } else {
                                        val authority = "${context.packageName}.fileprovider"
                                        val uri = androidx.core.content.FileProvider.getUriForFile(context, authority, logFile)
                                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                            type = "text/plain"
                                            putExtra(Intent.EXTRA_STREAM, uri)
                                            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        }
                                        context.startActivity(Intent.createChooser(shareIntent, "Export Debug Log"))
                                    }
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Failed to share log: ${e.message}", Toast.LENGTH_LONG).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text("Export Log", fontSize = 10.sp, maxLines = 1)
                        }

                        Button(
                            onClick = {
                                DeveloperLogger.clearLog(context)
                                Toast.makeText(context, "Debug logs cleared!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text("Clear Log", fontSize = 10.sp, maxLines = 1)
                        }
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // Debug Actions Buttons Grid
                Text("Debug Actions", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            Toast.makeText(context, "Diagnostics refreshed!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text("Refresh", fontSize = 10.sp, maxLines = 1)
                    }

                    Button(
                        onClick = {
                            com.novabar.app.domain.OverlayStateManager.expand()
                            Toast.makeText(context, "Force Expand requested", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text("Expand", fontSize = 10.sp, maxLines = 1)
                    }

                    Button(
                        onClick = {
                            com.novabar.app.domain.OverlayStateManager.collapse()
                            Toast.makeText(context, "Force Collapse requested", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text("Collapse", fontSize = 10.sp, maxLines = 1)
                    }
                }

                Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            DiagnosticsManager.resetTrigger.value += 1
                            Toast.makeText(context, "Recreating Overlay View...", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text("Reset Overlay", fontSize = 10.sp, maxLines = 1)
                    }

                    Button(
                        onClick = {
                            com.novabar.app.domain.OverlayStateManager.updateNotification(
                                com.novabar.app.domain.NotificationState(
                                    id = "test_diagnostics_id",
                                    packageName = "com.novabar.app",
                                    appName = "Nova Bar Test",
                                    title = "Diagnostics Testing",
                                    summary = "Simulating active overlay alerts."
                                )
                            )
                            Toast.makeText(context, "Injected Test Overlay", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text("Test UI", fontSize = 10.sp, maxLines = 1)
                    }

                    Button(
                        onClick = {
                            val list = com.novabar.app.domain.OverlayStateManager.activeActivities.value
                            val current = com.novabar.app.domain.OverlayStateManager.activeState.value
                            val report = "Active States: ${list.joinToString { it::class.java.simpleName }}, Current: ${current::class.java.simpleName}"
                            Log.d("NovaBarDiagnostics", "STATE_DUMP: $report")
                            Toast.makeText(context, "State dumped to Logcat!", Toast.LENGTH_LONG).show()
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text("Dump State", fontSize = 10.sp, maxLines = 1)
                    }
                }

                Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            com.novabar.app.domain.OverlayStateManager.mediaState.value = 
                                com.novabar.app.domain.MediaState(
                                    isPlaying = true,
                                    title = "Citizen - Sun Kisses",
                                    artist = "Citizen",
                                    progress = 0.592f,
                                    duration = 238000L,
                                    position = 141000L,
                                    albumArt = null,
                                    appName = "YouTube"
                                )
                            Toast.makeText(context, "Injected Test Media", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text("Test Media", fontSize = 10.sp, maxLines = 1)
                    }

                    Button(
                        onClick = {
                            com.novabar.app.domain.OverlayStateManager.mediaState.value = null
                            com.novabar.app.domain.OverlayStateManager.phoneCallState.value = null
                            com.novabar.app.domain.OverlayStateManager.dismissNotification()
                            com.novabar.app.domain.OverlayStateManager.setTimerState(null)
                            Toast.makeText(context, "Cleared States", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Text("Clear States", fontSize = 10.sp, maxLines = 1)
                    }
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // Logging, Copying & Sharing buttons
                val generateReport = {
                    val list = com.novabar.app.domain.OverlayStateManager.activeActivities.value
                    val currentState = com.novabar.app.domain.OverlayStateManager.activeState.value
                    """
                    # Nova Bar Runtime Diagnostics Report
                    Generated: ${java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())}
                    
                    ## 1. General Info
                    - Overlay Engine: $overlayEngine
                    - Window Type: $windowType
                    - Touchable State: $touchableState
                    - Current Activity: $currentActivity
                    - Current State: $currentPresentationState
                    - Blur Enabled: $blurEnabled
                    - Blur Backend: $blurBackend
                    - Dimensions: ${windowWidth}x${windowHeight} px
                    - Touch Count: $touchEventsReceived
                    
                    ## 2. Media Playback
                    - Active Session: $mediaSessionActive
                    - App Name: $mediaAppName
                    - Package Name: $mediaPackageName
                    - Playback State: $mediaPlaybackState
                    - Track Title: $mediaTrackTitle
                    - Artist: $mediaArtist
                    - Progress: $mediaProgress
                    - Controls Available: $mediaControlsAvailable
                    - Last Update: $lastMediaUpdateTimestamp
                    
                    ## 3. Notifications & Listener
                    - Listener Connected: $notificationListenerConnected
                    - Last Notification App: $lastNotificationApp
                    - Last Notification Package: $lastNotificationPackage
                    - Notification Count: $notificationCount
                    - Notification Access: ${if (hasNotificationPermission) "GRANTED" else "DENIED"}
                    
                    ## 4. Accessibility Service
                    - Enabled: ${if (hasAccessibilityPermission) "ENABLED" else "DISABLED"}
                    - Overlay Active: $accessibilityOverlayActive
                    - Last Event: $lastAccessibilityEvent
                    - Foreground App: $accessibilityForegroundPackage
                    
                    ## 5. Overlay Layout Tree
                    - Overlay Visible: $overlayVisible
                    - Overlay Attached: $overlayAttached
                    - Overlay Bounds: $currentOverlayBounds
                    - Priority Activity List: $currentPriorityActivity
                    """.trimIndent()
                }

                Text("Logs & Exporting", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val report = generateReport()
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            clipboard.setPrimaryClip(ClipData.newPlainText("Nova Bar Diagnostics", report))
                            Toast.makeText(context, "Report copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Copy Report", fontSize = 11.sp)
                    }

                    Button(
                        onClick = {
                            val report = generateReport()
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, "Nova Bar Runtime Diagnostics")
                                putExtra(Intent.EXTRA_TEXT, report)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Diagnostics"))
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Share Report", fontSize = 11.sp)
                    }
                }
            }
        }
    }
}

@Composable
fun DiagnosticsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            content()
        }
    }
}

@Composable
fun DiagnosticsRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        Text(value, fontSize = 10.sp, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

