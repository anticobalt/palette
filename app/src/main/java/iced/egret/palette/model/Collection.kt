package iced.egret.palette.model

import android.net.Uri
import android.view.View
import com.bumptech.glide.Glide
import iced.egret.palette.R
import iced.egret.palette.recyclerview_component.CoverViewHolder
import java.io.Serializable

/***
 * Properties:
 * - name
 * - terminal
 * - deletable
 * - cover
 * - pictures
 * - size
 * - totalSize
 */
abstract class Collection(override var name: String) : Coverable {

    override val terminal = false
    override val cover = mutableMapOf<String, Any>(
            "id" to R.drawable.ic_default_collection_cover
    )

    abstract var _pictures: MutableList<Picture>  // internal
    abstract val pictures: List<Picture>  // external

    abstract val contentsMap: Map<String, List<Coverable>>
    abstract val totalSize : Int
    var size = 0
        protected set

    fun isEmpty() : Boolean {
        return size == 0
    }

    fun isNotEmpty() : Boolean {
        return !isEmpty()
    }

    override fun toString(): String {
        return "$name, $size, $totalSize"
    }

    override fun loadCoverInto(holder: CoverViewHolder) {
        if (holder.ivItem != null) {
            setCoverUri()
            val image : Any? = if (cover["uri"] != null) {
                cover["uri"]
            } else {
                cover["id"]
            }
            Glide.with(holder.itemView.context)
                    .load(image)
                    .centerCrop()
                    .into(holder.ivItem)
        }
        if (holder.tvItem != null) {
            holder.tvItem.visibility = View.VISIBLE
        }
    }

    abstract fun getContents() : List<Coverable>

    open fun addPicture(newPicture: Picture) {
        _pictures.add(newPicture)
        size += 1
    }
    open fun addPictures(newPictures: MutableList<Picture>) {
        _pictures.addAll(newPictures)
        size += newPictures.size
    }

    open fun removePicture(picture: Picture) {
        _pictures.remove(picture)
        size -= 1
    }

    private fun setCoverUri() {
        val uri = getOnePictureUri()
        if (uri != null) {
            cover["uri"] = uri
        }
    }

    abstract fun getOnePictureUri() : Uri?

}

/***
 * Properties:
 * - name
 * - path
 * - terminal
 * - deletable
 * - cover
 * - pictures
 * - folders
 * - size
 * - totalSize
 *
 * Backing fields _folders and _pictures only used when adding/deleting internally.
 */
class Folder(name: String, val path: String, subFolders: MutableList<Folder> = mutableListOf()) : Collection(name) {

    override var _pictures : MutableList<Picture> = ArrayList()
    override val pictures : List<Picture>
        get() = _pictures

    private var _folders : MutableList<Folder> = subFolders
    val folders : List<Folder>
        get() = _folders

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

    fun toDataClass() : FolderData {
        val picturePaths = pictures.map { picture -> picture.path }
        val subFolders = folders.map { folder -> folder.toDataClass() }
        return FolderData(name, path, subFolders, picturePaths)
    }

    override fun getOnePictureUri() : Uri? {
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

    fun addFolder(newFolder: Folder) {
        _folders.add(newFolder)
        size += 1
    }

    fun addFolders(newFolders: MutableList<Folder>) {
        _folders.addAll(newFolders)
        size += newFolders.size
    }

    fun removeFolder(folder: Folder) {
        _folders.remove(folder)
        size -= 1
    }

}

data class FolderData(val name: String,
                      val path: String,
                      val subFolders: List<FolderData> = mutableListOf(),
                      val picturePaths : List<String> = mutableListOf()) : Serializable {

    fun toFullClass() : Folder {
        val fullSubFolders = subFolders.map {folder -> folder.toFullClass() } as MutableList<Folder>
        val pictures = picturePaths.map {path -> Picture(path.split("/").last(), path) } as MutableList<Picture>
        val folder = Folder(name, path, fullSubFolders)
        folder.addPictures(pictures)
        return folder
    }

}

/***
 * Properties:
 * - name
 * - path
 * - terminal
 * - deletable
 * - cover
 * - albums
 * - folders
 * - pictures
 * - size
 * - totalSize
 *
 * Backing fields _folders, _albums, and _pictures only used when adding/deleting internally.
 */
class Album(name: String, val path: String) : Collection(name) {

    companion object {
        const val NAME_MAX_LENGTH = 25
    }

    override var _pictures : MutableList<Picture> = ArrayList()
    override val pictures : List<Picture>
        get() = _pictures

    private var _albums : MutableList<Album> = ArrayList()
    val albums : List<Album>
        get() = _albums

    private var _folders : MutableList<Folder> = ArrayList()
    val folders : List<Folder>
        get() = _folders

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

    fun toDataClass() : AlbumData {
        val picturePaths = pictures.map { picture -> picture.path }
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

    private fun getCollections() : List<Collection> {
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

}

data class AlbumData(val name: String,
                     val path: String,
                     val picturePaths: List<String>,
                     val foldersData: List<FolderData>,
                     val albumsData: List<AlbumData>) : Serializable {

    fun toFullClass(existingPictures: Map<String, Picture> = mapOf()) : Album {

        val album = Album(name, path)
        val folders = foldersData.map {data -> data.toFullClass() } as MutableList<Folder>
        val albums = albumsData.map {data -> data.toFullClass() } as MutableList<Album>
        val pictures = arrayListOf<Picture>()

        for (path in picturePaths) {
            var picture = existingPictures[path]
            if (picture == null) {
                picture = Picture(path.split("/").last(), path)
            }
            pictures.add(picture)
        }

        album.addFolders(folders)
        album.addAlbums(albums)
        album.addPictures(pictures)
        return album
    }
}
