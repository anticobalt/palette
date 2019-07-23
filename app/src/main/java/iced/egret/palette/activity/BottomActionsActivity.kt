package iced.egret.palette.activity

import kotlinx.android.synthetic.main.activity_view_picture.*

/**
 * An activity with a bar at the bottom for various actions.
 * The bar will sit above any existing system navigation bar.
 */
abstract class BottomActionsActivity : BaseActivity() {

    /**
     * Should be extended. Bottom action bar MUST be have id "bottomActions".
     */
    protected open fun buildBottomActions() {
        bottomActions.setPadding(0, 0, 0, getNavigationBarHeight())
    }

    /**
     * Get height in pixels of bottom navigation bar (present in devices without physical buttons).
     * https://stackoverflow.com/a/20264361
     */
    private fun getNavigationBarHeight() : Int {
        if (!hasNavigationBar()) return 0

        val id = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        return if (id > 0) {
            resources.getDimensionPixelSize(id)
        }
        else 0
    }

    /**
     * Check if navigation bar actually exists.
     * https://stackoverflow.com/a/32698387
     */
    private fun hasNavigationBar() : Boolean {
        val id = resources.getIdentifier("config_showNavigationBar", "bool", "android")
        return id > 0 && resources.getBoolean(id)
    }

}