package rmbak_pkg;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.flows.*;
import net.corda.core.identity.Party;

@InitiatingFlow
@StartableByRPC
public class FlowTwoParty extends FlowLogic<Integer> {
    private Party _cpty;
    private Integer _nr;

    public FlowTwoParty(Party cpty,Integer nr) {
        _cpty= cpty;
        _nr= nr;
    }

    @Suspendable
    public Integer call() throws FlowException {
        //any 2 nodes communicate over this session.
        FlowSession session = initiateFlow(_cpty);
        session.send(_nr);

        int recvdIncrementedInt= session.receive(Integer.class).unwrap(it ->  it);

        return recvdIncrementedInt;
    }

}
