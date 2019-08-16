package iced.egret.palette.activity

import android.content.Intent
import android.view.MenuItem
import com.theartofdev.edmodo.cropper.CropImage
import iced.egret.palette.R
import iced.egret.palette.activity.inherited.StatefulPagerActivity
import iced.egret.palette.model.Folder
import iced.egret.palette.util.CollectionManager
import iced.egret.palette.util.CoverableMutator
import iced.egret.palette.util.DialogGenerator
import kotlinx.android.synthetic.main.bottom_bar_main_pager.view.*


class MainPagerActivity : StatefulPagerActivity() {

    override val bottomBarRes = R.layout.bottom_bar_main_pager
    override val menuRes = R.menu.menu_main_pager

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
        mBottomBar.crop.setOnClickListener {
            startCropActivity()
        }
        mBottomBar.delete.setOnClickListener {
            initiateMoveToRecycleBin()
        }
    }

    override fun fetchPictures() {
        mPictures.clear()
        mPictures.addAll(CollectionManager.getCurrentCollectionPictures())
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        val retVal = when (item?.itemId) {
            R.id.actionMove -> {
                initiateMove()
                true
            }
            R.id.actionRename -> {
                initiateRename()
                true
            }
            R.id.actionSetAsCover -> {
                initiateCoverSet()
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

    private fun startCropActivity() {
        val imageUri = mCurrentPicture.uri
        // setting initial crop padding doesn't working in XML for whatever reason
        CropImage.activity(imageUri)
                .setInitialCropWindowPaddingRatio(0f)
                .start(this, CropActivity::class.java)
    }

    private fun initiateMove() {
        val picture = mCurrentPicture
        CoverableMutator.move(listOf(picture), this) {
            setResult(RESULT_OK)
            finish()
        }
    }

    private fun initiateRename() {
        val picture = mCurrentPicture
        CoverableMutator.rename(picture, this) {
            setToolbarTitle()  // to update name
        }
    }

    private fun initiateCoverSet() {
        val picture = mCurrentPicture
        CoverableMutator.setAsCover(picture, this) {
            toastLong(R.string.success_set_cover)
            setResult(RESULT_OK)
        }
    }

    private fun initiateMoveToRecycleBin() {
        val picture = mCurrentPicture
        CoverableMutator.delete(listOf(picture), this) {
            setResult(RESULT_OK)
            finish()
        }
    }

}