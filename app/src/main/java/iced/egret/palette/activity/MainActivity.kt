package iced.egret.palette.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.widget.ActionBarContextView
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import androidx.fragment.app.Fragment
import com.afollestad.materialdialogs.MaterialDialog
import iced.egret.palette.R
import iced.egret.palette.activity.inherited.BaseActivity
import iced.egret.palette.fragment.CollectionViewFragment
import iced.egret.palette.fragment.LinksFragment
import iced.egret.palette.fragment.inherited.MainFragment
import iced.egret.palette.layout.HackySlidingPaneLayout
import iced.egret.palette.model.Folder
import iced.egret.palette.model.inherited.Collection
import iced.egret.palette.model.inherited.FileObject
import iced.egret.palette.util.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_links.*

/**
 * Holds a SlidingPaneLayout with two fragments inside each panel, and listens to it.
 * Also handles permissions and starts up the app's shared state.
 *
 * General order of functions:
 * - Lifecycle
 * - Permissions
 * - UI builders
 * - Panel listening
 * - Fragment management
 */
class MainActivity : BaseActivity(), HackySlidingPaneLayout.HackyPanelSlideListener {

    private var hasPermission = false
    private val permissions = arrayOf(
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    private val fragments = Array<MainFragment>(2) { DummyFragment() }
    private val finishedFragments = mutableListOf<MainFragment>()
    private var afterPanelClosed: () -> Unit = {}
    private val leftIndex = 0
    private val rightIndex = 1

    private lateinit var sharedPrefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setAccentThemeFromSettings()
        setContentView(R.layout.activity_main)
        sharedPrefs = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE)

        hasPermission = permissions
                .map { permission -> Permission.isAccepted(this, permission) }
                .all { accepted -> accepted }

        if (!hasPermission) {
            Permission.request(this, permissions, EXTERNAL_CODE)
        } else {
            buildApp(savedInstanceState)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString(ON_SCREEN_COLLECTION_KEY, CollectionManager.currentCollection?.path)
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

    override fun onActivityResult(requestCode: Int, resultCode: Int, intent: Intent?) {
        super.onActivityResult(requestCode, resultCode, intent)

        if (resultCode == RESULT_OK) {
            when (requestCode) {
                SD_CARD_WRITE_REQUEST -> tryAccessSdCard()
                GO_HOME_REQUEST -> {
                    (fragments[rightIndex] as CollectionViewFragment).onNavigation()
                    slidingPaneLayout.closePane()
                }
            }
        }
    }

    /**
     * Get write access to SD card, and save SD card URI to preferences.
     * https://stackoverflow.com/a/43317703
     */
    private fun tryAccessSdCard() {
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

        with(sharedPrefs.edit()) {
            putString(getString(R.string.sd_card_uri_key), sdTreeUri.toString())
            apply()
        }
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
                        positiveButton {
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

    private fun buildApp(savedInstanceState: Bundle?) {

        if (savedInstanceState == null) {
            // First start of activity
            StateBuilder.build(this, null) {
                makeFragments()
                handleIntentViewRequest()
                buildSlidingPane()
                if (isFirstRun()) applyDefaultSettings()  // may recreate, so must be after UI building
                checkSdWriteAccess()
                mainLayout.visibility = View.VISIBLE
            }
        } else {
            val navigateToPath = savedInstanceState.getString(ON_SCREEN_COLLECTION_KEY)
            StateBuilder.build(this, navigateToPath)
            // Save the remade fragments (which are technically different).
            updateFragments(supportFragmentManager.fragments)
            handleIntentViewRequest()
            buildSlidingPane()
            mainLayout.visibility = View.VISIBLE
        }
    }

    private fun isFirstRun(): Boolean {
        val key = "isFirstRun"
        val firstRun = sharedPrefs.getBoolean(key, true)
        sharedPrefs.edit().putBoolean(key, false).apply()
        return firstRun
    }

    @SuppressLint("ApplySharedPref")
    private fun applyDefaultSettings() {
        defSharedPreferences
                .edit()
                .putInt(getString(R.string.toolbar_item_color_key), Color.WHITE)
                .putInt(getString(R.string.primary_color_key), idToColor(R.color.colorPrimary))
                .putInt(getString(R.string.accent_color_key), idToColor(R.color.colorAccent))
                .commit()
        recreate()
    }

    private fun setAccentThemeFromSettings() {
        val resId = when (getColorInt(ColorType.ACCENT)) {
            idToColor(R.color.dodger_blue) -> R.style.AppTheme_StartActivity_DodgerBlue
            idToColor(R.color.sea_green) -> R.style.AppTheme_StartActivity_SeaGreen
            idToColor(R.color.fruit_salad) -> R.style.AppTheme_StartActivity_FruitSalad
            idToColor(R.color.tangerine) -> R.style.AppTheme_StartActivity_Tangerine
            idToColor(R.color.lynch) -> R.style.AppTheme_StartActivity_Lynch
            idToColor(R.color.coffee) -> R.style.AppTheme_StartActivity_Coffee
            idToColor(R.color.idle_pink) -> R.style.AppTheme_StartActivity_IdlePink
            idToColor(R.color.boppin_blue) -> R.style.AppTheme_StartActivity_BoppinBlue
            idToColor(R.color.astral_yellow) -> R.style.AppTheme_StartActivity_AstralYellow
            idToColor(R.color.myriad_magenta) -> R.style.AppTheme_StartActivity_MyriadMagenta
            idToColor(R.color.resolute_cyan) -> R.style.AppTheme_StartActivity_ResoluteCyan
            idToColor(R.color.fashion_red) -> R.style.AppTheme_StartActivity_FashionRed
            else -> R.style.AppTheme_StartActivity  // fallback, default accent colors
        }
        setTheme(resId)
    }

    private fun makeFragments() {

        fragments[leftIndex] = LinksFragment()
        fragments[rightIndex] = CollectionViewFragment()

        val frames = listOf(R.id.leftFragmentContainer, R.id.rightFragmentContainer)
        assert(frames.size == fragments.size)

        // bind fragments
        for (i in 0 until fragments.size) {
            supportFragmentManager
                    .beginTransaction()
                    .replace(frames[i], fragments[i])
                    .commitAllowingStateLoss()
        }

    }

    private fun updateFragments(fragments: List<Fragment>) {
        loop@ for (fragment in fragments) {
            val index: Int = when (fragment) {
                is LinksFragment -> leftIndex
                is CollectionViewFragment -> rightIndex
                else -> continue@loop
            }
            this.fragments[index] = fragment as MainFragment
        }
    }

    private fun buildSlidingPane() {
        // convert dp to px: https://stackoverflow.com/a/4275969
        val dpParallax = 60
        val scale = resources.displayMetrics.density
        val pxParallax = (dpParallax * scale + 0.5f).toInt()

        slidingPaneLayout.parallaxDistance = pxParallax  // make left move when scrolling right
        slidingPaneLayout.sliderFadeColor = Color.TRANSPARENT  // make right not greyed out
        slidingPaneLayout.setShadowResourceLeft(R.drawable.shadow_left_fixed)

        slidingPaneLayout.setPanelSlideListener(this)
    }

    override fun onSlidingPanelReady() {
        if (slidingPaneLayout.isOpen) {
            fragments[rightIndex].navigationDrawable.progress = 1f
        }
    }

    fun togglePanel() {
        if (slidingPaneLayout.isOpen) slidingPaneLayout.closePane()
        else slidingPaneLayout.openPane()
    }

    /**
     * [afterPanelClosed] must be explicitly reset, to avoid doing it every time the panel closes.
     */
    override fun onPanelClosed(panel: View) {
        fragments[leftIndex].slider.closePane()
        afterPanelClosed()
        afterPanelClosed = {}
    }

    override fun onPanelSlide(panel: View, slideOffset: Float) {
        fragments[rightIndex].navigationDrawable.progress = slideOffset
    }

    override fun onPanelOpened(panel: View) {}


    override fun onBackPressed() {
        if (hasPermission) {
            val index = if (slidingPaneLayout.isOpen) leftIndex else rightIndex
            val currentFragment = fragments[index]
            val success = (currentFragment).onBackPressed()
            if (!success) {
                if (index == leftIndex) moveTaskToBack(true)  // don't destroy
                else if (index == rightIndex) slidingPaneLayout.openPane()
            }
        }
    }

    /**
     * Note that argument fragment has finished onCreateView(),
     * and thus its views can be indirectly manipulated e.g. via isolateFragment().
     */
    fun notifyFragmentCreationFinished(finishedFragment: MainFragment) {
        finishedFragments.add(finishedFragment)
        if (finishedFragments.toSet() == fragments.toSet()) {
            for (fragment in finishedFragments) {
                fragment.onAllFragmentsCreated()
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

    /**
     * One ActionMode per Activity, so fragments will ask activity to style it.
     * From https://stackoverflow.com/a/45955606
     */
    fun colorActionMode() {
        val color = ContextCompat.getColor(this, R.color.colorActionMode)
        window.decorView.findViewById<ActionBarContextView?>(R.id.action_mode_bar)
                ?.setBackgroundColor(color)
        window.statusBarColor = color
        window.navigationBarColor = color
    }

    fun undoColorActionMode() {
        val primaryColor = getColorInt(ColorType.PRIMARY)
        window.statusBarColor = Painter.getMaterialDark(primaryColor)
        window.navigationBarColor = Painter.getMaterialDark(primaryColor)
    }

    fun notifyCollectionsChanged() {
        (fragments[leftIndex] as LinksFragment).onCollectionsUpdated()
    }

    fun notifyPinnedAlbumDeleted() {
        val current = CollectionManager.currentCollection
        val allCollections = CollectionManager.getCollections()
        if (current != null && !allCollections.contains(current)) {
            CollectionManager.resetStack()
            // Top collection might have been the one that was deleted
            (fragments[rightIndex] as CollectionViewFragment).onTopCollectionChanged()
        }
    }

    /**
     * Close the main panel, then launch the collection.
     * Launching collection is UI intensive, so trying to do it at the same time
     * as animating the panel closing causes stuttering.
     *
     * See [onPanelClosed] as well.
     */
    fun buildCollectionView(collection: Collection) {
        val cvFragment = fragments[rightIndex] as CollectionViewFragment

        afterPanelClosed = {
            when (collection) {
                is Folder -> CollectionManager.launchAsShortcut(collection)
                else -> {
                    CollectionManager.clearStack()
                    CollectionManager.launch(collection)
                }
            }
            cvFragment.onTopCollectionChanged()
        }

        findViewById<HackySlidingPaneLayout>(R.id.slidingPaneLayout)?.closePane()
    }

    private fun handleIntentViewRequest() {
        val requestFileObject = ThirdPartyIntentHandler.getViewRequest(intent, contentResolver) {
            toast(R.string.generic_error)
            null
        }
        val pathToFulfillRequest = requestFileObject?.parent?.filePath

        when {
            pathToFulfillRequest == null && requestFileObject != null -> {
                // No path, can only preview
                startActivity(Intent(this, PreviewPagerActivity::class.java))
            }
            pathToFulfillRequest == null -> {
                // Can't preview
                return
            }
            else -> {
                // Is file, can show in entirety
                StateBuilder.build(this, pathToFulfillRequest)  // unwinds stack with path
                notifyFragmentsOfIntentRequest(requestFileObject)
            }
        }
    }

    private fun notifyFragmentsOfIntentRequest(fileObject: FileObject?) {
        (fragments[rightIndex] as CollectionViewFragment).toLaunchFromIntent = fileObject
    }

    companion object Constants {
        const val ON_SCREEN_COLLECTION_KEY = "on-screen-collection"
        const val EXTERNAL_CODE = 100
        const val SD_CARD_WRITE_REQUEST = 1
        const val GO_HOME_REQUEST = 2
    }

    class DummyFragment : MainFragment() {
        override fun onActionItemClicked(p0: ActionMode?, p1: MenuItem?): Boolean {return false}
        override fun onCreateActionMode(p0: ActionMode?, p1: Menu?): Boolean {return false}
        override fun onPrepareActionMode(p0: ActionMode?, p1: Menu?): Boolean {return false}
        override fun onDestroyActionMode(p0: ActionMode?) {}
        override fun onItemClick(view: View?, position: Int): Boolean {return false}
        override fun onItemLongClick(position: Int) {}
        override fun setClicksBlocked(doBlock: Boolean) {}
        override fun onAllFragmentsCreated() {}
        override fun onBackPressed(): Boolean {
            return false
        }
    }

}
