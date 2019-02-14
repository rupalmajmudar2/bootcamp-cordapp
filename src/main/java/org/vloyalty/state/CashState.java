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
import org.vloyalty.schema.CashSchema;
import org.vloyalty.token.Cash;

import java.math.BigDecimal;
import java.security.PublicKey;
import java.util.List;

public class CashState implements FungibleAsset<Cash>, QueryableState {
    private int _amount;
    private String _ccy;
    private Party _owner;
    private Party _issuer;

    public CashState() {
    }  // For serialization

    //@see https://stackoverflow.com/questions/50035970/java-io-notserializableexception-no-constructor-for-deserialization-found-fo
    public CashState(Party issuer, Party owner, int amount, String ccy) {
        _amount = amount;
        _ccy = ccy; //"CHF"; //hard-coded for now
        _owner = owner;
        _issuer = issuer;
    }

    @ConstructorForDeserialization
    public CashState(Party issuer, Party owner, Amount<Issued<Cash>> amount, String ccy) {
        _amount = (int) amount.getQuantity();
        _owner = owner;
        _issuer = issuer;
        _ccy = ccy;
    }

    @Override
    public List<PublicKey> getExitKeys() {
        //@TODO : check!
        return ImmutableList.of(_owner.getOwningKey());
    }

    @Override
    public Amount<Issued<Cash>> getAmount() {
        return getAmountFor(_amount);
    }

    public int getAmountInt() {
        return _amount;
    }

    public Amount<Issued<Cash>> getAmountFor(int numTokens) {
        //@TODO : Ugly Bad - do cleanup!!
        //Amount amt= getCcyAmount(String.valueOf(_numTokens));
        Cash t= new Cash();

        //@see https://stackoverflow.com/questions/50131549/in-corda-what-should-partyandreference-reference-be-set-to
        //@TODO : review this later.
        OpaqueBytes dummyRef= OpaqueBytes.of("RMTEMP".getBytes());
        PartyAndReference pr= new PartyAndReference(_issuer, dummyRef);
        Issued<Cash> ic= new Issued(pr, t);
        BigDecimal b= BigDecimal.valueOf(numTokens);
        return Amount.fromDecimal(b, ic);
    }

    public CommandAndState withNewOwner(AbstractParty newOwner) {
        Party newOwnerParty= (Party) newOwner; //@TODO : cleanup!

        return new CommandAndState(new TokenContract.Commands.Transfer(), new CashState(_issuer, newOwnerParty, _amount, _ccy));
    }

    public CashState withNewOwnerAndAmount(Amount<Issued<Cash>> amount, AbstractParty newOwner) {
        int amt= (int) amount.getQuantity(); //@TODO : check!
        Party newOwnerParty= (Party) newOwner; //@TODO : cleanup!

        return new CashState(_issuer, newOwnerParty, amt, "CHF");
    }

    //For re-issue of part of the amount to ourselves
    public TokenState withNewAmount(Amount<Issued<Cash>> amount) {
        int numTokens= (int) amount.getQuantity(); //@TODO : check!

        return new TokenState(_issuer, _owner, numTokens);
    }

    public Party getOwner() {
        return _owner;
    }

    public Party getIssuer() {
        return _issuer;
    }

    public String getCcy() {
        if (_ccy == null) _ccy="CHF";

        return _ccy;
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
        if (schema instanceof CashSchema) {
            return new CashSchema.PersistentToken(
                    this._issuer.getName().toString(),
                    this._owner.getName().toString(),
                    this._amount,
                    this._ccy);
        } else {
            throw new IllegalArgumentException("Unrecognised schema.");
        }
    }

    @Override public Iterable<MappedSchema> supportedSchemas() {
        return ImmutableList.of(new CashSchema());
    }

    @Override
    public String toString() {
        //return String.format("CashState(amount=%s %s, owner=%s, issuer=%s)", _amount, _ccy, _owner, _issuer);
        String ownerOrg= _owner.getName().getOrganisation();
        String issuerOrg= _issuer.getName().getOrganisation();

        return "Cash [" + _amount + " " + _ccy + " Owner=" + ownerOrg + " Issuer=" + issuerOrg;
    }
}