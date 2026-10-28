# Membangunkan Car Glass

## Keperluan
- JDK 17, Android SDK (platform 34, build-tools 34.0.0), Gradle 8.7 (wrapper disertakan: `gradlew.bat`).
- `local.properties` dengan `sdk.dir=C:/path/ke/android-sdk` (git-ignored), atau tetapkan `ANDROID_HOME`.
- Pada Windows, gunakan laluan Windows (bukan `/c/...`) untuk `ANDROID_HOME` dan `JAVA_HOME`.

```powershell
.\gradlew.bat assembleDebug
# hasil: app\build\outputs\apk\debug\app-debug.apk
```

## Struktur
| Fail | Fungsi |
|---|---|
| `MainActivity.java` | Susun atur, tema, status, cuaca, drawer, kitaran hayat |
| `SpeedometerView.java` | Gauge bersegmen dan nombor kelajuan |
| `BackgroundView.java` | Latar jalan malam dan siang (pudar mengikut `dayFactor`), kelabu bila hujan |
| `RainView.java` | Efek hujan (~30 fps, hanya bila kelihatan) |
| `PrayerTimes.java` | Waktu solat luar talian |
| `WeatherClient.java` | Open-Meteo dan Nominatim (jalankan di luar thread utama) |
| `Fonts.java` | Memuatkan Exo 2 (font boleh ubah) dari `assets/fonts` |
| `design/home.html` | Pratonton reka bentuk dalam pelayar (tidak digunakan oleh app) |

Unit ukuran UI ialah `cq` = lebar skrin / 100, supaya susun atur skala pada resolusi lain.

## Pengait ujian ADB
`MainActivity` menerima parameter melalui `am start` (app mesti sudah berjalan):

```powershell
adb shell am start -n my.carglass/.MainActivity --ez rain true       # hujan (false untuk matikan)
adb shell am start -n my.carglass/.MainActivity --ef day 1           # paksa siang (0 malam, -1 normal)
adb shell am start -n my.carglass/.MainActivity --ef speed 95        # paparkan kelajuan 95
```

Ambil tangkap skrin dengan `tools\screenshot.ps1`. Nota: `adb exec-out screencap -p > file.png` dalam PowerShell
5.1 merosakkan fail; gunakan `screencap` di unit dan `adb pull`.

## Perkara yang perlu diingat
- `onDraw` tidak mengalokasi objek berat dan latar hanya dilukis semula bila `dayFactor` berubah.
- Cat (`Paint`) yang dikongsi mesti ditetapkan ke warna legap sebelum melukis dengan `Shader`, kerana alfa warna
  terakhir akan mendarab alfa shader (ini pernah menggelapkan langit siang).
- Cuaca tidak diambil sehingga GPS pernah dapat isyarat, supaya tempat lalai bukan dipaparkan sebagai lokasi sebenar.
