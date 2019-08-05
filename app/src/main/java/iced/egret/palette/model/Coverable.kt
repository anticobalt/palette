package iced.egret.palette.model

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.view.View
import android.widget.ImageView
import androidx.exifinterface.media.ExifInterface
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import com.bumptech.glide.RequestBuilder
import com.bumptech.glide.signature.MediaStoreSignature
import iced.egret.palette.flexible.CoverViewHolder
import java.io.File
import java.io.FileNotFoundException

interface Coverable {
    val terminal: Boolean
    var name: String
    val cover: MutableMap<String, *>
    val icon: Int?
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
    private fun createMediaStoreSignature(context: Context, imageReference: Any?): MediaStoreSignature? {
        if (imageReference is Uri) {
            return try {
                val file = File(imageReference.path)
                val mimeType = context.contentResolver.getType(imageReference)
                val dateModified = file.lastModified()
                val orientation = ExifInterface(file.path).getAttributeInt(
                        ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_UNDEFINED)
                MediaStoreSignature(mimeType, dateModified, orientation)
            } catch (e: FileNotFoundException) {
                // Occurs when image file moved/deleted outside of app
                null
            }
        }
        return null
    }

    fun getPlaceholderDrawable(imageView: ImageView): Drawable {
        val progressDrawable = CircularProgressDrawable(imageView.context)
        progressDrawable.strokeWidth = 5f
        progressDrawable.centerRadius = 40f
        progressDrawable.start()
        return progressDrawable
    }

}

interface TerminalCoverable : Coverable {
    override val terminal: Boolean
        get() = true
    fun loadInto(view: View)
}