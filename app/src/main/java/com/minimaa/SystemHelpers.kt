package com.minimaa

import android.Manifest
import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Intent
import android.os.Process
import android.provider.Settings
import android.content.Context
import android.content.pm.PackageManager
import android.provider.ContactsContract
import androidx.core.content.ContextCompat
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

object UsageHelper {
    fun hasUsageAccess(ctx: Context): Boolean = runCatching {
        val appOps = ctx.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        @Suppress("DEPRECATION")
        val mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(), ctx.packageName)
        mode == AppOpsManager.MODE_ALLOWED
    }.getOrDefault(false)

    fun openDigitalWellbeingAppUsage(ctx: Context, pkg: String) {
        val intents = listOf(
            Intent("com.google.android.apps.wellbeing.action.SHOW_APP_USAGE").putExtra(Intent.EXTRA_PACKAGE_NAME, pkg),
            Intent("android.settings.APP_USAGE_SETTINGS").putExtra(Intent.EXTRA_PACKAGE_NAME, pkg),
            ctx.packageManager.getLaunchIntentForPackage("com.google.android.apps.wellbeing")
        )
        for (intent in intents) {
            if (intent != null && runCatching { ctx.startActivity(intent) }.isSuccess) return
        }
        runCatching { ctx.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }
    }

    fun getTodayUsageMinutes(ctx: Context): Map<String, Int> = runCatching {
        val usm = ctx.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return emptyMap()
        val end = System.currentTimeMillis()
        val start = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0)
        }.timeInMillis
        usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, end)
            .orEmpty()
            .filter { it.totalTimeInForeground > 0 }
            .associate { it.packageName to (it.totalTimeInForeground / 60_000L).toInt() }
    }.getOrDefault(emptyMap())

    fun getActivePackagesLast7Days(ctx: Context): Set<String> = runCatching {
        val usm = ctx.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return emptySet()
        val end = System.currentTimeMillis()
        val start = end - 7L * 24 * 60 * 60 * 1000
        usm.queryUsageStats(UsageStatsManager.INTERVAL_WEEKLY, start, end)
            .orEmpty()
            .filter { it.totalTimeInForeground > 0 }
            .map { it.packageName }
            .toSet()
    }.getOrDefault(emptySet())

    fun formatMinutes(mins: Int): String = when {
        mins < 60 -> "${mins}m"
        else -> "${mins / 60}h ${mins % 60}m"
    }
}

object ContactHelper {
    fun searchContacts(ctx: Context, query: String): List<Pair<String, String>> {
        if (query.isBlank()) return emptyList()
        if (ContextCompat.checkSelfPermission(ctx, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return emptyList()
        }
        return runCatching {
            val res = mutableListOf<Pair<String, String>>()
            val cursor = ctx.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
                arrayOf("%$query%"),
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} ASC"
            )
            cursor?.use { c ->
                val nameIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (c.moveToNext() && res.size < 5) {
                    val n = if (nameIdx >= 0) c.getString(nameIdx).orEmpty() else ""
                    val p = if (numIdx >= 0) c.getString(numIdx).orEmpty() else ""
                    if (n.isNotBlank() && p.isNotBlank()) res.add(n to p)
                }
            }
            res.distinctBy { it.first + it.second }
        }.getOrDefault(emptyList())
    }
}

object BackupHelper {
    fun exportJson(journals: List<Journal>, tasks: List<Task>): String {
        val root = JSONObject()
        val jArr = JSONArray()
        journals.forEach { j ->
            jArr.put(JSONObject().apply {
                put("text", j.text)
                put("mood", j.mood)
                put("branches", j.branches)
                put("created", j.created)
            })
        }
        val tArr = JSONArray()
        tasks.forEach { t ->
            tArr.put(JSONObject().apply {
                put("title", t.title)
                put("type", t.type)
                put("category", t.category)
                put("priority", t.priority)
                put("done", t.done)
                put("scheduledAt", t.scheduledAt)
                put("created", t.created)
            })
        }
        root.put("journals", jArr)
        root.put("tasks", tArr)
        return root.toString(2)
    }

    fun importJson(raw: String): Pair<List<Journal>, List<Task>> {
        val root = JSONObject(raw)
        val jList = mutableListOf<Journal>()
        val jArr = root.optJSONArray("journals")
        if (jArr != null) {
            for (i in 0 until jArr.length()) {
                val o = jArr.getJSONObject(i)
                jList.add(Journal(
                    text = o.optString("text"),
                    mood = o.optInt("mood", 2),
                    branches = o.optString("branches"),
                    created = o.optLong("created", System.currentTimeMillis())
                ))
            }
        }
        val tList = mutableListOf<Task>()
        val tArr = root.optJSONArray("tasks")
        if (tArr != null) {
            for (i in 0 until tArr.length()) {
                val o = tArr.getJSONObject(i)
                tList.add(Task(
                    title = o.optString("title"),
                    type = o.optString("type", "TODO"),
                    category = o.optString("category", "Personal"),
                    priority = o.optString("priority", "Low"),
                    done = o.optBoolean("done", false),
                    scheduledAt = o.optLong("scheduledAt", 0L),
                    created = o.optLong("created", System.currentTimeMillis())
                ))
            }
        }
        return jList to tList
    }
}
