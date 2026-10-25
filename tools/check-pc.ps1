<#
.SYNOPSIS  Checks this computer. Installs nothing and does not touch any device.
#>
. "$PSScriptRoot\common.ps1"
$ErrorActionPreference = 'Continue'

Write-Host "== Komputer ==" -ForegroundColor Cyan
Write-Host ("Windows: " + [Environment]::OSVersion.VersionString)
Write-Host ("PowerShell: " + $PSVersionTable.PSVersion)

try {
    $adb = Get-Adb
    Write-Host "adb: $adb"
    (& $adb version) | Select-Object -First 1 | ForEach-Object { Write-Host "  $_" }
} catch { Write-Host "adb: TIADA. $($_.Exception.Message)" -ForegroundColor Yellow }

$java = Get-Command java -ErrorAction SilentlyContinue
if ($java) {
    Write-Host "java: $($java.Source)"
    (& java -version 2>&1) | Select-Object -First 1 | ForEach-Object { Write-Host "  $_" }
} else {
    Write-Host "java: TIADA (hanya perlu untuk membina APK; JDK 17)" -ForegroundColor Yellow
}

foreach ($v in 'ANDROID_HOME', 'ANDROID_SDK_ROOT') {
    $val = [Environment]::GetEnvironmentVariable($v)
    if (-not $val) { $val = '(tidak ditetapkan, hanya perlu untuk membina APK)' }
    Write-Host "$v : $val"
}

$apk = Join-Path (Split-Path $PSScriptRoot -Parent) 'app\build\outputs\apk\debug\app-debug.apk'
if (Test-Path $apk) { Write-Host "APK sedia ada: $apk" }
else { Write-Host "APK sedia ada: tiada (bina dengan gradlew.bat assembleDebug)" }
