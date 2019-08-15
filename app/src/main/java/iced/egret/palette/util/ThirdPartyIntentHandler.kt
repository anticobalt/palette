package iced.egret.palette.util

import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.provider.MediaStore
import iced.egret.palette.model.PreviewPicture
import iced.egret.palette.model.inherited.FileObject
import java.io.File

object ThirdPartyIntentHandler {

    val previewPictures = mutableListOf<PreviewPicture>()

    /**
     * Try to find the Picture referenced by the Intent.
     * If it doesn't exist (i.e. file not on disk), then create a Preview Picture and return it.
     */
    fun getViewRequest(intent: Intent, contentResolver: ContentResolver,
                       onError: () -> FileObject?) : FileObject? {

        // Confirm that Picture referenced by intent exists, then return its Folder's path
        if (intent.action == Intent.ACTION_VIEW) {
            val uri : Uri = intent.data
                    ?: return onError()

            val picturePath = if (uri.scheme == "file") File(uri.toString()).path
            else getFilePathFromUri(uri, contentResolver)

            if (picturePath == null) {
                val preview = PreviewPicture(uri, contentResolver)
                previewPictures.add(preview)
                return preview
            }

            val folderPath = picturePath.split("/").dropLast(1).joinToString("/")
            val folder = CollectionManager.findFolderByPath(folderPath)
                    ?: return onError()

            return folder.findPictureByPath(picturePath) ?: return onError()
        }

        return null
    }

    /**
     * https://stackoverflow.com/a/17546561
     */
    private fun getFilePathFromUri(contentUri: Uri, contentResolver: ContentResolver): String? {
        var path: String? = null
        val projection = arrayOf(MediaStore.MediaColumns.DATA)
        val cursor = contentResolver.query(contentUri, projection, null, null, null)
        if (cursor?.moveToFirst() == true) {
            try {
                val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA)
                path = cursor.getString(columnIndex)
            } catch (e : IllegalArgumentException) {
                // No file path exists
            }
        }
        cursor?.close()
        return path
    }

}