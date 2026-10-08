# Laporan Audit Kualitas: Anti DumbScroll

## 1. Ringkasan
- **Tanggal Audit:** 8 Oktober 2026
- **Hasil Build & Lint:** Build `assembleDebug` & `assembleRelease` berhasil, namun **Lint gagal** (23 Errors, 127 Warnings). Terdapat potensi *crash* karena kesalahan *formatting string*.
- **Hasil Test Unit:** 1 lulus (bawaan), 4 gagal (test khusus mesin state Pomodoro & Edge Cases ditambahkan selama audit membuktikan celah logika).

---

## 2. Tabel Verifikasi Fitur & Analisis Kode

| ID | Area | Butir / Pengecekan | Status | Bukti (File/Line) | Tingkat | Saran Perbaikan Singkat |
|---|---|---|---|---|---|---|
| 1 | **Lint / Statis** | Tidak ada error Lint kritikal | **FAIL** | `AppTrackingService.kt:366` | P0 | Hapus argumen ekstra ke-3 pada `getString(R.string.overlay_time_spent)` untuk mencegah `IllegalFormatConversionException`. |
| 2 | **Fitur** | State Pomodoro selesai di *background* | **FAIL** | `FocusModeViewModel.kt:270` | P0 | Pindahkan eksekusi perpindahan fase (State Machine) sepenuhnya ke `PomodoroService`, bukan bergantung pada ViewModel yang dapat dihancurkan sistem. |
| 3 | **Fitur** | Penghitungan sesi akurat (berhenti saat layar mati) | **FAIL** | `AppTrackingService.kt:180` | P1 | Daftarkan `BroadcastReceiver` untuk mendeteksi `ACTION_SCREEN_OFF` agar `trackingJob` dihentikan/dijeda. |
| 4 | **Fitur** | Eskalasi jeda tombol lanjut di Overlay | **FAIL** | `AppTrackingService.kt:410` | P2 | Tambahkan coroutine delay sebelum `btnSecondary` dapat diklik, serta batasi batas klik maksimal harian. |
| 5 | **Privasi** | Deklarasi izin minimum yang tepat | **FAIL** | `AndroidManifest.xml:5` | P2 | Izin `android.permission.INTERNET` digunakan tanpa adanya request internal (semua tautan dilempar ke *browser intent*). Hapus izin ini. |
| 6 | **Fitur** | Kondisi kosong Dashboard (tidak ada angka tiruan) | **PASS** | `DashboardScreen.kt:288` | - | Logika `empty_monitored_apps_desc` bekerja dengan baik untuk kondisi belum ada aplikasi terdaftar. |
| 7 | **Fitur** | Overlay muncul tunggal & dihapus saat tidak aktif | **PASS** | `AppTrackingService.kt:156` | - | `removeOverlay()` selalu dipanggil di awal `handleAppChange()` memastikan overlay tidak bocor dan menumpuk. |

---

## 3. Daftar Bug (Diurutkan Berdasarkan Keparahan)
1. **[P0] Crash pada Overlay:** Pemanggilan `getString(R.string.overlay_time_spent, usageMinutes, usageSeconds, appName)` mengirimkan 3 variabel, sedangkan pada `strings.xml` hanya mendefinisikan 2 placeholder (`%1$s` dan `%2$s`). Ini akan memicu exception *crash* saat overlay mencoba tampil.
2. **[P0] Pomodoro Terjebak (*Stuck*):** Jika aplikasi ditutup dari *recent apps*, `FocusModeViewModel` dihancurkan. Akibatnya, `PomodoroService` terus menunjukkan 00:00 di notifikasi tanpa otomatis berpindah ke mode Istirahat, dan statistik harian tidak dicatat hingga pengguna membuka aplikasi kembali.
3. **[P1] Penghitungan Sesi Layar Mati:** `AppTrackingService` menggunakan `delay(1000)` *coroutine loop* konstan. Pemakaian layar akan terus dihitung dan menambah beban baterai (*wakelock* halus) biarpun layar perangkat mati karena tidak merespon pada `Intent.ACTION_SCREEN_OFF`.
4. **[P2] Jeda Tombol "Beri saya 30 detik":** Tombol dapat langsung ditekan seketika tanpa ada batas maksimal "kesempatan" atau penundaan klik, bertolak belakang dengan rencana eskalasi paksaan pada desain awal.
5. **[P2] Lint Jetpack Compose State:** 6 `Error` pada `DashboardScreen.kt` dan `AboutScreen.kt` terkait pembacaan `Locale.getDefault()` dan `LocalContext` yang tidak direkomendasikan pada Compose karena tidak *observable* pada konfigurasi ubah (misalnya saat ganti tema/bahasa).

---

## 4. HARUS DIUJI MANUAL di HP Asli
1. **Vibrator/Getaran Sistem:** Tidak dapat disimulasikan apakah *VibrationEffect* pada Android 14+ berjalan mulus pada akhir sesi Pomodoro di semua pabrikan HP.
2. **Perilaku OEM Battery Optimization:** Apakah UI Xiaomi/Oppo/Samsung agresif membunuh `PomodoroService` yang berjalan dengan tipe `specialUse` di background.
3. **Aplikasi Terlindungi:** Perilaku *overlay blocker* jika melapis aplikasi-aplikasi Bank, Autentikator, atau layar *password manager* (Android memiliki *Flag Secure* yang mungkin menyebabkan masalah klik).
4. **Interupsi Telepon Masuk:** Uji coba apakah overlay pemblokiran mengganggu layar UI telepon masuk saat durasi sosmed habis.

---

## 5. Keterbatasan Diketahui (*Known Limitations*)
- Sistem Android membatasi aplikasi non-sistem (bukan `root`/device owner) untuk dapat secara murni mematikan aplikasi latar belakang lain. Pemanggilan `killBackgroundProcesses` bersifat *soft request* dan bergantung pada keputusan OS untuk benar-benar menutup aplikasi sasaran.
- Aplikasi yang memanfaatkan API *AccessibilityService* berpotensi ditandai (*flagged*) sebagai sensitif oleh Google Play Protect atau beberapa aplikasi perbankan ketat.

---

## 6. VONIS KESIAPAN RILIS
**BELUM SIAP (NOT READY)**

**Alasan Utama:** 
Terdapat 2 bug tingkat P0 yang fatal: *Crash* format string yang akan langsung menutup paksa layanan saat overlay seharusnya tampil, dan state mesin Pomodoro yang bergantung pada siklus hidup *UI (ViewModel)* alih-alih beroperasi mandiri di *Background Service*, membuat inti fitur utama tidak dapat bekerja sempurna jika pengguna tidak membiarkan aplikasi terbuka.

**Harus diselesaikan sebelum siap rilis (Atau Beta):**
1. Perbaiki `StringFormatMatches` di `AppTrackingService.kt:366`.
2. Pindahkan logika *timer* & perpindahan sesi `Fokus <-> Istirahat` ke `PomodoroService`.
3. Terapkan BroadcastReceiver `ACTION_SCREEN_OFF` untuk menjeda penghitung pemakaian aplikasi latar.
