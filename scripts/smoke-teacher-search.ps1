[CmdletBinding()]
param(
    [string] $BaseUrl = "http://localhost:18080"
)

$ErrorActionPreference = "Stop"
$base = $BaseUrl.TrimEnd('/')

function Get-Json([string] $Path) {
    $uri = "$base$Path"
    try {
        $response = Invoke-RestMethod -Uri $uri -Method Get -TimeoutSec 15
    } catch {
        throw "GET $uri failed: $($_.Exception.Message)"
    }

    if ($null -eq $response -or $response.success -ne $true) {
        throw "GET $uri returned an unsuccessful API response"
    }
    return $response
}

function Get-Health() {
    $uri = "$base/actuator/health"
    try {
        return Invoke-RestMethod -Uri $uri -Method Get -TimeoutSec 15
    } catch {
        throw "GET $uri failed: $($_.Exception.Message)"
    }
}

function Get-TeacherIds($Response) {
    if ($null -eq $Response.data -or $null -eq $Response.meta) {
        throw "Teacher search response is missing data or meta"
    }
    return @($Response.data | ForEach-Object { [string]$_.id } | Sort-Object)
}

$health = Get-Health
if ($health.status -ne "UP") {
    throw "Actuator health is not UP"
}

$toan = Get-Json "/api/public/teachers?keyword=Toan&page=0&size=20"
$accented = Get-Json "/api/public/teachers?keyword=$([uri]::EscapeDataString('  TOÁN  '))&page=0&size=20"
$upper = Get-Json "/api/public/teachers?keyword=TOAN&page=0&size=20"

$expectedIds = Get-TeacherIds $toan
foreach ($variant in @($accented, $upper)) {
    $variantIds = Get-TeacherIds $variant
    if ((@($expectedIds) -join ',') -ne (@($variantIds) -join ',')) {
        throw "Accent/case/whitespace normalization returned different teacher IDs"
    }
}

$rating = Get-Json "/api/public/teachers?sort=rating_desc&page=0&size=100"
$ratings = @($rating.data | ForEach-Object { [double]$_.averageRating })
$positiveSeen = $false
foreach ($value in $ratings) {
    if ($value -gt 0) {
        $positiveSeen = $true
    } elseif ($positiveSeen) {
        throw "rating_desc placed an unrated teacher before a rated teacher"
    }
}

Write-Host "Teacher search smoke passed for $base"
Write-Host "Toan result count: $($toan.data.Count)"
Write-Host "rating_desc result count: $($rating.data.Count)"
