package org.vloyalty.state;

import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.*;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.schemas.MappedSchema;
import net.corda.core.schemas.PersistentState;
import net.corda.core.schemas.QueryableState;
import net.corda.core.serialization.ConstructorForDeserialization;
import net.corda.core.utilities.OpaqueBytes;
import org.jetbrains.annotations.NotNull;
import org.vloyalty.contract.TokenContract;
import org.vloyalty.schema.TokenSchema;
import org.vloyalty.token.Token;

import java.math.BigDecimal;
import java.security.PublicKey;
import java.util.Currency;
import java.util.List;

public class TokenState implements FungibleAsset<Token>, OwnableState, QueryableState {
    private int _numTokens;
    private Party _owner;
    private Party _issuer;

    public TokenState() {
    }  // For serialization

    //@see https://stackoverflow.com/questions/50035970/java-io-notserializableexception-no-constructor-for-deserialization-found-fo
    public TokenState(Party issuer, Party owner, int amount) {
        _numTokens = amount;
        _owner = owner;
        _issuer = issuer;
    }

    @ConstructorForDeserialization
    public TokenState(Party issuer, Party owner, Amount<Issued<Token>> amount) {
        _numTokens = (int) amount.getQuantity();
        _owner = owner;
        _issuer = issuer;
    }

    @Override
    public List<PublicKey> getExitKeys() {
        //@TODO : check!
        return ImmutableList.of(_owner.getOwningKey());
    }

    @Override
    public Amount<Issued<Token>> getAmount() {
        return getAmountFor(_numTokens);
    }

    public Amount<Issued<Token>> getAmountFor(int numTokens) {
        //@TODO : Ugly Bad - do cleanup!!
        //Amount amt= getCcyAmount(String.valueOf(_numTokens));
        Token t= new Token();

        //@see https://stackoverflow.com/questions/50131549/in-corda-what-should-partyandreference-reference-be-set-to
        //@TODO : review this later.
        OpaqueBytes dummyRef= OpaqueBytes.of("RMTEMP".getBytes());
        PartyAndReference pr= new PartyAndReference(_issuer, dummyRef);
        Issued<Token> ic= new Issued(pr, t);
        BigDecimal b= BigDecimal.valueOf(numTokens);
        return Amount.fromDecimal(b, ic);
    }

    public CommandAndState withNewOwner(AbstractParty newOwner) {
        Party newOwnerParty= (Party) newOwner; //@TODO : cleanup!

        return new CommandAndState(new TokenContract.Commands.Transfer(), new TokenState(_issuer, newOwnerParty, _numTokens));
    }

    public TokenState withNewOwnerAndAmount(Amount<Issued<Token>> amount, AbstractParty newOwner) {
        int numTokens= (int) amount.getQuantity(); //@TODO : check!
        Party newOwnerParty= (Party) newOwner; //@TODO : cleanup!

        return new TokenState(_issuer, newOwnerParty, numTokens);
    }

    public int getNumTokens() {
        return _numTokens;
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
                    this._numTokens);
        } else {
            throw new IllegalArgumentException("Unrecognised schema $schema");
        }
    }

    @Override public Iterable<MappedSchema> supportedSchemas() {
        return ImmutableList.of(new TokenSchema());
    }

    @Override
    public String toString() {
        return String.format("TokenState(#tokens=%s, owner=%s, issuer=%s)", _numTokens, _owner, _issuer);
    }
}