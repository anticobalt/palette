package iced.egret.palette

abstract class Collection(val name: String) {

    protected var pictures: ArrayList<Picture> = ArrayList()
    protected var size = 0

    open fun addPicture(picture: Picture) {
        pictures.add(picture)
        size += 1
    }

    open fun addPictures(pictures: MutableList<Picture>) {
        pictures.addAll(pictures)
        size += pictures.size
    }

}

class Folder(name: String, val path: String, private var folders: MutableList<Folder> = mutableListOf()) : Collection(name) {

    private var recursiveSize = 0

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

    fun addFolder(folder: Folder) {
        folders.add(folder)
        size += 1
        recursiveSize += folder.recursiveSize
    }

    fun addFolders(folders: MutableList<Folder>) {
        folders.addAll(folders)
        size += folders.size
        for (folder: Folder in folders) {
            recursiveSize += folder.recursiveSize
        }
    }

    fun getFolders() : MutableList<Folder> {
        return folders
    }

    override fun addPicture(picture: Picture) {
        super.addPicture(picture)
        recursiveSize += 1
    }

    override fun addPictures(pictures: MutableList<Picture>) {
        super.addPictures(pictures)
        recursiveSize += pictures.size
    }

}
class Album(name: String) : Collection(name)
class SmartAlbum(name: String, val folders: MutableList<Folder> = mutableListOf()) : Collection(name)

