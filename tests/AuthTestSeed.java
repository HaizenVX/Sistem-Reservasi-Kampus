public class AuthTestSeed {
 public static void main(String[] args)throws Exception{
  Database.transaksi(true,()->{Auth.seed("other","Mahasiswa Pengujian","mahasiswa","TEST_OTHER",System.getenv("DEMO_MAHASISWA_PASSWORD"));return null;});
 }
}
