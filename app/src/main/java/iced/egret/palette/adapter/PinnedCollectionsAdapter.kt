package iced.egret.palette.adapter

import iced.egret.palette.model.Collection
import iced.egret.palette.recyclerview_component.PinnedCollectionsSection
import iced.egret.palette.util.CollectionManager
import io.github.luizgrp.sectionedrecyclerviewadapter.Section

class PinnedCollectionsAdapter : CoverableAdapter() {

    private var mCollections = CollectionManager.getCollections().toMutableList()
    private var mSections = mutableListOf<PinnedCollectionsSection>()

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

    fun isolateSection(toIsolateSection: PinnedCollectionsSection) {
        (mSections.filter {section -> section != toIsolateSection}).map { section -> super.removeSection(section) }
        notifyDataSetChanged()
    }

    /**
     * Need to remove all then add all again to retain order.
     */
    fun showAllSections() {
        mSections.map { section -> super.removeSection(section) }
        mSections.map { section -> super.addSection(section) }
        notifyDataSetChanged()
    }

}