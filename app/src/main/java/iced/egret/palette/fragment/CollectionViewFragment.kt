package iced.egret.palette.fragment

import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v4.app.FragmentActivity
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import iced.egret.palette.R
import iced.egret.palette.adapter.CollectionViewAdapter
import iced.egret.palette.model.Coverable
import iced.egret.palette.util.CollectionManager

class CollectionViewFragment : MainFragment() {

    private lateinit var mContents : MutableList<Coverable>
    private var mRootView : View? = null
    private lateinit var mToolbarItem : Toolbar
    private lateinit var mCollectionRecyclerView : RecyclerView
    private lateinit var mFloatingActionButton : FloatingActionButton

    lateinit var adapter: CollectionViewAdapter
        private set

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        mRootView = inflater.inflate(R.layout.fragment_view_collection, container, false)
        mCollectionRecyclerView = mRootView!!.findViewById(R.id.rvCollectionItems)
        mFloatingActionButton = mRootView!!.findViewById(R.id.fab)
        mToolbarItem = mRootView!!.findViewById(R.id.toolbarViewCollection)

        mFloatingActionButton.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }

        if (activity != null) {
            CollectionManager.initRootFolder(activity as FragmentActivity)
            mContents = CollectionManager.getContents()
            mToolbarItem.title = CollectionManager.getCurrentCollectionName()
            buildRecyclerView()
        }

        return mRootView

    }

    private fun buildRecyclerView() {
        if (mContents.isNotEmpty()) {
            mCollectionRecyclerView.layoutManager = GridLayoutManager(activity, 3)
            adapter = CollectionViewAdapter()
            mCollectionRecyclerView.adapter = adapter
        }
        else {
            val toast = Toast.makeText(activity, getString(R.string.alert_no_folders), Toast.LENGTH_LONG)
            toast.show()
        }
    }

    override fun onBackPressed() : Boolean {
        return returnToParentCollection()
    }

    /**
     * If contents are updated inside CollectionManager, changes are not reflected in adapter.
     */
    private fun returnToParentCollection() : Boolean {
        val newContents = CollectionManager.revertToParent()
        return if (newContents != null){
            mContents.clear()
            mContents.addAll(newContents)
            mCollectionRecyclerView.adapter?.notifyDataSetChanged()
            true
        }
        else {
            false
        }
    }

}