package de.kraftwerkone.overlord.monitor.bam;

/**
 * Ein Eintrag der <b>Feld-Gruppe</b> des Suchangebots: ein Feldname aus {@code
 * MessagePropertySearchListEntry}.
 *
 * <p><b>Der Name steht unverändert da — technisch, ohne Beschriftung, ohne Übersetzung</b> (E‑105).
 * Die Tabelle hat keine Beschreibungsspalte (M153), und eine Kuratierung dafür entsteht nicht. Wer
 * {@code Message.SNDPRN} nicht versteht, sieht {@code Message.SNDPRN}.
 *
 * @param quelle immer {@link SuchfelderResponse#QUELLE_FELD}
 * @param name {@code MessagePropertyName}, wie konfiguriert — zugleich der erste Teil des
 *     Suchparameters {@code feld=<name>:<wert>}
 * @param spalte {@code true}, wenn der Name eine <b>Spalte</b> benennt (Typ 0, {@link Typ0Feld})
 *     und die Suche dafür ein Spaltenprädikat baut; {@code false} für eine Zeile in {@code
 *     MessageProperty} (Typ 1). Die Unterscheidung steht in der Antwort, weil sie die Bedeutung des
 *     Werts verändert: {@code Message.Status} vergleicht gegen den Rohstatus der Nachricht, {@code
 *     Message.GUID} gegen eine Eigenschaftszeile
 */
public record SuchfeldFeldResponse(String quelle, String name, boolean spalte) {}
