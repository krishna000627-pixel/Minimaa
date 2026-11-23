package com.minimaa

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlin.math.sqrt

class MinimaaAccessibilityService : AccessibilityService(), SensorEventListener {
    private var sensorManager: SensorManager? = null
    private var lastShakeTime = 0L

    private val blockedKeywords = listOf(
        "instagram.com", "tiktok.com", "reddit.com", "twitter.com", "x.com", "facebook.com"
    )
    private val adultKeywords = listOf("pornhub", "xvideos", "xnxx", "onlyfans")

    override fun onServiceConnected() {
        super.onServiceConnected()
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        val accel = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (accel != null) {
            sensorManager?.registerListener(this, accel, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    override fun onDestroy() {
        sensorManager?.unregisterListener(this)
        super.onDestroy()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        val prefs = getSharedPreferences("minimaa", Context.MODE_PRIVATE)
        val webDetox = prefs.getBoolean("detox_web", false)
        val adultBlock = prefs.getBoolean("detox_adult", false)
        if (!webDetox && !adultBlock) return

        val pkg = event.packageName?.toString() ?: return
        if (!pkg.contains("chrome") && !pkg.contains("browser") && !pkg.contains("firefox")) return

        val root = rootInActiveWindow ?: return
        val urlText = findUrlBarText(root)?.lowercase() ?: return
        val hitWeb = webDetox && blockedKeywords.any { it in urlText }
        val hitAdult = adultBlock && adultKeywords.any { it in urlText }
        if (hitWeb || hitAdult) {
            performGlobalAction(GLOBAL_ACTION_BACK)
        }
    }

    private fun findUrlBarText(node: AccessibilityNodeInfo): String? {
        val id = node.viewIdResourceName.orEmpty()
        if (id.contains("url_bar") || id.contains("address") || id.contains("omnibox")) {
            return node.text?.toString()
        }
        for (i in 0 until node.childCount) {
            val child = node.getChild(i) ?: continue
            val res = findUrlBarText(child)
            if (res != null) return res
        }
        return null
    }

    override fun onInterrupt() {}

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return
        val prefs = getSharedPreferences("minimaa", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("motion_enabled", false)) return

        val sens = prefs.getFloat("motion_sens", 3f)
        val threshold = 24f - sens * 2.5f
        val x = event.values[0]
        val y = event.values[1]
        val z = event.values[2]
        val mag = sqrt(x * x + y * y + z * z)
        val now = System.currentTimeMillis()
        if (mag > threshold && now - lastShakeTime > 1200L) {
            lastShakeTime = now
            val action = prefs.getString("motion_shake", "None") ?: "None"
            runAction(action)
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    private fun runAction(action: String) {
        when (action) {
            "System Back" -> performGlobalAction(GLOBAL_ACTION_BACK)
            "System Home" -> performGlobalAction(GLOBAL_ACTION_HOME)
            "Recent Apps" -> performGlobalAction(GLOBAL_ACTION_RECENTS)
            "Notifications" -> performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
            "Quick Settings" -> performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
            "Lock Screen" -> if (Build.VERSION.SDK_INT >= 28) performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN)
        }
    }
}
