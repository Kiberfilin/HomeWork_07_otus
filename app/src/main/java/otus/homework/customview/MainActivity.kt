package otus.homework.customview

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.annotation.RawRes
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import otus.homework.data.Payment

class MainActivity : AppCompatActivity() {
    private var isCategoriesWasSet: Boolean = false
    private val keyForBundle: String = "isCategoriesWasSet"
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val data: List<Payment> = readRawJson(R.raw.payload)
        val button = findViewById<Button>(R.id.button).apply {
            text = "Установить значения"
            visibility = when (savedInstanceState?.getBoolean(keyForBundle)) {
                true -> View.GONE
                false, null -> View.VISIBLE
            }
        }
        findViewById<PieView>(R.id.pie).apply {
            button.setOnClickListener {
                setValues(data)
                button.visibility = View.GONE
                isCategoriesWasSet = true
            }
            callback = { category: Category ->
                Snackbar.make(rootView, category.name, Snackbar.LENGTH_LONG).show()
                findViewById<ChartView>(R.id.chart).apply {
                    setValues(data, category.name, category.color)
                }
            }
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(keyForBundle, isCategoriesWasSet)
    }

    override fun onRestoreInstanceState(savedInstanceState: Bundle) {
        super.onRestoreInstanceState(savedInstanceState)
        isCategoriesWasSet = savedInstanceState.getBoolean(keyForBundle)
    }

    private inline fun <reified T> readRawJson(@RawRes rawResId: Int): T {
        val gson = Gson()
        resources.openRawResource(rawResId).bufferedReader().use {
            return gson.fromJson<T>(it, object : TypeToken<T>() {}.type)
        }
    }
}
