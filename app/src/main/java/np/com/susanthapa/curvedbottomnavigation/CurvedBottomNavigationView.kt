package np.com.susanthapa.curvedbottomnavigation

import android.content.Context
import android.content.res.Resources
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.core.content.res.ResourcesCompat
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomnavigation.LabelVisibilityMode

/**
 * Created by suson on 9/28/20
 */

class CurvedBottomNavigationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val FAB_RADIUS = resources.getDimension(R.dimen.fab_size) / 2
    private val TOP_CONTROL_X = FAB_RADIUS + FAB_RADIUS / 2
    private val TOP_CONTROL_Y = FAB_RADIUS / 6
    private val BOTTOM_CONTROL_X = FAB_RADIUS + (FAB_RADIUS / 2)
    private val BOTTOM_CONTROL_Y = FAB_RADIUS / 4
    private val CURVE_OFFSET = FAB_RADIUS * 2 + (FAB_RADIUS / 6)
    private val TAG = "CurvedBottomNavigation"

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
    private var menuWidth: Int = 0
    private var selectedItem: Int = -1

    // listener for the menuItemClick
    var menuClickListener: ((Int, Int, Int) -> Unit)? = null

    init {
        setBackgroundColor(Color.TRANSPARENT)
    }

    fun setMenuItems(menuItems: Array<Int>) {
        this.menuItems = menuItems
        menuImageViews = Array(menuItems.size) {
            ImageView(context)
        }
        initializeBottomItems(menuItems)
    }

    fun getMenuItems(): Array<Int> {
        return menuItems
    }

    private fun initializeBottomItems(menuItems: Array<Int>, activeItem: Int = 1) {
        selectedItem = activeItem
        // clear layout
        removeAllViews()
        // get the ripple from the theme
        val typedValue = TypedValue()
        context.theme.resolveAttribute(R.attr.selectableItemBackground, typedValue, true)
        menuItems.forEachIndexed {index, item ->
            val menuItem = menuImageViews[index]
            menuItem.setBackgroundResource(typedValue.resourceId)
            menuItem.setImageResource(item)
            menuItem.scaleType = ImageView.ScaleType.CENTER
            menuItem.setOnClickListener {
                selectedItem = index
                onMenuItemClick(index)
            }
            if (index == activeItem) {
                // render the icon in fab instead of image view, but still allocate the space
                menuItem.visibility = View.INVISIBLE
            }
            val layoutParams = LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT)
            layoutParams.weight = 1f
            addView(menuItem, layoutParams)
        }
    }

    private fun onMenuItemClick(index: Int) {
        // make all item except current item invisible
        menuImageViews.forEachIndexed {i, imageView ->
            if (index == i) {
                // hide the current view
                imageView.visibility = View.INVISIBLE
            } else {
                imageView.visibility = View.VISIBLE
            }
        }
        val newOffsetX = menuWidth * index
        menuClickListener?.invoke(newOffsetX, menuWidth, index)
        computeCurve(newOffsetX, menuWidth)
        invalidate()
    }


    private fun computeCurve(offsetX: Int, w: Int) {
        // first curve
        firstCurveStart.apply {
            // you can change the 3rd param to control the spacing between curve start and FAB start
            x = offsetX + (w / 2) - CURVE_OFFSET
            y = 0f
        }
        firstCurveEnd.apply {
            x = offsetX + (w / 2f)
            y = FAB_RADIUS + (FAB_RADIUS / 4)
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
            y = 0f
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
        path.moveTo(0f, 0f)
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
        path.lineTo(width.toFloat(), 0f)
        path.lineTo(width.toFloat(), height.toFloat())
        path.lineTo(0f, height.toFloat())
        path.close()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        Log.d(TAG, "size: ($w, $h), center: (${w / 2}, ${h / 2})")
        // by default make the center item active
        menuWidth = w / menuItems.size
        computeCurve(menuWidth, menuWidth)
        // let the listener know about the initial item selection
        menuClickListener?.invoke(menuWidth * selectedItem, menuWidth, selectedItem)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawPath(path, paint)
    }

}