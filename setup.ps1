$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
New-Item -ItemType Directory -Force lib,build | Out-Null
$dependencies = @{
 'mysql-connector-j-9.2.0.jar' = 'https://repo.maven.apache.org/maven2/com/mysql/mysql-connector-j/9.2.0/mysql-connector-j-9.2.0.jar'
 'gson-2.12.1.jar' = 'https://repo.maven.apache.org/maven2/com/google/code/gson/gson/2.12.1/gson-2.12.1.jar'
}
foreach ($name in $dependencies.Keys) {
 if (!(Test-Path "lib/$name")) { Invoke-WebRequest $dependencies[$name] -OutFile "lib/$name" }
}
& javac -encoding UTF-8 -cp 'lib/*' -d build (Get-ChildItem -File *.java).Name
if ($LASTEXITCODE -ne 0) { throw 'Kompilasi gagal' }
