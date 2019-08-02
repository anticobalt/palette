package iced.egret.palette.fragment

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.view.*
import androidx.appcompat.app.AppCompatActivity
import androidx.documentfile.provider.DocumentFile
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.transition.Visibility
import com.google.android.material.floatingactionbutton.FloatingActionButton
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.SelectableAdapter
import iced.egret.palette.R
import iced.egret.palette.activity.BaseActivity
import iced.egret.palette.activity.MainActivity
import iced.egret.palette.activity.PICTURE_ACTIVITY_REQUEST
import iced.egret.palette.delegate.AlbumViewDelegate
import iced.egret.palette.delegate.CollectionViewDelegate
import iced.egret.palette.delegate.FolderViewDelegate
import iced.egret.palette.flexible.CoverableItem
import iced.egret.palette.flexible.GridCoverableItem
import iced.egret.palette.flexible.ToolbarActionModeHelper
import iced.egret.palette.model.Album
import iced.egret.palette.model.Coverable
import iced.egret.palette.model.Folder
import iced.egret.palette.model.Picture
import iced.egret.palette.util.CollectionManager
import iced.egret.palette.util.DialogGenerator
import iced.egret.palette.util.Painter
import kotlinx.android.synthetic.main.appbar_list_fragment.view.*
import kotlinx.android.synthetic.main.fragment_view_collection.*
import java.io.File

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
class CollectionViewFragment :
        ListFragment(),
        ActionMode.Callback,
        FlexibleAdapter.OnItemClickListener,
        FlexibleAdapter.OnItemLongClickListener,
        SwipeRefreshLayout.OnRefreshListener {

    private lateinit var mMaster: MainActivity
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
    private lateinit var mActionModeHelper: ToolbarActionModeHelper
    private var mSelectedContentType: String? = null
    private var mReturningFromStop = false

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        super.onCreateView(inflater, container, savedInstanceState)
        mRootView = inflater.inflate(R.layout.fragment_view_collection, container, false)

        mCollectionRecyclerView = mRootView!!.findViewById(R.id.rvCollectionItems)
        mFloatingActionButton = mRootView!!.findViewById(R.id.fab)
        mSwipeRefreshLayout = mRootView!!.findViewById(R.id.swipeRefreshLayout)
        mMaster = activity as MainActivity

        fetchContents()
        buildDelegate()
        buildToolbar()
        buildRecyclerView()
        mSwipeRefreshLayout.setOnRefreshListener(this)
        mFloatingActionButton.setOnClickListener { onFabClick() }

        return mRootView

    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        mMaster.notifyFragmentCreationFinished(this)
    }

    /**
     * Called after MainActivity done loading collection, so safe to restore state here.
     */
    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)

        //If selected content type is saved, restore it, otherwise get out
        if (savedInstanceState == null) return
        val contentType = savedInstanceState.getString(selectedType, "")
        if (contentType.isEmpty()) return

        // Isolate internal contents and self
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

    /**
     * Called after onActivityResult() if applicable.
     */
    override fun onResume() {
        super.onResume()
        styleFab()
        // Never refresh if currently selecting stuff.
        // If not selecting, refresh if onCreate() not called.
        if (mSelectedContentType != null) {
            mMaster.isolateFragment(this)
            return
        }
        if (mReturningFromStop) {
            showUpdatedContents()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        mAdapter.onSaveInstanceState(outState)
        outState.putString(selectedType, mSelectedContentType)
        super.onSaveInstanceState(outState)
    }

    override fun onStop() {
        super.onStop()
        // Assume need to update onStart().
        mReturningFromStop = true
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PICTURE_ACTIVITY_REQUEST) {
            if (resultCode == AppCompatActivity.RESULT_OK) {
                // Changes occurred: notify update; self-update occurs automatically in onResume()
                mReturningFromStop = true
            }
        }
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
            mMaster.togglePanel()
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

    private fun styleFab() {
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
        val manager = GridLayoutManager(activity, numColumns)

        fetchContents()
        mAdapter = FlexibleAdapter(mContentItems, this, true)
        initializeActionModeHelper(SelectableAdapter.Mode.IDLE)

        mCollectionRecyclerView.layoutManager = manager
        mCollectionRecyclerView.adapter = mAdapter
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
            val updates = CollectionManager.launch(coverable, relativePosition, this, PICTURE_ACTIVITY_REQUEST)
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
        mDelegate.onOptionsItemSelected(item, context!!, CollectionManager.currentCollection!!)
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

        mMaster.colorActionMode()
        mMaster.isolateFragment(this)

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

        // sanity check that selected type is valid
        CollectionManager.getContentsMap()[mSelectedContentType] ?: return false

        val coverables = mAdapter.selectedPositions.map { index ->
            CollectionManager.getContentsMap()[mSelectedContentType]!![index]
        }

        val typePlural = mSelectedContentType!!.toLowerCase()
        val typeSingular = typePlural.dropLast(1)  // only works on s-appending plurals
        val typeString = if (coverables.size > 1) typePlural else typeSingular

        when (item.itemId) {
            R.id.actionSelectAll -> {
                selectAll()
            }
            R.id.albumActions -> {
                // No changes, don't refresh, so exit immediately.
                // Must return true for submenus to popup.
                return true
            }
            R.id.actionAddToAlbum -> {
                DialogGenerator.addToAlbum(context!!) { indices, albums ->
                    val destinations = albums.filterIndexed { index, _ -> indices.contains(index) }
                    val albumString = if (destinations.size > 1) "albums" else "album"
                    CollectionManager.addContentToAllAlbums(coverables, destinations)
                    toast("Added ${coverables.size} $typeString to ${destinations.size} $albumString.")
                    mMaster.notifyCollectionsChanged()
                    mActionModeHelper.destroyActionModeIfCan()
                }
            }
            R.id.actionMove -> {
                DialogGenerator.moveTo(context!!) {
                    @Suppress("UNCHECKED_CAST")  // assume internal consistency
                    movePictures(coverables as List<Picture>, it, typeString)
                    onCurrentContentsChanged()
                    mActionModeHelper.destroyActionModeIfCan()
                }
            }
            R.id.actionDelete -> {
                when (mSelectedContentType) {
                    CollectionManager.PICTURE_KEY -> {
                        DialogGenerator.moveToRecycleBin(context!!, typeString) {
                            @Suppress("UNCHECKED_CAST")  // assume internal consistency
                            recyclePictures(coverables as List<Picture>, typeString)
                            onCurrentContentsChanged()
                            mActionModeHelper.destroyActionModeIfCan()
                        }
                    }
                }

            }
            else -> {}
        }

        mDelegate.onActionItemClicked(mode, item, mAdapter, context!!, mSelectedContentType!!)
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
        mMaster.restoreAllFragments()

        mode.menu.findItem(R.id.albumActions).isVisible = false
        mode.menu.findItem(R.id.actionAddToAlbum).isVisible = false
        mode.menu.findItem(R.id.actionMove).isVisible = false
        mode.menu.findItem(R.id.actionDelete).isVisible = false

        mDelegate.onDestroyActionMode(mode)
    }

    fun onDelegateAlert(alert: CollectionViewDelegate.ActionAlert) {
        if (alert.message.isNotEmpty()) toast(alert.message)

        if (alert.success) {
            // Update everything (short of remaking menus) b/c delegate doesn't specify what exactly changed.
            fetchContents()
            mAdapter.updateDataSet(mContentItems)
            setToolbarTitle()
            mMaster.notifyCollectionsChanged()
        }
    }

    private fun selectAll() {
        mAdapter.currentItems.map { item -> item.setSelection(true) }
        mAdapter.selectAll()
        mActionModeHelper.updateContextTitle(mAdapter.selectedItemCount)
    }

    private fun movePictures(pictures: List<Picture>, destination: File, typeString: String) {
        val failedCounter = CollectionManager.movePictures(pictures, destination,
                getSdCardDocumentFile(), activity!!.contentResolver) { sourceFile, movedFile ->
            broadcastMediaChanged(sourceFile)
            broadcastMediaChanged(movedFile)
        }
        if (failedCounter > 0) toast("Failed to move $failedCounter!")
        else toast("${pictures.size} $typeString moved")
    }

    private fun recyclePictures(pictures: List<Picture>, typeString: String) {
        val failedCounter = CollectionManager.movePicturesToRecycleBin(pictures, getSdCardDocumentFile()) {
            // If moved a Picture successfully, broadcast change
            broadcastMediaChanged(it)
        }
        if (failedCounter > 0) toast("Failed to move $failedCounter!")
        else toast("${pictures.size} $typeString moved to recycle bin")
    }

    /**
     * Get new data from Collection Manager.
     */
    private fun fetchContents() {
        mContents.clear()
        mContentItems.clear()

        val contentsMap = CollectionManager.getContentsMap()
        for ((type, coverables) in contentsMap) {
            val coverableItems = coverables.map { content -> GridCoverableItem(content) }
            mContents.addAll(coverables)
            mContentItems.addAll(coverableItems)
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

    private fun onNavigation() {
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
        mMaster.notifyCollectionsChanged()
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
        CollectionManager.fetchFromStorage(activity!!) {
            // When done async fetch, refresh fragment to view up-to-date contents
            onCurrentContentsChanged()
            mSwipeRefreshLayout.isRefreshing = false
        }
    }

    private fun getSdCardDocumentFile(): DocumentFile? {
        return mMaster.getSdCardDocumentFile()
    }

    private fun broadcastMediaChanged(file: File) {
        mMaster.broadcastMediaChanged(file)
    }

    private fun toast(message: String) {
        mMaster.toast(message)
    }

    private fun getColorInt(type: BaseActivity.ColorType) : Int {
        return mMaster.getColorInt(type)
    }

    companion object SaveDataKeys {
        const val selectedType = "CollectionViewFragment_ST"
    }

}