package iced.egret.palette

import android.content.Context
import android.net.Uri
import android.os.Parcelable
import android.widget.ImageView
import com.bumptech.glide.Glide
import kotlinx.android.parcel.Parcelize
import java.io.File

class Picture(override val name: String, val path: String) : TerminalCoverable {

    val file : File = File(path)
    val uri : Uri = Uri.fromFile(file)

    override val terminal = true
    override val cover = mutableMapOf(
        "uri" to uri
    )
    override val activity = ViewPictureActivity::class.java

    override fun loadCoverInto(imageView: ImageView?, context: Context) {
        if (imageView != null) {
            Glide.with(context).load(cover["uri"]).placeholder(R.drawable.ic_folder_silver_24dp).into(imageView)
        }
    }

    override fun toDataClass() = PictureData(name, path)

}

@Parcelize
data class PictureData(private val name: String, private val path: String) : Parcelable {
    fun toFullClass() = Picture(name, path)
}