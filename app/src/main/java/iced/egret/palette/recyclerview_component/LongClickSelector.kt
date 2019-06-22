package iced.egret.palette.recyclerview_component

import iced.egret.palette.fragment.MainFragment
import iced.egret.palette.model.Coverable
import io.github.luizgrp.sectionedrecyclerviewadapter.StatelessSection
import java.lang.ref.WeakReference

/**
 * Basically a custom, stripped-down implementation of ActionMode, which doesn't exist for
 * RecyclerViews.
 *
 * Activated when long click occurs. Activation causes state change (e.g. switching to edit mode).
 * Can also decide to deactivate via onBackPressed().
 *
 * How to use:
 *  1. Ensure fragments implement onAlternateModeActivated() and onAlternateModeDeactivated(),
 *     so that fragment components (like toolbars) can change in response to mode changes.
 *  2. Adapters should call onItemClick() or onItemLongClick() when a ViewHolder's view is clicked.
 *     Ensure these two methods receive a CoverableClickListener; the listener will do the
 *     actual responding to clicks. The only thing LongClickSelector should do is decide which
 *     specific click type to emulate, based on state.
 *  3. LongClickSelector only manages selection state: it has nothing to do with actions done
 *     to selected items, responses to clicks, or visual representation of selection. All of those
 *     are delegated to fragment, adapter, or click listener.
 */
class LongClickSelector(fragment: MainFragment, val section: StatelessSection) {

    var active = false
    var selectedItemIds = ArrayList<Long>()

    var clickListener : CoverableClickListener? = null
    var fragmentReference = WeakReference(fragment)

    /**
     * @return If adapter data changed (true) or not (false)
     */
    fun onItemClick(item: Coverable,
                    position: Int,
                    holder: CoverViewHolder,
                    sharedClickListener: CoverableClickListener) : Boolean {

        var dataChanged : Boolean? = false
        clickListener = sharedClickListener
        clickListener?.setup(item, position, holder, section)

        if (active) {
            clickListener?.onItemAlternateClick(selectedItemIds)
            if (selectedItemIds.isEmpty()) {
                deactivate()
            }
        }
        else {
            dataChanged = clickListener?.onItemDefaultClick(selectedItemIds)
        }

        clickListener?.tearDown()
        return dataChanged ?: false

    }

    fun onItemLongClick(item: Coverable,
                        position: Int,
                        holder: CoverViewHolder,
                        sharedClickListener: CoverableClickListener) : Boolean {

        var handled = false
        clickListener = sharedClickListener
        clickListener?.setup(item, position, holder, section)

        if (!active) {
            clickListener?.onItemDefaultLongClick(selectedItemIds)
            clickListener?.tearDown()
            if (selectedItemIds.isNotEmpty()) {  // empty if clicking on non-deletable item
                activate()
            }
            handled = true
        }

        return handled
    }

    /**
     * @return Whether event handled here (true) or ignored (false)
     */
    fun onBackPressed() : Boolean {
        return if (active) {
            deactivate()
            true
        }
        else {
            false
        }
    }

    fun activate() {
        val fragment = fragmentReference.get()
        if (fragment is MainFragment) {
            active = true  // must be before fragment call
            fragment.onAlternateModeActivated(section)
        }
    }

    fun deactivate() {
        val fragment = fragmentReference.get()
        if (fragment is MainFragment) {
            selectedItemIds.clear()
            active = false  // must be before fragment call
            fragment.onAlternateModeDeactivated(section)
        }
    }

}

/**
 * Click listener used by LongClickSelector to manipulate models and views.
 */
abstract class CoverableClickListener {

    abstract fun setup(item: Coverable, position: Int, holder: CoverViewHolder, section: StatelessSection)
    abstract fun tearDown()

    /**
     * Short click action for when view is default/normal state
     *
     * @return Adapter data changed (true) or didn't (false)
     */
    abstract fun onItemDefaultClick(selectedItemIds: ArrayList<Long>) : Boolean

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