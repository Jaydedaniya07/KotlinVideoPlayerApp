package com.app.videostream.UI.Activity

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.app.videostream.Adapter.CategoryAdapter
import com.app.videostream.R
import com.app.videostream.databinding.ActivityMainBinding
import com.app.videostream.interfaces.OnClickHandler
import com.app.videostream.model.CategoryResponse
import com.app.videostream.model.VideoItemResponse
import com.app.videostream.utils.DeviceCapabilityDetector
import com.app.videostream.utils.JsonParser
import com.app.videostream.utils.VideoPlayerManager
import com.app.videostream.utils.VideoQualityManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : BaseActivity<ActivityMainBinding>(), OnClickHandler {

    companion object {
        private const val TAG = "MainActivity"
    }

    private lateinit var categoryAdapter: CategoryAdapter
    private lateinit var deviceCapabilityDetector: DeviceCapabilityDetector
    private lateinit var videoQualityManager: VideoQualityManager
    private lateinit var videoPlayerManager: VideoPlayerManager

    private var categories: List<CategoryResponse> = emptyList()
    private var currentPlayingVideo: VideoItemResponse? = null
    override fun getLayout(inflater: LayoutInflater) = ActivityMainBinding.inflate(layoutInflater)

    override fun initView() {
        binding.setOnClickHandler = this
        deviceCapabilityDetector = DeviceCapabilityDetector(this)
        videoQualityManager = VideoQualityManager(deviceCapabilityDetector)
        videoPlayerManager = VideoPlayerManager.getInstance(this)

        setupRecyclerView()
        loadVideosFromJson()
        logDeviceCapabilities()
    }

    override fun onViewClicked(view: View) {}

    private fun setupRecyclerView() {
        categoryAdapter = CategoryAdapter(
            activity,
            videoQualityManager = videoQualityManager,
            onVideoClick = { video ->
                handleVideoClick(video)
            }
        )

        binding.categoryRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = categoryAdapter
            setHasFixedSize(true)
            setItemViewCacheSize(20)
        }
    }

    private fun loadVideosFromJson() {
        showLoading(true)

        lifecycleScope.launch {
            try {
                categories = withContext(Dispatchers.IO) {
                    JsonParser.loadVideoCategoriesFromAssets(
                        context = this@MainActivity,
                        fileName = "videos.json"
                    )
                }

                // Update UI on main thread
                if (categories.isNotEmpty()) {
                    categoryAdapter.submitList(categories)
                    showContent()
                } else {
                    showError("No videos found in JSON")
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error loading videos from JSON", e)
                showError("Failed to load videos: ${e.message}")
            } finally {
                showLoading(false)
            }
        }
    }

    private fun handleVideoClick(video: VideoItemResponse) {
        if (currentPlayingVideo != null && currentPlayingVideo != video) {
            videoPlayerManager.stopCurrentPlayer()
        }
        currentPlayingVideo = video
    }

    private fun showLoading(show: Boolean) {
        binding.mainLoadingAnimation.visibility = if (show) View.VISIBLE else View.GONE
        binding.categoryRecyclerView.visibility = if (show) View.GONE else View.VISIBLE
        binding.errorText.visibility = View.GONE
    }

    private fun showContent() {
        binding.mainLoadingAnimation.visibility = View.GONE
        binding.categoryRecyclerView.visibility = View.VISIBLE
        binding.errorText.visibility = View.GONE
    }

    private fun showError(message: String) {
        binding.mainLoadingAnimation.visibility = View.GONE
        binding.categoryRecyclerView.visibility = View.GONE
        binding.errorText.visibility = View.VISIBLE
        binding.errorText.text = message

        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun logDeviceCapabilities() {
        val capabilities = deviceCapabilityDetector.getDeviceCapabilities()
        Log.d(TAG, "=== Device Capabilities ===")
        Log.d(TAG, "RAM: ${capabilities.totalRam / (1024 * 1024)}MB total, ${capabilities.availableRam / (1024 * 1024)}MB available")
        Log.d(TAG, "CPU Cores: ${capabilities.cpuCores}")
        Log.d(TAG, "Performance Tier: ${capabilities.performanceTier}")
        Log.d(TAG, "Screen: ${capabilities.screenWidth}x${capabilities.screenHeight}")
        Log.d(TAG, "Recommended Quality: ${videoQualityManager.getRecommendedQuality()}")
    }

    override fun onPause() {
        super.onPause()
        videoPlayerManager.stopCurrentPlayer()
        currentPlayingVideo = null
    }

    override fun onDestroy() {
        super.onDestroy()
        videoPlayerManager.release()
    }
}