package org.vloyalty.schema;

import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.Issued;
import net.corda.core.schemas.MappedSchema;
import net.corda.core.schemas.PersistentState;
import org.vloyalty.token.Token;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.util.UUID;

/**
 * A TokenState schema.
 */
public class TokenSchema extends MappedSchema {
    public TokenSchema() {
        super(TokenSchema.class, 1, ImmutableList.of(PersistentToken.class));
    }

    @Entity
    @Table(name = "token_states")
    public static class PersistentToken extends PersistentState {
        @Column(name = "issuer") private final String issuer;
        @Column(name = "owner") private final String owner;
        @Column(name = "numTokens") private final int numTokens;

        public PersistentToken(String issuer, String owner, int numTokens) {
            this.issuer = issuer;
            this.owner = owner;
            this.numTokens = numTokens;
        }

        public PersistentToken(String issuer, String owner, Amount<Issued<Token>> amount) {
            this.issuer = issuer;
            this.owner = owner;
            this.numTokens = (int) amount.getQuantity();
        }

        // Default constructor required by hibernate.
        public PersistentToken() {
            this.issuer = null;
            this.owner = null;
            this.numTokens = 0;
        }

        public String getIssuer() {
            return issuer;
        }

        public String getOwner() {
            return owner;
        }

        public int getNumTokens() {
            return numTokens;
        }
    }
}