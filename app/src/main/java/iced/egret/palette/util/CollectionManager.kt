package iced.egret.palette.util

import android.content.Context
import android.content.Intent
import androidx.fragment.app.FragmentActivity
import iced.egret.palette.R
import iced.egret.palette.model.*
import iced.egret.palette.model.Collection
import iced.egret.palette.recyclerview_component.CoverViewHolder
import java.util.*
import kotlin.collections.ArrayList

object CollectionManager {

    private var mCollections : MutableList<Collection> = ArrayList()
    private var mCollectionStack = ArrayDeque<Collection>()

    // Aliases
    var currentCollection: Collection?
        get() = mCollectionStack.peek()
        private set(value) = mCollectionStack.push(value)
    val contents: MutableList<Coverable>
        get() = currentCollection?.getContents() ?: ArrayList()
    val albums: List<Album>
        get() = mCollections.filterIsInstance<Album>()
    val folders: List<Folder>
        get() = mCollections.filterIsInstance<Folder>()

    fun setup(activity: FragmentActivity) {

        val root = Storage.retrievedFolders.firstOrNull()

        if (root != null) {

            // defensive
            mCollectionStack.clear()
            mCollections.clear()

            for (folder in root.folders) {
                val practicalRoot = getPracticalRoot(folder)
                mCollections.add(practicalRoot)
            }

            mCollections[0].name = activity.getString(R.string.external_storage_name)
            mCollectionStack.push(mCollections[0])

        }

        mCollections.addAll(Storage.retrievedAlbums)

    }

    fun getCollections() : MutableList<Collection> {
        return mCollections
    }

    fun createNewAlbum(name: String, addToCurrent: Boolean = false) : Album {
        val newAlbum : Album
        if (addToCurrent && currentCollection is Album) {
            val currentAlbum = currentCollection as Album
            newAlbum = Album(name, path = "${currentAlbum.path}/$name")
            currentAlbum.addAlbum(newAlbum)
        }
        else {
            newAlbum = Album(name, path = name)
            mCollections.add(newAlbum)
        }
        Storage.saveAlbumsToDisk(albums)
        return newAlbum
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

    /**
     * Launch an item by updating current collection, or creating activity.
     *
     * @return Adapter needs to be updated (true) or not (false)
     */
    fun launch(item: Coverable, holder: CoverViewHolder, position: Int = -1) : Boolean {
        if (!item.terminal) {
            if (item as? Collection != null) {
                currentCollection = item
                return true
            }
        }
        else {
            if (item as? TerminalCoverable != null) {
                val context : Context? = holder.ivItem?.context
                val intent = Intent(context, item.activity)
                val key = context?.getString(R.string.intent_item_key)
                intent.putExtra(key, position)
                context?.startActivity(intent)
            }
        }
        return false
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
     * Given a folder in /storage/, return folder with path
     * /storage/emulated/0 or /storage/<SD-CARD ID>,
     * since emulated is always empty anyways
     */
    private fun getPracticalRoot(folder: Folder) : Folder {
        var f = folder
        if (f.name == "emulated") {
            f = f.folders.first()
        }
        return f
    }

}