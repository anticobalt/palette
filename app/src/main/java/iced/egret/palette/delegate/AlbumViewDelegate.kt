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
import iced.egret.palette.util.DialogGenerator
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
        val typePlural = selectedContentType.toLowerCase()
        val typeSingular = typePlural.dropLast(1)  // only works on s-appending plurals
        val typeString = if (coverables.size > 1) typePlural else typeSingular

        when (item.itemId) {
            R.id.actionRemoveFromAlbum -> {
                DialogGenerator.removeFromAlbum(context, typeString) {
                    CollectionManager.removeContentFromCurrentAlbum(coverables)
                    alert(ActionAlert("Removed ${coverables.size} $typeString.", true))
                }
            }
            R.id.actionDelete -> {
                when (selectedContentType) {
                    CollectionManager.ALBUM_KEY -> {
                        DialogGenerator.deleteAlbum(context) {
                            CollectionManager.deleteAlbumsByRelativePosition(
                                    adapter.selectedPositions, deleteFromCurrent = true)
                            alert(ActionAlert("Deleted ${adapter.selectedItemCount} $typeString", true))
                        }
                    }
                }
            }
        }

        return true
    }

    override fun onDestroyActionMode(mode: ActionMode) {
        mode.menu.findItem(R.id.actionRemoveFromAlbum).isVisible = false
    }

    override fun onFabClick(context: Context, contents: List<Coverable>) {

        fun albumExists(name: CharSequence): Boolean {
            val found = contents.find { coverable ->
                coverable is Album
                        && coverable.name == name.toString()
            }
            return found != null
        }

        fun createNewAlbum(name: CharSequence) {
            CollectionManager.createNewAlbum(name.toString(), addToCurrent = true)
            alert(ActionAlert("", true))
        }

        DialogGenerator.createAlbum(context, ::albumExists, ::createNewAlbum)
    }

    override fun onOptionsItemSelected(item: MenuItem, context: Context, currentCollection: Collection): Boolean {

        if (currentCollection !is Album) return false

        fun nameInUse(name: CharSequence): Boolean {
            val parent = currentCollection.parent
            if (parent != null) {
                return parent.albums.find {album -> album.name == name.toString()
                        && album != currentCollection } != null
            }
            else {
                return CollectionManager.albums.find {album -> album.name == name.toString()
                        && album != currentCollection } != null
            }
        }
        fun rename(charSequence: CharSequence) {
            CollectionManager.renameCollection(currentCollection, charSequence.toString())
            alert(ActionAlert("", true))
        }

        when (item.itemId) {
            R.id.actionRename -> {
                DialogGenerator.renameAlbum(context, currentCollection.name, ::nameInUse, ::rename)
            }
        }
        return true
    }

}