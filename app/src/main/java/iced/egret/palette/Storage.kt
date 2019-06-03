package iced.egret.palette

import android.util.Log
import java.io.File

object Storage {

    /**
     * Get the full names of all folders with pictures.
     *
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

}