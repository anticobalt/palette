package iced.egret.palette.adapter

import androidx.recyclerview.widget.RecyclerView
import iced.egret.palette.model.Coverable
import iced.egret.palette.recyclerview_component.CollectionViewSection
import iced.egret.palette.recyclerview_component.CoverViewHolder
import iced.egret.palette.util.CollectionManager
import io.github.luizgrp.sectionedrecyclerviewadapter.Section

class CollectionViewAdapter(contents: MutableList<Coverable>) : CoverableAdapter() {

    private var mItems: MutableList<Coverable> = contents
    private var mSections = mutableListOf<CollectionViewSection>()

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

        // Map<LowerCaseString, List<Coverable>>
        val contentsMap = CollectionManager.getContentsMap()

        for (section in mSections) {
            val sectionItems = contentsMap[section.title.toLowerCase()]
            if (sectionItems != null) {
                section.items.clear()
                section.items.addAll(sectionItems)
            }
        }

        notifyDataSetChanged()

    }

    /**
     * Keep track of sections, so that they can be updated via the adapter
     */
    override fun addSection(section: Section?): String {
        mSections.add(section as CollectionViewSection)
        return super.addSection(section)
    }

}