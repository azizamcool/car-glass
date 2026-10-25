<#
.SYNOPSIS  Installs Car Glass. Without -SetHome it is only an ordinary app (Home is unchanged).
.EXAMPLE   .\install.ps1 -Device 192.168.1.50:41569
.EXAMPLE   .\install.ps1 -Device 192.168.1.50:41569 -SetHome
#>
param(
    [Parameter(Mandatory)][string]$Device,
    [string]$Apk = (Join-Path (Split-Path $PSScriptRoot -Parent) 'app\build\outputs\apk\debug\app-debug.apk'),
    [switch]$SetHome
)
. "$PSScriptRoot\common.ps1"

if (-not (Test-Path $Apk)) { throw "APK tiada di $Apk. Bina dahulu dengan gradlew.bat assembleDebug." }
$backup = Join-Path (Get-BackupDir $Device) 'home.txt'
if (-not (Test-Path $backup)) { throw "Tiada sandaran Home. Jalankan tools\preflight.ps1 dahulu." }

Write-Host "Memasang $Apk ..."
$r = Invoke-Adb $Device @('install', '-r', $Apk)
$r | ForEach-Object { Write-Host "  $_" }
if (-not ($r -match 'Success')) { throw "Pemasangan gagal." }

Invoke-Adb $Device @('shell', 'pm', 'grant', 'my.carglass', 'android.permission.ACCESS_FINE_LOCATION') | Out-Null
Invoke-Adb $Device @('shell', 'am', 'start', '-n', 'my.carglass/.MainActivity') | Out-Null
Write-Host "Car Glass dibuka sebagai app biasa." -ForegroundColor Green

if ($SetHome) {
    Invoke-Adb $Device @('shell', 'cmd', 'package', 'set-home-activity', 'my.carglass/.MainActivity') | Out-Null
    $now = Get-HomeComponent $Device
    if ($now -eq 'my.carglass/.MainActivity') {
        Write-Host "Home lalai sekarang: $now" -ForegroundColor Green
        Write-Host "Kembali ke asal: .\tools\revert.ps1 -Device $Device"
    } else {
        Write-Host "Home lalai masih: $now (penetapan tidak berjaya)" -ForegroundColor Yellow
    }
} else {
    Write-Host "Home tidak diubah. Tambah -SetHome selepas anda berpuas hati."
}
