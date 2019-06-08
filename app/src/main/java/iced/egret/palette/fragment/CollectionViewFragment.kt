package iced.egret.palette.fragment

import android.app.Fragment
import android.os.Bundle
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import iced.egret.palette.R
import iced.egret.palette.adapter.CollectionRecyclerViewAdapter
import iced.egret.palette.model.Coverable
import iced.egret.palette.util.CollectionManager

class CollectionViewFragment : Fragment() {

    private lateinit var mContents : MutableList<Coverable>
    private var rootView : View? = null
    private lateinit var toolbarItem : android.support.v7.widget.Toolbar
    private lateinit var collectionRecyclerView : RecyclerView
    private lateinit var floatingActionButton : FloatingActionButton

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        rootView = inflater?.inflate(R.layout.fragment_view_collection, container, false)
        collectionRecyclerView = rootView!!.findViewById(R.id.collectionRecyclerView)
        floatingActionButton = rootView!!.findViewById(R.id.fab)
        toolbarItem = rootView!!.findViewById(R.id.toolbar)
        toolbarItem.setTitle(R.string.app_name)

        floatingActionButton.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }

        CollectionManager.initRootFolder(activity)
        mContents = CollectionManager.getContents()
        buildRecyclerView()

        return rootView

    }

    fun onBackPressed() : Boolean {
        return returnToParentCollection()
    }

    private fun buildRecyclerView() {
        if (mContents.isNotEmpty()) {
            collectionRecyclerView.layoutManager = GridLayoutManager(activity, 3)
            collectionRecyclerView.adapter = CollectionRecyclerViewAdapter(mContents)
        }
        else {
            val toast = Toast.makeText(activity, getString(R.string.alert_no_folders), Toast.LENGTH_LONG)
            toast.show()
        }
    }

    private fun returnToParentCollection() : Boolean {
        val newContents = CollectionManager.getParentCollectionContents()
        val success : Boolean
        success = if (newContents != null) {
            mContents.clear()
            mContents.addAll(newContents)
            collectionRecyclerView.adapter?.notifyDataSetChanged()
            true
        }
        else {
            false
        }
        return success
    }

}