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

    fun updateFragments(fragments: List<Fragment>) {
        loop@ for (fragment in fragments) {
            val index : Int = when (fragment) {
                is PinnedCollectionsFragment -> PINNED_COLLECTIONS
                is CollectionViewFragment -> COLLECTION_CONTENTS
                else -> continue@loop
            }
            this.fragments[index] = fragment
        }
    }

    fun notifyAlbumUpdateFromCollectionView() {
        (fragments[PINNED_COLLECTIONS] as PinnedCollectionsFragment).refreshFragment()
    }

}