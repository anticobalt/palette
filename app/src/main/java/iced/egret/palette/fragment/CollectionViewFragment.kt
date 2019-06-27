package iced.egret.palette.fragment

import android.os.Bundle
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.Visibility
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.SelectableAdapter
import eu.davidea.flexibleadapter.helpers.ActionModeHelper
import iced.egret.palette.R
import iced.egret.palette.adapter.CollectionViewAdapter
import iced.egret.palette.model.Album
import iced.egret.palette.model.Coverable
import iced.egret.palette.recyclerview_component.CollectionViewItem
import iced.egret.palette.recyclerview_component.LongClickSelector
import iced.egret.palette.section.CollectionViewSection
import iced.egret.palette.util.CollectionManager
import iced.egret.palette.util.DialogGenerator
import iced.egret.palette.util.MainFragmentManager
import iced.egret.palette.util.Painter
import io.github.luizgrp.sectionedrecyclerviewadapter.StatelessSection


class CollectionViewFragment :
        MainFragment(),
        ActionMode.Callback,
        FlexibleAdapter.OnItemClickListener,
        FlexibleAdapter.OnItemLongClickListener {

    private lateinit var mActionModeHelper: ActionModeHelper

    private var mRootView : View? = null
    private lateinit var mDefaultToolbar : Toolbar
    private lateinit var mEditToolbar : Toolbar
    private lateinit var mCollectionRecyclerView : RecyclerView
    private lateinit var mFloatingActionButton : FloatingActionButton

    // these two lists are parallel
    private val mSelectors = mutableListOf<LongClickSelector>()
    private val mSections = mutableListOf<CollectionViewSection>()

    // relies on mSelectors and mSections being parallel
    private val mActiveSelector: LongClickSelector?
        get() = mSelectors.find { s -> s.active }
    private val mActiveSection: CollectionViewSection
        get() = mSections[mSelectors.indexOf(mActiveSelector)]

    private var mContents = mutableListOf<Coverable>()
    private var mContentItems = mutableListOf<CollectionViewItem>()

    private lateinit var mAdapter: CollectionViewAdapter
    private lateinit var mAdapterNew: FlexibleAdapter<CollectionViewItem>

    /**
     * Straight from https://github.com/davideas/FlexibleAdapter/wiki/5.x-%7C-ActionModeHelper
     */
    private fun initializeActionModeHelper(@Visibility.Mode mode: Int) {
        //this = ActionMode.Callback instance
        mActionModeHelper = object : ActionModeHelper(mAdapterNew, R.menu.menu_view_collection_edit, this as ActionMode.Callback) {
            // Override to customize the title
            override fun updateContextTitle(count: Int) {
                // You can use the internal mActionMode instance
                if (mActionMode != null) {
                    mActionMode.title = if (count == 1)
                        getString(R.string.action_selected_one, count)
                    else
                        getString(R.string.action_selected_many, count)
                }
            }
        }.withDefaultMode(mode)
    }

    override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
        return true
    }

    override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        return true
    }

    override fun onDestroyActionMode(mode: ActionMode?) {}

    override fun onItemClick(view: View, position: Int): Boolean {
        return if (mAdapterNew.mode != SelectableAdapter.Mode.IDLE) {
            mActionModeHelper.onClick(position)
        }
        else {
            val coverable = mContents[position]
            val updates = CollectionManager.launch(coverable, position = position, c = this.context)
            if (updates) {
                fetchContents()
                mAdapterNew.updateDataSet(mContentItems)
            }
            false
        }
    }

    override fun onItemLongClick(position: Int) {
        mActionModeHelper.onLongClick(this.activity as AppCompatActivity, position)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        mRootView = inflater.inflate(R.layout.fragment_view_collection, container, false)
        mCollectionRecyclerView = mRootView!!.findViewById(R.id.rvCollectionItems)
        mFloatingActionButton = mRootView!!.findViewById(R.id.fab)

        fetchContents()

        mFloatingActionButton.setOnClickListener {
            onFabClick()
        }

        buildToolbars()
        buildRecyclerView()

        return mRootView

    }

    /**
     * Makes default and edit toolbars and fills with items and titles
     */
    private fun buildToolbars() {

        mDefaultToolbar = mRootView!!.findViewById(R.id.toolbarViewCollection)
        mEditToolbar = mRootView!!.findViewById(R.id.toolbarViewCollectionEdit)

        mDefaultToolbar.title = CollectionManager.currentCollection?.name
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

            val manager = GridLayoutManager(activity, 3)

            /*
            val contentsMap = CollectionManager.getContentsMap()
            for ((type, coverables) in contentsMap) {
                val section = CollectionViewSection(type.capitalize(), coverables, mAdapter, this)
                mSections.add(section)
                mSelectors.add(section.selector)
                mAdapter.addSection(section)
            }
            */

            mAdapterNew = FlexibleAdapter(mContentItems)

            mCollectionRecyclerView.layoutManager = manager
            mCollectionRecyclerView.adapter = mAdapterNew

            initializeActionModeHelper(SelectableAdapter.Mode.IDLE)

            mAdapterNew.addListener(this)

        }
    }

    /**
     * Adds new Coverables to current Collection
     */
    private fun onFabClick() {
        val collection = CollectionManager.currentCollection
        if (collection is Album) {
            DialogGenerator.createAlbum(context!!, ::createNewAlbum)
        }
        else {
            Snackbar.make(view!!, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        // Get selected items, if applicable
        var coverables = listOf<Coverable>()
        val typePlural = mActiveSection.title.toLowerCase()
        val typeSingular = typePlural.dropLast(1)  // only works on s-appending plurals
        var typeString = typePlural  // temp so that it compiles
        when (item.itemId) {
            R.id.actionViewCollectionAddToAlbum,
            R.id.actionViewCollectionRemoveFromAlbum,
            R.id.actionViewCollectionDelete -> {
                // null if no active selector, which should never happen
                coverables = getSelectedCoverables() ?: return false
                typeString = if (coverables.size > 1) typePlural else typeSingular
            }
        }

        // reaching here assumes (if applicable) active selector exists and coverables is not empty
        return when (item.itemId) {
            R.id.actionViewCollectionAddToAlbum -> {
                DialogGenerator.addToAlbum(context!!) { indices, albums ->
                    val destinations = albums.filterIndexed { index, _ -> indices.contains(index) }
                    val albumString = if (destinations.size > 1) "albums" else "album"
                    CollectionManager.addContentToAllAlbums(coverables, destinations)
                    Toast.makeText(
                            context,
                            "Added ${coverables.size} $typeString to ${destinations.size} $albumString.",
                            Toast.LENGTH_SHORT
                    ).show()
                    mActiveSelector!!.deactivate()
                    MainFragmentManager.notifyAlbumUpdateFromCollectionView()
                }
                true
            }
            R.id.actionViewCollectionRemoveFromAlbum -> {
                DialogGenerator.removeFromAlbum(context!!, typeString) {
                    CollectionManager.removeContentFromCurrentAlbum(coverables)
                    Toast.makeText(
                            context,
                            "Removed ${coverables.size} $typeString.",
                            Toast.LENGTH_SHORT
                    ).show()
                    mActiveSelector!!.deactivate()
                    mAdapter.update()
                    MainFragmentManager.notifyAlbumUpdateFromCollectionView()
                }
                true
            }
            R.id.actionViewCollectionDelete -> {
                true
            }
            R.id.actionViewCollectionSettings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    /**
     * Adds new album to current Collection
     */
    private fun createNewAlbum(name: CharSequence) {
        val pos = CollectionManager.createNewAlbum(name.toString(), addToCurrent = true)
        mAdapter.updateNewAlbum(listOf(pos))
    }

    /**
     * Gets all selected items.
     */
    private fun getSelectedCoverables() : List<Coverable>? {
        val positions = mActiveSelector?.selectedItemIds?.toSet() ?: return null
        return mActiveSection.items.filterIndexed { index, _ -> positions.contains(index.toLong()) }
    }

    /**
     * @return handled here (true) or not (false)
     */
    override fun onBackPressed() : Boolean {

        var handledBySelector = false

        /**
         * Try to find one selector to handle
         */
        for (selector in mSelectors) {
            handledBySelector = handledBySelector || selector.onBackPressed()
            if (handledBySelector) return true
        }

        return returnToParentCollection()
    }

    /**
     * Called by LongClickSelector upon its activation
     */
    override fun onAlternateModeActivated(section: StatelessSection) {
        mDefaultToolbar.visibility = Toolbar.GONE
        mEditToolbar.visibility = Toolbar.VISIBLE

        section as CollectionViewSection  // cast
        val editMenu = mEditToolbar.menu
        when (section.title.toLowerCase()) {
            "folders", "pictures" -> {
                editMenu.findItem(R.id.actionViewCollectionAlbumActions).isVisible = true
                editMenu.findItem(R.id.actionViewCollectionAddToAlbum).isVisible = true
                editMenu.findItem(R.id.actionViewCollectionDelete).isVisible = true
                if (CollectionManager.currentCollection is Album) {
                    editMenu.findItem(R.id.actionViewCollectionRemoveFromAlbum).isVisible = true
                }
            }
            "albums" -> {
                editMenu.findItem(R.id.actionViewCollectionDelete).isVisible = true
            }
        }
    }

    /**
     * Called by LongClickSelector after it finishes cleaning up
     */
    override fun onAlternateModeDeactivated(section: StatelessSection) {
        mEditToolbar.visibility = Toolbar.GONE
        mDefaultToolbar.visibility = Toolbar.VISIBLE

        val editMenu = mEditToolbar.menu
        editMenu.findItem(R.id.actionViewCollectionAlbumActions).isVisible = false
        editMenu.findItem(R.id.actionViewCollectionAddToAlbum).isVisible = false
        editMenu.findItem(R.id.actionViewCollectionRemoveFromAlbum).isVisible = false
        editMenu.findItem(R.id.actionViewCollectionDelete).isVisible = false

        // already notifies adapter, so don't do it again to reset views
        mAdapter.showAllSections()
    }

    /**
     * Decide if parent exists and can be returned to
     */
    private fun returnToParentCollection() : Boolean {
        val newContents = CollectionManager.revertToParent()
        return if (newContents != null){
            mAdapter.update()
            true
        }
        else {
            false
        }
    }

    /**
     * Update everything to reflect changes
     */
    fun notifyChanges() {
        mAdapter.update()
    }

    private fun fetchContents() {
        mContents.clear()
        mContentItems.clear()
        mContents.addAll(CollectionManager.contents.toMutableList())
        mContents.map {content -> mContentItems.add(CollectionViewItem(content))}
    }

}