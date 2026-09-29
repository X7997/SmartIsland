# E2E Device Automation Test Script for Smart Island
# Usage: powershell -ExecutionPolicy Bypass -File .\scripts\test_device_e2e.ps1

$ErrorActionPreference = "Continue"

function Test-AccessibilityOverlayWindow {
    param([string]$WindowDump, [string]$PackageName)
    $blocks = [regex]::Split($WindowDump, '(?m)(?=^\s*Window #\d+ Window\{)')
    foreach ($block in $blocks) {
        if ($block.Contains($PackageName) -and $block -match '\bty=ACCESSIBILITY_OVERLAY\b' -and
            $block -match 'mViewVisibility=0x0') {
            return $true
        }
    }
    return $false
}

$adbCandidates = @(
    "adb.exe",
    "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe",
    "C:\Android\platform-tools\adb.exe"
)

$adb = $null
foreach ($c in $adbCandidates) {
    if (Get-Command $c -ErrorAction SilentlyContinue) {
        $adb = $c
        break
    } elseif (Test-Path $c) {
        $adb = $c
        break
    }
}

if (-not $adb) {
    Write-Host "[ERROR] adb.exe not found in PATH or Android SDK." -ForegroundColor Red
    exit 1
}

Write-Host "====================================================" -ForegroundColor Cyan
Write-Host " [Smart Island] Automated E2E Device Verification" -ForegroundColor Cyan
Write-Host "====================================================" -ForegroundColor Cyan

# 1. Check Connected Devices
Write-Host "`n[Step 1] Checking connected devices..." -ForegroundColor Yellow
$devicesOutput = & $adb devices -l
Write-Host $devicesOutput

$deviceList = @()
foreach ($line in ($devicesOutput -split "`r?`n")) {
    if ($line -match '^(\S+)\s+device\b') {
        $deviceList += $matches[1]
    }
}

if ($deviceList.Count -eq 0) {
    Write-Host "[FAIL] No authorized Android device detected via ADB." -ForegroundColor Red
    Write-Host "Please ensure:" -ForegroundColor Yellow
    Write-Host "  1. USB Debugging is turned ON in Developer Options." -ForegroundColor Yellow
    Write-Host "  2. 'Always allow from this computer' is accepted on the phone screen." -ForegroundColor Yellow
    exit 1
}

$targetDevice = $deviceList[0]
Write-Host "[OK] Connected to device: $targetDevice" -ForegroundColor Green

# 2. Build APK
Write-Host "`n[Step 2] Compiling Debug APK..." -ForegroundColor Yellow
$buildResult = & .\gradlew.bat assembleDebug
if ($LASTEXITCODE -ne 0) {
    Write-Host "[FAIL] Gradle build failed!" -ForegroundColor Red
    exit 1
}
Write-Host "[OK] Build successful." -ForegroundColor Green

$apkPath = "app\build\outputs\apk\debug\app-debug.apk"
if (-not (Test-Path $apkPath)) {
    Write-Host "[FAIL] APK output file not found at $apkPath" -ForegroundColor Red
    exit 1
}

# 3. Install APK
Write-Host "`n[Step 3] Installing APK to $targetDevice..." -ForegroundColor Yellow
& $adb -s $targetDevice install -r -d $apkPath
if ($LASTEXITCODE -ne 0) {
    Write-Host "[FAIL] Installation failed." -ForegroundColor Red
    exit 1
}
Write-Host "[OK] Installed successfully." -ForegroundColor Green

# 4. Auto-Grant Essential Permissions via ADB
Write-Host "`n[Step 4] Configuring system permissions via ADB..." -ForegroundColor Yellow
$pkg = "com.agupta07505.smartisland"
$accService = "$pkg/$pkg.service.SmartIslandOverlayService"
$notifService = "$pkg/$pkg.service.SmartIslandNotificationListenerService"

& $adb -s $targetDevice shell "appops set $pkg SYSTEM_ALERT_WINDOW allow"
& $adb -s $targetDevice shell "appops set $pkg POST_NOTIFICATION allow"
& $adb -s $targetDevice shell "appops set $pkg GET_USAGE_STATS allow"
& $adb -s $targetDevice shell "cmd notification allow_listener $notifService"

# Enable Accessibility Service
$currentAcc = & $adb -s $targetDevice shell "settings get secure enabled_accessibility_services"
$currentAcc = $currentAcc.Trim()
if ($currentAcc -notmatch [regex]::Escape($accService)) {
    $newAcc = if ([string]::IsNullOrWhiteSpace($currentAcc) -or $currentAcc -eq "null") { $accService } else { "$($currentAcc):$($accService)" }
    & $adb -s $targetDevice shell "settings put secure enabled_accessibility_services $newAcc"
}
& $adb -s $targetDevice shell "settings put secure accessibility_enabled 1"
Write-Host "[OK] Permissions configured." -ForegroundColor Green

# 5. Cold Launch App
Write-Host "`n[Step 5] Launching Smart Island App..." -ForegroundColor Yellow
& $adb -s $targetDevice shell "am start -n $pkg/.MainActivity"
Start-Sleep -Seconds 3

# 6. Verify 2400 Topmost Window
Write-Host "`n[Step 6] Inspecting Window Hierarchy & Layer..." -ForegroundColor Yellow
$windowDump = & $adb -s $targetDevice shell "dumpsys window windows" | Out-String
if (Test-AccessibilityOverlayWindow $windowDump $pkg) {
    Write-Host "[OK] Visible accessibility overlay window belongs to Smart Island." -ForegroundColor Green
} else {
    Write-Host "[FAIL] No visible Smart Island accessibility overlay window was found." -ForegroundColor Red
    exit 1
}

# 7. Test Gesture Swipe
Write-Host "`n[Step 7] Simulating Left/Right Swipe Gestures on Island..." -ForegroundColor Yellow
# Get screen resolution
$wmSize = & $adb -s $targetDevice shell "wm size"
if ($wmSize -match '(\d+)x(\d+)') {
    $screenWidth = [int]$matches[1]
    $screenHeight = [int]$matches[2]
    $centerX = [int]($screenWidth / 2)
    $islandY = 60 # near top cutout
    
    Write-Host "  Screen: ${screenWidth}x${screenHeight}. Simulating Swipe Left on ($centerX, $islandY)..." -ForegroundColor Gray
    & $adb -s $targetDevice shell "input swipe $($centerX + 80) $islandY $($centerX - 80) $islandY 150"
    Start-Sleep -Milliseconds 500
    
    Write-Host "  Simulating Swipe Right on ($centerX, $islandY)..." -ForegroundColor Gray
    & $adb -s $targetDevice shell "input swipe $($centerX - 80) $islandY $($centerX + 80) $islandY 150"
    Start-Sleep -Milliseconds 500
}

# 8. Test Kill Background & Cold Recovery
Write-Host "`n[Step 8] Testing force-stop and launcher recovery..." -ForegroundColor Yellow
Write-Host "  Force-stopping package (this does not simulate a recent-task swipe)..." -ForegroundColor Gray
& $adb -s $targetDevice shell "am force-stop $pkg"
Start-Sleep -Seconds 1

$afterStopAcc = (& $adb -s $targetDevice shell "settings get secure enabled_accessibility_services" | Out-String).Trim()
if ($afterStopAcc -notmatch [regex]::Escape($accService)) {
    Write-Host "[BLOCKED] Device removed the accessibility authorization after force-stop. Launcher relaunch cannot recreate a type-2032 window without system authorization." -ForegroundColor Yellow
    exit 2
}

Write-Host "  Relaunching from Launcher..." -ForegroundColor Gray
& $adb -s $targetDevice shell "am start -n $pkg/.MainActivity"
Start-Sleep -Seconds 3

$afterDump = & $adb -s $targetDevice shell "dumpsys window windows" | Out-String
if (Test-AccessibilityOverlayWindow $afterDump $pkg) {
    Write-Host "[OK] Visible accessibility overlay restored after force-stop and relaunch." -ForegroundColor Green
} else {
    Write-Host "[FAIL] Smart Island accessibility overlay did not return after relaunch." -ForegroundColor Red
    exit 1
}

Write-Host "[NOTE] Injected swipes were sent; workout/media state changes and recent-task swipe still need separate verification." -ForegroundColor Yellow

Write-Host "`n====================================================" -ForegroundColor Cyan
Write-Host " [Smart Island] E2E Automation Pipeline Completed!" -ForegroundColor Cyan
Write-Host "====================================================" -ForegroundColor Cyan
