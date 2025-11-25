# Script to check backend status
# Usage: .\check-backend-status.ps1

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Qhome Base Backend Status Check" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# Get the script directory
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $scriptDir

# 1. Check if port 8989 is in use
Write-Host "1. Checking if port 8989 is in use..." -ForegroundColor Yellow
$port8989 = Get-NetTCPConnection -LocalPort 8989 -ErrorAction SilentlyContinue
if ($port8989) {
    Write-Host "   ✅ Port 8989 is in use" -ForegroundColor Green
    $process = Get-Process -Id $port8989.OwningProcess -ErrorAction SilentlyContinue
    if ($process) {
        Write-Host "   Process: $($process.ProcessName) (PID: $($process.Id))" -ForegroundColor Gray
    }
} else {
    Write-Host "   ❌ Port 8989 is NOT in use - Backend is NOT running!" -ForegroundColor Red
    Write-Host "   Solution: Run .\start-all-services.ps1" -ForegroundColor Yellow
}
Write-Host ""

# 2. Check if API Gateway responds
Write-Host "2. Checking if API Gateway responds..." -ForegroundColor Yellow
try {
    $response = Invoke-WebRequest -Uri "http://localhost:8989/api/health" -Method Get -TimeoutSec 3 -ErrorAction Stop
    if ($response.StatusCode -eq 200) {
        Write-Host "   ✅ API Gateway is responding (localhost:8989)" -ForegroundColor Green
    }
} catch {
    Write-Host "   ❌ API Gateway is NOT responding on localhost:8989" -ForegroundColor Red
    Write-Host "   Error: $($_.Exception.Message)" -ForegroundColor Gray
}
Write-Host ""

# 3. Get current IP addresses
Write-Host "3. Current IP addresses:" -ForegroundColor Yellow
$ipAddresses = Get-NetIPAddress -AddressFamily IPv4 | 
    Where-Object { $_.IPAddress -notlike "127.*" -and $_.IPAddress -notlike "169.254.*" } | 
    Select-Object IPAddress, InterfaceAlias

if ($ipAddresses) {
    foreach ($ip in $ipAddresses) {
        Write-Host "   📍 $($ip.IPAddress) ($($ip.InterfaceAlias))" -ForegroundColor Cyan
        
        # Try to connect to this IP
        try {
            $response = Invoke-WebRequest -Uri "http://$($ip.IPAddress):8989/api/health" -Method Get -TimeoutSec 2 -ErrorAction Stop
            if ($response.StatusCode -eq 200) {
                Write-Host "      ✅ Backend is accessible on this IP" -ForegroundColor Green
            }
        } catch {
            Write-Host "      ❌ Backend is NOT accessible on this IP" -ForegroundColor Red
        }
    }
} else {
    Write-Host "   ❌ No IP addresses found" -ForegroundColor Red
}
Write-Host ""

# 4. Check ngrok status
Write-Host "4. Checking ngrok status..." -ForegroundColor Yellow
try {
    $ngrokResponse = Invoke-RestMethod -Uri "http://localhost:4040/api/tunnels" -Method Get -TimeoutSec 2 -ErrorAction Stop
    if ($ngrokResponse.tunnels -and $ngrokResponse.tunnels.Count -gt 0) {
        $httpsTunnel = $ngrokResponse.tunnels | Where-Object { $_.public_url -like "https://*" } | Select-Object -First 1
        if ($httpsTunnel) {
            Write-Host "   ✅ Ngrok is running" -ForegroundColor Green
            Write-Host "   Public URL: $($httpsTunnel.public_url)" -ForegroundColor Cyan
        } else {
            Write-Host "   ⚠️ Ngrok is running but no HTTPS tunnel found" -ForegroundColor Yellow
        }
    } else {
        Write-Host "   ⚠️ Ngrok is running but no tunnels found" -ForegroundColor Yellow
    }
} catch {
    Write-Host "   ℹ️ Ngrok is NOT running" -ForegroundColor Gray
    Write-Host "   (This is OK if you're using local network)" -ForegroundColor Gray
}
Write-Host ""

# 5. Check VNPAY_BASE_URL
Write-Host "5. Current VNPAY_BASE_URL:" -ForegroundColor Yellow
if ($env:VNPAY_BASE_URL) {
    Write-Host "   ✅ VNPAY_BASE_URL = $env:VNPAY_BASE_URL" -ForegroundColor Green
} else {
    Write-Host "   ⚠️ VNPAY_BASE_URL is NOT set" -ForegroundColor Yellow
    Write-Host "   (Will be auto-set when starting services)" -ForegroundColor Gray
}
Write-Host ""

# 6. Summary and recommendations
Write-Host "========================================" -ForegroundColor Cyan
Write-Host "  Summary & Recommendations" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

if (-not $port8989) {
    Write-Host "❌ Backend is NOT running!" -ForegroundColor Red
    Write-Host "   → Run: .\start-all-services.ps1" -ForegroundColor Yellow
    Write-Host ""
} else {
    Write-Host "✅ Backend appears to be running" -ForegroundColor Green
    Write-Host ""
    Write-Host "If Flutter app still can't connect:" -ForegroundColor Yellow
    Write-Host "   1. Make sure your device is on the same WiFi/LAN network" -ForegroundColor White
    Write-Host "   2. Check firewall settings (allow port 8989)" -ForegroundColor White
    Write-Host "   3. Use the IP address shown above in Flutter app" -ForegroundColor White
    Write-Host "   4. If using ngrok, use the ngrok URL shown above" -ForegroundColor White
    Write-Host ""
}

Write-Host "Press any key to exit..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")

