<#
    Selbstpruefung des Auswertungsskripts.

    Ergaenzt am 17.08.2026 um den Entpackschritt: Was der Filestore liefert,
    ist ein ZIP (Q4). Die drei erfundenen Testdateien werden deshalb zusaetzlich
    VERPACKT geprueft - die Erkennung muss weiterhin greifen, insbesondere der
    eingeschleuste Fall in 3.log.

    Vierter Fall neu: ein ZIP mit ZWEI Eintraegen. Das Altsystem liest nur den
    ersten (JsonServlet.java:801-802); die Auswertung muss es genauso machen und
    die Zahl der Eintraege trotzdem melden.

    Die Testdateien sind und bleiben FREI ERFUNDEN. Kein echter Inhalt.

    Bewusst reines ASCII: Windows PowerShell 5.1 liest .ps1 ohne BOM als ANSI.
#>

[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$hier  = $PSScriptRoot
$quell = Join-Path $hier 'testdaten'
$zipdir = Join-Path $hier 'testdaten-zip'

Add-Type -AssemblyName System.IO.Compression.FileSystem

if (Test-Path $zipdir) { Remove-Item -Recurse -Force $zipdir }
New-Item -ItemType Directory -Force -Path $zipdir | Out-Null

# 1.zip, 2.zip, 3.zip - je ein Eintrag, aus den erfundenen Protokolldateien
foreach ($n in 1, 2, 3) {
    $zip = [System.IO.Compression.ZipFile]::Open((Join-Path $zipdir "$n.zip"), 'Create')
    try {
        [void] [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile(
            $zip, (Join-Path $quell "$n.log"), "erfunden-$n.log")
    } finally { $zip.Dispose() }
}

# 4.zip - ZWEI Eintraege. Erster ist 1.log (vollstaendiges Paar), zweiter ist
# 2.log. Ausgewertet werden darf nur der erste.
$zip = [System.IO.Compression.ZipFile]::Open((Join-Path $zipdir '4.zip'), 'Create')
try {
    [void] [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip, (Join-Path $quell '1.log'), 'erster.log')
    [void] [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip, (Join-Path $quell '2.log'), 'zweiter.log')
} finally { $zip.Dispose() }

Write-Output "=== unverpackt (wie bisher) ==="
& (Join-Path $hier 'auswertung.ps1') -Arbeitsverzeichnis $quell `
    -Ausgabe (Join-Path $hier 'ergebnis\selbstpruefung-roh.csv')

Write-Output ""
Write-Output "=== verpackt (neuer Entpackschritt) ==="
& (Join-Path $hier 'auswertung.ps1') -Arbeitsverzeichnis $zipdir `
    -Ausgabe (Join-Path $hier 'ergebnis\selbstpruefung-zip.csv')

Write-Output ""
Write-Output "=== Vergleich: verpackt muss dieselben Marken finden wie unverpackt ==="
$roh = Import-Csv (Join-Path $hier 'ergebnis\selbstpruefung-roh.csv')
$zp  = Import-Csv (Join-Path $hier 'ergebnis\selbstpruefung-zip.csv')
foreach ($n in 1, 2, 3) {
    $a = $roh | Where-Object { $_.nr -eq $n }
    $b = $zp  | Where-Object { $_.nr -eq $n }
    $gleich = ($a.start_marken -eq $b.start_marken) -and ($a.end_marken -eq $b.end_marken) -and
              ($a.paar_vollstaendig -eq $b.paar_vollstaendig) -and ($a.innen_zeilen -eq $b.innen_zeilen) -and
              ($a.marke_abweichend -eq $b.marke_abweichend) -and ($a.marke_in_echozeile -eq $b.marke_in_echozeile)
    Write-Output ("  Datei {0}: Erkennung gleich = {1}   (Start {2}/{3}, Paar {4}, innen {5} Zeilen, Eintraege {6})" -f
        $n, $gleich, $b.start_marken, $b.end_marken, $b.paar_vollstaendig, $b.innen_zeilen, $b.zip_eintraege)
}
$v = $zp | Where-Object { $_.nr -eq 4 }
$e = $zp | Where-Object { $_.nr -eq 1 }
Write-Output ("  Datei 4: Eintraege {0}, ausgewertet wie Datei 1 = {1}" -f
    $v.zip_eintraege, (($v.innen_zeilen -eq $e.innen_zeilen) -and ($v.bytes -eq $e.bytes)))

Write-Output ""
Write-Output "=== G1-Gegenprobe: Inhalt in der CSV? ==="
$csv = Get-Content (Join-Path $hier 'ergebnis\selbstpruefung-zip.csv') -Raw
foreach ($w in 'erfunden', 'Erfundene', 'Program', 'StartOfLog', 'EndOfLog', 'Message.', 'opt', 'invalid', 'PROD') {
    Write-Output ("  enthaelt '{0}': {1}" -f $w, $csv.Contains($w))
}

Remove-Item -Recurse -Force $zipdir
