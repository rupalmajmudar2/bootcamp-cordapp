package org.vloyalty.api;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.corda.core.contracts.Amount;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.crypto.SecureHash;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.identity.Party;
import net.corda.core.messaging.CordaRPCOps;
import net.corda.core.node.NodeInfo;
import net.corda.core.node.services.Vault;
import net.corda.core.node.services.vault.Builder;
import net.corda.core.node.services.vault.CriteriaExpression;
import net.corda.core.node.services.vault.QueryCriteria;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.WireTransaction;
import net.corda.core.utilities.NetworkHostAndPort;
import net.corda.core.utilities.OpaqueBytes;
import net.corda.finance.flows.AbstractCashFlow;
import net.corda.finance.flows.CashIssueAndPaymentFlow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vloyalty.client.TokenClientAttachmentRPC;
import org.vloyalty.flow.CashIssueFlow;
import org.vloyalty.flow.CouponIssueFlow;
import org.vloyalty.flow.TokenAttachmentSender;
import org.vloyalty.flow.TokenIssueFlow;
import org.vloyalty.flow.TokenTransferFlowInitiator;
import org.vloyalty.schema.TokenSchema;
import org.vloyalty.state.CouponState;
import org.vloyalty.state.TokenState;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.ExecutionException;

import static java.util.stream.Collectors.toList;
import static javax.security.auth.callback.ConfirmationCallback.OK;
import static javax.ws.rs.core.Response.Status.BAD_REQUEST;
import static javax.ws.rs.core.Response.Status.CREATED;
import static org.junit.Assert.assertTrue;

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
                //.filter(name -> !name.equals(myLegalName) && !serviceNames.contains(name.getOrganisation()))
                .collect(toList()));
    }

    /**
     * Returns all parties details ** <b>Ugly ** Hard-coded **</b>
     * rupal 29Jan19 : Adding node-specific attributes right here, since -configFile option seems not to work
     * @TODO : Move these to configFile lnode.conf
     * Output: Hashmap of [key = nodeName,
     *                      value = Hashmap  [
     *                                  isCustomer = true/false
     */
    @GET
    @Path("peer-details")
    @Produces(MediaType.APPLICATION_JSON)
    public String /*Map<String, List<CordaX500Name>>*/ getPeerDetails() {
        List<NodeInfo> nodeInfoSnapshot = rpcOps.networkMapSnapshot();
        HashMap<String, HashMap<String,String>> peerMap= new HashMap();
        for (NodeInfo node: nodeInfoSnapshot) {
            NetworkHostAndPort hp= node.getAddresses().get(0);
            int port = hp.getPort();

            boolean isCustNode= false;
            boolean isPartnerNode= false;
            boolean isNotary= false;

            if (node.toString().toLowerCase().contains("customer")) isCustNode = true;

            if (node.toString().toLowerCase().contains("valora")) isPartnerNode= true;
            if (node.toString().toLowerCase().contains("loyalty")) isPartnerNode = true;
            if (node.toString().toLowerCase().contains("sbb")) isPartnerNode = true;
            if (node.toString().toLowerCase().contains("alpamare")) isPartnerNode = true;
            if (node.toString().toLowerCase().contains("evian")) isPartnerNode = true;

            if (node.toString().toLowerCase().contains("notary")) isNotary = true;

            HashMap mapValues= new HashMap<String,String>();
            mapValues.put("port", port);
            mapValues.put("isCustomerNode", isCustNode);
            mapValues.put("isPartnerNode", isPartnerNode);
            mapValues.put("isNotary", isNotary);

            Party nodeParty= node.getLegalIdentities().get(0);
            String nodePartyStr= nodeParty.getName().toString();

            /*System.out.println("Node is: " + nodePartyStr + " IsCustomerNode=" + isCustNode
                                    + " IsPartnerNode=" + isPartnerNode  + " IsMyNotary=" + isNotary);*/

            peerMap.put(nodePartyStr, mapValues);
        }

        //Gson gson = new Gson();
        //@see https://stackoverflow.com/questions/16558709/gson-issue-with-string
        Gson gson = new GsonBuilder().disableHtmlEscaping().create();
        String json = gson.toJson(peerMap, HashMap.class);

        return json;
        /*return peerMap; /* ImmutableMap.of("peer-details", peerMap
                .stream()
                .map(node -> node.getLegalIdentities().get(0).getName())
                //.filter(name -> !name.equals(myLegalName) && !serviceNames.contains(name.getOrganisation()))
                .collect(toList()));*/
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
     * Displays all Coupon states that exist in the node's vault.
     */
    @GET
    @Path("coupons")
    @Produces(MediaType.APPLICATION_JSON)
    public List<StateAndRef<CouponState>> getCoupons() {
        return rpcOps.vaultQuery(CouponState.class).getStates();
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
        System.out.println("In TokenApi#issueTokens");
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

            final String msg = String.format("TokenIssue by %s for %s: Txn# %s committed to ledger.\n",
                                            myLegalName.toString(),
                                            ownerPartyName.toString(),
                                            signedTx.getId());
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

    @PUT
    @Path("send-attachment")
    public Response sendAttachment(
            @QueryParam("filename") String zipFileName,
            @QueryParam("newowner") CordaX500Name newOwnerPartyName) throws InterruptedException, ExecutionException, Exception {
        System.out.println("In TokenApi#sendAttachment");
        if (zipFileName == null) {
            return Response.status(BAD_REQUEST).entity("Query parameter 'filename' must be valid.\n").build();
        }
        if (newOwnerPartyName == null) {
            return Response.status(BAD_REQUEST).entity("Query parameter 'newowner' missing or has wrong format.\n").build();
        }

        System.out.println("#sendAttachment: newowner="+newOwnerPartyName+ " attachment file="+zipFileName);
        final Party otherParty = rpcOps.wellKnownPartyFromX500Name(newOwnerPartyName);
        if (otherParty == null) {
            return Response.status(BAD_REQUEST).entity("Party named " + newOwnerPartyName + "cannot be found.\n").build();
        }

        SecureHash attachmentHash= null;
        attachmentHash = TokenClientAttachmentRPC.doAttachZipFile(rpcOps, zipFileName);
        assertTrue (rpcOps.attachmentExists(attachmentHash));

        System.out.println("TokenClientAttachmentRPC#doAttachZipFile Done uploading pdf - AtachmentHash#"+attachmentHash);

                   /* startTrackedFlow(::AttachmentDemoFlow, otherSideFuture.get(), notaryFuture.get(), hash)
            flowHandle.progress.subscribe(::println)
            val stx = flowHandle.returnValue.getOrThrow()
            println("Sent ${stx.id}")*/
        try {
            final SignedTransaction signedTx2 = rpcOps
                    .startTrackedFlowDynamic(TokenAttachmentSender.class, otherParty, attachmentHash)
                    .getReturnValue()
                    .get();

            final String msg = String.format("TokenAttachmentSender Transaction id %s committed to ledger.\n", signedTx2.getId());
            System.out.println(" TokenAttachmentSender Transaction id %s committed to ledger."  + signedTx2.getId());
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
        System.out.println("#tokens-total: all-criteria count=" + all_results.size());

        Field issuer = TokenSchema.PersistentToken.class.getDeclaredField("issuer");
        CordaX500Name loyaltyAg = new CordaX500Name("Valora", "Zurich", "CH");
        CriteriaExpression issuerIndex = Builder.equal(issuer, loyaltyAg.toString());
        QueryCriteria issuerCriteria = new QueryCriteria.VaultCustomQueryCriteria(issuerIndex);

        QueryCriteria criteria = issuerCriteria; //generalCriteria.and(issuerCriteria);
        List<StateAndRef<TokenState>> results = rpcOps.vaultQueryByCriteria(criteria,TokenState.class).getStates();
        System.out.println("#tokens-issued-by-loyaltyAg: count=" + results.size());
        return Response.status(OK).entity(results).build();
    }

    @GET
    @Path("txns")
    @Produces(MediaType.APPLICATION_JSON)
    public String getTxns()  {
        List<SignedTransaction> txns= rpcOps.internalVerifiedTransactionsSnapshot();

        System.out.println("#Txns: count=" + txns.size());

        //Map(Sequence# ->(TxnId : TxnString_Inputs_Outputs)
        HashMap<String, String> txnMap= new HashMap();
        for (int i=0; i < txns.size(); i++) {
            SignedTransaction stxn= txns.get(i);
            String txnId= stxn.getId().toString();
            WireTransaction wtx= stxn.getTx();
            String txnDets= wtx.toString();

            //String txnStr = "Txn#" + (i+1) + " : Id=" + txnId.substring(0,4) + " " + txnDets;
            String txnNr= (i+1) + "";
            String txnStr= txnId.substring(0,4) + ":" + txnDets;

            int  cmdIndx= txnStr.indexOf("COMMAND:");
            txnStr= txnStr.substring(0, cmdIndx);

            txnStr= txnStr.replaceAll("\r\n", "");
            txnStr= txnStr.replaceAll("[^\\x00-\\x7F]", ""); //Removing funny asci, thanks to https://stackoverflow.com/questions/8519669/replace-non-ascii-character-from-string

            System.out.println("#getTxns starting with: " + txnStr);
            txnStr= inputStringCleanup(txnStr);
            System.out.println("#getTxns returning: " + txnStr);

            txnMap.put(txnNr, txnStr);
        }

        Gson gson = new GsonBuilder().disableHtmlEscaping().create();
        String json = gson.toJson(txnMap, HashMap.class);

        return json; //Response.status(OK).entity(txnStr).build();
    }

    //@see ApiTests#parseTxnString
    public String inputStringCleanup(String str) {
        String finalStr= "";
        int startIndx= str.indexOf("INPUT:");
        if (startIndx > -1) {
            finalStr = str.substring(0, startIndx+6); //upto INPUT:
            int endIndx = str.indexOf("(");
            String stringEndOfInputTillEnd= str.substring(startIndx+6, endIndx).trim(); //the full txnId
            String shortTxnId= stringEndOfInputTillEnd.substring(0,4);

            String remaining= str.substring(endIndx);
            if (endIndx > -1) {
                finalStr= finalStr.concat(shortTxnId).concat(remaining);
            }
        }
        else return str;

        return finalStr;
    }

    //===================== Now for Cash, Coupons etc. =============================

    /**
     *  Request Cash issuance
     *  https://github.com/corda/corda/tree/master/samples/bank-of-corda-demo
     *  BankOfCordaWebApi.kt
     */
    @PUT
    @Path("issue-cash")
    public Response issueCash( //createIOU(
                                  @QueryParam("amount") int cashAmount,
                                  @QueryParam("owner") CordaX500Name ownerPartyName) throws InterruptedException, ExecutionException {
        System.out.println("In TokenApi#issueCash");
        if (cashAmount <= 0) {
            return Response.status(BAD_REQUEST).entity("Query parameter 'amount' must be non-negative.\n").build();
        }
        if (ownerPartyName == null) {
            return Response.status(BAD_REQUEST).entity("Query parameter 'owner' missing or has wrong format.\n").build();
        }

        System.out.println("#createCash: owner=" + ownerPartyName + " #cash=" + cashAmount);
        final Party otherParty = rpcOps.wellKnownPartyFromX500Name(ownerPartyName);
        if (otherParty == null) {
            return Response.status(BAD_REQUEST).entity("Party named " + ownerPartyName + "cannot be found.\n").build();
        }

        Party notary = rpcOps.notaryIdentities().get(0);
        boolean anonymous = true;
        //val issuerBankPartyRef = OpaqueBytes.of(params.issuerBankPartyRef.toByte())
        //String txnRefStr = "CashTxn#1234";
        //OpaqueBytes txnRef= OpaqueBytes.of(txnRefStr.getBytes());
        //Currency chf= Currency.getInstance("CHF");

        //Amount<Currency> amount= Amount.parseCurrency(cashAmount + " CHF");
        try {
            final SignedTransaction result = rpcOps
                    .startTrackedFlowDynamic(CashIssueFlow.class, otherParty, cashAmount)
                    .getReturnValue()
                    .get();

            final String msg = String.format("Cash Issue to %s for %s: Txn# %s committed to ledger.\n",
                    myLegalName.toString(),
                    cashAmount,
                    result.getId());
            //System.out.println("#createTokens: #1");
            Response rr = Response.status(CREATED).entity(msg).build();
            //System.out.println("#createTokens: #2");
            return rr;

        } catch (Throwable ex) {
            final String msg = ex.getMessage();
            logger.error(ex.getMessage(), ex);
            return Response.status(BAD_REQUEST).entity(msg).build();
        }
    }

    //===============

    @PUT
    @Path("issue-coupon")
    public Response issueCoupon(
                                  @QueryParam("text") String text,
                                  @QueryParam("owner") CordaX500Name ownerPartyName,
                                  @QueryParam("distributor") CordaX500Name distributorPartyName) throws Exception {
        System.out.println("In TokenApi#issueCoupon");
        if (text.trim().equals("")) {
            return Response.status(BAD_REQUEST).entity("Query parameter 'text' must be specified.\n").build();
        }
        if (ownerPartyName == null) {
            return Response.status(BAD_REQUEST).entity("Query parameter 'owner' missing or has wrong format.\n").build();
        }
        if (distributorPartyName == null) {
            return Response.status(BAD_REQUEST).entity("Query parameter 'distributor' missing or has wrong format.\n").build();
        }

        System.out.println("#issueCoupon: distributor="+distributorPartyName+" owner="+ownerPartyName+ " text="+text);
        final Party ownerParty = rpcOps.wellKnownPartyFromX500Name(ownerPartyName);
        if (ownerParty == null) {
            return Response.status(BAD_REQUEST).entity("Party named " + ownerPartyName + "cannot be found.\n").build();
        }
        final Party distParty = rpcOps.wellKnownPartyFromX500Name(distributorPartyName);
        if (distParty == null) {
            return Response.status(BAD_REQUEST).entity("Party named " + distributorPartyName + "cannot be found.\n").build();
        }

        try {
            String timestamp= new SimpleDateFormat("yyyyMMdd_HHmmss").format(Calendar.getInstance().getTime());
            final SignedTransaction signedTx = rpcOps
                    .startTrackedFlowDynamic(CouponIssueFlow.class, text, ownerParty, distParty, "Coupon_Issued_" + timestamp)
                    .getReturnValue()
                    .get();

            final String msg = String.format("Coupon %s issued by %s to %s: Txn# %s committed to ledger.\n",
                    text,
                    myLegalName.toString(),
                    ownerPartyName.toString(),
                    signedTx.getId());
            Response rr= Response.status(CREATED).entity(msg).build();

            return rr;

        } catch (Throwable ex) {
            final String msg = ex.getMessage();
            logger.error(ex.getMessage(), ex);
            return Response.status(BAD_REQUEST).entity(msg).build();
        }
    }
}