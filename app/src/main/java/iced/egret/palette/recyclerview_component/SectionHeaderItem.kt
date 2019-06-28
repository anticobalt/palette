package iced.egret.palette.recyclerview_component

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractExpandableHeaderItem
import eu.davidea.flexibleadapter.items.IFlexible
import iced.egret.palette.R

/**
 * A sticky header for an expandable section.
 * Has IFlexible, IExpandable, and IHeader functionality.
 */
class SectionHeaderItem(val title : String) : AbstractExpandableHeaderItem<SectionHeaderViewHolder, CoverableItem>() {

    override fun createViewHolder(view: View, adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>): SectionHeaderViewHolder {
        return SectionHeaderViewHolder(view, adapter)
    }

    override fun bindViewHolder(adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>,
                                holder: SectionHeaderViewHolder,
                                position: Int,
                                payloads: MutableList<Any>?) {

        holder.tvItem.text = title
    }

    override fun equals(other: Any?): Boolean {
        return this === other
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }

    override fun getLayoutRes(): Int {
        return R.layout.header_section_view_collection
    }

}