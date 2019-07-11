package iced.egret.palette.activity

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.viewpager.widget.ViewPager
import com.github.piasy.biv.BigImageViewer
import com.github.piasy.biv.loader.glide.GlideImageLoader
import iced.egret.palette.R
import iced.egret.palette.adapter.PicturePagerAdapter
import iced.egret.palette.util.CollectionManager
import kotlinx.android.synthetic.main.activity_view_picture.*

class PictureViewActivity : AppCompatActivity() {

    private var uiHidden = false
    private var position = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        BigImageViewer.initialize(GlideImageLoader.with(applicationContext))
        setContentView(R.layout.activity_view_picture)

        if (!getStartPosition()) return

        buildToolbar()
        buildViewPager()
        buildBottomActions()
    }

    /**
     * Get the starting position of the ViewPager i.e. index of picture
     * @return Success (true) or failure (false)
     */
    private fun getStartPosition() : Boolean {
        position = intent.getIntExtra(getString(R.string.intent_item_key), -1)
        return if (position == -1) {
            Toast.makeText(this, R.string.error_intent_extra, Toast.LENGTH_SHORT).show()
            false
        }
        else {
            true
        }
    }

    private fun buildToolbar() {
        val backgroundColor = ContextCompat.getColor(this, R.color.translucentBlack)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setBackgroundDrawable(ColorDrawable(backgroundColor))
        setToolbarTitle()

        /*
        setToolbarTitle()
        val back = this.getDrawable(R.drawable.ic_arrow_back_black_24dp) ?: return
        Painter.paintDrawable(back)
        toolbar.navigationIcon = back
        toolbar.setNavigationOnClickListener {
            finish()
        }*/
    }

    private fun setToolbarTitle() {
        supportActionBar?.title = CollectionManager.getCurrentCollectionPictures()[position].name
        //toolbar.toolbarTitle.text = CollectionManager.getCurrentCollectionPictures()[position].name
    }

    private fun buildViewPager() {
        val pictures = CollectionManager.getCurrentCollectionPictures()
        val adapter = PicturePagerAdapter(pictures, this)
        viewpager.adapter = adapter
        viewpager.currentItem = position
        viewpager.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            override fun onPageScrollStateChanged(state: Int) {}
            override fun onPageSelected(position: Int) {}
            override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int) {
                this@PictureViewActivity.position = position
                setToolbarTitle()
            }
        })
    }

    private fun buildBottomActions() {
        (bottomActions.layoutParams as ViewGroup.MarginLayoutParams).bottomMargin = getNavigationBarHeight()

        // prevent propagation of touch events on bottom action bar
        bottomActions.setOnTouchListener { _, _ -> true }
    }

    private fun hideSystemUI() {
        // For regular immersive mode, add SYSTEM_UI_FLAG_IMMERSIVE.
        // For "sticky immersive," add SYSTEM_UI_FLAG_IMMERSIVE_STICKY instead.
        // For "lean back" mode, have neither.
        uiHidden = true
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
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
            //toolbar.visibility = View.VISIBLE
            supportActionBar?.show()
        }
        else {
            hideSystemUI()
            bottomActions.visibility = View.GONE
            //toolbar.visibility = View.GONE
            supportActionBar?.hide()
        }
    }

    /**
     * Get height in pixels of bottom navigation bar (present in devices without physical buttons).
     * https://stackoverflow.com/a/20264361
     */
    private fun getNavigationBarHeight() : Int {
        if (!hasNavigationBar()) return 0

        val id = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        return if (id > 0) {
            resources.getDimensionPixelSize(id)
        }
        else 0
    }

    /**
     * Check if navigation bar actually exists.
     * https://stackoverflow.com/a/32698387
     */
    private fun hasNavigationBar() : Boolean {
        val id = resources.getIdentifier("config_showNavigationBar", "bool", "android")
        return id > 0 && resources.getBoolean(id)
    }

}