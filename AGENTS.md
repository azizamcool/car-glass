# AGENTS.md: peraturan untuk pembantu AI

Anda membantu seorang pengguna memasang **Car Glass** pada head unit Android kereta mereka, atau membangunkan
projek ini. Unit kereta mengawal fungsi sebenar (stereng, kamera, radio, Bluetooth), jadi **berhati-hati lebih
penting daripada cepat**.

## Peraturan keras

1. **Semak dahulu, ubah kemudian.** Jangan ubah apa-apa di unit sebelum fasa semakan selesai dan pengguna setuju.
2. **Sahkan dengan pengguna sebelum setiap perubahan**, dengan satu ayat tentang apa ia buat.
3. **Sandar dahulu.** Simpan senarai pakej dan Home asal (`tools/preflight.ps1`). Sahkan arahan pemulihan wujud.
4. **Jangan buang apa-apa secara kekal.** Gunakan kaedah yang boleh dipulihkan (`pm uninstall -k --user 0`,
   `pm disable-user`) hanya jika pengguna meminta, satu kumpulan kecil pada satu masa.
5. **Jangan sentuh** MCU, CAN bus, kamera belakang, kawalan stereng, Bluetooth, radio, bootlogo, factory mode,
   atau pakej yang kelihatan mengawal perkakasan atau kenderaan (contoh `com.imotor.*`, `com.clw.*`, `mcu`, `can`,
   `launcher`, `systemui`, `settings`).
6. **Jangan cuba memintas** kata laluan Developer options, Factory mode atau kemas kini. Jika terkunci, berhenti dan
   beritahu pengguna supaya meminta kod daripada pembekal.
7. **Jangan menerima lesen, memuat turun fail atau memasang perisian di komputer pengguna tanpa bertanya**, dengan
   nama fail, sumber dan saiz.
8. **Jangan terbitkan** apa-apa (GitHub, dll.) atau hantar data keluar tanpa arahan jelas daripada pengguna.
9. Jangan masukkan data peribadi pengguna (nama Wi-Fi, IP, nombor siri, lokasi) ke dalam fail projek atau commit.
10. **Berhenti dan tanya** jika sesuatu tidak sepadan dengan jangkaan.

## Aliran kerja

### Fasa 1: Semakan komputer (belum sentuh unit)
Laporkan: sistem pengendalian, `adb` (versi), JDK 17 dan Android SDK (hanya perlu jika membina). Jika ada yang
tiada, senaraikan apa yang perlu dipasang dengan saiz muat turun dan tunggu kebenaran. `tools/check-pc.ps1`.

### Fasa 2: Semakan unit
Selepas pengguna hidupkan USB/Wireless debugging dan beri IP:port (dan kod pairing jika perlu):
`tools/connect.ps1`, kemudian `tools/preflight.ps1`. Laporkan model, versi Android, RAM, resolusi, Home sekarang,
pakej pengeluar, dan sama ada pakej yang diperlukan wujud. **Berhenti** jika: Android di bawah 9, skrin bukan
landskap, tiada GPS, atau unit tak benarkan pasang APK. Tanya sebelum teruskan.

### Fasa 3: Sandar
`preflight.ps1` menulis `backup/<serial>/` (pakej, Home asal). Jangan teruskan tanpa fail itu.

### Fasa 4: Pemasangan
1. `tools/install.ps1 -Device ...` memasang APK, memberi kebenaran lokasi dan membuka app. **Belum jadi Home.**
2. Ambil tangkap skrin (`tools/screenshot.ps1`) dan minta pengguna mengesahkan rupanya.
3. Tanya pengguna enam app skrin utama. Pakej radio/muzik/Carlink berbeza mengikut unit: semak senarai pakej unit
   mereka dan sesuaikan `DEFAULT_TILES`/`PINNED` dalam `MainActivity.java` jika perlu (pengguna juga boleh tukar
   dengan tahan lama).
4. **Hanya selepas pengguna setuju**, `tools/install.ps1 -SetHome`.

### Fasa 5: Ujian
- Tekan Home dan sahkan Car Glass kekal.
- Minta pengguna matikan dan hidupkan semula kereta, sambung semula ADB (port baharu), dan semak Home.
- Jika Home kembali ke skrin asal, cadangkan pilihan (tetapan boot dalam Factory mode oleh pengguna, dsb.).
  **Jangan buang launcher asal.**
- Ingatkan pengguna menguji: stereng, kamera belakang, radio, Bluetooth, Carlink. Anda tak boleh mengesahkan ini.

### Fasa 6: Optimum (semak RAM dan prestasi, hanya jika pengguna mahu)
Tujuannya: unit tidak tersekat. **Semak dahulu, cadang, tunggu persetujuan, baru ubah.**

1. Jalankan `tools/ram-audit.ps1 -Device ...` (hanya membaca). Terangkan hasilnya dengan mudah:
   - *Free RAM* sudah termasuk cache; cache besar itu normal. Jangan panik melihat "RAM hampir penuh".
   - Cari proses paling berat dan app yang hidup tapi jarang digunakan.
   - **Play Store dengan 2 proses atau lebih** biasanya bermaksud kemas kini automatik sedang berjalan.
2. Cadangan, dari yang paling selamat:
   - Matikan kemas kini automatik Play Store (pengguna: *Play Store, akaun, Settings, Network preferences,
     Auto-update apps, Don't auto-update*). Kemas kini dibuat secara manual semasa kereta berhenti.
   - Skala animasi sistem 0.5 (`settings put global window_animation_scale 0.5` dan dua yang serupa).
   - Senaraikan app yang digunakan kurang daripada kira-kira 1 minit sebulan (pasang sendiri oleh pengguna).
3. **Tanya pengguna untuk setiap app sebelum dibuang. Jangan buang terus.** Tunjuk bukti (masa penggunaan, RAM).
   Gunakan `pm uninstall -k --user 0 <pakej>` hanya selepas persetujuan, dan catat cara memulihkannya:
   app sistem `cmd package install-existing <pakej>`; app Play Store dipasang semula daripada Play Store.
4. Jangan sentuh pakej pengeluar atau Google Play Services/Store.
5. Untuk kelewatan GPS: Car Glass merekod `gps-log.csv` (masa sahaja, tanpa koordinat) di
   `/sdcard/Android/data/my.carglass/files/`. Selepas pengguna memandu, tarik fail itu (`adb pull`) dan baca
   `age_ms` (umur bacaan semasa tiba) dan `interval_ms` (jarak antara bacaan). Selang sekitar 1000 ms bermaksud
   cip GPS melaporkan 1 Hz; `age_ms` yang besar bermaksud bacaan sampai lewat.
6. Kemas kini Android/firmware: hanya daripada pembekal. Beritahu pengguna, jangan flash sendiri.

### Pilihan: Pakej Ringan (`tools/lighten.ps1`, hanya dengan persetujuan)
Mengurangkan app latar dan animasi. Ukur dahulu (`ram-audit.ps1`, RAM bebas, masa mula sejuk beberapa app) dan ukur
semula selepas `-Apply`; beritahu pengguna angka sebenar. Pada unit Mohawk (2 GB) hasilnya sederhana: RAM bebas
+~160 MB, masa mula 2-9% lebih pantas, bilangan proses tidak berubah. Pulihkan dengan `-Restore`.

### Pilihan: lumpuhkan launcher asal (hanya jika pengguna meminta)
Jika Home asal sering muncul kerana ia masih berada dalam susunan tugasan di bawah Car Glass (lihat
`dumpsys activity activities`: dua tugasan `type=home`), `pm disable-user --user 0 <launcher asal>` memastikan
Home sentiasa Car Glass. **Terangkan kos ini dahulu dan dapatkan persetujuan:**
- App tidak boleh mengaktifkannya semula; pada unit Mohawk, app Settings juga **tidak menawarkan "Enable"** untuk
  pakej ini (butang itu kelabu). Pemulihan hanya melalui ADB: `tools/revert.ps1` (ia mengaktifkan semula pakej).
- Ujian selepas melumpuhkan: Home lalai masih Car Glass, `force-stop` Car Glass + HOME kembali ke Car Glass, dan
  hanya Car Glass dan `FallbackHome` Settings kekal sebagai calon HOME.
- Jangan sekali-kali lakukan ini tanpa `backup/<unit>/home.txt` (supaya revert tahu apa yang dipulihkan).

### Penutup
Beri pengguna: cara kembali ke Home asal (`tools/revert.ps1`), cara tukar enam app, dan apa yang telah diubah.

## Membangunkan projek

- Projek Android (Java, tanpa pustaka luar): `app/`. Reka bentuk web untuk pratonton: `design/home.html`.
- Padankan gaya kod sedia ada. Ukuran UI dalam unit "cq" (`lebar skrin / 100`), bukan dp tetap.
- Pengait ujian ADB dalam `MainActivity.onNewIntent`: `--ez rain true|false`,
  `--ef day 0|1` (-1 untuk kosongkan), `--ef speed N`, `--es media play|idle|off` (pemain palsu untuk tangkap skrin).
- Kad media perlu *Notification access*. Berikan hanya dengan persetujuan pengguna (`tools/install.ps1 -MediaAccess`);
  tarik balik dengan `cmd notification disallow_listener my.carglass/my.carglass.MediaAccess`. Lihat [docs/DEVELOPING.md](docs/DEVELOPING.md).
- Sahkan perubahan UI dengan tangkap skrin di unit sebenar, bukan hanya dengan membina.
- Selepas `adb install -r`, kebenaran lokasi kekal; jika tidak, `pm grant my.carglass android.permission.ACCESS_FINE_LOCATION`.
