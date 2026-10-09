# Privacy Policy for CalorieTrack

**Effective Date**: October 7, 2026  
**Application Name**: CalorieTrack  
**Developer**: CalorieTrack Team  
**Contact**: support@calorietrack.app *(placeholder for developer contact email)*  

---

## 1. Introduction

CalorieTrack ("the Application") is an offline-first nutrition and calorie tracking tool for Android. We believe your personal health and nutrition records belong exclusively to you. This Privacy Policy explains how information is handled by the Application.

---

## 2. Zero Off-Device Data Collection

**CalorieTrack does NOT collect, transmit, sell, or share any personal or nutritional data with external servers, third parties, or the developer.**

- **No Internet Access**: The Application does not request or include the `android.permission.INTERNET` permission in its Android Manifest. It is technologically incapable of transmitting data over the internet.
- **No User Accounts**: You do not need to create an account, log in, or provide an email address, phone number, or name to use the Application.
- **No Third-Party Analytics**: The Application contains zero third-party software development kits (SDKs), telemetry libraries, crash reporting tools, or analytics trackers.
- **No Advertising**: The Application contains no advertisements and does not collect or access Android Advertising IDs.

---

## 3. Information Stored Locally on Your Device

All information created or managed in CalorieTrack is stored locally in the Application's private sandboxed database on your device. This includes:

- **Meal Logs**: Foods consumed, quantities logged (in grams), meal categories (Breakfast, Lunch, Dinner, Snack), and timestamps.
- **Custom Foods**: Names, serving units, and nutritional content per 100 grams for custom foods you create.
- **Recipes**: Custom recipes, ingredient breakdowns, and cooked weights created using the Recipe Builder.
- **Daily Nutritional Targets**: Calorie and macronutrient goals (protein, carbs, fat) configured in Settings.
- **User Preferences**: Local application state flags (such as onboarding completion).

This information remains entirely within your device's internal storage and is not accessible to other applications.

---

## 4. Backups and Data Portability

### A. User-Initiated Local Backup (JSON Export)
The Application provides an offline backup and restore feature ("Data & Backup" in Settings):
- **User-Initiated**: Backups are created only when you explicitly tap "Export My Data".
- **Destination Control**: You choose where the resulting backup file (`.json`) is saved on your device using Android's system file picker (Storage Access Framework).
- **Importing**: Restoring a backup requires you to explicitly select a valid CalorieTrack backup file from your storage.
- **No Developer Transmission**: The Application does not send your backup to the developer or any dedicated cloud backend. If you choose to save your exported file to a cloud drive (e.g. Google Drive), that transfer is performed directly by Android's Storage Access Framework according to your chosen storage provider's privacy policy.

### B. Android Operating System Backup (Device & Cloud Transfer)
- Android provides native operating system backup and device-to-device migration features (Auto Backup).
- If you have enabled Android System Backup in your device settings (**Android Settings > System > Backup**), the Android operating system may automatically include CalorieTrack's local SQLite database in your encrypted Google account backup to facilitate device restoration or phone upgrades.
- This OS-level backup is managed entirely by Google and Android system services, not by the Application developer. The developer does not receive, operate, or access these backups. You may disable Android system backup at any time in your Android OS device settings.


---

## 5. Data Deletion and Retention

Because all data resides strictly on your device:
- You can delete individual meal entries, custom foods, or recipes at any time within the Application.
- You can permanently erase all data by clearing the Application's storage via **Android Settings > Apps > CalorieTrack > Storage & cache > Clear storage**.
- Uninstalling the Application permanently deletes the local SQLite database and all stored application data.

---

## 6. Permissions

CalorieTrack requests **zero** runtime or dangerous permissions. It does not access your location, camera, microphone, contacts, or external storage without explicit user interaction via the Android Storage Access Framework.

---

## 7. Changes to This Privacy Policy

If the Application's functionality evolves in future updates, this Privacy Policy will be updated accordingly. Any changes will be documented in the repository documentation and accompanying release notes.

---

## 8. Contact Us

If you have questions or concerns regarding this Privacy Policy, please contact:
- **Email**: `privacy@calorietrack.app` *(or developer support contact)*
