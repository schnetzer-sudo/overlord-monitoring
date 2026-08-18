<#
    Messrunde vor Schritt 8 — Auswertung der geholten Dateien
    Auftrag: docs/messungen-schritt8-auftrag.md, Fassung 3 vom 17.08.2026

    ZWECK
    Dieses Skript wertet die in Teil B geholten Dateien aus und schreibt EINE
    CSV mit Zaehlungen. Es deckt M61 (Kodierung), M63 (Markenbilanz), M65
    (Inhaltsklassen je Bereich) und M67 (Groesse des Beschnitts) ab.

    DIE TRAGENDE REGEL (G1, Stufe 2)
    Das Skript gibt NIEMALS Dateiinhalt aus — nicht auf stdout, nicht in die
    CSV, nicht in eine Protokolldatei. Jedes Feld der Ausgabe ist eine Zahl,
    ein Ja/Nein-Wert oder ein Wort aus einem geschlossenen, im Skript fest
    verdrahteten Vokabular. Es gibt keinen Codepfad, der eine Zeichenfolge aus
    einer Eingabedatei weiterreicht.

    Das einzige Feld mit Textinhalt ist `unb_zeichensatz`; es kann
    ausschliesslich die Werte UNOA, UNOB, UNOC, UNOY oder "keiner" annehmen.
    Diese vier Werte stehen unten als Literal im Skript und stammen nicht aus
    der Datei — die Datei entscheidet nur, welches davon ausgegeben wird.

    NETZ
    Das Skript laeuft ohne Netzzugriff. Holen und Auswerten sind getrennt.

    AUFRUF
      .\auswertung.ps1 -Arbeitsverzeichnis <pfad> [-Zuordnung <pfad>] [-Ausgabe <pfad>]

    EINGABE
      <Arbeitsverzeichnis>\1.bin, 2.bin, ...   die geholten Dateien
      <Zuordnung>  optionale CSV mit den Spalten nr,familie,ablage,art
                   (art = nutzdaten | protokoll). Fehlt sie, bleiben die
                   Spalten leer. Die Zuordnungsdatei ist Stufe-1-Material und
                   gehoert NICHT ins Repository.
#>

[CmdletBinding()]
param(
    [Parameter(Mandatory = $true)]
    [string] $Arbeitsverzeichnis,

    [string] $Zuordnung,

    [string] $Ausgabe = (Join-Path $PSScriptRoot 'ergebnis\auswertung.csv')
)

$ErrorActionPreference = 'Stop'

# ─── Die Marken, exakt wie sie der Auftrag nennt ────────────────────────────
$MarkeStart = '***StartOfLog***'
$MarkeEnde  = '***EndOfLog***'

# Nachsichtige Fassung fuer die Frage "genuegt ein exakter Vergleich?".
# Erlaubt beliebige Gross-/Kleinschreibung und Leerraum zwischen den Teilen.
$MusterStartLose = '(?i)\*{3}\s*start\s*of\s*log\s*\*{3}'
$MusterEndeLose  = '(?i)\*{3}\s*end\s*of\s*log\s*\*{3}'

# ─── Die Inhaltsklassen aus M65, als benannte Muster ────────────────────────
# Absolute Pfade: die drei im Auftrag genannten plus die naheliegenden
# "oder vergleichbar"-Faelle. Die Liste steht hier, damit nachvollziehbar ist,
# was gezaehlt wurde.
$MusterPfad = '(/opt/|/var/|/usr/|/etc/|/home/|/tmp/|/srv/|/mnt/|[A-Za-z]:\\)'

# Dienstkennungen wie FILESTOREPROD09, MPSERVICEPROD01.
$MusterDienst = '[A-Z]+PROD[0-9]+'

# Punkt-getrennter Bezeichner, dessen letztes Glied keine Ziffernfolge ist.
# Das ist die unschaerfste Klasse der Messung; sie wird in der Ergebnisdatei
# ausdruecklich als solche ausgewiesen.
$MusterHost = '\b[A-Za-z0-9][A-Za-z0-9-]*(\.[A-Za-z0-9-]+)*\.[A-Za-z]{2,}\b'

# Aus der Nachricht echote Werte: die Zeile BEGINNT mit "Message.".
$MusterMessage = '^\s*Message\.'

# Kanonische UUID.
$MusterUuid = '(?i)\b[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\b'

# Zeitstempelzeilen.
$MusterZeit = '(Program start|Program end)'

# Dateiendungen, die NICHT als Hostname zaehlen sollen.
$EndungenKeinHost = @(
    'xml','txt','log','csv','edi','dat','zip','gz','tar','pdf','xls','xlsx',
    'doc','docx','jar','war','properties','cfg','ini','bak','tmp','cod','out'
)

function Get-Nutzinhalt {
    <#
        Ergaenzt am 17.08.2026, nach dem Fund in JsonServlet.java:801-802 (Q4).

        Was der Filestore liefert, ist ein ZIP, keine Protokolldatei. Das
        Altsystem liest daraus ausschliesslich den ERSTEN Eintrag
        (`getNextEntry()` genau einmal) und wirft alle weiteren stillschweigend
        weg. Diese Auswertung macht es genauso -- zaehlt die Eintraege aber
        mit, damit jeder Fall mit mehr als einem Eintrag sichtbar wird. Dort
        muesste das neue Werkzeug eine Entscheidung treffen, die im Altsystem
        nie getroffen wurde.

        Gibt Bytes des ersten Eintrags zurueck. Ist die Datei kein ZIP, gilt
        sie unveraendert als Nutzinhalt (so laufen die erfundenen Testdaten
        auch unverpackt durch).

        Kein Byte verlaesst diese Funktion in Richtung Ausgabe.
    #>
    param([string] $Pfad)

    $roh = [System.IO.File]::ReadAllBytes($Pfad)
    $istZip = ($roh.Length -ge 4 -and $roh[0] -eq 0x50 -and $roh[1] -eq 0x4B `
               -and ($roh[2] -eq 0x03 -or $roh[2] -eq 0x05 -or $roh[2] -eq 0x07))
    if (-not $istZip) {
        return [pscustomobject] @{ Bytes = $roh; Eintraege = 0; ZipBytes = $roh.Length }
    }

    Add-Type -AssemblyName System.IO.Compression.FileSystem -ErrorAction SilentlyContinue
    $archiv = [System.IO.Compression.ZipFile]::OpenRead($Pfad)
    try {
        $n = $archiv.Entries.Count
        if ($n -eq 0) {
            return [pscustomobject] @{ Bytes = @(); Eintraege = 0; ZipBytes = $roh.Length }
        }
        $strom = $archiv.Entries[0].Open()
        $puffer = New-Object System.IO.MemoryStream
        $strom.CopyTo($puffer)
        $strom.Close()
        return [pscustomobject] @{ Bytes = $puffer.ToArray(); Eintraege = $n; ZipBytes = $roh.Length }
    } finally {
        $archiv.Dispose()
    }
}

function Test-GueltigesUtf8 {
    param([byte[]] $Bytes)
    try {
        $enc = New-Object System.Text.UTF8Encoding($false, $true)
        [void] $enc.GetString($Bytes)
        return $true
    } catch {
        return $false
    }
}

function Get-Zeilenende {
    param([byte[]] $Bytes)
    $crlf = 0; $lfAllein = 0
    for ($i = 0; $i -lt $Bytes.Length; $i++) {
        if ($Bytes[$i] -eq 0x0A) {
            if ($i -gt 0 -and $Bytes[$i - 1] -eq 0x0D) { $crlf++ } else { $lfAllein++ }
        }
    }
    if ($crlf -gt 0 -and $lfAllein -gt 0) { return 'gemischt' }
    if ($crlf -gt 0) { return 'CRLF' }
    if ($lfAllein -gt 0) { return 'LF' }
    return 'keines'
}

function Measure-Bereich {
    <#
        Zaehlt die Inhaltsklassen ueber eine Menge von Zeilen.
        Gibt ausschliesslich Zahlen zurueck.
    #>
    param([string[]] $Zeilen)

    $z = [ordered]@{
        zeilen = $Zeilen.Count
        pfade = 0; dienste = 0; hosts = 0; message = 0; uuids = 0; zeitstempel = 0
    }
    foreach ($zeile in $Zeilen) {
        if ($zeile -cmatch $MusterPfad)    { $z.pfade++ }
        if ($zeile -cmatch $MusterDienst)  { $z.dienste++ }
        if ($zeile -match  $MusterMessage) { $z.message++ }
        if ($zeile -match  $MusterUuid)    { $z.uuids++ }
        if ($zeile -cmatch $MusterZeit)    { $z.zeitstempel++ }

        # Hostname: mindestens ein Treffer, dessen letztes Glied keine
        # bekannte Dateiendung ist.
        $trefferHost = $false
        foreach ($m in [regex]::Matches($zeile, $MusterHost)) {
            $letztes = ($m.Value -split '\.')[-1].ToLowerInvariant()
            if ($EndungenKeinHost -notcontains $letztes) { $trefferHost = $true; break }
        }
        if ($trefferHost) { $z.hosts++ }
    }
    return $z
}

# ─── Zuordnung einlesen, falls vorhanden ────────────────────────────────────
$zuordnungMap = @{}
if ($Zuordnung -and (Test-Path $Zuordnung)) {
    foreach ($zeile in (Import-Csv -Path $Zuordnung)) {
        $zuordnungMap[[string] $zeile.nr] = $zeile
    }
}

# ─── Ausgabeverzeichnis sicherstellen ───────────────────────────────────────
$ausgabeVerzeichnis = Split-Path -Parent $Ausgabe
if (-not (Test-Path $ausgabeVerzeichnis)) {
    New-Item -ItemType Directory -Force -Path $ausgabeVerzeichnis | Out-Null
}

# ─── Dateien einsammeln, numerisch sortiert ─────────────────────────────────
$dateien = Get-ChildItem -Path $Arbeitsverzeichnis -File |
    Where-Object { $_.BaseName -match '^\d+$' } |
    Sort-Object { [int] $_.BaseName }

if (-not $dateien) {
    Write-Output "Keine Dateien der Form <zahl>.<endung> in $Arbeitsverzeichnis gefunden."
    return
}

$ergebnisse = New-Object System.Collections.Generic.List[object]

foreach ($datei in $dateien) {
    # Entpackschritt (Q4): ausgewertet wird der ERSTE ZIP-Eintrag.
    $nutz = Get-Nutzinhalt -Pfad $datei.FullName
    $bytes = $nutz.Bytes

    # ─── M61 — Kodierung und Binaererkennung ────────────────────────────────
    $bom = ($bytes.Length -ge 3 -and $bytes[0] -eq 0xEF -and $bytes[1] -eq 0xBB -and $bytes[2] -eq 0xBF)
    $ueber7f = 0; $nullbytes = 0; $druckbar = 0
    foreach ($b in $bytes) {
        if ($b -gt 0x7F) { $ueber7f++ }
        if ($b -eq 0x00) { $nullbytes++ }
        if (($b -ge 0x20 -and $b -le 0x7E) -or $b -eq 0x09 -or $b -eq 0x0A -or $b -eq 0x0D) { $druckbar++ }
    }
    $utf8Gueltig = Test-GueltigesUtf8 -Bytes $bytes
    $zeilenende = Get-Zeilenende -Bytes $bytes

    # Binaer vor jeder Kodierungsfrage: ein Nullbyte oder unter 90 % druckbare
    # Zeichen gilt als binaer. Eine Kodierungsaussage ueber eine binaere Datei
    # waere sinnlos.
    $binaer = ($nullbytes -gt 0 -or ($bytes.Length -gt 0 -and $druckbar * 100 -lt $bytes.Length * 90))
    # Nur Dateien mit Bytes ueber 0x7F erlauben ueberhaupt eine Unterscheidung
    # zwischen UTF-8 und ISO-8859-1. Reines ASCII ist beides.
    $unterscheidbar = ($ueber7f -gt 0)

    # Zum Klassifizieren verlustfrei dekodieren: gueltiges UTF-8 als UTF-8,
    # sonst Latin-1 (jedes Byte ist dort ein gueltiges Zeichen, es kann also
    # nichts scheitern und nichts verschluckt werden).
    $text = if ($utf8Gueltig) {
        (New-Object System.Text.UTF8Encoding($false, $false)).GetString($bytes)
    } else {
        [System.Text.Encoding]::GetEncoding(28591).GetString($bytes)
    }
    if ($bom) { $text = $text.TrimStart([char] 0xFEFF) }

    $zeilen = $text -split "`r`n|`n|`r"

    # EDIFACT-Zeichensatz aus dem UNB-Segment. Geschlossenes Vokabular.
    $unb = 'keiner'
    foreach ($kandidat in @('UNOA', 'UNOB', 'UNOC', 'UNOY')) {
        if ($text -cmatch ("UNB\+" + $kandidat)) { $unb = $kandidat; break }
    }

    # ─── M63 — Markenbilanz ─────────────────────────────────────────────────
    $startZeilen = @(); $endeZeilen = @()
    $abweichend = 0; $inEchozeile = 0
    for ($i = 0; $i -lt $zeilen.Count; $i++) {
        $zeile = $zeilen[$i]
        $hatStartExakt = $zeile.Contains($MarkeStart)
        $hatEndeExakt  = $zeile.Contains($MarkeEnde)
        if ($hatStartExakt) { $startZeilen += $i }
        if ($hatEndeExakt)  { $endeZeilen  += $i }

        # Abweichende Schreibweise: die lose Fassung trifft, die exakte nicht.
        if (-not $hatStartExakt -and $zeile -match $MusterStartLose) { $abweichend++ }
        if (-not $hatEndeExakt  -and $zeile -match $MusterEndeLose)  { $abweichend++ }

        # Marke in einer erkennbar echoten Zeile.
        if (($hatStartExakt -or $hatEndeExakt) -and
            ($zeile.Contains('=') -or $zeile.Contains('Message.'))) { $inEchozeile++ }
    }

    # ─── Die konservative Schnittregel ──────────────────────────────────────
    # Von der ERSTEN Startmarke bis zur NAECHSTEN darauffolgenden Endmarke.
    $startPos = if ($startZeilen.Count -gt 0) { $startZeilen[0] } else { -1 }
    $endePos = -1
    if ($startPos -ge 0) {
        foreach ($e in $endeZeilen) { if ($e -gt $startPos) { $endePos = $e; break } }
    }
    $paarVollstaendig = ($startPos -ge 0 -and $endePos -gt $startPos)

    if ($paarVollstaendig) {
        $vor   = if ($startPos -gt 0) { $zeilen[0..($startPos - 1)] } else { @() }
        $innen = if ($endePos - $startPos -gt 1) { $zeilen[($startPos + 1)..($endePos - 1)] } else { @() }
        $nach  = if ($endePos -lt $zeilen.Count - 1) { $zeilen[($endePos + 1)..($zeilen.Count - 1)] } else { @() }
        $unbestimmt = 0
    } else {
        # Ohne vollstaendiges Paar gibt es keinen Innenbereich. Die Zeilen ab
        # der Startmarke werden NICHT auf "nach" verteilt — das waere eine
        # Deutung. Sie werden als unbestimmt gezaehlt.
        $vor = if ($startPos -gt 0) { $zeilen[0..($startPos - 1)] }
               elseif ($startPos -lt 0) { $zeilen }
               else { @() }
        $innen = @(); $nach = @()
        $unbestimmt = $zeilen.Count - $vor.Count
    }

    $mVor   = Measure-Bereich -Zeilen $vor
    $mInnen = Measure-Bereich -Zeilen $innen
    $mNach  = Measure-Bereich -Zeilen $nach

    # ─── M67 — Groesse des Beschnitts ───────────────────────────────────────
    # Bytes des Innenbereichs in derselben Kodierung wie die Quelle gemessen.
    $innenText = ($innen -join "`n")
    $innenBytes = if ($paarVollstaendig) {
        if ($utf8Gueltig) { ([System.Text.Encoding]::UTF8).GetByteCount($innenText) }
        else { ([System.Text.Encoding]::GetEncoding(28591)).GetByteCount($innenText) }
    } else { 0 }

    $zu = $zuordnungMap[[string] $datei.BaseName]

    $ergebnisse.Add([pscustomobject] [ordered] @{
        nr                = [int] $datei.BaseName
        familie           = if ($zu) { $zu.familie } else { '' }
        ablage            = if ($zu) { $zu.ablage }  else { '' }
        art               = if ($zu) { $zu.art }     else { '' }
        # Freies Zusatzfeld aus der Zuordnung. In M60 traegt es
        # FileReader.FileProperty.Size aus der Datenbank, damit sich pruefen
        # laesst, ob jene Spalte Bytes zaehlt. Es ist eine ZAHL aus der
        # Datenbank, kein Inhalt aus einer Datei.
        zusatz            = if ($zu) { $zu.zusatz }  else { '' }

        zip_eintraege     = $nutz.Eintraege
        zip_bytes         = $nutz.ZipBytes
        bytes             = $bytes.Length
        zeilen            = $zeilen.Count

        bom               = [int] $bom
        bytes_ueber_7f    = $ueber7f
        nullbytes         = $nullbytes
        druckbar          = $druckbar
        binaer            = [int] $binaer
        unterscheidbar    = [int] $unterscheidbar
        gueltiges_utf8    = [int] $utf8Gueltig
        zeilenende        = $zeilenende
        unb_zeichensatz   = $unb

        start_marken      = $startZeilen.Count
        end_marken        = $endeZeilen.Count
        paar_vollstaendig = [int] $paarVollstaendig
        marke_abweichend  = $abweichend
        marke_in_echozeile = $inEchozeile
        start_zeilennr    = $startPos
        ende_zeilennr     = $endePos

        innen_bytes       = $innenBytes
        innen_zeilen      = $mInnen.zeilen
        unbestimmt_zeilen = $unbestimmt

        vor_zeilen        = $mVor.zeilen
        vor_pfade         = $mVor.pfade
        vor_dienste       = $mVor.dienste
        vor_hosts         = $mVor.hosts
        vor_message       = $mVor.message
        vor_uuids         = $mVor.uuids
        vor_zeitstempel   = $mVor.zeitstempel

        innen_pfade       = $mInnen.pfade
        innen_dienste     = $mInnen.dienste
        innen_hosts       = $mInnen.hosts
        innen_message     = $mInnen.message
        innen_uuids       = $mInnen.uuids
        innen_zeitstempel = $mInnen.zeitstempel

        nach_zeilen       = $mNach.zeilen
        nach_pfade        = $mNach.pfade
        nach_dienste      = $mNach.dienste
        nach_hosts        = $mNach.hosts
        nach_message      = $mNach.message
        nach_uuids        = $mNach.uuids
        nach_zeitstempel  = $mNach.zeitstempel
    })
}

$ergebnisse | Export-Csv -Path $Ausgabe -NoTypeInformation -Encoding utf8

Write-Output "Ausgewertet: $($ergebnisse.Count) Dateien"
Write-Output "Geschrieben: $Ausgabe"
Write-Output ""
# @(...) erzwingt ein Array. Ohne das liefert .Count bei GENAU EINEM Treffer
# unter Windows PowerShell 5.1 nichts, und die Zeile bleibt still leer — der
# Fehler faellt nur auf, wenn man ihn sucht.
Write-Output "Verdichtung (ausschliesslich Zaehlungen):"
Write-Output ("  mit vollstaendigem Markenpaar : {0} von {1}" -f
    @($ergebnisse | Where-Object { $_.paar_vollstaendig -eq 1 }).Count, $ergebnisse.Count)
Write-Output ("  mit abweichender Schreibweise : {0}" -f
    @($ergebnisse | Where-Object { $_.marke_abweichend -gt 0 }).Count)
Write-Output ("  Marke in echoter Zeile        : {0}" -f
    @($ergebnisse | Where-Object { $_.marke_in_echozeile -gt 0 }).Count)
Write-Output ("  ohne Startmarke               : {0}" -f
    @($ergebnisse | Where-Object { $_.start_marken -eq 0 }).Count)
Write-Output ("  Start ohne passende Endmarke  : {0}" -f
    @($ergebnisse | Where-Object { $_.start_marken -gt 0 -and $_.paar_vollstaendig -eq 0 }).Count)
Write-Output ("  Bytes gesamt / innen          : {0} / {1}" -f
    (($ergebnisse | Measure-Object bytes -Sum).Sum),
    (($ergebnisse | Measure-Object innen_bytes -Sum).Sum))
Write-Output ""
Write-Output "ZIP und Binaererkennung:"
Write-Output ("  mehr als ein ZIP-Eintrag      : {0}" -f
    @($ergebnisse | Where-Object { $_.zip_eintraege -gt 1 }).Count)
Write-Output ("  binaer                        : {0} von {1}" -f
    @($ergebnisse | Where-Object { $_.binaer -eq 1 }).Count, $ergebnisse.Count)
Write-Output ""
Write-Output "Kodierung -- nur unterscheidbare Dateien zaehlen:"
$unt = @($ergebnisse | Where-Object { $_.unterscheidbar -eq 1 })
Write-Output ("  mit Bytes ueber 0x7F          : {0} von {1}" -f $unt.Count, $ergebnisse.Count)
Write-Output ("  davon gueltiges UTF-8         : {0}" -f
    @($unt | Where-Object { $_.gueltiges_utf8 -eq 1 }).Count)
Write-Output ("  davon NICHT gueltiges UTF-8   : {0}   (dann ISO-8859-1 oder anderes)" -f
    @($unt | Where-Object { $_.gueltiges_utf8 -eq 0 }).Count)
Write-Output ("  reines ASCII, entscheidet nichts : {0}" -f
    ($ergebnisse.Count - $unt.Count))
