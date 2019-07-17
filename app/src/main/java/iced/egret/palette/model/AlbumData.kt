package iced.egret.palette.model

import java.io.Serializable

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