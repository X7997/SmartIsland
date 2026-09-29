/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.ui

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.agupta07505.smartisland.util.OemAutostartUtil
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.AvTimer
import androidx.compose.material.icons.rounded.BatteryChargingFull
import androidx.compose.material.icons.rounded.BluetoothConnected
import androidx.compose.material.icons.rounded.Call
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ColorLens
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FitnessCenter
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.FlashlightOn
import androidx.compose.material.icons.rounded.Gesture
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.HourglassBottom
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lightbulb
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Navigation
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.WifiTethering
import com.agupta07505.smartisland.ui.components.FemaleMuscleAnatomyDualView
import com.agupta07505.smartisland.ui.components.MuscleMatcher
import com.agupta07505.smartisland.ui.components.TargetMuscle
import com.agupta07505.smartisland.ui.components.MuscleCategory
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.data.INotificationRepository
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository
import com.agupta07505.smartisland.di.SmartIslandRepositories
import com.agupta07505.smartisland.model.IslandMode
import com.agupta07505.smartisland.ui.sections.AboutSection
import com.agupta07505.smartisland.ui.sections.AppShortcutsSection
import com.agupta07505.smartisland.ui.sections.CustomizationsSection
import com.agupta07505.smartisland.ui.sections.GesturesSection
import com.agupta07505.smartisland.ui.sections.NotificationHistorySection
import com.agupta07505.smartisland.ui.sections.NotificationsAndPrivacySection
import com.agupta07505.smartisland.ui.sections.PermissionsSection
import com.agupta07505.smartisland.ui.sections.PositionsSection
import com.agupta07505.smartisland.ui.sections.SupportSection
import com.agupta07505.smartisland.util.SystemServiceRecovery
import com.agupta07505.smartisland.util.runCatchingLogged
import kotlinx.coroutines.launch

private enum class StudioTab {
    Studio,
    Position,
    Settings
}

private enum class FeatureDetailSection {
    NotificationRules,
    AppShortcuts,
    NotificationHistory,
    ColorStudio,
    GesturesGuide,
    PermissionsCenter,
    AboutApp,
    SupportCommunity
}

@SuppressLint("BatteryLife")
@Composable
fun SmartIslandHomeScreen(
    repository: SmartIslandSettingsRepository? = null,
    notificationRepository: INotificationRepository? = null
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val resolvedRepository = remember(repository, context) {
        repository ?: runCatching {
            SmartIslandRepositories.settingsRepository(context)
        }.getOrElse {
            SmartIslandSettingsRepository(context.applicationContext)
        }
    }
    val resolvedNotificationRepository = remember(notificationRepository, context) {
        notificationRepository ?: runCatching {
            SmartIslandRepositories.notificationRepository(context)
        }.getOrNull()
    }

    val settings by resolvedRepository.settings.collectAsStateWithLifecycle(initialValue = SmartIslandSettings.Default)
    val scope = rememberCoroutineScope()

    var showWelcomeDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        if (!resolvedRepository.isWelcomeDialogShown()) {
            showWelcomeDialog = true
        }
    }

    if (showWelcomeDialog) {
        WelcomeDialog(
            onDismiss = {
                showWelcomeDialog = false
                scope.launch { resolvedRepository.setWelcomeDialogShown(true) }
            },
            onStarClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/agupta07505/SmartIsland"))
                runCatching { context.startActivity(intent) }
            },
            onJoinCommunityClick = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://telegram.me/SmartIslandApp"))
                runCatching { context.startActivity(intent) }
            }
        )
    }

    var overlayGranted by remember { mutableStateOf(Settings.canDrawOverlays(context)) }
    var notificationGranted by remember { mutableStateOf(isNotificationListenerEnabled(context)) }
    var batteryIgnored by remember { mutableStateOf(isBatteryOptimizationIgnored(context)) }
    var accessibilityGranted by remember {
        mutableStateOf(
            com.agupta07505.smartisland.service.SmartIslandOverlayService.isSystemConnected ||
                SystemServiceRecovery.isAccessibilityPermissionGranted(context)
        )
    }

    DisposableEffect(lifecycleOwner, context) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                overlayGranted = Settings.canDrawOverlays(context)
                notificationGranted = isNotificationListenerEnabled(context)
                batteryIgnored = isBatteryOptimizationIgnored(context)
                accessibilityGranted = com.agupta07505.smartisland.service.SmartIslandOverlayService.isSystemConnected ||
                    SystemServiceRecovery.isAccessibilityPermissionGranted(context)
                SystemServiceRecovery.requestRecovery(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var selectedTab by remember { mutableStateOf(StudioTab.Studio) }
    var activeDetailSection by remember { mutableStateOf<FeatureDetailSection?>(null) }
    var transitionDirection by remember { mutableStateOf(1) } // 1 = forward, -1 = backward

    // Active preview mode for interactive live preview
    var previewMode by remember { mutableStateOf(IslandMode.Music) }

    val canEnable = (overlayGranted || accessibilityGranted) && notificationGranted && batteryIgnored

    BackHandler(enabled = activeDetailSection != null) {
        transitionDirection = -1
        activeDetailSection = null
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        bottomBar = {
            if (activeDetailSection == null) {
                StudioBottomNavigationBar(
                    selectedTab = selectedTab,
                    onTabSelected = { tab ->
                        transitionDirection = if (tab.ordinal > selectedTab.ordinal) 1 else -1
                        selectedTab = tab
                    }
                )
            }
        }
    ) { scaffoldPadding ->
        AnimatedContent(
            targetState = activeDetailSection,
            modifier = Modifier.padding(scaffoldPadding),
            transitionSpec = {
                if (transitionDirection == 1) {
                    (slideInHorizontally(initialOffsetX = { it }) + fadeIn())
                        .togetherWith(slideOutHorizontally(targetOffsetX = { -it }) + fadeOut())
                } else {
                    (slideInHorizontally(initialOffsetX = { -it }) + fadeIn())
                        .togetherWith(slideOutHorizontally(targetOffsetX = { it }) + fadeOut())
                }
            },
            label = "ScreenTransition"
        ) { detailSection ->
            if (detailSection == null) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                        .verticalScroll(rememberScrollState())
                        .padding(
                            start = 20.dp,
                            end = 20.dp,
                            top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 12.dp,
                            bottom = 32.dp
                        ),
                    verticalArrangement = Arrangement.spacedBy(18.dp)
                ) {
                    // Studio Top Header
                    StudioTopHeader(
                        isIslandEnabled = settings.enabled,
                        canEnable = canEnable,
                        onHealthClick = {
                            transitionDirection = 1
                            activeDetailSection = FeatureDetailSection.PermissionsCenter
                        }
                    )

                    when (selectedTab) {
                        StudioTab.Studio -> {
                            // 1. Master Power Switch Card
                            MasterPowerCard(
                                enabled = settings.enabled,
                                canEnable = canEnable,
                                onCheckedChange = { turnOn ->
                                    if (turnOn) {
                                        com.agupta07505.smartisland.service.SmartIslandOverlayService.wakeUpOverlaySession(context)
                                        SystemServiceRecovery.requestRecovery(context)
                                    } else {
                                        com.agupta07505.smartisland.service.SmartIslandOverlayService.stopOverlaySessionFromUser(context)
                                    }
                                    scope.launch { resolvedRepository.setEnabled(turnOn) }
                                },
                                onSetupPermissionsClick = {
                                    transitionDirection = 1
                                    activeDetailSection = FeatureDetailSection.PermissionsCenter
                                }
                            )

                            // 2. Keep-Alive & Anti-Kill Guide Card
                            KeepAliveProtectionCard()

                            // 3. Fitness Companion Mode Card
                            FitnessCompanionCard()

                            // 3. Interactive Simulation Lab
                            SimulationLabCard(
                                activeMode = previewMode,
                                onModeSelect = { mode ->
                                    previewMode = mode
                                    resolvedNotificationRepository?.showDemo(mode)
                                },
                                onClearAll = {
                                    resolvedNotificationRepository?.clearTestNotifications()
                                    Toast.makeText(context, context.getString(R.string.toast_cleared_test_notifications), Toast.LENGTH_SHORT).show()
                                }
                            )

                            // 3. System Diagnostics Strip
                            DiagnosticsSummaryCard(
                                overlayGranted = overlayGranted,
                                notificationGranted = notificationGranted,
                                batteryIgnored = batteryIgnored,
                                accessibilityGranted = accessibilityGranted,
                                onOpenDiagnostics = {
                                    transitionDirection = 1
                                    activeDetailSection = FeatureDetailSection.PermissionsCenter
                                }
                            )
                        }

                        StudioTab.Position -> {
                            PositionsSection(
                                settings = settings,
                                repository = resolvedRepository
                            )
                        }

                        StudioTab.Settings -> {
                            SettingsOverviewSection(
                                settings = settings,
                                overlayGranted = overlayGranted,
                                notificationGranted = notificationGranted,
                                batteryIgnored = batteryIgnored,
                                onNavigateTo = { section ->
                                    transitionDirection = 1
                                    activeDetailSection = section
                                }
                            )
                        }
                    }

                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.made_by),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            } else {
                DetailScreenHost(
                    section = detailSection,
                    settings = settings,
                    repository = resolvedRepository,
                    overlayGranted = overlayGranted,
                    notificationGranted = notificationGranted,
                    batteryIgnored = batteryIgnored,
                    accessibilityGranted = accessibilityGranted,
                    onBack = {
                        transitionDirection = -1
                        activeDetailSection = null
                    },
                    onRefreshPermissions = {
                        overlayGranted = Settings.canDrawOverlays(context)
                        notificationGranted = isNotificationListenerEnabled(context)
                        batteryIgnored = isBatteryOptimizationIgnored(context)
                        accessibilityGranted = com.agupta07505.smartisland.service.SmartIslandOverlayService.isSystemConnected ||
                            SystemServiceRecovery.isAccessibilityPermissionGranted(context)
                    }
                )
            }
        }
    }
}

@Composable
private fun StudioTopHeader(
    isIslandEnabled: Boolean,
    canEnable: Boolean,
    onHealthClick: () -> Unit
) {
    val context = LocalContext.current
    val appIcon = remember(context) {
        runCatchingLogged("HeaderSection", "Failed to get application icon") {
            val drawable = context.packageManager.getApplicationIcon(context.packageName)
            val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 144
            val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 144
            val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            drawable.setBounds(0, 0, width, height)
            drawable.draw(canvas)
            bitmap.asImageBitmap()
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (appIcon != null) {
                Image(
                    bitmap = appIcon,
                    contentDescription = "Smart Island Logo",
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .background(
                            brush = Brush.linearGradient(
                                listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.secondary)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("SI", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }
            }
            Column {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.app_subtitle),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Live Health Status Badge
        val statusColor = when {
            !canEnable -> Color(0xFFE88C25) // Action required
            isIslandEnabled -> Color(0xFF0F9F6E) // Active
            else -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f) // Ready/Off
        }
        val statusText = when {
            !canEnable -> stringResource(R.string.status_setup_needed)
            isIslandEnabled -> stringResource(R.string.status_active)
            else -> stringResource(R.string.status_ready)
        }

        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(statusColor.copy(alpha = 0.12f))
                .border(1.dp, statusColor.copy(alpha = 0.3f), RoundedCornerShape(20.dp))
                .clickable(onClick = onHealthClick)
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(statusColor, CircleShape)
                )
                Text(
                    text = statusText,
                    color = statusColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun MasterPowerCard(
    enabled: Boolean,
    canEnable: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onSetupPermissionsClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(
                                if (enabled) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FlashOn,
                            contentDescription = null,
                            tint = if (enabled) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Column {
                        Text(
                            text = stringResource(R.string.master_switch_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = if (canEnable) {
                                if (enabled) stringResource(R.string.master_switch_active_desc)
                                else stringResource(R.string.master_switch_ready_desc)
                            } else stringResource(R.string.master_switch_missing_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
                Switch(
                    checked = enabled,
                    enabled = canEnable || enabled,
                    onCheckedChange = onCheckedChange
                )
            }

            if (!canEnable) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    thickness = 1.dp
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onSetupPermissionsClick)
                        .background(Color(0xFFE88C25).copy(alpha = 0.08f))
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Warning,
                            contentDescription = null,
                            tint = Color(0xFFE88C25),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(R.string.btn_grant_required_permissions),
                            color = Color(0xFFE88C25),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun KeepAliveProtectionCard() {
    val context = LocalContext.current
    var isExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, Color(0xFF3B82F6).copy(alpha = 0.3f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { isExpanded = !isExpanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF3B82F6).copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Shield,
                            contentDescription = null,
                            tint = Color(0xFF3B82F6),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "🛡️ 绿色生命周期与后台启停说明",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = "在后台时伴随运行 · 清除后台即刻彻底销毁退出 · 0% CPU 占用与隐私安全",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }
                Text(
                    text = if (isExpanded) "收起 🔼" else "查看 🔽",
                    color = Color(0xFF3B82F6),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

                    Text(
                        text = "💡 绿色设计理念（随退随止 · 零暗中监控）：\n" +
                                "• 【在后台时启动】：只要应用在多任务后台保留（如按 Home 键返回桌面或切换其他 App），灵动岛悬浮窗稳定伴随运行，提供健身、音乐等悬浮交互。\n" +
                                "• 【清除后台即退出】：一旦您在多任务界面上划清除后台，灵动岛立即物理移除屏幕上的悬浮窗与状态栏常驻通知，彻底释放 CPU 与内存，完全不留后台监控，切实保护您的隐私与信息安全！\n" +
                                "• 【配置永久保存】：下次点击 App 图标秒级恢复，您的挖孔位置、宽度、健身计划等所有自定义配置已由本地 DataStore 永久保存，绝不丢失！",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        lineHeight = 16.sp
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                            .padding(10.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "⚙️ 两种使用模式由您决定：",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "🟢 【推荐 · 用完即走模式】：直接使用，不加锁。用完在多任务上划清除后台，灵动岛立刻完全退出，干净无残留，0% CPU 占用。",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 15.sp
                        )
                        Text(
                            text = "🔒 【全天候常驻模式（可选）】：若您希望一键清理多任务时灵动岛也不退出，可在多任务界面长按卡片点击“加锁”，并开启自启动与电池无限制。",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            lineHeight = 15.sp
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                OemAutostartUtil.openAutostartSettings(context)
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Text("⚡ 去设置自启动", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                        OutlinedButton(
                            onClick = {
                                runCatching {
                                    context.startActivity(
                                        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                                            .setData(Uri.parse("package:${context.packageName}"))
                                    )
                                }.onFailure {
                                    context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(vertical = 8.dp)
                        ) {
                            Text("🔋 电池设无限制", fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun FitnessCompanionCard() {
    val context = LocalContext.current
    val fitnessRepo = remember { SmartIslandRepositories.fitnessRepository(context) }
    val plan by fitnessRepo.plan.collectAsState()
    val planSourceInfo by fitnessRepo.planSourceInfo.collectAsState()
    val sessionState by fitnessRepo.sessionState.collectAsState()

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val result = fitnessRepo.importPlanFromUri(uri)
            result.onSuccess { msg ->
                Toast.makeText(context, "✅ $msg", Toast.LENGTH_LONG).show()
            }.onFailure { err ->
                Toast.makeText(context, "❌ 导入失败: ${err.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    var selectedCategory by remember(plan) {
        mutableStateOf(plan.categories.firstOrNull()?.categoryName ?: "背")
    }

    val currentCategory = plan.categories.firstOrNull { it.categoryName == selectedCategory }
        ?: plan.categories.firstOrNull()

    var selectedExerciseIndex by remember(selectedCategory) { mutableStateOf(0) }
    val currentExercise = currentCategory?.exercises?.getOrNull(
        selectedExerciseIndex.coerceIn(0, (currentCategory.exercises.size - 1).coerceAtLeast(0))
    )

    val currentTargetMuscle = remember(currentExercise?.name, selectedCategory, currentExercise?.targetMuscle) {
        MuscleMatcher.match(currentExercise?.name.orEmpty(), selectedCategory, currentExercise?.targetMuscle)
    }
    val themeColor = currentTargetMuscle.category.color

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.5.dp, themeColor.copy(alpha = 0.25f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. 顶部标题栏
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(themeColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.FitnessCenter,
                                contentDescription = null,
                                tint = themeColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "举铁伴侣 · 肌肉与器械训练",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = "一眼掌握目标肌群、训练器械与组数记录",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // 2. 部位分类选择器 (胸-粉红 | 腹-绿 | 臀腿-紫/珊瑚红 | 背-蓝 | 肩-橙)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                val availableCategories = if (plan.categories.isNotEmpty()) {
                    plan.categories.map { it.categoryName }
                } else {
                    listOf("背", "臀腿", "腹", "胸", "肩")
                }

                availableCategories.forEach { catName ->
                    val isSelected = (if (sessionState.isActive) sessionState.categoryName else selectedCategory) == catName
                    val catColor = MuscleMatcher.getCategoryColor(catName)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                if (isSelected) catColor else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            )
                            .clickable(enabled = !sessionState.isActive) {
                                selectedCategory = catName
                                selectedExerciseIndex = 0
                            }
                            .padding(vertical = 9.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = catName,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    }
                }
            }

            // 3. Apple Health 风格：女性人体正反双面解剖肌肉点亮图
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp, horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    FemaleMuscleAnatomyDualView(
                        activeMuscle = currentTargetMuscle,
                        activeCategory = selectedCategory,
                        isDarkTheme = false
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // 目标肌群中文胶囊标签
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(themeColor.copy(alpha = 0.15f))
                                .padding(horizontal = 10.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "目标肌群: ${currentTargetMuscle.displayName}",
                                color = themeColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // 4. 动作切换小标签栏 (点击快速切换动作并联动人体肌群)
            if (currentCategory != null && currentCategory.exercises.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "训练动作选择：",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        currentCategory.exercises.forEachIndexed { index, ex ->
                            val isExSelected = selectedExerciseIndex == index
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isExSelected) themeColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
                                    )
                                    .border(
                                        width = if (isExSelected) 1.5.dp else 0.dp,
                                        color = if (isExSelected) themeColor else Color.Transparent,
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { selectedExerciseIndex = index }
                                    .padding(vertical = 7.dp, horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = ex.name,
                                    fontSize = 11.sp,
                                    fontWeight = if (isExSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isExSelected) themeColor else MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // 5. 核心器械训练卡片 (回答“怎么练”：动作、器械、重量、组数、设置)
            if (currentExercise != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = themeColor.copy(alpha = 0.05f)),
                    border = BorderStroke(1.dp, themeColor.copy(alpha = 0.25f))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // 动作名称 + 器械标签
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = currentExercise.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            val equipName = currentExercise.equipment ?: "专业器械"
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(themeColor.copy(alpha = 0.15f))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = equipName,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = themeColor
                                )
                            }
                        }

                        // 关键训练指标行：重量 | 组数×次数 | 组间休息
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // 重量
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "当前重量",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                val rawWeight = currentExercise.weight.trim()
                                val cleanWeight = if (rawWeight.endsWith(".0")) rawWeight.removeSuffix(".0") else rawWeight
                                val displayWeight = when {
                                    cleanWeight.isBlank() -> "自重/徒手"
                                    cleanWeight.endsWith("kg", ignoreCase = true) || cleanWeight == "空杆" -> cleanWeight
                                    else -> "$cleanWeight kg"
                                }
                                Text(
                                    text = displayWeight,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = themeColor
                                )
                            }

                            // 组数×次数
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "计划做组",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${currentExercise.reps} × ${currentExercise.sets}组",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // 组间休息
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "组间休息",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "1 min",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF9800)
                                )
                            }
                        }

                        // 1. 个人动作注意事项 / 器械调节 (表格第三列总结)
                        if (!currentExercise.setupTips.isNullOrBlank()) {
                            HorizontalDivider(color = themeColor.copy(alpha = 0.15f))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFFFF9800).copy(alpha = 0.08f))
                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "📌 注意事项",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF9800),
                                    modifier = Modifier.padding(top = 1.dp)
                                )
                                Text(
                                    text = currentExercise.setupTips,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }

                        // 2. 部位发力口诀
                        if (!currentCategory.cue.isNullOrBlank() && currentCategory.cue != currentExercise.setupTips) {
                            if (currentExercise.setupTips.isNullOrBlank()) {
                                HorizontalDivider(color = themeColor.copy(alpha = 0.15f))
                            }
                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.padding(horizontal = 2.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Lightbulb,
                                    contentDescription = null,
                                    tint = Color(0xFFFFB300),
                                    modifier = Modifier
                                        .size(15.dp)
                                        .padding(top = 2.dp)
                                )
                                Text(
                                    text = "发力口诀：${currentCategory.cue}",
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // 视频教程按钮 (若存在)
                        if (!currentExercise.videoUrl.isNullOrBlank()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(themeColor.copy(alpha = 0.1f))
                                    .clickable {
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(currentExercise.videoUrl)).apply {
                                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                            }
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "无法唤醒视频播放", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                    .padding(vertical = 6.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Videocam,
                                    contentDescription = null,
                                    tint = themeColor,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "查看标准动作视频演示",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = themeColor
                                )
                            }
                        }
                    }
                }
            }

            // 6. 训练状态与开启按钮
            if (sessionState.isActive) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(themeColor.copy(alpha = 0.12f))
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = "🟢 正在训练: ${sessionState.categoryName}部",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        color = themeColor
                    )
                    Text(
                        text = "当前动作: ${sessionState.currentExercise?.name ?: ""} (第 ${sessionState.currentSet}/${sessionState.totalSets} 组)",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (sessionState.isResting) {
                        Text(
                            text = "⏳ 组间休息: ${sessionState.restSecondsRemaining}s / 60s",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFF9800)
                        )
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFEF4444))
                        .clickable {
                            fitnessRepo.stopWorkout()
                            Toast.makeText(context, "今日训练已结束，灵动岛已收起", Toast.LENGTH_SHORT).show()
                        }
                        .padding(vertical = 13.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "🛑 结束训练",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(themeColor)
                        .clickable {
                            val hasAccessibility = com.agupta07505.smartisland.service.SmartIslandOverlayService.isSystemConnected ||
                                SystemServiceRecovery.isAccessibilityPermissionGranted(context)
                            val hasOverlay = Settings.canDrawOverlays(context)
                            if (!hasOverlay && !hasAccessibility) {
                                Toast.makeText(context, "请先授予 Smart Island 悬浮窗或无障碍权限", Toast.LENGTH_LONG).show()
                                runCatching {
                                    val intent = Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        Uri.parse("package:${context.packageName}")
                                    )
                                    context.startActivity(intent)
                                }.onFailure {
                                    context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
                                }
                                return@clickable
                            }
                            com.agupta07505.smartisland.service.SmartIslandOverlayService.wakeUpOverlaySession(context)
                            fitnessRepo.startWorkout(selectedCategory)
                            Toast.makeText(context, "已开启【$selectedCategory】训练！灵动岛已置顶就绪", Toast.LENGTH_SHORT).show()
                        }
                        .padding(vertical = 13.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "🚀 开始今日【$selectedCategory】训练 (1 min 组间休息)",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // 7. 计划状态与文件导入 / 重置
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                    .padding(horizontal = 12.dp, vertical = 8.dp)
            ) {
                Column {
                    Text(
                        text = "📋 当前健身计划数据",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = planSourceInfo,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier
                        .weight(1.1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF2563EB).copy(alpha = 0.12f))
                        .clickable {
                            filePickerLauncher.launch(
                                arrayOf(
                                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                                    "application/vnd.ms-excel",
                                    "application/json",
                                    "application/octet-stream",
                                    "*/*"
                                )
                            )
                        }
                        .padding(vertical = 10.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.FileDownload,
                        contentDescription = null,
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "📂 导入计划 (.xlsx/.json)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2563EB)
                    )
                }

                Row(
                    modifier = Modifier
                        .weight(0.9f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        .clickable {
                            val result = fitnessRepo.resetToDefaultPlan()
                            Toast.makeText(context, "已恢复内置健身计划: $result", Toast.LENGTH_SHORT).show()
                        }
                        .padding(vertical = 10.dp, horizontal = 8.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "🔄 恢复内置计划",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SimulationLabCard(
    activeMode: IslandMode,
    onModeSelect: (IslandMode) -> Unit,
    onClearAll: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.simulation_lab_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.simulation_lab_desc),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            // Modes Grid in categorized rows
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Row 1: Media & Calls
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ModeChipButton(
                        label = stringResource(R.string.mode_music_player),
                        icon = Icons.Rounded.MusicNote,
                        iconTint = Color(0xFFFF6B9A),
                        isSelected = activeMode == IslandMode.Music,
                        onClick = { onModeSelect(IslandMode.Music) },
                        modifier = Modifier.weight(1f)
                    )
                    ModeChipButton(
                        label = stringResource(R.string.mode_incoming_call),
                        icon = Icons.Rounded.Call,
                        iconTint = Color(0xFF22C55E),
                        isSelected = activeMode == IslandMode.IncomingCall,
                        onClick = { onModeSelect(IslandMode.IncomingCall) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 2: Notifications & Power
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ModeChipButton(
                        label = stringResource(R.string.mode_notification),
                        icon = Icons.Rounded.Notifications,
                        iconTint = Color(0xFF38BDF8),
                        isSelected = activeMode == IslandMode.Notification,
                        onClick = { onModeSelect(IslandMode.Notification) },
                        modifier = Modifier.weight(1f)
                    )
                    ModeChipButton(
                        label = stringResource(R.string.mode_battery_charge),
                        icon = Icons.Rounded.BatteryChargingFull,
                        iconTint = Color(0xFF10B981),
                        isSelected = activeMode == IslandMode.Battery,
                        onClick = { onModeSelect(IslandMode.Battery) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 3: Live Activities & Maps
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ModeChipButton(
                        label = stringResource(R.string.mode_live_activity),
                        icon = Icons.Rounded.Navigation,
                        iconTint = Color(0xFF8B5CF6),
                        isSelected = activeMode == IslandMode.LiveActivity,
                        onClick = { onModeSelect(IslandMode.LiveActivity) },
                        modifier = Modifier.weight(1f)
                    )
                    ModeChipButton(
                        label = stringResource(R.string.mode_turn_navigation),
                        icon = Icons.Rounded.Explore,
                        iconTint = Color(0xFF10B981),
                        isSelected = activeMode == IslandMode.Navigation,
                        onClick = { onModeSelect(IslandMode.Navigation) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 4: System Tools
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ModeChipButton(
                        label = stringResource(R.string.mode_file_transfer),
                        icon = Icons.Rounded.FileDownload,
                        iconTint = Color(0xFF06B6D4),
                        isSelected = activeMode == IslandMode.DownloadUpload,
                        onClick = { onModeSelect(IslandMode.DownloadUpload) },
                        modifier = Modifier.weight(1f)
                    )
                    ModeChipButton(
                        label = stringResource(R.string.mode_hotspot_share),
                        icon = Icons.Rounded.WifiTethering,
                        iconTint = Color(0xFFF59E0B),
                        isSelected = activeMode == IslandMode.Hotspot,
                        onClick = { onModeSelect(IslandMode.Hotspot) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 5: Hardware
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ModeChipButton(
                        label = stringResource(R.string.mode_bluetooth),
                        icon = Icons.Rounded.BluetoothConnected,
                        iconTint = Color(0xFF38BDF8),
                        isSelected = activeMode == IslandMode.Bluetooth,
                        onClick = { onModeSelect(IslandMode.Bluetooth) },
                        modifier = Modifier.weight(1f)
                    )
                    ModeChipButton(
                        label = stringResource(R.string.mode_flashlight),
                        icon = Icons.Rounded.FlashlightOn,
                        iconTint = Color(0xFFF59E0B),
                        isSelected = activeMode == IslandMode.Flashlight,
                        onClick = { onModeSelect(IslandMode.Flashlight) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 6: Screen Recording & Timer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ModeChipButton(
                        label = stringResource(R.string.mode_screen_recording),
                        icon = Icons.Rounded.Videocam,
                        iconTint = Color(0xFFEF4444),
                        isSelected = activeMode == IslandMode.ScreenRecording,
                        onClick = { onModeSelect(IslandMode.ScreenRecording) },
                        modifier = Modifier.weight(1f)
                    )
                    ModeChipButton(
                        label = stringResource(R.string.mode_timer),
                        icon = Icons.Rounded.HourglassBottom,
                        iconTint = Color(0xFFF59E0B),
                        isSelected = activeMode == IslandMode.Timer,
                        onClick = { onModeSelect(IslandMode.Timer) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Row 7: Stopwatch & Fitness
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ModeChipButton(
                        label = stringResource(R.string.mode_stopwatch),
                        icon = Icons.Rounded.AvTimer,
                        iconTint = Color(0xFF06B6D4),
                        isSelected = activeMode == IslandMode.Stopwatch,
                        onClick = { onModeSelect(IslandMode.Stopwatch) },
                        modifier = Modifier.weight(1f)
                    )
                    ModeChipButton(
                        label = "举铁伴侣",
                        icon = Icons.Rounded.FitnessCenter,
                        iconTint = Color(0xFFFF9800),
                        isSelected = activeMode == IslandMode.Fitness,
                        onClick = { onModeSelect(IslandMode.Fitness) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(4.dp))

            OutlinedButton(
                onClick = onClearAll,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
            ) {
                Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.btn_clear_all_test_notifications), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ModeChipButton(
    label: String,
    icon: ImageVector,
    iconTint: Color,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isSelected) iconTint.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
            .border(
                1.dp,
                if (isSelected) iconTint.copy(alpha = 0.8f) else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(iconTint.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun DiagnosticsSummaryCard(
    overlayGranted: Boolean,
    notificationGranted: Boolean,
    batteryIgnored: Boolean,
    accessibilityGranted: Boolean = false,
    onOpenDiagnostics: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenDiagnostics),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Shield,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Column {
                        Text(
                            text = stringResource(R.string.system_diagnostics_title),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.system_diagnostics_desc),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Icon(
                    Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatusBadgePill(
                    label = stringResource(R.string.diag_accessibility),
                    isGranted = accessibilityGranted,
                    modifier = Modifier.weight(1f)
                )
                StatusBadgePill(
                    label = stringResource(R.string.perm_overlay_title),
                    isGranted = overlayGranted,
                    modifier = Modifier.weight(1f)
                )
                StatusBadgePill(
                    label = stringResource(R.string.diag_notifications),
                    isGranted = notificationGranted,
                    modifier = Modifier.weight(1f)
                )
                StatusBadgePill(
                    label = stringResource(R.string.diag_battery_saver),
                    isGranted = batteryIgnored,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@Composable
private fun StatusBadgePill(
    label: String,
    isGranted: Boolean,
    modifier: Modifier = Modifier
) {
    val color = if (isGranted) Color(0xFF0F9F6E) else Color(0xFFE88C25)
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.1f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
            .padding(vertical = 6.dp, horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(modifier = Modifier.size(6.dp).background(color, CircleShape))
            Text(
                text = label,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun SettingsOverviewSection(
    settings: SmartIslandSettings,
    overlayGranted: Boolean,
    notificationGranted: Boolean,
    batteryIgnored: Boolean,
    onNavigateTo: (FeatureDetailSection) -> Unit
) {
    val canEnable = overlayGranted && notificationGranted && batteryIgnored

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = stringResource(R.string.settings_overview_title),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Text(
                text = stringResource(R.string.settings_overview_desc),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Section 1: Island Behaviors & App Launcher
        SettingsCategoryGroup(title = stringResource(R.string.category_behaviors_launcher)) {
            FeatureStudioNavigationCard(
                title = stringResource(R.string.card_notifications_privacy_title),
                subtitle = stringResource(R.string.card_notifications_privacy_desc),
                icon = Icons.Rounded.Notifications,
                iconColor = Color(0xFF38BDF8),
                statusText = if (settings.showOnLockScreen) stringResource(R.string.card_notifications_privacy_status_lock) else stringResource(R.string.card_notifications_privacy_status_standard),
                onClick = { onNavigateTo(FeatureDetailSection.NotificationRules) }
            )

            FeatureStudioNavigationCard(
                title = stringResource(R.string.card_app_shortcuts_title),
                subtitle = stringResource(R.string.card_app_shortcuts_desc),
                icon = Icons.Rounded.Apps,
                iconColor = Color(0xFF22D3EE),
                statusText = stringResource(R.string.card_app_shortcuts_status, settings.shortcutPackages.size),
                onClick = { onNavigateTo(FeatureDetailSection.AppShortcuts) }
            )

            FeatureStudioNavigationCard(
                title = stringResource(R.string.card_notification_history_title),
                subtitle = stringResource(R.string.card_notification_history_desc),
                icon = Icons.Rounded.History,
                iconColor = Color(0xFF38BDF8),
                statusText = if (settings.enableNotificationHistory) stringResource(R.string.card_notification_history_status_active) else stringResource(R.string.card_notification_history_status_disabled),
                statusColor = if (settings.enableNotificationHistory) Color(0xFF0F9F6E) else Color(0xFF94A3B8),
                onClick = { onNavigateTo(FeatureDetailSection.NotificationHistory) }
            )
        }

        // Section 2: Appearance & Gesture Controls
        SettingsCategoryGroup(title = stringResource(R.string.category_appearance_controls)) {
            FeatureStudioNavigationCard(
                title = stringResource(R.string.card_color_studio_title),
                subtitle = stringResource(R.string.card_color_studio_desc),
                icon = Icons.Rounded.Palette,
                iconColor = Color(0xFFA855F7),
                statusText = stringResource(R.string.card_color_studio_status, (settings.opacity * 100).toInt()),
                onClick = { onNavigateTo(FeatureDetailSection.ColorStudio) }
            )

            FeatureStudioNavigationCard(
                title = stringResource(R.string.card_gestures_guide_title),
                subtitle = stringResource(R.string.card_gestures_guide_desc),
                icon = Icons.Rounded.Gesture,
                iconColor = Color(0xFF6366F1),
                statusText = stringResource(R.string.card_gestures_guide_status),
                onClick = { onNavigateTo(FeatureDetailSection.GesturesGuide) }
            )
        }

        // Section 3: System & Permissions
        SettingsCategoryGroup(title = stringResource(R.string.category_system_core)) {
            FeatureStudioNavigationCard(
                title = stringResource(R.string.card_permissions_setup_title),
                subtitle = stringResource(R.string.card_permissions_setup_desc),
                icon = Icons.Rounded.Shield,
                iconColor = Color(0xFF10B981),
                statusText = if (canEnable) stringResource(R.string.card_permissions_setup_status_all) else stringResource(R.string.status_action_required),
                statusColor = if (canEnable) Color(0xFF0F9F6E) else Color(0xFFE88C25),
                onClick = { onNavigateTo(FeatureDetailSection.PermissionsCenter) }
            )
        }

        // Section 4: About & Community
        SettingsCategoryGroup(title = stringResource(R.string.category_about_community)) {
            FeatureStudioNavigationCard(
                title = stringResource(R.string.card_about_app_title),
                subtitle = stringResource(R.string.card_about_app_desc, com.agupta07505.smartisland.BuildConfig.VERSION_NAME),
                icon = Icons.Rounded.Info,
                iconColor = Color(0xFFEC4899),
                statusText = stringResource(R.string.card_about_app_status),
                onClick = { onNavigateTo(FeatureDetailSection.AboutApp) }
            )

            FeatureStudioNavigationCard(
                title = stringResource(R.string.card_support_requests_title),
                subtitle = stringResource(R.string.card_support_requests_desc),
                icon = Icons.Rounded.People,
                iconColor = Color(0xFFF59E0B),
                onClick = { onNavigateTo(FeatureDetailSection.SupportCommunity) }
            )
        }
    }
}

@Composable
private fun SettingsCategoryGroup(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 4.dp, top = 4.dp)
        )
        content()
    }
}

@Composable
private fun FeatureStudioNavigationCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconColor: Color,
    statusText: String? = null,
    statusColor: Color = MaterialTheme.colorScheme.primary,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
                if (statusText != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(statusColor.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = statusText,
                            color = statusColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.width(10.dp))
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun StudioBottomNavigationBar(
    selectedTab: StudioTab,
    onTabSelected: (StudioTab) -> Unit
) {
    NavigationBar(
        containerColor = MaterialTheme.colorScheme.surface,
        tonalElevation = 6.dp
    ) {
        NavigationBarItem(
            selected = selectedTab == StudioTab.Studio,
            onClick = { onTabSelected(StudioTab.Studio) },
            icon = { Icon(Icons.Rounded.FlashOn, contentDescription = stringResource(R.string.tab_studio)) },
            label = { Text(stringResource(R.string.tab_studio), fontWeight = FontWeight.SemiBold) },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary
            )
        )
        NavigationBarItem(
            selected = selectedTab == StudioTab.Position,
            onClick = { onTabSelected(StudioTab.Position) },
            icon = { Icon(Icons.Rounded.Tune, contentDescription = stringResource(R.string.tab_position)) },
            label = { Text(stringResource(R.string.tab_position), fontWeight = FontWeight.SemiBold) },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary
            )
        )
        NavigationBarItem(
            selected = selectedTab == StudioTab.Settings,
            onClick = { onTabSelected(StudioTab.Settings) },
            icon = { Icon(Icons.Rounded.Settings, contentDescription = stringResource(R.string.tab_settings)) },
            label = { Text(stringResource(R.string.tab_settings), fontWeight = FontWeight.SemiBold) },
            colors = NavigationBarItemDefaults.colors(
                indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                selectedIconColor = MaterialTheme.colorScheme.primary,
                selectedTextColor = MaterialTheme.colorScheme.primary
            )
        )
    }
}

@SuppressLint("BatteryLife")
@Composable
private fun DetailScreenHost(
    section: FeatureDetailSection,
    settings: SmartIslandSettings,
    repository: SmartIslandSettingsRepository,
    overlayGranted: Boolean,
    notificationGranted: Boolean,
    batteryIgnored: Boolean,
    accessibilityGranted: Boolean = false,
    onBack: () -> Unit,
    onRefreshPermissions: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val isScrollableParent = section != FeatureDetailSection.NotificationHistory
    val scrollModifier = if (isScrollableParent) Modifier.verticalScroll(rememberScrollState()) else Modifier

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .then(scrollModifier)
            .padding(
                start = 20.dp,
                end = 20.dp,
                top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 12.dp,
                bottom = if (isScrollableParent) 28.dp else 12.dp
            ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            val title = when (section) {
                FeatureDetailSection.NotificationRules -> stringResource(R.string.detail_title_notifications_privacy)
                FeatureDetailSection.AppShortcuts -> stringResource(R.string.detail_title_app_shortcuts)
                FeatureDetailSection.NotificationHistory -> stringResource(R.string.detail_title_notification_history)
                FeatureDetailSection.ColorStudio -> stringResource(R.string.detail_title_color_studio)
                FeatureDetailSection.GesturesGuide -> stringResource(R.string.detail_title_gestures_guide)
                FeatureDetailSection.PermissionsCenter -> stringResource(R.string.detail_title_permissions_center)
                FeatureDetailSection.AboutApp -> stringResource(R.string.detail_title_about_app)
                FeatureDetailSection.SupportCommunity -> stringResource(R.string.detail_title_support_community)
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
        }

        when (section) {
            FeatureDetailSection.NotificationRules -> {
                NotificationsAndPrivacySection(settings = settings, repository = repository)
            }
            FeatureDetailSection.AppShortcuts -> {
                AppShortcutsSection(settings = settings, repository = repository)
            }
            FeatureDetailSection.NotificationHistory -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    NotificationHistorySection(settings = settings, repository = repository)
                }
            }
            FeatureDetailSection.ColorStudio -> {
                CustomizationsSection(settings = settings, repository = repository)
            }
            FeatureDetailSection.GesturesGuide -> {
                GesturesSection()
            }
            FeatureDetailSection.PermissionsCenter -> {
                PermissionsSection(
                    overlayGranted = overlayGranted,
                    notificationGranted = notificationGranted,
                    batteryIgnored = batteryIgnored,
                    accessibilityGranted = accessibilityGranted,
                    onAccessibilityClick = {
                        runCatching {
                            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                            context.startActivity(intent)
                        }.onFailure {
                            Toast.makeText(context, "请在系统设置中找到并开启【Smart Island 灵动岛】无障碍服务", Toast.LENGTH_LONG).show()
                        }
                    },
                    onOverlayClick = {
                        val intent = Intent(
                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:${context.packageName}")
                        )
                        runCatching {
                            context.startActivity(intent)
                        }.onFailure {
                            context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
                        }
                    },
                    onNotificationClick = {
                        val detailIntent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS").apply {
                            val component = ComponentName(context, com.agupta07505.smartisland.service.SmartIslandNotificationListenerService::class.java)
                            putExtra("android.provider.extra.NOTIFICATION_LISTENER_COMPONENT_NAME", component.flattenToString())
                        }
                        runCatching {
                            context.startActivity(detailIntent)
                        }.onFailure {
                            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        }
                    },
                    onBatteryClick = {
                        runCatching {
                            context.startActivity(
                                Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                                    .setData(Uri.parse("package:${context.packageName}"))
                            )
                        }.onFailure {
                            context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
                        }
                    },
                    onRefreshPermissions = onRefreshPermissions
                )
            }
            FeatureDetailSection.AboutApp -> {
                AboutSection(settings = settings, repository = repository)
            }
            FeatureDetailSection.SupportCommunity -> {
                SupportSection()
            }
        }
    }
}

private fun isNotificationListenerEnabled(context: Context): Boolean {
    val enabled = Settings.Secure.getString(
        context.contentResolver,
        "enabled_notification_listeners"
    )
    return enabled?.split(":")?.any {
        ComponentName.unflattenFromString(it)?.packageName == context.packageName
    } == true
}

private fun isOverlayPermissionGranted(context: Context): Boolean {
    return Settings.canDrawOverlays(context)
}

private fun isBatteryOptimizationIgnored(context: Context): Boolean {
    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.M) return true
    val pm = context.getSystemService(Context.POWER_SERVICE) as? android.os.PowerManager
    return pm?.isIgnoringBatteryOptimizations(context.packageName) ?: true
}

@Composable
private fun WelcomeDialog(
    onDismiss: () -> Unit,
    onStarClick: () -> Unit,
    onJoinCommunityClick: () -> Unit
) {
    val context = LocalContext.current
    val appIcon = remember(context) {
        runCatchingLogged("WelcomeDialog", "Failed to get app icon") {
            val drawable = context.packageManager.getApplicationIcon(context.packageName)
            val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 144
            val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 144
            val bitmap = android.graphics.Bitmap.createBitmap(width, height, android.graphics.Bitmap.Config.ARGB_8888)
            val canvas = android.graphics.Canvas(bitmap)
            drawable.setBounds(0, 0, width, height)
            drawable.draw(canvas)
            bitmap.asImageBitmap()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .padding(16.dp),
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 10.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (appIcon != null) {
                    Image(
                        bitmap = appIcon,
                        contentDescription = "Smart Island Logo",
                        modifier = Modifier
                            .size(68.dp)
                            .clip(RoundedCornerShape(18.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(18.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("SI", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    }
                }

                Text(
                    text = stringResource(R.string.welcome_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Text(
                    text = stringResource(R.string.welcome_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = onStarClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        GithubIcon(tint = MaterialTheme.colorScheme.onSecondary)
                        Text(
                            stringResource(R.string.star_on_github),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondary
                        )
                    }
                }

                OutlinedButton(
                    onClick = onJoinCommunityClick,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.People,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            stringResource(R.string.join_telegram),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                ElevatedButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.elevatedButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text(stringResource(R.string.btn_get_started), fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun GithubIcon(tint: Color = Color.Black) {
    Canvas(modifier = Modifier.size(18.dp)) {
        val scaleX = size.width / 24f
        val scaleY = size.height / 24f
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(12f * scaleX, 2f * scaleY)
            cubicTo(6.477f * scaleX, 2f * scaleY, 2f * scaleX, 6.477f * scaleX, 2f * scaleX, 12f * scaleY)
            cubicTo(2f * scaleX, 16.42f * scaleY, 4.865f * scaleX, 20.166f * scaleY, 8.839f * scaleX, 21.489f * scaleY)
            cubicTo(9.339f * scaleX, 21.581f * scaleY, 9.521f * scaleX, 21.272f * scaleY, 9.521f * scaleX, 21.007f * scaleY)
            cubicTo(9.521f * scaleX, 20.77f * scaleY, 9.513f * scaleX, 20.141f * scaleY, 9.508f * scaleX, 19.307f * scaleY)
            cubicTo(6.726f * scaleX, 19.91f * scaleY, 6.139f * scaleX, 17.97f * scaleY, 6.139f * scaleX, 17.97f * scaleY)
            cubicTo(5.685f * scaleX, 16.814f * scaleY, 5.029f * scaleX, 16.506f * scaleY, 5.029f * scaleX, 16.506f * scaleY)
            cubicTo(4.121f * scaleX, 15.886f * scaleY, 5.098f * scaleX, 15.898f * scaleY, 5.098f * scaleX, 15.898f * scaleY)
            cubicTo(6.101f * scaleX, 15.968f * scaleY, 6.629f * scaleX, 16.928f * scaleY, 6.629f * scaleX, 16.928f * scaleY)
            cubicTo(7.521f * scaleX, 18.457f * scaleY, 8.97f * scaleX, 18.015f * scaleY, 9.539f * scaleX, 17.759f * scaleY)
            cubicTo(9.631f * scaleX, 17.113f * scaleY, 9.889f * scaleX, 16.673f * scaleY, 10.175f * scaleX, 16.423f * scaleY)
            cubicTo(7.955f * scaleX, 16.17f * scaleY, 5.62f * scaleX, 15.313f * scaleY, 5.62f * scaleX, 11.48f * scaleY)
            cubicTo(5.62f * scaleX, 10.389f * scaleY, 6.01f * scaleX, 9.496f * scaleY, 6.649f * scaleX, 8.797f * scaleY)
            cubicTo(6.546f * scaleX, 8.544f * scaleY, 6.203f * scaleX, 7.527f * scaleY, 6.747f * scaleX, 6.15f * scaleY)
            cubicTo(6.747f * scaleX, 6.15f * scaleY, 7.587f * scaleX, 5.881f * scaleY, 9.497f * scaleX, 7.175f * scaleY)
            cubicTo(10.295f * scaleX, 6.953f * scaleY, 11.15f * scaleX, 6.842f * scaleY, 12f * scaleX, 6.838f * scaleY)
            cubicTo(12.85f * scaleX, 6.842f * scaleY, 13.705f * scaleX, 6.953f * scaleY, 14.503f * scaleX, 7.175f * scaleY)
            cubicTo(16.413f * scaleX, 5.881f * scaleY, 17.253f * scaleX, 6.15f * scaleY, 17.253f * scaleX, 6.15f * scaleY)
            cubicTo(17.797f * scaleX, 7.527f * scaleY, 17.454f * scaleX, 8.544f * scaleY, 17.351f * scaleX, 8.797f * scaleY)
            cubicTo(17.99f * scaleX, 9.496f * scaleY, 18.38f * scaleX, 10.389f * scaleY, 18.38f * scaleX, 11.48f * scaleY)
            cubicTo(18.38f * scaleX, 15.323f * scaleY, 16.041f * scaleX, 16.168f * scaleY, 13.813f * scaleX, 16.415f * scaleY)
            cubicTo(14.172f * scaleX, 16.724f * scaleY, 14.491f * scaleX, 17.334f * scaleY, 14.491f * scaleX, 18.267f * scaleY)
            cubicTo(14.491f * scaleX, 19.603f * scaleY, 14.479f * scaleX, 20.682f * scaleY, 14.479f * scaleX, 21.01f * scaleY)
            cubicTo(14.479f * scaleX, 21.277f * scaleY, 14.659f * scaleX, 21.589f * scaleY, 15.167f * scaleX, 21.489f * scaleY)
            cubicTo(19.141f * scaleX, 20.16f * scaleY, 22f * scaleX, 12f * scaleY, 22f * scaleX, 12f * scaleY)
            cubicTo(22f * scaleX, 6.477f * scaleY, 17.523f * scaleY, 2f * scaleY, 12f * scaleY, 2f * scaleY)
            close()
        }
        drawPath(path, color = tint)
    }
}

@Preview(showBackground = true, name = "Light Mode")
@Composable
fun SmartIslandHomeScreenLightPreview() {
    SmartIslandTheme(darkTheme = false) {
        SmartIslandHomeScreen()
    }
}

@Preview(showBackground = true, name = "Dark Mode")
@Composable
fun SmartIslandHomeScreenDarkPreview() {
    SmartIslandTheme(darkTheme = true) {
        SmartIslandHomeScreen()
    }
}
