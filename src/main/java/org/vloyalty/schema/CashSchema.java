package org.vloyalty.schema;

import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.Issued;
import net.corda.core.schemas.MappedSchema;
import net.corda.core.schemas.PersistentState;
import org.vloyalty.token.Cash;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * A TokenState schema.
 */
public class CashSchema extends MappedSchema {
    public CashSchema() {
        super(CashSchema.class, 1, ImmutableList.of(PersistentToken.class));
    }

    @Entity
    @Table(name = "cash_states")
    public static class PersistentToken extends PersistentState {
        @Column(name = "issuer") private final String issuer;
        @Column(name = "owner") private final String owner;
        @Column(name = "amt") private final int amt;
        @Column(name = "ccy") private final String ccy;

        public PersistentToken(String issuer, String owner, int amt, String ccy) {
            this.issuer = issuer;
            this.owner = owner;
            this.amt = amt;
            this.ccy = ccy;
        }

        public PersistentToken(String issuer, String owner, Amount<Issued<Cash>> amount, String ccy) {
            this.issuer = issuer;
            this.owner = owner;
            this.amt = (int) amount.getQuantity();
            this.ccy = ccy;
        }

        // Default constructor required by hibernate.
        public PersistentToken() {
            this.issuer = null;
            this.owner = null;
            this.amt = 0;
            this.ccy = "CHF";
        }

        public String getIssuer() {
            return issuer;
        }

        public String getOwner() {
            return owner;
        }

        public int getAmt() {
            return amt;
        }

        public String getCcy() {
            return ccy;
        }
    }
}