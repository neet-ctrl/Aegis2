package com.android.geto.feature.home

import android.content.Context

data class SavedAutomation(
    val id: Long,
    val name: String,
    val triggerLabel: String,
    val conditionCount: Int,
    val actionSummary: String,
    val delaySeconds: Int,
    val isHidden: Boolean,
    val isEnabled: Boolean,
    val createdAt: Long,
)

object AegisAutomationStore {

    private const val PREF_NAME = "aegis_automations_v1"
    private const val KEY_SET = "automations"
    private const val SEP = "\u001E"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    private fun encode(a: SavedAutomation): String = listOf(
        a.id,
        a.name.replace(SEP, " "),
        a.triggerLabel.replace(SEP, " "),
        a.conditionCount,
        a.actionSummary.replace(SEP, " "),
        a.delaySeconds,
        a.isHidden,
        a.isEnabled,
        a.createdAt,
    ).joinToString(SEP)

    private fun decode(raw: String): SavedAutomation? = runCatching {
        val p = raw.split(SEP)
        if (p.size < 9) return null
        SavedAutomation(
            id = p[0].toLong(),
            name = p[1],
            triggerLabel = p[2],
            conditionCount = p[3].toInt(),
            actionSummary = p[4],
            delaySeconds = p[5].toInt(),
            isHidden = p[6].toBoolean(),
            isEnabled = p[7].toBoolean(),
            createdAt = p[8].toLong(),
        )
    }.getOrNull()

    fun addAutomation(context: Context, automation: SavedAutomation) {
        val p = prefs(context)
        val existing = p.getStringSet(KEY_SET, null)?.toMutableSet() ?: mutableSetOf()
        existing.add(encode(automation))
        p.edit().putStringSet(KEY_SET, existing).apply()
    }

    fun getAutomations(context: Context): List<SavedAutomation> {
        val raw = prefs(context).getStringSet(KEY_SET, null) ?: return emptyList()
        return raw.mapNotNull { decode(it) }.sortedByDescending { it.createdAt }
    }

    fun getVisibleAutomations(context: Context): List<SavedAutomation> =
        getAutomations(context).filter { !it.isHidden }

    fun getHiddenAutomations(context: Context): List<SavedAutomation> =
        getAutomations(context).filter { it.isHidden }

    fun deleteAutomation(context: Context, id: Long) {
        val p = prefs(context)
        val existing = p.getStringSet(KEY_SET, null)?.toMutableSet() ?: return
        val toRemove = existing.filter { decode(it)?.id == id }
        toRemove.forEach { existing.remove(it) }
        p.edit().putStringSet(KEY_SET, existing).apply()
        AegisActionStore.deleteActions(context, id)
    }

    fun toggleEnabled(context: Context, id: Long) {
        val p = prefs(context)
        val existing = p.getStringSet(KEY_SET, null)?.toMutableSet() ?: return
        val updated = mutableSetOf<String>()
        for (raw in existing) {
            val decoded = decode(raw)
            updated.add(
                if (decoded?.id == id) encode(decoded.copy(isEnabled = !decoded.isEnabled))
                else raw,
            )
        }
        p.edit().putStringSet(KEY_SET, updated).apply()
    }

    fun getEnabledCount(context: Context): Int =
        getAutomations(context).count { it.isEnabled }

    fun getTotalCount(context: Context): Int =
        getAutomations(context).size
}
