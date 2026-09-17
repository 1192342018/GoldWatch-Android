package com.example.goldwatch

import android.content.Context

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("gold_watch_settings", Context.MODE_PRIVATE)

    var threshold: Double
        get() = java.lang.Double.longBitsToDouble(
            prefs.getLong(KEY_THRESHOLD, java.lang.Double.doubleToRawLongBits(900.0))
        )
        set(value) = prefs.edit().putLong(KEY_THRESHOLD, java.lang.Double.doubleToRawLongBits(value)).apply()

    var monitoringEnabled: Boolean
        get() = prefs.getBoolean(KEY_MONITORING, false)
        set(value) = prefs.edit().putBoolean(KEY_MONITORING, value).apply()

    var wasBelowThreshold: Boolean
        get() = prefs.getBoolean(KEY_WAS_BELOW, false)
        set(value) = prefs.edit().putBoolean(KEY_WAS_BELOW, value).apply()

    var lastPrice: Double
        get() = java.lang.Double.longBitsToDouble(
            prefs.getLong(KEY_LAST_PRICE, java.lang.Double.doubleToRawLongBits(Double.NaN))
        )
        set(value) = prefs.edit().putLong(KEY_LAST_PRICE, java.lang.Double.doubleToRawLongBits(value)).apply()

    var lastUpdatedAt: Long
        get() = prefs.getLong(KEY_LAST_UPDATED_AT, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_UPDATED_AT, value).apply()

    companion object {
        private const val KEY_THRESHOLD = "threshold"
        private const val KEY_MONITORING = "monitoring"
        private const val KEY_WAS_BELOW = "was_below_threshold"
        private const val KEY_LAST_PRICE = "last_price"
        private const val KEY_LAST_UPDATED_AT = "last_updated_at"
    }
}
