package iced.egret.palette.util

import android.content.Context
import android.content.Intent
import android.support.v4.app.FragmentActivity
import android.util.Log
import com.google.gson.Gson
import iced.egret.palette.R
import iced.egret.palette.adapter.CollectionViewAdapter
import iced.egret.palette.model.*
import iced.egret.palette.model.Collection
import java.io.File
import java.io.FileNotFoundException

object CollectionManager {

    private const val rootCacheFileName = "root-cache.json"

    private var mCurrentCollection: Collection? = null
    private var mContents: MutableList<Coverable> = ArrayList()
    private var mRoot : Folder? = null

    private var gson = Gson()

    fun initRootFolder(activity: FragmentActivity) {
        // TODO: account for multiple roots

        val folders = Storage.getPictureFoldersMediaStore(activity)
        if (folders.isNotEmpty()) {
            val folder = getPracticalRoot(folders[0])
            folder.name = activity.getString(R.string.external_storage_name)
            folder.parent = null
            mRoot = folder
            mCurrentCollection = folder
            mContents = folder.getContents()
        }

    }

    fun getCollections() : MutableList<Collection> {
        return mutableListOf(mRoot as Folder)
    }

    fun getCollectionByPosition(position: Int) : Collection {
        return mRoot as Collection
    }

    fun getContents() : MutableList<Coverable> {
        return mContents
    }

    fun launch(item: Coverable, adapter : CollectionViewAdapter, position: Int = -1) {
        if (!item.terminal) {
            if (item as? Collection != null) {
                val contents = item.getContents()
                adapter.update(contents)
                mCurrentCollection = item
                mContents = contents
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

    fun revertToParent() : MutableList<Coverable>? {
        val siblings = mCurrentCollection?.parent?.getContents()
        if (siblings != null) {
            mCurrentCollection = mCurrentCollection!!.parent  // not null b/c siblings not null
        }
        return siblings
    }

    fun getCurrentCollectionPictures() : MutableList<Picture> {
        val collection = mCurrentCollection
        var pictures : MutableList<Picture> = ArrayList()
        if (collection != null) {
            pictures = collection.pictures
        }
        return pictures
    }

    fun getCurrentCollectionName() : String? {
        return mCurrentCollection?.name
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
            val rootData = gson.fromJson(json, FolderData::class.java)
            mRoot = rootData.toFullClass()
        }
        catch (e : FileNotFoundException) {
            Log.i("collection-manager", "couldn't find root cache when trying to read")
        }
    }

    /**
     * Return folder with path /storage/emulated/0,
     * since storage and emulated are always empty anyways
     */
    private fun getPracticalRoot(folder: Folder) : Folder {
        var f = folder
        while (f.path != "/storage/emulated/0") {
            f = f.folders.first()
        }
        return f
    }

}