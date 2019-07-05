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
import iced.egret.palette.R
import iced.egret.palette.activity.MainActivity
import iced.egret.palette.model.Album
import iced.egret.palette.model.Collection
import iced.egret.palette.model.Folder
import iced.egret.palette.recyclerview_component.CoverableItem
import iced.egret.palette.recyclerview_component.PinnedCollectionsItem
import iced.egret.palette.recyclerview_component.ToolbarActionModeHelper
import iced.egret.palette.util.CollectionManager
import iced.egret.palette.util.DialogGenerator
import iced.egret.palette.util.MainFragmentManager
import iced.egret.palette.util.Painter
import kotlinx.android.synthetic.main.fragment_pinned_collections.*

class PinnedCollectionsFragment :
        MainFragment(),
        ActionMode.Callback,
        FlexibleAdapter.OnItemClickListener,
        FlexibleAdapter.OnItemLongClickListener {

    companion object SaveDataKeys {
        const val selectedType = "PinnedCollectionFragment_ST"
    }

    private var mRootView : View? = null
    private lateinit var mRecyclerView : RecyclerView
    private var mBlockableFragments = mutableListOf<MainFragment>()
    private lateinit var mDefaultToolbar : Toolbar

    private var mCollections = mutableListOf<Collection>()
    private var mCollectionItems = mutableListOf<PinnedCollectionsItem>()

    lateinit var adapter: FlexibleAdapter<PinnedCollectionsItem>
    private lateinit var mActionModeHelper: ToolbarActionModeHelper
    private var mSelectedContentType : String? = null

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
        outState.putString(selectedType, mSelectedContentType)
        super.onSaveInstanceState(outState)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)

        if (savedInstanceState != null) {
            // If selected content type is saved, restore it, otherwise get out
            val contentType = savedInstanceState.getString(selectedType, "")
            if (contentType.isEmpty()) return
            mSelectedContentType = contentType
            isolateContent(mSelectedContentType!!)

            // Must restore adapter and helper AFTER type isolation to keep position ints consistent
            adapter.onRestoreInstanceState(savedInstanceState)
            mActionModeHelper.restoreSelection(mDefaultToolbar)

            // Re-select all previously selected items
            for (i in 0 until adapter.currentItems.size) {
                if (i in adapter.selectedPositionsAsSet) {
                    adapter.currentItems[i].setSelection(true)
                }
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
        mDefaultToolbar.setTitle(R.string.app_name)
        mDefaultToolbar.inflateMenu(R.menu.menu_pinned_collections)
        mDefaultToolbar.setOnMenuItemClickListener {
            onOptionsItemSelected(it)
        }

    }

    private fun buildRecyclerView() {
        fetchContents()
        mRecyclerView.layoutManager = GridLayoutManager(activity, 1)
        adapter = FlexibleAdapter(mCollectionItems, this, true)
        mRecyclerView.adapter = adapter
    }

    /**
     * Straight from https://github.com/davideas/FlexibleAdapter/wiki/5.x-%7C-ActionModeHelper
     */
    private fun initializeActionModeHelper(@Visibility.Mode mode: Int) {
        // this = ActionMode.Callback instance
        mActionModeHelper = object : ToolbarActionModeHelper(adapter, R.menu.menu_pinned_collections_edit, this) {
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

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        // Will re-block some fragments, depending on the order they were built in.
        for (fragment in mBlockableFragments) {
            fragment.setClicksBlocked(true)
        }

        // Make items visible depending on selected content.
        // Painting has to be done here for ActionMode icons, because XML app:iconTint doesn't work.
        when (mSelectedContentType) {
            CollectionManager.FOLDER_KEY -> {
                val item = menu.findItem(R.id.actionPinnedCollectionsHide)
                item.isVisible = true
                Painter.paintDrawable(item.icon)
            }
            CollectionManager.ALBUM_KEY -> {
                val item = menu.findItem(R.id.actionPinnedCollectionsDeleteAlbum)
                item.isVisible = true
                Painter.paintDrawable(item.icon)
            }
        }

        return true
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.actionPinnedCollectionsDeleteAlbum -> {
                val callback = callback@{
                    CollectionManager.deleteAlbumsByRelativePosition(adapter.selectedPositions)
                    refreshFragment()
                    MainFragmentManager.notifyAlbumDeletedFromPinnedCollections()
                }
                DialogGenerator.deleteAlbum(context ?: return false, onConfirm = callback)
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
        mCollectionItems.map { item -> item.setSelection(false)}
        restoreAllContent()
        mSelectedContentType = null  // nothing isolated
        (activity as MainActivity).restoreAllFragments()

        mode.menu.findItem(R.id.actionPinnedCollectionsHide).isVisible = false
        mode.menu.findItem(R.id.actionPinnedCollectionsDeleteAlbum).isVisible = false
    }

    override fun onItemClick(view: View, absolutePosition: Int): Boolean {
        val clickedItem = adapter.getItem(absolutePosition) as? CoverableItem
                ?: return false

        return if (adapter.mode != SelectableAdapter.Mode.IDLE) {
            mActionModeHelper.onClick(absolutePosition, clickedItem)
        }
        else {
            val fragmentIndex = MainFragmentManager.COLLECTION_CONTENTS
            val fragment = MainFragmentManager.fragments[fragmentIndex] as CollectionViewFragment
            fragment.activity?.findViewById<SlidingPaneLayout>(R.id.slidingPaneMain)?.closePane()

            val coverable = mCollections[absolutePosition]
            CollectionManager.clearStack()
            CollectionManager.launch(coverable)  // == true
            fragment.setDefaultToolbarTitle()
            fragment.refreshFragment()
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
        val relativePosition : Int

        // Isolate the content type BEFORE ActionMode is created, so that the correct
        // relative position can be noted by the ActionModeHelper (instead of global position,
        // which unnecessarily accounts for temporarily remove types)
        if (mSelectedContentType == null) {
            mSelectedContentType = inferContentType(mCollections[absolutePosition]) ?: return
            isolateContent(mSelectedContentType!!)
            // adapter only holds one type now, so global == relative
            relativePosition = adapter.getGlobalPositionOf(mCollectionItems[absolutePosition])
        }
        else {
            relativePosition = absolutePosition
        }
        mActionModeHelper.onLongClick(mDefaultToolbar, relativePosition, mCollectionItems[absolutePosition])
    }

    private fun inferContentType(collection: Collection) : String? {
        return when (collection) {
            is Folder -> CollectionManager.FOLDER_KEY
            is Album -> CollectionManager.ALBUM_KEY
            else -> null
        }
    }

    /**
     * Get one collection with type name provided, if one exists
     */
    private fun getInstanceOfType(typeName: String) : Collection? {
        val type = when (typeName) {
            CollectionManager.FOLDER_KEY -> Folder::class.java
            CollectionManager.ALBUM_KEY -> Album::class.java
            else -> return null
        }
        return mCollections.find {collection -> collection.javaClass == type }
    }

    /**
     * Get all collections with same type as parameter, including the parameter
     */
    private fun getAllOfSameType(collection: Collection) : List<Collection> {
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
                adapter.removeRange(adapterOffset, removeTypeList.size)
                // next type is now at position adapterOffset after remove, so don't increment
                index += removeTypeList.size
            }
        }
    }

    private fun restoreAllContent() {
        for (i in 0 until mCollectionItems.size) {
            val item = mCollectionItems[i]
            if (!adapter.contains(item)) {
                adapter.addItem(i, item)
            }
        }
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
        return true
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
            else -> super.onOptionsItemSelected(item)
        }

        return true
    }


    private fun onCreateNewAlbum(charSequence: CharSequence) {
        CollectionManager.createNewAlbum(charSequence.toString())
        refreshFragment()
    }

    fun refreshFragment() {
        fetchContents()
        adapter.updateDataSet(mCollectionItems)
        mActionModeHelper.destroyActionModeIfCan()
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
            val contentItem = PinnedCollectionsItem(folder)
            mCollectionItems.add(contentItem)
        }

        // Fetch albums
        for (album in CollectionManager.albums) {
            mCollections.add(album)
            val contentItem = PinnedCollectionsItem(album)
            mCollectionItems.add(contentItem)
        }

    }

}