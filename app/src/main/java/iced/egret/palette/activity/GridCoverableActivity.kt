package iced.egret.palette.activity

import android.content.res.Configuration
import android.os.Bundle
import android.view.ActionMode
import android.view.Menu
import android.view.View
import androidx.appcompat.widget.ActionBarContextView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.Visibility
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.SelectableAdapter
import eu.davidea.flexibleadapter.helpers.EmptyViewHelper
import iced.egret.palette.R
import iced.egret.palette.flexible.GridCoverableItem
import iced.egret.palette.flexible.ToolbarActionModeHelper
import iced.egret.palette.model.Picture
import iced.egret.palette.util.CollectionManager
import iced.egret.palette.util.Painter
import iced.egret.palette.util.StateBuilder
import kotlinx.android.synthetic.main.view_empty.*

/**
 * Basic themed activity with selectable and long-clickable GridCoverableItems.
 * Works with and assumes the existence of a valid CollectionManager, unless activity was stopped.
 * All lateinits are initialized here for safety.
 */
abstract class GridCoverableActivity : SlideActivity(), ActionMode.Callback,
        FlexibleAdapter.OnItemClickListener, FlexibleAdapter.OnItemLongClickListener {

    protected lateinit var mActionModeHelper: ToolbarActionModeHelper
    protected lateinit var mRecyclerView: RecyclerView
    protected lateinit var mAdapter: FlexibleAdapter<GridCoverableItem>
    protected lateinit var mToolbar: Toolbar

    protected var mContents = mutableListOf<Picture>()
    protected var mContentItems = mutableListOf<GridCoverableItem>()

    abstract var actionModeMenuRes: Int

    abstract fun fetchContents()
    abstract fun buildToolbar()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_grid_coverable)
        setState(savedInstanceState)
        mToolbar = findViewById(R.id.toolbar)

        fetchContents()
        buildToolbar()
        buildRecyclerView()

        colorStandardElements(mToolbar)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        mAdapter.onSaveInstanceState(outState)
        outState.putString(COLLECTION, CollectionManager.currentCollection?.path)
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

    private fun setState(savedInstanceState: Bundle?) {
        // Build state if activity restarted. Don't if entering for the first time
        // (b/c it is already built by another activity).
        // Supply saved Collection path to unwind stack properly.
        if (savedInstanceState != null) {
            val path = savedInstanceState.getString(COLLECTION)
            StateBuilder.build(this, path)
        }
    }

    private fun buildRecyclerView() {
        val orientation = resources.configuration.orientation
        val numColumns = if (orientation == Configuration.ORIENTATION_LANDSCAPE) 5 else 3
        val manager = GridLayoutManager(this, numColumns)

        mAdapter = FlexibleAdapter(mContentItems, this, true)
        initializeActionModeHelper(SelectableAdapter.Mode.IDLE, actionModeMenuRes)
        EmptyViewHelper.create(mAdapter, empty_view)
        mRecyclerView = findViewById(R.id.recyclerView)
        mRecyclerView.layoutManager = manager
        mRecyclerView.adapter = mAdapter
    }

    /**
     * Straight from https://github.com/davideas/FlexibleAdapter/wiki/5.x-%7C-ActionModeHelper
     */
    private fun initializeActionModeHelper(@Visibility.Mode mode: Int, menuRes: Int) {
        mActionModeHelper = object : ToolbarActionModeHelper(mAdapter, menuRes, this as ActionMode.Callback) {
            override fun updateContextTitle(count: Int) {
                mActionMode?.title = if (count == 1) getString(R.string.action_selected_one, count)
                else getString(R.string.action_selected_many, count)
            }
        }.withDefaultMode(mode)
    }

    /**
     * From https://stackoverflow.com/a/45955606
     */
    override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
        val color = ContextCompat.getColor(this, R.color.colorActionMode)
        window.decorView.findViewById<ActionBarContextView?>(R.id.action_mode_bar)
                ?.setBackgroundColor(color)
        window.statusBarColor = color
        window.navigationBarColor = color
        return true
    }

    override fun onPrepareActionMode(p0: ActionMode?, p1: Menu?): Boolean {
        return true
    }

    override fun onDestroyActionMode(mode: ActionMode?) {
        val primaryColor = getColorInt(ColorType.PRIMARY)
        window.statusBarColor = Painter.getMaterialDark(primaryColor)
        window.navigationBarColor = Painter.getMaterialDark(primaryColor)
        mContentItems.map { item -> item.setSelection(false) }
    }

    /**
     * @return True if click should be handled by selection mode, false otherwise.
     */
    override fun onItemClick(view: View?, position: Int): Boolean {
        return if (mAdapter.mode != SelectableAdapter.Mode.IDLE) {
            mActionModeHelper.onClick(position, mContentItems[position])
        }
        else {
            onIdleItemClick(position)
            false
        }
    }

    abstract fun onIdleItemClick(position: Int)

    override fun onItemLongClick(position: Int) {
        mActionModeHelper.onLongClick(mToolbar, position, mContentItems[position])
    }

    companion object SaveDataKeys {
        const val COLLECTION = "current-collection"
    }

}