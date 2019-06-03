package iced.egret.palette

import android.os.Environment

object CollectionManager {

    private var mContents: MutableList<Coverable>
    private val mRoot : Folder?
    private var mDefault : Collection?

    init {

        mRoot = getRootFolder()
        mDefault = getDefaultCollection()

        val contents = mDefault?.getContents()
        if (contents == null) {
            mContents = ArrayList()
        }
        else {
            mContents = contents
        }

    }

    fun getContents() : MutableList<Coverable> {
        return mContents
    }

    fun getContentByPosition(position: Int) : Coverable {
        return mContents[position]
    }

    fun launch(item: Coverable, adapter : CollectionRecyclerViewAdapter) {
        if (!item.terminal && item is Collection) {  // 2nd clause is cast
            adapter.update(item.getContents())
        }
        else {
            TODO()
        }
    }

    private fun getRootFolder() : Folder? {
        val ignore = arrayListOf("android", "music", "movies")
        return Storage.getPictureFolder(Environment.getExternalStorageDirectory(), ignore)
    }

    private fun getDefaultCollection() : Collection? {
        return mRoot
    }

}