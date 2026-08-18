<#
    M72 - Traegt der jakarta-Zweig von SAAJ dieselben Bytes auf die Leitung?

    DIESES SKRIPT IST DER AUFRUF. Abgelegt und von hier ausgefuehrt.

    Offener Punkt 1 aus docs/rohdaten.md: M70 und M71 liefen mit dem
    javax-Zweig (javax.xml.soap-api 1.4.0, saaj-impl 1.5.3). In einem
    Spring-Boot-4-Projekt ist der ein Fremdkoerper. Der jakarta-Zweig waere
    stimmig, sein Verhalten auf der Leitung ist aber ungeprueft.

    Ablauf:
      1. SHA-256 der vier Alt-Dateien VOR dem Lauf.
      2. javax-Variante gegen den lokalen Lauscher  -> anfrage.bin
      3. jakarta-Variante gegen den lokalen Lauscher -> anfrage.bin
      4. Bytevergleich: Anfragezeile, Kopfzeilen in gesendeter Reihenfolge, Rumpf.
      5. SHA-256 der vier Alt-Dateien NACH dem Lauf.
      6. Aufraeumen, Loeschung pruefen.

    Warum die javax-Variante mitlaeuft, obwohl M70 sie schon gemessen hat:
    Der Mitschnitt aus M70 ist am 17.08.2026 geloescht worden (siehe M71,
    Abschnitt "Loeschung"). Es gibt keine Datei mehr, gegen die sich
    vergleichen liesse. Der Lauf holt sie unter denselben Bedingungen zurueck
    und weist zugleich nach, dass er die in M70 dokumentierten 579 Byte
    reproduziert. M70 selbst wird dabei nicht angefasst.

    G1: Es wird NICHTS an einen Filestore gesendet. Beide Lauscher binden auf
    127.0.0.1, die GUID ist frei erfunden, der ServiceConnectString wird
    weder gelesen noch aufgeloest. Der Mitschnitt enthaelt keine echten Daten
    und darf deshalb im Wortlaut in die Ergebnisdatei.

    Bewusst reines ASCII: Windows PowerShell 5.1 liest .ps1 ohne BOM als ANSI.

    Aufruf: .\m72.ps1
#>

[CmdletBinding()]
param(
    [switch] $Behalten
)

$ErrorActionPreference = 'Stop'

$jakartaProj = $PSScriptRoot
$wurzel      = (Resolve-Path (Join-Path $jakartaProj '..\..')).Path
$javaxProj   = Join-Path $wurzel 'scripts\mitschnitt-saaj'
$altCode     = Join-Path $wurzel 'alterCode\OverlordFilestoreClient\src\de\kraftwerkone\filestore\client'
$mvnw        = Join-Path $wurzel 'backend\mvnw.cmd'

$vier = @('FilestoreClient', 'FilestoreFile', 'PayloadReader', 'PayloadWriter')

function Pruefsummen {
    param([string] $Ueberschrift)
    Write-Output ""
    Write-Output "=== $Ueberschrift ==="
    foreach ($f in $vier) {
        $orig  = Join-Path $altCode "$f.java"
        $kopie = Join-Path $javaxProj "src\main\java\de\kraftwerkone\filestore\client\$f.java"
        $hOrig  = (Get-FileHash -Algorithm SHA256 $orig).Hash.ToLower()
        $hKopie = (Get-FileHash -Algorithm SHA256 $kopie).Hash.ToLower()
        $gleich = if ($hOrig -eq $hKopie) { 'ja' } else { 'NEIN' }
        Write-Output ("{0,-18} {1}  Kopie=M70 identisch: {2}" -f "$f.java", $hOrig, $gleich)
    }
}

# Gibt nichts zurueck. Der Pfad des Mitschnitts wird vom Aufrufer gebildet:
# Eine Funktion, die zugleich meldet und einen Wert liefert, gibt in PowerShell
# beides ueber dieselbe Pipeline zurueck - der Aufrufer bekaeme ein Feld aus
# Meldezeilen mit dem Pfad am Ende.
function Laufen {
    param([string] $Projekt, [string] $MainClass, [string] $Etikett)

    Write-Output ""
    Write-Output "########## Lauf: $Etikett ##########"

    $mit = Join-Path $Projekt 'mitschnitt'
    if (Test-Path $mit) { Remove-Item -Recurse -Force $mit }

    Push-Location $Projekt
    try {
        & $mvnw -q -f "$Projekt\pom.xml" compile
        if ($LASTEXITCODE -ne 0) { throw "ABBRUCH: compile fehlgeschlagen ($Etikett)." }

        # Welche Jars der Klassenpfad wirklich traegt - das gehoert zum Befund.
        $baum = Join-Path $Projekt 'abhaengigkeiten.txt'
        & $mvnw -q -f "$Projekt\pom.xml" dependency:list "-DoutputFile=$baum" "-DincludeScope=runtime" | Out-Null

        # mainClass ausdruecklich (Abweichung 11 aus M70 wiederholt sich nicht).
        & $mvnw -q -f "$Projekt\pom.xml" exec:java "-Dexec.mainClass=$MainClass"
        if ($LASTEXITCODE -ne 0) { throw "ABBRUCH: Lauf fehlgeschlagen ($Etikett)." }
    }
    finally {
        Pop-Location
    }

    if (-not (Test-Path (Join-Path $mit 'anfrage.bin'))) {
        throw "ABBRUCH: kein Mitschnitt entstanden ($Etikett)."
    }
}

# Zerlegt den Mitschnitt in Anfragezeile, Kopfzeilen und Rumpf. Rein bytebasiert:
# Die Grenze ist CRLFCRLF, nichts wird umkodiert oder normalisiert.
function Zerlegen {
    param([string] $Pfad)

    $bytes = [System.IO.File]::ReadAllBytes($Pfad)
    $grenze = -1
    for ($i = 0; $i -lt $bytes.Length - 3; $i++) {
        if ($bytes[$i] -eq 13 -and $bytes[$i+1] -eq 10 -and $bytes[$i+2] -eq 13 -and $bytes[$i+3] -eq 10) {
            $grenze = $i
            break
        }
    }
    if ($grenze -lt 0) { throw "ABBRUCH: keine Kopf/Rumpf-Grenze in $Pfad." }

    $kopfBytes  = $bytes[0..($grenze - 1)]
    $rumpfBytes = if ($grenze + 4 -lt $bytes.Length) { $bytes[($grenze + 4)..($bytes.Length - 1)] } else { @() }

    # Kopfzeilen sind nach RFC ISO-8859-1; hier ist ohnehin alles ASCII.
    $kopfText = [System.Text.Encoding]::GetEncoding('ISO-8859-1').GetString($kopfBytes)
    $zeilen   = $kopfText -split "`r`n"

    return [pscustomobject]@{
        Pfad        = $Pfad
        Gesamtbytes = $bytes.Length
        Sha256      = (Get-FileHash -Algorithm SHA256 $Pfad).Hash.ToLower()
        Anfragezeile = $zeilen[0]
        Kopfzeilen  = @($zeilen[1..($zeilen.Length - 1)])
        RumpfBytes  = $rumpfBytes
        RumpfText   = [System.Text.Encoding]::GetEncoding('ISO-8859-1').GetString($rumpfBytes)
        RumpfLaenge = $rumpfBytes.Length
        UeberSieben = @($rumpfBytes | Where-Object { $_ -gt 127 }).Count
    }
}

# Der Loopback-Port wechselt bei jedem Lauf. Fuer den Vergleich wird er
# ersetzt, fuer die Ausgabe ebenso - es ist ein Loopback-Port, keine Adresse
# nach G1, aber er gehoert nicht zum Befund.
function PortWeg {
    param([string] $Zeile)
    return ($Zeile -replace '^(Host:\s*127\.0\.0\.1):\d+$', '$1:<port>')
}

Write-Output "########## M72 ##########"
# Kein "2>&1" auf java.exe: Windows PowerShell 5.1 verpackt jede Zeile dann in
# einen ErrorRecord und laesst den Lauf an $ErrorActionPreference='Stop' sterben.
# java -version schreibt nach stderr, deshalb ueber ProcessStartInfo.
$jvmInfo = New-Object System.Diagnostics.ProcessStartInfo
$jvmInfo.FileName = 'java'
$jvmInfo.Arguments = '-version'
$jvmInfo.RedirectStandardError = $true
$jvmInfo.UseShellExecute = $false
$jvm = [System.Diagnostics.Process]::Start($jvmInfo)
$jvmText = $jvm.StandardError.ReadToEnd()
$jvm.WaitForExit()
Write-Output "Java:"
Write-Output $jvmText.TrimEnd()

Pruefsummen -Ueberschrift 'SHA-256 der vier Alt-Dateien VOR dem Lauf'

Write-Output ""
Write-Output "=== Die jakarta-Kopie gegen das Original ==="
foreach ($f in $vier) {
    $orig  = Join-Path $altCode "$f.java"
    $kopie = Join-Path $jakartaProj "src\main\java\de\kraftwerkone\filestore\client\$f.java"
    $hOrig  = (Get-FileHash -Algorithm SHA256 $orig).Hash.ToLower()
    $hKopie = (Get-FileHash -Algorithm SHA256 $kopie).Hash.ToLower()
    if ($hOrig -eq $hKopie) {
        Write-Output ("{0,-18} byteidentisch" -f "$f.java")
    } else {
        $abw = @(Compare-Object (Get-Content $orig) (Get-Content $kopie) | Where-Object { $_.SideIndicator -eq '=>' })
        Write-Output ("{0,-18} {1} geaenderte Zeile(n):" -f "$f.java", $abw.Count)
        foreach ($a in $abw) { Write-Output ("                   + " + $a.InputObject) }
    }
}

Laufen -Projekt $javaxProj -MainClass 'mitschnitt.Mitschnitt' -Etikett 'javax  (M70-Aufbau)'
$javaxKopie = Join-Path $jakartaProj 'anfrage-javax.bin'
Copy-Item (Join-Path $javaxProj 'mitschnitt\anfrage.bin') $javaxKopie -Force

Laufen -Projekt $jakartaProj -MainClass 'mitschnitt.MitschnittJakarta' -Etikett 'jakarta'
$jakartaKopie = Join-Path $jakartaProj 'anfrage-jakarta.bin'
Copy-Item (Join-Path $jakartaProj 'mitschnitt\anfrage.bin') $jakartaKopie -Force

$a = Zerlegen -Pfad $javaxKopie
$b = Zerlegen -Pfad $jakartaKopie

Write-Output ""
Write-Output "=== Bytevergleich ==="
Write-Output ("{0,-26} {1,12} {2,12}" -f '', 'javax', 'jakarta')
Write-Output ("{0,-26} {1,12} {2,12}" -f 'Gesamtbytes',   $a.Gesamtbytes, $b.Gesamtbytes)
Write-Output ("{0,-26} {1,12} {2,12}" -f 'Rumpfbytes',    $a.RumpfLaenge, $b.RumpfLaenge)
Write-Output ("{0,-26} {1,12} {2,12}" -f 'Bytes ueber 0x7F im Rumpf', $a.UeberSieben, $b.UeberSieben)
Write-Output ("{0,-26} {1,12} {2,12}" -f 'Kopfzeilen',    $a.Kopfzeilen.Count, $b.Kopfzeilen.Count)

Write-Output ""
Write-Output "--- Anfragezeile ---"
Write-Output ("javax   : " + $a.Anfragezeile)
Write-Output ("jakarta : " + $b.Anfragezeile)
$anfrageGleich = ($a.Anfragezeile -eq $b.Anfragezeile)
Write-Output ("gleich  : " + $(if ($anfrageGleich) { 'ja' } else { 'NEIN' }))

Write-Output ""
Write-Output "--- Kopfzeilen in gesendeter Reihenfolge ---"
$max = [Math]::Max($a.Kopfzeilen.Count, $b.Kopfzeilen.Count)
$kopfAbweichungen = @()
for ($i = 0; $i -lt $max; $i++) {
    $za = if ($i -lt $a.Kopfzeilen.Count) { PortWeg $a.Kopfzeilen[$i] } else { '<fehlt>' }
    $zb = if ($i -lt $b.Kopfzeilen.Count) { PortWeg $b.Kopfzeilen[$i] } else { '<fehlt>' }
    $mark = if ($za -eq $zb) { '   ' } else { ' ! '; }
    if ($za -ne $zb) { $kopfAbweichungen += [pscustomobject]@{ Nr = $i + 1; Javax = $za; Jakarta = $zb } }
    Write-Output ("{0}{1,2}. javax   : {2}" -f $mark, ($i + 1), $za)
    Write-Output ("{0}    jakarta : {1}" -f $mark, $zb)
}

Write-Output ""
Write-Output "--- Rumpf ---"
$rumpfGleich = ($a.RumpfLaenge -eq $b.RumpfLaenge)
if ($rumpfGleich) {
    for ($i = 0; $i -lt $a.RumpfLaenge; $i++) {
        if ($a.RumpfBytes[$i] -ne $b.RumpfBytes[$i]) { $rumpfGleich = $false; break }
    }
}
Write-Output ("byteidentisch : " + $(if ($rumpfGleich) { 'ja' } else { 'NEIN' }))
Write-Output ""
Write-Output "javax:"
Write-Output $a.RumpfText
Write-Output ""
Write-Output "jakarta:"
Write-Output $b.RumpfText

Write-Output ""
Write-Output "=== Deutung nach der vorregistrierten Tabelle ==="
$auswertend = @('Content-Type', 'SOAPAction', 'Content-Length', 'Accept',
                'Cache-Control', 'Pragma', 'Connection', 'Host')
$kritisch = @($kopfAbweichungen | Where-Object {
    $name = ($_.Javax -split ':')[0]
    if ($name -eq '<fehlt>') { $name = ($_.Jakarta -split ':')[0] }
    $auswertend -contains $name
})
Write-Output ("abweichende Kopfzeilen gesamt   : " + $kopfAbweichungen.Count)
Write-Output ("davon auswertungsrelevant       : " + $kritisch.Count)
if ($rumpfGleich -and $anfrageGleich -and $kritisch.Count -eq 0) {
    Write-Output "TOR OFFEN  -> weiter mit dem jakarta-Zweig im Backend."
} else {
    Write-Output "TOR ZU     -> anhalten und melden."
}

if (-not $Behalten) {
    Write-Output ""
    Write-Output "=== Aufraeumen ==="
    foreach ($p in @($javaxKopie, $jakartaKopie,
                     (Join-Path $javaxProj 'mitschnitt'), (Join-Path $jakartaProj 'mitschnitt'),
                     (Join-Path $javaxProj 'retrieve'),    (Join-Path $jakartaProj 'retrieve'),
                     (Join-Path $javaxProj 'abhaengigkeiten.txt'), (Join-Path $jakartaProj 'abhaengigkeiten.txt'))) {
        if (Test-Path $p) { Remove-Item -Recurse -Force $p }
        Write-Output ("weg: {0,-5}  {1}" -f (-not (Test-Path $p)), $p)
    }
}

Pruefsummen -Ueberschrift 'SHA-256 der vier Alt-Dateien NACH dem Lauf'
Write-Output ""
Write-Output ("Ende: " + (Get-Date -Format 'yyyy-MM-dd HH:mm:ss'))
