package iced.egret.palette.model

import android.app.Activity
import android.os.Parcelable
import iced.egret.palette.adapter.CollectionRecyclerViewAdapter

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