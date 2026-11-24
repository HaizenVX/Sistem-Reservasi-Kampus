# Menjalankan Rsi_Kelompok5 di Windows

Program memakai HTML -> fetch -> Java HttpServer -> controller proyek -> model JDBC -> MySQL. Nama file, kelas, method, dan parameter lama dipertahankan. `JdbcModels.java` menyediakan adapter database melalui konstruktor controller yang sudah ada. Model simulasi asli tetap tersedia untuk referensi, tetapi **server memakai adapter JDBC, bukan penyimpanan memori**.

## Jalankan di laptop ini

Buka folder proyek di VS Code, buka Terminal PowerShell, lalu:

```powershell
cd D:\Coding\Code\Rsi_Kelompok5
powershell -ExecutionPolicy Bypass -File .\run.ps1
```

Buka **http://localhost:8080/** (halaman login). Gunakan navigasi di atas halaman untuk fitur lainnya. Jangan membuka HTML dengan klik ganda/file:// atau Live Server: API dan HTML dilayani dari Java pada alamat yang sama.

`run.ps1` mengompilasi Java, menyalakan database khusus proyek pada `127.0.0.1:3307`, menyiapkan skema, lalu menjalankan server. MySQL lama pada port 3306 tidak digunakan. Database proyek bernama `rsi_kelompok5`; berkas database berada di `.runtime/mysql`. Ruang A/B/C merupakan master awal dari UI asli; tidak ada pengajuan contoh yang dimasukkan ke database utama.

Kebutuhan: JDK 17+ (di laptop ini diuji JDK 25.0.2), PowerShell, MySQL Server 8.0 (di laptop ini 8.0.45). Driver JDBC MySQL 9.2.0 dan Gson 2.12.1 disimpan di `lib/`; `setup.ps1` mengunduhnya dari Maven Central jika belum ada. Maven tidak diperlukan.

Jika lokasi MySQL berbeda:

```powershell
$env:MYSQL_BASE = 'C:\Program Files\MySQL\MySQL Server 8.0'
.\run.ps1
```

Kredensial database khusus proyek dibuat acak dan disimpan terenkripsi dengan DPAPI di `.runtime/database-credential.xml`, bukan di source code. Hanya akun Windows yang membuatnya dapat membacanya. Skrip memasukkannya ke `DB_USER` dan `DB_PASSWORD` saat berjalan. Jangan menghapus file kredensial, membagikan `.runtime`, atau mengunggahnya ke Git. Direktori data bukan cadangan portabel; untuk pindah laptop gunakan dump SQL dan kredensial baru.

Untuk menghentikan Java gunakan Ctrl+C pada terminal. Database dapat dihentikan dengan:

```powershell
powershell -ExecutionPolicy Bypass -File .\stop-database.ps1
```

## Coba alur program

1. Login sebagai **admin**, lalu buka **Barang**: tambahkan barang, pilih **Lihat Detail**, kemudian **Daftarkan sebagai alat multimedia** bila barang tersebut memang alat yang dipinjamkan. Hubungan dibuat secara eksplisit; ID barang lama tidak ditebak sebagai ID alat.
2. **Inspeksi**: pilih barang dan simpan kondisi **Layak**. Baru setelah itu alat terkait dapat dipinjam. Jika riwayat terakhir dihapus, ringkasan mengikuti riwayat tersisa (tanggal lalu urutan pencatatan). Riwayat kosong menjadi **Belum Diperiksa**.
3. Login sebagai **mahasiswa**, buka **Peminjaman**: pilih alat, tanggal, waktu, dan keperluan. ID mahasiswa diambil dari sesi login. **Admin peminjaman** menampilkan pengajuan yang sama dan dapat menyetujui/menolak sekali. Persetujuan mengubah status alat menjadi **Dipinjam**.
4. **Reservasi**: cek tanggal/jam, pilih ruangan, kirim pengajuan; proses dari **Admin reservasi**. Persetujuan yang bentrok atau pengajuan yang sudah final ditolak server.
5. Sebagai mahasiswa, buka **Laporan insiden**: isi fasilitas, lokasi, detail, dan foto JPEG/PNG maksimal 5 MiB (5 x 1024 x 1024 byte), maksimal 25 megapiksel. Tiket dan foto disimpan dalam database. Daftar di bawah form menyediakan foto dan pembaruan status/catatan.

Alur peminjaman tetap memakai status alat global seperti controller Modul 3, bukan kalender durasi atau alur pengembalian baru. Reservasi memakai tanggal dan jam yang sama sebagai satu slot, seperti kode awal. Aplikasi ini untuk penggunaan lokal di laptop. Login membatasi halaman dan API berdasarkan peran mahasiswa/admin; tidak dipublikasikan ke jaringan.

## Database yang dikelola sendiri (opsional)

Gunakan database baru/kosong khusus proyek. `sql/schema.sql` tidak berisi DROP/TRUNCATE dan tidak menimpa data, tetapi bukan migrasi otomatis untuk skema lama yang berbeda. Periksa skema/data lama terlebih dahulu.

```powershell
. .\config.example.ps1
.\run.ps1 -UseEnvironment
```

Sesuaikan DB_URL/DB_USER, lalu isi password lewat prompt kredensial. DB_URL tidak boleh memuat password. Untuk mengganti port HTTP: `$env:PORT='8082'`.

## Mempertahankan data browser lama

Tidak ada skrip yang menghapus `siprf_barang` atau `siprf_reservasi`. Browser membatasi localStorage berdasarkan alamat asal, sehingga data dari file:// atau Live Server tidak otomatis terlihat di localhost:8080.

Buka halaman lama pada alamat asal, buka Developer Tools > Console, lalu jalankan ekspor berikut. Ini hanya membaca data dan mengunduh JSON:

```javascript
const dataLama = {
  barang: JSON.parse(localStorage.getItem('siprf_barang') || '[]'),
  reservasi: JSON.parse(localStorage.getItem('siprf_reservasi') || '[]')
};
const tautanEkspor = document.createElement('a');
const urlEkspor = URL.createObjectURL(new Blob([JSON.stringify(dataLama, null, 2)], {type:'application/json'}));
tautanEkspor.href = urlEkspor;
tautanEkspor.download = 'data-rsi-lama.json';
tautanEkspor.click();
setTimeout(() => URL.revokeObjectURL(urlEkspor), 1000);
```

Pada `MigrasiDataUI.html`, pilih JSON, periksa pratinjau, lalu klik **Impor data yang ditampilkan**. Impor mempertahankan ID, menyimpan salinan payload asal, menghindari duplikasi impor, dan menolak benturan ID/jadwal. Jika ada konflik, seluruh transaksi dibatalkan. Ringkasan kelayakan dihitung dari riwayat valid. Data browser asal tetap utuh. Data contoh hardcoded di admin lama bukan pengajuan pengguna dan tidak diimpor otomatis.

## Pengujian ulang (opsional)

```powershell
powershell -ExecutionPolicy Bypass -File .\test.ps1
powershell -ExecutionPolicy Bypass -File .\test-auth.ps1
```

Tes memakai database baru `rsi_test_<waktu>` pada instance proyek dan port HTTP 8081. Tidak menghapus database utama atau database tes sebelumnya. Selain Java, tes memerlukan Python 3, Node.js, Playwright, dan Chromium. Pengujian browser di laptop ini memakai runtime Playwright yang tersedia; pada laptop lain install Playwright untuk pengujian atau isi `PLAYWRIGHT_MODULE` (lokasi paket) dan `PLAYWRIGHT_CHROMIUM` (executable Chromium). Semua ini hanya kebutuhan tes, bukan kebutuhan menjalankan aplikasi.

Tes mencakup keputusan bersamaan/berulang, bentrok reservasi, perubahan status inspeksi, batas foto, impor dan rollback, restart Java, dan interaksi browser. Output aktual dicetak ke terminal; log server tes berada di `.runtime/test-server.log`.

Jika port sibuk, hentikan proses proyek sebelumnya atau gunakan PORT lain. Jika DB gagal, lihat `.runtime/mysql.log`; jangan menghapus `.runtime/mysql` untuk mencoba memperbaikinya karena folder itu berisi data.

## Login, dashboard, dan sidebar

Alamat utama `/` mengarahkan pengguna tanpa sesi ke LoginUI.html. Setelah masuk, dashboard dan menu kiri disesuaikan peran:

- `admin`: dashboard, kelola reservasi/peminjaman, barang, inspeksi, kelola insiden, impor data lama.
- `mahasiswa`: dashboard, reservasi, peminjaman, laporan insiden, pengajuan saya.

Dua akun dibuat otomatis pada startup pertama. Password acak disimpan terenkripsi DPAPI pada `.runtime/demo-accounts.xml`; database hanya menyimpan salt dan hash PBKDF2 (210.000 iterasi). Untuk melihat kredensial pada laptop ini, jalankan pada terminal baru dari folder proyek:

```powershell
. .\start-database.ps1
.\setup-accounts.ps1 -ShowCredentials
```

Jangan unggah `.runtime` ke GitHub. Instalasi baru menghasilkan password sendiri. Akun yang sudah ada tidak ditimpa. Sesi menggunakan cookie HttpOnly, SameSite=Strict, berlaku 8 jam; logout mencabutnya di database. Lima kegagalan login mengunci akun sementara selama satu menit.

Mahasiswa hanya melihat data yang dibuat lewat akunnya, termasuk akses foto. Data lama sebelum login tetap tersedia bagi admin, tanpa otomatis mengaitkannya dengan akun demo meskipun teks ID mahasiswa sama. Ketersediaan ruangan dibagikan sebagai slot terisi tanpa nama/keperluan pemohon. Skema login menambah Akun, SesiLogin, dan PemilikData tanpa menghapus tabel lama.

`test-auth.ps1` menguji otorisasi, isolasi data, identitas dari sesi, logout, restart, dashboard, sidebar, dan browser desktop/mobile pada database uji terpisah. Akun `other` hanya dibuat di database uji untuk memeriksa isolasi, bukan di database utama.
