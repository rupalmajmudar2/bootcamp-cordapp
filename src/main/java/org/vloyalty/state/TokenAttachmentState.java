package org.vloyalty.state;

import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.ContractState;
import net.corda.core.crypto.SecureHash;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;
import net.corda.core.serialization.ConstructorForDeserialization;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class TokenAttachmentState implements ContractState {
    private SecureHash _attachmentHash;
    private Party _receiver;

    @ConstructorForDeserialization
    public TokenAttachmentState(SecureHash attachmentHash, Party receiver) {
        _attachmentHash= attachmentHash;
        _receiver= receiver;
    }

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return ImmutableList.of(_receiver);
    }

    public SecureHash getAttachmentHash() {
        return _attachmentHash;
    }

    public Party getReceiver() {
        return _receiver;
    }

    @Override
    public String toString() {
        String hash= _attachmentHash.toString();
        return "Attachment Id#" + hash.substring(0,4) + " to " + _receiver.getName().getOrganisation();
        //return String.format("TokenAttachmentState(attachmentHash#=%s,)", _attachmentHash.toString());
    }
}