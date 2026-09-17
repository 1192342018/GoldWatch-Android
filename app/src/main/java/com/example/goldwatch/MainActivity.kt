package com.example.goldwatch

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.goldwatch.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.DateFormat
import java.util.Date

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var settings: SettingsStore

    private val handler = Handler(Looper.getMainLooper())
    private var refreshInProgress = false

    private val foregroundRefresh = object : Runnable {
        override fun run() {
            refreshPrice(showToastOnError = false)
            handler.postDelayed(this, 60_000L)
        }
    }

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (!granted) {
                Toast.makeText(
                    this,
                    "未授予通知权限：后台仍会检查价格，但无法弹出低价通知。",
                    Toast.LENGTH_LONG
                ).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        settings = SettingsStore(this)
        NotificationHelper.createChannel(this)
        restoreSettings()
        showCachedPrice()

        binding.refreshButton.setOnClickListener {
            refreshPrice(showToastOnError = true)
        }

        binding.saveButton.setOnClickListener {
            saveReminderSettings()
        }

        requestNotificationPermissionIfNeeded()
        refreshPrice(showToastOnError = false)
    }

    override fun onStart() {
        super.onStart()
        handler.removeCallbacks(foregroundRefresh)
        handler.postDelayed(foregroundRefresh, 60_000L)
    }

    override fun onStop() {
        handler.removeCallbacks(foregroundRefresh)
        super.onStop()
    }

    private fun restoreSettings() {
        binding.thresholdInput.setText("%.2f".format(settings.threshold))
        binding.monitorSwitch.isChecked = settings.monitoringEnabled
        updateStatusText()
    }

    private fun showCachedPrice() {
        if (settings.lastPrice.isFinite() && settings.lastUpdatedAt > 0L) {
            binding.priceText.text = "¥%.2f / 克".format(settings.lastPrice)
            binding.updateText.text = "上次更新：${formatTime(settings.lastUpdatedAt)}"
        }
    }

    private fun saveReminderSettings() {
        val threshold = binding.thresholdInput.text?.toString()?.trim()?.toDoubleOrNull()
        if (threshold == null || threshold <= 0.0 || threshold > 100_000.0) {
            binding.thresholdInput.error = "请输入有效的提醒价格"
            return
        }

        val changedThreshold = threshold != settings.threshold
        settings.threshold = threshold
        settings.monitoringEnabled = binding.monitorSwitch.isChecked
        if (changedThreshold) settings.wasBelowThreshold = false

        if (settings.monitoringEnabled) {
            MonitorScheduler.enable(this)
            requestNotificationPermissionIfNeeded()
        } else {
            MonitorScheduler.disable(this)
        }

        updateStatusText()
        Toast.makeText(this, "提醒设置已保存", Toast.LENGTH_SHORT).show()
    }

    private fun updateStatusText() {
        binding.statusText.text = if (settings.monitoringEnabled) {
            "已开启：系统将在有网络时约每 15 分钟检查一次；首次跌破阈值时提醒，持续低于阈值不会重复轰炸。"
        } else {
            "后台提醒未开启。打开应用时仍会每 60 秒自动刷新一次价格。"
        }
    }

    private fun refreshPrice(showToastOnError: Boolean) {
        if (refreshInProgress) return
        refreshInProgress = true
        binding.refreshButton.isEnabled = false
        binding.refreshButton.text = "刷新中…"

        lifecycleScope.launch {
            try {
                val price = withContext(Dispatchers.IO) {
                    GoldPriceRepository().fetch()
                }

                settings.lastPrice = price.cnyPerGram
                settings.lastUpdatedAt = price.fetchedAtMillis

                binding.priceText.text = "¥%.2f / 克".format(price.cnyPerGram)
                binding.changeText.text = "XAU/USD %.2f · USD/CNY %.4f".format(
                    price.usdPerOunce,
                    price.usdCny
                )
                binding.updateText.text = "更新时间：${formatTime(price.fetchedAtMillis)}"

                if (settings.monitoringEnabled) {
                    val isBelow = price.cnyPerGram <= settings.threshold
                    if (isBelow && !settings.wasBelowThreshold) {
                        NotificationHelper.notifyLowPrice(
                            this@MainActivity,
                            price.cnyPerGram,
                            settings.threshold
                        )
                    }
                    settings.wasBelowThreshold = isBelow
                }
            } catch (e: Exception) {
                binding.updateText.text = "更新失败：${e.message ?: "网络或数据源异常"}"
                if (showToastOnError) {
                    Toast.makeText(this@MainActivity, "价格刷新失败", Toast.LENGTH_SHORT).show()
                }
            } finally {
                refreshInProgress = false
                binding.refreshButton.isEnabled = true
                binding.refreshButton.text = "立即刷新"
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    private fun formatTime(timeMillis: Long): String =
        DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM).format(Date(timeMillis))
}
