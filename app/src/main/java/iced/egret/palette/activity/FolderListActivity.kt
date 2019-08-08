package iced.egret.palette.activity

import android.view.MenuItem
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.helpers.EmptyViewHelper
import iced.egret.palette.R
import iced.egret.palette.activity.inherited.RecyclerViewActivity
import iced.egret.palette.flexible.item.FileItem
import kotlinx.android.synthetic.main.view_empty.*
import java.io.File

class FolderListActivity : RecyclerViewActivity() {

    private val mContents = mutableListOf<File>()
    private val mContentItems = mutableListOf<FileItem>()
    private lateinit var mAdapter : FlexibleAdapter<FileItem>

    override fun fetchContents() {
        mContents.clear()
        mContentItems.clear()
    }

    override fun buildToolbar() {
        mToolbar.inflateMenu(R.menu.menu_folder_list)
        mToolbar.title = getString(R.string.folder_list)

        mToolbar.setOnMenuItemClickListener {
            onOptionsItemSelected(it)
        }
        mToolbar.setNavigationOnClickListener {
            onBackPressed()
        }
    }

    override fun buildRecyclerView() {
        mAdapter = FlexibleAdapter(mContentItems, this, true)
        EmptyViewHelper.create(mAdapter, empty_view)
        mRecyclerView = findViewById(R.id.recyclerView)
        mRecyclerView.layoutManager = LinearLayoutManager(this)
        mRecyclerView.adapter = mAdapter
    }

    override fun onItemClick(view: View?, position: Int): Boolean {
        mContentItems[position].toggleChecked()
        return true  // handled
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.apply -> apply()
            else -> return super.onOptionsItemSelected(item)
        }
        return true
    }

    private fun apply() {

    }

}