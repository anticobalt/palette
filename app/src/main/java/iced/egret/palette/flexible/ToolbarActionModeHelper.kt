package iced.egret.palette.flexible

import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import androidx.annotation.CallSuper
import androidx.annotation.MenuRes
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.RecyclerView
import eu.davidea.flexibleadapter.FlexibleAdapter
import eu.davidea.flexibleadapter.SelectableAdapter.Mode
import eu.davidea.flexibleadapter.utils.Log
import iced.egret.palette.flexible.item.inherited.CoverableItem

/**
 * A extension of ActionModeHelper (from FlexibleAdapter) that uses Toolbar
 * instead of Activity to start Action Mode, and handles item selection instead of delegating to
 * FlexibleAdapter.
 * https://github.com/davideas/FlexibleAdapter
 *
 * Literally copy-pasted the original class, converted to Kotlin,
 * changed the classes of some return objects and properties, and added selection.
 *
 * Selection handling added because the one in FlexibleAdapter is way too slow on large sets.
 *
 * ActionModes created with Toolbar.startActionMode() are the non-appcompat version.
 *
 * Also modified behaviour of onClick(), onLongClick() and toggleSelection() to handle
 * more advanced visual selection than the ones provided by FlexibleUtils.
 */
open class ToolbarActionModeHelper(adapter: FlexibleAdapter<*>,
                                   @MenuRes cabMenu: Int,
                                   callback: ActionMode.Callback? = null) : ActionMode.Callback {

    @Mode
    private var defaultMode = Mode.IDLE
    @MenuRes
    private var mCabMenu: Int = cabMenu
    private var disableSwipe: Boolean = false
    private var disableDrag: Boolean = false
    private var longPressDragDisabledByHelper: Boolean = false
    private var handleDragDisabledByHelper: Boolean = false
    private var swipeDisabledByHelper: Boolean = false
    private var mAdapter: FlexibleAdapter<*> = adapter
    private val mCallback: ActionMode.Callback? = callback
    var mActionMode: ActionMode? = null

    val selectedPositions = mutableSetOf<Int>()

    /**
     * Changes the default mode to apply when the ActionMode is destroyed and normal selection is
     * again active.
     *
     * Default value is [Mode.IDLE].
     *
     * @param defaultMode the new default mode when ActionMode is off, accepted values:
     * `IDLE, SINGLE`
     * @return this object, so it can be chained
     * @since 1.0.0-b1
     */
    fun withDefaultMode(@Mode defaultMode: Int): ToolbarActionModeHelper {
        if (defaultMode == Mode.IDLE || defaultMode == Mode.SINGLE)
            this.defaultMode = defaultMode
        return this
    }

    /**
     * Automatically disables LongPress drag and Handle drag capability when ActionMode is
     * activated and enable it again when ActionMode is destroyed.
     *
     * @param disableDrag true to disable the drag, false to maintain the drag during ActionMode
     * @return this object, so it can be chained
     * @since 1.0.0-b1
     */
    fun disableDragOnActionMode(disableDrag: Boolean): ToolbarActionModeHelper {
        this.disableDrag = disableDrag
        return this
    }

    /**
     * Automatically disables Swipe capability when ActionMode is activated and enable it again
     * when ActionMode is destroyed.
     *
     * @param disableSwipe true to disable the swipe, false to maintain the swipe during ActionMode
     * @return this object, so it can be chained
     * @since 1.0.0-b1
     */
    fun disableSwipeOnActionMode(disableSwipe: Boolean): ToolbarActionModeHelper {
        this.disableSwipe = disableSwipe
        return this
    }

    /**
     * @return the current instance of the ActionMode, `null` if ActionMode is off.
     * @since 1.0.0-b1
     */
    fun getActionMode(): ActionMode? {
        return mActionMode
    }

    /**
     * Implements the basic behavior of a CAB and multi select behavior.
     *
     * @param position the current item position
     * @return true if selection is changed, false if the click event should ignore the ActionMode
     * and continue
     * @since 1.0.0-b1
     */
    fun onClick(position: Int, item: CoverableItem?): Boolean {
        if (position != RecyclerView.NO_POSITION) {
            toggleSelection(position, item)
            return true
        }
        return false
    }

    /**
     * Implements the basic behavior of a CAB and multi select behavior onLongClick.
     *
     * @param toolbar the current Fragment's toolbar
     * @param position the position of the clicked item
     * @param item the clicked item
     * @return the initialized ActionMode or null if nothing was done
     * @since 1.0.0-b1
     */
    fun onLongClick(toolbar: Toolbar, position: Int, item: CoverableItem?): ActionMode? {
        // Activate ActionMode
        if (mActionMode == null) {
            mActionMode = toolbar.startActionMode(this)
        }
        // We have to select this on our own as we will consume the event
        toggleSelection(position, item)
        return mActionMode
    }

    /**
     * Toggle the selection state of an item.
     *
     * If the item was the last one in the selection and is unselected, the selection is stopped.
     * Note that the selection must already be started (actionMode must not be null).
     *
     * @param position position of the item to toggle the selection state
     * @param item: item whose visuals will be modified to represent selection state
     * @since 1.0.0-b1
     */
    fun toggleSelection(position: Int, item: CoverableItem?) {
        if (position >= 0 && (mAdapter.mode == Mode.SINGLE && position !in selectedPositions || mAdapter.mode == Mode.MULTI)) {
            item?.toggleSelection()
            if (position in selectedPositions) {
                selectedPositions.remove(position)
            } else {
                selectedPositions.add(position)
            }
        }
        // If SINGLE is active then ActionMode can be null
        if (mActionMode == null) return

        val count = selectedPositions.size
        if (count == 0) {
            mActionMode!!.finish()
        } else {
            updateContextTitle(count)
        }
    }

    /**
     * Updates the title of the Context Menu.
     *
     * Override to customize the title and subtitle.
     *
     * @param count the current number of selected items
     * @since 1.0.0-b1
     */
    open fun updateContextTitle(count: Int) {
        if (mActionMode != null) {
            mActionMode!!.title = count.toString()
        }
    }

    /**
     * Helper method to restart the action mode after a restoration of deleted items and after
     * screen rotation. The ActionMode will be activated only if
     * [FlexibleAdapter.getSelectedItemCount] has selections.
     *
     * To be called in the `onUndo` method after the restoration is done or at the end
     * of `onRestoreInstanceState`.
     *
     * @param toolbar the current Fragment's toolbar
     * @since 1.0.0-b1
     */
    fun restoreSelection(toolbar: Toolbar) {
        if (defaultMode == Mode.IDLE && selectedPositions.size > 0 || defaultMode == Mode.SINGLE && selectedPositions.size > 1) {
            onLongClick(toolbar, -1, null)
        }
    }

    @CallSuper
    override fun onCreateActionMode(actionMode: ActionMode, menu: Menu): Boolean {
        // Inflate the Context Menu
        actionMode.menuInflater.inflate(mCabMenu, menu)
        Log.d("ActionMode is active!")
        // Activate the ActionMode Multi
        mAdapter.mode = Mode.MULTI
        // Disable Swipe and Drag capabilities as per settings
        disableSwipeDragCapabilities()
        // Notify the provided callback
        return mCallback == null || mCallback.onCreateActionMode(actionMode, menu)
    }

    @CallSuper
    override fun onPrepareActionMode(actionMode: ActionMode, menu: Menu): Boolean {
        return mCallback != null && mCallback.onPrepareActionMode(actionMode, menu)
    }

    @CallSuper
    override fun onActionItemClicked(actionMode: ActionMode, item: MenuItem): Boolean {
        var consumed = false
        if (mCallback != null) {
            consumed = mCallback.onActionItemClicked(actionMode, item)
        }
        if (!consumed) {
            // Finish the actionMode
            actionMode.finish()
        }
        return consumed
    }

    /**
     * {@inheritDoc}
     * With FlexibleAdapter v5.0.0 the default mode is [Mode.IDLE], if
     * you want single selection enabled change default mode with [.withDefaultMode].
     */
    @CallSuper
    override fun onDestroyActionMode(actionMode: ActionMode) {
        Log.d("ActionMode is about to be destroyed!")
        // Change mode and deselect everything
        mAdapter.mode = defaultMode
        selectedPositions.clear()
        mActionMode = null
        // Re-enable Swipe and Drag capabilities if they were disabled by this helper
        enableSwipeDragCapabilities()
        // Notify the provided callback
        mCallback?.onDestroyActionMode(actionMode)
    }

    /**
     * Utility method to be called from Activity in many occasions such as: *onBackPressed*,
     * *onRefresh* for SwipeRefreshLayout, after *deleting* all selected items.
     *
     * @return true if ActionMode was active (in case it is also terminated), false otherwise
     * @since 1.0.0-b1
     */
    fun destroyActionModeIfCan(): Boolean {
        if (mActionMode != null) {
            mActionMode!!.finish()
            return true
        }
        return false
    }

    private fun enableSwipeDragCapabilities() {
        if (longPressDragDisabledByHelper) {
            longPressDragDisabledByHelper = false
            mAdapter.isLongPressDragEnabled = true
        }
        if (handleDragDisabledByHelper) {
            handleDragDisabledByHelper = false
            mAdapter.isHandleDragEnabled = true
        }
        if (swipeDisabledByHelper) {
            swipeDisabledByHelper = false
            mAdapter.isSwipeEnabled = true
        }
    }

    private fun disableSwipeDragCapabilities() {
        if (disableDrag && mAdapter.isLongPressDragEnabled) {
            longPressDragDisabledByHelper = true
            mAdapter.isLongPressDragEnabled = false
        }
        if (disableDrag && mAdapter.isHandleDragEnabled) {
            handleDragDisabledByHelper = true
            mAdapter.isHandleDragEnabled = false
        }
        if (disableSwipe && mAdapter.isSwipeEnabled) {
            swipeDisabledByHelper = true
            mAdapter.isSwipeEnabled = false
        }
    }

}