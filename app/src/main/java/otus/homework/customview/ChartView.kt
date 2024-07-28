package otus.homework.customview

import android.content.Context
import android.content.res.Resources
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt
import otus.homework.data.Payment
import java.math.BigDecimal
import java.util.Calendar
import java.util.Date
import java.util.Random

class ChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {
    companion object {
        private const val DEFAULT_STEP: Int = 50
        private const val DAY_IN_MILLS: Int = 1000 * 60 * 60 * 24
        private const val OFFSET_START: Int = 20
        private const val OFFSET_TOP: Int = 20
    }

    private val typedArray = context.obtainStyledAttributes(attrs, R.styleable.ChartView)
    private val step = typedArray.getDimension(R.styleable.ChartView_stepInDp, DEFAULT_STEP.dp)
    private val greedPaint = Paint().apply {
        style = Paint.Style.STROKE
        isAntiAlias = true
        color = Color.DKGRAY
        alpha = 200
        strokeWidth = 3f
    }
    private val paymentList: ArrayList<PayDay> = ArrayList()
    private var days: Int = 0

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
}

private val Int.dp: Float
    get() = (this * Resources.getSystem().displayMetrics.density)

private data class PayDay(val date: Calendar, val amount: BigDecimal)