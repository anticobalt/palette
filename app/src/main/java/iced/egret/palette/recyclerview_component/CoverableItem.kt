package iced.egret.palette.recyclerview_component

import androidx.recyclerview.widget.RecyclerView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.AbstractFlexibleItem
import eu.davidea.flexibleadapter.items.IFlexible
import iced.egret.palette.model.Coverable

abstract class CoverableItem(private val obj: Coverable) : AbstractFlexibleItem<CoverViewHolder>() {

    override fun bindViewHolder(adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>,
                                holder: CoverViewHolder,
                                position: Int,
                                payloads: MutableList<Any>?) {

        obj.loadCoverInto(holder)
        holder.tvItem?.text = obj.name
    }

    override fun equals(other: Any?): Boolean {
        return this === other
    }

    override fun hashCode(): Int {
        return obj.hashCode()
    }

}