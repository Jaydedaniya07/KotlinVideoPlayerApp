package com.app.videostream.Adapter

import android.app.Activity
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.app.videostream.R
import com.app.videostream.databinding.ListItemVideoBinding
import com.app.videostream.enums.VideoQuality
import com.app.videostream.model.VideoItemResponse
import com.app.videostream.utils.VideoPlayerManager
import com.app.videostream.utils.VideoQualityManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.PlaybackException

class VideoAdapter(
    private val activity: Activity,
    private val videos: List<VideoItemResponse>,
    private val videoQualityManager: VideoQualityManager,
    private val onVideoClick: (VideoItemResponse, Int) -> Unit,
    private val onVideoStop: (Int) -> Unit
) : RecyclerView.Adapter<VideoAdapter.ViewHolder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val binding = ListItemVideoBinding.inflate(LayoutInflater.from(activity), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount() = videos.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val videoData = videos[position]
        holder.setData(
            position,
            activity,
            this,
            holder,
            videoData,
            videos,
            videoQualityManager,
            onVideoClick,
            onVideoStop
        )
    }

    fun stopVideo(holder: ViewHolder, position: Int) {
        if (position == holder.currentPlayingPosition) {
            holder.currentPlayer?.stop()
            holder.currentPlayer?.release()
            holder.currentPlayer = null
            holder.currentPlayingPosition = -1
            notifyItemChanged(position)
        }
    }

    class ViewHolder(val binding: ListItemVideoBinding) :
        RecyclerView.ViewHolder(binding.root) {

        var currentPlayingPosition = -1
        var currentPlayer: ExoPlayer? = null

        fun setData(
            position: Int,
            activity: Activity,
            adapter: VideoAdapter,
            holder: ViewHolder,
            videoData: VideoItemResponse,
            videos: List<VideoItemResponse>,
            videoQualityManager: VideoQualityManager,
            onVideoClick: (VideoItemResponse, Int) -> Unit,
            onVideoStop: (Int) -> Unit
        ) {
            binding.videoTitle.text = videoData.title
            binding.videoDuration.text = videoData.duration

            // Load thumbnail with Glide (optimized for memory)
            val thumbnailSize = videoQualityManager.getThumbnailSize()
            val requestOptions = RequestOptions()
                .override(thumbnailSize.first, thumbnailSize.second)
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .placeholder(R.drawable.placeholder_thumbnail)
                .error(R.drawable.error_thumbnail)

            Glide.with(itemView.context)
                .load(videoData.thumbnailUrl)
                .apply(requestOptions)
                .into(binding.videoThumbnail)

            // Reset views
            binding.videoPlayer.visibility = View.GONE
            binding.videoThumbnail.visibility = View.VISIBLE
            binding.playButtonContainer.visibility = View.VISIBLE
            binding.loadingAnimation.visibility = View.GONE

            // If this video is currently playing, show player
            if (position == currentPlayingPosition && currentPlayer != null) {
                binding.videoPlayer.player = currentPlayer
                binding.videoPlayer.visibility = View.VISIBLE
                binding.videoThumbnail.visibility = View.GONE
                binding.playButtonContainer.visibility = View.GONE
            }

            // Handle play button click
            binding.playButtonContainer.setOnClickListener {
                // Check if device should only show thumbnail
                if (videoQualityManager.shouldShowThumbnailOnly()) {
                    Toast.makeText(
                        itemView.context,
                        "Device memory is low. Video playback is disabled.",
                        Toast.LENGTH_SHORT
                    ).show()
                    return@setOnClickListener
                }

                // Stop previous video
                if (currentPlayingPosition != -1 && currentPlayingPosition != position) {
                    adapter.stopVideo(holder, currentPlayingPosition)
                    onVideoStop(currentPlayingPosition)
                }

                // Show loading
                binding.loadingAnimation.visibility = View.VISIBLE
                binding.playButtonContainer.visibility = View.GONE

                // Get appropriate video URL based on device capability
                val urlMap = mapOf(
                    VideoQuality.HIGH to videoData.videoUrls.high,
                    VideoQuality.MEDIUM to videoData.videoUrls.medium,
                    VideoQuality.LOW to videoData.videoUrls.low
                ).filterValues { it != null }

                val videoUrl = videoQualityManager.getVideoUrl(urlMap as Map<VideoQuality, String>)

                if (videoUrl.isEmpty()) {
                    Toast.makeText(
                        itemView.context,
                        "No suitable video quality available",
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.loadingAnimation.visibility = View.GONE
                    binding.playButtonContainer.visibility = View.VISIBLE
                    return@setOnClickListener
                }

                try {
                    // Create player through manager (ensures only one plays)
                    val videoPlayerManager = VideoPlayerManager.getInstance(itemView.context)

                    currentPlayer = videoPlayerManager.createPlayer(
                        onPlayerReady = {
                            // Hide loading, show player
                            binding.loadingAnimation.visibility = View.GONE
                            binding.videoThumbnail.visibility = View.GONE
                            binding.videoPlayer.visibility = View.VISIBLE
                            binding.videoPlayer.player = currentPlayer

                            Log.d("VideoAdapter", "Video ready: ${videoData.title}")
                        },
                        onPlayerError = { error ->
                            handlePlayerError(error)
                        },
                        onBuffering = { isBuffering ->
                            binding.loadingAnimation.visibility = if (isBuffering) View.VISIBLE else View.GONE
                        }
                    )

                    // Play video
                    videoPlayerManager.playVideo(currentPlayer!!, videoUrl)

                    currentPlayingPosition = position
                    onVideoClick(videoData, position)

                } catch (e: Exception) {
                    Log.e("VideoAdapter", "Error playing video", e)
                    Toast.makeText(
                        itemView.context,
                        "Failed to play video: ${e.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                    binding.loadingAnimation.visibility = View.GONE
                    binding.playButtonContainer.visibility = View.VISIBLE
                }
            }
        }

        private fun handlePlayerError(error: PlaybackException) {
            binding.loadingAnimation.visibility = View.GONE
            binding.playButtonContainer.visibility = View.VISIBLE
            binding.videoThumbnail.visibility = View.VISIBLE
            binding.videoPlayer.visibility = View.GONE

            val errorMessage = when (error.errorCode) {
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT ->
                    "Network connection failed. Please check your internet."

                PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS ->
                    "Video not available."

                PlaybackException.ERROR_CODE_DECODER_INIT_FAILED ->
                    "Device cannot decode this video format."

                else -> "Playback error: ${error.message}"
            }

            Toast.makeText(itemView.context, errorMessage, Toast.LENGTH_SHORT).show()
            Log.e("VideoAdapter", "Playback error", error)
        }
    }

    override fun onViewRecycled(holder: ViewHolder) {
        super.onViewRecycled(holder)
        if (holder.absoluteAdapterPosition == holder.currentPlayingPosition) {
            holder.currentPlayer?.stop()
        }
    }
}