$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
$secret = Import-Clixml '.runtime/database-credential.xml'
$env:MYSQL_PWD = $secret.GetNetworkCredential().Password
$mysqlBase = if ($env:MYSQL_BASE) {$env:MYSQL_BASE} else {'C:\Program Files\MySQL\MySQL Server 8.0'}
$dbPort = Get-Content '.runtime/database-port.txt'
try { & (Join-Path $mysqlBase 'bin/mysqladmin.exe') --protocol=TCP --host=127.0.0.1 "--port=$dbPort" --user=root shutdown }
finally { Remove-Item Env:MYSQL_PWD }
