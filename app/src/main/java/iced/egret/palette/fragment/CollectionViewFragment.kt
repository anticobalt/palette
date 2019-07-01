package iced.egret.palette.fragment

import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.Visibility
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.SelectableAdapter
import eu.davidea.flexibleadapter.items.IFlexible
import iced.egret.palette.R
import iced.egret.palette.activity.MainActivity
import iced.egret.palette.adapter.CollectionViewAdapter
import iced.egret.palette.model.Album
import iced.egret.palette.model.Coverable
import iced.egret.palette.recyclerview_component.*
import iced.egret.palette.section.CollectionViewSection
import iced.egret.palette.util.CollectionManager
import iced.egret.palette.util.DialogGenerator
import iced.egret.palette.util.MainFragmentManager
import iced.egret.palette.util.Painter
import io.github.luizgrp.sectionedrecyclerviewadapter.StatelessSection
import kotlinx.android.synthetic.main.fragment_view_collection.*


class CollectionViewFragment :
        MainFragment(),
        ActionMode.Callback,
        FlexibleAdapter.OnItemClickListener,
        FlexibleAdapter.OnItemLongClickListener {

    companion object SaveDataKeys {
        const val selectedHeaderPosition = "CollectionViewFragment_SHP"
    }

    private lateinit var mActionModeHelper: ToolbarActionModeHelper

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
    private var mHeaders = mutableListOf<SectionHeaderItem>()

    private lateinit var mAdapter: CollectionViewAdapter
    lateinit var adapter: FlexibleAdapter<SectionHeaderItem>

    private var selectedSectionHeader : SectionHeaderItem? = null

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
     * Where all the save state loading is done.
     * Must be done after MainActivity is finished creation,
     * as fragment relies on it to load the correct viewing Collection
     */
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        (activity as MainActivity).notifyFragmentCreationFinished(this)

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
                mContentItems[getCardinalPosition(pos)].setSelection(true)
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        adapter.onSaveInstanceState(outState)
        outState.putInt(selectedHeaderPosition, mHeaders.indexOf(selectedSectionHeader))
        super.onSaveInstanceState(outState)
    }

    override fun onFragmentCreationFinished(fragment: MainFragment) {
        // TODO
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

            fetchContents()
            val manager = GridLayoutManager(activity, 3)
            adapter = FlexibleAdapter(mHeaders, this, true).expandItemsAtStartUp()
            manager.spanSizeLookup = GridSectionSpanLookup(adapter, 3)

            mCollectionRecyclerView.layoutManager = manager
            mCollectionRecyclerView.adapter = adapter

            initializeActionModeHelper(SelectableAdapter.Mode.IDLE)

        }
    }

    /**
     * Straight from https://github.com/davideas/FlexibleAdapter/wiki/5.x-%7C-ActionModeHelper
     */
    private fun initializeActionModeHelper(@Visibility.Mode mode: Int) {
        //this = ActionMode.Callback instance
        mActionModeHelper = object : ToolbarActionModeHelper(adapter, R.menu.menu_view_collection_edit, this as ActionMode.Callback) {
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

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        return true
    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        (activity as MainActivity).isolateFragment(this)
        return true
    }

    private fun isolateSection(toIsolateHeader: SectionHeaderItem) {
        for (header in mHeaders) {
            if (header != toIsolateHeader) {
                adapter.removeSection(header)
            }
        }
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        return true
    }

    override fun onDestroyActionMode(mode: ActionMode) {
        // ToolbarActionModeHelper doesn't have references to CoverableItems,
        // so can't clear all selections visually
        mContentItems.map {item -> item.setSelection(false)}
        restoreAllSections()
        selectedSectionHeader = null  // no selection active
        (activity as MainActivity).restoreAllFragments()
    }

    private fun restoreAllSections() {
        for (header in mHeaders) {
            if (!adapter.contains(header)) {
                adapter.addSection(header, CollectionManager.SectionComparator)
            }
        }
    }

    override fun onItemClick(view: View, absolutePosition: Int): Boolean {
        val clickedItem = adapter.getItem(absolutePosition) as IFlexible<*>

        if (clickedItem !is CoverableItem) return true

        val cardinalPosition = getCardinalPosition(absolutePosition)
        Log.i("CLICK", "position is $absolutePosition, $cardinalPosition")
        return if (adapter.mode != SelectableAdapter.Mode.IDLE) {
            mActionModeHelper.onClick(absolutePosition, mContentItems[cardinalPosition])
        }
        else {
            val coverable = mContents[cardinalPosition]
            val updates = CollectionManager.launch(coverable, position = cardinalPosition, c = this.context)
            if (updates) refreshData()
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
        Log.i("CLICK", "position long click is $absolutePosition, $cardinalPosition")

        // Isolate the section BEFORE ActionMode is created, so that the correct
        // section position can be noted by the ActionModeHelper (instead of global position,
        // which unnecessarily accounts for temporarily remove sections)
        if (selectedSectionHeader == null) {
            selectedSectionHeader = adapter.getSectionHeader(absolutePosition) as SectionHeaderItem
            isolateSection(selectedSectionHeader!!)
            // adapter only has one section now, so global == relative
            relativePosition = adapter.getGlobalPositionOf(mContentItems[cardinalPosition])
        }
        else {
            relativePosition = absolutePosition
        }
        mActionModeHelper.onLongClick(mDefaultToolbar, relativePosition, mContentItems[cardinalPosition])
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

    /**
     * Given a relative position (as used by mActionModeHelper),
     * figure out position that it would have taken BEFORE section isolation.
     * @param relativePosition position in section (including header, whose position = 0)
     */
    private fun convertRelativePosition(relativePosition: Int) : Int? {
        var oldPosition = 0
        for (header in mHeaders) {
            if (header == selectedSectionHeader) {
                oldPosition += relativePosition
                return oldPosition
            }
            else {
                oldPosition += header.subItemsCount + 1
            }
        }
        return null  // couldn't find selected section; should never happen
    }

    override fun setClicksBlocked(doBlock: Boolean) {
        if (doBlock) {
            rvCollectionItems.visibility = View.GONE
            fab.hide()
            blockerViewCollection.visibility = View.VISIBLE
        }
        else {
            rvCollectionItems.visibility = View.VISIBLE
            fab.show()
            blockerViewCollection.visibility = View.GONE
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
            refreshData()
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

    /**
     * Refresh the content shown in the recycler view.
     */
    fun refreshData() {
        fetchContents()
        adapter.updateDataSet(mHeaders)
    }

    /**
     * Get new data from Collection Manager.
     */
    private fun fetchContents() {
        mContents.clear()
        mContentItems.clear()
        mHeaders.clear()

        val contentsMap = CollectionManager.getContentsMap()
        for ((type, coverables) in contentsMap) {
            val header = SectionHeaderItem(type)
            val coverableItems = coverables.map {content -> CollectionViewItem(content, header)}
            for (item in coverableItems) {
                header.addSubItem(item)
            }
            mHeaders.add(header)
            mContents.addAll(coverables)
            mContentItems.addAll(coverableItems)
        }
    }

}