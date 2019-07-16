package iced.egret.palette.model

import android.net.Uri
import android.os.Parcelable
import android.view.View
import android.widget.ImageView
import com.bumptech.glide.Glide
import iced.egret.palette.R
import iced.egret.palette.activity.PictureViewActivity
import iced.egret.palette.recyclerview_component.CoverViewHolder
import kotlinx.android.parcel.Parcelize
import java.io.File

class Picture(override var name: String, var path: String) : TerminalCoverable {

    // Getters computed lazily, so that changing path doesn't break everything
    private val file : File
        get() = File(path)
    val uri : Uri
        get() = Uri.fromFile(file)
    val fileLocation
        get() = path.removeSuffix(name).removeSuffix("/")

    override val terminal = true
    override val cover : MutableMap<String, Uri>
        get() = mutableMapOf(
                "uri" to uri
        )

    override val icon: Nothing? = null
    override val activity = PictureViewActivity::class.java

    override fun toString(): String {
        return name
    }
    override fun toDataClass() = PictureData(name, path)

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

@Parcelize
data class PictureData(private val name: String, private val path: String) : Parcelable {
    fun toFullClass() = Picture(name, path)
}