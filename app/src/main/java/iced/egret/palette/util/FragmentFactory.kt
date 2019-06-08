package iced.egret.palette.util

import android.support.v4.app.Fragment
import iced.egret.palette.fragment.CollectionViewFragment

object FragmentFactory {

    fun create() : ArrayList<Fragment> {
        val fragments = ArrayList<Fragment>()
        fragments.add(CollectionViewFragment())
        return fragments
    }

}