param(
  [switch]$IncludeE2E
)

$ErrorActionPreference = 'Stop'
$composeFile = Join-Path $PSScriptRoot '..\compose.frontend-test.yml'

docker compose -f $composeFile config --quiet
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

docker compose -f $composeFile build frontend-check
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

docker compose -f $composeFile run --rm frontend-check
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

if ($IncludeE2E) {
  docker compose -f $composeFile build frontend frontend-e2e
  if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

  docker compose -f $composeFile up -d frontend
  if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

  try {
    docker compose -f $composeFile run --rm frontend-e2e
    if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
  } finally {
    docker compose -f $composeFile stop frontend
  }
}
