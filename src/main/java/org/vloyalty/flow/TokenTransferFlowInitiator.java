package org.vloyalty.flow;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.crypto.SecureHash;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.node.services.AttachmentStorage;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import org.vloyalty.contract.TokenContract;
import org.vloyalty.state.TokenState;

import java.security.PublicKey;
import java.util.List;

@InitiatingFlow
@StartableByRPC
public class TokenTransferFlowInitiator extends AbstractTokenFlow { //FlowLogic<SignedTransaction> {
    private final Party _newOwner;
    private final int _numTokensToTxfr;

    //TODO: Common superclass, refactoring with TokenIssueFlow
    public TokenTransferFlowInitiator(Party newOwner, int amount) {
        _newOwner = newOwner;
        _numTokensToTxfr = amount;
    }

    /*private final ProgressTracker progressTracker = new ProgressTracker();

    @Override
    public ProgressTracker getProgressTracker() {
        return progressTracker;
    }*/

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {
        // We extract all the `TokenState`(s) from the vault.
        List<StateAndRef<TokenState>> tokenStateAndRefs = getServiceHub().getVaultService().queryBy(TokenState.class).getStates();

        Party currentOwner= getOurIdentity();

        // We find the `TokenState`(s) with the current owner (i.e. us!). NO amount bcos it is fungible! RM TODO
        StateAndRef<TokenState> inputTokenStateAndRef = tokenStateAndRefs
                .stream().filter(tokenStateAndRef -> {
                    TokenState tokenState = tokenStateAndRef.getState().getData();
                    return tokenState.getOwner().equals(currentOwner) /*&& artState.getTitle().equals(title)*/;
                }).findAny().orElseThrow(() -> new IllegalArgumentException("No Tokens found for owner=" + currentOwner));
        TokenState inputTokenState = inputTokenStateAndRef.getState().getData();

        // We throw an exception if the flow was not started by the token's current owner.
        if (!(getOurIdentity().equals(inputTokenState.getOwner())))
            throw new IllegalStateException(
                    "This flow (TokenTransferFlowInitiator) must be started by the current owner i.e. " + currentOwner +
                            ". Currenty being attempted by " + inputTokenState.getOwner());

        int tokensToOurselves= inputTokenState.getNumTokens() - _numTokensToTxfr;
        if (tokensToOurselves < 0) { //trying to transfer more than what we have
            String err= "This flow (TokenTransferFlowInitiator) is attempting to transfer more tokens ("
                    + _numTokensToTxfr + ") than what " + inputTokenState.getOwner() + " currently owns ("
                    + inputTokenState.getNumTokens() + ")";
            throw new IllegalArgumentException(err);
            //@TODO: Should use InsufficientBalanceException instead.
        }

        getProgressTracker().setCurrentStep(GENERATING_TXFR_INITIATOR_TRANSACTION);

        // We use the notary used by the input state.
        Party notary = inputTokenStateAndRef.getState().getNotary();

        // We build a transaction using a `TransactionBuilder`.
        TransactionBuilder txBuilder = new TransactionBuilder();

        // After creating the `TransactionBuilder`, we must specify which
        // notary it will use.
        txBuilder.setNotary(notary);

        // We add the input Token State to the transaction.
        txBuilder.addInputState(inputTokenStateAndRef);

        // We add the TWO output TokenStates to the transaction:
        //(i) The specified #tokens to the newOwner
        //(ii) The remaining tokens back to us!!
        /*TokenState outputTokenStateForNewOwner = new TokenState( //issuer, owner, amount
                currentOwner,
                _newOwner,
                _numTokensToTxfr);*/
        TokenState outputTokenStateForNewOwner= inputTokenState.withNewOwnerAndAmount(
                                                                    inputTokenState.getAmountFor(_numTokensToTxfr),
                                                                    _newOwner);
        txBuilder.addOutputState(outputTokenStateForNewOwner, TokenContract.ID);

        if (tokensToOurselves > 0) { //else dont bother creating this new state
            TokenState outputTokenStateForOurselves = inputTokenState.withNewAmount(
                    inputTokenState.getAmountFor(tokensToOurselves));
            txBuilder.addOutputState(outputTokenStateForOurselves, TokenContract.ID);
        }

        // We add the Transfer command to the transaction.
        // Note that we also specific who is required to sign the transaction.
        TokenContract.Commands.Transfer commandData = new TokenContract.Commands.Transfer();
        List<PublicKey> requiredSigners = ImmutableList.of(
                inputTokenState.getOwner().getOwningKey(), _newOwner.getOwningKey()); //BOTH sign.
        txBuilder.addCommand(commandData, requiredSigners);

        //Add a dummy attachment- done separately in TokenClientAttachmentRPC.java
        /*List<NetworkHostAndPort> ourOwnHostPortList= getServiceHub().getNetworkMapCache().getNodeByLegalIdentity(getOurIdentity()).getAddresses();
        String ourOwnHostPort= ourOwnHostPortList.get(0).toString();
        System.out.println("Getting Attachment from : " + getOurIdentity().getName() + " ] @ HostPort= " + ourOwnHostPort.toString()); //e.g. "localhost:10008"
        List<NetworkHostAndPort> hostPortList= getServiceHub().getNetworkMapCache().getNodeByLegalIdentity(_newOwner).getAddresses();
        String hostPort= hostPortList.get(0).toString();
        System.out.println("Sending to New Owner [ " + _newOwner.getName() + " ] @ HostPort= " + hostPort.toString()); //e.g. "localhost:10008"
        try {
            //Note that this will NOT work with JUnits since the server CorDapp is not running sp client cannot start...
            SecureHash attachmentHash= TokenClientAttachmentRPC.doAttachZipFile(hostPort, "C:\\Users\\rupal\\corda-attach.zip");
            txBuilder.addAttachment(attachmentHash);

            System.out.println("Attached corda-attach.zip ");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }*/
        //New : Attachments
        //Note : do before CollectSignaturesFlow below as per https://stackoverflow.com/questions/49798273/attachment-resolution-failure
        SecureHash attachmentHash= SecureHash.parse("46A5895F131FBA4A29DE6FAB301177BCA721A5AD4E70C11466A51AF62F3E6D20"); //TEMP!!
        //System.out.println("TokenFlowInitiator sending attachmt 46A5 to " + _newOwner);
        /*SignedTransaction attachedSignedTx = subFlow(
                new TokenAttachmentSender(_newOwner, attachmentHash));*/
        //txBuilder.addAttachment(attachmentHash);
        //AttachmentStorage as= getServiceHub().getAttachments();

        // We check that the transaction builder we've created meets the
        // contracts of the input and output states.
        getProgressTracker().setCurrentStep(VERIFYING_TRANSACTION);
        txBuilder.verify(getServiceHub());

        //txBuilder.addAttachment(attachmentHash); @TODO: Needs more work. Throws Attachment-exception in #verify.
        AttachmentStorage as= getServiceHub().getAttachments();

        // We finalise the transaction builder by signing it,
        // converting it into a `SignedTransaction`.
        getProgressTracker().setCurrentStep(SIGNING_TRANSACTION);
        SignedTransaction partlySignedTx = getServiceHub().signInitialTransaction(txBuilder);

        // We use `CollectSignaturesFlow` to automatically gather a
        // signature from each counterparty. The counterparty will need to
        // call `SignTransactionFlow` to decided whether or not to sign.
        getProgressTracker().setCurrentStep(GATHERING_SIGS);
        FlowSession ownerSession = initiateFlow(_newOwner);
        SignedTransaction fullySignedTx = subFlow(
                new CollectSignaturesFlow(partlySignedTx, ImmutableSet.of(ownerSession)));

        // We use `FinalityFlow` to automatically notarise the transaction
        // and have it recorded by all the `participants` of all the
        // transaction's states.
        getProgressTracker().setCurrentStep(FINALISING_TRANSACTION);
        return subFlow(new FinalityFlow(fullySignedTx));

        //return null;
    }
}