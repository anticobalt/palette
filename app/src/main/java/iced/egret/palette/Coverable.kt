package iced.egret.palette

import android.app.Activity
import android.content.Context
import android.os.Parcelable
import android.widget.ImageView

interface Coverable {
    val terminal : Boolean
    val name : String
    val cover : MutableMap<String, *>
    fun loadCoverInto(holder: CollectionRecyclerViewAdapter.ViewHolder)
}

interface TerminalCoverable : Coverable {
    override val terminal: Boolean
        get() = true
    val activity: Class<out Activity>
    fun toDataClass() : Parcelable
}