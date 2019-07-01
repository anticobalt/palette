package iced.egret.palette.fragment

import android.os.Bundle
import android.view.*
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.slidingpanelayout.widget.SlidingPaneLayout
import androidx.transition.Visibility
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.SelectableAdapter
import eu.davidea.flexibleadapter.items.IFlexible
import iced.egret.palette.R
import iced.egret.palette.activity.MainActivity
import iced.egret.palette.adapter.PinnedCollectionsAdapter
import iced.egret.palette.model.Collection
import iced.egret.palette.recyclerview_component.*
import iced.egret.palette.section.PinnedCollectionsSection
import iced.egret.palette.util.CollectionManager
import iced.egret.palette.util.DialogGenerator
import iced.egret.palette.util.MainFragmentManager
import io.github.luizgrp.sectionedrecyclerviewadapter.StatelessSection
import kotlinx.android.synthetic.main.fragment_pinned_collections.*

class PinnedCollectionsFragment :
        MainFragment(),
        ActionMode.Callback,
        FlexibleAdapter.OnItemClickListener,
        FlexibleAdapter.OnItemLongClickListener {

    companion object SaveDataKeys {
        const val selectedHeaderPosition = "PinnedCollectionFragment_SHP"
    }

    private var mRootView : View? = null
    private lateinit var mRecyclerView : RecyclerView

    private lateinit var mDefaultToolbar : Toolbar
    private lateinit var mEditToolbar : Toolbar

    private lateinit var mAdapter : PinnedCollectionsAdapter
    private var mSelectors = mutableListOf<LongClickSelector>()

    private var mCollections = mutableListOf<Collection>()
    private var mCollectionItems = mutableListOf<PinnedCollectionsItem>()
    private var mHeaders = mutableListOf<SectionHeaderItem>()
    lateinit var adapter: FlexibleAdapter<SectionHeaderItem>
    private lateinit var mActionModeHelper: ToolbarActionModeHelper
    private var mBlockableFragments = mutableListOf<MainFragment>()
    private var selectedSectionHeader : SectionHeaderItem? = null


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mRootView = inflater.inflate(R.layout.fragment_pinned_collections, container, false)
        mRecyclerView = mRootView!!.findViewById(R.id.rvPinnedCollections)
        buildToolbar()
        buildRecyclerView()
        initializeActionModeHelper(SelectableAdapter.Mode.IDLE)
        return mRootView
    }

    override fun onSaveInstanceState(outState: Bundle) {
        adapter.onSaveInstanceState(outState)
        outState.putInt(selectedHeaderPosition, mHeaders.indexOf(selectedSectionHeader))
        super.onSaveInstanceState(outState)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)

        if (savedInstanceState != null) {
            // If selected section saved, restore it, otherwise get out
            val selectedSectionPosition = savedInstanceState.getInt(selectedHeaderPosition, -1)
            if (selectedSectionPosition == -1) return
            selectedSectionHeader = mHeaders[selectedSectionPosition]
            isolateSection(selectedSectionHeader!!)

            // Must restore adapter and helper AFTER section isolation to keep position ints consistent
            adapter.onRestoreInstanceState(savedInstanceState)
            mActionModeHelper.restoreSelection(mDefaultToolbar)

            // Re-select all previously selected items
            for (pos in adapter.selectedPositions) {
                mCollectionItems[getCardinalPosition(pos)].setSelection(true)
            }
        }
    }

    override fun onFragmentCreationFinished(fragment: MainFragment) {
        if (mActionModeHelper.getActionMode() != null) {
            (fragment).setClicksBlocked(true)
        }
        mBlockableFragments.add(fragment)
    }

    private fun buildToolbar() {

        mDefaultToolbar = mRootView!!.findViewById(R.id.toolbarPinnedCollections)
        mEditToolbar = mRootView!!.findViewById(R.id.toolbarEditPinnedCollections)

        mDefaultToolbar.setTitle(R.string.app_name)
        mDefaultToolbar.inflateMenu(R.menu.menu_pinned_collections)
        mDefaultToolbar.setOnMenuItemClickListener {
            onOptionsItemSelected(it)
        }

    }

    private fun buildRecyclerView() {
        fetchContents()
        mRecyclerView.layoutManager = GridLayoutManager(activity, 1)
        adapter = FlexibleAdapter(mHeaders, this, true).expandItemsAtStartUp()
        mRecyclerView.adapter = adapter
    }

    /**
     * Straight from https://github.com/davideas/FlexibleAdapter/wiki/5.x-%7C-ActionModeHelper
     */
    private fun initializeActionModeHelper(@Visibility.Mode mode: Int) {
        //this = ActionMode.Callback instance
        mActionModeHelper = object : ToolbarActionModeHelper(adapter, R.menu.menu_pinned_collections_edit, this as ActionMode.Callback) {
            // Override to customize the title
            override fun updateContextTitle(count: Int) {
                // You can use the internal mActionMode instance
                mActionMode?.title = if (count == 1)
                    getString(R.string.action_selected_one, count)
                else
                    getString(R.string.action_selected_many, count)
            }
        }.withDefaultMode(mode)
    }

    override fun onActionItemClicked(p0: ActionMode, p1: MenuItem): Boolean {
        return true
    }

    override fun onCreateActionMode(p0: ActionMode, p1: Menu): Boolean {
        // Will re-block some fragments, depending on the order they were built in.
        for (fragment in mBlockableFragments) {
            fragment.setClicksBlocked(true)
        }
        return true
    }

    override fun onPrepareActionMode(p0: ActionMode, p1: Menu): Boolean {
        return true
    }

    override fun onDestroyActionMode(p0: ActionMode) {
        // ToolbarActionModeHelper doesn't have references to CoverableItems,
        // so can't clear all selections visually
        mCollectionItems.map { item -> item.setSelection(false)}
        restoreAllSections()
        selectedSectionHeader = null  // no selection active
        (activity as MainActivity).restoreAllFragments()
    }

    override fun onItemClick(view: View, absolutePosition: Int): Boolean {
        val clickedItem = adapter.getItem(absolutePosition) as IFlexible<*>

        if (clickedItem !is CoverableItem) return false

        val cardinalPosition = getCardinalPosition(absolutePosition)
        return if (adapter.mode != SelectableAdapter.Mode.IDLE) {
            mActionModeHelper.onClick(absolutePosition, mCollectionItems[cardinalPosition])
        }
        else {
            val fragmentIndex = MainFragmentManager.COLLECTION_CONTENTS
            val fragment = MainFragmentManager.fragments[fragmentIndex] as CollectionViewFragment
            fragment.activity?.findViewById<SlidingPaneLayout>(R.id.slidingPaneMain)?.closePane()

            val coverable = mCollections[cardinalPosition]
            CollectionManager.clearStack()
            CollectionManager.launch(coverable)  // == true
            fragment.setDefaultToolbarTitle()
            fragment.refreshData()
            false
        }
    }

    /**
     * @param absolutePosition the actual position amongst all on-screen items
     * cardinalPosition is absolutePosition while ignoring headers
     * relativePosition is absolutePosition while ignoring all other sections (but keeping own header)
     *
     * All positions refer to arrangement before click handling.
     */
    override fun onItemLongClick(absolutePosition: Int) {
        val cardinalPosition = getCardinalPosition(absolutePosition)
        val relativePosition : Int

        // Isolate the section BEFORE ActionMode is created, so that the correct
        // section position can be noted by the ActionModeHelper (instead of global position,
        // which unnecessarily accounts for temporarily remove sections)
        if (selectedSectionHeader == null) {
            selectedSectionHeader = adapter.getSectionHeader(absolutePosition) as SectionHeaderItem
            isolateSection(selectedSectionHeader!!)
            // adapter only has one section now, so global == relative
            relativePosition = adapter.getGlobalPositionOf(mCollectionItems[cardinalPosition])
        }
        else {
            relativePosition = absolutePosition
        }
        mActionModeHelper.onLongClick(mDefaultToolbar, relativePosition, mCollectionItems[cardinalPosition])
    }

    private fun isolateSection(toIsolateHeader: SectionHeaderItem) {
        for (header in mHeaders) {
            if (header != toIsolateHeader) {
                // adapter.removeSection() does not work,
                // because it collapses/removes the section's items,
                // and then deletes section_items.size's worth of
                // items AGAIN.
                val position = adapter.getGlobalPositionOf(header)
                adapter.collapse(position)
                adapter.removeItem(position)
            }
        }
    }

    private fun restoreAllSections() {
        for (header in mHeaders) {
            if (!adapter.contains(header)) {
                adapter.addSection(header, CollectionManager.SectionComparator)
                adapter.expand(header, true) // skipping init=true causes duplication
            }
        }
    }

    /**
     * Given it's absolute position, get item's position ignoring headers.
     * FlexibleAdapter's doesn't want to work, so making my own.
     */
    private fun getCardinalPosition(position: Int) : Int {
        val header = adapter.getSectionHeader(position)
        val headerPosition = adapter.headerItems.indexOf(header)
        return position - headerPosition - 1
    }

    override fun setClicksBlocked(doBlock: Boolean) {
        if (doBlock) {
            rvPinnedCollections.visibility = View.GONE
            blockerPinnedCollections.visibility = View.VISIBLE
        }
        else {
            rvPinnedCollections.visibility = View.VISIBLE
            blockerPinnedCollections.visibility = View.GONE
        }
    }

    override fun onBackPressed(): Boolean {
        var handledBySelector = false
        /**
         * Try to find one selector to handle
         */
        for (selector in mSelectors) {
            handledBySelector = handledBySelector || selector.onBackPressed()
            if (handledBySelector) return true
        }
        return handledBySelector
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        val context = context!!

        when (item.itemId) {
            R.id.actionPinnedCollectionsCreateAlbum -> {
                DialogGenerator.createAlbum(context, onConfirm = ::onCreateNewAlbum)
            }
            R.id.actionPinnedCollectionsDeleteAlbum -> {
                val callback = callback@ {
                    val activeSelector = mSelectors.find { s -> s.active } ?: return@callback
                    CollectionManager.deleteAlbumsByPosition(activeSelector.selectedItemIds)
                    activeSelector.deactivate()
                    mAdapter.updateCollections()
                }
                DialogGenerator.deleteAlbum(context, onConfirm = callback)
            }
            else -> super.onOptionsItemSelected(item)
        }

        return true
    }

    /**
     * Called by LongClickSelector upon its activation
     */
    override fun onAlternateModeActivated(section: StatelessSection) {
        mDefaultToolbar.visibility = Toolbar.GONE
        mEditToolbar.visibility = Toolbar.VISIBLE

        section as PinnedCollectionsSection  // cast
        val editMenu = mEditToolbar.menu
        when (section.title.toLowerCase()) {
            "folders" -> {
                editMenu.findItem(R.id.actionPinnedCollectionsHide).isVisible = true
            }
            "albums" -> {
                editMenu.findItem(R.id.actionPinnedCollectionsDeleteAlbum).isVisible = true
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
        editMenu.findItem(R.id.actionPinnedCollectionsHide).isVisible = false
        editMenu.findItem(R.id.actionPinnedCollectionsDeleteAlbum).isVisible = false

        // already does notifyDataSetChanged(), so don't call it again to reset views
        mAdapter.showAllSections()
    }

    private fun onCreateNewAlbum(charSequence: CharSequence) {
        CollectionManager.createNewAlbum(charSequence.toString())
        mAdapter.updateCollections()
    }

    /**
     * Update everything to reflect changes
     */
    fun notifyChanges() {
        mAdapter.updateCollections()
    }

    /**
     * Get new data from Collection Manager.
     */
    private fun fetchContents() {
        var headerItem : SectionHeaderItem
        mCollections.clear()
        mCollectionItems.clear()
        mHeaders.clear()

        // Fetch folders
        headerItem = SectionHeaderItem("Folders")
        mHeaders.add(headerItem)
        for (folder in CollectionManager.folders) {
            mCollections.add(folder)
            val contentItem = PinnedCollectionsItem(folder, headerItem)
            mCollectionItems.add(contentItem)
            headerItem.addSubItem(contentItem)
        }

        // Fetch albums
        headerItem = SectionHeaderItem("Albums")
        mHeaders.add(headerItem)
        for (album in CollectionManager.albums) {
            mCollections.add(album)
            val contentItem = PinnedCollectionsItem(album, headerItem)
            mCollectionItems.add(contentItem)
            headerItem.addSubItem(contentItem)
        }

    }

}