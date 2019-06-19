package iced.egret.palette.recyclerview_component

import androidx.recyclerview.widget.GridLayoutManager
import iced.egret.palette.adapter.CoverableAdapter
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionedRecyclerViewAdapter

/**
 * Used to make section headers span the entire width of the screen.
 */
class GridSectionSpanSizeLookup(private val adapter: CoverableAdapter,
                                private val spanCount: Int) : GridLayoutManager.SpanSizeLookup() {

    override fun getSpanSize(position: Int): Int {
        return when (adapter.getSectionItemViewType(position)) {
            SectionedRecyclerViewAdapter.VIEW_TYPE_HEADER -> spanCount
            else -> 1
        }
    }

}