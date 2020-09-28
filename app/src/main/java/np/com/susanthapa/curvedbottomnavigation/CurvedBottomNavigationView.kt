package np.com.susanthapa.curvedbottomnavigation

import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.util.Log
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.bottomnavigation.LabelVisibilityMode

/**
 * Created by suson on 9/28/20
 */

class CurvedBottomNavigationView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : BottomNavigationView(context, attrs, defStyleAttr) {

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

    init {
        setBackgroundColor(Color.TRANSPARENT)
        labelVisibilityMode = LabelVisibilityMode.LABEL_VISIBILITY_UNLABELED
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        Log.d(TAG, "size: ($w, $h), center: (${w / 2}, ${h / 2})")

        // first curve
        firstCurveStart.apply {
            // you can change the 3rd param to control the spacing between curve start and FAB start
            x = (w / 2) - CURVE_OFFSET
            y = 0f
        }
        firstCurveEnd.apply {
            x = (w / 2f)
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
            x = (w / 2) + CURVE_OFFSET
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
        // draw the remaining portion of the bottom navigation
        path.lineTo(w.toFloat(), 0f)
        path.lineTo(w.toFloat(), h.toFloat())
        path.lineTo(0f, h.toFloat())
        path.close()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawPath(path, paint)
    }

}