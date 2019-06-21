package iced.egret.palette.adapter

import androidx.recyclerview.widget.RecyclerView
import iced.egret.palette.model.Collection
import iced.egret.palette.recyclerview_component.CoverViewHolder
import iced.egret.palette.recyclerview_component.PinnedCollectionsSection
import iced.egret.palette.util.CollectionManager
import io.github.luizgrp.sectionedrecyclerviewadapter.Section

class PinnedCollectionsAdapter : CoverableAdapter() {

    private var mCollections = CollectionManager.getCollections().toMutableList()
    private var mSections = mutableListOf<PinnedCollectionsSection>()

    /**
     * Set all indications about selectability and selection status.
     * Requires mSelector to be properly activated or deactivated.
     */
    fun setAllIndications(rv: RecyclerView, section: PinnedCollectionsSection) {
        setAllIndicateIsSelectable(rv, section)
        for (pos in 0 until mCollections.size) {
            val holder = rv.findViewHolderForAdapterPosition(pos)
            if (holder is CoverViewHolder) section.indicateSelectionStatus(holder, pos, section.selector.selectedItemIds)
        }
    }

    /**
     * Indicate that certain covers are not selectable based on item attributes.
     * Requires mSelector to be properly activated or deactivated.
     */
    fun setAllIndicateIsSelectable(rv: RecyclerView, section: PinnedCollectionsSection) {
        for (pos in 0 until mCollections.size) {
            val item = mCollections[pos]
            if (!item.deletable) {
                val holder = rv.findViewHolderForAdapterPosition(pos)
                if (holder is CoverViewHolder) section.indicateIsSelectable(holder, item)
            }
        }
    }

    fun updateCollections() {

        mCollections.clear()
        mCollections.addAll(CollectionManager.getCollections())

        for (section in mSections) {
            var newItems = listOf<Collection>()
            when (section.title.toLowerCase()) {
                "folders" -> newItems = CollectionManager.folders
                "albums" -> newItems = CollectionManager.albums
            }
            section.items.clear()
            section.items.addAll(newItems)
        }

        notifyDataSetChanged()
    }

    /**
     * Keep track of sections, so that they can be updated via the adapter
     */
    override fun addSection(section: Section?): String {
        mSections.add(section as PinnedCollectionsSection)
        return super.addSection(section)
    }

}