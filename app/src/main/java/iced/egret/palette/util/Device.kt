package iced.egret.palette.util

import android.content.res.Resources

/**
 * Checks system UI elements.
 */
object Device {

    fun getStatusBarHeight(resources: Resources): Int {
        val resourceId = resources.getIdentifier("status_bar_height", "dimen", "android")
        return if (resourceId > 0) {
            resources.getDimensionPixelSize(resourceId)
        } else 0
    }

    /**
     * Get height in pixels of bottom navigation bar (present in devices without physical buttons).
     * https://stackoverflow.com/a/20264361
     */
    fun getNavigationBarHeight(resources: Resources): Int {
        if (!hasNavigationBar(resources)) return 0

        val id = resources.getIdentifier("navigation_bar_height", "dimen", "android")
        return if (id > 0) {
            resources.getDimensionPixelSize(id)
        } else 0
    }

    /**
     * Check if navigation bar actually exists.
     * https://stackoverflow.com/a/32698387
     */
    private fun hasNavigationBar(resources: Resources): Boolean {
        val id = resources.getIdentifier("config_showNavigationBar", "bool", "android")
        return id > 0 && resources.getBoolean(id)
    }

}