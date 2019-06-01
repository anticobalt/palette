package iced.egret.palette

abstract class Collection(val name: String) {

    protected var pictures: ArrayList<Picture> = ArrayList()
    var size = 0
        protected set

    open fun addPicture(newPicture: Picture) {
        pictures.add(newPicture)
        size += 1
    }

    open fun addPictures(newPictures: MutableList<Picture>) {
        pictures.addAll(newPictures)
        size += newPictures.size
    }

    open fun getPictures() : MutableList<Picture> {
        return pictures
    }

}

class Folder(name: String, val path: String, private var folders: MutableList<Folder> = mutableListOf()) : Collection(name) {

    var recursiveSize = 0
        private set

    init {
        size += folders.size
        recursiveSize += folders.size
    }

    fun isEmpty() : Boolean {
        return (folders.size + pictures.size) == 0
    }

    fun isNotEmpty() : Boolean {
        return !isEmpty()
    }


    fun addFolder(newFolder: Folder) {
        folders.add(newFolder)
        size += 1
        recursiveSize += newFolder.recursiveSize
    }

    fun addFolders(newFolders: MutableList<Folder>) {
        folders.addAll(newFolders)
        size += newFolders.size
        for (newFolder: Folder in newFolders) {
            recursiveSize += newFolder.recursiveSize
        }
    }

    fun getFolders() : MutableList<Folder> {
        return folders
    }

    override fun addPicture(newPicture: Picture) {
        super.addPicture(newPicture)
        recursiveSize += 1
    }

    override fun addPictures(newPictures: MutableList<Picture>) {
        super.addPictures(newPictures)
        recursiveSize += newPictures.size
    }

}
class Album(name: String) : Collection(name)
class SmartAlbum(name: String, val folders: MutableList<Folder> = mutableListOf()) : Collection(name)

