# CalorieTrack â€” Privacy, Security & Data Ownership

In an era where health applications routinely harvest and sell personal dietary habits to data brokers and advertisers, CalorieTrack is built from the ground up on the principle of **Zero-Knowledge, Local-Only Data Ownership**.

---

## 1. Local-Only Data Architecture
- **Device-Bound Storage**: All databases, preferences, goals, and meal logs reside strictly in the app's private sandbox directory on the Android phone (`/data/data/com.calorietrack.app/databases/`).
- **No Remote Servers**: CalorieTrack does not operate or communicate with any cloud server, backend, or cloud database.
- **Zero Third-Party SDKs**: The application bundle contains **zero tracking libraries**:
  - No Google Firebase Analytics
  - No Facebook Graph SDK
  - No Crashlytics telemetry
  - No advertising networks (AdMob, Unity Ads, etc.)
  - No session recording or heatmaps

---

## 2. No Accounts & No Identifiers
- **Zero Sign-Up**: The user is never prompted to enter an email address, password, phone number, or name.
- **No Advertising IDs**: The app does not query or store the Android Advertising ID (`GAID`), IMEI, MAC address, or hardware serial numbers.
- **Immediate Utility**: The user can open the app for the very first time and log a meal in under 10 seconds without encountering a single registration gate.

---

## 3. Minimal Android Permissions

To maintain trust and security, CalorieTrack requests **almost zero Android operating system permissions**:

| Permission | Status | Rationale |
| :--- | :---: | :--- |
| `android.permission.INTERNET` | âŒ **Excluded in V1** | Not declared in `AndroidManifest.xml`. The app physically cannot send or receive network data. The optional web lookup uses an external browser Intent, which requires no app-level internet permission. |
| `android.permission.ACCESS_FINE_LOCATION` | âŒ **Excluded** | The app never needs to know where the user eats. |
| `android.permission.CAMERA` | âŒ **Excluded in V1** | Deferred until offline barcode scanning is introduced. |
| `android.permission.READ_CONTACTS` | âŒ **Excluded** | Completely unrelated to nutrition tracking. |
| `android.permission.POST_NOTIFICATIONS` | âŒ **Excluded in V1** | No push notification spam. |

---

## 4. Safe Handling of User-Created Data
- **SQL Injection Prevention**: Because CalorieTrack uses Android **Room**, all database queries are compiled into parameterized Prepared Statements. User input in food names, serving descriptions, and quantities can never trigger SQL injection attacks.
- **Input Sanitization**: Numerical fields (calories, protein, carbs, fats) are strictly validated as non-negative finite floating-point numbers to prevent overflow or corrupted application state.

---

## 5. Data Backup & Future Portability
- **Android Auto-Backup Compliance**: By default, standard Android operating system backup (Google Drive device backup encrypted with the user's Google credentials) can back up the local database when the user changes or restores phones.
- **Future Open Export (CSV/JSON)**: In post-V1 releases, a local **"Export My Data"** button will allow users to generate a standard `.csv` or `.json` file saved directly to their phone's Downloads folder, ensuring users never feel locked into the application.

---

## 6. Information That Must Never Be Collected

Even if future versions introduce optional cloud backups or synced accounts, the following categories of sensitive personal data are **strictly prohibited** from ever being collected:
1. Precise GPS or network location coordinates.
2. Device identifiers, phone numbers, or contact address books.
3. Biometric data (fingerprint, face scans).
4. Browsing history or search queries outside the app.
5. Advertising attribution metadata.

---

## 7. Official Nutrition & Medical Disclaimer
CalorieTrack includes the following clear, unambiguous disclaimer within the app's Settings and About screen:

> **Medical Disclaimer**:
> CalorieTrack is designed solely for self-monitoring and educational purposes. It is not a medical device, nor does it provide personalized dietary advice, medical diagnosis, or treatment plans. Nutritional information in the built-in database is compiled from standard public reference catalogues; individual food items, preparation methods, and metabolic responses may vary. Consult a qualified physician, certified dietitian, or healthcare professional before embarking on any significant dietary changes or calorie restriction programs.
