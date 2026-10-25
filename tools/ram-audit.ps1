<#
.SYNOPSIS  Reads memory, CPU and app-usage facts from the unit. Changes nothing.
.DESCRIPTION
  Prints: RAM summary, the heaviest processes, apps that are alive but barely used, Play Store auto-update
  activity and installed versions. It only reads. Removing or disabling anything is a separate step that the
  assistant must propose to the user first (see AGENTS.md, "Fasa 6: Optimum").
#>
param([Parameter(Mandatory)][string]$Device, [int]$Top = 20)
. "$PSScriptRoot\common.ps1"
$ErrorActionPreference = 'Continue'

Write-Host "== RAM ==" -ForegroundColor Cyan
$mem = Invoke-Adb $Device @('shell', 'dumpsys', 'meminfo')
$mem | Where-Object { $_ -match '^\s*(Total|Free|Used|Lost) RAM|^\s*ZRAM' } | ForEach-Object { "  $($_.Trim())" }
Write-Host "(Free RAM sudah termasuk cache. Cache yang besar adalah normal dan membantu.)"

Write-Host "`n== Proses paling berat (PSS) ==" -ForegroundColor Cyan
$all = @($mem | Where-Object { $_ -match '^\s+[\d,]+K: \S+ \(pid \d+' } | Select-Object -Unique)
$all | Select-Object -First $Top | ForEach-Object { "  $($_.Trim())" }
$playStore = @($all | ForEach-Object { if ($_ -match 'K: (com\.android\.vending\S*) \(pid') { $Matches[1] } } | Select-Object -Unique)
if ($playStore.Count -ge 2) {
    Write-Host "  ! Play Store berjalan dalam $($playStore.Count) proses: kemungkinan sedang mengemas kini app secara automatik." -ForegroundColor Yellow
}

Write-Host "`n== Penggunaan app sebulan (masa foreground) ==" -ForegroundColor Cyan
$u = (Invoke-Adb $Device @('shell', 'dumpsys usagestats')) -join "`n"
$i = $u.IndexOf('In-memory monthly stats')
if ($i -ge 0) { $u = $u.Substring($i) }
$end = $u.IndexOf('In-memory yearly'); if ($end -gt 0) { $u = $u.Substring(0, $end) }
$rows = [regex]::Matches($u, 'package=(\S+) totalTimeUsed="?([\d:]+)"? lastTimeUsed="?([\d\- :]+)"?') |
    ForEach-Object { [pscustomobject]@{ Package = $_.Groups[1].Value; Foreground = $_.Groups[2].Value; Last = $_.Groups[3].Value.Trim() } } |
    Sort-Object Package -Unique |
    Where-Object { $_.Package -match '^(com\.(google|netflix|spotify|waze|adobe|pintaz|android\.(chrome|gallery3d|vending))|com\.imotor\.(music|video|btmusic|fmam|phoneconnect|dialer))' }
$rows | Sort-Object Foreground -Descending | Format-Table -AutoSize | Out-String | Write-Host
Write-Host "Calon untuk ditanya kepada pengguna (digunakan kurang daripada 1 minit sebulan atau tidak pernah):"
$rows | Where-Object { $_.Foreground -match '^00:[0-5]\d$' -and $_.Package -notmatch 'gms|gsf|vending|verifier|webview|tts|inputmethod|partnersetup' } |
    Where-Object { $_.Package -notmatch 'overlay|ext\.shared|onetimeinitializer|syncadapters|location\.history|com\.imotor\.|com\.clw\.' } |
    ForEach-Object { "  - $($_.Package)  ($($_.Foreground))" }
Write-Host "(Pakej com.imotor.* dan com.clw.* ialah pakej pengeluar: jangan cadangkan untuk dibuang.)"

Write-Host "`n== Versi dan kemas kini ==" -ForegroundColor Cyan
"  Android   : $(Get-Prop $Device 'ro.build.version.release'), tampalan keselamatan $(Get-Prop $Device 'ro.build.version.security_patch')"
"  Firmware  : $(Get-Prop $Device 'ro.build.fingerprint')"
Write-Host "  Kemas kini Android datang daripada pembekal (firmware). Tanya pembekal; jangan flash tanpa panduan mereka."
