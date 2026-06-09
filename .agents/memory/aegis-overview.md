---
name: Aegis project overview
description: Key decisions, architecture, and spec compliance status for the Aegis Android app (multi-module Kotlin/Compose, package com.android.geto).
---

## What is Aegis
Multi-module Kotlin/Compose Android app rebranded from Geto. Package: `com.android.geto`. Modules: app/, feature/app-settings/, feature/home/, feature/settings/, design-system/, domain/, data/, framework/. No AI, no cloud, no root (Shizuku/AShell U only). GitHub Actions handles builds — no local build needed.

## Architecture
- Navigation: 5 bottom-nav destinations (Dashboard, Apps, Automations, Activity, Settings)
- Design tokens: design-system module with GetoIcons.kt, Material 3 Expressive + AMOLED theme
- App lock: AppLockService (AccessibilityService) + AppLockActivity (singleTask/showWhenLocked) + AppLockManager (SHA-256+salt SharedPrefs "aegis_app_lock_v1")

## AppSettingsScreen — 5 tabs via ScrollableTabRow
1. Rules — existing CRUD
2. Details — PackageInfo, RunningStateCard, AppOpsCard (11 ops via checkOpNoThrow), Manifest summary
3. Controls — 5 categories × multiple entries with Android Settings keys (SYSTEM/SECURE/GLOBAL)
4. Security — PIN/Pattern/Password + fingerprint biometric + block toggle
5. Detect — EnvironmentDetectionTab (4 categories: Developer, Mock & Debug, System State, Install Source)

## AutomationBuilderSheet — 5-step ModalBottomSheet wizard
Step 1: Trigger (21 trigger chips in 6 categories), Step 2: Conditions (AND/OR logic, nested condition rows), Step 3: Actions (THEN — 13 action chips + value fields), Step 4: Delay (seconds), Step 5: Name + Hidden Vault toggle → saves to SharedPrefs.

## DashboardScreen
Sections: HeroBanner → StatusGrid (6 cards) → AshellCommandsPanel (16 commands, collapsible, copy buttons) → StatisticsSection (4 stats with LinearProgressIndicator) → RecentActivitySection → QuickActionsSection.

## AShell U commands (16 total)
Steps 1-3 REQUIRED (WRITE_SECURE_SETTINGS, DUMP, WRITE_SETTINGS). Steps 4-16 optional. All shown in collapsible tray in both Dashboard and Settings screens.

## Quick Settings Tiles (API 24+)
- AegisDashboardTileService → opens MainActivity via PendingIntent (API 34 compat)
- AegisAutomationTileService → toggle automations on/off, persists in SharedPrefs "aegis_tile_prefs"
Both registered in AndroidManifest with BIND_QUICK_SETTINGS_TILE permission.

## Widget
- AegisWidgetProvider extends AppWidgetProvider → layout: widget_aegis.xml, metadata: aegis_widget_info.xml
- Background: widget_background.xml drawable (dark rounded rectangle)
- Shows: Automations status, Rules count, Last trigger
- Registered in AndroidManifest as <receiver> with APPWIDGET_UPDATE intent

## GetoIcons additions (vs original)
Added: Memory, Dns, VpnKey, NetworkCheck, Widgets, Visibility, VisibilityOff, Analytics, Policy, DeveloperMode, Usb, BugReport, Layers, Store, Timer, AccessibilityNew, BrightnessHigh, ScreenRotation, Fullscreen, BatteryFull.

**Why:** Spec requires display/network/device controls + environment detection + tiles/widget UI.

## Hidden Vault
In AutomationsScreen: HiddenVaultSection collapsible card. Unlock button (biometric UI only). When unlocked shows empty state. Hidden flag on automations set in AutomationBuilderSheet step 5.

## Per-app Controls (AppControlsTab)
Display (10): brightness, auto-brightness, screen timeout, font scale, rotation, accelerometer, refresh rate, DPI, force fullscreen, immersive mode policy.
Audio (6): media/notification/alarm/ring volume, vibrate, DND.
Network (7): WiFi, Bluetooth, airplane mode, mobile data, VPN lockdown, DNS mode, metered override.
Device State (8): battery saver, adaptive battery, performance mode, wake lock, pointer speed, haptic, sound effects, screen lock timeout.
Privacy (4): location mode, install unknown, developer options, USB debugging.
