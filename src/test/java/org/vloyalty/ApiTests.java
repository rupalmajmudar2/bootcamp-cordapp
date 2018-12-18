package org.vloyalty;

import com.google.common.collect.ImmutableList;
import net.corda.client.rpc.CordaRPCClient;
import net.corda.client.rpc.CordaRPCClientConfiguration;
import net.corda.core.concurrent.CordaFuture;
import net.corda.core.contracts.Command;
import net.corda.core.contracts.TransactionState;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.utilities.NetworkHostAndPort;
import net.corda.testing.core.TestIdentity;
import net.corda.testing.node.MockNetwork;
import net.corda.testing.node.StartedMockNode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.vloyalty.contract.TokenContract;
import org.vloyalty.flow.TokenIssueFlow;
import org.vloyalty.state.TokenState;
import org.vloyalty.api.TokenApi;

import static org.junit.Assert.assertEquals;

/**
 * Rupal 18Dec18 - try automate the things done inside TokenApi.
 */
public class ApiTests {
    private MockNetwork network;
    private StartedMockNode nodeA;
    private StartedMockNode nodeB;

    @Before
    public void setup() {
        network = new MockNetwork(ImmutableList.of("org.vloyalty.contract"));
        nodeA = network.createPartyNode(null);
        nodeB = network.createPartyNode(null);
        network.runNetwork();
    }

    @After
    public void tearDown() {
        network.stopNodes();
    }

    @Test
    public void createTokenCreationFlow() throws Exception {

        Party otherParty= new TestIdentity(new CordaX500Name("SBB", "Bern", "CH")).getParty();
        int numTokens= 20;

        final NetworkHostAndPort nodeAddress = NetworkHostAndPort.parse("localhost:10009");
        final CordaRPCClient rpcOps = new CordaRPCClient(nodeAddress, CordaRPCClientConfiguration.DEFAULT);
        final CordaRPCOps proxy = rpcOps.start("user1", "test").getProxy();
        final SignedTransaction signedTx = proxy
                .startTrackedFlowDynamic(TokenIssueFlow.class, otherParty, numTokens)
                .getReturnValue()
                .get();

        final String msg = String.format("Transaction id %s committed to ledger.\n", signedTx.getId());

        TokenIssueFlow flow = new TokenIssueFlow(nodeB.getInfo().getLegalIdentities().get(0), 99);
        CordaFuture<SignedTransaction> future = nodeA.startFlow(flow);
        network.runNetwork();
        SignedTransaction signedTransaction = future.get();

        assertEquals(1, signedTransaction.getTx().getOutputStates().size());
        TransactionState output = signedTransaction.getTx().getOutputs().get(0);

        assertEquals(network.getNotaryNodes().get(0).getInfo().getLegalIdentities().get(0), output.getNotary());
    }
}