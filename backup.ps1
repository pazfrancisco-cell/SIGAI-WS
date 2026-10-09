# Backup de SIGAI-WS: base de datos, .env (clave de cifrado) y archivos subidos (storage/).
# Uso (desde PowerShell, en esta carpeta):  .\backup.ps1
#        o con otro destino / cantidad:     .\backup.ps1 -Destino "E:\Backups" -Conservar 20
# Cada corrida crea <Destino>\<fecha_hora>\ y borra las mas viejas, dejando las ultimas $Conservar.
# Ojo: el .env tiene secretos (clave AES, password SMTP): no subir estas copias a ningun lugar publico.
param(
    [string]$Destino = "C:\backups\SIGAI-WS",
    [int]$Conservar = 10,
    [string]$Servidor = "localhost,14330",
    [string]$Base = "SIGAI_WS"
)
$ErrorActionPreference = "Stop"

$sqlcmd = "C:\Program Files\Microsoft SQL Server\Client SDK\ODBC\170\Tools\Binn\SQLCMD.EXE"
if (-not (Test-Path $sqlcmd)) { $sqlcmd = "sqlcmd" }

$fecha = Get-Date -Format "yyyy-MM-dd_HHmm"
$carpeta = Join-Path $Destino $fecha
New-Item -ItemType Directory -Force -Path $carpeta | Out-Null

# 1) Base de datos. SQL Server graba el .bak con su propia cuenta de servicio, asi que se escribe
#    en su carpeta de backups (donde seguro tiene permiso) y despues se copia al destino.
$dirSql = (& $sqlcmd -S $Servidor -E -h -1 -W -Q "SET NOCOUNT ON; DECLARE @d NVARCHAR(4000); EXEC master.dbo.xp_instance_regread N'HKEY_LOCAL_MACHINE', N'Software\Microsoft\MSSQLServer\MSSQLServer', N'BackupDirectory', @d OUTPUT; SELECT @d" | Select-Object -First 1).Trim()
$bakSql = Join-Path $dirSql "$($Base)_$fecha.bak"
& $sqlcmd -S $Servidor -E -b -Q "BACKUP DATABASE [$Base] TO DISK = N'$bakSql' WITH INIT, CHECKSUM, NAME = N'$Base $fecha'"
if ($LASTEXITCODE -ne 0) { throw "Fallo el BACKUP DATABASE." }
& $sqlcmd -S $Servidor -E -b -Q "RESTORE VERIFYONLY FROM DISK = N'$bakSql' WITH CHECKSUM"
if ($LASTEXITCODE -ne 0) { throw "El backup se grabo pero no paso la verificacion." }
Copy-Item -LiteralPath $bakSql -Destination $carpeta
Remove-Item -LiteralPath $bakSql -ErrorAction SilentlyContinue

# 2) .env (sin la clave AES la base restaurada no se puede leer: DNI y datos sensibles estan cifrados)
if (Test-Path "$PSScriptRoot\.env") { Copy-Item -LiteralPath "$PSScriptRoot\.env" -Destination $carpeta }

# 3) Archivos subidos (fotos, documentos de legajo, adjuntos)
if ((Test-Path "$PSScriptRoot\storage") -and (Get-ChildItem -LiteralPath "$PSScriptRoot\storage" -Recurse -File | Select-Object -First 1)) {
    Compress-Archive -Path "$PSScriptRoot\storage\*" -DestinationPath (Join-Path $carpeta "storage.zip") -Force
}

# Rotacion: deja solo las ultimas $Conservar copias
Get-ChildItem -LiteralPath $Destino -Directory | Sort-Object Name -Descending |
    Select-Object -Skip $Conservar | Remove-Item -Recurse -Force

Write-Host "Backup listo en $carpeta"
Get-ChildItem -LiteralPath $carpeta | Format-Table Name, @{ n = "KB"; e = { [math]::Round($_.Length / 1KB) } } -AutoSize
