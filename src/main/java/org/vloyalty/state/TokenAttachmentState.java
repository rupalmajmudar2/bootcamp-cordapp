package org.vloyalty.state;

import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.ContractState;
import net.corda.core.crypto.SecureHash;
import net.corda.core.identity.AbstractParty;
import net.corda.core.serialization.ConstructorForDeserialization;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class TokenAttachmentState implements ContractState {
    private SecureHash _attachmentHash;

    @ConstructorForDeserialization
    public TokenAttachmentState(SecureHash attachmentHash) {
        _attachmentHash= attachmentHash;
    }

    @NotNull
    @Override
    public List<AbstractParty> getParticipants() {
        return ImmutableList.of();
    }

    public SecureHash getAttachmentHash() {
        return _attachmentHash;
    }

    @Override
    public String toString() {
        return String.format("TokenAttachmentState(attachmentHash#=%s,)", _attachmentHash.toString());
    }
}