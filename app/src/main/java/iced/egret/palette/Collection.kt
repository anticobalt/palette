package iced.egret.palette


abstract class Collection(val name: String) : Thumbnail {

    // Standard vars
    protected abstract var mPictures: ArrayList<Picture>
    protected abstract var mNameTag : String

    // Read-Only vars
    abstract var mCoverId: Int
        protected set
    abstract var mSize : Int
        protected set

    abstract fun getContents() : MutableList<Thumbnail>
    abstract fun getCollections() : MutableList<Collection>

    override fun getNameTag() : String {
        return mNameTag
    }

    open fun addPicture(newPicture: Picture) {
        mPictures.add(newPicture)
        mSize += 1
    }
    open fun addPictures(newPictures: MutableList<Picture>) {
        mPictures.addAll(newPictures)
        mSize += newPictures.size
    }
    open fun getPictures() : MutableList<Picture> {
        return mPictures
    }

}

class Folder(mName: String, val mPath: String, private var mFolders: MutableList<Folder> = mutableListOf()) : Collection(mName) {

    override var mCoverId = R.drawable.ic_folder_silver_24dp
    override var mNameTag = mPath
    override var mSize = mFolders.size
    override var mPictures = ArrayList<Picture>()

    var mRecursiveSize = mFolders.size
        private set

    @Suppress("UNCHECKED_CAST")
    override fun getContents(): MutableList<Thumbnail> {
        val folders = mFolders as ArrayList<Thumbnail>
        val pictures = mPictures as ArrayList<Thumbnail>
        return (folders + pictures) as ArrayList<Thumbnail>
    }

    @Suppress("UNCHECKED_CAST")
    override fun getCollections(): MutableList<Collection> {
        return getFolders() as MutableList<Collection>
    }

    fun isEmpty() : Boolean {
        return (mFolders.size + mPictures.size) == 0
    }

    fun isNotEmpty() : Boolean {
        return !isEmpty()
    }


    fun addFolder(newFolder: Folder) {
        mFolders.add(newFolder)
        mSize += 1
        mRecursiveSize += newFolder.mRecursiveSize
    }

    fun addFolders(newFolders: MutableList<Folder>) {
        mFolders.addAll(newFolders)
        mSize += newFolders.size
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

}
class Album(name: String) : Collection(name) {
    override var mCoverId = R.drawable.ic_folder_silver_24dp
    override var mNameTag = name
    override var mSize = 0
    override var mPictures = ArrayList<Picture>()
    override fun getContents(): MutableList<Thumbnail> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun getCollections(): MutableList<Collection> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

class SmartAlbum(name: String, private val folders: MutableList<Folder> = mutableListOf()) : Collection(name) {
    override var mCoverId = R.drawable.ic_folder_silver_24dp
    override var mNameTag = name
    override var mSize = folders.size
    override var mPictures = ArrayList<Picture>()
    override fun getContents(): MutableList<Thumbnail> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
    override fun getCollections(): MutableList<Collection> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

