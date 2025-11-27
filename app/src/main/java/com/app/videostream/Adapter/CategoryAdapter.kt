package com.app.videostream.Adapter

import android.app.Activity
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.app.videostream.databinding.ListItemCategoryBinding
import com.app.videostream.databinding.ListItemVideoBinding
import com.app.videostream.model.CategoryResponse
import com.app.videostream.model.VideoItemResponse
import com.app.videostream.utils.VideoQualityManager

class CategoryAdapter(
    private val activity: Activity,
    private val videoQualityManager: VideoQualityManager,
    private val onVideoClick: (VideoItemResponse) -> Unit
) : RecyclerView.Adapter<CategoryAdapter.ViewHolder>() {

    private var categories: List<CategoryResponse> = emptyList()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val binding = ListItemCategoryBinding.inflate(LayoutInflater.from(activity), parent, false)
        return ViewHolder(binding)
    }

    override fun getItemCount() = categories.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val categoryData = categories[position]
        holder.setData(position, activity, categoryData, categories, videoQualityManager, onVideoClick)
    }

    fun submitList(newCategories: List<CategoryResponse>) {
        categories = newCategories
        notifyDataSetChanged()
    }

    class ViewHolder(val binding: ListItemCategoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        private var currentPlayingPosition: Pair<Int, Int>? = null
        private var videoAdapter: VideoAdapter? = null

        fun setData(
            position: Int,
            activity: Activity,
            categoryData: CategoryResponse,
            categories: List<CategoryResponse>,
            videoQualityManager: VideoQualityManager,
            onVideoClick: (VideoItemResponse) -> Unit
        ) {
            binding.videoTitle.text = categoryData.name

            // Setup horizontal RecyclerView for videos
            videoAdapter = VideoAdapter(
                activity,
                videos = categoryData.videos,
                videoQualityManager = videoQualityManager,
                onVideoClick = { video, videoPosition ->
                    //stopPreviousVideo()
                    currentPlayingPosition = Pair(adapterPosition, videoPosition)
                    onVideoClick(video)
                },
                onVideoStop = { videoPosition ->
                    if (currentPlayingPosition?.first == absoluteAdapterPosition &&
                        currentPlayingPosition?.second == videoPosition) {
                        currentPlayingPosition = null
                    }
                }
            )

            binding.videoRecyclerView.apply {
                layoutManager = LinearLayoutManager(
                    itemView.context,
                    LinearLayoutManager.HORIZONTAL,
                    false
                )
                adapter = videoAdapter
                setHasFixedSize(true)

                // Optimize RecyclerView
                setItemViewCacheSize(10)
                isNestedScrollingEnabled = false
            }
        }

        /*private fun stopPreviousVideo() {
            currentPlayingPosition?.let { (catPos, vidPos) ->
                // Find the adapter for the previous category and stop its video
                val prevCategoryHolder = (itemView.parent as? RecyclerView)
                    ?.findViewHolderForAdapterPosition(catPos) as? CategoryViewHolder
                prevCategoryHolder?.videoAdapter?.stopVideo(vidPos)
            }
        }*/
    }
}