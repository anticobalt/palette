package iced.egret.palette.util

import android.content.Context
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.input
import iced.egret.palette.R
import iced.egret.palette.model.Album

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
        val dialog = MaterialDialog(context)
        dialog.title(R.string.title_delete_album_confirm)
        dialog.message(R.string.message_delete_album_confirm)
        dialog.negativeButton()
        dialog.positiveButton(R.string.action_delete_albums) {
            onConfirm()
        }
        dialog.show()
    }

}