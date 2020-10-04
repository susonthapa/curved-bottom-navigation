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
import androidx.annotation.IdRes
import androidx.core.content.res.ResourcesCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.navigation.NavController
import androidx.navigation.NavDestination
import androidx.navigation.NavGraph
import androidx.navigation.NavOptions
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


    // default values for custom attributes
    var selectedColor = Color.parseColor("#000000")
        set(value) {
            field = value
            initializeMenuAVDs()
            invalidate()
        }
    var unSelectedColor = Color.parseColor("#8F8F8F")
        set(value) {
            field = value
            initializeMenuIcons()
            invalidate()
        }
    private val shadowColor: Int = Color.parseColor("#75000000")

    var animDuration: Long = 300L

    var fabElevation = 4.toPx(context).toFloat()
    var navElevation = 6.toPx(context).toFloat()

    var fabBackgroundColor = Color.WHITE
    var navBackgroundColor = Color.WHITE

    // paint and paths
    private val path: Path = Path()
    private val navPaint: Paint
    private val fabPaint: Paint

    // initialize empty array so that we don't have to check if it's initialized or not
    private var menuItems: Array<MenuItem> = arrayOf()
    private lateinit var bottomNavItemViews: Array<BottomNavItemView>
    private lateinit var menuIcons: Array<Bitmap>
    private lateinit var menuAVDs: Array<AnimatedVectorDrawableCompat>
    private var menuWidth: Int = 0
    private var offsetX: Int = 0
    private var selectedItem: Int = -1
    private var prevSelectedItem: Int = -1
    private val indicatorSize = resources.getDimensionPixelSize(R.dimen.cbn_fab_size)
    private val bottomNavOffsetY =
        resources.getDimensionPixelSize(R.dimen.cbn_layout_height) - resources.getDimensionPixelSize(
            R.dimen.cbn_height
        )

    private val fabMargin = resources.getDimensionPixelSize(R.dimen.cbn_fab_margin)
    private val fabRadius = resources.getDimension(R.dimen.cbn_fab_size) / 2
    private val topControlX = fabRadius + fabRadius / 2
    private val topControlY = bottomNavOffsetY + fabRadius / 6
    private val bottomControlX = fabRadius + (fabRadius / 2)
    private val bottomControlY = fabRadius / 4
    private val curveWidth = fabRadius * 2 + fabMargin
    private val curveBottomOffset =
        resources.getDimensionPixelSize(R.dimen.cbn_bottom_curve_offset)
    private val centerY = indicatorSize / 2f + fabMargin
    private var centerX = -1f
    private var curCenterY = centerY

    private var isAnimating = false

    // listener for the menuItemClick
    private var menuItemClickListener: ((MenuItem, Int) -> Unit)? = null

    init {
        // remove the bg as will do our own drawing
        setBackgroundColor(Color.TRANSPARENT)
        // read the attributes values
        context.theme.obtainStyledAttributes(attrs, R.styleable.CurvedBottomNavigationView, 0, 0)
            .apply {
                try {
                    selectedColor = getColor(
                        R.styleable.CurvedBottomNavigationView_cbn_selectedColor,
                        selectedColor
                    )
                    unSelectedColor = getColor(
                        R.styleable.CurvedBottomNavigationView_cbn_unSelectedColor,
                        unSelectedColor
                    )
                    animDuration = getInteger(
                        R.styleable.CurvedBottomNavigationView_cbn_animDuration,
                        animDuration.toInt()
                    ).toLong()
                    fabBackgroundColor = getColor(
                        R.styleable.CurvedBottomNavigationView_cbn_fabBg,
                        fabBackgroundColor
                    )
                    navBackgroundColor =
                        getColor(R.styleable.CurvedBottomNavigationView_cbn_bg, navBackgroundColor)
                    fabElevation = getDimension(
                        R.styleable.CurvedBottomNavigationView_cbn_fabElevation,
                        fabElevation
                    )
                    navElevation = getDimension(
                        R.styleable.CurvedBottomNavigationView_cbn_elevation,
                        navElevation
                    )
                } finally {
                    recycle()
                }
            }

        navPaint = Paint().apply {
            style = Paint.Style.FILL_AND_STROKE
            color = navBackgroundColor
            setShadowLayer(
                navElevation,
                0f,
                6f,
                shadowColor
            )
        }
        fabPaint = Paint().apply {
            style = Paint.Style.FILL_AND_STROKE
            color = fabBackgroundColor
            setShadowLayer(
                fabElevation,
                0f,
                6f,
                shadowColor
            )
        }
//        setLayerType(LAYER_TYPE_SOFTWARE, null)
    }

    fun setMenuItems(menuItems: Array<MenuItem>, activeIndex: Int = 0) {
        this.menuItems = menuItems
        bottomNavItemViews = Array(menuItems.size) {
            BottomNavItemView(context)
        }
        initializeMenuIcons()
        initializeMenuAVDs()
        initializeBottomItems(menuItems, activeIndex)
    }

    private fun initializeMenuAVDs() {
        val activeColorFilter = PorterDuffColorFilter(selectedColor, PorterDuff.Mode.SRC_IN)
        menuAVDs = Array(menuItems.size) {
            val avd = AnimatedVectorDrawableCompat.create(context, menuItems[it].avdIcon)!!
            avd.colorFilter = activeColorFilter
            avd
        }
    }

    private fun initializeMenuIcons() {
        menuIcons = Array(menuItems.size) {
            val drawable =
                ResourcesCompat.getDrawable(resources, menuItems[it].icon, context.theme)!!
            drawable.setTint(unSelectedColor)
            drawable.toBitmap()
        }
    }

    fun getMenuItems(): Array<MenuItem> {
        return menuItems
    }

    fun setOnMenuItemClickListener(menuItemClickListener: (MenuItem, Int) -> Unit) {
        this.menuItemClickListener = menuItemClickListener
    }

    fun setupWithNavController(navController: NavController) {
        // initialize the menu
        setOnMenuItemClickListener { item, _ ->
            navigateToDestination(navController, item)
        }
        // setup destination change listener to properly sync the back button press
        navController.addOnDestinationChangedListener { controller, destination, arguments ->
            for (i in menuItems.indices) {
                if (matchDestination(destination, menuItems[i].destinationId)) {
                    onMenuItemClick(i)
                }
            }
        }
    }

    private fun navigateToDestination(navController: NavController, item: MenuItem) {
        if (item.destinationId == -1) {
            throw RuntimeException("please set a valid id, unable the navigation!")
        }
        val builder = NavOptions.Builder()
            .setLaunchSingleTop(true)
            .setEnterAnim(R.anim.nav_default_enter_anim)
            .setExitAnim(R.anim.nav_default_exit_anim)
            .setPopEnterAnim(R.anim.nav_default_pop_enter_anim)
            .setPopExitAnim(R.anim.nav_default_pop_exit_anim)
        // pop to the navigation graph's start  destination
        builder.setPopUpTo(findStartDestination(navController.graph).id, false)
        val options = builder.build()
        try {
            navController.navigate(item.destinationId, null, options)
        } catch (e: IllegalArgumentException) {
            Log.w(TAG, "unable to navigate!", e)
        }
    }

    private fun matchDestination(destination: NavDestination, @IdRes destinationId: Int): Boolean {
        var currentDestination = destination
        while (currentDestination.id != destinationId && currentDestination.parent != null) {
            currentDestination = currentDestination.parent!!
        }

        return currentDestination.id == destinationId
    }

    private fun findStartDestination(graph: NavGraph): NavDestination {
        var startDestination: NavDestination = graph
        while (startDestination is NavGraph) {
            startDestination = graph.findNode(graph.startDestination)!!
        }

        return startDestination
    }

    private fun initializeBottomItems(menuItems: Array<MenuItem>, activeItem: Int) {
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
            resources.getDimension(R.dimen.cbn_height).toInt(),
            Gravity.BOTTOM
        )
        addView(bottomNavLayout, bottomNavLayoutParams)
    }

    private fun onMenuItemClick(index: Int) {
        if (selectedItem == index) {
            Log.i(TAG, "same icon multiple clicked, skipping animation!")
            return
        }
        if (isAnimating) {
            Log.i(TAG, "animation is in progress, skipping navigation")
            return
        }
        prevSelectedItem = selectedItem
        selectedItem = index
        // make all item except current item invisible
        bottomNavItemViews.forEachIndexed { i, imageView ->
            if (prevSelectedItem == i) {
                // show the previous selected view with alpha 0
                imageView.visibility = VISIBLE
                imageView.alpha = 0f
            }
        }
        val newOffsetX = menuWidth * index
        isAnimating = true
        animateItemSelection(newOffsetX, menuWidth, index)
        // notify the listener
        menuItemClickListener?.invoke(menuItems[index], index)
    }

    private fun animateItemSelection(offset: Int, width: Int, index: Int) {
        val finalCenterX = menuWidth * index + (menuWidth / 2f)
        val propertyOffset = PropertyValuesHolder.ofInt(PROPERTY_OFFSET, offsetX, offset)
        val propertyCenterX = PropertyValuesHolder.ofFloat(PROPERTY_CENTER_X, centerX, finalCenterX)
        // watch the direction and compute the diff
        val isLTR = (prevSelectedItem - index) < 0
        val diff = abs(prevSelectedItem - index)
        // time allocated for each icon in the bottom nav
        val iconAnimSlot = animDuration / diff
        // compute the time it will take to move from start to bottom of the curve
        val curveBottomOffset = (((curveWidth / 2) * animDuration) / this.width).toLong()
        Log.d(TAG, "CURVE_OFFSET: $curveWidth, width: ${this.width}")
        Log.d(
            TAG,
            "diff: $diff, animDuration: $animDuration, ,slot: $iconAnimSlot, curveBottomOffset: $curveBottomOffset"
        )
        val offsetAnimator = getBezierCurveAnimation(
            animDuration,
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
        val hideTimeInterval = animDuration / 2
        val centerYAnimatorHide = hideFAB(fabYOffset)
        centerYAnimatorHide.duration = hideTimeInterval
        val centerYAnimatorShow = showFAB(fabYOffset, index)
        centerYAnimatorShow.startDelay = animDuration / 2
        centerYAnimatorShow.duration = animDuration / 2
        menuAVDs[index].start()

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
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator?) {
                    isAnimating = false
                }
            })
        }
    }

    private fun hideFAB(fabYOffset: Float): ValueAnimator {
        val propertyCenterY =
            PropertyValuesHolder.ofFloat(PROPERTY_CENTER_Y, centerY, fabYOffset)
        return ValueAnimator().apply {
            setValues(propertyCenterY)
            addUpdateListener { animator ->
                val newCenterY = animator.getAnimatedValue(PROPERTY_CENTER_Y) as Float
                curCenterY = newCenterY
                invalidate()
            }
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
        menuAVDs[selectedItem].setBounds(
            (centerX - menuIcons[selectedItem].width / 2).toInt(),
            (curCenterY - menuIcons[selectedItem].height / 2).toInt(),
            (centerX + menuIcons[selectedItem].width / 2).toInt(),
            (curCenterY + menuIcons[selectedItem].height / 2).toInt()
        )
        menuAVDs[selectedItem].draw(canvas)
        canvas.drawPath(path, navPaint)
    }

}