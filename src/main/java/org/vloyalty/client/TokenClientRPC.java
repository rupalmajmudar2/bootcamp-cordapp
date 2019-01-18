package org.vloyalty.client;

import net.corda.client.rpc.CordaRPCClient;
import net.corda.client.rpc.CordaRPCClientConfiguration;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.StateRef;
import net.corda.core.contracts.TransactionState;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.messaging.DataFeed;
import net.corda.core.node.ServiceHub;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.VaultService;
import net.corda.core.node.services.vault.Builder;
import net.corda.core.node.services.vault.CriteriaExpression;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.utilities.NetworkHostAndPort;
import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.graphstream.graph.Node;
import org.graphstream.graph.implementations.MultiGraph;
import org.graphstream.ui.spriteManager.Sprite;
import org.graphstream.ui.spriteManager.SpriteManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vloyalty.schema.TokenSchema;
import org.vloyalty.state.TokenState;
import org.vloyalty.token.Token;
import rx.Observable;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Demonstration of using the CordaRPCClient to connect to a Corda Node and
 * steam some State data from the node.
 */
public class TokenClientRPC {
    private static final Logger logger = LoggerFactory.getLogger(TokenClientRPC.class);
    private static MultiGraph _graph = new MultiGraph("Corda transactions");
    private static SpriteManager _sman = new SpriteManager(_graph);
    private static int _nodeNr = 0;

    private static void logState(StateAndRef<TokenState> state) {
        logger.info("#TokenClientRPC.logState");
        logger.info("{}", state.getState().getData());
        System.out.println("RM TokenClientRPC.logState : Ref#"+ state.getRef() + " : " + state.toString());
        StateRef stateRef= state.getRef();
        TransactionState<TokenState> tokenState= state.getState();
        TokenState ts= tokenState.getData();

        String stateRefStr= stateRef.toString();
        int indx= stateRefStr.indexOf("(");
        if (indx > -1) {
            stateRefStr= stateRefStr.substring(0, (indx));
        }
        _graph.addNode(stateRefStr);
        Sprite s = _sman.addSprite("S" + _nodeNr++);
        s.attachToNode(stateRefStr);
        System.out.println("Node added: " + stateRefStr);

        //_graph.display();
    }

    /*
        The graph will be defined as follows:
                Each transaction is a vertex, represented by printing NODE <txhash>
                Each input-output relationship is an edge, represented by prining EDGE <txhash> <txhash>
    */
    private static void logFeedTxn(SignedTransaction nextTxn) {
        //System.out.println("NODE: " + nextTxn.getId() + " Inputs size=" );
        List<StateRef> inputStateRefs= nextTxn.getInputs();
        //System.out.println("NODE: " + nextTxn.getId() + " Inputs size=" + inputStateRefs.size());
        //HashMap<StateRef, Integer> txn_io= new HashMap<>();
        //transaction.tx.inputs.forEach { (txhash) ->
        //                println("EDGE $txhash ${transaction.id}")
        List<ContractState> outputStates= nextTxn.getTx().getOutputStates();
        String nextTxnId= nextTxn.getId().toString();
        Node n= _graph.addNode(nextTxnId);
        Sprite s = _sman.addSprite("S" + _nodeNr++);
        s.attachToNode(nextTxnId);
        System.out.println("Node added: " + nextTxnId);
        System.out.println("Txn#: " + nextTxnId + " #Inputs=" + inputStateRefs.size() + " #Outputs=" + outputStates.size());

        if (inputStateRefs.size() == 0) System.out.println("   InputRef: {}");
        else {
            for (StateRef stateRef : inputStateRefs) {
                System.out.println("   InputRef: " + stateRef.getTxhash() + " Index=" + stateRef.getIndex());
                //graph.addEdge<Edge>("$ref", "${ref.txhash}", "${transaction.id}")
                _graph.addEdge(stateRef.toString(), stateRef.getTxhash().toString(), nextTxnId);
                //Testing:
                if (outputStates.size() == 2) {
                    //Node to itseld
                    System.out.println("   Graphing self-ref");
                    _graph.addEdge(stateRef.toString()+"Self", stateRef.getTxhash().toString(), stateRef.getTxhash().toString());
                }
            }
        }

        int ind=0;
        for (ContractState outState: outputStates) {
            System.out.println("   OutputRef=" + nextTxnId + " Index=" + ind++);
            System.out.println("              OutputState=" + outState.toString());
        }

        _graph.display();
        System.out.println("Break");
    }

    public static void main(String[] args) throws ActiveMQException, InterruptedException, ExecutionException, Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("Usage: TokenClientRPC <node address>");
            //args= new String[1]; args[0] = "localhost:10009";
        }

        final NetworkHostAndPort nodeAddress = NetworkHostAndPort.parse(args[0]);
        System.out.println("Client running at: " + nodeAddress.toString());
        final CordaRPCClient client = new CordaRPCClient(nodeAddress, CordaRPCClientConfiguration.DEFAULT);

        // Can be amended in the com.example.Main file.
        final CordaRPCOps proxy = client.start("user1", "test").getProxy();

        //SecureHash txnId= SecureHash.parse(""); //4C9CF9FDD1648F0FCF9DCE536665F07D8D4A13E165CAC37694339095BCF4C164 ");
        //proxy.getVaultTransactionNotes(txnId); //4C9CF9FDD1648F0FCF9DCE536665F07D8D4A13E165CAC37694339095BCF4C164);
        //proxy.openAttachment();
                List<Party> notaries= proxy.notaryIdentities();
                Party notary= notaries.get(0);

        //proxy.getTokensIssuedByMe() {
            QueryCriteria generalCriteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.ALL);
            List<StateAndRef<TokenState>> allResults = proxy.vaultQueryByCriteria(generalCriteria, TokenState.class).getStates();

            Field issuer = TokenSchema.PersistentToken.class.getDeclaredField("issuer");
            CordaX500Name myLegalName = new CordaX500Name("Loyalty_AG", "Zurich", "CH");
            CriteriaExpression issuerIndex = Builder.equal(issuer, myLegalName.toString());
            QueryCriteria issuerCriteria = new QueryCriteria.VaultCustomQueryCriteria(issuerIndex);

            QueryCriteria criteria = /*generalCriteria.and*/(issuerCriteria);
            List<StateAndRef<TokenState>> results = proxy.vaultQueryByCriteria(criteria, TokenState.class).getStates();
        //}

        results= proxy.vaultQuery(TokenState.class).getStates();

        //
        //Txn history list - @see https://stackoverflow.com/questions/50594282/how-to-get-transaction-history-in-corda
       /* String txnHash= "B43107D09FDDA1A552B0D68AB716B1B5759581AC0F16B1BAC7271F77C70EEA29";
        List<SignedTransaction> txns= proxy.internalVerifiedTransactionsSnapshot();
        SignedTransaction myTxn = txns.stream()
                .filter(txn -> txnHash.equals(txn.getId().toString()))
                .findAny()
                .orElse(null);
        System.out.println("Found txn obj= " + myTxn.toString());

        List<StateRef> inputStateRefs= myTxn.getInputs();

        HashMap<StateRef, Integer> txn_io= new HashMap<>();
        for (StateRef ref: inputStateRefs) {
            SignedTransaction prevTxn = txns.stream()
                    .filter(txn -> txnHash.equals(ref.getTxhash().toString()))
                    .findAny()
                    .orElse(null);
            System.out.println("Input for C18B40 is: " + prevTxn);
        }*/
       /* List<ContractState> inputStates= inputStateRefs.
        val inputStates = inputStateRefs.
                map { stateRef ->
                val transaction = transactions.find { it.id == stateRef.txhash }
            ?: throw IllegalArgumentException("Unknown transaction hash.")
            transaction.tx.outputStates[stateRef.index]
        }*/

        /// -- End txn hist

        //Tracking from https://docs.corda.net/tutorial-clientrpc-api.html
        //MultiGraph graph = new MultiGraph("transactions");

        //First the already posted txns
        final DataFeed<Vault.Page<TokenState>, Vault.Update<TokenState>> dataFeed = proxy.vaultTrack(TokenState.class);
        final Vault.Page<TokenState> snapshot = dataFeed.getSnapshot();
        for (StateAndRef<TokenState> tokenStateStateAndRef : snapshot.getStates()) {
            logState(tokenStateStateAndRef);
        }
        //graph.display();

        Vault.Page<TokenState> allStates = proxy.vaultQuery(TokenState.class);

        proxy.internalVerifiedTransactionsFeed();
        DataFeed<List<SignedTransaction>, SignedTransaction> txnsFeed = proxy.internalVerifiedTransactionsFeed();
        Observable<SignedTransaction> futureTxns= txnsFeed.getUpdates();
        futureTxns.toBlocking().subscribe(
                nextTxn -> TokenClientRPC.logFeedTxn(nextTxn)
        );

        /*System.out.println("NODE : " + nextTxn.toString());

                println("NODE ${transaction.id}")
            transaction.tx.inputs.forEach { (txhash) ->
                    println("EDGE $txhash ${transaction.id}")
            }
        }*/


        // Grab all existing and future IOU states in the vault. *** For the node that this client is connected to. ***
        /*final DataFeed<Vault.Page<TokenState>, Vault.Update<TokenState>> dataFeed = proxy.vaultTrack(TokenState.class);
        final Vault.Page<TokenState> snapshot = dataFeed.getSnapshot();
        final Observable<Vault.Update<TokenState>> updates = dataFeed.getUpdates();

        // Log the 'placed' Tokens and listen for new ones.
        snapshot.getStates().forEach(TokenClientRPC::logState);
        updates.toBlocking().subscribe(update -> update.getProduced().forEach(TokenClientRPC::logState));

        System.out.println("Post-block");*/
    }
}
