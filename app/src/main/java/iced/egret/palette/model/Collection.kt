package iced.egret.palette.model

import android.net.Uri
import android.view.View
import com.bumptech.glide.Glide
import iced.egret.palette.R
import iced.egret.palette.adapter.CoverViewHolder
import java.io.Serializable

/***
 * Properties:
 * - name
 * - terminal
 * - deletable
 * - cover
 * - pictures
 * - size
 */
abstract class Collection(override var name: String) : Coverable {

    override val terminal = false
    override val cover = mutableMapOf<String, Any>(
            "id" to R.drawable.ic_default_collection_cover
    )

    abstract var pictures: ArrayList<Picture>
        protected set
    var size = 0
        protected set
    var recursiveSize = 0
        protected set

    fun isEmpty() : Boolean {
        return size == 0
    }

    fun isNotEmpty() : Boolean {
        return !isEmpty()
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

    // If contents are of varying types,
    // Non-Terminals should be before Terminals
    // to simplify launching (because position is required)
    abstract fun getContents() : MutableList<Coverable>

    open fun addPicture(newPicture: Picture) {
        pictures.add(newPicture)
        size += 1
    }
    open fun addPictures(newPictures: MutableList<Picture>) {
        pictures.addAll(newPictures)
        size += newPictures.size
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
 * - recursiveSize
 */
class Folder(name: String, val path: String, subFolders: MutableList<Folder> = mutableListOf()) : Collection(name) {

    override val deletable = false
    override var pictures = ArrayList<Picture>()
    var folders = subFolders
        private set

    init {
        size = folders.size
        recursiveSize = folders.size
    }

    override fun toString(): String {
        return "$path, $size"
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
    override fun getContents(): MutableList<Coverable> {
        val folders = folders as ArrayList<Coverable>
        val pictures = pictures as ArrayList<Coverable>
        return (folders + pictures) as MutableList<Coverable>
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

    override fun addPicture(newPicture: Picture) {
        super.addPicture(newPicture)
        recursiveSize += 1
    }

    override fun addPictures(newPictures: MutableList<Picture>) {
        super.addPictures(newPictures)
        recursiveSize += newPictures.size
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
 * - terminal
 * - deletable
 * - cover
 * - albums
 * - folders
 * - pictures
 * - size
 * - recursiveSize
 */
class Album(name: String) : Collection(name) {

    companion object {
        const val NAME_MAX_LENGTH = 25
    }

    override val deletable = true
    override var pictures = ArrayList<Picture>()
    var albums : MutableList<Album> = ArrayList()
        private set
    var folders : MutableList<Folder> = ArrayList()
        private set

    fun toDataClass() : AlbumData {
        val picturePaths = pictures.map { picture -> picture.path }
        val foldersData = folders.map { folder -> folder.toDataClass() }
        val albumsData = albums.map { album -> album.toDataClass() }
        return AlbumData(name, picturePaths, foldersData, albumsData)
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

    @Suppress("UNCHECKED_CAST")
    private fun getCollections() : MutableList<Collection> {
        return ((albums as ArrayList<Collection>) + (folders as ArrayList<Collection>)) as MutableList<Collection>
    }

    @Suppress("UNCHECKED_CAST")
    override fun getContents(): MutableList<Coverable> {
        val folders = folders as ArrayList<Coverable>
        val pictures = pictures as ArrayList<Coverable>
        val albums = albums as ArrayList<Coverable>
        return (folders + pictures + albums) as MutableList<Coverable>
    }

    private fun addCollection(newCollection: Collection, collectionList: MutableList<Collection>) {
        collectionList.add(newCollection)
        size += 1
        recursiveSize += newCollection.recursiveSize
    }

    private fun addCollections(newCollections: MutableList<Collection>, collectionList: MutableList<Collection>) {
        collectionList.addAll(newCollections)
        size += newCollections.size
        for (newCollection: Collection in newCollections) {
            recursiveSize += newCollection.recursiveSize
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun addFolder(newFolder: Folder) {
        addCollection(newFolder, folders as MutableList<Collection>)
    }

    @Suppress("UNCHECKED_CAST")
    fun addFolders(newFolders: MutableList<Folder>) {
        addCollections(newFolders as MutableList<Collection>, folders as MutableList<Collection>)
    }

    @Suppress("UNCHECKED_CAST")
    fun addAlbum(newAlbum: Album) {
        addCollection(newAlbum, albums as MutableList<Collection>)
    }

    @Suppress("UNCHECKED_CAST")
    fun addAlbums(newAlbums: MutableList<Album>) {
        addCollections(newAlbums as MutableList<Collection>, albums as MutableList<Collection>)
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

data class AlbumData(val name: String,
                     val picturePaths: List<String>,
                     val foldersData: List<FolderData>,
                     val albumsData: List<AlbumData>) : Serializable {

    fun toFullClass() : Album {
        val album = Album(name)
        val folders = foldersData.map {data -> data.toFullClass() } as MutableList<Folder>
        val albums = albumsData.map {data -> data.toFullClass() } as MutableList<Album>
        val pictures = picturePaths.map {path -> Picture(path.split("/").last(), path) } as MutableList<Picture>
        album.addFolders(folders)
        album.addAlbums(albums)
        album.addPictures(pictures)
        return album
    }
}
