package iced.egret.palette.activity

import android.app.Activity
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.util.Pair
import android.view.MenuItem
import android.widget.TextView
import androidx.preference.PreferenceManager
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageOptions
import iced.egret.palette.R
import iced.egret.palette.activity.inherited.PicturePagerActivity
import iced.egret.palette.activity.inherited.SlideActivity
import iced.egret.palette.util.CollectionManager
import iced.egret.palette.util.DialogGenerator
import iced.egret.palette.util.StateBuilder
import iced.egret.palette.util.Storage
import kotlinx.android.synthetic.main.activity_crop.*
import kotlinx.android.synthetic.main.appbar.*
import kotlinx.android.synthetic.main.bottom_bar_crop.view.*
import java.io.File
import java.io.IOException

class CropActivity : SlideActivity() {

    private lateinit var sharedPreferences: SharedPreferences

    private lateinit var mImageUri: Uri
    private lateinit var mOptions: CropImageOptions

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_crop)
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        setState(savedInstanceState)

        val bundle = intent.getBundleExtra(CropImage.CROP_IMAGE_EXTRA_BUNDLE)
        mImageUri = bundle.getParcelable(CropImage.CROP_IMAGE_EXTRA_SOURCE)
        mOptions = bundle.getParcelable(CropImage.CROP_IMAGE_EXTRA_OPTIONS)

        buildCropView()
        buildToolbar()
        buildBottomBar()

        colorStandardElements(toolbar)
        colorBottomBar()
    }

    override fun onResume() {
        super.onResume()

        // If handling on-disk item, but it was moved, get out.
        val path = mImageUri.path
        if (path != null && !Storage.fileExists(path)) finish()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(PicturePagerActivity.COLLECTION, CollectionManager.currentCollection?.path)
        super.onSaveInstanceState(outState)
    }

    private fun setState(savedInstanceState: Bundle?) {
        // Build state if activity restarted. Don't if entering for the first time
        // (b/c it is already built by another activity).
        // Supply saved Collection path to unwind stack properly.
        if (savedInstanceState != null) {
            val path = savedInstanceState.getString(COLLECTION)
            StateBuilder.build(this, path)
        }
    }

    private fun buildCropView() {
        cropImageView.setImageUriAsync(mImageUri)
        cropImageView.setOnCropImageCompleteListener { _, _ ->
            saveCroppedBitmap(cropImageView.croppedImage)
        }
    }

    private fun buildToolbar() {
        toolbar.title = getString(R.string.title_activity_crop)
        toolbar.inflateMenu(R.menu.menu_crop)
        toolbar.setOnMenuItemClickListener {
            onOptionsItemSelected(it)
        }
        toolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    private fun buildBottomBar() {

        bottomBarWrapper.ratio_free.setOnClickListener {
            // FIXME: side-effect: crop window fills whole screen if set from true to false
            cropImageView.setFixedAspectRatio(false)
        }
        bottomBarWrapper.ratio_1x1.setOnClickListener {
            cropImageView.setAspectRatio(1, 1)
        }
        bottomBarWrapper.ratio_4x3.setOnClickListener {
            if (cropImageView.aspectRatio == Pair(4, 3)) {
                cropImageView.setAspectRatio(3, 4)
            } else {
                cropImageView.setAspectRatio(4, 3)
            }
        }
        bottomBarWrapper.ratio_16x9.setOnClickListener {
            if (cropImageView.aspectRatio == Pair(16, 9)) {
                cropImageView.setAspectRatio(9, 16)
            } else {
                cropImageView.setAspectRatio(16, 9)
            }
        }
        bottomBarWrapper.ratio_reset.setOnClickListener {
            cropImageView.clearAspectRatio()
            cropImageView.resetCropRect()  // expand to fill whole image
        }
    }

    private fun colorBottomBar() {
        val barBgColor = getColorInt(ColorType.PRIMARY)
        val barTextColor = getColorInt(ColorType.ITEM)

        bottomBarWrapper.background = ColorDrawable(barBgColor)
        for (touchable in bottomBarWrapper.touchables) {
            if (touchable is TextView) {
                touchable.setTextColor(barTextColor)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        val retValue = when (item?.itemId) {
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
                    .createPictureFromBitmap(bitmap, name, location, isNewFile, sdCardFile, contentResolver)
            if (file == null) {
                showAccessDeniedToast()
                return
            }
            broadcastMediaChanged(file)
            setResult(Activity.RESULT_OK)
            finish()
        }

        // names with periods are fair game
        fun generateNewName(original: String?): String {
            val parts = original?.split(".")
            if (parts == null || parts.size <= 1) return "image.jpg"  // only if original is corrupt

            val extension = parts.last()
            var nameWithoutExtension = parts.dropLast(1).joinToString(".")
            var id = 1

            // Get suffix in form "-43" from "name-43" if it exists, remove it,
            // and update id (in this case, to 44)
            val regex = Regex("-[0-9]+\$")  // anything + dash + any number
            val existingEditMarker = regex.find(nameWithoutExtension)?.value
            if (existingEditMarker != null) {
                nameWithoutExtension = nameWithoutExtension.removeSuffix(existingEditMarker)
                id = existingEditMarker.removePrefix("-").toInt() + 1
            }

            return "$nameWithoutExtension-$id.$extension"
        }

        val oldName = mImageUri.lastPathSegment
        val defaultNewName = generateNewName(oldName)
        val location = mImageUri.path?.removeSuffix(oldName as CharSequence)

        if (location == null) {
            showFailToast()
            return
        }

        // Nested dialogs is a QoL feature; makes it easy to change filenames if you
        // don't want to overwrite existing file b/c first dialog won't close after OK pressed
        DialogGenerator.nameFile(this, defaultNewName) { charSequence, _ ->
            val name = charSequence.toString()
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

    companion object SaveDataKeys {
        const val COLLECTION = "current-collection"
    }

}
