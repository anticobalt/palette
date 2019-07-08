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

    private const val originalExternalStorageName = "emulated"
    const val BASE_STORAGE_NAME = "Main Storage"
    const val FOLDER_KEY = "folders"
    const val ALBUM_KEY = "albums"
    const val PICTURE_KEY = "pictures"

    private var mCollections : MutableList<Collection> = ArrayList()
    private var mCollectionStack = ArrayDeque<Collection>()
    private val mContentsMap = linkedMapOf<String, List<Coverable>>(
            FOLDER_KEY to listOf(),
            ALBUM_KEY to listOf(),
            PICTURE_KEY to listOf()
    )

    val albums: List<Album>
        get() = mCollections.filterIsInstance<Album>()
    val folders: List<Folder>
        get() = mCollections.filterIsInstance<Folder>()

    var currentCollection: Collection?
        get() = mCollectionStack.peek()
        private set(value) = mCollectionStack.push(value)
    val contents: List<Coverable>
        get() = currentCollection?.getContents() ?: listOf()

    /**
     * Used by FlexibleAdapter to sort sections with Coverables
     */
    /*
    object SectionComparator : Comparator<IFlexible<*>> {

        // Sort by same order as in mContentsMap
        override fun compare(p0: IFlexible<*>, p1: IFlexible<*>): Int {
            val types = mContentsMap.keys.map {key -> key.toLowerCase()}
            var title0 = ""
            var title1 = ""

            if (p0 is SectionHeaderItem && p1 is SectionHeaderItem) {
                title0 = p0.title.toLowerCase()
                title1 = p1.title.toLowerCase()
            }
            else if (p0 is CoverableItem && p1 is CoverableItem) {
                title0 = p0.header.title.toLowerCase()
                title1 = p1.header.title.toLowerCase()
            }
            else if (p0 is SectionHeaderItem && p1 is CoverableItem){
                title0 = p0.title.toLowerCase()
                title1 = p1.header.title.toLowerCase()
            }
            else if (p0 is CoverableItem && p1 is SectionHeaderItem){
                title0 = p0.header.title.toLowerCase()
                title1 = p1.title.toLowerCase()
            }

            return when {
                types.indexOf(title0) < types.indexOf(title1) -> -1
                types.indexOf(title0) == types.indexOf(title1) -> 0
                else -> 1
            }
        }
    }
    */

    fun setup(activity: FragmentActivity) {

        val root = Storage.retrievedFolders.firstOrNull()
        if (root != null) {

            // defensive
            mCollectionStack.clear()
            mCollections.clear()

            for (folder in root.folders) {
                //val practicalRoot = getPracticalRoot(folder)
                mCollections.add(folder)
            }

            val externalStorage = mCollections.find {collection -> collection.name == originalExternalStorageName }
            if (externalStorage != null) {  // should always be true
                externalStorage.name = BASE_STORAGE_NAME
                mCollections.remove(externalStorage)
                mCollections.add(0, externalStorage)
                mCollectionStack.push(externalStorage)
            }
        }

        mCollections.addAll(Storage.retrievedAlbums)

    }

    fun getCollections() : MutableList<Collection> {
        return mCollections
    }

    fun getNestedAlbums(albums : List<Album> = this.albums, runningList: MutableList<Album> = mutableListOf()) : MutableList<Album> {
        for (album in albums) {
            runningList.add(album)
            getNestedAlbums(album.albums, runningList)
        }
        return runningList
    }

    /**
     * Lazily create ordered contents map
     */
    fun getContentsMap() : LinkedHashMap<String, List<Coverable>> {

        val currentMap = (currentCollection?.contentsMap ?: mapOf())
                as MutableMap<String, List<Coverable>>

        for (type in mContentsMap.keys)
        {
            val contentsOfType = currentMap[type] ?: listOf()
            mContentsMap[type] = contentsOfType
        }
        return mContentsMap
    }

    /**
     * @return Relative position of new album amongst other albums
     */
    fun createNewAlbum(name: String, addToCurrent: Boolean = false) : Int {
        val newAlbum : Album
        val position : Int
        if (addToCurrent && currentCollection is Album) {  // only albums can have albums
            val currentAlbum = currentCollection as Album
            newAlbum = Album(name, path = "${currentAlbum.path}/$name")
            position = currentAlbum.albums.size
            currentAlbum.addAlbum(newAlbum)
        }
        else {
            newAlbum = Album(name, path = name)
            position = mCollections.size
            mCollections.add(newAlbum)
        }
        Storage.saveAlbumsToDisk(albums)
        return position
    }

    /**
     * @param positions relative positions of albums within albums list
     */
    fun deleteAlbumsByRelativePosition(positions: List<Int>, deleteFromCurrent: Boolean = false) {
        val indices = positions.toSet()

        if (deleteFromCurrent) {
            val toDeleteAlbums = mutableListOf<Album>()
            val currentAlbum = (currentCollection as? Album) ?: return  // should always succeed
            for (position in positions) {
                toDeleteAlbums.add(currentAlbum.albums[position])
            }
            currentAlbum.removeAlbums(toDeleteAlbums)
        }
        else {
            val remainingCollections : MutableList<Collection> = folders.toMutableList()
            for (i in 0 until albums.size) {
                if (!indices.contains(i)) {
                    remainingCollections.add(albums[i])
                }
            }
            mCollections = remainingCollections
        }
        Storage.saveAlbumsToDisk(albums)
    }

    /**
     * Adds given contents to all given albums. Does not add if already exists in collection.
     *
     */
    fun addContentToAllAlbums(contents: List<Coverable>, albums: List<Album>) {
        if (contents.isEmpty()) return

        for (album in albums) {
            loop@ for (content in contents) {
                when (content) {
                    is Folder -> {
                        if (album.folders.contains(content)) continue@loop
                        album.addFolder(content)
                    }
                    is Album -> {
                        if (album.albums.contains(content)) continue@loop
                        album.addAlbum(content)
                    }
                    is Picture -> {
                        if (album.pictures.contains(content)) continue@loop
                        album.addPicture(content)
                    }
                }
            }
        }

        Storage.saveAlbumsToDisk(this.albums)

    }

    /**
     * Remove given contents from current album. References may still exist in other collections.
     */
    fun removeContentFromCurrentAlbum(contents: List<Coverable>) {
        if (currentCollection !is Album) return

        val album = currentCollection as Album
        for (content in contents) {
            when (content) {
                is Folder -> album.removeFolder(content)
                is Album -> album.removeAlbum(content)
                is Picture -> album.removePicture(content)
            }
        }

        Storage.saveAlbumsToDisk(albums)
    }

    /**
     * Launch an item by updating current collection, or creating activity.
     *
     * @return Adapter needs to be updated (true) or not (false)
     */
    fun launch(item: Coverable, holder: CoverViewHolder? = null, position: Int = -1, c: Context? = null) : Boolean {
        var context = c  // TODO: remove backwards compatibility

        if (!item.terminal) {
            if (item as? Collection != null) {
                currentCollection = item
                return true
            }
        }
        else {
            if (item as? TerminalCoverable != null) {
                if (holder != null) context = holder.ivItem?.context

                val intent = Intent(context, item.activity)
                val key = context?.getString(R.string.intent_item_key)
                intent.putExtra(key, position)
                context?.startActivity(intent)
            }
        }
        return false
    }

    fun revertToParent() : List<Coverable>? {
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

    fun resetStack() {
        mCollectionStack.clear()
        currentCollection = mCollections.firstOrNull()
    }

    /**
     * Given a real path to a Collection, use it to restore stack state.
     * If can't unwind completely, do it as much as possible, then return.
     */
    fun unwindStack(path: String) {
        val levels = path.split("/")
        var levelIndex = 0
        var collectionsOnLevel = mCollections.toList()

        for (level in levels) {
            // Use path to find Collection because those (unlike names) are immutable.
            // If can't find Collection, move to next level,
            // as Collections may have been merged during setup()
            // via a function like getPracticalRoot().
            currentCollection = collectionsOnLevel.find {
                collection -> collection.path.split("/")[levelIndex] == level }
                    ?: continue
            collectionsOnLevel = (currentCollection as Collection).getContents().filterIsInstance<Collection>()
            levelIndex += 1
        }
    }

    fun getCurrentCollectionPictures() : List<Picture> {
        val collection = currentCollection
        var pictures : List<Picture> = listOf()
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
        if (f.name == originalExternalStorageName) {
            f = f.folders.first()
            f.name = originalExternalStorageName
        }
        return f
    }

}