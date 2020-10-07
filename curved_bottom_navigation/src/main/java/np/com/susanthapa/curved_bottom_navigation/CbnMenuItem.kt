package np.com.susanthapa.curved_bottom_navigation

import androidx.annotation.DrawableRes
import androidx.annotation.IdRes
import androidx.annotation.NavigationRes

/**
 * Created by suson on 10/1/20
 */
data class CbnMenuItem(
    @DrawableRes
    val icon: Int,
    @DrawableRes
    val avdIcon: Int,
    @IdRes
    val destinationId: Int = -1
)