param(
    [string]$Alias = "yina",
    [string]$OutFile = "release.jks"
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

if (Test-Path $OutFile) {
    Write-Error "$OutFile zaten var. Önce yedekle, sonra sil."
}

$storePass = Read-Host "Keystore parolası (en az 6 karakter)" -AsSecureString
$keyPass = Read-Host "Anahtar parolası (boş bırak = keystore ile aynı)" -AsSecureString

$storePlain = [Runtime.InteropServices.Marshal]::PtrToStringAuto(
    [Runtime.InteropServices.Marshal]::SecureStringToBSTR($storePass)
)
$keyPlain = [Runtime.InteropServices.Marshal]::PtrToStringAuto(
    [Runtime.InteropServices.Marshal]::SecureStringToBSTR($keyPass)
)
if ([string]::IsNullOrEmpty($keyPlain)) { $keyPlain = $storePlain }

Write-Host "Anahtar üretiliyor..." -ForegroundColor Cyan
& keytool -genkeypair -v `
    -keystore $OutFile `
    -alias $Alias `
    -keyalg RSA -keysize 4096 -validity 10000 `
    -storepass $storePlain -keypass $keyPlain `
    -dname "CN=Yina, OU=Mobile, O=Yina, L=Istanbul, C=TR"

$props = @"
storeFile=$OutFile
storePassword=$storePlain
keyAlias=$Alias
keyPassword=$keyPlain
"@
[IO.File]::WriteAllText(
    (Join-Path $root "keystore.properties"),
    $props,
    (New-Object System.Text.UTF8Encoding($false))
)

$b64 = [Convert]::ToBase64String([IO.File]::ReadAllBytes((Join-Path $root $OutFile)))

Write-Host ""
Write-Host "Tamam. Şimdi:" -ForegroundColor Green
Write-Host "  1) $OutFile ve keystore.properties dosyalarını yedekle (ikisi de gitignore'da)."
Write-Host "  2) GitHub > Settings > Secrets > Actions içine şunları ekle:"
Write-Host "     KEYSTORE_BASE64  = aşağıdaki tek satırlık değer"
Write-Host "     KEYSTORE_PASSWORD= $storePlain"
Write-Host "     KEY_ALIAS        = $Alias"
Write-Host "     KEY_PASSWORD     = $keyPlain"
Write-Host ""
Write-Host $b64
