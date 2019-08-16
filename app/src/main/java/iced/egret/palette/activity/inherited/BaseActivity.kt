package iced.egret.palette.activity.inherited

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.documentfile.provider.DocumentFile
import iced.egret.palette.R
import iced.egret.palette.util.Painter
import java.io.File

/**
 * Base class that all activities extend from.
 *
 * Handles basic colouring of UI items, SD card access, and generally hosts various common helper
 * functions that most activities need.
 */
abstract class BaseActivity : AppCompatActivity() {

    enum class ColorType { PRIMARY, ACCENT, ITEM }

    protected lateinit var defSharedPreferences: SharedPreferences
    protected lateinit var sharedPrefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        defSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        sharedPrefs = getSharedPreferences(getString(R.string.preference_file_key), Context.MODE_PRIVATE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == RESULT_OK) {
            when (requestCode) {
                SD_CARD_WRITE_REQUEST -> tryAccessSdCard(data)
            }
        }
    }

    protected fun hasSdCardAccess(): Boolean {
        return sharedPrefs.getString(getString(R.string.sd_card_uri_key), null) != null
    }

    /**
     * Get write access to SD card, and save SD card URI to preferences.
     * https://stackoverflow.com/a/43317703
     */
    private fun tryAccessSdCard(intent: Intent?) {
        val sdTreeUri = intent?.data
        if (sdTreeUri == null) {
            toast(R.string.error_sd_access)
            return
        }

        // Try to get SD card
        val directory = DocumentFile.fromTreeUri(this, sdTreeUri)
        if (directory?.name == null) {
            toast(R.string.error_sd_connect)
            return
        }

        val modeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        grantUriPermission(packageName, sdTreeUri, modeFlags)
        contentResolver.takePersistableUriPermission(sdTreeUri, modeFlags)

        with(sharedPrefs.edit()) {
            putString(getString(R.string.sd_card_uri_key), sdTreeUri.toString())
            apply()
        }

        toast(R.string.success_sd_connect)
    }

    fun toast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }

    fun toast(resInt: Int) {
        Toast.makeText(this, resInt, Toast.LENGTH_SHORT).show()
    }

    fun toastLong(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    fun toastLong(resInt: Int) {
        Toast.makeText(this, resInt, Toast.LENGTH_LONG).show()
    }

    fun getSdCardDocumentFile(): DocumentFile? {
        val preferences = getSharedPreferences(
                getString(R.string.preference_file_key),
                Context.MODE_PRIVATE
        )
        val uriAsString = preferences
                .getString(getString(R.string.sd_card_uri_key), null) ?: return null
        val uri = Uri.parse(uriAsString)
        return DocumentFile.fromTreeUri(this, uri)
    }

    /**
     * Broadcast changes so that they show up immediately whenever MediaStore is accessed.
     * https://stackoverflow.com/a/39241495
     */
    fun broadcastMediaChanged(file: File) {
        val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
        val uri = Uri.fromFile(file)
        mediaScanIntent.data = uri
        sendBroadcast(mediaScanIntent)
    }

    /**
     * Colors a normal support toolbar, status bar, and navigation bar. Nested items not supported.
     */
    fun colorStandardElements(toolbar: Toolbar) {
        val primaryColor = getColorInt(ColorType.PRIMARY)
        val itemColor = getColorInt(ColorType.ITEM)

        for (i in 0 until toolbar.menu.size()) {
            val item = toolbar.menu.getItem(i)
            item.icon?.setTint(itemColor)
        }
        toolbar.setTitleTextColor(itemColor)
        toolbar.navigationIcon?.setTint(itemColor)
        toolbar.overflowIcon?.setTint(itemColor)
        toolbar.setBackgroundColor(primaryColor)

        window.statusBarColor = Painter.getMaterialDark(primaryColor)
        window.navigationBarColor = Painter.getMaterialDark(primaryColor)
    }

    fun idToColor(colorResId: Int) = ContextCompat.getColor(this, colorResId)

    fun getColorInt(type: ColorType): Int {
        val keyRef = when (type) {
            ColorType.PRIMARY -> R.string.primary_color_key
            ColorType.ACCENT -> R.string.accent_color_key
            ColorType.ITEM -> R.string.toolbar_item_color_key
        }
        val defaultRef = when (type) {
            ColorType.PRIMARY -> R.color.colorPrimary
            ColorType.ACCENT -> R.color.colorAccent
            ColorType.ITEM -> R.color.white
        }
        return defSharedPreferences.getInt(getString(keyRef), idToColor(defaultRef))
    }

    companion object Constants {
        const val SD_CARD_WRITE_REQUEST = 1
    }

}