package iced.egret.palette.activity

import android.content.res.Configuration
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.view.ActionMode
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.Visibility
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.SelectableAdapter
import eu.davidea.flexibleadapter.helpers.ActionModeHelper
import iced.egret.palette.R
import iced.egret.palette.model.Picture
import iced.egret.palette.recyclerview_component.CollectionViewItem
import iced.egret.palette.util.CollectionManager
import iced.egret.palette.util.Painter

class RecycleBinActivity : AppCompatActivity(), ActionMode.Callback,
        FlexibleAdapter.OnItemClickListener, FlexibleAdapter.OnItemLongClickListener {

    private lateinit var mActionModeHelper: ActionModeHelper
    private lateinit var mRecyclerView: RecyclerView
    private lateinit var mAdapter: FlexibleAdapter<CollectionViewItem>

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
        Painter.paintDrawable(menu.findItem(R.id.empty).icon)
    }

    private fun fetchContents() {
        mContents.clear()
        mContentItems.clear()
        mContents.addAll(CollectionManager.recycleBin)
        mContentItems.addAll(mContents.map {content -> CollectionViewItem(content)})
    }

    private fun buildActionBar() {
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    private fun buildRecyclerView(){
        if (mContents.isEmpty()) return

        val orientation = resources.configuration.orientation
        val numColumns = if (orientation == Configuration.ORIENTATION_LANDSCAPE) 5 else 3
        val manager = GridLayoutManager(this, numColumns)

        mAdapter = FlexibleAdapter(mContentItems, this, true)
        initializeActionModeHelper(SelectableAdapter.Mode.IDLE)
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
        mActionModeHelper = object : ActionModeHelper(mAdapter, R.menu.menu_recycle_bin_edit, this as ActionMode.Callback) {
            override fun updateContextTitle(count: Int) {
                mActionMode?.title = if (count == 1) getString(R.string.action_selected_one, count)
                else getString(R.string.action_selected_many, count)
            }
        }.withDefaultMode(mode)
    }

    // Return true to continue with Action Mode
    override fun onActionItemClicked(p0: ActionMode?, p1: MenuItem?): Boolean {
        return true
    }
    override fun onCreateActionMode(p0: ActionMode?, p1: Menu?): Boolean {
        return true
    }
    override fun onPrepareActionMode(p0: ActionMode?, p1: Menu?): Boolean {
        return true
    }

    override fun onDestroyActionMode(p0: ActionMode?) {}

    /**
     * @return True if click should be handled by selection mode, false otherwise.
     */
    override fun onItemClick(view: View?, position: Int): Boolean {
        return mActionModeHelper.onClick(position)
    }

    override fun onItemLongClick(position: Int) {
        mActionModeHelper.onLongClick(this, position)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        mAdapter.onSaveInstanceState(outState)
        super.onSaveInstanceState(outState)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle?) {
        super.onRestoreInstanceState(savedInstanceState)
        mAdapter.onRestoreInstanceState(savedInstanceState)
    }

}
