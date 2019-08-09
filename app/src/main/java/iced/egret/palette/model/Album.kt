package iced.egret.palette.model

import android.net.Uri
import iced.egret.palette.R
import iced.egret.palette.model.dataclass.AlbumData
import iced.egret.palette.model.inherited.Collection
import iced.egret.palette.model.inherited.Coverable

/***
 * Properties:
 * - name
 * - path
 * - terminal
 * - deletable
 * - cover
 * - icon
 * - albums
 * - folders
 * - pictures
 * - size
 * - totalSize
 *
 * Backing fields _folders, _albums, and _pictures only used when adding/deleting internally.
 */
class Album(name: String, path: String, val parent: Album? = null) : Collection(name, path) {

    companion object {
        const val NAME_MAX_LENGTH = 25
    }

    override val icon = R.drawable.ic_photo_album_black_24dp

    override var _pictures: MutableList<Picture> = ArrayList()
    override val pictures: List<Picture>
        get() = _pictures

    private var _albums: MutableList<Album> = ArrayList()
    val albums: List<Album>
        get() = _albums

    private var _folders: MutableList<Folder> = ArrayList()
    val folders: List<Folder>
        get() = _folders

    override fun rename(name: String) {
        this.name = name
        path = (path.split("/").dropLast(1).joinToString("/") + "/" + name)
                .trim('/')  // remove leading "/" if exists

        for (album in _albums) {
            album.onParentPathChange(path)
        }
    }

    /**
     * Update path due to parent path change, and call recursively on children.
     */
    private fun onParentPathChange(parentPath: String) {
        path = "$parentPath/$name"
        for (album in albums) {
            album.onParentPathChange(path)
        }
    }

    override val contentsMap: Map<String, List<Coverable>>
        get() {
            val map = mutableMapOf<String, List<Coverable>>()
            map["folders"] = folders
            map["albums"] = albums
            map["pictures"] = pictures
            return map
        }

    override val totalSize: Int
        get() {
            var rs = pictures.size
            for (folder in folders) {
                rs += folder.totalSize
            }
            for (album in albums) {
                rs += album.totalSize
            }
            return rs
        }

    fun toDataClass(): AlbumData {
        val picturePaths = pictures.map { picture -> picture.filePath }
        val foldersData = folders.map { folder -> folder.toDataClass() }
        val albumsData = albums.map { album -> album.toDataClass() }
        return AlbumData(name, path, picturePaths, foldersData, albumsData)
    }

    override fun getOnePictureUri(): Uri? {
        val collections = getCollections()
        if (pictures.isNotEmpty()) {
            return pictures[0].uri
        } else if (collections.isNotEmpty()) {
            return collections[0].getOnePictureUri()
        }
        return null
    }

    private fun getCollections(): List<Collection> {
        return ((albums as List<Collection>) + (folders as List<Collection>))
    }

    @Suppress("UNCHECKED_CAST")
    override fun getContents(): List<Coverable> {
        val folders = folders as List<Coverable>
        val pictures = pictures as List<Coverable>
        val albums = albums as List<Coverable>
        return (folders + albums + pictures)
    }

    private fun addCollection(newCollection: Collection, collectionList: MutableList<Collection>) {
        collectionList.add(newCollection)
        size += 1
    }

    private fun addCollections(newCollections: List<Collection>, collectionList: MutableList<Collection>) {
        collectionList.addAll(newCollections)
        size += newCollections.size
    }

    @Suppress("UNCHECKED_CAST")
    fun addFolder(newFolder: Folder) {
        addCollection(newFolder, _folders as MutableList<Collection>)
    }

    @Suppress("UNCHECKED_CAST")
    fun addFolders(newFolders: List<Folder>) {
        addCollections(newFolders as List<Collection>, _folders as MutableList<Collection>)
    }

    @Suppress("UNCHECKED_CAST")
    fun addAlbum(newAlbum: Album) {
        addCollection(newAlbum, _albums as MutableList<Collection>)
    }

    @Suppress("UNCHECKED_CAST")
    fun addAlbums(newAlbums: List<Album>) {
        addCollections(newAlbums as List<Collection>, _albums as MutableList<Collection>)
    }

    fun removeFolder(folder: Folder) {
        _folders.remove(folder)
        size -= 1
    }

    fun removeAlbum(album: Album) {
        _albums.remove(album)
        size -= 1
    }

    fun removeAlbums(albums: List<Album>) {
        albums.map { album -> removeAlbum(album) }
    }

}