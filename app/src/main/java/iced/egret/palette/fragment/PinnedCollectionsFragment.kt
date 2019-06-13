package iced.egret.palette.fragment

import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.appcompat.widget.Toolbar
import android.view.LayoutInflater
import android.view.MenuItem
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
    private lateinit var mToolbarItem : Toolbar

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        mRootView = inflater.inflate(R.layout.fragment_pinned_collections, container, false)
        mRecyclerView = mRootView!!.findViewById(R.id.rvPinnedCollections)
        mToolbarItem = mRootView!!.findViewById(R.id.toolbarPinnedCollections)

        buildToolbar()

        if (activity != null) {
            CollectionManager.initRootFolder(activity as FragmentActivity)
            mCollections = CollectionManager.getCollections()
            buildRecyclerView()
        }

        return mRootView

    }

    private fun buildToolbar() {
        mToolbarItem.setTitle(R.string.app_name)
        mToolbarItem.inflateMenu(R.menu.menu_pinned_collections)
        mToolbarItem.setOnMenuItemClickListener {
            onOptionsItemSelected(it)
        }
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

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.actionPinnedCollectionsSettings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

}