package rmbak_pkg;

import com.google.common.collect.ImmutableList;
import net.corda.core.contracts.ContractState;
import net.corda.core.identity.AbstractParty;
import net.corda.core.identity.Party;

import java.util.List;

public class ContainerState implements ContractState {
    private int _width;
    private int _height;
    private int _depth;
    private String _contents;
    private Party _owner;
    private Party _carrier;

    public ContainerState(int width, int height, int depth, String contents, Party owner, Party carrier) {
        _width = width;
        _height = height;
        _depth = depth;
        _contents = contents;
        _owner = owner;
        _carrier = carrier;
    }

    public int get_width() {
        return _width;
    }

    public int get_height() {
        return _height;
    }

    public int get_depth() {
        return _depth;
    }

    public String get_contents() {
        return _contents;
    }

    public Party get_owner() {
        return _owner;
    }

    public Party get_carrier() {
        return _carrier;
    }

    public static void main(String[] args) {
        Party jetpackImporters= null;
        Party jetpackCarriers= null;

        ContainerState ks= new ContainerState(
                                2,
                                4,
                                2,
                                "JetPack",
                                jetpackImporters,
                                jetpackCarriers);

    }

    //Who gets notified when the ledger state changes - issued/modified/updated/destroyed
    //e.g. regulatory body, common authority etc.
    public List<AbstractParty> getParticipants() {
        return ImmutableList.of(_owner, _carrier);
    }
}
