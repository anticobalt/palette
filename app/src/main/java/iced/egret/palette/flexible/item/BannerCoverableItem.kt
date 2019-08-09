package iced.egret.palette.flexible.item

import android.widget.TextView
import iced.egret.palette.R
import iced.egret.palette.flexible.item.inherited.CoverableItem
import iced.egret.palette.flexible.viewholder.CoverViewHolder
import iced.egret.palette.model.inherited.Coverable
import iced.egret.palette.util.Painter

class BannerCoverableItem(obj: Coverable) : CoverableItem(obj, Painter.DARKEN_SLIGHT) {

    override fun getLayoutRes(): Int {
        return R.layout.item_coverable_banner
    }

    override fun setLabel(holder: CoverViewHolder) {
        val textContainer = holder.textContainer
        if (textContainer != null) {
            textContainer.findViewById<TextView>(R.id.label)?.text = coverable.name
            textContainer.findViewById<TextView>(R.id.blurb)?.text = coverable.longBlurb
        }
    }

}