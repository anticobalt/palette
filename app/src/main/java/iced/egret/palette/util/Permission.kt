package iced.egret.palette.util

import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object Permission {

    fun isAccepted(activity: Activity, permission: String): Boolean {
        val status = ContextCompat.checkSelfPermission(activity, permission)
        return (status == PackageManager.PERMISSION_GRANTED)
    }

    fun request(activity: Activity, permission: String, requestCode: Int) {
        ActivityCompat.requestPermissions(activity, arrayOf(permission), requestCode)
    }

}