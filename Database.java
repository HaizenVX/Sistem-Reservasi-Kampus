import java.sql.*;
import java.nio.file.*;
import java.util.*;

/** Koneksi per transaksi; seluruh penulisan memakai satu kunci database bersama. */
public final class Database {
    private static final ThreadLocal<Connection> AKTIF = new ThreadLocal<>();
    public interface Work<T> { T run() throws Exception; }
    public static Connection buka() throws SQLException {
        String url = System.getenv("DB_URL"), user = System.getenv("DB_USER"), pass = System.getenv("DB_PASSWORD");
        if (url == null || user == null) throw new IllegalStateException("Isi DB_URL dan DB_USER pada lingkungan; lihat config.example.ps1");
        return DriverManager.getConnection(url, user, pass == null ? "" : pass);
    }
    public static Connection koneksi() {
        Connection c = AKTIF.get();
        if (c == null) throw new IllegalStateException("Operasi JDBC harus berada dalam Database.transaksi");
        return c;
    }
    public static <T> T transaksi(boolean tulis, Work<T> work) throws Exception {
        try (Connection c = buka()) {
            c.setAutoCommit(false);
            c.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            AKTIF.set(c);
            try {
                if (tulis) query("SELECT id FROM AppLock WHERE id=1 FOR UPDATE");
                T result = work.run(); c.commit(); return result;
            } catch (Exception e) { c.rollback(); throw e; }
            finally { AKTIF.remove(); }
        }
    }
    public static List<Map<String,Object>> query(String sql, Object... args) {
        try (PreparedStatement p = koneksi().prepareStatement(sql)) {
            bind(p,args);
            try (ResultSet r = p.executeQuery()) {
                List<Map<String,Object>> list = new ArrayList<>();
                while(r.next()) {
                    Map<String,Object> row = new LinkedHashMap<>();
                    for(int i=1;i<=r.getMetaData().getColumnCount();i++) row.put(r.getMetaData().getColumnLabel(i),r.getObject(i));
                    list.add(row);
                }
                return list;
            }
        } catch(SQLException e) { throw new IllegalStateException("Operasi database gagal",e); }
    }
    public static int update(String sql, Object... args) {
        try(PreparedStatement p=koneksi().prepareStatement(sql)) { bind(p,args); return p.executeUpdate(); }
        catch(SQLException e) { throw new IllegalStateException("Operasi database gagal",e); }
    }
    private static void bind(PreparedStatement p,Object[] args) throws SQLException {
        for(int i=0;i<args.length;i++) p.setObject(i+1,args[i]);
    }
    public static String id(String jenis) {
        update("INSERT IGNORE INTO Nomor(jenis,nilai) VALUES (?,0)",jenis);
        update("UPDATE Nomor SET nilai=nilai+1 WHERE jenis=?",jenis);
        long n=((Number)query("SELECT nilai FROM Nomor WHERE jenis=?",jenis).get(0).get("nilai")).longValue();
        String id=jenis+String.format("%012d",n);
        String[] target=switch(jenis) {
            case "PJM" -> new String[]{"Pengajuan","idPengajuan"};
            case "RSV" -> new String[]{"Reservasi","idReservasi"};
            case "BRG" -> new String[]{"Barang","idBarang"};
            case "LPN" -> new String[]{"LaporanInspeksi","idLaporan"};
            case "TKT" -> new String[]{"TiketInsiden","idTiket"};
            case "FTO" -> new String[]{"FotoInsiden","idFoto"};
            default -> null;
        };
        // ID hasil impor boleh mempertahankan nomor aslinya; jangan pernah menimpanya.
        if(target!=null && !query("SELECT "+target[1]+" FROM "+target[0]+" WHERE "+target[1]+"=?",id).isEmpty()) return id(jenis);
        return id;
    }
    public static String teks(Map<String,Object> row,String key) { Object x=row.get(key); return x==null?null:x.toString(); }
    public static void main(String[] args) throws Exception {
        if(args.length!=1 || !args[0].equals("--init")) throw new IllegalArgumentException("Gunakan Database --init pada database proyek baru");
        try(Connection c=buka(); Statement s=c.createStatement()) {
            String sql=Files.readString(Path.of("sql/schema.sql")).replaceAll("(?m)^--.*$","");
            for(String part:sql.split(";")) if(!part.isBlank()) s.execute(part);
        }
        System.out.println("Skema siap. Tidak ada data lama yang dihapus.");
    }
}
