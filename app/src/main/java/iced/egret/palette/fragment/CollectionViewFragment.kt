package iced.egret.palette.fragment

import android.os.Bundle
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
import iced.egret.palette.R
import iced.egret.palette.activity.MainActivity
import iced.egret.palette.model.Album
import iced.egret.palette.model.Coverable
import iced.egret.palette.model.Folder
import iced.egret.palette.model.Picture
import iced.egret.palette.recyclerview_component.CollectionViewItem
import iced.egret.palette.recyclerview_component.CoverableItem
import iced.egret.palette.recyclerview_component.ToolbarActionModeHelper
import iced.egret.palette.util.CollectionManager
import iced.egret.palette.util.DialogGenerator
import iced.egret.palette.util.MainFragmentManager
import iced.egret.palette.util.Painter
import kotlinx.android.synthetic.main.fragment_view_collection.*
import kotlinx.android.synthetic.main.fragment_view_collection.view.*


class CollectionViewFragment :
        MainFragment(),
        ActionMode.Callback,
        FlexibleAdapter.OnItemClickListener,
        FlexibleAdapter.OnItemLongClickListener {

    companion object SaveDataKeys {
        const val selectedType = "CollectionViewFragment_ST"
    }

    private lateinit var mActionModeHelper: ToolbarActionModeHelper

    private var mRootView : View? = null
    private lateinit var mDefaultToolbar : Toolbar
    private lateinit var mCollectionRecyclerView : RecyclerView
    private lateinit var mFloatingActionButton : FloatingActionButton

    private var mContents = mutableListOf<Coverable>()
    private var mContentItems = mutableListOf<CollectionViewItem>()
    lateinit var adapter: FlexibleAdapter<CollectionViewItem>
    private var mSelectedContentType : String? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {

        mRootView = inflater.inflate(R.layout.fragment_view_collection, container, false)
        mCollectionRecyclerView = mRootView!!.findViewById(R.id.rvCollectionItems)
        mFloatingActionButton = mRootView!!.findViewById(R.id.fab)

        fetchContents()

        mFloatingActionButton.setOnClickListener {
            onFabClick()
        }

        buildToolbar()
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

    override fun onSaveInstanceState(outState: Bundle) {
        adapter.onSaveInstanceState(outState)
        outState.putString(selectedType, mSelectedContentType)
        super.onSaveInstanceState(outState)
    }

    override fun onAllFragmentsCreated() {
        // This fragment is always created last, so this function is not required.
    }

    /**
     * Makes default toolbar and fills with items and title
     */
    private fun buildToolbar() {
        mDefaultToolbar = mRootView!!.findViewById(R.id.toolbarViewCollection)
        mDefaultToolbar.inflateMenu(R.menu.menu_view_collection)
        mDefaultToolbar.setOnMenuItemClickListener {
            onOptionsItemSelected(it)
        }
        setToolbarTitle()
        mDefaultToolbar.tvToolbarTitle.setOnLongClickListener {
            if (CollectionManager.currentCollection != null) {
                DialogGenerator.showCollectionDetails(context!!, CollectionManager.currentCollection!!)
            }
            true
        }
    }

    /**
     * Sets toolbar's title to current Collection name
     */
    fun setToolbarTitle(title: String = "") {
        mDefaultToolbar.tvToolbarTitle.text = if (title.isEmpty()) {
            val collection = CollectionManager.currentCollection
            collection?.path?.split("/")?.joinToString(" / ") ?: ""
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
            adapter = FlexibleAdapter(mContentItems, this, true)

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

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        (activity as MainActivity).isolateFragment(this)

        val albumActions = menu.findItem(R.id.actionViewCollectionAlbumActions)
        val addToAlbum = menu.findItem(R.id.actionViewCollectionAddToAlbum)
        val delete = menu.findItem(R.id.actionViewCollectionDelete)
        val removeFromAlbum = menu.findItem(R.id.actionViewCollectionRemoveFromAlbum)

        // Make items visible depending on selected content.
        // Painting has to be done here for ActionMode icons, because XML app:iconTint doesn't work.
        when (mSelectedContentType) {
            CollectionManager.FOLDER_KEY,
            CollectionManager.PICTURE_KEY -> {
                albumActions.isVisible = true; Painter.paintDrawable(albumActions.icon)
                addToAlbum.isVisible = true; Painter.paintDrawable(addToAlbum.icon)
                delete.isVisible = true; Painter.paintDrawable(delete.icon)
                if (CollectionManager.currentCollection is Album) {
                    removeFromAlbum.isVisible = true; Painter.paintDrawable(removeFromAlbum.icon)
                }
            }
            CollectionManager.ALBUM_KEY -> {
                delete.isVisible = true; Painter.paintDrawable(delete.icon)
            }
        }
        return true
    }

    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {

        fun refresh() {
            refreshFragment()
            MainFragmentManager.notifyAlbumUpdateFromCollectionView()  // to update cover
        }

        fun toast(message: String) {
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }

        // sanity check that selected type is valid
        CollectionManager.getContentsMap()[mSelectedContentType] ?: return false

        val coverables = adapter.selectedPositions.map {
            index -> CollectionManager.getContentsMap()[mSelectedContentType]!![index]
        }

        val typePlural = mSelectedContentType!!.toLowerCase()
        val typeSingular = typePlural.dropLast(1)  // only works on s-appending plurals
        val typeString = if (coverables.size > 1) typePlural else typeSingular

        when (item.itemId) {
            R.id.actionViewCollectionAlbumActions -> {
                // No changes, don't refresh, so exit immediately.
                // Must return true for submenus to popup.
                return true
            }
            R.id.actionViewCollectionAddToAlbum -> {
                DialogGenerator.addToAlbum(context!!) { indices, albums ->
                    val destinations = albums.filterIndexed { index, _ -> indices.contains(index) }
                    val albumString = if (destinations.size > 1) "albums" else "album"
                    CollectionManager.addContentToAllAlbums(coverables, destinations)
                    toast("Added ${coverables.size} $typeString to ${destinations.size} $albumString.")
                    refresh()
                }
            }
            R.id.actionViewCollectionRemoveFromAlbum -> {
                DialogGenerator.removeFromAlbum(context!!, typeString) {
                    CollectionManager.removeContentFromCurrentAlbum(coverables)
                    toast("Removed ${coverables.size} $typeString.")
                    refresh()
                }
            }
            R.id.actionViewCollectionDelete -> {
                DialogGenerator.deleteAlbum(context!!) {
                    CollectionManager.deleteAlbumsByRelativePosition(adapter.selectedPositions, deleteFromCurrent = true)
                    toast("Deleted ${adapter.selectedItemCount} $typeString")
                    refresh()
                }
            }
            else -> {}
        }
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
        return true
    }

    override fun onDestroyActionMode(mode: ActionMode) {
        // ToolbarActionModeHelper doesn't have references to CoverableItems,
        // so can't clear all selections visually
        mContentItems.map {item -> item.setSelection(false)}
        restoreAllContent()
        mSelectedContentType = null  // nothing isolated
        (activity as MainActivity).restoreAllFragments()

        mode.menu.findItem(R.id.actionViewCollectionAlbumActions).isVisible = false
        mode.menu.findItem(R.id.actionViewCollectionAddToAlbum).isVisible = false
        mode.menu.findItem(R.id.actionViewCollectionRemoveFromAlbum).isVisible = false
        mode.menu.findItem(R.id.actionViewCollectionDelete).isVisible = false
    }

    private fun inferContentType(content: Coverable) : String? {
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
                adapter.removeRange(adapterOffset, removeTypeList.size)
                // next type is now at position adapterOffset after remove, so don't increment
                index += removeTypeList.size
            }
        }
    }

    private fun restoreAllContent() {
        for (i in 0 until mContentItems.size) {
            val item = mContentItems[i]
            if (!adapter.contains(item)) {
                adapter.addItem(i, item)
            }
        }
    }

    override fun onItemClick(view: View, absolutePosition: Int): Boolean {
        val clickedItem = adapter.getItem(absolutePosition) as? CoverableItem
                ?: return true

        return if (adapter.mode != SelectableAdapter.Mode.IDLE) {
            mActionModeHelper.onClick(absolutePosition, clickedItem)
        }
        else {
            val coverable = mContents[absolutePosition]
            val relativePosition =
                    CollectionManager.getContentsMap()[inferContentType(coverable)]?.indexOf(coverable)
                            ?: return false
            val updates = CollectionManager.launch(coverable, position = relativePosition, c = this.context)
            if (updates) {
                setToolbarTitle()
                refreshFragment()
            }
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
        val relativePosition : Int

        // Isolate the content type BEFORE ActionMode is created, so that the correct
        // relative position can be noted by the ActionModeHelper (instead of global position,
        // which unnecessarily accounts for temporarily remove types)
        if (mSelectedContentType == null) {
            mSelectedContentType = inferContentType(mContents[absolutePosition]) ?: return
            isolateContent(mSelectedContentType!!)
            // adapter only holds one type now, so global == relative
            relativePosition = adapter.getGlobalPositionOf(mContentItems[absolutePosition])
        }
        else {
            relativePosition = absolutePosition
        }
        mActionModeHelper.onLongClick(mDefaultToolbar, relativePosition, mContentItems[absolutePosition])
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
        return when (item.itemId) {
            R.id.actionViewCollectionSettings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }


    /**
     * Adds new album to current Collection
     */
    private fun createNewAlbum(name: CharSequence) {
        val pos = CollectionManager.createNewAlbum(name.toString(), addToCurrent = true)
        refreshFragment()
    }

    /**
     * @return handled here (true) or not (false)
     */
    override fun onBackPressed() : Boolean {
        return returnToParentCollection()
    }

    /**
     * Decide if parent exists and can be returned to
     */
    private fun returnToParentCollection() : Boolean {
        val newContents = CollectionManager.revertToParent()
        return if (newContents != null){
            setToolbarTitle()
            refreshFragment()
            true
        }
        else {
            false
        }
    }

    fun refreshFragment() {
        fetchContents()
        adapter.updateDataSet(mContentItems)
        mActionModeHelper.destroyActionModeIfCan()
    }

    /**
     * Get new data from Collection Manager.
     */
    private fun fetchContents() {
        mContents.clear()
        mContentItems.clear()

        val contentsMap = CollectionManager.getContentsMap()
        for ((type, coverables) in contentsMap) {
            val coverableItems = coverables.map {content -> CollectionViewItem(content)}
            mContents.addAll(coverables)
            mContentItems.addAll(coverableItems)
        }
    }

}