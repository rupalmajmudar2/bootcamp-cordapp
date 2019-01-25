package org.vloyalty.contract;

import net.corda.core.contracts.Attachment;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.contracts.ContractState;
import net.corda.core.transactions.LedgerTransaction;
import org.jetbrains.annotations.NotNull;
import org.vloyalty.state.TokenAttachmentState;

import java.util.List;

/* Our contract, governing how our state will evolve over time.
 * See src/main/java/examples/ArtContract.java for an example. */
public class TokenAttachmentContract implements Contract {
    public static String ID = "org.vloyalty.contract.TokenAttachmentContract";

    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {
        System.out.println("In TokenAttachmentContract#verify for Cmd=" + tx.getCommand(0));

        if (tx.getOutputStates().size() != 1)
            throw new IllegalArgumentException(("Txn outputs should be 1. Found: " + tx.getOutputStates().size()));

        ContractState cs = tx.getOutput(0);
        if (!(cs instanceof TokenAttachmentState))
            throw new IllegalArgumentException("Tx output must be a TokenAttachmentState. Found: " + cs.getClass());
        TokenAttachmentState state= (TokenAttachmentState) cs;

        // we check that at least one has the matching hash, the other will be the contract
        List<Attachment> attachments= tx.getAttachments();
        boolean foundAttachment= false;
        System.out.println("AttachmentContract found #attchmts=" + attachments.size() + " . TBD: FIXME TODO!!");
        /*for (Attachment attachment: attachments) {
            if ( attachment.getId() == state.getAttachmentHash() ) {
                foundAttachment= true;
                break;
            }
        }
        if (!foundAttachment) {
            throw new IllegalArgumentException("Could not find our attachment with Hash=" + state.getAttachmentHash());
        }*/
    }

    public static class Attach implements CommandData{

    }
}