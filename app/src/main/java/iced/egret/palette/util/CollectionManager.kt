package iced.egret.palette.util

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.fragment.app.FragmentActivity
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import iced.egret.palette.R
import iced.egret.palette.adapter.CollectionViewAdapter
import iced.egret.palette.model.*
import iced.egret.palette.model.Collection
import java.io.File
import java.io.FileNotFoundException
import java.util.*
import kotlin.collections.ArrayList

object CollectionManager {

    private const val rootCacheFileName = "root-cache.json"
    private const val albumsFileName = "albums.json"
    private lateinit var fileDirectory : File

    private var mRoot : Folder? = null
    private var mCollections : MutableList<Collection> = ArrayList()
    private var mCollectionStack = ArrayDeque<Collection>()
    private var gson = Gson()

    // Aliases
    private var mCurrentCollection: Collection?
        get() = mCollectionStack.peek()
        set(value) = mCollectionStack.push(value)
    private val mContents: MutableList<Coverable>
        get() {
            return mCurrentCollection?.getContents() ?: ArrayList()
        }

    fun setup(activity: FragmentActivity) {

        // TODO: account for multiple roots
        val folders = Storage.getPictureFoldersMediaStore(activity)
        if (folders.isNotEmpty()) {

            val folder = getPracticalRoot(folders[0])
            folder.name = activity.getString(R.string.external_storage_name)
            mRoot = folder

            // defensive
            mCollectionStack.clear()
            mCollections.clear()

            mCollectionStack.push(folder)
            mCollections.add(folder)

        }

        fileDirectory = activity.filesDir
        getAlbumsFromDisk()

    }

    fun getCollections() : MutableList<Collection> {
        return mCollections
    }

    fun createNewAlbum(name: String) : Album {
        val album = Album(name)
        mCollections.add(album)
        saveAlbumsToDisk()
        return album
    }

    fun deleteCollectionsByPosition(positions: ArrayList<Long>) {
        val indices = positions.toSet()
        val remainingCollections = ArrayList<Collection>()
        for (i in 0 until mCollections.size) {
            if (!indices.contains(i.toLong())) {
                remainingCollections.add(mCollections[i])
            }
        }
        mCollections = remainingCollections
        saveAlbumsToDisk()
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
        return if (mCollectionStack.size > 1) {
            mCollectionStack.pop()
            mContents
        }
        else {
            null
        }
    }

    fun clearStack() {
        mCollectionStack.clear()
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

    private fun saveAlbumsToDisk() {
        val albumsData = ArrayList<AlbumData>()
        for (collection in mCollections) {
            if (collection is Album) {
                albumsData.add(collection.toDataClass())
            }
        }
        val json = gson.toJson(albumsData)
        saveJsonToDisk(json, albumsFileName)
    }

    private fun getAlbumsFromDisk() {
        val json = readJsonFromDisk(albumsFileName)
        val type = object : TypeToken<ArrayList<AlbumData>>() {}.type
        if (json != null) {
            val albumsData = gson.fromJson<ArrayList<AlbumData>>(json, type)
            val albums  = ArrayList<Album>()
            for (data in albumsData) {
                albums.add(data.toFullClass())
            }
            mCollections.addAll(albums)
        }
    }

    private fun saveRootToDisk() {
        val rootData = mRoot?.toDataClass()
        val json = gson.toJson(rootData)
        saveJsonToDisk(json, rootCacheFileName)
    }

    private fun getRootFromDisk() {
        val json = readJsonFromDisk(rootCacheFileName)
        if (json != null) {
            val rootData = gson.fromJson(json, FolderData::class.java)
            mRoot = rootData.toFullClass()
        }
    }

    private fun saveJsonToDisk(json: String, fileName: String) {
        try {
            File(fileDirectory, fileName).writeText(json)
        }
        catch (e : FileNotFoundException) {
            File(fileDirectory, fileName).createNewFile()
            File(fileDirectory, fileName).writeText(json)
        }
    }

    private fun readJsonFromDisk(fileName: String) : String? {
        return try {
            File(fileDirectory, fileName).bufferedReader().readText()
        }
        catch (e : FileNotFoundException) {
            Log.i("collection-manager", "couldn't find $fileName when trying to read")
            null
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