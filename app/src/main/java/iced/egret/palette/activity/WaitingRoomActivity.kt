package iced.egret.palette.activity

import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import com.jaredrummler.cyanea.tinting.MenuTint
import iced.egret.palette.R
import iced.egret.palette.flexible.GridCoverableItem
import iced.egret.palette.model.Picture
import iced.egret.palette.util.CollectionManager
import iced.egret.palette.util.CoverableMutator
import iced.egret.palette.util.DialogGenerator
import iced.egret.palette.util.Painter

class WaitingRoomActivity : GridCoverableActivity() {

    override var actionModeMenuRes = R.menu.menu_new_pictures_edit

    override fun onStart() {
        super.onStart()
        CollectionManager.fetchNewMedia(this) {
            refresh()
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
        mToolbar.inflateMenu(R.menu.menu_new_pictures)
        mToolbar.title = getString(R.string.waiting_room)
        mToolbar.setOnMenuItemClickListener {
            onOptionsItemSelected(it)
        }
        mToolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    override fun styleToolbar() {
        val itemColor = getColorInt(ColorType.ITEM)
        mToolbar.setTitleTextColor(itemColor)
        MenuTint(mToolbar.menu, itemColor, tintOverflowIcon = true, tintNavigationIcon = true).apply(this)
    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        super.onCreateActionMode(mode, menu)
        MenuTint(menu, Painter.currentDrawableColor, tintOverflowIcon = true, tintNavigationIcon = true).apply(this)
        return true
    }

    override fun onActionItemClicked(mode: ActionMode, menuItem: MenuItem): Boolean {
        val selected = mAdapter.selectedPositions.map { i -> mContents[i] }
        when (menuItem.itemId) {
            R.id.actionSelectAll -> selectAll()
            R.id.actionClear -> processClear(selected)
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

    private fun selectAll() {
        mAdapter.currentItems.map { item -> item.setSelection(true) }
        mAdapter.selectAll()
        mActionModeHelper.updateContextTitle(mAdapter.selectedItemCount)
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

    private fun processMove(pictures: List<Picture>) {
        CoverableMutator.move(pictures, this) {
            clear(pictures)
            mAdapter.updateDataSet(mContentItems)
            mActionModeHelper.destroyActionModeIfCan()
        }
    }

    private fun processAddToAlbum(pictures: List<Picture>) {
        CoverableMutator.addToAlbum(pictures, this) {
            clear(pictures)
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

}