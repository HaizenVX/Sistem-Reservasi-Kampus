import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Entitas Barang (CD-03).
 * Data disimpan sementara di memori.
 */
public class Barang {

    public static final String STATUS_BELUM = "Belum Diperiksa";
    public static final String STATUS_LAYAK = "Layak Digunakan";
    public static final String STATUS_PERBAIKAN = "Dalam Perbaikan";
    public static final String STATUS_TIDAK_LAYAK = "Tidak Layak";

    private static final List<Barang> DAFTAR_BARANG =
            new ArrayList<>();

    private static final DateTimeFormatter FORMAT_TANGGAL =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private String idBarang;
    private String namaBarang;
    private String kategori;
    private String kondisi;
    private String statusKelayakan;
    private LocalDate terakhirDiperiksa;

    private List<LaporanInspeksi> riwayatKondisi =
            new ArrayList<>();

    public Barang() {
        this.statusKelayakan = STATUS_BELUM;
        this.kondisi = "-";
    }

    public Barang(
            String idBarang,
            String namaBarang,
            String kategori,
            String statusKelayakan) {

        this.idBarang = idBarang;
        this.namaBarang = namaBarang;
        this.kategori = kategori;
        this.statusKelayakan = statusKelayakan;
        this.kondisi = "-";
    }

    public List<Barang> getBarangPerluInspeksi() {
        return new ArrayList<>(DAFTAR_BARANG);
    }

    public Barang getDetailBarang(String idBarang) {
        return cariBarang(idBarang);
    }

    public void updateStatusKelayakan(String idBarang, String status) {
        Barang barang = cariBarang(idBarang);

        if (barang != null && status != null && !status.isEmpty()) {
            barang.statusKelayakan = status;
        }
    }

    public List<Barang> getDaftarDanStatus() {
        return new ArrayList<>(DAFTAR_BARANG);
    }

    public List<LaporanInspeksi> getRiwayatKondisi(String idBarang) {
        Barang barang = cariBarang(idBarang);

        return barang == null
                ? new ArrayList<>()
                : new ArrayList<>(barang.riwayatKondisi);
    }

    public Barang getDetailDanRiwayat(String idBarang) {
        return cariBarang(idBarang);
    }

    public static Barang cariBarang(String idBarang) {
        if (idBarang == null) {
            return null;
        }

        for (Barang barang : DAFTAR_BARANG) {
            if (idBarang.equals(barang.idBarang)) {
                return barang;
            }
        }

        return null;
    }

    public String tambahBarang(String namaBarang, String kategori) {
        if (namaBarang == null
                || namaBarang.trim().isEmpty()
                || kategori == null
                || kategori.trim().isEmpty()) {

            return null;
        }

        int terbesar = 0;

        for (Barang barang : DAFTAR_BARANG) {
            terbesar = Math.max(
                    terbesar,
                    Integer.parseInt(barang.idBarang.substring(3))
            );
        }

        String id = String.format("BRG%03d", terbesar + 1);

        DAFTAR_BARANG.add(
                new Barang(
                        id,
                        namaBarang.trim(),
                        kategori.trim(),
                        STATUS_BELUM
                )
        );

        return id;
    }

    public boolean hapusBarang(String idBarang) {
        Barang barang = cariBarang(idBarang);

        if (barang == null) {
            return false;
        }

        LaporanInspeksi.hapusLaporanBarang(idBarang);
        return DAFTAR_BARANG.remove(barang);
    }

    public void tambahRiwayat(LaporanInspeksi laporan) {
        riwayatKondisi.add(laporan);
        kondisi = laporan.getKondisi();
        terakhirDiperiksa = laporan.getTanggalInspeksi();
    }

    public boolean hapusRiwayat(String idLaporan) {
        if (idLaporan == null) {
            return false;
        }

        boolean terhapus = riwayatKondisi.removeIf(
                laporan -> idLaporan.equals(laporan.getIdLaporan())
        );

        if (terhapus) {
            if (riwayatKondisi.isEmpty()) {
                kondisi = "-";
                terakhirDiperiksa = null;
                statusKelayakan = STATUS_BELUM;
            } else {
                LaporanInspeksi terakhir =
                        riwayatKondisi.get(riwayatKondisi.size() - 1);

                kondisi = terakhir.getKondisi();
                terakhirDiperiksa = terakhir.getTanggalInspeksi();

                if ("Layak".equals(kondisi)) {
                    statusKelayakan = STATUS_LAYAK;
                } else if ("Rusak Ringan".equals(kondisi)) {
                    statusKelayakan = STATUS_PERBAIKAN;
                } else {
                    statusKelayakan = STATUS_TIDAK_LAYAK;
                }
            }
        }

        return terhapus;
    }

    public boolean isDapatDipinjam() {
        return STATUS_LAYAK.equals(statusKelayakan);
    }

    public String getTerakhirDiperiksaTeks() {
        return terakhirDiperiksa == null
                ? "-"
                : terakhirDiperiksa.format(FORMAT_TANGGAL);
    }

    public static void kosongkanData() {
        DAFTAR_BARANG.clear();
        LaporanInspeksi.kosongkanData();
    }

    public String getIdBarang() {
        return idBarang;
    }

    public void setIdBarang(String idBarang) {
        this.idBarang = idBarang;
    }

    public String getNamaBarang() {
        return namaBarang;
    }

    public void setNamaBarang(String namaBarang) {
        this.namaBarang = namaBarang;
    }

    public String getKategori() {
        return kategori;
    }

    public void setKategori(String kategori) {
        this.kategori = kategori;
    }

    public String getKondisi() {
        return kondisi;
    }

    public void setKondisi(String kondisi) {
        this.kondisi = kondisi;
    }

    public String getStatusKelayakan() {
        return statusKelayakan;
    }

    public void setStatusKelayakan(String statusKelayakan) {
        this.statusKelayakan = statusKelayakan;
    }

    public LocalDate getTerakhirDiperiksa() {
        return terakhirDiperiksa;
    }

    public void setTerakhirDiperiksa(LocalDate terakhirDiperiksa) {
        this.terakhirDiperiksa = terakhirDiperiksa;
    }

    public List<LaporanInspeksi> getRiwayatKondisi() {
        return new ArrayList<>(riwayatKondisi);
    }

    public void setRiwayatKondisi(
            List<LaporanInspeksi> riwayatKondisi) {

        this.riwayatKondisi = riwayatKondisi == null
                ? new ArrayList<>()
                : new ArrayList<>(riwayatKondisi);
    }

    @Override
    public String toString() {
        return idBarang + " | "
                + namaBarang + " | "
                + kategori + " | "
                + statusKelayakan;
    }
}