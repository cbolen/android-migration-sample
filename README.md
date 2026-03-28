# InventoryApp — Legacy Sample (API 30 / Android 11 era)

## What This App Does

InventoryApp is a Zebra enterprise inventory management application. Warehouse operators use Zebra
mobile computers (TC-series, MC-series) to scan barcodes with the physical imager, look up items in
the local SQLite database, and adjust quantities on the spot. Scan data arrives via DataWedge intent
broadcasts — the app never touches the camera directly for scanning.

Key features:
- Barcode-driven item lookup and creation via DataWedge
- SQLite inventory database with quantity, location, and notes
- CSV export to external storage for downstream ERP import
- Camera capture for item photos attached to inventory records
- Low-stock notifications when quantities fall below a configurable threshold
- DataWedge profile auto-configuration on first launch

## Target Platform

| Property | Value |
|---|---|
| `compileSdkVersion` | 30 (Android 11) |
| `targetSdkVersion` | 30 |
| `minSdkVersion` | 26 (Android 8.0) |
| Language | Kotlin 1.7 |
| Build tools | Gradle 7.4, AGP 7.3 |

This codebase was written in 2020–2021, before Android 12–15 requirements were enforced by the
Play Store and before several deprecated APIs were removed. It runs fine on Android 8–11 Zebra
devices, but will encounter runtime crashes, permission denials, and compatibility warnings on
Android 12 and above.

## Using This App With the Migration Guide

This project is designed as a hands-on exercise companion for the **Android 11 → 15 Migration Guide
for Zebra Enterprise Apps**. The code is realistic production-style Kotlin — not annotated with
warning markers — so you need to identify and fix the issues yourself.

**Workflow:**

1. Open the project in Android Studio.
2. Read through the migration guide sections (Manifest, Storage, Async, Permissions,
   Notifications, Jetpack modernisation).
3. Apply each fix to the corresponding file in this project.
4. Use `MIGRATION-ISSUES.md` as a self-check checklist to confirm you found and fixed every issue.
5. Test on a Zebra device running Android 13 or 14 to verify nothing crashes.

## Self-Check Reference

See **[MIGRATION-ISSUES.md](./MIGRATION-ISSUES.md)** for the complete checklist of every migration
issue embedded in this codebase, grouped by category, with file names, approximate line numbers,
and a one-line description of the correct fix.

## Project Structure

```
app/src/main/
├── AndroidManifest.xml
└── java/com/example/inventoryapp/
    ├── SplashActivity.kt
    ├── MainActivity.kt
    ├── AddItemActivity.kt
    ├── ExportActivity.kt
    ├── SettingsActivity.kt
    ├── InventoryAdapter.kt
    ├── model/
    │   └── InventoryItem.kt
    ├── data/
    │   ├── InventoryDatabase.kt
    │   └── InventoryRepository.kt
    ├── datawedge/
    │   ├── ScanReceiver.kt
    │   └── DataWedgeManager.kt
    └── util/
        ├── NotificationHelper.kt
        └── StorageHelper.kt
```

## AI-Assisted Migration

This project is set up to work directly with the migration guide pack. To automate fixes with your AI tool:

1. Clone the **[Android Migration Guide Pack](https://github.com/zebra-oss/android-migration-guide-pack)**
2. Copy the context file for your AI tool into this project root:
   - Claude Code → `CLAUDE.md`
   - Cursor → `.cursorrules`
   - GitHub Copilot → contents into `.github/copilot-instructions.md`
3. Follow the phase-by-phase prompts in `docs/how-to-use.md` from the guide pack
4. Verify each fix against `MIGRATION-ISSUES.md`

For Claude Code users, the guide pack includes a `migrate.sh` script that runs all 12 migration phases non-interactively using `claude -p`.

## DataWedge Notes

The app configures its own DataWedge profile (`InventoryApp`) on first launch via `SET_CONFIG`.
The profile routes scan results to the broadcast action `com.example.inventoryapp.SCAN_RESULT`.
`DataWedgeManager` handles all scanner lifecycle (enable/disable on resume/pause) and soft-scan
trigger support. No EMDK or Camera API is used for barcode reading.