package com.novabar.app

import android.accessibilityservice.AccessibilityServiceInfo
import android.util.Log
import androidx.compose.ui.res.painterResource
import com.novabar.app.ui.icons.NovaIcons
import com.novabar.app.data.OverlayEngine
import com.novabar.app.domain.DiagnosticsManager
import com.novabar.app.domain.OverlayStateManager
import com.novabar.app.domain.OverlayState
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
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.novabar.app.data.NovaSettings
import com.novabar.app.presentation.SettingsViewModel
import com.novabar.app.presentation.ViewModelFactory
import com.novabar.app.services.NovaNotificationListener
import com.novabar.app.services.OverlayService
import com.novabar.app.ui.theme.NovaBarTheme
import com.novabar.app.utils.DeveloperLogger
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
                SettingsScreen(
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

data class TabItem(val title: String, val iconRes: Int)

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val viewModel: SettingsViewModel = viewModel(
        factory = ViewModelFactory(context)
    )
    val settings by viewModel.settingsFlow.collectAsState()

    LaunchedEffect(settings) {
        OverlayStateManager.settingsFlow.value = settings
    }

    var selectedTab by remember { mutableStateOf(0) }

    val hasOverlayPermission = remember { mutableStateOf(false) }
    val hasNotificationPermission = remember { mutableStateOf(false) }
    val hasAccessibilityPermission = remember { mutableStateOf(false) }
    val hasPhonePermission = remember { mutableStateOf(false) }

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

    val tabs = listOf(
        TabItem("Home", NovaIcons.Home),
        TabItem("Appearance", NovaIcons.Appearance),
        TabItem("Activities", NovaIcons.Activities),
        TabItem("Settings", NovaIcons.Settings),
        TabItem("Developer", NovaIcons.Developer)
    )

    Scaffold(
        bottomBar = {
            FloatingPillNavigationBar(
                tabs = tabs,
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(bottom = paddingValues.calculateBottomPadding())
        ) {
            AnimatedContent(
                targetState = selectedTab,
                transitionSpec = {
                    if (targetState > initialState) {
                        slideInHorizontally { width -> width } + fadeIn() togetherWith
                                slideOutHorizontally { width -> -width } + fadeOut()
                    } else {
                        slideInHorizontally { width -> -width } + fadeIn() togetherWith
                                slideOutHorizontally { width -> width } + fadeOut()
                    }
                },
                modifier = Modifier.fillMaxSize()
            ) { tab ->
                when (tab) {
                    0 -> HomeScreen(
                        viewModel = viewModel,
                        settings = settings,
                        hasOverlay = hasOverlayPermission.value,
                        hasNotification = hasNotificationPermission.value,
                        hasAccessibility = hasAccessibilityPermission.value,
                        hasPhone = hasPhonePermission.value,
                        onRequestPhone = { requestPermissionLauncher.launch(android.Manifest.permission.ANSWER_PHONE_CALLS) }
                    )
                    1 -> AppearanceStudioScreen(viewModel = viewModel, settings = settings)
                    2 -> ActivityManagerScreen(viewModel = viewModel, settings = settings)
                    3 -> GeneralSettingsScreen(viewModel = viewModel, settings = settings)
                    4 -> DeveloperScreen(
                        context = context,
                        viewModel = viewModel,
                        settings = settings,
                        hasNotificationPermission = hasNotificationPermission.value,
                        hasAccessibilityPermission = hasAccessibilityPermission.value,
                        hasOverlayPermission = hasOverlayPermission.value
                    )
                }
            }
        }
    }
}

@Composable
fun FloatingPillNavigationBar(
    tabs: List<TabItem>,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedIndex by animateFloatAsState(
        targetValue = selectedTab.toFloat(),
        animationSpec = spring(
            stiffness = Spring.StiffnessLow,
            dampingRatio = Spring.DampingRatioMediumBouncy
        )
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .shadow(elevation = 12.dp, shape = CircleShape),
            shape = CircleShape,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val maxWidthPx = constraints.maxWidth
                    val tabWidth = maxWidth / tabs.size
                    Box(
                        modifier = Modifier
                            .width(tabWidth)
                            .fillMaxHeight()
                            .padding(vertical = 8.dp, horizontal = 6.dp)
                            .offset {
                                IntOffset(
                                    x = (animatedIndex * maxWidthPx / tabs.size).toInt(),
                                    y = 0
                                )
                            }
                            .background(
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                shape = CircleShape
                            )
                    )
                }

                // Row of active icons
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEachIndexed { index, tab ->
                        val isSelected = index == selectedTab
                        val scale by animateFloatAsState(
                            targetValue = if (isSelected) 1.2f else 1.0f,
                            animationSpec = spring(stiffness = Spring.StiffnessMedium)
                        )

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    onTabSelected(index)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    painter = painterResource(id = tab.iconRes),
                                    contentDescription = tab.title,
                                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                    modifier = Modifier
                                        .size(24.dp)
                                        .graphicsLayer {
                                            scaleX = scale
                                            scaleY = scale
                                        }
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = tab.title,
                                    fontSize = 9.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 1. HOME DASHBOARD
// ==========================================
@Composable
fun HomeScreen(
    viewModel: SettingsViewModel,
    settings: NovaSettings,
    hasOverlay: Boolean,
    hasNotification: Boolean,
    hasAccessibility: Boolean,
    hasPhone: Boolean,
    onRequestPhone: () -> Unit
) {
    val context = LocalContext.current
    var permissionsExpanded by remember { mutableStateOf(false) }

    val activeState by DiagnosticsManager.currentPresentationState.collectAsState()

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Nova Bar Running Status Header Minimal Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, if (settings.isEnabled) Color(0xFF4CAF50).copy(alpha = 0.3f) else Color(0xFFF44336).copy(alpha = 0.3f)),
            colors = CardDefaults.cardColors(
                containerColor = if (settings.isEnabled) Color(0xFF4CAF50).copy(alpha = 0.08f) else Color(0xFFF44336).copy(alpha = 0.08f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(if (settings.isEnabled) Color(0xFF4CAF50) else Color(0xFFF44336))
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (settings.isEnabled) "Nova Bar Active" else "Nova Bar Inactive",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    val serviceHealth = (if (hasOverlay) 1 else 0) + 
                                         (if (hasAccessibility) 1 else 0) + 
                                         (if (hasNotification) 1 else 0)
                    Text(
                        text = "System Health: $serviceHealth/3 Engines Running • State: $activeState",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Expandable Permission Panel
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                val grantedCount = (if (hasAccessibility) 1 else 0) +
                                   (if (hasNotification) 1 else 0) +
                                   (if (hasOverlay) 1 else 0) +
                                   (if (hasPhone) 1 else 0)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { permissionsExpanded = !permissionsExpanded },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Permission Center",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "$grantedCount of 4 permissions configured",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }

                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    ) {
                        CircularProgressIndicator(
                            progress = grantedCount / 4.0f,
                            modifier = Modifier.size(36.dp),
                            strokeWidth = 3.dp,
                            color = if (grantedCount == 4) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "${(grantedCount * 25)}%",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Icon(
                        painter = painterResource(id = if (permissionsExpanded) NovaIcons.ArrowUp else NovaIcons.ArrowDown),
                        contentDescription = "Expand/Collapse",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                }

                if (permissionsExpanded) {
                    Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
                    
                    PermissionRowItem(
                        title = "Display Overlay Permission",
                        description = "Shows the floating capsule bar over apps.",
                        isGranted = hasOverlay,
                        onRequest = {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            context.startActivity(intent)
                        }
                    )

                    PermissionRowItem(
                        title = "Notification & Media Access",
                        description = "Reads active music tracks, timer and alerts.",
                        isGranted = hasNotification,
                        onRequest = {
                            val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
                            context.startActivity(intent)
                        }
                    )

                    PermissionRowItem(
                        title = "Accessibility Color Service",
                        description = "Enables content-aware status bar color matching.",
                        isGranted = hasAccessibility,
                        onRequest = {
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            context.startActivity(intent)
                        }
                    )

                    PermissionRowItem(
                        title = "Phone call control Permission",
                        description = "Answers or ends telephone calls from the pill directly.",
                        isGranted = hasPhone,
                        onRequest = onRequestPhone
                    )
                }
            }
        }

        // Quick Controls Large Cards Stacked Vertically
        Text("Quick Controls", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Enable Nova Bar
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val checked = !settings.isEnabled
                        viewModel.setEnabled(checked)
                        val intent = Intent(context, OverlayService::class.java)
                        if (checked) {
                            if (hasOverlay) {
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
                    },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (settings.isEnabled) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        painter = painterResource(id = NovaIcons.Overlay),
                        contentDescription = "Enable Nova Bar",
                        tint = if (settings.isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Enable Nova Bar",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = if (settings.isEnabled) "Engine Active" else "Engine Stopped",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            // Status Bar Sync (Follow Status Bar Visibility)
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        viewModel.setFollowStatusBarVisibility(!settings.followStatusBarVisibility)
                    },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (settings.followStatusBarVisibility) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        painter = painterResource(id = NovaIcons.StatusSync),
                        contentDescription = "Status Bar Sync",
                        tint = if (settings.followStatusBarVisibility) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(24.dp)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Status Bar Sync",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = if (settings.followStatusBarVisibility) "Auto Hide Active" else "Always Visible",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        // System Health Checklist
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("System Health Checklist", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                HealthCheckRow(label = "Overlay Window Host", status = hasOverlay)
                HealthCheckRow(label = "Notification Listener Connected", status = hasNotification)
                HealthCheckRow(label = "Accessibility Helper Engine", status = hasAccessibility)
            }
        }
    }
}

@Composable
fun PermissionRowItem(
    title: String,
    description: String,
    isGranted: Boolean,
    onRequest: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(if (isGranted) Color(0xFF4CAF50) else Color(0xFFFF5252))
                )
                Text(title, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }
            Text(description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
        }
        if (!isGranted) {
            Button(
                onClick = onRequest,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                modifier = Modifier.height(28.dp)
            ) {
                Text("Grant", fontSize = 10.sp)
            }
        } else {
            Text("Active", fontSize = 11.sp, color = Color(0xFF4CAF50), fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun HealthCheckRow(label: String, status: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
        Text(
            text = if (status) "ONLINE" else "OFFLINE",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = if (status) Color(0xFF4CAF50) else Color(0xFFFF5252)
        )
    }
}

// ==========================================
// 2. APPEARANCE STUDIO
// ==========================================
@Composable
fun AppearanceStudioScreen(viewModel: SettingsViewModel, settings: NovaSettings) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Dynamic Live Preview
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Live Studio Preview",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Simulated Phone Notch Area
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .background(Color.Black, RoundedCornerShape(12.dp))
                        .padding(8.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    // Camera Notch Dot
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1A1A1A))
                            .align(Alignment.TopCenter)
                    )

                    // Simulated Nova Bar capsule representing gravity and position
                    val barWidthScale = settings.barWidthScale
                    val barGravity = settings.barGravity
                    val opacity = settings.opacity
                    val cornerRadius = settings.cornerRadius
                    val cutoutMode = settings.cameraCutoutMode
                    val leftW = settings.leftSegmentWidthDp
                    val rightW = settings.rightSegmentWidthDp

                    if (!cutoutMode) {
                        // Standard Capsule
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(barWidthScale * 0.7f)
                                .height((26 + settings.barHeightPadding / 2).dp.coerceAtLeast(16.dp))
                                .align(
                                    when (barGravity) {
                                        "Left" -> Alignment.TopStart
                                        "Right" -> Alignment.TopEnd
                                        else -> Alignment.TopCenter
                                    }
                                )
                                .offset(
                                    x = (settings.offsetX / 12).dp,
                                    y = ((settings.offsetY.coerceIn(0, 100)) / 10).dp
                                )
                                .background(
                                    color = Color.White.copy(alpha = opacity),
                                    shape = RoundedCornerShape(cornerRadius.dp)
                                )
                                .border(1.dp, Color.White.copy(alpha = 0.3f), RoundedCornerShape(cornerRadius.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Nova Bar Preview",
                                color = Color.Black,
                                fontSize = (8f + settings.pillTextSize).sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1
                            )
                        }
                    } else {
                        // Split Cutout layout
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .offset(y = ((settings.offsetY.coerceIn(0, 100)) / 10).dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .width((leftW / 6).dp)
                                    .height((24 + settings.barHeightPadding / 2).dp.coerceAtLeast(16.dp))
                                    .background(
                                        color = Color.White.copy(alpha = opacity),
                                        shape = RoundedCornerShape(cornerRadius.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Left", color = Color.Black, fontSize = (7f + settings.pillTextSize).sp, fontWeight = FontWeight.Bold)
                            }

                            Spacer(modifier = Modifier.width(20.dp))

                            Box(
                                modifier = Modifier
                                    .width((rightW / 6).dp)
                                    .height((24 + settings.barHeightPadding / 2).dp.coerceAtLeast(16.dp))
                                    .background(
                                        color = Color.White.copy(alpha = opacity),
                                        shape = RoundedCornerShape(cornerRadius.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("Right", color = Color.Black, fontSize = (7f + settings.pillTextSize).sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }

        // Layout Customizations Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Layout & Sizing", fontSize = 15.sp, fontWeight = FontWeight.Bold)

                ToggleSetting(
                    title = "Camera Cutout Layout",
                    description = "Split capsule around front camera punch-holes.",
                    checked = settings.cameraCutoutMode,
                    onCheckedChange = { viewModel.setCameraCutoutMode(it) }
                )

                if (settings.cameraCutoutMode) {
                    SliderSetting(
                        title = "Camera Cutout Size Scale: ${String.format(java.util.Locale.US, "%.1fx", settings.cameraCutoutGapScale)}",
                        value = settings.cameraCutoutGapScale,
                        valueRange = 0.7f..2.0f,
                        onValueChange = { viewModel.setCameraCutoutGapScale(it) }
                    )
                    SliderSetting(
                        title = "Left Segment Size: ${settings.leftSegmentWidthDp}dp",
                        value = settings.leftSegmentWidthDp.toFloat(),
                        valueRange = 60f..240f,
                        onValueChange = { viewModel.setLeftSegmentWidthDp(it.toInt()) }
                    )
                    SliderSetting(
                        title = "Right Segment Size: ${settings.rightSegmentWidthDp}dp",
                        value = settings.rightSegmentWidthDp.toFloat(),
                        valueRange = 60f..240f,
                        onValueChange = { viewModel.setRightSegmentWidthDp(it.toInt()) }
                    )
                } else {
                    SliderSetting(
                        title = "Pill Width Scale: ${String.format("%.2fx", settings.barWidthScale)}",
                        value = settings.barWidthScale.coerceIn(0.5f..1.5f),
                        valueRange = 0.5f..1.5f,
                        onValueChange = { viewModel.setBarWidthScale(it) }
                    )
                }

                SliderSetting(
                    title = "Height Adjustment: ${settings.barHeightPadding}dp",
                    value = settings.barHeightPadding.toFloat().coerceIn(-30f..40f),
                    valueRange = -30f..40f,
                    onValueChange = { viewModel.setBarHeightPadding(it.toInt()) }
                )

                SliderSetting(
                    title = "Pill Text Size: ${if (settings.pillTextSize == 0f) "Default (0)" else if (settings.pillTextSize > 0) "+${settings.pillTextSize.toInt()}" else settings.pillTextSize.toInt().toString()}",
                    value = settings.pillTextSize.coerceIn(-4f..6f),
                    valueRange = -4f..6f,
                    onValueChange = { viewModel.setPillTextSize(it) }
                )

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

                if (!settings.cameraCutoutMode) {
                    Text("Pill Horizontal Alignment", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
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
                                Text(gravity, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
        }

        // Aesthetics Settings Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text("Styling & Effects", fontSize = 15.sp, fontWeight = FontWeight.Bold)

                SliderSetting(
                    title = "Corner Radius: ${settings.cornerRadius}dp",
                    value = settings.cornerRadius.toFloat(),
                    valueRange = 12f..36f,
                    onValueChange = { viewModel.setCornerRadius(it.toInt()) }
                )

                SliderSetting(
                    title = "Transparency Opacity: ${(settings.opacity * 100).toInt()}%",
                    value = settings.opacity,
                    valueRange = 0.2f..1.0f,
                    onValueChange = { viewModel.setOpacity(it) }
                )



                ToggleSetting(
                    title = "Dynamic Luminance Adaptation",
                    description = "Adapts text color depending on wallpaper brightness.",
                    checked = settings.colorAdaptationEnabled,
                    onCheckedChange = { viewModel.setColorAdaptationEnabled(it) }
                )
            }
        }
    }
}

// ==========================================
// 3. ACTIVITY MANAGER
// ==========================================
@Composable
fun ActivityManagerScreen(viewModel: SettingsViewModel, settings: NovaSettings) {
    val scrollState = rememberScrollState()
    var selectedActivityDetail by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Live Activity Modules",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Text(
            text = "Select any activity module to access specific customization features.",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
        )

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            val activeActivitiesList by OverlayStateManager.activeActivities.collectAsState(initial = emptyList())
            val isMediaActive = activeActivitiesList.any { it is OverlayState.Media }
            val isNavActive = activeActivitiesList.any { it is OverlayState.Navigation }
            val isTimerActive = activeActivitiesList.any { it is OverlayState.Timer }
            val isStopwatchActive = activeActivitiesList.any { it is OverlayState.Stopwatch }
            val isChargingActive = activeActivitiesList.any { it is OverlayState.Charging }
            val isNotificationActive = activeActivitiesList.any { it is OverlayState.Notification }

            val torchState by OverlayStateManager.torchState.collectAsState()
            val isTorchActive = torchState != null
            val torchStrength = torchState?.brightnessPercentage ?: 0

            val hotspotState by OverlayStateManager.hotspotState.collectAsState()
            val isHotspotActive = hotspotState?.isActive == true

            ActivityCardItem(
                title = "Media Playback Controls",
                desc = "Track music, show visualizer, controls.",
                iconRes = NovaIcons.Media,
                isEnabled = settings.mediaControlsEnabled,
                statusText = if (isMediaActive) "Active (Playing)" else "Inactive"
            ) {
                selectedActivityDetail = "media"
            }
            ActivityCardItem(
                title = "Maps Navigation Maneuvers",
                desc = "Clean glanceable arrow directions.",
                iconRes = NovaIcons.Navigation,
                isEnabled = settings.navigationEnabled,
                statusText = if (isNavActive) "Active (Navigating)" else "Inactive"
            ) {
                selectedActivityDetail = "navigation"
            }
            ActivityCardItem(
                title = "Ongoing Timer Clocks",
                desc = "Track countdown and remaining times.",
                iconRes = NovaIcons.Timer,
                isEnabled = settings.timerEnabled,
                statusText = if (isTimerActive) "Active (Running)" else "Inactive"
            ) {
                selectedActivityDetail = "timer"
            }
            ActivityCardItem(
                title = "Stopwatch Elapsed Time",
                desc = "Displays live active stopwatches.",
                iconRes = NovaIcons.Stopwatch,
                isEnabled = settings.stopwatchEnabled,
                statusText = if (isStopwatchActive) "Active (Running)" else "Inactive"
            ) {
                selectedActivityDetail = "stopwatch"
            }
            ActivityCardItem(
                title = "Battery Charging Alert",
                desc = "Presents metrics when battery is plugged in.",
                iconRes = NovaIcons.Charging,
                isEnabled = settings.chargingEnabled,
                statusText = if (isChargingActive) "Active (Charging)" else "Inactive"
            ) {
                selectedActivityDetail = "charging"
            }
            ActivityCardItem(
                title = "Device Torch Mode",
                desc = "Toggles status and controls flashlight capsule.",
                iconRes = NovaIcons.Torch,
                isEnabled = settings.torchEnabled,
                statusText = if (isTorchActive) "Active ($torchStrength%)" else "Inactive"
            ) {
                selectedActivityDetail = "torch"
            }
            ActivityCardItem(
                title = "Wi-Fi Hotspot Broadcast",
                desc = "Monitors tethering session states.",
                iconRes = NovaIcons.Hotspot,
                isEnabled = settings.hotspotEnabled,
                statusText = if (isHotspotActive) "Active" else "Inactive"
            ) {
                selectedActivityDetail = "hotspot"
            }
            ActivityCardItem(
                title = "Heads-up Notifications",
                desc = "Shows notification bubbles in capsule.",
                iconRes = NovaIcons.Notifications,
                isEnabled = settings.notificationsEnabled,
                statusText = if (isNotificationActive) "Active (Alert)" else "Inactive"
            ) {
                selectedActivityDetail = "notifications"
            }
        }
    }

    if (selectedActivityDetail != null) {
        val torchState by OverlayStateManager.torchState.collectAsState()
        val isTorchActive = torchState != null
        val torchStrength = torchState?.brightnessPercentage ?: 0



        AlertDialog(
            onDismissRequest = { selectedActivityDetail = null },
            title = {
                Text(
                    text = when (selectedActivityDetail) {
                        "media" -> "Media Playback Settings"
                        "navigation" -> "Navigation Settings"
                        "timer" -> "Timer Settings"
                        "stopwatch" -> "Stopwatch Settings"
                        "charging" -> "Charging Settings"
                        "torch" -> "Torch Flashlight Settings"
                        "hotspot" -> "Wi-Fi Hotspot Settings"
                        else -> "Notification Settings"
                    },
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    when (selectedActivityDetail) {
                        "media" -> {
                            ToggleSetting(
                                title = "Enable Media Module",
                                description = "Monitor background audio sessions.",
                                checked = settings.mediaControlsEnabled,
                                onCheckedChange = { viewModel.setMediaEnabled(it) }
                            )

                            Divider()

                            Text("Visualizer Style", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("Waveform", "Pulse", "Minimal").forEach { style ->
                                    val isSelected = settings.visualizerStyle == style
                                    Button(
                                        onClick = { viewModel.setVisualizerStyle(style) },
                                        modifier = Modifier.weight(1f),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer,
                                            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    ) {
                                        Text(style, fontSize = 10.sp)
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
                                title = "Show Playback Progress Bar",
                                description = "Displays linear progress bar under music labels.",
                                checked = settings.progressVisibility,
                                onCheckedChange = { viewModel.setProgressVisibility(it) }
                            )
                        }
                        "navigation" -> {
                            ToggleSetting(
                                title = "Enable Navigation Module",
                                description = "Show upcoming maneuver arrow icons & distances.",
                                checked = settings.navigationEnabled,
                                onCheckedChange = { viewModel.setNavigationEnabled(it) }
                            )
                        }
                        "timer" -> {
                            ToggleSetting(
                                title = "Enable Timer Module",
                                description = "Exposes countdown status on status bar.",
                                checked = settings.timerEnabled,
                                onCheckedChange = { viewModel.setTimerEnabled(it) }
                            )
                        }
                        "stopwatch" -> {
                            ToggleSetting(
                                title = "Enable Stopwatch Module",
                                description = "Renders ongoing elapsed timings.",
                                checked = settings.stopwatchEnabled,
                                onCheckedChange = { viewModel.setStopwatchEnabled(it) }
                            )
                        }
                        "charging" -> {
                            ToggleSetting(
                                title = "Enable Charging Module",
                                description = "Injects brief power connection stats.",
                                checked = settings.chargingEnabled,
                                onCheckedChange = { viewModel.setChargingEnabled(it) }
                            )
                        }
                        "torch" -> {
                            ToggleSetting(
                                title = "Enable Torch Module",
                                description = "Allows Nova Bar to display Torch status inside the pill.",
                                checked = settings.torchEnabled,
                                onCheckedChange = { viewModel.setTorchEnabled(it) }
                            )
                            if (isTorchActive) {
                                Text(
                                    text = "Brightness Level: $torchStrength%",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                        "hotspot" -> {
                            ToggleSetting(
                                title = "Enable Hotspot Module",
                                description = "Allows Nova Bar to display Wi-Fi Hotspot status inside the pill.",
                                checked = settings.hotspotEnabled,
                                onCheckedChange = { viewModel.setHotspotEnabled(it) }
                            )
                        }
                        "notifications" -> {
                            ToggleSetting(
                                title = "Enable Notification Banner",
                                description = "Displays heads-up popup banners in pill.",
                                checked = settings.notificationsEnabled,
                                onCheckedChange = { viewModel.setNotificationsEnabled(it) }
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { selectedActivityDetail = null }) {
                    Text("Done")
                }
            }
        )
    }
}

@Composable
fun ActivityCardItem(
    title: String,
    desc: String,
    iconRes: Int,
    isEnabled: Boolean,
    statusText: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = title,
                tint = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(28.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                Text(desc, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Status: $statusText",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isEnabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isEnabled) Color(0xFF4CAF50) else Color(0xFFFF5252))
            )
        }
    }
}

// ==========================================
// 4. GENERAL SETTINGS
// ==========================================
@Composable
fun GeneralSettingsScreen(viewModel: SettingsViewModel, settings: NovaSettings) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("General Settings", fontSize = 18.sp, fontWeight = FontWeight.Bold)

        // General System configurations
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Sizing & Presentation", fontSize = 14.sp, fontWeight = FontWeight.Bold)

                Text("Default Presentation Mode", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                            Text(mode, fontSize = 11.sp, maxLines = 1)
                        }
                    }
                }

                Text("Overlay Position", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
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

                Text("Overlay Engine", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(OverlayEngine.APPLICATION, OverlayEngine.ACCESSIBILITY).forEach { engine ->
                        val selected = settings.overlayEngine == engine
                        val label = when (engine) {
                            OverlayEngine.APPLICATION -> "App Layer"
                            OverlayEngine.ACCESSIBILITY -> "A11y Layer"
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
            }
        }

        // Time Customizations Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Text("Clocks & Idle states", fontSize = 14.sp, fontWeight = FontWeight.Bold)

                ToggleSetting(
                    title = "Show Clock Pill when Idle",
                    description = "Displays clock when no activities are active.",
                    checked = settings.showWhenIdle,
                    onCheckedChange = { viewModel.setShowWhenIdle(it) }
                )

                Text("Time Format", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
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
                    description = "Expose clock seconds across layouts.",
                    checked = settings.showSeconds,
                    onCheckedChange = { viewModel.setShowSeconds(it) }
                )

                ToggleSetting(
                    title = "Show on Lockscreen",
                    description = "Allow overlay on top of lockscreens.",
                    checked = settings.showOnLockscreen,
                    onCheckedChange = { viewModel.setShowOnLockscreen(it) }
                )

                ToggleSetting(
                    title = "Always On Bar Mode",
                    description = "Minimal battery/clock bar when idle.",
                    checked = settings.alwaysOnBar,
                    onCheckedChange = { viewModel.setAlwaysOnBar(it) }
                )

                if (settings.alwaysOnBar) {
                    Text("Always On Layout", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
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
                                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(cfg, fontSize = 9.sp, maxLines = 1)
                            }
                        }
                    }
                }
            }
        }

        // Backup & Clipboard Configuration Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Backup & Configuration", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
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
                        Text("Export Config", fontSize = 12.sp)
                    }

                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val item = clipboard.primaryClip?.getItemAt(0)
                            val text = item?.text?.toString() ?: ""
                            if (text.startsWith("NovaBarSettingsV1:") || text.startsWith("NovaBarSettingsV2:") || text.startsWith("NovaBarSettingsV3:") || text.startsWith("NovaBarSettingsV4:") || text.startsWith("NovaBarSettingsV5:") || text.startsWith("NovaBarSettingsV6:")) {
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
                        Text("Import Config", fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

// ==========================================
// 5. DEVELOPER DIAGNOSTICS & LOGS
// ==========================================
@Composable
fun DeveloperScreen(
    context: Context,
    viewModel: SettingsViewModel,
    settings: NovaSettings,
    hasNotificationPermission: Boolean,
    hasAccessibilityPermission: Boolean,
    hasOverlayPermission: Boolean
) {
    val scrollState = rememberScrollState()
    val debugModeEnabled = settings.debugModeEnabled

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Enable Debug Mode", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Enables advanced developer diagnostics, real-time overlay metrics, testing simulators, and verbose system logging.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Switch(
                checked = debugModeEnabled,
                onCheckedChange = { viewModel.setDebugModeEnabled(it) }
            )
        }

        if (debugModeEnabled) {
            Text("Developer Diagnostics Console", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)

            // Real-time parameter stateflows from DiagnosticsManager
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

            // Diagnostics Cards Grouped visually
            DiagnosticsSection(title = "Engine & Host Parameters") {
                DiagnosticsRow("Overlay Engine", overlayEngine)
                DiagnosticsRow("Window Layout Class", windowType)
                DiagnosticsRow("Touchable State", touchableState)
                DiagnosticsRow("Current Active Module", currentActivity)
                DiagnosticsRow("Presentation state", currentPresentationState)
                DiagnosticsRow("Hardware Blur Active", blurEnabled.toString())
                DiagnosticsRow("Blur Render Engine", blurBackend)
                DiagnosticsRow("Canvas Bounds", "${windowWidth}x${windowHeight} px")
                DiagnosticsRow("Cumulative Touches", touchEventsReceived.toString())
                DiagnosticsRow("Overlay System Permission", if (hasOverlayPermission) "GRANTED" else "DENIED")
            }

            DiagnosticsSection(title = "Camera Cutout Metrics") {
                DiagnosticsRow("Notch cutout detected", cutoutHasCutout.toString())
                DiagnosticsRow("Cutout bounding width", "$cutoutWidthDiag px")
                DiagnosticsRow("Notch Center position X", "$cutoutCenterXDiag px")
                DiagnosticsRow("Cutout split mode", cutoutModeEnabled.toString())
                DiagnosticsRow("Spacing multiplier scale", "${String.format(java.util.Locale.US, "%.2f", cutoutGapScale)}x")
                DiagnosticsRow("Resulting gap padding", "$finalGapWidth px")
            }

            DiagnosticsSection(title = "Media Session Pipeline") {
                DiagnosticsRow("Active session active", mediaSessionActive.toString())
                DiagnosticsRow("Audio player source app", mediaAppName)
                DiagnosticsRow("Package target signature", mediaPackageName)
                DiagnosticsRow("Playback state enum", mediaPlaybackState)
                DiagnosticsRow("Track metadata title", mediaTrackTitle)
                DiagnosticsRow("Track metadata artist", mediaArtist)
                DiagnosticsRow("Stream progress percentage", mediaProgress)
                DiagnosticsRow("Active Remote controls", mediaControlsAvailable)
                DiagnosticsRow("Pipeline update timestamp", lastMediaUpdateTimestamp)
            }

            DiagnosticsSection(title = "Notification Manager Receiver") {
                DiagnosticsRow("Receiver listener bound", notificationListenerConnected.toString())
                DiagnosticsRow("Notification provider name", lastNotificationApp)
                DiagnosticsRow("Notification package ID", lastNotificationPackage)
                DiagnosticsRow("Active alert listings", notificationCount.toString())
                DiagnosticsRow("Permission connection flag", if (hasNotificationPermission) "GRANTED" else "DENIED")
            }

            DiagnosticsSection(title = "Accessibility Engine Diagnostics") {
                DiagnosticsRow("A11y service running", if (hasAccessibilityPermission) "ENABLED" else "DISABLED")
                DiagnosticsRow("A11y Window overlay attached", accessibilityOverlayActive.toString())
                DiagnosticsRow("Recent trigger event", lastAccessibilityEvent)
                DiagnosticsRow("Foreground Package listener", accessibilityForegroundPackage)
            }

            DiagnosticsSection(title = "Window Manager Nodes") {
                DiagnosticsRow("Overlay shown state", overlayVisible.toString())
                DiagnosticsRow("View attached to window", overlayAttached.toString())
                DiagnosticsRow("Overlay container bounds", currentOverlayBounds)
                DiagnosticsRow("Scheduler active modules", currentPriorityActivity)
            }

            // Developer Actions Grid
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Diagnostics Debug Simulator", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                Toast.makeText(context, "Parameters refreshed!", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text("Refresh", fontSize = 10.sp, maxLines = 1)
                        }

                        Button(
                            onClick = {
                                com.novabar.app.domain.OverlayStateManager.expand()
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text("Force Expand", fontSize = 10.sp, maxLines = 1)
                        }

                        Button(
                            onClick = {
                                com.novabar.app.domain.OverlayStateManager.collapse()
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text("Force Collapse", fontSize = 10.sp, maxLines = 1)
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                DiagnosticsManager.resetTrigger.value += 1
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text("Rebuild View", fontSize = 10.sp, maxLines = 1)
                        }

                        Button(
                            onClick = {
                                com.novabar.app.domain.OverlayStateManager.updateNotification(
                                    com.novabar.app.domain.NotificationState(
                                        id = "test_diagnostics_id",
                                        packageName = "com.novabar.app",
                                        appName = "Nova Bar Test",
                                        title = "Diagnostics Simulator",
                                        summary = "Active simulated overlay alert is running."
                                    )
                                )
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text("Inject Alert", fontSize = 10.sp, maxLines = 1)
                        }

                        Button(
                            onClick = {
                                com.novabar.app.domain.OverlayStateManager.mediaState.value = 
                                    com.novabar.app.domain.MediaState(
                                        isPlaying = true,
                                        title = "Citizen - Sun Kisses",
                                        artist = "Citizen",
                                        progress = 0.59f,
                                        duration = 238000L,
                                        position = 141000L,
                                        albumArt = null,
                                        appName = "YouTube"
                                    )
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text("Inject Music", fontSize = 10.sp, maxLines = 1)
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                com.novabar.app.domain.OverlayStateManager.mediaState.value = null
                                com.novabar.app.domain.OverlayStateManager.phoneCallState.value = null
                                com.novabar.app.domain.OverlayStateManager.dismissNotification()
                                com.novabar.app.domain.OverlayStateManager.setTimerState(null)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text("Clear Debug Simulator states", fontSize = 10.sp, maxLines = 1)
                        }
                    }
                }
            }

            // Developer logging
            val isLoggingEnabledState by DeveloperLogger.isLoggingEnabled.collectAsState()
            
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Developer Diagnostic Logs", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Record Debug Logs", fontSize = 12.sp)
                        Switch(
                            checked = isLoggingEnabledState,
                            onCheckedChange = { checked ->
                                DeveloperLogger.setLoggingEnabled(context, checked)
                            }
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val logContent = DeveloperLogger.readLog(context)
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("Nova Bar Debug Log", logContent))
                                Toast.makeText(context, "Logs copied to clipboard!", Toast.LENGTH_SHORT).show()
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
                                        Toast.makeText(context, "Log file is empty", Toast.LENGTH_SHORT).show()
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
                                    Toast.makeText(context, "Failed to share log", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text("Share Log", fontSize = 10.sp, maxLines = 1)
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
            }
        }
    }
}

// ==========================================
// UTILITY COMPONENTS & HELPERS
// ==========================================
@Composable
fun DiagnosticsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(title, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
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
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        Text(value, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
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
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.Bold)
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
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
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

private fun exportSettings(s: NovaSettings): String {
    val pkgs = s.allowedNotificationPackages.joinToString("|")
    return "NovaBarSettingsV6:${s.isEnabled},${s.positionY},${s.cornerRadius},${s.opacity},25,1.0,${s.mediaControlsEnabled},${s.timerEnabled},${s.stopwatchEnabled},${s.navigationEnabled},${s.chargingEnabled},${s.notificationsEnabled},${s.colorAdaptationEnabled},${pkgs},${s.barWidthScale},${s.barHeightPadding},${s.barBorderThickness},${s.barGravity},${s.offsetX},${s.offsetY},${s.showWhenIdle},${s.defaultPresentationMode},${s.visualizerStyle},${s.visualizerSensitivity},${s.albumArtCornerRadius},${s.progressVisibility},${s.autoCollapseTimeout},${s.textSize},${s.overlayPosition},${s.alwaysOnBar},${s.alwaysOnConfig},${s.timeFormat},${s.showSeconds},${s.showOnLockscreen},${s.cameraCutoutMode},${s.cameraCutoutGapScale},${s.leftSegmentWidthDp},${s.rightSegmentWidthDp},${s.pillTextSize}"
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
                rightSegmentWidthDp = if (parts.size > 37) parts[37].toInt() else 120,
                pillTextSize = if (parts.size > 38) parts[38].toFloat() else 0.0f
            )
        } else if (str.startsWith("NovaBarSettingsV5:")) {
            val parts = str.substringAfter("NovaBarSettingsV5:").split(",")
            val pkgs = parts[13].split("|").filter { it.isNotEmpty() }.toSet()
            NovaSettings(
                isEnabled = parts[0].toBoolean(),
                positionY = parts[1].toInt(),
                cornerRadius = parts[2].toInt(),
                opacity = parts[3].toFloat(),
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
