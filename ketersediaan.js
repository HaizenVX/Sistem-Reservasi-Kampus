const tanggal=document.getElementById('tanggal'),waktu=document.getElementById('waktu'),pesan=document.getElementById('pesan'),cek=document.getElementById('cek');
const now=new Date();tanggal.value=`${now.getFullYear()}-${String(now.getMonth()+1).padStart(2,'0')}-${String(now.getDate()).padStart(2,'0')}`;
function rows(id,data,fields){
  const target=document.getElementById(id);target.replaceChildren();
  for(const record of data){
    const tr=document.createElement('tr');
    for(const field of fields){const td=document.createElement('td');
      if(field==='status'){const badge=document.createElement('span');const value=String(record[field]??'Belum Diperiksa');badge.className='public-status'+(['Tersedia','Layak','Layak Digunakan'].includes(value)?' good':['Tidak tersedia','Tidak Layak','Tidak Layak Dipinjam','Dipinjam','Rusak'].includes(value)?' bad':'');badge.textContent=value;td.append(badge);}
      else td.textContent=record[field]??'Belum Diperiksa';tr.append(td);
    }target.append(tr);
  }
  if(!data.length){const tr=document.createElement('tr'),td=document.createElement('td');td.colSpan=fields.length;td.textContent='Belum ada data.';tr.append(td);target.append(tr);}
}
async function load(){
  cek.disabled=true;pesan.textContent='Memuat data…';
  for(const id of ['rooms','tools','items'])document.getElementById(id).replaceChildren();
  document.getElementById('jadwalInfo').textContent='Memuat jadwal…';
  const date=tanggal.value,time=waktu.value;
  try{
    const res=await fetch('/api/publik/ketersediaan?'+new URLSearchParams({tanggal:date,waktu:time}));
    const data=await res.json();if(!res.ok)throw new Error(data.error||'Data gagal dimuat');
    rows('rooms',data.ruangan,['nama','status']);rows('tools',data.alat,['nama','status']);rows('items',data.barang,['nama','kategori','kondisi','status','terakhir']);
    document.getElementById('jadwalInfo').textContent=`Status ruangan untuk ${date} pukul ${time}.`;pesan.textContent='Data terbaru berhasil dimuat.';
  }catch(e){pesan.textContent=e.message;document.getElementById('jadwalInfo').textContent='Jadwal belum dapat ditampilkan. Silakan coba kembali.';}
  finally{cek.disabled=false;}
}
document.getElementById('cekForm').addEventListener('submit',e=>{e.preventDefault();load();});
load();
