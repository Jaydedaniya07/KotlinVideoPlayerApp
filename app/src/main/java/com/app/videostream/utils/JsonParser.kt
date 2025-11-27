package com.app.videostream.utils

import android.content.Context
import com.app.videostream.enums.VideoQuality
import com.app.videostream.model.CategoryResponse
import com.app.videostream.model.VideoItemResponse
import com.app.videostream.model.VideoResponse
import com.google.common.reflect.TypeToken
import com.google.gson.Gson
import java.io.IOException
import kotlin.collections.map

object JsonParser {

    private val gson = Gson()

    /**
     * Read JSON from assets folder
     */
    fun readJsonFromAssets(context: Context, fileName: String): String {
        return try {
            context.assets.open(fileName).bufferedReader().use { it.readText() }
        } catch (e: IOException) {
            e.printStackTrace()
            ""
        }
    }

    /**
     * Parse JSON string to VideoCategory list
     */
    fun parseVideoCategoriesFromJson(jsonString: String): List<CategoryResponse> {
        return try {
            val videoResponse = gson.fromJson(jsonString, VideoResponse::class.java)
            videoResponse.categories.map { it.toDomainModel() }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Load and parse JSON from assets in one step
     */
    fun loadVideoCategoriesFromAssets(context: Context, fileName: String = "videos.json"): List<CategoryResponse> {
        val jsonString = readJsonFromAssets(context, fileName)
        return parseVideoCategoriesFromJson(jsonString)
    }

    /**
     * Parse JSON array directly (if your JSON is just an array of categories)
     */
    fun parseCategoriesArray(jsonString: String): List<CategoryResponse> {
        return try {
            val type = object : TypeToken<List<CategoryResponse>>() {}.type
            val categories: List<CategoryResponse> = gson.fromJson(jsonString, type)
            categories.map { it.toDomainModel() }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}

/**
 * Extension functions to convert Response models to Domain models
 */
fun VideoItemResponse.toDomainModel(): VideoItemResponse {
    return VideoItemResponse(
        id = this.id,
        title = this.title,
        duration = this.duration,
        thumbnailUrl = this.thumbnailUrl,
        videoUrls = this.videoUrls,
        categoryId = this.categoryId
    )
}

fun CategoryResponse.toDomainModel(): CategoryResponse {
    return CategoryResponse(
        id = this.id,
        name = this.name,
        videos = this.videos.map { it.toDomainModel() }
    )
}