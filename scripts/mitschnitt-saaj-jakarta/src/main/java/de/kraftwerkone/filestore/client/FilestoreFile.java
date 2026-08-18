package de.kraftwerkone.filestore.client;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.util.zip.Adler32;
import java.util.zip.CheckedOutputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class FilestoreFile {

    static final long MAXSIZE_COMPRESS = 0;

    public enum filestoreAction {

        CREATE, APPEND, DELETE, RETRIEVE
    }
    private String id;
    private URL fileURL;
    private filestoreAction action;
    private String messageUUID;
    private String response;

    public FilestoreFile(String id, URL fileURL, filestoreAction action, String messageUUID) {
       this.id = id;
       this.fileURL = fileURL;
       this.action = action;
       this.messageUUID = messageUUID;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public URL getFileURL() {
        return fileURL;
    }

    public void setFileURL(URL fileURL) {
        this.fileURL = fileURL;
    }

    public filestoreAction getAction() {
        return action;
    }

    public void setAction(filestoreAction action) {
        this.action = action;
    }

    public String getMessageUUID() {
        return messageUUID;
    }

    public void setMessageUUID(String messageUUID) {
        this.messageUUID = messageUUID;
    }

    public String getResponse() {
        return response;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public void prepareFile() {
        int BUFFER = 1048576;
        
        if (this.fileURL != null) {
            try {
                File file = new File(this.fileURL.toURI());
                String filePath = file.getPath();
                String fileName = file.getName();
                if (file.length() >= MAXSIZE_COMPRESS) {
                    FileOutputStream fileOutputStream = new FileOutputStream(filePath + ".zip");
                    CheckedOutputStream checkedOutputStream = new CheckedOutputStream(fileOutputStream, new Adler32());
                    ZipOutputStream zipOutputStream = new ZipOutputStream(new BufferedOutputStream(checkedOutputStream));
                    byte data[] = new byte[BUFFER];

                    FileInputStream fileInputStream = new FileInputStream(filePath);
                    BufferedInputStream bufferedInputStream = new BufferedInputStream(fileInputStream, BUFFER);

                    ZipEntry zipEntry = new ZipEntry(fileName);
                    zipOutputStream.putNextEntry(zipEntry);
                    int bytesRead;
                    while ((bytesRead = bufferedInputStream.read(data, 0, BUFFER)) != -1) {
                        zipOutputStream.write(data, 0, bytesRead);
                    }
                    bufferedInputStream.close();
                    fileInputStream.close();
                    zipOutputStream.close();
                    checkedOutputStream.close();
                    fileOutputStream.close();

                    setFileURL(new java.net.URL("file:///" + filePath.replaceAll(" ", "%20") + ".zip"));
                    Files.delete(file.toPath());
                }
            } catch (IOException | URISyntaxException ex) {
                System.out.println(ex.getMessage());
            }
        }
    }
}
