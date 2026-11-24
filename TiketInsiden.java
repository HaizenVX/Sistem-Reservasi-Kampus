import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Model pendukung controller; fotoBukti berupa referensi, bukan unggahan berkas. */
public class TiketInsiden {
    // Pilihan status simulasi karena controller Modul 3 tidak menetapkan daftarnya.
    public static final String DILAPORKAN = "Dilaporkan";
    public static final String DIPROSES = "Diproses";
    public static final String SELESAI = "Selesai";
    private static final Map<String, TiketInsiden> DAFTAR = new LinkedHashMap<>();
    private static long nomor = 0;
    private String idTiket;
    private String idMhs;
    private String detailKerusakan;
    private String lokasi;
    private String fotoBukti;
    private String statusPenanganan;
    private String catatan = "";
    private LocalDate tanggalLaporan;

    public TiketInsiden() { }

    // Konstruktor pembacaan JDBC, tanpa mengubah signature method Modul 3.
    public TiketInsiden(String idTiket, String idMhs, String detailKerusakan, String lokasi,
                        String fotoBukti, String statusPenanganan, String catatan, LocalDate tanggalLaporan) {
        this.idTiket = idTiket; this.idMhs = idMhs; this.detailKerusakan = detailKerusakan;
        this.lokasi = lokasi; this.fotoBukti = fotoBukti; this.statusPenanganan = statusPenanganan;
        this.catatan = catatan; this.tanggalLaporan = tanggalLaporan;
    }

    public String createTiket(String idMhs, String detailKerusakan,
                              String lokasi, String fotoBukti) {
        if (kosong(idMhs) || kosong(detailKerusakan) || kosong(lokasi) || kosong(fotoBukti)) {
            return null;
        }
        TiketInsiden tiket = new TiketInsiden();
        tiket.idTiket = String.format("TKT%03d", ++nomor);
        tiket.idMhs = idMhs.trim();
        tiket.detailKerusakan = detailKerusakan.trim();
        tiket.lokasi = lokasi.trim();
        tiket.fotoBukti = fotoBukti.trim();
        tiket.statusPenanganan = DILAPORKAN;
        tiket.tanggalLaporan = LocalDate.now();
        DAFTAR.put(tiket.idTiket, tiket);
        return tiket.idTiket;
    }

    public TiketInsiden getTiketById(String idTiket) {
        return DAFTAR.get(idTiket);
    }

    public List<TiketInsiden> getDaftarTiket() {
        return new ArrayList<>(DAFTAR.values());
    }

    public boolean setStatus(String status) {
        if (!DILAPORKAN.equals(status) && !DIPROSES.equals(status) && !SELESAI.equals(status)) {
            return false;
        }
        this.statusPenanganan = status;
        return true;
    }

    private static boolean kosong(String teks) {
        return teks == null || teks.trim().isEmpty();
    }

    public String getIdTiket() { return idTiket; }
    public String getIdMhs() { return idMhs; }
    public String getDetailKerusakan() { return detailKerusakan; }
    public String getLokasi() { return lokasi; }
    public String getFotoBukti() { return fotoBukti; }
    public String getStatusPenanganan() { return statusPenanganan; }
    public String getCatatan() { return catatan; }
    public void setCatatan(String catatan) { this.catatan = catatan == null ? "" : catatan; }
    public LocalDate getTanggalLaporan() { return tanggalLaporan; }
}
