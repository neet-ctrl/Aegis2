package com.android.geto.feature.home

import android.content.Context

data class StoredAction(
    val label: String,
    val settingKey: String,
    val settingType: String,
    val value: String,
)

object AegisActionStore {

    private const val PREF_NAME = "aegis_actions_v1"
    private const val SEP = "\u001F"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    private fun encode(action: StoredAction): String = listOf(
        action.label.replace(SEP, " "),
        action.settingKey.replace(SEP, " "),
        action.settingType.replace(SEP, " "),
        action.value.replace(SEP, " "),
    ).joinToString(SEP)

    private fun decode(raw: String): StoredAction? = runCatching {
        val parts = raw.split(SEP)
        if (parts.size < 4) return null
        StoredAction(
            label = parts[0],
            settingKey = parts[1],
            settingType = parts[2],
            value = parts[3],
        )
    }.getOrNull()

    fun setActions(context: Context, automationId: Long, actions: List<StoredAction>) {
        val set = actions.map { encode(it) }.toSet()
        prefs(context).edit().putStringSet("actions_$automationId", set).apply()
    }

    fun getActions(context: Context, automationId: Long): List<StoredAction> {
        val raw = prefs(context).getStringSet("actions_$automationId", null) ?: return emptyList()
        return raw.mapNotNull { decode(it) }
    }

    fun deleteActions(context: Context, automationId: Long) {
        prefs(context).edit().remove("actions_$automationId").apply()
    }
}
