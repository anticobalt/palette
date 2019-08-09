package iced.egret.palette.activity

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.helpers.EmptyViewHelper
import iced.egret.palette.R
import iced.egret.palette.activity.inherited.RecyclerViewActivity
import iced.egret.palette.flexible.item.FileItem
import iced.egret.palette.model.Folder
import iced.egret.palette.util.CollectionManager
import iced.egret.palette.util.StateBuilder
import kotlinx.android.synthetic.main.view_empty.*
import java.io.File

class SyncedFoldersActivity : RecyclerViewActivity() {

    private val mFiles = mutableListOf<File>()
    private val mFileItems = mutableListOf<FileItem>()
    private lateinit var mAdapter: FlexibleAdapter<FileItem>
    private lateinit var albumPath : String

    override fun onCreate(savedInstanceState: Bundle?) {
        // Get path before fetching contents (done by superclass)
        albumPath = intent.getStringExtra(getString(R.string.intent_album_path_key))
        super.onCreate(savedInstanceState)
    }

    override fun fetchContents() {
        StateBuilder.build(this, null)  // ensure Collections set up
        mFiles.clear()
        mFileItems.clear()

        // Add synced folders
        val album = CollectionManager.getNestedAlbums().find { album -> album.path == albumPath }
                ?: return
        mFiles.addAll(album.syncedFolderFiles)
        mFileItems.addAll(mFiles.map { file -> FileItem(file, true) })

        // Add some other folders that user may want to sync
        val candidates = getPotentialFolderFiles(CollectionManager.folders)
                .filter { candidate -> candidate !in mFiles }
        mFiles.addAll(candidates)
        mFileItems.addAll(candidates.map { file -> FileItem(file, false) })
    }

    private fun getPotentialFolderFiles(roots: List<Folder>): List<File> {
        val runningList = mutableListOf<File>()
        for (root in roots) {
            runningList.addAll(getPotentialFolderFiles(root))
        }
        return runningList
    }

    private fun getPotentialFolderFiles(root: Folder): List<File> {
        val runningList = mutableListOf<File>()
        if (root.size > 0) {
            runningList.add(File(root.filePath))
        }
        for (child in root.folders) {
            runningList.addAll(getPotentialFolderFiles(child))
        }
        return runningList
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
        mAdapter = FlexibleAdapter(mFileItems, this, true)
        val manager = LinearLayoutManager(this)

        EmptyViewHelper.create(mAdapter, empty_view)
        mRecyclerView = findViewById(R.id.recyclerView)
        mRecyclerView.layoutManager = manager
        mRecyclerView.adapter = mAdapter

        mRecyclerView.addItemDecoration(DividerItemDecoration(this, manager.orientation))
    }

    override fun onItemClick(view: View?, position: Int): Boolean {
        mFileItems[position].toggleChecked()
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