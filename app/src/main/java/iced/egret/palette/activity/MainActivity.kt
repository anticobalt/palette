package iced.egret.palette.activity

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.afollestad.materialdialogs.MaterialDialog
import iced.egret.palette.R
import iced.egret.palette.util.*
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
        else {
            Storage.setup(this)
        }

        buildApp()

    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            READ_EXTERNAL_CODE -> {
                if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                    val dialog = MaterialDialog(this)
                    dialog.cancelable(false)
                    dialog.show {
                        title(R.string.title_permission_error)
                        message(R.string.message_permission_error)
                        positiveButton(R.string.confirm) {
                            finish()
                        }
                    }
                } else {
                    Storage.setup(this)
                    buildApp()
                }
            }
        }
    }

    /*
    override fun onBackPressed() {
        val currentFragmentIndex = viewpagerMainFragments.currentItem
        val currentFragment = MainFragmentManager.getFragmentByIndex(currentFragmentIndex)
        val success = (currentFragment).onBackPressed()
        if (!success) {
            moveTaskToBack(true)  // don't destroy
        }
    }*/

    /**
     * Setup up models, visuals, and fragments.
     * If Storage is not set up before this, the app won't immediately crash, but it will
     * after some user input.
     */
    private fun buildApp() {
        CollectionManager.setup(this)
        Painter.color = ContextCompat.getColor(this, Painter.colorResource)
        buildFragments()
    }

    private fun buildFragments() {
        MainFragmentManager.setup(supportFragmentManager)
        MainFragmentManager.createFragments()
        val fragments = MainFragmentManager.fragments.toMutableList()

        // bind fragments
        supportFragmentManager.beginTransaction().replace(R.id.frag1, fragments[0]).commit()
        supportFragmentManager.beginTransaction().replace(R.id.frag2, fragments[1]).commit()

        // convert dp to px: https://stackoverflow.com/a/4275969
        val dpParallax = 60
        val scale = resources.displayMetrics.density
        val pxParallax = (dpParallax * scale + 0.5f).toInt()

        slidingPaneMain.parallaxDistance = pxParallax  // make left move when scrolling right
        slidingPaneMain.sliderFadeColor = Color.TRANSPARENT  // make right not greyed out
        slidingPaneMain.setShadowResourceLeft(R.drawable.shadow)

    }

}
