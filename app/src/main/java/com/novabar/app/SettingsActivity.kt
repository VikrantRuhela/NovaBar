package com.novabar.app

import android.accessibilityservice.AccessibilityServiceInfo
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

class SettingsActivity : ComponentActivity() {

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                hasOverlayPermission.value = Settings.canDrawOverlays(context)
                hasNotificationPermission.value = isNotificationServiceEnabled(context)
                hasAccessibilityPermission.value = isAccessibilityServiceEnabled(context)

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
        Text("Permissions Status", fontSize = 18.sp, fontWeight = FontWeight.Bold)

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

        Divider()

        // --- 2. MAIN SWITCH ---
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Enable Nova Bar", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    Text("Enable/disable floating now bar overlay", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                }
                Switch(
                    checked = settings.isEnabled,
                    onCheckedChange = { checked ->
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
                )
            }
        }

        Divider()

        // --- 3. CUSTOMIZATION SLIDERS ---
        Text("Visual Customizations", fontSize = 18.sp, fontWeight = FontWeight.Bold)

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
            value = settings.barWidthScale,
            valueRange = 0.1f..2.0f,
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
            title = "Glass Opacity: ${(settings.opacity * 100).toInt()}%",
            value = settings.opacity,
            valueRange = 0.2f..1.0f,
            onValueChange = { viewModel.setOpacity(it) }
        )

        SliderSetting(
            title = "Blur Intensity: ${settings.blurRadius}px",
            value = settings.blurRadius.toFloat(),
            valueRange = 5f..50f,
            onValueChange = { viewModel.setBlurRadius(it.toInt()) }
        )

        SliderSetting(
            title = "Animation Speed: ${String.format("%.1fx", settings.animationSpeedMultiplier)}",
            value = settings.animationSpeedMultiplier,
            valueRange = 0.5f..2.0f,
            onValueChange = { viewModel.setAnimationSpeed(it) }
        )

        Divider()

        // --- Now Bar State & Visualizer Settings ---
        Text("Now Bar State & Visualizer Settings", fontSize = 18.sp, fontWeight = FontWeight.Bold)

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
                    if (text.startsWith("NovaBarSettingsV1:") || text.startsWith("NovaBarSettingsV2:") || text.startsWith("NovaBarSettingsV3:") || text.startsWith("NovaBarSettingsV4:")) {
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
    return "NovaBarSettingsV4:${s.isEnabled},${s.positionY},${s.cornerRadius},${s.opacity},${s.blurRadius},${s.animationSpeedMultiplier},${s.mediaControlsEnabled},${s.timerEnabled},${s.stopwatchEnabled},${s.navigationEnabled},${s.chargingEnabled},${s.notificationsEnabled},${s.colorAdaptationEnabled},${pkgs},${s.barWidthScale},${s.barHeightPadding},${s.barBorderThickness},${s.barGravity},${s.offsetX},${s.offsetY},${s.showWhenIdle},${s.defaultPresentationMode},${s.visualizerStyle},${s.visualizerSensitivity},${s.albumArtCornerRadius},${s.progressVisibility},${s.autoCollapseTimeout},${s.textSize},${s.overlayPosition},${s.alwaysOnBar},${s.alwaysOnConfig},${s.timeFormat},${s.showSeconds}"
}

private fun importSettings(str: String): NovaSettings? {
    return try {
        if (str.startsWith("NovaBarSettingsV4:")) {
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
                showSeconds = parts[32].toBoolean()
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

