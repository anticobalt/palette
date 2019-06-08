package iced.egret.palette.adapter

import android.support.v7.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import android.widget.TextView

class CoverViewHolder(view : View,
                      textViewId : Int,
                      imageViewId : Int) : RecyclerView.ViewHolder(view) {

    val tvItem : TextView? = view.findViewById(textViewId)
    val ivItem : ImageView? = view.findViewById(imageViewId)

}