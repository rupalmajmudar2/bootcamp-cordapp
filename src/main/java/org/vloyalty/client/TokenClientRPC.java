package org.vloyalty.client;

import net.corda.client.rpc.CordaRPCClient;
import net.corda.client.rpc.CordaRPCClientConfiguration;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.contracts.StateRef;
import net.corda.core.crypto.SecureHash;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.messaging.DataFeed;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.Builder;
import net.corda.core.node.services.vault.CriteriaExpression;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.utilities.NetworkHostAndPort;
import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vloyalty.schema.TokenSchema;
import org.vloyalty.state.TokenState;
import rx.Observable;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Demonstration of using the CordaRPCClient to connect to a Corda Node and
 * steam some State data from the node.
 */
public class TokenClientRPC {
    private static final Logger logger = LoggerFactory.getLogger(TokenClientRPC.class);

    private static void logState(StateAndRef<TokenState> state) {
        logger.info("#TokenClientRPC.logState");
        logger.info("{}", state.getState().getData());
    }

    public static void main(String[] args) throws ActiveMQException, InterruptedException, ExecutionException, Exception {
        if (args.length != 1) {
            throw new IllegalArgumentException("Usage: TokenClientRPC <node address>");
            //args= new String[1]; args[0] = "localhost:10009";
        }

        final NetworkHostAndPort nodeAddress = NetworkHostAndPort.parse(args[0]);
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
        String txnHash= "1661F28BBBB4839F601B745E4C83D67F97C31F3C1A950341D168153D2B7D35F0";
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
        }
       /* List<ContractState> inputStates= inputStateRefs.
        val inputStates = inputStateRefs.
                map { stateRef ->
                val transaction = transactions.find { it.id == stateRef.txhash }
            ?: throw IllegalArgumentException("Unknown transaction hash.")
            transaction.tx.outputStates[stateRef.index]
        }*/

        /// -- End txn hist

        // Grab all existing and future IOU states in the vault.
        final DataFeed<Vault.Page<TokenState>, Vault.Update<TokenState>> dataFeed = proxy.vaultTrack(TokenState.class);
        final Vault.Page<TokenState> snapshot = dataFeed.getSnapshot();
        final Observable<Vault.Update<TokenState>> updates = dataFeed.getUpdates();

        // Log the 'placed' Tokens and listen for new ones.
        snapshot.getStates().forEach(TokenClientRPC::logState);
        updates.toBlocking().subscribe(update -> update.getProduced().forEach(TokenClientRPC::logState));
    }
}
