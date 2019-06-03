package iced.egret.palette

import android.content.Context
import android.widget.ImageView

interface Coverable {
    val terminal : Boolean
    val name : String
    val cover: MutableMap<String, *>
    fun loadCoverInto(imageView: ImageView?, context: Context)
}

interface TerminalCoverable : Coverable {
    override val terminal: Boolean
        get() = true
}