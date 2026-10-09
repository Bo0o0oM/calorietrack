# Google Play Data Safety Notes: CalorieTrack

**Application Name**: CalorieTrack  
**Application ID**: `com.calorietrack.app`  
**Version**: 1.0.0 (`versionCode 1`)  
**Target SDK**: Android 16 (API 36)  
**Document Status**: Prepared for Google Play Console Submission  

---

## 1. Overview & Architectural Factsheet

CalorieTrack is an offline-first, local-only nutrition tracking application built on Android Jetpack Compose and Room SQLite.

Key architectural boundaries:
- **No Internet Permission**: The app manifest does NOT declare `android.permission.INTERNET`. The app cannot initiate or receive any network requests.
- **No Third-Party SDKs**: Contains zero advertising SDKs, tracking libraries, analytics frameworks (e.g. Firebase, Mixpanel), or crash reporting daemons (e.g. Sentry, Crashlytics).
- **No Accounts / Authentication**: Does not require or support user accounts, email sign-up, phone numbers, or passwords.
- **Local SQLite Sandboxed Storage**: All meal records, custom foods, recipes, and daily targets reside solely in the application's private SQLite database on the user's device.
- **User-Controlled Local Backups**: Backup export and restore operations use the Android Storage Access Framework (`Intent.ACTION_CREATE_DOCUMENT` / `Intent.ACTION_OPEN_DOCUMENT`). Backups are saved directly to a file destination selected by the user.
- **Android OS System Backup**: Standard Android Auto Backup (`allowBackup="true"`) is supported at the OS level for device-to-device migration and Google Drive device backup according to the user's Android system settings. This is managed by the Android operating system and Google Play Services, not by a developer backend.


---

## 2. Google Play Data Safety Form Responses

When completing the **Data safety** questionnaire in the Google Play Console, use the following responses based strictly on actual code behavior:

### Question 1: Data Collection and Security
- **Does your app collect or share any of the required user data types?**  
  **Answer: No.**  
  *Rationale*: Under Google Play's definitions, "collection" means data is transmitted off the device. Since CalorieTrack stores all data on-device and has no internet access, no data is collected by the developer.

- **Is all of the user data collected by your app encrypted in transit?**  
  **Answer: Not applicable** (No data is transmitted off the device).

- **Do you provide a way for users to request that their data be deleted?**  
  **Answer: Yes.**  
  *Mechanism*: Users can delete individual meal entries, custom foods, and recipes directly within the app UI at any time. Alternatively, users can clear app data in Android System Settings or uninstall the app to permanently delete all data.

---

## 3. Data Classification & Local Storage Details

Although not collected or transmitted off-device, the following data is processed and stored locally:

| Data Category | Data Elements | Storage Mechanism | Transmission |
| :--- | :--- | :--- | :--- |
| **Health & Fitness** | Daily calorie intake, protein, carbohydrate, and fat consumption, daily nutrition targets | Local Room SQLite database (`meal_entries`, `daily_goals`) | **None** (On-device only) |
| **User Content** | Custom food names, serving sizes, custom recipes, ingredient lists | Local Room SQLite database (`foods`, `recipes`, `recipe_ingredients`) | **None** (On-device only) |
| **App Preferences** | Onboarding completion flag | Android `SharedPreferences` (`calorietrack_user_prefs`) | **None** (On-device only) |
| **User Backups** | JSON export containing custom foods, recipes, meal history, and daily goals | Saved to local storage via Android Storage Access Framework | **None** (Transferred only to user-chosen file) |

---

## 4. Permissions Audit

The application declares **zero** dangerous, signature, or network permissions:

| Permission | Manifest Presence | Purpose |
| :--- | :---: | :--- |
| `android.permission.INTERNET` | **ABSENT** | App operates strictly offline. |
| `android.permission.ACCESS_NETWORK_STATE` | **ABSENT** | No network state inspection needed. |
| `android.permission.READ_EXTERNAL_STORAGE` | **ABSENT** | Storage Access Framework used for exports/imports. |
| `android.permission.WRITE_EXTERNAL_STORAGE` | **ABSENT** | Storage Access Framework used for exports/imports. |
| `android.permission.POST_NOTIFICATIONS` | **ABSENT** | App sends no notifications. |
| Location / Camera / Contacts | **ABSENT** | Not utilized. |
