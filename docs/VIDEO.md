# Skrip video pendek (30-45 saat, menegak atau mendatar)

## Sebelum merakam

- Tahan lama penanda Wi-Fi dan nama tempat supaya disembunyikan (lihat Privasi dalam README).
- Rakam skrin unit: `adb shell screenrecord --time-limit 30 /sdcard/klip.mp4` kemudian `adb pull /sdcard/klip.mp4`.
  Had 3 minit sekali rakam. Resolusi 1280x720 sudah mencukupi.
- Pengait ujian (tanpa perlu memandu): 
  `adb shell am start -n my.carglass/.MainActivity --ef speed 112 --ef day 1 --ez rain true`
  (`--ez rain false`, `--ef day 0|1|-1`, `--ef speed N`).
- Jangan rakam skrin kamera belakang atau lokasi rumah.

## Susunan syot

| Masa | Syot | Cara |
|---|---|---|
| 0-3 s | Skrin utama malam, speedometer 0 | `--ef day 0 --ef speed 0` |
| 3-9 s | Malam, speedometer naik 0 ke 146 | `--ef day 0` lalu `--ef speed` 0, 40, 80, 120, 146 |
| 9-14 s | Peralihan siang, speedometer 112 | `--ef day 1 --ef speed 112` |
| 14-19 s | Hujan malam | `--ez rain true --ef day 0` |
| 19-26 s | Tahan lama satu kotak, pilih app lain | tangan atau `input swipe x y x y 600` |
| 26-32 s | Semua apps dan butang Kemaskini | ketik "Semua apps" |
| 32-38 s | Rajah pemasangan: arahan AI (PROMPT.md) | tangkap skrin repo GitHub |
| 38-45 s | Teks akhir: nama repo dan amaran "percubaan" | suntingan |

## Skrip suara (BM)

1. "Head unit kereta saya lembap dan skrin utamanya biasa-biasa sahaja. Jadi saya bina sendiri: Car Glass."
2. "Speedometer GPS, waktu solat, cuaca, siang dan malam bertukar sendiri, dan ada hujan."
3. "Enam app pilihan saya. Tahan lama untuk tukar."
4. "Pasangnya senang: beri arahan kepada Claude, ia semak dahulu, sandar, dan tanya sebelum ubah apa-apa."
5. "Kod terbuka di GitHub. Percubaan, diuji pada satu unit. Guna atas risiko sendiri."

## Sari kata (jika tanpa suara)

Ulang baris 1-5 sebagai teks pendek, maksimum dua baris setiap syot.
