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

### Penutup
Beri pengguna: cara kembali ke Home asal (`tools/revert.ps1`), cara tukar enam app, dan apa yang telah diubah.

## Membangunkan projek

- Projek Android (Java, tanpa pustaka luar): `app/`. Reka bentuk web untuk pratonton: `design/home.html`.
- Padankan gaya kod sedia ada. Ukuran UI dalam unit "cq" (`lebar skrin / 100`), bukan dp tetap.
- Pengait ujian ADB dalam `MainActivity.onNewIntent`: `--ez splash true`, `--ez rain true|false`,
  `--ef day 0|1` (-1 untuk kosongkan), `--ef speed N`. Lihat [docs/DEVELOPING.md](docs/DEVELOPING.md).
- Sahkan perubahan UI dengan tangkap skrin di unit sebenar, bukan hanya dengan membina.
- Selepas `adb install -r`, kebenaran lokasi kekal; jika tidak, `pm grant my.carglass android.permission.ACCESS_FINE_LOCATION`.
