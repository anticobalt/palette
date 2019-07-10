package iced.egret.palette.activity

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.GestureDetector
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.github.piasy.biv.BigImageViewer
import com.github.piasy.biv.loader.glide.GlideImageLoader
import iced.egret.palette.R
import iced.egret.palette.adapter.PicturePagerAdapter
import iced.egret.palette.util.CollectionManager
import kotlinx.android.synthetic.main.activity_view_picture.*

class PictureViewActivity : AppCompatActivity() {

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemUI()
    }

    private fun hideSystemUI() {
        // For regular immersive mode, add SYSTEM_UI_FLAG_IMMERSIVE.
        // For "sticky immersive," add SYSTEM_UI_FLAG_IMMERSIVE_STICKY instead.
        // For "lean back" mode, have neither.
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

    // Shows the system bars by removing all the flags
    // except for the ones that make the content appear under the system bars.
    private fun showSystemUI() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        hideSystemUI()
        BigImageViewer.initialize(GlideImageLoader.with(applicationContext))
        setContentView(R.layout.activity_view_picture)

        lateinit var gestureDetector: GestureDetector

        //imageView.setOnTouchListener { _, event ->
        //    gestureDetector.onTouchEvent(event)
        //}

        val position = intent.getIntExtra(getString(R.string.intent_item_key), -1)
        if (position == -1) {
            Toast.makeText(this, R.string.error_intent_extra, Toast.LENGTH_SHORT).show()
            return
        }

        val pictures = CollectionManager.getCurrentCollectionPictures()
        val adapter = PicturePagerAdapter(pictures)
        viewpager.adapter = adapter
        viewpager.currentItem = position

    }

}