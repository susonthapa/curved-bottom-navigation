package np.com.susanthapa.curved_bottom_navigation

import android.content.Context
import android.util.DisplayMetrics
import android.view.View
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat

/**
 * Created by suson on 10/1/20
 */

fun Int.toPx(context: Context) = (this * context.resources.displayMetrics.densityDpi) / DisplayMetrics.DENSITY_DEFAULT

fun Context.getColorRes(@ColorRes colorId: Int) = ContextCompat.getColor(this, colorId)

fun Float.getScaledSize(context: Context): Float {
    val metrics = context.resources.displayMetrics
    val screenWidthDp = metrics.widthPixels / metrics.density
    val scaleFactor = screenWidthDp / 360f
    return this * scaleFactor
}
