package iced.egret.palette.activity

import android.app.Activity
import android.content.Intent
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
        mFileItems.clear()

        // Add synced folders
        val album = CollectionManager.getNestedAlbums().find { album -> album.path == albumPath }
                ?: return
        val files = album.syncedFolderFiles
        mFileItems.addAll(files.map { file -> FileItem(file, true) })

        // Add some other folders that user may want to sync
        val candidates = getPotentialFolderFiles(CollectionManager.folders)
                .filter { file -> file !in files }
                .sortedBy { file -> file.path }
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
        val fileNames = mFileItems
                .filter {item -> item.isChecked}
                .map {item -> item.file.path}
                as ArrayList

        val intent = Intent()
        intent.putStringArrayListExtra(getString(R.string.intent_synced_folder_paths), fileNames)
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

}