import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Penyimpanan sementara; semua instance berbagi daftar alat dalam JVM yang sama. */
public class AlatMultimedia {
    public static final String TERSEDIA = "Tersedia";
    public static final String DIPINJAM = "Dipinjam";
    private static final Map<String, AlatMultimedia> DAFTAR = new LinkedHashMap<>();
    private String idAlat;
    private String namaAlat;
    private String status;

    public AlatMultimedia() { }

    public AlatMultimedia(String idAlat, String namaAlat) {
        this(idAlat, namaAlat, TERSEDIA);
    }

    public AlatMultimedia(String idAlat, String namaAlat, String status) {
        this.idAlat = idAlat;
        this.namaAlat = namaAlat;
        this.status = status;
    }

    public boolean tambahAlat(AlatMultimedia alat) {
        if (alat == null || alat.idAlat == null || alat.idAlat.trim().isEmpty()
                || alat.idAlat.length() > 10 || alat.namaAlat == null
                || alat.namaAlat.trim().isEmpty() || !statusValid(alat.status)
                || DAFTAR.containsKey(alat.idAlat)) {
            return false;
        }
        DAFTAR.put(alat.idAlat, new AlatMultimedia(
                alat.idAlat, alat.namaAlat.trim(), alat.status));
        return true;
    }

    public String getStatus(String idAlat) {
        AlatMultimedia alat = DAFTAR.get(idAlat);
        return alat == null ? null : alat.status;
    }

    public Boolean updateStatusAlat(String idAlat, String status) {
        AlatMultimedia alat = DAFTAR.get(idAlat);
        if (alat == null || !statusValid(status)) return false;
        alat.status = status;
        return true;
    }

    public AlatMultimedia getDetailAlat(String idAlat) {
        AlatMultimedia alat = DAFTAR.get(idAlat);
        return alat == null ? null : new AlatMultimedia(alat.idAlat, alat.namaAlat, alat.status);
    }

    public List<AlatMultimedia> getDaftarAlat() {
        List<AlatMultimedia> hasil = new ArrayList<>();
        for (String id : DAFTAR.keySet()) hasil.add(getDetailAlat(id));
        return hasil;
    }

    private static boolean statusValid(String status) {
        return TERSEDIA.equals(status) || DIPINJAM.equals(status);
    }

    public String getIdAlat() { return idAlat; }
    public String getNamaAlat() { return namaAlat; }
    public String getStatus() { return status; }
}
