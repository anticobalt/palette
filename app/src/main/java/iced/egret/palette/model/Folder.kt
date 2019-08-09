package iced.egret.palette.model

import android.net.Uri
import iced.egret.palette.R
import iced.egret.palette.model.dataclass.FolderData
import iced.egret.palette.model.inherited.Collection
import iced.egret.palette.model.inherited.Coverable
import iced.egret.palette.model.inherited.FileObject

/***
 * Properties:
 * - name
 * - path
 * - filePath (identical to path)
 * - parent
 * - deletable
 * - cover
 * - terminal
 * - icon
 * - pictures
 * - folders
 * - size
 * - totalSize
 *
 * Backing fields _folders and _pictures only used when adding/deleting internally.
 * TODO: make Folder's parent's type Folder, while not restricting other FileObject's parents, somehow.
 *  If this done, all remove functions need to be adjusted.
 *
 */
class Folder(name: String, override var filePath: String, subFolders: MutableList<Folder> = mutableListOf(),
             override var parent: FileObject? = null)
    : Collection(name, filePath), FileObject {

    override val icon = R.drawable.ic_folder_black_24dp
    override val deletable: Boolean
        get() = _folders.size + _pictures.size == 0

    override var _pictures: MutableList<Picture> = ArrayList()
    override val pictures: List<Picture>
        get() = _pictures

    private var _folders: MutableList<Folder> = subFolders
    val folders: List<Folder>
        get() = _folders

    val nestedPictures: List<Picture>
        get() {
            return (pictures + folders.flatMap { folder -> folder.nestedPictures })
                    .sortedByDescending { picture -> picture.file.lastModified() }
        }

    override val contentsMap: Map<String, List<Coverable>>
        get() {
            val map = mutableMapOf<String, List<Coverable>>()
            map["folders"] = folders
            map["pictures"] = pictures
            return map
        }

    override val totalSize: Int
        get() {
            var rs = pictures.size
            for (folder in folders) {
                rs += folder.totalSize
            }
            return rs
        }

    init {
        size = folders.size
    }

    fun toDataClass(): FolderData {
        val picturePaths = pictures.map { picture -> picture.filePath }
        val subFolders = folders.map { folder -> folder.toDataClass() }
        return FolderData(name, path, subFolders, picturePaths)
    }

    override fun getOnePictureUri(): Uri? {
        if (pictures.isNotEmpty()) {
            return pictures[0].uri
        } else if (folders.isNotEmpty()) {
            return folders[0].getOnePictureUri()
        }
        return null
    }

    @Suppress("UNCHECKED_CAST")
    override fun getContents(): List<Coverable> {
        val folders = folders as List<Coverable>
        val pictures = pictures as List<Coverable>
        return (folders + pictures)
    }

    override fun rename(name: String) {
        // TODO
    }

    fun addFolder(newFolder: Folder) {
        _folders.add(newFolder)
        size += 1
    }

    fun addFolders(newFolders: List<Folder>) {
        _folders.addAll(newFolders)
        size += newFolders.size
    }

    fun removeFolder(folder: Folder) {
        _folders.remove(folder)
        size -= 1
        deleteIfShould()
    }

    /**
     * Remove Picture and delete self if required.
     */
    override fun removePicture(picture: Picture) {
        super.removePicture(picture)
        deleteIfShould()
    }

    private fun deleteIfShould() {
        if (deletable) delete()
    }

    /**
     * Deletes self by removing own reference to parent and parent's reference to self,
     * in doubly-linked list fashion.
     *
     * @return Success or failure. Fails if object initialized with non-Folder parent.
     */
    private fun delete(): Boolean {
        val parentAsFolder = parent as? Folder ?: return false
        parentAsFolder.removeFolder(this)
        parent = null
        return true
    }

}