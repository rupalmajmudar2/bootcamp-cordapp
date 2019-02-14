package org.vloyalty.state;

import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Random;

public class CouponState implements ContractState {
    private int _couponId;
    private String _text;
    private Party _issuer; //e.g. Evian
    private Party _distributor; //e.g. SBB
    private Party _owner; //e.g. Valora
    private String _status; //= status; //timeStamp; //"Issued", "Presented", "Redeemed", "Present_Evidence"

    //@see https://stackoverflow.com/questions/50035970/java-io-notserializableexception-no-constructor-for-deserialization-found-fo
    public CouponState(String text, Party issuer, Party owner, Party distributor, String status) {
        Random random = new Random();
        int upperBound = 999999; int lowerBound = 100000;
        _couponId = lowerBound + (int)(Math.random() * ((upperBound - lowerBound) + 1));

        _text = text;
        _owner = owner;
        _issuer = issuer;
        _distributor = distributor;
        _status = status;
    }

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return ImmutableList.of(_issuer, _owner);
    }

    public String getText() {
        return _text;
    }

    public Party getOwner() {
        return _owner;
    }

    public Party getIssuer() {
        return _issuer;
    }

    public Party getDistributor() {
        return _distributor;
    }

    public String getStatus() {
        if (_status == null) {
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
            _status= timeStamp;
        }

        return _status;
    }

    @Override
    public String toString() {
        String ownerOrg= _owner.getName().getOrganisation();
        String issuerOrg= _issuer.getName().getOrganisation();

        return "Coupon [" + _text + " Owner=" + ownerOrg + " Issuer=" + issuerOrg + " Status=" + _status;
        //ownerOrg + " " + _numTokens + " tokens issued by " + issuerOrg;

        //return String.format("CouponState(text=%s, owner=%s, issuer=%s, distributor=%s, status=%s)", _text, _owner, _issuer, _distributor, _status);
    }
}