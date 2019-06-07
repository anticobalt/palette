package iced.egret.palette

import android.net.Uri
import android.os.Parcelable
import android.view.View
import com.bumptech.glide.Glide
import kotlinx.android.parcel.Parcelize
import java.io.File

class Picture(override val name: String, val path: String) : TerminalCoverable {

    private val file : File = File(path)
    val uri : Uri = Uri.fromFile(file)

    override val terminal = true
    override val cover = mutableMapOf(
        "uri" to uri
    )
    override val activity = ViewPictureActivity::class.java

    override fun loadCoverInto(holder: CollectionRecyclerViewAdapter.ViewHolder) {

        val imageView = holder.ivItem
        val textView = holder.tvItem

        // FIXME: loading is slow because the covers are large (e.g. set to 24dp and its fine)
        if (imageView != null) {
            Glide.with(holder.itemView.context)
                    .load(cover["uri"])
                    .centerCrop()
                    .placeholder(R.drawable.ic_image_silver_128dp)
                    .error(R.drawable.ic_broken_image_silver_128dp)
                    .into(imageView)
        }

        if (textView != null) {
            holder.tvItem.visibility = View.INVISIBLE
        }

    }

    override fun toDataClass() = PictureData(name, path)

}

@Parcelize
data class PictureData(private val name: String, private val path: String) : Parcelable {
    fun toFullClass() = Picture(name, path)
}