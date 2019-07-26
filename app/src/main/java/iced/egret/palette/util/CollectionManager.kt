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

    private const val STORAGE_PATH = "/storage"
    private const val MAIN_STORAGE_PATH = "$STORAGE_PATH/emulated"
    private const val PRACTICAL_MAIN_STORAGE_PATH = "$MAIN_STORAGE_PATH/0"
    private const val MAIN_STORAGE_NAME = "Main Storage"
    const val FOLDER_KEY = "folders"
    const val ALBUM_KEY = "albums"
    const val PICTURE_KEY = "pictures"

    private var ready = false
    private lateinit var root: Folder
    private var mCollections: MutableList<Collection> = ArrayList()
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

    fun setupIfRequired() {
        if (!ready) {
            setup()
            ready = true
        }
    }

    private fun setup() {

        root = Storage.retrievedFolders.firstOrNull() ?: return
        val displayedFolders = findFolderByPath(STORAGE_PATH)?.folders
                ?: root.folders  // uncharted territory

        // defensive
        mCollectionStack.clear()
        mCollections.clear()

        // Add Folders
        mCollections.addAll(displayedFolders)
        val baseStorage = mCollections.find { collection -> collection.path == MAIN_STORAGE_PATH }
        if (baseStorage != null) {  // should be true if displayedFolders initialized as normal
            baseStorage.name = MAIN_STORAGE_NAME
            mCollections.remove(baseStorage)
            mCollections.add(0, baseStorage)
            unwindStack(PRACTICAL_MAIN_STORAGE_PATH)
        }

        // Add Albums
        mCollections.addAll(Storage.retrievedAlbums)

    }

    fun getCollections(): MutableList<Collection> {
        return mCollections
    }

    fun getNestedAlbums(albums: List<Album> = this.albums, runningList: MutableList<Album> = mutableListOf()): MutableList<Album> {
        for (album in albums) {
            runningList.add(album)
            getNestedAlbums(album.albums, runningList)
        }
        return runningList
    }

    /**
     * Lazily create ordered contents map
     */
    fun getContentsMap(): LinkedHashMap<String, List<Coverable>> {

        val currentMap = (currentCollection?.contentsMap ?: mutableMapOf())
                as MutableMap<String, List<Coverable>>

        for (type in mContentsMap.keys) {
            val contentsOfType = currentMap[type] ?: listOf()
            mContentsMap[type] = contentsOfType
        }
        return mContentsMap
    }

    /**
     * @return Relative position of new album amongst other albums
     */
    fun createNewAlbum(name: String, addToCurrent: Boolean = false): Int {
        val newAlbum: Album
        val position: Int
        if (addToCurrent && currentCollection is Album) {  // only albums can have albums
            val currentAlbum = currentCollection as Album
            newAlbum = Album(name, path = "${currentAlbum.path}/$name")
            position = currentAlbum.albums.size
            currentAlbum.addAlbum(newAlbum)
        } else {
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
        } else {
            val remainingCollections: MutableList<Collection> = folders.toMutableList()
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
    fun launch(item: Coverable, position: Int = -1, callingFragment: Fragment? = null, requestCode: Int = -1): Boolean {
        if (!item.terminal) {
            if (item as? Collection != null) {
                currentCollection = item
                return true
            }
        } else {
            if (item as? TerminalCoverable != null) {
                val intent = Intent(callingFragment?.context, item.activity)
                val key = callingFragment?.getString(R.string.intent_item_key)
                intent.putExtra(key, position)
                callingFragment?.startActivityForResult(intent, requestCode)
            }
        }
        return false
    }

    /**
     * Similar behaviour to launch(), but unwinds with some specific configurations to improve QoL.
     */
    fun launchAsShortcut(item: Folder) {
        when (item.name) {
            MAIN_STORAGE_NAME -> unwindStack(PRACTICAL_MAIN_STORAGE_PATH) // skip the empty sub-folders
            else -> unwindStack(item.path)
        }
    }

    fun revertToParent(): List<Coverable>? {
        return if (mCollectionStack.size > 1) {
            mCollectionStack.pop()
            contents
        } else {
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
     * Given a path to a Collection, use it to set a stack state.
     * If can't unwind completely, do it as much as possible, then return.
     */
    fun unwindStack(path: String) {
        val levels = path.split("/")
        var levelIndex = 0
        var collectionsOnLevel = (albums as List<Collection>) + root

        for (level in levels) {
            // Use path to find Collection because those (unlike names) are immutable
            currentCollection = collectionsOnLevel.find { collection -> collection.path.split("/")[levelIndex] == level }
                    ?: return
            collectionsOnLevel = (currentCollection as Collection).getContents().filterIsInstance<Collection>()
            levelIndex += 1
        }
    }

    /**
     * Tries to find a Folder by given path and optional starting ancestor Folder.
     * By default, returns null on failure. onMissing() function can be supplied
     * to handle failure and return something else.
     *
     */
    private fun findFolderByPath(path: String, startFolder: Folder? = null,
                                 onMissing: (Folder, List<String>, Int) -> Folder? = { _, _, _ -> null }): Folder? {

        var currentFolder = startFolder ?: root
        val destinationPath = path.removeSuffix("/")
        val startPath = currentFolder.filePath.removeSuffix("/")
        val levels = destinationPath.split("/")

        // start at children level, not own level
        var index = getDepthOfPath(startPath, destinationPath)
        if (index == 0) return null

        while (currentFolder.folders.isNotEmpty() && index < levels.size) {
            currentFolder = currentFolder.folders.find { child -> child.filePath.split("/")[index] == levels[index] }
                    ?: return onMissing(currentFolder, levels, index)
            index += 1
        }

        return if (currentFolder.filePath.removeSuffix("/") == destinationPath) currentFolder
        else null

    }

    /**
     * Assumes either both OR neither paths have trailing backslash.
     * If not the case, there will be off-by-one error.
     *
     *  Result example: path "files/music" has depth of 2 relative to path "files/music/r.mp3"
     */
    private fun getDepthOfPath(pathToCheck: String, referencePath: String): Int {
        if (!referencePath.startsWith(pathToCheck)) return 0
        val initial = referencePath.split("/").size
        val final = referencePath.removePrefix("$pathToCheck/").split("/").size
        return initial - final
    }

    /**
     * Finds the parent Folder of the FileObject, or makes it (and all required ancestors)
     * with proper linking.
     */
    private fun getParentFolder(fileObject: FileObject): Folder? {
        val pathToParent = fileObject.parentFilePath

        val parent = findFolderByPath(pathToParent) { folder, levels, i ->
            // Folders from levels[i] onwards don't exist, so make them
            var index = i
            var workingFolder = folder
            var path = workingFolder.filePath.removeSuffix("/")

            while (index < levels.size) {

                val name = levels[index]
                path += "/$name"
                val childFolder = Folder(name, path)

                // Link the parent and child
                workingFolder.addFolder(childFolder)
                childFolder.parent = workingFolder

                workingFolder = childFolder
                index += 1
            }
            workingFolder
        }
        fileObject.parent = parent
        return parent
    }

    fun getCurrentCollectionPictures(): List<Picture> {
        val collection = currentCollection
        var pictures: List<Picture> = listOf()
        if (collection != null) {
            pictures = collection.pictures
        }
        return pictures
    }

    /**
     * Save a Picture to disk and update Collections as required.
     */
    fun createPictureFromBitmap(bitmap: Bitmap, name: String, location: String, isNew: Boolean,
                                sdCardFile: DocumentFile?, contentResolver: ContentResolver): File? {

        val file = Storage.saveBitmapToDisk(bitmap, name, location, sdCardFile, contentResolver)
                ?: return null
        val picture: Picture
        val folder: Folder

        // Get Picture from Folder, as it's guaranteed to reside in there if it exists, unlike in Album
        folder = findFolderByPath(location) ?: return file  // should never return
        picture = folder.findPictureByPath(file.path) ?: Picture(name, file.path)

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
                    sdCardFile: DocumentFile?, contentResolver: ContentResolver): Pair<File, File>? {

        val picture = currentCollection?.pictures?.get(position) ?: return null
        val files = Storage.moveFile(picture.filePath, folderFile, sdCardFile, contentResolver)
                ?: return null

        // Remove from old Folder
        val oldFolder = findFolderByPath(picture.fileLocation)  // looks for existing Folder
                ?: return files  // should never return here
        oldFolder.removePicture(picture)

        // Update Picture's path
        picture.filePath = files.second.path

        // Add to new Folder
        val newFolder = getParentFolder(picture)  // looks for Folder or makes it
                ?: return files  // should never return here
        newFolder.addPicture(picture, toFront = true)

        // Save changes
        Storage.saveAlbumsToDisk(albums)

        return files
    }

    fun renamePicture(picture: Picture, newName: String, sdCardFile: DocumentFile?): Pair<File, File>? {
        val files = Storage.renameFile(picture.filePath, newName, sdCardFile)
                ?: return null
        picture.name = newName
        picture.filePath = files.second.path
        Storage.saveAlbumsToDisk(albums)
        return files
    }

    /**
     * Different from movePicture() because a) destination Folder is not updated (since Recycle Bin
     * is not a Folder), and b) new moved File doesn't need post-processing
     * @return The recycled file, or null if failed
     */
    private fun movePictureToRecycleBin(picture: Picture, sdCardFile: DocumentFile?): File? {
        // Pair<Original, New>
        val files = Storage.moveFileToRecycleBin(picture.filePath, sdCardFile)
                ?: return null
        findFolderByPath(picture.fileLocation)?.removePicture(picture)
        return files.first
    }

    fun movePicturesToRecycleBin(pictures: List<Picture>, sdCardFile: DocumentFile?,
                                 afterEachMoved: (File) -> Unit): Int {
        var failCounter = 0
        for (picture in pictures) {
            val originalFile = movePictureToRecycleBin(picture, sdCardFile)
            if (originalFile == null) failCounter += 1
            else afterEachMoved(originalFile)
        }

        if (failCounter != pictures.size) {
            cleanAlbums()
            Storage.saveAlbumsToDisk(albums)
            Storage.recycleBin.saveLocationsToDisk()
        }

        return failCounter
    }

    private fun restorePictureFromRecycleBin(picture: Picture, sdCardFile: DocumentFile?,
                                             contentResolver: ContentResolver): File? {

        // Pair<RecycleBin File, Original File>
        val files = Storage.restoreFileFromRecycleBin(picture.filePath, sdCardFile, contentResolver)
                ?: return null
        picture.filePath = files.second.path
        getParentFolder(picture)?.addPicture(picture, toFront = true)
        return files.second
    }

    fun restorePicturesFromRecycleBin(pictures: List<Picture>, sdCardFile: DocumentFile?, contentResolver: ContentResolver,
                                      afterEachRestored: (File) -> Unit): Int {
        var failCounter = 0
        for (picture in pictures) {
            val restoredFile = restorePictureFromRecycleBin(picture, sdCardFile, contentResolver)
            if (restoredFile == null) failCounter += 1
            else afterEachRestored(restoredFile)
        }

        if (failCounter != pictures.size) Storage.recycleBin.saveLocationsToDisk()
        return failCounter
    }

    fun deletePictures(pictures: List<Picture>): Int {
        var failCounter = 0
        for (picture in pictures) {
            if (!Storage.deleteFileFromRecycleBin(picture.filePath, null)) failCounter += 1
        }

        if (failCounter != pictures.size) Storage.recycleBin.saveLocationsToDisk()
        return failCounter
    }

    /**
     * Remove all Pictures that don't exist on device from all albums.
     */
    private fun cleanAlbums(start: Album? = null) {
        val toCheck: List<Album>
        if (start != null) {
            Storage.cleanAlbum(start)
            toCheck = start.albums
        } else {
            toCheck = albums
        }
        for (album in toCheck) {
            cleanAlbums(album)
        }
    }

}