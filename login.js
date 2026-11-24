fetch('/api/me').then(r=>{if(r.ok)location.replace('/DashboardUI.html');}).catch(()=>{});
document.getElementById('loginForm').onsubmit=async event=>{
 event.preventDefault();const button=document.getElementById('masuk'),pesan=document.getElementById('pesan');button.disabled=true;const finishLoading=UiMotion.loading(button);pesan.textContent='';
 try{const response=await fetch('/api/login',{method:'POST',headers:{'Content-Type':'application/json','X-RSI-Request':'1'},body:JSON.stringify({username:document.getElementById('username').value,password:document.getElementById('password').value})});const result=await response.json();if(!response.ok)throw new Error(result.error||'Login gagal');location.replace('/DashboardUI.html');}
 catch(e){pesan.className='pesan buruk';pesan.textContent=e.message;}finally{finishLoading();button.disabled=false;}
};
