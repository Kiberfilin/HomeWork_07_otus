package otus.homework.customview

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.os.Build
import android.os.Parcel
import android.os.Parcelable
import android.util.AttributeSet
import android.view.View
import android.view.View.BaseSavedState
import androidx.annotation.ColorInt
import kotlinx.parcelize.Parcelize
import otus.homework.data.Payment
import java.math.BigDecimal
import java.text.SimpleDateFormat
import java.util.Calendar
import kotlin.math.min
import kotlin.math.pow

class ChartView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {
    companion object {
        private const val DEFAULT_STEP: Int = 50
        private const val DAY_IN_MILLS: Int = 1000 * 60 * 60 * 24
        private const val MILLS_IN_ONE_SEC: Int = 1000
        private const val OFFSET_START: Int = 50
        private const val OFFSET_TOP: Int = 50
        private const val OFFSET_BOTTOM: Int = 50
        private const val X_TAIL: Int = 50
        private const val TAG: String = "ChartView"
    }

    private val typedArray = context.obtainStyledAttributes(attrs, R.styleable.ChartView)
    private val step = typedArray.getDimension(R.styleable.ChartView_stepInDp, DEFAULT_STEP.dp)
    private val axisPaint = Paint().apply {
        style = Paint.Style.STROKE
        isAntiAlias = true
        color = Color.DKGRAY
        alpha = 200
        strokeWidth = 4f
    }

    private val greedPaint = Paint().apply {
        style = Paint.Style.STROKE
        pathEffect = DashPathEffect(floatArrayOf(10f, 20f), 0f)
        isAntiAlias = true
        color = Color.DKGRAY
        alpha = 200
        strokeWidth = 2f
    }
    private val graphPaint = Paint().apply {
        style = Paint.Style.STROKE
        isAntiAlias = true
        strokeWidth = 5f
    }

    private val textPaint = Paint().apply {
        style = Paint.Style.FILL
        textAlign = Paint.Align.LEFT
        isAntiAlias = true
        color = Color.DKGRAY
        alpha = 200
        strokeWidth = 2f
    }

    private val paymentList: ArrayList<PayDay> = ArrayList()
    private var days: Int = 0
    private val axisPath = Path()
    private val greedPath = Path()
    private val graphPath = Path()
    private var maxSpentInOneDay: Float = 0F
    private var divisionCostY: Float = 1F
    private var useShortDateFormat: Boolean = false

    @ColorInt
    private var categoryColor: Int = 0

    @SuppressLint("SimpleDateFormat")
    private var dateFormat: SimpleDateFormat = SimpleDateFormat("dd.MM.yyyy")

    @SuppressLint("SimpleDateFormat")
    private var shortDateFormat: SimpleDateFormat = SimpleDateFormat("dd")

    fun setValues(data: List<Payment>, category: Category) {
        categoryColor = category.color
        val categoryPayments = data.filter { it.category == category.name }
        println("$TAG categoryPayments $categoryPayments")
        days = daysTotal(categoryPayments)
        println("$TAG daysTotal $days")
        paymentList.apply {
            clear()
            addAll(calculatePaymentData(categoryPayments))
        }
        println("$TAG paymentList $paymentList")
        maxSpentInOneDay = paymentList.maxBy { it.amount }.amount.toFloat()
        println("$TAG maxSpentInOneDay $maxSpentInOneDay")
        divisionCostY = calculateDivisionCostY(maxSpentInOneDay)
        println("$TAG divisionCostY $divisionCostY")
        requestLayout()
    }

    private fun calculateDivisionCostY(maxSpentInOneDay: Float): Float {
        val maxSpentInOneDayString = maxSpentInOneDay.toString()
        val number = maxSpentInOneDayString.split(',', '.').first()
        val lengthOfNumber = number.length
        return 10f.pow(lengthOfNumber - 1)
    }

    private fun calculatePaymentData(data: List<Payment>): List<PayDay> {
        val tmpPaymentList: ArrayList<PayDay> = ArrayList()
        val currentDate = Calendar.getInstance().apply {
            timeInMillis = data.minBy { it.time }.time * MILLS_IN_ONE_SEC
        }

        for (day in 1..days) {
            var tmpAmount: BigDecimal = BigDecimal.ZERO
            data.forEach { payment: Payment ->
                val paymentDate = Calendar.getInstance().apply {
                    timeInMillis = payment.time * MILLS_IN_ONE_SEC
                }
                if ((currentDate.get(Calendar.YEAR) == paymentDate.get(Calendar.YEAR)) &&
                    (currentDate.get(Calendar.DAY_OF_YEAR) == paymentDate.get(Calendar.DAY_OF_YEAR))
                ) {
                    tmpAmount += payment.amount.toBigDecimal()
                }
            }
            val tmpDate = Calendar.getInstance().apply {
                timeInMillis = currentDate.timeInMillis
            }
            val tmpPayDay = PayDay(date = tmpDate, amount = tmpAmount)
            tmpPaymentList.add(tmpPayDay)
            currentDate.add(Calendar.DATE, 1)
        }
        return tmpPaymentList
    }

    private fun daysTotal(data: List<Payment>): Int {
        val minTime = data.minBy { it.time }.time
        val maxTime = data.maxBy { it.time }.time
        return ((maxTime - minTime) * 1000L / DAY_IN_MILLS).toInt() + 1
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val wMode = MeasureSpec.getMode(widthMeasureSpec)
        val hMode = MeasureSpec.getMode(heightMeasureSpec)
        val wSize = MeasureSpec.getSize(widthMeasureSpec)
        val hSize = MeasureSpec.getSize(heightMeasureSpec)

        val newW = min((step * days + OFFSET_START + X_TAIL).toInt(), wSize)
        val newH = min((5 * step + OFFSET_TOP + OFFSET_BOTTOM).toInt(), hSize)
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
                    setMeasuredDimension(wSize, (5 * step).toInt())
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
                    setMeasuredDimension(newW, (5 * step).toInt())
                }
            }

            MeasureSpec.UNSPECIFIED -> when (hMode) {
                MeasureSpec.EXACTLY     -> {
                    println("$TAG W_UNSPECIFIED H_EXACTLY $wSize $hSize")
                    setMeasuredDimension(
                        (step * days + OFFSET_START + X_TAIL).toInt(),
                        hSize
                    )
                }

                MeasureSpec.AT_MOST     -> {
                    println("$TAG W_UNSPECIFIED H_AT_MOST $wSize $hSize")
                    setMeasuredDimension(
                        (step * days + OFFSET_START + X_TAIL).toInt(),
                        newH
                    )
                }

                MeasureSpec.UNSPECIFIED -> {
                    println("$TAG W_UNSPECIFIED H_UNSPECIFIED $wSize $hSize")
                    setMeasuredDimension(
                        (step * days + OFFSET_START + X_TAIL).toInt(),
                        (5 * step).toInt()
                    )
                }
            }
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (days != 0) {
            println("$TAG ---------------------------")
            val xStart: Float = OFFSET_START.toFloat()
            val yStart: Float = OFFSET_TOP.toFloat()
            val xFinish: Float = OFFSET_START.toFloat() + step * days + X_TAIL
            val yFinish: Float = (height - OFFSET_TOP - OFFSET_BOTTOM).toFloat()
            val totalHorisontalLines = (maxSpentInOneDay / divisionCostY).toInt() + 1
            println("$TAG totalHorisontalLines $totalHorisontalLines")
            val density = (yFinish - OFFSET_TOP) / (totalHorisontalLines * divisionCostY)
            println("$TAG density $density")
            val tmpTextSize = yFinish / 10f / 3f
            textPaint.textSize = tmpTextSize
            paymentList.forEach { day: PayDay ->
                val tmpText = dateFormat.format(day.date.time)
                val fittedCharsNumber = textPaint.breakText(tmpText, false, step, null)
                useShortDateFormat = fittedCharsNumber < tmpText.length
            }
            greedPath.apply {
                reset()
                for (day in 1..days) {
                    // рисуем вертикальные линии сетки и подписи к ним
                    val xDay: Float = xStart + day * step
                    moveTo(xDay, yStart)
                    lineTo(xDay, yFinish)
                    val verticalGreedLineText = if (useShortDateFormat) {
                        shortDateFormat.format(paymentList[day - 1].date.time)
                    } else {
                        dateFormat.format(paymentList[day - 1].date.time)
                    }
                    canvas.drawText(
                        verticalGreedLineText,
                        xDay - step + OFFSET_START * 0.2f,
                        yFinish - textPaint.textSize * 0.2f,
                        textPaint
                    )
                }
                // рисуем горизонтальные линии сетки и подписи к ним
                textPaint.textSize = tmpTextSize
                for (division in 1..totalHorisontalLines) {
                    val tmpY = yFinish - (division * density * divisionCostY)
                    moveTo(xStart, tmpY)
                    lineTo(xFinish, tmpY)
                    val horizontalGreedLineText = (divisionCostY * division).toInt().toString()
                    canvas.drawText(
                        horizontalGreedLineText,
                        xStart + OFFSET_START * 0.2f,
                        tmpY + textPaint.textSize,
                        textPaint
                    )
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
                drawPath(axisPath, axisPaint)
                drawPath(greedPath, greedPaint)
                graphPath.apply {
                    reset()
                    //отрисовываем график
                    moveTo(xStart, yFinish - density * paymentList.first().amount.toFloat())
                    for (day in 1..<paymentList.size) {
                        val tmpX = xStart + step * day
                        val tmpY = yFinish - density * paymentList[day].amount.toFloat()
                        lineTo(tmpX, tmpY)
                    }
                    lineTo(
                        xStart + step * days - X_TAIL,
                        yFinish - density * paymentList.last().amount.toFloat()
                    )
                }
                graphPaint.color = categoryColor
                drawPath(graphPath, graphPaint)
            }
        }
    }

    override fun onSaveInstanceState(): Parcelable? {
        val parentState = super.onSaveInstanceState()
        val savedState = ChartViewSavedState(parentState)
        savedState.paymentList = paymentList
        savedState.days = days
        savedState.maxSpentInOneDay = maxSpentInOneDay
        savedState.divisionCostY = divisionCostY
        savedState.categoryColor = categoryColor
        return savedState
    }

    override fun onRestoreInstanceState(state: Parcelable?) {
        super.onRestoreInstanceState((state as ChartViewSavedState).superState)
        paymentList.apply {
            clear()
            addAll(state.paymentList)
        }
        days = state.days
        maxSpentInOneDay = state.maxSpentInOneDay
        divisionCostY = state.divisionCostY
        categoryColor = state.categoryColor
    }
}

private val Int.dp: Float
    get() = (this * Resources.getSystem().displayMetrics.density)

@Parcelize
private data class PayDay(val date: Calendar, val amount: BigDecimal) : Parcelable

private class ChartViewSavedState : BaseSavedState {
    var paymentList: ArrayList<PayDay> = ArrayList()
    var days: Int = 0
    var maxSpentInOneDay: Float = 0F
    var divisionCostY: Float = 1F

    @ColorInt
    var categoryColor: Int = 0

    //Конструктор для сохранения состояния
    constructor(superState: Parcelable?) : super(superState)

    //Конструктор для восстановления состояния
    constructor(source: Parcel?) : super(source) {
        val tmpList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) ({
            source?.readArrayList(PayDay::class.java.classLoader, PayDay::class.java)
        }) else ({
            source?.readArrayList(PayDay::class.java.classLoader)
        })
        paymentList = tmpList as ArrayList<PayDay>
        days = source?.readInt()!!
        maxSpentInOneDay = source.readFloat()
        divisionCostY = source.readFloat()
        categoryColor = source.readInt()
    }

    override fun writeToParcel(out: Parcel, flags: Int) {
        super.writeToParcel(out, flags)
        out.apply {
            writeTypedList(paymentList)
            writeInt(days)
            writeFloat(maxSpentInOneDay)
            writeFloat(divisionCostY)
            writeInt(categoryColor)
        }

    }

    companion object CREATOR : Parcelable.Creator<ChartViewSavedState> {
        override fun createFromParcel(source: Parcel): ChartViewSavedState =
            ChartViewSavedState(source)

        override fun newArray(size: Int): Array<ChartViewSavedState?> = arrayOfNulls(size)
    }
}
