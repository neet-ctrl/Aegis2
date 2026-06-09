---
name: Aegis accessibility watchdog
description: How the accessibility service heartbeat and watchdog are implemented, cross-module boundary decisions.
---

## Heartbeat (AppLockService → app module)
- `AppLockService.onServiceConnected()` writes `System.currentTimeMillis()` to SharedPreferences.
- A `Handler(Looper.getMainLooper())` re-writes the heartbeat every 60 seconds even without events.
- SharedPreferences file: `aegis_accessibility_watchdog`, key: `heartbeat_ms`.
- On `onUnbind` and `onDestroy`, the handler's callbacks are removed cleanly.

## Watchdog (AegisMonitorService → app module)
- `AegisMonitorService` runs a `Handler.postDelayed` every 5 minutes.
- Calls `AccessibilityHealthMonitor.getHealth(context)` (in `app/watchdog/`).
- If status is MALFUNCTIONING or ENABLED_NOT_RUNNING → shows a high-priority notification with a deeplink to Accessibility Settings.
- Notification channel: `aegis_accessibility_watchdog`, notification ID 7002.

## AccessibilityHealthMonitor (app module only)
- Lives in `app/src/main/kotlin/com/android/geto/watchdog/AccessibilityHealthMonitor.kt`.
- **NOT shared with feature modules** — feature modules inline the same logic directly (5-min stale threshold, same SP keys).
- **Why**: `feature/home` cannot depend on `:app`. Duplicating the 3-liner inline check is cleaner than moving to a shared library module.

## DashboardScreen (feature/home module)
- Inlines all health-check logic as private functions (`getA11yHealth`, `A11yHealth`, `A11yStatus`).
- `AccessibilityHealthCard` composable uses `DisposableEffect(lifecycleOwner)` + `ON_RESUME` to re-check status when user returns from Settings.
- Card is hidden when service is OK AND battery optimization is exempt (no noise when healthy).

## Battery optimization fix
- Added `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` permission to AndroidManifest.
- Added `IGNORE_BATTERY_OPTIMIZATIONS` as a REQUIRED PermEntry in SettingsScreen Permission Center.
- Uses `Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` with `usePackageUri = true` to open the system exemption dialog directly.

## PACKAGE_REPLACED receiver
- `PackageReplacedReceiver` listens for `MY_PACKAGE_REPLACED` (exported=false, no data scheme needed).
- On update: re-starts `AegisMonitorService` and posts a high-priority notification reminding user to re-enable the accessibility service.
- Android always disables AccessibilityService on app update — this is why the notification is needed.
