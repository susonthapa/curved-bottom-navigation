package np.com.susanthapa.curvedbottomnavigation

import android.animation.*
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import kotlin.math.abs

/**
 * Created by suson on 9/28/20
 */

class CurvedBottomNavigationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    private val TAG = "CurvedBottomNavigation"
    private val PROPERTY_OFFSET = "OFFSET"
    private val PROPERTY_CENTERY = "CENTER_Y"
    private val PROPERTY_FADE_IN = "FADE_IN"
    private val PROPERTY_FADE_OUT = "FADE_OUT"

    // first bezier curve
    private val firstCurveStart = PointF()
    private val firstCurveEnd = PointF()
    private val firstCurveControlPoint1 = PointF()
    private val firstCurveControlPoint2 = PointF()

    // second bezier curve
    private val secondCurveStart = PointF()
    private val secondCurveEnd = PointF()
    private val secondCurveControlPoint1 = PointF()
    private val secondCurveControlPoint2 = PointF()

    private val path: Path = Path()
    private val paint: Paint = Paint().apply {
        style = Paint.Style.FILL_AND_STROKE
        color = Color.WHITE
    }

    private lateinit var menuItems: Array<Int>
    private lateinit var menuImageViews: Array<ImageView>
    private lateinit var menuIcons: Array<Bitmap>
    private var menuWidth: Int = 0
    private var offsetX: Int = 0
    private var selectedItem: Int = -1
    private var prevSelectedItem: Int = -1
    private val indicatorSize = resources.getDimensionPixelSize(R.dimen.fab_size)
    private var centerY = indicatorSize / 2f
    private val bottomNavOffsetY =
        resources.getDimensionPixelSize(R.dimen.bottom_nav_layout_height) - resources.getDimensionPixelSize(
            R.dimen.bottom_nav_height
        )

    private val FAB_RADIUS = resources.getDimension(R.dimen.fab_size) / 2
    private val TOP_CONTROL_X = FAB_RADIUS + FAB_RADIUS / 2
    private val TOP_CONTROL_Y = bottomNavOffsetY + FAB_RADIUS / 6
    private val BOTTOM_CONTROL_X = FAB_RADIUS + (FAB_RADIUS / 2)
    private val BOTTOM_CONTROL_Y = FAB_RADIUS / 4
    private val CURVE_OFFSET = FAB_RADIUS * 2 + (FAB_RADIUS / 6)
    private val fabColor: Int

    // listener for the menuItemClick
    var menuClickListener: ((Int, Int, Int) -> Unit)? = null

    init {
        setBackgroundColor(Color.TRANSPARENT)
        val typedValue = TypedValue()
        context.theme.resolveAttribute(R.attr.colorAccent, typedValue, true)
        fabColor = ContextCompat.getColor(context, typedValue.resourceId)
    }

    fun setMenuItems(menuItems: Array<Int>) {
        this.menuItems = menuItems
        menuImageViews = Array(menuItems.size) {
            ImageView(context)
        }
        menuIcons = Array(menuItems.size) {
            ResourcesCompat.getDrawable(resources, menuItems[it], context.theme)!!.toBitmap()
        }
        initializeBottomItems(menuItems)
    }

    fun getMenuItems(): Array<Int> {
        return menuItems
    }

    private fun initializeBottomItems(menuItems: Array<Int>, activeItem: Int = 0) {
        selectedItem = activeItem
        prevSelectedItem = selectedItem
        // clear layout
        removeAllViews()
        val bottomNavLayout = LinearLayout(context)
        // get the ripple from the theme
        val typedValue = TypedValue()
        context.theme.resolveAttribute(R.attr.selectableItemBackground, typedValue, true)
        menuItems.forEachIndexed { index, item ->
            val menuItem = menuImageViews[index]
            menuItem.setImageBitmap(menuIcons[selectedItem])
            menuItem.setImageResource(item)
            menuItem.scaleType = ImageView.ScaleType.CENTER
            menuItem.setOnClickListener {
                prevSelectedItem = selectedItem
                onMenuItemClick(index)
            }
            if (index == activeItem) {
                // render the icon in fab instead of image view, but still allocate the space
                menuItem.visibility = View.INVISIBLE
            }
            val layoutParams = LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT)
            layoutParams.weight = 1f
            bottomNavLayout.addView(menuItem, layoutParams)
        }
        val bottomNavLayoutParams = LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            resources.getDimension(R.dimen.bottom_nav_height).toInt(),
            Gravity.BOTTOM
        )
        addView(bottomNavLayout, bottomNavLayoutParams)
    }

    private fun onMenuItemClick(index: Int) {
        // make all item except current item invisible
        menuImageViews.forEachIndexed { i, imageView ->
            if (prevSelectedItem == i) {
                // show the previous selected view with alpha 0
                imageView.visibility = VISIBLE
                imageView.alpha = 0f
            }
        }
        val newOffsetX = menuWidth * index
        animateItemSelection(newOffsetX, menuWidth, index)
    }

    private fun animateItemSelection(offset: Int, width: Int, index: Int) {
        val propertyOffset = PropertyValuesHolder.ofInt(PROPERTY_OFFSET, offsetX, offset)
        val propertyFadeIn = PropertyValuesHolder.ofFloat(PROPERTY_FADE_IN, 0f, 1f)
        val propertyFadeOut = PropertyValuesHolder.ofFloat(PROPERTY_FADE_OUT, 1f, 0f)
        // watch the direction and compute the diff
        val isLTR = (selectedItem - index) < 0
        val diff = abs(selectedItem - index)
        // compute the animation time dynamically based on the distance between clicks
//        val animDuration = intermediateCount * 100L
        val slideAnimDuration = 350L
        val iconAnimSlot = slideAnimDuration / diff
        val fabAnimDuration = 75L
        // compute the time it will take to move from start to bottom of the curve
        val curveBottomOffset = ((firstCurveEnd.x - firstCurveStart.x) * slideAnimDuration) / width - iconAnimSlot
        Log.d(TAG, "diff: $diff, animDuration: $slideAnimDuration, ,slot: $iconAnimSlot, curveBottomOffset: $curveBottomOffset")
        val offsetAnimator = ValueAnimator().apply {
            setValues(propertyOffset, propertyFadeIn, propertyFadeOut)
            startDelay = fabAnimDuration
            duration = slideAnimDuration
            addUpdateListener { animator ->
                val newOffset = animator.getAnimatedValue(PROPERTY_OFFSET) as Int
                val fadeIn = animator.getAnimatedValue(PROPERTY_FADE_IN) as Float
                val fadeOut = animator.getAnimatedValue(PROPERTY_FADE_OUT) as Float
                // the curve will animate no matter what
                computeCurve(newOffset, width)
                invalidate()
                // delay the animation by taking the bottom of the curve as reference
                val currentTime = animator.animatedFraction * slideAnimDuration
                Log.d(TAG, "fraction: $animatedFraction, computedValue: ${(animator.animatedFraction) * slideAnimDuration + (iconAnimSlot)}")
                // compute the index above which this curve is moving
                var overIconIndex = ((currentTime + (iconAnimSlot)) / iconAnimSlot).toInt()
                val overIconAlpha = if (overIconIndex < 2) {
                    currentTime / (iconAnimSlot * overIconIndex)
                } else {
                    // clamp the values between 0 and 1
                    (currentTime - (iconAnimSlot * (overIconIndex - 1))) / iconAnimSlot
                }
                if (isLTR) {
                    overIconIndex += prevSelectedItem
                    // prevent animatedFraction to be 0
                    if (overIconIndex > index) {
                        Log.w(TAG, "overIconIndex: $overIconIndex overflow, skipping")
                        return@addUpdateListener
                    }
                } else {
                    // recompute the index when we move from right to left
                    overIconIndex = prevSelectedItem - overIconIndex
                    // prevent animatedFraction to be 0
                    if (overIconIndex < index) {
                        Log.w(TAG, "overIconIndex: $overIconIndex underflow, skipping")
                        return@addUpdateListener
                    }
                }
                // animate the leaving view when the curve moves over the half of the adjacent view
                if (abs((overIconIndex - prevSelectedItem)) == 1) {
                    // divide the animate time in half
                    menuImageViews[prevSelectedItem].alpha = overIconAlpha * 2
                }
                Log.d(TAG, "isLTR: $isLTR, currentTime: $currentTime, iconOverIndex: $overIconIndex, iconOverAlpha: $overIconAlpha")
                // animate the alpha of the current item below the curve in it's range
                if (overIconIndex == index) {
                    // animate the target view alpha from 1 to 0
                    menuImageViews[overIconIndex].alpha = 1 - overIconAlpha
                } else {
                    menuImageViews[overIconIndex].alpha = overIconAlpha
                }
            }
        }
        val propertyCenterY = PropertyValuesHolder.ofFloat(PROPERTY_CENTERY, centerY, centerY * 2.5f)
        val centerYAnimatorHide = ValueAnimator().apply {
            setValues(propertyCenterY)
            duration = fabAnimDuration
            addUpdateListener { animator ->
                val newCenterY = animator.getAnimatedValue(PROPERTY_CENTERY) as Float
                centerY = newCenterY
                invalidate()
            }
            addListener(object: AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    // only update the index after this hidden animation is complete
                    selectedItem = index
                }
            })
        }

        val propertyCenterYReverse = PropertyValuesHolder.ofFloat(PROPERTY_CENTERY, centerY * 2.5f, centerY)
        val centerYAnimatorShow = ValueAnimator().apply {
            setValues(propertyCenterYReverse)
            startDelay = (slideAnimDuration)
            duration = fabAnimDuration
            addListener(object: AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    // disable the clicks in the target view
                    menuImageViews[index].visibility = INVISIBLE
                }
            })
            addUpdateListener { animator ->
                val newCenterY = animator.getAnimatedValue(PROPERTY_CENTERY) as Float
                centerY = newCenterY
                invalidate()
            }
        }

        val set = AnimatorSet()
        set.playTogether(centerYAnimatorHide, offsetAnimator, centerYAnimatorShow)
        set.interpolator = AccelerateDecelerateInterpolator()
        set.start()
    }


    private fun computeCurve(offsetX: Int, w: Int) {
        this.offsetX = offsetX
        // first curve
        firstCurveStart.apply {
            // you can change the 3rd param to control the spacing between curve start and FAB start
            x = offsetX + (w / 2) - CURVE_OFFSET
            y = bottomNavOffsetY.toFloat()
        }
        firstCurveEnd.apply {
            x = offsetX + (w / 2f)
            y = bottomNavOffsetY + FAB_RADIUS + (FAB_RADIUS / 4)
        }
        firstCurveControlPoint1.apply {
            x = firstCurveStart.x + TOP_CONTROL_X
            y = TOP_CONTROL_Y
        }
        firstCurveControlPoint2.apply {
            x = firstCurveEnd.x - BOTTOM_CONTROL_X
            y = firstCurveEnd.y - BOTTOM_CONTROL_Y
        }
        Log.d(TAG, "first_start: (${firstCurveStart.x}, ${firstCurveStart.y})")
        Log.d(TAG, "first_c1: (${firstCurveControlPoint1.x}, ${firstCurveControlPoint1.y})")
        Log.d(TAG, "first_c2: (${firstCurveControlPoint2.x}, ${firstCurveControlPoint2.y})")
        Log.d(TAG, "first_end: (${firstCurveEnd.x}, ${firstCurveEnd.y})")

        // second curve
        secondCurveStart.set(firstCurveEnd.x, firstCurveEnd.y)
        secondCurveEnd.apply {
            x = offsetX + (w / 2) + CURVE_OFFSET
            y = bottomNavOffsetY.toFloat()
        }
        secondCurveControlPoint1.apply {
            x = secondCurveStart.x + BOTTOM_CONTROL_X
            y = secondCurveStart.y - BOTTOM_CONTROL_Y
        }
        secondCurveControlPoint2.apply {
            x = secondCurveEnd.x - TOP_CONTROL_X
            y = TOP_CONTROL_Y
        }
        Log.d(TAG, "second_start: (${secondCurveStart.x}, ${secondCurveStart.y})")
        Log.d(TAG, "second_c1: (${secondCurveControlPoint1.x}, ${secondCurveControlPoint1.y})")
        Log.d(TAG, "second_c2: (${secondCurveControlPoint2.x}, ${secondCurveControlPoint2.y})")
        Log.d(TAG, "second_end: (${secondCurveEnd.x}, ${secondCurveEnd.y})")

        // generate the path
        path.reset()
        path.moveTo(0f, bottomNavOffsetY.toFloat())
        // horizontal line from left to the start of first curve
        path.lineTo(firstCurveStart.x, firstCurveStart.y)
        // add the first curve
        path.cubicTo(
            firstCurveControlPoint1.x,
            firstCurveControlPoint1.y,
            firstCurveControlPoint2.x,
            firstCurveControlPoint2.y,
            firstCurveEnd.x,
            firstCurveEnd.y
        )
        // add the second curve
        path.cubicTo(
            secondCurveControlPoint1.x,
            secondCurveControlPoint1.y,
            secondCurveControlPoint2.x,
            secondCurveControlPoint2.y,
            secondCurveEnd.x,
            secondCurveEnd.y
        )
        // continue to draw the remaining portion of the bottom navigation
        path.lineTo(width.toFloat(), bottomNavOffsetY.toFloat())
        path.lineTo(width.toFloat(), height.toFloat())
        path.lineTo(0f, height.toFloat())
        path.close()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        Log.d(TAG, "size: ($w, $h), center: (${w / 2}, ${h / 2})")
        // by default make the center item active
        menuWidth = w / menuItems.size
        computeCurve(0, menuWidth)
        // let the listener know about the initial item selection
//        menuClickListener?.invoke(menuWidth * selectedItem, menuWidth, selectedItem)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val centerX = menuWidth * selectedItem + menuWidth / 2f
        paint.color = fabColor
        canvas.drawCircle(centerX, centerY, indicatorSize / 2f, paint)
        paint.color = Color.BLACK
        canvas.drawBitmap(
            menuIcons[selectedItem],
            (centerX - menuIcons[selectedItem].width / 2f),
            (centerY - menuIcons[selectedItem].height / 2f),
            paint
        )
        paint.color = Color.WHITE
        canvas.drawPath(path, paint)
    }

}