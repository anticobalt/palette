package iced.egret.palette.model

import android.net.Uri
import iced.egret.palette.R
import iced.egret.palette.model.dataclass.AlbumData
import iced.egret.palette.model.inherited.Collection
import iced.egret.palette.model.inherited.Coverable
import iced.egret.palette.util.CollectionManager
import java.io.File

/***
 * Properties:
 * - name
 * - path
 * - terminal
 * - deletable
 * - cover
 * - icon
 * - albums
 * - syncedFolderFiles
 * - syncedFolders
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
        get() = (_pictures + syncedFolders.flatMap { folder -> folder.nestedPictures })
                .sortedByDescending { picture -> picture.file.lastModified() }

    private var _albums: MutableList<Album> = ArrayList()
    val albums: List<Album>
        get() = _albums

    // All folders, existent or not, that is synced
    var syncedFolderFiles: MutableList<File> = ArrayList()
    // Folders that are synced and actually currently exist and have Pictures in them
    private val syncedFolders: List<Folder>
        get() {
            val folders = mutableListOf<Folder>()
            for (file in syncedFolderFiles) {
                val folder = CollectionManager.findFolderByPath(file.path) ?: continue
                folders.add(folder)
            }
            return folders
        }

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
        for (album in _albums) {
            album.onParentPathChange(path)
        }
    }

    override val contentsMap: Map<String, List<Coverable>>
        get() {
            val map = mutableMapOf<String, List<Coverable>>()
            map["albums"] = _albums
            map["pictures"] = pictures
            return map
        }

    override val totalSize: Int
        get() {
            var rs = _pictures.size
            for (folder in syncedFolders) {
                rs += folder.totalSize
            }
            for (album in _albums) {
                rs += album.totalSize
            }
            return rs
        }

    fun toDataClass(): AlbumData {
        val picturePaths = _pictures.map { picture -> picture.filePath }
        val syncedFolderFileData = syncedFolderFiles.map { file -> file.path }
        val albumsData = _albums.map { album -> album.toDataClass() }
        return AlbumData(name, path, picturePaths, syncedFolderFileData, albumsData)
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
        return ((_albums as List<Collection>) + (syncedFolders as List<Collection>))
    }

    override fun getContents(): List<Coverable> {
        val pictures = pictures as List<Coverable>
        val albums = _albums as List<Coverable>
        return (albums + pictures)
    }

    fun addAlbum(newAlbum: Album) {
        _albums.add(newAlbum)
        size += 1
    }

    fun addAlbums(newAlbums: List<Album>) {
        newAlbums.map { album -> addAlbum(album) }
    }

    fun removeAlbum(album: Album) {
        _albums.remove(album)
        size -= 1
    }

    fun removeAlbums(albums: List<Album>) {
        albums.map { album -> removeAlbum(album) }
    }

    /**
     * Check if the given pictures all actually belong to the album, or contained synced ones.
     */
    fun ownsPictures(samplePictures: List<Picture>): Boolean {
        val setDifference = samplePictures.toSet() - _pictures.toSet()
        return setDifference.isEmpty()
    }

}