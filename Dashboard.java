import java.util.*;
public final class Dashboard {
    static long count(String table,String condition,Object... args){return ((Number)Database.query("SELECT COUNT(*) AS jumlah FROM "+table+" WHERE "+condition,args).get(0).get("jumlah")).longValue();}
    public static Object data(Auth.User u){
        String own=u.admin()?"1=1":"EXISTS (SELECT 1 FROM PemilikData p WHERE p.jenis='%s' AND p.idRecord=%s AND p.username=?)";
        Object[] args=u.admin()?new Object[]{}:new Object[]{u.username()};
        String rp=u.admin()?own:own.formatted("reservasi","idReservasi"),pp=u.admin()?own:own.formatted("pengajuan","idPengajuan"),tp=u.admin()?own:own.formatted("insiden","idTiket");
        return ApiServer.obj("barang",count("Barang","1=1"),"menunggu",count("Reservasi",rp+" AND status='Menunggu Persetujuan'",args)+count("Pengajuan",pp+" AND statusPengajuan='Menunggu Persetujuan'",args),"reservasi",count("Reservasi",rp+" AND status='Disetujui'",args),"insiden",count("TiketInsiden",tp+" AND statusPenanganan<>'Selesai'",args),"pengajuan",count("Pengajuan",pp,args));
    }
}
