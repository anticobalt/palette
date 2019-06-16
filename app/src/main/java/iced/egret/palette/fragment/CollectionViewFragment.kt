package iced.egret.palette.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.afollestad.materialdialogs.MaterialDialog
import com.afollestad.materialdialogs.input.input
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import iced.egret.palette.R
import iced.egret.palette.adapter.CollectionViewAdapter
import iced.egret.palette.model.Album
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

        mFloatingActionButton.setOnClickListener {
            onFabClick()
        }

        if (activity != null) {
            mContents = CollectionManager.contents.toMutableList()
            setToolbarTitle()
            buildRecyclerView()
        }

        return mRootView

    }

    private fun onFabClick() {
        val collection = CollectionManager.currentCollection
        if (collection is Album) {
            MaterialDialog(this.context!!).show {
                title(R.string.title_album_form)
                input(hintRes = R.string.hint_set_name, maxLength = Album.NAME_MAX_LENGTH) {
                    _, charSequence ->
                    createNewAlbum(charSequence.toString())
                }
                positiveButton(R.string.action_create_album)
                negativeButton()
            }
        }
        else {
            Snackbar.make(view!!, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }
    }

    private fun createNewAlbum(name: String) {
        CollectionManager.createNewAlbum(name, addToCurrent = true)
        adapter.update(CollectionManager.contents)
    }

    fun setToolbarTitle(title: String = "") {
        mToolbarItem.title = if (title.isEmpty()) {
            CollectionManager.currentCollection?.name
        }
        else title
    }

    private fun buildRecyclerView() {
        if (mContents.isNotEmpty()) {
            mCollectionRecyclerView.layoutManager = GridLayoutManager(activity, 3)
            adapter = CollectionViewAdapter(mContents)
            mCollectionRecyclerView.adapter = adapter
        }
    }

    override fun onBackPressed() : Boolean {
        return returnToParentCollection()
    }

    override fun onAlternateModeActivated() {}
    override fun onAlternateModeDeactivated() {}

    /**
     * If contents are updated inside CollectionManager, changes are not reflected in adapter.
     */
    private fun returnToParentCollection() : Boolean {
        val newContents = CollectionManager.revertToParent()
        return if (newContents != null){
            adapter.update(newContents)
            true
        }
        else {
            false
        }
    }

}