#Requires -Version 5.1
<#
.SYNOPSIS
    Startet Backend und Frontend von Overlord Monitoring lokal in zwei getrennten
    PowerShell-Fenstern.

.DESCRIPTION
    Ablauf:
      1. Verzeichnisse pruefen (backend/, frontend/)
      2. Umgebung pruefen (OVERLORD_*-Variablen, frontend/.env.local)
      3. Datenbank-Erreichbarkeit pruefen (reiner TCP-Verbindungstest)
      4. Pruefen, ob Port 8080 und 3000 frei sind
      5. Zwei Fenster starten
      6. Auf /actuator/health warten
      7. Auf das Frontend warten und den Browser oeffnen

    Dieses Skript enthaelt KEINE Zugangsdaten und setzt keine. Host, Benutzer und
    Passwoerter kommen ausschliesslich aus Umgebungsvariablen -- siehe
    PROJEKTBESCHREIBUNG.md Abschnitt 3.0.

.PARAMETER CodeGen
    Laesst die jOOQ-Codegenerierung mitlaufen. Ohne diesen Schalter wird sie
    uebersprungen (-Djooq.codegen.skip=true).

.PARAMETER Force
    Startet auch dann, wenn Port 8080 oder 3000 bereits belegt sind.

.PARAMETER NoBrowser
    Oeffnet am Ende keinen Browser.

.PARAMETER DbHost
    Datenbank-Host fuer den Erreichbarkeitstest, falls er sich nicht aus den
    Umgebungsvariablen ermitteln laesst.

.PARAMETER DbPort
    Datenbank-Port fuer den Erreichbarkeitstest. Standard 3306.

.PARAMETER TimeoutSekunden
    Wartezeit auf das Backend. Standard 300.

.EXAMPLE
    .\dev-start.ps1

.EXAMPLE
    .\dev-start.ps1 -CodeGen
#>
[CmdletBinding()]
param(
    [switch] $CodeGen,
    [switch] $Force,
    [switch] $NoBrowser,
    [string] $DbHost,
    [int]    $DbPort = 3306,
    [int]    $TimeoutSekunden = 300
)

$ErrorActionPreference = 'Stop'

# ---------------------------------------------------------------------------
# Konfiguration
# ---------------------------------------------------------------------------

$RepoWurzel   = Split-Path -Parent $PSScriptRoot
$BackendPfad  = Join-Path $RepoWurzel 'backend'
$FrontendPfad = Join-Path $RepoWurzel 'frontend'

$BackendPort  = 8080
$FrontendPort = 3000
$HealthUrl    = "http://localhost:$BackendPort/actuator/health"
$FrontendUrl  = "http://localhost:$FrontendPort"

# Namen der Umgebungsvariablen, aus denen Host und Port der Datenbank gelesen
# werden. Weichen die Namen im Projekt ab, hier anpassen -- sonst faellt nur der
# Erreichbarkeitstest weg, der Start laeuft trotzdem.
$DbUrlVariablen  = @('OVERLORD_DB_URL', 'OVERLORD_DB_JDBC_URL')
$DbHostVariablen = @('OVERLORD_DB_HOST', 'OVERLORD_DB_SERVER')
$DbPortVariablen = @('OVERLORD_DB_PORT')

# ---------------------------------------------------------------------------
# Ausgabe-Helfer
# ---------------------------------------------------------------------------

function Schritt($text) { Write-Host ""; Write-Host "== $text" -ForegroundColor Cyan }
function Ok($text)      { Write-Host "   [ok]   $text" -ForegroundColor Green }
function Hinweis($text) { Write-Host "   [info] $text" -ForegroundColor Gray }
function Warnung($text) { Write-Host "   [warn] $text" -ForegroundColor Yellow }
function Fehler($text)  { Write-Host "   [FEHL] $text" -ForegroundColor Red }

function Abbruch($text) {
    Fehler $text
    Write-Host ""
    exit 1
}

# ---------------------------------------------------------------------------
# Technische Helfer
# ---------------------------------------------------------------------------

function Test-TcpVerbindung {
    param([string] $Ziel, [int] $Port, [int] $TimeoutMs = 3000)

    $client = New-Object System.Net.Sockets.TcpClient
    try {
        $async = $client.BeginConnect($Ziel, $Port, $null, $null)
        if (-not $async.AsyncWaitHandle.WaitOne($TimeoutMs)) { return $false }
        $client.EndConnect($async)
        return $true
    } catch {
        return $false
    } finally {
        $client.Close()
    }
}

function Get-PortBelegung {
    param([int] $Port)

    try {
        $verbindung = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction Stop |
                      Select-Object -First 1
        if (-not $verbindung) { return $null }
        $prozess = Get-Process -Id $verbindung.OwningProcess -ErrorAction SilentlyContinue
        $name = if ($prozess) { $prozess.ProcessName } else { 'unbekannt' }
        return [pscustomobject]@{ Pid = $verbindung.OwningProcess; Name = $name }
    } catch {
        # Get-NetTCPConnection nicht verfuegbar oder Port frei
        return $null
    }
}

function Get-DatenbankZiel {
    # 1. Vollstaendige JDBC-URL
    foreach ($name in $DbUrlVariablen) {
        $wert = [Environment]::GetEnvironmentVariable($name)
        if ($wert -and $wert -match 'jdbc:[a-z]+://([^:/,?]+)(?::(\d+))?') {
            $port = 3306
            if ($Matches[2]) { $port = [int] $Matches[2] }
            return [pscustomobject]@{ Ziel = $Matches[1]; Port = $port; Quelle = $name }
        }
    }

    # 2. Getrennte Host-/Port-Variablen
    foreach ($name in $DbHostVariablen) {
        $wert = [Environment]::GetEnvironmentVariable($name)
        if ($wert) {
            $port = 3306
            foreach ($pName in $DbPortVariablen) {
                $pWert = [Environment]::GetEnvironmentVariable($pName)
                if ($pWert) { $port = [int] $pWert; break }
            }
            return [pscustomobject]@{ Ziel = $wert; Port = $port; Quelle = $name }
        }
    }

    return $null
}

function Get-PowerShellPfad {
    $pfad = (Get-Process -Id $PID).Path
    if ($pfad -and (Split-Path -Leaf $pfad) -match '^(pwsh|powershell)\.exe$') { return $pfad }
    return 'powershell.exe'
}

function Warte-AufBackend {
    param([int] $Sekunden, [System.Diagnostics.Process] $Fenster)

    $ende = (Get-Date).AddSeconds($Sekunden)
    while ((Get-Date) -lt $ende) {

        if ($Fenster -and $Fenster.HasExited) {
            Write-Host ""
            return 'FENSTER_ZU'
        }

        try {
            $antwort = Invoke-WebRequest -Uri $HealthUrl -UseBasicParsing -TimeoutSec 5
            Write-Host ""
            try { return (ConvertFrom-Json $antwort.Content).status } catch { return 'UP' }
        } catch {
            $code = $null
            if ($_.Exception.Response -and $_.Exception.Response.StatusCode) {
                $code = [int] $_.Exception.Response.StatusCode
            }
            if ($code -eq 401 -or $code -eq 403) { Write-Host ""; return 'GESCHUETZT' }
            if ($code -eq 503)                   { Write-Host ""; return 'DOWN' }
            Write-Host "." -NoNewline
            Start-Sleep -Seconds 2
        }
    }

    Write-Host ""
    return $null
}

# ---------------------------------------------------------------------------
# 1. Verzeichnisse
# ---------------------------------------------------------------------------

Write-Host ""
Write-Host "Overlord Monitoring -- lokaler Start" -ForegroundColor White
Write-Host "Repo: $RepoWurzel" -ForegroundColor DarkGray

Schritt "Verzeichnisse"

if (-not (Test-Path (Join-Path $BackendPfad 'mvnw.cmd'))) {
    Abbruch "backend\mvnw.cmd nicht gefunden. Liegt dieses Skript in <repo>\scripts\ ?"
}
if (-not (Test-Path (Join-Path $FrontendPfad 'package.json'))) {
    Abbruch "frontend\package.json nicht gefunden. Liegt dieses Skript in <repo>\scripts\ ?"
}
Ok "backend\ und frontend\ gefunden"

# ---------------------------------------------------------------------------
# 2. Umgebung
# ---------------------------------------------------------------------------

Schritt "Umgebung"

$overlordVariablen = Get-ChildItem Env: | Where-Object { $_.Name -like 'OVERLORD_*' }
if ($overlordVariablen.Count -eq 0) {
    Warnung "Keine einzige OVERLORD_*-Umgebungsvariable gesetzt."
    Warnung "Das Backend wird die Datenbank nicht erreichen. Siehe backend\.env.example."
} else {
    Ok "$($overlordVariablen.Count) OVERLORD_*-Variablen gesetzt: $(($overlordVariablen.Name | Sort-Object) -join ', ')"
}

$envLocal = Join-Path $FrontendPfad '.env.local'
if (Test-Path $envLocal) {
    Ok "frontend\.env.local vorhanden"
} else {
    Warnung "frontend\.env.local fehlt. Ohne OVERLORD_BACKEND_URL greift der Standard http://localhost:8080."
}

# ---------------------------------------------------------------------------
# 3. Datenbank
# ---------------------------------------------------------------------------

Schritt "Datenbank"

if ($DbHost) {
    $ziel = [pscustomobject]@{ Ziel = $DbHost; Port = $DbPort; Quelle = 'Parameter -DbHost' }
} else {
    $ziel = Get-DatenbankZiel
}

if (-not $ziel) {
    Warnung "Host nicht ermittelbar. Gesucht wurde in: $(($DbUrlVariablen + $DbHostVariablen) -join ', ')."
    Warnung "Erreichbarkeitstest uebersprungen. Alternativ: -DbHost <name> -DbPort <port>."
} else {
    Hinweis "Ziel $($ziel.Ziel):$($ziel.Port) (aus $($ziel.Quelle))"
    if (Test-TcpVerbindung -Ziel $ziel.Ziel -Port $ziel.Port) {
        Ok "Datenbank erreichbar"
    } else {
        Fehler "Datenbank NICHT erreichbar."
        Fehler "Backend und Flyway werden beim Start scheitern. VPN / Netz pruefen."
        Write-Host ""
        $weiter = Read-Host "   Trotzdem starten? (j/N)"
        if ($weiter -notmatch '^[jJyY]') { Abbruch "Abgebrochen." }
    }
}

# ---------------------------------------------------------------------------
# 4. Ports
# ---------------------------------------------------------------------------

Schritt "Ports"

$belegt = $false
foreach ($p in @($BackendPort, $FrontendPort)) {
    $b = Get-PortBelegung -Port $p
    if ($b) {
        Fehler "Port $p ist belegt von $($b.Name) (PID $($b.Pid))."
        $belegt = $true
    } else {
        Ok "Port $p frei"
    }
}

if ($belegt -and -not $Force) {
    Write-Host ""
    Fehler "Ein belegter Port bedeutet meist eine Altinstanz aus dem letzten Lauf."
    Fehler "Der Health-Check wuerde dann gruen melden, waehrend du die alte Instanz testest."
    Write-Host ""
    Write-Host "   Beenden mit:" -ForegroundColor Gray
    Write-Host "   Get-NetTCPConnection -LocalPort $BackendPort,$FrontendPort -State Listen |" -ForegroundColor DarkGray
    Write-Host "     Select-Object -Expand OwningProcess -Unique | ForEach-Object { Stop-Process -Id `$_ -Force }" -ForegroundColor DarkGray
    Write-Host ""
    Write-Host "   Oder Start erzwingen: .\dev-start.ps1 -Force" -ForegroundColor DarkGray
    Abbruch "Abgebrochen."
}

# ---------------------------------------------------------------------------
# 5. Fenster starten
# ---------------------------------------------------------------------------

Schritt "Start"

$mvnArgumente = "spring-boot:run '-Dspring-boot.run.profiles=dev'"
if ($CodeGen) {
    Hinweis "jOOQ-Codegenerierung laeuft mit (langsamer, braucht die Datenbank)"
} else {
    $mvnArgumente += " '-Djooq.codegen.skip=true'"
    Hinweis "jOOQ-Codegenerierung uebersprungen. Erzwingen mit -CodeGen."
}

$pwshPfad = Get-PowerShellPfad

$cmdBackend  = "`$Host.UI.RawUI.WindowTitle = 'Overlord BACKEND :$BackendPort'; " +
               "Set-Location '$BackendPfad'; " +
               "Write-Host 'mvnw.cmd $mvnArgumente' -ForegroundColor DarkGray; " +
               ".\mvnw.cmd $mvnArgumente; " +
               "Write-Host ''; Write-Host '--- Backend beendet. Fenster bleibt offen. ---' -ForegroundColor Yellow"

$cmdFrontend = "`$Host.UI.RawUI.WindowTitle = 'Overlord FRONTEND :$FrontendPort'; " +
               "Set-Location '$FrontendPfad'; " +
               "Write-Host 'pnpm dev' -ForegroundColor DarkGray; " +
               "pnpm dev; " +
               "Write-Host ''; Write-Host '--- Frontend beendet. Fenster bleibt offen. ---' -ForegroundColor Yellow"

$fensterBackend = Start-Process -FilePath $pwshPfad -PassThru `
    -ArgumentList @('-NoExit', '-NoProfile', '-Command', $cmdBackend)
Ok "Backend-Fenster gestartet (PID $($fensterBackend.Id))"

$fensterFrontend = Start-Process -FilePath $pwshPfad -PassThru `
    -ArgumentList @('-NoExit', '-NoProfile', '-Command', $cmdFrontend)
Ok "Frontend-Fenster gestartet (PID $($fensterFrontend.Id))"

# ---------------------------------------------------------------------------
# 6. Auf das Backend warten
# ---------------------------------------------------------------------------

Schritt "Warte auf Backend ($HealthUrl)"
Hinweis "Erster Start kann dauern -- Maven laedt, Flyway migriert. Timeout $TimeoutSekunden s."
Write-Host "   " -NoNewline

$status = Warte-AufBackend -Sekunden $TimeoutSekunden -Fenster $fensterBackend

switch ($status) {
    'UP'         { Ok "Backend UP" }
    'GESCHUETZT' { Ok "Backend antwortet (Health ist abgesichert, 401/403) -- laeuft" }
    'DOWN'       { Warnung "Backend antwortet, meldet aber DOWN. Details im Backend-Fenster." }
    'FENSTER_ZU' { Abbruch "Das Backend-Fenster wurde geschlossen. Kein Start moeglich." }
    $null        { Abbruch "Timeout nach $TimeoutSekunden s. Fehlermeldung steht im Backend-Fenster." }
    default      { Ok "Backend meldet Status '$status'" }
}

# ---------------------------------------------------------------------------
# 7. Auf das Frontend warten
# ---------------------------------------------------------------------------

Schritt "Warte auf Frontend ($FrontendUrl)"
Write-Host "   " -NoNewline

$frontendBereit = $false
$ende = (Get-Date).AddSeconds(120)
while ((Get-Date) -lt $ende) {
    if (Test-TcpVerbindung -Ziel 'localhost' -Port $FrontendPort -TimeoutMs 1000) {
        $frontendBereit = $true
        break
    }
    Write-Host "." -NoNewline
    Start-Sleep -Seconds 2
}
Write-Host ""

if ($frontendBereit) {
    Ok "Frontend erreichbar"
    if (-not $NoBrowser) {
        Start-Process $FrontendUrl | Out-Null
        Ok "Browser geoeffnet"
    }
} else {
    Warnung "Frontend antwortet nicht. Meldung steht im Frontend-Fenster."
}

# ---------------------------------------------------------------------------
# 8. Zusammenfassung
# ---------------------------------------------------------------------------

Write-Host ""
Write-Host "----------------------------------------------------------" -ForegroundColor DarkGray
Write-Host " Anwendung:  $FrontendUrl" -ForegroundColor White
Write-Host " Backend:    http://localhost:$BackendPort  (nur direkt zum Pruefen)" -ForegroundColor DarkGray
Write-Host ""
Write-Host " Beenden:    In beiden Fenstern Strg+C, dann Fenster schliessen." -ForegroundColor Gray
Write-Host " Neu starten: Dieses Skript erneut ausfuehren." -ForegroundColor Gray
Write-Host "----------------------------------------------------------" -ForegroundColor DarkGray
Write-Host ""
