package iced.egret.palette.flexible.item

import android.annotation.SuppressLint
import android.widget.TextView
import iced.egret.palette.R
import iced.egret.palette.flexible.item.inherited.CoverableItem
import iced.egret.palette.flexible.viewholder.CoverViewHolder
import iced.egret.palette.model.inherited.Coverable

class GridCoverableItem(obj: Coverable, iconRes : Int? = null) : CoverableItem(obj, null, iconRes) {

    override fun getLayoutRes(): Int {
        return R.layout.item_coverable_grid
    }

    @SuppressLint("SetTextI18n")  // blurb is language agnostic
    override fun setLabel(holder: CoverViewHolder) {
        val textContainer = holder.textContainer
        if (textContainer != null) {
            textContainer.findViewById<TextView>(R.id.label)?.text = coverable.name
            textContainer.findViewById<TextView>(R.id.blurb)?.text = "(${coverable.shortBlurb})"
        }
    }

}