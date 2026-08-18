package mitschnitt;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Minimaler Lauscher fuer M70.
 *
 * Bindet auf 127.0.0.1 und einen freien Port, nimmt GENAU EINE Verbindung an,
 * schreibt die Rohbytes der Anfrage unveraendert in eine Datei und antwortet
 * mit einem minimalen SOAP-Umschlag, damit soapConnection.call(...) sauber
 * zurueckkehrt statt in eine Ausnahme zu laufen.
 *
 * Er schreibt nichts ins Netz und schlaegt nichts nach.
 *
 * Keine Umkodierung, keine Normalisierung von Zeilenenden: Was ankommt, wird
 * Byte fuer Byte weggeschrieben. Das ist der ganze Zweck der Messung.
 */
final class Lauscher implements AutoCloseable {

    private static final byte[] ANTWORT_RUMPF =
            ("<?xml version=\"1.0\" encoding=\"UTF-8\"?>"
             + "<SOAP-ENV:Envelope xmlns:SOAP-ENV=\"http://schemas.xmlsoap.org/soap/envelope/\">"
             + "<SOAP-ENV:Body/></SOAP-ENV:Envelope>").getBytes(StandardCharsets.UTF_8);

    private final ServerSocket serverSocket;
    private final Path ziel;
    private Thread thread;

    Lauscher(Path ziel) throws Exception {
        this.ziel = ziel;
        this.serverSocket = new ServerSocket();
        // Ausdruecklich 127.0.0.1, nicht 0.0.0.0. Port 0 = freier Port.
        this.serverSocket.bind(new InetSocketAddress(InetAddress.getByName("127.0.0.1"), 0));
    }

    int port() {
        return serverSocket.getLocalPort();
    }

    void starten() {
        thread = new Thread(this::annehmen, "lauscher");
        thread.setDaemon(true);
        thread.start();
    }

    void warten() throws InterruptedException {
        if (thread != null) {
            thread.join(15_000);
        }
    }

    private void annehmen() {
        try (Socket socket = serverSocket.accept()) {
            socket.setSoTimeout(1000);

            ByteArrayOutputStream roh = new ByteArrayOutputStream();
            InputStream in = socket.getInputStream();
            byte[] puffer = new byte[8192];
            try {
                int gelesen;
                while ((gelesen = in.read(puffer)) != -1) {
                    roh.write(puffer, 0, gelesen);
                }
            } catch (SocketTimeoutException erwartet) {
                // Der Client hat gesendet und wartet jetzt auf Antwort.
                // Das Zeitlimit ist hier das Ende der Anfrage, kein Fehler.
            }

            Files.createDirectories(ziel.getParent());
            Files.write(ziel, roh.toByteArray());
            System.out.println("Mitschnitt geschrieben: " + roh.size() + " Byte nach " + ziel);

            OutputStream out = socket.getOutputStream();
            String kopf = "HTTP/1.1 200 OK\r\n"
                        + "Content-Type: text/xml\r\n"
                        + "Content-Length: " + ANTWORT_RUMPF.length + "\r\n"
                        + "Connection: close\r\n"
                        + "\r\n";
            out.write(kopf.getBytes(StandardCharsets.ISO_8859_1));
            out.write(ANTWORT_RUMPF);
            out.flush();
        } catch (Exception ex) {
            System.out.println("Lauscher-Fehler: " + ex);
        }
    }

    @Override
    public void close() throws Exception {
        serverSocket.close();
    }
}
