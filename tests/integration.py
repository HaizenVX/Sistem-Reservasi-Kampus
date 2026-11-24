"""Pengujian nyata HTTP + restart proses pada database uji terpisah."""
import base64, concurrent.futures, json, os, pathlib, subprocess, time, urllib.request, urllib.error

BASE='http://127.0.0.1:'+os.environ.get('PORT','8081')
checks=0
session_cookie=""
def check(value, message):
    global checks
    assert value, message
    checks+=1
    print('PASS',message,flush=True)
def request(path, data=None, method=None, expected=200):
    req=urllib.request.Request(BASE+path, data=None if data is None else json.dumps(data).encode(),method=method or ('POST' if data is not None else 'GET'),headers={'Content-Type':'application/json','X-RSI-Request':'1','Cookie':session_cookie})
    try:
        with urllib.request.urlopen(req,timeout=30) as response: code=response.status; body=response.read()
    except urllib.error.HTTPError as e: code=e.code;body=e.read()
    assert code==expected, (path,expected,code,body[:500])
    return json.loads(body) if body.startswith((b'{',b'[')) else body
log=open('.runtime/test-server.log','w',encoding='utf-8')
def start():
    global session_cookie
    p=subprocess.Popen(['java','-cp','build;lib/*','ApiServer'],stdout=log,stderr=log)
    for _ in range(60):
        if p.poll() is not None:raise RuntimeError('Server gagal: lihat .runtime/test-server.log')
        try:
            login_req=urllib.request.Request(BASE+'/api/login',data=json.dumps({'username':'admin','password':os.environ['DEMO_ADMIN_PASSWORD']}).encode(),headers={'Content-Type':'application/json','X-RSI-Request':'1'})
            with urllib.request.urlopen(login_req,timeout=5) as login_response: session_cookie=login_response.headers['Set-Cookie'].split(';')[0]
            request('/api/health');return p
        except Exception:time.sleep(.2)
    p.terminate();raise RuntimeError('Server tidak siap')
server=start()
try:
    check(request('/api/health')['database'],'Koneksi database melalui API')
    check(len(request('/api/ruangan'))==3,'Master ruangan tersedia')
    request('/api/barang',{'nama':'   ','kategori':'Elektronik'},expected=400);check(True,'Validasi nama kosong')
    b=request('/api/barang',{'nama':'Kamera uji','kategori':'Elektronik'})['id']
    check(request('/api/barang/'+b)['status']=='Belum Diperiksa','Barang baru belum diperiksa')
    a=request('/api/alat',{'idBarang':b})['idAlat']
    payload={'idAlat':a,'idMhs':'MHS001','tglPinjam':'2026-12-20','waktu':'09:00','keperluan':'Tes peminjaman'}
    request('/api/pengajuan',payload,expected=409);check(True,'Barang belum diperiksa tidak dapat dipinjam')
    request('/api/inspeksi',{'idBarang':b,'kondisi':'Tidak dikenal','catatan':''},expected=400);check(True,'Kondisi inspeksi invalid ditolak')
    l1=request('/api/inspeksi',{'idBarang':b,'kondisi':'Layak','catatan':'Baik'})['id']
    l2=request('/api/inspeksi',{'idBarang':b,'kondisi':'Rusak Ringan','catatan':'Perbaiki'})['id']
    l3=request('/api/inspeksi',{'idBarang':b,'kondisi':'Rusak Berat','catatan':'Rusak'})['id']
    check(request('/api/barang/'+b)['status']=='Tidak Layak','Rusak Berat menjadi Tidak Layak')
    request('/api/inspeksi/'+l2,method='DELETE')
    check(request('/api/barang/'+b)['status']=='Tidak Layak','Hapus riwayat tengah mempertahankan kondisi terakhir')
    request('/api/inspeksi/'+l3,method='DELETE');v=request('/api/barang/'+b)
    check(v['status']=='Layak Digunakan' and v['kondisi']=='Layak' and v['terakhir']==v['riwayat'][-1]['tanggal'],'Hapus terakhir memulihkan kondisi, tanggal dan status')
    request('/api/inspeksi/'+l1,method='DELETE');v=request('/api/barang/'+b)
    check(v['status']=='Belum Diperiksa' and v['kondisi']=='-' and v['terakhir']=='-','Riwayat kosong mengembalikan Belum Diperiksa')
    request('/api/inspeksi',{'idBarang':b,'kondisi':'Layak','catatan':''})
    p1=request('/api/pengajuan',payload)['id'];p2=request('/api/pengajuan',payload)['id']
    def approve(pid):
        try:request('/api/pengajuan/'+pid,{'keputusan':'Disetujui'});return 200
        except AssertionError as e:
            if e.args[0][2]==409:return 409
            raise
    with concurrent.futures.ThreadPoolExecutor(2) as pool:results=list(pool.map(approve,[p1,p2]))
    check(sorted(results)==[200,409],'Dua persetujuan alat bersamaan: hanya satu berhasil')
    winner=p1 if request('/api/pengajuan/'+p1)['status']=='Disetujui' else p2
    loser=p2 if winner==p1 else p1
    request('/api/pengajuan/'+winner,{'keputusan':'Ditolak'},expected=409)
    request('/api/pengajuan/'+winner,{'keputusan':'Disetujui'},expected=409)
    check(True,'Pengajuan final tidak dapat diproses ulang')
    request('/api/pengajuan/'+loser,{'keputusan':'Ditolak'})
    check(request('/api/pengajuan/'+loser)['status']=='Ditolak','Penolakan tersimpan')
    check(next(x for x in request('/api/alat') if x['idAlat']==a)['status']=='Dipinjam','Status alat ikut disimpan')
    rdata={'idRuangan':'R001','tanggal':'2026-12-20','waktu':'09:00','keperluan':'Reservasi tes'}
    r1=request('/api/reservasi',rdata)['id'];r2=request('/api/reservasi',rdata)['id']
    request('/api/reservasi/'+r1,{'keputusan':'Disetujui'})
    request('/api/reservasi/'+r2,{'keputusan':'Disetujui'},expected=409)
    request('/api/reservasi/'+r1,{'keputusan':'Ditolak'},expected=409)
    request('/api/reservasi',rdata,expected=409)
    check(True,'Reservasi bentrok dan keputusan ulang ditolak')
    request('/api/reservasi/'+r2,{'keputusan':'Ditolak'})
    png=base64.b64decode('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aZ1kAAAAASUVORK5CYII=')
    # Generate a valid image using only Python standard library (PNG CRCs).
    import struct,zlib
    def chunk(kind,data):return struct.pack('>I',len(data))+kind+data+struct.pack('>I',zlib.crc32(kind+data)&0xffffffff)
    png=b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack('>IIBBBBB',1,1,8,2,0,0,0))+chunk(b'IDAT',zlib.compress(b'\x00\x00\x80\x80'))+chunk(b'IEND',b'')
    pathlib.Path('.runtime/test-photo.png').write_bytes(png)
    incident={'idMhs':'MHS001','detailKerusakan':'Kursi rusak','lokasi':'Lantai 2','fasilitas':'Auditorium','foto':base64.b64encode(png).decode()}
    t=request('/api/insiden',incident)['idTiket'];ticket=request('/api/insiden/'+t)
    check(ticket['statusPenanganan']=='Dilaporkan' and request(ticket['fotoBukti'])==png,'Tiket dan foto benar-benar tersimpan')
    request('/api/insiden',{**incident,'detailKerusakan':' '},expected=400)
    request('/api/insiden',{**incident,'foto':base64.b64encode(b'not an image').decode()},expected=400)
    request('/api/insiden',{**incident,'foto':base64.b64encode(png+b'\0'*(5*1024*1024-len(png)+1)).decode()},expected=413)
    exact=request('/api/insiden',{**incident,'foto':base64.b64encode(png+b'\0'*(5*1024*1024-len(png))).decode()})
    check(bool(exact['idTiket']),'Foto tepat 5 MiB diterima; lebih satu byte ditolak')
    check(len(request('/api/insiden'))==2,'Laporan invalid tidak meninggalkan tiket')
    request('/api/insiden/'+t,{'statusPenanganan':'Diproses','catatan':'Teknisi datang'})
    check(request('/api/insiden/'+t)['catatan']=='Teknisi datang','Pembaruan status/catatan melalui controller tersimpan')
    request('/api/insiden/'+t,{'statusPenanganan':'INVALID','catatan':'Tidak boleh tersimpan'},expected=400)
    check(request('/api/insiden/'+t)['catatan']=='Teknisi datang','Status invalid tidak mengubah catatan')
    old={'barang':[{'id':'BRG900','nama':'Barang lama','kategori':'Furniture','riwayat':[{'id':'LPN900','tanggal':'2/10/2026','kondisi':'Layak','catatan':'lama'}]}], 'reservasi':[{'id':'RSV900','ruangan':'Ruang B','tanggal':'2026-10-20','waktu':'09:00','keperluan':'Lama','status':'Disetujui'}]}
    check(request('/api/impor',old)['ditambahkan']==2,'Impor mempertahankan ID lama')
    check(request('/api/impor',old)['sudahDiimpor']==2,'Impor ulang tidak menggandakan data')
    changed=json.loads(json.dumps(old));changed['barang'][0]['nama']='Berubah'
    request('/api/impor',changed,expected=409)
    check(request('/api/barang/BRG900')['nama']=='Barang lama','Impor konflik tidak menimpa data')
    rollback={'barang':[{'id':'BRG901','nama':'Rollback','kategori':'Furniture','riwayat':[]},old['barang'][0]|{'nama':'Konflik'}]}
    request('/api/impor',rollback,expected=409);request('/api/barang/BRG901',expected=404)
    check(True,'Impor konflik membatalkan seluruh transaksi')
    request('/api/barang/'+b,method='DELETE',expected=400);check(True,'Barang terkait alat tidak dihapus diam-diam')
    server.terminate();server.wait(timeout=10);server=start()
    check(request('/api/pengajuan/'+winner)['status']=='Disetujui','Pengajuan bertahan setelah restart Java')
    check(request('/api/reservasi/'+r1)['status']=='Disetujui','Reservasi bertahan setelah restart Java')
    check(request('/api/insiden/'+t)['statusPenanganan']=='Diproses' and request(ticket['fotoBukti'])==png,'Tiket dan foto bertahan setelah restart Java')
    check(request('/api/barang/BRG900')['status']=='Layak Digunakan','Barang dan riwayat bertahan setelah restart Java')
    # Browser dan pemisahan peran diuji lewat test-auth.ps1.
    print('TOTAL API CHECKS:',checks,flush=True)
finally:
    server.terminate();server.wait(timeout=10);log.close()
