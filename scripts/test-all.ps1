$ErrorActionPreference = "Stop"

$RepoRoot = Split-Path -Parent $PSScriptRoot
$FrontendDir = Join-Path $RepoRoot "frontend"
$NodeModulesDir = Join-Path $FrontendDir "node_modules"

Write-Host "== Backend: tests =="
& (Join-Path $PSScriptRoot "run-backend.ps1") test

Write-Host ""
Write-Host "== Frontend: dependencias =="
if (-not (Get-Command npm -ErrorAction SilentlyContinue)) {
    throw "npm no esta disponible en PATH. Instala Node.js antes de validar el frontend."
}

Push-Location $FrontendDir
try {
    if (-not (Test-Path $NodeModulesDir)) {
        npm install
    }

    Write-Host ""
    Write-Host "== Frontend: build =="
    npm run build

    Write-Host ""
    Write-Host "== Frontend: tests =="
    npm test -- --watch=false
} finally {
    Pop-Location
}

Write-Host ""
Write-Host "Validacion completa."
