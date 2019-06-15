package iced.egret.palette.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.input
import iced.egret.palette.R
import iced.egret.palette.adapter.PinnedCollectionsAdapter
import iced.egret.palette.model.Album
import iced.egret.palette.model.Collection
import iced.egret.palette.util.CollectionManager

class PinnedCollectionsFragment : MainFragment() {

    private lateinit var mCollections : MutableList<Collection>
    private var mRootView : View? = null
    private lateinit var mRecyclerView : RecyclerView
    private lateinit var mToolbarItem : Toolbar

    private lateinit var mAdapter : PinnedCollectionsAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        mRootView = inflater.inflate(R.layout.fragment_pinned_collections, container, false)
        mRecyclerView = mRootView!!.findViewById(R.id.rvPinnedCollections)
        mToolbarItem = mRootView!!.findViewById(R.id.toolbarPinnedCollections)

        buildToolbar()
        buildRecyclerView()

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
        mRecyclerView.layoutManager = GridLayoutManager(activity, 1)
        mAdapter = PinnedCollectionsAdapter()
        mRecyclerView.adapter = mAdapter
    }

    override fun onBackPressed(): Boolean {
        return false
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.actionPinnedCollectionsCreateAlbum -> {
                MaterialDialog(this.context!!).show {
                    title(R.string.title_album_form)
                    input(hintRes = R.string.hint_set_name, maxLength = Album.NAME_MAX_LENGTH) {
                        _, charSequence ->
                            onCreateNewAlbum(charSequence)
                    }
                    positiveButton(R.string.action_create_album)
                    negativeButton()
                }
                true
            }
            R.id.actionPinnedCollectionsSettings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun onCreateNewAlbum(charSequence: CharSequence) {
        CollectionManager.createNewAlbum(charSequence.toString())
        mAdapter.notifyDataSetChanged()
    }

}