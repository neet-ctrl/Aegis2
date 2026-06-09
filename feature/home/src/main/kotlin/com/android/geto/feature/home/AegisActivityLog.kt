package com.android.geto.feature.home

import android.content.Context
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

internal data class LogEntry(
    val timestampMs: Long,
    val title: String,
    val subtitle: String,
    val tag: String,
)

internal object AegisActivityLog {

    private const val PREF_NAME = "aegis_activity_log_v1"
    private const val KEY_ENTRIES = "entries"
    private const val MAX_ENTRIES = 100
    private const val SEP = "\u001F"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    private fun encode(entry: LogEntry): String =
        "${entry.timestampMs}$SEP${entry.tag}$SEP${entry.title.replace(SEP, " ")}$SEP${entry.subtitle.replace(SEP, " ")}"

    private fun decode(raw: String): LogEntry? = runCatching {
        val parts = raw.split(SEP)
        if (parts.size < 4) return null
        LogEntry(
            timestampMs = parts[0].toLong(),
            tag = parts[1],
            title = parts[2],
            subtitle = parts[3],
        )
    }.getOrNull()

    fun addEntry(context: Context, title: String, subtitle: String, tag: String) {
        val p = prefs(context)
        val existing = p.getStringSet(KEY_ENTRIES, null)?.toMutableSet() ?: mutableSetOf()
        val newEntry = LogEntry(System.currentTimeMillis(), title, subtitle, tag)
        existing.add(encode(newEntry))
        val sorted = existing.mapNotNull { decode(it) }.sortedByDescending { it.timestampMs }
        val trimmed = sorted.take(MAX_ENTRIES).map { encode(it) }.toSet()
        p.edit().putStringSet(KEY_ENTRIES, trimmed).apply()
    }

    fun getEntries(context: Context): List<LogEntry> {
        val raw = prefs(context).getStringSet(KEY_ENTRIES, null) ?: return emptyList()
        return raw.mapNotNull { decode(it) }.sortedByDescending { it.timestampMs }
    }

    fun isEmpty(context: Context): Boolean =
        prefs(context).getStringSet(KEY_ENTRIES, null).isNullOrEmpty()

    fun clearAll(context: Context) {
        prefs(context).edit().remove(KEY_ENTRIES).apply()
    }

    fun getTodayCount(context: Context, tag: String): Int {
        val startOfDay = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        return getEntries(context).count { it.tag == tag && it.timestampMs >= startOfDay }
    }

    fun getSuccessRate(context: Context): Float {
        val entries = getEntries(context)
        val triggers = entries.count { it.tag == "trigger" }
        val errors = entries.count { it.tag == "error" }
        val total = triggers + errors
        return if (total == 0) 0f else triggers.toFloat() / total.toFloat()
    }

    fun formatRelativeTime(timestampMs: Long): String {
        val diff = System.currentTimeMillis() - timestampMs
        return when {
            diff < 60_000L -> "Just now"
            diff < 3_600_000L -> "${diff / 60_000L} min ago"
            diff < 86_400_000L -> "${diff / 3_600_000L} hr ago"
            else -> SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(timestampMs))
        }
    }

    fun seedIfEmpty(context: Context) {
        if (isEmpty(context)) {
            addEntry(context, "Aegis Started", "App launched — activity log initialized", "system")
        }
    }
}
