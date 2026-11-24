import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Entitas dan penyimpanan pengajuan sementara, bukan koneksi database. */
public class Pengajuan {
    public static final String MENUNGGU = "Menunggu Persetujuan";
    public static final String DISETUJUI = "Disetujui";
    public static final String DITOLAK = "Ditolak";
    private static final Map<String, Pengajuan> DAFTAR = new LinkedHashMap<>();
    private static long nomor = 0;
    private String idPengajuan;
    private String idAlat;
    private String idMhs;
    private Date tglPinjam;
    private LocalTime waktu;
    private String keperluan;
    private String statusPengajuan = MENUNGGU;

    public Pengajuan() { }

    // Konstruktor pembacaan JDBC; parameter/method lama tetap tersedia.
    public Pengajuan(String idPengajuan, String idAlat, String idMhs, Date tglPinjam,
                     LocalTime waktu, String keperluan, String statusPengajuan) {
        this(idAlat, idMhs, tglPinjam, waktu, keperluan);
        this.idPengajuan = idPengajuan;
        setStatusPengajuan(statusPengajuan);
    }

    public Pengajuan(String idAlat, String idMhs, Date tglPinjam,
                     LocalTime waktu, String keperluan) {
        this.idAlat = idAlat;
        this.idMhs = idMhs;
        setTglPinjam(tglPinjam);
        this.waktu = waktu;
        this.keperluan = keperluan;
    }

    public String simpanPengajuan(Pengajuan data) {
        if (data == null || kosong(data.idAlat) || data.idAlat.length() > 10
                || kosong(data.idMhs) || data.idMhs.length() > 15
                || data.tglPinjam == null || data.waktu == null
                || kosong(data.keperluan) || data.idPengajuan != null
                || new AlatMultimedia().getDetailAlat(data.idAlat) == null) {
            return null;
        }
        String id = String.format("PJM%03d", ++nomor);
        Pengajuan tersimpan = salin(data);
        tersimpan.idPengajuan = id;
        tersimpan.statusPengajuan = MENUNGGU;
        DAFTAR.put(id, tersimpan);
        data.idPengajuan = id;
        data.statusPengajuan = MENUNGGU;
        return id;
    }

    public List<Pengajuan> getAll() {
        List<Pengajuan> hasil = new ArrayList<>();
        for (Pengajuan data : DAFTAR.values()) hasil.add(salin(data));
        return hasil;
    }

    public Pengajuan getById(String idPengajuan) {
        Pengajuan data = DAFTAR.get(idPengajuan);
        return data == null ? null : salin(data);
    }

    public String getIdAlat(String idPengajuan) {
        Pengajuan data = DAFTAR.get(idPengajuan);
        return data == null ? null : data.idAlat;
    }

    // ID wajib diberikan supaya keputusan diterapkan ke pengajuan yang benar.
    public Boolean setStatus(String idPengajuan, String keputusan) {
        Pengajuan data = DAFTAR.get(idPengajuan);
        if (data == null || !MENUNGGU.equals(data.statusPengajuan)
                || (!DISETUJUI.equals(keputusan) && !DITOLAK.equals(keputusan))) {
            return false;
        }
        data.statusPengajuan = keputusan;
        return true;
    }

    private static boolean kosong(String teks) {
        return teks == null || teks.trim().isEmpty();
    }

    private static Pengajuan salin(Pengajuan data) {
        Pengajuan hasil = new Pengajuan(data.idAlat, data.idMhs,
                data.tglPinjam, data.waktu, data.keperluan);
        hasil.idPengajuan = data.idPengajuan;
        hasil.statusPengajuan = data.statusPengajuan;
        return hasil;
    }

    public String getIdPengajuan() { return idPengajuan; }
    public String getIdAlat() { return idAlat; }
    public void setIdAlat(String idAlat) { this.idAlat = idAlat; }
    public String getIdMhs() { return idMhs; }
    public void setIdMhs(String idMhs) { this.idMhs = idMhs; }
    public Date getTglPinjam() { return tglPinjam == null ? null : new Date(tglPinjam.getTime()); }
    public void setTglPinjam(Date tanggal) {
        this.tglPinjam = tanggal == null ? null : new Date(tanggal.getTime());
    }
    public LocalTime getWaktu() { return waktu; }
    public void setWaktu(LocalTime waktu) { this.waktu = waktu; }
    public String getKeperluan() { return keperluan; }
    public void setKeperluan(String keperluan) { this.keperluan = keperluan; }
    public String getStatusPengajuan() { return statusPengajuan; }
    public void setStatusPengajuan(String status) {
        if (!MENUNGGU.equals(status) && !DISETUJUI.equals(status) && !DITOLAK.equals(status)) {
            throw new IllegalArgumentException("Status pengajuan tidak valid");
        }
        this.statusPengajuan = status;
    }
}
