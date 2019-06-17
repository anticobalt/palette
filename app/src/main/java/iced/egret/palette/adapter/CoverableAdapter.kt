package iced.egret.palette.adapter

import androidx.recyclerview.widget.RecyclerView
import iced.egret.palette.recyclerview_component.CoverViewHolder

abstract class CoverableAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        onBindCoverViewHolder(holder as CoverViewHolder, position)
    }

    /**
     * An alias, to increase naming consistency and dissociate link between
     * CoverableAdapter and RecyclerView.ViewHolder
     */
    abstract fun onBindCoverViewHolder(holder: CoverViewHolder, position: Int)

}

