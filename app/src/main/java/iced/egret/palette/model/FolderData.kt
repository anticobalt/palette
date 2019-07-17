package iced.egret.palette.model

import java.io.Serializable

data class FolderData(val name: String,
                      val path: String,
                      val subFolderDataList: List<FolderData> = mutableListOf(),
                      val picturePaths : List<String> = mutableListOf()) : Serializable {

    fun toFullClass(parent: Folder? = null) : Folder {
        val folder = Folder(name, path, parent = parent)
        val subFolders = subFolderDataList.map { subFolderData -> subFolderData.toFullClass(folder) }
        val pictures = picturePaths.map {path -> Picture(path.split("/").last(), path) }
        folder.addFolders(subFolders)
        folder.addPictures(pictures)
        return folder
    }

}
