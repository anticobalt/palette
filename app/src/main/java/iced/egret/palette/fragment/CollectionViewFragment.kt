package iced.egret.palette.fragment

import android.app.Activity.RESULT_OK
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.*
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.transition.Visibility
import com.google.android.material.floatingactionbutton.FloatingActionButton
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.SelectableAdapter
import eu.davidea.flexibleadapter.helpers.EmptyViewHelper
import iced.egret.palette.R
import iced.egret.palette.activity.MainPagerActivity
import iced.egret.palette.activity.inherited.BaseActivity
import iced.egret.palette.delegate.AlbumViewDelegate
import iced.egret.palette.delegate.FolderViewDelegate
import iced.egret.palette.delegate.inherited.CollectionViewDelegate
import iced.egret.palette.flexible.ToolbarActionModeHelper
import iced.egret.palette.flexible.item.GridCoverableItem
import iced.egret.palette.flexible.item.inherited.CoverableItem
import iced.egret.palette.fragment.inherited.MainFragment
import iced.egret.palette.model.Album
import iced.egret.palette.model.Folder
import iced.egret.palette.model.Picture
import iced.egret.palette.model.inherited.Collection
import iced.egret.palette.model.inherited.Coverable
import iced.egret.palette.util.CollectionManager
import iced.egret.palette.util.CoverableMutator
import iced.egret.palette.util.DialogGenerator
import iced.egret.palette.util.Painter
import kotlinx.android.synthetic.main.appbar_list_fragment.view.*
import kotlinx.android.synthetic.main.fragment_view_collection.*
import java.io.File
import java.util.*

/**
 * Meat of the app. Shows Collection contents and allows navigation through them.
 * Automatically requests CollectionManager to get fresh data when returning from another activity
 * or app.
 *
 * General order of functions:
 * - Lifecycle
 * - UI builders
 * - Click and gesture handlers
 * - ActionMode
 * - Content manipulation
 * - State manipulation and refreshers
 * - Management by MainActivity
 * - Aliases
 */
class CollectionViewFragment : MainFragment(), SwipeRefreshLayout.OnRefreshListener {

    private var mDelegate: CollectionViewDelegate = FolderViewDelegate()  // default
        set(value) {
            value.listener = this
            field = value
        }

    private var mRootView: View? = null
    private lateinit var mCollectionRecyclerView: RecyclerView
    private lateinit var mFloatingActionButton: FloatingActionButton
    private lateinit var mSwipeRefreshLayout: SwipeRefreshLayout

    private var mContents = mutableListOf<Coverable>()
    private var mContentItems = mutableListOf<GridCoverableItem>()

    private lateinit var mAdapter: FlexibleAdapter<GridCoverableItem>
    private lateinit var mLayoutManager: GridLayoutManager
    private lateinit var mActionModeHelper: ToolbarActionModeHelper
    private var mSelectedContentType: String? = null
    private var mReturningFromStop = false

    private val syncIconRes = R.drawable.ic_sync_black_24dp

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        super.onCreateView(inflater, container, savedInstanceState)
        mRootView = inflater.inflate(R.layout.fragment_view_collection, container, false)

        mCollectionRecyclerView = mRootView!!.findViewById(R.id.rvCollectionItems)
        mFloatingActionButton = mRootView!!.findViewById(R.id.fab)
        mSwipeRefreshLayout = mRootView!!.findViewById(R.id.swipeRefreshLayout)

        fetchContents()
        buildDelegate()
        buildToolbar()
        buildRecyclerView()
        mSwipeRefreshLayout.setOnRefreshListener(this)
        mFloatingActionButton.setOnClickListener { onFabClick() }

        // Color changes force recreation (see SettingsActivity)
        colorBars()
        colorFab()

        return mRootView

    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mActivity.notifyFragmentCreationFinished(this)
    }

    /**
     * Called after MainActivity done loading collection, so safe to restore state here.
     */
    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)

        //If selected content type is saved, restore it, otherwise get out
        if (savedInstanceState == null) return
        val contentType = savedInstanceState.getString(SELECTED_TYPE, "")
        if (contentType.isEmpty()) return

        // Isolate internal contents and self
        mSelectedContentType = contentType
        isolateContent(mSelectedContentType!!)

        // Must restore selections and helper AFTER type isolation to keep position ints consistent
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

    /**
     * Called after onActivityResult() if applicable.
     */
    override fun onResume() {
        super.onResume()
        // Never refresh if currently selecting stuff.
        // If not selecting, refresh if onCreate() not called.
        if (mSelectedContentType != null) {
            mActivity.isolateFragment(this)
            return
        }
        if (mReturningFromStop) {
            showUpdatedContents()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putIntegerArrayList(SELECTED_POSITIONS, mActionModeHelper.selectedPositions.toMutableList() as ArrayList<Int>)
        outState.putString(SELECTED_TYPE, mSelectedContentType)
        super.onSaveInstanceState(outState)
    }

    override fun onStop() {
        super.onStop()
        CollectionManager.writeCache()
        // Assume need to update onResume().
        mReturningFromStop = true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        when (requestCode) {
            PICTURE_ACTIVITY_REQUEST -> {
                if (resultCode == RESULT_OK) {
                    // Changes occurred: notify update; self-update occurs automatically in onResume()
                    mReturningFromStop = true
                    if (data?.getBooleanExtra(getString(R.string.intent_go_home), false) == true) {
                        setToolbarTitle()
                    }
                }
            }
            FOLDER_LIST_ACTIVITY_REQUEST -> {
                if (resultCode == RESULT_OK) {
                    applySyncedFolders(data)
                    mReturningFromStop = true
                }
            }
        }
    }

    private fun applySyncedFolders(intent: Intent?) {
        val album = CollectionManager.currentCollection

        // These really shouldn't ever happen
        if (intent == null) {
            toast(R.string.generic_error)
            return
        }
        if (album !is Album) {
            toast(R.string.edit_fail_error)
            return
        }

        val filePaths = intent.getStringArrayListExtra(getString(R.string.intent_synced_folder_paths))
        val folderFiles = filePaths.map { path -> File(path) }
        album.syncedFolderFiles.clear()
        album.syncedFolderFiles.addAll(folderFiles)
    }


    /**
     * Makes default toolbar and fills with items and title
     */
    private fun buildToolbar() {

        // Inflate menu items
        toolbar = mRootView!!.findViewById(R.id.toolbar)
        toolbar.menu.clear()
        toolbar.inflateMenu(R.menu.menu_view_collection)
        toolbar.setOnMenuItemClickListener {
            onOptionsItemSelected(it)
        }

        // Build toggle navigation icon
        toolbar.navigation.setImageDrawable(navigationDrawable)
        toolbar.navigation.setOnClickListener {
            mActivity.togglePanel()
        }

        // Set title
        setToolbarTitle()
        toolbar.toolbarTitle.setOnLongClickListener {
            if (CollectionManager.currentCollection != null) {
                DialogGenerator.showCollectionDetails(context!!, CollectionManager.currentCollection!!)
            }
            true
        }

        // Delegate other operations
        mDelegate.onBuildToolbar(toolbar)
    }

    /**
     * Sets toolbar's title to current Collection name
     */
    private fun setToolbarTitle(title: String = "") {
        var text: String
        if (title.isEmpty()) {
            val collection = CollectionManager.currentCollection
            text = collection?.path?.split("/")
                    ?.joinToString(" / ")
                    ?.trim(' ')
                    ?: getString(R.string.app_name)
            if (text.isEmpty()) text = getString(R.string.root_name)
        } else text = title

        toolbar.toolbarTitle.text = text
    }

    private fun colorFab() {
        val color = getColorInt(BaseActivity.ColorType.ITEM)
        mFloatingActionButton.drawable.setTint(color)
    }

    private fun buildDelegate() {
        when (CollectionManager.currentCollection) {
            is Folder -> {
                if (mDelegate !is FolderViewDelegate) mDelegate = FolderViewDelegate()
            }
            is Album -> {
                if (mDelegate !is AlbumViewDelegate) mDelegate = AlbumViewDelegate()
            }
        }
    }

    /**
     * Hooks up adapter and LayoutManager to RecyclerView
     */
    private fun buildRecyclerView() {
        val orientation = resources.configuration.orientation
        val numColumns = if (orientation == Configuration.ORIENTATION_LANDSCAPE) 5 else 3
        mLayoutManager = GridLayoutManager(activity, numColumns)

        fetchContents()
        mAdapter = FlexibleAdapter(mContentItems, this, true)
        initializeActionModeHelper(SelectableAdapter.Mode.IDLE)
        mCollectionRecyclerView.layoutManager = mLayoutManager
        mCollectionRecyclerView.adapter = mAdapter

        // Need to supply explicit view in fragment
        EmptyViewHelper.create(mAdapter, mRootView!!.findViewById(R.id.empty_view))
    }

    /**
     * Straight from https://github.com/davideas/FlexibleAdapter/wiki/5.x-%7C-ActionModeHelper
     */
    private fun initializeActionModeHelper(@Visibility.Mode mode: Int) {
        //this = ActionMode.Callback instance
        mActionModeHelper = object : ToolbarActionModeHelper(mAdapter, R.menu.menu_view_collection_edit, this as ActionMode.Callback) {
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
                ?: return true

        return if (mAdapter.mode != SelectableAdapter.Mode.IDLE) {
            mActionModeHelper.onClick(absolutePosition, clickedItem)
        } else {
            val coverable = mContents[absolutePosition]
            val relativePosition =
                    CollectionManager.getContentsMap()[inferContentType(coverable)]?.indexOf(coverable)
                            ?: return false
            // May start activity for result if required
            val launchPack = CollectionManager.PagerLaunchPack(relativePosition,
                    callingFragment = this, newActivityClass = MainPagerActivity::class.java,
                    requestCode = PICTURE_ACTIVITY_REQUEST)
            val updates = CollectionManager.launch(coverable, launchPack)
            if (updates) onNavigation()
            false
        }
    }

    /**
     * @param absolutePosition the actual position amongst all on-screen items
     * relativePosition is absolutePosition while ignoring all other types
     *
     * All positions refer to arrangement before click handling.
     */
    override fun onItemLongClick(absolutePosition: Int) {
        val relativePosition: Int

        // Isolate the content type BEFORE ActionMode is created, so that the correct
        // relative position can be noted by the ActionModeHelper (instead of global position,
        // which unnecessarily accounts for temporarily remove types)
        if (mSelectedContentType == null) {
            mSelectedContentType = inferContentType(mContents[absolutePosition]) ?: return
            isolateContent(mSelectedContentType!!)
            // adapter only holds one type now, so global == relative
            relativePosition = mAdapter.getGlobalPositionOf(mContentItems[absolutePosition])
        } else {
            relativePosition = absolutePosition
        }
        mActionModeHelper.onLongClick(toolbar, relativePosition, mContentItems[absolutePosition])
    }

    private fun inferContentType(content: Coverable): String? {
        return when (content) {
            is Folder -> CollectionManager.FOLDER_KEY
            is Album -> CollectionManager.ALBUM_KEY
            is Picture -> CollectionManager.PICTURE_KEY
            else -> null
        }
    }

    private fun isolateContent(isolateType: String) {
        // sanity check; all code after should never fail
        for (item in mContents) {
            inferContentType(item) ?: return
        }

        var index = 0
        var adapterOffset = 0
        val isolateTypeList = CollectionManager.getContentsMap()[isolateType]!!

        // Jump to this first item of each type and batch remove all of its type if required.
        // This only works because items of the same type are guaranteed to be successive
        // inside mContents, since it is copied from CollectionManager.
        while (index < mContents.size) {
            if (mContents[index] in isolateTypeList) {
                // keeping this type
                index += isolateTypeList.size
                adapterOffset += isolateTypeList.size
            } else {
                // removing this type
                val removeType: String = inferContentType(mContents[index])!!
                val removeTypeList = CollectionManager.getContentsMap()[removeType]!!
                mAdapter.removeRange(adapterOffset, removeTypeList.size)
                // next type is now at position adapterOffset after remove, so don't increment
                index += removeTypeList.size
            }
        }
    }

    private fun restoreAllContent() {
        for (i in 0 until mContentItems.size) {
            val item = mContentItems[i]
            if (!mAdapter.contains(item)) {
                mAdapter.addItem(i, item)
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.actionResetCover -> resetCover()
        }
        mDelegate.onOptionsItemSelected(item, this, CollectionManager.currentCollection!!)
        return true
    }

    private fun onFabClick() {
        mDelegate.onFabClick(context!!, mContents)
    }

    override fun onRefresh() {
        showUpdatedContents()
    }

    /**
     * @return handled here (true) or not (false)
     */
    override fun onBackPressed(): Boolean {
        return returnToParentCollection()
    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {

        mActivity.colorActionMode()
        mActivity.isolateFragment(this)

        // Always visible
        val selectAll = menu.findItem(R.id.actionSelectAll)

        // Conditional
        val albumActions = menu.findItem(R.id.albumActions)
        val addToAlbum = menu.findItem(R.id.actionAddToAlbum)
        val move = menu.findItem(R.id.actionMove)
        val delete = menu.findItem(R.id.actionDelete)

        // Display settings common to Folders and Pictures
        fun setFolderOrPicture() {
            albumActions.isVisible = true; Painter.paintDrawable(albumActions.icon)
            addToAlbum.isVisible = true; Painter.paintDrawable(addToAlbum.icon)
        }

        // Make items visible depending on selected content.
        // Painting has to be done here for ActionMode icons, because XML app:iconTint
        // doesn't work on items not visible on activity start.
        when (mSelectedContentType) {
            CollectionManager.FOLDER_KEY -> {
                setFolderOrPicture()
            }
            CollectionManager.PICTURE_KEY -> {
                setFolderOrPicture()
                move.isVisible = true; Painter.paintDrawable(move.icon)
                delete.isVisible = true; Painter.paintDrawable(delete.icon)
            }
            CollectionManager.ALBUM_KEY -> {
                delete.isVisible = true; Painter.paintDrawable(delete.icon)
            }
        }
        Painter.paintDrawable(selectAll.icon)

        mDelegate.onCreateActionMode(mode, menu, mSelectedContentType!!)
        return true
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {

        val coverablesOfType = CollectionManager.getContentsMap()[mSelectedContentType]
                ?: return false  // sanity check that selected type is valid

        val selectedCoverables = mActionModeHelper.selectedPositions.map { index -> coverablesOfType[index] }

        when (item.itemId) {
            R.id.actionSelectAll -> selectAll()
            R.id.albumActions -> {
                // No changes, don't refresh, so exit immediately.
                // Must return true for submenus to popup.
                return true
            }
            R.id.actionAddToAlbum -> addToAlbum(selectedCoverables)
            R.id.actionMove -> move(selectedCoverables)
            R.id.actionDelete -> delete(selectedCoverables)
        }

        mDelegate.onActionItemClicked(mode, item, selectedCoverables, context!!, mSelectedContentType!!)
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
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

        mode.menu.findItem(R.id.albumActions).isVisible = false
        mode.menu.findItem(R.id.actionAddToAlbum).isVisible = false
        mode.menu.findItem(R.id.actionMove).isVisible = false
        mode.menu.findItem(R.id.actionDelete).isVisible = false

        mDelegate.onDestroyActionMode(mode)
    }

    fun onDelegateAlert(alert: CollectionViewDelegate.ActionAlert) {
        if (alert.success) {
            // Update everything (short of remaking menus) b/c delegate doesn't specify what exactly changed.
            fetchContents()
            mAdapter.updateDataSet(mContentItems)
            setToolbarTitle()
            mActivity.notifyCollectionsChanged()
            mActionModeHelper.destroyActionModeIfCan()
        }
    }

    private fun resetCover() {
        val collection = CollectionManager.currentCollection

        if (collection is Collection) {
            CoverableMutator.resetCover(collection, context!!) {
                mActivity.notifyCollectionsChanged()  // update views if collection is pinned
                toast(R.string.success_reset_cover)
            }
        }
        else toast(R.string.generic_error)  // should never happen

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

    private fun addToAlbum(coverables: List<Coverable>) {
        CoverableMutator.addToAlbum(coverables, context!!) {
            mActivity.notifyCollectionsChanged()
            mActionModeHelper.destroyActionModeIfCan()
        }
    }

    private fun move(coverables: List<Coverable>) {
        @Suppress("UNCHECKED_CAST")  // assume internal consistency
        CoverableMutator.move(coverables as List<Picture>, context!!) {
            onCurrentContentsChanged()
            mActionModeHelper.destroyActionModeIfCan()
        }
    }

    @Suppress("UNCHECKED_CAST")  // assume internal consistency
    private fun delete(coverables: List<Coverable>) {
        when (mSelectedContentType) {
            CollectionManager.PICTURE_KEY -> {
                CoverableMutator.delete(coverables as List<Picture>, context!!) {
                    // Resolve weird bug where Pictures not removed from Collections
                    // but removed from disk
                    CollectionManager.cleanCurrentCollection()
                    // Refresh
                    onCurrentContentsChanged()
                    mActionModeHelper.destroyActionModeIfCan()
                }
            }
        }
    }

    /**
     * Get new data from Collection Manager.
     */
    private fun fetchContents() {
        mContents.clear()
        mContentItems.clear()

        val contentsMap = CollectionManager.getContentsMap()
        val collection = CollectionManager.currentCollection
        for ((type, coverables) in contentsMap) {
            mContents.addAll(coverables)

            // Inside albums, icons for synced pictures have to be set manually,
            // b/c pictures have no idea if they are synced with anything.
            if (collection is Album) {
                var res: Int?
                for (coverable in coverables) {
                    // If synchronized picture
                    if (coverable is Picture && !collection.ownsPictures(listOf(coverable))) {
                        res = syncIconRes
                    } else res = null
                    mContentItems.add(GridCoverableItem(coverable, res))
                }
            } else {
                val coverableItems = coverables.map { content -> GridCoverableItem(content) }
                mContentItems.addAll(coverableItems)
            }

        }
    }

    /**
     * Decide if parent exists and can be returned to
     */
    private fun returnToParentCollection(): Boolean {
        val newContents = CollectionManager.revertToParent()
        return if (newContents != null) {
            onNavigation()
            true
        } else {
            false
        }
    }

    fun onNavigation() {
        fetchContents()
        mAdapter.updateDataSet(mContentItems)
        setToolbarTitle()
    }

    /**
     * Called when collection hasn't changed, but its contents have.
     */
    private fun onCurrentContentsChanged() {
        fetchContents()
        mAdapter.updateDataSet(mContentItems)
        mActivity.notifyCollectionsChanged()
    }

    /**
     * Called when the root collection changes. Menu items and the like have to be rebuilt to
     * satisfy new collection type.
     */
    fun onTopCollectionChanged() {
        fetchContents()
        mAdapter.updateDataSet(mContentItems)
        buildDelegate()
        buildToolbar()  // updates title and delegate's menu items
    }

    override fun onAllFragmentsCreated() {
        // This fragment is always created last, so this function is not required.
    }

    override fun setClicksBlocked(doBlock: Boolean) {
        if (doBlock) {
            rvCollectionItems.visibility = View.GONE
            fab.hide()
            blocker.visibility = View.VISIBLE
        } else {
            rvCollectionItems.visibility = View.VISIBLE
            fab.show()
            blocker.visibility = View.GONE
        }
    }

    /**
     * Queries CollectionManager to get fresh data from disk, and shows it.
     */
    private fun showUpdatedContents() {
        CollectionManager.fetchNewMedia(activity!!) {
            // When done async fetch, refresh fragment to view up-to-date contents
            onCurrentContentsChanged()
            mSwipeRefreshLayout.isRefreshing = false
        }
    }

    private fun getColorInt(type: BaseActivity.ColorType): Int {
        return mActivity.getColorInt(type)
    }

    private fun toast(strId: Int) {
        return mActivity.toast(strId)
    }

    companion object Constants {
        const val SELECTED_TYPE = "CollectionViewFragment_ST"
        const val SELECTED_POSITIONS = "CVF_SP"
        const val PICTURE_ACTIVITY_REQUEST = 1
        const val FOLDER_LIST_ACTIVITY_REQUEST = 2
    }

}