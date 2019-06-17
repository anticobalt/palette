package iced.egret.palette.adapter

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import iced.egret.palette.R
import iced.egret.palette.model.Coverable
import iced.egret.palette.recyclerview_component.CoverViewHolder
import iced.egret.palette.recyclerview_component.CoverableClickListener
import iced.egret.palette.recyclerview_component.LongClickSelector
import iced.egret.palette.util.CollectionManager
import java.lang.ref.WeakReference


class CollectionViewAdapter(contents: MutableList<Coverable>, selector: LongClickSelector) : CoverableAdapter() {

    class OnItemClickListener : CoverableClickListener() {

        private var item: Coverable? = null
        private var position: Int? = null
        private var adapter: CollectionViewAdapter? = null
        private var holder: CoverViewHolder? = null

        private var ready = false

        override fun setup(item: Coverable, position: Int, holder: CoverViewHolder, adapter: CoverableAdapter) {
            this.item = item
            this.position = position
            this.holder = holder
            this.adapter = adapter as CollectionViewAdapter
            this.ready = true
        }

        override fun tearDown() {
            this.item = null
            this.position = null
            this.holder = null
            this.adapter = null
            this.ready = false
        }

        override fun onItemDefaultClick(selectedItemIds: ArrayList<Long>) {
            if (!ready) return
            CollectionManager.launch(item!!, adapter as CollectionViewAdapter, position!!)
        }

        override fun onItemDefaultLongClick(selectedItemIds: ArrayList<Long>) {
            if (!ready) return

            val positionLong = position!!.toLong()
            if (positionLong in selectedItemIds) selectedItemIds.remove(positionLong)
            else selectedItemIds.add(positionLong)

            adapter!!.indicateSelection(holder!!, position!!, selectedItemIds)
        }

        override fun onItemAlternateClick(selectedItemIds: ArrayList<Long>) {
            if (!ready) return
            onItemDefaultLongClick(selectedItemIds)
        }

        override fun onItemAlternateLongClick(selectedItemIds: ArrayList<Long>) {}

    }

    private var mItems: MutableList<Coverable> = contents
    private val mListener = OnItemClickListener()
    private lateinit var mContextReference : WeakReference<Context>
    private val mSelector = selector

    override fun getItemCount(): Int {
        return mItems.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CoverViewHolder {
        mContextReference = WeakReference(parent.context)
        return CoverViewHolder(
                LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_view_collection, parent, false),
                imageViewId = R.id.ivCollectionItemImage,
                textViewId = R.id.tvCollectionItemText
        )
    }

    override fun onBindCoverViewHolder(holder: CoverViewHolder, position: Int) {
        val context = mContextReference.get()
        if (context != null) {

            val item = mItems[position]
            buildHolder(holder, item)

            // Update selection visuals to match state, so that they show
            // properly in the case that the ViewHolder is recycled
            indicateSelection(holder, position, mSelector.selectedItemIds)

            holder.itemView.setOnClickListener{
                mSelector.onItemClick(item, position, holder, this, mListener)
            }
            holder.itemView.setOnLongClickListener{
                mSelector.onItemLongClick(item, position, holder, this, mListener)
            }
        }
    }

    private fun buildHolder(holder: CoverViewHolder, item: Coverable) {
        item.loadCoverInto(holder)
        holder.tvItem?.text = item.toString()
    }

    /**
     * Set all indications about selection status.
     * Requires mSelector to be properly activated or deactivated.
     */
    fun setAllIndications(rv: RecyclerView) {
        for (pos in 0 until mItems.size) {
            val holder = rv.findViewHolderForAdapterPosition(pos)
            if (holder is CoverViewHolder) indicateSelection(holder, pos, mSelector.selectedItemIds)
        }
    }

    /**
     * Show a checkmark + black tint if item is selected, otherwise reset visuals.
     */
    private fun indicateSelection(holder: CoverViewHolder, position: Int, selectedItemIds: ArrayList<Long>) {

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
    fun update(items: MutableList<Coverable>) {
        // mItems = items doesn't work
        // Need to clear + allAll, or onclick will refer to old items
        mItems.clear()
        mItems.addAll(items)
        notifyDataSetChanged()
    }

    fun getContext() : Context? {
        return mContextReference.get()
    }

}