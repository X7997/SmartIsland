/*
 * Smart Island (2026)
 * © Animesh Gupta — github.com/agupta07505
 * Licensed under the GNU GPL v3 License
 * Do not remove or alter this notice. - Per GPL-3.0 Section 4 & Section 5
 */

package com.agupta07505.smartisland

import android.app.Application
import android.os.Build
import dagger.hilt.android.HiltAndroidApp
import org.lsposed.hiddenapibypass.HiddenApiBypass

@HiltAndroidApp
class SmartIslandApp : Application() {
    override fun onCreate() {
        super.onCreate()
        bypassHiddenApis()
    }

    private fun bypassHiddenApis() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                HiddenApiBypass.addHiddenApiExemptions("L")
                android.util.Log.d("SmartIslandApp", "Successfully bypassed Hidden API restrictions via HiddenApiBypass")
            } catch (e: Throwable) {
                android.util.Log.e("SmartIslandApp", "Failed to bypass Hidden API restrictions via HiddenApiBypass", e)
            }
        }
    }
}
