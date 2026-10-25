# Shared helpers for the Car Glass tools. Dot-source: . "$PSScriptRoot\common.ps1"
$ErrorActionPreference = 'Stop'

function Get-Adb {
    $c = Get-Command adb -ErrorAction SilentlyContinue
    if ($c) { return $c.Source }
    $candidates = @(
        "$env:LOCALAPPDATA\Microsoft\WinGet\Packages\Google.PlatformTools_Microsoft.Winget.Source_8wekyb3d8bbwe\platform-tools\adb.exe",
        "$env:ANDROID_HOME\platform-tools\adb.exe",
        "$env:ANDROID_SDK_ROOT\platform-tools\adb.exe"
    )
    foreach ($p in $candidates) {
        if ($p -and (Test-Path $p)) { return $p }
    }
    throw "adb tidak dijumpai. Pasang Android platform-tools (contoh: winget install Google.PlatformTools)."
}

# Runs adb (optionally against one device) and returns the output lines.
function Invoke-Adb {
    param([string]$Device, [string[]]$AdbArgs)
    $adb = Get-Adb
    if ($Device) { $AdbArgs = @('-s', $Device) + $AdbArgs }
    $out = & $adb @AdbArgs 2>&1
    return @($out | ForEach-Object { "$_" })
}

function Get-Prop {
    param([string]$Device, [string]$Name)
    ((Invoke-Adb $Device @('shell', 'getprop', $Name)) -join '').Trim()
}

# Stable folder name for one physical unit (the wireless port changes on every boot).
function Get-DeviceKey {
    param([string]$Device)
    $model = Get-Prop $Device 'ro.product.model'
    $serial = Get-Prop $Device 'ro.serialno'
    $key = ("{0}_{1}" -f $model, $serial) -replace '[^A-Za-z0-9._-]', '_'
    if (-not $key.Trim('_')) { $key = ($Device -replace '[^A-Za-z0-9._-]', '_') }
    return $key
}

function Get-BackupDir {
    param([string]$Device)
    Join-Path (Join-Path (Split-Path $PSScriptRoot -Parent) 'backup') (Get-DeviceKey $Device)
}

# Component of the current default Home, e.g. com.android.launcher3/.Launcher
function Get-HomeComponent {
    param([string]$Device)
    $lines = Invoke-Adb $Device @('shell', 'cmd', 'package', 'resolve-activity', '--brief',
        '-a', 'android.intent.action.MAIN', '-c', 'android.intent.category.HOME')
    ($lines | Where-Object { $_ -match '^[\w.]+/[\w.$]+$' } | Select-Object -Last 1)
}
