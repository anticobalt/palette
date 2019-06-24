package iced.egret.palette.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import iced.egret.palette.R
import iced.egret.palette.adapter.PinnedCollectionsAdapter
import iced.egret.palette.recyclerview_component.LongClickSelector
import iced.egret.palette.section.PinnedCollectionsSection
import iced.egret.palette.util.CollectionManager
import iced.egret.palette.util.DialogGenerator
import iced.egret.palette.util.Painter
import io.github.luizgrp.sectionedrecyclerviewadapter.StatelessSection

class PinnedCollectionsFragment : MainFragment() {

    private var mRootView : View? = null
    private lateinit var mRecyclerView : RecyclerView

    private lateinit var mDefaultToolbar : Toolbar
    private lateinit var mEditToolbar : Toolbar

    private lateinit var mAdapter : PinnedCollectionsAdapter
    private var mSelectors = mutableListOf<LongClickSelector>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        mRootView = inflater.inflate(R.layout.fragment_pinned_collections, container, false)
        mRecyclerView = mRootView!!.findViewById(R.id.rvPinnedCollections)
        buildRecyclerView()
        return mRootView
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        buildToolbar()  // edit menu needs activity
    }

    private fun buildToolbar() {

        mDefaultToolbar = mRootView!!.findViewById(R.id.toolbarPinnedCollections)
        mEditToolbar = mRootView!!.findViewById(R.id.toolbarEditPinnedCollections)

        mDefaultToolbar.setTitle(R.string.app_name)
        mDefaultToolbar.inflateMenu(R.menu.menu_pinned_collections)
        mDefaultToolbar.setOnMenuItemClickListener {
            onOptionsItemSelected(it)
        }

        mEditToolbar.setTitle(R.string.app_name)
        mEditToolbar.inflateMenu(R.menu.menu_pinned_collections_edit)
        mEditToolbar.setOnMenuItemClickListener {
            onOptionsItemSelected(it)
        }
        val back = activity?.getDrawable(R.drawable.ic_chevron_left_black_24dp)
        Painter.paintDrawable(back)
        mEditToolbar.navigationIcon = back
        mEditToolbar.setNavigationOnClickListener {
            onBackPressed()
        }

    }

    private fun buildRecyclerView() {

        mRecyclerView.layoutManager = GridLayoutManager(activity, 1)
        mAdapter = PinnedCollectionsAdapter()

        val folderSection = PinnedCollectionsSection("Folders", CollectionManager.folders, mAdapter, this)
        mAdapter.addSection(folderSection)
        mSelectors.add(folderSection.selector)

        val albumSection = PinnedCollectionsSection("Albums", CollectionManager.albums, mAdapter, this)
        mAdapter.addSection(albumSection)
        mSelectors.add(albumSection.selector)

        mRecyclerView.adapter = mAdapter
    }

    override fun onBackPressed(): Boolean {
        var handledBySelector = false
        /**
         * Try to find one selector to handle
         */
        for (selector in mSelectors) {
            handledBySelector = handledBySelector || selector.onBackPressed()
            if (handledBySelector) return true
        }
        return handledBySelector
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
            R.id.actionPinnedCollectionsDeleteAlbum -> {
                val callback = callback@ {
                    val activeSelector = mSelectors.find { s -> s.active } ?: return@callback
                    CollectionManager.deleteAlbumsByPosition(activeSelector.selectedItemIds)
                    activeSelector.deactivate()
                    mAdapter.updateCollections()
                }
                DialogGenerator.deleteAlbum(context, onConfirm = callback)
            }
            else -> super.onOptionsItemSelected(item)
        }

        return true
    }

    /**
     * Called by LongClickSelector upon its activation
     */
    override fun onAlternateModeActivated(section: StatelessSection) {
        mDefaultToolbar.visibility = Toolbar.GONE
        mEditToolbar.visibility = Toolbar.VISIBLE

        section as PinnedCollectionsSection  // cast
        val editMenu = mEditToolbar.menu
        when (section.title.toLowerCase()) {
            "folders" -> {
                editMenu.findItem(R.id.actionPinnedCollectionsHide).isVisible = true
            }
            "albums" -> {
                editMenu.findItem(R.id.actionPinnedCollectionsDeleteAlbum).isVisible = true
            }
        }
    }

    /**
     * Called by LongClickSelector after it finishes cleaning up
     */
    override fun onAlternateModeDeactivated(section: StatelessSection) {
        mEditToolbar.visibility = Toolbar.GONE
        mDefaultToolbar.visibility = Toolbar.VISIBLE

        val editMenu = mEditToolbar.menu
        editMenu.findItem(R.id.actionPinnedCollectionsHide).isVisible = false
        editMenu.findItem(R.id.actionPinnedCollectionsDeleteAlbum).isVisible = false

        // already does notifyDataSetChanged(), so don't call it again to reset views
        mAdapter.showAllSections()
    }

    private fun onCreateNewAlbum(charSequence: CharSequence) {
        CollectionManager.createNewAlbum(charSequence.toString())
        mAdapter.updateCollections()
    }

    /**
     * Update everything to reflect changes
     */
    fun notifyChanges() {
        mAdapter.updateCollections()
    }

}