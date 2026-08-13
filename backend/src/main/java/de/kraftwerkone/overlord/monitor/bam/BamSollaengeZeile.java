package de.kraftwerkone.overlord.monitor.bam;

/**
 * Eine Zeile aus {@code overlord_monitor.bam_sollaenge} — die Kuratierung aus Teil 2a, für
 * <b>einen</b> Mandanten gelesen.
 *
 * <p>Vollständig beschrieben in {@code docs/bam-sollaengen.md}. Hier stehen nur die drei Angaben,
 * aus denen die Suchvarianten entstehen; Dominanz, Zeilenzahl und Messdatum sind Herkunftsnachweis
 * und gehen die Suche nichts an.
 *
 * @param typ {@code MessageBAM.MessageBAMType}, {@code smallint(6)} (M46‑0)
 * @param sollaenge die Länge, auf die ein Wert dieses Paares mit Nullen aufgefüllt wird — <b>{@code
 *     null}-fähig</b>. Zwei der sechzehn Zeilen tragen ausschließlich das Leerzeichen-Kennzeichen
 *     und keine Sollänge ({@code NEXANS}/9018 und {@code NEXANS}/9020, Längendominanz 43,61 % und
 *     82,31 %). <b>Das ist kein Fehlerfall</b>: Für ein solches Paar wird nur roh gesucht.
 *     <p>Der generierte Typ ist {@code org.jooq.types.UByte} — der Preis für {@code TINYINT
 *     UNSIGNED}; die Zahl kommt über {@code .intValue()} und steht hier bereits als {@code
 *     Integer}.
 * @param fuehrendesLeerzeichen ob dieses Paar im Bestand Werte mit einem <b>führenden</b>
 *     Leerzeichen trägt. Folgende brauchen kein Kennzeichen: {@code utf8mb4_general_ci} ist PAD
 *     SPACE, {@code '123 ' = '123'} ist wahr und {@code ' 123' = '123'} falsch (M43‑3)
 */
public record BamSollaengeZeile(short typ, Integer sollaenge, boolean fuehrendesLeerzeichen) {}
