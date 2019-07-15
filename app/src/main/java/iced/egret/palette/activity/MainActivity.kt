package iced.egret.palette.activity

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import com.afollestad.materialdialogs.MaterialDialog
import iced.egret.palette.R
import iced.egret.palette.fragment.MainFragment
import iced.egret.palette.util.*
import kotlinx.android.synthetic.main.activity_main.*

const val EXTERNAL_CODE = 100
const val PICTURE_ACTIVITY_REQUEST = 1
const val SD_CARD_WRITE_REQUEST = 2

class MainActivity : AppCompatActivity() {

    private var hasPermission = false
    private val permissions = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
            )

    private val finishedFragments = mutableListOf<MainFragment>()
    private lateinit var sharedPrefs : SharedPreferences

    companion object SaveDataKeys {
        const val onScreenCollection = "on-screen-collection"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        hasPermission = permissions
                .map {permission -> Permission.isAccepted(this, permission)}
                .all { accepted -> accepted }

        if (!hasPermission) {
            Permission.request(this, permissions, EXTERNAL_CODE)
        }
        else {
            buildApp(savedInstanceState)
        }

        checkSdWriteAccess()

    }

    override fun onSaveInstanceState(outState: Bundle?) {
        outState?.putString(onScreenCollection, CollectionManager.currentCollection?.path)
        super.onSaveInstanceState(outState)
    }

    override fun onStop() {
        // Restore fragment views so fragments can be rebuilt properly if activity is recreated.
        // If only restarting and NOT recreating activity, fragments won't be re-isolated
        // (which is expected behaviour, as any previously active ActionMode won't be reactivated
        // either).
        if (hasPermission) restoreAllFragments()
        super.onStop()
    }

    /**
     * Note that argument fragment has finished onCreateView(),
     * and thus its views can be indirectly manipulated e.g. via isolateFragment().
     */
    fun notifyFragmentCreationFinished(finishedFragment: MainFragment) {
        finishedFragments.add(finishedFragment)
        if (finishedFragments.toSet() == MainFragmentManager.fragments.toSet()) {
            for (fragment in finishedFragments) {
                fragment.onAllFragmentsCreated()
            }
        }
    }

    private fun buildApp(savedInstanceState: Bundle?) {
        Storage.setup(this)
        CollectionManager.setup()
        Painter.color = ContextCompat.getColor(this, Painter.colorResource)
        sharedPrefs = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE)

        if (savedInstanceState == null) {
            // Don't make fragments again if rotating device,
            // because they're automatically remade
            makeFragments()
        } else {
            // Save the remade fragments (which are technically different)
            MainFragmentManager.updateFragments(supportFragmentManager.fragments)
            // Try to restore Collection being viewed
            val navigateToPath = savedInstanceState.getString(onScreenCollection) ?: return
            CollectionManager.unwindStack(navigateToPath)
        }
        styleSlidingPane()
    }

    private fun makeFragments() {
        MainFragmentManager.setup(supportFragmentManager)
        MainFragmentManager.createFragments()
        val fragments = MainFragmentManager.fragments.toMutableList()

        // bind fragments
        supportFragmentManager
                .beginTransaction()
                .replace(R.id.linksFragment, fragments[MainFragmentManager.PINNED_COLLECTIONS])
                .commit()
        supportFragmentManager
                .beginTransaction()
                .replace(R.id.contentsFragment, fragments[MainFragmentManager.COLLECTION_CONTENTS])
                .commit()
    }

    private fun styleSlidingPane() {
        // convert dp to px: https://stackoverflow.com/a/4275969
        val dpParallax = 60
        val scale = resources.displayMetrics.density
        val pxParallax = (dpParallax * scale + 0.5f).toInt()

        slidingPaneLayout.parallaxDistance = pxParallax  // make left move when scrolling right
        slidingPaneLayout.sliderFadeColor = Color.TRANSPARENT  // make right not greyed out
        slidingPaneLayout.setShadowResourceLeft(R.drawable.shadow)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        when (requestCode) {
            EXTERNAL_CODE -> {
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
                    buildApp(null)
                }
            }
        }
    }

    private fun checkSdWriteAccess() {
        val sdTreeUri = sharedPrefs.getString(getString(R.string.sd_card_uri_key), null)
        if (sdTreeUri == null) {
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT_TREE), SD_CARD_WRITE_REQUEST)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)

        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {

                // Get write access to SD card, and save SD card URI to preferences.
                // https://stackoverflow.com/a/43317703
                SD_CARD_WRITE_REQUEST -> {
                    val sdTreeUri = intent?.data
                    if (sdTreeUri == null) {
                        toast("Failed to gain SD card access!")
                        return
                    }

                    // Try to get SD card
                    val directory = DocumentFile.fromTreeUri(this, sdTreeUri)
                    if (directory?.name == null) {
                        toast("Failed to connect with SD card!")
                        return
                    }

                    val modeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                            Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    grantUriPermission(packageName, sdTreeUri, modeFlags)
                    contentResolver.takePersistableUriPermission(sdTreeUri, modeFlags)

                    with (sharedPrefs.edit()) {
                        putString(getString(R.string.sd_card_uri_key), sdTreeUri.toString())
                        apply()
                    }

                }
            }
        }
    }

    override fun onBackPressed() {
        if (hasPermission) {
            var index = MainFragmentManager.COLLECTION_CONTENTS
            if (slidingPaneLayout.isOpen) {
                index = MainFragmentManager.PINNED_COLLECTIONS
            }

            val currentFragment = MainFragmentManager.fragments[index] as MainFragment
            val success = (currentFragment).onBackPressed()
            if (!success) {
                moveTaskToBack(true)  // don't destroy
            }
        }
    }

    /**
     * Block click actions for all manipulable fragments except the given one.
     */
    fun isolateFragment(toIsolateFragment: MainFragment) {
        for (fragment in finishedFragments) {
            if (fragment != toIsolateFragment) {
                fragment.setClicksBlocked(true)
            }
        }
    }

    /**
     * Undo isolateFragment()
     */
    fun restoreAllFragments() {
        for (fragment in finishedFragments) {
            fragment.setClicksBlocked(false)
        }
    }

    private fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

}
