package com.darius.placebook.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.media.Image
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.content.FileProvider
import androidx.databinding.DataBindingUtil
import com.darius.placebook.R
import com.darius.placebook.databinding.ActivityBookmarkDetailsBinding
import com.darius.placebook.util.ImageUtils
import com.darius.placebook.viewmodel.BookmarkDetailsViewModel
import java.io.File
import java.io.IOException

class BookmarkDetailsActivity : AppCompatActivity(),
    PhotoOptionDialogFragment.PhotoOptionDialogListener {

    private lateinit var dataBinding: ActivityBookmarkDetailsBinding
    private val bookmarkDetailsViewModel by viewModels<BookmarkDetailsViewModel>()
    private var bookmarkDetailsMarkerView: BookmarkDetailsViewModel.BookmarkMarkerView? = null
    private var photoFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        dataBinding = DataBindingUtil.setContentView(this, R.layout.activity_bookmark_details)
        setupToolbar()
        getIntentData()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_bookmark_details, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean =
        when (item.itemId) {
            R.id.action_save -> {
                saveChanges()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            when (requestCode) {
                REQUEST_CODE_CAPTURE -> {
                    val photoFile = photoFile ?: return
                    val uri = FileProvider.getUriForFile(
                        this,
                        "com.darius.placebook.fileprovider",
                        photoFile
                    )
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION

                    val image = getImageWithPath(photoFile.absolutePath)
                    val bitmap = ImageUtils.rotateImageIfRequired(this, image, uri)
                    updateImage(bitmap)
                }
                REQUEST_GALLERY_IMAGE -> if (data != null && data.data != null) {
                    val imageUri = data.data as Uri
                    val image = getImageWithAuthority(imageUri)
                    image?.let {
                        val bitmap = ImageUtils.rotateImageIfRequired(this, it, imageUri)
                        updateImage(bitmap)
                    }
                }
            }
        }
    }

    private fun getIntentData() {
        val bookmarkId = intent.getLongExtra(MapsActivity.Companion.EXTRA_BOOKMARK_ID, 0)

        bookmarkDetailsViewModel.getBookmark(bookmarkId)?.observe(
            this,
            {
                it?.let {
                    bookmarkDetailsMarkerView = it
                    dataBinding.bookmarkDetailsView = it
                    populateImageView()
                    populateCategoryList()
                }
            },
        )
    }

    private fun populateCategoryList() {
        val bookmarkView = bookmarkDetailsMarkerView ?: return
        val resourceId = bookmarkDetailsViewModel.getCategoryResourceId(bookmarkView.category)

        resourceId?.let { dataBinding.imageViewCategory.setImageResource(it) }
        val categories = bookmarkDetailsViewModel.getCategories()
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, categories)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        dataBinding.spinnerCategory.adapter = adapter

        val placeCategory = bookmarkView.category
        dataBinding.spinnerCategory.setSelection(adapter.getPosition(placeCategory))

        dataBinding.spinnerCategory.post {
            dataBinding.spinnerCategory.onItemSelectedListener =
                object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: AdapterView<*>,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        val category = parent.getItemAtPosition(position) as String
                        val resourceId = bookmarkDetailsViewModel.getCategoryResourceId(category)
                        resourceId?.let {
                            dataBinding.imageViewCategory.setImageResource(it)
                        }
                    }

                    override fun onNothingSelected(p0: AdapterView<*>?) {
                        // not needed to be implemented
                    }
                }
        }
    }

    private fun saveChanges() {
        val name = dataBinding.editTextName.text.toString()

        if (name.isEmpty()) return

        bookmarkDetailsMarkerView?.let { bookmarkView ->
            bookmarkView.name = dataBinding.editTextName.text.toString()
            bookmarkView.notes = dataBinding.editTextNotes.text.toString()
            bookmarkView.address = dataBinding.editTextAddress.text.toString()
            bookmarkView.phone = dataBinding.editTextPhone.text.toString()
            bookmarkView.category = dataBinding.spinnerCategory.selectedItem as String
            bookmarkDetailsViewModel.updateBookmark(bookmarkView)
        }
        finish()
    }

    private fun populateImageView() {
        bookmarkDetailsMarkerView?.let { bookmarkView ->
            val placeImage = bookmarkView.getImage(this)
            placeImage?.let {
                dataBinding.imageViewPlace.setImageBitmap(placeImage)
            }
        }
        dataBinding.imageViewPlace.setOnClickListener {
            replaceImage()
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(dataBinding.toolbar)
    }

    override fun onCaptureClick() {
        photoFile = null
        try {
            photoFile = ImageUtils.generateUniqueImageFile(this)
        } catch (ex: IOException) {
            return
        }

        photoFile?.let { photoFile ->
            val photoUri =
                FileProvider.getUriForFile(this, "com.darius.placebook.fileprovider", photoFile)
            val captureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)

            captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
            val intentActivities = packageManager.queryIntentActivities(
                captureIntent, PackageManager.MATCH_DEFAULT_ONLY
            )
            intentActivities.map { it.activityInfo.packageName }
                .forEach {
                    grantUriPermission(
                        it,
                        photoUri,
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    )
                }

            startActivityForResult(captureIntent, REQUEST_CODE_CAPTURE)
        }
    }

    override fun onPickClick() {
        val pickIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(pickIntent, REQUEST_GALLERY_IMAGE)
    }

    private fun getImageWithAuthority(uri: Uri) = ImageUtils.decodeUriStreamToSize(
        uri,
        resources.getDimensionPixelSize(R.dimen.default_image_width),
        resources.getDimensionPixelSize(R.dimen.default_image_height),
        this
    )

    private fun updateImage(image: Bitmap) {
        bookmarkDetailsMarkerView?.let {
            dataBinding.imageViewPlace.setImageBitmap(image)
            it.setImage(this, image)
        }
    }

    private fun getImageWithPath(filePath: String) = ImageUtils.decodeFileToSize(
        filePath,
        resources.getDimensionPixelSize(R.dimen.default_image_width),
        resources.getDimensionPixelSize(R.dimen.default_image_height)
    )

    private fun replaceImage() {
        val newFragment = PhotoOptionDialogFragment.newInstance(this)
        newFragment?.show(supportFragmentManager, "photoOptionDialog")
    }

    companion object {
        private const val REQUEST_CODE_CAPTURE = 1
        private const val REQUEST_GALLERY_IMAGE = 2
    }

}