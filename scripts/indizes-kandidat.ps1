#Requires -Version 5.1
<#
.SYNOPSIS
    Erhebt den Indexbestand beider Schemata und schreibt ihn in die
    Kandidatendatei neben der Sollliste. Schreibt NIEMALS die Sollliste.

.DESCRIPTION
    Ein Erzeuger, der die Sollliste ueberschreibt, macht IndexbestandDbIT zum
    Gummistempel: rot, Befehl laufen lassen, gruen -- und niemand hat
    hingesehen. Der Test lebt davon, dass das Nachziehen ein bewusster Akt ist.

    Dieses Skript erzeugt deshalb ausschliesslich

        backend/src/test/resources/indizes-sollliste.kandidat

    und zeigt am Ende den Unterschied zur Sollliste an. Uebernommen wird von
    Hand, Zeile fuer Zeile, im Branch -- mit der Frage, WER sich geaendert hat:
    die Datenbank oder die Liste.

    Welche Tabellen des Quellschemas erhoben werden, entscheidet dieses Skript
    nicht. Es liest die tabelle;glassfish;-Zeilen der Sollliste und erhebt genau
    die. Kommt eine weitere Quelltabelle in Anwendungscode, gehoert sie nach
    Regel L8 ohnehin vorher erhoben und von Hand eingetragen. Fuer das eigene
    Schema gilt das Gegenteil: Dort werden ALLE Basistabellen erhoben.

    Zugangsdaten kommen ausschliesslich aus den OVERLORD_DB_*-Variablen und
    werden dem Client ueber MYSQL_PWD gereicht (Regel G1). Ausgefuehrt wird nur
    SELECT auf information_schema (Regel S1).

.PARAMETER MysqlPfad
    Pfad zu mysql.exe, falls sie nicht im PATH liegt. Standard ist die Kopie aus
    MySQL Workbench -- dieselbe, mit der die Messrunden gefahren sind.

.EXAMPLE
    powershell.exe -NoProfile -File scripts\indizes-kandidat.ps1
#>
[CmdletBinding()]
param(
    [string] $MysqlPfad
)

$ErrorActionPreference = 'Stop'

$RepoWurzel = Split-Path -Parent $PSScriptRoot
$Sollliste  = Join-Path $RepoWurzel 'backend\src\test\resources\indizes-sollliste.txt'
$Kandidat   = Join-Path $RepoWurzel 'backend\src\test\resources\indizes-sollliste.kandidat'
$Statement  = Join-Path $PSScriptRoot 'indizes-kandidat.sql'

function Abbruch($text) { Write-Host "   [FEHL] $text" -ForegroundColor Red; Write-Host ''; exit 1 }
function Ok($text)      { Write-Host "   [ok]   $text" -ForegroundColor Green }
function Hinweis($text) { Write-Host "   [info] $text" -ForegroundColor Gray }

Write-Host ''
Write-Host 'Index-Sollliste -- Kandidatendatei erzeugen (E39)' -ForegroundColor White

# --- 1. Dateien -------------------------------------------------------------

if (-not (Test-Path $Sollliste)) { Abbruch "Sollliste nicht gefunden: $Sollliste" }
if (-not (Test-Path $Statement)) { Abbruch "Statement nicht gefunden: $Statement" }

# --- 2. Umgebung ------------------------------------------------------------

$noetig = @('OVERLORD_DB_HOST','OVERLORD_DB_PORT','OVERLORD_DB_GLASSFISH_SCHEMA',
            'OVERLORD_DB_MONITOR_SCHEMA','OVERLORD_DB_READ_USER','OVERLORD_DB_READ_PASSWORD')
$fehlend = $noetig | Where-Object { -not [Environment]::GetEnvironmentVariable($_) }
if ($fehlend) { Abbruch "Nicht gesetzt: $($fehlend -join ', '). Siehe backend\.env.example." }

if (-not $MysqlPfad) {
    $imPfad = Get-Command mysql.exe -ErrorAction SilentlyContinue
    if ($imPfad) {
        $MysqlPfad = $imPfad.Source
    } else {
        $MysqlPfad = 'C:\Program Files\MySQL\MySQL Workbench 8.0 CE\mysql.exe'
    }
}
if (-not (Test-Path $MysqlPfad)) { Abbruch "mysql.exe nicht gefunden: $MysqlPfad. Alternativ: -MysqlPfad <pfad>" }
Hinweis "Client: $MysqlPfad"

# --- 3. Die bewachten Quelltabellen aus der Sollliste ------------------------

$sollSaetze = Get-Content $Sollliste -Encoding UTF8 | Where-Object { $_ -match '^(tabelle|index);' }
$bewacht = ($sollSaetze | Where-Object { $_ -like 'tabelle;glassfish;*' } |
            ForEach-Object { $_.Split(';')[2] }) -join ','
if (-not $bewacht) { Abbruch 'Die Sollliste fuehrt keine tabelle;glassfish;-Zeile.' }
Hinweis "Bewachte Quelltabellen: $(($bewacht -split ',').Count)"

# --- 4. Erhebung ------------------------------------------------------------

$init = "SET @glassfish='$($env:OVERLORD_DB_GLASSFISH_SCHEMA)'," +
        " @monitor='$($env:OVERLORD_DB_MONITOR_SCHEMA)'," +
        " @bewacht='$bewacht'"

$env:MYSQL_PWD = $env:OVERLORD_DB_READ_PASSWORD
try {
    $zeilen = & $MysqlPfad --host=$env:OVERLORD_DB_HOST --port=$env:OVERLORD_DB_PORT `
                           --user=$env:OVERLORD_DB_READ_USER `
                           --batch --skip-column-names --raw `
                           --ssl-mode=DISABLED --default-character-set=utf8mb4 `
                           --init-command=$init `
                           -e "source $($Statement.Replace([char]92, '/'))"
} finally {
    Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue
}
if ($LASTEXITCODE -ne 0) { Abbruch "mysql.exe endete mit $LASTEXITCODE." }
if (-not $zeilen)        { Abbruch 'Die Erhebung hat keine Zeile geliefert.' }
Ok "$($zeilen.Count) Saetze erhoben"

# --- 5. Kandidatendatei schreiben -- und NUR sie ----------------------------

# LF und UTF-8 ohne BOM: Die Datei wird gegen die Sollliste diffed, und ein BOM
# oder ein CR machte jede Zeile zur Abweichung.
$utf8OhneBom = New-Object System.Text.UTF8Encoding($false)
[System.IO.File]::WriteAllText($Kandidat, ($zeilen -join "`n") + "`n", $utf8OhneBom)
Ok "geschrieben: $Kandidat"

# --- 6. Der Unterschied -----------------------------------------------------

Write-Host ''
$unterschied = Compare-Object -ReferenceObject $sollSaetze -DifferenceObject $zeilen -SyncWindow 500
if (-not $unterschied) {
    Write-Host '   Kein Unterschied. Die Sollliste bildet den Bestand ab.' -ForegroundColor Green
} else {
    Write-Host '   UNTERSCHIED -- von Hand pruefen und uebernehmen:' -ForegroundColor Yellow
    Write-Host '     "<=" steht in der Sollliste und nicht in der Datenbank' -ForegroundColor DarkGray
    Write-Host '     "=>" steht in der Datenbank und nicht in der Sollliste' -ForegroundColor DarkGray
    Write-Host ''
    foreach ($u in $unterschied) { Write-Host "     $($u.SideIndicator) $($u.InputObject)" }
    Write-Host ''
    Write-Host '   Zuerst klaeren, WER sich geaendert hat -- die Datenbank oder die Liste.' -ForegroundColor Yellow
    Write-Host '   Es wird kein Index angelegt, geaendert oder geloescht, um einen Lauf' -ForegroundColor Yellow
    Write-Host '   gruen zu bekommen. Auf GlassfishDB ohnehin nicht (Regel S1).' -ForegroundColor Yellow
}
Write-Host ''
