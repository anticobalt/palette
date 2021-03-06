package iced.egret.palette.fragment

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.*
import android.widget.ImageButton
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.slidingpanelayout.widget.SlidingPaneLayout
import androidx.transition.Visibility
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.SelectableAdapter
import eu.davidea.flexibleadapter.common.FlexibleItemDecoration
import iced.egret.palette.R
import iced.egret.palette.activity.MainActivity
import iced.egret.palette.activity.MainActivity.Constants.GO_HOME_REQUEST
import iced.egret.palette.activity.RecycleBinActivity
import iced.egret.palette.activity.SettingsActivity
import iced.egret.palette.activity.WaitingRoomActivity
import iced.egret.palette.activity.inherited.BaseActivity
import iced.egret.palette.flexible.ToolbarActionModeHelper
import iced.egret.palette.flexible.item.BannerCoverableItem
import iced.egret.palette.flexible.item.inherited.CoverableItem
import iced.egret.palette.fragment.inherited.MainFragment
import iced.egret.palette.model.Album
import iced.egret.palette.model.Folder
import iced.egret.palette.model.inherited.Collection
import iced.egret.palette.util.CollectionManager
import iced.egret.palette.util.CoverableMutator
import iced.egret.palette.util.Painter
import kotlinx.android.synthetic.main.appbar_list_fragment.view.*
import kotlinx.android.synthetic.main.fragment_links.*
import java.util.*


/**
 * Has links to other activities and pinned collections, organized inside a [SlidePaneLayout].
 *
 * General order of functions:
 * - Lifecycle
 * - UI builders
 * - Click handlers
 * - ActionMode
 * - Management by MainActivity
 * - Refreshers
 * - Aliases
 */
class LinksFragment : MainFragment() {

    private var mRootView: View? = null
    private lateinit var mRecyclerView: RecyclerView

    private var mCollections = mutableListOf<Collection>()
    private var mCollectionItems = mutableListOf<BannerCoverableItem>()

    private lateinit var mAdapter: FlexibleAdapter<BannerCoverableItem>
    private lateinit var mActionModeHelper: ToolbarActionModeHelper
    private var mSelectedContentType: String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        super.onCreateView(inflater, container, savedInstanceState)
        mRootView = inflater.inflate(R.layout.fragment_links, container, false)

        mRecyclerView = mRootView!!.findViewById(R.id.recyclerView)

        buildToolbar()
        buildRecyclerView()
        buildSideActions()
        styleSlidePane()

        // Color changes force recreation (see SettingsActivity)
        colorBars()
        colorSideLayout()

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
        val contentType = savedInstanceState.getString(SELECTED_TYPE, "")
        if (contentType.isEmpty()) return

        // Isolate contents
        mSelectedContentType = contentType
        isolateContent(mSelectedContentType!!)

        // Must restore adapter and helper AFTER type isolation to keep position ints consistent
        val selections = savedInstanceState.getIntegerArrayList(SELECTED_POSITIONS)
        if (selections == null) {
            restoreAllContent()
            return
        }
        mActionModeHelper.selectedPositions.addAll(selections)
        mActionModeHelper.restoreSelection(toolbar)

        // Re-select all previously selected items
        for (i in 0 until mAdapter.currentItems.size) {
            if (i in mActionModeHelper.selectedPositions) {
                mAdapter.currentItems[i].setSelection(true)
            }
        }

    }

    override fun onResume() {
        super.onResume()
        colorSideLayout()
        if (mSelectedContentType != null) mActivity.isolateFragment(this)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putIntegerArrayList(SELECTED_POSITIONS, mActionModeHelper.selectedPositions.toMutableList() as ArrayList<Int>)
        outState.putString(SELECTED_TYPE, mSelectedContentType)
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

        // Pixel to DPI: https://stackoverflow.com/a/9563438
        val marginInPx = resources.getDimensionPixelSize(R.dimen.banner_margin)
        val marginInDp = marginInPx / (mActivity.resources.displayMetrics.densityDpi / DisplayMetrics.DENSITY_DEFAULT)
        mRecyclerView.addItemDecoration(FlexibleItemDecoration(context!!)
                .addItemViewType(R.layout.item_coverable_banner)
                .withOffset(marginInDp)
                .withEdge(true)
        )
    }

    private fun buildSideActions() {
        val sideLayout = mRootView!!.findViewById<ConstraintLayout>(R.id.sideActionsLayout)

        // None of these actions have anything really to do with the fragment's state,
        // they're just housed here visually, so let activity handle them.
        sideLayout.findViewById<ImageButton>(R.id.settings).setOnClickListener {
            mActivity.startActivity(Intent(this.context, SettingsActivity::class.java))
        }
        sideLayout.findViewById<ImageButton>(R.id.recycleBin).setOnClickListener {
            mActivity.startActivity(Intent(this.context, RecycleBinActivity::class.java))
        }
        sideLayout.findViewById<ImageButton>(R.id.waitingRoom).setOnClickListener {
            mActivity.startActivityForResult(Intent(this.context, WaitingRoomActivity::class.java), GO_HOME_REQUEST)
        }
    }

    private fun styleSlidePane() {
        val slider = mRootView!!.findViewById<SlidingPaneLayout>(R.id.slider)
        slider.sliderFadeColor = Color.TRANSPARENT  // make right not greyed out
        //slider.setShadowResourceLeft(R.drawable.shadow_left_fixed)
    }

    private fun colorSideLayout() {
        val accentColor = getColorInt(BaseActivity.ColorType.ACCENT)
        val iconColor = getColorInt(BaseActivity.ColorType.ITEM)

        val sideLayout = mRootView!!.findViewById<ConstraintLayout>(R.id.sideActionsLayout)
        sideLayout.background = ColorDrawable(accentColor)
        for (touchable in sideLayout.touchables) {
            if (touchable is ImageButton) {
                touchable.imageTintList = ColorStateList.valueOf(iconColor)
            }
        }

        /*
        val shadow = sideLayout.findViewById<View>(R.id.sideActionsTopShadow)
        shadow.layoutParams.width =
                resources.getDimensionPixelSize(R.dimen.side_icon_total_width) -
                resources.getDimensionPixelSize(R.dimen.fixed_shadow_size) */
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
                mActionMode?.title = getString(R.string.action_selected, count, mAdapter.itemCount)
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
        when (item.itemId) {
            R.id.actionCreateAlbum -> createAlbum()
            else -> super.onOptionsItemSelected(item)
        }
        return true
    }

    override fun onBackPressed(): Boolean {
        return false  // not handled here
    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        mActivity.colorActionMode()

        // Handles blocking in the case where ActionMode is created after other fragments created.
        mActivity.isolateFragment(this)

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
        val selectAll = menu.findItem(R.id.actionSelectAll)
        Painter.paintDrawable(selectAll.icon)

        return true
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.actionSelectAll -> selectAll()
            R.id.actionDeleteAlbum -> deleteAlbum()
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
        mActivity.restoreAllFragments()
        mActivity.undoColorActionMode()

        mode.menu.findItem(R.id.actionHide).isVisible = false
        mode.menu.findItem(R.id.actionDeleteAlbum).isVisible = false
    }

    private fun createAlbum() {
        CoverableMutator.createTopAlbum(context!!) {
            onCollectionsUpdated()
        }
    }

    private fun deleteAlbum() {
        val allAlbums = mCollections.filterIsInstance<Album>()
        val selectedAlbums = mActionModeHelper.selectedPositions.map { i -> allAlbums[i] }
        CoverableMutator.deleteTopAlbums(selectedAlbums, context!!) {
            mActivity.notifyPinnedAlbumDeleted()
            mActionModeHelper.destroyActionModeIfCan()
            onCollectionsUpdated()
        }
    }

    private fun selectAll() {
        if (mActionModeHelper.selectedPositions.size == mAdapter.currentItems.size) return

        var i = 0
        for (item in mAdapter.currentItems) {
            item.setSelection(true)
            mActionModeHelper.selectedPositions.add(i)
            i += 1
        }
        mActionModeHelper.updateContextTitle(mActionModeHelper.selectedPositions.size)
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
     * @param referencePosition Index of [Collection] to open with
     */
    private fun openCollectionViewPanel(referencePosition: Int) {
        mActivity.buildCollectionView(mCollections[referencePosition])
    }

    /**
     * Get new data from [CollectionManager].
     */
    private fun fetchContents() {
        mCollections.clear()
        mCollectionItems.clear()

        // Fetch folders
        for (folder in CollectionManager.folders) {
            mCollections.add(folder)
            val contentItem = BannerCoverableItem(folder)
            mCollectionItems.add(contentItem)
        }

        // Fetch albums
        for (album in CollectionManager.albums) {
            mCollections.add(album)
            val contentItem = BannerCoverableItem(album)
            mCollectionItems.add(contentItem)
        }

    }

    /**
     * Since this can be called whenever [CollectionViewFragment] fetches new content,
     * (even when it's clicks are blocked and this fragment is isolated), don't
     * update the adapter if in selection mode.
     *
     * If the cover changes while in selection mode, need to manually refresh for it
     * to show up.
     */
    fun onCollectionsUpdated() {
        fetchContents()
        if (mSelectedContentType == null) mAdapter.updateDataSet(mCollectionItems)
    }

    private fun getColorInt(type: BaseActivity.ColorType): Int {
        return mActivity.getColorInt(type)
    }

    companion object SaveDataKeys {
        const val SELECTED_TYPE = "LinksFragment_ST"
        const val SELECTED_POSITIONS = "LF_SP"
    }

}