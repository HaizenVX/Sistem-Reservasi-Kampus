$ErrorActionPreference='Stop'
Set-Location $PSScriptRoot
& ./setup.ps1
. ./start-database.ps1
$testDb='rsi_auth_'+(Get-Date -Format yyyyMMdd_HHmmss)
$env:MYSQL_PWD=$env:DB_PASSWORD
& 'C:\Program Files\MySQL\MySQL Server 8.0\bin\mysql.exe' --host=127.0.0.1 --port=3307 --user=root -e "CREATE DATABASE $testDb CHARACTER SET utf8mb4;"
if($LASTEXITCODE -ne 0){throw 'Database uji gagal'}
Remove-Item Env:MYSQL_PWD
$env:DB_URL="jdbc:mysql://127.0.0.1:3307/${testDb}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Jakarta"
& java -cp 'build;lib/*' Database --init
$creds=@(Import-Clixml '.runtime/demo-accounts.xml')
$env:DEMO_ADMIN_PASSWORD=($creds | Where-Object UserName -eq admin).GetNetworkCredential().Password
$env:DEMO_MAHASISWA_PASSWORD=($creds | Where-Object UserName -eq mahasiswa).GetNetworkCredential().Password
& java -cp 'build;lib/*' Auth --seed
& javac -encoding UTF-8 -cp 'build;lib/*' -d build tests/AuthTestSeed.java
& java -cp 'build;lib/*' AuthTestSeed
& node tests/auth.cjs
if($LASTEXITCODE -ne 0){throw 'Tes login gagal'}
