# Levanta SIGAI-WS localmente. Uso: .\run.ps1  (desde PowerShell, en esta carpeta)
$ErrorActionPreference = "Stop"

$env:JAVA_HOME = "C:\Program Files\Eclipse Adoptium\jdk-21.0.12.8-hotspot"
$env:Path = "$env:JAVA_HOME\bin;C:\tools\apache-maven-3.9.9\bin;" + $env:Path

if (-not (Test-Path "$PSScriptRoot\.env")) {
    Write-Error "No existe .env. Copia .env.example como .env y completalo primero."
    exit 1
}

Get-Content "$PSScriptRoot\.env" | ForEach-Object {
    if ($_ -match '^\s*#' -or $_ -match '^\s*$') { return }
    $parts = $_ -split '=', 2
    if ($parts.Length -eq 2) {
        [Environment]::SetEnvironmentVariable($parts[0].Trim(), $parts[1].Trim(), "Process")
    }
}

Set-Location $PSScriptRoot
& mvn spring-boot:run
