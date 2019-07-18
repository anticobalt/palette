package iced.egret.palette.activity

import android.content.res.Configuration
import android.os.Bundle
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.Visibility
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.SelectableAdapter
import iced.egret.palette.R
import iced.egret.palette.model.Picture
import iced.egret.palette.recyclerview_component.CollectionViewItem
import iced.egret.palette.recyclerview_component.ToolbarActionModeHelper
import iced.egret.palette.util.CollectionManager
import iced.egret.palette.util.DialogGenerator
import iced.egret.palette.util.Painter
import iced.egret.palette.util.Storage

class RecycleBinActivity : BaseActivity(), ActionMode.Callback,
        FlexibleAdapter.OnItemClickListener, FlexibleAdapter.OnItemLongClickListener {

    private lateinit var mActionModeHelper: ToolbarActionModeHelper
    private lateinit var mRecyclerView: RecyclerView
    private lateinit var mAdapter: FlexibleAdapter<CollectionViewItem>
    private lateinit var mToolbar: Toolbar

    private var mContents = mutableListOf<Picture>()
    private var mContentItems = mutableListOf<CollectionViewItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recycle_bin)
        fetchContents()
        buildActionBar()
        buildRecyclerView()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_recycle_bin, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        paintMenuItems(menu)
        return super.onPrepareOptionsMenu(menu)
    }

    private fun paintMenuItems(menu: Menu) {
        Painter.paintDrawable(menu.findItem(R.id.actionEmpty).icon)
    }

    private fun fetchContents() {
        mContents.clear()
        mContentItems.clear()
        mContents.addAll(Storage.recycleBin.contents)
        mContentItems.addAll(mContents.map { content -> CollectionViewItem(content) })
    }

    /**
     * Get RecycleBin contents from storage, compare them with current contents,
     * and discard those not found in storage.
     */
    private fun discardOutdatedContents() {
        val freshUris = Storage.recycleBin.contents.map{ c -> c.uri}.toSet()
        val toDiscardItems = mutableListOf<CollectionViewItem>()
        val toDiscardContents = mutableListOf<Picture>()

        for (i in 0 until mContents.size) {
            if (mContents[i].uri !in freshUris) {
                toDiscardItems.add(mContentItems[i])
                toDiscardContents.add(mContents[i])
            }
        }
        mContentItems.removeAll(toDiscardItems)
        mContents.removeAll(toDiscardContents)
    }

    private fun refresh() {
        // Getting contents again from RecycleBin is more taxing than just tracking
        // removed items inside the activity, but keeps the storage as the single source
        // of truth, and prevents potential sync errors.
        discardOutdatedContents()
        mAdapter.updateDataSet(mContentItems)
        mActionModeHelper.destroyActionModeIfCan()
    }

    private fun buildActionBar() {
        mToolbar = findViewById(R.id.toolbar)
        setSupportActionBar(mToolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun buildRecyclerView() {
        if (mContents.isEmpty()) return

        val orientation = resources.configuration.orientation
        val numColumns = if (orientation == Configuration.ORIENTATION_LANDSCAPE) 5 else 3
        val manager = GridLayoutManager(this, numColumns)

        mAdapter = FlexibleAdapter(mContentItems, this, true)
        initializeActionModeHelper(SelectableAdapter.Mode.IDLE)
        mRecyclerView = findViewById(R.id.recyclerView)
        mRecyclerView.layoutManager = manager
        mRecyclerView.adapter = mAdapter
    }

    /**
     * @return True to consume, false if to continue.
     */
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    /**
     * Straight from https://github.com/davideas/FlexibleAdapter/wiki/5.x-%7C-ActionModeHelper
     */
    private fun initializeActionModeHelper(@Visibility.Mode mode: Int) {
        mActionModeHelper = object : ToolbarActionModeHelper(mAdapter, R.menu.menu_recycle_bin_edit, this as ActionMode.Callback) {
            override fun updateContextTitle(count: Int) {
                mActionMode?.title = if (count == 1) getString(R.string.action_selected_one, count)
                else getString(R.string.action_selected_many, count)
            }
        }.withDefaultMode(mode)
    }

    // Return true to continue with Action Mode
    override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {

        val selected = mAdapter.selectedPositions.map {i -> mContents[i]}

        val typePlural = CollectionManager.PICTURE_KEY.toLowerCase()
        val typeSingular = typePlural.dropLast(1)  // only works on s-appending plurals
        val typeString = if (selected.size > 1) typePlural else typeSingular

        when (item.itemId) {
            R.id.actionRestore -> {
                DialogGenerator.restore(this, typeString) {
                    restorePictures(selected, typeString)
                    refresh()
                }
            }
            R.id.actionDelete -> {
                DialogGenerator.delete(this, typeString) {
                    deletePictures(selected, typeString)
                    refresh()
                }
            }
        }
        return true
    }

    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        Painter.paintDrawable(menu.findItem(R.id.actionRestore).icon)
        Painter.paintDrawable(menu.findItem(R.id.actionDelete).icon)
        return true
    }

    override fun onPrepareActionMode(p0: ActionMode?, p1: Menu?): Boolean {
        return true
    }

    override fun onDestroyActionMode(mode: ActionMode?) {
        mContentItems.map { item -> item.setSelection(false) }
    }

    /**
     * @return True if click should be handled by selection mode, false otherwise.
     */
    override fun onItemClick(view: View?, position: Int): Boolean {
        return mActionModeHelper.onClick(position, mContentItems[position])
    }

    override fun onItemLongClick(position: Int) {
        mActionModeHelper.onLongClick(mToolbar, position, mContentItems[position])
    }

    override fun onSaveInstanceState(outState: Bundle) {
        mAdapter.onSaveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)
        mAdapter.onRestoreInstanceState(savedInstanceState)
        mActionModeHelper.restoreSelection(mToolbar)

        // Re-select all previously selected items
        for (i in 0 until mAdapter.currentItems.size) {
            if (i in mAdapter.selectedPositionsAsSet) {
                mAdapter.currentItems[i].setSelection(true)
            }
        }
    }

    private fun restorePictures(pictures: List<Picture>, typeString: String) {
        val failCounter = CollectionManager.restorePicturesFromRecycleBin(
                pictures, getSdCardDocumentFile(), contentResolver) {
            broadcastNewMedia(it)
        }
        if (failCounter > 0) toast("Failed to restore $failCounter $typeString!")
        else toast("${pictures.size} ${typeString.capitalize()} restored")
    }

    private fun deletePictures(pictures: List<Picture>, typeString: String) {
        val failCounter = CollectionManager.deletePictures(pictures)
        if (failCounter > 0) toast("Failed to delete $failCounter $typeString!")
        else toast("${pictures.size} ${typeString.capitalize()} permanently deleted")
    }
}
