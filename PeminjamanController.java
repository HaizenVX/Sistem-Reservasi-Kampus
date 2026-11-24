import java.util.Date;
import java.util.List;
import java.util.Objects;

public class PeminjamanController {
    private final AlatMultimedia alatMultimedia;
    private final Pengajuan pengajuan;

    public PeminjamanController(AlatMultimedia alatMultimedia, Pengajuan pengajuan) {
        this.alatMultimedia = Objects.requireNonNull(alatMultimedia);
        this.pengajuan = Objects.requireNonNull(pengajuan);
    }

    // PC-02A: mengikuti modul: status alat global, belum kalender per tanggal/jam.
    public Boolean cekKetersediaan(String idAlat, Date tglPinjam) {
        return tglPinjam != null
                && AlatMultimedia.TERSEDIA.equals(alatMultimedia.getStatus(idAlat));
    }

    // PC-02B
    public String prosesPengajuan(Pengajuan dataLengkap) {
        if (dataLengkap == null) return "Data pengajuan belum lengkap";
        if (!cekKetersediaan(dataLengkap.getIdAlat(), dataLengkap.getTglPinjam())) {
            return "Alat tidak tersedia atau tanggal belum diisi";
        }
        dataLengkap.setStatusPengajuan(Pengajuan.MENUNGGU);
        String id = pengajuan.simpanPengajuan(dataLengkap);
        return id != null ? "Pengajuan peminjaman alat multimedia berhasil dikirim"
                : "Pengajuan gagal disimpan";
    }

    // PC-02C
    public List<Pengajuan> getDaftar() {
        return pengajuan.getAll();
    }

    // PC-02D: verifikasi berdasarkan ID; jangan mengubah pengajuan yang sudah diproses.
    public String updateStatusPengajuan(String idPengajuan, String keputusan) {
        if (!Pengajuan.DISETUJUI.equals(keputusan) && !Pengajuan.DITOLAK.equals(keputusan)) {
            return "Keputusan tidak valid";
        }
        Pengajuan data = pengajuan.getById(idPengajuan);
        if (data == null || !Pengajuan.MENUNGGU.equals(data.getStatusPengajuan())) {
            return "Gagal memproses verifikasi";
        }
        if (Pengajuan.DISETUJUI.equals(keputusan)
                && !cekKetersediaan(data.getIdAlat(), data.getTglPinjam())) {
            return "Alat tidak tersedia";
        }
        if (!pengajuan.setStatus(idPengajuan, keputusan)) {
            return "Gagal memproses verifikasi";
        }
        if (Pengajuan.DISETUJUI.equals(keputusan)) {
            alatMultimedia.updateStatusAlat(data.getIdAlat(), AlatMultimedia.DIPINJAM);
            return "Pengajuan disetujui dan status alat diperbarui";
        }
        return "Pengajuan peminjaman telah ditolak";
    }
}
