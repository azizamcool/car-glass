<#
.SYNOPSIS  Optional "Pakej Ringan": fewer background apps and shorter animations. Fully reversible.
.DESCRIPTION
  Only run with the user's consent (AGENTS.md, Fasa 6). Run tools\ram-audit.ps1 first and show the user the evidence.
  -Apply    animator scale 0.5; Play Store and Chrome may not run in the background; disables, for user 0,
            apps that are unused or leftovers of the first-time setup.
  -Restore  puts everything back (re-enables the packages, resets the background limits, animator scale to default).
  Never touches vendor packages (com.imotor.*, com.clw.*), Google Play services/Store/Protect, the keyboard or the TTS engine.
#>
param(
    [Parameter(Mandatory)][string]$Device,
    [switch]$Apply,
    [switch]$Restore
)
. "$PSScriptRoot\common.ps1"

if ($Apply -eq $Restore) { throw "Pilih tepat satu: -Apply atau -Restore." }

$disable = @(
    'com.android.gallery3d',                      # gallery, unused
    'com.google.android.partnersetup',            # first-time setup leftover
    'com.google.android.onetimeinitializer',
    'com.google.android.feedback',
    'com.google.android.syncadapters.calendar',
    'com.google.android.syncadapters.contacts',
    'com.android.providers.partnerbookmarks'
)
$limitBackground = @('com.android.vending', 'com.android.chrome')

if ($Apply) {
    $dir = Get-BackupDir $Device
    New-Item -ItemType Directory -Force $dir | Out-Null
    $before = @("tarikh=$(Get-Date -Format s)",
        "animator_duration_scale=$((Invoke-Adb $Device @('shell', 'settings', 'get', 'global', 'animator_duration_scale')) -join '')")
    foreach ($p in $disable) {
        $on = (Invoke-Adb $Device @('shell', 'pm', 'list', 'packages', '-e', $p)) -contains "package:$p"
        $before += "$p=$(if ($on) { 'aktif' } else { 'tidak aktif/tiada' })"
    }
    $before | Set-Content (Join-Path $dir 'lightweight-before.txt') -Encoding utf8

    Invoke-Adb $Device @('shell', 'settings', 'put', 'global', 'animator_duration_scale', '0.5') | Out-Null
    foreach ($p in $limitBackground) {
        foreach ($op in 'RUN_IN_BACKGROUND', 'RUN_ANY_IN_BACKGROUND') {
            Invoke-Adb $Device @('shell', 'cmd', 'appops', 'set', $p, $op, 'ignore') | Out-Null
        }
    }
    foreach ($p in $disable) {
        $r = (Invoke-Adb $Device @('shell', 'pm', 'disable-user', '--user', '0', $p)) -join ' '
        Write-Host ("{0,-46} {1}" -f $p, $r)
    }
    Write-Host "Pakej Ringan dilaksanakan. Pulih: .\tools\lighten.ps1 -Device $Device -Restore" -ForegroundColor Green
    return
}

# -Restore
Invoke-Adb $Device @('shell', 'settings', 'delete', 'global', 'animator_duration_scale') | Out-Null
foreach ($p in $limitBackground) { Invoke-Adb $Device @('shell', 'cmd', 'appops', 'reset', $p) | Out-Null }
foreach ($p in $disable) {
    $r = (Invoke-Adb $Device @('shell', 'pm', 'enable', '--user', '0', $p)) -join ' '
    Write-Host ("{0,-46} {1}" -f $p, $r)
}
Write-Host "Pakej Ringan dipulihkan." -ForegroundColor Green
