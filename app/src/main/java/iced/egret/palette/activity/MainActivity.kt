package iced.egret.palette.activity

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentActivity
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import iced.egret.palette.R
import iced.egret.palette.adapter.MainFragmentPagerAdapter
import iced.egret.palette.fragment.MainFragment
import iced.egret.palette.util.FragmentFactory
import iced.egret.palette.util.Permission
import kotlinx.android.synthetic.main.activity_main.*

const val READ_EXTERNAL_CODE = 100
const val WRITE_EXTERNAL_CODE = 101
const val TAG = "VIEW"

class MainActivity : FragmentActivity() {

    private lateinit var fragments : ArrayList<Fragment>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (!Permission.isAccepted(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            Permission.request(this, Manifest.permission.READ_EXTERNAL_STORAGE, READ_EXTERNAL_CODE)
        }

        fragments = FragmentFactory.create()
        mainFragmentPager.adapter = MainFragmentPagerAdapter(supportFragmentManager, fragments)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
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
        // A hack: https://stackoverflow.com/a/18611036
        // Good since 2013, so good enough for me
        val currentFragment = supportFragmentManager.findFragmentByTag(
                "android:switcher:" + R.id.mainFragmentPager + ":0"
        )
        val success = (currentFragment as MainFragment).onBackPressed()
        if (!success) {
            super.onBackPressed()
        }
    }

}
