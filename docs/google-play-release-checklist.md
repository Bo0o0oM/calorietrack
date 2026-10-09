# Google Play Release Checklist: CalorieTrack

**Application Name**: CalorieTrack  
**Application ID**: `com.calorietrack.app`  
**Current Milestone**: Milestone 3A (Release Readiness & Play Store Preparation)  
**Status**: Ready for Closed Testing Phase  

---

## 1. Technical & Build Readiness

- [x] **Target API Level**: Android 16 (API 36) configured in `compileSdk` and `targetSdk`. Meets Google Play's requirement for new submissions.
- [x] **Minimum SDK**: Android 8.0 (API 26), ensuring compatibility across >95% of active Android devices.
- [x] **Application Format**: Android App Bundle (`app-release.aab`) generated via `./gradlew bundleRelease`.
- [x] **Version Code & Name**: `versionCode = 1`, `versionName = "1.0.0"`.
- [x] **Architecture**: 100% offline-first. Zero network permissions (`android.permission.INTERNET` absent).
- [x] **Security Audit**: No exported components other than `MainActivity` (with `MAIN` / `LAUNCHER` intent filter). No cleartext traffic, no hardcoded API keys or credentials.
- [x] **Android 12+ Backup Support**: `dataExtractionRules` and `fullBackupContent` linked in manifest.
- [x] **Adaptive Launcher Icon**: Custom CalorieTrack branding with adaptive vector background and foreground assets.

---

## 2. Release Signing & Key Management Strategy

> [!CRITICAL]
> Never commit keystore files, passwords, or credentials into source control. `.gitignore` strictly ignores `*.jks`, `*.keystore`, and `keystore.properties`.

### Local Signing Setup (Optional / Developer Machine)
To sign release bundles locally without checking secrets into Git:
1. Generate a release keystore using Java `keytool`:
   ```bash
   keytool -genkey -v -keystore release.keystore -alias calorietrack-key -keyalg RSA -keysize 2048 -validity 10000
   ```
2. Store `release.keystore` in a secure location outside the repository (e.g., `~/.android/calorietrack/`).
3. Set environment variables prior to building:
   ```bash
   export CALORIETRACK_KEYSTORE_FILE="/path/to/release.keystore"
   export CALORIETRACK_KEYSTORE_PASSWORD="your-keystore-password"
   export CALORIETRACK_KEY_ALIAS="calorietrack-key"
   export CALORIETRACK_KEY_PASSWORD="your-key-password"
   ```

### CI/CD Signing Setup (GitHub Actions)
For automated release builds via `.github/workflows/android-release.yml`:
1. Encode keystore to base64:
   ```bash
   base64 -w 0 release.keystore > keystore.base64
   ```
2. Add the following repository secrets under **GitHub Repository > Settings > Secrets and variables > Actions**:
   - `CALORIETRACK_KEYSTORE_BASE64`: Contents of `keystore.base64`
   - `CALORIETRACK_KEYSTORE_PASSWORD`: Keystore password
   - `CALORIETRACK_KEY_ALIAS`: Key alias name
   - `CALORIETRACK_KEY_PASSWORD`: Key password
3. The workflow decodes the keystore to a temporary runner path, builds the signed `.aab`, securely wipes the keystore file, and uploads the artifact.

---

## 3. Store Listing & Metadata Checklist

- [ ] **App Name**: CalorieTrack (max 30 characters)
- [ ] **Short Description**: "Fast, private, and offline-first calorie and nutrition tracker." (max 80 characters)
- [ ] **Full Description**: Comprehensive description detailing the 500-food catalogue, exact gram-based logging, custom recipe builder, offline privacy, and local backup/restore capabilities (max 4,000 characters).
- [ ] **App Icon**: 512 × 512 px PNG (32-bit color, up to 1 MB).
- [ ] **Feature Graphic**: 1024 × 500 px JPEG or 24-bit PNG.
- [ ] **Screenshots**:
  - Phone: Minimum 2 screenshots, 16:9 or 9:16 aspect ratio (recommended: Dashboard, Food Search, Recipe Builder, Settings & Goals).
  - Tablet (7-inch and 10-inch): Optional for MVP, recommended for tablet readiness.
- [ ] **App Category**: Health & Fitness.
- [ ] **Contact Details**: Developer support email address.

---

## 4. Google Play Policy & Declarations

- [ ] **Privacy Policy**: Publicly hosted URL pointing to the text in [`docs/privacy-policy-draft.md`](file:///d:/AntigravityProjects/CalorieTrack/docs/privacy-policy-draft.md) (e.g. via GitHub Pages or simple static host).
- [ ] **Data Safety Form**: Completed per [`docs/google-play-data-safety-notes.md`](file:///d:/AntigravityProjects/CalorieTrack/docs/google-play-data-safety-notes.md) (declaring NO data collected, NO data shared).
- [ ] **Ads Declaration**: Select **No, my app does not contain ads**.
- [ ] **App Access**: Select **All functionality is available without special access** (no logins or credentials needed).
- [ ] **Content Rating Questionnaire (IARC)**: Complete the questionnaire (Food & Nutrition tool; no objectionable content $\rightarrow$ PEGI 3 / Everyone).
- [ ] **Target Audience & Content**: Target age 13 and older (not directed primarily to children under 13).
- [ ] **Financial Features**: Select **None** (no in-app purchases or subscriptions in MVP).
- [ ] **Government Apps**: Select **No**.
- [ ] **Health & Medical**: Select **Nutrition & fitness tracking**. Acknowledge standard medical disclaimer (app provides nutritional tracking, not clinical medical advice).

---

## 5. Google Play Closed Testing Requirements (Mandatory for Personal Accounts)

> [!IMPORTANT]
> Google Play policy for personal developer accounts created after November 13, 2023 requires:
> **At least 12 testers must be opted in continuously for at least 14 days in a closed test before you can apply for production access.**

### Closed Testing Execution Plan
1. **Create Closed Testing Track**:
   - In Google Play Console, navigate to **Testing > Closed testing**.
   - Create a new closed track (e.g., "Closed Alpha / Beta").
2. **Recruit 12+ Testers**:
   - Create an email list of at least 12 testers (recommended: 15–20 testers to maintain continuous opt-in if someone leaves).
3. **Upload Release AAB**:
   - Upload `app-release.aab` to the closed test track.
4. **Send Opt-In Links**:
   - Provide testers with the web or Android opt-in URL generated by Play Console.
   - Confirm all 12+ testers have accepted the invitation and installed the build.
5. **Maintain 14 Continuous Days**:
   - Keep the closed test active and opted-in for a minimum of 14 consecutive days.
   - Collect feedback on installation, performance, offline behavior, and UI scaling across different screen sizes.
6. **Apply for Production Access**:
   - After 14 days, complete the production access questionnaire in Google Play Console detailing test feedback received and fixes applied.

---

## 6. Production Rollout Strategy

1. **Staged Rollout**:
   - Begin with a 10% or 20% staged rollout to early production users.
   - Monitor Play Console vitals (crash rate, ANR rate; expected to be 0.0% due to offline architecture).
2. **Full Rollout (100%)**:
   - Expand rollout to 100% within 48–72 hours after confirming zero anomalies.
