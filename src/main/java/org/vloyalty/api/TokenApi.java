package org.vloyalty.api;

import net.corda.core.contracts.FungibleAsset;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.identity.Party;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.Builder;
import net.corda.core.node.services.vault.CriteriaExpression;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import org.vloyalty.flow.TokenIssueFlow;
import org.vloyalty.flow.TokenTransferFlowInitiator;
import org.vloyalty.schema.TokenSchema;
import org.vloyalty.state.TokenState;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.node.NodeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vloyalty.token.Token;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static java.util.stream.Collectors.toList;
import static javax.security.auth.callback.ConfirmationCallback.OK;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;

// This API is accessible from /api/token. All paths specified below are relative to it.
@Path("token")
public class TokenApi {
    private final CordaRPCOps rpcOps;
    private final CordaX500Name myLegalName;

    private final List<String> serviceNames = ImmutableList.of("Notary");

    static private final Logger logger = LoggerFactory.getLogger(TokenApi.class);

    public TokenApi(CordaRPCOps rpcOps) {
        this.rpcOps = rpcOps;
        this.myLegalName = rpcOps.nodeInfo().getLegalIdentities().get(0).getName();
    }

    /**
     * Returns the node's name.
     */
    @GET
    @Path("me")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, CordaX500Name> whoami() {
        return ImmutableMap.of("me", myLegalName);
    }

    /**
     * Returns all parties registered with the [NetworkMapService]. These names can be used to look up identities
     * using the [IdentityService].
     */
    @GET
    @Path("peers")
    @Produces(MediaType.APPLICATION_JSON)
    public Map<String, List<CordaX500Name>> getPeers() {
        List<NodeInfo> nodeInfoSnapshot = rpcOps.networkMapSnapshot();
        return ImmutableMap.of("peers", nodeInfoSnapshot
                .stream()
                .map(node -> node.getLegalIdentities().get(0).getName())
                .filter(name -> !name.equals(myLegalName) && !serviceNames.contains(name.getOrganisation()))
                .collect(toList()));
    }

    /**
     * Displays all Token states that exist in the node's vault.
     */
    @GET
    @Path("tokens")
    @Produces(MediaType.APPLICATION_JSON)
    public List<StateAndRef<TokenState>> getTokens() {
        return rpcOps.vaultQuery(TokenState.class).getStates();
    }

    /**
     * Initiates a flow to agree an IOU between two parties.
     *
     * Once the flow finishes it will have written the IOU to ledger. Both the lender and the borrower will be able to
     * see it when calling /api/example/ious on their respective nodes.
     *
     * This end-point takes a Party name parameter as part of the path. If the serving node can't find the other party
     * in its network map cache, it will return an HTTP bad request.
     *
     * The flow is invoked asynchronously. It returns a future when the flow's call() method returns.
     */
    //TokenIssueFlow(Party owner, int amount)
    //
    @PUT
    @Path("issue-tokens")
    public Response createTokens( //createIOU(
                @QueryParam("numtokens") int numTokens,
                @QueryParam("owner") CordaX500Name ownerPartyName) throws InterruptedException, ExecutionException {
        if (numTokens <= 0) {
            return Response.status(BAD_REQUEST).entity("Query parameter 'numtokens' must be non-negative.\n").build();
        }
        if (ownerPartyName == null) {
            return Response.status(BAD_REQUEST).entity("Query parameter 'owner' missing or has wrong format.\n").build();
        }

        System.out.println("#createTokens: owner="+ownerPartyName+ " #tokens="+numTokens);
        final Party otherParty = rpcOps.wellKnownPartyFromX500Name(ownerPartyName);
        if (otherParty == null) {
            return Response.status(BAD_REQUEST).entity("Party named " + ownerPartyName + "cannot be found.\n").build();
        }

        try {
            final SignedTransaction signedTx = rpcOps
                    .startTrackedFlowDynamic(TokenIssueFlow.class, otherParty, numTokens)
                    .getReturnValue()
                    .get();

            final String msg = String.format("TokenIssue Transaction id %s committed to ledger.\n", signedTx.getId());
            //System.out.println("#createTokens: #1");
            Response rr= Response.status(CREATED).entity(msg).build();
            //System.out.println("#createTokens: #2");
            return rr;

        } catch (Throwable ex) {
            final String msg = ex.getMessage();
            logger.error(ex.getMessage(), ex);
            return Response.status(BAD_REQUEST).entity(msg).build();
        }
    }

    @PUT
    @Path("transfer-tokens")
    public Response transferTokens(
                                  @QueryParam("numtokens") int numTokens,
                                  @QueryParam("newowner") CordaX500Name newOwnerPartyName) throws InterruptedException, ExecutionException {
        if (numTokens <= 0) {
            return Response.status(BAD_REQUEST).entity("Query parameter 'numtokens' must be non-negative.\n").build();
        }
        if (newOwnerPartyName == null) {
            return Response.status(BAD_REQUEST).entity("Query parameter 'newowner' missing or has wrong format.\n").build();
        }

        System.out.println("#transferTokens: newowner="+newOwnerPartyName+ " #tokens="+numTokens);
        final Party otherParty = rpcOps.wellKnownPartyFromX500Name(newOwnerPartyName);
        if (otherParty == null) {
            return Response.status(BAD_REQUEST).entity("Party named " + newOwnerPartyName + "cannot be found.\n").build();
        }

        try {
            final SignedTransaction signedTx = rpcOps
                    .startTrackedFlowDynamic(TokenTransferFlowInitiator.class, otherParty, numTokens)
                    .getReturnValue()
                    .get();

            final String msg = String.format("TokenTransfer Transaction id %s committed to ledger.\n", signedTx.getId());
            return Response.status(CREATED).entity(msg).build();

        } catch (Throwable ex) {
            final String msg = ex.getMessage();
            logger.error(ex.getMessage(), ex);
            return Response.status(BAD_REQUEST).entity(msg).build();
        }
    }

	/**
     * Displays all Token states that are created by this Party.
     * Update 15Jan.19 : Only those on THIS node will be queried!
     * So for now - show the states issued by LoyaltyAG
     */
    @GET
    @Path("tokens-issued-by-me")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getTokensIssuedByMe() throws NoSuchFieldException {
        QueryCriteria generalCriteria = new QueryCriteria.VaultQueryCriteria(Vault.StateStatus.ALL);
        QueryCriteria all_criteria = generalCriteria;
        List<StateAndRef<TokenState>> all_results = rpcOps.vaultQueryByCriteria(all_criteria,TokenState.class).getStates();
        System.out.println("#tokens-issued-by-loyaltyAg: all-criteria count=" + all_results.size());

        Field issuer = TokenSchema.PersistentToken.class.getDeclaredField("issuer");
        CordaX500Name loyaltyAg = new CordaX500Name("Loyalty_AG", "Zurich", "CH");
        CriteriaExpression issuerIndex = Builder.equal(issuer, loyaltyAg.toString());
        QueryCriteria issuerCriteria = new QueryCriteria.VaultCustomQueryCriteria(issuerIndex);

        QueryCriteria criteria = issuerCriteria; //generalCriteria.and(issuerCriteria);
        List<StateAndRef<TokenState>> results = rpcOps.vaultQueryByCriteria(criteria,TokenState.class).getStates();
        System.out.println("#tokens-issued-by-loyaltyAg: count=" + results.size());
        return Response.status(OK).entity(results).build();
    }
}