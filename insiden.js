const form = document.getElementById('formPelaporan');
form.onsubmit = async event => {
  event.preventDefault();const button=document.getElementById('btnKirim');button.disabled=true;
  try{
    const foto=document.getElementById('fotoBukti').files[0];
    if(!foto||foto.size>5*1024*1024)throw new Error('Foto wajib diisi, maksimal 5 MB');
    if(!['image/jpeg','image/png'].includes(foto.type))throw new Error('Gunakan foto JPEG atau PNG');
    const encoded=await new Promise((resolve,reject)=>{const r=new FileReader();r.onload=()=>resolve(r.result.split(',')[1]);r.onerror=()=>reject(new Error('Foto gagal dibaca'));r.readAsDataURL(foto);});
    const data={foto:encoded,fasilitas:document.getElementById('idFasilitas').selectedOptions[0].textContent};
    for(const k of ['idMhs','lokasi','detailKerusakan'])data[k]=document.getElementById(k).value.trim();
    const hasil=await api('/api/insiden',{method:'POST',button,body:JSON.stringify(data)});
    const el=document.getElementById('pesan');el.className='pesan baik';el.textContent=hasil.message;form.reset();await muatTiket();
  }catch(e){tampilError(e);}finally{button.disabled=false;}
};
async function muatTiket(){
  const user=await appReady; const admin=user.role==='admin';
  document.querySelector('#daftarTiket').closest('table').querySelector('thead th:last-child').hidden=!admin;
  const tickets=await api('/api/insiden');const body=document.getElementById('daftarTiket');body.replaceChildren();
  for(const t of tickets){const row=body.insertRow();for(const v of [t.idTiket,t.lokasi,t.detailKerusakan,t.statusPenanganan,t.catatan])row.insertCell().textContent=v;
    const link=document.createElement('a');link.href=t.fotoBukti;link.target='_blank';link.rel='noopener';link.textContent='Lihat foto';row.insertCell().append(link);
    if(!admin)continue;
    const cell=row.insertCell(), select=document.createElement('select');for(const status of ['Dilaporkan','Diproses','Selesai'])select.add(new Option(status,status));select.value=t.statusPenanganan;
    const catatan=document.createElement('input');catatan.placeholder='Catatan penanganan';catatan.maxLength=500;catatan.value=t.catatan;
    const btn=document.createElement('button');btn.textContent='Simpan status';btn.onclick=async()=>{btn.disabled=true;try{await api('/api/insiden/'+t.idTiket,{method:'POST',button:btn,body:JSON.stringify({statusPenanganan:select.value,catatan:catatan.value})});await muatTiket();}catch(e){tampilError(e);btn.disabled=false;}};
    cell.append(select,catatan,btn);
  }
}
muatTiket();
