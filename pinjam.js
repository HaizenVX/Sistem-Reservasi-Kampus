let currentId = null;
async function muatAlat() {
  const data = await api('/api/alat');
  const tbody = document.getElementById('daftarAlat'), select = document.getElementById('idAlat');
  tbody.replaceChildren(); select.replaceChildren(new Option('-- Pilih --',''));
  for (const a of data) {
    const row=tbody.insertRow();for(const v of [a.idAlat,a.namaAlat])row.insertCell().textContent=v;
    const badge=document.createElement('span');badge.className='label '+(a.status==='Tersedia'?'baik':'buruk');badge.textContent=a.status;row.insertCell().append(badge);
    if(a.status==='Tersedia')select.add(new Option(a.namaAlat,a.idAlat));
  }
  if(!data.length){const c=tbody.insertRow().insertCell();c.colSpan=3;c.textContent='Daftarkan barang sebagai alat dari halaman Barang, lalu lakukan inspeksi.';}
  const id=new URLSearchParams(location.search).get('alat');if(id)select.value=id;
}
async function muatPengajuan() {
  const data=await api('/api/pengajuan');const body=document.getElementById('daftarPengajuan');body.replaceChildren();
  for(const p of data){const row=body.insertRow();for(const v of [p.id,p.mhs,p.tgl])row.insertCell().textContent=v;
    const badge=document.createElement('span');badge.className='label '+(p.status==='Disetujui'?'baik':p.status==='Ditolak'?'buruk':'tunggu');badge.textContent=p.status;row.insertCell().append(badge);
    const b=document.createElement('button');b.textContent='Detail';b.onclick=()=>tampilkanDetail(p.id);row.insertCell().append(b);
  }
  if(currentId)await tampilkanDetail(currentId);
}
async function tampilkanDetail(id) {
  currentId=id;const data=await api('/api/pengajuan/'+encodeURIComponent(id));document.getElementById('panelDetail').style.display='block';
  for(const [name,value] of Object.entries({detailId:id,detMhs:data.mhs,detAlat:data.alat,detTgl:data.tgl,detWaktu:data.waktu,detKeperluan:data.keperluan,detStatus:data.status}))document.getElementById(name).textContent=value;
  document.getElementById('btnSetujui').disabled=document.getElementById('btnTolak').disabled=data.status!=='Menunggu Persetujuan';
}
async function prosesVerifikasi(keputusan) {
  document.getElementById('btnSetujui').disabled=document.getElementById('btnTolak').disabled=true;
  try{const result=await api('/api/pengajuan/'+encodeURIComponent(currentId),{method:'POST',button:document.getElementById(keputusan==='Disetujui'?'btnSetujui':'btnTolak'),body:JSON.stringify({keputusan})});
    const el=document.getElementById('pesanHasil');el.textContent=result.message;el.className='pesan '+(keputusan==='Disetujui'?'baik':'buruk');
  }catch(e){tampilError(e);}finally{await muatPengajuan();}
}
if(document.getElementById('formPinjam')){
  document.getElementById('formPinjam').onsubmit=async event=>{
    event.preventDefault();const button=document.getElementById('btnAjukan');button.disabled=true;
    try{const data={};for(const k of ['idMhs','idAlat','tglPinjam','waktu','keperluan'])data[k]=document.getElementById(k).value.trim();
      const result=await api('/api/pengajuan',{method:'POST',button,body:JSON.stringify(data)});const el=document.getElementById('pesan');el.textContent=result.message+' — ID: '+result.id;el.className='pesan baik';
      event.target.reset();await muatAlat();
    }catch(e){tampilError(e);}finally{button.disabled=false;}
  };muatAlat();window.addEventListener('focus',muatAlat);
}else{muatPengajuan();window.addEventListener('focus',muatPengajuan);}
