$ErrorActionPreference='Stop'
Set-Location $PSScriptRoot
& ./setup.ps1
. ./start-database.ps1
$testDatabase='rsi_test_'+(Get-Date -Format 'yyyyMMdd_HHmmss')
$env:MYSQL_PWD=$env:DB_PASSWORD
& 'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe' --protocol=TCP --host=127.0.0.1 --port=3307 --user=root -e "CREATE DATABASE $testDatabase CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;"
if($LASTEXITCODE -ne 0){throw 'Database uji gagal dibuat'}
Remove-Item Env:MYSQL_PWD
$env:DB_URL="jdbc:mysql://127.0.0.1:3307/${testDatabase}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Jakarta"
$env:PORT='8081'
Set-Content '.runtime/last-test-database.txt' $testDatabase
& java -cp 'build;lib/*' Database --init
if($LASTEXITCODE -ne 0){throw 'Skema uji gagal'}
$creds=@(Import-Clixml '.runtime/demo-accounts.xml')
$env:DEMO_ADMIN_PASSWORD=($creds | Where-Object UserName -eq admin).GetNetworkCredential().Password
$env:DEMO_MAHASISWA_PASSWORD=($creds | Where-Object UserName -eq mahasiswa).GetNetworkCredential().Password
& java -cp 'build;lib/*' Auth --seed
& python tests/integration.py
if($LASTEXITCODE -ne 0){throw 'Pengujian gagal'}
