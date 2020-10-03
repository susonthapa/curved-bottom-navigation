package np.com.susanthapa.curved_bottom_navigation

import android.animation.*
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.vectordrawable.graphics.drawable.AnimatedVectorDrawableCompat
import kotlin.math.abs

/**
 * Created by suson on 9/28/20
 */

class CurvedBottomNavigationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : FrameLayout(context, attrs, defStyleAttr) {

    companion object {
        private const val TAG = "CurvedBottomNavigation"
        private const val PROPERTY_OFFSET = "OFFSET"
        private const val PROPERTY_CENTER_Y = "CENTER_Y"
        private const val PROPERTY_CENTER_X = "CENTER_X"
    }


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


    // icon colors
    private val unSelectedIconTint = ContextCompat.getColor(context, R.color.color_BDBDBD)
    private val activeIconTint = ContextCompat.getColor(context, R.color.color_000000)
    private val activeColorFilter = PorterDuffColorFilter(activeIconTint, PorterDuff.Mode.SRC_IN)

    // paint and paths
    private val path: Path = Path()
    private val bezierPaint: Paint = Paint().apply {
        style = Paint.Style.FILL_AND_STROKE
        color = Color.WHITE
        setShadowLayer(
            8.toPx(context).toFloat(),
            0f,
            6f,
            context.getColorRes(R.color.color_75000000)
        )
    }
    private val fabPaint: Paint = Paint().apply {
        style = Paint.Style.FILL_AND_STROKE
        color = Color.WHITE
        setShadowLayer(
            6.toPx(context).toFloat(),
            0f,
            6f,
            context.getColorRes(R.color.color_75000000)
        )
    }
    private val iconPaint: Paint = Paint().apply {
        style = Paint.Style.FILL_AND_STROKE
        color = Color.BLACK
        colorFilter = activeColorFilter
    }

    private lateinit var menuItems: Array<MenuItem>
    private lateinit var bottomNavItemViews: Array<BottomNavItemView>
    private lateinit var menuIcons: Array<Bitmap>
    private lateinit var menuAvds: Array<AnimatedVectorDrawableCompat>
    private var menuWidth: Int = 0
    private var offsetX: Int = 0
    private var selectedItem: Int = -1
    private var prevSelectedItem: Int = -1
    private val indicatorSize = resources.getDimensionPixelSize(R.dimen.fab_size)
    private val bottomNavOffsetY =
        resources.getDimensionPixelSize(R.dimen.bottom_nav_layout_height) - resources.getDimensionPixelSize(
            R.dimen.bottom_nav_height
        )

    private val fabMargin = resources.getDimensionPixelSize(R.dimen.bottom_nav_fab_margin)
    private val fabRadius = resources.getDimension(R.dimen.fab_size) / 2
    private val topControlX = fabRadius + fabRadius / 2
    private val topControlY = bottomNavOffsetY + fabRadius / 6
    private val bottomControlX = fabRadius + (fabRadius / 2)
    private val bottomControlY = fabRadius / 4
    private val curveWidth = fabRadius * 2 + fabMargin
    private val curveBottomOffset =
        resources.getDimensionPixelSize(R.dimen.bottom_nav_bottom_curve_offset)
    private val centerY = indicatorSize / 2f + fabMargin
    private var centerX = -1f
    private var curCenterY = centerY
    private val fabColor: Int

    // listener for the menuItemClick
    var menuClickListener: ((Int, Int, Int) -> Unit)? = null

    init {
        setBackgroundColor(Color.TRANSPARENT)
        val typedValue = TypedValue()
        context.theme.resolveAttribute(R.attr.colorAccent, typedValue, true)
        fabColor = Color.WHITE
//        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    fun setMenuItems(menuItems: Array<MenuItem>) {
        this.menuItems = menuItems
        bottomNavItemViews = Array(menuItems.size) {
            BottomNavItemView(context)
        }
        menuIcons = Array(menuItems.size) {
            val drawable =
                ResourcesCompat.getDrawable(resources, menuItems[it].icon, context.theme)!!
            drawable.setTint(unSelectedIconTint)
            drawable.toBitmap()
        }
        menuAvds = Array(menuItems.size) {
            AnimatedVectorDrawableCompat.create(context, menuItems[it].avdIcon)!!
        }
        initializeBottomItems(menuItems)
    }

    fun getMenuItems(): Array<MenuItem> {
        return menuItems
    }

    private fun initializeBottomItems(menuItems: Array<MenuItem>, activeItem: Int = 0) {
        selectedItem = activeItem
        prevSelectedItem = selectedItem
        // clear layout
        removeAllViews()
        val bottomNavLayout = LinearLayout(context)
        // get the ripple from the theme
        val typedValue = TypedValue()
        context.theme.resolveAttribute(R.attr.selectableItemBackground, typedValue, true)
        menuItems.forEachIndexed { index, item ->
            val menuItem = bottomNavItemViews[index]
            menuItem.setMenuItem(item)
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
        bottomNavItemViews.forEachIndexed { i, imageView ->
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
        val finalCenterX = menuWidth * index + (menuWidth / 2f)
        val propertyOffset = PropertyValuesHolder.ofInt(PROPERTY_OFFSET, offsetX, offset)
        val propertyCenterX = PropertyValuesHolder.ofFloat(PROPERTY_CENTER_X, centerX, finalCenterX)
        // watch the direction and compute the diff
        val isLTR = (selectedItem - index) < 0
        val diff = abs(selectedItem - index)
        if (diff == 0) {
            Log.w(TAG, "same icon multiple clicked, skipping animation!")
            return
        }
        // compute the animation time dynamically based on the distance between clicks
        val slideAnimDuration = 300L
        val iconAnimSlot = slideAnimDuration / diff
        // compute the time it will take to move from start to bottom of the curve
        val curveBottomOffset = (((curveWidth / 2) * slideAnimDuration) / this.width).toLong()
        Log.d(TAG, "CURVE_OFFSET: $curveWidth, width: ${this.width}")
        Log.d(
            TAG,
            "diff: $diff, animDuration: $slideAnimDuration, ,slot: $iconAnimSlot, curveBottomOffset: $curveBottomOffset"
        )
        val offsetAnimator = getBezierCurveAnimation(
            slideAnimDuration,
            width,
            iconAnimSlot,
            isLTR,
            index,
            curveBottomOffset,
            diff,
            propertyOffset,
            propertyCenterX
        )
        val fabYOffset = firstCurveEnd.y + fabRadius
        val hideTimeInterval = slideAnimDuration / 2
        val centerYAnimatorHide = hideFAB(fabYOffset, index)
        centerYAnimatorHide.duration = hideTimeInterval
        val centerYAnimatorShow = showFAB(fabYOffset, index)
        centerYAnimatorShow.startDelay = slideAnimDuration / 2
        centerYAnimatorShow.duration = slideAnimDuration / 2
        menuAvds[index].start()

        val set = AnimatorSet()
        set.playTogether(centerYAnimatorHide, offsetAnimator, centerYAnimatorShow)
        set.interpolator = FastOutSlowInInterpolator()
        set.start()
    }

    private fun getBezierCurveAnimation(
        slideAnimDuration: Long,
        width: Int,
        iconAnimSlot: Long,
        isLTR: Boolean,
        index: Int,
        curveBottomOffset: Long,
        diff: Int,
        vararg propertyOffset: PropertyValuesHolder,
    ): ValueAnimator {
        return ValueAnimator().apply {
            setValues(*propertyOffset)
            duration = slideAnimDuration
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    // reset all the status of icon animations
                    bottomNavItemViews.forEach {
                        it.resetAnimation()
                    }
                }

                override fun onAnimationCancel(animation: Animator) {
                    // reset all the status of icon animations
                    bottomNavItemViews.forEach {
                        it.resetAnimation()
                    }
                }
            })
            addUpdateListener { animator ->
                val newOffset = getAnimatedValue(PROPERTY_OFFSET) as Int
                // change the centerX of the FAB
                centerX = getAnimatedValue(PROPERTY_CENTER_X) as Float
                // the curve will animate no matter what
                computeCurve(newOffset, width)
                invalidate()
                // delay the animation by taking the bottom of the curve as reference
                val currentTime = animator.animatedFraction * slideAnimDuration
                Log.d(TAG, "centerX: $centerX")
                // compute the index above which this curve is moving
                var overIconIndex = ((currentTime + (iconAnimSlot)) / iconAnimSlot).toInt()
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
                when {
                    overIconIndex == index -> {
                        // animate the destination icon within the time the curve reaches it's boundary
                        bottomNavItemViews[index].startDestinationAnimation(curveBottomOffset)
                        if (diff == 1) {
                            // also animate the source icon as this is the adjacent click event
                            bottomNavItemViews[prevSelectedItem].startSourceAnimation(
                                slideAnimDuration
                            )
                        }
                    }
                    abs(overIconIndex - prevSelectedItem) == 1 -> {
                        // we currently in the adjacent icon of the current source icon, show source animations
                        bottomNavItemViews[prevSelectedItem].startSourceAnimation(slideAnimDuration)
                        // also initialize the intermediate animations
                        bottomNavItemViews[overIconIndex].startIntermediateAnimation(
                            slideAnimDuration,
                            curveBottomOffset
                        )
                    }
                    else -> {
                        // we over intermediate icons, show the intermediate icons
                        bottomNavItemViews[overIconIndex].startIntermediateAnimation(
                            slideAnimDuration,
                            curveBottomOffset
                        )
                    }
                }
                Log.d(
                    TAG,
                    "isLTR: $isLTR, currentTime: $currentTime, iconOverIndex: $overIconIndex"
                )
            }
        }
    }

    private fun showFAB(
        fabYOffset: Float,
        index: Int
    ): ValueAnimator {
        val propertyCenterYReverse =
            PropertyValuesHolder.ofFloat(PROPERTY_CENTER_Y, fabYOffset, centerY)
        return ValueAnimator().apply {
            setValues(propertyCenterYReverse)
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    // disable the clicks in the target view
                    bottomNavItemViews[index].visibility = INVISIBLE
                }
            })
            addUpdateListener { animator ->
                val newCenterY = animator.getAnimatedValue(PROPERTY_CENTER_Y) as Float
                curCenterY = newCenterY
                invalidate()
            }
        }
    }

    private fun hideFAB(
        fabYOffset: Float,
        index: Int
    ): ValueAnimator {
        val propertyCenterY =
            PropertyValuesHolder.ofFloat(PROPERTY_CENTER_Y, centerY, fabYOffset)
        return ValueAnimator().apply {
            setValues(propertyCenterY)
            addUpdateListener { animator ->
                val newCenterY = animator.getAnimatedValue(PROPERTY_CENTER_Y) as Float
                curCenterY = newCenterY
                invalidate()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    // only update the index after this hidden animation is complete
                    selectedItem = index
                }
            })
        }
    }


    private fun computeCurve(offsetX: Int, w: Int) {
        this.offsetX = offsetX
        // first curve
        firstCurveStart.apply {
            // you can change the 3rd param to control the spacing between curve start and FAB start
            x = offsetX + (w / 2) - curveWidth
            y = bottomNavOffsetY.toFloat()
        }
        firstCurveEnd.apply {
            x = offsetX + (w / 2f)
            y = bottomNavOffsetY + fabRadius + curveBottomOffset
        }
        firstCurveControlPoint1.apply {
            x = firstCurveStart.x + topControlX
            y = topControlY
        }
        firstCurveControlPoint2.apply {
            x = firstCurveEnd.x - bottomControlX
            y = firstCurveEnd.y - bottomControlY
        }
        Log.d(TAG, "first_start: (${firstCurveStart.x}, ${firstCurveStart.y})")
        Log.d(TAG, "first_c1: (${firstCurveControlPoint1.x}, ${firstCurveControlPoint1.y})")
        Log.d(TAG, "first_c2: (${firstCurveControlPoint2.x}, ${firstCurveControlPoint2.y})")
        Log.d(TAG, "first_end: (${firstCurveEnd.x}, ${firstCurveEnd.y})")

        // second curve
        secondCurveStart.set(firstCurveEnd.x, firstCurveEnd.y)
        secondCurveEnd.apply {
            x = offsetX + (w / 2) + curveWidth
            y = bottomNavOffsetY.toFloat()
        }
        secondCurveControlPoint1.apply {
            x = secondCurveStart.x + bottomControlX
            y = secondCurveStart.y - bottomControlY
        }
        secondCurveControlPoint2.apply {
            x = secondCurveEnd.x - topControlX
            y = topControlY
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
        // by default make the center item active
        menuWidth = w / menuItems.size
        Log.d(TAG, "size: ($w, $h), center: (${w / 2}, ${h / 2}), menuWidth: $menuWidth")
        centerX = menuWidth * selectedItem + menuWidth / 2f
        computeCurve(0, menuWidth)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawCircle(centerX, curCenterY, indicatorSize / 2f, fabPaint)
        menuAvds[selectedItem].setBounds(
            (centerX - menuIcons[selectedItem].width / 2).toInt(),
            (curCenterY - menuIcons[selectedItem].height / 2).toInt(),
            (centerX + menuIcons[selectedItem].width / 2).toInt(),
            (curCenterY + menuIcons[selectedItem].height / 2).toInt()
        )
        menuAvds[selectedItem].draw(canvas)
//        canvas.drawBitmap(
//            menuIcons[selectedItem],
//            (centerX - menuIcons[selectedItem].width / 2f),
//            (curCenterY - menuIcons[selectedItem].height / 2f),
//            iconPaint
//        )
        canvas.drawPath(path, bezierPaint)
    }

}