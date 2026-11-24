import java.time.*;
import java.util.*;
import static java.util.Objects.requireNonNull;

/** Adapter JDBC: controller dan signature model asli tetap digunakan oleh API.
 * Tidak memakai DAFTAR/ArrayList statis pada model simulasi. */
public final class JdbcModels {
    static String s(Map<String,Object> r,String k) { return Database.teks(r,k); }
    static Map<String,Object> one(String sql,Object... args) { var rows=Database.query(sql,args); return rows.isEmpty()?null:rows.get(0); }
    static String status(String kondisi) {
        return "Layak".equals(kondisi)?Barang.STATUS_LAYAK:"Rusak Ringan".equals(kondisi)?Barang.STATUS_PERBAIKAN:Barang.STATUS_TIDAK_LAYAK;
    }
    public static class Alat extends AlatMultimedia {
        @Override public boolean tambahAlat(AlatMultimedia a) {
            return Database.update("INSERT INTO AlatMultimedia(idAlat,namaAlat,status) VALUES (?,?,?)",a.getIdAlat(),a.getNamaAlat(),a.getStatus())==1;
        }
        @Override public String getStatus(String idAlat) {
            var r=one("SELECT a.status,b.statusKelayakan FROM AlatMultimedia a LEFT JOIN Barang b ON b.idBarang=a.idBarang WHERE a.idAlat=?",idAlat);
            if(r==null) return null;
            if(r.get("statusKelayakan")!=null && !Barang.STATUS_LAYAK.equals(s(r,"statusKelayakan"))) return "Tidak Layak Dipinjam";
            return s(r,"status");
        }
        @Override public Boolean updateStatusAlat(String idAlat,String status) {
            if(!TERSEDIA.equals(status)&&!DIPINJAM.equals(status)) return false;
            return Database.update("UPDATE AlatMultimedia SET status=? WHERE idAlat=?",status,idAlat)==1;
        }
        @Override public AlatMultimedia getDetailAlat(String idAlat) {
            var r=one("SELECT * FROM AlatMultimedia WHERE idAlat=?",idAlat);
            return r==null?null:new AlatMultimedia(idAlat,s(r,"namaAlat"),getStatus(idAlat));
        }
        @Override public List<AlatMultimedia> getDaftarAlat() {
            List<AlatMultimedia> out=new ArrayList<>();
            for(var r:Database.query("SELECT idAlat FROM AlatMultimedia ORDER BY idAlat")) out.add(getDetailAlat(s(r,"idAlat")));
            return out;
        }
    }
    public static class Pinjam extends Pengajuan {
        public String terakhirId;
        @Override public String simpanPengajuan(Pengajuan d) {
            if(d==null || d.getIdPengajuan()!=null || new Alat().getDetailAlat(d.getIdAlat())==null) return null;
            String id=Database.id("PJM");
            Database.update("INSERT INTO Pengajuan(idPengajuan,idAlat,idMhs,tglPinjam,waktu,keperluan) VALUES (?,?,?,?,?,?)",
                id,d.getIdAlat(),d.getIdMhs(),new java.sql.Date(d.getTglPinjam().getTime()),d.getWaktu(),d.getKeperluan());
            terakhirId=id; return id;
        }
        Pengajuan map(Map<String,Object> r) {
            return new Pengajuan(s(r,"idPengajuan"),s(r,"idAlat"),s(r,"idMhs"),java.sql.Date.valueOf(s(r,"tglPinjam")),
                LocalTime.parse(s(r,"waktu")),s(r,"keperluan"),s(r,"statusPengajuan"));
        }
        @Override public List<Pengajuan> getAll() { List<Pengajuan> out=new ArrayList<>(); for(var r:Database.query("SELECT * FROM Pengajuan ORDER BY idPengajuan"))out.add(map(r)); return out; }
        @Override public Pengajuan getById(String idPengajuan) { var r=one("SELECT * FROM Pengajuan WHERE idPengajuan=?",idPengajuan);return r==null?null:map(r); }
        @Override public String getIdAlat(String idPengajuan) { var p=getById(idPengajuan);return p==null?null:p.getIdAlat(); }
        @Override public Boolean setStatus(String idPengajuan,String keputusan) {
            if(!DISETUJUI.equals(keputusan)&&!DITOLAK.equals(keputusan))return false;
            return Database.update("UPDATE Pengajuan SET statusPengajuan=? WHERE idPengajuan=? AND statusPengajuan=?",keputusan,idPengajuan,MENUNGGU)==1;
        }
    }
    public static class Inventaris extends Barang {
        @Override public List<Barang> getBarangPerluInspeksi(){return getDaftarDanStatus();}
        @Override public List<Barang> getDaftarDanStatus(){
            List<Barang> out=new ArrayList<>();for(var r:Database.query("SELECT * FROM Barang ORDER BY idBarang"))out.add(map(r));return out;
        }
        Barang map(Map<String,Object> r){
            Barang b=new Barang(s(r,"idBarang"),s(r,"namaBarang"),s(r,"kategori"),s(r,"statusKelayakan"));
            b.setKondisi(s(r,"kondisi")); String date=s(r,"terakhirDiperiksa");
            b.setTerakhirDiperiksa(date==null?null:LocalDate.parse(date)); b.setRiwayatKondisi(getRiwayatKondisi(b.getIdBarang())); return b;
        }
        @Override public Barang getDetailBarang(String idBarang){var r=one("SELECT * FROM Barang WHERE idBarang=?",idBarang);return r==null?null:map(r);}
        @Override public Barang getDetailDanRiwayat(String idBarang){return getDetailBarang(idBarang);}
        @Override public List<LaporanInspeksi> getRiwayatKondisi(String idBarang){return new Inspeksi().riwayat(idBarang);}
        @Override public void updateStatusKelayakan(String idBarang,String status){Database.update("UPDATE Barang SET statusKelayakan=? WHERE idBarang=?",status,idBarang);}
        @Override public String tambahBarang(String namaBarang,String kategori){
            String id=Database.id("BRG");Database.update("INSERT INTO Barang(idBarang,namaBarang,kategori) VALUES (?,?,?)",id,namaBarang,kategori);return id;
        }
        @Override public boolean hapusBarang(String idBarang){
            if(one("SELECT idAlat FROM AlatMultimedia WHERE idBarang=?",idBarang)!=null)throw new IllegalArgumentException("Barang terhubung dengan alat; tidak dapat dihapus");
            Database.update("DELETE FROM LaporanInspeksi WHERE idBarang=?",idBarang);
            return Database.update("DELETE FROM Barang WHERE idBarang=?",idBarang)==1;
        }
    }
    public static class Inspeksi extends LaporanInspeksi {
        public String terakhirId;
        LaporanInspeksi map(Map<String,Object> r){return new LaporanInspeksi(s(r,"idLaporan"),s(r,"idBarang"),s(r,"kondisi"),s(r,"catatanRekomendasi"),LocalDate.parse(s(r,"tanggalInspeksi")));}
        List<LaporanInspeksi> rows(String sql,Object... args){List<LaporanInspeksi> out=new ArrayList<>();for(var r:Database.query(sql,args))out.add(map(r));return out;}
        public List<LaporanInspeksi> riwayat(String idBarang){return rows("SELECT * FROM LaporanInspeksi WHERE idBarang=? ORDER BY tanggalInspeksi,urutan",idBarang);}
        @Override public List<LaporanInspeksi> getDaftarLaporan(){return rows("SELECT * FROM LaporanInspeksi ORDER BY tanggalInspeksi,urutan");}
        @Override public String createLaporan(String idBarang,String kondisi,String catatanRekomendasi){
            if(!kondisiValid(kondisi)||new Inventaris().getDetailBarang(idBarang)==null)return null;
            String id=Database.id("LPN");Database.update("INSERT INTO LaporanInspeksi(idLaporan,idBarang,kondisi,catatanRekomendasi,tanggalInspeksi) VALUES (?,?,?,?,?)",id,idBarang,kondisi,catatanRekomendasi,LocalDate.now());
            hitung(idBarang);terakhirId=id;return id;
        }
        @Override public boolean hapusLaporan(String idLaporan){
            var r=one("SELECT idBarang FROM LaporanInspeksi WHERE idLaporan=?",idLaporan);if(r==null)return false;
            Database.update("DELETE FROM LaporanInspeksi WHERE idLaporan=?",idLaporan);hitung(s(r,"idBarang"));return true;
        }
        public void hitung(String idBarang){
            var r=one("SELECT * FROM LaporanInspeksi WHERE idBarang=? ORDER BY tanggalInspeksi DESC,urutan DESC LIMIT 1",idBarang);
            Database.update("UPDATE Barang SET kondisi=?,terakhirDiperiksa=?,statusKelayakan=? WHERE idBarang=?",
                r==null?"-":s(r,"kondisi"),r==null?null:r.get("tanggalInspeksi"),r==null?Barang.STATUS_BELUM:status(s(r,"kondisi")),idBarang);
        }
    }
    public static class Insiden extends TiketInsiden {
        public String terakhirId;
        public Insiden() { super(); }
        Insiden(Map<String,Object> r){super(s(r,"idTiket"),s(r,"idMhs"),s(r,"detailKerusakan"),s(r,"lokasi"),s(r,"fotoBukti"),s(r,"statusPenanganan"),s(r,"catatan"),LocalDate.parse(s(r,"tanggalLaporan")));}
        @Override public String createTiket(String idMhs,String detailKerusakan,String lokasi,String fotoBukti){
            String id=Database.id("TKT");Database.update("INSERT INTO TiketInsiden(idTiket,idMhs,detailKerusakan,lokasi,fotoBukti,tanggalLaporan) VALUES (?,?,?,?,?,?)",id,idMhs,detailKerusakan,lokasi,fotoBukti,LocalDate.now());terakhirId=id;return id;
        }
        @Override public TiketInsiden getTiketById(String idTiket){var r=one("SELECT * FROM TiketInsiden WHERE idTiket=?",idTiket);return r==null?null:new Insiden(r);}
        @Override public List<TiketInsiden> getDaftarTiket(){List<TiketInsiden> out=new ArrayList<>();for(var r:Database.query("SELECT * FROM TiketInsiden ORDER BY idTiket"))out.add(new Insiden(r));return out;}
        @Override public boolean setStatus(String status){if(!super.setStatus(status))return false;return Database.update("UPDATE TiketInsiden SET statusPenanganan=? WHERE idTiket=?",status,getIdTiket())==1;}
        @Override public void setCatatan(String catatan){super.setCatatan(catatan);Database.update("UPDATE TiketInsiden SET catatan=? WHERE idTiket=?",getCatatan(),getIdTiket());}
    }
}
