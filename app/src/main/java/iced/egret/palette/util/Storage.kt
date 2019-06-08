package iced.egret.palette.util

import java.io.File
import android.provider.MediaStore
import android.provider.MediaStore.MediaColumns
import android.app.Activity
import android.database.Cursor
import android.net.Uri
import iced.egret.palette.model.Folder
import iced.egret.palette.model.Picture


object Storage {

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
     fun getPictureFoldersMediaStore(activity: Activity) : ArrayList<Folder> {

        val uri: Uri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI
        val cursor: Cursor?
        val columnIndexData: Int
        val projection = arrayOf(MediaColumns.DATA)
        val rootFolders = ArrayList<Folder>()

        cursor = activity.contentResolver.query(uri, projection, null, null, null)
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

                childFolder = parentFolder!!.getFolders().find {folder -> folder.path == folderPath}
                if (childFolder == null) {
                    childFolder = Folder(level, folderPath)
                    parentFolder.addFolder(childFolder)
                }
                parentFolder = childFolder

                levelInt++

            }

            // add picture, parentFolder guaranteed to exist
            parentFolder!!.addPicture(Picture(pathLevels[pathLevels.size - 1], absolutePathOfImage))

        }

        cursor.close()
        return rootFolders

    }

}