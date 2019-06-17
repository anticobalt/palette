package iced.egret.palette.model

import android.net.Uri
import android.os.Parcelable
import android.view.View
import com.bumptech.glide.Glide
import iced.egret.palette.R
import iced.egret.palette.activity.PictureViewActivity
import iced.egret.palette.recyclerview_component.CoverViewHolder
import kotlinx.android.parcel.Parcelize
import java.io.File

class Picture(override var name: String, val path: String) : TerminalCoverable {

    private val file : File = File(path)
    val uri : Uri = Uri.fromFile(file)

    override val terminal = true
    override val deletable = false
    override val cover = mutableMapOf(
        "uri" to uri
    )
    override val activity = PictureViewActivity::class.java

    override fun toString(): String {
        return name
    }
    override fun toDataClass() = PictureData(name, path)

    override fun loadCoverInto(holder: CoverViewHolder) {

        val imageView = holder.ivItem
        val textView = holder.tvItem

        // FIXME: loading is slow because the covers are large (e.g. set to 24dp and its fine)
        if (imageView != null) {
            Glide.with(holder.itemView.context)
                    .load(cover["uri"])
                    .centerCrop()
                    .error(R.drawable.ic_broken_image_black_128dp)
                    .into(imageView)
        }

        if (textView != null) {
            holder.tvItem.visibility = View.INVISIBLE
        }

    }

}

@Parcelize
data class PictureData(private val name: String, private val path: String) : Parcelable {
    fun toFullClass() = Picture(name, path)
}