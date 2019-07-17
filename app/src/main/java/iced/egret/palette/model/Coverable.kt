package iced.egret.palette.model

import android.app.Activity
import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.widget.ImageView
import androidx.exifinterface.media.ExifInterface
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.signature.MediaStoreSignature
import iced.egret.palette.recyclerview_component.CoverViewHolder
import java.io.File

interface Coverable {
    val terminal : Boolean
    var name : String
    val cover : MutableMap<String, *>
    val icon : Int?
    fun loadCoverInto(holder: CoverViewHolder)

    /**
     * Shorthand for using Glide to put image into ImageView with and without signature.
     *
     * @param builder The result of Glide.with().load()... etc
     * @param imageView Where the image will end up
     * @param identifier Unique to the image; makes signature. Only URIs can create signatures.
     */
    fun buildGlideImage(builder: RequestBuilder<Drawable>, imageView: ImageView, identifier: Any?) {
        val signature = createMediaStoreSignature(imageView.context, identifier)
        if (signature != null) builder.signature(signature).into(imageView)
        else builder.into(imageView)
    }

    /**
     * Create a signature object that allows Glide cache invalidation when image in collection is edited.
     *
     * https://bumptech.github.io/glide/doc/caching.html#custom-cache-invalidation
     * Getting MIME type: https://stackoverflow.com/a/12473985
     * Getting orientation: https://stackoverflow.com/a/20480741
     */
    private fun createMediaStoreSignature(context: Context, imageReference: Any?) : MediaStoreSignature? {
        if (imageReference is Uri) {
            val file = File(imageReference.path)
            val mimeType = context.contentResolver.getType(imageReference)
            val dateModified = file.lastModified()
            val orientation = ExifInterface(file.path).getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)

            return MediaStoreSignature(mimeType, dateModified, orientation)
        }
        return null
    }

}

interface TerminalCoverable : Coverable {
    override val terminal: Boolean
        get() = true
    val activity: Class<out Activity>
}