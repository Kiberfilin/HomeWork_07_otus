package otus.homework.customview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.content.res.Resources
import android.graphics.Path
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import android.view.MotionEvent
import android.view.View
import android.view.View.BaseSavedState
import androidx.annotation.ColorInt
import kotlinx.parcelize.Parcelize
import otus.homework.data.Payment
import java.util.Random
import kotlin.math.atan2
import kotlin.math.min
import kotlin.math.pow

class PieView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {
    companion object {
        private const val DEGREE_TOTAL: Float = 360f
        private const val DEFAULT_PIE_RADIUS: Int = 200
        private const val TAG: String = "MeasureSpecInfo"
        private const val DECIMAL_FRACTION_OF_CIRCLE: Float = 0.7f

        @ColorInt
        private fun generateColor(): Int {
            val rnd = Random()
            return Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256))
        }
    }

    private val typedArray = context.obtainStyledAttributes(attrs, R.styleable.PieView)
    private val pieRadius = typedArray.getDimension(
        R.styleable.PieView_pieRadius, DEFAULT_PIE_RADIUS.dp
    )
    private val oval: RectF = RectF()
    private val categories: ArrayList<Category> = ArrayList()
    private val paint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }
    private val pathOuterCircle = Path()
    private val pathInnerCircle = Path()
    private var startAngle: Float = 0f

    var callback: ((Category) -> Unit)? = null

    fun setValues(data: List<Payment>) {
        val totalAmount: Float = data.sumOf { it.amount }.toFloat()
        val categoryMap = data.groupBy { it.category }
        categoryMap.keys.forEach { categoryName: String ->
            val weight: Float = (categoryMap[categoryName]?.sumOf { it.amount }?.toFloat()
                ?: 0f) * DEGREE_TOTAL / totalAmount

            @ColorInt var tmpColor: Int = generateColor()
            while (categories.map { it.color }.contains(tmpColor)) {
                tmpColor = generateColor()
            }

            categories.add(
                Category(weight = weight, name = categoryName, color = tmpColor)
            )
        }
        categories.sortBy { it.weight }
        requestLayout()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val wMode = MeasureSpec.getMode(widthMeasureSpec)
        val hMode = MeasureSpec.getMode(heightMeasureSpec)
        val wSize = MeasureSpec.getSize(widthMeasureSpec)
        val hSize = MeasureSpec.getSize(heightMeasureSpec)

        val newW = min((2 * pieRadius).toInt(), wSize)
        val newH = min((2 * pieRadius).toInt(), hSize)
        println(
            "$TAG (2 * pieRadius).toInt() = ${(2 * pieRadius).toInt()}; wSize = $wSize; hSize = $hSize"
        )
        when (wMode) {
            MeasureSpec.EXACTLY     -> when (hMode) {
                MeasureSpec.EXACTLY     -> {
                    println("$TAG W_EXACTLY H_EXACTLY $wSize $hSize")
                    setMeasuredDimension(wSize, hSize)
                }

                MeasureSpec.AT_MOST     -> {
                    println("$TAG W_EXACTLY H_AT_MOST $wSize $hSize")
                    setMeasuredDimension(wSize, newH)
                }

                MeasureSpec.UNSPECIFIED -> {
                    println("$TAG W_EXACTLY H_UNSPECIFIED $wSize $hSize")
                    setMeasuredDimension(wSize, 2 * pieRadius.toInt())
                }
            }

            MeasureSpec.AT_MOST     -> when (hMode) {
                MeasureSpec.EXACTLY     -> {
                    println("$TAG W_AT_MOST H_EXACTLY $wSize $hSize")
                    setMeasuredDimension(newW, hSize)
                }

                MeasureSpec.AT_MOST     -> {
                    println("$TAG W_AT_MOST H_AT_MOST $wSize $hSize")
                    setMeasuredDimension(newW, newH)
                }

                MeasureSpec.UNSPECIFIED -> {
                    println("$TAG W_AT_MOST H_UNSPECIFIED $wSize $hSize")
                    setMeasuredDimension(newW, 2 * pieRadius.toInt())
                }
            }

            MeasureSpec.UNSPECIFIED -> when (hMode) {
                MeasureSpec.EXACTLY     -> {
                    println("$TAG W_UNSPECIFIED H_EXACTLY $wSize $hSize")
                    setMeasuredDimension(2 * pieRadius.toInt(), hSize)
                }

                MeasureSpec.AT_MOST     -> {
                    println("$TAG W_UNSPECIFIED H_AT_MOST $wSize $hSize")
                    setMeasuredDimension(2 * pieRadius.toInt(), newH)
                }

                MeasureSpec.UNSPECIFIED -> {
                    println("$TAG W_UNSPECIFIED H_UNSPECIFIED $wSize $hSize")
                    setMeasuredDimension(2 * pieRadius.toInt(), 2 * pieRadius.toInt())
                }
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (categories.size == 0) return
        oval.set(
            leftOvalRect(width.toFloat(), height.toFloat()),
            topOvalRect(width.toFloat(), height.toFloat()),
            rightOvalRect(width.toFloat(), height.toFloat()),
            bottomOvalRect(width.toFloat(), height.toFloat())
        )
        pathOuterCircle.apply {
            addCircle(oval.centerX(), oval.centerY(), oval.width() / 2f, Path.Direction.CCW)
        }
        pathInnerCircle.apply {
            addCircle(
                oval.centerX(),
                oval.centerY(),
                oval.width() * DECIMAL_FRACTION_OF_CIRCLE / 2f,
                Path.Direction.CCW
            )
            op(pathOuterCircle, Path.Op.REVERSE_DIFFERENCE)
        }
        canvas.apply {
            clipPath(pathInnerCircle)
            startAngle = 0f
            categories.forEach { category: Category ->
                paint.color = category.color
                drawArc(oval, startAngle, category.weight, true, paint)
                startAngle += category.weight
            }
        }
    }

    private fun topOvalRect(width: Float, height: Float): Float = when {
        width < height -> (height - width) / 2
        else           -> 0f
    }

    private fun leftOvalRect(width: Float, height: Float): Float = when {
        width > height -> (width - height) / 2
        else           -> 0f
    }

    private fun rightOvalRect(width: Float, height: Float): Float = when {
        width > height -> height + (width - height) / 2
        else           -> width
    }

    private fun bottomOvalRect(width: Float, height: Float): Float = when {
        width < height -> width + (height - width) / 2
        else           -> height
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val radius: Float = min(width, height).toFloat() / 2f
            val katetX: Float = event.x - width.toFloat() / 2f
            val katetY: Float = event.y - height.toFloat() / 2f
            if (
                (katetX.pow(2) + katetY.pow(2)) in
                (radius * DECIMAL_FRACTION_OF_CIRCLE).pow(2)..radius.pow(2)

            ) {
                val atan: Float = Math.toDegrees(atan2(katetY, katetX).toDouble()).toFloat()
                val ugol = when {
                    atan < 0 -> atan + DEGREE_TOTAL
                    else     -> atan
                }
                var tmpWeight: Float = 0f
                for (category in categories) {
                    tmpWeight += category.weight
                    if (ugol <= tmpWeight) {
                        callback?.invoke(category)
                        break
                    }
                }
            }
        }

        return true
    }

    override fun onSaveInstanceState(): Parcelable? {
        val parentState = super.onSaveInstanceState()
        val savedState = SavedState(parentState)
        savedState.categories = categories
        return savedState
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        super.onRestoreInstanceState((state as SavedState).superState)
        categories.apply {
            clear()
            addAll(state.categories)
        }
    }
}

@Parcelize
class Category(val weight: Float, val name: String, @ColorInt val color: Int) : Parcelable

private val Int.dp: Float
    get() = (this * Resources.getSystem().displayMetrics.density)

private class SavedState : BaseSavedState {
    var categories: ArrayList<Category> = ArrayList()

    //Конструктор для сохранения состояния
    constructor(superState: Parcelable?) : super(superState)

    //Конструктор для восстановления состояния
    constructor(source: Parcel?) : super(source) {
        val tmpList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) ({
            source?.readArrayList(Category::class.java.classLoader, Category::class.java)
        }) else ({
            source?.readArrayList(Category::class.java.classLoader)
        })
        categories = tmpList as ArrayList<Category>
    }

    override fun writeToParcel(out: Parcel, flags: Int) {
        super.writeToParcel(out, flags)
        out.writeTypedList(categories)
    }

    companion object CREATOR : Parcelable.Creator<SavedState> {
        override fun createFromParcel(source: Parcel): SavedState = SavedState(source)
        override fun newArray(size: Int): Array<SavedState?> = arrayOfNulls(size)
    }
}