package iced.egret.palette.activity

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageOptions
import com.theartofdev.edmodo.cropper.CropImageView
import iced.egret.palette.R
import iced.egret.palette.util.CollectionManager
import iced.egret.palette.util.DialogGenerator
import iced.egret.palette.util.Storage
import java.io.File
import java.io.IOException

class CropActivity : BottomActionsActivity() {

    private lateinit var mImageView : CropImageView
    private lateinit var mImageUri : Uri
    private lateinit var mOptions : CropImageOptions

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crop)

        val bundle = intent.getBundleExtra(CropImage.CROP_IMAGE_EXTRA_BUNDLE)
        mImageUri = bundle.getParcelable(CropImage.CROP_IMAGE_EXTRA_SOURCE)
        mOptions = bundle.getParcelable(CropImage.CROP_IMAGE_EXTRA_OPTIONS)

        buildCropView()
        buildActionBar()
        buildBottomActions()
    }

    private fun buildCropView() {
        mImageView = findViewById(R.id.cropImageView)
        mImageView.setImageUriAsync(mImageUri)
        mImageView.setOnCropImageCompleteListener { _, result ->
            saveCroppedBitmap(mImageView.croppedImage)
        }
    }

    private fun buildActionBar() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    override fun buildBottomActions() {
        super.buildBottomActions()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // use CropImageActivity's default menu
        menuInflater.inflate(R.menu.crop_image_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        val retValue = when (item?.itemId) {
            R.id.crop_image_menu_crop -> {
                val result = cropImage()
                if (!result) showFailToast()
                true
            }
            R.id.crop_image_menu_rotate_left -> {
                mImageView.rotateImage(-mOptions.rotationDegrees)
                true
            }
            R.id.crop_image_menu_rotate_right -> {
                mImageView.rotateImage(mOptions.rotationDegrees)
                true
            }
            R.id.crop_image_menu_flip_horizontally -> {
                mImageView.flipImageHorizontally()
                true
            }
            R.id.crop_image_menu_flip_vertically -> {
                mImageView.flipImageVertically()
                true
            }
            else -> false
        }
        return if (retValue) true  // consume action
        else super.onOptionsItemSelected(item)
    }

    private fun cropImage() : Boolean {
        val newUri : Uri = getOutputUri() ?: return false
        mImageView.saveCroppedImageAsync(newUri)
        return true
    }

    private fun saveCroppedBitmap(bitmap: Bitmap) {

        fun save(name: String, location: String, isNewFile: Boolean) {
            val file = CollectionManager.createPictureFromBitmap(bitmap, name, location, isNewFile)
            broadcastNewMedia(file)
            setResult(Activity.RESULT_OK)
            finish()
        }

        val oldName = mImageUri.lastPathSegment
        val location = mImageUri.path?.removeSuffix(oldName as CharSequence)

        if (location == null) {
            showFailToast()
            return
        }

        // Nested dialogs is a QoL feature; makes it easy to change filenames if you
        // don't want to overwrite existing file b/c first dialog won't close after OK pressed
        DialogGenerator.nameFile(this, oldName) {
            val name = it.toString()
            if (Storage.fileExists(name, location)) {
                DialogGenerator.confirmReplaceFile(this) {
                    save(name, location, isNewFile = false)
                }
            }
            else {
                save(name, location, isNewFile = true)
            }
        }

    }

    /**
     * Copied from CropImageActivity.
     * https://github.com/ArthurHub/Android-Image-Cropper
     */
    private fun getOutputUri() : Uri? {
        var outputUri: Uri? = mOptions.outputUri
        if (outputUri == null || outputUri == Uri.EMPTY) {
            try {
                val ext = when {
                    mOptions.outputCompressFormat == Bitmap.CompressFormat.JPEG -> ".jpg"
                    mOptions.outputCompressFormat == Bitmap.CompressFormat.PNG -> ".png"
                    else -> ".webp"
                }
                outputUri = Uri.fromFile(File.createTempFile("cropped", ext, cacheDir))
            } catch (e: IOException) {
                throw RuntimeException("Failed to create temp file for output image", e)
            }

        }
        return outputUri
    }

    /**
     * Broadcast changes so that show up immediately whenever MediaStore is accessed.
     * https://stackoverflow.com/a/39241495
     */
    private fun broadcastNewMedia(file: File) {
        val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
        val uri = Uri.fromFile(file)
        mediaScanIntent.data = uri
        sendBroadcast(mediaScanIntent)
    }

    private fun showFailToast() {
        Toast.makeText(this, R.string.edit_fail_error, Toast.LENGTH_SHORT).show()
    }

}
