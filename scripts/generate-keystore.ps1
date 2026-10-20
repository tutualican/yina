param(
    [string]$Alias = "yina",
    [string]$OutFile = "release.jks",
    [string]$StorePass = "",
    [string]$KeyPass = ""
)

$ErrorActionPreference = "Stop"
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

if (Test-Path $OutFile) {
    throw "$OutFile already exists. Move it somewhere safe before generating a new key."
}

function Read-PlainPassword([string]$Prompt) {
    $secure = Read-Host $Prompt -AsSecureString
    $bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
    try {
        return [Runtime.InteropServices.Marshal]::PtrToStringAuto($bstr)
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
    }
}

if ([string]::IsNullOrEmpty($StorePass)) {
    $StorePass = Read-PlainPassword "Keystore password (at least 6 characters)"
}
if ([string]::IsNullOrEmpty($KeyPass)) {
    $KeyPass = $StorePass
}
if ($StorePass.Length -lt 6) {
    throw "The keystore password must be at least 6 characters."
}

Write-Host "Generating $OutFile ..."
& keytool -genkeypair -v `
    -keystore $OutFile `
    -alias $Alias `
    -keyalg RSA -keysize 4096 -validity 10000 `
    -storepass $StorePass -keypass $KeyPass `
    -dname "CN=Yina, O=Yina"
if ($LASTEXITCODE -ne 0) {
    throw "keytool failed with exit code $LASTEXITCODE"
}

$props = @"
storeFile=$OutFile
storePassword=$StorePass
keyAlias=$Alias
keyPassword=$KeyPass
"@
[IO.File]::WriteAllText(
    (Join-Path $root "keystore.properties"),
    $props,
    (New-Object System.Text.UTF8Encoding($false))
)

Write-Host ""
Write-Host "Created $OutFile and keystore.properties." -ForegroundColor Green
Write-Host "Both are gitignored. Back them up: losing them prevents updates to an installed app."
Write-Host ""
Write-Host "For the GitHub release workflow, add these repository secrets:"
Write-Host "  KEYSTORE_BASE64   (base64 of $OutFile)"
Write-Host "  KEYSTORE_PASSWORD"
Write-Host "  KEY_ALIAS         = $Alias"
Write-Host "  KEY_PASSWORD"
