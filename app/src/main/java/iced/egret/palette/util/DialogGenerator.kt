package iced.egret.palette.util

import android.annotation.SuppressLint
import android.content.Context
import android.os.Environment
import android.text.InputType
import android.view.View
import android.widget.TextView
import com.afollestad.materialdialogs.LayoutMode
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.WhichButton
import com.afollestad.materialdialogs.actions.setActionButtonEnabled
import com.afollestad.materialdialogs.bottomsheets.BottomSheet
import com.afollestad.materialdialogs.customview.customView
import com.afollestad.materialdialogs.customview.getCustomView
import com.afollestad.materialdialogs.files.FileFilter
import com.afollestad.materialdialogs.files.folderChooser
import com.afollestad.materialdialogs.input.getInputField
import com.afollestad.materialdialogs.input.input
import com.afollestad.materialdialogs.list.listItemsMultiChoice
import iced.egret.palette.R
import iced.egret.palette.model.Album
import iced.egret.palette.model.Folder
import iced.egret.palette.model.Picture
import iced.egret.palette.model.inherited.Collection
import iced.egret.palette.model.inherited.FileObject
import java.io.File

/**
 * Shows MaterialDialogs with custom actions on confirmation.
 */
object DialogGenerator {

    private fun hasInvalidCharacters(sequence: CharSequence): Boolean {
        val invalidSymbols = setOf('|', '/', '\\', '\'', '"', '?', '*', '<', '>', '+', '=', ':')
        return sequence.any { char -> char in invalidSymbols }
    }

    private fun albumNameError(name: CharSequence, nameInUse: (CharSequence) -> Boolean): String? {
        return when {
            nameInUse(name) -> "Album with name already exists"
            hasInvalidCharacters(name) -> "Invalid characters"
            name.firstOrNull() == ' ' -> "Cannot start with space"
            name.lastOrNull() == ' ' -> "Cannot end with space"
            name.isEmpty() -> "Empty name not allowed"
            else -> null
        }
    }

    private fun fileNameError(name: CharSequence): String? {
        return when {
            hasInvalidCharacters(name) -> "Invalid characters"
            name.firstOrNull() == ' ' -> "Cannot start with space"
            name.lastOrNull() == ' ' -> "Cannot end with space"
            name.isEmpty() -> "Empty name not allowed"
            else -> null
        }
    }

    fun createAlbum(context: Context, nameUsed: (CharSequence) -> Boolean, onConfirm: (CharSequence) -> Unit) {
        MaterialDialog(context).show {
            title(R.string.title_album_form)
            input(hintRes = R.string.hint_set_name,
                    maxLength = Album.NAME_MAX_LENGTH,
                    waitForPositiveButton = false,
                    inputType = InputType.TYPE_TEXT_FLAG_CAP_SENTENCES) { dialog, text ->
                val error = albumNameError(text, nameUsed)
                dialog.getInputField().error = error
                dialog.setActionButtonEnabled(WhichButton.POSITIVE, error == null)
            }
            positiveButton(R.string.action_create_album) {
                onConfirm(this.getInputField().text)
            }
            negativeButton()
        }
    }

    fun renameAlbum(context: Context, oldName: String, nameUsed: (CharSequence) -> Boolean,
                    onConfirm: (CharSequence) -> Unit) {

        MaterialDialog(context).show {
            title(R.string.action_rename)
            input(hintRes = R.string.hint_set_name,
                    prefill = oldName,
                    maxLength = Album.NAME_MAX_LENGTH,
                    waitForPositiveButton = false,
                    inputType = InputType.TYPE_TEXT_FLAG_CAP_SENTENCES) { dialog, text ->
                val error = albumNameError(text, nameUsed)
                dialog.getInputField().error = error
                dialog.setActionButtonEnabled(WhichButton.POSITIVE, error == null)
            }
            positiveButton {
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

    fun addToAlbum(context: Context, albumChoices: List<Album>, onConfirm: (IntArray) -> Unit) {
        val albumPaths = albumChoices.map { album -> album.path }
        MaterialDialog(context).show {
            title(R.string.action_add_to_album)
            listItemsMultiChoice(items = albumPaths) { _, indices, _ ->
                onConfirm(indices)
            }
            negativeButton()
            positiveButton()
        }
    }

    fun removeFromAlbum(context: Context, onConfirm: () -> Unit) {
        MaterialDialog(context).show {
            title(R.string.title_remove)
            message(R.string.warning_generic)
            negativeButton()
            positiveButton {
                onConfirm()
            }
        }
    }

    fun moveToRecycleBin(context: Context, onConfirm: () -> Unit) {
        MaterialDialog(context).show {
            title(R.string.action_delete)
            message(text = "Move to the recycle bin?")
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

    /**
     * No auto-dismiss to allow follow-up dialogs in case name already in use.
     */
    fun nameFile(context: Context, name: String, onConfirm: (CharSequence, MaterialDialog) -> Unit) {
        MaterialDialog(context).show {
            noAutoDismiss()
            title(R.string.action_save_file)
            // TODO: custom view to separate extension and name fields
            input(prefill = name, waitForPositiveButton = false) { dialog, text ->
                val error = fileNameError(text)
                dialog.getInputField().error = error
                dialog.setActionButtonEnabled(WhichButton.POSITIVE, error == null)
            }
            negativeButton {
                dismiss()
            }
            positiveButton {
                onConfirm(this.getInputField().text, this)
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

    fun moveTo(context: Context, initialPath: String? = null, onConfirm: (File) -> Unit) {
        val filter: FileFilter = { it.path.startsWith("/storage") }
        val initial = if (initialPath == null) Environment.getExternalStorageDirectory() else File(initialPath)
        MaterialDialog(context).show {
            folderChooser(
                    emptyTextRes = R.string.folder_empty,
                    initialDirectory = initial,
                    filter = filter) { _, file ->
                onConfirm(file)
                negativeButton()
            }
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
            message(text = "Permanently delete $typeString? This action is irreversible.")
            negativeButton()
            positiveButton {
                onConfirm()
            }
        }
    }

    /**
     * Show bottom sheet with picture details. Can supply alternate file path to show.
     */
    fun pictureDetails(context: Context, picture: Picture, filepath: String? = null) {

        fun populateWithMetadata(view: View, picture: Picture) {
            view.findViewById<TextView>(R.id.path).text = filepath ?: picture.filePath
            view.findViewById<TextView>(R.id.type).text = picture.mimeType
            view.findViewById<TextView>(R.id.size).text = picture.fileSize
            @SuppressLint("SetTextI18n")  // is not language specific
            view.findViewById<TextView>(R.id.dimensions).text = "${picture.width} x ${picture.height}"
            view.findViewById<TextView>(R.id.orientation).text = picture.orientation
            view.findViewById<TextView>(R.id.modified).text = picture.lastModifiedDate
            view.findViewById<TextView>(R.id.created).text = picture.createdDate
        }

        val dialog = MaterialDialog(context, BottomSheet(LayoutMode.WRAP_CONTENT))
        dialog.cornerRadius(res = R.dimen.bottom_sheet_corner_radius)
        dialog.customView(R.layout.content_bottomsheet_picture_details, horizontalPadding = true)
        populateWithMetadata(dialog.getCustomView(), picture)
        dialog.show()
    }

    fun clear(context: Context, onConfirm: () -> Unit) {
        MaterialDialog(context).show {
            title(R.string.action_clear)
            message(R.string.warning_generic)
            negativeButton()
            positiveButton {
                onConfirm()
            }
        }
    }

    fun clearAll(context: Context, onConfirm: () -> Unit) {
        MaterialDialog(context).show {
            title(R.string.action_clear_all)
            message(R.string.warning_generic)
            negativeButton()
            positiveButton {
                onConfirm()
            }
        }
    }

    fun setAsCover(picture: Picture, context: Context, onConfirm: (List<Collection>) -> Unit) {
        val candidates = mutableMapOf<String, Collection>()

        // Get (most) Folders that this Picture is in, directly or indirectly
        var obj: FileObject? = picture.parent
        val topFolders = CollectionManager.folders
        while (obj != null) {
            if (obj is Folder) candidates[obj.path] = obj
            if (obj in topFolders) break
            obj = obj.parent
        }
        // Get all Albums this Picture is directly in
        // TODO: handle indirection by making Albums doubly-linked
        for (album in CollectionManager.getNestedAlbums()) {
            if (picture in album.pictures) candidates[album.path] = album
        }
        // Get all pinned Albums
        for (album in CollectionManager.albums) {
            candidates[album.path] = album
        }

        val candidateCollections = candidates.values.sortedBy { c -> c.path }
        val disabledIndices = candidateCollections
                .filter { c -> c.hasCustomCoverable && c.cover["uri"] == picture.uri }
                .map { c -> candidateCollections.indexOf(c) }
                .toIntArray()

        MaterialDialog(context).show {
            title(R.string.action_set_as_cover)
            listItemsMultiChoice(
                    items = candidateCollections.map { c -> c.path },
                    disabledIndices = disabledIndices
            ) { _, indices, _ ->
                onConfirm(indices.map {i -> candidateCollections[i]})
            }
            negativeButton()
            positiveButton()
        }
    }

}