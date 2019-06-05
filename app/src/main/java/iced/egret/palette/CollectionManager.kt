package iced.egret.palette

import android.content.Context
import android.content.Intent
import android.os.Environment

object CollectionManager {

    private var mContents: MutableList<Coverable>
    private val mRoot : Folder?
    private var mDefault : Collection?
    var mCurrentCollection: Collection?

    init {

        mRoot = getRootFolder()
        mDefault = getDefaultCollection()
        mCurrentCollection = getDefaultCollection()

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

    fun launch(item: Coverable, adapter : CollectionRecyclerViewAdapter, position: Int) {
        if (!item.terminal) {
            if (item as? Collection != null) {
                adapter.update(item.getContents())
                mCurrentCollection = item
            }
        }
        else {
            if (item as? TerminalCoverable != null) {
                val context : Context? = adapter.getContext()
                val intent = Intent(context, item.activity)
                val key = context?.getString(R.string.intent_item_key)
                intent.putExtra(key, position)
                context?.startActivity(intent)
            }

        }
    }

    fun getCurrentCollectionPictures() : MutableList<Picture> {
        val collection = mCurrentCollection
        var pictures : MutableList<Picture> = ArrayList()
        if (collection != null) {
            pictures = collection.getPictures()
        }
        return pictures
    }

    private fun getRootFolder() : Folder? {
        val ignore = arrayListOf("android", "music", "movies")
        return Storage.getPictureFolder(Environment.getExternalStorageDirectory(), ignore)
    }

    private fun getDefaultCollection() : Collection? {
        return mRoot
    }

}