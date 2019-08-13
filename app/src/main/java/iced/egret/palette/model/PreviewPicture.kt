package iced.egret.palette.model

import android.net.Uri

/**
 * A hacky class that allows loading into ImageViews by content:// URI instead of file:// URI.
 */
class PreviewPicture(name: String, contentUri: Uri) : Picture(name, name) {
    override val uri = contentUri
}