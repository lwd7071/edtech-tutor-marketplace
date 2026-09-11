param([switch]$SkipE2E)

$ErrorActionPreference = 'Stop'
$root = Split-Path $PSScriptRoot -Parent
$logRoot = Join-Path $root 'test-logs\student-journey'
New-Item -ItemType Directory -Force -Path $logRoot | Out-Null

function Assert-LastExit([string]$step) {
  if ($LASTEXITCODE -ne 0) { throw "$step failed with exit code $LASTEXITCODE" }
}

Push-Location $root
try {
  docker compose config --quiet 2>&1 | Tee-Object (Join-Path $logRoot '01-compose.log')
  Assert-LastExit 'docker compose config'

  $env:SPRING_PROFILES_ACTIVE = 'test'
  $env:PAYMENT_PROVIDER = 'fake'
  $env:MAIL_TRANSPORT = 'logging'
  $env:TEST_STUDENT_EMAIL = 'student-journey@example.test'
  Push-Location (Join-Path $root 'backend')
  try {
    mvn clean test -B --no-transfer-progress '-Dfile.encoding=UTF-8' 2>&1 | Tee-Object (Join-Path $logRoot '02-backend.log')
    Assert-LastExit 'backend tests'
  } finally { Pop-Location }

  Push-Location (Join-Path $root 'frontend')
  try {
    npm run typecheck 2>&1 | Tee-Object (Join-Path $logRoot '03-typecheck.log'); Assert-LastExit 'frontend typecheck'
    npm run lint 2>&1 | Tee-Object (Join-Path $logRoot '04-lint.log'); Assert-LastExit 'frontend lint'
    npm run test:ci 2>&1 | Tee-Object (Join-Path $logRoot '05-jest.log'); Assert-LastExit 'frontend tests'
    npm run build 2>&1 | Tee-Object (Join-Path $logRoot '06-build.log'); Assert-LastExit 'frontend build'
    if (-not $SkipE2E) {
      npm run test:e2e 2>&1 | Tee-Object (Join-Path $logRoot '07-playwright.log'); Assert-LastExit 'Playwright tests'
    }
  } finally { Pop-Location }
} finally {
  Pop-Location
  Remove-Item Env:SPRING_PROFILES_ACTIVE,Env:PAYMENT_PROVIDER,Env:MAIL_TRANSPORT,Env:TEST_STUDENT_EMAIL -ErrorAction SilentlyContinue
}

Write-Host "Student journey checks passed. Logs: $logRoot"
