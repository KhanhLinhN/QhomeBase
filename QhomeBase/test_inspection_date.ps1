# PowerShell script để test API inspectionDate
# Usage: .\test_inspection_date.ps1 -ContractId {contractId} [-Token {token}]

param(
    [Parameter(Mandatory=$true)]
    [string]$ContractId,
    
    [Parameter(Mandatory=$false)]
    [string]$Token
)

$apiUrl = "http://localhost:8080/api/contracts/$ContractId"

Write-Host "Testing API: $apiUrl" -ForegroundColor Cyan
Write-Host ""

$headers = @{
    "Content-Type" = "application/json"
}

if ($Token) {
    $headers["Authorization"] = "Bearer $Token"
    Write-Host "✅ Using provided token..." -ForegroundColor Green
} else {
    Write-Host "⚠️  No token provided, testing without authentication..." -ForegroundColor Yellow
}

try {
    $response = Invoke-RestMethod -Uri $apiUrl -Method Get -Headers $headers -ErrorAction Stop
    
    if ($response.inspectionDate) {
        Write-Host "✅ SUCCESS: API response contains 'inspectionDate' field" -ForegroundColor Green
        Write-Host ""
        Write-Host "inspectionDate value: $($response.inspectionDate)" -ForegroundColor Green
    } else {
        Write-Host "❌ ERROR: API response does NOT contain 'inspectionDate' field" -ForegroundColor Red
        Write-Host ""
        Write-Host "Available fields:" -ForegroundColor Yellow
        $response.PSObject.Properties.Name | ForEach-Object { Write-Host "  - $_" }
    }
    
    Write-Host ""
    Write-Host "Full response:" -ForegroundColor Cyan
    $response | ConvertTo-Json -Depth 10
    
} catch {
    Write-Host "❌ ERROR: Failed to call API" -ForegroundColor Red
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
    
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $responseBody = $reader.ReadToEnd()
        Write-Host "Response body: $responseBody" -ForegroundColor Yellow
    }
}



