package np.com.susanthapa.curved_bottom_navigation

import android.animation.*
import android.content.Context
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Log
import android.view.animation.DecelerateInterpolator
import androidx.appcompat.widget.AppCompatImageView
import androidx.interpolator.view.animation.FastOutSlowInInterpolator

/**
 * Created by suson on 10/1/20
 */

class BottomNavItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : AppCompatImageView(context, attrs, defStyleAttr) {

    companion object {
        const val TAG = "BottomNavItemView"
    }

    // the animation progress as the caller might call multiple times even when we
    // might have animation running
    private var isAnimating = false


    fun setMenuIcon(icon: Drawable) {
        setImageDrawable(icon)
        scaleType = ScaleType.CENTER
    }

    fun resetAnimation() {
        isAnimating = false
    }

    fun startIntermediateAnimation(time: Long, offset: Long) {
        // check for already running animations for the icon
        if (isAnimating) {
            return
        }
        // hide the icon within the time the curve reaches the start of this icon slot
        val hideAnimation = getIconHideAnimation(offset)
        // animate only when the curve reaches the end of this icon slot
        val showDuration = time - 2 * offset
        if (showDuration < 0) {
            Log.w(TAG, "show animation duration < 0, try increasing iconSlotAnimation")
            return
        }
        val showAnimation = getIconShowAnimation(time - 2 * offset)
        showAnimation.startDelay = offset
        val set = AnimatorSet()
        set.playSequentially(hideAnimation, showAnimation)
        set.interpolator = FastOutSlowInInterpolator()
        set.start()
    }

    fun startSourceAnimation(time: Long) {
        // check for already running animations for the icon
        if (isAnimating) {
            return
        }
        // show the icon
        val showAnimation = getIconShowAnimation(time)
        showAnimation.interpolator = DecelerateInterpolator()
        showAnimation.start()
    }

    fun startDestinationAnimation(time: Long) {
        // check for already running animations for the icon
        if (isAnimating) {
            return
        }
        // hide the icon
        val hideAnimation = getIconHideAnimation(time)
        hideAnimation.interpolator = DecelerateInterpolator()
        hideAnimation.start()
    }

    private fun getIconHideAnimation(time: Long): ValueAnimator {
        return ObjectAnimator.ofFloat(this, "alpha", 1f, 0f)
            .apply {
                duration = time
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationStart(animation: Animator?) {
                        isAnimating = true
                    }
                })
            }
    }

    private fun getIconShowAnimation(time: Long): ValueAnimator {
        val translateYProperty =
            PropertyValuesHolder.ofFloat("translationY", height * 0.2f, 0f)
        val alphaProperty = PropertyValuesHolder.ofFloat("alpha", 0f, 1f)
        return ObjectAnimator.ofPropertyValuesHolder(
            this,
            alphaProperty,
            translateYProperty
        )
            .apply {
                duration = time
                addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationStart(animation: Animator?) {
                        isAnimating = true
                    }
                })
            }
    }

}