package iced.egret.palette

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import java.util.*
import kotlin.collections.ArrayList

object CollectionManager {

    private var mContents: MutableList<Coverable> = ArrayList()
    private var mRoot : Folder? = null
    private var mDefault : Collection? = null
    var mCollectionStack = ArrayDeque<Collection>()

    fun initRootFolder(activity: Activity) {
        // TODO: account for multiple roots
        val folders = Storage.getPictureFoldersMediaStore(activity)
        if (folders.isNotEmpty()) {
            val folder = folders[0]
            mRoot = folder
            mContents = folder.getContents()
            mCollectionStack.clear()
            mCollectionStack.push(folder)
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
                mCollectionStack.push(item)
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

    fun getParentCollectionContents() : MutableList<Coverable>? {
        var contents : MutableList<Coverable>? = null
        if (mCollectionStack.size >= 2) {
            mCollectionStack.pop()
            val parentCollection = mCollectionStack.peek()
            contents = parentCollection.getContents()
        }
        return contents
    }

    fun getCurrentCollectionPictures() : MutableList<Picture> {
        val collection = mCollectionStack.peek()
        var pictures : MutableList<Picture> = ArrayList()
        if (collection != null) {
            pictures = collection.getPictures()
        }
        return pictures
    }

}