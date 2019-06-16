package iced.egret.palette.util

import android.app.Activity
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import android.provider.MediaStore.MediaColumns
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import iced.egret.palette.model.*
import java.io.File
import java.io.FileNotFoundException


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
     * @param root Root directory
     * @param ignoreFolderNames Names of folders to not scan
     */
    fun getPictureFolder(root: File, ignoreFolderNames: ArrayList<String> = arrayListOf()) : Folder? {

        var rootFolder : Folder? = Folder(root.name, root.path)
        val rootObjects = root.listFiles().filter {
            obj -> (obj.name.toLowerCase() !in ignoreFolderNames && !obj.name.startsWith("."))
        }

        for (obj: File in rootObjects) {
            if (obj.isDirectory) {
                val subfolder = getPictureFolder(obj)
                if (subfolder != null) {
                    rootFolder!!.addFolder(subfolder)
                }
            }
            else {
                if (obj.name.endsWith(".png")
                        || obj.name.endsWith(".jpg")
                        || obj.name.endsWith("jpeg")
                        || obj.name.endsWith("gif")) {
                    rootFolder!!.addPicture(Picture(obj.name, obj.path))
                }
            }
        }

        if (rootFolder!!.isEmpty()) {
            rootFolder = null
        }

        return rootFolder

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
        val sortBy = MediaStore.Images.ImageColumns.DATE_ADDED + " DESC"
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
            parentFolder = rootFolders.find { root -> root.path == rootPath }
            if (parentFolder == null) {
                parentFolder = Folder(rootPath, rootPath)
                rootFolders.add(parentFolder)
            }

            // add additional folders as required, in linked-list-like fashion
            var levelInt = rootLevelIndex + 1
            for (level in pathLevels.subList(rootLevelIndex + 1, pathLevels.size - 1)) {

                folderPath = pathLevels.subList(0, levelInt + 1).joinToString("/")

                childFolder = parentFolder!!.folders.find {folder -> folder.path == folderPath}
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
                albums.add(data.toFullClass())
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

}