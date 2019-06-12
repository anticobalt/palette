package iced.egret.palette

import iced.egret.palette.model.Folder
import iced.egret.palette.model.Picture
import org.junit.Test

class CollectionUnitTest {

    private val name = "a name"
    private val path = "a/path/to/a name"

    @Test fun folderInit() {

        var folder = Folder(name, path)
        assert(folder.name == name)
        assert(folder.path == path)
        assert(folder.folders.size == 0)
        assert(folder.pictures.size == 0)

        val subFolders = arrayListOf(
                Folder("a", "path/to/a"),
                Folder("b", "/a/path/to/b")
        )
        folder = Folder(name, path, subFolders = subFolders)
        assert(folder.folders == subFolders)
        assert(folder.pictures.size == 0)

    }

    @Test fun folderAddChildren() {

        val folder = Folder(name, path)
        val subFolder = Folder("z", "antipath/to/z")
        val subFolders = arrayListOf(
                Folder("a", "path/to/a"),
                Folder("b", "a/path/to/b")
        )
        val picture = Picture("name", "whats/in/a/name")
        val pictures = arrayListOf(
                Picture("here.jpg", "/destination/here.jpg"),
                Picture("here.png", "source/here.png"),
                Picture("here.gif", "/redirect/to/here.gif")
        )

        // one folder
        assert(subFolder !in folder.folders)
        folder.addFolder(subFolder)
        assert(subFolder in folder.folders)

        // many folders
        assert(folder.folders.intersect(subFolders).isEmpty())
        folder.addFolders(subFolders)
        assert(folder.folders.intersect(subFolders).size == subFolders.size)
        assert(subFolder in folder.folders)

        // one picture
        assert(picture !in folder.pictures)
        folder.addPicture(picture)
        assert(picture in folder.pictures)

        // many pictures
        assert(folder.pictures.intersect(pictures).isEmpty())
        folder.addPictures(pictures)
        assert(folder.pictures.intersect(pictures).size == pictures.size)
        assert(picture in folder.pictures)

    }

    @Test fun folderSizes() {

        val folder = Folder(name, path)
        val subFolders = arrayListOf(
                Folder("a", "path/to/a"),
                Folder("b", "a/path/to/b")
        )
        val pictures = arrayListOf(
                Picture("here.jpg", "/destination/here.jpg"),
                Picture("here.png", "source/here.png"),
                Picture("here.gif", "/redirect/to/here.gif")
        )
        var recursiveSize = 0

        assert(folder.isEmpty())
        assert(!(folder.isNotEmpty()))
        assert(folder.size == 0)
        assert(folder.recursiveSize == 0)

        folder.addFolders(subFolders)
        assert(folder.size == subFolders.size)
        for (f: Folder in subFolders) {
            recursiveSize += f.recursiveSize
        }
        assert(folder.recursiveSize == recursiveSize)

        folder.addPictures(pictures)
        assert(folder.size == subFolders.size + pictures.size)
        assert(folder.recursiveSize == recursiveSize + pictures.size)

    }

}