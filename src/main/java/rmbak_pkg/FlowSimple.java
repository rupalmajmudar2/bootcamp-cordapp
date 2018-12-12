package rmbak_pkg;

import co.paralleluniverse.fibers.Suspendable;
import net.corda.core.flows.FlowException;
import net.corda.core.flows.FlowLogic;
import net.corda.core.flows.InitiatingFlow;
import net.corda.core.flows.StartableByRPC;

@InitiatingFlow
@StartableByRPC
public class FlowSimple extends FlowLogic<Integer> {

    @Suspendable
    //Qasar lib allows bytecodes to suspendable.
    public Integer call() throws FlowException {
        Integer a= 1;
        return a+2;
    }
}
