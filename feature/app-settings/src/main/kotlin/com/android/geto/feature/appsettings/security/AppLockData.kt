package com.android.geto.feature.appsettings.security

enum class LockType(val label: String, val description: String) {
    NONE("None", "No lock"),
    PIN("PIN", "4–8 digit numeric code"),
    PATTERN("Pattern", "Draw a pattern on a 3×3 grid"),
    PASSWORD("Password", "Alphanumeric passphrase"),
}

data class AppLockConfig(
    val packageName: String,
    val lockType: LockType = LockType.NONE,
    val isEnabled: Boolean = false,
    val isBiometricEnabled: Boolean = false,
    val isBlocked: Boolean = false,
    val hasCredential: Boolean = false,
)

sealed interface LockVerifyResult {
    data object Success : LockVerifyResult
    data object Failed : LockVerifyResult
    data object NoCredential : LockVerifyResult
}
