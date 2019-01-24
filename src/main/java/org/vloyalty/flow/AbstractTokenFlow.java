package org.vloyalty.flow;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.flows.CollectSignaturesFlow;
import net.corda.core.flows.FinalityFlow;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.utilities.ProgressTracker;

abstract class AbstractTokenFlow extends FlowLogic<SignedTransaction> {

    protected final ProgressTracker.Step CHECK_OTHER_NODES = new ProgressTracker.Step("Checking the other Nodes on the Network.");
    protected final ProgressTracker.Step GENERATING_ISSUE_TRANSACTION = new ProgressTracker.Step("Generating TokenIssue transaction.");
    protected final ProgressTracker.Step GENERATING_TXFR_INITIATOR_TRANSACTION= new ProgressTracker.Step("Generating TokenTransfer Initiator transaction.");
    protected final ProgressTracker.Step VERIFYING_TRANSACTION = new ProgressTracker.Step("Verifying Contract constraints.");
    protected final ProgressTracker.Step SIGNING_TRANSACTION = new ProgressTracker.Step("Signing Token Transaction with our Private Key.");
    protected final ProgressTracker.Step GATHERING_SIGS = new ProgressTracker.Step("Gathering the Counterparty's Signature.") {
        @Override
        public ProgressTracker childProgressTracker() {
            return CollectSignaturesFlow.Companion.tracker();
        }
    };
    protected final ProgressTracker.Step FINALISING_TRANSACTION = new ProgressTracker.Step("Obtaining Notary Signature and Recording Transaction.") {
        @Override
        public ProgressTracker childProgressTracker() {
            return FinalityFlow.Companion.tracker();
        }
    };

    protected final ProgressTracker _progressTracker = new ProgressTracker(
                                                                CHECK_OTHER_NODES,
                                                                GENERATING_ISSUE_TRANSACTION,
                                                                GENERATING_TXFR_INITIATOR_TRANSACTION,
                                                                VERIFYING_TRANSACTION,
                                                                SIGNING_TRANSACTION,
                                                                GATHERING_SIGS,
                                                                FINALISING_TRANSACTION
                                                        );

    @Override
    public ProgressTracker getProgressTracker() {
        return _progressTracker;
    }

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {
        //do nothing
        //for testing additional call aspects
        //@see https://docs.corda.net/flow-cookbook.html
        getProgressTracker().setCurrentStep(CHECK_OTHER_NODES);

        // Dummy checks: We retrieve the notary from the network map.
        /*CordaX500Name notaryName = new CordaX500Name(
                "Notary",
                "Muttenz",
                "CH");
        Party specificNotary = getServiceHub().getIdentityService().wellKnownPartyFromX500Name(notaryName); //.getNetworkMapCache().getNotary(notaryName);
        if (specificNotary == null)
            throw new IllegalArgumentException("Couldn't find Counterparty in Muttenz");*/

                // Alternatively, we can pick an arbitrary notary from the notary
                // list. However, it is always preferable to specify the notary
                // explicitly, as the notary list might change when new notaries are
                // introduced, or old ones decommissioned.
        Party firstNotary = getServiceHub().getNetworkMapCache().getNotaryIdentities().get(0);
        System.out.println("Found Notary=" + firstNotary.getName());

        /*CordaX500Name counterpartyName = new CordaX500Name(
                "SBB",
                "Bern",
                "CH");
        Party namedCounterparty = getServiceHub().getIdentityService().wellKnownPartyFromX500Name(counterpartyName);
        if (namedCounterparty == null)
            throw new IllegalArgumentException("Couldn't find counterparty for NodeA in identity service");
        System.out.println("Found SBB Node=" + namedCounterparty.getName());*/

        return null;
    }
}
