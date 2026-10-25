<#
.SYNOPSIS  Reads facts from the unit and writes a backup. Changes nothing on the unit.
.DESCRIPTION
  Saves the package list and the current Home to backup\<model_serial>\ (git-ignored) so
  tools\revert.ps1 can restore it. Exit code 2 means the unit does not meet a requirement.
#>
param([Parameter(Mandatory)][string]$Device)
. "$PSScriptRoot\common.ps1"

$model   = Get-Prop $Device 'ro.product.model'
$release = Get-Prop $Device 'ro.build.version.release'
$sdk     = [int](Get-Prop $Device 'ro.build.version.sdk')
$cpu     = Get-Prop $Device 'ro.board.platform'
$mem     = ((Invoke-Adb $Device @('shell', 'grep', 'MemTotal', '/proc/meminfo')) -join ' ') -replace '\s+', ' '
$size    = ((Invoke-Adb $Device @('shell', 'wm', 'size')) | Select-Object -Last 1)
$uptime  = ((Invoke-Adb $Device @('shell', 'uptime')) -join ' ').Trim()
$homeNow = Get-HomeComponent $Device
$gps     = ((Invoke-Adb $Device @('shell', 'pm', 'list', 'features')) -join "`n") -match 'android.hardware.location.gps'

Write-Host "== Unit ==" -ForegroundColor Cyan
"Model        : $model"
"Android      : $release (SDK $sdk)"
"Platform     : $cpu"
"RAM          : $mem"
"Skrin        : $size"
"Uptime       : $uptime"
"Home semasa  : $homeNow"
if ($gps) { "GPS          : ada" } else { "GPS          : TIADA" }

$blocked = @()
if ($sdk -lt 28) { $blocked += "Android terlalu lama (perlu SDK 28 / Android 9 ke atas)." }
if (-not $gps)   { $blocked += "Tiada perkakasan GPS, speedometer tak akan berfungsi." }
if ($size -match '(\d+)x(\d+)') {
    if ([int]$Matches[1] -lt [int]$Matches[2]) { $blocked += "Skrin bukan landskap ($($Matches[1])x$($Matches[2]))." }
}

Write-Host ""
Write-Host "== Pakej yang dirujuk oleh pintasan (maklumat sahaja) ==" -ForegroundColor Cyan
$packages = Invoke-Adb $Device @('shell', 'pm', 'list', 'packages')
$wanted = 'com.imotor.fmam', 'com.imotor.btmusic', 'com.imotor.music', 'com.imotor.video', 'com.imotor.phoneconnect',
          'com.google.android.youtube', 'com.google.android.apps.youtube.music', 'com.google.android.apps.maps', 'com.android.vending'
foreach ($w in $wanted) {
    $found = $packages -contains "package:$w"
    $label = if ($found) { 'ada' } else { 'tiada' }
    "{0,-45} {1}" -f $w, $label
}
$vendor = @($packages | Where-Object { $_ -match 'package:com\.(imotor|clw|carletter|ms)\.' })
Write-Host ("Pakej pengeluar dijumpai: {0} (jangan sentuh)" -f $vendor.Count)

$dir = Get-BackupDir $Device
New-Item -ItemType Directory -Force $dir | Out-Null
$packages | Set-Content (Join-Path $dir 'packages.txt') -Encoding utf8
if ($homeNow -eq 'my.carglass/.MainActivity') {
    # Never overwrite the saved original Home with Car Glass itself.
    Write-Host "Home semasa sudah Car Glass; sandaran Home asal TIDAK ditulis ganti." -ForegroundColor Yellow
    if (-not (Test-Path (Join-Path $dir 'home.txt'))) {
        Write-Host "Tiada sandaran Home asal untuk unit ini. Tanya pengguna Home asalnya (biasanya com.android.launcher3/.Launcher)." -ForegroundColor Yellow
    }
} else {
    $homeNow | Set-Content (Join-Path $dir 'home.txt') -Encoding utf8
}
@("model=$model", "android=$release", "sdk=$sdk", "platform=$cpu", "saved=$(Get-Date -Format s)") |
    Set-Content (Join-Path $dir 'device.txt') -Encoding utf8
Write-Host ""
Write-Host "Sandaran disimpan di: $dir" -ForegroundColor Green

if ($blocked.Count -gt 0) {
    Write-Host ""
    Write-Host "BERHENTI:" -ForegroundColor Red
    $blocked | ForEach-Object { Write-Host "  - $_" -ForegroundColor Red }
    exit 2
}
Write-Host "Semakan lulus. Belum ada yang diubah di unit." -ForegroundColor Green
