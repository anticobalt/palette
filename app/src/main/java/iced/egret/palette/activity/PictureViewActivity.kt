package iced.egret.palette.activity

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
import androidx.core.content.ContextCompat
import androidx.preference.PreferenceManager
import androidx.viewpager.widget.ViewPager
import com.theartofdev.edmodo.cropper.CropImage
import iced.egret.palette.R
import iced.egret.palette.adapter.PicturePagerAdapter
import iced.egret.palette.model.Picture
import iced.egret.palette.util.CollectionManager
import iced.egret.palette.util.DialogGenerator
import iced.egret.palette.util.Storage
import kotlinx.android.synthetic.main.activity_view_picture.*
import kotlinx.android.synthetic.main.appbar_view_picture.*
import kotlinx.android.synthetic.main.bottom_actions_view_picture.view.*


class PictureViewActivity : BottomActionsActivity() {

    private lateinit var mSharedPrefs: SharedPreferences
    private var mBarBackgroundColor: Int = Color.BLACK
    private var mBarIconColor: Int = Color.WHITE

    private var mUiHidden = false
    private var mActivePage = -1

    private var mViewPagerPosition = 0
    private var mViewPagerOffsetPixels = 0

    private val mPictures = mutableListOf<Picture>()
    private val mCurrentPicture: Picture
        get() = mPictures[mActivePage]

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_view_picture)
        mSharedPrefs = PreferenceManager.getDefaultSharedPreferences(this)

        if (!getStartPosition()) return

        setColors()
        buildSystemBars()
        buildActionBar()
        buildViewPager()
        buildBottomActions()

    }

    override fun onResume() {
        super.onResume()
        if (!Storage.fileExists(mCurrentPicture.filePath)) finish()
    }

    /**
     * Get the starting position of the ViewPager i.e. index of picture
     * @return Success (true) or failure (false)
     */
    private fun getStartPosition(): Boolean {
        mActivePage = intent.getIntExtra(getString(R.string.intent_item_key), -1)
        return if (mActivePage == -1) {
            toast(R.string.intent_extra_error)
            false
        } else {
            true
        }
    }

    /**
     * Update from Preferences if possible.
     */
    private fun setColors() {

        val usePrimary = mSharedPrefs.getBoolean(getString(R.string.flipper_toolbar_color_key), false)
        if (usePrimary) {
            mBarBackgroundColor = mSharedPrefs.getInt(getString(R.string.primary_color_key),
                    ContextCompat.getColor(this, R.color.colorPrimary))
        }  // else use default

        mBarIconColor = mSharedPrefs.getInt(getString(R.string.toolbar_item_color_key), mBarIconColor)

        // make translucent
        mBarBackgroundColor = getTranslucentColor(mBarBackgroundColor)
        mBarIconColor = getTranslucentColor(mBarIconColor)
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
        appbar.setPadding(0, getStatusBarHeight(), 0, 0)

        // setting AppBarLayout background instead of toolbar makes entire hide animation show
        appbar.background = getGradientToTransparent(mBarBackgroundColor, GradientDrawable.Orientation.TOP_BOTTOM)

        // Universal fix for appbar being behind ImageView due to elevation=0dp in XML.
        // Setting translationZ=0.1dp doesn't work on some devices (e.g. Nexus5).
        appbar.bringToFront()

        // https://stackoverflow.com/a/33534039
        toolbar.overflowIcon?.setTint(mBarIconColor)
        toolbar.navigationIcon?.setTint(mBarIconColor)
        toolbarTitle.setTextColor(mBarIconColor)

        setSupportActionBar(toolbar)
        supportActionBar?.title = ""  // toolbarTitle is handling title
    }

    private fun getStatusBarHeight(): Int {
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) {
            resources.getDimensionPixelSize(resourceId)
        } else 0
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

    private fun setToolbarTitle() {
        toolbarTitle.text = CollectionManager.getCurrentCollectionPictures()[mActivePage].name
    }

    override fun buildBottomActions() {
        super.buildBottomActions()
        bottomActions.setPadding(0, 0, 0, getNavigationBarHeight())

        // color bar and bar actions
        bottomActions.background = getGradientToTransparent(mBarBackgroundColor, GradientDrawable.Orientation.BOTTOM_TOP)
        for (touchable in bottomActions.touchables) {
            if (touchable is ImageButton) {
                touchable.imageTintList = ColorStateList.valueOf(mBarIconColor)
            }
        }

        // prevent propagation of touch events on bottom action bar
        bottomActions.setOnTouchListener { _, _ -> true }

        bottomActions.details.setOnClickListener {
            DialogGenerator.pictureDetails(this, mPictures[mActivePage])
        }
        bottomActions.home_folder.setOnClickListener {

        }
        bottomActions.share.setOnClickListener {

        }
        bottomActions.crop.setOnClickListener {
            startCropActivity()
        }
        bottomActions.delete.setOnClickListener {
            initiateMoveToRecycleBin()
        }

    }

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

    private fun refreshViewPager() {
        val currentPage = viewpager.currentItem
        viewpager.adapter = viewpager.adapter
        viewpager.currentItem = currentPage
    }

    private fun setPage(position: Int) {
        mActivePage = position
        setToolbarTitle()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_picture, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val trueZoomOn = mSharedPrefs.getBoolean(getString(R.string.true_zoom_key), false)
        val trueZoomItem = menu.findItem(R.id.switchTrueZoom)
        trueZoomItem.isChecked = trueZoomOn
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
                mSharedPrefs.edit().putBoolean(getString(R.string.true_zoom_key), item.isChecked).apply()
                refreshViewPager()
                true
            }
            R.id.actionMove -> {
                initiateMove()
                true
            }
            R.id.actionRename -> {
                initiateRename()
                true
            }
            else -> false
        }
        return if (retVal) true  // consume action
        else super.onOptionsItemSelected(item)
    }

    private fun fetchPictures() {
        mPictures.clear()
        mPictures.addAll(CollectionManager.getCurrentCollectionPictures())
    }

    private fun startCropActivity() {
        val imageUri = CollectionManager.getCurrentCollectionPictures()[mActivePage].uri
        // setting initial crop padding doesn't working in XML for whatever reason
        CropImage.activity(imageUri)
                .setInitialCropWindowPaddingRatio(0f)
                .start(this, CropActivity::class.java)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK) {
            toast(R.string.file_save_success)
            setResult(RESULT_OK)
            finish()
        }
    }

    private fun initiateMove() {
        DialogGenerator.moveTo(this) {
            val destination = it
            val oldPicture = CollectionManager.getCurrentCollectionPictures()[mActivePage]

            if (oldPicture.fileLocation == destination.path) {
                toast(R.string.already_exists_error)
                return@moveTo
            }

            val failCount = CollectionManager.movePictures(listOf(oldPicture), destination,
                    getSdCardDocumentFile(), contentResolver) { sourceFile, movedFile ->
                broadcastMediaChanged(sourceFile)
                broadcastMediaChanged(movedFile)
            }
            if (failCount == 0) {
                toast(R.string.file_move_success)
                setResult(RESULT_OK)
                finish()
            } else {
                toast(R.string.move_fail_error)
            }
        }
    }

    private fun initiateRename() {
        val picture = mPictures[mActivePage]
        val nameList = picture.name.split(".")
        val nameWithoutExtension = nameList.dropLast(1).joinToString(".")
        val extension = nameList.last()

        DialogGenerator.nameFile(this, nameWithoutExtension) { charSequence, dialog ->
            val newName = "$charSequence.$extension"
            if (Storage.fileExists(newName, picture.parentFilePath)) {
                toast(R.string.already_exists_error)
            } else {
                val files = CollectionManager.renamePicture(picture, newName, getSdCardDocumentFile())
                if (files == null) {
                    toast(R.string.edit_fail_error)  // either no SD card access, or OS-level rename error
                } else {
                    // File by old name is technically it's own file
                    broadcastMediaChanged(files.first)
                    broadcastMediaChanged(files.second)
                    toast(R.string.file_save_success)
                    dialog.dismiss()  // nameFile dialog has no auto-dismiss, so do it manually
                    setToolbarTitle()  // to update name
                }
            }

        }
    }

    private fun initiateMoveToRecycleBin() {
        val picture = mPictures[mActivePage]
        DialogGenerator.moveToRecycleBin(this, "picture") {
            // Need sdCardFile to delete from SD card (if required)
            val failCount = CollectionManager.movePicturesToRecycleBin(listOf(picture), getSdCardDocumentFile()) {
                broadcastMediaChanged(it)
            }
            if (failCount == 0) {
                toast("Picture moved to recycle bin")
                setResult(RESULT_OK)
                finish()
            } else toast("Failed to move picture to recycle bin!")
        }
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
            bottomActions.animate().translationY(0f)
            appbar.animate().translationY(0f)
        } else {
            hideSystemUI()
            // bottomActions and appbar are padded, so translating by height causes disappearance
            bottomActions.animate().translationY(bottomActions.height.toFloat())
            appbar.animate().translationY(-appbar.height.toFloat())
        }
    }

}