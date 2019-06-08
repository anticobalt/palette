package iced.egret.palette.fragment

import android.os.Bundle
import android.support.v4.app.FragmentActivity
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import iced.egret.palette.R
import iced.egret.palette.adapter.PinnedCollectionsAdapter
import iced.egret.palette.model.Collection
import iced.egret.palette.util.CollectionManager

class PinnedCollectionsFragment : MainFragment() {

    private lateinit var mCollections : MutableList<Collection>
    private var mRootView : View? = null
    private lateinit var mRecyclerView : RecyclerView

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        mRootView = inflater.inflate(R.layout.fragment_pinned_collections, container, false)
        mRecyclerView = mRootView!!.findViewById(R.id.rvPinnedCollections)

        if (activity != null) {
            CollectionManager.initRootFolder(activity as FragmentActivity)
            mCollections = CollectionManager.getCollections()
            buildRecyclerView()
        }

        return mRootView

    }

    private fun buildRecyclerView() {
        if (mCollections.isNotEmpty()) {
            mRecyclerView.layoutManager = GridLayoutManager(activity, 1)
            mRecyclerView.adapter = PinnedCollectionsAdapter(mCollections)
        }
        else {
            val toast = Toast.makeText(activity, getString(R.string.alert_no_folders), Toast.LENGTH_LONG)
            toast.show()
        }
    }

    override fun onBackPressed(): Boolean {
        return false
    }

}