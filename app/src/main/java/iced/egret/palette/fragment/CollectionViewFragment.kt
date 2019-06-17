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
import iced.egret.palette.recyclerview_component.LongClickSelector
import iced.egret.palette.util.CollectionManager
import iced.egret.palette.util.Painter

class CollectionViewFragment : MainFragment() {

    private var mRootView : View? = null
    private lateinit var mDefaultToolbar : Toolbar
    private lateinit var mEditToolbar : Toolbar
    private lateinit var mCollectionRecyclerView : RecyclerView
    private lateinit var mFloatingActionButton : FloatingActionButton

    private lateinit var mContents : MutableList<Coverable>
    private lateinit var mSelector: LongClickSelector

    lateinit var adapter: CollectionViewAdapter
        private set

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        mRootView = inflater.inflate(R.layout.fragment_view_collection, container, false)
        mCollectionRecyclerView = mRootView!!.findViewById(R.id.rvCollectionItems)
        mFloatingActionButton = mRootView!!.findViewById(R.id.fab)

        mContents = CollectionManager.contents.toMutableList()
        mSelector = LongClickSelector(this)

        mFloatingActionButton.setOnClickListener {
            onFabClick()
        }

        buildToolbars()
        buildRecyclerView()

        return mRootView

    }

    /**
     * Adds new Coverables to current Collection
     */
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

    /**
     * Adds new album to current Collection
     */
    private fun createNewAlbum(name: String) {
        CollectionManager.createNewAlbum(name, addToCurrent = true)
        adapter.update(CollectionManager.contents)
    }

    /**
     * Makes default and edit toolbars and fills with items and titles
     */
    private fun buildToolbars() {

        mDefaultToolbar = mRootView!!.findViewById(R.id.toolbarViewCollection)
        mEditToolbar = mRootView!!.findViewById(R.id.toolbarViewCollectionEdit)

        mDefaultToolbar.setTitle(R.string.app_name)
        mDefaultToolbar.inflateMenu(R.menu.menu_view_collection)
        mDefaultToolbar.setOnMenuItemClickListener {
            onOptionsItemSelected(it)
        }

        mEditToolbar.setTitle(R.string.app_name)
        mEditToolbar.inflateMenu(R.menu.menu_view_collection_edit)
        mEditToolbar.setOnMenuItemClickListener {
            onOptionsItemSelected(it)
        }
        val back = activity?.getDrawable(R.drawable.ic_chevron_left_black_24dp)
        Painter.paintDrawable(back)
        mEditToolbar.navigationIcon = back
        mEditToolbar.setNavigationOnClickListener {
            onBackPressed()
        }

    }

    /**
     * Sets default toolbar's title to current Collection name
     */
    fun setDefaultToolbarTitle(title: String = "") {
        mDefaultToolbar.title = if (title.isEmpty()) {
            CollectionManager.currentCollection?.name
        }
        else title
    }

    /**
     * Hooks up adapter and LayoutManager to RecyclerView
     */
    private fun buildRecyclerView() {
        if (mContents.isNotEmpty()) {
            mCollectionRecyclerView.layoutManager = GridLayoutManager(activity, 3)
            adapter = CollectionViewAdapter(mContents, mSelector)
            mCollectionRecyclerView.adapter = adapter
        }
    }

    /**
     * @return handled here (true) or not (false)
     */
    override fun onBackPressed() : Boolean {
        val handledBySelector = mSelector.onBackPressed()
        return if (!handledBySelector) {
            returnToParentCollection()
        }
        else true
    }

    /**
     * Called by LongClickSelector upon its activation
     */
    override fun onAlternateModeActivated() {
        mDefaultToolbar.visibility = Toolbar.GONE
        mEditToolbar.visibility = Toolbar.VISIBLE
        adapter.notifyDataSetChanged()  // signal style changes
    }

    /**
     * Called by LongClickSelector after it finishes cleaning up
     */
    override fun onAlternateModeDeactivated() {
        mEditToolbar.visibility = Toolbar.GONE
        mDefaultToolbar.visibility = Toolbar.VISIBLE
        adapter.notifyDataSetChanged()  // signal that selections were cleared
    }

    /**
     * Decide if parent exists and can be returned to
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