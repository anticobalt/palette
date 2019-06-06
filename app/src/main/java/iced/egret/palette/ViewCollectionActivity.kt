package iced.egret.palette

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.GridLayoutManager
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast

import kotlinx.android.synthetic.main.activity_view_collection.*
import kotlinx.android.synthetic.main.content_view_collection.*

const val READ_EXTERNAL_CODE = 100
const val WRITE_EXTERNAL_CODE = 101
const val TAG = "VIEW"

class ViewCollectionActivity : AppCompatActivity() {

    private lateinit var mContents : MutableList<Coverable>

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_collection)
        setSupportActionBar(toolbar)

        fab.setOnClickListener { view ->
            Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show()
        }

        if (!Permission.isAccepted(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            Permission.request(this, Manifest.permission.READ_EXTERNAL_STORAGE, READ_EXTERNAL_CODE)
        }

        CollectionManager.initRootFolder(this)
        mContents = CollectionManager.getContents()
        buildRecyclerView()

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_view_collection, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        return when (item.itemId) {
            R.id.action_settings -> true
            else -> super.onOptionsItemSelected(item)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            READ_EXTERNAL_CODE -> {
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    Log.i("External permission", "failed")
                } else {
                    Log.i("External permission","succeeded")
                }
            }
        }
    }

    override fun onBackPressed() {
        val newContents = CollectionManager.getParentCollectionContents()
        if (newContents != null) {
            mContents.clear()
            mContents.addAll(newContents)
            collectionRecyclerView.adapter.notifyDataSetChanged()
        }
        else {
            super.onBackPressed()
        }
    }

    private fun buildRecyclerView() {
        if (mContents.isNotEmpty()) {
            collectionRecyclerView.layoutManager = GridLayoutManager(this, 3)
            collectionRecyclerView.adapter = CollectionRecyclerViewAdapter(mContents)
        }
        else {
            val toast = Toast.makeText(this, getString(R.string.alert_no_folders), Toast.LENGTH_LONG)
            toast.show()
        }
    }

}
