package iced.egret.palette.activity

import android.content.Intent
import android.view.MenuItem
import iced.egret.palette.R
import iced.egret.palette.activity.inherited.StatefulPagerActivity
import iced.egret.palette.model.Folder
import iced.egret.palette.util.CollectionManager
import iced.egret.palette.util.CoverableMutator
import iced.egret.palette.util.DialogGenerator
import kotlinx.android.synthetic.main.bottom_bar_waiting_room_pager.view.*

class WaitingRoomPagerActivity : StatefulPagerActivity() {

    override val bottomBarRes = R.layout.bottom_bar_waiting_room_pager
    override val menuRes = R.menu.menu_waiting_room_pager

    private val autoClear: Boolean
        get() = defSharedPreferences.getBoolean(getString(R.string.key_waiting_room_autoclear), false)

    override fun setBottomBarListeners() {
        mBottomBar.details.setOnClickListener {
            DialogGenerator.pictureDetails(this, mCurrentPicture)
        }
        mBottomBar.home_folder.setOnClickListener {
            goToHomeFolder()
        }
        mBottomBar.share.setOnClickListener {
            startShareActivity()
        }
        mBottomBar.delete.setOnClickListener {
            initiateMoveToRecycleBin()
        }
    }

    override fun fetchPictures() {
        mPictures.clear()
        mPictures.addAll(CollectionManager.getBufferPictures())
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        val retVal = when (item?.itemId) {
            R.id.switchTrueZoom -> {
                item.isChecked = !item.isChecked
                mSharedPrefs.edit().putBoolean(getString(R.string.key_true_zoom), item.isChecked).apply()
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

    private fun goToHomeFolder() {
        val home = mCurrentPicture.parent
        if (home !is Folder) {
            toastLong(R.string.error_generic)
        } else {
            CollectionManager.launchAsShortcut(home)
            val intent = Intent()
            intent.putExtra(getString(R.string.intent_go_home), true)
            setResult(RESULT_OK, intent)
            finish()
        }
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
            // Deletion always clears internally
            setResult(RESULT_OK)
            finish()
        }
    }

    private fun clearAndFinish() {
        if (autoClear) CollectionManager.removeFromBufferPictures(listOf(mCurrentPicture))
        setResult(RESULT_OK)
        finish()
    }
}