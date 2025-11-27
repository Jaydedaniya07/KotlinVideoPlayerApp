package com.app.videostream.utils

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.source.DefaultMediaSourceFactory
import com.google.android.exoplayer2.upstream.DefaultDataSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.upstream.cache.CacheDataSource
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor
import com.google.android.exoplayer2.upstream.cache.SimpleCache
import java.io.File

/**
 * Singleton class that manages video playback
 * Ensures only one video plays at a time
 * Implements caching for better performance
 */
class VideoPlayerManager private constructor(context: Context) {

    private val appContext = context.applicationContext
    private var currentPlayer: ExoPlayer? = null
    private var currentPlayerListener: Player.Listener? = null

    // Video cache to reduce network usage and improve loading speed
    private val videoCache: SimpleCache by lazy {
        val cacheDir = File(appContext.cacheDir, "video_cache")
        val cacheSize = 100 * 1024 * 1024L // 100 MB cache
        val cacheEvictor = LeastRecentlyUsedCacheEvictor(cacheSize)
        SimpleCache(cacheDir, cacheEvictor)
    }

    companion object {
        @Volatile
        private var instance: VideoPlayerManager? = null
        private const val TAG = "VideoPlayerManager"

        /**
         * Get singleton instance
         */
        fun getInstance(context: Context): VideoPlayerManager {
            return instance ?: synchronized(this) {
                instance ?: VideoPlayerManager(context).also { instance = it }
            }
        }
    }

    /**
     * Creates a new ExoPlayer instance with caching enabled
     * Automatically stops any currently playing video
     *
     * @param onPlayerReady Callback when player is ready to play
     * @param onPlayerError Callback when playback error occurs
     * @param onBuffering Callback for buffering state changes
     * @return Configured ExoPlayer instance
     */
    fun createPlayer(
        onPlayerReady: () -> Unit = {},
        onPlayerError: (PlaybackException) -> Unit = {},
        onBuffering: (Boolean) -> Unit = {}
    ): ExoPlayer {
        // Stop current player if any
        stopCurrentPlayer()

        // Create HTTP data source with timeouts
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(10000)  // 10 seconds
            .setReadTimeoutMs(10000)      // 10 seconds

        // Create cache data source that wraps HTTP source
        val cacheDataSourceFactory = CacheDataSource.Factory()
            .setCache(videoCache)
            .setUpstreamDataSourceFactory(httpDataSourceFactory)
            .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

        // Create default data source
        val dataSourceFactory = DefaultDataSource.Factory(
            appContext,
            cacheDataSourceFactory
        )

        // Create media source factory with caching
        val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)

        // Build ExoPlayer
        val player = ExoPlayer.Builder(appContext)
            .setMediaSourceFactory(mediaSourceFactory)
            .build()

        // Add listener for player events
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> {
                        onPlayerReady()
                        onBuffering(false)
                        Log.d(TAG, "Player ready to play")
                    }
                    Player.STATE_BUFFERING -> {
                        onBuffering(true)
                        Log.d(TAG, "Player buffering...")
                    }
                    Player.STATE_ENDED -> {
                        Log.d(TAG, "Playback ended")
                    }
                    Player.STATE_IDLE -> {
                        Log.d(TAG, "Player idle")
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e(TAG, "Player error: ${error.message}", error)
                onPlayerError(error)
                onBuffering(false)
            }
        }

        player.addListener(listener)

        // Store current player and listener
        currentPlayer = player
        currentPlayerListener = listener

        return player
    }

    /**
     * Stops and releases the current player
     * Call this before playing a new video
     */
    fun stopCurrentPlayer() {
        currentPlayer?.let { player ->
            // Remove listener
            currentPlayerListener?.let { listener ->
                player.removeListener(listener)
            }

            // Stop playback
            player.stop()

            // Release resources
            player.release()

            Log.d(TAG, "Current player stopped and released")
        }

        currentPlayer = null
        currentPlayerListener = null
    }

    /**
     * Prepares and plays a video URL
     *
     * @param player ExoPlayer instance created by createPlayer()
     * @param videoUrl URL of the video to play
     */
    fun playVideo(player: ExoPlayer, videoUrl: String) {
        try {
            val mediaItem = MediaItem.fromUri(Uri.parse(videoUrl))
            player.setMediaItem(mediaItem)
            player.prepare()
            player.playWhenReady = true
            Log.d(TAG, "Playing video: $videoUrl")
        } catch (e: Exception) {
            Log.e(TAG, "Error playing video", e)
            throw e
        }
    }

    /**
     * Releases all resources including cache
     * Call this in onDestroy of your Activity
     */
    fun release() {
        stopCurrentPlayer()
        try {
            videoCache.release()
            Log.d(TAG, "Video cache released")
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing cache", e)
        }
    }

    /**
     * Clears the video cache
     * Useful for freeing up storage space
     */
    fun clearCache() {
        try {
            val cacheDir = File(appContext.cacheDir, "video_cache")
            if (cacheDir.exists()) {
                cacheDir.deleteRecursively()
                Log.d(TAG, "Video cache cleared")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing cache", e)
        }
    }

    /**
     * Gets the current cache size in bytes
     *
     * @return Cache size in bytes
     */
    fun getCacheSize(): Long {
        return try {
            videoCache.cacheSpace
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Checks if a video is currently playing
     *
     * @return true if video is playing
     */
    fun isPlaying(): Boolean {
        return currentPlayer?.isPlaying ?: false
    }

    /**
     * Pauses the current video
     */
    fun pause() {
        currentPlayer?.pause()
    }

    /**
     * Resumes the current video
     */
    fun resume() {
        currentPlayer?.play()
    }
}