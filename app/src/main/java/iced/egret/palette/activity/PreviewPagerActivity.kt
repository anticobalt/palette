package iced.egret.palette.activity

import android.os.Bundle
import iced.egret.palette.R
import iced.egret.palette.activity.inherited.PicturePagerActivity
import iced.egret.palette.model.Picture
import iced.egret.palette.util.ThirdPartyIntentHandler


class PreviewPagerActivity : PicturePagerActivity() {

    override val bottomBarRes: Nothing? = null
    override val menuRes = R.menu.menu_preview_pager

    private val previewPictures = mutableListOf<Picture>()

    override fun onCreate(savedInstanceState: Bundle?) {

        if (ThirdPartyIntentHandler.previewPictures.isEmpty()) finish()
        previewPictures.addAll(ThirdPartyIntentHandler.previewPictures)

        // hack to set the active page, so superclass doesn't quit
        intent.putExtra(getString(R.string.intent_item_key), 0)

        super.onCreate(savedInstanceState)
    }

    override fun fetchPictures() {
        mPictures.clear()
        mPictures.addAll(previewPictures)
    }

}