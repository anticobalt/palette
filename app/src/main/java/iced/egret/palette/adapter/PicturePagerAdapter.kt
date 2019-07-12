package iced.egret.palette.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter
import com.alexvasilkov.gestures.views.GestureFrameLayout
import com.bumptech.glide.Glide
import iced.egret.palette.R
import iced.egret.palette.activity.PictureViewActivity
import iced.egret.palette.model.Picture
import kotlinx.android.synthetic.main.activity_view_picture.*
import kotlinx.android.synthetic.main.item_view_picture.view.*
import java.lang.ref.WeakReference

class PicturePagerAdapter(private val pictures: List<Picture>, activity: PictureViewActivity) : PagerAdapter() {

    private val activityReference = WeakReference(activity)

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view == `object` as GestureFrameLayout
    }

    override fun getCount(): Int {
        return pictures.size
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {

        val layoutItem = LayoutInflater
                            .from(container.context)
                            .inflate(R.layout.item_view_picture, container, false)

        val bigImageView = layoutItem.bigImageView
        val normalImageView = layoutItem.normalImageView
        val gestureView = layoutItem.gestureFrameLayout

        // BigImageViews (or SSIVs) are not compatible with GestureViews, so use both
        // ImageView and BigImageView, depending on whether deep zoom is required or not
        Glide.with(normalImageView.context).load(pictures[position].uri).into(normalImageView)
        bigImageView.showImage(pictures[position].uri)

        gestureView.controller.settings.isRotationEnabled = true
        gestureView.controller.settings.isRestrictRotation = true

        // Allow scrolling when zoomed in
        gestureView.controller.enableScrollInViewPager(activityReference.get()?.viewpager)

        // Handle clicks
        layoutItem.setOnClickListener {
            activityReference.get()?.toggleUIs()
        }

        container.addView(layoutItem)
        return layoutItem

    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
    }

}