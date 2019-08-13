package iced.egret.palette.delegate

import android.content.Context
import android.content.Intent
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import iced.egret.palette.R
import iced.egret.palette.activity.SyncedFoldersActivity
import iced.egret.palette.delegate.inherited.CollectionViewDelegate
import iced.egret.palette.fragment.CollectionViewFragment
import iced.egret.palette.fragment.CollectionViewFragment.Constants.FOLDER_LIST_ACTIVITY_REQUEST
import iced.egret.palette.model.Album
import iced.egret.palette.model.Picture
import iced.egret.palette.model.inherited.Collection
import iced.egret.palette.model.inherited.Coverable
import iced.egret.palette.util.CollectionManager
import iced.egret.palette.util.CoverableMutator
import iced.egret.palette.util.Painter

class AlbumViewDelegate : CollectionViewDelegate() {

    override fun onBuildToolbar(toolbar: Toolbar) {
        toolbar.menu.findItem(R.id.actionRename).isVisible = true
        toolbar.menu.findItem(R.id.actionShowSyncFolders).isVisible = true
    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu, selectedContentType: String): Boolean {
        val removeFromAlbum = menu.findItem(R.id.actionRemoveFromAlbum)
        when (selectedContentType) {
            CollectionManager.PICTURE_KEY -> {
                removeFromAlbum.isVisible = true; Painter.paintDrawable(removeFromAlbum.icon)
            }
        }
        return true
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem, selectedContent: List<Coverable>,
                                     context: Context, selectedContentType: String): Boolean {

        when (item.itemId) {
            R.id.actionRemoveFromAlbum -> removeContents(selectedContent, context)
            R.id.actionDelete -> deleteContents(selectedContent, selectedContentType, context)
        }
        return true
    }

    override fun onDestroyActionMode(mode: ActionMode) {
        mode.menu.findItem(R.id.actionRemoveFromAlbum).isVisible = false
    }

    override fun onFabClick(context: Context, contents: List<Coverable>) {
        createAlbum(contents, context)
    }

    override fun onOptionsItemSelected(item: MenuItem, fragment: CollectionViewFragment, currentCollection: Collection): Boolean {
        if (currentCollection !is Album) return false
        when (item.itemId) {
            R.id.actionRename -> rename(currentCollection, fragment.context!!)
            R.id.actionShowSyncFolders -> showSyncFolders(currentCollection, fragment)
        }
        return true
    }

    private fun createAlbum(contents: List<Coverable>, context: Context) {
        CoverableMutator.createNestedAlbum(contents, context) {
            alert(ActionAlert(true))
        }
    }

    @Suppress("UNCHECKED_CAST")  // assume internal consistency
    private fun removeContents(coverables: List<Coverable>, context: Context) {
        if (hasSyncedPicture(coverables as List<Picture>)) {
            Toast.makeText(context, R.string.synced_is_not_removable_error, Toast.LENGTH_SHORT).show()
            return
        }
        CoverableMutator.removeFromAlbum(coverables, context) {
            alert(ActionAlert(true))
        }
    }

    private fun deleteContents(coverables: List<Coverable>, selectedContentType: String, context: Context) {
        when (selectedContentType) {
            CollectionManager.ALBUM_KEY -> {
                @Suppress("UNCHECKED_CAST")
                CoverableMutator.delete(coverables as List<Album>, true, context) {
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

    private fun hasSyncedPicture(pictures: List<Picture>): Boolean {
        val album = CollectionManager.currentCollection as Album
        return !album.ownsPictures(pictures)
    }

    private fun showSyncFolders(album: Album, fragment: CollectionViewFragment) {
        val intent = Intent(fragment.context, SyncedFoldersActivity::class.java)
        intent.putExtra(fragment.getString(R.string.intent_album_path_key), album.path)
        fragment.startActivityForResult(intent, FOLDER_LIST_ACTIVITY_REQUEST)
    }

}