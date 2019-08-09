package iced.egret.palette.flexible.viewholder

import android.view.View
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.items.IFlexible
import eu.davidea.viewholders.FlexibleViewHolder
import iced.egret.palette.R

class FileViewHolder(val view: View,
                     adapter: FlexibleAdapter<IFlexible<RecyclerView.ViewHolder>>)
    : FlexibleViewHolder(view, adapter) {

    val textView: TextView = view.findViewById(R.id.path)
    val checkBox: CheckBox = view.findViewById(R.id.checkbox)

}