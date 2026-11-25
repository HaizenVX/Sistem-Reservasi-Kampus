import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;

/** Tampilan baca publik UC-01, UC-03, UC-05; tidak mengirim identitas/pengajuan. */
public class PublicAvailability {
    static Object data(URI uri) throws Exception {
        Map<String,String> params=new HashMap<>();
        for(String pair:Optional.ofNullable(uri.getRawQuery()).orElse("").split("&")) {
            String[] part=pair.split("=",2);
            if(part.length==2)params.put(URLDecoder.decode(part[0],StandardCharsets.UTF_8),URLDecoder.decode(part[1],StandardCharsets.UTF_8));
        }
        ApiServer.wajib(params.containsKey("tanggal")&&params.containsKey("waktu"),400,"Pilih tanggal dan waktu");
        LocalDate tanggal=ApiServer.date(params.get("tanggal"));
        LocalTime waktu=ApiServer.time(params.get("waktu"));
        var rc=new ReservasiController(new Ruangan(Database.koneksi()),new Reservasi(Database.koneksi()));
        Set<String> tersedia=new HashSet<>();
        for(var r:rc.cekKetersediaan(tanggal,waktu))tersedia.add(r.getIdRuangan());
        List<Object> rooms=new ArrayList<>(),tools=new ArrayList<>(),items=new ArrayList<>();
        for(var r:Database.query("SELECT idRuangan,namaRuangan FROM Ruangan ORDER BY idRuangan"))
            rooms.add(ApiServer.obj("nama",Database.teks(r,"namaRuangan"),"status",tersedia.contains(Database.teks(r,"idRuangan"))?"Tersedia":"Tidak tersedia"));
        for(var a:new JdbcModels.Alat().getDaftarAlat())
            tools.add(ApiServer.obj("nama",a.getNamaAlat(),"status",a.getStatus()));
        var ic=new InspeksiController(new JdbcModels.Inventaris(),new JdbcModels.Inspeksi());
        for(var b:ic.getDaftarDanStatus())
            items.add(ApiServer.obj("nama",b.getNamaBarang(),"kategori",b.getKategori(),"kondisi",b.getKondisi(),"status",b.getStatusKelayakan(),"terakhir",b.getTerakhirDiperiksa()==null?"Belum Diperiksa":b.getTerakhirDiperiksa().toString()));
        return ApiServer.obj("ruangan",rooms,"alat",tools,"barang",items);
    }
}
