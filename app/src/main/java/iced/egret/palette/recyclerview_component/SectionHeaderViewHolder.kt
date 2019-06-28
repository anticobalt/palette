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

}