package org.vloyalty;

import com.google.gson.Gson;
import net.corda.client.rpc.CordaRPCClient;
import net.corda.client.rpc.CordaRPCClientConfiguration;
import net.corda.core.contracts.StateAndRef;
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
import org.vloyalty.state.CouponState;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

//import org.apache.shiro.util.Assert;

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
    public void getCoupons() throws Exception {
        final NetworkHostAndPort nodeAddress = NetworkHostAndPort.parse("localhost:10008");
        final CordaRPCClient rpcOps = new CordaRPCClient(nodeAddress, CordaRPCClientConfiguration.DEFAULT);
        final CordaRPCOps proxy = rpcOps.start("user1", "test").getProxy();

        TokenApi api = new TokenApi(proxy);
        List<StateAndRef<CouponState>> coupons=  api.getCoupons();
        assert(true);
    }

    @Test
    public void issueCash() throws Exception {
        final NetworkHostAndPort nodeAddress = NetworkHostAndPort.parse("localhost:10008");
        final CordaRPCClient rpcOps = new CordaRPCClient(nodeAddress, CordaRPCClientConfiguration.DEFAULT);
        final CordaRPCOps proxy = rpcOps.start("user1", "test").getProxy();

        TokenApi api = new TokenApi(proxy);
        CordaX500Name me= proxy.nodeInfo().getLegalIdentities().get(0).getName();
        api.issueCash(100, me);
        assert(true);
    }

    /*
    @Test
    public void ui() throws Exception {
        String longString= "hfkldjfkd";
        JTextArea textArea = new JTextArea(longString);
        textArea.setSize( new Dimension(400, 40) );
        int response = JOptionPane.showConfirmDialog(textArea, "Do you wish to Sign this txn?", "Confirm for Node:" + "MyNodeName",
                JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
    }
    */
    @Test
    public void issueCoupon() throws Exception {
        final NetworkHostAndPort nodeAddress = NetworkHostAndPort.parse("localhost:10008");
        final CordaRPCClient rpcOps = new CordaRPCClient(nodeAddress, CordaRPCClientConfiguration.DEFAULT);
        final CordaRPCOps proxy = rpcOps.start("user1", "test").getProxy();

        TokenApi api = new TokenApi(proxy);

        String  text= "Coupon : 40% off Evian until 31.03.2019";
        CordaX500Name sbb_dist= new CordaX500Name("SBB", "Bern", "CH");
        CordaX500Name evian_issuer= new CordaX500Name("Evian", "Pfaffikon", "CH");
        CordaX500Name sbb_owner= new CordaX500Name("SBB", "Bern", "CH");

        api.issueCoupon(text, sbb_owner, sbb_dist);
    }

    @Test
    public void updateCoupon() throws Exception {
        final NetworkHostAndPort nodeAddress = NetworkHostAndPort.parse("localhost:10008");
        final CordaRPCClient rpcOps = new CordaRPCClient(nodeAddress, CordaRPCClientConfiguration.DEFAULT);
        final CordaRPCOps proxy = rpcOps.start("user1", "test").getProxy();

        TokenApi api = new TokenApi(proxy);

        //Transfer from Valora (to whom this client/api is connected) to the Customer
        CordaX500Name customer_owner= new CordaX500Name("Customer", "Zug", "CH");

        api.updateCoupon("New Status is with Customer", customer_owner);
    }

    @Test
    public void parseTxnString() throws Exception {
        final NetworkHostAndPort nodeAddress = NetworkHostAndPort.parse("localhost:10008");
        final CordaRPCClient rpcOps = new CordaRPCClient(nodeAddress, CordaRPCClientConfiguration.DEFAULT);
        final CordaRPCOps proxy = rpcOps.start("user1", "test").getProxy();
        TokenApi api = new TokenApi(proxy);

        String str= "Input: B6A4797D1934A42D48C05DCEAD90F997029024A2CCF46ECD7D30749C52E7D955(1)";
        String expectedOut= "Input:B6A4(1)";
        String finalStr= api.inputStringCleanup(str);
        assert(expectedOut.equals(finalStr));

        str="Txn: 2FA1:Transaction:Input: B6A4797D1934A42D48C05DCEAD90F997029024A2CCF46ECD7D30749C52E7D955(1)Output: TokenState(#tokens=20, owner=O=Customer, L=Zug, C=CH, issuer=O=Valora, L=Zurich, C=CH)Output: TokenState(#tokens=391, owner=O=Valora, L=Zurich, C=CH, issuer=O=Valora, L=Zurich, C=CH)";
        expectedOut="Txn: 2FA1:Transaction:Input:B6A4(1)Output: TokenState(#tokens=20, owner=O=Customer, L=Zug, C=CH, issuer=O=Valora, L=Zurich, C=CH)Output: TokenState(#tokens=391, owner=O=Valora, L=Zurich, C=CH, issuer=O=Valora, L=Zurich, C=CH)";
        finalStr= api.inputStringCleanup(str);
        assert(expectedOut.equals(finalStr));

        str="Txn: 8458:Transaction:Output: TokenState(#tokens=444, owner=O=Valora, L=Zurich, C=CH, issuer=O=Valora, L=Zurich, C=CH)";
        expectedOut="Txn: 8458:Transaction:Output: TokenState(#tokens=444, owner=O=Valora, L=Zurich, C=CH, issuer=O=Valora, L=Zurich, C=CH)";
        finalStr= api.inputStringCleanup(str);
        assert(expectedOut.equals(finalStr));

        str="C1C1:Transaction:Input:      1F2410CFD542C6968A557E73C105BC858C038D3BDD1F66250EF180DBF6A937A3(0)Output:     TokenState(#tokens=33, owner=O=Evian, L=Pfaffikon, C=CH, issuer=O=Valora, L=Zurich, C=CH)Output:     TokenState(#tokens=78, owner=O=Valora, L=Zurich, C=CH, issuer=O=Valora, L=Zurich, C=CH)";
        expectedOut="C1C1:Transaction:Input:1F24(0)Output:     TokenState(#tokens=33, owner=O=Evian, L=Pfaffikon, C=CH, issuer=O=Valora, L=Zurich, C=CH)Output:     TokenState(#tokens=78, owner=O=Valora, L=Zurich, C=CH, issuer=O=Valora, L=Zurich, C=CH)";
        finalStr= api.inputStringCleanup(str);
        assert(expectedOut.equals(finalStr));

    }

    @Test
    public void getMyTxnx() throws Exception {
        final NetworkHostAndPort nodeAddress = NetworkHostAndPort.parse("localhost:10008");
        final CordaRPCClient rpcOps = new CordaRPCClient(nodeAddress, CordaRPCClientConfiguration.DEFAULT);
        final CordaRPCOps proxy = rpcOps.start("user1", "test").getProxy();

        TokenApi api = new TokenApi(proxy);
        String res= api.getTxns();
        /*List<SignedTransaction> txns= (List<SignedTransaction>) res.getEntity();
        assert(txns.size() > 0);
        SignedTransaction txn = txns.get(0);
        WireTransaction wtx= txn.getTx();*/
        //List<ContractState> outputStates= wtx.getOutputStates();
        //Object resp= res.getEntity();
        System.out.println(res);
        assert(true);
    }

    @Test
    public void getTokensBrief() throws Exception {
        final NetworkHostAndPort nodeAddress = NetworkHostAndPort.parse("localhost:10008");
        final CordaRPCClient rpcOps = new CordaRPCClient(nodeAddress, CordaRPCClientConfiguration.DEFAULT);
        final CordaRPCOps proxy = rpcOps.start("user1", "test").getProxy();

        TokenApi api = new TokenApi(proxy);
        api.getTokensWithOrgs();
    }

    @Test
    public void getTokensIssuedByMe() throws Exception {
        final NetworkHostAndPort nodeAddress = NetworkHostAndPort.parse("localhost:10008");
        final CordaRPCClient rpcOps = new CordaRPCClient(nodeAddress, CordaRPCClientConfiguration.DEFAULT);
        final CordaRPCOps proxy = rpcOps.start("user1", "test").getProxy();

        TokenApi api = new TokenApi(proxy);
        api.getTokensIssuedByMe();
    }

    @Test
    public void getPeerInfos() throws Exception {
        final NetworkHostAndPort nodeAddress = NetworkHostAndPort.parse("localhost:10008");
        final CordaRPCClient rpcOps = new CordaRPCClient(nodeAddress, CordaRPCClientConfiguration.DEFAULT);
        final CordaRPCOps proxy = rpcOps.start("user1", "test").getProxy();

        TokenApi api= new TokenApi(proxy);
        String detailsJson= api.getPeerDetails();
        HashMap dets= new Gson().fromJson(detailsJson, HashMap.class);
        Assert.assertTrue(dets.size() == 5);

        CordaX500Name sbb= new CordaX500Name("SBB", "Bern", "CH");
        Map sbb_dets= (Map) dets.get(sbb.toString());
        Assert.assertTrue(sbb_dets.get("isPartnerNode") instanceof Boolean);
        boolean isPartnerNode= (Boolean) sbb_dets.get("isPartnerNode");
        Assert.assertTrue(isPartnerNode);

        Assert.assertTrue(sbb_dets.get("isCustomerNode") instanceof Boolean);
        boolean isCustomerNode= (Boolean) sbb_dets.get("isCustomerNode");
        Assert.assertFalse(isCustomerNode);
    }


    @Test
    public void sendAttachmentToPeer() throws Exception {
        final NetworkHostAndPort nodeAddress = NetworkHostAndPort.parse("localhost:10008");
        final CordaRPCClient rpcOps = new CordaRPCClient(nodeAddress, CordaRPCClientConfiguration.DEFAULT);
        final CordaRPCOps proxy = rpcOps.start("user1", "test").getProxy();

        TokenApi api= new TokenApi(proxy);
        String attachmentFn= "c:\\users\\rupal\\corda-attach.zip";
        CordaX500Name sbb= new CordaX500Name("Valora", "Zurich", "CH");
        //CordaX500Name sbb= new CordaX500Name("SBB", "Bern", "CH");
        api.sendAttachment(attachmentFn, sbb);
        assert(true);
    }

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