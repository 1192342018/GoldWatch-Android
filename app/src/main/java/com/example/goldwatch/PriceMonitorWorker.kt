package com.example.goldwatch

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class PriceMonitorWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val settings = SettingsStore(applicationContext)
        if (!settings.monitoringEnabled) return Result.success()

        return try {
            val price = GoldPriceRepository().fetch()
            settings.lastPrice = price.cnyPerGram
            settings.lastUpdatedAt = price.fetchedAtMillis

            val threshold = settings.threshold
            val isBelow = price.cnyPerGram <= threshold

            if (isBelow && !settings.wasBelowThreshold) {
                NotificationHelper.notifyLowPrice(
                    applicationContext,
                    price.cnyPerGram,
                    threshold
                )
            }
            settings.wasBelowThreshold = isBelow
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}
