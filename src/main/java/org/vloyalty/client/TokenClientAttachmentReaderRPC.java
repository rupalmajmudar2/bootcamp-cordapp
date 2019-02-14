package org.vloyalty.client;

import net.corda.client.rpc.CordaRPCClient;
import net.corda.client.rpc.CordaRPCClientConfiguration;
import net.corda.core.crypto.SecureHash;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.messaging.DataFeed;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.utilities.NetworkHostAndPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import rx.Observable;

import java.util.List;

//https://stackoverflow.com/questions/47657566/attachment-in-corda?rq=1
public class TokenClientAttachmentReaderRPC {
    private static final Logger logger = LoggerFactory.getLogger(TokenClientAttachmentReaderRPC.class);

    public static void main(String[] args) throws Exception {
        if (args.length != 2) {
            throw new IllegalArgumentException("Usage: TokenClientAttachmentReaderRPC <hostPort, expected pdf filename>");
        }

        final NetworkHostAndPort nodeAddress = NetworkHostAndPort.parse(args[0]);
        System.out.println("Attachment Client running at: " + nodeAddress.toString());
        final CordaRPCClient client = new CordaRPCClient(nodeAddress, CordaRPCClientConfiguration.DEFAULT);
        final CordaRPCOps proxy = client.start("user1", "test").getProxy();

        openAttachedZipFile(proxy);
    }

    //So that this can be used from the flows. @see TokenApi#sendAttachment
    public static void openAttachedZipFile(CordaRPCOps proxy) throws Exception {
        SecureHash sh = SecureHash.parse("46A5895F131FBA4A29DE6FAB301177BCA721A5AD4E70C11466A51AF62F3E6D20");
        boolean foundAttachment = proxy.attachmentExists(sh);

        proxy.internalVerifiedTransactionsFeed();
        DataFeed<List<SignedTransaction>, SignedTransaction> txnsFeed = proxy.internalVerifiedTransactionsFeed();
        Observable<SignedTransaction> futureTxns = txnsFeed.getUpdates();
        futureTxns.toBlocking().subscribe(
                nextTxn -> {
                    try {
                        TokenClientAttachmentReaderRPC.logFeedTxn(nextTxn, proxy);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
        );
    }

    private static void logFeedTxn(SignedTransaction nextTxn, CordaRPCOps proxy) throws Exception {
        List<SecureHash> attachments= nextTxn.getTx().getAttachments();
        System.out.println("NODE: " + nextTxn.getId() + " Attachment size=" + attachments.size());
        for(SecureHash attachment: attachments) {
            System.out.println("Hash= " + attachment);
            TokenClientAttachmentRPC.downloadAttachment(proxy, attachment);
        }
    }
}
