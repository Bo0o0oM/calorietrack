# CalorieTrack â€” Deployment & Release Plan

This document details the release pipeline for CalorieTrack, transitioning from automated development artifact generation to eventual Google Play Store distribution.

---

## 1. Development & Testing Distribution (Current Workflow)

To keep PC hardware load at zero and eliminate physical cable constraints, our development distribution workflow is fully automated through GitHub:

```
[ Developer Commits Code ]
           â”‚
           â–¼
[ Push to GitHub (main branch) ]
           â”‚
           â–¼
[ GitHub Actions Cloud Runner (Ubuntu) ]
   â”œâ”€â”€ Validates Gradle wrapper
   â”œâ”€â”€ Runs JVM unit tests
   â””â”€â”€ Assembles app-debug.apk
           â”‚
           â–¼
[ GitHub Actions Artifact (calorietrack-debug-apk) ]
           â”‚
           â–¼
[ Developer Downloads APK to Phone via Browser & Tests ]
```

### Benefits:
- **Zero Local RAM/CPU Impact**: All compilation happens in GitHub's cloud.
- **Repeatable Testing**: Every APK downloaded matches an exact Git commit hash.
- **Fast Turnaround**: Debug builds compile and publish in ~2â€“4 minutes.

---

## 2. Pre-Release Quality Checklist (Prior to Play Store)

Before promoting CalorieTrack to an official public release:
1. **Target SDK Compliance**: Must target modern Android (API 36+) to meet Google Play submission requirements.
2. **Min SDK Support**: Tested on Android 8.0 (API 26) through Android 16 (API 36).
3. **Airplane Mode Verification**: Full test pass executed with Wi-Fi and Cellular turned off.
4. **Clean Asset Bundling**: Verify that the embedded seed SQLite database is intact and uncorrupted.
5. **No Prohibited Permissions**: Verify that no unnecessary permissions (Camera, Location, Contacts, Internet) are declared.

---

## 3. Google Play Store Release Preparation (V1 Launch)

When ready for public app store release:
- **Android App Bundle (AAB)**: Run `./gradlew bundleRelease` in GitHub Actions. Google Play generates optimized device-specific APKs from the `.aab` file.
- **App Signing**: Use Android Keystore and GitHub Actions Secrets to sign the release bundle securely in CI without committing keystore files to Git.
- **Listing Assets**:
  - Simple, clean screenshots showing the Dashboard, Food Search, and Custom Food screens.
  - Transparent description highlighting: *"100% Offline, No Accounts, No Subscriptions, Total Privacy."*
