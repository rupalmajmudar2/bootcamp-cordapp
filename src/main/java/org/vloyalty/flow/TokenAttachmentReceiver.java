package org.vloyalty.flow;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowSession;
import net.corda.core.flows.InitiatedBy;
import net.corda.core.flows.ReceiveTransactionFlow;
import net.corda.core.transactions.SignedTransaction;

@InitiatedBy(TokenAttachmentSender.class)
public class TokenAttachmentReceiver extends AbstractTokenFlow { //FlowLogic<SignedTransaction> {
    private final FlowSession counterpartySession;

    public TokenAttachmentReceiver(FlowSession counterpartySession) {
        this.counterpartySession = counterpartySession;
    }

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {
        System.out.println("TokenAttachmentReceiver#call");
        return subFlow(new ReceiveTransactionFlow(counterpartySession));
    }

  /*  class StoreAttachmentFlow(private val otherSide: FlowSession) : FlowLogic<Unit>() {
        @Suspendable
        override fun call() {
            // As a non-participant to the transaction we need to record all states
            subFlow(ReceiveFinalityFlow(otherSide, statesToRecord = StatesToRecord.ALL_VISIBLE))
        }
    }

    @StartableByRPC
    @StartableByService
    class NoProgressTrackerShellDemo : FlowLogic<String>() {
        @Suspendable
        override fun call(): String {
            return "You Called me!"
        }
    }*/
}
