param(
    [string[]]$GradleTask = @("bootRun")
)

$ErrorActionPreference = "Stop"

$GradleVersion = "8.14.3"
$RepoRoot = Split-Path -Parent $PSScriptRoot
$BackendDir = Join-Path $RepoRoot "backend"
$ToolsDir = Join-Path $RepoRoot ".tools"
$JdkHome = Join-Path $ToolsDir "jdk-17"
$JdkZip = Join-Path $ToolsDir "temurin-jdk-17.zip"
$JdkUrl = "https://api.adoptium.net/v3/binary/latest/17/ga/windows/x64/jdk/hotspot/normal/eclipse"
$GradleHome = Join-Path $ToolsDir "gradle-$GradleVersion"
$GradleZip = Join-Path $ToolsDir "gradle-$GradleVersion-bin.zip"
$GradleUrl = "https://services.gradle.org/distributions/gradle-$GradleVersion-bin.zip"

function Download-File {
    param(
        [string]$Url,
        [string]$OutFile
    )

    if (Test-Path $OutFile) {
        $ExistingFile = Get-Item $OutFile
        if ($ExistingFile.Length -eq 0) {
            Remove-Item -Force $OutFile
        }
    }

    if (Test-Path $OutFile) {
        return
    }

    if (Get-Command curl.exe -ErrorAction SilentlyContinue) {
        curl.exe -L --fail --retry 5 --retry-delay 2 --output $OutFile $Url
    } else {
        Invoke-WebRequest -Uri $Url -OutFile $OutFile
    }

    if (-not (Test-Path $OutFile) -or (Get-Item $OutFile).Length -eq 0) {
        throw "No se pudo descargar $Url"
    }
}

if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
    New-Item -ItemType Directory -Force -Path $ToolsDir | Out-Null

    if (-not (Test-Path (Join-Path $JdkHome "bin\java.exe"))) {
        Write-Host "Java 17 no esta en PATH. Descargando JDK 17 portable..."
        Download-File -Url $JdkUrl -OutFile $JdkZip

        Write-Host "Descomprimiendo JDK 17 portable..."
        $TempJdkDir = Join-Path $ToolsDir "jdk-17-temp"
        if (Test-Path $TempJdkDir) {
            Remove-Item -Recurse -Force $TempJdkDir
        }

        Expand-Archive -Path $JdkZip -DestinationPath $TempJdkDir -Force -ErrorAction Stop
        $ExtractedJava = Get-ChildItem -Path $TempJdkDir -Filter "java.exe" -Recurse |
                Where-Object { $_.FullName -like "*\bin\java.exe" } |
                Select-Object -First 1

        if (-not $ExtractedJava) {
            throw "No se encontro java.exe dentro del JDK descargado."
        }

        $ExtractedHome = Split-Path -Parent (Split-Path -Parent $ExtractedJava.FullName)
        if (Test-Path $JdkHome) {
            Remove-Item -Recurse -Force $JdkHome
        }

        Move-Item -Path $ExtractedHome -Destination $JdkHome
        Remove-Item -Recurse -Force $TempJdkDir
    }

    $env:JAVA_HOME = $JdkHome
    $env:PATH = "$(Join-Path $JdkHome "bin");$env:PATH"
}

if (-not (Test-Path $GradleHome)) {
    New-Item -ItemType Directory -Force -Path $ToolsDir | Out-Null

    Write-Host "Descargando Gradle $GradleVersion..."
    Download-File -Url $GradleUrl -OutFile $GradleZip

    Write-Host "Descomprimiendo Gradle $GradleVersion..."
    Expand-Archive -Path $GradleZip -DestinationPath $ToolsDir -Force -ErrorAction Stop
}

$env:PATH = "$(Join-Path $GradleHome "bin");$env:PATH"

Write-Host "Ejecutando Gradle: $($GradleTask -join ' ')"
Write-Host "CTI_WEBSOCKET_URL=$env:CTI_WEBSOCKET_URL"

Push-Location $BackendDir
try {
    gradle @GradleTask
} finally {
    Pop-Location
}
