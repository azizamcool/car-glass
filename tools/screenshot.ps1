<#
.SYNOPSIS  Saves a screenshot of the unit. NOTE: the Wi-Fi name is visible on screen; do not share blindly.
#>
param(
    [Parameter(Mandatory)][string]$Device,
    [string]$Out = (Join-Path (Get-Location) ("screenshot-" + (Get-Date -Format 'yyyyMMdd-HHmmss') + '.png'))
)
. "$PSScriptRoot\common.ps1"

Invoke-Adb $Device @('shell', 'screencap', '-p', '/sdcard/cg-shot.png') | Out-Null
Invoke-Adb $Device @('pull', '/sdcard/cg-shot.png', $Out) | Out-Null
Invoke-Adb $Device @('shell', 'rm', '/sdcard/cg-shot.png') | Out-Null
Write-Host "Disimpan: $Out"
