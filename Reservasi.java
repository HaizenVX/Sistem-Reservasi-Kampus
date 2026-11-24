import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

public class Reservasi {

    private String idReservasi;
    private String idRuangan;
    private LocalDate tanggal;
    private LocalTime waktu;
    private String keperluan;
    private String status;
    private final Connection koneksi;

    public Reservasi(Connection koneksi) {
        this.koneksi = koneksi;
    }

    public Reservasi(
            Connection koneksi,
            String idReservasi,
            String idRuangan,
            LocalDate tanggal,
            LocalTime waktu,
            String keperluan) {

        this.koneksi = koneksi;
        this.idReservasi = idReservasi;
        this.idRuangan = idRuangan;
        this.tanggal = tanggal;
        this.waktu = waktu;
        this.keperluan = keperluan;
    }

    public String getIdReservasi() {
        return idReservasi;
    }

    public String getIdRuangan() {
        return idRuangan;
    }

    public LocalDate getTanggal() {
        return tanggal;
    }

    public LocalTime getWaktu() {
        return waktu;
    }

    public String getKeperluan() {
        return keperluan;
    }

    public String getStatus() {
        return status;
    }

    public Reservasi createReservasi(Reservasi dataReservasi) {
        String sql =
                "INSERT INTO Reservasi "
                + "(idReservasi, idRuangan, tanggal, waktu, keperluan) "
                + "VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement statement =
                koneksi.prepareStatement(sql)) {

            statement.setString(
                    1, dataReservasi.idReservasi);
            statement.setString(
                    2, dataReservasi.idRuangan);
            statement.setDate(
                    3, java.sql.Date.valueOf(dataReservasi.tanggal));
            statement.setTime(
                    4, java.sql.Time.valueOf(dataReservasi.waktu));
            statement.setString(
                    5, dataReservasi.keperluan);

            int jumlahTersimpan = statement.executeUpdate();

            if (jumlahTersimpan == 1) {
                return dataReservasi;
            }

        } catch (SQLException e) {
            System.err.println(
                    "Gagal menyimpan reservasi: " + e.getMessage());
        }

        return null;
    }

    public Reservasi getDataReservasi(String idReservasi) {
        String sql =
                "SELECT idReservasi, idRuangan, tanggal, "
                + "waktu, keperluan, status "
                + "FROM Reservasi WHERE idReservasi = ?";

        try (PreparedStatement statement =
                koneksi.prepareStatement(sql)) {

            statement.setString(1, idReservasi);

            try (ResultSet hasil = statement.executeQuery()) {
                if (hasil.next()) {
                    return bacaData(hasil);
                }
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Gagal membaca detail reservasi", e);
        }

        return null;
    }

    public Boolean updateStatus(
            String idReservasi, String keputusan) {

        if (!"Disetujui".equals(keputusan) && !"Ditolak".equals(keputusan)) return false;
        Reservasi data = getDataReservasi(idReservasi);
        if (data == null || !"Menunggu Persetujuan".equals(data.status)) return false;
        if ("Disetujui".equals(keputusan) && new Ruangan(koneksi)
                .getKetersediaan(data.tanggal, data.waktu).stream()
                .noneMatch(r -> r.getIdRuangan().equals(data.idRuangan))) return false;

        String sql =
                "UPDATE Reservasi SET status = ? "
                + "WHERE idReservasi = ? AND status = 'Menunggu Persetujuan'";

        try (PreparedStatement statement =
                koneksi.prepareStatement(sql)) {

            statement.setString(1, keputusan);
            statement.setString(2, idReservasi);

            return statement.executeUpdate() == 1;

        } catch (SQLException e) {
            System.err.println(
                    "Gagal memperbarui status: " + e.getMessage());
            return false;
        }
    }

    public List<Reservasi> getDaftarReservasi() {
        List<Reservasi> daftarReservasi = new ArrayList<>();

        String sql =
                "SELECT idReservasi, idRuangan, tanggal, "
                + "waktu, keperluan, status "
                + "FROM Reservasi "
                + "ORDER BY tanggal, waktu, idReservasi";

        try (PreparedStatement statement =
                koneksi.prepareStatement(sql);
             ResultSet hasil = statement.executeQuery()) {

            while (hasil.next()) {
                daftarReservasi.add(bacaData(hasil));
            }

        } catch (SQLException e) {
            throw new IllegalStateException(
                    "Gagal membaca daftar reservasi", e);
        }

        return daftarReservasi;
    }

    private Reservasi bacaData(ResultSet hasil)
            throws SQLException {

        Reservasi data = new Reservasi(
                koneksi,
                hasil.getString("idReservasi"),
                hasil.getString("idRuangan"),
                hasil.getDate("tanggal").toLocalDate(),
                hasil.getTime("waktu").toLocalTime(),
                hasil.getString("keperluan")
        );

        data.status = hasil.getString("status");

        return data;
    }
}
