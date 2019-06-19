package iced.egret.palette.recyclerview_component

import android.graphics.Color
import android.view.View
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import iced.egret.palette.R
import iced.egret.palette.adapter.CollectionViewAdapter
import iced.egret.palette.fragment.MainFragment
import iced.egret.palette.model.Coverable
import iced.egret.palette.util.CollectionManager
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionParameters
import io.github.luizgrp.sectionedrecyclerviewadapter.StatelessSection

class CollectionViewSection(val title: String,
                            list: List<Coverable>,
                            private val adapter: CollectionViewAdapter,
                            val fragment: MainFragment) :
        StatelessSection(SectionParameters.builder()
                .itemResourceId(R.layout.item_view_collection)
                .headerResourceId(R.layout.header_section_view_collection)
                .build()) {

    class OnItemClickListener : CoverableClickListener() {

        private var item: Coverable? = null
        private var position: Int? = null
        private var section: CollectionViewSection? = null
        private var holder: CoverViewHolder? = null

        private var ready = false

        override fun setup(item: Coverable, position: Int, holder: CoverViewHolder, section: StatelessSection) {
            this.item = item
            this.position = position
            this.holder = holder
            this.section = section as CollectionViewSection
            this.ready = true
        }

        override fun tearDown() {
            this.item = null
            this.position = null
            this.holder = null
            this.section = null
            this.ready = false
        }

        /**
         *@return Adapter data changed (true) or didn't (false)
         */
        override fun onItemDefaultClick(selectedItemIds: ArrayList<Long>) : Boolean {
            if (!ready) return false
            return CollectionManager.launch(item!!, holder!!, position!!)
        }

        override fun onItemDefaultLongClick(selectedItemIds: ArrayList<Long>) {
            if (!ready) return

            val positionLong = position!!.toLong()
            if (positionLong in selectedItemIds) selectedItemIds.remove(positionLong)
            else selectedItemIds.add(positionLong)

            section!!.indicateSelection(holder!!, position!!, selectedItemIds)
        }

        override fun onItemAlternateClick(selectedItemIds: ArrayList<Long>) {
            if (!ready) return
            onItemDefaultLongClick(selectedItemIds)
        }

        override fun onItemAlternateLongClick(selectedItemIds: ArrayList<Long>) {}

    }

    private val mListener = OnItemClickListener()
    val selector = LongClickSelector(fragment, this)
    val items = list.toMutableList()  // must make a copy to prevent side-effects

    override fun getContentItemsTotal(): Int {
        return items.size
    }

    override fun getHeaderViewHolder(view: View): RecyclerView.ViewHolder {
        return SectionHeaderViewHolder(view)
    }

    override fun onBindHeaderViewHolder(holder: RecyclerView.ViewHolder) {
        holder as SectionHeaderViewHolder
        holder.tvItem.text = title
    }

    override fun getItemViewHolder(view: View): RecyclerView.ViewHolder {
        return CoverViewHolder(view, imageViewId = R.id.ivCollectionItemImage, textViewId = R.id.tvCollectionItemText)
    }

    override fun onBindItemViewHolder(holder: RecyclerView.ViewHolder?, position: Int) {

        holder as CoverViewHolder
        val item = items[position]
        buildItemHolder(holder, item)

        // Update selection visuals to match state, so that they show
        // properly in the case that the ViewHolder is recycled
        indicateSelection(holder, position, selector.selectedItemIds)

        holder.itemView.setOnClickListener{
            val dataChanged = selector.onItemClick(item, position, holder, mListener)
            if (dataChanged) {
                adapter.update()
            }
        }
        holder.itemView.setOnLongClickListener{
            selector.onItemLongClick(item, position, holder, mListener)
        }

    }

    private fun buildItemHolder(holder: CoverViewHolder, item: Coverable) {
        item.loadCoverInto(holder)
        holder.tvItem?.text = item.toString()
    }

    /**
     * Show a checkmark + black tint if item is selected, otherwise reset visuals.
     */
    fun indicateSelection(holder: CoverViewHolder, position: Int, selectedItemIds: ArrayList<Long>) {

        val statusView = holder.itemView.findViewById<ImageView>(R.id.ivCollectionItemSelectStatus)
        if (position.toLong() in selectedItemIds) {
            statusView.visibility = View.VISIBLE
            holder.ivItem?.setColorFilter(
                    Color.rgb(200, 200, 200),
                    android.graphics.PorterDuff.Mode.MULTIPLY
            )
        } else {
            statusView.visibility = View.GONE
            holder.ivItem?.colorFilter = null
        }
    }

}