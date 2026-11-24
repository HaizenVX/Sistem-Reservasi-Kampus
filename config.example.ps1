# Contoh untuk MySQL yang dikelola sendiri. Jangan isi password di source code.
$env:DB_URL = 'jdbc:mysql://127.0.0.1:3306/rsi_kelompok5?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=Asia/Jakarta'
$env:DB_USER = 'rsi_app'
$credential = Get-Credential -UserName $env:DB_USER -Message 'Kredensial database lokal'
$env:DB_PASSWORD = $credential.GetNetworkCredential().Password
$env:PORT = '8080'
