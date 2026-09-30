# CalorieTrack â€” Security Requirements

This document outlines the security architecture and defensive controls for CalorieTrack. Because CalorieTrack is an offline-first application, its security model focuses on device-level safety, data integrity, and strict minimization of attack surface.

---

## 1. Zero External Attack Surface
- **No Remote Network Endpoints**: The app does not expose or consume proprietary REST/GraphQL APIs or cloud databases in V1.
- **No Internet Permission**: The app manifest omits `android.permission.INTERNET`. Even if malicious code were somehow introduced via a dependency, it would be blocked at the OS kernel level from transmitting data over the network.
- **No User Credentials to Compromise**: Because there are no user accounts, passwords, or session tokens, there are no authentication databases that could be breached or leaked.

---

## 2. Local Database & Query Security
- **Room Prepared Statements**: All interactions with the underlying SQLite database use Room's compile-time verified parameterized queries. User inputs (food names, serving units, search strings) are always treated as literals, mathematically preventing SQL injection attacks.
- **Input Validation**: All nutritional values entered by users (calories, protein, carbs, fats) are validated at the ViewModel boundary:
  - Must be positive finite floating-point numbers.
  - Capped at realistic physical thresholds (e.g., maximum 10,000 kcal per single entry) to avoid numeric overflow or corrupted totals.

---

## 3. Sandboxed Application Storage
- **Internal Storage Sandbox**: All application files, including `calorietrack.db` and preferences, are stored exclusively within Android's private app sandbox (`/data/data/com.calorietrack.app/`).
- **No World-Readable Files**: No files are written to external public shared storage without user-mediated Storage Access Framework (SAF) interaction.
- **Process Isolation**: Android assigns a unique Linux User ID (UID) to the application process, preventing other third-party apps on the device from accessing CalorieTrack's local files.

---

## 4. Supply Chain & Build Security
- **Gradle Wrapper Verification**: In CI, GitHub Actions executes `gradle/actions/wrapper-validation@v6` before any build tasks run, ensuring `gradle-wrapper.jar` has not been tampered with.
- **Minimal Dependencies**: The application avoids importing massive third-party utility libraries, reducing supply-chain vulnerability exposure to near zero.
- **Reproducible CI Builds**: APKs are built in clean, isolated ephemeral Ubuntu runners via GitHub Actions.
