package de.kraftwerkone.filestore.client;

import java.io.IOException;
import java.net.URL;
import java.util.Iterator;
import java.util.LinkedList;

import javax.activation.DataHandler;
import javax.xml.namespace.QName;
import javax.xml.soap.*;

public class FilestoreClient {

    private URL filestoreURL;
    private String filestoreRetrieveDir;
    private SOAPConnectionFactory soapConnectionFactory;
    private SOAPConnection soapConnection;
    private MessageFactory messageFactory;
    private SOAPMessage soapMessageRequest;
    private SOAPMessage soapMessageResponse;
    private LinkedList<FilestoreFile> filestoreFiles;

    public FilestoreClient(URL filestoreURL, String filestoreRetrieveDir) {

        try {
            filestoreFiles = new LinkedList<>();
            soapConnectionFactory = SOAPConnectionFactory.newInstance();
            soapConnection = soapConnectionFactory.createConnection();
            messageFactory = MessageFactory.newInstance();
            soapMessageRequest = messageFactory.createMessage();
            SOAPHeader soapHeader = soapMessageRequest.getSOAPHeader();
            soapHeader.detachNode();
        } catch (SOAPException ex) {
            System.out.println(ex.getMessage());
        }
        this.filestoreURL = filestoreURL;
        this.filestoreRetrieveDir = filestoreRetrieveDir;
    }

    public void clearFilestoreClient() {
        this.filestoreFiles.clear();
    }

    public LinkedList<FilestoreFile> getFilestoreFiles() {
        return filestoreFiles;
    }

    public URL getFilestoreURL() {
        return filestoreURL;
    }

    public String getFilestoreRetrieveDir() {
        return filestoreRetrieveDir;
    }

    public void addFileStoreAction(FilestoreFile filestoreFile) {
        this.filestoreFiles.add(filestoreFile);
    }

    public boolean requestFilestoreActions() {
        return requestFilestoreActions(this.filestoreFiles);
    }

    @SuppressWarnings("incomplete-switch")
	public boolean requestFilestoreActions(LinkedList<FilestoreFile> filestoreFiles) {
        try {
            SOAPBody requestSoapBody = soapMessageRequest.getSOAPBody();
            SOAPBodyElement requestSoapBodyElement = requestSoapBody.addBodyElement(new QName("http://filestore.kraftwerkone.de", "FileList", "m"));
            Iterator<FilestoreFile> filestoreFilesIterator = filestoreFiles.iterator();
            int filestoreFilesCounter = 0;
            while (filestoreFilesIterator.hasNext()) {
                FilestoreFile filestoreFilesNext = (FilestoreFile) filestoreFilesIterator.next();
                SOAPElement requestSoapElement = requestSoapBodyElement.addChildElement(new QName("File"));
                requestSoapElement.addAttribute(new QName("Counter"), Integer.toString(filestoreFilesCounter));
                requestSoapElement.addAttribute(new QName("ID"), filestoreFilesNext.getId());
                requestSoapElement.addAttribute(new QName("Action"), filestoreFilesNext.getAction().toString());

                switch (filestoreFilesNext.getAction()) {
                    case CREATE:
                        DataHandler dataHandler = new DataHandler(filestoreFilesNext.getFileURL());
                        AttachmentPart attachmentPart = soapMessageRequest.createAttachmentPart(dataHandler);
                        attachmentPart.setContentType("application/octet-stream");
                        attachmentPart.setContentId(Integer.toString(filestoreFilesCounter));
                        soapMessageRequest.addAttachmentPart(attachmentPart);
                        break;
                    case RETRIEVE:
                        break;
                    case DELETE:
                        break;
                }
                filestoreFilesCounter++;
            }

            soapMessageResponse = soapConnection.call(soapMessageRequest, filestoreURL);
            SOAPBody responseSoapBody = soapMessageResponse.getSOAPBody();
            Iterator<?> responseSoapBodyIterator = responseSoapBody.getChildElements(new QName("http://filestore.kraftwerkone.de", "FileList", "m"));
            while (responseSoapBodyIterator.hasNext()) {
                Iterator<?> responseSOAPBodyElementIterator = ((SOAPBodyElement) responseSoapBodyIterator.next()).getChildElements(new QName("File"));
                while (responseSOAPBodyElementIterator.hasNext()) {
                    SOAPBodyElement soapBodyElement = ((SOAPBodyElement) responseSOAPBodyElementIterator.next());
                    filestoreFiles.get(Integer.parseInt(soapBodyElement.getAttribute("Counter"))).setId(soapBodyElement.getAttribute("ID"));
                    filestoreFiles.get(Integer.parseInt(soapBodyElement.getAttribute("Counter"))).setResponse(soapBodyElement.getAttribute("Response"));
                }
            }

            Iterator<?> attachmentIterator = soapMessageResponse.getAttachments();
            while (attachmentIterator.hasNext()) {
                AttachmentPart attachmentPart = (AttachmentPart) attachmentIterator.next();
                byte[] fileContentByteArray = new byte[((java.io.ByteArrayInputStream) attachmentPart.getContent()).available()];
                ((java.io.ByteArrayInputStream) attachmentPart.getContent()).read(fileContentByteArray, 0, ((java.io.ByteArrayInputStream) attachmentPart.getContent()).available());
                java.io.FileOutputStream fileOut = new java.io.FileOutputStream(filestoreRetrieveDir + filestoreFiles.get(Integer.parseInt(attachmentPart.getContentId())).getMessageUUID() + ".zip");
                fileOut.write(fileContentByteArray);
                fileOut.close();
            }
            return true;
        } catch (SOAPException | NumberFormatException | IOException ex) {
            System.out.println(ex.getMessage());
            return false;
        }
    }
}
