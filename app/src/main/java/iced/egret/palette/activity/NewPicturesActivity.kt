package iced.egret.palette.activity

import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import iced.egret.palette.R
import iced.egret.palette.flexible.GridCoverableItem
import iced.egret.palette.model.Picture
import iced.egret.palette.util.CollectionManager
import iced.egret.palette.util.DialogGenerator
import iced.egret.palette.util.Painter

class NewPicturesActivity : GridCoverableActivity() {

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
        mToolbar.title = getString(R.string.new_pictures)
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
        mToolbar.navigationIcon?.setTint(itemColor)
        mToolbar.menu.findItem(R.id.actionClearAll).icon.setTint(itemColor)
    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        super.onCreateActionMode(mode, menu)
        Painter.paintDrawable(menu.findItem(R.id.actionSelectAll).icon)
        Painter.paintDrawable(menu.findItem(R.id.actionClear).icon)
        return true
    }

    override fun onActionItemClicked(mode: ActionMode, menuItem: MenuItem): Boolean {

        val selected = mAdapter.selectedPositions.map { i -> mContents[i] }

        when (menuItem.itemId) {
            R.id.actionSelectAll -> selectAll()
            R.id.actionClear -> processClear(selected)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.actionClearAll -> processClear(mContents)
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
        val typeString = if (pictures.size == 1) "picture" else "pictures"

        DialogGenerator.clear(this, typeString) {
            val set = pictures.toSet()
            CollectionManager.removeFromBufferPictures(pictures)
            mContents.removeAll(pictures)
            mContentItems.removeAll { item -> item.coverable in set }

            mAdapter.updateDataSet(mContentItems)
            mActionModeHelper.destroyActionModeIfCan()

        }
    }

}