package com.app.videostream.utils

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.StatFs
import android.util.DisplayMetrics
import android.view.WindowManager
import com.app.videostream.enums.PerformanceTier
import com.app.videostream.model.DeviceCapabilities

class DeviceCapabilityDetector(private val context: Context) {

    private val activityManager: ActivityManager by lazy {
        context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
    }

    fun getDeviceCapabilities(): DeviceCapabilities {
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)

        val statFs = StatFs(context.filesDir.absolutePath)
        val totalStorage = statFs.totalBytes
        val availableStorage = statFs.availableBytes

        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)

        val cpuCores = Runtime.getRuntime().availableProcessors()

        return DeviceCapabilities(
            totalRam = memoryInfo.totalMem,
            availableRam = memoryInfo.availMem,
            totalStorage = totalStorage,
            availableStorage = availableStorage,
            screenWidth = displayMetrics.widthPixels,
            screenHeight = displayMetrics.heightPixels,
            screenDensity = displayMetrics.density,
            sdkVersion = Build.VERSION.SDK_INT,
            cpuCores = cpuCores,
            performanceTier = determinePerformanceTier(memoryInfo.totalMem, cpuCores)
        )
    }

    private fun determinePerformanceTier(totalRam: Long, cpuCores: Int): PerformanceTier {
        val ramInGB = totalRam / (1024 * 1024 * 1024)

        return when {
            ramInGB >= 6 && cpuCores >= 8 -> PerformanceTier.HIGH
            ramInGB >= 3 && cpuCores >= 4 -> PerformanceTier.MEDIUM
            else -> PerformanceTier.LOW
        }
    }

    fun canPlayHighQualityVideo(): Boolean {
        val capabilities = getDeviceCapabilities()
        val availableRamInMB = capabilities.availableRam / (1024 * 1024)

        return when (capabilities.performanceTier) {
            PerformanceTier.HIGH -> availableRamInMB > 500
            PerformanceTier.MEDIUM -> availableRamInMB > 300
            PerformanceTier.LOW -> availableRamInMB > 200
        }
    }

    fun canLoadHighQualityThumbnail(): Boolean {
        val capabilities = getDeviceCapabilities()
        val availableRamInMB = capabilities.availableRam / (1024 * 1024)

        // Thumbnails require less memory than videos
        return availableRamInMB > 100
    }

    fun isLowMemoryDevice(): Boolean {
        return activityManager.isLowRamDevice ||
                (getDeviceCapabilities().totalRam / (1024 * 1024 * 1024)) < 2
    }

    fun getAvailableMemoryPercentage(): Float {
        val capabilities = getDeviceCapabilities()
        return (capabilities.availableRam.toFloat() / capabilities.totalRam.toFloat()) * 100
    }
}