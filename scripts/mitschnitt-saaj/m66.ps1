<#
    M66 - Decken sich die beiden Kopien?

    DIESES SKRIPT IST DER AUFRUF. Abgelegt und von hier ausgefuehrt, nicht
    direkt abgesetzt.

    Fuenf Zeitscheiben wie im Auftrag, 25 Nachrichten je Scheibe (Ergaenzung 5
    vom 17.08.2026, sonst 50). Je Nachricht Message.Payload.GUID und alle
    *.Log.GUID. Sequenziell, je Verweis bei der Ablage, die er nennt, kein
    Rueckfall auf die andere.

    Was hier NICHT drinsteht (Regel G1): keine Kennung, kein Verweis, keine
    Adresse, kein Passwort. Alles wird in m66-auswahl.sql hergeleitet.

    Bewusst reines ASCII: Windows PowerShell 5.1 liest .ps1 ohne BOM als ANSI.
#>

[CmdletBinding()]
param(
    [string] $Mysql = "C:\Program Files\MySQL\MySQL Workbench 8.0 CE\mysql.exe"
)

$ErrorActionPreference = 'Stop'
$proj   = $PSScriptRoot
$arbeit = Join-Path $proj 'arbeit'
New-Item -ItemType Directory -Force -Path $arbeit | Out-Null

# --- 1. Stichprobe deterministisch beschaffen -------------------------------
$env:MYSQL_PWD = $env:OVERLORD_DB_READ_PASSWORD
$auswahl = Join-Path $arbeit 'm66-auswahl.tsv'
& $Mysql -h $env:OVERLORD_DB_HOST -P $env:OVERLORD_DB_PORT `
         -u $env:OVERLORD_DB_READ_USER `
         --ssl-mode=DISABLED --default-character-set=utf8mb4 --batch --raw `
         $env:OVERLORD_DB_GLASSFISH_SCHEMA `
         -e "source $proj/m66-auswahl.sql" |
    Out-File -FilePath $auswahl -Encoding utf8

$zeilen = Get-Content $auswahl
if ($zeilen[1].Trim() -ne '1') {
    throw "ABBRUCH: global.read_only ist nicht 1, das ist nicht die Testkopie."
}
Write-Output "read_only-Nachweis : $($zeilen[1].Trim())"
Write-Output "Zeilen in der Auswahl (inkl. Kopfzeilen): $($zeilen.Count)"
Write-Output ""

# --- 2. Bauen ---------------------------------------------------------------
$mvnw = Join-Path $proj '..\..\backend\mvnw.cmd'
& $mvnw -q -f "$proj\pom.xml" compile
if ($LASTEXITCODE -ne 0) { throw "ABBRUCH: compile fehlgeschlagen." }

# --- 3. Der Lauf ------------------------------------------------------------
# mainClass ausdruecklich: das Projekt hat mehrere Einstiegspunkte
# (M70 Mitschnitt, M71 Abruf, M66 Deckung). Abweichung 11 wiederholt sich nicht.
& $mvnw -q -f "$proj\pom.xml" exec:java `
    "-Dexec.mainClass=mitschnitt.Deckung" `
    "-Dexec.args=$auswahl $arbeit"

Write-Output ""
Write-Output "exit=$LASTEXITCODE"
