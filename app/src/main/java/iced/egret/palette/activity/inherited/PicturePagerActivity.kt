package iced.egret.palette.activity.inherited

import android.content.Intent
import android.content.SharedPreferences
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageButton
import androidx.core.content.FileProvider
import androidx.preference.PreferenceManager
import androidx.viewpager.widget.ViewPager
import iced.egret.palette.BuildConfig
import iced.egret.palette.R
import iced.egret.palette.adapter.PicturePagerAdapter
import iced.egret.palette.model.Picture
import iced.egret.palette.util.Device
import kotlinx.android.synthetic.main.activity_picture_pager.*
import kotlinx.android.synthetic.main.appbar_picture_pager.*

/**
 * Allows image flipping via ViewPager. Translucent top and bottom bars with seamless transition
 * between system's and app's bars; all toolbar menu actions in overflow.
 * Supports true-zoom toggling and sharing/sending image. Hides/shows UI on click.
 */
abstract class PicturePagerActivity : SlideActivity() {

    abstract val bottomBarRes: Int?
    abstract val menuRes: Int?

    protected lateinit var mSharedPrefs: SharedPreferences
    private var mBarBackgroundColor: Int = Color.BLACK
    private var mBarIconColor: Int = Color.WHITE
    protected lateinit var mBottomBar: View

    private var mUiHidden = false
    protected var mActivePage = -1

    private var mViewPagerPosition = 0
    private var mViewPagerOffsetPixels = 0

    protected val mPictures = mutableListOf<Picture>()
    protected val mCurrentPicture: Picture
        get() = mPictures[mActivePage]

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_picture_pager)
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)

        if (!setStartPosition(savedInstanceState)) finish()

        setColors()
        buildSystemBars()
        buildActionBar()
        buildViewPager()
        if (bottomBarRes != null) buildBottomBar()

    }

    /**
     * Set the starting position of the ViewPager i.e. index of picture.
     * @return Success (true) or failure (false)
     */
    private fun setStartPosition(savedInstanceState: Bundle?): Boolean {
        mActivePage = intent.getIntExtra(getString(R.string.intent_item_key), -1)

        if (savedInstanceState != null) {
            mActivePage = savedInstanceState.getInt(INDEX, -1)
        }

        return if (mActivePage == -1) {
            toastLong(R.string.error_generic)
            false
        } else {
            true
        }
    }

    /**
     * Update from Preferences if possible.
     */
    private fun setColors() {

        val usePrimary = mSharedPrefs.getBoolean(getString(R.string.key_pager_toolbar_color), false)
        if (usePrimary) {
            mBarBackgroundColor = getColorInt(ColorType.PRIMARY)
        }  // else use default

        mBarIconColor = getColorInt(ColorType.ITEM)

        // make translucent
        mBarBackgroundColor = getTranslucentColor(mBarBackgroundColor)
    }

    private fun getTranslucentColor(color: Int): Int {
        val sixDigitHex = String.format("%06X", 0xFFFFFF and color)  // https://stackoverflow.com/a/6540378
        val translucency = "B3"  // 70% opacity
        return Color.parseColor("#$translucency$sixDigitHex")
    }

    private fun buildSystemBars() {
        // Prevent extra space below and above status bar and navigation bar, respectively.
        // https://developer.android.com/reference/kotlin/android/view/Window.html#setstatusbarcolor
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)

        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
    }

    private fun buildActionBar() {
        appbar.setPadding(0, Device.getStatusBarHeight(resources), 0, 0)

        // setting AppBarLayout background instead of toolbar makes entire hide animation show
        appbar.background = getGradientToTransparent(mBarBackgroundColor, GradientDrawable.Orientation.TOP_BOTTOM)

        // Universal fix for appbar being behind ImageView.
        // Setting translationZ=0.1dp doesn't work on some devices (e.g. Nexus5).
        appbar.bringToFront()

        // https://stackoverflow.com/a/33534039
        toolbar.overflowIcon?.setTint(mBarIconColor)
        toolbar.navigationIcon?.setTint(mBarIconColor)
        toolbarTitle.setTextColor(mBarIconColor)

        setSupportActionBar(toolbar)
        supportActionBar?.title = ""  // toolbarTitle is handling title
    }

    /**
     * E.g. orientation of BOTTOM_TOP with color=red has red on bottom.
     * @return a linear gradient that can be directly set as view background
     */
    private fun getGradientToTransparent(color: Int, orientation: GradientDrawable.Orientation): GradientDrawable {
        val colors = intArrayOf(color, Color.TRANSPARENT)
        val gradientDrawable = GradientDrawable(orientation, colors)
        gradientDrawable.gradientType = GradientDrawable.LINEAR_GRADIENT
        return gradientDrawable
    }

    protected fun setToolbarTitle() {
        toolbarTitle.text = mCurrentPicture.name
    }

    private fun buildBottomBar() {
        if (bottomBarRes == null) return

        // Need to inflate into wrapper with layout_gravity=bottom for it show on bottom
        mBottomBar = layoutInflater.inflate(bottomBarRes!!, findViewById(R.id.bottomBarWrapper))

        mBottomBar.setPadding(0, 0, 0, Device.getNavigationBarHeight(resources))

        // color bar and bar actions
        mBottomBar.background = getGradientToTransparent(mBarBackgroundColor, GradientDrawable.Orientation.BOTTOM_TOP)
        for (touchable in mBottomBar.touchables) {
            if (touchable is ImageButton) {
                touchable.imageTintList = ColorStateList.valueOf(mBarIconColor)
            }
        }

        // prevent propagation of touch events on bottom action bar
        mBottomBar.setOnTouchListener { _, _ -> true }

        setBottomBarListeners()

    }

    // Override if have bottom bar, ignore otherwise
    open fun setBottomBarListeners() {}

    /**
     * Higher offscreenPageLimit -> more lag when scrolling if in true zoom mode
     */
    private fun buildViewPager() {
        fetchPictures()
        viewpager.adapter = PicturePagerAdapter(mPictures, this)
        viewpager.currentItem = mActivePage
        viewpager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {}
            /**
             * If this is called without scrolling (e.g. button press),
             * make sure to call setPage().
             */
            override fun onPageSelected(position: Int) {}

            /**
             * Keep title and position if doing less than half-scroll back;
             * change if doing more than half scroll forward.
             * https://stackoverflow.com/a/29095096
             */
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                // If moving right
                if (mActivePage == position) {
                    if (positionOffset > 0.5) setPage(position + 1)
                    else setPage(position)
                }
                // If moving left
                else {
                    if (positionOffset < 0.5) setPage(position)
                    else setPage(position + 1)
                }
                mViewPagerPosition = position
                mViewPagerOffsetPixels = positionOffsetPixels
            }
        })
    }

    protected fun refreshViewPager() {
        val currentPage = viewpager.currentItem
        viewpager.adapter = viewpager.adapter
        viewpager.currentItem = currentPage
    }

    private fun setPage(position: Int) {
        mActivePage = position
        setToolbarTitle()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if (menuRes != null) menuInflater.inflate(menuRes!!, menu)
        return super.onCreateOptionsMenu(menu)
    }

    /**
     * Sets the true zoom checkbox if it exists
     */
    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val trueZoomOn = mSharedPrefs.getBoolean(getString(R.string.key_true_zoom), false)
        val trueZoomItem = menu.findItem(R.id.switchTrueZoom)
        trueZoomItem?.isChecked = trueZoomOn
        return super.onPrepareOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        val retVal = when (item?.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            R.id.switchTrueZoom -> {
                item.isChecked = !item.isChecked
                mSharedPrefs.edit().putBoolean(getString(R.string.key_true_zoom), item.isChecked).apply()
                refreshViewPager()
                true
            }
            else -> false
        }
        return if (retVal) true  // consume action
        else super.onOptionsItemSelected(item)
    }

    abstract fun fetchPictures()

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            toastLong(R.string.file_save_success)
            setResult(RESULT_OK)
            finish()
        }
    }

    protected fun startShareActivity() {
        // Need to create content:// URI to share, instead of natively-used file:// one
        // https://stackoverflow.com/a/38858040
        // https://developer.android.com/training/sharing/send
        val picture = mCurrentPicture
        val imageUri = FileProvider.getUriForFile(
                this,
                BuildConfig.APPLICATION_ID + ".file_provider",
                picture.file
        )
        val intent = Intent().apply {
            action = Intent.ACTION_SEND
            putExtra(Intent.EXTRA_STREAM, imageUri)
            type = picture.mimeType
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(Intent.createChooser(intent, getText(R.string.share_intent_title)))
    }

    private fun hideSystemUI() {
        // For regular immersive mode, add SYSTEM_UI_FLAG_IMMERSIVE.
        // For "sticky immersive," add SYSTEM_UI_FLAG_IMMERSIVE_STICKY instead.
        // For "lean back" mode, have neither.
        mUiHidden = true
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE
                // Set the content to appear under the system bars so that the
                // content doesn't resize when the system bars hide and show.
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                // Hide the nav bar and status bar
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)
    }

    /**
     * Shows the system bars by removing all the flags,
     * except for the ones that make the content appear under the system bars.
     */
    private fun showSystemUI() {
        mUiHidden = false
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
    }

    fun toggleUIs() {
        if (mUiHidden) {
            showSystemUI()
            if (bottomBarRes != null) mBottomBar.animate().translationY(0f)
            appbar.animate().translationY(0f)
        } else {
            hideSystemUI()
            // bottomActions and appbar are padded, so translating by height causes disappearance
            if (bottomBarRes != null) mBottomBar.animate().translationY(mBottomBar.height.toFloat())
            appbar.animate().translationY(-appbar.height.toFloat())
        }
    }

    companion object SaveDataKeys {
        const val COLLECTION = "current-collection"
        const val INDEX = "media-index"
    }

}