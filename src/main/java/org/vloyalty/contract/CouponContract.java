package org.vloyalty.contract;

import net.corda.core.contracts.Command;
import net.corda.core.contracts.CommandData;
import net.corda.core.contracts.Contract;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.Party;
import net.corda.core.transactions.LedgerTransaction;
import org.jetbrains.annotations.NotNull;
import org.vloyalty.state.CouponState;

import java.security.PublicKey;
import java.util.List;

/* Our contract, governing how our state will evolve over time.
 * See src/main/java/examples/ArtContract.java for an example. */
public class CouponContract implements Contract {
    public static String ID = "org.vloyalty.contract.CouponContract";

    @Override
    public void verify(@NotNull LedgerTransaction tx) throws IllegalArgumentException {

        //(A) Shape rules - size of input and output
        if (tx.getCommands().size() != 1) throw new IllegalArgumentException("Exactly One command expected. Found: " + tx.getCommands().size());
        Command cmd= tx.getCommand(0);

        if (cmd.getValue() instanceof CouponContract.Commands.Update) { //TODO: Put in the checks here!
            System.out.println("TODO: CouponTransfer Contract verification");
        }
        else {
            if (!(cmd.getValue() instanceof CouponContract.Commands.Issue))
                throw new IllegalArgumentException("Cmd has to be of type Issue. Found: " + cmd.getValue());

            if (tx.getInputStates().size() != 0)
                throw new IllegalArgumentException(("Txn inputs should be 0. Found: " + tx.getInputStates().size()));
            if (tx.getOutputStates().size() != 1)
                throw new IllegalArgumentException(("Txn outputs should be 1. Found: " + tx.getOutputStates().size()));

            //(B) Content rules -
            ContractState cs = tx.getOutput(0);
            if (!(cs instanceof CouponState))
                throw new IllegalArgumentException("Tx output must be a TokenState. Found: " + cs.getClass());
            CouponState ts = (CouponState) cs;
            String text = ((CouponState) cs).getText();
            if (text.trim().equals("")) throw new IllegalArgumentException("Coupon Textmust be defined.");

            //(C) Signer rules
            final List<PublicKey> requiredSigners = cmd.getSigners();

            Party issuer = ts.getIssuer();
            PublicKey ipk = issuer.getOwningKey();
            if (!(requiredSigners.contains(ipk))) throw new IllegalArgumentException("Coupon issuer must be a signer");

            //etc.
        }
    }

    public interface Commands extends CommandData {
        public static class Issue implements Commands {
        }
        public static class Update implements Commands {
        }
    }
}