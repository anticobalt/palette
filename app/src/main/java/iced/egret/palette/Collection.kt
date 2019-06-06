package iced.egret.palette

import android.content.Context
import android.widget.ImageView
import com.bumptech.glide.Glide
import java.io.Serializable


abstract class Collection(override val name: String) : Coverable {

    override val terminal = false
    override val cover = mutableMapOf(
            "id" to R.drawable.ic_folder_silver_24dp
    )

    // Standard vars
    protected abstract var mPictures: ArrayList<Picture>

    // Read-Only vars
    abstract var size : Int
        protected set

    override fun loadCoverInto(imageView: ImageView?, context: Context) {
        if (imageView != null) {
            Glide.with(context).load(cover["id"]).into(imageView)
        }
    }

    abstract fun getContents() : MutableList<Coverable>
    abstract fun getCollections() : MutableList<Collection>

    open fun addPicture(newPicture: Picture) {
        mPictures.add(newPicture)
        size += 1
    }
    open fun addPictures(newPictures: MutableList<Picture>) {
        mPictures.addAll(newPictures)
        size += newPictures.size
    }
    open fun getPictures() : MutableList<Picture> {
        return mPictures
    }

}

class Folder(name: String, val path: String, subFolders: MutableList<Folder> = mutableListOf()) : Collection(name) {

    private var mFolders = subFolders

    override var size = mFolders.size
    override var mPictures = ArrayList<Picture>()

    var mRecursiveSize = mFolders.size
        private set

    @Suppress("UNCHECKED_CAST")
    override fun getContents(): MutableList<Coverable> {
        val folders = mFolders as ArrayList<Coverable>
        val pictures = mPictures as ArrayList<Coverable>
        return (folders + pictures) as MutableList<Coverable>
    }

    @Suppress("UNCHECKED_CAST")
    override fun getCollections(): MutableList<Collection> {
        return getFolders() as MutableList<Collection>
    }

    override fun toString(): String {
        return "$path, $size"
    }

    fun isEmpty() : Boolean {
        return (mFolders.size + mPictures.size) == 0
    }

    fun isNotEmpty() : Boolean {
        return !isEmpty()
    }

    fun addFolder(newFolder: Folder) {
        mFolders.add(newFolder)
        size += 1
        mRecursiveSize += newFolder.mRecursiveSize
    }

    fun addFolders(newFolders: MutableList<Folder>) {
        mFolders.addAll(newFolders)
        size += newFolders.size
        for (newFolder: Folder in newFolders) {
            mRecursiveSize += newFolder.mRecursiveSize
        }
    }

    fun getFolders() : MutableList<Folder> {
        return mFolders
    }

    override fun addPicture(newPicture: Picture) {
        super.addPicture(newPicture)
        mRecursiveSize += 1
    }

    override fun addPictures(newPictures: MutableList<Picture>) {
        super.addPictures(newPictures)
        mRecursiveSize += newPictures.size
    }

    fun toDataClass() : SerializedFolder {
        val picturePaths = mPictures.map { picture -> picture.path }
        val subFolders = mFolders.map {folder -> folder.toDataClass() }
        return SerializedFolder(name, path, subFolders, picturePaths)
    }

}

class SerializedFolder(val name: String,
                       val path: String,
                       val subFolders: List<SerializedFolder> = mutableListOf(),
                       val picturePaths : List<String> = mutableListOf()) : Serializable {

    fun toFullClass() : Folder {
        val fullSubFolders = subFolders.map {folder -> folder.toFullClass() } as MutableList<Folder>
        val pictures = picturePaths.map {path -> Picture(path.split("/").last(), path)} as MutableList<Picture>
        val folder = Folder(name, path, fullSubFolders)
        folder.addPictures(pictures)
        return folder
    }

}


class Album(name: String) : Collection(name) {
    override var size = 0
    override var mPictures = ArrayList<Picture>()
    override fun getContents(): MutableList<Coverable> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
    override fun getCollections(): MutableList<Collection> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

class SmartAlbum(name: String, folders : MutableList<Folder> = mutableListOf()) : Collection(name) {
    private var mFolders = folders
    override var size = mFolders.size
    override var mPictures = ArrayList<Picture>()
    override fun getContents(): MutableList<Coverable> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
    override fun getCollections(): MutableList<Collection> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

