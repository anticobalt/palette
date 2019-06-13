package iced.egret.palette.activity

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.FragmentActivity
import iced.egret.palette.R
import iced.egret.palette.adapter.MainFragmentPagerAdapter
import iced.egret.palette.util.MainFragmentManager
import iced.egret.palette.util.Permission
import kotlinx.android.synthetic.main.activity_main.*

const val READ_EXTERNAL_CODE = 100
const val WRITE_EXTERNAL_CODE = 101
const val TAG = "VIEW"

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (!Permission.isAccepted(this, Manifest.permission.READ_EXTERNAL_STORAGE)) {
            Permission.request(this, Manifest.permission.READ_EXTERNAL_STORAGE, READ_EXTERNAL_CODE)
        }

        MainFragmentManager.setup(supportFragmentManager)
        MainFragmentManager.createFragments()
        val fragments = MainFragmentManager.fragments.toMutableList()
        viewpagerMainFragments.adapter = MainFragmentPagerAdapter(supportFragmentManager, fragments)
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
        val currentFragmentIndex = viewpagerMainFragments.currentItem
        val currentFragment = MainFragmentManager.getFragmentByIndex(currentFragmentIndex)
        val success = (currentFragment).onBackPressed()
        if (!success) {
            super.onBackPressed()
        }
    }

}
