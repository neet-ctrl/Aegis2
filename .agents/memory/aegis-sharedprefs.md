---
name: Aegis SharedPreferences keys
description: Exact SP file names and keys used by AegisAutomationStore and AegisActivityLog — needed for direct access from feature/settings backup/restore.
---

## AegisAutomationStore
- SP file: `aegis_automations_v1`
- Key: `"automations"` (StringSet)
- Encoding: pipe-separated fields using `\u001E` separator
- Class: `internal object` in `com.android.geto.feature.home` — NOT accessible from feature/settings directly

## AegisActivityLog
- SP file: `aegis_activity_log_v1`
- Key: `"entries"` (StringSet, max 100)
- Encoding: pipe-separated fields using `\u001F` separator
- Class: `internal object` in `com.android.geto.feature.home` — NOT accessible from feature/settings directly

## Backup/Restore approach (feature/settings)
Access SharedPreferences by name directly (same app package), bypassing the internal classes.
Backup JSON format: `{ "version": 1, "timestamp": Long, "automations": JSONArray, "activity_log": JSONArray }`

**Why:** `AegisAutomationStore` and `AegisActivityLog` are `internal` to `feature.home`. The backup lives in `feature.settings` which has no module dep on `feature.home`. Direct SharedPreferences access by file name is the correct pattern.
