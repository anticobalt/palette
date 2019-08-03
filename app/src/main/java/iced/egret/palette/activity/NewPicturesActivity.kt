package iced.egret.palette.activity

import android.view.ActionMode
import android.view.MenuItem
import iced.egret.palette.R
import iced.egret.palette.flexible.GridCoverableItem
import iced.egret.palette.util.CollectionManager

class NewPicturesActivity : GridCoverableActivity() {

    override var actionModeMenuRes = R.menu.menu_new_pictures_edit

    override fun onStart() {
        super.onStart()
        CollectionManager.fetchNewMedia(this) {
            refresh()
        }
    }

    override fun fetchContents() {
        mContents.clear()
        mContentItems.clear()
        mContents.addAll(CollectionManager.bufferPictures)
        mContentItems.addAll(mContents.map { content -> GridCoverableItem(content) })
    }

    private fun refresh() {
        fetchContents()
        mAdapter.updateDataSet(mContentItems)
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

    override fun onActionItemClicked(mode: ActionMode, menuItem: MenuItem): Boolean {
        when (menuItem.itemId) {
            R.id.actionSelectAll -> {
            }
            R.id.actionClear -> {
            }
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.actionClearAll -> {
            }
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

}