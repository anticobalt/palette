package iced.egret.palette.delegate

import android.content.Context
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.widget.Toolbar
import eu.davidea.flexibleadapter.FlexibleAdapter
import iced.egret.palette.R
import iced.egret.palette.model.Album
import iced.egret.palette.model.Collection
import iced.egret.palette.model.Coverable
import iced.egret.palette.util.CollectionManager
import iced.egret.palette.util.CoverableMutator
import iced.egret.palette.util.Painter

class AlbumViewDelegate : CollectionViewDelegate() {

    override fun onBuildToolbar(toolbar: Toolbar) {
        toolbar.menu.findItem(R.id.actionRename).isVisible = true
    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu, selectedContentType: String): Boolean {
        val removeFromAlbum = menu.findItem(R.id.actionRemoveFromAlbum)
        when (selectedContentType) {
            CollectionManager.FOLDER_KEY -> {
                removeFromAlbum.isVisible = true; Painter.paintDrawable(removeFromAlbum.icon)
            }
            CollectionManager.PICTURE_KEY -> {
                removeFromAlbum.isVisible = true; Painter.paintDrawable(removeFromAlbum.icon)
            }
        }
        return true
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem, adapter: FlexibleAdapter<*>,
                                     context: Context, selectedContentType: String): Boolean {
        val coverables = adapter.selectedPositions.map { index ->
            CollectionManager.getContentsMap()[selectedContentType]!![index]
        }
        when (item.itemId) {
            R.id.actionRemoveFromAlbum -> removeContents(coverables, context)
            R.id.actionDelete -> deleteContents(coverables, selectedContentType, context)
        }
        return true
    }

    override fun onDestroyActionMode(mode: ActionMode) {
        mode.menu.findItem(R.id.actionRemoveFromAlbum).isVisible = false
    }

    override fun onFabClick(context: Context, contents: List<Coverable>) {
        createAlbum(contents, context)
    }

    override fun onOptionsItemSelected(item: MenuItem, context: Context, currentCollection: Collection): Boolean {
        if (currentCollection !is Album) return false
        when (item.itemId) {
            R.id.actionRename -> rename(currentCollection, context)
        }
        return true
    }

    private fun createAlbum(contents: List<Coverable>, context: Context) {
        CoverableMutator.createNestedAlbum(contents, context) {
            alert(ActionAlert(true))
        }
    }

    private fun removeContents(coverables: List<Coverable>, context: Context) {
        CoverableMutator.removeFromAlbum(coverables, context) {
            alert(ActionAlert(true))
        }
    }

    private fun deleteContents(coverables: List<Coverable>, selectedContentType: String, context: Context) {
        when (selectedContentType) {
            CollectionManager.ALBUM_KEY -> {
                @Suppress("UNCHECKED_CAST")
                CoverableMutator.delete(coverables as List<Album>, false, context) {
                    alert(ActionAlert(true))
                }
            }
        }
    }

    private fun rename(album: Album, context: Context) {
        CoverableMutator.rename(album, context) {
            alert(ActionAlert(true))
        }
    }

}