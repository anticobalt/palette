package iced.egret.palette.util

import android.content.ContentResolver
import android.content.Intent
import android.graphics.Bitmap
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import iced.egret.palette.R
import iced.egret.palette.model.*
import iced.egret.palette.model.Collection
import java.io.File
import java.util.*
import kotlin.collections.ArrayList

object CollectionManager {

    const val BASE_STORAGE_PATH = "emulated"
    const val PRACTICAL_BASE_STORAGE_PATH = "$BASE_STORAGE_PATH/0"
    const val BASE_STORAGE_NAME = "Main Storage"
    const val FOLDER_KEY = "folders"
    const val ALBUM_KEY = "albums"
    const val PICTURE_KEY = "pictures"

    private var storageRoot : Folder? = null
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

    fun setup() {

        storageRoot = Storage.retrievedFolders.firstOrNull()
        val displayedFolders = storageRoot?.folders ?: emptyList()

        // defensive
        mCollectionStack.clear()
        mCollections.clear()

        // Add Folders
        mCollections.addAll(displayedFolders)
        val baseStorage = mCollections.find { collection -> collection.path == BASE_STORAGE_PATH }
        if (baseStorage != null) {  // should always be true
            baseStorage.name = BASE_STORAGE_NAME
            mCollections.remove(baseStorage)
            mCollections.add(0, baseStorage)
            unwindStack(PRACTICAL_BASE_STORAGE_PATH)
        }

        // Add Albums
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

        val currentMap = (currentCollection?.contentsMap ?: mutableMapOf())
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
    fun launch(item: Coverable, position: Int = -1, callingFragment: Fragment? = null, requestCode : Int = -1) : Boolean {
        if (!item.terminal) {
            if (item as? Collection != null) {
                when (item.name) {
                    BASE_STORAGE_NAME -> unwindStack(PRACTICAL_BASE_STORAGE_PATH) // skip the empty sub-folders
                    else -> currentCollection = item
                }
                return true
            }
        }
        else {
            if (item as? TerminalCoverable != null) {
                val intent = Intent(callingFragment?.context, item.activity)
                val key = callingFragment?.getString(R.string.intent_item_key)
                intent.putExtra(key, position)
                callingFragment?.startActivityForResult(intent, requestCode)
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
     * Given a path to a Collection, use it to restore stack state.
     * If can't unwind completely, do it as much as possible, then return.
     */
    fun unwindStack(path: String) {
        val levels = path.split("/")
        var levelIndex = 0
        var collectionsOnLevel = mCollections.toList()

        for (level in levels) {
            // Use path to find Collection because those (unlike names) are immutable
            currentCollection = collectionsOnLevel.find {
                collection -> collection.path.split("/")[levelIndex] == level }
                    ?: return
            collectionsOnLevel = (currentCollection as Collection).getContents().filterIsInstance<Collection>()
            levelIndex += 1
        }
    }

    /**
     * Keeping it simple and using a recursive search.
     * Traversing Folders that are organized pseudo-hierarchically is difficult.
     */
    fun getFolderByTruePath(truePath: String, startFolders : List<Folder> = folders) : Folder? {
        val cleanPath = cleanPath(truePath)
        var matchedFolder : Folder? = null

        for (folder in startFolders) {
            val folderCleanPath = cleanPath(folder.filePath)
            matchedFolder = if (folderCleanPath == cleanPath) folder
            else getFolderByTruePath(truePath, folder.folders)
            // stop if match found
            if (matchedFolder != null) break
        }
        return matchedFolder
    }

    private fun betterGetFolderByTruePath(truePath: String, startFolder: Folder? = null,
                                          onMissing : (Folder, List<String>, Int) -> Folder ) : Folder? {

        var folder = startFolder ?: storageRoot ?: return null
        val cleanWorkingPath = cleanPath(truePath)
        val cleanStartPath = cleanPath(folder.filePath)
        val levels = cleanWorkingPath.split("/")

        // start at children level, not own level
        val index = getDepthOfPath(cleanStartPath, cleanWorkingPath)
        if (index == 0) return null

        while (folder.folders.isNotEmpty()) {
            folder = folder.folders.find {child -> child.filePath.split("/")[index] == levels[index]}
                    ?: return onMissing(folder, levels, index)
        }

        return if (cleanPath(folder.filePath) == cleanWorkingPath) folder
        else null

    }

    private fun cleanPath(path: String) : String {
        return path.trim { char -> char == '/'}
    }

    /**
     * Assumes either both OR neither paths have trailing backslash.
     * If not the case, there will be off-by-one error.
     *
     *  Result example: path "files/music" has depth of 2 relative to path "files/music/r.mp3"
     */
    private fun getDepthOfPath(pathToCheck: String, referencePath: String) : Int {
        if (!referencePath.startsWith(pathToCheck)) return 0
        val initial = referencePath.split("/").size
        val final = referencePath.removePrefix("$pathToCheck/").split("/").size
        return initial - final
    }

    /**
     * Start with FileObject, build parent Folders until you reach a Folder that already exists.
     */
    private fun buildAncestorFolders(fileObject: FileObject) : Folder? {
        val pathToParent = fileObject.parentFilePath
        val parent = betterGetFolderByTruePath(pathToParent) { folder, levels, i ->
            var index = i + 1
            var workingFolder = folder
            var path = workingFolder.filePath.removeSuffix("/")
            while (index < levels.size) {
                val name = levels[index]
                path += "$/name"
                workingFolder = Folder(name, path, parent = workingFolder)
                index += 1
            }
            workingFolder
        }
        fileObject.parent = parent
        return parent
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
     * Save a Picture to disk and update Collections as required.
     */
    fun createPictureFromBitmap(bitmap: Bitmap, name: String, location: String, isNew: Boolean,
                                sdCardFile: DocumentFile?, contentResolver: ContentResolver) : File? {

        val file = Storage.saveBitmapToDisk(bitmap, name, location, sdCardFile, contentResolver) ?: return null
        val picture : Picture
        val folder : Folder

        // Get Picture from Folder, as it's guaranteed to reside in there if it exists, unlike in Album
        folder = getFolderByTruePath(location) ?: return file  // should never return
        picture = folder.getPictureByPath(file.path) ?: Picture(name, file.path)

        // Update Folder
        if (!isNew) folder.removePicture(picture)  // move Picture
        folder.addPicture(picture, toFront = true)

        // Update current Collection if it's an Album, otherwise another update would be redundant
        if (currentCollection is Album) {
            if (!isNew) currentCollection?.removePicture(picture)  // move Picture
            currentCollection?.addPicture(picture, toFront = true)
            Storage.saveAlbumsToDisk(albums)
        }

        return file
    }

    /**
     * Moves picture on disk, and updates Folders. If Folder updating fails (which should
     * never happen), the moved file is still returned.
     *
     * @return The old and new Files, or null if move fails.
     */
    fun movePicture(position: Int, folderFile: File,
                    sdCardFile: DocumentFile?, contentResolver: ContentResolver) : Pair<File, File>? {

        val picture = currentCollection?.pictures?.get(position) ?: return null
        val oldFile = File(picture.filePath)
        val newFile = Storage.moveFile(picture.filePath, folderFile, sdCardFile, contentResolver)
                ?: return null
        val files = Pair(oldFile, newFile)

        // Remove from old Folder
        val oldFolder = getFolderByTruePath(picture.fileLocation)
                ?: return files  // should never return here
        oldFolder.removePicture(picture)

        // Update Picture's path
        picture.filePath = newFile.path

        // Add to new Folder
        val newFolder = getFolderByTruePath(folderFile.path)
                ?: buildAncestorFolders(picture)  // returns direct parent
                ?: return files  // should never return here
        newFolder.addPicture(picture, toFront = true)

        // Save changes
        Storage.saveAlbumsToDisk(albums)

        return files
    }

}