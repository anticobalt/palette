package iced.egret.palette.flexible

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IFlexible
import eu.davidea.viewholders.FlexibleViewHolder
import iced.egret.palette.R

class CoverViewHolder(view: View,
                      adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>)
    : FlexibleViewHolder(view, adapter) {

    val tvItem: TextView? = view.findViewById(R.id.label)
    val ivItem: ImageView? = view.findViewById(R.id.cover)

}