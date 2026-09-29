/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.OnBackPressedCallback
import com.agupta07505.smartisland.data.INotificationRepository
import com.agupta07505.smartisland.data.SmartIslandSettingsRepository
import com.agupta07505.smartisland.service.SmartIslandOverlayService
import com.agupta07505.smartisland.ui.SmartIslandHomeScreen
import com.agupta07505.smartisland.ui.SmartIslandTheme
import com.agupta07505.smartisland.util.SystemServiceRecovery
import dagger.hilt.android.AndroidEntryPoint
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var settingsRepository: SmartIslandSettingsRepository
    @Inject lateinit var notificationRepository: INotificationRepository
    private var recoveryJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                moveTaskToBack(true)
            }
        })

        setContent {
            SmartIslandTheme {
                SmartIslandHomeScreen(
                    repository = settingsRepository,
                    notificationRepository = notificationRepository
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        recoveryJob?.cancel()
        recoveryJob = lifecycleScope.launch {
            val settings = settingsRepository.settings.first()
            if (settings.enabled) {
                SmartIslandOverlayService.wakeUpOverlaySession(this@MainActivity)
                // Give a normal system bind time to finish before resetting a stale OEM state.
                delay(1_500L)
                if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED) &&
                    !SmartIslandOverlayService.isSystemConnected
                ) {
                    SystemServiceRecovery.rebindPreviouslyEnabledAccessibility(this@MainActivity)
                }
            }
        }
        SystemServiceRecovery.requestRecovery(this)
    }

    override fun onPause() {
        recoveryJob?.cancel()
        super.onPause()
    }
}
