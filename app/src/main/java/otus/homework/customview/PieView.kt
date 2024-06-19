package otus.homework.customview

import android.content.Context
import android.graphics.Color
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import androidx.annotation.ColorInt
import otus.homework.data.Payment
import java.util.Random

class PieView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
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

    fun setValues(data: List<Payment>) {
        val categoryMap = data.groupBy { it.category }
        categoryMap.keys.forEach { categoryName: String ->
            val weight: Float = (categoryMap[categoryName]
                ?.sumOf { it.amount }?.toFloat() ?: 0f) / DEGREE_TOTAL

            @ColorInt
            var tmpColor: Int = generateColor()
            while (categories.map { it.color }.contains(tmpColor)) {
                tmpColor = generateColor()
            }

            categories.add(
                Category(weight = weight, name = categoryName, color = tmpColor)
            )
        }
    }
}

private data class Category(
    val weight: Float,
    val name: String,
    @ColorInt
    val color: Int
)
