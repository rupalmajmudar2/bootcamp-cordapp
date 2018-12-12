package rmbak_pkg;

import co.paralleluniverse.fibers.Suspendable;
import java_bootcamp.TokenState;
import net.corda.core.contracts.StateAndRef;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.FlowSession;
import net.corda.core.flows.InitiatedBy;
import net.corda.core.identity.CordaX500Name;
import net.corda.core.node.NodeInfo;
import net.corda.core.node.ServiceHub;

import java.util.List;

@InitiatedBy(FlowTwoParty.class)
public class FlowTwoPartyResponder extends FlowLogic<Void> {
    private FlowSession _cptySession;

    public FlowTwoPartyResponder(FlowSession cptySession) {
        _cptySession= cptySession;
    }

    @Suspendable
    public Void call() throws FlowException {
        ServiceHub sh= getServiceHub(); //Portal to the services a node offers.
                    //Incl. past txns, states we know of etc. (VaultState) IMPORTANT!

        List<StateAndRef<TokenState>> statesFromVault=
                sh.getVaultService().queryBy(TokenState.class).getStates();

        CordaX500Name aliceKaName= new CordaX500Name("Alice", "Manchester", "UK");
        NodeInfo alice= sh.getNetworkMapCache().getNodeByLegalName(aliceKaName);

        int recvdInt= _cptySession.receive(Integer.class).unwrap(it -> {  //validating esp foa a signed txn or doc etc.
            if (it > 3) throw new IllegalArgumentException("Too high");

            return it;
        });

        int recvdIntPlusOne= recvdInt + 1;

        _cptySession.send(recvdIntPlusOne);

        return null;
    }
}
