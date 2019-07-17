package iced.egret.palette.model

import android.net.Uri
import android.view.View
import android.widget.ImageView
import com.bumptech.glide.Glide
import iced.egret.palette.R
import iced.egret.palette.activity.PictureViewActivity
import iced.egret.palette.recyclerview_component.CoverViewHolder
import java.io.File

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
 */
class Picture(override var name: String, override var filePath: String) : TerminalCoverable, FileObject {

    // Getters computed lazily, so that changing path doesn't break everything
    private val file : File
        get() = File(filePath)
    val uri : Uri
        get() = Uri.fromFile(file)
    val fileLocation
        get() = filePath.removeSuffix(name).removeSuffix("/")

    override val terminal = true
    override val cover : MutableMap<String, Uri>
        get() = mutableMapOf(
                "uri" to uri
        )

    override val icon: Nothing? = null
    override val activity = PictureViewActivity::class.java
    override var parent: FileObject? = null
    override val deletable = true

    override fun toString() = name

    override fun loadCoverInto(holder: CoverViewHolder) {

        val imageView = holder.ivItem
        val textView = holder.tvItem

        if (imageView != null) {
            val glide =
                    Glide.with(holder.itemView.context)
                            .load(cover["uri"])
                            .centerCrop()
                            .error(R.drawable.ic_broken_image_black_128dp)
            // Build image with signature if possible
            buildGlideImage(glide, imageView, cover["uri"])
        }

        if (textView != null) {
            holder.tvItem.visibility = View.INVISIBLE
        }

    }

    fun loadPictureInto(imageView: ImageView) {
        val glide = Glide.with(imageView.context).load(uri)
        buildGlideImage(glide, imageView, uri)
    }

}
