package otus.homework.customview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.annotation.ColorInt
import otus.homework.data.Payment
import java.util.Random
import kotlin.math.max
import kotlin.math.min

class PieView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {
    companion object {
        private const val DEGREE_TOTAL: Float = 360f

        @ColorInt
        private fun generateColor(): Int {
            val rnd = Random()
            return Color.argb(255, rnd.nextInt(256), rnd.nextInt(256), rnd.nextInt(256))
        }
    }

    private val oval: RectF = RectF()
    private val categories: ArrayList<Category> = ArrayList()
    private val paint = Paint().apply { style = Paint.Style.FILL }
    private var startAngle: Float = 0f

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
        startAngle = 0f
        categories.forEach { category: Category ->
            paint.color = category.color
            canvas.drawArc(oval, startAngle, category.weight, true, paint)
            startAngle += category.weight
        }
    }

    private fun topOvalRect(width: Float, height: Float): Float = when {
        width < height -> (height - width) / 2
        else -> 0f
    }

    private fun leftOvalRect(width: Float, height: Float): Float = when {
        width > height -> (width - height) / 2
        else -> 0f
    }

    private fun rightOvalRect(width: Float, height: Float): Float = when {
        width > height -> height + (width - height) / 2
        else -> width
    }

    private fun bottomOvalRect(width: Float, height: Float): Float = when {
        width < height -> width + (height - width) / 2
        else -> height
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            val radius = min(width, height) / 2
            if (event.x <= radius && event.y <= radius) {
                TODO("Обработать координаты внутри окружности")
            }
        }

        return true
    }
}

private data class Category(
    val weight: Float, val name: String, @ColorInt val color: Int
)
