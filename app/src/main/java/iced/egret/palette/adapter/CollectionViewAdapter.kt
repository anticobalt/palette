package iced.egret.palette.adapter

import iced.egret.palette.model.Coverable
import iced.egret.palette.recyclerview_component.CollectionViewSection
import iced.egret.palette.util.CollectionManager
import io.github.luizgrp.sectionedrecyclerviewadapter.Section

class CollectionViewAdapter(contents: MutableList<Coverable>) : CoverableAdapter() {

    private var mItems: MutableList<Coverable> = contents
    private var mSections = mutableListOf<CollectionViewSection>()

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