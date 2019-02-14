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
import org.vloyalty.contract.CouponContract;
import org.vloyalty.state.CouponState;

import java.security.PublicKey;
import java.util.List;

/* Our flow, automating the process of updating the ledger.
 * See src/main/java/examples/ArtTransferFlowInitiator.java for an example. */
@InitiatingFlow
@StartableByRPC
public class CouponIssueFlow extends AbstractTokenFlow { //FlowLogic<SignedTransaction> {
    private final String _text;
    private final Party _owner;
    private final Party _distributor;
    private final String _status;

    public CouponIssueFlow(String text, Party owner, Party distributor, String status) {
        _text = text;
        _owner = owner;
        _distributor = distributor;
        _status = status;
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
         *         STEP 1 - Create our CouponState to represent on-ledger tokens!
         * ===========================================================================*/
        // We create our new CouponState.
        CouponState tokenState = new CouponState(_text, issuer, _owner, _distributor, _status);

        CouponContract.Commands.Issue command= new CouponContract.Commands.Issue();

        /* ============================================================================
         *      STEP 3 - Build our coupon issuance transaction to update the ledger!
         * ===========================================================================*/
        // We build our transaction.

        getProgressTracker().setCurrentStep(GENERATING_ISSUE_TRANSACTION);

        PublicKey reqdSigner= issuer.getOwningKey();
        List<PublicKey> reqdSigners= ImmutableList.of(reqdSigner);

        TransactionBuilder transactionBuilder = new TransactionBuilder();
        transactionBuilder.setNotary(notary);

        transactionBuilder
                //.addInputState(inputState)
                .addOutputState(tokenState, CouponContract.ID) //Which contract will be associated with this (new) outputState
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