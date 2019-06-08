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
        assert(folder.getFolders().size == 0)
        assert(folder.getPictures().size == 0)

        val subFolders = arrayListOf(
                Folder("a", "path/to/a"),
                Folder("b", "/a/path/to/b")
        )
        folder = Folder(name, path, subFolders = subFolders)
        assert(folder.getFolders() == subFolders)
        assert(folder.getPictures().size == 0)

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
        assert(subFolder !in folder.getFolders())
        folder.addFolder(subFolder)
        assert(subFolder in folder.getFolders())

        // many folders
        assert(folder.getFolders().intersect(subFolders).isEmpty())
        folder.addFolders(subFolders)
        assert(folder.getFolders().intersect(subFolders).size == subFolders.size)
        assert(subFolder in folder.getFolders())

        // one picture
        assert(picture !in folder.getPictures())
        folder.addPicture(picture)
        assert(picture in folder.getPictures())

        // many pictures
        assert(folder.getPictures().intersect(pictures).isEmpty())
        folder.addPictures(pictures)
        assert(folder.getPictures().intersect(pictures).size == pictures.size)
        assert(picture in folder.getPictures())

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
        assert(folder.mRecursiveSize == 0)

        folder.addFolders(subFolders)
        assert(folder.size == subFolders.size)
        for (f: Folder in subFolders) {
            recursiveSize += f.mRecursiveSize
        }
        assert(folder.mRecursiveSize == recursiveSize)

        folder.addPictures(pictures)
        assert(folder.size == subFolders.size + pictures.size)
        assert(folder.mRecursiveSize == recursiveSize + pictures.size)

    }

}