package iced.egret.palette.activity

import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.viewpager.widget.ViewPager
import com.github.piasy.biv.BigImageViewer
import com.github.piasy.biv.loader.glide.GlideImageLoader
import com.google.android.material.appbar.AppBarLayout
import com.theartofdev.edmodo.cropper.CropImage
import iced.egret.palette.R
import iced.egret.palette.adapter.PicturePagerAdapter
import iced.egret.palette.model.Picture
import iced.egret.palette.util.CollectionManager
import iced.egret.palette.util.DialogGenerator
import kotlinx.android.synthetic.main.activity_view_picture.*
import kotlinx.android.synthetic.main.bottom_actions_view_picture.view.*

class PictureViewActivity : BottomActionsActivity() {

    private var uiHidden = false
    private var itemPosition = -1

    private val mPictures = mutableListOf<Picture>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BigImageViewer.initialize(GlideImageLoader.with(applicationContext))
        setContentView(R.layout.activity_view_picture)

        if (!getStartPosition()) return

        buildSystemBars()
        buildActionBar()
        buildViewPager()
        buildBottomActions()

    }

    /**
     * Get the starting position of the ViewPager i.e. index of picture
     * @return Success (true) or failure (false)
     */
    private fun getStartPosition(): Boolean {
        itemPosition = intent.getIntExtra(getString(R.string.intent_item_key), -1)
        return if (itemPosition == -1) {
            toast(R.string.intent_extra_error)
            false
        } else {
            true
        }
    }

    private fun buildSystemBars() {
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        window.addFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)

        window.statusBarColor = ContextCompat.getColor(this, R.color.translucentBlack)
        window.navigationBarColor = ContextCompat.getColor(this, R.color.translucentBlack)
    }

    private fun buildActionBar() {
        val backgroundColor = ContextCompat.getColor(this, R.color.translucentBlack)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        val appBarLayout = findViewById<AppBarLayout>(R.id.appbar)

        // setting AppBarLayout background instead of toolbar makes entire hide animation show
        appBarLayout.background = ColorDrawable(backgroundColor)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }

    /**
     * Set ellipse location for overflow text in default action bar.
     * Getting action_bar_title directly or via resources do not work.
     * https://stackoverflow.com/a/34933846
     *
     * Default action bar is used because custom one is buggy when trying to animate it alongside
     * system status bar.
     */
    private fun setActionBarEllipsize(location: TextUtils.TruncateAt) {
        val toolbar = findViewById<Toolbar>(R.id.action_bar)
        for (child in 0 until toolbar.childCount) {
            if (toolbar.getChildAt(child) is TextView) {
                (toolbar.getChildAt(child) as TextView).ellipsize = location
            }
        }
    }

    private fun setToolbarTitle() {
        supportActionBar?.title = CollectionManager.getCurrentCollectionPictures()[itemPosition].name
    }

    private fun buildViewPager() {
        fetchPictures()
        viewpager.adapter = PicturePagerAdapter(mPictures, this)
        viewpager.currentItem = itemPosition
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
                if (itemPosition == position) {
                    if (positionOffset > 0.5) setPage(position + 1)
                    else setPage(position)
                }
                // If moving left
                else {
                    if (positionOffset < 0.5) setPage(position)
                    else setPage(position + 1)
                }
            }
        })
    }

    private fun setPage(position: Int) {
        itemPosition = position
        setToolbarTitle()
    }

    override fun buildBottomActions() {
        super.buildBottomActions()

        // prevent propagation of touch events on bottom action bar
        bottomActions.setOnTouchListener { _, _ -> true }

        bottomActions.crop.setOnClickListener {
            startCropActivity()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_picture, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        val retVal = when (item?.itemId) {
            android.R.id.home -> {
                onBackPressed()
                true
            }
            R.id.actionMove -> {
                initiateMove()
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

    private fun updatePictures() {
        fetchPictures()
        viewpager.adapter?.notifyDataSetChanged()
    }

    private fun startCropActivity() {
        val imageUri = CollectionManager.getCurrentCollectionPictures()[itemPosition].uri
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
        DialogGenerator.moveFile(this) {
            val destination = it
            val oldPicture = CollectionManager.getCurrentCollectionPictures()[itemPosition]

            if (oldPicture.fileLocation == destination.path) {
                toast(R.string.already_exists_error)
                return@moveFile
            }

            val files = CollectionManager.movePicture(itemPosition, destination, getSdCardDocumentFile(), contentResolver)
            if (files != null) {
                broadcastNewMedia(files.first)
                broadcastNewMedia(files.second)
                toast(R.string.file_move_success)
                setResult(RESULT_OK)
                finish()
            } else {
                toast(R.string.move_fail_error)
            }
        }
    }

    private fun hideSystemUI() {
        // For regular immersive mode, add SYSTEM_UI_FLAG_IMMERSIVE.
        // For "sticky immersive," add SYSTEM_UI_FLAG_IMMERSIVE_STICKY instead.
        // For "lean back" mode, have neither.
        uiHidden = true
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
        uiHidden = false
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
    }

    fun toggleUIs() {
        if (uiHidden) {
            showSystemUI()
            bottomActions.visibility = View.VISIBLE
            supportActionBar?.show()
        } else {
            hideSystemUI()
            bottomActions.visibility = View.GONE
            supportActionBar?.hide()
        }
    }

}