package iced.egret.palette.adapter

import android.view.*
import android.widget.FrameLayout
import androidx.preference.PreferenceManager
import androidx.viewpager.widget.PagerAdapter
import com.davemorrissey.labs.subscaleview.ImageSource
import com.davemorrissey.labs.subscaleview.SubsamplingScaleImageView
import iced.egret.palette.R
import iced.egret.palette.activity.PicturePagerActivity
import iced.egret.palette.model.Picture
import kotlinx.android.synthetic.main.activity_picture_pager.*
import kotlinx.android.synthetic.main.item_view_picture_gestures.view.*
import kotlinx.android.synthetic.main.item_view_picture_true_zoom.view.*
import java.lang.ref.WeakReference

class PicturePagerAdapter(private val pictures: MutableList<Picture>, activity: PicturePagerActivity) : PagerAdapter() {

    private class DummyTouchListener : View.OnTouchListener {
        override fun onTouch(p0: View?, p1: MotionEvent?): Boolean {
            return false
        }
    }

    private val activityReference = WeakReference(activity)

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view == `object` as FrameLayout
    }

    override fun getCount(): Int {
        return pictures.size
    }

    /**
     * SSIVs are not compatible with GestureViews, so use both GestureImageView and SSIV,
     * depending on whether true zoom is required or not.
     *
     * Click listeners have to be set to individual ImageViews, or they won't be detected.
     */
    override fun instantiateItem(container: ViewGroup, position: Int): Any {

        val picture = pictures[position]
        val trueZoomOn = PreferenceManager
                .getDefaultSharedPreferences(activityReference.get())
                .getBoolean(activityReference.get()?.getString(R.string.true_zoom_key), false)

        // Set layout and build ImageView
        val layoutItem: View
        if (trueZoomOn) {
            layoutItem = LayoutInflater
                    .from(container.context)
                    .inflate(R.layout.item_view_picture_true_zoom, container, false)
            buildTrueZoomImageView(layoutItem, picture)
        } else {
            layoutItem = LayoutInflater
                    .from(container.context)
                    .inflate(R.layout.item_view_picture_gestures, container, false)
            buildGestureImageView(layoutItem, picture)
        }

        container.addView(layoutItem)
        return layoutItem

    }

    /**
     * Show a normal ImageView while the SSIV is loading, then hide it when SSIV done,
     * to reduce perceived loading time.
     * Idea from https://github.com/kollerlukas/Camera-Roll-Android-App.
     */
    private fun buildTrueZoomImageView(layoutItem: View, picture: Picture) {

        val staticImageView = layoutItem.staticImageView
        val ssImageView = layoutItem.ssImageView
        picture.loadInto(staticImageView)

        // Only JPGs/PNGs work with SSIV; just use normal ImageView for other types
        if (picture.isJpgOrPng) {
            ssImageView.orientation = SubsamplingScaleImageView.ORIENTATION_USE_EXIF
            ssImageView.setImage(ImageSource.uri(picture.uri))
            ssImageView.setOnImageEventListener(object : SubsamplingScaleImageView.DefaultOnImageEventListener() {
                override fun onImageLoaded() {
                    super.onImageLoaded()
                    staticImageView.visibility = View.INVISIBLE
                }
            })
            ssImageView.setOnClickListener {
                onImageViewClick()
            }
        }

        // Allow onImageViewClick() gesture while SSIV is loading
        val gestureDetector = GestureDetector(
                staticImageView.context,
                object : GestureDetector.SimpleOnGestureListener() {
                    override fun onSingleTapUp(e: MotionEvent?): Boolean {
                        onImageViewClick()
                        return super.onSingleTapUp(e)
                    }
                }
        )
        staticImageView.setOnTouchListener(View.OnTouchListener { _, motionEvent ->
            gestureDetector.onTouchEvent(motionEvent)
        })

        // Reset ViewPager touch listener; GestureView controller sets its own, which prevents
        // coherent scrolling of other ImageViews.
        activityReference.get()?.viewpager?.setOnTouchListener(DummyTouchListener())
    }

    private fun buildGestureImageView(layoutItem: View, picture: Picture) {

        val gestureImageView = layoutItem.gestureImageView
        picture.loadInto(gestureImageView)

        gestureImageView.controller.settings.isRotationEnabled = true
        gestureImageView.controller.settings.isRestrictRotation = true

        // Allow scrolling when zoomed in
        gestureImageView.controller.enableScrollInViewPager(activityReference.get()?.viewpager)

        gestureImageView.setOnClickListener {
            onImageViewClick()
        }
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
    }

    fun onImageViewClick() {
        activityReference.get()?.toggleUIs()
    }

}