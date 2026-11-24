import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class Ruangan {

    private String idRuangan;
    private String namaRuangan;
    private Boolean statusKetersediaan;
    private final Connection koneksi;

    public Ruangan(Connection koneksi) {
        this.koneksi = koneksi;
    }

    public String getIdRuangan() {
        return idRuangan;
    }

    public String getNamaRuangan() {
        return namaRuangan;
    }

    public Boolean getStatusKetersediaan() {
        return statusKetersediaan;
    }

    public List<Ruangan> getKetersediaan(
            LocalDate tanggal, LocalTime waktu) {

        List<Ruangan> daftarRuangan = new ArrayList<>();

        String sql =
                "SELECT r.idRuangan, r.namaRuangan "
                + "FROM Ruangan r "
                + "WHERE NOT EXISTS ("
                + "SELECT 1 FROM Reservasi rs "
                + "WHERE rs.idRuangan = r.idRuangan "
                + "AND rs.tanggal = ? "
                + "AND rs.waktu = ? "
                + "AND rs.status = 'Disetujui'"
                + ") "
                + "ORDER BY r.idRuangan";

        try (PreparedStatement statement =
                koneksi.prepareStatement(sql)) {

            statement.setDate(
                    1, java.sql.Date.valueOf(tanggal));
            statement.setTime(
                    2, java.sql.Time.valueOf(waktu));

            try (ResultSet hasil = statement.executeQuery()) {
                while (hasil.next()) {
                    Ruangan ruangan = new Ruangan(koneksi);

                    ruangan.idRuangan =
                            hasil.getString("idRuangan");
                    ruangan.namaRuangan =
                            hasil.getString("namaRuangan");
                    ruangan.statusKetersediaan = true;

                    daftarRuangan.add(ruangan);
                }
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Gagal membaca ketersediaan ruangan", e);
        }

        return daftarRuangan;
    }
}