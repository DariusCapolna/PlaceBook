package com.darius.placebook.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.Transformations
import com.darius.placebook.model.Bookmark
import com.darius.placebook.repository.BookmarkRepo
import com.darius.placebook.util.ImageUtils
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

class BookmarkDetailsViewModel(application: Application) : AndroidViewModel(application) {

    private val bookmarkRepo = BookmarkRepo(getApplication())
    private var bookmarkMarkerView: LiveData<BookmarkMarkerView>? = null

    fun getBookmark(bookmarkId: Long): LiveData<BookmarkMarkerView>? {
        if (bookmarkMarkerView == null)
            mapBookmarkToBookmarkView(bookmarkId)
        return bookmarkMarkerView
    }

    fun updateBookmark(bookmarkMarkerView: BookmarkMarkerView) {
        GlobalScope.launch {
            val bookmark = bookmarkViewToBookmark(bookmarkMarkerView)
            bookmark?.let {
                bookmarkRepo.updateBookmark(bookmark)
            }
        }
    }

    fun getCategoryResourceId(category: String): Int? {
        return bookmarkRepo.getCategoryResourceId(category)
    }

    fun getCategories(): List<String> {
        return bookmarkRepo.categories
    }

    private fun bookmarkToBookmarkView(bookmark: Bookmark): BookmarkMarkerView =
        BookmarkMarkerView(
            bookmark.id,
            bookmark.name,
            bookmark.phone,
            bookmark.address,
            bookmark.notes,
            bookmark.category,
        )

    private fun mapBookmarkToBookmarkView(bookmarkId: Long) {
        val bookmark = bookmarkRepo.getLiveBookmark(bookmarkId)
        bookmarkMarkerView = Transformations.map(bookmark)
        { repoBookmark ->
            bookmarkToBookmarkView(repoBookmark)
        }
    }


    private fun bookmarkViewToBookmark(bookmarkMarkerView: BookmarkMarkerView): Bookmark? {
        val bookmark = bookmarkMarkerView.id?.let {
            bookmarkRepo.getBookmark(it)
        }

        if (bookmark != null) {
            bookmark.id = bookmarkMarkerView.id
            bookmark.name = bookmarkMarkerView.name
            bookmark.phone = bookmarkMarkerView.phone
            bookmark.address = bookmarkMarkerView.address
            bookmark.notes = bookmarkMarkerView.notes
            bookmark.category = bookmarkMarkerView.category
        }
        return bookmark
    }

    data class BookmarkMarkerView(
        var id: Long? = null,
        var name: String = "",
        var phone: String = "",
        var address: String = "",
        var notes: String = "",
        var category: String = "",
    ) {
        fun getImage(context: Context) = id?.let {
            ImageUtils.loadBitmapFromFile(context, Bookmark.generateImageFilename(it))
        }

        fun setImage(context: Context, image: Bitmap) {
            id?.let {
                ImageUtils.saveBitmapToFile(
                    context, image,
                    Bookmark.generateImageFilename(it)
                )
            }
        }
    }
}