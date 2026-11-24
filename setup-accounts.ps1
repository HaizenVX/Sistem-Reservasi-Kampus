param([switch]$ShowCredentials)
$ErrorActionPreference='Stop'
$accountFile=Join-Path $PSScriptRoot '.runtime/demo-accounts.xml'
if(!(Test-Path $accountFile)){
 New-Item -ItemType Directory -Force (Join-Path $PSScriptRoot '.runtime') | Out-Null
 $accounts=@()
 foreach($username in @('admin','mahasiswa')){
  $bytes=New-Object byte[] 12
  [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($bytes)
  $pass=[Convert]::ToBase64String($bytes)
  $accounts+=New-Object System.Management.Automation.PSCredential($username,(ConvertTo-SecureString $pass -AsPlainText -Force))
 }
 $accounts | Export-Clixml -LiteralPath $accountFile
}
$accounts=@(Import-Clixml -LiteralPath $accountFile)
$env:DEMO_ADMIN_PASSWORD=($accounts | Where-Object UserName -eq 'admin').GetNetworkCredential().Password
$env:DEMO_MAHASISWA_PASSWORD=($accounts | Where-Object UserName -eq 'mahasiswa').GetNetworkCredential().Password
& java -cp 'build;lib/*' Auth --seed
if($LASTEXITCODE -ne 0){throw 'Pembuatan akun gagal'}
if($ShowCredentials){foreach($account in $accounts){Write-Output ($account.UserName+' : '+$account.GetNetworkCredential().Password)}}
Remove-Item Env:DEMO_ADMIN_PASSWORD,Env:DEMO_MAHASISWA_PASSWORD
