package iced.egret.palette.fragment

import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import iced.egret.palette.R
import iced.egret.palette.model.Album
import iced.egret.palette.util.CollectionManager
import iced.egret.palette.util.DialogGenerator
import iced.egret.palette.util.Painter

class AlbumViewFragment : CollectionViewFragment() {

    override fun buildToolbar() {
        super.buildToolbar()
    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        super.onCreateActionMode(mode, menu)

        val removeFromAlbum = menu.findItem(R.id.actionRemoveFromAlbum)
        when (mSelectedContentType) {
            CollectionManager.FOLDER_KEY -> {
                removeFromAlbum.isVisible = true; Painter.paintDrawable(removeFromAlbum.icon)
            }
            CollectionManager.PICTURE_KEY -> {
                removeFromAlbum.isVisible = true; Painter.paintDrawable(removeFromAlbum.icon)
            }
        }

        return true
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        super.onActionItemClicked(mode, item)

        val coverables = adapter.selectedPositions.map { index ->
            CollectionManager.getContentsMap()[mSelectedContentType]!![index]
        }
        val typePlural = mSelectedContentType!!.toLowerCase()
        val typeSingular = typePlural.dropLast(1)  // only works on s-appending plurals
        val typeString = if (coverables.size > 1) typePlural else typeSingular

        when (item.itemId) {
            R.id.actionRemoveFromAlbum -> {
                DialogGenerator.removeFromAlbum(context!!, typeString) {
                    CollectionManager.removeContentFromCurrentAlbum(coverables)
                    toast("Removed ${coverables.size} $typeString.")
                    refreshFragment()
                    mMaster.notifyCollectionsChanged()
                }
            }
        }

        return true
    }

    override fun onDestroyActionMode(mode: ActionMode) {
        super.onDestroyActionMode(mode)
        mode.menu.findItem(R.id.actionRemoveFromAlbum).isVisible = false
    }

    override fun onFabClick() {
        fun albumExists(name: CharSequence): Boolean {
            val found = mContents.find { coverable -> coverable is Album
                    && coverable.name == name.toString() }
            return found != null
        }
        fun createNewAlbum(name: CharSequence) {
            CollectionManager.createNewAlbum(name.toString(), addToCurrent = true)
            refreshFragment()
        }

        DialogGenerator.createAlbum(context!!, ::albumExists, ::createNewAlbum)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return super.onOptionsItemSelected(item)
    }

}