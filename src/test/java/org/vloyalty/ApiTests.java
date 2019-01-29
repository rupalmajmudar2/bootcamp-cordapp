package org.vloyalty;

import com.google.gson.Gson;
import net.corda.client.rpc.CordaRPCClient;
import net.corda.client.rpc.CordaRPCClientConfiguration;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.utilities.NetworkHostAndPort;
import net.corda.testing.node.MockNetwork;
import net.corda.testing.node.StartedMockNode;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.vloyalty.api.TokenApi;

import java.util.HashMap;
import java.util.Map;

/**
 * Rupal 18Dec18 - try automate the things done inside TokenApi.
 */
public class ApiTests {
    private MockNetwork network;
    private StartedMockNode nodeA;
    private StartedMockNode nodeB;
    private StartedMockNode nodeC;

    @Before
    public void setup() {
        /*network = new MockNetwork(ImmutableList.of("org.vloyalty.contract"));
        nodeA = network.createPartyNode(new CordaX500Name("Valora", "Bern", "CH"));
        nodeB = network.createPartyNode(new CordaX500Name("SBB", "Bern", "CH"));
        nodeC = network.createPartyNode(new CordaX500Name("Alpamare", "Bern", "CH"));
        network.runNetwork();*/
    }

    @After
    public void tearDown() {
        //network.stopNodes();
    }

    @Test
    public void getPeerInfos() throws Exception {
        final NetworkHostAndPort nodeAddress = NetworkHostAndPort.parse("localhost:10008");
        final CordaRPCClient rpcOps = new CordaRPCClient(nodeAddress, CordaRPCClientConfiguration.DEFAULT);
        final CordaRPCOps proxy = rpcOps.start("user1", "test").getProxy();

        TokenApi api= new TokenApi(proxy);
        String detailsJson= api.getPeerDetails();
        HashMap dets= new Gson().fromJson(detailsJson, HashMap.class);
        Assert.assertTrue(dets.size() == 4);

        CordaX500Name sbb= new CordaX500Name("SBB", "Bern", "CH");
        Map sbb_dets= (Map) dets.get(sbb.toString());
        Assert.assertTrue(sbb_dets.get("isPartnerNode") instanceof Boolean);
        Boolean isPartnerNode= (Boolean) sbb_dets.get("isPartnerNode");
        Assert.assertTrue(isPartnerNode);

        Assert.assertTrue(sbb_dets.get("isCustomerNode") instanceof Boolean);
        Boolean isCustomerNode= (Boolean) sbb_dets.get("isCustomerNode");
        Assert.assertFalse(isCustomerNode);
    }

    /*
    @Test
    public void sendAttachmentToPeer() throws Exception {
        final NetworkHostAndPort nodeAddress = NetworkHostAndPort.parse("localhost:10008");
        final CordaRPCClient rpcOps = new CordaRPCClient(nodeAddress, CordaRPCClientConfiguration.DEFAULT);
        final CordaRPCOps proxy = rpcOps.start("user1", "test").getProxy();

        TokenApi api= new TokenApi(proxy);
        String attachmentFn= "c:\\users\\rupal\\corda-attach.zip";
        CordaX500Name sbb= new CordaX500Name("SBB", "Bern", "CH");
        api.sendAttachment(attachmentFn, sbb);
        assert(true);
    }
*/
    /*
    @Test
    public void createTokenCreationFlow() throws Exception {
*/
        //Party otherParty= new TestIdentity(new CordaX500Name("SBB", "Bern", "CH")).getParty();
        //StartedMockNode nodeC = network.createPartyNode( new CordaX500Name("SBB", "Bern", "CH") );
       /* int numTokens= 20;

        final NetworkHostAndPort nodeAddress = NetworkHostAndPort.parse("localhost:10008");
        final CordaRPCClient rpcOps = new CordaRPCClient(nodeAddress, CordaRPCClientConfiguration.DEFAULT);
        final CordaRPCOps proxy = rpcOps.start("user1", "test").getProxy();
        System.out.println( proxy.nodeInfo() );*/

        //nodeB.getServices();
        //System.out.println( nodeB.getId() );

        /*final SignedTransaction signedTx = proxy
                .startTrackedFlowDynamic(TokenIssueFlow.class, nodeB.getInfo().getLegalIdentities().get(0), numTokens)
                .getReturnValue()
                .get();

        final String msg = String.format("Transaction id %s committed to ledger.\n", signedTx.getId());

        TokenIssueFlow flow = new TokenIssueFlow(nodeB.getInfo().getLegalIdentities().get(0), 99);
        CordaFuture<SignedTransaction> future = nodeA.startFlow(flow);
        network.runNetwork();
        SignedTransaction signedTransaction = future.get();

        assertEquals(1, signedTransaction.getTx().getOutputStates().size());
        TransactionState output = signedTransaction.getTx().getOutputs().get(0);

        assertEquals(network.getNotaryNodes().get(0).getInfo().getLegalIdentities().get(0), output.getNotary());*/
    //}
}