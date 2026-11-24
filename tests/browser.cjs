const fs=require('node:fs'),path=require('node:path');
module.exports=async function({base,check}){
 // Browser: akun mahasiswa dan admin memakai konteks cookie terpisah.
 let pw;try{pw=require('playwright');}catch{pw=require(process.env.PLAYWRIGHT_MODULE||'C:/Program Files/WindowsApps/OpenAI.CodexPrimaryRuntime.v26-1116-1220-0_26.1116.1220.0_x64__3k8sg7r9htsxt/dependencies/node/node_modules/playwright');}
 const root=path.join(process.env.LOCALAPPDATA,'ms-playwright');const executablePath=fs.readdirSync(root).filter(x=>x.startsWith('chromium-')).map(x=>path.join(root,x,'chrome-win64/chrome.exe')).find(fs.existsSync);
 const browser=await pw.chromium.launch({headless:true,executablePath});const errors=[];
 try{
  for(const role of ['mahasiswa','admin']){
   const ctx=await browser.newContext({viewport:{width:1440,height:960}});await ctx.addInitScript(()=>{window.addEventListener('pagereveal',event=>{window.__revealed=true;if(event.viewTransition){window.__viewTransition='running';event.viewTransition.ready.then(()=>{window.__transitionReady=true;}).catch(()=>{});event.viewTransition.finished.then(()=>{window.__viewTransition='finished';});}});});const page=await ctx.newPage();page.on('pageerror',e=>errors.push(e.message));
   await page.goto(base);await page.waitForURL('**/LoginUI.html');if(role==='mahasiswa')await page.screenshot({path:'.runtime/login.png',fullPage:true});
   if(role==='mahasiswa') {
    let release;const gate=new Promise(resolve=>release=resolve);
    await page.route('**/api/login',async route=>{await gate;await route.fulfill({status:401,contentType:'application/json',body:JSON.stringify({error:'Pesan pengujian'})});});
    await page.fill('#username','mahasiswa');await page.fill('#password','uji-loading');await page.click('#masuk');
    await page.waitForSelector('#masuk.is-loading');
    check(await page.locator('#masuk').isDisabled()&&await page.locator('#masuk').getAttribute('aria-busy')==='true','WEB loading login tampil dan mencegah klik berulang');
    check((await page.locator('#masuk').innerText()).includes('Memproses'),'WEB label proses jelas');
    await page.emulateMedia({reducedMotion:'reduce'});await page.waitForFunction(()=>matchMedia('(prefers-reduced-motion: reduce)').matches);
    check(await page.locator('#masuk').evaluate(el=>getComputedStyle(el,'::before').animationName)==='none','WEB reduced motion menghentikan spinner');
    release();await page.waitForFunction(()=>!document.getElementById('masuk').disabled);
    check(await page.locator('#masuk').innerText()==='Login'&&!(await page.locator('#masuk').getAttribute('aria-busy')),'WEB loading pulih setelah error');
    await page.unroute('**/api/login');await page.emulateMedia({reducedMotion:'no-preference'});
    check(await page.evaluate(async()=>{const m=document.getElementById('pesan');m.textContent='Pesan animasi';await new Promise(r=>setTimeout(r,30));return m.getAnimations().length>0;}),'WEB pesan hasil beranimasi');
    await page.emulateMedia({reducedMotion:'reduce'});await page.waitForFunction(()=>matchMedia('(prefers-reduced-motion: reduce)').matches);
    check(await page.evaluate(async()=>{const m=document.getElementById('pesan');m.textContent='Pesan tanpa gerakan';await new Promise(r=>setTimeout(r,30));return m.getAnimations().length===0;}),'WEB pesan mengikuti reduced motion');
    await page.emulateMedia({reducedMotion:'no-preference'});
   }
   await page.fill('#username',role);await page.fill('#password',role==='admin'?process.env.DEMO_ADMIN_PASSWORD:process.env.DEMO_MAHASISWA_PASSWORD);await page.click('#masuk');await page.waitForURL('**/DashboardUI.html');await page.waitForSelector('.sidebar');await page.waitForSelector('.stat-card');
   check(await page.locator('.stat-card').count()===4,'WEB dashboard '+role+' menampilkan statistik');
   check(await page.locator('.sidebar a[href="KelolaPinjamUI.html"]').count()===(role==='admin'?1:0),'WEB sidebar sesuai peran '+role);
   await page.evaluate(()=>Promise.all(document.getAnimations().filter(a=>a.effect?.getComputedTiming().iterations!==Infinity).map(a=>a.finished.catch(()=>{}))));
   await page.screenshot({path:'.runtime/dashboard-'+role+'.png',fullPage:true});
   await page.waitForFunction(()=>!document.querySelector('#statistik').classList.contains('reveal-cards'));
   await page.click('#refresh');await page.waitForFunction(()=>!document.querySelector('#refresh').disabled);
   check(await page.locator('#statistik').evaluate(el=>!el.classList.contains('reveal-cards')),'WEB refresh tidak mengulang animasi kartu '+role);
   await page.emulateMedia({reducedMotion:'reduce'});await page.waitForFunction(()=>matchMedia('(prefers-reduced-motion: reduce)').matches);
   check(await page.locator('.service-card').first().evaluate(el=>getComputedStyle(el).transitionDuration)==='0s','WEB transisi kartu mengikuti reduced motion '+role);
   await page.emulateMedia({reducedMotion:'no-preference'});

   if(role==='mahasiswa'){
    let releaseSession;const sessionGate=new Promise(resolve=>releaseSession=resolve);
    await page.route('**/api/me',async route=>{await sessionGate;await route.continue();});
    await page.goto(base+'/DashboardUI.html',{waitUntil:'domcontentloaded'});
    check(await page.locator('.sidebar nav a').count()===5,'WEB sidebar tersedia sebelum respons sesi');
    check(await page.locator('.skeleton-card').count()===4&&await page.locator('#statistik').getAttribute('aria-busy')==='true','WEB placeholder tampil saat data tertunda');
    const skeletonHeight=await page.locator('#statistik').evaluate(el=>el.getBoundingClientRect().height);
    releaseSession();await page.waitForSelector('.stat-card');
    check(Math.abs(await page.locator('#statistik').evaluate(el=>el.getBoundingClientRect().height)-skeletonHeight)<2,'WEB tinggi placeholder sesuai kartu akhir');
    check(await page.locator('.stat-card,.service-card').evaluateAll(els=>els.every(el=>getComputedStyle(el).animationName==='none')),'WEB kartu tidak mengulang animasi setelah data masuk');await page.unroute('**/api/me');
    check(await page.locator('[data-loading]').count()===0,'WEB placeholder dan status busy dibersihkan');
    await page.route('**/api/dashboard',route=>route.fulfill({status:500,contentType:'application/json',body:JSON.stringify({error:'Uji kegagalan data'})}));
    await page.reload();await page.waitForSelector('.loading-error');
    check(await page.locator('[data-loading]').count()===0,'WEB kegagalan data tidak menyisakan placeholder');
    await page.unroute('**/api/dashboard');await page.click('#refresh');await page.waitForSelector('.stat-card');
    check(await page.locator('.loading-error').count()===0,'WEB data dapat dimuat ulang setelah gagal');
    await page.locator('.sidebar a[href="PengajuanSayaUI.html"]').click();
    await page.waitForURL('**/PengajuanSayaUI.html');
    await page.waitForFunction(()=>window.__viewTransition==='finished',{},{timeout:5000});
    check(await page.evaluate(()=>window.__transitionReady===true),'WEB transisi lintas halaman benar-benar berjalan');
    check(await page.locator('.sidebar').evaluate(el=>getComputedStyle(el).viewTransitionName==='app-sidebar'),'WEB sidebar dipisahkan dari fade konten');
    check(await page.locator('.nav-marker').evaluate(el=>{const a=document.querySelector('nav a.active');return Math.abs(el.offsetTop-a.offsetTop)<1&&el.offsetHeight===a.offsetHeight;}),'WEB indikator berada pada menu tujuan');
    await page.goBack();await page.waitForSelector('.stat-card');
    check(await page.locator('body').evaluate(el=>!el.classList.contains('is-navigating')),'WEB tombol Back memulihkan tampilan');
    await page.emulateMedia({reducedMotion:'reduce'});
    await page.locator('.sidebar nav a[href="PengajuanSayaUI.html"]').click();await page.waitForURL('**/PengajuanSayaUI.html');await page.waitForSelector('#aktivitas tr');
    check(await page.locator('main').evaluate(el=>getComputedStyle(el).animationName==='none')&&await page.evaluate(()=>window.__transitionReady!==true),'WEB navigasi reduced motion tanpa animasi');
    await page.emulateMedia({reducedMotion:'no-preference'});
    const popupPromise=ctx.waitForEvent('page');await page.locator('.sidebar nav a[href="DashboardUI.html"]').click({modifiers:['Control']});const popup=await popupPromise;await popup.waitForLoadState();
    check(popup.url().endsWith('/DashboardUI.html')&&page.url().endsWith('/PengajuanSayaUI.html'),'WEB Ctrl+klik tetap membuka tab baru');await popup.close();

    await page.goto(base+'/KelolaPinjamUI.html');await page.waitForURL('**/DashboardUI.html');check(true,'WEB URL admin ditolak untuk mahasiswa');
    await page.locator('.sidebar a[href="ReservasiUI.html"]').click();await page.waitForSelector('#ruangan option',{state:'attached'});await page.fill('#tanggal','2027-02-11');await page.fill('#waktu','11:00');await page.locator('#filter button').click();await page.waitForFunction(()=>document.querySelector('#ruangan').options.length>0);await page.fill('#keperluan','Reservasi browser dengan login');await page.click('#ajukan');await page.waitForFunction(()=>document.querySelector('#pesan').textContent.includes('berhasil'));
    await page.locator('.sidebar a[href="PengajuanSayaUI.html"]').click();await page.waitForURL('**/PengajuanSayaUI.html');await page.waitForFunction(()=>document.querySelector('#aktivitas')?.rows.length>=3);check(true,'WEB mahasiswa mengajukan dan melihat riwayat sendiri');check(await page.locator('#refresh').count()===0,'WEB Pengajuan Saya tanpa tombol refresh tetap memuat data');
   }
   await page.goto(base+'/FormLaporanInsidenUI.html');await page.waitForLoadState('networkidle');check(await page.locator('#formPelaporan').isVisible()===(role==='mahasiswa'),'WEB form insiden sesuai peran');check(await page.locator('#daftarTiket button').count()===(role==='admin'?1:0),'WEB mahasiswa tidak diberi tombol penanganan');
   await page.goto(base+'/DashboardUI.html');await page.waitForSelector('.sidebar');await page.setViewportSize({width:390,height:844});await page.click('.menu-toggle');check(await page.locator('.menu-toggle').getAttribute('aria-expanded')==='true','WEB menu mobile '+role);await page.click('.menu-toggle');await page.waitForFunction(()=>getComputedStyle(document.querySelector('.sidebar')).visibility==='hidden');await page.evaluate(()=>Promise.all(document.getAnimations().filter(a=>a.effect?.getComputedTiming().iterations!==Infinity).map(a=>a.finished.catch(()=>{}))));await page.screenshot({path:'.runtime/mobile-'+role+'.png',fullPage:true});check(await page.evaluate(()=>document.documentElement.scrollWidth<=innerWidth+1),'WEB tidak overflow '+role);
   await page.click('.menu-toggle');
   check(await page.locator('main').evaluate(el=>el.inert),'WEB konten belakang menu tidak interaktif '+role);
   await page.keyboard.press('Escape');
   check(await page.locator('.menu-toggle').getAttribute('aria-expanded')==='false'&&await page.locator('.menu-toggle').evaluate(el=>el===document.activeElement),'WEB Escape menutup menu dan memulihkan fokus '+role);
   await page.click('.menu-toggle');await page.locator('.sidebar-backdrop').click({position:{x:360,y:400}});
   check(await page.locator('.menu-toggle').getAttribute('aria-expanded')==='false'&&await page.locator('main').evaluate(el=>!el.inert),'WEB klik backdrop menutup menu '+role);
   await page.setViewportSize({width:1440,height:960});await page.click('#logout');await page.waitForURL('**/LoginUI.html');check(true,'WEB logout '+role);await ctx.close();
  }
  check(errors.length===0,'Tidak ada JavaScript error di browser');
 }finally{await browser.close();}

};

