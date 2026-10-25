# Arahan untuk pembantu AI: pasang Car Glass

Salin teks di bawah garis ke Claude Code (atau pembantu AI lain yang boleh menjalankan arahan di komputer anda,
dengan folder repo ini dibuka). Gantikan bahagian dalam [kurungan] dengan maklumat unit anda.

---

Saya mahu pasang **Car Glass** pada head unit Android kereta saya. Baca dan ikut `AGENTS.md` dalam repo ini
dengan tepat: semak dahulu, sandar, dan tanya saya sebelum setiap perubahan.

Maklumat unit: jenama/model [contoh: Mohawk], kereta [contoh: Perodua Axia].

Sebelum memulakan:
1. Jalankan fasa semakan komputer (`tools/check-pc.ps1`) dan beritahu saya apa yang tiada. Jangan memasang
   apa-apa tanpa menyebut nama, sumber dan saiz muat turun dan menunggu kebenaran saya.
2. Beritahu saya langkah di unit yang perlu saya buat sendiri (hidupkan Developer options dan Wireless debugging,
   beri IP:port, dan kod pairing jika diminta).
3. Jalankan `tools/preflight.ps1`, laporkan keputusan, dan tanya saya sama ada mahu teruskan.

Selepas Car Glass berjalan, tanya saya sama ada mahu **semak prestasi dan RAM** (Fasa 6 dalam `AGENTS.md`):
jalankan `tools/ram-audit.ps1`, terangkan hasilnya dengan bahasa mudah, dan cadangkan cara mengoptimumkan.
**Tanya saya dahulu untuk setiap app sebelum membuang atau mematikannya. Jangan buang terus.**

Peraturan penting (ringkasan, lihat `AGENTS.md`):
- Jangan sentuh MCU, CAN bus, kamera belakang, kawalan stereng, Bluetooth, radio atau bootlogo.
- Jangan cuba memintas kata laluan Developer options atau Factory mode. Kalau terkunci, berhenti dan beritahu saya.
- Jangan buang apa-apa secara kekal. Jangan jadikan Car Glass sebagai Home sehingga saya setuju.
- Selepas setiap langkah penting, ambil tangkap skrin dan semak.
- Beri saya cara kembali ke Home asal (`tools/revert.ps1`) dan senarai apa yang telah diubah.
