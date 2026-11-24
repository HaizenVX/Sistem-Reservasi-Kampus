param([int]$DatabasePort = 3307)
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
$mysqlBase = if ($env:MYSQL_BASE) { $env:MYSQL_BASE } else { 'C:\Program Files\MySQL\MySQL Server 8.0' }
$mysqlServer = Join-Path $mysqlBase 'bin\mysqld.exe'
$mysqlClient = Join-Path $mysqlBase 'bin\mysql.exe'
if (!(Test-Path $mysqlServer)) { throw 'MySQL Server tidak ditemukan; isi MYSQL_BASE atau gunakan config.example.ps1.' }
$runtimePath = Join-Path $PSScriptRoot '.runtime'
$dataPath = Join-Path $runtimePath 'mysql'
New-Item -ItemType Directory -Force $runtimePath,$dataPath | Out-Null
$secretPath = Join-Path $runtimePath 'database-credential.xml'
if (!(Test-Path (Join-Path $dataPath 'mysql'))) {
 & $mysqlServer --no-defaults --initialize-insecure "--basedir=$mysqlBase" "--datadir=$dataPath" --console
 if ($LASTEXITCODE -ne 0) { throw 'Inisialisasi database gagal' }
}
# Kredensial dilindungi DPAPI Windows, hanya dapat dibaca oleh akun Windows ini.
if (!(Test-Path $secretPath)) {
 $passwordBytes = New-Object byte[] 32
 [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($passwordBytes)
 $newPassword = [Convert]::ToBase64String($passwordBytes)
 $secret = New-Object System.Management.Automation.PSCredential('root', (ConvertTo-SecureString $newPassword -AsPlainText -Force))
 $secret | Export-Clixml -LiteralPath $secretPath
}
$secret = Import-Clixml -LiteralPath $secretPath
$env:DB_USER = $secret.UserName
$env:DB_PASSWORD = $secret.GetNetworkCredential().Password
$env:DB_URL = "jdbc:mysql://127.0.0.1:$DatabasePort/rsi_kelompok5?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Jakarta"
$portFile = Join-Path $runtimePath 'database-port.txt'
# Jangan menjalankan instance kedua pada direktori data yang sama.
$pidFile = Join-Path $runtimePath 'mysql.pid'
$dbProcess = $null
if (Test-Path $pidFile) { $dbProcess = Get-Process -Id ([int](Get-Content $pidFile)) -ErrorAction SilentlyContinue }
if (!$dbProcess) {
 $listener = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback,$DatabasePort)
 try { $listener.Start() } catch { throw "Port $DatabasePort digunakan aplikasi lain. Pilih -DatabasePort lain." } finally { $listener.Stop() }
 $arguments = @('--no-defaults', ('--basedir="' + $mysqlBase + '"'), ('--datadir="' + $dataPath + '"'), "--port=$DatabasePort", '--bind-address=127.0.0.1', '--mysqlx=0', ('--pid-file="' + $pidFile + '"'), ('--log-error="' + (Join-Path $runtimePath 'mysql.log') + '"'))
 Start-Process -FilePath $mysqlServer -ArgumentList $arguments -WindowStyle Hidden
 Set-Content -LiteralPath $portFile -Value $DatabasePort
} elseif ((Get-Content $portFile) -ne "$DatabasePort") { throw 'Database proyek sudah berjalan di port berbeda; gunakan port di .runtime/database-port.txt.' }
$ready = $false
for ($i=0; $i -lt 30; $i++) {
 $socket = [System.Net.Sockets.TcpClient]::new()
 try { $socket.Connect('127.0.0.1',$DatabasePort); $ready=$true; break } catch { Start-Sleep -Milliseconds 500 } finally { $socket.Dispose() }
}
if (!$ready) { throw 'Database belum siap; lihat .runtime/mysql.log' }
# MYSQL_PWD hanya untuk proses konfigurasi; tidak ditulis ke source atau argumen proses.
$env:MYSQL_PWD=$env:DB_PASSWORD
$previousErrorPreference=$ErrorActionPreference
$ErrorActionPreference='Continue'
& $mysqlClient --protocol=TCP --host=127.0.0.1 "--port=$DatabasePort" --user=root --connect-timeout=3 -e 'SELECT 1' 2>$null | Out-Null
$probeExitCode=$LASTEXITCODE
$ErrorActionPreference=$previousErrorPreference
if ($probeExitCode -ne 0) {
 Remove-Item Env:MYSQL_PWD
 $sqlPassword = $env:DB_PASSWORD.Replace("'","''")
 "ALTER USER 'root'@'localhost' IDENTIFIED BY '$sqlPassword';" | & $mysqlClient --protocol=TCP --host=127.0.0.1 "--port=$DatabasePort" --user=root --skip-password
 if ($LASTEXITCODE -ne 0) { throw 'Pengaturan kredensial database gagal; file kredensial tidak dihapus.' }
 $env:MYSQL_PWD=$env:DB_PASSWORD
}
& $mysqlClient --protocol=TCP --host=127.0.0.1 "--port=$DatabasePort" --user=root -e 'CREATE DATABASE IF NOT EXISTS rsi_kelompok5 CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;'
if ($LASTEXITCODE -ne 0) { throw 'Pembuatan database gagal' }
Remove-Item Env:MYSQL_PWD
Write-Host "Database proyek siap pada 127.0.0.1:$DatabasePort."
