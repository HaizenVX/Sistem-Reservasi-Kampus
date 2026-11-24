import com.google.gson.*;
import java.time.*;
import java.time.format.*;
import java.util.*;

/** Impor eksplisit: tidak menghapus localStorage, tidak menimpa ID database. */
public final class LegacyImport {
    static String text(JsonObject j,String k,int max){return ApiServer.str(j,k,max);}
    static LocalDate tanggal(String value){
        try{return LocalDate.parse(value);}catch(DateTimeParseException e){return LocalDate.parse(value,DateTimeFormatter.ofPattern("d/M/uuuu").withResolverStyle(ResolverStyle.STRICT));}
    }
    static boolean pernah(String jenis,String id,JsonObject row){
        var old=JdbcModels.one("SELECT isi FROM ImporLama WHERE jenis=? AND idLama=?",jenis,id);
        if(old==null)return false;
        ApiServer.wajib(JsonParser.parseString(Database.teks(old,"isi")).equals(row),409,"Data impor lama berubah: "+id+". Tidak ditimpa.");return true;
    }
    public static Object proses(JsonObject j){
        int added=0,skipped=0;
        for(String jenis:List.of("barang","reservasi")){
            if(!j.has(jenis))continue;
            ApiServer.wajib(j.get(jenis).isJsonArray(),400,"Data "+jenis+" harus array");
            for(JsonElement el:j.getAsJsonArray(jenis)){
                JsonObject r=el.getAsJsonObject();String id=text(r,"id",15);
                if(pernah(jenis,id,r)){skipped++;continue;}
                if(jenis.equals("barang")){
                    ApiServer.wajib(new JdbcModels.Inventaris().getDetailBarang(id)==null,409,"ID barang sudah ada: "+id);
                    Database.update("INSERT INTO Barang(idBarang,namaBarang,kategori) VALUES (?,?,?)",id,text(r,"nama",100),text(r,"kategori",25));
                    ApiServer.wajib(r.has("riwayat") && r.get("riwayat").isJsonArray(),400,"Riwayat barang harus berupa array");
                    for(JsonElement le:r.getAsJsonArray("riwayat")){
                        var l=le.getAsJsonObject();String kondisi=text(l,"kondisi",25);
                        ApiServer.wajib(LaporanInspeksi.kondisiValid(kondisi),400,"Kondisi impor tidak valid");String lid=text(l,"id",15);
                        ApiServer.wajib(JdbcModels.one("SELECT idLaporan FROM LaporanInspeksi WHERE idLaporan=?",lid)==null,409,"ID riwayat sudah ada: "+lid);
                        Database.update("INSERT INTO LaporanInspeksi(idLaporan,idBarang,kondisi,catatanRekomendasi,tanggalInspeksi) VALUES (?,?,?,?,?)",lid,id,kondisi,ApiServer.optional(l,"catatan",255),tanggal(text(l,"tanggal",20)));
                    }
                    new JdbcModels.Inspeksi().hitung(id);
                }else{
                    ApiServer.wajib(JdbcModels.one("SELECT idReservasi FROM Reservasi WHERE idReservasi=?",id)==null,409,"ID reservasi sudah ada: "+id);
                    var room=JdbcModels.one("SELECT idRuangan FROM Ruangan WHERE namaRuangan=?",text(r,"ruangan",100));
                    ApiServer.wajib(room!=null,400,"Ruangan impor belum terdaftar");String rid=Database.teks(room,"idRuangan");
                    String status=text(r,"status",25);ApiServer.wajib(Set.of("Menunggu Persetujuan","Disetujui","Ditolak").contains(status),400,"Status impor tidak valid");
                    LocalDate d=tanggal(text(r,"tanggal",20));LocalTime t=ApiServer.time(text(r,"waktu",8));
                    if(status.equals("Disetujui"))ApiServer.wajib(JdbcModels.one("SELECT idReservasi FROM Reservasi WHERE idRuangan=? AND tanggal=? AND waktu=? AND status='Disetujui'",rid,d,t)==null,409,"Jadwal impor bentrok");
                    Database.update("INSERT INTO Reservasi(idReservasi,idRuangan,tanggal,waktu,keperluan,status) VALUES (?,?,?,?,?,?)",id,rid,d,t,text(r,"keperluan",255),status);
                }
                Database.update("INSERT INTO ImporLama(jenis,idLama,isi) VALUES (?,?,?)",jenis,id,r.toString());added++;
            }
        }
        return ApiServer.obj("message","Impor selesai; data asal tetap tersimpan","ditambahkan",added,"sudahDiimpor",skipped);
    }
}
