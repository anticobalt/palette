package iced.egret.palette.model

import android.content.ContentResolver
import android.net.Uri

/**
 * A hacky class that allows loading into ImageViews by content:// URI instead of file:// URI.
 */
class PreviewPicture(name: String, contentUri: Uri, contentResolver: ContentResolver) : Picture(name, name) {
    override val uri = contentUri
    override val mimeType : String = contentResolver.getType(contentUri) ?: "image/webm"
}