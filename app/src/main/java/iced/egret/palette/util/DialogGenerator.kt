package iced.egret.palette.util

import android.content.Context
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.setActionButtonEnabled
import com.afollestad.materialdialogs.files.FileFilter
import com.afollestad.materialdialogs.files.folderChooser
import com.afollestad.materialdialogs.input.getInputField
import com.afollestad.materialdialogs.input.input
import com.afollestad.materialdialogs.list.listItemsMultiChoice
import iced.egret.palette.R
import iced.egret.palette.model.Album
import iced.egret.palette.model.Collection
import java.io.File

/**
 * Shows MaterialDialogs with custom actions on confirmation.
 */
object DialogGenerator {

    fun createAlbum(context: Context, albumExists: (CharSequence) -> Boolean, onConfirm: (CharSequence) -> Unit) {
        MaterialDialog(context).show {
            title(R.string.title_album_form)
            input(hintRes = R.string.hint_set_name, maxLength = Album.NAME_MAX_LENGTH, waitForPositiveButton = false) {
                dialog, text ->
                    val isValid = !albumExists(text)
                    dialog.getInputField().error = if (isValid) {
                        null
                    } else {
                        "Album with name already exists"
                    }
                    dialog.setActionButtonEnabled(WhichButton.POSITIVE, isValid)
            }
            positiveButton(R.string.action_create_album) {
                onConfirm(this.getInputField().text)
            }
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

    fun moveToRecycleBin(context: Context, type: String, onConfirm: () -> Unit) {
        MaterialDialog(context).show {
            title(R.string.action_delete)
            message(text = "Move the selected $type to the recycle bin?")
            negativeButton()
            positiveButton {
                onConfirm()
            }
        }
    }

    fun showCollectionDetails(context: Context, collection: Collection) {
        val path = collection.path
        MaterialDialog(context).show {
            message(text =
            "Location: $path\n" +
                    "Number of pictures: ${collection.totalSize}"
            )
        }
    }

    fun nameFile(context: Context, name: String?, onConfirm: (CharSequence) -> Unit) {

        var nameWithoutExtension : String? = null
        var extension : String? = null
        val parts = name?.split(".")

        // names with periods are fair game
        if (parts != null && parts.size > 1) {
            extension = parts.last()
            nameWithoutExtension = parts.dropLast(1).joinToString(".")
        }

        MaterialDialog(context).show {
            noAutoDismiss()
            title(R.string.action_save_file)
            // TODO: custom view to separate extension and name fields
            input(prefill = "$nameWithoutExtension-1.$extension")
            negativeButton {
                dismiss()
            }
            positiveButton {
                onConfirm(this.getInputField().text)
            }
        }

    }

    fun confirmReplaceFile(context: Context, onConfirm: () -> Unit) {
        MaterialDialog(context).show {
            title(R.string.action_replace_file)
            message(R.string.message_replace_file)
            negativeButton()
            positiveButton {
                onConfirm()
            }
        }
    }

    fun moveFile(context: Context, onConfirm: (File) -> Unit) {
        val filter: FileFilter = {it.path.startsWith("/storage")}
        MaterialDialog(context).show {
            folderChooser(emptyTextRes = R.string.folder_empty, filter = filter) {
                dialog, file -> onConfirm(file)
            }
            negativeButton()
        }
    }

    fun restore(context: Context, typeString: String, onConfirm: () -> Unit) {
        MaterialDialog(context).show {
            title(R.string.action_restore)
            message(text = "Restore the selected $typeString?")
            negativeButton()
            positiveButton {
                onConfirm()
            }
        }
    }

    fun delete(context: Context, typeString: String, onConfirm: () -> Unit) {
        MaterialDialog(context).show {
            title(R.string.action_delete)
            message(text = "Permanently delete the selected $typeString? This action is irreversible.")
            negativeButton()
            positiveButton {
                onConfirm()
            }
        }
    }

}