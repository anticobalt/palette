package iced.egret.palette.activity.inherited

import android.os.Bundle
import com.r0adkll.slidr.Slidr
import com.r0adkll.slidr.model.SlidrConfig
import iced.egret.palette.R

/**
 * An activity that is transparent and can finish via sliding.
 * When sliding, previous activity is visible.
 */
abstract class SlideActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setAccentThemeFromSettings()

        val config = SlidrConfig.Builder()
                .edge(true)
                .edgeSize(0.30f)
                .build()
        Slidr.attach(this, config)
    }

    private fun setAccentThemeFromSettings() {
        val resId = when (getColorInt(ColorType.ACCENT)) {
            idToColor(R.color.dodger_blue) -> R.style.AppTheme_SlideActivity_DodgerBlue
            idToColor(R.color.sea_green) -> R.style.AppTheme_SlideActivity_SeaGreen
            idToColor(R.color.fruit_salad) -> R.style.AppTheme_SlideActivity_FruitSalad
            idToColor(R.color.tangerine) -> R.style.AppTheme_SlideActivity_Tangerine
            idToColor(R.color.lynch) -> R.style.AppTheme_SlideActivity_Lynch
            idToColor(R.color.coffee) -> R.style.AppTheme_SlideActivity_Coffee
            idToColor(R.color.idle_pink) -> R.style.AppTheme_SlideActivity_IdlePink
            idToColor(R.color.boppin_blue) -> R.style.AppTheme_SlideActivity_BoppinBlue
            idToColor(R.color.astral_yellow) -> R.style.AppTheme_SlideActivity_AstralYellow
            idToColor(R.color.myriad_magenta) -> R.style.AppTheme_SlideActivity_MyriadMagenta
            idToColor(R.color.resolute_cyan) -> R.style.AppTheme_SlideActivity_ResoluteCyan
            idToColor(R.color.fashion_red) -> R.style.AppTheme_SlideActivity_FashionRed
            else -> R.style.AppTheme_SlideActivity  // fallback, default accent colors
        }
        setTheme(resId)
    }

}