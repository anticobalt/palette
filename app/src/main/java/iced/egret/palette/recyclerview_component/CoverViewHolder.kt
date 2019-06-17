package iced.egret.palette.recyclerview_component

import androidx.recyclerview.widget.RecyclerView
import android.view.View
import android.widget.ImageView
import android.widget.TextView

class CoverViewHolder(view : View,
                      textViewId : Int,
                      imageViewId : Int) : RecyclerView.ViewHolder(view) {

    val tvItem : TextView? = view.findViewById(textViewId)
    val ivItem : ImageView? = view.findViewById(imageViewId)

}