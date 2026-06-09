---
name: Aegis broadcast trigger system
description: Real Android broadcast receiver architecture wired to the automation engine — static receiver, foreground monitor service, accessibility hook.
---

## Architecture

### AegisAutomationEngine (app/engine/)
- Singleton `object` that matches `triggerLabel` against stored automations
- Calls `Settings.System/Global/Secure.putString()` to execute structured actions
- Logs to `AegisActivityLog`, posts notification via `aegis_automation_fired` channel
- `fireTrigger(context, label, detail)` — main entry point
- `fireBatteryTrigger(context, pct)` — called from monitor service (service does dedup)
- Respects global pause state via `aegis_tile_prefs → automations_enabled`

### AegisSystemTriggerReceiver (app/receiver/)
Static manifest receiver for events that survive between app launches:
- `ACTION_POWER_CONNECTED/DISCONNECTED` → "Charger Connected/Disconnected"
- `BluetoothDevice.ACTION_ACL_CONNECTED/DISCONNECTED` → "Bluetooth Connected/Disconnected"
- `PACKAGE_ADDED/REMOVED` (with `<data android:scheme="package"/>` in its own `<intent-filter>`)

**Why:** These are on Android's "implicitly-allowlisted" list and CAN be statically registered. Power/BT and package events must be in separate `<intent-filter>` blocks (package events need data scheme, power/BT must not have it).

### AegisMonitorService (app/service/)
Foreground service (`foregroundServiceType="dataSync"`, `IMPORTANCE_LOW` notification).
Dynamically registers for events that CANNOT be statically declared:
- `ACTION_SCREEN_ON/OFF`, `ACTION_USER_PRESENT` (device unlock)
- `ACTION_BATTERY_CHANGED` — deduped by service's `lastBatteryPct` instance field
- `AudioManager.ACTION_HEADSET_PLUG` → state=1 connected, state=0 disconnected
- `WifiManager.NETWORK_STATE_CHANGED_ACTION` — deduped via `wifiConnectedFired` flag
Uses `RECEIVER_NOT_EXPORTED` on API 33+ (safe for system-protected broadcasts).

Started from:
1. `GetoApplication.onCreate()` — when app launches
2. `BootReceiver.onReceive()` — when device boots

### AppLockService hook (app/lock/)
`TYPE_WINDOW_STATE_CHANGED` events fire "App Launch" trigger when `packageName != lastLaunchPackage`.
Skips systemui, android, and own package. Dedup via `lastLaunchPackage` instance field.

### AegisActionStore (feature/home/)
Public `object` in `feature.home` package. Stores `List<StoredAction>` per automation ID
in SP file `aegis_actions_v1`, key `"actions_{id}"` as `StringSet`.
`StoredAction(label, settingKey, settingType, value)` — created in `AutomationBuilderSheet` save block.
Deleted automatically in `AegisAutomationStore.deleteAutomation()`.

### Visibility changes
- `SavedAutomation`, `AegisAutomationStore`, `LogEntry`, `AegisActivityLog` changed from `internal` to public so the `:app` module can access them.
- `StoredAction`, `AegisActionStore` are public by default (new types).

## Trigger label exact strings
Must match exactly what `SavedAutomation.triggerLabel` stores (from builder):
"Screen On", "Screen Off", "Device Unlock", "Device Lock", "Charger Connected",
"Charger Disconnected", "Battery %", "Wi-Fi Connected", "Wi-Fi Disconnected",
"Bluetooth Connected", "Bluetooth Disconnected", "Headphones Connected",
"Headphones Disconnected", "App Launch", "App Close", "App Install", "App Uninstall"

"App Close", "Device Lock", "NFC Tag Detected", "Volume Changed", "Time Schedule", "Day Schedule"
are NOT yet wired (no Android broadcast equivalent without UsageStatsManager or AlarmManager).
