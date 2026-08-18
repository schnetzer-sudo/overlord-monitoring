package de.kraftwerkone.filestore.client;

import java.io.BufferedWriter;

public class PayloadWriter {

    private BufferedWriter bufferedWriter = null;

    public PayloadWriter(String filename, String coding) throws Exception {
        bufferedWriter = new BufferedWriter(new java.io.OutputStreamWriter(new java.io.FileOutputStream(filename), coding));
    }

    public void write(String buffer) throws Exception {
        bufferedWriter.write(buffer);

    }

    public void close() throws Exception {
        bufferedWriter.close();

    }

    public void flush() throws Exception {
        bufferedWriter.flush();

    }
}
