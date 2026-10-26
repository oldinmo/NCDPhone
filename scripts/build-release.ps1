$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path $PSScriptRoot -Parent
$keyFolder = Join-Path $taskRoot '.signing'
New-Item -ItemType Directory -Force -Path $keyFolder | Out-Null
$keyFile = Join-Path $keyFolder 'ncdphone-release.jks'
$passwordFile = Join-Path $keyFolder 'password.dpapi'
if (-not (Test-Path $keyFile)) {
    if (Test-Path $passwordFile) { throw 'Password exists without keystore; restore the original keystore before building.' }
    $keyBytes = New-Object byte[] 32
    $rng = [System.Security.Cryptography.RandomNumberGenerator]::Create()
    $rng.GetBytes($keyBytes)
    $keyPassword = [Convert]::ToBase64String($keyBytes)
    ConvertTo-SecureString $keyPassword -AsPlainText -Force | ConvertFrom-SecureString | Set-Content $passwordFile
    $env:NCD_STORE_PASSWORD = $keyPassword
    & "$env:JAVA_HOME/bin/keytool.exe" -genkeypair -keystore $keyFile -storepass:env NCD_STORE_PASSWORD -keypass:env NCD_STORE_PASSWORD -alias ncdphone -keyalg RSA -keysize 3072 -validity 10000 -dname 'CN=NCDPhone, OU=Elder Care, O=NCDPhone, C=TW'
    if ($LASTEXITCODE -ne 0) { throw 'Signing key generation failed' }
}
$secure = Get-Content $passwordFile | ConvertTo-SecureString
$env:NCD_STORE_PASSWORD = [System.Net.NetworkCredential]::new('', $secure).Password
$env:NCD_KEY_PASSWORD = $env:NCD_STORE_PASSWORD
$env:NCD_KEY_ALIAS = 'ncdphone'
$env:NCD_KEYSTORE = $keyFile
try {
    Push-Location $taskRoot
    & .\gradlew.bat assembleRelease lintRelease --no-daemon
    if ($LASTEXITCODE -ne 0) { throw 'Release build failed' }
    New-Item -ItemType Directory -Force -Path dist | Out-Null
    $version = ((Get-Content version.properties | Where-Object { $_ -like 'versionName=*' }) -split '=',2)[1]
    Copy-Item app/build/outputs/apk/release/app-release.apk "dist/NCDPhone-$version.apk"
    Get-FileHash "dist/NCDPhone-$version.apk" -Algorithm SHA256 | ForEach-Object { "$($_.Hash.ToLower())  NCDPhone-$version.apk" } | Set-Content "dist/SHA256SUMS.txt"
} finally {
    Pop-Location
    Remove-Item Env:NCD_STORE_PASSWORD,Env:NCD_KEY_PASSWORD,Env:NCD_KEY_ALIAS,Env:NCD_KEYSTORE -ErrorAction SilentlyContinue
}
