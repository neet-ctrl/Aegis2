package com.android.geto.feature.appsettings.security

import android.content.Context
import java.security.MessageDigest

object AppLockManager {

    private const val PREF_NAME = "aegis_app_lock_v1"
    private const val APP_SALT = "aegis_security_2024"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun getConfig(context: Context, packageName: String): AppLockConfig {
        val p = prefs(context)
        val typeName = p.getString("${packageName}_type", LockType.NONE.name) ?: LockType.NONE.name
        return AppLockConfig(
            packageName = packageName,
            lockType = runCatching { LockType.valueOf(typeName) }.getOrDefault(LockType.NONE),
            isEnabled = p.getBoolean("${packageName}_enabled", false),
            isBiometricEnabled = p.getBoolean("${packageName}_biometric", false),
            isBlocked = p.getBoolean("${packageName}_blocked", false),
            hasCredential = p.contains("${packageName}_hash"),
        )
    }

    fun saveConfig(context: Context, config: AppLockConfig) {
        prefs(context).edit().apply {
            putString("${config.packageName}_type", config.lockType.name)
            putBoolean("${config.packageName}_enabled", config.isEnabled)
            putBoolean("${config.packageName}_biometric", config.isBiometricEnabled)
            putBoolean("${config.packageName}_blocked", config.isBlocked)
            apply()
        }
    }

    fun setCredential(context: Context, packageName: String, credential: String) {
        val hash = sha256("$APP_SALT:$packageName:$credential")
        prefs(context).edit().putString("${packageName}_hash", hash).apply()
    }

    fun verifyCredential(context: Context, packageName: String, credential: String): LockVerifyResult {
        val stored = prefs(context).getString("${packageName}_hash", null)
            ?: return LockVerifyResult.NoCredential
        val hash = sha256("$APP_SALT:$packageName:$credential")
        return if (stored == hash) LockVerifyResult.Success else LockVerifyResult.Failed
    }

    fun clearLock(context: Context, packageName: String) {
        prefs(context).edit().apply {
            remove("${packageName}_type")
            remove("${packageName}_enabled")
            remove("${packageName}_biometric")
            remove("${packageName}_blocked")
            remove("${packageName}_hash")
            apply()
        }
    }

    fun setBiometric(context: Context, packageName: String, enabled: Boolean) {
        prefs(context).edit().putBoolean("${packageName}_biometric", enabled).apply()
    }

    fun setBlocked(context: Context, packageName: String, blocked: Boolean) {
        prefs(context).edit().putBoolean("${packageName}_blocked", blocked).apply()
    }

    fun isAppLockActive(context: Context, packageName: String): Boolean {
        val p = prefs(context)
        return p.getBoolean("${packageName}_enabled", false) ||
            p.getBoolean("${packageName}_blocked", false)
    }

    fun getLockType(context: Context, packageName: String): LockType {
        val name = prefs(context).getString("${packageName}_type", LockType.NONE.name)
            ?: LockType.NONE.name
        return runCatching { LockType.valueOf(name) }.getOrDefault(LockType.NONE)
    }

    fun isBiometricEnabled(context: Context, packageName: String): Boolean =
        prefs(context).getBoolean("${packageName}_biometric", false)
}
