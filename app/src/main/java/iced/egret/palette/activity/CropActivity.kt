package iced.egret.palette.activity

import android.app.Activity
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Pair
import android.view.MenuItem
import androidx.preference.PreferenceManager
import com.afollestad.aesthetic.Aesthetic
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageOptions
import iced.egret.palette.R
import iced.egret.palette.util.CollectionManager
import iced.egret.palette.util.DialogGenerator
import iced.egret.palette.util.Storage
import kotlinx.android.synthetic.main.activity_crop.*
import kotlinx.android.synthetic.main.activity_view_picture.bottomActions
import kotlinx.android.synthetic.main.appbar_list_fragment.*
import kotlinx.android.synthetic.main.bottom_actions_crop.view.*
import java.io.File
import java.io.IOException

class CropActivity : BottomActionsActivity() {

    // Assume theme color can't be white
    private val invalidColor = -1
    private lateinit var sharedPreferences : SharedPreferences

    private lateinit var mImageUri: Uri
    private lateinit var mOptions: CropImageOptions

    override fun onCreate(savedInstanceState: Bundle?) {
        Aesthetic.attach(this)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crop)

        val bundle = intent.getBundleExtra(CropImage.CROP_IMAGE_EXTRA_BUNDLE)
        mImageUri = bundle.getParcelable(CropImage.CROP_IMAGE_EXTRA_SOURCE)
        mOptions = bundle.getParcelable(CropImage.CROP_IMAGE_EXTRA_OPTIONS)

        buildCropView()
        buildActionBar()
        buildBottomActions()

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        //applyTheme()
    }

    override fun onResume() {
        super.onResume()
        Aesthetic.resume(this)
    }

    override fun onPause() {
        super.onPause()
        Aesthetic.pause(this)
    }

    private fun buildCropView() {
        cropImageView.setImageUriAsync(mImageUri)
        cropImageView.setOnCropImageCompleteListener { _, _ ->
            saveCroppedBitmap(cropImageView.croppedImage)
        }
    }

    private fun buildActionBar() {
        //supportActionBar?.setDisplayHomeAsUpEnabled(true)
        toolbar.title = getString(R.string.title_activity_crop)
        toolbar.inflateMenu(R.menu.menu_crop)
        toolbar.setOnMenuItemClickListener {
            onOptionsItemSelected(it)
        }
    }

    override fun buildBottomActions() {
        super.buildBottomActions()
        bottomActions.ratio_free.setOnClickListener {
            // FIXME: side-effect: crop window fills whole screen if set from true to false
            cropImageView.setFixedAspectRatio(false)
        }
        bottomActions.ratio_1x1.setOnClickListener {
            cropImageView.setAspectRatio(1, 1)
        }
        bottomActions.ratio_4x3.setOnClickListener {
            if (cropImageView.aspectRatio == Pair(4, 3)) {
                cropImageView.setAspectRatio(3, 4)
            } else {
                cropImageView.setAspectRatio(4, 3)
            }
        }
        bottomActions.ratio_16x9.setOnClickListener {
            if (cropImageView.aspectRatio == Pair(16, 9)) {
                cropImageView.setAspectRatio(9, 16)
            } else {
                cropImageView.setAspectRatio(16, 9)
            }
        }
        bottomActions.ratio_reset.setOnClickListener {
            cropImageView.clearAspectRatio()
            cropImageView.resetCropRect()  // expand to fill whole image
        }
    }

    /*
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        // use CropImageActivity's default menu
        menuInflater.inflate(R.menu.menu_crop, menu)
        return super.onCreateOptionsMenu(menu)
    }*/

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        val retValue = when (item?.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            R.id.crop_image_menu_crop -> {
                val result = cropImage()
                if (!result) showFailToast()
                true
            }
            R.id.crop_image_menu_rotate_left -> {
                cropImageView.rotateImage(-mOptions.rotationDegrees)
                true
            }
            R.id.crop_image_menu_rotate_right -> {
                cropImageView.rotateImage(mOptions.rotationDegrees)
                true
            }
            R.id.crop_image_menu_flip_horizontally -> {
                cropImageView.flipImageHorizontally()
                true
            }
            R.id.crop_image_menu_flip_vertically -> {
                cropImageView.flipImageVertically()
                true
            }
            else -> false
        }
        return if (retValue) true  // consume action
        else super.onOptionsItemSelected(item)
    }

    private fun cropImage(): Boolean {
        val newUri: Uri = getOutputUri() ?: return false
        cropImageView.saveCroppedImageAsync(newUri)
        return true
    }

    private fun saveCroppedBitmap(bitmap: Bitmap) {

        fun save(name: String, location: String, isNewFile: Boolean) {
            val sdCardFile = getSdCardDocumentFile()
            val file = CollectionManager
                    .createPictureFromBitmap(bitmap, name, location, isNewFile,
                            sdCardFile, contentResolver)
            if (file == null) {
                showAccessDeniedToast()
                return
            }
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
            } else {
                save(name, location, isNewFile = true)
            }
        }

    }

    /**
     * Copied from CropImageActivity.
     * https://github.com/ArthurHub/Android-Image-Cropper
     */
    private fun getOutputUri(): Uri? {
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

    private fun showFailToast() {
        toast(R.string.edit_fail_error)
    }

    private fun showAccessDeniedToast() {
        toast(R.string.access_denied_error)
    }

}
