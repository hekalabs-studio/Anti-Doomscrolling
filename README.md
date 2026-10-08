# Anti DumbScroll 🚫📱

> **Menghentikan scrolling tanpa henti dan mengembalikan fokus Anda.**

Anti DumbScroll adalah aplikasi Android yang dirancang untuk membantu Anda lepas dari jeratan *doomscrolling* atau *dumbscrolling* di media sosial dan aplikasi adiktif lainnya. Aplikasi ini memaksa Anda untuk berhenti sejenak, berpikir ulang, dan mengambil kembali kendali atas waktu Anda.

## ✨ Fitur Utama
*   **Pemblokir Pintar (Smart Blocker):** Menerapkan eskalasi jeda 30 detik yang memaksa Anda berpikir dua kali sebelum membuka aplikasi adiktif.
*   **Mode Pomodoro Tangguh:** Sesi fokus yang dijamin berjalan stabil di *background service* agar Anda tidak terdistraksi dan tetap produktif.
*   **Analitik 7 Hari:** Pantau tren fokus dan kebiasaan digital Anda melalui visualisasi *Bar Chart* yang interaktif.
*   **Jadwal Pemblokiran Otomatis:** Atur rutinitas harian untuk memblokir aplikasi pada jam-jam tertentu secara otomatis.
*   **Widget Layar Beranda:** Akses cepat untuk memulai sesi Pomodoro atau melihat status pemblokiran langsung dari *Home Screen*.
*   **Multi-bahasa:** Mendukung berbagai bahasa, termasuk Bahasa Indonesia (ID), English (EN), Русский (RU), dan 中文 (ZH).
*   **Tema Dinamis (Material You):** Tampilan antarmuka modern yang secara dinamis menyesuaikan dengan warna *wallpaper* perangkat Anda.

## 🔒 Komitmen Privasi (Privacy-First)
**100% Offline & Aman.**
Aplikasi ini memprioritaskan privasi Anda dan beroperasi sepenuhnya di dalam perangkat.
*   **Tanpa Akses Internet:** Kami sama sekali tidak mendeklarasikan izin `INTERNET` di aplikasi ini. Data Anda tidak akan pernah keluar dari perangkat.
*   **Hanya Membaca Package Name:** *Accessibility Service* secara eksklusif hanya digunakan untuk mendeteksi ID aplikasi (*package name*) yang sedang dibuka (untuk keperluan pemblokiran). Aplikasi tidak membaca isi layar, teks yang diketik, atau data pribadi lainnya.

## 🚀 Panduan Instalasi (Penting!)

Karena Anti DumbScroll bekerja di tingkat sistem untuk mendeteksi dan memblokir aplikasi, Anda perlu memberikan beberapa izin sistem khusus agar aplikasi dapat berjalan optimal.

### 1. Instalasi APK
Unduh file APK dari halaman *Releases* dan instal secara manual (sideload) di perangkat Android Anda.

### 2. Mengizinkan "Pengaturan Terbatas" di Android 13+ (Aksesibilitas)
Pada Android 13 ke atas, Google membatasi izin Aksesibilitas untuk aplikasi yang diinstal di luar Play Store. Ikuti langkah ini untuk mengaktifkannya:
1. Buka **Pengaturan (Settings)** > **Aplikasi (Apps)** > Cari dan pilih **Anti DumbScroll**.
2. Ketuk ikon **tiga titik (⋮)** di pojok kanan atas.
3. Pilih **Izinkan pengaturan terbatas (Allow restricted settings)**.
4. Setelah itu, buka pengaturan Aksesibilitas di perangkat Anda, cari "Anti DumbScroll", dan aktifkan layanannya.

### 3. Pengaturan Baterai (Unrestricted)
Agar *Smart Blocker* dan *Pomodoro Background Service* tidak dimatikan secara paksa oleh sistem operasi (terutama pada perangkat OEM seperti Xiaomi, Samsung, Oppo, dan Vivo):
1. Buka **Info Aplikasi (App Info)** untuk Anti DumbScroll.
2. Masuk ke menu **Baterai (Battery)**.
3. Ubah pengaturannya menjadi **Tidak Dibatasi (Unrestricted)**.

> [!WARNING]  
> **Catatan Terkait Aplikasi Perbankan:** Beberapa aplikasi bank mungkin menampilkan peringatan keamanan karena mendeteksi Anti DumbScroll menggunakan fitur *Accessibility Service*. Ini adalah peringatan standar dari aplikasi perbankan. Anda tidak perlu khawatir karena Anti DumbScroll 100% *offline* dan tidak memiliki kemampuan untuk mengirim data ke internet.

## 🛠 Tech Stack
Aplikasi ini dibangun menggunakan arsitektur dan teknologi Android modern:
*   **Kotlin** - Bahasa pemrograman utama.
*   **Jetpack Compose** - UI toolkit deklaratif untuk antarmuka pengguna.
*   **Room DB** - Penyimpanan database relasional lokal.
*   **Dagger-Hilt** - *Dependency Injection* untuk arsitektur yang bersih.
*   **AccessibilityService** - Layanan inti (core system) untuk mendeteksi aktivitas aplikasi.

## 📄 Lisensi & Hak Cipta
Aplikasi ini didistribusikan di bawah lisensi **GPL-3.0**.

**Hak Cipta © 2026 Hekalabs Studio.**
Logo, desain merek, dan nama "Anti DumbScroll" adalah properti eksklusif milik Hekalabs Studio.
