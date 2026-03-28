# Migration Issues Checklist

This file lists every Android 11 → 15 migration issue intentionally present in the legacy
InventoryApp sample. Use it as a self-check after working through the migration guide. Each item
links to the file and approximate line where the issue appears, and gives a one-line description
of the correct fix.

Check off each item (`- [x]`) once you have found and fixed it.

---

## Manifest

- [ ] **`AndroidManifest.xml` ~L6** — `READ_EXTERNAL_STORAGE` declared: replace with
  `READ_MEDIA_IMAGES` / `READ_MEDIA_VIDEO` (API 33+) and remove for API 32 and below where
  scoped storage applies; use `getExternalFilesDir()` instead of public paths so no read
  permission is needed at all.

- [ ] **`AndroidManifest.xml` ~L7** — `WRITE_EXTERNAL_STORAGE` declared: this permission is
  ignored on API 30+ for apps targeting API 29+; remove it and use `MediaStore` or SAF for any
  shared-storage writes.

- [ ] **`AndroidManifest.xml`** — `POST_NOTIFICATIONS` permission is missing entirely: add
  `<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />` and request it at
  runtime before posting any notification on API 33+.

- [ ] **`AndroidManifest.xml` ~L22** — `MainActivity` has an `<intent-filter>` but is missing
  `android:exported="true"`: all activities/services/receivers with intent-filters must declare
  `exported` explicitly (enforced from API 31).

- [ ] **`AndroidManifest.xml` ~L28** — `AddItemActivity` has an `<intent-filter>` but is missing
  `android:exported="false"` (or `true` if intentionally public): same exported requirement as
  above.

- [ ] **`AndroidManifest.xml` ~L38** — The static `<receiver>` for `ScanReceiver` is missing
  `android:exported="true"`: DataWedge sends broadcasts from a separate process, so the receiver
  must be exported; add `android:exported="true"` to the declaration.

---

## Splash Screen / Handler

- [ ] **`SplashActivity.kt` ~L28** — `Handler().postDelayed(...)` uses the deprecated no-arg
  `Handler()` constructor (removed API 33 behaviour, lint error API 30+): replace with
  `Handler(Looper.getMainLooper()).postDelayed(...)`.

- [ ] **`SplashActivity.kt` ~L22-34** — Custom splash `Activity` with a timed delay is the old
  pattern: from API 31 the system provides a mandatory splash screen. Implement
  `SplashScreen.installSplashScreen(this)` (Jetpack `androidx.core:core-splashscreen`) and
  remove the delay + `finish()` redirect pattern, or accept the behaviour on pre-31 only.

---

## Deprecated Activity Result APIs

- [ ] **`MainActivity.kt` ~L44** — `startActivityForResult(intent, EDIT_ITEM_REQUEST)` for
  launching `AddItemActivity`: replace with `ActivityResultContracts.StartActivityForResult`
  registered via `registerForActivityResult`.

- [ ] **`MainActivity.kt` ~L49** — `startActivityForResult(intent, ADD_ITEM_REQUEST)` for FAB
  tap: same fix as above — move to `registerForActivityResult`.

- [ ] **`MainActivity.kt` ~L80-100** — `override fun onActivityResult(requestCode, resultCode,
  data)`: remove this entire override; handle results in the `ActivityResultCallback` registered
  with `registerForActivityResult`.

- [ ] **`AddItemActivity.kt` ~L68** — `startActivityForResult(cameraIntent, CAMERA_REQUEST)`:
  replace with `ActivityResultContracts.TakePicture` (or `StartActivityForResult`) via
  `registerForActivityResult`.

- [ ] **`AddItemActivity.kt` ~L76** — `startActivityForResult(galleryIntent, GALLERY_REQUEST)`:
  replace with `ActivityResultContracts.GetContent` via `registerForActivityResult`.

- [ ] **`AddItemActivity.kt` ~L84-100** — `override fun onActivityResult(requestCode,
  resultCode, data)`: remove; consolidate camera and gallery callbacks into their respective
  `ActivityResultCallback` lambdas.

---

## Deprecated Permission Result API

- [ ] **`AddItemActivity.kt` ~L78-86** — `override fun onRequestPermissionsResult(requestCode,
  permissions, grantResults)`: replace with `ActivityResultContracts.RequestPermission` (or
  `RequestMultiplePermissions`) via `registerForActivityResult`; `onRequestPermissionsResult` is
  deprecated from API 33.

- [ ] **`ExportActivity.kt` ~L57-67** — `override fun onRequestPermissionsResult(...)` for
  `WRITE_EXTERNAL_STORAGE`: same fix — use `registerForActivityResult`; also reconsider whether
  the permission is needed at all (see Storage section below).

---

## Back Navigation

- [ ] **`MainActivity.kt` ~L103-111** — `override fun onBackPressed()` contains custom logic
  (confirm dialog): `onBackPressed()` is deprecated from API 33. Replace with
  `onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) { ... })`.

---

## BroadcastReceiver — Context Registration

- [ ] **`MainActivity.kt` ~L58-62** — `registerReceiver(scanReceiver, filter)` called without
  `RECEIVER_NOT_EXPORTED` (or `RECEIVER_EXPORTED`) flag: from API 34 (Android 14), dynamically
  registered receivers that do not specify an export flag cause a crash. Add
  `ContextCompat.registerReceiver(this, scanReceiver, filter,
  ContextCompat.RECEIVER_NOT_EXPORTED)` since this receiver is only for local DataWedge
  broadcasts to this app.

---

## Storage — Hardcoded Paths

- [ ] **`AddItemActivity.kt` ~L65** — `File("/sdcard/InventoryApp/photos/$barcode.jpg")`:
  hardcoded `/sdcard/` path; replace with `getExternalFilesDir(Environment.DIRECTORY_PICTURES)`
  which requires no permission and is not accessible to other apps (or use `FileProvider` +
  `MediaStore` if the photos need to be shared).

- [ ] **`ExportActivity.kt` ~L71** —
  `File(Environment.getExternalStorageDirectory(), "InventoryApp/exports/...")`: public
  external storage path; replace with `getExternalFilesDir("exports")` for private app-specific
  storage, or use `MediaStore.Downloads` with a `ContentValues` insert if the file must appear
  in the shared Downloads folder.

- [ ] **`StorageHelper.kt` ~L27** — `File("/sdcard/$APP_FOLDER/photos")` in `getPhotosDirectory`:
  same hardcoded `/sdcard/` path; same fix as above.

- [ ] **`StorageHelper.kt` ~L17** — `Environment.getExternalStorageDirectory()` in
  `getExportDirectory()`: this path is no longer writable without `WRITE_EXTERNAL_STORAGE`
  (denied on API 30+); use `context.getExternalFilesDir()` or `MediaStore`.

- [ ] **`StorageHelper.kt` ~L35** — `Environment.getExternalStorageDirectory()` in
  `getTempDirectory()`: same as above.

---

## AsyncTask (Removed API 33)

- [ ] **`InventoryRepository.kt` ~L38** — `FetchAllTask : AsyncTask<Void, Void,
  List<InventoryItem>>`: `AsyncTask` was deprecated in API 30 and removed in API 33; replace
  with a coroutine using `Dispatchers.IO` and a `suspend` function or `ViewModel` +
  `viewModelScope`.

- [ ] **`InventoryRepository.kt` ~L53** — `FindByBarcodeTask : AsyncTask<Void, Void,
  InventoryItem?>`: same fix — replace with coroutine.

- [ ] **`InventoryRepository.kt` ~L68** — `InsertItemTask : AsyncTask<InventoryItem, Void,
  Boolean>`: replace with coroutine.

- [ ] **`InventoryRepository.kt` ~L84** — `UpdateItemTask : AsyncTask<InventoryItem, Void,
  Boolean>`: replace with coroutine.

- [ ] **`InventoryRepository.kt` ~L100** — `CheckLowStockTask : AsyncTask<Void, Void,
  List<InventoryItem>>`: replace with coroutine.

---

## Notifications — PendingIntent Flags

- [ ] **`NotificationHelper.kt` ~L47** —
  `PendingIntent.getActivity(context, 0, mainIntent, 0)`: the flags argument `0` must include
  `PendingIntent.FLAG_IMMUTABLE` (or `FLAG_MUTABLE` if the intent must be mutated by the system);
  from API 31 this causes a crash. Fix: `PendingIntent.FLAG_UPDATE_CURRENT or
  PendingIntent.FLAG_IMMUTABLE`.

- [ ] **`NotificationHelper.kt` ~L61** —
  `PendingIntent.getActivity(context, 1, mainIntent, 0)` in `showExportCompleteNotification`:
  same missing `FLAG_IMMUTABLE` as above.

---

## Notifications — Runtime Permission

- [ ] **`NotificationHelper.kt` ~L44 / `InventoryRepository.kt` ~L14`** — Notifications are
  posted without checking the `POST_NOTIFICATIONS` runtime permission: on API 33+ the
  `NotificationManager.notify()` call is silently ignored unless the user has granted
  `POST_NOTIFICATIONS`. Add a runtime permission check (or request via
  `ActivityResultContracts.RequestPermission`) before posting, and declare the permission in the
  manifest (see Manifest section above).

---

## Edge-to-Edge / WindowInsets

- [ ] **`MainActivity.kt`** — No `WindowInsetsCompat` handling: from API 35 (Android 15),
  edge-to-edge is enforced and system bars overlap app content. Add
  `WindowCompat.setDecorFitsSystemWindows(window, false)` and apply
  `ViewCompat.setOnApplyWindowInsetsListener` to the root view (or RecyclerView padding) so
  content is not clipped by the navigation bar.

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
| Manifest | 6 |
| Splash / Handler | 2 |
| Deprecated activity result | 6 |
| Deprecated permission result | 2 |
| Back navigation | 1 |
| BroadcastReceiver export flag | 1 |
| Storage / hardcoded paths | 4 |
| AsyncTask removal | 5 |
| PendingIntent flags | 2 |
| POST_NOTIFICATIONS runtime check | 1 |
| Edge-to-edge insets | 1 |
| **Total** | **31** |