package otus.homework.customview

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.annotation.RawRes
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import otus.homework.data.Payment

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val data: List<Payment> = readRawJson(R.raw.payload)
        findViewById<PieView>(R.id.chart).apply {
            setValues(data)
        }
    }

    private inline fun <reified T> readRawJson(@RawRes rawResId: Int): T {
        val gson = Gson()
        resources.openRawResource(rawResId).bufferedReader().use {
            return gson.fromJson<T>(it, object : TypeToken<T>() {}.type)
        }
    }
}