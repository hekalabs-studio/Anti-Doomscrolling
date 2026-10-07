# Anti DumbScroll

Anti DumbScroll adalah aplikasi Android sumber terbuka (open source) yang dibuat untuk membantu Anda membatasi waktu layar (*screen time*) berlebihan di aplikasi media sosial, agar Anda bisa fokus pada hal yang lebih penting atau istirahat sejenak.

## Fitur Utama
- **Pemblokiran Waktu Nyata**: Menggunakan layanan aksesibilitas untuk mendeteksi kapan aplikasi media sosial aktif, dan mencegah Anda scrolling tanpa henti.
- **Pomodoro Timer**: Membantu Anda belajar atau bekerja dalam mode fokus (contoh: 25 menit kerja, 5 menit istirahat).
- **Notifikasi Persisten**: Mengingatkan Anda tentang durasi penggunaan.
- **Overlay Layar Penuh**: Mengambil alih layar untuk memaksa Anda beristirahat saat batas waktu terlampaui.

## Cara Build
Untuk mengompilasi dan menjalankan aplikasi ini:
1. Pastikan Anda sudah menginstal **Android Studio** atau command-line tools Android (CLI).
2. Kloning (*clone*) repositori ini (`https://github.com/hekalabs-studio/Anti-Doomscrolling`) ke komputer Anda.
3. Buka terminal atau Android Studio, lalu jalankan:
   ```bash
   ./gradlew :app:assembleDebug
   ```
4. APK hasil *build* akan berada di folder `app/build/outputs/apk/debug/`.

## Izin yang Diperlukan
Aplikasi ini membutuhkan beberapa izin sistem Android agar berfungsi dengan baik:
- **POST_NOTIFICATIONS**: Diperlukan untuk menampilkan timer, informasi penggunaan sesi, dan notifikasi bahwa pemantauan sedang aktif.
- **ACCESSIBILITY (Layanan Aksesibilitas)**: Diperlukan untuk mendeteksi aplikasi apa yang sedang aktif di layar utama Anda (hanya mengambil nama aplikasi/*package name* untuk menghitung waktu layar). Aplikasi tidak merekam isi layar, tidak membaca pesan, dan tidak mencuri kata sandi.
- **SYSTEM_ALERT_WINDOW (Tampil di Atas Aplikasi Lain / Overlay)**: Diperlukan untuk menampilkan pesan pemblokiran atau mode fokus (mengambil alih layar saat waktu habis) tanpa membuka aplikasi secara langsung.

## Cara Instalasi APK & Panduan Izin
Saat menginstal aplikasi di luar Google Play Store (sideload), Anda mungkin melihat peringatan **Play Protect** ("Unsafe app blocked" atau semacamnya).
1. Pilih **"More details"** dan klik **"Install anyway"**.
2. **Android 13+ "Restricted Settings"**: Pada Android 13 dan yang lebih baru, Android membatasi izin Layanan Aksesibilitas bagi APK yang diinstal dari luar toko resmi. Jika Anda mencoba mengaktifkan Layanan Aksesibilitas untuk Anti DumbScroll dan mendapatkan peringatan "Restricted Setting":
   - Buka **Pengaturan** sistem Android.
   - Pergi ke **Aplikasi** > cari **Anti DumbScroll**.
   - Tekan ikon **tiga titik (Menu)** di sudut kanan atas info aplikasi, lalu pilih **"Allow restricted settings"** (Izinkan pengaturan terbatas). Konfirmasi identitas Anda (PIN/Sidik Jari).
   - Setelah diizinkan, kembali ke setelan Aksesibilitas dan Anda sudah bisa mengaktifkan **Anti DumbScroll Service**.
3. **Peringatan Aplikasi Bank**: Harap dicatat bahwa beberapa aplikasi perbankan modern mungkin mengenali Overlay (System Alert Window) dan Layanan Aksesibilitas dari Anti DumbScroll. Aplikasi perbankan tersebut mungkin meminta Anda mematikan overlay atau layanan aksesibilitas saat menggunakannya demi alasan keamanan, untuk mencegah aplikasi lain merekam layar Anda. Hal ini normal pada Android.

## Dukung Pengembangan
Jika aplikasi ini membantu hidup Anda lebih fokus dan produktif, Anda dapat mendukung pengembangannya. Donasi sepenuhnya **opsional** dan tidak diwajibkan untuk menggunakan seluruh fitur aplikasi.
[Dukung Pengembangan (Donasi)](https://hekalabs-donation.web.app)

## Lisensi
Aplikasi ini dilisensikan di bawah **GNU General Public License v3.0 (GPL-3.0)**.
Kode sumber dapat digunakan secara bebas, diubah, dan didistribusikan ulang sesuai syarat GPLv3. Seluruh karya turunan harus bersumber terbuka (open source) menggunakan lisensi yang sama.

**Catatan Merek Dagang:**
Nama dan logo "Anti DumbScroll" adalah hak milik Hekalabs Studio dan tidak untuk digunakan pada produk turunan/fork tanpa izin.
