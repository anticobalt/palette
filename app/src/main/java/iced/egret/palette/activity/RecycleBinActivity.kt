package iced.egret.palette.activity

import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import iced.egret.palette.R
import iced.egret.palette.activity.inherited.GridCoverableActivity
import iced.egret.palette.flexible.item.GridCoverableItem
import iced.egret.palette.model.Picture
import iced.egret.palette.util.CollectionManager
import iced.egret.palette.util.DialogGenerator
import iced.egret.palette.util.Painter
import iced.egret.palette.util.Storage

class RecycleBinActivity : GridCoverableActivity() {

    override var actionModeMenuRes = R.menu.menu_recycle_bin_edit

    override fun buildToolbar() {
        mToolbar.inflateMenu(R.menu.menu_recycle_bin)
        mToolbar.title = getString(R.string.recycle_bin)
        mToolbar.setOnMenuItemClickListener {
            onOptionsItemSelected(it)
        }
        mToolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_recycle_bin, menu)
        return super.onCreateOptionsMenu(menu)
    }

    /**
     * @return True to consume, false if to continue.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
            R.id.actionEmpty -> {
                DialogGenerator.delete(this, "pictures") {
                    deletePictures(mContents, "pictures")
                    refresh()
                }
            }
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        super.onCreateActionMode(mode, menu)
        Painter.paintDrawable(menu.findItem(R.id.actionRestore).icon)
        Painter.paintDrawable(menu.findItem(R.id.actionDelete).icon)
        return true
    }

    // Return true to continue with Action Mode
    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {

        val selected = mActionModeHelper.selectedPositions.map { i -> mContents[i] }

        val typePlural = CollectionManager.PICTURE_KEY.toLowerCase()
        val typeSingular = typePlural.dropLast(1)  // only works on s-appending plurals
        val typeString = if (selected.size > 1) typePlural else typeSingular

        when (item.itemId) {
            R.id.actionRestore -> {
                DialogGenerator.restore(this, typeString) {
                    restorePictures(selected, typeString)
                    refresh()
                }
            }
            R.id.actionDelete -> {
                DialogGenerator.delete(this, typeString) {
                    deletePictures(selected, typeString)
                    refresh()
                }
            }
        }
        return true
    }

    override fun onIdleItemClick(position: Int) {
        val picture = mContents[position]
        val nameInBin = picture.filePath.split("/").last()
        DialogGenerator.pictureDetails(this, picture, Storage.recycleBin.oldLocations[nameInBin])
    }

    override fun fetchContents() {
        mContents.clear()
        mContentItems.clear()
        mContents.addAll(Storage.recycleBin.contentsByDateDesc)
        mContentItems.addAll(mContents.map { content -> GridCoverableItem(content) })
    }

    /**
     * Get RecycleBin contents from storage, compare them with current contents,
     * and discard those not found in storage.
     */
    private fun discardOutdatedContents() {
        val freshUris = Storage.recycleBin.contents.map { c -> c.uri }.toSet()
        val toDiscardItems = mutableListOf<GridCoverableItem>()
        val toDiscardContents = mutableListOf<Picture>()

        for (i in 0 until mContents.size) {
            if (mContents[i].uri !in freshUris) {
                toDiscardItems.add(mContentItems[i])
                toDiscardContents.add(mContents[i])
            }
        }
        mContentItems.removeAll(toDiscardItems)
        mContents.removeAll(toDiscardContents)
    }

    private fun refresh() {
        discardOutdatedContents()
        mAdapter.updateDataSet(mContentItems)
        mActionModeHelper.destroyActionModeIfCan()
    }

    private fun restorePictures(pictures: List<Picture>, typeString: String) {
        val failCounter = CollectionManager.restorePicturesFromRecycleBin(
                pictures, getSdCardDocumentFile(), contentResolver) {
            broadcastMediaChanged(it)
        }
        if (failCounter > 0) toast("Failed to restore $failCounter!")
        else toast("${pictures.size} $typeString restored")
    }

    private fun deletePictures(pictures: List<Picture>, typeString: String) {
        val failCounter = CollectionManager.deletePictures(pictures)
        if (failCounter > 0) toast("Failed to delete $failCounter!")
        else toast("${pictures.size} $typeString permanently deleted")
    }
}
