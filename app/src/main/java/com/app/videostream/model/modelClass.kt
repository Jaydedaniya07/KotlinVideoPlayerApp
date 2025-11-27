package com.app.videostream.model

import com.app.videostream.enums.PerformanceTier
import com.google.gson.annotations.SerializedName

data class DeviceCapabilities(
    val totalRam: Long,
    val availableRam: Long,
    val totalStorage: Long,
    val availableStorage: Long,
    val screenWidth: Int,
    val screenHeight: Int,
    val screenDensity: Float,
    val sdkVersion: Int,
    val cpuCores: Int,
    val performanceTier: PerformanceTier
)

data class VideoResponse(
    @SerializedName("categories") val categories: List<CategoryResponse>
)

data class CategoryResponse(
    @SerializedName("id") val id: String,
    @SerializedName("name") val name: String,
    @SerializedName("videos") val videos: List<VideoItemResponse>
)

data class VideoItemResponse(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("duration") val duration: String,
    @SerializedName("thumbnail_url") val thumbnailUrl: String,
    @SerializedName("video_urls") val videoUrls: VideoUrlsResponse,
    @SerializedName("category_id") val categoryId: String
)

data class VideoUrlsResponse(
    @SerializedName("high") val high: String?,
    @SerializedName("medium") val medium: String?,
    @SerializedName("low") val low: String?
)