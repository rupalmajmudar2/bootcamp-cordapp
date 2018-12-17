package rmbak_pkg;

import org.vloyalty.state.TokenState;
import net.corda.core.contracts.ContractState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.identity.Party;
import net.corda.core.transactions.TransactionBuilder;

import net.corda.core.flows.*;
import net.corda.core.transactions.SignedTransaction;

public class TxnBuilderScratch extends FlowLogic<SignedTransaction> {

    public static void main(String[] args) throws Exception {
        StateAndRef<ContractState> inputState= null;
        Party owner= null; Party issuer= null;
        TokenState outputState= new TokenState(issuer, owner, 100);
        //PublicKey reqdSigner= issuer.getOwningKey();
       //List<PublicKey> reqdSigners= ImmutableList.of(reqdSigner);

        Party notary= null;
        TransactionBuilder txnBldr = new TransactionBuilder();
        txnBldr
              //  .addInputState(inputState)
                .addOutputState(outputState, "java.bootcamp.TokenState"); //Which contract will be associated with this (new) outputState
                //.addCommand(new TokenContract.Commands.Issue(), reqdSigners);

        txnBldr.verify(new TxnBuilderScratch().getServiceHub());
        //txnBldr.setNotary(notary);
    }

    @Override
    public SignedTransaction call() throws FlowException {
        return null;
    }
}
