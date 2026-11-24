import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Entitas LaporanInspeksi (CD-03).
 * Data disimpan sementara di memori.
 */
public class LaporanInspeksi {

    private static final List<LaporanInspeksi> DAFTAR_LAPORAN =
            new ArrayList<>();

    private static final DateTimeFormatter FORMAT_TANGGAL =
            DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private String idLaporan;
    private String idBarang;
    private String kondisi;
    private String catatanRekomendasi;
    private LocalDate tanggalInspeksi;

    public LaporanInspeksi() {
    }

    public LaporanInspeksi(
            String idLaporan,
            String idBarang,
            String kondisi,
            String catatanRekomendasi,
            LocalDate tanggalInspeksi) {

        this.idLaporan = idLaporan;
        this.idBarang = idBarang;
        this.kondisi = kondisi;
        this.catatanRekomendasi = catatanRekomendasi;
        this.tanggalInspeksi = tanggalInspeksi;
    }

    public String createLaporan(
            String idBarang,
            String kondisi,
            String catatanRekomendasi) {

        Barang barang = Barang.cariBarang(idBarang);

        if (barang == null || !kondisiValid(kondisi)) {
            return null;
        }

        int terbesar = 0;

        for (LaporanInspeksi laporan : DAFTAR_LAPORAN) {
            terbesar = Math.max(
                    terbesar,
                    Integer.parseInt(laporan.idLaporan.substring(3))
            );
        }

        String id = String.format("LPN%03d", terbesar + 1);

        LaporanInspeksi laporan = new LaporanInspeksi(
                id,
                idBarang,
                kondisi,
                catatanRekomendasi == null ? "" : catatanRekomendasi,
                LocalDate.now()
        );

        DAFTAR_LAPORAN.add(laporan);
        barang.tambahRiwayat(laporan);

        return id;
    }

    public static boolean kondisiValid(String kondisi) {
        return "Layak".equals(kondisi)
                || "Rusak Ringan".equals(kondisi)
                || "Rusak Berat".equals(kondisi);
    }

    public List<LaporanInspeksi> getDaftarLaporan() {
        return new ArrayList<>(DAFTAR_LAPORAN);
    }

    public boolean hapusLaporan(String idLaporan) {
        if (idLaporan == null) {
            return false;
        }

        for (int i = 0; i < DAFTAR_LAPORAN.size(); i++) {
            LaporanInspeksi laporan = DAFTAR_LAPORAN.get(i);

            if (idLaporan.equals(laporan.idLaporan)) {
                Barang barang = Barang.cariBarang(laporan.idBarang);

                if (barang != null) {
                    barang.hapusRiwayat(idLaporan);
                }

                DAFTAR_LAPORAN.remove(i);
                return true;
            }
        }

        return false;
    }

    public static void hapusLaporanBarang(String idBarang) {
        if (idBarang == null) {
            return;
        }

        DAFTAR_LAPORAN.removeIf(
                laporan -> idBarang.equals(laporan.idBarang)
        );
    }

    public static void kosongkanData() {
        DAFTAR_LAPORAN.clear();
    }

    public String getTanggalInspeksiTeks() {
        return tanggalInspeksi == null
                ? "-"
                : tanggalInspeksi.format(FORMAT_TANGGAL);
    }

    public String getIdLaporan() {
        return idLaporan;
    }

    public void setIdLaporan(String idLaporan) {
        this.idLaporan = idLaporan;
    }

    public String getIdBarang() {
        return idBarang;
    }

    public void setIdBarang(String idBarang) {
        this.idBarang = idBarang;
    }

    public String getKondisi() {
        return kondisi;
    }

    public void setKondisi(String kondisi) {
        this.kondisi = kondisi;
    }

    public String getCatatanRekomendasi() {
        return catatanRekomendasi;
    }

    public void setCatatanRekomendasi(String catatanRekomendasi) {
        this.catatanRekomendasi = catatanRekomendasi;
    }

    public LocalDate getTanggalInspeksi() {
        return tanggalInspeksi;
    }

    public void setTanggalInspeksi(LocalDate tanggalInspeksi) {
        this.tanggalInspeksi = tanggalInspeksi;
    }

    @Override
    public String toString() {
        return idLaporan + " | "
                + idBarang + " | "
                + kondisi + " | "
                + getTanggalInspeksiTeks();
    }
}