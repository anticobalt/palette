package iced.egret.palette.adapter

import androidx.recyclerview.widget.RecyclerView
import iced.egret.palette.section.CollectionViewSection
import iced.egret.palette.util.CollectionManager
import io.github.luizgrp.sectionedrecyclerviewadapter.Section

class CollectionViewAdapter(private val rv: RecyclerView) : CoverableAdapter() {

    private var mSections = mutableListOf<CollectionViewSection>()
    private var mExcludedPositions = mutableListOf<Int>()

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


        for (section in sectionsToRemove) {
            mExcludedPositions.add(getHeaderPositionInAdapter(section))
            for (index in 0 until section.items.size) {
                mExcludedPositions.add(getPositionInAdapter(section, index))
            }
        }

        sectionsToRemove.map { section -> section.backup() }
        sectionsToRemove.map { section -> super.removeSection(section) }
        notifyItemsRemoved(mExcludedPositions)

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
        mSections.map { section -> section.restore() }
        mSections.map { section -> super.addSection(section) }
        notifyItemsInserted(mExcludedPositions)
        mExcludedPositions.clear()
    }

    private fun notifyItemsInserted(positions: List<Int>) {
        for (pos in positions) {
            notifyItemInserted(pos)
        }
    }



}