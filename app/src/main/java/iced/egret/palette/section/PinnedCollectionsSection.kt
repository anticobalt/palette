package iced.egret.palette.section

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
import iced.egret.palette.recyclerview_component.CoverViewHolder
import iced.egret.palette.recyclerview_component.CoverableClickListener
import iced.egret.palette.recyclerview_component.LongClickSelector
import iced.egret.palette.recyclerview_component.SectionHeaderViewHolder
import iced.egret.palette.util.CollectionManager
import iced.egret.palette.util.MainFragmentManager
import io.github.luizgrp.sectionedrecyclerviewadapter.SectionParameters
import io.github.luizgrp.sectionedrecyclerviewadapter.StatelessSection

class PinnedCollectionsSection(val title: String,
                               list: List<Coverable>,
                               private val adapter: PinnedCollectionsAdapter,
                               val fragment: MainFragment)
    : StatelessSection(SectionParameters.builder()
        .itemResourceId(R.layout.item_pinned_collections)
        .headerResourceId(R.layout.header_section_view_collection)
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

            fragment.notifyChanges()
            return true
        }

        override fun onItemDefaultLongClick(selectedItemIds: ArrayList<Long>) {
            if (!ready) return

            val positionLong = position!!.toLong()
            if (positionLong in selectedItemIds) selectedItemIds.remove(positionLong)
            else selectedItemIds.add(positionLong)

            section!!.indicateSelectionStatus(holder!!, position!!, selectedItemIds)
            section!!.isolateSelf(true)

        }

        override fun onItemAlternateClick(selectedItemIds: ArrayList<Long>) {
            if (!ready) return
            onItemDefaultLongClick(selectedItemIds)
        }

        override fun onItemAlternateLongClick(selectedItemIds: ArrayList<Long>) {}

    }

    private val mListener = ActionClickListener()
    val selector = LongClickSelector(fragment, this)
    val items = list.toMutableList()

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
        return CoverViewHolder(view, imageViewId = R.id.ivPinnedCollectionCover, textViewId = R.id.tvPinnedCollectionLabel)
    }

    override fun onBindItemViewHolder(holder: RecyclerView.ViewHolder?, position: Int) {

        holder as CoverViewHolder
        val item = items[position]
        buildHolder(holder, item)

        // Update selection visuals to match state
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
     * Make this section the only section visible in adapter, or not
     */
    fun isolateSelf(isolate: Boolean) {
        if (isolate) adapter.isolateSection(this)
        else adapter.showAllSections()
    }

}