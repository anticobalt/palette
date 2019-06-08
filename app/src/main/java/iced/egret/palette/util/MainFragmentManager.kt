package iced.egret.palette.util

import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import iced.egret.palette.R
import iced.egret.palette.fragment.CollectionViewFragment
import iced.egret.palette.fragment.MainFragment
import iced.egret.palette.fragment.PinnedCollectionsFragment

object MainFragmentManager {

    private lateinit var nativeFragmentManager : FragmentManager
    var fragments = ArrayList<Fragment>()
        private set

    fun setup(fm: FragmentManager) {
        nativeFragmentManager = fm
    }

    fun createFragments() {
        fragments.add(PinnedCollectionsFragment())
        fragments.add(CollectionViewFragment())
    }

    fun getFragmentByIndex(index: Int) : MainFragment {
        val currentFragment = nativeFragmentManager.findFragmentByTag(
                "android:switcher:" + R.id.viewpagerMainFragments + ":" + index
        )
        return currentFragment as MainFragment
    }

}