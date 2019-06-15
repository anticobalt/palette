package iced.egret.palette.adapter

import iced.egret.palette.model.Coverable

/**
 * Basically a custom, stripped-down implementation of ActionMode, which doesn't exist for
 * RecyclerViews.
 *
 * Keeps track of selected Coverable items, and delegates click actions to a communal
 * CoverableClickListener, which MUST be setup manually with the proper item's values on click.
 *
 * Activated when long click occurs. Activation causes state change (e.g. switching to edit mode).
 */
class LongClickSelector {

    private var active = false
    private var selectedItemIds = ArrayList<Long>()

    var clickListener : CoverableClickListener? = null

    fun onItemClick(item: Coverable, position: Int, holder: CoverViewHolder, sharedClickListener: CoverableClickListener) {

        clickListener = sharedClickListener
        clickListener?.setup(item, position, holder)

        if (active) {
            clickListener?.onItemAlternateClick(selectedItemIds)
        }
        else {
            clickListener?.onItemDefaultClick(selectedItemIds)
        }

        clickListener?.tearDown()
    }

    fun onItemLongClick(item: Coverable, position: Int, holder: CoverViewHolder, sharedClickListener: CoverableClickListener) : Boolean {

        clickListener = sharedClickListener
        clickListener?.setup(item, position, holder)

        return if (active) {
            clickListener?.tearDown()
            false
        }
        else {
            active = true
            clickListener?.onItemDefaultLongClick(selectedItemIds)
            clickListener?.tearDown()
            true
        }
    }

}

/**
 * Click listener used by LongClickSelector to manipulate models and views.
 */
abstract class CoverableClickListener {

    abstract fun setup(item: Coverable, position: Int, holder: CoverViewHolder)
    abstract fun tearDown()

    /**
     * Short click action for when view is default/normal state
     */
    abstract fun onItemDefaultClick(selectedItemIds: ArrayList<Long>)

    /**
     * Long click action for when view is default/normal state
     */
    abstract fun onItemDefaultLongClick(selectedItemIds: ArrayList<Long>)

    /**
     * Short click action for when view is alternate state and LongClickSelector is activated
     */
    abstract fun onItemAlternateClick(selectedItemIds: ArrayList<Long>)

    /**
     * Long click action for when view is alternate state and LongClickSelector is activated
     */
    abstract fun onItemAlternateLongClick(selectedItemIds: ArrayList<Long>)
}