package iced.egret.palette.util

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.MediaStore.MediaColumns
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.annotation.RequiresApi
import androidx.documentfile.provider.DocumentFile
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import iced.egret.palette.model.Album
import iced.egret.palette.model.Folder
import iced.egret.palette.model.Picture
import iced.egret.palette.model.dataclass.AlbumData
import iced.egret.palette.model.inherited.FileObject
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

/**
 * Handles disk-related operations, like saving/loading images, hosting the Recycle Bin,
 * and moving files.
 */
object Storage {

    private var contentBuilder = ContentBuilder()
    private var gson = Gson()

    val initialFolders: List<Folder>
        get() = contentBuilder.folders
    val initialAlbums: List<Album>
        get() = contentBuilder.albums
    val initialBufferPictures : List<Picture>
        get() = contentBuilder.bufferPictures

    val knownPictures = linkedMapOf<String, Picture>()
    lateinit var recycleBin: RecycleBin
        private set

    private var ready = false
    private const val pictureCacheFileName = "pictures-cache.json"
    private const val pictureBufferFileName = "pictures-buffer.json"
    private const val albumsFileName = "albums.json"
    private lateinit var fileDirectory: File

    internal fun setupIfRequired(context: Context) {
        if (!ready) {
            setup(context)
            ready = true
        }
    }

    private fun setup(context: Context) {
        fileDirectory = context.filesDir
        contentBuilder.runForPictures(context)
        knownPictures.putAll(contentBuilder.pictures)
        recycleBin = RecycleBin(fileDirectory)
        recycleBin.loadLocationsFromDisk()
    }

    /**
     * Require Folders and Pictures to be in place
     */
    internal fun buildAlbumsFromDisk() : List<Album> {
        contentBuilder.runForAlbums()
        return contentBuilder.albums
    }

    internal fun getUpdateKit(context: Context): UpdateKit {

        val foundPicturePaths = mutableListOf<String>()
        val existingPicturePaths = contentBuilder.pictures.keys

        val uri: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val projection = arrayOf(MediaColumns.DATA)
        val sortBy = MediaStore.Images.ImageColumns.DATE_MODIFIED + " DESC"
        val cursor = context.contentResolver.query(uri, projection, null, null, sortBy)
        val columnIndexData = cursor!!.getColumnIndexOrThrow(MediaColumns.DATA)

        while (cursor.moveToNext()) {
            foundPicturePaths.add(cursor.getString(columnIndexData))
        }
        cursor.close()

        // addedPaths is a list to preserve order.
        // In general, collection difference works the same as set difference,
        // except duplicates are removed as well: not an issue as all paths are unique.
        val addedPaths = foundPicturePaths - existingPicturePaths
        val removedPaths = existingPicturePaths - foundPicturePaths

        val addedPictures = mutableListOf<Picture>()
        for (path in addedPaths) {
            val name = path.split("/").last()
            val picture = Picture(name, path)
            addedPictures.add(picture)
            knownPictures[path] = picture
        }
        for (path in removedPaths) knownPictures.remove(path)

        return UpdateKit(addedPictures, removedPaths)
    }

    internal fun saveAlbumsToDisk(albums: List<Album>) {
        val albumsData = ArrayList<AlbumData>()
        for (album in albums) {
            albumsData.add(album.toDataClass())
        }
        val json = gson.toJson(albumsData)
        saveJsonToDisk(json, albumsFileName)
    }

    internal fun savePictureCacheToDisk(cache: Map<String, Picture>) {
        val json = gson.toJson(cache.keys)
        saveJsonToDisk(json, pictureCacheFileName)
    }

    internal fun saveBufferPicturesToDisk(pictures: List<Picture>) {
        val json = gson.toJson(pictures.map {picture -> picture.filePath })
        saveJsonToDisk(json, pictureBufferFileName)
    }

    private fun saveJsonToDisk(json: String, fileName: String) {
        try {
            File(fileDirectory, fileName).writeText(json)
        } catch (e: FileNotFoundException) {
            File(fileDirectory, fileName).createNewFile()
            File(fileDirectory, fileName).writeText(json)
        }
    }

    private fun readJsonFromDisk(fileName: String): String? {
        return try {
            File(fileDirectory, fileName).bufferedReader().readText()
        } catch (e: FileNotFoundException) {
            Log.i("palette/storage", "couldn't find $fileName when trying to read in")
            null
        }
    }

    /**
     * Save bitmap to specified location with specified name.
     * Created date and orientation are also set if image is JPG;
     * this metadata editing process differs based on API version.
     *
     * https://stackoverflow.com/a/46755279
     *
     * @param location Has trailing file separator (i.e. "/")
     */
    internal fun saveBitmapToDisk(bitmap: Bitmap, name: String, location: String,
                                  sdCardFile: DocumentFile?, contentResolver: ContentResolver): File? {

        val extension = name.split(".").last().toLowerCase()
        val file: File

        val compressionFormat = when (extension) {
            "png" -> Bitmap.CompressFormat.PNG
            "webp" -> Bitmap.CompressFormat.WEBP
            else -> Bitmap.CompressFormat.JPEG
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {

            // If JPG and API < 24: need to write to temp file, then move it
            if (compressionFormat == Bitmap.CompressFormat.JPEG) {
                val tempFile = saveRawBitmap(bitmap, name, compressionFormat)
                setJpegMetadata(tempFile)
                val files = moveFile(tempFile.path, File(location), sdCardFile, contentResolver)
                        ?: return null
                file = files.second  // new file
            }
            // If not JPG and API < 24: no metadata writing, so save normally
            else {
                saveRawBitmapToLocation(bitmap, name, compressionFormat, location, sdCardFile, contentResolver)
                        ?: return null
                file = File(location + name)
            }

        }
        // Else, API >= 24: save bitmap and set metadata freely as required
        else {

            val fileDescriptor =
                    saveRawBitmapToLocation(bitmap, name, compressionFormat, location, sdCardFile, contentResolver)
                            ?: return null
            if (compressionFormat == Bitmap.CompressFormat.JPEG) setJpegMetadata(fileDescriptor)
            file = File(location + name)

        }

        return file
    }

    /**
     * Save bitmap without metadata to app storage.
     */
    private fun saveRawBitmap(bitmap: Bitmap, name: String, compressionFormat: Bitmap.CompressFormat): File {
        val file = File(fileDirectory.path.removeSuffix("/") + "/temp/$name")
        file.parentFile.mkdirs()

        // Save as max quality
        val outputStream = FileOutputStream(file)
        bitmap.compress(compressionFormat, 100, outputStream)
        outputStream.close()

        return file
    }

    /**
     * Save bitmap without metadata to specified location.
     */
    private fun saveRawBitmapToLocation(bitmap: Bitmap, name: String, compressionFormat: Bitmap.CompressFormat,
                                        location: String, sdCardFile: DocumentFile?,
                                        contentResolver: ContentResolver): FileDescriptor? {

        val path = location + name
        val file = File(path)
        var documentFile: DocumentFile? = null
        val locationOnSdCard = pathOnSdCard(location)
        val outStream: OutputStream

        // If saving to SD card and can do it, do it. Otherwise try to save normally.
        // https://stackoverflow.com/a/43317703
        if (locationOnSdCard != null && sdCardFile != null) {
            documentFile = getImageDocumentFile(name, locationOnSdCard, "image/webp", sdCardFile)
                    ?: return null
            outStream = contentResolver.openOutputStream(documentFile.uri) ?: return null
        } else {
            try {
                outStream = FileOutputStream(file)
            } catch (accessDenied: FileNotFoundException) {
                return null
            }
        }

        // Save as max quality
        bitmap.compress(compressionFormat, 100, outStream)
        outStream.close()

        // Get ParcelFileDescriptor
        val parcelFd = if (documentFile == null) {
            contentResolver.openFileDescriptor(Uri.fromFile(file), "rw") ?: return null
        } else contentResolver.openFileDescriptor(documentFile.uri, "rw") ?: return null

        return parcelFd.fileDescriptor

    }

    /**
     * Assumes file with new name doesn't already exist.
     */
    internal fun renameFile(path: String, newName: String, sdCardFile: DocumentFile?): Pair<File, File>? {

        val original = File(path)
        val target = File(original.parent + "/" + newName)
        val pathOnSdCard = pathOnSdCard(original.path)

        val success = if (pathOnSdCard != null && sdCardFile != null) {
            val documentFile = findFileInsideRecursive(sdCardFile, pathOnSdCard)
                    ?: return null
            documentFile.renameTo(newName)
        } else {
            original.renameTo(target)
        }

        return if (success) Pair(original, target) else null
    }

    /**
     * Try to copy to destination, then delete original. Abort if copy fails.
     * Since moving is technically copy + delete, moving to the same file will delete the file,
     * so don't do it: just signal success.
     * @return Old and new file if successfully moved, null otherwise
     */
    internal fun moveFile(sourcePath: String, destinationParent: File, sdCardFile: DocumentFile?,
                          contentResolver: ContentResolver?, newName: String? = null): Pair<File, File>? {

        val sourceFile = File(sourcePath)
        if (sourceFile.parentFile == destinationParent) return Pair(sourceFile, sourceFile)

        val newFile = copyFile(sourceFile, destinationParent, sdCardFile, contentResolver, newName)
                ?: return null

        val success = deleteFile(sourceFile, sdCardFile)
        if (!success) {
            // Undo copy; assumption is that if you could copy to destination,
            // you can delete from it as well.
            deleteFile(newFile, sdCardFile)
            return null
        }

        return Pair(sourceFile, newFile)
    }

    /**
     * If copying to SD card (and can access SD card), do so using DocumentFiles.
     * Otherwise, try to copy as normal.
     *
     * @return The copy of the File, or null if failed.
     */
    internal fun copyFile(sourceFile: File, destinationParent: File, sdCardFile: DocumentFile?,
                          contentResolver: ContentResolver?, newName: String? = null): File? {

        val name = newName ?: sourceFile.name
        val destinationLocation = destinationParent.path.removeSuffix("/") + "/"
        val destinationOnSdCard = pathOnSdCard(destinationLocation)
        val inStream = FileInputStream(sourceFile)
        val outStream: OutputStream
        val copyAsFile = File(destinationLocation + name)
        val copyAsDocumentFile: DocumentFile

        if (destinationOnSdCard != null && sdCardFile != null && contentResolver != null) {
            copyAsDocumentFile = getImageDocumentFile(name, destinationOnSdCard, "image/webp", sdCardFile)
                    ?: return null
            outStream = contentResolver.openOutputStream(copyAsDocumentFile.uri) ?: return null
        } else {
            try {
                outStream = FileOutputStream(copyAsFile)
            } catch (accessDenied: FileNotFoundException) {
                return null
            }
        }

        copyStreams(inStream, outStream)
        return copyAsFile
    }

    /**
     * If file is on SD card, and have access to SD card, use DocumentFiles to delete.
     * Otherwise, try to delete as normal.
     *
     * @return True if deleted, false if not.
     */
    private fun deleteFile(sourceFile: File, sdCardFile: DocumentFile?): Boolean {
        val pathOnSdCard = pathOnSdCard(sourceFile.path)
        if (pathOnSdCard != null && sdCardFile != null) {
            val sourceDocumentFile = findFileInsideRecursive(sdCardFile, pathOnSdCard)
                    ?: return false
            return sourceDocumentFile.delete()
        } else {
            return sourceFile.delete()
        }
    }

    /**
     * Try to move to recycle bin. Abort if move fails.
     * @return Old and new file if successfully moved, null otherwise
     */
    internal fun moveFileToRecycleBin(sourcePath: String, sdCardFile: DocumentFile?): Pair<File, File>? {
        val newName = getUniqueNameInRecycleBin(sourcePath.split("/").last())
        // Pair<Original, New>
        val files = moveFile(sourcePath, recycleBin.file, sdCardFile, null, newName)
                ?: return null
        recycleBin.oldLocations[newName] = files.first.path
        return files
    }

    internal fun restoreFileFromRecycleBin(pathInBin: String, sdCardFile: DocumentFile?,
                                           contentResolver: ContentResolver): Pair<File, File>? {

        val nameInBin = pathInBin.split("/").last()
        val oldPath = recycleBin.oldLocations[nameInBin] ?: return null

        // Get old name and location's File
        val temp = oldPath.split("/")
        val oldName = temp.last()
        val oldLocation = temp.dropLast(1).joinToString("/")
        val destination = File(oldLocation)

        // Pair<RecycleBin File, Original File>
        val files = moveFile(pathInBin, destination, sdCardFile, contentResolver, oldName)
                ?: return null

        recycleBin.oldLocations.remove(nameInBin)
        return files
    }

    /**
     * @return True if deleted, false if not.
     */
    internal fun deleteFileFromRecycleBin(path: String, sdCardFile: DocumentFile?): Boolean {
        val file = File(path)
        val success = deleteFile(file, sdCardFile)
        recycleBin.oldLocations.remove(file.name)
        return success
    }

    /**
     * @param name The filename (e.g. myImage.png) or whole path (e.g. /path/to/myImage.png)
     * @param location The folder the file sits in (e.g. /path/to)
     */
    fun fileExists(name: String, location: String = ""): Boolean {
        val file = File(location.removeSuffix("/") + "/" + name)
        return file.exists()
    }

    /**
     * Make unique name in case two files with same name in recycle bin.
     */
    private fun getUniqueNameInRecycleBin(originalName: String): String {
        var name = originalName
        var counter = 1
        while (fileExists(name, recycleBin.file.path)) {
            name = "$originalName-$counter"
            counter += 1
        }
        return name
    }

    private fun copyStreams(inputStream: InputStream, outputStream: OutputStream) {
        inputStream.copyTo(outputStream)
        inputStream.close()
        outputStream.close()
    }

    /**
     * Set JPG metadata for specified File.
     *
     * @param tags Attribute name and value pairs; if null, default values are set.
     */
    private fun setJpegMetadata(file: File, tags: Map<String, String>? = null) {
        setAttributes(ExifInterface(file.path), tags)
    }

    /**
     * Set JPG metadata for specified FileDescriptor, which can be stand-in for a DocumentFile.
     * Uses non-support version of ExifInterface because support ver. doesn't have FileDescriptor
     * constructor. Need to use FileDescriptor to edit SD card files.
     *
     * @param tags Attribute name and value pairs; if null, default values are set.
     */
    @RequiresApi(Build.VERSION_CODES.N)
    private fun setJpegMetadata(fileDescriptor: FileDescriptor, tags: Map<String, String>? = null) {
        setAttributes(ExifInterface(fileDescriptor), tags)
    }

    private fun setAttributes(exifInterface: ExifInterface, tags: Map<String, String>?) {

        if (tags == null) {
            val dtString = SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.getDefault())
                    .format(Calendar.getInstance().time)

            exifInterface.setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL.toString())
            exifInterface.setAttribute(ExifInterface.TAG_DATETIME, dtString)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                exifInterface.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL, dtString)
            }

        } else {
            for (tag in tags) {
                try {
                    exifInterface.setAttribute(tag.key, tag.value)
                } catch (e: Exception) {
                    // If attribute doesn't exist or API version not high enough
                    e.printStackTrace()
                }
            }
        }

        exifInterface.saveAttributes()
    }

    private fun isPathOnSdCard(path: String): Boolean {
        return pathOnSdCard(path) != null
    }

    private fun pathOnSdCard(path: String): String? {
        // At start of string, match "/storage/" + any 4 alphanumerics + a dash + any 4 alphanumerics + "/"
        val regex = Regex("^/storage/[a-zA-z0-9]{4}-[a-zA-z0-9]{4}/")
        val sdCardLocation = regex.find(path)?.value ?: return null
        return path.removePrefix(sdCardLocation).removeSuffix("/")
    }

    private fun getImageDocumentFile(name: String, locationOnSdCard: String, defaultMimeType: String,
                                     sdCardFile: DocumentFile): DocumentFile? {
        val extension = name.split(".").last().toLowerCase()
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
                ?: defaultMimeType
        val subFolderFile = findFileInsideRecursive(sdCardFile, locationOnSdCard) ?: return null
        return getChildFile(subFolderFile, mimeType, name)
    }

    /**
     * Get the DocumentFile for some folder/file somewhere inside given root DocumentFile.
     *
     * @return The desired DocumentFile, or null if not found.
     */
    private fun findFileInsideRecursive(rootFile: DocumentFile, relativePath: String): DocumentFile? {

        // splitting empty string will still produce list with one element
        if (relativePath.isEmpty()) {
            return rootFile
        }

        val levels = relativePath.split("/")
        var currentFile = rootFile
        for (level in levels) {
            currentFile = currentFile.findFile(level) ?: return null
        }
        return currentFile
    }

    /**
     * Find or make the DocumentFile with given name inside given DocumentFile.
     *
     * @return The desired DocumentFile, or null if it doesn't exist and couldn't be created.
     */
    private fun getChildFile(subFolderFile: DocumentFile, mimeType: String, name: String): DocumentFile? {
        return subFolderFile.findFile(name) ?: subFolderFile.createFile(mimeType, name)
    }

    /**
     * Ensure all Pictures in album exist on disk. If they don't, remove them.
     */
    internal fun cleanAlbum(album: Album) {
        val pictures = album.pictures.toList()  // a copy to avoid concurrency error
        for (picture in pictures) {
            if (!fileExists(picture.filePath)) {
                album.removePicture(picture)
            }
        }
    }

    /**
     * Gets all albums/folders/media from disk ONCE, ideally when app is created.
     */
    private class ContentBuilder {
        private var picturesRan = false
        private var albumsRan = false

        // Purposely set as lateinit to prevent usage before run functions is called
        lateinit var folders: List<Folder>
        lateinit var albums: List<Album>
        lateinit var pictures : LinkedHashMap<String, Picture>
        lateinit var bufferPictures: List<Picture>

        fun runForPictures(context: Context) {
            if (!picturesRan) {
                picturesRan = true
                pictures = linkedMapOf()
                folders = getPictureFoldersMediaStore(context)
                bufferPictures = getAllBufferPictures()
            }
        }

        fun runForAlbums() {
            if (!albumsRan) {
                albumsRan = true
                albums = getAlbumsFromDisk()
            }
        }

        /**
         * Gets images from Android's default gallery API
         * API access stuff from https://stackoverflow.com/a/36815451
         */
        private fun getPictureFoldersMediaStore(context: Context): List<Folder> {

            val uri: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            val cursor: Cursor?
            val columnIndexData: Int
            val projection = arrayOf(MediaColumns.DATA)
            val sortBy = MediaStore.Images.ImageColumns.DATE_MODIFIED + " DESC"
            val rootFolders = ArrayList<Folder>()

            cursor = context.contentResolver.query(uri, projection, null, null, sortBy)
            columnIndexData = cursor!!.getColumnIndexOrThrow(MediaColumns.DATA)

            val rootLevelIndex = 0
            var absolutePathOfImage: String
            var pathLevels: List<String>
            var rootPath: String
            var parentFolder: Folder?
            var childFolder: Folder?
            var folderPath: String

            while (cursor.moveToNext()) {

                // get image path
                absolutePathOfImage = cursor.getString(columnIndexData)
                pathLevels = absolutePathOfImage.split("/")
                rootPath = pathLevels[rootLevelIndex]

                // check if root already created (in most cases, yes)
                parentFolder = rootFolders.find { root -> root.filePath == rootPath }
                if (parentFolder == null) {
                    parentFolder = Folder(rootPath, rootPath)
                    rootFolders.add(parentFolder)
                }

                // add additional folders as required, in linked-list-like fashion
                var levelInt = rootLevelIndex + 1
                for (level in pathLevels.subList(rootLevelIndex + 1, pathLevels.size - 1)) {

                    folderPath = pathLevels.subList(0, levelInt + 1).joinToString("/")

                    childFolder = parentFolder!!.folders.find { folder -> folder.filePath == folderPath }
                    if (childFolder == null) {
                        childFolder = Folder(level, folderPath, parent = parentFolder)
                        parentFolder.addFolder(childFolder)
                    }
                    parentFolder = childFolder

                    levelInt++

                }

                // Ensure file exists on disk.
                // MediaStore will return non-existent images if removal was not broadcasted correctly.
                val file = File(absolutePathOfImage)
                if (!file.exists()) {
                    val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
                    intent.data = Uri.fromFile(file)
                    context.sendBroadcast(intent)
                }

                // add picture, parentFolder guaranteed to exist
                val picture = Picture(pathLevels[pathLevels.size - 1], absolutePathOfImage)
                parentFolder!!.addPicture(picture)
                pictures[absolutePathOfImage] = picture

            }

            cursor.close()
            return rootFolders

        }

        /**
         * Uses initially retrieved Pictures to rebuild Albums from JSON.
         */
        private fun getAlbumsFromDisk(): List<Album> {
            val json = readJsonFromDisk(albumsFileName)
            val type = object : TypeToken<ArrayList<AlbumData>>() {}.type
            if (json != null) {
                val albumsData = gson.fromJson<ArrayList<AlbumData>>(json, type)
                val albums = ArrayList<Album>()
                for (data in albumsData) {
                    val album = data.toFullClass(existingPictures = pictures)
                    albums.add(album)
                    cleanAlbum(album)
                }
                return albums
            }
            return arrayListOf()
        }

        /**
         * Get buffer pictures as saved in index file + anything new since last time
         * pictures cached was saved.
         *
         * Since this function is supposed to run once on app start, assumption is that the last
         * time the cache was saved was the last time the app was open.
         */
        private fun getAllBufferPictures() : List<Picture> {
            val fromPreviousSession = getBufferPicturesFromDisk()
            val fromPreviousSessionAsSet = fromPreviousSession.toSet()
            val newToSession = getNewPicturePaths()
                    .mapNotNull { path -> pictures[path] }  // Get the Pictures
                    .filter { picture -> picture !in fromPreviousSessionAsSet }  // Remove duplicates
            return newToSession + fromPreviousSession
        }

        /**
         * Gets all buffer pictures listed on save index file that still exist.
         */
        private fun getBufferPicturesFromDisk(): List<Picture> {
            val json = readJsonFromDisk(pictureBufferFileName)
            val type = object : TypeToken<List<String>>() {}.type
            val buffer = mutableListOf<Picture>()
            if (json == null) return listOf()

            val bufferPaths = gson.fromJson<List<String>>(json, type)
            for (path in bufferPaths) {
                // If Picture on disk, add to buffer
                val picture = pictures[path]
                if (picture != null) buffer.add(picture)
            }
            return buffer
        }

        /**
         * Get paths for all Pictures that are not listed in the cache, in order.
         * @return new paths, or empty list if cache doesn't exist (i.e. fresh install)
         */
        private fun getNewPicturePaths(): List<String> {
            return pictures.keys.toList() - (getPathsFromPictureCache() ?: return listOf())
        }

        private fun getPathsFromPictureCache(): Set<String>? {
            val json = readJsonFromDisk(pictureCacheFileName)
            if (json != null) {
                val type = object : TypeToken<HashSet<String>>() {}.type
                return gson.fromJson(json, type)
            }
            return null
        }
    }

    /**
     * Abstraction of changes to on-disk items.
     *
     * @property toAdd list of FileObjects in MediaStore order, because these are brand new
     * @property toRemove a set of paths, because these have to be searched for and removed
     */
    internal class UpdateKit(add: List<FileObject>? = null, remove: Set<String>? = null) {
        val toAdd = add ?: mutableListOf()
        val toRemove = remove ?: mutableSetOf()
    }

    class RecycleBin(directory: File) {

        val name = "recycle-bin"
        private val locationsName = "recycle-restore-locations"
        val file = File(directory, name)
        val oldLocations = mutableMapOf<String, String>()

        val valid: Boolean
            get() = file.isDirectory && file.canRead()
        val contents: List<Picture>
            get() = file.listFiles().map { file -> Picture(file.path.split("/").last(), file.path) }
        val contentsByDateDesc : List<Picture>  // new ones first
            get() = contents.sortedByDescending { picture -> picture.file.lastModified() }

        init {
            file.mkdir()
        }

        fun saveLocationsToDisk() {
            cleanLocations()
            saveJsonToDisk(gson.toJson(oldLocations), locationsName)
        }

        fun loadLocationsFromDisk() {
            val json = readJsonFromDisk(locationsName) ?: return
            val type = object : TypeToken<HashMap<String, String>>() {}.type
            oldLocations.clear()
            oldLocations.putAll(gson.fromJson<HashMap<String, String>>(json, type))
        }

        private fun cleanLocations() {
            if (file.listFiles().isEmpty()) oldLocations.clear()
        }
    }

}