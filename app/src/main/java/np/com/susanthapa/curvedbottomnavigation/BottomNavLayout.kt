package np.com.susanthapa.curvedbottomnavigation

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import np.com.susanthapa.curvedbottomnavigation.databinding.BottomNavLayoutBinding

/**
 * Created by suson on 9/28/20
 */

class BottomNavLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val binding = BottomNavLayoutBinding.inflate(LayoutInflater.from(context), this)

    val bottomNavigation: CurvedBottomNavigationView
        get() = binding.curvedBottomNav

    init {
        bottomNavigation.menuClickListener = {offset, width, index ->
            val left = offset + (width - binding.curvedFab.width) / 2
            val newLayoutParams = (binding.curvedFab.layoutParams as LayoutParams).apply {
                marginStart = left
            }
            binding.curvedFab.layoutParams = newLayoutParams
//            binding.curvedFab.setImageResource(bottomNavigation.getMenuItems()[index])
        }
    }

}