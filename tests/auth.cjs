const assert=require('node:assert/strict'),{spawn}=require('node:child_process'),fs=require('node:fs'),path=require('node:path');
const base='http://127.0.0.1:8082';let server;let checks=0;
function check(v,m){assert(v,m);console.log('PASS '+m);checks++;}
async function req(url,body,cookie='',method=body?'POST':'GET',status=200){const r=await fetch(base+url,{method,headers:{'Content-Type':'application/json','X-RSI-Request':'1',Cookie:cookie},body:body?JSON.stringify(body):undefined,redirect:'manual'});const data=await r.text();assert.equal(r.status,status,url+' '+data.slice(0,200));return {data:data.startsWith('{')||data.startsWith('[')?JSON.parse(data):data,cookie:r.headers.get('set-cookie')?.split(';')[0],response:r};}
async function login(name){return (await req('/api/login',{username:name,password:name==='admin'?process.env.DEMO_ADMIN_PASSWORD:process.env.DEMO_MAHASISWA_PASSWORD})).cookie;}
async function start(){server=spawn('java',['-cp','build;lib/*','ApiServer'],{env:{...process.env,PORT:'8082'},stdio:['ignore',fs.openSync('.runtime/auth-server.log','a'),fs.openSync('.runtime/auth-server.log','a')]});for(let i=0;i<60;i++){try{await req('/api/me',null,'','GET',401);return;}catch{await new Promise(r=>setTimeout(r,200));}}throw Error('Server tidak siap');}
async function stop(){await new Promise(r=>{server.once('exit',r);server.kill();});}
(async()=>{await start();try{
 await req('/api/dashboard',null,'','GET',401);check(true,'API membutuhkan login');await req('/KelolaPinjamUI.html',null,'','GET',302);check(true,'HTML admin tanpa sesi diarahkan ke login');
 await req('/api/login',{username:'admin',password:'salah'},'','POST',401);const admin=await login('admin'),student=await login('mahasiswa'),other=await login('other');
 check((await req('/api/me',null,student)).data.role==='mahasiswa','Peran ditentukan database');
 check(!(await req('/api/me',null,student)).data.passwordHash,'Hash password tidak dikirim ke browser');
 await req('/api/barang',{nama:'Tidak boleh',kategori:'Elektronik'},student,'POST',403);await req('/api/impor',{},student,'POST',403);await req('/api/inspeksi/x',null,student,'DELETE',403);check(true,'Mahasiswa ditolak pada operasi admin');
 const b=(await req('/api/barang',{nama:'Alat auth test',kategori:'Elektronik'},admin)).data.id;
 const a=(await req('/api/alat',{idBarang:b},admin)).data.idAlat;
 await req('/api/inspeksi',{idBarang:b,kondisi:'Layak',catatan:'baik'},admin);
 const p=(await req('/api/pengajuan',{idAlat:a,idMhs:'PALSU',tglPinjam:'2027-01-10',waktu:'10:00',keperluan:'Uji milik mahasiswa'},student)).data.id;
 check((await req('/api/pengajuan/'+p,null,admin)).data.mhs==='DEMO_MHS01','ID mahasiswa palsu diabaikan; identitas dari sesi');
 await req('/api/pengajuan/'+p,null,other,'GET',404);check((await req('/api/pengajuan',null,other)).data.length===0,'Mahasiswa lain tidak melihat daftar/detail pengajuan');
 await req('/api/pengajuan/'+p,{keputusan:'Disetujui'},student,'POST',403);await req('/api/pengajuan/'+p,{keputusan:'Disetujui'},admin);await req('/api/pengajuan/'+p,{keputusan:'Ditolak'},admin,'POST',409);check(true,'Admin menyetujui sekali; mahasiswa tidak dapat memproses');
 const r=(await req('/api/reservasi',{idRuangan:'R001',tanggal:'2027-01-11',waktu:'10:00',keperluan:'Privat'},student)).data.id;
 await req('/api/reservasi/'+r,null,other,'GET',404);await req('/api/reservasi/'+r,{keputusan:'Disetujui'},admin);
 const jadwal=(await req('/api/jadwal',null,other)).data;check(jadwal.length===1&&!('keperluan'in jadwal[0])&&!('id'in jadwal[0]),'Ketersediaan bersama tanpa membocorkan isi reservasi');
 const png=fs.readFileSync('.runtime/test-photo.png').toString('base64');
 const t=(await req('/api/insiden',{idMhs:'PALSU',lokasi:'Lab',fasilitas:'Gedung F',detailKerusakan:'Laporan privat',foto:png},student)).data.idTiket;
 const ticket=(await req('/api/insiden/'+t,null,student)).data;check(ticket.idMhs==='DEMO_MHS01','Pelapor insiden berasal dari sesi');
 await req('/api/insiden/'+t,null,other,'GET',404);await req(ticket.fotoBukti,null,other,'GET',404);await req('/api/insiden/'+t,{statusPenanganan:'Selesai',catatan:'tipu'},student,'POST',403);check(true,'Tiket, foto, dan perubahan status dilindungi');
 await req('/api/insiden/'+t,{statusPenanganan:'Selesai',catatan:'Diperbaiki'},admin);
 check((await req('/api/dashboard',null,student)).data.reservasi===1&&(await req('/api/dashboard',null,other)).data.reservasi===0,'Ringkasan dashboard dibatasi per pemilik');
 const legacy=(await req('/api/reservasi',{idRuangan:'R002',tanggal:'2027-01-11',waktu:'10:00',keperluan:'Data admin'},admin)).data.id;
 await req('/api/reservasi/'+legacy,null,student,'GET',404);check(true,'Data admin tidak diklaim mahasiswa');
 await stop();await start();check((await req('/api/me',null,student)).data.username==='mahasiswa','Sesi tetap valid setelah restart Java');
 await req('/api/logout',{},student);await req('/api/me',null,student,'GET',401);check(true,'Logout mencabut sesi di backend');
 await require('./public-access.cjs')({base,check,req});
 await require('./browser.cjs')({base,check});
 console.log('TOTAL '+checks+' CHECKS PASSED');
}finally{await stop();}})().catch(e=>{console.error(e);process.exitCode=1;});
