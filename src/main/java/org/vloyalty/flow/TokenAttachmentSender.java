package org.vloyalty.flow;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import org.vloyalty.contract.TokenAttachmentContract;
import org.vloyalty.state.TokenAttachmentState;

import java.security.PublicKey;

@InitiatingFlow
@StartableByRPC
public class TokenAttachmentSender extends AbstractTokenFlow {
    private final Party _newOwner;
    private final SecureHash _attachmentHash;

    public TokenAttachmentSender(Party newOwner, SecureHash attachmentHash) {
        System.out.println("Constructing TokenAttachmentSender hash= " + attachmentHash);
        _newOwner = newOwner;
        _attachmentHash = attachmentHash;
    }

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {
        super.call();
        // Create a trivial transaction with an output that describes the attachment, and the attachment itself

        TransactionBuilder txBuilder = new TransactionBuilder();
        //Party notary = inputTokenStateAndRef.getState().getNotary();
        Party notary= getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
        System.out.println("TokenAttachmentSender Notary=" + notary.getName());
        txBuilder.setNotary(notary);

        TokenAttachmentState attachState = new TokenAttachmentState(_attachmentHash, _newOwner);
        PublicKey ourPk = getOurIdentity().getOwningKey();
        txBuilder
                .addOutputState(attachState, TokenAttachmentContract.ID)
                .addCommand(new TokenAttachmentContract.Commands.Attach(), ourPk)
                .addAttachment(_attachmentHash);

        System.out.println("TokenAttachmentSender Before Verify");
        txBuilder.verify(getServiceHub());
        System.out.println("TokenAttachmentSender After Verify");

        getProgressTracker().setCurrentStep(SIGNING_TRANSACTION);
        SignedTransaction signedTransaction = getServiceHub().signInitialTransaction(txBuilder);

        // We get the transaction notarised and recorded automatically by the platform.
        getProgressTracker().setCurrentStep(FINALISING_TRANSACTION);
        return subFlow(new FinalityFlow(signedTransaction));
        /*SignedTransaction signedTransaction = getServiceHub().signInitialTransaction(txBuilder);

        getProgressTracker().setCurrentStep(FINALISING_TRANSACTION);
        FlowSession ownerSession = initiateFlow(_newOwner);
        //SignedTransaction fullySignedTx = subFlow(
        //        new CollectSignaturesFlow(signedTransaction, ImmutableSet.of(ownerSession)));


        // We use `FinalityFlow` to automatically notarise the transaction
        // and have it recorded by all the `participants` of all the
        // transaction's states.
        getProgressTracker().setCurrentStep(FINALISING_TRANSACTION);
        return subFlow(new FinalityFlow(signedTransaction));*/
    }
}