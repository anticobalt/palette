package iced.egret.palette.flexible

import android.annotation.SuppressLint
import android.widget.TextView
import iced.egret.palette.R
import iced.egret.palette.model.Coverable

class GridCoverableItem(obj: Coverable) : CoverableItem(obj, null) {

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