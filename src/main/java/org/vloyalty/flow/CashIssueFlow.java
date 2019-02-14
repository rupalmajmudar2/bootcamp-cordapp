package org.vloyalty.flow;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import net.corda.core.flows.FinalityFlow;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.InitiatingFlow;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import org.vloyalty.contract.CashContract;
import org.vloyalty.state.CashState;

import java.security.PublicKey;
import java.util.List;

/* Our flow, automating the process of updating the ledger.
 * See src/main/java/examples/ArtTransferFlowInitiator.java for an example. */
@InitiatingFlow
@StartableByRPC
public class CashIssueFlow extends AbstractTokenFlow { //FlowLogic<SignedTransaction> {
    private final Party owner;
    private final int amount;

    public CashIssueFlow(Party owner, int amount) {
        this.owner = owner;
        this.amount = amount;
    }

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {
        super.call(); //for testing additional call aspects

        // We choose our transaction's notary (the notary prevents double-spends).
        Party notary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
        // We get a reference to our own identity.
        Party issuer = getOurIdentity();

        /* ============================================================================
         *         STEP 1 - Create our TokenState to represent on-ledger tokens!
         * ===========================================================================*/
        // We create our new TokenState.
        CashState cashState = new CashState(issuer, owner, amount, "CHF");

        //The issuer then has a negative cash balance!!
        CashState cashState2 = new CashState(issuer, issuer, (amount*-1), "CHF");

        CashContract.Commands.Issue command= new CashContract.Commands.Issue();

        /* ============================================================================
         *      STEP 3 - Build our token issuance transaction to update the ledger!
         * ===========================================================================*/
        // We build our transaction.
        //StateAndRef<ContractState> inputState= new StateAndRef<TokenState>;

        getProgressTracker().setCurrentStep(GENERATING_ISSUE_TRANSACTION);

        PublicKey reqdSigner= issuer.getOwningKey();
        List<PublicKey> reqdSigners= ImmutableList.of(reqdSigner);

        TransactionBuilder transactionBuilder = new TransactionBuilder();
        transactionBuilder.setNotary(notary);

        transactionBuilder
                //.addInputState(inputState)
                .addOutputState(cashState, CashContract.ID) //Which contract will be associated with this (new) outputState
               // .addOutputState(cashState2, CashContract.ID) //new! -ve cash balance for issuer
                    //No :( -ve Amount is not allowed by Amount.kt :(
                .addCommand(command, reqdSigners);

        /* ============================================================================
         *          STEP 2 - Write our TokenContract to control token issuance!
         * ===========================================================================*/
        // We check our transaction is valid based on its contracts.
        getProgressTracker().setCurrentStep(VERIFYING_TRANSACTION);
        transactionBuilder.verify(getServiceHub());

        // We sign the transaction with our private key, making it immutable.
        getProgressTracker().setCurrentStep(SIGNING_TRANSACTION);
        SignedTransaction signedTransaction = getServiceHub().signInitialTransaction(transactionBuilder);

        // We get the transaction notarised and recorded automatically by the platform.
        getProgressTracker().setCurrentStep(FINALISING_TRANSACTION);
        return subFlow(new FinalityFlow(signedTransaction));
    }
}