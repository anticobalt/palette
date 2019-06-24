package iced.egret.palette.util

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import iced.egret.palette.fragment.CollectionViewFragment
import iced.egret.palette.fragment.PinnedCollectionsFragment

object MainFragmentManager {

    const val PINNED_COLLECTIONS = 0
    const val COLLECTION_CONTENTS = 1
    const val NUM_FRAGMENTS = 2

    private lateinit var nativeFragmentManager : FragmentManager
    var fragments : Array<Fragment> = Array(NUM_FRAGMENTS) { Fragment() }
        private set

    fun setup(fm: FragmentManager) {
        nativeFragmentManager = fm
    }

    fun createFragments() {
        fragments[PINNED_COLLECTIONS] = PinnedCollectionsFragment()
        fragments[COLLECTION_CONTENTS] = CollectionViewFragment()
    }

    /**
     * A hack: https://stackoverflow.com/a/18611036
     * Good since 2013, so good enough for me
     *//*
    fun getFragmentByIndex(index: Int) : MainFragment {
        val fragment = nativeFragmentManager.findFragmentByTag(
                "android:switcher:" + R.id.viewpagerMainFragments + ":" + index
        )
        return fragment as MainFragment
    }*/

    fun notifyAlbumUpdateFromCollectionView() {
        (fragments[PINNED_COLLECTIONS] as PinnedCollectionsFragment).notifyChanges()
    }

}