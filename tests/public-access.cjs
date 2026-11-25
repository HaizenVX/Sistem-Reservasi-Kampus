const assert=require('node:assert/strict');
module.exports=async({base,check,req})=>{
 const data=(await req('/api/publik/ketersediaan?tanggal=2027-01-11&waktu=10%3A00')).data;
 check(data.ruangan.some(r=>r.status==='Tidak tersedia'),'PUBLIK ruangan yang disetujui tidak tersedia');
 const free=(await req('/api/publik/ketersediaan?tanggal=2027-01-11&waktu=11%3A00')).data;
 check(free.ruangan.every(r=>r.status==='Tersedia'),'PUBLIK filter waktu memakai jadwal database');
 check(data.alat.length>0&&data.barang.length>0,'PUBLIK alat dan kelayakan tersedia tanpa sesi');
 for(const r of data.ruangan)assert.deepEqual(Object.keys(r).sort(),['nama','status']);
 for(const r of data.alat)assert.deepEqual(Object.keys(r).sort(),['nama','status']);
 for(const r of data.barang)assert.deepEqual(Object.keys(r).sort(),['kategori','kondisi','nama','status','terakhir']);
 check(true,'PUBLIK hanya kolom yang diizinkan, tanpa pemilik, catatan, foto, riwayat');
 await req('/api/publik/ketersediaan?tanggal=salah&waktu=10:00',null,'','GET',400);
 await req('/api/publik/ketersediaan',null,'','GET',400);
 for(const path of ['/api/reservasi','/api/pengajuan','/api/insiden','/api/barang','/api/foto/x','/api/dashboard'])await req(path,null,'','GET',401);
 for(const path of ['/api/reservasi','/api/pengajuan','/api/insiden','/api/publik/ketersediaan'])await req(path,{},'','POST',401);
 await req('/api/barang/x',null,'','DELETE',401);
 check(true,'PUBLIK tidak membuka data pribadi atau operasi tulis');
 await req('/KetersediaanUI.html');await req('/ketersediaan.js');await req('/ketersediaan.css');
 for(const path of ['/ReservasiUI.html','/FormPinjamUI.html','/FormLaporanInsidenUI.html','/InspeksiUII.html'])await req(path,null,'','GET',302);
 check(true,'PUBLIK halaman pengunjung terbuka; form tindakan tetap login');
 let pw;try{pw=require('playwright');}catch{pw=require(process.env.PLAYWRIGHT_MODULE||'C:/Program Files/WindowsApps/OpenAI.CodexPrimaryRuntime.v26-1116-1220-0_26.1116.1220.0_x64__3k8sg7r9htsxt/dependencies/node/node_modules/playwright');}
 const fs=require('node:fs'),path=require('node:path'),root=path.join(process.env.LOCALAPPDATA,'ms-playwright');
 const executablePath=fs.readdirSync(root).filter(x=>x.startsWith('chromium-')).map(x=>path.join(root,x,'chrome-win64/chrome.exe')).find(fs.existsSync);
 const browser=await pw.chromium.launch({headless:true,executablePath});
 try{
  const page=await browser.newPage();const errors=[];page.on('pageerror',e=>errors.push(e.message));
  await page.goto(base+'/LoginUI.html');await page.click('a[href="KetersediaanUI.html"]');await page.waitForSelector('#rooms tr');
  await page.fill('#tanggal','2027-01-11');await page.fill('#waktu','10:00');await page.click('#cek');await page.waitForFunction(()=>document.getElementById('jadwalInfo').textContent.includes('2027-01-11'));
  check((await page.locator('#rooms').innerText()).includes('Tidak tersedia'),'WEB pengunjung memfilter jadwal tanpa login');
  await page.screenshot({path:'.runtime/public-desktop.png',fullPage:true});
  await page.setViewportSize({width:390,height:844});
  check(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth),'WEB pengunjung muat pada layar mobile');
  await page.screenshot({path:'.runtime/public-mobile.png',fullPage:true});
  await page.click('a[href="ReservasiUI.html"]');await page.waitForURL('**/LoginUI.html');check(true,'WEB aksi reservasi pengunjung diarahkan ke login');
  assert.deepEqual(errors,[],'WEB pengunjung tanpa error JavaScript');check(true,'WEB pengunjung tanpa error JavaScript');
 }finally{await browser.close();}
};
