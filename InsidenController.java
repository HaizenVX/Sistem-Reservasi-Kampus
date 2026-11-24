import java.util.List;
import java.util.Objects;

public class InsidenController {
    private final TiketInsiden tiketInsiden;

    public InsidenController(TiketInsiden tiketInsiden) {
        this.tiketInsiden = Objects.requireNonNull(tiketInsiden);
    }

    // PC-04A: String fotoBukti tidak cukup untuk memvalidasi ukuran/isi foto.
    public String validasiDanSimpanLaporan(String idMhs, String detailKerusakan,
                                          String lokasi, String fotoBukti) {
        if (kosong(idMhs) || kosong(detailKerusakan) || kosong(lokasi) || kosong(fotoBukti)) {
            return "Formulir laporan belum lengkap";
        }
        String idTiket = tiketInsiden.createTiket(idMhs, detailKerusakan, lokasi, fotoBukti);
        return idTiket == null ? "Gagal membuat tiket laporan"
                : "Laporan Berhasil Dibuat dengan ID Tiket: " + idTiket;
    }

    // PC-04B
    public String getStatusLaporan(String idTiket) {
        TiketInsiden tiket = tiketInsiden.getTiketById(idTiket);
        return tiket == null ? "Tiket tidak ditemukan" : tiket.getStatusPenanganan();
    }

    // PC-04C
    public List<TiketInsiden> getDaftarTiket() {
        return tiketInsiden.getDaftarTiket();
    }

    // PC-04D
    public TiketInsiden getDetailTiket(String idTiket) {
        return tiketInsiden.getTiketById(idTiket);
    }

    // PC-04E: catatan hanya diubah jika status berhasil diperbarui.
    public Boolean simpanPembaruanStatus(String idTiket, String statusPenanganan, String catatan) {
        TiketInsiden tiket = tiketInsiden.getTiketById(idTiket);
        if (tiket == null || !tiket.setStatus(statusPenanganan)) return false;
        tiket.setCatatan(catatan);
        return true;
    }

    private static boolean kosong(String teks) {
        return teks == null || teks.trim().isEmpty();
    }
}
