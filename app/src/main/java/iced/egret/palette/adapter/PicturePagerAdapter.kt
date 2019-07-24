package iced.egret.palette.adapter

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.preference.PreferenceManager
import androidx.viewpager.widget.PagerAdapter
import iced.egret.palette.R
import iced.egret.palette.activity.PictureViewActivity
import iced.egret.palette.model.Picture
import kotlinx.android.synthetic.main.activity_view_picture.*
import kotlinx.android.synthetic.main.item_view_picture_big.view.*
import kotlinx.android.synthetic.main.item_view_picture_gestures.view.*
import java.lang.ref.WeakReference

class PicturePagerAdapter(private val pictures: MutableList<Picture>, activity: PictureViewActivity) : PagerAdapter() {

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
     * BigImageViews (or SSIVs) are not compatible with GestureViews, so use both
     * ImageView and BigImageView, depending on whether deep zoom is required or not.
     */
    override fun instantiateItem(container: ViewGroup, position: Int): Any {

        val deepZoomOn = PreferenceManager
                .getDefaultSharedPreferences(activityReference.get())
                .getBoolean(activityReference.get()?.getString(R.string.deep_zoom_key), false)

        // Set layout
        val layoutItem = if (deepZoomOn) {
            LayoutInflater
                    .from(container.context)
                    .inflate(R.layout.item_view_picture_big, container, false)
        } else {
            LayoutInflater
                    .from(container.context)
                    .inflate(R.layout.item_view_picture_gestures, container, false)
        }

        // Build ImageView; each needs its own onClickListener
        if (deepZoomOn) {
            val bigImageView = layoutItem.bigImageView
            bigImageView.showImage(pictures[position].uri)
            // Reset touch listener; GestureView controller sets its own, which prevents
            // coherent scrolling of BigImageViews.
            activityReference.get()?.viewpager?.setOnTouchListener(DummyTouchListener())
            bigImageView.setOnClickListener {
                activityReference.get()?.toggleUIs()
            }
        } else {
            val gestureImageView = layoutItem.gestureImageView
            pictures[position].loadPictureInto(gestureImageView)
            gestureImageView.controller.settings.isRotationEnabled = true
            gestureImageView.controller.settings.isRestrictRotation = true
            // Allow scrolling when zoomed in
            gestureImageView.controller.enableScrollInViewPager(activityReference.get()?.viewpager)
            gestureImageView.setOnClickListener {
                activityReference.get()?.toggleUIs()
            }
        }

        container.addView(layoutItem)
        return layoutItem

    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
    }

}