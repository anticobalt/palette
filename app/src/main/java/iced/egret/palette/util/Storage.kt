package iced.egret.palette.util

import android.app.Activity
import android.content.ContentResolver
import android.database.Cursor
import android.graphics.Bitmap
import android.net.Uri
import android.provider.MediaStore
import android.provider.MediaStore.MediaColumns
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.documentfile.provider.DocumentFile
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import iced.egret.palette.model.*
import java.io.*


object Storage {

    private const val rootCacheFileName = "root-cache.json"
    private const val albumsFileName = "albums.json"
    private lateinit var fileDirectory : File

    var retrievedFolders = mutableListOf<Folder>()
    var retrievedAlbums = mutableListOf<Album>()
    val retrievedPictures = mutableMapOf<String, Picture>()

    private var gson = Gson()

    fun setup(activity: Activity) {
        fileDirectory = activity.filesDir
        retrievedFolders = getPictureFoldersMediaStore(activity)
        retrievedAlbums = getAlbumsFromDisk()
    }

    /**
     * Gets images from Android's default gallery API
     * API access stuff from https://stackoverflow.com/a/36815451
     */
     private fun getPictureFoldersMediaStore(activity: Activity) : ArrayList<Folder> {

        val uri: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val cursor: Cursor?
        val columnIndexData: Int
        val projection = arrayOf(MediaColumns.DATA)
        val sortBy = MediaStore.Images.ImageColumns.DATE_MODIFIED + " DESC"
        val rootFolders = ArrayList<Folder>()

        cursor = activity.contentResolver.query(uri, projection, null, null, sortBy)
        columnIndexData = cursor!!.getColumnIndexOrThrow(MediaColumns.DATA)

        val rootLevelIndex = 1  // level 0 is empty due to /path/to/file syntax
        var absolutePathOfImage: String
        var pathLevels : List<String>
        var rootPath : String
        var parentFolder : Folder?
        var childFolder : Folder?
        var folderPath : String

        while (cursor.moveToNext()) {

            // get image path
            absolutePathOfImage = cursor.getString(columnIndexData)
            pathLevels = absolutePathOfImage.split("/")
            rootPath = pathLevels[rootLevelIndex]

            // check if root already created (in most cases, yes)
            parentFolder = rootFolders.find { root -> root.truePath == rootPath }
            if (parentFolder == null) {
                parentFolder = Folder(rootPath, rootPath)
                rootFolders.add(parentFolder)
            }

            // add additional folders as required, in linked-list-like fashion
            var levelInt = rootLevelIndex + 1
            for (level in pathLevels.subList(rootLevelIndex + 1, pathLevels.size - 1)) {

                folderPath = pathLevels.subList(0, levelInt + 1).joinToString("/")

                childFolder = parentFolder!!.folders.find {folder -> folder.truePath == folderPath}
                if (childFolder == null) {
                    childFolder = Folder(level, folderPath)
                    parentFolder.addFolder(childFolder)
                }
                parentFolder = childFolder

                levelInt++

            }

            // add picture, parentFolder guaranteed to exist
            val picture = Picture(pathLevels[pathLevels.size - 1], absolutePathOfImage)
            parentFolder!!.addPicture(picture)
            retrievedPictures[absolutePathOfImage] = picture

        }

        cursor.close()
        return rootFolders

    }

    fun saveAlbumsToDisk(albums: List<Album>) {
        val albumsData = ArrayList<AlbumData>()
        for (album in albums) {
            albumsData.add(album.toDataClass())
        }
        val json = gson.toJson(albumsData)
        saveJsonToDisk(json, albumsFileName)
    }

    fun getAlbumsFromDisk() : MutableList<Album> {
        val json = readJsonFromDisk(albumsFileName)
        val type = object : TypeToken<ArrayList<AlbumData>>() {}.type
        if (json != null) {
            val albumsData = gson.fromJson<ArrayList<AlbumData>>(json, type)
            val albums  = ArrayList<Album>()
            for (data in albumsData) {
                val album = data.toFullClass(existingPictures = retrievedPictures)
                albums.add(album)
                cleanAlbum(album)
            }
            return albums
        }
        return arrayListOf()
    }

    fun saveFolderToDisk(folder: Folder) {
        val rootData = folder.toDataClass()
        val json = gson.toJson(rootData)
        saveJsonToDisk(json, rootCacheFileName)
    }

    fun getFolderFromDisk() : Folder? {
        val json = readJsonFromDisk(rootCacheFileName)
        if (json != null) {
            val rootData = gson.fromJson(json, FolderData::class.java)
            return rootData.toFullClass()
        }
        return null
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

    fun saveBitmapToDisk(bitmap: Bitmap, name: String, location: String,
                         sdCardFile: DocumentFile?, contentResolver: ContentResolver) : File? {

        val extension = name.split(".").last().toLowerCase()
        val path = location + name
        val file = File(path)
        val locationOnSdCard = pathOnSdCard(location)
        val outStream : OutputStream

        val compressionFormat = when (extension) {
            "png" -> Bitmap.CompressFormat.PNG
            "webp" -> Bitmap.CompressFormat.WEBP
            else -> Bitmap.CompressFormat.JPEG
        }

        // If saving to SD card and can do it, do it. Otherwise try to save normally.
        // https://stackoverflow.com/a/43317703
        if (locationOnSdCard != null && sdCardFile != null) {
            outStream = getSdOutputStream(name, locationOnSdCard, "image/webp", sdCardFile, contentResolver)
                    ?: return null
        }
        else {
            try {
                outStream = FileOutputStream(file)
            } catch (accessDenied: FileNotFoundException) {
                return null
            }
        }

        // save as max quality
        bitmap.compress(compressionFormat, 100, outStream)
        outStream.close()

        return file

    }

    fun moveFile(sourcePath: String, destinationParent: File,
                 sdCardFile: DocumentFile?, contentResolver: ContentResolver) : File? {
        val sourceFile = File(sourcePath)
        val newFile = copyFile(sourceFile, destinationParent, sdCardFile, contentResolver)
        deleteFile(sourceFile, sdCardFile, contentResolver)
        return newFile
    }

    /**
     * If copying to SD card (and can access SD card), do so using DocumentFiles.
     * Otherwise, try to copy as normal.
     *
     * @return The copy of the File, or null if failed.
     */
    fun copyFile(sourceFile: File, destinationParent: File,
                 sdCardFile: DocumentFile?, contentResolver: ContentResolver) : File? {

        val name = sourceFile.name
        val destinationLocation = destinationParent.path.removeSuffix("/") + "/"
        val destinationOnSdCard = pathOnSdCard(destinationLocation)
        val inStream = FileInputStream(sourceFile)
        val outStream: OutputStream
        val copy = File(destinationLocation + name)

        if (destinationOnSdCard != null && sdCardFile != null) {
            outStream = getSdOutputStream(name, destinationOnSdCard, "image/webp", sdCardFile, contentResolver)
                    ?: return null
        }
        else {
            try {
                outStream = FileOutputStream(copy)
            } catch (accessDenied: FileNotFoundException) {
                return null
            }
        }

        copyStreams(inStream, outStream)
        return copy
    }

    /**
     * If file is on SD card, and have access to SD card, use DocumentFiles to delete.
     * Otherwise, try to delete as normal.
     *
     * @return True if deleted, false if not.
     */
    fun deleteFile(sourceFile: File, sdCardFile: DocumentFile?, contentResolver: ContentResolver) : Boolean {
        val pathOnSdCard = pathOnSdCard(sourceFile.path)
        if (pathOnSdCard != null && sdCardFile != null) {
            val sourceDocumentFile = findFileInsideRecursive(sdCardFile, pathOnSdCard) ?: return false
            return sourceDocumentFile.delete()
        }
        else {
            return sourceFile.delete()
        }
    }

    /**
     * @param name The filename (e.g. myImage.png) or whole path (e.g. /path/to/myImage.png)
     * @param location The folder the file sits in (e.g. /path/to)
     */
    fun fileExists(name: String, location: String = "") : Boolean {
        val file = File(location + name)
        return file.exists()
    }

    private fun copyStreams(inputStream: InputStream, outputStream: OutputStream) {
        inputStream.copyTo(outputStream)
        inputStream.close()
        outputStream.close()
    }

    private fun isPathOnSdCard(path: String) : Boolean {
        return pathOnSdCard(path) != null
    }

    private fun pathOnSdCard(path: String) : String? {
        // At start of string, match "/storage/" + any 4 alphanumerics + a dash + any 4 alphanumerics + "/"
        val regex = Regex("^/storage/[a-zA-z0-9]{4}-[a-zA-z0-9]{4}/")
        val sdCardLocation = regex.find(path)?.value ?: return null
        return path.removePrefix(sdCardLocation).removeSuffix("/")
    }

    private fun getSdOutputStream(name: String, locationOnSdCard: String, defaultMimeType : String,
                                  sdCardFile: DocumentFile, contentResolver: ContentResolver) : OutputStream? {
        val extension = name.split(".").last().toLowerCase()
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: defaultMimeType
        val subFolderFile = findFileInsideRecursive(sdCardFile, locationOnSdCard) ?: return null
        val imageFile = getChildFile(subFolderFile, mimeType, name) ?: return null
        return contentResolver.openOutputStream(imageFile.uri) ?: return null
    }

    /**
     * Get the DocumentFile for some folder/file somewhere inside given root DocumentFile.
     *
     * @return The desired DocumentFile, or null if not found.
     */
    private fun findFileInsideRecursive(rootFile: DocumentFile, relativePath: String) : DocumentFile? {

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
    private fun getChildFile(subFolderFile: DocumentFile, mimeType: String, name: String) : DocumentFile? {
        return subFolderFile.findFile(name) ?: subFolderFile.createFile(mimeType, name)
    }

    /**
     * Ensure all Pictures in album exist on disk. If they don't, remove them.
     */
    private fun cleanAlbum(album: Album) {
        val pictures = album.pictures.toList()  // a copy to avoid concurrency error
        for (picture in pictures) {
            if (!fileExists(picture.path)) {
                album.removePicture(picture)
            }
        }
    }

}