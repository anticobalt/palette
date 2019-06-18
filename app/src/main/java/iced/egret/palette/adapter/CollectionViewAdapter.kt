package iced.egret.palette.adapter

import androidx.recyclerview.widget.RecyclerView
import iced.egret.palette.model.Coverable
import iced.egret.palette.recyclerview_component.CollectionViewSection
import iced.egret.palette.recyclerview_component.CoverViewHolder
import iced.egret.palette.util.CollectionManager

class CollectionViewAdapter(contents: MutableList<Coverable>) : CoverableAdapter() {

    private var mItems: MutableList<Coverable> = contents

    /**
     * Set all indications about selection status.
     * Requires mSelector to be properly activated or deactivated.
     */
    fun setAllIndications(rv: RecyclerView, section: CollectionViewSection) {
        for (pos in 0 until mItems.size) {
            val holder = rv.findViewHolderForAdapterPosition(pos)
            val positionInSection = getPositionInSection(pos)
            if (holder is CoverViewHolder) {
                section.indicateSelection(holder, positionInSection, section.selector.selectedItemIds)
            }
        }
    }

    /**
     * Fetch newest data and rebuild
     */
    fun update() {
        mItems.clear()
        mItems.addAll(CollectionManager.contents)
        notifyDataSetChanged()
    }

}