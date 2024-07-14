package otus.homework.data

import com.google.gson.annotations.SerializedName

data class Payment(
    @SerializedName("id") var id: Int,
    @SerializedName("name") var name: String,
    @SerializedName("amount") var amount: Int,
    @SerializedName("category") var category: String,
    @SerializedName("time") var time: Long
)