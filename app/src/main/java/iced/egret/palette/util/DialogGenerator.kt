package iced.egret.palette.util

import android.content.Context
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.input
import com.afollestad.materialdialogs.list.listItemsMultiChoice
import iced.egret.palette.R
import iced.egret.palette.model.Album
import iced.egret.palette.model.Collection
import iced.egret.palette.model.Folder

/**
 * Shows MaterialDialogs with custom actions on confirmation.
 */
object DialogGenerator {

    fun createAlbum(context: Context, onConfirm: (CharSequence) -> Unit) {
        MaterialDialog(context).show {
            title(R.string.title_album_form)
            input(hintRes = R.string.hint_set_name, maxLength = Album.NAME_MAX_LENGTH) {
                _, charSequence ->
                onConfirm(charSequence)
            }
            positiveButton(R.string.action_create_album)
            negativeButton()
        }
    }

    fun deleteAlbum(context: Context, onConfirm: () -> Unit) {
        MaterialDialog(context).show {
            title(R.string.title_delete_album_confirm)
            message(R.string.message_delete_album_confirm)
            negativeButton()
            positiveButton(R.string.action_delete) {
                onConfirm()
            }
        }
    }

    fun addToAlbum(context: Context, onConfirm: (IntArray, List<Album>) -> Unit) {
        val albums = CollectionManager.getNestedAlbums()
        val albumPaths = albums.map { album -> album.path }
        MaterialDialog(context).show {
            title(R.string.action_add_to_album)
            listItemsMultiChoice(items = albumPaths) { _, indices, _ ->
                onConfirm(indices, albums)
            }
            negativeButton()
            positiveButton()
        }
    }

    fun removeFromAlbum(context: Context, type: String, onConfirm: () -> Unit) {
        MaterialDialog(context).show {
            title(R.string.title_remove)
            message(text = "Remove the selected $type from this album?")
            negativeButton()
            positiveButton {
                onConfirm()
            }
        }
    }

    fun showCollectionDetails(context: Context, collection: Collection) {
        val path = if (collection is Folder) collection.truePath else collection.path
        MaterialDialog(context).show {
            message(text = path)
        }
    }

}