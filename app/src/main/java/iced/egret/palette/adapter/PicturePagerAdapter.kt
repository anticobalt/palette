package iced.egret.palette.adapter

import android.support.v4.view.PagerAdapter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.github.piasy.biv.view.BigImageView
import iced.egret.palette.R
import iced.egret.palette.model.Picture

class PicturePagerAdapter(private val pictures: MutableList<Picture>) : PagerAdapter() {

    override fun isViewFromObject(view: View, `object`: Any): Boolean {
        return view == `object` as LinearLayout
    }

    override fun getCount(): Int {
        return pictures.size
    }

    override fun instantiateItem(container: ViewGroup, position: Int): Any {

        val layoutItem = LayoutInflater
                            .from(container.context)
                            .inflate(R.layout.item_view_picture, container, false)
        val imageView = layoutItem.findViewById<BigImageView>(R.id.bivPicture)

        imageView.showImage(pictures[position].uri)
        container.addView(layoutItem)
        return layoutItem

    }

    override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
        container.removeView(`object` as View)
    }

}