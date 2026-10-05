# Dumbscrolling

Dumbscrolling adalah aplikasi Android untuk membantu mengurangi kecanduan scrolling pada aplikasi media sosial atau aplikasi lain yang menghabiskan waktu. Aplikasi ini melacak waktu penggunaan dan akan memunculkan layar overlay ("Time's Up") ketika batas waktu tercapai.

## Catatan Penting Tentang Force Stop

Dalam sistem operasi Android, aplikasi reguler (tanpa akses Root atau bukan sebagai System App) **dilarang secara ketat** untuk melakukan "Force Stop" secara langsung terhadap aplikasi lain. Fitur Force Stop secara sistem hanya tersedia lewat pengaturan perangkat.

Sebagai alternatif yang realistis dan paling efektif tanpa memerlukan root, Dumbscrolling menggunakan metode berikut:
1. Memanggil `performGlobalAction(GLOBAL_ACTION_HOME)` untuk membawa pengguna kembali ke Home Screen.
2. Memanggil `ActivityManager.killBackgroundProcesses(packageName)` setelah jeda beberapa saat. Metode ini hanya menghentikan proses di latar belakang, namun ketika aplikasi target dibuka lagi, ia akan mulai dari kondisi awal (restart) atau tidak dapat mempertahankan state sebelumnya secara penuh.

Ini adalah metode yang "jujur" dan paling mendekati Force Stop yang bisa dilakukan oleh aplikasi biasa di Android saat ini.
