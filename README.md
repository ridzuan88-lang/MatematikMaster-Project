# Matematik Master – Family Edition

Aplikasi Android native (Kotlin + Jetpack Compose + Room), luar talian, untuk telefon, tablet, Android TV dan TV box.
Pakej: `com.chewanninja.matematikmaster`

## Cara bina APK
1. Letak SEMUA fail dalam repo (struktur sama seperti dalam ZIP, termasuk folder `.github`).
2. GitHub → Actions → **Build Matematik Master APK** → **Run workflow**.
3. Tunggu hijau → buka run → **Artifacts** → muat turun **Matematik-Master-APK** → extract ZIP → `app-debug.apk`.
4. Pasang pada telefon (benarkan "install unknown apps") atau Android TV (USB / aplikasi Downloader / `adb install`).

Release APK hanya dibina jika Secrets ini wujud: `KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`.
Tanpa secrets, debug APK tetap dibina.

## Nota
- Tiada `gradlew` / `gradle-wrapper.jar` (fail binari). Workflow memasang Gradle 8.7 sendiri; Android Studio boleh jana wrapper kemudian.
- Larian pertama: Ibu Bapa/Admin dicipta di skrin Selamat Datang, kemudian tambah profil anak di Dashboard Ibu Bapa.
