package iced.egret.palette.util

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import iced.egret.palette.R
import iced.egret.palette.activity.inherited.BaseActivity
import iced.egret.palette.model.Album
import iced.egret.palette.model.Folder
import iced.egret.palette.model.Picture
import iced.egret.palette.model.inherited.Collection
import iced.egret.palette.model.inherited.Coverable
import iced.egret.palette.model.inherited.FileObject
import iced.egret.palette.model.inherited.TerminalCoverable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.*
import kotlin.collections.ArrayList
import kotlin.coroutines.CoroutineContext

/**
 * Responsible for handling the Collection Stack needed for in-app navigation,
 * the state/properties of Collections and their contents, and acts as a mediator between
 * Activities and Storage. Often delegates work to Storage, so is mildly coupled to it.
 */
object CollectionManager : CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO

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
    private val contents: List<Coverable>
        get() = currentCollection?.getContents() ?: listOf()

    private val mPictureCache = mutableMapOf<String, Picture>()
    private val mBufferPictures = mutableListOf<Picture>()

    /**
     * Ready = Don't need to remake collections.
     */
    internal fun setup(path: String?) {
        if (!ready) {
            buildPictures(path)
            mCollections.addAll(Storage.buildAlbumsFromDisk())
            bindCustomCovers(Storage.customCovers)
            ready = true
        } else if (path != null) {
            clearStack()
            unwindStack(path)
        }
    }

    internal fun reset(path: String?) {
        mPictureCache.clear()
        buildPictures(path)
        mCollections.addAll(Storage.buildAlbumsFromDisk())
        bindCustomCovers(Storage.customCovers)

        if (path != null) {
            clearStack()
            unwindStack(path)
        }
        ready = true
    }

    private fun buildPictures(path: String?) {

        mPictureCache.putAll(Storage.knownPictures)
        root = Storage.initialFolders.firstOrNull() ?: return
        val displayedFolders = findFolderByPath(STORAGE_PATH)?.folders
                ?: root.folders  // uncharted territory

        mCollectionStack.clear()
        mCollections.clear()
        mBufferPictures.clear()

        // Add Folders.
        // Also do some hacks to make /storage/emulated/0 a pinned collection
        // instead of /storage/emulated.
        mCollections.addAll(displayedFolders)
        val baseStorage = mCollections.find { collection -> collection.path == MAIN_STORAGE_PATH } as? Folder
        if (baseStorage != null) {  // should be true if displayedFolders initialized as normal
            mCollections.remove(baseStorage)
            if (baseStorage.folders.size == 1) baseStorage.folders[0].name = MAIN_STORAGE_NAME
            mCollections.addAll(0, baseStorage.folders)
            unwindStack(path ?: PRACTICAL_MAIN_STORAGE_PATH)
        }

        mBufferPictures.addAll(Storage.initialBufferPictures)

    }

    private fun bindCustomCovers(customCovers: Map<String, String>) {
        val coversToSave = customCovers.toMutableMap<String, String?>()
        var misses = 0
        for ((collectionPath, picturePath) in customCovers) {
            val picture = mPictureCache[picturePath]
            if (picture == null) {
                coversToSave[collectionPath] = null
                misses += 1
                continue
            }
            // Find Album or Folder and set cover
            (getNestedAlbums().find { album -> album.path == collectionPath }
                    ?: findFolderByPath(collectionPath))?.addCustomCover(picture)
        }
        if (misses > 0) Storage.setCustomCovers(coversToSave.toList())
    }

    /**
     * Gets fresh data from disk, updates state, saves it to disk,
     * and does UI callback.
     */
    fun fetchNewMedia(context: Context, callback: () -> Unit) {
        launch {
            // On IO thread
            val updateKit = Storage.getUpdateKit(context)
            updatePicturesFromKit(updateKit)
            cleanCollections()
            Storage.saveAlbumsToDisk(albums)
            Storage.saveBufferPicturesToDisk(mBufferPictures)
            writeCache()

            // On UI thread
            withContext(Dispatchers.Main) { callback() }
        }
    }

    /**
     * The if clauses of the for loops will fail if a Picture was moved/deleted/added due to
     * in-app operations. These changes are properly reflected on disk, but Storage isn't alerted
     * about them, so UpdateKit reports them as added/deleted externally.
     * Extra overhead to make Storage keep track of those things is probably not worth it.
     *
     * @return If something updated (true) or not (false)
     */
    private fun updatePicturesFromKit(updateKit: Storage.UpdateKit) {

        val addedPictures = updateKit.toAdd.filterIsInstance<Picture>()
        val removedPaths = updateKit.toRemove
        val toPrependToBuffer = mutableListOf<Picture>()

        for (picture in addedPictures) {
            val folder = getParentFolder(picture)
            // Don't add if Picture already exists
            if (folder != null && folder.findPictureByPath(picture.filePath) == null) {
                folder.addPicture(picture, toFront = true)
                toPrependToBuffer.add(picture)  // keep order
                mPictureCache[picture.filePath] = picture
            }
        }
        mBufferPictures.addAll(0, toPrependToBuffer)

        for (path in removedPaths) {
            val parentPath = path.split("/").dropLast(1).joinToString("/")
            val folder = findFolderByPath(parentPath)
            val picture = folder?.findPictureByPath(path)

            if (folder != null && picture != null) {
                // Doesn't enter here if Picture previously removed by in-app operations
                folder.removePicture(picture)
                mBufferPictures.remove(picture)
                mPictureCache.remove(picture.filePath)
            }
        }
    }

    private fun writeCache() {
        Storage.savePictureCacheToDisk(mPictureCache)
    }

    fun getBufferPictures(): List<Picture> {
        return mBufferPictures.toList()
    }

    fun removeFromBufferPictures(pictures: List<Picture>) {
        mBufferPictures.removeAll(pictures)
        Storage.saveBufferPicturesToDisk(mBufferPictures)
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
            newAlbum = Album(name, path = "${currentAlbum.path}/$name", parent = currentAlbum)
            position = currentAlbum.albums.size
            currentAlbum.addAlbum(newAlbum)
        } else {
            newAlbum = Album(name, path = name)
            position = mCollections.size
            mCollections.add(newAlbum)
        }
        sortAllAlbums()
        Storage.saveAlbumsToDisk(albums)
        return position
    }

    fun renameCollection(collection: Collection, newName: String) {
        collection.rename(newName)
        sortAllAlbums()
        Storage.saveAlbumsToDisk(albums)
    }

    fun applySyncedFolders(filePaths: kotlin.collections.Collection<String>, toCurrent: Boolean, album: Album? = null) {
        if (album != null && !toCurrent) {
            val folderFiles = filePaths.map { path -> File(path) }
            album.syncedFolderFiles.clear()
            album.syncedFolderFiles.addAll(folderFiles)
            Storage.saveAlbumsToDisk(albums)
        } else if (toCurrent) {
            val currentAlbum = currentCollection as? Album ?: return
            val folderFiles = filePaths.map { path -> File(path) }
            currentAlbum.syncedFolderFiles.clear()
            currentAlbum.syncedFolderFiles.addAll(folderFiles)
            Storage.saveAlbumsToDisk(albums)
        }
    }

    fun deleteAlbums(albums: List<Album>, fromCurrent: Boolean) {
        if (fromCurrent) {
            val currentAlbum = (currentCollection as? Album) ?: return  // should always succeed
            currentAlbum.removeAlbums(albums)
        } else mCollections.removeAll(albums)
        Storage.saveAlbumsToDisk(this.albums)
    }

    // FIXME: very inefficient, you don't need to sort everything when only one album changes
    private fun sortAllAlbums() {
        val albums = this.albums
        mCollections.removeAll(albums)
        mCollections.addAll(albums.sortedBy { album -> album.name })
        for (album in albums) {
            album.sortSelf(albums = true)
        }
    }

    /**
     * Adds given contents to all given albums. Does not add if already exists in collection.
     */
    fun addContentToAllAlbums(contents: List<Coverable>, albums: List<Album>) {
        if (contents.isEmpty()) return

        for (album in albums) {
            loop@ for (content in contents) {
                when (content) {
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
    fun launch(item: Coverable, lp: PagerLaunchPack? = null): Boolean {
        when {
            item !is TerminalCoverable -> if (item as? Collection != null) {
                currentCollection = item
                return true
            }
            lp != null -> {
                if (lp.callingFragment != null) {
                    val intent = Intent(lp.callingFragment.context, lp.newActivityClass)
                    val key = lp.callingFragment.getString(R.string.intent_item_key)
                    intent.putExtra(key, lp.position)
                    lp.callingFragment.startActivityForResult(intent, lp.requestCode)
                } else if (lp.callingActivity != null) {
                    val intent = Intent(lp.callingActivity, lp.newActivityClass)
                    val key = lp.callingActivity.getString(R.string.intent_item_key)
                    intent.putExtra(key, lp.position)
                    lp.callingActivity.startActivityForResult(intent, lp.requestCode)
                }
            }
        }
        return false
    }

    /**
     * Similar behaviour to launch(), but unwinds to improve QoL.
     */
    fun launchAsShortcut(item: Folder) {
        clearStack()
        unwindStack(item.path)
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
    private fun unwindStack(path: String) {
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
    fun findFolderByPath(path: String, startFolder: Folder? = null,
                         onMissing: (Folder, List<String>, Int) -> Folder? = { _, _, _ -> null }): Folder? {

        var currentFolder = startFolder ?: root
        val destinationPath = path.removeSuffix("/")
        val startPath = currentFolder.filePath.removeSuffix("/")
        val levels = destinationPath.split("/")

        // start at children level, not own level
        var index = getDepthOfPath(startPath, destinationPath)
        if (index == 0) return null

        while (index < levels.size) {
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
     *
     * Returning null should not happen (if it does, it's programmer error), and is in place
     * (instead of forcing non-nullity) to keep app running in case it does.
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

                workingFolder = childFolder
                index += 1
            }
            workingFolder
        }
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
     *
     * @param isNew if false, the new bitmap is replacing an existing file
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
        if (!isNew) folder.removePicture(picture)  // if replacing Picture
        folder.addPicture(picture, toFront = true)

        // Update current Collection if it's an Album and picture actually belongs to it (i.e. not synced),
        // otherwise another update would be redundant
        val album = currentCollection as? Album
        if (album is Album && album.ownsPictures(listOf(picture))) {
            if (!isNew) currentCollection?.removePicture(picture)  // if replacing Picture
            currentCollection?.addPicture(picture, toFront = true)
            Storage.saveAlbumsToDisk(albums)
        }

        mPictureCache[file.path] = picture
        writeCache()
        return file
    }

    /**
     * Moves picture on disk and updates cache.
     * @return The old and new Files, or null if move fails.
     */
    private fun movePicture(picture: Picture, folderFile: File, sdCardFile: DocumentFile?,
                            contentResolver: ContentResolver): Pair<File, File>? {

        val files = Storage.moveFile(picture.filePath, folderFile, sdCardFile, contentResolver)
                ?: return null

        // Update cache
        mPictureCache.remove(picture.filePath)
        mPictureCache[files.second.path] = picture

        picture.filePath = files.second.path
        return files
    }

    /**
     * Handles pictures with different old locations but same new location.
     * Can't cache newFolder because it may be deleted when "moving" a picture to the same
     * location (if that picture was the only one in the folder).
     */
    fun movePictures(pictures: List<Picture>, folderFile: File, sdCardFile: DocumentFile?,
                     contentResolver: ContentResolver, afterEachMoved: (File, File) -> Unit): Int {

        var failCount = 0
        var i = 0
        var oldFolder: Folder
        var newFolder: Folder

        for (picture in pictures) {

            // Look for existing folder
            oldFolder = findFolderByPath(picture.parentFilePath)
                    // something terrible happened, abort
                    ?: return failCount + pictures.size - i  // remaining items fail

            // Move file and update Picture properties
            val files = movePicture(picture, folderFile, sdCardFile, contentResolver)

            if (files == null) failCount += 1
            else {
                // Broadcast changes ASAP
                afterEachMoved(files.first, files.second)

                oldFolder.removePicture(picture)

                // Find new folder or make it
                newFolder = getParentFolder(picture)
                        // something terrible happened, abort
                        ?: return failCount + pictures.size - i  // remaining items fail

                newFolder.addPicture(picture, toFront = true)
            }
            i += 1
        }

        // Save changes
        Storage.saveAlbumsToDisk(albums)
        writeCache()

        return failCount
    }

    fun renamePicture(picture: Picture, newName: String, sdCardFile: DocumentFile?): Pair<File, File>? {
        val files = Storage.renameFile(picture.filePath, newName, sdCardFile)
                ?: return null

        // Update cache
        mPictureCache.remove(picture.filePath)
        mPictureCache[files.second.path] = picture

        picture.name = newName
        picture.filePath = files.second.path

        Storage.saveAlbumsToDisk(albums)
        writeCache()
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

        mPictureCache.remove(picture.filePath)
        mBufferPictures.removeAll { pic -> pic.filePath == files.first.path }  // remove if in buffer
        findFolderByPath(picture.parentFilePath)?.removePicture(picture)
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
            Storage.saveBufferPicturesToDisk(mBufferPictures)
            Storage.recycleBin.saveLocationsToDisk()
            writeCache()
        }

        return failCounter
    }

    private fun restorePictureFromRecycleBin(picture: Picture, sdCardFile: DocumentFile?,
                                             contentResolver: ContentResolver): File? {

        // Pair<RecycleBin File, Original File>
        val files = Storage.restoreFileFromRecycleBin(picture.filePath, sdCardFile, contentResolver)
                ?: return null

        mPictureCache[files.second.path] = picture
        mBufferPictures.add(0, picture)  // always add to buffer
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

        if (failCounter != pictures.size) {
            Storage.recycleBin.saveLocationsToDisk()
            writeCache()
        }
        return failCounter
    }

    fun deletePictures(pictures: List<Picture>): Int {
        var failCounter = 0
        for (picture in pictures) {
            if (!Storage.deleteFileFromRecycleBin(picture.filePath, null)) failCounter += 1
        }

        if (failCounter != pictures.size) {
            Storage.recycleBin.saveLocationsToDisk()
            writeCache()
        }
        return failCounter
    }

    /**
     * Remove all Pictures that don't exist on device from all collections.
     */
    fun cleanCollections() {
        cleanFolders()
        cleanAlbums()
    }

    private fun cleanAlbums(start: Album? = null) {
        val toCheck: List<Album>
        if (start != null) {
            Storage.cleanCollection(start)
            toCheck = start.albums
        } else {
            toCheck = albums
        }
        for (album in toCheck) {
            cleanAlbums(album)
        }
    }

    private fun cleanFolders(start: Folder? = null) {
        val toCheck: List<Folder>
        if (start != null) {
            Storage.cleanCollection(start)
            toCheck = start.folders
        } else {
            toCheck = folders
        }
        for (folder in toCheck) {
            cleanFolders(folder)
        }
    }

    data class PagerLaunchPack(val position: Int,
                               val callingActivity: BaseActivity? = null,
                               val callingFragment: Fragment? = null,
                               val newActivityClass: Class<out BaseActivity>?,
                               val requestCode: Int = -1)
}