package iced.egret.palette.activity

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.provider.OpenableColumns
import iced.egret.palette.R
import iced.egret.palette.activity.inherited.PicturePagerActivity
import iced.egret.palette.model.Picture
import iced.egret.palette.model.PreviewPicture


class PreviewPagerActivity : PicturePagerActivity() {

    override val bottomBarRes: Nothing? = null
    override val menuRes = R.menu.menu_preview_pager

    private val previewPictures = mutableListOf<Picture>()

    override fun onCreate(savedInstanceState: Bundle?) {

        when (intent.action) {
            Intent.ACTION_SEND -> {
                val extra = intent.getParcelableExtra<Parcelable>(Intent.EXTRA_STREAM)
                if (extra is Uri) previewPictures.add(PreviewPicture(getName(extra), extra, contentResolver))
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                val extras = intent.getParcelableArrayListExtra<Parcelable>(Intent.EXTRA_STREAM)
                for (extra in extras) {
                    if (extra is Uri) previewPictures.add(PreviewPicture(getName(extra), extra, contentResolver))
                }
            }
        }
        // hack to set the active page, so superclass doesn't quit
        intent.putExtra(getString(R.string.intent_item_key), 0)

        super.onCreate(savedInstanceState)
    }

    /**
     * https://stackoverflow.com/a/38304115
     */
    private fun getName(uri: Uri) : String {
        val returnCursor = contentResolver.query(uri, null, null, null, null)!!
        val nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        returnCursor.moveToFirst()
        val name = returnCursor.getString(nameIndex)
        returnCursor.close()
        return name
    }

    override fun fetchPictures() {
        mPictures.clear()
        mPictures.addAll(previewPictures)
    }

}