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

# Keep the documented error-code table in lockstep with the backend enum.
$errorEnumPath = Join-Path $repo 'backend\src\main\java\com\edtech\platform\common\exception\ErrorCode.java'
$errorDocsPath = Join-Path $repo 'docs\architecture\ERROR_CODES.md'
if ((Test-Path $errorEnumPath -PathType Leaf) -and (Test-Path $errorDocsPath -PathType Leaf)) {
    $enumText = Get-Content -Raw -LiteralPath $errorEnumPath
    $docText = Get-Content -Raw -LiteralPath $errorDocsPath
    $statusMap = @{
        BAD_REQUEST=400; UNAUTHORIZED=401; FORBIDDEN=403; NOT_FOUND=404; CONFLICT=409
        UNSUPPORTED_MEDIA_TYPE=415; PAYLOAD_TOO_LARGE=413
        UNPROCESSABLE_ENTITY=422; TOO_MANY_REQUESTS=429; INTERNAL_SERVER_ERROR=500
        BAD_GATEWAY=502; SERVICE_UNAVAILABLE=503; NO_CONTENT=204
    }
    $enumEntries = @{}
    foreach ($m in [regex]::Matches($enumText, '(?m)^\s*([A-Z][A-Z0-9_]+)\(HttpStatus\.([A-Z_]+),')) {
        $enumEntries[$m.Groups[1].Value] = $statusMap[$m.Groups[2].Value]
    }
    $docEntries = @{}
    foreach ($m in [regex]::Matches($docText, '(?m)^\|\s*`([A-Z][A-Z0-9_]+)`\s*\|\s*(\d{3})\s*\|')) {
        $docEntries[$m.Groups[1].Value] = [int]$m.Groups[2].Value
    }
    foreach ($code in $enumEntries.Keys) {
        if (-not $docEntries.ContainsKey($code)) { $failures.Add("Error code only in enum: $code") }
        elseif ($enumEntries[$code] -ne $docEntries[$code]) { $failures.Add("Error code status mismatch: $code enum=$($enumEntries[$code]) docs=$($docEntries[$code])") }
    }
    foreach ($code in $docEntries.Keys) {
        if (-not $enumEntries.ContainsKey($code)) { $failures.Add("Error code only in docs: $code") }
    }
}

if ($failures.Count -gt 0) {
    $failures | ForEach-Object { Write-Error $_ }
    exit 1
}

Write-Output "Documentation checks passed: $($currentDocs.Count) current Markdown files scanned."
