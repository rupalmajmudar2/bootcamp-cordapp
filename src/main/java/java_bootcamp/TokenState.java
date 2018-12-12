package java_bootcamp;

import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class TokenState implements ContractState {
    private int _amount;
    private Party _owner;
    private Party _issuer;

    public TokenState(Party issuer, Party owner, int amount) {
        _amount = amount;
        _owner = owner;
        _issuer = issuer;
    }

    public int getAmount() {
        return _amount;
    }

    public Party getOwner() {
        return _owner;
    }

    public Party getIssuer() {
        return _issuer;
    }

    public static void main(String[] args) {
        Party owner= null;
        Party issuer= null;
        TokenState ks= new TokenState(issuer, owner, 100);
    }

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return ImmutableList.of(_issuer, _owner);
    }
}
