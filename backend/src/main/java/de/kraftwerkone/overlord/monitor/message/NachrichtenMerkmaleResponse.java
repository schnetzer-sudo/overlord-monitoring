package de.kraftwerkone.overlord.monitor.message;

/**
 * Was der Bestand des aktiven Mandanten hergibt — <b>Stammdaten der Ansicht, nicht Inhalt einer
 * Seite.</b>
 *
 * <p>Die Oberflaeche entscheidet daran, welche Bedienelemente sie ueberhaupt anbietet. Ein
 * Schalter, der etwas ausblendet, das es beim eigenen Mandanten gar nicht gibt, kuendigt eine
 * Wirkung an, die ausbleibt — und ein Bedienelement ohne Wirkung ist schlimmer als keins.
 *
 * <p><b>Warum eigener Endpunkt und nicht ein Feld an jeder Seite.</b> Der Wert aendert sich selten
 * (er beschreibt den Gesamtbestand, nicht das Zeitfenster), waehrend die Liste sich unter
 * Umstaenden jede Minute aktualisiert. An jeder Seite haenge er an einer Abfrage, die ihn jedes Mal
 * neu ermitteln muesste — und genau das verbietet Regel L2.
 *
 * @param zwischenschritteVorhanden ob beim aktiven Mandanten ueberhaupt {@code SPLITTED}- oder
 *     {@code MERGED}-Zeilen vorkommen. Messung M12: Fuenf von neun Mandanten mit Nachrichten haben
 *     ueber den gesamten Bestand nicht eine einzige — zusammen 112.801 Nachrichten. Bei {@code
 *     NEXANS} sind es 39,6 Prozent.
 */
public record NachrichtenMerkmaleResponse(boolean zwischenschritteVorhanden) {}
