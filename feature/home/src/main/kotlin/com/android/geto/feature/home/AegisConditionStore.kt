package com.android.geto.feature.home

import android.content.Context

data class StoredCondition(
    val field: String,
    val operator: String,
    val value: String,
)

object AegisConditionStore {

    private const val PREF_NAME = "aegis_conditions_v1"
    private const val SEP = "\u001F"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    private fun encode(c: StoredCondition): String = listOf(
        c.field.replace(SEP, " "),
        c.operator.replace(SEP, " "),
        c.value.replace(SEP, " "),
    ).joinToString(SEP)

    private fun decode(raw: String): StoredCondition? = runCatching {
        val p = raw.split(SEP)
        if (p.size < 3) return null
        StoredCondition(field = p[0], operator = p[1], value = p[2])
    }.getOrNull()

    fun setConditions(context: Context, automationId: Long, conditions: List<StoredCondition>) {
        prefs(context).edit()
            .putStringSet("conds_$automationId", conditions.map { encode(it) }.toSet())
            .apply()
    }

    fun getConditions(context: Context, automationId: Long): List<StoredCondition> {
        val raw = prefs(context).getStringSet("conds_$automationId", null) ?: return emptyList()
        return raw.mapNotNull { decode(it) }
    }

    fun deleteConditions(context: Context, automationId: Long) {
        prefs(context).edit().remove("conds_$automationId").apply()
    }
}
