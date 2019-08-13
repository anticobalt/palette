package iced.egret.palette.activity.inherited

import iced.egret.palette.util.Storage

/**
 * A pager activity that depends on the the state of the app to be correct (i.e. CollectionManager
 * and Storage to be set up properly) to function.
 */
abstract class StatefulPagerActivity : PicturePagerActivity() {

    /**
     * If process is killed and this is the returning activity, trying to invoke StateBuilder
     * to set up the state again in onCreate() or onRestoreInstanceState() doesn't work.
     * So just get out if state is reset.
     *
     * Also get out if the picture doesn't exist anymore.
     */
    override fun onResume() {
        super.onResume()
        if (mPictures.size == 0 || !Storage.fileExists(mCurrentPicture.filePath)) finish()
    }

}