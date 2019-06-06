package iced.egret.palette

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.gson.Gson
import java.io.BufferedReader
import java.io.File
import java.io.FileNotFoundException
import java.util.*
import kotlin.collections.ArrayList

object CollectionManager {

    private const val rootCacheFileName = "root-cache.json"

    private var mContents: MutableList<Coverable> = ArrayList()
    private var mRoot : Folder? = null
    private var mCollectionStack = ArrayDeque<Collection>()

    private var gson = Gson()

    fun initRootFolder(activity: Activity) {
        // TODO: account for multiple roots

        getRootFromDisk(activity.filesDir)
        val root = mRoot
        if (root == null) {
            val folders = Storage.getPictureFoldersMediaStore(activity)
            if (folders.isNotEmpty()) {
                val folder = folders[0]
                mRoot = folder
                mContents = folder.getContents()
                mCollectionStack.clear()
                mCollectionStack.push(folder)
                saveRootToDisk(activity.filesDir)
            }
        } else {
            mContents = root.getContents()
            mCollectionStack.clear()
            mCollectionStack.push(root)
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

    private fun saveRootToDisk(fileDirectory : File) {
        val rootData = mRoot?.toDataClass()
        val json = gson.toJson(rootData)
        try {
            File(fileDirectory, rootCacheFileName).writeText(json)
        }
        catch (e : FileNotFoundException) {
            File(fileDirectory, rootCacheFileName).createNewFile()
            File(fileDirectory, rootCacheFileName).writeText(json)
        }
    }

    private fun getRootFromDisk(fileDirectory : File) {
        try {
            val json = File(fileDirectory, rootCacheFileName).bufferedReader().readText()
            val rootData = gson.fromJson(json, SerializedFolder::class.java)
            mRoot = rootData.toFullClass()
        }
        catch (e : FileNotFoundException) {
            Log.i("collection-manager", "couldn't find root cache when trying to read")
        }
    }

}