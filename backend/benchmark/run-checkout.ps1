[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [long]$ProductId,

    [ValidateRange(1, 100)]
    [int]$UserCount = 4,

    [ValidateSet("burst", "sustained")]
    [string]$Mode = "burst",

    [string]$Duration = "30s",

    [switch]$AllowInsufficientStock,

    [uri]$BaseUri = [uri]"http://localhost:8080"
)

$ErrorActionPreference = "Stop"

if ($BaseUri.Scheme -ne "http" -or
    $BaseUri.Host -notin @("localhost", "127.0.0.1", "::1")) {
    throw "Safety check: checkout benchmark is allowed only against localhost HTTP."
}

if (-not (Get-Command k6 -ErrorAction SilentlyContinue)) {
    throw "k6 was not found. Install it first (for example: winget install k6 --source winget)."
}

$password = [Environment]::GetEnvironmentVariable("BENCHMARK_USER_PASSWORD")
$securePassword = $null
$passwordPointer = [IntPtr]::Zero
if ([string]::IsNullOrWhiteSpace($password)) {
    $securePassword = Read-Host "Local benchmark user password (8+ characters)" -AsSecureString
    $passwordPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
    try {
        $password = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($passwordPointer)
    }
    finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordPointer)
    }
}

if ([string]::IsNullOrWhiteSpace($password) -or $password.Length -lt 8) {
    if ($null -ne $securePassword) {
        $securePassword.Dispose()
    }
    $password = $null
    throw "The local benchmark password must contain at least 8 characters."
}

$environmentNames = @(
    "BENCHMARK_USER_PASSWORD",
    "K6_BEARER_TOKENS",
    "PRODUCT_ID",
    "VUS",
    "BASE_URL",
    "MODE",
    "DURATION",
    "ALLOW_INSUFFICIENT_STOCK"
)
$previousEnvironment = @{}
foreach ($name in $environmentNames) {
    $previousEnvironment[$name] = [Environment]::GetEnvironmentVariable($name, "Process")
}

$runId = Get-Date -Format "yyyyMMdd-HHmmss-fff"
$summaryPath = "checkout-$runId-summary.json"
$rawPath = "checkout-$runId-raw.json"
$base = $BaseUri.GetLeftPart([System.UriPartial]::Authority).TrimEnd("/")

try {
    $product = Invoke-RestMethod -Method Get -Uri "$base/api/v1/products/$ProductId"
    if (-not $product.active) {
        throw "Product $ProductId is inactive."
    }

    $runId = [guid]::NewGuid().ToString("N").Substring(0, 12)
    $tokens = [System.Collections.Generic.List[string]]::new()
    for ($index = 1; $index -le $UserCount; $index++) {
        $email = "k6-$runId-$index@example.test"
        $registerBody = @{
            email = $email
            password = $password
            fullName = "Local k6 Buyer $index"
        } | ConvertTo-Json -Compress

        try {
            $auth = Invoke-RestMethod -Method Post `
                -Uri "$base/api/v1/auth/register" `
                -ContentType "application/json" `
                -Body $registerBody
        }
        catch {
            $statusCode = 0
            if ($null -ne $_.Exception.Response) {
                $statusCode = [int]$_.Exception.Response.StatusCode
            }
            if ($statusCode -ne 429) {
                throw
            }

            Write-Host "Registration rate limit reached; waiting 60 seconds before retrying user $index..."
            Start-Sleep -Seconds 60
            $auth = Invoke-RestMethod -Method Post `
                -Uri "$base/api/v1/auth/register" `
                -ContentType "application/json" `
                -Body $registerBody
        }

        $cartBody = @{
            productId = $ProductId
            quantity = 1
        } | ConvertTo-Json -Compress

        Invoke-RestMethod -Method Post `
            -Uri "$base/api/v1/cart/items" `
            -Headers @{ Authorization = "Bearer $($auth.accessToken)" } `
            -ContentType "application/json" `
            -Body $cartBody | Out-Null

        $tokens.Add($auth.accessToken)
    }

    if ($tokens.Count -ne $UserCount) {
        throw "Expected $UserCount access tokens, received $($tokens.Count)."
    }

    [Environment]::SetEnvironmentVariable(
        "K6_BEARER_TOKENS",
        (ConvertTo-Json -Compress -InputObject $tokens),
        "Process"
    )
    [Environment]::SetEnvironmentVariable("PRODUCT_ID", "$ProductId", "Process")
    [Environment]::SetEnvironmentVariable("VUS", "$UserCount", "Process")
    [Environment]::SetEnvironmentVariable("BASE_URL", $base, "Process")
    [Environment]::SetEnvironmentVariable("MODE", $Mode, "Process")
    [Environment]::SetEnvironmentVariable("DURATION", $Duration, "Process")
    [Environment]::SetEnvironmentVariable(
        "ALLOW_INSUFFICIENT_STOCK",
        ([bool]$AllowInsufficientStock).ToString().ToLowerInvariant(),
        "Process"
    )

    $scriptPath = Join-Path $PSScriptRoot "checkout.js"
    Write-Host "Running $Mode checkout benchmark with $UserCount users..."
    & k6 run `
        "--summary-export=$summaryPath" `
        --out `
        "json=$rawPath" `
        $scriptPath
    if ($LASTEXITCODE -ne 0) {
        throw "k6 exited with code $LASTEXITCODE. See the summary and raw output files."
    }

    Write-Host "Summary: $summaryPath"
    Write-Host "Raw output: $rawPath (review/redact before sharing)"
}
finally {
    foreach ($name in $environmentNames) {
        [Environment]::SetEnvironmentVariable($name, $previousEnvironment[$name], "Process")
    }
    if ($null -ne $securePassword) {
        $securePassword.Dispose()
    }
    $password = $null
    $tokens = $null
}
