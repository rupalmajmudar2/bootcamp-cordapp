package org.vloyalty.client;

import net.corda.client.rpc.CordaRPCClient;
import net.corda.client.rpc.CordaRPCClientConfiguration;
import net.corda.core.crypto.SecureHash;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.utilities.NetworkHostAndPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;

//https://stackoverflow.com/questions/47657566/attachment-in-corda?rq=1
public class TokenClientAttachmentRPC {
    private static final Logger logger = LoggerFactory.getLogger(TokenClientAttachmentRPC.class);

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: TokenClientAttachmentRPC <hostPort, pdf filename>");
        }

        SecureHash hash = doAttachZipFile(args[0], args[1]);
    }

    public static SecureHash doAttachZipFile(String hostPortString, String fileName) throws FileNotFoundException, Exception {
        final NetworkHostAndPort nodeAddress = NetworkHostAndPort.parse(hostPortString);
        System.out.println("Attachment Client running at: " + nodeAddress.toString());
        final CordaRPCClient client = new CordaRPCClient(nodeAddress, CordaRPCClientConfiguration.DEFAULT);
        final CordaRPCOps proxy = client.start("user1", "test").getProxy();

        return doAttachZipFile(proxy, fileName);

        /*File attachmentFile = new File(fileName);
        FileInputStream attachmentInputStream = new FileInputStream(attachmentFile);
        SecureHash attachmentHash = proxy.uploadAttachment(attachmentInputStream);
        System.out.println("Done attaching pdf - AtachmentHash" + attachmentHash);

        return attachmentHash;*/
    }

    //So that this can be used from the flows. @see TokenApi#sendAttachment
    public static SecureHash doAttachZipFile(CordaRPCOps proxy, String fileName) throws FileNotFoundException, Exception {
        File attachmentFile = new File(fileName);
        FileInputStream attachmentInputStream = new FileInputStream(attachmentFile);

        SecureHash attachmentHash= null;
        try {
            attachmentHash = proxy.uploadAttachment(attachmentInputStream);
        }
        catch (Exception e) {
            System.out.println(" Exception: " + e.getClass() + " Msg: " + e.getMessage());
            attachmentHash= SecureHash.parse(e.getMessage()); //TEMP!! @TODO : cleanup
            //The msg is the hash of the file.
            //The other way would be to check for an attachment before starting,
            //as shown in https://docs.corda.net/tutorial-attachments.html.
        }

        return attachmentHash;
    }

    public static void downloadAttachment(CordaRPCOps proxy, SecureHash attachmentHash) throws Exception {
        InputStream is = proxy.openAttachment(attachmentHash);
        JarInputStream attachmentJar = new JarInputStream(is);
        ZipEntry nextDoc= null;
        while ((nextDoc= attachmentJar.getNextEntry()) != null) {
            System.out.println("#downloadAttachment : Hash#"+ attachmentHash.toString() + " : Found File=" + nextDoc.getName());
        }
        System.out.println("Done reading pdf");
    }
}
