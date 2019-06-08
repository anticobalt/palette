package iced.egret.palette.util

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import iced.egret.palette.R
import iced.egret.palette.fragment.CollectionViewFragment
import iced.egret.palette.fragment.MainFragment
import iced.egret.palette.fragment.PinnedCollectionsFragment

object MainFragmentManager {

    const val PINNED_COLLECTIONS = 0
    const val COLLECTION_CONTENTS = 1

    private lateinit var nativeFragmentManager : FragmentManager
    var fragments : Array<Fragment> = Array(6) {Fragment()}
        private set

    fun setup(fm: FragmentManager) {
        nativeFragmentManager = fm
    }

    fun createFragments() {
        fragments[PINNED_COLLECTIONS] = PinnedCollectionsFragment()
        fragments[COLLECTION_CONTENTS] = CollectionViewFragment()
    }

    fun getFragmentByIndex(index: Int) : MainFragment {
        val fragment = nativeFragmentManager.findFragmentByTag(
                "android:switcher:" + R.id.viewpagerMainFragments + ":" + index
        )
        return fragment as MainFragment
    }

}