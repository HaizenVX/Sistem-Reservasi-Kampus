import java.util.List;
import java.util.Objects;

public class InspeksiController {
    private final Barang barang;
    private final LaporanInspeksi laporanInspeksi;

    public InspeksiController(Barang barang, LaporanInspeksi laporanInspeksi) {
        this.barang = Objects.requireNonNull(barang);
        this.laporanInspeksi = Objects.requireNonNull(laporanInspeksi);
    }

    // PC-03A
    public List<Barang> getDaftarBarang() {
        return barang.getBarangPerluInspeksi();
    }

    // PC-03B
    public Barang getFormInspeksi(String idBarang) {
        return barang.getDetailBarang(idBarang);
    }

    // PC-03C: mengikuti konstanta Barang.java dan status pada UI inspeksi.
    public String validasiDanSimpan(String idBarang, String kondisi,
                                    String catatanRekomendasi) {
        if (idBarang == null || idBarang.trim().isEmpty()
                || kondisi == null || kondisi.trim().isEmpty()) {
            return "Data inspeksi belum lengkap";
        }
        if (!LaporanInspeksi.kondisiValid(kondisi)) {
            return "Kriteria kondisi tidak valid";
        }
        String idLaporan = laporanInspeksi.createLaporan(
                idBarang, kondisi, catatanRekomendasi == null ? "" : catatanRekomendasi);
        if (idLaporan == null) {
            return "Laporan inspeksi gagal disimpan";
        }
        String status = Barang.STATUS_TIDAK_LAYAK;
        if ("Layak".equals(kondisi)) {
            status = Barang.STATUS_LAYAK;
        } else if ("Rusak Ringan".equals(kondisi)) {
            status = Barang.STATUS_PERBAIKAN;
        }
        barang.updateStatusKelayakan(idBarang, status);
        return "Laporan berhasil disimpan";
    }

    // PC-03D
    public List<Barang> getDaftarDanStatus() {
        return barang.getDaftarDanStatus();
    }

    // PC-03E
    public Barang getDetailKelayakan(String idBarang) {
        return barang.getDetailBarang(idBarang);
    }
}
