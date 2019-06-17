package iced.egret.palette.adapter

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import iced.egret.palette.R
import iced.egret.palette.fragment.CollectionViewFragment
import iced.egret.palette.model.Coverable
import iced.egret.palette.recyclerview_component.CoverViewHolder
import iced.egret.palette.recyclerview_component.CoverableClickListener
import iced.egret.palette.recyclerview_component.LongClickSelector
import iced.egret.palette.util.CollectionManager
import iced.egret.palette.util.MainFragmentManager
import java.lang.ref.WeakReference

class PinnedCollectionsAdapter(selector: LongClickSelector) : CoverableAdapter() {

    /**
     * Responds to click actions, by changing display or launching items.
     */
    class ActionClickListener : CoverableClickListener() {

        private var item: Coverable? = null
        private var position: Int? = null
        private var holder: CoverViewHolder? = null
        private var adapter: PinnedCollectionsAdapter? = null

        private var ready = false

        override fun setup(item: Coverable, position: Int, holder: CoverViewHolder, adapter: CoverableAdapter) {
            this.item = item
            this.position = position
            this.holder = holder
            this.adapter = adapter as PinnedCollectionsAdapter
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

            val fragmentIndex = MainFragmentManager.COLLECTION_CONTENTS
            val fragment =
                    MainFragmentManager.getFragmentByIndex(fragmentIndex) as CollectionViewFragment
            val viewPager = fragment.activity?.findViewById<ViewPager>(R.id.viewpagerMainFragments)
            // FIXME: animate slower e.g https://stackoverflow.com/a/28297483
            viewPager?.setCurrentItem(fragmentIndex, true)
            CollectionManager.clearStack()
            CollectionManager.launch(item!!, fragment.adapter)
            fragment.setDefaultToolbarTitle()
        }

        override fun onItemDefaultLongClick(selectedItemIds: ArrayList<Long>) {
            if (!ready) return
            if (!item!!.deletable) return

            val positionLong = position!!.toLong()
            if (positionLong in selectedItemIds) selectedItemIds.remove(positionLong)
            else selectedItemIds.add(positionLong)

            adapter!!.indicateSelectionStatus(holder!!, position!!, selectedItemIds)

        }

        override fun onItemAlternateClick(selectedItemIds: ArrayList<Long>) {
            if (!ready) return
            onItemDefaultLongClick(selectedItemIds)
        }

        override fun onItemAlternateLongClick(selectedItemIds: ArrayList<Long>) {}

    }

    private var mCollections = CollectionManager.getCollections().toMutableList()
    private lateinit var mContextReference : WeakReference<Context>

    private val mClickListener = ActionClickListener()
    private var mSelector = selector

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CoverViewHolder {
        mContextReference = WeakReference(parent.context)
        return CoverViewHolder(
                LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_pinned_collections, parent, false),
                textViewId = R.id.tvPinnedCollectionLabel,
                imageViewId = R.id.ivPinnedCollectionCover
        )
    }

    override fun getItemCount(): Int {
        return mCollections.size
    }

    override fun onBindCoverViewHolder(holder: CoverViewHolder, position: Int) {
        val context = mContextReference.get()
        if (context != null) {

            val item = mCollections[position]
            buildHolder(holder, item)

            // Indications must be remade in case ViewHolder is recycled,
            // even though it is sometimes redundant (e.g. on activity start)
            indicateIsSelectable(holder, item)
            indicateSelectionStatus(holder, position, mSelector.selectedItemIds)

            holder.itemView.setOnClickListener{
                mSelector.onItemClick(item, position, holder, this, mClickListener)
            }
            holder.itemView.setOnLongClickListener {
                mSelector.onItemLongClick(item, position, holder, this, mClickListener)
            }
        }
    }

    /**
     * Fill holder with mandatory elements
     */
    private fun buildHolder(holder: CoverViewHolder, item: Coverable) {

        item.loadCoverInto(holder)
        holder.tvItem?.text = item.toString()

        // Darken a little, so that white text is readable
        // https://stackoverflow.com/a/15896811
        holder.ivItem?.setColorFilter(
                Color.rgb(200, 200, 200),
                android.graphics.PorterDuff.Mode.MULTIPLY
        )
    }


    /**
     * Set all indications about selectability and selection status.
     * Requires mSelector to be properly activated or deactivated.
     */
    fun setAllIndications(rv: RecyclerView) {
        setAllIndicateIsSelectable(rv)
        for (pos in 0 until mCollections.size) {
            val holder = rv.findViewHolderForAdapterPosition(pos)
            if (holder is CoverViewHolder) indicateSelectionStatus(holder, pos, mSelector.selectedItemIds)
        }
    }

    /**
     * Indicate that certain covers are not selectable based on item attributes.
     * Requires mSelector to be properly activated or deactivated.
     */
    fun setAllIndicateIsSelectable(rv: RecyclerView) {
        for (pos in 0 until mCollections.size) {
            val item = mCollections[pos]
            if (!item.deletable) {
                val holder = rv.findViewHolderForAdapterPosition(pos)
                if (holder is CoverViewHolder) indicateIsSelectable(holder, item)
            }
        }
    }

    /**
     * Style holder to match selection status as described in selectedItemIds
     */
    private fun indicateSelectionStatus(holder: CoverViewHolder, position: Int, selectedItemIds: ArrayList<Long>) {

        val button = holder.itemView.findViewById<ImageButton>(R.id.btnSelect)

        if (position.toLong() in selectedItemIds) {
            button.visibility = View.VISIBLE
        }
        else {
            button.visibility = View.INVISIBLE
        }

    }

    /**
     * Style in order to hint to user that item is or isn't selectable
     */
    private fun indicateIsSelectable(holder: CoverViewHolder, item: Coverable) {
        if (mSelector.active && !item.deletable) {
            holder.itemView.alpha = 0.5F
        } else {
            holder.itemView.alpha = 1F
        }
    }

    fun updateCollections() {
        mCollections.clear()
        mCollections.addAll(CollectionManager.getCollections())
        notifyDataSetChanged()
    }

}