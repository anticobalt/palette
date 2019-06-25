package iced.egret.palette.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import iced.egret.palette.R
import iced.egret.palette.adapter.CollectionViewAdapter
import iced.egret.palette.model.Album
import iced.egret.palette.model.Coverable
import iced.egret.palette.recyclerview_component.AnimatedGridLayoutManager
import iced.egret.palette.recyclerview_component.GridSectionSpanSizeLookup
import iced.egret.palette.recyclerview_component.LongClickSelector
import iced.egret.palette.section.CollectionViewSection
import iced.egret.palette.util.CollectionManager
import iced.egret.palette.util.DialogGenerator
import iced.egret.palette.util.MainFragmentManager
import iced.egret.palette.util.Painter
import io.github.luizgrp.sectionedrecyclerviewadapter.StatelessSection
import jp.wasabeef.recyclerview.animators.FadeInAnimator

class CollectionViewFragment : MainFragment() {

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

    private lateinit var mContents : MutableList<Coverable>

    private lateinit var mAdapter: CollectionViewAdapter

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        mRootView = inflater.inflate(R.layout.fragment_view_collection, container, false)
        mCollectionRecyclerView = mRootView!!.findViewById(R.id.rvCollectionItems)
        mFloatingActionButton = mRootView!!.findViewById(R.id.fab)

        mContents = CollectionManager.contents.toMutableList()

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

            val manager = AnimatedGridLayoutManager(activity!!, 3)
            mAdapter = CollectionViewAdapter(mContents, mCollectionRecyclerView)

            manager.spanSizeLookup = GridSectionSpanSizeLookup(mAdapter, 3)

            val contentsMap = CollectionManager.getContentsMap()
            for ((type, coverables) in contentsMap) {
                val section = CollectionViewSection(type.capitalize(), coverables, mAdapter, this)
                mSections.add(section)
                mSelectors.add(section.selector)
                mAdapter.addSection(section)
            }

            mCollectionRecyclerView.layoutManager = manager
            mCollectionRecyclerView.adapter = mAdapter
            mCollectionRecyclerView.itemAnimator = FadeInAnimator()

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

        // already does notifyDataSetChanged(), so don't call it again to reset views
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

}