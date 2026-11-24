import com.sun.net.httpserver.HttpExchange;
import com.google.gson.JsonObject;
import java.security.*;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.util.*;
import java.sql.Timestamp;

/** Login lokal, password PBKDF2 dan sesi opaque yang disimpan sebagai hash. */
public final class Auth {
    public record User(String username,String nama,String role,String idMhs) {
        public boolean admin(){return role.equals("admin");}
    }
    static final SecureRandom RANDOM=new SecureRandom();
    static String random(){byte[] b=new byte[32];RANDOM.nextBytes(b);return Base64.getUrlEncoder().withoutPadding().encodeToString(b);}
    static String hash(String s)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(java.nio.charset.StandardCharsets.UTF_8)));}
    static byte[] password(String password,String salt)throws Exception{
        PBEKeySpec spec=new PBEKeySpec(password.toCharArray(),Base64.getDecoder().decode(salt),210000,256);
        try{return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();}finally{spec.clearPassword();}
    }
    static String token(HttpExchange ex){
        String cookie=ex.getRequestHeaders().getFirst("Cookie");if(cookie==null)return "";
        for(String part:cookie.split(";"))if(part.trim().startsWith("rsi_session="))return part.trim().substring(12);
        return "";
    }
    public static User user(HttpExchange ex)throws Exception{
        String token=token(ex);if(token.isEmpty()||token.length()>100)return null;
        var r=JdbcModels.one("SELECT a.username,a.nama,a.role,a.idMhs FROM SesiLogin s JOIN Akun a ON a.username=s.username WHERE s.tokenHash=? AND s.kedaluwarsa>CURRENT_TIMESTAMP",hash(token));
        return r==null?null:new User(Database.teks(r,"username"),Database.teks(r,"nama"),Database.teks(r,"role"),Database.teks(r,"idMhs"));
    }
    public static Object login(HttpExchange ex,JsonObject j)throws Exception{
        String username=ApiServer.str(j,"username",50),pass=ApiServer.str(j,"password",200);
        var r=JdbcModels.one("SELECT * FROM Akun WHERE username=?",username);
        boolean locked=r!=null&&r.get("terkunciSampai")!=null&&((Timestamp)r.get("terkunciSampai")).getTime()>System.currentTimeMillis();
        String salt=r==null?Base64.getEncoder().encodeToString(new byte[16]):Database.teks(r,"salt");
        boolean valid=MessageDigest.isEqual(password(pass,salt),r==null?new byte[32]:Base64.getDecoder().decode(Database.teks(r,"passwordHash")));
        if(r==null||!valid||locked){
            if(r!=null&&!locked)Database.update("UPDATE Akun SET gagalLogin=gagalLogin+1,terkunciSampai=CASE WHEN gagalLogin>=5 THEN DATE_ADD(CURRENT_TIMESTAMP,INTERVAL 1 MINUTE) ELSE NULL END WHERE username=?",username);
            return ApiServer.obj("error","Username/password salah atau akun terkunci sementara. Coba lagi setelah satu menit.");
        }
        Database.update("UPDATE Akun SET gagalLogin=0,terkunciSampai=NULL WHERE username=?",username);
        Database.update("DELETE FROM SesiLogin WHERE kedaluwarsa<CURRENT_TIMESTAMP");
        if(!token(ex).isEmpty())Database.update("DELETE FROM SesiLogin WHERE tokenHash=?",hash(token(ex)));
        String session=random();Database.update("INSERT INTO SesiLogin(tokenHash,username,kedaluwarsa) VALUES (?,?,DATE_ADD(CURRENT_TIMESTAMP,INTERVAL 8 HOUR))",hash(session),username);
        ex.getResponseHeaders().add("Set-Cookie","rsi_session="+session+"; Path=/; HttpOnly; SameSite=Strict; Max-Age=28800"+DeploymentConfig.cookieSecurity());
        return new User(username,Database.teks(r,"nama"),Database.teks(r,"role"),Database.teks(r,"idMhs"));
    }
    public static void logout(HttpExchange ex)throws Exception{
        Database.update("DELETE FROM SesiLogin WHERE tokenHash=?",hash(token(ex)));
        ex.getResponseHeaders().add("Set-Cookie","rsi_session=; Path=/; HttpOnly; SameSite=Strict; Max-Age=0"+DeploymentConfig.cookieSecurity());
    }
    public static boolean owns(User u,String jenis,String id){return u.admin()||JdbcModels.one("SELECT idRecord FROM PemilikData WHERE jenis=? AND idRecord=? AND username=?",jenis,id,u.username())!=null;}
    public static void claim(User u,String jenis,String id){Database.update("INSERT INTO PemilikData(jenis,idRecord,username) VALUES (?,?,?)",jenis,id,u.username());}
    public static boolean page(User u,String file){
        if(file.equals("DashboardUI.html"))return true;
        return u.admin()?Set.of("KelolaReservasiUI.html","KelolaPinjamUI.html","DaftarBarangUI.html","InspeksiUII.html","FormLaporanInsidenUI.html","MigrasiDataUI.html").contains(file):Set.of("ReservasiUI.html","FormPinjamUI.html","FormLaporanInsidenUI.html","PengajuanSayaUI.html").contains(file);
    }
    public static void authorize(User u,String method,String resource,String id){
        ApiServer.wajib(u!=null,401,"Silakan login terlebih dahulu");
        if(u.admin())return;
        boolean allowed=method.equals("GET")&&Set.of("dashboard","jadwal","ruangan","alat","reservasi","pengajuan","insiden","foto").contains(resource)
            ||method.equals("POST")&&id==null&&Set.of("reservasi","pengajuan","insiden").contains(resource);
        ApiServer.wajib(allowed,403,"Akses hanya untuk admin");
        if(method.equals("GET")&&id!=null){
            if(Set.of("reservasi","pengajuan","insiden").contains(resource))ApiServer.wajib(owns(u,resource,id),404,"Data tidak ditemukan");
            if(resource.equals("foto"))ApiServer.wajib(JdbcModels.one("SELECT t.idTiket FROM TiketInsiden t JOIN PemilikData p ON p.jenis='insiden' AND p.idRecord=t.idTiket WHERE t.fotoBukti=? AND p.username=?",id,u.username())!=null,404,"Foto tidak ditemukan");
        }
    }
    public static void main(String[] args)throws Exception{
        Database.transaksi(true,()->{
            seed("admin","Admin Kelompok 5","admin",null,System.getenv("DEMO_ADMIN_PASSWORD"));
            seed("mahasiswa","Mahasiswa Demo","mahasiswa","DEMO_MHS01",System.getenv("DEMO_MAHASISWA_PASSWORD"));return null;
        });
        System.out.println("Akun admin dan mahasiswa siap; akun yang sudah ada tidak ditimpa.");
    }
    static void seed(String username,String nama,String role,String idMhs,String pass)throws Exception{
        if(JdbcModels.one("SELECT username FROM Akun WHERE username=?",username)!=null)return;
        if(pass==null||pass.length()<10)throw new IllegalArgumentException("Password akun demo minimal 10 karakter dari lingkungan");
        byte[] bytes=new byte[16];RANDOM.nextBytes(bytes);String salt=Base64.getEncoder().encodeToString(bytes);
        Database.update("INSERT INTO Akun(username,nama,role,idMhs,salt,passwordHash) VALUES (?,?,?,?,?,?)",username,nama,role,idMhs,salt,Base64.getEncoder().encodeToString(password(pass,salt)));
    }
}
