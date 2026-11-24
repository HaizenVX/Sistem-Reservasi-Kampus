param([switch]$UseEnvironment)
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
& "$PSScriptRoot/setup.ps1"
if (!$UseEnvironment) { . "$PSScriptRoot/start-database.ps1" }
& java -cp 'build;lib/*' Database --init
if ($LASTEXITCODE -ne 0) { throw 'Skema database gagal disiapkan' }
& "$PSScriptRoot/setup-accounts.ps1"
& java -cp 'build;lib/*' ApiServer
