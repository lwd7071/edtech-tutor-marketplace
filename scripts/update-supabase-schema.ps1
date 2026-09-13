param(
    [Parameter(Mandatory = $true)]
    [int]$ExpectedCurrentVersion,

    [Parameter(Mandatory = $true)]
    [int]$TargetVersion,

    [switch]$Apply
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$repoRoot = (Resolve-Path (Join-Path $PSScriptRoot "..")).Path
$backendRoot = Join-Path $repoRoot "backend"
$envPath = Join-Path $repoRoot ".env.cloud"
$pomPath = Join-Path $backendRoot "pom.xml"

function Stop-WithMessage([string]$message) {
    throw "Supabase migration blocked: $message"
}

if (-not (Test-Path -LiteralPath $pomPath -PathType Leaf)) {
    Stop-WithMessage "backend/pom.xml was not found."
}
if (-not (Test-Path -LiteralPath $envPath -PathType Leaf)) {
    Stop-WithMessage ".env.cloud was not found."
}
if ($TargetVersion -le $ExpectedCurrentVersion) {
    Stop-WithMessage "TargetVersion must be greater than ExpectedCurrentVersion."
}

$migrationFiles = @(Get-ChildItem -LiteralPath (Join-Path $backendRoot "src/main/resources/db/migration") -Filter ("V{0}__*.sql" -f $TargetVersion) -File)
if ($migrationFiles.Count -ne 1) {
    Stop-WithMessage ("Expected exactly one migration for V{0}, found {1}." -f $TargetVersion, $migrationFiles.Count)
}

$unsafePattern = '(?im)\b(DROP\s+TABLE|DROP\s+COLUMN|TRUNCATE|DELETE\s+FROM|ALTER\s+COLUMN\s+\S+\s+TYPE|RENAME\s+(TO|COLUMN))\b'
$migrationText = Get-Content -LiteralPath $migrationFiles[0].FullName -Raw
if ($migrationText -match $unsafePattern) {
    Stop-WithMessage "Target migration contains a destructive or non-backward-compatible operation."
}

$requiredKeys = @('CLOUD_DB_URL', 'CLOUD_DB_USERNAME', 'CLOUD_DB_PASSWORD')
$values = @{}
foreach ($line in Get-Content -LiteralPath $envPath) {
    $trimmed = $line.Trim()
    if ($trimmed -eq '' -or $trimmed.StartsWith('#')) { continue }
    if ($trimmed -notmatch '^([A-Za-z_][A-Za-z0-9_]*)=(.*)$') { continue }
    $key = $Matches[1]
    if ($requiredKeys -notcontains $key) { continue }
    $value = $Matches[2].Trim()
    if ($value.Length -ge 2 -and (($value.StartsWith('"') -and $value.EndsWith('"')) -or ($value.StartsWith("'") -and $value.EndsWith("'")))) {
        $value = $value.Substring(1, $value.Length - 2)
    }
    $values[$key] = $value
}

foreach ($key in $requiredKeys) {
    if (-not $values.ContainsKey($key) -or [string]::IsNullOrWhiteSpace($values[$key])) {
        Stop-WithMessage ("Required .env.cloud key {0} is missing or empty." -f $key)
    }
    [Environment]::SetEnvironmentVariable($key, $values[$key], 'Process')
}

function Invoke-Flyway([string]$goal, [string[]]$extraArgs = @()) {
    $args = @('-B', '--no-transfer-progress', ('org.flywaydb:flyway-maven-plugin:9.22.3:{0}' -f $goal)) + $extraArgs
    Push-Location $backendRoot
    try {
        $output = @(& mvn @args 2>&1)
        $output | ForEach-Object { Write-Host $_ }
        if ($LASTEXITCODE -ne 0) {
            Stop-WithMessage ("Flyway {0} failed with exit code {1}." -f $goal, $LASTEXITCODE)
        }
        return $output
    } finally {
        Pop-Location
    }
}

function Get-CurrentFlywayVersion([object[]]$infoOutput) {
    $successVersions = @()
    foreach ($line in $infoOutput) {
        $text = [string]$line
        if ($text -match 'Schema version:\s*(?<schemaVersion>\d+)') {
            return [int]$Matches['schemaVersion']
        }
        if ($text -match '^\|\s*Versioned\s*\|\s*(?<version>\d+)\s*\|.*\|\s*Success\s*\|\s*$') {
            $successVersions += [int]$Matches['version']
        }
    }
    if ($successVersions.Count -eq 0) { return 0 }
    return ($successVersions | Measure-Object -Maximum).Maximum
}

$changedMigrations = @(
    & git -C $repoRoot diff --name-only -- backend/src/main/resources/db/migration
    & git -C $repoRoot diff --cached --name-only -- backend/src/main/resources/db/migration
)
foreach ($changedPath in $changedMigrations) {
    if ($changedPath -match 'V(?<version>\d+)__' -and [int]$Matches['version'] -lt $ExpectedCurrentVersion) {
        Stop-WithMessage ("An already-applied migration was modified: {0}." -f $changedPath)
    }
}

Write-Output ("Running read-only Flyway info/validate for target V{0}." -f $TargetVersion)
$infoBefore = @(Invoke-Flyway 'info')
$currentVersion = Get-CurrentFlywayVersion $infoBefore
if ($currentVersion -eq $TargetVersion) {
    Write-Output ("Supabase is already at V{0}; no migration needed." -f $TargetVersion)
    Invoke-Flyway 'validate' | Out-Null
    if ($Apply) { Write-Output 'Apply requested but database is already at target; no-op.' }
    exit 0
}
if ($currentVersion -ne $ExpectedCurrentVersion) {
    Stop-WithMessage ("Expected current V{0}, but Flyway info reported V{1}." -f $ExpectedCurrentVersion, $currentVersion)
}
Invoke-Flyway 'validate' @('-Dflyway.ignoreMigrationPatterns=*:pending') | Out-Null

if (-not $Apply) {
    Write-Output 'Read-only preflight complete; no migration applied.'
    exit 0
}

Write-Output ("Applying V{0}; Flyway will not clean the database." -f $TargetVersion)
Invoke-Flyway 'migrate' @('-Dflyway.target=' + $TargetVersion) | Out-Null
Invoke-Flyway 'validate' | Out-Null
Invoke-Flyway 'info' | Out-Null
Write-Output ("Supabase migration target V{0} completed and post-migrate validation passed." -f $TargetVersion)
