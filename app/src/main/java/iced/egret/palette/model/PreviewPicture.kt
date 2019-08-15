package iced.egret.palette.model

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns

/**
 * A hacky class that allows loading into ImageViews by content:// URI instead of file:// URI.
 */
class PreviewPicture(contentUri: Uri, contentResolver: ContentResolver) : Picture("", "") {
    override val uri = contentUri
    override val mimeType: String = contentResolver.getType(contentUri) ?: "image/webm"

    init {
        val resolvedName = getName(contentUri, contentResolver)
        this.name = resolvedName
        this.filePath = resolvedName
    }

    /**
     * https://stackoverflow.com/a/38304115
     */
    private fun getName(uri: Uri, contentResolver: ContentResolver): String {
        val returnCursor = contentResolver.query(uri, null, null, null, null)!!
        val nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        returnCursor.moveToFirst()
        val name = returnCursor.getString(nameIndex)
        returnCursor.close()
        return name
    }
}