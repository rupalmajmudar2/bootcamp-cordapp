package org.vloyalty.flow;

import co.paralleluniverse.fibers.Suspendable;
import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.FinalityFlow;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.InitiatingFlow;
import net.corda.core.flows.StartableByRPC;
import net.corda.core.identity.Party;
import net.corda.core.transactions.SignedTransaction;
import net.corda.core.transactions.TransactionBuilder;
import org.vloyalty.contract.CouponContract;
import org.vloyalty.state.CouponState;

import java.security.PublicKey;
import java.util.List;

@InitiatingFlow
@StartableByRPC
public class CouponUpdateFlow extends AbstractTokenFlow { //FlowLogic<SignedTransaction> {
    private final Party _newOwner;
    private final String _newStatus;

    //TODO: Common superclass, refactoring with TokenIssueFlow
    public CouponUpdateFlow(Party newOwner, String newStatus) {
        _newOwner = newOwner;
        _newStatus = newStatus;
    }

    @Suspendable
    @Override
    public SignedTransaction call() throws FlowException {
        List<StateAndRef<CouponState>> couponStateAndRefs = getServiceHub().getVaultService().queryBy(CouponState.class).getStates();

        Party currentOwner= getOurIdentity();

        // We find the `Coupon`(s) with the current owner
        StateAndRef<CouponState> inputCouponStateAndRef = couponStateAndRefs
                .stream().filter(couponStateAndRef -> {
                    CouponState couponState = couponStateAndRef.getState().getData();
                    return couponState.getOwner().equals(currentOwner) /*&& couponState.getTitle().equals(title)*/;
                }).findAny().orElseThrow(() -> new IllegalArgumentException("No Coupon found for owner=" + currentOwner));
        CouponState inputCouponState = inputCouponStateAndRef.getState().getData();

        // We throw an exception if the flow was not started by the token's current owner.
        if (!(getOurIdentity().equals(inputCouponState.getOwner()))) {
            throw new IllegalStateException(
                    "This flow (CouponUpdateFlow) must be started by the current owner i.e. " + currentOwner +
                            ". Currenty being attempted by " + inputCouponState.getOwner());
        }

        //getProgressTracker().setCurrentStep(GENERATING_TXFR_INITIATOR_TRANSACTION);

        // We use the notary used by the input state.
        Party notary = inputCouponStateAndRef.getState().getNotary();

        // We build a transaction using a `TransactionBuilder`.
        TransactionBuilder txBuilder = new TransactionBuilder();

        // After creating the `TransactionBuilder`, we must specify which
        // notary it will use.
        txBuilder.setNotary(notary);

        // We add the input Token State to the transaction.
        txBuilder.addInputState(inputCouponStateAndRef);

        CouponState outputCouponStateForNewOwner= new CouponState(
                                                        inputCouponState.getText(),
                                                        inputCouponState.getIssuer(),
                                                        _newOwner,
                                                        inputCouponState.getDistributor(),
                                                        _newStatus);
        CouponState outputCouponStateForMe= new CouponState(
                inputCouponState.getText(),
                inputCouponState.getIssuer(),
                currentOwner,
                inputCouponState.getDistributor(),
                _newStatus);
        txBuilder.addOutputState(outputCouponStateForNewOwner, CouponContract.ID);
        //txBuilder.addOutputState(outputCouponStateForMe, CouponContract.ID);

        // We add the Transfer command to the transaction.
        // Note that we also specific who is required to sign the transaction.
        CouponContract.Commands.Update commandData = new CouponContract.Commands.Update();
        List<PublicKey> requiredSigners = ImmutableList.of(
                inputCouponState.getOwner().getOwningKey());
        txBuilder.addCommand(commandData, requiredSigners);

        // We check that the transaction builder we've created meets the
        // contracts of the input and output states.
        getProgressTracker().setCurrentStep(VERIFYING_TRANSACTION);
        txBuilder.verify(getServiceHub());

        // We finalise the transaction builder by signing it,
        // converting it into a `SignedTransaction`.
        getProgressTracker().setCurrentStep(SIGNING_TRANSACTION);

        SignedTransaction signedTransaction = getServiceHub().signInitialTransaction(txBuilder);

        // We get the transaction notarised and recorded automatically by the platform.
        getProgressTracker().setCurrentStep(FINALISING_TRANSACTION);
        return subFlow(new FinalityFlow(signedTransaction));

        //return null;
    }
}