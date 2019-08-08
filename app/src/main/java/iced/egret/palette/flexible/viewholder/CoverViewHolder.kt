package iced.egret.palette.flexible.viewholder

import android.view.View
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IFlexible
import eu.davidea.viewholders.FlexibleViewHolder
import iced.egret.palette.R

class CoverViewHolder(view: View,
                      adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>)
    : FlexibleViewHolder(view, adapter) {

    val textContainer: View? = view.findViewById(R.id.textContainer)
    val ivItem: ImageView? = view.findViewById(R.id.cover)

}