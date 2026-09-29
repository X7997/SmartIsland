/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland.service

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.app.KeyguardManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.agupta07505.smartisland.MainActivity
import com.agupta07505.smartisland.R
import com.agupta07505.smartisland.data.INotificationRepository
import com.agupta07505.smartisland.data.SmartIslandCommand
import com.agupta07505.smartisland.data.SmartIslandSettings
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository
import com.agupta07505.smartisland.model.IslandNotification
import com.agupta07505.smartisland.ui.IslandViewModel
import com.agupta07505.smartisland.ui.OverlayIsland
import com.agupta07505.smartisland.ui.expanded.sendIntentWithOptions
import com.agupta07505.smartisland.util.SystemServiceRecovery
import com.agupta07505.smartisland.util.runCatchingLogged
import com.agupta07505.smartisland.util.runSuspendCatchingLogged
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SmartIslandOverlayService : AccessibilityService() {
    private val overlayWindowType: Int = WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
    private var overlayWindowContext: Context? = null
    private var privateLayerFlagSupported = true
    private val windowManager: WindowManager
        get() = (overlayWindowContext ?: this).getSystemService(Context.WINDOW_SERVICE) as WindowManager
    @Inject lateinit var repository: SmartIslandSettingsRepository
    @Inject lateinit var notificationRepository: INotificationRepository
    private var islandView: ComposeView? = null
    private val overlayOwners = OverlayViewTreeOwners()
    private lateinit var systemEventReceiver: SystemEventReceiver
    private lateinit var viewModel: IslandViewModel
    private var isLockScreenActive: Boolean = false
    private var systemEventReceiverRegistered = false
    private var screenStateReceiverRegistered = false
    private var torchCallbackRegistered = false
    private var foregroundStarted = false
    private var taskRemoved = false
    private var startedForTaskLifecycle = false
    private val isTouchableRegionSupportedState = mutableStateOf(false)
    private var isTouchableRegionSupported: Boolean
        get() = isTouchableRegionSupportedState.value
        set(value) {
            isTouchableRegionSupportedState.value = value
        }
    @Volatile private var destroyed = false
    // Helper to determine if a secondary island (e.g., workout plan) is present
    private fun hasSecondaryIsland(): Boolean = viewModel.notifications.value.size >= 2

    private var isWindowExpanded: Boolean = false
    private var collapseJob: kotlinx.coroutines.Job? = null
    private var lastParams: WindowManager.LayoutParams? = null

    private val torchCallback = object : android.hardware.camera2.CameraManager.TorchCallback() {
        override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
            if (destroyed || !::viewModel.isInitialized) return
            if (enabled) {
                notificationRepository.postNotification(
                    IslandNotification(
                        key = "system_flashlight",
                        packageName = "com.android.systemui",
                        appName = "Flashlight",
                        title = "Flashlight ON",
                        text = "Tap to turn off",
                        mode = com.agupta07505.smartisland.model.IslandMode.Flashlight,
                        timeMillis = System.currentTimeMillis(),
                        actionIntents = listOf(
                            com.agupta07505.smartisland.model.IslandNotificationAction("Turn Off", null)
                        )
                    ),
                    autoExpand = true
                )
            } else {
                notificationRepository.removeNotification("system_flashlight")
            }
        }
    }

    private val serviceScope = kotlinx.coroutines.CoroutineScope(
        SupervisorJob() +
            Dispatchers.Main.immediate +
            CoroutineExceptionHandler { _, error ->
                android.util.Log.e(TAG, "Unhandled overlay coroutine failure", error)
            }
    )

    // Monitor screen state and unlock events to show/hide the island accordingly
    private val screenStateReceiver = object : android.content.BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            runCatchingLogged(TAG, "Screen-state callback failed") {
                if (destroyed || !::viewModel.isInitialized) return@runCatchingLogged
                val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
                when (intent.action) {
                    Intent.ACTION_SCREEN_ON -> {
                        overlayOwners.resume()
                        isLockScreenActive = keyguardManager?.isKeyguardLocked == true
                        updateWindowLayoutParams(
                            isWindowExpanded,
                            viewModel.settings.value
                        )
                    }
                    Intent.ACTION_SCREEN_OFF -> {
                        overlayOwners.pause()
                        isLockScreenActive = true
                        updateWindowLayoutParams(
                            isWindowExpanded,
                            viewModel.settings.value
                        )
                    }
                    Intent.ACTION_USER_PRESENT -> {
                        isLockScreenActive = false
                        updateWindowLayoutParams(
                            isWindowExpanded,
                            viewModel.settings.value
                        )
                    }
                }
            }
        }
    }


    override fun onConfigurationChanged(newConfig: android.content.res.Configuration) {
        super.onConfigurationChanged(newConfig)
        // Wrapped: a throw here would make Android disable the service automatically.
        runCatchingLogged(TAG, "onConfigurationChanged failed") {
            if (destroyed || !::viewModel.isInitialized) return@runCatchingLogged
            updateWindowLayoutParams(isWindowExpanded, viewModel.settings.value)
        }
    }

    override fun onCreate() {
        super.onCreate()
        destroyed = false
        instance = this

        runCatchingLogged(TAG, "createNotificationChannel failed") {
            createNotificationChannel()
        }

        val initializedViewModel = runCatchingLogged(TAG, "Overlay ViewModel initialization failed") {
            // Lifecycle must be restored before the service-owned ViewModel is created.
            overlayOwners.resume()
            ViewModelProvider(
                overlayOwners,
                IslandViewModel.provideFactory(repository, notificationRepository)
            )[IslandViewModel::class.java]
        }
        if (initializedViewModel == null) {
            android.util.Log.e(TAG, "Overlay ViewModel is unavailable; overlay cannot start")
            return
        }
        viewModel = initializedViewModel
        
        systemEventReceiver = SystemEventReceiver(notificationRepository)
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_POWER_CONNECTED)
            addAction(Intent.ACTION_POWER_DISCONNECTED)
            addAction(Intent.ACTION_BATTERY_CHANGED)
            addAction(Intent.ACTION_BATTERY_LOW)
            addAction(Intent.ACTION_BATTERY_OKAY)
            addAction(android.os.PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
            addAction(android.bluetooth.BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(android.bluetooth.BluetoothDevice.ACTION_ACL_DISCONNECTED)
        }
        
        // CRASH FIX: Android 13+/14+ requires explicit export flag for system broadcasts
        runCatchingLogged(TAG, "registerReceiver failed") {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(systemEventReceiver, filter, Context.RECEIVER_EXPORTED)
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                registerReceiver(systemEventReceiver, filter)
            }
            systemEventReceiverRegistered = true
        }

        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        isLockScreenActive = keyguardManager?.isKeyguardLocked == true

        val screenFilter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
            addAction(Intent.ACTION_USER_PRESENT)
        }
        runCatchingLogged(TAG, "registerReceiver screenStateReceiver failed") {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                registerReceiver(screenStateReceiver, screenFilter, Context.RECEIVER_EXPORTED)
            } else {
                @Suppress("UnspecifiedRegisterReceiverFlag")
                registerReceiver(screenStateReceiver, screenFilter)
            }
            screenStateReceiverRegistered = true
        }

        runCatchingLogged(TAG, "registerTorchCallback failed") {
            val cameraManager = getSystemService(Context.CAMERA_SERVICE) as? android.hardware.camera2.CameraManager
            cameraManager?.registerTorchCallback(torchCallback, android.os.Handler(android.os.Looper.getMainLooper()))
            torchCallbackRegistered = true
        }

        serviceScope.launch {
            runSuspendCatchingLogged(TAG, "Settings collector failed") {
                repository.settings.collect { settings ->
                    if (destroyed) return@collect
                    if (!settings.enabled) {
                        stopOverlaySession()
                    } else if (isSystemConnected) {
                        startOverlaySession(settings)
                    }
                }
            }
        }

        serviceScope.launch {
            runSuspendCatchingLogged(TAG, "Expanded-state collector failed") {
                viewModel.expanded.collectLatest { expanded ->
                    if (destroyed || !viewModel.settings.value.enabled) {
                        return@collectLatest
                    }
                    collapseJob?.cancel()
                    if (expanded) {
                        isWindowExpanded = true
                        updateWindowLayoutParams(true, viewModel.settings.value)
                    } else {
                        collapseJob = serviceScope.launch {
                            kotlinx.coroutines.delay(AUTO_COLLAPSE_DELAY_MS)
                            isWindowExpanded = false
                            updateWindowLayoutParams(false, viewModel.settings.value)
                        }
                    }
                }
            }
        }

        serviceScope.launch {
            runSuspendCatchingLogged(TAG, "Notifications-state collector failed") {
                viewModel.notifications.collectLatest {
                    if (destroyed || !viewModel.settings.value.enabled) {
                        return@collectLatest
                    }
                    updateWindowLayoutParams(isWindowExpanded, viewModel.settings.value)
                }
            }
        }

        serviceScope.launch {
            runSuspendCatchingLogged(TAG, "Input-active collector failed") {
                viewModel.isInputActive.collectLatest {
                    if (destroyed || !viewModel.settings.value.enabled) {
                        return@collectLatest
                    }
                    updateWindowLayoutParams(isWindowExpanded, viewModel.settings.value)
                }
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        runCatchingLogged(TAG, "onAccessibilityEvent failed") {
            if (destroyed || !::viewModel.isInitialized) return@runCatchingLogged
            val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
            val locked = keyguardManager?.isKeyguardLocked == true
            if (isLockScreenActive != locked) {
                isLockScreenActive = locked
                updateWindowLayoutParams(isWindowExpanded, viewModel.settings.value)
            }
            if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                val openedPackage = event.packageName?.toString()
                if (!openedPackage.isNullOrEmpty() &&
                    openedPackage != packageName &&
                    openedPackage != "com.android.systemui"
                ) {
                    viewModel.foregroundPackage.value = openedPackage
                }
            }
        }
    }

    override fun onInterrupt() {}

    override fun onServiceConnected() {
        super.onServiceConnected()
        isSystemConnected = true
        instance = this
        android.util.Log.i(TAG, "onServiceConnected: AccessibilityService connected with system window token")
        if (destroyed || taskRemoved || !::viewModel.isInitialized) return
        serviceScope.launch {
            runSuspendCatchingLogged(TAG, "Service reconnect failed") {
                val settings = repository.settings.first()
                if (settings.enabled) {
                    if (islandView != null && lastParams?.type != WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY) {
                        android.util.Log.i(TAG, "Promoting overlay window to TYPE_ACCESSIBILITY_OVERLAY")
                        removeCollapsedWindow()
                    }
                    startOverlaySession(settings)
                } else {
                    stopOverlaySession()
                }
            }
        }
    }

    override fun onUnbind(intent: Intent?): Boolean {
        isSystemConnected = false
        removeCollapsedWindow()
        overlayWindowContext = null
        return true
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (taskRemoved) return START_NOT_STICKY
        instance = this
        startedForTaskLifecycle = true
        ensureForegroundStarted()
        if (isSystemConnected && ::viewModel.isInitialized && viewModel.settings.value.enabled) {
            startOverlaySession(viewModel.settings.value)
        }
        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        runCatchingLogged(TAG, "onTaskRemoved clean exit") {
            taskRemoved = true
            startedForTaskLifecycle = false
            stopOverlaySession()
            unregisterRuntimeCallbacks()
            stopSelf()
        }
    }

    override fun onDestroy() {
        if (destroyed) return
        destroyed = true
        if (instance === this) {
            instance = null
        }
        isSystemConnected = false
        serviceScope.cancel()

        unregisterRuntimeCallbacks()

        removeCollapsedWindow()
        overlayWindowContext = null
        stopForegroundSafely()
        runCatchingLogged(TAG, "Overlay owners destroy failed") {
            overlayOwners.destroy()
        }
        super.onDestroy()
    }

    private fun unregisterRuntimeCallbacks() {
        if (::systemEventReceiver.isInitialized && systemEventReceiverRegistered) {
            runCatchingLogged(TAG, "unregisterReceiver failed") {
                unregisterReceiver(systemEventReceiver)
            }
            systemEventReceiverRegistered = false
        }
        if (screenStateReceiverRegistered) {
            runCatchingLogged(TAG, "unregisterReceiver screenStateReceiver failed") {
                unregisterReceiver(screenStateReceiver)
            }
            screenStateReceiverRegistered = false
        }
        if (torchCallbackRegistered) {
            runCatchingLogged(TAG, "unregisterTorchCallback failed") {
                val cameraManager = getSystemService(Context.CAMERA_SERVICE) as? android.hardware.camera2.CameraManager
                cameraManager?.unregisterTorchCallback(torchCallback)
            }
            torchCallbackRegistered = false
        }

    }

    private fun registerRuntimeCallbacksAfterTaskRemoval() {
        if (!::systemEventReceiver.isInitialized || destroyed) return
        if (!systemEventReceiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_POWER_CONNECTED)
                addAction(Intent.ACTION_POWER_DISCONNECTED)
                addAction(Intent.ACTION_BATTERY_CHANGED)
                addAction(Intent.ACTION_BATTERY_LOW)
                addAction(Intent.ACTION_BATTERY_OKAY)
                addAction(android.os.PowerManager.ACTION_POWER_SAVE_MODE_CHANGED)
                addAction(android.bluetooth.BluetoothDevice.ACTION_ACL_CONNECTED)
                addAction(android.bluetooth.BluetoothDevice.ACTION_ACL_DISCONNECTED)
            }
            runCatchingLogged(TAG, "registerReceiver after task removal failed") {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    registerReceiver(systemEventReceiver, filter, Context.RECEIVER_EXPORTED)
                } else {
                    @Suppress("UnspecifiedRegisterReceiverFlag")
                    registerReceiver(systemEventReceiver, filter)
                }
                systemEventReceiverRegistered = true
            }
        }
        if (!screenStateReceiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_ON)
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_USER_PRESENT)
            }
            runCatchingLogged(TAG, "registerScreenReceiver after task removal failed") {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    registerReceiver(screenStateReceiver, filter, Context.RECEIVER_EXPORTED)
                } else {
                    @Suppress("UnspecifiedRegisterReceiverFlag")
                    registerReceiver(screenStateReceiver, filter)
                }
                screenStateReceiverRegistered = true
            }
        }
        if (!torchCallbackRegistered) {
            runCatchingLogged(TAG, "registerTorchCallback after task removal failed") {
                val cameraManager = getSystemService(Context.CAMERA_SERVICE) as? android.hardware.camera2.CameraManager
                cameraManager?.registerTorchCallback(torchCallback, android.os.Handler(android.os.Looper.getMainLooper()))
                torchCallbackRegistered = cameraManager != null
            }
        }
    }

    private fun startOverlaySession(settings: SmartIslandSettings) {
        if (destroyed || taskRemoved || !::viewModel.isInitialized || !isSystemConnected) return
        ensureForegroundStarted()
        ensureCollapsedWindow()
        updateWindowLayoutParams(isWindowExpanded, settings)
    }

    private fun stopOverlaySession() {
        removeCollapsedWindow()
        stopForegroundSafely()
        if (::viewModel.isInitialized) {
            viewModel.collapse()
        }
    }

    private fun ensureForegroundStarted() {
        if (foregroundStarted || destroyed) return
        runCatchingLogged(TAG, "startForeground failed") {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ServiceCompat.startForeground(
                    this,
                    NOTIFICATION_ID,
                    buildNotification(),
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                    } else {
                        0
                    }
                )
            } else {
                startForeground(NOTIFICATION_ID, buildNotification())
            }
            foregroundStarted = true
        }
    }

    private fun stopForegroundSafely() {
        if (!foregroundStarted) return
        runCatchingLogged(TAG, "stopForeground failed") {
            ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        }
        foregroundStarted = false
    }

    private val statusBarHeight: Float
        get() {
            val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
            val heightPx = if (resourceId > 0) resources.getDimensionPixelSize(resourceId) else 0
            val heightDp = heightPx / resources.displayMetrics.density
            return if (heightDp > 0f) heightDp else 24f
        }

    private fun ensureCollapsedWindow() {
        if (destroyed ||
            islandView != null ||
            !::viewModel.isInitialized ||
            !isSystemConnected
        ) return

        try {
            // AccessibilityService.createWindowContext supplies the system's overlay token
            // on Android R+. The service's ordinary WindowManager can lack that token on OEM ROMs.
            val viewContext = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val displayManager = getSystemService(Context.DISPLAY_SERVICE) as android.hardware.display.DisplayManager
                val display = displayManager.getDisplay(android.view.Display.DEFAULT_DISPLAY)
                    ?: error("Default display is unavailable for accessibility overlay")
                overlayWindowContext ?: createWindowContext(display, overlayWindowType, null).also {
                    overlayWindowContext = it
                }
            } else {
                this
            }
            val newView = ComposeView(viewContext).apply {
                val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
                val isLocked = keyguardManager?.isKeyguardLocked == true
                isLockScreenActive = isLocked
                val isLandscape = resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
                val isHidden = (!viewModel.settings.value.showOnLockScreen && isLocked) || (isLandscape && !viewModel.settings.value.showInLandscape)
                visibility = if (isHidden) android.view.View.GONE else android.view.View.VISIBLE

                installOverlayViewTreeOwners()
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
                background = null
                elevation = 0f
                outlineProvider = null
                isFocusable = false
                isFocusableInTouchMode = false
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    defaultFocusHighlightEnabled = false
                }
                setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnDetachedFromWindow)
                setContent {
                    OverlayIsland(
                        viewModel = this@SmartIslandOverlayService.viewModel,
                        statusBarHeight = statusBarHeight,
                        onOpenNotification = { notification -> openNotification(notification) },
                        onLaunchApp = { packageName -> launchApp(packageName) },
                        onOpenFloatingWindow = { openCurrentNotificationInFloatingWindow() },
                        isFullWidth = isTouchableRegionSupportedState.value
                    )
                }

                setupTouchableRegion(this)
            }
            var params = collapsedParams(viewModel.settings.value)
            try {
                windowManager.addView(newView, params)
            } catch (e: WindowManager.BadTokenException) {
                if (!privateLayerFlagSupported) throw e
                // Some OEMs reject this privileged private flag even for a valid 2032 token.
                // Retry the same accessibility window type without the flag.
                android.util.Log.w(TAG, "Private layer flag rejected; retrying type 2032 without it", e)
                privateLayerFlagSupported = false
                params = collapsedParams(viewModel.settings.value)
                windowManager.addView(newView, params)
            }
            islandView = newView
            lastParams = params
            android.util.Log.i(TAG, "ensureCollapsedWindow: Overlay window added successfully with type=${params.type}")
        } catch (e: Exception) {
            android.util.Log.e(TAG, "ensureCollapsedWindow: addView failed", e)
            islandView = null
            lastParams = null
        }
    }

    // Use reflection to set up OnComputeInternalInsetsListener since it is a hidden system API.
    // This allows the overlay window to pass through touches outside the pill boundary.
    // Keep the suppression local: this best-effort workaround is guarded by
    // runCatchingLogged so unsupported devices fall back without crashing.
    @SuppressLint("PrivateApi", "SoonBlockedPrivateApi")
    private fun setupTouchableRegion(view: ComposeView) {
        android.util.Log.d(TAG, "setupTouchableRegion: starting registration for view=$view")
        runCatchingLogged(TAG, "Failed to setup touchable region") {
            val listenerClass = Class.forName("android.view.ViewTreeObserver\$OnComputeInternalInsetsListener")
            val insetsClass = Class.forName("android.view.ViewTreeObserver\$InternalInsetsInfo")
            
            val setTouchableInsetsMethod = insetsClass.getMethod("setTouchableInsets", Int::class.javaPrimitiveType)
            val touchableRegionField = runCatching {
                insetsClass.getDeclaredField("touchableRegion")
            }.getOrElse {
                insetsClass.getField("touchableRegion")
            }.apply {
                isAccessible = true
            }
            
            // InternalInsetsInfo touchable insets options
            val TOUCHABLE_INSETS_FRAME = 0
            val TOUCHABLE_INSETS_REGION = 3
            
            // Create a dynamic proxy implementation of OnComputeInternalInsetsListener
            val proxyListener = java.lang.reflect.Proxy.newProxyInstance(
                classLoader,
                arrayOf(listenerClass)
            ) { _, method, args ->
                if (method.name == "onComputeInternalInsets" && args != null && args.isNotEmpty()) {
                    val insets = args[0]
                    val isExpanded = viewModel.expanded.value
                    val isGone = view.visibility == android.view.View.GONE
                    android.util.Log.d(TAG, "onComputeInternalInsets callback: isExpanded=$isExpanded isGone=$isGone")
                    if (isGone) {
                        setTouchableInsetsMethod.invoke(insets, TOUCHABLE_INSETS_REGION)
                        val region = touchableRegionField.get(insets) as android.graphics.Region
                        region.setEmpty()
                    } else if (isExpanded) {
                        // When expanded, let the entire frame intercept touches so clicking outside collapses it
                        setTouchableInsetsMethod.invoke(insets, TOUCHABLE_INSETS_FRAME)
                    } else {
                        // Keep the top-edge gesture corridor only as wide as the visible island.
                        // A wide transparent region makes adjacent controls in other apps untouchable.
                        setTouchableInsetsMethod.invoke(insets, TOUCHABLE_INSETS_REGION)
                        
                        val density = resources.displayMetrics.density
                        val screenWidth = resources.displayMetrics.widthPixels
                        val settingsVal = viewModel.settings.value
                        val bounds = collapsedTouchBounds(
                            screenWidth, density, settingsVal.width, settingsVal.height,
                            settingsVal.xOffset, settingsVal.yOffset,
                            viewModel.notifications.value.size >= 2
                        )
                        android.util.Log.d(TAG, "onComputeInternalInsets: region set to $bounds")
                        val region = touchableRegionField.get(insets) as android.graphics.Region
                        region.set(bounds.left, bounds.top, bounds.right, bounds.bottom)
                    }
                }
                null
            }
            
            val registerListener = {
                val observer = view.viewTreeObserver
                android.util.Log.d(TAG, "registerListener lambda: viewTreeObserver=$observer, isAlive=${observer.isAlive}")
                if (observer.isAlive) {
                    val addListenerMethod = observer.javaClass.getMethod(
                        "addOnComputeInternalInsetsListener",
                        listenerClass
                    )
                    addListenerMethod.invoke(observer, proxyListener)
                    isTouchableRegionSupported = true
                    android.util.Log.d(TAG, "OnComputeInternalInsetsListener successfully registered on live ViewTreeObserver")
                    if (::viewModel.isInitialized && !isWindowExpanded) {
                        updateWindowLayoutParams(false, viewModel.settings.value)
                    }
                }
            }
            
            // ViewTreeObserver changes when the view is attached to a window.
            // We must register the listener on the live ViewTreeObserver of the attached window.
            android.util.Log.d(TAG, "setupTouchableRegion: isAttachedToWindow=${view.isAttachedToWindow}")
            if (view.isAttachedToWindow) {
                registerListener()
            } else {
                view.addOnAttachStateChangeListener(object : android.view.View.OnAttachStateChangeListener {
                    override fun onViewAttachedToWindow(v: android.view.View) {
                        android.util.Log.d(TAG, "onViewAttachedToWindow: registering listener now")
                        registerListener()
                    }
                    override fun onViewDetachedFromWindow(v: android.view.View) {
                        android.util.Log.d(TAG, "onViewDetachedFromWindow called")
                    }
                })
            }
        } ?: run {
            isTouchableRegionSupported = false
            android.util.Log.w(TAG, "Touchable region reflection unsupported or blocked, falling back to physical bounds")
        }
    }

    private fun updateWindowLayoutParams(expanded: Boolean, settings: SmartIslandSettings) {
        if (destroyed || taskRemoved || !isSystemConnected || !::viewModel.isInitialized) return
        if (islandView == null && settings.enabled) {
            ensureForegroundStarted()
            ensureCollapsedWindow()
        }
        val view = islandView ?: return
        val density = resources.displayMetrics.density
        val screenWidthPx = resources.displayMetrics.widthPixels.toFloat()
        
        val keyguardManager = getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager
        val isLocked = keyguardManager?.isKeyguardLocked == true
        isLockScreenActive = isLocked
        viewModel.isLocked.value = isLocked
        
        val isLandscape = resources.configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        val isHidden = (!settings.showOnLockScreen && isLocked) || (isLandscape && !settings.showInLandscape)

        val targetVisibility = if (isHidden) android.view.View.GONE else android.view.View.VISIBLE
        if (view.visibility != targetVisibility) {
            view.visibility = targetVisibility
        }

        val isSplitMode = viewModel.notifications.value.size >= 2
        val bounds = collapsedTouchBounds(
            screenWidthPx.toInt(), density, settings.width, settings.height,
            settings.xOffset, settings.yOffset, isSplitMode
        )
        val h = if (expanded) {
            WindowManager.LayoutParams.MATCH_PARENT
        } else {
            bounds.bottom
        }
        val w = if (expanded || isTouchableRegionSupported) WindowManager.LayoutParams.MATCH_PARENT else bounds.width
        val isInput = viewModel.isInputActive.value && expanded
        view.isFocusable = isInput
        view.isFocusableInTouchMode = isInput
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            view.defaultFocusHighlightEnabled = false
        }
        val focusFlags = if (isInput) 0 else WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
        val currentFlags = focusFlags or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED

        val currentX = if (expanded || isTouchableRegionSupported) 0 else (bounds.left + bounds.right) / 2 - screenWidthPx.toInt() / 2
        val currentY = 0
        val currentSoftInputMode = if (isInput) {
            WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE or
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE
        } else {
            0
        }

        val lp = lastParams
        val targetType = overlayWindowType
        if (lp != null && lp.type != targetType) {
            // Window type changed (e.g. promoted from APPLICATION_OVERLAY to ACCESSIBILITY_OVERLAY).
            // Android strictly forbids modifying window type via updateViewLayout.
            removeCollapsedWindow()
            ensureCollapsedWindow()
            return
        }

        if (lp != null &&
            lp.type == targetType &&
            lp.width == w &&
            lp.height == h &&
            lp.flags == currentFlags &&
            lp.x == currentX &&
            lp.y == currentY &&
            lp.softInputMode == currentSoftInputMode
        ) {
            return
        }

        val params = WindowManager.LayoutParams(
            w,
            h,
            targetType,
            currentFlags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = currentX
            y = currentY
            softInputMode = currentSoftInputMode
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            }
            applyPrivateFlags()
        }
        lastParams = params
        runCatchingLogged(TAG, "Failed to update view layout") { 
            windowManager.updateViewLayout(view, params) 
        }
    }

    private fun WindowManager.LayoutParams.applyPrivateFlags() {
        if (!privateLayerFlagSupported) return
        runCatchingLogged(TAG, "applyPrivateFlags failed") {
            val privateFlagsField = WindowManager.LayoutParams::class.java.getField("privateFlags")
            val current = privateFlagsField.getInt(this)
            // PRIVATE_FLAG_LAYER_FOR_SCREEN = 0x00100000 (draw over system UI / cutout)
            // PRIVATE_FLAG_TRUSTED_OVERLAY is privileged and rejected on ColorOS.
            val PRIVATE_FLAG_LAYER_FOR_SCREEN = 0x00100000
            privateFlagsField.setInt(this, current or PRIVATE_FLAG_LAYER_FOR_SCREEN)
        }
    }

    private fun removeCollapsedWindow() {
        val view = islandView ?: return
        // Clear the reference before removal so repeated teardown calls are harmless,
        // even when an OEM WindowManager throws while detaching an already-removed view.
        islandView = null
        lastParams = null
        isWindowExpanded = false
        collapseJob?.cancel()
        collapseJob = null
        runCatchingLogged(TAG, "Failed to remove view") {
            if (view.isAttachedToWindow) {
                windowManager.removeViewImmediate(view)
            }
        }
    }

    private fun collapsedParams(
        settings: SmartIslandSettings
    ): WindowManager.LayoutParams {
        val density = resources.displayMetrics.density
        val screenWidthPx = resources.displayMetrics.widthPixels.toFloat()
        val isSplitMode = if (::viewModel.isInitialized) viewModel.notifications.value.size >= 2 else false
        val bounds = collapsedTouchBounds(
            screenWidthPx.toInt(), density, settings.width, settings.height,
            settings.xOffset, settings.yOffset, isSplitMode
        )
        val w = if (isTouchableRegionSupported) WindowManager.LayoutParams.MATCH_PARENT else bounds.width
        val currentFlags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS or
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
            WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED

        val winType = overlayWindowType

        return WindowManager.LayoutParams(
            w,
            bounds.bottom,
            winType,
            currentFlags,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = if (isTouchableRegionSupported) 0 else (bounds.left + bounds.right) / 2 - screenWidthPx.toInt() / 2
            y = 0
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
            }
            applyPrivateFlags()
        }.also {
            lastParams = it
        }
    }

    private fun openNotification(notification: IslandNotification) {
        if (notification.contentIntent != null) {
            sendIntentWithOptions(this, notification.contentIntent)
        } else {
            runCatchingLogged(TAG, "Failed to launch package activity") {
                when (notification.mode) {
                    com.agupta07505.smartisland.model.IslandMode.Bluetooth -> {
                        val intent = Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        startActivity(intent)
                    }
                    com.agupta07505.smartisland.model.IslandMode.Battery -> {
                        val intent = Intent(Intent.ACTION_POWER_USAGE_SUMMARY).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        if (intent.resolveActivity(packageManager) != null) {
                            startActivity(intent)
                        } else {
                            val altIntent = Intent(android.provider.Settings.ACTION_BATTERY_SAVER_SETTINGS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            startActivity(altIntent)
                        }
                    }
                    com.agupta07505.smartisland.model.IslandMode.Hotspot -> {
                        val intent = Intent(android.provider.Settings.ACTION_WIRELESS_SETTINGS).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        startActivity(intent)
                    }
                    com.agupta07505.smartisland.model.IslandMode.Timer,
                    com.agupta07505.smartisland.model.IslandMode.Stopwatch -> {
                        val launchIntent = packageManager.getLaunchIntentForPackage(notification.packageName)
                            ?: Intent(android.provider.AlarmClock.ACTION_SHOW_TIMERS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                        if (launchIntent.resolveActivity(packageManager) != null) {
                            startActivity(launchIntent)
                        } else {
                            val clockIntent = Intent(android.provider.AlarmClock.ACTION_SHOW_ALARMS).apply {
                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            }
                            startActivity(clockIntent)
                        }
                    }
                    else -> {
                        val launchIntent = packageManager.getLaunchIntentForPackage(notification.packageName)
                        if (launchIntent != null) {
                            launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            startActivity(launchIntent)
                        } else {
                            Toast.makeText(this, "Opening ${notification.appName} (Demo)", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
        if (notification.mode != com.agupta07505.smartisland.model.IslandMode.Music) {
            notificationRepository.removeNotificationsForPackage(notification.packageName)
        }
        viewModel.collapse()
    }

    private fun launchApp(packageName: String) {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        if (launchIntent == null) {
            Toast.makeText(this, "App is no longer available", Toast.LENGTH_SHORT).show()
            return
        }
        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatchingLogged(TAG, "Failed to launch shortcut app") {
            startActivity(launchIntent)
        }
        notificationRepository.removeNotificationsForPackage(packageName)
        viewModel.collapse()
    }

    private fun openCurrentNotificationInFloatingWindow() {
        val list = viewModel.notifications.value
        val index = viewModel.selectedIndex.value
        if (list.isNotEmpty() && index in list.indices) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                Toast.makeText(this, "Floating window requires Android 7+.", Toast.LENGTH_SHORT).show()
                viewModel.collapse()
                return
            }
            val notification = list[index]
            val options = ActivityOptions.makeBasic()
            runCatchingLogged(TAG, "Failed to set launch bounds") {
                val displayMetrics = resources.displayMetrics
                val screenWidth = displayMetrics.widthPixels
                val screenHeight = displayMetrics.heightPixels
                val w = (screenWidth * 0.90f).toInt()
                val h = (screenHeight * 0.65f).toInt()
                val left = (screenWidth - w) / 2
                val top = (screenHeight - h) / 2
                options.setLaunchBounds(android.graphics.Rect(left, top, left + w, top + h))
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                runCatchingLogged(TAG, "Failed to set background activity start mode") {
                    options.setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
                }
            }
            val bundle = options.toBundle() ?: android.os.Bundle()
            bundle.putInt("android.activity.windowingMode", WINDOWING_MODE_FREEFORM)

            val fillInIntent = Intent().apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
            }

            if (notification.contentIntent != null) {
                runCatchingLogged(TAG, "Failed to send content intent") {
                    notification.contentIntent.send(this, 0, fillInIntent, null, null, null, bundle)
                }
            } else {
                runCatchingLogged(TAG, "Failed to launch package activity") {
                    val launchIntent = packageManager.getLaunchIntentForPackage(notification.packageName)
                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_MULTIPLE_TASK)
                        startActivity(launchIntent, bundle)
                    } else {
                        Toast.makeText(this, "Opening ${notification.appName} in floating window (Demo)", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            notificationRepository.removeNotification(notification.key)
            notificationRepository.sendCommand(SmartIslandCommand.CancelNotification(notification.key))
        }
        viewModel.collapse()
    }

    private fun Float.dpToPx(): Int = (this * resources.displayMetrics.density).toInt()

    private fun ComposeView.installOverlayViewTreeOwners() {
        setViewTreeLifecycleOwner(overlayOwners)
        setViewTreeViewModelStoreOwner(overlayOwners)
        setViewTreeSavedStateRegistryOwner(overlayOwners)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                OVERLAY_CHANNEL_ID,
                OVERLAY_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the Smart Island overlay running"
                setShowBadge(false)
            }
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            nm?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, com.agupta07505.smartisland.MainActivity::class.java),
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            else
                PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, OVERLAY_CHANNEL_ID)
            .setContentTitle("Smart Island is active")
            .setContentText("Tap to open Smart Island")
            .setSmallIcon(R.drawable.ic_stat_smart_island)
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setShowWhen(false)
            .build()
    }

    companion object {
        @Volatile
        var isSystemConnected: Boolean = false
            private set

        @Volatile
        private var instance: SmartIslandOverlayService? = null

        fun wakeUpOverlaySession(context: Context? = null) {
            val service = instance
            if (service != null && !service.destroyed) {
                if (service.taskRemoved) service.registerRuntimeCallbacksAfterTaskRemoval()
                service.taskRemoved = false
                if (service::viewModel.isInitialized &&
                    service.viewModel.settings.value.enabled
                ) {
                    service.startOverlaySession(service.viewModel.settings.value)
                }
            }
            if (context != null &&
                SystemServiceRecovery.isAccessibilityPermissionGranted(context) &&
                (service == null || service.destroyed || !service.startedForTaskLifecycle)
            ) {
                // Keep a started service for onTaskRemoved, but only when accessibility is
                // authorized. The overlay itself waits for the system binding callback.
                runCatchingLogged(TAG, "Failed to start overlay service from wakeUp") {
                    val intent = Intent(context, SmartIslandOverlayService::class.java)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(intent)
                    } else {
                        context.startService(intent)
                    }
                }
            }
            if (context != null) {
                SystemServiceRecovery.requestRecovery(context)
            }
        }

        fun stopOverlaySessionFromUser(context: Context? = null) {
            val service = instance
            service?.stopOverlaySession()
            service?.startedForTaskLifecycle = false
            if (context != null) {
                runCatchingLogged(TAG, "Failed to stopService") {
                    context.stopService(Intent(context, SmartIslandOverlayService::class.java))
                }
            } else {
                service?.stopSelf()
            }
        }

        fun setAppTaskActive(active: Boolean, context: Context? = null) {
            if (active) {
                wakeUpOverlaySession(context)
            } else {
                stopOverlaySessionFromUser(context)
            }
        }

        private const val TAG = "SmartIslandOverlayService"
        private const val NOTIFICATION_ID = 8105
        private const val WINDOWING_MODE_FREEFORM = 5
        private const val OVERLAY_CHANNEL_ID = "smart_island_overlay"
        private const val OVERLAY_CHANNEL_NAME = "Smart Island overlay"
        private const val AUTO_COLLAPSE_DELAY_MS = 220L
    }
}
