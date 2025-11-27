package com.app.videostream.utils

import com.app.videostream.enums.PerformanceTier
import com.app.videostream.enums.ThumbnailQuality
import com.app.videostream.enums.VideoQuality

class VideoQualityManager(private val deviceCapabilityDetector: DeviceCapabilityDetector) {

    /**
     * Get recommended video quality based on device performance
     */
    fun getRecommendedQuality(): VideoQuality {
        val capabilities = deviceCapabilityDetector.getDeviceCapabilities()
        val canPlayHighQuality = deviceCapabilityDetector.canPlayHighQualityVideo()
        val memoryPercentage = deviceCapabilityDetector.getAvailableMemoryPercentage()

        return when {
            // High-end devices with plenty of memory
            capabilities.performanceTier == PerformanceTier.HIGH &&
                    canPlayHighQuality &&
                    memoryPercentage > 40 -> VideoQuality.HIGH

            // Mid-range devices or high-end with limited memory
            capabilities.performanceTier == PerformanceTier.MEDIUM &&
                    canPlayHighQuality &&
                    memoryPercentage > 30 -> VideoQuality.MEDIUM

            // Low-end devices or devices with very limited memory
            capabilities.performanceTier == PerformanceTier.LOW &&
                    canPlayHighQuality &&
                    memoryPercentage > 20 -> VideoQuality.LOW

            // Fall back to thumbnail if can't play video
            else -> VideoQuality.THUMBNAIL
        }
    }

    /**
     * Get the appropriate video URL based on device capability
     */
    fun getVideoUrl(videoUrls: Map<VideoQuality, String>): String {
        val recommendedQuality = getRecommendedQuality()

        // Try to get the recommended quality, fall back to lower qualities if not available
        return when (recommendedQuality) {
            VideoQuality.HIGH -> videoUrls[VideoQuality.HIGH]
                ?: videoUrls[VideoQuality.MEDIUM]
                ?: videoUrls[VideoQuality.LOW]
                ?: ""
            VideoQuality.MEDIUM -> videoUrls[VideoQuality.MEDIUM]
                ?: videoUrls[VideoQuality.LOW]
                ?: videoUrls[VideoQuality.HIGH]
                ?: ""
            VideoQuality.LOW -> videoUrls[VideoQuality.LOW]
                ?: videoUrls[VideoQuality.MEDIUM]
                ?: ""
            VideoQuality.THUMBNAIL -> "" // Don't play video, show thumbnail
        }
    }

    /**
     * Check if should only show thumbnail (no video playback)
     */
    fun shouldShowThumbnailOnly(): Boolean {
        return getRecommendedQuality() == VideoQuality.THUMBNAIL
    }

    /**
     * Get recommended thumbnail quality
     */
    fun getThumbnailQuality(): ThumbnailQuality {
        val canLoadHighQuality = deviceCapabilityDetector.canLoadHighQualityThumbnail()
        val isLowMemory = deviceCapabilityDetector.isLowMemoryDevice()

        return when {
            canLoadHighQuality && !isLowMemory -> ThumbnailQuality.HIGH
            canLoadHighQuality -> ThumbnailQuality.MEDIUM
            else -> ThumbnailQuality.LOW
        }
    }

    /**
     * Get thumbnail dimensions based on quality
     */
    fun getThumbnailSize(): Pair<Int, Int> {
        return when (getThumbnailQuality()) {
            ThumbnailQuality.HIGH -> Pair(800, 1200)   // High resolution
            ThumbnailQuality.MEDIUM -> Pair(400, 600)  // Medium resolution
            ThumbnailQuality.LOW -> Pair(200, 300)     // Low resolution
        }
    }
}