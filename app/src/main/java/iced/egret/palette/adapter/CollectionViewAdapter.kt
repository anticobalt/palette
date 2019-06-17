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
        private var adapter: CoverableAdapter? = null
        private var holder: CoverViewHolder? = null

        private var ready = false

        override fun setup(item: Coverable, position: Int, holder: CoverViewHolder, adapter: CoverableAdapter) {
            this.item = item
            this.position = position
            this.holder = holder
            this.adapter = adapter
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
            if (positionLong in selectedItemIds) {
                indicateSelection(holder!!, false)
                selectedItemIds.remove(positionLong)
            }
            else {
                indicateSelection(holder!!, true)
                selectedItemIds.add(positionLong)
            }
        }

        override fun onItemAlternateClick(selectedItemIds: ArrayList<Long>) {
            if (!ready) return
            onItemDefaultLongClick(selectedItemIds)
        }

        override fun onItemAlternateLongClick(selectedItemIds: ArrayList<Long>) {}

        private fun indicateSelection(holder: CoverViewHolder, selected: Boolean) {
            val statusView = holder.itemView.findViewById<ImageView>(R.id.ivCollectionItemSelectStatus)
            if (selected){
                statusView.visibility = View.VISIBLE
                holder.ivItem?.setColorFilter(
                        Color.rgb(200, 200, 200),
                        android.graphics.PorterDuff.Mode.MULTIPLY
                )
            }
            else {
                statusView.visibility = View.GONE
                holder.ivItem?.colorFilter = null
            }
        }

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

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val context = mContextReference.get()
        if (holder is CoverViewHolder && context != null) {
            val item = mItems[position]
            buildHolder(holder, item)
            indicateSelection(holder, position)
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
     * If this is not called, selection won't be visually represented initially.
     * Should be called indirectly by fragment on mode change.
     */
    private fun indicateSelection(holder: CoverViewHolder, position: Int) {

        val statusView = holder.itemView.findViewById<ImageView>(R.id.ivCollectionItemSelectStatus)
        if (position.toLong() in mSelector.selectedItemIds) {
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