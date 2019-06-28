package iced.egret.palette.recyclerview_component

import androidx.recyclerview.widget.GridLayoutManager
import eu.davidea.flexibleadapter.FlexibleAdapter
import iced.egret.palette.R

class GridSectionSpanLookup(private val adapter: FlexibleAdapter<SectionHeaderItem>, private val spanCount: Int) : GridLayoutManager.SpanSizeLookup() {

    override fun getSpanSize(position: Int): Int {
        return when (adapter.getItemViewType(position)) {
            R.layout.header_section_view_collection -> spanCount
            else -> 1
        }
    }

}