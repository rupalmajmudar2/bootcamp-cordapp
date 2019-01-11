package org.vloyalty;

import net.corda.core.contracts.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.testing.core.TestIdentity;
import org.junit.Test;
import org.vloyalty.state.TokenState;
import org.vloyalty.token.Token;

import java.math.BigDecimal;
import java.util.Currency;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class StateTests {
    private final Party alice = new TestIdentity(new CordaX500Name("Alice", "", "GB")).getParty();
    private final Party bob = new TestIdentity(new CordaX500Name("Bob", "", "GB")).getParty();
    private final Party carol = new TestIdentity(new CordaX500Name("Carol", "", "CH")).getParty();

    @Test
    public void tokenStateHasIssuerOwnerAndAmountParamsOfCorrectTypeInConstructor() {
        new TokenState(alice, bob, 1);
    }

    @Test
    public void tokenStateHasGettersForIssuerOwnerAndAmount() {
        TokenState tokenState = new TokenState(alice, bob, 1);
        assertEquals(alice, tokenState.getIssuer());
        assertEquals(bob, tokenState.getOwner());
        assertEquals(1, tokenState.getNumTokens());
    }

    @Test
    public void tokenStateImplementsContractState() {
        assert(new TokenState(alice, bob, 1) instanceof ContractState);
    }

    @Test
    public void tokenStateHasTwoParticipantsTheIssuerAndTheOwner() {
        TokenState tokenState = new TokenState(alice, bob, 1);
        assertEquals(2, tokenState.getParticipants().size());
        assert(tokenState.getParticipants().contains(alice));
        assert(tokenState.getParticipants().contains(bob));
    }

    @Test
    public void tokenStatefromInt() {
        int numTokens= 25;
        TokenState tokenState = new TokenState(alice, bob, numTokens);
        Party o= tokenState.getOwner();
        int n= tokenState.getNumTokens();
        Amount<Issued<Token>> aa= tokenState.getAmount();
        assertEquals(aa.getQuantity(), numTokens);
        //tokenState.withNewOwnerAndAmount(Amount<Issued< Token >> amount, AbstractParty newOwner)

        CommandAndState transferred= tokenState.withNewOwner(carol);
        AbstractParty newOwn= transferred.getOwnableState().getOwner();
        CommandData cd= transferred.getCommand();
        assertTrue(true);

        int tokensToTxfr= 13;
        Amount<Issued<Token>> amtt= tokenState.getAmountFor(tokensToTxfr);
        TokenState transferredWithAmt= tokenState.withNewOwnerAndAmount(amtt, bob);
        AbstractParty newOwn2= transferredWithAmt.getOwner();
        int numT= transferredWithAmt.getNumTokens();
        assertTrue(true);
    }
}