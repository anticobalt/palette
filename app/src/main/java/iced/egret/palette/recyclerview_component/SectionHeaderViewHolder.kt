package iced.egret.palette.recyclerview_component

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IFlexible
import eu.davidea.viewholders.ExpandableViewHolder
import iced.egret.palette.R

class SectionHeaderViewHolder(view: View,
                              adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>> = FlexibleAdapter(null))
    : ExpandableViewHolder(view, adapter) {

    val tvItem = view.findViewById<TextView>(R.id.tvCollectionViewSectionHeader)

    // A hacky way to resolve sections being duplicated after CoverableItems are deselected.
    // Straight-up using a non-expandable implementation (e.g. just a normal FlexibleViewHolder)
    // does not work because it also duplicates sections when restoring them.
    // At least with expandable sections (i.e. Use Case 2 from https://github.com/davideas/
    // FlexibleAdapter/wiki/5.x-%7C-Headers-and-Sections), the duplication can be avoided
    // until section expansion is invoked.

    override fun isViewExpandableOnClick(): Boolean {
        return false
    }
    override fun isViewCollapsibleOnClick(): Boolean {
        return false
    }
    override fun isViewCollapsibleOnLongClick(): Boolean {
        return false
    }
}