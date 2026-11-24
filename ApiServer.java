import com.sun.net.httpserver.*;
import com.google.gson.*;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.Executors;
import javax.imageio.ImageIO;

/** Server lokal: HTML -> API -> controller Modul 3 -> adapter model JDBC. */
public class ApiServer {
    private static final Gson JSON=new GsonBuilder().serializeNulls().create();
    private static final int MAX_FOTO=5*1024*1024;
    private static final Set<String> FILES=Set.of("motion.js","LoginUI.html","DashboardUI.html","PengajuanSayaUI.html","dashboard.js","login.js","dashboard.css","ReservasiUI.html","KelolaReservasiUI.html","FormPinjamUI.html","KelolaPinjamUI.html","FormLaporanInsidenUI.html","InspeksiUII.html","DaftarBarangUI.html","MigrasiDataUI.html","reservasi.css","data-reservasi.js","api.js","pinjam.js","insiden.js","migrasi.js");
    static class ApiError extends RuntimeException {final int code;ApiError(int code,String m){super(m);this.code=code;}}
    static void wajib(boolean b,int code,String m){if(!b)throw new ApiError(code,m);}
    static String str(JsonObject j,String key,int max){
        wajib(j.has(key)&&j.get(key).isJsonPrimitive()&&j.get(key).getAsJsonPrimitive().isString(),400,"Isi "+key);
        String s=j.get(key).getAsString().trim();wajib(!s.isEmpty()&&s.length()<=max,400,key+" wajib diisi, maksimal "+max+" karakter");return s;
    }
    static String optional(JsonObject j,String key,int max){if(!j.has(key)||j.get(key).isJsonNull())return "";String s=j.get(key).getAsString().trim();wajib(s.length()<=max,400,key+" terlalu panjang");return s;}
    static LocalDate date(String s){return LocalDate.parse(s);}
    static LocalTime time(String s){LocalTime t=LocalTime.parse(s);wajib(t.getSecond()==0&&t.getNano()==0,400,"Waktu harus dalam menit");return t;}
    static Map<String,Object> obj(Object... values){Map<String,Object> m=new LinkedHashMap<>();for(int i=0;i<values.length;i+=2)m.put(values[i].toString(),values[i+1]);return m;}
    static String s(Map<String,Object> r,String k){return Database.teks(r,k);}
    public static void main(String[] args)throws Exception{
        Database.transaksi(false,()->{Database.query("SELECT id FROM AppLock");return null;});
        int port=Integer.parseInt(System.getenv().getOrDefault("PORT","8080"));
        HttpServer server=HttpServer.create(new InetSocketAddress(DeploymentConfig.BIND,port),0);
        server.createContext("/",ApiServer::handle);server.setExecutor(Executors.newFixedThreadPool(8));server.start();
        System.out.println("Buka http://localhost:"+port+"/ (login dan database siap)");
    }
    static void handle(HttpExchange ex)throws IOException{
        try{
            String path=ex.getRequestURI().getPath(),method=ex.getRequestMethod();
            String host=ex.getRequestHeaders().getFirst("Host");
            wajib(DeploymentConfig.hostAllowed(host),403,"Host tidak diizinkan");
            if(!path.startsWith("/api/")){
                wajib(method.equals("GET"),405,"Method tidak didukung");
                String file=path.equals("/")?"DashboardUI.html":path.substring(1);
                String pageRole=null;
                if(file.endsWith(".html")&&!file.equals("LoginUI.html")){
                    Auth.User u=Database.transaksi(false,()->Auth.user(ex));
                    if(u==null){ex.getResponseHeaders().set("Location","/LoginUI.html");ex.sendResponseHeaders(302,-1);return;}
                    if(!Auth.page(u,file)){ex.getResponseHeaders().set("Location","/DashboardUI.html");ex.sendResponseHeaders(302,-1);return;}
                    pageRole=u.admin()?"admin":"mahasiswa";
                }
                wajib(FILES.contains(file),404,"Halaman tidak ditemukan");
                String mime=file.endsWith(".html")?"text/html":file.endsWith(".css")?"text/css":"text/javascript";
                byte[] content=Files.readAllBytes(Path.of(file));
                // Peran untuk tampilan awal berasal dari sesi yang sudah diverifikasi,
                // agar sidebar tidak menunggu request /api/me. Otorisasi API tetap berlaku.
                if(pageRole!=null)content=new String(content,StandardCharsets.UTF_8)
                    .replace("<body", "<body data-role=\""+pageRole+"\"").getBytes(StandardCharsets.UTF_8);
                send(ex,200,mime+"; charset=utf-8",content);return;
            }
            boolean write=!method.equals("GET");
            if(write){
                wajib(Set.of("POST","DELETE").contains(method),405,"Method tidak didukung");
                String origin=ex.getRequestHeaders().getFirst("Origin");
                wajib(DeploymentConfig.originAllowed(origin,host),403,"Origin tidak diizinkan");
                wajib("1".equals(ex.getRequestHeaders().getFirst("X-RSI-Request")),403,"Header API diperlukan");
            }
            JsonObject body=new JsonObject();
            if(method.equals("POST")){
                wajib(Optional.ofNullable(ex.getRequestHeaders().getFirst("Content-Type")).orElse("").startsWith("application/json"),415,"Gunakan application/json");
                byte[] bytes=ex.getRequestBody().readNBytes(8*1024*1024+1);
                wajib(bytes.length<=8*1024*1024,413,"Request terlalu besar");
                JsonElement parsed=JsonParser.parseString(new String(bytes,StandardCharsets.UTF_8));
                wajib(parsed.isJsonObject(),400,"Body harus objek JSON");body=parsed.getAsJsonObject();
            }
            final JsonObject data=body;
            Object result=Database.transaksi(write,()->{
                if(path.equals("/api/login")&&method.equals("POST"))return Auth.login(ex,data);
                Auth.User u=Auth.user(ex);
                if(path.equals("/api/me")&&method.equals("GET")){wajib(u!=null,401,"Silakan login");return u;}
                if(path.equals("/api/logout")&&method.equals("POST")){Auth.logout(ex);return obj("message","Logout berhasil");}
                String[] parts=path.substring(5).split("/");
                Auth.authorize(u,method,parts[0],parts.length>1?parts[1]:null);
                return route(method,path,data,u);
            });
            if(path.equals("/api/login")&&result instanceof Map<?,?> m&&m.containsKey("error")){
                send(ex,401,"application/json; charset=utf-8",JSON.toJson(result).getBytes(StandardCharsets.UTF_8));return;
            }
            if(result instanceof Foto f){send(ex,200,f.mime,f.bytes);return;}
            send(ex,200,"application/json; charset=utf-8",JSON.toJson(result).getBytes(StandardCharsets.UTF_8));
        }catch(Exception e){
            int code=e instanceof ApiError a?a.code:e instanceof IllegalArgumentException||e instanceof JsonParseException||e instanceof DateTimeException?400:500;
            if(code==500)e.printStackTrace();
            String message=code==500?"Operasi database/server gagal; periksa terminal server":e.getMessage();
            send(ex,code,"application/json; charset=utf-8",JSON.toJson(obj("error",message)).getBytes(StandardCharsets.UTF_8));
        }finally{ex.close();}
    }
    static void send(HttpExchange ex,int code,String mime,byte[] bytes)throws IOException{
        ex.getResponseHeaders().set("Content-Type",mime);ex.getResponseHeaders().set("Cache-Control","no-store");
        ex.getResponseHeaders().set("X-Content-Type-Options","nosniff");ex.sendResponseHeaders(code,bytes.length);ex.getResponseBody().write(bytes);
    }
    record Foto(String mime,byte[] bytes){}
    static Object route(String method,String path,JsonObject j,Auth.User user)throws Exception{
        String[] p=path.substring(5).split("/");String resource=p[0],id=p.length>1?p[1]:null;
        var alat=new JdbcModels.Alat();var pinjam=new JdbcModels.Pinjam();var barang=new JdbcModels.Inventaris();var inspeksi=new JdbcModels.Inspeksi();var tiket=new JdbcModels.Insiden();
        var pc=new PeminjamanController(alat,pinjam);var ic=new InspeksiController(barang,inspeksi);var tc=new InsidenController(tiket);
        var rc=new ReservasiController(new Ruangan(Database.koneksi()),new Reservasi(Database.koneksi()));
        if(resource.equals("dashboard")&&method.equals("GET"))return Dashboard.data(user);
        if(resource.equals("jadwal")&&method.equals("GET"))return Database.query("SELECT r.namaRuangan AS ruangan,CAST(rs.tanggal AS CHAR) AS tanggal,LEFT(CAST(rs.waktu AS CHAR),5) AS waktu,rs.status FROM Reservasi rs JOIN Ruangan r ON r.idRuangan=rs.idRuangan WHERE rs.status='Disetujui'");
        if(resource.equals("health")&&method.equals("GET"))return obj("status","ok","database",true);
        if(resource.equals("ruangan")&&method.equals("GET"))return Database.query("SELECT idRuangan,namaRuangan FROM Ruangan ORDER BY idRuangan");
        if(resource.equals("alat")){
            if(method.equals("GET")){List<Object> out=new ArrayList<>();for(var a:alat.getDaftarAlat())out.add(obj("idAlat",a.getIdAlat(),"namaAlat",a.getNamaAlat(),"status",a.getStatus()));return out;}
            if(method.equals("POST")){
                String bid=str(j,"idBarang",15);Barang b=ic.getDetailKelayakan(bid);wajib(b!=null,404,"Barang tidak ditemukan");
                wajib(JdbcModels.one("SELECT idAlat FROM AlatMultimedia WHERE idBarang=?",bid)==null,409,"Barang sudah terdaftar sebagai alat");
                String aid="A"+Database.id("ALT").substring(8);
                Database.update("INSERT INTO AlatMultimedia(idAlat,namaAlat,idBarang) VALUES (?,?,?)",aid,b.getNamaBarang(),bid);return obj("idAlat",aid);
            }
        }
        if(resource.equals("reservasi")){
            if(method.equals("GET")){if(id!=null){var r=rc.getDetailReservasi(id);wajib(r!=null,404,"Reservasi tidak ditemukan");return reservasi(r);}List<Object> out=new ArrayList<>();for(var r:rc.getPengajuan())if(Auth.owns(user,"reservasi",r.getIdReservasi()))out.add(reservasi(r));return out;}
            if(method.equals("POST")&&id==null){
                String rid=str(j,"idRuangan",10);LocalDate d=date(str(j,"tanggal",10));LocalTime t=time(str(j,"waktu",8));
                wajib(rc.cekKetersediaan(d,t).stream().anyMatch(r->r.getIdRuangan().equals(rid)),409,"Ruangan tidak tersedia");
                String newId=Database.id("RSV");String msg=rc.simpanReservasi(new Reservasi(Database.koneksi(),newId,rid,d,t,str(j,"keperluan",255)));
                wajib(rc.getDetailReservasi(newId)!=null,409,msg);Auth.claim(user,"reservasi",newId);return obj("id",newId,"message",msg);
            }
            if(method.equals("POST")&&id!=null){String keputusan=decision(j);wajib(rc.prosesReservasi(id,keputusan),409,"Reservasi sudah diproses atau jadwal terisi");return obj("message","Reservasi "+keputusan.toLowerCase());}
        }
        if(resource.equals("pengajuan")){
            if(method.equals("GET")){if(id!=null){var d=pinjam.getById(id);wajib(d!=null,404,"Pengajuan tidak ditemukan");return pengajuan(d);}List<Object> out=new ArrayList<>();for(var d:pc.getDaftar())if(Auth.owns(user,"pengajuan",d.getIdPengajuan()))out.add(pengajuan(d));return out;}
            if(method.equals("POST")&&id==null){
                var d=new Pengajuan(str(j,"idAlat",10),(user.admin()?str(j,"idMhs",15):user.idMhs()),java.sql.Date.valueOf(date(str(j,"tglPinjam",10))),time(str(j,"waktu",8)),str(j,"keperluan",255));
                String msg=pc.prosesPengajuan(d);wajib(pinjam.terakhirId!=null,409,msg);Auth.claim(user,"pengajuan",pinjam.terakhirId);return obj("id",pinjam.terakhirId,"message",msg);
            }
            if(method.equals("POST")&&id!=null){String keputusan=decision(j);var old=pinjam.getById(id);wajib(old!=null&&Pengajuan.MENUNGGU.equals(old.getStatusPengajuan()),409,"Pengajuan sudah diproses atau tidak ditemukan");String msg=pc.updateStatusPengajuan(id,keputusan);wajib(keputusan.equals(pinjam.getById(id).getStatusPengajuan()),409,msg);return obj("message",msg);}
        }
        if(resource.equals("barang")){
            if(method.equals("GET")){if(id!=null){var b=ic.getDetailKelayakan(id);wajib(b!=null,404,"Barang tidak ditemukan");return barang(b);}List<Object> out=new ArrayList<>();for(var b:ic.getDaftarDanStatus())out.add(barang(b));return out;}
            if(method.equals("POST")&&id==null){String bid=barang.tambahBarang(str(j,"nama",100),str(j,"kategori",25));return obj("id",bid);}
            if(method.equals("DELETE")&&id!=null){wajib(barang.hapusBarang(id),404,"Barang tidak ditemukan");return obj("message","Barang dihapus");}
        }
        if(resource.equals("inspeksi")){
            if(method.equals("POST")&&id==null){String msg=ic.validasiDanSimpan(str(j,"idBarang",15),str(j,"kondisi",25),optional(j,"catatan",255));wajib(inspeksi.terakhirId!=null,400,msg);return obj("id",inspeksi.terakhirId,"message",msg);}
            if(method.equals("DELETE")&&id!=null){wajib(inspeksi.hapusLaporan(id),404,"Riwayat tidak ditemukan");return obj("message","Riwayat dihapus dan kelayakan dihitung kembali");}
        }
        if(resource.equals("insiden")){
            if(method.equals("GET")){if(id!=null){var t=tc.getDetailTiket(id);wajib(t!=null,404,"Tiket tidak ditemukan");return insiden(t);}List<Object> out=new ArrayList<>();for(var t:tc.getDaftarTiket())if(Auth.owns(user,"insiden",t.getIdTiket()))out.add(insiden(t));return out;}
            if(method.equals("POST")&&id==null){
                String mhs=(user.admin()?str(j,"idMhs",15):user.idMhs()),detail=str(j,"detailKerusakan",500),lokasi=str(j,"lokasi",200),fasilitas=str(j,"fasilitas",50);
                byte[] foto=Base64.getDecoder().decode(str(j,"foto",7*1024*1024));wajib(foto.length>0&&foto.length<=MAX_FOTO,413,"Foto maksimal 5 MB");
                String mime=validFoto(foto);String fid=Database.id("FTO");Database.update("INSERT INTO FotoInsiden(idFoto,mime,isi) VALUES (?,?,?)",fid,mime,foto);
                String msg=tc.validasiDanSimpanLaporan(mhs,detail,fasilitas+" / "+lokasi,fid);wajib(tiket.terakhirId!=null,400,msg);Auth.claim(user,"insiden",tiket.terakhirId);return obj("idTiket",tiket.terakhirId,"message",msg);
            }
            if(method.equals("POST")&&id!=null){wajib(tc.simpanPembaruanStatus(id,str(j,"statusPenanganan",20),optional(j,"catatan",500)),400,"Tiket/status tidak valid");return obj("message","Status tiket disimpan");}
        }
        if(resource.equals("foto")&&method.equals("GET")&&id!=null){var r=JdbcModels.one("SELECT mime,isi FROM FotoInsiden WHERE idFoto=?",id);wajib(r!=null,404,"Foto tidak ditemukan");return new Foto(s(r,"mime"),(byte[])r.get("isi"));}
        if(resource.equals("impor")&&method.equals("POST"))return LegacyImport.proses(j);
        throw new ApiError(404,"Endpoint tidak ditemukan");
    }
    static String decision(JsonObject j){String d=str(j,"keputusan",25);wajib(Set.of("Disetujui","Ditolak").contains(d),400,"Keputusan tidak valid");return d;}
    static String validFoto(byte[] data)throws IOException{
        try(var in=ImageIO.createImageInputStream(new ByteArrayInputStream(data))){
            var readers=ImageIO.getImageReaders(in);wajib(readers.hasNext(),400,"Isi berkas bukan foto JPEG/PNG");var reader=readers.next();
            try{reader.setInput(in);String format=reader.getFormatName().toLowerCase();wajib(Set.of("jpeg","png").contains(format),400,"Gunakan foto JPEG/PNG");wajib((long)reader.getWidth(0)*reader.getHeight(0)<=25000000,400,"Resolusi foto maksimal 25 megapiksel");wajib(reader.read(0)!=null,400,"Foto tidak dapat dibaca");return format.equals("png")?"image/png":"image/jpeg";}finally{reader.dispose();}
        } catch (javax.imageio.IIOException e) { throw new ApiError(400,"Foto rusak atau tidak dapat dibaca"); }
    }
    static Object reservasi(Reservasi r){var room=JdbcModels.one("SELECT namaRuangan FROM Ruangan WHERE idRuangan=?",r.getIdRuangan());return obj("id",r.getIdReservasi(),"idRuangan",r.getIdRuangan(),"ruangan",s(room,"namaRuangan"),"tanggal",r.getTanggal().toString(),"waktu",r.getWaktu().toString(),"keperluan",r.getKeperluan(),"status",r.getStatus());}
    static Object pengajuan(Pengajuan p){return obj("id",p.getIdPengajuan(),"idAlat",p.getIdAlat(),"mhs",p.getIdMhs(),"alat",new JdbcModels.Alat().getDetailAlat(p.getIdAlat()).getNamaAlat(),"tgl",new java.sql.Date(p.getTglPinjam().getTime()).toString(),"waktu",p.getWaktu().toString(),"keperluan",p.getKeperluan(),"status",p.getStatusPengajuan());}
    static Object barang(Barang b){List<Object> rows=new ArrayList<>();for(var r:b.getRiwayatKondisi())rows.add(obj("id",r.getIdLaporan(),"tanggal",r.getTanggalInspeksi().toString(),"kondisi",r.getKondisi(),"catatan",r.getCatatanRekomendasi()));var a=JdbcModels.one("SELECT idAlat FROM AlatMultimedia WHERE idBarang=?",b.getIdBarang());return obj("id",b.getIdBarang(),"nama",b.getNamaBarang(),"kategori",b.getKategori(),"kondisi",b.getKondisi(),"status",b.getStatusKelayakan(),"terakhir",b.getTerakhirDiperiksa()==null?"-":b.getTerakhirDiperiksa().toString(),"riwayat",rows,"idAlat",a==null?null:s(a,"idAlat"));}
    static Object insiden(TiketInsiden t){return obj("idTiket",t.getIdTiket(),"idMhs",t.getIdMhs(),"detailKerusakan",t.getDetailKerusakan(),"lokasi",t.getLokasi(),"fotoBukti","/api/foto/"+t.getFotoBukti(),"statusPenanganan",t.getStatusPenanganan(),"catatan",t.getCatatan(),"tanggalLaporan",t.getTanggalLaporan().toString());}
}
