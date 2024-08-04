package otus.homework.customview

import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt
import otus.homework.data.Payment
import java.math.BigDecimal
import java.util.Calendar
import java.util.Date

class ChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {
    companion object {
        private const val DEFAULT_STEP: Int = 50
        private const val DAY_IN_MILLS: Int = 1000 * 60 * 60 * 24
        private const val OFFSET_START: Int = 50
        private const val OFFSET_TOP: Int = 50
        private const val OFFSET_BOTTOM: Int = 50
        private const val X_TAIL: Int = 50
        private const val MAX_SPENT_AMOUNT_IN_DAY_Y_PERSENTAGE: Float = 0.9F
    }

    private val typedArray = context.obtainStyledAttributes(attrs, R.styleable.ChartView)
    private val step = typedArray.getDimension(R.styleable.ChartView_stepInDp, DEFAULT_STEP.dp)
    private val axisPaint = Paint().apply {
        style = Paint.Style.STROKE
        isAntiAlias = true
        color = Color.DKGRAY
        alpha = 200
        strokeWidth = 5f
    }

    private val greedPaint = Paint().apply {
        style = Paint.Style.STROKE
        isAntiAlias = true
        color = Color.DKGRAY
        alpha = 200
        strokeWidth = 2f
    }
    private val paymentList: ArrayList<PayDay> = ArrayList()
    private var days: Int = 0
    private val axisPath = Path()
    private val greedPath = Path()
    private var maxSpentInOneDay: Float = 0F

    @ColorInt
    private var categoryColor: Int = 0

    fun setValues(data: List<Payment>, category: Category) {
        categoryColor = category.color
        val categoryPayments = data.filter { it.category == category.name }
        days = daysTotal(categoryPayments)
        paymentList.apply {
            clear()
            addAll(calculatePaymentData(categoryPayments))
        }
        maxSpentInOneDay = paymentList.maxBy { it.amount }.amount.toFloat()
        requestLayout()
    }

    private fun calculatePaymentData(data: List<Payment>): List<PayDay> {
        val tmpPaymentList: ArrayList<PayDay> = ArrayList()
        val currentDate = Calendar.getInstance().apply {
            time = Date(data.minBy { it.time }.time)
        }
        for (day in 1..days) {
            var tmpAmount: BigDecimal = BigDecimal.ZERO
            data.forEach { payment: Payment ->
                val paymentDate = Calendar.getInstance().apply {
                    time = Date(payment.time)
                }
                if ((currentDate.get(Calendar.YEAR) == paymentDate.get(Calendar.YEAR)) &&
                    (currentDate.get(Calendar.DAY_OF_YEAR) == paymentDate.get(Calendar.DAY_OF_YEAR))
                ) {
                    tmpAmount += payment.amount.toBigDecimal()
                }
            }
            val tmpDate = Calendar.getInstance().apply {
                time = currentDate.time
            }
            val tmpPayDay: PayDay = PayDay(date = tmpDate, amount = tmpAmount)
            tmpPaymentList.add(tmpPayDay)
            currentDate.add(Calendar.DATE, 1)
        }
        return tmpPaymentList
    }

    private fun daysTotal(data: List<Payment>): Int {
        val minTime = data.minBy { it.time }.time
        val maxTime = data.maxBy { it.time }.time
        return ((maxTime - minTime) / DAY_IN_MILLS).toInt() + 1
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val xStart: Float = OFFSET_START.toFloat()
        val yStart: Float = OFFSET_TOP.toFloat()
        val xFinish: Float = OFFSET_START.toFloat() + step * days + X_TAIL
        val yFinish: Float = height - OFFSET_TOP.toFloat() - OFFSET_BOTTOM.toFloat()
        greedPath.apply {
            reset()
            for (day in 1..days) {
                // рисуем вертикальные линии сетки
                val xDay: Float = xStart + day * step
                moveTo(xDay, yStart)
                lineTo(xDay, yFinish)
            }
        }
        axisPath.apply {
            reset()
            // ось y: деньги
            moveTo(xStart, yStart)
            lineTo(xStart, yFinish)
            // ось x: дни
            moveTo(xStart, yFinish)
            lineTo(xFinish, yFinish)
        }

        canvas.apply {
            //рисуем оси и сетку, если есть количество дней уже известно
            if (days != 0) {
                drawPath(axisPath, axisPaint)
                drawPath(greedPath, greedPaint)
            }
        }
    }
}

private val Int.dp: Float
    get() = (this * Resources.getSystem().displayMetrics.density)

private data class PayDay(val date: Calendar, val amount: BigDecimal)