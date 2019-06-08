package iced.egret.palette.model

import android.net.Uri
import android.view.View
import com.bumptech.glide.Glide
import iced.egret.palette.R
import iced.egret.palette.adapter.CoverViewHolder
import java.io.Serializable


abstract class Collection(override var name: String) : Coverable {

    override val terminal = false
    override val cover = mutableMapOf<String, Any>(
            "id" to R.drawable.ic_folder_silver_64dp
    )

    // Standard vars
    protected abstract var mPictures: ArrayList<Picture>

    // Read-Only vars
    abstract var size : Int
        protected set
    abstract var parent : Collection?

    override fun loadCoverInto(holder: CoverViewHolder) {
        if (holder.ivItem != null) {
            setUri()
            Glide.with(holder.itemView.context)
                    .load(cover["uri"])
                    .centerCrop()
                    .into(holder.ivItem)
        }
        if (holder.tvItem != null) {
            holder.tvItem.visibility = View.VISIBLE
        }
    }

    abstract fun setUri()
    abstract fun getContents() : MutableList<Coverable>
    abstract fun getCollections() : MutableList<Collection>

    open fun getPictures() : MutableList<Picture> {
        return mPictures
    }

    open fun addPicture(newPicture: Picture) {
        mPictures.add(newPicture)
        size += 1
    }
    open fun addPictures(newPictures: MutableList<Picture>) {
        mPictures.addAll(newPictures)
        size += newPictures.size
    }

}

class Folder(name: String,
             val path: String,
             subFolders: MutableList<Folder> = mutableListOf(),
             override var parent: Collection? = null)
    : Collection(name) {

    private var mFolders = subFolders

    override var size = mFolders.size
    override var mPictures = ArrayList<Picture>()

    var mRecursiveSize = mFolders.size
        private set

    override fun toString(): String {
        return "$path, $size"
    }

    fun isEmpty() : Boolean {
        return (mFolders.size + mPictures.size) == 0
    }

    fun isNotEmpty() : Boolean {
        return !isEmpty()
    }

    fun toDataClass() : FolderData {
        val picturePaths = mPictures.map { picture -> picture.path }
        val subFolders = mFolders.map {folder -> folder.toDataClass() }
        return FolderData(name, path, subFolders, picturePaths)
    }

    override fun setUri() {
        val uri = getOnePictureUri()
        if (uri != null) {
            cover["uri"] = uri
        }
    }

    private fun getOnePictureUri() : Uri? {
        if (mPictures.isNotEmpty()) {
            return mPictures[0].uri
        } else if (mFolders.isNotEmpty()) {
            return mFolders[0].getOnePictureUri()
        }
        return null
    }

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

    fun getFolders() : MutableList<Folder> {
        return mFolders
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

    override fun addPicture(newPicture: Picture) {
        super.addPicture(newPicture)
        mRecursiveSize += 1
    }

    override fun addPictures(newPictures: MutableList<Picture>) {
        super.addPictures(newPictures)
        mRecursiveSize += newPictures.size
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


class Album(name: String, override var parent: Collection?) : Collection(name) {
    override fun setUri() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override var size = 0
    override var mPictures = ArrayList<Picture>()
    override fun getContents(): MutableList<Coverable> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
    override fun getCollections(): MutableList<Collection> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

class SmartAlbum(name: String, folders : MutableList<Folder> = mutableListOf(), override var parent: Collection?) : Collection(name) {
    override fun setUri() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

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

