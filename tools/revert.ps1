<#
.SYNOPSIS  Restores the Home that was saved by preflight.ps1. Car Glass stays installed.
#>
param([Parameter(Mandatory)][string]$Device)
. "$PSScriptRoot\common.ps1"

$file = Join-Path (Get-BackupDir $Device) 'home.txt'
if (-not (Test-Path $file)) {
    throw "Tiada sandaran di $file. Cuba: adb shell cmd package set-home-activity com.android.launcher3/.Launcher"
}
$component = (Get-Content $file -Raw).Trim()
if ($component -notmatch '^[\w.]+/[\w.$]+$') { throw "Sandaran Home tidak sah: '$component'" }

Invoke-Adb $Device @('shell', 'cmd', 'package', 'set-home-activity', $component) | Out-Null
$now = Get-HomeComponent $Device
if ($now -eq $component) { Write-Host "Home asal dipulihkan: $now" -ForegroundColor Green }
else { Write-Host "Home sekarang: $now (dijangka: $component)" -ForegroundColor Yellow }
