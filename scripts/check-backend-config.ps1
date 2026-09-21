param(
  [ValidateSet('local', 'cloud')]
  [string]$Profile = 'local',
  [string]$EnvironmentFile
)

$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent

if ($Profile -eq 'local') {
  Write-Host 'Backend local profile is self-contained; PostgreSQL and Redis must be reachable.'
  exit 0
}

if ([string]::IsNullOrWhiteSpace($EnvironmentFile)) {
  $EnvironmentFile = Join-Path $root '.env.cloud'
}

if (-not (Test-Path -LiteralPath $EnvironmentFile -PathType Leaf)) {
  throw "Cloud environment file was not found: $EnvironmentFile"
}

$required = @(
  'CLOUD_DB_URL',
  'CLOUD_DB_USERNAME',
  'CLOUD_DB_PASSWORD',
  'REDIS_URL',
  'APP_JWT_SECRET',
  'GOOGLE_CLIENT_ID',
  'GOOGLE_CLIENT_SECRET',
  'CLOUDINARY_CLOUD_NAME',
  'CLOUDINARY_API_KEY',
  'CLOUDINARY_API_SECRET',
  'EDTECH_ACCOUNT_ENCRYPTION_KEY'
)

$configured = @{}
foreach ($line in Get-Content -LiteralPath $EnvironmentFile) {
  if ($line -match '^\s*([A-Za-z_][A-Za-z0-9_]*)\s*=\s*(.*)$') {
    $configured[$matches[1]] = $matches[2].Trim().Trim('"').Trim("'")
  }
}

$missing = @($required | Where-Object { -not $configured.ContainsKey($_) -or [string]::IsNullOrWhiteSpace($configured[$_]) })

if ($configured['APP_EMAIL_PROVIDER'] -eq 'brevo') {
  $brevoRequired = @('BREVO_API_KEY', 'BREVO_SENDER_EMAIL', 'BREVO_SENDER_NAME')
  $missing += @($brevoRequired | Where-Object {
    -not $configured.ContainsKey($_) -or
    [string]::IsNullOrWhiteSpace($configured[$_]) -or
    $configured[$_] -match '^replace-with-'
  })
}

$missing = @($missing | Select-Object -Unique)
if ($missing.Count -gt 0) {
  throw "Cloud configuration is incomplete. Missing or blank keys: $($missing -join ', ')"
}

if ($configured['EDTECH_ACCOUNT_ENCRYPTION_KEY'].Length -ne 32) {
  throw 'Cloud configuration is invalid: EDTECH_ACCOUNT_ENCRYPTION_KEY must contain exactly 32 characters.'
}

if ($configured['APP_JWT_SECRET'].Length -lt 32) {
  throw 'Cloud configuration is invalid: APP_JWT_SECRET must contain at least 32 characters.'
}

Write-Host 'Cloud configuration contains all required keys. Values were not printed.'
