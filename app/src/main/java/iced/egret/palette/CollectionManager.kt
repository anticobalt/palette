package iced.egret.palette

import android.os.Environment

object CollectionManager {

    lateinit var mCollections: MutableList<out Collection>  // allow Collection or its subtypes

    /**
     * Scan device for folders with images, track all folders, then return them.
     */
    fun fetchRootSubFolders() : MutableList<Folder>{

        val ignore = arrayListOf("android", "music", "movies")
        val root : Folder? = Storage.getPictureFolder(Environment.getExternalStorageDirectory(), ignore)
        val ret : MutableList<Folder>

        if (root != null) {
            ret = root.getFolders()
        }
        else {
            ret = ArrayList()
        }

        mCollections = ret
        return ret

    }

    fun getCollectionByPosition(position: Int) : Collection {
        return mCollections[position]
    }

    /**
     * Track new Collection, given a parent Collection's ID.
     */
    fun trackNewCollectionsFromNameTag(nameTag : String){
        val taggedCollection : Collection? = mCollections.find { collection -> collection.mNameTag == nameTag }
        if (taggedCollection == null) {
            mCollections = ArrayList()
        }
        else {
            @Suppress("UNCHECKED_CAST")
            mCollections = taggedCollection.getContents().filter {content -> content is Collection} as MutableList<out Collection>
        }
    }

}