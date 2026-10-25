<#
.SYNOPSIS  Connects to a head unit over wireless ADB (optionally pairing first).
.EXAMPLE   .\connect.ps1 -Target 192.168.1.50:41569
.EXAMPLE   .\connect.ps1 -Target 192.168.1.50:41569 -PairWith 192.168.1.50:44775 -Code 123456
#>
param(
    [Parameter(Mandatory)][string]$Target,
    [string]$PairWith,
    [string]$Code
)
. "$PSScriptRoot\common.ps1"

if ($PairWith) {
    if (-not $Code) { throw "-Code (6 digit) diperlukan bersama -PairWith." }
    Write-Host "Pairing dengan $PairWith ..."
    Invoke-Adb $null @('pair', $PairWith, $Code) | ForEach-Object { Write-Host "  $_" }
}
Write-Host "Menyambung ke $Target ..."
Invoke-Adb $null @('connect', $Target) | ForEach-Object { Write-Host "  $_" }

$state = ((Invoke-Adb $Target @('get-state')) -join '').Trim()
if ($state -ne 'device') {
    throw "Unit tidak bersambung (keadaan: '$state'). Semak: Wireless debugging hidup, port betul, komputer dan unit di Wi-Fi yang sama, dan skrin unit tidak menunggu kebenaran."
}
Write-Host "Bersambung: $Target" -ForegroundColor Green
