import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public class ReservasiController {

    private final Ruangan ruangan;
    private final Reservasi reservasi;

    public ReservasiController(
            Ruangan ruangan, Reservasi reservasi) {
        this.ruangan = ruangan;
        this.reservasi = reservasi;
    }

    // PC-01A: Mengecek ketersediaan ruangan.
    public List<Ruangan> cekKetersediaan(
            LocalDate tanggal, LocalTime waktu) {
        return ruangan.getKetersediaan(tanggal, waktu);
    }

    // PC-01B: Menyimpan pengajuan reservasi.
    public String simpanReservasi(Reservasi dataReservasi) {
        // createReservasi menggunakan default status dari database.
        Reservasi dataTersimpan =
                reservasi.createReservasi(dataReservasi);

        if (dataTersimpan != null) {
            return "Pengajuan berhasil disimpan";
        }

        return "Pengajuan gagal disimpan";
    }

    // PC-01C: Mengambil daftar pengajuan.
    public List<Reservasi> getPengajuan() {
        return reservasi.getDaftarReservasi();
    }

    // PC-01D: Mengambil detail pengajuan.
    public Reservasi getDetailReservasi(String idReservasi) {
        return reservasi.getDataReservasi(idReservasi);
    }

    // PC-01E: Memproses keputusan Admin.
    public Boolean prosesReservasi(
            String idReservasi, String keputusan) {

        if ("Disetujui".equals(keputusan)
                || "Ditolak".equals(keputusan)) {
            return reservasi.updateStatus(
                    idReservasi, keputusan);
        }

        return false;
    }
}