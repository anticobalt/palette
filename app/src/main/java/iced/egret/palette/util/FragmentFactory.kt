package iced.egret.palette.util

import android.support.v4.app.Fragment
import iced.egret.palette.fragment.CollectionViewFragment
import iced.egret.palette.fragment.PinnedCollectionsFragment

object FragmentFactory {

    fun create() : ArrayList<Fragment> {
        val fragments = ArrayList<Fragment>()
        fragments.add(CollectionViewFragment())
        fragments.add(PinnedCollectionsFragment())
        return fragments
    }

}