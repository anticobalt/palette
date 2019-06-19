package iced.egret.palette.recyclerview_component

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import iced.egret.palette.R

class SectionHeaderViewHolder(view: View) : RecyclerView.ViewHolder(view) {

    val tvItem = view.findViewById<TextView>(R.id.tvCollectionViewSectionHeader)

}