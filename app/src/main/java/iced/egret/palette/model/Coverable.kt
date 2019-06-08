package iced.egret.palette.model

import android.app.Activity
import android.os.Parcelable
import iced.egret.palette.adapter.CoverViewHolder

interface Coverable {
    val terminal : Boolean
    var name : String
    val cover : MutableMap<String, *>
    fun loadCoverInto(holder: CoverViewHolder)
}

interface TerminalCoverable : Coverable {
    override val terminal: Boolean
        get() = true
    val activity: Class<out Activity>
    fun toDataClass() : Parcelable
}