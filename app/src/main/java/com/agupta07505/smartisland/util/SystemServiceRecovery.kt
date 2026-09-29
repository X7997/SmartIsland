/*
 * Smart Island (2026)
 * Copyright Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 */

package com.agupta07505.smartisland.util

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import android.service.notification.NotificationListenerService
import com.agupta07505.smartisland.service.SmartIslandNotificationListenerService
import com.agupta07505.smartisland.service.SmartIslandOverlayService

/**
 * Reconnects system-managed services after the package leaves Android's force-stopped state.
 *
 * NotificationListenerService provides an official rebind API. AccessibilityService does not,
 * Only the system may bind AccessibilityService with its window token. A normal service start
 * cannot establish that connection. PackageManager component toggles can cause ColorOS to
 * move the service into its crashed list, so this class leaves the accessibility component alone.
 */
object SystemServiceRecovery {

    private var lastAccessibilityRefreshTime: Long = 0L
    private var lastAccessibilityRebindTime: Long = 0L

    fun requestRecovery(context: Context) {
        runCatchingLogged(TAG, "System-service recovery failed") {
            val appContext = context.applicationContext
            requestNotificationListenerRebind(appContext)

            if (!SmartIslandOverlayService.isSystemConnected) {
                refreshAccessibilityComponent(appContext)
            }
        }
    }

    fun refreshAccessibilityComponent(context: Context) {
        if (!isAccessibilityPermissionGranted(context)) return
        val now = System.currentTimeMillis()
        if (now - lastAccessibilityRefreshTime < 1200L) return
        lastAccessibilityRefreshTime = now

        runCatchingLogged(TAG, "refreshAccessibilityComponent failed") {
            android.util.Log.w(
                TAG,
                "Accessibility is enabled but not connected; waiting for the system binding callback"
            )
        }
    }

    /**
     * ColorOS can keep the service in its crashed list after a recent-task swipe while the
     * user's enabled-services entry remains present. Only a caller with a one-time ADB grant of
     * WRITE_SECURE_SETTINGS may reset that stale system state. A removed entry is treated as a
     * user revocation and is never restored here.
     */
    fun rebindPreviouslyEnabledAccessibility(context: Context): Boolean {
        if (SmartIslandOverlayService.isSystemConnected ||
            context.checkSelfPermission(Manifest.permission.WRITE_SECURE_SETTINGS) != PackageManager.PERMISSION_GRANTED
        ) return false

        val resolver = context.contentResolver
        val key = Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        val original = Settings.Secure.getString(resolver, key) ?: return false
        val ownService = ComponentName(context, SmartIslandOverlayService::class.java)
        val entries = original.split(':').filter { it.isNotBlank() }
        if (entries.none { ComponentName.unflattenFromString(it) == ownService }) return false
        val originalEnabled = Settings.Secure.getInt(resolver, Settings.Secure.ACCESSIBILITY_ENABLED, 0)

        val now = android.os.SystemClock.elapsedRealtime()
        if (now - lastAccessibilityRebindTime < 10_000L) return false
        lastAccessibilityRebindTime = now

        val withoutOwnService = entries.filterNot { ComponentName.unflattenFromString(it) == ownService }
            .joinToString(":")
        var completed = false
        return try {
            check(Settings.Secure.putString(resolver, key, withoutOwnService.ifEmpty { null }))
            if (withoutOwnService.isEmpty()) {
                check(Settings.Secure.putInt(resolver, Settings.Secure.ACCESSIBILITY_ENABLED, 0))
            }
            val current = Settings.Secure.getString(resolver, key).orEmpty()
            val restored = (current.split(':').filter { it.isNotBlank() } + ownService.flattenToString())
                .distinct().joinToString(":")
            check(Settings.Secure.putString(resolver, key, restored))
            check(Settings.Secure.putInt(resolver, Settings.Secure.ACCESSIBILITY_ENABLED, 1))
            completed = true
            android.util.Log.i(TAG, "Requested accessibility rebind after stale ColorOS connection")
            true
        } catch (e: Exception) {
            android.util.Log.e(TAG, "Accessibility rebind failed", e)
            false
        } finally {
            // A failed intermediate write must not leave the user's authorized service removed.
            val current = Settings.Secure.getString(resolver, key).orEmpty()
            if (current.split(':').none { ComponentName.unflattenFromString(it) == ownService }) {
                runCatching { Settings.Secure.putString(resolver, key, original) }
            }
            if (!completed &&
                Settings.Secure.getInt(resolver, Settings.Secure.ACCESSIBILITY_ENABLED, 0) != originalEnabled
            ) {
                runCatching { Settings.Secure.putInt(resolver, Settings.Secure.ACCESSIBILITY_ENABLED, originalEnabled) }
            }
        }
    }

    private fun requestNotificationListenerRebind(context: Context) {
        if (!isNotificationListenerPermissionGranted(context) ||
            SmartIslandNotificationListenerService.isSystemConnected
        ) return

        runCatchingLogged(TAG, "Notification-listener rebind failed") {
            NotificationListenerService.requestRebind(
                ComponentName(context, SmartIslandNotificationListenerService::class.java)
            )
        }
    }

    fun isAccessibilityPermissionGranted(context: Context): Boolean {
        val expected = ComponentName(context, SmartIslandOverlayService::class.java)
        val enabledServices = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false
        return enabledServices.split(':').any {
            ComponentName.unflattenFromString(it) == expected ||
            (it.contains(context.packageName) && it.contains("SmartIslandOverlayService"))
        }
    }

    fun isNotificationListenerPermissionGranted(context: Context): Boolean {
        val expected = ComponentName(context, SmartIslandNotificationListenerService::class.java)
        val enabledListeners = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        ) ?: return false
        return enabledListeners.split(':').any {
            ComponentName.unflattenFromString(it) == expected
        }
    }

    private const val TAG = "SystemServiceRecovery"
}
