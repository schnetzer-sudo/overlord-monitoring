<#
    Gemeinsamer Ablauf fuer M60, M61, M63, M64, M65, M66 (2), M67 und M68.

    DIESES SKRIPT IST DER AUFRUF. Abgelegt und von hier ausgefuehrt.

    Ablauf: Stichprobe per SQL beschaffen -> sequenziell abrufen -> zaehlend
    auswerten -> Dateien loeschen -> Loeschung pruefen.

    Holregel: je Verweis bei der Ablage, die er nennt. Kein Rueckfall.
    Einzige Ausnahme ist M68, wo die SQL-Datei bewusst die Adresse der ANDEREN
    Ablage liefert; das ist im Auftrag als Kreuzabruf benannt.

    Operation ausschliesslich RETRIEVE.

    G1 Stufe 2: Kein ZIP-Eintrag wird hier geoeffnet, kein Inhalt ausgegeben.
    Die geholten Dateien werden nach dem Zaehlen geloescht.

    Bewusst reines ASCII: Windows PowerShell 5.1 liest .ps1 ohne BOM als ANSI.

    Aufruf: .\messung.ps1 -Sql m60-auswahl.sql -Name M60 [-Auswerten]
#>

[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)] [string] $Sql,
    [Parameter(Mandatory = $true)] [string] $Name,
    [switch] $Auswerten,
    [string] $Mysql = "C:\Program Files\MySQL\MySQL Workbench 8.0 CE\mysql.exe"
)

$ErrorActionPreference = 'Stop'
$proj   = $PSScriptRoot
$arbeit = Join-Path $proj "arbeit-$Name"
if (Test-Path $arbeit) { Remove-Item -Recurse -Force $arbeit }
New-Item -ItemType Directory -Force -Path $arbeit | Out-Null

Write-Output "########## $Name ##########"

# --- 1. Stichprobe beschaffen -----------------------------------------------
$env:MYSQL_PWD = $env:OVERLORD_DB_READ_PASSWORD
$tsv = Join-Path $arbeit 'stichprobe.tsv'
& $Mysql -h $env:OVERLORD_DB_HOST -P $env:OVERLORD_DB_PORT `
         -u $env:OVERLORD_DB_READ_USER `
         --ssl-mode=DISABLED --default-character-set=utf8mb4 --batch --raw `
         $env:OVERLORD_DB_GLASSFISH_SCHEMA `
         -e "source $proj/$Sql" |
    Out-File -FilePath $tsv -Encoding utf8

$zeilen = Get-Content $tsv
if ($zeilen[1].Trim() -ne '1') {
    throw "ABBRUCH: global.read_only ist nicht 1, das ist nicht die Testkopie."
}
Write-Output "read_only-Nachweis : $($zeilen[1].Trim())"
# Wie viele Verweise sind uebergangen worden, weil sie eine abgeschaltete
# Ablage nennen? Diese Zahl ist selbst ein Befund: Sie sagt, welcher Anteil des
# Bestands heute ueberhaupt abrufbar ist.
$zeilen | Where-Object { $_ -like 'UEBERGANGEN*' } | ForEach-Object {
    Write-Output ("uebergangene Verweise (abgeschaltete Ablage) : " + (($_ -split "`t")[1..2] -join ' von '))
}

# --- 2. Bauen ---------------------------------------------------------------
$mvnw = Join-Path $proj '..\..\backend\mvnw.cmd'
& $mvnw -q -f "$proj\pom.xml" compile
if ($LASTEXITCODE -ne 0) { throw "ABBRUCH: compile fehlgeschlagen." }

# --- 3. Abruf ---------------------------------------------------------------
# mainClass ausdruecklich (Abweichung 11 wiederholt sich nicht).
& $mvnw -q -f "$proj\pom.xml" exec:java `
    "-Dexec.mainClass=mitschnitt.Stichprobe" `
    "-Dexec.args=$tsv $arbeit"

# --- 4. Auswertung ----------------------------------------------------------
if ($Auswerten) {
    Write-Output ""
    Write-Output "=== Auswertung (Entpackschritt, zaehlend) ==="
    & (Join-Path $proj '..\messung-schritt8\auswertung.ps1') `
        -Arbeitsverzeichnis (Join-Path $arbeit 'dateien') `
        -Zuordnung (Join-Path $arbeit 'zuordnung.csv') `
        -Ausgabe (Join-Path $proj "..\messung-schritt8\ergebnis\$Name.csv")
}

# --- 5. Aufraeumen ----------------------------------------------------------
$dat = Join-Path $arbeit 'dateien'
$anz = @(Get-ChildItem $dat -File -ErrorAction SilentlyContinue).Count
$sum = (Get-ChildItem $dat -File -ErrorAction SilentlyContinue | Measure-Object -Property Length -Sum).Sum
Write-Output ""
Write-Output "=== Aufraeumen ==="
Write-Output "geholte Dateien   : $anz  ($sum Byte)"
Remove-Item -Recurse -Force $arbeit
Write-Output "geloescht         : $(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')"
Write-Output "Verzeichnis weg   : $(-not (Test-Path $arbeit))"
