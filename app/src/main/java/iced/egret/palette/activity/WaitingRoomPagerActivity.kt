package iced.egret.palette.activity

import android.view.Menu
import android.view.MenuItem
import iced.egret.palette.R
import iced.egret.palette.util.CollectionManager
import iced.egret.palette.util.CoverableMutator
import iced.egret.palette.util.DialogGenerator
import kotlinx.android.synthetic.main.bottom_bar_waiting_room_pager.view.*

class WaitingRoomPagerActivity : PicturePagerActivity() {

    override val bottomBarRes = R.layout.bottom_bar_waiting_room_pager
    override val menuRes = R.menu.menu_waiting_room_pager

    private val autoClear : Boolean
        get() = defSharedPreferences.getBoolean(getString(R.string.waiting_room_autoclear_key), false)

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val trueZoomOn = mSharedPrefs.getBoolean(getString(R.string.true_zoom_key), false)
        val trueZoomItem = menu.findItem(R.id.switchTrueZoom)
        trueZoomItem.isChecked = trueZoomOn
        return super.onPrepareOptionsMenu(menu)
    }

    override fun setBottomBarListeners() {
        mBottomBar.details.setOnClickListener {
            DialogGenerator.pictureDetails(this, mCurrentPicture)
        }
        mBottomBar.home_folder.setOnClickListener {}
        mBottomBar.share.setOnClickListener {}
        mBottomBar.delete.setOnClickListener {
            initiateMoveToRecycleBin()
        }
    }

    override fun fetchPictures() {
        mPictures.clear()
        mPictures.addAll(CollectionManager.getBufferPictures())
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        val retVal =  when (item?.itemId) {
            R.id.switchTrueZoom -> {
                item.isChecked = !item.isChecked
                mSharedPrefs.edit().putBoolean(getString(R.string.true_zoom_key), item.isChecked).apply()
                refreshViewPager()
                true
            }
            R.id.actionMove -> {
                initiateMove()
                true
            }
            R.id.actionRename -> {
                initiateRename()
                true
            }
            else -> false
        }
        return if (retVal) true  // consume action
        else super.onOptionsItemSelected(item)
    }

    private fun initiateMove() {
        val picture = mCurrentPicture
        CoverableMutator.move(listOf(picture), this) {
            clearAndFinish()
        }
    }

    private fun initiateRename() {
        val picture = mCurrentPicture
        CoverableMutator.rename(picture, this) {
            setToolbarTitle()  // to update name
        }
    }

    private fun initiateMoveToRecycleBin() {
        val picture = mCurrentPicture
        CoverableMutator.delete(listOf(picture), this) {
            clearAndFinish()
        }
    }

    private fun clearAndFinish() {
        if (autoClear) CollectionManager.removeFromBufferPictures(listOf(mCurrentPicture))
        setResult(RESULT_OK)
        finish()
    }
}