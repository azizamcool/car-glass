# Masalah biasa

| Masalah | Sebab dan tindakan |
|---|---|
| Developer options minta kata laluan | Unit mengunci menu itu. Dapatkan kod daripada pembekal unit. Jangan cuba memintas. |
| `adb connect` ditolak (10061) | Port sudah tukar. Buka *Wireless debugging* dan baca port baharu. |
| `adb connect` tamat masa (10060) | Unit tidur/mati, atau komputer di rangkaian lain. Hidupkan unit dan semak Wi-Fi. |
| Unit minta pairing | Tekan *Pair device with pairing code*, kemudian `tools\connect.ps1 -PairWith IP:PORT -Code 123456`. |
| Speedometer kekal 0 | GPS belum dapat isyarat (di dalam bangunan). Penanda GPS menunjukkan jumlah satelit bila ada fix. |
| Cuaca tiada | Perlu GPS fix dan internet (Wi-Fi atau hotspot). |
| Nama Wi-Fi tidak keluar | Android perlu kebenaran lokasi dan lokasi dihidupkan: `pm grant my.carglass android.permission.ACCESS_FINE_LOCATION`. |
| Home kembali ke skrin asal selepas hidupkan kereta | Unit memaksa launcher asal. Semak tetapan boot unit. Jangan buang launcher asal. |
| Mahu kembali ke asal | `tools\revert.ps1 -Device IP:PORT`, atau `adb shell cmd package set-home-activity com.android.launcher3/.Launcher`. |
| Mahu nyahpasang | `adb uninstall my.carglass` (tetapkan Home asal dahulu). |
