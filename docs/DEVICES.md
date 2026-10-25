# Unit yang diuji

| Unit | Android | Platform | RAM | Skrin | Keputusan |
|---|---|---|---|---|---|
| Mohawk (model `auto_rk_t10`), Perodua | 11 | RK3326 | 2 GB | 1280x720 | Berfungsi: pasang, Home lalai, splash, drawer, tukar app, tema, hujan, simulasi speedometer. Lihat di bawah |

## Apa yang telah disahkan pada unit Mohawk
- Pemasangan melalui ADB wayarles, kebenaran lokasi, dan menjadi Home lalai (kekal selepas beberapa kitaran tidur dan bangun).
- Splash NEXUP, skrin utama, senarai Semua apps, tahan lama untuk tukar app, penanda Wi-Fi (nama rangkaian).
- Tema siang/malam, efek hujan dan speedometer pada pelbagai kelajuan (dipaksa melalui pengait ujian ADB).

## Belum disahkan
- Speedometer dengan kelajuan GPS sebenar, cuaca dan nama tempat (memerlukan isyarat GPS di luar rumah).
- Nama peranti Bluetooth (unit ini nampaknya mengurus Bluetooth melalui modulnya sendiri).
- Kekal sebagai Home selepas boot penuh (cabut kuasa).
- Prestasi CarPlay semasa Car Glass dipasang.

## Nota unit Mohawk
- Developer options dikunci kata laluan. Kod diperoleh daripada pembekal.
- Pakej pintasan lalai (`com.imotor.fmam`, `btmusic`, `phoneconnect`) khusus untuk firmware ini.
- "Navigasi" (`com.imotor.navigation`) hanyalah skrin tetapan untuk memilih app peta lalai, bukan app peta.
- Unit tidak benar-benar reboot setiap kali kereta dimatikan; ia tidur dan bangun (uptime berhari-hari).

## Menambah unit anda
Jalankan `tools/preflight.ps1` dan kongsi ringkasan (model, Android, RAM, skrin, sama ada pintasan berfungsi).
Jangan sertakan nombor siri, nama Wi-Fi atau tangkap skrin yang menunjukkan nama Wi-Fi anda.
