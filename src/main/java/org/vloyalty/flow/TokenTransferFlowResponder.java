package org.vloyalty.flow;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowSession;
import net.corda.core.flows.InitiatedBy;
import net.corda.core.flows.SignTransactionFlow;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.utilities.ProgressTracker;

import javax.swing.*;
import java.util.List;

// `InitiatedBy` means that we will start this flow in response to a
// message from `TokenTransferFlow.Initiator`.
@InitiatedBy(TokenTransferFlowInitiator.class)
public class TokenTransferFlowResponder extends AbstractTokenFlow { //FlowLogic<SignedTransaction> {
    private final FlowSession counterpartySession;

    // Responder flows always have a single constructor argument - a
    // `FlowSession` with the counterparty who initiated the flow.
    public TokenTransferFlowResponder(FlowSession counterpartySession) {
        this.counterpartySession = counterpartySession;
    }

    /*private final ProgressTracker progressTracker = new ProgressTracker();

    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }*/

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {
        // If the counterparty requests our signature on a transaction using
        // `CollectSignaturesFlow`, we need to respond by invoking our own
        // `SignTransactionFlow` subclass.
        class TokenTransferSignTxFlow extends SignTransactionFlow {
            private TokenTransferSignTxFlow(FlowSession otherSession, ProgressTracker progressTracker) {
                super(otherSession, progressTracker);
            }

            // As part of `SignTransactionFlow`, the contracts of the
            // transaction's input and output states are run automatically.
            // Inside `checkTransaction`, we define our own additional logic
            // for checking the received transaction. If `checkTransaction`
            // throws an exception, we'll refuse to sign.
            @Override
            protected void checkTransaction(SignedTransaction stx) throws FlowException {
                // Whatever checking you want to do...
                //TODO - add checks
                //List<ContractState> outputStates= stx.getTx().getOutputStates();
                /*
                String msg=""; int i= 0;
                for (ContractState cs: outputStates) {
                    msg += "#" + i++ + " : " + cs.toString() + " ppl: " + cs.getParticipants().toString();
                }
                JOptionPane.showInputDialog(msg);
                */
                Party thisNode= getOurIdentity();
                if (!(thisNode.getName().getOrganisation().equals("Customer"))) { //Dont ask the customer to confirm.
                    JDialog.setDefaultLookAndFeelDecorated(true);
                    int response = JOptionPane.showConfirmDialog(null, "Do you wish to Sign this txn?", thisNode.getName().getOrganisation() + " to confirm:",
                            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                    if (response == JOptionPane.NO_OPTION) {
                        System.out.println("No button clicked");
                        throw new FlowException("Counterparty refused to sign. Cancelling the Transaction.");
                    } else if (response == JOptionPane.YES_OPTION) {
                        System.out.println("Yes button clicked");
                        //ok.
                    } else if (response == JOptionPane.CLOSED_OPTION) {
                        System.out.println("JOptionPane closed");
                        //ok.
                    }
                }
                //stx.getInputs();

                List<SecureHash> attachments= stx.getTx().getAttachments();
                System.out.println("Txn#: " + stx.getId() + " Attachment size=" + attachments.size());
                for(SecureHash attachment: attachments) {
                    System.out.println("Hash= " + attachment);
                }
            }
        }

        return subFlow(new TokenTransferSignTxFlow(counterpartySession, SignTransactionFlow.tracker()));
    }
}