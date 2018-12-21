package org.vloyalty.state;

import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.schemas.MappedSchema;
import net.corda.core.schemas.PersistentState;
import net.corda.core.schemas.QueryableState;
import org.jetbrains.annotations.NotNull;
import org.vloyalty.schema.TokenSchema;

import java.util.List;

//@TODO : Move this to implement #withNewOwner @see https://docs.corda.net/api-states.html
public class TokenState implements ContractState, QueryableState {
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

    //Now the api required for QueryableState
    @Override public PersistentState generateMappedObject(MappedSchema schema) {
        if (schema instanceof TokenSchema) {
            return new TokenSchema.PersistentToken(
                    this._issuer.getName().toString(),
                    this._owner.getName().toString(),
                    this._amount);
        } else {
            throw new IllegalArgumentException("Unrecognised schema $schema");
        }
    }

    @Override public Iterable<MappedSchema> supportedSchemas() {
        return ImmutableList.of(new TokenSchema());
    }

    @Override
    public String toString() {
        return String.format("TokenState(#tokens=%s, owner=%s, issuer=%s)", _amount, _owner, _issuer);
    }
}