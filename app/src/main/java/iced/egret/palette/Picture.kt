package iced.egret.palette

import android.content.Context
import android.net.Uri
import android.widget.ImageView
import com.bumptech.glide.Glide
import java.io.File

class Picture(override val name: String, path: String) : TerminalCoverable {

    private val mPath = path
    private val mFile = File(mPath)
    private val mUri = Uri.fromFile(mFile)

    override val terminal = true
    override val cover = mutableMapOf(
        "uri" to mUri
    )

    override fun loadCoverInto(imageView: ImageView?, context: Context) {
        if (imageView != null) {
            Glide.with(context).load(cover["uri"]).placeholder(R.drawable.ic_folder_silver_24dp).into(imageView)
        }
    }
}