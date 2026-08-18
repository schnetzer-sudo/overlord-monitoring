package de.kraftwerkone.filestore.client;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.zip.ZipInputStream;

public class PayloadReader {

    private BufferedReader reader = null;
    private fileMode fileMode = null;

    public enum fileMode {

        ZIPPED, UNZIPPED
    };

    public PayloadReader(String filename, fileMode fileMode) throws Exception {
        this.fileMode = fileMode;
        
        switch ( this.fileMode) {
            case ZIPPED:
                ZipInputStream payloadZipFileInputStream = new ZipInputStream(new FileInputStream(filename));
                payloadZipFileInputStream.getNextEntry();

                reader = new BufferedReader(new InputStreamReader(payloadZipFileInputStream, "8859_1"));
                break;
            case UNZIPPED:
                reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "8859_1"));
                break;
        }
    }

    public String read() throws Exception {
        return (String) reader.readLine();
    }

    public String[] readArray(String delimiter) throws Exception {
        String line = read();
        if (line == null) {
            return null;
        } else {
            return line.split(delimiter);
        }
    }

    public void close() throws Exception {
        reader.close();
    }
}
