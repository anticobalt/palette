package iced.egret.palette


abstract class Collection(override val name: String) : Coverable {

    override val terminal: Boolean = false
    override val coverId: Int = R.drawable.ic_folder_silver_24dp

    // Standard vars
    protected abstract var mPictures: ArrayList<Picture>

    // Read-Only vars
    abstract var size : Int
        protected set

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

    override var coverId = R.drawable.ic_folder_silver_24dp
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

}
class Album(name: String) : Collection(name) {
    override var coverId = R.drawable.ic_folder_silver_24dp
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
    override var coverId = R.drawable.ic_folder_silver_24dp
    override var size = mFolders.size
    override var mPictures = ArrayList<Picture>()
    override fun getContents(): MutableList<Coverable> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
    override fun getCollections(): MutableList<Collection> {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}

