async function api(path, options = {}) {
  const {button, ...requestOptions} = options;
  const finishLoading = UiMotion.loading(button);
  try {
    const response = await fetch(path, {...requestOptions, headers:{'Content-Type':'application/json','X-RSI-Request':'1',...requestOptions.headers}});
    const data = await response.json();
    if(response.status===401){location.replace('/LoginUI.html');throw new Error('Silakan login kembali.');}
    if(!response.ok)throw new Error(data.error || 'Permintaan gagal');
    return data;
  } finally {finishLoading();}
}
function tampilError(error) {
  UiMotion.dataFailed();
  const box = document.getElementById('pesan') || document.getElementById('hasil') || document.getElementById('pesanHasil');
  if (box) {box.textContent = error.message || String(error); box.className = 'pesan buruk';}
}
window.addEventListener('unhandledrejection', event => {tampilError(event.reason); event.preventDefault();});
const appReady=api('/api/me');
function renderShell(role){
  if(document.getElementById('sidebar'))return;
  const user={role};
  document.body.classList.add('app-layout');
  const aside=document.createElement('aside');aside.className='sidebar';aside.id='sidebar';
  const brand=document.createElement('a');brand.className='brand';brand.href='DashboardUI.html';brand.innerHTML='<span>SIPRAKTIS<span class="brand-subtitle">Layanan Fasilitas Kampus</span></span>';aside.append(brand);
  const caption=document.createElement('p');caption.className='nav-caption';caption.textContent=user.role==='admin'?'PENGELOLAAN ADMIN':'LAYANAN MAHASISWA';aside.append(caption);
  const nav=document.createElement('nav');nav.setAttribute('aria-label','Menu utama');
  const links=user.role==='admin'?[['Dashboard','DashboardUI.html'],['Kelola Reservasi','KelolaReservasiUI.html'],['Kelola Peminjaman','KelolaPinjamUI.html'],['Data Barang','DaftarBarangUI.html'],['Inspeksi Barang','InspeksiUII.html'],['Kelola Insiden','FormLaporanInsidenUI.html']]:[['Dashboard','DashboardUI.html'],['Reservasi Ruangan','ReservasiUI.html'],['Peminjaman Alat','FormPinjamUI.html'],['Laporan Insiden','FormLaporanInsidenUI.html'],['Pengajuan Saya','PengajuanSayaUI.html']];
  for(const [label,href] of links){const a=document.createElement('a');a.href=href;a.textContent=label;if(location.pathname.endsWith(href)||location.pathname==='/'&&href==='DashboardUI.html'){a.className='active';a.setAttribute('aria-current','page');}nav.append(a);}aside.append(nav);
  const account=document.createElement('div');account.className='sidebar-account';const name=document.createElement('strong');name.textContent=user.role==='admin'?'Admin':'Mahasiswa';const logout=document.createElement('button');logout.id='logout';logout.textContent='Logout';logout.onclick=async()=>{logout.disabled=true;try{await api('/api/logout',{method:'POST',body:'{}',button:logout});location.replace('/LoginUI.html');}finally{logout.disabled=false;}};account.append(name,logout);aside.append(account);document.body.prepend(aside);
  document.querySelector('[data-shell-placeholder]')?.remove();
  const top=document.createElement('header');top.className='app-topbar';const toggle=document.createElement('button');toggle.className='menu-toggle';toggle.textContent='☰';toggle.setAttribute('aria-label','Buka menu');toggle.setAttribute('aria-controls','sidebar');toggle.setAttribute('aria-expanded','false');const title=document.createElement('span');title.textContent='Sistem Peminjaman & Reservasi Fasilitas';const badge=document.createElement('span');badge.className='role-chip';badge.textContent=user.role==='admin'?'Admin':'Mahasiswa';top.append(toggle,title,badge);document.body.insertBefore(top,document.querySelector('main'));
  const backdrop=document.createElement('button');backdrop.type='button';backdrop.className='sidebar-backdrop';backdrop.tabIndex=-1;backdrop.setAttribute('aria-label','Tutup menu');backdrop.setAttribute('aria-hidden','true');document.body.append(backdrop);
  const marker=document.createElement('span');marker.className='nav-marker';marker.setAttribute('aria-hidden','true');nav.prepend(marker);
  function positionMarker(){
    const selected=nav.querySelector('a.active');
    marker.hidden=!selected;
    if(selected){marker.style.top=selected.offsetTop+'px';marker.style.height=selected.offsetHeight+'px';}
  }
  positionMarker();
  new ResizeObserver(positionMarker).observe(nav);
  const mobile=matchMedia('(max-width:720px)'),main=document.querySelector('main');
  function setMenu(open,restoreFocus=true){
    const shown=open&&mobile.matches;
    document.body.classList.toggle('menu-open',shown);
    toggle.setAttribute('aria-expanded',String(shown));toggle.setAttribute('aria-label',shown?'Tutup menu':'Buka menu');
    backdrop.setAttribute('aria-hidden',String(!shown));
    aside.inert=mobile.matches&&!shown;main.inert=shown;
    if(shown)aside.querySelector('nav a')?.focus({preventScroll:true});
    else if(restoreFocus&&mobile.matches)toggle.focus({preventScroll:true});
  }
  toggle.onclick=()=>setMenu(!document.body.classList.contains('menu-open'));
  backdrop.onclick=()=>setMenu(false);
  aside.addEventListener('click',event=>{if(event.target.closest('a'))setMenu(false,false);});
  document.addEventListener('keydown',event=>{
    if(!document.body.classList.contains('menu-open'))return;
    if(event.key==='Escape'){event.preventDefault();setMenu(false);}
    if(event.key==='Tab'){
      const items=[toggle,...aside.querySelectorAll('a[href],button:not(:disabled)')];
      const first=items[0],last=items[items.length-1];
      if(event.shiftKey&&document.activeElement===first){event.preventDefault();last.focus();}
      else if(!event.shiftKey&&document.activeElement===last){event.preventDefault();first.focus();}
    }
  });
  mobile.addEventListener('change',()=>setMenu(false,false));
  window.addEventListener('pageshow',()=>setMenu(false,false));
  setMenu(false,false);
}
// Sidebar dibangun sebelum paint awal dari peran yang dikirim server, tanpa cache akun.
const initialRole=document.body.dataset.role;
if(['admin','mahasiswa'].includes(initialRole))renderShell(initialRole);
appReady.then(user=>{
  renderShell(user.role);
  if(user.role==='admin'&&document.getElementById('lanjut'))document.getElementById('lanjut').hidden=true;
  const input=document.getElementById('idMhs');if(input&&user.role==='mahasiswa'){input.value=user.idMhs;input.defaultValue=user.idMhs;input.readOnly=true;input.closest('label').hidden=true;}
  if(location.pathname.endsWith('FormLaporanInsidenUI.html')&&user.role==='admin'){document.getElementById('formPelaporan').hidden=true;document.querySelector('h1').textContent='Kelola Laporan Insiden';}
});
