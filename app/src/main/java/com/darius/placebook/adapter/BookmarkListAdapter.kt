package com.darius.placebook.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.darius.placebook.R
import com.darius.placebook.databinding.BookmarkItemBinding
import com.darius.placebook.ui.MapsActivity
import com.darius.placebook.viewmodel.MapsViewModel.BookmarkView

class BookmarkListAdapter(
    private var bookmarkMarkerData: List<BookmarkView>?,
    private val mapsActivity: MapsActivity
) : RecyclerView.Adapter<BookmarkListAdapter.ViewHolder>() {

    class ViewHolder(val binding: BookmarkItemBinding, private val mapsActivity: MapsActivity) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                val bookmarkView = itemView.tag as BookmarkView
                mapsActivity.moveToBookmark(bookmarkView)
            }
        }
    }

    fun setBookmarkData(bookmarks: List<BookmarkView>) {
        this.bookmarkMarkerData = bookmarks
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        val binding = BookmarkItemBinding.inflate(layoutInflater, parent, false)
        return ViewHolder(binding, mapsActivity)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        bookmarkMarkerData?.let { list ->
            val bookmarkViewData = list[position]
            holder.binding.root.tag = bookmarkViewData
            holder.binding.bookmarkData = bookmarkViewData
            bookmarkViewData.categoryResourceId?.let {
                holder.binding.bookmarkIcon.setImageResource(it)
            }
        }
    }

    override fun getItemCount(): Int = bookmarkMarkerData?.size ?: 0


}