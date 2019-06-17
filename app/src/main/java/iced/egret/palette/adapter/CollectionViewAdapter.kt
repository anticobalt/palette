package iced.egret.palette.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
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

        private var ready = false

        override fun setup(item: Coverable, position: Int, holder: CoverViewHolder, adapter: CoverableAdapter) {
            this.item = item
            this.position = position
            this.adapter = adapter
            this.ready = true
        }

        override fun tearDown() {
            this.item = null
            this.position = null
            this.adapter = null
            this.ready = false
        }

        override fun onItemDefaultClick(selectedItemIds: ArrayList<Long>) {
            if (!ready) return
            CollectionManager.launch(item!!, adapter as CollectionViewAdapter, position!!)
        }

        override fun onItemDefaultLongClick(selectedItemIds: ArrayList<Long>) {
            //TODO
        }

        override fun onItemAlternateClick(selectedItemIds: ArrayList<Long>) {
            //TODO
        }

        override fun onItemAlternateLongClick(selectedItemIds: ArrayList<Long>) {
            //TODO
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
            item.loadCoverInto(holder)
            holder.tvItem?.text = item.toString()
            holder.itemView.setOnClickListener{
                mSelector.onItemClick(item, position, holder, this, mListener)
            }
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