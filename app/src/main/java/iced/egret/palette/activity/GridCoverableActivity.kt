package iced.egret.palette.activity

import android.content.res.Configuration
import android.os.Bundle
import android.view.ActionMode
import android.view.Menu
import android.view.View
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.Visibility
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.SelectableAdapter
import iced.egret.palette.R
import iced.egret.palette.flexible.GridCoverableItem
import iced.egret.palette.flexible.ToolbarActionModeHelper
import iced.egret.palette.model.Picture

/**
 * Basic themed activity with selectable and long-clickable GridCoverableItems.
 */
abstract class GridCoverableActivity : BasicThemedActivity(), ActionMode.Callback,
        FlexibleAdapter.OnItemClickListener, FlexibleAdapter.OnItemLongClickListener {

    protected lateinit var mActionModeHelper: ToolbarActionModeHelper
    protected lateinit var mRecyclerView: RecyclerView
    protected lateinit var mAdapter: FlexibleAdapter<GridCoverableItem>
    protected lateinit var mToolbar: Toolbar

    protected var mContents = mutableListOf<Picture>()
    protected var mContentItems = mutableListOf<GridCoverableItem>()

    abstract var menuRes: Int

    abstract fun fetchContents()
    abstract fun buildToolbar()
    abstract fun styleToolbar()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_recycle_bin)
        fetchContents()
        buildToolbar()
        buildRecyclerView()
    }

    override fun onResume() {
        super.onResume()
        styleToolbar()
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

    private fun buildRecyclerView() {
        val orientation = resources.configuration.orientation
        val numColumns = if (orientation == Configuration.ORIENTATION_LANDSCAPE) 5 else 3
        val manager = GridLayoutManager(this, numColumns)

        mAdapter = FlexibleAdapter(mContentItems, this, true)
        initializeActionModeHelper(SelectableAdapter.Mode.IDLE, menuRes)
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

}