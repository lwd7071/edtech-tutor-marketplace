[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$repo = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$required = @(
    'AGENTS.md',
    'CONTEXT.md',
    'docs\README.md',
    'docs\STATUS.md',
    'docs\CHANGELOG.md',
    'docs\adr\README.md'
)
$failures = [System.Collections.Generic.List[string]]::new()

foreach ($path in $required) {
    if (-not (Test-Path (Join-Path $repo $path) -PathType Leaf)) {
        $failures.Add("Missing required document: $path")
    }
}

$currentDocs = Get-ChildItem (Join-Path $repo 'docs') -Recurse -File -Filter '*.md' |
    Where-Object { $_.FullName -notmatch '[\\/]archive[\\/]' }

foreach ($file in $currentDocs) {
    $text = Get-Content -Raw -LiteralPath $file.FullName
    if ($text -match '(?i)docs[\\/]A|docs[\\/]B|file:///') {
        $failures.Add("Legacy or machine-local reference in $($file.FullName.Replace($repo + '\', ''))")
    }
    if ($text -match '(?i)-----BEGIN (RSA |EC |OPENSSH )?PRIVATE KEY-----') {
        $failures.Add("Private key material detected in $($file.FullName.Replace($repo + '\', ''))")
    }
    if ($text -match '(?im)(GOOGLE_CLIENT_SECRET|PAYOS_CLIENT_SECRET|MAIL_PASSWORD|JWT_SECRET|CLOUDINARY_API_SECRET)\s*[:=]\s*(?!<|\$\{|your|placeholder|change-me|redacted)[^\s`"]+') {
        $failures.Add("Possible secret value detected in $($file.FullName.Replace($repo + '\', ''))")
    }

    $matches = [regex]::Matches($text, '\[[^\]]+\]\(([^)]+)\)')
    foreach ($match in $matches) {
        $target = $match.Groups[1].Value.Trim()
        if ([string]::IsNullOrWhiteSpace($target) -or $target.StartsWith('#') -or $target -match '^(?i)(https?|mailto):') { continue }
        $target = ($target -split '#')[0].Trim()
        if ([string]::IsNullOrWhiteSpace($target)) { continue }
        $resolved = [System.IO.Path]::GetFullPath((Join-Path $file.DirectoryName $target))
        if (-not (Test-Path $resolved -PathType Leaf)) {
            $failures.Add("Broken link in $($file.FullName.Replace($repo + '\', '')): $target")
        }
    }
}

if ($failures.Count -gt 0) {
    $failures | ForEach-Object { Write-Error $_ }
    exit 1
}

Write-Output "Documentation checks passed: $($currentDocs.Count) current Markdown files scanned."
