document.getElementById('baca').onclick=()=>{
  const data={barang:JSON.parse(localStorage.getItem('siprf_barang')||'[]'),reservasi:JSON.parse(localStorage.getItem('siprf_reservasi')||'[]')};
  document.getElementById('data').value=JSON.stringify(data,null,2);
};
document.getElementById('berkas').onchange=async event=>{const f=event.target.files[0];if(!f)return;if(f.size>5*1024*1024){tampilError(new Error('JSON maksimal 5 MB'));return;}document.getElementById('data').value=await f.text();};
document.getElementById('impor').onclick=async()=>{
  const button=document.getElementById('impor');button.disabled=true;
  try{const data=JSON.parse(document.getElementById('data').value);if(!confirm('Impor data yang ditampilkan ke database? Data asal tidak dihapus.'))return;
    const result=await api('/api/impor',{method:'POST',button,body:JSON.stringify(data)});const box=document.getElementById('pesan');box.className='pesan baik';box.textContent=result.message+'. Ditambahkan: '+result.ditambahkan+', sudah diimpor: '+result.sudahDiimpor;
  }catch(e){tampilError(e);}finally{button.disabled=false;}
};
