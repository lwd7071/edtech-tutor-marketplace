$ErrorActionPreference = 'Stop'

$projectRoot = Split-Path -Parent $PSScriptRoot
$backupDirectory = Join-Path $projectRoot 'backups'
New-Item -ItemType Directory -Force -Path $backupDirectory | Out-Null
$backupPath = Join-Path $backupDirectory "edtech-$(Get-Date -Format 'yyyyMMdd-HHmmss').dump"

$startInfo = [System.Diagnostics.ProcessStartInfo]::new()
$startInfo.FileName = 'docker.exe'
$startInfo.WorkingDirectory = $projectRoot
$startInfo.UseShellExecute = $false
$startInfo.RedirectStandardOutput = $true
$startInfo.RedirectStandardError = $true
foreach ($argument in @('compose', 'exec', '-T', 'postgres', 'pg_dump', '-U', 'postgres', '-d', 'edtech_db', '-Fc')) {
    [void]$startInfo.ArgumentList.Add($argument)
}

$process = [System.Diagnostics.Process]::new()
$process.StartInfo = $startInfo
if (-not $process.Start()) { throw 'Could not start docker compose pg_dump.' }
$stderrTask = $process.StandardError.ReadToEndAsync()
$output = [System.IO.File]::Open($backupPath, [System.IO.FileMode]::CreateNew, [System.IO.FileAccess]::Write, [System.IO.FileShare]::None)
try {
    $process.StandardOutput.BaseStream.CopyTo($output)
} finally {
    $output.Dispose()
}
$process.WaitForExit()
$null = $stderrTask.GetAwaiter().GetResult()
if ($process.ExitCode -ne 0) {
    Remove-Item -LiteralPath $backupPath -Force -ErrorAction SilentlyContinue
    throw "pg_dump failed with exit code $($process.ExitCode). Check that the PostgreSQL service is healthy."
}

$file = Get-Item -LiteralPath $backupPath
Write-Output "Backup created: $($file.FullName) ($($file.Length) bytes)"
