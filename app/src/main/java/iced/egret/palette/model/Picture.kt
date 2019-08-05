package iced.egret.palette.model

import android.graphics.BitmapFactory
import android.net.Uri
import android.view.View
import android.widget.ImageView
import androidx.exifinterface.media.ExifInterface
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import iced.egret.palette.R
import iced.egret.palette.flexible.CoverViewHolder
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/***
 * Properties:
 * - name
 * - filePath
 * - uri
 * - fileLocation
 * - parent
 * - deletable
 * - terminal
 * - cover
 * - icon
 * - activity
 * - mimeType
 * - fileSize
 * - height
 * - width
 * - orientation
 * - lastModifiedDate
 * - createdDate
 */
class Picture(override var name: String, override var filePath: String) : TerminalCoverable, FileObject {

    // Getters computed lazily, so that changing path doesn't break everything
    private val file: File
        get() = File(filePath)
    val uri: Uri
        get() = Uri.fromFile(file)
    val fileLocation
        get() = filePath.removeSuffix(name).removeSuffix("/")

    override val terminal = true
    override val cover: MutableMap<String, Uri>
        get() = mutableMapOf(
                "uri" to uri
        )

    override val icon: Nothing? = null
    override var parent: FileObject? = null
    override val deletable = true

    // Image Properties
    val mimeType: String
        get() {
            // Tries to get type from actual file instead of reading off the extension
            // https://stackoverflow.com/a/19739471
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true  // don't allocate memory for pixels
            BitmapFactory.decodeFile(filePath, options)
            return options.outMimeType
        }
    val isJpgOrPng: Boolean
        get() {
            val mimeType = this.mimeType
            return mimeType in setOf("image/jpeg", "image/png")
        }
    val fileSize: String
        get() = parseFileSize(file.length())
    val height: Int
        get() {
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true  // don't allocate memory for pixels
            BitmapFactory.decodeFile(filePath, options)
            return options.outHeight
        }
    val width: Int
        get() {
            val options = BitmapFactory.Options()
            options.inJustDecodeBounds = true  // don't allocate memory for pixels
            BitmapFactory.decodeFile(filePath, options)
            return options.outWidth
        }
    val orientation: String
        get() {
            val rawInt = ExifInterface(filePath).getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_UNDEFINED)
            return parseOrientation(rawInt)
        }

    val lastModifiedDate: String
        get() {
            val stamp = Date(file.lastModified()).toString()
            return parseDateStamp(stamp, "EEE MMM dd HH:mm:ss zzz yyyy")
        }
    val createdDate: String
        get() {
            val exif = ExifInterface(filePath)
            val stamp = exif.getAttribute(ExifInterface.TAG_DATETIME_ORIGINAL)
                    ?: exif.getAttribute(ExifInterface.TAG_DATETIME)
                    ?: return "Unknown"
            return parseDateStamp(stamp, "yyyy:MM:dd HH:mm:ss")
        }

    private fun parseFileSize(bytes: Long): String {
        val format = "%.2f"
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format(format, bytes / 1024.toDouble()) + " KB"
            bytes < 1024 * 1024 * 1024 -> String.format(format, bytes / (1024 * 1024.toDouble())) + " MB"
            else -> String.format(format, bytes / (1024 * 1024 * 1024.toDouble())) + " GB"
        }
    }

    /**
     * https://stackoverflow.com/a/20815893
     */
    private fun parseDateStamp(stamp: String, format: String): String {
        val returnFormat = "EEE, MMM dd, yyyy @ HH:mm:ss zzz"  // e.g. Thu, Apr 6, 2000 @ 17:45:21 UTC
        val parser = SimpleDateFormat(format, Locale.getDefault())
        val date = parser.parse(stamp)
        parser.applyPattern(returnFormat)
        return parser.format(date)
    }

    private fun parseOrientation(rawInt: Int): String {
        return when (rawInt) {
            ExifInterface.ORIENTATION_NORMAL -> "Normal"
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> "Flipped horizontally"
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> "Flipped vertically"
            ExifInterface.ORIENTATION_ROTATE_90 -> "Rotated 90 degrees clockwise"
            ExifInterface.ORIENTATION_ROTATE_180 -> "Rotated 180 degrees"
            ExifInterface.ORIENTATION_ROTATE_270 -> "Rotated 90 degrees counterclockwise"
            ExifInterface.ORIENTATION_TRANSPOSE -> "Flipped top-left to bottom-right"
            ExifInterface.ORIENTATION_TRANSVERSE -> "Flipped top-right to bottom-left"
            else -> "Not specified"
        }

    }

    override fun toString() = name

    override fun loadCoverInto(holder: CoverViewHolder) {

        val imageView = holder.ivItem
        val textView = holder.tvItem

        if (imageView != null) {
            val glide =
                    Glide.with(holder.itemView.context)
                            .load(cover["uri"])
                            .centerCrop()
                            .diskCacheStrategy(DiskCacheStrategy.ALL)
                            .error(R.drawable.ic_broken_image_black_128dp)

            // Build image with signature if possible
            buildGlideImage(glide, imageView, cover["uri"])
        }

        if (textView != null) {
            holder.tvItem.visibility = View.INVISIBLE
        }

    }

    override fun loadInto(view: View) {
        when (view) {
            is ImageView -> {
                val glide = Glide
                        .with(view.context)
                        .load(uri)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .placeholder(getPlaceholderDrawable(view))
                buildGlideImage(glide, view, uri)
            }
        }


    }

}
