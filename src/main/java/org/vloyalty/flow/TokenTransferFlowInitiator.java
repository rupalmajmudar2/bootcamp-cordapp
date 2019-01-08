package org.vloyalty.flow;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.vloyalty.contract.TokenContract;
import org.vloyalty.state.TokenState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import net.corda.core.utilities.ProgressTracker;

import java.security.PublicKey;
import java.util.List;

@InitiatingFlow
@StartableByRPC
public class TokenTransferFlowInitiator extends FlowLogic<SignedTransaction> {
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

        // We use the notary used by the input state.
        Party notary = inputTokenStateAndRef.getState().getNotary();

        // We build a transaction using a `TransactionBuilder`.
        TransactionBuilder txBuilder = new TransactionBuilder();

        // After creating the `TransactionBuilder`, we must specify which
        // notary it will use.
        txBuilder.setNotary(notary);

        // We add the input ArtState to the transaction.
        txBuilder.addInputState(inputTokenStateAndRef);

        // We add the TWO output TokenStates to the transaction:
        //(i) The specified #tokens to the newOwner
        //(ii) The remaining tokens back to us!!
        TokenState outputTokenStateForNewOwner = new TokenState( //issuer, owner, amount
                currentOwner,
                _newOwner,
                _numTokensToTxfr);
        txBuilder.addOutputState(outputTokenStateForNewOwner, TokenContract.ID);

        TokenState outputTokenStateForOurselves = new TokenState( //issuer, owner, amount
                currentOwner,
                currentOwner,
                55); //TEMP!!
        /**
         * TODO:
         * (i) Compute the total tokens with currentOwner
         * (ii) Validate that amountToTxfr is less than this total
         * (iii) Txfr total less amountToTxfr, to oueselves.
         **/
        txBuilder.addOutputState(outputTokenStateForOurselves, TokenContract.ID);

        // We add the Transfer command to the transaction.
        // Note that we also specific who is required to sign the transaction.
        TokenContract.Commands.Transfer commandData = new TokenContract.Commands.Transfer();
        List<PublicKey> requiredSigners = ImmutableList.of(
                inputTokenState.getOwner().getOwningKey(), _newOwner.getOwningKey()); //BOTH sign.
        txBuilder.addCommand(commandData, requiredSigners);

        // We check that the transaction builder we've created meets the
        // contracts of the input and output states.
        txBuilder.verify(getServiceHub());

        // We finalise the transaction builder by signing it,
        // converting it into a `SignedTransaction`.
        SignedTransaction partlySignedTx = getServiceHub().signInitialTransaction(txBuilder);

        // We use `CollectSignaturesFlow` to automatically gather a
        // signature from each counterparty. The counterparty will need to
        // call `SignTransactionFlow` to decided whether or not to sign.
        FlowSession ownerSession = initiateFlow(_newOwner);
        SignedTransaction fullySignedTx = subFlow(
                new CollectSignaturesFlow(partlySignedTx, ImmutableSet.of(ownerSession)));

        // We use `FinalityFlow` to automatically notarise the transaction
        // and have it recorded by all the `participants` of all the
        // transaction's states.
        return subFlow(new FinalityFlow(fullySignedTx));

        //return null;
    }
}