# Migration Issues Checklist

This file lists every Android 11 → 15 migration issue intentionally present in the legacy
InventoryApp sample. Use it as a self-check after working through the migration guide. Each item
links to the file and approximate line where the issue appears, and gives a one-line description
of the correct fix.

Check off each item (`- [x]`) once you have found and fixed it.

---

## Manifest

- [ ] **`AndroidManifest.xml`** — `READ_EXTERNAL_STORAGE` declared: replace with
  `READ_MEDIA_IMAGES` / `READ_MEDIA_VIDEO` (API 33+) and remove for API 32 and below where
  scoped storage applies; use `getExternalFilesDir()` instead of public paths so no read
  permission is needed at all.

- [ ] **`AndroidManifest.xml`** — `WRITE_EXTERNAL_STORAGE` declared: this permission is
  ignored on API 30+ for apps targeting API 29+; remove it and use `getExternalFilesDir()` or
  MediaStore for any storage writes.

- [ ] **`AndroidManifest.xml`** — `POST_NOTIFICATIONS` permission is missing entirely: add
  `<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />` and request it at
  runtime before posting any notification on API 33+.

- [ ] **`AndroidManifest.xml`** — `SCHEDULE_EXACT_ALARM` permission is missing: add
  `<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />` and check
  `canScheduleExactAlarms()` before scheduling (see StockAlarmScheduler below).

- [ ] **`AndroidManifest.xml` — `MainActivity`** — has `<intent-filter>` but is missing
  `android:exported`: all activities/services/receivers with intent-filters must declare
  `exported` explicitly on API 31+; add `android:exported="true"` (or `false` for internal-only
  components).

- [ ] **`AndroidManifest.xml` — `AddItemActivity`** — has `<intent-filter>` but is missing
  `android:exported="false"`: same requirement as above; this is an internal-only component.

- [ ] **`AndroidManifest.xml` — `ScanReceiver`** — static `<receiver>` is missing
  `android:exported="true"`: DataWedge sends broadcasts from a separate process, so this
  receiver must be exported; add `android:exported="true"`.

- [ ] **`AndroidManifest.xml` — `LowStockAlertReceiver`** — has `<intent-filter>` but is
  missing `android:exported="false"`: only receives intents from within this app; set
  `android:exported="false"`.

- [ ] **`AndroidManifest.xml` — `StockCheckReceiver`** — has `<intent-filter>` but is missing
  `android:exported="false"`: only receives alarms scheduled by this app; set
  `android:exported="false"`.

---

## Splash Screen / Handler

- [ ] **`SplashActivity.kt` ~L43** — `Handler()` uses the deprecated no-arg constructor
  (removed API 33 behaviour, lint error API 30+): replace with
  `Handler(Looper.getMainLooper()).postDelayed(...)`.

- [ ] **`SplashActivity.kt`** — Custom splash `Activity` with a timed delay is the old pattern:
  from API 31 the system provides a mandatory splash screen. Implement
  `SplashScreen.installSplashScreen(this)` (Jetpack `androidx.core:core-splashscreen`) and
  remove the delay + `finish()` redirect pattern.

---

## Deprecated Activity Result APIs

- [ ] **`MainActivity.kt` ~L55** — `startActivityForResult(intent, EDIT_ITEM_REQUEST)` for
  launching `AddItemActivity`: replace with `ActivityResultContracts.StartActivityForResult`
  registered via `registerForActivityResult`.

- [ ] **`MainActivity.kt` ~L62** — `startActivityForResult(intent, ADD_ITEM_REQUEST)` for FAB
  tap: same fix.

- [ ] **`MainActivity.kt` ~L68-85** — `override fun onActivityResult(requestCode, resultCode,
  data)`: remove this override; handle results in the `ActivityResultCallback` registered with
  `registerForActivityResult`.

- [ ] **`MainActivity.kt` ~L131** — `startActivityForResult(intent, ADD_ITEM_REQUEST)` in
  `handleScannedBarcode`: same fix.

- [ ] **`AddItemActivity.kt` ~L65** — `startActivityForResult(galleryIntent, GALLERY_REQUEST)`:
  replace with `ActivityResultContracts.PickVisualMedia` via `registerForActivityResult`.

- [ ] **`AddItemActivity.kt` ~L108** — `startActivityForResult(cameraIntent, CAMERA_REQUEST)`:
  replace with `ActivityResultContracts.TakePicture` via `registerForActivityResult`.

- [ ] **`AddItemActivity.kt` ~L70-85** — `override fun onActivityResult(...)`: remove; handle
  camera and gallery results in their respective `ActivityResultCallback` lambdas.

---

## Deprecated Permission Result API

- [ ] **`AddItemActivity.kt` ~L73-87** — `override fun onRequestPermissionsResult(requestCode,
  permissions, grantResults)` for `CAMERA`: replace with
  `ActivityResultContracts.RequestPermission` via `registerForActivityResult`;
  `onRequestPermissionsResult` is deprecated from API 33.

- [ ] **`ExportActivity.kt` ~L60-72** — `override fun onRequestPermissionsResult(...)` for
  `WRITE_EXTERNAL_STORAGE`: same fix — use `registerForActivityResult`; also reconsider whether
  the permission is needed at all (see Storage section below).

---

## Back Navigation

- [ ] **`MainActivity.kt` ~L140-150** — `override fun onBackPressed()` contains custom logic
  (confirm dialog): `onBackPressed()` is deprecated from API 33. Replace with
  `onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) { ... })`.

---

## BroadcastReceiver — Context Registration

- [ ] **`MainActivity.kt` ~L126** — `registerReceiver(scanReceiver, filter)` called without
  `RECEIVER_NOT_EXPORTED` (or `RECEIVER_EXPORTED`) flag: on API 34+, dynamically registered
  receivers that do not specify an export flag throw an exception at runtime. Fix: use
  `ContextCompat.registerReceiver(this, scanReceiver, filter,
  ContextCompat.RECEIVER_NOT_EXPORTED)` — DataWedge broadcasts are from a system service but
  targeted at this app, so NOT_EXPORTED is correct.

---

## Storage — Hardcoded Paths

- [ ] **`StorageHelper.kt` ~L22** — `Environment.getExternalStorageDirectory()` in
  `getExportDirectory()`: public external storage is not writable on API 30+ without
  `WRITE_EXTERNAL_STORAGE` (denied for API 29+ targets); use `context.getExternalFilesDir()`
  or `MediaStore.Downloads`.

- [ ] **`StorageHelper.kt` ~L35** — `File("/sdcard/$APP_FOLDER/photos")` in
  `getPhotosDirectory()`: hardcoded `/sdcard/` path; replace with
  `context.getExternalFilesDir(Environment.DIRECTORY_PICTURES)`.

- [ ] **`StorageHelper.kt` ~L48** — `Environment.getExternalStorageDirectory()` in
  `getTempDirectory()`: same issue as `getExportDirectory()`; use
  `context.getExternalFilesDir()` or `context.cacheDir`.

- [ ] **`ExportActivity.kt` ~L94** — `File(Environment.getExternalStorageDirectory(),
  "InventoryApp/exports/...")` in `writeExportFile()`: same public storage path issue; replace
  with `getExternalFilesDir("exports")` or `MediaStore.Downloads`.

- [ ] **`AddItemActivity.kt` ~L104** — `StorageHelper.getPhotoFile()` calls
  `File("/sdcard/...")` internally: same fix — `StorageHelper.getPhotosDirectory()` must be
  updated (see above).

---

## AsyncTask (Removed API 33)

- [ ] **`InventoryRepository.kt` ~L35** — `FetchAllTask : AsyncTask<Void, Void,
  List<InventoryItem>>`: `AsyncTask` was deprecated in API 30 and removed in API 33; replace
  with a coroutine using `Dispatchers.IO` and a `suspend` function or `ViewModel` +
  `viewModelScope`.

- [ ] **`InventoryRepository.kt` ~L42** — `FindByBarcodeTask : AsyncTask<Void, Void,
  InventoryItem?>`: same fix — replace with coroutine.

- [ ] **`InventoryRepository.kt` ~L50** — `InsertItemTask : AsyncTask<Void, Void, Boolean>`:
  replace with coroutine.

- [ ] **`InventoryRepository.kt` ~L57** — `UpdateItemTask : AsyncTask<Void, Void, Boolean>`:
  replace with coroutine.

- [ ] **`InventoryRepository.kt` ~L64** — `CheckLowStockTask : AsyncTask<Void, Void,
  List<InventoryItem>>`: replace with coroutine.

---

## Notifications — PendingIntent Flags

- [ ] **`NotificationHelper.kt` ~L46** —
  `PendingIntent.getBroadcast(context, 0, trampolineIntent, 0)`: flags argument `0` must include
  `PendingIntent.FLAG_IMMUTABLE` (or `FLAG_MUTABLE` if the intent must be mutated by the system);
  from API 31 this causes a crash. Fix: `PendingIntent.FLAG_IMMUTABLE`.

- [ ] **`NotificationHelper.kt` ~L64** —
  `PendingIntent.getActivity(context, 1, mainIntent, 0)` in `showExportCompleteNotification`:
  same missing `FLAG_IMMUTABLE`.

- [ ] **`StockAlarmScheduler.kt` ~L51** —
  `PendingIntent.getBroadcast(context, REQUEST_CODE, intent, 0)`: same missing `FLAG_IMMUTABLE`.

---

## Notifications — Runtime Permission

- [ ] **`NotificationHelper.kt` ~L56 / ~L72** — Notifications posted without checking the
  `POST_NOTIFICATIONS` runtime permission: on API 33+, `notify()` is silently ignored unless
  the user has granted `POST_NOTIFICATIONS`. Add a runtime permission check before posting and
  declare the permission in the manifest (see Manifest section above).

---

## Notifications — Trampoline (New — API 31)

- [ ] **`NotificationHelper.kt` ~L36-47** — Low-stock notification uses a `PendingIntent`
  targeting `LowStockAlertReceiver`, which then calls `startActivity(MainActivity)`. This is a
  notification trampoline — starting an Activity from a `BroadcastReceiver` in response to a
  notification tap is blocked on API 31+ for apps targeting API 31+.
  Fix: remove `LowStockAlertReceiver` and attach a `PendingIntent.getActivity()` targeting
  `MainActivity` directly as the notification's `setContentIntent`.

---

## Cryptography — BouncyCastle Removed (New — API 31)

- [ ] **`CryptoHelper.kt` ~L42 / ~L56** — `Cipher.getInstance(TRANSFORMATION, "BC")` explicitly
  requests the BouncyCastle provider. BouncyCastle implementations of many cryptographic
  algorithms were removed in Android 12 (API 31) — this throws `NoSuchProviderException` at
  runtime on API 31+.
  Fix: remove the provider argument. Use `Cipher.getInstance("AES/CBC/PKCS5Padding")` — the
  default provider (Conscrypt) handles this algorithm correctly on all Android versions.

---

## Exact Alarm Permission (New — API 31)

- [ ] **`StockAlarmScheduler.kt` ~L57** — `alarmManager.setExactAndAllowWhileIdle(...)` called
  without a `canScheduleExactAlarms()` check. On API 31+, apps must hold
  `SCHEDULE_EXACT_ALARM` and verify it is granted before scheduling exact alarms — the call
  throws `SecurityException` without the permission.
  Fix:
  ```kotlin
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
      startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM))
      return
  }
  ```
  Also add `<uses-permission android:name="android.permission.SCHEDULE_EXACT_ALARM" />` to
  the manifest. On API 14+, re-check `canScheduleExactAlarms()` in `onResume` — the permission
  is silently revoked on app update.

---

## Edge-to-Edge / WindowInsets

- [ ] **`MainActivity.kt`** — No `WindowInsetsCompat` handling: from API 35 (Android 15),
  edge-to-edge is enforced and system bars overlap app content. Add
  `WindowCompat.setDecorFitsSystemWindows(window, false)` and apply
  `ViewCompat.setOnApplyWindowInsetsListener` to the root view so content is not clipped by
  the navigation bar.

- [ ] **`AddItemActivity.kt`** — Same missing `WindowInsetsCompat` handling as `MainActivity`.

- [ ] **`ExportActivity.kt`** — Same missing `WindowInsetsCompat` handling as `MainActivity`.

---

## DataWedge — Correct Patterns (No Issues)

The following DataWedge patterns are implemented correctly and do **not** need to be changed:

- `ScanReceiver.kt` — reads `com.symbol.datawedge.data_string` and
  `com.symbol.datawedge.label_type` from the broadcast intent. Correct.
- `DataWedgeManager.kt` — uses `SET_CONFIG` Bundle API to create the profile, configures the
  Intent output plugin with the app-specific action, disables Keystroke output. Correct.
- `DataWedgeManager.kt` — `SWITCH_TO_PROFILE`, `SOFT_SCAN_TRIGGER`, `SCANNER_INPUT_PLUGIN`
  enable/disable follow the DataWedge Intent API spec. Correct.
- Scanner lifecycle tied to `onResume` / `onPause` in `MainActivity`. Correct.

---

## Summary Count

| Category | Issues |
|---|---|
| Manifest | 9 |
| Splash / Handler | 2 |
| Deprecated activity result | 7 |
| Deprecated permission result | 2 |
| Back navigation | 1 |
| BroadcastReceiver export flag | 1 |
| Storage / hardcoded paths | 5 |
| AsyncTask removal | 5 |
| PendingIntent flags | 3 |
| POST_NOTIFICATIONS runtime check | 1 |
| Notification trampoline | 1 |
| BouncyCastle removed | 1 |
| Exact alarm permission | 1 |
| Edge-to-edge insets | 3 |
| **Total** | **42** |
