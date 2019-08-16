package iced.egret.palette.activity

import android.content.Intent
import android.net.Uri
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import androidx.core.content.FileProvider
import iced.egret.palette.BuildConfig
import iced.egret.palette.R
import iced.egret.palette.activity.inherited.GridCoverableActivity
import iced.egret.palette.flexible.item.GridCoverableItem
import iced.egret.palette.model.Picture
import iced.egret.palette.util.CollectionManager
import iced.egret.palette.util.CoverableMutator
import iced.egret.palette.util.DialogGenerator
import iced.egret.palette.util.Painter

/**
 * Shows items in the buffer i.e. new items that haven't been processed, and allows basic
 * actions on them.
 */
class WaitingRoomActivity : GridCoverableActivity() {

    override var actionModeMenuRes = R.menu.menu_waiting_room_edit

    private val autoClear: Boolean
        get() = defSharedPreferences.getBoolean(getString(R.string.key_waiting_room_autoclear), false)

    /**
     * Update when returning to activity.
     * onStart() not called when another other activity finishes.
     */
    override fun onResume() {
        super.onResume()
        if (!mActionModeIsRunning) {
            CollectionManager.fetchNewMedia(this) {
                refresh()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK && requestCode == PAGER_REQUEST) {
            val goHome = getString(R.string.intent_go_home)
            if (data?.getBooleanExtra(goHome, false) == true) {
                val intent = Intent()
                intent.putExtra(goHome, true)
                setResult(RESULT_OK, intent)
                finish()
            }
        }
    }

    private fun refresh() {
        fetchContents()
        mAdapter.updateDataSet(mContentItems)
    }

    override fun fetchContents() {
        mContents.clear()
        mContentItems.clear()
        mContents.addAll(CollectionManager.getBufferPictures())
        mContentItems.addAll(mContents.map { content -> GridCoverableItem(content) })
    }

    override fun buildToolbar() {
        mToolbar.inflateMenu(R.menu.menu_waiting_room)
        mToolbar.title = getString(R.string.waiting_room)
        mToolbar.setOnMenuItemClickListener {
            onOptionsItemSelected(it)
        }
        mToolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        super.onCreateActionMode(mode, menu)
        Painter.paintDrawable(menu.findItem(R.id.actionSelectAll).icon)
        Painter.paintDrawable(menu.findItem(R.id.actionClear).icon)
        return true
    }

    override fun onActionItemClicked(mode: ActionMode, menuItem: MenuItem): Boolean {
        val selected = mActionModeHelper.selectedPositions.map { i -> mContents[i] }
        when (menuItem.itemId) {
            R.id.actionSelectAll -> selectAll()
            R.id.actionClear -> processClear(selected)
            R.id.actionShare -> processShare(selected)
            R.id.actionMove -> processMove(selected)
            R.id.actionAddToAlbum -> processAddToAlbum(selected)
            R.id.actionDelete -> processDelete(selected)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.actionClearAll -> processClearAll()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onIdleItemClick(position: Int) {
        val intent = Intent(this, WaitingRoomPagerActivity::class.java)
        intent.putExtra(getString(R.string.intent_item_key), position)
        startActivityForResult(intent, PAGER_REQUEST)
    }

    private fun selectAll() {
        if (mActionModeHelper.selectedPositions.size == mAdapter.currentItems.size) return

        var i = 0
        for (item in mAdapter.currentItems) {
            item.setSelection(true)
            mActionModeHelper.selectedPositions.add(i)
            i += 1
        }
        mActionModeHelper.updateContextTitle(mActionModeHelper.selectedPositions.size)
    }

    private fun processClear(pictures: List<Picture>) {
        if (pictures.isEmpty()) return
        DialogGenerator.clear(this) {
            clear(pictures)
            mAdapter.updateDataSet(mContentItems)
            mActionModeHelper.destroyActionModeIfCan()
        }
    }

    private fun processClearAll() {
        if (mContents.isEmpty()) return
        DialogGenerator.clearAll(this) {
            clear(mContents)
            mAdapter.updateDataSet(mContentItems)
            mActionModeHelper.destroyActionModeIfCan()
        }
    }

    private fun processShare(pictures: List<Picture>) {
        // Need to create content:// URI to share, instead of natively-used file:// one
        // https://stackoverflow.com/a/38858040
        // https://developer.android.com/training/sharing/send
        val imageUris: ArrayList<Uri> = pictures.map { picture ->
            FileProvider.getUriForFile(
                    this, BuildConfig.APPLICATION_ID + ".file_provider", picture.file)
        } as ArrayList<Uri>

        val intent = Intent().apply {
            action = Intent.ACTION_SEND_MULTIPLE
            putExtra(Intent.EXTRA_STREAM, imageUris)
            type = "image/*"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, getText(R.string.share_intent_title)))
    }

    private fun processMove(pictures: List<Picture>) {
        CoverableMutator.move(pictures, this) {
            if (autoClear) {
                clear(pictures)
                mAdapter.updateDataSet(mContentItems)
            }
            mActionModeHelper.destroyActionModeIfCan()
        }
    }

    private fun processAddToAlbum(pictures: List<Picture>) {
        CoverableMutator.addToAlbum(pictures, this) {
            if (autoClear) {
                clear(pictures)
                mAdapter.updateDataSet(mContentItems)
            }
            mAdapter.updateDataSet(mContentItems)
            mActionModeHelper.destroyActionModeIfCan()
        }
    }

    private fun processDelete(pictures: List<Picture>) {
        CoverableMutator.delete(pictures, this) {
            clear(pictures)
            mAdapter.updateDataSet(mContentItems)
            mActionModeHelper.destroyActionModeIfCan()
        }
    }

    private fun clear(pictures: List<Picture>) {
        val set = pictures.toSet()
        CollectionManager.removeFromBufferPictures(pictures)
        mContents.removeAll(pictures)
        mContentItems.removeAll { item -> item.coverable in set }
    }

    companion object Constants {
        private const val PAGER_REQUEST = 1
    }

}