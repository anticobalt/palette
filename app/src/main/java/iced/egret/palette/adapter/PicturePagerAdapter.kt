package iced.egret.palette.adapter

import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.preference.PreferenceManager
import androidx.viewpager.widget.PagerAdapter
import com.davemorrissey.labs.subscaleview.ImageSource
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
     * SSIVs are not compatible with GestureViews, so use both
     * GestureImageView and SSIV, depending on whether true zoom is required or not.
     */
    override fun instantiateItem(container: ViewGroup, position: Int): Any {

        val picture = pictures[position]
        val trueZoomOn = PreferenceManager
                .getDefaultSharedPreferences(activityReference.get())
                .getBoolean(activityReference.get()?.getString(R.string.true_zoom_key), false)

        // Set layout and build ImageView
        val layoutItem : View
        if (trueZoomOn && picture.isJpgOrPng) {
            layoutItem = LayoutInflater
                    .from(container.context)
                    .inflate(R.layout.item_view_picture_big, container, false)
            buildSSIV(layoutItem, picture)
        } else {
            layoutItem = LayoutInflater
                    .from(container.context)
                    .inflate(R.layout.item_view_picture_gestures, container, false)
            buildGestureImageView(layoutItem, picture)
        }

        container.addView(layoutItem)
        return layoutItem

    }

    private fun buildSSIV(layoutItem: View, picture: Picture) {
        val ssImageView = layoutItem.ssImageView
        ssImageView.setImage(ImageSource.uri(picture.uri))

        // Reset touch listener; GestureView controller sets its own, which prevents
        // coherent scrolling of BigImageViews.
        activityReference.get()?.viewpager?.setOnTouchListener(DummyTouchListener())

        ssImageView.setOnClickListener {
            activityReference.get()?.toggleUIs()
        }
    }

    private fun buildGestureImageView(layoutItem: View, picture: Picture) {
        val gestureImageView = layoutItem.gestureImageView
        picture.loadPictureInto(gestureImageView)

        gestureImageView.controller.settings.isRotationEnabled = true
        gestureImageView.controller.settings.isRestrictRotation = true

        // Allow scrolling when zoomed in
        gestureImageView.controller.enableScrollInViewPager(activityReference.get()?.viewpager)

        gestureImageView.setOnClickListener {
            activityReference.get()?.toggleUIs()
        }
    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
    }

}