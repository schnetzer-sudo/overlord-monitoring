<#
    M71 - Ein Abruf mit dem echten Client gegen die Ablage, die der Verweis nennt.

    DIESES SKRIPT IST DER AUFRUF. Es wird abgelegt und dann ausgefuehrt, nicht
    direkt abgesetzt - die Luecke aus Teil B, wo die curl-Aufrufe von M59 nur im
    Sitzungsprotokoll standen und nie als Skript, wiederholt sich damit nicht.

    EIN Lauf, EIN Verweis. Keine Schleife, keine Wiederholung.

    Was hier NICHT drinsteht (Regel G1):
    Keine Kennung, kein Verweis, keine Adresse, kein Passwort. Verweis und
    ServiceConnectString werden in abruf-auswahl.sql deterministisch hergeleitet
    und ueber Prozessargumente weitergereicht. Auf den Bildschirm kommen
    ausschliesslich Zahlen.

    Zugangsdaten kommen aus den Umgebungsvariablen OVERLORD_DB_*.

    Bewusst reines ASCII: Windows PowerShell 5.1 liest .ps1 ohne BOM als ANSI,
    und ein Gedankenstrich im Quelltext bricht dann den Parser.
#>

[CmdletBinding()]
param(
    [string] $Mysql = "C:\Program Files\MySQL\MySQL Workbench 8.0 CE\mysql.exe"
)

$ErrorActionPreference = 'Stop'
$proj     = $PSScriptRoot
$arbeit   = Join-Path $proj 'arbeit'
$retrieve = Join-Path $proj 'retrieve'
New-Item -ItemType Directory -Force -Path $arbeit, $retrieve | Out-Null

# --- 1. Verweis und Adresse deterministisch beschaffen ----------------------
$env:MYSQL_PWD = $env:OVERLORD_DB_READ_PASSWORD
& $Mysql -h $env:OVERLORD_DB_HOST -P $env:OVERLORD_DB_PORT `
         -u $env:OVERLORD_DB_READ_USER `
         --ssl-mode=DISABLED --default-character-set=utf8mb4 --batch --raw `
         $env:OVERLORD_DB_GLASSFISH_SCHEMA `
         -e "source $proj/abruf-auswahl.sql" |
    Out-File -FilePath (Join-Path $arbeit 'auswahl.txt') -Encoding utf8

$zeilen = Get-Content (Join-Path $arbeit 'auswahl.txt')
# Zeile 0/1: read_only-Nachweis, Zeile 2: Spaltenkopf, Zeile 3: die Daten
if ($zeilen[1].Trim() -ne '1') {
    throw "ABBRUCH: global.read_only ist nicht 1, das ist nicht die Testkopie."
}
$felder  = $zeilen[3] -split "`t"
$verweis = $felder[0]
$adresse = $felder[1]

Write-Output "read_only-Nachweis        : $($zeilen[1].Trim())"
Write-Output "Verweis beschafft         : $($verweis.Length) Zeichen"
Write-Output "Adresse beschafft         : $($adresse.Length) Zeichen"
Write-Output ""

# --- 2. Bauen ---------------------------------------------------------------
# exec:java kompiliert nicht von selbst. Der Bau gehoert ins Skript, sonst ist
# der Lauf nicht reproduzierbar.
$mvnw = Join-Path $proj '..\..\backend\mvnw.cmd'
& $mvnw -q -f "$proj\pom.xml" compile
if ($LASTEXITCODE -ne 0) { throw "ABBRUCH: compile fehlgeschlagen." }

# --- 3. Der eine Abruf ------------------------------------------------------
# Der Java-Teil zerlegt den Verweis nach Q1 und sendet die nackte GUID.
# mainClass wird ausdruecklich gesetzt, weil das Projekt zwei Einstiegspunkte
# hat (M70: mitschnitt.Mitschnitt, M71: mitschnitt.Abruf).
& $mvnw -q -f "$proj\pom.xml" exec:java `
    "-Dexec.mainClass=mitschnitt.Abruf" `
    "-Dexec.args=$adresse $verweis $retrieve"

Write-Output ""
Write-Output "exit=$LASTEXITCODE"
