$ErrorActionPreference = "Stop"

$RepoRoot = Split-Path -Parent $PSScriptRoot
$FrontendDir = Join-Path $RepoRoot "frontend"
$NodeModulesDir = Join-Path $FrontendDir "node_modules"

if (-not (Get-Command npm -ErrorAction SilentlyContinue)) {
    throw "npm no esta disponible en PATH. Instala Node.js antes de ejecutar el frontend."
}

Push-Location $FrontendDir
try {
    if (-not (Test-Path $NodeModulesDir)) {
        Write-Host "Instalando dependencias frontend..."
        npm install
    }

    Write-Host "Levantando frontend Angular..."
    npm start
} finally {
    Pop-Location
}
