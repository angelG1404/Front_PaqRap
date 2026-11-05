#requires -Version 7.5
param([string]$Maven = 'mvn.cmd')
$ErrorActionPreference = 'Stop'
$raiz = Split-Path -Parent $PSScriptRoot
$variables = @('VITE_USE_MOCK', 'VITE_API_URL', 'VITE_WS_URL')
$anteriores = @{}
foreach ($nombre in $variables) { $anteriores[$nombre] = [Environment]::GetEnvironmentVariable($nombre, 'Process') }
try {
    # Las variables de proceso prevalecen sobre cualquier .env local.
    $env:VITE_USE_MOCK = 'false'
    $env:VITE_API_URL = ''
    $env:VITE_WS_URL = ''
    Push-Location (Join-Path $raiz 'front')
    try {
        & npm.cmd ci
        if ($LASTEXITCODE -ne 0) { throw 'Falló npm ci.' }
        & npm.cmd run build
        if ($LASTEXITCODE -ne 0) { throw 'Falló la compilación del frontend.' }
    } finally { Pop-Location }
    Push-Location (Join-Path $raiz 'back')
    try {
        & $Maven clean verify
        if ($LASTEXITCODE -ne 0) { throw 'Fallaron las pruebas o el empaquetado del backend.' }
    } finally { Pop-Location }
    $salida = Join-Path $raiz ('dist/release-' + (Get-Date -Format 'yyyyMMdd-HHmmss'))
    New-Item -ItemType Directory -Path $salida | Out-Null
    Copy-Item (Join-Path $raiz 'back/target/paqrap-back-1.0.0.jar') (Join-Path $salida 'paqrap.jar')
    Copy-Item (Join-Path $raiz 'front/dist') (Join-Path $salida 'frontend') -Recurse
    Copy-Item (Join-Path $raiz 'deploy') (Join-Path $salida 'deploy') -Recurse
    Copy-Item (Join-Path $raiz 'data') (Join-Path $salida 'data') -Recurse
    Copy-Item (Join-Path $raiz 'DESPLIEGUE.md') $salida
    Copy-Item (Join-Path $raiz 'GUIA_DE_PRUEBA.md') $salida
    $hash = Get-FileHash (Join-Path $salida 'paqrap.jar') -Algorithm SHA256
    ($hash.Hash.ToLower() + '  paqrap.jar') | Set-Content (Join-Path $salida 'SHA256SUMS.txt') -Encoding ascii
    Compress-Archive -Path "$salida/*" -DestinationPath "$salida.zip"
    Write-Host "Paquete listo: $salida.zip"
} finally {
    foreach ($nombre in $variables) { [Environment]::SetEnvironmentVariable($nombre, $anteriores[$nombre], 'Process') }
}
