package com.example.goldwatch

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

private const val TROY_OUNCE_GRAMS = 31.1034768

data class GoldPrice(
    val cnyPerGram: Double,
    val usdPerOunce: Double,
    val usdCny: Double,
    val fetchedAtMillis: Long
)

class GoldPriceRepository {
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    fun fetch(): GoldPrice {
        val goldJson = getJson("https://api.gold-api.com/price/XAU")
        val fxJson = getJson("https://api.frankfurter.dev/v2/rate/USD/CNY")

        val usdPerOunce = goldJson.optDouble("price", Double.NaN)
        val usdCny = fxJson.optDouble("rate", Double.NaN)

        if (!usdPerOunce.isFinite() || usdPerOunce <= 0.0) {
            throw IOException("黄金价格接口返回无效数据")
        }
        if (!usdCny.isFinite() || usdCny <= 0.0) {
            throw IOException("USD/CNY 汇率接口返回无效数据")
        }

        return GoldPrice(
            cnyPerGram = usdPerOunce * usdCny / TROY_OUNCE_GRAMS,
            usdPerOunce = usdPerOunce,
            usdCny = usdCny,
            fetchedAtMillis = System.currentTimeMillis()
        )
    }

    private fun getJson(url: String): JSONObject {
        val request = Request.Builder()
            .url(url)
            .header("Accept", "application/json")
            .header("User-Agent", "GoldWatch-Android/1.0")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw IOException("HTTP ${response.code}: $url")
            }
            val body = response.body?.string() ?: throw IOException("接口返回空数据")
            return JSONObject(body)
        }
    }
}
