package iced.egret.palette.fragment

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.*
import android.widget.ImageButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.slidingpanelayout.widget.SlidingPaneLayout
import androidx.transition.Visibility
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.SelectableAdapter
import iced.egret.palette.R
import iced.egret.palette.activity.MainActivity
import iced.egret.palette.activity.RecycleBinActivity
import iced.egret.palette.activity.SettingsActivity
import iced.egret.palette.flexible.CoverableItem
import iced.egret.palette.flexible.PinnedCollectionItem
import iced.egret.palette.flexible.ToolbarActionModeHelper
import iced.egret.palette.itemdecoration.PinnedCollectionMargin
import iced.egret.palette.model.Album
import iced.egret.palette.model.Collection
import iced.egret.palette.model.Folder
import iced.egret.palette.util.CollectionManager
import iced.egret.palette.util.DialogGenerator
import iced.egret.palette.util.Painter
import kotlinx.android.synthetic.main.appbar_list_fragment.view.*
import kotlinx.android.synthetic.main.fragment_links.*

/**
 * Has links to other activities and pinned collections, organized inside a SlidePaneLayout.
 *
 * General order of functions:
 * - Lifecycle
 * - UI builders
 * - Click handlers
 * - ActionMode
 * - Management by MainActivity
 * - Refreshers
 */
class LinksFragment :
        ListFragment(),
        ActionMode.Callback,
        FlexibleAdapter.OnItemClickListener,
        FlexibleAdapter.OnItemLongClickListener {

    companion object SaveDataKeys {
        const val selectedType = "LinksFragment_ST"
    }

    private lateinit var mMaster: MainActivity
    private var mRootView: View? = null
    private lateinit var mRecyclerView: RecyclerView

    private var mCollections = mutableListOf<Collection>()
    private var mCollectionItems = mutableListOf<PinnedCollectionItem>()

    private lateinit var mAdapter: FlexibleAdapter<PinnedCollectionItem>
    private lateinit var mActionModeHelper: ToolbarActionModeHelper
    private var mSelectedContentType: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        super.onCreateView(inflater, container, savedInstanceState)
        mRootView = inflater.inflate(R.layout.fragment_links, container, false)

        mRecyclerView = mRootView!!.findViewById(R.id.recyclerView)
        mMaster = activity as MainActivity

        buildToolbar()
        buildRecyclerView()
        buildSideActions()
        styleSlidePane()
        styleExtraThemeElements()

        initializeActionModeHelper(SelectableAdapter.Mode.IDLE)
        return mRootView
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        (activity as MainActivity).notifyFragmentCreationFinished(this)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)

        // If selected content type is saved, restore it, otherwise get out
        if (savedInstanceState == null) return
        val contentType = savedInstanceState.getString(selectedType, "")
        if (contentType.isEmpty()) return

        // Isolate contents
        mSelectedContentType = contentType
        isolateContent(mSelectedContentType!!)

        // Must restore adapter and helper AFTER type isolation to keep position ints consistent
        mAdapter.onRestoreInstanceState(savedInstanceState)
        mActionModeHelper.restoreSelection(toolbar)

        // Re-select all previously selected items
        for (i in 0 until mAdapter.currentItems.size) {
            if (i in mAdapter.selectedPositionsAsSet) {
                mAdapter.currentItems[i].setSelection(true)
            }
        }

    }

    override fun onResume() {
        super.onResume()
        styleExtraThemeElements()
        if (mSelectedContentType != null) mMaster.isolateFragment(this)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        mAdapter.onSaveInstanceState(outState)
        outState.putString(selectedType, mSelectedContentType)
        super.onSaveInstanceState(outState)
    }

    private fun buildToolbar() {
        toolbar = mRootView!!.findViewById(R.id.toolbar)
        toolbar.toolbarTitle.text = getString(R.string.app_name)
        toolbar.inflateMenu(R.menu.menu_links)
        toolbar.setOnMenuItemClickListener {
            onOptionsItemSelected(it)
        }

        toolbar.navigation.setImageDrawable(navigationDrawable)
        toolbar.navigation.setOnClickListener {
            if (slider.isOpen) slider.closePane()
            else slider.openPane()
        }
    }

    private fun buildRecyclerView() {
        fetchContents()
        mRecyclerView.layoutManager = GridLayoutManager(activity, 1)
        mAdapter = FlexibleAdapter(mCollectionItems, this, true)
        mRecyclerView.adapter = mAdapter

        val marginInPx = resources.getDimensionPixelSize(R.dimen.banner_margin)
        mRecyclerView.addItemDecoration(PinnedCollectionMargin(marginInPx))
    }

    private fun buildSideActions() {
        val sideLayout = mRootView!!.findViewById<ConstraintLayout>(R.id.sideActionsLayout)

        sideLayout.findViewById<ImageButton>(R.id.settings).setOnClickListener {
            startActivity(Intent(this.context, SettingsActivity::class.java))
        }
        sideLayout.findViewById<ImageButton>(R.id.recycleBin).setOnClickListener {
            startActivity(Intent(this.context, RecycleBinActivity::class.java))
        }
    }

    private fun styleSlidePane() {
        val slider = mRootView!!.findViewById<SlidingPaneLayout>(R.id.slider)
        slider.sliderFadeColor = Color.TRANSPARENT  // make right not greyed out
        slider.setShadowResourceLeft(R.drawable.shadow_left_fixed)
    }

    // Style themed stuff that isn't handled by Aesthetic or other inherited classes.
    private fun styleExtraThemeElements() {
        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val accentColor = prefs.getInt(getString(R.string.accent_color_key), R.color.colorAccent)
        val iconColor = prefs.getInt(getString(R.string.toolbar_item_color_key), R.color.white)

        val sideLayout = mRootView!!.findViewById<ConstraintLayout>(R.id.sideActionsLayout)
        sideLayout.background = ColorDrawable(accentColor)
        for (touchable in sideLayout.touchables) {
            if (touchable is ImageButton) {
                touchable.imageTintList = ColorStateList.valueOf(iconColor)
            }
        }
    }

    /**
     * Straight from https://github.com/davideas/FlexibleAdapter/wiki/5.x-%7C-ActionModeHelper
     */
    private fun initializeActionModeHelper(@Visibility.Mode mode: Int) {
        // this = ActionMode.Callback instance
        mActionModeHelper = object : ToolbarActionModeHelper(mAdapter, R.menu.menu_links_edit, this) {
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

    override fun onItemClick(view: View, absolutePosition: Int): Boolean {
        val clickedItem = mAdapter.getItem(absolutePosition) as? CoverableItem
                ?: return false

        return if (mAdapter.mode != SelectableAdapter.Mode.IDLE) {
            mActionModeHelper.onClick(absolutePosition, clickedItem)
        } else {
            openCollectionViewPanel(absolutePosition)
            false
        }
    }

    /**
     * @param absolutePosition the actual position amongst all on-screen items
     * relativePosition is absolutePosition while ignoring all other content types
     *
     * All positions refer to arrangement before click handling.
     */
    override fun onItemLongClick(absolutePosition: Int) {
        val relativePosition: Int

        // Isolate the content type BEFORE ActionMode is created, so that the correct
        // relative position can be noted by the ActionModeHelper (instead of global position,
        // which unnecessarily accounts for temporarily remove types)
        if (mSelectedContentType == null) {
            mSelectedContentType = inferContentType(mCollections[absolutePosition]) ?: return
            isolateContent(mSelectedContentType!!)
            // adapter only holds one type now, so global == relative
            relativePosition = mAdapter.getGlobalPositionOf(mCollectionItems[absolutePosition])
        } else {
            relativePosition = absolutePosition
        }
        mActionModeHelper.onLongClick(toolbar, relativePosition, mCollectionItems[absolutePosition])
    }

    private fun inferContentType(collection: Collection): String? {
        return when (collection) {
            is Folder -> CollectionManager.FOLDER_KEY
            is Album -> CollectionManager.ALBUM_KEY
            else -> null
        }
    }

    /**
     * Get one collection with type name provided, if one exists
     */
    private fun getInstanceOfType(typeName: String): Collection? {
        val type = when (typeName) {
            CollectionManager.FOLDER_KEY -> Folder::class.java
            CollectionManager.ALBUM_KEY -> Album::class.java
            else -> return null
        }
        return mCollections.find { collection -> collection.javaClass == type }
    }

    /**
     * Get all collections with same type as parameter, including the parameter
     */
    private fun getAllOfSameType(collection: Collection): List<Collection> {
        val typeList = mutableListOf<Collection>()
        for (c in mCollections) {
            if (c.javaClass == collection.javaClass) {
                typeList.add(c)
            }
        }
        return typeList
    }

    private fun isolateContent(isolateType: String) {
        var index = 0
        var adapterOffset = 0
        val isolateTypeList = getAllOfSameType(getInstanceOfType(isolateType) ?: return)

        // Jump to this first item of each type and batch remove all of its type if required.
        // This only works because items of the same type are guaranteed to be successive
        // inside mContents, since it is copied from CollectionManager.
        while (index < mCollections.size) {
            if (mCollections[index] in isolateTypeList) {
                // keeping this type
                index += isolateTypeList.size
                adapterOffset += isolateTypeList.size
            } else {
                // removing this type
                val removeTypeList = getAllOfSameType(mCollections[index])
                mAdapter.removeRange(adapterOffset, removeTypeList.size)
                // next type is now at position adapterOffset after remove, so don't increment
                index += removeTypeList.size
            }
        }
    }

    private fun restoreAllContent() {
        for (i in 0 until mCollectionItems.size) {
            val item = mCollectionItems[i]
            if (!mAdapter.contains(item)) {
                mAdapter.addItem(i, item)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {

        val context = context!!

        fun albumExists(name: CharSequence): Boolean {
            val found = CollectionManager.albums.find { album -> album.name == name.toString() }
            return found != null
        }

        fun onCreateNewAlbum(charSequence: CharSequence) {
            CollectionManager.createNewAlbum(charSequence.toString())
            onCollectionsUpdated()
        }

        when (item.itemId) {
            R.id.actionCreateAlbum -> {
                DialogGenerator.createAlbum(context, ::albumExists, ::onCreateNewAlbum)
            }
            else -> super.onOptionsItemSelected(item)
        }

        return true
    }

    override fun onBackPressed(): Boolean {
        return false  // not handled here
    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        // Handles blocking in the case where ActionMode is created after other fragments created.
        (activity as MainActivity).isolateFragment(this)

        // Make items visible depending on selected content.
        // Painting has to be done here for ActionMode icons, because XML app:iconTint
        // doesn't work on items not visible on activity start.
        when (mSelectedContentType) {
            CollectionManager.FOLDER_KEY -> {
                val item = menu.findItem(R.id.actionHide)
                item.isVisible = true
                Painter.paintDrawable(item.icon)
            }
            CollectionManager.ALBUM_KEY -> {
                val item = menu.findItem(R.id.actionDeleteAlbum)
                item.isVisible = true
                Painter.paintDrawable(item.icon)
            }
        }
        val selectAll = menu.findItem(R.id.actionToggleSelectAll)
        Painter.paintDrawable(selectAll.icon)

        return true
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.actionToggleSelectAll -> {
                selectAll()
            }
            R.id.actionDeleteAlbum -> {
                DialogGenerator.deleteAlbum(context!!) {
                    CollectionManager.deleteAlbumsByRelativePosition(mAdapter.selectedPositions)
                    onCollectionsUpdated()
                    mMaster.notifyPinnedAlbumDeleted()
                    mActionModeHelper.destroyActionModeIfCan()
                }
            }
        }
        return true
    }

    override fun onPrepareActionMode(p0: ActionMode, p1: Menu): Boolean {
        return true
    }

    override fun onDestroyActionMode(mode: ActionMode) {
        // ToolbarActionModeHelper doesn't have references to CoverableItems,
        // so can't clear all selections visually
        mAdapter.currentItems.map { item -> item.setSelection(false) }
        restoreAllContent()
        mSelectedContentType = null  // nothing isolated
        (activity as MainActivity).restoreAllFragments()

        mode.menu.findItem(R.id.actionHide).isVisible = false
        mode.menu.findItem(R.id.actionDeleteAlbum).isVisible = false
    }

    private fun selectAll() {
        mAdapter.currentItems.map { item -> item.setSelection(true) }
        mAdapter.selectAll()
        mActionModeHelper.updateContextTitle(mAdapter.selectedItemCount)
    }

    override fun onAllFragmentsCreated() {
        // Handles blocking in the case where ActionMode is created before other fragments created.
        // See onCreateActionMode() for other case.
        if (mActionModeHelper.getActionMode() != null) {
            (activity as MainActivity).isolateFragment(this)
        }
    }

    override fun setClicksBlocked(doBlock: Boolean) {
        if (doBlock) {
            recyclerView.visibility = View.GONE
            blocker.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            blocker.visibility = View.GONE
        }
    }

    /**
     * Set up and slide open right panel.
     * @param referencePosition Index of Collection to open with
     */
    private fun openCollectionViewPanel(referencePosition: Int) {
        mMaster.buildCollectionView(mCollections[referencePosition])
    }

    /**
     * Get new data from Collection Manager.
     */
    private fun fetchContents() {
        mCollections.clear()
        mCollectionItems.clear()

        // Fetch folders
        for (folder in CollectionManager.folders) {
            mCollections.add(folder)
            val contentItem = PinnedCollectionItem(folder)
            mCollectionItems.add(contentItem)
        }

        // Fetch albums
        for (album in CollectionManager.albums) {
            mCollections.add(album)
            val contentItem = PinnedCollectionItem(album)
            mCollectionItems.add(contentItem)
        }

    }

    fun onCollectionsUpdated() {
        fetchContents()
        mAdapter.updateDataSet(mCollectionItems)
    }

}