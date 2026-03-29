#!/bin/bash
# Android 11-15 Migration Script for Zebra Enterprise Apps
# Runs each migration phase non-interactively using Claude Code.
# Review changes after each phase with: git diff
#
# Usage:
#   git checkout -b migrate/android-15
#   chmod +x migrate.sh
#   ./migrate.sh

set -e

echo "=== Phase 1: android:exported ==="
claude -p "Scan AndroidManifest.xml and add android:exported to every activity, service, receiver, and provider that has an intent-filter but is missing the attribute. Use false for internal components, true only for components that must accept external intents." --allowedTools Edit,Read,Glob,Grep

echo "=== Phase 2: PendingIntent FLAG_IMMUTABLE ==="
claude -p "Find all PendingIntent.getActivity, getBroadcast, and getService calls missing FLAG_IMMUTABLE and add it. Only use FLAG_MUTABLE where genuinely required." --allowedTools Edit,Read,Glob,Grep

echo "=== Phase 3: Activity Results ==="
claude -p "Replace all startActivityForResult and onActivityResult usage with registerForActivityResult using ActivityResultContracts. Keep existing business logic." --allowedTools Edit,Read,Glob,Grep

echo "=== Phase 4: Permission Results ==="
claude -p "Replace all onRequestPermissionsResult overrides with registerForActivityResult using ActivityResultContracts.RequestPermission or RequestMultiplePermissions." --allowedTools Edit,Read,Glob,Grep

echo "=== Phase 5: Storage paths ==="
claude -p "Find and replace hardcoded external storage paths and Environment.getExternalStorageDirectory() usage. Migrate to getExternalFilesDir() for app-private files and MediaStore for shared media." --allowedTools Edit,Read,Glob,Grep

echo "=== Phase 6: AsyncTask ==="
claude -p "Replace all AsyncTask subclasses with Kotlin coroutines using viewModelScope or lifecycleScope. Move IO work to Dispatchers.IO." --allowedTools Edit,Read,Glob,Grep

echo "=== Phase 7: POST_NOTIFICATIONS ==="
claude -p "Add POST_NOTIFICATIONS permission check before all NotificationManager.notify() calls. Add the permission to AndroidManifest.xml." --allowedTools Edit,Read,Glob,Grep

echo "=== Phase 8: Back navigation ==="
claude -p "Replace all onBackPressed() overrides with OnBackPressedCallback registered via onBackPressedDispatcher.addCallback()." --allowedTools Edit,Read,Glob,Grep

echo "=== Phase 9: Edge-to-edge insets ==="
claude -p "Add WindowInsetsCompat inset handling to all activities so content is not obscured by system bars. Required for targetSdk 35." --allowedTools Edit,Read,Glob,Grep

echo "=== Phase 10: Splash screen ==="
claude -p "Remove custom SplashActivity and replace with androidx.core:core-splashscreen. Add the dependency, update the theme, and call installSplashScreen() in MainActivity." --allowedTools Edit,Read,Glob,Grep

echo "=== Phase 11: DataWedge receiver flag ==="
claude -p "Update all registerReceiver calls for DataWedge scan receivers to pass RECEIVER_NOT_EXPORTED on API 33+ with a Build.VERSION.SDK_INT check." --allowedTools Edit,Read,Glob,Grep

echo "=== Phase 12: Build target ==="
claude -p "Update build.gradle to compileSdk 35, targetSdk 35, minSdk 30. Add any Jetpack dependencies required by the changes made in previous phases." --allowedTools Edit,Read,Glob,Grep

echo ""
echo "=== Migration complete ==="
echo "Review all changes before committing:"
echo "  git diff"
echo "  git diff --stat"
