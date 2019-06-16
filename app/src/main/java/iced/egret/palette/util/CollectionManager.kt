package iced.egret.palette.util

import android.content.Context
import android.content.Intent
import androidx.fragment.app.FragmentActivity
import iced.egret.palette.R
import iced.egret.palette.adapter.CollectionViewAdapter
import iced.egret.palette.model.*
import iced.egret.palette.model.Collection
import java.util.*
import kotlin.collections.ArrayList

object CollectionManager {

    private var mRoot : Folder? = null
    private var mCollections : MutableList<Collection> = ArrayList()
    private var mCollectionStack = ArrayDeque<Collection>()

    // Aliases
    var currentCollection: Collection?
        get() = mCollectionStack.peek()
        private set(value) = mCollectionStack.push(value)
    val contents: MutableList<Coverable>
        get() {
            return currentCollection?.getContents() ?: ArrayList()
        }
    val albums: List<Album>
        get() = mCollections.filterIsInstance<Album>()

    fun setup(activity: FragmentActivity) {

        // TODO: account for multiple roots
        val folders = Storage.retrievedFolders
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

        mCollections.addAll(Storage.retrievedAlbums)

    }

    fun getCollections() : MutableList<Collection> {
        return mCollections
    }

    fun createNewAlbum(name: String, addToCurrent: Boolean = false) : Album {
        val album = Album(name)
        if (addToCurrent) {
            if (currentCollection is Album) (currentCollection as Album).addAlbum(album)
        }
        else mCollections.add(album)
        Storage.saveAlbumsToDisk(albums)
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
        Storage.saveAlbumsToDisk(albums)
    }

    fun launch(item: Coverable, adapter : CollectionViewAdapter, position: Int = -1) {
        if (!item.terminal) {
            if (item as? Collection != null) {
                val contents = item.getContents()
                adapter.update(contents)
                currentCollection = item
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
            contents
        }
        else {
            null
        }
    }

    fun clearStack() {
        mCollectionStack.clear()
    }

    fun getCurrentCollectionPictures() : MutableList<Picture> {
        val collection = currentCollection
        var pictures : MutableList<Picture> = ArrayList()
        if (collection != null) {
            pictures = collection.pictures
        }
        return pictures
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