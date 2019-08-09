package iced.egret.palette.model.dataclass

import iced.egret.palette.model.Album
import iced.egret.palette.model.Picture
import java.io.File
import java.io.Serializable

data class AlbumData(val name: String,
                     val path: String,
                     val picturePaths: List<String>,
                     val syncedFolderFileData: List<String>,
                     val albumsData: List<AlbumData>) : Serializable {

    fun toFullClass(existingPictures: Map<String, Picture> = mapOf()): Album {

        val album = Album(name, path)
        val syncedFolderFiles = syncedFolderFileData.map { data -> File(data) }
        val albums = albumsData.map { data -> data.toFullClass(existingPictures) } as MutableList<Album>
        val pictures = arrayListOf<Picture>()

        // Link up pictures in album that exist on disk
        for (path in picturePaths) {
            val picture = existingPictures[path]
            if (picture != null) {
                pictures.add(picture)
            }
        }

        album.syncedFolderFiles.addAll(syncedFolderFiles)
        album.addAlbums(albums)
        album.addPictures(pictures)
        return album
    }
}