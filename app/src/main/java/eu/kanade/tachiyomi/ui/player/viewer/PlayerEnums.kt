package eu.kanade.tachiyomi.ui.player.viewer

import android.os.Build
import androidx.annotation.StringRes
import eu.kanade.tachiyomi.R

/**
 * Results of the set as cover feature.
 */
enum class SetAsCover {
    Success, AddToLibraryFirst, Error
}

/**
 * Player's Picture-In-Picture state handler
 */
enum class PipState {
    OFF, ON, STARTED;

    companion object {
        internal var mode: PipState = OFF
    }
}

/**
 * Player's Seek state handler
 */
enum class SeekState {
    DOUBLE_TAP, LOCKED, NONE, SCROLL, SEEKBAR;

    companion object {
        internal var mode = NONE
    }
}

/**
 * Player's Video Aspect state handler
 */
enum class AspectState(val index: Int, @StringRes val stringRes: Int) {
    CROP(index = 0, stringRes = R.string.video_crop_screen),
    FIT(index = 1, stringRes = R.string.video_fit_screen),
    STRETCH(index = 2, stringRes = R.string.video_stretch_screen),
    ;

    companion object {
        internal var mode: AspectState = FIT

        internal fun get(index: Int) = values().find { index == it.index } ?: FIT
    }
}

/**
 * Player's Hardware Decoder type handler
 */
enum class HwDecType(val title: String, val mpvValue: String) {
    HW_PLUS(title = "HW+", mpvValue = "mediacodec"),
    HW(title = "HW", mpvValue = "mediacodec-copy"),
    SW(title = "SW", mpvValue = "no"),
    ;

    companion object {
        internal val defaultHwDec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) HW_PLUS else HW

        internal fun get(title: String) = when (title) {
            "mediacodec" -> HW_PLUS
            "mediacodec-copy" -> HW
            "no" -> SW
            else -> defaultHwDec
        }
    }
}