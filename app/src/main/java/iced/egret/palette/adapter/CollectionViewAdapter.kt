package iced.egret.palette.adapter

import androidx.recyclerview.widget.RecyclerView
import iced.egret.palette.model.Coverable
import iced.egret.palette.section.CollectionViewSection
import iced.egret.palette.util.CollectionManager
import io.github.luizgrp.sectionedrecyclerviewadapter.Section

class CollectionViewAdapter(contents: MutableList<Coverable>,
                            private val rv: RecyclerView) : CoverableAdapter() {

    private var mItems: MutableList<Coverable> = contents
    private var mSections = mutableListOf<CollectionViewSection>()

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    /**
     * Fetch newest data and rebuild
     */
    fun update() {

        refreshItems()
        notifyDataSetChanged()

    }

    fun updateNewAlbum(positions : List<Int>) {
        refreshItems()

        val section = mSections.find {section -> section.title.toLowerCase() == "albums"} ?: return
        for (pos in positions) {
            notifyItemInserted(getPositionInAdapter(section, pos))
        }
    }

    private fun refreshItems() {
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
    }

    /**
     * Keep track of sections, so that they can be updated via the adapter
     */
    override fun addSection(section: Section?): String {
        mSections.add(section as CollectionViewSection)
        return super.addSection(section)
    }

    fun isolateSection(toIsolateSection: CollectionViewSection) {
        val sectionsToRemove = mSections.filter {section -> section != toIsolateSection}
        val positionsToRemove = mutableListOf<Int>()


        for (section in sectionsToRemove) {
            positionsToRemove.add(getHeaderPositionInAdapter(section))
            for (index in 0 until section.items.size) {
                positionsToRemove.add(getPositionInAdapter(section, index))
            }
        }

        for (index in 0 until toIsolateSection.items.size) {
            notifyItemChanged(getPositionInAdapter(toIsolateSection, index))
        }

        //sectionsToRemove.map { section -> section.backup(); section.items.clear() }
        sectionsToRemove.map { section -> super.removeSection(section) }
        notifyItemsRemoved(positionsToRemove)
        //notifyDataSetChanged()

        /*
        for (section in sectionsToRemove) {
            val start = getHeaderPositionInAdapter(section)
            val count = section.items.size
            notifyItemRangeRemoved(start, count)
        }
        sectionsToRemove.map { section -> super.removeSection(section) }*/

        /*
        sectionsToRemove.map { section -> super.removeSection(section) }
        for (index in 0 until mItems.size) {
            notifyItemChanged(index)
        }*/

    }

    private fun notifyItemsRemoved(positions: List<Int>) {
        for (pos in positions) {
            notifyItemRemoved(pos)
        }
    }

    /**
     * Need to remove all then add all again to retain order.
     */
    fun showAllSections() {
        mSections.map { section -> super.removeSection(section) }
        mSections.map { section -> super.addSection(section) }
        //mSections.map { section -> section.restore() }
        notifyDataSetChanged()
    }

}