package org.vloyalty;

import com.google.common.collect.ImmutableList;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.TransactionState;
import net.corda.core.contracts.TransactionVerificationException;
import net.corda.core.flows.FlowException;
import net.corda.core.transactions.SignedTransaction;
import net.corda.testing.node.MockNetwork;
import net.corda.testing.node.StartedMockNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.vloyalty.contract.TokenContract;
import org.vloyalty.flow.TokenIssueFlow;
import org.vloyalty.flow.TokenTransferFlowInitiator;
import org.vloyalty.flow.TokenTransferFlowResponder;
import org.vloyalty.state.TokenState;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TransferFlowTests {
    private MockNetwork network;
    private StartedMockNode nodeA;
    private StartedMockNode nodeB;

    @Before
    public void setup() {
        network = new MockNetwork(ImmutableList.of("org.vloyalty.contract"));
        nodeA = network.createPartyNode(null);
        nodeB = network.createPartyNode(null);

        //From IOUFlowTests.java :
        // For real nodes this happens automatically, but we have to manually register the flow for tests.
        for (StartedMockNode node : ImmutableList.of(nodeA, nodeB)) {
            node.registerInitiatedFlow(TokenTransferFlowResponder.class);
        }

        network.runNetwork();
    }

    @After
    public void tearDown() {
        network.stopNodes();
    }

    @Rule
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void flowRejectsInvalidTokenAmounts() throws Exception {
        TokenTransferFlowInitiator flow = new TokenTransferFlowInitiator(nodeB.getInfo().getLegalIdentities().get(0), -1);
        CordaFuture<SignedTransaction> future = nodeA.startFlow(flow);
        network.runNetwork();

        // IllegalArgumentException("No Tokens found for owner...") - since this nodeA has zero token balance
        //@see TokenTransferFlowInitiator#call
        exception.expectCause(instanceOf(IllegalArgumentException.class));
        exception.expectMessage("No Tokens found for owner=O=Mock Company 1, L=London, C=GB");

        future.get();
    }

    @Test
    public void signedTransactionReturnedByTheFlowIsSignedByTheInitiator() throws Exception {

        //First issue tokens to nodeA so that it can do the transfer to nodeB.
        /*TokenIssueFlow issueFlow = new TokenIssueFlow(nodeA.getInfo().getLegalIdentities().get(0), 99);
        CordaFuture<SignedTransaction> issueFuture = nodeA.startFlow(issueFlow);
        network.runNetwork();
        SignedTransaction signedIssueTx = issueFuture.get();
        TokenState issuedState = signedIssueTx.getTx().outputsOfType(TokenState.class).get(0);
        assertEquals(99, issuedState.getAmount());*/
        provideInitialTokenBalance();

        TokenTransferFlowInitiator flow = new TokenTransferFlowInitiator(nodeB.getInfo().getLegalIdentities().get(0), 22);
        CordaFuture<SignedTransaction> future = nodeA.startFlow(flow);
        network.runNetwork();

        SignedTransaction signedTx = future.get();
        signedTx.verifySignaturesExcept(nodeB.getInfo().getLegalIdentities().get(0).getOwningKey());
    }

    private void provideInitialTokenBalance() throws Exception {
        //First issue tokens to nodeA so that it can do the transfer to nodeB.
        TokenIssueFlow issueFlow = new TokenIssueFlow(nodeA.getInfo().getLegalIdentities().get(0), 99);
        CordaFuture<SignedTransaction> issueFuture = nodeA.startFlow(issueFlow);
        network.runNetwork();
        SignedTransaction signedIssueTx = issueFuture.get();
        TokenState issuedState = signedIssueTx.getTx().outputsOfType(TokenState.class).get(0);
        assertEquals(99, issuedState.getAmount());
    }

    @Test
    public void signedTransactionReturnedByTheFlowIsSignedByTheAcceptor() throws Exception {
        provideInitialTokenBalance();

        TokenTransferFlowInitiator flow = new TokenTransferFlowInitiator(nodeB.getInfo().getLegalIdentities().get(0), 33);
        CordaFuture<SignedTransaction> future = nodeA.startFlow(flow);
        network.runNetwork();

        SignedTransaction signedTx = future.get();
        signedTx.verifySignaturesExcept(nodeA.getInfo().getLegalIdentities().get(0).getOwningKey());
    }

    @Test
    public void flowRecordsATransactionInBothPartiesTransactionStorages() throws Exception {
        provideInitialTokenBalance();

        TokenTransferFlowInitiator flow = new TokenTransferFlowInitiator(nodeB.getInfo().getLegalIdentities().get(0), 44);
        CordaFuture<SignedTransaction> future = nodeA.startFlow(flow);
        network.runNetwork();
        SignedTransaction signedTx = future.get();

        // We check the recorded transaction in both vaults.
        for (StartedMockNode node : ImmutableList.of(nodeA, nodeB)) {
            assertEquals(signedTx, node.getServices().getValidatedTransactions().getTransaction(signedTx.getId()));
        }
    }
}