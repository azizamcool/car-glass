# Car Glass

Skrin utama "kaca" untuk head unit Android kereta. Speedometer GPS yang sporty, waktu solat, cuaca,
status Wi-Fi / Bluetooth / GPS, dan enam pintasan app yang boleh ditukar. Tema siang dan malam bertukar
mengikut matahari, dengan efek hujan.

> **Status: percubaan.** Diuji pada satu unit sahaja (lihat [docs/DEVICES.md](docs/DEVICES.md)).
> Anda bertanggungjawab ke atas unit anda sendiri. Launcher asal tidak dibuang dan boleh dipulihkan.

*English:* a touch-friendly Android launcher for car head units: sporty GPS speedometer, prayer times,
weather, Wi-Fi/Bluetooth/GPS status, six customisable app tiles, automatic day/night theme and a rain effect.
Experimental, tested on a single Mohawk RK3326 unit (Android 11).

## Ciri-ciri

- **Speedometer** 50 segmen dengan zon merah, jarum penanda dan nombor condong (kelajuan daripada GPS unit).
- **Jam** tebal, mengikut tetapan 12/24 jam sistem, tarikh dalam Bahasa Melayu.
- **Waktu solat** dikira dalam app (tanpa internet) menggunakan lokasi GPS: Subuh/Isyak 18°, Asar mazhab Syafie.
  Anggaran sahaja; rujuk JAKIM untuk waktu rasmi.
- **Cuaca dan nama tempat** melalui Open-Meteo dan OpenStreetMap, dikemas kini setiap 30 minit bila ada internet.
- **Penanda status:** nama Wi-Fi, Bluetooth, GPS (jumlah satelit). Tekan untuk buka tetapan.
- **Enam app** di skrin utama. **Tahan lama** untuk tukar; **Semua apps** untuk senarai penuh. Ikon diambil terus daripada app.
- **Siang / malam** dengan pudar sekitar Syuruk dan Maghrib; **hujan** bila cuaca menunjukkan hujan.
- **Splash** pendek (sekitar 1.3 saat) bila app dimulakan atau unit bangun.

## Apa yang diperlukan

| Perkara | Keperluan |
|---|---|
| Unit | Android 9+ (minSdk 28), skrin landskap, GPS, kebenaran memasang APK |
| Komputer | Windows 10/11 dengan PowerShell, `adb` (Android platform-tools) |
| Rangkaian | Komputer dan unit di Wi-Fi yang sama (untuk ADB wayarles) |
| Kata laluan | Banyak unit mengunci *Developer options*. Anda perlu kod daripada pembekal. **Jangan cuba memintasnya.** |

## Pasang dengan pembantu AI (disyorkan)

1. Buka folder ini dalam Claude Code (atau pembantu AI yang boleh menjalankan arahan).
2. Salin arahan dalam [PROMPT.md](PROMPT.md) dan gantikan maklumat unit anda.
3. Pembantu mengikut [AGENTS.md](AGENTS.md): semak dahulu, sandar, tanya sebelum setiap perubahan.

## Pasang secara manual

```powershell
# 1. Sambung (ganti IP:port daripada skrin Wireless debugging; -PairWith jika diminta kod 6 digit)
.\tools\connect.ps1 -Target 192.168.1.50:41569

# 2. Semak unit dan sandar. Tidak mengubah apa-apa selain menulis folder backup\
.\tools\preflight.ps1 -Device 192.168.1.50:41569

# 3. Bina APK (JDK 17 + Android SDK) atau guna APK yang anda ada
.\gradlew.bat assembleDebug

# 4. Pasang dan buka (belum jadi Home)
.\tools\install.ps1 -Device 192.168.1.50:41569

# 5. Selepas anda berpuas hati, jadikan Home lalai
.\tools\install.ps1 -Device 192.168.1.50:41569 -SetHome

# Kembali ke Home asal bila-bila masa
.\tools\revert.ps1 -Device 192.168.1.50:41569
```

Port ADB wayarles bertukar setiap kali unit dihidupkan semula, jadi dapatkan port baharu pada skrin unit.

## Tukar app di skrin utama

Tahan satu kotak app selama kira-kira setengah saat, kemudian pilih app pengganti. Pilihan disimpan.

## Bina daripada kod

Keperluan: JDK 17 dan Android SDK (platform 34, build-tools 34). Lihat [docs/DEVELOPING.md](docs/DEVELOPING.md).

## Privasi

- App tidak ada akaun, analitik atau iklan.
- Rangkaian digunakan hanya untuk cuaca (`api.open-meteo.com`) dan nama tempat (`nominatim.openstreetmap.org`),
  menghantar koordinat anda. Tanpa internet, bahagian itu menunjukkan data terakhir yang disimpan.
- Nama Wi-Fi dipaparkan di skrin. Berhati-hati jika anda mengambil tangkap skrin untuk dikongsi.

## Kredit dan lesen

- Kod: MIT (lihat [LICENSE](LICENSE)). Nama dan logo NEXUP dalam splash ialah jenama pemilik dan tidak termasuk dalam lesen.
- Font **Exo 2**: SIL Open Font License 1.1 ([licenses/OFL-Exo2.txt](licenses/OFL-Exo2.txt)).
- Cuaca: [Open-Meteo](https://open-meteo.com) (penggunaan bukan komersial, dengan atribusi).
- Nama tempat dan data peta: © pengeluar sumbangan [OpenStreetMap](https://www.openstreetmap.org/copyright) (ODbL), melalui Nominatim.
- Konsep repo yang boleh dipasang oleh pembantu AI diilhamkan oleh [Glass TV](https://github.com/cocombee/glass-tv). Tiada kod disalin.
