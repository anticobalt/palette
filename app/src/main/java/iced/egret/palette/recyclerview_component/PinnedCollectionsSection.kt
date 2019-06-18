package iced.egret.palette.recyclerview_component

import android.graphics.Color
import android.view.View
import android.widget.ImageButton
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager.widget.ViewPager
import iced.egret.palette.R
import iced.egret.palette.adapter.PinnedCollectionsAdapter
import iced.egret.palette.fragment.CollectionViewFragment
import iced.egret.palette.fragment.MainFragment
import iced.egret.palette.model.Coverable
import iced.egret.palette.util.CollectionManager
import iced.egret.palette.util.MainFragmentManager
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionParameters
import io.github.luizgrp.sectionedrecyclerviewadapter.StatelessSection

class PinnedCollectionsSection(private val items: List<Coverable>,
                               private val adapter: PinnedCollectionsAdapter,
                               val fragment: MainFragment)
    : StatelessSection(SectionParameters.builder()
        .itemResourceId(R.layout.item_pinned_collections)
        .build()) {

    /**
     * Responds to click actions, by changing display or launching items.
     */
    class ActionClickListener : CoverableClickListener() {

        private var item: Coverable? = null
        private var position: Int? = null
        private var holder: CoverViewHolder? = null
        private var section: PinnedCollectionsSection? = null

        private var ready = false

        override fun setup(item: Coverable, position: Int, holder: CoverViewHolder, section: StatelessSection) {
            this.item = item
            this.position = position
            this.holder = holder
            this.section = section as PinnedCollectionsSection
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
         * Handles the adapter updating, because the adapter to update does not belong
         * to the current fragment.
         *
         *@return Adapter data changed (true) or didn't (false)
         */
        override fun onItemDefaultClick(selectedItemIds: ArrayList<Long>) : Boolean {
            if (!ready) return false

            val fragmentIndex = MainFragmentManager.COLLECTION_CONTENTS
            val fragment =
                    MainFragmentManager.getFragmentByIndex(fragmentIndex) as CollectionViewFragment
            val viewPager = fragment.activity?.findViewById<ViewPager>(R.id.viewpagerMainFragments)

            // FIXME: animate slower e.g https://stackoverflow.com/a/28297483
            viewPager?.setCurrentItem(fragmentIndex, true)
            CollectionManager.clearStack()
            CollectionManager.launch(item!!, holder!!)  // == true
            fragment.setDefaultToolbarTitle()

            val adapter = fragment.adapter
            adapter.update()
            return true
        }

        override fun onItemDefaultLongClick(selectedItemIds: ArrayList<Long>) {
            if (!ready) return
            if (!item!!.deletable) return

            val positionLong = position!!.toLong()
            if (positionLong in selectedItemIds) selectedItemIds.remove(positionLong)
            else selectedItemIds.add(positionLong)

            section!!.indicateSelectionStatus(holder!!, position!!, selectedItemIds)

        }

        override fun onItemAlternateClick(selectedItemIds: ArrayList<Long>) {
            if (!ready) return
            onItemDefaultLongClick(selectedItemIds)
        }

        override fun onItemAlternateLongClick(selectedItemIds: ArrayList<Long>) {}

    }

    private val mListener = ActionClickListener()
    val selector = LongClickSelector(fragment, this)

    override fun getContentItemsTotal(): Int {
        return items.size
    }

    override fun onBindItemViewHolder(holder: RecyclerView.ViewHolder?, position: Int) {

        holder as CoverViewHolder
        val item = items[position]
        buildHolder(holder, item)

        // Indications must be remade in case ViewHolder is recycled,
        // even though it is sometimes redundant (e.g. on activity start)
        indicateIsSelectable(holder, item)
        indicateSelectionStatus(holder, position, selector.selectedItemIds)

        holder.itemView.setOnClickListener{
            selector.onItemClick(item, position, holder, mListener)
        }
        holder.itemView.setOnLongClickListener {
            selector.onItemLongClick(item, position, holder, mListener)
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
     * Style holder to match selection status as described in selectedItemIds
     */
    fun indicateSelectionStatus(holder: CoverViewHolder, position: Int, selectedItemIds: ArrayList<Long>) {

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
    fun indicateIsSelectable(holder: CoverViewHolder, item: Coverable) {
        if (selector.active && !item.deletable) {
            holder.itemView.alpha = 0.5F
        } else {
            holder.itemView.alpha = 1F
        }
    }

    override fun getItemViewHolder(view: View): RecyclerView.ViewHolder {
        return CoverViewHolder(view, imageViewId = R.id.ivPinnedCollectionCover, textViewId = R.id.tvPinnedCollectionLabel)
    }
}