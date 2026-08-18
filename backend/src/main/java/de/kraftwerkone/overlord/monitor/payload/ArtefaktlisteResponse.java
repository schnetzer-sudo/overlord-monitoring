package de.kraftwerkone.overlord.monitor.payload;

import java.util.List;

/**
 * Die Artefakte einer Nachricht, <b>zweigeteilt</b> ({@code docs/rohdaten.md} §5, Entscheidung 6).
 *
 * <p>Die Aufteilung steht hier und nicht in der Oberflaeche, weil sie aus dem Datenmodell folgt und
 * nicht aus einer Gestaltungsentscheidung: {@code Message.Payload.GUID} haengt auf Schritt {@code
 * 0}, dem Ort der <i>Metadaten</i> — das ist kein Ablaufschritt (M57, M17 (3)). Sie in die
 * Schrittfolge zu legen waere schlicht falsch.
 *
 * <p>Jede Nachricht traegt <b>3 bis 15</b> Artefakte und mindestens ein Protokoll, bei jedem
 * Mandanten (M55). Ein leerer Kasten ist deshalb kein erwarteter Zustand — kommt er vor, ist er ein
 * Befund.
 *
 * @param messageId die Nachricht, zu der die Liste gehoert
 * @param eingang die eingegangene Datei ({@code Message.Payload.GUID}), oder {@code null}, wenn die
 *     Nachricht keine traegt. In den gemessenen Bestaenden traegt sie jede Nachricht (M17), die
 *     Tabelle erzwingt es aber nicht
 * @param nutzdaten die umgewandelten Fassungen, nach Schritt geordnet. <b>Ohne</b> den Eingang
 * @param protokolle die Protokolle je Schritt, nach Schritt geordnet
 */
public record ArtefaktlisteResponse(
    String messageId,
    ArtefaktResponse eingang,
    List<ArtefaktResponse> nutzdaten,
    List<ArtefaktResponse> protokolle) {}
