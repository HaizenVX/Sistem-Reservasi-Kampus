-- Skema integrasi Modul 3. Jalankan hanya pada database proyek khusus.
-- Baris TiketInsiden pada PDF berisi DDL Barang; tabel tiket dilengkapi di sini.
CREATE TABLE IF NOT EXISTS AppLock (id INT PRIMARY KEY);
INSERT IGNORE INTO AppLock VALUES (1);
CREATE TABLE IF NOT EXISTS Nomor (jenis VARCHAR(10) PRIMARY KEY, nilai BIGINT NOT NULL);
CREATE TABLE IF NOT EXISTS Ruangan (idRuangan VARCHAR(10) PRIMARY KEY, namaRuangan VARCHAR(100) NOT NULL);
-- Master ruangan yang sudah ada pada data-reservasi.js; tidak menimpa baris lama.
INSERT IGNORE INTO Ruangan(idRuangan,namaRuangan) VALUES ('R001','Ruang A'),('R002','Ruang B'),('R003','Ruang C');
CREATE TABLE IF NOT EXISTS Reservasi (
 idReservasi VARCHAR(15) PRIMARY KEY, idRuangan VARCHAR(10) NOT NULL,
 tanggal DATE NOT NULL, waktu TIME NOT NULL, keperluan VARCHAR(255) NOT NULL,
 status VARCHAR(25) NOT NULL DEFAULT 'Menunggu Persetujuan',
 FOREIGN KEY (idRuangan) REFERENCES Ruangan(idRuangan),
 CHECK (status IN ('Menunggu Persetujuan','Disetujui','Ditolak')),
 INDEX jadwal (idRuangan,tanggal,waktu,status)
);
CREATE TABLE IF NOT EXISTS Barang (
 idBarang VARCHAR(15) PRIMARY KEY, namaBarang VARCHAR(100) NOT NULL, kategori VARCHAR(25) NOT NULL,
 kondisi VARCHAR(25) NOT NULL DEFAULT '-', terakhirDiperiksa DATE NULL,
 statusKelayakan VARCHAR(25) NOT NULL DEFAULT 'Belum Diperiksa',
 CHECK (statusKelayakan IN ('Belum Diperiksa','Layak Digunakan','Dalam Perbaikan','Tidak Layak'))
);
CREATE TABLE IF NOT EXISTS AlatMultimedia (
 idAlat VARCHAR(10) PRIMARY KEY, namaAlat VARCHAR(100) NOT NULL,
 status VARCHAR(20) NOT NULL DEFAULT 'Tersedia', idBarang VARCHAR(15) NULL UNIQUE,
 FOREIGN KEY (idBarang) REFERENCES Barang(idBarang), CHECK (status IN ('Tersedia','Dipinjam'))
);
CREATE TABLE IF NOT EXISTS Pengajuan (
 idPengajuan VARCHAR(15) PRIMARY KEY, idAlat VARCHAR(10) NOT NULL, idMhs VARCHAR(15) NOT NULL,
 tglPinjam DATE NOT NULL, waktu TIME NOT NULL, keperluan TEXT NOT NULL,
 statusPengajuan VARCHAR(25) NOT NULL DEFAULT 'Menunggu Persetujuan',
 FOREIGN KEY (idAlat) REFERENCES AlatMultimedia(idAlat),
 CHECK (statusPengajuan IN ('Menunggu Persetujuan','Disetujui','Ditolak'))
);
CREATE TABLE IF NOT EXISTS LaporanInspeksi (
 idLaporan VARCHAR(15) PRIMARY KEY, idBarang VARCHAR(15) NOT NULL, kondisi VARCHAR(25) NOT NULL,
 catatanRekomendasi VARCHAR(255), tanggalInspeksi DATE NOT NULL,
 urutan BIGINT NOT NULL AUTO_INCREMENT UNIQUE,
 FOREIGN KEY (idBarang) REFERENCES Barang(idBarang),
 CHECK (kondisi IN ('Layak','Rusak Ringan','Rusak Berat'))
);
CREATE TABLE IF NOT EXISTS FotoInsiden (
 idFoto VARCHAR(15) PRIMARY KEY, mime VARCHAR(30) NOT NULL, isi MEDIUMBLOB NOT NULL
);
CREATE TABLE IF NOT EXISTS TiketInsiden (
 idTiket VARCHAR(15) PRIMARY KEY, idMhs VARCHAR(15) NOT NULL, detailKerusakan VARCHAR(500) NOT NULL,
 lokasi VARCHAR(255) NOT NULL, fotoBukti VARCHAR(15) NOT NULL,
 statusPenanganan VARCHAR(20) NOT NULL DEFAULT 'Dilaporkan', catatan VARCHAR(500) NOT NULL DEFAULT '',
 tanggalLaporan DATE NOT NULL,
 FOREIGN KEY (fotoBukti) REFERENCES FotoInsiden(idFoto),
 CHECK (statusPenanganan IN ('Dilaporkan','Diproses','Selesai'))
);
CREATE TABLE IF NOT EXISTS ImporLama (jenis VARCHAR(20) NOT NULL, idLama VARCHAR(100) NOT NULL, isi MEDIUMTEXT NOT NULL, PRIMARY KEY(jenis,idLama));
-- Penambahan login tanpa mengubah atau mengklaim pemilik data lama.
CREATE TABLE IF NOT EXISTS Akun (
 username VARCHAR(50) PRIMARY KEY, nama VARCHAR(100) NOT NULL, role VARCHAR(15) NOT NULL,
 idMhs VARCHAR(15) UNIQUE, salt VARCHAR(50) NOT NULL, passwordHash VARCHAR(100) NOT NULL,
 gagalLogin INT NOT NULL DEFAULT 0, terkunciSampai DATETIME NULL,
 CHECK (role IN ('admin','mahasiswa'))
);
CREATE TABLE IF NOT EXISTS SesiLogin (
 tokenHash CHAR(64) PRIMARY KEY, username VARCHAR(50) NOT NULL, kedaluwarsa DATETIME NOT NULL,
 FOREIGN KEY (username) REFERENCES Akun(username)
);
CREATE TABLE IF NOT EXISTS PemilikData (
 jenis VARCHAR(20) NOT NULL, idRecord VARCHAR(15) NOT NULL, username VARCHAR(50) NOT NULL,
 PRIMARY KEY(jenis,idRecord), FOREIGN KEY(username) REFERENCES Akun(username)
);
